package com.mriss.dsh.restapi.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.restapi.dto.DocumentStatusDto;
import com.mriss.dsh.restapi.dto.TokenDto;
import com.mriss.dsh.restapi.service.DocumentHandlingService;
import com.mriss.dsh.restapi.service.DocumentSubmissionException;
import com.mriss.dsh.restapi.service.DocumentSubmissionService;

@RestController
@RequestMapping("/v1/dsh/document")
public class DocumentResource {
	
	public static final String TOKEN_NOT_FOUND_MESSAGE = "No token was found with the contents: ";

	public static final String TOKEN_NOT_FOUND = "TOKEN_NOT_FOUND";

	private final static Logger log = Logger.getLogger(DocumentResource.class);
	
	@Autowired
	DocumentSubmissionService documentSubmissionService;
	
	@Autowired
	DocumentHandlingService documentHandlingService;

	@RequestMapping(value = "/submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody TokenDto submitDocument(HttpServletRequest request, @RequestParam("title") String title, @RequestParam("contents") MultipartFile contents)
            throws IllegalStateException, IOException {
		try {			
			validateParameters(title, contents);
			String token = documentSubmissionService.getTokenFromDocument(contents.getInputStream(), title, false);
			documentSubmissionService.storeDocumentAndQueueForProcessing();
			return new TokenDto(token, "");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new TokenDto("ERROR", "Error submitting file: " + e.getMessage()); 
		}
	}
	
	@RequestMapping(value = "/status/{token}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody DocumentStatusDto getStatus(HttpServletRequest request, @PathVariable("token") String token) {
		Document d = documentHandlingService.getDocumentByToken(token);
		if (d == null) {
			return new DocumentStatusDto(TOKEN_NOT_FOUND, TOKEN_NOT_FOUND_MESSAGE + token);
		} else {
			return new DocumentStatusDto(d.getDocumentStatus().getStatusDescription(), d.getDocumentStatusMessage());
		}
	}

	private void validateParameters(String title, MultipartFile contents)
			throws DocumentSubmissionException {
		if (StringUtils.isBlank(title) || contents == null) {
			throw new DocumentSubmissionException("Document title and contents can't be null.");
		}
	}

}

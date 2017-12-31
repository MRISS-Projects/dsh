package com.mriss.dsh.restapi.service;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import com.mriss.dsh.data.models.Document;

@Service
@RequestScope(proxyMode=ScopedProxyMode.TARGET_CLASS)
public class DocumentSubmissionServiceImpl implements DocumentSubmissionService {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentSubmissionServiceImpl.class);
	
	private Document document;
	
	@Autowired
	private DocumentHandlingService docHandlingService;
	
	@Autowired
	private DocumentQueueService docQueueService;
	
	@Autowired
	private DocumentEnqueueMessageHandler messageHandler;

	private boolean storeDocument = false;
	
	@Override
	public String getTokenFromDocument(InputStream originalDocumentContents, String documentTitle,
			boolean useCache) throws DocumentSubmissionException {
		try {
			this.document = new Document(originalDocumentContents, documentTitle);
			messageHandler.setDocument(this.document);			
			if (useCache) {
				Document doc = docHandlingService.getDocumentByHash(this.document.getFileHash());
				if (doc != null) {
					return doc.getToken();
				} else {
					storeDocument = true;
					return this.document.getToken();
				}
			} else {
				storeDocument = true;
				return document.getToken();
			}
		} catch (Exception e) {
			try {
				throw new DocumentSubmissionException(e.getMessage(), e, this.document != null ? document : new Document(documentTitle));
			} catch (NoSuchAlgorithmException e1) {
				logger.error(e1.getMessage(), e1);
				return null;
			}
		}
	}

	@Override
	public void storeDocumentAndQueueForProcessing() {
		if (document != null && storeDocument) {
			docHandlingService.storeDocument(this.document);
			docQueueService.enqueueDocumentId(document.getId(), messageHandler);
		}
	}

	public void setMessageHandler(DocumentEnqueueMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

}

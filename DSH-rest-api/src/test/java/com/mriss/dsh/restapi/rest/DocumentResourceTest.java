package com.mriss.dsh.restapi.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.RequestContextFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.restapi.DshRestApplication;
import com.mriss.dsh.restapi.dto.TokenDto;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:/abstract-web-request-context.xml" }, classes = {DshRestApplication.class} )
public class DocumentResourceTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	private MediaType contentType = new MediaType(
			MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	protected MockHttpServletRequest request;

	@Autowired(required = true)
	@Qualifier("testFile1")
	private File testFile1;

	final static Logger logger = LoggerFactory.getLogger(DocumentResourceTest.class);

	private static boolean clean = false;

	@Autowired(required = true)
	private DocumentDao dao;

	@Before
	public void setup() throws Exception {
		if (!clean) {
			dao.clearDocuments();
			clean = true;
		}		
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).dispatchOptions(true).addFilter(new RequestContextFilter(), "/*").build();
	}

	@Test
	public void testSubmitDocument() throws Exception {

		MockMultipartFile firstFile = new MockMultipartFile("contents", "bbc-news-1.pdf", "application/pdf", new FileInputStream(testFile1));

		// First request
		
		startRequest();
		
		MvcResult result = mockMvc
				.perform(
						MockMvcRequestBuilders
								.multipart("/v1/dsh/document/submit")
								.file(firstFile)
								.param("title",
										"Russia-Trump: FBI chief Wray defends agency"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(
						jsonPath("token", org.hamcrest.Matchers.notNullValue()))
				.andExpect(
						jsonPath("message",
								org.hamcrest.Matchers.notNullValue()))
				.andReturn();

		synchronized (this) {
			this.wait(5000);
		}
		
		endRequest();
		
		String contentAsString = result.getResponse().getContentAsString();
		logger.info("Document submission response: " + contentAsString);
		TokenDto tDto = new ObjectMapper().readerFor(TokenDto.class).readValue(contentAsString);
		Document document = dao.findDocumentByToken(tDto.getToken());
		assertNotNull(document);
		asssertEquals(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS, document.getDocumentStatus());
		
		// Second request to test new instances of document submission service and message handler
		
		firstFile = new MockMultipartFile("contents", "bbc-news-1.pdf", "application/pdf", new FileInputStream(testFile1));

		startRequest();
		
		result = mockMvc
				.perform(
						MockMvcRequestBuilders
								.multipart("/v1/dsh/document/submit")
								.file(firstFile)
								.param("title",
										"Russia-Trump: FBI chief Wray defends agency"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(
						jsonPath("token", org.hamcrest.Matchers.notNullValue()))
				.andExpect(
						jsonPath("message",
								org.hamcrest.Matchers.notNullValue()))
				.andReturn();

		synchronized (this) {
			this.wait(5000);
		}
		
		endRequest();
		
		contentAsString = result.getResponse().getContentAsString();
		logger.info("Document submission response: " + contentAsString);
		tDto = new ObjectMapper().readerFor(TokenDto.class).readValue(contentAsString);
		assertNotNull(document);
		asssertEquals(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS, document.getDocumentStatus());

	}
	
	private void asssertEquals(DocumentStatus queuedForIndexingSuccess,
			DocumentStatus documentStatus) {
		// TODO Auto-generated method stub
		
	}

	@Test
	public void testError() throws Exception {
		
		MockMultipartFile firstFile = new MockMultipartFile("contents", "bbc-news-1.pdf", "application/pdf", new FileInputStream(testFile1));
		
		startRequest();
		
		MvcResult result = mockMvc
				.perform(
						MockMvcRequestBuilders
								.multipart("/v1/dsh/document/submit")
								.file(firstFile)
								.param("title",
										""))
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(
						jsonPath("token", is("ERROR")))
				.andExpect(
						jsonPath("message", org.hamcrest.Matchers.startsWith("Error submitting file: ")
								))
				.andReturn();

		synchronized (this) {
			this.wait(5000);
		}
		
		endRequest();
		
		String contentAsString = result.getResponse().getContentAsString();
		logger.info("Document submission response: " + contentAsString);

	}
	

	protected void startRequest() {
		request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request), true);
	}

	protected void endRequest() {
		((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).requestCompleted();
		RequestContextHolder.resetRequestAttributes();
		request = null;
	}

}

@Configuration
class DocumentResourceTestTestConfiguration {

	@Bean(name = "testFile1")
	public File getTestFile1() throws Exception {
		return new File("target/test-classes/pdf/bbc-news-1.pdf");
	}

}

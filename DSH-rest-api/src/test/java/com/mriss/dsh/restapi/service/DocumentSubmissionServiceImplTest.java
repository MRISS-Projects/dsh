package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.mriss.dsh.data.document.dao.mongo.MongoDocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class DocumentSubmissionServiceImplTest {
	
	@Autowired
	DocumentQueueService queueService;
	
	@Autowired(required = true)
	private DocumentHandlingService docHandlingService;
	
	@Autowired(required=true)
	private MongoDocumentDao dao;	

	@Mock
	DocumentEnqueueResponseMessageHandler messageHandler;
	
	@InjectMocks
	@Autowired
	DocumentSubmissionService service;
			
	private static boolean clean =  false;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		if (!clean) {
			dao.clearDocuments();
			clean = true;
		}
	}
	
	@Test(expected =  DocumentSubmissionException.class)
	public void testException() throws DocumentSubmissionException {
		service.getTokenFromDocument(null, "Russia-Trump: FBI chief Wray defends agency", false);
	}

	@Test
	public void testGetTokenFromDocument() throws FileNotFoundException, DocumentSubmissionException, InterruptedException {

		Mockito.doCallRealMethod().when(messageHandler).setDocument(Mockito.any());
		Mockito.doCallRealMethod().when(messageHandler).handleMessage(Mockito.any());
		Mockito.doCallRealMethod().when(messageHandler).setDocHandlingService(Mockito.any());
		
		messageHandler.setDocHandlingService(docHandlingService);

		Mockito.when(messageHandler.isOk(Mockito.any())).thenReturn(true);
		InputStream is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));				
		String token = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", false);
		assertNotNull(token);
		service.storeDocumentAndQueueForProcessing();
		synchronized (service) {
			service.wait(5000);
		}
		simulateDocProcessing(token);
		is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
		String token1 = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", true);
		assertEquals(token, token1);
		Document d = dao.findDocumentByToken(token);
		assertEquals(d.getDocumentStatus(), DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS);		
		
		Mockito.when(messageHandler.isOk(Mockito.any())).thenReturn(true);
		is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
		token = service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", true);
		assertNotNull(token);
		service.storeDocumentAndQueueForProcessing();
		synchronized (service) {
			service.wait(5000);
		}		
		simulateDocProcessing(token);
		is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
		token1 = service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", true);
		assertEquals(token, token1);
		d = dao.findDocumentByToken(token);
		assertEquals(d.getDocumentStatus(), DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS);		
		
		Mockito.when(messageHandler.isOk(Mockito.any())).thenReturn(false);
		is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
		token = service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", false);
		assertNotNull(token);
		service.storeDocumentAndQueueForProcessing();
		synchronized (service) {
			service.wait(5000);
		}
		d = dao.findDocumentByToken(token);
		assertEquals(d.getDocumentStatus(), DocumentStatus.QUEUED_FOR_INDEXING_ERROR);
		
		synchronized (queueService) {
			queueService.wait(3000);
		}
		
	}

	private void simulateDocProcessing(String token) {
		Document d = null;
		while (d == null) d = dao.findDocumentByToken(token);
		assertNotNull(d);
		d.addKeyword(new Keyword("test", 0.5d));
		d.addSentence(new Sentence("test sentence", 0.6d, 1));
		dao.storeDocument(d);
	}

}

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
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class DocumentSubmissionServiceImplTest {
	
	@Autowired
	DocumentQueueService queueService;
	
	@InjectMocks
	@Autowired
	DocumentSubmissionService service;
	
	@Autowired(required=true)
	private MongoDocumentDao dao;
	
	@Mock
	DocumentEnqueueResponseMessageHandler messageHandler;
	
	private static boolean clean =  false;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		if (!clean) {
			dao.clearDocuments();
			clean = true;
		}
	}

	@Test
	public void testGetTokenFromDocument() throws FileNotFoundException, DocumentSubmissionException, InterruptedException {

		Mockito.doCallRealMethod().when(messageHandler).setDocument(Mockito.any());
		Mockito.doCallRealMethod().when(messageHandler).handleMessage(Mockito.any());

		InputStream is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));				
		String token = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", false);
		assertNotNull(token);
		service.storeDocumentAndQueueForProcessing();
		simulateDocProcessing(token);
		is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
		String token1 = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", true);
		assertEquals(token, token1);
		
		is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
		token = service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", true);
		assertNotNull(token);
		service.storeDocumentAndQueueForProcessing();
		simulateDocProcessing(token);
		is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
		token1 = service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", true);
		assertEquals(token, token1);
		
		synchronized (queueService) {
			queueService.wait(3000);
		}
		
	}

	private void simulateDocProcessing(String token) {
		Document d = dao.findDocumentByToken(token);
		assertNotNull(d);
		d.addKeyword(new Keyword("test", 0.5d));
		d.addSentence(new Sentence("test sentence", 0.6d, 1));
		dao.storeDocument(d);
	}

}

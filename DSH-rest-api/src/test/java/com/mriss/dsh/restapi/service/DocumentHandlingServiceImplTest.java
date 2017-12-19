package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
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
import org.springframework.test.context.junit4.SpringRunner;

import com.mriss.dsh.data.document.dao.mongo.MongoDocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentHandlingServiceImplTest {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentHandlingServiceImplTest.class);

	@Autowired(required=true)
	@Qualifier("docForTest1")
	private Document docForTest1;	
	
	@Autowired(required=true)
	private MongoDocumentDao dao;
	
	@Autowired(required=true)
	private DocumentHandlingService service;	

	private static boolean clean =  false;

	@Before
	public void before() {
		if (!clean) {
			dao.clearDocuments();
			clean = true;
		}
	}

	@Test
	public void testStoreDocument() {
		String id = service.storeDocument(docForTest1);
		assertNotNull(id);
		Document d = dao.findDocumentById(id);
		assertNotNull(d);
	}

	@Test
	public void testGetDocumentByHash() {
		String id = service.storeDocument(docForTest1);
		assertNotNull(id);
		Document d = service.getDocumentByHash(docForTest1.getFileHash());
		assertNull(d);
		docForTest1.addKeyword(new Keyword("test", 5.0));
		docForTest1.addSentence(new Sentence("testee", 4.0, 1));
		id = service.storeDocument(docForTest1);
		assertNotNull(id);
		d = service.getDocumentByHash(docForTest1.getFileHash());
		assertNotNull(d);		
	}

}

@Configuration
class DocumentHandlingServiceImplTestConfiguration {
	
	@Bean(name="docForTest1")
	public Document getDocumentWithTitleAndContents() throws Exception {
		return new Document(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))), "Russia-Trump: FBI chief Wray defends agency");
	}
	
}

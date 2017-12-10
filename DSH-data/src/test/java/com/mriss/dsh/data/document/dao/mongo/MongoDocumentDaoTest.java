package com.mriss.dsh.data.document.dao.mongo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;

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
import org.springframework.test.context.junit4.SpringRunner;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MongoDocumentDaoTest {
	
	final static Logger logger = LoggerFactory.getLogger(MongoDocumentDaoTest.class);

	@Autowired(required=true)
	@Qualifier("docTitleConstructor")
	private Document docTitleConstructor;	
	
	@Autowired(required=true)
	private MongoDocumentDao dao;

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
		assertNotNull(docTitleConstructor);
		for(int i = 1; i <= 5; i++) {
			Keyword k = new Keyword("test"+i, ((double)i) + 0.1);
			docTitleConstructor.addKeyword(k);
		}
		for(int i = 1; i <= 5; i++) {
			Sentence s = new Sentence("test"+i, ((double)i) + 0.21, i+1);
			docTitleConstructor.addSentence(s);
		}		
		String id = dao.storeDocument(docTitleConstructor);
		assertNotNull(id);
	}

	@Test
	public void testFindDocumentById() {
		assertNotNull(docTitleConstructor);
		for(int i = 1; i <= 5; i++) {
			Keyword k = new Keyword("test"+i, ((double)i) + 0.1);
			docTitleConstructor.addKeyword(k);
		}
		for(int i = 1; i <= 5; i++) {
			Sentence s = new Sentence("test"+i, ((double)i) + 0.21, i+1);
			docTitleConstructor.addSentence(s);
		}		
		String id = dao.storeDocument(docTitleConstructor);
		assertNotNull(id);
		Document d = dao.findDocumentById(id);
		assertNotNull(d);
		assertTrue(d.equals(docTitleConstructor));
		logger.info("document(): " + d);
		assertNull(dao.findDocumentById("398j"));
	}

	@Test
	public void testFindDocumentsByHash() {
		assertNotNull(docTitleConstructor);
		for(int i = 1; i <= 5; i++) {
			Keyword k = new Keyword("test"+i, ((double)i) + 0.1);
			docTitleConstructor.addKeyword(k);
		}
		for(int i = 1; i <= 5; i++) {
			Sentence s = new Sentence("test"+i, ((double)i) + 0.21, i+1);
			docTitleConstructor.addSentence(s);
		}		
		String id = dao.storeDocument(docTitleConstructor);
		assertNotNull(id);
		List<Document> docs = dao.findDocumentsByHash(docTitleConstructor.getFileHash());
		assertNotNull(docs);
		assertFalse(docs.isEmpty());
		for (Iterator<Document> iterator = docs.iterator(); iterator.hasNext();) {
			Document document = (Document) iterator.next();
			assertTrue(document.equals(docTitleConstructor));
		}
		docs = dao.findDocumentsByHash("1234");
		assertNotNull(docs);
		assertTrue(docs.isEmpty());
	}

	@Test
	public void testFindDocumentByToken() {
		assertNotNull(docTitleConstructor);
		for(int i = 1; i <= 5; i++) {
			Keyword k = new Keyword("test"+i, ((double)i) + 0.1);
			docTitleConstructor.addKeyword(k);
		}
		for(int i = 1; i <= 5; i++) {
			Sentence s = new Sentence("test"+i, ((double)i) + 0.21, i+1);
			docTitleConstructor.addSentence(s);
		}		
		String id = dao.storeDocument(docTitleConstructor);
		assertNotNull(id);
		Document d = dao.findDocumentByToken(docTitleConstructor.getToken());
		assertNotNull(d);
		assertTrue(d.equals(docTitleConstructor));
		d = dao.findDocumentByToken("1234");
		assertNull(d);
	}

}

class MongoDocumentDaoTestConfiguration {
	
	@Bean(name="docTitleConstructor")
	public Document getDocumentWithTitleAndContents() throws Exception {
		return new Document(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))), "Russia-Trump: FBI chief Wray defends agency");
	}
	
}
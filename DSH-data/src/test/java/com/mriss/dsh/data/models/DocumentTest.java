package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentTest {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentTest.class);

	@Autowired(required=true)
	@Qualifier("docTitleConstructor")
	private Document docTitleConstructor;
	
	@Autowired(required=true)
	@Qualifier("docTitleContentsConstructor")
	private Document docTitleContentsConstructor;
	
	@Autowired(required=true)
	@Qualifier("docTitleContentsFromStreamConstructor")
	private Document docTitleContentsFromStreamConstructor;	
	
	@Autowired(required=true)
	@Qualifier("anotherDocument")
	private Document anotherDocument;	

	@Test
	public void testHashCode() {
		assertNotNull(docTitleContentsFromStreamConstructor.hashCode());
		assertNotNull(docTitleContentsConstructor.hashCode());
		assertNotNull(docTitleConstructor.hashCode());
		
		logger.info("docTitleContentsFromStreamConstructor.hashCode(): " + docTitleContentsFromStreamConstructor.hashCode());
		logger.info("docTitleContentsConstructor.hashCode(): " + docTitleContentsConstructor.hashCode());
		logger.info("docTitleConstructor.hashCode(): " + docTitleConstructor.hashCode());
		
		assertEquals(docTitleContentsConstructor.hashCode(), docTitleConstructor.hashCode());
	}

	@Test
	public void testDocumentString() {
		assertNotNull(docTitleConstructor);
		assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleConstructor.getTitle());
	}

	@Test
	public void testDocumentByteArrayString() {
		assertNotNull(docTitleContentsConstructor);
		assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleContentsConstructor.getTitle());
	}

	@Test
	public void testDocumentInputStreamString() {
		assertNotNull(docTitleContentsFromStreamConstructor);
		assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleContentsFromStreamConstructor.getTitle());
	}

	@Test
	public void testGetToken() {
		assertNotNull(docTitleContentsFromStreamConstructor.getToken());
		assertNotNull(docTitleContentsConstructor.getToken());
		assertNotNull(docTitleConstructor.getToken());
		
		logger.info("docTitleContentsFromStreamConstructor.getToken(): " + docTitleContentsFromStreamConstructor.getToken());
		logger.info("docTitleContentsConstructor.getToken(): " + docTitleContentsConstructor.getToken());
		logger.info("docTitleConstructor.getToken(): " + docTitleConstructor.getToken());
	}

	@Test
	public void testGetFileHash() {
		assertNotNull(docTitleConstructor);
		assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleConstructor.getTitle());
		logger.info("docTitleConstructor.getFileHash(): " + docTitleConstructor.getFileHash());
		
		assertNotNull(docTitleContentsConstructor);
		assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleContentsConstructor.getTitle());
		logger.info("docTitleContentsConstructor.getFileHash(): " + docTitleContentsConstructor.getFileHash());
		
		assertEquals(docTitleConstructor.getFileHash(), docTitleContentsConstructor.getFileHash());
	}

	@Test
	public void testGetKeyWords() {
		Keyword k1 = new Keyword("test", 0.5);
		Keyword k2 = new Keyword("test1", 1.0);
		Keyword k3 = new Keyword("test2", 2.0);
		Keyword k4 = new Keyword("test2", 1.5);
		docTitleConstructor.addKeyword(k1);
		docTitleConstructor.addKeyword(k2);
		docTitleConstructor.addKeyword(k3);
		docTitleConstructor.addKeyword(k4);
		assertEquals("keyword score validation", 2.0, docTitleConstructor.getKeyWords().first().getScore(), 0.0);
		for (Iterator<Keyword> iterator = docTitleConstructor.getKeyWords().iterator(); iterator.hasNext();) {
			Keyword type = (Keyword) iterator.next();
			logger.info("docTitleConstructor.getKeyWords(): " + type);
		}
	}

	@Test
	public void testGetRelevantSentences() {
		Sentence s1 = new Sentence("test", 0.5, 1);
		Sentence s2 = new Sentence("test1", 1.0, 4);
		Sentence s3 = new Sentence("test2", 2.0, 2);
		Sentence s4 = new Sentence("test2", 1.5, 2);
		docTitleConstructor.addSentence(s1);
		docTitleConstructor.addSentence(s2);
		docTitleConstructor.addSentence(s3);
		docTitleConstructor.addSentence(s4);
		assertEquals("addSentence score validation", 2.0, docTitleConstructor.getRelevantSentences().first().getScore(), 0.0);
		for (Iterator<Sentence> iterator = docTitleConstructor.getRelevantSentences().iterator(); iterator.hasNext();) {
			Sentence type = (Sentence) iterator.next();
			logger.info("docTitleConstructor.getRelevantSentences(): " + type);
		}
	}

	@Test
	public void testGetOriginalFileContents() throws IOException {
		assertNotNull(docTitleConstructor);
		FileUtils.writeByteArrayToFile(new File("target/test-file.pdf"), docTitleConstructor.getOriginalFileContents());
		assertTrue(new File("target/test-file.pdf").exists());
		assertEquals(new File("target/test-classes/pdf/bbc-news-1.pdf").length(), new File("target/test-file.pdf").length());
	}

	@Test
	public void testEqualsObject() throws NoSuchAlgorithmException {
		assertNotNull(docTitleConstructor);
		assertNotNull(docTitleContentsConstructor);
		
		assertEquals(docTitleConstructor, docTitleContentsConstructor);
		assertTrue(docTitleConstructor.equals(docTitleConstructor));
		
		assertNotEquals(docTitleConstructor, anotherDocument);
		assertNotEquals(docTitleContentsConstructor, anotherDocument);
		assertNotEquals(docTitleContentsFromStreamConstructor, anotherDocument);
		
		assertNotEquals(docTitleConstructor, null);
		assertNotEquals(docTitleConstructor, new Object());
		
		// this object is different, since it does not have contents
		Document d = new Document("Russia-Trump: FBI chief Wray defends agency");
		assertFalse(docTitleConstructor.equals(d));
		assertFalse(d.equals(docTitleConstructor));
	}

}

@Configuration
class DocumentTestConfiguration {
	
	@Bean(name="docTitleConstructor")
	public Document getDocumentWithTitle() throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		Document d = new Document("Russia-Trump: FBI chief Wray defends agency");
		d.setOriginalFileContents(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))));
		return d;
	}
	
	@Bean(name="docTitleContentsConstructor")
	public Document getDocumentWithTitleAndContents() throws Exception {
		return new Document(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))), "Russia-Trump: FBI chief Wray defends agency");
	}
	
	@Bean(name="docTitleContentsFromStreamConstructor")
	public Document getDocumentWithTitleAndContentsFromStream() throws Exception {
		return new Document(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf")), "Russia-Trump: FBI chief Wray defends agency");
	}

	@Bean(name="anotherDocument")
	public Document getAnotherDocument() throws Exception {
		return new Document(new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf")), "Emails show Trump Tower meeting follow-up");
	}
}
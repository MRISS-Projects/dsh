package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Unit tests for {@link Document}.
 * Uses @ContextConfiguration to load only the test bean definitions,
 * avoiding the full Spring Boot autoconfiguration (no MongoDB required).
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DocumentTestConfiguration.class)
public class DocumentTest {

    final static Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    @Autowired(required = true)
    @Qualifier("docTitleConstructor")
    private Document docTitleConstructor;

    @Autowired(required = true)
    @Qualifier("docTitleContentsConstructor")
    private Document docTitleContentsConstructor;

    @Autowired(required = true)
    @Qualifier("docTitleContentsFromStreamConstructor")
    private Document docTitleContentsFromStreamConstructor;

    @Autowired(required = true)
    @Qualifier("anotherDocument")
    private Document anotherDocument;

    @Test
    public void testDocumentStatus() {
        assertEquals(DocumentStatus.CREATED, docTitleContentsFromStreamConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, docTitleContentsConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, docTitleConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, anotherDocument.getDocumentStatus());
    }

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
            Keyword type = iterator.next();
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
            Sentence type = iterator.next();
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

    @Test
    public void testTransitionStatus() throws Exception {
        Document d = new Document("Transition test document");
        assertEquals(DocumentStatus.CREATED, d.getDocumentStatus());
        assertNotNull(d.getDocumentStatusMessage());

        d.transitionStatus(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, d.getDocumentStatus());
        assertNotNull(d.getDocumentStatusMessage());

        d.transitionStatus(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS, d.getDocumentStatus());

        d.transitionStatus(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.DEQUEUED_FOR_INDEXING, d.getDocumentStatus());
    }

    @Test
    public void testToString() {
        assertNotNull(docTitleConstructor);
        String s = docTitleConstructor.toString();
        assertNotNull(s);
        assertTrue(s.contains("Russia-Trump"));
    }

    /** Covers the no-arg constructor and direct setters / getters. */
    @Test
    public void testNoArgConstructorAndSettersGetters() {
        Document d = new Document();
        assertNotNull(d);
        d.setId("some-id");
        assertEquals("some-id", d.getId());
    }

    /** Covers the full-args constructor. */
    @Test
    public void testFullArgsConstructor() {
        Document d = new Document("id1", "title1", "token1", "hash1",
                new TreeSet<>(KeywordSorter.getInstance()),
                new TreeSet<>(SentenceSorter.getInstance()),
                new byte[]{1, 2, 3},
                DocumentStatus.CREATED,
                DocumentStatus.CREATED.getStatusMessage());
        assertEquals("id1", d.getId());
        assertEquals("title1", d.getTitle());
        assertEquals("token1", d.getToken());
        assertEquals("hash1", d.getFileHash());
        assertEquals(DocumentStatus.CREATED, d.getDocumentStatus());
        assertNotNull(d.getDocumentStatusMessage());
        assertNotNull(d.getOriginalFileContents());
        assertNotNull(d.getKeyWords());
        assertNotNull(d.getRelevantSentences());
    }

    /** Covers Document.equals() when both documents have null fileHash. */
    @Test
    public void testEqualsWhenBothFileHashNull() throws NoSuchAlgorithmException {
        Document d1 = new Document("Title A");
        Document d2 = new Document("Title B");
        // Neither d1 nor d2 has had file contents set, so fileHash is null for both.
        assertTrue(d1.equals(d2));
    }

    /** Covers Document.hashCode() when fileHash is null. */
    @Test
    public void testHashCodeNullFileHash() throws NoSuchAlgorithmException {
        Document d = new Document("No contents");
        // Should not throw; prime * 1 + 0 = 31
        assertEquals(31, d.hashCode());
    }

    /** Covers Document.getNewFileHash() null branch via setOriginalFileContents(null). */
    @Test
    public void testSetOriginalFileContentsNull() throws NoSuchAlgorithmException {
        Document d = new Document("test");
        d.setOriginalFileContents(null);
        assertNull(d.getFileHash());
    }

}

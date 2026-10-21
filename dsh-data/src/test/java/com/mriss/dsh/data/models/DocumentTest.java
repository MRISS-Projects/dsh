package com.mriss.dsh.data.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link Document}.
 * Fixtures are rebuilt before every test, so no test sees another's mutations.
 */
public class DocumentTest {

    final static Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    private static final String TITLE = "Russia-Trump: FBI chief Wray defends agency";

    private static final String BBC_PDF = "target/test-classes/pdf/bbc-news-1.pdf";

    private Document docAllArgsConstructor;

    private Document docTitleContentsConstructor;

    private Document docTitleContentsFromStreamConstructor;

    private Document anotherDocument;

    @BeforeEach
    public void setUp() throws Exception {
        docAllArgsConstructor = documentWithAllArgs();
        docTitleContentsConstructor = documentWithTitleAndContents();
        docTitleContentsFromStreamConstructor = documentWithTitleAndContentsFromStream();
        anotherDocument = anotherDocument();
    }

    private static Document documentWithAllArgs() throws NoSuchAlgorithmException, IOException {
        return new Document(null, TITLE, "token-of-all-args",
                new TreeSet<>(KeywordSorter.getInstance()),
                new TreeSet<>(SentenceSorter.getInstance()),
                FileUtils.readFileToByteArray(new File(BBC_PDF)),
                DocumentStatus.CREATED,
                DocumentStatus.CREATED.getStatusMessage());
    }

    private static Document documentWithTitleAndContents() throws Exception {
        return new Document(FileUtils.readFileToByteArray(new File(BBC_PDF)), TITLE);
    }

    private static Document documentWithTitleAndContentsFromStream() throws Exception {
        try (InputStream is = new FileInputStream(new File(BBC_PDF))) {
            return new Document(is, TITLE);
        }
    }

    private static Document anotherDocument() throws Exception {
        try (InputStream is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"))) {
            return new Document(is, "Emails show Trump Tower meeting follow-up");
        }
    }

    @Test
    public void testDocumentStatus() {
        assertEquals(DocumentStatus.CREATED, docTitleContentsFromStreamConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, docTitleContentsConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, docAllArgsConstructor.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED, anotherDocument.getDocumentStatus());
    }

    @Test
    public void testHashCode() {
        assertNotNull(docTitleContentsFromStreamConstructor.hashCode());
        assertNotNull(docTitleContentsConstructor.hashCode());
        assertNotNull(docAllArgsConstructor.hashCode());

        logger.info("docTitleContentsFromStreamConstructor.hashCode(): " + docTitleContentsFromStreamConstructor.hashCode());
        logger.info("docTitleContentsConstructor.hashCode(): " + docTitleContentsConstructor.hashCode());
        logger.info("docAllArgsConstructor.hashCode(): " + docAllArgsConstructor.hashCode());

        assertEquals(docTitleContentsConstructor.hashCode(), docAllArgsConstructor.hashCode());
    }

    @Test
    public void testDocumentString() {
        assertNotNull(docAllArgsConstructor);
        assertEquals("Russia-Trump: FBI chief Wray defends agency", docAllArgsConstructor.getTitle());
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
        assertNotNull(docAllArgsConstructor.getToken());

        logger.info("docTitleContentsFromStreamConstructor.getToken(): " + docTitleContentsFromStreamConstructor.getToken());
        logger.info("docTitleContentsConstructor.getToken(): " + docTitleContentsConstructor.getToken());
        logger.info("docAllArgsConstructor.getToken(): " + docAllArgsConstructor.getToken());
    }

    @Test
    public void testGetFileHash() {
        assertNotNull(docAllArgsConstructor);
        assertEquals("Russia-Trump: FBI chief Wray defends agency", docAllArgsConstructor.getTitle());
        logger.info("docAllArgsConstructor.getFileHash(): " + docAllArgsConstructor.getFileHash());

        assertNotNull(docTitleContentsConstructor);
        assertEquals("Russia-Trump: FBI chief Wray defends agency", docTitleContentsConstructor.getTitle());
        logger.info("docTitleContentsConstructor.getFileHash(): " + docTitleContentsConstructor.getFileHash());

        assertEquals(docAllArgsConstructor.getFileHash(), docTitleContentsConstructor.getFileHash());
    }

    @Test
    public void testGetKeyWords() {
        Keyword k1 = new Keyword("test", 0.5);
        Keyword k2 = new Keyword("test1", 1.0);
        Keyword k3 = new Keyword("test2", 2.0);
        Keyword k4 = new Keyword("test2", 1.5);
        docAllArgsConstructor.addKeyword(k1);
        docAllArgsConstructor.addKeyword(k2);
        docAllArgsConstructor.addKeyword(k3);
        docAllArgsConstructor.addKeyword(k4);
        assertEquals(2.0, docAllArgsConstructor.getKeyWords().first().getScore(), 0.0, "keyword score validation");
        for (Iterator<Keyword> iterator = docAllArgsConstructor.getKeyWords().iterator(); iterator.hasNext();) {
            Keyword type = iterator.next();
            logger.info("docAllArgsConstructor.getKeyWords(): " + type);
        }
    }

    @Test
    public void testGetRelevantSentences() {
        Sentence s1 = new Sentence("test", 0.5, 1);
        Sentence s2 = new Sentence("test1", 1.0, 4);
        Sentence s3 = new Sentence("test2", 2.0, 2);
        Sentence s4 = new Sentence("test2", 1.5, 2);
        docAllArgsConstructor.addSentence(s1);
        docAllArgsConstructor.addSentence(s2);
        docAllArgsConstructor.addSentence(s3);
        docAllArgsConstructor.addSentence(s4);
        assertEquals(2.0, docAllArgsConstructor.getRelevantSentences().first().getScore(), 0.0, "addSentence score validation");
        for (Iterator<Sentence> iterator = docAllArgsConstructor.getRelevantSentences().iterator(); iterator.hasNext();) {
            Sentence type = iterator.next();
            logger.info("docAllArgsConstructor.getRelevantSentences(): " + type);
        }
    }

    @Test
    public void testGetOriginalFileContents() throws IOException {
        assertNotNull(docAllArgsConstructor);
        FileUtils.writeByteArrayToFile(new File("target/test-file.pdf"), docAllArgsConstructor.getOriginalFileContents());
        assertTrue(new File("target/test-file.pdf").exists());
        assertEquals(new File("target/test-classes/pdf/bbc-news-1.pdf").length(), new File("target/test-file.pdf").length());
    }

    @Test
    public void testEqualsObject() throws NoSuchAlgorithmException {
        assertNotNull(docAllArgsConstructor);
        assertNotNull(docTitleContentsConstructor);

        assertEquals(docAllArgsConstructor, docTitleContentsConstructor);
        assertTrue(docAllArgsConstructor.equals(docAllArgsConstructor));

        assertNotEquals(docAllArgsConstructor, anotherDocument);
        assertNotEquals(docTitleContentsConstructor, anotherDocument);
        assertNotEquals(docTitleContentsFromStreamConstructor, anotherDocument);

        assertNotEquals(docAllArgsConstructor, null);
        assertNotEquals(docAllArgsConstructor, new Object());

        // this object is different, since it does not have contents
        Document d = new Document("Russia-Trump: FBI chief Wray defends agency");
        assertFalse(docAllArgsConstructor.equals(d));
        assertFalse(d.equals(docAllArgsConstructor));
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

    /** A document queued again starts over: NEUTRAL from a later status would not queue it. */
    @Test
    public void testResetStatus() throws Exception {
        Document d = new Document("Reset test document");
        d.transitionStatus(TransitionType.NEUTRAL);
        d.transitionStatus(TransitionType.ERROR);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_ERROR, d.getDocumentStatus());

        d.resetStatus();

        assertEquals(DocumentStatus.CREATED, d.getDocumentStatus());
        assertEquals(DocumentStatus.CREATED.getStatusMessage(), d.getDocumentStatusMessage());
        d.transitionStatus(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, d.getDocumentStatus());
    }

    @Test
    public void testToString() {
        assertNotNull(docAllArgsConstructor);
        String s = docAllArgsConstructor.toString();
        assertNotNull(s);
        assertTrue(s.contains("Russia-Trump"));
    }

    /** Covers the id setter and getter. */
    @Test
    public void testSetId() throws NoSuchAlgorithmException {
        Document d = new Document(TITLE);
        d.setId("some-id");
        assertEquals("some-id", d.getId());
    }

    /** Covers the full-args constructor: the file hash is the SHA-256 of the contents given. */
    @Test
    public void testFullArgsConstructor() throws NoSuchAlgorithmException {
        Document d = new Document("id1", "title1", "token1",
                new TreeSet<>(KeywordSorter.getInstance()),
                new TreeSet<>(SentenceSorter.getInstance()),
                new byte[]{1, 2, 3},
                DocumentStatus.CREATED,
                DocumentStatus.CREATED.getStatusMessage());
        assertEquals("id1", d.getId());
        assertEquals("title1", d.getTitle());
        assertEquals("token1", d.getToken());
        assertEquals("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81", d.getFileHash());
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

    /** A document built from a title alone has no contents, so no file hash. */
    @Test
    public void testTitleConstructorHasNoFileHash() throws NoSuchAlgorithmException {
        Document d = new Document("test");
        assertNull(d.getOriginalFileContents());
        assertNull(d.getFileHash());
    }

}

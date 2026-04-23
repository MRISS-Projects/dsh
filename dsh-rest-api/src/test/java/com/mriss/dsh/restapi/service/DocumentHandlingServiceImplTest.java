package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

/**
 * Tests for {@link DocumentHandlingServiceImpl}.
 * <p>
 * MongoDB ({@link DocumentDao}) and RabbitMQ ({@link DocumentQueueService}) are
 * mocked because both infrastructures are deprecated for this project.
 * The real service logic is still exercised via the mocked DAO.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentHandlingServiceImplTest {

    final static Logger logger = LoggerFactory.getLogger(DocumentHandlingServiceImplTest.class);

    /** Mock MongoDB DAO – no real MongoDB instance required. */
    @MockBean
    private DocumentDao dao;

    /** Mock RabbitMQ queue service – no broker required. */
    @MockBean
    private DocumentQueueService documentQueueService;

    @Autowired
    private DocumentHandlingService service;

    private Document docForTest1;

    @Before
    public void before() throws Exception {
        Mockito.reset(dao);
        docForTest1 = new Document(
                IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))),
                "Russia-Trump: FBI chief Wray defends agency");
    }

    /**
     * Verifies that {@code storeDocument} delegates to the DAO and returns the
     * generated id.
     */
    @Test
    public void testStoreDocument() {
        String expectedId = "mock-doc-id-001";
        when(dao.storeDocument(any(Document.class))).thenReturn(expectedId);

        String id = service.storeDocument(docForTest1);

        assertNotNull("Returned id must not be null", id);
        verify(dao).storeDocument(docForTest1);
    }

    /**
     * Verifies the two branches of {@code getDocumentByHash}:
     * <ol>
     *   <li>No matching document → returns {@code null}.</li>
     *   <li>A document with keywords AND sentences exists → returns that document.</li>
     * </ol>
     */
    @Test
    public void testGetDocumentByHash() {
        String hash = docForTest1.getFileHash();

        // Branch 1 – empty list → null
        when(dao.findDocumentsByHash(hash)).thenReturn(Collections.emptyList());
        Document result = service.getDocumentByHash(hash);
        assertNull("Should return null when no document matches", result);

        // Branch 1b – null list → null
        when(dao.findDocumentsByHash(hash)).thenReturn(null);
        result = service.getDocumentByHash(hash);
        assertNull("Should return null when dao returns null", result);

        // Branch 2 – document without keywords/sentences → still null
        when(dao.findDocumentsByHash(hash)).thenReturn(Arrays.asList(docForTest1));
        result = service.getDocumentByHash(hash);
        assertNull("Should return null when document has no keywords/sentences", result);

        // Branch 2b – document with keywords but no sentences → still null
        docForTest1.addKeyword(new Keyword("test", 5.0));
        when(dao.findDocumentsByHash(hash)).thenReturn(Arrays.asList(docForTest1));
        result = service.getDocumentByHash(hash);
        assertNull("Should return null when document has keywords but no sentences", result);

        // Branch 3 – document with both keywords and sentences → found
        docForTest1.addKeyword(new Keyword("test", 5.0));
        docForTest1.addSentence(new Sentence("testee", 4.0, 1));
        when(dao.findDocumentsByHash(hash)).thenReturn(Arrays.asList(docForTest1));
        result = service.getDocumentByHash(hash);
        assertNotNull("Should return document when it has keywords and sentences", result);
    }

    /**
     * Verifies that {@code getDocumentByToken} delegates to the DAO.
     */
    @Test
    public void testGetDocumentByToken() {
        String token = docForTest1.getToken();
        when(dao.findDocumentByToken(token)).thenReturn(docForTest1);

        Document result = service.getDocumentByToken(token);

        assertNotNull(result);
        verify(dao).findDocumentByToken(token);
    }
}

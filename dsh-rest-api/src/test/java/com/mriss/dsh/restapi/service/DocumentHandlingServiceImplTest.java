package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;

/**
 * Tests for {@link DocumentHandlingServiceImpl}.
 * <p>
 * The document store ({@link DocumentDao}) is mocked; the real service logic is exercised
 * through the mocked DAO, with no Spring context.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentHandlingServiceImplTest {

    final static Logger logger = LoggerFactory.getLogger(DocumentHandlingServiceImplTest.class);

    /** Mock document store. */
    @Mock
    private DocumentDao dao;

    @InjectMocks
    private DocumentHandlingServiceImpl service;

    private Document docForTest1;

    @Before
    public void before() throws Exception {
        docForTest1 = new Document(
                FileUtils.readFileToByteArray(new File("target/test-classes/pdf/bbc-news-1.pdf")),
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
     * Verifies that {@code getDocumentByHash} delegates to the DAO:
     * <ol>
     *   <li>No matching document → returns {@code null}.</li>
     *   <li>A document has that hash → returns it, processed or not.</li>
     * </ol>
     */
    @Test
    public void testGetDocumentByHash() {
        String hash = docForTest1.getFileHash();

        // No document → null
        when(dao.findDocumentByHash(hash)).thenReturn(null);
        Document result = service.getDocumentByHash(hash);
        assertNull("Should return null when no document matches", result);

        // A document not processed yet is still the document with that hash
        when(dao.findDocumentByHash(hash)).thenReturn(docForTest1);
        result = service.getDocumentByHash(hash);
        assertSame("Should return the document whatever its keywords and sentences", docForTest1, result);
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

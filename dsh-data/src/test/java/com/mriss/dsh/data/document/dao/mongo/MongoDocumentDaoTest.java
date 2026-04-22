package com.mriss.dsh.data.document.dao.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;

/**
 * Unit tests for {@link MongoDocumentDao}.
 * {@link DocumentRepository} is fully mocked – no MongoDB instance required.
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoDocumentDaoTest {

    final static Logger logger = LoggerFactory.getLogger(MongoDocumentDaoTest.class);

    @Mock
    private DocumentRepository repository;

    @InjectMocks
    private MongoDocumentDao dao;

    private Document docTitleConstructor;

    @Before
    public void before() throws Exception {
        docTitleConstructor = new Document(
                IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))),
                "Russia-Trump: FBI chief Wray defends agency");
        docTitleConstructor.setId("test-id");

        // Stub save: return the same document (id already set above)
        when(repository.save(any(Document.class))).thenReturn(docTitleConstructor);

        // Stub findById
        when(repository.findById("test-id")).thenReturn(Optional.of(docTitleConstructor));
        when(repository.findById("398j")).thenReturn(Optional.empty());

        // Stub findByFileHash
        when(repository.findByFileHash(docTitleConstructor.getFileHash()))
                .thenReturn(Arrays.asList(docTitleConstructor));
        when(repository.findByFileHash("1234")).thenReturn(Collections.emptyList());

        // Stub findByToken
        when(repository.findByToken(docTitleConstructor.getToken())).thenReturn(docTitleConstructor);
        when(repository.findByToken("1234")).thenReturn(null);
    }

    @Test
    public void testStoreDocument() {
        for (int i = 1; i <= 5; i++) {
            docTitleConstructor.addKeyword(new Keyword("test" + i, ((double) i) + 0.1));
        }
        for (int i = 1; i <= 5; i++) {
            docTitleConstructor.addSentence(new Sentence("test" + i, ((double) i) + 0.21, i + 1));
        }
        String id = dao.storeDocument(docTitleConstructor);
        assertNotNull(id);
    }

    @Test
    public void testFindDocumentById() {
        String id = dao.storeDocument(docTitleConstructor);
        assertNotNull(id);

        Document d = dao.findDocumentById(id);
        assertNotNull(d);
        assertTrue(d.equals(docTitleConstructor));
        assertEquals(DocumentStatus.CREATED, d.getDocumentStatus());
        logger.info("document(): " + d);

        d = dao.findDocumentById("398j");
        assertNull(d);
    }

    @Test
    public void testFindDocumentsByHash() {
        String id = dao.storeDocument(docTitleConstructor);
        assertNotNull(id);

        List<Document> docs = dao.findDocumentsByHash(docTitleConstructor.getFileHash());
        assertNotNull(docs);
        assertFalse(docs.isEmpty());
        for (Iterator<Document> iterator = docs.iterator(); iterator.hasNext();) {
            Document document = iterator.next();
            assertTrue(document.equals(docTitleConstructor));
            assertEquals(DocumentStatus.CREATED, document.getDocumentStatus());
        }

        docs = dao.findDocumentsByHash("1234");
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    public void testClearDocuments() {
        // Should not throw
        dao.clearDocuments();
    }

    @Test
    public void testFindDocumentByToken() {
        String id = dao.storeDocument(docTitleConstructor);
        assertNotNull(id);

        Document d = dao.findDocumentByToken(docTitleConstructor.getToken());
        assertNotNull(d);
        assertEquals(DocumentStatus.CREATED, d.getDocumentStatus());
        assertTrue(d.equals(docTitleConstructor));

        d = dao.findDocumentByToken("1234");
        assertNull(d);
    }
}



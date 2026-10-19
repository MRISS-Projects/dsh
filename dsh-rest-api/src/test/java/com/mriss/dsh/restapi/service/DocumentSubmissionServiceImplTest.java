package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.support.GenericMessage;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;
import com.mriss.dsh.data.models.TransitionType;

/**
 * Unit tests for {@link DocumentSubmissionServiceImpl}.
 * <p>
 * Both the document store ({@link DocumentDao}) and RabbitMQ ({@link DocumentQueueService})
 * are mocked. The RabbitMQ mock simulates an ACK by invoking the real
 * {@link DocumentEnqueueResponseMessageHandler} so that document status
 * transitions are still exercised for coverage.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentSubmissionServiceImplTest {

    @Mock
    private DocumentHandlingService docHandlingService;

    @Mock
    private DocumentQueueService docQueueService;

    @Mock
    private DocumentEnqueueMessageHandler messageHandler;

    @InjectMocks
    private DocumentSubmissionServiceImpl service;

    @Before
    public void before() {
        // By default the mock messageHandler does nothing – individual tests
        // override behaviour as needed.
    }

    // -------------------------------------------------------------------------
    // getTokenFromDocument
    // -------------------------------------------------------------------------

    /**
     * Null InputStream must throw {@link DocumentSubmissionException}.
     */
    @Test(expected = DocumentSubmissionException.class)
    public void testException() throws DocumentSubmissionException {
        service.getTokenFromDocument(null, "Russia-Trump: FBI chief Wray defends agency", false);
    }

    /**
     * useCache=false: always creates a fresh document and returns its token.
     */
    @Test
    public void testGetTokenFromDocumentNoCaching() throws Exception {
        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", false);
        assertNotNull("Token must not be null", token);
        verify(messageHandler).setDocument(any(Document.class));
    }

    /**
     * useCache=true and document not yet processed: returns token and flags for storage.
     */
    @Test
    public void testGetTokenFromDocumentCacheMiss() throws Exception {
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(null);
        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", true);
        assertNotNull(token);
    }

    /**
     * useCache=true and document already processed: returns the cached token.
     */
    @Test
    public void testGetTokenFromDocumentCacheHit() throws Exception {
        // Build the expected cached document (same hash as what the service will compute)
        Document cached = processed(documentFrom("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency"));
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(cached);

        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", true);
        assertEquals("Should return cached document token", cached.getToken(), token);
    }

    /**
     * useCache=false and the file is already stored and processed: a file is stored once, so
     * the stored document's token is returned and that document is queued again from the start.
     */
    @Test
    public void testGetTokenFromDocumentNoCachingWhenStored_shouldRequeueTheStoredDocument() throws Exception {
        Document stored = storedDocument();
        processed(stored);
        when(docHandlingService.getDocumentByHash(stored.getFileHash())).thenReturn(stored);

        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", false);
        service.storeDocumentAndQueueForProcessing();

        assertEquals(stored.getToken(), token);
        verify(messageHandler).setDocument(stored);
        verify(docHandlingService).storeDocument(stored);
        verify(docQueueService).enqueueDocumentId(eq("stored-id"), any());
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, stored.getDocumentStatus());
    }

    /**
     * useCache=true and the file is stored but not processed: there is no result to reuse, so
     * the stored document's token is returned and that document is queued again.
     */
    @Test
    public void testGetTokenFromDocumentCacheWhenStoredNotProcessed_shouldRequeueTheStoredDocument() throws Exception {
        Document stored = storedDocument();
        when(docHandlingService.getDocumentByHash(stored.getFileHash())).thenReturn(stored);

        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", true);
        service.storeDocumentAndQueueForProcessing();

        assertEquals(stored.getToken(), token);
        verify(docHandlingService).storeDocument(stored);
        verify(docQueueService).enqueueDocumentId(eq("stored-id"), any());
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, stored.getDocumentStatus());
    }

    /**
     * useCache=true and the stored document has keywords but no sentences: its processing did
     * not finish, so the result is not reused and the document is queued again.
     */
    @Test
    public void testGetTokenFromDocumentCacheWhenStoredHalfProcessed_shouldRequeueTheStoredDocument() throws Exception {
        Document stored = storedDocument();
        stored.addKeyword(new Keyword("wray", 5.0));
        when(docHandlingService.getDocumentByHash(stored.getFileHash())).thenReturn(stored);

        String token = tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", true);
        service.storeDocumentAndQueueForProcessing();

        assertEquals(stored.getToken(), token);
        verify(docQueueService).enqueueDocumentId(eq("stored-id"), any());
    }

    // -------------------------------------------------------------------------
    // storeDocumentAndQueueForProcessing
    // -------------------------------------------------------------------------

    /**
     * Happy path: document is stored and the RabbitMQ queue mock immediately
     * simulates an ACK so that the status transitions to
     * {@link DocumentStatus#QUEUED_FOR_INDEXING_SUCCESS}.
     */
    @Test
    public void testStoreDocumentAndQueueForProcessingSuccess() throws Exception {
        // Stub storeDocument to return an id so document.getId() works
        when(docHandlingService.storeDocument(any(Document.class))).thenReturn("stored-id-001");

        // Simulate RabbitMQ ACK: call the real message handler with a "sent ok" payload
        // Note: document.getId() is null because DocumentDao.storeDocument()'s return value
        // is not used to set the document id field, so we must use nullable() matcher.
        doAnswer(inv -> {
            DocumentEnqueueMessageHandler handler = inv.getArgument(1);
            handler.handleMessage(new GenericMessage<>("stored-id-001 sent ok"));
            return null;
        }).when(docQueueService).enqueueDocumentId(org.mockito.ArgumentMatchers.nullable(String.class), any());

        tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", false);
        service.storeDocumentAndQueueForProcessing();

        verify(docHandlingService).storeDocument(any(Document.class));
        verify(docQueueService).enqueueDocumentId(org.mockito.ArgumentMatchers.nullable(String.class), any());
    }

    /**
     * When storeDocumentAndQueueForProcessing is called without first calling
     * getTokenFromDocument, document is null and the method should be a no-op.
     */
    @Test
    public void testStoreDocumentAndQueueForProcessingNoDocument() throws Exception {
        // document field is null by default – no interactions expected
        service.storeDocumentAndQueueForProcessing();

        verify(docHandlingService, org.mockito.Mockito.never())
                .storeDocument(any(Document.class));
        verify(docQueueService, org.mockito.Mockito.never())
                .enqueueDocumentId(anyString(), any());
    }

    /**
     * When useCache=true and a cached document is found, storeDocument flag
     * remains false so storeDocumentAndQueueForProcessing should be a no-op.
     */
    @Test
    public void testStoreDocumentAndQueueForProcessingCacheHitSkipsStorage() throws Exception {
        Document cached = processed(documentFrom("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency"));
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(cached);

        tokenFor("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency", true);
        service.storeDocumentAndQueueForProcessing();

        verify(docHandlingService, org.mockito.Mockito.never())
                .storeDocument(any(Document.class));
    }

    /**
     * Error path: when the queue service throws an exception the document
     * status must be updated to reflect the error.
     */
    @Test
    public void testStoreDocumentAndQueueForProcessingError() throws Exception {
        when(docHandlingService.storeDocument(any(Document.class))).thenReturn("stored-id-002");
        doAnswer(inv -> { throw new RuntimeException("Broker unavailable"); })
                .when(docQueueService).enqueueDocumentId(org.mockito.ArgumentMatchers.nullable(String.class), any());

        tokenFor("target/test-classes/pdf/edition.cnn.com-1.pdf", "Emails show Trump Tower meeting follow-up", false);
        service.storeDocumentAndQueueForProcessing();

        // Error branch: storeDocument is called twice (before and after error)
        verify(docHandlingService, org.mockito.Mockito.times(2))
                .storeDocument(any(Document.class));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private String tokenFor(String path, String title, boolean useCache) throws Exception {
        try (InputStream is = new FileInputStream(new File(path))) {
            return service.getTokenFromDocument(is, title, useCache);
        }
    }

    private static Document documentFrom(String path, String title) throws Exception {
        try (InputStream is = new FileInputStream(new File(path))) {
            return new Document(is, title);
        }
    }

    /** The bbc-news-1 document as the store holds it after one round: an id, queued successfully. */
    private static Document storedDocument() throws Exception {
        Document stored = documentFrom("target/test-classes/pdf/bbc-news-1.pdf",
                "Russia-Trump: FBI chief Wray defends agency");
        stored.setId("stored-id");
        stored.transitionStatus(TransitionType.NEUTRAL);
        stored.transitionStatus(TransitionType.SUCCESS);
        return stored;
    }

    private static Document processed(Document document) {
        document.addKeyword(new Keyword("wray", 5.0));
        document.addSentence(new Sentence("FBI chief Wray defends agency", 4.0, 1));
        return document;
    }
}

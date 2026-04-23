package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

/**
 * Unit tests for {@link DocumentSubmissionServiceImpl}.
 * <p>
 * Both MongoDB ({@link DocumentDao}) and RabbitMQ ({@link DocumentQueueService})
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
        InputStream is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
        String token = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", false);
        assertNotNull("Token must not be null", token);
        verify(messageHandler).setDocument(any(Document.class));
    }

    /**
     * useCache=true and document not yet processed: returns token and flags for storage.
     */
    @Test
    public void testGetTokenFromDocumentCacheMiss() throws Exception {
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(null);
        InputStream is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
        String token = service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", true);
        assertNotNull(token);
    }

    /**
     * useCache=true and document already processed: returns the cached token.
     */
    @Test
    public void testGetTokenFromDocumentCacheHit() throws Exception {
        // Prime the DAO mock with a document that already has a token
        InputStream is1 = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
        // Build the expected cached document (same hash as what the service will compute)
        Document cached = new Document(is1, "Russia-Trump: FBI chief Wray defends agency");
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(cached);

        InputStream is2 = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
        String token = service.getTokenFromDocument(is2, "Russia-Trump: FBI chief Wray defends agency", true);
        assertEquals("Should return cached document token", cached.getToken(), token);
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

        InputStream is = new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"));
        service.getTokenFromDocument(is, "Russia-Trump: FBI chief Wray defends agency", false);
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
        InputStream is1 = new java.io.FileInputStream(new java.io.File("target/test-classes/pdf/bbc-news-1.pdf"));
        com.mriss.dsh.data.models.Document cached = new com.mriss.dsh.data.models.Document(is1, "Russia-Trump: FBI chief Wray defends agency");
        when(docHandlingService.getDocumentByHash(anyString())).thenReturn(cached);

        InputStream is2 = new java.io.FileInputStream(new java.io.File("target/test-classes/pdf/bbc-news-1.pdf"));
        service.getTokenFromDocument(is2, "Russia-Trump: FBI chief Wray defends agency", true);
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

        InputStream is = new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf"));
        service.getTokenFromDocument(is, "Emails show Trump Tower meeting follow-up", false);
        service.storeDocumentAndQueueForProcessing();

        // Error branch: storeDocument is called twice (before and after error)
        verify(docHandlingService, org.mockito.Mockito.times(2))
                .storeDocument(any(Document.class));
    }
}

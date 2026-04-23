package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.data.models.TransitionType;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DocumentEnqueueResponseMessageHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentEnqueueResponseMessageHandlerTest {

    @Mock
    private DocumentHandlingService docHandlingService;

    @Mock
    private SubscribableChannel channel;

    @InjectMocks
    private DocumentEnqueueResponseMessageHandler handler;

    private Document document;

    @Before
    public void setUp() throws Exception {
        document = new Document(
                new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf")),
                "Russia-Trump: FBI chief Wray defends agency");
        // Advance to QUEUED_FOR_INDEXING so SUCCESS/ERROR transitions work
        document.transitionStatus(TransitionType.NEUTRAL);
        handler.setDocument(document);
    }

    /**
     * When payload contains "sent ok", status transitions to SUCCESS.
     */
    @Test
    public void testHandleMessageSuccess() {
        handler.handleMessage(new GenericMessage<>("doc-id sent ok"));

        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS, document.getDocumentStatus());
        verify(docHandlingService).storeDocument(document);
        verify(channel).unsubscribe(handler);
    }

    /**
     * When payload does NOT contain "sent ok", status transitions to ERROR.
     */
    @Test
    public void testHandleMessageError() {
        handler.handleMessage(new GenericMessage<>("doc-id failed"));

        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_ERROR, document.getDocumentStatus());
        verify(docHandlingService).storeDocument(document);
        verify(channel).unsubscribe(handler);
    }

    /**
     * When docHandlingService is null, storeDocument must NOT be called (null guard).
     */
    @Test
    public void testHandleMessageNullDocHandlingService() {
        handler.setDocHandlingService(null);

        handler.handleMessage(new GenericMessage<>("doc-id sent ok"));

        // No NullPointerException; storeDocument never called
        verify(docHandlingService, never()).storeDocument(any());
    }

    /**
     * When channel is null (e.g. not wired), the unsubscribe call must be skipped.
     */
    @Test
    public void testHandleMessageNullChannel() throws Exception {
        // Use reflection to set the private channel field to null
        java.lang.reflect.Field f = DocumentEnqueueResponseMessageHandler.class.getDeclaredField("channel");
        f.setAccessible(true);
        f.set(handler, null);

        // Must not throw NullPointerException
        handler.handleMessage(new GenericMessage<>("doc-id sent ok"));

        verify(channel, never()).unsubscribe(any());
    }

    /**
     * Verifies the isOk helper method directly.
     */
    @Test
    public void testIsOk() {
        assertTrue(handler.isOk(new GenericMessage<>("abc sent ok")));
        assertFalse(handler.isOk(new GenericMessage<>("abc failed")));
    }
}





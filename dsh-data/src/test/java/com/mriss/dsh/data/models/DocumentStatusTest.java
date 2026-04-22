package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DocumentStatusTest {

    @Test
    public void testGetStatusDescription() {
        assertNotNull(DocumentStatus.CREATED.getStatusDescription());
        assertEquals("CREATED", DocumentStatus.CREATED.getStatusDescription());
        assertNotNull(DocumentStatus.INDEXED_SUCCESS.getStatusDescription());
    }

    @Test
    public void testGetStatusMessage() {
        assertNotNull(DocumentStatus.CREATED.getStatusMessage());
        assertNotNull(DocumentStatus.SENTENCES_PROCESSED_SUCCESS.getStatusMessage());
        assertNotNull(DocumentStatus.QUEUED_FOR_INDEXING.getStatusMessage());
    }

    @Test
    public void testTransition() {

        DocumentStatus status = DocumentStatus.CREATED;
        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, status);

        // NEUTRAL on QUEUED_FOR_INDEXING returns self
        DocumentStatus neutral = status.transition(TransitionType.NEUTRAL);
        assertSame(status, neutral);

        status = status.transition(TransitionType.ERROR);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_ERROR, status);
        DocumentStatus status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);

        status = DocumentStatus.QUEUED_FOR_INDEXING;
        status = status.transition(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.DEQUEUED_FOR_INDEXING, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.INDEXING, status);

        // NEUTRAL on INDEXING returns self
        assertSame(DocumentStatus.INDEXING, DocumentStatus.INDEXING.transition(TransitionType.NEUTRAL));

        status = status.transition(TransitionType.ERROR);
        assertEquals(DocumentStatus.NOT_INDEXED_ERROR, status);
        status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);

        status = DocumentStatus.INDEXING;
        status = status.transition(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.INDEXED_SUCCESS, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.QUEUED_FOR_PROCESSING, status);

        // NEUTRAL on QUEUED_FOR_PROCESSING returns self
        assertSame(DocumentStatus.QUEUED_FOR_PROCESSING,
                DocumentStatus.QUEUED_FOR_PROCESSING.transition(TransitionType.NEUTRAL));

        status = status.transition(TransitionType.ERROR);
        assertEquals(DocumentStatus.QUEUED_FOR_PROCESSING_ERROR, status);
        status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);

        status = DocumentStatus.QUEUED_FOR_PROCESSING;
        status = status.transition(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.QUEUED_FOR_PROCESSING_SUCCESS, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.DEQUEUED_FOR_PROCESSING, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.PROCESSING_KEYWORDS, status);

        // NEUTRAL on PROCESSING_KEYWORDS returns self
        assertSame(DocumentStatus.PROCESSING_KEYWORDS,
                DocumentStatus.PROCESSING_KEYWORDS.transition(TransitionType.NEUTRAL));

        status = status.transition(TransitionType.ERROR);
        assertEquals(DocumentStatus.KEYWORDS_NOT_PROCESSED_ERROR, status);
        status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);

        status = DocumentStatus.PROCESSING_KEYWORDS;
        status = status.transition(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.KEYWORDS_PROCESSED_SUCCESS, status);

        status = status.transition(TransitionType.NEUTRAL);
        assertEquals(DocumentStatus.PROCESSING_SENTENCES, status);

        // NEUTRAL on PROCESSING_SENTENCES returns self
        assertSame(DocumentStatus.PROCESSING_SENTENCES,
                DocumentStatus.PROCESSING_SENTENCES.transition(TransitionType.NEUTRAL));

        status = status.transition(TransitionType.ERROR);
        assertEquals(DocumentStatus.SENTENCES_NOT_PROCESSED_ERROR, status);
        status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);

        status = DocumentStatus.PROCESSING_SENTENCES;
        status = status.transition(TransitionType.SUCCESS);
        assertEquals(DocumentStatus.SENTENCES_PROCESSED_SUCCESS, status);
        status1 = status.transition(TransitionType.NEUTRAL);
        assertTrue(status == status1);
    }
}

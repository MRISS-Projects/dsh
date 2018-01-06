package com.mriss.dsh.data.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class DocumentStatusTest {

	@Test
	public void testTransition() {
		
		DocumentStatus status = DocumentStatus.CREATED;
		status = status.transition(TransitionType.NEUTRAL);
		assertEquals(DocumentStatus.QUEUED_FOR_INDEXING, status);
		
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

		status = status.transition(TransitionType.ERROR);
		assertEquals(DocumentStatus.NOT_INDEXED_ERROR, status);
		status1 = status.transition(TransitionType.NEUTRAL);
		assertTrue(status == status1);
		
		status = DocumentStatus.INDEXING;
		status = status.transition(TransitionType.SUCCESS);
		assertEquals(DocumentStatus.INDEXED_SUCCESS, status);
		
		status = status.transition(TransitionType.NEUTRAL);
		assertEquals(DocumentStatus.QUEUED_FOR_PROCESSING, status);
		
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
		
		status = status.transition(TransitionType.ERROR);
		assertEquals(DocumentStatus.KEYWORDS_NOT_PROCESSED_ERROR, status);
		status1 = status.transition(TransitionType.NEUTRAL);
		assertTrue(status == status1);		
		
		status = DocumentStatus.PROCESSING_KEYWORDS;
		status = status.transition(TransitionType.SUCCESS);
		assertEquals(DocumentStatus.KEYWORDS_PROCESSED_SUCCESS, status);
		
		status = status.transition(TransitionType.NEUTRAL);
		assertEquals(DocumentStatus.PROCESSING_SENTENCES, status);
		
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

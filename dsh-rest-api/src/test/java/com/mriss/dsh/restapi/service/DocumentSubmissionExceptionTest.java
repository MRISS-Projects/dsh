package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.mriss.dsh.data.models.Document;

/**
 * Tests for all constructors and accessors of {@link DocumentSubmissionException}.
 */
public class DocumentSubmissionExceptionTest {

    @Test
    public void testNoArgConstructor() {
        DocumentSubmissionException ex = new DocumentSubmissionException();
        assertNotNull(ex);
        assertNull(ex.getMessage());
        assertNull(ex.getDocument());
    }

    @Test
    public void testMessageConstructor() {
        DocumentSubmissionException ex = new DocumentSubmissionException("some error");
        assertEquals("some error", ex.getMessage());
        assertNull(ex.getDocument());
    }

    @Test
    public void testCauseConstructor() {
        Throwable cause = new RuntimeException("root cause");
        DocumentSubmissionException ex = new DocumentSubmissionException(cause);
        assertEquals(cause, ex.getCause());
        assertNull(ex.getDocument());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        Throwable cause = new RuntimeException("root cause");
        DocumentSubmissionException ex = new DocumentSubmissionException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertNull(ex.getDocument());
    }

    @Test
    public void testMessageCauseAndDocumentConstructor() throws Exception {
        Throwable cause = new RuntimeException("root cause");
        Document doc = new Document("Test Title");
        DocumentSubmissionException ex = new DocumentSubmissionException("msg", cause, doc);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(doc, ex.getDocument());
    }

    @Test
    public void testFullConstructor() {
        Throwable cause = new RuntimeException("root cause");
        DocumentSubmissionException ex = new DocumentSubmissionException("msg", cause, true, true);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}


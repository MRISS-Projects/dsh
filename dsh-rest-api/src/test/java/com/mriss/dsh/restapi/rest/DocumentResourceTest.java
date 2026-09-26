package com.mriss.dsh.restapi.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.restapi.dto.DocumentStatusDto;
import com.mriss.dsh.restapi.dto.TokenDto;
import com.mriss.dsh.restapi.service.DocumentHandlingService;
import com.mriss.dsh.restapi.service.DocumentSubmissionException;
import com.mriss.dsh.restapi.service.DocumentSubmissionService;

/**
 * Unit tests for {@link DocumentResource}, with its services mocked and no
 * Spring context. The web wiring is exercised by
 * {@code integration.DocumentResourceIT}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentResourceTest {

    private static final String TITLE = "A title";

    @Mock
    private DocumentSubmissionService documentSubmissionService;

    @Mock
    private DocumentHandlingService documentHandlingService;

    @InjectMocks
    private DocumentResource resource;

    private final MockMultipartFile contents =
            new MockMultipartFile("contents", "doc.pdf", "application/pdf", new byte[] {1, 2, 3});

    @Test
    public void submitDocument_whenValid_returnsToken() throws Exception {
        when(documentSubmissionService.getTokenFromDocument(any(InputStream.class), eq(TITLE), eq(false)))
                .thenReturn("tok");

        TokenDto result = resource.submitDocument(null, TITLE, contents);

        assertEquals("tok", result.getToken());
        assertEquals("", result.getMessage());
        verify(documentSubmissionService).storeDocumentAndQueueForProcessing();
    }

    @Test
    public void submitDocument_whenTitleBlank_returnsError() throws Exception {
        TokenDto result = resource.submitDocument(null, "  ", contents);

        assertEquals("ERROR", result.getToken());
        assertTrue(result.getMessage().startsWith("Error submitting file: "));
        verifyNoInteractions(documentSubmissionService);
    }

    @Test
    public void submitDocument_whenContentsNull_returnsError() throws Exception {
        TokenDto result = resource.submitDocument(null, TITLE, null);

        assertEquals("ERROR", result.getToken());
        assertTrue(result.getMessage().startsWith("Error submitting file: "));
        verifyNoInteractions(documentSubmissionService);
    }

    @Test
    public void submitDocument_whenServiceThrows_returnsError() throws Exception {
        when(documentSubmissionService.getTokenFromDocument(any(InputStream.class), anyString(), anyBoolean()))
                .thenThrow(new DocumentSubmissionException("boom"));

        TokenDto result = resource.submitDocument(null, TITLE, contents);

        assertEquals("ERROR", result.getToken());
        assertEquals("Error submitting file: boom", result.getMessage());
        verify(documentSubmissionService, never()).storeDocumentAndQueueForProcessing();
    }

    @Test
    public void getStatus_whenTokenUnknown_returnsTokenNotFound() {
        when(documentHandlingService.getDocumentByToken("unknown")).thenReturn(null);

        DocumentStatusDto result = resource.getStatus(null, "unknown");

        assertEquals(DocumentResource.TOKEN_NOT_FOUND, result.getStatus());
        assertEquals(DocumentResource.TOKEN_NOT_FOUND_MESSAGE + "unknown", result.getMessage());
    }

    @Test
    public void getStatus_whenDocumentFound_returnsItsStatus() throws Exception {
        Document document = new Document("t");
        when(documentHandlingService.getDocumentByToken("known")).thenReturn(document);

        DocumentStatusDto result = resource.getStatus(null, "known");

        assertEquals(document.getDocumentStatus().getStatusDescription(), result.getStatus());
        assertEquals(document.getDocumentStatusMessage(), result.getMessage());
    }
}

package com.mriss.dsh.restapi.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.RequestContextFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.data.models.Keyword;
import com.mriss.dsh.data.models.Sentence;
import com.mriss.dsh.restapi.DshRestApplication;
import com.mriss.dsh.restapi.dto.TokenDto;
import com.mriss.dsh.restapi.service.DocumentQueueService;
import com.mriss.dsh.restapi.service.DocumentEnqueueMessageHandler;

/**
 * Integration tests for {@link DocumentResource} using Spring MockMvc.
 * <p>
 * MongoDB ({@link DocumentDao}) and RabbitMQ ({@link DocumentQueueService})
 * are replaced with Mockito mocks so no real infrastructure is required.
 * The RabbitMQ mock simulates an ACK to drive the document through its
 * full status lifecycle, exercising all production code paths.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:/abstract-web-request-context.xml"}, classes = {DshRestApplication.class})
public class DocumentResourceTest {

    final static Logger logger = LoggerFactory.getLogger(DocumentResourceTest.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final MediaType contentType = MediaType.APPLICATION_JSON;

    protected MockHttpServletRequest request;

    /** Mock MongoDB DAO – prevents any real MongoDB connection. */
    @MockBean
    private DocumentDao dao;

    /** Mock RabbitMQ queue service – prevents any real broker connection. */
    @MockBean
    private DocumentQueueService documentQueueService;

    @Autowired
    private File testFile1;

    @Autowired
    private File testFile2;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .dispatchOptions(true)
                .addFilter(new RequestContextFilter(), "/*")
                .build();

        // Default stub: storeDocument returns a generated id
        when(dao.storeDocument(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            return d.getId() != null ? d.getId() : "mock-stored-id";
        });

        // Default stub: simulate RabbitMQ ACK – handler receives "sent ok" message
        // document.getId() is null at enqueueing time, use nullable() matcher
        doAnswer(inv -> {
            DocumentEnqueueMessageHandler handler = inv.getArgument(1);
            handler.handleMessage(
                    new org.springframework.messaging.support.GenericMessage<>("id sent ok"));
            return null;
        }).when(documentQueueService).enqueueDocumentId(
                org.mockito.ArgumentMatchers.nullable(String.class), any());
    }

    // -------------------------------------------------------------------------
    // POST /v1/dsh/document/submit
    // -------------------------------------------------------------------------

    /**
     * A valid document submission must return a non-null token and message.
     * The RabbitMQ ACK mock drives the document to QUEUED_FOR_INDEXING_SUCCESS.
     */
    @Test
    public void testSubmitDocument() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "contents", "bbc-news-1.pdf", "application/pdf",
                new FileInputStream(testFile1));

        startRequest();

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/dsh/document/submit")
                        .file(firstFile)
                        .param("title", "Russia-Trump: FBI chief Wray defends agency"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("token", notNullValue()))
                .andExpect(jsonPath("message", notNullValue()))
                .andReturn();

        // Allow async storeDocumentAndQueueForProcessing to complete
        synchronized (this) { this.wait(2000); }

        endRequest();

        String body = result.getResponse().getContentAsString();
        logger.info("Submit response: {}", body);
        TokenDto tDto = new ObjectMapper().readerFor(TokenDto.class).readValue(body);

        // Stub findDocumentByToken so status endpoint also works in follow-up call
        Document stored = new Document(
                new FileInputStream(testFile1), "Russia-Trump: FBI chief Wray defends agency");
        stored.transitionStatus(com.mriss.dsh.data.models.TransitionType.SUCCESS);
        when(dao.findDocumentByToken(tDto.getToken())).thenReturn(stored);
    }

    /**
     * Submitting with a blank title must return an error token.
     */
    @Test
    public void testError() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "contents", "bbc-news-1.pdf", "application/pdf",
                new FileInputStream(testFile1));

        startRequest();

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/dsh/document/submit")
                        .file(firstFile)
                        .param("title", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("token", notNullValue()))
                .andExpect(jsonPath("message", notNullValue()))
                .andReturn();

        synchronized (this) { this.wait(1000); }
        endRequest();

        logger.info("Error response: {}", result.getResponse().getContentAsString());
    }

    // -------------------------------------------------------------------------
    // GET /v1/dsh/document/status/{token}
    // -------------------------------------------------------------------------

    /**
     * Status endpoint with a known token must return the document status.
     */
    @Test
    public void testStatus() throws Exception {
        // Submit a document first to obtain a token
        MockMultipartFile firstFile = new MockMultipartFile(
                "contents", "edition.cnn.com-1.pdf", "application/pdf",
                new FileInputStream(testFile2));

        startRequest();
        MvcResult submitResult = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/dsh/document/submit")
                        .file(firstFile)
                        .param("title", "Emails show Trump Tower meeting follow-up"))
                .andExpect(status().isOk())
                .andReturn();
        synchronized (this) { this.wait(2000); }
        endRequest();

        String body = submitResult.getResponse().getContentAsString();
        TokenDto tDto = new ObjectMapper().readerFor(TokenDto.class).readValue(body);

        // Prepare a document in success state for the status check
        Document d = new Document(new FileInputStream(testFile2),
                "Emails show Trump Tower meeting follow-up");
        d.addKeyword(new Keyword("test", 0.5));
        d.addSentence(new Sentence("test sentence", 0.6, 1));
        d.transitionStatus(com.mriss.dsh.data.models.TransitionType.NEUTRAL);
        d.transitionStatus(com.mriss.dsh.data.models.TransitionType.SUCCESS);
        when(dao.findDocumentByToken(tDto.getToken())).thenReturn(d);

        startRequest();
        mockMvc.perform(get("/v1/dsh/document/status/" + tDto.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("status",
                        is(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS.getStatusDescription())))
                .andExpect(jsonPath("message",
                        is(DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS.getStatusMessage())))
                .andReturn();
        endRequest();
    }

    /**
     * Status endpoint with an unknown token must return TOKEN_NOT_FOUND.
     */
    @Test
    public void testStatusTokenNotFound() throws Exception {
        when(dao.findDocumentByToken("12344353")).thenReturn(null);

        startRequest();
        mockMvc.perform(get("/v1/dsh/document/status/12344353"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("status", is(DocumentResource.TOKEN_NOT_FOUND)))
                .andExpect(jsonPath("message",
                        is(DocumentResource.TOKEN_NOT_FOUND_MESSAGE + "12344353")))
                .andReturn();
        endRequest();
    }

    /**
     * Submitting with null contents (no file part) must return an error token.
     */
    @Test
    public void testErrorNullContents() throws Exception {
        // An empty-byte file IS provided so Spring binds it, but Document ctor will fail
        // on zero-length content → triggers the catch-block error return.
        MockMultipartFile emptyFile = new MockMultipartFile(
                "contents", "empty.pdf", "application/pdf", new byte[0]);

        startRequest();

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/v1/dsh/document/submit")
                        .file(emptyFile)
                        .param("title", "Some valid title"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("token", notNullValue()))
                .andReturn();

        endRequest();

        logger.info("Empty-contents response: {}", result.getResponse().getContentAsString());
    }

    /**
     * Direct unit test of submitDocument with null MultipartFile to cover the
     * {@code contents == null} branch in {@code validateParameters}.
     */
    @Test
    public void testSubmitDocumentNullContents() throws Exception {
        com.mriss.dsh.restapi.service.DocumentSubmissionService mockSubmissionService =
                org.mockito.Mockito.mock(com.mriss.dsh.restapi.service.DocumentSubmissionService.class);
        com.mriss.dsh.restapi.service.DocumentHandlingService mockHandlingService =
                org.mockito.Mockito.mock(com.mriss.dsh.restapi.service.DocumentHandlingService.class);

        DocumentResource resource = new DocumentResource();
        // Inject mocks via reflection
        setField(resource, "documentSubmissionService", mockSubmissionService);
        setField(resource, "documentHandlingService", mockHandlingService);

        TokenDto result = resource.submitDocument(new MockHttpServletRequest(), "Valid Title", null);

        org.junit.Assert.assertEquals("ERROR", result.getToken());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    protected void startRequest() {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request), true);
    }

    protected void endRequest() {
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).requestCompleted();
        RequestContextHolder.resetRequestAttributes();
        request = null;
    }
}

@Configuration
class DocumentResourceTestTestConfiguration {

    @Bean(name = "testFile1")
    public File getTestFile1() {
        return new File("target/test-classes/pdf/bbc-news-1.pdf");
    }

    @Bean(name = "testFile2")
    public File getTestFile2() {
        return new File("target/test-classes/pdf/edition.cnn.com-1.pdf");
    }
}

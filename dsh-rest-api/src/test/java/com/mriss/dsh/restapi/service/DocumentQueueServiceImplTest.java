package com.mriss.dsh.restapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

/**
 * Unit tests for {@link DocumentQueueServiceImpl}.
 * <p>
 * RabbitMQ is mocked at the Spring Integration channel level: both
 * {@code toRabbit} ({@link MessageChannel}) and {@code toRabbitResponse}
 * ({@link SubscribableChannel}) are replaced with Mockito mocks so no
 * real broker connection is required.
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentQueueServiceImplTest {

    /** Mock for the outbound Spring Integration channel (was backed by RabbitMQ). */
    @Mock
    private MessageChannel rabbitChannel;

    /** Mock for the inbound publish-subscribe response channel. */
    @Mock
    private SubscribableChannel toRabbitResponse;

    /** Mock handler passed by callers to receive the enqueue acknowledgement. */
    @Mock
    private MessageHandler messageHandler;

    @InjectMocks
    private DocumentQueueServiceImpl service;

    @Before
    public void setUp() {
        // Ensure a clean state before each test.
    }

    /**
     * Verifies that {@link DocumentQueueServiceImpl#enqueueDocumentId} subscribes
     * the supplied handler to the response channel and sends a message to the
     * outbound channel.
     */
    @Test
    public void testEnqueueDocumentId() {
        service.enqueueDocumentId("doc-id-abc", messageHandler);

        verify(toRabbitResponse).subscribe(messageHandler);
        verify(rabbitChannel).send(any());
    }

    /**
     * Verifies that {@link DocumentQueueServiceImpl#processEnqueueingResponse}
     * wraps the payload in a {@link Message} and returns it.
     */
    @Test
    public void testProcessEnqueueingResponse() {
        String payload = "doc-id-abc sent ok";
        Message<String> result = service.processEnqueueingResponse(payload);

        assertNotNull("Result message must not be null", result);
        assertEquals("Payload must be preserved", payload, result.getPayload());
    }
}

package com.mriss.dsh.restapi.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class DocumentQueueServiceImplTest {
	
	@Autowired
	DocumentQueueService service;
	
	@Mock
	DocumentEnqueueMessageHandler messageHandler;

	@Test
	public void testEnqueueDocumentId() throws InterruptedException {
		service.enqueueDocumentId("234d232e3e3", messageHandler);
		synchronized (service) {
			service.wait();
		}
		verify(messageHandler).handleMessage(any());
	}

}

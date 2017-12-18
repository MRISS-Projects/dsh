package com.mriss.dsh.restapi.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentQueueServiceImplTest {
	
	@Autowired
	DocumentQueueService service;

	@Test
	public void testEnqueueDocumentId() throws InterruptedException {
		service.enqueueDocumentId("234d232e3e3");
		synchronized (service) {
			service.wait();
		}
	}

}

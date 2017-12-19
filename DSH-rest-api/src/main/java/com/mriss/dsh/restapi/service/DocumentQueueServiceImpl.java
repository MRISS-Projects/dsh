package com.mriss.dsh.restapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

@Component
public class DocumentQueueServiceImpl implements DocumentQueueService {
	
	@Autowired
	@Qualifier("toRabbit")
	private MessageChannel rabbitChannel;

	@Override
	public void enqueueDocumentId(String documentId) {
		rabbitChannel.send(new GenericMessage<String>(documentId));
	}
	
	//TODO move this to the DocumentSubmissionService implementation class
	@ServiceActivator (inputChannel = "toRabbitResponse", outputChannel="loggingChannel")
	public Message<String> processEnqueueingResponse(String payload) {
		synchronized (this) {
			this.notifyAll();
		}
		return new GenericMessage<String>(payload);
	}

}

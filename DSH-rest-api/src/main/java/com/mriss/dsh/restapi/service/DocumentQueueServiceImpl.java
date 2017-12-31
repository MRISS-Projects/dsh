package com.mriss.dsh.restapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

@Service
public class DocumentQueueServiceImpl implements DocumentQueueService {
	
	@Autowired
	@Qualifier("toRabbit")
	private MessageChannel rabbitChannel;

	@Autowired
	@Qualifier("toRabbitResponse")
	private SubscribableChannel toRabbitResponse;

	@Override
	public void enqueueDocumentId(String documentId, MessageHandler messageHandler) {
		this.toRabbitResponse.subscribe(messageHandler);
		rabbitChannel.send(new GenericMessage<String>(documentId));
	}
	
	@ServiceActivator (inputChannel = "toRabbitResponse", outputChannel="loggingChannel")
	public Message<String> processEnqueueingResponse(String payload) {
		synchronized (this) {
			this.notifyAll();
		}
		return new GenericMessage<String>(payload);
	}

}

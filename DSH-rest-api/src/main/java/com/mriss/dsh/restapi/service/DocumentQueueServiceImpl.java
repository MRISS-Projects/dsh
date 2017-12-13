package com.mriss.dsh.restapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

public class DocumentQueueServiceImpl implements DocumentQueueService {
	
	@Autowired
	@Qualifier("toRabbit")
	private MessageChannel rabbitChannel;

	@Override
	public void enqueueDocumentId(String documentId) {
		rabbitChannel.send(new GenericMessage<String>(documentId));
	}

}

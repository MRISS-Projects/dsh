package com.mriss.dsh.restapi.service;

import org.springframework.messaging.MessageHandler;


public interface DocumentQueueService {
	
	public void enqueueDocumentId(String documentId, MessageHandler messageHandler);

}

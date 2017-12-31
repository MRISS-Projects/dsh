package com.mriss.dsh.restapi.service;

import org.springframework.messaging.MessageHandler;

import com.mriss.dsh.data.models.Document;

public interface DocumentEnqueueMessageHandler extends MessageHandler {
	
	void setDocument(Document document);

}

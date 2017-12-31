package com.mriss.dsh.restapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.mriss.dsh.data.models.Document;

@Component
@RequestScope(proxyMode=ScopedProxyMode.TARGET_CLASS)
public class DocumentEnqueueResponseMessageHandler implements DocumentEnqueueMessageHandler {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentEnqueueResponseMessageHandler.class);
	
	@Autowired
	private DocumentHandlingService docHandlingService;
	
	@Autowired
	@Qualifier("toRabbitResponse")
	private SubscribableChannel channel;
	
	private Document submittedDocument;

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		logger.info("Processing enqueue process response for message: " + message + ". Document: " + submittedDocument);
		//TODO handle error success/error of enqueue process, set the status at the document and store at the database.
		if (this.channel != null) this.channel.unsubscribe(this);
	}

	@Override
	public void setDocument(Document document) {
		logger.info("MessageHandler: " + this);
		this.submittedDocument = document;
	}

}

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
import com.mriss.dsh.data.models.TransitionType;

@Component
@RequestScope(proxyMode=ScopedProxyMode.TARGET_CLASS)
public class DocumentEnqueueResponseMessageHandler implements DocumentEnqueueMessageHandler {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentEnqueueResponseMessageHandler.class);
	
	@Autowired(required = true)
	private DocumentHandlingService docHandlingService;
	
	@Autowired
	@Qualifier("toRabbitResponse")
	private SubscribableChannel channel;
	
	private Document submittedDocument;

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		logger.info("Processing enqueue process response for message: " + message + ". Document: " + submittedDocument);
		if (isOk(message)) {
			submittedDocument.transitionStatus(TransitionType.SUCCESS);
		} else {
			submittedDocument.transitionStatus(TransitionType.ERROR);
		}
		if (docHandlingService != null) docHandlingService.storeDocument(submittedDocument);
		if (this.channel != null) this.channel.unsubscribe(this);
	}

	boolean isOk(Message<?> message) {
		return ((String) message.getPayload()).indexOf("sent ok") != -1;
	}

	@Override
	public void setDocument(Document document) {
		logger.info("MessageHandler: " + this);
		this.submittedDocument = document;
	}

	public void setDocHandlingService(DocumentHandlingService docHandlingService) {
		this.docHandlingService = docHandlingService;
	}

	
}

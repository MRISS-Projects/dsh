package com.mriss.dsh.restapi.service;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.TransitionType;

@Service
@RequestScope(proxyMode=ScopedProxyMode.TARGET_CLASS)
public class DocumentSubmissionServiceImpl implements DocumentSubmissionService {
	
	final static Logger logger = LoggerFactory.getLogger(DocumentSubmissionServiceImpl.class);
	
	private Document document;
	
	@Autowired
	private DocumentHandlingService docHandlingService;
	
	@Autowired
	private DocumentQueueService docQueueService;
	
	@Autowired
	private DocumentEnqueueMessageHandler messageHandler;

	private boolean storeDocument = false;
	
	@Override
	public String getTokenFromDocument(InputStream originalDocumentContents, String documentTitle,
			boolean useCache) throws DocumentSubmissionException {
		logger.info("DocumentSubmissionServiceImpl.this(): " + this);
		try {
			this.document = new Document(originalDocumentContents, documentTitle);
			Document stored = docHandlingService.getDocumentByHash(this.document.getFileHash());
			if (stored != null) {
				if (useCache && isProcessed(stored)) {
					return stored.getToken();
				}
				stored.resetStatus();
				this.document = stored;
			}
			messageHandler.setDocument(this.document);
			storeDocument = true;
			return this.document.getToken();
		} catch (Exception e) {
			try {
				throw new DocumentSubmissionException(e.getMessage(), e, this.document != null ? document : new Document(documentTitle));
			} catch (NoSuchAlgorithmException e1) {
				logger.error(e1.getMessage(), e1);
				return null;
			}
		}
	}

	@Override
	@Async
	public void storeDocumentAndQueueForProcessing() {
		if (document != null && storeDocument) {
			document.transitionStatus(TransitionType.NEUTRAL);
			docHandlingService.storeDocument(this.document);			
			try {
				docQueueService.enqueueDocumentId(document.getId(), messageHandler);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				document.transitionStatus(TransitionType.ERROR);
				docHandlingService.storeDocument(document);
			}
		}
		synchronized (this) {
			this.notifyAll();
		}
	}

	private static boolean isProcessed(Document document) {
		return !document.getKeyWords().isEmpty() && !document.getRelevantSentences().isEmpty();
	}

	public void setMessageHandler(DocumentEnqueueMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

}

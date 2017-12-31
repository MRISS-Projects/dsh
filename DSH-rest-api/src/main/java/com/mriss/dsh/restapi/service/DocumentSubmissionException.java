package com.mriss.dsh.restapi.service;

import com.mriss.dsh.data.models.Document;

public class DocumentSubmissionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Document document;

	public DocumentSubmissionException() {
		super();
	}

	public DocumentSubmissionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DocumentSubmissionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DocumentSubmissionException(String message, Throwable cause, Document document) {
		this(message, cause);
		this.document = document;
	}


	public DocumentSubmissionException(String message) {
		super(message);
	}

	public DocumentSubmissionException(Throwable cause) {
		super(cause);
	}

	public Document getDocument() {
		return document;
	}

	
}

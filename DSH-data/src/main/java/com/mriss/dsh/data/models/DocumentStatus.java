package com.mriss.dsh.data.models;

public enum DocumentStatus {
	
	CREATED("CREATED", "Document created and stored at the database.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return QUEUED_FOR_INDEXING;
		}
	},
	
	QUEUED_FOR_INDEXING("QUEUED_FOR_INDEXING", "Document id sent to the indexing queue to be processed.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			if (type == TransitionType.SUCCESS) return QUEUED_FOR_INDEXING_SUCCESS;
			else if (type == TransitionType.ERROR) return QUEUED_FOR_INDEXING_ERROR;
			else return this;
		}
	},
	
	QUEUED_FOR_INDEXING_SUCCESS("QUEUED_FOR_INDEXING_SUCCESS", "Document id successfuly queued for indexing.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return DEQUEUED_FOR_INDEXING;
		}
	},
	
	QUEUED_FOR_INDEXING_ERROR("QUEUED_FOR_INDEXING_ERROR", "Error queueing recently created document id to be indexed.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	},
	
	DEQUEUED_FOR_INDEXING("DEQUEUED_FOR_INDEXING", "Document dequeued for indexing.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return INDEXING;
		}
	},
	
	INDEXING("INDEXING", "Document under indexing operation.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			if (type == TransitionType.SUCCESS) return INDEXED_SUCCESS;
			else if (type == TransitionType.ERROR) return NOT_INDEXED_ERROR;
			else return this;
		}
	},
	
	INDEXED_SUCCESS("INDEXED_SUCCESS", "Document successfully indexed") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return QUEUED_FOR_PROCESSING;
		}
	},
	
	NOT_INDEXED_ERROR("DEQUEUED_FOR_INDEXING", "Document dequeued for indexing.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	},
	
	QUEUED_FOR_PROCESSING("QUEUED_FOR_PROCESSING", "Document id sent to the processing queue to have its keywords and sentences extracted.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			if (type == TransitionType.SUCCESS) return QUEUED_FOR_PROCESSING_SUCCESS;
			else if (type == TransitionType.ERROR) return QUEUED_FOR_PROCESSING_ERROR;
			else return this;
		}
	},
	
	QUEUED_FOR_PROCESSING_SUCCESS("QUEUED_FOR_PROCESSING_SUCCESS", "Document id successfuly queued for processing keywords and sentendes.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return DEQUEUED_FOR_PROCESSING;
		}
	},
	
	QUEUED_FOR_PROCESSING_ERROR("QUEUED_FOR_PROCESSING_ERROR", "Error queueing document for processing keywords and sentences.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	},
	
	DEQUEUED_FOR_PROCESSING("DEQUEUED_FOR_PROCESSING", "Document dequeued for processing keywords and sentences.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return PROCESSING_KEYWORDS;
		}
	},
	
	PROCESSING_KEYWORDS("PROCESSING_KEYWORDS", "Extracting document keyworkds.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			if (type == TransitionType.SUCCESS) return KEYWORDS_PROCESSED_SUCCESS;
			else if (type == TransitionType.ERROR) return KEYWORDS_NOT_PROCESSED_ERROR;
			else return this;
		}
	},
	
	KEYWORDS_PROCESSED_SUCCESS("KEYWORDS_PROCESSED_SUCCESS", "Keywords successfuly extracted.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return PROCESSING_SENTENCES;
		}
	},
	
	KEYWORDS_NOT_PROCESSED_ERROR("KEYWORDS_NOT_PROCESSED_ERROR", "Error extracting keywords.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	},
	
	PROCESSING_SENTENCES("PROCESSING_SENTENCES", "Extracting most relevant sentences.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			if (type == TransitionType.SUCCESS) return SENTENCES_PROCESSED_SUCCESS;
			else if (type == TransitionType.ERROR) return SENTENCES_NOT_PROCESSED_ERROR;
			else return this;
		}
	},
	
	SENTENCES_PROCESSED_SUCCESS("SENTENCES_PROCESSED_SUCCESS", "Most relevant sentences successfuly extracted.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	},
	
	SENTENCES_NOT_PROCESSED_ERROR("SENTENCES_NOT_PROCESSED_ERROR", "Error extracting most relevant sentences.") {
		@Override
		DocumentStatus transition(TransitionType type) {
			return this;
		}
	};
	
	private String statusDescription;
	private String statusMessage;

	DocumentStatus(String statusDescription, String statusMessage) {
		this.statusDescription = statusDescription;
		this.statusMessage = statusMessage;
	}

	public String getStatusDescription() {
		return statusDescription;
	}

	public String getStatusMessage() {
		return statusMessage;
	}
	
	abstract DocumentStatus transition(TransitionType type);
	
}

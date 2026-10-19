package com.mriss.dsh.data.document.dao.memory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;

/**
 * Keeps documents in this JVM's memory. The store of a run with no profile (ADR-003, section 8):
 * nothing has to be installed, and every document is lost when the application stops.
 * <p>
 * An id, a file hash and a token each belong to one document at most, so every lookup is a
 * single map read.
 */
@Component
@ConditionalOnProperty(name = "dsh.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryDocumentDao implements DocumentDao {

	private final Map<String, Document> documentsById = new ConcurrentHashMap<>();
	private final Map<String, String> idsByHash = new ConcurrentHashMap<>();
	private final Map<String, String> idsByToken = new ConcurrentHashMap<>();

	/**
	 * Gives a document with no id a new one, on the instance itself, as the submission flow
	 * expects. Storing a document again under its id replaces its record. A file hash or a token
	 * that belongs to another document is rejected with an {@link IllegalStateException}.
	 */
	@Override
	public synchronized String storeDocument(Document document) {
		if (document == null) {
			throw new IllegalArgumentException("document must not be null");
		}
		if (document.getFileHash() == null || document.getToken() == null) {
			throw new IllegalArgumentException("document must have a file hash and a token");
		}
		String id = document.getId();
		String owner = id != null && documentsById.containsKey(id) ? id : null;
		if (!Objects.equals(owner, idsByHash.get(document.getFileHash()))
				|| !Objects.equals(owner, idsByToken.get(document.getToken()))) {
			throw new IllegalStateException("another document already has this file hash or token, or this id: " + document);
		}
		if (id == null) {
			id = UUID.randomUUID().toString();
			document.setId(id);
		}
		documentsById.put(id, document);
		idsByHash.put(document.getFileHash(), id);
		idsByToken.put(document.getToken(), id);
		return id;
	}

	@Override
	public Document findDocumentById(String id) {
		return id == null ? null : documentsById.get(id);
	}

	@Override
	public Document findDocumentByHash(String documentHash) {
		return findByIndex(idsByHash, documentHash);
	}

	@Override
	public Document findDocumentByToken(String token) {
		return findByIndex(idsByToken, token);
	}

	@Override
	public synchronized void clearDocuments() {
		documentsById.clear();
		idsByHash.clear();
		idsByToken.clear();
	}

	private Document findByIndex(Map<String, String> index, String key) {
		String id = key == null ? null : index.get(key);
		return id == null ? null : documentsById.get(id);
	}
}

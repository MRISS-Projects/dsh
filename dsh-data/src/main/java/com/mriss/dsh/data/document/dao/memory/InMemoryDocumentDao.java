package com.mriss.dsh.data.document.dao.memory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;

/**
 * Keeps documents in this JVM's memory. The store of a run with no profile (ADR-003, section 8):
 * nothing has to be installed, and every document is lost when the application stops.
 */
@Component
@ConditionalOnProperty(name = "dsh.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryDocumentDao implements DocumentDao {

	private final Map<String, Document> documents = new ConcurrentHashMap<>();

	/**
	 * Gives a document with no id a new one, on the instance itself, as the submission flow
	 * expects. Storing an instance again replaces its record.
	 */
	@Override
	public String storeDocument(Document document) {
		if (document == null) {
			throw new IllegalArgumentException("document must not be null");
		}
		if (document.getId() == null) {
			document.setId(UUID.randomUUID().toString());
		}
		documents.put(document.getId(), document);
		return document.getId();
	}

	@Override
	public Document findDocumentById(String id) {
		return documents.get(id);
	}

	@Override
	public List<Document> findDocumentsByHash(String documentHash) {
		return documents.values().stream()
				.filter(document -> Objects.equals(documentHash, document.getFileHash()))
				.collect(Collectors.toList());
	}

	@Override
	public Document findDocumentByToken(String token) {
		return documents.values().stream()
				.filter(document -> Objects.equals(token, document.getToken()))
				.findFirst()
				.orElse(null);
	}

	@Override
	public void clearDocuments() {
		documents.clear();
	}
}

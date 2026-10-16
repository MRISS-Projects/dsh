package com.mriss.dsh.data.document.dao.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
	private final Map<String, Set<String>> documentIdsByHash = new ConcurrentHashMap<>();
	private final Map<String, String> documentIdByToken = new ConcurrentHashMap<>();
	private final Set<String> documentIdsWithoutHash = ConcurrentHashMap.newKeySet();
	private final Set<String> documentIdsWithoutToken = ConcurrentHashMap.newKeySet();
	private final Map<String, IndexKeys> indexKeysByDocumentId = new HashMap<>();

	/**
	 * Gives a document with no id a new one, on the instance itself, as the submission flow
	 * expects. Storing an instance again replaces its record.
	 */
	@Override
	public synchronized String storeDocument(Document document) {
		if (document == null) {
			throw new IllegalArgumentException("document must not be null");
		}
		if (document.getId() == null) {
			document.setId(UUID.randomUUID().toString());
		}
		String id = document.getId();
		IndexKeys oldKeys = indexKeysByDocumentId.put(id, new IndexKeys(document.getFileHash(), document.getToken()));
		if (oldKeys != null) {
			removeFromIndexes(id, oldKeys);
		}
		documents.put(id, document);
		addToIndexes(id, document.getFileHash(), document.getToken());
		return id;
	}

	@Override
	public Document findDocumentById(String id) {
		return documents.get(id);
	}

	@Override
	public List<Document> findDocumentsByHash(String documentHash) {
		Set<String> ids = documentHash == null ? documentIdsWithoutHash : documentIdsByHash.get(documentHash);
		if (ids == null) {
			return new ArrayList<>();
		}
		List<Document> matchingDocuments = new ArrayList<>();
		for (String id : ids) {
			Document document = documents.get(id);
			if (document != null && Objects.equals(documentHash, document.getFileHash())) {
				matchingDocuments.add(document);
			}
		}
		return matchingDocuments;
	}

	@Override
	public Document findDocumentByToken(String token) {
		if (token == null) {
			for (String id : documentIdsWithoutToken) {
				Document document = documents.get(id);
				if (document != null && document.getToken() == null) {
					return document;
				}
			}
			return null;
		}
		String id = documentIdByToken.get(token);
		Document document = id == null ? null : documents.get(id);
		return document != null && Objects.equals(token, document.getToken()) ? document : null;
	}

	@Override
	public synchronized void clearDocuments() {
		documents.clear();
		documentIdsByHash.clear();
		documentIdByToken.clear();
		documentIdsWithoutHash.clear();
		documentIdsWithoutToken.clear();
		indexKeysByDocumentId.clear();
	}

	private void addToIndexes(String id, String fileHash, String token) {
		if (fileHash == null) {
			documentIdsWithoutHash.add(id);
		} else {
			documentIdsByHash.computeIfAbsent(fileHash, key -> ConcurrentHashMap.newKeySet()).add(id);
		}
		if (token == null) {
			documentIdsWithoutToken.add(id);
		} else {
			documentIdByToken.put(token, id);
		}
	}

	private void removeFromIndexes(String id, IndexKeys keys) {
		if (keys.fileHash == null) {
			documentIdsWithoutHash.remove(id);
		} else {
			Set<String> ids = documentIdsByHash.get(keys.fileHash);
			if (ids != null) {
				ids.remove(id);
				if (ids.isEmpty()) {
					documentIdsByHash.remove(keys.fileHash, ids);
				}
			}
		}
		if (keys.token == null) {
			documentIdsWithoutToken.remove(id);
		} else {
			documentIdByToken.remove(keys.token, id);
		}
	}

	private static final class IndexKeys {
		private final String fileHash;
		private final String token;

		private IndexKeys(String fileHash, String token) {
			this.fileHash = fileHash;
			this.token = token;
		}
	}
}

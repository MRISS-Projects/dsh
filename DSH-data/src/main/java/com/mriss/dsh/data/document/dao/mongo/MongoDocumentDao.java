package com.mriss.dsh.data.document.dao.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;

@Component
public class MongoDocumentDao implements DocumentDao {
	
	@Autowired
	DocumentRepository repository;

	@Override
	public String storeDocument(Document document) {
		Document d = repository.save(document);
		if (!d.equals(Optional.empty()))
			return d.getId();
		else 
			return null;
	}

	@Override
	public Document findDocumentById(String id) {
		Optional<Document> d = repository.findById(id);
		if (!d.equals(Optional.empty()))
			return d.get();
		else 
			return null;
	}

	@Override
	public List<Document> findDocumentsByHash(String documentHash) {
		return repository.findByFileHash(documentHash);
	}

	@Override
	public Document findDocumentByToken(String token) {
		return repository.findByToken(token);
	}

	@Override
	public void clearDocuments() {
		repository.deleteAll();
	}

}

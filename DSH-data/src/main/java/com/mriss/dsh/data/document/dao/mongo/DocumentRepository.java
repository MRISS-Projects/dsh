	package com.mriss.dsh.data.document.dao.mongo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.Nullable;

import com.mriss.dsh.data.models.Document;

public interface DocumentRepository extends MongoRepository<Document, String> {
	
	@Nullable
	public Document findByToken(@Nullable String token);
	
	public List<Document> findByFileHash(@Nullable String fileHash);

}

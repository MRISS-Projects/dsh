package com.mriss.dsh.restapi.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.models.Document;

@Service
public class DocumentHandlingServiceImpl implements DocumentHandlingService {
	
	@Autowired
	private DocumentDao dao;

	@Override
	public String storeDocument(Document d) {
		return dao.storeDocument(d);
	}

	@Override
	public Document getDocumentByHash(String hash) {
		List<Document> documents = dao.findDocumentsByHash(hash);
		if (documents == null || documents.isEmpty()) return null;
		for (Document document : documents) {
			if (!document.getKeyWords().isEmpty() && !document.getRelevantSentences().isEmpty()) {
				return document;
			}
		}
		return null;
	}

}

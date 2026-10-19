package com.mriss.dsh.restapi.service;

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
		Document document = dao.findDocumentByHash(hash);
		return document != null && !document.getKeyWords().isEmpty()
				&& !document.getRelevantSentences().isEmpty() ? document : null;
	}

	@Override
	public Document getDocumentByToken(String token) {
		Document d = dao.findDocumentByToken(token);
		return d;
	}

	
}

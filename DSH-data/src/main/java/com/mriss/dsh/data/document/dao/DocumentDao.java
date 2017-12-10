package com.mriss.dsh.data.document.dao;

import java.util.List;

import com.mriss.dsh.data.models.Document;

/**
 * Spec for Data Access Object used to handle documents in the database.
 * @author riss
 *
 */
public interface DocumentDao {
	
	/**
	 * Write downn a document to the database
	 * @param document the document to add
	 * @return the id of the new added document
	 */
	String storeDocument(Document document);
	
	/**
	 * Gets a document by its internal unique id.
	 * @param id the unique internal database id fot a document
	 * @return the document
	 */
	Document findDocumentById(String id);
	
	/**
	 * Gets all documents for the given hash based on the original file binary contents
	 * @param documentHash the hash based on document original binary content
	 * @return the document
	 */
	List<Document> findDocumentsByHash(String documentHash);
	
	/**
	 * Gets a document by its generated processin token
	 * @param token the token to be used to search for the document
	 * @return the document
	 */
	Document findDocumentByToken(String token);
	
	/**
	 * Remove all documents from the database
	 */
	void clearDocuments();

}

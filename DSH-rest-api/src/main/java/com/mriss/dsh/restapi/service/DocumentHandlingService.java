package com.mriss.dsh.restapi.service;

import com.mriss.dsh.data.models.Document;

/**
 * Interface responsible to handle document operations called from the rest service layer.
 * 
 * @author riss
 *
 */
public interface DocumentHandlingService {
	
	/**
	 * Sores a document at the database
	 * 
	 * @param d document to be stored
	 * @return the database generated unique id of the new stored document
	 */
	public String storeDocument(Document d);
	
	/**
	 * Get document by hash code.
	 * 
	 * @param hash the documents hash based on its original binary contents
	 * @return the first document instance where the hash is the same
	 * and the document's kewords and relevant sentences collections are not
	 * empty
	 */
	public Document getDocumentByHash(String hash);
	
	public Document getDocumentByToken(String token);

}

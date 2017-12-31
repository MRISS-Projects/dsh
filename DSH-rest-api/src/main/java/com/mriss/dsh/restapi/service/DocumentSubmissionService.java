package com.mriss.dsh.restapi.service;

import java.io.InputStream;

import com.mriss.dsh.data.models.Document;

/**
 * This service should use other services to handle documents storage (database) and
 * document data queuing systems. If using fields to store document instances, classes
 * implementing this interface should be scoped as <code>request</code>.
 * 
 * @author riss
 */
public interface DocumentSubmissionService {
	
	/**
	 * Gets a token (new or already used) based from the original contents
	 * of a file. The method basically creates a new {@link Document} and
	 * return its token for other clients test about processing. The method
	 * tests if the document already exists in the database by using a unique
	 * hash generated from the original file binary contents. If so, and
	 * the flag <code>useCache</code> is set, that old token associated with
	 * the found document is returned.
	 * @param originalDocumentContents the document original contents 
	 * @param useCache if set to <code>true</code>, the method should test if
	 * the document already exists in the database using its hash based on its 
	 * original binary contents. If document exists and the keywords and sentences document's
	 * fields are not empty, then the old token for the old document should be
	 * returned. If set to <code>false</code> no test is executed and a new token 
	 * for a newly generated document should be returned.
	 * @param documentTitle the document title
	 * @return the token (new or old) of the document created (or retrieved from database)
	 * @see Document
	 */
	String getTokenFromDocument(InputStream originalDocumentContents, String documentTitle, boolean useCache) throws DocumentSubmissionException;
	
	/**
	 * This method should preferable be implemented to be asynchronous. It should use
	 * a previously created document to store it in the documents database and then
	 * enqueue the document's data for later processing by other processes. The method
	 * should also test if an instance of {@link Document} has the sentences and keywords
	 * collections not empty. If they are not empty, and cache settings are set,
	 * the method should do nothing.
	 * @see Document
	 */
	void storeDocumentAndQueueForProcessing() throws DocumentSubmissionException ;
	
}

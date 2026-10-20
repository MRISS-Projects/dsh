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
	 * of a file. A file is stored once: the method looks the document up by
	 * the unique hash of the file's binary contents. If it is not stored, a
	 * new {@link Document} is created and its token returned. If it is, the
	 * stored document's token is returned, and <code>useCache</code> decides
	 * whether the document is processed again.
	 * @param originalDocumentContents the document original contents
	 * @param useCache if set to <code>true</code> and the stored document's
	 * keywords and sentences are not empty, its result is reused and nothing
	 * is queued. Otherwise the stored document is put back at its first status
	 * and queued again by {@link #storeDocumentAndQueueForProcessing()}.
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

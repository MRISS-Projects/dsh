package com.mriss.dsh.data.models;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;

public class Document {
	
	final static Logger logger = LoggerFactory.getLogger(Document.class);
	
	@Id
    private String id;
	
	private String title;
	
	private String token;
	
	private String fileHash;
	
	private SortedSet<Keyword> keyWords;
	
	private SortedSet<Sentence> relevantSentences;
	
	private byte[] originalFileContents;
	
	public Document() {
		
	}

	public Document(String id, String title, String token, String fileHash,
			SortedSet<Keyword> keyWords, SortedSet<Sentence> relevantSentences,
			byte[] originalFileContents) {
		super();
		this.id = id;
		this.title = title;
		this.token = token;
		this.fileHash = fileHash;
		this.keyWords = keyWords;
		this.relevantSentences = relevantSentences;
		this.originalFileContents = originalFileContents;
	}

	public Document(String title) throws NoSuchAlgorithmException {
		this.title = title;
		token = getNewToken();
		keyWords = new TreeSet<Keyword>(KeywordSorter.getInstance());
		relevantSentences = new TreeSet<Sentence>(SentenceSorter.getInstance());			
	}
	
	public Document(byte[] originalFileContents, String title) throws Exception {
		this(title);
		this.originalFileContents = originalFileContents;
		fileHash = getNewFileHash();
	}
	
	public Document(InputStream fileContentsStream, String title) throws Exception {
		this(title);
		this.originalFileContents = IOUtils.toByteArray(fileContentsStream);
		fileHash = getNewFileHash();
	}

	private String getNewFileHash() throws Exception {
		if (this.originalFileContents != null) {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return String.format("%064x", new BigInteger(1, md.digest(this.originalFileContents)));
		} else return null;
	}

	private String getNewToken() {
		return new StringBuilder(new SimpleDateFormat("YYYYMMdd").format(new Date())).append(UUID.randomUUID().toString()).toString();
	}

	public String getToken() {
		return token;
	}

	public String getFileHash() {
		return fileHash;
	}

	public SortedSet<Keyword> getKeyWords() {
		return keyWords;
	}

	public SortedSet<Sentence> getRelevantSentences() {
		return relevantSentences;
	}

	public byte[] getOriginalFileContents() {
		return originalFileContents;
	}
	
	public void addKeyword(Keyword keyword) {
		this.keyWords.add(keyword);
	}
	
	public void addSentence(Sentence keyword) {
		this.relevantSentences.add(keyword);
	}

	public String getTitle() {
		return title;
	}

	public void setOriginalFileContents(byte[] originalFileContents) {
		this.originalFileContents = originalFileContents;
		try {
			this.fileHash = getNewFileHash();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHash == null) ? 0 : fileHash.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Document other = (Document) obj;
		if (fileHash == null) {
			if (other.fileHash != null)
				return false;
		} else if (!fileHash.equals(other.fileHash))
			return false;
		return true;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Document [id=" + id + ", title=" + title + ", token=" + token
				+ ", fileHash=" + fileHash + ", keyWords=" + keyWords
				+ ", relevantSentences=" + relevantSentences + "]";
	}
	
}

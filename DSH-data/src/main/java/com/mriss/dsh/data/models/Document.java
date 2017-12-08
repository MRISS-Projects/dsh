package com.mriss.dsh.data.models;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

public class Document {
	
	private String title;
	
	private String token;
	
	private String fileHash;
	
	private SortedSet<Keyword> keyWords;
	
	private SortedSet<Sentence> relevantSentences;
	
	private byte[] originalFileContents;	

	public Document(String title) throws NoSuchAlgorithmException {
		this.title = title;
		token = getNewToken();
		fileHash = getNewFileHash();
		keyWords = new TreeSet<Keyword>(KeywordSorter.getInstance());
		relevantSentences = new TreeSet<Sentence>(SentenceSorter.getInstance());			
	}
	
	public Document(byte[] originalFileContents, String title) throws NoSuchAlgorithmException {
		this(title);
		this.originalFileContents = originalFileContents; 
	}
	
	public Document(InputStream fileContentsStream, String title) throws IOException, NoSuchAlgorithmException {
		this(title);
		this.originalFileContents = IOUtils.toByteArray(fileContentsStream);
	}

	private String getNewFileHash() throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		return new String(md.digest(this.originalFileContents));
	}

	private String getNewToken() {
		return new StringBuilder(new SimpleDateFormat("YYYYMMdd").format(new Date())).append(System.currentTimeMillis()).toString();
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
	
	
	
	
	
}

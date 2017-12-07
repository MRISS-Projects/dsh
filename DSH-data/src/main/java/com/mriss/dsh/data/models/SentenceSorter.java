package com.mriss.dsh.data.models;

import java.util.Comparator;

public class SentenceSorter implements Comparator<Sentence> {
	
	private static SentenceSorter INSTANCE = null;
	
	private SentenceSorter() {
		
	}
	
	public static SentenceSorter getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SentenceSorter();
		}
		return INSTANCE;
	}

	@Override
	public int compare(Sentence o1, Sentence o2) {
		if (o1.getScore() > o2.getScore()) return -1;
		else if (o1.getScore() < o2.getScore()) return 1;
		else return 0;	
	}

}

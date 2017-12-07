package com.mriss.dsh.data.models;

import java.util.Comparator;

public class KeywordSorter implements Comparator<Keyword> {
	
	private static KeywordSorter INSTANCE = null;
	
	private KeywordSorter() {
		
	}
	
	public static KeywordSorter getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new KeywordSorter();
		}
		return INSTANCE;
	}

	@Override
	public int compare(Keyword o1, Keyword o2) {
		if (o1.getScore() > o2.getScore()) return -1;
		else if (o1.getScore() < o2.getScore()) return 1;
		else return 0;
	}

}

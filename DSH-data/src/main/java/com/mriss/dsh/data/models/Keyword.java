package com.mriss.dsh.data.models;

public class Keyword {
	
	private String value;
	
	private double score;
	
	public Keyword(String value, double score) {
		this.value = value;
		this.score = score;
	}

	public String getValue() {
		return value;
	}

	public double getScore() {
		return score;
	}

}

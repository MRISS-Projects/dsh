package com.mriss.dsh.data.models;

public class Sentence {
	
	private String value;
	
	private double score;
	
	private int paragraph;
	
	public Sentence(String value, double score, int paragraph) {
		this.value = value;
		this.score = score;
		this.paragraph = paragraph;
	}

	public String getValue() {
		return value;
	}

	public double getScore() {
		return score;
	}

	public int getParagraph() {
		return paragraph;
	}

	
}

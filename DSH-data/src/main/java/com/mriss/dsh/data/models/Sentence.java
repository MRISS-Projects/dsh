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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Sentence other = (Sentence) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	
	
}

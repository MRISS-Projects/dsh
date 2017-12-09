package com.mriss.dsh.data.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class SentenceTest {

	@Test
	public void testHashCode() {
		Sentence s1 = new Sentence("test", 0, 1);
		Sentence s2 = new Sentence("test", 0, 1);
		Sentence s3 = new Sentence("test1", 0, 1);
		Sentence s4 = new Sentence("test", 0, 2);		

		assertEquals(s1.hashCode(), s2.hashCode());
		assertNotEquals(s2.hashCode(), s3.hashCode());
		assertNotEquals(s2.hashCode(), s4.hashCode());
	}

	@Test
	public void testGetValue() {
		Sentence s1 = new Sentence("test", 0, 1);
		assertEquals("test", s1.getValue());
	}

	@Test
	public void testGetScore() {
		Sentence s1 = new Sentence("test", 0.1, 1);
		assertEquals("sentence score test", 0.1, s1.getScore(), 0.0);
	}

	@Test
	public void testGetParagraph() {
		Sentence s1 = new Sentence("test", 0.1, 1);
		assertEquals(1, s1.getParagraph());
	}

	@Test
	public void testEqualsObject() {
		Sentence s1 = new Sentence("test", 0, 1);
		Sentence s2 = new Sentence("test", 0, 1);
		Sentence s3 = new Sentence("test1", 0, 1);
		Sentence s4 = new Sentence("test", 0, 2);	
		Sentence s5 = new Sentence(null, 0, 4);
		Sentence s6 = new Sentence(null, 0, 4);
		Sentence s7 = new Sentence(null, 0, 1);

		assertEquals(s1, s2);
		assertNotEquals(s2, s3);
		assertNotEquals(s2, s4);
		
		assertFalse(s1.equals(null));
		assertFalse(s1.equals(new Object()));
		assertTrue(s1.equals(s1));
		
		assertTrue(s5.equals(s6));
		assertFalse(s7.equals(s1));
		assertFalse(s1.equals(s7));
	}

}

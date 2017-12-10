package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeywordTest {

	@Test
	public void testHashCode() {
		Keyword k1 = new Keyword("test", 0);
		Keyword k2 = new Keyword("test", 0);
		assertEquals(k1.hashCode(), k2.hashCode());
	}

	@Test
	public void testGetValue() {
		Keyword k1 = new Keyword("test", 0);
		assertEquals("test", k1.getValue());
	}

	@Test
	public void testGetScore() {
		Keyword k1 = new Keyword("test", 1.0);
		assertEquals("keyword score test", 1.0, k1.getScore(), 0.0);
	}

	@Test
	public void testEqualsObject() {
		Keyword k1 = new Keyword("test", 0);
		Keyword k2 = new Keyword("test", 0);
		Keyword k3 = new Keyword("test1", 0);
		Keyword k4 = new Keyword(null, 0);
		
		assertEquals(k1, k2);
		assertTrue(k1.equals(k2));
		assertTrue(k1.equals(k1));
		
		assertNotEquals(k1, k3);
		assertNotEquals(k2, k3);
		
		assertNotEquals(k1, null);
		assertNotEquals(k1, new Object());
		
		assertEquals(k4, k4);
		assertFalse(k4.equals(k1));
		
	}

}

package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testToString() {
        Sentence s = new Sentence("hello", 1.5, 3);
        assertTrue(s.toString().contains("hello"));
        assertTrue(s.toString().contains("1.5"));
        assertTrue(s.toString().contains("3"));
    }

    @Test
    public void testCompareTo() {
        Sentence higher = new Sentence("a", 2.0, 1);
        Sentence lower  = new Sentence("b", 1.0, 2);
        Sentence equal  = new Sentence("c", 2.0, 3);
        assertTrue(higher.compareTo(lower) < 0);
        assertTrue(lower.compareTo(higher) > 0);
        assertEquals(0, higher.compareTo(equal));
    }

    @Test
    public void testHashCodeNullValue() {
        Sentence s = new Sentence(null, 1.0, 3);
        // Should not throw; covers the value == null branch in hashCode
        int h = s.hashCode();
        Sentence s2 = new Sentence(null, 2.0, 3);
        assertEquals(s.hashCode(), s2.hashCode());
    }

}

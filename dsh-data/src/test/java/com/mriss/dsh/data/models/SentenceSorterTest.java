package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link SentenceSorter}.
 */
public class SentenceSorterTest {

    @Test
    public void testGetInstanceReturnsSingleton() {
        SentenceSorter s1 = SentenceSorter.getInstance();
        SentenceSorter s2 = SentenceSorter.getInstance();
        assertSame(s1, s2);
    }

    @Test
    public void testCompareHigherFirst() {
        Sentence higher = new Sentence("a", 2.0, 1);
        Sentence lower  = new Sentence("b", 1.0, 2);
        assertTrue(SentenceSorter.getInstance().compare(higher, lower) < 0);
    }

    @Test
    public void testCompareLowerSecond() {
        Sentence higher = new Sentence("a", 2.0, 1);
        Sentence lower  = new Sentence("b", 1.0, 2);
        assertTrue(SentenceSorter.getInstance().compare(lower, higher) > 0);
    }

    @Test
    public void testCompareEqualScores() {
        Sentence s1 = new Sentence("a", 1.5, 1);
        Sentence s2 = new Sentence("b", 1.5, 2);
        assertEquals(0, SentenceSorter.getInstance().compare(s1, s2));
    }
}


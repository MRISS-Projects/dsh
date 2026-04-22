package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link KeywordSorter}.
 */
public class KeywordSorterTest {

    @Test
    public void testGetInstanceReturnsSingleton() {
        KeywordSorter s1 = KeywordSorter.getInstance();
        KeywordSorter s2 = KeywordSorter.getInstance();
        assertSame(s1, s2);
    }

    @Test
    public void testCompareHigherFirst() {
        Keyword higher = new Keyword("a", 2.0);
        Keyword lower  = new Keyword("b", 1.0);
        assertTrue(KeywordSorter.getInstance().compare(higher, lower) < 0);
    }

    @Test
    public void testCompareLowerSecond() {
        Keyword higher = new Keyword("a", 2.0);
        Keyword lower  = new Keyword("b", 1.0);
        assertTrue(KeywordSorter.getInstance().compare(lower, higher) > 0);
    }

    @Test
    public void testCompareEqualScores() {
        Keyword k1 = new Keyword("a", 1.5);
        Keyword k2 = new Keyword("b", 1.5);
        assertEquals(0, KeywordSorter.getInstance().compare(k1, k2));
    }
}


package com.mriss.dsh.solr.vectororderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link Order}.
 */
public class OrderTest {

    // -------------------------------------------------------------------------
    // getOrderByName
    // -------------------------------------------------------------------------

    @Test
    public void testGetOrderByNameAsc() {
        assertEquals(Order.ASC, Order.getOrderByName("asc"));
    }

    @Test
    public void testGetOrderByNameDesc() {
        assertEquals(Order.DESC, Order.getOrderByName("desc"));
    }

    @Test
    public void testGetOrderByNameUnknown() {
        assertNull(Order.getOrderByName("unknown"));
    }

    // -------------------------------------------------------------------------
    // ASC.compare
    // -------------------------------------------------------------------------

    @Test
    public void testAscCompareLess() {
        // o1 < o2 → -1
        assertEquals(-1, Order.ASC.compare(1, 5));
    }

    @Test
    public void testAscCompareGreater() {
        // o1 > o2 → 1
        assertEquals(1, Order.ASC.compare(5, 1));
    }

    @Test
    public void testAscCompareEqual() {
        // o1 == o2 → 1 (equal counts as >=, returns 1 so sorted maps keep both)
        assertEquals(1, Order.ASC.compare(3, 3));
    }

    // -------------------------------------------------------------------------
    // DESC.compare
    // -------------------------------------------------------------------------

    @Test
    public void testDescCompareGreater() {
        // o1 > o2 → -1
        assertEquals(-1, Order.DESC.compare(5, 1));
    }

    @Test
    public void testDescCompareLess() {
        // o1 < o2 → 1
        assertEquals(1, Order.DESC.compare(1, 5));
    }

    @Test
    public void testDescCompareEqual() {
        // o1 == o2 → 1
        assertEquals(1, Order.DESC.compare(3, 3));
    }
}


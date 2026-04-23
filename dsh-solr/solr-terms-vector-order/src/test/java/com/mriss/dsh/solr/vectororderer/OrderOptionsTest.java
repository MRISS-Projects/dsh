package com.mriss.dsh.solr.vectororderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link OrderOptions}.
 */
public class OrderOptionsTest {

    @Test
    public void testGetters() {
        OrderOptions opts = new OrderOptions(Order.DESC, "tv.tf");
        assertEquals(Order.DESC, opts.getOrder());
        assertEquals("tv.tf", opts.getTermVectorParam());
    }

    @Test
    public void testToString() {
        OrderOptions opts = new OrderOptions(Order.ASC, "tv.tf");
        String s = opts.toString();
        assertNotNull(s);
        // Verify both parts appear in toString output
        assert s.contains("ASC");
        assert s.contains("tv.tf");
    }
}


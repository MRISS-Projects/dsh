package com.mriss.dsh.solr.vectororderer;

import static org.junit.Assert.assertEquals;

import org.apache.solr.common.util.NamedList;
import org.junit.Test;

/**
 * Unit tests for {@link TermsVectorComparator} covering all {@code getParamValue} branches.
 */
public class TermsVectorComparatorTest {

    /**
     * Both terms have a Long tf value – exercises the {@code tf instanceof Long} branch.
     */
    @Test
    public void testCompareWithLongTf() {
        NamedList<Object> list = new NamedList<>();

        NamedList<Object> termA = new NamedList<>();
        termA.add("tf", 5L);
        list.add("termA", termA);

        NamedList<Object> termB = new NamedList<>();
        termB.add("tf", 2L);
        list.add("termB", termB);

        OrderOptions opts = new OrderOptions(Order.DESC, "tv.tf");
        TermsVectorComparator cmp = new TermsVectorComparator(opts, list);

        // DESC: 5 > 2 → compare("termA","termB") == -1
        assertEquals(-1, cmp.compare("termA", "termB"));
    }

    /**
     * Both terms have an Integer tf value – exercises the {@code tf instanceof Integer} branch.
     */
    @Test
    public void testCompareWithIntegerTf() {
        NamedList<Object> list = new NamedList<>();

        NamedList<Object> termA = new NamedList<>();
        termA.add("tf", 3);   // Integer
        list.add("termA", termA);

        NamedList<Object> termB = new NamedList<>();
        termB.add("tf", 7);   // Integer
        list.add("termB", termB);

        OrderOptions opts = new OrderOptions(Order.ASC, "tv.tf");
        TermsVectorComparator cmp = new TermsVectorComparator(opts, list);

        // ASC: 3 < 7 → compare("termA","termB") == -1
        assertEquals(-1, cmp.compare("termA", "termB"));
    }

    /**
     * A term whose tf field is missing – exercises the {@code tf == null → return 0} branch.
     * Both values are 0 so result is 1 (equal case returns 1).
     */
    @Test
    public void testCompareWithNullTf() {
        NamedList<Object> list = new NamedList<>();

        NamedList<Object> termA = new NamedList<>();
        // no "tf" entry
        list.add("termA", termA);

        NamedList<Object> termB = new NamedList<>();
        // no "tf" entry
        list.add("termB", termB);

        OrderOptions opts = new OrderOptions(Order.ASC, "tv.tf");
        TermsVectorComparator cmp = new TermsVectorComparator(opts, list);

        // Both values = 0, equal → returns 1
        assertEquals(1, cmp.compare("termA", "termB"));
    }

    /**
     * A term that maps to a plain String (not a NamedList) – exercises the
     * {@code !(value instanceof NamedList) → return 0} branch.
     */
    @Test
    public void testCompareWhenValueNotNamedList() {
        NamedList<Object> list = new NamedList<>();
        list.add("termA", "plain string value");   // not a NamedList
        list.add("termB", "another string");

        OrderOptions opts = new OrderOptions(Order.ASC, "tv.tf");
        TermsVectorComparator cmp = new TermsVectorComparator(opts, list);

        // Both return 0 from getParamValue → equal → returns 1
        assertEquals(1, cmp.compare("termA", "termB"));
    }

    /**
     * A term whose tf value is a non-Long, non-Integer type (e.g. Double) –
     * exercises the final {@code return 0} inside the NamedList branch.
     */
    @Test
    public void testCompareWithUnsupportedTfType() {
        NamedList<Object> list = new NamedList<>();

        NamedList<Object> termA = new NamedList<>();
        termA.add("tf", 3.14);  // Double – not Long or Integer
        list.add("termA", termA);

        NamedList<Object> termB = new NamedList<>();
        termB.add("tf", 1.5);
        list.add("termB", termB);

        OrderOptions opts = new OrderOptions(Order.ASC, "tv.tf");
        TermsVectorComparator cmp = new TermsVectorComparator(opts, list);

        // Both return 0 → equal → returns 1
        assertEquals(1, cmp.compare("termA", "termB"));
    }
}


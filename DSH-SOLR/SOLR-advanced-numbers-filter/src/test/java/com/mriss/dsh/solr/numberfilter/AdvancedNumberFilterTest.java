package com.mriss.dsh.solr.numberfilter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AdvancedNumberFilterTest {

    @Spy
    Map attributes = new HashMap();

    @Spy
    @InjectMocks
    TokenStream tokenStream = new TokenStream() {

        int calls = 0;

        @Override
        public boolean incrementToken() throws IOException {
            if (calls < 4) {
                calls++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void reset() throws IOException {
            calls = 0;
        }

    };

    @Mock
    private CharTermAttribute termAtt;

    @Spy
    @InjectMocks
    private AdvancedNumberFilter filter = new AdvancedNumberFilter(tokenStream);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIncrementToken() throws IOException {
        Mockito.when(termAtt.buffer()).thenReturn(new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' },
                new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' },
                new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' });
        Mockito.when(termAtt.length()).thenReturn(3);
        filter.setTermAtt(termAtt);
        boolean a = filter.incrementToken();
        while (a) {
            a = filter.incrementToken();
        }
        assertEquals("abc abc abc abc ", filter.getContent().toString());
    }

}

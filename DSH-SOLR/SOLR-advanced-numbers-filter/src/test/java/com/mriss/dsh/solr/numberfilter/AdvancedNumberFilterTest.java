package com.mriss.dsh.solr.numberfilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        @Override
        public boolean incrementToken() throws IOException {
            return true;
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
    public void testRegularExpressionMixedNumbers1() throws Exception {
        Pattern r1 = Pattern.compile(AdvancedNumberFilter.MIXED_NUMBER_1);
        Matcher m1 = r1.matcher("canada-39902108");
        assertTrue(m1.matches());
        r1 = Pattern.compile(AdvancedNumberFilter.MIXED_NUMBER_2);
        m1 = r1.matcher("39902108-canada");
        assertTrue(m1.matches());
        r1 = Pattern.compile(AdvancedNumberFilter.MIXED_NUMBER_2);
        m1 = r1.matcher("39902108");
        assertFalse(m1.matches());
        r1 = Pattern.compile(AdvancedNumberFilter.MIXED_NUMBER_2);
        m1 = r1.matcher("canada");
        assertFalse(m1.matches());
    }

    @Test
    public void testAcceptUrl() throws IOException {
        String testString = "https://bla.com.br";
        Mockito.when(termAtt.buffer()).thenReturn(testString.toCharArray());
        Mockito.when(termAtt.length()).thenReturn(testString.length());
        assertFalse(filter.accept());
    }

    @Test
    public void testMixedNumbers1() throws IOException {
        String testString = "canada-39902108";
        Mockito.when(termAtt.buffer()).thenReturn(testString.toCharArray());
        Mockito.when(termAtt.length()).thenReturn(testString.length());
        assertFalse(filter.accept());
    }

    @Test
    public void testMixedNumbers2() throws IOException {
        String testString = "39902108-canada";
        Mockito.when(termAtt.buffer()).thenReturn(testString.toCharArray());
        Mockito.when(termAtt.length()).thenReturn(testString.length());
        assertFalse(filter.accept());
    }

    @Test
    public void testHostName() throws IOException {
        String testString = "www.myhost.com.br";
        Mockito.when(termAtt.buffer()).thenReturn(testString.toCharArray());
        Mockito.when(termAtt.length()).thenReturn(testString.length());
        assertFalse(filter.accept());
    }

    @Test
    public void testRegularTerm() throws IOException {
        String testString = "myRegularTerm";
        Mockito.when(termAtt.buffer()).thenReturn(testString.toCharArray());
        Mockito.when(termAtt.length()).thenReturn(testString.length());
        assertTrue(filter.accept());
    }

}

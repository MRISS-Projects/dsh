package com.mriss.dsh.solr.numberfilter;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AdvancedNumberFilterFactoryTest {

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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAdvancecNumberFilterFactory() {
        AdvancedNumberFilterFactory factory = new AdvancedNumberFilterFactory(new HashMap<String, String>());
        assertNotNull(factory);
    }

    @Test
    public void testCreateTokenStream() {
        AdvancedNumberFilterFactory factory = new AdvancedNumberFilterFactory(new HashMap<String, String>());
        assertNotNull(factory);
        AdvancedNumberFilter filter = (AdvancedNumberFilter) factory.create(tokenStream);
        assertNotNull(filter);
    }

    @Test
    public void testNormalizeTokenStream() {
        AdvancedNumberFilterFactory factory = new AdvancedNumberFilterFactory(new HashMap<String, String>());
        assertNotNull(factory);
        AdvancedNumberFilter filter = (AdvancedNumberFilter) factory.create(tokenStream);
        assertNotNull(filter);
        filter = (AdvancedNumberFilter) factory.normalize(tokenStream);
        assertNotNull(filter);
    }

}

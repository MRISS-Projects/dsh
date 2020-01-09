package com.mriss.dsh.solr.numberfilter;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedNumberFilter extends TokenFilter {

    Logger LOGGER = LoggerFactory.getLogger(AdvancedNumberFilter.class);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final TermFrequencyAttribute termFreqAtt = addAttribute(TermFrequencyAttribute.class);

    private boolean initialized = false;

    private StringBuffer content = new StringBuffer();

    protected AdvancedNumberFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (!initialized) {
                char[] buffer = termAtt.buffer();
                content.append(buffer);
            } else {

            }
            return true;
        } else {
            if (!initialized) {
                initialized = true;
                input.reset();
                LOGGER.info("content: " + content.toString());
                return true;
            } else {
                return false;
            }
        }
    }

}

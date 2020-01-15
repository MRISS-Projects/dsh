package com.mriss.dsh.solr.numberfilter;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedNumberFilter extends TokenFilter {

    Logger LOGGER = LoggerFactory.getLogger(AdvancedNumberFilter.class);

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private TermFrequencyAttribute termFreqAtt = addAttribute(TermFrequencyAttribute.class);

    private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private boolean initialized = false;
    
    private int upto = 0;

    private int finalOffset;

    private StringBuffer content = new StringBuffer();

    protected AdvancedNumberFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (!initialized) {
                char[] buffer = termAtt.buffer();
                int length = termAtt.length();
                content.append(buffer, 0, length).append(' ');
                upto += length;
                finalOffset = upto;
                offsetAtt.setOffset(0, finalOffset);
            } else {
                LOGGER.info("Processing term.");
            }
            return true;
        } else {
            if (!initialized) {
                initialized = true;
                LOGGER.info("content: " + content.toString());
                return true;
            } else {
                return false;
            }
        }
    }

    public StringBuffer getContent() {
        return content;
    }

    public void setTermAtt(CharTermAttribute termAtt) {
        this.termAtt = termAtt;
    }

    @Override
    public final void end() throws IOException {
        super.end();
        // set final offset
        offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        this.initialized = false;
    }
}

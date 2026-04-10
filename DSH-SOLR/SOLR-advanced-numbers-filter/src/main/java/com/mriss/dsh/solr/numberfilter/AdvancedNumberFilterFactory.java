package com.mriss.dsh.solr.numberfilter;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 */
public class AdvancedNumberFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "advancedNumberFilter";

    public AdvancedNumberFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new AdvancedNumberFilter(input);
    }

    @Override
    public TokenStream normalize(TokenStream input) {
        return create(input);
    }

}

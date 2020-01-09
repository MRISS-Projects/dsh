package com.mriss.dsh.solr.numberfilter;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 */
public class AdvancecNumberFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "advancedNumberFilter";

    public AdvancecNumberFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
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

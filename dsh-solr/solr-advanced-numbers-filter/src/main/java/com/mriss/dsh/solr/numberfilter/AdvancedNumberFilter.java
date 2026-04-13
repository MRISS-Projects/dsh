package com.mriss.dsh.solr.numberfilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedNumberFilter extends FilteringTokenFilter {

    public static final String MIXED_NUMBER_1 = "(\\D*)\\-(\\d*)";
    public static final String MIXED_NUMBER_2 = "(\\d*)\\-(\\D*)";

    Logger LOGGER = LoggerFactory.getLogger(AdvancedNumberFilter.class);

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    protected AdvancedNumberFilter(TokenStream input) {
        super(input);
    }

    @Override
    protected boolean accept() throws IOException {
        int length = termAtt.length();
        char[] buffer = termAtt.buffer();
        String contents = new String(buffer, 0, length);
        return validate(contents);
    }

    private boolean validate(String contents) {
        return !isUrl(contents) && !isMixedNumber(contents) && !isHostName(contents);
    }

    private boolean isHostName(String contents) {
        Pattern r1 = Pattern.compile(
                "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
        Matcher m1 = r1.matcher(contents);
        return contents.contains(".") && m1.matches();
    }

    private boolean isMixedNumber(String contents) {
        Pattern r1 = Pattern.compile(MIXED_NUMBER_1);
        Matcher m1 = r1.matcher(contents);
        Pattern r2 = Pattern.compile(MIXED_NUMBER_2);
        Matcher m2 = r2.matcher(contents);
        return m1.matches() || m2.matches();
    }

    private boolean isUrl(String contents) {
        Pattern r1 = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m1 = r1.matcher(contents);
        return m1.matches();
    }
}

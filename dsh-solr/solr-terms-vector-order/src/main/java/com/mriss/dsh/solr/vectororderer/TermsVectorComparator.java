package com.mriss.dsh.solr.vectororderer;

import java.util.Comparator;

import org.apache.solr.common.params.TermVectorParams;
import org.apache.solr.common.util.NamedList;

/**
 * A comparator class to compare two instances of {@link NamedList}. Those lists
 * represent terms information (TF, DF, TF-IDF, etc) in a terms vector. This
 * class assumes to be ordering keys in a sorted map where the key is a
 * {@link String}. This string is used to get the field information from
 * {@link #originalNamedList}.
 * 
 * @author riss
 *
 */
public class TermsVectorComparator implements Comparator<Object> {

    /**
     * The order information to be used to order.
     */
    private OrderOptions orderOptions;

    /**
     * A striped version of the field/parameter to be used. The prefix
     * <code>.tv</code> will be removed from the original field name at
     * {@link #orderOptions}.
     */
    private String strippedTermVectorParam;

    /**
     * The original named list to be sorted. This list will be used to get the
     * original value of the objects having field information.
     */
    private NamedList<Object> originalNamedList;

    public TermsVectorComparator(OrderOptions orderOptions, NamedList<Object> originalNamedList) {
        this.orderOptions = orderOptions;
        this.originalNamedList = originalNamedList;
        this.strippedTermVectorParam = orderOptions.getTermVectorParam().replaceAll(TermVectorParams.TV_PREFIX, "");
    }

    /**
     * For this implementation the objects are strings.
     */
    @Override
    public int compare(Object o1, Object o2) {
        int paramValueO1 = getParamValue((String) o1);
        int paramValueO2 = getParamValue((String) o2);
        return orderOptions.getOrder().compare(paramValueO1, paramValueO2);
    }

    /**
     * Search the field information (TF, DF, etc) based on the term. Executes the
     * search at list in {@link #originalNamedList}.
     * 
     * @param o the term.
     * @return the value for the field configured at {@link #orderOptions}, for the
     *         specific term.
     */
    private int getParamValue(String o) {
        Object value = this.originalNamedList.get(o);
        if (value instanceof NamedList) {
            NamedList<Object> nl = (NamedList<Object>) value;
            Object tf = nl.get(strippedTermVectorParam);
            if (tf != null && tf instanceof Long) {
                return ((Long) tf).intValue();
            } else if (tf != null && tf instanceof Integer) {
                return (Integer) tf;
            } else {
                return 0;
            }
        } else {
            return 0;
        }

    }

}

package com.mriss.dsh.solr.vectororderer;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.collections.FastTreeMap;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A version of {@link NamedList} handling a sort of elements. This class copy
 * the contents on <code>nvPairs</code> field to an instance of a
 * {@link SortedMap} initialized with an instance of
 * {@link TermsVectorComparator}. Then, all contents of the resulting
 * {@link SortedMap} are added to the instance of this class.
 * 
 * @author riss
 * @see TermsVectorComparator
 *
 */
public class SortedNamedList extends NamedList<Object> {

    Logger LOGGER = LoggerFactory.getLogger(SortedNamedList.class);

    /**
     * Serial id.
     */
    private static final long serialVersionUID = 6977027481117549645L;

    /**
     * Sort method. It will use a {@link SortedMap} to sort the contents of this
     * named list.
     * 
     * @param orderOptions the order options to get order information.
     */
    public void sort(OrderOptions orderOptions) {
        Map<String, Object> sortedMap = copyToSortedMap(orderOptions);
        this.clear();
        this.addAll(sortedMap);
    }

    /**
     * Copy this list content to a sorted map, in order to order it.
     * 
     * @param orderOptions the order options to get order information.
     * @return a sorted map with all contents sorted.
     */
    private Map<String, Object> copyToSortedMap(OrderOptions orderOptions) {
        Map<String, Object> fastTreeMap = new FastTreeMap(new TermsVectorComparator(orderOptions, this));
        for (Iterator iterator = nvPairs.iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            Object value = iterator.next();
            fastTreeMap.put(key, value);
        }
        return fastTreeMap;
    }

}

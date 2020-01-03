package com.mriss.dsh.solr.vectororderer;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.FastTreeMap;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortedNamedList extends NamedList<Object> {

	Logger LOGGER = LoggerFactory.getLogger(SortedNamedList.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6977027481117549645L;

	public void sort(OrderOptions orderOptions) {
		Map<String, Object> sortedMap = copyToSortedMap(orderOptions);
		LOGGER.info("sortedMap: " + sortedMap);
		this.clear();
		this.addAll(sortedMap);
		//Collections.sort(this.nvPairs, new TermsVectorComparator(orderOptions));		
	}

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

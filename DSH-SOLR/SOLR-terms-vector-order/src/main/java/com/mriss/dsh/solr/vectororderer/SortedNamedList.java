package com.mriss.dsh.solr.vectororderer;

import java.util.Collections;

import org.apache.solr.common.util.NamedList;

public class SortedNamedList extends NamedList<Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6977027481117549645L;

	public void sort(OrderOptions orderOptions) {
		Collections.sort(this.nvPairs, new TermsVectorComparator(orderOptions));		
	}

	
}

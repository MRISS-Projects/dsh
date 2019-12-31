package com.mriss.dsh.solr.vectororderer;

import java.util.Comparator;

import org.apache.solr.common.params.TermVectorParams;

public class TermsVectorComparator implements Comparator<Object> {

	private OrderOptions orderOptions;
	private String strippedTermVectorParam;

	public TermsVectorComparator(OrderOptions orderOptions) {
		this.orderOptions = orderOptions;
		this.strippedTermVectorParam = orderOptions.getTermVectorParam().replaceAll(TermVectorParams.TV_PREFIX, "");
	}

	@Override
	public int compare(Object o1, Object o2) {
		int paramValueO1 = getParamValue(o1);
		int paramValueO2 = getParamValue(o2);
		return orderOptions.getOrder().compare(paramValueO1, paramValueO2);
	}

	private int getParamValue(Object o1) {
		// TODO Auto-generated method stub
		return 0;
	}

}

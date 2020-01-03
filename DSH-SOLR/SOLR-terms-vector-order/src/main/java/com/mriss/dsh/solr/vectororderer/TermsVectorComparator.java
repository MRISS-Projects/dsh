package com.mriss.dsh.solr.vectororderer;

import java.util.Comparator;

import org.apache.solr.common.params.TermVectorParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermsVectorComparator implements Comparator<Object> {

	private OrderOptions orderOptions;
	private String strippedTermVectorParam;
	private NamedList<Object> originalNamedList;

	public TermsVectorComparator(OrderOptions orderOptions, NamedList<Object> originalNamedList) {
		this.orderOptions = orderOptions;
		this.originalNamedList = originalNamedList;
		this.strippedTermVectorParam = orderOptions.getTermVectorParam().replaceAll(TermVectorParams.TV_PREFIX, "");
	}

	@Override
	public int compare(Object o1, Object o2) {
		int paramValueO1 = getParamValue((String) o1);
		int paramValueO2 = getParamValue((String) o2);
		return orderOptions.getOrder().compare(paramValueO1, paramValueO2);
	}

	private int getParamValue(String o) {
		Object value = this.originalNamedList.get(o);
		if (value instanceof NamedList) {
			NamedList<Object> nl = (NamedList<Object>) value;
			Object tf = nl.get(strippedTermVectorParam);
			if (tf != null && tf instanceof Long) {
				return ((Long)tf).intValue();
			} else if (tf != null && tf instanceof Integer) {
				return (Integer)tf;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
		
	}

}

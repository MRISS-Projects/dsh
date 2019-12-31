package com.mriss.dsh.solr.vectororderer;

public class OrderOptions {

	private Order order;
	
	private String termVectorParam;

	public OrderOptions(Order order, String orderField) {
		super();
		this.order = order;
		this.termVectorParam = orderField;
	}

	public Order getOrder() {
		return order;
	}

	public String getTermVectorParam() {
		return termVectorParam;
	}

	@Override
	public String toString() {
		return "OrderOptions [order=" + order + ", orderField=" + termVectorParam + "]";
	}
	
}

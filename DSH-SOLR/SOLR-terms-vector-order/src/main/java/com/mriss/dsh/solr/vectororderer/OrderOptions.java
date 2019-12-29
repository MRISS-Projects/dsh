package com.mriss.dsh.solr.vectororderer;

public class OrderOptions {

	private Order order;
	
	private String orderField;

	public OrderOptions(Order order, String orderField) {
		super();
		this.order = order;
		this.orderField = orderField;
	}

	public Order getOrder() {
		return order;
	}

	public String getOrderField() {
		return orderField;
	}

	@Override
	public String toString() {
		return "OrderOptions [order=" + order + ", orderField=" + orderField + "]";
	}
	
}

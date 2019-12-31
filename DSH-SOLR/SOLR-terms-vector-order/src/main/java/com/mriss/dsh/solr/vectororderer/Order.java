package com.mriss.dsh.solr.vectororderer;

import java.util.HashMap;
import java.util.Map;

public enum Order {

	ASC("asc"),
	
	DESC("desc");
	
	private String orderName;

	private static class OrderNamesHolder {
		
		private static Map<String, Order> orderNames = new HashMap<String, Order>(); 
		
		public static void put(String orderName, Order order) {
			orderNames.put(orderName, order);
		}

		public static Order get(String orderName) {
			return orderNames.get(orderName);
		}

	}
	
	private Order(String orderName) {
		this.orderName = orderName;
		OrderNamesHolder.put(this.orderName, this);
	}
	
	public static Order getOrderByName(String orderName) {
		return OrderNamesHolder.get(orderName);
	}

	public int compare(int paramValueO1, int paramValueO2) {
		if (this == ASC) {
			if (paramValueO1 < paramValueO2) {
				return -1;
			} else if (paramValueO1 > paramValueO2) {
				return 1;
			} else {
				return 0;
			}
		} else if(this == DESC) {
			if (paramValueO1 > paramValueO2) {
				return -1;
			} else if (paramValueO1 < paramValueO2) {
				return 1;
			} else {
				return 0;
			}			
		} else {
			return 0;
		}		
	}

}

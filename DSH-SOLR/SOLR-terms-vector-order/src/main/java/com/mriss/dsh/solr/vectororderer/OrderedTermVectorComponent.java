package com.mriss.dsh.solr.vectororderer;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.TermVectorComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedTermVectorComponent extends TermVectorComponent {

	private static final String ORDER_PARAM = "order";
	
	Logger LOGGER = LoggerFactory.getLogger(OrderedTermVectorComponent.class);

	@Override
	public void process(ResponseBuilder rb) throws IOException {		
		super.process(rb);
		
		SolrParams params = rb.req.getParams();
		
		OrderOptions orderOptions = getOrderOptions(params.get(ORDER_PARAM));
		
		if (orderOptions != null) {
			LOGGER.info("Order options: " + orderOptions);
		}
		
		LOGGER.info("Leaving Custom Ordered Terms Vector Component...");
	}

	private OrderOptions getOrderOptions(String orderParam) {
		LOGGER.info("orderParam: " + orderParam);
		if (orderParam != null && !orderParam.isEmpty()) {
			String[] orderInfo = orderParam.split(";");
			return new OrderOptions(Order.getOrderByName(orderInfo[1]), orderInfo[0]);
		} else {
			return null;
		}
	}

	@Override
	public void init(NamedList args) {
		super.init(args);
	}

}

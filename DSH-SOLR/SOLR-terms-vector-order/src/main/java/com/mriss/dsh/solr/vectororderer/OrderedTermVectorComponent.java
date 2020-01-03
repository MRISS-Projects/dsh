package com.mriss.dsh.solr.vectororderer;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.TermVectorComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedTermVectorComponent extends TermVectorComponent {

	Logger LOGGER = LoggerFactory.getLogger(OrderedTermVectorComponent.class);
	
	static final String ORDER_PARAM = "order";
	
	private TermVectorComponent responseBuilderProcessor = new TermVectorComponent();

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		
		responseBuilderProcessor.process(rb);

		SolrParams params = rb.req.getParams();

		OrderOptions orderOptions = getOrderOptions(params.get(ORDER_PARAM));

		if (orderOptions != null) {
			LOGGER.info("Order options: " + orderOptions);
			NamedList<Object> responseValues = rb.rsp.getValues();
			NamedList<Object> tvNamedList = (NamedList<Object>) responseValues.get(TermVectorComponent.TERM_VECTORS);
			tvNamedList.forEach( (tvKey, tvValue) -> {
				if (tvValue instanceof NamedList) {
					NamedList<Object> docNamedList = (NamedList<Object>) tvValue;
					if (docNamedList.get("uniqueKey") != null) {
						NamedList<Object> newDocNamedList = new NamedList<Object>();
						String uniqueKey = (String) docNamedList.get("uniqueKey");
						newDocNamedList.add("uniqueKey", uniqueKey);						
						docNamedList.forEach( (fieldKey, fieldValue) -> {
							if (fieldValue instanceof NamedList) {
								NamedList<Object> fieldNamedList = (NamedList<Object>) fieldValue;
								SortedNamedList snl = new SortedNamedList();
								snl.addAll(fieldNamedList);
								snl.sort(orderOptions);
								newDocNamedList.add(fieldKey, snl);								
							}
						});
						docNamedList.clear();
						docNamedList.addAll(newDocNamedList);
					}
				}
			});
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

}

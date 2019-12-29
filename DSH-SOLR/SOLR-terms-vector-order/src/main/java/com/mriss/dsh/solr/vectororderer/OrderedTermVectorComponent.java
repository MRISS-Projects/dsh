package com.mriss.dsh.solr.vectororderer;

import java.io.IOException;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.TermVectorComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedTermVectorComponent extends TermVectorComponent {

	Logger LOGGER = LoggerFactory.getLogger(OrderedTermVectorComponent.class);

	@Override
	public void process(ResponseBuilder arg0) throws IOException {
		super.process(arg0);
		LOGGER.info("Leaving Custom Ordered Terms Vector Component...");
	}

	@Override
	public void init(NamedList args) {
		super.init(args);
	}

}

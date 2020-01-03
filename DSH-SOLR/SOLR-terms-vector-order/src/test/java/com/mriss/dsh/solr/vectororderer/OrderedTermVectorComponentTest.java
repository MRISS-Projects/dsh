package com.mriss.dsh.solr.vectororderer;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.Utils;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.TermVectorComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedTermVectorComponentTest {
	
	Logger LOGGER = LoggerFactory.getLogger(OrderedTermVectorComponentTest.class);

	private Map<Object, Object> originalJson;
	
	private NamedList<Object> namedList;
	
	@Mock
	TermVectorComponent responseBuilderProcessor;
	
	@InjectMocks
	private OrderedTermVectorComponent termVectorComponent = new OrderedTermVectorComponent();
	
	@Mock
	private SolrQueryRequest req;
	
	@Mock
	private SolrQueryResponse rsp;
	
	@InjectMocks
	private ResponseBuilder rb = new ResponseBuilder(null, null, null);
	
	@Before
	public void setUp() throws Exception {
		originalJson = (Map<Object, Object>) Utils.fromJSON(new FileReader(new File("target/test-classes/test-ordering.json")));
		assertNotNull(originalJson);
		namedList = convertToNamedList(originalJson);
		assertNotNull(namedList);
		MockitoAnnotations.initMocks(this);
	}
	 
	private NamedList<Object> convertToNamedList(Map<Object, Object> jsonMap) {
		NamedList<Object> newNamedList = new NamedList();
		Iterator<Entry<Object, Object>> it = jsonMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Object, Object> e = it.next();
			if (!(e.getValue() instanceof NamedList) && e.getValue() instanceof Map) {
				String key = (String) e.getKey();
				Object value = e.getValue();
				newNamedList.add(key, convertToNamedList((Map<Object, Object>) value));
			} else if (!(e.getValue() instanceof NamedList) && e.getValue() instanceof List) {
				String key = (String) e.getKey();
				Object value = e.getValue();				
				newNamedList.add(key, convertToNamedList((List) value));				
			}
		}
		return newNamedList;
	}

	private NamedList<Object> convertToNamedList(List<?> list) {
		NamedList<Object> newNamedList = new NamedList<Object>();
		Iterator<?> it = list.iterator();
		while (it.hasNext()) {
			Object e = it.next();
			if (e instanceof String) {
				Object next = it.next();
				if (!(next instanceof NamedList) && next instanceof Map) {
					newNamedList.add((String)e, convertToNamedList((Map<Object, Object>) next));
				} else if (!(next instanceof NamedList) && next instanceof List) {
					newNamedList.add((String)e, convertToNamedList((List) next));				
				} else if (next instanceof NamedList) {
					newNamedList.addAll((NamedList<Object>)next);
				} else {
					newNamedList.add((String) e, next);
				}
			}
		}
		return newNamedList;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProcessResponseBuilderDesc() throws IOException {
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		    	  System.out.println("Mocking process method.");
		          return null;
		      }})
		  .when(responseBuilderProcessor).process(rb);
		SolrParams params = new SolrParams() {

			@Override
			public String get(String param) {
				if (param.equals(OrderedTermVectorComponent.ORDER_PARAM)) {
					return "tv.tf;desc";
				}
				return null;
			}

			@Override
			public String[] getParams(String param) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Iterator<String> getParameterNamesIterator() {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		Mockito.when(req.getParams()).thenReturn(params);
		Mockito.when(rsp.getValues()).thenReturn(namedList);
		termVectorComponent.process(rb);
		System.out.println(namedList);
	}

	@Test
	public void testProcessResponseBuilderAsc() throws IOException {
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		    	  System.out.println("Mocking process method.");
		          return null;
		      }})
		  .when(responseBuilderProcessor).process(rb);
		SolrParams params = new SolrParams() {

			@Override
			public String get(String param) {
				if (param.equals(OrderedTermVectorComponent.ORDER_PARAM)) {
					return "tv.tf;asc";
				}
				return null;
			}

			@Override
			public String[] getParams(String param) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Iterator<String> getParameterNamesIterator() {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		Mockito.when(req.getParams()).thenReturn(params);
		Mockito.when(rsp.getValues()).thenReturn(namedList);
		termVectorComponent.process(rb);
		System.out.println(namedList);
	}

}

package com.mriss.dsh.solr.vectororderer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OrderedTermVectorComponentTest {

	private Map<Object, Object> originalJson;
	
	private NamedList<Object> namedList;
	
	@Before
	public void setUp() throws Exception {
		originalJson = (Map<Object, Object>) Utils.fromJSON(new FileReader(new File("target/test-classes/test-ordering.json")));
		assertNotNull(originalJson);
		namedList = convertToNamedList(originalJson);
		assertNotNull(namedList);
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
	public void testProcessResponseBuilder() {
		fail("Not yet implemented");
	}

}

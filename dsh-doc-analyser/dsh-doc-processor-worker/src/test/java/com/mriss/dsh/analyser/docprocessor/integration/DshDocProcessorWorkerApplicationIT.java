package com.mriss.dsh.analyser.docprocessor.integration;

import com.mriss.dsh.analyser.docprocessor.DshDocProcessorWorkerApplication;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DshDocProcessorWorkerApplicationIT {
	
	final static Logger logger = LoggerFactory.getLogger(DshDocProcessorWorkerApplicationIT.class);

	@BeforeClass
	public static void setUp() throws Exception {
		logger.info("This is a log message!!!");
	}
	
	@AfterClass
	public static void tearDown() throws Exception{
		logger.info("This is a log message!!!");
	}

	@Test
	public void testSomething() throws Exception {
		logger.info("This is a log message!!!");
		DshDocProcessorWorkerApplication.main(new String[]{"arg1", "arg2"});		
	}
	
	@Test
	public void contextLoads() {
	}
	
	

}

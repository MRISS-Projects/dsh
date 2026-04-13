package com.mriss.dsh.analyser.docprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshDocProcessorWorkerApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshDocProcessorWorkerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshDocProcessorWorkerApplication.class, args);
		logger.info("Main application run!!!");
	}
}

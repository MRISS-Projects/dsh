package com.mriss.dsh.docindexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshDocIndexerApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshDocIndexerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshDocIndexerApplication.class, args);
		logger.info("Main application run!!!");
	}
}

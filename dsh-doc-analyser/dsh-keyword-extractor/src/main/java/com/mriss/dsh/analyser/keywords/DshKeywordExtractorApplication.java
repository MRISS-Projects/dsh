package com.mriss.dsh.analyser.keywords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshKeywordExtractorApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshKeywordExtractorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshKeywordExtractorApplication.class, args);
		logger.info("Main application run!!!");
	}
}

package com.mriss.dsh.analyser.topsentences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshTopSentencesExtractorApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshTopSentencesExtractorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshTopSentencesExtractorApplication.class, args);
		logger.info("Main application run!!!");
	}
}

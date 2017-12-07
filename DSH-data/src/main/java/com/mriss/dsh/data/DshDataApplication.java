package com.mriss.dsh.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshDataApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshDataApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshDataApplication.class, args);
		logger.info("Main application run!!!");
	}
}

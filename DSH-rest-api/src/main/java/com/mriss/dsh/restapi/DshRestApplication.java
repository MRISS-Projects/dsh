package com.mriss.dsh.restapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DshRestApplication {
	
	final static Logger logger = LoggerFactory.getLogger(DshRestApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshRestApplication.class, args);
		logger.info("Main application run!!!");
	}
}

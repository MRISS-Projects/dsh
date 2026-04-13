package com.mriss.dsh.restapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages={"com.mriss.dsh"})
@ImportResource(locations = { "classpath:/enqueue-docId-context.xml"})
@EnableAsync
public class DshRestApplication extends SpringBootServletInitializer {
	
	final static Logger logger = LoggerFactory.getLogger(DshRestApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DshRestApplication.class, args);
		logger.info("Main application run!!!");
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(DshRestApplication.class);
	}
	
}

package com.mriss.dsh.restapi.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class DocumentResourceTest {
	
	@Autowired
	private WebApplicationContext webApplicationContext;
	
	private MockMvc mockMvc;
	
	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	@Autowired(required = true)
	@Qualifier("testFile1")
	private File testFile1;
	
	@Before
	public void setup() throws Exception {				
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testSubmitDocument() throws Exception {
		
		MockMultipartFile firstFile = new MockMultipartFile("contents", "bbc-news-1.pdf", "application/pdf", new FileInputStream(testFile1));
		
		mockMvc.perform(
				MockMvcRequestBuilders.multipart("/v1/dsh/document/submit").file(firstFile).
				param("title", "Russia-Trump: FBI chief Wray defends agency")
		)
			.andExpect(status().isOk())
			.andExpect(content().contentType(contentType))
			.andExpect(jsonPath("token", org.hamcrest.Matchers.notNullValue()))
			.andExpect(jsonPath("message", org.hamcrest.Matchers.notNullValue()));

	}

}

@Configuration
class DocumentResourceTestTestConfiguration {
	
	@Bean(name="testFile1")
	public File getTestFile1() throws Exception {
		return new File("target/test-classes/pdf/bbc-news-1.pdf");
	}
	
}

package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.restapi.rest.DocumentResource;

/**
 * Calls the REST API over HTTP against the server that the {@code http-integration-tests} profile
 * starts, backed by the MongoDB and RabbitMQ containers it starts. No Spring context runs in this
 * JVM: the application under test is a separate process.
 */
public class DocumentResourceHttpIT {

	private static final String BASE_URL_PROPERTY = "dsh.it.baseUrl";

	private static final File FIXTURE = new File("target/test-classes/pdf/bbc-news-1.pdf");

	private static final long STATUS_TIMEOUT_MILLIS = 15_000;

	private static final long STATUS_POLL_MILLIS = 250;

	private static final ParameterizedTypeReference<Map<String, Object>> JSON =
			new ParameterizedTypeReference<Map<String, Object>>() { };

	private RestTemplate rest;

	private String baseUrl;

	@Before
	public void setUp() {
		baseUrl = System.getProperty(BASE_URL_PROPERTY);
		if (baseUrl == null) {
			fail(BASE_URL_PROPERTY + " is not set. Run this class under "
					+ "'mvn -B install -DintegrationTests', which starts the server it calls.");
		}
		rest = new RestTemplate();
		rest.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return false;
			}
		});
	}

	@Test
	public void submit_whenValidPdf_shouldQueueForIndexing() throws InterruptedException {
		ResponseEntity<Map<String, Object>> submitted = submit("Russia-Trump: FBI chief Wray defends agency", true);

		assertJson(submitted, HttpStatus.OK);
		String token = (String) submitted.getBody().get("token");
		assertThat(token).isNotBlank().isNotEqualTo("ERROR");

		String expected = DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS.getStatusDescription();
		String failed = DocumentStatus.QUEUED_FOR_INDEXING_ERROR.getStatusDescription();
		long deadline = System.currentTimeMillis() + STATUS_TIMEOUT_MILLIS;
		String last = null;
		while (System.currentTimeMillis() < deadline) {
			ResponseEntity<Map<String, Object>> status = status(token);
			assertJson(status, HttpStatus.OK);
			last = (String) status.getBody().get("status");
			if (expected.equals(last)) {
				return;
			}
			assertThat(last).as("the broker rejected or never confirmed the enqueue").isNotEqualTo(failed);
			Thread.sleep(STATUS_POLL_MILLIS);
		}
		fail("document " + token + " did not reach " + expected + " within "
				+ STATUS_TIMEOUT_MILLIS + " ms; last status was " + last);
	}

	@Test
	public void status_whenTokenUnknown_shouldReturnTokenNotFound() {
		String token = UUID.randomUUID().toString();

		ResponseEntity<Map<String, Object>> response = status(token);

		assertJson(response, HttpStatus.OK);
		assertThat(response.getBody())
				.containsEntry("status", DocumentResource.TOKEN_NOT_FOUND)
				.containsEntry("message", DocumentResource.TOKEN_NOT_FOUND_MESSAGE + token);
	}

	@Test
	public void submit_whenTitleBlank_shouldReturnErrorToken() {
		ResponseEntity<Map<String, Object>> response = submit("", true);

		assertJson(response, HttpStatus.OK);
		assertThat(response.getBody())
				.containsEntry("token", "ERROR")
				.containsEntry("message", "Error submitting file: Document title and contents can't be null.");
	}

	@Test
	public void submit_whenContentsMissing_shouldReturnBadRequest() {
		ResponseEntity<Map<String, Object>> response = submit("A title", false);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<Map<String, Object>> submit(String title, boolean withContents) {
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("title", title);
		if (withContents) {
			form.add("contents", new FileSystemResource(FIXTURE));
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return rest.exchange(baseUrl + "/v1/dsh/document/submit", HttpMethod.POST,
				new HttpEntity<>(form, headers), JSON);
	}

	private ResponseEntity<Map<String, Object>> status(String token) {
		return rest.exchange(baseUrl + "/v1/dsh/document/status/" + token, HttpMethod.GET, null, JSON);
	}

	private static void assertJson(ResponseEntity<?> response, HttpStatus expected) {
		assertThat(response.getStatusCode()).isEqualTo(expected);
		assertThat(response.getHeaders().getContentType()).isNotNull();
		assertThat(MediaType.APPLICATION_JSON.isCompatibleWith(response.getHeaders().getContentType())).isTrue();
	}
}

package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs every Postman collection in {@code specs/api/postman/} with newman, against the server that the
 * {@code http-integration-tests} profile starts: layer 4, a client that sees only the API contract.
 * newman is a separate process, run from the Node and the newman the same profile installs under
 * {@code target/newman}, so neither has to be on the PATH. A failed collection fails its test: failsafe
 * records it, the profile still stops the server and the container, and {@code verify} fails the build.
 */
public class PostmanCollectionsIT {

	private static final String COLLECTION_SUFFIX = ".postman_collection.json";

	/** So that a newman run that hangs fails the test rather than the build. */
	private static final long RUN_TIMEOUT_MINUTES = 5;

	/** The read timeout DocumentResourceHttpIT uses. */
	private static final String REQUEST_TIMEOUT_MILLIS = "60000";

	@TestFactory
	public Stream<DynamicTest> everyCollection_shouldPassAgainstTheServer() {
		String baseUrl = required("dsh.it.baseUrl");
		File node = node(new File(required("dsh.it.nodeDir")));
		File newman = new File(required("dsh.it.newman"));
		File collections = new File(required("dsh.it.collections"));
		File fixtures = new File(required("dsh.it.fixtures"));
		File reports = new File(required("dsh.it.newmanReports"));

		File[] found = collections.listFiles((dir, name) -> name.endsWith(COLLECTION_SUFFIX));
		if (found == null || found.length == 0) {
			fail("No *" + COLLECTION_SUFFIX + " in " + collections.getAbsolutePath()
					+ ". An empty directory is a failure, not a pass (#137).");
		}
		Arrays.sort(found);
		return Arrays.stream(found).map(collection -> DynamicTest.dynamicTest(collection.getName(),
				() -> run(node, newman, collection, baseUrl, fixtures, reports)));
	}

	private static void run(File node, File newman, File collection, String baseUrl, File fixtures, File reports)
			throws IOException, InterruptedException {
		String fileName = collection.getName();
		String name = fileName.substring(0, fileName.length() - COLLECTION_SUFFIX.length());
		Files.createDirectories(reports.toPath());
		File json = new File(reports, name + ".json");
		File log = new File(reports, name + ".log");
		Files.deleteIfExists(json.toPath());

		// Every path absolute: newman refuses a relative working directory under --no-insecure-file-read.
		List<String> command = List.of(node.getAbsolutePath(), newman.getAbsolutePath(), "run",
				collection.getAbsolutePath(),
				"--env-var", "baseUrl=" + baseUrl,
				"--working-dir", fixtures.getAbsolutePath(), "--no-insecure-file-read",
				"--reporters", "cli,json,junit",
				"--reporter-json-export", json.getAbsolutePath(),
				"--reporter-junit-export", new File(reports, name + ".xml").getAbsolutePath(),
				"--timeout-request", REQUEST_TIMEOUT_MILLIS,
				"--color", "off");
		// To a file, not a pipe: a full pipe buffer would block newman.
		Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start();
		if (!process.waitFor(RUN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
			process.destroyForcibly();
			fail("newman did not finish " + fileName + " within " + RUN_TIMEOUT_MINUTES + " minutes; see " + log);
		}
		String output = Files.readString(log.toPath(), StandardCharsets.UTF_8);

		assertThat(process.exitValue()).as("newman's exit code for %s. Its output:%n%s", fileName, output).isZero();
		JsonNode assertions = new ObjectMapper().readTree(json).path("run").path("stats").path("assertions");
		assertThat(assertions.path("total").asInt())
				.as("assertions in %s: newman passes a collection that asserts nothing", fileName)
				.isPositive();
	}

	/** Node's binary is node.exe on Windows and node elsewhere. */
	private static File node(File nodeDir) {
		File windows = new File(nodeDir, "node.exe");
		return windows.isFile() ? windows : new File(nodeDir, "node");
	}

	private static String required(String property) {
		String value = System.getProperty(property);
		if (value == null) {
			fail(property + " is not set. Run this class under 'mvn -B install -DintegrationTests', "
					+ "which installs newman and starts the server it calls.");
		}
		return value;
	}
}

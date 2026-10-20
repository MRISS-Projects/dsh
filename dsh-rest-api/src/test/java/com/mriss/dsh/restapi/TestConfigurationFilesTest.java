package com.mriss.dsh.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Checks the configuration files dsh-rest-api's context tests read (#140, rule R10 of
 * specs/architecture/profiles-and-configuration.md). A test application.properties would replace
 * the main one entirely (P24), so test values go in application-test.properties, which merges
 * with it (P25) and must repeat none of its keys.
 */
public class TestConfigurationFilesTest {

    @Test
    public void applicationProperties_onlyTheMainFileIsOnTheClasspath() throws Exception {
        List<Path> found = new ArrayList<>();
        for (URL url : Collections.list(
                getClass().getClassLoader().getResources("application.properties"))) {
            found.add(Paths.get(url.toURI()));
        }
        assertEquals("only the main application.properties may be on the test classpath: "
                + "test values go in application-test.properties (#140)",
                Collections.singletonList(mainDirectory().resolve("application.properties")),
                found);
    }

    @Test
    public void testProfileFile_repeatsNoKeyOfTheMainFile() throws Exception {
        URL testFile = getClass().getClassLoader().getResource("application-test.properties");
        assertNotNull("application-test.properties is missing from the test classpath (#140)",
                testFile);
        Set<String> repeated = new TreeSet<>(load(Paths.get(testFile.toURI())).stringPropertyNames());
        repeated.retainAll(load(mainDirectory().resolve("application.properties")).stringPropertyNames());
        assertTrue("application-test.properties repeats keys of the main application.properties, "
                + "which it is merged with (P25): " + repeated, repeated.isEmpty());
    }

    private static Properties load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Path mainDirectory() throws URISyntaxException {
        return Paths.get(
                DshRestApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}

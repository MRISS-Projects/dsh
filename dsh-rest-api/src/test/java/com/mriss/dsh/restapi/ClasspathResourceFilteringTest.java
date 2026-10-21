package com.mriss.dsh.restapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * Checks what Maven's resource filtering leaves in dsh-rest-api's main output (#158).
 * The files are read from the directory {@link DshRestApplication} was loaded from,
 * so that nothing on the test classpath can stand in for the main output.
 */
public class ClasspathResourceFilteringTest {

    @Test
    public void applicationProperties_springPlaceholder_reachesTheJarAsWritten() throws Exception {
        assertTrue(mainResource("application.properties").contains("${project.version}"),
                "Maven replaced ${project.version} in application.properties: "
                + "classpath resources must be filtered with @...@ only (#158)");
    }

    @Test
    public void applicationProperties_mavenToken_isReplaced() throws Exception {
        assertFalse(mainResource("application.properties").contains("@project.version@"),
                "@project.version@ was not replaced: dsh-rest-api's src/main/resources "
                + "must stay filtered, so that the rule of #158 is exercised by every build");
    }

    @Test
    public void enqueueContext_springPlaceholders_reachTheJarAsWritten() throws Exception {
        String context = mainResource("enqueue-docId-context.xml");
        assertTrue(context.contains("${spring.rabbitmq.host:localhost}"),
                "Maven replaced ${spring.rabbitmq.host:localhost} in enqueue-docId-context.xml: "
                + "classpath resources must be filtered with @...@ only (#158)");
        assertTrue(context.contains("${spring.rabbitmq.port:5672}"),
                "Maven replaced ${spring.rabbitmq.port:5672} in enqueue-docId-context.xml: "
                + "classpath resources must be filtered with @...@ only (#158)");
    }

    private static String mainResource(String name) throws IOException, URISyntaxException {
        Path classes = Paths.get(
                DshRestApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new String(Files.readAllBytes(classes.resolve(name)), StandardCharsets.UTF_8);
    }
}

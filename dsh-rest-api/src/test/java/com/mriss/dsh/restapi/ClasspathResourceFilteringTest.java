package com.mriss.dsh.restapi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Checks what Maven's resource filtering leaves in dsh-rest-api's main output (#158).
 * The files are read from the directory {@link DshRestApplication} was loaded from,
 * because src/test/resources holds an application.properties that shadows the main one.
 */
public class ClasspathResourceFilteringTest {

    @Test
    public void applicationProperties_springPlaceholder_reachesTheJarAsWritten() throws Exception {
        assertTrue("Maven replaced ${project.version} in application.properties: "
                + "classpath resources must be filtered with @...@ only (#158)",
                mainResource("application.properties").contains("${project.version}"));
    }

    @Test
    public void applicationProperties_mavenToken_isReplaced() throws Exception {
        assertFalse("@project.version@ was not replaced: dsh-rest-api's src/main/resources "
                + "must stay filtered, so that the rule of #158 is exercised by every build",
                mainResource("application.properties").contains("@project.version@"));
    }

    @Test
    public void enqueueContext_springPlaceholders_reachTheJarAsWritten() throws Exception {
        String context = mainResource("enqueue-docId-context.xml");
        assertTrue("Maven replaced ${spring.rabbitmq.host:localhost} in enqueue-docId-context.xml: "
                + "classpath resources must be filtered with @...@ only (#158)",
                context.contains("${spring.rabbitmq.host:localhost}"));
        assertTrue("Maven replaced ${spring.rabbitmq.port:5672} in enqueue-docId-context.xml: "
                + "classpath resources must be filtered with @...@ only (#158)",
                context.contains("${spring.rabbitmq.port:5672}"));
    }

    private static String mainResource(String name) throws IOException, URISyntaxException {
        Path classes = Paths.get(
                DshRestApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new String(Files.readAllBytes(classes.resolve(name)), StandardCharsets.UTF_8);
    }
}

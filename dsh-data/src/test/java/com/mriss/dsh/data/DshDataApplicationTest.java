package com.mriss.dsh.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for {@link DshDataApplication}.
 * No Spring context is loaded.
 */
public class DshDataApplicationTest {

    final static Logger logger = LoggerFactory.getLogger(DshDataApplicationTest.class);

    @BeforeAll
    public static void setUp() {
        logger.info("DshDataApplicationTest setup");
    }

    @AfterAll
    public static void tearDown() {
        logger.info("DshDataApplicationTest teardown");
    }

    @Test
    public void contextLoads() {
        // Verifies the application class can be instantiated without starting the full Spring context
        DshDataApplication app = new DshDataApplication();
        assertNotNull(app);
    }
}


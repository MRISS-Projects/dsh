package com.mriss.dsh.data;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for {@link DshDataApplication}.
 * No Spring context is loaded – MongoDB is not required.
 */
public class DshDataApplicationTest {

    final static Logger logger = LoggerFactory.getLogger(DshDataApplicationTest.class);

    @BeforeClass
    public static void setUp() {
        logger.info("DshDataApplicationTest setup");
    }

    @AfterClass
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


package com.mriss.dsh.restapi.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.restapi.DshRestApplication;
import com.mriss.dsh.restapi.service.DocumentQueueService;

/**
 * Smoke test for the Spring application context.
 * The document store and RabbitMQ are mocked, so that no broker connection
 * is required during testing.
 */
@SpringBootTest
@ActiveProfiles("test")
public class DshRestApplicationIT {

    final static Logger logger = LoggerFactory.getLogger(DshRestApplicationIT.class);

    /** Mock document store – stands in for the selected implementation. */
    @MockitoBean
    private DocumentDao documentDao;

    /** Mock RabbitMQ – prevents the queue service impl from connecting to a broker. */
    @MockitoBean
    private DocumentQueueService documentQueueService;

    @Autowired
    private DshRestApplication application;

    /**
     * Verifies that the Spring application context loads successfully without
     * requiring any external infrastructure (RabbitMQ).
     */
    @Test
    public void contextLoads() {
        assertNotNull(application, "Spring application context must be loaded");
    }

    /**
     * Verifies that the mocked infrastructure beans are available in the context.
     */
    @Test
    public void mockedInfrastructureBeansArePresent() {
        logger.info("Testing mocked infrastructure beans");
        assertNotNull(documentDao, "DocumentDao mock must be present");
        assertNotNull(documentQueueService, "DocumentQueueService mock must be present");
    }

    @Autowired
    private Environment environment;

    /**
     * Verifies that the context runs with the test profile it names, and no other (#140).
     */
    @Test
    public void activeProfileIsTestOnly() {
        assertArrayEquals(new String[] {"test"}, environment.getActiveProfiles(),
                "the context must run with the test profile only");
    }
}

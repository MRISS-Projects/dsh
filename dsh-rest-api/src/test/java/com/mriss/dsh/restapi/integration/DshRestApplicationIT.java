package com.mriss.dsh.restapi.integration;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.restapi.DshRestApplication;
import com.mriss.dsh.restapi.service.DocumentQueueService;

/**
 * Smoke test for the Spring application context.
 * MongoDB and RabbitMQ are mocked because both infrastructures are deprecated
 * for this project – no real connections are required during testing.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DshRestApplicationIT {

    final static Logger logger = LoggerFactory.getLogger(DshRestApplicationIT.class);

    /** Mock MongoDB – prevents the DAO impl from trying to reach a real MongoDB instance. */
    @MockBean
    private DocumentDao documentDao;

    /** Mock RabbitMQ – prevents the queue service impl from connecting to a broker. */
    @MockBean
    private DocumentQueueService documentQueueService;

    @Autowired
    private DshRestApplication application;

    /**
     * Verifies that the Spring application context loads successfully without
     * requiring any external infrastructure (MongoDB, RabbitMQ).
     */
    @Test
    public void contextLoads() {
        assertNotNull("Spring application context must be loaded", application);
    }

    /**
     * Verifies that the mocked infrastructure beans are available in the context.
     */
    @Test
    public void mockedInfrastructureBeansArePresent() {
        logger.info("Testing mocked infrastructure beans");
        assertNotNull("DocumentDao mock must be present", documentDao);
        assertNotNull("DocumentQueueService mock must be present", documentQueueService);
    }
}

package com.mriss.dsh.restapi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.restapi.config.SwaggerConfig;
import com.mriss.dsh.restapi.dto.DocumentStatusDto;
import com.mriss.dsh.restapi.dto.TokenDto;
import com.mriss.dsh.restapi.service.DocumentQueueService;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Smoke test for the Spring application context.
 * MongoDB and RabbitMQ are mocked because both infrastructures are deprecated
 * for this project – no real connections are required during testing.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DshRestApplicationTest {

    final static Logger logger = LoggerFactory.getLogger(DshRestApplicationTest.class);

    /** Mock MongoDB – prevents the DAO impl from trying to reach a real MongoDB instance. */
    @MockBean
    private DocumentDao documentDao;

    /** Mock RabbitMQ – prevents the queue service impl from connecting to a broker. */
    @MockBean
    private DocumentQueueService documentQueueService;

    @Autowired
    private DshRestApplication application;

    @BeforeClass
    public static void setUp() {
        logger.info("DshRestApplicationTest setUp");
    }

    @AfterClass
    public static void tearDown() {
        logger.info("DshRestApplicationTest tearDown");
    }

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
    public void testSomething() {
        logger.info("Testing mocked infrastructure beans");
        assertNotNull("DocumentDao mock must be present", documentDao);
        assertNotNull("DocumentQueueService mock must be present", documentQueueService);
    }

    /**
     * Verifies DshRestApplication.configure() returns a non-null builder.
     */
    @Test
    public void testConfigure() {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        SpringApplicationBuilder result = application.configure(builder);
        assertNotNull("configure() must return a non-null builder", result);
    }

    /**
     * Verifies DocumentStatusDto accessors.
     */
    @Test
    public void testDocumentStatusDto() {
        DocumentStatusDto dto = new DocumentStatusDto();
        dto.setStatus("STATUS");
        dto.setMessage("MSG");
        assertEquals("STATUS", dto.getStatus());
        assertEquals("MSG", dto.getMessage());

        DocumentStatusDto dto2 = new DocumentStatusDto("S2", "M2");
        assertEquals("S2", dto2.getStatus());
        assertEquals("M2", dto2.getMessage());
    }

    /**
     * Verifies TokenDto accessors.
     */
    @Test
    public void testTokenDto() {
        TokenDto dto = new TokenDto();
        dto.setToken("TOKEN");
        dto.setMessage("MSG");
        assertEquals("TOKEN", dto.getToken());
        assertEquals("MSG", dto.getMessage());

        TokenDto dto2 = new TokenDto("T2", "M2");
        assertEquals("T2", dto2.getToken());
        assertEquals("M2", dto2.getMessage());
    }

    /**
     * Verifies SwaggerConfig produces a non-null Docket bean.
     */
    @Test
    public void testSwaggerConfig() {
        SwaggerConfig config = new SwaggerConfig();
        assertNotNull("Docket must not be null", config.newsApi());
    }
}

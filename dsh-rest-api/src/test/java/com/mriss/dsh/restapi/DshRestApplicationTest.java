package com.mriss.dsh.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.mriss.dsh.restapi.config.SwaggerConfig;
import com.mriss.dsh.restapi.dto.DocumentStatusDto;
import com.mriss.dsh.restapi.dto.TokenDto;

/**
 * Unit tests for {@link DshRestApplication}, its DTOs and {@link SwaggerConfig},
 * with no Spring context. The context itself is exercised by
 * {@code integration.DshRestApplicationIT}.
 */
public class DshRestApplicationTest {

    /**
     * Verifies main() hands its arguments to SpringApplication.run().
     */
    @Test
    public void main_delegatesToSpringApplicationRun() {
        String[] args = {"arg1"};
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            DshRestApplication.main(args);
            spring.verify(() -> SpringApplication.run(DshRestApplication.class, args));
        }
    }

    /**
     * Verifies DshRestApplication.configure() returns a non-null builder.
     */
    @Test
    public void testConfigure() {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        SpringApplicationBuilder result = new DshRestApplication().configure(builder);
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

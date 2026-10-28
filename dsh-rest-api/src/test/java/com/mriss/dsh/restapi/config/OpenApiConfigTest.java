package com.mriss.dsh.restapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OpenApiConfig}, with no Spring context. That springdoc serves the
 * description is checked by {@code integration.DocumentResourceHttpIT}.
 */
public class OpenApiConfigTest {

    @Test
    public void dshOpenApi_namesTheApi() {
        assertEquals("DSH - Document Smart Highlights API",
                new OpenApiConfig().dshOpenApi().getInfo().getTitle());
    }
}

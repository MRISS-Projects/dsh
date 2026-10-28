package com.mriss.dsh.restapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * The OpenAPI description springdoc serves at {@code /v3/api-docs}. Which paths it covers is set
 * by {@code springdoc.paths-to-match} in {@code application.properties}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dshOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DSH - Document Smart Highlights API")
                .description("DSH - Document Smart Highlights Application"));
    }
}

package com.mriss.dsh.restapi.config;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Springfox / Swagger2 configuration.
 *
 * <p>The {@code webMvcRequestHandlerProvider} override filters out handler
 * mappings that use PathPatternParser (introduced in Spring Boot 2.6), which
 * causes a {@link NullPointerException} inside Springfox 3.0.0 because those
 * mappings have a null {@code PatternsRequestCondition}.  The workaround
 * restricts Springfox to mappings backed by the legacy AntPathMatcher.</p>
 */
@EnableSwagger2
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket newsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("dsh-app")
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/v1/dsh.*"))
                .build();
    }

    /**
     * Override Springfox's default {@link WebMvcRequestHandlerProvider} to
     * exclude handler mappings that rely on PathPatternParser.  Without this
     * override, Spring Boot 2.6+ (which defaults to PathPatternParser) causes
     * a {@link NullPointerException} inside Springfox 3.0.0 at context
     * refresh time.
     */
    @Bean
    @Primary
    @Order(Integer.MIN_VALUE)
    public WebMvcRequestHandlerProvider webMvcRequestHandlerProvider(
            Optional<ServletContext> servletContext,
            HandlerMethodResolver methodResolver,
            List<RequestMappingInfoHandlerMapping> handlerMappings) {
        // Filter out mappings that use PathPatternParser (patternParser != null).
        // Springfox only supports AntPathMatcher-based mappings.
        List<RequestMappingInfoHandlerMapping> filtered = handlerMappings.stream()
                .filter(m -> m.getPatternParser() == null)
                .collect(Collectors.toList());
        return new WebMvcRequestHandlerProvider(servletContext, methodResolver, filtered);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("DSH - Document Smart Hightlights API")
                .description("DSH - Document Smart Hightlights Application")
                .build();
    }
}

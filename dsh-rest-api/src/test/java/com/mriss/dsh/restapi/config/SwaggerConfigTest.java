package com.mriss.dsh.restapi.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

/**
 * Unit tests for {@link SwaggerConfig}'s {@link WebMvcRequestHandlerProvider}
 * override, with no Spring context.
 */
public class SwaggerConfigTest {

    /**
     * Only the mapping backed by AntPathMatcher reaches Springfox; the one using
     * PathPatternParser is filtered out, so its handler methods are never read.
     */
    @Test
    public void webMvcRequestHandlerProvider_dropsPathPatternParserMappings() {
        RequestMappingInfoHandlerMapping antPathMapping = mock(RequestMappingInfoHandlerMapping.class);
        RequestMappingInfoHandlerMapping pathPatternMapping = mock(RequestMappingInfoHandlerMapping.class);
        when(antPathMapping.getPatternParser()).thenReturn(null);
        when(pathPatternMapping.getPatternParser()).thenReturn(new PathPatternParser());

        WebMvcRequestHandlerProvider provider = new SwaggerConfig().webMvcRequestHandlerProvider(
                Optional.empty(), mock(HandlerMethodResolver.class),
                Arrays.asList(antPathMapping, pathPatternMapping));
        provider.requestHandlers();

        verify(antPathMapping).getHandlerMethods();
        verify(pathPatternMapping, never()).getHandlerMethods();
    }
}

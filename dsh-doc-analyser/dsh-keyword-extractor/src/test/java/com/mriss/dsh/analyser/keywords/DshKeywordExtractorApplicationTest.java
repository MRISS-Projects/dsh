package com.mriss.dsh.analyser.keywords;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Unit tests for {@link DshKeywordExtractorApplication}, with no Spring context. The context itself is
 * exercised by {@code integration.DshKeywordExtractorApplicationIT}.
 */
public class DshKeywordExtractorApplicationTest {

    @Test
    public void main_delegatesToSpringApplicationRun() {
        String[] args = {"arg1", "arg2"};
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            DshKeywordExtractorApplication.main(args);
            spring.verify(() -> SpringApplication.run(DshKeywordExtractorApplication.class, args));
        }
    }

    @Test
    public void constructor_isInstantiable() {
        assertNotNull(new DshKeywordExtractorApplication());
    }
}

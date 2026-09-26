package com.mriss.dsh.analyser.topsentences;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Unit tests for {@link DshTopSentencesExtractorApplication}, with no Spring context. The context itself is
 * exercised by {@code integration.DshTopSentencesExtractorApplicationIT}.
 */
public class DshTopSentencesExtractorApplicationTest {

    @Test
    public void main_delegatesToSpringApplicationRun() {
        String[] args = {"arg1", "arg2"};
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            DshTopSentencesExtractorApplication.main(args);
            spring.verify(() -> SpringApplication.run(DshTopSentencesExtractorApplication.class, args));
        }
    }

    @Test
    public void constructor_isInstantiable() {
        assertNotNull(new DshTopSentencesExtractorApplication());
    }
}

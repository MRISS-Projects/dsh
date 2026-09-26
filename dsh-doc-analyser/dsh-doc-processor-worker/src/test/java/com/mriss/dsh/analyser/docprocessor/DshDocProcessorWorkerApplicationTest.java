package com.mriss.dsh.analyser.docprocessor;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Unit tests for {@link DshDocProcessorWorkerApplication}, with no Spring context. The context itself is
 * exercised by {@code integration.DshDocProcessorWorkerApplicationIT}.
 */
public class DshDocProcessorWorkerApplicationTest {

    @Test
    public void main_delegatesToSpringApplicationRun() {
        String[] args = {"arg1", "arg2"};
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            DshDocProcessorWorkerApplication.main(args);
            spring.verify(() -> SpringApplication.run(DshDocProcessorWorkerApplication.class, args));
        }
    }

    @Test
    public void constructor_isInstantiable() {
        assertNotNull(new DshDocProcessorWorkerApplication());
    }
}

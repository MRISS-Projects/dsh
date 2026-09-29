package com.mriss.dsh.docindexer;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Unit tests for {@link DshDocIndexerApplication}, with no Spring context. The context itself is
 * exercised by {@code integration.DshDocIndexerApplicationIT}.
 */
public class DshDocIndexerApplicationTest {

    @Test
    public void main_delegatesToSpringApplicationRun() {
        String[] args = {"arg1", "arg2"};
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            DshDocIndexerApplication.main(args);
            spring.verify(() -> SpringApplication.run(DshDocIndexerApplication.class, args));
        }
    }

    @Test
    public void constructor_isInstantiable() {
        assertNotNull(new DshDocIndexerApplication());
    }
}

package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.document.dao.memory.InMemoryDocumentDao;
import com.mriss.dsh.restapi.DshRestApplication;

/**
 * Which {@link DocumentDao} the application runs with, by {@code dsh.persistence} (ADR-003,
 * section 3). Starts the application as {@code main} does, once per case, with the {@code test}
 * profile as an argument: {@code @ActiveProfiles} does not reach a {@link SpringApplicationBuilder},
 * and an argument outranks a profile leaked from the environment (#140). A case that passes its
 * own {@code --spring.profiles.active} adds to {@code test} rather than replacing it: Spring joins
 * a repeated option's values.
 */
public class PersistenceSelectionIT {

	private ConfigurableApplicationContext context;

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void startup_whenPersistenceKeyAbsent_shouldUseTheInMemoryStore() {
		context = start();

		assertThat(context.getEnvironment().containsProperty("dsh.persistence"))
				.as("dsh.persistence must be absent, or this case does not exercise matchIfMissing")
				.isFalse();
		assertOnlyStoreIsInMemory();
	}

	@Test
	public void startup_whenPersistenceIsMemory_shouldUseTheInMemoryStore() {
		context = start("--dsh.persistence=memory");

		assertOnlyStoreIsInMemory();
	}

	@Test
	public void startup_whenPersistenceMatchesNoStore_shouldFail() {
		Throwable thrown = catchThrowable(() -> context = start("--dsh.persistence=mongodb"));

		assertThat(thrown).as("the startup must fail").isNotNull();
		Throwable cause = NestedExceptionUtils.getMostSpecificCause(thrown);
		assertThat(cause).isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThat(((NoSuchBeanDefinitionException) cause).getBeanType()).isEqualTo(DocumentDao.class);
	}

	@Test
	public void startup_shouldRunWithTheTestProfileOnly() {
		context = start();

		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("test");
	}

	private void assertOnlyStoreIsInMemory() {
		Map<String, DocumentDao> stores = context.getBeansOfType(DocumentDao.class);
		assertThat(stores).hasSize(1);
		assertThat(stores.values().iterator().next()).isInstanceOf(InMemoryDocumentDao.class);
	}

	private static ConfigurableApplicationContext start(String... arguments) {
		String[] all = new String[arguments.length + 2];
		all[0] = "--server.port=0";
		all[1] = "--spring.profiles.active=test";
		System.arraycopy(arguments, 0, all, 2, arguments.length);
		return new SpringApplicationBuilder(DshRestApplication.class).run(all);
	}
}

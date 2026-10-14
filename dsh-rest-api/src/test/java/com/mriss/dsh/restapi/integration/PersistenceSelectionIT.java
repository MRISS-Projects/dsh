package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.document.dao.memory.InMemoryDocumentDao;
import com.mriss.dsh.restapi.DshRestApplication;

/**
 * Which {@link DocumentDao} the application runs with, by {@code dsh.persistence} (ADR-003,
 * section 3). Starts the application as {@code main} does, once per case.
 */
public class PersistenceSelectionIT {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void startup_whenPersistenceKeyAbsent_shouldUseTheInMemoryStore() {
		context = start();

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

	private void assertOnlyStoreIsInMemory() {
		Map<String, DocumentDao> stores = context.getBeansOfType(DocumentDao.class);
		assertThat(stores).hasSize(1);
		assertThat(stores.values().iterator().next()).isInstanceOf(InMemoryDocumentDao.class);
	}

	private static ConfigurableApplicationContext start(String... arguments) {
		String[] all = new String[arguments.length + 1];
		all[0] = "--server.port=0";
		System.arraycopy(arguments, 0, all, 1, arguments.length);
		return new SpringApplicationBuilder(DshRestApplication.class).run(all);
	}
}

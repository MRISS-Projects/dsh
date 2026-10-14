package com.mriss.dsh.data.document.dao.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.mriss.dsh.data.models.Document;

public class InMemoryDocumentDaoTest {

	private InMemoryDocumentDao dao;

	@Before
	public void setUp() {
		dao = new InMemoryDocumentDao();
	}

	@Test
	public void storeDocument_whenNoId_shouldReturnTheIdAndSetItOnTheSameInstance() throws Exception {
		Document document = document("a");

		String id = dao.storeDocument(document);

		assertThat(id).isNotBlank();
		assertThat(document.getId()).isEqualTo(id);
	}

	@Test
	public void storeDocument_whenIdAlreadySet_shouldStoreUnderThatId() throws Exception {
		Document document = document("a");
		document.setId("given-id");

		String id = dao.storeDocument(document);

		assertThat(id).isEqualTo("given-id");
		assertThat(dao.findDocumentById("given-id")).isSameAs(document);
	}

	@Test
	public void storeDocument_whenStoredAgain_shouldReplaceTheRecordNotAddOne() throws Exception {
		Document document = document("a");
		String first = dao.storeDocument(document);

		String second = dao.storeDocument(document);

		assertThat(second).isEqualTo(first);
		assertThat(dao.findDocumentsByHash(document.getFileHash())).containsExactly(document);
	}

	@Test
	public void storeDocument_whenNull_shouldThrowIllegalArgumentException() {
		assertThatThrownBy(() -> dao.storeDocument(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void findDocumentById_whenStored_shouldReturnIt() throws Exception {
		Document document = document("a");
		String id = dao.storeDocument(document);

		assertThat(dao.findDocumentById(id)).isSameAs(document);
	}

	@Test
	public void findDocumentById_whenUnknown_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentById("unknown")).isNull();
	}

	@Test
	public void findDocumentByToken_whenStored_shouldReturnIt() throws Exception {
		Document document = document("a");
		dao.storeDocument(document("b"));
		dao.storeDocument(document);

		assertThat(dao.findDocumentByToken(document.getToken())).isSameAs(document);
	}

	@Test
	public void findDocumentByToken_whenUnknown_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentByToken("unknown")).isNull();
	}

	@Test
	public void findDocumentsByHash_whenStored_shouldReturnEveryDocumentWithThatHash() throws Exception {
		Document first = document("same");
		Document second = document("same");
		Document other = document("other");
		dao.storeDocument(first);
		dao.storeDocument(second);
		dao.storeDocument(other);

		List<Document> found = dao.findDocumentsByHash(first.getFileHash());

		assertThat(found).hasSize(2);
		assertThat(found).extracting(Document::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
	}

	@Test
	public void findDocumentsByHash_whenUnknown_shouldReturnAnEmptyList() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentsByHash("unknown")).isNotNull().isEmpty();
	}

	@Test
	public void clearDocuments_whenDocumentsStored_shouldRemoveEveryOne() throws Exception {
		Document document = document("a");
		String id = dao.storeDocument(document);
		dao.storeDocument(document("b"));

		dao.clearDocuments();

		assertThat(dao.findDocumentById(id)).isNull();
		assertThat(dao.findDocumentByToken(document.getToken())).isNull();
		assertThat(dao.findDocumentsByHash(document.getFileHash())).isEmpty();
	}

	@Test
	public void storeDocument_whenManyThreadsStoreAtOnce_shouldKeepEveryDocumentUnderAnIdOfItsOwn() throws Exception {
		int threads = 8;
		int perThread = 50;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<List<Document>>> results = new ArrayList<>();
		try {
			for (int t = 0; t < threads; t++) {
				int thread = t;
				results.add(pool.submit(() -> {
					start.await();
					List<Document> stored = new ArrayList<>();
					for (int i = 0; i < perThread; i++) {
						Document document = document("thread-" + thread + "-document-" + i);
						dao.storeDocument(document);
						stored.add(document);
					}
					return stored;
				}));
			}
			start.countDown();

			Set<String> ids = new HashSet<>();
			for (Future<List<Document>> result : results) {
				for (Document document : result.get(30, TimeUnit.SECONDS)) {
					assertThat(dao.findDocumentById(document.getId())).isSameAs(document);
					ids.add(document.getId());
				}
			}
			assertThat(ids).hasSize(threads * perThread);
		} finally {
			pool.shutdownNow();
		}
	}

	private static Document document(String contents) throws Exception {
		return new Document(contents.getBytes(StandardCharsets.UTF_8), "title of " + contents);
	}
}

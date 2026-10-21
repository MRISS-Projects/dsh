package com.mriss.dsh.data.document.dao.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mriss.dsh.data.models.Document;
import com.mriss.dsh.data.models.DocumentStatus;

public class InMemoryDocumentDaoTest {

	private InMemoryDocumentDao dao;

	@BeforeEach
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
		assertThat(dao.findDocumentByHash(document.getFileHash())).isSameAs(document);
		assertThat(dao.findDocumentByToken(document.getToken())).isSameAs(document);
	}

	@Test
	public void storeDocument_whenNull_shouldThrowIllegalArgumentException() {
		assertThatThrownBy(() -> dao.storeDocument(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void storeDocument_whenNoFileHash_shouldThrowIllegalArgumentException() throws Exception {
		Document document = new Document("no contents");

		assertThatThrownBy(() -> dao.storeDocument(document)).isInstanceOf(IllegalArgumentException.class);
		assertThat(document.getId()).isNull();
	}

	@Test
	public void storeDocument_whenNoToken_shouldThrowIllegalArgumentException() throws Exception {
		Document document = documentWithToken(null, "a");

		assertThatThrownBy(() -> dao.storeDocument(document)).isInstanceOf(IllegalArgumentException.class);
		assertThat(document.getId()).isNull();
	}

	@Test
	public void storeDocument_whenAnotherDocumentHasTheSameFileHash_shouldRejectIt() throws Exception {
		Document first = document("same");
		dao.storeDocument(first);
		Document second = document("same");

		assertThatThrownBy(() -> dao.storeDocument(second)).isInstanceOf(IllegalStateException.class);

		assertThat(second.getId()).isNull();
		assertThat(dao.findDocumentByHash(first.getFileHash())).isSameAs(first);
		assertThat(dao.findDocumentByToken(second.getToken())).isNull();
	}

	@Test
	public void storeDocument_whenAnotherDocumentHasTheSameToken_shouldRejectIt() throws Exception {
		Document first = documentWithToken("shared-token", "first");
		dao.storeDocument(first);
		Document second = documentWithToken("shared-token", "second");

		assertThatThrownBy(() -> dao.storeDocument(second)).isInstanceOf(IllegalStateException.class);

		assertThat(second.getId()).isNull();
		assertThat(dao.findDocumentByToken("shared-token")).isSameAs(first);
		assertThat(dao.findDocumentByHash(second.getFileHash())).isNull();
	}

	@Test
	public void storeDocument_whenAnotherDocumentTakesAStoredIdWithOtherKeys_shouldRejectIt() throws Exception {
		Document stored = document("stored");
		String id = dao.storeDocument(stored);
		Document other = document("other");
		other.setId(id);

		assertThatThrownBy(() -> dao.storeDocument(other)).isInstanceOf(IllegalStateException.class);

		assertThat(dao.findDocumentById(id)).isSameAs(stored);
		assertThat(dao.findDocumentByHash(other.getFileHash())).isNull();
		assertThat(dao.findDocumentByToken(other.getToken())).isNull();
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
	public void findDocumentById_whenNull_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentById(null)).isNull();
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
	public void findDocumentByToken_whenNull_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentByToken(null)).isNull();
	}

	@Test
	public void findDocumentByHash_whenStored_shouldReturnIt() throws Exception {
		Document document = document("a");
		dao.storeDocument(document("b"));
		dao.storeDocument(document);

		assertThat(dao.findDocumentByHash(document.getFileHash())).isSameAs(document);
	}

	@Test
	public void findDocumentByHash_whenUnknown_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentByHash("unknown")).isNull();
	}

	@Test
	public void findDocumentByHash_whenNull_shouldReturnNull() throws Exception {
		dao.storeDocument(document("a"));

		assertThat(dao.findDocumentByHash(null)).isNull();
	}

	@Test
	public void clearDocuments_whenDocumentsStored_shouldRemoveEveryOne() throws Exception {
		Document document = document("a");
		String id = dao.storeDocument(document);
		dao.storeDocument(document("b"));

		dao.clearDocuments();

		assertThat(dao.findDocumentById(id)).isNull();
		assertThat(dao.findDocumentByToken(document.getToken())).isNull();
		assertThat(dao.findDocumentByHash(document.getFileHash())).isNull();
		assertThat(dao.storeDocument(document("a"))).isNotBlank();
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

	@Test
	public void storeDocument_whenManyThreadsStoreTheSameFileAtOnce_shouldAcceptExactlyOne() throws Exception {
		int threads = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Document>> results = new ArrayList<>();
		try {
			for (int t = 0; t < threads; t++) {
				Document document = document("same");
				results.add(pool.submit(() -> {
					start.await();
					try {
						dao.storeDocument(document);
						return document;
					} catch (IllegalStateException rejected) {
						return null;
					}
				}));
			}
			start.countDown();

			List<Document> accepted = new ArrayList<>();
			for (Future<Document> result : results) {
				Document document = result.get(30, TimeUnit.SECONDS);
				if (document != null) {
					accepted.add(document);
				}
			}
			assertThat(accepted).hasSize(1);
			assertThat(dao.findDocumentByHash(accepted.get(0).getFileHash())).isSameAs(accepted.get(0));
		} finally {
			pool.shutdownNow();
		}
	}

	private static Document document(String contents) throws Exception {
		return new Document(contents.getBytes(StandardCharsets.UTF_8), "title of " + contents);
	}

	private static Document documentWithToken(String token, String contents) throws Exception {
		return new Document(null, "title of " + contents, token,
				new TreeSet<>(), new TreeSet<>(), contents.getBytes(StandardCharsets.UTF_8),
				DocumentStatus.CREATED, DocumentStatus.CREATED.getStatusMessage());
	}
}

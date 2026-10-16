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
	public void storeDocument_whenIndexKeysChange_shouldUpdateTheIndexes() throws Exception {
		Document document = document("before");
		String id = dao.storeDocument(document);
		Document sameHash = document("before");
		dao.storeDocument(sameHash);
		String oldHash = document.getFileHash();
		String oldToken = document.getToken();

		document.setOriginalFileContents("after".getBytes(StandardCharsets.UTF_8));
		List<Document> oldHashMatches = dao.findDocumentsByHash(oldHash);
		assertThat(oldHashMatches).hasSize(1);
		assertThat(oldHashMatches.get(0)).isSameAs(sameHash);
		dao.storeDocument(document);

		oldHashMatches = dao.findDocumentsByHash(oldHash);
		assertThat(oldHashMatches).hasSize(1);
		assertThat(oldHashMatches.get(0)).isSameAs(sameHash);
		assertThat(dao.findDocumentsByHash(document.getFileHash())).containsExactly(document);
		assertThat(dao.findDocumentByToken(oldToken)).isSameAs(document);

		Document replacement = document("replacement");
		replacement.setId(id);
		dao.storeDocument(replacement);

		assertThat(dao.findDocumentsByHash(document.getFileHash())).isEmpty();
		assertThat(dao.findDocumentsByHash(replacement.getFileHash())).containsExactly(replacement);
		assertThat(dao.findDocumentByToken(oldToken)).isNull();
		assertThat(dao.findDocumentByToken(replacement.getToken())).isSameAs(replacement);
	}

	@Test
	public void storeDocument_whenHashAndTokenAreNull_shouldIndexAndClearThem() throws Exception {
		Document document = new Document();

		dao.storeDocument(document);

		assertThat(dao.findDocumentsByHash(null)).containsExactly(document);
		assertThat(dao.findDocumentByToken(null)).isSameAs(document);

		Document replacement = document("replacement");
		replacement.setId(document.getId());
		dao.storeDocument(replacement);

		assertThat(dao.findDocumentsByHash(null)).isEmpty();
		assertThat(dao.findDocumentByToken(null)).isNull();
		assertThat(dao.findDocumentsByHash(replacement.getFileHash())).containsExactly(replacement);

		dao.clearDocuments();

		assertThat(dao.findDocumentsByHash(null)).isEmpty();
		assertThat(dao.findDocumentByToken(null)).isNull();
	}

	@Test
	public void findDocumentByToken_whenNullTokenWasChangedBeforeRestoring_shouldNotReturnIt() {
		MutableTokenDocument document = new MutableTokenDocument();
		dao.storeDocument(document);

		document.token = "changed";

		assertThat(dao.findDocumentByToken(null)).isNull();
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

	@Test
	public void storeDocument_whenConcurrentWritesReplaceSameId_shouldKeepOnlyCurrentIndexes() throws Exception {
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
						Document document = document("shared-" + thread + "-" + i);
						document.setId("shared-id");
						dao.storeDocument(document);
						stored.add(document);
					}
					return stored;
				}));
			}
			start.countDown();

			List<Document> stored = new ArrayList<>();
			for (Future<List<Document>> result : results) {
				stored.addAll(result.get(30, TimeUnit.SECONDS));
			}

			Document current = dao.findDocumentById("shared-id");
			assertThat(dao.findDocumentsByHash(current.getFileHash())).containsExactly(current);
			assertThat(dao.findDocumentByToken(current.getToken())).isSameAs(current);
			for (Document document : stored) {
				if (document != current) {
					assertThat(dao.findDocumentsByHash(document.getFileHash())).isEmpty();
					assertThat(dao.findDocumentByToken(document.getToken())).isNull();
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}

	private static Document document(String contents) throws Exception {
		return new Document(contents.getBytes(StandardCharsets.UTF_8), "title of " + contents);
	}

	private static final class MutableTokenDocument extends Document {
		private String token;

		@Override
		public String getToken() {
			return token;
		}
	}
}

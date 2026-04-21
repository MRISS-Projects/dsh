# ADR-001: Adopt Google Cloud Platform Components for DSH Infrastructure

| Field       | Value                        |
|-------------|------------------------------|
| **Date**    | 2026-04-12                   |
| **Status**  | Proposed                     |
| **Authors** | Backend Development Team     |

---

## Context

Document Smart Highlights (DSH) currently relies on self-managed, on-premise infrastructure components:

| Component   | Current Role in DSH |
|-------------|---------------------|
| **MongoDB** | Primary document store — persists `Document` entities (metadata, keywords, sentences, original file binary contents, processing status) via Spring Data `MongoRepository` through the `dsh-data` module. |
| **RabbitMQ** | Asynchronous message queue — `dsh-rest-api` enqueues document IDs via Spring Integration AMQP (`enqueue-docId-context.xml`); `dsh-doc-indexer-worker` and `dsh-doc-processor-worker` dequeue and process them. |
| **Apache Solr** | Full-text search index — custom Solr search components (`OrderedTermVectorComponent`, `AdvancedNumberFilter`) for sorted term vectors and token filtering; document content indexing for search. |

These components require manual provisioning, patching, scaling, and monitoring. As DSH moves to **Google Cloud Platform (GCP)** with all Spring Boot services running on **Cloud Run**, the infrastructure must be modernised to cloud-native, fully managed equivalents that align with a serverless deployment model.

### Motivation

1. **Operational overhead** — Self-managed MongoDB, RabbitMQ, and Solr clusters require significant operational investment.
2. **Scalability** — Cloud Run auto-scales the application tier, but the backing services remain fixed-capacity bottlenecks.
3. **Cloud-native alignment** — Using GCP-managed services alongside Cloud Run provides unified IAM, monitoring, and networking.
4. **Cost efficiency** — Serverless/managed services follow a pay-per-use model that better matches variable document processing workloads.

---

## Decision

### 1. Deprecate Current Implementations — Do Not Remove

All existing MongoDB, RabbitMQ, and Solr integration code will be **deprecated**, not removed. Deprecated classes and configurations must be annotated with `@Deprecated` and include a Javadoc `@deprecated` tag pointing to the replacement.

### 2. Interface-First / Bridge Design Pattern

All new GCP-based implementations must be introduced behind **interfaces** (or the Bridge design pattern where appropriate). The current (deprecated) implementation and the new GCP implementation will both satisfy the same contract, selectable via Spring profiles or configuration.

#### Current Interface Inventory

The codebase already follows an interface-based approach in several areas:

| Interface | Current Implementation | Module | Status |
|-----------|----------------------|--------|--------|
| `DocumentDao` | `MongoDocumentDao` | `dsh-data` | ✅ Interface exists — ready for a new GCP implementation |
| `DocumentQueueService` | `DocumentQueueServiceImpl` (RabbitMQ) | `dsh-rest-api` | ✅ Interface exists — ready for a new GCP implementation |
| `DocumentHandlingService` | `DocumentHandlingServiceImpl` | `dsh-rest-api` | ✅ Interface exists — no direct infrastructure coupling (delegates to `DocumentDao`) |
| `DocumentSubmissionService` | `DocumentSubmissionServiceImpl` | `dsh-rest-api` | ✅ Interface exists — orchestrates `DocumentHandlingService` and `DocumentQueueService` |
| `DocumentRepository` | Extends `MongoRepository<Document, String>` | `dsh-data` | ⚠️ Tightly coupled to MongoDB — needs a technology-neutral repository interface |

##### Components Requiring Refactoring Before GCP Implementation

| Component | Issue | Required Refactoring |
|-----------|-------|---------------------|
| `DocumentRepository` (`dsh-data`) | Directly extends `MongoRepository`, binding the interface to MongoDB. | Extract a technology-neutral `DocumentPersistenceRepository` interface with methods `save`, `findById`, `findByToken`, `findByFileHash`, `deleteAll`. Create `MongoDocumentPersistenceRepository` (deprecated) wrapping the current `DocumentRepository`, and a new `FirestoreDocumentPersistenceRepository`. |
| `enqueue-docId-context.xml` / `dequeue-docId-context.xml` | Spring Integration AMQP XML configuration hardcoded for RabbitMQ (`int-amqp:outbound-channel-adapter`, `rabbit:*` beans). | Replace with Java-based `@Configuration` classes behind a profile. Create `RabbitMqIntegrationConfig` (deprecated, `@Profile("rabbitmq")`) and `PubSubIntegrationConfig` (`@Profile("gcp")`). |
| `OrderedTermVectorComponent` (`dsh-solr`) | A custom Solr `SearchComponent` plugin — extends `TermVectorComponent` directly, tightly coupled to Solr internals (`ResponseBuilder`, `SolrParams`, `NamedList`). | Extract the **sorting logic** (term vector ordering by TF/DF/TF-IDF, ascending/descending) into a technology-neutral `TermVectorOrderingService` interface. The Solr-specific wrapper becomes a deprecated adapter; the new GCP adapter will call Vertex AI Search and apply the same ordering in the application layer. |
| `AdvancedNumberFilter` / `AdvancedNumberFilterFactory` (`dsh-solr`) | Lucene `TokenFilter` plugin — coupled to Solr/Lucene analysis chain. | Evaluate whether Vertex AI Search's built-in tokenisation covers this use case. If not, implement an equivalent pre-processing step in the indexing pipeline behind an interface. |

### 3. GCP Component Mapping

#### 3.1 MongoDB → Firestore (Native Mode) + Google Cloud Storage

| Aspect | Detail |
|--------|--------|
| **Document metadata & highlights** | Stored in **Firestore** collections. The `Document` model entity will be annotated with `@com.google.cloud.spring.data.firestore.Document`. |
| **Original file binary contents** | Stored in **Google Cloud Storage (GCS)**. The `Document` entity's `byte[] originalFileContents` field (up to 50 MB per FR001 in `document-analysis.md`) will be replaced with a `String fileStorageUri` pointing to the GCS object. This is required because Firestore has a **1 MB document size limit**. |
| **Spring dependency** | `spring-cloud-gcp-starter-data-firestore`, `spring-cloud-gcp-starter-storage` |
| **Repository** | New `FirestoreDocumentPersistenceRepository` implementing `DocumentPersistenceRepository` |

#### 3.2 RabbitMQ → Google Cloud Pub/Sub

| Aspect | Detail |
|--------|--------|
| **Messaging** | Replace AMQP channels with **Pub/Sub** topics and subscriptions. The REST API publishes document IDs to a Pub/Sub topic; the indexer worker receives them via a push or pull subscription. |
| **Delivery semantics** | Pub/Sub supports at-least-once delivery, matching FR001 in `indexing-workflow.md`. Dead-letter topics handle the "retry up to 3 times" requirement natively. |
| **Spring dependency** | `spring-cloud-gcp-starter-pubsub`, `spring-integration-gcp` |
| **Cloud Run integration** | Pub/Sub can push messages directly to Cloud Run via HTTP endpoints, eliminating the need for the worker to poll. |
| **Configuration** | New `PubSubIntegrationConfig` (`@Profile("gcp")`) replaces `enqueue-docId-context.xml`. |

#### 3.3 Apache Solr → Vertex AI Search

| Aspect | Detail |
|--------|--------|
| **Full-text search** | Replace Solr core with **Vertex AI Search** (formerly Discovery Engine / Enterprise Search). Fully managed, serverless, built-in NLP-based ranking. |
| **Spring dependency** | `google-cloud-discoveryengine` (Vertex AI Search client library) |
| **Custom component migration** | See §4 — Migration Plan for `OrderedTermVectorComponent`. |
| **Number filtering** | Evaluate Vertex AI Search's built-in tokenisation. If `AdvancedNumberFilter` behaviour is not covered, implement equivalent pre-processing in the indexing pipeline. |

### 4. Migration Plan

#### Phase 1 — Interface Extraction & Deprecation

**Goal**: Refactor all infrastructure-coupled code to use technology-neutral interfaces. Mark current implementations as deprecated.

| Task | Module | Detail |
|------|--------|--------|
| 1.1 | `dsh-data` | Create `DocumentPersistenceRepository` interface. Wrap existing `DocumentRepository` (Mongo) as `MongoDocumentPersistenceRepository implements DocumentPersistenceRepository`. Mark with `@Deprecated`. |
| 1.2 | `dsh-data` | Refactor `MongoDocumentDao` to depend on `DocumentPersistenceRepository` instead of `DocumentRepository` directly. Mark `MongoDocumentDao` with `@Deprecated`. |
| 1.3 | `dsh-rest-api` | Convert `enqueue-docId-context.xml` / `dequeue-docId-context.xml` to Java `@Configuration` classes. Create `RabbitMqIntegrationConfig` (`@Profile("rabbitmq")`). Mark with `@Deprecated`. |
| 1.4 | `dsh-rest-api` | Mark `DocumentQueueServiceImpl` (RabbitMQ) with `@Deprecated`, assign `@Profile("rabbitmq")`. |
| 1.5 | `dsh-solr` | Extract `TermVectorOrderingService` interface from `OrderedTermVectorComponent`'s sorting logic. The interface accepts a list of term-vector entries and order options, returns the sorted list. Mark the Solr adapter with `@Deprecated`. |
| 1.6 | `dsh-solr` | Evaluate `AdvancedNumberFilter`. If it can be expressed as a pre-indexing step, extract a `NumberFilterService` interface. Mark Solr implementation with `@Deprecated`. |
| 1.7 | `dsh-data` | Refactor `Document` entity: extract `byte[] originalFileContents` into a `FileStorageService` interface with methods `store(byte[]) → URI` and `retrieve(URI) → byte[]`. Create `LocalFileStorageService` (deprecated, for backward compatibility) and later `GcsFileStorageService`. |

#### Phase 2 — GCP Implementation: Firestore + GCS (MongoDB Replacement)

| Task | Module | Detail |
|------|--------|--------|
| 2.1 | `dsh-data` | Add `spring-cloud-gcp-starter-data-firestore` and `spring-cloud-gcp-starter-storage` dependencies. |
| 2.2 | `dsh-data` | Create `FirestoreDocumentPersistenceRepository implements DocumentPersistenceRepository`. Map `Document` fields to Firestore collections. |
| 2.3 | `dsh-data` | Create `FirestoreDocumentDao implements DocumentDao` (`@Profile("gcp")`). Wire to `FirestoreDocumentPersistenceRepository`. |
| 2.4 | `dsh-data` | Create `GcsFileStorageService implements FileStorageService` (`@Profile("gcp")`). Upload/download file bytes to a GCS bucket. The `Document` entity stores a `fileStorageUri` instead of raw bytes. |
| 2.5 | `dsh-data` | Write migration utility to bulk-copy existing MongoDB documents to Firestore and upload `originalFileContents` blobs to GCS, replacing the field with the resulting URI. |

#### Phase 3 — GCP Implementation: Cloud Pub/Sub (RabbitMQ Replacement)

| Task | Module | Detail |
|------|--------|--------|
| 3.1 | `dsh-rest-api` | Add `spring-cloud-gcp-starter-pubsub` and `spring-integration-gcp` dependencies. |
| 3.2 | `dsh-rest-api` | Create `PubSubIntegrationConfig` (`@Profile("gcp")`) — defines `PubSubTemplate`, outbound channel adapter to a `document-tasks` topic. |
| 3.3 | `dsh-rest-api` | Create `PubSubDocumentQueueServiceImpl implements DocumentQueueService` (`@Profile("gcp")`). Publishes document IDs to the Pub/Sub topic. |
| 3.4 | `dsh-doc-indexer-worker` | Replace `spring-boot-starter-amqp` with `spring-cloud-gcp-starter-pubsub`. Create a Pub/Sub subscriber (pull or push via Cloud Run HTTP endpoint) that invokes the existing processing pipeline. |
| 3.5 | `dsh-doc-analyser/dsh-doc-processor-worker` | Replace `spring-boot-starter-amqp` dependency. Wire Pub/Sub subscriber analogously to 3.4. |

#### Phase 4 — GCP Implementation: Vertex AI Search (Solr Replacement)

| Task | Module | Detail |
|------|--------|--------|
| 4.1 | `dsh-solr` (or new `dsh-search` module) | Add `google-cloud-discoveryengine` dependency. |
| 4.2 | New module / `dsh-solr` | Create `VertexAiSearchIndexingService` — indexes document content in a Vertex AI Search data store using the Document AI / ingestion APIs. |
| 4.3 | New module / `dsh-solr` | Create `VertexAiSearchQueryService` — executes full-text search queries via the Vertex AI Search serving API. |
| 4.4 | **`OrderedTermVectorComponent` migration** | Vertex AI Search does not expose raw term vectors (TF, DF, TF-IDF) like Solr's `TermVectorComponent`. The migration strategy is: |
|      | | **a)** At **indexing time**, compute TF/DF/TF-IDF statistics in the application layer (Java) using the extracted document text, *before* sending to Vertex AI Search. Store these statistics as structured metadata fields on the Vertex AI Search document or in Firestore. |
|      | | **b)** Extract the sorting logic currently in `SortedNamedList` / `TermsVectorComparator` / `Order` into the new `TermVectorOrderingService` implementation (`VertexAiTermVectorOrderingService`). This service retrieves the pre-computed term statistics from Firestore or Vertex AI Search metadata and applies the same ascending/descending sort by the requested field (TF, DF, TF-IDF). |
|      | | **c)** The `order` query parameter format (`order=tv.tf;desc`) is preserved in the REST API. The `dsh-rest-api` delegates to `TermVectorOrderingService`, which is now infrastructure-agnostic. |
|      | | **d)** Classes reusable as-is in the new implementation: `Order` (enum), `OrderOptions` (POJO), `TermsVectorComparator` (generic `Comparator<Object>` — only depends on `OrderOptions`). Only `OrderedTermVectorComponent` and `SortedNamedList` are Solr-coupled and need replacement. |
| 4.5 | **`AdvancedNumberFilter` migration** | Evaluate Vertex AI Search's built-in tokenisation/filtering. If number filtering is not natively supported, implement a `NumberFilterPreProcessor` in the indexing pipeline that strips or normalises numeric tokens before content is sent to Vertex AI Search. |

#### Phase 5 — Validation & Cutover

| Task | Detail |
|------|--------|
| 5.1 | Run both profiles (`rabbitmq`/`mongodb` and `gcp`) in parallel in a staging environment. Compare results for functional parity. |
| 5.2 | Execute integration tests from `specs/testing/test-plans/` against GCP profile. |
| 5.3 | Performance-test against benchmarks in `specs/testing/performance-benchmarks/` (FR004: 95% of docs < 10 MB within 30 s). |
| 5.4 | Switch default Spring profile to `gcp`. Deprecated implementations remain available via `@Profile("legacy")`. |

---

## Consequences

### Positive

- **Modernisation** — DSH moves to fully managed, cloud-native services that auto-scale with Cloud Run.
- **Reduced operational overhead** — No more self-managed MongoDB clusters, RabbitMQ brokers, or Solr nodes.
- **Better coupling to cloud-native components** — Unified GCP IAM, Cloud Logging, Cloud Monitoring, and VPC networking across all services.
- **Interface-driven architecture** — The Bridge/interface pattern improves testability and allows swapping implementations without changing business logic.
- **Backward compatibility** — Deprecated implementations are retained; teams can run the legacy stack locally or in environments without GCP.
- **Pay-per-use cost model** — Firestore, Pub/Sub, and Vertex AI Search charge based on usage, aligning cost with actual document processing volume.

### Negative

- **Migration complexity** — Dual implementations must be maintained during the transition period.
- **Firestore 1 MB document limit** — Forces the extraction of file binary content to GCS, adding a storage indirection layer.
- **Loss of raw term vectors** — Vertex AI Search does not expose TF/DF/TF-IDF natively like Solr; term statistics must be computed and stored at indexing time in the application layer.
- **Learning curve** — Team must gain proficiency with Firestore, Pub/Sub, and Vertex AI Search APIs.
- **Vendor lock-in** — Mitigated by the interface/Bridge pattern, but GCP-specific annotations and client libraries will exist in the GCP implementation modules.

---

## Alternatives Considered

| Alternative | Reason for Rejection |
|-------------|---------------------|
| **MongoDB Atlas on GCP** (instead of Firestore) | Viable but not a native GCP service; does not benefit from unified GCP IAM/billing integration as deeply. |
| **Elastic Cloud on GCP** (instead of Vertex AI Search) | Easier Solr-to-Elasticsearch migration (concepts map closely), but still requires cluster management. Chosen path favours fully serverless. |
| **Cloud Tasks** (instead of Pub/Sub) | Cloud Tasks is designed for task dispatch, not pub/sub messaging. Pub/Sub better matches the existing event-driven, fan-out architecture. |
| **Keep current stack, containerised on GKE** | Reduces cloud benefit; still requires self-managing stateful services inside Kubernetes. |

---

## References

- Architecture: [`specs/architecture/system-design.md`](./system-design.md)
- Indexing Workflow: [`specs/features/indexing-workflow.md`](../features/indexing-workflow.md)
- Document Analysis: [`specs/features/document-analysis.md`](../features/document-analysis.md)
- REST API Endpoints: [`specs/features/rest-api-endpoints.md`](../features/rest-api-endpoints.md)
- OpenAPI Specification: [`specs/api/openapi/dsh-rest-api.yaml`](../api/openapi/dsh-rest-api.yaml)
- `DocumentDao` interface: `dsh-data/.../dao/DocumentDao.java`
- `MongoDocumentDao`: `dsh-data/.../dao/mongo/MongoDocumentDao.java`
- `DocumentQueueService` interface: `dsh-rest-api/.../service/DocumentQueueService.java`
- `DocumentQueueServiceImpl` (RabbitMQ): `dsh-rest-api/.../service/DocumentQueueServiceImpl.java`
- `OrderedTermVectorComponent`: `dsh-solr/solr-terms-vector-order/.../OrderedTermVectorComponent.java`
- `AdvancedNumberFilter`: `dsh-solr/solr-advanced-numbers-filter/.../AdvancedNumberFilter.java`
- Roles: [`/.github/roles.md`](../../.github/roles.md)


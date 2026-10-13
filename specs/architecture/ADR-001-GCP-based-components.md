# ADR-001: Adopt Google Cloud Platform Components for DSH Infrastructure

| Field       | Value                        |
|-------------|------------------------------|
| **Date**    | 2026-04-12                   |
| **Status**  | Proposed                     |
| **Authors** | Backend Development Team     |

---

## Revision note

Revised on 2026-10-13 under `#160`, by [ADR-003](./ADR-003-document-pipeline.md), which
decides the document pipeline. This ADR keeps its status, `Proposed`. It stays the record of
why DSH moves to GCP and onto which services. What changed:

- **Context table.** It says what the code does today: `dsh-rest-api` enqueues and nothing
  dequeues, and Solr is called by no code. It used to say that the workers "dequeue and process".
- **Decision 1, "Deprecate Current Implementations — Do Not Remove".** Reversed. The legacy code
  is removed, because it has no consumers and no data. Nothing is annotated with `@Deprecated`.
- **Decision 2, interface first.** Kept, and made a rule the build checks. Selection is by a
  property per service, as ADR-003 decides, and not by `@Profile("gcp")` against
  `@Profile("rabbitmq")` or `@Profile("legacy")`.
- **Current interface inventory.** `DocumentDao` stays. `DocumentQueueService` goes with the
  RabbitMQ path. The `DocumentRepository` row is removed, and so is the refactoring table below
  it, which planned a `DocumentPersistenceRepository` layer, the conversion of
  `dequeue-docId-context.xml`, and a `TermVectorOrderingService`.
- **§3.1, Firestore and Cloud Storage.** Kept. The repository class named there is replaced by a
  `DocumentDao` implementation. Cloud Storage also holds the indexing stage's output, and
  Firestore also holds the corpus term statistics.
- **§3.2, Pub/Sub.** Rewritten for push to the same service, two topics, and no worker.
- **§3.3, Vertex AI Search.** Removed, with the `google-cloud-discoveryengine` dependency. No
  search engine has a role.
- **Phase 1.** Only the file storage task survives, without "deprecated". Tasks 1.3 to 1.6 are
  removed. Tasks 1.1 and 1.2 are replaced by the in-memory `DocumentDao` and the removal of
  MongoDB.
- **Phase 2.** Task 2.5, the migration utility, is removed. `@Profile("gcp")` becomes selection by
  property. Term statistics on Firestore are added. The implementations go into the new `dsh-gcp`
  module.
- **Phase 3.** Rewritten to match Wave 6.
- **Phase 4.** Removed. Phase 5 keeps its number, so that an older document that cites "Phase 5"
  still points at the right one.
- **Phase 5.** Rewritten to match Wave 9: the parity comparison and the `legacy` profile are gone.
- **Consequences, Alternatives.** Brought in line: no dual implementations, no backward
  compatibility through deprecated code, and no loss of term vectors to a search engine.
- **References.** The entries for Solr classes are removed. Entries for code that still exists
  stay.

Each phase now names the PRD wave that delivers it. The wave numbers are those of the
re-plan of 2026-10-10, and a later re-plan will move them. Until the PRD has been re-planned
from ADR-003's consequences, it differs from this ADR in places. Where they differ, this ADR
follows ADR-003.

---

## Context

Document Smart Highlights (DSH) currently relies on self-managed, on-premise infrastructure components:

| Component   | Current Role in DSH |
|-------------|---------------------|
| **MongoDB** | Primary document store — persists `Document` entities (metadata, keywords, sentences, original file binary contents, processing status) via Spring Data `MongoRepository` through the `dsh-data` module. |
| **RabbitMQ** | Asynchronous message queue — `dsh-rest-api` enqueues document IDs via Spring Integration AMQP (`enqueue-docId-context.xml`). **Nothing dequeues them.** The worker modules (`dsh-doc-indexer-worker`, `dsh-doc-processor-worker` and the two extractors) each hold one `Application` class and no consumer. |
| **Apache Solr** | **Called by no code.** `dsh-solr` holds two plugins, `OrderedTermVectorComponent` and `AdvancedNumberFilter`, written for sorted term vectors and token filtering. No module indexes a document in Solr or queries it. |

These components require manual provisioning, patching, scaling, and monitoring. As DSH moves to **Google Cloud Platform (GCP)**, running on **Cloud Run**, the infrastructure must be modernised to cloud-native, fully managed equivalents that align with a serverless deployment model.

DSH does not process documents yet. How the pipeline that will process them is built, with and without GCP, is decided in [ADR-003](./ADR-003-document-pipeline.md). This ADR records why DSH moves to GCP and onto which services.

### Motivation

1. **Operational overhead** — Self-managed MongoDB, RabbitMQ, and Solr clusters require significant operational investment.
2. **Scalability** — Cloud Run auto-scales the application tier, but the backing services remain fixed-capacity bottlenecks.
3. **Cloud-native alignment** — Using GCP-managed services alongside Cloud Run provides unified IAM, monitoring, and networking.
4. **Cost efficiency** — Serverless/managed services follow a pay-per-use model that better matches variable document processing workloads.

---

## Decision

### 1. Remove the Current Implementations

The existing MongoDB, RabbitMQ and Solr integration code is **removed**, not deprecated. Nothing consumes the queue, nothing calls Solr, and no DSH installation holds MongoDB data to keep, so there is nothing to stay compatible with and nothing to migrate.

| What | Removed in |
| ------ | ------------ |
| MongoDB: `MongoDocumentDao`, `DocumentRepository`, `dshApplicationContext.xml`, `mongo.properties` | Wave 1 task 9, once the in-memory `DocumentDao` is the default |
| RabbitMQ: `enqueue-docId-context.xml`, `DocumentQueueServiceImpl` and its handlers | Wave 2 task 1, with the flow that replaces the enqueue path |
| Solr: the `dsh-solr` module, with its two plugins | Wave 2 task 3, with the worker modules |

See ADR-003, sections 2, 7 and 8.

### 2. Interface-First / Bridge Design Pattern

Every service outside the JVM is reached through an **interface that names no technology**. A consumer names the interface and never an implementation. ADR-003, section 3, states the rules; in short:

- **Selection is by a property per service**: `dsh.persistence`, `dsh.transport` and `dsh.file-store`. The `gcp` profile sets the three together, and each can still be set alone.
- **With no profile, nothing on GCP is reached.** Documents and term statistics are kept in memory, and uploads go to a local directory.
- **The build checks the rule.** Every class that names a GCP type lives in a module of its own, `dsh-gcp`, which only `dsh-rest-api` depends on, at `runtime` scope. A class anywhere else that names a provider's type does not compile.

#### Current Interface Inventory

The codebase already follows an interface-based approach in several areas:

| Interface | Current Implementation | Module | Status |
| ----------- | ---------------------- | -------- | -------- |
| `DocumentDao` | `MongoDocumentDao` | `dsh-data` | ✅ Stays. It names no technology. It gains a conditional status advance and an in-memory implementation (Phase 1), and a Firestore implementation (Phase 2) |
| `DocumentQueueService` | `DocumentQueueServiceImpl` (RabbitMQ) | `dsh-rest-api` | ❌ Goes with the RabbitMQ path, in Wave 2 task 1. A hop of the pipeline is a Spring Integration `MessageChannel`, and no DSH interface stands in front of it |
| `DocumentHandlingService` | `DocumentHandlingServiceImpl` | `dsh-rest-api` | ✅ Interface exists — no direct infrastructure coupling (delegates to `DocumentDao`) |
| `DocumentSubmissionService` | `DocumentSubmissionServiceImpl` | `dsh-rest-api` | ✅ Interface exists. It orchestrates `DocumentHandlingService` and `DocumentQueueService` today; Wave 2 task 1 moves submission to the flow's gateway |

ADR-003 adds two interfaces to `dsh-data`: `FileStorageService` (Wave 1 task 10), and one for the corpus term statistics (Wave 3 task 2).

### 3. GCP Component Mapping

#### 3.1 MongoDB → Firestore (Native Mode) + Google Cloud Storage

| Aspect | Detail |
| -------- | -------- |
| **Document metadata & highlights** | Stored in **Firestore**. The mapping lives in the Firestore implementation, in `dsh-gcp`. The `Document` model in `dsh-data` carries no provider's annotation. |
| **Original file binary contents** | Stored in **Google Cloud Storage (GCS)**. The `Document` entity's `byte[] originalFileContents` field (up to 50 MB per FR001 in `document-analysis.md`) will be replaced with a `String fileStorageUri` pointing to the GCS object. This is required because Firestore limits one document to **1 MiB** (ADR-003, F7). |
| **The indexing stage's output** | Also stored in **Cloud Storage**, beside the original file: the extracted text and the document's own term frequencies. The record in Firestore holds only what does not grow with the document (ADR-003, section 1). |
| **Corpus term statistics** | Stored in **Firestore**, behind an interface of their own, with a bound on the writes per document (ADR-003, section 5). |
| **Spring dependency** | `spring-cloud-gcp-starter-data-firestore`, `spring-cloud-gcp-starter-storage`, in `dsh-gcp` only |
| **Implementation** | A `DocumentDao` implementation on Firestore, in `dsh-gcp`, selected by `dsh.persistence=firestore`. The file store's is selected by `dsh.file-store=gcs`. |

#### 3.2 RabbitMQ → Google Cloud Pub/Sub

| Aspect | Detail |
| -------- | -------- |
| **Messaging** | Each of the pipeline's two hops is a **Pub/Sub** topic with a **push** subscription to the same Cloud Run service. The service publishes the document's id, and Pub/Sub posts it back to an HTTP endpoint of the service. There is no worker and no subscriber that pulls. |
| **Delivery semantics** | Pub/Sub delivers at least once (ADR-003, F3), which FR001 in `indexing-workflow.md` asks for, so every stage is idempotent. FR001's "retried up to 3 times" is overridden by ADR-003 (section 1, "Failures"): a stage's own failure is recorded at once, and only a store's failure is delivered again. How many times is a setting of the subscription that Wave 6 chooses, and a dead-letter topic cannot set it below 5 (ADR-003, F4). |
| **Spring dependency** | `spring-cloud-gcp-starter-pubsub`, `spring-integration-gcp`, in `dsh-gcp` only |
| **Cloud Run integration** | A stage is started by the push request, so no instance waits on a queue and minimum instances stays 0, as the cost rule in `CLAUDE.md` requires (ADR-003, section 4). |
| **Configuration** | Java configuration in `dsh-gcp`, selected by `dsh.transport=pubsub`. With no profile a hop is a channel in the JVM. `enqueue-docId-context.xml` is deleted, not converted. |

Apache Solr has no replacement on GCP. What it was to supply, the terms of a document and their statistics, is computed inside the JVM and stored in Firestore (ADR-003, sections 1, 5 and 6).

### 4. Migration Plan

Each phase names the PRD wave that delivers it. Task numbers that this revision removed are not reused.

#### Phase 1 — Interfaces and the Default Implementations (Wave 1, tasks 8 to 10)

**Goal**: Put every service outside the JVM behind a technology-neutral interface, each with a default implementation that needs nothing installed.

Tasks 1.1 and 1.2 are delivered together, or in an order that writes no MongoDB code: after 1.1 alone, `MongoDocumentDao` would have to implement the new advance only to be deleted by 1.2. The re-plan of the waves settles which (ADR-003, "Consequences for the waves").

| Task | Module | Detail |
| ------ | -------- | -------- |
| 1.1 | `dsh-data` | Add the conditional status advance to `DocumentDao`, and an in-memory implementation that is the default. The contract is written on the interface: the atomic advance, writes by field, copies. One contract test, which every implementation passes (ADR-003, sections 1 and 3). Wave 1 task 8. |
| 1.2 | `dsh-data` | Remove MongoDB: `MongoDocumentDao`, `DocumentRepository`, `dshApplicationContext.xml`, `mongo.properties`, the MongoDB dependencies, and the MongoDB container that `-DintegrationTests` starts. Wave 1 task 9. |
| 1.7 | `dsh-data` | Refactor `Document` entity: extract `byte[] originalFileContents` into a `FileStorageService` interface that stores and retrieves an object, and lets a caller store one so that a repeat replaces it. Create `LocalFileStorageService`, the default, and later the Cloud Storage implementation. Wave 1 task 10. |

#### Phase 2 — GCP Implementation: Firestore + GCS (Wave 5)

| Task | Module | Detail |
| ------ | -------- | -------- |
| 2.1 | `dsh-gcp` | Create the module. Add `spring-cloud-gcp-starter-data-firestore` and `spring-cloud-gcp-starter-storage` dependencies. `dsh-rest-api` depends on it at `runtime` scope, and no other module does. |
| 2.2 | `dsh-gcp` | Map `Document` fields to Firestore, inside the module. |
| 2.3 | `dsh-gcp` | Create `FirestoreDocumentDao implements DocumentDao`, selected by `dsh.persistence=firestore`. The status advance is a transaction that reads the document and writes the status only if it matches. It passes the contract test of task 1.1 against the emulator, and the case of two callers against Firestore itself. |
| 2.4 | `dsh-gcp` | Create `GcsFileStorageService implements FileStorageService`, selected by `dsh.file-store=gcs`. Upload/download file bytes to a GCS bucket. The `Document` entity stores a `fileStorageUri` instead of raw bytes. |
| 2.6 | `dsh-gcp` | Corpus term statistics on Firestore, behind the interface Wave 3 task 2 adds to `dsh-data`, with a bound on the writes per document (ADR-003, section 5). |

#### Phase 3 — GCP Implementation: Cloud Pub/Sub (Wave 6)

| Task | Module | Detail |
| ------ | -------- | -------- |
| 3.1 | `dsh-gcp` | Add `spring-cloud-gcp-starter-pubsub` and `spring-integration-gcp` dependencies. |
| 3.2 | `dsh-gcp` | The `pubsub` transport, selected by `dsh.transport=pubsub`. For each of the two hops: a publish to the hop's topic whose outcome is awaited, and a push endpoint that hands the document's id to the flow. The endpoints accept requests from Pub/Sub only. |
| 3.3 | build | The Pub/Sub emulator, for tests over the wire. |
| 3.4 | deploy | Two topics and two push subscriptions for each tier, with their redelivery settings. |

There is no Phase 4.

#### Phase 5 — Validation & Cutover (Wave 9)

| Task | Detail |
| ------ | -------- |
| 5.2 | Execute the integration tests against the deployed application under the `gcp` profile. They run against `dsh-dev` from Wave 5 and against `dsh-staging` from Wave 7, so nothing of this task is left for Wave 9. |
| 5.3 | Performance-test against benchmarks in `specs/testing/performance-benchmarks/` (FR004: 95% of docs < 10 MB within 30 s). Wave 9 task 1. |
| 5.4 | The jar sets no default profile. The deployment sets `gcp`, and has since Wave 4. A run with no profile keeps working, in memory, and is not a legacy mode. |

---

## Consequences

### Positive

- **Modernisation** — DSH moves to fully managed, cloud-native services that auto-scale with Cloud Run.
- **Reduced operational overhead** — No more self-managed MongoDB clusters, RabbitMQ brokers, or Solr nodes.
- **Better coupling to cloud-native components** — Unified GCP IAM, Cloud Logging, Cloud Monitoring, and VPC networking across all services.
- **Interface-driven architecture** — The Bridge/interface pattern improves testability and allows swapping implementations without changing business logic. The build checks it, through the `dsh-gcp` module boundary.
- **Nothing to install for a trial** — A run with no profile keeps everything in memory and on local disk, and reaches nothing on GCP.
- **Pay-per-use cost model** — Firestore, Pub/Sub and Cloud Storage charge based on usage, aligning cost with actual document processing volume.
- **One implementation of each service per mode** — No dual implementations, no migration of data, and no deprecated code to carry.

### Negative

- **No fallback to the old stack** — The MongoDB, RabbitMQ and Solr code is removed, so the only modes are in memory and GCP.
- **Firestore 1 MiB document limit** — Forces the file's binary content and the indexing stage's output into Cloud Storage, adding a storage indirection layer.
- **Term statistics are DSH's own to compute and store** — No search engine supplies them. They are computed inside the JVM and kept in Firestore, where concurrent documents contend for the same records.
- **No search across documents** — It needs a new decision (ADR-003, section 6).
- **Learning curve** — Team must gain proficiency with the Firestore, Pub/Sub and Cloud Storage APIs.
- **Vendor lock-in** — Mitigated by the interface/Bridge pattern. GCP-specific annotations and client libraries exist in `dsh-gcp` and nowhere else.

---

## Alternatives Considered

| Alternative | Reason for Rejection |
| ------------- | --------------------- |
| **MongoDB Atlas on GCP** (instead of Firestore) | Viable but not a native GCP service; does not benefit from unified GCP IAM/billing integration as deeply. |
| **Elastic Cloud on GCP** (in place of Solr) | Still requires cluster management, and no search engine has a role in DSH (ADR-003, section 6). |
| **Cloud Tasks** (instead of Pub/Sub) | Cloud Tasks is designed for task dispatch, not pub/sub messaging. Pub/Sub better matches the existing event-driven, fan-out architecture. |
| **Keep current stack, containerised on GKE** | Reduces cloud benefit; still requires self-managing stateful services inside Kubernetes. |

The alternatives weighed for the pipeline itself are in ADR-003, "Alternatives considered".

---

## References

- The document pipeline: [`specs/architecture/ADR-003-document-pipeline.md`](./ADR-003-document-pipeline.md)
- Profiles and configuration, the findings: [`specs/architecture/profiles-and-configuration.md`](./profiles-and-configuration.md)
- Architecture: [`specs/architecture/system-design.md`](./system-design.md)
- Indexing Workflow: [`specs/features/indexing-workflow.md`](../features/indexing-workflow.md)
- Document Analysis: [`specs/features/document-analysis.md`](../features/document-analysis.md)
- REST API Endpoints: [`specs/features/rest-api-endpoints.md`](../features/rest-api-endpoints.md)
- OpenAPI Specification: [`specs/api/openapi/dsh-rest-api.yaml`](../api/openapi/dsh-rest-api.yaml)
- `DocumentDao` interface: `dsh-data/.../dao/DocumentDao.java`
- `MongoDocumentDao`: `dsh-data/.../dao/mongo/MongoDocumentDao.java`
- `DocumentQueueService` interface: `dsh-rest-api/.../service/DocumentQueueService.java`
- `DocumentQueueServiceImpl` (RabbitMQ): `dsh-rest-api/.../service/DocumentQueueServiceImpl.java`
- Roles: [`/.github/roles.md`](../../.github/roles.md)

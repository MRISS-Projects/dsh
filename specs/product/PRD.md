# DSH Product Requirements Document (PRD)

## 1. Purpose

This PRD turns the proposed GCP migration in
[`specs/architecture/ADR-001-GCP-based-components.md`](../architecture/ADR-001-GCP-based-components.md)
into schedulable waves of work, and triages the existing GitHub issue backlog into those waves.
It is the single place that says what DSH builds next and in what order.

The PRD is step 1's output in the six-step process described in
[`docs/process/ai-driven-development.md`](../../docs/process/ai-driven-development.md): the
`dsh-plan-wave` skill updates it after a brainstorming pass, `dsh-new-story` reads a task from it
to draft a GitHub issue, and the wave-to-milestone mapping in §3 tells `dsh-new-story` which
milestone to set on that issue.

ADR-001 itself is **Status: Proposed**. No migration code exists yet — the codebase has no GCP
dependencies and no code marked `@Deprecated`. Waves 1-5 below describe the work ADR-001 proposes,
not work already in progress.

## 2. How to read a wave

- Waves are **ordered**. A task belongs in the earliest wave whose dependencies it satisfies.
- Each task listed under a wave is meant to become one INVEST story via `dsh-new-story` (step 2 of
  the process) — independent, negotiable, valuable, estimable, small, and testable. A task too
  large for one task branch gets split into more than one story at that point, not written as a
  single oversized story.
- An issue number next to a task (e.g. `#48`) means an existing GitHub issue was triaged into that
  wave and should be referenced from the resulting story. A task with no issue number is new work
  identified while writing this PRD, and does not yet have an issue.
- Waves 1-5 mirror the five migration phases in ADR-001 §4 one-for-one. Their task lists are drawn
  directly from those phase tables — no tasks were invented here that ADR-001 does not already
  enumerate.

## 3. Wave-to-milestone mapping

| Wave | Milestone |
|---|---|
| 0 | `0.3.0-SNAPSHOT` |
| 1 | `0.4.0-SNAPSHOT` |
| 2 | `0.4.0-SNAPSHOT` |
| 3 | `0.4.0-SNAPSHOT` |
| 4 | `1.0.0-SNAPSHOT` |
| 5 | `1.0.0-SNAPSHOT` |
| 6 | `1.0.0-SNAPSHOT` |

These three milestones already exist in GitHub. `dsh-new-story` sets `--milestone` on
`gh issue create` from this table, keyed by the wave the task came from.

## 4. The waves

### Wave 0 — Engineering foundation

Milestone: `0.3.0-SNAPSHOT`. Housekeeping and build-health work that has no dependency on the GCP
migration, plus gaps found while writing this PRD.

**Triaged issues:**

- `#85` — Update documentation: replace Maven 3.3.9 with 3.9.9 and standardise Java version to 17
- `#86` — Pin Maven 3.9.9 in all GitHub Actions workflows that invoke Maven
- `#87` — Update Maven pinned version from 3.9.9 to 3.9.16 in documentation and GitHub Actions
- `#43` — Configure surefire, jacoco and other useful reports for the maven generated docs
- `#46` — Implement integration tests using embedded tomcat server
- `#70` — Project link not working at maven generated site
- `#90` — index.html missing from published site on gh-pages (root + all submodules)

**New tasks identified while writing this PRD (no issue yet):**

1. **Remove `install-parent-pom.sh` and the root `parent-pom.xml`.** They install
   `com.mriss:mriss-parent:1.2.4`, an artifact no module in this repo inherits from — the real
   parent is `com.mriss.mriss-parent:products` (see root `pom.xml`). They survive only because the
   Travis-era `build-ci*.sh` scripts still reference them. Removing all of it together is one
   story.
2. ~~Widen the markdown lint glob to cover `.claude/**`.~~ **Done** in this fix wave:
   `.github/workflows/spec-validation.yml`'s `validate-markdown` job and its trigger `paths:` now
   both cover `CLAUDE.md` and `.claude/**`, so the five project skills under `.claude/skills/` are
   inside the enforcing gate.
3. **The JaCoCo aggregate's scope is already complete — no widening needed.** Verified directly:
   `dsh-coverage-report/pom.xml` depends on all 8 code-bearing modules (`dsh-data`, `dsh-rest-api`,
   `dsh-doc-indexer-worker`, `dsh-doc-processor-worker`, `dsh-keyword-extractor`,
   `dsh-top-sentences-extractor`, `solr-advanced-numbers-filter`, `solr-terms-vector-order`) plus
   `dsh-test-dataset` (no main sources), and the aggregate's CSV contains 15 packages spanning all
   8 of them. The rest of the 13 modules are aggregator POMs (root, `dsh-doc-analyser`, `dsh-solr`,
   `dsh-coverage-report`) with no production code of their own. The whole repository has 38 main
   `.java` files, so 2,028 instructions **is** the entire codebase, not a slice of it. The
   codebase is simply small — which is exactly what makes the coverage floor sensitive to a single
   new class.
4. **Revisit the coverage baseline.** `.github/coverage-baseline.txt` is `98.13`. On a
   2,028-instruction denominator that floor is brittle — one new untested ~100-instruction class
   drops the aggregate to roughly 93.5% and turns the build red. The ADR-001 migration (waves 1-5)
   adds substantial new code and will fight this gate as written.
5. **Make `check-spec-references` enforcing, or remove it.** The `check-spec-references` job in
   `.github/workflows/spec-validation.yml` initializes `missing=0`, never increments it, prints a
   `WARNING` for each unresolved reference, and always exits 0 — it cannot fail a run. It reads as
   an enforcing gate but is advisory only. This mirrors the `|| true` problem already fixed for
   markdownlint: either make it actually fail the job on a missing reference, or remove it and stop
   describing it as a gate.

The root `pom.xml`'s SNAPSHOT parent pin is a related, but deliberately *not* actionable, item —
see §6 for why it belongs in accepted risks rather than the backlog.

### Wave 1 — ADR-001 Phase 1: interface extraction and deprecation

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 1 — Interface Extraction &
Deprecation". This wave's theme legitimately involves deprecation as the work to be performed —
marking current implementations `@Deprecated` as new interfaces are extracted — not a claim that
anything is deprecated today.

**Triaged issue:** `#48` — Investigate how to use profiles (dev, staging, production) with Spring
and Maven. This underpins the `@Profile`-based selection ADR-001 Phase 1 calls for throughout.

**Tasks (ADR-001 §4 Phase 1):**

1. `dsh-data` — Create `DocumentPersistenceRepository` interface. Wrap the existing
   `DocumentRepository` (Mongo) as `MongoDocumentPersistenceRepository implements
   DocumentPersistenceRepository`, marked `@Deprecated`.
2. `dsh-data` — Refactor `MongoDocumentDao` to depend on `DocumentPersistenceRepository` instead of
   `DocumentRepository` directly. Mark `MongoDocumentDao` `@Deprecated`.
3. `dsh-rest-api` — Convert `enqueue-docId-context.xml` / `dequeue-docId-context.xml` to Java
   `@Configuration` classes. Create `RabbitMqIntegrationConfig` (`@Profile("rabbitmq")`), marked
   `@Deprecated`.
4. `dsh-rest-api` — Mark `DocumentQueueServiceImpl` (RabbitMQ) `@Deprecated`, assign
   `@Profile("rabbitmq")`.
5. `dsh-solr` — Extract a `TermVectorOrderingService` interface from
   `OrderedTermVectorComponent`'s sorting logic (accepts term-vector entries and order options,
   returns the sorted list). Mark the Solr adapter `@Deprecated`.
6. `dsh-solr` — Evaluate `AdvancedNumberFilter`. If expressible as a pre-indexing step, extract a
   `NumberFilterService` interface. Mark the Solr implementation `@Deprecated`.
7. `dsh-data` — Refactor the `Document` entity: extract `byte[] originalFileContents` into a
   `FileStorageService` interface with `store(byte[]) → URI` and `retrieve(URI) → byte[]`. Create
   `LocalFileStorageService` (deprecated, for backward compatibility); `GcsFileStorageService`
   follows in Wave 2.

### Wave 2 — ADR-001 Phase 2: Firestore + GCS

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 2 — GCP Implementation:
Firestore + GCS (MongoDB Replacement)". No existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 2):**

1. `dsh-data` — Add `spring-cloud-gcp-starter-data-firestore` and
   `spring-cloud-gcp-starter-storage` dependencies.
2. `dsh-data` — Create `FirestoreDocumentPersistenceRepository implements
   DocumentPersistenceRepository`. Map `Document` fields to Firestore collections.
3. `dsh-data` — Create `FirestoreDocumentDao implements DocumentDao` (`@Profile("gcp")`). Wire to
   `FirestoreDocumentPersistenceRepository`.
4. `dsh-data` — Create `GcsFileStorageService implements FileStorageService` (`@Profile("gcp")`).
   Upload/download file bytes to a GCS bucket; the `Document` entity stores a `fileStorageUri`
   instead of raw bytes.
5. `dsh-data` — Write a migration utility to bulk-copy existing MongoDB documents to Firestore and
   upload `originalFileContents` blobs to GCS, replacing the field with the resulting URI.

### Wave 3 — ADR-001 Phase 3: Cloud Pub/Sub

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 3 — GCP Implementation: Cloud
Pub/Sub (RabbitMQ Replacement)".

**Triaged issue:** `#49` — re-scoped. Originally titled "Create docker structure based on docker
files to have all servers configured as docker containers to run tests and/or the application" —
i.e., Docker containers for MongoDB/RabbitMQ/Solr. It is re-scoped to **Firestore and Pub/Sub
emulators** for local and CI testing, which is why it sits in Wave 3 (alongside the Pub/Sub work
it will exercise) rather than Wave 0.

**Tasks (ADR-001 §4 Phase 3):**

1. `dsh-rest-api` — Add `spring-cloud-gcp-starter-pubsub` and `spring-integration-gcp`
   dependencies.
2. `dsh-rest-api` — Create `PubSubIntegrationConfig` (`@Profile("gcp")`) — defines a
   `PubSubTemplate` and outbound channel adapter to a `document-tasks` topic.
3. `dsh-rest-api` — Create `PubSubDocumentQueueServiceImpl implements DocumentQueueService`
   (`@Profile("gcp")`). Publishes document IDs to the Pub/Sub topic.
4. `dsh-doc-indexer-worker` — Replace `spring-boot-starter-amqp` with
   `spring-cloud-gcp-starter-pubsub`. Create a Pub/Sub subscriber (pull or push via a Cloud Run
   HTTP endpoint) that invokes the existing processing pipeline.
5. `dsh-doc-analyser` / `dsh-doc-processor-worker` — Replace the `spring-boot-starter-amqp`
   dependency. Wire a Pub/Sub subscriber analogously to task 4.

### Wave 4 — ADR-001 Phase 4: Vertex AI Search

Milestone: `1.0.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 4 — GCP Implementation: Vertex AI
Search (Solr Replacement)". No existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 4):**

1. `dsh-solr` (or a new `dsh-search` module) — Add `google-cloud-discoveryengine` dependency.
2. New module / `dsh-solr` — Create `VertexAiSearchIndexingService`, indexing document content in
   a Vertex AI Search data store via the Document AI / ingestion APIs.
3. New module / `dsh-solr` — Create `VertexAiSearchQueryService`, executing full-text search
   queries via the Vertex AI Search serving API.
4. **`OrderedTermVectorComponent` migration.** Vertex AI Search does not expose raw term vectors
   (TF, DF, TF-IDF) the way Solr's `TermVectorComponent` does:
   - At indexing time, compute TF/DF/TF-IDF statistics in the application layer (Java) from the
     extracted document text, before sending it to Vertex AI Search. Store these statistics as
     structured metadata on the Vertex AI Search document or in Firestore.
   - Extract the sorting logic currently in `SortedNamedList` / `TermsVectorComparator` / `Order`
     into the new `TermVectorOrderingService` implementation (`VertexAiTermVectorOrderingService`).
     It retrieves the pre-computed term statistics from Firestore or Vertex AI Search metadata and
     applies the same ascending/descending sort by the requested field.
   - The `order` query parameter format (`order=tv.tf;desc`) is preserved in the REST API;
     `dsh-rest-api` delegates to `TermVectorOrderingService`, now infrastructure-agnostic.
   - `Order` (enum), `OrderOptions` (POJO), and `TermsVectorComparator` (a generic
     `Comparator<Object>` depending only on `OrderOptions`) are reusable as-is. Only
     `OrderedTermVectorComponent` and `SortedNamedList` are Solr-coupled and need replacement.
5. **`AdvancedNumberFilter` migration.** Evaluate Vertex AI Search's built-in
   tokenisation/filtering. If number filtering is not natively supported, implement a
   `NumberFilterPreProcessor` in the indexing pipeline that strips or normalises numeric tokens
   before content is sent to Vertex AI Search.

### Wave 5 — ADR-001 Phase 5: validation and cutover

Milestone: `1.0.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 5 — Validation & Cutover". No
existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 5):**

1. Run both profiles (`rabbitmq`/`mongodb` and `gcp`) in parallel in a staging environment.
   Compare results for functional parity.
2. Execute integration tests from `specs/testing/test-plans/` against the `gcp` profile.
3. Performance-test against benchmarks in `specs/testing/performance-benchmarks/` (FR004: 95% of
   documents under 10 MB processed within 30 s).
4. Switch the default Spring profile to `gcp`. Deprecated implementations remain available via
   `@Profile("legacy")`.

### Wave 6 — Product backlog

Milestone: `1.0.0-SNAPSHOT`. Existing backlog issues with no dependency on the GCP migration,
scheduled after it so the migration lands first.

**Triaged issues:**

- `#44` — Add architecture and components diagrams and description following the three
  architecture patterns at content.pivotal.io/blog/agile-architecture
- `#45` — Document, configure and test Spring Boot actuators for the REST API module
- `#50` — Add extra Swagger documentation using annotations
- `#51` — Investigate and add support for `spring-boot-starter-hateoas`
- `#52` — Improvement for performance: file hash generation could be another service. Reviewed and
  **kept**: the issue mentions Mongo only as one option for where to generate the hash, and the
  file-hash-as-a-service idea itself is independent of the persistence backend, so it survives the
  migration.
- `#53` — Make the `DocumentStatus` enumeration dynamic by reading descriptions and messages from
  properties files, per locale

## 5. Won't-fix

Two open issues are superseded by the ADR-001 migration itself and will not be built as written.
Closure is recorded here for visibility; the issues are not closed by this PRD — see
`scripts/close-wontfix-issues.sh`, which is for manual review, not automatic execution.

| Issue | Reason | Superseded by |
|---|---|---|
| `#65` — Implement indexer-worker daemon | Its body specifies enqueuing via RabbitMQ and storing results in Solr — both surfaces this migration replaces. | Wave 3 (Cloud Pub/Sub) and Wave 4 (Vertex AI Search) |
| `#47` — Mongo DAO ordering by timestamp | Targets `MongoDocumentDao`, which ADR-001 Phase 1 wraps and Phase 2 replaces with a Firestore-backed implementation. Ordering behaviour belongs on the new repository, not the one being replaced. | Wave 2 (Firestore + GCS) |

`#52` is explicitly **not** in this table — see Wave 6 above for why it was reviewed and kept.

## 6. Known risks / accepted decisions

- **SNAPSHOT parent pin — accepted deliberately, not an oversight.** The root `pom.xml`
  intentionally tracks `com.mriss.mriss-parent:products:3.8.0-SNAPSHOT`. A SNAPSHOT parent
  re-resolves on Maven's daily snapshot refresh, so the same commit can build differently on
  different days — that is a real reproducibility cost. It is accepted because upcoming work on
  the `MRISS-Projects/parent-poms` project will change this repository's parent, and staying on
  the SNAPSHOT is how those changes reach DSH without a release cycle per iteration.
  `.github/workflows/ci.yml` omits `-U` specifically to limit the drift — it does not force a
  re-resolve on every run, only lets Maven's normal refresh interval apply. **Do not file this as
  a task or assign it a wave/milestone.** Revisit pinning a released version only once the
  parent-poms work has settled; no story and no owner action are needed today.
- **Coverage badge and CI figure disagree.** The committed badge
  `dsh-coverage-report/badges/jacoco.svg` reads 92%, while CI's JaCoCo aggregate currently computes
  98.13% against a 2,028-instruction denominator, which is the whole codebase, not a partial one —
  see Wave 0 tasks 3 and 4. The two either measure different scopes or the badge is stale; they
  should be reconciled directly (e.g. regenerating the badge), rather than trusted as-is in the
  meantime.

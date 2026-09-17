# DSH Product Requirements Document (PRD)

## 1. Purpose

This PRD turns the proposed GCP migration in
[`specs/architecture/ADR-001-GCP-based-components.md`](../architecture/ADR-001-GCP-based-components.md)
into schedulable waves of work, and triages the existing GitHub issue backlog into those waves.
It is the single place that says what DSH builds next and in what order.

The PRD is step 1's output in the eight-step process described in
[`docs/process/ai-driven-development.md`](../../docs/process/ai-driven-development.md): the
`dsh-plan-wave` skill updates it after a brainstorming pass, `dsh-new-story` reads a task from it
to draft a GitHub issue, and the wave-to-milestone mapping in §3 tells `dsh-new-story` which
milestone to set on that issue. Step 8's `dsh-reconcile-prd` closes the loop the other way,
reconciling this document against GitHub once a story has merged and its issue is closed.

ADR-001 itself is **Status: Proposed**. No migration code exists yet — the codebase has no GCP
dependencies and no code marked `@Deprecated`. Waves 1-5 below describe the work ADR-001 proposes,
not work already in progress.

## 2. How to read a wave

- Waves are **ordered**. A task belongs in the earliest wave whose dependencies it satisfies.
- Each task listed under a wave is meant to become one INVEST story via `dsh-new-story` (step 2 of
  the process) — independent, negotiable, valuable, estimable, small, and testable. A task too
  large for one task branch gets split into more than one story at that point, not written as a
  single oversized story.
- An issue number next to a task (e.g. `#48`) means a GitHub issue exists for it and should be
  referenced from the resulting story. Wave 0's items all have issues; waves 1-6 mostly do not yet,
  and their tasks become issues via `dsh-new-story` when they are picked up.
- Where an item has an issue, **the issue is the source of truth** for its rationale and acceptance
  criteria — this document should not restate them, so the two cannot drift apart.
- Where a wave lists its issues in a table, the `Status` column carries only what GitHub says:
  `open`, or `closed` naming the pull request that delivered it. It is maintained by step 8
  (`dsh-reconcile-prd`) after a merge, never hand-edited ahead of one. Titles in that column are
  the GitHub titles, normalised to sentence case with any `[STORY]` prefix dropped.
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

**Triaged issues.** `Status` is reconciled against GitHub in step 8 of the process
(`dsh-reconcile-prd`); the issue itself remains the source of truth for rationale and acceptance
criteria.

| Issue | Status | Title |
|---|---|---|
| `#85` | open | Update documentation: replace Maven 3.3.9 with 3.9.9 and standardise Java version to 17 |
| `#86` | open | Pin Maven 3.9.9 in all GitHub Actions workflows that invoke Maven |
| `#43` | open | Configure surefire, jacoco and other useful reports for the maven generated docs |
| `#46` | open | Implement integration tests using embedded tomcat server |
| `#70` | open | Project link not working at maven generated site |
| `#90` | open | index.html missing from published site on gh-pages (root + all submodules) |
| `#92` | **closed** — PR #96 | Remove the dead Travis build estate |
| `#93` | **closed** — PR #102 | Remove the redundant coverage ratchet — `jacoco:check` at 95% is already inherited |
| `#94` | open | Make `check-spec-references` enforcing, or remove it |
| `#95` | **closed** — PR #98 | Use a read-only token for CI package authentication |
| `#97` | open | Resolve the tooling orphaned by the Travis estate removal |
| `#99` | **closed** — PR #100 | Standardise Maven builds on `-U` while the parent is a SNAPSHOT |
| `#101` | open | Fail CI when the package token cannot authenticate, not just when it is absent |
| `#103` | **closed** — PR #105 | Make PR review rounds repo-aware and authoritative |
| `#104` | open | Regenerate the coverage badge, or stop publishing a stale one |

Issues `#92`, `#93`, `#94` and `#95` were raised from findings made while writing this PRD and
while reviewing the branch that introduced it; each carries its full rationale and acceptance
criteria. The paragraphs that originated them have been removed from this document — the issues are
now the single source of truth for that work.

`#93` was **rewritten** before it was built, and its entry above carries the new title. It was
opened on a false premise — that DSH had no coverage threshold — and ended up asking for the
opposite of what it originally asked: `jacoco:check` at 95% is inherited from parent-poms, per
module and build-failing, so the second gate (`scripts/check-coverage.sh` plus
`.github/coverage-baseline.txt`) was the redundant one. Shipped in PR #102, which removed that gate
and left `jacoco:check` as the only one.

Closing the blind spot the removal opened needed a change in `parent-poms`, not here:
`jacoco:check` skips a module that produced no exec data, so a module with production classes and
no tests would have passed at 0%. That went upstream as two direct commits under `CLAUDE.md`'s
light round trip, both referenced on `#93`. The reason there were two is recorded in `#103` below.

`#99` came later still, spun off from `#95`'s out-of-scope list while that story was being built:
`ci.yml` and `api-testing.yml` disagreed on `-U`, so the two could resolve different parent
SNAPSHOTs from the same commit. It was resolved *towards* `-U` rather than away from it — while the
parent is a `-SNAPSHOT` and this repository is the first consumer of `parent-poms` changes,
tracking the current parent on every run is the intended contract, and omitting `-U` never bought
reproducibility in the first place. Shipped in PR #100.

`#101` was spun off from `#99` during that story's review: `ci.yml`'s credential step checks that
`PACKAGES_READ_TOKEN` is non-empty, never that it authenticates, so a dead token yields either a
green build against a cached parent or — once the cache misses — a failure that reads as a broken
parent rather than a broken credential. It predates `#99` and was left out of it deliberately:
`#99` changed how often the symptom appears, not the presence-only check that hides it, and
folding a second CI behaviour change into a flag-and-prose story would have made both harder to
review.

`#97` was spun off from `#92` when that story shipped: removing the Travis estate deliberately left
three things behind — four uncalled `mvn` wrapper scripts, the `update-readme` Maven profile whose
only caller was the deleted `post-release-script.sh`, and the generated-vs-checked-in status of
`README.md`. `#92`'s spec promised the follow-up rather than widening its own scope.

`#103` was spun off from `#93`'s review round. Copilot raised six findings on PR #102; five were
right, and one — repeated in 3 of its 9 comments — asserted that `-Denforcer.skip=true` bypasses
the new coverage-data guard and "must be verified/fixed in the shared parent". Running it showed
the opposite, and produced the second upstream commit: the guard is unaffected, but DSH's *other*
enforcer execution has no `<skip>` and **is** disabled by that flag, so the caveat was wrong in a
different way than the review claimed. `#103` was opened on the reading that two gaps made that
round expensive, and **the first of the two turned out to be false.** Copilot code review reads
`CLAUDE.md` from the head branch of the pull request, and `CLAUDE.md` at `fb3f0a09` — the exact
commit reviewed — already stated in bold that both coverage gates are inherited from `parent-poms`
and will not be found by grepping this repository, and that `-Denforcer.skip=true` does not reach
the guard. The context was present and did not reach the finding, so the gap is salience and
reviewer effort, not absence; `#102` was reviewed on `Lite`. The second gap stands as written:
nothing said who adjudicates a disputed finding, or that evidence rather than seniority settles it.
`#93` was not widened to cover either: they concern how a review round is set up and arbitrated,
not the coverage gate, and one of the fixes lands in a `.github/skills` file that has nothing to do
with coverage. Shipped in PR #105, which added `.github/skills/code-review/SKILL.md` as the
reviewer-facing seat and wrote the adjudication rule into `dsh-pr-cycle` and step 7 of the process
doc. Like `#93`, it was **rewritten before it was built**, because the premise it was opened on
did not survive checking.

`#104` came out of reconciling this document after `#93` merged. The badge-versus-aggregate
disagreement had sat in §6 as a risk phrased as an open question — "either they measure different
scopes or the badge is stale". Checking it answered the question: the badge reads the same
`INSTRUCTION` counter from the same aggregate CSV as the 98.13% figure, so it is stale, and it is
stale because the `process-badges` profile can only be activated with `-P`, which this estate
deliberately abandoned. A diagnosed defect with a known cause is work, not a risk, so it became an
issue. It was not folded into `#93` because that story removed a coverage *gate* and explicitly
preserved the reporting path the badge belongs to — the two touch the same module and nothing else.

**Two findings from the same review are deliberately *not* issues:**

- **The markdown lint glob already covers `.claude/**`.** `.github/workflows/spec-validation.yml`'s
  `validate-markdown` job and its trigger `paths:` were widened on the branch that created this
  PRD, so the project skills under `.claude/skills/` sit inside the enforcing gate. Done, not
  pending.
- **The JaCoCo aggregate's scope is already complete — there is nothing to widen.** Verified
  directly: `dsh-coverage-report/pom.xml` depends on all 8 code-bearing modules (`dsh-data`,
  `dsh-rest-api`, `dsh-doc-indexer-worker`, `dsh-doc-processor-worker`, `dsh-keyword-extractor`,
  `dsh-top-sentences-extractor`, `solr-advanced-numbers-filter`, `solr-terms-vector-order`) plus
  `dsh-test-dataset` (no main sources), and the aggregate CSV contains 15 packages spanning all 8.
  The remaining modules of the 13 are aggregator POMs (root, `dsh-doc-analyser`, `dsh-solr`,
  `dsh-coverage-report`) with no production code. The repository has 38 main `.java` files, so
  2,028 instructions **is** the entire codebase, not a slice.

  This matters because it reframed `#93`: the coverage floor is sensitive to a single new class
  because the codebase is genuinely small, not because the measurement is partial. An earlier draft
  of this PRD claimed the scope was narrow and asked for it to be widened — that was wrong, and it
  had gated the baseline work behind a task that could never complete. `#93` has since been
  rewritten around the real problem — see the note above the table.

**Wave 0 also has a goal in another repository.** DSH inherits from
`com.mriss.mriss-parent:products`, maintained in `MRISS-Projects/parent-poms`. Wave 0 is not
finished until that repo's open milestones are cleared and released, and DSH is re-pinned:

| parent-poms milestone | Open issues | Outcome |
|---|---|---|
| `3.8.0-SNAPSHOT` | `#57`, `#58`, `#13` | Clear, then release **3.8.0** |
| `3.9.0-SNAPSHOT` | `#59`, `#65`, `#67` | Clear, then release **3.9.0** |

`parent-poms#67` was raised from this work: `maven-failsafe-plugin` is configured there in
`<pluginManagement>` with the right includes, but never activated, so integration tests cannot run
in any inheriting project. It adds a profile keyed on `-DintegrationTests`, so `mvn clean install`
keeps running unit tests only — and the inherited 95% `jacoco:check` keeps measuring unit-test
coverage — while integration tests run on request, in CI. DSH `#46` is blocked on it.

At the end of Wave 0 the root `pom.xml` should inherit from a **released `3.9.0`**, not a SNAPSHOT.
That also retires the accepted risk in §6 — see there for why the SNAPSHOT pin stands until then.
The round trip for making a change in parent-poms and re-pinning here is documented in
`docs/process/ai-driven-development.md`, "Working across the parent-poms boundary".

Note the overlap: parent-poms `#57`/`#58`/`#59` carry the same titles as DSH `#85`/`#86`/`#87`.
Both repositories have workflows that invoke Maven and docs that cite versions, so the work is
genuinely parallel rather than duplicated — each issue is scoped to its own repository and
cross-links its twin. **DSH `#87` no longer sits in this wave** — it moved to Wave 1 on
2026-09-17, for the reason recorded there — so that third pair no longer moves in step: parent-poms
`#59` remains on that repo's `3.9.0-SNAPSHOT` milestone and is still part of the goal below, while
its DSH twin waits for `0.4.0-SNAPSHOT`. Parent-poms `#13` (image links broken in the generated
Maven site) is adjacent to DSH `#70` and `#90`; check whether those are symptoms of it before
fixing them here.

The root `pom.xml`'s SNAPSHOT parent pin is a related, but deliberately *not* actionable, item
**until the above completes** — see §6.

### Wave 1 — ADR-001 Phase 1: interface extraction and deprecation

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 1 — Interface Extraction &
Deprecation". This wave's theme legitimately involves deprecation as the work to be performed —
marking current implementations `@Deprecated` as new interfaces are extracted — not a claim that
anything is deprecated today.

**Triaged issues:**

- `#48` — Investigate how to use profiles (dev, staging, production) with Spring and Maven. This
  underpins the `@Profile`-based selection ADR-001 Phase 1 calls for throughout.
- `#87` — Update Maven pinned version from 3.9.9 to 3.9.16 in documentation and GitHub Actions.
  Not an ADR-001 phase task, and so not in the task list below; it sits here the way `#48` does.
  **Moved out of Wave 0 on 2026-09-17** because `#86` and `#87` contradicted each other inside one
  wave: `#86` pins 3.9.9 in the workflows and `#87` replaces that same pin with 3.9.16, so building
  both in Wave 0 meant writing a version and immediately rewriting it. Deferring `#87` lets `#86`
  ship as written and the bump land once, later.

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

Two issues were superseded by the ADR-001 migration itself and will not be built as written. Both
were **closed as `not planned` on 2026-09-16** by the repo owner, each with a comment naming the
wave that supersedes it. `scripts/close-wontfix-issues.sh` records exactly what was run.

| Issue | Reason | Superseded by |
|---|---|---|
| `#65` (closed) — Implement indexer-worker daemon | Its body specifies enqueuing via RabbitMQ and storing results in Solr — both surfaces this migration replaces. | Wave 3 (Cloud Pub/Sub) and Wave 4 (Vertex AI Search) |
| `#47` (closed) — Mongo DAO ordering by timestamp | Targets `MongoDocumentDao`, which ADR-001 Phase 1 wraps and Phase 2 replaces with a Firestore-backed implementation. Ordering behaviour belongs on the new repository, not the one being replaced. | Wave 2 (Firestore + GCS) |

`#52` is explicitly **not** in this table — see Wave 6 above for why it was reviewed and kept.

## 6. Known risks / accepted decisions

- **SNAPSHOT parent pin — accepted deliberately, not an oversight.** The root `pom.xml`
  intentionally tracks `com.mriss.mriss-parent:products:3.8.0-SNAPSHOT`. A SNAPSHOT parent
  re-resolves as the parent moves, so the same commit can build differently from one run to the
  next — that is a real reproducibility cost. It is accepted because upcoming work on the
  `MRISS-Projects/parent-poms` project will change this repository's parent, and staying on the
  SNAPSHOT is how those changes reach DSH without a release cycle per iteration. Both Maven
  invocations in this repository's workflows pass `-U` (`#99`), which does not add that drift — it
  makes the drift the SNAPSHOT pin already carried consistent and visible, instead of dependent on
  the age of whatever `~/.m2` cache the runner restored. **Do not file this as a separate task.**
  It is not a standing risk with no end date: Wave 0 now carries the parent-poms goal explicitly —
  clear both open milestones there, release `3.8.0` and `3.9.0`, then re-pin this repo's root
  `pom.xml` to the released `3.9.0`. **This risk closes when that completes**, and needs no owner
  action before then.
- **The coverage badge is stale, and nothing regenerates it.** The committed badge
  `dsh-coverage-report/badges/jacoco.svg` reads 92%, while CI's JaCoCo aggregate computes 98.13%
  against a 2,028-instruction denominator, which is the whole codebase, not a partial one — see
  Wave 0's note on aggregate scope.

  Earlier drafts of this risk left open whether the two measure different scopes or the badge had
  simply gone stale. **It is staleness, and that is now settled.** The badge is configured with
  `<metric>instruction</metric>` against `jacoco-aggregate/jacoco.csv` in
  `dsh-coverage-report/pom.xml` — the same counter and the same file the aggregate figure comes
  from, so the two cannot legitimately disagree. The file's only commit is the April 2026
  directory-casing rename, so its content is older than that.

  The cause is not an infrequent pipeline run. The `process-badges` profile is
  `activeByDefault=false` with no property activation, so it is reachable only with
  `-P process-badges` — and no workflow in this repository passes `-P` at all, nor does any
  reusable workflow in `parent-poms`. The only occurrences in the tracked tree are historical
  planning documents. So the profile was orphaned by the estate's deliberate move away from `-P`
  (see `parent-poms`' CLAUDE.md: "never reintroduce `-P`"), not by scheduling. Running
  `staging.yml` more often would not fix it.

  Whoever picks this up should also expect the profile's `maven-scm-plugin` `checkin` execution,
  which commits the badge back to the repository from inside the build — a git write needing
  credentials, and one that interacts with the `[skip ci]` conventions.

  **`#93` closing does not close this.** That story removed the second coverage *gate* and
  deliberately left the `dsh-coverage-report` aggregation, the badge and CI's uploaded artifact
  untouched — the reporting path is exactly what this risk concerns.

  **Now tracked by `#104` in Wave 0**, which carries the full diagnosis and the two side-findings
  that came with it: the badge's `<passing>70</passing>` threshold contradicts the real 95% gate,
  and `maven-scm-plugin` is pinned locally to `1.9.5` against `parent-poms`' managed `2.1.0`. This
  entry should be deleted when `#104` ships — `#104`'s AC005 requires that — since a diagnosed
  defect with an owner is work in a wave, not a standing risk.

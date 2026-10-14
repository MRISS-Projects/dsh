---
issue: 162
slug: openapi-and-feature-specs-describe-the-api
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 162 — Make the OpenAPI document and the feature specs describe the API that exists

## 1. Story

**As a** developer about to change DSH's REST API, starting with Wave 2's results endpoint
**I want** the OpenAPI document and the feature specs to describe the two endpoints the code
serves, as it serves them
**So that** an API change can start in the OpenAPI document from a baseline that is true, and nobody
reading it is sent to routes that return 404

## 2. Context

- Wave: 1, task 11 (`specs/product/PRD.md` §4, Wave 1), DSH milestone `0.4.0-SNAPSHOT`
- Issue: [#162](https://github.com/MRISS-Projects/dsh/issues/162)
- Parent branch: `DEVELOP`, at `65b02a6ad` when this spec was written. The task branch is
  `issue-162-openapi-and-feature-specs-describe-the-api`.
- The decision this story carries is decision 5 of the re-plan of 2026-10-13 (PRD §4): the code
  is the baseline for the public API. ADR-003 (`specs/architecture/ADR-003-document-pipeline.md`)
  names the gap as follow-up candidate 8, and its "Consequences for the waves" table names this
  task for `specs/features/indexing-workflow.md`.
- Latest point: before Wave 2 task 2, which writes the results endpoint's contract in the OpenAPI
  document first, from the baseline this story leaves.

### 2.1 What exists today

**The code.** `DocumentResource` (`dsh-rest-api`) maps two operations under `/v1/dsh/document`.
The application sets no context path and no port, so a local run serves them at
`http://localhost:8080`. Nothing authenticates a request.

| Operation | Input | What it answers |
|---|---|---|
| `POST /v1/dsh/document/submit` | multipart `title` (`@RequestParam`) and `contents` (`MultipartFile`), both required | `200` `TokenDto` `{token, message}`. A blank title, or any exception the method catches, gives `200` with `token` `ERROR` and `message` `Error submitting file: <reason>`. A missing part is rejected by Spring before the method runs, with `400` |
| `GET /v1/dsh/document/status/{token}` | path `token` | `200` `DocumentStatusDto` `{status, message}`. An unknown token gives `200` with `status` `TOKEN_NOT_FOUND` and `message` `No token was found with the contents: <token>` |

`DocumentResourceHttpIT` (`dsh-rest-api`, run under `-DintegrationTests`) already asserts the
valid submission, the blank title, the missing `contents` and the unknown token, so those four
outcomes are pinned by a test.

Three facts are not visible from the controller alone:

- **`status` is `DocumentStatus.getStatusDescription()`.** For 17 of the 18 statuses that string is
  the constant's name. `NOT_INDEXED_ERROR` is declared with `DEQUEUED_FOR_INDEXING`'s description
  and message, which Wave 1 task 12 fixes. `message` is the document's status message.
- **`/submit` answers before the document is stored.** `storeDocumentAndQueueForProcessing()` is
  `@Async`, and `getTokenFromDocument(…, false)` touches no store. So a `/status` call made at once
  can answer `TOKEN_NOT_FOUND`, and a store that cannot be reached does not fail the submission.
  Wave 2 task 1 removes the `@Async` call (ADR-003 section 1, "`/submit`").
- **Every submission gets a new token**, the same file included: `useCache` is `false`. The token is
  a date and a UUID concatenated, and is opaque to a client.

Around the controller:

- **Springfox** generates a Swagger 2 description of every path matching `/v1/dsh.*`, as group
  `dsh-app` (`SwaggerConfig`). Whether `/v2/api-docs?group=dsh-app` answers is unverified: `#144`
  found the UI it feeds broken.
- **Actuator** exposes `health` and `info` (`management.endpoints.web.exposure.include`). They are
  not `DocumentResource`'s, and `#45` owns them.
- **How far a document gets.** `/submit` stores it and sends its id to RabbitMQ.
  `dsh-doc-indexer-worker` holds one class, `DshDocIndexerApplication`, and nothing consumes the
  queue, so a document stops at the enqueue's outcome. Task 1 records which status that is.

**The documents.** `specs/api/openapi/dsh-rest-api.yaml` describes four other operations under
`http://localhost:8080/api/v1` — `POST /documents`, `GET /documents/{documentId}`,
`GET /documents/{documentId}/highlights`, `GET /health` — behind a JWT bearer scheme, with
`AnalysisOptions`, `Highlight`, pagination and an error body none of which exist. The four feature
specs follow it:

| File | What it describes that the code does not have |
|---|---|
| `rest-api-endpoints.md` | The four routes, JWT, `202`, `documentId`, `processing`/`completed`, highlight filters and pagination, `/health`, the error body |
| `document-analysis.md` | PDF, DOC and DOCX up to 50 MB; `AnalysisOptions`; keyword matching, semantic analysis, pattern recognition, named entity recognition; confidence thresholds; three statuses; a worker that analyses, persists highlights and indexes in Solr; a 30-second SLA |
| `indexing-workflow.md` | A worker that extracts, analyses, persists to MongoDB and indexes in Solr; three retries of a failed task; FR003 Solr indexing; an "Analysis timeout" and a "Solr unavailable" row; FR004's SLAs |
| `smart-highlighting.md` | Highlights with types, confidence, positions and metadata; filters; pagination; a ranking strategy |

What DSH is planned to do instead is in the PRD: the flow (Wave 2 task 1), its repeats and failures
(Wave 2 task 4), a results endpoint returning keywords and sentences by token (Wave 2 task 2), text
extraction from PDF and HTML (Wave 3 task 1), corpus term statistics in place of Solr (Wave 3
task 2), and keyword and sentence ranking with bounded limits (Wave 3 tasks 3 to 6).

### 2.2 Constraints found while designing

- **Links into these files.** `.github/copilot-instructions.md`, the prompts under
  `.github/copilot/prompts/` and three issue templates name the four feature specs and the OpenAPI
  document. AC007 keeps every file outside `specs/` unchanged, so the five files keep their names.
- **`api-standards.md` stays the standard.** Its URL, status-code, error-body and security rules
  describe a target the two endpoints do not meet. The issue keeps the file as it is, so the OpenAPI
  document names each departure instead.
- **Tools on this machine.** No `jq`, `yq` or Node on `PATH`. `jq` is `~/apps/jq/jq.exe`; markdownlint
  is under `~/apps/node-v24.21.0-win-x64`, put on `PATH` per command. The validator is Java:
  `org.openapitools:openapi-generator-cli:7.26.0`, the latest release on 2026-10-14. Docker 29.8.1
  is running. (Corrected in the local review: CI's validator is redocly, not this one.
  `spec-validation.yml` runs `redocly lint specs/api/openapi/dsh-rest-api.yaml` with the
  `recommended` rules, so AC004 is redocly's verdict, run with `npx @redocly/cli`.
  `openapi-generator-cli` stays as the converter AC001's check uses, and as a second validator.)
- **A local run needs MongoDB and RabbitMQ** until Wave 1 task 8 and Wave 2 task 1 remove them. The
  `http-integration-tests` profile of `dsh-rest-api/pom.xml` shows how: `mongo:6` with
  `src/test/docker/mongo-init.js` mounted, `rabbitmq:3`, and the application started with
  `--mongo.host`, `--mongo.port`, `--mongo.user=dshuser`, `--mongo.password=dshpass`,
  `--spring.rabbitmq.host` and `--spring.rabbitmq.port`.

## 3. Design

### 3.1 Decisions

1. **The code is the baseline, and a live run is the evidence.** What the OpenAPI document says a
   request answers is what Task 1 observes against a running `dsh-rest-api`. Where Task 1 contradicts
   §3.2, the run wins, §3.2 is corrected in this file before Task 2, and the human is told if the
   correction changes a decision.
2. **The four feature specs keep their names and get one shape**: an overview, a **Today** section
   that describes only what the code does, a **Planned** section in which every item names the wave
   task or issue that plans it, and references. What no wave plans is removed, and §3.4 lists it with
   the reason. Keeping it marked "not planned" was rejected: the next story would have to triage it
   again. Merging files was rejected: it breaks links from `.github/`.
3. **The OpenAPI document holds no `enum` of statuses.** `status`'s description points at the
   generated `specs/architecture/document-status-workflow.md` and at `DocumentStatus`. A copied list
   would go stale the day Wave 1 task 12 lands, with no test to say so.
4. **Departures from `api-standards.md` are stated, not hidden.** `info.description` lists them, and
   each operation's description names the issue that would change its part: `#138` for status codes.
   No issue plans the path prefix or authentication, and the document says so.
5. **Actuator is outside the contract.** The document mentions that `/actuator/health` and
   `/actuator/info` exist and that `#45` owns them, and describes no operation for them. AC001 is
   about `DocumentResource`.
6. **AC001's check compares operations, not prose.** An operation is the pair of upper-case HTTP
   method and path. The check derives the set from Springfox's `/v2/api-docs?group=dsh-app` and from
   the YAML (converted to JSON by `openapi-generator-cli generate -g openapi`), and the two sets must
   be equal. If Springfox does not answer, the controller's `@RequestMapping` values are the source,
   and §8 says which was used.
7. **The check runs once, for this story.** It is a script in this spec, run in Task 2 and Task 5,
   not a test in the build (issue, Out of Scope).

### 3.2 The OpenAPI document

`specs/api/openapi/dsh-rest-api.yaml` is rewritten whole.

- **`openapi: 3.0.3`.** `info.title` stays `DSH REST API`; `info.version` stays `1.0.0`.
- **`info.description`** says, in order:
  - the document describes the API `dsh-rest-api` serves today, and an API change starts here
    (`.github/copilot/rules/api-standards.md`, "OpenAPI First");
  - the departures from `api-standards.md`: paths are under `/v1/dsh/document`, not `/api/v1`, and
    name actions (`submit`) rather than resources (no issue plans a change); a handled failure
    answers `200` (`#138`); no request is authenticated (no issue plans it);
  - `/actuator/health` and `/actuator/info` exist and are outside this document (`#45`);
  - the feature specs, by path: `specs/features/rest-api-endpoints.md`,
    `specs/features/document-analysis.md`, `specs/features/indexing-workflow.md`,
    `specs/features/smart-highlighting.md`.
- **`servers`:** `http://localhost:8080`, described as a local run of `dsh-rest-api`.
- **`tags`:** `documents` only.
- **`POST /v1/dsh/document/submit`**, `operationId: submitDocument`, tag `documents`.
  - Description: stores a document and queues it for processing; the answer is sent before the
    document is stored, so `/status` can answer `TOKEN_NOT_FOUND` for a moment and a store failure
    does not reach the client (Wave 2 task 1 changes this); every submission gets a new token, the
    same file included; the token is opaque. References `specs/features/rest-api-endpoints.md` and
    `specs/features/document-analysis.md`.
  - `requestBody`, required, `multipart/form-data`: `title` (`string`, required, "must not be
    blank"), `contents` (`string`, `format: binary`, required).
  - `200`: `TokenDto`, with examples `accepted` (`token` a sample, `message` `""`) and
    `blankTitle` (`token` `ERROR`, `message`
    `Error submitting file: Document title and contents can't be null.`). The description says a
    caught failure also answers here, with `token` `ERROR` (`#138`).
  - `400`: `ErrorBody`, "a part is missing, or the request is not `multipart/form-data`", with the
    body Task 1 captured as its example. (Corrected by Task 1: B6, a JSON body, answers this same
    `400`.)
  - No further code: B11 showed that a store that cannot be reached still answers `200` with a
    token.
- **`GET /v1/dsh/document/status/{token}`**, `operationId: getDocumentStatus`, tag `documents`.
  - Path parameter `token`, `string`, required: "the token `/submit` returned".
  - Description: references `specs/features/rest-api-endpoints.md` and
    `specs/features/indexing-workflow.md`.
  - `200`: `DocumentStatusDto`, with examples `known` (`QUEUED_FOR_INDEXING_SUCCESS` and its
    message, the status Task 1 recorded) and `tokenNotFound` (`TOKEN_NOT_FOUND`,
    `No token was found with the contents: <token>`). The description says an unknown token answers
    here (`#138`).
  - `500`: `ErrorBody`, "the document store cannot be reached", with the body B10 captured. B10
    answered after about 30 seconds, the store client's own timeout, and the description says so.
- **`components.schemas`:**
  - `TokenDto`: `token` (`string`: "the document's token, or `ERROR` when the submission failed"),
    `message` (`string`: "empty on success; the reason on failure").
  - `DocumentStatusDto`: `status` (`string`: "`DocumentStatus`'s description, which is the status's
    name except for `NOT_INDEXED_ERROR` (Wave 1 task 12); the statuses are listed in
    `specs/architecture/document-status-workflow.md`; or `TOKEN_NOT_FOUND`"), `message`
    (`string`: "the status's message, or the reason the token was not found").
  - `ErrorBody`: Spring Boot's default error attributes, with exactly the fields Task 1's `400` body
    holds, each typed as observed. Task 1 observed `timestamp` (date-time string), `status`
    (integer), `error` (string) and `path` (string), and no `message`.
- **`security: []` on each operation**, which declares that no authentication applies. It is not a
  security scheme. redocly's `security-defined` rule fails an operation that declares no security
  at all. (Corrected in the local review: this line said "no `security`".)
- **No `securitySchemes`, no `parameters` or `responses` components**, and none of
  `AnalysisOptions`, `AnalysisResult`, `Highlight`, `HighlightCollection`, `Position`,
  `HealthStatus`, `ErrorResponse`.

### 3.3 The feature specs

Every file takes this outline:

```text
# <Title> Specification

## Overview          what the feature is, in two or three sentences
## Today             what the code does, nothing else
## Planned           one item per change, each naming its wave task or issue
## References        files that exist and describe the subject
```

A file may give **Today** or **Planned** subsections. Requirement IDs (`FR001`…) are dropped: they
named requirements that no wave plans, and none is referenced from outside the file (Task 4 checks
this before removing them). (Corrected during the build: Task 4's check found two that are.
`indexing-workflow.md`'s FR001 is cited by ADR-001 and ADR-003, and its FR004 by ADR-001 and by
the PRD's Wave 9 task 1. Both are kept in that file's Planned section, each on what still holds.
Every other ID is dropped.)

**`rest-api-endpoints.md` — the readable form of §3.2.**

- Today: the base URL `http://localhost:8080`; no authentication; each operation with a request and
  response example; a table of outcomes — accepted, blank title, missing part, unknown token, and
  any store failure Task 1 observed — with code and body; the departures of §3.1 decision 4; the
  actuator note.
- Planned: a results endpoint returning a document's keywords and sentences by token (Wave 2
  task 2); `4xx` codes for the failures (`#138`); `/submit` storing and sending before it answers
  (Wave 2 task 1).

**`document-analysis.md` — what happens to a submitted document.**

- Today: `/submit` reads the file and, after answering, stores the document as
  `QUEUED_FOR_INDEXING` and sends its id to RabbitMQ; the enqueue's outcome then gives
  `QUEUED_FOR_INDEXING_SUCCESS`, where Task 1's document stopped, or `QUEUED_FOR_INDEXING_ERROR`;
  nothing consumes the queue, and the analysis modules are not wired to it. The file's type and
  size are not checked. (Corrected by Task 1: the document is never stored as `CREATED`, because
  `storeDocumentAndQueueForProcessing()` advances it before the first store.)
- Planned: one flow carries a document through every status to `SENTENCES_PROCESSED_SUCCESS`
  (Wave 2 task 1); text extraction from PDF and HTML (Wave 3 task 1); corpus term statistics
  (Wave 3 task 2); TF and IDF variants and keyword ranking (Wave 3 tasks 3 and 4); sentence
  similarity and ranking (Wave 3 tasks 5 and 6), against the two papers (Wave 1 task 7).

**`indexing-workflow.md` — the stages after a submission.**

- Today: the enqueue on RabbitMQ, and the statuses it leads to; nothing indexes a document.
- Planned, from ADR-003 section 1: one `IntegrationFlow` with stages as endpoints, and one recorder
  of every status (Wave 2 task 1); the statuses as `specs/architecture/document-status-workflow.md`
  draws them; a stage's own failure is final and recorded, a store's failure is retried by
  redelivery and loses no message, and a repeat resumes from the document's status (Wave 2 task 4,
  ADR-003 section 1 "Failures"); delivery is at least once and the response does not wait for the
  processing; text extraction as the indexing stage (Wave 3 task 1); corpus term statistics in place
  of a search engine (Wave 3 task 2, ADR-003 section 5).
- Removed: Solr everywhere (ADR-003 sections 2 and 6), the three retries and the "Analysis timeout"
  row (overridden by ADR-003 section 1, "Failures"), FR004's 10 concurrent tasks (§3.4).
- Kept as Planned (corrected during the build): FR001's asynchronous response and at-least-once
  delivery (ADR-003 section 1, "Failures"), and FR004's 30-second target for 95% of documents
  under 10 MB (Wave 9 task 1).

**`smart-highlighting.md` — a document's smart highlights are its keywords and top sentences.**

- Today: `Document` holds a sorted set of `Keyword` and of `Sentence`; nothing fills them, and no
  endpoint returns them.
- Planned: the results endpoint returns both by token, empty until Wave 3 fills them (Wave 2
  task 2); keyword ranking with a limit on the number returned (Wave 3 task 4); top sentences with
  their paragraph numbers and a limit (Wave 3 task 6).

### 3.4 What is removed, and why

Each line is removed because no wave or issue plans it. A later story that wants one adds it to the
PRD first.

| Removed | From |
|---|---|
| `/api/v1/documents`, `/documents/{documentId}`, `/highlights`, `/health` as DSH routes | OpenAPI, all four specs |
| JWT bearer authentication | OpenAPI, `rest-api-endpoints.md` |
| `documentId`, `202 Accepted`, `processing` / `completed` / `failed` | OpenAPI, all four specs |
| `AnalysisOptions`: highlight types, confidence threshold, maximum highlights | OpenAPI, `document-analysis.md` |
| Semantic analysis, pattern recognition, named entity recognition | `document-analysis.md`, `smart-highlighting.md` |
| DOC and DOCX, the 50 MB limit | `document-analysis.md`, `rest-api-endpoints.md` |
| Highlight IDs, types, confidence, positions, metadata, filters, pagination, ranking strategy | OpenAPI, `smart-highlighting.md`, `rest-api-endpoints.md` |
| 10 concurrent tasks; the 30-second SLA in `document-analysis.md`, which `indexing-workflow.md`'s FR004 now carries as planned (Wave 9 task 1, corrected during the build) | `document-analysis.md`, `indexing-workflow.md` |
| The error body of `api-standards.md` | OpenAPI, `rest-api-endpoints.md` |
| Solr indexing and every Solr row | `indexing-workflow.md`, `document-analysis.md` (ADR-003 removes Solr) |
| Three retries of a failed task, "Analysis timeout" | `indexing-workflow.md` (ADR-003 overrides them) |
| References to `../testing/…` and `../requirements/…` | the specs' references: the directories hold empty scaffolding READMEs (Wave 1 task 4) |

The limits on keywords and sentences are not in this table: Wave 3 tasks 4 and 6 plan them.

### 3.5 Out of scope

As the issue says: changing the code toward `api-standards.md`; `NOT_INDEXED_ERROR`'s description
(Wave 1 task 12); serving the document or a UI (`#144`); a build test holding the document to the
controller; `specs/features/pom-hierarchy-migration.md`; the Copilot prompts. Also:

- `specs/api/postman/README.md` and `specs/api/schemas/README.md`, scaffolding for Wave 1 task 4.
- `specs/architecture/system-design.md`, corrected by the stories that remove MongoDB, RabbitMQ and
  Solr (ADR-003 follow-up candidate 7).

## 4. Verification design

No production code changes, so gates 1 and 2 are untouched by the diff. They run anyway in Task 5,
because the commit must build, and `DocumentResourceHttpIT` is the test that pins the behaviour the
document describes. Gate 3 does not apply: no code touching an external system or an entry point
changes.

| AC | Check | Where |
|---|---|---|
| AC001 | The operation sets from Springfox and from the YAML are equal (§3.1 decision 6) | Task 2 Steps 1 and 5, Task 5 |
| AC002 | Each documented outcome matches the response Task 1 recorded; `DocumentResourceHttpIT` passes | Task 1, Task 2 Step 4, Task 5 |
| AC003 | No `securitySchemes` key, and every `security` is `[]`; Task 1's calls were made at the `servers` URL | Task 2 Step 5 |
| AC004 | `redocly lint`, CI's command, exits 0; `openapi-generator-cli validate` exits 0 | Task 2 Steps 1 and 5 |
| AC005 | The terms of §3.4 appear in `specs/features/` only where a Planned section names a wave task; every Planned item names one | Task 3, Task 4, Task 5 |
| AC006 | `indexing-workflow.md` has no `Solr`, `3 times`, `Analysis timeout` | Task 3 Steps 1 and 3 |
| AC007 | markdownlint exits 0; `git diff --name-only DEVELOP...HEAD` lists only `specs/` | Task 5 |

## 5. Files to change

| File | Change |
|---|---|
| `specs/api/openapi/dsh-rest-api.yaml` | Rewritten (§3.2) |
| `specs/features/rest-api-endpoints.md` | Rewritten (§3.3) |
| `specs/features/document-analysis.md` | Rewritten (§3.3) |
| `specs/features/indexing-workflow.md` | Rewritten (§3.3) |
| `specs/features/smart-highlighting.md` | Rewritten (§3.3) |
| `specs/stories/162-openapi-and-feature-specs-describe-the-api.md` | This spec; §3.2 corrected by Task 1 if needed, §8 filled in |
| `specs/architecture/ADR-001-GCP-based-components.md` | One parenthetical, which cited `document-analysis.md`'s removed FR001 and its 50 MB (added in the local review, §8.6) |

Nothing else. Logs and scratch output go to `.logs/`, which is gitignored.

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order: Task 1's observations feed Tasks 2 and 3.

**Goal:** the OpenAPI document and the four feature specs describe the two operations
`dsh-rest-api` serves, as a live run shows them, with planned work marked and unplanned content
removed.

**Architecture:** a live run of `dsh-rest-api` against MongoDB and RabbitMQ in Docker is the
evidence. The YAML is rewritten from it and checked by a validator and by an operation-set
comparison with Springfox's own description; the Markdown is rewritten from it and from ADR-003 and
the PRD.

**Tech stack:** OpenAPI 3.0.3, `openapi-generator-cli` 7.26.0, Docker (`mongo:6`, `rabbitmq:3`),
curl, `jq`, markdownlint.

**Spec:** this file.

### Global constraints

- Documentation only. No file outside `specs/` changes (AC007).
- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs").
- Commands run in Git Bash. Tools are put on `PATH` per command:
  `PATH="$HOME/apps/jq:$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- The containers this plan starts are named `dsh-162-mongo` and `dsh-162-rabbit`, on host ports
  `27117` and `5772`, and are removed at the end of Task 1 and Task 5.
- Status values are never copied into the YAML as a list (§3.1 decision 3).

### Task 1: Record the live baseline

**Files:** `.logs/162-*` only; this spec's §3.2 if the run contradicts it.

- [ ] **Step 1: Build the jar.**

  ```bash
  mkdir -p .logs
  mvn -B -DskipTests install > .logs/mvn-install-skiptests.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-install-skiptests.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`, and `dsh-rest-api/target/dsh-rest-api-0.4.0-SNAPSHOT.jar` exists.

- [ ] **Step 2: Start MongoDB and RabbitMQ.**

  ```bash
  MSYS_NO_PATHCONV=1 docker run -d --name dsh-162-mongo -p 127.0.0.1:27117:27017 \
    -v "$(pwd -W)/dsh-rest-api/src/test/docker/mongo-init.js:/docker-entrypoint-initdb.d/mongo-init.js:ro" \
    mongo:6
  docker run -d --name dsh-162-rabbit -p 127.0.0.1:5772:5672 rabbitmq:3
  ```

  Then wait until `docker logs dsh-162-mongo` shows `init process complete` followed by
  `Waiting for connections`, and `docker logs dsh-162-rabbit` shows `Server startup complete`.

- [ ] **Step 3: Start the application on 8080.** Run in the background, logged:

  ```bash
  java -jar dsh-rest-api/target/dsh-rest-api-0.4.0-SNAPSHOT.jar \
    --mongo.host=localhost --mongo.port=27117 --mongo.user=dshuser --mongo.password=dshpass \
    --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5772 \
    > .logs/162-app.log 2>&1
  ```

  Expected: `.logs/162-app.log` shows `Started DshRestApplication` and Tomcat on port 8080.

- [ ] **Step 4: Record every outcome.** For each call, record the HTTP code and body in
  `.logs/162-baseline.txt` with `curl -s -w '\nHTTP %{http_code}\n'`. `PDF` is
  `dsh-test-dataset/src/test/resources/pdf/bbc-news-1.pdf`, `BASE` is
  `http://localhost:8080/v1/dsh/document`.

  | # | Call | Expected |
  |---|---|---|
  | B1 | `POST $BASE/submit -F title='A title' -F contents=@$PDF` | `200`, a token, `message` `""` |
  | B2 | `GET $BASE/status/<B1 token>` at once, then after 10 s | `200`; the first may be `TOKEN_NOT_FOUND` or `CREATED`; record the second, the status a document stops at |
  | B3 | `POST $BASE/submit -F title= -F contents=@$PDF` | `200`, `ERROR`, the blank-title message |
  | B4 | `POST $BASE/submit -F title='A title'` | `400` and Spring's error body |
  | B5 | `POST $BASE/submit -F contents=@$PDF` | `400` and Spring's error body |
  | B6 | `POST $BASE/submit -H 'Content-Type: application/json' -d '{}'` | record |
  | B7 | `GET $BASE/status/no-such-token` | `200`, `TOKEN_NOT_FOUND`, the not-found message |
  | B8 | `GET http://localhost:8080/v2/api-docs?group=dsh-app` | `200` and a Swagger 2 JSON; save it as `.logs/162-api-docs.json` |
  | B9 | `GET http://localhost:8080/actuator/health` and `/actuator/info` | record the codes |
  | B10 | `docker stop dsh-162-mongo`, then `GET $BASE/status/<B1 token>` with `--max-time 120` | record; `500` and Spring's error body expected |
  | B11 | With MongoDB still stopped, `POST $BASE/submit -F title='A title' -F contents=@$PDF` | `200` and a token expected (§2.1, `@Async`) |

- [ ] **Step 5: Compare with §3.2.** For each row whose result differs from "Expected", correct
  §3.2 and §3.3 in this file to what was observed. If a difference changes a decision of §3.1 — for
  example, B8 fails and the fallback of decision 6 applies — note it for the human.

- [ ] **Step 6: Stop everything.** Stop the application, then
  `docker rm -f dsh-162-mongo dsh-162-rabbit`.

### Task 2: Rewrite the OpenAPI document

**Files:** modify `specs/api/openapi/dsh-rest-api.yaml`.

**Interfaces:** consumes `.logs/162-baseline.txt` and `.logs/162-api-docs.json` from Task 1.
Produces the operation IDs `submitDocument` and `getDocumentStatus`, and the schemas `TokenDto`,
`DocumentStatusDto` and `ErrorBody`, which Task 3 names in `rest-api-endpoints.md`.

- [ ] **Step 1: Write the checks and watch them fail on today's file.** Fetch the validator once:

  ```bash
  mvn -B -N dependency:copy -Dartifact=org.openapitools:openapi-generator-cli:7.26.0 \
    -DoutputDirectory=.logs > .logs/mvn-dependency-copy.log 2>&1
  echo "maven exit=$?"
  ```

  Write `.logs/162-check-operations.sh`:

  ```bash
  #!/usr/bin/env bash
  # AC001: the YAML's operations equal Springfox's. Usage: 162-check-operations.sh
  set -euo pipefail
  export PATH="$HOME/apps/jq:$PATH"
  CLI=.logs/openapi-generator-cli-7.26.0.jar
  rm -rf .logs/162-openapi-json
  java -jar "$CLI" generate -g openapi -i specs/api/openapi/dsh-rest-api.yaml \
    -o .logs/162-openapi-json > .logs/162-generate.log 2>&1
  ops='.paths | to_entries[] | .key as $p | .value | keys[]
       | select(test("^(get|put|post|delete|patch|head|options)$"))
       | "\(ascii_upcase) \($p)"'
  jq -r "$ops" .logs/162-openapi-json/openapi.json | sort > .logs/162-ops-yaml.txt
  jq -r "$ops" .logs/162-api-docs.json | sort > .logs/162-ops-springfox.txt
  diff .logs/162-ops-springfox.txt .logs/162-ops-yaml.txt && echo "AC001: operations match"
  ```

  If Task 1's B8 failed, replace the Springfox line with the two operations read from
  `DocumentResource`'s annotations, written by hand into `.logs/162-ops-springfox.txt`, and say so
  in §8.

  Run `bash .logs/162-check-operations.sh`. Expected: **FAIL**, a diff showing the four `/documents`
  and `/health` operations against `POST /v1/dsh/document/submit` and
  `GET /v1/dsh/document/status/{token}`.

  Run `java -jar .logs/openapi-generator-cli-7.26.0.jar validate -i specs/api/openapi/dsh-rest-api.yaml`
  and record its result on today's file.

- [ ] **Step 2: Rewrite the YAML** to §3.2 as corrected by Task 1, with the bodies Task 1 recorded
  as examples. Every operation has a `summary` and a `description` (`api-standards.md`,
  "Documentation").

- [ ] **Step 3: Validate.** `java -jar .logs/openapi-generator-cli-7.26.0.jar validate -i
  specs/api/openapi/dsh-rest-api.yaml`. Expected: `No validation issues detected.` and exit 0.

- [ ] **Step 4: Hold each outcome to the baseline.** For B1, B3, B4, B5, B7 and every further code
  Task 1 recorded, confirm the YAML documents that code with a schema whose fields are those of the
  recorded body. Record the comparison in §8.

- [ ] **Step 5: Run the checks green.** `bash .logs/162-check-operations.sh` prints
  `AC001: operations match`. `grep -nE 'securitySchemes|^security:|bearer' specs/api/openapi/dsh-rest-api.yaml`
  prints nothing. `grep -n 'url:' specs/api/openapi/dsh-rest-api.yaml` shows
  `http://localhost:8080`, the URL Task 1 called.

- [ ] **Step 6: Commit.**

  ```bash
  git add specs/api/openapi/dsh-rest-api.yaml
  git commit -m "docs(#162): describe the two operations dsh-rest-api serves in the OpenAPI document"
  ```

### Task 3: Rewrite `indexing-workflow.md` and `rest-api-endpoints.md`

**Files:** modify `specs/features/indexing-workflow.md`, `specs/features/rest-api-endpoints.md`.

**Interfaces:** consumes Task 1's B2 status, Task 2's operation IDs and schema names.

- [ ] **Step 1: The red check.**

  ```bash
  grep -nE 'Solr|3 times|Analysis timeout' specs/features/indexing-workflow.md
  grep -nE '/api/v1|Bearer|documentId|/health' specs/features/rest-api-endpoints.md
  ```

  Expected: matches in both (they are the content being replaced).

- [ ] **Step 2: Rewrite both files** to §3.3, in the outline of §3.3. `indexing-workflow.md`'s
  Planned items cite ADR-003 by section; `rest-api-endpoints.md`'s outcome table carries the codes
  and bodies of Task 1, and names `TokenDto`, `DocumentStatusDto` and `ErrorBody` as the YAML does.

- [ ] **Step 3: The green check.** Rerun Step 1. Expected: `indexing-workflow.md` gives no match.
  `rest-api-endpoints.md` gives matches only on lines that state a departure or a Planned item.

- [ ] **Step 4: Commit.**

  ```bash
  git add specs/features/indexing-workflow.md specs/features/rest-api-endpoints.md
  git commit -m "docs(#162): correct the indexing workflow and the endpoint spec against the code and ADR-003"
  ```

### Task 4: Rewrite `document-analysis.md` and `smart-highlighting.md`

**Files:** modify `specs/features/document-analysis.md`, `specs/features/smart-highlighting.md`.

- [ ] **Step 1: Check the requirement IDs are not referenced from outside.**

  ```bash
  git grep -nE '(document-analysis|smart-highlighting|indexing-workflow|rest-api-endpoints)\.md.{0,40}FR0' -- ':!specs/features'
  ```

  Expected: no match. If one appears, keep that ID in its file and say so in §8.

- [ ] **Step 2: The red check.**

  ```bash
  grep -nE 'DOCX|50 MB|[Ss]emantic|[Nn]amed entity|confidence|pagination|AnalysisOptions|Solr' \
    specs/features/document-analysis.md specs/features/smart-highlighting.md
  ```

  Expected: matches in both.

- [ ] **Step 3: Rewrite both files** to §3.3.

- [ ] **Step 4: The green check.** Rerun Step 2. Expected: no match.

- [ ] **Step 5: Commit.**

  ```bash
  git add specs/features/document-analysis.md specs/features/smart-highlighting.md
  git commit -m "docs(#162): describe document analysis and smart highlights as built and as planned"
  ```

### Task 5: The gates and the acceptance checks

- [ ] **Step 1: AC005 across the four specs.**

  ```bash
  grep -nE '/api/v1|documentId|processing`|completed`|DOCX|50 MB|[Ss]emantic|[Nn]amed entity|JWT|Bearer|pagination|Solr|3 times|Analysis timeout' specs/features/{document-analysis,indexing-workflow,rest-api-endpoints,smart-highlighting}.md
  ```

  Every match is on a line that states a departure or sits in a Planned section. Then read each
  Planned section: every item names a wave task or an issue. Record both in §8.

- [ ] **Step 2: Markdown lint**, the command in `CLAUDE.md`, with Node on `PATH`. Expected: exit 0.

- [ ] **Step 3: AC007.** `git diff --name-only DEVELOP...HEAD` lists only paths under `specs/`.

- [ ] **Step 4: Gates 1 and 2.**

  ```bash
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`.

- [ ] **Step 5: AC002's pinned outcomes.** `DocumentResourceHttpIT` is the test that asserts them.

  ```bash
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api > .logs/mvn-verify-it-rest-api.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-it-rest-api.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`, `DocumentResourceHttpIT` with 4 tests run and 0 failures.

- [ ] **Step 6: Rerun AC001 and AC004** from Task 2 Steps 3 and 5 on the final tree.

- [ ] **Step 7: Fill in §8 and commit.**

  ```bash
  git add specs/stories/162-openapi-and-feature-specs-describe-the-api.md
  git commit -m "docs(#162): record the verification results"
  ```

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: the document describes exactly `DocumentResource`'s operations; a check finds the sets equal | §3.1 decision 6, Task 2 Steps 1 and 5, Task 5 Step 6 |
| AC002: responses as the code returns them; departures stated, `#138` cited | §3.1 decisions 1 and 4, Task 1, Task 2 Step 4, Task 5 Step 5 |
| AC003: no security scheme; the server URL answers both operations | §3.2, Task 1 Step 4, Task 2 Step 5 |
| AC004: valid OpenAPI 3.0.3, validator named | §2.2, Task 2 Steps 3 and 5 |
| AC005: the specs present only today as current; planned items name their task; removals justified | §3.3, §3.4, Tasks 3 and 4, Task 5 Step 1 |
| AC006: `indexing-workflow.md` without Solr, three retries, "Analysis timeout"; pointers to ADR-003 section 1 and Wave 2 task 4 | §3.3, Task 3 |
| AC007: markdownlint passes; nothing outside `specs/` changes | Task 5 Steps 2 and 3 |

## 8. Verification results

Built on 2026-10-14, against `dsh-rest-api-0.4.0-SNAPSHOT.jar` from `mvn -B -DskipTests install`
(`maven exit=0`). Scratch output is in `.logs/162-*`, which is gitignored.

### 8.1 Task 1: the live baseline

MongoDB (`mongo:6`) on `127.0.0.1:27117` and RabbitMQ (`rabbitmq:3`) on `127.0.0.1:5772`;
`Started DshRestApplication`, Tomcat on 8080 with context path `''`.

| # | Call | Observed | Against "Expected" |
|---|---|---|---|
| B1 | `POST /submit`, title and file | `200` `{"token":"2026101461d279be-cc9e-40f3-a373-cd3fdabdcdf7","message":""}` | as expected |
| B2 | `GET /status/<B1>` at once, then after 10 s | `200` `QUEUED_FOR_INDEXING` / "Document id sent to the indexing queue to be processed.", then `200` `QUEUED_FOR_INDEXING_SUCCESS` / "Document id successfuly queued for indexing." | **differs**: the first answer is neither `TOKEN_NOT_FOUND` nor `CREATED`. The document is advanced to `QUEUED_FOR_INDEXING` before its first store, so `CREATED` is never stored. A document stops at `QUEUED_FOR_INDEXING_SUCCESS` |
| B3 | `POST /submit`, blank title | `200` `{"token":"ERROR","message":"Error submitting file: Document title and contents can't be null."}` | as expected |
| B4 | `POST /submit`, no `contents` | `400` `{"timestamp":"2026-10-14T16:10:01.339+00:00","status":400,"error":"Bad Request","path":"/v1/dsh/document/submit"}` | as expected; the body has no `message` |
| B5 | `POST /submit`, no `title` | `400`, the same four fields | as expected |
| B6 | `POST /submit`, JSON `{}` | `400`, the same four fields | recorded: a non-multipart body is the same `400` |
| B7 | `GET /status/no-such-token` | `200` `{"status":"TOKEN_NOT_FOUND","message":"No token was found with the contents: no-such-token"}` | as expected |
| B8 | `GET /v2/api-docs?group=dsh-app` | `200`, Swagger 2.0, paths `/v1/dsh/document/status/{token}` and `/v1/dsh/document/submit` | as expected: Springfox answers, so decision 6's fallback was not needed |
| B9 | `/actuator/health`, `/actuator/info` | `200` `{"status":"UP"}`, `200` `{}` | recorded |
| B10 | MongoDB stopped, `GET /status/<B1>` | `500` `{"timestamp":"2026-10-14T16:10:43.987+00:00","status":500,"error":"Internal Server Error","path":"/v1/dsh/document/status/2026101461d279be-cc9e-40f3-a373-cd3fdabdcdf7"}`, after 31 s (`MongoTimeoutException: Timed out after 30000 ms`) | as expected |
| B11 | MongoDB stopped, `POST /submit` | `200` with a token, in under 1 s | as expected. The app was stopped while the `@Async` store (`task-2`) was still waiting on MongoDB's 30-second timeout, so its failure was not seen. That the document is then not stored comes from the code: `storeDocument` throws outside the `try` in `storeDocumentAndQueueForProcessing()` |

**Corrections made to this file from Task 1** (Step 5): §3.2's `/submit` `400` also covers a
non-multipart request (B6) and no further code exists (B11); `/status`'s `known` example is
`QUEUED_FOR_INDEXING_SUCCESS` and its `500` is documented with B10's body and the 30-second wait;
`ErrorBody` holds `timestamp`, `status`, `error` and `path`. §3.3's `document-analysis.md` Today says
`QUEUED_FOR_INDEXING`, not `CREATED`. None changes a decision of §3.1.

Springfox's description also lists `201`, `401`, `403` and `404`. Those are Springfox's default
responses, not codes the application returns, and the YAML does not carry them. It types `contents`
as a body parameter and `title` as a query parameter, which is how Springfox renders a
`@RequestParam` beside a `MultipartFile`; the YAML follows the request the run made, two parts of
one `multipart/form-data` body.

### 8.2 Task 2: the OpenAPI document

- **AC001.** Red on today's file: `diff` showed `GET /documents/{documentId}`,
  `GET /documents/{documentId}/highlights`, `GET /health`, `POST /documents` against Springfox's
  `GET /v1/dsh/document/status/{token}` and `POST /v1/dsh/document/submit`. Green after the rewrite,
  and again in Task 5: `AC001: operations match`. The Springfox side is B8's
  `.logs/162-api-docs.json`; no operation was written by hand.
- **AC004.** `openapi-generator-cli` 7.26.0 `validate`: `No validation issues detected.`, exit 0,
  on the old file and on the new one. So the validator alone would never have caught the drift.
  `generate -g openapi` logs one `WARN Failed to get the schema name: null`, from the inline
  multipart schema it skips as an unused form model; `openapi.json` is written in full.
- **AC003.** `grep -nE 'securitySchemes|^security:|bearer'` prints nothing. `servers` is
  `http://localhost:8080`, the URL every call of Task 1 was made at. Since the local review each
  operation declares `security: []` (§8.6).
- **AC002, Step 4.** Each recorded body's field names were compared with its documented schema's
  properties, by `jq` over the generated `openapi.json`:

  | Row | Code | Schema | Fields | Result |
  |---|---|---|---|---|
  | B1, B3 | `200` | `TokenDto` | `message`, `token` | equal |
  | B4, B5, B6 | `400` | `ErrorBody` | `error`, `path`, `status`, `timestamp` | equal |
  | B2, B7 | `200` | `DocumentStatusDto` | `message`, `status` | equal |
  | B10 | `500` | `ErrorBody` | `error`, `path`, `status`, `timestamp` | equal |

### 8.3 Tasks 3 and 4: the feature specs

- **Task 3.** Red: `indexing-workflow.md` matched `Solr`, `3 times` and `Analysis timeout` on 10 lines,
  `rest-api-endpoints.md` matched on 11. Green: `indexing-workflow.md` has no match (AC006).
  `rest-api-endpoints.md` matches on two lines: the departure "not `/api/v1`", and the actuator note
  of §3.1 decision 5, where `/health` matches as part of `/actuator/health`. That line describes a
  route outside the contract, not a DSH route.
- **Task 4 Step 1 found references.** The regex of Step 1 matched only
  `specs/stories/160-adr-003-document-pipeline.md`, a closed story quoting the old text. A wider
  `git grep '\bFR00[0-9]'` found the IDs that live documents cite: `indexing-workflow.md`'s FR001 in
  ADR-003 (section 1, "Failures", and "Consequences for the waves") and ADR-001 (§3.2), and its FR004
  in ADR-001 (Phase 5, task 5.3) and in the PRD's Wave 9 task 1. By Step 1's rule both IDs are kept in
  `indexing-workflow.md`, in its Planned section, on the part that still holds: FR001 on the
  asynchronous response and at-least-once delivery, which ADR-003 keeps, and FR004 on the 30-second
  target, which Wave 9 task 1 plans. That also corrects §3.4: the 30-second figure is planned, not
  unplanned. The 10 concurrent tasks stay removed. Every other ID is dropped. The FR references in
  the poms and in `specs/devops/` name other documents' requirements. §3.3 and §3.4 are corrected
  above, and the change is its own commit. (Corrected in the local review: the wider grep also
  matched `ADR-001-GCP-based-components.md:121`, which cites `document-analysis.md`'s FR001 for a
  50 MB limit. That FR001 and the limit are removed here, so the citation was dangling and this
  paragraph missed it. The ADR's parenthetical is corrected, §8.6 item 2.)
- **Task 4.** Red: both files matched (11 lines in `document-analysis.md`, 7 in
  `smart-highlighting.md`). Green: no match.

### 8.4 Task 5: the gates and the acceptance checks

- **AC005.** The grep of Step 1 over the four specs matches one line,
  `rest-api-endpoints.md:94`, a departure. Every Planned item in the four files names a wave task or
  an issue: Wave 1 tasks 7, 8, 9, 10 and 12, Wave 2 tasks 1 to 5, Wave 3 tasks 1 to 6, Wave 6 and
  Wave 9 task 1, `#138` and `#142`.
- **AC007.** markdownlint, the command in `CLAUDE.md`: exit 0. `git diff --name-only DEVELOP...HEAD`
  lists `specs/api/openapi/dsh-rest-api.yaml`, the four files under `specs/features/` and this
  file, and nothing else.
- **Gates 1 and 2.** `mvn -B clean install`: `maven exit=0`, `BUILD SUCCESS` in 1:05,
  `All coverage checks have been met` in 8 modules.
- **AC002, pinned.** `mvn -B clean verify -DintegrationTests -pl dsh-rest-api`: `maven exit=0`,
  `DocumentResourceHttpIT` 4 tests run, 0 failures, 0 errors.
- **Gate 3** does not apply: no code changed.
- **Containers.** `dsh-162-mongo` and `dsh-162-rabbit` were removed at the end of Task 1, and
  `docker ps -a` showed none left after Task 5.

### 8.5 Found along the way, not fixed here

- **Ties in the ranked sets.** `KeywordSorter` and `SentenceSorter` return `0` for two entries with
  the same score, and `Document`'s `TreeSet`s use them, so a set keeps only one of two keywords, or
  of two sentences, that score the same. Nothing fills the sets today, so it is latent. Wave 3 tasks
  4 and 6 rank into them. `smart-highlighting.md` states it under Today. Filed as `#163`, with no
  milestone.

### 8.6 The local review

Step 5's review found ten things to fix, one critical, three important and six minor. All ten were
fixed, each checked red before the fix and green after it.

1. **Critical: CI's validator failed the new YAML.** `spec-validation.yml` runs `redocly lint`,
   which AC004 had not named. Red: `@redocly/cli` 2.63.0 gave "Validation failed with 2 errors and
   3 warnings", the errors being `security-defined` on both operations. The old file had passed.
   Fix: `security: []` on each operation. Green: exit 0, "Your API description is valid", with
   three warnings kept as known: `info-license`, `no-server-example.com` (the server is a local
   run), and `operation-4xx-response` (`/status` answers no `4xx`, the `#138` departure).
   `openapi-generator-cli validate` and AC001 stay green.
2. **Important: a dangling citation outside `specs/features/`.** `ADR-001-GCP-based-components.md:121`
   gave the original file's size as "up to 50 MB per FR001 in `document-analysis.md`", both of
   which this story removes, and §8.3 said no such citation existed. Task 4 Step 1's regex could not
   match it, because the ID comes before the file name. Fix: the parenthetical now says no
   requirement bounds the size. The ADR's reason for moving the file to GCS, Firestore's 1 MiB limit
   on one document, holds without it. §5 lists the file, and §8.3 carries the correction.
3. **Important: "the file's size is not checked" was false, and the contract lacked a code.**
   Nothing sets `spring.servlet.multipart.*`, so Spring Boot's defaults apply. A second live run on
   2026-10-14, against the same jar and containers as Task 1 (`.logs/162-baseline-review.txt`),
   recorded:

   | # | Call | Observed |
   |---|---|---|
   | B12 | `POST /submit` with a 2 MB file | `500` `ErrorBody` (`FileSizeLimitExceededException`: "exceeds its maximum permitted size of 1048576 bytes") |
   | B13 | `POST /submit` with an 11 MB file | `500` `ErrorBody` (`SizeLimitExceededException`: request size 11534663 "exceeds the configured maximum (10485760)") |

   Fix: the YAML documents `/submit`'s `500` with B12's body and states the 1 MB and 10 MB limits.
   `rest-api-endpoints.md` gives them, with a row in its outcomes table, and so does
   `document-analysis.md`. `indexing-workflow.md`'s FR004 notes that a 10 MB document cannot be
   submitted today, and that no task plans raising the limit.
4. **Important: inferences were presented as recorded.** The same second run made two of them
   observations, and the rest are marked "(from the code)":

   | # | Call | Observed |
   |---|---|---|
   | B15 | 20 submissions, each followed at once by `/status` | 3 `QUEUED_FOR_INDEXING`, 17 `QUEUED_FOR_INDEXING_SUCCESS`, no `TOKEN_NOT_FOUND` |
   | B16 | MongoDB stopped, `/submit`; 40 s later MongoDB restarted, then `/status` | `200` with a token; the `@Async` store failed after 30 s (`task-6`, `SimpleAsyncUncaughtExceptionHandler`); then `200` `TOKEN_NOT_FOUND` |

   B16 closes B11's gap (§8.1): the unreachable-store row of `rest-api-endpoints.md` is now
   observed, and narrowed to a store still unreachable after its client's 30-second timeout, so
   "never found" is gone. `TOKEN_NOT_FOUND` "for a moment" stays as the code allows it, marked as
   from the code and as never seen in 20 tries, in the YAML and in `rest-api-endpoints.md`. The
   "any other exception" row is marked from the code. The outcomes table's heading names the
   marker.
5. **Minor: the order of the response and the store was stated as a guarantee.** "The answer is
   sent before the document is stored", and "after answering" in two specs. The `@Async` call is
   started before the method returns, and the two race: B2 and B15 show the store usually wins.
   Fix: "`/submit` does not wait for the store or the enqueue", in the YAML and in the three specs
   that said it. §3.2's wording above is the design as written and is left as it was.
6. **Minor: "Where it stops" left out `QUEUED_FOR_INDEXING`.** `DocumentEnqueueResponseMessageHandler`
   advances the document only when a confirm, nack or return reaches it, and stores it once more
   afterwards. If neither happens, or the second store fails, the document stays where the first
   store left it. Fix: `indexing-workflow.md` says so, as from the code.
7. **Minor: a Planned item said `/status` would "never" answer `TOKEN_NOT_FOUND`** for a token
   `/submit` returned. The default store after Wave 1 task 8 is in memory, and a restart empties it.
   Fix: "while the store still holds the document", with the in-memory store's limit stated.
8. **Minor: one departure from `api-standards.md` was missing.** Its "Never expose internal error
   details in production responses" is broken by `/submit`, whose `message` carries the caught
   exception's `getMessage()`. Fix: the departure is listed in the YAML's `info.description` and in
   `rest-api-endpoints.md`. No issue plans it: `#138`'s criteria change the status codes, not the
   message.
9. **Minor: two Planned items cited a task that does not plan them.** "A failed send on a hop
   records the hop's `…_ERROR` status" sat under Wave 2 task 4, which does not list it. Recording it
   is the status recorder's points 2 and 7 (Wave 2 task 1, ADR-003 section 1, "Failures", second
   row), and what the publisher retries first is Wave 6's. Fix: the item stands on its own with
   those citations. The same check found a second one the review had not named: "every stage is
   idempotent" sat under Wave 2 task 4 too, but the PRD says it of Wave 3's stages, and ADR-003
   states it under "A stage". It now cites both.
10. **Minor: `title` as a query parameter was left out.** Springfox renders `title` as `in: query`
    (§8.1). B14, `POST /submit?title=A%20title` with only a `contents` part, answered `200` with a
    token. Fix: the YAML's `title` property and `rest-api-endpoints.md` say it is also accepted that
    way, and the YAML keeps describing the part.

After the ten fixes: `.logs/162-review-checks.sh`, one check per finding, failed all 12 checks
before the fixes and passes all 12 after them. AC001, both validators, markdownlint and AC007 were
run again on the final tree; the results follow this list in §8.7.

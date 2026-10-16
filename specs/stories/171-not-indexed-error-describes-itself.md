---
issue: 171
slug: not-indexed-error-describes-itself
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 171 — `NOT_INDEXED_ERROR` describes itself

## 1. Story

**As a** client of `/v1/dsh/document/status/{token}`
**I want** a document whose indexing failed to answer a status that says so
**So that** I can tell a failed document from one still in progress, and stop polling it

## 2. Context

- Wave: 1, task 12 (`specs/product/PRD.md` §4, Wave 1, "Every status describes itself"), DSH
  milestone `0.4.0-SNAPSHOT`
- Issue: [#171](https://github.com/MRISS-Projects/dsh/issues/171), labelled `bug`, so that the fix
  is in 0.4.0's release notes (PRD §4, re-plan of 2026-10-13, decision 6)
- Parent branch: `DEVELOP`, at `85fb54233` when this spec was written. The task branch is
  `issue-171-not-indexed-error-describes-itself`.

### 2.1 What exists today

`DocumentStatus` (`dsh-data/src/main/java/com/mriss/dsh/data/models/DocumentStatus.java`) has 18
constants, each built from a description and a message. `NOT_INDEXED_ERROR` copies both from
`DEQUEUED_FOR_INDEXING`:

```java
NOT_INDEXED_ERROR("DEQUEUED_FOR_INDEXING", "Document dequeued for indexing.") {
```

Every other description is the constant's own name, and no other two constants share a message.

The defect reaches a client this way:

- `Document.setDocumentStatus` copies `getStatusMessage()` into the stored `documentStatusMessage`
  (`Document.java:177`).
- `DocumentResource` answers `/status/{token}` with `getStatusDescription()` and that stored message
  (`DocumentResource.java:63`).

A document that failed indexing would therefore read as one waiting to be indexed. No production
code reaches `INDEXING` today, so no stored document holds `NOT_INDEXED_ERROR`. No stored message
needs migrating.

`DocumentStatusTest` samples three descriptions and three messages, checks them for presence only,
and so misses the copy. Its `testTransition` asserts that `INDEXING` goes to `NOT_INDEXED_ERROR` on
`ERROR`. That stays true.

The documentation records the defect in these places:

| File | Where | What it says |
|---|---|---|
| `specs/api/openapi/dsh-rest-api.yaml` | `DocumentStatusDto.status` | the status's name "except for `NOT_INDEXED_ERROR` (Wave 1 task 12)" |
| `specs/features/rest-api-endpoints.md` | Today, the status response | "except for `NOT_INDEXED_ERROR`, which answers `DEQUEUED_FOR_INDEXING`" |
| `specs/features/rest-api-endpoints.md` | Planned | "`NOT_INDEXED_ERROR` answers a description of its own" |
| `specs/architecture/ADR-003-document-pipeline.md` | Context table, line 48 | "`NOT_INDEXED_ERROR` carries the description and message of `DEQUEUED_FOR_INDEXING`" |
| `specs/architecture/ADR-003-document-pipeline.md` | Decision on statuses, line 748 | "One defect in a status is recorded as a follow-up candidate" |
| `specs/architecture/ADR-003-document-pipeline.md` | Follow-up candidates, item 5, line 942 | "`NOT_INDEXED_ERROR`'s description and message are those of `DEQUEUED_FOR_INDEXING`" |
| `docs/api/index.html` | the rendered `DocumentStatusDto` | the YAML's text, rendered |

### 2.2 Constraints found while designing

- **`docs/api/index.html` is generated.** `.github/workflows/documentation-sync.yml` runs
  `redocly build-docs` on the OpenAPI file and commits the result. It runs on every push to `DEVELOP`
  that touches `specs/api/openapi/**`, so this story's merge regenerates the page. The task branch
  does not edit it by hand.
- **The status diagram is unaffected.** `DocumentStatusDiagramTest` builds
  `specs/architecture/document-status-workflow.md` from constant names and transitions only. It
  reads no description or message.
- **ADR-003 is Accepted**, and nothing has changed it since `#160` merged. The owner decided on
  2026-10-16 that its three passages each get a dated note, and that the original sentences stay.

## 3. Design

### 3.1 Decisions

1. **The constant gets its own values.** `NOT_INDEXED_ERROR("NOT_INDEXED_ERROR", "Error indexing
   document.")`. The message follows the other error constants ("Error extracting keywords.",
   "Error queueing document for processing keywords and sentences.").
2. **The fields stay.** The constructor still takes a description, and `getStatusDescription()`
   still returns it. Deriving the description from `name()` would make the defect impossible, but
   the issue rules it out of scope.
3. **The test covers every value, not a sample.** The two sampling tests, `testGetStatusDescription`
   and `testGetStatusMessage`, are replaced:
   - `getStatusDescription_forEveryStatus_shouldEqualItsName` loops over
     `DocumentStatus.values()` and asserts that the description equals `name()`.
   - `getStatusMessage_forEveryStatus_shouldBeItsOwn` asserts that every message is non-null and
     non-blank, and that no message belongs to two constants. Its failure message names both
     constants.

   A third test, `getStatusMessage_ofNotIndexedError_shouldSayThatIndexingFailed`, pins AC002's
   wording. `testTransition` is unchanged.
4. **Proof that the test catches a regression.** AC003 asks that reintroducing either copy fails the
   test. Task 1 shows this by putting each copy back on its own, running the test, and restoring
   the fix. The results go in §8.
5. **The documentation drops the exception, and ADR-003 gets dated notes.**
   - The OpenAPI file and `rest-api-endpoints.md` state the rule with no exception.
   - `rest-api-endpoints.md` loses its Planned bullet.
   - In ADR-003, each of the three passages keeps its sentence and gains ``Fixed by `#171` on
     <date>.``. The date is the day Task 2 runs, as `YYYY-MM-DD`.
6. **Historical records are left alone.** Those are the story specs of `#152`, `#160` and `#162`,
   and the PRD's task-12 text. The PRD is reconciled after the merge, in step 8
   (`dsh-reconcile-prd`).

### 3.2 The documentation text

**`specs/api/openapi/dsh-rest-api.yaml`**, `DocumentStatusDto.status.description`:

```yaml
          description: |
            `DocumentStatus`'s description (`dsh-data`), which is the status's name. The statuses
            and their transitions are listed in `specs/architecture/document-status-workflow.md`.
            Or `TOKEN_NOT_FOUND`, when no document has the token.
```

**`specs/features/rest-api-endpoints.md`**, the Today paragraph's first two sentences become:

```markdown
`status` is `DocumentStatus`'s description (`dsh-data`), which is the status's name. The statuses
are listed in `../architecture/document-status-workflow.md`.
```

The rest of that paragraph is unchanged, and so is the paragraph's line wrapping from `Today a
document stops at` on. In Planned, the bullet saying that `NOT_INDEXED_ERROR` answers a description of its
own (Wave 1 task 12) is deleted.

**`specs/architecture/ADR-003-document-pipeline.md`**, with `<date>` as in §3.1 decision 5:

- Line 48, the Context row's first cell:
  `` `NOT_INDEXED_ERROR` carries the description and message of `DEQUEUED_FOR_INDEXING`. Fixed by `#171` on <date> ``
- Line 748, after `` `NOT_INDEXED_ERROR` carries the description and message of another status. ``,
  add the sentence: `` Fixed by `#171` on <date>. ``
- Line 942, candidate 5 ends `` visible through `/status/{token}`. Fixed by `#171` on <date>. ``

### 3.3 Out of scope

As in the issue:

- the spelling of other messages ("successfuly", "keyworkds", "sentendes");
- merging the description into the name, or changing `DocumentStatusDto`'s shape;
- any code path that reaches `NOT_INDEXED_ERROR`, which is Wave 2's and Wave 3's;
- gate 3, because no external system or REST entry point changes.

Also out of scope: `#53`, which moves descriptions and messages into locale-dependent properties
files. It stays open and unchanged. Once this story merges, the "no shared message" test is a
constraint on that work.

## 4. Verification design

The test of §3.1 decision 3 is written first and fails on today's code. That failure is the red
step. Gates 1 and 2 run in Task 3. `dsh-data`'s coverage does not move, because the enum's branches
are already covered by `testTransition`, and the change adds no branch.

| AC | Check | Where |
|---|---|---|
| AC001 | `getStatusDescription_forEveryStatus_shouldEqualItsName` | Task 1 |
| AC002 | `getStatusMessage_ofNotIndexedError_shouldSayThatIndexingFailed`, and the uniqueness test | Task 1 |
| AC003 | The tests loop over `values()`; each reintroduced copy fails them | Task 1 Steps 2 and 5 |
| AC004 | `grep -n NOT_INDEXED_ERROR specs/api/openapi/dsh-rest-api.yaml` finds nothing | Task 2, Task 3 |
| AC005 | The grep of Task 3 Step 1 finds the exception nowhere it reads as current | Task 2, Task 3 |
| AC006 | `mvn -B clean install` exits 0 | Task 3 |

## 5. Files to change

| File | Change |
|---|---|
| `dsh-data/src/main/java/com/mriss/dsh/data/models/DocumentStatus.java` | `NOT_INDEXED_ERROR`'s two constructor arguments |
| `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusTest.java` | Two sampling tests replaced by three (§3.1 decision 3) |
| `specs/api/openapi/dsh-rest-api.yaml` | `DocumentStatusDto.status`'s description |
| `specs/features/rest-api-endpoints.md` | Today paragraph, Planned bullet |
| `specs/architecture/ADR-003-document-pipeline.md` | Three dated notes |
| `specs/stories/171-not-indexed-error-describes-itself.md` | This spec, §8 filled in |

Nothing else. `docs/api/index.html` is regenerated by CI after the merge (§2.2).

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** `NOT_INDEXED_ERROR` answers its own name and a message saying that indexing failed, a test
holds every status to that, and the documents drop the exception.

**Architecture:** one enum constant in `dsh-data`, guarded by a unit test that loops over
`DocumentStatus.values()`. Three documents are edited by hand. The rendered API page follows from
CI.

**Tech stack:** Java 17, JUnit 4, Maven, markdownlint.

**Spec:** this file.

### Global constraints

- JUnit 4 only, as in `dsh-data`. Test names follow
  `.github/copilot/rules/testing-patterns.md`, "Naming Conventions".
- `DocumentStatusTest.java` indents with four spaces. `DocumentStatus.java` indents with tabs.
  Keep each file's style.
- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs").
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- The new message is exactly `Error indexing document.`.
- Commit messages are `test(#171): …`, `fix(#171): …` and `docs(#171): …`, and end with the
  session's attribution lines.

### Task 1: The test, red, then the fix

**Files:**

- Test: `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusTest.java`
- Modify: `dsh-data/src/main/java/com/mriss/dsh/data/models/DocumentStatus.java:58`

**Interfaces:** consumes `DocumentStatus.values()`, `name()`, `getStatusDescription()` and
`getStatusMessage()`, all existing. Produces nothing new.

- [ ] **Step 1: Write the failing tests.** In `DocumentStatusTest`, delete `testGetStatusDescription`
  and `testGetStatusMessage` and add:

  ```java
      @Test
      public void getStatusDescription_forEveryStatus_shouldEqualItsName() {
          for (DocumentStatus status : DocumentStatus.values()) {
              assertEquals(status.name(), status.getStatusDescription());
          }
      }

      @Test
      public void getStatusMessage_forEveryStatus_shouldBeItsOwn() {
          Map<String, DocumentStatus> owners = new HashMap<>();
          for (DocumentStatus status : DocumentStatus.values()) {
              String message = status.getStatusMessage();
              assertNotNull(status + " has no message", message);
              assertFalse(status + " has a blank message", message.trim().isEmpty());
              DocumentStatus owner = owners.putIfAbsent(message, status);
              assertNull(status + " shares its message with " + owner, owner);
          }
      }

      @Test
      public void getStatusMessage_ofNotIndexedError_shouldSayThatIndexingFailed() {
          assertEquals("Error indexing document.", DocumentStatus.NOT_INDEXED_ERROR.getStatusMessage());
      }
  ```

  Add the imports `java.util.HashMap`, `java.util.Map`, `static org.junit.Assert.assertFalse` and
  `static org.junit.Assert.assertNull`. Keep `assertNotNull`, which the new test uses.

- [ ] **Step 2: Run it and watch it fail.**

  ```bash
  mkdir -p .logs
  mvn -B -pl dsh-data test -Dtest=DocumentStatusTest > .logs/mvn-test-documentstatus.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-test-documentstatus.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=1`, 3 failures among 4 tests:
  - `expected:<NOT_INDEXED_ERROR> but was:<DEQUEUED_FOR_INDEXING>`;
  - `NOT_INDEXED_ERROR shares its message with DEQUEUED_FOR_INDEXING`;
  - `expected:<Error indexing document.> but was:<Document dequeued for indexing.>`.

  Record them in §8.1.

- [ ] **Step 3: Commit the red test.**

  ```bash
  git add dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusTest.java
  git commit -m "test(#171): hold every DocumentStatus to its own description and message"
  ```

- [ ] **Step 4: Fix the constant.** In `DocumentStatus.java`, line 58, indented with one tab as the
  rest of the file is:

  ```java
      NOT_INDEXED_ERROR("NOT_INDEXED_ERROR", "Error indexing document.") {
  ```

  Run Step 2's command again. Expected: `maven exit=0`, 4 tests, 0 failures.

- [ ] **Step 5: Show that each copy is caught (AC003).**
  1. Put back only the description, as `NOT_INDEXED_ERROR("DEQUEUED_FOR_INDEXING", "Error indexing document.")`.
     Run Step 2's command, logging to `.logs/mvn-test-documentstatus-mutant-description.log`.
     Expected: `maven exit=1`, failing in `getStatusDescription_forEveryStatus_shouldEqualItsName`.
  2. Put back only the message, as `NOT_INDEXED_ERROR("NOT_INDEXED_ERROR", "Document dequeued for indexing.")`.
     Run it again, logging to `.logs/mvn-test-documentstatus-mutant-message.log`. Expected:
     `maven exit=1`, failing in `getStatusMessage_forEveryStatus_shouldBeItsOwn` with
     `NOT_INDEXED_ERROR shares its message with DEQUEUED_FOR_INDEXING`. The wording test fails too.
  3. Restore Step 4's line, and check that `git diff` on `DocumentStatus.java` shows only Step 4's
     change. Record the three results in §8.1.

- [ ] **Step 6: Commit the fix.**

  ```bash
  git add dsh-data/src/main/java/com/mriss/dsh/data/models/DocumentStatus.java
  git commit -m "fix(#171): NOT_INDEXED_ERROR answers its own description and message"
  ```

### Task 2: The documentation

**Files:**

- Modify: `specs/api/openapi/dsh-rest-api.yaml:219-223`
- Modify: `specs/features/rest-api-endpoints.md:68-70` and `:128`
- Modify: `specs/architecture/ADR-003-document-pipeline.md:48`, `:748`, `:942`

**Interfaces:** none.

- [ ] **Step 1: The OpenAPI file.** Replace `DocumentStatusDto.status.description` with §3.2's text.
- [ ] **Step 2: `rest-api-endpoints.md`.** Apply §3.2: the Today paragraph's first two sentences,
  and the deleted Planned bullet.
- [ ] **Step 3: ADR-003.** Add the three dated notes of §3.2, with today's date.
- [ ] **Step 4: Lint.**

  ```bash
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json
  ```

  This is the command in `CLAUDE.md`. `markdownlint` is installed globally in that Node directory.
  Expected: exit 0.

- [ ] **Step 5: Commit.**

  ```bash
  git add specs/api/openapi/dsh-rest-api.yaml specs/features/rest-api-endpoints.md specs/architecture/ADR-003-document-pipeline.md
  git commit -m "docs(#171): drop the NOT_INDEXED_ERROR exception from the API documents"
  ```

### Task 3: The gates and the acceptance checks

- [ ] **Step 1: AC004 and AC005.**

  ```bash
  grep -rn "NOT_INDEXED_ERROR" specs docs --include=*.md --include=*.yaml | grep -v '^specs/stories/' | grep -v 'docs/wiki/'
  ```

  Expected matches, and nothing else:
  - `document-status-workflow.md`, the two diagram edges;
  - ADR-003's transition table row (line 181), and its three passages at lines 48, 748 and 942.
    Read each passage to its end: each must carry ``Fixed by `#171` on <date>``. Candidate 5's note
    is on the line after the match;
  - the PRD's lines on task 12: decision 6 and its index entry, the order-of-work sentence, and
    the task itself. They are step 8's (§3.1 decision 6).

  `dsh-rest-api.yaml` and `rest-api-endpoints.md` must not appear. `docs/api/index.html` is not
  matched by the `--include` filter. It still holds the old text until CI regenerates it (§2.2).
  Record the output in §8.2.

- [ ] **Step 2: Gates 1 and 2.**

  ```bash
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`. Record `dsh-data`'s LINE and BRANCH coverage from
  `dsh-data/target/site/jacoco/index.html` in §8.3.

- [ ] **Step 3: Fill in §8 and commit.**

  ```bash
  git add specs/stories/171-not-indexed-error-describes-itself.md
  git commit -m "docs(#171): record the verification results"
  ```

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: `NOT_INDEXED_ERROR.getStatusDescription()` is `NOT_INDEXED_ERROR` | §3.1 decision 1, Task 1 Step 4 |
| AC002: the message says indexing failed, and differs from `DEQUEUED_FOR_INDEXING`'s | §3.1 decisions 1 and 3, Task 1 Steps 1 and 4 |
| AC003: a test over every value; either copy fails it | §3.1 decisions 3 and 4, Task 1 Steps 1, 2 and 5 |
| AC004: the OpenAPI file has no exception | §3.2, Task 2 Step 1, Task 3 Step 1 |
| AC005: the feature spec drops both mentions; nothing else describes the exception as current | §3.1 decisions 5 and 6, §3.2, Task 2 Steps 2 and 3, Task 3 Step 1 |
| AC006: `mvn -B clean install` passes at the 95% gate | Task 3 Step 2 |

## 8. Verification results

Filled in by Task 1 and Task 3.

### 8.1 Task 1: red, green, and the two reintroduced copies

Run on 2026-10-16 with `mvn -B -pl dsh-data test -Dtest=DocumentStatusTest`.

| Run | `DocumentStatus.java:58` | Exit | Result |
|---|---|---|---|
| Red, Step 2 | `("DEQUEUED_FOR_INDEXING", "Document dequeued for indexing.")` | 1 | 4 tests, 3 failures |
| Green, Step 4 | `("NOT_INDEXED_ERROR", "Error indexing document.")` | 0 | 4 tests, 0 failures |
| Description copied back, Step 5.1 | `("DEQUEUED_FOR_INDEXING", "Error indexing document.")` | 1 | 4 tests, 1 failure |
| Message copied back, Step 5.2 | `("NOT_INDEXED_ERROR", "Document dequeued for indexing.")` | 1 | 4 tests, 2 failures |

The red run failed as predicted:

```text
getStatusDescription_forEveryStatus_shouldEqualItsName:20 expected:<[NOT_INDEXED_ERROR]> but was:<[DEQUEUED_FOR_INDEXING]>
getStatusMessage_forEveryStatus_shouldBeItsOwn:32 NOT_INDEXED_ERROR shares its message with DEQUEUED_FOR_INDEXING expected null, but was:<DEQUEUED_FOR_INDEXING>
getStatusMessage_ofNotIndexedError_shouldSayThatIndexingFailed:38 expected:<[Error indexing document].> but was:<[Document dequeued for indexing].>
```

With the description copied back, only `getStatusDescription_forEveryStatus_shouldEqualItsName`
failed, with the first line above. With the message copied back,
`getStatusMessage_forEveryStatus_shouldBeItsOwn` and
`getStatusMessage_ofNotIndexedError_shouldSayThatIndexingFailed` failed, with the second and third
lines above. After Step 5.3, `git diff` on `DocumentStatus.java` showed only Step 4's line.

### 8.2 Task 3 Step 1: the grep

```text
specs/architecture/ADR-003-document-pipeline.md:48:| `NOT_INDEXED_ERROR` carries the description and message of `DEQUEUED_FOR_INDEXING`. Fixed by `#171` on 2026-10-16 | `DocumentStatus.java`, the constant's constructor arguments |
specs/architecture/ADR-003-document-pipeline.md:181:| 5 | indexing ends | `SUCCESS` or `ERROR` | `INDEXING` | `INDEXED_SUCCESS` or `NOT_INDEXED_ERROR` |
specs/architecture/ADR-003-document-pipeline.md:748:`NOT_INDEXED_ERROR` carries the description and message of another status. Fixed by `#171` on
specs/architecture/ADR-003-document-pipeline.md:943:5. **`NOT_INDEXED_ERROR`'s description and message are those of `DEQUEUED_FOR_INDEXING`.** A small
specs/architecture/document-status-workflow.md:23:    INDEXING --> NOT_INDEXED_ERROR : ERROR
specs/architecture/document-status-workflow.md:25:    NOT_INDEXED_ERROR --> [*]
specs/product/PRD.md:790:6. **`NOT_INDEXED_ERROR`'s wrong description gets an issue of its own**, Wave 1 task 12, so that
specs/product/PRD.md:830:5. `NOT_INDEXED_ERROR`: Wave 1 task 12 (decision 6).
specs/product/PRD.md:1045:thing that can leave a document in `NOT_INDEXED_ERROR`.
specs/product/PRD.md:1307:12. **Every status describes itself.** Added on 2026-10-13. `NOT_INDEXED_ERROR` is declared with
```

These are the expected matches and nothing else. `dsh-rest-api.yaml` and `rest-api-endpoints.md`
do not appear. Candidate 5 is at line 943, not 942, because the note at line 748 wraps onto a
second line. Each of ADR-003's three passages carries ``Fixed by `#171` on 2026-10-16``: line 748's
note ends on line 749, and candidate 5's on line 944.

### 8.3 Task 3 Step 2: the gates

`mvn -B clean install` on 2026-10-16: `maven exit=0`, `BUILD SUCCESS` across the 13-module reactor
in 1:34 min. Every `jacoco:check` reported "All coverage checks have been met." `DocumentStatusTest`
ran 4 tests with 0 failures.

`dsh-data/target/site/jacoco/index.html`, which Step 2 names, is not produced: `install` writes
`dsh-data/target/jacoco.exec` and runs `jacoco:check`, but no per-module report. The figures below
come from the aggregate report of the same build,
`dsh-coverage-report/target/site/jacoco-aggregate/dsh-data/index.html`, which reads that
`jacoco.exec`.

| `dsh-data` | Covered | Missed | Coverage |
|---|---|---|---|
| LINE | 217 of 224 | 7 | 96.9% |
| BRANCH | 81 of 82 | 1 | 98.8% |

`DocumentStatus` is fully covered. The enum's own class has no branches, and its 18 constant bodies,
which JaCoCo reports as separate anonymous classes, are at 100% of their lines and, for the five with
an `if`, 100% of their branches. That bears out §4: the branches are `transition`'s, and the change
adds none.

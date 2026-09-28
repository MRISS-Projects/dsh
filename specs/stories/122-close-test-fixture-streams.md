---
issue: 122
slug: close-test-fixture-streams
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 122 — Close the file streams that test fixtures leave open

## 1. Story

**As a** developer running DSH's tests on Windows
**I want** every test fixture to close the files it opens
**So that** file handles stop accumulating across a test run, and a locked PDF never fails a build
for reasons unrelated to the test

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#122](https://github.com/MRISS-Projects/dsh/issues/122)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`. It matches the milestone, and `#123` and `#124` were
  cut from and merged into the same RC.
- Spun off from `#112`'s local review (point 6). `#112`'s context-free `DocumentTest` rebuilds its
  fixtures before every test, so the leak now repeats per test instead of per Spring context.

### 2.1 Who owns the stream

None of the three consumers the tests hand a stream to closes it, and none should. Closing belongs
to whoever opened the stream:

| Consumer | What it does with the stream |
|---|---|
| `Document(InputStream, String)` (`dsh-data`, `Document.java:75`) | `IOUtils.toByteArray` — reads it fully, does not close it |
| `DocumentSubmissionServiceImpl.getTokenFromDocument` (`dsh-rest-api`) | passes it to `Document(InputStream, String)` |
| `MockMultipartFile(String, String, String, InputStream)` (Spring test) | copies it fully into a `byte[]`, does not close it |

So the defect is entirely in test code, and AC002 holds with no production change.

### 2.2 The count is 20, not 18

The issue counts 18 sites, which is what `grep "new FileInputStream"` finds.
`DocumentSubmissionServiceImplTest:158,162` spell it `new java.io.FileInputStream(...)` and are
missed by that pattern. The search this spec uses throughout is:

    grep -rnE "new (java\.io\.)?FileInputStream\(" --include=*.java dsh-*/src/test dsh-*/*/src/test

It finds 20 sites at `d93916034`, across the six files the issue names.

## 3. Design

Each call shape gets one rule.

### 3.1 Rule A — the bytes are wanted: read the file, open no stream (4 sites)

`IOUtils.toByteArray(new FileInputStream(new File(p)))` becomes `FileUtils.readFileToByteArray(new File(p))`.
Commons IO is already on both modules' test classpath: `DocumentTest` imports `FileUtils`, and all
three files import `IOUtils`.

| File | Line |
|---|---|
| `dsh-data/.../models/DocumentTest.java` | 54, 59 |
| `dsh-data/.../document/dao/mongo/MongoDocumentDaoTest.java` | 54 |
| `dsh-rest-api/.../service/DocumentHandlingServiceImplTest.java` | 52 |

### 3.2 Rule B — `MockMultipartFile`: use the `byte[]` constructor (3 sites)

`new MockMultipartFile(name, original, type, new FileInputStream(f))` becomes
`new MockMultipartFile(name, original, type, FileUtils.readFileToByteArray(f))`. The upload body is
the same bytes, and none of these tests exercises `MockMultipartFile`'s stream path.

| File | Line |
|---|---|
| `dsh-rest-api/.../integration/DocumentResourceIT.java` | 133, 170, 202 |

### 3.3 Rule C — the stream path is under test: keep the stream, own it with try-with-resources (13 sites)

Where a stream reaches `Document(InputStream, String)` or `getTokenFromDocument`, keep the stream.
This keeps `Document`'s stream constructor and the service's stream parameter exercised exactly as
today, which is what keeps coverage unchanged (AC003). The stream is opened in a try-with-resources
header and lives only for that one call.

Where a test class opens the same shape more than once, the try-with-resources goes into one private
helper, not into each test:

| File | Lines | Shape |
|---|---|---|
| `dsh-data/.../models/DocumentTest.java` | 63, 67 | inside the existing `documentWithTitleAndContentsFromStream()` and `anotherDocument()` helpers |
| `dsh-rest-api/.../service/DocumentSubmissionServiceImplTest.java` | 73, 85, 96, 101, 129, 158, 162, 180 | two new helpers, below |
| `dsh-rest-api/.../service/DocumentEnqueueResponseMessageHandlerTest.java` | 47 | inline in `setUp()` |
| `dsh-rest-api/.../integration/DocumentResourceIT.java` | 158, 218 | one new helper, `documentFrom(File, String)` |

`DocumentSubmissionServiceImplTest` gets:

```java
private String tokenFor(String path, String title, boolean useCache) throws Exception {
    try (InputStream is = new FileInputStream(new File(path))) {
        return service.getTokenFromDocument(is, title, useCache);
    }
}

private static Document documentFrom(String path, String title) throws Exception {
    try (InputStream is = new FileInputStream(new File(path))) {
        return new Document(is, title);
    }
}
```

Its eight sites become calls to these. The two cache-hit tests use `documentFrom` for the cached
document and `tokenFor` for the submission, so each stream is still a separate stream, as today.
`tokenFor` returns the token. Tests that discard it today keep discarding it.

The rewrite drops the fully-qualified `java.io.FileInputStream`, `java.io.File` and
`com.mriss.dsh.data.models.Document` in `testStoreDocumentAndQueueForProcessingCacheHitSkipsStorage`,
since those lines are replaced anyway. The `org.mockito.Mockito.never()` and `times()` qualifiers
elsewhere in that file are left alone. They are not on lines this story touches.

### 3.4 Imports

Every file drops the imports its rewrite leaves unused. Rule A removes the only uses of `IOUtils`
in all three of its files, so `IOUtils` goes from all three, and `FileInputStream` goes from
`MongoDocumentDaoTest` and `DocumentHandlingServiceImplTest`. `FileUtils` is added to
`MongoDocumentDaoTest`, `DocumentHandlingServiceImplTest` and `DocumentResourceIT`; `DocumentTest`
already has it. `InputStream` is added where Rule C introduces it and it is not already imported.

### 3.5 The invariant after the change

Every remaining `new FileInputStream(` is in a try-with-resources header, and each header opens
exactly one resource. So each match is on a line that starts with `try (`:

    grep -rnE "new (java\.io\.)?FileInputStream\(" --include=*.java dsh-*/src/test dsh-*/*/src/test \
      | grep -vE ":[0-9]+:\s*try \(InputStream [a-z0-9]+ = new FileInputStream\("

This prints nothing. The first grep alone finds 6 sites: 2 in `DocumentTest`, 2 in
`DocumentSubmissionServiceImplTest`, 1 in `DocumentEnqueueResponseMessageHandlerTest` and 1 in
`DocumentResourceIT`.

### 3.6 Out of scope

- **A CI guard against unclosed streams.** A new check script for a one-off cleanup. The §3.5 grep
  proves this story. It is not wired into CI.
- **Production code.** `Document`, `DocumentSubmissionServiceImpl` and every other `src/main` file
  are unchanged (AC002).
- **Other tidying.** Fixture paths repeated as string literals, the remaining FQN qualifiers, the
  `synchronized (this) { this.wait(...) }` waits in `DocumentResourceIT` and every other style point
  in these files stay as they are.

## 4. Files to change

| File | Rules |
|---|---|
| `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentTest.java` | A (2), C (2) |
| `dsh-data/src/test/java/com/mriss/dsh/data/document/dao/mongo/MongoDocumentDaoTest.java` | A (1) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/service/DocumentHandlingServiceImplTest.java` | A (1) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/service/DocumentSubmissionServiceImplTest.java` | C (8), two helpers |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/service/DocumentEnqueueResponseMessageHandlerTest.java` | C (1) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceIT.java` | B (3), C (2), one helper |

No file under `src/main` changes.

## 5. Tasks

Every Maven run follows `CLAUDE.md`'s "Always log local Maven runs": redirect to `.logs/`, print
the `tail -f` command, `wait`, and report the exit code.

This is a test-code refactor that must not change behaviour, so there is no new red test. The
failing check is the §3.5 invariant: **Task 1** records it failing on the RC, and later tasks turn
it green without changing any other measurement.

- [x] **Task 1 — baseline on the RC plus this spec.** Before any edit:
      1. Run the §2.2 grep. Expected: 20 lines. Run the §3.5 pipeline. Expected: 20 lines. Record
         both in §7.
      2. `mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1`. Expected: exit 0.
         Record in §7 the `Tests run:` summary for each module, from
         `grep -E "Tests run: [0-9]+, Failures" .logs/mvn-clean-install-baseline.log` (the last
         summary line per module).
      3. Record coverage: copy `dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv` to
         `.logs/jacoco-aggregate-baseline.csv`. If that file does not exist, stop and report. The
         `dsh-coverage-report` module is what `#104` reads the CSV from, and a missing file means
         the comparison in Task 4 needs a different source.
      4. `mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it-baseline.log 2>&1`.
         Expected: exit 0. Record `DocumentResourceIT`'s `Tests run:` line.
      5. Commit nothing.
- [x] **Task 2 — `dsh-data`.** Apply Rules A and C to `DocumentTest` and Rule A to
      `MongoDocumentDaoTest`, with §3.4's imports.
      `mvn -B -pl dsh-data -am clean install > .logs/mvn-clean-install-dsh-data.log 2>&1`.
      Expected: exit 0, the same `dsh-data` test count as Task 1, and `All coverage checks have been met.`
      Commit: `test(#122): close the fixture streams in dsh-data's tests`.
- [x] **Task 3 — `dsh-rest-api`.** Apply §3 to the four `dsh-rest-api` files, with §3.4's imports.
      `mvn -B -pl dsh-rest-api -am clean install -DintegrationTests > .logs/mvn-clean-install-rest-api-it.log 2>&1`.
      Expected: exit 0, the same `dsh-rest-api` unit and IT counts as Task 1, and
      `All coverage checks have been met.` Commit:
      `test(#122): close the fixture streams in dsh-rest-api's tests`.
- [x] **Task 4 — verify on the full reactor.** Run §6 in order, record every result in §7, tick the
      ACs in §8, commit: `docs(#122): record the verification`.

## 6. Verification

Run one after another, each logging to its own file in `.logs/`.

1. **The invariant.** The §3.5 pipeline prints nothing. The §2.2 grep prints 6 lines.
2. **AC002, no production change.**
   `git diff --name-only staging-0.3.0-SNAPSHOT-RC...HEAD -- '*/src/main/*' '*/*/src/main/*'`
   prints nothing, and every path in `git diff --name-only staging-0.3.0-SNAPSHOT-RC...HEAD` is
   under `src/test/` or is this spec.
3. **AC003, unit build.** `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`. Expected:
   - exit 0;
   - each module's last `Tests run:` summary equals Task 1's;
   - `grep -c "All coverage checks have been met."` equals Task 1's count, and `grep -c "Rule violated"` is `0`;
   - `diff .logs/jacoco-aggregate-baseline.csv dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv`
     prints nothing. Production classes are unchanged, and the tests take the same paths through
     them, so every covered and missed count is identical.
4. **AC003, integration build.**
   `mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it.log 2>&1`. Expected: exit 0,
   and `DocumentResourceIT`'s `Tests run:` line equals Task 1's.

## 7. Build record

Filled in by `dsh-build-story` on 2026-09-28. Every Maven run below exited 0.

### 7.1 Baseline — Task 1, at `a0d17a462`

| Measurement | Result |
|---|---|
| §2.2 grep | 20 lines |
| §3.5 pipeline | 20 lines |
| `mvn -B clean install` | exit 0; `All coverage checks have been met.` ×8; `Rule violated` ×0 |
| Coverage | `jacoco.csv` present, copied to `.logs/jacoco-aggregate-baseline.csv` |
| `mvn -B clean install -DintegrationTests` | exit 0; `DocumentResourceIT`: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` |

Last `Tests run:` summary per module, unit build:

| Module | Tests run |
|---|---|
| `dsh-data` | 51 |
| `dsh-rest-api` | 36 |
| `solr-terms-vector-order` | 22 |
| `solr-advanced-numbers-filter` | 10 |
| `dsh-doc-indexer-worker` | 2 |
| `dsh-keyword-extractor` | 2 |
| `dsh-top-sentences-extractor` | 2 |
| `dsh-doc-processor-worker` | 2 |

All with 0 failures, 0 errors, 0 skipped.

### 7.2 Per-module builds

| Task | Commit | Command | Result |
|---|---|---|---|
| 2 | `d3247a981` | `mvn -B -pl dsh-data -am clean install` | exit 0; `dsh-data` 51 tests; coverage met |
| 3 | `9dcacb412` | `mvn -B -pl dsh-rest-api -am clean install -DintegrationTests` | exit 0; `dsh-rest-api` 36 unit, 8 IT (`DocumentResourceIT` 6); coverage met |

### 7.3 Verification — §6, at `9dcacb412`

| Run | Result |
|---|---|
| 1 | §3.5 pipeline prints nothing. §2.2 grep prints 6 lines. |
| 2 | No path under `src/main`. The branch diff is the six §4 test files plus this spec. |
| 3 | exit 0. Every `Tests run:` summary identical to the baseline (`diff` empty). `All coverage checks have been met.` ×8, `Rule violated` ×0. `diff` of `jacoco.csv` against the baseline prints nothing. |
| 4 | exit 0. `DocumentResourceIT`: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`. Every `Tests run:` summary identical to the IT baseline. |

## 8. Acceptance criteria

- [x] **AC001** — no test opens a `FileInputStream` without closing it. Byte arrays come from
      `FileUtils.readFileToByteArray`, and the stream constructor is called inside
      try-with-resources. *§3, §6 run 1.*
- [x] **AC002** — no production code changes. *§6 run 2.*
- [x] **AC003** — `mvn -B clean install` is green, with test counts and coverage unchanged. The AC
      says `mvn -B install`. `clean` is added because `CLAUDE.md` requires it for any result about the
      gate. *§6 runs 3 and 4.*

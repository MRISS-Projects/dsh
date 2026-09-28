---
issue: 124
slug: keep-test-fixtures-out-of-production-artifacts
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 124 — Keep `dsh-test-dataset` fixtures and test classes out of production artifacts

## 1. Story

**As a** DSH maintainer building locally or cutting a release
**I want** the test fixtures from `dsh-test-dataset` to reach the test classpath only
**So that** production artifacts carry no test fixtures or test classes, and an `install` without
`clean` passes the coverage gate like a clean one

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#124](https://github.com/MRISS-Projects/dsh/issues/124)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`. It matches the milestone, and `#104`, which shipped
  this story's configuration change, merged there.

### 2.1 The configuration change has already shipped

The issue's mechanism is not repeated here. `attachToMain` defaulted to `true`, which registered
`target/test-classes` as a main resource directory, so `resources:resources` copied the fixture
PDFs, and on any build after the first every compiled test class, into `target/classes`.

PR #131 (`#104`) fixed that in commit `2fa1f70ba` by setting `<attachToMain>false</attachToMain>` in
all four modules. `#104` needed it because its coverage badge could not match the aggregate while
test classes leaked into it. `#104`'s spec never mentioned `#124`, and the overlap was first noticed
in the PRD reconciliation `57c1922c9`, which kept `#124` open for the part `#131` did not do: the
investigation of whether anything relied on the main-side attachment.

This story therefore:

1. records that investigation (AC001, §3);
2. removes the unpack from the two modules that never use it (§4.1);
3. documents why `attachToMain` must stay `false` in the two that do (§4.2);
4. verifies AC002–AC004 against the result on a fresh build (§6);
5. notes in the PRD what the dataset lacks for the ADR-001 waves that will use it (§4.3).

## 3. Investigation — does anything depend on the main-side attachment?

Carried out on 2026-09-27/28 at `48c31d354`. The dataset is four PDFs in
`dsh-test-dataset/src/test/resources/pdf/`: `The-Categories.pdf`, `bbc-news-1.pdf`, `edition.cnn.com-1.pdf`,
`edition.cnn.com-2.pdf`. **There are no HTML fixtures**, despite `#12`'s title and `README.md`.

### 3.1 Per module

| Module | Reads the fixtures? | How | Anything on the main side? |
|---|---|---|---|
| `dsh-data` | yes, tests only | `MongoDocumentDaoTest:54`, `DocumentTest:34,67,173` open `target/test-classes/pdf/...` **by file path** | no |
| `dsh-rest-api` | yes, tests only | `DocumentEnqueueResponseMessageHandlerTest`, `DocumentHandlingServiceImplTest`, `DocumentSubmissionServiceImplTest` and `integration/DocumentResourceIT` open `target/test-classes/pdf/...` **by file path** | no |
| `solr-advanced-numbers-filter` | **no** | no source file, test or main, names a fixture; its only test resource is `log4j2.properties` | no |
| `solr-terms-vector-order` | **no** | no source file names a fixture; its test resources are `log4j2.properties` and `test-ordering.json` | no |

Evidence: `grep -rnI -E "The-Categories|bbc-news-1|edition\.cnn\.com-[12]|\"/?pdf/|classpath:pdf|pdf/\"|getResource"`
over the repository, excluding `target/`, `.git/` and `.logs/`. Every match is under `src/test/java`
in `dsh-data` or `dsh-rest-api`, and every one is a filesystem path. **No test loads a fixture from
the classpath**, main or test. The tests need the files in `target/test-classes/pdf/`, which the
`process` goal's `outputDirectory` provides whatever `attachToMain` is.

### 3.2 The places the issue asked to check

| Place | Finding |
|---|---|
| Main code and resources | The only classpath resource loads in `src/main` are `dshApplicationContext.xml`, `mongo.properties` and `enqueue-docId-context.xml`. No fixture. |
| `spring-boot:run` | Runs the main classpath. Nothing in it references a fixture (row above). |
| The ITs under failsafe | `DocumentResourceIT` reads `target/test-classes/pdf/...` by path and uploads it as a multipart body. It does not load it from the classpath. |
| `api-testing.yml` | Builds with `-DskipTests`, boots `dsh-rest-api/target/*.jar`, then runs Newman over `specs/api/postman/*.json`. That directory holds only `README.md`, so the step skips and nothing uploads a fixture. |
| `README.md` demo flow | The Swagger "submit" flow has the user choose a file to upload. It names no fixture and loads nothing from the jar. |
| The Solr plugins in Solr | Neither plugin's `src/main` loads any resource (§3.1). Solr would get nothing from the PDFs in their jars. |
| Site and PDF generation | The `deployment` and `product-release-deployment` profiles run `maven-pdf-plugin` with `siteDirectory=${project.build.directory}/generated-site`. Nothing in site generation reads `target/classes`. |
| Downstream consumers | `dsh-doc-indexer-worker` depends on `dsh-data` and has no source naming a fixture. `dsh-coverage-report` depends on all four for `report-aggregate`, which analyses classes. Not reading the PDFs, and it is what `#104` needed rid of the stray test classes. Nothing depends on the two Solr jars. |
| Classpath route | None of the four modules declares `dsh-test-dataset` as a dependency. The bundle is resolved by the plugin, not put on any classpath, so the PDFs in `dsh-test-dataset`'s own jar cannot reach `dsh-rest-api`'s `BOOT-INF/lib` either. |

### 3.3 History — the original intent was *not* to attach

The issue says `attachToMain` "has never been set explicitly", so attaching "has always been the
default rather than a choice". The history goes further. `911bd34e1` (`#12`, 2017-12-07) wrote
`<attached>false</attached>` on the execution, and the Solr modules copied it in `fe784e091`
(2019-12-27). **The intent was to keep the bundle off the main build.** Plugin 3.x has no `attached`
parameter, and Maven ignores an unknown parameter silently, so the setting stopped doing anything
without a word. `1edf653b3` (2026-04-21, a POM clean-up) then deleted it from `dsh-rest-api`.
`2fa1f70ba` restored the intent under the parameter that exists.

### 3.4 Conclusion

**No consumer of the main-side attachment exists, in any of the four modules.** `attachToMain=false`
stays, and nothing needs moving to the test classpath, because every reader is already there.

A local indication, not the proof: the jars built at 19:46 on 2026-09-27 from the branch carrying
`2fa1f70ba` list no `.pdf`, `*Test.class`, `*IT.class` or `*ITConfiguration.class`. §6 proves it on
a fresh build.

## 4. Design

### 4.1 Remove the unpack from the two Solr modules

Remove the whole `maven-remote-resources-plugin` block from:

- `dsh-solr/solr-advanced-numbers-filter/pom.xml:62-81`
- `dsh-solr/solr-terms-vector-order/pom.xml:62-81`

It is the only plugin in each, so the now-empty `<plugins>` element (lines 61 and 82) goes too.
`<build>` keeps its `<resources>`.

**Why here and only here.** The fixtures live in `dsh-test-dataset`, which this story does not touch.
A module opts in with its own `process` execution, so removing one deletes no fixture, and a future
module that needs them adds the execution back (§4.2 is the pattern). The rule for removing it is:
the module does not use the fixtures, **and** ADR-001 replaces the module. Both Solr modules meet
both conditions. ADR-001 Phase 4 replaces Solr with Vertex AI Search, and neither module reads a
fixture (§3.1).

ADR-001 deprecates the Solr code and does not remove it, so the modules stay in the reactor for now.
`Order`, `OrderOptions` and `TermsVectorComparator` are to be reused as they are (ADR-001 §4, Phase 4
item d). Neither fact involves the PDFs.

`dsh-data` and `dsh-rest-api` both survive the migration. ADR-001 gives them Firestore, GCS and
Pub/Sub implementations next to the deprecated Mongo and RabbitMQ ones. They are also the modules
whose tests read the fixtures, so they keep the unpack.

### 4.2 Say why `attachToMain` is `false` in the two that keep it

`dsh-data/pom.xml:116-117` and `dsh-rest-api/pom.xml:174-175`: add a comment above
`<attachToMain>`, so it cannot rot unnoticed as `<attached>` did:

```xml
<outputDirectory>${project.build.directory}/test-classes</outputDirectory>
<!-- #124: must stay false. The default, true, makes target/test-classes a main
     resource root, and the fixture PDFs and every compiled test class then
     reach target/classes and the jar. Tests read the fixtures by path. -->
<attachToMain>false</attachToMain>
```

The snippet shows the lines unindented. In the POMs, indent them with tabs to match the
surrounding lines.

### 4.3 PRD: what the dataset lacks for the ADR-001 waves

The same four PDFs remain the fixtures after the migration. They are input documents, independent
of the backend that stores or indexes them. The first consumers will be `GcsFileStorageService`
(Wave 2 task 4, `dsh-data`), `PubSubDocumentQueueServiceImpl` (Wave 3 task 3, `dsh-rest-api`) and
`VertexAiSearchIndexingService` (Wave 4 task 2, a new module that adds the §4.2 execution).

The dataset has three gaps those waves will hit. Recording them is this story's job. Filling them is
theirs, so no issue is opened now. Add to `specs/product/PRD.md`, directly under the Wave 2 heading
paragraph (after line 640), this paragraph:

```markdown
**Test fixtures for Waves 2–4.** `dsh-test-dataset` holds four PDFs and nothing else (`#124` §3).
A module unpacks them with a `maven-remote-resources-plugin:process` execution into
`target/test-classes`, with `attachToMain=false` — copy the block from `dsh-data/pom.xml`. The
dataset has three gaps these waves will meet, each for the story that first needs it:

1. **No HTML fixtures**, although `#12` and `README.md` promise PDF and HTML.
2. **No expected outputs.** No reference keywords or top sentences exist to assert against, which
   Wave 4 needs to compare Vertex AI Search's results with Solr's.
3. **Sizes against the 1 MB Firestore limit.** ADR-001 moves file bytes to GCS because Firestore
   caps a document at 1 MB. <SIZE-FINDING>
```

`<SIZE-FINDING>` is filled at build time by Task 3 step 1, from a measurement. It is not a
placeholder left for later.

### 4.4 Explicitly not in scope

- **`dsh-test-dataset`'s own jar carries the PDFs** under `pdf/`, because its POM lists
  `src/test/resources` as a main resource. That jar *is* the fixture artifact, not a production
  one. The `bundle` goal reads `src/test/resources` directly, so that resource entry may be
  redundant, but changing how the dataset is built is not this story.
- **Widening `jacoco:check`'s excludes, moving the configuration to parent-poms, and `#122`** — as
  the issue's Out of Scope says.
- **Any regression guard in CI.** The §4.2 comment is the proportionate guard for a one-line
  setting. A jar-content check in CI would be a new mechanism, and nothing asks for one.
- **The PRD's Wave 0 row and paragraph on `#124`** (lines 98 and 236–240). Step 8,
  `dsh-reconcile-prd`, updates them after merge.

## 5. Files to change

| File | Change |
|---|---|
| `dsh-solr/solr-advanced-numbers-filter/pom.xml` | §4.1: delete lines 61–82, `<plugins>` through `</plugins>` |
| `dsh-solr/solr-terms-vector-order/pom.xml` | §4.1: delete lines 61–82 |
| `dsh-data/pom.xml` | §4.2: comment above line 117 |
| `dsh-rest-api/pom.xml` | §4.2: comment above line 175 |
| `specs/product/PRD.md` | §4.3: paragraph after line 640 |
| this spec | Tick tasks and ACs, and fill §8 |

## 6. Tasks

Every Maven run follows `CLAUDE.md`'s "Always log local Maven runs": redirect to `.logs/`, print
the `tail -f` command, `wait`, and report the exit code.

This is build configuration, so there is no red–green unit test. The failing check is **Task 1**:
it establishes, on the RC as it stands, what each AC measures, so Task 4 compares the same things.

- [x] **Task 1 — baseline on the RC as merged.** Before any edit, run
      `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`, then record for §8:
      1. `grep -nE "Copying [0-9]+ resources? from target.test-classes to target.classes" .logs/mvn-clean-install.log`.
         Expected: no match. `#131` already fixed this, and a match means the premise of §2.1 is
         wrong, so stop and report.
      2. `grep -cE "remote-resources:[^:]+:process" .logs/mvn-clean-install.log`.
         Expected: `4`, one per unpacking module. Task 4 expects `2`. Maven 3.9 logs the goal
         prefix, `remote-resources:3.3.0:process`, not the artifactId (§8, Task 1).
      3. Commit nothing.
- [x] **Task 2 — the POMs.** Apply §4.1 to both Solr POMs and §4.2 to `dsh-data` and
      `dsh-rest-api`. Then `mvn -B -pl dsh-solr/solr-advanced-numbers-filter,dsh-solr/solr-terms-vector-order,dsh-data,dsh-rest-api -am clean install > .logs/mvn-clean-install-four.log 2>&1`.
      Expected: exit 0, all four report `All coverage checks have been met.` Commit:
      `build(#124): drop the unused fixture unpack from the Solr modules, explain attachToMain`.
- [x] **Task 3 — the PRD note.**
      1. Measure the fixtures: `ls -l dsh-test-dataset/src/test/resources/pdf/`. If the largest is
         under 1 MiB (1,048,576 bytes), `<SIZE-FINDING>` becomes "The largest fixture is N KB, so
         none reaches it: add one above 1 MB with the story that implements `GcsFileStorageService`."
         Otherwise: "`<name>` (N MB) exceeds it and exercises the GCS path. Keep it."
      2. Insert §4.3's paragraph with the finding filled in.
      3. Run the markdown lint command from `CLAUDE.md`. Expected: exit 0.
      4. Commit: `docs(#124): record the fixture gaps for the ADR-001 waves`.
- [x] **Task 4 — verify AC002–AC004 on the full reactor.** §7, in order. Record every result in §8,
      tick the ACs, commit: `docs(#124): record the verification`.

## 7. Verification

Run from a clean state, one after another. Each run logs to its own file in `.logs/`.

1. **AC002, clean build.** `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`. Then:
   - `grep -nE "Copying [0-9]+ resources? from target.test-classes to target.classes" .logs/mvn-clean-install.log`:
     no match.
   - For each production jar, `dsh-data/target/dsh-data-*.jar`,
     `dsh-rest-api/target/dsh-rest-api-*.jar` and the two Solr jars:
     `unzip -l <jar> | grep -ciE '\.pdf$'`: `0` for each.
   - `grep -cE "remote-resources:[^:]+:process" .logs/mvn-clean-install.log`: `2`,
     down from Task 1's `4`, and `ls dsh-solr/*/target/test-classes/pdf 2>&1` reports "No such
     file or directory" for both Solr modules. Together they show §4.1 took effect.
2. **AC003, second build without clean.** `mvn -B install > .logs/mvn-install-no-clean.log 2>&1`,
   straight after run 1. Then:
   - exit 0, and `grep -c "All coverage checks have been met." .logs/mvn-install-no-clean.log`
     equals the count in run 1's log, with `grep -c "Rule violated"` equal to `0`;
   - for every jar under `*/target/` and `*/*/target/` except `dsh-test-dataset`'s:
     `unzip -l <jar> | grep -cE '(Test|IT|ITConfiguration)(\$[^/]*)?\.class$'` is `0`.
3. **AC004, unit and integration.** Run 1 covers the unit tests, including `DocumentTest`,
   `MongoDocumentDaoTest` and the three `dsh-rest-api` service tests. Then
   `mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it.log 2>&1`: exit 0, and
   `DocumentResourceIT` reports `Tests run: 6, Failures: 0, Errors: 0`.

## 8. Build record

Built on 2026-09-28 with Maven 3.9.16 and JDK 17.0.20.1.

### Task 1 — baseline at `3ba637116` (the RC plus this spec)

`mvn -B clean install`: exit 0.

1. `Copying … from target\test-classes to target\classes`: **no match.** `#131`'s fix is in place, as
   §2.1 says.
2. Process executions: **4**, in `dsh-data`, `dsh-rest-api`, `solr-terms-vector-order` and
   `solr-advanced-numbers-filter`. The pattern as first written,
   `maven-remote-resources-plugin:[^:]+:process`, counted **0**. Maven 3.9 logs the goal prefix
   (`--- remote-resources:3.3.0:process (default) @ dsh-data ---`), not the artifactId. The pattern
   was wrong, not the premise, so the build went ahead and §6 and §7 now use
   `remote-resources:[^:]+:process`.
3. Coverage: 8 `All coverage checks have been met.`, 0 `Rule violated`.

### Task 2 — the POMs, commit `a0eed58b9`

Four-module `clean install`: exit 0. `dsh-data`, `dsh-rest-api`, `solr-terms-vector-order` and
`solr-advanced-numbers-filter` each report `All coverage checks have been met.` Only `dsh-data` and
`dsh-rest-api` still log a `process` execution.

### Task 3 — the PRD note, commit `6cedfd9da`

| Fixture | Bytes |
|---|---|
| `The-Categories.pdf` | 268,696 |
| `bbc-news-1.pdf` | 105,412 |
| `edition.cnn.com-1.pdf` | 35,454 |
| `edition.cnn.com-2.pdf` | 39,031 |

The largest is 262 KB, under 1 MiB, so the "none reaches it" finding went in. Markdown lint: exit 0.

### Task 4 — §7

**Run 1, `mvn -B clean install`: exit 0.**

- `Copying … from target\test-classes to target\classes`: no match.
- `.pdf` entries: `dsh-data-0.3.0-SNAPSHOT.jar` 0, `dsh-rest-api-0.3.0-SNAPSHOT.jar` 0,
  `solr-advanced-numbers-filter-0.3.0-SNAPSHOT.jar` 0, `solr-terms-vector-order-0.3.0-SNAPSHOT.jar` 0.
- Process executions: **2**, down from 4. `ls dsh-solr/*/target/test-classes/pdf`: "No such file or
  directory".
- Coverage: 8 met, 0 `Rule violated`.
- Fixture readers, all passing: `MongoDocumentDaoTest` 5, `DocumentTest` 18,
  `DocumentEnqueueResponseMessageHandlerTest` 5, `DocumentHandlingServiceImplTest` 3,
  `DocumentSubmissionServiceImplTest` 8. `dsh-data/target/test-classes/pdf/` and
  `dsh-rest-api/target/test-classes/pdf/` each hold all four PDFs.

**Run 2, `mvn -B install` straight after: exit 0.**

- Coverage: 8 met, as in run 1, and 0 `Rule violated`.
- `*Test`/`*IT`/`*ITConfiguration` classes: 0 in each of the nine jars other than
  `dsh-test-dataset`'s: `dsh-coverage-report`, `dsh-data`, `dsh-doc-processor-worker`,
  `dsh-keyword-extractor`, `dsh-top-sentences-extractor`, `dsh-doc-indexer-worker`, `dsh-rest-api`
  and the two Solr jars.
- The log shows `Copying 14 resources from target\test-classes to target\test-classes`
  (`dsh-data`) and `Copying 20 …` (`dsh-rest-api`). That is the bundle's test-side attachment,
  `attachToTest`, which defaults to `true`: `testResources` re-copies the directory onto itself. It
  never reaches `target/classes`, and AC002's pattern does not match it. It is harmless, so it stays
  as it is.

**Run 3, `mvn -B clean install -DintegrationTests`: exit 0.** `DocumentResourceIT`: `Tests run: 6,
Failures: 0, Errors: 0, Skipped: 0`. The five other ITs in the reactor pass too.

## 9. Acceptance criteria

- [x] **AC001** — the spec records the investigation for each of the four modules, with the
      evidence behind each answer. *§3.*
- [x] **AC002** — a clean `mvn -B install` logs no copy from `target\test-classes` to
      `target\classes` in any module, and no production jar contains the fixture PDFs. §3 found no
      consumer that needs them. *§7 run 1.*
- [x] **AC003** — after a `mvn -B install`, a second `mvn -B install` without `clean` passes
      `jacoco:check` in every module, and no production jar contains a `*Test` or `*IT` class.
      *§7 run 2.*
- [x] **AC004** — every test that reads a fixture still passes, unit and integration, and the
      fixtures remain on the test classpath. *§7 runs 1 and 3.*

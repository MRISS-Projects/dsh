---
issue: 137
slug: run-postman-collections-against-lifecycle-server
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 137 — Run the Postman collections against a lifecycle-managed server

## 1. Story

**As a** developer changing the REST API
**I want** the Postman collections in `specs/api/postman/` run by newman against a server Maven starts and stops
**So that** `api-testing.yml` asserts something, rather than building the reactor and booting the app to skip every check

## 2. Context

- Wave: 1 (`specs/product/PRD.md` §4, "Wave 1 — Pipeline design, documentation and upstream
  catch-up", where `#137` is listed with `#140` as spun off from `#46`). DSH milestone
  `0.4.0-SNAPSHOT`.
- Issue: [#137](https://github.com/MRISS-Projects/dsh/issues/137), labelled `task`. AC005 was added
  from `#48`.
- Parent branch: `DEVELOP`, at `8a84a4d07` when this spec was written. The task branch is
  `issue-137-run-postman-collections-against-lifecycle-server`.
- It is layer 4 of the test layers, "External client", in `.github/copilot/rules/testing-patterns.md`,
  which `#46` wrote down as "when collections exist, `#137`".

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| `specs/api/postman/` holds only a `README.md`, so `api-testing.yml`'s newman step prints `No Postman collections found in specs/api/postman/ – skipping.` and the job passes | `specs/api/postman/`; `.github/workflows/api-testing.yml:228-236` |
| `api-testing.yml` runs on pull requests touching `dsh-rest-api/**` or `specs/api/**`, starts the jar with `java -jar … --spring.profiles.active=test &`, never stops it, and waits on `/actuator/health` | `.github/workflows/api-testing.yml:1-12`, `:212-218` |
| No `test` profile reaches the server. `#140` added `dsh-rest-api/src/test/resources/application-test.properties`, but neither the jar nor `spring-boot:start` (which runs on `target/classes`) puts test resources on the classpath | `dsh-rest-api/pom.xml`, the `start-application` execution; `specs/architecture/profiles-and-configuration.md` P31 |
| `#46`'s `http-integration-tests` profile, active under `-DintegrationTests`, reserves ports and starts `rabbitmq:3` in `package`, forks the application in `pre-integration-test`, stops both in `post-integration-test`, and gives failsafe `dsh.it.baseUrl` | `dsh-rest-api/pom.xml`, the profile |
| A red test does not leak, because `failsafe:integration-test` records and `failsafe:verify` fails | parent-poms `mriss-parent` 3.11.2, profile `integration-tests`; `specs/stories/46-start-app-around-integration-tests.md` §4.1 |
| Layers 2 to 4 never run on pull requests; staging runs `-DintegrationTests` on every build, on `ubuntu-latest` with `setup-java`'s Maven cache | `testing-patterns.md`, "Where integration tests run"; parent-poms `project-staging.yml:55`, `:84-88`, `:226-228` |
| The deployed acceptance layer 5a reuses the layer-3 and layer-4 tests with another base URL | `profiles-and-configuration.md` §7, row 5a; `ADR-004-gcp-tiers.md`, "A local run" |
| parent-poms manages neither `io.fabric8:docker-maven-plugin` nor `com.github.eirslett:frontend-maven-plugin`. DSH pins the first locally, with a comment pointing at parent-poms `#90`, open in milestone `3.12.0-SNAPSHOT` beside `#109` | `dsh-rest-api/pom.xml`; parent-poms issues |
| DSH pins the released parent `3.11.2` and passes no `-U`, so a parent-poms snapshot deploy does not reach it | root `pom.xml:13`; `docs/devops/README.md`, "Parent POM" |
| This box has no Node on `PATH`, only a standalone zip under `~/apps` | `CLAUDE.md`'s environment; the owner's standing rule |

### 2.2 Measured while designing

Probes on 2026-11-08, in the session scratchpad, with newman `6.2.3` under the standalone Node
`v24.21.0` and a throwaway HTTP server:

| Probe | Result |
|---|---|
| `pm.execution.setNextRequest(pm.info.requestName)` from a test script | the request re-ran: 3 polls, then the next item. `typeof pm.execution.setNextRequest` is `function` |
| A collection with a request and no `pm.test` | exit `0`, `run.stats.assertions.total` `0` in the JSON report. **newman passes a collection that asserts nothing** |
| A collection with `{{baseUrl}}` and no value for it | exit `1`, the request errors |
| A failing `pm.test` | exit `1`, `run.stats.assertions.failed` `1`, `run.failures` length `1` |
| A multipart file part, `"src": "f.pdf"`, with `--working-dir` an **absolute Windows path** and `--no-insecure-file-read` | the file arrived |
| The same with `--working-dir` relative, or an MSYS path | `file load error: PPERM: insecure file access outside working directory`. **The request is still sent, without the file, and the run does not fail on it** |
| A file part pointing outside the working directory | refused with the same `PPERM` |
| `frontend-maven-plugin` `2.0.2`'s descriptor | requires Java 17 and Maven 3.6.0. `install-node-and-npm` and `npm` both take `installDirectory`, `workingDirectory` and `skip`; `npm`'s `arguments` defaults to `install` |

Two design rules follow: the IT passes every path absolute, and a missing fixture is caught by the
collection's assertions, not by newman.

### 2.3 A contract drift found while designing

`specs/api/openapi/dsh-rest-api.yaml` (`/submit`'s description) and
`specs/features/rest-api-endpoints.md:50` say **"Every submission gets a new token, the same file
included."** Since `d0d5c9537` (2026-10-19, "reuse the stored document of a resubmitted file"),
a resubmitted file keeps its token, and `DocumentResourceHttpIT.submit_whenSameFileTwice_shouldReturnTheSameTokenAndQueueItAgain`
asserts that. The contract text dates from `#162` (2026-10-14) and was not updated.

This story does not fix it: the issue does not cover the contract, and it is the owner's call which
side is right. The collection keeps clear of the case (§3.4), and a follow-up issue is proposed in
Task 7.

## 3. Design

### 3.1 Decisions

1. **`api-testing.yml` is removed** (the issue's open question). Layer 4 runs where layers 2 and 3
   run: staging on every build, the deploy workflow when it exists, and the local gate 3. Staging
   already passes `-DintegrationTests`, so no workflow changes. Keeping a workflow on pull requests
   would reverse `#46`'s "never on pull requests". This settles AC003 and AC005.
2. **newman runs inside a failsafe IT**, `PostmanCollectionsIT`, as a child process. AC002 then
   holds by the mechanism `#46` already proved: failsafe records in `integration-test`,
   `post-integration-test` tears down, `verify` fails. Rejected: a plugin goal in
   `integration-test`, which stops the build before teardown, or which needs a hand-written
   record-then-fail split; and Node from `PATH`, which this box does not have.
3. **Maven brings Node and newman.** `frontend-maven-plugin` installs Node `v24.22.2` and runs
   `npm ci` on a committed `package.json` and `package-lock.json` that pin newman `6.2.3`, all
   under `dsh-rest-api/target/newman/`. No Node from `PATH` is used, locally or in CI.
4. **The base URL is the runner's only input about the server.** `dsh.it.baseUrl`, as layer 3.
   Pointing it at `dsh-dev` later is all layer 5a needs from this class.
5. **An empty directory fails, and so does a collection with no assertion** (AC004, and §2.2's
   second probe).
6. **`frontend-maven-plugin`'s version is pinned locally, temporarily.** It belongs in parent-poms'
   `pluginManagement`. A snapshot deploy does not reach DSH (§2.1), and parent-poms `3.12.0` cannot
   release until `#109` and `#90` are done, so this story follows `#46`'s precedent: parent-poms
   `#90` is widened to both plugins, and the local `<version>` carries a comment pointing at it.
   `#90`'s re-pin deletes both local versions.
7. **Layer 4 comes into force**, with layer 3's trigger: a story that adds a REST endpoint or
   changes one's contract updates the collection too.

### 3.2 The lifecycle

All of it is in `dsh-rest-api/pom.xml`'s existing `http-integration-tests` profile, so a plain
`mvn install` binds none of it.

| Phase | Goal | Does |
|---|---|---|
| `process-test-resources` | `resources:copy-resources`, `stage-newman` | copies `src/test/newman/` (`package.json`, `package-lock.json`) to `target/newman/` |
| `package` | `frontend:install-node-and-npm`, `install-node` | Node `v24.22.2` and its npm into `target/newman/node/`. The archive is downloaded once and cached in the local Maven repository |
| `package` | `frontend:npm`, `install-newman` | `npm ci --no-audit --no-fund` in `target/newman/` |
| `package` | unchanged | port reservation, `docker:start` |
| `pre-integration-test` | unchanged | `spring-boot:start` |
| `integration-test`, `verify` | failsafe | `PostmanCollectionsIT` beside `DocumentResourceHttpIT` |
| `post-integration-test` | unchanged | `spring-boot:stop`, then `docker:stop` |

- **Every new execution skips with `${skipITs}`**, as the containers and the server do.
- **Declaration order inside the profile.** `#46` found that Maven inserts a profile's *new* plugins
  ahead of the first plugin the profile shares with the main build. `frontend-maven-plugin` is new
  and is declared **first**, so in `package` it lands ahead of `build-helper` and `docker`: a failed
  download or `npm ci` fails the build before any container exists. `maven-resources-plugin` is
  declared **last**, after failsafe, so that whether Maven treats it as shared or new, it moves no
  other plugin. The build log proves the order (Task 3), as `#46`'s did.
- **Failsafe gets five more system properties**, beside `dsh.it.baseUrl`:

  | Property | Value |
  |---|---|
  | `dsh.it.nodeDir` | `${project.build.directory}/newman/node` |
  | `dsh.it.newman` | `${project.build.directory}/newman/node_modules/newman/bin/newman.js` |
  | `dsh.it.collections` | `${project.basedir}/../specs/api/postman` |
  | `dsh.it.fixtures` | `${project.build.directory}/test-classes/pdf` |
  | `dsh.it.newmanReports` | `${project.build.directory}/newman/reports` |

**Locally and in CI it is the same command and the same Node.** On this box the archive is
`node-v24.22.2-win-x64.zip`, cached under `D:\.m2\repository\com\github\eirslett\node\`; on
`ubuntu-latest` it is the `linux-x64` tarball, usually restored by `setup-java`'s Maven cache. The
runner's own Node and `~/apps`' are never used. `clean` removes Node and newman with `target/`.

### 3.3 The IT

`dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PostmanCollectionsIT.java`, JUnit
Jupiter, no Spring context. Its code is in Task 2.

- **Inputs.** The five properties of §3.2 and `dsh.it.baseUrl`. A missing one fails at once, naming
  `-DintegrationTests`. No fallback to `localhost:8080`.
- **One dynamic test per collection.** A `@TestFactory` lists `*.postman_collection.json`, sorted,
  and yields one test named after each file.
- **AC004.** An empty or missing directory fails the factory itself, naming the directory. JUnit
  would otherwise report an empty stream as a pass.
- **Per collection** it runs, as a child process, with every path absolute (§2.2):

  ```text
  <nodeDir>/node[.exe] <newman> run <collection>
    --env-var baseUrl=<dsh.it.baseUrl>
    --working-dir <fixtures> --no-insecure-file-read
    --reporters cli,json,junit
    --reporter-json-export  <reports>/<name>.json
    --reporter-junit-export <reports>/<name>.xml
    --timeout-request 60000 --color off
  ```

  Output goes to `<reports>/<name>.log`, not a pipe, so a full buffer cannot block it. `node.exe` is
  used when it exists, `node` otherwise. A run longer than 5 minutes is killed and fails.
- **It asserts** newman's exit code is `0`, with the log in the message, and then that the JSON
  report counts more than zero assertions.
- **It shares the server** with `DocumentResourceHttpIT`. Failsafe runs the classes one after
  another in one JVM, so the collection's single submission never overlaps another (`#142`).

### 3.4 The collection

`specs/api/postman/dsh-rest-api.postman_collection.json`, Collection v2.1, written by hand to
`specs/api/openapi/dsh-rest-api.yaml`. It asserts what the contract promises, never the code's
constants. Its JSON is in Task 4.

| # | Request | Asserts |
|---|---|---|
| 1 | `POST /submit`, `title` and `contents` = `edition.cnn.com-1.pdf` | `200`, JSON, `token` a non-empty string other than `ERROR`, `message` `''`. Stores `token` and a 15 s deadline |
| 2 | `GET /status/{{token}}`, polled | every answer `200` and JSON. It re-runs itself after 250 ms until the status is `QUEUED_FOR_INDEXING_SUCCESS` or `QUEUED_FOR_INDEXING_ERROR`, or the deadline passes, and the last answer must be `QUEUED_FOR_INDEXING_SUCCESS`. The contract allows `TOKEN_NOT_FOUND` for a moment after `/submit` |
| 3 | `GET /status/{{unknownToken}}`, a fresh `no-such-token-<guid>` | `200`, JSON, `status` `TOKEN_NOT_FOUND`, `message` "No token was found with the contents: " followed by the token |
| 4 | `POST /submit`, blank `title`, with `contents` | `200`, JSON, `token` `ERROR`, `message` `Error submitting file: Document title and contents can't be null.` |
| 5 | `POST /submit`, `title` only | `400`, JSON, `status` `400`, `error` `Bad Request`, `path` `/v1/dsh/document/submit` |

- **Why `edition.cnn.com-1.pdf`.** `DocumentResourceHttpIT` submits `bbc-news-1.pdf`, twice, before
  or after this class runs. A fixture nobody else submits keeps request 1 a first submission, so
  the collection never touches the resubmission case of §2.3.
- **`baseUrl` has no default in the collection.** A run without it fails (§2.2).
- **`specs/api/postman/local.postman_environment.json`** gives `baseUrl` `http://localhost:8080` for
  Postman Desktop and manual runs. Its name does not match the IT's pattern.
- **Not in it:** JSON-schema checks against `components.schemas`, which would be a second copy of
  the schemas; the oversized-upload `500`, which needs a fixture over 1 MB; resubmission (§2.3).

### 3.5 The documents

Present-tense documents change. History does not: `specs/stories/*`, `docs/superpowers/*`,
`.superpowers/*`, the PRD's narrative (step 8 reconciles its status), the dated fact table of
`profiles-and-configuration.md` §2, and `specs/devops/deploy-release-profiles-reorganization.md`,
which is `#84`'s feature specification. The exact text is in Task 5.

| File | Change |
|---|---|
| `.github/workflows/api-testing.yml` | deleted |
| `specs/api/postman/README.md` | rewritten: contents, naming rule, how the IT runs the collections, Desktop setup |
| `.github/copilot/rules/testing-patterns.md` | layer 4 in force; the `api-testing.yml` clause and pointer go; `-DskipITs` skips the install too |
| `.github/copilot-instructions.md` | layer 4 is no longer "not yet in force" |
| `docs/devops/README.md` | the diagram node, the table row, and every "`ci.yml` and `api-testing.yml`" go to `ci.yml` alone |
| `CLAUDE.md` | gate 1's note: `-DintegrationTests` also needs nodejs.org and registry.npmjs.org; "neither workflow passes `-U`" becomes `ci.yml` |
| `specs/architecture/profiles-and-configuration.md` | §6.4 and §7's present-tense sentences, and follow-up 3 marked done |
| `specs/testing/performance-benchmarks/README.md` | its CI pointer: no workflow runs performance tests yet |

### 3.6 Known limits, recorded rather than engineered around

- **Every `-DintegrationTests` run needs registry.npmjs.org**, and the first on a machine needs
  nodejs.org. An outage turns staging red with no change here.
- **A failure before the tests still leaks**, as `#46` recorded. `npm ci` runs before `docker:start`,
  so this story adds no new leak path.
- **Staging is the first CI proof.** A task branch cannot run it; until then the evidence is gate 3.
- **The collection asserts the contract as written.** Where the contract is stale, the collection
  either keeps clear (§2.3) or fails, which is its purpose.

### 3.7 Out of scope

Layer 5a against `dsh-dev`; JSON-schema checks; the oversized upload; `#138`'s status codes; the
contract drift of §2.3; moving `docker-maven-plugin`'s version upstream (`#90`); publishing newman's
reports on the Maven site, planned by Task 7 Step 7.

## 4. Verification design

| AC | Check | Where |
|---|---|---|
| order | the gate-3 log shows `install-node`, `install-newman` before `docker:start`, and `spring-boot:stop` before `docker:stop` | Task 3 |
| AC001 | green: gate 3 passes and the failsafe report shows `dsh-rest-api.postman_collection.json` passing with its assertion count. Mutation 1: one expected value changed, the build fails in `verify` naming the collection and the assertion | Tasks 4, 6 |
| AC002 | every red run (Task 2, Task 3, mutations 1 and 3) reaches `spring-boot:stop` and `docker:stop`, and `docker ps -a --filter name=dsh-it-` prints nothing after it | Tasks 2, 3, 6 |
| AC003 | `.github/workflows/api-testing.yml` does not exist | Task 7 |
| AC004 | Tasks 2 and 3 fail with the empty-directory message (mutation 2, by construction). Mutation 3: a collection with no `pm.test` fails on the zero-assertion check | Tasks 2, 3, 6 |
| AC005 | `grep -rn "spring.profiles.active" .github/` finds nothing | Task 7 |
| no side effects | gate 1 unchanged, no new plugin in its log; `-DskipITs` installs nothing; coverage equals the baseline, since nothing in `src/main` changes | Tasks 1, 7 |

## 5. Files to change

| File | Change |
|---|---|
| `dsh-rest-api/pom.xml` | the profile: `frontend-maven-plugin`, `stage-newman`, five failsafe properties (§3.2) |
| `dsh-rest-api/src/test/newman/package.json`, `package-lock.json` | new (Task 3) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PostmanCollectionsIT.java` | new (Task 2) |
| `specs/api/postman/dsh-rest-api.postman_collection.json` | new (Task 4) |
| `specs/api/postman/local.postman_environment.json` | new (Task 4) |
| the documents of §3.5 | Task 5 |
| `specs/stories/137-run-postman-collections-against-lifecycle-server.md` | this spec, §8 filled in |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** the Postman collections in `specs/api/postman/` run under `-DintegrationTests` against the
lifecycle-managed server, fail the build when they fail, and `api-testing.yml` is gone.

**Architecture:** a failsafe IT runs newman as a child process. Node and newman are installed by
`frontend-maven-plugin` into `target/newman/` inside `#46`'s profile. One hand-written collection.

**Tech stack:** Java 17, JUnit Jupiter, AssertJ, Jackson 3, Maven, `frontend-maven-plugin` 2.0.2,
Node v24.22.2, newman 6.2.3, Docker, markdownlint.

**Spec:** this file.

### Global constraints

- `dsh-rest-api/pom.xml` and the Java class indent with tabs; the code blocks below show four
  spaces, to satisfy markdownlint. JSON files indent with two spaces.
- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"), and every gate run includes `clean`.
- Gate 3 needs Docker. Docker Desktop is at `D:\Docker\app` and is often stopped: start it, then
  wait until `docker info` succeeds.
- Commands run in Git Bash. `npm` and markdownlint need the standalone Node on `PATH`, per command:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`. The build itself never uses it.
- Gate 3 for this story is `mvn -B clean verify -DintegrationTests -pl dsh-rest-api -amd`, after gate 1
  has installed every module.
- Versions, exactly: `frontend-maven-plugin` `2.0.2`, Node `v24.22.2`, newman `6.2.3`.
- Commit messages are `test(#137): …`, `build(#137): …`, `ci(#137): …` and `docs(#137): …`, and end
  with the session's attribution lines.

### Task 1: Baseline and parent-poms `#90`

- [ ] **Step 1: No leftovers.** `docker ps -a --filter name=dsh-it- --format '{{.Names}}'` prints
  nothing. If it prints names, remove them with `docker rm -f $(docker ps -aq --filter name=dsh-it-)`.
- [ ] **Step 2: Gate 1 baseline.**

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-baseline.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`. Record `dsh-rest-api`'s LINE and BRANCH from
  `dsh-rest-api/target/site/jacoco/jacoco.csv` in §8.1.
- [ ] **Step 3: Widen parent-poms `#90`,** after the owner approves the text. Title: "Manage
  docker-maven-plugin's and frontend-maven-plugin's versions". Body, appended:

  > DSH `#137` (MRISS-Projects/dsh#137) adds `com.github.eirslett:frontend-maven-plugin` to the same
  > `dsh-rest-api` profile, to install Node and newman for the Postman collections. It is not
  > managed here either, so DSH pins `2.0.2` locally, with the same comment.
  >
  > - Add `frontend.maven.plugin.version` (`2.0.2`, or the then-current release) and a
  >   `pluginManagement` entry for `com.github.eirslett:frontend-maven-plugin`.
  > - The DSH re-pin deletes both local `<version>`s and their comments.

  ```bash
  gh issue edit 90 -R MRISS-Projects/parent-poms --title "…" --body-file <approved body>
  ```

### Task 2: The IT, red

**Files:**

- Create: `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PostmanCollectionsIT.java`
- Modify: `dsh-rest-api/pom.xml`, the profile's `maven-failsafe-plugin` `systemPropertyVariables`

**Interfaces:** consumes `dsh.it.baseUrl` (existing) and the five properties below. Produces the
property names Task 3's paths must satisfy.

- [ ] **Step 1: The failsafe properties.** In the profile's `maven-failsafe-plugin`, after
  `<dsh.it.baseUrl>`, add:

  ```xml
                                <!-- #137: what PostmanCollectionsIT needs to run newman; the profile installs both under target/newman -->
                                <dsh.it.nodeDir>${project.build.directory}/newman/node</dsh.it.nodeDir>
                                <dsh.it.newman>${project.build.directory}/newman/node_modules/newman/bin/newman.js</dsh.it.newman>
                                <dsh.it.collections>${project.basedir}/../specs/api/postman</dsh.it.collections>
                                <dsh.it.fixtures>${project.build.directory}/test-classes/pdf</dsh.it.fixtures>
                                <dsh.it.newmanReports>${project.build.directory}/newman/reports</dsh.it.newmanReports>
  ```

- [ ] **Step 2: Write the IT.**

  ```java
  package com.mriss.dsh.restapi.integration;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.fail;

  import java.io.File;
  import java.io.IOException;
  import java.nio.charset.StandardCharsets;
  import java.nio.file.Files;
  import java.util.Arrays;
  import java.util.List;
  import java.util.concurrent.TimeUnit;
  import java.util.stream.Stream;

  import org.junit.jupiter.api.DynamicTest;
  import org.junit.jupiter.api.TestFactory;

  import tools.jackson.databind.JsonNode;
  import tools.jackson.databind.ObjectMapper;

  /**
   * Runs every Postman collection in {@code specs/api/postman/} with newman, against the server that the
   * {@code http-integration-tests} profile starts: layer 4, a client that sees only the API contract.
   * newman is a separate process, run from the Node and the newman the same profile installs under
   * {@code target/newman}, so neither has to be on the PATH. A failed collection fails its test: failsafe
   * records it, the profile still stops the server and the container, and {@code verify} fails the build.
   */
  public class PostmanCollectionsIT {

      private static final String COLLECTION_SUFFIX = ".postman_collection.json";

      /** So that a newman run that hangs fails the test rather than the build. */
      private static final long RUN_TIMEOUT_MINUTES = 5;

      /** The read timeout DocumentResourceHttpIT uses. */
      private static final String REQUEST_TIMEOUT_MILLIS = "60000";

      @TestFactory
      public Stream<DynamicTest> everyCollection_shouldPassAgainstTheServer() {
          String baseUrl = required("dsh.it.baseUrl");
          File node = node(new File(required("dsh.it.nodeDir")));
          File newman = new File(required("dsh.it.newman"));
          File collections = new File(required("dsh.it.collections"));
          File fixtures = new File(required("dsh.it.fixtures"));
          File reports = new File(required("dsh.it.newmanReports"));

          File[] found = collections.listFiles((dir, name) -> name.endsWith(COLLECTION_SUFFIX));
          if (found == null || found.length == 0) {
              fail("No *" + COLLECTION_SUFFIX + " in " + collections.getAbsolutePath()
                      + ". An empty directory is a failure, not a pass (#137).");
          }
          Arrays.sort(found);
          return Arrays.stream(found).map(collection -> DynamicTest.dynamicTest(collection.getName(),
                  () -> run(node, newman, collection, baseUrl, fixtures, reports)));
      }

      private static void run(File node, File newman, File collection, String baseUrl, File fixtures, File reports)
              throws IOException, InterruptedException {
          String fileName = collection.getName();
          String name = fileName.substring(0, fileName.length() - COLLECTION_SUFFIX.length());
          Files.createDirectories(reports.toPath());
          File json = new File(reports, name + ".json");
          File log = new File(reports, name + ".log");
          Files.deleteIfExists(json.toPath());

          // Every path absolute: newman refuses a relative working directory under --no-insecure-file-read.
          List<String> command = List.of(node.getAbsolutePath(), newman.getAbsolutePath(), "run",
                  collection.getAbsolutePath(),
                  "--env-var", "baseUrl=" + baseUrl,
                  "--working-dir", fixtures.getAbsolutePath(), "--no-insecure-file-read",
                  "--reporters", "cli,json,junit",
                  "--reporter-json-export", json.getAbsolutePath(),
                  "--reporter-junit-export", new File(reports, name + ".xml").getAbsolutePath(),
                  "--timeout-request", REQUEST_TIMEOUT_MILLIS,
                  "--color", "off");
          // To a file, not a pipe: a full pipe buffer would block newman.
          Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start();
          if (!process.waitFor(RUN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
              process.destroyForcibly();
              fail("newman did not finish " + fileName + " within " + RUN_TIMEOUT_MINUTES + " minutes; see " + log);
          }
          String output = Files.readString(log.toPath(), StandardCharsets.UTF_8);

          assertThat(process.exitValue()).as("newman's exit code for %s. Its output:%n%s", fileName, output).isZero();
          JsonNode assertions = new ObjectMapper().readTree(json).path("run").path("stats").path("assertions");
          assertThat(assertions.path("total").asInt())
                  .as("assertions in %s: newman passes a collection that asserts nothing", fileName)
                  .isPositive();
      }

      /** Node's binary is node.exe on Windows and node elsewhere. */
      private static File node(File nodeDir) {
          File windows = new File(nodeDir, "node.exe");
          return windows.isFile() ? windows : new File(nodeDir, "node");
      }

      private static String required(String property) {
          String value = System.getProperty(property);
          if (value == null) {
              fail(property + " is not set. Run this class under 'mvn -B install -DintegrationTests', "
                      + "which installs newman and starts the server it calls.");
          }
          return value;
      }
  }
  ```

- [ ] **Step 3: Run it red.** Docker running, gate 3, logged as
  `.logs/mvn-clean-verify-it-red.log`. Expected: `maven exit=1`, failing in `failsafe:verify` with
  `No *.postman_collection.json in …specs\api\postman. An empty directory is a failure, not a pass
  (#137).`, and `spring-boot:stop` and `docker:stop` in the log after the failure. `docker ps -a
  --filter name=dsh-it-` prints nothing. `DocumentResourceHttpIT` passes. Record in §8.2.
- [ ] **Step 4: Commit.**

  ```bash
  git add dsh-rest-api/pom.xml dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PostmanCollectionsIT.java
  git commit -m "test(#137): run every Postman collection with newman, and fail an empty directory"
  ```

### Task 3: Node and newman in the lifecycle

**Files:**

- Create: `dsh-rest-api/src/test/newman/package.json`, `dsh-rest-api/src/test/newman/package-lock.json`
- Modify: `dsh-rest-api/pom.xml`, the profile

**Interfaces:** produces `target/newman/node/node[.exe]` and
`target/newman/node_modules/newman/bin/newman.js`, the paths Task 2's properties name.

- [ ] **Step 1: `package.json`.**

  ```json
  {
    "name": "dsh-rest-api-newman",
    "version": "0.0.0",
    "private": true,
    "description": "newman for PostmanCollectionsIT; installed by the http-integration-tests profile under target/newman (#137)",
    "devDependencies": {
      "newman": "6.2.3"
    }
  }
  ```

- [ ] **Step 2: The lock file.**

  ```bash
  cd dsh-rest-api/src/test/newman
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" npm install --package-lock-only --no-audit --no-fund
  ls node_modules 2>/dev/null; cd -
  ```

  Expected: `package-lock.json` written, `"lockfileVersion": 3`, `newman` `6.2.3` under
  `packages["node_modules/newman"]`, and no `node_modules` directory.
- [ ] **Step 3: The plugins.** In the profile, as the **first** `<plugin>`:

  ```xml
                      <!-- #137: Node and newman for PostmanCollectionsIT, under target/newman, so neither has to be
                           on the PATH. Declared first: Maven puts a profile's new plugins ahead of the plugins it
                           shares with the main build, so in package this runs before docker:start, and a failed
                           install leaks no container. -->
                      <plugin>
                          <groupId>com.github.eirslett</groupId>
                          <artifactId>frontend-maven-plugin</artifactId>
                          <!-- Not yet managed by parent-poms; see parent-poms#90. -->
                          <version>2.0.2</version>
                          <configuration>
                              <skip>${skipITs}</skip>
                              <installDirectory>${project.build.directory}/newman</installDirectory>
                              <workingDirectory>${project.build.directory}/newman</workingDirectory>
                          </configuration>
                          <executions>
                              <execution>
                                  <id>install-node</id>
                                  <phase>package</phase>
                                  <goals>
                                      <goal>install-node-and-npm</goal>
                                  </goals>
                                  <configuration>
                                      <nodeVersion>v24.22.2</nodeVersion>
                                  </configuration>
                              </execution>
                              <execution>
                                  <id>install-newman</id>
                                  <phase>package</phase>
                                  <goals>
                                      <goal>npm</goal>
                                  </goals>
                                  <configuration>
                                      <arguments>ci --no-audit --no-fund</arguments>
                                  </configuration>
                              </execution>
                          </executions>
                      </plugin>
  ```

  And as the **last** `<plugin>`, after `maven-failsafe-plugin`:

  ```xml
                      <!-- #137: package.json and its lock into target/newman, so that npm ci leaves nothing in
                           src and clean removes it all. Declared last, so it moves no other plugin. -->
                      <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-resources-plugin</artifactId>
                          <executions>
                              <execution>
                                  <id>stage-newman</id>
                                  <phase>process-test-resources</phase>
                                  <goals>
                                      <goal>copy-resources</goal>
                                  </goals>
                                  <configuration>
                                      <skip>${skipITs}</skip>
                                      <outputDirectory>${project.build.directory}/newman</outputDirectory>
                                      <resources>
                                          <resource>
                                              <directory>src/test/newman</directory>
                                          </resource>
                                      </resources>
                                  </configuration>
                              </execution>
                          </executions>
                      </plugin>
  ```

  Extend the profile's leading comment with one line: "`#137`: `frontend-maven-plugin` is declared
  first and `maven-resources-plugin` last, for the same reason."
- [ ] **Step 4: Run it, still red.** Gate 3, logged as `.logs/mvn-clean-verify-it-tooling.log`.
  Expected: `maven exit=1` with Task 2's empty-directory message, because no collection exists yet.
  In the log, in this order: `stage-newman`, `install-node`, `install-newman`, `reserve-it-ports`,
  `start-it-infrastructure`, `start-application`, …, `stop-application`, `stop-it-infrastructure`.
  `dsh-rest-api/target/newman/node/node.exe` and
  `dsh-rest-api/target/newman/node_modules/newman/bin/newman.js` exist. `dsh-rest-api/src/test/newman/`
  holds the two files only. Record the order in §8.3.

  If `install-newman` runs after `start-it-infrastructure`, stop: the declaration-order rule of §3.2
  does not hold, and the spec is revised before going on.
- [ ] **Step 5: Commit.**

  ```bash
  git add dsh-rest-api/pom.xml dsh-rest-api/src/test/newman/package.json dsh-rest-api/src/test/newman/package-lock.json
  git commit -m "build(#137): install Node and newman under target/newman for the integration tests"
  ```

### Task 4: The collection, green

**Files:**

- Create: `specs/api/postman/dsh-rest-api.postman_collection.json`
- Create: `specs/api/postman/local.postman_environment.json`
- Modify: `specs/api/postman/README.md`

- [ ] **Step 1: The collection.**

  ```json
  {
    "info": {
      "name": "DSH REST API",
      "description": "Layer 4 of .github/copilot/rules/testing-patterns.md: the REST API as an external client sees it, asserted against specs/api/openapi/dsh-rest-api.yaml. PostmanCollectionsIT runs it under -DintegrationTests (#137).",
      "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
      {
        "name": "Submit a document",
        "event": [
          {
            "listen": "test",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.test('answers 200 with JSON', () => {",
                "  pm.response.to.have.status(200);",
                "  pm.response.to.be.json;",
                "});",
                "const body = pm.response.json();",
                "pm.test('accepts the document and answers its token', () => {",
                "  pm.expect(body.token).to.be.a('string').and.not.empty;",
                "  pm.expect(body.token).to.not.equal('ERROR');",
                "  pm.expect(body.message).to.equal('');",
                "});",
                "pm.collectionVariables.set('token', body.token);",
                "pm.collectionVariables.set('statusDeadline', Date.now() + 15000);"
              ]
            }
          }
        ],
        "request": {
          "method": "POST",
          "url": "{{baseUrl}}/v1/dsh/document/submit",
          "body": {
            "mode": "formdata",
            "formdata": [
              { "key": "title", "value": "CNN page, layer 4", "type": "text" },
              { "key": "contents", "type": "file", "src": "edition.cnn.com-1.pdf" }
            ]
          }
        }
      },
      {
        "name": "Status of the submitted document",
        "event": [
          {
            "listen": "prerequest",
            "script": {
              "type": "text/javascript",
              "exec": [
                "// The contract lets /status answer TOKEN_NOT_FOUND for a moment after /submit, so this request polls.",
                "setTimeout(() => {}, 250);"
              ]
            }
          },
          {
            "listen": "test",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.test('answers 200 with JSON', () => {",
                "  pm.response.to.have.status(200);",
                "  pm.response.to.be.json;",
                "});",
                "const status = pm.response.json().status;",
                "const settled = status === 'QUEUED_FOR_INDEXING_SUCCESS' || status === 'QUEUED_FOR_INDEXING_ERROR';",
                "if (!settled && Date.now() < Number(pm.collectionVariables.get('statusDeadline'))) {",
                "  pm.execution.setNextRequest(pm.info.requestName);",
                "} else {",
                "  pm.test('reaches QUEUED_FOR_INDEXING_SUCCESS within 15 s', () => {",
                "    pm.expect(status).to.equal('QUEUED_FOR_INDEXING_SUCCESS');",
                "  });",
                "}"
              ]
            }
          }
        ],
        "request": {
          "method": "GET",
          "url": "{{baseUrl}}/v1/dsh/document/status/{{token}}"
        }
      },
      {
        "name": "Status of an unknown token",
        "event": [
          {
            "listen": "prerequest",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.collectionVariables.set('unknownToken', 'no-such-token-' + pm.variables.replaceIn('{{$guid}}'));"
              ]
            }
          },
          {
            "listen": "test",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.test('answers 200 with JSON', () => {",
                "  pm.response.to.have.status(200);",
                "  pm.response.to.be.json;",
                "});",
                "pm.test('answers TOKEN_NOT_FOUND and names the token', () => {",
                "  const body = pm.response.json();",
                "  pm.expect(body.status).to.equal('TOKEN_NOT_FOUND');",
                "  pm.expect(body.message).to.equal('No token was found with the contents: ' + pm.collectionVariables.get('unknownToken'));",
                "});"
              ]
            }
          }
        ],
        "request": {
          "method": "GET",
          "url": "{{baseUrl}}/v1/dsh/document/status/{{unknownToken}}"
        }
      },
      {
        "name": "Submit with a blank title",
        "event": [
          {
            "listen": "test",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.test('answers 200 with JSON', () => {",
                "  pm.response.to.have.status(200);",
                "  pm.response.to.be.json;",
                "});",
                "pm.test('rejects the submission with token ERROR and the reason', () => {",
                "  const body = pm.response.json();",
                "  pm.expect(body.token).to.equal('ERROR');",
                "  pm.expect(body.message).to.equal(\"Error submitting file: Document title and contents can't be null.\");",
                "});"
              ]
            }
          }
        ],
        "request": {
          "method": "POST",
          "url": "{{baseUrl}}/v1/dsh/document/submit",
          "body": {
            "mode": "formdata",
            "formdata": [
              { "key": "title", "value": "", "type": "text" },
              { "key": "contents", "type": "file", "src": "edition.cnn.com-1.pdf" }
            ]
          }
        }
      },
      {
        "name": "Submit without contents",
        "event": [
          {
            "listen": "test",
            "script": {
              "type": "text/javascript",
              "exec": [
                "pm.test('answers 400 with JSON', () => {",
                "  pm.response.to.have.status(400);",
                "  pm.response.to.be.json;",
                "});",
                "pm.test('answers the error body', () => {",
                "  const body = pm.response.json();",
                "  pm.expect(body.status).to.equal(400);",
                "  pm.expect(body.error).to.equal('Bad Request');",
                "  pm.expect(body.path).to.equal('/v1/dsh/document/submit');",
                "});"
              ]
            }
          }
        ],
        "request": {
          "method": "POST",
          "url": "{{baseUrl}}/v1/dsh/document/submit",
          "body": {
            "mode": "formdata",
            "formdata": [
              { "key": "title", "value": "No contents", "type": "text" }
            ]
          }
        }
      }
    ]
  }
  ```

- [ ] **Step 2: The environment.**

  ```json
  {
    "name": "DSH local",
    "values": [
      { "key": "baseUrl", "value": "http://localhost:8080", "type": "default", "enabled": true }
    ],
    "_postman_variable_scope": "environment"
  }
  ```

- [ ] **Step 3: Run it green.** Gate 3, logged as `.logs/mvn-clean-verify-it-green.log`. Expected:
  `maven exit=0`. `dsh-rest-api/target/newman/reports/dsh-rest-api.log` shows 5 items, the status
  request repeated while it polls, and no failure. The failsafe report lists
  `dsh-rest-api.postman_collection.json` as passed. Record the requests, assertions and failsafe
  summary in §8.4.

  If the blank-title request answers `400` rather than `200`, newman omitted the empty part: change
  that `title` value to a single space, which `StringUtils.isBlank` also rejects, and record it.
- [ ] **Step 4: The README.** Replace `specs/api/postman/README.md` with:

  ````markdown
  # API Postman Collections

  Postman collections for the DSH REST API. They are layer 4 of the test layers in
  `/.github/copilot/rules/testing-patterns.md`: a client that sees only the API contract,
  `specs/api/openapi/dsh-rest-api.yaml`.

  ## Contents

  | File | Description |
  |------|-------------|
  | `dsh-rest-api.postman_collection.json` | Submit a document and follow its status; an unknown token; the two rejected submissions |
  | `local.postman_environment.json` | `baseUrl` for a server on `http://localhost:8080`, for Postman Desktop and manual runs |

  ## How the build runs them

  `PostmanCollectionsIT` in `dsh-rest-api` runs every `*.postman_collection.json` here with newman,
  under `-DintegrationTests`, against the server and RabbitMQ that the build starts:

  ```bash
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api
  ```

  - The build installs Node and newman under `dsh-rest-api/target/newman/`. Neither has to be on the
    `PATH`. The build needs Docker, and network access to nodejs.org and registry.npmjs.org.
  - It runs on every staging build, and in the local integration gate. Never on pull requests.
  - The reports are in `dsh-rest-api/target/newman/reports/`: newman's output, JSON and JUnit XML,
    one of each per collection.
  - A file whose name does not end in `.postman_collection.json` is not run.
  - The build fails when this directory holds no collection, and when a collection runs no
    assertion.

  ## Writing a collection

  1. Export it as **Collection v2.1**, or write it by hand, and name it `<name>.postman_collection.json`.
  2. Use `{{baseUrl}}` for the server, and give it no value in the collection: the build passes it.
  3. Assert what the OpenAPI document promises, with `pm.test`, never the code's internals.
  4. Name a file to upload by its bare file name. The build resolves it against the PDF fixtures of
     `dsh-test-dataset` and reads no file outside them.
  5. Run it under the command above before committing, and update the table above.

  ## Postman Desktop

  Import the collection and `local.postman_environment.json`, and select the environment. Set
  Postman's working directory (Settings, General) to `dsh-test-dataset/src/test/resources/pdf` in
  this repository, so the upload finds its file.
  ````

- [ ] **Step 5: Commit.**

  ```bash
  git add specs/api/postman/
  git commit -m "test(#137): add the first Postman collection, run against the lifecycle-managed server"
  ```

### Task 5: Remove `api-testing.yml`, and the documents

**Files:** §3.5.

- [ ] **Step 1: Delete the workflow.** `git rm .github/workflows/api-testing.yml`
- [ ] **Step 2: `testing-patterns.md`.**
  - Layer 4's table row becomes:
    `| 4. External client | Postman collections run by newman against layer 3's server: a consumer that sees only the API contract, not the internals | a story adds a REST endpoint or changes one's contract | staging, deploy, local gate |`
  - "Where integration tests run": `` `ci.yml` does not pass `-DintegrationTests`, and `api-testing.yml` builds with `-DskipTests`. `` becomes `` `ci.yml` does not pass `-DintegrationTests`. ``
  - Layer 3's `-DskipITs` bullet becomes: "**Skip with `-DskipITs`**, which skips the containers, the
    application, and layer 4's Node and newman install too. `-DskipTests` alone skips the tests but
    still starts and installs all of them"
  - "Layer 4: external client" is replaced by:

    ```markdown
    ## Layer 4: external client

    - One collection per file, `specs/api/postman/*.postman_collection.json`, Collection v2.1
    - **Required when a story adds a REST endpoint or changes one's contract**, as layer 3 is. Both
      change: layer 3 holds what we mean the code to do, layer 4 what the contract promises a client
    - Assert against `specs/api/openapi/dsh-rest-api.yaml`, never against the code's constants

    `PostmanCollectionsIT` runs every collection with newman under `-DintegrationTests`, against the
    server and the container of layer 3. The `http-integration-tests` profile installs Node and
    newman into `dsh-rest-api/target/newman/`, so neither has to be on the `PATH`.

    - **The base URL is the only input**, `baseUrl`, from `dsh.it.baseUrl`. A collection gives it no
      value
    - **Uploads name a fixture by its bare file name**, resolved against `target/test-classes/pdf/`.
      newman reads no file outside it
    - **An empty directory fails, and so does a collection that runs no assertion.** newman alone
      exits `0` for both
    - **A failing collection does not leak**, as a failing layer-3 test does not
    - **Reports** are in `dsh-rest-api/target/newman/reports/`: newman's output, JSON and JUnit XML,
      per collection
    - **The network is a prerequisite**, as Docker is: Node comes from nodejs.org, cached in the local
      Maven repository after the first run, and newman from registry.npmjs.org on every run
    ```

  - "Test Execution": `Performance tests run on release: reference .github/workflows/api-testing.yml`
    becomes "Performance tests are meant to run on release; no workflow runs them yet".
- [ ] **Step 3: `.github/copilot-instructions.md`.** The layer-4 bullet becomes:
  "- API tests as an external client (layer 4): Postman collections in `/specs/api/postman/`, run by
  `PostmanCollectionsIT` under `-DintegrationTests`"
- [ ] **Step 4: `docs/devops/README.md`.**
  - The Pipeline Map loses the `AT[...]` line, and the Workflow Reference its `api-testing.yml` row.
  - "The Maven toolchain is pinned": "`ci.yml` and `api-testing.yml` — the only two workflows here
    that invoke `mvn` — provision" becomes "`ci.yml` — the only workflow here that invokes `mvn` —
    provisions", and "Neither relies" becomes "It does not rely".
  - The Secrets table's `PACKAGES_READ_TOKEN` row: "Used by" `ci.yml`; "Why" ends "`ci.yml` builds
    pull-request code, so the credential it exposes must not be able to write."
  - "Both `ci.yml` and `api-testing.yml` open with" becomes "`ci.yml` opens with".
  - "Parent POM": ", as does `api-testing.yml`" goes; "neither workflow passes `-U`" becomes "`ci.yml`
    does not pass `-U`"; "**While a `SNAPSHOT` pin lasts, both workflows pass `-U`.** Add it back to
    `ci.yml` and `api-testing.yml`" becomes "**While a `SNAPSHOT` pin lasts, `ci.yml` passes `-U`.**
    Add it back to `ci.yml`".
  - Then `grep -n "api-testing\|both workflows\|neither workflow\|Both workflows" docs/devops/README.md`
    finds nothing about the removed workflow; fix any sentence it finds the same way.
- [ ] **Step 5: `CLAUDE.md`.**
  - Gate 1's note becomes: "For `dsh-rest-api`, `-DintegrationTests` also starts RabbitMQ in Docker
    and installs Node and newman under `target/newman/`, so it needs a running Docker daemon and
    access to nodejs.org and registry.npmjs.org."
  - "The parent is the released 3.11.2 since `#120`, so neither workflow passes `-U`." becomes "…, so
    `ci.yml` does not pass `-U`."
- [ ] **Step 6: `specs/architecture/profiles-and-configuration.md`.**
  - §6.4, "The `test` profile in `api-testing.yml`" becomes: "**The `test` profile in
    `api-testing.yml`.** Gone with the workflow (`#137`). Under R10 `test` has a meaning for context
    tests, and the forked server that layers 3 and 4 use carries no profile."
  - §7: "No profile is passed to the forked server, and layer 4 has no collection: `api-testing.yml`
    starts the application with `java -jar`, not through Maven (`#137`)." becomes "No profile is
    passed to the forked server. Since `#137`, layer 4 runs against it: `PostmanCollectionsIT` runs
    the collections in `specs/api/postman/` with newman."
  - Follow-up 3 gains: "Done by `#137`, which removed the workflow."
- [ ] **Step 7: `specs/testing/performance-benchmarks/README.md`.** "- CI workflow:
  `/.github/workflows/api-testing.yml`" becomes "- CI workflow: none yet; performance tests are meant
  to run on release".
- [ ] **Step 8: Nothing live still names the workflow.**

  ```bash
  grep -rn "api-testing" --include=*.md --include=*.yml --include=*.xml --include=*.json . \
    | grep -v -e "^./specs/stories/" -e "^./docs/superpowers/" -e "^./.superpowers/" -e "^./docs/wiki/" \
              -e "^./specs/product/PRD.md" -e "^./specs/devops/deploy-release-profiles-reorganization.md" \
              -e "^./README.md" -e "profiles-and-configuration.md:6[0-9]:"
  ```

  Expected: only the two sentences Step 6 wrote, which name it as removed.
- [ ] **Step 9: markdownlint.** The CLAUDE.md command, with
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`. Expected: no error.
- [ ] **Step 10: Commit,** in two commits.

  ```bash
  git add .github/workflows/api-testing.yml
  git commit -m "ci(#137): remove api-testing.yml; staging runs the collections"
  git add .github/copilot/rules/testing-patterns.md .github/copilot-instructions.md docs/devops/README.md \
          CLAUDE.md specs/architecture/profiles-and-configuration.md specs/testing/performance-benchmarks/README.md
  git commit -m "docs(#137): layer 4 is in force, and no document points at api-testing.yml"
  ```

### Task 6: The mutations

Each mutation is made in the working tree, run once with gate 3 logged as
`.logs/mvn-clean-verify-mutation-<n>.log`, and reverted with `git checkout -- <file>` or `rm`. After
each, `docker ps -a --filter name=dsh-it-` prints nothing, and `git status --short` is clean.

- [ ] **Step 1: Mutation 1, a wrong expectation (AC001, AC002).** In the collection, change
  `'TOKEN_NOT_FOUND'` in "Status of an unknown token" to `'TOKEN_NOT_FOUNDX'`. Expected: `maven
  exit=1` in `failsafe:verify`; the failure names `dsh-rest-api.postman_collection.json`, and its
  message carries newman's output with `answers TOKEN_NOT_FOUND and names the token`;
  `stop-application` and `stop-it-infrastructure` follow it in the log.
- [ ] **Step 2: Mutation 3, no assertion (AC004).** Add
  `specs/api/postman/zz-empty.postman_collection.json`:

  ```json
  {
    "info": { "name": "asserts nothing", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
    "item": [ { "name": "status", "request": { "method": "GET", "url": "{{baseUrl}}/v1/dsh/document/status/x" } } ]
  }
  ```

  Expected: `maven exit=1`; `zz-empty.postman_collection.json` fails on `assertions in
  zz-empty.postman_collection.json: newman passes a collection that asserts nothing`, while
  `dsh-rest-api.postman_collection.json` passes. Teardown as in Step 1.
- [ ] **Step 3: Mutation 2, the empty directory (AC004)** was Task 2 Step 3 and Task 3 Step 4, run on
  this IT. Nothing more to run. Record all three in §8.5.

### Task 7: The gates and the acceptance checks

- [ ] **Step 1: Gate 1,** as Task 1 Step 2, logged as `.logs/mvn-clean-install.log`. Expected:
  `maven exit=0`; no `frontend-maven-plugin` and no `stage-newman` in the log; `dsh-rest-api`'s
  LINE and BRANCH equal §8.1's.
- [ ] **Step 2: Gate 3,** logged as `.logs/mvn-clean-verify-it.log`. Expected: `maven exit=0`.
- [ ] **Step 3: `-DskipITs`.** `mvn -B clean verify -DintegrationTests -DskipITs -pl dsh-rest-api`,
  logged as `.logs/mvn-clean-verify-skipits.log`. Expected: `maven exit=0`, and `install-node`,
  `install-newman` and `stage-newman` each log that they are skipped; `target/newman/` does not
  exist.
- [ ] **Step 4: AC003, AC005, AC004's wording.**

  ```bash
  test ! -e .github/workflows/api-testing.yml && echo "AC003 ok"
  grep -rn "spring.profiles.active" .github/ || echo "AC005 ok"
  grep -rn "No Postman collections found" . --include=*.yml --include=*.java || echo "AC004 ok"
  ```

- [ ] **Step 5: Fill in §8 and commit.**

  ```bash
  git add specs/stories/137-run-postman-collections-against-lifecycle-server.md
  git commit -m "docs(#137): record the verification results"
  ```

- [ ] **Step 6: The follow-up issue of §2.3,** opened only after the owner approves its text. Title:
  "The contract says every submission gets a new token; a resubmitted file keeps its token".
  Labelled `bug`, milestone `0.4.0-SNAPSHOT`. Body: §2.3, with the evidence, and the question of
  which side is right. Recorded in §8.7.
- [ ] **Step 7: Plan the follow-up that publishes newman's reports on the Maven site.** This story
  writes the JSON and JUnit reports to `target/newman/reports/` and nothing reads them after the
  build. Run `dsh-plan-wave` to add a PRD task for publishing them on the Maven site, then
  `dsh-new-story` to open its issue, after the owner approves both. This runs at the end of this
  task, before step 8's PRD reconcile, so the reconcile finds the new issue already placed. How
  the reports reach the site is not decided here. Decide or probe it at this step, with what
  this story's runs have shown. Candidates: an HTML reporter, the failsafe report, or a site
  resource copied from `target/newman/reports/`. Site generation lives in parent-poms, so check
  first whether the change belongs there. Recorded in §8.7.

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: at least one collection runs under `mvn -B install -DintegrationTests` and fails the build when an assertion fails | §3.3, §3.4, Task 4 Step 3, Task 6 Step 1 |
| AC002: the server and containers are torn down when newman fails | §3.1 decision 2, Task 2 Step 3, Task 3 Step 4, Task 6 |
| AC003: `api-testing.yml`'s start, `curl` loop, service containers and `createUser` step are gone | §3.1 decision 1, Task 5 Step 1, Task 7 Step 4 |
| AC004: no "skipping" path; an empty directory fails | §3.3, Task 2 Step 3, Task 6 Step 2, Task 7 Step 4 |
| AC005: nothing activates a profile that matches nothing | §3.1 decision 1, Task 5 Step 1, Task 7 Step 4 |
| Gate 1 and 2: `mvn -B clean install` passes at the 95% gate | Task 7 Step 1 |
| Gate 3 | Task 7 Step 2 |

## 8. Verification results

Runs of 2026-11-08, on Windows 11 with Docker Desktop, logs in `.logs/`. Commits `14d8d4d1b` (Task 2),
`420b552d1` (Task 3), `5d681be67` (Task 4), `7d7fcc56a` and `a09d74d66` (Task 5).

### 8.1 Baseline

`mvn-clean-install-baseline.log`: `maven exit=0`. No per-module `jacoco.csv` is written, so the
figures come from `dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv`, rows of group
`dsh-rest-api`: **LINE 126/129 (97.67%), BRANCH 29/30 (96.67%)**. No `dsh-it-` container before the
first run.

### 8.2 Task 2, red

`mvn-clean-verify-it-red.log`: `maven exit=1`, in `failsafe:verify`, with
`No *.postman_collection.json in C:\Users\marce\github\dsh\dsh-rest-api\..\specs\api\postman. An
empty directory is a failure, not a pass (#137).` `stop-application` (line 1402) and
`stop-it-infrastructure` (1431) follow the failure and precede `failsafe:verify` (1442).
`DocumentResourceHttpIT` 10/10. No `dsh-it-` container afterwards.

### 8.3 Task 3, the order

`mvn-clean-verify-it-tooling.log`: `maven exit=1` with the same message. Order in the log:
`stage-newman` (67), `install-node` (517), `install-newman` (556), `reserve-it-ports` (565),
`start-it-infrastructure` (577), `start-application` (585), then after the failure
`stop-application` (1460), `stop-it-infrastructure` (1489). The declaration-order rule of §3.2 holds.
Node `v24.22.2` came from `node-v24.22.2-win-x64.zip`, cached in
`D:\.m2\repository\com\github\eirslett\node\24.22.2`; `npm ci` added 148 packages.
`target/newman/node/node.exe` and `target/newman/node_modules/newman/bin/newman.js` exist;
`src/test/newman/` holds `package.json` and `package-lock.json` only.

### 8.4 Task 4, green

`mvn-clean-verify-it-green.log`: `maven exit=0`, failsafe 25 tests, 0 failures. newman: 1 iteration,
5 requests, 5 test scripts, 2 pre-request scripts, **10 assertions, 0 failed**, 763 ms. The
blank-title request answered `200` with the expected body, so the empty `title` part was sent and
the single-space fallback was not needed.

Two deviations from Task 4 Step 3's expectations, recorded rather than engineered around:

- **The status request did not repeat.** `/status` already answered `QUEUED_FOR_INDEXING_SUCCESS`
  on the first call, in this run and every later one. The polling path is therefore not exercised
  by these runs; it rests on §2.2's first probe.
- **The failsafe report names the test `everyCollection_shouldPassAgainstTheServer()[1]`**, not the
  collection's file. JUnit's display name is the file name, but failsafe's XML reporter writes the
  method name and index unless `usePhrasedTestCaseMethodName` is set, which would rename every IT in
  the report and is reporter configuration inherited from parent-poms. A failure still names the
  collection, in the assertion message (§8.5); newman's own reports are named after it.

### 8.5 Task 6, the mutations

| Mutation | Log | Result |
|---|---|---|
| 1, `'TOKEN_NOT_FOUNDX'` | `mvn-clean-verify-mutation-1-rerun.log` | `maven exit=1` in `failsafe:verify`. The failure reads `newman's exit code for dsh-rest-api.postman_collection.json. Its output:` and newman's `answers TOKEN_NOT_FOUND and names the token`, `expected 'TOKEN_NOT_FOUND' to equal 'TOKEN_NOT_FOUNDX'`. `stop-application`, then `stop-it-infrastructure` removed the container. No leftover |
| 2, the empty directory | §8.2, §8.3 | as recorded there |
| 3, `zz-empty.postman_collection.json` | `mvn-clean-verify-mutation-3.log` | `maven exit=1`. `[1]` passed; `[2]` failed on `assertions in zz-empty.postman_collection.json: newman passes a collection that asserts nothing`. Teardown as in mutation 1. No leftover |

Each mutation was reverted, and `git status --short` was clean after each.

Two failures in these runs are **not** caused by this story, and are recorded for the owner:

- **Mutation 1's first run** (`mvn-clean-verify-mutation-1.log`) failed in `docker:stop` itself:
  `Cannot create docker access object [\\.\pipe\docker_engine (Todas as instâncias de pipes estão
  ocupadas)]`, all pipe instances busy. `spring-boot:stop` had run; the container leaked and was
  removed by hand. Docker Desktop refused the named-pipe connection; nothing this story changed
  talks to Docker. The re-run, identical, tore down cleanly.
- **Mutation 3's run** also failed `DocumentResourceHttpIT.submit_whenSameFileTwice_shouldReturnTheSameTokenAndQueueItAgain`:
  `did not reach QUEUED_FOR_INDEXING_SUCCESS within 15000 ms; last status was DEQUEUED_FOR_INDEXING`.
  It ran before `PostmanCollectionsIT`. `QUEUED_FOR_INDEXING_SUCCESS.transition(…)` returns
  `DEQUEUED_FOR_INDEXING` for every transition type, so when the two enqueue acks of a resubmitted
  file land in that order, the second moves the document past the status the test waits for. A
  pre-existing race in the resubmission path, beside §2.3 and `#142`; it passed in the other runs.

### 8.6 Task 7, the gates and the checks

| Check | Log | Result |
|---|---|---|
| Gate 1 | `mvn-clean-install.log` | `maven exit=0`. No `frontend` and no `stage-newman` in the log. `dsh-rest-api` LINE 126/129, BRANCH 29/30, equal to §8.1 |
| Gate 3 | `mvn-clean-verify-it.log` | `maven exit=0`, failsafe 25 tests, 0 failures. No `dsh-it-` container afterwards |
| `-DskipITs` | `mvn-clean-verify-skipits.log` | `maven exit=0`. `stage-newman` "Skipping the execution", `install-node` and `install-newman` "Skipping execution". `target/newman/` does not exist |
| AC003 | | `.github/workflows/api-testing.yml` does not exist |
| AC005 | | `grep -rn "spring.profiles.active" .github/` finds nothing |
| AC004 | | `No Postman collections found` is in no `.yml` or `.java` |
| Task 5 Step 8 | | only `profiles-and-configuration.md:711` and `:897`, both naming the workflow as removed. A wider `git grep` found one more live reference the spec had not listed, the usage comment of `scripts/test-credential-step.sh`; fixed in `a09d74d66` |
| markdownlint | | no error |

`scripts/test-credential-step.sh .github/workflows/ci.yml` reports 17 passed, 1 failed, with and
without this story's change: its stubbed-`200` case still expects the parent `3.8.0-SNAPSHOT`, and
the pin is `3.11.2`. Pre-existing, outside this story.

### 8.7 Follow-ups

Approved by the owner on 2026-11-08.

- **Task 1 Step 3.** parent-poms `#90` is retitled "Manage docker-maven-plugin's and
  frontend-maven-plugin's versions", with §1 Step 3's text appended.
- **Task 7 Step 6.** `#185`, "The contract says every submission gets a new token; a resubmitted
  file keeps its token", `bug`, milestone `0.4.0-SNAPSHOT`.
- **§8.5's resubmission race is `#142`**, reproduced: a resubmitted file subscribes a second
  `DocumentEnqueueResponseMessageHandler` for the same stored document, the first ack reaches both,
  and two transitions take it past `QUEUED_FOR_INDEXING_SUCCESS`. Recorded as a comment on `#142`
  rather than a new issue.
- **§8.6's stale script case.** `#186`, `bug`, milestone `0.4.0-SNAPSHOT`.
- **Task 7 Step 7**, the publication of newman's reports on the Maven site: held by the owner to the
  end of the story, before step 8's reconcile.

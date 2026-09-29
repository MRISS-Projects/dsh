---
issue: 46
slug: start-app-around-integration-tests
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 46 — Start the application around the integration-test phase and test it over the wire

## 1. Story

**As a** developer changing the REST API
**I want** the application started and stopped by the Maven lifecycle around the integration-test phase
**So that** integration tests run against the application actually serving HTTP, and the server is torn down
even when they fail

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#46](https://github.com/MRISS-Projects/dsh/issues/46), label `task`
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`, matching the milestone, as `#112` and `#113` did.
- Depends on `MRISS-Projects/parent-poms#67`, closed and released in parent-poms `3.9.0`, which this
  repository pins. Failsafe runs behind `-DintegrationTests`, with a two-goal execution
  (`integration-test` records, `verify` fails) and a second JaCoCo agent writing `jacoco-it.exec`.
- Sibling of `#112`, which moved the in-process Spring-context tests to `*IT`. This story adds the
  first **out-of-process** one.

### 2.1 What was measured before designing

A probe on 2026-09-28 ran the jar built from the RC at `f2ef0eb6a` with no MongoDB and no RabbitMQ:

| Request | Answer |
|---|---|
| boot | `Started DshRestApplication in 6.914 seconds`: both clients connect lazily |
| `GET /actuator/health` | `503 {"status":"DOWN"}` |
| `POST /v1/dsh/document/submit`, blank title | `200 {"token":"ERROR","message":"Error submitting file: Document title and contents can't be null."}` |
| `GET /v1/dsh/document/status/abc` | `500` after 30.04 s: the Mongo driver's server-selection timeout |
| `GET /v2/api-docs`, `GET /v3/api-docs` | `404` |

Two consequences shape the design. `api-testing.yml`'s `curl` loop on `/actuator/health` only
works because that job runs Mongo and Rabbit as service containers, so a health probe is the wrong
readiness signal. And an end-to-end test needs real infrastructure, which Maven has to provide:
the staging build has none, because `parent-poms#78` removed its service containers.

### 2.2 Where integration tests run

- **Pull requests: never.** `ci.yml` runs `mvn -B -U install` without `-DintegrationTests`, and
  `api-testing.yml` builds with `-DskipTests`. This story keeps it that way; end-to-end tests are a
  staging concern.
- **Staging: always.** `project-staging.yml` runs on `ubuntu-latest`, which has Docker, and passes
  `-DintegrationTests`.
- **Locally: on demand**, with `mvn -B clean install -DintegrationTests`, which after this story
  needs a running Docker daemon.

## 3. Decisions taken while specifying

1. **The open question in the issue: newman.** Neither of the issue's options. `api-testing.yml` is
   left untouched (AC007's "deliberately left alone" branch), because end-to-end tests do not run on
   pull requests, and newman has no collections to run. Moving newman into the lifecycle, and
   deciding what becomes of `api-testing.yml`, is follow-up issue `#137` (§8).
2. **Real infrastructure, owned by Maven.** The over-the-wire IT goes end to end through MongoDB and
   RabbitMQ. Both run as Docker containers started and stopped by `dsh-rest-api`'s own POM, so the
   same command works on a developer box and on staging, and no workflow changes.
3. **Only the new IT uses the containers.** The six Spring-context ITs keep their `@MockBean`s. A
   later story can move them onto real infrastructure; this one proves the mechanism.
4. **The API's `200`-for-errors contract is asserted, not changed.** It is follow-up issue
   `#138` (§8).
5. **The test layers become the written rule.** Added at spec review, delivered by Task 8. There are
   four layers, and where integration tests may run is stated with them:

   | Layer | What | Required when | Runs |
   |---|---|---|---|
   | 1. Unit | Mockito, no Spring context, REST entry points included | always; the 95% gate counts only this layer | everywhere, `mvn -B clean install` |
   | 2. Spring context | `*IT`, full or sliced context, `@MockBean` for every external service, MockMvc or Spring's test framework for REST entry points | a story creates or changes a Spring bean class, REST entry points included | staging, deploy, local gate |
   | 3. Over the wire | `*HttpIT`, the application forked by Maven, real ports, real external services in Docker | a story adds a REST endpoint or changes one's contract | staging, deploy, local gate |
   | 4. External client | Postman collections run by newman: a consumer that sees only the API contract, not the internals | when collections exist, `#137` | staging, deploy, local gate |

   - **Layers 2 and 3 share a vantage point.** Both are written by us as client *and* provider.
     Layer 4 is the only one that tests as an outsider, which is why it is needed at all.
   - **"Deploy" is future.** It means a deploy workflow from `DEVELOP`, which does not exist yet.
   - **Never on pull requests.** Layers 2 to 4 do not run on PRs.
   - **The local gate is conditional.** It applies only when a story changes code that touches an
     external system (MongoDB, RabbitMQ, Solr, or any other service outside the JVM) or a REST API
     entry point.
   - **It covers the changed modules and their dependents.** After the root `mvn -B clean install`
     gate passes, the story passes `mvn -B clean verify -DintegrationTests -pl <changed modules> -amd`.
     The changed modules are the ones whose code changed. *Amended at code review:* this first said
     `-pl <affected modules>` without `-amd`, which would have run no IT for a `dsh-data`-only
     change, though #139 shows up only in `dsh-rest-api`.
   - **No `-am`, deliberately.** The root build has already installed every upstream module, and
     `-am` would run their integration tests too.
   - **Going forward only.** The layer-2 requirement is enforced at review, not mechanically. Existing
     beans are not retrofitted.

## 4. Design

### 4.1 The lifecycle

A profile `http-integration-tests` in `dsh-rest-api/pom.xml`, activated by the **same**
`integrationTests` property as parent-poms' failsafe profile. Without `-D` nothing below is bound,
which makes AC002 structural rather than a runtime skip.

| Phase | Goal | Does |
|---|---|---|
| `package` | `build-helper:reserve-network-port` | reserves `dsh.it.http.port` and `dsh.it.jmx.port` |
| `package` | `docker:start` | `mongo:6` and `rabbitmq:3`, each on a random host port exported as `dsh.it.mongo.port` / `dsh.it.rabbitmq.port`; blocks until each logs readiness |
| `pre-integration-test` | `spring-boot:start` | forks the application with the ports as arguments; readiness through its JMX channel on `dsh.it.jmx.port` |
| `integration-test`, `verify` | failsafe (inherited) | the tests receive `dsh.it.baseUrl=http://localhost:${dsh.it.http.port}` |
| `post-integration-test` | `spring-boot:stop`, then `docker:stop` | teardown, reached on a red test because `failsafe:integration-test` does not fail the build |

**Why `package` and not `pre-integration-test`.** Within one phase Maven runs executions in plugin
declaration order. `spring-boot-maven-plugin` is declared in the main `<build>` for `repackage`, so
the profile names a plugin the main build already has. Maven merges such a profile by inserting its
*new* plugins ahead of the first plugin it shares with the main build. *Corrected during the build:*
this spec first said the profile's plugins merge in after, and with `docker-maven-plugin` declared
first, Task 4's log showed `docker:stop` before `spring-boot:stop`. So inside the profile
`spring-boot-maven-plugin` is declared **before** `docker-maven-plugin`, which puts it first in every
phase. Everything bound to `pre-integration-test` would then run `spring-boot:start` before the
containers exist. Binding the port reservation and `docker:start` to `package` puts them first
without moving `repackage`, and `spring-boot:stop` runs before `docker:stop`, the teardown order
wanted. §7.4 proves the order from the build log.

**What `spring-boot:start` runs.** It forks a JVM on `target/classes` plus the runtime classpath.
It does not launch the repackaged jar. The code is the same, but the jar's launcher is not
exercised. The issue's "packaged artifact" is therefore met in substance, not literally, and this
spec says so rather than overclaim.

**Readiness.**

- **MongoDB.** The image runs `/docker-entrypoint-initdb.d` scripts on a temporary `mongod` bound to
  the container's loopback, then restarts it for real, so `Waiting for connections` is logged twice.
  The wait regex is `(?s)init process complete.*Waiting for connections`, which only matches the
  second one. A TCP wait is not used: Docker Desktop's port proxy accepts connections before the
  container listens.
- **RabbitMQ.** It waits on `Server startup complete`.
- **Both** time out at 90 s, and a timeout fails the build.

**The Mongo user.** After §4.2.2, `dsh-data`'s `dshApplicationContext.xml` authenticates as
`${mongo.user}:${mongo.password}` against the `dsh` database, so the user must exist there. (This
spec first assumed it already authenticated. It did not; see §4.2.2.)
`dsh-rest-api/src/test/docker/mongo-init.js` creates `dshuser`/`dshpass` with `readWrite`
on `dsh`, bind-mounted read-only into `/docker-entrypoint-initdb.d`. The entrypoint runs init scripts
whenever that directory is non-empty, so no root user is needed.

**Arguments to the application.** `mongo.properties` is filtered at `dsh-data`'s build time, but
`context:property-placeholder` gives the `Environment` precedence over the file, so command-line
arguments win once §4.2.2 stops the context XML itself being filtered. `api-testing.yml` never proved
this: it passes the same values the build baked in. The arguments are `--server.port`,
`--mongo.host`, `--mongo.port`, `--mongo.user`, `--mongo.password`, `--spring.rabbitmq.host` and
`--spring.rabbitmq.port`.

**Container names** follow `dsh-it-%a-%t` (alias, timestamp), so AC005's leak check can filter on
`name=dsh-it-`, and two concurrent builds on one box do not collide.

### 4.2 The production changes

The spec was approved with one, the RabbitMQ port. The build found the second, the MongoDB
connection, when the containers first ran (§7.3); it was added with the human's approval.

#### 4.2.1 The RabbitMQ port

`dsh-rest-api/src/main/resources/enqueue-docId-context.xml` declares
`<rabbit:connection-factory … host="localhost" …/>` and no port, so it always dials 5672. With the
broker on a random port that makes AC006 impossible. The change:

```xml
<rabbit:connection-factory id="connectionFactory"
    host="${spring.rabbitmq.host:localhost}" port="${spring.rabbitmq.port:5672}"
    confirm-type="CORRELATED" publisher-returns="true" />
```

The defaults reproduce today's behaviour byte for byte. Placeholders in an `@ImportResource` XML are
resolved from the Boot `Environment`. The property names are Boot's own, so the familiar
`spring.rabbitmq.*` keys now reach this factory. The dequeue side lives in other modules and is out
of scope.

`dsh-rest-api` filters `src/main/resources`, but Maven reads `spring.rabbitmq.host:localhost` as
one property name, which is never defined, so the placeholders reach `target/classes` intact
(checked there).

#### 4.2.2 The MongoDB connection

With both containers up, the forked application still dialled `localhost:27017`. Two defects in
`dsh-data`, both older than this story:

- **The context XML was filtered at build time.** `dsh-data/pom.xml` excludes the Spring context
  from filtering by name, and the name was `applicationContext.xml`, a file that no longer exists.
  So `dshApplicationContext.xml` was filtered, and `${mongo.host}`, `${mongo.port}` and the
  credentials were baked in as literals. No runtime argument could change them. Shown by the
  packaged jar's copy (`host="localhost" port="27017"`), and by a probe with `--mongo.port=11111`
  that still dialled 27017.
- **The credentials were never used.** The element carried `credentials="user:password@dsh"`.
  spring-data-mongodb 3.4.18's `MongoClientParser` reads `port`, `host`, `credential`, `replica-set`
  and `connection-string` (from its bytecode). It does not read `credentials`, so the client
  connected unauthenticated. The pinned `spring-mongo-2.0.xsd` still declared the old attribute, so
  nothing failed.

The change:

- `dsh-data/pom.xml` names `dshApplicationContext.xml` in both the filtered resource's exclude and
  the unfiltered resource's include;
- the context uses `connection-string="mongodb://${mongo.user}:${mongo.password}@${mongo.host}:${mongo.port}/dsh"`,
  and its schema location becomes the versionless `spring-mongo.xsd` (3.3 in this jar), because the
  2.0 schema rejects `connection-string`.

The probe with `--mongo.port=11111` then dialled `localhost:11111`. Every consumer of `dsh-data`
now authenticates. That matters wherever MongoDB runs without the user: `api-testing.yml` creates
`dshuser`, and `mongo.properties`'s values come from each environment's Maven settings. A password
containing `@`, `:` or `/` would need percent-encoding in the connection string. The defect is
recorded as its own bug issue, #139 (§8).

### 4.3 The IT

`dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java`, JUnit 4.

- **No Spring context in the test JVM.** It is a `RestTemplate` client against the forked server.
  `spring-web` is already on the classpath, so multipart needs no new dependency.
- **The error handler never throws**, so a `4xx` or `5xx` is asserted as a status code rather than
  surfacing as an exception.
- **The base URL comes from `dsh.it.baseUrl` only.** When it is absent, for example when the class is
  run from an IDE outside the lifecycle, every test fails at once with a message naming
  `-DintegrationTests`. It never falls back to `localhost:8080`.

| Test | Request | Asserts |
|---|---|---|
| `submit_whenValidPdf_shouldQueueForIndexing` | `POST /submit`, title and `bbc-news-1.pdf` | `200`, JSON, token not `ERROR`; then polls `GET /status/{token}` every 250 ms for up to 15 s until `QUEUED_FOR_INDEXING_SUCCESS`, failing at once on `QUEUED_FOR_INDEXING_ERROR` |
| `status_whenTokenUnknown_shouldReturnTokenNotFound` | `GET /status/{random UUID}` | `200`, JSON, `status=TOKEN_NOT_FOUND`, `message=TOKEN_NOT_FOUND_MESSAGE + token` |
| `submit_whenTitleBlank_shouldReturnErrorToken` | `POST /submit`, blank title | `200`, JSON, `token=ERROR`, the exact message from §2.1 |
| `submit_whenContentsMissing_shouldReturnBadRequest` | `POST /submit`, no `contents` part | `400` |

The first test proves the whole cycle: Mongo write, Rabbit publish, **broker ack**, Mongo update,
Mongo read. `QUEUED_FOR_INDEXING_SUCCESS` is only reached when
`DocumentEnqueueResponseMessageHandler` receives the publisher confirm and stores the transition.
No consumer is needed: the broker acks once the message is routed to `si.test.queue`, which
`rabbit:admin` declares.

**Known hazard, not fixed here.** `DocumentSubmissionServiceImpl` is a singleton holding the
in-flight `document` in a field, so concurrent submissions race. The IT submits once and never in
parallel; failsafe runs the classes of this module in one JVM, sequentially.

### 4.4 Known limits, recorded rather than engineered around

- **A failure before the tests leaks containers.** If `spring-boot:start` itself fails, the build
  ends in `pre-integration-test`, so `post-integration-test` never runs and the containers survive.
  Random ports mean a leak cannot break the next build. Staging runners are discarded. Locally they
  go with `docker rm -f $(docker ps -aq --filter name=dsh-it-)`, and the testing-patterns rule says
  so. AC005 is about a failing *test*, and that path is covered.
- **The forked server is not measured.** `jacoco-it.exec` covers the failsafe JVM only. Adding an
  agent to the fork is not needed by any AC.
- **Docker becomes a prerequisite of `-DintegrationTests`.** `mvn -B clean install`, the gate, is
  unaffected.
- **The reserved ports can be taken.** *Added at code review.* `build-helper` releases the HTTP and
  JMX ports as soon as it has chosen them, and `spring-boot:start` binds them 15–20 s later, after
  the containers are up. Another process can take one in between, and the start then fails. The
  window is short and the ports random, so the risk is accepted.
- **The image tags float.** *Added at code review.* `mongo:6` and `rabbitmq:3` resolve to the newest
  matching image, and staging pulls on every run. An upstream image change, or a truncated pull as
  in §7.3, can turn staging red with no change here. Accepted for now: pinning digests would trade
  that for a maintenance chore nobody has asked for.

### 4.5 The `docker-maven-plugin` version, pinned locally for now

CLAUDE.md puts plugin versions in parent-poms' `pluginManagement`, and `io.fabric8:docker-maven-plugin`
is not managed there. The standing hold is no parent-poms work until DSH `0.3.0` ships, and this
story is on the `0.3.0` milestone. **Decided at spec review:** `<version>0.49.0</version>` is
declared in `dsh-rest-api`'s profile, with a comment pointing at
[parent-poms#90](https://github.com/MRISS-Projects/parent-poms/issues/90). That issue is worked
after `0.3.0` ships: it moves the version into `pluginManagement`, and the re-pin deletes the local
one.

### 4.6 Out of scope

- `api-testing.yml`, newman, Postman collections: `#137`.
- `4xx` status codes for the API's own validation failures: `#138`.
- Moving the Spring-context ITs onto real infrastructure.
- `#45`'s actuator configuration. Readiness no longer depends on it.

## 5. Files to change

| File | Change |
|---|---|
| `dsh-rest-api/pom.xml` | the `http-integration-tests` profile (§4.1) |
| `dsh-rest-api/src/test/docker/mongo-init.js` | new: creates `dshuser` in `dsh` |
| `dsh-rest-api/src/main/resources/enqueue-docId-context.xml` | host and port placeholders (§4.2.1) |
| `dsh-data/pom.xml` | the context XML excluded from filtering by its real name (§4.2.2) |
| `dsh-data/src/main/resources/dshApplicationContext.xml` | a connection string, the versionless schema (§4.2.2) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java` | new (§4.3) |
| `.github/copilot/rules/testing-patterns.md` | an "Over the wire" subsection (Task 7), then restructured around the four test layers (Task 8) |
| `.github/copilot/rules/java-conventions.md` | its unit-test line points at the layers (Task 8) |
| `.github/copilot/prompts/test-generation.md` | templates aligned with layers 1 and 2 (Task 8) |
| `.github/copilot-instructions.md` | its testing bullets point at the layers; Postman is layer 4, not in force (Task 8, Step 5) |
| `CLAUDE.md` | *Quality gates*: `-DintegrationTests` needs Docker (Task 7), a pointer to the layers and the local integration gate (Task 8) |
| `.claude/skills/dsh-ship-story/SKILL.md` | runs the conditional, per-module integration gate in step 5 (Task 8) |

`api-testing.yml` is deliberately absent (§3.1).

## 6. Tasks

All Maven runs follow CLAUDE.md: log to `.logs/`, print the `tail` command, `wait`, report the exit
code. Every gate run includes `clean`.

### Task 1 — Baseline

- [x] **Step 1.** `docker ps -a --filter name=dsh-it- --format '{{.Names}}'` prints nothing.
- [x] **Step 2.** `mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1`, exit `0`.
- [x] **Step 3.** Record `dsh-rest-api`'s LINE and BRANCH from
      `dsh-rest-api/target/site/jacoco/jacoco.csv` (sum `LINE_MISSED`/`LINE_COVERED` and
      `BRANCH_MISSED`/`BRANCH_COVERED` over all rows) into §7.1.

### Task 2 — The IT, red

**Files:** create `DocumentResourceHttpIT.java`.

- [x] **Step 1.** Write the class:

```java
package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mriss.dsh.data.models.DocumentStatus;
import com.mriss.dsh.restapi.rest.DocumentResource;

/**
 * Calls the REST API over HTTP against the server that the {@code http-integration-tests} profile
 * starts, backed by the MongoDB and RabbitMQ containers it starts. No Spring context runs in this
 * JVM: the application under test is a separate process.
 */
public class DocumentResourceHttpIT {

    private static final String BASE_URL_PROPERTY = "dsh.it.baseUrl";

    private static final File FIXTURE = new File("target/test-classes/pdf/bbc-news-1.pdf");

    private static final long STATUS_TIMEOUT_MILLIS = 15_000;

    private static final long STATUS_POLL_MILLIS = 250;

    private static final ParameterizedTypeReference<Map<String, Object>> JSON =
            new ParameterizedTypeReference<Map<String, Object>>() { };

    private RestTemplate rest;

    private String baseUrl;

    @Before
    public void setUp() {
        baseUrl = System.getProperty(BASE_URL_PROPERTY);
        if (baseUrl == null) {
            fail(BASE_URL_PROPERTY + " is not set. Run this class under "
                    + "'mvn -B install -DintegrationTests', which starts the server it calls.");
        }
        rest = new RestTemplate();
        rest.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    @Test
    public void submit_whenValidPdf_shouldQueueForIndexing() throws InterruptedException {
        ResponseEntity<Map<String, Object>> submitted = submit("Russia-Trump: FBI chief Wray defends agency", true);

        assertJson(submitted, HttpStatus.OK);
        String token = (String) submitted.getBody().get("token");
        assertThat(token).isNotBlank().isNotEqualTo("ERROR");

        String expected = DocumentStatus.QUEUED_FOR_INDEXING_SUCCESS.getStatusDescription();
        String failed = DocumentStatus.QUEUED_FOR_INDEXING_ERROR.getStatusDescription();
        long deadline = System.currentTimeMillis() + STATUS_TIMEOUT_MILLIS;
        String last = null;
        while (System.currentTimeMillis() < deadline) {
            ResponseEntity<Map<String, Object>> status = status(token);
            assertJson(status, HttpStatus.OK);
            last = (String) status.getBody().get("status");
            if (expected.equals(last)) {
                return;
            }
            assertThat(last).as("the broker rejected or never confirmed the enqueue").isNotEqualTo(failed);
            Thread.sleep(STATUS_POLL_MILLIS);
        }
        fail("document " + token + " did not reach " + expected + " within "
                + STATUS_TIMEOUT_MILLIS + " ms; last status was " + last);
    }

    @Test
    public void status_whenTokenUnknown_shouldReturnTokenNotFound() {
        String token = UUID.randomUUID().toString();

        ResponseEntity<Map<String, Object>> response = status(token);

        assertJson(response, HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("status", DocumentResource.TOKEN_NOT_FOUND)
                .containsEntry("message", DocumentResource.TOKEN_NOT_FOUND_MESSAGE + token);
    }

    @Test
    public void submit_whenTitleBlank_shouldReturnErrorToken() {
        ResponseEntity<Map<String, Object>> response = submit("", true);

        assertJson(response, HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("token", "ERROR")
                .containsEntry("message", "Error submitting file: Document title and contents can't be null.");
    }

    @Test
    public void submit_whenContentsMissing_shouldReturnBadRequest() {
        ResponseEntity<Map<String, Object>> response = submit("A title", false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> submit(String title, boolean withContents) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("title", title);
        if (withContents) {
            form.add("contents", new FileSystemResource(FIXTURE));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange(baseUrl + "/v1/dsh/document/submit", HttpMethod.POST,
                new HttpEntity<>(form, headers), JSON);
    }

    private ResponseEntity<Map<String, Object>> status(String token) {
        return rest.exchange(baseUrl + "/v1/dsh/document/status/" + token, HttpMethod.GET, null, JSON);
    }

    private static void assertJson(ResponseEntity<?> response, HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(MediaType.APPLICATION_JSON.isCompatibleWith(response.getHeaders().getContentType())).isTrue();
    }
}
```

- [x] **Step 2.** `mvn -B clean install -DintegrationTests -pl dsh-rest-api -am -Dit.test=DocumentResourceHttpIT -Dit.failIfNoSpecifiedTests=false`
      to `.logs/mvn-clean-install-it-task2.log`. Expected: exit non-zero, all four tests failing
      with `dsh.it.baseUrl is not set`. Record in §7.2.
- [x] **Step 3.** Commit: `test(#46): add the over-the-wire IT, red without a server`.

### Task 3 — Start and stop the application, no containers yet

**Files:** `dsh-rest-api/pom.xml`.

- [x] **Step 1.** Add the profile with the `build-helper`, `spring-boot` and `failsafe` parts of
      §6.1's final XML, leaving the `docker-maven-plugin` block and the Mongo/Rabbit arguments out.
- [x] **Step 2.** Same command as Task 2, log `.logs/mvn-clean-install-it-task3.log`. Expected:
      `submit_whenTitleBlank…` and `submit_whenContentsMissing…` **green** over real HTTP.
      `status_whenTokenUnknown…` is **red** with `500` after ~30 s, and `submit_whenValidPdf…` is
      **red**; there is no infrastructure. The log shows `spring-boot:stop` after the red tests.
      Record in §7.2.
- [x] **Step 3.** Commit: `build(#46): start and stop the application around integration tests`.

### Task 4 — The containers

**Files:** `dsh-rest-api/pom.xml`, create `dsh-rest-api/src/test/docker/mongo-init.js`:

```js
// Creates the user dsh-data authenticates as (dshApplicationContext.xml: <user>:<password>@dsh).
// Mounted into /docker-entrypoint-initdb.d by the http-integration-tests profile of dsh-rest-api.
db.getSiblingDB('dsh').createUser({
  user: 'dshuser',
  pwd: 'dshpass',
  roles: [{ role: 'readWrite', db: 'dsh' }]
});
```

- [x] **Step 1.** Add the `docker-maven-plugin` block and the Mongo/Rabbit arguments, completing
      §6.1's XML. **The bind mount is the one untested piece on Windows.** If `docker:start` rejects
      the `C:\…` host path, or the user is missing afterwards, drop the `<volumes>` block and
      bake the script into an image instead: add `dsh-rest-api/src/test/docker/mongo/Dockerfile`
      holding `FROM mongo:6` and `COPY mongo-init.js /docker-entrypoint-initdb.d/`, move
      `mongo-init.js` beside it, and give the `mongo` image a
      `<build><contextDir>${project.basedir}/src/test/docker/mongo</contextDir></build>` with
      `<name>dsh-it-mongo:${project.version}</name>`. `docker:start` builds it on demand. Record
      which form shipped, and why, in §7.3.
- [x] **Step 2.** Same command, log `.logs/mvn-clean-install-it-task4.log`. Expected:
      `status_whenTokenUnknown…` **green**, a real Mongo miss. `submit_whenValidPdf…` is **red** at
      `QUEUED_FOR_INDEXING_ERROR`, because the application still dials RabbitMQ on 5672 and the
      broker sits on a random port. If something already listens on 5672 locally, stop it for this
      run; otherwise this red turns falsely green. Record in §7.3.
- [x] **Step 3.** Commit: `build(#46): run MongoDB and RabbitMQ containers around integration tests`.

### Task 5 — The RabbitMQ port, green

**Files:** `enqueue-docId-context.xml` (§4.2.1).

- [x] **Step 1.** Apply §4.2.1.
- [x] **Step 2.** Same command, log `.logs/mvn-clean-install-it-task5.log`: all four **green**.
- [x] **Step 3.** `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`, exit `0`. The
      placeholder defaults keep the six Spring-context ITs and every unit test unchanged.
- [x] **Step 4.** Commit: `fix(#46): let the RabbitMQ host and port be configured`.

### Task 6 — Acceptance evidence

Nothing here is committed except §7.

- [x] **AC002.** In `.logs/mvn-clean-install.log` from Task 5, `grep -c
      'spring-boot-maven-plugin:.*:start\|docker-maven-plugin\|reserve-network-port'` is `0`, and
      `docker ps -a --filter name=dsh-it-` is empty.
- [x] **AC001 / AC003.** `mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it.log`
      for the whole reactor, exit `0`. Record the ordered `grep -n` of `reserve-network-port`,
      `docker-maven-plugin.*start`, `spring-boot-maven-plugin.*start`, `maven-failsafe-plugin`,
      `spring-boot-maven-plugin.*stop` and `docker-maven-plugin.*stop`.
- [x] **AC004.** Two mutations, each run with `-Dit.test=DocumentResourceHttpIT`, each reverted and
      the revert confirmed by `git diff --exit-code`:
  - In `DocumentResource.validateParameters`, replace `StringUtils.isBlank(title)` with `false`.
    Expected: `submit_whenTitleBlank…` red.
  - In `DocumentResource.getStatus`, replace `d == null` with `false`. Expected:
    `status_whenTokenUnknown…` red, `500`.
- [x] **AC005.** Change `submit_whenTitleBlank…`'s expected token to `"NOT-ERROR"`, run the whole
      reactor with `-DintegrationTests`. Expected: exit non-zero from `failsafe:verify`, with
      `spring-boot:stop` and `docker:stop` both in the log before it. Then:
      - `jps -l` lists no `com.mriss.dsh.restapi.DshRestApplication`;
      - `netstat -ano | grep ":<dsh.it.http.port> "` is empty, with the port read from the log;
      - `docker ps -a --filter name=dsh-it-` is empty.
      Revert and confirm with `git diff --exit-code`.
- [x] **AC006.** `git diff staging-0.3.0-SNAPSHOT-RC -- dsh-rest-api | grep -nE '8080|27017|5672'`
      matches only the container-side ports in `<port>…:27017</port>` / `<port>…:5672</port>`, and
      the `:5672` fallback in `enqueue-docId-context.xml`. None of them is a port the host binds.
- [x] **AC007.** `git diff --stat staging-0.3.0-SNAPSHOT-RC -- .github/workflows` is empty; the
      reason is §3.1.
- [x] **AC008.** Task 5's `jacoco.csv` gives the same LINE and BRANCH for `dsh-rest-api` as Task 1.

### Task 7 — Documentation

- [x] **Step 1.** `testing-patterns.md`, after *Integration Tests (`@SpringBootTest`)*, add
      *Over the wire*:
  - what distinguishes it from `RANDOM_PORT`/MockMvc ITs: a separate process, real MongoDB and
    RabbitMQ, no Spring context in the test JVM;
  - that the URL comes from `dsh.it.baseUrl` and a port is never hardcoded;
  - that it needs Docker;
  - the leaked-container cleanup command from §4.4.

  Update the `RANDOM_PORT` bullet so the two are not confused.
- [x] **Step 2.** `CLAUDE.md`, *Quality gates* item 1: after "run under `mvn -B clean install
      -DintegrationTests`", add that this now starts MongoDB and RabbitMQ in Docker for
      `dsh-rest-api` and needs a running Docker daemon.
- [x] **Step 3.** The markdown-lint command from CLAUDE.md passes.
- [x] **Step 4.** Commit: `docs(#46): document over-the-wire integration tests`.

### Task 8 — The test layers

Documentation only; the content is §3.5. `testing-patterns.md` is the single source of truth.
CLAUDE.md is a router and does not restate standards, so it gets a pointer and the gate, not the
table.

- [x] **Step 1.** `testing-patterns.md`:
  - replace the three-item list at the top (`:7-9`) with §3.5's table and its bullets;
  - the *API / E2E* entry becomes layer 4, owned by `#137`;
  - rename *Integration Tests (`@SpringBootTest`)* to *Layer 2: Spring context*;
  - rewrite its "optional by design" line (`:113`) as §3.5's layer-2 trigger;
  - rename Task 7's *Over the wire* to *Layer 3: over the wire* and state the `*HttpIT` suffix;
  - add *Layer 4: external client*: the contract-only vantage point, not yet in force, `#137`;
  - add *Where integration tests run*: staging, deploy (future), local gate, never on pull
    requests.
- [x] **Step 2.** `java-conventions.md:60`: keep the one-line unit rule and add "see the test layers
      in `testing-patterns.md`". Do not copy the table.
- [x] **Step 3.** `.github/copilot/prompts/test-generation.md`:
  - the unit template states that REST entry points get a Mockito unit test (layer 1), with
    `MockMvcBuilders.standaloneSetup` and no context;
  - the *Controller Slice Test* template is labelled layer 2 and names the class `*IT` in the
    `integration` package.
- [x] **Step 4.** `CLAUDE.md`, *Quality gates*:
  - add a third, conditional gate, worded as in §3.5:
    - when it applies: code touching an external system or a REST API entry point;
    - what runs: `mvn -B clean verify -DintegrationTests -pl <affected modules>`, after the root
      `clean install`;
    - why there is no `-am`;
  - add the same gate to `dsh-ship-story`'s step-5 checks (`.claude/skills/dsh-ship-story/`), so
    the process runs it rather than only documenting it;
  - add one sentence pointing at `testing-patterns.md` for the four layers;
  - keep the section's existing statement that `jacoco-it.exec` is never read by the coverage gate.
- [x] **Step 5.** Grep the touched files for `optional by design` and `Postman collections in` and
      confirm no stale statement survives outside layer 4's description. Run the markdown-lint
      command from CLAUDE.md.
- [ ] **Step 6.** Comment on `#137` that it delivers layer 4 of the rule in `testing-patterns.md`,
      linking the commit. Commit: `docs(#46): state the four test layers and where they run`.

### 6.1 The profile, final form

Appended to `dsh-rest-api/pom.xml`, replacing the commented `<!-- <profiles> -->` placeholder. The
POM indents with tabs; the block below uses spaces only because markdownlint forbids tabs.

```xml
<profiles>
    <!-- #46: keyed on the same property as parent-poms' failsafe profile, so nothing here is bound by
         a plain `mvn install`. Port reservation and containers bind to `package`, not
         `pre-integration-test`, because within a phase executions run in plugin order and
         spring-boot-maven-plugin precedes docker-maven-plugin. That order comes from this profile,
         not the main build: Maven inserts a profile's new plugins ahead of the first plugin the
         profile shares with the main build, so spring-boot-maven-plugin must stay declared before
         docker-maven-plugin here, or docker:stop runs before spring-boot:stop. -->
    <profile>
        <id>http-integration-tests</id>
        <activation>
            <property>
                <name>integrationTests</name>
            </property>
        </activation>
        <properties>
            <!-- -DskipITs skips the containers and the application along with the tests. -DskipTests
                 alone still starts them for nothing: add -DskipITs, or leave -DintegrationTests off. -->
            <skipITs>false</skipITs>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>build-helper-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>reserve-it-ports</id>
                            <phase>package</phase>
                            <goals>
                                <goal>reserve-network-port</goal>
                            </goals>
                            <configuration>
                                <portNames>
                                    <portName>dsh.it.http.port</portName>
                                    <portName>dsh.it.jmx.port</portName>
                                </portNames>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>start-application</id>
                            <phase>pre-integration-test</phase>
                            <goals>
                                <goal>start</goal>
                            </goals>
                            <configuration>
                                <skip>${skipITs}</skip>
                                <jmxPort>${dsh.it.jmx.port}</jmxPort>
                                <maxAttempts>120</maxAttempts>
                                <arguments>
                                    <argument>--server.port=${dsh.it.http.port}</argument>
                                    <argument>--mongo.host=localhost</argument>
                                    <argument>--mongo.port=${dsh.it.mongo.port}</argument>
                                    <!-- Must match the user src/test/docker/mongo-init.js creates. -->
                                    <argument>--mongo.user=dshuser</argument>
                                    <argument>--mongo.password=dshpass</argument>
                                    <argument>--spring.rabbitmq.host=localhost</argument>
                                    <argument>--spring.rabbitmq.port=${dsh.it.rabbitmq.port}</argument>
                                </arguments>
                            </configuration>
                        </execution>
                        <execution>
                            <id>stop-application</id>
                            <phase>post-integration-test</phase>
                            <goals>
                                <goal>stop</goal>
                            </goals>
                            <configuration>
                                <skip>${skipITs}</skip>
                                <jmxPort>${dsh.it.jmx.port}</jmxPort>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>io.fabric8</groupId>
                    <artifactId>docker-maven-plugin</artifactId>
                    <!-- Not yet managed by parent-poms; see parent-poms#90. -->
                    <version>0.49.0</version>
                    <configuration>
                        <skip>${skipITs}</skip>
                        <containerNamePattern>dsh-it-%a-%t</containerNamePattern>
                        <images>
                            <image>
                                <alias>mongo</alias>
                                <name>mongo:6</name>
                                <run>
                                    <ports>
                                        <port>127.0.0.1:dsh.it.mongo.port:27017</port>
                                    </ports>
                                    <volumes>
                                        <bind>
                                            <volume>${project.basedir}/src/test/docker/mongo-init.js:/docker-entrypoint-initdb.d/mongo-init.js:ro</volume>
                                        </bind>
                                    </volumes>
                                    <wait>
                                        <!-- The init scripts run on a temporary mongod that also logs this line. -->
                                        <log>(?s)init process complete.*Waiting for connections</log>
                                        <time>90000</time>
                                    </wait>
                                </run>
                            </image>
                            <image>
                                <alias>rabbitmq</alias>
                                <name>rabbitmq:3</name>
                                <run>
                                    <ports>
                                        <port>127.0.0.1:dsh.it.rabbitmq.port:5672</port>
                                    </ports>
                                    <wait>
                                        <log>Server startup complete</log>
                                        <time>90000</time>
                                    </wait>
                                </run>
                            </image>
                        </images>
                    </configuration>
                    <executions>
                        <execution>
                            <id>start-it-infrastructure</id>
                            <phase>package</phase>
                            <goals>
                                <goal>start</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>stop-it-infrastructure</id>
                            <phase>post-integration-test</phase>
                            <goals>
                                <goal>stop</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <dsh.it.baseUrl>http://localhost:${dsh.it.http.port}</dsh.it.baseUrl>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

`maxAttempts` 120 at the default 500 ms `wait` gives the application 60 s. The probe booted it in
7 s, and a cold staging runner needs the margin.

## 7. Verification

Filled in during the build step (`dsh-build-story`) on 2026-09-28. All runs are on Windows 11 with
Docker Desktop (engine 29.8.0, Linux containers).

### 7.1 Baseline and result (Tasks 1, 5, 6: AC008)

`mvn -B clean install` does not write `dsh-rest-api/target/site/jacoco/jacoco.csv`; nothing binds
`jacoco:report` there. Both figures come from `mvn -B -pl <module> jacoco:report` run straight after
the gate, on the same `jacoco.exec` that `jacoco:check` had just read.

| Module | Task 1, baseline at `26e056e3d` | Task 5, after `3fc8c05b8` |
|---|---|---|
| `dsh-rest-api` | LINE 142/145, BRANCH 35/36 | LINE 142/145, BRANCH 35/36 |
| `dsh-data` | not recorded | LINE 235/242, BRANCH 81/82 |

`dsh-rest-api` is identical. `dsh-data` joined the story after the baseline (§4.2.2), so it has no
Task 1 figure; its change is XML and POM only, no Java, and it passes the gate. Both gate runs exited
`0`. AC008 holds.

### 7.2 Red, then partly green (Tasks 2, 3)

- **Task 2.** Exit `1`. All four tests failed in `setUp` with `dsh.it.baseUrl is not set`.
- **Task 3.** Exit `1`. `spring-boot:start` booted the application in 6.2 s.
  - `submit_whenTitleBlank…` and `submit_whenContentsMissing…` were green over real HTTP.
  - `status_whenTokenUnknown…` was red with `500` after 30.03 s.
  - `submit_whenValidPdf…` was red with `500` after 30.24 s. The submit returned `200`, and the
    first status poll hit the Mongo timeout.
  - `spring-boot:stop` ran after the red tests.
- **Pre-existing, not changed here:** `spring-boot:repackage` runs twice in `dsh-rest-api`, as
  `repackage` from parent-poms' `pluginManagement` and `default` from this module. The baseline log
  shows it too.

### 7.3 Containers (Task 4)

- **The first attempt** failed pulling `mongo:6` with `short read: expected 246738406 bytes but got
  240294146: unexpected EOF`. That was a network truncation. A manual `docker pull` of both images
  cleared it. A cold staging runner pulls on every build and can meet the same failure.
- **The bind mount works on Windows**, so the `<volumes>` form shipped, not the baked image.
  - Mongo was ready in 4.1 s to 7.2 s, and RabbitMQ in about 11.3 s, across runs.
  - The init regex matched the second `Waiting for connections`.
- **Run 2 was red for a reason this spec had not foreseen.** `status_whenTokenUnknown…` was still
  `500` after 30 s, and the log showed the driver dialling `localhost:27017`. The cause is §4.2.2:
  `dsh-data`'s context XML was filtered at build time.
  - An actuator probe of the running jar with `--mongo.port=11111` found the `mongoClient` bean
    defined by the XML. `Environment` resolved `mongo.port=11111` from `commandLineArgs`, and the
    driver still tried `localhost:27017`.
  - **The human chose to fix it in this story.** That became commit `4e1c8c0c2`, and the same probe
    then dialled `localhost:11111`.
- **Run 2 also showed a teardown-order defect.** `docker:stop` ran before `spring-boot:stop`, the
  declaration-order error corrected in §4.1.
- **Run 3** matched this task's expectation:
  - `status_whenTokenUnknown…` green, a real Mongo miss;
  - `submit_whenValidPdf…` red at `QUEUED_FOR_INDEXING_ERROR`, because the broker was not on 5672;
  - `spring-boot:stop`, then `docker:stop`.

### 7.4 Lifecycle order (Task 6: AC001, AC002, AC003)

**AC002.** In Task 5's `.logs/mvn-clean-install.log`, a plain `mvn -B clean install`, the count of
`spring-boot:…:start`, `docker:…:` and `reserve-network-port` lines is `0`.
`docker ps -a --filter name=dsh-it-` is empty.

**AC001 and AC003.** `mvn -B clean install -DintegrationTests` on the whole reactor exited `0`. The
lifecycle lines of `.logs/mvn-clean-install-it.log`, in order:

| Line | Execution |
|---|---|
| 593 | `build-helper:3.6.1:reserve-network-port (reserve-it-ports)` |
| 603 | `docker:0.49.0:start (start-it-infrastructure)` |
| 614 | `spring-boot:2.7.18:start (start-application)` |
| 691 | `failsafe:3.5.5:integration-test (integration-tests)` |
| 1174 | `spring-boot:2.7.18:stop (stop-application)` |
| 1247 | `docker:0.49.0:stop (stop-it-infrastructure)` |
| 1259 | `failsafe:3.5.5:verify (integration-tests)` |

`DocumentResourceHttpIT` ran 4 tests, all green. The Spring-context ITs of every module were green
beside it: `DocumentResourceIT` 6, `DshRestApplicationIT` 2, and one IT class in each worker module.

### 7.5 Mutations (Task 6: AC004)

The mutation runs are `mvn -B clean verify -DintegrationTests -pl dsh-rest-api
-Dit.test=DocumentResourceHttpIT`, with `-Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false`.
Without those two flags, the unit tests fail on the same mutation and the build stops before the ITs.
Both mutations exited `1`, and each was reverted with `git diff --exit-code` clean.

| Mutation | Expected red | Also red |
|---|---|---|
| `validateParameters`: `StringUtils.isBlank(title)` → `false` | `submit_whenTitleBlank…` | `submit_whenValidPdf…` |
| `getStatus`: `d == null` → `false` | `status_whenTokenUnknown…`, `500` | `submit_whenValidPdf…` |

Both collateral reds are caused by the mutation, not by flakiness:

- **Mutation 1** lets the blank-title request enqueue the same PDF a second time. The extra broker
  ack advanced the stored document past `QUEUED_FOR_INDEXING_SUCCESS` to `DEQUEUED_FOR_INDEXING`
  before the next poll.
- **Mutation 2:** the first status poll arrives before the async store finishes, so it reaches the
  mutated null path too.

Unmutated, `submit_whenValidPdf…` was green in every run from Task 5 onwards.

### 7.6 Teardown on red (Task 6: AC005)

`submit_whenTitleBlank…`'s expected token was changed to `"NOT-ERROR"`, and the whole reactor was
run with `-DintegrationTests` into `.logs/mvn-clean-install-it-ac005.log`. It exited `1`, from
`failsafe:3.5.5:verify` on `dsh-rest-api` (line 1322).

- **Teardown ran first:** `spring-boot:stop` at line 1237, and `docker:stop` at line 1310, which
  logged `Stop and removed container` for both.
- **No process is left:** `jps -l` lists no `DshRestApplication`.
- **No listener is left.** The reserved HTTP port was 54500, and `netstat -ano | grep ":54500 "`
  shows no `LISTENING` socket. It showed one `TIME_WAIT` line, `127.0.0.1:54500 ↔ 127.0.0.1:54512`:
  the kernel's remnant of a closed client connection, not a server. The spec's "empty" was too
  strict, and this is recorded rather than waited out.
- **No container is left:** `docker ps -a --filter name=dsh-it-` is empty.

The test was reverted, with `git diff --exit-code` clean.

**AC006.** `git diff staging-0.3.0-SNAPSHOT-RC -- dsh-rest-api dsh-data`, grepped for
`8080|27017|5672` on added lines, matches three:

- `dsh.it.mongo.port:27017`;
- `dsh.it.rabbitmq.port:5672`;
- the `:5672` fallback in `enqueue-docId-context.xml`.

The first two are container-side ports. No host port is hardcoded.

**AC007.** `git diff --stat staging-0.3.0-SNAPSHOT-RC -- .github/workflows` is empty.

### 7.7 Code review round (step 5)

The review of `26e056e3d..354665f51` returned **With fixes**, with no critical findings. The human
chose what to act on; each finding was checked against the code first.

| Finding | Outcome |
|---|---|
| Important: gate 3's "changed modules, no `-am`" misses consumers of shared code | `-pl <changed modules> -amd` in CLAUDE.md, `testing-patterns.md`, `dsh-ship-story` and §3.5 (`a8a8d1d7b`) |
| Important: build-story, ship-story, pr-cycle and the process doc still called one command "the whole gate" | scoped to gates 1 and 2, with gate 3 named where it applies (`8daaed202`) |
| Important: a password with URI-reserved characters now breaks the connection string | stated in the README source and beside the connection string (`859ba5992`) |
| Minor: `-DskipITs` still started containers and the application; the leak list was too short | `<skip>${skipITs}</skip>` on all four goals, red then green (`38dce73d8`) |
| Minor: `RestTemplate` without timeouts | connect 5 s, read 60 s (`98556a977`) |
| Minor: container ports on `0.0.0.0` | `127.0.0.1:` bindings; `docker ps` showed `127.0.0.1:54669->27017/tcp` and `127.0.0.1:54671->5672/tcp` (`b5860207b`) |
| Minor: duplicated test credentials; stale `mongo-init.js` comment | cross-referenced both ways; comment rewritten (`776da5f16`) |
| Minor: reserved-port race; floating image tags | accepted, §4.4 |
| Minor: dead `spring.data.mongodb.*` lines in `application.properties` | pre-existing; a follow-up issue if the human approves one |

- **The skip wiring.** Before the change, `-DintegrationTests -DskipITs -DskipTests` logged `Start
  container` for both images, and `spring-boot:start` forked the application (`.logs/mvn-verify-skipits-red.log`).
  After it, all four goals ran as no-ops, and no container or process was left.
- **After the round,** `mvn -B clean install` exited `0`, and so did gate 3,
  `mvn -B clean verify -DintegrationTests -pl dsh-data,dsh-rest-api -amd`. `-amd` pulled in
  `dsh-doc-indexer-worker` and `dsh-coverage-report`, and `DshDocIndexerApplicationIT` ran beside
  the three `dsh-rest-api` IT classes. All were green.

## 8. Follow-up issues

Created on 2026-09-28 at spec approval, before the spec was committed.

- [#137](https://github.com/MRISS-Projects/dsh/issues/137): *Run the Postman collections against a
  lifecycle-managed server*, label `task`, no milestone.
- [#138](https://github.com/MRISS-Projects/dsh/issues/138): *Return 4xx for document submission and
  lookup failures*, label `enhancement`, no milestone.
- [parent-poms#90](https://github.com/MRISS-Projects/parent-poms/issues/90): *Manage
  docker-maven-plugin's version*, label `task`; worked after DSH `0.3.0` ships (§4.5).

Created on 2026-09-28 during the build:

- [#139](https://github.com/MRISS-Projects/dsh/issues/139): *dsh-data connects to MongoDB
  unauthenticated, with connection settings fixed at build time*, label `bug`, milestone
  `0.3.0-SNAPSHOT`. Not a follow-up: it records the defect that §4.2.2 fixes on this branch, and
  closes with #46's PR.

## 9. Acceptance criteria

- [x] AC001: `dsh-rest-api` binds `spring-boot:start` to `pre-integration-test` and `spring-boot:stop`
  to `post-integration-test` — §6.1, evidence §7.4.
- [x] AC002: `mvn -B install` does not start the application — the profile is not active, §7.4.
- [x] AC003: `mvn -B install -DintegrationTests` starts it before the integration tests and stops it
  after — §7.4.
- [x] AC004: `DocumentResourceHttpIT` calls the REST API over HTTP against the started server, and
  fails when the endpoint is broken — §4.3, §7.5.
- [x] AC005: the application **and the containers** are stopped even when an integration test fails
  — §7.6.
- [x] AC006: no hardcoded port; every host port is reserved or mapped at build time and handed on —
  §4.1, §4.2.
- [x] AC007: `api-testing.yml` deliberately left alone — §3.1, follow-up `#137`.
- [x] AC008: the coverage gate is unaffected — §7.1.
- [x] AC009 (added at spec review, 2026-09-28): `testing-patterns.md` states the four test
  layers, when each is required, and where integration tests run; CLAUDE.md points at them and
  carries the conditional, per-module `-DintegrationTests` gate, which `dsh-ship-story` runs — §3.5, Task 8.

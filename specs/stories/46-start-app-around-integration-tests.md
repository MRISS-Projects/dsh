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
   - **It covers only the affected modules.** After the root `mvn -B clean install` gate passes,
     the story passes `mvn -B clean verify -DintegrationTests -pl <affected modules>`.
     The affected modules are the ones whose code changed.
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
declaration order. `spring-boot-maven-plugin` is declared in the main `<build>` for `repackage`, and
a profile's plugins merge in after it, so everything bound to `pre-integration-test` would run
`spring-boot:start` before the containers exist. Binding the port reservation and `docker:start` to
`package` puts them first without moving `repackage`. The same declaration order makes
`spring-boot:stop` run before `docker:stop` in `post-integration-test`, which is the teardown order
wanted. Task 3 and Task 6 prove the order from the build log instead of trusting this paragraph.

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

**The Mongo user.** `dsh-data`'s `dshApplicationContext.xml` authenticates as
`${mongo.user}:${mongo.password}@dsh`. The `@dsh` is literal, so the user must exist in the `dsh`
database. `dsh-rest-api/src/test/docker/mongo-init.js` creates `dshuser`/`dshpass` with `readWrite`
on `dsh`, bind-mounted read-only into `/docker-entrypoint-initdb.d`. The entrypoint runs init scripts
whenever that directory is non-empty, so no root user is needed.

**Arguments to the application.** `mongo.properties` is filtered at `dsh-data`'s build time, but
`context:property-placeholder` gives the `Environment` precedence over the file, so command-line
arguments win. `api-testing.yml` already relies on this. The arguments are `--server.port`,
`--mongo.host`, `--mongo.port`, `--mongo.user`, `--mongo.password`, `--spring.rabbitmq.host` and
`--spring.rabbitmq.port`.

**Container names** follow `dsh-it-%a-%t` (alias, timestamp), so AC005's leak check can filter on
`name=dsh-it-`, and two concurrent builds on one box do not collide.

### 4.2 The one production change: the RabbitMQ port

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
| `dsh-rest-api/src/main/resources/enqueue-docId-context.xml` | host and port placeholders (§4.2) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java` | new (§4.3) |
| `.github/copilot/rules/testing-patterns.md` | an "Over the wire" subsection (Task 7), then restructured around the four test layers (Task 8) |
| `.github/copilot/rules/java-conventions.md` | its unit-test line points at the layers (Task 8) |
| `.github/copilot/prompts/test-generation.md` | templates aligned with layers 1 and 2 (Task 8) |
| `CLAUDE.md` | *Quality gates*: `-DintegrationTests` needs Docker (Task 7), a pointer to the layers and the local integration gate (Task 8) |
| `.claude/skills/dsh-ship-story/SKILL.md` | runs the conditional, per-module integration gate in step 5 (Task 8) |

`api-testing.yml` is deliberately absent (§3.1).

## 6. Tasks

All Maven runs follow CLAUDE.md: log to `.logs/`, print the `tail` command, `wait`, report the exit
code. Every gate run includes `clean`.

### Task 1 — Baseline

- [ ] **Step 1.** `docker ps -a --filter name=dsh-it- --format '{{.Names}}'` prints nothing.
- [ ] **Step 2.** `mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1`, exit `0`.
- [ ] **Step 3.** Record `dsh-rest-api`'s LINE and BRANCH from
      `dsh-rest-api/target/site/jacoco/jacoco.csv` (sum `LINE_MISSED`/`LINE_COVERED` and
      `BRANCH_MISSED`/`BRANCH_COVERED` over all rows) into §7.1.

### Task 2 — The IT, red

**Files:** create `DocumentResourceHttpIT.java`.

- [ ] **Step 1.** Write the class:

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

- [ ] **Step 2.** `mvn -B clean install -DintegrationTests -pl dsh-rest-api -am -Dit.test=DocumentResourceHttpIT -Dit.failIfNoSpecifiedTests=false`
      to `.logs/mvn-clean-install-it-task2.log`. Expected: exit non-zero, all four tests failing
      with `dsh.it.baseUrl is not set`. Record in §7.2.
- [ ] **Step 3.** Commit: `test(#46): add the over-the-wire IT, red without a server`.

### Task 3 — Start and stop the application, no containers yet

**Files:** `dsh-rest-api/pom.xml`.

- [ ] **Step 1.** Add the profile with the `build-helper`, `spring-boot` and `failsafe` parts of
      §6.1's final XML, leaving the `docker-maven-plugin` block and the Mongo/Rabbit arguments out.
- [ ] **Step 2.** Same command as Task 2, log `.logs/mvn-clean-install-it-task3.log`. Expected:
      `submit_whenTitleBlank…` and `submit_whenContentsMissing…` **green** over real HTTP.
      `status_whenTokenUnknown…` is **red** with `500` after ~30 s, and `submit_whenValidPdf…` is
      **red**; there is no infrastructure. The log shows `spring-boot:stop` after the red tests.
      Record in §7.2.
- [ ] **Step 3.** Commit: `build(#46): start and stop the application around integration tests`.

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

- [ ] **Step 1.** Add the `docker-maven-plugin` block and the Mongo/Rabbit arguments, completing
      §6.1's XML. **The bind mount is the one untested piece on Windows.** If `docker:start` rejects
      the `C:\…` host path, or the user is missing afterwards, drop the `<volumes>` block and
      bake the script into an image instead: add `dsh-rest-api/src/test/docker/mongo/Dockerfile`
      holding `FROM mongo:6` and `COPY mongo-init.js /docker-entrypoint-initdb.d/`, move
      `mongo-init.js` beside it, and give the `mongo` image a
      `<build><contextDir>${project.basedir}/src/test/docker/mongo</contextDir></build>` with
      `<name>dsh-it-mongo:${project.version}</name>`. `docker:start` builds it on demand. Record
      which form shipped, and why, in §7.3.
- [ ] **Step 2.** Same command, log `.logs/mvn-clean-install-it-task4.log`. Expected:
      `status_whenTokenUnknown…` **green**, a real Mongo miss. `submit_whenValidPdf…` is **red** at
      `QUEUED_FOR_INDEXING_ERROR`, because the application still dials RabbitMQ on 5672 and the
      broker sits on a random port. If something already listens on 5672 locally, stop it for this
      run; otherwise this red turns falsely green. Record in §7.3.
- [ ] **Step 3.** Commit: `build(#46): run MongoDB and RabbitMQ containers around integration tests`.

### Task 5 — The RabbitMQ port, green

**Files:** `enqueue-docId-context.xml` (§4.2).

- [ ] **Step 1.** Apply §4.2.
- [ ] **Step 2.** Same command, log `.logs/mvn-clean-install-it-task5.log`: all four **green**.
- [ ] **Step 3.** `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`, exit `0`. The
      placeholder defaults keep the six Spring-context ITs and every unit test unchanged.
- [ ] **Step 4.** Commit: `fix(#46): let the RabbitMQ host and port be configured`.

### Task 6 — Acceptance evidence

Nothing here is committed except §7.

- [ ] **AC002.** In `.logs/mvn-clean-install.log` from Task 5, `grep -c
      'spring-boot-maven-plugin:.*:start\|docker-maven-plugin\|reserve-network-port'` is `0`, and
      `docker ps -a --filter name=dsh-it-` is empty.
- [ ] **AC001 / AC003.** `mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it.log`
      for the whole reactor, exit `0`. Record the ordered `grep -n` of `reserve-network-port`,
      `docker-maven-plugin.*start`, `spring-boot-maven-plugin.*start`, `maven-failsafe-plugin`,
      `spring-boot-maven-plugin.*stop` and `docker-maven-plugin.*stop`.
- [ ] **AC004.** Two mutations, each run with `-Dit.test=DocumentResourceHttpIT`, each reverted and
      the revert confirmed by `git diff --exit-code`:
  - In `DocumentResource.validateParameters`, replace `StringUtils.isBlank(title)` with `false`.
    Expected: `submit_whenTitleBlank…` red.
  - In `DocumentResource.getStatus`, replace `d == null` with `false`. Expected:
    `status_whenTokenUnknown…` red, `500`.
- [ ] **AC005.** Change `submit_whenTitleBlank…`'s expected token to `"NOT-ERROR"`, run the whole
      reactor with `-DintegrationTests`. Expected: exit non-zero from `failsafe:verify`, with
      `spring-boot:stop` and `docker:stop` both in the log before it. Then:
      - `jps -l` lists no `com.mriss.dsh.restapi.DshRestApplication`;
      - `netstat -ano | grep ":<dsh.it.http.port> "` is empty, with the port read from the log;
      - `docker ps -a --filter name=dsh-it-` is empty.
      Revert and confirm with `git diff --exit-code`.
- [ ] **AC006.** `git diff staging-0.3.0-SNAPSHOT-RC -- dsh-rest-api | grep -nE '8080|27017|5672'`
      matches only the container-side ports in `<port>…:27017</port>` / `<port>…:5672</port>`, and
      the `:5672` fallback in `enqueue-docId-context.xml`. None of them is a port the host binds.
- [ ] **AC007.** `git diff --stat staging-0.3.0-SNAPSHOT-RC -- .github/workflows` is empty; the
      reason is §3.1.
- [ ] **AC008.** Task 5's `jacoco.csv` gives the same LINE and BRANCH for `dsh-rest-api` as Task 1.

### Task 7 — Documentation

- [ ] **Step 1.** `testing-patterns.md`, after *Integration Tests (`@SpringBootTest`)*, add
      *Over the wire*:
  - what distinguishes it from `RANDOM_PORT`/MockMvc ITs: a separate process, real MongoDB and
    RabbitMQ, no Spring context in the test JVM;
  - that the URL comes from `dsh.it.baseUrl` and a port is never hardcoded;
  - that it needs Docker;
  - the leaked-container cleanup command from §4.4.

  Update the `RANDOM_PORT` bullet so the two are not confused.
- [ ] **Step 2.** `CLAUDE.md`, *Quality gates* item 1: after "run under `mvn -B clean install
      -DintegrationTests`", add that this now starts MongoDB and RabbitMQ in Docker for
      `dsh-rest-api` and needs a running Docker daemon.
- [ ] **Step 3.** The markdown-lint command from CLAUDE.md passes.
- [ ] **Step 4.** Commit: `docs(#46): document over-the-wire integration tests`.

### Task 8 — The test layers

Documentation only; the content is §3.5. `testing-patterns.md` is the single source of truth.
CLAUDE.md is a router and does not restate standards, so it gets a pointer and the gate, not the
table.

- [ ] **Step 1.** `testing-patterns.md`:
  - replace the three-item list at the top (`:7-9`) with §3.5's table and its bullets;
  - the *API / E2E* entry becomes layer 4, owned by `#137`;
  - rename *Integration Tests (`@SpringBootTest`)* to *Layer 2: Spring context*;
  - rewrite its "optional by design" line (`:113`) as §3.5's layer-2 trigger;
  - rename Task 7's *Over the wire* to *Layer 3: over the wire* and state the `*HttpIT` suffix;
  - add *Layer 4: external client*: the contract-only vantage point, not yet in force, `#137`;
  - add *Where integration tests run*: staging, deploy (future), local gate, never on pull
    requests.
- [ ] **Step 2.** `java-conventions.md:60`: keep the one-line unit rule and add "see the test layers
      in `testing-patterns.md`". Do not copy the table.
- [ ] **Step 3.** `.github/copilot/prompts/test-generation.md`:
  - the unit template states that REST entry points get a Mockito unit test (layer 1), with
    `MockMvcBuilders.standaloneSetup` and no context;
  - the *Controller Slice Test* template is labelled layer 2 and names the class `*IT` in the
    `integration` package.
- [ ] **Step 4.** `CLAUDE.md`, *Quality gates*:
  - add a third, conditional gate, worded as in §3.5:
    - when it applies: code touching an external system or a REST API entry point;
    - what runs: `mvn -B clean verify -DintegrationTests -pl <affected modules>`, after the root
      `clean install`;
    - why there is no `-am`;
  - add the same gate to `dsh-ship-story`'s step-5 checks (`.claude/skills/dsh-ship-story/`), so
    the process runs it rather than only documenting it;
  - add one sentence pointing at `testing-patterns.md` for the four layers;
  - keep the section's existing statement that `jacoco-it.exec` is never read by the coverage gate.
- [ ] **Step 5.** Grep the touched files for `optional by design` and `Postman collections in` and
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
         `pre-integration-test`: spring-boot-maven-plugin is declared in the main build and would
         otherwise start the application before its databases. Declaration order also makes
         spring-boot:stop run before docker:stop. -->
    <profile>
        <id>http-integration-tests</id>
        <activation>
            <property>
                <name>integrationTests</name>
            </property>
        </activation>
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
                    <groupId>io.fabric8</groupId>
                    <artifactId>docker-maven-plugin</artifactId>
                    <!-- §4.5: not yet managed by parent-poms; see parent-poms#90. -->
                    <version>0.49.0</version>
                    <configuration>
                        <containerNamePattern>dsh-it-%a-%t</containerNamePattern>
                        <images>
                            <image>
                                <alias>mongo</alias>
                                <name>mongo:6</name>
                                <run>
                                    <ports>
                                        <port>dsh.it.mongo.port:27017</port>
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
                                        <port>dsh.it.rabbitmq.port:5672</port>
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
                                <jmxPort>${dsh.it.jmx.port}</jmxPort>
                                <maxAttempts>120</maxAttempts>
                                <arguments>
                                    <argument>--server.port=${dsh.it.http.port}</argument>
                                    <argument>--mongo.host=localhost</argument>
                                    <argument>--mongo.port=${dsh.it.mongo.port}</argument>
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
                                <jmxPort>${dsh.it.jmx.port}</jmxPort>
                            </configuration>
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

Filled in during the build step (`dsh-build-story`).

### 7.1 Baseline and result (Tasks 1, 5, 6: AC008)

### 7.2 Red, then partly green (Tasks 2, 3)

### 7.3 Containers (Task 4)

### 7.4 Lifecycle order (Task 6: AC001, AC002, AC003)

### 7.5 Mutations (Task 6: AC004)

### 7.6 Teardown on red (Task 6: AC005)

## 8. Follow-up issues

Created on 2026-09-28 at spec approval, before the spec was committed.

- [#137](https://github.com/MRISS-Projects/dsh/issues/137): *Run the Postman collections against a
  lifecycle-managed server*, label `task`, no milestone.
- [#138](https://github.com/MRISS-Projects/dsh/issues/138): *Return 4xx for document submission and
  lookup failures*, label `enhancement`, no milestone.
- [parent-poms#90](https://github.com/MRISS-Projects/parent-poms/issues/90): *Manage
  docker-maven-plugin's version*, label `task`; worked after DSH `0.3.0` ships (§4.5).

## 9. Acceptance criteria

- [ ] AC001: `dsh-rest-api` binds `spring-boot:start` to `pre-integration-test` and `spring-boot:stop`
  to `post-integration-test` — §6.1, evidence §7.4.
- [ ] AC002: `mvn -B install` does not start the application — the profile is not active, §7.4.
- [ ] AC003: `mvn -B install -DintegrationTests` starts it before the integration tests and stops it
  after — §7.4.
- [ ] AC004: `DocumentResourceHttpIT` calls the REST API over HTTP against the started server, and
  fails when the endpoint is broken — §4.3, §7.5.
- [ ] AC005: the application **and the containers** are stopped even when an integration test fails
  — §7.6.
- [ ] AC006: no hardcoded port; every host port is reserved or mapped at build time and handed on —
  §4.1, §4.2.
- [ ] AC007: `api-testing.yml` deliberately left alone — §3.1, follow-up `#137`.
- [ ] AC008: the coverage gate is unaffected — §7.1.
- [ ] AC009 (added at spec review, 2026-09-28): `testing-patterns.md` states the four test
  layers, when each is required, and where integration tests run; CLAUDE.md points at them and
  carries the conditional, per-module `-DintegrationTests` gate, which `dsh-ship-story` runs — §3.5, Task 8.

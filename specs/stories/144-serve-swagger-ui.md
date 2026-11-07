---
issue: 144
slug: serve-swagger-ui
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 144 — Serve the Swagger UI

## 1. Story

**As a** developer or tester trying the REST API
**I want** the Swagger UI served by `dsh-rest-api`
**So that** I can read both operations and try them from a browser, without writing a `curl` command

## 2. Context

- Wave: 1, step 7 of the order of work (`specs/product/PRD.md` §4, Wave 1), triaged issue. DSH
  milestone `0.4.0-SNAPSHOT`.
- Issue: [#144](https://github.com/MRISS-Projects/dsh/issues/144), labelled `bug`. It was written
  against Springfox on 0.3.0. `#120`'s comment on it records what changed.
- Parent branch: `DEVELOP`, at `66d0730a5` when this spec was written. The task branch is
  `issue-144-serve-swagger-ui`.

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| Springfox is gone. `#120` replaced it with springdoc-openapi 3.1.1 on Spring Boot 4.1.1, and the `dsh-app` group the issue names went with it | `specs/stories/120-upgrade-spring-boot.md`; `#120`'s comment on `#144` |
| `dsh-rest-api` depends on springdoc's API-only starter, `springdoc-openapi-starter-webmvc-api`, with no version | `dsh-rest-api/pom.xml:67-70` |
| parent-poms `products` 3.11.2, which DSH pins, manages both starters, `-webmvc-api` and `-webmvc-ui`, at `${springdoc.version}` = 3.1.1 | `D:/.m2/repository/com/mriss/mriss-parent/products/3.11.2/products-3.11.2.pom:33-50` |
| springdoc serves the description at `/v3/api-docs`, for `/v1/dsh/**` only | `application.properties:11-12`; `OpenApiConfig` |
| `DocumentResourceHttpIT.apiDocs_shouldListTheDocumentEndpoints` already asserts that `/v3/api-docs` lists exactly the two operations, submit and status | `DocumentResourceHttpIT.java:153` |
| `DocumentResourceHttpIT` calls the server that the `http-integration-tests` profile forks on the main classpath, with RabbitMQ in Docker. It starts no Spring context | `dsh-rest-api/pom.xml:176` on; the class Javadoc |
| The README says the UI is not served, in both its source and the generated copy | `src/site/markdown/README.md:376-378`, `README.md:377-379` |
| No other document names the UI or `/v3/api-docs`: `specs/features`, `specs/api`, `system-design.md` and `docs/wiki` have no match | `grep -rn -i 'api-docs\|swagger'` over them |

### 2.2 Constraints found while designing

- **springdoc does not serve the issue's path.** The UI starter serves the page at
  `/swagger-ui/index.html` and redirects `springdoc.swagger-ui.path`, default `/swagger-ui.html`,
  to it. Nothing maps `/swagger-ui/` itself. AC001 is restated to springdoc's paths (§3.1
  decision 1). This comes from springdoc's documented defaults and was not observed here: the UI
  jar is not in the local repository yet. Task 1's green step observes it.
- **The UI's configuration is a second endpoint.** The page reads
  `/v3/api-docs/swagger-config` to learn where the description is. A UI that returns 200 but
  points at the wrong description would show nothing. The integration test checks that endpoint
  as well as the page.
- **A browser is needed for "the page lists both operations".** The page draws the operations with
  JavaScript, so an HTTP client sees only the empty shell. The automated check covers the page, its
  configuration and the description it points at. The smoke run in Task 3 covers the drawing.
- **`README.md` is generated from `src/site/markdown/README.md`** (`docs/devops/README.md`, "README.md
  is a Generated File"). Both are edited with the same text, as `#120` did, so that the copy GitHub
  shows is right before the next staging run regenerates it.

## 3. Design

### 3.1 Decisions

1. **AC001 uses springdoc's own paths.** `/swagger-ui.html` redirects to
   `/swagger-ui/index.html`, which returns 200. The README links `/swagger-ui.html`, the shorter
   path that springdoc keeps stable. No redirect of our own maps `/swagger-ui/`. Chosen by the
   owner on 2026-11-07. Rejected: keeping `/swagger-ui/` with a controller or view mapping of DSH's
   own, which is code to test and maintain for a path that springdoc does not use.
2. **Every profile serves the UI and the description**, as every profile serves `/v3/api-docs`
   today. ADR-004 makes every GCP tier private, so only a tier's invokers reach them. Whether a
   tier turns them off is decided with that tier's work, from Wave 4 on. Chosen by the owner on
   2026-11-07, which settles the question `#120`'s review left "for Wave 2's `gcp` profile or
   `#144`". Rejected: `springdoc.swagger-ui.enabled=false` under `gcp` now, which nothing could
   test, because no tier exists.
3. **One starter replaces the other.** `springdoc-openapi-starter-webmvc-ui` depends on
   `springdoc-openapi-starter-webmvc-api`, so `-webmvc-api` is removed rather than kept beside it.
   No version is written: parent-poms manages it. No parent-poms change is needed.
4. **No new configuration.** springdoc's defaults serve the UI where decision 1 says.
   `springdoc.paths-to-match` already limits what the UI shows to `/v1/dsh/**`. Only the comment
   above it changes, to name the UI.
5. **No unit test.** The story adds no production Java, so there is nothing for layer 1 to cover
   and no coverage moves. The change is to what the server serves over HTTP, which is layer 3
   (`testing-patterns.md`, "Test layers"). Gate 3 applies, because the story changes a REST entry
   point.

### 3.2 The documentation text

In both `src/site/markdown/README.md` and `README.md`, the sentence that ends "Calling the API":

```markdown
The API description, in OpenAPI 3 format, is served at
`http://localhost:8080/v3/api-docs`. The Swagger UI is not served; see
[#144](https://github.com/MRISS-Projects/dsh/issues/144).
```

becomes:

```markdown
The API description, in OpenAPI 3 format, is served at
`http://localhost:8080/v3/api-docs`. The Swagger UI, at
`http://localhost:8080/swagger-ui.html`, lists both operations and can call them from the browser.
```

In `dsh-rest-api/src/main/resources/application.properties`, the comment above
`springdoc.paths-to-match`:

```properties
# springdoc-openapi serves the description at /v3/api-docs, for the REST API only (#120),
# and the Swagger UI at /swagger-ui.html (#144)
```

### 3.3 Out of scope

- Turning the UI or the description off on a GCP tier (§3.1 decision 2).
- A redirect from `/swagger-ui/` (§3.1 decision 1).
- The README's old screenshot of the Springfox UI. `#143` removed it, and no new one is added.
- The PRD's `#144` paragraph, which is step 8's.

## 4. Verification design

The two tests of Task 1 are written first and fail on today's code with 404. That failure is the
red step. The starter swap turns them green. Gates 1 and 2 run in Task 3, unchanged in what they
measure. Gate 3 runs in Tasks 1 and 3.

| AC | Check | Where |
|---|---|---|
| AC001 | `swaggerUi_shouldBeServedAtTheReadmeLink`: `GET /swagger-ui.html`, redirect followed, is 200 HTML from the Swagger UI. `swaggerConfig_shouldPointTheUiAtTheApiDocs`: the UI's configuration names `/v3/api-docs`. `apiDocs_shouldListTheDocumentEndpoints`, unchanged, lists the two operations. The smoke run sees them drawn | Task 1, Task 3 |
| AC002 | The two tests above, in `DocumentResourceHttpIT`, run by gate 3 | Task 1, Task 3 |
| AC003 | Both READMEs carry §3.2's text; `grep -n 'swagger-ui.html' README.md src/site/markdown/README.md` finds one line in each | Task 2, Task 3 |
| AC004 | `mvn -B clean install` exits 0 | Task 3 |

`SimpleClientHttpRequestFactory` follows redirects for `GET`, which `DocumentResourceHttpIT`
already uses, so the first test asserts what the README's link leads to, not the redirect itself.

## 5. Files to change

| File | Change |
|---|---|
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java` | Two tests (§4) |
| `dsh-rest-api/pom.xml` | `-webmvc-api` replaced by `-webmvc-ui` (§3.1 decision 3) |
| `dsh-rest-api/src/main/resources/application.properties` | The comment (§3.2) |
| `src/site/markdown/README.md`, `README.md` | §3.2's sentence |
| `specs/stories/144-serve-swagger-ui.md` | This spec, §8 filled in |

Nothing else.

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** `dsh-rest-api` serves the Swagger UI at `/swagger-ui.html`, an over-the-wire test holds it
there, and the README links it.

**Architecture:** one starter swapped in `dsh-rest-api/pom.xml`, at the version parent-poms
manages. Two tests in the existing layer-3 class. Two documents and a comment edited by hand.

**Tech stack:** Java 17, Spring Boot 4.1.1, springdoc-openapi 3.1.1, JUnit Jupiter, AssertJ, Maven,
Docker, markdownlint.

**Spec:** this file.

### Global constraints

- `DocumentResourceHttpIT.java` and `dsh-rest-api/pom.xml` indent with tabs; the code blocks below show them as four spaces, to satisfy markdownlint. Test names follow
  `.github/copilot/rules/testing-patterns.md`, "Naming Conventions".
- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs").
- Gate 3 needs a running Docker daemon. Docker Desktop is at `D:\Docker\app` and is often stopped:
  start it, then wait until `docker info` succeeds.
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- The README link is exactly `http://localhost:8080/swagger-ui.html`.
- Commit messages are `test(#144): …`, `fix(#144): …` and `docs(#144): …`, and end with the
  session's attribution lines.

### Task 1: The tests, red, then the starter

**Files:**

- Test: `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java`
- Modify: `dsh-rest-api/pom.xml:67-70`
- Modify: `dsh-rest-api/src/main/resources/application.properties:11`

**Interfaces:** consumes the class's `rest`, `baseUrl`, `JSON` and `assertJson`, all existing.
Produces nothing new.

- [ ] **Step 1: Write the failing tests.** In `DocumentResourceHttpIT`, after
  `apiDocs_shouldListTheDocumentEndpoints`, add:

  ```java
    @Test
    public void swaggerUi_shouldBeServedAtTheReadmeLink() {
        ResponseEntity<String> response = rest.exchange(baseUrl + "/swagger-ui.html", HttpMethod.GET, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(MediaType.TEXT_HTML.isCompatibleWith(response.getHeaders().getContentType())).isTrue();
        assertThat(response.getBody()).contains("swagger-ui");
    }

    @Test
    public void swaggerConfig_shouldPointTheUiAtTheApiDocs() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(baseUrl + "/v3/api-docs/swagger-config", HttpMethod.GET, null, JSON);

        assertJson(response, HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("url", "/v3/api-docs");
    }
  ```

  No import is added: `ResponseEntity`, `HttpMethod`, `HttpStatus`, `MediaType` and `Map` are
  imported already.

- [ ] **Step 2: Run gate 1, then gate 3, to see them fail.** Gate 1 installs the upstream modules
  that gate 3 resolves.

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api -amd > .logs/mvn-verify-integration.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-integration.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: gate 1 `maven exit=0`, because failsafe does not run without `-DintegrationTests`.
  Gate 3 `maven exit=1`, with both new tests failing on `404 NOT_FOUND` and every other IT passing.
  Record the two failure lines in §8.1.

- [ ] **Step 3: Swap the starter.** In `dsh-rest-api/pom.xml`, replace:

  ```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
        </dependency>
  ```

  with:

  ```xml
        <!-- #144: the UI starter serves the Swagger UI, and brings the API starter with it -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
  ```

  In `application.properties`, replace the comment above `springdoc.paths-to-match` with §3.2's
  two lines.

- [ ] **Step 4: Run gate 3 to see them pass.** The same gate 3 command as Step 2, same log.

  Expected: `maven exit=0`. If `swaggerConfig_shouldPointTheUiAtTheApiDocs` fails because springdoc
  3.1.1 shapes the configuration differently from §2.2's premise, fetch the endpoint from the
  running server, correct the assertion to the field that names the description, and record the
  served JSON and the correction in §8.1. The test must still fail if the UI points anywhere but
  `/v3/api-docs`.

- [ ] **Step 5: Commit.**

  ```bash
  git add dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceHttpIT.java
  git commit -m "test(#144): hold the Swagger UI and its configuration over the wire"
  git add dsh-rest-api/pom.xml dsh-rest-api/src/main/resources/application.properties
  git commit -m "fix(#144): serve the Swagger UI with springdoc's UI starter"
  ```

### Task 2: The documentation

**Files:**

- Modify: `src/site/markdown/README.md:376-378`
- Modify: `README.md:377-379`

- [ ] **Step 1: Replace the sentence in both files** with §3.2's text, verbatim.

- [ ] **Step 2: Lint.**

  ```bash
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json
  ```

  Expected: no output, exit 0. The READMEs are outside the lint's globs, as they were for `#120`.

- [ ] **Step 3: Commit.**

  ```bash
  git add src/site/markdown/README.md README.md
  git commit -m "docs(#144): link the Swagger UI from the README"
  ```

### Task 3: The gates and the acceptance checks

- [ ] **Step 1: Gates 1 and 2,** with the Task 1 Step 2 command for gate 1. Expected:
  `maven exit=0`. Record `dsh-rest-api`'s LINE and BRANCH coverage from
  `dsh-rest-api/target/site/jacoco/index.html` in §8.2. They match `DEVELOP`'s, because no
  production Java changed.

- [ ] **Step 2: Gate 3,** with the Task 1 Step 2 command, after Step 1. Expected: `maven exit=0`,
  with the failsafe summary in §8.2.

- [ ] **Step 3: AC003.**

  ```bash
  grep -n 'swagger-ui.html' README.md src/site/markdown/README.md
  grep -n 'Swagger UI is not served' README.md src/site/markdown/README.md
  ```

  Expected: one line in each file from the first, nothing from the second.

- [ ] **Step 4: The smoke run, the browser half of AC001.** Start the jar with RabbitMQ in Docker,
  as the README's "Running the Jar" section says, open `http://localhost:8080/swagger-ui.html`, and check
  that the page lists `POST /v1/dsh/document/submit` and `GET /v1/dsh/document/status/{token}`,
  and nothing else. Call status with an unknown token from the page and check it returns
  `TOKEN_NOT_FOUND`. Stop the jar and the container. Record what was seen in §8.3.

- [ ] **Step 5: Fill in §8 and commit.**

  ```bash
  git add specs/stories/144-serve-swagger-ui.md
  git commit -m "docs(#144): record the verification results"
  ```

- [ ] **Step 6: The comment on `#144`,** posted after the owner approves its text:

  > AC001 is restated by the spec, `specs/stories/144-serve-swagger-ui.md` §3.1 decision 1:
  > springdoc serves the UI at `/swagger-ui/index.html` and redirects `/swagger-ui.html` to it;
  > `/swagger-ui/` itself is not mapped, and no redirect of DSH's own is added. The `dsh-app` group
  > went with Springfox in `#120`. Every profile serves the UI, as it serves `/v3/api-docs`
  > (decision 2).

## 7. Acceptance criteria

The issue's AC001 is restated by §3.1 decision 1. AC004 is the quality gate every story carries.

| AC | Covered by |
|---|---|
| AC001: `/swagger-ui.html` leads to the UI, which returns 200 and loads `/v3/api-docs`, listing both operations | §3.1 decisions 1 and 3, Task 1 Steps 1 to 4, Task 3 Step 4 |
| AC002: an integration test asserts the UI and the description endpoint respond | §4, Task 1, Task 3 Step 2 |
| AC003: the README's "Calling the API" section links the UI | §3.2, Task 2, Task 3 Step 3 |
| AC004: `mvn -B clean install` passes at the 95% gate | Task 3 Step 1 |

## 8. Verification results

Filled in by Task 1 and Task 3.

### 8.1 Task 1: red and green

### 8.2 Task 3 Steps 1 and 2: the gates

### 8.3 Task 3 Step 4: the smoke run

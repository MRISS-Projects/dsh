---
issue: 120
slug: upgrade-spring-boot
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 120 — Upgrade Spring Boot to a supported line, version chosen by analysis

## 1. Story

**As a** DSH maintainer
**I want** DSH on a supported Spring Boot line, with the target version chosen by analysis
**So that** the application runs on a framework that still receives fixes, and the GCP migration in
Waves 2-3 is built against the Spring Boot and Spring Cloud GCP versions it will ship on

## 2. Context

- Wave: 1 (`specs/product/PRD.md` §4), step 4 of the wave's order of work, after `#119`, which
  closed on 2026-10-21 with PR `#179`. DSH milestone `0.4.0-SNAPSHOT`.
- Issue: [#120](https://github.com/MRISS-Projects/dsh/issues/120), label `task`.
- Parent branch: `DEVELOP`, at `b6450a884` when this spec was written. The task branch is
  `issue-120-upgrade-spring-boot`.
- Decided on the issue on 2026-10-21, before this spec: MongoDB and `dsh-solr` are gone;
  `enqueue-docId-context.xml` and AMQP stay, and Wave 2 task 1 deletes them after this story; this
  story removes the managed `spring-data-solr` from parent-poms; it opens its own parent-poms issue
  in milestone `3.11.2-SNAPSHOT`, and ends by releasing 3.11.2, re-pinning DSH to it and dropping
  `-U` from `ci.yml` and `api-testing.yml`.

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| Spring Boot is 2.7.18, set by `spring-boot.version` in parent-poms `products/pom.xml`, which imports `spring-boot-dependencies` and sets `spring-boot-maven-plugin`'s version from it. DSH sets no Boot version | `products/pom.xml` lines 38-44, 80, 107 at parent-poms `dfe8ecb2`; DSH `pom.xml` line 72 |
| `products/pom.xml` also manages `springfox-swagger2` and `springfox-swagger-ui` (`spring-fox.version` 3.0.0), `io.swagger:swagger-annotations` 1.6.14, `javax.xml.bind:jaxb-api` 2.4.0-b180830.0359, `spring-data-solr` 4.3.15, and a `byte-buddy`/`byte-buddy-agent` override at 1.14.5 whose comment ties it to Boot 2.7's 1.12.x | the same file, lines 24-72 and 80-89 |
| DSH is the only product on the current `products`. `mail-processor-service` is pinned to `products` 3.6.3; the translator and DSD-SlideShow use other parents. Nothing else in parent-poms uses the entries above | a search of the MRISS repositories on 2026-10-21 |
| DSH's parent is `products:3.11.2-SNAPSHOT` since `#119`; both workflows pass `-U` while it lasts. parent-poms `master` is at `3.11.2-SNAPSHOT`, milestone `3.11.2-SNAPSHOT` (number 26) is open with `#112` closed in it | DSH `pom.xml` lines 8-15; `ci.yml:215`; `api-testing.yml:208`; parent-poms milestones |
| `javax.*` is used in two main classes: `SwaggerConfig` (`javax.servlet.ServletContext`) and `DocumentResource` (`javax.servlet.http.HttpServletRequest`) | `git grep 'javax\.'` |
| `SwaggerConfig` is a Springfox `Docket`, group `dsh-app`, paths `regex("/v1/dsh.*")`, plus a `WebMvcRequestHandlerProvider` override working around Springfox's failure on `PathPatternParser`. Two `application.properties` keys exist only for Springfox: `spring.mvc.pathmatch.matching-strategy=ant_path_matcher` and `spring.main.allow-bean-definition-overriding=true`. `SwaggerConfigTest` and `DshRestApplicationTest.testSwaggerConfig` test it | the files |
| `dsh-rest-api/pom.xml` declares `spring-boot-starter-web-services` and `woodstox-core` 6.4.0 "required by spring-ws", but no class uses `org.springframework.ws`. It declares `jaxb-api`, used by nothing. It excludes `jackson-module-kotlin` from `spring-integration-stream` and `-amqp` | the POM; `git grep org.springframework.ws` |
| `@MockBean` is used in `DshRestApplicationIT` and `DocumentResourceIT`. `DocumentResourceIT` imports Jackson 2's `com.fasterxml.jackson.databind.ObjectMapper`; no main class uses Jackson directly. The DTOs hold only `String` fields | the files |
| `DshRestApplication` imports `enqueue-docId-context.xml` (Spring Integration and the rabbit namespace, versioned `spring-integration-5.0.xsd`) | the class and the XML |
| `.github/scripts/check-unit-tests-context-free.sh` matches `org.springframework.boot.test.autoconfigure` for slices and `org.springframework.boot.test.mock.mockito` for `@MockBean` | its `PATTERN`, line 48 |
| The README says the API description is served "in Swagger 2 format" at `/v2/api-docs?group=dsh-app`, and that the UI is not served (`#144`) | `README.md:377-379`; `src/site/markdown/README.md:376-378` |
| `profiles-and-configuration.md` was measured on Boot 2.7.18 only and says so; it cites 2.7.18 or `javax` at lines 67, 73-77, 358 and 856 | the file |

### 2.2 The analysis (AC001)

**Support windows**, from <https://endoflife.date/api/spring-boot.json>, read on 2026-10-21:

| Line | Released | Open-source support ends | Latest |
|---|---|---|---|
| 2.7 | 2022-05 | 2023-11-24 (ended) | 2.7.18 |
| 3.5 | 2025-05 | 2026-06-30 (ended) | 3.5.16 |
| 4.0 | 2025-11 | 2026-12-31 | 4.0.8 |
| **4.1** | **2026-06** | **2027-07-31** (commercial to 2028-07-31) | **4.1.1**, 2026-08-20 |

4.2 is announced but not released; Maven Central's newest `spring-boot-dependencies` is 4.1.1. 4.1
supports Java 17 to 26, so DSH stays on Java 17.

**Spring Cloud GCP**, from its compatibility table: the 8.x line pairs with Spring Cloud 2025.1.x
(Oakwood), Spring Boot 4.0.x and 4.1.x, and Spring Framework 7. Its latest release, 8.2.4, has a
`spring-cloud-gcp-dependencies` BOM that sets `spring-boot-dependencies.version` to 4.1.1 exactly.
Waves 2-3 add `spring-cloud-gcp-starter-*` from 8.x, through `spring-cloud-gcp-dependencies`.

**What Boot 4.1.1's BOM moves**, against 2.7.18:

| Library | 2.7.18 | 4.1.1 |
|---|---|---|
| Spring Framework | 5.3.31 | 7.0.9 |
| Spring Integration | 5.5.20 | 7.1.1 |
| Spring AMQP | 2.4.17 | 4.1.1 |
| Tomcat | 9.0.83 | 11.0.24 |
| Jackson | 2.13.5 | 3.1.5 (`tools.jackson`), plus `jackson-2-bom` 2.21.5 |
| Jakarta Servlet | `javax` 4.0.4 | `jakarta` 6.1.0 |
| Hibernate Validator | 6.2.5 | 9.1.3 |
| JUnit Jupiter | 5.8.2 | 6.0.3 |
| Mockito | 4.5.1 | 5.23.0; parent-poms' root still forces 5.17.0 |
| Byte Buddy | 1.12.23 | 1.18.11 |

**The replacements:**

- **Springfox → springdoc-openapi 3.1.1.** Springfox has no release since 3.0.0 (2020) and does not
  start on Boot 3 or later. springdoc 3.1.1 is built against Boot 4.1.0. It serves OpenAPI 3 at
  `/v3/api-docs`.
- **`spring-boot-starter-web` → `spring-boot-starter-webmvc`**, and **`spring-boot-starter-web-services`
  is dropped**: 4.1.1 deprecates both old names, and DSH uses no Spring WS.
- **`@MockBean` → `@MockitoBean`** (`org.springframework.test.context.bean.override.mockito`). Boot
  4 removed `@MockBean`.
- **Slice annotations moved.** In Boot 4 `@WebMvcTest` is
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, in `spring-boot-webmvc-test`
  4.1.1 (checked in the jar on 2026-10-21). DSH uses no slice, but the context-free guard looks for
  slices only under `org.springframework.boot.test.autoconfigure`, so it would miss one (§3.5).

**Which lands first, this or the XML contexts:** this story. Spring Integration 7.1.1 accepts
`enqueue-docId-context.xml` unchanged (§2.3), so the XML is deleted where the issue comment of
2026-10-21 put it, in Wave 2 task 1, already on Boot 4.

**Where the version is managed:** in parent-poms `products/pom.xml`, on the 3.11.2 hotfix line. DSH
is its only consumer (§2.1), so the move breaks no other product, and keeping the version central
is what `ADR-002` and `#119` did.

### 2.3 The spike, 2026-10-21

A throwaway detached worktree off `DEVELOP` `b6450a884`, outside the repository. To emulate the
parent-poms change, it set `spring-boot.version` 4.1.1 and `byte-buddy.version` 1.18.11 in DSH's
root POM. Its patch (402 lines) is in the session scratchpad and is not committed.

- **Changes needed:** the starters and dependencies of §3.3, the `jakarta.servlet` import,
  `@MockitoBean` in two ITs, Jackson 3's `ObjectMapper` in `DocumentResourceIT`, and Springfox
  replaced by `OpenApiConfig` plus `springdoc.paths-to-match`.
- **`mvn -B clean verify`:** `maven exit=0`, coverage met, with no use of deprecated API under
  `-Dmaven.compiler.showDeprecation=true`.
- **`mvn -B clean verify -DintegrationTests`:** `maven exit=0`. 71 unit tests in `dsh-data`, 43 in
  `dsh-rest-api`, and 20 ITs. One IT was new: `GET /v3/api-docs` lists exactly the two
  `/v1/dsh/document` paths.
- **`dependency:tree`:** `spring-boot` 4.1.1 only; `byte-buddy` 1.18.11. No hit for `springfox`,
  `spring-data-solr`, `junit:junit`, the vintage engine, `javax.servlet`, `javax.xml.bind`,
  `jackson-module-kotlin` or `spring-ws`. Mockito stayed 5.17.0, from parent-poms' root.
- **One red IT run, which was `#142`.**
  `DocumentResourceHttpIT.submit_whenSameFileTwice_shouldReturnTheSameTokenAndQueueItAgain` failed
  with "did not reach QUEUED_FOR_INDEXING_SUCCESS within 15000 ms; last status was
  DEQUEUED_FOR_INDEXING", and the log showed `toRabbitResponse` "has 3 subscriber(s)". `#142`'s
  comment of 2026-10-20 records the same signature on Boot 2.7.18. Run alone, the test then passed
  5 times out of 5, and the full run above passed.
- **Two log lines carried over:** springdoc warns at startup that `/v3/api-docs` is enabled by
  default; `logback.xml` is found twice on the classpath, as it is on 2.7.18.

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-21

1. **Spring Boot 4.1.1**, the only line with open-source support past 2026, managed in parent-poms
   `products/pom.xml`. Spring Cloud GCP 8.x and Spring Cloud 2025.1.x for Waves 2-3.
2. **springdoc's API-only starter**, `springdoc-openapi-starter-webmvc-api`. The UI stays with `#144`,
   which becomes one dependency and keeps its own acceptance check. parent-poms manages the UI starter
   too (§3.2), so `#144` needs no parent-poms change.
3. **`#142` in gate 3.** A red IT run counts as `#142` only when its log shows both "3 subscriber(s)"
   and `DEQUEUED_FOR_INDEXING`. Such a run is recorded in §7 and rerun once. Any other red run, or a
   second `#142` red in a row, is a failure to fix.
4. **Mockito stays at 5.17.0**, forced by parent-poms' root over Boot's 5.23.0. Bumping it changes
   the root POM every parent-poms consumer inherits; it is out of scope.

### 3.2 Upstream: parent-poms

The issue to open in `MRISS-Projects/parent-poms`, label `task`, milestone `3.11.2-SNAPSHOT`:

> **Move products to Spring Boot 4.1.1 and drop what Boot 4 leaves dead**
>
> `products/pom.xml` imports Spring Boot 2.7.18, whose open-source support ended in 2023-11. Move
> `spring-boot.version` to 4.1.1, the line supported to 2027-07-31, which Spring Cloud GCP 8.x is
> built against.
>
> Remove what the move leaves dead: the Springfox entries and `spring-fox.version` (Springfox does
> not start on Boot 3 or later), `io.swagger:swagger-annotations` 1.x, `javax.xml.bind:jaxb-api`,
> `spring-data-solr` and its property (discontinued; DSH removed Solr in
> `MRISS-Projects/dsh#177`), and the `byte-buddy` override at 1.14.5, which would now hold Byte Buddy
> below Boot's 1.18.11.
>
> Manage springdoc-openapi instead: `springdoc-openapi-starter-webmvc-api` and
> `springdoc-openapi-starter-webmvc-ui` at `${springdoc.version}`, 3.1.1.
>
> DSH is the only product on the current `products`; `mail-processor-service` is pinned to 3.6.3.
> Asked for by `MRISS-Projects/dsh#120`. Ships in hotfix 3.11.2 with `#112`, and 3.11.2 is
> released when this is done.

The change, in `products/pom.xml`, four-space indentation:

- `dependencyManagement`: delete the `byte-buddy` and `byte-buddy-agent` entries and their comment,
  the two `io.springfox` entries, `swagger-annotations`, `jaxb-api` with its `FR006` comment, and
  `spring-data-solr` with its comment. After the Boot BOM import, add:

  ```xml
              <dependency>
                  <groupId>org.springdoc</groupId>
                  <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
                  <version>${springdoc.version}</version>
              </dependency>
              <dependency>
                  <groupId>org.springdoc</groupId>
                  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                  <version>${springdoc.version}</version>
              </dependency>
  ```

- `properties`: delete `spring-fox.version`, `swagger-annotations.version`, `jaxb-api.version`,
  `spring-data-solr.version` and `byte-buddy.version` with their comments. Replace the `FR005`
  comment and the Boot version with:

  ```xml
          <!-- #114: Spring Boot 4.1.1, open-source support to 2027-07-31 (Spring Cloud GCP 8.x) -->
          <spring-boot.version>4.1.1</spring-boot.version>
          <springdoc.version>3.1.1</springdoc.version>
  ```

parent-poms' own process applies (`parent-poms-specs-on-master`): a task branch off `master`, a spec
at `specs/114-spring-boot-4-1-1.md` laid out as `specs/112-manage-mockito-junit-jupiter.md` is, with
this section as its design, then a pull request. No re-versioning: `master` is already
`3.11.2-SNAPSHOT`.

**No snapshot deploy.** `#119` deployed one because its DSH pull request built against the
`SNAPSHOT`. This story's pull request builds against the released 3.11.2 (§3.8), so DSH validates
the `SNAPSHOT` locally, from `mvn -B install` in parent-poms, and `DEVELOP` keeps resolving `#112`'s
`SNAPSHOT` from GitHub Packages until this story merges.

The issue is [`parent-poms#114`](https://github.com/MRISS-Projects/parent-poms/issues/114), in
milestone `3.11.2-SNAPSHOT`, opened on 2026-10-28 after the owner approved this spec.

### 3.3 DSH: the build

`dsh-rest-api/pom.xml`, tab indentation:

- `spring-boot-starter-web` becomes `spring-boot-starter-webmvc`.
- Delete `spring-boot-starter-web-services`, the `woodstox-core` entry and its comment, `jaxb-api`,
  and both `io.springfox` entries.
- Delete the `jackson-module-kotlin` exclusion from `spring-integration-stream` and
  `spring-integration-amqp`. Boot 4 manages Jackson 3, whose Kotlin module has another group, so
  the exclusions exclude nothing.
- Add, where Springfox was, with no version (parent-poms manages it). Shown with spaces; the file
  uses tabs:

  ```xml
          <dependency>
              <groupId>org.springdoc</groupId>
              <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
          </dependency>
  ```

The root `pom.xml` keeps `enforce-no-solr` as it is: it bans `spring-data-solr` by coordinates, so it
needs no managed version. No ban is added for `io.springfox`: with its management gone, a
declaration without a version fails the build by itself, and AC002's tree shows the rest.

### 3.4 DSH: the code and the tests

**Main code:**

- `DocumentResource`: `javax.servlet.http.HttpServletRequest` → `jakarta.servlet.http.HttpServletRequest`.
- Delete `config/SwaggerConfig.java`.
- Add `config/OpenApiConfig.java`, four-space indentation as `SwaggerConfig` had:

  ```java
  package com.mriss.dsh.restapi.config;

  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  import io.swagger.v3.oas.models.OpenAPI;
  import io.swagger.v3.oas.models.info.Info;

  /**
   * The OpenAPI description springdoc serves at {@code /v3/api-docs}. Which paths it covers is set
   * by {@code springdoc.paths-to-match} in {@code application.properties}.
   */
  @Configuration
  public class OpenApiConfig {

      @Bean
      public OpenAPI dshOpenApi() {
          return new OpenAPI().info(new Info()
                  .title("DSH - Document Smart Highlights API")
                  .description("DSH - Document Smart Highlights Application"));
      }
  }
  ```

  The title corrects Springfox's "Hightlights".
- `application.properties` (main): delete the two Springfox keys and their comments; add

  ```properties
  # springdoc-openapi serves the description at /v3/api-docs, for the REST API only (#120)
  springdoc.paths-to-match=/v1/dsh/**
  ```

  The test `application.properties` has neither Springfox key; it gets no springdoc key, so the IT
  of AC005 proves the main file's.

**Tests:**

- Delete `config/SwaggerConfigTest.java` and `DshRestApplicationTest.testSwaggerConfig`, with the
  `SwaggerConfig` import and the class Javadoc's mention of it.
- Add `config/OpenApiConfigTest.java`, a unit test with no context:

  ```java
  package com.mriss.dsh.restapi.config;

  import static org.junit.jupiter.api.Assertions.assertEquals;

  import org.junit.jupiter.api.Test;

  /**
   * Unit tests for {@link OpenApiConfig}, with no Spring context. That springdoc serves the
   * description is checked by {@code integration.DocumentResourceHttpIT}.
   */
  public class OpenApiConfigTest {

      @Test
      public void dshOpenApi_namesTheApi() {
          assertEquals("DSH - Document Smart Highlights API",
                  new OpenApiConfig().dshOpenApi().getInfo().getTitle());
      }
  }
  ```

- `DshRestApplicationIT`, `DocumentResourceIT`: `org.springframework.boot.test.mock.mockito.MockBean`
  → `org.springframework.test.context.bean.override.mockito.MockitoBean`, and each `@MockBean` →
  `@MockitoBean`.
- `DocumentResourceIT`: `com.fasterxml.jackson.databind.ObjectMapper` →
  `tools.jackson.databind.ObjectMapper`. On Boot 4, Jackson 2 reaches the test classpath only
  through `swagger-core`; the test should use the Jackson the application serializes with.
- `DocumentResourceHttpIT`: AC005's test, after the last existing test. Shown with spaces; the file
  uses tabs:

  ```java
      @Test
      @SuppressWarnings("unchecked")
      public void apiDocs_shouldListTheDocumentEndpoints() {
          ResponseEntity<Map<String, Object>> response = rest.exchange(baseUrl + "/v3/api-docs", HttpMethod.GET, null, JSON);

          assertJson(response, HttpStatus.OK);
          assertThat((Map<String, Object>) response.getBody().get("paths"))
                  .containsOnlyKeys("/v1/dsh/document/submit", "/v1/dsh/document/status/{token}");
      }
  ```

  `containsOnlyKeys` does not prove `paths-to-match`, as first written here: springdoc 3.1.1
  leaves the actuator out by default (`springdoc.show-actuator` is `false`), and the application run
  with `--springdoc.paths-to-match=/**` listed the same two paths (§7.6). The key stays, so that a
  later controller outside `/v1/dsh/**` is not described by accident; nothing tests it.

### 3.5 The context-free guard

Boot 4 puts slice annotations in per-technology modules, under
`org.springframework.boot.<technology>.test.autoconfigure`. The guard's slice alternative,
`org\.springframework\.boot\.test\.autoconfigure`, no longer matches them. `@MockitoBean` needs
nothing: its package is under `org.springframework.test.context`, which the guard already matches.

- `check-unit-tests-context-free.test.sh`, two new cases after case 5:
  - **5b**, red first: a `FooTest.java` importing
    `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` is a violation naming the file.
  - **5c**, a characterisation: a `FooTest.java` importing
    `org.springframework.test.context.bean.override.mockito.MockitoBean` is a violation naming the
    file. It passes on the current pattern; it is there so that a later narrowing of the first
    alternative cannot drop it.
- `check-unit-tests-context-free.sh`: the `PATTERN` becomes

  ```bash
  PATTERN='org\.springframework\.test\.context|org\.springframework\.boot\.test\.(context|autoconfigure|mock\.mockito)|org\.springframework\.boot\.[a-z0-9.]+\.test\.autoconfigure|webAppContextSetup|new [A-Za-z]*ApplicationContext\('
  ```

  and the header's signal list gains, after the `org.springframework.boot.test.autoconfigure` line:

  ```text
  #   org.springframework.boot.<tech>.test.autoconfigure  Boot 4's slices: @WebMvcTest, ...
  ```

  and the `org.springframework.test.context` line names `@MockitoBean` among its examples. The
  `boot.test.mock.mockito` alternative stays: it costs nothing and still catches a stale import.

### 3.6 AC007: the `#48` probes on 4.1.1

The harness is rebuilt in the session scratchpad from `specs/stories/48-investigate-spring-and-maven-profiles.md`
§5.2, unchanged except `spring-boot.version` 4.1.1 in its root POM, with §5.1's guards. Then every
probe of the findings' appendix runs as recorded there: P0, P1 to P27, and the added P3b, P4b and
P28 to P31, 34 runs. P28 imports `jakarta.validation` where it imported `javax.validation`.

- **P0 first.** It must print §5.3's 25 baseline lines. A difference is resolved before any other
  probe is trusted, as in `#48`.
- Each finding S1 to S8 and M1 to M4 gets a verdict, **unchanged** or **changed**, from its probes'
  output against the output the findings quote.
- `specs/architecture/profiles-and-configuration.md`:
  - The head of section 3 says the findings were measured on 2.7.18 on 2026-10-07 and re-run on
    4.1.1 on the run's date by `#120`, and drops "Not checked against Spring Boot 3".
  - A new subsection at the end of section 3, "Re-run on Spring Boot 4.1.1", holds a table: finding,
    probes, verdict, and for a changed one the new output and what it changes.
  - Each changed finding's own text says what differs on 4.1.1. Every rule of section 6.1, and every
    row of 6.2 to 6.4, that a changed finding touches is corrected.
  - Line 67's row becomes Spring Boot 4.1.1, managed in parent-poms, citing `products-3.11.2.pom`.
    Line 358 says the validation annotations are in `jakarta.validation`, as DSH now uses. Line 856
    names `spring-boot-dependencies:4.1.1`, noting the 2.7.18 run. Section 9.2's re-run candidate
    is marked done by `#120`.
- If a finding changes in a way that moves `ADR-003`, the ADR is not edited here: it is reported to
  the owner, as a follow-up.

### 3.7 The documents

| File | Change |
|---|---|
| `README.md`, `src/site/markdown/README.md` | "The API description, in OpenAPI 3 format, is served at `http://localhost:8080/v3/api-docs`. The Swagger UI is not served; see #144." The "in 0.3.0" goes, since 0.4.0 does not serve it either |
| `.github/copilot/rules/testing-patterns.md` | `@MockBean` → `@MockitoBean` in the layer table, the layer-2 bullets and the slice example; the slice example's import names Boot 4's package; the guard's package list as §3.5 has it |
| `.github/copilot/prompts/test-generation.md`, `docs/copilot/prompt-examples.md` | `@MockBean` → `@MockitoBean` |
| `specs/architecture/profiles-and-configuration.md` | §3.6. Its line 670 says `@MockitoBean` |
| `CLAUDE.md`, `docs/devops/README.md` | the pin is the released 3.11.2 and no workflow passes `-U` (§3.8) |

Historical records keep their text: the ADRs, the PRD, `specs/stories/`, `specs/features/`,
`docs/superpowers/plans/`. The PRD's `#144` paragraph is reconciled in step 8.

### 3.8 The release and the re-pin

The order, each step needing the owner's go where it is the owner's:

1. Tasks 2 to 8 build and verify DSH against the `SNAPSHOT` installed locally.
2. Step 5's local review covers the DSH diff and the parent-poms diff, and finishes.
3. The owner merges the parent-poms pull request and closes its issue. Milestone `3.11.2-SNAPSHOT`
   then has nothing open.
4. The milestone is renamed `3.11.2`, on the owner's go.
5. parent-poms `deploy.yml` is dispatched with `release_type: releases`, on the owner's go and only
   when no review touching parent-poms is running. It tags `mriss-parent-3.11.2` and moves `master`
   to `3.12.0-SNAPSHOT`.
6. DSH re-pins to 3.11.2 and drops `-U`, then gates 1 and 3 run again against the released parent.
7. Step 6 pushes, so the pull request's CI builds against the release from its first run.

The re-pin, in DSH:

- `pom.xml`: the parent version becomes `3.11.2`, and the comment above it:

  ```xml
      <!-- parent-poms 3.11.2, the hotfix release that carries #119's and #120's upstream changes
           (parent-poms#112, #114): Mockito's extension at Mockito's version, and Spring Boot 4.1.1. -->
  ```

- `ci.yml`: `run: mvn -B install --file pom.xml`, and the five-line `-U` comment above it deleted.
- `api-testing.yml`: `mvn -B install -DskipTests \`.
- `CLAUDE.md`: the paragraph's second and third sentences become "The parent is the released
  3.11.2 since `#120`, so neither workflow passes `-U`."
- `docs/devops/README.md`: `ci.yml`'s table row says `mvn -B install` only; "Parent POM" says the
  pin is `products:3.11.2` since `#120`, with the date of the release, and that neither workflow
  passes `-U`.
  The paragraphs on why a `SNAPSHOT` pin takes `-U` stay: they are the rule for the next one.

### 3.9 Out of scope

- Serving the Swagger UI: `#144`.
- Deleting the XML contexts and AMQP: Wave 2 task 1. Any GCP dependency: Wave 2.
- Fixing `#142`'s race. §3.1 decision 3 says how a red run is treated.
- Mockito 5.23.0 and parent-poms' managed `junit:junit`.
- `spring-restdocs-mockmvc`, declared and unused. It builds on 4.1.1 and is not part of the move.
- springdoc's startup warning and the duplicate `logback.xml`.
- The static `specs/api/openapi/dsh-rest-api.yaml`. It is the API's contract, not the served
  description, and the served paths do not change.

### 3.10 Risks

- **`#142` makes gate 3 red at random.** §3.1 decision 3.
- **A released 3.11.2 is frozen.** A defect found after step 5 in the parent-poms change needs
  3.11.3. Hence the release waits for step 5's review (§3.8).
- **The JSON key order may change.** Jackson 3 sorts properties alphabetically by default, so
  `{"status","message"}` may arrive as `{"message","status"}`. Clients that read by name are
  unaffected; the ITs, which read every field by name, are the evidence. No external-client check
  runs: `specs/api/postman/` holds no collection, and `api-testing.yml` skips the Newman step (`#137`).
- **A trailing slash no longer matches.** Spring MVC 7 has no trailing-slash matching, which Spring
  MVC 5.3 had on by default (`setUseTrailingSlashMatch`; 7.0.9 has no such option). So
  `GET /v1/dsh/document/status/{token}/` and `POST /v1/dsh/document/submit/` matched on 2.7.18, by
  that default, and are 404 on 4.1.1, as run (§7.6). A client that adds the slash breaks. The pull
  request and the release notes say so.
- **parent-poms `master` stays at `3.11.2-SNAPSHOT`** until step 5 of §3.8; `#109` and `#90` wait,
  as the owner accepted for `#119`.
- **Stale `target/` trees** hold classes compiled against Boot 2.7. The gates always run `clean`.

### 3.11 Follow-ups

1. **A comment on `#144`**, posted at Task 9 after the owner approves it:

   > `#120` replaced Springfox with springdoc-openapi 3.1.1 on Spring Boot 4.1.1. The description
   > is served at `/v3/api-docs`. parent-poms 3.11.2 manages `springdoc-openapi-starter-webmvc-ui` at
   > the same version, so serving the UI is that one dependency in `dsh-rest-api/pom.xml`; springdoc
   > serves it at `/swagger-ui/index.html`.

2. **`ADR-003`'s F8 rows** cite the Spring Cloud GCP 3.9.0 reference, paired with Boot 2.7. Wave 3
   re-reads them against 8.x before building on them. Reported to the owner at Task 9, not edited
   here.

## 4. Acceptance criteria and verification

| AC | Check | Expected | Where |
|---|---|---|---|
| AC001 | §2.2 and §3.1 of this spec | the version, the reasons, the Spring Cloud GCP line, where it is managed | this spec |
| AC002 | `mvn -B dependency:tree` over the reactor, searched for `spring-boot:`, `io.springfox`, `spring-data-solr` | every `org.springframework.boot` artifact, in every module that has one, at 4.1.1; no `io.springfox`; no `spring-data-solr` | Tasks 8, 9 |
| AC003 | `mvn -B clean install` | `maven exit=0`; `jacoco:check` and the coverage-data guard pass in every module | Tasks 3, 4, 8, 9 |
| AC004 | `mvn -B clean install -DintegrationTests`, Docker running | `maven exit=0`; `DshRestApplicationIT` and `DocumentResourceIT` run with no failure | Tasks 8, 9 |
| AC005 | `DocumentResourceHttpIT.apiDocs_shouldListTheDocumentEndpoints` | red at Task 4 step 2 with 404; green after | Tasks 4, 8 |
| AC006 | the parent-poms issue, pull request and release; DSH's `<parent>` | `parent-poms#114` referenced here and on `#120`; 3.11.2 released; DSH names `products:3.11.2` | Tasks 2, 9 |
| AC007 | §3.6 | 34 runs; a verdict per finding in `profiles-and-configuration.md`; every touched rule corrected | Task 7 |

Gate 3 applies: the story changes a REST entry point and the parent every module builds against.
AC004 runs `-DintegrationTests` over the whole reactor, which covers it.

## 5. Files to change

| Repository | File | Change |
|---|---|---|
| parent-poms | `products/pom.xml` | §3.2 |
| parent-poms | `specs/114-spring-boot-4-1-1.md` | its spec |
| dsh | `dsh-rest-api/pom.xml` | §3.3 |
| dsh | `DocumentResource.java`, `OpenApiConfig.java` (new), `SwaggerConfig.java` (deleted), `application.properties` | §3.4 |
| dsh | `OpenApiConfigTest.java` (new), `SwaggerConfigTest.java` (deleted), `DshRestApplicationTest.java`, `DshRestApplicationIT.java`, `DocumentResourceIT.java`, `DocumentResourceHttpIT.java` | §3.4 |
| dsh | `.github/scripts/check-unit-tests-context-free.sh`, `.test.sh` | §3.5 |
| dsh | `pom.xml`, `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml` | §3.8 |
| dsh | the files of §3.7 | §3.7 |
| dsh | `specs/stories/120-upgrade-spring-boot.md` | this spec, §7 filled in |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** DSH builds and runs on Spring Boot 4.1.1, from parent-poms 3.11.2, with springdoc serving
the API description, and the `#48` findings re-checked on 4.1.1.

**Architecture:** the upstream change first, installed locally. Then DSH moves in two green steps,
the migration and then the OpenAPI description with its red IT, followed by the guard, the
documents and the probes. The release and the re-pin come after step 5's review.

**Tech stack:** Maven 3.9.16, Java 17, Spring Boot 4.1.1 (Spring Framework 7.0.9, Spring Integration
7.1.1, Jackson 3.1.5, JUnit Jupiter 6.0.3), Mockito 5.17.0, springdoc-openapi 3.1.1.

**Spec:** this file.

### Global constraints

- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"). Never pipe `mvn` into `tail`. Maven is `~/apps/apache-maven-3.9.16`; its local
  repository is `D:\.m2\repository`.
- `-DintegrationTests` starts RabbitMQ in Docker. If `docker info` fails, start Docker Desktop from
  `D:\Docker\app` and wait until `docker info` succeeds.
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- Indentation: POMs keep theirs (`products/pom.xml` and DSH's root four spaces, `dsh-rest-api/pom.xml`
  tabs); Java files keep theirs (`DocumentResourceHttpIT` tabs, the other test classes and
  `config/` four spaces).
- Never pass `-Denforcer.skip=true`, never set `coverage.data.check.skip`, never weaken a test or a
  gate.
- A red IT run is `#142` only under §3.1 decision 3.
- DSH commits are `build(#120): …`, `feat(#120): …`, `test(#120): …`, `ci(#120): …` and
  `docs(#120): …`; parent-poms commits are `build(#114): …` and `docs(#114): …`. Each ends with the
  session's attribution lines. Nothing is pushed in DSH before step 6. Claude neither merges a pull
  request, nor closes an issue, nor dispatches a release without the owner's go.

### Task 1: Baseline (AC003, AC004)

- [x] **Step 1:** Gate 1 on the untouched branch:

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-baseline.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`.
- [x] **Step 2:** The integration baseline, Docker running:

  ```bash
  mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it-baseline.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-it-baseline.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`, or a `#142` red, rerun once.
- [x] **Step 3:** Record the per-module surefire and failsafe totals in §7:

  ```bash
  grep -E '^\[(INFO|WARNING|ERROR)\] (Building DSH|Tests run: [0-9]+)' .logs/mvn-clean-install-baseline.log | grep -v ' - in '
  grep -E '^\[(INFO|WARNING|ERROR)\] (Building DSH|Tests run: [0-9]+|--- failsafe)' .logs/mvn-clean-install-it-baseline.log | grep -v ' - in '
  ```

### Task 2: Upstream, parent-poms (AC006)

Run in `C:/Users/marce/github/parent-poms`. The issue is opened only after the owner approves this
spec, which approves §3.2's text.

**Files:** `products/pom.xml`; `specs/114-spring-boot-4-1-1.md`.

- [x] **Step 1:** Open the issue of §3.2 in milestone `3.11.2-SNAPSHOT`:

  ```bash
  gh issue create -R MRISS-Projects/parent-poms --label task --milestone 3.11.2-SNAPSHOT \
    --title "Move products to Spring Boot 4.1.1 and drop what Boot 4 leaves dead" --body-file "$BODY"
  ```

  `$BODY` is a file in the session scratchpad holding §3.2's quoted body, from "`products/pom.xml`
  imports" to "when this is done.", with the blockquote marker removed.

  Done on 2026-10-28: [`parent-poms#114`](https://github.com/MRISS-Projects/parent-poms/issues/114).
  Its number was written into this spec, and the spec was committed only then.
- [x] **Step 2:** Branch: `git checkout master && git pull && git checkout -b issue-114-spring-boot-4-1-1`.
  Write the spec in `#112`'s layout: the header table (issue, milestone `3.11.2-SNAPSHOT`, branch,
  "Asked for by `MRISS-Projects/dsh#120`"), goal, "What is true today" from §2.1's parent-poms rows,
  the design of §3.2, ACs (the entries are as §3.2 says; DSH builds green against
  `3.11.2-SNAPSHOT`), and tasks. Commit `docs(#114): spec for moving products to Spring Boot 4.1.1`.
- [x] **Step 3:** Make §3.2's change, then install:

  ```bash
  mvn -B install > .logs/mvn-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -n -E 'spring-boot.version|springdoc|springfox|solr|byte-buddy|jaxb|swagger-annotations' \
    /d/.m2/repository/com/mriss/mriss-parent/products/3.11.2-SNAPSHOT/products-3.11.2-SNAPSHOT.pom
  ```

  Expected: `maven exit=0`; the installed POM shows `spring-boot.version` 4.1.1 and the two
  `springdoc` entries, and none of the deleted names. Commit
  `build(#114): move products to Spring Boot 4.1.1 and manage springdoc`.
- [x] **Step 4:** Push and open the pull request into `master`, after the owner approves its text.
  Its merge is needed only before §3.8 step 3.

### Task 3: DSH migrates to Boot 4.1.1 (AC003)

**Files:** `dsh-rest-api/pom.xml`, `DocumentResource.java`, `SwaggerConfig.java`,
`application.properties`, `SwaggerConfigTest.java`, `DshRestApplicationTest.java`,
`DshRestApplicationIT.java`, `DocumentResourceIT.java`.

- [x] **Step 1, red:** against Task 2's installed parent, `mvn -B clean install` to
  `.logs/mvn-clean-install-red.log`. Expected: `maven exit=1`, failing on `dsh-rest-api`'s
  `io.springfox` dependencies having no version. Record the error line in §7.
- [x] **Step 2:** §3.3's POM changes, without the springdoc dependency. §3.4's `jakarta.servlet`
  import, the deletions of `SwaggerConfig`, `SwaggerConfigTest`, `testSwaggerConfig` and the two
  Springfox keys, `@MockitoBean`, and Jackson 3 in `DocumentResourceIT`.
- [x] **Step 3, green:** `mvn -B clean install` to `.logs/mvn-clean-install-boot4.log`. Expected:
  `maven exit=0`; `dsh-data`'s total equal to the baseline; `dsh-rest-api`'s the baseline less 2.
- [x] **Step 4:** Commit `build(#120): move dsh-rest-api to Spring Boot 4.1.1 and drop Springfox`.

### Task 4: The OpenAPI description (AC005)

**Files:** `DocumentResourceHttpIT.java`, `OpenApiConfigTest.java`, `OpenApiConfig.java`,
`dsh-rest-api/pom.xml`, `application.properties`.

- [x] **Step 1:** Add §3.4's `apiDocs_shouldListTheDocumentEndpoints`.
- [x] **Step 2, red:** Docker running,

  ```bash
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api -Dit.test=DocumentResourceHttpIT > .logs/mvn-verify-it-api-docs-red.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-it-api-docs-red.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=1`, `apiDocs_shouldListTheDocumentEndpoints` failing on status 404, the
  other tests of the class passing (or a `#142` red, rerun once).
- [x] **Step 3, red:** add §3.4's `OpenApiConfigTest`; `mvn -B clean install -pl dsh-rest-api` to
  `.logs/mvn-clean-install-openapi-red.log`. Expected: `maven exit=1`, a compilation error naming
  `OpenApiConfig`.
- [x] **Step 4, green:** add the springdoc dependency of §3.3, `OpenApiConfig` and the
  `springdoc.paths-to-match` key. Run step 3's build, then step 2's. Expected: `maven exit=0` both.
- [x] **Step 5:** Commit `feat(#120): serve the OpenAPI description with springdoc at /v3/api-docs`.

### Task 5: The guard (AC003)

**Files:** `.github/scripts/check-unit-tests-context-free.test.sh`, `check-unit-tests-context-free.sh`.

- [x] **Step 1, red:** add §3.5's cases 5b and 5c; `bash .github/scripts/check-unit-tests-context-free.test.sh`.
  Expected: a non-zero exit, 5b failing, 5c passing.
- [x] **Step 2, green:** §3.5's `PATTERN` and header. Run the test script, then the guard over the
  repository: `bash .github/scripts/check-unit-tests-context-free.sh`. Expected: both exit 0.
- [x] **Step 3:** Commit `ci(#120): the context-free guard finds Boot 4's slice packages`.

### Task 6: The documents (AC003)

**Files:** the rows of §3.7 but the re-pin; of the profiles document, only line 670.

- [x] **Step 1:** Make §3.7's changes to the READMEs, `testing-patterns.md`, `test-generation.md`,
  `prompt-examples.md` and line 670 of `profiles-and-configuration.md`.
- [x] **Step 2:** `git grep -n -E 'MockBean|v2/api-docs|Swagger 2' -- ':!specs/stories' ':!docs/superpowers' ':!docs/wiki' ':!specs/product/PRD.md' ':!specs/features' ':!specs/architecture/ADR-*'`.
  Expected: every hit is `@MockitoBean`, or names the guard's `boot.test.mock.mockito` alternative:
  the guard's header, its test case 5, and `testing-patterns.md`'s list of the guard's packages.
- [x] **Step 3:** markdownlint over the changed files, with `--config .markdownlint.json`. Expected:
  no error.
- [x] **Step 4:** Commit `docs(#120): @MockitoBean and the /v3/api-docs description`.

### Task 7: The `#48` probes on 4.1.1 (AC007)

**Files:** `specs/architecture/profiles-and-configuration.md`.

- [x] **Step 1:** Build the harness of §3.6 in `<scratchpad>/profile-probes-boot4`, its own git
  repository, with `spring-boot.version` 4.1.1. Run P0. Expected: §5.3's 25 lines exactly.
- [x] **Step 2:** Run P1 to P31 with P3b and P4b, as the findings' appendix records each, reverting
  the tree between probes. Keep each probe's output in `<scratchpad>/profile-probes-boot4/logs`.
- [x] **Step 3:** Compare each output with the one the findings quote; give each finding its verdict.
- [x] **Step 4:** Edit the document as §3.6 says. markdownlint it.
- [x] **Step 5:** Commit `docs(#120): the #48 findings re-run on Spring Boot 4.1.1`.

### Task 8: The gates (AC002 to AC005)

- [x] **Step 1:** Gate 1, `mvn -B clean install` to `.logs/mvn-clean-install.log`. Expected:
  `maven exit=0`; `dsh-rest-api`'s unit total the baseline less 1.
- [x] **Step 2:** Gate 3 over the reactor, `mvn -B clean install -DintegrationTests` to
  `.logs/mvn-clean-install-it.log`. Expected: `maven exit=0`, the IT total the baseline plus 1, or a
  `#142` red, rerun once and recorded.
- [x] **Step 3:** AC002:

  ```bash
  mvn -B -o dependency:tree > .logs/mvn-dependency-tree.log 2>&1; echo "maven exit=$?"
  grep -o -E 'org\.springframework\.boot:[a-z0-9-]+:jar:[^:]+' .logs/mvn-dependency-tree.log | awk -F: '{print $4}' | sort | uniq -c
  grep -c -E 'io\.springfox|spring-data-solr' .logs/mvn-dependency-tree.log
  ```

  Expected: one version, 4.1.1; a count of 0.
- [x] **Step 4:** Write the totals and results into §7. Commit `docs(#120): record the gates`.

### Task 9: Before step 6, after step 5's review (AC002 to AC004, AC006)

- [x] **Step 1:** §3.8 steps 3 to 5, each on the owner's go. Then confirm the release resolves:
  `mvn -B -U dependency:get -Dartifact=com.mriss.mriss-parent:products:3.11.2:pom` to
  `.logs/mvn-get-products-3.11.2.log`, `maven exit=0`.
- [x] **Step 2:** The re-pin of §3.8. `git grep -n -E '3\.11\.2-SNAPSHOT|-B -U'` over `pom.xml`,
  `.github/workflows`, `CLAUDE.md` and `docs/devops/README.md`. Expected: no hit.
- [x] **Step 3:** Gate 1 and gate 3 again, as in Task 8 steps 1 and 2, and Task 8 step 3's tree.
  Expected: the same results.
- [x] **Step 4:** Commit `build(#120): pin the parent to the released parent-poms 3.11.2 and drop -U`.
- [ ] **Step 5:** Post §3.11's comment on `#144` and a comment on `#120` naming `parent-poms#114` and
  3.11.2, each after the owner approves its text. Report §3.11's second item to the owner.

## 7. Verification results

Filled in as the tasks run. Run on 2026-10-28, Maven 3.9.16, JDK 17.0.20.1, Windows 11, Docker
Desktop running.

### 7.1 Task 1, the baseline, on Boot 2.7.18

| Run | Result | Unit tests | ITs |
|---|---|---|---|
| `mvn -B clean install` | `maven exit=0` | `dsh-data` 71, `dsh-rest-api` 44 | — |
| `mvn -B clean install -DintegrationTests` | `maven exit=0`, no `#142` red | the same | `dsh-rest-api` 19: `DocumentResourceHttpIT` 5, `DocumentResourceIT` 7, `DshRestApplicationIT` 3, `PersistenceSelectionIT` 4 |

### 7.2 Task 2, parent-poms

- Branch `issue-114-spring-boot-4-1-1` off `master` at `dfe8ecb2`. `5548ebe4` is the spec,
  `specs/114-spring-boot-4-1-1.md`; `98d2c84b` the change to `products/pom.xml`, 9 lines in and 48
  out. Opened on 2026-10-28 as
  [`parent-poms#115`](https://github.com/MRISS-Projects/parent-poms/pull/115), into `master`.
- `mvn -B install` there: `maven exit=0`. The installed `products-3.11.2-SNAPSHOT.pom` holds
  `spring-boot.version` 4.1.1, `springdoc.version` 3.1.1 and both `springdoc` entries; the grep for
  `springfox`, `solr`, `byte-buddy`, `jaxb` and `swagger-annotations` finds nothing.

### 7.3 Tasks 3 to 6, red then green

| Step | Run | Result |
|---|---|---|
| Task 3 step 1, red | `mvn -B clean install` | `maven exit=1`: "'dependencies.dependency.version' for io.springfox:springfox-swagger-ui:jar is missing. @ line 83", the same for `springfox-swagger2` and `javax.xml.bind:jaxb-api` |
| Task 3 step 3, green | `mvn -B clean install` | `maven exit=0`; `dsh-data` 71, `dsh-rest-api` 42, the baseline less 2 |
| Task 4 step 2, red | `-DintegrationTests -pl dsh-rest-api -Dit.test=DocumentResourceHttpIT` | `maven exit=1`; `apiDocs_shouldListTheDocumentEndpoints`: "expected: 200 OK but was: 404 NOT_FOUND"; the other 5 passed |
| Task 4 step 3, red | `mvn -B clean install -pl dsh-rest-api` | `maven exit=1`; "cannot find symbol: class OpenApiConfig" |
| Task 4 step 4, green | both runs again | `maven exit=0` both; 43 unit tests; `DocumentResourceHttpIT` 6 of 6 |
| Task 5 step 1, red | the guard's test script | exit 1; 5b failed ("expected exit 1, got 0"), 5c passed |
| Task 5 step 2, green | the test script; the guard over the repository | exit 0, 14 cases and 1 skipped; exit 0 |
| Task 6 step 2 | §6's `git grep` | only the guard's `boot.test.mock.mockito` alternative: its header, case 5, and `testing-patterns.md`'s list |
| Task 6 step 3 | markdownlint over the six changed files | the one error is `README.md:521` MD012, in the generated release notes. It is there at `HEAD` too, and the root README is outside `CLAUDE.md`'s lint globs |

`DocumentResourceIT` places `tools.jackson.databind.ObjectMapper` in its own import group after
`com.*`, and both ITs place the `MockitoBean` import after `ActiveProfiles`, so that each block stays
in order.

### 7.4 Task 8, the gates against `3.11.2-SNAPSHOT`

| Run | Result |
|---|---|
| Gate 1, `mvn -B clean install` | `maven exit=0`; `dsh-data` 71, `dsh-rest-api` 43, the baseline less 1; "All coverage checks have been met" in both |
| Gate 3, `mvn -B clean install -DintegrationTests` | `maven exit=0`; 20 ITs, the baseline plus 1; no `#142` red |
| AC002, `mvn -B -o dependency:tree` | `maven exit=0`; 66 `org.springframework.boot` artifacts, all 4.1.1; 0 hits for `io.springfox` or `spring-data-solr`, and 0 for `javax.servlet`, `javax.xml.bind`, `jackson-module-kotlin`, `spring-ws`, `junit:junit` or the vintage engine; `byte-buddy` 1.18.11, `mockito-core` 5.17.0, springdoc 3.1.1 |

### 7.5 Task 7, the `#48` probes on 4.1.1

- The harness of `#48` §5.2 in the session scratchpad, its own git repository, with
  `spring-boot.version` 4.1.1. Every build stopped at `package`; nothing was installed.
- P0 printed §5.3's 25 lines exactly (`diff` empty). Then P1 to P31 with P3b and P4b: 34 runs. The
  builds exited 0 except P19's first, which exited 1 on the circular placeholder, as on 2.7.18.
- Verdicts, recorded in `profiles-and-configuration.md` section 3, "Re-run on Spring Boot 4.1.1":
  S1 to S6, S8, M2 to M4 and section 5's P31 unchanged. S7 and M1 changed in wording only: Spring
  Framework 7 reports an unresolved or circular placeholder as `PlaceholderResolutionException`,
  P19's message ends `in value "${probe.lib.host}" <-- "${probe.lib.host}"` where it ended `in
  property definitions`, and P28a's report no longer names the `BindException`. No rule of 6.1 and
  no row of 6.2 to 6.4 changes, and nothing moves `ADR-003`.
- Departures, recorded in the document's appendix: P28's class prints its bucket when bound; P21's
  `build-info.properties` was first read from a wrong path and read again after one more build.

### 7.6 Step 5, the local review

One reviewer, over the DSH range `f5e1f9e5f..d6f059e53` and the parent-poms range
`dfe8ecb2..cd82754d`: no Critical and no Important finding, eight Minor ones. The owner had 1 to 7
acted on and 8 verified first. Each was checked before anything changed:

| # | Finding | Checked | Outcome |
|---|---|---|---|
| 1 | The IT does not prove `springdoc.paths-to-match` | springdoc 3.1.1's metadata: `springdoc.show-actuator` defaults to `false`. Gate 3's jar, run with `--server.port=18089`, listed `/v1/dsh/document/submit` and `/v1/dsh/document/status/{token}` as built and with `--springdoc.paths-to-match=/**` | §3.4 corrected. The message of `4d9ff89de` makes the same claim and is not rewritten |
| 2 | A trailing slash is now 404 | `javap`: `setUseTrailingSlashMatch` in spring-webmvc 5.3.31, nothing for trailing slash or suffix in 7.0.9. The same jar: `…/status/abc` 200, `…/status/abc/` 404 | added to §3.10, and to the pull request |
| 3 | No Postman run exists | `specs/api/postman/` holds only `README.md`; `api-testing.yml` prints "No Postman collections found" | §3.10 corrected |
| 4 | The slice example needs its module | `spring-boot-starter-test` 4.1.1 depends on neither `spring-boot-webmvc-test` nor any slice module; `WebMvcTest.class` is in `spring-boot-webmvc-test` | `testing-patterns.md` says so |
| 5 | "JUnit 5" is stale; the BOM resolves Jupiter 6.0.3 | `git grep 'JUnit 5'` | "JUnit Jupiter" in the rules, the prompts, `system-design.md`, `specs/testing/` and the enforcer message in `pom.xml` |
| 6 | A `@WebMvcTest` example named `*Test` | `prompt-examples.md:44` | renamed `DocumentControllerIT`, in an `integration` package |
| 7 | Row 67 of a dated table | section 2 is headed "On `DEVELOP` at `a760552f8`" | the row says `#120` updated it |
| 8 | Boot 4 test annotations outside `.test.autoconfigure` | the 4.1.1 jars: `@AutoConfigureTestRestTemplate`, `@AutoConfigureRestTestClient` (`resttestclient.autoconfigure`), `@AutoConfigureWebTestClient` (`webtestclient.autoconfigure`) and `@AutoConfigureWebServer` (`web.server.test`) are not matched. `javap -v`: each carries `@ImportAutoConfiguration` only, with `@PropertyMapping` or `@AutoConfigureJson`, and no `SpringExtension` or `@BootstrapWith`; `@WebMvcTest` carries both. Every slice sampled (`webmvc`, `restclient`, `webclient`, `data.jpa`, `jdbc`) is under `<tech>.test.autoconfigure` | real as a gap in the pattern, but none of the four starts a context, and what does start one is matched. The guard is not changed |

The reviewer also suggested deciding whether production serves `/v3/api-docs`, which Springfox's
`/v2/api-docs` did too. Left for Wave 2's `gcp` profile or `#144`; not changed here.

### 7.7 Task 9, the release and the re-pin

- The owner merged `parent-poms#115` (`26d7be1e`) and closed `parent-poms#114`. Milestone 26 then
  had 0 open and 4 closed, and was renamed `3.11.2` on the owner's go. `Build` on `26d7be1e`:
  success.
- On the owner's go, `deploy.yml` with `release_type: releases`, run `40700934642`: success. Tag
  `mriss-parent-3.11.2`; `master` at `3.12.0-SNAPSHOT`. The milestone was left open, for the owner.
- `mvn -B -U dependency:get -Dartifact=com.mriss.mriss-parent:products:3.11.2:pom`: `maven exit=0`,
  downloaded from `MRISS-Projects/maven-repo`. Its POM equals the tested `3.11.2-SNAPSHOT`'s but for
  the version (`diff` empty), with `spring-boot.version` 4.1.1 at line 49.
- The re-pin of §3.8. `git grep -n -E '3\.11\.2-SNAPSHOT|-B -U'` over `pom.xml`, `.github/workflows`,
  `CLAUDE.md` and `docs/devops/README.md`: no hit.
- Against the released parent: gate 1 `maven exit=0`, 71 and 43 unit tests, coverage met in both;
  gate 3 `maven exit=0`, 20 ITs (`DocumentResourceHttpIT` 6, `DocumentResourceIT` 7,
  `DshRestApplicationIT` 3, `PersistenceSelectionIT` 4), no `#142` red; `dependency:tree`
  `maven exit=0`, 66 `org.springframework.boot` artifacts at 4.1.1, 0 for `io.springfox` or
  `spring-data-solr`. The same as §7.4.

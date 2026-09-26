---
issue: 112
slug: reclassify-spring-context-tests
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 112 — Reclassify the Spring-context tests as integration tests and pay the unit-coverage bill

## 1. Story

**As a** developer reading DSH's test suite
**I want** every test that starts a Spring context named and placed as an integration test
**So that** the 95% coverage gate measures unit tests alone, and "is this a unit test?" has one answer

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#112](https://github.com/MRISS-Projects/dsh/issues/112)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`, like every Wave 0 story before it.
- Depends on [`parent-poms#67`](https://github.com/MRISS-Projects/parent-poms/issues/67), closed:
  failsafe runs behind `-DintegrationTests`, includes `**/*IT.java` and `**/*IntegrationTest.java`,
  surefire excludes both, and a second JaCoCo agent writes `jacoco-it.exec` so `jacoco:check` keeps
  reading unit coverage only. DSH already names `3.9.0-SNAPSHOT`; no re-pin.
- Sibling: `#46` (embedded-server integration tests). Not in scope here.

### 2.1 The rule this story establishes

> **A unit test never starts a Spring context.** A test that starts one — full, sliced, or
> hand-built — is an integration test, named `*IT`, placed in an `integration` package.

The rule is about the context, not about Spring. A unit test may reference production types that
happen to be Spring classes (`SpringApplication` as a `mockStatic` target, `SpringApplicationBuilder`
as an argument), and may use context-free test helpers (`MockMultipartFile`,
`MockMvcBuilders.standaloneSetup`, `ReflectionTestUtils`). The owner confirmed this wording during
brainstorming as more precise than "no Spring in unit tests".

### 2.2 PowerMock is forbidden

Added to this story by the owner, because paying the coverage bill writes new unit tests and a
static-mocking need will surface immediately (the worker `main()` methods).

- `org.powermock:*` is forbidden in DSH **and** in every project inheriting from parent-poms, in any
  scope, directly or transitively.
- **Mockito is the sanctioned mocking tool, `mockStatic` included.** Static mocking is allowed but
  discouraged: use it only where no seam can reasonably be designed in.
- The current wording at `.github/copilot/rules/testing-patterns.md:19` — *"NEVER USE PowerMock or
  similar tools that require bytecode manipulation"* — is replaced. Its second clause is already
  contradicted by the code: DSH resolves Mockito **5.17.0**, whose default inline mock maker rewrites
  bytecode through an agent, so every `@Mock` in the repository "requires bytecode manipulation". A
  rule nobody can follow teaches people to ignore rules.
- Enforced by the build, not by prose alone: a `bannedDependencies` enforcer execution in
  parent-poms (§4.4).

## 3. The measurement

Eight test classes start a Spring context today. Checked while writing this spec:

| Module | Class | How it starts one | What it actually needs |
|---|---|---|---|
| `dsh-data` | `DocumentTest` | `SpringRunner` + `@ContextConfiguration(DocumentTestConfiguration)` | Four `Document` fixtures. Nothing else. |
| `dsh-rest-api` | `DocumentHandlingServiceImplTest` | `@SpringBootTest`, `DocumentDao` as `@MockBean` | One service over one mocked DAO |
| `dsh-rest-api` | `DshRestApplicationTest` | `@SpringBootTest` | `contextLoads` needs it; DTO, `configure()` and `SwaggerConfig` checks do not |
| `dsh-rest-api` | `DocumentResourceTest` | `@SpringBootTest` + `webAppContextSetup` MockMvc | The context — it tests the web wiring |
| `dsh-doc-indexer-worker` | `DshDocIndexerApplicationTest` | `@SpringBootTest`, then calls `main()` | The context |
| `dsh-keyword-extractor` | `DshKeywordExtractorApplicationTest` | same | The context |
| `dsh-top-sentences-extractor` | `DshTopSentencesExtractorApplicationTest` | same | The context |
| `dsh-doc-processor-worker` | `DshDocProcessorWorkerApplicationTest` | same | The context |

`DshDataApplicationTest` is already context-free and stays as is. The four worker modules each have
exactly one production class — `@SpringBootApplication` with a two-line `main()` — and exactly one test
class, the smoke test above.

## 4. Design

### 4.1 Sort, do not blanket-rename — amends AC001

The issue's AC001 asked for all eight to be renamed `*IT`. Three of them start a context by accident
rather than need, and moving them would push real unit coverage out of the gate only to write it
again. Agreed with the owner during brainstorming: **a test that uses Spring without needing it is
rewritten context-free and stays a unit test; only a test that genuinely exercises the context moves.**

| Class | Outcome |
|---|---|
| `DocumentTest` | Rewritten with `@Before` fixtures from static factory methods. `DocumentTestConfiguration` is deleted. |
| `DocumentHandlingServiceImplTest` | Rewritten with `MockitoJUnitRunner`, `@Mock DocumentDao`, `@InjectMocks` |
| `DshRestApplicationTest` | Split. `contextLoads` → `integration/DshRestApplicationIT`. The DTO, `configure()` and `SwaggerConfig` checks stay in a context-free `DshRestApplicationTest`, which gains a `main()` test. |
| `DocumentResourceTest` | Moves to `integration/DocumentResourceIT` unchanged in substance. A new context-free `DocumentResourceTest` covers the controller with Mockito. |
| The four worker smoke tests | Move to `integration/*ApplicationIT`. Each module gains a context-free `*ApplicationTest` covering `main()` via `mockStatic(SpringApplication.class)`. |

The `integration` package is `<module base package>.integration` — e.g.
`com.mriss.dsh.restapi.integration`, `com.mriss.dsh.docindexer.integration` — which is the shape
`parent-poms#67`'s proof used (`com.mriss.dsh.data.integration.TemporaryProofIT`).

New and rewritten tests stay on **JUnit 4** and `MockitoJUnitRunner`, matching every other test in
the repository. Migrating to JUnit 5 is out of scope.

### 4.2 Why `mockStatic` for the worker `main()` methods

Each worker `main()` is `SpringApplication.run(X.class, args)` plus a log line. Without a context,
the only ways to execute those lines are to mock the static call or to add a production seam (a
package-private launcher field a test swaps). The owner chose "PowerMock banned by name, Mockito
`mockStatic` allowed" (§2.2); a seam would bend four production classes to fit a test, so
`mockStatic` is the proportionate tool here. The existing smoke tests keep proving the context
actually starts, now as ITs.

JaCoCo also counts the implicit constructor on the class-declaration line, so each unit test
instantiates the class once.

### 4.3 The context check

The rule in §2.1 is enforced by `.github/scripts/check-unit-tests-context-free.sh`, with a plain-bash
test `check-unit-tests-context-free.test.sh`, following the `check-spec-references` pair.

- **Scope:** every `*.java` under any `src/test/java` except `*IT.java` and `*IntegrationTest.java`,
  outside `target/`.
- **Signal:** a reference to any of these packages anywhere in the file — they are the ones whose
  purpose is to build, bootstrap or populate a test context:
  - `org.springframework.test.context` — `SpringRunner`, `SpringJUnit4ClassRunner`,
    `SpringExtension`, `@ContextConfiguration`, `@WebAppConfiguration`, `@TestPropertySource`
  - `org.springframework.boot.test.context` — `@SpringBootTest`, `@TestConfiguration`
  - `org.springframework.boot.test.autoconfigure` — every slice: `@WebMvcTest`, `@DataMongoTest`,
    `@JsonTest`, …
  - `org.springframework.boot.test.mock.mockito` — `@MockBean`, `@SpyBean`, meaningless without a
    context
- Plus `webAppContextSetup`, the MockMvc builder that needs a `WebApplicationContext`.
- **Allowed** because they build nothing: `org.springframework.mock.web`,
  `org.springframework.test.web.servlet` builders other than `webAppContextSetup`,
  `org.springframework.test.util`.
- **Output:** exit 1 naming each offending file and the matched token; exit 0 otherwise.
- **Where it runs:** `ci.yml`, as steps ahead of the Maven build — the test first, then the check.
  `ci.yml` rather than `spec-validation.yml`, because it guards Java sources, not specs.

It is a proxy — it detects the imports that start a context, not a context actually starting. A
test that constructs `new AnnotationConfigApplicationContext(...)` by hand would slip through; the
checker also flags `ApplicationContext(` constructions (`new [A-Za-z]*ApplicationContext(`) to
close the obvious hole. Anything subtler is for review.

### 4.4 The PowerMock ban in parent-poms — light round trip

One new execution on the existing `maven-enforcer-plugin` in parent-poms' root `pom.xml`
`<build><plugins>`, next to `enforce-coverage-data-exists`:

```xml
<execution>
    <id>ban-powermock</id>
    <phase>validate</phase>
    <goals>
        <goal>enforce</goal>
    </goals>
    <configuration>
        <!-- Explicit, so -Denforcer.skip=true cannot disarm it: explicit configuration beats the
             parameter's user property. There is deliberately no property to skip this rule. -->
        <skip>false</skip>
        <rules>
            <bannedDependencies>
                <excludes>
                    <exclude>org.powermock</exclude>
                </excludes>
                <searchTransitive>true</searchTransitive>
                <message>PowerMock is forbidden in every MRISS project. Use Mockito; mockStatic is allowed where no seam can be designed in.</message>
            </bannedDependencies>
        </rules>
    </configuration>
</execution>
```

- **No skip property**, unlike the coverage guard. The owner asked for the ban to hold "whatsoever";
  the coverage guard has a genuine exempt case, this rule has none.
- It qualifies for `CLAUDE.md`'s light round trip: one plugin execution, reviewable in one sitting.
  Commit on parent-poms `master`, `mvn -B install` there, dispatch `deploy.yml` with
  `release_type: snapshots`, comment the SHA on `#112`.
- parent-poms' `CLAUDE.md` gains one line stating the ban. Its setup stays deliberately minimal.

### 4.5 Failsafe and `repackage` — measure, then decide

`dsh-rest-api` runs `spring-boot-maven-plugin:repackage`, and parent-poms' failsafe configuration
does not set `classesDirectory`. Failsafe puts the module's packaged jar on the test classpath when
one exists; after `repackage` the classes sit under `BOOT-INF/classes` and may not be found.
Spring Boot's own starter parent sets `classesDirectory` to `${project.build.outputDirectory}` for
this reason; DSH does not inherit from it.

It has not been observed here — `parent-poms#67`'s proof ran in `dsh-data`, which does not repackage.
So Task 7 measures it:

- **If the rest-api ITs fail to load classes:** add
  `<classesDirectory>${project.build.outputDirectory}</classesDirectory>` to failsafe's
  `<configuration>` in parent-poms' `pluginManagement`, as a second light-round-trip commit, SHA
  commented on `#112`. It belongs there: every Boot product inheriting the parent has the same
  problem, and it is inert for one that does not repackage.
- **If they pass:** record the passing run in §7 and change nothing.

### 4.6 Out of scope

- `#46` — ITs that boot the packaged application over real HTTP.
- JUnit 5 migration.
- The Mockito version mix (`mockito-junit-jupiter` 4.5.1 against `mockito-core` 5.17.0,
  `byte-buddy` 1.14.5). Noted, not fixed; Task 6 confirms `mockStatic` works on this classpath.
- `specs/product/PRD.md` — reconciled in step 8, not here.

## 5. Files to change

| File | Change |
|---|---|
| `.github/scripts/check-unit-tests-context-free.sh` | New — §4.3 |
| `.github/scripts/check-unit-tests-context-free.test.sh` | New — fixture-based tests for the checker |
| `.github/workflows/ci.yml` | Two steps before `Build and test all modules` |
| `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentTest.java` | Context-free rewrite |
| `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentTestConfiguration.java` | Delete |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/service/DocumentHandlingServiceImplTest.java` | Mockito rewrite |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/DshRestApplicationTest.java` | Context-free; gains `main()` test |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DshRestApplicationIT.java` | New — `contextLoads` |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/rest/DocumentResourceTest.java` | Replaced by a Mockito unit test |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceIT.java` | The former `DocumentResourceTest` |
| 4 × worker `…/<pkg>/<X>ApplicationTest.java` | Replaced by a `mockStatic` unit test |
| 4 × worker `…/<pkg>/integration/<X>ApplicationIT.java` | The former smoke test |
| `CLAUDE.md` | PowerMock hard rule; quality-gates text on integration tests |
| `.github/copilot/rules/testing-patterns.md` | §5.1 |
| `.github/copilot/rules/java-conventions.md:60` | `@WebMvcTest` is a slice → an IT, not a unit test |
| `.github/copilot/prompts/test-generation.md` | Unit prompt: no context; IT prompt: `integration` package |
| `docs/process/ai-driven-development.md:163` | Same correction as `CLAUDE.md`'s quality gates |
| parent-poms `pom.xml`, `CLAUDE.md` | §4.4, and §4.5 if measured |

### 5.1 `testing-patterns.md`, specifically

- State the §2.1 rule and the `integration` package convention under a new "Unit vs integration"
  heading.
- Replace line 19 with the §2.2 wording.
- "Controller Tests (`@WebMvcTest`)": a slice starts a context, so the example is renamed
  `…IT` and moved under integration; the unit-level controller pattern is Mockito over the
  controller, as `DocumentResourceTest` now does.
- "Integration Tests": drop `@Tag("integration")` — the name is the selector — and the
  Testcontainers claim, which describes nothing in the repository. State that an integration test is
  **mandatory** when a task touches the REST API, optional by design for other Spring beans.
- "Test Execution": `mvn verify -P integration-tests` → `mvn -B install -DintegrationTests`; `-P` is
  never used in this estate. Staging always passes the flag.

## 6. Tasks

Each code task is red first: the checker (Task 2) is the red signal for the move tasks, and a
coverage run is the red signal for the unit tests that pay the bill. Every Maven run follows
`CLAUDE.md`'s logging rule: redirect to `.logs/<command>.log`, print the `tail -f`, `wait`, report
the exit code.

### Task 1 — Baseline

- [x] `mvn -B install > .logs/mvn-install-baseline.log 2>&1` — expect green.
- [x] Record per-module LINE and BRANCH from each module's `target/site/jacoco/jacoco.csv` into §7.
- [x] Record `Tests run:` totals per module.

### Task 2 — The context checker, red against today's tree

**Files:** `.github/scripts/check-unit-tests-context-free.sh`, `…test.sh`, `.github/workflows/ci.yml`

- [x] Write `check-unit-tests-context-free.test.sh` first, with cases:
  1. Clean unit test using `MockitoJUnitRunner` → exit 0.
  2. `@RunWith(SpringRunner.class)` in `FooTest.java` → exit 1, output names the file.
  3. Same content in `FooIT.java` → exit 0.
  4. Same content in `FooIntegrationTest.java` → exit 0.
  5. `@MockBean` import only → exit 1.
  6. `MockMvcBuilders.webAppContextSetup(` → exit 1; `MockMvcBuilders.standaloneSetup(` → exit 0.
  7. `new AnnotationConfigApplicationContext(` → exit 1.
  8. `MockMultipartFile` and `ReflectionTestUtils` imports → exit 0.
  9. A file under `target/` with a violation → ignored, exit 0.
- [x] Run it — fails, the checker does not exist.
- [x] Write the checker. Interface: `check-unit-tests-context-free.sh [ROOT]`, `ROOT` defaulting to
  the repository root. Pattern:

  ```bash
  PATTERN='org\.springframework\.test\.context|org\.springframework\.boot\.test\.(context|autoconfigure|mock\.mockito)|webAppContextSetup|new [A-Za-z]*ApplicationContext\('
  find "$ROOT" -path '*/target' -prune -o -path '*/src/test/java/*' -name '*.java' \
       ! -name '*IT.java' ! -name '*IntegrationTest.java' -print \
    | xargs -r grep -EnH "$PATTERN"
  ```

  Exit 1 with a header line (`Unit tests must not start a Spring context. Rename to *IT and move to
  an integration package, or remove the context:`) when grep matched anything.
- [x] Test script green.
- [x] Run the checker on the repository — **red, naming exactly the eight classes in §3** (plus
  `DocumentTestConfiguration`, which is `@Configuration` but imports nothing from the banned
  packages, so it should *not* appear — confirm).
- [x] Add to `ci.yml` before `Build and test all modules`:

  ```yaml
      - name: Test the unit-test context checker
        run: bash .github/scripts/check-unit-tests-context-free.test.sh

      - name: Unit tests must not start a Spring context
        run: bash .github/scripts/check-unit-tests-context-free.sh
  ```

- [x] Commit: `test(#112): check that unit tests never start a Spring context`. CI will be red on
  this commit until Task 6; the branch is not pushed until the end, so that is acceptable locally.

### Task 3 — `dsh-data`: `DocumentTest` context-free

- [x] Replace the four `@Autowired @Qualifier` fields with fields assigned in `@Before`, from private
  static factory methods carrying the bodies of `DocumentTestConfiguration`'s four `@Bean` methods
  (same file paths, same titles). Drop `@RunWith(SpringRunner.class)` and
  `@ContextConfiguration`, and the Spring imports. Every test method stays as it is.
- [x] Delete `DocumentTestConfiguration.java`.
- [x] `mvn -B -pl dsh-data -am install > .logs/mvn-install-dsh-data.log 2>&1` — green, same test
  count as the baseline, coverage not lower than baseline.
- [x] Checker no longer names `DocumentTest`.
- [x] Commit: `test(#112): build Document fixtures without a Spring context`.

Note: fixtures now rebuild per test instead of being context singletons shared across methods.
`testGetKeyWords` and `testGetRelevantSentences` mutate `docTitleConstructor`; with fresh fixtures
they no longer leak into each other, which is strictly safer.

### Task 4 — `dsh-rest-api`: `DocumentHandlingServiceImplTest` on Mockito

- [x] Replace the header with:

  ```java
  @RunWith(MockitoJUnitRunner.class)
  public class DocumentHandlingServiceImplTest {

      @Mock
      private DocumentDao dao;

      @InjectMocks
      private DocumentHandlingServiceImpl service;
  ```

  Drop the `DocumentQueueService` mock (the service does not use it), `Mockito.reset(dao)` (a fresh
  mock per test under the runner), and the Spring imports. Test bodies unchanged.
- [x] If `MockitoJUnitRunner` (strict stubs) flags the re-stubbing in `testGetDocumentByHash` as
  unnecessary, split that method into one test per branch rather than switching to a lenient runner.
- [x] Module build green; checker no longer names it. Commit:
  `test(#112): test DocumentHandlingServiceImpl without a Spring context`.

### Task 5 — `dsh-rest-api`: split `DshRestApplicationTest`, move `DocumentResourceTest`, unit-test the controller

- [x] Create `integration/DshRestApplicationIT.java` holding `contextLoads` and the two `@MockBean`s,
  with `@RunWith(SpringRunner.class) @SpringBootTest`. No explicit `classes`: `@SpringBootTest`
  searches upward from the test's package, and `integration` is a child of the application's package.
- [x] Rewrite `DshRestApplicationTest` context-free: keep `testDocumentStatusDto`, `testTokenDto`,
  `testSwaggerConfig`; `testConfigure` uses `new DshRestApplication().configure(new
  SpringApplicationBuilder())` (protected, same package); add:

  ```java
  @Test
  public void main_delegatesToSpringApplicationRun() {
      String[] args = {"arg1"};
      try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
          DshRestApplication.main(args);
          spring.verify(() -> SpringApplication.run(DshRestApplication.class, args));
      }
  }
  ```

- [x] `git mv` `rest/DocumentResourceTest.java` → `integration/DocumentResourceIT.java`; rename the
  class and logger, change the package. `@ContextConfiguration` already names
  `DshRestApplication.class` and an absolute `classpath:/` resource, so nothing else moves.
- [x] Run the module build **before** writing the new controller test — coverage for
  `DocumentResource` drops to zero and `jacoco:check` fails. That is the red.
- [x] Write a new `rest/DocumentResourceTest.java` — `MockitoJUnitRunner`, `@Mock
  DocumentSubmissionService`, `@Mock DocumentHandlingService`, `@InjectMocks DocumentResource`,
  `MockMultipartFile` for contents, `null` for the unused `HttpServletRequest`:
  - `submitDocument_whenValid_returnsToken` — stub `getTokenFromDocument` → `"tok"`; expect
    `TokenDto("tok", "")`, and `storeDocumentAndQueueForProcessing` verified.
  - `submitDocument_whenTitleBlank_returnsError` — token `"ERROR"`, message starting
    `"Error submitting file: "`; the submission service never called.
  - `submitDocument_whenContentsNull_returnsError`.
  - `submitDocument_whenServiceThrows_returnsError` — `getTokenFromDocument` throws.
  - `getStatus_whenTokenUnknown_returnsTokenNotFound` — `TOKEN_NOT_FOUND`, message
    `TOKEN_NOT_FOUND_MESSAGE + token`.
  - `getStatus_whenDocumentFound_returnsItsStatus` — status description and message from a
    `new Document("t")`.
- [x] Module build green at ≥ 95% LINE and BRANCH. If short, the shortfall names the class; add unit
  tests for it. Never an exemption.
- [x] Checker clean for `dsh-rest-api`. Commit:
  `test(#112): move the rest-api context tests to integration and unit-test the controller`.

### Task 6 — The four workers

For each of `dsh-doc-indexer-worker` (`com.mriss.dsh.docindexer`, `DshDocIndexerApplication`),
`dsh-keyword-extractor` (`com.mriss.dsh.analyser.keywords`, `DshKeywordExtractorApplication`),
`dsh-top-sentences-extractor` (`com.mriss.dsh.analyser.topsentences`,
`DshTopSentencesExtractorApplication`), `dsh-doc-processor-worker`
(`com.mriss.dsh.analyser.docprocessor`, `DshDocProcessorWorkerApplication`):

- [x] `git mv` the smoke test to `integration/<X>ApplicationIT.java`; rename the class and change the
  package. `@SpringBootTest` still finds the application by searching upward.
- [x] Module build — **red**: no `jacoco.exec`, `enforce-coverage-data-exists` fails. Record the
  message once in §7.
- [x] Write `<X>ApplicationTest.java`:

  ```java
  public class DshDocIndexerApplicationTest {

      @Test
      public void main_delegatesToSpringApplicationRun() {
          String[] args = {"arg1", "arg2"};
          try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
              DshDocIndexerApplication.main(args);
              spring.verify(() -> SpringApplication.run(DshDocIndexerApplication.class, args));
          }
      }

      @Test
      public void constructor_isInstantiable() {
          assertNotNull(new DshDocIndexerApplication());
      }
  }
  ```

- [x] Module build green at ≥ 95%. The first module also confirms `mockStatic` works on the mixed
  Mockito classpath (§4.6); if it does not, stop and bring it back — do not reach for a seam or a
  version bump without agreement.
- [x] One commit per module: `test(#112): unit-test <module> main without a Spring context`.
- [x] After the fourth, the checker is **green on the whole repository**.

### Task 7 — Full build, integration build, failsafe measurement

- [x] `mvn -B install > .logs/mvn-install.log 2>&1` — green; no IT ran under surefire
  (no `TEST-*IT.xml` in any `target/surefire-reports`).
- [x] `mvn -B install -DintegrationTests > .logs/mvn-install-it.log 2>&1`. Expect 6 IT classes
  (`DshRestApplicationIT`, `DocumentResourceIT`, four worker ITs) under failsafe.
- [x] If `dsh-rest-api`'s ITs fail to load classes because of `repackage`: §4.5's parent-poms
  commit, light round trip, SHA commented on `#112`; rerun with `-U` until green. Otherwise record
  the passing run.
- [x] Confirm `jacoco-it.exec` exists and `jacoco.exec` totals are unchanged by the IT run.

### Task 8 — Ban PowerMock in parent-poms

- [x] In parent-poms on `master` (pulled): add §4.4's execution; one line in its `CLAUDE.md`.
- [x] Probe, red: in a scratch copy of a consuming module (or in DSH with the dependency added
  locally, never committed), add `org.powermock:powermock-api-mockito2:2.0.9` in `test` scope and run
  `mvn -B validate` against the locally installed parent — expect failure naming the ban. Then with
  `-Denforcer.skip=true` — expect the same failure.
- [x] Without the dependency, DSH `mvn -B validate` is green.
- [x] Commit on parent-poms `master`, `mvn -B install`, dispatch `deploy.yml` with
  `release_type: snapshots`, comment the SHA on `#112` stating what it does and why.

### Task 9 — Documentation

- [x] `CLAUDE.md`: under "Quality gates", replace the "integration tests are not yet a separately
  enforced gate" parenthesis with: integration tests are `*IT` in an `integration` package, run under
  `-DintegrationTests` and on every staging build, measured by `jacoco-it.exec`, never by the gate. Add
  a **Hard rules** line: *PowerMock is forbidden here and in parent-poms, enforced by the
  `ban-powermock` enforcer execution; a unit test never starts a Spring context, enforced by
  `check-unit-tests-context-free.sh` in CI.* Coding standards stay in `.github/` — `CLAUDE.md` states
  the rule and points to `testing-patterns.md`.
- [x] `testing-patterns.md` per §5.1; `java-conventions.md:60`; `test-generation.md`;
  `docs/process/ai-driven-development.md:163`.
- [x] Markdown lint (the command in `CLAUDE.md`) clean.
- [x] Commit: `docs(#112): state the unit/integration rule and the PowerMock ban`.

### Task 10 — The issue

- [x] After the spec is approved: update `#112`'s body — AC001 amended per §4.1, AC008–AC010 added —
  with the owner's approval of the edited text.

## 7. Verification

Filled in while building: baseline figures (Task 1), the red messages (Tasks 5 and 6), the failsafe
measurement (Task 7), the PowerMock probe (Task 8), the parent-poms SHAs.

### 7.1 Baseline and result (Task 1, Task 7)

Figures from `dsh-coverage-report`'s aggregate `jacoco.csv` — the modules produce no per-module CSV.
"After" is `mvn -B clean install`, unit tests only.

| Module | Tests before | Tests after | LINE before | LINE after | BRANCH before | BRANCH after |
|---|---|---|---|---|---|---|
| `dsh-data` | 51 | 51 | 235/242 97.11% | 235/242 97.11% | 81/82 98.78% | 81/82 98.78% |
| `dsh-rest-api` | 36 | 36 | 139/145 95.86% | 142/145 97.93% | 35/36 97.22% | 35/36 97.22% |
| `dsh-doc-indexer-worker` | 1 | 2 | 5/5 | 5/5 | — | — |
| `dsh-keyword-extractor` | 2 | 2 | 5/5 | 5/5 | — | — |
| `dsh-top-sentences-extractor` | 2 | 2 | 5/5 | 5/5 | — | — |
| `dsh-doc-processor-worker` | 2 | 2 | 5/5 | 5/5 | — | — |
| `solr-terms-vector-order` | 22 | 22 | 88/88 | 88/88 | 30/30 | 30/30 |
| `solr-advanced-numbers-filter` | 10 | 10 | 24/24 | 24/24 | 14/14 | 14/14 |

"Before" rest-api and worker figures included context-started tests; "after" is unit tests alone.
No `TEST-*IT.xml` in any `target/surefire-reports`.

**A trap met on the way.** JaCoCo's agent appends to `target/jacoco.exec`, so a module build
without `clean` measures the previous run's data too. The first red run for Task 5 passed the gate
for that reason; every red and green run from then on used `clean install`.

### 7.2 Context checker (Task 2)

Test script at Task 2: 10 checks (the nine cases of Task 2, case 6 as two), all pass. Review rounds
added three more, for 13 in CI (the grep-failure check is skipped on Windows, where `chmod` does
not revoke read access):

- the local review: an empty tree fails, and a failing search fails;
- PR #121, Copilot: an `*IT` outside an `integration` package is not exempt.

Against the tree before Tasks
3–6 the checker exited 1 naming exactly the eight classes of §3; `DocumentTestConfiguration` did not
appear. After Task 6: `Every unit test is free of a Spring context.`, exit 0.

The draft pipeline through `xargs` was replaced by a direct `grep` over a `find -print0` array:
`xargs` folds grep's "no match" (1) and "error" (2) into the same 123, which made every clean case fail.

### 7.3 Red messages (Tasks 5, 6)

- `dsh-rest-api` with `DocumentResourceTest` moved and no controller unit test:
  `lines covered ratio is 0.83` and `branches covered ratio is 0.75, but expected minimum is 0.95`.
  With the controller test written, branches were still `0.91`: `SwaggerConfig.webMvcRequestHandlerProvider`
  had only ever been reached by context startup. `SwaggerConfigTest` covers its PathPatternParser
  filter with no context.
- Each worker with its smoke test moved: `enforce-coverage-data-exists` failed — *"This module has
  production sources … but produced no coverage data (no jacoco.exec in its build directory)"*.
- `mockStatic(SpringApplication.class)` works on the mixed Mockito classpath (§4.6); no seam, no
  version change.

### 7.4 Failsafe measurement (Task 7)

`mvn -B clean install -DintegrationTests` against the parent as it stood: both `dsh-rest-api` ITs
errored with `NoClassDefFoundError: com/mriss/dsh/restapi/service/DocumentQueueService` — §4.5's
`repackage` case. Fixed in parent-poms by `classesDirectory` =
`${project.build.outputDirectory}` on failsafe (light round trip, `6a60713c`). Rerun green: 6 IT
classes, 15 tests — `DocumentResourceIT` 6, `DshRestApplicationIT` 2, `DshDocIndexerApplicationIT` 1,
the three analyser ITs 2 each. `jacoco-it.exec` written in the five modules holding ITs. The aggregate
unit `jacoco.csv` is byte-identical before and after the IT run.

### 7.5 PowerMock probe (Task 8)

`org.powermock:powermock-api-mockito2:2.0.9`, test scope, added to `dsh-data` locally (never
committed):

- Before the ban: `mvn -B -pl dsh-data validate` → BUILD SUCCESS (the red).
- With the ban installed: exit 1, `enforce (ban-powermock) … PowerMock is forbidden in every MRISS
  project … org.powermock:powermock-api-mockito2:jar:2.0.9 <--- banned via the exclude/include list`.
- With `-Denforcer.skip=true`: the same failure.
- Dependency removed: DSH `mvn -B validate` green, `ban-powermock` ran in all 13 modules.

parent-poms commit: `d39ccae4`.

### 7.6 parent-poms round trip

Both commits (`6a60713c` failsafe `classesDirectory`, `d39ccae4` `ban-powermock`) pushed to
parent-poms `master` and deployed as `3.9.0-SNAPSHOT` by `deploy.yml`, `release_type: snapshots`:
[run 36203811843](https://github.com/MRISS-Projects/parent-poms/actions/runs/36203811843), success.
SHAs commented on `#112`:
[issuecomment-5841463833](https://github.com/MRISS-Projects/dsh/issues/112#issuecomment-5841463833).
Final `mvn -B clean install` against that parent: green, 8 coverage checks met, `ban-powermock` ran
in all 13 modules.

## 8. Acceptance criteria

- [ ] AC001 (amended): each of the eight classes in §3 is either rewritten so it starts no Spring
  context, or renamed `*IT` and moved to an `integration` package under its module's
  `src/test/java`. No class outside `*IT`/`*IntegrationTest` starts a Spring context.
- [ ] AC002: `mvn -B install` is green across all modules, and `coverage.data.check.skip` appears
  nowhere in the repository.
- [ ] AC003: each of the four worker modules runs at least one surefire unit test and produces
  `target/jacoco.exec`.
- [ ] AC004: `dsh-rest-api` and `dsh-data` hold ≥ 95% LINE and BRANCH from unit tests alone.
- [ ] AC005: `mvn -B install -DintegrationTests` runs every IT through failsafe and is green.
- [ ] AC006: any module-level JaCoCo `<excludes>` entry added carries a comment naming the class and
  why it cannot be unit-tested.
- [ ] AC007: `testing-patterns.md` records the rule in §2.1, the `integration` package convention,
  and that an integration test is mandatory when a task touches the REST API; `CLAUDE.md`'s
  quality-gates section stops describing integration tests as "not yet a separately enforced gate".
- [ ] AC008: `check-unit-tests-context-free.sh` and its test run in `ci.yml` ahead of the build; the
  test covers the nine cases in Task 2; the checker is green on the branch.
- [ ] AC009: parent-poms' `ban-powermock` execution fails `validate` for a project declaring an
  `org.powermock` dependency, with and without `-Denforcer.skip=true`; the SHA is commented on `#112`.
- [ ] AC010: `CLAUDE.md`, `testing-patterns.md` and parent-poms' `CLAUDE.md` state the PowerMock ban
  by name, with Mockito (including `mockStatic`) as the sanctioned tool; the "similar tools that
  require bytecode manipulation" clause is gone.

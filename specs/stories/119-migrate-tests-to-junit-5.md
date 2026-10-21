---
issue: 119
slug: migrate-tests-to-junit-5
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 119 — Migrate every test to JUnit 5 and drop the vintage engine

## 1. Story

**As a** developer writing DSH tests
**I want** every test in the repository on JUnit 5 (Jupiter), with the JUnit 4 vintage engine gone
**So that** tests use one framework, and a Spring Boot upgrade that drops the vintage engine cannot
silently stop JUnit 4 tests from running

## 2. Context

- Wave: 1 (`specs/product/PRD.md` §4), step 4 of the wave's order of work, the last item of that
  step. `#158`, `#140` and Wave 2 task 3 (`#177`) are closed, so `#119` migrates no MongoDB and no
  Solr test. `#120` follows it. DSH milestone `0.4.0-SNAPSHOT`.
- Issue: [#119](https://github.com/MRISS-Projects/dsh/issues/119), label `task`.
- Parent branch: `DEVELOP`, at `48827e587` when this spec was written. The task branch is
  `issue-119-migrate-tests-to-junit-5`.

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| Only `dsh-data` and `dsh-rest-api` have tests: 18 unit test classes and 4 ITs. `dsh-doc-indexer` and the two extractors are empty libraries since `#177` | `git ls-files 'dsh-*/src/test/java/*.java'` |
| Every test class is JUnit 4. The constructs in use: `@Test`, `@Test(expected = …)` once, `@Before`, `@After`, `@BeforeClass`, `@AfterClass`, `org.junit.Assert`, `@RunWith(MockitoJUnitRunner.class)` in 6 classes, `@RunWith(SpringRunner.class)` in 2 ITs. No `@Rule`, `@ClassRule`, `@Category`, `Parameterized`, `Assume` or `@FixMethodOrder` | the inventory in §3.4 |
| `junit:junit` 4.13.2 and `org.junit.vintage:junit-vintage-engine` 5.8.2 come from **DSH's own root `pom.xml`**, a `<dependencies>` block every module inherits. Nothing else brings either in. The issue says they arrive through `spring-boot-starter-test`; that is not so, since Boot 2.4 | `mvn dependency:tree -pl dsh-data,dsh-rest-api`, run 2026-10-21; `pom.xml` lines 59-72 |
| `junit-jupiter` 5.8.2 comes through `spring-boot-starter-test` 2.7.18, version from Boot's BOM (`junit-jupiter.version`) | the same tree; `spring-boot-dependencies-2.7.18.pom` |
| `junit.version` 4.13.2 and `mockito-core` at `mockito.version` 5.17.0 are managed in parent-poms' **root** `pom.xml` (`mriss-parent`), as explicit entries, so they beat Boot's BOM, which `products/pom.xml` imports. `mockito-junit-jupiter` is not managed there, so Boot's BOM gives it 4.5.1 | `mriss-parent-3.11.1.pom` lines 28-57 and 170-172; the tree above |
| Surefire and failsafe are 3.5.5, from parent-poms. Both already run on the JUnit Platform, through the vintage engine | `mriss-parent-3.11.1.pom` line 106 |
| `.github/scripts/check-unit-tests-context-free.sh` matches `org.springframework.test.context`, which also covers Jupiter's `SpringExtension` (`org.springframework.test.context.junit.jupiter`). It needs no change | the script's `PATTERN` |
| DSH's parent is the released `products:3.11.1`. parent-poms `master` is at `3.12.0-SNAPSHOT`, holding nothing past tag `mriss-parent-3.11.1` but two `docs(#110)` commits. Milestone `3.12.0-SNAPSHOT` carries `parent-poms#109` and `#90` | `git log mriss-parent-3.11.1..origin/master` in parent-poms; its milestones |
| The issue's "Affected Modules" names `dsh-doc-indexer-worker` and `dsh-solr`, both gone with `#177` | the issue body |

### 2.2 Two probes, run 2026-10-21

A throwaway project under the session scratchpad, parented on `products:3.11.1`, with
`spring-boot-starter-test` and one test class: `@ExtendWith(MockitoExtension.class)`, a `@Mock`, a
`@BeforeEach`, `when`/`verify` and `assertThrows`.

| Probe | Mockito extension | Jupiter | Result |
|---|---|---|---|
| P1, `mockito-junit-jupiter` managed at `${mockito.version}` | 5.17.0 | 5.8.2 | 2 tests run, pass |
| P2, unmanaged | 4.5.1, with `mockito-core` 5.17.0 | 5.8.2 | 2 tests run, pass |

So `mockito-junit-jupiter` 5.17.0, built against Jupiter 5.11.4, runs on Boot 2.7.18's 5.8.2, and
the mismatch AC006 removes is hygiene, not a live defect.

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-21

1. **No JUnit version is pinned anywhere.** Jupiter is what Boot's BOM manages: 5.8.2 today,
   whatever `#120`'s Boot line manages after it (5.12 for Boot 3.5, JUnit 6 for Boot 4; the
   `org.junit.jupiter` API carries over to both). A `junit-bom` pinned now would be a pin `#120`
   has to remove. So AC007's first clause does not apply: the JUnit version is not set in
   parent-poms.
2. **Mockito is aligned upstream.** `mockito-junit-jupiter` is managed in parent-poms' root
   `pom.xml` at `${mockito.version}`, beside `mockito-core`. A DSH-local entry was rejected: the
   gap is parent-poms' own, since it pins one Mockito artifact and not its sibling. Splitting AC006
   into a follow-up was rejected.
3. **The upstream change ships as 3.11.2, not 3.12.0.** `3.12.0-SNAPSHOT` has other work attached.
   parent-poms opens milestone `3.11.2-SNAPSHOT`, holding this change and, later, `#120`'s.
   `master` is re-versioned to `3.11.2-SNAPSHOT`, as for 3.11.1 (`efe85b4e`) and 3.9.2
   (`d5a80948`). DSH pins `3.11.2-SNAPSHOT` from this story through `#120`. **3.11.2 is released
   at the end of `#120`, and `#120` re-pins DSH to it.**
4. **Two upstream issues, one milestone.** This story opens one parent-poms issue for the Mockito
   entry. `#120` opens its own for whatever its analysis moves in parent-poms. An issue is closed
   when its change is verified in DSH, which needs a known scope; `#120`'s is not known yet. The
   milestone is what binds the two into the one release.
5. **An enforcer ban replaces a CI grep.** `enforce-no-junit4` bans `junit:junit` and
   `org.junit.vintage:*`. That is AC002, mechanically. It is also AC001: with `junit:junit` off
   every classpath, an `org.junit.Test`, `Assert`, `Before*`, `After*`, `runner.RunWith` or `Rule`
   import fails to compile, so CI's `mvn -B install` fails. A grep script was rejected: it would
   detect only what the compiler already rejects.
6. **The tests are migrated by hand.** 22 classes and one mapping table (§3.4). OpenRewrite was
   rejected: the review costs the same, and its recipes rewrite more than the mapping (POMs,
   visibility) for one use.

### 3.2 Upstream: parent-poms

The issue to open in `MRISS-Projects/parent-poms`, label `task`, milestone `3.11.2-SNAPSHOT`
(created with it):

> **Manage mockito-junit-jupiter at mockito-core's version**
>
> The root `pom.xml` manages `org.mockito:mockito-core` at `${mockito.version}` (5.17.0), and
> nothing manages `org.mockito:mockito-junit-jupiter`. A product that uses `MockitoExtension` gets
> it from Spring Boot's BOM, imported by `products/pom.xml`: 4.5.1 under Boot 2.7.18. So the
> extension and the core it drives come from two Mockito lines.
>
> Add a managed `mockito-junit-jupiter` at `${mockito.version}`, test scope, beside
> `mockito-core`. One property then sets both, whatever Boot's BOM says.
>
> Asked for by `MRISS-Projects/dsh#119`, which moves DSH's tests to JUnit 5. Shipped as hotfix
> 3.11.2: `master` is re-versioned to `3.11.2-SNAPSHOT`. DSH's `#120` adds its own issue to the
> same milestone, and 3.11.2 is released when that one is done.

The change, in the root `pom.xml`'s `dependencyManagement`, after the `mockito-core` entry, with
its indentation (spaces):

```xml
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-junit-jupiter</artifactId>
                <version>${mockito.version}</version>
                <scope>test</scope>
            </dependency>
```

The re-version: the eight POMs at `3.12.0-SNAPSHOT` go to `3.11.2-SNAPSHOT`, by hand, as
`efe85b4e` did, not with `set-version.sh`: `pom.xml`, `products/pom.xml`, `framework/pom.xml`,
`infrastructure/pom.xml`, `infrastructure/announcement-templates/pom.xml`,
`infrastructure/maven-archetypes/pom.xml`, `infrastructure/maven-plugins/pom.xml`,
`infrastructure/skins/pom.xml`.

parent-poms' own process applies (`parent-poms-specs-on-master`): a task branch off `master`, a spec
at `specs/<n>-manage-mockito-junit-jupiter.md` laid out as `specs/110-filter-classpath-resources-with-at-delimiter.md`
is, with this section as its design, then a pull request. After the owner merges it, `deploy.yml`
is dispatched with `release_type: snapshots`, so that DSH's CI resolves `3.11.2-SNAPSHOT`. No
release in this story.

The issue is [`parent-poms#112`](https://github.com/MRISS-Projects/parent-poms/issues/112), in
milestone `3.11.2-SNAPSHOT`, both opened on 2026-10-21 after the owner approved this spec.

### 3.3 DSH: the build

**The pin.** The root `pom.xml`'s `<parent>` version goes from `3.11.1` to `3.11.2-SNAPSHOT`, and
the comment above it says why and until when:

```xml
    <!-- #119: parent-poms 3.11.2-SNAPSHOT, the hotfix line that carries #119's and #120's upstream
         changes. #120 re-pins to the released 3.11.2. CI passes -U while the pin lasts; see
         docs/devops/README.md, "Parent POM". -->
```

**`-U` comes back** in `ci.yml` (`mvn -B -U install --file pom.xml`) and `api-testing.yml`
(`mvn -B -U install -DskipTests \`), as `e5b2895b4` did for `#158`. `ci.yml`'s comment above the
step, which today says "No -U: the parent is a released version (#150)", is rewritten:

```yaml
      # -U while the parent is pinned to parent-poms 3.11.2-SNAPSHOT (#119, until
      # #120 re-pins to the released 3.11.2), here and in api-testing.yml: Maven
      # otherwise refreshes SNAPSHOT metadata on its own daily schedule, and the
      # ~/.m2 cache restored by setup-java varies in age, so the parent moves
      # invisibly. Drop it with the re-pin. See docs/devops/README.md, "Parent POM".
```

**The JUnit 4 block goes.** The root `pom.xml`'s `<dependencies>` block (the `junit` and
`junit-vintage-engine` entries and the two comments) is deleted whole. No module declares either
itself.

**The ban.** A third execution of `maven-enforcer-plugin` in the root `pom.xml`, after
`enforce-no-solr`, four-space indentation:

```xml
                    <execution>
                        <id>enforce-no-junit4</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <bannedDependencies>
                                    <excludes>
                                        <exclude>junit:junit</exclude>
                                        <exclude>org.junit.vintage:*</exclude>
                                    </excludes>
                                    <message>Every DSH test is JUnit 5 (#119): no module may depend on JUnit 4 or the vintage engine, directly or transitively. A library that drags junit:junit in needs an exclusion, or a version that does not.</message>
                                </bannedDependencies>
                            </rules>
                            <fail>true</fail>
                        </configuration>
                    </execution>
```

`bannedDependencies` searches every scope and transitive dependencies by default. The plugin's
comment, which since `#177` names both DSH rules, is updated to name three. `-Denforcer.skip=true`
disables this rule with the others, as `CLAUDE.md` already says of them; it is updated to list
`enforce-no-junit4` too.

**What the ban will catch later.** Testcontainers 1.x depends on `junit:junit` at compile scope,
because its `GenericContainer` implements JUnit 4's `TestRule`. If `#137` or a GCP-emulator story
adds it, the ban fails the build, and that story chooses Testcontainers 2.x or an exclusion. That is
the intended outcome.

### 3.4 DSH: the tests

**The mapping.** Applied to the 22 classes; nothing else in a test changes.

| JUnit 4 | Jupiter |
|---|---|
| `org.junit.Test` | `org.junit.jupiter.api.Test` |
| `org.junit.Before` / `After` | `org.junit.jupiter.api.BeforeEach` / `AfterEach` |
| `org.junit.BeforeClass` / `AfterClass` | `org.junit.jupiter.api.BeforeAll` / `AfterAll` (the methods are already `static`) |
| `static org.junit.Assert.<m>` | `static org.junit.jupiter.api.Assertions.<m>`, one import per method, as today |
| `org.junit.Assert.assertEquals(…)`, fully qualified (`DocumentResourceIT:311`) | `assertEquals(…)`, statically imported like its neighbours |
| `@Test(expected = X.class)` (`DocumentSubmissionServiceImplTest:67`) | `@Test` and `assertThrows(X.class, () -> …)` around the one call |
| `@RunWith(MockitoJUnitRunner.class)` | `@ExtendWith(MockitoExtension.class)` (`org.junit.jupiter.api.extension.ExtendWith`, `org.mockito.junit.jupiter.MockitoExtension`) |
| `@RunWith(SpringRunner.class)` beside `@SpringBootTest` | deleted, with its two imports: `@SpringBootTest` is meta-annotated `@ExtendWith(SpringExtension.class)` |

Visibility stays as it is. Jupiter allows package-private test classes and methods; changing them
is a rewrite this story does not make.

**Message arguments move last.** JUnit 4's `Assert` takes an optional message first, and Jupiter's
`Assertions` last. Every converted call with a message is rewritten
`assertX(expected, actual, message)`, `assertX(actual, message)`, `assertX(condition, message)`,
or for doubles `assertEquals(expected, actual, delta, message)`.

35 calls carry a message. Left in JUnit 4 order under Jupiter, 29 fail to compile, but **six
compile**, because their message and their values are all `String`s:

| Call, as today | Read by Jupiter as | Effect |
|---|---|---|
| `DocumentStatusDiagramTest:68` `assertNotEquals("self-loop: " + line, ends[0], ends[1])` | unexpected = the message, actual = `ends[0]` | **always passes**: the two ends are never compared |
| `DocumentStatusTest:29` `assertNotNull(status + " has no message", message)` | actual = the message literal | **always passes** |
| `DocumentHandlingServiceImplTest:63` `assertNotNull("Returned id must not be null", id)` | actual = the literal | **always passes** |
| `DocumentSubmissionServiceImplTest:79` `assertNotNull("Token must not be null", token)` | actual = the literal | **always passes** |
| `DocumentQueueServiceImplTest:73` `assertEquals("Payload must be preserved", payload, result.getPayload())` | expected = the literal, actual = `payload` | fails |
| `DocumentSubmissionServiceImplTest:106` `assertEquals("Should return cached document token", cached.getToken(), token)` | expected = the literal | fails |

The first four are why a green build does not prove this migration right, and why every call below
is reordered and read, not only those the compiler flags. The 35, by start line:

| Class | Lines |
|---|---|
| `DocumentStatusDiagramTest` | 68, 72, 75, 86, 87, 90, 94 |
| `DocumentStatusTest` | 29, 30, 32 |
| `DocumentTest` | 150, 167 |
| `KeywordTest` | 28 |
| `SentenceTest` | 33 |
| `ClasspathResourceFilteringTest` | 24, 31, 39, 42 |
| `DshRestApplicationTest` | 42, 83 |
| `TestConfigurationFilesTest` | 42, 51, 55 |
| `DocumentHandlingServiceImplTest` | 63, 81, 86 |
| `DocumentQueueServiceImplTest` | 72, 73 |
| `DocumentSubmissionServiceImplTest` | 79, 106 |
| `DocumentResourceIT` | 130 |
| `DshRestApplicationIT` | 50, 59, 60, 71 |

The list was produced on 2026-10-21 by parsing every `assert*` call in the classes that import
`org.junit.Assert`, joining multi-line calls and skipping string literals, and keeping those with
one argument more than the assertion needs.

**Mockito strictness.** `MockitoExtension` defaults to `Strictness.STRICT_STUBS`, which also fails
a stub called with arguments other than those it was given (`PotentialStubbingProblem`).
`MockitoJUnitRunner` does not. If a migrated test fails that way, the stub is corrected to the call
the code really makes, or marked `lenient()` on that one stubbing when a shared `@BeforeEach` stub is
legitimately unused by some tests. A class-wide `@MockitoSettings(strictness = LENIENT)` is not
allowed. Neither changes what a test asserts.

**The classes.** Test methods counted by `@Test`:

| Module | Class | `@Test` | JUnit 4 constructs beyond `@Test` and `Assert` |
|---|---|---|---|
| `dsh-data` | `DshDataApplicationTest` | 1 | `@BeforeClass`, `@AfterClass` |
| | `InMemoryDocumentDaoTest` | 21 | `@Before` (assertions are AssertJ) |
| | `DocumentStatusDiagramTest` | 2 | messages, `fail` |
| | `DocumentStatusTest` | 4 | messages |
| | `DocumentTest` | 19 | `@Before`, messages |
| | `KeywordSorterTest`, `KeywordTest`, `SentenceSorterTest`, `SentenceTest` | 4, 8, 4, 8 | messages in `KeywordTest`, `SentenceTest` |
| `dsh-rest-api` | `ClasspathResourceFilteringTest` | 3 | messages |
| | `DshRestApplicationTest` | 5 | messages |
| | `TestConfigurationFilesTest` | 2 | messages |
| | `config/SwaggerConfigTest` | 1 | |
| | `rest/DocumentResourceTest` | 6 | `MockitoJUnitRunner` |
| | `service/DocumentEnqueueResponseMessageHandlerTest` | 5 | `MockitoJUnitRunner`, `@Before` |
| | `service/DocumentHandlingServiceImplTest` | 3 | `MockitoJUnitRunner`, `@Before`, messages |
| | `service/DocumentQueueServiceImplTest` | 2 | `MockitoJUnitRunner`, `@Before`, messages |
| | `service/DocumentSubmissionExceptionTest` | 6 | |
| | `service/DocumentSubmissionServiceImplTest` | 11 | `MockitoJUnitRunner`, `@Before`, `@Test(expected)`, messages |
| | `integration/DocumentResourceHttpIT` | 5 | `@Before` (assertions are AssertJ) |
| | `integration/DocumentResourceIT` | 7 | `SpringRunner`, `@Before`, a message, the qualified `assertEquals` |
| | `integration/DshRestApplicationIT` | 3 | `SpringRunner`, messages |
| | `integration/PersistenceSelectionIT` | 4 | `@After` (assertions are AssertJ) |

That is 71 unit test methods in `dsh-data`, 44 in `dsh-rest-api`, and 19 IT methods. Task 1's
surefire and failsafe counts are the baseline AC003 and AC004 compare against, not these.

### 3.5 The documents

| File | Change |
|---|---|
| `.github/copilot/rules/testing-patterns.md` | "Frameworks & Libraries": JUnit 5 (Jupiter) with `@Test`, `@BeforeEach`, `@ExtendWith`, and the vintage engine banned by `enforce-no-junit4`; Mockito with `MockitoExtension` in place of `MockitoJUnitRunner`. The controller example loses `@RunWith(SpringRunner.class)`. "Clean up test data in `@After`" becomes `@AfterEach`. "a JUnit 4 `@Category(PerformanceTest.class)`" becomes "a JUnit 5 `@Tag("performance")`" |
| `CLAUDE.md` | "Commands": the parent is `3.11.2-SNAPSHOT` since `#119`, until `#120` re-pins to the released 3.11.2, and both workflows pass `-U` while it lasts. "Quality gates": the `-Denforcer.skip=true` paragraph names `enforce-no-junit4` with the other two rules |
| `docs/devops/README.md` | "Parent POM": the current pin is `3.11.2-SNAPSHOT` since `#119`, its exit `#120`; both workflows pass `-U`. The `ci.yml` row of the workflow table names `mvn -B -U install` |

**AC007's second clause** — "`testing-patterns.md`'s examples compile against the result" — is
checked by reading: the examples use classes that do not exist (`DocumentController`,
`DocumentFixture`), so they cannot be compiled. Every annotation and static method an example uses
must exist in `junit-jupiter-api` 5.8.2, `spring-boot-test` 2.7.18 or AssertJ, and no example uses
a JUnit 4 type.

### 3.6 Out of scope

- The Spring Boot upgrade, and releasing parent-poms 3.11.2: `#120`.
- Changing what any test asserts, or moving tests to AssertJ.
- New tests. No production code changes, so coverage stays as it is.
- `parent-poms`' managed `junit:junit`. Other products may still use it.
- Historical records: the ADRs, the PRD, `specs/stories/`, `docs/superpowers/plans/`.

### 3.7 Risks

- **Pushing before the snapshot is deployed fails CI.** DSH's CI resolves `3.11.2-SNAPSHOT` from
  GitHub Packages, which has it only after the parent-poms pull request is merged and `deploy.yml`
  has run. Locally, `mvn -B install` in parent-poms is enough. So step 6 waits for the deploy.
- **parent-poms `master` is held at `3.11.2-SNAPSHOT`** through `#119` and `#120`. `#109` and `#90`
  wait for 3.11.2 to ship, which moves `master` to `3.12.0-SNAPSHOT` again. The owner accepted it.
- **A message left first can compile and pass vacuously** (§3.4, four calls). The build cannot
  catch it; Tasks 5 and 6 reorder every listed call, and step 5's review reads them.
- **STRICT_STUBS fails a test that passed** (§3.4). It is fixed in the stub, never by loosening the
  whole class.
- **Stale `target/` trees** hold compiled JUnit 4 classes. The gates always run `clean`.

### 3.8 Follow-ups

1. **A comment on `#120`**, approved by the owner on 2026-10-21 and posted at Task 9, once the pin
   it describes has landed:

   > `#119` pinned DSH to parent-poms `3.11.2-SNAPSHOT`, a hotfix line cut for `#119` and this
   > story, with `ci.yml` and `api-testing.yml` passing `-U`. Its upstream change is
   > `parent-poms#112`, in milestone `3.11.2-SNAPSHOT`. This story opens its own parent-poms issue
   > in the same milestone for what it moves there, and ends by releasing 3.11.2 (milestone renamed
   > `3.11.2` first), re-pinning DSH to it and dropping `-U`. Decided while specifying `#119`, on
   > 2026-10-21.

2. **The issue body of `#119`**: its "Affected Modules" and the source of the vintage engine are
   stale (§2.1). Corrected in the issue on 2026-10-21, with the owner's approval, each correction
   marked as such in the body.

## 4. Acceptance criteria and verification

| AC | Check | Expected | Where |
|---|---|---|---|
| AC001 | `git grep -n -E 'import (static )?org\.junit\.(Test\|Assert\|Before\|After\|Rule\|runner\.)' -- '*.java'`; the red run of the ban | no hit; the red run fails at the root module, naming `junit:junit` | Tasks 4, 8 |
| AC002 | `mvn dependency:tree -Dincludes=junit,org.junit.vintage` over the reactor | no artifact listed in any module | Task 8 |
| AC003 | `mvn -B clean install`; surefire totals per module against Task 1's | `maven exit=0`; each module's total equal to its baseline | Tasks 1, 8 |
| AC004 | `mvn -B clean install -DintegrationTests`, Docker running; failsafe totals | `maven exit=0`; the IT total equal to its baseline | Tasks 1, 8 |
| AC005 | the two builds above | `jacoco:check` and the coverage-data guard pass in every module | Task 8 |
| AC006 | `mvn dependency:tree -Dincludes=org.mockito -pl dsh-data,dsh-rest-api` | baseline: `mockito-junit-jupiter:4.5.1`; after: `5.17.0`, equal to `mockito-core` | Tasks 1, 3, 8 |
| AC007 | §3.1 decision 1 for the first clause; the reading of §3.5 for the second; `parent-poms#112` referenced in this spec and on `#119` | no JUnit version in parent-poms; every example uses Jupiter, Boot-test 2.7.18 or AssertJ types only | Tasks 2, 7, 8 |

Gate 3 applies: the story changes `dsh-rest-api`'s ITs and the parent every module builds against.
AC004 runs `-DintegrationTests` over the whole reactor, which covers it.

## 5. Files to change

| Repository | File | Change |
|---|---|---|
| parent-poms | the eight POMs of §3.2 | re-versioned to `3.11.2-SNAPSHOT` |
| parent-poms | `pom.xml` | the managed `mockito-junit-jupiter` |
| parent-poms | `specs/112-manage-mockito-junit-jupiter.md` | its spec |
| dsh | `pom.xml` | the pin, the JUnit 4 block deleted, `enforce-no-junit4`, the plugin comment |
| dsh | `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml` | `-U`; `ci.yml`'s comment |
| dsh | the 22 test classes of §3.4 | migrated |
| dsh | `.github/copilot/rules/testing-patterns.md`, `CLAUDE.md`, `docs/devops/README.md` | §3.5 |
| dsh | `specs/stories/119-migrate-tests-to-junit-5.md` | this spec, §7 filled in |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** every DSH test runs on Jupiter, no module can depend on JUnit 4 or the vintage engine, and
Mockito's extension and core come from one version, through parent-poms `3.11.2-SNAPSHOT`.

**Architecture:** the upstream change first, so that DSH builds against it. Then DSH re-pins, the
ban is shown red, the two modules' tests are migrated while JUnit 4 is still on the classpath (each
commit builds), and the last commit deletes JUnit 4 and adds the ban, green.

**Tech stack:** Maven 3.9.16, `maven-enforcer-plugin` 3.5.0 and surefire/failsafe 3.5.5 from
parent-poms, Spring Boot 2.7.18 (Jupiter 5.8.2), Mockito 5.17.0, Java 17.

**Spec:** this file.

### Global constraints

- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"). Never pipe `mvn` into `tail`. Maven is `~/apps/apache-maven-3.9.16`; its local
  repository is `D:\.m2\repository`.
- `-DintegrationTests` starts RabbitMQ in Docker. If `docker info` fails, start Docker Desktop from
  `D:\Docker\app` and wait until `docker info` succeeds.
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- Indentation: the root `pom.xml` uses four spaces; test classes keep the indentation they have
  (`InMemoryDocumentDaoTest`, `KeywordTest`, `SentenceTest`, `DocumentResourceHttpIT` and
  `PersistenceSelectionIT` use tabs).
- Never pass `-Denforcer.skip=true`, never set `coverage.data.check.skip`, never weaken a test.
- DSH commits are `build(#119): …`, `test(#119): …`, `ci(#119): …` and `docs(#119): …`;
  parent-poms commits are `build(#112): …` and `docs(#112): …`. Each ends with the session's attribution
  lines. Nothing is pushed in DSH before step 6. Claude neither merges a pull request nor closes an
  issue, in either repository.

### Task 1: Baseline (AC003, AC004, AC006)

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

  Expected: `maven exit=0`.
- [x] **Step 3:** Record the totals, one per module for surefire and one for failsafe (per-class
  lines carry " - in "), and the Mockito tree:

  ```bash
  grep -E '^\[(INFO|WARNING|ERROR)\] (Building DSH|Tests run: [0-9]+)' .logs/mvn-clean-install-baseline.log | grep -v ' - in '
  grep -E '^\[(INFO|WARNING|ERROR)\] (Building DSH|Tests run: [0-9]+|--- failsafe)' .logs/mvn-clean-install-it-baseline.log | grep -v ' - in '
  mvn -B -o dependency:tree -pl dsh-data,dsh-rest-api -Dincludes=org.mockito > .logs/mvn-dependency-tree-baseline.log 2>&1; echo "maven exit=$?"
  grep -E 'mockito' .logs/mvn-dependency-tree-baseline.log
  ```

  Expected: `mockito-core:5.17.0` and `mockito-junit-jupiter:4.5.1` in both modules. Write all
  three into §7.

### Task 2: Upstream, parent-poms (AC006, AC007)

Run in `C:/Users/marce/github/parent-poms`. The issue and the milestone are opened only after the
owner approves this spec, which approves §3.2's text.

**Files:** the eight POMs of §3.2; `pom.xml`; `specs/112-manage-mockito-junit-jupiter.md`.

- [x] **Step 1:** Create milestone `3.11.2-SNAPSHOT` and the issue of §3.2 in it:

  ```bash
  gh api repos/MRISS-Projects/parent-poms/milestones -f title=3.11.2-SNAPSHOT -f state=open
  gh issue create -R MRISS-Projects/parent-poms --label task --milestone 3.11.2-SNAPSHOT \
    --title "Manage mockito-junit-jupiter at mockito-core's version" --body-file "$BODY"
  ```

  `$BODY` is a file in the session scratchpad holding §3.2's quoted issue body, from "The root
  `pom.xml` manages" to "when that one is done.", with the blockquote marker removed from each line.

  Done on 2026-10-21: milestone 26, issue `parent-poms#112`. Its number was written into this spec,
  and the spec was committed only then.
- [x] **Step 2:** Branch: `git checkout master && git pull && git checkout -b issue-112-manage-mockito-junit-jupiter`.
  Write the spec at `specs/112-manage-mockito-junit-jupiter.md` in `#110`'s layout: the header
  table (issue, milestone `3.11.2-SNAPSHOT`, branch, "Asked for by `MRISS-Projects/dsh#119`"), goal,
  "What is true today" from §2.1's Mockito rows and §2.2, the design of §3.2, ACs (the entry is
  managed; DSH resolves `mockito-junit-jupiter` 5.17.0 against `3.11.2-SNAPSHOT`), and tasks. Commit
  `docs(#112): spec for managing mockito-junit-jupiter`.
- [x] **Step 3:** Re-version the eight POMs from `3.12.0-SNAPSHOT` to `3.11.2-SNAPSHOT` by hand.
  `git grep -n '3.12.0-SNAPSHOT' -- '*pom.xml'` finds nothing; `git diff --stat` shows 8 files, 8
  lines each way. Commit `build(#112): re-version master to 3.11.2-SNAPSHOT for the hotfix line`.
- [x] **Step 4:** Add the entry of §3.2 after `mockito-core`'s, then install:

  ```bash
  mvn -B install > .logs/mvn-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ls /d/.m2/repository/com/mriss/mriss-parent/products/3.11.2-SNAPSHOT/
  ```

  Expected: `maven exit=0`, and `products-3.11.2-SNAPSHOT.pom` present. Commit
  `build(#112): manage mockito-junit-jupiter at mockito-core's version`.
- [x] **Step 5:** Push and open the pull request into `master`, after the owner approves its text.
  Its merge and the snapshot deploy (Task 9) are needed only before DSH pushes.

### Task 3: DSH re-pins to 3.11.2-SNAPSHOT (AC006)

**Files:** `pom.xml`, `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml`, `CLAUDE.md`,
`docs/devops/README.md`.

- [x] **Step 1:** In `pom.xml`, the `<parent>` version becomes `3.11.2-SNAPSHOT` and the comment above
  it is §3.3's.
- [x] **Step 2:** `ci.yml`: `run: mvn -B -U install --file pom.xml`, and the comment above it is
  §3.3's. `api-testing.yml`: `mvn -B -U install -DskipTests \`.
- [x] **Step 3:** `CLAUDE.md` and `docs/devops/README.md` as §3.5 says for the pin, `-U` and the
  workflow table. Leave `CLAUDE.md`'s enforcer paragraph for Task 7.
- [x] **Step 4:** Verify, against the parent installed by Task 2:

  ```bash
  mvn -B -o dependency:tree -pl dsh-data,dsh-rest-api -Dincludes=org.mockito > .logs/mvn-dependency-tree-pinned.log 2>&1; echo "maven exit=$?"
  grep -E 'mockito' .logs/mvn-dependency-tree-pinned.log
  mvn -B clean install > .logs/mvn-clean-install-pinned.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-pinned.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `mockito-junit-jupiter:5.17.0` in both modules; `maven exit=0`, with the baseline's test
  totals (JUnit 4 still runs through the vintage engine and the 5.17.0 jar is unused so far).
- [x] **Step 5:** Commit `build(#119): pin the parent to parent-poms 3.11.2-SNAPSHOT, with -U while it lasts`.

### Task 4: The ban, red (AC001, AC002)

**Files:** `pom.xml`.

- [x] **Step 1:** Insert §3.3's `enforce-no-junit4` execution after `enforce-no-solr`'s
  `</execution>`.
- [x] **Step 2:** Run it, expecting failure:

  ```bash
  mvn -B clean install > .logs/mvn-clean-install-red.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-red.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -n -E 'enforce-no-junit4|junit:junit|junit-vintage|BUILD FAILURE' .logs/mvn-clean-install-red.log | head -20
  ```

  Expected: `maven exit=1`, at the first module, in `enforce (enforce-no-junit4)`, naming
  `junit:junit:jar:4.13.2` and `junit-vintage-engine`. Record the lines in §7.
- [x] **Step 3:** Remove the execution again (`git checkout -- pom.xml`; Task 3 is committed, so
  only the execution is undone). The ban returns in Task 7, so that Tasks 5 and 6 each commit a
  building tree.

### Task 5: Migrate `dsh-data` (AC001, AC003)

**Files:** the nine `dsh-data` classes of §3.4.

- [x] **Step 1:** Apply §3.4's mapping to each class, and move the message last at every
  `dsh-data` line of §3.4's table. The two silent ones become
  `assertNotEquals(ends[0], ends[1], "self-loop: " + line);` (`DocumentStatusDiagramTest:68`) and
  `assertNotNull(message, status + " has no message");` (`DocumentStatusTest:29`). In `DocumentTest`
  lines 150 and 167, `KeywordTest:28` and `SentenceTest:33`, the double form, e.g.
  `assertEquals(2.0, docAllArgsConstructor.getKeyWords().first().getScore(), 0.0, "keyword score validation");`.
- [x] **Step 2:** Check no JUnit 4 is left and every message is last:

  ```bash
  git grep -n -E 'org\.junit\.(Test|Assert|Before|After|runner|Rule)\b' -- dsh-data
  git grep -n -E 'assert[A-Za-z]*\(\s*"' -- dsh-data
  ```

  Expected: no hit on the first. The second lists only calls whose first argument is a genuine
  expected value (a string literal compared to an actual one, as `DocumentTest:101`). Read each.
- [x] **Step 3:** Build the module:

  ```bash
  mvn -B clean install -pl dsh-data > .logs/mvn-clean-install-dsh-data.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-dsh-data.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -E '^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+' .logs/mvn-clean-install-dsh-data.log | grep -v ' - in '
  ```

  Expected: `maven exit=0`, and the total equal to `dsh-data`'s baseline.
- [x] **Step 4:** Commit `test(#119): move dsh-data's tests to JUnit 5`.

### Task 6: Migrate `dsh-rest-api` (AC001, AC003, AC004)

**Files:** the thirteen `dsh-rest-api` classes of §3.4.

- [x] **Step 1:** Apply §3.4's mapping. In `DocumentSubmissionServiceImplTest`, `testException`
  becomes:

  ```java
      @Test
      public void testException() {
          assertThrows(DocumentSubmissionException.class,
                  () -> service.getTokenFromDocument(null, "Russia-Trump: FBI chief Wray defends agency", false));
      }
  ```

  with `import static org.junit.jupiter.api.Assertions.assertThrows;`. In `DocumentResourceIT` and
  `DshRestApplicationIT`, delete `@RunWith(SpringRunner.class)` and the imports of `RunWith` and
  `SpringRunner`; replace `org.junit.Assert.assertEquals(` at `DocumentResourceIT:311` with
  `assertEquals(`, adding its static import. Move the message last at every `dsh-rest-api` line of
  §3.4's table; the four that compile in either order become
  `assertNotNull(id, "Returned id must not be null");` (`DocumentHandlingServiceImplTest:63`),
  `assertNotNull(token, "Token must not be null");` (`DocumentSubmissionServiceImplTest:79`),
  `assertEquals(payload, result.getPayload(), "Payload must be preserved");`
  (`DocumentQueueServiceImplTest:73`) and
  `assertEquals(cached.getToken(), token, "Should return cached document token");`
  (`DocumentSubmissionServiceImplTest:106`).
- [x] **Step 2:** The two checks of Task 5 step 2, over `dsh-rest-api`.
- [x] **Step 3:** Build, unit then integration, Docker running:

  ```bash
  mvn -B clean install -pl dsh-rest-api > .logs/mvn-clean-install-dsh-rest-api.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-dsh-rest-api.log"
  wait $MVN_PID; echo "maven exit=$?"
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api > .logs/mvn-clean-verify-it-dsh-rest-api.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-verify-it-dsh-rest-api.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: both `maven exit=0`; the surefire total and the failsafe total equal to the baseline's.
  A `PotentialStubbingProblem` or `UnnecessaryStubbingException` is fixed as §3.4 says.
- [x] **Step 4:** Commit `test(#119): move dsh-rest-api's tests to JUnit 5`.

### Task 7: JUnit 4 out, the ban in, green (AC001, AC002)

**Files:** `pom.xml`, `CLAUDE.md`.

- [x] **Step 1:** Delete the root `pom.xml`'s `<dependencies>` block (§3.3). Re-insert
  `enforce-no-junit4` after `enforce-no-solr`. Update the enforcer plugin's comment to name the three
  DSH rules, and `CLAUDE.md`'s `-Denforcer.skip=true` paragraph to name `enforce-no-junit4` with
  `enforce-lowercase-artifact-id` and `enforce-no-solr`.
- [x] **Step 2:** Gate 1:

  ```bash
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`; every module's surefire total equal to its baseline. A compile error
  here is a JUnit 4 import Tasks 5 and 6 missed.
- [x] **Step 3:** Commit `build(#119): ban JUnit 4 and the vintage engine`.

### Task 8: Documents and the gates (all ACs)

**Files:** `.github/copilot/rules/testing-patterns.md`.

- [x] **Step 1:** Edit `testing-patterns.md` as §3.5 says. Check its examples against §3.5's AC007
  reading. Commit `docs(#119): testing patterns describe JUnit 5`.
- [x] **Step 2:** The gates and the checks of §4:

  ```bash
  mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-it.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-it.log"
  wait $MVN_PID; echo "maven exit=$?"
  mvn -B -o dependency:tree -Dincludes=junit,org.junit.vintage,org.mockito > .logs/mvn-dependency-tree.log 2>&1; echo "maven exit=$?"
  grep -E 'junit:junit|junit-vintage|mockito' .logs/mvn-dependency-tree.log
  git grep -n -E 'import (static )?org\.junit\.(Test|Assert|Before|After|Rule|runner\.)' -- '*.java'
  bash .github/scripts/check-unit-tests-context-free.sh
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json
  ```

  Expected: `maven exit=0` with the baseline's failsafe total and every coverage check passing; the
  tree lists `mockito-core:5.17.0` and `mockito-junit-jupiter:5.17.0` and no `junit:junit` or
  `junit-vintage`; no grep hit; the checker passes; lint exit 0.
- [x] **Step 3:** Fill §7 with the numbers, and commit `docs(#119): record the verification`.

### Task 9: Before step 6

- [x] **Step 1:** After the owner merges the parent-poms pull request, dispatch its `deploy.yml`
  with `release_type: snapshots`, and wait for the run to succeed:

  ```bash
  gh workflow run deploy.yml -R MRISS-Projects/parent-poms --ref master -f release_type=snapshots
  ```

- [x] **Step 2:** Comment on `#119` naming `parent-poms#112`, its merge commit and the deploy run.
  Post §3.8's comment on `#120`, approved on 2026-10-21.

## 7. Verification results

### Task 1: baseline, 2026-10-21, at `cea54730f`

| Run | Log | Result |
|---|---|---|
| `mvn -B clean install` | `.logs/mvn-clean-install-baseline.log` | `maven exit=0`; surefire `dsh-data` 71, `dsh-rest-api` 44 |
| `mvn -B clean install -DintegrationTests` | `.logs/mvn-clean-install-it-baseline.log` | `maven exit=0`; surefire as above; failsafe `dsh-rest-api` 19: `DocumentResourceHttpIT` 5, `DocumentResourceIT` 7, `DshRestApplicationIT` 3, `PersistenceSelectionIT` 4 |
| `dependency:tree -Dincludes=org.mockito` | `.logs/mvn-dependency-tree-baseline.log` | both modules: `mockito-core:5.17.0`, `mockito-junit-jupiter:4.5.1` |

The totals equal §3.4's `@Test` counts.

### Task 2: upstream, parent-poms `#112`

Branch `issue-112-manage-mockito-junit-jupiter`, cut from `master` at `1dab0893`: its spec
(`ee41dbb5`), the re-version of the eight POMs, 8 lines each way (`4e0aa3b3`), the managed entry
(`6073d6ce`), and the spec's Task 1 ticked (`5b87fa15`). `mvn -B clean install` there exited 0, and
the installed `mriss-parent-3.11.2-SNAPSHOT.pom` carries `mockito-junit-jupiter` at
`${mockito.version}`. Pushed, and opened as pull request `parent-poms#113` on 2026-10-21, after the
owner approved its text. Its merge and the snapshot deploy are Task 9's.

### Tasks 3 to 8, 2026-10-21

| AC | Run | Result |
|---|---|---|
| AC006 | `dependency:tree` after the re-pin (`a6cc15bea`), `.logs/mvn-dependency-tree-pinned.log` | both modules: `mockito-core:5.17.0`, `mockito-junit-jupiter:5.17.0`. `mvn -B clean install` on the pin: exit 0, 71 and 44 |
| AC001, AC002 | the ban, red, `.logs/mvn-clean-install-red.log` | `maven exit=1` at the root module `dsh`, `enforce (enforce-no-junit4)`: `junit:junit:jar:4.13.2` and `org.junit.vintage:junit-vintage-engine:jar:5.8.2 <--- banned` |
| AC003 | `dsh-data` migrated (`3bed51faa`), `.logs/mvn-clean-install-dsh-data.log` | exit 0; 71 tests; coverage met |
| AC003, AC004 | `dsh-rest-api` migrated (`53a351157`), `.logs/mvn-clean-install-dsh-rest-api.log`, `.logs/mvn-clean-verify-it-dsh-rest-api.log` | exit 0 both; 44 unit tests, 19 ITs. No `PotentialStubbingProblem` or `UnnecessaryStubbingException` under STRICT_STUBS |
| AC001, AC002 | JUnit 4 out, the ban in (`200fae117`), `.logs/mvn-clean-install.log` | exit 0; `enforce-no-junit4` ran in all 9 modules; 71 and 44; coverage met in both |
| AC001–AC006 | `mvn -B clean install -DintegrationTests` at `efa82b08a`, `.logs/mvn-clean-install-it.log` | exit 0; 71, 44 and 19, each equal to the baseline; coverage met; no `dsh-it-` container left |
| AC002, AC006 | `dependency:tree -Dincludes=junit,org.junit.vintage,org.mockito` over the reactor | no `junit:junit` or `junit-vintage` in any of the 9 modules; `mockito-core` and `mockito-junit-jupiter` 5.17.0 |
| AC001 | `git grep` for JUnit 4 imports | no hit |
| AC007 | §3.1 decision 1; `testing-patterns.md` read against §3.5 | no JUnit version in parent-poms. The examples use `@Test`, `@WebMvcTest`, `@MockBean`, `@Autowired`, MockMvc's builders and matchers, Mockito's `when`/`any` and AssertJ's `assertThat`, all on the resulting classpath; none uses a JUnit 4 type |
| — | `check-unit-tests-context-free.test.sh` and `.sh`; markdownlint; `check-spec-references.sh` | 12 cases pass, 1 skipped on Windows as before; the checker passes; lint exit 0; 0 missing paths |

**The message order.** Every one of §3.4's 35 calls was reordered. A parse of every test class after
Task 6, the same parse that produced §3.4's list, counted the 35 calls carrying a message and found
none with a string message still in first place.

### 7.1 What the spec missed

- **`.github/copilot/prompts/test-generation.md`** asked for JUnit 4 tests, `@Category` and
  `@Test(timeout = …)`, which no longer compile here. It asks for Jupiter's now (`bbd379e56`). Its
  integration-test prompt kept "Clean up test data in @After" until step 5's review found it; that
  is `@AfterEach` too, and nothing else of JUnit 4 is left in `.github/copilot/prompts/`.
- **`check-unit-tests-context-free.test.sh`** built its cases from `SpringRunner` and
  `MockitoJUnitRunner`. They use `SpringExtension` and `MockitoExtension` now, and the checker's
  comment names `SpringExtension` (`efa82b08a`). The checker's pattern is unchanged; §2.1 was right
  that it covers the Jupiter form, and the violating cases, which expect exit 1, prove it.
- **`testing-patterns.md`'s `-Denforcer.skip=true` line** named only `enforce-lowercase-artifact-id`,
  stale since `#177`. It names the three DSH rules now, with `CLAUDE.md`'s.

### Task 9, 2026-10-21

The owner merged `parent-poms#113` as `e38be1c8`. `deploy.yml` with `release_type: snapshots` ran
as 39611868727 and succeeded. Resolved into an empty local repository
(`mvn -B -N -U validate -Dmaven.repo.local=…`, `.logs/mvn-validate-remote-parent.log`), the parent
downloaded from GitHub Packages as `products-3.11.2-20261021.135351-1.pom`, and its root POM
carries `mockito-junit-jupiter`. The record is commented on `#119`, and §3.8's comment is posted on
`#120`.

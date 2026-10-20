---
issue: 140
slug: rest-api-test-profile-file
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 140 — Give dsh-rest-api's tests a `test` profile file

## 1. Story

**As a** developer adding a setting to `dsh-rest-api`
**I want** its context tests to read the main `application.properties`, with a `test` profile's
file for what differs
**So that** each setting is written once, and a test cannot drift from what the jar runs with

## 2. Context

- Wave: 1, step 4 of the order of work (`specs/product/PRD.md` §4, Wave 1), triaged issue. DSH
  milestone `0.4.0-SNAPSHOT`.
- Issue: [#140](https://github.com/MRISS-Projects/dsh/issues/140). Its present scope came from
  `#48`: finding S8, probes P22 to P25 and P29, rule R10 and follow-up candidate 4 of
  `specs/architecture/profiles-and-configuration.md`.
- Parent branch: `DEVELOP`, at `1888bdc63` when this spec was written. The task branch is
  `issue-140-rest-api-test-profile-file`.

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| `src/test/resources/application.properties` holds four settings, and all four are in the main file with the same values. `#167` removed the `spring.data.mongodb.*` lines from both, so nothing is left that differs | the two files |
| A test `application.properties` replaces the main one, so the test classpath has two and Spring reads only the test one | P24; `ClasspathResourceFilteringTest`'s Javadoc |
| No context test names a profile, so a profile leaked from the shell or the Maven command line reaches it | P22, P29; §7 of the profiles document |
| Three tests start a Spring context in the test JVM: `DshRestApplicationIT` and `DocumentResourceIT`, through `SpringRunner` and `@SpringBootTest`, and `PersistenceSelectionIT`, through `new SpringApplicationBuilder(DshRestApplication.class).run(…)`. `@ActiveProfiles` does not reach the third | `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration` |
| `DocumentResourceHttpIT` starts no context: it calls the server that the `http-integration-tests` Maven profile forks, and `spring-boot:start` runs that server on the main classpath only | `dsh-rest-api/pom.xml`, profile `http-integration-tests` |
| `src/test/resources` is not filtered. `dsh-rest-api/pom.xml` declares `<resources>` and no `<testResources>` | `dsh-rest-api/pom.xml:109-116` |

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-20

1. **`application-test.properties` is created with a comment and no setting.** Nothing differs
   today. The file is where the next test value goes, and its comment says so. Deleting the test
   file and relying on a profile with no file was rejected: it works (P31), but it leaves no sign
   of where a test value belongs.
2. **A guard unit test pins the arrangement.** Only the main `application.properties` is on the
   test classpath, and the profile's file repeats none of its keys. The issue's done condition,
   "no property is written in both files", becomes a check that keeps holding.
3. **`PersistenceSelectionIT` names the profile too**, as the argument
   `--spring.profiles.active=test`, because `@ActiveProfiles` does not reach a
   `SpringApplicationBuilder`. A command-line argument outranks a system property and an
   environment variable, so a leaked profile cannot replace it. R10 says every context test.

### 3.2 The files

`git mv dsh-rest-api/src/test/resources/application.properties
dsh-rest-api/src/test/resources/application-test.properties`, then its content becomes:

```properties
# The test profile's settings (#140). Every context test of dsh-rest-api names this profile:
# @ActiveProfiles("test"), or --spring.profiles.active=test where the test starts the application
# itself. Spring reads this file after src/main/resources/application.properties and merges the
# two (P25 in specs/architecture/profiles-and-configuration.md). Put here only what a test needs
# to differ from the main file; today nothing does. Never repeat a key of the main file, and never
# add an application.properties to src/test/resources: it would replace the main one entirely
# (P24). TestConfigurationFilesTest fails on either.
```

The main `application.properties` does not change.

### 3.3 The context tests

| Test | Change | New check |
|---|---|---|
| `DshRestApplicationIT` | `@ActiveProfiles("test")`; an `@Autowired Environment` | `activeProfileIsTestOnly`: the active profiles are exactly `{"test"}` |
| `DocumentResourceIT` | `@ActiveProfiles("test")` | `testActiveProfile`: `webApplicationContext.getEnvironment().getActiveProfiles()` is exactly `{"test"}` |
| `PersistenceSelectionIT` | `start()` passes `--spring.profiles.active=test` after `--server.port=0`; the Javadoc says why | `startup_shouldRunWithTheTestProfileOnly` |

The existing cases keep their assertions. `PersistenceSelectionIT`'s first case, which checks that
`dsh.persistence` is absent, is the one a leaked profile with its own file could break.

### 3.4 The guard: `TestConfigurationFilesTest`

A unit test in `com.mriss.dsh.restapi`, beside `ClasspathResourceFilteringTest`. It reads files
with `java.util.Properties` and the class loader, starts no context, and so passes
`check-unit-tests-context-free.sh`.

- `applicationProperties_onlyTheMainFileIsOnTheClasspath`: `ClassLoader.getResources("application.properties")`,
  each URL turned into a `Path`, equals the single path `application.properties` inside the
  directory `DshRestApplication` was loaded from.
- `testProfileFile_repeatsNoKeyOfTheMainFile`: `application-test.properties` is on the classpath,
  and the keys it shares with the main file are none.

The helper that finds the main output directory repeats `ClasspathResourceFilteringTest`'s. Three
lines in two tests of one package do not earn a shared class.

`ClasspathResourceFilteringTest`'s Javadoc says the main output is read "because src/test/resources
holds an application.properties that shadows the main one". That stops being true. It becomes:
read from the directory `DshRestApplication` was loaded from, so that nothing on the test classpath
can stand in for the main output.

### 3.5 Documentation

`specs/architecture/profiles-and-configuration.md`:

- Finding 8, line 39: "That is why `dsh-rest-api`'s repeats it line for line." becomes "That is why
  `dsh-rest-api`'s repeated it line for line, until `#140`."
- §7, after the table: "Today no layer-2 test has `@ActiveProfiles`, no profile is passed to the
  forked server, …" becomes "Since `#140`, every layer-2 test of `dsh-rest-api` names the `test`
  profile, and its test values have `application-test.properties` to go in. No profile is passed
  to the forked server, …". The rest of the sentence stays.

The §2 table, "On `DEVELOP` at `a760552f8`", is a dated snapshot, and §9.2 records what each
follow-up became. Neither changes.

### 3.6 Out of scope

- The server the `http-integration-tests` profile forks. It is layer 3, its profile comes as an
  argument from the Maven profile (§7 of the profiles document), and `#137` reworks it.
- `--spring.profiles.active=test` in `api-testing.yml`. It is `#137`'s AC005. It is unaffected:
  `application-test.properties` is a test resource and is not in the jar.
- The two `property.properties` files, main and test. Both are empty and nothing reads them. Owner's
  call whether that becomes an issue.
- Other modules. None has a test `application*.properties`.

## 4. Verification design

| AC | Check | Where |
|---|---|---|
| AC001 | No `application.properties` in `src/test/resources`; `application-test.properties` exists and repeats no key of the main file. `TestConfigurationFilesTest` fails before the change and passes after | Tasks 2 and 3 |
| AC002 | The three context tests run with exactly the `test` profile, and with the main file's values. Their new checks fail before the change and pass after; their other cases still pass | Tasks 2 and 3 |
| AC003 | A leaked profile does not reach them: the three ITs pass with `SPRING_PROFILES_ACTIVE=other` and `-Dspring.profiles.active=other` (P23, repeated on the module) | Task 5 |
| AC004 | Gate 1, `mvn -B clean install`, and gate 3, `mvn -B clean verify -DintegrationTests -pl dsh-rest-api -amd`, exit 0 | Task 5 |
| AC005 | §3.4's Javadoc and §3.5's two sentences; markdownlint passes | Task 4 |

"With the main file's values" is covered by AC001's first check together with AC002: with one
`application.properties` on the classpath, and that one the main file, a context reads the main
file's values. No setting today differs between the two, so a value in the context cannot tell the
files apart; the classpath can.

## 5. Files to change

| File | Change |
|---|---|
| `dsh-rest-api/src/test/resources/application.properties` → `application-test.properties` | renamed; comment only (§3.2) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/TestConfigurationFilesTest.java` | new (§3.4) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/ClasspathResourceFilteringTest.java` | Javadoc (§3.4) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DshRestApplicationIT.java` | §3.3 |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/DocumentResourceIT.java` | §3.3 |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PersistenceSelectionIT.java` | §3.3 |
| `specs/architecture/profiles-and-configuration.md` | §3.5 |
| `specs/stories/140-rest-api-test-profile-file.md` | this spec, §7 filled in |

No production source changes, so the coverage gate measures what it measured before.

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** `dsh-rest-api`'s context tests read the main `application.properties` merged with an
`application-test.properties` that repeats none of it, under a `test` profile they name
themselves.

**Architecture:** the test `application.properties` is renamed to the `test` profile's file and
emptied of duplicates. The two `SpringRunner` tests name the profile by annotation; the test that
builds its own application names it by argument. A unit test pins the classpath and the key sets.

**Tech stack:** Spring Boot 2.7.18, JUnit 4, AssertJ, Maven 3.9.16 with surefire and failsafe,
Java 17.

**Spec:** this file.

### Global constraints

- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"). The Maven local repository is `D:\.m2\repository`.
- `-DintegrationTests` starts RabbitMQ in Docker. If `docker info` fails, start Docker Desktop from
  `D:\Docker\app` and wait for `docker info` to succeed.
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- Keep each file's indentation: `DshRestApplicationIT`, `DocumentResourceIT` and the two tests in
  `com.mriss.dsh.restapi` use four spaces; `PersistenceSelectionIT` uses tabs. Its snippets below
  are shown with spaces, for markdownlint, and go into the file with tabs.
- Commit messages are `test(#140): …` and `docs(#140): …`, ending with the session's attribution
  lines. Nothing is pushed before step 6.

### Task 1: Baseline

- [x] **Step 1:** Gate 1 on the untouched branch, which also installs every upstream module:

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`. Record it in §7.

### Task 2: The checks, red (AC001, AC002)

**Files:** create `TestConfigurationFilesTest.java`; modify `DshRestApplicationIT.java`,
`DocumentResourceIT.java`, `PersistenceSelectionIT.java` (new checks only).

- [x] **Step 1:** Create
  `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/TestConfigurationFilesTest.java`:

  ```java
  package com.mriss.dsh.restapi;

  import static org.junit.Assert.assertEquals;
  import static org.junit.Assert.assertNotNull;
  import static org.junit.Assert.assertTrue;

  import java.io.IOException;
  import java.io.Reader;
  import java.net.URISyntaxException;
  import java.net.URL;
  import java.nio.charset.StandardCharsets;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.nio.file.Paths;
  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.List;
  import java.util.Properties;
  import java.util.Set;
  import java.util.TreeSet;

  import org.junit.Test;

  /**
   * Checks the configuration files dsh-rest-api's context tests read (#140, rule R10 of
   * specs/architecture/profiles-and-configuration.md). A test application.properties would replace
   * the main one entirely (P24), so test values go in application-test.properties, which merges
   * with it (P25) and must repeat none of its keys.
   */
  public class TestConfigurationFilesTest {

      @Test
      public void applicationProperties_onlyTheMainFileIsOnTheClasspath() throws Exception {
          List<Path> found = new ArrayList<>();
          for (URL url : Collections.list(
                  getClass().getClassLoader().getResources("application.properties"))) {
              found.add(Paths.get(url.toURI()));
          }
          assertEquals("only the main application.properties may be on the test classpath: "
                  + "test values go in application-test.properties (#140)",
                  Collections.singletonList(mainDirectory().resolve("application.properties")),
                  found);
      }

      @Test
      public void testProfileFile_repeatsNoKeyOfTheMainFile() throws Exception {
          URL testFile = getClass().getClassLoader().getResource("application-test.properties");
          assertNotNull("application-test.properties is missing from the test classpath (#140)",
                  testFile);
          Set<String> repeated = new TreeSet<>(load(Paths.get(testFile.toURI())).stringPropertyNames());
          repeated.retainAll(load(mainDirectory().resolve("application.properties")).stringPropertyNames());
          assertTrue("application-test.properties repeats keys of the main application.properties, "
                  + "which it is merged with (P25): " + repeated, repeated.isEmpty());
      }

      private static Properties load(Path file) throws IOException {
          Properties properties = new Properties();
          try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
              properties.load(reader);
          }
          return properties;
      }

      private static Path mainDirectory() throws URISyntaxException {
          return Paths.get(
                  DshRestApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      }
  }
  ```

- [x] **Step 2:** In `DshRestApplicationIT`, add the imports
  `static org.junit.Assert.assertArrayEquals` and `org.springframework.core.env.Environment`, the
  field, and the case, after `mockedInfrastructureBeansArePresent`:

  ```java
      @Autowired
      private Environment environment;

      /**
       * Verifies that the context runs with the test profile it names, and no other (#140).
       */
      @Test
      public void activeProfileIsTestOnly() {
          assertArrayEquals("the context must run with the test profile only",
                  new String[] {"test"}, environment.getActiveProfiles());
      }
  ```

- [x] **Step 3:** In `DocumentResourceIT`, add the import `static org.junit.Assert.assertArrayEquals`
  and, before the `POST /v1/dsh/document/submit` banner, the case:

  ```java
      /**
       * The context must run with the test profile it names, and no other (#140).
       */
      @Test
      public void testActiveProfile() {
          assertArrayEquals("the context must run with the test profile only",
                  new String[] {"test"}, webApplicationContext.getEnvironment().getActiveProfiles());
      }
  ```

- [x] **Step 4:** In `PersistenceSelectionIT`, after
  `startup_whenPersistenceMatchesNoStore_shouldFail`:

  ```java
      @Test
      public void startup_shouldRunWithTheTestProfileOnly() {
          context = start();

          assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("test");
      }
  ```

- [x] **Step 5:** Run red. Both test layers must run, so failures are recorded rather than fatal:

  ```bash
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api -Dmaven.test.failure.ignore=true \
    > .logs/mvn-verify-red.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-red.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -h "Tests run:.*Fail\|<<< FAIL" dsh-rest-api/target/surefire-reports/*.txt \
    dsh-rest-api/target/failsafe-reports/*.txt
  ```

  Expected failures, and no others: `applicationProperties_onlyTheMainFileIsOnTheClasspath` (two
  paths found), `testProfileFile_repeatsNoKeyOfTheMainFile` (file missing), `activeProfileIsTestOnly`,
  `testActiveProfile` and `startup_shouldRunWithTheTestProfileOnly` (no active profile). Record the
  lines in §7. The Maven exit code may be non-zero if the coverage check fails; that is not the
  evidence here.

- [x] **Step 6:** Commit: `test(#140): pin the test profile and the configuration files, red`.

### Task 3: The profile's file and the profile, green (AC001, AC002)

**Files:** rename `application.properties` → `application-test.properties`; modify the three ITs
(profile) and `ClasspathResourceFilteringTest.java` (Javadoc).

- [x] **Step 1:** `git mv dsh-rest-api/src/test/resources/application.properties
  dsh-rest-api/src/test/resources/application-test.properties`, then replace its content with
  §3.2's comment.

- [x] **Step 2:** On `DshRestApplicationIT` and `DocumentResourceIT`, add
  `import org.springframework.test.context.ActiveProfiles;` and `@ActiveProfiles("test")` directly
  after `@SpringBootTest`.

- [x] **Step 3:** In `PersistenceSelectionIT`, `start()` becomes:

  ```java
      private static ConfigurableApplicationContext start(String... arguments) {
          String[] all = new String[arguments.length + 2];
          all[0] = "--server.port=0";
          all[1] = "--spring.profiles.active=test";
          System.arraycopy(arguments, 0, all, 2, arguments.length);
          return new SpringApplicationBuilder(DshRestApplication.class).run(all);
      }
  ```

  and the class Javadoc's last sentence becomes: "Starts the application as {@code main} does,
  once per case, with the {@code test} profile as an argument: {@code @ActiveProfiles} does not
  reach a {@link SpringApplicationBuilder}, and an argument outranks a profile leaked from the
  environment (#140)."

- [x] **Step 4:** In `ClasspathResourceFilteringTest`, the Javadoc's second sentence becomes: "The
  files are read from the directory {@link DshRestApplication} was loaded from, so that nothing on
  the test classpath can stand in for the main output."

- [x] **Step 5:** Run green, failures fatal this time:

  ```bash
  mvn -B clean verify -DintegrationTests -pl dsh-rest-api > .logs/mvn-verify-green.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-green.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`, and the five cases of Task 2 pass. Record in §7.

- [x] **Step 6:** Commit: `test(#140): read the main application.properties under a test profile`.

### Task 4: Documentation (AC005)

- [x] **Step 1:** Make §3.5's two edits to `specs/architecture/profiles-and-configuration.md`.
- [x] **Step 2:** markdownlint:

  ```bash
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' \
    'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json
  ```

  Expected: no output, exit 0.
- [x] **Step 3:** Commit: `docs(#140): record that dsh-rest-api's context tests name their profile`.

### Task 5: The gates and the leak (AC003, AC004)

- [x] **Step 1:** Gate 1, `mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as in
  Task 1. Expected: `maven exit=0`.
- [x] **Step 2:** Gate 3, logged to `.logs/mvn-verify-integration.log`:
  `mvn -B clean verify -DintegrationTests -pl dsh-rest-api -amd`. No module depends on
  `dsh-rest-api`, so `-amd` adds none. Expected: `maven exit=0`.
- [x] **Step 3:** The leak, logged to `.logs/mvn-verify-leak.log`:

  ```bash
  SPRING_PROFILES_ACTIVE=other mvn -B clean verify -DintegrationTests -pl dsh-rest-api \
    -Dspring.profiles.active=other > .logs/mvn-verify-leak.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-verify-leak.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`; the three profile checks pass, so each context ran with `test` and not
  `other`. The forked server inherits `other`, which has no file and selects nothing (P31).
- [x] **Step 4:** Fill in §7 and commit: `docs(#140): record the verification`.

## 7. Verification results

Run on 2026-10-20, in Git Bash on Windows 11, Maven 3.9.16, Java 17, Docker Desktop running.

| Step | Command | Result |
|---|---|---|
| Task 1, baseline | `mvn -B clean install`, at `a97b44e0f` | `maven exit=0`, BUILD SUCCESS |
| Task 2, red | `mvn -B clean verify -DintegrationTests -pl dsh-rest-api -Dmaven.test.failure.ignore=true` | `maven exit=0`; the five new cases fail, and no other (below) |
| Task 3, green | `mvn -B clean verify -DintegrationTests -pl dsh-rest-api` | `maven exit=0`; 44 unit and 19 integration tests pass; "All coverage checks have been met." |
| Task 4, AC005 | markdownlint, the command in `CLAUDE.md` | no output, exit 0 |
| Task 5, gate 1 (AC004) | `mvn -B clean install` | `maven exit=0`, BUILD SUCCESS |
| Task 5, gate 3 (AC004) | `mvn -B clean verify -DintegrationTests -pl dsh-rest-api -amd` | `maven exit=0`; 44 unit and 19 integration tests pass |
| Task 5, the leak (AC003) | `SPRING_PROFILES_ACTIVE=other mvn -B clean verify -DintegrationTests -pl dsh-rest-api -Dspring.profiles.active=other` | `maven exit=0`; the four ITs pass, the three profile checks among them |

The red failures, each for the reason §6 expects:

```text
TestConfigurationFilesTest.applicationProperties_onlyTheMainFileIsOnTheClasspath
  expected:<[…\target\classes\application.properties]>
  but was:<[…\target\test-classes\application.properties, …\target\classes\application.properties]>
TestConfigurationFilesTest.testProfileFile_repeatsNoKeyOfTheMainFile
  application-test.properties is missing from the test classpath (#140)
DshRestApplicationIT.activeProfileIsTestOnly
  expected.length=1 actual.length=0; expected:<test> but was:<end of array>
DocumentResourceIT.testActiveProfile
  expected.length=1 actual.length=0; expected:<test> but was:<end of array>
PersistenceSelectionIT.startup_shouldRunWithTheTestProfileOnly
  Expecting actual: [] to contain exactly (and in same order): ["test"]
```

The leak reached the build: `.logs/mvn-verify-leak.log` has "The following 1 profile is active:
"other"" once, from the server the `http-integration-tests` profile forks, and "The following 1
profile is active: "test"" six times, once per context started in the test JVM.

One correction to Task 5, step 2: `-amd` does add a module. `dsh-coverage-report` depends on
`dsh-rest-api`, so the reactor was `dsh-rest-api` then `dsh-coverage-report`. It has no
integration tests, and the run is unaffected.

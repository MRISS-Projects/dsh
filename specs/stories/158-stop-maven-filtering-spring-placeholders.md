---
issue: 158
slug: stop-maven-filtering-spring-placeholders
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 158 — Stop Maven from filtering `${…}` in Spring configuration

## 1. Story

**As a** developer writing a Spring property that is derived from another
**I want** Maven's resource filtering to leave `${…}` alone in the files Spring reads
**So that** a placeholder is resolved once, by Spring at run time, and never by whichever Maven
property a build happens to define

## 2. Context

- Wave: 1, step 4 of the order of work (`specs/product/PRD.md` §4, Wave 1), triaged issue. DSH
  milestone `0.4.0-SNAPSHOT`.
- Issue: [#158](https://github.com/MRISS-Projects/dsh/issues/158). It was spun off from `#48`:
  finding M1, probes P18 to P20 and rule R9 of `specs/architecture/profiles-and-configuration.md`,
  follow-up candidate 1 of its §9.2.
- Parent branch: `DEVELOP`, at `09647d43c` when this spec was written. The task branch is
  `issue-158-stop-maven-filtering-spring-placeholders`.

### 2.1 What exists today

**Every module filters `src/main/resources` with both default delimiters.** Twelve module POMs
list `src/main/resources` in `<build><resources>`, each with `<filtering>true</filtering>`:
`dsh-coverage-report`, `dsh-data`, `dsh-doc-analyser` and its three children,
`dsh-doc-indexer-worker`, `dsh-rest-api`, `dsh-solr` and its two children, and `dsh-test-dataset`.
The root `pom.xml` declares `<build><resources>` too, with only the two `src/site` blocks. Neither parent-poms POM declares
`<build><resources>` or a delimiter, so `resources:resources` uses `${…}` and `@…@`.

**Nothing in `src/main/resources` uses filtering.** `#167` removed `dsh-data`'s `mongo.properties`
and `dshApplicationContext.xml`, the only files that did. What is left:

| Module | Files | Tokens |
|---|---|---|
| `dsh-rest-api` | `application.properties`, `enqueue-docId-context.xml`, `logback.xml`, `property.properties` | `${spring.rabbitmq.host:localhost}` and `${spring.rabbitmq.port:5672}` in the XML context, line 45, both meant for Spring |
| `dsh-data`, `dsh-doc-processor-worker`, `dsh-keyword-extractor`, `dsh-top-sentences-extractor`, `dsh-doc-indexer-worker` | `logback.xml`, and an empty `property.properties` | none |
| the other seven | nothing | — |

The two RabbitMQ placeholders are already in the hazard M1 describes. They survive only because no
Maven property is named `spring.rabbitmq.host:localhost`.

**The site Markdown reaches the classpath executions too.** Every module except the root also lists
two `src/site` blocks in `<build><resources>`, with a `targetPath` of `target/generated-site`, so
`default-resources` filters Markdown as well. Those blocks duplicate an execution every module
already has: the root's `copy-site-resources-for-pdf` (`pom.xml:138`), which is inherited, copies
the same files to the same place and filters the Markdown with `${…}`. The effective POM of
`dsh-rest-api` shows all three executions: `default-resources`, `default-testResources` and
`copy-site-resources-for-pdf`.

**Only one Markdown file uses `${…}` on purpose:** the root `src/site/markdown/README.md`, with
`${release.type}`, `${project.build.version}` and `${issues.text.list}`. The generated `README.md`
comes from parent-poms' `copy-readme-md` execution in the `readme-generation` profile, a third
execution with its own ID.

**Stamping does not need filtering.** M2 and R8 say that the build stamps the version and the
commit, and that `spring-boot:build-info` does it.

### 2.2 What was established while writing this spec

- **`dsh-rest-api`'s test classpath shadows its main `application.properties`.**
  `src/test/resources/application.properties` exists (`#140` is about it), and `target/test-classes`
  comes before `target/classes`. A check that looks the resource up by name reads the test copy.
  The check in §3.4 reads the main output directory itself.
- **Leaving out `<resources>` inherits the nearest ancestor's.** `dsh-doc-analyser` and `dsh-solr`
  are aggregators whose own `<resources>` would reach their children. So every level is cleaned
  together, and a module with no `<resources>` gets the Super POM default: `src/main/resources`,
  unfiltered.
- **The aggregators' own `copy-resources` executions** (`dsh-doc-analyser/pom.xml:65`,
  `dsh-solr/pom.xml:97`) have their own IDs, and nothing here changes them.
- **What the re-pin brings from 3.11.0.** Between `mriss-parent-3.10.0` and `mriss-parent-3.11.0`
  the POMs change only their version lines (15 files, one line each). 3.11.0's work, `#98` and
  `#106`, is in the reusable workflows, which DSH already calls at `@master`.
- **parent-poms' `master` holds nothing beyond 3.11.0.** `git log mriss-parent-3.11.0..origin/master`
  is empty, so the hotfix line is `master` re-versioned, as it was for 3.9.2 (`d5a80948`, "re-version
  master to 3.9.2-SNAPSHOT for the hotfix line"). `deploy.yml` with `release_type: releases` releases
  it and advances `master` to the next MINOR, which is `3.12.0-SNAPSHOT` again.
- **An effective POM prints resolved `settings.xml` values**, a GitHub token among them
  (`maven-changes-plugin`'s `personalToken`). One was read into the transcript while this spec was
  written, and the file was deleted. Any later effective-POM read is narrowed to the
  `maven-resources-plugin` section before anything is printed.

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-20

1. **The filters that are not used are removed.** No `src/main/resources` file needs filtering, so
   every module drops `<filtering>true</filtering>`. The one exception is `dsh-rest-api`, which keeps
   it to exercise the check (§3.4).
2. **Filtering stays possible, with `@…@` only, for every file on the classpath.** A module can turn
   filtering back on at any time. When it does, only `@name@` is replaced, and `${…}` always reaches
   the jar as written. This is AC002's answer: **`@…@` only**, not "no filtering". "No filtering"
   would have shut a door the project may need, and "`@…@` only" removes the hazard just as well.
3. **The rule covers the classpath executions, and Markdown is out.** It is set on
   `default-resources` and `default-testResources`, the two executions that write `target/classes`
   and `target/test-classes`. Every `copy-resources` execution keeps the default delimiters:
   `copy-site-resources-for-pdf`, `copy-readme-md` and the aggregators' `copy-resources`. The site
   Markdown and the README keep `${…}`. The delimiter is therefore set inside those two executions,
   never in the plugin's own `<configuration>`, which every execution inherits.
   `spring-boot-starter-parent` sets it at plugin level, and following it there would take `${…}`
   away from the README.
4. **The rule lives in parent-poms `products/pom.xml`.** `products` imports `spring-boot-dependencies`
   and is the Spring Boot parent of every product. `spring-boot-starter-parent` sets the same rule in
   its parent (`resource.delimiter=@`, `useDefaultDelimiters=false`), and DSH misses it only because
   it does not inherit that parent.
5. **It ships as a parent-poms hotfix, 3.11.1, not with 3.12.0.** Its parent-poms issue is the only
   issue on a new `3.11.1-SNAPSHOT` milestone. The full round trip of `CLAUDE.md` runs: issue,
   milestone, change tested against `3.11.1-SNAPSHOT`, a temporary DSH pin on that `SNAPSHOT`, the
   3.11.1 release, then the re-pin. 3.12.0's open issues (`#90`, and `#109` in Wave 4) do not hold it
   up.

### 3.2 parent-poms: the rule

In `products/pom.xml`, `<build><pluginManagement><plugins>` gains this entry, after
`spring-boot-maven-plugin`. The version comes from the root parent-poms `pluginManagement`
(`${resources.plugin.version}`).

```xml
<!-- Classpath resources are filtered with @...@ only, as spring-boot-starter-parent does:
     ${...} is Spring's placeholder syntax, and Maven must leave it for Spring to resolve
     at run time. Only the two lifecycle executions are configured, so a copy-resources
     execution, such as the ones that filter site Markdown with ${...}, keeps both
     delimiters. A product turns filtering on per resource; this sets only what is replaced. -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>default-resources</id>
            <configuration>
                <useDefaultDelimiters>false</useDefaultDelimiters>
                <delimiters>
                    <delimiter>@</delimiter>
                </delimiters>
            </configuration>
        </execution>
        <execution>
            <id>default-testResources</id>
            <configuration>
                <useDefaultDelimiters>false</useDefaultDelimiters>
                <delimiters>
                    <delimiter>@</delimiter>
                </delimiters>
            </configuration>
        </execution>
    </executions>
</plugin>
```

`pluginManagement` executions merge into the lifecycle's executions by ID. The DSH check of §3.4
proves that the merge reaches a product, because it fails on 3.10.0 and passes on 3.11.1-SNAPSHOT.

The rest follows parent-poms' own conventions: a task branch off `master`, a spec at
`specs/<n>-<slug>.md` (parent-poms keeps its specs on `master`), and the re-version commit on the
same branch, made by hand on the eight POMs at `3.12.0-SNAPSHOT`, as for 3.9.2 (§7.2).

### 3.3 DSH: the `<resources>` blocks

In every POM listed in §2.1, and in the root:

- the two `src/site` blocks and their `FR009` comment are deleted;
- `<filtering>true</filtering>` is deleted from `src/main/resources`;
- where nothing is left but `src/main/resources`, the whole `<resources>` element is deleted.

Two modules keep a block:

**`dsh-rest-api`**, so that the check of §3.4 has a filtered file to read:

```xml
<resources>
    <!-- Filtered with @...@ only, by the rule inherited from parent-poms products (#158).
         application.properties carries the probe ClasspathResourceFilteringTest reads. -->
    <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
    </resource>
</resources>
```

**`dsh-test-dataset`**, which packages its PDFs from `src/test/resources`. The block keeps its two
directories, unfiltered as today, without the site blocks and without the `<filtering>` line.

The Markdown still reaches `target/generated-site` through `copy-site-resources-for-pdf`, filtered
with `${…}` as before. §4 checks that by comparing the trees.

### 3.4 DSH: the check

**The probe.** `dsh-rest-api/src/main/resources/application.properties` starts with:

```properties
# Maven filters this file with the at-sign delimiter only (#158). So ${project.version} reaches
# the jar as written, for Spring to resolve, while @project.version@ is replaced by the version
# this jar was built from. ClasspathResourceFilteringTest fails if either one changes.
```

`project.version` is defined in every build, so the probe does not depend on any `-D` or
`settings.xml`. If `${…}` were ever replaced again, line 1 would lose its placeholder in every
build. Spring ignores comment lines. The comment has no lone at-sign, so no token can start in it
by accident.

**The test.** `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/ClasspathResourceFilteringTest.java`,
JUnit 4 like the rest of the module, with no Spring context. It reads the files from the directory
`DshRestApplication` was loaded from, `target/classes`, because of the shadowing in §2.2.

```java
package com.mriss.dsh.restapi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Checks what Maven's resource filtering leaves in dsh-rest-api's main output (#158).
 * The files are read from the directory {@link DshRestApplication} was loaded from,
 * because src/test/resources holds an application.properties that shadows the main one.
 */
public class ClasspathResourceFilteringTest {

    @Test
    public void applicationProperties_springPlaceholder_reachesTheJarAsWritten() throws Exception {
        assertTrue("Maven replaced ${project.version} in application.properties: "
                + "classpath resources must be filtered with @...@ only (#158)",
                mainResource("application.properties").contains("${project.version}"));
    }

    @Test
    public void applicationProperties_mavenToken_isReplaced() throws Exception {
        assertFalse("@project.version@ was not replaced: dsh-rest-api's src/main/resources "
                + "must stay filtered, so that the rule of #158 is exercised by every build",
                mainResource("application.properties").contains("@project.version@"));
    }

    @Test
    public void enqueueContext_springPlaceholders_reachTheJarAsWritten() throws Exception {
        String context = mainResource("enqueue-docId-context.xml");
        assertTrue(context.contains("${spring.rabbitmq.host:localhost}"));
        assertTrue(context.contains("${spring.rabbitmq.port:5672}"));
    }

    private static String mainResource(String name) throws IOException, URISyntaxException {
        Path classes = Paths.get(
                DshRestApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return new String(Files.readAllBytes(classes.resolve(name)), StandardCharsets.UTF_8);
    }
}
```

**Red, then green.** On 3.10.0, once the probe is in, the first test fails: both delimiters are
active, so `${project.version}` becomes the version. On 3.11.1-SNAPSHOT all three pass. The third
test passes on both, and it is there to keep the existing placeholders pinned.

### 3.5 DSH: where the rule is written down (AC005)

- **`.github/copilot/rules/java-conventions.md`**, "Spring Boot Patterns". It is where a developer
  adding a property reads the Spring rules. A bullet is added after the `@ConfigurationProperties`
  one:

  ```markdown
  - Write a Spring placeholder as `${…}` in any file on the classpath; Maven never replaces it.
    Classpath resources are filtered with `@…@` only, by a rule inherited from parent-poms
    `products` (`#158`). For a build value, write `@name@` and turn on `<filtering>` for that
    module's `src/main/resources`. The site Markdown is not on the classpath and keeps `${…}`
  ```

- **The probe comment** in `application.properties` (§3.4) says the same where the file is edited.

### 3.6 DSH: the parent pin

| When | Root `pom.xml` parent | `ci.yml`, `api-testing.yml` | `CLAUDE.md`, `docs/devops/README.md` |
|---|---|---|---|
| Today | `3.10.0` | no `-U` | "the released `3.10.0`" |
| While parent-poms is unreleased | `3.11.1-SNAPSHOT` | `-U` added to the one `mvn` invocation in each | unchanged: the pin is temporary |
| After 3.11.1 is released | `3.11.1` | `-U` removed | "the released `3.11.1`", since `#158` |

The pull request is not ready to merge while the `SNAPSHOT` pin is in. `docs/devops/README.md`,
"Parent POM", describes this pattern. Its `#127` paragraph keeps its history, and the pin sentence
names `3.11.1` and `#158`.

### 3.7 The issue edits

On approval of this spec, `#158`'s body changes in two places. Both were true when it was written:

- Context, last bullet. "The `<resources>` blocks are DSH's own, in each module's POM. The spec
  confirms that nothing has to move to parent-poms." becomes: "The `<resources>` blocks are DSH's
  own, in each module's POM. The delimiter rule is not: it belongs in parent-poms `products`, and it
  ships as parent-poms 3.11.1 (see the spec)."
- Out of Scope, first bullet, about `mongo.properties`, is deleted: `#167` removed the file.

The ACs stay as they are. AC002's choice is made in §3.1.

The parent-poms issue, in its repository's plain form (no INVEST framing), with label `enhancement`
and milestone `3.11.1-SNAPSHOT`, which is created first:

> **Title:** Filter classpath resources with `@…@` only, as spring-boot-starter-parent does
>
> `products` is the Spring Boot parent of every product, but `maven-resources-plugin` filters
> classpath resources with both default delimiters. `${…}` is Spring's placeholder syntax, so a
> Spring placeholder in a filtered `application.properties` is replaced at build time by any Maven
> property of the same name, and stops following its run-time value. DSH measured this (dsh `#48`,
> finding M1, probes P18 to P20) and `dsh#158` needs it fixed here.
>
> **Change.** In `products/pom.xml` `pluginManagement`, configure the `default-resources` and
> `default-testResources` executions with `useDefaultDelimiters=false` and the single delimiter
> `@`. Every `copy-resources` execution keeps the default delimiters, so site Markdown filtered with
> `${…}` is unaffected.
>
> **Release.** Hotfix 3.11.1, the only issue on its milestone. `master` holds nothing beyond 3.11.0,
> so it is re-versioned to `3.11.1-SNAPSHOT`, as for 3.9.2.
>
> **Done when.** `dsh#158`'s `ClasspathResourceFilteringTest` passes against `3.11.1-SNAPSHOT` and
> fails against 3.10.0, and 3.11.1 is released.

### 3.8 Out of scope

- Writing any `${dsh.name}` property, or any other part of the proposed convention (as in the issue).
- Moving the stamp of version and commit to `spring-boot:build-info` (R8). It is not wired today,
  and nothing asks for it.
- The filtering of test resources. No module filters them, and the rule reaches them through
  `default-testResources` anyway.
- `#140`, which moves `dsh-rest-api`'s test `application.properties` into a profile file. The check
  does not depend on it either way.
- The `dsh-solr` modules beyond the same cleanup as everyone else. Wave 2 task 3 removes them.
- Gate 3 as a separate step. AC004 asks for `-DintegrationTests` on the full build, which covers it.

## 4. Verification design

| AC | Check | Where |
|---|---|---|
| AC001 | `ClasspathResourceFilteringTest` fails on 3.10.0 and passes on 3.11.1-SNAPSHOT and 3.11.1. Its probe is `${project.version}`, which every build defines | Tasks 2, 4 and 7 |
| AC002 | §3.1 decisions 2 to 4. `@project.version@` is replaced, and the second test pins it | Tasks 2 and 4 |
| AC003 | `target/generated-site` of every module, and the generated `README.md`, are identical before and after, apart from timestamps | Tasks 1 and 5 |
| AC004 | `mvn -B clean install` and `mvn -B clean install -DintegrationTests` exit 0, with Docker running | Tasks 6 and 7 |
| AC005 | §3.5: `java-conventions.md` and the probe comment | Tasks 2 and 6 |

## 5. Files to change

**parent-poms** (on its own task branch, off `master`):

| File | Change |
|---|---|
| `products/pom.xml` | the `maven-resources-plugin` entry of §3.2 |
| the eight `pom.xml` at `3.12.0-SNAPSHOT` | `3.12.0-SNAPSHOT` → `3.11.1-SNAPSHOT`, by hand (§7.2) |
| `specs/<n>-<slug>.md` | its spec |

**DSH:**

| File | Change |
|---|---|
| `pom.xml` | `src/site` blocks out of `<resources>` (§3.3); parent pin (§3.6) |
| `dsh-coverage-report`, `dsh-data`, `dsh-doc-analyser`, its three children, `dsh-doc-indexer-worker`, `dsh-solr` and its two children: `pom.xml` | `<resources>` deleted (§3.3) |
| `dsh-rest-api/pom.xml` | `<resources>` reduced to the filtered `src/main/resources` block (§3.3) |
| `dsh-test-dataset/pom.xml` | `<resources>` reduced to its two directories, unfiltered (§3.3) |
| `dsh-rest-api/src/main/resources/application.properties` | the probe comment (§3.4) |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/ClasspathResourceFilteringTest.java` | new (§3.4) |
| `.github/copilot/rules/java-conventions.md` | one bullet (§3.5) |
| `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml` | `-U` during the pin, removed at the re-pin (§3.6) |
| `CLAUDE.md`, `docs/devops/README.md` | the released pin is `3.11.1` (§3.6) |
| `specs/stories/158-stop-maven-filtering-spring-placeholders.md` | this spec, §7 filled in |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order. Task 3 is in the parent-poms repository.

**Goal:** a `${…}` in any file on a DSH module's classpath reaches the jar as written, because
`products` filters classpath resources with `@…@` only, and a DSH unit test fails if that ever
stops being true.

**Architecture:** the delimiter rule is set on the two lifecycle executions of
`maven-resources-plugin` in parent-poms `products`, released as 3.11.1. DSH removes its unused
filters, takes Markdown out of the classpath executions, and keeps one filtered file in
`dsh-rest-api` as a probe that a plain unit test reads.

**Tech stack:** Maven 3.9.16, `maven-resources-plugin` 3.3.1, Java 17, JUnit 4, GitHub Actions.

**Spec:** this file.

### Global constraints

- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"). The Maven local repository is `D:\.m2\repository`.
- An effective POM is never printed whole. Extract the `maven-resources-plugin` section and nothing
  else (§2.2).
- Commands run in Git Bash. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- POMs indent with tabs, except the root and `dsh-coverage-report`, which use four spaces. Java test
  files use four spaces. Keep each file's style.
- Creating the parent-poms milestone and issue, opening its pull request, and dispatching its
  `deploy.yml` each need the owner's go. Claude neither merges nor closes an issue, in either
  repository.
- Commit messages are `test(#158): …`, `build(#158): …` and `docs(#158): …` in DSH, and
  `build(#<p>): …` and `docs(#<p>): …` in parent-poms. They end with the session's attribution
  lines.

### Task 0: The issues

- [x] **Step 1:** Edit `#158`'s body as in §3.7. Read it back with `gh issue view 158`.
- [x] **Step 2:** Create parent-poms milestone `3.11.1-SNAPSHOT`, then the issue of §3.7 on it,
  labelled `enhancement`. Record its number as `<p>` in §7.

### Task 1: Baseline (AC003)

- [x] **Step 1:** On the task branch, before any POM change:

  ```bash
  mkdir -p .logs
  mvn -B clean install -DskipTests > .logs/mvn-baseline-install.log 2>&1 &
  MVN_PID=$!; echo "Monitor with:  tail -f .logs/mvn-baseline-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`.

- [x] **Step 2:** Copy every module's `target/generated-site` into
  `<scratchpad>/158-baseline/<module path>/`, using `find . -path '*/target/generated-site' -prune`.
- [x] **Step 3:** Generate the README and keep a copy, then restore it:

  ```bash
  mvn -B -Ddeployment -N process-resources > .logs/mvn-baseline-readme.log 2>&1; echo "maven exit=$?"
  cp README.md <scratchpad>/158-baseline/README.md && git checkout -- README.md
  ```

### Task 2: The probe and the test, red (AC001, AC002, AC005)

**Files:**

- Modify: `dsh-rest-api/src/main/resources/application.properties` (top of file)
- Create: `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/ClasspathResourceFilteringTest.java`

- [x] **Step 1:** Add the three comment lines of §3.4 at the top of `application.properties`, then
  an empty line, before the existing first comment.
- [x] **Step 2:** Create the test exactly as in §3.4.
- [x] **Step 3:** Run it on 3.10.0, where the POMs are unchanged:

  ```bash
  mvn -B clean test -pl dsh-rest-api -Dtest=ClasspathResourceFilteringTest \
    > .logs/mvn-test-filtering-red.log 2>&1; echo "maven exit=$?"
  grep -E 'Tests run|FAIL|replaced' .logs/mvn-test-filtering-red.log
  ```

  Expected: `maven exit=1`, `Tests run: 3, Failures: 1`, failing
  `applicationProperties_springPlaceholder_reachesTheJarAsWritten` with "Maven replaced
  ${project.version}". Record the lines in §7.

- [x] **Step 4:** Commit: `test(#158): pin Spring placeholders in dsh-rest-api's classpath output`.

### Task 3: parent-poms, the rule and the `SNAPSHOT` (in `../parent-poms`)

- [x] **Step 1:** `git checkout master && git pull && git checkout -b issue-<p>-filter-classpath-resources-with-at-delimiter`.
- [x] **Step 2:** Write `specs/<p>-filter-classpath-resources-with-at-delimiter.md`, short, in the
  form of `specs/98-retry-transient-registry-errors.md`, from §3.2 and §3.7. Commit
  `docs(#<p>): spec for filtering classpath resources with @...@ only`.
- [x] **Step 3:** Re-version the eight POMs at `3.12.0-SNAPSHOT` by hand, not with
  `./set-version.sh 3.11.1-SNAPSHOT` (§7.2). Check with
  `git grep -n '3.12.0-SNAPSHOT' -- '*pom.xml'`, which should find nothing. Commit
  `build(#<p>): re-version master to 3.11.1-SNAPSHOT for the hotfix line`, with a body that gives
  the 3.9.2 precedent of §2.2.
- [x] **Step 4:** Add the entry of §3.2 to `products/pom.xml`. Then
  `mvn -B clean install > ../dsh/.logs/mvn-parent-poms-install.log 2>&1; echo "maven exit=$?"`.
  Expected: `maven exit=0`, and `D:\.m2\repository\com\mriss\mriss-parent\products\3.11.1-SNAPSHOT`
  exists. Commit `build(#<p>): filter classpath resources with @...@ only`.
- [ ] **Step 5:** Push, and open the pull request into `master` once the owner approves its text.
  After the owner merges it, dispatch `deploy.yml` with `release_type: snapshots` once the owner
  says go, so that DSH's CI can resolve `3.11.1-SNAPSHOT`.

### Task 4: DSH on the `SNAPSHOT`, green (AC001, AC002)

**Files:** every POM of §5, `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml`

- [x] **Step 1:** Root `pom.xml`: parent `3.10.0` → `3.11.1-SNAPSHOT`. Add `-U` to
  `mvn -B install --file pom.xml` (`ci.yml:217`) and to `mvn -B install -DskipTests`
  (`api-testing.yml:208`).
- [x] **Step 2:** Rerun Task 2 Step 3's command into `.logs/mvn-test-filtering-snapshot.log`.
  Expected: `maven exit=0`, `Tests run: 3, Failures: 0`. The POMs' `<resources>` are still unchanged,
  so this proves the inherited rule on its own.
- [x] **Step 3:** Apply §3.3 to every POM. Then
  `git grep -n '<filtering>true</filtering>' -- '*pom.xml'`. Expected: `dsh-rest-api/pom.xml`'s
  `src/main/resources` block, and the Markdown and `site.xml` of the `copy-resources` executions:
  two in the root's `copy-site-resources-for-pdf`, and the `copy-resources` executions of
  `dsh-doc-analyser/pom.xml` and `dsh-solr/pom.xml`. Nothing else.
- [x] **Step 4:** Rerun the test into `.logs/mvn-test-filtering-green.log`. Expected: `maven exit=0`,
  `Tests run: 3, Failures: 0`.
- [x] **Step 5:** Extract the `maven-resources-plugin` section of `dsh-rest-api`'s effective POM
  (constraint above), and check that `default-resources` and `default-testResources` carry the
  `@` delimiter and `copy-site-resources-for-pdf` does not. Record that section in §7, then delete
  the effective POM file.
- [x] **Step 6:** Commit: `build(#158): filter classpath resources with @...@ only, through parent-poms 3.11.1-SNAPSHOT`.

### Task 5: Markdown and README unchanged (AC003)

- [x] **Step 1:** Repeat Task 1 Steps 1 to 3 into `<scratchpad>/158-after/`.
- [x] **Step 2:** `diff -r <scratchpad>/158-baseline <scratchpad>/158-after`. Expected: no
  difference except the timestamp lines that `create-time-stamp` writes. Record the diff in §7. Any
  other difference stops the task.

### Task 6: Documentation and gate 1 (AC004, AC005)

- [x] **Step 1:** Add the bullet of §3.5 to `.github/copilot/rules/java-conventions.md`.
- [x] **Step 2:** Run markdownlint (`CLAUDE.md`, Commands). Expected: no output.
- [x] **Step 3:** Gate 1 and 2:

  ```bash
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!; echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`.

- [x] **Step 4:** Start Docker Desktop if `docker info` fails, then the same with
  `-DintegrationTests` into `.logs/mvn-clean-install-it.log`. Expected: `maven exit=0`.
- [x] **Step 5:** Commit: `docs(#158): write down the @...@ rule for classpath resources`.

### Task 7: The release and the re-pin, during step 6 or 7

The DSH pull request is opened with the `SNAPSHOT` pin, and it says so. It is not ready to merge until
this task is done.

- [ ] **Step 1:** Once the owner has closed parent-poms `#<p>` and says go, dispatch parent-poms'
  `deploy.yml` with `release_type: releases`. Check that tag `mriss-parent-3.11.1` exists, that
  `master`'s POMs are at `3.12.0-SNAPSHOT`, and that milestone `3.11.1-SNAPSHOT` has no open issue.
- [ ] **Step 2:** In DSH: parent `3.11.1-SNAPSHOT` → `3.11.1`. Remove the two `-U`. In `CLAUDE.md`
  ("The parent is the released `3.10.0`") and `docs/devops/README.md` ("The current pin is the released
  `com.mriss.mriss-parent:products:3.10.0`, since `#150`"), name `3.11.1` and `#158`.
- [ ] **Step 3:** Rerun Task 6 Step 3 into `.logs/mvn-clean-install-repin.log`. Expected:
  `maven exit=0`, `ClasspathResourceFilteringTest` among the tests run with no failure.
- [ ] **Step 4:** Commit `build(#158): re-pin the parent to the released parent-poms 3.11.1`, and
  push.

## 7. Verification results

Run on 2026-10-20, Maven 3.9.16, Docker running. Logs in `.logs/`, named as in §6.

### 7.1 The issues (Task 0)

Both were done before the build started. `#158`'s body already carried the two §3.7 edits. The
parent-poms issue is [`#110`](https://github.com/MRISS-Projects/parent-poms/issues/110), so
`<p>` is `110`, the only issue on milestone `3.11.1-SNAPSHOT`.

### 7.2 Deviations from the plan

- **The parent-poms re-version was made by hand, not with `set-version.sh`.** Run on the branch,
  the script changed 15 files, 49 lines in and 71 out: `release:update-versions` also moved the
  seven independently released modules' parents and reformatted every POM. That is the reason the
  3.9.2 precedent (`d5a80948`) gives for not using it, and the script has not changed since. The
  tree was restored and the eight POMs at `3.12.0-SNAPSHOT` were edited, one line each, the same
  eight files as `d5a80948`. `git grep '3.12.0-SNAPSHOT' -- '*pom.xml'` finds nothing. The text of
  §3.2, §5 and Task 3 Step 3 now says so, and parent-poms' spec records it too.
- **Six POMs lost an empty `<build>`.** Once `<resources>` was deleted, `dsh-doc-processor-worker`,
  `dsh-keyword-extractor`, `dsh-top-sentences-extractor`, `dsh-doc-indexer-worker` and the two
  `dsh-solr` children held `<build></build>` and nothing else, so the element went too.
- **The root's `FR009` comment went with its blocks.** It sat above `<plugins>` once the
  `<resources>` it described was gone. The `FR009` comments inside `copy-resources` executions
  (`dsh-doc-analyser/pom.xml`) stay.
- **`dsh-test-dataset` has no `src/main` directory.** Its block keeps the `src/main/resources` entry
  as §3.3 says, unfiltered; it matches nothing.

### 7.3 Red, then green (AC001, AC002)

| Run | Parent | POM `<resources>` | Result |
|---|---|---|---|
| `mvn-test-filtering-red.log` | `3.10.0` | unchanged | `maven exit=1`, `Tests run: 3, Failures: 1` |
| `mvn-test-filtering-snapshot.log` | `3.11.1-SNAPSHOT` | unchanged | `maven exit=0`, `Tests run: 3, Failures: 0` |
| `mvn-test-filtering-green.log` | `3.11.1-SNAPSHOT` | §3.3 applied | `maven exit=0`, `Tests run: 3, Failures: 0` |

The red run failed the one test it was meant to, for the reason it was meant to:

```text
[ERROR] com.mriss.dsh.restapi.ClasspathResourceFilteringTest.applicationProperties_springPlaceholder_reachesTheJarAsWritten -- Time elapsed: 0.008 s <<< FAILURE!
java.lang.AssertionError: Maven replaced ${project.version} in application.properties: classpath resources must be filtered with @...@ only (#158)
```

The middle run proves the inherited rule on its own, before any DSH POM changed.

After §3.3, `git grep -n '<filtering>true</filtering>' -- '*pom.xml'` finds exactly five lines:
`dsh-rest-api/pom.xml:114` (its `src/main/resources`), `pom.xml:135` and `pom.xml:159` (the
Markdown and `site.xml` of `copy-site-resources-for-pdf`), `dsh-doc-analyser/pom.xml:51` and
`dsh-solr/pom.xml:84` (their `copy-resources`).

### 7.4 The effective POM of `dsh-rest-api` (AC002)

Extracted to a scratch file, narrowed to `maven-resources-plugin`, and deleted. The `<build><plugins>`
entry, shortened where marked:

```xml
<artifactId>maven-resources-plugin</artifactId>
<version>3.3.1</version>
<executions>
  <execution>
    <id>default-testResources</id>
    <phase>process-test-resources</phase>
    <goals><goal>testResources</goal></goals>
    <configuration>
      <useDefaultDelimiters>false</useDefaultDelimiters>
      <delimiters><delimiter>@</delimiter></delimiters>
    </configuration>
  </execution>
  <execution>
    <id>default-resources</id>
    <phase>process-resources</phase>
    <goals><goal>resources</goal></goals>
    <configuration>
      <useDefaultDelimiters>false</useDefaultDelimiters>
      <delimiters><delimiter>@</delimiter></delimiters>
    </configuration>
  </execution>
  <execution>
    <id>copy-site-resources-for-pdf</id>
    <phase>process-resources</phase>
    <goals><goal>copy-resources</goal></goals>
    <configuration>
      <encoding>UTF-8</encoding>
      <outputDirectory>.</outputDirectory>
      <resources><!-- src/site **/*.md filtered, src/site the rest unfiltered,
                      src/site-desc site.xml filtered; all to target/generated-site --></resources>
    </configuration>
  </execution>
</executions>
```

`copy-site-resources-for-pdf` carries no delimiter, so it keeps both defaults.

### 7.5 Markdown and README unchanged (AC003)

`diff -r` of `158-baseline` (3.10.0, POMs unchanged) against `158-after` (3.11.1-SNAPSHOT, §3.3
applied), covering the 13 `target/generated-site` trees and the generated `README.md`:

```text
diff -r .../158-baseline/README.md .../158-after/README.md
7c7
< 0.4.0-SNAPSHOT - dev - 20261020-104937
---
> 0.4.0-SNAPSHOT - dev - 20261020-105504
```

The timestamp line is the only difference. Every `generated-site` tree is byte-identical.

### 7.6 The gates (AC004, AC005)

- markdownlint, as in `CLAUDE.md`: no output, exit 0, with the §3.5 bullet in place.
- `mvn -B clean install` (`mvn-clean-install.log`): `maven exit=0`, all 13 modules `SUCCESS`, the
  coverage checks met, and `ClasspathResourceFilteringTest` run with `Tests run: 3, Failures: 0`.
- `mvn -B clean install -DintegrationTests` (`mvn-clean-install-it.log`): `maven exit=0`, in
  2 min 33 s. Every `*IT` passed: `DocumentResourceHttpIT` 5, `DocumentResourceIT` 6,
  `DshRestApplicationIT` 2, `PersistenceSelectionIT` 3, and the four workers' application ITs.

### 7.7 Still to do

- parent-poms Task 3 Step 5: the branch is pushed and its pull request is
  [`#111`](https://github.com/MRISS-Projects/parent-poms/pull/111), with commits `1cb701e4` (spec),
  `efe85b4e` (re-version) and `c4a9931f` (the rule). The merge and the snapshot deploy remain.
  `3.11.1-SNAPSHOT` is so far installed only in the local repository, so DSH's CI cannot resolve
  it until the deploy.
- Task 7, the release and the re-pin.

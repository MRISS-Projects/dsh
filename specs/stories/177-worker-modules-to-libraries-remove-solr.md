---
issue: 177
slug: worker-modules-to-libraries-remove-solr
parent_branch: DEVELOP
wave: 2
milestone: 0.4.0-SNAPSHOT
---

# Story 177 — Reduce the worker modules to libraries and remove dsh-solr

## 1. Story

**As a** DSH developer
**I want** the reactor to hold only the modules ADR-003 section 2 keeps, with the stub applications
reduced to libraries and `dsh-solr` gone
**So that** the build, the coverage report and the documents stop carrying four empty deployables
and a Solr integration nothing calls, and `#119` and `#120` migrate and upgrade none of it

## 2. Context

- Wave: 2, task 3 (`specs/product/PRD.md` §4, Wave 2). It runs at step 4 of Wave 1's order of
  work, after `#158` and `#140`, both closed, and ahead of `#119` and `#120`. DSH milestone
  `0.4.0-SNAPSHOT`.
- Issue: [#177](https://github.com/MRISS-Projects/dsh/issues/177), label `task`.
- Decision: `specs/architecture/ADR-003-document-pipeline.md`, section 2 "The deployables" and its
  module table; its consequences row "Wave 2 task 3".
- Parent branch: `DEVELOP`, at `655ddc8f0` when this spec was written. The task branch is
  `issue-177-worker-modules-to-libraries-remove-solr`.

### 2.1 What exists today

| Fact | Evidence |
|---|---|
| The reactor has 13 modules: the root, `dsh-test-dataset`, `dsh-data`, `dsh-rest-api`, `dsh-solr` and its two plugins, `dsh-doc-indexer-worker`, `dsh-doc-analyser` and its three modules, `dsh-coverage-report` | `<modules>` of `pom.xml`, `dsh-solr/pom.xml`, `dsh-doc-analyser/pom.xml` |
| Each of the four worker modules holds one `Application` class, its `*ApplicationTest` (two methods) and its `*ApplicationIT`, and nothing else. `src/main/resources` holds `logback.xml` and an empty `property.properties`; `src/test/resources` holds `logback-test.xml` and an empty `property.properties` | `git ls-files dsh-doc-indexer-worker dsh-doc-analyser` |
| `dsh-doc-processor-worker` depends on the two extractors, and on `spring-boot-starter-amqp` and `spring-data-solr` | its `pom.xml` |
| `dsh-doc-indexer-worker` depends on `spring-boot-starter-amqp`, `spring-data-solr` and `dsh-data`, and uses none of them | its `pom.xml`; its one class imports only SLF4J and Spring Boot |
| The extractors depend on `spring-boot-starter` and on `spring-boot-starter-test`, the latter with no `<scope>` | their `pom.xml` |
| `dsh-solr` is 85 tracked files: two plugins that depend on `org.apache.solr:solr-core`, a Solr `config/` tree, and its site. Nothing outside it calls either plugin | `git ls-files dsh-solr`; ADR-003 "What exists today" |
| `spring-data-solr` is declared in `dsh-data`, `dsh-doc-indexer-worker` and `dsh-doc-processor-worker`. Its version, `4.3.15`, is managed by parent-poms' `products/pom.xml`, whose comment says "managed here for DSH modules" | the three POMs; `products-3.11.1.pom` lines 67-85 |
| The only main source outside `dsh-solr` that names Solr is `dsh-data`'s empty `SOLRIndexedDocument`, which implements the empty `IndexedDocumentDao`. No test refers to either | `git grep -n -i solr -- '*.java'` |
| `dsh-rest-api/pom.xml` holds a commented-out `spring-boot-starter-data-solr` dependency | `dsh-rest-api/pom.xml:18-21` |
| The root `pom.xml`'s `dependencyManagement` lists every module this story removes or renames, and `dsh-coverage-report` depends on each of them | the two POMs |
| The inherited `enforce-coverage-data-exists` guard passes a module whose `${project.build.sourceDirectory}` does not exist. So a module with no `src/main/java` needs no `coverage.data.check.skip` | `mriss-parent-3.11.1.pom`, executions `coverage-guard-source-dir` and `enforce-coverage-data-exists` |
| No `javadoc:jar` or `source:jar` execution is inherited, so an empty module attaches nothing that needs sources | `mriss-parent-3.11.1.pom`, `products-3.11.1.pom` |
| The root `README.md` is regenerated from `src/site/markdown/README.md` by `github-actions[bot]` ("Auto-generated README.md"), and is never edited by hand | `git log -- README.md`; `specs/stories/143-readme-describes-dsh-today.md` §2 |
| No workflow or script names any of the modules | `git grep` over `.github/workflows`, `.github/scripts`, `*.sh`, `*.yml` |

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-20

1. **parent-poms is not touched.** Its managed `spring-data-solr` and the
   `spring-data-solr.version` property become dead with this story. They are left for `#120`,
   which edits `products/pom.xml` for the Spring Boot upgrade anyway. A comment on `#120` records
   it (§3.9). A light round trip inside this story, and a separate issue, were rejected.
2. **The three libraries are stripped to their POM and their site.** Every resource, main and
   test, and every dependency goes. A library that ships `logback.xml` would contend with
   `dsh-rest-api`'s logging once the API depends on it, and both `property.properties` files are
   empty. Wave 3 adds what each stage needs, `dsh-data` included. Keeping `spring-boot-starter` and
   `dsh-data` for the stages to come was rejected.
3. **A build rule bans Solr.** An enforcer execution in the root `pom.xml` fails any module that
   depends on `org.apache.solr:*` or `spring-data-solr`, directly or transitively. It is this
   story's red step, and it keeps ADR-003's decision enforced afterwards. Verifying by grep alone
   was rejected: nothing would fail when Solr came back.

### 3.2 The modules

Once this story is done the reactor has 9 modules, matching ADR-003 section 2 for Wave 2:

| Module | Change |
|---|---|
| `dsh-solr`, `solr-terms-vector-order`, `solr-advanced-numbers-filter` | Removed: `git rm -r dsh-solr` |
| `dsh-doc-processor-worker` | Removed: `git rm -r dsh-doc-analyser/dsh-doc-processor-worker` |
| `dsh-doc-indexer-worker` | `git mv` to `dsh-doc-indexer`. `artifactId` `dsh-doc-indexer`, `<name>` `DSH - Document Indexer`, `<description>` `DSH Document Indexer`. The groupId stays `com.mriss.products.dsh`. Then stripped, as the extractors |
| `dsh-keyword-extractor`, `dsh-top-sentences-extractor` | Stripped: the `Application` class, `*ApplicationTest`, `*ApplicationIT`, `src/main/resources`, `src/test/resources`, and the `<dependencies>` block with the `FR005` comment above it. Left: `pom.xml` (packaging `jar`), `maven-site.sh`, `maven-site-deploy.sh`, `src/site`, `src/site-desc` |
| `dsh-doc-analyser` | Its `<modules>` loses `dsh-doc-processor-worker` |
| `dsh-rest-api`, `dsh-data`, `dsh-test-dataset`, `dsh-coverage-report` | Kept; POM edits in §3.3 |

The stripped modules build as empty jars. `maven-jar-plugin` warns "JAR will be empty - no content
was marked for inclusion!", surefire reports no tests, and the coverage guard passes because there
is no `src/main/java`. **No POM sets `coverage.data.check.skip`.** A skip set now would silently
skip the gate once Wave 3 puts code in the module.

### 3.3 The POMs

| POM | Change |
|---|---|
| `pom.xml` | `<modules>`: delete `dsh-solr`; `dsh-doc-indexer-worker` → `dsh-doc-indexer`. `dependencyManagement`: `dsh-doc-indexer-worker` → `dsh-doc-indexer`; delete `dsh-doc-processor-worker`, `solr-terms-vector-order`, `solr-advanced-numbers-filter`. Add `enforce-no-solr` (§3.5) |
| `dsh-doc-analyser/pom.xml` | delete `<module>dsh-doc-processor-worker</module>` |
| `dsh-data/pom.xml` | delete the `spring-data-solr` dependency and its comment line |
| `dsh-rest-api/pom.xml` | delete the commented-out `spring-boot-starter-data-solr` dependency, lines 18-21 |
| `dsh-coverage-report/pom.xml` | `dsh-doc-indexer-worker` → `dsh-doc-indexer`; delete `dsh-doc-processor-worker` and the two plugins. The empty libraries stay listed, so that Wave 3's code is aggregated the day it exists |
| `dsh-doc-indexer/pom.xml`, `dsh-keyword-extractor/pom.xml`, `dsh-top-sentences-extractor/pom.xml` | the `FR005` comment and the `<dependencies>` block deleted; the indexer renamed (§3.2) |

### 3.4 `dsh-data`

`git rm dsh-data/src/main/java/com/mriss/dsh/data/index/dao/solr/SOLRIndexedDocument.java`. Its
package directory goes with it. `IndexedDocumentDao` stays: it names no technology, and whether the
indexing stage needs it is Wave 3's call.

### 3.5 The ban: `enforce-no-solr`

A second execution of `maven-enforcer-plugin` (3.5.0, from parent-poms) in the root `pom.xml`,
after `enforce-lowercase-artifact-id`. It binds to `validate`, the goal's default, so it runs in
every module before compilation:

```xml
<execution>
    <id>enforce-no-solr</id>
    <goals>
        <goal>enforce</goal>
    </goals>
    <configuration>
        <rules>
            <bannedDependencies>
                <excludes>
                    <exclude>org.apache.solr:*</exclude>
                    <exclude>org.springframework.data:spring-data-solr</exclude>
                </excludes>
                <message>Solr was removed by ADR-003 section 2 (#177): no module may depend on it, directly or transitively. Lucene's own artifacts (org.apache.lucene) remain allowed.</message>
            </bannedDependencies>
        </rules>
        <fail>true</fail>
    </configuration>
</execution>
```

`bannedDependencies` searches transitive dependencies by default, so `solr-solrj` arriving through
`spring-data-solr`, or `solr-core` through the plugins, fails as well. Lucene is not banned: ADR-003
section 2 says the number filter can come back from git history if Wave 3 task 1 chooses Lucene's
analysis library, and the filter imports only `org.apache.lucene`.

`-Denforcer.skip=true` disables this rule along with `enforce-lowercase-artifact-id`, as `CLAUDE.md`
describes for the latter. That is acceptable: the flag is already documented as the wrong tool.

### 3.6 The documents

| File | Change |
|---|---|
| `CLAUDE.md` | Opening paragraph: "the Solr code is still here, but nothing runs it" becomes "Solr was removed by `#177`". Module table: `dsh-doc-analyser` → "Parent of the two extractor libraries, keyword extraction and top-sentence extraction"; the indexer row → `` `dsh-doc-indexer` `` "A library: the indexing stage, empty until Wave 3"; the `dsh-solr` row deleted. "13-module reactor" → "9-module reactor". Gate 3: "(RabbitMQ, Solr, or any other service outside the JVM)" → "(RabbitMQ, or any other service outside the JVM)" |
| `.github/copilot/rules/testing-patterns.md:27`, `.claude/skills/dsh-ship-story/SKILL.md:14` | The same edit to the gate-3 example list |
| `specs/architecture/system-design.md` | The components diagram: the right box becomes `dsh-doc-indexer`, "Indexing stage library", "(called by the pipeline)"; the left box's "(sync analysis)" becomes "(extractor libraries)"; the `dsh-solr` box under `dsh-data` is deleted, leaving the document store alone. The migration note's last sentence: "the RabbitMQ and Solr code is still in the repository" → "`#177` removed the Solr code (section 2), and the RabbitMQ code is still in the repository". `dsh-rest-api`: "Delegates business logic to `dsh-doc-analyser` and `dsh-doc-indexer`". The `dsh-doc-indexer-worker` section becomes `### dsh-doc-indexer`: "A library with no `Application` class (ADR-003 section 2), empty until Wave 3", "Will hold the indexing stage: text extraction and the document's term frequencies, to the file store". The `### dsh-solr` section is deleted. Technology table: "In memory (default); Apache Solr (unused, removal decided)" → "In memory (default)" |
| `src/site/markdown/README.md` | Module list (§3.6.1). The root `README.md` follows on the next snapshot deploy |
| `dsh-doc-indexer/src/site/markdown/index.md` | Rewritten (§3.6.2) |
| `dsh-doc-analyser/src/site/markdown/index.md` | "This is a container module for the keyword and relevant sentences extractor modules. One extra module dequeues documents to be analysed and calls the extractors." → "This is a parent module for the keyword and top-sentences extractor libraries. The document pipeline in `dsh-rest-api` calls them; neither runs on its own." |
| `.github/copilot-instructions.md` | Architecture summary: `dsh-doc-indexer-worker` – "Background indexing worker" → `dsh-doc-indexer` – "Indexing stage library"; the `dsh-solr` line deleted. `### Indexer Worker (dsh-doc-indexer-worker)` → `### Indexer (dsh-doc-indexer)` |
| `.github/roles.md` | `` `dsh-doc-indexer-worker` – Background indexing worker `` → `` `dsh-doc-indexer` – Indexing stage library `` |
| `.github/copilot/prompts/feature-implementation.md` | `dsh-doc-indexer-worker – for indexing workflow changes` → `dsh-doc-indexer – for indexing workflow changes` |
| `.github/ISSUE_TEMPLATE/story.md`, `bug-specification.md`, `feature-specification.md` | "Affected Modules": `dsh-doc-indexer-worker` → `dsh-doc-indexer`; the `dsh-solr` line deleted |
| `docs/troubleshooting/README.md` | The `solr-connection-issues.md` suggestion and the "**Solr**: check the Solr admin UI" line deleted. "check `dsh-rest-api` and `dsh-doc-indexer-worker` logs" → "check the `dsh-rest-api` logs; the pipeline's stages run inside it" |
| `specs/architecture/component-diagrams/README.md` | The example: `A -->\|queues work to\| C[dsh-doc-indexer-worker]` → `A -->\|depends on\| C[dsh-doc-indexer]`; the `C --> E[dsh-solr]` edge deleted |
| `specs/architecture/sequence-diagrams/README.md` | The example: `participant Worker as dsh-doc-indexer-worker` → `participant Worker as dsh-doc-indexer` |

**This widens the issue's AC006 in two ways**, both found while writing this spec: the two module
site pages, and the gate-3 example list in `CLAUDE.md`, `testing-patterns.md` and `dsh-ship-story`.
Each is a live document that would otherwise describe a module or a system DSH no longer has.

#### 3.6.1 `src/site/markdown/README.md`, the module list

The `dsh-solr` entry, its two sub-entries, the `dsh-doc-indexer-worker` entry and the
`dsh-doc-analyser` entry with its three sub-entries are replaced by:

```markdown
* **dsh-doc-indexer**: a library, empty so far, that will hold the indexing stage: extracting a
  document's text and counting its terms. The document pipeline in `dsh-rest-api` will call it.
  It was `dsh-doc-indexer-worker`, a Spring Boot application holding only its main class, until
  `#177` made it a library.
* **dsh-doc-analyser**: a parent module grouping the two extractor libraries below.
  * **dsh-keyword-extractor**: not implemented yet. It will score each term of a document with
    combinations of TF/IDF and return the best ranked terms as keywords.
  * **dsh-top-sentences-extractor**: not implemented yet; planned to rank sentences with
    [automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization) techniques
    and return the most relevant ones.
```

followed, after the `dsh-coverage-report` entry, by one paragraph:

```markdown
`#177` removed two modules, following
[ADR-003](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-003-document-pipeline.md)
section 2: **dsh-solr**, with its two Solr plugins, which nothing called, and
**dsh-doc-processor-worker**, an empty application whose only job, running the two extractors one
after the other, is now the pipeline's.
```

#### 3.6.2 `dsh-doc-indexer/src/site/markdown/index.md`

```markdown
# Welcome to DSH Document Indexer

## Document Smart Highlights Document Indexer

This module is a library: it has no `Application` class, and it does not run on its own. It will
hold the indexing stage of the document pipeline, which runs inside `dsh-rest-api`
([ADR-003](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-003-document-pipeline.md),
sections 1 and 2). The stage will:

1. Read the original file of a document.
2. Extract its text, and count the document's own term frequencies.
3. Store that output in the file store, so that a repeat replaces it.
4. Update the corpus term statistics that the keyword and sentence stages read.

It is empty until Wave 3. Until `#177` it was `dsh-doc-indexer-worker`, a Spring Boot application
that held only its main class.
```

### 3.7 Out of scope

- **Any stage code or flow.** The libraries are left empty. The flow is Wave 2 task 1; the stages
  are Wave 3.
- **The RabbitMQ enqueue path** and `dsh-rest-api`'s AMQP dependency. Wave 2 task 1 deletes them.
  This story removes only the AMQP dependencies of the modules it removes or strips.
- **`IndexedDocumentDao`** (§3.4).
- **parent-poms' managed `spring-data-solr`** (§3.1, decision 1).
- **`specs/features/*`'s Solr text**, `indexing-workflow.md` included: ADR-003 follow-up candidate 8.
- **Historical records**: the ADRs, the PRD, `specs/stories/`, `docs/superpowers/plans/`. The PRD
  is reconciled at step 8.
- **Redrawing the component and sequence diagrams.** §3.6 renames and removes modules in their
  examples only; Wave 1 tasks 5 and 6 draw the real ones.
- **The root `README.md`.** It is regenerated (§2.1).
- **The published site pages of the removed and renamed modules** (§3.9).

### 3.8 Risks

- **An IDE re-creates an empty `src/main/java`** in a stripped module, as Eclipse's m2e does for a
  declared source folder. The coverage guard then fails that module locally: "This module has
  production sources … but produced no coverage data". The fix is to delete the empty directory.
  CI builds from a fresh checkout, and git does not track empty directories, so CI is unaffected.
  The guard stays armed on purpose (§3.2).
- **A local clone keeps the old directories' `target/` trees.** `git` removes tracked files, not
  ignored output, so `dsh-doc-indexer-worker/target/` and the others survive a checkout of this
  branch. They are outside the reactor and harmless; `rm -rf` them.

### 3.9 Follow-ups

1. **A comment on `#120`**, posted once the owner approves it:

   > `#177` removed the last `spring-data-solr` declaration from DSH, and added an enforcer rule,
   > `enforce-no-solr`, that bans `org.apache.solr:*` and `spring-data-solr`. Two entries in
   > parent-poms' `products/pom.xml` are now dead: the managed `org.springframework.data:spring-data-solr`
   > in `dependencyManagement`, and the `spring-data-solr.version` property (`4.3.15`). This story
   > removes both as part of its edit to `products/pom.xml`. Decided while specifying `#177`, on
   > 2026-10-20.

2. **The gh-pages pages** of `dsh-solr`, `dsh-doc-processor-worker` and `dsh-doc-indexer-worker`.
   After the first snapshot deploy that follows the merge, check whether
   `https://mriss-projects.github.io/dsh/snapshots/products/dsh/<module>/` still serves them. Open
   a follow-up issue only if it does.

## 4. Verification design

| AC | Check | Expected | Where |
|---|---|---|---|
| AC001 | `ls -d dsh-solr dsh-doc-analyser/dsh-doc-processor-worker dsh-doc-indexer-worker`; gate 1 and the reactor summary | none exists; `maven exit=0`, 9 modules | Tasks 3-5, 7 |
| AC002 | `git grep -n 'dsh-doc-indexer-worker' -- '*pom.xml'`; `grep -n 'dsh-doc-indexer<' dsh-coverage-report/pom.xml` | no hit; one hit | Tasks 5, 7 |
| AC003 | `git grep -l -e '@SpringBootApplication' -e 'static void main' -- dsh-doc-indexer dsh-doc-analyser`; `git ls-files dsh-doc-indexer dsh-doc-analyser \| grep /src/main/` | no hit; no file | Tasks 5, 7 |
| AC004 | the red run of `enforce-no-solr`; then `git grep -n -i solr -- '*pom.xml'` and `git grep -n -i solr -- '*/src/main/*'` | the red run fails at `dsh-data`; after, only `enforce-no-solr`'s lines; no hit | Tasks 2, 3, 7 |
| AC005 | `git grep -n -i amqp -- dsh-doc-indexer dsh-doc-analyser` | no hit | Tasks 5, 7 |
| AC006 | `git grep -n -e dsh-doc-indexer-worker -e dsh-doc-processor-worker -e dsh-solr --` over the files of §3.6; markdownlint over every changed `.md` | each hit says the module was renamed or removed; lint exit 0 | Tasks 6, 7 |
| AC007 | `mvn -B clean install -DintegrationTests`, with Docker running | `maven exit=0`; no `*ApplicationIT` of the four modules runs | Task 7 |
| Gates 1, 2 | `mvn -B clean install`; the count of unit tests before and after | `maven exit=0`; the only tests gone are the four `*ApplicationTest` classes, 8 methods | Tasks 1, 7 |

Gate 3 applies: the story changes `dsh-data`, which every other module depends on, and removes code
written for Solr. AC007 runs `-DintegrationTests` over the whole reactor, which covers gate 3's
`-pl dsh-data -amd`.

## 5. Files to change

| File | Change |
|---|---|
| `pom.xml` | §3.3, §3.5 |
| `dsh-solr/**` | deleted |
| `dsh-doc-analyser/dsh-doc-processor-worker/**` | deleted |
| `dsh-doc-indexer-worker/**` → `dsh-doc-indexer/**` | renamed; `pom.xml` edited; class, tests and resources deleted; `src/site/markdown/index.md` rewritten |
| `dsh-doc-analyser/pom.xml` | §3.3 |
| `dsh-doc-analyser/src/site/markdown/index.md` | §3.6 |
| `dsh-doc-analyser/dsh-keyword-extractor/**`, `dsh-doc-analyser/dsh-top-sentences-extractor/**` | `pom.xml` edited; class, tests and resources deleted |
| `dsh-data/pom.xml`, `dsh-data/src/main/java/com/mriss/dsh/data/index/dao/solr/SOLRIndexedDocument.java` | §3.3, §3.4 |
| `dsh-rest-api/pom.xml` | §3.3 |
| `dsh-coverage-report/pom.xml` | §3.3 |
| `CLAUDE.md`, `.github/copilot/rules/testing-patterns.md`, `.claude/skills/dsh-ship-story/SKILL.md`, `specs/architecture/system-design.md`, `src/site/markdown/README.md`, `.github/copilot-instructions.md`, `.github/roles.md`, `.github/copilot/prompts/feature-implementation.md`, the three issue templates, `docs/troubleshooting/README.md`, the two diagram READMEs | §3.6 |
| `specs/stories/177-worker-modules-to-libraries-remove-solr.md` | this spec, §7 filled in |

No production class with behaviour changes. `dsh-data` loses an empty class that no test covered,
so its coverage can only rise.

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** the reactor holds the 9 modules of ADR-003 section 2, no module can depend on Solr, and
the live documents describe that reactor.

**Architecture:** removals and one rename, made in the order that keeps every commit building. A
ban on Solr goes in first and fails; the Solr removal turns it green; the worker modules are then
removed or stripped; the documents follow.

**Tech stack:** Maven 3.9.16 with `maven-enforcer-plugin` 3.5.0 inherited from parent-poms 3.11.1,
Spring Boot 2.7.18, Java 17.

**Spec:** this file.

### Global constraints

- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs"). Never pipe `mvn` into `tail`. The Maven local repository is `D:\.m2\repository`.
- `-DintegrationTests` starts RabbitMQ in Docker. If `docker info` fails, start Docker Desktop from
  `D:\Docker\app` and wait until `docker info` succeeds.
- Commands run in Git Bash from the repository root. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`.
- Indentation: the root `pom.xml` uses four spaces; every module POM uses tabs. The module-POM
  snippets below are shown with spaces, for markdownlint, and are matched in the files with tabs.
- Never set `coverage.data.check.skip` and never pass `-Denforcer.skip=true` (§3.2, §3.5).
- Commit messages are `build(#177): …` and `docs(#177): …`, ending with the session's attribution
  lines. Nothing is pushed before step 6.

### Task 1: Baseline

- [ ] **Step 1:** Gate 1 on the untouched branch:

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-baseline.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`, and a reactor summary of 13 modules.
- [ ] **Step 2:** Record the unit test count per module:

  ```bash
  grep -E '^\[(INFO|WARNING|ERROR)\] Tests run: [0-9]+' .logs/mvn-clean-install-baseline.log | grep -v ' - in '
  sed -n '/Reactor Summary/,/BUILD/p' .logs/mvn-clean-install-baseline.log
  ```

  The first prints one total per module that ran tests (per-class lines carry " - in "); the
  second, the reactor summary. Write both into §7.

### Task 2: The ban, red (AC004)

**Files:** modify `pom.xml`.

- [ ] **Step 1:** In `pom.xml`, inside the `maven-enforcer-plugin`'s `<executions>`, after the
  closing `</execution>` of `enforce-lowercase-artifact-id`, insert the execution of §3.5, indented
  with four spaces per level to match.
- [ ] **Step 2:** Run it, expecting failure:

  ```bash
  mvn -B clean install > .logs/mvn-clean-install-red.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-red.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -n -E 'BannedDependencies|spring-data-solr|solr-solrj|BUILD FAILURE|Failed to execute' .logs/mvn-clean-install-red.log | head -20
  ```

  Expected: `maven exit=1`. The first failing module is `dsh-data`, in
  `maven-enforcer-plugin:3.5.0:enforce (enforce-no-solr)`, naming
  `org.springframework.data:spring-data-solr`. Record the lines in §7. Do not commit.

### Task 3: Remove Solr, green (AC004)

**Files:** delete `dsh-solr/**` and `SOLRIndexedDocument.java`; modify `pom.xml`,
`dsh-data/pom.xml`, `dsh-rest-api/pom.xml`, `dsh-coverage-report/pom.xml`,
`dsh-doc-indexer-worker/pom.xml`, `dsh-doc-analyser/dsh-doc-processor-worker/pom.xml`.

- [ ] **Step 1:** Delete the module and the class:

  ```bash
  git rm -r -q dsh-solr
  git rm -q dsh-data/src/main/java/com/mriss/dsh/data/index/dao/solr/SOLRIndexedDocument.java
  ```

- [ ] **Step 2:** `pom.xml`: delete `<module>dsh-solr</module>`, and in `dependencyManagement`
  delete the two `<dependency>` blocks with groupId `com.mriss.products.dsh.dsh-solr`
  (`solr-terms-vector-order`, `solr-advanced-numbers-filter`).
- [ ] **Step 3:** `dsh-coverage-report/pom.xml`: delete the same two `<dependency>` blocks.
- [ ] **Step 4:** `dsh-data/pom.xml`: delete these lines:

  ```xml
          <!-- spring-boot-starter-data-solr removed in Spring Boot 2.6; use spring-data-solr directly -->
          <dependency>
              <groupId>org.springframework.data</groupId>
              <artifactId>spring-data-solr</artifactId>
          </dependency>
  ```

- [ ] **Step 5:** Delete the same five lines from `dsh-doc-indexer-worker/pom.xml` and from
  `dsh-doc-analyser/dsh-doc-processor-worker/pom.xml`. Both modules go in Tasks 4 and 5; removing
  the lines now keeps this commit green.
- [ ] **Step 6:** `dsh-rest-api/pom.xml`: delete these four lines:

  ```xml
          <!-- <dependency> -->
          <!-- <groupId>org.springframework.boot</groupId> -->
          <!-- <artifactId>spring-boot-starter-data-solr</artifactId> -->
          <!-- </dependency> -->
  ```

- [ ] **Step 7:** Gate 1, logged to `.logs/mvn-clean-install-green.log` as in Task 1. Expected:
  `maven exit=0`, 10 modules, `enforce-no-solr` passing in each.
- [ ] **Step 8:** Commit:

  ```bash
  git add -A pom.xml dsh-data dsh-rest-api dsh-coverage-report dsh-doc-indexer-worker dsh-doc-analyser dsh-solr
  git commit -m "build(#177): ban Solr from the build, and remove dsh-solr and spring-data-solr"
  ```

### Task 4: Remove `dsh-doc-processor-worker` (AC001)

**Files:** delete `dsh-doc-analyser/dsh-doc-processor-worker/**`; modify `pom.xml`,
`dsh-doc-analyser/pom.xml`, `dsh-coverage-report/pom.xml`.

- [ ] **Step 1:** `git rm -r -q dsh-doc-analyser/dsh-doc-processor-worker`
- [ ] **Step 2:** `dsh-doc-analyser/pom.xml`: delete `<module>dsh-doc-processor-worker</module>`.
- [ ] **Step 3:** `pom.xml` and `dsh-coverage-report/pom.xml`: delete the `<dependency>` block with
  artifactId `dsh-doc-processor-worker`.
- [ ] **Step 4:** Build without tests, logged to `.logs/mvn-install-skiptests.log`:
  `mvn -B -DskipTests install`. Expected: `maven exit=0`, 9 modules. The full gate runs in Task 5.
- [ ] **Step 5:** Commit:

  ```bash
  git add -A pom.xml dsh-doc-analyser dsh-coverage-report
  git commit -m "build(#177): remove dsh-doc-processor-worker, whose job is now the pipeline's"
  ```

### Task 5: Rename the indexer, and strip the three libraries (AC002, AC003, AC005)

**Files:** rename `dsh-doc-indexer-worker` → `dsh-doc-indexer`; delete each library's class, tests
and resources; modify the three library POMs, `pom.xml`, `dsh-coverage-report/pom.xml`.

- [ ] **Step 1:** Rename: `git mv dsh-doc-indexer-worker dsh-doc-indexer`, then
  `rm -rf dsh-doc-indexer-worker` if an untracked `target/` was left behind.
- [ ] **Step 2:** Strip each library:

  ```bash
  for m in dsh-doc-indexer dsh-doc-analyser/dsh-keyword-extractor dsh-doc-analyser/dsh-top-sentences-extractor; do
    git rm -r -q "$m/src/main" "$m/src/test"
    rm -rf "$m/src/main" "$m/src/test"
  done
  git ls-files dsh-doc-indexer dsh-doc-analyser/dsh-keyword-extractor dsh-doc-analyser/dsh-top-sentences-extractor
  ```

  Expected listing per module: `maven-site-deploy.sh`, `maven-site.sh`, `pom.xml`,
  `src/site-desc/site.xml`, `src/site/**`. Nothing under `src/main` or `src/test`.
- [ ] **Step 3:** `dsh-doc-indexer/pom.xml`: `artifactId` `dsh-doc-indexer`; `<name>DSH - Document
  Indexer</name>`; `<description>DSH Document Indexer</description>`. Delete the `FR005` comment
  line and the whole `<dependencies>` block (after Task 3: `spring-boot-starter-amqp`,
  `spring-boot-starter-test`, `dsh-data`).
- [ ] **Step 4:** Both extractor POMs: delete the `FR005` comment line and the whole
  `<dependencies>` block (`spring-boot-starter`, `spring-boot-starter-test`).
- [ ] **Step 5:** `pom.xml`: `<module>dsh-doc-indexer-worker</module>` → `<module>dsh-doc-indexer</module>`;
  in `dependencyManagement`, `<artifactId>dsh-doc-indexer-worker</artifactId>` →
  `<artifactId>dsh-doc-indexer</artifactId>`. `dsh-coverage-report/pom.xml`: the same artifactId
  change.
- [ ] **Step 6:** Gate 1, logged to `.logs/mvn-clean-install.log` as in Task 1. Expected:
  `maven exit=0`, 9 modules; "JAR will be empty" for each of the three libraries; no
  `enforce-coverage-data-exists` failure.
- [ ] **Step 7:** Commit:

  ```bash
  git add -A pom.xml dsh-doc-indexer dsh-doc-indexer-worker dsh-doc-analyser dsh-coverage-report
  git commit -m "build(#177): rename the indexer to dsh-doc-indexer, and reduce the three stage modules to libraries"
  ```

### Task 6: The documents (AC006)

**Files:** the documents of §3.6.

- [ ] **Step 1:** Make each edit in §3.6's table, with §3.6.1 and §3.6.2 as given. Keep each
  file's existing line width and list style.
- [ ] **Step 2:** Check what still names an old module:

  ```bash
  git grep -n -e dsh-doc-indexer-worker -e dsh-doc-processor-worker -e dsh-solr -- \
    CLAUDE.md specs/architecture/system-design.md src/site/markdown/README.md \
    dsh-doc-indexer/src/site dsh-doc-analyser/src/site .github/copilot-instructions.md \
    .github/roles.md .github/copilot/prompts/feature-implementation.md .github/ISSUE_TEMPLATE \
    docs/troubleshooting/README.md specs/architecture/component-diagrams/README.md \
    specs/architecture/sequence-diagrams/README.md
  git grep -n -i solr -- CLAUDE.md .github/copilot/rules/testing-patterns.md .claude/skills/dsh-ship-story/SKILL.md
  ```

  Expected: the first finds only the text of §3.6.1 and §3.6.2 that says a module was renamed or
  removed: the indexer entry and the removal paragraph of the README, and the last paragraph of the
  indexer's page. The second finds only `CLAUDE.md`'s "Solr was removed by `#177`".
- [ ] **Step 3:** markdownlint, over the command in `CLAUDE.md`, plus the two module pages:

  ```bash
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json; echo "lint exit=$?"
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint src/site/markdown/README.md dsh-doc-indexer/src/site/markdown/index.md dsh-doc-analyser/src/site/markdown/index.md --config .markdownlint.json; echo "lint exit=$?"
  ```

  Expected: `lint exit=0` twice.
- [ ] **Step 4:** Commit:

  ```bash
  git add CLAUDE.md .github/copilot/rules/testing-patterns.md .claude/skills/dsh-ship-story/SKILL.md \
    specs/architecture/system-design.md src/site/markdown/README.md \
    dsh-doc-indexer/src/site/markdown/index.md dsh-doc-analyser/src/site/markdown/index.md \
    .github/copilot-instructions.md .github/roles.md .github/copilot/prompts/feature-implementation.md \
    .github/ISSUE_TEMPLATE docs/troubleshooting/README.md \
    specs/architecture/component-diagrams/README.md specs/architecture/sequence-diagrams/README.md
  git commit -m "docs(#177): describe the reactor ADR-003 section 2 leaves"
  ```

### Task 7: The gates and the checks (all ACs)

- [ ] **Step 1:** Gate 1, logged to `.logs/mvn-clean-install.log`. Expected: `maven exit=0`. Record
  the unit test count per module, as in Task 1 Step 2, and compare: the only change is the four
  `*ApplicationTest` classes, 8 methods, gone.
- [ ] **Step 2:** AC007 and gate 3, with `docker info` succeeding first:

  ```bash
  mvn -B clean install -DintegrationTests > .logs/mvn-clean-install-integration.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install-integration.log"
  wait $MVN_PID; echo "maven exit=$?"
  grep -o 'Running com\.mriss[^ ]*ApplicationIT' .logs/mvn-clean-install-integration.log | sort -u
  ```

  Expected: `maven exit=0`, and one line: `Running com.mriss.dsh.restapi.integration.DshRestApplicationIT`.
- [ ] **Step 3:** The checks of §4, each with its output recorded in §7:

  ```bash
  ls -d dsh-solr dsh-doc-analyser/dsh-doc-processor-worker dsh-doc-indexer-worker 2>&1
  git grep -n 'dsh-doc-indexer-worker' -- '*pom.xml'
  grep -n 'dsh-doc-indexer<' dsh-coverage-report/pom.xml
  git grep -l -e '@SpringBootApplication' -e 'static void main' -- dsh-doc-indexer dsh-doc-analyser
  git ls-files dsh-doc-indexer dsh-doc-analyser | grep /src/main/
  git grep -n -i solr -- '*pom.xml'
  git grep -n -i solr -- '*/src/main/*'
  git grep -n -i amqp -- dsh-doc-indexer dsh-doc-analyser
  ```

- [ ] **Step 4:** Fill in §7 and commit: `docs(#177): record the verification`.

## 7. Verification results

Run on 2026-10-20, in Git Bash on Windows 11, Maven 3.9.16, Java 17.0.20.1, Docker Desktop running.

| Step | Command | Result |
|---|---|---|
| Task 1, baseline | `mvn -B clean install`, at `3ab381a97` | `maven exit=0`; 13 modules; 155 unit tests (below) |
| Task 2, red (AC004) | `mvn -B clean install`, with `enforce-no-solr` added | `maven exit=1`; `dsh-data` fails in `enforce (enforce-no-solr)` (below) |
| Task 3, green (AC004) | `mvn -B clean install` | `maven exit=0`; 10 modules; `enforce-no-solr` passes in each. Two runs failed first (§7.1) |
| Task 4 | `mvn -B -DskipTests install` | `maven exit=0`; 9 modules |
| Task 5 | `mvn -B clean install` | `maven exit=0`; 9 modules; "JAR will be empty" for the three libraries and, as before, `dsh-coverage-report`; `enforce-coverage-data-exists` passes in each |
| Task 6 (AC006) | markdownlint, the command in `CLAUDE.md`, and over the three site pages | `lint exit=0` twice, after removing a blank line the analyser page already ended with |
| Task 7, gate 1 | `mvn -B clean install` | `maven exit=0`; 9 modules; 115 unit tests; "All coverage checks have been met." in `dsh-data` and `dsh-rest-api` |
| Task 7, AC007 and gate 3 | `mvn -B clean install -DintegrationTests` | `maven exit=0` on the second run; 115 unit and 19 integration tests; the first run hit `#142` (§7.2) |

The red run:

```text
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce (enforce-no-solr) on project dsh-data:
[ERROR] Rule 0: org.apache.maven.enforcer.rules.dependency.BannedDependencies failed with message:
[ERROR] Solr was removed by ADR-003 section 2 (#177): no module may depend on it, directly or transitively. Lucene's own artifacts (org.apache.lucene) remain allowed.
[ERROR] com.mriss.products.dsh:dsh-data:jar:0.4.0-SNAPSHOT
[ERROR]    org.springframework.data:spring-data-solr:jar:4.3.15 <--- banned via the exclude/include list
```

**Unit tests, before and after.** Before: `dsh-data` 71, `dsh-rest-api` 44,
`solr-terms-vector-order` 22, `solr-advanced-numbers-filter` 10, and 2 in each of the four worker
modules, 155 in all. After: `dsh-data` 71, `dsh-rest-api` 44, 115 in all. **The Gates 1, 2 row of
§4 was wrong** to expect only the four `*ApplicationTest` classes, 8 methods, to go: the two Solr
plugins' 32 tests went with `dsh-solr`, so 40 tests went. Task 1 Step 2's filter was wrong too:
surefire 3 writes per-class lines with `-- in`, not ` - in `, so the grep prints both kinds; the
per-module totals are the lines without a class name.

**The checks of §4, after the last commit:**

| AC | Command | Output |
|---|---|---|
| AC001 | `ls -d dsh-solr dsh-doc-analyser/dsh-doc-processor-worker dsh-doc-indexer-worker` | "No such file or directory" for each of the three |
| AC002 | `git grep -n 'dsh-doc-indexer-worker' -- '*pom.xml'` | no hit |
| AC002 | `grep -n 'dsh-doc-indexer<' dsh-coverage-report/pom.xml` | `25:            <artifactId>dsh-doc-indexer</artifactId>` |
| AC003 | `git grep -l -e '@SpringBootApplication' -e 'static void main' -- dsh-doc-indexer dsh-doc-analyser` | no hit |
| AC003 | `git ls-files dsh-doc-indexer dsh-doc-analyser \| grep /src/main/` | no file |
| AC004 | `git grep -n -i solr -- '*pom.xml'` | `pom.xml` lines 208, 216, 217 and 219, all `enforce-no-solr`'s |
| AC004 | `git grep -n -i solr -- '*/src/main/*'` | no hit |
| AC005 | `git grep -n -i amqp -- dsh-doc-indexer dsh-doc-analyser` | no hit |
| AC006 | Task 6 Step 2's two greps | the indexer entry and the removal paragraph of `src/site/markdown/README.md`, and line 15 of the indexer's page; `CLAUDE.md:5`, "Solr was removed by `#177`" |
| AC007 | `grep -o 'Running com\.mriss[^ ]*ApplicationIT'` over the gate 3 log | `Running com.mriss.dsh.restapi.integration.DshRestApplicationIT` only |

### 7.1 What the spec missed

- **Three libraries came only through `spring-data-solr`.** The first green run failed to compile
  `dsh-data`: `Document` imports `org.apache.commons.io.IOUtils` and
  `org.springframework.data.annotation.Id`. With those declared, the second failed `dsh-rest-api`:
  `DocumentResource` imports `org.apache.commons.lang3.StringUtils`. `dsh-data` now declares
  `spring-data-commons` and `commons-io`, and `dsh-rest-api` declares `commons-lang3`, each without
  a version: `commons-io` is managed by `mriss-parent` (2.18.0), the other two by the Spring Boot
  BOM. Each resolves to the version it had before. A diff of `dsh-rest-api`'s runtime
  `dependency:list` against `3ab381a97` shows removals only: 29 jars, all of them Solr's tree
  (`solr-solrj`, `spring-data-solr`, ZooKeeper, Netty, Jetty's HTTP client, Apache HttpClient,
  `commons-lang` 2.6, `commons-math3`, `snappy-java`). No DSH source imports any of them.
  `@Id` is read by nothing since `#167`; whether `Document` keeps it is left to the store's own
  story, not decided here.
- **`management.health.solr.enabled=false`** in `dsh-rest-api`'s `application.properties` failed
  AC004's second grep. It was deleted, with its comment. It was inert before this story too: Spring
  Boot's Solr health contributor needs a `SolrClient` bean, and DSH never defined one.
- **The first wording of the two new POM comments named `spring-data-solr`**, which failed AC004's
  first grep. They now say only that the dependency `#177` removed brought the library.

### 7.2 Gate 3 and `#142`

Gate 3 ran three times. The first, before the changes of §7.1's last two items, exited 0. The second,
after them, exited 1 on
`DocumentResourceHttpIT.submit_whenSameFileTwice_shouldReturnTheSameTokenAndQueueItAgain`: the
document's status was `DEQUEUED_FOR_INDEXING`, not `QUEUED_FOR_INDEXING_SUCCESS`. That is `#142`,
with the signature `#140` §7.1 recorded. The server's log shows the test's two submissions
subscribing two handlers to `toRabbitResponse`, three subscribers with the service activator,
before either confirm arrived. The first confirm reached both handlers, which hold the same
document, so the document moved two steps, and the second confirm reached none. The only
`dsh-rest-api` change between the two runs was deleting the inert property of §7.1. The third run
exited 0.

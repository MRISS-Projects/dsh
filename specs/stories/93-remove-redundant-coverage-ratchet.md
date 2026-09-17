---
issue: 93
slug: remove-redundant-coverage-ratchet
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 93 — Remove the redundant coverage ratchet

## 1. Story

**As a** developer reading the build configuration
**I want** a single coverage gate rather than two that disagree
**So that** "what is the coverage rule here?" has one answer

## 2. Context

DSH has two coverage gates. They use different counters, different scopes, and can disagree.

**Gate one — inherited, and the one that stays.** `jacoco:check` is declared in
`MRISS-Projects/parent-poms`, root `pom.xml`, inside `<build><plugins>`, so every module of every
consuming project inherits it:

```xml
<execution>
    <id>check-code-coverage</id>
    <phase>verify</phase>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.95</minimum></limit>
                    <limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.95</minimum></limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

Bound to `verify`, so `mvn install` runs it and a violation fails the build.

**This was verified against a real build log, not read off a pom.** `.logs/mvn-final.log` shows the
goal firing on all 13 modules. Eight enforce; five skip:

| Outcome | Modules |
|---|---|
| `All coverage checks have been met.` | `dsh-data`, `dsh-rest-api`, `solr-terms-vector-order`, `solr-advanced-numbers-filter`, `dsh-doc-indexer-worker`, `dsh-keyword-extractor`, `dsh-top-sentences-extractor`, `dsh-doc-processor-worker` |
| `Skipping JaCoCo execution due to missing execution data file` | `dsh` (root), `dsh-solr`, `dsh-doc-analyser` (aggregators), `dsh-test-dataset`, `dsh-coverage-report` |

**Gate two — local, and the one this story removes.** `scripts/check-coverage.sh` parses
`dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv`, sums columns 4 and 5 into an
aggregate `INSTRUCTION` ratio, and compares it against `.github/coverage-baseline.txt`. It runs as
the `Coverage ratchet` step in `.github/workflows/ci.yml`, lines 148-153.

The two differ on every axis: `INSTRUCTION` versus `LINE` and `BRANCH`; whole-aggregate versus
per-module; a committed baseline file versus a fixed floor. The inherited one is per-module and
build-failing, so a module can fail it while the aggregate still clears the baseline. It also needs
no file to be kept in step with reality.

**The baseline has already decayed.** `.github/coverage-baseline.txt` holds `95.00`, while the
aggregate this repository actually produces is `98.13`. A ratchet set three points below the
current figure is not ratcheting anything — it permits a silent slide from 98.13 to 95.00. This is
not an argument for raising it; it is evidence that a second gate needing manual upkeep does not
get that upkeep.

## 3. The blind spot the removal opens

`jacoco:check` skips when there is no `jacoco.exec`. For the five skipping modules above that is
correct — four are aggregators and `dsh-test-dataset` ships fixtures, and none holds a class.

But the skip is keyed on missing exec data, not on missing classes. A **new module with production
classes and zero tests** runs no tests, writes no `jacoco.exec`, and the check skips it. The build
is green with that module at 0% coverage. The aggregate ratchet would have caught exactly that,
because a new uncovered module drags the aggregate ratio down.

So the inherited gate is stronger *per module* but blind to a module that never ran a test at all.
Deleting the ratchet without closing that blind spot trades one weakness for another, which is why
this story ships the two changes together.

**The fix belongs upstream.** `CLAUDE.md` is explicit that coverage gating is parent-poms'
responsibility, and the guard completes the `jacoco:check` that already lives there. Building a
DSH-local equivalent would reproduce the exact mistake that created this issue: a local replacement
for something the parent owns.

## 4. The upstream change in `MRISS-Projects/parent-poms`

### 4.1 What is added

One execution on `maven-enforcer-plugin`, in root `pom.xml` `<build><plugins>`, inserted between
the jacoco plugin's closing `</plugin>` (line 670) and `</plugins>` (line 671) — immediately after
the `jacoco:check` it completes:

```xml
<!-- Normalises the two paths the coverage-data guard below tests. A backslash
     inside a BeanShell string literal is an escape sequence, so interpolating
     a Windows path such as C:\Users\... makes the condition unparseable rather
     than false, and the rule then fails on every module. Replacing the
     separators keeps the guard evaluable on Windows and Linux alike, while
     still reading the directories the module actually configured. -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>coverage-guard-source-dir</id>
            <phase>validate</phase>
            <goals><goal>regex-property</goal></goals>
            <configuration>
                <name>coverage.guard.source.dir</name>
                <value>${project.build.sourceDirectory}</value>
                <regex>\\</regex>
                <replacement>/</replacement>
                <failIfNoMatch>false</failIfNoMatch>
            </configuration>
        </execution>
        <execution>
            <id>coverage-guard-exec-file</id>
            <phase>validate</phase>
            <goals><goal>regex-property</goal></goals>
            <configuration>
                <name>coverage.guard.exec.file</name>
                <value>${project.build.directory}/jacoco.exec</value>
                <regex>\\</regex>
                <replacement>/</replacement>
                <failIfNoMatch>false</failIfNoMatch>
            </configuration>
        </execution>
    </executions>
</plugin>
<!-- Completes jacoco:check, which skips a module that produced no exec data.
     A module with production sources that ran no tests would otherwise pass
     the coverage gate at 0%. -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-coverage-data-exists</id>
            <phase>verify</phase>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <skip>${coverage.data.check.skip}</skip>
                <rules>
                    <evaluateBeanshell>
                        <condition><![CDATA[
                            "true".equals("${skipTests}")
                                || "true".equals("${maven.test.skip}")
                                || "true".equals("${maven.test.skip.exec}")
                                || "true".equals("${jacoco.skip}")
                                || !new java.io.File("${coverage.guard.source.dir}").isDirectory()
                                || new java.io.File("${coverage.guard.exec.file}").isFile()
                        ]]></condition>
                        <message>This module has production sources (its configured source directory exists) but produced no coverage data (no jacoco.exec in its build directory), so jacoco:check skipped it and the 95% coverage gate did not run. Add tests, or set -Dcoverage.data.check.skip=true if this module is genuinely exempt. Skipping tests (-DskipTests, -Dmaven.test.skip) or coverage (-Djacoco.skip) disarms this check along with the thing being skipped.</message>
                    </evaluateBeanshell>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Neither plugin needs a `<version>`: the enforcer resolves from the existing `pluginManagement`
entry at line 573 (`${enforcer.plugin.version}` = `3.5.0`, which ships `evaluateBeanshell`), and
`build-helper-maven-plugin` from the one at line 513.

**The normalisation is not optional, and was added after the first implementation attempt failed.**
Interpolating `${project.build.sourceDirectory}` directly into the condition, as this section
originally specified, produces `"C:\Users\marce\github\dsh\dsh-data\src\main\java"` on Windows.
BeanShell reads `\U`, `\m` and `\g` as escape sequences, so the rule does not evaluate to `false` —
it fails outright with `Couldn't evaluate condition`, on **every** module. CI runs on Linux, where
the same pom is fine, so the original form would have passed the pull request and broken every
local build on Windows. Normalising the separators first is what makes the guard portable.

**`<regex>\\</regex>` is a regex escape, not an XML escape, and must stay doubled.**
Plexus passes the element's text through literally, so the plugin receives the two characters
`\\` and `Pattern.compile` reads them as one escaped backslash matching a single
separator. Written as one character it becomes `Pattern.compile("\")` — a dangling escape that
throws at `validate` on every module, failing harder than the bug the normalisation fixes. This is
not hypothetical: it happened twice while building this story, once in the pom and once in this
document, because a doubled backslash is easy to collapse when generating a file.

**One property** is added to the root `<properties>` block (opens at line 75), beside
`enforcer.plugin.version` at line 133:

```xml
<coverage.data.check.skip>false</coverage.data.check.skip>
```

It needs a declared default because it feeds `<skip>`, a boolean parameter that would fail type
conversion on an uninterpolated `${...}` literal.

The test-skipping properties the condition reads are deliberately **not** declared. An undefined
Maven property survives interpolation as the literal string `${skipTests}`, which
`"true".equals(...)` already reads as "not skipped" — exactly the behaviour the guard wants. So the
defaults bought nothing, while declaring names as globally meaningful as `skipTests` and
`maven.test.skip` in a shared **root** parent is hard to unwind once consumers exist and is read by
the compiler, surefire, failsafe and an unknown number of third-party plugins. An earlier draft of
this section declared all three; the two test-skipping ones were removed after a build matrix
confirmed the guard behaves identically without them.

A command-line `-D` still works in every case, because it defines the property that was previously
defaulted: `-DskipTests`, `-Dmaven.test.skip=true`, `-Dmaven.test.skip.exec=true` and
`-Djacoco.skip=true` each disarm the guard along with the thing they skip.

`jacoco.skip` and `maven.test.skip.exec` are in the condition for a reason found by testing:
`-Djacoco.skip=true` suppresses `prepare-agent`, so **tests run and pass** but no exec file is
written. Without that clause the guard failed a `dsh-data` build in which all 51 tests passed,
telling the developer to "add tests" they already had. Turning coverage off is a legitimate thing
for a consumer to do and must not break their build.

Two limitations remain, and neither is detectable from properties: surefire or jacoco disabled by
**plugin configuration** rather than by property (`<configuration><skip>true</skip></configuration>`)
still trips the guard. `coverage.data.check.skip` is the remedy in those cases.

### 4.2 Why the condition keys on `src/main/java`

The obvious test — "does `target/classes` hold anything?" — is wrong here, and checking it caught
the bug before it was written. `dsh-test-dataset` maps `src/test/resources` straight into the
**main** output directory (`dsh-test-dataset/pom.xml:54-59`, no `targetPath`), so its
`target/classes` currently holds four PDF fixtures plus a `META-INF/maven/remote-resources.xml`
marker written by `maven-remote-resources-plugin` — five files, zero classes. A
non-empty-directory test would demand exec data from a module that is correctly exempt, and fail
the build on it.

That marker file is the wider point: `maven-remote-resources-plugin` can put one in any module's
`target/classes`, so "non-empty" is not a reliable proxy for "has classes" anywhere in this
reactor, not just in the one module where it currently misfires.

Counting `**/*.class` recursively would be correct but needs a directory walk inside a BeanShell
expression. `${project.build.sourceDirectory}` is a standard Maven property, needs no walk, and
selects the right set for every module in this reactor. Verified across the whole reactor:

| Has `src/main/java` | Modules | Current `jacoco:check` behaviour |
|---|---|---|
| Yes | the 8 enforcing modules listed in §2 | enforces |
| No | `dsh` (root), `dsh-solr`, `dsh-doc-analyser`, `dsh-test-dataset`, `dsh-coverage-report` | skips |

The two partitions are identical **for DSH today**. The guard changes nothing about the current
build and only fires on a module that is new or newly broken.

**It is not a general test for "has production code", and the original blind spot stays open where
it is not.** The property names one directory, so production code that lives anywhere else is
invisible to the guard exactly as it was before:

- another JVM language — Kotlin, Groovy or Scala under `src/main/kotlin` and friends
- sources registered with `build-helper:add-source`
- **generated code**, which matters most here: this same parent offers `castor-maven-plugin`,
  `gwt-maven-plugin` and `hibernate4-maven-plugin` in `pluginManagement`, so a consumer module whose
  production classes are entirely generated, with no tests, still passes the coverage gate at 0%

That is a residual gap, not a regression — those modules were unguarded before this change too. It
is recorded here rather than closed because closing it needs the class-counting walk this section
rejected, and no module in DSH is in that position. A consumer that is should not rely on this
guard.

Evaluating at execution time also avoids profile `<activation><file><exists>` semantics, whose
relative-path resolution for a profile inherited into a multi-module reactor is unreliable
(MNG-2363). A gate is the wrong place to depend on that.

### 4.3 Blast radius, and why the opt-out exists

This lands in the shared parent, so it reaches **every** consuming repository on the next snapshot
deploy — `mail-processor-service` included, and any project not audited here.

Within parent-poms itself the guard is inert: no reactor module has `src/main/java`. The only
matches under that path are template files inside
`infrastructure/maven-archetypes/*/src/main/resources/archetype-resources`, which are archetype
*resources*, not modules in the build.

`coverage.data.check.skip` exists for the downstream case. A project that legitimately cannot
satisfy the guard sets one property instead of being broken by a parent deploy it did not ask for.
That is the one piece of defensiveness this design keeps, and it is there because the blast radius
is wider than the repository being changed.

Four caveats a consumer should know about, all found by testing rather than by reading:

- **`-Denforcer.skip=true` does not skip this execution — but it does skip DSH's other one.** An
  explicit `<skip>` in the plugin configuration takes precedence over the parameter's
  `enforcer.skip` user property, so the usual global escape hatch is inert against this guard;
  `coverage.data.check.skip` is the one that works. It is the only enforcer execution in
  parent-poms, but not the only one in a consuming build: DSH binds `enforce-lowercase-artifact-id`
  in its own root `pom.xml`, with no `<skip>`, and that one *is* skipped by the flag. A single run
  with `-Denforcer.skip=true` shows both behaviours in one log — `Skipping Rule Enforcement.` for
  the lowercase rule, and the coverage guard failing the build regardless. The flag therefore drops
  a check the developer wanted while leaving armed the one they were trying to bypass, which is why
  it should not be reached for at all.
- **A stale `jacoco.exec` disarms the guard.** The test is `isFile()`, with no check on freshness,
  so any incremental local build that does not re-run tests is satisfied by the previous run's file.
  CI is unaffected because it builds from a fresh checkout. This is a property of the gate, not just
  of how it is tested.
- **`build-helper-maven-plugin` is now bound, not merely managed.** A consumer's first build after
  the snapshot lands must resolve that artifact, so a strictly offline (`-o`) build against a cold
  local repository now fails at `validate` on an unresolvable plugin. One-time and transient, but it
  is a new resolution requirement where there was none.
- **Invoking the goal directly proves nothing.** `mvn enforcer:enforce` outside a lifecycle never
  runs `validate`, so the normalised properties are unset, the source-directory test reads an
  uninterpolated literal and the rule passes. The gate is the `verify` binding, not the goal, so
  this is harmless — but it means a standalone goal run is not a way to test the guard.

### 4.4 How the change is made

Per `CLAUDE.md`'s "The light round trip, for a small change", a change this small goes to
parent-poms directly rather than through an issue and a release:

1. Commit the change on parent-poms `master`. No issue is opened there — the policy allows a direct
   commit for a small infrastructure change **on condition that the commit stays referenced from
   the originating issue here**, which AC006 enforces.
2. `mvn -B install` locally in parent-poms, so `3.8.0-SNAPSHOT` in the local repository carries the
   guard. This is what lets DSH's local verification run without `-U`.
3. Dispatch parent-poms' `deploy.yml` with `release_type: snapshots`. **Deploy, not release** —
   there is no version bump, no tag, and no milestone to clear.
4. Comment the resulting commit SHA on DSH issue `#93`.

No re-pin is needed. DSH's root `pom.xml` already names `3.8.0-SNAPSHOT`, and both DSH workflows
pass `-U` (`#99`), so CI picks up the redeployed snapshot on the next run.

**That subsection did not exist when this spec was written, and adding it is part of this story.**
`CLAUDE.md` documented only the full round trip — issue, milestone, release, re-pin — so the
citation above originally pointed at a policy the cited document did not contain. The light path
was real, but it lived outside the repository. That is the same defect `#93` exists to correct, in
the same file that corrects it, so the fix is to write the policy down rather than to stop
referring to it.

**This story took the round trip twice.** A review round after the first deploy found two problems
in the guard — see §4.1 — so a second commit and a second `deploy.yml` dispatch followed. Both
SHAs are referenced on `#93`, which is what step 4 requires; nothing else about the path changes
when it repeats.

## 5. Files to change in DSH

### 5.1 Deleted

| File | Note |
|---|---|
| `scripts/check-coverage.sh` | AC001 |
| `scripts/test-check-coverage.sh` | AC001. Its only subject is the script above |
| `.github/coverage-baseline.txt` | AC002 |

`scripts/` survives — `close-wontfix-issues.sh` is unrelated and stays.

### 5.2 `.github/workflows/ci.yml`

Delete the `Coverage ratchet` step, lines 148-153, whole step including its `run:` block. Nothing
else in the file changes: the `Upload coverage report` step still publishes the aggregate, and the
job name `Build, Test and Coverage Gate` is still accurate — after this change the build *is* the
coverage gate.

### 5.3 `CLAUDE.md`

Remove the Commands-table row at line 50:

```text
| Coverage gate (after a full build) | `./scripts/check-coverage.sh` |
```

There is no separate coverage command any more, so the row is deleted rather than rewritten.

Rewrite quality gate 2 (line 99) and the paragraph below it (lines 101-102). Gate 2 becomes the
inherited `jacoco:check` — 95% LINE and BRANCH, per module, bound to `verify` — plus the
`enforce-coverage-data-exists` guard, and the paragraph states that `mvn -B install` runs both
gates, with no second command to run. It must also say they are inherited from parent-poms and will
not be found by grepping this repository, which is the specific failure `#93` exists to correct.

Add a paragraph naming the properties that disarm the guard — `-DskipTests`,
`-Dmaven.test.skip=true`, `-Djacoco.skip=true` and `-Dcoverage.data.check.skip=true` — and the fact
that `-Denforcer.skip=true` does not. A documented gate with an undocumented escape hatch sends the
next developer to read the parent pom, which is the lookup this story is trying to remove.

**Add "The light round trip, for a small change" to the "The round trip for a shared change"
section.** `CLAUDE.md` documents only the full path — issue, milestone, release, re-pin — so the
route this very story takes to parent-poms is not written down anywhere in the repository. §4.4
cites it, and a citation to an absent policy is the defect `#93` exists to fix. The subsection
states the four steps, and that commenting the SHA on the originating issue is the condition for
using the short path rather than a courtesy, since the commit has no issue of its own.

### 5.4 `docs/process/ai-driven-development.md`

Step 6's **Artifact** paragraph, line 159: replace "the coverage ratchet against
`.github/coverage-baseline.txt`" with the inherited `jacoco:check`. The surrounding sentences about
surefire and `#46` are unaffected.

### 5.5 `docs/devops/README.md`

Two edits:

- Line 43, the Pipeline Map mermaid node: `build, tests, coverage ratchet` becomes
  `build, tests, inherited coverage gate`.
- Line 66, the `ci.yml` row of the Workflow Reference table: drop "then runs the coverage ratchet
  (`scripts/check-coverage.sh` against `.github/coverage-baseline.txt`)" and state that
  `mvn -B -U install` is itself the gate, naming both `jacoco:check` and `enforce-coverage-data-exists`
  as bound to `verify` and inherited.

### 5.6 `.github/copilot/rules/testing-patterns.md`

`## Coverage Requirements`, lines 86-89, currently reads:

```text
- Minimum 80% line coverage for service classes
- Minimum 70% line coverage overall (enforced by dsh-coverage-report module)
```

Both figures are wrong and the attribution is wrong — `dsh-coverage-report` aggregates for
reporting and enforces nothing. This file was not in the issue's original AC004 and was found by
sweeping the repository for coverage claims while writing this spec. It is the same class of
false-premise document that produced `#93` itself, so leaving it would defeat the story's purpose.

Replace with the real rule: 95% LINE and 95% BRANCH per module, from `jacoco:check` inherited from
parent-poms, failing the build at `verify`; the `enforce-coverage-data-exists` guard; and the
properties that disarm both. Keep the "all critical paths" bullet.

### 5.7 The three skills under `.claude/skills/`

Each runs `./scripts/check-coverage.sh` as a separate gate after the build:

| File | Lines |
|---|---|
| `.claude/skills/dsh-build-story/SKILL.md` | 35, and the paragraphs at 37-42 |
| `.claude/skills/dsh-ship-story/SKILL.md` | 29 |
| `.claude/skills/dsh-pr-cycle/SKILL.md` | 73 |

Remove the invocation from all three. In `dsh-build-story`, the two paragraphs that follow it also
go: they tell the agent that coverage is checked against `.github/coverage-baseline.txt` and that
editing that file falsifies the gate. Both describe a file that will not exist. The replacement
keeps the intent that survives — a red build is not "done with a known issue", and if `jacoco:check`
fails the answer is tests, never weakening the gate.

While rewriting that passage, drop its "all integration tests pass" claim too. `CLAUDE.md` states
there is no failsafe configuration and no `*IT.java` test in the repository (`#46`), so the line
asserted a gate that does not exist — the same fault as the coverage claims this story removes. It
survived only because nothing else in the sentence was wrong.

**Deleting the invocation is not enough on its own.** AC004 requires these skills to *describe* the
inherited gate, and a skill that merely loses a line describes nothing: an agent invoking
`dsh-pr-cycle` alone, which the process explicitly supports, would never learn what the gate is or
that it lives in the parent. So `dsh-ship-story` and `dsh-pr-cycle` each gain the same short
paragraph naming `jacoco:check`, `enforce-coverage-data-exists` and their origin. `dsh-build-story`
already carries the longer version.

These three were not in the issue's original AC004 either. Left alone they would instruct an agent
to run a deleted script on every story.

## 6. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `dsh-coverage-report/pom.xml` | **The aggregation is not the duplicate.** `report-aggregate`, the `<reporting>` section and the `process-badges` profile are the reporting surface for the Maven site, the badge and CI's uploaded artifact. Only the second *gate* is removed. |
| `ci.yml`'s `Upload coverage report` step | Consumes the aggregate report, which still exists. Untouched. |
| Root `pom.xml` | The guard lives in parent-poms, not here. No parent version change either: `3.8.0-SNAPSHOT` already names what the deploy republishes. |
| `specs/product/PRD.md` | Its `#93` paragraph is history and stays true; its badge-versus-aggregate risk bullet concerns the aggregate report, which survives; and the open/closed status row is step 8's job via `dsh-reconcile-prd`. |
| `docs/process/ai-driven-development.md` §"Working across the parent-poms boundary" | Tells the story of the false gap analysis that produced `#93`. This change vindicates it rather than dating it. |
| `specs/stories/92-*.md`, `95-*.md`, `99-*.md` | Shipped stories' records. Each references the ratchet as it existed when written. Rewriting them would falsify the record. `99` §9 already anticipates this story landing and calls the `ci.yml` overlap trivial. |
| `docs/superpowers/plans/2026-09-16-*.md`, `docs/superpowers/specs/2026-09-16-*.md` | Tracked historical records of the work that built the ratchet. The design doc at `2026-09-16-...-design.md:56` already records that the script "was built to fill a gap that was not there" — it documents the error rather than repeating it, so no banner is needed. |
| `README.md` | Its jacoco badge references `dsh-coverage-report/badges/jacoco.svg`, which survives. Its issue table is generated history. |

## 7. Acceptance criteria

- [ ] **AC001** — `scripts/check-coverage.sh` and `scripts/test-check-coverage.sh` are removed.
- [ ] **AC002** — `.github/coverage-baseline.txt` is removed.
- [ ] **AC003** — The `Coverage ratchet` step is removed from `.github/workflows/ci.yml`.
- [ ] **AC004** — `CLAUDE.md`, `docs/process/ai-driven-development.md`, `docs/devops/README.md`,
  `.github/copilot/rules/testing-patterns.md`, and the `dsh-build-story`, `dsh-ship-story` and
  `dsh-pr-cycle` skills describe the inherited `jacoco:check` as *the* coverage gate, and no longer
  reference a baseline file or a coverage script. Checkable: grepping the tracked tree for
  `check-coverage` or `coverage-baseline` returns hits only in `specs/stories/`,
  `docs/superpowers/` and the `#93` history paragraph of `specs/product/PRD.md`, all of which are
  historical records per §6. That last location was missing from this criterion as first written:
  §6 preserves the PRD paragraph deliberately, so the grep can never come back clean of it.
- [ ] **AC005** — A deliberate drop below 95% on a module still fails `mvn -B install`. Verified by
  running it, with the failure output recorded — not assumed from reading the parent pom.
- [ ] **AC006** — parent-poms enforces that a module with production sources produced coverage
  data, committed directly on its `master`, with every commit SHA referenced in issue `#93` and the
  change published via `deploy.yml` (`release_type: snapshots`). No release and no re-pin. Two
  commits satisfy this: the guard itself, and the review-round fix that stops it breaking a build
  with `-Djacoco.skip=true`. The guard must also not fail a build that skipped tests or coverage
  deliberately — a gate that breaks a documented command is a regression, not a stricter gate.
- [ ] **AC007** — The guard is verified from DSH: a module with classes but no exec data fails
  `mvn -B install`, and `mvn -B -DskipTests install` stays green.
- [ ] **AC008** — CI is green on the pull request. No `.java` file appears in the diff, so the
  coverage figures themselves cannot move.
- [ ] **AC009** — Markdown lint passes over the changed documentation, using the command in
  `CLAUDE.md`'s Commands table.

## 8. Testing approach

No production code changes, so there is no red-green unit-test cycle. The gates are the build
itself and two greps. Every Maven run below follows `CLAUDE.md`'s rule — redirected to `.logs/`,
never piped to `tail`, exit code reported explicitly.

**1. Baseline, before any change.** Run `mvn -B install` on the unmodified branch and keep the log.
Everything after this is a comparison against it, and without it a failure cannot be attributed to
the change rather than to a pre-existing break.

**2. AC005 — prove the inherited gate bites.** Deselect a test class from `dsh-data` and rebuild
that module, then confirm `jacoco:check` fails naming the module and the counter:

```bash
mvn -B -pl dsh-data clean install '-Dtest=!MongoDocumentDaoTest' > .logs/mvn-ac005.log 2>&1
```

Expected: `Rule violated for bundle dsh-data: lines covered ratio is 0.91, but expected minimum is
0.95` and `BUILD FAILURE`.

Deselecting a class is preferred over editing a test file, which is what this section originally
called for. It produces the same drop and the same proof, but **nothing is modified, so there is
nothing to revert** — and therefore no way for a deliberately broken test to reach a commit if the
run is interrupted. The remaining tests still execute, so `jacoco.exec` is written and the guard
from §4.1 stays satisfied; this test exercises the coverage ratio, not the guard.

**3. AC007 — prove the new guard bites.** No throwaway module is needed. Running a code-bearing
module with tests selected away produces exactly the state the guard is for: classes present, no
`jacoco.exec`, `jacoco:check` skipping.

```bash
mvn -B -pl dsh-data clean install -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  > .logs/mvn-ac007-guard.log 2>&1
```

Expected: `BUILD FAILURE` from `enforce-coverage-data-exists`, with the message from §4.1. Note
`-Dtest=none` does **not** set `skipTests`, which is why the guard stays armed — that distinction
is the whole test.

Two details in that command are load-bearing, and both were found by running it:

- **`-Dsurefire.failIfNoSpecifiedTests=false`, not `-DfailIfNoTests=false`.** The latter is the
  surefire 2.x name and is ignored by surefire 3.5.5, which fails the build itself with
  `No tests matching pattern "none" were executed!` during `test`. The run never reaches `verify`,
  so it produces a `BUILD FAILURE` that looks like the guard firing and is nothing of the kind.
- **`clean` is required.** Without it the `jacoco.exec` written by the previous build survives, and
  both `jacoco:check` and the guard read that stale file and pass. The guard tests for the presence
  of an exec file, so a leftover one disarms it exactly as a real test run would.

Then prove the escape hatch:

```bash
mvn -B clean -DskipTests install > .logs/mvn-ac007-skiptests.log 2>&1
```

Expected: `BUILD SUCCESS` across all 13 modules. `CLAUDE.md` documents this as a supported command,
so a guard that broke it would be a regression in this story, not an acceptable cost. `clean`
matters here for the same reason: a partial `jacoco.exec` left by step 2 fails `jacoco:check` on
the stale ratio before the guard is ever consulted.

Finally, prove §4.3's opt-out on the same state:

```bash
mvn -B -pl dsh-data clean install -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  -Dcoverage.data.check.skip=true > .logs/mvn-optout.log 2>&1
```

Expected: `BUILD SUCCESS`. This is the one piece of defensiveness §4.3 keeps, so it is worth one
command to know it works rather than assuming it from the `<skip>` element.

**4. Confirm the guard is inert on a normal build.** The full `mvn -B install` must stay green with
all 13 modules, and the log must show `enforce-coverage-data-exists` passing rather than being
skipped wholesale. Compare against the step 1 baseline.

**4b. The guard's disarm semantics, as a matrix.** The condition has six clauses and the properties
that feed four of them are not declared in the pom, so "it works" has to mean each clause was
exercised. The four property clauses get a row each below; the two `java.io.File` clauses are
exercised by the full build in step 4, where the five modules without a source directory take the
`!isDirectory()` branch and the eight with exec data take the `isFile()` one. Every row was run:

| Run | Expected | Why it discriminates |
|---|---|---|
| `mvn -B install` (parent-poms) | SUCCESS, 8/8 | The guard must be inert in its own repository |
| `mvn -B clean install` | SUCCESS, 13/13 | Guard armed on every module and firing on none |
| `mvn -B clean -DskipTests install` | SUCCESS, 13/13 | Proves a CLI `-D` disarms it with **no** POM default present |
| `-pl dsh-data clean install -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false` | **FAILURE** | Proves removing those defaults did not disarm the guard by accident |
| `-pl dsh-data clean install -Djacoco.skip=true` | SUCCESS | Tests run and pass, no exec written — must not fail |
| `-pl dsh-data clean install -Dmaven.test.skip=true` | SUCCESS | No compile, no tests, no exec |
| `-pl dsh-data clean install -Dmaven.test.skip.exec=true` | SUCCESS | Classes compiled, tests not executed, no exec |
| `-pl dsh-data clean install -Dtest=none ... -Denforcer.skip=true` | **FAILURE** | The guard's explicit `<skip>` beats `enforcer.skip`; see §4.3 |

The fourth row is the one that matters most. The three SUCCESS rows above it would all pass a guard
that had been silently disabled, so without a row that must still fail, the matrix would prove
nothing about the removal of the property defaults.

Check the `-DskipTests` log for `EvaluateBeanshell passed` rather than `Skipping Rule Enforcement`:
the first means the condition was evaluated and returned true, the second would mean `<skip>`
swallowed the rule and the clause under test never ran.
**5. Ordering is load-bearing.** Steps 2-4 require §4.4's local `mvn -B install` of parent-poms to
have run first. Running them against the old parent tests nothing: the guard would not exist, step 3
would pass, and the pass would be meaningless. Confirm the local `3.8.0-SNAPSHOT` carries the guard
before starting.

**6. CI is the only proof for AC008.** `ci.yml` runs on the pull request with `-U`, so it resolves
the deployed snapshot rather than the local one. A green run is what proves the deploy in §4.4 step
3 actually published the guard — a local build cannot, because it reads the local repository that
§4.4 step 2 wrote by hand.

**7. Markdown lint for AC009.** `spec-validation.yml` covers `docs/**`, `specs/**`, `CLAUDE.md`,
`.github/copilot/**` and `.claude/**` — every documentation file in this diff — so the job fires on
the PR. Run the `CLAUDE.md` command locally first.

## 9. Out of scope

- **Changing the 95% figure.** A parent-poms decision on its own, and nothing here argues the
  threshold is wrong.
- **Moving `jacoco:check` itself out of parent-poms.** The opposite of this story's direction.
- **The `dsh-coverage-report` aggregation.** Explicitly preserved — see §6. The module stays as
  DSH's coverage reporting surface.
- **Reconciling the badge against the aggregate.** `dsh-coverage-report/badges/jacoco.svg` reads 92%
  while the aggregate computes 98.13%, recorded as a risk in `specs/product/PRD.md`. It concerns the
  reporting path, not the gate, and survives this story untouched.
- **Auditing other parent-poms consumers against the new guard.** `mail-processor-service` and any
  other consumer inherit it on the next snapshot deploy. §4.3's `coverage.data.check.skip` is the
  designed mitigation; a proactive audit is separate work.
- **Reconciling `specs/product/PRD.md`.** Step 8, via `dsh-reconcile-prd`.

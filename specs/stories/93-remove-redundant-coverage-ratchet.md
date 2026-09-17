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
                                || !new java.io.File("${project.build.sourceDirectory}").isDirectory()
                                || new java.io.File("${project.build.directory}/jacoco.exec").isFile()
                        ]]></condition>
                        <message>This module has production sources under src/main/java but produced no target/jacoco.exec, so jacoco:check skipped it and the 95% coverage gate did not run. Add tests, or set -Dcoverage.data.check.skip=true if this module is genuinely exempt.</message>
                    </evaluateBeanshell>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

No `<version>`: it resolves from the existing `pluginManagement` entry at line 573,
`${enforcer.plugin.version}` = `3.5.0`, which ships `evaluateBeanshell`.

Three properties are added to the root `<properties>` block (opens at line 75), beside
`enforcer.plugin.version` at line 133, so every placeholder above resolves on a plain build:

```xml
<skipTests>false</skipTests>
<maven.test.skip>false</maven.test.skip>
<coverage.data.check.skip>false</coverage.data.check.skip>
```

A command-line `-D` overrides a POM property, so `-DskipTests` and `-Dmaven.test.skip=true` still
work and still disable the guard along with the tests they skip.

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
selects exactly the right set. Verified across the whole reactor:

| Has `src/main/java` | Modules | Current `jacoco:check` behaviour |
|---|---|---|
| Yes | the 8 enforcing modules listed in §2 | enforces |
| No | `dsh` (root), `dsh-solr`, `dsh-doc-analyser`, `dsh-test-dataset`, `dsh-coverage-report` | skips |

The two partitions are identical. The guard changes nothing about today's build and only fires on a
module that is new or newly broken.

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

### 4.4 How the change is made

Per `CLAUDE.md`'s round-trip policy, a change this small goes to parent-poms directly rather than
through an issue and a release:

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
inherited `jacoco:check` — 95% LINE and BRANCH, per module, bound to `verify` — and the paragraph
states that `mvn -B install` runs both gates, with no second command to run. It must also say the
gate is inherited from parent-poms and will not be found by grepping this repository, which is the
specific failure `#93` exists to correct.

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
  `mvn -B -U install` is itself the gate, because `jacoco:check` is bound to `verify`.

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
parent-poms, failing the build at `verify`. Keep the "all critical paths" bullet.

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
  `check-coverage` or `coverage-baseline` returns hits only in `specs/stories/` and
  `docs/superpowers/`, which are historical records per §6.
- [ ] **AC005** — A deliberate drop below 95% on a module still fails `mvn -B install`. Verified by
  running it, with the failure output recorded — not assumed from reading the parent pom.
- [ ] **AC006** — parent-poms enforces that a module with `src/main/java` produced `jacoco.exec`,
  committed directly on its `master`, with the commit SHA referenced in issue `#93` and the change
  published via `deploy.yml` (`release_type: snapshots`). No release and no re-pin.
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

**2. AC005 — prove the inherited gate bites.** Delete a test method from `dsh-data`, rebuild that
module, confirm `jacoco:check` fails naming the module and the counter, then revert:

```bash
mvn -B -pl dsh-data -am install > .logs/mvn-ac005.log 2>&1
```

Expected: `Coverage checks have not been met` and `BUILD FAILURE`. Revert with `git checkout --` and
rebuild to confirm green. **The revert is part of the test, not cleanup** — an un-reverted edit
would ship a deliberately broken test.

**3. AC007 — prove the new guard bites.** No throwaway module is needed. Running a code-bearing
module with tests selected away produces exactly the state the guard is for: classes present, no
`jacoco.exec`, `jacoco:check` skipping.

```bash
mvn -B -pl dsh-data install -Dtest=none -DfailIfNoTests=false > .logs/mvn-ac007-guard.log 2>&1
```

Expected: `BUILD FAILURE` from `enforce-coverage-data-exists`, with the message from §4.1. Note
`-Dtest=none` does **not** set `skipTests`, which is why the guard stays armed — that distinction
is the whole test.

Then prove the escape hatch:

```bash
mvn -B -DskipTests install > .logs/mvn-ac007-skiptests.log 2>&1
```

Expected: `BUILD SUCCESS` across all 13 modules. `CLAUDE.md` documents this as a supported command,
so a guard that broke it would be a regression in this story, not an acceptable cost.

**4. Confirm the guard is inert on a normal build.** The full `mvn -B install` must stay green with
all 13 modules, and the log must show `enforce-coverage-data-exists` passing rather than being
skipped wholesale. Compare against the step 1 baseline.

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

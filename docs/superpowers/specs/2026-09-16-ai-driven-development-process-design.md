# Design: AI-Driven Development Process for DSH

| Field | Value |
| --- | --- |
| **Date** | 2026-09-16 |
| **Status** | Approved (brainstorm) |
| **Scope** | Repo initialisation for Claude Code + enforcement of a 6-step AI-driven development process |
| **Supersedes** | Nothing. Complements `.github/copilot-instructions.md`, which remains the standards source of truth. |

---

## 1. Problem

The DSH repository is already spec-driven and well documented for GitHub Copilot, but it has no
entry point for Claude Code and no encoded development process. The owner wants a repeatable
6-step loop — brainstorm to PRD, PRD task to GitHub story, story to detailed spec, spec to code
via TDD, code review, then CI-gated merge — built on the Superpowers skill set rather than prose
convention.

Four gaps block that loop today. They were confirmed by inspection, not assumed:

| # | Gap | Evidence |
| --- | --- | --- |
| G1 | No build/test workflow runs on a task-branch PR | `.github/workflows/api-testing.yml` is path-filtered to `dsh-rest-api/**` and `specs/api/**`; no other workflow builds on PRs. Step 6's "everything green" has nothing to be green. |
| G2 | No JaCoCo coverage threshold exists | No `check` goal, `minimum`, `limit` or `COVEREDRATIO` element in any pom. `dsh-coverage-report` aggregates and renders a badge only. |
| G3 | `.markdownlint.json` is referenced but absent | `spec-validation.yml` passes `--config .markdownlint.json`; the file does not exist. Masked by a trailing `\|\| true`. |
| G4 | No PRD exists | Step 1 of the process has no artifact to update. |

A fifth item is dead weight rather than a gap:

| # | Finding | Evidence |
|---|---|---|
| G5 | `install-parent-pom.sh` / `parent-pom.xml` are obsolete | Root pom inherits `com.mriss.mriss-parent:products:3.8.0-SNAPSHOT` from GitHub Packages. Local `parent-pom.xml` declares `com.mriss:mriss-parent:1.2.4` — a different artifact that **no module inherits from**. Only `build-ci*.sh` (Travis-era), `README.md:355`, `src/site/markdown/README.md:355` and `copilot-instructions.md:17` still reference it. |

---

## 2. Decisions

These were settled during the brainstorm and are not reopened by the implementation plan.

| ID | Decision | Rationale |
| --- | --- | --- |
| D1 | `CLAUDE.md` is a **thin router**. `.github/copilot-instructions.md` and `.github/copilot/rules/*` stay the source of truth for standards. | Zero duplication; one place to edit standards; Copilot and Claude cannot drift apart. |
| D2 | The process is enforced by **project skills that delegate to Superpowers**, not by hooks. | Claude gets a named, callable entry point per step; Superpowers remains the engine. Hooks were rejected as too many moving parts for the first iteration. |
| D3 | Coverage is a **ratchet**, not a fixed target, and is implemented **outside the poms**. | A fixed 80% against an untested legacy codebase produces a permanently red build. Keeping it out of the poms means zero risk to release builds that inherit from `products`. |
| D4 | Claude may **create** issues and PRs on approval; it **never closes issues and never merges**. | Those are the irreversible, outward-facing actions. Won't-fix closures ship as a reviewable script. |
| D5 | `ci.yml` resolves the parent from **GitHub Packages via `settings.xml`**; it never runs `install-parent-pom.sh`. Parent version upgrades stay manual. | Per repo owner: validation CI must not rebuild `parent-poms`. Pure inheritance from the published package. |
| D6 | Task branches are `issue-<n>-<slug>`, always cut from `DEVELOP`, an RC branch, or a hotfix branch. **Never from `master`.** | Extends the existing `issue-55` convention rather than replacing it. |

### 2.1 Open risk, deliberately not fixed here

The root pom pins `com.mriss.mriss-parent:products:` **`3.8.0-SNAPSHOT`**. A SNAPSHOT parent
re-resolves on Maven's daily refresh interval, so the same commit can build differently on two
different days — which defeats the manual, deliberate upgrade model D5 exists to support.
`api-testing.yml` compounds this by passing `-U`, forcing a re-resolve every run.

Mitigation in this design: `ci.yml` **omits `-U`**, limiting drift to Maven's daily refresh rather
than forcing a re-resolve every run.

**Updated 2026-09-16, per the repo owner:** pinning a released parent version is *not* scheduled
work and must not be recorded as a backlog task. Upcoming work on the `MRISS-Projects/parent-poms`
project will change this repository, and tracking a `SNAPSHOT` is how those changes reach it
without cutting a parent release per iteration. The reproducibility cost is therefore an **accepted
trade-off with a named mitigation**, not an oversight. It is worth revisiting only once the
`parent-poms` work has settled. A backlog entry here would actively mislead: someone could pick it
up and break the incoming integration.

---

## 3. Architecture

### 3.1 Document topology

```text
CLAUDE.md  ──────────────── thin router, ~120 lines
  │
  ├─→ docs/process/ai-driven-development.md   the 6 steps, mapped to skills
  ├─→ docs/devops/README.md                   branching + CI/CD + release, with diagrams
  ├─→ specs/product/PRD.md                    waves and their stories
  │
  └─→ .github/copilot-instructions.md         STANDARDS SOURCE OF TRUTH (unchanged in substance)
        ├─→ .github/copilot/rules/java-conventions.md
        ├─→ .github/copilot/rules/api-standards.md
        ├─→ .github/copilot/rules/testing-patterns.md
        ├─→ .github/roles.md
        └─→ specs/**
```

`CLAUDE.md` restates **no** standard. It states identity, commands, branch rules, gates, and the
process contract, then delegates.

### 3.2 The six steps and their skills

| Step | Action | Skill | Artifact | Hard stop |
| --- | --- | --- | --- | --- |
| 1 | Brainstorm a path forward | `dsh-plan-wave` → `superpowers:brainstorming` | `specs/product/PRD.md` | Owner approves before write |
| 2 | Task becomes an INVEST story | `dsh-new-story` | GitHub issue | Owner approves body before `gh issue create` |
| 3 | Story becomes a detailed spec | `dsh-story-spec` → `superpowers:brainstorming` + `writing-plans` | `specs/stories/<n>-<slug>.md` | Refuses `master` as parent; owner approves spec before commit/push |
| 4 | Spec becomes code, TDD | `dsh-build-story` → `superpowers:test-driven-development` + `executing-plans` | Code + tests | Red must fail for the right reason before green |
| 5 | Code review | `dsh-ship-story` → `/code-review`, `superpowers:receiving-code-review` | Review findings | Owner reads findings before fixes land |
| 6 | Commit, push, CI, merge | `dsh-ship-story` → `verification-before-completion` + `finishing-a-development-branch` | PR, green CI | **Never merges.** Stops at green. |

Steps 5 and 6 share one skill because they are a continuous flow, but the review gate between
them is an explicit stop, not a formality.

### 3.3 Story spec front matter

`dsh-story-spec` writes machine-readable front matter so later steps need not guess:

```yaml
---
issue: 91
slug: extract-document-persistence-repository
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---
```

`dsh-ship-story` reads `parent_branch` to target the PR. This is the single piece of state that
couples the steps; everything else is derived from the issue or the working tree.

---

## 4. Components

### 4.1 CLAUDE.md

Sections, in order: identity paragraph; module map table; build and test commands; branch rules;
quality gates; the 6-step contract with the skill per step; a "where things live" delegation table.

Build commands must reflect D5 — `mvn -B install`, no `install-parent-pom.sh`.

**These commands must be run before they are written down.** D5 assumes the parent resolves from
GitHub Packages using the credentials already in the owner's `~/.m2/settings.xml`; that assumption
is untested on this machine. If `mvn -B install` cannot resolve
`com.mriss.mriss-parent:products:3.8.0-SNAPSHOT` locally, that is a finding to report, not a
command to document.

### 4.2 Project skills

Five skills under `.claude/skills/`, each a thin wrapper. None reimplements what a Superpowers
skill already does; each one's job is to supply DSH-specific context (branch rules, spec paths,
gate commands, issue templates) and then hand off.

`dsh-story-spec` carries the one piece of real logic: it resolves the parent branch, and **refuses
to proceed if the parent is `master`**.

### 4.3 The PRD

`specs/product/PRD.md`. Wave structure, with the existing open issues triaged into it:

| Wave | Theme | Issues |
| --- | --- | --- |
| 0 | Engineering foundation | #85, #86, #87 (Maven/Java alignment), #43 (site reports), #46 (embedded-tomcat integration tests), #70, #90 (gh-pages), plus: deprecate `install-parent-pom.sh`, pin a released parent |
| 1 | ADR-001 Phase 1 — interface extraction and deprecation | #48 (dev/staging/prod profiles — the migration is profile-selected, `gcp` vs `legacy`) |
| 2 | ADR-001 Phase 2 — Firestore + GCS | — |
| 3 | ADR-001 Phase 3 — Cloud Pub/Sub | #49, re-scoped from "docker containers for the servers" to **GCP emulators** (Firestore, Pub/Sub) for local and CI testing |
| 4 | ADR-001 Phase 4 — Vertex AI Search | — |
| 5 | ADR-001 Phase 5 — validation and cutover | — |
| 6 | Product backlog | #44, #45, #50, #51, #52, #53 |

**Won't-fix, recorded inside the migration section:**

| Issue | Reason | Superseded by |
| --- | --- | --- |
| #65 — Implement indexer-worker daemon | Body specifies RabbitMQ enqueue and Solr storage. | Waves 3 and 4 |
| #47 — Mongo DAO ordering by timestamp | Targets `MongoDocumentDao`, which ADR-001 proposes replacing. | Wave 2 |

Issue #52 was reviewed and **kept**: it mentions Mongo only as one option for automatic
file-hash generation; the file-hash-as-a-service idea survives the migration intact.

Closures ship as a reviewable `gh issue close` script under `scripts/`. Per D4, it is not run.

Milestones `0.3.0-SNAPSHOT`, `0.4.0-SNAPSHOT` and `1.0.0-SNAPSHOT` already exist and map onto
the waves.

### 4.4 The DevOps README

`docs/devops/README.md`. Two Mermaid diagrams: a `gitGraph` of the branching and release model
(DEVELOP → stage → RC → release → master, plus the hotfix line), and a flowchart of which workflow
fires on what — including the four release workflows delegating into `MRISS-Projects/parent-poms`
reusable workflows. The main `README.md` is not touched; it describes the system, not the pipeline.

### 4.5 The CI workflow

`.github/workflows/ci.yml`.

| Aspect | Detail |
| --- | --- |
| Triggers | All pull requests (unfiltered); pushes to `DEVELOP`, `staging-*-RC`, `*.x`. Task branches are gated by the `pull_request` trigger, deliberately **not** listed under `push` — listing both would run the full build twice for every task-branch PR. |
| Runtime | `ubuntu-latest`, JDK 17 Temurin, Maven cache |
| Services | `mongo:6`, `rabbitmq:3-management`, plus the Mongo user bootstrap lifted from `api-testing.yml` |
| Auth | `~/.m2/settings.xml` for GitHub Packages via `DEPLOY_TOKEN`, same shape as `api-testing.yml` |
| Build | `mvn -B install` — **tests on**, **no `-U`** (see §2.1), **no `install-parent-pom.sh`** (D5) |
| Artifacts | Surefire/Failsafe reports, JaCoCo aggregate report |

**Coverage ratchet (D3):** a step parses
`dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv`, computes the aggregate covered
ratio, and compares it against a committed `.github/coverage-baseline.txt`. Below baseline fails
the job. A push to `DEVELOP` updates the baseline. No pom is modified.

### 4.6 Smaller changes

| File | Change |
| --- | --- |
| `.markdownlint.json` | Create it (G3). Rules chosen to pass on existing content, so the gate starts green. |
| `.github/workflows/spec-validation.yml` | Drop the trailing `\|\| true` on the markdownlint step. Today the lint gate cannot fail, so it is decoration. Removing it is only safe *because* the new config is tuned to pass on existing content — so this change lands in the same commit as `.markdownlint.json`, never before it. |
| `.github/ISSUE_TEMPLATE/story.md` | New INVEST story template for step 2. |
| `.github/copilot-instructions.md` | Add rows for `CLAUDE.md` and the process doc; fix line 17, which currently directs agents to `parent-pom.xml` (G5). |
| `specs/architecture/system-design.md` | Mark the MongoDB / Solr / RabbitMQ boxes as "replacement proposed" and link ADR-001 (its status is Proposed; nothing is deprecated yet). Stack table still says "Java 11+" and "Travis CI"; correct to Java 17 and GitHub Actions. |
| `install-parent-pom.sh`, `parent-pom.xml` | Add deprecation headers. **Not deleted** — `build-ci*.sh` still reference them. Removal is a Wave 0 story. |

---

## 5. Testing

This change set is documentation, configuration and skills; it has no unit-testable surface. It is
verified by execution, not by assertion:

| What | How |
| --- | --- |
| `ci.yml` is correct | Push the branch and observe the run go green. A workflow that has never run is not verified. |
| Coverage ratchet is correct | Confirm the parsed ratio matches the JaCoCo badge, and that a deliberately lowered baseline passes while a raised one fails. |
| `.markdownlint.json` is correct | `markdownlint` over `specs/**`, `.github/**`, `docs/**` **without** the `\|\| true` fallback must exit 0. |
| Skills load | Each of the five appears in the skill listing and its frontmatter parses. |
| Links resolve | Every relative link in the new documents points at a file that exists. |

The ratchet and the markdownlint config are the two items with real failure modes; both are
checked by running them, not by reading them.

---

## 6. Consequences

**Positive.** Step 6 gains a real gate for the first time. Standards live in exactly one place.
The PRD turns ADR-001's five phases into schedulable work with issues already triaged against it.
Dead build machinery is labelled as dead.

**Negative.** Five project skills are five more files to keep true as the process evolves; a skill
that lies is worse than no skill. The coverage baseline file must be committed and will conflict
on concurrent branches. `ci.yml` runs the full multi-module build with integration tests on every
PR, which is slower than the current no-op.

**Deferred.** Branch protection is documented, not applied. The SNAPSHOT parent pin (§2.1) is
recorded, not fixed. Enforcement hooks (D2) are rejected for now, revisitable once the process has
run for a few stories.

---

## 7. References

- `specs/architecture/ADR-001-GCP-based-components.md` — the target architecture and its 5 phases
- `.github/copilot-instructions.md` — standards source of truth (D1)
- `.github/roles.md` — role definitions
- `.github/workflows/api-testing.yml` — the setup `ci.yml` is modelled on

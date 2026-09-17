# CLAUDE.md

Document Smart Highlights (DSH) is a Java 17 / Spring Boot multi-module Maven system that
analyses documents and produces smart highlights. It currently runs on self-managed
MongoDB / RabbitMQ / Solr. A migration to managed GCP services (Firestore, Pub/Sub, Vertex AI
Search) is **proposed** in `specs/architecture/ADR-001-GCP-based-components.md` — status
Proposed, no implementation work has started yet.

## This file is a router

Coding standards live in `.github/copilot-instructions.md` and `.github/copilot/rules/`.
**Do not duplicate them here.** This file covers only what is specific to working as an
agent in this repo: commands, branch rules, gates, and the development process.

| I need... | Read |
|---|---|
| Coding standards, module guidelines, code-gen preferences | `.github/copilot-instructions.md` |
| Java conventions | `.github/copilot/rules/java-conventions.md` |
| API standards | `.github/copilot/rules/api-standards.md` |
| Testing patterns | `.github/copilot/rules/testing-patterns.md` |
| Who owns what | `.github/roles.md` |
| Target architecture and migration phases | `specs/architecture/ADR-001-GCP-based-components.md` |
| Current architecture | `specs/architecture/system-design.md` |
| The development process, in full | `docs/process/ai-driven-development.md` |
| Branching, CI/CD and release pipeline | `docs/devops/README.md` |
| What we are building next | `specs/product/PRD.md` |

## Modules

| Module | Responsibility |
|---|---|
| `dsh-rest-api` | Public REST API, Spring Boot |
| `dsh-doc-analyser` | Analysis engine; sub-modules for keyword extraction, top-sentence extraction, and doc processing (`dsh-doc-processor-worker`) |
| `dsh-doc-indexer-worker` | Async indexing worker |
| `dsh-data` | Shared models and persistence |
| `dsh-solr` | Solr integration and custom plugins (proposed for replacement — see ADR-001) |
| `dsh-test-dataset` | PDF/HTML fixtures used by tests |
| `dsh-coverage-report` | Aggregates JaCoCo coverage across modules |

## Commands

The parent POM `com.mriss.mriss-parent:products` resolves from GitHub Packages via your
`~/.m2/settings.xml`.

| Task | Command |
|---|---|
| Full build with tests | `mvn -B install` |
| Fast build, no tests | `mvn -B -DskipTests install` |
| Single module | `mvn -B -pl dsh-data -am install` |
| Coverage gate (after a full build) | `./scripts/check-coverage.sh` |
| Markdown lint | `markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json` |

### Always log local Maven runs

This is a 13-module reactor and a full build is slow. **Never run `mvn` locally as a silent
blocking command.** Redirect to `.logs/` and print a `tail` command first, so progress is
watchable:

```bash
mkdir -p .logs
mvn -B install > .logs/mvn-install.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-install.log"
wait $MVN_PID; echo "maven exit=$?"
```

Name the log after the command (`.logs/mvn-install.log`, `.logs/mvn-validate.log`). Report the
exit code explicitly — a backgrounded `mvn` without `wait` reports success no matter what.
Never pipe `mvn` directly into `tail`; you lose the diagnostics and `$?` becomes the pipe's
status. `.logs/` is gitignored; never commit a build log. In `ci.yml` do **not** redirect —
GitHub Actions already captures the output.

Upgrading the parent version is a deliberate, manual edit to the root `pom.xml`. CI never
rebuilds `parent-poms`, and every CI Maven invocation passes `-U` — while the parent is a
`SNAPSHOT`, tracking the current one on every run is the intended contract. See
`docs/devops/README.md`, "Parent POM", for why, and for when to drop the flag.

## Branch rules

    master                      release automation only - NEVER branch from it
    DEVELOP                     mainline integration          \
    staging-X.Y.Z-SNAPSHOT-RC   release candidate              }- legal task-branch parents
    X.Y.x                       hotfix line                   /
    issue-<n>-<slug>            task branch

A task branch is always cut from `DEVELOP`, an RC branch, or a hotfix branch, and merges
back into the branch it came from. **Never branch from `master`. Never open a PR into
`master`** — the release workflow puts code there.

## Quality gates

A story is not done until both pass:

1. All tests pass under `mvn -B install` (surefire only — there is no failsafe configuration and
   no `*IT.java` test in the repo today, so "integration tests" are not yet a separately enforced
   gate; implementing them is tracked as `#46` in PRD Wave 0).
2. Aggregate instruction coverage has not dropped below `.github/coverage-baseline.txt`.

`.github/workflows/ci.yml` enforces both on every PR. Run `mvn -B install` then
`./scripts/check-coverage.sh` to check locally before pushing.

## The development process

Eight steps. Full detail in `docs/process/ai-driven-development.md`.

| Step | Do this | Skill |
|---|---|---|
| 1 | Brainstorm, then update the PRD with waves | `dsh-plan-wave` |
| 2 | Turn a PRD task into an INVEST story on GitHub | `dsh-new-story` |
| 3 | Turn the story into a reviewed spec on the task branch | `dsh-story-spec` |
| 4 | Build the spec with TDD — red first, then green | `dsh-build-story` |
| 5 | Local code review | `dsh-ship-story` |
| 6 | Commit, push, open the PR | `dsh-ship-story` |
| 7 | PR review cycle — CI and reviewer findings, triaged, fixed or answered, until green with every thread resolved | `dsh-pr-cycle` |
| 8 | After you merge and close the issue — reconcile the PRD against GitHub | `dsh-reconcile-prd` |

Step 7 repeats. Each round is: read CI and the review comments, **triage them** (a finding may be
stale, or right for the wrong reason, or propose a remedy that does not work), then one commit per
fix and one push per round. It ends when every check is green *and* every review thread is
resolved — not at green alone.

`dsh-pr-cycle` is invocable on its own. A review round often lands days after the PR opened; you do
not need to re-run steps 5 and 6 to handle it.

Step 8 starts where your merge ends. Once the PR is merged and the issue closed,
`specs/product/PRD.md` is stale in two ways: the issue it tracked is still listed as open, and
any issue the story spun off along the way is not listed at all. `dsh-reconcile-prd` reconciles
the document against GitHub — status, placement by milestone, drifted titles — and is likewise
invocable on its own.

**Two things Claude never does:** close a GitHub issue, or merge a pull request. Both are
yours. Claude creates issues and PRs only after you approve the content.

## Shared build infrastructure lives in another repo

DSH inherits from `com.mriss.mriss-parent:products`, maintained in
[`MRISS-Projects/parent-poms`](https://github.com/MRISS-Projects/parent-poms). That repo owns the
build and release machinery: plugin versions and configuration, the coverage gate, the reusable
release/stage/staging/hotfix workflows, and Maven site generation.

**A lot of what looks like a DSH problem is actually a parent-poms problem.** Before changing build
configuration here, check whether it belongs there. Symptoms that usually mean *there, not here*:
Maven site generation, gh-pages publishing, plugin versions, release workflow behaviour, coverage
thresholds.

Example already in place: the 95% coverage gate is **not** in this repo. `jacoco:check`
(`element=BUNDLE`, LINE and BRANCH ≥ 0.95, bound to `verify`) is inherited from parent-poms and runs
on every module here. Grepping only this repo's poms will tell you it does not exist. It does.

### The round trip for a shared change

1. Open an issue in `parent-poms`. Plain issue — INVEST framing is not required there; it is
   infrastructure, not product.
2. Make sure parent-poms' next milestone is open as a `-SNAPSHOT`.
3. Implement and test it there, against that `-SNAPSHOT`.
4. Point this repo's root `pom.xml` at that `-SNAPSHOT` temporarily to validate end to end.
5. Close the issue and release parent-poms — its release script already exists.
6. Re-pin this repo's root `pom.xml` to the newly released version.

**Before releasing parent-poms, clear the milestone being released.** If it still has open issues,
fix those first. A release that leaves its own milestone half-done makes the version meaningless.

**Keep parent-poms' Claude setup lightweight.** It has a deliberately minimal `CLAUDE.md` and none
of this repo's eight-step process. It is infrastructure. Do not port this process there.

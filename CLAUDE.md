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
`~/.m2/settings.xml`. **Do not run `install-parent-pom.sh`** — it installs
`com.mriss:mriss-parent:1.2.4`, a legacy artifact no module inherits from.

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
rebuilds `parent-poms` and never passes `-U`.

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

Six steps. Full detail in `docs/process/ai-driven-development.md`.

| Step | Do this | Skill |
|---|---|---|
| 1 | Brainstorm, then update the PRD with waves | `dsh-plan-wave` |
| 2 | Turn a PRD task into an INVEST story on GitHub | `dsh-new-story` |
| 3 | Turn the story into a reviewed spec on the task branch | `dsh-story-spec` |
| 4 | Build the spec with TDD — red first, then green | `dsh-build-story` |
| 5 | Code review | `dsh-ship-story` |
| 6 | Commit, push, CI green, then merge | `dsh-ship-story` |

**Two things Claude never does:** close a GitHub issue, or merge a pull request. Both are
yours. Claude creates issues and PRs only after you approve the content.

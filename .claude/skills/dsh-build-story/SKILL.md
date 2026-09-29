---
name: dsh-build-story
description: Use when implementing a DSH story from its spec using TDD - step 4 of the DSH development process
---

# DSH: Build Story

Step 4 of the process in `docs/process/ai-driven-development.md`.

**Invoke `superpowers:test-driven-development`.** It owns the red/green discipline. For a
spec with many independent tasks, `superpowers:subagent-driven-development` runs them with
a fresh agent per task.

## DSH specifics

- Conventions are in `.github/copilot/rules/java-conventions.md`; test patterns are in
  `.github/copilot/rules/testing-patterns.md`. Follow them rather than inventing a style.
- ADR-001 requires **interface-first** work: new GCP implementations go behind an existing
  interface, selected by Spring profile (`gcp` vs `legacy`). Never edit a deprecated
  implementation in place - add alongside it.
- Deprecate, do not delete. Annotate with `@Deprecated` plus a Javadoc `@deprecated` tag
  naming the replacement.

## The gate before you claim done

Log the build and give the human something to watch - see "Always log local Maven runs" in
`CLAUDE.md`:

    mkdir -p .logs
    mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
    MVN_PID=$!
    echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
    wait $MVN_PID; echo "maven exit=$?"

That one command is the whole of gates 1 and 2. Every module with production sources holds at least 95% LINE
and 95% BRANCH coverage, enforced by `jacoco:check`, and a module that produced no coverage data at
all fails `enforce-coverage-data-exists` - the companion guard that exists because `jacoco:check`
silently skips a module with no exec data. Both are bound to `verify` and inherited from
`parent-poms`, so the build fails by itself - there is no second command to run, and nothing to
find by grepping this repository. All tests pass. A red build is not "done with a known issue".

Gate 3 in CLAUDE.md's *Quality gates* is conditional:
when the story touches an external system or a REST API entry point, also run
`mvn -B clean verify -DintegrationTests -pl <changed modules> -amd`, logged the same way, after it.
A red gate-3 run sent back here from `dsh-ship-story` is fixed here and re-run here.

If the coverage gate fails, add tests. Weakening the gate to make it pass is falsifying it - if a
drop is genuinely justified, say so out loud and let the human decide.

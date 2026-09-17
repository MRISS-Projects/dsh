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
    mvn -B install > .logs/mvn-install.log 2>&1 &
    MVN_PID=$!
    echo "Monitor with:  tail -f .logs/mvn-install.log"
    wait $MVN_PID; echo "maven exit=$?"

That one command is the whole gate. Every module with production sources holds at least 95% LINE
and 95% BRANCH coverage, enforced by `jacoco:check` bound to `verify` and inherited from
`parent-poms`, so the build fails on a shortfall by itself - there is no second command to run.
All tests pass. A red build is not "done with a known issue".

If the coverage gate fails, add tests. Weakening the gate to make it pass is falsifying it - if a
drop is genuinely justified, say so out loud and let the human decide.

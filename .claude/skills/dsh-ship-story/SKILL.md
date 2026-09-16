---
name: dsh-ship-story
description: Use when a DSH story is built and needs local code review, then a push and a pull request - steps 5 and 6 of the DSH development process
---

# DSH: Ship Story

Steps 5 and 6 of the process in `docs/process/ai-driven-development.md`. The review rounds that
follow on the open pull request are step 7 — see `dsh-pr-cycle`.

## Step 5 - review

1. Invoke `superpowers:requesting-code-review`, or run `/code-review` for the diff.
2. **Stop. The human reads the findings.**
3. For the findings you act on, invoke `superpowers:receiving-code-review` - verify each
   claim against the code rather than agreeing on reflex.
4. Fixes go back through `dsh-build-story` (TDD still applies to review fixes).

## Step 6 - verify, push, PR

Invoke `superpowers:verification-before-completion` first. Evidence before assertions:

    mkdir -p .logs
    mvn -B install > .logs/mvn-install.log 2>&1 &
    MVN_PID=$!
    echo "Monitor with:  tail -f .logs/mvn-install.log"
    wait $MVN_PID; echo "maven exit=$?"

    ./scripts/check-coverage.sh

Read `parent_branch` from the front matter of `specs/stories/<n>-<slug>.md`.

## Hard stop

Show the human the PR title, the PR body, and the resolved base branch. Wait for approval.
**Then** run:

    git push -u origin issue-<n>-<slug>
    gh pr create --base <parent_branch> --title "<title>" --body "Closes #<n>" --fill
    gh run watch

**`--base` is never `master`.** If the front matter says `master`, something went wrong
upstream in step 3 - stop and raise it.

## Where this skill stops

The pull request is open and the first CI run has been started. That is the end.

Do **not** carry on into the review rounds from here. CI results and reviewer comments — from
GitHub Copilot or from a person — are **step 7**, and they belong to `dsh-pr-cycle`, which is
invocable on its own because a round often arrives hours or days later in a fresh session.

**Do not merge the PR. Do not close the issue.** Report the PR URL and the CI result, and hand it
to the human. See `superpowers:finishing-a-development-branch` for what integration options look
like, but the decision and the action are theirs.

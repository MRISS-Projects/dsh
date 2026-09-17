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

That one command is the whole gate. `jacoco:check` enforces 95% LINE and BRANCH per module and
`enforce-coverage-data-exists` rejects a module that produced no coverage data at all; both are
bound to `verify` and inherited from `parent-poms`, so there is no second command to run and
nothing to find by grepping this repository.

Read `parent_branch` from the front matter of `specs/stories/<n>-<slug>.md`.

## Hard stop

Show the human the PR title, the PR body, the resolved base branch, and which review effort level
this PR wants ("Ask for the right review effort", below). Wait for approval.
**Then** run:

    git push -u origin issue-<n>-<slug>
    gh pr create --base <parent_branch> --title "<title>" --body "Refs #<n>" --fill
    gh run watch

**`--base` is never `master`.** If the front matter says `master`, something went wrong
upstream in step 3 - stop and raise it.

**`Refs`, not `Closes` — and do not "fix" this back.** GitHub only auto-closes a linked issue
when the pull request merges into the repository's **default branch**. Here that is `master`,
and a story PR never targets it: it targets `DEVELOP`, an RC branch, or a hotfix line. So
`Closes #<n>` would silently do nothing on every story PR, while reading as though the issue
were handled. Verified on `#98` - merged into `staging-0.3.0-SNAPSHOT-RC` with `Closes #95` in
the body, and `#95` stayed open.

Closing the issue stays the human's call, which is what the hard stop below already requires.
The issue's real close point is when the release merges the RC into `master`.

## Ask for the right review effort

Copilot code review runs at a selectable **effort level**, chosen **per pull request** under
**Reviewers** at the moment the review is requested. It is not a repository setting, there is no
workflow to change, and **Claude cannot select it** — it is a human action on the PR page.

- **`Lite`** — cost-efficient and targeted.
- **`Balanced`** — the level to request for a **substantive** pull request.

So when you hand the PR over, say which one this PR wants. A review is only as good as the facts
it applies; on a substantive change, buying the higher effort level is cheaper than a round spent
answering a finding that the repository already contradicts.

## Where this skill stops

The pull request is open and the first CI run has been started. That is the end.

Do **not** carry on into the review rounds from here. CI results and reviewer comments — from
GitHub Copilot or from a person — are **step 7**, and they belong to `dsh-pr-cycle`, which is
invocable on its own because a round often arrives hours or days later in a fresh session.

**Do not merge the PR. Do not close the issue.** Report the PR URL and the CI result, and hand it
to the human. See `superpowers:finishing-a-development-branch` for what integration options look
like, but the decision and the action are theirs.

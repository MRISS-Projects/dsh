---
name: dsh-reconcile-prd
description: Use after a DSH pull request is merged and its issue closed, to reconcile specs/product/PRD.md against GitHub - issue status, issues created mid-session placed into the right wave - step 8 of the DSH development process
---

# DSH: Reconcile the PRD

Step 8 of the process in `docs/process/ai-driven-development.md`.

Invocable on its own. Step 7 stops at green; the human merges the pull request and closes the
issue. This step runs **after** that, and often in a later session — all you need is the repo.

**GitHub is the ground truth. The PRD is the thing that drifted.** Every edit here moves the PRD
towards GitHub, never the reverse.

## Input

At least one of:

- a merged pull request whose issue the human has closed,
- issues created during a session that are not yet in a wave,
- a suspicion that the PRD has gone stale.

## 1. Ask GitHub, not your memory of the session

    gh issue list --state all --limit 100 \
      --json number,title,state,milestone \
      --jq '.[] | "\(.number)\t\(.state)\t\(.milestone.title // "-")\t\(.title)"'

    gh pr list --state merged --limit 20 \
      --json number,title,closingIssuesReferences \
      --jq '.[] | "PR #\(.number)\t\(.title)\t closes: \([.closingIssuesReferences[].number] | @csv)"'

Story PR bodies use `Refs`, not `Closes`, so `closingIssuesReferences` is often empty. Fall back to
the branch name (`issue-<n>-<slug>`) and the commit subjects (`type(#n): ...`) to map a PR to its
issue.

## 2. Diff GitHub against the PRD

Read `specs/product/PRD.md`. For every issue GitHub returned, check four things:

| Check | Fix in the PRD |
|---|---|
| Is it in the PRD at all? | Missing issue → add it to the wave its milestone maps to (§3 of the PRD). |
| Does `Status` match GitHub? | Closed on GitHub → `**closed** — PR #<n>`. Still open → `open`. |
| Does the title match? | Retitled on GitHub → replace. Issues get rewritten; `#93` was. |
| Does the surrounding prose still hold? | A paragraph that explains an issue's rationale can go stale when the issue is rewritten or superseded. Fix the prose, do not just fix the row. |

Two directions to walk, not one. Issues absent from the PRD are the easy half; PRD rows naming an
issue that no longer exists, or that was closed as `not planned`, are the other half and belong in
the won't-fix table with a reason.

Milestone maps to wave via the PRD's §3 table. An issue with **no** milestone cannot be placed —
say so and ask the human whether to set one, rather than guessing a wave.

## 3. What the PRD records, and what it does not

The issue stays the single source of truth for rationale and acceptance criteria. The PRD carries
three things per issue and nothing more: **number, status, title**. Titles are GitHub's, normalised
to sentence case with any `[STORY]` prefix dropped.

When an issue was spun off from another during a story, add one short paragraph saying where it
came from and why it was not folded into its parent. That is provenance, not restated content —
it is the thing GitHub cannot tell the next reader.

## 4. Verify and commit

    PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" \
      markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' \
      --ignore 'docs/wiki/**' --config .markdownlint.json

Docs-only changes do not need `mvn -B install`. If the reconciliation touched anything outside
markdown, it is no longer a reconciliation — it is a story, and it goes back to step 2.

## Hard stops

- **Never close an issue and never merge a pull request.** This step exists *because* the human
  already did both.
- **The human approves the PRD diff before it is committed.** Show it, then wait.
- Do not invent a wave placement for an issue with no milestone. Ask.
- Do not restate an issue's acceptance criteria in the PRD, however convenient. That is exactly the
  drift this document's §2 forbids.

## Where this skill stops

Report what changed: issues whose status flipped, issues added and to which wave, titles corrected,
prose rewritten, and anything you could not place. Then hand back — the next story starts at
step 2.

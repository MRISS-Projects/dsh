---
name: dsh-pr-cycle
description: Use when a DSH pull request is open and needs a review round - CI results, Copilot or human review comments, triage, fixes and replies until green with all threads resolved - step 7 of the DSH development process
---

# DSH: PR Review Cycle

Step 7 of the process in `docs/process/ai-driven-development.md`.

Invocable on its own. A review round can arrive hours or days after the PR opened, in a fresh
session — you do not need to have run `dsh-ship-story` in this session to run this. All you need
is the PR number.

## Input

An open pull request, and at least one of: a failed CI check, or unresolved review threads.

## 1. Gather the actual state

Do not work from what the PR page looked like earlier. Ask GitHub:

    gh pr view <n> --json url,state,mergeable,statusCheckRollup
    gh api repos/<owner>/<repo>/pulls/<n>/comments --paginate \
      --jq '.[] | "ID: \(.id)\nFILE: \(.path):\(.line // .original_line)\n\(.body)\n"'

For thread resolution state, the REST API does not expose it — use GraphQL:

    gh api graphql -f query='query($o:String!,$r:String!,$n:Int!){repository(owner:$o,name:$r){
      pullRequest(number:$n){reviewThreads(first:50){nodes{id isResolved
      comments(first:1){nodes{databaseId path}}}}}}}' \
      -F o=<owner> -F r=<repo> -F n=<n>

## 2. Triage every finding before changing anything

**This is the part that matters.** A review comment is a claim, not an instruction. Invoke
`superpowers:receiving-code-review` — it owns the discipline of verifying rather than complying.

Check each finding against three things, in this order:

| Check | Why |
|---|---|
| **Is it still true at current HEAD?** | Reviews are pinned to the commit they ran on. Later commits on the branch may already have fixed it. A stale finding needs a reply, not a patch. |
| **Is the stated mechanism right, or only the conclusion?** | A finding can reach the right verdict by the wrong reasoning. If you fix what it *says* rather than what is *wrong*, you fix nothing. |
| **Would the proposed remedy actually work?** | Reviewers suggest fixes without running them. Verify the suggestion before adopting it; propose a better one when it does not hold. |

Reproduce the claim where you can. If a finding says a script mishandles an input, feed the script
that input and watch what happens. Evidence beats argument in the reply either way.

Classify each finding as **valid** (fix it), **stale** (already fixed — reply and resolve), or
**incorrect** (reply with your evidence; do not change code to satisfy a wrong claim).

Show the human your triage — finding, verdict, rationale, intended fix — before you act on it.

## 3. Fix and reply

- **One commit per fix.** Each commit message names the finding and what was actually wrong.
  Reviewable and revertible one at a time.
- **Reply to every finding**, including the ones you fix. The reply says what you did and why, and
  names the commit. Where you disagreed, say so plainly and show the evidence.
- A fix that changes behaviour needs a test that would have caught the original defect — TDD still
  applies, so go through `dsh-build-story`.

## 4. One push per round

Run the local gate first, then push once:

    mkdir -p .logs
    mvn -B install > .logs/mvn-install.log 2>&1 &
    MVN_PID=$!
    echo "Monitor with:  tail -f .logs/mvn-install.log"
    wait $MVN_PID; echo "maven exit=$?"

Batching the round into one push keeps CI runs proportional to review rounds rather than to
individual fixes.

## 5. Re-verify, then loop or stop

    gh pr view <n> --json statusCheckRollup
    gh run watch <run-id> --exit-status

A new round starts if CI is red or new comments arrived. Otherwise you are done when **both** hold:

1. every check is green, and
2. every review thread is resolved.

## Hard stops

- **Never merge the pull request.** Green CI is where this skill ends; merging is the human's.
- **Never resolve a thread whose resolution the human has not seen.** Resolving hides the
  conversation. Resolve stale findings you have replied to; for findings you *fixed*, leave them
  open so the human can check the fix, unless they tell you otherwise.
- **Never close an issue** as part of a review round.
- Do not weaken a gate to make a check pass. A red build caused by a real defect is information.

## Where this skill stops

Report the PR URL, the check status, a line per finding with its verdict and commit, and anything
still open. Then hand back.

After the human merges and closes the issue, step 8 (`dsh-reconcile-prd`) reconciles
`specs/product/PRD.md` against GitHub — including any issue this review round spun off. That is a
separate invocation, not something to run ahead of the merge.

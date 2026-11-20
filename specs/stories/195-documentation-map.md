---
issue: 195
slug: documentation-map
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 195 — Documentation map: every Markdown document under docs/, specs/ and .github/ is reachable from CLAUDE.md, checked in CI

## 1. Story

**As a** developer or agent opening this repository with `CLAUDE.md` as the way in
**I want** every Markdown document under `docs/`, `specs/` and `.github/` to be reachable from
`CLAUDE.md` by following references, enforced by CI, and the empty scaffolding that no one reads
deleted
**So that** the documentation tree has one root, no document is stranded where nothing points at
it, and nothing I land on is a placeholder for content that was never written

## 2. Context

- Wave: 1, task 4 (`specs/product/PRD.md` §4, Wave 1), step 8 of the wave's order of work. DSH
  milestone `0.4.0-SNAPSHOT`.
- Issue: [#195](https://github.com/MRISS-Projects/dsh/issues/195). Its one input, task 5, `#44`,
  is closed: the wiki's source is `docs/wiki/`, so the wiki is part of the tree this check covers.
- Parent branch: `DEVELOP`, at `72c589cff` when this spec was written. The task branch is
  `issue-195-documentation-map`.
- Precedent: `.github/scripts/check-spec-references.sh` and its test script, from `#94`
  (`specs/stories/94-enforce-spec-references.md`), run by `.github/workflows/spec-validation.yml`.

### 2.1 What exists today, measured

Measured on 2026-11-17 and 2026-11-19 on tracked files at `72c589cff`, with a throwaway walker
first and then with the checker of §6, Task 1, both kept out of the repository.

- **107 tracked Markdown files** are under `docs/`, `specs/` and `.github/`.
- **`CLAUDE.md` holds no Markdown link into the tree.** Its one link goes to parent-poms on
  GitHub. Every pointer it gives is a path in a backtick span. Counted by Markdown links alone,
  none of the 107 is reachable, and adding backtick paths changes the answer completely.
- **How a directory reference counts decides everything else.** Counting Markdown links and
  backtick paths, the files left unreached are:

  | A path to a directory reaches | Unreached |
  |---|---|
  | nothing | 43 |
  | its `README.md` only | 35 |
  | the `.md` files directly inside it | 2 |
  | every `.md` file under it, at any depth | 0 |

  The 2 under the third rule are the two files of `specs/requirements/functional/`. With the 8
  scaffolding files of the issue deleted, the third rule leaves 0 of 99 unreached.
- **Three reaches are accidental.** `specs/stories/` is reached only because `#177`'s spec names
  the directory, in a list of historical records. `.github/ISSUE_TEMPLATE/` is reached only through
  the two September documents in `docs/superpowers/`. `docs/architecture/README.md` is reached
  only through those documents too, or, once the story specs are reached, through `#113`'s spec.
  With all three removed in a scratch export, 32 documents go unreached.
- **`docs/architecture/README.md` is scaffolding too.** Its table reads "*(to be added)*", and
  it says that `documentation-sync.yml` generates the directory from `specs/architecture/`. That
  workflow writes `docs/api/` only. No current document names the file.
- **The scaffolding is referenced from 11 current files**, listed in §3.4.
  `.github/copilot-instructions.md` is one of them, so deleting the directories fails
  `check-spec-references.sh` until its references go. The issue's Context counted twelve, with
  `.github/ISSUE_TEMPLATE/feature-specification.md` among them. That template has "Acceptance
  Criteria" and "Functional Requirements" headings and no path (§3.6).
- **The one target worth keeping survives.** Wave 9 task 1 and ADR-001 task 5.3 point at
  `specs/testing/performance-benchmarks/` for FR004, which `specs/features/indexing-workflow.md`
  states. The benchmark README's other targets are for routes the API does not have, such as
  `GET /documents/{id}`.
- **`spec-validation.yml`'s triggers miss part of the tree.** It runs on `specs/**`, `docs/**`,
  `CLAUDE.md`, `.claude/**` and four named paths under `.github/`. A Markdown file added under
  `.github/ISSUE_TEMPLATE/` runs nothing.

## 3. Design

### 3.1 Decisions

| # | Decision | Why |
|---|---|---|
| D1 | A reference is the target of an inline Markdown link, or an inline code span on one line | Links alone reach nothing (§2.1); the tree is written with backtick paths, and rewriting every one as a link is a story of its own |
| D2 | A path to a directory reaches the tracked `.md` files directly inside it, not those in subdirectories | Owner's choice, rule C of the brainstorm. A directory path works as GitHub's listing of it does. Recursion would make one mention of `specs/` pass everything; `README.md` only would need a 46-line story index kept by hand |
| D3 | No exemptions | Owner's choice. A document that should pass gets a real reference. An exemption list is an exit that grows. AC002 and AC003 change to match (§3.6) |
| D4 | `specs/stories/` and `.github/ISSUE_TEMPLATE/` get deliberate references in `docs/process/ai-driven-development.md`, worded as descriptions | Owner's choice. The process doc is read when the process is, not every session as `CLAUDE.md` is. A description, not "Read …", so an agent is pointed at the directory without being told to open it |
| D5 | `docs/architecture/README.md` is deleted with the scaffolding | Owner's choice. It is the same kind of placeholder, its one claim is false (§2.1), and nothing current points at it |
| D6 | Only tracked files count, from `git ls-files` | CI checks out tracked files. A local run that walked through `.logs/` or `.superpowers/` would pass where CI fails |
| D7 | The walk reads every tracked Markdown file it reaches, inside the three directories or not; only those inside are checked | `CLAUDE.md` routes through `.claude/skills/` and the root `README.md` like any other document. Out of scope means not checked, not unreadable |
| D8 | A target that names no tracked Markdown file or directory reaches nothing, and is ignored | Broken links are a separate check. This is also why the brainstorm's rules for placeholders, URL schemes and `?query` were dropped while drafting: none changes a result, because a placeholder, a URL or a query names no tracked path anyway. A test keeps the URL case on record |
| D9 | Absolute URLs are not followed, a github.com link to this repository's own file included | The wiki links repository files that way, by its own rule. Mapping those URLs back to paths is not needed for the tree to pass, and is left out |
| D10 | `CLAUDE.md` does not change | It is loaded in every session. The failure message says what to do, so the rule is met where it matters |
| D11 | The workflow gains `'**/*.md'` as a trigger | D7 means any tracked Markdown file can change the answer. The cost is spec-validation also running when a module's site page changes |
| D12 | A job of its own, "Check Documentation Map" | One job per check, as the workflow does, so the check list on a PR names it |

### 3.2 The checker

`.github/scripts/check-docs-reachable.sh [REPO_ROOT]`, plain bash. §6 Task 1 has it in full.

1. **Root.** The walk starts at `CLAUDE.md`, which must be tracked at the root of the work tree.
2. **Scope.** Every tracked `*.md` file whose path starts with `docs/`, `specs/` or `.github/`.
3. **Walk.** Breadth first, with a visited set, over every tracked Markdown file reached (D7). A
   file that is not Markdown is never read.
4. **References.** ``grep -oP '\]\([^)\s]+|`[^`]+`'``: a link target, or a code span matched as a
   pair so that a closing backtick cannot open the next span, as `check-spec-references.sh`
   does. A `#anchor` is stripped.
5. **Resolution.** A target starting with `/` is relative to the root. Any other is tried
   relative to the directory of the file that names it, then relative to the root. `.` and `..`
   are resolved as strings, with no subprocess per path: a fork per call is slow on Windows.
6. **Reach.** A target that names a tracked Markdown file reaches it. One that names a directory
   holding tracked Markdown files reaches those directly inside it (D2). Anything else reaches
   nothing (D8).
7. **Exit.** On success it prints `Reached N of N documents under docs/, specs/ and .github/ from
   CLAUDE.md.` and exits 0. When documents are unreached, it prints the count line, then each one,
   sorted, then how to fix it, and exits 1. It also exits 1 when `REPO_ROOT` is not in a git work
   tree, when `CLAUDE.md` is not tracked at the root, when `grep` fails on a file, and when there
   is no document in scope at all.

Measured with the draft: about 16 seconds on Windows on the real tree. Its 26 test cases take
about 12.

### 3.3 CI and the documents that describe it

- **`.github/workflows/spec-validation.yml`.** `'**/*.md'` joins both trigger lists (D11). A new
  job after `check-spec-references`:

  ```yaml
    check-docs-reachable:
      name: Check Documentation Map
      runs-on: ubuntu-latest
      permissions:
        contents: read
      steps:
        - name: Checkout repository
          uses: actions/checkout@v4

        - name: Test the documentation map checker
          run: bash .github/scripts/check-docs-reachable.test.sh

        - name: Verify every document is reachable from CLAUDE.md
          run: bash .github/scripts/check-docs-reachable.sh
  ```

- **`docs/devops/README.md`.** The `spec-validation.yml` row gains the trigger and two sentences
  on the check. The Mermaid node's label gains "+ documentation map". Exact text in §6 Task 2.
- **`docs/process/ai-driven-development.md`.** One sentence under "Step 2" names
  `.github/ISSUE_TEMPLATE/`, and one under "Step 3" names `specs/stories/` (D4). Exact text in §6
  Task 3.

### 3.4 The deletions and the references to them

**Deleted, 9 files:** `specs/testing/` (3), `specs/requirements/` (4), `docs/user-guides/` (1),
`docs/architecture/` (1).

Each deleted path stood for something that lives elsewhere now. Each reference is pointed there, or
removed when there is nowhere:

| The path stood for | It lives in |
|---|---|
| Acceptance criteria | The story: its GitHub issue and its spec in `specs/stories/` |
| Performance targets | FR004 in `specs/features/indexing-workflow.md` |
| Test patterns and plans | `.github/copilot/rules/testing-patterns.md` |
| Requirements | The feature specs in `specs/features/` |
| User guides | Nowhere yet; Wave 10 task 5 writes the README as the user guide |

The 11 files, with the exact edits in §6 Task 4:

| File | Change |
|---|---|
| `.github/copilot-instructions.md` | Lines 36 and 116 removed; 39 and 99 point at FR004; 113 names the story and the feature specs |
| `.github/copilot/rules/testing-patterns.md` | Lines 236–237: FR004, and Wave 9 task 1 decides where load-test scripts live |
| `.github/roles.md` | Lines 12, 13, 19 and 36 |
| `.github/copilot/prompts/feature-implementation.md` | Lines 28, 43–44, 61 and 74 |
| `.github/copilot/prompts/test-generation.md` | Lines 26, 82 and 120 |
| `docs/copilot/prompt-examples.md` | Lines 138, 157 and 182, as their copies in `feature-implementation.md` |
| `docs/copilot/best-practices.md` | Line 103 |
| `docs/troubleshooting/README.md` | Line 25 removed |
| `specs/architecture/ADR-001-GCP-based-components.md` | Task 5.3 points at FR004. ADR-001 is Proposed, so it is revised in place, as ADR-003 did |
| `specs/product/PRD.md` | Wave 9 task 1 points at FR004. Wave 1 task 4's own text, which names what it deletes, stays: the PRD keeps the plan as written, and `dsh-reconcile-prd` adds the outcome |
| `specs/features/pom-hierarchy-migration.md` | Lines 415–416 removed from its references |

Story specs under `specs/stories/` keep their text: they are a record.

### 3.5 Out of scope

- Broken links. `check-spec-references.sh` checks `.github/copilot-instructions.md`'s; no check
  covers the rest, and this story does not add one.
- Checking Markdown outside the three directories: `.claude/`, the modules' `src/site/`, the
  root `README.md`. They are walked through (D7), not checked.
- Following absolute URLs (D9).
- Rewriting what the remaining documents say, beyond §3.4's edits. Retiring or merging
  `docs/copilot/` or `docs/superpowers/` is not this story's.
- Editing old story specs to drop paths this story deletes.
- Writing the tests, plans or requirements the scaffolding stood for.

### 3.6 Changes to the issue

The design changes four acceptance criteria and corrects the issue's Context. Proposed for the
owner's approval with this spec, and applied to the issue with `gh issue edit` on 2026-11-20, once
approved:

- **AC002** becomes: "**What counts is written down.** What counts as a reference is stated in one
  place in the repository, the check's own source, and described in `docs/devops/README.md`. There
  are no exemptions: a document that should pass is given a reference."
- **AC003**'s list loses "an exempt file" and gains "a file that is not tracked".
- **AC005** loses "or exempt": every document is reached.
- **AC006** gains `docs/architecture/`.
- **Context**: the scaffolding is 9 files, `docs/architecture/README.md` added; the references are
  in 11 files, not twelve, and `.github/ISSUE_TEMPLATE/feature-specification.md` is not one of
  them.

§7 holds the criteria as amended.

## 4. Verification design

| AC | Check |
|---|---|
| AC001 | Test cases "a document not reached fails and is named", "two documents not reached", "no documents in scope is a failure"; the checker on the real tree before Task 4 exits 1 and names the two files of `specs/requirements/functional/` |
| AC002 | The checker's header states the rule; `grep -n 'documentation map' docs/devops/README.md` finds the description; the script has no exemption list |
| AC003 | `check-docs-reachable.test.sh` exits 0 with its 26 cases; each case of the issue's list maps to a named case (§6 Task 1, Step 1); seven mutants of the checker each turn at least one case red (§6 Task 1, Step 5) |
| AC004 | `grep -c "'\*\*/\*.md'" .github/workflows/spec-validation.yml` prints 2; the new job is in the run of the pull request |
| AC005 | The checker on the branch prints `Reached 99 of 99 documents …` and exits 0; in a scratch export with the three accidental reaches removed it still exits 0 (§6 Task 3) |
| AC006 | `git ls-files specs/testing specs/requirements docs/user-guides docs/architecture` prints nothing |
| AC007 | `git grep -n -E 'specs/testing\|specs/requirements\|user-guides\|docs/architecture' -- . ':!specs/stories/'` prints exactly one line, Wave 1 task 4's plan text in the PRD; `check-spec-references.sh` exits 0 |
| AC008 | The markdownlint command in `CLAUDE.md` exits 0 |
| Gate 1 | `mvn -B clean install`, logged to `.logs/mvn-clean-install.log`, exits 0, as `#192` ran it for a change with no Java code |
| Gate 3 | Does not apply: nothing touches an external system or a REST entry point |
| Scope | `git diff --stat DEVELOP` lists only the files of §5 |

## 5. Files to change

| File | Change |
|---|---|
| `.github/scripts/check-docs-reachable.sh` | New, mode 100755 (§6 Task 1) |
| `.github/scripts/check-docs-reachable.test.sh` | New, mode 100755 (§6 Task 1) |
| `.github/workflows/spec-validation.yml` | Trigger and job (§6 Task 2) |
| `docs/devops/README.md` | The row and the diagram label (§6 Task 2) |
| `docs/process/ai-driven-development.md` | Two sentences (§6 Task 3) |
| 9 scaffolding files | Deleted (§6 Task 4) |
| The 11 files of §3.4 | Edited (§6 Task 4) |
| `specs/stories/195-documentation-map.md` | This spec, and its verification results |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `dsh-build-story`, which applies
> superpowers:test-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** a CI check that every Markdown document under `docs/`, `specs/` and `.github/` is
reachable from `CLAUDE.md`, the tree brought to pass it, and the scaffolding deleted.

**Architecture:** one bash script and its bash test script, run by a new job in
`spec-validation.yml`; then Markdown edits and deletions, each verified by the checker, by
`git grep` and by `check-spec-references.sh`.

**Tech stack:** bash 5, GNU grep with `-P`, git, GitHub Actions, markdownlint.

**Spec:** this file, §1 to §5.

### Global constraints

- The checker and its tests are the text of Task 1, verbatim. The edits of Tasks 2 to 4 are the
  text given, verbatim.
- Shell commands run in Git Bash. markdownlint is run as
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint ...`; there is no other Node.
- Write a script with the Write tool, never a heredoc: a heredoc of this size has failed in the
  Bash tool and written nothing.
- Scratch exports and mutants go in the session's scratchpad, never in the repository.
- Commits: `ci(#195): ...` for the scripts and the workflow, `docs(#195): ...` for the rest, each
  ending with the attribution lines the session gives.
- Never push, never open the PR, never close an issue: steps 5 and 6 are `dsh-ship-story`'s.

### Task 1: The checker and its tests

**Files:**

- Create: `.github/scripts/check-docs-reachable.test.sh`
- Create: `.github/scripts/check-docs-reachable.sh`

**Interfaces:**

- Produces: `bash .github/scripts/check-docs-reachable.sh [REPO_ROOT]`, exit 0 when every document
  is reached, 1 otherwise; its success line `Reached N of N documents under docs/, specs/ and
  .github/ from CLAUDE.md.`; and `bash .github/scripts/check-docs-reachable.test.sh`, exit 0 when
  every case passes.

- [ ] **Step 1: Write the tests**

Create `.github/scripts/check-docs-reachable.test.sh` with exactly this text:

```bash
#!/usr/bin/env bash
#
# Tests for check-docs-reachable.sh.
#
# Plain bash: no Node, no Python, no test framework. Each case builds a fixture
# repository under a temporary directory -- git init, then git add for every
# file meant to be tracked, because the checker reads git ls-files -- runs the
# checker against it, and asserts the exit code and, where the message is the
# point, what the output names.
#
# Usage: check-docs-reachable.test.sh
#
# Exits non-zero if any case fails.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKER="${SCRIPT_DIR}/check-docs-reachable.sh"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cases=0
failures=0
OUT=""
STATUS=0

# new_case -> prints a fresh fixture directory, initialised as a git repository.
# It runs in a command substitution, so it cannot keep a counter -- mktemp gives
# each case its own directory and "check" does the counting in the main shell.
new_case() {
  local dir
  dir="$(mktemp -d "${WORK}/case-XXXXXX")"
  git -C "$dir" init -q
  printf %s "$dir"
}

# put DIR PATH TEXT -> writes TEXT to DIR/PATH, creating its directory. The file
# is not tracked.
put() {
  mkdir -p "$(dirname "$1/$2")"
  printf '%s\n' "$3" > "$1/$2"
}

# track DIR PATH TEXT -> as put, then git add, so the file is tracked.
track() {
  put "$1" "$2" "$3"
  git -C "$1" -c core.autocrlf=false add -- "$2"
}

# run_checker ROOT -> captures combined output in OUT and status in STATUS. The
# timeout turns a walk that never ends into a failure instead of a hung job.
run_checker() {
  OUT="$(timeout 20 bash "$CHECKER" "$1" 2>&1)"
  STATUS=$?
}

# report NAME REASON -> prints PASS when REASON is empty, FAIL and the output
# otherwise, and counts the case.
report() {
  cases=$((cases + 1))
  if [ -z "$2" ]; then
    echo "PASS: $1"
  else
    echo "FAIL: $1"
    echo "      $2"
    printf '      output: %s\n' "${OUT:-<empty>}"
    failures=$((failures + 1))
  fi
}

# check NAME EXPECTED_STATUS [EXPECTED_TEXT]
check() {
  local reason=""
  if [ "$STATUS" -ne "$2" ]; then
    reason="expected exit $2, got ${STATUS}"
  elif [ -n "${3:-}" ] && ! printf '%s\n' "$OUT" | grep -qF -- "$3"; then
    reason="output did not contain: $3"
  fi
  report "$1" "$reason"
}

# check_absent NAME TEXT -> the output must not contain TEXT.
check_absent() {
  local reason=""
  if printf '%s\n' "$OUT" | grep -qF -- "$2"; then
    reason="output contained: $2"
  fi
  report "$1" "$reason"
}

# check_listed NAME EXPECTED -> the indented lines of the output, the unreached
# documents, joined by spaces, must equal EXPECTED.
check_listed() {
  local listed reason=""
  listed="$(printf '%s\n' "$OUT" | sed -n 's/^  //p' | tr '\n' ' ')"
  if [ "$listed" != "$2" ]; then
    reason="listed '${listed}', expected '$2'"
  fi
  report "$1" "$reason"
}

# --- reached directly ---------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/a.md` first.'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "reached directly by a path in backticks" 0 \
  "Reached 1 of 1 documents under docs/, specs/ and .github/ from CLAUDE.md."

# --- reached through another document -----------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/a.md`.'
track "$d" docs/a.md 'Then `specs/b.md`.'
track "$d" specs/b.md 'B.'
run_checker "$d"
check "reached through another document" 0 "Reached 2 of 2 documents"

# --- not reached --------------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/a.md`.'
track "$d" docs/a.md 'A.'
track "$d" specs/lost.md 'Nothing points here.'
run_checker "$d"
check "a document not reached fails and is named" 1 "  specs/lost.md"
check "the failure says how to fix it" 1 "Reference each one from a document that is reached"
check "the failure counts what was reached" 1 "Reached 1 of 2 documents"

# --- two not reached, listed sorted -------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Nothing here.'
track "$d" specs/z.md 'Z.'
track "$d" docs/m.md 'M.'
run_checker "$d"
check "two documents not reached" 1 "2 document(s) cannot be reached"
check_listed "the unreached documents are listed sorted" "docs/m.md specs/z.md "

# --- a Markdown link ----------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'See [the guide](docs/a.md).'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "reached by a Markdown link" 0 "Reached 1 of 1"

# --- a path rooted at / -------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/x/a.md`.'
track "$d" docs/x/a.md 'Then `/specs/b.md`.'
track "$d" specs/b.md 'B.'
run_checker "$d"
check "a path starting with / is relative to the root" 0 "Reached 2 of 2"

# --- a path relative to the referencing file ----------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/x/a.md`.'
track "$d" docs/x/a.md 'Then [b](../b.md).'
track "$d" docs/b.md 'B.'
run_checker "$d"
check "a path relative to the file that names it" 0 "Reached 2 of 2"

# --- a root-relative path without /, from a nested document -------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/x/a.md`.'
track "$d" docs/x/a.md 'Then `specs/b.md`.'
track "$d" specs/b.md 'B.'
run_checker "$d"
check "a path without / falls back to the root" 0 "Reached 2 of 2"

# --- a directory reaches the files directly inside it -------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Specs are in `specs/stories/`.'
track "$d" specs/stories/1-one.md 'One.'
track "$d" specs/stories/2-two.md 'Two.'
run_checker "$d"
check "a directory reaches the files directly inside it" 0 "Reached 2 of 2"

# --- a directory does not reach into its subdirectories -----------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Everything is under `specs/`.'
track "$d" specs/a.md 'A.'
track "$d" specs/sub/b.md 'B.'
run_checker "$d"
check "a directory does not reach a subdirectory's file" 1 "  specs/sub/b.md"
check_listed "only the subdirectory's file is unreached" "specs/sub/b.md "

# --- a cycle ------------------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/a.md`.'
track "$d" docs/a.md 'See `docs/b.md`.'
track "$d" docs/b.md 'Back to `docs/a.md`.'
run_checker "$d"
check "a cycle of references ends" 0 "Reached 2 of 2"

# --- a reference to a file that does not exist --------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Read `docs/gone.md`, then `docs/a.md`.'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "a reference to a missing file is ignored" 0 "Reached 1 of 1"

# --- an anchor ----------------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'See [part two](docs/a.md#part-two).'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "an anchor is stripped from a target" 0 "Reached 1 of 1"

# --- a URL is not a path ------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'See [a](https://github.com/owner/repo/blob/main/docs/a.md).'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "a URL reaches nothing, even one naming a tracked path" 1 "  docs/a.md"

# --- the walk passes through a tracked file outside the scope -----------------
d="$(new_case)"
track "$d" CLAUDE.md 'Skills: `.claude/skills/s/SKILL.md`.'
track "$d" .claude/skills/s/SKILL.md 'Uses `docs/a.md`.'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "the walk passes through a tracked file outside the scope" 0 "Reached 1 of 1"

# --- a file that is not Markdown is not read ----------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'The workflow is `build.yml`.'
track "$d" build.yml '# reads `docs/a.md`'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "a file that is not Markdown is not read" 1 "  docs/a.md"

# --- untracked files count for nothing ----------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'Notes: `notes/n.md`.'
put "$d" notes/n.md 'Points at `docs/a.md`.'
track "$d" docs/a.md 'A.'
put "$d" docs/untracked.md 'Not tracked.'
run_checker "$d"
check "an untracked file reaches nothing" 1 "  docs/a.md"
check_absent "an untracked document is not checked" "docs/untracked.md"

# --- nothing to check ---------------------------------------------------------
d="$(new_case)"
track "$d" CLAUDE.md 'No documents yet.'
run_checker "$d"
check "no documents in scope is a failure" 1 "the check verified nothing"

# --- CLAUDE.md missing --------------------------------------------------------
d="$(new_case)"
track "$d" docs/a.md 'A.'
run_checker "$d"
check "a repository without CLAUDE.md fails" 1 "CLAUDE.md is not tracked"

d="$(new_case)"
put "$d" CLAUDE.md 'Read `docs/a.md`.'
track "$d" docs/a.md 'A.'
run_checker "$d"
check "an untracked CLAUDE.md fails" 1 "CLAUDE.md is not tracked"

# --- not a git repository -----------------------------------------------------
d="$(mktemp -d "${WORK}/plain-XXXXXX")"
put "$d" CLAUDE.md 'Read `docs/a.md`.'
put "$d" docs/a.md 'A.'
OUT="$(GIT_CEILING_DIRECTORIES="$WORK" timeout 20 bash "$CHECKER" "$d" 2>&1)"
STATUS=$?
check "a directory outside any git work tree fails" 1 "is not in a git work tree"

# --- summary ------------------------------------------------------------------
echo
echo "${cases} case(s), ${failures} failure(s)."
if [ "$failures" -gt 0 ]; then
  exit 1
fi
```

The issue's AC003 list, as amended (§3.6), maps to these cases: reached directly → "reached
directly by a path in backticks"; through another document → "reached through another document";
not reached → "a document not reached fails and is named"; a cycle → "a cycle of references ends";
each form that counts → the backtick, link, `/`-rooted, relative, root-fallback and directory
cases; a missing file → "a reference to a missing file is ignored"; a file that is not tracked →
"an untracked file reaches nothing" and "an untracked document is not checked"; a run that
checked nothing → "no documents in scope is a failure".

- [ ] **Step 2: Run the tests, expecting them to fail**

```bash
bash .github/scripts/check-docs-reachable.test.sh; echo "test exit=$?"
```

Expected: `26 case(s), 25 failure(s).` and `test exit=1`. The checker does not exist, so `bash`
exits 127 and every case that asserts an exit code or a listed file fails. The one `PASS` is "an
untracked document is not checked", which asserts only that a path is absent from the output, and
holds vacuously here. Step 4 makes it hold for the right reason, and Step 5's second mutant shows
it can fail.

- [ ] **Step 3: Write the checker**

Create `.github/scripts/check-docs-reachable.sh` with exactly this text:

```bash
#!/usr/bin/env bash
#
# Verify that every Markdown document under docs/, specs/ and .github/ can be
# reached from CLAUDE.md by following references.
#
# The walk starts at CLAUDE.md. It reads every tracked Markdown file it reaches,
# inside those three directories or not, and follows two kinds of reference:
#
#   - the target of an inline Markdown link: [text](target)
#   - an inline code span on one line:        `target`
#
# A "#anchor" is stripped from a target. A target starting with "/" is relative
# to the repository root; any other is tried relative to the directory of the
# file that names it, then relative to the root. A target naming a directory
# reaches the tracked .md files directly inside it, not those in subdirectories.
# A target that names no tracked Markdown file or directory -- a command, a URL,
# a placeholder, a file that no longer exists -- reaches nothing and is ignored:
# broken links are not this check's concern.
#
# Only tracked files count. The list comes from git ls-files, so a local run
# answers as CI does, whatever untracked files lie in the work tree.
#
# There are no exemptions. A document that should pass gets a reference from a
# document that is reached.
#
# Usage: check-docs-reachable.sh [REPO_ROOT]
#
# REPO_ROOT is any directory inside the work tree; the whole repository is
# checked. Exits non-zero when a document is not reached, when REPO_ROOT is not
# in a git work tree, when CLAUDE.md is not tracked at the root, when a file
# cannot be read, or when there are no documents to check at all -- a run that
# verified nothing is a failure, not a pass.

set -euo pipefail

ROOT="${1:-.}"
SCOPE_LABEL="docs/, specs/ and .github/"

if ! top="$(git -C "$ROOT" rev-parse --show-toplevel 2>/dev/null)"; then
  echo "ERROR: $ROOT is not in a git work tree."
  exit 1
fi
cd "$top"

if ! listing="$(git -c core.quotePath=false ls-files -- '*.md')"; then
  echo "ERROR: git ls-files failed in $top."
  exit 1
fi

declare -A doc=()     # every tracked Markdown file -> 1
declare -A dirmd=()   # a directory -> the tracked .md files directly inside it
declare -A seen=()    # every file the walk has reached -> 1
queue=()

while IFS= read -r path; do
  [ -n "$path" ] || continue
  doc[$path]=1
  if [[ "$path" == */* ]]; then
    dirmd[${path%/*}]+="$path"$'\n'
  fi
done <<< "$listing"

if [ -z "${doc[CLAUDE.md]+x}" ]; then
  echo "ERROR: CLAUDE.md is not tracked at the repository root."
  exit 1
fi

# normalise PATH -> sets REPLY to PATH with "." and ".." resolved and no empty
# segment. A ".." above the root is dropped. No subprocess: this runs once per
# candidate path, and a fork per call is slow on Windows.
normalise() {
  local -a parts=() out=()
  local part
  IFS=/ read -r -a parts <<< "$1"
  for part in "${parts[@]}"; do
    case "$part" in
      '' | .) ;;
      ..) if [ "${#out[@]}" -gt 0 ]; then unset 'out[-1]'; fi ;;
      *) out+=("$part") ;;
    esac
  done
  local IFS=/
  REPLY="${out[*]}"
}

# visit FILE -> queues FILE unless the walk has reached it already.
visit() {
  if [ -z "${seen[$1]+x}" ]; then
    seen[$1]=1
    queue+=("$1")
  fi
}

# follow FROM TARGET -> visits what TARGET reaches when the file FROM names it.
follow() {
  local from="$1" target="${2%%#*}" dir="" candidate child
  local -a candidates=()
  [ -n "$target" ] || return 0
  if [ "${target:0:1}" = / ]; then
    candidates=("$target")
  else
    if [[ "$from" == */* ]]; then dir="${from%/*}"; fi
    candidates=("$dir/$target" "$target")
  fi
  for candidate in "${candidates[@]}"; do
    normalise "$candidate"
    [ -n "$REPLY" ] || continue
    if [ -n "${doc[$REPLY]+x}" ]; then
      visit "$REPLY"
      return 0
    fi
    if [ -n "${dirmd[$REPLY]+x}" ]; then
      while IFS= read -r child; do
        if [ -n "$child" ]; then visit "$child"; fi
      done <<< "${dirmd[$REPLY]}"
      return 0
    fi
  done
  return 0
}

visit CLAUDE.md
i=0
while [ "$i" -lt "${#queue[@]}" ]; do
  file="${queue[$i]}"
  i=$((i + 1))
  # A link target, or a code span matched as a pair so that a closing backtick
  # cannot open the next span. grep exits 1 on no matches; anything higher is
  # the extractor failing and must not read as "this file references nothing".
  if refs="$(grep -oP '\]\([^)\s]+|`[^`]+`' -- "$file")"; then
    :
  else
    status=$?
    if [ "$status" -gt 1 ]; then
      echo "ERROR: could not read references from $file; grep exited $status."
      exit 1
    fi
    continue
  fi
  while IFS= read -r ref; do
    case "$ref" in
      ']('*) follow "$file" "${ref:2}" ;;
      '`'*) follow "$file" "${ref:1:${#ref}-2}" ;;
    esac
  done <<< "$refs"
done

total=0
unreached=()
for path in "${!doc[@]}"; do
  case "$path" in
    docs/* | specs/* | .github/*) ;;
    *) continue ;;
  esac
  total=$((total + 1))
  if [ -z "${seen[$path]+x}" ]; then
    unreached+=("$path")
  fi
done

if [ "$total" -eq 0 ]; then
  echo "ERROR: no Markdown documents under $SCOPE_LABEL; the check verified nothing."
  exit 1
fi

echo "Reached $((total - ${#unreached[@]})) of $total documents under $SCOPE_LABEL from CLAUDE.md."

if [ "${#unreached[@]}" -gt 0 ]; then
  echo "ERROR: ${#unreached[@]} document(s) cannot be reached from CLAUDE.md:"
  printf '%s\n' "${unreached[@]}" | LC_ALL=C sort | sed 's/^/  /'
  echo "Reference each one from a document that is reached -- a Markdown link, a path in"
  echo "backticks, or a path to its directory -- or delete it if nothing needs it."
  exit 1
fi
```

- [ ] **Step 4: Run the tests, expecting them to pass**

```bash
bash .github/scripts/check-docs-reachable.test.sh; echo "test exit=$?"
```

Expected: 26 `PASS` lines, `26 case(s), 0 failure(s).`, `test exit=0`.

- [ ] **Step 5: Show that every rule is held by a test**

Copy both scripts into a scratch directory, change the checker as each row says, run the copied
test script, and record which cases fail. Restore before the next row. A mutant that no case
catches is a missing test: add the case before going on.

| Mutant | Expected red |
|---|---|
| A directory reaches every `.md` file under it, at any depth | "a directory does not reach a subdirectory's file", "only the subdirectory's file is unreached" |
| Files are listed with `find`, not `git ls-files` | the two untracked cases and "an untracked CLAUDE.md fails" |
| The empty-scope guard is `if false` | "no documents in scope is a failure" |
| `visit` queues a file it has seen | "a cycle of references ends" (the timeout) |
| The `#anchor` is not stripped | "an anchor is stripped from a target" |
| Only the path relative to the file is tried | "a path without / falls back to the root" and the cases that depend on it |
| Only the root-relative path is tried | "a path relative to the file that names it" |

These seven were run against this exact text while the spec was written, and each went red as
listed.

- [ ] **Step 6: Run the checker on the tree, expecting it to fail**

```bash
bash .github/scripts/check-docs-reachable.sh; echo "check exit=$?"
```

Expected: `check exit=1`, naming exactly `specs/requirements/functional/README.md` and
`specs/requirements/functional/overall-process-description.md`. Task 4 deletes them.

- [ ] **Step 7: Commit**

```bash
git add .github/scripts/check-docs-reachable.sh .github/scripts/check-docs-reachable.test.sh
git update-index --chmod=+x .github/scripts/check-docs-reachable.sh .github/scripts/check-docs-reachable.test.sh
git ls-files -s .github/scripts/check-docs-reachable*.sh
git commit -m "ci(#195): add a check that every document is reachable from CLAUDE.md"
```

Expected: both files listed with mode `100755`, as the other scripts there are.

### Task 2: The CI job and its description

**Files:**

- Modify: `.github/workflows/spec-validation.yml`
- Modify: `docs/devops/README.md`, line 52 and the `spec-validation.yml` row at line 74

**Interfaces:**

- Consumes: the two scripts of Task 1, by path.

- [ ] **Step 1: Run the checks, expecting them to fail**

```bash
grep -c "'\*\*/\*.md'" .github/workflows/spec-validation.yml; echo "grep exit=$?"
grep -n 'check-docs-reachable' .github/workflows/spec-validation.yml docs/devops/README.md; echo "grep exit=$?"
```

Expected: `0` with `grep exit=1`, then no lines with `grep exit=1`.

- [ ] **Step 2: Edit the workflow**

After each of the two `- '.claude/**'` lines, one under `push.paths` and one under
`pull_request.paths`, add, at the same indentation:

```yaml
      - '**/*.md'
```

After the `check-spec-references` job, before `test-wiki-publisher`, add the job of §3.3, with one
blank line before and after it.

- [ ] **Step 3: Edit `docs/devops/README.md`**

On line 52, replace `OpenAPI lint + markdownlint + spec references` with
`OpenAPI lint + markdownlint + spec references + documentation map`.

In the `spec-validation.yml` row, replace `` `CLAUDE.md`, `.claude/**` | `` with
`` `CLAUDE.md`, `.claude/**`, `**/*.md` | ``. In the same row, after the sentence "Its logic is
covered by `.github/scripts/check-spec-references.test.sh`, which the same job runs first.", insert:

```markdown
A separate job checks the documentation map: every Markdown file under `docs/`, `specs/` and `.github/` must be reachable from `CLAUDE.md` through Markdown links or paths in backticks, where a path to a directory reaches the files directly inside it. Only tracked files count, and there are no exemptions. Its logic is covered by `.github/scripts/check-docs-reachable.test.sh`, which that job runs first.
```

The row stays one line.

- [ ] **Step 4: Run the checks, expecting them to pass**

The commands of Step 1. Expected: `2` with `grep exit=0`; then lines in both files with
`grep exit=0`. Then:

```bash
grep -n 'documentation map' docs/devops/README.md
bash .github/scripts/check-docs-reachable.test.sh | tail -1
```

Expected: lines 52 and 74; `26 case(s), 0 failure(s).`

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/spec-validation.yml docs/devops/README.md
git commit -m "ci(#195): run the documentation map check on every Markdown change"
```

### Task 3: The deliberate references

**Files:**

- Modify: `docs/process/ai-driven-development.md`, "Step 2" and "Step 3"
- Scratch, not committed: an export of `HEAD` in the scratchpad

**Interfaces:**

- Consumes: `check-docs-reachable.sh` of Task 1.

- [ ] **Step 1: Prove the reaches are accidental, expecting the check to fail**

Set `SCRATCH` to the session's scratchpad directory first; the command refuses to run without it.

```bash
REPO="$(git rev-parse --show-toplevel)"
S="${SCRATCH:?set SCRATCH to the session scratchpad}/export-195"
rm -rf "$S" && mkdir -p "$S" && git archive HEAD | tar -x -C "$S"
cd "$S" && git init -q
sed -i 's#`specs/stories/`#the stories#' specs/stories/177-worker-modules-to-libraries-remove-solr.md
rm -rf docs/superpowers
git add -A
bash "$REPO/.github/scripts/check-docs-reachable.sh" .; echo "check exit=$?"
cd "$REPO"
```

Expected: `check exit=1`, listing every story spec but those reached by name, the three templates
other than `story.md`, `docs/architecture/README.md` and the two files of
`specs/requirements/functional/`.

- [ ] **Step 2: Add the two sentences**

In "Step 2", replace:

```markdown
**Artifact.** A GitHub issue, numbered, with an INVEST-shaped body that links back to its PRD wave
and task.
```

with:

```markdown
**Artifact.** A GitHub issue, numbered, with an INVEST-shaped body that links back to its PRD wave
and task. The body follows `.github/ISSUE_TEMPLATE/story.md`, one of the issue templates in
`.github/ISSUE_TEMPLATE/`.
```

In "Step 3", replace:

```markdown
`specs/stories/<n>-<slug>.md` committed on it, front matter filled in per the contract below.
```

with:

```markdown
`specs/stories/<n>-<slug>.md` committed on it, front matter filled in per the contract below.
`specs/stories/` holds one spec per story.
```

- [ ] **Step 3: Commit**

```bash
git add docs/process/ai-driven-development.md
git commit -m "docs(#195): name the story specs and the issue templates in the process, so the map reaches them"
```

- [ ] **Step 4: Repeat Step 1, expecting only `docs/architecture/` and the functional files**

The commands of Step 1, on the new `HEAD`. Expected: `check exit=1`, listing exactly
`docs/architecture/README.md` and the two files of `specs/requirements/functional/`. Task 4
deletes all three, and Task 5 repeats this proof expecting a pass.

### Task 4: The scaffolding, deleted, and every reference to it

**Files:**

- Delete: `specs/testing/`, `specs/requirements/`, `docs/user-guides/`, `docs/architecture/`
- Modify: the 11 files of §3.4

**Interfaces:**

- Consumes: `check-docs-reachable.sh` of Task 1; `check-spec-references.sh`.

- [ ] **Step 1: Run the checks, expecting them to fail**

```bash
git grep -n -E 'specs/testing|specs/requirements|user-guides|docs/architecture' -- . ':!specs/stories/'; echo "grep exit=$?"
bash .github/scripts/check-docs-reachable.sh | head -1; echo "check exit=${PIPESTATUS[0]}"
```

Expected: 28 lines across the 11 files of §3.4, with `grep exit=0`; `check exit=1`.

- [ ] **Step 2: Delete the scaffolding**

```bash
git rm -rq specs/testing specs/requirements docs/user-guides docs/architecture
git status --short | grep -c '^D '
```

Expected: `9`.

- [ ] **Step 3: Edit `.github/copilot-instructions.md`**

```diff
-- Integration tests: Reference `/specs/testing/test-plans/`
```

```diff
-- Performance tests: Reference `/specs/testing/performance-benchmarks/`
+- Performance tests: the target is FR004 in `/specs/features/indexing-workflow.md`
```

```diff
-- Reference performance benchmarks: `/specs/testing/performance-benchmarks/`
+- Performance target: its FR004
```

```diff
-- Generate tests based on specifications in `/specs/testing/`
+- Generate tests from the story's acceptance criteria, in its GitHub issue and its spec under `/specs/stories/`, and from the feature specs in `/specs/features/`
```

```diff
-- Reference acceptance criteria in `/specs/requirements/acceptance-criteria/`
```

- [ ] **Step 4: Edit `.github/copilot/rules/testing-patterns.md`**

```diff
-- Reference benchmark targets in `/specs/testing/performance-benchmarks/`
-- Use JMeter or Gatling for load tests; store scripts in `/specs/testing/performance-benchmarks/`
+- The performance target is FR004 in `/specs/features/indexing-workflow.md`
+- Use JMeter or Gatling for load tests. Wave 9 task 1, the first performance test, decides where their scripts live
```

- [ ] **Step 5: Edit `.github/roles.md`**

```diff
-- Define and document functional and non-functional requirements in `specs/requirements/`
-- Write acceptance criteria in `specs/requirements/acceptance-criteria/`
+- Define functional and non-functional requirements in the feature specifications
+- Write acceptance criteria into each story, the GitHub issue `dsh-new-story` drafts
```

```diff
-- User stories and requirements (`specs/requirements/`)
+- User stories, as GitHub issues written from `.github/ISSUE_TEMPLATE/story.md`
```

```diff
-- Write unit and integration tests following patterns in `specs/testing/`
+- Write unit and integration tests following `.github/copilot/rules/testing-patterns.md`
```

- [ ] **Step 6: Edit the two prompts**

`.github/copilot/prompts/feature-implementation.md`:

```diff
-- Cover acceptance criteria from /specs/requirements/acceptance-criteria/
+- Cover the acceptance criteria of the story: its GitHub issue and its spec in /specs/stories/
```

```diff
-- Ensure the implementation matches acceptance criteria in
-  /specs/requirements/acceptance-criteria/[feature].md
+- Ensure the implementation matches the story's acceptance criteria
+  (its GitHub issue and its spec in /specs/stories/)
```

```diff
-- Reference /specs/testing/performance-benchmarks/ for any performance targets
+- Take performance targets from FR004 in /specs/features/indexing-workflow.md
```

```diff
-- Ensure acceptance criteria exist in /specs/requirements/acceptance-criteria/
+- Ensure the story states acceptance criteria that a test can check
```

`.github/copilot/prompts/test-generation.md`:

```diff
-- Cover acceptance criteria from /specs/requirements/acceptance-criteria/[feature].md
+- Cover the acceptance criteria of the story: its GitHub issue and its spec in /specs/stories/
```

```diff
-- Reference acceptance criteria in /specs/requirements/acceptance-criteria/[feature].md
+- Reference the story's acceptance criteria, in its GitHub issue and its spec in /specs/stories/
```

```diff
-- Reference benchmark targets in /specs/testing/performance-benchmarks/
+- Take the target from FR004 in /specs/features/indexing-workflow.md
```

- [ ] **Step 7: Edit the two pages in `docs/copilot/`**

`docs/copilot/prompt-examples.md`, lines 138, 157 and 182: the same three replacements as in
`feature-implementation.md` — "Cover acceptance criteria from", "Ensure acceptance criteria exist
in" and "Reference /specs/testing/performance-benchmarks/" — with the same new text.

`docs/copilot/best-practices.md`:

```diff
-- Reference the acceptance criteria from `specs/requirements/acceptance-criteria/`
+- Reference the story's acceptance criteria, in its GitHub issue and its spec in `specs/stories/`
```

- [ ] **Step 8: Edit the remaining four**

`docs/troubleshooting/README.md`:

```diff
-- User guides: `../user-guides/`
```

`specs/architecture/ADR-001-GCP-based-components.md`:

```diff
-| 5.3 | Performance-test against benchmarks in `specs/testing/performance-benchmarks/` (FR004: 95% of docs < 10 MB within 30 s). Wave 9 task 1. |
+| 5.3 | Performance-test against FR004 in `specs/features/indexing-workflow.md` (95% of docs < 10 MB within 30 s). Wave 9 task 1. |
```

`specs/product/PRD.md`, Wave 9 task 1:

```diff
-1. Performance-test against `specs/testing/performance-benchmarks/` (FR004: 95% of documents under
+1. Performance-test against FR004 in `specs/features/indexing-workflow.md` (95% of documents under
    10 MB processed within 30 s). It runs against `dsh-staging`, and the cost rule in `CLAUDE.md`
```

`specs/features/pom-hierarchy-migration.md`:

```diff
-- Test plans: `specs/testing/test-plans/`
-- Acceptance criteria: `specs/requirements/acceptance-criteria/`
```

- [ ] **Step 9: Run the checks, expecting them to pass**

```bash
git grep -n -E 'specs/testing|specs/requirements|user-guides|docs/architecture' -- . ':!specs/stories/'; echo "grep exit=$?"
bash .github/scripts/check-spec-references.sh | tail -1; echo "refs exit=${PIPESTATUS[0]}"
bash .github/scripts/check-docs-reachable.sh; echo "check exit=$?"
```

Expected: exactly one line, `specs/product/PRD.md` in Wave 1 task 4 (`specs/testing/*`,
`specs/requirements/*`, `docs/user-guides`), with `grep exit=0`; `refs exit=0`; `Reached 99 of 99
documents under docs/, specs/ and .github/ from CLAUDE.md.` with `check exit=0`.

- [ ] **Step 10: Commit**

```bash
git add -A specs docs .github
git status --short
git commit -m "docs(#195): delete the empty scaffolding, and point every reference at what replaced it"
```

Expected before the commit: 9 deletions and the 11 modified files of §3.4, nothing else.

### Task 5: The gates, the proof, and the results

**Files:**

- Modify: `specs/stories/195-documentation-map.md`, §8

**Interfaces:** none.

- [ ] **Step 1: Repeat Task 3's proof, expecting a pass**

The commands of Task 3, Step 1, on the new `HEAD`. Expected: `Reached 97 of 97 documents …` and
`check exit=0`: the two `docs/superpowers/` documents are gone from the export, and nothing else
depends on an accidental reach.

- [ ] **Step 2: Run the lint, both test scripts and both checks**

```bash
PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --config .markdownlint.json; echo "lint exit=$?"
bash .github/scripts/check-docs-reachable.test.sh | tail -1; echo "test exit=${PIPESTATUS[0]}"
bash .github/scripts/check-spec-references.test.sh | tail -1; echo "refs test exit=${PIPESTATUS[0]}"
bash .github/scripts/check-docs-reachable.sh; echo "check exit=$?"
bash .github/scripts/check-spec-references.sh | tail -1; echo "refs exit=${PIPESTATUS[0]}"
```

Expected: every exit 0.

- [ ] **Step 3: Run gate 1 and the scope check**

```bash
mkdir -p .logs
mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
wait $MVN_PID; echo "maven exit=$?"
git diff --stat DEVELOP
```

Expected: `maven exit=0`, `BUILD SUCCESS`; the diff lists only the files of §5.

- [ ] **Step 4: Record the results**

Fill §8 with every check of §4 run so far, the commands' exit codes, the red-then-green of each
task, and the mutant table of Task 1, Step 5, as run. The new job's result on the pull request is
recorded after the push, by `dsh-ship-story`.

- [ ] **Step 5: Commit**

```bash
git add specs/stories/195-documentation-map.md
git commit -m "docs(#195): record the verification results"
```

## 7. Acceptance criteria

As amended by §3.6.

- [ ] AC001: **A CI check fails on an unreachable document.** A job fails when a Markdown file
  under `docs/`, `specs/` or `.github/` cannot be reached from `CLAUDE.md` by following
  references, and its log names every such file. It fails, too, when it checked no files at all.
- [ ] AC002: **What counts is written down.** What counts as a reference is stated in one place
  in the repository, the check's own source, and described in `docs/devops/README.md`. There are
  no exemptions: a document that should pass is given a reference.
- [ ] AC003: **The check has its own tests, run before it in CI.** A test script, run by the same
  job before the check, covers at least: a file reached directly, one reached through another
  document, one not reached, a cycle of references (the check ends), each form of reference that
  counts, a reference to a file that does not exist, a file that is not tracked, and a run that
  checked no files.
- [ ] AC004: **The check runs whenever it could change its answer.** Its job runs on every pull
  request and push to `DEVELOP` that adds, moves, deletes or edits a Markdown file under `docs/`,
  `specs/` or `.github/`, or edits `CLAUDE.md`, the check, its tests or its workflow.
- [ ] AC005: **The tree passes.** On the merged branch the check passes: every Markdown file under
  the three directories is reached from `CLAUDE.md`.
- [ ] AC006: **The scaffolding is gone.** `specs/testing/`, `specs/requirements/`,
  `docs/user-guides/` and `docs/architecture/` no longer exist.
- [ ] AC007: **Nothing current points at it.** No current document names a deleted path. The
  references are removed or repointed, and Wave 9 task 1 and ADR-001 task 5.3 point at FR004 in
  `specs/features/indexing-workflow.md`. Story specs under `specs/stories/` keep their text.
  `check-spec-references.sh` passes.
- [ ] AC008: **The lint passes.** The markdown lint command in `CLAUDE.md` passes.

## 8. Verification results

Filled in by §6 Task 5.

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

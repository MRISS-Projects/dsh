#!/usr/bin/env bash
#
# Tests for publish-wiki.sh.
#
# Plain bash: no Node, no Python, no test framework. Each case builds a source
# directory and a wiki directory under a temporary directory, runs the publisher
# against them, and asserts the exit code, the output where the message is the
# point, and the files the wiki directory holds afterwards.
#
# Usage: publish-wiki.test.sh
#
# Exits non-zero if any case fails.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBLISHER="${SCRIPT_DIR}/publish-wiki.sh"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cases=0
failures=0
OUT=""
STATUS=0

# new_case -> prints a fresh fixture directory holding an empty "src", and a
# "wiki" that holds only a .git directory, as a clone does.
new_case() {
  local dir
  dir="$(mktemp -d "${WORK}/case-XXXXXX")"
  mkdir -p "${dir}/src" "${dir}/wiki/.git"
  printf %s "$dir"
}

# run_publisher ARGS... -> captures combined output in OUT and status in STATUS.
run_publisher() {
  OUT="$(bash "$PUBLISHER" "$@" 2>&1)"
  STATUS=$?
}

# listing DIR -> the top-level entries of DIR except .git, sorted, one per line.
listing() {
  (cd "$1" && ls -A | grep -vx '\.git' | LC_ALL=C sort)
}

# check NAME EXPECTED_STATUS EXPECTED_TEXT [ACTUAL_LISTING EXPECTED_LISTING]
check() {
  cases=$((cases + 1))
  local name="$1" expected_status="$2" expected_text="$3" reason=""

  if [ "$STATUS" -ne "$expected_status" ]; then
    reason="expected exit ${expected_status}, got ${STATUS}"
  elif [ -n "$expected_text" ] && ! printf '%s\n' "$OUT" | grep -qF -- "$expected_text"; then
    reason="output did not contain: ${expected_text}"
  elif [ "$#" -ge 5 ] && [ "$4" != "$5" ]; then
    reason="wiki holds [$(echo $4)], expected [$(echo $5)]"
  fi

  if [ -z "$reason" ]; then
    echo "PASS: ${name}"
  else
    echo "FAIL: ${name}"
    echo "      ${reason}"
    printf '      output: %s\n' "${OUT:-<empty>}"
    failures=$((failures + 1))
  fi
}

# --- every page is published, README.md is not ---------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "arch" > "${d}/src/Architecture.md"
echo "about the directory" > "${d}/src/README.md"
run_publisher "${d}/src" "${d}/wiki"
check "every page is published, README.md is not" 0 "Published: Home.md" \
  "$(listing "${d}/wiki")" "$(printf 'Architecture.md\nHome.md')"

# --- a changed page replaces the wiki's copy -----------------------------------
d="$(new_case)"
echo "new text" > "${d}/src/Home.md"
echo "old text" > "${d}/wiki/Home.md"
run_publisher "${d}/src" "${d}/wiki"
check "a changed page replaces the wiki's copy" 0 "" \
  "$(cat "${d}/wiki/Home.md")" "new text"

# --- a page with no source is removed ------------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "gone" > "${d}/wiki/Old-Page.md"
run_publisher "${d}/src" "${d}/wiki"
check "a page with no source is removed" 0 "Removed: Old-Page.md" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a README.md in the wiki is removed ----------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "about" > "${d}/src/README.md"
echo "stray" > "${d}/wiki/README.md"
run_publisher "${d}/src" "${d}/wiki"
check "a README.md in the wiki is removed" 0 "Removed: README.md" \
  "$(listing "${d}/wiki")" "Home.md"

# --- .git and files that are not Markdown are left alone -----------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "ref" > "${d}/wiki/.git/HEAD"
echo "png" > "${d}/wiki/diagram.png"
run_publisher "${d}/src" "${d}/wiki"
check ".git and files that are not Markdown are left alone" 0 "" \
  "$(listing "${d}/wiki"; cat "${d}/wiki/.git/HEAD")" "$(printf 'Home.md\ndiagram.png\nref')"

# --- Markdown below the top level is not a page --------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
mkdir -p "${d}/src/drafts"
echo "draft" > "${d}/src/drafts/Draft.md"
run_publisher "${d}/src" "${d}/wiki"
check "Markdown below the top level is not a page" 0 "" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a page name with a space is published -------------------------------------
d="$(new_case)"
echo "page" > "${d}/src/Two Words.md"
run_publisher "${d}/src" "${d}/wiki"
check "a page name with a space is published" 0 "Published: Two Words.md" \
  "$(listing "${d}/wiki")" "Two Words.md"

# --- a source with no page is refused, and the wiki is untouched ---------------
d="$(new_case)"
echo "about" > "${d}/src/README.md"
echo "home" > "${d}/wiki/Home.md"
run_publisher "${d}/src" "${d}/wiki"
check "a source with no page is refused, and the wiki is untouched" 1 "refusing to empty the wiki" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a missing source directory is an error ------------------------------------
d="$(new_case)"
run_publisher "${d}/absent" "${d}/wiki"
check "a missing source directory is an error" 1 "not a directory: ${d}/absent"

# --- a missing wiki directory is an error --------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
run_publisher "${d}/src" "${d}/absent"
check "a missing wiki directory is an error" 1 "not a directory: ${d}/absent"

# --- a wiki directory that is not a clone is refused, and is untouched ---------
d="$(new_case)"
rmdir "${d}/wiki/.git"
echo "home" > "${d}/src/Home.md"
echo "keep" > "${d}/wiki/README.md"
run_publisher "${d}/src" "${d}/wiki"
check "a wiki directory that is not a clone is refused, and is untouched" 1 \
  "not a clone of a repository: ${d}/wiki" \
  "$(listing "${d}/wiki")" "README.md"

# --- one directory as both source and wiki is refused, and is untouched --------
d="$(new_case)"
echo "home" > "${d}/wiki/Home.md"
echo "about" > "${d}/wiki/README.md"
run_publisher "${d}/wiki" "${d}/wiki/."
check "one directory as both source and wiki is refused, and is untouched" 1 \
  "SOURCE_DIR and WIKI_DIR are the same directory" \
  "$(listing "${d}/wiki")" "$(printf 'Home.md\nREADME.md')"

# --- a wrong number of arguments prints the usage ------------------------------
run_publisher "only-one"
check "a wrong number of arguments prints the usage" 2 "Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR"

echo
echo "${cases} case(s), ${failures} failure(s)."
[ "$failures" -eq 0 ]

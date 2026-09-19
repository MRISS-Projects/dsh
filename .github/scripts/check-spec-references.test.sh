#!/usr/bin/env bash
#
# Tests for check-spec-references.sh.
#
# Plain bash: no Node, no Python, no test framework. Each case builds a fixture
# tree under a temporary directory, runs the checker against it with an explicit
# source file and repository root, and asserts the exit code and -- where the
# message is the point -- that the output names the offending path.
#
# Usage: check-spec-references.test.sh
#
# Exits non-zero if any case fails.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKER="${SCRIPT_DIR}/check-spec-references.sh"

WORK="$(mktemp -d)"
trap 'chmod -R u+rwx "$WORK" 2>/dev/null; rm -rf "$WORK"' EXIT

cases=0
failures=0
skipped=0
OUT=""
STATUS=0

# new_case -> prints a fresh fixture directory containing an empty "root".
# It runs in a command substitution, so it cannot keep a counter -- mktemp gives
# each case its own directory and "check" does the counting in the main shell.
new_case() {
  local dir
  dir="$(mktemp -d "${WORK}/case-XXXXXX")"
  mkdir -p "${dir}/root"
  printf %s "$dir"
}

# run_checker SOURCE ROOT -> captures combined output in OUT and status in STATUS.
run_checker() {
  OUT="$(bash "$CHECKER" "$1" "$2" 2>&1)"
  STATUS=$?
}

# check NAME EXPECTED_STATUS [EXPECTED_TEXT]
check() {
  cases=$((cases + 1))
  local name="$1" expected_status="$2" expected_text="${3:-}" reason=""

  if [ "$STATUS" -ne "$expected_status" ]; then
    reason="expected exit ${expected_status}, got ${STATUS}"
  elif [ -n "$expected_text" ] && ! printf '%s\n' "$OUT" | grep -qF -- "$expected_text"; then
    reason="output did not contain: ${expected_text}"
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

# skip NAME REASON -- for a case whose precondition the platform will not honour.
skip() {
  skipped=$((skipped + 1))
  echo "SKIP: $1"
  echo "      $2"
}

# --- every reference resolves -------------------------------------------------
d="$(new_case)"
mkdir -p "${d}/root/specs"
touch "${d}/root/specs/present.md"
cat > "${d}/source.md" <<'INNER'
Read `/specs/present.md` before starting.
INNER
run_checker "${d}/source.md" "${d}/root"
check "every reference resolves" 0 "Checked 1 referenced path(s); 0 missing."

# --- a missing reference fails and is named -----------------------------------
d="$(new_case)"
mkdir -p "${d}/root/specs"
cat > "${d}/source.md" <<'INNER'
Read `/specs/gone.md` before starting.
INNER
run_checker "${d}/source.md" "${d}/root"
check "a missing reference fails and is named" 1 "/specs/gone.md"

# --- a placeholder is skipped, not resolved -----------------------------------
d="$(new_case)"
mkdir -p "${d}/root/specs"
touch "${d}/root/specs/present.md"
cat > "${d}/source.md" <<'INNER'
| page | `/docs/wiki/<Page-Name>.md` |
Read `/specs/present.md` before starting.
INNER
run_checker "${d}/source.md" "${d}/root"
check "a <placeholder> is skipped, not resolved" 0 "Checked 1 referenced path(s); 0 missing."

# --- every path on a line is checked, not just the first ----------------------
d="$(new_case)"
mkdir -p "${d}/root/specs"
touch "${d}/root/specs/present.md"
cat > "${d}/source.md" <<'INNER'
See `/specs/present.md` and also `/specs/second-gone.md` for details.
INNER
run_checker "${d}/source.md" "${d}/root"
check "every path on a line is checked, not just the first" 1 "/specs/second-gone.md"

# --- a closing backtick does not open a reference ------------------------------
# `dsh-rest-api`/src/main/java is a closed code span followed by prose. Only a
# path inside a span is a reference; "/src/main/java; see " is not one.
d="$(new_case)"
mkdir -p "${d}/root/specs"
touch "${d}/root/specs/present.md"
cat > "${d}/source.md" <<'INNER'
Controllers live under `dsh-rest-api`/src/main/java; see `/specs/present.md`.
INNER
run_checker "${d}/source.md" "${d}/root"
check "a closing backtick does not open a reference" 0 "Checked 1 referenced path(s); 0 missing."

# --- inline code that is not a path is not a reference -------------------------
d="$(new_case)"
mkdir -p "${d}/root/specs"
touch "${d}/root/specs/present.md"
cat > "${d}/source.md" <<'INNER'
Run `mvn -B install`, then read `/specs/present.md`.
INNER
run_checker "${d}/source.md" "${d}/root"
check "inline code that is not a path is not a reference" 0 "Checked 1 referenced path(s); 0 missing."

# --- extracting zero references is a failure ----------------------------------
d="$(new_case)"
cat > "${d}/source.md" <<'INNER'
Prose with no backtick-quoted absolute paths in it at all.
INNER
run_checker "${d}/source.md" "${d}/root"
check "extracting zero references is a failure" 1 "the check verified nothing"

# --- a failing extractor is a tool error, not an empty document ----------------
# A stub grep on PATH exits 2, as a grep without PCRE support would. That must
# not read as "this document contains no references".
d="$(new_case)"
mkdir -p "${d}/stub"
cat > "${d}/stub/grep" <<'INNER'
#!/usr/bin/env bash
exit 2
INNER
chmod +x "${d}/stub/grep"
cat > "${d}/source.md" <<'INNER'
Read `/specs/present.md` before starting.
INNER
OUT="$(PATH="${d}/stub:${PATH}" bash "$CHECKER" "${d}/source.md" "${d}/root" 2>&1)"
STATUS=$?
check "a failing extractor is a tool error, not an empty document" 1 "grep exited 2"

# --- a missing source file is a failure ---------------------------------------
d="$(new_case)"
run_checker "${d}/no-such-source.md" "${d}/root"
check "a missing source file is a failure" 1 "reference source not found"

# --- an unreadable source file is a failure -----------------------------------
# Skipped where chmod does not actually revoke read access (Git Bash on Windows,
# or running as root); the guard is exercised on the Linux runner.
d="$(new_case)"
cat > "${d}/source.md" <<'INNER'
Read `/specs/present.md` before starting.
INNER
chmod 000 "${d}/source.md"
if [ -r "${d}/source.md" ]; then
  skip "an unreadable source file is a failure" "chmod does not revoke read access here"
else
  run_checker "${d}/source.md" "${d}/root"
  check "an unreadable source file is a failure" 1 "reference source not readable"
fi
chmod 644 "${d}/source.md"

# --- a directory reference resolves -------------------------------------------
d="$(new_case)"
mkdir -p "${d}/root/docs/wiki"
cat > "${d}/source.md" <<'INNER'
Wiki copies live under `/docs/wiki/` in this repository.
INNER
run_checker "${d}/source.md" "${d}/root"
check "a directory reference resolves" 0 "Checked 1 referenced path(s); 0 missing."

# --- summary ------------------------------------------------------------------
echo
if [ "$failures" -gt 0 ]; then
  echo "${failures} of ${cases} case(s) failed."
  exit 1
fi

if [ "$skipped" -gt 0 ]; then
  echo "All ${cases} case(s) passed; ${skipped} skipped."
else
  echo "All ${cases} case(s) passed."
fi

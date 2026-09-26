#!/usr/bin/env bash
#
# Tests for check-unit-tests-context-free.sh.
#
# Plain bash: no Node, no Python, no test framework. Each case builds a fixture
# tree under a temporary directory, runs the checker against it as ROOT, and
# asserts the exit code and -- where the message is the point -- that the output
# names the offending file.
#
# Usage: check-unit-tests-context-free.test.sh
#
# Exits non-zero if any case fails.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKER="${SCRIPT_DIR}/check-unit-tests-context-free.sh"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cases=0
failures=0
skipped=0
OUT=""
STATUS=0

# new_case -> prints a fresh fixture root containing a module's test source dir.
new_case() {
  local dir
  dir="$(mktemp -d "${WORK}/case-XXXXXX")"
  mkdir -p "${dir}/mod/src/test/java/com/example"
  printf %s "$dir"
}

# write ROOT FILE_NAME -> writes stdin to the fixture module's test package.
write() {
  cat > "$1/mod/src/test/java/com/example/$2"
}

# write_integration ROOT FILE_NAME -> writes stdin to the fixture module's
# integration package, the only place an *IT is exempt.
write_integration() {
  mkdir -p "$1/mod/src/test/java/com/example/integration"
  cat > "$1/mod/src/test/java/com/example/integration/$2"
}

# add_clean_unit_test ROOT -> gives the fixture one context-free unit test, so
# a case about what is excluded still has something to check: a tree with no
# unit test at all is its own failure.
add_clean_unit_test() {
  printf 'public class CleanTest {\n}\n' | write "$1" CleanTest.java
}

# skip NAME REASON -- for a case whose precondition the platform will not honour.
skip() {
  skipped=$((skipped + 1))
  echo "SKIP: $1"
  echo "      $2"
}

# run_checker ROOT -> captures combined output in OUT and status in STATUS.
run_checker() {
  OUT="$(bash "$CHECKER" "$1" 2>&1)"
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

SPRING_RUNNER_TEST='import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class Foo {
}'

# --- 1. a Mockito unit test is clean ------------------------------------------
d="$(new_case)"
write "$d" FooTest.java <<'INNER'
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FooTest {
}
INNER
run_checker "$d"
check "a Mockito unit test is clean" 0

# --- 2. SpringRunner in a *Test is a violation --------------------------------
d="$(new_case)"
printf '%s\n' "$SPRING_RUNNER_TEST" | write "$d" FooTest.java
run_checker "$d"
check "SpringRunner in a unit test is a violation" 1 "FooTest.java"

# --- 3. the same content in a *IT in an integration package is allowed --------
d="$(new_case)"
printf '%s\n' "$SPRING_RUNNER_TEST" | write_integration "$d" FooIT.java
add_clean_unit_test "$d"
run_checker "$d"
check "SpringRunner in an integration *IT is allowed" 0

# --- 4. ... and in a *IntegrationTest in an integration package ---------------
d="$(new_case)"
printf '%s\n' "$SPRING_RUNNER_TEST" | write_integration "$d" FooIntegrationTest.java
add_clean_unit_test "$d"
run_checker "$d"
check "SpringRunner in an integration *IntegrationTest is allowed" 0

# --- 4b. a *IT outside an integration package is not exempt -------------------
# The rule is name *and* package: a misplaced *IT that starts a context is flagged.
d="$(new_case)"
printf '%s\n' "$SPRING_RUNNER_TEST" | write "$d" FooIT.java
run_checker "$d"
check "SpringRunner in a *IT outside an integration package is a violation" 1 "FooIT.java"

# --- 5. a @MockBean import alone is a violation -------------------------------
d="$(new_case)"
write "$d" FooTest.java <<'INNER'
import org.springframework.boot.test.mock.mockito.MockBean;

public class FooTest {
}
INNER
run_checker "$d"
check "a @MockBean import is a violation" 1 "FooTest.java"

# --- 6. webAppContextSetup is a violation; standaloneSetup is not -------------
d="$(new_case)"
write "$d" FooTest.java <<'INNER'
public class FooTest {
    Object mvc = MockMvcBuilders.webAppContextSetup(context).build();
}
INNER
run_checker "$d"
check "webAppContextSetup is a violation" 1 "webAppContextSetup"

d="$(new_case)"
write "$d" FooTest.java <<'INNER'
public class FooTest {
    Object mvc = MockMvcBuilders.standaloneSetup(new Object()).build();
}
INNER
run_checker "$d"
check "standaloneSetup is allowed" 0

# --- 7. a hand-built application context is a violation -----------------------
d="$(new_case)"
write "$d" FooTest.java <<'INNER'
public class FooTest {
    Object ctx = new AnnotationConfigApplicationContext(Object.class);
}
INNER
run_checker "$d"
check "a hand-built application context is a violation" 1 "AnnotationConfigApplicationContext"

# --- 8. context-free Spring test helpers are allowed --------------------------
d="$(new_case)"
write "$d" FooTest.java <<'INNER'
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

public class FooTest {
}
INNER
run_checker "$d"
check "MockMultipartFile and ReflectionTestUtils are allowed" 0

# --- 9. build output under target/ is ignored ---------------------------------
d="$(new_case)"
mkdir -p "${d}/mod/target/src/test/java/com/example"
printf '%s\n' "$SPRING_RUNNER_TEST" > "${d}/mod/target/src/test/java/com/example/FooTest.java"
add_clean_unit_test "$d"
run_checker "$d"
check "a violation under target/ is ignored" 0

# --- 10. a tree with no unit test is a failure --------------------------------
# A layout change that stops the search matching would otherwise leave CI green
# while checking nothing.
d="$(new_case)"
run_checker "$d"
check "a tree with no unit test is a failure" 1 "No unit test sources found"

# --- 11. a search that fails is a failure, not a pass -------------------------
# Skipped where chmod does not actually revoke read access (Git Bash on Windows,
# or running as root); the guard is exercised on the Linux runner.
d="$(new_case)"
add_clean_unit_test "$d"
unreadable="${d}/mod/src/test/java/com/example/UnreadableTest.java"
printf 'public class UnreadableTest {\n}\n' > "$unreadable"
chmod 000 "$unreadable"
if [ -r "$unreadable" ]; then
  skip "a search that fails is a failure" "chmod does not revoke read access here"
else
  run_checker "$d"
  check "a search that fails is a failure" 2 "the search failed"
fi
chmod 644 "$unreadable"

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

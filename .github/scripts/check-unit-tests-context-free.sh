#!/usr/bin/env bash
#
# Verify that no unit test starts a Spring context.
#
# The rule (.github/copilot/rules/testing-patterns.md, "Unit vs integration"):
# a unit test never starts a Spring context. A test that starts one -- full,
# sliced, or hand-built -- is an integration test, named *IT and placed in an
# integration package, where surefire does not run it and the coverage gate does
# not count it.
#
# Scope: every *.java under a src/test/java directory, except *IT.java and
# *IntegrationTest.java, outside target/.
#
# Signal: a reference to a package whose purpose is to build, bootstrap or
# populate a test context --
#   org.springframework.test.context            SpringRunner, @ContextConfiguration, ...
#   org.springframework.boot.test.context       @SpringBootTest, @TestConfiguration
#   org.springframework.boot.test.autoconfigure every slice: @WebMvcTest, @DataMongoTest, ...
#   org.springframework.boot.test.mock.mockito  @MockBean, @SpyBean
# plus webAppContextSetup (the MockMvc builder that needs a WebApplicationContext)
# and a hand-built context, "new <Something>ApplicationContext(".
#
# Context-free helpers -- org.springframework.mock.web, the other MockMvc
# builders, org.springframework.test.util -- build nothing and are allowed.
#
# This is a proxy: it detects the code that starts a context, not a context
# actually starting. Anything subtler is for review.
#
# Usage: check-unit-tests-context-free.sh [ROOT]
#
# ROOT defaults to the repository root. Exits 1 naming each offending file, line
# and matched token; exits 0 when every unit test is context-free.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${1:-${SCRIPT_DIR}/../..}" && pwd)"

PATTERN='org\.springframework\.test\.context|org\.springframework\.boot\.test\.(context|autoconfigure|mock\.mockito)|webAppContextSetup|new [A-Za-z]*ApplicationContext\('

mapfile -d '' FILES < <(find "$ROOT" -path '*/target' -prune -o -path '*/src/test/java/*' -name '*.java' \
    ! -name '*IT.java' ! -name '*IntegrationTest.java' -print0)

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "No unit test sources found under ${ROOT}."
  exit 0
fi

# grep is called directly, not through xargs: xargs folds grep's "no match" (1)
# and "error" (2) into the same 123. Here 1 is the passing case, and any higher
# status is grep itself failing, which must not read as a pass.
set +e
MATCHES="$(grep -EnH "$PATTERN" -- "${FILES[@]}")"
STATUS=$?
set -e

if [ "$STATUS" -gt 1 ]; then
  echo "ERROR: the search failed (exit ${STATUS}); nothing was checked."
  exit "$STATUS"
fi

if [ -n "$MATCHES" ]; then
  echo "Unit tests must not start a Spring context. Rename to *IT and move to an integration package, or remove the context:"
  printf '%s\n' "$MATCHES"
  exit 1
fi

echo "Every unit test is free of a Spring context."

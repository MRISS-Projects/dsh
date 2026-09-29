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
# Scope: every *.java under a src/test/java directory, outside target/, except
# an *IT.java or *IntegrationTest.java inside an integration package. The rule is
# name and package together, so an *IT placed anywhere else is checked like a
# unit test, and flagged if it starts a context.
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
# actually starting. Anything subtler is for review. Known gaps a reviewer should
# look for, because no token can tell them from legitimate code:
#   - an unmocked SpringApplication.run(...) -- the worker unit tests contain the
#     same call inside Mockito.mockStatic(SpringApplication.class), where it
#     starts nothing;
#   - new SpringApplicationBuilder(...).run(...) -- a unit test may build one as
#     an argument without running it.
#
# Usage: check-unit-tests-context-free.sh [ROOT]
#
# ROOT defaults to the repository root. Exits 0 when every unit test is
# context-free; 1 naming each offending file, line and matched token, or when no
# unit test source is found at all; 2 when the search itself fails.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${1:-${SCRIPT_DIR}/../..}" && pwd)"

PATTERN='org\.springframework\.test\.context|org\.springframework\.boot\.test\.(context|autoconfigure|mock\.mockito)|webAppContextSetup|new [A-Za-z]*ApplicationContext\('

# The list goes through a file, not a process substitution, so that find failing
# (an unreadable directory, say) is seen rather than yielding a partial list.
LIST="$(mktemp)"
trap 'rm -f "$LIST"' EXIT

if ! find "$ROOT" -path '*/target' -prune -o -path '*/src/test/java/*' -name '*.java' \
    ! \( -path '*/integration/*' \( -name '*IT.java' -o -name '*IntegrationTest.java' \) \) \
    -print0 > "$LIST"; then
  echo "ERROR: listing the test sources failed; nothing was checked."
  exit 2
fi
mapfile -d '' FILES < "$LIST"

# A run that checked nothing is a failure, not a pass: a layout change that stops
# the search matching would otherwise leave this gate green and vacuous.
if [ "${#FILES[@]}" -eq 0 ]; then
  echo "ERROR: No unit test sources found under ${ROOT}; nothing was checked."
  exit 1
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

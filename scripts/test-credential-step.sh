#!/usr/bin/env bash
# Tests the "Verify package credentials work" step of a workflow (issue #101).
#
# ============================================================================
# NOT RUN AUTOMATICALLY. Run it by hand after editing that step in either
# workflow, or when reviewing a change to it:
#     ./scripts/test-credential-step.sh .github/workflows/ci.yml
#     ./scripts/test-credential-step.sh .github/workflows/api-testing.yml
# No CI job invokes it. The change it guards is a shell step in a workflow
# file, so there is no unit test to hang it on - this script is the whole
# regression net. See specs/stories/101-fail-ci-on-unusable-package-token.md.
# ============================================================================
#
# Two decisions make this a test of the shipped step rather than of a copy:
#
#   1. It extracts the `run:` body FROM the workflow file and executes that, so
#      nothing can pass while the YAML says something else. The extractor
#      matches the step-name prefix, so it finds a renamed step too.
#   2. It runs that body under `bash -e`, which is the shell GitHub Actions
#      gives a `run:` block. Without it the `|| code="000"` guard would appear
#      to work when it does not.
#
# Statuses the live registry will not hand a machine without a working
# credential - 2xx, 3xx, 5xx, and a curl that exits non-zero - come from a stub
# `curl` prepended to PATH. The 401 cases use the real registry, so they need
# network access; they are the only cases that do.

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKFLOW="${1:-$REPO/.github/workflows/ci.yml}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PASS=0
FAIL=0

mkdir -p "$WORK/stub" "$WORK/nopom" "$WORK/commented"

awk '
  /- name: Verify package credentials/ { found = 1; next }
  found && /run: \|/                   { inrun = 1; next }
  inrun {
    if ($0 ~ /^[[:space:]]*$/)    { print ""; next }
    if (match($0, /^          /)) { print substr($0, 11); next }
    exit
  }
' "$WORKFLOW" > "$WORK/step.sh"

if [ ! -s "$WORK/step.sh" ]; then
  echo "FATAL: could not extract a step body from $WORKFLOW"
  exit 2
fi
echo "Extracted $(wc -l < "$WORK/step.sh") lines of step body from $WORKFLOW"
echo

# A pom.xml with no <parent> block, for the coordinates guard.
printf '<project>\n  <artifactId>orphan</artifactId>\n</project>\n' > "$WORK/nopom/pom.xml"

# A pom.xml whose live <parent> is preceded by a commented-out one, and whose
# live block contains a commented-out <version>. Reading either commented value
# builds a URL for an artifact that does not exist, which a working token
# answers with 404 - so the check would warn and pass forever while testing
# nothing. The root pom.xml already carries a comment directly above <parent>,
# and Wave 0 closes by editing that very version.
cat > "$WORK/commented/pom.xml" <<'POM'
<project>
  <!-- <parent>
    <groupId>old.group</groupId>
    <artifactId>old-artifact</artifactId>
    <version>1.0.0</version>
  </parent> -->
  <parent>
    <groupId>com.mriss.mriss-parent</groupId>
    <artifactId>products</artifactId>
    <!-- <version>3.7.0-SNAPSHOT</version> -->
    <version>3.8.0-SNAPSHOT</version>
  </parent>
</project>
POM

# Prints the status code the real curl prints with -w '%{http_code}'.
# STUB_FAIL=1 exits non-zero without printing, as a connection failure does.
cat > "$WORK/stub/curl" <<'STUB'
#!/usr/bin/env bash
if [ "$STUB_FAIL" = "1" ]; then exit 7; fi
printf '%s' "$STUB_CODE"
STUB
chmod +x "$WORK/stub/curl"

# run_case <name> <cwd> <expected-exit> <expected-substring>
# Env in: PACKAGES_READ_TOKEN, STUB_CODE, STUB_FAIL, USE_STUB
run_case() {
  name="$1"; cwd="$2"; want_exit="$3"; want_text="$4"
  if [ "$USE_STUB" = "1" ]; then
    out=$(cd "$cwd" && PATH="$WORK/stub:$PATH" bash -e "$WORK/step.sh" 2>&1)
  else
    out=$(cd "$cwd" && bash -e "$WORK/step.sh" 2>&1)
  fi
  got_exit=$?

  if [ "$got_exit" = "$want_exit" ] && printf '%s' "$out" | grep -qF "$want_text"; then
    echo "  PASS  $name"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  $name"
    echo "          expected exit $want_exit containing: $want_text"
    echo "          got exit $got_exit: ${out:-<no output>}"
    FAIL=$((FAIL + 1))
  fi
  unset STUB_CODE STUB_FAIL USE_STUB
}

export GITHUB_ACTOR="test-actor"

echo "Against the live registry (needs network):"

PACKAGES_READ_TOKEN="" \
  run_case "absent secret fails and blames the fork, not the token" \
    "$REPO" 1 "PACKAGES_READ_TOKEN is unavailable to this run"

PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" \
  run_case "invalid token fails" \
    "$REPO" 1 "cannot read com.mriss.mriss-parent:products"

PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" \
  run_case "invalid token says the secret IS present" \
    "$REPO" 1 "this is not a missing-secret problem"

PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" \
  run_case "invalid token lists the causes" \
    "$REPO" 1 "has lost the read:packages scope"

PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" \
  run_case "invalid token points at the documentation" \
    "$REPO" 1 "docs/devops/README.md"

PACKAGES_READ_TOKEN="anything" \
  run_case "unreadable <parent> fails rather than probing a malformed URL" \
    "$WORK/nopom" 1 "Could not read the <parent> coordinates"

echo
echo "POM parsing, with the probe stubbed out:"

USE_STUB=1 STUB_CODE=200 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "a commented-out <parent> is ignored in favour of the live one" \
    "$WORK/commented" 0 "can read com.mriss.mriss-parent:products"

USE_STUB=1 STUB_CODE=200 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "a commented-out <version> inside the live block is ignored too" \
    "$WORK/commented" 0 "(parent 3.8.0-SNAPSHOT)"

echo
echo "Against a stubbed curl, for statuses the live registry cannot return without a working credential:"

USE_STUB=1 STUB_CODE=200 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "200 passes and names the artifact it reached" \
    "$REPO" 0 "PACKAGES_READ_TOKEN can read com.mriss.mriss-parent:products (parent 3.8.0-SNAPSHOT)"

USE_STUB=1 STUB_CODE=403 PACKAGES_READ_TOKEN="pretend-forbidden" \
  run_case "403 fails like 401" \
    "$REPO" 1 "cannot read com.mriss.mriss-parent:products from GitHub Packages (HTTP 403)"

USE_STUB=1 STUB_CODE=404 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "404 warns and passes, because it proves the credential authenticated" \
    "$REPO" 0 "The credential authenticated"

USE_STUB=1 STUB_CODE=404 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "404 names the stale URL so it can be fixed" \
    "$REPO" 0 "products/maven-metadata.xml returned 404"

USE_STUB=1 STUB_CODE=302 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "302 passes: a redirect proves the credential authenticated, same as 404" \
    "$REPO" 0 "PACKAGES_READ_TOKEN can read com.mriss.mriss-parent:products"

USE_STUB=1 STUB_CODE=500 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "500 warns and passes rather than inventing a flaky gate" \
    "$REPO" 0 "Could not verify PACKAGES_READ_TOKEN"

USE_STUB=1 STUB_FAIL=1 PACKAGES_READ_TOKEN="pretend-valid" \
  run_case "a curl that exits non-zero does not abort the step under bash -e" \
    "$REPO" 0 "(HTTP 000)"

echo
echo "AC004 - cost of the step:"
start=$(date +%s%N)
( cd "$REPO" && PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" bash -e "$WORK/step.sh" >/dev/null 2>&1 )
elapsed=$(( ($(date +%s%N) - start) / 1000000 ))
if [ "$elapsed" -lt 5000 ]; then
  echo "  PASS  step completed in ${elapsed} ms (< 5000 ms)"
  PASS=$((PASS + 1))
else
  echo "  FAIL  step took ${elapsed} ms"
  FAIL=$((FAIL + 1))
fi

echo
echo "================================"
echo "  $PASS passed, $FAIL failed"
echo "================================"
[ "$FAIL" -eq 0 ]

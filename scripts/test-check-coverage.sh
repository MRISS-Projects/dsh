#!/usr/bin/env bash
# Fixture-driven tests for check-coverage.sh
set -uo pipefail
cd "$(dirname "$0")/.."
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
fail=0

# JaCoCo CSV header, then two packages: 300 missed, 700 covered => 70.00%
cat > "$TMP/jacoco.csv" <<'CSV'
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
dsh,com.mriss.a,A,100,400,0,0,0,0,0,0,0,0
dsh,com.mriss.b,B,200,300,0,0,0,0,0,0,0,0
CSV

check() { # name expected_exit baseline
  echo "$3" > "$TMP/baseline.txt"
  out=$(./scripts/check-coverage.sh "$TMP/jacoco.csv" "$TMP/baseline.txt"); rc=$?
  if [ "$rc" -ne "$2" ]; then
    echo "FAIL: $1 (exit $rc, wanted $2) :: $out"; fail=1
  else
    echo "PASS: $1"
  fi
}

check "equal to baseline passes"   0 "70.00"
check "above baseline passes"      0 "65.00"
check "below baseline fails"       1 "75.00"

# Missing baseline file bootstraps and passes
rm -f "$TMP/baseline.txt"
out=$(./scripts/check-coverage.sh "$TMP/jacoco.csv" "$TMP/baseline.txt"); rc=$?
if [ "$rc" -eq 0 ] && echo "$out" | grep -q "bootstrap"; then
  echo "PASS: missing baseline bootstraps"
else
  echo "FAIL: missing baseline bootstraps (exit $rc) :: $out"; fail=1
fi

# Missing CSV is a hard error, not a silent pass
out=$(./scripts/check-coverage.sh "$TMP/nope.csv" "$TMP/baseline.txt" 2>&1); rc=$?
if [ "$rc" -eq 2 ]; then echo "PASS: missing csv errors"; else
  echo "FAIL: missing csv errors (exit $rc) :: $out"; fail=1; fi

# A malformed baseline must fail closed (exit 2), never silently pass.
# The blank case is the one that used to slip through: an empty string made awk do a
# string comparison that always looked like a pass.
bad_baseline() { # name file-content
  printf '%s' "$2" > "$TMP/baseline.txt"
  out=$(./scripts/check-coverage.sh "$TMP/jacoco.csv" "$TMP/baseline.txt" 2>&1); rc=$?
  if [ "$rc" -eq 2 ]; then
    echo "PASS: $1"
  else
    echo "FAIL: $1 (exit $rc, wanted 2) :: $out"; fail=1
  fi
}

bad_baseline "blank baseline fails closed"        ""
bad_baseline "whitespace baseline fails closed"   "   "
bad_baseline "non-numeric baseline fails closed"  "garbage"
bad_baseline "over-100 baseline fails closed"     "150"
bad_baseline "negative baseline fails closed"     "-5"

exit $fail

#!/usr/bin/env bash
# Coverage ratchet: fail if aggregate INSTRUCTION coverage drops below the
# committed baseline. Deliberately implemented outside the poms so that
# release builds inheriting from com.mriss.mriss-parent:products are unaffected.
#
# Usage: check-coverage.sh <jacoco-csv> <baseline-file>
set -uo pipefail

CSV="${1:-dsh-coverage-report/target/site/jacoco-aggregate/jacoco.csv}"
BASELINE_FILE="${2:-.github/coverage-baseline.txt}"

if [ ! -f "$CSV" ]; then
  echo "ERROR: JaCoCo CSV not found at '$CSV'." >&2
  echo "The aggregate report is produced by jacoco:report-aggregate, bound to" >&2
  echo "the 'verify' phase in dsh-coverage-report/pom.xml. Run 'mvn -B install'." >&2
  exit 2
fi

# Columns 4 and 5 are INSTRUCTION_MISSED and INSTRUCTION_COVERED.
current=$(awk -F, 'NR>1 {m+=$4; c+=$5} END {
  if (m+c == 0) printf "0.00"; else printf "%.2f", 100*c/(m+c)
}' "$CSV")

if [ ! -f "$BASELINE_FILE" ]; then
  echo "bootstrap: no baseline at '$BASELINE_FILE'; current=$current"
  echo "Commit this value to establish the ratchet:"
  echo "  echo $current > $BASELINE_FILE"
  exit 0
fi

baseline=$(tr -d '[:space:]' < "$BASELINE_FILE")
echo "current=$current baseline=$baseline"

if awk -v c="$current" -v b="$baseline" 'BEGIN { exit !(c + 0.005 < b) }'; then
  echo "FAIL: coverage dropped from $baseline% to $current%." >&2
  echo "Add tests, or justify the drop and update $BASELINE_FILE deliberately." >&2
  exit 1
fi

echo "OK: coverage $current% >= baseline $baseline%"
exit 0

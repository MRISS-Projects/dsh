#!/usr/bin/env bash
#
# Verify that every repository path referenced in .github/copilot-instructions.md
# resolves to a file or directory that exists.
#
# A reference is a path beginning with "/" inside a backtick code span,
# interpreted relative to the repository root. Spans are matched as pairs, so a
# closing backtick cannot open a reference: in "`dsh-rest-api`/src/main/java"
# the path is prose, not a reference. Inline code that is not a path is ignored,
# and a path containing "<" or ">" is a documentation placeholder, not a
# reference, and is skipped.
#
# Usage: check-spec-references.sh [SOURCE_FILE] [REPO_ROOT]
#
# Exits non-zero when a reference does not resolve, when SOURCE_FILE cannot be
# read, when the extractor itself fails, or when no references are extracted at
# all -- a run that verified nothing is a failure, not a pass.
#
# Note: this check runs from .github/workflows/spec-validation.yml, whose
# triggers are path-scoped. Every path referenced from the source file currently
# lives under one of those trigger paths, so deleting one always runs this job.
# A reference to a path outside them would not, and the gate would go quiet.

set -euo pipefail

SOURCE="${1:-.github/copilot-instructions.md}"
ROOT="${2:-.}"

if [ ! -f "$SOURCE" ]; then
  echo "ERROR: reference source not found: $SOURCE"
  exit 1
fi

if [ ! -r "$SOURCE" ]; then
  echo "ERROR: reference source not readable: $SOURCE"
  exit 1
fi

# grep exits 1 on no matches, which the zero-reference guard below reports. Any
# higher status is the extractor failing -- no PCRE support, an unreadable file
# -- and must not be allowed to read as "this document contains no references".
if spans="$(grep -oP '`[^`]+`' "$SOURCE")"; then
  grep_status=0
else
  grep_status=$?
fi

if [ "$grep_status" -gt 1 ]; then
  echo "ERROR: could not extract references from $SOURCE; grep exited $grep_status."
  exit 1
fi

total=0
missing=0

while IFS= read -r span; do
  ref="${span:1:${#span}-2}"
  case "$ref" in
    /*) ;;
    *) continue ;;
  esac
  case "$ref" in
    *"<"* | *">"*) continue ;;
  esac
  total=$((total + 1))
  if [ ! -e "${ROOT}${ref}" ]; then
    echo "ERROR: referenced file not found: $ref"
    missing=$((missing + 1))
  fi
done <<< "$spans"

if [ "$total" -eq 0 ]; then
  echo "ERROR: extracted no references from $SOURCE; the check verified nothing."
  exit 1
fi

echo "Checked $total referenced path(s); $missing missing."

if [ "$missing" -gt 0 ]; then
  exit 1
fi

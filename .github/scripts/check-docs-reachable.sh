#!/usr/bin/env bash
#
# Verify that every Markdown document under docs/, specs/ and .github/ can be
# reached from CLAUDE.md by following references.
#
# The walk starts at CLAUDE.md. It reads every tracked Markdown file it reaches,
# inside those three directories or not, and follows two kinds of reference:
#
#   - the target of an inline Markdown link: [text](target)
#   - an inline code span on one line:        `target`
#
# A "#anchor" is stripped from a target. A target starting with "/" is relative
# to the repository root; any other is tried relative to the directory of the
# file that names it, then relative to the root. A target naming a directory
# reaches the tracked .md files directly inside it, not those in subdirectories.
# A target that names no tracked Markdown file or directory -- a command, a URL,
# a placeholder, a file that no longer exists -- reaches nothing and is ignored:
# broken links are not this check's concern. Nothing else counts: not a
# reference-style link ([text][label]), not a target in angle brackets
# (<target>), not a percent-encoded path (my%20guide.md).
#
# A story spec, any file under specs/stories/, is reached like any other
# document, but it is not read. It is a record of what a story did, kept as
# written, so a path it names reaches nothing: a document that only an old
# story names is a document nothing current points at.
#
# Only tracked files count. The list comes from git ls-files, so a local run
# answers as CI does, whatever untracked files lie in the work tree.
#
# There are no exemptions. A document that should pass gets a reference from a
# document that is reached.
#
# Usage: check-docs-reachable.sh [REPO_ROOT]
#
# REPO_ROOT is any directory inside the work tree; the whole repository is
# checked. Exits non-zero when a document is not reached, when REPO_ROOT is not
# in a git work tree, when CLAUDE.md is not tracked at the root, when a file
# cannot be read, or when there are no documents to check at all -- a run that
# verified nothing is a failure, not a pass.

set -euo pipefail

ROOT="${1:-.}"
SCOPE_LABEL="docs/, specs/ and .github/"
RECORDS="specs/stories/"   # reached, never read: see the header

if ! top="$(git -C "$ROOT" rev-parse --show-toplevel 2>/dev/null)"; then
  echo "ERROR: $ROOT is not in a git work tree."
  exit 1
fi
cd "$top"

# NUL-separated, so that no name is quoted: without -z, git wraps a name holding
# a double quote, a backslash or a control character in quotes, and the quoted
# form would fall outside every scope prefix unnoticed. A variable cannot hold a
# NUL, so the list goes through a file.
listing="$(mktemp)"
trap 'rm -f "$listing"' EXIT
if ! git ls-files -z -- '*.md' > "$listing"; then
  echo "ERROR: git ls-files failed in $top."
  exit 1
fi

declare -A doc=()     # every tracked Markdown file -> 1
declare -A dirmd=()   # a directory -> the tracked .md files directly inside it
declare -A seen=()    # every file the walk has reached -> 1
queue=()

while IFS= read -r -d '' path; do
  [ -n "$path" ] || continue
  doc[$path]=1
  if [[ "$path" == */* ]]; then
    dirmd[${path%/*}]+="$path"$'\n'
  fi
done < "$listing"

if [ -z "${doc[CLAUDE.md]+x}" ]; then
  echo "ERROR: CLAUDE.md is not tracked at the repository root."
  exit 1
fi

# normalise PATH -> sets REPLY to PATH with "." and ".." resolved and no empty
# segment. A ".." above the root is dropped. No subprocess: this runs once per
# candidate path, and a fork per call is slow on Windows.
normalise() {
  local -a parts=() out=()
  local part
  IFS=/ read -r -a parts <<< "$1"
  for part in "${parts[@]}"; do
    case "$part" in
      '' | .) ;;
      ..) if [ "${#out[@]}" -gt 0 ]; then unset 'out[-1]'; fi ;;
      *) out+=("$part") ;;
    esac
  done
  local IFS=/
  REPLY="${out[*]}"
}

# visit FILE -> queues FILE unless the walk has reached it already.
visit() {
  if [ -z "${seen[$1]+x}" ]; then
    seen[$1]=1
    queue+=("$1")
  fi
}

# follow FROM TARGET -> visits what TARGET reaches when the file FROM names it.
follow() {
  local from="$1" target="${2%%#*}" dir="" candidate child
  local -a candidates=()
  [ -n "$target" ] || return 0
  # A URL is never a path: one with a scheme (https:, mailto:) or a
  # protocol-relative one (//host). Rejected before normalising, or its ".."
  # segments could climb back to a tracked path.
  if [[ "$target" =~ ^[A-Za-z][A-Za-z0-9+.-]*: || "$target" == //* ]]; then
    return 0
  fi
  if [ "${target:0:1}" = / ]; then
    candidates=("$target")
  else
    if [[ "$from" == */* ]]; then dir="${from%/*}"; fi
    candidates=("$dir/$target" "$target")
  fi
  for candidate in "${candidates[@]}"; do
    normalise "$candidate"
    [ -n "$REPLY" ] || continue
    if [ -n "${doc[$REPLY]+x}" ]; then
      visit "$REPLY"
      return 0
    fi
    if [ -n "${dirmd[$REPLY]+x}" ]; then
      while IFS= read -r child; do
        if [ -n "$child" ]; then visit "$child"; fi
      done <<< "${dirmd[$REPLY]}"
      return 0
    fi
  done
  return 0
}

visit CLAUDE.md
i=0
while [ "$i" -lt "${#queue[@]}" ]; do
  file="${queue[$i]}"
  i=$((i + 1))
  case "$file" in
    "$RECORDS"*) continue ;;
  esac
  # A link target, or a code span matched as a pair so that a closing backtick
  # cannot open the next span. grep exits 1 on no matches; anything higher is
  # the extractor failing and must not read as "this file references nothing".
  # -a: a file grep takes for binary, a NUL byte in it, is still read as text,
  # instead of yielding "Binary file matches" and none of its references.
  if refs="$(grep -a -oP '\]\([^)\s]+|`[^`]+`' -- "$file")"; then
    :
  else
    status=$?
    if [ "$status" -gt 1 ]; then
      echo "ERROR: could not read references from $file; grep exited $status."
      exit 1
    fi
    continue
  fi
  while IFS= read -r ref; do
    case "$ref" in
      ']('*) follow "$file" "${ref:2}" ;;
      '`'*) follow "$file" "${ref:1:${#ref}-2}" ;;
    esac
  done <<< "$refs"
done

total=0
unreached=()
for path in "${!doc[@]}"; do
  case "$path" in
    docs/* | specs/* | .github/*) ;;
    *) continue ;;
  esac
  total=$((total + 1))
  if [ -z "${seen[$path]+x}" ]; then
    unreached+=("$path")
  fi
done

if [ "$total" -eq 0 ]; then
  echo "ERROR: no Markdown documents under $SCOPE_LABEL; the check verified nothing."
  exit 1
fi

echo "Reached $((total - ${#unreached[@]})) of $total documents under $SCOPE_LABEL from CLAUDE.md."

if [ "${#unreached[@]}" -gt 0 ]; then
  echo "ERROR: ${#unreached[@]} document(s) cannot be reached from CLAUDE.md:"
  printf '%s\n' "${unreached[@]}" | LC_ALL=C sort | sed 's/^/  /'
  echo "Reference each one from a document that is reached -- a Markdown link, a path in"
  echo "backticks, or a path to its directory -- or delete it if nothing needs it."
  exit 1
fi

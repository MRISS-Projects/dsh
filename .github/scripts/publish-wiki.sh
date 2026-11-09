#!/usr/bin/env bash
#
# Mirror the wiki's source, docs/wiki/, into a clone of the wiki's repository.
#
# Every top-level *.md file in SOURCE_DIR except README.md is a page, and is
# copied to WIKI_DIR under the same name. A top-level *.md file in WIKI_DIR with
# no page of the same name in SOURCE_DIR is deleted, so removing a file from
# docs/wiki removes its page. Anything else in WIKI_DIR -- .git, files that are
# not Markdown -- is left alone. README.md describes the source directory and is
# never a page.
#
# Names are compared as the filesystem compares them. On a case-insensitive one,
# Windows' included, a wiki page whose name differs from its source only in case
# is overwritten in place and keeps the wiki's case; on Linux, where the
# workflow runs, it is removed and published under the source's name.
#
# Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR
#
# Exits 2 on a wrong number of arguments, and 1 when either directory is missing,
# when both name the same directory, when WIKI_DIR is not a clone (it holds no
# .git), or when SOURCE_DIR holds no page at all: mirroring an empty source would
# delete every page of the wiki. The two guards on WIKI_DIR keep a mistyped
# argument from deleting the Markdown of whatever directory it names.

set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR"
  exit 2
fi

SOURCE="$1"
WIKI="$2"

for dir in "$SOURCE" "$WIKI"; do
  if [ ! -d "$dir" ]; then
    echo "ERROR: not a directory: $dir"
    exit 1
  fi
done

if [ "$(cd "$SOURCE" && pwd -P)" = "$(cd "$WIKI" && pwd -P)" ]; then
  echo "ERROR: SOURCE_DIR and WIKI_DIR are the same directory: $SOURCE"
  exit 1
fi

if [ ! -e "$WIKI/.git" ]; then
  echo "ERROR: not a clone of a repository: $WIKI"
  exit 1
fi

shopt -s nullglob

pages=()
for f in "$SOURCE"/*.md; do
  [ "$(basename "$f")" = "README.md" ] && continue
  pages+=("$f")
done

if [ "${#pages[@]}" -eq 0 ]; then
  echo "ERROR: no page in $SOURCE; refusing to empty the wiki."
  exit 1
fi

for f in "$WIKI"/*.md; do
  name="$(basename "$f")"
  if [ "$name" = "README.md" ] || [ ! -f "$SOURCE/$name" ]; then
    rm -- "$f"
    echo "Removed: $name"
  fi
done

for f in "${pages[@]}"; do
  cp -- "$f" "$WIKI/"
  echo "Published: $(basename "$f")"
done

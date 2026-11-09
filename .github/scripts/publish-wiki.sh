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
# Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR
#
# Exits 2 on a wrong number of arguments, and 1 when either directory is missing
# or SOURCE_DIR holds no page at all: mirroring an empty source would delete
# every page of the wiki.

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

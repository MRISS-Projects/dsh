# docs/wiki — Synced Wiki Pages

This directory contains Markdown copies of the [DSH GitHub Wiki](https://github.com/MRISS-Projects/dsh/wiki) pages.

## Purpose

Wiki pages are automatically synced here so that they become part of the repository's
`@workspace` context for GitHub Copilot. This means you can reference them in prompts
just like any other spec or documentation file:

```
@workspace Based on /docs/wiki/Architecture-Decisions.md and
/specs/features/document-analysis.md, explain how the analyser module fits
into the overall architecture.
```

## How sync works

The `.github/workflows/wiki-sync.yml` workflow runs daily at 02:00 UTC (and can be
triggered manually via **Actions → Wiki Sync → Run workflow**). It:

1. Clones the wiki's separate Git repository (`MRISS-Projects/dsh.wiki.git`).
2. Copies every top-level `.md` file into this directory.
3. Prepends an auto-generation header to each file so editors know to edit the
   canonical source in the wiki, not this copy.
4. Commits any changes with `[skip ci]` so the sync does not trigger further
   workflow runs.

## Editing wiki content

**Do not edit files in this directory directly.** All changes must be made in the
[GitHub Wiki](https://github.com/MRISS-Projects/dsh/wiki). The next scheduled sync
(or a manual workflow run) will propagate the changes here.

## Available pages

The files in this directory mirror the wiki pages one-for-one. If the wiki is empty
or has not been synced yet, this directory will contain only this README.

Once synced, refer to the [Wiki References table](../../.github/copilot-instructions.md)
for the exact paths to use in Copilot prompts.

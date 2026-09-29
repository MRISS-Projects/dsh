---
name: dsh-story-spec
description: Use when turning a DSH GitHub issue into a detailed spec on a task branch - step 3 of the DSH development process
---

# DSH: Story Spec

Step 3 of the process in `docs/process/ai-driven-development.md`.

## 1. Resolve the parent branch - before anything else

Ask which branch this work belongs on, or infer it and confirm:

- `DEVELOP` for ordinary feature work
- `staging-*-RC` only for fixes to the release being stabilised
- `*.x` for hotfixes to a released line

    git rev-parse --verify <parent>

**If the parent is `master`, refuse.** Say why: `master` holds released code placed there
by the release workflow, and branching from it produces work that cannot be merged back
through the normal path. Ask for `DEVELOP`, an RC, or a hotfix branch instead.

Then:

    git checkout <parent> && git pull && git checkout -b issue-<n>-<slug>

## 2. Write the spec

Invoke `superpowers:brainstorming` (architectural path), then `superpowers:writing-plans`.

Save to `specs/stories/<n>-<slug>.md`, starting with this front matter - `dsh-ship-story`
parses it later, so the keys are a contract:

    ---
    issue: 91
    slug: extract-document-persistence-repository
    parent_branch: DEVELOP
    wave: 1
    milestone: 0.4.0-SNAPSHOT
    ---

## 3. Hard stop

The human reviews the spec before it is committed. On approval, commit and push it to the
task branch. The spec lands *before* any implementation code - that is the point of the step.

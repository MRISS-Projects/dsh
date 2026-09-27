---
name: dsh-new-story
description: Use when turning a DSH PRD task into a GitHub issue written as an INVEST user story - step 2 of the DSH development process
---

# DSH: New Story

Step 2 of the process in `docs/process/ai-driven-development.md`.

## Input

A task from a wave in `specs/product/PRD.md`.

## Write it as an INVEST story

Use `.github/ISSUE_TEMPLATE/story.md`. Check each letter explicitly and say so:

| Letter | Test |
|---|---|
| Independent | Can it be built without waiting on another open story? |
| Negotiable | Does it state the need, not a prescribed implementation? |
| Valuable | Can you name who benefits, in one sentence? |
| Estimable | Is the work knowable, or does it need a spike first? |
| Small | One task branch, days not weeks. If not, split it. |
| Testable | Are the acceptance criteria checkable by a test? |

If any letter fails, fix the story before showing it. A story that fails "Small" gets split
into two stories, not written anyway.

Set the milestone to match the wave, per the mapping in `specs/product/PRD.md`.

**Every issue gets at least one label** — `bug`, `enhancement` or `task`, the first one being the
type. Until `MRISS-Projects/parent-poms#86` ships, the release notes in every generated `README.md`
silently omit an issue with no label (`MRISS-Projects/maven-changes-plugin#36`). An empty
`--label` below is a defect, not a default. Drop this paragraph once `#86` is closed and DSH is
pinned to a parent-poms release that includes it.

## Hard stop

Show the full issue body and wait for approval. **Then** run:

    gh issue create --title "<title>" --body-file <file> --milestone "<milestone>" --label "<labels>"

Never close an existing issue. That is the human's call - see `docs/process/ai-driven-development.md`.

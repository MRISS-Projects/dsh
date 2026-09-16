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

## Hard stop

Show the full issue body and wait for approval. **Then** run:

    gh issue create --title "<title>" --body-file <file> --milestone "<milestone>" --label "<labels>"

Never close an existing issue. That is the human's call - see `docs/process/ai-driven-development.md`.

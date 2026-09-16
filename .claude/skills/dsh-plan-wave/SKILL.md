---
name: dsh-plan-wave
description: Use when planning what DSH should build next, adding or reshaping a wave, or updating specs/product/PRD.md - step 1 of the DSH development process
---

# DSH: Plan a Wave

Step 1 of the process in `docs/process/ai-driven-development.md`.

**First, invoke `superpowers:brainstorming`.** That skill runs the conversation; this one
only supplies DSH context.

## DSH context to bring

- Read `specs/product/PRD.md` for existing waves, and
  `specs/architecture/ADR-001-GCP-based-components.md` for the migration phases that
  waves 1-5 mirror.
- Waves are ordered and roughly sequential. A task belongs in the earliest wave whose
  dependencies it satisfies.
- Every task must be small enough to become one INVEST story in step 2. "Migrate to
  Firestore" is a wave, not a task.

## Hard stop

Do not write to `specs/product/PRD.md` until the human approves the wave contents.

## Output

Update `specs/product/PRD.md` in place. Commit it. Then stop — turning tasks into issues
is step 2 (`dsh-new-story`), a separate decision.

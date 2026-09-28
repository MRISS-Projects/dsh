---
issue: 113
slug: remove-dead-main-branch-trigger
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 113 — Remove the dead `main` branch trigger from `api-testing.yml` and `documentation-sync.yml`

## 1. Story

**As a** developer reading a workflow's triggers
**I want** the branch lists to name branches this repository actually has
**So that** a trigger list is a statement about when the workflow runs, not a guess

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#113](https://github.com/MRISS-Projects/dsh/issues/113), label `task`
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`, matching the milestone, as `#115` did.
- Found while scoping `#46`, and kept out of it: a dead trigger has nothing to do with the
  integration-test lifecycle.

Two workflows list `main` under `on.push.branches`:

- `.github/workflows/api-testing.yml:6`
- `.github/workflows/documentation-sync.yml:6`

The default branch is `master`, and `git ls-remote --heads origin` on 2026-09-28 lists no `main`.
`CLAUDE.md`'s branch rules name four kinds of branch — `master`, `DEVELOP`,
`staging-X.Y.Z-SNAPSHOT-RC`, `X.Y.x` — plus task branches. `main` is none of them, so both entries
have never fired and never can.

### 2.1 Where `main` appears today

`grep -rn "main" .github/workflows/` on the RC at `9059a8769` prints four lines:

| Line | What it is |
|---|---|
| `api-testing.yml:6` — `- main` | Branch trigger. **Dead.** |
| `api-testing.yml:187` — `dsh-data/src/main/resources/mongo.properties` | A path in a comment. Not a trigger. |
| `ci.yml:232` — `parent still moved - just invisibly. Parent version upgrades remain a` | The word "remain". Not a trigger. |
| `documentation-sync.yml:6` — `- main` | Branch trigger. **Dead.** |

So AC002's grep is read as "no *branch-trigger* match", which the issue's wording already says.
The exact check is a list entry whose whole value is `main`:

```bash
grep -rnE '^[[:space:]]*-[[:space:]]*main[[:space:]]*$' .github/workflows/
```

Before this story it prints the two dead lines. After, nothing.

### 2.2 Docs that restate the trigger

Four documents describe these triggers and would be wrong once they change:

| File | Says today |
|---|---|
| `docs/devops/README.md:76` | `api-testing.yml`: `push` to `DEVELOP`/`main` … |
| `docs/devops/README.md:77` | `documentation-sync.yml`: `push` to `DEVELOP`/`main` … |
| `docs/api/README.md:17` | Trigger: Push to `develop` or `main` via `documentation-sync.yml` |
| `docs/architecture/README.md:17` | … on every push to `develop` or `main`. |
| `specs/devops/deploy-release-profiles-reorganization.md:300` | FR014 table, API Testing trigger: Manual / push to `develop` or `main` |

The last three also spell the branch `develop`; it is `DEVELOP`, and Actions branch filters are
case-sensitive. `documentation-sync.yml` does not regenerate either README — it writes only
`docs/api/index.html` and TOCs under `specs/` — so a hand edit sticks.

`deploy-release-profiles-reorganization.md` is the `#84` feature spec. It is still live — indexed
from `.github/copilot-instructions.md:62` — and `#92` already corrected stale lines in it
(`specs/stories/92-remove-travis-build-estate.md` §6), so it is corrected rather than treated as
a historical record.

## 3. Design

### 3.1 Workflows

Delete the `- main` line from each workflow. The list keeps its block form with one entry,
`- DEVELOP`, matching `ci.yml`'s style. Nothing else in either `on:` block changes: `paths`,
`pull_request` and `workflow_dispatch` stay as they are.

### 3.2 Docs

Each doc in §2.2 is corrected to state the trigger as it will be:

- `docs/devops/README.md:76` — `` `push` to `DEVELOP` and `pull_request`, both scoped to paths … ``
- `docs/devops/README.md:77` — `` `push` to `DEVELOP`, scoped to paths … ``
- `docs/api/README.md:17` — ``- **Trigger**: Push to `DEVELOP` via `/.github/workflows/documentation-sync.yml` ``
- `docs/architecture/README.md:17` — ``… on every push to `DEVELOP`.``
- `specs/devops/deploy-release-profiles-reorganization.md:300` — ``Manual / push to `DEVELOP` ``

The rest of each line is unchanged. `docs/devops/README.md`'s diagram (lines 53, 56) names no
branch and is unaffected; the claim under it — "checked node-for-node against the live YAML" —
stays true.

### 3.3 AC003: the remaining trigger lists

Three workflows filter `push` by branch. Checked against `CLAUDE.md`'s branch rules and
`git ls-remote --heads origin` on 2026-09-28:

| Workflow | Entry | Branch kind in `CLAUDE.md` | Exists on the remote |
|---|---|---|---|
| `api-testing.yml` | `DEVELOP` | mainline | yes |
| `documentation-sync.yml` | `DEVELOP` | mainline | yes |
| `ci.yml` | `DEVELOP` | mainline | yes |
| `ci.yml` | `staging-*-RC` | release candidate | yes — `staging-0.3.0-SNAPSHOT-RC` |
| `ci.yml` | `*.x` | hotfix line | no branch today |

`*.x` matching nothing is expected, not dead: a hotfix line exists only while a hotfix is in
flight, and `X.Y.x` is a branch kind `CLAUDE.md` names. It is a pattern for a kind this
repository uses. No other workflow declares a branch filter: `spec-validation.yml` filters by
path only, and `wiki-sync.yml`, `stage.yml`, `staging.yml`, `release.yml` and `hotfix.yml` have no
`push` trigger.

So AC003 finds nothing further to report. §6 posts that result on the issue.

### 3.4 Out of scope

- **Which branches these workflows *should* run on.** Neither fires on `staging-*-RC` or `*.x`
  pushes. That is a question about what they are for, not about dead names, and nothing here
  depends on it.
- **Renaming `master` to `main`.** Excluded by the issue.
- **Historical records.** `.superpowers/sdd/2026-09-16-ai-driven-development-process/` and
  `docs/superpowers/plans/2026-09-16-ai-driven-development-process.md` quote the old triggers.
  They record what was true then and are not edited.

## 4. Files to change

| File | Change |
|---|---|
| `.github/workflows/api-testing.yml` | Delete line 6, `- main` |
| `.github/workflows/documentation-sync.yml` | Delete line 6, `- main` |
| `docs/devops/README.md` | Lines 76–77, per §3.2 |
| `docs/api/README.md` | Line 17, per §3.2 |
| `docs/architecture/README.md` | Line 17, per §3.2 |
| `specs/devops/deploy-release-profiles-reorganization.md` | Line 300, per §3.2 |
| `specs/stories/113-remove-dead-main-branch-trigger.md` | §7 build record, §8 ticks |

No Maven module changes, so no Maven build. CI's `ci.yml` still runs on the PR and must be green;
`spec-validation.yml` runs because the PR touches `docs/**` and `specs/**`.

## 5. Tasks

There is no Java under test. The failing check is §2.1's grep, plus a doc grep: Task 1 records
both red, Tasks 2 and 3 turn them green.

The doc grep, over everything that is not a historical record:

```bash
grep -rnE '`(main|develop)`' docs specs .github CLAUDE.md \
  --exclude-dir=superpowers --exclude-dir=wiki \
  --exclude=113-remove-dead-main-branch-trigger.md --exclude=PRD.md
```

`--exclude-dir=superpowers` drops `docs/superpowers/`; `--exclude-dir=wiki` drops the synced
`docs/wiki/`; `.superpowers/` is not under any searched root. The spec excludes itself because
it quotes the old text, and `PRD.md` because its two hits (lines 92 and 218) name this issue,
not a trigger.

- [ ] **Task 1 — baseline on the task branch.** Before any edit:
      1. Run §2.1's grep. Expected: exactly `api-testing.yml:6` and `documentation-sync.yml:6`.
      2. Run the doc grep. Expected: exactly the five lines in §2.2's table.
      3. Record both outputs in §7. Commit nothing.
- [ ] **Task 2 — remove the dead triggers.**
      1. Delete the `- main` line from both workflows.
      2. Run §2.1's grep. Expected: no output, exit 1.
      3. Read back each file's `on:` block (`sed -n 1,15p`). Expected: `push.branches` holds only
         `- DEVELOP`; `paths`, `pull_request` and `workflow_dispatch` unchanged.
      4. `git diff --stat`. Expected: two files, one deletion each.
      5. Commit: `fix(#113): remove the dead main branch trigger from two workflows`.
- [ ] **Task 3 — correct the docs.**
      1. Apply §3.2's five edits.
      2. Run the doc grep. Expected: no output, exit 1.
      3. Run the `CLAUDE.md` markdownlint command, with
         `export PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`. Expected: no findings.
      4. Commit: `docs(#113): state the DEVELOP-only trigger in the workflow docs`.
- [ ] **Task 4 — record and report.**
      1. Fill in §7, tick §8, rerun markdownlint. Expected: no findings.
      2. Commit: `docs(#113): record the verification`.
      3. After the user approves the text, post §6 on `#113` and record the comment URL in §7.

## 6. Issue comment

To be posted on `#113` in Task 4, after approval:

> **AC003 — remaining trigger lists checked against `CLAUDE.md`'s branch rules**
>
> Three workflows filter `push` by branch. With `main` removed:
>
> | Workflow | Entry | Branch kind | On the remote |
> |---|---|---|---|
> | `api-testing.yml` | `DEVELOP` | mainline | yes |
> | `documentation-sync.yml` | `DEVELOP` | mainline | yes |
> | `ci.yml` | `DEVELOP` | mainline | yes |
> | `ci.yml` | `staging-*-RC` | release candidate | yes |
> | `ci.yml` | `*.x` | hotfix line | none today |
>
> Nothing else to report. `*.x` matching no branch is expected: a hotfix line exists only while a
> hotfix is in flight, and `X.Y.x` is a branch kind the rules name. No other workflow filters by
> branch.
>
> Also fixed here, because the change made them wrong: `docs/devops/README.md`,
> `docs/api/README.md`, `docs/architecture/README.md` and
> `specs/devops/deploy-release-profiles-reorganization.md` restated the `main` trigger, the last
> three alongside a lowercase `develop`.
>
> Not addressed, and not proposed: neither workflow fires on `staging-*-RC` or `*.x` pushes.
> That is a question of what they are for, separate from dead names.

## 7. Build record

Filled in during Tasks 1–4.

## 8. Acceptance criteria

- [ ] AC001: `main` is removed from the `push.branches` list in `api-testing.yml` and
  `documentation-sync.yml`. — Task 2
- [ ] AC002: `grep -rn "main" .github/workflows/` returns no branch-trigger match. — Task 2,
  §2.1's exact grep
- [ ] AC003: Each workflow's remaining trigger list is checked against `CLAUDE.md`'s branch rules,
  and any other branch named there that this repository does not have is reported on this issue.
  — §3.3, comment §6

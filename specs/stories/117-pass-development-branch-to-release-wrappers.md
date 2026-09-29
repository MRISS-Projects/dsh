---
issue: 117
slug: pass-development-branch-to-release-wrappers
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 117 — Pass `development_branch` to the release and hotfix wrappers

## 1. Story

**As a** release manager cutting a DSH release
**I want** `release.yml` and `hotfix.yml` to name `DEVELOP` as this repository's development branch
**So that** every release is merged back into `DEVELOP`, and the release workflows stop failing at
their preflight before they have done anything

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#117](https://github.com/MRISS-Projects/dsh/issues/117)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the milestone, and follows `#114`/`#111`,
  which edited the same two files and shipped onto the same RC. `DEVELOP` is at `0.4.0-SNAPSHOT`.
- Upstream half: [`parent-poms#65`](https://github.com/MRISS-Projects/parent-poms/issues/65), fixed
  by `parent-poms#82`, closed 2026-09-25. Spec: `parent-poms/specs/65-merge-release-back-into-develop.md`.
- Twin pattern: `#85`/`parent-poms#57`, `#111`/`parent-poms#72`. The wrappers reference parent-poms
  at `@master`, and this change touches no POM, so no parent-poms release or re-pin is involved.

### 2.1 The ordering constraint is satisfied

The issue says `#117` can merge only after `parent-poms#65`, because a caller passing an input the
callee does not declare is a hard error. Checked on `parent-poms` `master` while writing this spec:

| Where | What |
|---|---|
| `project-release.yml:48-52` | `development_branch`, `type: string`, `default: 'DEVELOPMENT'` |
| `project-hotfix.yml:32` | the same input |
| `project-release.yml:185-191` / `project-hotfix.yml:164-170` | preflight `Check the development branch exists` — `git ls-remote --exit-code --heads`, error names `dsh: DEVELOP` |
| `project-release.yml:415-421` / `project-hotfix.yml:277-280` | `merge-to-develop` action, fed `inputs.development_branch` |
| `project-release.yml:447` / `project-hotfix.yml:304` | `merge-to-develop` in `declared_markers` |
| `.github/actions/merge-to-develop/assert-carried-over.sh:75` | `merge-to-develop: carried $carried path(s) from $tag into $branch; 0 lost` |

So the constraint is met, and the reverse is now the live defect: **since `parent-poms#82` merged,
both wrappers resolve `development_branch` to `DEVELOPMENT`, which does not exist here, and fail at
the preflight.** Nothing is written — the preflight runs before `release:prepare`.

## 3. The defect

`.github/workflows/release.yml:33-50` and `.github/workflows/hotfix.yml:21-31` pass `git_project`,
the version inputs, `dry_run` and `maven_properties`. Neither passes `development_branch`.
`stage.yml:18` already passes `development_branch: DEVELOP` to `project-stage.yml`, which has
carried that input for longer; the release pair is the gap.

## 4. Design

One line in each wrapper's `with:` block, placed after `git_project` so the two repository-identity
inputs sit together:

```yaml
      git_project: dsh
      # #117: parent-poms#65 merges every release back into the development branch, and
      # defaults its name to DEVELOPMENT. Ours is DEVELOP, as stage.yml already says.
      development_branch: DEVELOP
```

**A constant, not a dispatch input** — the same reasoning as `maven_properties` in `#114` §7.1. The
development branch is a fact about this repository, not a per-run choice, and a form field an
operator can retype is a form field that can send a release's merge-back to the wrong branch. The
preflight would catch a non-existent name, but not an existing wrong one.

### 4.1 Explicitly not in scope

- **`staging.yml`.** `project-staging.yml` has no merge-back and no `development_branch` input.
- **`stage.yml`.** Already correct (`stage.yml:18`).
- **Anything upstream.** `parent-poms#65` is closed and its spec owns the merge-back's behaviour.
  If a rehearsal from here finds a defect in it, that is a new parent-poms issue, not an edit on
  this branch (`CLAUDE.md`, "Fix shared gaps upstream").
- **Changing the upstream default to `DEVELOP`.** DSH is the only consumer today, but parent-poms
  names its own convention `DEVELOPMENT`; the consumer supplies its branch name, as it supplies its
  build properties.

## 5. Files to change

### 5.1 `.github/workflows/release.yml`

Add the three lines from §4 after `git_project: dsh` (line 34).

### 5.2 `.github/workflows/hotfix.yml`

The same three lines after `git_project: dsh` (line 22). The comment may shorten to "same as
`release.yml`", matching how the `maven_properties` comment is shortened there today.

### 5.3 `docs/devops/README.md`

The document describes a release flow that no longer matches what the workflows do once this lands.

- **Branching gitGraph, lines 9-25** — add a merge of the release back into `DEVELOP` after the
  `v0.3.0` tag, so the diagram shows the new edge.
- **Release-model bullets, lines 28-37** — one bullet: every release and hotfix release ends by
  merging its tag back into `DEVELOP`, which keeps its own version; a conflict stops the run with
  nothing pushed, leaving a completed release and a merge a human finishes from the tag.
- **Workflow Reference rows, lines 73-74** — `release.yml` and `hotfix.yml` rows add that they pass
  `development_branch: DEVELOP` and end with the merge-back.
- **"Rehearsing a release", lines 82-97** — the list of suppressed writes gains "no push to
  `DEVELOP`". A rehearsal still computes and verifies the merge and prints the `carried … 0 lost`
  line, which is the thing worth reading before a first release.

### 5.4 `specs/product/PRD.md`

`#117`'s row (line 94) and the `#65` paragraphs (lines 314-319, 694-697) describe `#117` as the
missing piece. `dsh-reconcile-prd` (step 8) owns that edit after the merge; listed here so the
reviewer does not flag it as missed.

## 6. Tasks

No `.java` file changes, so the red/green cycle is on the workflow itself: the dispatched run that
fails today is the failing test, and the same dispatch passing is green.

- [x] **Task 1 — red.** Dispatch `release.yml` from `staging-0.3.0-SNAPSHOT-RC` (the wrapper as it
      stands) with `dry_run: true` and the inputs in §7. Expect failure at
      `Check the development branch exists`, error naming `DEVELOPMENT`, and nothing written.
      Record the run link. Run [36162065850](https://github.com/MRISS-Projects/dsh/actions/runs/36162065850).
- [x] **Task 2 — `release.yml`.** §5.1. Commit.
- [x] **Task 3 — `hotfix.yml`.** §5.2. Commit.
- [x] **Task 4 — green.** Push the branch; dispatch `release.yml` from it with the same inputs.
      Checks in §7. Record the run link against AC002.
- [x] **Task 5 — `docs/devops/README.md`.** §5.3. Run the markdown lint from `CLAUDE.md`. Commit.
- [x] **Task 6 — tick the ACs** in §8 with the run links. Commit.

## 7. Verification

**Dispatch inputs**, for both Task 1 and Task 4:

| Input | Value |
|---|---|
| `branch_name` | `staging-0.3.0-SNAPSHOT-RC` |
| `current_version` | `0.3.0` |
| `next_development_version` | `0.4.0-SNAPSHOT` |
| `hotfix_branch` | `0.3.x` |
| `initial_hotfix_version` | `0.3.1-SNAPSHOT` |
| `dry_run` | `true` |

```bash
gh workflow run release.yml --ref <branch> \
  -f branch_name=staging-0.3.0-SNAPSHOT-RC -f current_version=0.3.0 \
  -f next_development_version=0.4.0-SNAPSHOT -f hotfix_branch=0.3.x \
  -f initial_hotfix_version=0.3.1-SNAPSHOT -f dry_run=true
```

**Green run must show**, all read from the log rather than inferred from the conclusion:

1. The preflight passing for `DEVELOP`.
2. `merge-to-develop: DEVELOP is at 0.4.0-SNAPSHOT, <scm><tag> …` — `DEVELOP` read at its own version.
3. `merge-to-develop: carried <n> path(s) from v0.3.0 into DEVELOP; 0 lost` with `n > 0` — the
   RC-only work is real (the PRD counts 152 commits of this wave on the RC only), so `n = 0` would
   mean the merge carried nothing and is itself a finding.
4. The `merge-to-develop` rehearsal marker, and `rehearsal-verify` passing with all nine declared
   markers.

**And, against the live remote, before and after:** `origin/DEVELOP` and
`origin/staging-0.3.0-SNAPSHOT-RC` at the same SHA, and no `v0.3.0` tag. At spec time:
`DEVELOP` `60c759cc7f478528430b7bd8045c940ec3a9f1a2`, RC `6903a1ab324df723384a1286776dc058d0b8c4db`
(the RC will have advanced by this branch's merge by the time Task 4 runs — re-read it then).

**`hotfix.yml` is verified by inspection, not by a run.** Its rehearsal needs a `0.3.x` branch,
which does not exist until `0.3.0` is released — the same gap `#114` §11 recorded. The upstream
hotfix merge-back itself was proven by `parent-poms#65`'s rehearsals from this repository on a
scratch branch; what is unproven here is only that `hotfix.yml` passes the input, which is a
one-line diff identical to `release.yml`'s. The first real hotfix is the confirming run.

## 8. Acceptance criteria

From the issue:

- [x] **AC001** — `.github/workflows/release.yml` and `.github/workflows/hotfix.yml` pass
      `development_branch: DEVELOP`. Commits `3212b9c79`, `fc7902c6a`.
- [x] **AC002** — a `dry_run` release rehearsal shows
      `merge-to-develop: carried <n> path(s) from v0.3.0 into DEVELOP; 0 lost` and the
      `merge-to-develop` marker. Run
      [36162240349](https://github.com/MRISS-Projects/dsh/actions/runs/36162240349): preflight step 9 passed;
      `DEVELOP is at 0.4.0-SNAPSHOT, <scm><tag> HEAD`; `carried 94 path(s) from v0.3.0 into DEVELOP;
      0 lost`; `REHEARSAL merge-to-develop: would push the merge of v0.3.0 into DEVELOP`.

Added by this spec:

- [x] **AC003** — The same rehearsal inputs against the unchanged wrapper fail at the preflight,
      naming `DEVELOPMENT` (Task 1). Proves AC002's run passed because of this change. Run
      [36162065850](https://github.com/MRISS-Projects/dsh/actions/runs/36162065850), step 9: `development branch
      'DEVELOPMENT' does not exist on the remote. Pass development_branch from the calling workflow
      (dsh: DEVELOP).`
- [x] **AC004** — The AC002 rehearsal writes nothing: `DEVELOP` and the RC unchanged on the remote,
      no `v0.3.0` tag, `rehearsal-verify` green. Same run: `all 9 declared write point(s) announced exactly
      once`, `the remote is byte-for-byte as it was before the run`. After the run, `origin/DEVELOP`
      `60c759cc7`, RC `6903a1ab3` (both as at spec time), no `v0.3.0` tag.
- [x] **AC005** — `docs/devops/README.md` describes the merge-back into `DEVELOP` in the branching
      model, the Workflow Reference rows, and the rehearsal's list of suppressed writes.
- [x] **AC006** — CI green on the PR (`Build, Test and Coverage Gate`, `Validate Markdown Files`,
      `Check Spec File References`). No `.java` in the diff, so the coverage gate is unaffected. PR
      [#118](https://github.com/MRISS-Projects/dsh/pull/118) at `adcca20b3`: all seven checks pass; the build
      ([run 36193341855](https://github.com/MRISS-Projects/dsh/actions/runs/36193341855)) logs
      `All coverage checks have been met` and `BUILD SUCCESS`. Copilot review (Lite): no findings.

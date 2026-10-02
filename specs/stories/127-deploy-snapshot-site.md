---
issue: 127
slug: deploy-snapshot-site
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 127 — Deploy the snapshot site from DEVELOP, as parent-poms' deploy.yml does

## 1. Story

**As a** developer following DSH between releases
**I want** a workflow that deploys DSH's snapshot artifacts and site from `DEVELOP`
**So that** `snapshots/products/dsh/` shows the current development line, not a site from 2020

## 2. Context

- Wave: 1, upstream step 3 (`specs/product/PRD.md` §4, Wave 1), DSH milestone `0.4.0-SNAPSHOT`
- Issue: [#127](https://github.com/MRISS-Projects/dsh/issues/127)
- Parent branch: `DEVELOP`, at `d08501278` when this spec was written. The task branch is
  `issue-127-deploy-snapshot-site`.
- **This story is also a proof run.** It comes before parent-poms 3.10.0 is released, so that DSH's
  first snapshot deploy runs against `3.10.0-SNAPSHOT` and shows three parent-poms fixes working
  from a consumer, with a real publish:
  - [`parent-poms#89`](https://github.com/MRISS-Projects/parent-poms/issues/89): the "Products"
    parent link. It is still open, and closes on this story's evidence.
  - [`parent-poms#88`](https://github.com/MRISS-Projects/parent-poms/issues/88): the stage, verify
    and publish steps.
  - [`parent-poms#86`](https://github.com/MRISS-Projects/parent-poms/issues/86): the README from
    changes plugin 2.12.10.

### 2.1 What was established while writing this spec

- **Nothing publishes DSH's snapshot site.** DSH has `stage.yml`, `staging.yml`, `release.yml` and
  `hotfix.yml`. `gh-pages` holds `snapshots/dsh/`, 2,142 files last published on 2020-02-22 by the
  old pipeline, and no `snapshots/products/`.
- **parent-poms' `deploy.yml` is not reusable.** It is a `workflow_dispatch` workflow of that
  repository. It cannot be called from DSH.
- **`project-staging.yml` already does everything a snapshot deploy needs.** It checks out a branch,
  builds and deploys the artifacts, checks the generated README, stages, verifies and publishes the
  site, and commits the README to the branch. Two things in it are specific to an RC:
  `-Drelease.type=rcs` and the `RC` prefix on the build number. Both are literals in two `run:`
  steps.
- **`release.type` decides the path.** With `snapshots`, the POM's default, the root site stages at
  `/tmp/sites/snapshots/products/dsh`, from `group.id.path` (`pom.xml:95`). That is AC002's
  address.

## 3. Design

### 3.1 Decisions

1. **Reuse `project-staging.yml`, with one new input, rather than copy it.** AC001 asks for
   parent-poms' reusable workflows "where one fits". This one fits but for the two literals. A copy
   in DSH would duplicate about 150 lines, including the generated `settings.xml`, that then drift.
   - parent-poms gains a `release_type` input on `project-staging.yml`, defaulting to `rcs`. With
     `rcs` the workflow behaves exactly as today. With `snapshots`, `-Drelease.type` is `snapshots`
     and the build number has no `RC` prefix.
   - That is a change to a release workflow, so it takes the full round trip: a parent-poms issue
     on `3.10.0-SNAPSHOT`, with its own spec there. It is not designed here.
2. **DSH gains `deploy.yml`, a thin wrapper**, in the shape of `staging.yml`: `workflow_dispatch`,
   one job calling `project-staging.yml@master` with `release_type: snapshots` and the same four
   `mongo.*` build properties.
3. **It deploys the ref it is dispatched on.** `branch_name` is `${{ github.ref_name }}`. Run from
   the Actions page it is `DEVELOP`, which is the normal case and AC001's. Dispatched on a task
   branch it deploys that branch, which is how this story proves itself before it merges.
   parent-poms' `deploy.yml` works the same way.
4. **It runs the integration tests**, as staging does. The same build command is used, so nothing
   is published that the tests did not pass on, and the reusable workflow needs no second switch.
5. **It commits the generated `README.md` to the branch**, as staging does for an RC and as
   parent-poms' `deploy.yml` does for `master`. `DEVELOP`'s README then shows the snapshot's
   version, build and release notes. This is also where `parent-poms#86`'s proof is read.
6. **The `3.10.0-SNAPSHOT` pin merges into `DEVELOP` with this story** (decided at review,
   2026-10-02). Upstream step 5 replaces it with the released `3.10.0`. So every snapshot deploy
   from `DEVELOP`, from the first one, has the three parent-poms fixes: the "Products" link works
   and the README has the 2.12.10 changes.
   - **The cost.** `DEVELOP` depends on a `-SNAPSHOT` parent from this merge until step 5. Both of
     this repository's Maven invocations in CI pass `-U`, so each run tracks the current snapshot,
     which `CLAUDE.md` names as the intended contract while the parent is a `SNAPSHOT`. A change to
     parent-poms `master` in that window reaches `DEVELOP`'s builds without a commit here.
   - **Not chosen:** keeping the pin on the task branch and dropping it before the merge. `DEVELOP`
     would stay on `3.9.2`, and its snapshot site would show the "Products" 404 until step 5.
7. **The stale `snapshots/dsh/` tree is removed** (AC004). It is a 2020 site at an address nothing
   will link to once the wiki row moves, and no publish ever cleans it: `publish-scm` runs with
   `skipDeletedFiles`. It is removed by one commit on `gh-pages`, after the new site is live.
   `#128` decides the same question for `releases/dsh/` and `rcs/dsh/`. This story does not touch
   them.

### 3.2 Cross-repository order

| # | Where | What | Depends on |
|---|---|---|---|
| P1 | parent-poms | New issue on `3.10.0-SNAPSHOT`: a `release_type` input for `project-staging.yml`. Spec and task branch there | this spec approved |
| D1 | DSH, this branch | `deploy.yml`, pointing at the parent-poms task branch for now. The root `pom.xml` pinned to `3.10.0-SNAPSHOT`, as a separate commit | P1 pushed |
| D2 | DSH, this branch | **The proof run**: dispatch `deploy.yml` on this branch. A real deploy and a real publish | D1 |
| P2 | parent-poms | Merge P1's PR | D2 green |
| D3 | DSH, this branch | Point `deploy.yml` at `@master`. The pin commit stays, and so does the README commit the proof run left on the branch: it is `parent-poms#86`'s evidence, and D5 regenerates it | P2 |
| D4 | DSH | Docs, including the parent pin in `docs/devops/README.md`. The wiki row, the legacy tree. PR into `DEVELOP` | D3 |
| D5 | DSH `DEVELOP` | After the merge: dispatch `deploy.yml` on `DEVELOP`, the first real snapshot deploy of the mainline, on `3.10.0-SNAPSHOT` | D4 merged |

P1 is proved by D2 before it merges. That is the same order `parent-poms#95` and `dsh#146` used.

### 3.3 Out of scope

- A schedule or a push trigger. The issue asks for `workflow_dispatch`.
- The legacy `releases/dsh/` and `rcs/dsh/` trees (`#128`).
- Re-pinning `DEVELOP` to the released `3.10.0`, and removing the issue-labelling rule (upstream
  step 5). This story only moves the pin from `3.9.2` to `3.10.0-SNAPSHOT`.

## 4. Files to change

| File | Change |
|---|---|
| `.github/workflows/deploy.yml` | new: the wrapper (§3.1, decisions 2 and 3) |
| `docs/devops/README.md` | the workflow table gains `deploy.yml`. The `${release.type}` and build-number rows name `snapshots`. The "Parent POM" section names the `3.10.0-SNAPSHOT` pin, why it is there, and that step 5 ends it |
| `CLAUDE.md` | nothing, unless the commands table needs the dispatch. Checked in D4 |
| the wiki, `Code-Based-Site-and-Reports` | the Snapshots row points at `snapshots/products/dsh` (AC003). Edited in the wiki repository. `wiki-sync.yml` copies it to `docs/wiki/` |
| `gh-pages` | `snapshots/dsh/` removed, one commit (AC004) |
| `pom.xml` | the parent version, `3.9.2` → `3.10.0-SNAPSHOT`, in its own commit. It reaches `DEVELOP` and stays until upstream step 5 |

There is no Java change, so no unit or integration test is added. But the parent changes under
every module, so gates 1 and 2 are run on the branch with the pin. Gate 3, the local integration
gate, is run too: the new parent changes how every module is built, `dsh-rest-api` included.

## 5. Tasks

- [ ] **P1.** Open the parent-poms issue for `release_type`, on `3.10.0-SNAPSHOT`, after its text is
      approved. Write its spec and build it on a parent-poms task branch.
- [ ] **D1.** Pin the root `pom.xml` to `3.10.0-SNAPSHOT` in its own commit, and run gates 1 to 3 on
      it, logged. Add `deploy.yml`, calling `project-staging.yml` at the parent-poms task branch.
- [ ] **D2.** Dispatch `deploy.yml` on this branch, and check §6.1.
- [ ] **P2.** After D2 is green, the parent-poms PR is merged by the owner.
- [ ] **D3.** Point `deploy.yml` at `@master`.
- [ ] **D4.** Update `docs/devops/README.md`, the parent pin included. Edit the wiki row. Remove
      `snapshots/dsh/` from `gh-pages`.
- [ ] **D5.** After the PR merges: dispatch `deploy.yml` on `DEVELOP`, and check §6.2.

## 6. Verification

### 6.1 The proof run, D2: against `3.10.0-SNAPSHOT`

The run is green, and:

1. **Artifacts.** `0.4.0-SNAPSHOT` artifacts are deployed for all 13 modules.
2. **`parent-poms#88`.** `Skipping site deployment` is logged 13 times while staging.
   `verify-staged-site` reports 13 module sites, each with an `index.html`. `gh-pages` gains exactly
   one commit.
3. **AC002.** `https://mriss-projects.github.io/dsh/snapshots/products/dsh/index.html` answers 200,
   and so does each module's page.
4. **`parent-poms#89`.** `https://mriss-projects.github.io/dsh/snapshots/products/index.html`
   answers 200 and redirects to `https://mriss-projects.github.io/parent-poms/releases/products/`.
   The home page's "Products" link is `../index.html`, which is that page.
5. **`parent-poms#86`.** The README committed to the branch lists `### Version 0.3.1` with a
   `No issues` row, between `0.3.2` and `0.3.0`. The site's GitHub report lists the same issues as
   the README, in the same version order.
6. **Nothing else moved.** `rcs/` and `releases/` on `gh-pages` hold the same files as before.

If any of 2, 4 or 5 fails, the fix is made in parent-poms' `3.10.0-SNAPSHOT`, and D2 is run again.

### 6.2 After the merge, D5: from `DEVELOP`, on `3.10.0-SNAPSHOT`

The run is green, the site is at `snapshots/products/dsh/`, and `DEVELOP` has one new README commit.
Every item of §6.1 holds again, because `DEVELOP` carries the pin (§3.1, decision 6).

### 6.3 AC003 and AC004

- The wiki's Snapshots row links to `…/dsh/snapshots/products/dsh`, and the link answers 200.
- `snapshots/dsh/` is gone from `gh-pages`, and `snapshots/products/dsh/` is untouched by that
  commit.

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: a `workflow_dispatch` workflow deploys DSH's snapshot artifacts and site from `DEVELOP`, through parent-poms' reusable workflows where one fits | §3.1 decisions 1 to 3; D1, D5 |
| AC002: the site lands at `snapshots/products/dsh/` | §2.1; §6.1 item 3 |
| AC003: the wiki's Snapshots row points at it | D4; §6.3 |
| AC004: the stale `snapshots/dsh/` tree is removed or labelled legacy, as an explicit decision | §3.1 decision 7; D4; §6.3 |

## 8. Verification results

To be filled in during the build.

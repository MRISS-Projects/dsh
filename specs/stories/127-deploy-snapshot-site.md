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

- [x] **P1.** Open the parent-poms issue for `release_type`, on `3.10.0-SNAPSHOT`, after its text is
      approved. Write its spec and build it on a parent-poms task branch.
- [x] **D1.** Pin the root `pom.xml` to `3.10.0-SNAPSHOT` in its own commit, and run gates 1 to 3 on
      it, logged. Add `deploy.yml`, calling `project-staging.yml` at the parent-poms task branch.
- [x] **D2.** Dispatch `deploy.yml` on this branch, and check §6.1.
- [x] **P2.** After D2 is green, the parent-poms PR is merged by the owner.
- [x] **D3.** Point `deploy.yml` at `@master`.
- [x] **D4.** Update `docs/devops/README.md`, the parent pin included. Edit the wiki row. Remove
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

### 8.1 The gates, on the branch with the pin (D1), 2026-10-02

- **Gates 1 and 2.** `mvn -B -U clean install`: all 13 modules succeed, 127 tests run with no
  failure, and "All coverage checks have been met" in each of the 8 modules with production code.
  The parent resolved to `products-3.10.0-20261002.123547-2`, the deployed snapshot.
- **Gate 3.** `mvn -B clean verify -DintegrationTests`, over the whole reactor because the pin
  changes every module's parent: all 13 modules succeed, with failsafe running in each.

### 8.2 A finding the spec missed: a dispatched workflow must exist on the default branch

The first dispatch of `deploy.yml` on this branch was refused:
`HTTP 404: workflow deploy.yml not found on the default branch`.

- **The rule.** GitHub starts a `workflow_dispatch` workflow only if a file of that name is on the
  repository's default branch. DSH's is `master`, which receives code only at a release. Once the
  file is there, a dispatch on any other ref runs that ref's version.
- **So a new workflow file is not dispatchable from `DEVELOP` until the next release**, which is
  where AC001 says it runs. §3.1 did not see this.
- **Options weighed.** Folding the snapshot deploy into `staging.yml`, which is already on `master`.
  Waiting for 0.4.0. Putting `deploy.yml` on `master` now.
- **Decided by the owner, 2026-10-02: a direct commit to `master`, as a one-off exception** to
  "master is release automation only". Commit `ea2c5e444` adds one file and changes nothing else.
- **What is on `master` is a placeholder**, not the wrapper. Dispatched on `master` it fails with
  "Dispatch it on DEVELOP instead", so a snapshot is never deployed from released code. The 0.4.0
  release replaces it with the real file.

### 8.3 The proof run (D2), 2026-10-02

[Run 37032819476](https://github.com/MRISS-Projects/dsh/actions/runs/37032819476): `deploy.yml` on
this branch at `5fd5e5f58`, through `project-staging.yml` at parent-poms' `#104` branch. Green.

| §6.1 item | Result |
|---|---|
| 1. Artifacts | `0.4.0-SNAPSHOT` POMs uploaded for all 13 modules |
| 2. `parent-poms#88` | `Skipping site deployment` logged 13 times. `verify-staged-site: all 13 module site(s) under '/tmp/sites' have an index.html.` One real publish, and `gh-pages` gained exactly one commit, `fbc5e0e68` |
| 3. AC002 | `snapshots/products/dsh/index.html` answers 200, and so do the module pages checked: `dsh-data`, `dsh-rest-api`, `dsh-keyword-extractor`, `dsh-coverage-report`. `gh-pages` holds 13 module sites under `snapshots/products/` |
| 4. `parent-poms#89` | `snapshots/products/index.html` answers 200 and carries `http-equiv="refresh" content="0; url=https://mriss-projects.github.io/parent-poms/releases/products/"`. That target answers 200. The home page links `<a href="../index.html">Products</a>` |
| 5. `parent-poms#86` | The README committed to the branch (`502d32f7e`) has `### Version 0.3.1` with `\| - \| - \| No issues \| - \| - \| - \|`, between `0.3.2` and `0.3.0`. The site report runs `0.3.2 0.3.0 0.2.4 … 0.0.1`, the README's order. `0.3.1` is in the README only, by design: the empty-milestone row is a text-list feature |
| 6. Nothing else moved | `rcs/` (4,469 files), `releases/` (4,380) and `snapshots/dsh/` (2,142) are unchanged. No file outside `snapshots/products/` changed |

- **`parent-poms#104`.** The README's version line is `0.4.0-SNAPSHOT - 1 - 20261002-162717`: a plain
  build number, with no `RC` prefix, and the site is under `snapshots/`.
- **The plugin's delete count.** It logged `13507 delete(s)`. None was applied, as item 6 shows.
  `skipDeletedFiles` is on in the profile.

### 8.4 After the proof run (P2, D3, D4), 2026-10-02

- **P2.** parent-poms PR #105 merged, `bdbe8c35`, and `parent-poms#104` closed with it.
- **D3.** `deploy.yml` calls `project-staging.yml@master` (`e451c7401`). The pin commit and the
  README commit from the proof run both stay on the branch.
- **D4, the docs.** `docs/devops/README.md` (`80e28c31b`): `deploy.yml` in both workflow tables, the
  snapshot case in the README placeholder rows, and the "Parent POM" section. That section named a
  stale `3.8.0-SNAPSHOT`. It now names `3.10.0-SNAPSHOT`, why it is there and what ends it.
  `CLAUDE.md` needed no change: it says nothing that the new workflow or the pin contradicts.
- **D4, AC004.** `gh-pages` commit `2e468ccf2` removes `snapshots/dsh/`: 2,142 deletions, and no
  path outside that tree. `snapshots/products/` still holds its 2,609 files.
  `https://mriss-projects.github.io/dsh/snapshots/dsh/index.html` now answers 404.
- **D4, AC003.** The wiki's Snapshots row, which said "TBD", links to
  `https://mriss-projects.github.io/dsh/snapshots/products/dsh/` (wiki commit `6d392ed`). The link
  answers 200. `docs/wiki/` picks the change up at the next `wiki-sync.yml` run.

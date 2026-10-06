---
issue: 128
slug: point-wiki-release-link-at-products-path
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 128 — Point the wiki's release link at releases/products/dsh once 0.3.0 is released

## 1. Story

**As a** reader of DSH's published site
**I want** one release site and one RC site, at the addresses the wiki gives
**So that** no old address goes on quietly serving 0.2.4 as though it were current

## 2. Context

- Wave: 1, step 1 of the order agreed on 2026-10-06 (`specs/product/PRD.md` §4, Wave 1), DSH
  milestone `0.4.0-SNAPSHOT`
- Issue: [#128](https://github.com/MRISS-Projects/dsh/issues/128)
- Parent branch: `DEVELOP`, at `2be7a423f` when this spec was written. The task branch is
  `issue-128-point-wiki-release-link-at-products-path`.
- **AC001 and AC002 are already done.** The
  [2026-09-30 comment](https://github.com/MRISS-Projects/dsh/issues/128) on the issue records it:
  `releases/products/dsh/` answers 200 and shows `Version: 0.3.2`, and the wiki's release row was
  moved to it (wiki commit `ded3572`). `#90` is closed, so the conditional part of AC002, linking
  `project-info.html`, does not apply. This story re-checks both (§6) and decides AC003.
- **Precedent.** `#127` removed `snapshots/dsh/` from `gh-pages` in one commit, `2e468ccf2`,
  without a pointer page (`specs/stories/127-deploy-snapshot-site.md` §3.1, decision 7).

### 2.1 What was established while writing this spec

All against `origin/gh-pages` on 2026-10-06.

| Fact | Evidence |
|---|---|
| Two legacy trees remain: `releases/dsh/` and `rcs/dsh/`, 1,772 files each | `git ls-tree -r origin/gh-pages <tree> \| wc -l` |
| `releases/dsh/` serves 0.2.4. It was last published on 2019-04-26, in commit `5a4bc0e8` | `Version: 0.2.4` in its `index.html`, and `git log -1 -- releases/dsh` |
| `rcs/dsh/` serves 0.2.4-SNAPSHOT. It was last published on 2019-04-26, in commit `f69affba` | the same checks |
| Nothing publishes to either tree again. `group.id.path` is `products/dsh` (`pom.xml:97`), and `publish-scm` runs with `skipDeletedFiles`, so no publish cleans them up either | `#127` §3.1 decision 7 |
| Nothing in this repository links to either tree. The only mentions are the PRD's notes about this issue, and `#90`'s spec, which compares against 0.2.4 as history | `git grep -E "(releases\|rcs)/dsh\b"` on `DEVELOP` |
| The wiki's release row still points readers at `releases/dsh`. It calls the tree legacy, says it "still serves 0.2.4", and links `#128` | `docs/wiki/Code-Based-Site-and-Reports.md` |
| Nothing in the legacy trees is unique. The pages that are missing from the new site are either renamed reports (`#90` spec §4) or empty templates (`user-guide`, `features`, `install`, `use`, `configure`, `vision`), each with only a "0.0.1-SNAPSHOT" heading. The one page with text, `vision-html`, is a 2010 RUP vision outline, and it is not DSH content: it comes from the parent-poms archetype templates (`infrastructure/maven-archetypes/*/archetype-resources/src/site/apt/vision-html.apt`). DSH deleted its copies in `6c69738d5` (2019-04-12), before 0.2.4 | the rendered bodies, `pdf/site.tmp/apt/` in the tree, and `git show --stat 6c69738d5` |
| GitHub Pages builds from the `gh-pages` branch root (`build_type: legacy`). There is no custom 404, so a removed path returns GitHub's default 404 | `gh api repos/MRISS-Projects/dsh/pages` |

## 3. Design

### 3.1 Decision (AC003): remove both trees, with no pointer page

`releases/dsh/` and `rcs/dsh/` are removed from `gh-pages` in one commit, as `snapshots/dsh/` was
in `#127`.

- **Nothing is lost.** The `gh-pages` history keeps every rendered file (`5a4bc0e8`, `f69affba`).
  The one page with text, `vision-html`, was a parent-poms archetype template, which parent-poms
  still holds.
- **Nothing links there.** The wiki row moved on 2026-09-30, and nothing in this repository links
  either tree.
- **One rule for all three trees.** `snapshots/dsh/` went without a pointer. Keeping the other two
  would leave the same question open twice.

**Not chosen:**

- *Keep them, labelled legacy.* This is today's state, the one the issue was raised to end. A
  0.2.4 site that answers 200 without a word about its age is the problem described in the issue
  summary.
- *Replace each with a pointer page.* A single `index.html` that redirects to `…/products/dsh/` saves
  only the root address. Every deep link, such as `releases/dsh/DSH-rest-api/…`, would still 404,
  because the modules have different names now (`DSH-rest-api` became `dsh-rest-api`). Saving
  those would take a redirect per page, for 0.2.4 pages nobody links to. A root pointer also leaves
  two directories of one file each on `gh-pages`, with no owner, for good.

### 3.2 The wiki row

The release row's note, "The legacy `releases/dsh` tree still serves 0.2.4 and is no longer
updated — see #128", becomes a past-tense note, as the Snapshots row has: the legacy
`releases/dsh` and `rcs/dsh` trees, 0.2.x leftovers, were removed (#128).

The RC row says `Working (0.3.0-SNAPSHOT)`. That is still true, because no RC has been staged
since 0.3.0, so it stays as it is.

The wiki is edited in the wiki repository. `wiki-sync.yml` copies the page to `docs/wiki/`, so
this branch does not edit `docs/wiki/` by hand: the file is auto-generated.

### 3.3 Order

1. Re-check AC001 and AC002 (§6.1).
2. Remove both trees from `gh-pages`, in one commit (§4).
3. Wait for Pages to report that commit as `built`, then check §6.2.
4. Edit the wiki row.
5. The PR into `DEVELOP` carries this spec and the PRD note. Removing a tree from `gh-pages` cannot
   go through a PR into `DEVELOP`, because it is a different branch. So it is done directly, as in
   `#127`, and the PR records it.

### 3.4 Out of scope

- A custom `404.html` for the Pages site.
- `snapshots/products/`, `releases/products/` and `rcs/products/`: they are not touched.
- The root `README.html` images and the licenses pages that `#90` §7 left out of scope.

## 4. Files to change

| Where | Change |
|---|---|
| `gh-pages` | `releases/dsh/` and `rcs/dsh/` removed, in one commit with `#127`'s message shape: what the trees were, when they were last published, that nothing links to them, and why no publish cleans them up |
| the wiki, `Code-Based-Site-and-Reports` | the release row's legacy note, past tense (§3.2) |
| `specs/product/PRD.md` | nothing on this branch. `dsh-reconcile-prd` marks `#128` closed after the merge |
| `specs/stories/128-point-wiki-release-link-at-products-path.md` | this spec, plus its verification results |

There is no Java, POM or workflow change, so gate 3, the local integration gate, does not apply:
nothing here touches an external system or a REST API entry point. Gates 1 and 2 apply as on every
pull request. CI runs them, and so does the local `mvn -B clean install` before each push. Their
results are in §8.5. Markdown lint runs on the spec.

## 5. Tasks

- [x] **T1.** Re-check AC001 and AC002 against the live site and the wiki (§6.1).
- [x] **T2.** Remove `releases/dsh/` and `rcs/dsh/` from `gh-pages` in one commit, and push it.
- [x] **T3.** Once Pages reports the commit `built`, check §6.2.
- [x] **T4.** Edit the wiki row (§3.2), and check §6.3 once `wiki-sync.yml` has run.
- [x] **T5.** Record the results in §8. Lint the spec.

## 6. Verification

### 6.1 AC001 and AC002, re-checked

- `https://mriss-projects.github.io/dsh/releases/products/dsh/` answers 200, and its `index.html`
  shows `Version: 0.3.2`.
- The wiki's "Current Official Release" row links that address.

### 6.2 The removal

- The `gh-pages` commit deletes 3,544 files. Every one of them is under `releases/dsh/` or
  `rcs/dsh/`, and nothing else changes: `git show --stat` lists no other path.
- `https://mriss-projects.github.io/dsh/releases/dsh/` and `…/rcs/dsh/` answer 404.
- `…/releases/products/dsh/`, `…/rcs/products/dsh/` and `…/snapshots/products/dsh/` still answer
  200.

### 6.3 The wiki

- The release row no longer says that a legacy tree "still serves" anything, and no link on the
  page points at `releases/dsh` or `rcs/dsh`.
- `docs/wiki/Code-Based-Site-and-Reports.md` on `DEVELOP` matches, after `wiki-sync.yml` runs.

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: after the 0.3.0 release, the site is confirmed under `releases/products/dsh/` | done on 2026-09-30 (0.3.2), and re-checked by T1 and §6.1 |
| AC002: the wiki row is updated, linking `project-info.html` if `#90` is still open | done on 2026-09-30. `#90` is closed, so the conditional does not apply. Re-checked by T1, and finished by T4 |
| AC003: a decision on the legacy `releases/dsh/` and `rcs/dsh/` trees: keep, remove, or leave a pointer page | §3.1, removed. T2, T3, §6.2 |

## 8. Verification results

### 8.1 T1, AC001 and AC002 re-checked, 2026-10-06

- `releases/products/dsh/` answers 200, and its `index.html` shows `Version: 0.3.2`.
  `rcs/products/dsh/` and `snapshots/products/dsh/` answer 200.
- The wiki's "Current Official Release" row links `…/dsh/releases/products/dsh/`.
- The baseline before T2: `releases/dsh/`, `rcs/dsh/` and `releases/dsh/DSH-rest-api/index.html`
  all answer 200.

### 8.2 T2, the removal tree

Built from `origin/gh-pages` at `fd4d322e6` with a temporary index, without checking the branch
out. The tree is `2b0a910df`. `git diff-tree` against the parent shows 3,544 deletions and no
other change, and no path outside `releases/dsh/` and `rcs/dsh/`.

The commit is `9c1316b79`, pushed as a fast-forward of `gh-pages` from `fd4d322e6`. `git show
--stat` reports `3544 files changed, 408271 deletions(-)`.

### 8.3 T3, the live site, 2026-10-06

Pages reported `9c1316b79` as `built`. Then:

| Address | Before | After |
|---|---|---|
| `releases/dsh/` | 200 | 404 |
| `rcs/dsh/` | 200 | 404 |
| `releases/dsh/DSH-rest-api/index.html` | 200 | 404 |
| `releases/products/dsh/` | 200 | 200 |
| `rcs/products/dsh/` | 200 | 200 |
| `snapshots/products/dsh/` | 200 | 200 |

### 8.4 T4, the wiki, 2026-10-06

- Wiki commit `a7f5ae9`. The release row's note now says the legacy `releases/dsh` and `rcs/dsh`
  trees, 0.2.x leftovers, were removed (#128). No link on the page points at either tree.
- `wiki-sync.yml`'s schedule has not run since 2026-07-21, so it was dispatched by hand, in run
  [`37508413576`](https://github.com/MRISS-Projects/dsh/actions/runs/37508413576). That opened
  [#155](https://github.com/MRISS-Projects/dsh/pull/155), whose diff is that one row. §6.3's
  `DEVELOP` check holds once #155 merges.

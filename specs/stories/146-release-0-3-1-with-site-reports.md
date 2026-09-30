---
issue: 146
slug: release-0-3-1-with-site-reports
parent_branch: 0.3.x
wave: 1
milestone: 0.3.2
---

# Story 146 — Release DSH 0.3.1 with a release site that carries its test reports and coverage badge

## 1. Story

**As a** reader of DSH's `master` README and its release site
**I want** a DSH 0.3.1 hotfix release whose site carries real test and coverage reports and a working
coverage badge
**So that** the released documentation shows the coverage DSH actually has, rather than an empty
report and a broken badge image

## 2. Context

- Wave: 1, upstream step 0 (`specs/product/PRD.md` §4, Wave 1), DSH milestone `0.3.1-SNAPSHOT`
- Issue: [#146](https://github.com/MRISS-Projects/dsh/issues/146)
- Parent branch: `0.3.x`, the hotfix line, at `6932be686` (`0.3.1-SNAPSHOT`, parent `3.9.0`) when
  this spec was written. The task branch is `issue-146-release-0-3-1-with-site-reports`.
- Upstream half: [`parent-poms#95`](https://github.com/MRISS-Projects/parent-poms/issues/95) (empty
  release reports) and [`#96`](https://github.com/MRISS-Projects/parent-poms/issues/96) (no coverage
  badge), parent-poms milestone `3.9.2-SNAPSHOT`. Their fix is designed in parent-poms' own spec,
  `specs/95-<slug>.md` on parent-poms `master`, not here.
- Evidence of the defect: DSH 0.3.0's release,
  [run 36606687680](https://github.com/MRISS-Projects/dsh/actions/runs/36606687680).

### 2.1 What was established while writing this spec

| Fact | Evidence |
|---|---|
| parent-poms `master` is `3.9.0` plus only `#93`, a fix to the `commit-readme` placeholder check | `git diff --stat mriss-parent-3.9.0 origin/master`: five files, all under `.github/` and `specs/`. `master` is at `f7600ffe`, POM version `3.10.0-SNAPSHOT` |
| A parent-poms release always advances MINOR and resets FIX | `deploy.yml`, `Release Deploy`: `NEXT_DEV_VERSION="${MAJOR}.$((MINOR + 1)).0-SNAPSHOT"`. Releasing `3.9.2` leaves `master` at `3.10.0-SNAPSHOT` with no manual step |
| A rehearsal accepts a `-SNAPSHOT` parent, and a real release does not | `parent-poms/specs/72-dry-run-release-workflows.md:104`: dry-run `check-dependency-snapshots` logs `Ignoring SNAPSHOT dependencies and plugins`. DSH `#117`'s green rehearsal, [run 36162240349](https://github.com/MRISS-Projects/dsh/actions/runs/36162240349), ran at `fc7902c6a`, whose parent is `3.9.0-SNAPSHOT`. A real `release:prepare` refuses a SNAPSHOT parent |
| `project-hotfix.yml` releases whatever branch it is given | `project-hotfix.yml:57`: the checkout uses `ref: ${{ inputs.branch_name }}`. The version it releases is the checked-out POM's (`Read hotfix release number`, line 182). So a dry run can target the task branch itself |
| DSH's wrapper calls the upstream workflow at `@master` | `.github/workflows/hotfix.yml`: `uses: MRISS-Projects/parent-poms/.github/workflows/project-hotfix.yml@master`. A workflow change upstream reaches DSH as soon as it merges |

## 3. The defect, in brief

Full analysis in `parent-poms#95`. In `project-hotfix.yml`, as in `project-release.yml`, the only test
run of the released code is inside `release:perform`, in `target/checkout`. `Merge Release Tag to
Master` then begins with `rm -rf target/checkout` (line 229) and clones `master` afresh. `Deploy Site
to gh-pages` runs `site-deploy` in that fresh tree. The site lifecycle reaches neither `test` nor
`verify`, and `-DintegrationTests` is never passed. So surefire, failsafe, per-module JaCoCo and the
aggregate report all render empty, and `dsh-coverage-report`'s badge, which is bound to `verify` (`#104`),
is never written.

## 4. Design

### 4.1 Decisions

1. **The pin is unconditional.** `#146`'s AC001 left it to where the upstream fix lands: a
   workflow-only fix would reach DSH at `@master` with no pin. This spec pins anyway, because
   parent-poms releases `3.9.2` regardless. Its `master` is re-versioned to `3.9.2-SNAPSHOT` for the
   fix and released from there. Pinning `0.3.x` to that release keeps the 0.3.1 release's parent the
   one that was released alongside its fix, whatever files the fix touched. AC001 records this reason.
2. **parent-poms' fix line is `master`, re-versioned. It is not a `3.9.x` branch.** A branch cut from
   `mriss-parent-3.9.0` would add nothing: every consumer calls the reusable workflows at `@master`,
   so the workflow half of the fix must be on `master` anyway. `master` holds nothing beyond `3.9.0`
   except `#93`, which is itself a fix and belongs in a patch release.
3. **The `-SNAPSHOT` pin never reaches `0.3.x`** (option A, chosen 2026-09-29). This branch carries
   both pin commits and rehearses against itself. It opens one PR into `0.3.x`, after the re-pin to
   the released `3.9.2`. `0.3.x` goes from `3.9.0` straight to `3.9.2`.
4. **A second rehearsal on `0.3.x` precedes the real release.** The first rehearsal, on this branch,
   proves the fix against `3.9.2-SNAPSHOT`. The second, on `0.3.x` after the merge, is AC002 as the
   issue words it, and it proves the exact tree and parent the real release will use.

### 4.2 Cross-repository order

`P` steps are in parent-poms, `T` steps are the DSH tasks in §6.

| # | Where | Step | Waits for |
|---|---|---|---|
| T1 | DSH | Red rehearsal on this branch, against `3.9.0` and the unfixed `@master` workflow | this spec pushed |
| P1 | parent-poms | `./set-version.sh 3.9.2-SNAPSHOT` on `master`, committed referencing `#95` | T1 |
| P2 | parent-poms | Fix `#95`/`#96` through their own spec and PR into `master` | P1 |
| P3 | parent-poms | Dispatch `deploy.yml` with `release_type: snapshots` on `master`, which deploys `3.9.2-SNAPSHOT` | P2 |
| T2 | DSH | Pin this branch to `3.9.2-SNAPSHOT` | P3 |
| T3 | DSH | Green rehearsal on this branch | T2 |
| P4 | parent-poms | Clear the milestone (§4.3), rename it to `3.9.2`, dispatch `deploy.yml` with `release_type: releases`. The result is tag `mriss-parent-3.9.2`, with `master` back at `3.10.0-SNAPSHOT` | T3 |
| T4 | DSH | Re-pin this branch to `3.9.2` | P4 |
| T5 | DSH | Ship: local review, PR into `0.3.x`, review cycle, merge by you | T4 |
| T6 | DSH | Confirming rehearsal on `0.3.x` | T5 merged |
| T7 | DSH | Real `hotfix.yml` dispatch on `0.3.x`, which releases 0.3.1 | T6 |

**T1 must run before P2 merges.** Once the fix is on parent-poms `master`, the unfixed workflow no
longer exists to be run.

**Nothing destined for `3.10.0` merges to parent-poms `master` between P1 and P4.** Anything that did
would ship in `3.9.2`. That milestone's issues have not started, so this costs nothing today.

### 4.3 A circularity to settle at P4

CLAUDE.md requires a parent-poms milestone to be cleared before its release. But `#95` AC004 and
`#96` AC003 are proven only by DSH's 0.3.1 release (T7), which needs `3.9.2` released first (P4). As
the issues stand, they cannot close before the release that ships their fix.

**Resolution, applied at P4:** before the release, revise `#95` AC004 and `#96` AC003 to say that the
fix is proven by DSH `#146`'s green rehearsal (T3), and that the confirming release is tracked in DSH
`#146` AC004/AC005, which already state the same URL checks. Then close both issues on T3's evidence
and release. Their other criteria are all provable at T3. The revision is a comment and an edit on each
issue, made by the parent-poms side, with the T3 run link.

### 4.4 Out of scope

- The fix itself, which belongs to `parent-poms#95`/`#96` and their spec. If a rehearsal from here
  shows it incomplete, that goes back upstream and is not patched on this branch (CLAUDE.md, "Fix
  shared gaps upstream").
- Any change to `.github/workflows/hotfix.yml`. It already passes `development_branch: DEVELOP` (`#117`)
  and the Mongo properties (`#114`), and takes its behaviour from `@master`.
- Republishing 0.3.0's site, re-pinning `DEVELOP` to `3.10.0` (Wave 1 upstream step 3), `ci.yml`,
  `staging.yml`, `release.yml`, and any product code.
- `specs/product/PRD.md`. `dsh-reconcile-prd` (step 8) owns its update after the merge. It is listed
  here so a reviewer does not flag it as missed.

## 5. Files to change

### 5.1 `pom.xml` (root)

Lines 9-13, `<parent>`: `<version>3.9.0</version>` → `3.9.2-SNAPSHOT` (T2) → `3.9.2` (T4). Nothing
else. The modules inherit their parent from the root reactor, so the root is the only file with a
parent version.

## 6. Tasks

No `.java` file changes, so the red/green cycle is on the workflow: T1's rehearsal against the
unfixed workflow is the failing test, and T3's is green. Every local Maven run follows CLAUDE.md,
"Always log local Maven runs".

- [x] **T1 — red.** After this spec is pushed and before `parent-poms#95`'s fix merges, dispatch the
      rehearsal in §7.1 on this branch. Expect green overall, since the defect writes nothing wrong in
      a dry run. The log shows no `maven-surefire-plugin` or `jacoco:report` execution between
      `Merge Release Tag to Master` and `Deploy Site to gh-pages`, and no `badges/jacoco.svg`
      written. Record the run link and the lines against AC007.
      [Run 36614968935](https://github.com/MRISS-Projects/dsh/actions/runs/36614968935), at `c70d50ab6`
      against parent-poms `f7600ffe`, is green. Findings in §7.4.
- [x] **T2 — pin to `3.9.2-SNAPSHOT`.** After P3. Confirm that it resolves:
      `mvn -B -U -N help:evaluate -Dexpression=project.parent.version -DforceStdout`, logged to
      `.logs/mvn-help-evaluate.log`. Edit §5.1. Run gate 1, `mvn -B -U clean install`, logged to
      `.logs/mvn-clean-install.log`, and report its exit code. Commit
      `build(#146): pin the parent to 3.9.2-SNAPSHOT`.
      `e36d7c612`. It resolved `products-3.9.2-20260929.220215-2.pom`, P3's build from parent-poms
      `master` at `7cfc6d30`, which carries `release.forked.test.arguments`. Gate 1: `maven exit=0`,
      every module's coverage checks met.
- [x] **T3 — green.** Push. Dispatch §7.1 on this branch. Check §7.2. Record the run link against
      AC001 and AC002, and hand it to the parent-poms side for §4.3.
      [Run 36638594571](https://github.com/MRISS-Projects/dsh/actions/runs/36638594571), green. The
      parent-poms fix had already been proven against its PR branch before the merge (`parent-poms#97`
      review round 1, spec Tasks 7 and 8), so this is the confirming run against merged `master`.
      §7.2 point by point:
  1. Prepare's fork ran 146 tests: 127 unit and 19 integration, with 13
     `failsafe:integration-test`. That fork is the tree the site step builds from, since
     `parent-poms#95` moved the site to the workspace, detached at the tag. The site step ran no
     tests.
  2. The site step logged 21 × `Loading execution data file` (`jacoco.exec` ×8,
     `jacoco-it.exec` ×5). Its only 5 skips are the modules without production classes. T1 had
     13 skips and no loads.
  3. `jacoco-badge:badge` ran in `dsh-coverage-report`, and the scm-publish set lists
     `addition releases/products/dsh/dsh-coverage-report/badges/jacoco.svg`.
  4. `all 6 declared write point(s) announced exactly once`, and
     `the remote is byte-for-byte as it was before the run`.
  5. `merge-to-develop: carried 1 path(s) from v0.3.1 into DEVELOP; 0 lost`.
  6. The parent was `3.9.2-SNAPSHOT`. The log has no download line for it: its first resolution
     is in `Read hotfix release number`'s `-q` `help:evaluate`. The proof is item 1: failsafe runs
     only under `-DintegrationTests`, which reaches prepare's fork only through `3.9.2-SNAPSHOT`'s
     `release.forked.test.arguments`. T1, on `3.9.0`, had no failsafe execution.

  Remote before and after: `master` `7647692ab`, `DEVELOP` `b3a65eaa9`, `0.3.x` `6932be686`,
  `gh-pages` `b818352ed`, and no `v0.3.1` tag.
- [x] **T4 — re-pin to `3.9.2`.** After P4. Edit §5.1. Run gate 1 again (`-U`, logged). Commit
      `build(#146): pin the parent to the released 3.9.2`. Record AC003.
      `4eb55bae3`. P4 was parent-poms
      [run 36642378743](https://github.com/MRISS-Projects/parent-poms/actions/runs/36642378743), which
      tagged `mriss-parent-3.9.2` and moved parent-poms `master` to `3.10.0-SNAPSHOT` (`2ffe1a9c`).
      `#95` and `#96` were closed on the rehearsal evidence, and the `3.9.2` milestone was closed.
      §4.3's revision of their ACs was skipped at the human's direction. Gate 1 downloaded the
      released `products-3.9.2.pom`, which carries `release.forked.test.arguments`: `maven exit=0`,
      coverage checks met in all 8 code modules. AC003 is ticked once the merge puts this on `0.3.x`.
- [x] **T5 — ship.** `dsh-ship-story`, then `dsh-pr-cycle`, into `0.3.x`. You merge.
      [PR #147](https://github.com/MRISS-Projects/dsh/pull/147), merged as `49e899855`. The local
      review raised nothing, and all CI checks were green.
- [x] **T6 — confirming rehearsal on `0.3.x`.** §7.1 with `--ref 0.3.x -f branch_name=0.3.x`. Check
      §7.2. This is AC002 as the issue words it.
      [Run 36648331265](https://github.com/MRISS-Projects/dsh/actions/runs/36648331265), green.
  - Prepare ran 146 tests (127 unit and 19 integration). The site step ran none, and logged 21
    loads and 5 skips.
  - The badge was in the publish set.
  - 6 of 6 write points were announced, and the remote was unchanged.
- [x] **T7 — release.** §7.1 with `--ref 0.3.x -f branch_name=0.3.x`, without `dry_run`. Then check
      §7.3 and record AC004 to AC006.
      **It released 0.3.2, not 0.3.1**, after three dispatches:
  1. [Run 36708887393](https://github.com/MRISS-Projects/dsh/actions/runs/36708887393) failed in
     `release:prepare`'s forked build.
     - `dsh-rest-api`'s `spring-boot:start` failed 0.95 s in with `Could not contact Spring Boot
       application over JMX on port 40139. Please make sure that no other process is using that
       port`, and the app printed nothing.
     - Nothing was written.
     - The suspected cause is a race between `reserve-network-port` and the containers'
       Docker-published host ports. Not proven, and not reproduced in six other runs of the same
       configuration. No issue was raised, by agreement, because it did not recur.
  2. [Run 36712998895](https://github.com/MRISS-Projects/dsh/actions/runs/36712998895) passed
     prepare. `release:perform` then got HTTP 500 from GitHub Packages on the last module,
     `dsh-coverage-report:pom:0.3.1`. The result is a partial 0.3.1:
     - tag `v0.3.1` (`d04438299`);
     - 12 of 13 artifacts;
     - `0.3.x` at `0.3.2-SNAPSHOT`;
     - no `master`, site or `DEVELOP` write.

     The line moved on to 0.3.2 rather than delete the tag, force-push `0.3.x` and delete
     packages, and `v0.3.1` stays as an incomplete release. The missing retry is
     [`parent-poms#98`](https://github.com/MRISS-Projects/parent-poms/issues/98), to be addressed
     later.
  3. [Run 36715711599](https://github.com/MRISS-Projects/dsh/actions/runs/36715711599), green,
     released **0.3.2**.

  The DSH milestone `0.3.1-SNAPSHOT` was renamed to `0.3.2`. Evidence is also on
  [#146](https://github.com/MRISS-Projects/dsh/issues/146#issuecomment-5911760247).

Gate 3 does not apply: no code touching an external system or a REST entry point changes. The parent
change does alter the build, and the rehearsals run the integration tests under `-DintegrationTests`,
which covers it.

T6 and T7 happen after this branch is merged. Their evidence goes on `#146` as a comment. The ticked
ACs go into this spec with a `docs(#146)` commit on `DEVELOP` once the release has merged `v0.3.1`
back into it, as parent-poms `#93` recorded its post-merge run.

## 7. Verification

### 7.1 Dispatch

```bash
gh workflow run hotfix.yml --ref issue-146-release-0-3-1-with-site-reports \
  -f branch_name=issue-146-release-0-3-1-with-site-reports -f dry_run=true
```

`--ref` selects which copy of the wrapper runs, and `branch_name` selects the tree that is released.
They match in every dispatch here.

### 7.2 A rehearsal is green when its log shows all of the following

Every item is read from the log, not inferred from the run's conclusion.

1. Unit tests **and** integration tests executing in the tree `Deploy Site to gh-pages` builds from,
   with surefire and failsafe summaries reporting a non-zero test count. That tree is either
   `target/checkout` or whatever tree the upstream fix makes the site step use.
2. `jacoco:report` and `jacoco:report-aggregate` reading real `jacoco.exec`/`jacoco-it.exec`, not
   `Skipping JaCoCo execution due to missing execution data file`.
3. `dsh-coverage-report`'s badge written to `…/badges/jacoco.svg`.
4. `rehearsal-verify`: `all 6 declared write point(s) announced exactly once`, and
   `the remote is byte-for-byte as it was before the run`.
5. `merge-to-develop: carried <n> path(s) from v0.3.1 into DEVELOP; 0 lost`.
6. The parent resolved is the one named in §5.1 at that point: `3.9.2-SNAPSHOT` at T3, `3.9.2` at T6.

Before and after, against the remote: no `v0.3.1` tag, and `master`, `DEVELOP`, `0.3.x` and `gh-pages`
unchanged. Record their SHAs in the task.

### 7.3 After the real release

Base: `https://mriss-projects.github.io/dsh/releases/products/dsh/`. Allow for gh-pages propagation,
re-checking for up to about ten minutes before treating a 404 as real.

```bash
B=https://mriss-projects.github.io/dsh/releases/products/dsh
for p in failsafe.html dsh-rest-api/jacoco/index.html dsh-data/jacoco/index.html \
         dsh-coverage-report/badges/jacoco.svg; do
  printf '%s %s\n' "$(curl -s -o /dev/null -w '%{http_code}' "$B/$p")" "$p"
done
curl -s "$B/surefire.html" | grep -o -E 'Tests</th>.{0,200}' | head -3
curl -s "$B/dsh-coverage-report/jacoco-aggregate/index.html" | grep -o -E 'Total.{0,300}' | head -1
git fetch -q && git tag -l v0.3.1 && git merge-base --is-ancestor v0.3.1 origin/DEVELOP && echo reachable
MSYS_NO_PATHCONV=1 git show origin/DEVELOP:pom.xml | sed -n 9,13p
```

Then open `master`'s README on GitHub and confirm that the badge renders as an image.

### 7.4 What T1's red rehearsal showed

[Run 36614968935](https://github.com/MRISS-Projects/dsh/actions/runs/36614968935), 2026-09-29. The job
log reports every step as `UNKNOWN STEP`, so the steps below are delimited by their `##[group]Run`
lines and the `REHEARSAL <point>:` markers.

| Step (log lines) | Tests | Coverage | Badge |
|---|---|---|---|
| `Maven Release` / `release:prepare` (538-10158) | 8 surefire summaries: 51 + 36 + 22 + 10 + 2 + 2 + 2 + 2 = **127 tests**, the staging count. No failsafe run, because `-DintegrationTests` is not passed | not reported here | the plugin is resolved, but nothing is published from this tree |
| `Maven Release Perform` (11295-11369) | **none**: under `-DdryRun=true`, `release:perform` builds nothing | none | none |
| `Deploy Site to gh-pages` (11428-191317) | **none**: no `surefire:test`, no `failsafe:integration-test`, no `Tests run:` line | all 13 `jacoco:report` log `Skipping JaCoCo execution due to missing execution data file`. `report-aggregate` analyses bundles with no execution data | none written |

AC007 holds. Beyond the red itself, T1 found two things:

1. **In a rehearsal, `release:perform` runs nothing.** A fix that keeps `perform`'s `target/checkout`
   output for the site step would pass a real release but could not be proven by any rehearsal: the
   rehearsal site would still be empty, and T3 would read the same as T1. For T3 to prove the fix,
   the tests must run in the tree `Deploy Site to gh-pages` builds from, as `project-staging.yml`
   does. This goes to `parent-poms#95` as design input.
2. **The dry-run site step lists 148,202 `- delete` lines**, covering all of `rcs/` and the `gh-pages`
   root files, 13 times over, once per module's scm-publish. This is dry-run reporting only. 0.3.0's
   real release made ten `gh-pages` commits, `6d7a18b16`..`b818352ed`, each of them insertions only
   with no deletions, and `rcs/` still serves its badge. It is not a finding against the fix, and a
   reader of T3's log should not mistake it for one.

The rehearsal also ended with `rehearsal: all 6 declared write point(s) announced exactly once`
and `merge-to-develop: carried 1 path(s) from v0.3.1 into DEVELOP; 0 lost`. The one path is this
spec. Remote refs before the run: `master` `7647692ab`, `DEVELOP` `b3a65eaa9`, `0.3.x` `6932be686`,
`gh-pages` `b818352ed`, and no `v0.3.1` tag.

## 8. Acceptance criteria

From the issue:

- [x] **AC001** — Before the rehearsal, this branch's root `pom.xml` names `3.9.2-SNAPSHOT`, which is
      deployed to GitHub Packages before the pin. Why: §4.1 (1). The fix line is parent-poms `master`
      re-versioned, and `3.9.2` is released whether or not the fix touches a POM. Evidence: P3's
      deploy run and T2's commit.
      P3: [run 36636958954](https://github.com/MRISS-Projects/parent-poms/actions/runs/36636958954). T2:
      `e36d7c612`. As it turned out, the fix does touch a POM (`products/pom.xml`,
      `release.forked.test.arguments`), so the pin is load-bearing and not only a matter of
      consistency.
- [x] **AC002** — A `hotfix.yml` dispatch on `0.3.x` with `dry_run=true` (T6) meets every point of §7.2.
      The same check on this branch against `3.9.2-SNAPSHOT` (T3) precedes it.
      T6, [run 36648331265](https://github.com/MRISS-Projects/dsh/actions/runs/36648331265).
- [x] **AC003** — Before the real release, the root `pom.xml` names the released `3.9.2`, with no
      `-SNAPSHOT` (T4), and that is what `0.3.x` carries after the merge. `49e899855`.
- [x] **AC004** — The real dispatch (T7) releases `0.3.1`, tagged `v0.3.1`. The site checks in §7.3
      hold: root `surefire.html` reports a non-zero test count, and `failsafe.html`,
      `dsh-rest-api/jacoco/index.html` and `dsh-data/jacoco/index.html` return 200.
      `dsh-coverage-report/jacoco-aggregate` reports more than 0% line coverage.
      **Met by 0.3.2**, tagged `v0.3.2` (`47c26fe9`), with all 13 artifacts in GitHub Packages (T7
      explains why 0.3.2):
  - root `surefire.html` reports **127 tests, 100%**, where it had 0;
  - `failsafe.html`, `dsh-rest-api/jacoco/index.html` and `dsh-data/jacoco/index.html` return
    **200**, where they returned 404;
  - the aggregate reports **98%** lines (30 of 2,028 missed), where it had 0%.

  Checked live with a cache-busting query. For a few minutes after the Pages build of
  `38a701903`, the CDN kept serving 0.3.0.
- [x] **AC005** — `…/dsh-coverage-report/badges/jacoco.svg` returns 200, and the badge renders on
      `master`'s README.
      It returns **200** and reads `jacoco 98%`. `master`'s README (`1e476e64b`) links it.
- [x] **AC006** — `DEVELOP` is at `0.4.0-SNAPSHOT` with `v0.3.1` reachable from it. Record the parent
      version `DEVELOP` names afterwards, expected to be `3.9.2`.
      With `v0.3.2`: `DEVELOP` `714d3c27e` is at `0.4.0-SNAPSHOT`, `v0.3.2` is reachable from it, and
      it names parent **`3.9.2`**, as expected. Wave 1 upstream step 3's re-pin to `3.10.0` supersedes
      that.

Added by this spec:

- [x] **AC007** — T1's rehearsal against the unfixed workflow shows no test or coverage execution
      before the site step and no badge written. This proves T3 is green because of the upstream fix.
      [Run 36614968935](https://github.com/MRISS-Projects/dsh/actions/runs/36614968935), §7.4.
- [x] **AC008** — `0.3.x` never names a `-SNAPSHOT` parent: its `pom.xml` history goes from `3.9.0` to
      `3.9.2` in one merge.
      On `0.3.x`'s first-parent history: `6932be686` names `3.9.0`, and every commit from
      `49e899855` (the merge of #147) to `1960098f0` names `3.9.2`.
- [x] **AC009** — CI is green on the PR into `0.3.x`. No `.java` is in the diff, so the coverage gate
      is unaffected. PR #147: `Build, Test and Coverage Gate`, `Check Spec File References`,
      `Validate Markdown Files` and `Validate OpenAPI Specification` all passed.

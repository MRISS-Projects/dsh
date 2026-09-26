---
issue: 87
slug: bump-maven-to-3-9-16
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 87 — Update Maven pinned version from 3.9.9 to 3.9.16 in documentation and GitHub Actions

> **This story is one half of a pair.** `MRISS-Projects/parent-poms#59` carries the same title and
> makes the same bump in the repository whose reusable workflows build every DSH staging and
> release. The two are delivered as one cycle, like `#85`/`#57` and `#86`/`#58` — see §7. The DSH
> half does **not** wait on a parent-poms release; it waits on `#59` merging to `master`.

## 1. Story

**As a** developer reading a build result, in either repository
**I want** every workflow and every install instruction to name one Maven version, and every
workflow to prove it ran that version
**So that** CI, staging, release and a local build are the same toolchain, and a difference between
them points at a change someone made

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`, "DSH `#87` is back in this wave")
- Twin issue upstream: `MRISS-Projects/parent-poms#59`, milestone `3.9.0-SNAPSHOT`, which it still
  gates together with `#70`
- Supersedes the version `#85` and `#86` wrote. Both specs say so and both named this story as the
  one that rewrites their lines.

**Why this wave, not Wave 1.** The PRD records the reasoning and it is not repeated here in full:
DSH's four release wrappers call parent-poms' reusable workflows at `@master`, so shipping `#59`
alone would move DSH's staging and releases to 3.9.16 while `ci.yml` and `api-testing.yml` stayed
on 3.9.9 — the CI-versus-release drift `#99` was built to remove.

**3.9.16 is real and current, verified on 2026-09-26.**
`https://archive.apache.org/dist/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip` answers
`200`, and `https://dlcdn.apache.org/maven/maven-3/` lists 3.9.16 and nothing later in the 3.9 line.
The banner, from the distribution already installed at `~/apps/apache-maven-3.9.16`:

```text
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
```

**Local builds on this box already run 3.9.16.** `mvn` on `PATH` is `~/apps/apache-maven-3.9.16`,
and has been since before `#86` (see `docs/superpowers/plans/2026-09-16-ai-driven-development-process.md`).
Every local `mvn -B install` recorded in the story specs since then exercised the target version.
What this story moves is CI and the release path, which are still on 3.9.9.

## 3. A gap the issue does not name — four workflows pin Maven but never check it

`#86` established that a pin is not evidence: Maven under `-B` prints no version banner, so a
`stCarolas/setup-maven` step that failed to take effect leaves a green build silently back on the
runner image's Maven. It added a `Verify Maven version` step to fix that. Today that step exists in
four workflows and is missing from four:

| Workflow | Pins | Asserts |
|---|---|---|
| DSH `ci.yml`, `api-testing.yml` | yes | yes (`#86`) |
| parent-poms `build.yml`, `deploy.yml` | yes | yes (`#58`) |
| parent-poms `project-stage.yml`, `project-staging.yml`, `project-release.yml`, `project-hotfix.yml` | yes | **no** |

`#58` left those four alone because they were "already pinned", which was true of the pin. They are
the four that DSH's `stage.yml`, `staging.yml`, `release.yml` and `hotfix.yml` hand off to — the
path that produces what DSH ships.

A version bump is the moment this gap costs the most. Moving the pin is the change most likely to
meet a `setup-maven` failure (a version the action cannot fetch, a download mirror that lags), and
without the assertion the first staging run after the bump would be green whether or not 3.9.16
ran. **Settled with the repo owner on 2026-09-26: `#59` adds the assertion to all four.** It is the
same two steps, byte for byte, that `build.yml` already carries.

## 4. Files to change in parent-poms (`#59`)

All on `issue-59-bump-maven-to-3-9-16`, cut from parent-poms `master`, merged back by PR.

### 4.1 `build.yml` and `deploy.yml` — move the pin and the guard

In each, four lines change: the step name, `maven-version`, the `grep -qF` pattern and the error
text. After the change the block reads:

```yaml
      - name: Set up Maven 3.9.16
        uses: stCarolas/setup-maven@v5
        with:
          maven-version: '3.9.16'

      - name: Verify Maven version
        run: |
          mvn -version
          mvn -version | grep -qF 'Apache Maven 3.9.16 ' || {
            echo "::error::Expected Maven 3.9.16. The 'Set up Maven 3.9.16' step did not take effect, so this build would have run the runner image's Maven."
            exit 1
          }
```

`build.yml:57-68`, `deploy.yml:65-76` today. `maven-version` stays quoted: `#86`'s local review
found that an unquoted `3.10` parses as the float `3.1`, and quoting was applied upstream then.

### 4.2 The four `project-*.yml` workflows — move the pin, add the guard

Change the `Set up Maven` step exactly as §4.1, and insert the `Verify Maven version` step from §4.1
**immediately after it**, verbatim:

| File | `Set up Maven` step today | First `mvn` invocation |
|---|---|---|
| `project-stage.yml` | 55-58 | 139 |
| `project-staging.yml` | 67-70 | 166 |
| `project-release.yml` | 97-100 | 197 |
| `project-hotfix.yml` | 78-81 | 176 |

The guard therefore runs after `setup-java` and before every `mvn` in each file.

**The error text carries no documentation path.** DSH's copy ends
`See docs/devops/README.md, 'Workflow Reference'.`; these workflows run inside *every* consuming
repository, most of which have no such file. That is the same reason `#86` §7.3 dropped the pointer
from `build.yml` and `deploy.yml`, so all six upstream blocks end up identical — §9.3 checks that
mechanically.

**It is safe under `dry_run`.** `project-release.yml` and `project-hotfix.yml` rehearse with
`dry_run: true` (`#72`). The guard makes no remote write and needs no `RH_*` variable, so it runs
the same in a rehearsal as in a real release, which is what makes §9.4 possible.

### 4.3 `infrastructure/src/site/markdown/maven.md`

| Lines | Change |
|---|---|
| 11-12 | `**3.9.9**` and the archive URL become `3.9.16` |
| 23, 33 | `apache-maven-3.9.9` in the two `M2_HOME` examples becomes `apache-maven-3.9.16` |
| 49-50 | Linux sample: banner and `Maven home:` line — see below |
| 82-83 | Windows sample: banner and `Maven home:` line — see below |

In both sample blocks exactly two lines change, and they become:

```text
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: /home/[YOUR_USER]/apps/apache-maven-3.9.16
```

```text
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: C:\data\apache-maven-3.9.16
```

The banner is the real one from §2, not a guess at a hash. The `Java version`, locale and `OS name`
lines are untouched: the JDK has not changed, and `#85` AC004 already records what those lines do
and do not claim (the Windows block is a capture with genericised paths; the Linux block is an
illustration whose OS line no Windows run could supply). This story does not reopen that.

### 4.4 `CLAUDE.md:128`

`Maven 3.9.9 (pinned via ...)` becomes `Maven 3.9.16 (pinned via ... and asserted by a
'Verify Maven version' step)`, because after §4.2 the assertion is standard across all six
workflows, and that line is where the standard is stated.

### 4.5 `specs/github-actions-reusable-workflows.md`

The issue names this file specifically. It is a design spec for the reusable workflows, and it
describes the toolchain they run, so it moves with them.

| Lines | Change |
|---|---|
| 278 | Table row: `**3.9.16**`, and `maven-version: '3.9.16'` |
| 280-283 | "Why 3.9.9?" — **rewritten, not renumbered**; see below |
| 294-297 | Setup snippet: step name and `maven-version: '3.9.16'`, plus the `Verify Maven version` step from §4.1 |
| 392-396, 532-536, 714-718, 1003-1007 | Step 3 of each of the four workflow sections: name and `maven-version: '3.9.16'`, then a step 4 naming the version assertion |
| 1352, 1369 | Acceptance table rows: `3.9.16`, and row 1369 adds the assertion |

Every `maven-version` in this file is unquoted today. They become quoted, matching the workflows,
for the reason in §4.1 — a spec whose snippet would break at `3.10` is a snippet someone will copy.

**The rationale paragraph.** It currently justifies 3.9.9 as "the latest stable Maven 3 release",
which stopped being true some time ago and would be the wrong reason to hold on 3.9.16 as well. The
replacement states what actually holds: 3.9.16 is the newest 3.9.x at the time of the bump; it is
compatible with `maven-release-plugin` 3.1.1, which needs Maven 3.6.3+; and the pin exists for
reproducibility, so it moves by a deliberate change in both repositories, never by the runner image.
No claim that it is "the latest" goes in — that is the sentence that rotted.

**Found while building — no step 4 was added.** The table above planned "then a step 4 naming the
version assertion" in each of the four workflow sections. Those sections number their steps, and
the file's Implementation Notes cite them as "§6.3 steps 9 & 10", "§6.4 step 9" and so on, so a new
step 4 would have renumbered four sections and silently broken those references. Step 3 became
**"Set up Maven 3.9.16, then assert it"** instead, and it points at the §5 setup snippet rather than
repeating it — one copy of the guard in the file, not five that can drift. `parent-poms@6bf0a368`.

## 5. Files to change in DSH (`#87`)

### 5.1 `.github/workflows/ci.yml` and `.github/workflows/api-testing.yml`

The same four-line change as §4.1, in `ci.yml:58-77` and `api-testing.yml:62-81`. The error text
keeps its DSH-only pointer to `docs/devops/README.md`. The two files stay identical to each other in
these steps, as `#86` left them.

The comment above each guard ends:

```text
# stops a future 3.9.90 satisfying a check written for 3.9.9.
```

It becomes `stops a future 3.9.160 satisfying a check written for 3.9.16.` — the same argument for
the trailing space, restated for the version the check now names. §9.1 tests exactly that case.

### 5.2 `src/site/markdown/README.md`

Line 75 (`* Maven 3.9.9`), 155 (download line and URL), 162 and 171 (`M2_HOME`), and the two sample
blocks at 184-185 and 208-209, changed exactly as §4.3 — the two pages were brought into the same
shape by `#85`/`#57` and they stay that way.

### 5.3 Root `README.md` — regenerated, never hand-edited

`CLAUDE.md` and `#85` §4.2 both say it: root `README.md` is produced from
`src/site/markdown/README.md` by the inherited `readme-generation` profile. It is regenerated by the
`staging.yml` dispatch in §9.4, which lands an `Auto-generated README.md [skip jenkins]` commit on
the task branch. That same dispatch is this story's evidence for `project-staging.yml`, so one run
does both.

Lines 587-588 are rows of the generated issue-history table. They quote the *titles* of `#85` and
`#86`, which contain `3.9.9`, and they will keep doing so — they are generated from closed
milestones, and rewriting a closed issue's title to satisfy a grep would falsify the history. §8's
AC001 grep excludes them by shape, not by line number.

### 5.4 `docs/devops/README.md`

**Line 158** — `Maven **3.9.9**` becomes `Maven **3.9.16**`.

**Lines 168-170** — the paragraph "The four release wrappers run no Maven of their own. The reusable
workflows they call in `parent-poms` pin the same 3.9.9, ..." becomes: they pin the same 3.9.16
**and assert it the same way**, as do that repository's own `build.yml` and `deploy.yml`. The
sentence "The version is bumped in both repositories or neither" and its reason stay as written;
this story is that sentence being followed.

**Lines 130-131** — "Precedence is command line > active settings profile > POM `<properties>`,
measured on Maven 3.9.9." This is a measurement, not a pin, and the obvious edit — changing the
number — would claim a measurement nobody made. So it is **re-measured on 3.9.16** with `#114`'s
probe (`specs/stories/114-supply-build-properties-to-release-wrappers.md`, "Measured on
2026-09-22"), and the sentence restated with the version it was measured on. If the three results
differ from `#114`'s, the story stops: the paragraph's advice depends on that order, and a changed
order is a finding, not an edit.

## 6. Files that deliberately stay unchanged

| What | Why |
|---|---|
| Story specs `85`, `86`, `97`, `114` (DSH) and `65`, `67`, `69`, `71`, `72`, `76` (parent-poms) | Records of what was true when they were written, several of them measurements taken *on* 3.9.9. Rewriting them would make them lie about their own evidence. |
| `specs/product/PRD.md` | Its `3.9.9` mentions are narrative history. Status is reconciled by step 8, after the merge. |
| Generated issue-history tables in both root `README.md` files | See §5.3. |
| The four DSH release wrappers | No `mvn` of their own — `#86` §6. They reach 3.9.16 through `@master`. |
| `stCarolas/setup-maven@v5` itself, and the `@v5` tag | `#86` §10 put SHA-pinning of actions out of scope for the whole estate; nothing here changes that. |
| Root `pom.xml` in DSH, and every POM in parent-poms | No POM change is needed, so there is nothing to re-pin — §7.2. |

## 7. The parent-poms round trip

### 7.1 Why it belongs in this cycle

`#59` is the reason this story is in Wave 0 at all (§2). Beyond that, it is the half that carries
the release path: the four `project-*.yml` workflows run every DSH stage, staging, release and
hotfix, and `deploy.yml` publishes the `3.9.0-SNAPSHOT` parent that every DSH `-U` build resolves.

### 7.2 Which round trip, and which steps are no-ops

`#59` exists as an issue on an open `-SNAPSHOT` milestone, so this is the **full** round trip from
`CLAUDE.md`. As with `#58`, half of it has nothing to act on:

| `CLAUDE.md` step | Applies? |
|---|---|
| 1. Open an issue in parent-poms | Already open as `#59`. |
| 2. Milestone open as a `-SNAPSHOT` | `3.9.0-SNAPSHOT` is open. |
| 3. Implement and test it there | Yes — §4, verified per §9.2-§9.3. |
| 4. Point DSH's root `pom.xml` at that SNAPSHOT | **No-op.** Workflow YAML and Markdown only; no artifact for DSH to validate against. |
| 5. Close the issue and release parent-poms | **Not this story, and not a blocker.** `#70` is still on the milestone. Releasing **3.9.0** is Wave 0's standing goal. |
| 6. Re-pin DSH | **No-op**, for the reason in step 4. |

**This story must not be gated on the 3.9.0 release.** `#59` merging to `master` is what DSH
consumes.

### 7.3 Cross-reference

Comment the `#59` merge commit SHA on DSH `#87`, and link `#87` from `#59`. Claude closes neither
issue.

## 8. Acceptance criteria

The issue's four criteria are AC001-AC004, with AC001 made mechanical. AC005-AC010 are added by this
spec. parent-poms `#59`'s own criteria are covered by AC001, AC003, AC006 and AC007 run in that
repository.

- [ ] **AC001** — No file references Maven `3.9.9` as the *target* pinned version. The issue's
  wording ("no file references 3.9.9") would require rewriting historical records (§6), so the
  criterion is defined by these two commands, each of which must print nothing:

  ```bash
  # DSH
  git grep -nF '3.9.9' -- ':!specs/stories/' ':!specs/product/PRD.md' \
    | grep -vE '^README\.md:[0-9]+:\| \['
  # parent-poms
  git grep -nF '3.9.9' -- ':!specs/[0-9]*' \
    | grep -vE '^README\.md:[0-9]+:\| \['
  ```

  Before this story they print 29 lines in DSH and 39 in parent-poms — exactly the change surface of
  §4 and §5, and nothing historical. That was checked on 2026-09-26 while writing this spec.
- [ ] **AC002** — Every document that tells a reader which Maven to install shows **3.9.16**:
  `src/site/markdown/README.md`, root `README.md` after regeneration, and parent-poms
  `infrastructure/src/site/markdown/maven.md`. Both sample-block banners read
  `Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)`.
- [ ] **AC003** — Every workflow in either repository that invokes `mvn` pins
  `maven-version: '3.9.16'` via `stCarolas/setup-maven@v5`, after `actions/setup-java@v4`. Verified
  by sweep: the set of workflow files containing `mvn` and the set containing `maven-version: '3.9.16'`
  are identical, in each repository.
- [ ] **AC004** — All existing workflow jobs pass after the change — per §9.4, by execution, not
  inspection, wherever a run is possible.
- [ ] **AC005** — Every workflow that pins Maven also asserts it: eight `Verify Maven version`
  steps, two in DSH and six in parent-poms.
- [ ] **AC006** — The guard rejects a wrong version. Demonstrated by §9.1, including `3.9.160`.
- [ ] **AC007** — The six upstream guard blocks are identical, and the two DSH blocks are identical
  to each other. Verified by §9.3's `diff`, not by eye.
- [ ] **AC008** — `docs/devops/README.md`'s precedence statement is re-measured on 3.9.16 and names
  the version it was measured on (§5.4).
- [ ] **AC009** — `specs/github-actions-reusable-workflows.md` states 3.9.16 throughout, quotes every
  `maven-version`, shows the assertion step, and its rationale no longer claims "latest" (§4.5).
- [ ] **AC010** — `#59` is merged to parent-poms `master`, `deploy.yml` is dispatched with
  `release_type: snapshots` and green, and the SHA is commented on `#87` (§7.3).

## 9. Testing approach

No Java changes, so `mvn -B install` proves nothing about this story; it is run to show nothing
broke. On this box it runs 3.9.16 either way (§2), so it is not evidence of the bump either.

### 9.1 The guard, locally, before any CI sees it

`#86` §9's harness, retargeted:

```bash
check() {
  echo "$1" | grep -qF 'Apache Maven 3.9.16 ' || { echo "REJECTED"; return 1; }
  echo "ACCEPTED"
}
check 'Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)'   # ACCEPTED
check 'Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)'    # REJECTED
check 'Apache Maven 3.9.160 (0000000000000000000000000000000000000000)'  # REJECTED
```

The first line is the real banner, and additionally the real output of
`~/apps/apache-maven-3.9.16/bin/mvn -version` is piped through the same check.

### 9.2 YAML typing

Every `maven-version` in the eight workflows parses to the **string** `"3.9.16"`, checked with
`js-yaml` as `#86` §12.1 did, using the standalone Node under `~/apps`.

### 9.3 Guard identity

Extract each guard block — from `- name: Set up Maven` to the closing `}` — strip leading
indentation, and `diff` every upstream block against `build.yml`'s:

```bash
S="$SCRATCH"   # the session scratchpad
for f in build deploy project-stage project-staging project-release project-hotfix; do
  awk '/- name: Set up Maven/{p=1} p{print} p && /^ *}$/{exit}' ".github/workflows/$f.yml" \
    | sed 's/^ *//' > "$S/$f.block"
done
for f in deploy project-stage project-staging project-release project-hotfix; do
  diff -u "$S/build.block" "$S/$f.block" && echo "$f identical"
done
```

In DSH the same extraction runs on `ci.yml` and `api-testing.yml` and diffs them against each other.
DSH's blocks differ from upstream only in the comment and the documentation pointer; that
difference is intended (§4.2) and not diffed.

This is what AC007 rests on, and it is what makes §9.4's two inspection-only rows acceptable: a block
proven identical to one that ran is as close to execution as those two workflows can get.

### 9.4 Per-workflow evidence, from runs

The evidence for each row is the `Apache Maven 3.9.16 (2bdd9fdd...)` line the guard logs.

| Workflow | How it runs | Notes |
|---|---|---|
| parent-poms `build.yml` | On the `#59` PR | |
| parent-poms `deploy.yml` | Dispatched after merge, `release_type: snapshots` | Also re-publishes the `3.9.0-SNAPSHOT` parent, so it is its own test |
| `project-staging.yml` | DSH `staging.yml`, dispatched on this task branch | Also regenerates root `README.md` (§5.3) |
| `project-release.yml` | DSH `release.yml`, `dry_run: true`, against the RC — see below | No remote write |
| `project-stage.yml` | **Not run.** | `stage` has no dry-run mode; a dispatch would cut a real RC branch. Evidenced by §9.3 only. |
| `project-hotfix.yml` | **Not run.** | Its rehearsal needs a hand-made scratch `X.Y.x` branch (`#72` Task 10) and DSH has no hotfix line. Evidenced by §9.3 only. |
| DSH `ci.yml` | On the `#87` PR | |
| DSH `api-testing.yml` | `gh workflow run api-testing.yml --ref issue-87-bump-maven-to-3-9-16` | Its `pull_request` trigger is path-filtered to `dsh-rest-api/**` and `specs/api/**`, so it does not run on this PR — `#86` §9 |

**The release rehearsal.** With `#69`, `#111` and `#114` all closed, `#72`'s dispatch is now a
single command against the real RC, and needs no scratch branch:

```bash
gh workflow run release.yml --ref staging-0.3.0-SNAPSHOT-RC \
  -f branch_name=staging-0.3.0-SNAPSHOT-RC -f current_version=0.3.0 \
  -f next_development_version=0.4.0-SNAPSHOT -f hotfix_branch=0.3.x \
  -f initial_hotfix_version=0.3.1-SNAPSHOT -f dry_run=true
```

It runs only after `#59` is on `master`, since the wrapper calls `project-release.yml@master`. The
evidence this story needs is the guard's line, which runs before any `mvn`. **The rehearsal's overall
outcome is recorded but is not an acceptance criterion here**: if it fails later in the run, that
is a finding for the release path and gets its own issue, not a reason to hold a version bump.
Run `git ls-remote --heads origin` before and after, and record that the two are identical, as `#72`
did.

### 9.5 What is not tested

`project-stage.yml` and `project-hotfix.yml` by execution — §9.4 says why and §9.3 covers them. The
first real stage and hotfix after this lands are their first executions, and the guard is what makes
that first run self-reporting.

## 10. Out of scope

- **Releasing parent-poms 3.9.0** — §7.2, step 5.
- **Maven 4.** The issue sets 3.9.16. Moving major versions changes plugin compatibility and is its
  own decision.
- **SHA-pinning `stCarolas/setup-maven`**, and **a Maven Wrapper** — both argued out in `#86` §10;
  nothing here changes those arguments.
- **Rehearsing `project-hotfix.yml` for real.** Creating a scratch hotfix line to exercise one guard
  step is more machinery than the risk warrants, given §9.3.

## 11. Implementation order

Each task ends with its own verification. Commits are one per task, messages
`ci(#59): ...` / `docs(#59): ...` upstream and `ci(#87): ...` / `docs(#87): ...` here, each ending in
the session's attribution lines.

### Task 1 — parent-poms: workflows (`#59`)

- [x] In parent-poms: `git checkout master && git pull && git checkout -b issue-59-bump-maven-to-3-9-16`
- [x] Run §8 AC001's parent-poms grep; record the 39-line baseline.
- [x] Edit `build.yml` and `deploy.yml` per §4.1.
- [x] Edit the four `project-*.yml` per §4.2: pin moved, guard inserted directly after it.
- [x] Run §9.1 (harness plus the real local `mvn -version`), §9.2 (`js-yaml`) and §9.3 (`diff`,
      all five `identical`).
- [x] Commit: `ci(#59): pin Maven 3.9.16 and assert it in every workflow`. — `fd1054a5`. Red first:
      39 AC001 lines, 18 structural failures, four guards missing; then all green.

### Task 2 — parent-poms: documentation (`#59`)

- [x] Edit `infrastructure/src/site/markdown/maven.md` per §4.3, `CLAUDE.md:128` per §4.4, and
      `specs/github-actions-reusable-workflows.md` per §4.5, including the rewritten rationale.
- [x] Run the AC001 parent-poms grep; it must print nothing.
- [x] Commit: `docs(#59): move the Maven 3.9.16 pin into the docs and the workflow spec`. —
      `6bf0a368`. See the §4.5 note on step 3.

### Task 3 — parent-poms: PR, merge, deploy

- [ ] Push, open the PR against `master` with content approved by the repo owner first. Body links
      DSH `#87`.
- [ ] Record the `build.yml` guard line from the PR run.
- [ ] **Repo owner merges.** Claude does not.
- [ ] Dispatch `deploy.yml` with `release_type: snapshots` on `master`; record the guard line and
      the run URL.
- [ ] Comment the merge SHA on DSH `#87`.

### Task 4 — DSH: workflows

- [x] On `issue-87-bump-maven-to-3-9-16`: record the 29-line AC001 baseline.
- [x] Edit `ci.yml` and `api-testing.yml` per §5.1, including the `3.9.160` comment.
- [x] Run §9.1, §9.2 and the DSH half of §9.3.
- [x] Commit: `ci(#87): pin Maven 3.9.16 in ci.yml and api-testing.yml`. — `4dca2a8b`. Red first:
      29 AC001 lines, 6 structural failures.

### Task 5 — DSH: documentation and the re-measurement

- [x] Edit `src/site/markdown/README.md` per §5.2.
- [x] Re-run `#114`'s precedence probe on 3.9.16 in the scratchpad. Expect `FROM_SETTINGS`,
      `POM_DEFAULT`, `FROM_CLI`; **stop and report** on any other result (§5.4). — Got exactly
      those three, on `Apache Maven 3.9.16 (2bdd9fdd...)`. The devops sentence names 3.9.16 and
      points at `#114` for the earlier measurement, so it carries no literal `3.9.9` for AC001 to
      flag.
- [x] Edit `docs/devops/README.md` lines 130-131, 158 and 168-170 per §5.4.
- [x] Run markdownlint per `CLAUDE.md`.
- [x] `mvn -B install`, logged to `.logs/mvn-install.log` with the exit code reported, per `CLAUDE.md`.
      — **First run failed**, `jacoco:check` on `dsh-rest-api`: lines 0.48, branches 0.92. Not
      this change: it is **`#124`**, reproduced exactly as that issue's run B. The log shows
      `Copying 14 resources from target\test-classes to target\classes` in `dsh-data` and
      `Copying 20` in `dsh-rest-api`, and 13 classes analysed where a clean build has 10 — the three
      `*IT` classes that `jacoco:check`'s `*Test` excludes do not match. The copy comes from
      `maven-remote-resources-plugin`'s default `attachToMain=true` pointed at `target/test-classes`,
      so any `install` without `clean` after an earlier build does this. **Corrected:** this note
      first blamed VS Code's Java extension, from a stray class timestamped just after the
      repository was opened in it; the log's copy line, not checked at the time, shows Maven put
      the classes there. `mvn -B clean install` (`.logs/mvn-clean-install.log`): exit 0, BUILD
      SUCCESS, 8 of 8 coverage checks met, 127 unit tests — the workaround until `#124` lands.
- [x] Commit: `docs(#87): move the Maven 3.9.16 pin into the docs`.

### Task 6 — DSH: runs that need `#59` on `master`

- [ ] Dispatch `staging.yml` on the task branch; record the `project-staging.yml` guard line; pull
      the `Auto-generated README.md` commit; run the DSH AC001 grep — it must now print nothing.
- [ ] Dispatch `api-testing.yml` on the task branch; record the guard line.
- [ ] Dispatch the §9.4 release rehearsal; record the guard line, the run's outcome, and the
      before/after `ls-remote` comparison.
- [ ] Fill §8's checkboxes with the evidence, in the style of `#86` §8's evidence table.

### Task 7 — ship

Hand over to `dsh-ship-story` (steps 5 and 6): local review, push, and the PR into
`staging-0.3.0-SNAPSHOT-RC`, whose `ci.yml` run supplies the last guard line.

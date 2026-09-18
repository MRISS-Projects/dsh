---
issue: 86
slug: pin-maven-3-9-9-in-workflows
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 86 — Pin Maven 3.9.9 in all GitHub Actions workflows that invoke Maven

> **This story is one half of a pair.** `MRISS-Projects/parent-poms#58` carries the same title and
> fixes the same gap in the two workflows that build and publish the parent DSH inherits. The two
> are scheduled together in one cycle — see §7. The DSH half stands on its own and does **not**
> wait on a parent-poms release.

## 1. Story

**As a** developer reading a CI result
**I want** every workflow that runs Maven to run a Maven version this repository chose
**So that** a build that changes behaviour points at a change someone made, not at a runner image
that was rebuilt overnight

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Twin issue upstream: `MRISS-Projects/parent-poms#58`, milestone `3.8.0-SNAPSHOT`
- Standard being applied: `parent-poms#55`, which established Maven 3.9.9 + Temurin Java 17 as the
  canonical toolchain for the estate

Exactly two workflows in this repository invoke Maven:

| Workflow | Invocation | Line |
|---|---|---|
| `.github/workflows/ci.yml` | `mvn -B -U install --file pom.xml` | 209 |
| `.github/workflows/api-testing.yml` | `mvn -B -U install -DskipTests` | 209 |

Both configure `actions/setup-java@v4` with Java 17 / Temurin and `cache: 'maven'`, and then run
`mvn` against whatever Maven the `ubuntu-latest` image happens to ship. That version is chosen by
GitHub, changes with the runner image, and is announced nowhere this repository reads.

This is the last unpinned input to a DSH build. `#99` settled the other one: the parent is a
SNAPSHOT and both workflows pass `-U`, so the parent moves deliberately and visibly. Leaving the
Maven version floating puts an undeclared variable back into the same builds — and unlike the
parent SNAPSHOT, nobody chose it and nothing records it.

## 3. Why a green build is not evidence the pin worked

Maven under `-B` prints no version banner. `mvn -B -U install` begins at
`[INFO] Scanning for projects...`; the `Apache Maven <version>` line comes only from `mvn -version`
or `mvn -V`. So after adding the pinning step, nothing in either workflow's log states which Maven
ran, and a `stCarolas/setup-maven` step that silently failed to take effect would leave the build
green and back on the runner's Maven — the exact condition this story exists to remove, now
invisible rather than merely undeclared.

`#101` settled this same question for the package credential: the workflow was checking that the
secret was *present*, not that it *worked*, and the fix was to probe it. The same reasoning applies
here, so §4 adds an assertion rather than a log line.

## 4. Files to change

### 4.1 `.github/workflows/ci.yml`

**Insert after line 56** (the end of the `Set up JDK 17` step, before the
`Verify package credentials work` comment block):

```yaml
      - name: Set up Maven 3.9.9
        uses: stCarolas/setup-maven@v5
        with:
          maven-version: '3.9.9'

      - name: Verify Maven version
        run: |
          mvn -version
          mvn -version | grep -qF 'Apache Maven 3.9.9 ' || {
            echo "::error::Expected Maven 3.9.9. The 'Set up Maven 3.9.9' step did not take effect, so this build would have run the runner image's Maven. See docs/devops/README.md, 'Workflow Reference'."
            exit 1
          }
```

In the file the `Verify Maven version` step carries a comment block explaining why it exists, in the
style of the credential check that follows it; the snippet above omits it for brevity.

Three details are deliberate:

- **Placement after `setup-java`.** `parent-poms#58` records that `stCarolas/setup-maven@v5` depends
  on `JAVA_HOME` being set, which `actions/setup-java@v4` does. The four already-pinned reusable
  workflows upstream order the two steps this way.
- **`grep -qF` with a trailing space**, not a regex. The fixed-string form needs no escaping, and
  the trailing space prevents a future `3.9.90` from satisfying a check written for `3.9.9`. If a
  Maven release ever printed its version with no trailing detail, this fails loudly rather than
  passing silently — the safe direction for a guard.
- **No `shell: bash`.** Lines 101-106 of this file already record why: an explicit `shell: bash`
  sets `pipefail`, which changes how the surrounding steps' pipelines behave. This step needs
  nothing that the default shell does not give it.

`cache: 'maven'` on `setup-java` stays. It caches `~/.m2`, which is dependency state, not the Maven
installation, and the two do not interact.

### 4.2 `.github/workflows/api-testing.yml`

**Insert after line 60**, the same two steps, verbatim. The step names, the action reference and the
error text are identical in both files; a reader comparing the two workflows should find no
difference to account for.

### 4.3 `docs/devops/README.md`

One paragraph, inserted after the "All four release workflows..." paragraph that ends the
`## Workflow Reference` section (currently lines 76-80). It records the toolchain in the one place
that already explains what each workflow does:

- `ci.yml` and `api-testing.yml` run Java 17 (Temurin) via `actions/setup-java@v4` and Maven 3.9.9
  pinned via `stCarolas/setup-maven@v5`, and each asserts the Maven version before building.
- The four release wrappers run no Maven of their own; the reusable workflows they call in
  `parent-poms` pin the same 3.9.9.
- The two repositories are kept in step deliberately, and the version is bumped in both or neither.

The `ci.yml` and `api-testing.yml` rows of the table itself (lines 66 and 68) describe what each
workflow *gates*, not how it is provisioned, and are left alone.

## 5. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `stage.yml`, `staging.yml`, `release.yml`, `hotfix.yml` | Thin `uses:` wrappers with no `steps:` block of their own and no local `mvn`. The reusable workflows they call are already pinned. See §6. |
| `documentation-sync.yml`, `wiki-sync.yml`, `spec-validation.yml` | No Maven invocation at all. Confirmed by sweeping `.github/workflows/` for `mvn`. |
| `README.md`, `src/site/markdown/README.md` | They hold the only remaining `3.3.9` references in the tree — six lines each, covering the download link, `M2_HOME` examples and a sample `mvn -version` block. That is `#85`'s scope, and splitting it across two stories would leave both half-done. |
| `CLAUDE.md` | Its Commands table gives `mvn -B install` without a version, and says nothing about how CI provisions Maven. Nothing in it becomes false. |
| Root `pom.xml` | Unrelated. Pinning a *released* parent is Wave 0's closing goal, not this story. |
| `specs/product/PRD.md` | Reconciled by step 8 (`dsh-reconcile-prd`) after the merge, not edited ahead of one. |

## 6. Issue body reconciliation — the release wrappers premise does not hold

The issue's Notes section says:

> The caller workflows in dsh that invoke them must also pin Maven in case they run any local `mvn`
> steps.

They do not run any. All four wrappers are 12-24 lines of `uses:` plus `with:`, with no `steps:`
block; `stage.yml:12`, `staging.yml:12`, `release.yml:24` and `hotfix.yml:12` each hand straight off
to a reusable workflow in `parent-poms`. There is no local `mvn` step to protect, now or at the
branch point.

Adding a pin to them anyway would be configuration that cannot execute, reading to the next person
as protection that is not there. The hypothetical it guards — someone later adds a local `mvn` step
to a wrapper — is better served by the reusable workflow that step would have to sit alongside,
which is already pinned.

This follows what `#97` and `#103` did with premises of their own that did not survive checking:
correct it in the spec, and say so, rather than implement against it.

The same Notes section's other claim **does** hold and needs no correction: the four `project-*.yml`
reusable workflows in `parent-poms` already pin 3.9.9, at `project-stage.yml:55-58`,
`project-staging.yml:96-99`, `project-release.yml:66-69` and `project-hotfix.yml:50-53`.

## 7. The parent-poms round trip

> **Done.** This section was written before the upstream work and is kept for its reasoning, not as
> outstanding work. What landed:
>
> | What | Evidence |
> |---|---|
> | `build.yml` and `deploy.yml` pinned | `MRISS-Projects/parent-poms@7411cbec`, on `master` |
> | Verified by a real run | `deploy.yml` dispatched with `release_type: snapshots` — [run 35363898584](https://github.com/MRISS-Projects/parent-poms/actions/runs/35363898584), success, logging `Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)` |
> | `maven-version` quoted in all six upstream steps | `MRISS-Projects/parent-poms@ea531f1f`, from this story's local review |
> | Cross-referenced | Both SHAs commented on `#86` |
>
> `parent-poms#58` itself stays open until the repo owner closes it — Claude never closes an issue.
> §7.1 and §7.2 below keep the present tense they were written in; the reasoning is what they are
> for, and the next person scoping a round trip needs it more than they need a receipt.

### 7.1 Why it belongs in this cycle

`parent-poms#58` is open, sits on milestone `3.8.0-SNAPSHOT`, and names `build.yml` and `deploy.yml`
— which are, confirmed by sweep, the only two workflows there that invoke `mvn` without a pin.

Two facts make doing it alongside this story cheap and make deferring it wasteful:

1. **It takes effect in DSH immediately, with no release and no re-pin.** All four DSH wrappers
   reference `MRISS-Projects/parent-poms/.github/workflows/<name>.yml@master`. The moment `#58`
   lands on `master`, DSH's release path is running pinned Maven.
2. **It closes the half this story cannot reach.** `deploy.yml` with `release_type: snapshots` is
   what publishes the `3.8.0-SNAPSHOT` parent that every `-U` build here resolves. Pinning the two
   DSH gates while the artifact they inherit is still produced by an unpinned Maven pins one end of
   the toolchain and leaves the other floating.

### 7.2 Which round trip, and which of its steps are no-ops

`#58` already exists as an issue on a milestone, so this is the **full** round trip from
`CLAUDE.md`, not the light one — no direct-commit-and-comment shortcut applies. But three of its six
steps have nothing to act on here, and the spec says so rather than leaving an executor to discover
it:

| `CLAUDE.md` step | Applies? |
|---|---|
| 1. Open an issue in parent-poms | Already open as `#58`. |
| 2. Milestone open as a `-SNAPSHOT` | `3.8.0-SNAPSHOT` is open. |
| 3. Implement and test it there | Yes — §7.3. |
| 4. Point DSH's root `pom.xml` at that SNAPSHOT to validate | **No-op.** `#58` changes workflow YAML only. No POM changes, no new artifact coordinate, nothing for DSH to validate against. |
| 5. Close the issue and release parent-poms | **Not this story, and not a blocker.** Releasing `3.8.0` also requires clearing `#57` and `#13` on that milestone. That is the standing Wave 0 goal already recorded in the PRD. |
| 6. Re-pin DSH to the released version | **No-op**, for the same reason as step 4. |

**This story must not be gated on a parent-poms release.** `#58` merging to `master` is what DSH
consumes; `3.8.0` shipping is a separate milestone-clearing exercise.

### 7.3 Files to change in parent-poms

Branch `issue-58` from `master`. The same step pair as §4.1, with the same text **less the
`docs/devops/README.md` pointer** in the error message — that file does not exist in `parent-poms`,
and a failure there must not send a reader to a path they do not have:

- **`.github/workflows/build.yml`** — insert after line 55 (end of `Set up JDK 17`), before the
  `mvn` invocations at lines 139, 149 and 157.
- **`.github/workflows/deploy.yml`** — insert after line 63 (end of `Set up JDK 17`), before the
  `mvn` invocations beginning at line 150.

The four `project-*.yml` workflows there are already pinned and are not touched, which satisfies
`#58`'s fifth acceptance criterion at the branch point rather than by new work.

**Verification there:** merge to `master`, then dispatch `deploy.yml` with
`release_type: snapshots`. That both exercises the changed workflow and re-publishes the
`3.8.0-SNAPSHOT` parent — the change is its own test, because the workflow being fixed is the
workflow that runs.

**Cross-reference:** comment the resulting `master` SHA on DSH `#86`, and link DSH `#86` from
parent-poms `#58`. `CLAUDE.md` requires that comment for the light round trip specifically; doing it
here as well is what keeps the pair legible from either repository.

## 8. Acceptance criteria

The issue's four criteria are restated as AC001-AC004 with no change of intent. AC005-AC008 are
added by this spec.

- **AC001** — Every file in `.github/workflows/` that invokes `mvn` contains a
  `stCarolas/setup-maven@v5` step with `maven-version: 3.9.9`. Verified by sweep, not by memory:
  the set of files matching `mvn` and the set containing `setup-maven` are identical.
- **AC002** — In both files the pinning step sits after `actions/setup-java@v4` and before every
  `mvn` invocation.
- **AC003** — No workflow relies on the runner image's default Maven installation.
- **AC004** — `ci.yml` and `api-testing.yml` both pass on the pull request.
- **AC005** — Each of the two workflows fails if the pin did not take effect. Demonstrated, not
  asserted: the guard is shown rejecting a wrong version (§9).
- **AC006** — The four release wrappers are unchanged, and §6 records why the issue's note about
  them does not apply.
- **AC007** — `docs/devops/README.md` states the pinned toolchain for the two gate workflows and
  notes that the wrappers inherit the same pin from `parent-poms`.
- **AC008** — `parent-poms#58` is delivered in the same cycle: both workflows there pinned, merged
  to `master`, a `deploy.yml` snapshot dispatch green, and the SHA commented on `#86`.
  **Met** — `parent-poms@7411cbec` on `master`, verified by
  [run 35363898584](https://github.com/MRISS-Projects/parent-poms/actions/runs/35363898584)
  (success), SHA commented on `#86`. See §7's completion table. Closing `#58` itself is the repo
  owner's, not part of this criterion.

**Evidence for the rest, gathered from runs rather than from the diff:**

| AC | Evidence |
|---|---|
| AC001 | Sweep — the set of workflows that invoke `mvn` and the set matching `setup-maven@v5` are identical |
| AC002 | Both files parsed with `js-yaml`: pin after `setup-java`, before every `mvn` |
| AC003, AC005 | `Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)` logged by the guard in both workflows |
| AC004 | CI [run 35367666876](https://github.com/MRISS-Projects/dsh/actions/runs/35367666876) and dispatched API Testing [run 35368154042](https://github.com/MRISS-Projects/dsh/actions/runs/35368154042), both success — see §9 on why the second needed a dispatch |
| AC006 | Four wrappers untouched in the diff; §6 records why |
| AC007 | `docs/devops/README.md`, "The Maven toolchain is pinned, in both repositories" |

## 9. Testing approach

There is no Java change, so `mvn -B install` proves nothing about this story. It is still run, to
confirm the story broke nothing.

**The guard is tested locally before CI sees it.** The assertion in §4.1 is ordinary shell, so run
it against both a matching and a non-matching version string:

```bash
check() {
  echo "$1" | grep -qF 'Apache Maven 3.9.9 ' || { echo "REJECTED"; return 1; }
  echo "ACCEPTED"
}
check 'Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)'   # expect ACCEPTED
check 'Apache Maven 3.9.6 (bc0240f3c744dd6b6ec2920b3cd08dcc295161ae)'   # expect REJECTED
check 'Apache Maven 3.9.90 (0000000000000000000000000000000000000000)'  # expect REJECTED
```

The third case is the reason for `-F` and the trailing space; without them it is accepted.

**In CI, the evidence is the log.** After the change, both workflows print an `Apache Maven 3.9.9`
line from the `Verify Maven version` step. Record that line from each of the two workflow runs —
that is AC001 through AC005 evidenced from the runs themselves rather than from the diff.

**Only one of the two runs itself, though.** `api-testing.yml`'s `pull_request` trigger is scoped to
`paths: ['dsh-rest-api/**', 'specs/api/**']`, and a workflow-only change touches neither, so **it
does not run on this story's own pull request** and its copy of the guard would go unexercised. Its
`workflow_dispatch` trigger is the way round:

```bash
gh workflow run api-testing.yml --ref issue-86-pin-maven-3-9-9-in-workflows
```

The dispatched run boots MongoDB and RabbitMQ service containers, builds the full reactor, starts
`dsh-rest-api` and runs the Postman collections, so it is a heavier run than the guard needs — but
it is the only way to make AC004 and AC005 true for that workflow by execution rather than by
inspection. Anyone editing `api-testing.yml` without touching `dsh-rest-api/` or `specs/api/` has
the same problem, `#87` included.

**The red state is worth capturing once.** Before adding the pinning step, the `Verify Maven
version` step alone will report whatever the runner ships. Push that first if you want the record;
otherwise note the runner's version from the first run's log, since it is the number this story
removes from the build and it is not written down anywhere else.

## 10. Out of scope

- **`#85`** — the Maven and Java versions in `README.md` and `src/site/markdown/README.md`.
- **`#87`** — the bump from 3.9.9 to 3.9.16. It sits in Wave 1, and it was moved out of Wave 0 on
  2026-09-17 precisely so this story could write 3.9.9 once without being rewritten in the same
  wave. This spec writes 3.9.9 knowing `#87` replaces it, in both repositories, later.
- **Pinning `stCarolas/setup-maven` to a commit SHA rather than the `@v5` tag.** A mutable tag on a
  third-party action is a real supply-chain exposure, but it is the estate's existing convention in
  the four reusable workflows upstream today, and in all eight across the two repositories once
  this cycle lands, and `actions/checkout@v4`, `actions/setup-java@v4` and
  `actions/setup-node@v4` carry the same exposure here. Tightening one action in isolation buys
  nothing. If it is worth doing it is worth doing across both repositories at once, which is its own
  issue.
- **Releasing parent-poms `3.8.0`** — see §7.2, step 5.
- **A Maven Wrapper (`mvnw`).** It would pin CI and local builds together from one committed file,
  which is a genuinely better end state, but it is a change to how every developer invokes Maven and
  it diverges from the estate standard `#55` set. Not smuggled in under a workflow-pinning story.

## 11. Implementation order

All six steps are complete; the order is kept because it is the order that worked.

1. ✅ `parent-poms` `issue-58` — both workflows, merge to `master`, dispatch `deploy.yml` with
   `release_type: snapshots`, comment the SHA on `#86`. Independent of everything below; doing it
   first means the DSH spec cites a landed commit rather than an intention. — `7411cbec`, plus
   `ea531f1f` from local review.
2. ✅ `ci.yml` — pinning step plus guard. — `1a8302a4`
3. ✅ `api-testing.yml` — the same two steps. — `1a8302a4`
4. ✅ `docs/devops/README.md` — the toolchain paragraph. — `1a8302a4`
5. ✅ `mvn -B install` locally, logged to `.logs/mvn-install.log` per `CLAUDE.md`, to confirm nothing
   broke. — BUILD SUCCESS, twice: before and after the review fix.
6. ✅ Push, open the PR, and collect the two `Apache Maven 3.9.9` log lines as AC evidence. —
   PR #108; both lines in §8's evidence table.

## 12. Review rounds

### 12.1 Local review (step 5)

One finding, taken: `maven-version` was passed unquoted in both workflows, while `java-version: '17'`
two lines above was quoted. Harmless for `3.9.9` and for `#87`'s `3.9.16` — both have two dots, so
YAML types them as strings — but `3.10` parses as the float `3.1` and `4.0` as `4`, handing the
action a version that does not exist. The symptom would be a `Verify Maven version` failure reading
as a broken `setup-maven` step rather than as a quoting bug. Fixed here and, under `CLAUDE.md`'s
light round trip, in all six upstream instances (`parent-poms@ea531f1f`). Verified with `js-yaml`:
all eight files now parse the value to the string `"3.9.9"`.

### 12.2 Copilot review, round 1 (step 7)

Arrived **automatically**, so it ran at the repository or organisation default effort level, not at
a per-PR choice — the same layer that supplied `#102`'s `Lite` review in `#103`'s history.

One finding, on `specs/stories/86-pin-maven-3-9-9-in-workflows.md`: §7, AC008 and §11 read as
outstanding work although `parent-poms#58` had already merged, so the spec appeared to name an
unmet dependency.

**Taken in part.** The conclusion was right and is fixed by §7's completion table, AC008's
**Met** line and §11's per-step commits. Two parts were not adopted:

- The finding said the spec instructs a future reviewer "to open and implement issue #58". It never
  asked anyone to open it — §7.2's table says "Already open as `#58`". Only the implementing half
  of the claim applied.
- It proposed rewriting §7 as completed evidence. §7.1 and §7.2 are the *reasoning* — why a release
  is not a gate for a workflow-only change, and which three of `CLAUDE.md`'s six round-trip steps
  are no-ops. That argument is what the next round trip needs; a receipt is not. The completion
  record was added above it instead, and the rationale kept in the present tense it was written in.

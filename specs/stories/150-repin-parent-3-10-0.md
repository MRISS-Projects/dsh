---
issue: 150
slug: repin-parent-3-10-0
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 150 — Re-pin the parent to the released parent-poms 3.10.0, and retire what the SNAPSHOT pin needed

## 1. Story

**As a** maintainer of DSH
**I want** `DEVELOP` built on the released parent-poms 3.10.0, with the workarounds of the
`SNAPSHOT` pin removed
**So that** every build resolves the same parent, and the rules written for defects that 3.10.0
fixed stop costing effort

## 2. Context

- Wave: 1, upstream step 5 (`specs/product/PRD.md` §4, Wave 1), DSH milestone `0.4.0-SNAPSHOT`
- Issue: [#150](https://github.com/MRISS-Projects/dsh/issues/150)
- Parent branch: `DEVELOP`, at `568e07615` when this spec was written. The task branch is
  `issue-150-repin-parent-3-10-0`.
- parent-poms 3.10.0 was released on 2026-10-03:
  [run 37080121832](https://github.com/MRISS-Projects/parent-poms/actions/runs/37080121832), tag
  `mriss-parent-3.10.0` on `4dee01a6`.

### 2.1 What the SNAPSHOT pin left behind

| Where | What | Why it existed |
|---|---|---|
| root `pom.xml` | parent `3.10.0-SNAPSHOT` | `#127`'s proof of parent-poms' unreleased fixes |
| `ci.yml:238`, `api-testing.yml:228` | `mvn -B -U install …` | a `SNAPSHOT` parent is tracked on every run |
| `CLAUDE.md`, "Commands" | "both Maven invocations … pass `-U` — while the parent is a `SNAPSHOT`" | the same |
| `docs/devops/README.md`, "Parent POM" | the pin's rationale and exit, and "when the parent is pinned to a released version, drop `-U`" | the same |
| `CLAUDE.md`, "Two things Claude never does" | "Every new issue carries a label … Remove this rule … once DSH is pinned to a parent-poms release that includes `#86`" | `maven-changes-plugin#36`, fixed in 2.12.10 |
| `.claude/skills/dsh-new-story/SKILL.md` | "Every issue gets at least one label … Drop this paragraph once `#86` is closed and DSH is pinned to a release that includes it" | the same |

Both labelling paragraphs name this story's pin as their own exit condition. `#86` is closed, and
3.10.0 includes it.

## 3. Design

### 3.1 Decisions

1. **The pin.** `3.10.0-SNAPSHOT` → `3.10.0`, in its own commit.
2. **`-U` is dropped from both workflows**, as `docs/devops/README.md` prescribes. With a released
   parent, resolution is reproducible, and the flag no longer earns its place.
3. **The labelling rule goes.** The `CLAUDE.md` paragraph is removed whole. In `dsh-new-story`, the
   rule becomes a plain instruction, because the skill still passes `--label` to `gh issue create`
   and a label still has a use: it fills the release notes' Type column. The new text says to give
   the issue its type as a label, and that an unlabelled issue is still listed, with Type `n/a`.
   It no longer calls an empty `--label` a defect.
4. **The docs describe a released pin, and the next `SNAPSHOT` pin.** The "Parent POM" section says
   DSH is on the released `3.10.0`. The next time a `SNAPSHOT` pin is taken, to validate parent-poms
   work end to end, `-U` goes back into both workflows for as long as it lasts. `CLAUDE.md`'s
   "Commands" line says the same, briefly.

### 3.2 Out of scope

- parent-poms `3.11.0-SNAPSHOT` (Wave 1, upstream step 6). If either of its issues needs a POM
  change, DSH takes another temporary `SNAPSHOT` pin then, with `-U`, as decision 4 says.
- The placeholder `deploy.yml` on `master`. The 0.4.0 release replaces it.

## 4. Verification design

- **Gates 1 and 2** on the pin: `mvn -B clean install`, logged.
- **Gate 3 is not re-run, and the reason is checked rather than assumed.** It ran on
  `3.10.0-SNAPSHOT` in `#127` and passed. The released `products-3.10.0.pom` and `mriss-parent-3.10.0`
  are compared with the snapshot that run used, `products-3.10.0-20261002.123547-2`. If they differ
  in anything but their version, gate 3 is run.
- **AC003: an unlabelled issue on the released parent.** No closed, milestoned, unlabelled issue
  exists in DSH: every one was labelled when `#86` was raised. As in `parent-poms#86`'s verification,
  DSH `#146` has its `bug` label removed for one local README generation, then put straight back.
  On this branch, with the released parent: `mvn -B -N -Ddeployment process-resources`. Then check:
  - `#146` is listed under `### Version 0.3.2` with Type `n/a`;
  - the generated `README.md` is restored afterwards and not committed.
- **CI** on the PR runs `ci.yml` without `-U`, against the released parent.

## 5. Files to change

| File | Change |
|---|---|
| `pom.xml` | parent `3.10.0-SNAPSHOT` → `3.10.0` |
| `.github/workflows/ci.yml`, `.github/workflows/api-testing.yml` | drop `-U` |
| `CLAUDE.md` | remove the labelling paragraph; reword the `-U` line in "Commands" |
| `.claude/skills/dsh-new-story/SKILL.md` | the labelling rule becomes an instruction (decision 3) |
| `docs/devops/README.md` | "Parent POM" describes the released pin and the next `SNAPSHOT` pin |
| `specs/product/PRD.md` | nothing here; the reconcile after the merge marks step 5 done |

## 6. Tasks

- [x] **T1.** Pin `3.10.0` in its own commit. Compare the released parent POMs with the snapshot
      (§4).
- [x] **T2.** Run gates 1 and 2, logged.
- [x] **T3.** Drop `-U` from both workflows.
- [x] **T4.** Remove or reword the labelling rule in `CLAUDE.md` and `dsh-new-story`.
- [x] **T5.** Update `docs/devops/README.md` and `CLAUDE.md`'s `-U` line. Run markdownlint.
- [x] **T6.** AC003's check: unlabel `#146` for one local README generation, then relabel it.
      Record the result, and restore the README.
- [ ] **T7.** Record the results in §8. Hand over to `dsh-ship-story`.

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: the root `pom.xml` names the released `3.10.0` | T1 |
| AC002: `mvn -B clean install` passes | T2 |
| AC003: the labelling rule is removed; an unlabelled issue is listed with Type `n/a`, shown on a README generated against the released parent | T4, T6 |
| AC004: `-U` is dropped from both workflows; the docs say to restore it for the next `SNAPSHOT` pin | T3, T5 |
| AC005: `docs/devops/README.md` and `CLAUDE.md` describe a released pin and the next `SNAPSHOT` pin | T5 |

## 8. Verification results

All on 2026-10-02, on the development machine.

- **AC001, the pin** (`f1127f75f`). The root `pom.xml` names `3.10.0`. The build downloaded
  `products-3.10.0.pom` and `mriss-parent-3.10.0.pom` from GitHub Packages.
- **AC002, gates 1 and 2.** `mvn -B clean install`: all 13 modules succeed, 127 tests run with no
  failure, and "All coverage checks have been met" in each of the 8 modules with production code.
- **Gate 3 is not needed, checked.** The released `products-3.10.0.pom` and `mriss-parent-3.10.0.pom`
  were compared with `products-3.10.0-20261002.123547-2` and `mriss-parent-3.10.0-20261002.123547-2`,
  the snapshot `#127`'s gate 3 passed on. With the version lines ignored, there are 0 differing
  lines in either.
- **AC003, the labelling rule.** `CLAUDE.md` loses the paragraph; `dsh-new-story` keeps a plain
  instruction (`31488ccf5`), as decided at review. The check: DSH `#146` had its `bug` label removed
  for one run of `mvn -B -N -Ddeployment process-resources` on this branch, against the released
  parent. The log shows `changes:2.12.10:github-text-list (generate-list-of-issues)`. The README
  listed `#146` under `### Version 0.3.2` with Type `n/a`. The label was restored straight after,
  and the generated README was discarded; the tree was clean.
- **AC004, `-U`** (`3dbe7657e`). Gone from `ci.yml` and `api-testing.yml`. `ci.yml`'s comment says to
  add it back to both for the next `SNAPSHOT` pin.
- **AC005, the docs** (`31488ccf5`, `47560dfb9`). `CLAUDE.md`'s "Commands" and the devops guide's
  "Parent POM" describe the released pin, and say what a `SNAPSHOT` pin needs.

### 8.1 Review round 1 on the PR, 2026-10-03

Copilot reviewed `9786be031` and raised two findings. The review arrived automatically, so it ran at
the repository's default effort level. Both were valid, both were wording, and neither changed
behaviour.

1. **`docs/devops/README.md:74`.** The workflow table still said `ci.yml` runs `mvn -B -U install`,
   contradicting the workflow and the "Parent POM" section. The search for leftover `-U` text during
   the build was malformed and missed it. Fixed in `611dcf83a`.
2. **`pom.xml:8`.** The comment above `<parent>`, from `FR001`, still said the parent was a
   `SNAPSHOT` so that every parent-poms change is immediately testable, above a released `3.10.0`.
   It now names the released pin and points at the temporary `SNAPSHOT` pin and its `-U`. Fixed in
   `170354a14`. `mvn -B validate` passes on it.

The only `-U` text left in the repository is in older story specs, which record what was true when
they were written.

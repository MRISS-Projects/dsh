---
issue: 92
slug: remove-travis-build-estate
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 92 — Remove the dead Travis build estate

## 1. Story

**As a** developer working in this repository
**I want** the Travis-era build estate deleted, including `install-parent-pom.sh` and the root
`parent-pom.xml`
**So that** nobody is misled into running a build path that has not worked since the move to
GitHub Actions, or into installing a parent POM that no module inherits from

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Issue: [#92](https://github.com/MRISS-Projects/dsh/issues/92)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the `0.3.0-SNAPSHOT` milestone and carries
  the Wave 0 work that `#92` was raised from. `DEVELOP` is at `0.4.0-SNAPSHOT` and 37 commits
  behind, so it lacks the PRD context this story references.

`install-parent-pom.sh` installs `com.mriss:mriss-parent:1.2.4` from the root `parent-pom.xml`.
No module in this repository inherits from that artifact — the real parent is
`com.mriss.mriss-parent:products`, resolved from GitHub Packages via the `<parent>` block in the
root `pom.xml`. Both files carry a `DEPRECATED` header today.

They survive only because the four Travis-era `build-ci*.sh` scripts call
`./install-parent-pom.sh` on line 7, under `set -e`. Those scripts are themselves reachable only
from `.travis.yml`, which has not been the working pipeline since the GitHub Actions migration:
it pins `dist: trusty` and `jdk: openjdk8` against a Java 17 codebase, and decrypts
`maven/settings.xml.enc` with an `$encrypted_*_key` held by Travis that is no longer recoverable.
`docs/devops/README.md` already states the Travis configuration "is not part of the working
pipeline and should not be relied on".

Deleting only the two files named in the issue title would leave four scripts failing on line 7.
Deleting the estate as a whole is the coherent change.

## 3. Scope decision

Issue `#92` as written places `.travis.yml` and the `build-ci*.sh` scripts **out of scope**. That
boundary was reconsidered during step 3 and **reversed by the repository owner**: the estate goes
as one unit. The issue body must be updated to match before this story ships — see §8.

## 4. Files to delete

Each file below is reachable **only** from another file in this table. The set is closed.

| File | Sole reachability |
|---|---|
| `.travis.yml` | the root of the estate |
| `build-ci.sh` | `.travis.yml:41` |
| `build-ci-stage.sh` | `.travis.yml:46` |
| `build-ci-staging.sh` | `.travis.yml:51` |
| `build-ci-release.sh` | `.travis.yml:56` |
| `install-parent-pom.sh` | line 7 of all four `build-ci*.sh` |
| `parent-pom.xml` | `install-parent-pom.sh` |
| `maven/settings.xml.enc` | `.travis.yml:22` |
| `maven/settings-security.xml` | lines 9-10 of all four `build-ci*.sh` |
| `install-and-configure-Mongo.sh` | `.travis.yml:32` (`before_script`) |
| `pre-release-script.sh` | `build-ci-release.sh:15` |
| `post-release-script.sh` | `build-ci-release.sh:24` (`mvn -N exec:exec` inside `target/checkout`) |

`maven/` is emptied by this change and is removed with its contents.

### Verified: parent-poms does not call these scripts

The release path now runs through reusable workflows in `MRISS-Projects/parent-poms`
(`project-release.yml`, `project-stage.yml`, `project-staging.yml`, `project-hotfix.yml`). All
four were read on `master` during step 3 and **none invokes any shell script from the calling
project**. `pre-release-script.sh` is therefore unreachable once `build-ci-release.sh` is gone.

`post-release-script.sh` needed a second check, because `build-ci-release.sh` reaches it
indirectly via `mvn -N exec:exec` rather than by name. It is Travis estate on three independent
counts: it runs `mvn -s ../../settings.xml`, which is the settings file `.travis.yml:22` decrypts
and `build-ci*.sh` delete afterwards; it embeds `[skip travis]` in its release commit message; and
its only caller is `build-ci-release.sh`. No `exec-maven-plugin` configuration exists in this
repository or in parent-poms' `products/pom.xml`, and a code search across parent-poms for
`post-release-script` returns zero results, so nothing outside the estate invokes it.

## 5. Files that deliberately stay

| File | Why |
|---|---|
| `src/site-desc/site.xml` | **Live build dependency.** `install-parent-pom.sh` copies it, which makes it look Travis-adjacent, but root `pom.xml:174` and `pom.xml:191` use it as the `siteDirectory`. Deleting it breaks site generation. |
| `connect-mongo.sh`, `connect-mongo-super-user.sh` | Documented developer tools (`README.md:296,313`, `docs/troubleshooting/README.md:23`). |
| `deploy.sh`, `maven-site.sh`, `maven-site-deploy.sh`, `set-version.sh` | Orphaned, but **not Travis-specific**: each is a one-line `mvn` convenience wrapper with no Travis variable, no `../../settings.xml`, and no `[skip travis]` marker. `deploy.sh` points at a personal `~/apps/maven/conf/empty-settings.xml`, confirming it is a local developer tool. Out of scope — see §9. |

## 6. Documentation to update

Five of these are warnings *about* the deleted files, which become references to nothing once the
files are gone.

| File | Line | Change |
|---|---|---|
| `README.md` | 355 | Replace the `./install-parent-pom.sh` build step with the GitHub Packages `~/.m2/settings.xml` prerequisite. |
| `src/site/markdown/README.md` | 355 | Same change; this file is a near-copy of `README.md`, not identical, so edit it separately. |
| `CLAUDE.md` | 43 | Drop the "**Do not run `install-parent-pom.sh`**" warning; keep the statement that the parent resolves from GitHub Packages. |
| `.github/copilot-instructions.md` | 21 | Remove the clause calling root `parent-pom.xml` a deprecated legacy artifact. |
| `.github/workflows/ci.yml` | 48-50 | Remove the comment explaining why `install-parent-pom.sh` is deliberately not used. |
| `docs/devops/README.md` | ~82-84, Parent POM section | Delete the Travis leftovers paragraph and the `install-parent-pom.sh` paragraph. |
| `.github/roles.md` | 70 | Stop naming `build-ci.sh` as a script to maintain. |
| `.github/copilot/prompts/feature-implementation.md` | 59 | Same. |
| `docs/copilot/prompt-examples.md` | 180 | Same. |
| `dsh-coverage-report/pom.xml` | 188 | `<message>badges commit [skip travis]</message>` — a live commit message in the `process-badges` profile carrying a Travis directive that no longer means anything. Drop `[skip travis]`. |
| `specs/product/PRD.md` | 67 | Update the Wave 0 entry to the issue's new title once `#92` is retitled per §8, so the PRD and the issue do not drift. |

Three files are **left unchanged**, and the AC002 sweep permits hits in them. All three are
historical records of work already completed; rewriting them would falsify the record:

- `specs/devops/deploy-release-profiles-reorganization.md:278-282`
- `docs/superpowers/plans/2026-09-16-ai-driven-development-process.md`
- `docs/superpowers/specs/2026-09-16-ai-driven-development-process-design.md`

## 7. Acceptance criteria

- [ ] AC001: Every file in §4 is deleted, and the `maven/` directory no longer exists.
- [ ] AC002: Every file in §6 is updated. No reference to a deleted file remains in any tracked
  file except the three historical records listed at the end of §6.
- [ ] AC003: `mvn -B install` succeeds, resolving `com.mriss.mriss-parent:products` from GitHub
  Packages with no locally installed parent artifact.
- [ ] AC004: `./scripts/check-coverage.sh` passes — no production code is touched, so aggregate
  coverage must be unchanged.
- [ ] AC005: Markdown lint passes over the changed documentation.
- [ ] AC006: Every file in §5 still exists, and site generation still resolves
  `src/site-desc/site.xml`.
- [ ] AC007: CI is green on the pull request.

## 8. Issue body reconciliation — done 2026-09-16

The process treats the GitHub issue as the source of truth, so `#92` had to be edited to match
this spec. Done on 2026-09-16 with the repository owner's authorisation:

1. **Title** — "Remove dead install-parent-pom.sh and root parent-pom.xml" became "Remove the dead
   Travis build estate".
2. **Scope** — the old **Out of Scope** clause excluding `.travis.yml` and the `build-ci*.sh`
   scripts was removed, and a "Scope widened" note records the reversal and why.
3. **AC002** — replaced with the full reference set in §6. The old text named `build-ci*.sh`,
   `README.md` and `src/site/markdown/README.md` only, omitting `CLAUDE.md`,
   `.github/copilot-instructions.md`, `.github/workflows/ci.yml`, `docs/devops/README.md`,
   `dsh-coverage-report/pom.xml` and `specs/product/PRD.md`.
4. **Out of Scope** — now carries the orphaned-wrapper follow-up and the historical records, per
   §9.

The issue and this spec agree as of that edit. If either changes again, reconcile before shipping.

## 9. Out of scope

- The orphaned non-Travis scripts in §5 (`deploy.sh`, `maven-site.sh`, `maven-site-deploy.sh`,
  `set-version.sh`). They have no caller in this repository and are not part of the Travis estate,
  so deciding their fate is a separate judgement about developer tooling. **Raise as a separate
  Wave 0 issue.**
- `specs/devops/deploy-release-profiles-reorganization.md`, per §6.
- Any change to the reusable workflows in `MRISS-Projects/parent-poms`.

## 10. Testing approach

There is no behaviour to drive red-green: this story deletes files and edits prose, adding no
production code. TDD does not apply, and no test is added. Verification is by gate, in this order:

1. `mvn -B install`, logged to `.logs/mvn-install.log` per `CLAUDE.md`, exit code reported
   explicitly. This is the load-bearing check — it proves the parent resolves from GitHub Packages
   with nothing installed locally by the deleted script.
2. A reference sweep asserting AC002, run over **tracked files only** so that untracked working
   directories such as `.superpowers/` cannot mask or fake a result:

   ```bash
   git ls-files | while read -r f; do
     grep -lE "install-parent-pom|build-ci|travis|settings-security|parent-pom\.xml|install-and-configure-Mongo|(pre|post)-release-script" "$f"
   done
   ```

   The only permitted hits are the three historical records listed at the end of §6.
3. `./scripts/check-coverage.sh` for AC004.
4. `markdownlint` over the changed files for AC005, using the command in `CLAUDE.md`.

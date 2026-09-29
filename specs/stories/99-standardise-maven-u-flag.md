---
issue: 99
slug: standardise-maven-u-flag
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 99 — Standardise Maven builds on `-U` while the parent is a SNAPSHOT

## 1. Story

**As a** developer debugging a CI failure
**I want** every Maven invocation in CI to resolve the parent POM the same way
**So that** a red build tells me something about my commit, not about which runner cache happened
to be warm

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Issue: [#99](https://github.com/MRISS-Projects/dsh/issues/99)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the `0.3.0-SNAPSHOT` milestone on the
  issue, and follows `#92` and `#95`, which both shipped onto the same RC. `DEVELOP` is at
  `0.4.0-SNAPSHOT`.
- Spun off from `#95` §10, which ruled it out of scope as unrelated to credentials

Exactly two workflow steps in this repository invoke Maven, and they disagree:

| Location | Invocation |
|---|---|
| `.github/workflows/ci.yml:139` | `mvn -B install --file pom.xml` |
| `.github/workflows/api-testing.yml:146` | `mvn -B -U install -DskipTests` |

Both build the whole reactor, and both inherit `com.mriss.mriss-parent:products:3.8.0-SNAPSHOT`
(root `pom.xml:9-12`). So the same commit can resolve **two different parent POMs** depending on
which workflow is looking at it.

`ci.yml:135-137` carries a comment arguing the omission is deliberate:

```text
# No -U: the parent is a SNAPSHOT and -U would re-resolve it on every
# run, so the same commit could build differently on different days.
# Parent upgrades are deliberate and manual.
```

## 3. Why the existing rationale does not hold

Verified during step 3 rather than assumed. Omitting `-U` does not buy reproducibility, for two
independent reasons:

1. **Maven refreshes SNAPSHOT metadata on its own schedule.** The default update policy for
   snapshot repositories is `daily`. `ci.yml` therefore already re-resolves the parent without
   `-U` — just at an unpredictable moment, determined by the clock rather than by the commit.
2. **The runner's `~/.m2` is restored from a cache of varying age.** Both workflows configure
   `actions/setup-java@v4` with `cache: 'maven'` (`ci.yml:56`, `api-testing.yml:60`). Whether a
   given run sees a new parent depends on which cache entry was restored — state nobody can
   inspect from the build log.

So the current setup is not reproducible; it is non-reproducible *invisibly*. `-U` does not
introduce drift that was otherwise absent — it makes the existing drift consistent and legible.

Per `CLAUDE.md`, `MRISS-Projects/parent-poms` is the deliberate home for build-level changes and
DSH is their first consumer. While the parent is pinned to a `-SNAPSHOT`, tracking the current
parent on every run **is** the intended contract. `-U` states that contract instead of leaving it
to cache luck.

Real reproducibility arrives only when the parent is pinned to a released version. That is Wave 0's
closing goal (`specs/product/PRD.md` §4) and is explicitly not this story.

## 4. Files to change

### 4.1 `.github/workflows/ci.yml`

- **Line 139** — `mvn -B install --file pom.xml` becomes `mvn -B -U install --file pom.xml`.
- **Lines 135-137** — replace the comment. It must no longer argue against `-U`. The replacement
  states why tracking the current parent SNAPSHOT is intended, and what changes when the parent is
  pinned to a release, so that the future reader who pins `3.9.0` knows this flag is theirs to
  revisit.

### 4.2 `.github/workflows/api-testing.yml`

No change. Line 146 already passes `-U`; this story makes `ci.yml` match it, not the reverse.
Listed here because AC001 sweeps every workflow and this one must be confirmed, not assumed.

### 4.3 `docs/devops/README.md`

Two places, both currently load-bearing prose that a future reader will obey:

- **Line 66**, the `ci.yml` row of the Workflow Reference table, reads "(`mvn install`, no `-U`)".
  Restate it as `mvn -B -U install`.
- **`## Parent POM` (lines 119-145)** — the substantive edit. It currently says CI "builds with
  plain `mvn install`, deliberately without `-U`", and then:

  > **This is an accepted decision, not an oversight — do not "fix" it.** [...] `ci.yml` omitting
  > `-U` is the deliberate mitigation: it limits drift to Maven's daily refresh rather than forcing
  > a re-resolve on every run.

  The section is rewritten so that the *accepted decision* is the SNAPSHOT pin itself — which
  remains accepted, and remains something not to "fix" ahead of the parent-poms round trip — while
  `-U` is described as making that decision explicit rather than mitigating it. The reproducibility
  cost stays documented; only its attribution changes.

### 4.4 `CLAUDE.md`

**Line 74** — "CI never rebuilds `parent-poms` and never passes `-U`." Half of that sentence
becomes false. "CI never rebuilds `parent-poms`" stays true and stays. The `-U` clause is replaced
with the new contract.

This file sits outside AC004's stated grep scope — see §6.

### 4.5 `specs/product/PRD.md`

**§6, line 318** — "`.github/workflows/ci.yml` omits `-U` specifically to limit the drift — it does
not force a re-resolve on every run, only lets Maven's normal refresh interval apply." One
sentence, rewritten. Everything else in that bullet stands: the SNAPSHOT pin remains an accepted
decision, and the risk still closes when the parent is re-pinned to a released `3.9.0`.

§4's paragraph on `#99` (lines 94-99) is already written from the post-change point of view and
needs no edit.

## 5. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `release.yml`, `stage.yml`, `staging.yml`, `hotfix.yml` | Each is a thin `uses:` wrapper around a reusable workflow in `parent-poms`. They run no local `mvn` step, so AC001 does not reach them. How *those* workflows resolve the parent is a parent-poms change — see the round trip in `CLAUDE.md`. |
| `documentation-sync.yml`, `wiki-sync.yml`, `spec-validation.yml` | No Maven invocation at all. Confirmed by sweeping `.github/workflows/` for `mvn`. |
| `CLAUDE.md`'s Commands table | Local `mvn -B install` stays as-is. A developer deciding when to refresh their own `~/.m2` is not the inconsistency this story fixes, and `-U` on every local build of a 13-module reactor costs a metadata check for no gain. Decided explicitly, not overlooked. |
| `specs/stories/95-read-only-ci-package-token.md` | A shipped story's record. Its §10 already resolves *towards* `-U`, so it neither contradicts this change nor needs rewriting to avoid doing so. |
| `docs/superpowers/plans/2026-09-16-*.md`, `docs/superpowers/specs/2026-09-16-*.md` | **Banner added, body untouched.** Both are tracked, both sit in AC004's `docs/**` scope, and both carried the pre-change rule — the plan as a live `## Global Constraints` line ("Never pass `-U` to Maven in `ci.yml`") that an agent executing the plan would obey. Rewriting their bodies would falsify a record of what was decided on 2026-09-16, so each gets a status banner instead: completed/superseded, with `CLAUDE.md` and `docs/devops/README.md` named as authoritative and the `-U` and `DEPLOY_TOKEN` divergences called out. The second of those was already stale from `#95`. |
| Root `pom.xml` | Pinning a released parent is Wave 0's closing goal, not this story. |
| `cache: 'maven'` in both workflows | Kept. With `-U` the cache no longer decides *which* parent is resolved, only how much is re-downloaded — which is what a build cache should decide. |

## 6. Issue body reconciliation — AC004's scope is narrower than its intent

AC004 on the issue reads:

> No other document in the repository still recommends omitting `-U`. Checkable by grep across
> `docs/**`, `specs/**` and `.github/**`.

That grep misses `CLAUDE.md`, which lives at the repository root and contains exactly such a claim
at line 74. The intent ("no other document") is right; the stated check is too narrow to verify it.

**Resolution:** implement to the intent. AC004 below sweeps the whole tracked tree rather than
three directories. The issue body should be updated to match when the PR opens, so the issue and
the spec do not drift — `dsh-ship-story` handles that.

## 7. Acceptance criteria

- [ ] **AC001** — Every Maven invocation under `.github/workflows/` that builds this project passes
  `-U`. Checkable: sweeping tracked workflow files for `mvn` returns exactly two build invocations
  and both carry the flag.
- [ ] **AC002** — The comment above `ci.yml`'s build step no longer argues against `-U`; it states
  why tracking the current parent SNAPSHOT is intended, and what would change if the parent were
  pinned to a release.
- [ ] **AC003** — `## Parent POM` in `docs/devops/README.md` is rewritten to match, and no longer
  calls omitting `-U` "the deliberate mitigation" or tells the reader not to fix it.
- [ ] **AC004** — No document in the repository still recommends omitting `-U`. Checkable by
  grepping tracked Markdown for the flag, with the only surviving hits being ones that *recommend*
  it or record history. This sweep includes `CLAUDE.md` and `specs/product/PRD.md` — see §6.
- [ ] **AC005** — CI is green on the pull request, and the coverage gate is unaffected: no
  production code is touched, so no `.java` file appears in the diff.
- [ ] **AC006** — Markdown lint passes over the changed documentation, using the command in
  `CLAUDE.md`'s Commands table.

## 8. Testing approach

There is no code change, so there is no unit test to write and TDD's red-green cycle does not
apply. The gates are greps and the existing CI run:

1. **AC001 and AC004 are executable checks.** Run both greps locally before pushing and paste the
   output into the PR body.

   **Run AC004's sweep unfiltered.** The first attempt piped the results through
   `grep -v "^docs/superpowers/"`, on the assumption that the directory was agent scratch output.
   It is not: both files there are tracked, sit inside the `docs/**` scope AC004 names, and held
   the pre-change rule. The filter made the check pass by hiding its only real failures. Local
   code review caught it. Any exclusion applied to an AC's own verification must be justified in
   this spec, not applied at the command line.
2. **The real test is CI itself.** `ci.yml` runs on the pull request, so the changed build step
   executes against a real runner. A green run proves `-U` resolves `3.8.0-SNAPSHOT` successfully
   with the read-only package token from `#95`.

   **A local run cannot prove that, and did not.** Running the changed command locally
   (`mvn -B -U install`, `BUILD SUCCESS`, 13/13 modules) produced:

   ```text
   [WARNING] Could not transfer metadata com.mriss.mriss-parent:products:3.8.0-SNAPSHOT/maven-metadata.xml
   from/to MRISS-Projects-maven-repo (https://maven.pkg.github.com/MRISS-Projects/maven-repo):
   status code: 401, reason phrase: Unauthorized (401)
   ```

   The build then succeeded from the parent POM already in the local repository. This is worth
   recording as a property of the change rather than a defect in it, but the behaviour is
   **conditional on that cached copy**, and the two cases differ sharply:

   | Local repository | A 401 on the parent's metadata |
   |---|---|
   | Holds a usable `3.8.0-SNAPSHOT` parent | Degrades to a `WARNING`; the build proceeds against the cached, possibly stale parent |
   | Does not (fresh runner, evicted cache) | The SNAPSHOT cannot be resolved at all; Maven fails with a non-resolvable parent POM |

   So a broken credential is **not** harmless — it is intermittently invisible, which is worse. On
   CI it usually lands in the first row, because `actions/setup-java` restores `~/.m2`. Either way
   CI, where the credential exists, is the only place the resolution path is actually exercised.
3. **`api-testing.yml` is path-scoped** to `dsh-rest-api/**` and `specs/api/**`, so this PR will
   not trigger it. That is expected, and no reason to touch it — it is already on `-U`.
4. **Markdown lint** runs in `spec-validation.yml`, whose `paths:` filter covers `docs/**`,
   `specs/**` and `CLAUDE.md` — all three are in this diff, so the job will fire.

## 9. Out of scope

- **Pinning the parent to a released version.** The real fix for reproducibility, but it waits on
  `MRISS-Projects/parent-poms` settling — see the Wave 0 goal in `specs/product/PRD.md` §4.
- **Any change to `parent-poms` itself**, including its own workflows' use of `-U`. Separate round
  trip per `CLAUDE.md`.
- **The coverage ratchet.** `ci.yml`'s `Coverage ratchet` step (lines 141-146) sits directly below
  the build step this story edits, and `#93` removes it. This story does not touch it. If `#93`
  ships first, AC005's wording is the only thing affected and the conflict is a trivial one in
  `ci.yml`.
- **Pinning the Maven version in workflows.** That is `#87`.
- **Making a dead package token fail the build.** Raised by local review of this story. `ci.yml`'s
  "Verify package credentials are present" step (lines 57-62) checks only that the secret is
  non-empty, never that it authenticates. Under `-U` a token that has expired or lost
  `read:packages` produces a `401` metadata `WARNING` on *every* run while the build stays green
  off the `setup-java` cache — silently building against a frozen parent for as long as that cache
  holds a usable parent. When it does not, the same credential fault stops being a warning and
  fails the build outright, with no visible link back to the token. `-U` raises the
  frequency of that warning, so it makes an existing blind spot easier to ignore; it does not
  create it. A follow-up issue, not a change to this story.

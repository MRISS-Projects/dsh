---
issue: 101
slug: fail-ci-on-unusable-package-token
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 101 — Fail CI when the package token cannot authenticate

## 1. Story

**As a** developer trusting a green CI run
**I want** a package token that no longer authenticates to fail the build immediately
**So that** a credential problem surfaces as a credential problem, not as an unrelated build
failure days later

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Issue: [#101](https://github.com/MRISS-Projects/dsh/issues/101)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the `0.3.0-SNAPSHOT` milestone on the issue,
  and follows `#92`, `#95` and `#99`, which all shipped onto the same RC. `DEVELOP` is at
  `0.4.0-SNAPSHOT`.
- Spun off from `#99` / PR #100, out-of-scope list in
  `specs/stories/99-standardise-maven-u-flag.md` §9

Two workflows open with a step named `Verify package credentials are present`
(`.github/workflows/ci.yml:58-63`, `.github/workflows/api-testing.yml:62-67`). Both contain the
same four lines, and both test only that the secret is a non-empty string.

## 3. What the present check proves, and what it does not

The step answers "did a secret reach this run?" — a real question, and the message it prints is the
right answer to it, because the usual cause is a pull request from a fork, which receives no
repository secrets. That branch is correct and this story keeps it verbatim.

What it never asks is whether the string works. A token that has expired, been revoked, lost the
`read:packages` scope or lost access to the `MRISS-Projects` organisation is still a non-empty
string, so it passes and the job proceeds to Maven.

What Maven then does depends on the runner's local repository, which is why the fault is hard to
notice. Observed directly while building `#99`, running `mvn -B -U install` with no working
credential:

```text
[WARNING] Could not transfer metadata com.mriss.mriss-parent:products:3.8.0-SNAPSHOT/maven-metadata.xml
from/to MRISS-Projects-maven-repo (https://maven.pkg.github.com/MRISS-Projects/maven-repo):
status code: 401, reason phrase: Unauthorized (401)
```

| Local repository | A 401 on the parent's metadata |
|---|---|
| Holds a usable `3.8.0-SNAPSHOT` parent | Degrades to a `WARNING`; `BUILD SUCCESS` against the cached, possibly stale parent |
| Does not (fresh runner, evicted cache) | The SNAPSHOT cannot be resolved at all; Maven fails with a non-resolvable parent POM |

So a dead credential is not harmless — it is **intermittently invisible**. `actions/setup-java`
restores `~/.m2`, so the usual outcome is the first row: green runs building against a frozen
parent, with a warning nobody reads. When the cache eventually misses, the same fault arrives as a
build failure with nothing tying it back to the token.

**`#99` did not create this.** It made the metadata refresh happen on every run rather than on
Maven's daily schedule, which raises how often the warning appears and therefore how reliably it is
ignored. The blind spot is the presence-only check, and it predates `#99`.

## 4. How the registry behaves — measured, not assumed

The design below rests on one property of `maven.pkg.github.com`, so it was measured first rather
than reasoned about. Each cell is the HTTP status from a bare `curl`, run on 2026-09-17:

| Path under `.../MRISS-Projects/maven-repo` | No credential | Invalid credential |
|---|---|---|
| `/com/mriss/mriss-parent/products/3.8.0-SNAPSHOT/maven-metadata.xml` | 401 | 401 |
| `/com/mriss/mriss-parent/products/maven-metadata.xml` | 401 | 401 |
| `/com/mriss/mriss-parent/products/9.9.9-NOPE/maven-metadata.xml` | 401 | 401 |
| `/does/not/exist/at/all.txt` | 401 | 401 |

**The registry authenticates before it resolves the path.** A request for an artifact that cannot
exist still answers `401` when the credential is bad. Two consequences drive the design:

1. A `401` from this host means the credential, and only the credential. It cannot be a typo in the
   URL, because a typo reaches path resolution only after authentication has already succeeded.
2. A `404` is therefore *positive evidence that the credential authenticated*. It says the token is
   usable and the URL this step built is wrong — a defect in the check, not in the secret.

## 5. The check

One authenticated `GET` against the registry, using the same credential and the same host Maven is
about to use. It tests the real thing rather than a proxy for it, so expiry, revocation, a lost
scope, lost organisation access and an unauthorised SSO session all collapse into the same
observable answer.

```text
2xx|3xx → pass, log which artifact was reachable
401|403 → ::error:: naming the token and the likely causes, exit 1
404     → ::warning::, pass  (see §4 — the credential authenticated)
other   → ::warning::, pass  (5xx, 429, connection failure)
```

### 5.1 Why everything except `401` and `403` passes

By §4, only `401` and `403` can be laid at the credential's door. Every other status is something
the registry produces *after* authenticating, so failing on one would report a credential fault the
response has already ruled out.

- **`2xx` and `3xx` pass**, logging one line that names the artifact reached and the status — no
  warning annotation. A redirect to an object store is a normal way for a package registry to serve
  an artifact, and an unusable token never gets one; it gets a `401`. Matching `200` alone would
  leave a build that is working perfectly printing "could not verify" on every run, which is the
  warning-nobody-reads failure of §3 rebuilt in miniature. Caught in review; see §11.1.
- **`404` warns and passes.** It proves the token works and says the URL has gone stale, so it
  names the URL.
- **`5xx`, `429` and a connection failure warn and pass.** They say nothing about the credential
  either way. Failing there would invent a new flaky gate in front of a build that is about to
  contact the same host and will report its own error, in its own words, if the registry is
  genuinely down.

`403` is the one judgement call in that list. It is the usual answer to a token that authenticates
but lacks `read:packages`, which is squarely this story's target — but GitHub also returns `403` for
secondary rate limits, which is the flaky case the previous paragraph argues against. It stays a
failure because the scope fault is far the likelier of the two here, and `docs/devops/README.md`
words the message as "check the token first" rather than as proof the token is dead.

### 5.2 Why the URL is derived from `pom.xml`

The step reads the `<parent>` block rather than hardcoding coordinates:

```text
groupId com.mriss.mriss-parent → com/mriss/mriss-parent
artifactId products
→ https://maven.pkg.github.com/MRISS-Projects/maven-repo/com/mriss/mriss-parent/products/maven-metadata.xml
```

A hardcoded URL would go stale the next time the parent moves, and by §5.1 a stale URL degrades to a
warning — the exact silent-pass failure mode this story exists to remove. Deriving it keeps the
check pointed at whatever the repository actually inherits from.

**Artifact-level metadata, not the versioned SNAPSHOT metadata.** Wave 0 closes with the root
`pom.xml` pinned to a released `3.9.0` (`specs/product/PRD.md` §4). Released versions have no
per-version `maven-metadata.xml` — that file exists only for a SNAPSHOT line — so a versioned URL
would begin answering `404` on the day that pin lands. The artifact-level metadata exists for both.
The trade-off is that the probe checks read access to the artifact rather than to one specific
version; version resolution is Maven's job a few steps later, and it now runs with a credential
already proven to work.

**An unreadable `<parent>` block fails the step.** If the extraction ever stops matching, the URL is
malformed, the request `404`s, and by §5.1 the check would pass silently from then on. Empty
coordinates are therefore an `::error::` in their own right.

**Comments are stripped before the block is read** — same-line first, then multi-line ranges. A
commented-out `<parent>` above the live one would otherwise win, and its dead coordinates answer
`404` even with a perfectly good token: warn, pass, and test nothing, for as long as the comment
survives. That is not hypothetical. The root `pom.xml` already carries a comment immediately above
`<parent>`, and Wave 0 closes by editing the very version inside it — commenting out the old line
is exactly how a person does that. Caught in review; see §11.1.

**Each coordinate is matched as an element, not as a line.** `grep -o "<tag>[^<]*</tag>"` finds
`<groupId>` wherever it sits, so the block's line layout does not matter. The first attempt split
lines on `>` and `<` instead, which quietly assumed one element per line — and a legal `<parent>`
that puts the three coordinates on a single line then collapsed all of them onto the *first* value,
producing `g:g` from `<groupId>g</groupId><artifactId>a</artifactId>`. Three non-empty values passed
the guard, so the step probed a URL for an artifact that does not exist, collected a `404` from a
working token, warned, and checked nothing — the same silent-pass this section exists to prevent,
arrived at from a different direction. Raised by Copilot on PR #106; see §11.2.

## 6. Files to change

### 6.1 `.github/workflows/ci.yml`

Replace the step at `ci.yml:58-63`. Rename it from `Verify package credentials are present` to
`Verify package credentials work` — the old name is an accurate description of the old behaviour and
would be a misleading one for the new.

```yaml
      # Checks that the token works, not merely that the secret is non-empty.
      # An expired or revoked token is still a non-empty string, and Maven's
      # reaction to one depends on whether the restored ~/.m2 happens to hold a
      # usable parent - so it surfaces either as a WARNING on a green build
      # against a frozen parent, or, once the cache misses, as a non-resolvable
      # parent POM with nothing tying it back to the credential. See the
      # Secrets section of docs/devops/README.md.
      - name: Verify package credentials work
        run: |
          if [ -z "$PACKAGES_READ_TOKEN" ]; then
            echo "::error::PACKAGES_READ_TOKEN is unavailable to this run. Pull requests from forks do not receive repository secrets; otherwise add it under Settings → Secrets and variables → Actions."
            exit 1
          fi

          # Comments are stripped first - same-line, then multi-line - because a
          # commented-out <parent> above the live one would otherwise win, and
          # its dead coordinates answer 404 even with a perfectly good token:
          # the check would warn and pass forever while testing nothing. The
          # coordinates are then read element by element, so the block's line
          # layout does not matter.
          parent=$(sed -e 's/<!--.*-->//g' -e '/<!--/,/-->/d' pom.xml | sed -n '/<parent>/,/<\/parent>/p')

          # grep -o matches the element wherever it sits, rather than assuming
          # one element per line. Splitting the line instead collapsed all three
          # coordinates onto the first value whenever a legal <parent> put them
          # on one line - and that passed the guard below, so the step probed a
          # nonsense URL, collected a 404 from a working token, and warned
          # instead of checking anything.
          coord() {
            echo "$parent" | grep -o "<$1>[^<]*</$1>" | head -1 | sed -e "s|<$1>||" -e "s|</$1>||" | tr -d ' '
          }

          group=$(coord groupId)
          artifact=$(coord artifactId)
          version=$(coord version)

          if [ -z "$group" ] || [ -z "$artifact" ] || [ -z "$version" ]; then
            echo "::error::Could not read the <parent> coordinates from pom.xml, so the credential cannot be checked. Fix this step rather than skipping it: a malformed URL would make the check pass silently."
            exit 1
          fi

          url="https://maven.pkg.github.com/MRISS-Projects/maven-repo/$(echo "$group" | tr '.' '/')/$artifact/maven-metadata.xml"

          # || code="000" is load-bearing: Actions runs this under `bash -e`, so
          # a curl that exits non-zero on a network failure would abort the step
          # instead of reaching the warning below. The grep pipelines above
          # survive a no-match only because there is no pipefail - adding an
          # explicit `shell: bash` here would set it, and the coordinates guard
          # would then abort with no output at all, printing no ::error::.
          code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 20 -u "$GITHUB_ACTOR:$PACKAGES_READ_TOKEN" "$url") || code="000"

          case "$code" in
            2??|3??)
              # Anything the registry can only answer once authenticated. A
              # redirect to an object store counts: an unusable credential never
              # gets one, it gets a 401.
              echo "PACKAGES_READ_TOKEN can read $group:$artifact (parent $version) - HTTP $code."
              ;;
            401|403)
              echo "::error::PACKAGES_READ_TOKEN is set but cannot read $group:$artifact from GitHub Packages (HTTP $code). The secret is present, so this is not a missing-secret problem: the token has expired, has been revoked, has lost the read:packages scope, or has lost access to the MRISS-Projects organisation. Replace it under Settings → Secrets and variables → Actions with a classic PAT holding read:packages. See the Secrets section of docs/devops/README.md."
              exit 1
              ;;
            404)
              echo "::warning::$url returned 404. The credential authenticated - this registry answers 401 for any unauthenticated request whatever the path - so PACKAGES_READ_TOKEN is usable and the build continues. The URL this step derives from the <parent> block of pom.xml is stale and should be corrected."
              ;;
            *)
              echo "::warning::Could not verify PACKAGES_READ_TOKEN against $url (HTTP $code). Letting the build proceed; Maven reports its own error if the registry is genuinely unreachable."
              ;;
          esac
```

Four details that are deliberate and easy to "tidy" into bugs:

- **`|| code="000"`.** GitHub Actions runs `run:` blocks under `bash -e`. Without it, a `curl` that
  exits non-zero on a network failure aborts the step, turning §5.1's warning into the failure that
  section argues against.
- **No `shell:` key, therefore no `pipefail`.** The `grep` pipelines that read the coordinates
  return 0 on a no-match only because errexit is unaccompanied by pipefail. Adding an explicit
  `shell: bash` sets `-eo pipefail`, and the coordinates guard would then abort the step *before*
  printing its `::error::` — a silent exit 1 in place of a diagnosis. Verified in review (§11.1), and re-confirmed against CI in §11.2.
- **`--max-time 20`.** Bounds the step's contribution to job duration whatever the registry does.
- **`$GITHUB_ACTOR` as the username.** Matches what the generated `settings.xml` already uses for
  the same three server entries, a few lines below.

### 6.2 `.github/workflows/api-testing.yml`

The same replacement at `api-testing.yml:62-67` (AC005). That workflow builds the same reactor from the same
parent with the same secret, so a credential fault reaches it identically.

The step is copied rather than shared. Both files already carry a verbatim duplicate of the
~40-line `Configure Maven settings for GitHub Packages` block; introducing a composite action for
the 10-line credential step while leaving the 40-line block duplicated beside it would be a
half-applied convention, harder to read than the duplication it replaces. Extracting *both* steps
into one composite action is the coherent end state and is recorded in §10 as a follow-up.

### 6.3 `docs/devops/README.md`

Lines 110-113 describe the old behaviour and the old step name:

> Both `ci.yml` and `api-testing.yml` open with a `Verify package credentials are present` step that
> fails with an explicit `::error::` annotation when the secret is missing. Without it an absent
> secret surfaces much later as a Maven 401 on `com.mriss.mriss-parent:products`, which reads as a
> broken parent rather than a missing credential.

Replace those four lines with the following. It stays inside `## Secrets`, immediately after the
classic-PAT requirement, which is where a reader who has just been told to create a token will look
for how its failure shows up.

> Both `ci.yml` and `api-testing.yml` open with a `Verify package credentials work` step that fails
> the job before Maven runs — both when the secret is missing and when it is present but unusable.
> The missing case is almost always a pull request from a fork, which receives no repository
> secrets. The unusable case is a token that has expired, been revoked, lost the `read:packages`
> scope, or lost access to the organisation; the step catches it with one authenticated `GET` for
> the parent's artifact-level `maven-metadata.xml`, at coordinates read from the root `pom.xml`.
>
> A `401` or `403` there is conclusive. `maven.pkg.github.com` authenticates before it resolves a
> path, so it answers `401` even for an artifact that does not exist — which means the status
> cannot be blamed on a wrong URL. The step fails on those two codes and names the token. A `404`
> means the opposite: the credential authenticated and the URL the step derived has gone stale. That
> logs a warning and lets the build continue, as does a `5xx` or a connection failure, which say
> nothing about the credential and precede a build that is about to contact the same host anyway.
>
> Without this step an unusable credential surfaces much later as a Maven `401` on
> `com.mriss.mriss-parent:products`, which reads as a broken parent rather than a broken
> credential — or, when the runner's restored `~/.m2` still holds a usable parent, as a `WARNING` on
> an otherwise green build compiling against a frozen one.

## 7. Files that deliberately stay unchanged

| File | Why |
|---|---|
| Root `pom.xml` | The check reads it; nothing about it needs to change. Pinning a released parent is Wave 0's closing goal, not this story. |
| The `Configure Maven settings for GitHub Packages` step in both workflows | Untouched. It consumes the credential; this story validates it. Sharing the two duplicated blocks is §10's follow-up. |
| `release.yml`, `stage.yml`, `staging.yml`, `hotfix.yml` | Thin `uses:` wrappers around reusable workflows in `parent-poms`, holding `DEPLOY_TOKEN`, not `PACKAGES_READ_TOKEN`. How those authenticate is a parent-poms change — see the round trip in `CLAUDE.md`. |
| `documentation-sync.yml`, `wiki-sync.yml`, `spec-validation.yml` | None resolves anything from the Maven registry. `wiki-sync.yml` uses the built-in `GITHUB_TOKEN`; `spec-validation.yml` touches no Maven build at all. |
| `CLAUDE.md` | Its Quality gates and Commands sections describe the build, not the credential path. No sentence there claims the current check does more than it does. |
| The empty-secret branch of the step | Kept verbatim, message included. It is the right answer to the fork-PR case, which is the common one, and §3 explains why it is not the case this story is about. |

## 8. Acceptance criteria

- [ ] **AC001** — A `PACKAGES_READ_TOKEN` that is present but cannot read the parent artifact fails
  `ci.yml` at the credential step, before `Build and test all modules` runs. Verified by §9.3.
- [ ] **AC002** — The failure message names `PACKAGES_READ_TOKEN`, states that the secret *is*
  present so the reader does not go looking for a missing one, lists expiry, revocation, a lost
  `read:packages` scope and lost organisation access as the causes, and points at
  `docs/devops/README.md`. It does not surface as a Maven resolution error.
- [ ] **AC003** — Every branch of the step is exercised against a deliberately invalid token or a
  forced status code, not assumed. The harness is `scripts/test-credential-step.sh`, tracked rather
  than thrown away, so the evidence is reproducible and a later edit to the step has something to
  fail against. Transcripts in §9.
- [ ] **AC004** — A valid token still passes and the step costs a small, bounded amount of time. The
  invalid-token path completed in 913 ms locally (§9.3) and `--max-time 20` bounds the worst case;
  the valid-token path is one request of the same shape. Confirmed by the PR's own CI run — see the
  honest limitation in §9.4.
- [ ] **AC005** — `api-testing.yml` carries the identical step (§6.2).
- [ ] **AC006** — `docs/devops/README.md` `## Secrets` describes the new behaviour, and no longer
  says the step checks only for a missing secret. The old step name survives on no live surface:
  `git grep "Verify package credentials are present" -- . ':!specs/stories/'` returns nothing. The
  one exemption is declared and justified in §8.1.
- [ ] **AC007** — Markdown lint passes over the changed documentation, using the command in
  `CLAUDE.md`'s Commands table.

AC001-AC006 are the issue's own criteria in its own order, tightened where §9 made them checkable.
**AC007 is additive** — the issue does not carry it, because it was written before the story was
known to touch documentation. Nothing here narrows the issue, so no reconciliation of the issue body
is needed beyond adding AC007 when the PR opens; `dsh-ship-story` handles that.

### 8.1 AC006's sweep carries one exemption, declared rather than filtered

AC006 first read "the old step name appears nowhere in tracked Markdown". Run unfiltered during the
build, that sweep **failed** — six hits across three files:

| File | Hits | What they are |
|---|---|---|
| `specs/stories/101-fail-ci-on-unusable-package-token.md` | 3 | This spec: the before-state in §2, the rename in §6.1, the superseded paragraph quoted in §6.3 |
| `specs/stories/95-read-only-ci-package-token.md` | 2 | A shipped story's record of the step as `#95` left it |
| `specs/stories/99-standardise-maven-u-flag.md` | 1 | The out-of-scope paragraph that raised `#101` |

Not one is a live instruction. Rewriting the two shipped specs would falsify the record of what was
decided and when; rewriting this one would delete the before-state that makes the change legible.

So the criterion was wrong, not the code. AC006 now exempts `specs/stories/` and says so here —
`#99` §8 records what happens when an AC's own sweep is quietly narrowed at the command line to make
it pass, and this is the same temptation arriving from the other direction. Every surface a reader
might act on — both workflow files, `docs/devops/README.md`, `CLAUDE.md` — is clean.

## 9. Verification and evidence

No production source changes, so `jacoco:check` and surefire are unaffected and TDD's red-green
cycle has no unit test to hang on. The cycle still applies, with the step body as the unit under
test.

### 9.1 The harness

`scripts/test-credential-step.sh` drives the step through every branch. It takes a workflow file as
its argument, so the same suite runs against both. Two decisions make it a test of the change rather
than a test of a copy of it:

- **It extracts the `run:` body from the workflow file itself** and executes that, so nothing can
  pass while the shipped YAML says something else. The extractor matches the step-name *prefix*, so
  it finds the old step as readily as the new one — which is what lets the same suite produce a
  meaningful red.
- **It runs the body under `bash -e`**, because that is the shell GitHub Actions gives a `run:`
  block. Without it the `|| code="000"` guard of §6.1 would appear to work when it does not.

Statuses the live registry cannot return to this machine — `200`, `404`, `5xx`, and a `curl` that
exits non-zero — are produced by a stub `curl` prepended to `PATH`, which prints a chosen status the
way `-w '%{http_code}'` does. The `401` cases use the real registry.

### 9.2 Red — the step as it stood

The suite, run against `ci.yml` before the change:

```text
Extracted 5 lines of step body from .github/workflows/ci.yml

  PASS  absent secret fails and blames the fork, not the token
  FAIL  invalid token fails
          expected exit 1 containing: cannot read com.mriss.mriss-parent:products
          got exit 0: <no output>
  ... 10 further failures, all "got exit 0: <no output>"
  PASS  step completed in 80 ms (< 5000 ms)

  2 passed, 11 failed
```

Every failure is the same defect: handed a credential that cannot authenticate, the step says
nothing and exits 0. The two passes are the branches that must survive the change — the fork-PR
message, and the cost bound.

### 9.3 Green — after the change

Identical suite, same extraction, against the shipped `ci.yml`. Five of these cases postdate a
review round: the `302` and the two commented-`<parent>` cases from §11.1, and the two one-line
cases from §11.2.

```text
Extracted 61 lines of step body from .github/workflows/ci.yml

Against the live registry (needs network):
  PASS  absent secret fails and blames the fork, not the token
  PASS  invalid token fails
  PASS  invalid token says the secret IS present
  PASS  invalid token lists the causes
  PASS  invalid token points at the documentation
  PASS  unreadable <parent> fails rather than probing a malformed URL

POM parsing, with the probe stubbed out:
  PASS  a commented-out <parent> is ignored in favour of the live one
  PASS  a commented-out <version> inside the live block is ignored too
  PASS  a <parent> written entirely on one line is read correctly
  PASS  coordinates sharing one line do not collapse onto the first value

Against a stubbed curl, for statuses the live registry cannot return without a working credential:
  PASS  200 passes and names the artifact it reached
  PASS  403 fails like 401
  PASS  404 warns and passes, because it proves the credential authenticated
  PASS  404 names the stale URL so it can be fixed
  PASS  302 passes: a redirect proves the credential authenticated, same as 404
  PASS  500 warns and passes rather than inventing a flaky gate
  PASS  a curl that exits non-zero does not abort the step under bash -e

AC004 - cost of the step:
  PASS  step completed in 931 ms (< 5000 ms)

  18 passed, 0 failed
```

`api-testing.yml` run through the same suite: **18 passed, 0 failed**. The two extracted bodies were
diffed and are byte-identical, 61 lines each, `md5 20df7d0777102dbf7c78f4c8d3200dbb` — which is
AC005's real check, stronger than reading them side by side.

Both files were also parsed with `js-yaml` after editing, confirming the literal block survives as
YAML and that the step is named `Verify package credentials work` in each.

### 9.4 What the harness proves, and what only CI can

**The stub proves the `case`, not the registry.** Every branch of the step's logic is now exercised,
including `200` — but against a stubbed status, so it shows the step does the right thing *when* the
registry answers `200`. It does not show that the registry answers `200` to an authenticated request
for artifact-level metadata. Nothing on this machine can: reading the real token from the local
`settings.xml` was denied by the agent's credential guard, and `#99` §8 records that credential
answering `401` here anyway.

**The PR's own CI run closes that gap**, running `ci.yml` with the real secret. If the URL is wrong
the step logs the `404` warning and the build proceeds (§5.1), so a bad guess degrades the check
rather than blocking the PR — and the warning names the URL, making it a one-commit fix. Watch for
it on the first run and treat it as work remaining, not noise.

**One shape of that gap is now closed by construction.** Before §11, only a literal `200` passed, so
a registry that redirects artifact `GET`s to an object store would have warned on every green run
for the life of the step. `2??|3??` removes that failure mode without waiting to find out which way
the registry behaves. The remaining thing the first run can still reveal is the `404` — a wrong
URL — and that one announces itself.

**`api-testing.yml` will not run on this PR.** Its `paths:` filter covers `dsh-rest-api/**` and
`specs/api/**`; this diff touches neither. Its copy is covered by the byte-identical diff above and
by its own passing run of the suite.

### 9.5 Documentation

Markdown lint runs in `spec-validation.yml`, whose `paths:` filter covers `docs/**` and `specs/**`.
Both are in this diff, so the job fires. Run the command from `CLAUDE.md`'s Commands table locally
before pushing (AC007).

## 10. Out of scope

- **Sharing the duplicated workflow steps.** Both workflows carry a verbatim copy of the credential
  step and of the ~40-line `Configure Maven settings for GitHub Packages` block. Extracting both
  into a single composite action under `.github/actions/` is the coherent end state, and doing only
  the small half here would be worse than the duplication. **Raise as a follow-up issue** when this
  story ships, the way `#101` itself was raised from `#99`.
- **Changing what the token can do.** `#95` settled the scope at `read:packages`; this story checks
  that the scope works, not what it should be.
- **Any change to `MRISS-Projects/parent-poms`**, including how its reusable release workflows
  authenticate, and whether `DEPLOY_TOKEN` deserves an equivalent check. Separate round trip per
  `CLAUDE.md`.
- **Removing `-U`.** `#99` settled that direction; the flag is what makes the symptom frequent, not
  what makes it invisible.
- **Pinning the parent to a released version.** Wave 0's closing goal (`specs/product/PRD.md` §4).
  It would let §5.2 probe a versioned path, but it is not this story.
- **Running `scripts/test-credential-step.sh` from CI.** It is a manual script, like
  `scripts/close-wontfix-issues.sh` beside it. Wiring it into `spec-validation.yml` would mean a new
  job and a decision about running the live-registry cases on every PR — a bigger change than the
  step it guards.

## 11. Review rounds

### 11.1 Local review (step 5)

A reviewer read the diff and re-measured the claims it rests on rather than
taking them: it reproduced §4's probe table independently, confirmed `bash -e` from this repository's
own CI logs, diffed the fork-PR branch against the base commit to confirm it is untouched, and drove
eight status codes through the extracted body. No Critical findings. Three were acted on, and each
one produced a test before it produced a fix:

| Finding | Verdict | What changed |
|---|---|---|
| Only `200` passed, so a redirecting registry would warn on every green run | **Accepted** | `2??\|3??`. The gap was between §4's own argument — everything but `401`/`403` proves authentication — and a `case` that implemented only part of it |
| A commented-out `<parent>` above the live one is read instead of it | **Accepted** | Comments stripped before the range. Reproduced first: the step read `old.group:old-artifact`, got a `404` with a good token, warned, passed |
| `docs/devops/README.md` called `403` "conclusive" | **Accepted** | Softened. `403` stays a failure — the scope fault is likelier here than a rate limit — but the prose no longer claims certainty it does not have |
| The harness was session scratch, so AC003 was unreproducible | **Accepted** | Committed as `scripts/test-credential-step.sh` |
| Spec cited "lines 57-62" for both files | **Accepted** | Verified wrong at the base commit; now `ci.yml:58-63` and `api-testing.yml:62-67` |
| §6.1's snippet had drifted from the file | **Accepted** | Regenerated from `ci.yml` rather than hand-synced |
| An explicit `shell: bash` would set `pipefail` and silence every `::error::` | **Accepted as a comment** | Latent, not a defect — the step does not set `shell:`. Documented in the step and in §6.1 so a future edit does not walk into it |
| A one-line `<parent>` block fails on valid XML | **Partly** | Reproduced. Fixing it properly needs an XML parser; failing loudly is the designed behaviour, so it got a comment, not a rewrite (§5.2) |
| Unclosed `<parent>` reports the wrong version; missing `pom.xml`; missing `curl` | **Declined** | Cosmetic, or unreachable after `actions/checkout`. Guarding states that cannot occur adds noise to a step whose whole value is a legible message |

The first two are the ones worth remembering: both were cases where the implementation was narrower
than the reasoning the spec had already written down, and neither was visible from the passing test
suite — the suite had no test for them, which is why it stayed green.

**One verdict from this round was overturned in the next.** "A one-line `<parent>` block fails on
valid XML" was marked *partly* and answered with a comment, on the strength of a reproduction
showing the guard firing. That reproduction was of the wrong input — see §11.2.

### 11.2 Copilot review, round 1 (step 7)

PR [#106](https://github.com/MRISS-Projects/dsh/pull/106), first CI run green on every check.
Copilot returned **🔵 Needs a closer look** with **6 suppressed comments and 0 posted threads** —
which means there was nothing to resolve on the PR page, and the findings had to be read out of the
review body. The review arrived **automatically**, so it ran at the repository or organisation
default effort level, not at a per-PR choice.

Six comments, three distinct claims; the first two were each raised twice, once per workflow.

| Claim | Verdict | Outcome |
|---|---|---|
| A legal one-line `<parent>` block defeats the parser, so the guard passes and the probe uses a malformed URL | **Right conclusion, wrong mechanism — fixed** | See below |
| Actions runs the step with `-eo pipefail`, so a no-match `grep` aborts before the guard can print | **Incorrect** | Answered with evidence; no change |
| §5.1 claims `2xx`/`3xx` "pass silently" while the step prints a success line | **Valid** | Wording corrected |

**The parser finding, and why the stated mechanism mattered.** Copilot's example was a block written
entirely on one line, and its reasoning was that `cut -d'>' -f2` "returns the group value for all
three fields". Run against that exact input, the step exits 1 with the coordinates guard — because
the line begins `<parent>`, so the second field is `<groupId`, which the following `cut` empties. The
stated mechanism does not occur there.

It does occur one line further in. With `<parent>` on its own line and the three coordinates sharing
the next one, all three fields collapse onto the first value:

```text
<parent>
  <groupId>g.two</groupId><artifactId>a-two</artifactId><version>v2</version>
</parent>

  before: PACKAGES_READ_TOKEN can read g.two:g.two (parent g.two) - HTTP 200.   exit=0
```

Three non-empty values, guard satisfied, nonsense URL, `404` from a working token, warning, pass.
The finding was right that the parser is defeated by a legal POM and right that the consequence is a
silent pass; it was wrong about which POM and wrong about the mechanism. Fixing what it described
would have changed nothing, because that case already failed correctly.

The fix reads each coordinate as an element rather than as a line (§5.2), which covers both layouts
— the one Copilot named now parses correctly instead of failing the guard, and the one it did not
name stops collapsing. Two tests, red first, then the change.

**The `pipefail` claim, settled by evidence rather than by position.** GitHub sets `-eo pipefail`
only when a step declares `shell: bash`; a bare `run:` gets `bash -e`. This PR's own run says so:

```text
Verify package credentials work    shell: /usr/bin/bash -e {0}
```

No workflow in this repository sets a `shell:` key. So the pipelines do survive a no-match, the
guard does print its `::error::` — CI demonstrated exactly that on the run under review — and
running the harness under `pipefail` would model a shell this repository does not use. Declined, and
the reply quotes the log line. The step already carries a comment about this trap, which is what
makes the claim easy to answer rather than easy to accept.

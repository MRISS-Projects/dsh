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
(`.github/workflows/ci.yml:57-62`, `.github/workflows/api-testing.yml:57-62`). Both contain the
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
200     → pass, log which artifact was reachable
401|403 → ::error:: naming the token and the likely causes, exit 1
404     → ::warning::, pass  (see §4 — the credential authenticated)
other   → ::warning::, pass
```

### 5.1 Why `404` and `5xx` pass rather than fail

By §4 a `404` proves the token works, so failing on it would report a credential fault that the
response has just ruled out. It still deserves a warning, because the URL needs fixing.

A `5xx` or a connection failure says nothing about the credential either way. Failing there would
invent a new flaky gate in front of a build that is about to contact the same host and will report
its own error, in its own words, if the registry is genuinely down. This story is about making a
credential fault legible, not about adding a second opinion on the registry's uptime.

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

## 6. Files to change

### 6.1 `.github/workflows/ci.yml`

Replace the step at lines 57-62. Rename it from `Verify package credentials are present` to
`Verify package credentials work` — the old name is an accurate description of the old behaviour and
would be a misleading one for the new.

```yaml
      - name: Verify package credentials work
        run: |
          if [ -z "$PACKAGES_READ_TOKEN" ]; then
            echo "::error::PACKAGES_READ_TOKEN is unavailable to this run. Pull requests from forks do not receive repository secrets; otherwise add it under Settings → Secrets and variables → Actions."
            exit 1
          fi

          parent=$(sed -n '/<parent>/,/<\/parent>/p' pom.xml)
          group=$(echo "$parent" | grep '<groupId>' | head -1 | cut -d'>' -f2 | cut -d'<' -f1 | tr -d ' ')
          artifact=$(echo "$parent" | grep '<artifactId>' | head -1 | cut -d'>' -f2 | cut -d'<' -f1 | tr -d ' ')
          version=$(echo "$parent" | grep '<version>' | head -1 | cut -d'>' -f2 | cut -d'<' -f1 | tr -d ' ')

          if [ -z "$group" ] || [ -z "$artifact" ] || [ -z "$version" ]; then
            echo "::error::Could not read the <parent> coordinates from pom.xml, so the credential cannot be checked. Fix this step rather than skipping it: a malformed URL would make the check pass silently."
            exit 1
          fi

          url="https://maven.pkg.github.com/MRISS-Projects/maven-repo/$(echo "$group" | tr '.' '/')/$artifact/maven-metadata.xml"

          code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 20 -u "$GITHUB_ACTOR:$PACKAGES_READ_TOKEN" "$url") || code="000"

          case "$code" in
            200)
              echo "PACKAGES_READ_TOKEN can read $group:$artifact (parent $version)."
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

Three details that are deliberate and easy to "tidy" into bugs:

- **`|| code="000"`.** GitHub Actions runs `run:` blocks under `bash -e`. Without it, a `curl` that
  exits non-zero on a network failure aborts the step, turning §5.1's warning into the failure that
  section argues against.
- **`--max-time 20`.** Bounds the step's contribution to job duration whatever the registry does.
- **`$GITHUB_ACTOR` as the username.** Matches what the generated `settings.xml` already uses for
  the same three server entries, a few lines below.

### 6.2 `.github/workflows/api-testing.yml`

The same replacement at lines 57-62 (AC005). That workflow builds the same reactor from the same
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
  `ci.yml` at the credential step, before `Build and test all modules` runs. Verified by §9.2.
- [ ] **AC002** — The failure message names `PACKAGES_READ_TOKEN`, states that the secret *is*
  present so the reader does not go looking for a missing one, lists expiry, revocation, a lost
  `read:packages` scope and lost organisation access as the causes, and points at
  `docs/devops/README.md`. It does not surface as a Maven resolution error.
- [ ] **AC003** — Every branch of the step is exercised against a deliberately invalid token or a
  forced status code, not assumed. Transcripts in §9.
- [ ] **AC004** — A valid token still passes and the step costs a small, bounded amount of time. The
  invalid-token path completed in 841 ms locally (§9.2) and `--max-time 20` bounds the worst case;
  the valid-token path is one request of the same shape. Confirmed by the PR's own CI run — see the
  honest limitation in §9.5.
- [ ] **AC005** — `api-testing.yml` carries the identical step (§6.2).
- [ ] **AC006** — `docs/devops/README.md` `## Secrets` describes the new behaviour, and no longer
  says the step checks only for a missing secret. The old step name appears nowhere in tracked
  Markdown.
- [ ] **AC007** — Markdown lint passes over the changed documentation, using the command in
  `CLAUDE.md`'s Commands table.

AC001-AC006 are the issue's own criteria in its own order, tightened where §9 made them checkable.
**AC007 is additive** — the issue does not carry it, because it was written before the story was
known to touch documentation. Nothing here narrows the issue, so no reconciliation of the issue body
is needed beyond adding AC007 when the PR opens; `dsh-ship-story` handles that.

## 9. Verification and evidence

No production source changes, so `jacoco:check` and surefire are unaffected and TDD's red-green
cycle has no unit test to hang on. The cycle still applies, with the step body as the unit under
test: extract the `run:` block to a shell script, drive it through every branch, and only then paste
it into the two workflows.

### 9.1 Red — the current step passes a token that cannot authenticate

The existing four lines, run with an invalid token:

```text
$ PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" bash verify-credentials-current.sh
exit=0
```

No output, exit 0, job continues. That is the defect, reproduced.

### 9.2 Green — the new step fails it, and says why

```text
$ PACKAGES_READ_TOKEN="ghp_deliberatelyInvalid000000000000000000" bash verify-credentials.sh
::error::PACKAGES_READ_TOKEN is set but cannot read com.mriss.mriss-parent:products from GitHub
Packages (HTTP 401). The secret is present, so this is not a missing-secret problem: the token has
expired, has been revoked, has lost the read:packages scope, or has lost access to the
MRISS-Projects organisation. Replace it under Settings -> Secrets and variables -> Actions with a
classic PAT holding read:packages. See the Secrets section of docs/devops/README.md.
exit=1
elapsed: 841 ms
```

### 9.3 The absent-secret branch still behaves as before

```text
$ PACKAGES_READ_TOKEN="" bash verify-credentials.sh
::error::PACKAGES_READ_TOKEN is unavailable to this run. Pull requests from forks do not receive
repository secrets; otherwise add it under Settings -> Secrets and variables -> Actions.
exit=1
```

### 9.4 The unreadable-`<parent>` guard fires

Run against a `pom.xml` with no `<parent>` block:

```text
$ PACKAGES_READ_TOKEN="anything" bash verify-credentials.sh
::error::Could not read the <parent> coordinates from pom.xml, so the credential cannot be checked.
Fix this step rather than skipping it: a malformed URL would make the check pass silently.
exit=1
```

### 9.5 What could not be verified locally, and why it is stated rather than claimed

**The `200` branch was not observed.** It needs a working credential, and this machine has none to
offer: reading the token out of the local `settings.xml` was denied by the agent's credential guard,
and `#99` §8 records that the local credential was itself answering `401` against this registry. So
the passing path rests on the registry answering `200` to an authenticated request for
artifact-level metadata — reasonable, and exactly what Maven relies on, but an assumption until a
runner executes it.

**The PR's own CI run settles it.** `ci.yml` runs on the pull request with the real secret. If the
assumption is wrong the step logs a `404` warning and the build still proceeds (§5.1), so a wrong
guess here degrades the check rather than blocking the PR — and the warning names the URL, which is
what makes it fixable in one commit. Watch for that warning on the first run and treat it as work
remaining, not as noise.

**`api-testing.yml` will not run on this PR.** Its `paths:` filter covers `dsh-rest-api/**` and
`specs/api/**`; this diff touches neither. Its copy of the step is verified by review against
`ci.yml`'s, which is identical text — and by §9.1-9.4, which exercise that text directly.

### 9.6 Branches reachable only with a working credential

`404`, `5xx` and a connection failure cannot be produced from this machine: by §4, every request
without a usable credential answers `401` whatever the path. Their handling is a `case` arm, driven
directly with forced status codes:

```text
200 -> PASS: PACKAGES_READ_TOKEN can read com.mriss.mriss-parent:products (parent 3.8.0-SNAPSHOT).
404 -> PASS with ::warning:: stale derived URL
500 -> PASS with ::warning:: could not verify (HTTP 500)
000 -> PASS with ::warning:: could not verify (HTTP 000)
```

### 9.7 Documentation

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

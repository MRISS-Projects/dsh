---
issue: 95
slug: read-only-ci-package-token
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 95 — Use a read-only token for CI package authentication

## 1. Story

**As a** repository owner
**I want** CI to authenticate to GitHub Packages with a read-only token
**So that** pull-request code cannot read a write-capable credential

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Issue: [#95](https://github.com/MRISS-Projects/dsh/issues/95)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the `0.3.0-SNAPSHOT` milestone on the
  issue, and follows `#92`, which shipped onto the same RC. `DEVELOP` is at `0.4.0-SNAPSHOT`.
- Raised by: Copilot review on `#91`

`ci.yml` and `api-testing.yml` both put `DEPLOY_TOKEN` in the job-wide `env:` block, so every step
can read it — including `mvn -B install`, which compiles and runs test code authored in the pull
request.

`DEPLOY_TOKEN` is write-capable: `documentation-sync.yml` hands it to `actions/checkout` so
`git-auto-commit-action` can push, and the four release wrappers pass it as a `secrets:` input to
the reusable workflows in `MRISS-Projects/parent-poms`, which deploy artifacts. Fork pull requests
receive no secrets, so exposure is limited to same-repository branches — but `ci.yml` runs on
*every* pull request with no path filter, a wider surface than `api-testing.yml`, which is scoped
to `dsh-rest-api/**` and `specs/api/**`.

**Step-scoping the variable is not the fix.** The generated `settings.xml` references
`${env.DEPLOY_TOKEN}`, which Maven interpolates at build time, so the build step genuinely needs
the value in its environment — and the build step is exactly where pull-request code runs. The fix
is a differently-scoped credential, not a differently-placed one.

## 3. Why `read:packages` alone is sufficient

Verified during step 3 rather than assumed. During `mvn install` the only authenticated traffic is
artifact resolution from `maven.pkg.github.com`. The `<github.personal.token>` property in the
generated `settings.xml` is consumed by `maven-changes-plugin`, which never runs in either CI
workflow:

| Use of `${github.personal.token}` in parent-poms | Reached by |
|---|---|
| `products/pom.xml:298` — `github-text-list`, bound to `generate-sources` | Only inside the `product-release-deployment` profile (`products/pom.xml:270-354`), which neither CI workflow activates. |
| `pom.xml:684` — `maven-changes-plugin` `<reporting>` | Only `mvn site`. Neither CI workflow runs it. |
| `pom.xml:455`, `pom.xml:928` | Release and announcement paths, out of both CI workflows. |

Both plugin configurations also set `<failOnError>false</failOnError>`, and `MRISS-Projects/dsh`
is public, so reading its issues needs no token scope at all. The property therefore stays in both
workflows, pointed at the new token: it costs nothing, and it keeps the generated `settings.xml`
structurally identical to the one parent-poms' own workflows generate.

**Authentication is still required even though `MRISS-Projects/maven-repo` is public.** The
GitHub Packages Maven registry demands credentials for anonymous-readable packages, which is why
the credential is swapped rather than dropped.

## 4. Prerequisite — already satisfied

`PACKAGES_READ_TOKEN` was created as a repository secret on **2026-09-16 20:18 UTC** and has not
been updated since. Confirmed via `gh api repos/MRISS-Projects/dsh/actions/secrets`:

    DEPLOY_TOKEN           2026-04-13T00:22:12Z
    PACKAGES_READ_TOKEN    2026-09-16T20:18:20Z

Repository secrets take precedence over organisation secrets of the same name, so nothing can
shadow it. Because the secret predates this branch, the first CI run on the pull request already
resolves it — there is no changeover window in which CI is red.

Two things this check does **not** establish, both of which belong to the repository owner:

1. **The token's scopes.** The Actions secrets API exposes a name and timestamps, never the value
   or its scopes. AC001 is verified from the PAT's page in account settings, or without revealing
   the value by reading the `x-oauth-scopes` response header on `GET /user`. The token must not
   appear in this repository, in a transcript, or in any workflow log.
2. **That it is a classic PAT.** It must be. Fine-grained PATs reach organisation-owned packages
   only when the organisation has opted in; the classic PAT with `read:packages` is the documented
   route for `maven.pkg.github.com`.

## 5. Files to change

### 5.1 `.github/workflows/ci.yml`

Five live references to `DEPLOY_TOKEN`, all replaced by `PACKAGES_READ_TOKEN`:

| Line | Current | Becomes |
|---|---|---|
| 35 | `DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}` | `PACKAGES_READ_TOKEN: ${{ secrets.PACKAGES_READ_TOKEN }}` |
| 61, 66, 71 | `<password>${env.DEPLOY_TOKEN}</password>` | `<password>${env.PACKAGES_READ_TOKEN}</password>` |
| 78 | `<github.personal.token>${env.DEPLOY_TOKEN}</github.personal.token>` | `<github.personal.token>${env.PACKAGES_READ_TOKEN}</github.personal.token>` |

The `env:` block also gains a comment block mirroring the one rewritten in `api-testing.yml`
(§5.2). Both files carry it — it does not move out of one into the other, or `api-testing.yml`'s
`env:` block would be left undocumented.

A preflight step is inserted **after** "Set up JDK 17" and **before** "Configure Maven settings for
GitHub Packages":

```yaml
      - name: Verify package credentials are present
        run: |
          if [ -z "$PACKAGES_READ_TOKEN" ]; then
            echo "::error::PACKAGES_READ_TOKEN is unavailable to this run. Pull requests from forks do not receive repository secrets; otherwise add it under Settings → Secrets and variables → Actions."
            exit 1
          fi
```

Without it, an absent or empty secret surfaces as a Maven 401 on `com.mriss.mriss-parent:products`
partway through dependency resolution, which reads as a broken parent rather than a missing
credential.

The message covers two distinct causes deliberately. Both workflows trigger on bare
`pull_request`, and GitHub withholds *all* repository secrets from fork pull requests — so the
guard fires there too, on a repository where the secret is correctly configured. Telling that
maintainer to "add it under Settings" would point away from the real cause. One message covering
both beats branching on `github.event.pull_request.head.repo.fork` for a five-line guard.

### 5.2 `.github/workflows/api-testing.yml`

The same five references, at lines 45, 73, 78, 83 and 90, and the same preflight step in the same
position.

Lines 40-44 additionally carry a comment block that is now actively wrong — it instructs the reader
to create `DEPLOY_TOKEN` as a classic PAT with the `read:packages` scope, which is the precise
conflation this story exists to end. It is rewritten to describe `PACKAGES_READ_TOKEN`.

It does **not** name `DEPLOY_TOKEN` while doing so. An earlier draft of this section asked for a
line saying `DEPLOY_TOKEN` is deliberately not used here, which AC002 forbids — it bars the name
from this file including in comments — and which AC003 would also break, since it confines the name
to five workflows. The acceptance criteria win. The comment states the rule without naming the
write-capable secret, and points at the `## Secrets` section of `docs/devops/README.md` (§5.3),
which names it and carries the full invariant:

```yaml
      # This workflow builds pull-request code, so the credential it puts in
      # the environment must not be able to write. See the Secrets section of
      # docs/devops/README.md for which token each workflow may use.
```

### 5.3 `docs/devops/README.md`

A new `## Secrets` section, placed after `## Workflow Reference` and before `## Parent POM`, so it
sits next to the table of what each workflow does. It records the invariant behind AC003 — which
token each workflow may use, and why the split exists — so that the next workflow added to this
repository reaches for the right one:

| Secret | Scope | Used by | Why |
|---|---|---|---|
| `PACKAGES_READ_TOKEN` | `read:packages` only | `ci.yml`, `api-testing.yml` | Resolving `com.mriss.mriss-parent:products` from the `MRISS-Projects/maven-repo` registry. Both workflows build pull-request code, so the credential they expose must not be able to write. |
| `DEPLOY_TOKEN` | write-capable | `documentation-sync.yml`, `stage.yml`, `staging.yml`, `release.yml`, `hotfix.yml` | Pushing auto-generated docs, and deploying artifacts via the reusable parent-poms workflows. None of these runs pull-request code on an automatic trigger. |

The section also states the rule plainly: **a workflow that builds pull-request code never receives
`DEPLOY_TOKEN`.**

It must also carry the manual-dispatch caveat, or the rule overstates itself. `documentation-sync.yml`
declares `workflow_dispatch` alongside its `push` trigger, and a dispatched run executes the
workflow from whichever ref the operator picks — so it can be aimed at an unmerged branch and will
check that branch out with `DEPLOY_TOKEN` in hand. That needs write access, so it is not open to a
pull-request author, but it makes the rule a statement about automatic triggers rather than an
absolute. AC006 requires the section to match `.github/workflows/` workflow for workflow, and an
unqualified "triggers on `push` only" would fail it.

`wiki-sync.yml` uses `secrets.GITHUB_TOKEN` and is unaffected; the section says so, so that a reader
comparing against `.github/workflows/` does not read the omission as an oversight.

## 6. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `.github/workflows/documentation-sync.yml` | Passes `DEPLOY_TOKEN` to `actions/checkout` so `git-auto-commit-action` can push past rulesets that block `GITHUB_TOKEN`. Genuinely needs write. Triggers on `push` and `workflow_dispatch` — never on `pull_request`. |
| `.github/workflows/release.yml`, `stage.yml`, `staging.yml`, `hotfix.yml` | `workflow_dispatch` only, started by a person, and they pass `DEPLOY_TOKEN` as a `secrets:` input to reusable workflows in `MRISS-Projects/parent-poms` that deploy artifacts. |
| `.github/workflows/wiki-sync.yml` | Uses `secrets.GITHUB_TOKEN`, not `DEPLOY_TOKEN`. |
| `.github/workflows/spec-validation.yml` | Uses no Maven and no package registry. |
| Every `pom.xml` | The credential is supplied entirely through `settings.xml`; no POM names a token. |

## 7. Residual risk, accepted

After this change, pull-request test code running in `ci.yml` can still read
`PACKAGES_READ_TOKEN` from its environment. That is inherent: Maven interpolates the value at build
time, in the same step that runs the tests.

What changes is the consequence. The token can read packages from a registry whose packages are
already public, and it can do nothing else — not push a commit, not publish a package, not touch
another repository. Recorded here so a future reader does not mistake the remaining exposure for an
oversight and re-open it.

## 8. Acceptance criteria

Numbered to match `#95`. AC005-AC008 cover the additions this spec makes to it — see §9.

- [ ] AC001: `PACKAGES_READ_TOKEN` is a classic PAT holding `read:packages` and no other scope,
  stored as a repository secret. Already in place per §4; the scope is confirmed by the repository
  owner against the PAT's settings page or the `x-oauth-scopes` header, not by the Actions API.
- [ ] AC002: `ci.yml` and `api-testing.yml` reference `PACKAGES_READ_TOKEN` and no longer reference
  `DEPLOY_TOKEN` anywhere, including in comments.
- [ ] AC003: `DEPLOY_TOKEN` still appears in exactly the five workflows listed in §6 —
  `documentation-sync.yml`, `stage.yml`, `staging.yml`, `release.yml`, `hotfix.yml` — and nowhere
  else under `.github/workflows/`.
- [ ] AC004: Both workflows still resolve `com.mriss.mriss-parent:products` from GitHub Packages
  and pass. Demonstrated by CI itself on the pull request.
- [ ] AC005: Both workflows fail with the explicit `::error::` message from §5.1 when the secret is
  absent, before Maven runs.
- [ ] AC006: `docs/devops/README.md` carries the `## Secrets` section from §5.3, and its contents
  match `.github/workflows/` workflow for workflow.
- [ ] AC007: Markdown lint passes over the changed documentation.
- [ ] AC008: CI is green on the pull request, and the coverage ratchet is unaffected — no
  production code is touched.

## 9. Issue body reconciliation — applied

The process treats the GitHub issue as the source of truth. This spec goes beyond `#95` as written
in three ways, and the issue had to be edited to match **before the pull request opens**.

**Done** — `#95` was reconciled on 2026-09-17, on the repository owner's instruction, with the four
points below. AC001 is now checked off there: the owner confirmed the PAT's scope against its own
settings page when they created it on 2026-09-16.

1. **AC001 is already satisfied.** The secret was created on 2026-09-16, after the issue was
   raised. The issue should record this, and should say that its scope is verified by the owner
   rather than by any check in the repository (§4).
2. **The preflight guard is new** — AC005 has no counterpart in the issue.
3. **The `## Secrets` section is new** — AC006 has no counterpart. The issue's AC003 states the
   invariant; this spec additionally writes it down where a workflow author will find it.
4. **`ci.yml` cannot be dispatched manually** — found while building, not during spec review. See
   §11 step 1. The issue records it so that AC005's evidence is not mistaken for a shortcut.

The issue's **Out of Scope** clause — rotating `DEPLOY_TOKEN`, and any change to the reusable
workflows in `MRISS-Projects/parent-poms` — is correct as written and carries over unchanged.

## 10. Out of scope

- **Rotating `DEPLOY_TOKEN`.** Worth doing, since the value had been readable by pull-request code
  on same-repository branches since `ci.yml` was introduced, and the secret dated from 2026-04-13.
  A credential operation with no change to this repository, so it does not belong in a task branch.
  **Done — the repository owner rotated it on 2026-09-17 and re-inserted the new value in both
  `MRISS-Projects/dsh` and `MRISS-Projects/parent-poms`.** No issue was needed. This change stops
  the exposure going forward; the rotation is what closes the window that was already open.
- **Step-scoping the `env:` block.** Analysed in §2 and rejected: it narrows nothing, because the
  step that needs the variable is the step that runs pull-request code.
- **Any change to `MRISS-Projects/parent-poms`.** Its reusable workflows deploy artifacts and
  legitimately need a write-capable token. Per `CLAUDE.md`, a change there is a separate round trip
  through that repository's own issue and release cycle.
- **`api-testing.yml`'s `mvn -B -U install`.** It uses `-U` where `ci.yml` deliberately does not, so
  the two workflows can resolve different parent SNAPSHOTs on the same commit. A real
  inconsistency, unrelated to credentials. **Raised as `#99`**, resolved *towards*
  `-U`, not away from it: while the parent is a `-SNAPSHOT` and this repository is the first
  consumer of `parent-poms` changes, tracking the current parent on every run is the intended
  contract. Omitting `-U` never bought reproducibility anyway — Maven refreshes SNAPSHOT metadata
  on its own daily schedule and `actions/setup-java` restores `~/.m2` from cache, so today's
  `ci.yml` already drifts, just unpredictably. That issue must also rewrite the comment above
  `ci.yml`'s build step and `## Parent POM` in `docs/devops/README.md`, both of which currently
  argue the opposite.

## 11. Testing approach

This story changes CI configuration and prose. It adds no production code and no behaviour that a
unit test can drive, so TDD does not apply and no test is added — the same posture as `#92`.

That does not leave it unverified. The guard in §5.1 is the testable unit, and it is driven
red-first through the one mechanism that actually executes a workflow.

**Order of verification:**

1. **Red for AC005, before the main change.** With the guard step added but the secret reference
   pointing at a name that does not exist, confirm the job fails at "Verify package credentials
   are present" with the `::error::` message — not later, at Maven. This proves the guard fires
   rather than merely being present.

   **`ci.yml` cannot be dispatched.** `workflow_dispatch` is only offered for workflows present on
   the default branch, and `ci.yml` is not yet on `master` — it arrived with `#91` and reaches
   `master` only at the next release. `api-testing.yml` *is* on `master` with a
   `workflow_dispatch` trigger, and carries a byte-identical guard step, so it is the workflow that
   was driven red. Redo this on `ci.yml` once it lands on `master`; until then the evidence
   transfers.

   Done on a throwaway branch (`tmp-95-guard-red`, pushed and deleted) so that no deliberate
   misname ever entered the task branch's history. Result —
   [run 35215797648](https://github.com/MRISS-Projects/dsh/actions/runs/35215797648):

   ```text
   success   Set up JDK 17
   failure   Verify package credentials are present
   skipped   Configure Maven settings for GitHub Packages
   skipped   Build entire project (all modules must be installed first)
   ```

   with the `::error::` annotation emitted before Maven ran, which is the point of the guard.

   **That run predates the message rewording** prompted by step 5 review — it emitted the earlier
   "is not set. Add it under Settings…" text. The guard's control flow is byte-identical since; only
   the string changed, and the new string is covered by the harness below. Re-dispatching for a
   wording change was not judged worth a second CI run, but the distinction is recorded rather than
   glossed.

   The guard is additionally driven red/green outside CI, by extracting the step's `run:` body from
   each workflow and executing it with the variable unset and set. That harness also asserts the
   step's position between "Set up JDK 17" and "Configure Maven settings", and performs the AC002
   and AC003 sweeps of step 3. It is verification scaffolding, not a committed test — this story
   adds no test, per the opening of this section.

2. **Green for AC002/AC004.** Push the branch; `ci.yml` runs on the pull request and must go green
   through `mvn -B install` and the coverage ratchet. This is the load-bearing check: it proves a
   `read:packages` token genuinely resolves the parent POM. `api-testing.yml` is path-filtered to
   `dsh-rest-api/**` and `specs/api/**`, so this pull request does **not** trigger it — it must be
   run once by `workflow_dispatch` and confirmed green before the pull request merges, or AC004 is
   only half demonstrated.

3. **Sweep for AC002 and AC003**, over tracked files only, so that untracked working directories
   cannot mask a result:

   ```bash
   git ls-files '.github/workflows/*' | while read -r f; do
     grep -Hn 'DEPLOY_TOKEN' "$f"
   done
   ```

   Expected: hits in `documentation-sync.yml`, `stage.yml`, `staging.yml`, `release.yml` and
   `hotfix.yml` only. Any hit in `ci.yml` or `api-testing.yml` fails AC002; any hit in a sixth
   workflow fails AC003.

4. **Local build unaffected.** `mvn -B install`, logged to `.logs/mvn-install.log` per `CLAUDE.md`
   with the exit code reported explicitly. It reads the developer's own `~/.m2/settings.xml`, so it
   cannot exercise the workflow credential — it is run to confirm this change breaks nothing
   locally, not as evidence for AC004.

5. **Markdown lint** for AC007, using the command in `CLAUDE.md`.

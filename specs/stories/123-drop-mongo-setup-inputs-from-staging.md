---
issue: 123
slug: drop-mongo-setup-inputs-from-staging
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 123 — Stop passing the Mongo setup inputs to `project-staging.yml`

## 1. Story

**As a** DSH maintainer dispatching a staging build
**I want** `staging.yml` to stop passing the Mongo setup inputs that parent-poms#78 removes
**So that** staging keeps running once `project-staging.yml` drops them, instead of failing before
any job starts

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#123](https://github.com/MRISS-Projects/dsh/issues/123)
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`. It matches the milestone, and `#117` and `#112`,
  both Wave 0, merged there. `DEVELOP` is at `0.4.0-SNAPSHOT`.
- Upstream half: [`parent-poms#78`](https://github.com/MRISS-Projects/parent-poms/issues/78). Its
  spec is `parent-poms/specs/78-remove-consumer-services-from-staging.md`, and its §5 describes this
  story. That branch, `issue-78-remove-consumer-services-from-staging`, has Tasks 1-5 done and
  pushed; its `build.yml` run [36236972450](https://github.com/MRISS-Projects/parent-poms/actions/runs/36236972450)
  is green.
- Twin pattern: `#117`/`parent-poms#65`, `#114`/`parent-poms#76`. `staging.yml` references
  parent-poms at `@master` and this change touches no POM, so no parent-poms release or re-pin is
  involved.

### 2.1 Why DSH needs no services — measured, not assumed

parent-poms#78 §1.2 records the measurement. On 2026-09-25, with nothing listening on 27017 or
5672, `mvn -B clean install -DintegrationTests` with this wrapper's four `mongo.*` values ended in
**BUILD SUCCESS**. All six ITs passed, each starting a Spring context, and `jacoco:check` was met in
every module. The `dsh-rest-api` ITs `@MockBean` the DAO and the queue service. The worker contexts
never open a connection: Spring AMQP connects lazily, and the Mongo driver's background monitor only
logs a failure.

### 2.2 The ordering is forced, and runs the other way from `#117`

A caller that passes an input the called workflow does not declare fails before any job starts.
`#117` had to wait for its input to exist. This story removes inputs, so it has to merge **before**
parent-poms#78. Until then, parent-poms `master` still declares the three inputs.

Dropping them is safe against `master` as it stands. The "Create MongoDB user and database" step is
guarded by `if: ${{ inputs.mongo_user != '' }}`, so it skips. The service containers still start,
idle, and nothing uses the user that is no longer created.

## 3. The change

`.github/workflows/staging.yml:24-30` at `aad71bfb8` passes `mongo_user: dshuser`,
`mongo_password: dshpass` and `mongo_database: dsh`, under a comment explaining they feed only the
upstream setup step. Once parent-poms#78 merges they are undeclared, and every staging dispatch
fails.

The comment above `maven_properties` (lines 16-18) says "a live MongoDB runs behind these values
here, unlike on the release path". After parent-poms#78 that is false.

## 4. Design

Delete lines 24-30, the comment and the three inputs. Keep `maven_properties` and its four values
unchanged. `dsh-data`'s `mongo.properties` is filtered from them, and without them every
`dsh-rest-api` context fails on an unresolved placeholder (`#114`), whether or not anything
connects. Rewrite the comment above it:

```yaml
      # #114: the build's Mongo settings, which dsh-data's mongo.properties is filtered from.
      # Without them every dsh-rest-api context fails on an unresolved placeholder. No MongoDB
      # runs behind them: parent-poms#78 removed the service containers, and no test connects.
      maven_properties: |
```

### 4.1 Explicitly not in scope

- **`ci.yml` and `api-testing.yml`,** which start their own `mongo:6` and `rabbitmq`. They are
  DSH-owned and untouched by parent-poms#78. `ci.yml` runs unit tests only since `#112`, so its
  services are idle too, but whether to drop them is a separate decision.
- **Starting infrastructure from DSH's tests** (e.g. Testcontainers). No test needs it today.
- **`specs/product/PRD.md`.** Its line saying `staging.yml` "names `dshuser` and `dshpass` twice"
  goes stale when this merges. Step 8 (`dsh-reconcile-prd`) corrects it, and places `#123` in the
  Wave 0 table.

## 5. Files to change

### 5.1 `.github/workflows/staging.yml`

§4: delete the three inputs and their comment, and rewrite the `maven_properties` comment.

### 5.2 `docs/devops/README.md`

The table in "How build properties reach a release build" (line 127) says `staging.yml` has a live
MongoDB, "yes, service container". Change that cell to "no — nothing connects on this path",
matching the `release.yml` row. It becomes true when parent-poms#78 merges. The pre-merge run in
§7 is what proves it, since that run calls the branch where the containers are already gone.

## 6. Tasks

- [x] **Task 1 — the wrapper, pointed at the upstream branch.** Apply §5.1, and temporarily change
      `project-staging.yml@master` to `@issue-78-remove-consumer-services-from-staging`. Commit.
- [x] **Task 2 — the docs.** Apply §5.2. Commit.
- [ ] **Task 3 — dispatch staging against this branch.** Run §7. This run is also
      parent-poms#78's Task 6: record it in both specs and comment it on both issues.
- [ ] **Task 4 — pin back to `@master`.** Revert the ref change from Task 1, then confirm with
      `grep -n 'project-staging.yml@' .github/workflows/staging.yml`. Expected: exactly one line,
      ending `@master`. Commit. This is AC004, and it must land before the PR is merged.

## 7. Verification

The only test that reaches this change is a staging run, which cannot run locally.

```bash
gh workflow run staging.yml --ref issue-123-drop-mongo-setup-inputs-from-staging \
  -f branch_name=issue-123-drop-mongo-setup-inputs-from-staging
```

`branch_name` is what `project-staging.yml` checks out and builds. Pointing it at this task branch
builds this branch's code, not the RC's, and it is the same code apart from the wrapper.

**Side effect, known and accepted.** A staging run deploys `rcs` artifacts and the staging site,
and commits a regenerated `README.md` to `branch_name`. With `branch_name` set to this task branch,
that commit lands on the task branch, as it did in PR #107's run. Pull before Task 4.

From the run, record:

1. The run URL, and the conclusion `success`.
2. **No "Initialize containers" section** in the job log. That is parent-poms#78's AC005.
3. The six IT lines, `Tests run: … in com.mriss.dsh.….integration.…IT`, with no failures.
4. `All coverage checks have been met.` for every module with production sources.

AC001 is checked by reading the file, not the run: a reusable workflow's log does not reliably list
the inputs it received.

## 8. Acceptance criteria

- [ ] **AC001** — `.github/workflows/staging.yml` passes no `mongo_user`, `mongo_password` or
      `mongo_database` input.
- [ ] **AC002** — `staging.yml` still passes `maven_properties` with `mongo.host`, `mongo.port`,
      `mongo.user` and `mongo.password`. No comment in the file claims a live MongoDB or describes
      the removed inputs.
- [ ] **AC003** — The §7 run, calling `project-staging.yml` at parent-poms#78's branch, concludes
      `success`: all six ITs pass, and the job log has no "Initialize containers" section.
- [ ] **AC004** — At merge, `staging.yml` calls `project-staging.yml` at `@master`, not at a task
      branch.

## 9. Merge order

1. This PR merges into `staging-0.3.0-SNAPSHOT-RC`, pinned to `@master` (AC004). Staging keeps
   working: `master` still declares the inputs, and the setup step skips.
2. parent-poms#78 merges.
3. parent-poms#78's Task 7 dispatches `staging.yml` on the RC branch against `@master` and confirms
   the end state.

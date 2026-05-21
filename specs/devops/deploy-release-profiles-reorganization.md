# Deploy / Release Profiles Reorganization — Feature Specification

## Overview and Purpose

This specification drives the **unification and reorganization of Maven deploy/release profiles**
across both the `parent-poms` and `dsh` repositories. It implements two linked user stories:

- **[parent-poms#56](https://github.com/MRISS-Projects/parent-poms/issues/56)** — Unify/reorganize
  deployment profiles in the `parent-poms` infrastructure project.
- **[dsh#84](https://github.com/MRISS-Projects/dsh/issues/84)** — Unify/reorg of deploy/release
  profiles in DSH, consuming the new profile structure from `parent-poms`.

Today both repositories activate Maven profiles with `-P` flags. This causes known Maven
interpolation/merge bugs when profiles are defined at multiple levels of a POM hierarchy
(`parent-poms/pom.xml` → `parent-poms/products/pom.xml` → `dsh/pom.xml` → module POMs).
The goal is to replace `-P` with `-D` property-based activation, establish a clear three-tier
profile hierarchy, eliminate duplication, and keep all existing CI/CD pipelines (GitHub Actions
workflows and Jenkinsfiles) fully operational.

All changes to `parent-poms` must be validated first (FR001–FR010); DSH changes follow (FR011–FR014).

---

## Scope

| Repository | Changes |
|------------|---------|
| `MRISS-Projects/parent-poms` | Profile restructuring, `-D` activation, `pluginManagement`, workflow adaptation |
| `MRISS-Projects/dsh` | Parent reference SNAPSHOT update, profile alignment, workflow adaptation, validation |

---

## Profile Hierarchy Design

After this refactoring, the three deployment profiles must follow this strict hierarchy:

```
deployment                     (generic — present at parent-poms/pom.xml and inherited everywhere)
  └── release-deployment        (release-specific — non-SNAPSHOT goals only)
        └── product-release-deployment  (product-specific — starts at parent-poms/products/pom.xml)
```

**Activation convention** (replaces `-P`):

| Profile | Activation flag |
|---------|----------------|
| `deployment` | `-Ddeployment` |
| `release-deployment` | `-Drelease-deployment` |
| `product-release-deployment` | `-Dproduct-release-deployment` |

---

## Functional Requirements

### FR001 — Reorganize `deployment` Profile Content (parent-poms)

- **Description**: The `deployment` profile in `parent-poms/pom.xml` must be the single location
  for all generic, non-release-specific goals applicable at every hierarchy level. It must be
  activated by the presence of the `-Ddeployment` property.
- **Inputs**: `parent-poms/pom.xml`, all sub-module `pom.xml` files
- **Actions**:
  - Move all generic artifact-deployment, README generation/commit, and build-number goals into
    `deployment`.
  - Ensure the profile activates via `<activation><property><name>deployment</name></property></activation>`
    rather than any `-P` flag.
  - Remove any duplicate `deployment` profile declarations from sub-modules
    (`infrastructure/`, `infrastructure/maven-archetypes/`, etc.) unless a project-specific
    override is explicitly required and commented.
- **Acceptance Criteria**:
  - `mvn help:effective-pom -Ddeployment` shows exactly one consolidated `deployment` profile
    active at every hierarchy level.
  - No `<profile><id>deployment</id>` appears in any sub-module POM unless accompanied by a
    comment explaining the DSH-specific override.

---

### FR002 — Reorganize `release-deployment` Profile Content (parent-poms)

- **Description**: The `release-deployment` profile must contain **only** goals that are
  meaningful for official (non-SNAPSHOT) releases. It must be activated by `-Drelease-deployment`.
- **Actions**:
  - Move goals specific to official releases (e.g., changelog generation, release site publishing
    beyond gh-pages snapshot) exclusively here.
  - Remove redundant `release-deployment` profile definitions from sub-module POMs.
- **Acceptance Criteria**:
  - `mvn help:effective-pom -Drelease-deployment` shows no duplicate plugin executions.
  - Running `mvn -Ddeployment deploy` (snapshot context) does **not** trigger any
    `release-deployment`-only goal.

---

### FR003 — Reorganize `product-release-deployment` Profile Content (parent-poms)

- **Description**: The `product-release-deployment` profile must contain **only** goals that apply
  to **product** releases. It must first be declared at `parent-poms/products/pom.xml` (not at
  the root) and activated by `-Dproduct-release-deployment`.
- **Actions**:
  - Move product-level release goals (e.g., README artifact attachment) into this profile.
  - Ensure the profile is absent from `parent-poms/pom.xml` and `parent-poms/infrastructure/`.
- **Acceptance Criteria**:
  - `grep -r "product-release-deployment" parent-poms/pom.xml parent-poms/infrastructure/` returns
    no results.
  - `mvn -f parent-poms/products/pom.xml help:effective-pom -Dproduct-release-deployment` shows
    the profile active with the expected goals.

---

### FR004 — Replace `-P` with `-D` Property Activation in `parent-poms` Workflows and Jenkinsfiles

- **Description**: All occurrences of `-P deployment`, `-P deployment,release-deployment`, and
  `-P product-release-deployment` in GitHub Actions workflows and Jenkinsfiles must be replaced
  with the equivalent `-Ddeployment`, `-Drelease-deployment`, `-Dproduct-release-deployment`
  flags.
- **Files to update**:

  | File | Current usage | Replacement |
  |------|---------------|-------------|
  | `.github/workflows/build.yml` | `-P deployment` | `-Ddeployment` |
  | `.github/workflows/build.yml` | `-P deployment,release-deployment` | `-Ddeployment -Drelease-deployment` |
  | `.github/workflows/deploy.yml` | `-P deployment` | `-Ddeployment` |
  | `.github/workflows/deploy.yml` | `-P deployment,release-deployment` | `-Ddeployment -Drelease-deployment` |
  | `ReleaseJenkinsfile` | any `-P` usage | equivalent `-D` flags |
  | `ProjectReleaseJenkinsfile` | any `-P` usage | equivalent `-D` flags |
  | `ProjectStageJenkinsfile` | any `-P` usage | equivalent `-D` flags |
  | `ProjectStagingJenkinsfile` | any `-P` usage | equivalent `-D` flags |
  | `ProjectHotfixJenkinsfile` | any `-P` usage | equivalent `-D` flags |

- **Acceptance Criteria**:
  - `grep -r "\-P " .github/workflows/ Jenkinsfile* Project*Jenkinsfile` returns no results in
    `parent-poms`.
  - After the change, `build.yml` workflow passes on a push to any branch.

---

### FR005 — Fix Maven Release Plugin Default Profiles (parent-poms)

- **Description**: The `maven-release-plugin` `<arguments>` configuration (which lists profiles
  to activate during release) must match the hierarchy level:
  - Root (`parent-poms/pom.xml`): `deployment,release-deployment` (as `-D` flags)
  - Products level and below (`parent-poms/products/pom.xml`):
    `deployment,release-deployment,product-release-deployment` (as `-D` flags)
- **Acceptance Criteria**:
  - `mvn help:effective-pom -f parent-poms/pom.xml | grep "release-deployment"` shows
    `deployment,release-deployment` in the release plugin arguments.
  - `mvn help:effective-pom -f parent-poms/products/pom.xml | grep "release-deployment"` shows
    `deployment,release-deployment,product-release-deployment`.

---

### FR006 — Scope `maven-scm-publish-plugin` Correctly (parent-poms)

- **Description**: `maven-scm-publish-plugin` must be configured **only** at the root of
  `parent-poms` and used exclusively for publishing the Maven site to GitHub `gh-pages`. It must
  **not** be used for artifact deployment.
- **Actions**:
  - Remove any `maven-scm-publish-plugin` declarations from sub-module POMs within `parent-poms`.
  - Verify the root-level configuration targets the `gh-pages` branch only.
- **Acceptance Criteria**:
  - `grep -r "scm-publish" parent-poms/ --include="pom.xml"` returns exactly one result:
    `parent-poms/pom.xml`.

---

### FR007 — Remove `distributionManagement` from Sub-Profile Blocks (parent-poms)

- **Description**: `<distributionManagement>` entries (`<repository>` and `<snapshotRepository>`)
  inside profile blocks must **only** exist in `parent-poms/pom.xml`. All other `pom.xml` files
  in the `parent-poms` hierarchy must not repeat them inside `<profiles>`.
- **Acceptance Criteria**:
  - `grep -r "snapshotRepository\|<repository>" parent-poms/ --include="pom.xml"` returns results
    only from `parent-poms/pom.xml`.

---

### FR008 — README Handling Consolidation (parent-poms)

- **Description**:
  - All custom README generation and SCM-commit steps must be consolidated in the `deployment`
    profile.
  - README artifact attachment (attaching README as a build artifact) must exist **only** in
    the `product-release-deployment` profile, starting at `parent-poms/products/pom.xml`.
- **Acceptance Criteria**:
  - Running `-Ddeployment` on any module generates and commits the README without error.
  - README artifact attachment does not occur for modules above `products` level.

---

### FR009 — Adopt `markdowndoc-maven-plugin` for PDF Generation (parent-poms)

- **Description**: PDF documentation generation must use:
  ```xml
  <groupId>se.natusoft.tools.doc.markdowndoc</groupId>
  <artifactId>markdowndoc-maven-plugin</artifactId>
  <version>2.1.4</version>
  ```
  The `<version>` and base `<configuration>` must be declared in `<pluginManagement>` at
  `parent-poms/pom.xml`. Individual modules reference this plugin without repeating the version.
- **Acceptance Criteria**:
  - `grep -r "markdowndoc-maven-plugin" parent-poms/ --include="pom.xml"` shows the version
    declared only in `parent-poms/pom.xml` `pluginManagement`.
  - PDF generation executes successfully when `-Ddeployment` is active.

---

### FR010 — Validate parent-poms GitHub Actions Workflows (parent-poms)

- **Description**: Both existing GitHub Actions workflows in `parent-poms` must remain fully
  operational after all profile refactoring changes. They are the primary validation gate for
  this feature in `parent-poms`.
- **Workflow validation requirements**:

  | Workflow | File | Trigger | Required outcome |
  |----------|------|---------|-----------------|
  | Build | `.github/workflows/build.yml` | Push to any branch | Must pass all steps (build, deployment-profile build, site generation) |
  | Deploy | `.github/workflows/deploy.yml` | Manual — `release_type=snapshots` | Must pass all steps (build, snapshot artifact deploy, snapshot site deploy) |

- **Acceptance Criteria**:
  - `build.yml` workflow passes successfully with the new `-D` profile flags.
  - `deploy.yml` workflow passes successfully when triggered with `release_type=snapshots`.
  - No `-P` flag remains anywhere in either workflow file after migration.

---

### FR011 — Update DSH Root `pom.xml` Parent Reference to `products` SNAPSHOT (dsh)

- **Description**: The `<parent>` block in the DSH root `pom.xml` must reference
  `parent-poms/products/pom.xml` at its **current SNAPSHOT version** so that DSH immediately
  inherits all refactored profile definitions from `parent-poms` once they are committed.
- **Current state**:
  ```xml
  <parent>
      <groupId>com.mriss.mriss-parent</groupId>
      <artifactId>products</artifactId>
      <version>3.8.0-SNAPSHOT</version>
  </parent>
  ```
- **Required state at implementation time**: The `<version>` must match whatever SNAPSHOT is
  active in `parent-poms/products/pom.xml` at the moment of implementation. As of this writing,
  this is `3.8.0-SNAPSHOT`. If `parent-poms` advances its version before DSH changes are applied,
  the DSH reference must be updated accordingly.
- **Sequencing**: This step must be completed **before** any profile or workflow changes in DSH,
  so that `mvn -f pom.xml validate` uses the new parent definitions throughout development.
- **Acceptance Criteria**:
  - `grep -A4 "<parent>" dsh/pom.xml` shows `<artifactId>products</artifactId>` with the current
    `parent-poms/products` SNAPSHOT version.
  - `mvn -f dsh/pom.xml validate` passes after a local `mvn install` of the `parent-poms` SNAPSHOT.

---

### FR012 — Align DSH Profiles with Reorganized parent-poms Structure (dsh)

- **Description**: After `parent-poms` completes FR001–FR010, DSH must be updated to align with
  the new profile structure:
  - Remove any `deployment`, `release-deployment`, or `product-release-deployment` profile blocks
    from DSH `pom.xml` files that are now fully inherited from `parent-poms` without DSH-specific
    overrides.
  - Any DSH-specific profile extension must be documented with a comment explaining why it cannot
    be placed in `parent-poms`.
- **Acceptance Criteria**:
  - `mvn -Ddeployment help:effective-pom -f dsh/pom.xml` shows no duplicate plugin executions.
  - No profile in any DSH POM duplicates a parent-level profile without a justifying comment.

---

### FR013 — Replace `-P` with `-D` in DSH Workflows and Jenkinsfiles (dsh)

- **Description**: All occurrences of `-P` profile activation in DSH GitHub Actions workflows and
  Jenkinsfiles must be replaced with `-D` property flags, consistent with the convention
  established in FR004 for `parent-poms`.
- **Files to update**:

  | File | Current usage | Replacement |
  |------|---------------|-------------|
  | `build-ci.sh` | any `-P` usage | equivalent `-D` flags |
  | `build-ci-release.sh` | any `-P` usage | equivalent `-D` flags |
  | `build-ci-stage.sh` | any `-P` usage | equivalent `-D` flags |
  | `build-ci-staging.sh` | any `-P` usage | equivalent `-D` flags |
  | `deploy.sh` | any `-P` usage | equivalent `-D` flags |
  | Any DSH Jenkinsfiles | any `-P` usage | equivalent `-D` flags |

- **Acceptance Criteria**:
  - `grep -r "\-P " dsh/ --include="*.sh" --include="*Jenkinsfile"` returns no results.
  - `mvn help:effective-pom -Ddeployment -f dsh/pom.xml` reflects the expected merged profile.

---

### FR014 — Validate DSH Changes with GitHub Actions (dsh)

- **Description**: After all DSH changes are applied (FR011–FR013), the following two GitHub
  Actions workflows must pass successfully. They form the **mandatory acceptance gate** for the
  DSH side of this feature.
- **Workflow validation requirements**:

  | Workflow | File | Trigger | Required outcome |
  |----------|------|---------|-----------------|
  | API Testing | `.github/workflows/api-testing.yml` | Manual / push to `develop` or `main` | All Postman collection tests pass; REST API starts and responds correctly |
  | Spec Validation | `.github/workflows/spec-validation.yml` | Push / PR touching `specs/**` | OpenAPI spec lints clean; Markdown files lint clean; all spec references resolve |

- **Acceptance Criteria**:
  - `api-testing.yml` workflow completes with all Newman Postman tests passing (or skipped if no
    collections exist yet), with no build failures.
  - `spec-validation.yml` workflow completes with all three jobs (`validate-openapi`,
    `validate-markdown`, `check-spec-references`) passing.
  - This spec file (`specs/devops/deploy-release-profiles-reorganization.md`) is included in the
    `spec-validation` run and referenced correctly from `.github/copilot-instructions.md`.

---

## Technical Implementation Notes

### Sequencing (Implementation Order)

1. **Step 1 — parent-poms profile restructuring** (FR001–FR003):
   - Consolidate `deployment`, `release-deployment`, `product-release-deployment` profiles.
   - Switch activation to `-D` properties throughout `parent-poms` POMs.
   - Remove redundant profile declarations from sub-modules.

2. **Step 2 — parent-poms `pluginManagement` and helpers** (FR006–FR009):
   - Centralize `markdowndoc-maven-plugin` version.
   - Clean up `maven-scm-publish-plugin` placement.
   - Fix `distributionManagement` profile blocks.
   - Consolidate README handling.

3. **Step 3 — parent-poms Maven release plugin defaults** (FR005):
   - Set correct default profiles in release plugin at root and `products` level.

4. **Step 4 — parent-poms workflow and Jenkinsfile migration** (FR004):
   - Replace all `-P` flags in CI/CD files.

5. **Step 5 — parent-poms validation** (FR010):
   - Run `build.yml` (push trigger).
   - Manually trigger `deploy.yml` with `release_type=snapshots`.
   - Both must pass before proceeding.

6. **Step 6 — DSH parent reference update** (FR011):
   - Update DSH `pom.xml` `<parent>` version to current `parent-poms/products` SNAPSHOT.
   - Run `mvn -f dsh/pom.xml validate` locally.

7. **Step 7 — DSH profile alignment** (FR012):
   - Remove DSH profiles now fully covered by `parent-poms`.

8. **Step 8 — DSH workflow and script migration** (FR013):
   - Replace all `-P` flags in DSH CI/CD shells and Jenkinsfiles.

9. **Step 9 — DSH validation** (FR014):
   - Manually trigger `api-testing.yml`.
   - Push spec changes to trigger `spec-validation.yml`.
   - Both must pass.

### Data Flow After Refactoring

```
parent-poms/pom.xml
│  profiles: deployment (-Ddeployment), release-deployment (-Drelease-deployment)
│  pluginManagement: markdowndoc, scm-publish, release plugin (root defaults)
│  distributionManagement: defined here only
│
├── parent-poms/infrastructure/pom.xml
│     (inherits deployment, release-deployment — no local override unless justified)
│
└── parent-poms/products/pom.xml
      profiles: product-release-deployment (-Dproduct-release-deployment)
      release plugin: deployment,release-deployment,product-release-deployment defaults
      │
      └── dsh/pom.xml  (parent version = products SNAPSHOT)
            profiles: DSH-specific only (no duplication of inherited profiles)
            │
            └── dsh/dsh-rest-api/pom.xml
            └── dsh/dsh-data/pom.xml
            └── dsh/dsh-doc-analyser/pom.xml
            └── ...
```

---

## Testing Requirements

### Unit / Structural Tests

- `mvn help:effective-pom -Ddeployment` at each hierarchy level must show no duplicate plugin
  executions and the expected consolidated profile content.
- `mvn help:effective-pom -Drelease-deployment` must not include `product-release-deployment`
  goals unless `-Dproduct-release-deployment` is also passed.

### Integration Tests

- `mvn -f parent-poms/pom.xml install -DskipTests` must succeed.
- `mvn -f dsh/pom.xml verify` must succeed with the new parent reference.
- `mvn -f dsh/pom.xml site -DskipTests -Ddeployment` must generate site without errors.

### CI/CD Acceptance Gates

| Gate | Repo | Workflow / Command | Pass Condition |
|------|------|--------------------|---------------|
| Build | `parent-poms` | `build.yml` (push) | All steps green |
| Deploy snapshots | `parent-poms` | `deploy.yml` (`release_type=snapshots`) | All steps green |
| API Testing | `dsh` | `api-testing.yml` (manual) | All Postman tests pass |
| Spec Validation | `dsh` | `spec-validation.yml` (push) | All 3 jobs green |

---

## References

- User story (parent-poms): https://github.com/MRISS-Projects/parent-poms/issues/56
- User story (dsh): https://github.com/MRISS-Projects/dsh/issues/84
- Related spec: `specs/features/pom-hierarchy-migration.md`
- Parent root POM: `D:/IdeaProjects/parent-poms/pom.xml`
- Products POM: `D:/IdeaProjects/parent-poms/products/pom.xml`
- DSH root POM: `D:/IdeaProjects/dsh/pom.xml`
- GitHub Actions (parent-poms build): `.github/workflows/build.yml` (MRISS-Projects/parent-poms)
- GitHub Actions (parent-poms deploy): `.github/workflows/deploy.yml` (MRISS-Projects/parent-poms)
- GitHub Actions (dsh api-testing): `.github/workflows/api-testing.yml` (MRISS-Projects/dsh)
- GitHub Actions (dsh spec-validation): `.github/workflows/spec-validation.yml` (MRISS-Projects/dsh)
- Java conventions: `.github/copilot/rules/java-conventions.md`
- Architecture overview: `specs/architecture/system-design.md`


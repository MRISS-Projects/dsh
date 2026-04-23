# POM Hierarchy Migration Feature Specification

## Overview and Purpose

As a Product Manager (see `/.github/roles.md`), this specification drives the migration of the DSH
project's Maven build configuration so that it inherits as much as possible from the `parent-poms`
hierarchy (`parent-poms/pom.xml` → `parent-poms/products/pom.xml`).

Currently, DSH module POMs duplicate plugin versions, re-declare skin and compiler properties,
embed Jenkins-specific artifacts, manage dependency versions locally, and use the deprecated
`.apt` site format. The goal is to eliminate that duplication, centralize generic configuration
in the parent hierarchy, and keep only DSH-specific customizations in DSH POMs.

This feature is governed by **ADR-002** (`specs/architecture/ADR-002-pom-hierarchy-configuration.md`).

---

## Functional Requirements

### FR001 – Update DSH Parent Reference to `products` SNAPSHOT

- **Description**: The `<parent>` section of the root DSH `pom.xml` must reference
  `parent-poms/products/pom.xml` at its current SNAPSHOT version so that all parent changes
  are immediately testable against DSH.
- **Input**: Root `D:/IdeaProjects/dsh/pom.xml`
- **Output**: `<parent>` block updated; `mvn validate` passes
- **Acceptance Criteria**:
  - `<groupId>com.mriss.mriss-parent</groupId>` / `<artifactId>products</artifactId>` /
    `<version>3.7.1-SNAPSHOT</version>` in the DSH root `<parent>` block.
  - `mvn -f dsh/pom.xml validate` succeeds after local installation of parent-poms SNAPSHOT.

---

### FR002 – Remove Duplicate Maven Profiles from DSH

- **Description**: Profiles in DSH POM files that are already defined in a parent POM and provide
  no DSH-specific configuration must be removed.
- **Input**: All `pom.xml` files in `D:/IdeaProjects/dsh/`
- **Output**: No redundant profile `<id>` values; remaining profiles are documented with a comment
  explaining why they are DSH-specific.
- **Profile Map**:

  | Profile ID                   | Present in DSH root `pom.xml` | Present in parent              | Action                                                 |
  |------------------------------|-------------------------------|--------------------------------|--------------------------------------------------------|
  | `deployment`                 | Yes                           | `parent-poms/pom.xml`          | **Remove from DSH** – generic; fully covered by parent |
  | `product-release-deployment` | Yes                           | `parent-poms/products/pom.xml` | **Merge DSH-specific parts up**, then remove from DSH  |
  | `update-readme`              | Yes                           | No                             | **Keep** – DSH-specific SCM checkin customisation      |
  | `release-deployment`         | Yes (implicit via parent)     | `parent-poms/pom.xml`          | **Remove from DSH**                                    |

- **Acceptance Criteria**:
  - `mvn -P deployment validate` succeeds, picking up the parent profile.
  - No profile `<id>` appears in both a DSH POM and an ancestor POM without a justifying comment.

---

### FR003 – Remove Plugin Versions from DSH; Centralize in Root Parent `pluginManagement`

- **Description**: All `<version>` elements on `<plugin>` declarations in DSH POMs must be removed.
  The corresponding versions must be present in `parent-poms/pom.xml` `<pluginManagement>`.
- **Input**: All DSH `pom.xml` files; `D:/IdeaProjects/parent-poms/pom.xml`
- **Output**: No hardcoded plugin versions in DSH; all plugin versions resolved from parent
  `pluginManagement`.
- **Plugins requiring version centralization in DSH today**:

  | Plugin                     | Current DSH Version      | Action                                                   |
  |----------------------------|--------------------------|----------------------------------------------------------|
  | `buildnumber-maven-plugin` | `1.0` (dsh-rest-api)     | Remove; use `${buildnumber.plugin.version}` from parent  |
  | `spring-boot-maven-plugin` | `${spring.boot.version}` | Remove; managed in `products/pom.xml` pluginManagement   |
  | `lifecycle-mapping` (m2e)  | `1.0.0`                  | Remove version; keep Eclipse m2e block as-is             |
  | `jacoco-maven-plugin`      | (DSH root)               | Remove; use `${jacoco.maven.plugin.version}` from parent |
  | `maven-enforcer-plugin`    | (DSH root)               | Remove; use `${enforcer.plugin.version}` from parent     |
  | `maven-release-plugin`     | (DSH root)               | Remove; use `${release.plugin.version}` from parent      |
  | `maven-site-plugin`        | (DSH root)               | Remove; use `${site.plugin.version}` from parent         |

- **Acceptance Criteria**:
  - `grep -r "<version>" dsh/ --include="pom.xml"` returns no plugin version lines
    (project/parent/dependency versions are still allowed).
  - `mvn -f dsh/pom.xml package -DskipTests` builds successfully.

---

### FR004 – Move Generic Plugin Configuration Up the Hierarchy

- **Description**: Plugin configuration in DSH that is not DSH-specific must be relocated to
  `parent-poms/pom.xml` or `parent-poms/products/pom.xml`.
- **Generic blocks to move up**:
  - `maven-resources-plugin` execution `copy-site-resources-for-pdf` → `products/pom.xml`
    `<pluginManagement>` (already pattern-matched in `deployment` profile there).
  - `maven-site-plugin` `<siteDirectory>src/site-desc</siteDirectory>` default → `products/pom.xml`
    `<pluginManagement>` if applicable to all products; otherwise DSH-only.
  - `maven-release-plugin` `<scmCommentPrefix>`, `<preparationGoals>`, `<goals>` →
    already in `parent-poms/pom.xml`; remove DSH duplicates.
  - `maven-changes-plugin` GitHub issue list execution inside `deployment` profile →
    already in `parent-poms/pom.xml` `deployment` profile; remove DSH duplicate.
  - `buildnumber-maven-plugin` timestamp execution inside `deployment` profile →
    already in `parent-poms/pom.xml` `deployment` profile; remove DSH duplicate.
- **Acceptance Criteria**:
  - No plugin configuration block in any DSH POM is identical (or functionally identical) to one
    in a parent POM.
  - `mvn -f dsh/pom.xml site -DskipTests` generates site without errors.

---

### FR005 – Bump Spring Boot Version; Align Spring Fox and Swagger Annotations

- **Description**: Centralize framework versions in `products/pom.xml` and remove them from DSH.
- **Version Table**:

  | Artefact              | Current `products/pom.xml`              | Current DSH                              | Target `products/pom.xml`                         |
  |-----------------------|-----------------------------------------|------------------------------------------|---------------------------------------------------|
  | Spring Boot           | `2.3.4.RELEASE` (`spring-boot.version`) | `2.3.12.RELEASE` (`spring.boot.version`) | `2.3.12.RELEASE`                                  |
  | Spring Fox Swagger UI | `2.10.0`                                | `2.8.0` (dsh-rest-api)                   | `3.0.0` (latest compatible with SB 2.3.x)         |
  | Spring Fox Swagger 2  | `2.10.0`                                | `2.8.0` (dsh-rest-api)                   | `3.0.0`                                           |
  | Swagger Annotations   | `1.5.20`                                | `1.5.x`                                  | `1.6.x` (latest 1.x, compatible with springfox 3) |
  | `jaxb-api`            | not declared                            | `2.4.0-b180830.0359` (dsh-rest-api)      | Move to `products/pom.xml` dependencyManagement   |

- **Property unification**:
  - DSH property `spring.boot.version` → **remove**; inherit `spring-boot.version` from products.
  - All DSH references to `${spring.boot.version}` → update to `${spring-boot.version}`.
- **Acceptance Criteria**:
  - `mvn dependency:tree -f dsh/dsh-rest-api/pom.xml | grep spring-boot-starter` shows `2.3.12`.
  - No `spring.boot.version` or `spring-fox.version` property in any DSH POM.
  - `mvn -f dsh/pom.xml package -DskipTests` builds successfully.

---

### FR006 – Move Third-Party Dependency Versions to `products/pom.xml` `dependencyManagement`

- **Description**: Only **third-party** dependency version declarations that are not specific to
  DSH must be moved from DSH POMs to `products/pom.xml` `<dependencyManagement>`. DSH-internal
  module cross-references (e.g. `dsh-data`, `dsh-rest-api`) must remain in the DSH root
  `pom.xml` `<dependencyManagement>` because they are DSH-project artifacts and have no meaning
  in the generic `products` parent.
- **Third-party dependencies to migrate to `products/pom.xml`**:

  | GroupId          | ArtifactId             | Current location                                  | Notes                                                    |
  |------------------|------------------------|---------------------------------------------------|----------------------------------------------------------|
  | `io.springfox`   | `springfox-swagger2`   | `products/pom.xml` (already, version bump needed) | Bump to `3.0.0` per FR005                                |
  | `io.springfox`   | `springfox-swagger-ui` | `products/pom.xml` (already, version bump needed) | Bump to `3.0.0` per FR005                                |
  | `io.swagger`     | `swagger-annotations`  | `products/pom.xml` (already, version bump needed) | Bump to latest `1.6.x` per FR005                         |
  | `javax.xml.bind` | `jaxb-api`             | `dsh-rest-api/pom.xml` inline                     | Move to `products/pom.xml`; version `2.4.0-b180830.0359` |

- **DSH-internal dependencies that MUST stay in DSH root `dependencyManagement`**:

  | GroupId                                   | ArtifactId                    | Reason                                                |
  |-------------------------------------------|-------------------------------|-------------------------------------------------------|
  | `com.mriss.products.dsh`                  | `dsh-rest-api`                | DSH-specific artefact; not relevant to other products |
  | `com.mriss.products.dsh`                  | `dsh-doc-indexer-worker`      | DSH-specific artefact                                 |
  | `com.mriss.products.dsh`                  | `dsh-data`                    | DSH-specific artefact                                 |
  | `com.mriss.products.dsh`                  | `dsh-test-dataset`            | DSH-specific artefact                                 |
  | `com.mriss.products.dsh.dsh-doc-analyser` | `dsh-keyword-extractor`       | DSH-specific artefact                                 |
  | `com.mriss.products.dsh.dsh-doc-analyser` | `dsh-top-sentences-extractor` | DSH-specific artefact                                 |
  | `com.mriss.products.dsh.dsh-doc-analyser` | `dsh-doc-processor-worker`    | DSH-specific artefact                                 |

- **Acceptance Criteria**:
  - `grep -r "dependencyManagement" dsh/ --include="pom.xml"` shows the DSH root still declaring
    all DSH-internal module versions plus the Spring Boot BOM import; no third-party version that
    is already covered by `products/pom.xml` remains in DSH.
  - All modules compile without explicit inline `<version>` on any third-party dependency that is
    now managed in `products/pom.xml`.
  - DSH-internal module versions continue to resolve correctly from DSH root `dependencyManagement`.

---

### FR007 – Remove `maven-scm-publish-plugin` References from DSH

- **Description**: All uses of `maven-scm-publish-plugin` in DSH POMs must be deleted. Artifact
  distribution relies on `<distributionManagement>` already configured in `parent-poms/pom.xml`
  pointing to `https://maven.pkg.github.com/MRISS-Projects/maven-repo`.
- **Input**: All DSH `pom.xml` files
- **Output**: Zero occurrences of `maven-scm-publish-plugin` in DSH source POMs
- **Acceptance Criteria**:
  - `grep -r "scm-publish" dsh/ --include="pom.xml"` returns no results.
  - `mvn -f dsh/pom.xml deploy -DskipTests` targets the GitHub Packages repository via inherited
    `distributionManagement`.

---

### FR008 – Remove All Jenkins References from DSH POM Files

- **Description**: Properties, comments, and plugin configuration referencing Jenkins must be
  removed from all DSH `pom.xml` files. CI/CD is now handled by GitHub Actions.
- **Occurrences to remove**:

  | File                             | Line(s)      | Content                                                     |
  |----------------------------------|--------------|-------------------------------------------------------------|
  | `dsh-rest-api/pom.xml`           | 135          | `<jenkins.view>${project.parent.artifactId}</jenkins.view>` |
  | `dsh-data/pom.xml`               | 62           | `<jenkins.view>${project.parent.artifactId}</jenkins.view>` |
  | `dsh-doc-analyser/pom.xml`       | 28           | `<jenkins.view>${project.parent.artifactId}</jenkins.view>` |
  | `dsh-doc-indexer-worker/pom.xml` | (equivalent) | `<jenkins.view>` property                                   |
  | `parent-poms/pom.xml`            | 152–153      | `<jenkins.view>` and `<jenkins.server>` properties          |
  | `parent-poms/pom.xml`            | 688–689      | `<ciManagement><system>jenkins</system>` block              |

- **Note**: The `parent-poms/pom.xml` Jenkins references are also in scope for this migration.
  `<ciManagement>` should be updated to `<system>github-actions</system>` with the appropriate URL.
- **Acceptance Criteria**:
  - `grep -ri "jenkins" dsh/ --include="pom.xml"` returns no results.
  - `mvn -f dsh/pom.xml validate` passes.

---

### FR009 – Replace `.apt` Site Sources with Markdown

- **Description**: All `.apt` documentation files under `src/site/` in DSH modules must be
  converted to equivalent Markdown (`.md`) files. Resource filtering configuration referencing
  `.apt` must be updated to `.md`.
- **Files to convert** (source only, `target/` excluded):

  | Module                                         | `.apt` file               | Target `.md` file             |
  |------------------------------------------------|---------------------------|-------------------------------|
  | `dsh-coverage-report`                          | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-data`                                     | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-doc-analyser`                             | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-doc-analyser`                             | `src/site/apt/readme.apt` | `src/site/markdown/readme.md` |
  | `dsh-doc-analyser/dsh-doc-processor-worker`    | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-doc-analyser/dsh-doc-processor-worker`    | `src/site/apt/readme.apt` | `src/site/markdown/readme.md` |
  | `dsh-doc-analyser/dsh-keyword-extractor`       | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-doc-analyser/dsh-top-sentences-extractor` | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-doc-indexer-worker`                       | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-rest-api`                                 | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-solr`                                     | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-solr`                                     | `src/site/apt/readme.apt` | `src/site/markdown/readme.md` |
  | `dsh-solr/solr-advanced-numbers-filter`        | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-solr/solr-advanced-numbers-filter`        | `src/site/apt/readme.apt` | `src/site/markdown/readme.md` |
  | `dsh-solr/solr-terms-vector-order`             | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | `dsh-solr/solr-terms-vector-order`             | `src/site/apt/readme.apt` | `src/site/markdown/readme.md` |
  | `dsh-test-dataset`                             | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |
  | Root `dsh`                                     | `src/site/apt/index.apt`  | `src/site/markdown/index.md`  |

- **POM changes per module**: Remove `**/*.apt` include/exclude filters from `<resources>` blocks;
  replace with `**/*.md` where `src/site` filtering is configured.
- **Acceptance Criteria**:
  - `find dsh/ -path "*/src/site/*.apt"` returns no results.
  - `mvn -f dsh/pom.xml site -DskipTests` generates site for all modules without apt-related errors.

---

### FR010 – Fully Inherit Skin Properties

- **Description**: The properties `skin.version`, `skin.artifactId`, and `skin.groupId` are
  already defined in `parent-poms/pom.xml` (lines 137–139). Any re-declaration in DSH module
  POMs must be removed.
- **Occurrences to remove**:

  | File                             | Lines        | Properties                                            |
  |----------------------------------|--------------|-------------------------------------------------------|
  | `dsh-rest-api/pom.xml`           | 120–122      | `skin.version=1.6`, `skin.artifactId`, `skin.groupId` |
  | `dsh-data/pom.xml`               | 47–49        | `skin.version=1.6`, `skin.artifactId`, `skin.groupId` |
  | `dsh-doc-analyser/pom.xml`       | 23–25        | `skin.version=1.6`, `skin.artifactId`, `skin.groupId` |
  | `dsh-doc-indexer-worker/pom.xml` | (equivalent) | same three properties                                 |
  | `parent-poms/products/pom.xml`   | (if present) | any skin property re-declaration                      |

- **Acceptance Criteria**:
  - `grep -r "skin.version\|skin.artifactId\|skin.groupId" dsh/ --include="pom.xml"` returns no results.
  - `mvn -f dsh/pom.xml site -DskipTests` renders the `fluido` skin correctly (version `2.0.0` from parent).

---

### FR011 – Remove Java Compiler Configuration from DSH

- **Description**: Java source/target version is set to `17` via `maven-compiler-plugin`
  `<pluginManagement>` in `parent-poms/pom.xml` (lines 250–253). Any local re-declaration in
  DSH module POMs must be removed.
- **Input**: All DSH `pom.xml` files; also check `parent-poms/products/pom.xml`
- **Acceptance Criteria**:
  - `grep -r "maven-compiler-plugin\|java.version\|<source>\|<target>" dsh/ --include="pom.xml"`
    returns no results (except inherited references in comments).
  - `mvn -f dsh/pom.xml compile` uses Java 17 as confirmed by
    `mvn -f dsh/pom.xml help:effective-pom | grep source`.

---

### FR012 – Standardize Maven Site Reports via Inherited Configuration

- **Description**: The `<reporting>` section in `parent-poms/pom.xml` currently includes
  `maven-changes-plugin`, `maven-project-info-reports-plugin`, and `maven-jxr-plugin` (lines
  582–646). It must be extended to also include `maven-surefire-report-plugin`,
  `maven-failsafe-plugin` report, and `jacoco-maven-plugin` report.  
  DSH module POMs must remove any duplicate `<reporting>` configuration.
- **Reports to add to `parent-poms/pom.xml` `<reporting>`**:

  | Plugin                         | Report Goal                   | Notes                           |
  |--------------------------------|-------------------------------|---------------------------------|
  | `maven-surefire-report-plugin` | `report` (aggregate)          | Unit test results               |
  | `maven-surefire-report-plugin` | `failsafe-report-only`        | Integration test results        |
  | `jacoco-maven-plugin`          | `report` / `report-aggregate` | Coverage per module + aggregate |

- **Acceptance Criteria**:
  - `mvn -f dsh/pom.xml site -DskipTests` generates surefire, failsafe, and `jacoco` HTML reports.
  - No `<reporting>` block in any DSH module POM duplicates a report already declared in a parent.

---

### FR013 – Standardize Integration Test Naming via Failsafe

- **Description**: `parent-poms/pom.xml` `<pluginManagement>` already declares
  `maven-failsafe-plugin` (line 312–314) but provides no `<includes>` configuration. A standard
  naming convention must be added so that integration tests are exclusively executed by Failsafe
  and never by Surefire.
- **Convention**:
  - **Integration tests** (Failsafe): classes matching `**/*IT.java` or `**/*IntegrationTest.java`
  - **Unit tests** (Surefire): classes matching `**/*Test.java` (default; exclude `**/*IT.java`,
    `**/*IntegrationTest.java`)
- **Changes**:
  - `parent-poms/pom.xml` `<pluginManagement>` → add `<configuration>` to `maven-failsafe-plugin`
    with `<includes>` for `*IT.java` / `*IntegrationTest.java` and `<executions>` for
    `integration-test` + `verify` goals.
  - `parent-poms/pom.xml` `<pluginManagement>` → add `<configuration>` to `maven-surefire-plugin`
    with `<excludes>` for `**/*IT.java` and `**/*IntegrationTest.java`.
- **Acceptance Criteria**:
  - `mvn -f dsh/pom.xml verify` runs unit tests via Surefire and integration tests via Failsafe.
  - A test class named `DocumentAnalysisIT` is not picked up by Surefire.
  - A test class named `DocumentAnalysisTest` is not picked up by Failsafe.

---

### FR014 – Add JaCoCo Coverage Check (≥ 95%) to Root Parent POM

- **Description**: A `jacoco:check` execution must be added to `parent-poms/pom.xml` so that
  every module building against the hierarchy enforces a minimum **95% line and branch coverage**.
  DSH modules must inherit this check without any local JaCoCo configuration (beyond the existing
  `prepare-agent` execution in DSH root).
- **Changes to `parent-poms/pom.xml`**:
  - Add `jacoco-maven-plugin` `<execution>` id `check-code-coverage` bound to `verify` phase
    with goal `check`.
  - Rules: `BUNDLE` counter for `LINE` and `BRANCH`, minimum value `0.95`.
- **DSH changes**:
  - Remove any `jacoco:check` or coverage rule declarations from DSH module POMs.
  - Keep only `prepare-agent` execution in DSH root (or move that up too if generic enough).
- **Acceptance Criteria**:
  - `mvn -f dsh/pom.xml verify` fails build for any module with line or branch coverage below 95%.
  - `mvn help:effective-pom -f dsh/dsh-data/pom.xml | grep jacoco` shows the check execution
    inherited from the parent.

---

## Technical Implementation Notes

### Module Integration
- **`parent-poms/pom.xml`**: Receives Failsafe naming config, Surefire exclusion, JaCoCo check,
  Surefire/Failsafe/JaCoCo reporting, removal of Jenkins `ciManagement`.
- **`parent-poms/products/pom.xml`**: Receives Spring Boot `2.3.12.RELEASE`, updated Spring Fox /
  Swagger versions, Spring Boot Maven plugin in `pluginManagement`, generic `deployment` profile
  plugin config, DSH and framework dependency versions in `dependencyManagement`.
- **DSH root `pom.xml`**: Parent ref updated; properties unified; profiles cleaned; plugin versions
  removed; `maven-scm-publish-plugin` removed; `dependencyManagement` thinned.
- **DSH module POMs** (`dsh-rest-api`, `dsh-data`, `dsh-doc-analyser`, `dsh-doc-indexer-worker`,
  etc.): Skin properties removed; Jenkins properties removed; `.apt` resource filters updated to
  `.md`; plugin versions removed.

### Sequencing (Implementation Order)
1. **Step 1** – Update `parent-poms/pom.xml`: Failsafe/Surefire config (FR013), JaCoCo check
   (FR014), reporting (FR012), remove Jenkins `ciManagement` (FR008), add `pluginManagement`
   entries for any plugin currently only versioned in DSH.
2. **Step 2** – Update `parent-poms/products/pom.xml`: Spring Boot bump (FR005), dependency
   versions (FR006), generic plugin config / `pluginManagement` (FR003, FR004).
3. **Step 3** – Install parent-poms SNAPSHOT locally:
   `mvn -f parent-poms/pom.xml install -DskipTests`.
4. **Step 4** – Update DSH root `pom.xml`: parent ref (FR001), profile cleanup (FR002), property
   unification (FR005), remove `maven-scm-publish-plugin` (FR007), thin `dependencyManagement`
   (FR006).
5. **Step 5** – Update each DSH module POM: remove skin props (FR010), Jenkins props (FR008),
   plugin versions (FR003), `.apt` resource filters (FR009).
6. **Step 6** – Convert `.apt` files to Markdown (FR009).
7. **Step 7** – Validate full build: `mvn -f dsh/pom.xml verify site -DskipTests`.

### Data Flow
```
parent-poms/pom.xml  (root – plugin versions, Failsafe/Surefire/JaCoCo config, reporting)
    └── parent-poms/products/pom.xml  (Spring Boot BOM, Spring Fox, dependency management)
            └── dsh/pom.xml  (DSH-specific modules, enforcer, DSH profiles only)
                    └── dsh/dsh-rest-api/pom.xml  (DSH-specific only)
                    └── dsh/dsh-data/pom.xml
                    └── ...
```

---

## Testing Requirements

### Unit Tests
- Each POM change must be validated with `mvn validate` at the module level.
- Property inheritance verified with `mvn help:effective-pom`.

### Integration Tests
- `mvn -f dsh/pom.xml verify` must pass with no Surefire/Failsafe mis-categorization.
- `mvn -f dsh/pom.xml site` must generate HTML reports for Surefire, Failsafe, and JaCoCo.
- Coverage gate: build must fail if any module drops below 95% (FR014).

### Regression Tests
- Snapshot deployment: `mvn -f dsh/pom.xml deploy -DskipTests` must reach
  `https://maven.pkg.github.com/MRISS-Projects/maven-repo`.
- Spring Boot application context loads in `dsh-rest-api` with Spring Boot `2.3.12.RELEASE`.

---

## References

- Architecture Decision: `specs/architecture/ADR-002-pom-hierarchy-configuration.md`
- Architecture Overview: `specs/architecture/system-design.md`
- Parent root POM: `D:/IdeaProjects/parent-poms/pom.xml`
- Products POM: `D:/IdeaProjects/parent-poms/products/pom.xml`
- DSH root POM: `D:/IdeaProjects/dsh/pom.xml`
- Java conventions: `.github/copilot/rules/java-conventions.md`
- Test plans: `specs/testing/test-plans/`
- Acceptance criteria: `specs/requirements/acceptance-criteria/`


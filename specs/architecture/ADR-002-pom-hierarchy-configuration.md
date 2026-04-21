# ADR-002: POM Hierarchy Configuration

## Date
2026-04-21

## Status
Proposed

## Context

The DSH project currently manages a large amount of Maven build configuration directly in its own `pom.xml` files (root and module levels), duplicating or overriding settings that already exist — or should exist — in the parent POM hierarchy (`parent-poms/products/pom.xml` → `parent-poms/pom.xml`). This leads to:

- Duplicated plugin versions and configuration scattered across multiple modules.
- Profile definitions in DSH that mirror profiles in parent POMs, making maintenance harder.
- Spring Boot, Spring Fox, and Swagger annotation versions defined locally in DSH rather than centrally.
- Dependency versions managed in DSH's `dependencyManagement` instead of in `products/pom.xml`.
- Jenkins-specific references in POM files even though the project has migrated to GitHub Actions.
- Site generation reports (Surefire, Failsafe, JaCoCo) not consistently inherited from the root parent.
- Integration test naming conventions not standardised across the hierarchy.
- Code coverage enforcement absent from the root parent POM.
- Skin and Java compiler properties re-declared in DSH modules instead of being fully inherited.
- Source documentation in `.apt` format instead of Markdown.
- `maven-scm-publish-plugin` used for artifact distribution when `distributionManagement` to `https://maven.pkg.github.com/MRISS-Projects/maven-repo` is already configured at the root parent.

The goal is to make the DSH project inherit as much configuration as possible from the parent-poms hierarchy, keeping only DSH-specific customisations locally.

## Decision

DSH Maven POM configurations will now be managed by the generic infrastructure provided by the `parent-poms` project. The following concrete changes will be made:

### 1.1 – Update DSH parent reference to current `products` SNAPSHOT

The `<parent>` section of the root DSH `pom.xml` will be updated to reference the current SNAPSHOT version of `parent-poms/products/pom.xml` (e.g. `3.7.1-SNAPSHOT`), so that all changes made in the parent hierarchy are immediately testable against DSH.

### 1.2 – Remove duplicate Maven profiles from DSH

Any Maven `<profile>` in DSH POM files that has the same `<id>` as a profile declared in a parent POM and provides no DSH-specific configuration will be removed. Profiles that only customise DSH-specific resources (e.g. module-specific output paths) may remain, but must be documented inline.

### 1.3 – Remove plugin versions from DSH; centralise in root parent `pluginManagement`

All explicit `<version>` elements for Maven plugins in DSH `pom.xml` files will be removed. The corresponding plugin versions and their primary configuration will be declared (or updated) in the `<pluginManagement>` section of `parent-poms/pom.xml`. DSH modules will only override execution-specific configuration that is genuinely DSH-specific.

### 1.4 – Move generic plugin configuration up the hierarchy

Any Maven plugin configuration block in DSH that is not specific to the DSH project (e.g. standard resource filtering, standard site settings) will be moved to the appropriate parent POM so it can be reused by other products.

### 1.5 – Bump Spring Boot version in `products/pom.xml`; align Spring Fox and Swagger annotations

- The `spring-boot.version` property in `products/pom.xml` will be updated from `2.3.4.RELEASE` to `2.3.12.RELEASE` (the version currently used by DSH).
- Spring Fox and Swagger annotation dependency versions in `products/pom.xml` will be updated to the latest versions compatible with Spring Boot `2.3.12.RELEASE`.
- The property name used in DSH (`spring.boot.version`) will be unified with the one used in `products/pom.xml` (`spring-boot.version`), and the local DSH property will be removed in favour of the inherited one.

### 1.6 – Move all DSH dependency versions to `products/pom.xml` `dependencyManagement`

All dependency version declarations currently in DSH `pom.xml` files will be removed. The corresponding entries will be added to the `<dependencyManagement>` section of `products/pom.xml` so they are available to all product modules.

### 1.7 – Remove `maven-scm-publish-plugin` from DSH

All references to `maven-scm-publish-plugin` in DSH POM files will be deleted. Artifact distribution will be handled exclusively via the `<distributionManagement>` configuration already present in `parent-poms/pom.xml`, pointing to `https://maven.pkg.github.com/MRISS-Projects/maven-repo`.

### 1.8 – Remove Jenkins references from POM files

All Jenkins-specific properties, profile activations, or plugin configurations (e.g. `<jenkins.view>`) in DSH POM files will be removed. The project now uses GitHub Actions for CI/CD.

### 1.9 – Replace `.apt` site sources with Markdown

All `.apt` documentation files under any `src/site` directory in DSH modules will be converted to equivalent Markdown (`.md`) files. Resource filtering configuration that included `.apt` files will be updated accordingly.

### 1.10 – Fully inherit skin properties

The properties `skin.version`, `skin.artifactId`, and `skin.groupId` (including any reference to `${doxia.skin.version}`) must be defined only in the root parent POM and fully inherited. Any re-declaration of these properties or their versions in DSH module POMs will be removed.

### 1.11 – Remove Java compiler configuration from DSH

Source and binary Java version settings (e.g. `maven-compiler-plugin` configuration for `source`/`target`, or `java.version` properties) are set at `parent-poms/pom.xml` and must not be overridden in DSH. All such local declarations will be removed.

### 1.12 – Standardise Maven site reports via inherited configuration

The `<reporting>` section in `parent-poms/pom.xml` will be updated to always include Surefire, Failsafe, and JaCoCo reports. DSH module POMs will remove any duplicate reporting configuration and rely on inheritance.

### 1.13 – Standardise integration test naming via Failsafe configuration

`parent-poms/pom.xml` will define a standard naming convention for integration tests (e.g. classes matching `**/*IT.java` or `**/*IntegrationTest.java`) executed exclusively by the `maven-failsafe-plugin`. Unit tests (Surefire) will continue to match `**/*Test.java`. This convention will be inherited by DSH and all other products.

### 1.14 – Add JaCoCo coverage check to root parent POM

If not already present, a `jacoco:check` execution will be added to `parent-poms/pom.xml` enforcing a minimum line/branch coverage of **95%** for all modules. DSH modules will inherit this check without needing local JaCoCo configuration.

## Consequences

### Positive
- Single source of truth for plugin versions, dependency versions, and build configuration.
- Easier upgrades: bumping a version in `products/pom.xml` applies to all products automatically.
- Cleaner, shorter DSH POM files that are easier to read and maintain.
- Consistent CI behaviour across all product modules (same test conventions, same coverage gates).
- Removal of Jenkins artefacts reduces noise for teams using GitHub Actions exclusively.
- Uniform Markdown-based documentation across all site sources.
- Guaranteed 95 %+ code coverage enforced at build time for all products.

### Negative
- Initial migration effort is non-trivial: each DSH module POM and the two parent POMs must be updated in a coordinated way.
- Bumping the Spring Boot version and aligning Spring Fox/Swagger may require minor code changes in DSH if any APIs have changed between `2.3.4.RELEASE` and `2.3.12.RELEASE`.
- Moving configuration to `parent-poms` may affect other products that also inherit from it; those products must be tested after the parent changes are merged.
- Removing the 95 % coverage gate may temporarily fail builds for modules currently below that threshold, requiring test additions before the change can be merged.

## Alternatives Considered

1. **Keep all configuration in DSH POMs** – Rejected. It leads to long-term maintenance burden and version drift across products.
2. **Create a DSH-specific intermediate parent POM** – Considered but rejected for now. The `products/pom.xml` level is sufficient; adding another intermediate level would increase hierarchy complexity without proportional benefit.
3. **Use a BOM-only approach (import scope)** – Partially applicable for dependency management, but does not solve plugin configuration centralisation. Rejected as the sole strategy.

## References

- [`parent-poms/products/pom.xml`](../../parent-poms/products/pom.xml)
- [`parent-poms/pom.xml`](../../parent-poms/pom.xml)
- [DSH root `pom.xml`](../../pom.xml)
- [`specs/architecture/system-design.md`](system-design.md)
- [`specs/features/rest-api-endpoints.md`](../features/rest-api-endpoints.md)
- [GitHub Actions workflows](../../.github/workflows/)


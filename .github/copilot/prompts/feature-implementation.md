# Feature Implementation Prompts

Use these prompt templates with GitHub Copilot Chat to implement features defined in the `specs/features/` folder.

---

## Implement Feature from Specification

```
As a Backend Developer (see /.github/roles.md), implement the feature described in
specs/features/[feature-file].md for the DSH project.

Requirements:
- Read and follow all functional requirements (FRxxx) in the feature specification
- Implement changes in the appropriate DSH modules:
  - DSH-rest-api – for REST endpoint changes
  - DSH-doc-analyser – for document analysis logic
  - DSH-doc-indexer-worker – for indexing workflow changes
  - DSH-data – for data model changes
- Follow Java conventions in /.github/copilot/rules/java-conventions.md
- Follow API standards in /.github/copilot/rules/api-standards.md
- Conform to OpenAPI spec in /specs/api/openapi/dsh-rest-api.yaml
- Use Spring Boot patterns: constructor injection, @Service, @RestController
- Use SLF4J with @Slf4j (Lombok) for logging
- Include Bean Validation annotations (@NotNull, @Valid, etc.)
- Generate comprehensive JavaDoc for all public classes and methods
- Write unit tests following patterns in /.github/copilot/rules/testing-patterns.md
- Cover acceptance criteria from /specs/requirements/acceptance-criteria/
```

---

## Implement Feature – Frontend

```
As a Frontend Developer (see /.github/roles.md), implement the UI for the feature
described in specs/features/[feature-file].md for the DSH project.

Requirements:
- Read and follow all functional requirements (FRxxx) in the feature specification
- Consume REST APIs defined in /specs/api/openapi/dsh-rest-api.yaml
- Implement UI components and views as described in the feature spec
- Ensure the implementation matches acceptance criteria in
  /specs/requirements/acceptance-criteria/[feature].md
- Write unit tests for all new components
```

---

## Implement Feature – DevOps Support

```
As a DevOps Developer (see /.github/roles.md), set up the infrastructure and CI/CD
support for the feature described in specs/features/[feature-file].md.

Requirements:
- Create or update GitHub Actions workflows in .github/workflows/
- Update Maven pom.xml configurations if new modules or dependencies are needed
- Update build scripts (build-ci.sh, deploy.sh) as necessary
- Ensure quality gates and automated tests run for the new feature
- Reference /specs/testing/performance-benchmarks/ for any performance targets
```

---

## Implement Feature – Product Manager Review

```
As a Product Manager (see /.github/roles.md), review and refine the feature
specification at specs/features/[feature-file].md before implementation begins.

Requirements:
- Verify the specification has clear, actionable functional requirements
- Ensure acceptance criteria exist in /specs/requirements/acceptance-criteria/
- Confirm data flow and technical notes are aligned with /specs/architecture/
- Validate that the feature aligns with overall project goals in README.md
- Update the specification if gaps are found
```


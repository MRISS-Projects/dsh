# Copilot Prompt Examples

Concrete example prompts and expected outputs for common DSH development tasks.
Each prompt begins with **"As a [Role]"** matching the team roles defined in
`/.github/roles.md`. See the prompt templates in `/.github/copilot/prompts/` for
the full scaffolding instructions.

---

## Example 1: Generate a New Service (Component Generation)

**Template:** `/.github/copilot/prompts/component-generation.md` → New Spring Boot Service

**Prompt:**
```
As a Backend Developer (see /.github/roles.md), generate a Spring Boot @Service
class called DocumentAnalysisService in the DSH project.

Requirements:
- Package: com.dsh.docanalyser
- Dependencies to inject: DocumentRepository, HighlightRepository
- Methods:
  - analyzeDocument(String documentId) → AnalysisResult
- Error handling: throw DocumentNotFoundException if the document is not found
- Logging: use @Slf4j, log entry/exit at DEBUG, errors at ERROR
- Follow conventions in /.github/copilot/rules/java-conventions.md
- Include JavaDoc referencing /specs/features/document-analysis.md
```

**Expected Output:** A fully scaffolded service class with constructor injection, SLF4J logging, JavaDoc, and custom exception handling.

---

## Example 2: Generate Controller Tests (Test Generation)

**Template:** `/.github/copilot/prompts/test-generation.md` → Controller Slice Test

**Prompt:**
```
As a Backend Developer (see /.github/roles.md), generate @WebMvcTest tests for
DocumentController in the DSH project.

Requirements:
- Test class name: DocumentControllerTest
- Mock service dependencies with @MockBean
- Test cases for each endpoint:
  - POST /api/v1/documents: success (expected status 202)
  - POST /api/v1/documents: validation error (expected status 400)
  - POST /api/v1/documents: file too large (expected status 413)
  - GET /api/v1/documents/{documentId}: success (expected status 200)
  - GET /api/v1/documents/{documentId}: not found (expected status 404)
- Use MockMvc with jsonPath assertions
- Verify response body structure matches /specs/api/openapi/dsh-rest-api.yaml
- Follow patterns in /.github/copilot/rules/testing-patterns.md
```

**Expected Output:** A complete `DocumentControllerTest` class with test methods following the AAA pattern.

---

## Example 3: Add an OpenAPI Endpoint (Documentation Generation)

**Template:** `/.github/copilot/prompts/documentation-generation.md` → API Endpoint Documentation

**Prompt:**
```
As a Backend Developer (see /.github/roles.md), document the DELETE
/documents/{documentId} endpoint for the DSH REST API.

Add to: specs/features/rest-api-endpoints.md

Include:
- Endpoint purpose and description
- Request parameters (path: documentId)
- Response: 204 No Content on success, 404 if not found
- Authentication: bearerAuth required
- Corresponding OpenAPI definition in /specs/api/openapi/dsh-rest-api.yaml
```

**Expected Output:** A well-formed endpoint documentation entry and a matching OpenAPI path entry with correct responses and security.

---

## Example 4: Generate a Feature Spec (Documentation Generation)

**Template:** `/.github/copilot/prompts/documentation-generation.md` → Feature Specification

**Prompt:**
```
As a Product Manager (see /.github/roles.md), generate a feature specification
document for "Document Deletion" in the DSH project.

Save to: specs/features/document-deletion.md

Include:
- Overview and purpose
- Functional requirements:
  - FR001: Delete document and all associated highlights
  - FR002: Return 404 if document does not exist
- Acceptance criteria for each requirement
- Technical implementation notes referencing dsh-rest-api and dsh-data
- Testing requirements (unit and integration)
- References to related specs and architecture documents
```

**Expected Output:** A fully structured Markdown feature spec consistent with `specs/features/document-analysis.md`.

---

## Example 5: Implement a Feature from Spec (Feature Implementation)

**Template:** `/.github/copilot/prompts/feature-implementation.md` → Implement Feature from Specification

**Prompt:**
```
As a Backend Developer (see /.github/roles.md), implement the feature described in
specs/features/document-analysis.md for the DSH project.

Requirements:
- Read and follow all functional requirements (FRxxx) in the feature specification
- Implement changes in the appropriate DSH modules:
  - dsh-rest-api – for REST endpoint changes
  - dsh-doc-analyser – for document analysis logic
  - dsh-data – for data model changes
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

**Expected Output:** New or updated Java classes across the relevant DSH modules, with services, controllers, data models, unit tests, and JavaDoc — all aligned with the feature specification.

---

## Example 6: Review a Feature Spec (Feature Implementation)

**Template:** `/.github/copilot/prompts/feature-implementation.md` → Implement Feature – Product Manager Review

**Prompt:**
```
As a Product Manager (see /.github/roles.md), review and refine the feature
specification at specs/features/smart-highlighting.md before implementation begins.

Requirements:
- Verify the specification has clear, actionable functional requirements
- Ensure acceptance criteria exist in /specs/requirements/acceptance-criteria/
- Confirm data flow and technical notes are aligned with /specs/architecture/
- Validate that the feature aligns with overall project goals in README.md
- Update the specification if gaps are found
```

**Expected Output:** An annotated review of the specification highlighting gaps, ambiguities, or missing acceptance criteria, with suggested improvements applied directly.

---

## Example 7: Set Up CI/CD for a Feature (Feature Implementation)

**Template:** `/.github/copilot/prompts/feature-implementation.md` → Implement Feature – DevOps Support

**Prompt:**
```
As a DevOps Developer (see /.github/roles.md), set up the infrastructure and CI/CD
support for the feature described in specs/features/indexing-workflow.md.

Requirements:
- Create or update GitHub Actions workflows in .github/workflows/
- Update Maven pom.xml configurations if new modules or dependencies are needed
- Update build scripts (build-ci.sh, deploy.sh) as necessary
- Ensure quality gates and automated tests run for the new feature
- Reference /specs/testing/performance-benchmarks/ for any performance targets
```

**Expected Output:** New or updated workflow YAML files, Maven configuration changes, and build script updates to support the indexing-workflow feature in CI/CD.

---

## Example 8: Explain Existing Code

**Prompt (in Copilot Chat, with a file open):**
```
Explain what this class does and how it relates to
/specs/features/indexing-workflow.md
```

**Expected Output:** A plain-English explanation of the class's role in the indexing workflow, referencing the spec steps.

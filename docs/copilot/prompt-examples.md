# Copilot Prompt Examples

Concrete example prompts and expected outputs for common DSH development tasks.

---

## Example 1: Generate a New Service

**Prompt:**
```
Using the conventions in /.github/copilot/rules/java-conventions.md,
generate a Spring Boot @Service class called DocumentAnalysisService.

It should:
- Inject DocumentRepository and HighlightRepository via constructor
- Have a method analyzeDocument(String documentId) returning AnalysisResult
- Throw DocumentNotFoundException if the document is not found
- Log entry/exit at DEBUG level with @Slf4j
- Include JavaDoc referencing /specs/features/document-analysis.md
```

**Expected Output:** A fully scaffolded service class with constructor injection, SLF4J logging, JavaDoc, and custom exception handling.

---

## Example 2: Generate Controller Tests

**Prompt:**
```
Generate @WebMvcTest tests for DocumentController.

Test the following endpoints from /specs/api/openapi/dsh-rest-api.yaml:
- POST /api/v1/documents: success (202), validation error (400), file too large (413)
- GET /api/v1/documents/{documentId}: success (200), not found (404)

Mock DocumentService with @MockBean.
Use MockMvc and AssertJ. Follow /.github/copilot/rules/testing-patterns.md.
```

**Expected Output:** A complete `DocumentControllerTest` class with test methods following the AAA pattern.

---

## Example 3: Add an OpenAPI Endpoint

**Prompt:**
```
Add a DELETE /documents/{documentId} endpoint to the OpenAPI spec at
/specs/api/openapi/dsh-rest-api.yaml.

The endpoint should:
- Require the documentId path parameter
- Return 204 No Content on success
- Return 404 if the document does not exist
- Require bearerAuth
- Follow the patterns already in the file
```

**Expected Output:** A well-formed OpenAPI path entry with the correct responses and security.

---

## Example 4: Generate a Feature Spec

**Prompt:**
```
Generate a feature specification document for "Document Deletion" in the DSH project.

Save to: specs/features/document-deletion.md

Include:
- Overview
- FR001: Delete document and all associated highlights
- FR002: Return 404 if document does not exist
- Acceptance criteria for each requirement
- Technical implementation notes referencing DSH-rest-api and DSH-data
- Testing requirements
- References to related specs
```

**Expected Output:** A fully structured Markdown feature spec consistent with `specs/features/document-analysis.md`.

---

## Example 5: Explain Existing Code

**Prompt (in Copilot Chat, with a file open):**
```
Explain what this class does and how it relates to
/specs/features/indexing-workflow.md
```

**Expected Output:** A plain-English explanation of the class's role in the indexing workflow, referencing the spec steps.

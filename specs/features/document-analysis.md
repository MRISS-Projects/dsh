# Document Analysis Feature Specification

## Overview
The document analysis feature processes uploaded documents to identify and extract smart highlights based on configurable analysis rules. Analysis is performed asynchronously by the `dsh-doc-analyser` module, orchestrated through the `dsh-doc-indexer-worker`.

## Functional Requirements

### FR001: Document Upload
- **Description**: The system shall accept document uploads in PDF, DOC, and DOCX formats
- **Input**: Document file + optional `AnalysisOptions`
- **Output**: Document ID + `processing` status
- **Acceptance Criteria**:
  - Maximum file size: 50 MB
  - Supported formats are validated; unsupported formats return HTTP 400
  - A unique document ID is generated and returned immediately (HTTP 202)
  - The document is persisted for downstream async processing

### FR002: Asynchronous Content Analysis
- **Description**: The system shall analyse document content for highlight candidates via a background worker
- **Algorithm**: Configurable analysis rules including:
  - Keyword matching
  - Semantic analysis
  - Pattern recognition
  - Named entity recognition
- **Performance**: Analysis completes within 30 seconds for documents under 10 MB
- **Acceptance Criteria**:
  - Document status transitions: `processing` → `completed` or `failed`
  - Status can be polled via `GET /api/v1/documents/{documentId}`
  - Failed analysis updates status to `failed` with an error message

### FR003: Highlight Generation
- **Description**: The system shall generate highlights with position data and confidence scores
- **Output**: Position-based highlights with type, confidence, and optional metadata
- **Confidence Threshold**: Configurable per request (default: 0.7)
- **Acceptance Criteria**:
  - Each highlight has a unique ID, text content, start/end position, and confidence score
  - Highlights with confidence below the threshold are excluded
  - Highlights are accessible via `GET /api/v1/documents/{documentId}/highlights`

### FR004: Analysis Configuration
- **Description**: Clients shall be able to configure the analysis behaviour per request
- **Input**: `AnalysisOptions` in the upload request
- **Configurable Parameters**:
  - `highlightTypes` – subset of analysis algorithms to apply
  - `confidenceThreshold` – minimum confidence score
  - `maxHighlights` – limit on number of returned highlights

## Technical Implementation

### Component Integration
- **dsh-rest-api**: Accepts upload, validates input, returns document ID
- **dsh-doc-indexer-worker**: Picks up queued documents, orchestrates analysis
- **dsh-doc-analyser**: Executes analysis algorithms, produces highlights
- **dsh-data**: Persists documents, analysis results, and highlights
- **dsh-solr**: Indexes document content for full-text search

### Data Flow
```
Client → [POST /documents] → dsh-rest-api
  → persist document (dsh-data / MongoDB)
  → enqueue analysis task
  → return {documentId, status: "processing"}

dsh-doc-indexer-worker (async):
  → dequeue task
  → call dsh-doc-analyser.analyze(document)
  → persist highlights (dsh-data)
  → index content (dsh-solr)
  → update status to "completed"
```

## Testing Requirements

### Unit Tests
- Analysis algorithm validation for each highlight type
- Configuration parameter boundary tests
- Error handling: unsupported format, file too large, analysis failure

### Integration Tests
- End-to-end document processing (upload → poll → retrieve highlights)
- API endpoint validation against OpenAPI spec
- Performance: analysis within SLA for representative document sizes

## References
- Architecture: `../architecture/system-design.md`
- API Specification: `../api/openapi/dsh-rest-api.yaml`
- Test Plans: `../testing/test-plans/`
- Test Cases: `../testing/test-cases/`
- Acceptance Criteria: `../requirements/acceptance-criteria/`

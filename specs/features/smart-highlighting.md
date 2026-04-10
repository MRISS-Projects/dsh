# Smart Highlighting Feature Specification

## Overview
The smart highlighting feature enables the system to intelligently identify and surface the most relevant passages in a document, providing users with a concise view of key information. Highlights are ranked by confidence and categorised by type.

## Functional Requirements

### FR001: Highlight Display
- **Description**: Clients shall be able to retrieve highlights for any successfully analysed document
- **Input**: `documentId`, optional filters (`type`, `minConfidence`, pagination)
- **Output**: Paginated `HighlightCollection`
- **Acceptance Criteria**:
  - Returns highlights for `completed` documents
  - Returns empty collection for documents with no highlights
  - Returns 404 for unknown `documentId`

### FR002: Highlight Filtering
- **Description**: Clients shall be able to filter highlights by type and confidence
- **Acceptance Criteria**:
  - `type` filter returns only highlights of the specified category
  - `minConfidence` filter excludes highlights below the threshold
  - Combined filters work correctly together
  - Default `minConfidence` is 0.7 when not specified

### FR003: Highlight Pagination
- **Description**: The API shall support pagination of highlight results
- **Acceptance Criteria**:
  - `page` and `size` parameters control pagination
  - Response includes `totalElements` and `totalPages`
  - Default page size is 20; maximum is 100

### FR004: Highlight Types
- **Description**: The system shall support multiple categories of highlights

| Type | Description |
|------|-------------|
| `keyword` | Exact keyword or phrase matches |
| `semantic` | Semantically relevant passages |
| `pattern` | Regex or structural pattern matches |
| `entity` | Named entities (persons, organisations, dates) |

## Technical Implementation

### Highlight Data Model
Each highlight contains:
- `id` – unique identifier
- `text` – highlighted text content
- `position` – `{start, end, page}` character offsets
- `confidence` – score between 0.0 and 1.0
- `type` – highlight category
- `metadata` – additional type-specific data

### Ranking Strategy
Highlights are sorted by:
1. Confidence score (descending)
2. Position in document (ascending, as tiebreaker)

## Testing Requirements

### Unit Tests
- Highlight filtering logic for each filter parameter
- Pagination boundary conditions
- Sorting correctness

### Integration Tests
- Full flow: upload document → analyse → retrieve highlights with filters
- Verify response structure matches `../api/openapi/dsh-rest-api.yaml`

## References
- Document Analysis: `./document-analysis.md`
- API Specification: `../api/openapi/dsh-rest-api.yaml`
- REST API Endpoints: `./rest-api-endpoints.md`

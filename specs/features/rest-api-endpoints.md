# REST API Endpoints Specification

This document provides a human-readable description of all DSH REST API endpoints. The machine-readable OpenAPI contract is in `../api/openapi/dsh-rest-api.yaml`.

## Base URL
```
/api/v1
```

## Authentication
All endpoints require a JWT Bearer token in the `Authorization` header, except `/health`.

```
Authorization: Bearer <token>
```

---

## Documents

### POST /documents
**Upload and analyse a document**

- Accepts a multipart file upload
- Returns immediately with `202 Accepted` and a `documentId`
- Analysis proceeds asynchronously

**Request**: `multipart/form-data`
- `file` (required) – document file (PDF, DOC, DOCX; max 50 MB)
- `options` (optional) – JSON `AnalysisOptions`

**Response 202**:
```json
{ "documentId": "doc-abc123", "status": "processing", "highlights": [] }
```

---

### GET /documents/{documentId}
**Get document analysis status**

- Returns current processing status
- When status is `completed`, highlights are available via the highlights endpoint

**Response 200**:
```json
{
  "documentId": "doc-abc123",
  "status": "completed",
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": "2024-01-15T10:30:28Z"
}
```

---

## Highlights

### GET /documents/{documentId}/highlights
**Retrieve highlights for a document**

**Query Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | string | — | Filter by highlight type |
| `minConfidence` | number | 0.7 | Minimum confidence score |
| `page` | integer | 0 | Page number (0-based) |
| `size` | integer | 20 | Page size (max 100) |

**Response 200**:
```json
{
  "documentId": "doc-abc123",
  "highlights": [
    {
      "id": "hl-001",
      "text": "machine learning",
      "position": { "start": 150, "end": 166, "page": 2 },
      "confidence": 0.92,
      "type": "keyword"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

---

## Health

### GET /health
**Service health check** (no authentication required)

**Response 200**:
```json
{ "status": "UP", "timestamp": "2024-01-15T10:30:00Z" }
```

---

## Error Responses

All error responses use this structure:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'confidence'",
  "path": "/api/v1/documents"
}
```

## References
- OpenAPI Spec: `../api/openapi/dsh-rest-api.yaml`
- Document Analysis: `./document-analysis.md`
- Smart Highlighting: `./smart-highlighting.md`
- API Standards: `../../.github/copilot/rules/api-standards.md`

# Indexing Workflow Specification

## Overview
The indexing workflow describes how documents submitted via the REST API are processed asynchronously by the `dsh-doc-indexer-worker` module, from task enqueuing through analysis and Solr indexing to final status persistence.

## Workflow Steps

```
1. Document Received
   └── REST API persists document metadata (status: processing)
   └── REST API enqueues indexing task

2. Task Dequeued
   └── Worker picks up task from queue
   └── Worker validates document is available

3. Content Extraction
   └── Worker extracts raw text from document (PDF/DOC/DOCX)
   └── Text is normalised (encoding, whitespace)

4. Analysis
   └── Worker calls dsh-doc-analyser with document content and options
   └── Analyser applies configured algorithms
   └── Analyser returns list of Highlight objects

5. Persistence
   └── Worker persists highlights to MongoDB (dsh-data)
   └── Worker updates document status to: completed

6. Solr Indexing
   └── Worker indexes document content in Apache Solr (dsh-solr)
   └── Enables full-text search over document content

7. Error Handling
   └── On any failure: update document status to: failed
   └── Persist error message for client retrieval
   └── Log full stack trace for diagnostics
```

## Functional Requirements

### FR001: Asynchronous Processing
- Documents must be processed without blocking the REST API response
- Processing queue must support at-least-once delivery semantics
- Failed tasks must be retried up to 3 times before marking as failed

### FR002: Status Tracking
- Document status is always queryable via `GET /api/v1/documents/{documentId}`
- Status transitions: `processing` → `completed` | `failed`
- Status updates must be atomic (no partial updates visible to clients)

### FR003: Solr Indexing
- Document full text must be indexed in Solr on successful analysis
- Solr index must be updated atomically with status change
- Solr document must be removed if the parent document is deleted

### FR004: Performance
- Worker must process 95% of documents under 10 MB within 30 seconds
- Worker must handle at least 10 concurrent document processing tasks

## Error Handling

| Scenario | Behaviour |
|----------|-----------|
| File not found / corrupted | Immediate failure; status → failed |
| Unsupported file format | Immediate failure; status → failed |
| Analysis timeout | Retry up to 3 times; then status → failed |
| Solr unavailable | Retry with backoff; alert on prolonged failure |
| Database unavailable | Retry with backoff; do not lose task |

## References
- System Architecture: `../architecture/system-design.md`
- Document Analysis: `./document-analysis.md`
- Performance Benchmarks: `../testing/performance-benchmarks/`

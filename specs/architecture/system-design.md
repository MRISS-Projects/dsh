# DSH System Architecture

## Overview

Document Smart Highlights (DSH) is a multi-module Java/Spring Boot application that analyses documents and generates smart highlights based on configurable rules. The system is composed of loosely coupled services communicating via REST APIs and an asynchronous message/worker queue.

## System Components

```text
┌─────────────────────────────────────────────────────────┐
│                        Clients                          │
│            (Web UI / External API Consumers)            │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP/REST
                      ▼
┌─────────────────────────────────────────────────────────┐
│                   dsh-rest-api                           │
│         Spring Boot REST API Service Layer              │
│   (routes requests, validation, authentication)        │
└────────────┬────────────────────────┬───────────────────┘
             │                        │
             ▼                        ▼
┌────────────────────┐   ┌────────────────────────────────┐
│  dsh-doc-analyser  │   │    dsh-doc-indexer-worker      │
│  Analysis Engine   │   │  Background Indexing Worker    │
│  (sync analysis)   │   │  (async document processing)   │
└────────────┬───────┘   └────────────┬───────────────────┘
             │                        │
             └────────────┬───────────┘
                          ▼
          ┌───────────────────────────────┐
          │          dsh-data             │
          │  Shared Data Models &         │
          │  Persistence Layer            │
          └───────────┬───────────────────┘
                      │
             ┌────────┴────────┐
             ▼                 ▼
     ┌───────────────────┐  ┌───────────────────┐
     │  Document store   │  │     dsh-solr      │
     │  (in memory by    │  │  (removal         │
     │   default)        │  │   decided)        │
     │  (documents)      │  │ (full-text        │
     │                   │  │  indexing)        │
     └───────────────────┘  └───────────────────┘
```

> **Migration note:** [ADR-001](./ADR-001-GCP-based-components.md) (Status: Proposed) proposes
> a Firestore + GCS document store, and replacing RabbitMQ with Cloud Pub/Sub.
> [ADR-003](./ADR-003-document-pipeline.md) (Status: Accepted) decides how the pipeline is built:
> the MongoDB, RabbitMQ and Solr code is removed, not migrated, and no search engine replaces
> Solr. The MongoDB code was removed by `#167` (ADR-003 section 8), and the RabbitMQ and Solr code
> is still in the repository.
> See `specs/product/PRD.md` for the delivery waves.

## Module Responsibilities

### dsh-rest-api

- Exposes the public REST API (see `../api/openapi/dsh-rest-api.yaml`)
- Handles authentication, request validation, and routing
- Delegates business logic to `dsh-doc-analyser` and `dsh-doc-indexer-worker`

### dsh-doc-analyser

- Core document analysis engine
- Implements keyword, semantic, pattern, and entity analysis algorithms
- Produces `Highlight` objects with confidence scores

### dsh-doc-indexer-worker

- Processes document indexing tasks asynchronously
- Integrates with Apache Solr via `dsh-solr` module for full-text search
- Updates document status in the persistence layer

### dsh-data

- Contains the shared models, the `DocumentDao` interface and its in-memory implementation
- Provides data access layer for other modules
- Manages database schema migrations
- Manages document status transitions. The state machine is drawn in
  [`document-status-workflow.md`](document-status-workflow.md), which is generated from
  `DocumentStatus` and checked by the build.

### dsh-solr

- Apache Solr configuration and integration utilities
- Manages Solr core configuration and indexing schemas

### dsh-coverage-report

- Aggregates JaCoCo code coverage reports across all modules
- Used in CI to enforce coverage thresholds

## Technology Stack

| Layer | Technology |
| ------- | ----------- |
| Language | Java 17 |
| Framework | Spring Boot, Spring Framework |
| Build | Maven (multi-module) |
| Persistence | In memory (default); Apache Solr (unused, removal decided) |
| CI | GitHub Actions |
| Testing | JUnit 5, Mockito, AssertJ |

## Cross-Cutting Concerns

- **Logging**: SLF4J with structured logging; correlation IDs per request
- **Validation**: Jakarta Bean Validation at API boundaries
- **Error Handling**: Global exception handler in `dsh-rest-api`

## Further Reading

- `../api/openapi/dsh-rest-api.yaml` – API contract
- `component-diagrams/` – Detailed component diagrams
- `sequence-diagrams/` – Process flow diagrams
- `data-models/` – Data model specifications
- `../features/` – Feature-level specifications

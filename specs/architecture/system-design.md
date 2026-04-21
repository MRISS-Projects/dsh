# DSH System Architecture

## Overview
Document Smart Highlights (DSH) is a multi-module Java/Spring Boot application that analyses documents and generates smart highlights based on configurable rules. The system is composed of loosely coupled services communicating via REST APIs and an asynchronous message/worker queue.

## System Components

```
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
     ┌──────────────┐  ┌──────────────┐
     │   MongoDB    │  │  dsh-solr    │
     │  (documents) │  │ (full-text   │
     │              │  │  indexing)   │
     └──────────────┘  └──────────────┘
```

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
- Contains shared JPA/MongoDB entity classes and repositories
- Provides data access layer for other modules
- Manages database schema migrations
- Manages document status transitions using as reference the flow 
  defined in `docs/wiki/Workflow.md`.

### dsh-solr
- Apache Solr configuration and integration utilities
- Manages Solr core configuration and indexing schemas

### dsh-coverage-report
- Aggregates JaCoCo code coverage reports across all modules
- Used in CI to enforce coverage thresholds

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 11+ |
| Framework | Spring Boot, Spring Framework |
| Build | Maven (multi-module) |
| Persistence | MongoDB, Apache Solr |
| CI | Travis CI (`.travis.yml`), GitHub Actions |
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

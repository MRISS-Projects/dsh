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
│                   DSH-rest-api                          │
│         Spring Boot REST API Service Layer              │
│   (routes requests, validation, authentication)        │
└────────────┬────────────────────────┬───────────────────┘
             │                        │
             ▼                        ▼
┌────────────────────┐   ┌────────────────────────────────┐
│  DSH-doc-analyser  │   │    DSH-doc-indexer-worker      │
│  Analysis Engine   │   │  Background Indexing Worker    │
│  (sync analysis)   │   │  (async document processing)   │
└────────────┬───────┘   └────────────┬───────────────────┘
             │                        │
             └────────────┬───────────┘
                          ▼
          ┌───────────────────────────────┐
          │          DSH-data             │
          │  Shared Data Models &         │
          │  Persistence Layer            │
          └───────────┬───────────────────┘
                      │
             ┌────────┴────────┐
             ▼                 ▼
     ┌──────────────┐  ┌──────────────┐
     │   MongoDB    │  │  DSH-SOLR    │
     │  (documents) │  │ (full-text   │
     │              │  │  indexing)   │
     └──────────────┘  └──────────────┘
```

## Module Responsibilities

### DSH-rest-api
- Exposes the public REST API (see `../api/openapi/dsh-rest-api.yaml`)
- Handles authentication, request validation, and routing
- Delegates business logic to `DSH-doc-analyser` and `DSH-doc-indexer-worker`

### DSH-doc-analyser
- Core document analysis engine
- Implements keyword, semantic, pattern, and entity analysis algorithms
- Produces `Highlight` objects with confidence scores

### DSH-doc-indexer-worker
- Processes document indexing tasks asynchronously
- Integrates with Apache Solr via `DSH-SOLR` module for full-text search
- Updates document status in the persistence layer

### DSH-data
- Contains shared JPA/MongoDB entity classes and repositories
- Provides data access layer for other modules
- Manages database schema migrations

### DSH-SOLR
- Apache Solr configuration and integration utilities
- Manages Solr core configuration and indexing schemas

### DSH-Coverage-Report
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
- **Error Handling**: Global exception handler in `DSH-rest-api`
- **Configuration**: Spring profiles (`dev`, `test`, `prod`)

## Further Reading

- `../api/openapi/dsh-rest-api.yaml` – API contract
- `component-diagrams/` – Detailed component diagrams
- `sequence-diagrams/` – Process flow diagrams
- `data-models/` – Data model specifications
- `../features/` – Feature-level specifications

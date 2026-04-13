# Data Models

This directory contains data model specifications for the DSH system.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Entity relationship diagrams and data model documentation |

## Suggested Models to Document

- `document-entity.md` – Document entity with all fields and constraints
- `highlight-entity.md` – Highlight entity with position and confidence data
- `analysis-result-entity.md` – Analysis result tracking entity
- `er-diagram.md` – Overall entity relationship diagram

## Guidelines

- Document all persistence entities (MongoDB documents, Solr schemas)
- Use Mermaid `erDiagram` syntax for ER diagrams
- Map each entity to its corresponding Java class in `dsh-data`
- Keep in sync with `../../api/schemas/` for API-facing models

## Relationship to Other Specs

- API schemas: `../../api/schemas/` – API-facing representation of these models
- OpenAPI spec: `../../api/openapi/dsh-rest-api.yaml` – Serialised form in responses
- Java entities: `dsh-data/src/main/java/` – Implementation

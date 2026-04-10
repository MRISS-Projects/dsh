# API Schemas

This directory contains reusable JSON Schema definitions for DSH data models.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Reusable schema fragments referenced from `../openapi/dsh-rest-api.yaml` |

## Usage

Schemas in this directory serve as the canonical data model definitions for the DSH system. They are referenced by:

- `../openapi/dsh-rest-api.yaml` – OpenAPI specification
- `../../architecture/data-models/` – Data model documentation
- Backend Java DTOs in `DSH-rest-api` and `DSH-data` modules

## Adding a New Schema

1. Create a `.json` file using JSON Schema Draft-07 or later
2. Reference it from the OpenAPI spec using `$ref`
3. Document it in the table above
4. Update the relevant feature spec in `../../features/`

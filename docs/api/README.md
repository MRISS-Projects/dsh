# API Documentation

This directory contains auto-generated API documentation for the DSH REST API.

## Contents

| File | Description |
|------|-------------|
| `index.html` | Redoc-generated HTML documentation (auto-generated, do not edit) |

## Generation

API documentation is automatically generated from the OpenAPI specification:
- **Source**: `/specs/api/openapi/dsh-rest-api.yaml`
- **Generator**: Redocly CLI (`redocly build-docs`)
- **Trigger**: Push to `develop` or `main` via `/.github/workflows/documentation-sync.yml`

## Local Generation

To generate documentation locally:
```bash
npm install -g @redocly/cli
redocly build-docs specs/api/openapi/dsh-rest-api.yaml --output docs/api/index.html
open docs/api/index.html
```

## Do Not Edit Manually
Files in this directory are auto-generated. Make changes in `/specs/api/openapi/dsh-rest-api.yaml` instead.

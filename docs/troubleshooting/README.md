# Troubleshooting Guides

This directory contains troubleshooting documentation for the DSH system.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Troubleshooting guides for common issues |

## Suggested Guides to Add

- `document-analysis-failures.md` – Common causes of analysis failures and fixes
- `solr-connection-issues.md` – Diagnosing and resolving Solr connectivity problems
- `mongodb-connection-issues.md` – MongoDB connectivity troubleshooting
- `performance-issues.md` – Diagnosing slow document processing
- `api-errors.md` – Common API error codes and their resolutions

## Diagnostic Tools

- **Health check**: `GET /api/v1/health` – verify the service is running
- **Application logs**: check `dsh-rest-api` and `dsh-doc-indexer-worker` logs
- **MongoDB**: use `connect-mongo.sh` script at the repository root
- **Solr**: check the Solr admin UI at `http://localhost:8983/solr`

## References
- Architecture: `/specs/architecture/system-design.md`
- User guides: `../user-guides/`

# Troubleshooting Guides

This directory contains troubleshooting documentation for the DSH system.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Troubleshooting guides for common issues |

## Suggested Guides to Add

- `document-analysis-failures.md` – Common causes of analysis failures and fixes
- `performance-issues.md` – Diagnosing slow document processing
- `api-errors.md` – Common API error codes and their resolutions

## Diagnostic Tools

- **Health check**: `GET /api/v1/health` – verify the service is running
- **Application logs**: check the `dsh-rest-api` logs; the pipeline's stages run inside it

## References

- Architecture: `/specs/architecture/system-design.md`
- User guides: `../user-guides/`

# API Postman Collections

This directory contains Postman collection files for API testing and exploration.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Postman collection JSON files for DSH API endpoints |

## Usage

Collections in this directory are used by:
- Manual API exploration and testing during development
- Automated API testing in the CI pipeline (see `/.github/workflows/api-testing.yml`)
- Onboarding new team members to the DSH API

## Running Collections

### Newman (CLI)
```bash
npm install -g newman
newman run <collection-file>.json --environment <env-file>.json
```

### Postman Desktop
Import the `.json` file directly into Postman.

## Adding a New Collection

1. Create and test the collection in Postman Desktop
2. Export as **Collection v2.1** format
3. Save the `.json` file in this directory
4. Update the table above
5. Verify it runs successfully with Newman locally before committing

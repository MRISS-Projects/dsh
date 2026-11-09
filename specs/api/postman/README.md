# API Postman Collections

Postman collections for the DSH REST API. They are layer 4 of the test layers in
`/.github/copilot/rules/testing-patterns.md`: a client that sees only the API contract,
`specs/api/openapi/dsh-rest-api.yaml`.

## Contents

| File | Description |
|------|-------------|
| `dsh-rest-api.postman_collection.json` | Submit a document and follow its status; an unknown token; the two rejected submissions |
| `local.postman_environment.json` | `baseUrl` for a server on `http://localhost:8080`, for Postman Desktop and manual runs |

## How the build runs them

`PostmanCollectionsIT` in `dsh-rest-api` runs every `*.postman_collection.json` here with newman,
under `-DintegrationTests`, against the server and RabbitMQ that the build starts:

```bash
mvn -B clean verify -DintegrationTests -pl dsh-rest-api
```

- The build installs Node and newman under `dsh-rest-api/target/newman/`. Neither has to be on the
  `PATH`. The build needs Docker, and network access to nodejs.org and registry.npmjs.org.
- It runs on every staging build, and in the local integration gate. Never on pull requests.
- The reports are in `dsh-rest-api/target/newman/reports/`: newman's output, JSON and JUnit XML,
  one of each per collection.
- A file whose name does not end in `.postman_collection.json` is not run.
- The build fails when this directory holds no collection, and when a collection runs no
  assertion.

## Writing a collection

1. Export it as **Collection v2.1**, or write it by hand, and name it `<name>.postman_collection.json`.
2. Use `{{baseUrl}}` for the server, and give it no value in the collection: the build passes it.
3. Assert what the OpenAPI document promises, with `pm.test`, never the code's internals.
4. Name a file to upload by its bare file name. The build resolves it against the PDF fixtures of
   `dsh-test-dataset` and reads no file outside them.
5. Run it under the command above before committing, and update the table above.

## Postman Desktop

Import the collection and `local.postman_environment.json`, and select the environment. Set
Postman's working directory (Settings, General) to `dsh-test-dataset/src/test/resources/pdf` in
this repository, so the upload finds its file.

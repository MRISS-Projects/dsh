# REST API Endpoints Specification

## Overview

The readable form of the OpenAPI document, `../api/openapi/dsh-rest-api.yaml`, which is the
contract. `dsh-rest-api` serves two operations, both in `DocumentResource`: one submits a document,
the other reads its status. An API change starts in the OpenAPI document, and this file follows it.

## Today

### Base URL and authentication

```text
http://localhost:8080
```

That is a local run of `dsh-rest-api`, which sets no port and no context path. No request is
authenticated.

### POST /v1/dsh/document/submit

Operation `submitDocument`. Stores a document and queues it for processing, and answers with the
token that identifies it.

**Request:** `multipart/form-data`, with two parts, both required:

- `title`: the document's title. It must not be blank. It is also accepted as a query parameter
  (`?title=`), beside a `contents` part, because the controller reads it with `@RequestParam`.
- `contents`: the document's file. Its type is not checked. It may be at most 1 MB, and the request
  at most 10 MB: Spring Boot's multipart defaults, which the application does not change.

```bash
curl -X POST http://localhost:8080/v1/dsh/document/submit \
  -F 'title=A title' -F 'contents=@bbc-news-1.pdf'
```

**Response `200`:** a `TokenDto`.

```json
{"token":"2026101461d279be-cc9e-40f3-a373-cd3fdabdcdf7","message":""}
```

- **`/submit` does not wait for the store or the enqueue.** Both run asynchronously and race the
  response, so `/status` can answer `TOKEN_NOT_FOUND` for a moment (from the code: in 20 tries a
  live run never saw it, because the store finished first), and a store that cannot be reached does
  not fail the submission.
- **Every submission gets a new token**, the same file included.
- **The token is opaque** to a client.

### GET /v1/dsh/document/status/{token}

Operation `getDocumentStatus`. Answers the status of the document a token identifies, and the
status's message.

```bash
curl http://localhost:8080/v1/dsh/document/status/2026101461d279be-cc9e-40f3-a373-cd3fdabdcdf7
```

**Response `200`:** a `DocumentStatusDto`.

```json
{"status":"QUEUED_FOR_INDEXING_SUCCESS","message":"Document id successfuly queued for indexing."}
```

`status` is `DocumentStatus`'s description (`dsh-data`). That is the status's name, except for
`NOT_INDEXED_ERROR`, which answers `DEQUEUED_FOR_INDEXING` (Wave 1 task 12). The statuses are listed
in `../architecture/document-status-workflow.md`. Today a document stops at
`QUEUED_FOR_INDEXING_SUCCESS` or `QUEUED_FOR_INDEXING_ERROR` (`./indexing-workflow.md`).

### Outcomes

Recorded against a running `dsh-rest-api` on 2026-10-14 (story `#162`,
`../stories/162-openapi-and-feature-specs-describe-the-api.md` §8), except where a row says
"(from the code)".

| Request | Code | Body |
|---|---|---|
| `/submit` with a title and a file | `200` | `TokenDto`: a token, `message` `""` |
| `/submit` with a blank title | `200` | `TokenDto`: `token` `ERROR`, `message` `Error submitting file: Document title and contents can't be null.` |
| `/submit`, and the controller catches any other exception (from the code) | `200` | `TokenDto`: `token` `ERROR`, `message` `Error submitting file: <reason>` |
| `/submit` without `title` or without `contents` | `400` | `ErrorBody` |
| `/submit` with a body that is not `multipart/form-data` | `400` | `ErrorBody` |
| `/submit` with a file over 1 MB, or a request over 10 MB | `500` | `ErrorBody`. Spring Boot's multipart defaults reject the upload before the controller runs |
| `/submit` while the document store cannot be reached | `200` | `TokenDto`: a token, `message` `""`. If the store is still unreachable when its client's 30-second timeout ends, the document is not stored, and `/status` then answers `TOKEN_NOT_FOUND` for that token |
| `/status/{token}` for a known token | `200` | `DocumentStatusDto`: the status and its message |
| `/status/{token}` for an unknown token | `200` | `DocumentStatusDto`: `status` `TOKEN_NOT_FOUND`, `message` `No token was found with the contents: <token>` |
| `/status/{token}` while the document store cannot be reached | `500` | `ErrorBody`, after about 30 seconds, the store client's own timeout |

`ErrorBody` is Spring Boot's default error body:

```json
{"timestamp":"2026-10-14T16:10:01.339+00:00","status":400,"error":"Bad Request","path":"/v1/dsh/document/submit"}
```

### Departures from the API standards

`../../.github/copilot/rules/api-standards.md` is the standard. The API does not meet it yet:

- Paths are under `/v1/dsh/document`, not `/api/v1`, and name actions (`submit`) rather than
  resources. No issue plans a change.
- A failure the controller handles answers `200`, with the failure in the body (`#138`).
- No request is authenticated. No issue plans authentication.
- Errors that reach Spring Boot answer its default error body, not the standard's.
- The standard says never to expose internal error details, but a rejected submission's `message`
  carries the caught exception's own message. No issue plans a change: `#138` changes the status
  codes, not the message.

### Outside this contract

`/actuator/health` and `/actuator/info` exist, served by Spring Boot Actuator. They are not
`DocumentResource`'s, and `#45` owns them.

## Planned

- **A results endpoint** returning a document's keywords and sentences by token, empty until Wave 3
  fills them. Its contract is written in the OpenAPI document first (Wave 2 task 2).
- **`4xx` codes** for the failures that answer `200` today (`#138`).
- **`/submit` stores the document and sends its id before it answers**, so a store failure reaches
  the client, and `/status` does not answer `TOKEN_NOT_FOUND` for a token `/submit` returned while
  the store still holds the document (Wave 2 task 1). The in-memory store of Wave 1 task 8 holds it
  only until the application stops.
- **`NOT_INDEXED_ERROR` answers a description of its own** (Wave 1 task 12).

## References

- The contract: `../api/openapi/dsh-rest-api.yaml`
- API standards: `../../.github/copilot/rules/api-standards.md`
- Document statuses: `../architecture/document-status-workflow.md`
- Document analysis: `./document-analysis.md`
- Indexing workflow: `./indexing-workflow.md`
- Smart highlighting: `./smart-highlighting.md`

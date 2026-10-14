# Indexing Workflow Specification

## Overview

The stages a document goes through after `/submit` accepts it, and the statuses that record them.
Today a document goes as far as one enqueue on RabbitMQ and stops there. What it is planned to go
through is decided by ADR-003 (`../architecture/ADR-003-document-pipeline.md`), and built in Waves
2 and 3 of the PRD (`../product/PRD.md`).

## Today

### The enqueue

After `/submit` has answered, `DocumentSubmissionServiceImpl.storeDocumentAndQueueForProcessing()`
runs asynchronously (`@Async`):

1. It advances the document from `CREATED` to `QUEUED_FOR_INDEXING` and stores it. That is the
   document's first store, so a client never reads `CREATED`.
2. It sends the document's id to RabbitMQ, on exchange `si.test.exchange` with routing key
   `si.test.binding`, bound to queue `si.test.queue` (`enqueue-docId-context.xml` in
   `dsh-rest-api`).
3. RabbitMQ's publisher confirm advances the document to `QUEUED_FOR_INDEXING_SUCCESS`, or a nack or
   a returned message advances it to `QUEUED_FOR_INDEXING_ERROR`, and the document is stored again.
4. If the send throws, the document is advanced to `QUEUED_FOR_INDEXING_ERROR` and stored.

A live run on 2026-10-14 read `QUEUED_FOR_INDEXING` at once and `QUEUED_FOR_INDEXING_SUCCESS` ten
seconds later (story `#162`, `../stories/162-openapi-and-feature-specs-describe-the-api.md` §8).

### Where it stops

Nothing consumes `si.test.queue`. `dsh-doc-indexer-worker` holds one class,
`DshDocIndexerApplication`, and no listener. So a document stops at `QUEUED_FOR_INDEXING_SUCCESS` or
`QUEUED_FOR_INDEXING_ERROR`, and nothing indexes it. None of the statuses after those two is ever
written. They are all listed, with their transitions, in
`../architecture/document-status-workflow.md`.

### Known defects

- **An enqueue's outcome can be applied to the wrong document** when submissions overlap: every
  in-flight handler receives every confirm (`#142`).
- **A store failure is not visible to the client** that submitted the document, because the store
  runs after the response. `/status` answers `500` for as long as the store cannot be reached.

## Planned

From ADR-003 section 1, "The flow", unless an item says otherwise.

- **One flow.** One Java DSL `IntegrationFlow` in `dsh-rest-api`, with the stages as its endpoints
  and two asynchronous hops: submit, hop 1, the indexing stage, hop 2, then the keywords and
  sentences stages. The message carries the document's id and nothing else (Wave 2 task 1).
- **One recorder of every status.** No stage writes a status. One component records every
  transition, by a compare-and-set advance of the stored status (Wave 2 task 1, on the conditional
  advance of Wave 1 task 9).
- **The statuses** are those of `../architecture/document-status-workflow.md`, and a document is
  carried through every one of them to `SENTENCES_PROCESSED_SUCCESS` (Wave 2 task 1).
- **`/submit` stores and sends before it answers**, and the `@Async` call is removed (Wave 2 task 1,
  ADR-003 section 1 "`/submit`").
- **Each document ends in its own status** when submissions overlap (Wave 2 task 1, which closes
  `#142`).
- **Failures** (Wave 2 task 4, ADR-003 section 1 "Failures"):
  - A stage's own failure is final. Its `…_ERROR` status is recorded at the first failure, and the
    message is consumed. It is not retried.
  - A store's failure is retried by redelivery. Nothing is recorded, the message is not consumed,
    and no message is lost. The number of deliveries is a setting of the transport (Wave 6).
  - A failed send on a hop records the hop's `…_ERROR` status.
- **Repeats** (Wave 2 task 4, ADR-003 section 1 "Compare-and-set"):
  - Delivery is at least once, so every stage is idempotent: running it twice for one document
    leaves the same result.
  - A repeat resumes from the document's status. It does not run again a stage the document is past.
  - When the sender has recorded a hop's error and the message arrives all the same, the first write
    wins.
- **The response does not wait for the processing.** `/submit` answers once the document is stored
  and its id sent on hop 1 (ADR-003 section 1, "`/submit`").
- **The indexing stage is text extraction**: PDF and HTML into title, paragraphs, sentences and
  terms, with the output kept in the file store (Wave 3 task 1, Wave 1 task 10).
- **Corpus term statistics replace a search engine**: term frequency per document and document
  frequency across the corpus, updated so that a document is counted once however often its stage
  runs (Wave 3 task 2, ADR-003 sections 5 and 6).
- **The pipeline diagram is generated from the running flow**, into
  `../architecture/document-pipeline-flow.md` (Wave 2 task 5).

## References

- ADR-003, the document pipeline: `../architecture/ADR-003-document-pipeline.md`
- Document statuses: `../architecture/document-status-workflow.md`
- The PRD's waves: `../product/PRD.md`
- Document analysis: `./document-analysis.md`
- REST API endpoints: `./rest-api-endpoints.md`

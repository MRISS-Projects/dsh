# Document Analysis Specification

## Overview

What happens to a document once it is submitted. DSH is planned to extract its text and to rank its
keywords and its top sentences, which are its smart highlights (`./smart-highlighting.md`). Today a
submitted document is stored and its id is queued, and no analysis runs.

## Today

### What `/submit` does with a document

1. It reads the file into the document, computes its SHA-256 hash, and gives it a new token. The
   file's type and size are not checked.
2. It answers with the token (`./rest-api-endpoints.md`).
3. After answering, asynchronously, it stores the document as `QUEUED_FOR_INDEXING` and sends its
   id to RabbitMQ. The enqueue's outcome gives `QUEUED_FOR_INDEXING_SUCCESS` or
   `QUEUED_FOR_INDEXING_ERROR` (`./indexing-workflow.md`).

The document is stored in MongoDB with the original file's bytes in the record.

### What does not happen

- **Nothing consumes the queue**, so a document is never analysed.
- **The analysis modules are not wired to anything.** `dsh-keyword-extractor`,
  `dsh-top-sentences-extractor` and `dsh-doc-processor-worker` each hold one `Application` class and
  no analysis code.
- **No text is extracted** from a file.

## Planned

- **One flow carries a document through every status** to `SENTENCES_PROCESSED_SUCCESS`, with
  pass-through stages at first (Wave 2 task 1, ADR-003 section 1).
- **MongoDB is replaced** by an in-memory `DocumentDao`, selected by `dsh.persistence` (Wave 1 task
  8), and the original file leaves the record for a file store, selected by `dsh.file-store` (Wave
  1 task 10).
- **The analysis modules become libraries** with no `Application` class, and
  `dsh-doc-processor-worker` is removed (Wave 2 task 3, ADR-003 section 2).
- **Text extraction** from PDF and HTML into title, paragraphs, sentences and terms (Wave 3 task 1).
- **Corpus term statistics**: term frequency per document and document frequency across the corpus
  (Wave 3 task 2, ADR-003 section 5).
- **TF and IDF variants**, as the keyword paper defines them (Wave 3 task 3).
- **Keyword ranking** by a meta-algorithmic combination of those variants, with a limit on the
  number of keywords returned (Wave 3 task 4).
- **Sentence similarity to the title**, by the sentence paper's algorithm (Wave 3 task 5).
- **Sentence ranking**, which returns the top sentences with their paragraph numbers, with a limit
  on the number returned (Wave 3 task 6).
- Every algorithm is written against the two reference papers (Wave 1 task 7), and each Wave 3
  story brings its expected-output fixtures.

## References

- ADR-003, the document pipeline: `../architecture/ADR-003-document-pipeline.md`
- Document statuses: `../architecture/document-status-workflow.md`
- The PRD's waves: `../product/PRD.md`
- The contract: `../api/openapi/dsh-rest-api.yaml`
- Indexing workflow: `./indexing-workflow.md`
- Smart highlighting: `./smart-highlighting.md`

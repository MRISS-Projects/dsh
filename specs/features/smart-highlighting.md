# Smart Highlighting Specification

## Overview

A document's smart highlights are its keywords and its top sentences, each ranked by a score. DSH
holds a place for both on every document. Nothing computes them yet, and no endpoint returns them.

## Today

### The model

`Document` (`dsh-data`) holds two sorted sets:

- `keyWords`, of `Keyword`: a `value` and a `score`.
- `relevantSentences`, of `Sentence`: a `value`, a `score` and the `paragraph` it comes from.

Both sets are ordered by score, highest first (`KeywordSorter`, `SentenceSorter`). The comparators
call two entries with the same score equal, so a set keeps only one of them (`#163`).

### What fills them, and what returns them

- **Nothing fills them.** No production code calls `Document.addKeyword` or `Document.addSentence`,
  and nothing analyses a document (`./document-analysis.md`).
- **No endpoint returns them.** `/status/{token}` answers a status and its message, and nothing else
  (`./rest-api-endpoints.md`).

## Planned

- **A results endpoint** returns a document's keywords and sentences by token. It returns empty
  lists until Wave 3 fills them, and its contract is written in the OpenAPI document first (Wave 2
  task 2).
- **Keywords** are ranked by a meta-algorithmic combination of TF and IDF variants, and a limit
  bounds the number returned (Wave 3 tasks 3 and 4).
- **Top sentences** are ranked by their similarity to the title combined with the terms' scores,
  and returned with their paragraph numbers, with a limit on the number returned (Wave 3 tasks 5
  and 6).

## References

- Document analysis: `./document-analysis.md`
- REST API endpoints: `./rest-api-endpoints.md`
- The contract: `../api/openapi/dsh-rest-api.yaml`
- The PRD's waves: `../product/PRD.md`

# Document Status Workflow

The statuses a document moves through in DSH, and the transitions between them. Each edge is
labelled with the `TransitionType` values that cause it. `[*]` marks where a document enters and
the statuses it never leaves.

This diagram is generated from `DocumentStatus` in `dsh-data`, not drawn.
`DocumentStatusDiagramTest` regenerates it on every build and fails when this file disagrees.
To update it, run `mvn -B -pl dsh-data test -Dtest=DocumentStatusDiagramTest`, and copy the block
from the failure message over the one below. Keep exactly one `mermaid` block in this file. The
test compares only that block, so the text around it can change freely.

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> QUEUED_FOR_INDEXING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_INDEXING --> QUEUED_FOR_INDEXING_SUCCESS : SUCCESS
    QUEUED_FOR_INDEXING --> QUEUED_FOR_INDEXING_ERROR : ERROR
    QUEUED_FOR_INDEXING_SUCCESS --> DEQUEUED_FOR_INDEXING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_INDEXING_ERROR --> [*]
    DEQUEUED_FOR_INDEXING --> INDEXING : SUCCESS, ERROR, NEUTRAL
    INDEXING --> INDEXED_SUCCESS : SUCCESS
    INDEXING --> NOT_INDEXED_ERROR : ERROR
    INDEXED_SUCCESS --> QUEUED_FOR_PROCESSING : SUCCESS, ERROR, NEUTRAL
    NOT_INDEXED_ERROR --> [*]
    QUEUED_FOR_PROCESSING --> QUEUED_FOR_PROCESSING_SUCCESS : SUCCESS
    QUEUED_FOR_PROCESSING --> QUEUED_FOR_PROCESSING_ERROR : ERROR
    QUEUED_FOR_PROCESSING_SUCCESS --> DEQUEUED_FOR_PROCESSING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_PROCESSING_ERROR --> [*]
    DEQUEUED_FOR_PROCESSING --> PROCESSING_KEYWORDS : SUCCESS, ERROR, NEUTRAL
    PROCESSING_KEYWORDS --> KEYWORDS_PROCESSED_SUCCESS : SUCCESS
    PROCESSING_KEYWORDS --> KEYWORDS_NOT_PROCESSED_ERROR : ERROR
    KEYWORDS_PROCESSED_SUCCESS --> PROCESSING_SENTENCES : SUCCESS, ERROR, NEUTRAL
    KEYWORDS_NOT_PROCESSED_ERROR --> [*]
    PROCESSING_SENTENCES --> SENTENCES_PROCESSED_SUCCESS : SUCCESS
    PROCESSING_SENTENCES --> SENTENCES_NOT_PROCESSED_ERROR : ERROR
    SENTENCES_PROCESSED_SUCCESS --> [*]
    SENTENCES_NOT_PROCESSED_ERROR --> [*]
```

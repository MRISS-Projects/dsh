# Welcome to DSH Document Indexer

## Document Smart Highlights Document Indexer

This module is a library: it has no `Application` class, and it does not run on its own. It will
hold the indexing stage of the document pipeline, which runs inside `dsh-rest-api`
([ADR-003](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-003-document-pipeline.md),
sections 1 and 2). The stage will:

1. Read the original file of a document.
2. Extract its text, and count the document's own term frequencies.
3. Store that output in the file store, so that a repeat replaces it.
4. Update the corpus term statistics that the keyword and sentence stages read.

It is empty until Wave 3. Until `#177` it was `dsh-doc-indexer-worker`, a Spring Boot application
that held only its main class.

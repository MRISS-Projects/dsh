# Welcome to DSH Document Processor Worker

## Document Smart Highlights Document Processor Worker

This module:

1. Dequeues a request for processing a document (typically a document id at
   SOLR + document id in the document store).
2. Invokes the keyword extractor.
3. Invokes the top sentences extractor.
4. Stores the result of extractions in the document's record in the document store.

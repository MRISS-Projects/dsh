# Welcome to DSH Document Processor Worker

## Document Smart Highlights Document Processor Worker

This module:

1. Dequeues a request for processing a document (typically a document id at
   SOLR + document id at MongoDB).
2. Invokes the keyword extractor.
3. Invokes the top sentences extractor.
4. Stores the result of extractions at the respective document line in the MongoDB collection.


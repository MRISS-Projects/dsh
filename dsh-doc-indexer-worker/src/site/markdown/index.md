# Welcome to DSH Document Indexer Worker

## Document Smart Highlights Document Indexer Worker

This module:

1. Dequeues a `documentId` from the queue.
2. Grabs document contents from MongoDB.
3. Stores an instance of that document at SOLR for automatic indexing/feature extraction.
   Important features are:
   - Relevant text content.
   - List of sentences.
   - List of paragraphs.
   - Bag of words.
4. Enqueues a new request for processing the indexed document by adding the SOLR document id
   and MongoDB document id to the queue.


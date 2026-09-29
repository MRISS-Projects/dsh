# Welcome to DSH

## Document Smart Highlights

Document Smart Highlights aims to provide web services that accept PDF and HTML files and return
a list of keywords and the most relevant sentences, extracted with NLP techniques; the sentences
through automatic document summarization.

## What DSH Does Today

Version 0.3.0 accepts a PDF document, stores it in a NoSQL store, puts its id on a queue for
indexing, and reports the document's processing status. Nothing consumes that queue yet, so 0.3.0
does not index documents and does not extract keywords or sentences. The REST API has two
operations: submit a document and receive a token, and ask for the status of the document a token
was issued for.

## Planned Process

The design DSH is being built towards. It is not the behaviour of 0.3.0, which covers the first two
steps of file submission and stops there.

### File Submission

A web service is established in a way that:

1. User uses a web service call to submit/upload a file.
   - System submits the file to process asynchronously and returns a token to the user.
2. User uses a web service call to keep probing the status of file processing, using
   the token previously returned.
3. When the file processing is done, the previous call returns success and user uses
   another call to retrieve the results having two parts:
   - A list of keywords
   - A list of most relevant sentences

   Retrieving the results is not available yet.

### File Indexing

1. The first processing task is to send the file to an indexer, which identifies the individual
   terms or tokens in the file's text.

### File Keyword Extraction

1. The keyword extraction is executed by setting scores to each term in
   the document's text. This is using a [TF/IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf)
   approach through several different implementations of TF and IDF, as shown at
   [this paper](https://doi.org/10.2352/issn.2168-3204.2017.1.0.105).

2. Those terms having the best scores obtained by a
   [meta-algorithmic](https://www.amazon.com/Meta-Algorithmics-Patterns-Robust-Quality-Systems/dp/1118343360)
   combination of all TF/IDF combinations, will then be returned as the best ranked keywords.

### Most Relevant Sentences Extraction

1. This is achieved applying typical [automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization)
   techniques like extractive summarization or
   [keyphrase extraction](https://en.wikipedia.org/wiki/Automatic_summarization#Keyphrase_extraction).

2. The sentence relevance is calculated taking into account two main criteria:
   - Similarity of the sentence with the title: using the algorithm provided from
     [this paper](https://doi.org/10.1016/j.csl.2016.01.003)
   - Multiple combinations of the TF/IDF of each term of the sentence, using the keyword
     extraction mechanism described above.

3. The sum of two criteria will define the score. The best scored sentences are returned
   along with their respective paragraph numbers.

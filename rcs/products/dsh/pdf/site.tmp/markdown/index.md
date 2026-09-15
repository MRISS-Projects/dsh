# Welcome to DSH

## Document Smart Highlights

Document Smart Highlights aims to provide a set of web services to allow uploading
of PDF and HTML files and return a list of keywords and most relevant sentences.
This system applies state of the art algorithms, using NLP techniques to produce
both the list of keywords and most relevant sentences. This last one, using
automatic document summarization techniques.

## Overall Process Description

The overall process of file submission and keywords/relevant sentence extraction is
described as follows:

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

### File Indexing

1. The first processing task is to submit files to a database where it will
   be indexed and processed to be able to identify individual terms or tokens
   inside the file context. This is done by submitting the files to be stored at
   a database provided by [Apache SOLR](http://lucene.apache.org/solr/).

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


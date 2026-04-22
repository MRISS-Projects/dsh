# Welcome to DSH Top Sentences Extractor

## Document Smart Highlights Top Sentences Extractor

The extraction of top sentences is achieved by applying typical
[automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization) techniques
like extractive summarization or
[keyphrase extraction](https://en.wikipedia.org/wiki/Automatic_summarization#Keyphrase_extraction).

The sentence relevance is calculated taking into account two main criteria:

- **Title similarity**: using the algorithm from [this paper](https://doi.org/10.1016/j.csl.2016.01.003).
- **TF/IDF combinations**: multiple combinations of the TF/IDF of each term in the sentence,
  using the top-sentences extraction mechanism described above.

The sum of the two criteria defines the score. The best scored sentences are returned
along with their respective paragraph numbers.


# Welcome to DSH Keyword Extractor

## Document Smart Highlights Keyword Extractor

The keyword extraction is executed by setting scores to each term in the document's text.
This uses a [TF/IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) approach through several
different implementations of TF and IDF, as shown in
[this paper](https://doi.org/10.2352/issn.2168-3204.2017.1.0.105).

Those terms having the best scores obtained by a
[meta-algorithmic](https://www.amazon.com/Meta-Algorithmics-Patterns-Robust-Quality-Systems/dp/1118343360)
combination of all TF/IDF combinations will then be returned as the best ranked keywords.


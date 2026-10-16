# Reference papers

Wave 3 of [the PRD](../product/PRD.md#wave-3--analysis-algorithms) writes DSH's analysis
algorithms from scratch against two papers. This record is the one place Wave 3's stories cite
them from. Each entry says whether the paper's full text is in this repository, and what decides
it. A paper whose licence does not allow redistribution is not committed: a placeholder stands in
its directory, and a copy you save there stays on your machine.

The licences were checked on 2026-10-16. Publishers change their policies; recheck the policy
before changing a decision here.

## Identifying top performing TF\*IDF classifiers using the CNN corpus

- **Authors:** A. Marie Vans, Steven J. Simske
- **Venue:** Archiving Conference, vol. 14, pp. 105–115, Society for Imaging Science and
  Technology (IS&T)
- **Year:** 2017
- **DOI:** [10.2352/issn.2168-3204.2017.1.0.105](https://doi.org/10.2352/issn.2168-3204.2017.1.0.105)
- **Used by:** Wave 3 task 3, TF and IDF variants, and task 4, keyword ranking
- **Full text in this repository:** no
- **Placeholder and local folder:**
  [`vans-2017-tf-idf-classifiers/`](vans-2017-tf-idf-classifiers/README.md)

**Why the full text is not here.** The
[article page](https://library.imaging.org/archiving/articles/14/1/art00023) lets anyone
download the PDF, but states only "Copyright © Society for Imaging Science and Technology 2017".
Neither that page nor the paper's Crossref record carries a licence. IS&T's Archiving copyright
form transfers copyright to IS&T, and the authors keep only the right to reuse the work in their
own future works. The 2017 form is no longer online; the
[2022 form](https://www.imaging.org/common/uploaded%20files/pdfs/Conferences/Archiving/2022/2022Archiving_Copyright.pdf)
is the nearest evidence. Free to read is not a licence to redistribute, so the paper is cited,
not committed.

**Getting a copy.** Follow the DOI to IS&T's library, use *Download PDF*, and save the file in
`vans-2017-tf-idf-classifiers/`.

## Assessing sentence similarity through lexical, syntactic and semantic analysis

- **Authors:** Rafael Ferreira, Rafael Dueire Lins, Steven J. Simske, Fred Freitas, Marcelo Riss
- **Venue:** Computer Speech & Language, vol. 39, pp. 1–28, Elsevier
- **Year:** 2016
- **DOI:** [10.1016/j.csl.2016.01.003](https://doi.org/10.1016/j.csl.2016.01.003)
- **Used by:** Wave 3 task 5, sentence similarity to the title
- **Full text in this repository:** no, neither the published version nor the accepted manuscript
- **Placeholder and local folder:**
  [`ferreira-2016-sentence-similarity/`](ferreira-2016-sentence-similarity/README.md)

**Why the published version is not here.** It is a subscription article. The only licence on its
Crossref record is Elsevier's text-and-data-mining licence, which grants no redistribution.
Elsevier's [sharing policy](https://www.elsevier.com/about/policies-and-standards/sharing) asks
its authors to "share a link to your article rather than the full-text".

**Why the accepted manuscript is not here.** The repository's owner is a co-author, so the same
policy's rules for an author's accepted manuscript apply. It may be shared immediately on the
author's non-commercial personal homepage or blog. It may be shared publicly after the journal's
embargo on non-commercial hosting platforms, such as an institutional repository, and on
commercial sites Elsevier has an agreement with. Every copy carries a CC BY-NC-ND licence and
links to the DOI. A public GitHub repository is none of these. The owner decided on 2026-10-16 to
keep the accepted manuscript out. The alternative was the reading that a homepage copy's
CC BY-NC-ND licence would itself allow it here, and Elsevier has not endorsed that reading. If the
owner posts it on a personal homepage, this entry will link to it.

**Getting a copy.** Follow the DOI to ScienceDirect, which needs a subscription. Or ask the
owner: Elsevier's policy lets authors give copies directly to research collaborators for their
personal use. Save the file in `ferreira-2016-sentence-similarity/`.

## How the folders work

- Git ignores everything inside a paper's directory except its `README.md`, by a rule in the
  root `.gitignore`. A copy of the paper saved there is never committed, whatever its format.
- Neither is any other file put there. Keep notes about a paper somewhere else.
- To add a paper, create `specs/references/<first-author>-<year>-<topic>/README.md` as its
  placeholder, and add an entry here. If its licence does allow the full text in this repository,
  add the file with `git add -f`, carrying the notice the licence requires, and say so in the
  entry.

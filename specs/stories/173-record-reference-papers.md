---
issue: 173
slug: record-reference-papers
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 173 — Record the reference papers Wave 3 is built from

## 1. Story

**As a** developer writing Wave 3's analysis algorithms from scratch
**I want** the papers those algorithms come from recorded in the repository: cited, with the full
text committed where its licence allows and kept out of git where it does not
**So that** every Wave 3 story can cite one source for the definitions it implements, and the
repository never redistributes a text it has no right to

## 2. Context

- Wave: 1, task 7 (`specs/product/PRD.md` §4, Wave 1, "Reference papers"), DSH milestone
  `0.4.0-SNAPSHOT`. It lands before `#148`, inside the wave, because Wave 3's stories cite the papers.
- Issue: [#173](https://github.com/MRISS-Projects/dsh/issues/173), labelled `task`.
- Parent branch: `DEVELOP`, at `f43fbebab` when this spec was written. The task branch is
  `issue-173-record-reference-papers`.

### 2.1 What exists today

- The papers are cited only by DOI links, in `src/site/markdown/index.md` (lines 47 and 61) and in
  `docs/wiki/Home.md` (lines 44 and 59). Both files are Wave 1 task 5's (`#44`), and out of scope.
- Wave 3's opening paragraph (`specs/product/PRD.md`, line 1397) reads "Everything here is written
  from scratch against the two papers (Wave 1 task 7)", and links nothing.
- `specs/references/` does not exist. `.gitignore` has no rule for it.
- `git ls-files '*.pdf'` lists four files, all fixtures in `dsh-test-dataset/src/test/resources/pdf/`
  (`The-Categories.pdf`, `bbc-news-1.pdf`, `edition.cnn.com-1.pdf`, `edition.cnn.com-2.pdf`). None
  is either paper.

### 2.2 What the licences say, checked on 2026-10-16

**The keyword paper.** A. Marie Vans and Steven J. Simske, *Identifying top performing TF\*IDF
classifiers using the CNN corpus*, Archiving Conference, vol. 14, pp. 105–115, Society for Imaging
Science and Technology (IS&T), 2017. DOI `10.2352/issn.2168-3204.2017.1.0.105`, which `doi.org`
redirects (302) to `https://library.imaging.org/archiving/articles/14/1/art00023`.

- The article page offers *Download PDF*, and the download answers `200 application/pdf` without
  a login: the paper is free to read.
- The page states only "Copyright © Society for Imaging Science and Technology 2017". Neither the
  page nor the paper's Crossref record carries a licence.
- IS&T's Archiving copyright form transfers copyright to IS&T. The authors keep the right to reuse
  the work, without fee, in their own future works. The 2017 form is no longer online (its URL
  answers 404); the [2022 form](https://www.imaging.org/common/uploaded%20files/pdfs/Conferences/Archiving/2022/2022Archiving_Copyright.pdf)
  is the nearest evidence. Neither author is this repository's owner.
- **Decision: not committed.** Free to read is not a licence to redistribute.

**The sentence paper.** Rafael Ferreira, Rafael Dueire Lins, Steven J. Simske, Fred Freitas and
Marcelo Riss, *Assessing sentence similarity through lexical, syntactic and semantic analysis*,
Computer Speech & Language, vol. 39, pp. 1–28, Elsevier, 2016. DOI `10.1016/j.csl.2016.01.003`,
which `doi.org` redirects (302) to `https://linkinghub.elsevier.com/retrieve/pii/S0885230816000048`.
Author order and both papers' volumes and pages are as Crossref lists them.

- **Published version.** A subscription article. The only licence on its Crossref record is
  Elsevier's text-and-data-mining user licence, which grants no redistribution. Elsevier's
  [sharing policy](https://www.elsevier.com/about/policies-and-standards/sharing) asks authors of a
  subscription article to "share a link to your article rather than the full-text". **Not
  committed.**
- **Accepted manuscript.** The same policy lets an author share it immediately "via their
  non-commercial personal homepage or blog". It lets them share it publicly only after the journal's
  embargo, on non-commercial hosting platforms such as an institutional repository, or on commercial
  sites Elsevier has an agreement with. Every shared copy carries a CC BY-NC-ND licence and links
  to the DOI. A public GitHub repository is none of these (`gh repo view` reports `PUBLIC`).
  **Not committed** (§3.1 decision 1).
- The same policy lets an author give copies "directly to students or research collaborators for
  their personal use". That is how a developer without a subscription gets the paper into the local
  folder.

### 2.3 Constraints found while designing

- **`git check-ignore -v` exits 0 for a negated match.** With `-v`, the command reports the rule that
  decided a path, and an `!` exception is such a rule, so a `README.md` that is *not* ignored still
  exits 0. Proved in a scratch repository: `git check-ignore -v specs/references/p1/README.md` printed
  `.gitignore:2:!specs/references/*/README.md` and exited 0, while `git check-ignore -q` on the same
  path exited 1. **Checks of "not ignored" use `git check-ignore -q` and expect exit 1.** `-v` is used
  only to show which rule ignored a file.
- The same scratch repository proved the rule of §3.2: `git add -A` staged `specs/references/README.md`
  and `specs/references/p1/README.md`, and nothing else. A `.pdf` and a `.docx` beside the placeholder
  were both ignored.

## 3. Design

### 3.1 Decisions

1. **The accepted manuscript stays out of the repository.** The owner decided this on 2026-10-16
   (brainstorming, option (a)). The other reading was considered and rejected: that a homepage copy's
   CC BY-NC-ND licence would itself allow anyone to redistribute it, so it could be committed (Josh
   Bolick, *Exploiting Elsevier's CC License Requirement to Subvert Embargo*, Journal of Copyright in
   Education and Librarianship, 2018). Elsevier has not endorsed it, and this repository is public.
   So AC004's conditional clause, which applies only if AC002 allows committing the accepted
   manuscript, does not apply: the sentence paper gets one placeholder covering both versions. If the
   owner posts the accepted manuscript on a personal homepage, the record links it then. That is not
   this story.
2. **One directory per paper** (brainstorming, option A). `specs/references/README.md` is the record.
   Each paper has a directory `specs/references/<first-author>-<year>-<topic>/`. Its `README.md` is
   the committed placeholder, and the directory is also the paper's gitignored local folder. The
   folder is where a developer browsing on GitHub expected the PDF, and the placeholder is what they
   find instead. The two directories are `vans-2017-tf-idf-classifiers/` and
   `ferreira-2016-sentence-similarity/`.
3. **The ignore rule ignores everything in a paper's directory except `README.md`**, not only
   `*.pdf`. A `.docx` or `.html` copy of a paper must not be committable either. The cost: any other
   file put in a paper's directory is silently ignored, and the record says so.
4. **The record carries the reasoning, the placeholder carries the pointer.** The placeholder gives
   the citation and DOI, one or two sentences on why the file is absent, how to get a copy, and a
   link back to the record. The policies, their quotes and the decisions are in the record only.
5. **Each decision is dated.** The record says the licences were checked on 2026-10-16. Publisher
   policies change, and an undated "not allowed" cannot be rechecked.
6. **"Used by" follows the issue.** The keyword paper: Wave 3 tasks 3 and 4. The sentence paper:
   Wave 3 task 5. Task 6 combines the outputs of tasks 3 and 5 and defines nothing from either
   paper, so it is not listed.
7. **The PRD changes in one sentence**, Wave 3's opening paragraph, which links the record. Wave 1
   task 7's status and issue number are `dsh-reconcile-prd`'s, at step 8.
8. **A paper whose licence allows its full text in the repository is force-added.** None does
   today. The record's "How the folders work" says how, in one line, so the rule does not block it
   by surprise: `git add -f`, with the notice its licence requires.

### 3.2 The ignore rule

Appended to `.gitignore`, after "Local build logs", in the file's section style:

```gitignore
# ----- Reference papers: full text stays local (see specs/references/README.md) -----
specs/references/*/*
!specs/references/*/README.md
```

It is anchored to `specs/references/`, deliberately without the `**/` prefix every other section
uses, because it applies to that one directory. A paper's directory itself is not ignored, only its
contents, which is why the `!` exception can take effect. A subdirectory inside a paper's directory
is ignored as a whole, `README.md` files inside it included.

### 3.3 Out of scope

- Committing the accepted manuscript, or linking a homepage copy of it (§3.1 decision 1).
- The *Meta-Algorithmics* book that Wave 3 task 4 also draws on. Left out by the owner's decision.
- `src/site/markdown/index.md` and `docs/wiki/Home.md`, including the wiki's malformed link to the
  book. Wave 1 task 5 (`#44`) rewrites both, and can point at the record then.
- Expected keywords or top sentences derived from the papers. Each Wave 3 task adds its own.
- Implementing or summarising the algorithms. The record cites; Wave 3 implements.

## 4. Verification design

The ignore rule is the only behaviour this story adds. It is checked red first: before the rule
exists, a file in a paper's directory is not ignored. The rest is checked by commands and by reading.

| AC | Check | Where |
|---|---|---|
| AC001 | Every DOI in the record and the placeholders is requested at `https://doi.org/`. Each answers 302 to its publisher's host. The fields are read entry by entry | Task 3 Steps 1 and 2 |
| AC002 | Each entry is read against the policy it links; the sentence paper's covers both versions | Task 3 Step 2 |
| AC003 | `git ls-files specs/references` lists exactly three `README.md`. `git ls-files '*.pdf'` lists only the four fixtures of §2.1 | Task 1 Step 6, Task 3 Step 3 |
| AC004 | Each placeholder contains its DOI link and a reason. The record links both placeholders | Task 1 Step 6 |
| AC005 | A throwaway `paper.pdf` in each paper's directory is ignored by the new rule. Each `README.md` is not ignored | Task 1 Steps 1, 3 and 6 |
| AC006 | A grep finds the record's link in Wave 3's opening paragraph | Task 2 Step 2 |
| AC007 | markdownlint exits 0. The diff against `DEVELOP` touches only `.gitignore`, `specs/references/`, the PRD and this spec | Task 3 Steps 3 and 4 |

## 5. Files to change

| File | Change |
|---|---|
| `.gitignore` | The section of §3.2 |
| `specs/references/README.md` | New: the record |
| `specs/references/vans-2017-tf-idf-classifiers/README.md` | New: the keyword paper's placeholder |
| `specs/references/ferreira-2016-sentence-similarity/README.md` | New: the sentence paper's placeholder |
| `specs/product/PRD.md` | Wave 3's opening paragraph links the record |
| `specs/stories/173-record-reference-papers.md` | This spec, §8 filled in |

Nothing else. No module's code or build changes.

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:executing-plans. Steps use checkbox
> (`- [ ]`) syntax. Tasks run in order.

**Goal:** the two papers are cited from one record under `specs/references/`, each with a committed
placeholder in a directory that git ignores everything else in, and Wave 3 links the record.

**Architecture:** three Markdown files, one `.gitignore` section, one sentence in the PRD. The
ignore rule is the only behaviour, and it is checked with `git check-ignore`, red first.

**Tech stack:** Git, Markdown, markdownlint, Maven for the gates.

**Spec:** this file.

### Global constraints

- Commands run in Git Bash from the repository root. markdownlint needs Node on `PATH`:
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"`. `markdownlint` is installed globally there.
- **A "not ignored" check is `git check-ignore -q <path>` expecting exit 1, never `-v`** (§2.3).
- The directory names are exactly `vans-2017-tf-idf-classifiers` and
  `ferreira-2016-sentence-similarity`.
- The DOIs are exactly `10.2352/issn.2168-3204.2017.1.0.105` and `10.1016/j.csl.2016.01.003`,
  linked as `https://doi.org/<doi>`.
- Every Maven run is logged to `.logs/` with its exit code reported (`CLAUDE.md`, "Always log local
  Maven runs").
- No PDF or other copy of either paper is ever staged. Run `git status --short` before each commit
  and read it.
- Commit messages are `docs(#173): …` and end with the session's attribution lines.

### Task 1: The ignore rule, the record and the placeholders

**Files:**

- Modify: `.gitignore` (append after the "Local build logs" section)
- Create: `specs/references/README.md`
- Create: `specs/references/vans-2017-tf-idf-classifiers/README.md`
- Create: `specs/references/ferreira-2016-sentence-similarity/README.md`

- [ ] **Step 1: Red. A file in a paper's directory is not ignored yet.**

  ```bash
  mkdir -p specs/references/vans-2017-tf-idf-classifiers specs/references/ferreira-2016-sentence-similarity
  for d in vans-2017-tf-idf-classifiers ferreira-2016-sentence-similarity; do
    touch "specs/references/$d/paper.pdf"
    git check-ignore -q "specs/references/$d/paper.pdf"; echo "exit=$? $d"
  done
  ```

  Expected: `exit=1` for both. Leave the two `paper.pdf` files in place for Step 3.

- [ ] **Step 2: Append the rule to `.gitignore`**, preceded by one blank line, exactly as §3.2 shows.

- [ ] **Step 3: Green. Each throwaway file is ignored by the new rule.**

  ```bash
  for d in vans-2017-tf-idf-classifiers ferreira-2016-sentence-similarity; do
    git check-ignore -v "specs/references/$d/paper.pdf"; echo "exit=$?"
  done
  ```

  Expected for each: `.gitignore:<n>:specs/references/*/*` followed by the path, and `exit=0`.

- [ ] **Step 4: Write the two placeholders.**

  `specs/references/vans-2017-tf-idf-classifiers/README.md`:

  ```markdown
  # Identifying top performing TF\*IDF classifiers using the CNN corpus

  A. Marie Vans and Steven J. Simske. Archiving Conference, vol. 14, pp. 105–115. Society for
  Imaging Science and Technology, 2017. DOI
  [10.2352/issn.2168-3204.2017.1.0.105](https://doi.org/10.2352/issn.2168-3204.2017.1.0.105).

  **The full text is not in this repository.** The Society for Imaging Science and Technology
  holds its copyright and publishes it under no licence that allows redistribution, although
  anyone can read it free on the
  [article page](https://library.imaging.org/archiving/articles/14/1/art00023).

  **To work with it locally,** download the PDF from that page and save it in this directory. Git
  ignores every file here except this one.

  The reasoning, and the other papers, are in [the reference record](../README.md).
  ```

  `specs/references/ferreira-2016-sentence-similarity/README.md`:

  ```markdown
  # Assessing sentence similarity through lexical, syntactic and semantic analysis

  Rafael Ferreira, Rafael Dueire Lins, Steven J. Simske, Fred Freitas and Marcelo Riss. Computer
  Speech & Language, vol. 39, pp. 1–28. Elsevier, 2016. DOI
  [10.1016/j.csl.2016.01.003](https://doi.org/10.1016/j.csl.2016.01.003).

  **The full text is not in this repository**, in either version. The published article is behind
  Elsevier's paywall, and Elsevier's
  [sharing policy](https://www.elsevier.com/about/policies-and-standards/sharing) asks authors to
  share a link to it rather than its full text. The same policy allows the authors' accepted
  manuscript on a personal homepage and on certain repositories, but not in a public GitHub
  repository.

  **To work with it locally,** get it through ScienceDirect with a subscription, by following the
  DOI, or ask the repository's owner, a co-author: Elsevier's policy lets authors give copies to
  research collaborators for their personal use. Save it in this directory. Git ignores every file
  here except this one.

  The reasoning, and the other papers, are in [the reference record](../README.md).
  ```

- [ ] **Step 5: Write the record, `specs/references/README.md`.**

  ```markdown
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
  ```

- [ ] **Step 6: AC003, AC004 and AC005 on the real tree.**

  ```bash
  for d in vans-2017-tf-idf-classifiers ferreira-2016-sentence-similarity; do
    git check-ignore -q "specs/references/$d/README.md"; echo "readme exit=$? $d"
  done
  git check-ignore -q specs/references/README.md; echo "record exit=$?"
  grep -c 'https://doi.org/10.2352/issn.2168-3204.2017.1.0.105' specs/references/vans-2017-tf-idf-classifiers/README.md
  grep -c 'https://doi.org/10.1016/j.csl.2016.01.003' specs/references/ferreira-2016-sentence-similarity/README.md
  grep -c -E '\((vans-2017-tf-idf-classifiers|ferreira-2016-sentence-similarity)/README\.md\)' specs/references/README.md
  git add .gitignore specs/references
  git status --short
  ```

  Expected: `readme exit=1` twice and `record exit=1`. The first two counts are `1`, the third is
  `2`. `git status --short` shows `M .gitignore` and `A` for exactly the three `README.md`; neither
  `paper.pdf` appears. Then remove the throwaway files and confirm they are gone:

  ```bash
  rm specs/references/*/paper.pdf
  git status --short --ignored specs/references
  ```

  Expected: only the three staged `README.md`, and no `!!` line.

- [ ] **Step 7: Commit.**

  ```bash
  git commit -m "docs(#173): record the reference papers and keep their full text local"
  ```

### Task 2: Wave 3 links the record

**Files:**

- Modify: `specs/product/PRD.md:1397`

- [ ] **Step 1: Edit Wave 3's opening paragraph.** The sentence sits in the middle of line 1397,
  after the milestone sentence. On that line, replace the phrase

  ```text
  against the two papers (Wave 1 task 7).
  ```

  with

  ```text
  against the two papers recorded in [`specs/references/README.md`](../references/README.md) (Wave 1 task 7).
  ```

  Change nothing else on the line, and add no line break.

- [ ] **Step 2: AC006.**

  ```bash
  sed -n '/^### Wave 3 /,/^\*\*Triaged issue/p' specs/product/PRD.md | grep -n 'references/README.md'
  ```

  Expected: one match.

- [ ] **Step 3: Commit.**

  ```bash
  git add specs/product/PRD.md
  git commit -m "docs(#173): link the reference record from Wave 3"
  ```

### Task 3: The gates and the acceptance checks

- [ ] **Step 1: AC001, every DOI resolves.**

  ```bash
  grep -ho 'https://doi.org/[^)]*' specs/references/README.md specs/references/*/README.md | sort -u |
  while read -r u; do curl -s -o /dev/null -w "%{http_code} %{redirect_url}  <- $u\n" "$u"; done
  ```

  Expected: exactly two lines, each `302`. The keyword DOI redirects to
  `https://library.imaging.org/archiving/articles/14/1/art00023`. The sentence DOI redirects to
  `https://linkinghub.elsevier.com/retrieve/pii/S0885230816000048`.

- [ ] **Step 2: AC001 and AC002, by reading.** Read each entry of the record. Each gives authors,
  title, venue, year, DOI and the Wave 3 tasks. Each says whether its full text is here, and links
  the evidence of §2.2. The sentence paper's entry covers the published version and the accepted
  manuscript separately. Open each linked policy page and confirm the quoted words are still on it.

- [ ] **Step 3: AC003 and AC007, the tree and the lint.**

  ```bash
  git ls-files specs/references
  git ls-files '*.pdf'
  git diff --stat DEVELOP
  PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --ignore 'docs/wiki/**' --config .markdownlint.json; echo "markdownlint exit=$?"
  ```

  Expected: three `README.md` under `specs/references`; the four fixture PDFs of §2.1 and nothing
  else; a diff touching only `.gitignore`, `specs/references/`, `specs/product/PRD.md` and this
  spec; `markdownlint exit=0`.

- [ ] **Step 4: Gates 1 and 2.** No code changes, but they run once, as `#162` did for its
  documentation-only change. Gate 3 does not apply: nothing touches an external system or an entry
  point.

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

  Expected: `maven exit=0`.

- [ ] **Step 5: Fill in §8 and commit.**

  ```bash
  git add specs/stories/173-record-reference-papers.md
  git commit -m "docs(#173): record the verification results"
  ```

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: one record lists both papers with authors, title, venue, year, DOI and Wave 3 tasks; every DOI resolves | §2.2, §3.1 decision 6, Task 1 Step 5, Task 3 Steps 1 and 2 |
| AC002: each paper's full-text decision and its deciding policy, linked; both versions of the sentence paper | §2.2, §3.1 decisions 1 and 5, Task 1 Step 5, Task 3 Step 2 |
| AC003: a file committed only where allowed; no other copy in `git ls-files` | §2.2 (neither is allowed), Task 1 Step 6, Task 3 Step 3 |
| AC004: a placeholder per uncommitted paper, with DOI link and reason; the record links each | §3.1 decisions 1, 2 and 4, Task 1 Steps 4 to 6 |
| AC005: a gitignored local folder per uncommitted paper, named by the record; `git check-ignore` reports a file there; the record says how to get the paper | §3.1 decisions 2 and 3, §3.2, Task 1 Steps 1, 3, 5 and 6 |
| AC006: Wave 3 links the record | §3.1 decision 7, Task 2 |
| AC007: markdownlint passes; no module's code or build changes | §5, Task 3 Steps 3 and 4 |

## 8. Verification results

Filled in by Task 1 and Task 3.

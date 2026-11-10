---
issue: 190
slug: readme-describes-dsh-today
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 190 — README describes DSH today: correct the storage sentence, and point the overview at the wiki

## 1. Story

**As a** person reading the README of a DSH release
**I want** it to say how that release keeps a document, and to send me to the wiki for the overview
**So that** the README does not describe a store the code no longer uses, and does not carry a
second overview that drifts from the wiki's Home

## 2. Context

- Wave: 1, a triaged issue, not an ADR-001 phase task (`specs/product/PRD.md` §4, Wave 1, the
  entry for `#190`). DSH milestone `0.4.0-SNAPSHOT`.
- Issue: [#190](https://github.com/MRISS-Projects/dsh/issues/190). Spun off from `#44`, decision
  D9 of `specs/stories/44-restructure-wiki.md`.
- Parent branch: `DEVELOP`, at `bf985a414` when this spec was written. The task branch is
  `issue-190-readme-describes-dsh-today`.
- It must land before the 0.4.0 release, which regenerates the root `README.md` from the source.

### 2.1 What exists today

- **The root `README.md` is generated, never edited.** The release process filters
  `src/site/markdown/README.md` into the root `README.md` and carries it to `DEVELOP` and `master`.
  `${project.build.version}`, `${release.type}` and `${issues.text.list}` are filled in then.
  Every change in this story goes into the source.
- **The Introduction of the source** (lines 9 to 46) has six parts: (a) what DSH aims to do;
  (b) "What 0.3.0 does today", which says the document is stored "in a NoSQL store"; (c) the
  distribution paragraph; (d) two goals, the first restating (a) and naming "a NoSQL store" again,
  the second the learning goal; (e) the note that DSH is an experiment, with the owner's contact
  email; (f) two link lines, the wiki and the development documentation.
- **The wiki's Home** (`docs/wiki/Home.md`, the source of the wiki since `#44`) is the one
  overview. It says "DSH accepts a PDF document, keeps it in a document store, in memory by
  default, puts the document's id on a queue for indexing". It carries neither (d) nor (e), and no
  other wiki page does.
- **The code on `DEVELOP`.** `InMemoryDocumentDao` is selected by `dsh.persistence=memory` with
  `matchIfMissing = true` (`#167`). No class under `dsh-*/src/main` receives from the queue: a grep
  for `RabbitListener`, `inbound-channel-adapter`, `message-driven-channel-adapter` and
  `Amqp.inbound` finds nothing.
- **"0.3.0" appears three times in the source**: lines 16 and 18, "**What 0.3.0 does today.**" and
  "so 0.3.0 does not index documents", and line 364,
  under Calling the API, "in 0.3.0 a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`". The 0.4.0
  release would copy all three into a README that describes itself as 0.3.0.
- **The root `README.md` still lists `dsh-solr`.** The source already describes it as removed by
  `#177`, so the next generation fixes it with no change here.
- **markdownlint does not cover `src/site/`.** The command in `CLAUDE.md` and in
  `spec-validation.yml` names `specs`, `.github`, `docs`, `CLAUDE.md` and `.claude`. The source
  passes markdownlint when it is named explicitly, checked when this spec was written.

## 3. Design

### 3.1 Decisions

D1 to D3 were made with the owner on 2026-11-09. D4 and D5 came from the local review (step 5)
and were approved by the owner on 2026-11-10.

- **D1. The Introduction keeps what belongs to a release, and points at Home for the rest.** Parts
  (a) and (d) go: Home is the overview, and goal 1 is the overview restated. A one-sentence pointer
  to Home takes their place, and (f) merges into it. Part (b) stays, corrected; it describes the
  release the README ships with. Part (c) stays unchanged. Part (e) stays, shortened, with the
  learning goal folded into it, because nothing else in the project's documentation carries the
  contact or the project's experimental status. Moving (d) and (e) into Home was rejected: it would
  edit the wiki and reopen what `#44` settled as Home's content.
- **D2. The prose names no version.** Line 16 becomes "**What this version does.**", line 18 says
  "it" in place of "0.3.0", and line 364 drops "in 0.3.0". The Version section right above
  already shows the version, from `${project.build.version}`. Filtering the version into the sentences was rejected, because
  `DEVELOP`'s site build would read "0.4.0-SNAPSHOT"; writing "0.4.0" by hand was rejected, because
  it claims behaviour for a release that does not exist yet, and Wave 2 is on the same milestone.
  When later work changes the behaviour, the story that changes it updates this text, as today.
- **D3. The storage wording follows Home.** "keeps it in a document store, in memory by default",
  so the README and the wiki say the same thing in the same words.
- **D4. The pointer names the wiki, and Home as where it starts.** "What DSH is, what it is planned
  to do and how it is built" spans two pages: Home holds the overview and the pipeline, and
  Architecture holds the modules and the components. So the sentence says they are described "in
  the project's wiki, starting from its Home page", and keeps its one link, to the wiki's root,
  which GitHub serves as Home.
- **D5. The status bullet says "indexing stage", not "indexing worker".** Its last sentence, "No
  later status is reached until the indexing worker exists", uses the wording `#177` and ADR-003
  retired: the workers became stages of the pipeline, and the module list of the same README
  calls `dsh-doc-indexer` the library that "will hold the indexing stage". The bullet is the one
  this story already edits, and leaving stale wording out of the 0.4.0 README is the story's
  point.

### 3.2 The text

Lines 11 to 46 of `src/site/markdown/README.md`, everything between `## Introduction` and
`## Package/Folders Description`, become exactly:

```markdown
Welcome to **DSH - Document Smart Highlights**. What DSH is, what it is planned to do and how it
is built are described once, in the [project's wiki](https://github.com/MRISS-Projects/dsh/wiki),
starting from its Home page. The development documentation is at
<https://mriss-projects.github.io/dsh-docs/>.

**What this version does.** It accepts a PDF document, keeps it in a document store, in memory by
default, puts the document's id on a queue for indexing, and reports the document's processing
status. Nothing takes the id off that queue yet, so it does not index documents and does not
extract keywords or sentences.

This project is distributed as source code; no binary distribution is provided yet. Building it
produces the REST API as an executable Spring Boot jar, which runs on its own, with no separate
server to install.

DSH is an experiment: a place to learn NLP, and how to build a scalable REST API with the
[Spring Framework](http://spring.io/). It is not a finished product. Comments and contributions
are welcome. To use it in a more product-oriented setting, write to <marcelo.riss@gmail.com>.
```

followed by one blank line before `## Package/Folders Description`.

Under `### Calling the API`, the status bullet's third line (D2) and last line (D5) change from

```markdown
  in 0.3.0 a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`: stored, and its id queued for
  token returns `TOKEN_NOT_FOUND`. No later status is reached until the indexing worker exists.
```

to

```markdown
  a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`: stored, and its id queued for
  token returns `TOKEN_NOT_FOUND`. No later status is reached until the indexing stage exists.
```

Nothing else in the file changes.

### 3.3 Out of scope

- The root `README.md`. The 0.4.0 release regenerates it, `dsh-solr` entry included.
- The wiki's pages, `src/site/markdown/index.md`, and every section of the source other than the
  Introduction and the one status bullet.
- Adding `src/site/` to the markdownlint scope of `CLAUDE.md` or `spec-validation.yml`.
- Wave 10 task 5, which rewrites the README as the user guide.
- Updating the PRD. Step 8, `dsh-reconcile-prd`, records the outcome.

## 4. Verification design

| AC | Check |
|---|---|
| AC001 | `grep -n -E 'NoSQL\|0\.3\.0' src/site/markdown/README.md` returns nothing |
| AC002 | The Introduction is the four paragraphs of §3.2, byte for byte; the first links `https://github.com/MRISS-Projects/dsh/wiki` |
| AC003 | The status bullet under Calling the API reads as §3.2 gives it; `grep -n 'indexing worker' src/site/markdown/README.md` returns nothing |
| AC004 | `git diff --stat DEVELOP` lists only `src/site/markdown/README.md` and this spec; the root `README.md` is unchanged |
| AC005 | markdownlint over `src/site/markdown/README.md` and over the `CLAUDE.md` scope exits 0 |
| Gate 1 | `mvn -B clean install` exits 0. Gate 3 does not apply: no code changes |

## 5. Files to change

| File | Change |
|---|---|
| `src/site/markdown/README.md` | The Introduction (§3.2, D1 to D4), and the status bullet (D2, D5) |
| `specs/stories/190-readme-describes-dsh-today.md` | This spec, and its verification results |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `dsh-build-story`, which applies
> superpowers:test-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** the README source says how this version keeps a document, names no version in its
prose, and points at the wiki's Home for the overview.

**Architecture:** a text edit to one Markdown file. The "test" is the grep of AC001, which fails
before the edit and passes after it.

**Tech stack:** GitHub-flavoured Markdown, markdownlint, Maven resource filtering at release.

**Spec:** this file, §1 to §5.

### Global constraints

- Edit `src/site/markdown/README.md` only. Never edit the root `README.md`.
- The text is §3.2's, verbatim. Leave `${...}` placeholders elsewhere in the file untouched.
- Shell commands run in Git Bash. markdownlint is run as
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint ...`; there is no other Node.
- One commit per task, message `<type>(#190): ...`, ending with the attribution lines the session
  gives.
- Never push, never open the PR, never close an issue: steps 5 and 6 are `dsh-ship-story`'s.

### Task 1: The README source

**Files:**

- Modify: `src/site/markdown/README.md:11-46` (the Introduction)
- Modify: `src/site/markdown/README.md:364` (the status bullet)

**Interfaces:** none. No other file reads this text; the release filters it into the root
`README.md`.

- [ ] **Step 1: Red.** Run
  `grep -n -E 'NoSQL|0\.3\.0' src/site/markdown/README.md; echo "grep exit=$?"`.
  Expected: four lines, 16, 18, 27 and 364, then `grep exit=0`.

- [ ] **Step 2: Replace the Introduction.** Replace everything between the `## Introduction`
  heading and the `## Package/Folders Description` heading with the block of §3.2, keeping one
  blank line after the heading and one before the next.

- [ ] **Step 3: Edit the status bullet.** Under `### Calling the API`, change
  `in 0.3.0 a document then ends at` to `a document then ends at`, as §3.2 shows.

- [ ] **Step 4: Green.** Run the grep of Step 1 again.
  Expected: no lines, then `grep exit=1`.

- [ ] **Step 5: Lint.**
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint src/site/markdown/README.md --config .markdownlint.json; echo "lint exit=$?"`.
  Expected: `lint exit=0`.

- [ ] **Step 6: Read it back.** `sed -n 9,30p src/site/markdown/README.md` shows the heading, the
  four paragraphs of §3.2, and `## Package/Folders Description`. `git diff DEVELOP --
  src/site/markdown/README.md` touches only the Introduction and the one bullet line.

- [ ] **Step 7: Commit.**

```bash
git add src/site/markdown/README.md
git commit -m "docs(#190): describe the in-memory store, and point the overview at the wiki"
```

### Task 2: The acceptance checks and the gate

- [ ] **Step 1: AC004.** `git diff --stat DEVELOP` lists `src/site/markdown/README.md` and
  `specs/stories/190-readme-describes-dsh-today.md`, nothing else.

- [ ] **Step 2: Lint, as `CLAUDE.md` gives it.**
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --config .markdownlint.json; echo "lint exit=$?"`.
  Expected: `lint exit=0`; this covers the spec.

- [ ] **Step 3: Gate 1.** `mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as
  `CLAUDE.md` prescribes, with its exit code reported. No Java changes, but the gate is the
  story's.

- [ ] **Step 4: Record the results in §8**, and commit:
  `docs(#190): record the verification results`.

### Task 3: The local review's fixes (D4, D5)

Added after the local review of step 5, on 2026-11-10.

**Files:**

- Modify: `src/site/markdown/README.md:11-13` (the Introduction's first paragraph)
- Modify: `src/site/markdown/README.md:346` (the status bullet's last line)

- [ ] **Step 1: Red.** Run
  `grep -n -E "wiki's Home page\]|indexing worker" src/site/markdown/README.md; echo "grep exit=$?"`.
  Expected: two lines, 12 and 346, then `grep exit=0`.

- [ ] **Step 2: Replace the Introduction's first paragraph** with the first paragraph of §3.2's
  block, which is now four lines long.

- [ ] **Step 3: Edit the status bullet's last line**, changing `indexing worker` to
  `indexing stage`, as §3.2 shows.

- [ ] **Step 4: Green.** Run the grep of Step 1 again. Expected: no lines, then `grep exit=1`.
  Then the AC002 check: the Introduction, now lines 11 to 27, is identical by `diff` to §3.2's
  block, and the grep of Task 1, Step 1, still finds nothing.

- [ ] **Step 5: Lint** the source, as in Task 1, Step 5, and the `CLAUDE.md` scope, as in Task 2,
  Step 2. Expected: `lint exit=0` for each.

- [ ] **Step 6: Commit.**

```bash
git add src/site/markdown/README.md specs/stories/190-readme-describes-dsh-today.md
git commit -m "docs(#190): point at the wiki from its Home, and say indexing stage"
```

- [ ] **Step 7: Gate 1 again, AC004 again, and the results in §8.1**, then commit:
  `docs(#190): record the review round's verification results`.

## 7. Acceptance criteria

| AC | Where it is met |
|---|---|
| AC001: the source names no NoSQL store and no 0.3.0 | Task 1, Steps 2 and 3; checked by Task 1, Step 4 |
| AC002: the Introduction is §3.2's text, pointing at the wiki's Home | Task 1, Step 2, and Task 3, Step 2; checked by Task 3, Step 4 |
| AC003: the status bullet names no version and no worker | Task 1, Step 3, and Task 3, Step 3; checked by Task 3, Step 4 |
| AC004: the root `README.md` is unchanged | Task 2, Step 1 |
| AC005: markdownlint passes | Task 1, Step 5; Task 2, Step 2 |

## 8. Verification results

Run on 2026-11-09, on the task branch after Task 1's commit `99842e694`.

| Check | Result |
|---|---|
| Red, Task 1 Step 1 | The grep found four lines, 16, 18, 27 and 364; `grep exit=0` |
| AC001, green | The same grep found nothing; `grep exit=1` |
| AC002 | Lines 11 to 26 of the source are identical to §3.2's block, by `diff`; the first paragraph links `https://github.com/MRISS-Projects/dsh/wiki` |
| AC003 | The status bullet reads "a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`", as §3.2 gives it; `git diff DEVELOP` changes that one line in it and nothing else outside the Introduction |
| AC004 | `git diff --stat DEVELOP` lists `specs/stories/190-readme-describes-dsh-today.md` and `src/site/markdown/README.md`, nothing else; the root `README.md` is unchanged |
| AC005 | markdownlint over the source: `lint exit=0`. Over the `CLAUDE.md` scope: `lint exit=0` |
| Gate 1 | `mvn -B clean install`, logged to `.logs/mvn-clean-install.log`: `maven exit=0`, `BUILD SUCCESS`, every module of the reactor `SUCCESS`. 71 tests in `dsh-data` and 43 in `dsh-rest-api`, none failing or skipped. "All coverage checks have been met" for both |
| Gate 3 | Not run: no code changed |

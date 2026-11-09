---
issue: 44
slug: restructure-wiki
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 44 — Restructure the wiki: overview, architecture and developer pages

## 1. Story

**As a** person new to DSH, whether a contributor or someone evaluating it
**I want** the wiki to say once what DSH does, how it is shaped and which parts are built
**So that** I understand the system without reading three ADRs, and without meeting two
overviews that disagree

## 2. Context

- Wave: 1, task 5, "Wiki restructure" (`specs/product/PRD.md` §4, Wave 1), DSH milestone
  `0.4.0-SNAPSHOT`. The PRD's order for the wave (step 8) puts it before task 6, the Spring
  Integration page, and task 4, the documentation map. Task 4's reachability check needs this
  story's AC007 answer.
- Issue: [#44](https://github.com/MRISS-Projects/dsh/issues/44), labelled `task`.
- Parent branch: `DEVELOP`, at `62c1bea0e` when this spec was written. The task branch is
  `issue-44-restructure-wiki`.
- Inputs: ADR-003 sections 1, 2 and 4 for the pipeline and the modules; ADR-001 and ADR-004 for
  the products the generic terms stand for. The three patterns come from Pivotal Labs' "Agile
  Architecture" (2017), now at <https://blogs.vmware.com/tanzu/agile-architecture/>.

### 2.1 What exists today

- **The wiki** (`MRISS-Projects/dsh.wiki.git`, checked by a clone on 2026-11-08) has three pages:
  `Home`, `Workflow` and `Code-Based-Site-and-Reports`. No Architecture page, no Developers page,
  no sidebar. `docs/wiki/` holds the same three pages byte for byte, each behind a one-line
  `AUTO-GENERATED` header, plus `docs/wiki/README.md`.
- **Home** is the overview from before 0.3.0. It names Apache Solr as the indexer, which `#177`
  removed, and carries a malformed link, `({https://www.amazon.com/...`.
- **`src/site/markdown/index.md`** carries a second, newer overview: what DSH aims to do, "What
  DSH Does Today", and the planned process. It says 0.3.0 "stores it in a NoSQL store". On
  `DEVELOP` the document is kept in memory by default (`#167`), so that sentence no longer
  describes the code the wiki is published from.
- **`wiki-sync.yml`** copies the wiki into `docs/wiki/` daily, by a pull request. Its cron fires
  on `master`, and a `redispatch` job re-runs it on `DEVELOP`. It is active today (`gh workflow
  list`), and ran successfully on 2026-11-08. GitHub disabled it for inactivity from 2026-07-21 to
  2026-10-06, and will do so again 60 days after the last activity on `master`. The last commit
  on `master` is from 2026-10-15.
- **markdownlint ignores `docs/wiki/**`**, in `CLAUDE.md`, `spec-validation.yml` and
  `dsh-reconcile-prd`. Linting the directory today reports 37 errors: 33 in `Home.md`
  (indentation, trailing spaces), 3 bare URLs in `Code-Based-Site-and-Reports.md`, and one fenced
  block with no language in `README.md`. `Workflow.md` is clean.
- **Other documents that describe the sync direction:** `docs/wiki/README.md`, the "Wiki
  References" section of `.github/copilot-instructions.md`, `docs/devops/README.md` (the Pipeline
  Map, the Workflow Reference table, and the sentence on secrets at line 277), and
  `docs/copilot/development-guide.md` (lines 48 to 57, whose example also names a wiki page,
  `Architecture-Decisions.md`, that never existed).
- **The code on `DEVELOP`.** Two modules hold production code: `dsh-rest-api` and `dsh-data`.
  `dsh-doc-indexer`, `dsh-keyword-extractor` and `dsh-top-sentences-extractor` exist as modules
  with no production class. `DocumentResource` serves `POST /v1/dsh/document/submit` and
  `GET /v1/dsh/document/status/{token}`. `/submit` stores the document and sends its id through
  an outbound AMQP adapter (`enqueue-docId-context.xml`). Nothing receives from that queue: no
  inbound adapter, no listener.
- **The root `README.md`** carries an overview of its own, and its module list still names
  `dsh-solr`. The issue does not name README. It is left to a follow-up issue (§3.1, D9).

### 2.2 Constraints found while designing

- **A new workflow cannot be dispatched before it reaches `master`.** GitHub dispatches a
  workflow only if its file exists on the default branch (`docs/devops/README.md`, `deploy.yml`'s
  row). A push trigger reads the file from the pushed branch, so the merge to `DEVELOP` is the
  publisher's first real run.
- **`GITHUB_TOKEN` pushing to the wiki.** Third-party sources say a workflow's own token with
  `contents: write` can push to the repository's `.wiki.git`, for example vscode-go's
  [`wiki.yml`](https://go.googlesource.com/vscode-go/+/HEAD/.github/workflows/wiki.yml) and
  [github-wiki-action](https://github.com/Andrew-Chen-Wang/github-wiki-action). GitHub's own
  documentation does not say so for wikis. The merge's run settles it (§3.2).
- **Deleting `wiki-sync.yml` on `DEVELOP` leaves it on `master`** until the next release. Its
  cron keeps firing there, and its `redispatch` job would ask for a run on `DEVELOP` of a file that
  no longer exists, failing every day. It is disabled at the merge (§3.2).
- **A wiki page cannot resolve a repository path**, and repository links to wiki pages are
  absolute. Links between wiki pages use the wiki's form.
- **`mmdc` is not installed here** (no Node package beyond markdownlint, by the box's rules).
  Mermaid diagrams are checked by GitHub's renderer, on the pushed branch.

## 3. Design

### 3.1 Decisions

- **D1. The wiki's source moves into `docs/wiki/` (AC007).** Decided with the owner on
  2026-11-08. Reasons:
  - the pages get review and markdownlint, which matters most for the hand-drawn diagrams this
    story adds;
  - GitHub disables only scheduled workflows for inactivity, so a push-triggered publisher cannot
    lapse the way `wiki-sync.yml` does, and no issue is needed to keep it alive;
  - no sync pull request to merge after every wiki edit (`#128`, `#152` and `#154` each needed
    one).

  The cost: an edit made on github.com is overwritten by the next publish. The owner turns on the
  wiki setting "Restrict editing to collaborators only" (§3.2), and `docs/wiki/README.md` says
  so.
- **D2. The wiki publishes from `DEVELOP`.** It describes what is built on `DEVELOP`, as AC001
  asks, and is updated by every merge that touches it. Publishing from `master` would need the
  release workflows, which belong to parent-poms.
- **D3. A tested script and a thin workflow.** `.github/scripts/publish-wiki.sh` mirrors the
  pages into a clone of the wiki. `.github/scripts/publish-wiki.test.sh` tests it, in the style
  of `check-spec-references.test.sh`: plain bash, no framework. `wiki-publish.yml` clones, runs
  the script, commits and pushes. A marketplace action was rejected: it would hold a write token,
  and leave nothing of ours to test.
- **D4. Mirror semantics.** Every top-level `*.md` in `docs/wiki/` except `README.md` is a page.
  A top-level `*.md` in the wiki with no source is deleted. Anything else in the wiki, `.git` and
  any file that is not Markdown, is left alone; there are none today. A source with no page at
  all is an error, because mirroring it would delete every page.
- **D5. Links.** Between wiki pages, the wiki's form: `[Architecture](Architecture)`. To a
  repository file, absolute, to `https://github.com/MRISS-Projects/dsh/blob/DEVELOP/...`.
- **D6. What counts as built (AC001, AC003).** A stage or component is built when code on
  `DEVELOP` does its job, whatever its shape. `/submit` does the gateway's job: it stores the
  document and sends its id on hop 1. It is not yet the `IntegrationFlow` gateway of ADR-003,
  which Wave 2 task 1 builds, but the diagram shows stages, not their implementation. Hop 1's send
  is built. Its receive is not, so the edge from hop 1 to indexing is planned. A module that
  exists but holds no production class is planned.
- **D7. How "planned" is shown.** Each planned node carries "(planned)" in its label, and a
  dashed outline through a `classDef`. Each edge into a planned node is dashed. A planned edge
  between two built nodes, such as a hop not yet received, is dashed and carries "(planned)" in
  its label. The label is the marking; the dashes repeat it, so it does not depend on colour or on
  how a theme draws dashes.
- **D8. Generic names on Home, Architecture and `index.md`.** Queue, NoSQL store, file store,
  document store. Module names are names in the build, not products, and stay: `dsh-gcp` is the
  planned module of ADR-003 section 2. Developers and the two existing pages may name products.
- **D9. The root `README.md` is a follow-up issue**, decided with the owner on 2026-11-08: its
  stale `dsh-solr` entry, its "NoSQL store" sentence, and trimming its overview to a pointer at
  Home. It is drafted during the build, opened after the owner approves its text, and placed in
  the PRD by `dsh-reconcile-prd`.
- **D10. `index.md` holds no overview.** AC005 allows one paragraph. A one-sentence summary of
  DSH would be the overview's text in a second place, so the paragraph says what the site is and
  where the overview lives.
- **D11. The two existing pages change only where markdownlint requires it.** `Workflow.md` loses
  its header. `Code-Based-Site-and-Reports.md` loses its header and gets its three bare URLs
  wrapped in `<>`. Their content is out of scope.
- **D12. The publisher's test also runs in `spec-validation.yml`**, as a job of its own, so a
  pull request that breaks the script fails before the merge. `spec-validation.yml` already
  triggers on `.github/scripts/**`.

### 3.2 The publisher

`wiki-publish.yml` runs on a push to `DEVELOP` that changes `docs/wiki/**`, the script, its test
or the workflow, and on `workflow_dispatch`. One job, guarded by `github.ref ==
'refs/heads/DEVELOP'`, with `contents: write` and `concurrency: wiki-publish` so that two merges
cannot race. It runs the test, clones the wiki with `GITHUB_TOKEN`, runs the script, and commits
"Publish docs/wiki from MRISS-Projects/dsh@`<sha>`" only when something changed.

**At the merge**, the owner and Claude do three things, in this order:

1. Claude runs `gh workflow disable wiki-sync.yml`, after asking: it changes GitHub. That stops
   `master`'s copy from firing until the next release removes it.
2. The merge's push runs `wiki-publish.yml`. Claude reads the run. A `403` on the push means the
   token cannot write to the wiki; one follow-up commit then switches the clone to
   `secrets.DEPLOY_TOKEN`, which other workflows here already use to push, and the run is repeated.
3. The owner turns on "Restrict editing to collaborators only" under Settings → Features → Wikis.
   Claude cannot change that setting.

### 3.3 The pages

**`Home.md`.** What DSH aims to do; what it does today on `DEVELOP`, in generic terms; the
pipeline diagram; the planned processing (submission, indexing, keywords, sentences), moved from
`index.md` and brought in line with ADR-003 (indexing is text and term extraction inside the
application, and the update of the statistics of terms across documents); links to the other
four pages. The full text is in Task 3.

The pipeline diagram, as it renders:

```mermaid
flowchart LR
    client([Client]) -->|/submit| gateway[Gateway]
    gateway --> hop1[(Hop 1<br/>queue)]
    hop1 -.-> indexing["Indexing<br/>(planned)"]
    indexing -.-> hop2[("Hop 2<br/>queue<br/>(planned)")]
    hop2 -.-> keywords["Keywords<br/>(planned)"]
    hop2 -.-> sentences["Sentences<br/>(planned)"]
    classDef planned stroke-dasharray: 5 5
    class indexing,hop2,keywords,sentences planned
```

**`Architecture.md`.** The three patterns, each with the part of DSH it covers:

- **Request/Response**: the REST API, `/submit` and `/status/{token}`. The client asks and
  waits, polls the status, and retries on its own.
- **Event Driven**: the pipeline. Each stage reacts to a message carrying only the document's id,
  and its result moves the document on. Redelivery after a failure is the system's job.
- **Batch**: DSH has no Batch part today, and the PRD plans none (`grep -n -i batch
  specs/product/PRD.md` returns nothing on 2026-11-08).

The components diagram, as it renders:

```mermaid
flowchart TB
    client([Client]) -->|HTTP| rest["dsh-rest-api<br/>REST API"]
    rest --> data["dsh-data<br/>models, store interfaces,<br/>in-memory document store"]
    rest -->|"hop 1: send"| queue[(Queue)]
    queue -.->|"hops 1 and 2: receive<br/>(planned)"| rest
    rest -.->|"hop 2: send<br/>(planned)"| queue
    rest -.-> indexer["dsh-doc-indexer<br/>indexing stage<br/>(planned)"]
    subgraph analyser["dsh-doc-analyser"]
        keywords["dsh-keyword-extractor<br/>keywords stage<br/>(planned)"]
        sentences["dsh-top-sentences-extractor<br/>sentences stage<br/>(planned)"]
    end
    rest -.-> keywords
    rest -.-> sentences
    rest -.-> provider["dsh-gcp<br/>cloud adapters<br/>(planned)"]
    provider -.-> nosql[("NoSQL store<br/>(planned)")]
    provider -.-> files[("File store<br/>(planned)")]
    classDef planned stroke-dasharray: 5 5
    class indexer,keywords,sentences,provider,nosql,files planned
```

Every edge to or from the queue is `dsh-rest-api`'s, and they are labelled by hop, so the two
diagrams line up: Home shows which stage writes and reads each hop, and this one shows which
module does it. The stage modules have no edge to the queue, because a stage imports nothing from
Spring Integration (ADR-003 §1, "A stage"): the flow in `dsh-rest-api` receives from a hop, calls
the stage's library, and sends on the next hop. Hop 1's send is the only part built; the other
three directions are dashed and labelled planned. Decided with the owner on 2026-11-09, over
drawing edges from the stage modules, which would contradict ADR-003.

No edge runs from `dsh-gcp` to the queue. Which adapter the application uses, today's or the cloud
provider's in `dsh-gcp`, is selected inside the application (ADR-003 section 3), so such an edge
would draw one queue with two ways in. The NoSQL store and the file store are drawn from
`dsh-gcp`, because nothing reaches them except through that planned module.

`dsh-test-dataset` and `dsh-coverage-report` are left out: they hold no production code. The page
links ADR-001, ADR-003 and ADR-004 for the decisions, and `system-design.md` for today's detail.

**`Developers.md`.** Start at `CLAUDE.md`, linked. How this wiki is edited. Links to Workflow and
Code-Based-Site-and-Reports. No build command, no branch rule.

**`src/site/markdown/index.md`.** One paragraph saying what the site is and that the wiki holds
the overview, then links: the wiki's Home, Architecture, the repository, the project information.

### 3.4 Out of scope

- Wave 1 task 6, the wiki page on Spring Integration and its flow diagram.
- Wave 1 task 4, the CI check that every Markdown file is reachable from `CLAUDE.md`.
- Wave 2 task 5, generating the pipeline diagram from the flow.
- The content of Workflow and Code-Based-Site-and-Reports (D11).
- The root `README.md` (D9).
- Any change to code, the ADRs or `specs/architecture/system-design.md`.
- `documentation-sync.yml`'s `docs/wiki/**` trigger. It starts two jobs that do not read
  `docs/wiki/`, which was already true, and harms nothing.
- Updating the PRD. Step 8, `dsh-reconcile-prd`, records the story's outcome, the follow-up issue
  and the end of the "`wiki-sync.yml` will be disabled again" note in Wave 1 task 1.

## 4. Verification design

| AC | Check |
|---|---|
| AC001 | Read `Home.md`'s diagram against ADR-003 §1 "The flow at run time" (six stages, in order) and against the code at merge: run the inbound-receiver grep of Task 5 and list `src/main/java` of the three stage modules. Every planned label matches a stage with no code |
| AC002 | `Architecture.md` has one section per pattern, Batch included, naming the REST API's two routes and the pipeline |
| AC003 | The components diagram has every module with production code (`find . -path '*/src/main/java/*.java' -not -path '*/target/*'`), the planned modules, the queue, the NoSQL store and the file store, and the page links the three ADRs |
| AC004 | `grep -n -E 'mvn\|git \|branch' docs/wiki/Developers.md` returns nothing, and the page links `CLAUDE.md` |
| AC005 | `index.md` is one paragraph and a list of links, one of them the wiki's Home; no sentence of Home's overview appears in it |
| AC006 | `grep -n -i -E 'rabbit\|firestore\|pub/?sub\|cloud storage\|mongo\|solr' docs/wiki/Home.md docs/wiki/Architecture.md src/site/markdown/index.md` returns nothing |
| AC007 | `publish-wiki.test.sh` passes; `wiki-sync.yml` is gone; `grep -rn "docs/wiki/\*\*" CLAUDE.md .github/workflows/spec-validation.yml .claude/skills/dsh-reconcile-prd/SKILL.md` returns nothing; the markdownlint command in `CLAUDE.md` exits 0; a dry run of the script against a scratch clone of the wiki shows the intended pages; after the merge, the wiki shows five pages, and its Home matches `docs/wiki/Home.md` |
| Diagrams | After the branch is pushed, both diagrams render on github.com in the branch's blob view of `docs/wiki/Home.md` and `docs/wiki/Architecture.md` |

## 5. Files to change

| File | Change |
|---|---|
| `.github/scripts/publish-wiki.sh` | New. The mirror (D3, D4) |
| `.github/scripts/publish-wiki.test.sh` | New. Its tests |
| `.github/workflows/wiki-publish.yml` | New. The publisher (§3.2) |
| `.github/workflows/wiki-sync.yml` | Deleted |
| `.github/workflows/spec-validation.yml` | Drop `--ignore 'docs/wiki/**'`; add the `test-wiki-publisher` job (D12) |
| `CLAUDE.md` | Drop `--ignore 'docs/wiki/**'` from the markdownlint command |
| `.claude/skills/dsh-reconcile-prd/SKILL.md` | Drop `--ignore 'docs/wiki/**'` from its markdownlint command |
| `docs/wiki/README.md` | Rewritten: the source, the publisher, the rules for a page |
| `docs/wiki/Home.md` | Rewritten (§3.3) |
| `docs/wiki/Architecture.md` | New (§3.3) |
| `docs/wiki/Developers.md` | New (§3.3) |
| `docs/wiki/Workflow.md` | Header removed (D11) |
| `docs/wiki/Code-Based-Site-and-Reports.md` | Header removed, bare URLs wrapped (D11) |
| `src/site/markdown/index.md` | One paragraph and links (D10) |
| `.github/copilot-instructions.md` | "Wiki References": the new direction, one row per page |
| `docs/devops/README.md` | Pipeline Map node, two table rows, the sentence on secrets |
| `docs/copilot/development-guide.md` | The example page name, and the note on syncing |
| `specs/stories/44-restructure-wiki.md` | This spec, and its verification results |

## 6. Implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `dsh-build-story`, which applies
> superpowers:test-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** the wiki's source lives in `docs/wiki/`, is published from there, and carries one
overview, an Architecture page and a Developers page.

**Architecture:** a bash script mirrors `docs/wiki/` into a clone of the wiki, under a plain-bash
test. A workflow on `DEVELOP` runs it and pushes. The pages are Markdown with Mermaid.

**Tech stack:** bash, GitHub Actions, GitHub-flavoured Markdown, Mermaid, markdownlint.

**Spec:** this file, §1 to §5.

### Global constraints

- Shell commands run in Git Bash. markdownlint is run as
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint ...`; there is no other Node.
- No product name (RabbitMQ, Firestore, Pub/Sub, Cloud Storage, MongoDB, Solr) on `Home.md`,
  `Architecture.md` or `src/site/markdown/index.md`.
- Wiki-to-wiki links: `[Page](Page-Name)`. Repository links: absolute, to `blob/DEVELOP/`.
- Every planned node: "(planned)" in its label, in the `planned` class, reached by a dashed edge.
  A planned edge between two built nodes: dashed, with "(planned)" in its label.
- One commit per task, message `<type>(#44): ...`, ending with the attribution lines the session
  gives.
- Never push, never open the PR, never close an issue: steps 5 and 6 are `dsh-ship-story`'s.

### Task 1: The publisher script, test first

**Files:**

- Create: `.github/scripts/publish-wiki.test.sh`
- Create: `.github/scripts/publish-wiki.sh`

**Interfaces:**

- Produces: `bash .github/scripts/publish-wiki.sh SOURCE_DIR WIKI_DIR`. Exit 0 on success,
  printing one `Published: <file>` or `Removed: <file>` line per change; exit 1 when a directory
  is missing or the source holds no page; exit 2 on a wrong number of arguments.
- Produces: `bash .github/scripts/publish-wiki.test.sh`, exit 0 when every case passes.

- [ ] **Step 1: Write the test.** Create `.github/scripts/publish-wiki.test.sh`:

```bash
#!/usr/bin/env bash
#
# Tests for publish-wiki.sh.
#
# Plain bash: no Node, no Python, no test framework. Each case builds a source
# directory and a wiki directory under a temporary directory, runs the publisher
# against them, and asserts the exit code, the output where the message is the
# point, and the files the wiki directory holds afterwards.
#
# Usage: publish-wiki.test.sh
#
# Exits non-zero if any case fails.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBLISHER="${SCRIPT_DIR}/publish-wiki.sh"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cases=0
failures=0
OUT=""
STATUS=0

# new_case -> prints a fresh fixture directory holding an empty "src" and "wiki".
new_case() {
  local dir
  dir="$(mktemp -d "${WORK}/case-XXXXXX")"
  mkdir -p "${dir}/src" "${dir}/wiki"
  printf %s "$dir"
}

# run_publisher ARGS... -> captures combined output in OUT and status in STATUS.
run_publisher() {
  OUT="$(bash "$PUBLISHER" "$@" 2>&1)"
  STATUS=$?
}

# listing DIR -> the top-level entries of DIR, sorted, one per line, .git included.
listing() {
  (cd "$1" && ls -A | LC_ALL=C sort)
}

# check NAME EXPECTED_STATUS EXPECTED_TEXT [ACTUAL_LISTING EXPECTED_LISTING]
check() {
  cases=$((cases + 1))
  local name="$1" expected_status="$2" expected_text="$3" reason=""

  if [ "$STATUS" -ne "$expected_status" ]; then
    reason="expected exit ${expected_status}, got ${STATUS}"
  elif [ -n "$expected_text" ] && ! printf '%s\n' "$OUT" | grep -qF -- "$expected_text"; then
    reason="output did not contain: ${expected_text}"
  elif [ "$#" -ge 5 ] && [ "$4" != "$5" ]; then
    reason="wiki holds [$(echo $4)], expected [$(echo $5)]"
  fi

  if [ -z "$reason" ]; then
    echo "PASS: ${name}"
  else
    echo "FAIL: ${name}"
    echo "      ${reason}"
    printf '      output: %s\n' "${OUT:-<empty>}"
    failures=$((failures + 1))
  fi
}

# --- every page is published, README.md is not ---------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "arch" > "${d}/src/Architecture.md"
echo "about the directory" > "${d}/src/README.md"
run_publisher "${d}/src" "${d}/wiki"
check "every page is published, README.md is not" 0 "Published: Home.md" \
  "$(listing "${d}/wiki")" "$(printf 'Architecture.md\nHome.md')"

# --- a changed page replaces the wiki's copy -----------------------------------
d="$(new_case)"
echo "new text" > "${d}/src/Home.md"
echo "old text" > "${d}/wiki/Home.md"
run_publisher "${d}/src" "${d}/wiki"
check "a changed page replaces the wiki's copy" 0 "" \
  "$(cat "${d}/wiki/Home.md")" "new text"

# --- a page with no source is removed ------------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "gone" > "${d}/wiki/Old-Page.md"
run_publisher "${d}/src" "${d}/wiki"
check "a page with no source is removed" 0 "Removed: Old-Page.md" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a README.md in the wiki is removed ----------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
echo "about" > "${d}/src/README.md"
echo "stray" > "${d}/wiki/README.md"
run_publisher "${d}/src" "${d}/wiki"
check "a README.md in the wiki is removed" 0 "Removed: README.md" \
  "$(listing "${d}/wiki")" "Home.md"

# --- .git and files that are not Markdown are left alone -----------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
mkdir -p "${d}/wiki/.git"
echo "ref" > "${d}/wiki/.git/HEAD"
echo "png" > "${d}/wiki/diagram.png"
run_publisher "${d}/src" "${d}/wiki"
check ".git and files that are not Markdown are left alone" 0 "" \
  "$(listing "${d}/wiki"; cat "${d}/wiki/.git/HEAD")" "$(printf '.git\nHome.md\ndiagram.png\nref')"

# --- Markdown below the top level is not a page --------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
mkdir -p "${d}/src/drafts"
echo "draft" > "${d}/src/drafts/Draft.md"
run_publisher "${d}/src" "${d}/wiki"
check "Markdown below the top level is not a page" 0 "" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a page name with a space is published -------------------------------------
d="$(new_case)"
echo "page" > "${d}/src/Two Words.md"
run_publisher "${d}/src" "${d}/wiki"
check "a page name with a space is published" 0 "Published: Two Words.md" \
  "$(listing "${d}/wiki")" "Two Words.md"

# --- a source with no page is refused, and the wiki is untouched ---------------
d="$(new_case)"
echo "about" > "${d}/src/README.md"
echo "home" > "${d}/wiki/Home.md"
run_publisher "${d}/src" "${d}/wiki"
check "a source with no page is refused, and the wiki is untouched" 1 "refusing to empty the wiki" \
  "$(listing "${d}/wiki")" "Home.md"

# --- a missing source directory is an error ------------------------------------
d="$(new_case)"
run_publisher "${d}/absent" "${d}/wiki"
check "a missing source directory is an error" 1 "not a directory: ${d}/absent"

# --- a missing wiki directory is an error --------------------------------------
d="$(new_case)"
echo "home" > "${d}/src/Home.md"
run_publisher "${d}/src" "${d}/absent"
check "a missing wiki directory is an error" 1 "not a directory: ${d}/absent"

# --- a wrong number of arguments prints the usage ------------------------------
run_publisher "only-one"
check "a wrong number of arguments prints the usage" 2 "Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR"

echo
echo "${cases} case(s), ${failures} failure(s)."
[ "$failures" -eq 0 ]
```

- [ ] **Step 2: Run it, and see it fail.**
  `bash .github/scripts/publish-wiki.test.sh; echo "exit=$?"`. Expected: every case FAILs, since
  the script does not exist (`bash` exits 127), and `exit=1`.

- [ ] **Step 3: Write the script.** Create `.github/scripts/publish-wiki.sh`:

```bash
#!/usr/bin/env bash
#
# Mirror the wiki's source, docs/wiki/, into a clone of the wiki's repository.
#
# Every top-level *.md file in SOURCE_DIR except README.md is a page, and is
# copied to WIKI_DIR under the same name. A top-level *.md file in WIKI_DIR with
# no page of the same name in SOURCE_DIR is deleted, so removing a file from
# docs/wiki removes its page. Anything else in WIKI_DIR -- .git, files that are
# not Markdown -- is left alone. README.md describes the source directory and is
# never a page.
#
# Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR
#
# Exits 2 on a wrong number of arguments, and 1 when either directory is missing
# or SOURCE_DIR holds no page at all: mirroring an empty source would delete
# every page of the wiki.

set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: publish-wiki.sh SOURCE_DIR WIKI_DIR"
  exit 2
fi

SOURCE="$1"
WIKI="$2"

for dir in "$SOURCE" "$WIKI"; do
  if [ ! -d "$dir" ]; then
    echo "ERROR: not a directory: $dir"
    exit 1
  fi
done

shopt -s nullglob

pages=()
for f in "$SOURCE"/*.md; do
  [ "$(basename "$f")" = "README.md" ] && continue
  pages+=("$f")
done

if [ "${#pages[@]}" -eq 0 ]; then
  echo "ERROR: no page in $SOURCE; refusing to empty the wiki."
  exit 1
fi

for f in "$WIKI"/*.md; do
  name="$(basename "$f")"
  if [ "$name" = "README.md" ] || [ ! -f "$SOURCE/$name" ]; then
    rm -- "$f"
    echo "Removed: $name"
  fi
done

for f in "${pages[@]}"; do
  cp -- "$f" "$WIKI/"
  echo "Published: $(basename "$f")"
done
```

- [ ] **Step 4: Run the test, and see it pass.** Same command. Expected: every case PASSes,
  `11 case(s), 0 failure(s).`, `exit=0`.

- [ ] **Step 5: Mark both files executable in git.**
  `git add .github/scripts/publish-wiki.sh .github/scripts/publish-wiki.test.sh && git update-index --chmod=+x .github/scripts/publish-wiki.sh .github/scripts/publish-wiki.test.sh`.

- [ ] **Step 6: Commit.** `test(#44): mirror docs/wiki into a clone of the wiki, under test`.

### Task 2: The workflows and the lint scope

**Files:**

- Create: `.github/workflows/wiki-publish.yml`
- Delete: `.github/workflows/wiki-sync.yml`
- Modify: `.github/workflows/spec-validation.yml`, `CLAUDE.md`,
  `.claude/skills/dsh-reconcile-prd/SKILL.md`

**Interfaces:**

- Consumes: `publish-wiki.sh` and `publish-wiki.test.sh` from Task 1.
- Produces: markdownlint covering `docs/wiki/`. From this commit on, `docs/wiki/` must lint
  clean, so Task 3 follows in the same session before any push.

- [ ] **Step 1: Create `.github/workflows/wiki-publish.yml`:**

```yaml
name: Wiki Publish

# docs/wiki is the wiki's source. Every push to DEVELOP that changes it mirrors
# it into the wiki's own repository (see docs/wiki/README.md).
on:
  push:
    branches:
      - DEVELOP
    paths:
      - 'docs/wiki/**'
      - '.github/scripts/publish-wiki.sh'
      - '.github/scripts/publish-wiki.test.sh'
      - '.github/workflows/wiki-publish.yml'
  workflow_dispatch:

concurrency:
  group: wiki-publish
  cancel-in-progress: false

jobs:
  publish:
    name: Publish docs/wiki to the wiki
    if: github.ref == 'refs/heads/DEVELOP'
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Test the publisher
        run: bash .github/scripts/publish-wiki.test.sh

      - name: Clone the wiki
        run: |
          git clone --depth=1 \
            "https://x-access-token:${WIKI_TOKEN}@github.com/${{ github.repository }}.wiki.git" \
            "${RUNNER_TEMP}/wiki"
        env:
          WIKI_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Mirror docs/wiki into the wiki
        run: bash .github/scripts/publish-wiki.sh docs/wiki "${RUNNER_TEMP}/wiki"

      - name: Commit and push
        working-directory: ${{ runner.temp }}/wiki
        run: |
          git add -A
          if git diff --cached --quiet; then
            echo "The wiki already matches docs/wiki."
            exit 0
          fi
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git commit -m "Publish docs/wiki from ${GITHUB_REPOSITORY}@${GITHUB_SHA}"
          git push
```

- [ ] **Step 2: Delete `wiki-sync.yml`.** `git rm .github/workflows/wiki-sync.yml`.

- [ ] **Step 3: `spec-validation.yml`.** In the `Lint markdown files` step, delete the whole line
  that holds `--ignore 'docs/wiki/**' \`. After the `check-spec-references` job, add:

```yaml

  test-wiki-publisher:
    name: Test the Wiki Publisher
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Test the publisher
        run: bash .github/scripts/publish-wiki.test.sh
```

- [ ] **Step 4: `CLAUDE.md`.** In the Markdown lint row of "Commands", delete
  `--ignore 'docs/wiki/**'` and the space before it, leaving
  `markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --config .markdownlint.json`.

- [ ] **Step 5: `dsh-reconcile-prd`.** In §4 of `.claude/skills/dsh-reconcile-prd/SKILL.md`,
  delete `--ignore 'docs/wiki/**'` and the space after it, keeping the line's indentation and
  `--config .markdownlint.json`.

- [ ] **Step 6: Check.** `grep -rn "docs/wiki/\*\*" CLAUDE.md .github/workflows/spec-validation.yml .claude/skills/dsh-reconcile-prd/SKILL.md`
  returns nothing. markdownlint over `docs/wiki/` now reports the 37 errors of §2.1, which Task 3
  clears; do not commit Task 2 alone if a push could follow.

- [ ] **Step 7: Commit.** `ci(#44): publish docs/wiki to the wiki, and lint it`.

### Task 3: The pages

**Files:**

- Modify: `docs/wiki/Home.md`, `docs/wiki/README.md`, `docs/wiki/Workflow.md`,
  `docs/wiki/Code-Based-Site-and-Reports.md`, `src/site/markdown/index.md`
- Create: `docs/wiki/Architecture.md`, `docs/wiki/Developers.md`

**Interfaces:**

- Consumes: the lint scope from Task 2.
- Produces: five pages, at the paths Task 4 lists in `copilot-instructions.md`.

- [ ] **Step 1: Write the failing checks.** These are the story's acceptance checks. Run them
  now and record that they fail:

```bash
PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'docs/wiki/**/*.md' --config .markdownlint.json; echo "lint exit=$?"
grep -n -i -E 'rabbit|firestore|pub/?sub|cloud storage|mongo|solr' docs/wiki/Home.md docs/wiki/Architecture.md src/site/markdown/index.md; echo "names exit=$?"
test -f docs/wiki/Architecture.md && test -f docs/wiki/Developers.md; echo "pages exit=$?"
```

  Expected now: `lint exit=1`; `names exit=2`, with `SOLR` matched in `Home.md` and an error for
  the missing `Architecture.md` (grep exits 2 on an error even when it matched); `pages exit=1`.

- [ ] **Step 2: Replace `docs/wiki/Home.md`** with:

````markdown
# Document Smart Highlights

Document Smart Highlights (DSH) aims to provide web services that accept PDF and HTML files and
return a list of keywords and the most relevant sentences, extracted with NLP techniques; the
sentences through automatic document summarization.

## What DSH does today

DSH accepts a PDF document, keeps it in a document store, in memory by default, puts the
document's id on a queue for indexing, and reports the document's processing status. Nothing takes
the id off that queue yet, so DSH does not index documents and does not extract keywords or
sentences. The REST API has two operations: submit a document and receive a token, and ask for the
status of the document a token was issued for.

## The pipeline

```mermaid
flowchart LR
    client([Client]) -->|/submit| gateway[Gateway]
    gateway --> hop1[(Hop 1<br/>queue)]
    hop1 -.-> indexing["Indexing<br/>(planned)"]
    indexing -.-> hop2[("Hop 2<br/>queue<br/>(planned)")]
    hop2 -.-> keywords["Keywords<br/>(planned)"]
    hop2 -.-> sentences["Sentences<br/>(planned)"]
    classDef planned stroke-dasharray: 5 5
    class indexing,hop2,keywords,sentences planned
```

A document passes six stages. The gateway is the API's `/submit`: it stores the document and
sends its id on hop 1. A message carries the document's id and nothing else, and each stage reads
what it needs from the stores. The parts marked planned, with a dashed outline, have no code yet:
today nothing receives from hop 1.

The diagram is drawn by hand from section 1 of
[ADR-003](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-003-document-pipeline.md),
which decides the pipeline. Once the pipeline's code exists, the diagram will be generated from it.
[Architecture](Architecture) shows the modules and the systems they use.

## Planned processing

### File submission

1. The user submits a file with a web service call. DSH accepts it for asynchronous processing and
   returns a token.
2. The user asks for the processing status with the token, as often as needed.
3. When processing is done, the user retrieves the results with another call, in two parts:
   - a list of keywords
   - a list of the most relevant sentences

   Retrieving the results is not available yet.

### File indexing

The indexing stage extracts the document's text and identifies its individual terms, inside the
application. It stores the result, and updates the statistics of every term across all documents,
which keyword scoring needs.

### Keyword extraction

1. Each term in the document's text is scored with a
   [TF/IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) approach, through several implementations
   of TF and IDF, as shown in [this paper](https://doi.org/10.2352/issn.2168-3204.2017.1.0.105).
2. The terms with the best scores, obtained by a
   [meta-algorithmic](https://www.amazon.com/Meta-Algorithmics-Patterns-Robust-Quality-Systems/dp/1118343360)
   combination of all the TF/IDF combinations, are returned as the best ranked keywords.

### Most relevant sentences

1. Typical [automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization)
   techniques are applied, such as extractive summarization or
   [keyphrase extraction](https://en.wikipedia.org/wiki/Automatic_summarization#Keyphrase_extraction).
2. A sentence's relevance takes two criteria into account:
   - its similarity to the title, using the algorithm of
     [this paper](https://doi.org/10.1016/j.csl.2016.01.003);
   - combinations of the TF/IDF of each of its terms, using the keyword extraction above.
3. The sum of the two criteria is the sentence's score. The best scored sentences are returned with
   their paragraph numbers.

## More

- [Architecture](Architecture): the patterns DSH follows, and its components.
- [Developers](Developers): where to start working on DSH.
- [Workflow](Workflow): the statuses a document moves through.
- [Code Based Site and Reports](Code-Based-Site-and-Reports): the Maven sites and their reports.
````

- [ ] **Step 3: Create `docs/wiki/Architecture.md`:**

````markdown
# Architecture

DSH is sorted here by the three patterns of Pivotal Labs'
["Agile Architecture"](https://blogs.vmware.com/tanzu/agile-architecture/) (2017), which follows
Robert Martin: Request/Response, Event Driven and Batch. The article prescribes no diagrams; the
ones below are DSH's own.

## Request/Response

A user asks and waits for the reply, and retrying is the user's job.

In DSH this is the REST API, under `/v1/dsh/document`:

- `POST /submit` takes a document and returns a token.
- `GET /status/{token}` returns the processing status of the document the token was issued for.

The user polls the status. A failed call is the caller's to repeat. Retrieving the results is not
available yet.

## Event Driven

Actors react to events, and may produce new ones.

In DSH this is the pipeline that processes a document after `/submit`, drawn on [Home](Home). Each
stage reacts to a message on a queue that carries the document's id and nothing else, and its
result moves the document to its next status. A message whose stage fails will be delivered again,
so a stage must be safe to run twice. Only the first step exists today: `/submit` puts the id on
the first queue, and nothing receives it yet.

## Batch

The system works on its own schedule, so detecting failures, retrying and recovering are its job.

DSH has no Batch part today, and none is planned. Every piece of work it does starts from a
request or from a message.

## Components

```mermaid
flowchart TB
    client([Client]) -->|HTTP| rest["dsh-rest-api<br/>REST API"]
    rest --> data["dsh-data<br/>models, store interfaces,<br/>in-memory document store"]
    rest -->|"hop 1: send"| queue[(Queue)]
    queue -.->|"hops 1 and 2: receive<br/>(planned)"| rest
    rest -.->|"hop 2: send<br/>(planned)"| queue
    rest -.-> indexer["dsh-doc-indexer<br/>indexing stage<br/>(planned)"]
    subgraph analyser["dsh-doc-analyser"]
        keywords["dsh-keyword-extractor<br/>keywords stage<br/>(planned)"]
        sentences["dsh-top-sentences-extractor<br/>sentences stage<br/>(planned)"]
    end
    rest -.-> keywords
    rest -.-> sentences
    rest -.-> provider["dsh-gcp<br/>cloud adapters<br/>(planned)"]
    provider -.-> nosql[("NoSQL store<br/>(planned)")]
    provider -.-> files[("File store<br/>(planned)")]
    classDef planned stroke-dasharray: 5 5
    class indexer,keywords,sentences,provider,nosql,files planned
```

- **Built.** `dsh-rest-api` is the one application: the REST API today, and the pipeline once it
  is built. `dsh-data` holds the models, the interfaces of the stores, and the document store DSH
  runs with by default, which keeps documents in memory. The queue carries a document's id from
  `/submit` to the pipeline: today `dsh-rest-api` sends it on hop 1, and nothing receives it.
- **Planned.** Each stage drawn on [Home](Home) is a library module: indexing is
  `dsh-doc-indexer`, keywords `dsh-keyword-extractor`, and sentences
  `dsh-top-sentences-extractor`. The modules exist and hold no code yet. A stage never touches the
  queue: the pipeline in `dsh-rest-api` receives a document's id from a hop, calls the stage, and
  sends the id on the next hop, which is why every edge to and from the queue is
  `dsh-rest-api`'s. `dsh-gcp` will hold the adapters to a cloud provider's NoSQL store and file store, and to
  its queue: the application will then reach the same queue through that adapter instead of
  today's. The NoSQL store will hold each document's record, and the file store the document and
  whatever grows with it.
- The modules that hold only test fixtures or build reports are not drawn.

## The decisions behind it

- [ADR-001](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-001-GCP-based-components.md):
  the managed services DSH moves to.
- [ADR-003](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-003-document-pipeline.md):
  the pipeline, its modules, and how an implementation is selected.
- [ADR-004](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-004-gcp-tiers.md):
  the tiers DSH runs in.
- [System design](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/system-design.md):
  today's architecture in detail.
````

- [ ] **Step 4: Create `docs/wiki/Developers.md`:**

```markdown
# Developers

Start at [`CLAUDE.md`](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/CLAUDE.md). It is the
entry point for everyone who works on DSH, people and agents alike, and it leads to every other
document a developer needs. This page repeats none of it.

## Editing this wiki

This wiki is written in
[`docs/wiki/`](https://github.com/MRISS-Projects/dsh/tree/DEVELOP/docs/wiki) and published from
there. Change a page by pull request, like any other file. An edit made here, on the wiki, is
overwritten by the next publish.

## Other pages for developers

- [Workflow](Workflow): the statuses a document moves through.
- [Code Based Site and Reports](Code-Based-Site-and-Reports): the Maven sites and their reports.
```

- [ ] **Step 5: Replace `docs/wiki/README.md`** with:

```markdown
# docs/wiki — the wiki's source

The pages of the [DSH wiki](https://github.com/MRISS-Projects/dsh/wiki) are written here and
published from here. Each top-level `.md` file except this README is one page, named after the
file: `Home.md` is the wiki's Home page.

## How a page is published

`.github/workflows/wiki-publish.yml` runs on every push to `DEVELOP` that changes this directory,
and can be dispatched by hand. It runs `.github/scripts/publish-wiki.sh`, which mirrors this
directory into the wiki's own repository. A file added here adds a page, a file changed changes
it, and a file deleted deletes it. This README is not published.

## Editing

Edit the pages here, by pull request, like any other file. They are reviewed, and markdownlint
checks them with the rest of the repository's Markdown. Do not edit the wiki on GitHub: the next
publish overwrites the edit.

## Writing a page

- **Links between pages** use the wiki's form, the page's name without `.md`:
  `[Architecture](Architecture)`. They resolve in the wiki, not in this directory.
- **Links to repository files** are absolute, to
  `https://github.com/MRISS-Projects/dsh/blob/DEVELOP/...`. A wiki page cannot resolve a path in
  the repository.
- **Names.** Home and Architecture use generic terms: queue, NoSQL store, file store. The products
  behind them are changing, under ADR-001, and an overview that names them goes stale. Developer
  pages keep the product names.
- **Planned parts of a diagram** carry "(planned)" in their label, a dashed outline, and dashed
  edges into them. A planned edge between two built parts is dashed, and carries "(planned)" in
  its label.
```

- [ ] **Step 6: The two existing pages.** Delete line 1, the `<!-- AUTO-GENERATED ... -->`
  header, from `docs/wiki/Workflow.md` and `docs/wiki/Code-Based-Site-and-Reports.md`. In the
  latter's table, wrap each of the three bare URLs in `<` and `>`, and change nothing else.

- [ ] **Step 7: Replace `src/site/markdown/index.md`** with:

```markdown
# Document Smart Highlights

This is the Maven site of DSH, Document Smart Highlights: its build reports and project
information. What DSH is, what it does today, what is planned and how it is built are described
once, in the project's wiki.

- [The wiki's Home page](https://github.com/MRISS-Projects/dsh/wiki): the overview and the pipeline
- [Architecture](https://github.com/MRISS-Projects/dsh/wiki/Architecture): the patterns and the
  components
- [Source code](https://github.com/MRISS-Projects/dsh)
- [Project information](project-info.html)
```

- [ ] **Step 8: Run the checks of Step 1 again.** Expected: `lint exit=0`; `names exit=1`, with
  no output; `pages exit=0`. Then
  `grep -n -E 'mvn|git |branch' docs/wiki/Developers.md; echo "dev exit=$?"`. Expected:
  `dev exit=1`, with no output.

- [ ] **Step 9: Commit.** `docs(#44): one overview on Home, with Architecture and Developers pages`.

### Task 4: The documents that describe the direction

**Files:**

- Modify: `.github/copilot-instructions.md:68-77`, `docs/devops/README.md`,
  `docs/copilot/development-guide.md:48-57`

**Interfaces:**

- Consumes: the five page paths from Task 3, which `check-spec-references.sh` resolves.

- [ ] **Step 1: Write the failing check.**
  `grep -rn -i "wiki-sync\|wiki sync\|synced" .github/copilot-instructions.md docs/devops/README.md docs/copilot/development-guide.md docs/wiki/README.md; echo "exit=$?"`.
  Expected now: matches in the first three files, `exit=0`.

- [ ] **Step 2: `copilot-instructions.md`.** Replace the "Wiki References" section, lines 68 to
  77, with:

```markdown
## Wiki References

The `/docs/wiki/` directory is the source of the [DSH GitHub Wiki](https://github.com/MRISS-Projects/dsh/wiki). `.github/workflows/wiki-publish.yml` publishes it to the wiki on every push to `DEVELOP` that changes it. Use these files as `@workspace` context in Copilot prompts exactly like any spec file.

| Wiki Page | Location in repo |
| ----------- | ----------------- |
| Home: the overview and the pipeline | `/docs/wiki/Home.md` |
| Architecture: the patterns and the components | `/docs/wiki/Architecture.md` |
| Developers | `/docs/wiki/Developers.md` |
| Workflow: the document statuses | `/docs/wiki/Workflow.md` |
| Code Based Site and Reports | `/docs/wiki/Code-Based-Site-and-Reports.md` |

> **Note:** Edit wiki pages in `/docs/wiki/`, by pull request. An edit made on the GitHub wiki is overwritten by the next publish. `/docs/wiki/README.md` has the rules for writing a page.
```

- [ ] **Step 3: `docs/devops/README.md`.** Three edits:
  - Pipeline Map: replace `WS["wiki-sync.yml<br/>daily 02:00 UTC to docs/wiki"]` with
    `WP["wiki-publish.yml<br/>docs/wiki to the wiki, on push to DEVELOP"]`.
  - Workflow Reference: in the `spec-validation.yml` row, delete `` (excluding `docs/wiki/**`) ``
    after `` `docs/` ``, and append to the row's last cell the sentence
    `` It also runs `.github/scripts/publish-wiki.test.sh`, the wiki publisher's tests. ``
  - Workflow Reference: replace the whole `wiki-sync.yml` row with the row below.
  - The sentence on secrets (line 277): replace `` `wiki-sync.yml` uses `` with
    `` `wiki-publish.yml` uses ``.

  The new row:

```markdown
| `wiki-publish.yml` | `push` to `DEVELOP`, scoped to paths `docs/wiki/**`, `.github/scripts/publish-wiki.sh`, `.github/scripts/publish-wiki.test.sh`, `.github/workflows/wiki-publish.yml`; `workflow_dispatch` | Publishes `docs/wiki/`, the wiki's source, to the GitHub wiki. It tests `.github/scripts/publish-wiki.sh`, clones the wiki, runs the script to mirror the directory into it, and pushes when a page changed. See `docs/wiki/README.md`. |
```

- [ ] **Step 4: `docs/copilot/development-guide.md`.** On line 51, replace
  `/docs/wiki/Architecture-Decisions.md` with `/docs/wiki/Architecture.md`, and on line 52 replace
  `the wiki's architecture decisions` with `the wiki's Architecture page`. Replace the two-line
  note at lines 56 and 57 with:

```markdown
> Wiki pages are written in `/docs/wiki/` and published to the [project wiki](https://github.com/MRISS-Projects/dsh/wiki)
> from there, so the copy here is always the current one.
```

- [ ] **Step 5: Run the check again, and the reference checker.** The grep of Step 1: expected
  `exit=1`, no output. `bash .github/scripts/check-spec-references.test.sh && bash .github/scripts/check-spec-references.sh; echo "refs exit=$?"`:
  expected `refs exit=0`, every `/docs/wiki/` path resolved.

- [ ] **Step 6: Commit.** `docs(#44): describe docs/wiki as the wiki's source`.

### Task 5: The acceptance checks, the dry run and the follow-up issue

- [ ] **Step 1: Lint, as `CLAUDE.md` gives it.**
  `PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' --config .markdownlint.json; echo "lint exit=$?"`.
  Expected: `lint exit=0`.

- [ ] **Step 2: The code at merge (AC001, AC003).**
  `find . -path '*/src/main/java/*.java' -not -path '*/target/*' | cut -d/ -f2-3 | sort -u` lists
  only `dsh-data/src` and `dsh-rest-api/src`.
  `grep -rn -i -E 'inbound-channel-adapter|message-driven-channel-adapter|RabbitListener|Amqp\.inbound' dsh-*/src/main; echo "exit=$?"`
  returns `exit=1`. If either differs when the story merges, the diagrams' planned labels change
  to match first.

- [ ] **Step 3: AC004 to AC006.** The `grep` checks of §4: each returns nothing.

- [ ] **Step 4: A dry run against the real wiki.** In a temporary directory, never in the
  repository:

```bash
DRY="$(mktemp -d)"
git clone -q https://github.com/MRISS-Projects/dsh.wiki.git "$DRY/wiki" || exit 1
bash .github/scripts/publish-wiki.sh docs/wiki "$DRY/wiki"; echo "publish exit=$?"
git -C "$DRY/wiki" status --short
rm -rf "$DRY"
```

  Expected: `publish exit=0`. `status` shows `Architecture.md` and `Developers.md` as untracked
  (`??`), `Home.md` and `Code-Based-Site-and-Reports.md` as modified (`M`), and nothing else. `Workflow.md` does not
  appear: without its header it is byte for byte the wiki's page, which is the proof that D11
  changed nothing in it. Do not push.

- [ ] **Step 5: Gate 1.** `mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as
  `CLAUDE.md` prescribes, with its exit code reported. No Java changes, but the gate is the
  story's. Gate 3 does not apply: no code touches an external system or an API entry point.

- [ ] **Step 6: Draft the follow-up issue (D9)** and show it to the owner. It is opened only on
  approval. Title: "README describes DSH today: drop `dsh-solr`, and point the overview at the
  wiki". Body: the module list still names `dsh-solr`, removed by `#177`; "What 0.3.0 does today"
  says a NoSQL store, while `DEVELOP` keeps documents in memory (`#167`); the overview duplicates
  the wiki's Home, which `#44` made the one overview. Spun off from `#44`, whose scope did not
  name README.

- [ ] **Step 7: Record the results in §8**, and commit:
  `docs(#44): record the verification results`.

**After the push (step 6 of the process):** open the branch's blob view of `docs/wiki/Home.md`
and `docs/wiki/Architecture.md` on github.com, and confirm both diagrams render. **At the
merge:** §3.2's three steps.

## 7. Acceptance criteria

| AC | Where it is met |
|---|---|
| AC001: Home carries the overview and the pipeline diagram, planned stages labelled | Task 3, Step 2; checked by Task 5, Step 2 |
| AC002: Architecture sorts DSH by the three patterns, Batch included | Task 3, Step 3 |
| AC003: Architecture's components diagram, planned labelled, ADR links | Task 3, Step 3; checked by Task 5, Step 2 |
| AC004: Developers links `CLAUDE.md`, restates none of it | Task 3, Step 4; checked by Task 3, Step 8 |
| AC005: `index.md` is one paragraph and links, including Home | Task 3, Step 7 |
| AC006: no product name on Home, Architecture, `index.md` | Task 3, Steps 2, 3 and 7; checked by Task 3, Step 8 |
| AC007: the source moves, decided and reasoned (D1) | Tasks 1, 2 and 4; Task 5, Steps 1 and 4; §3.2 at the merge |

## 8. Verification results

Filled in by Task 5.

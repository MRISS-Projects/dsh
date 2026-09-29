---
issue: 143
slug: readme-describes-dsh-today
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 143 — Make the README and the site index describe what DSH does today

## 1. Story

**As a** visitor reading the DSH 0.3.0 README or site
**I want** it to describe what the software does today and how to run it as it is built now
**So that** I neither expect keyword and sentence extraction that does not exist yet, nor follow
setup steps for a WAR on Tomcat 8 that no longer apply

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4, "One task has no issue yet"),
  milestone `0.3.0-SNAPSHOT`
- Issue: [#143](https://github.com/MRISS-Projects/dsh/issues/143), label `task`
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`. The 0.3.0 release regenerates the root `README.md`
  from `src/site/markdown/README.md`, so the fix has to reach the RC it will be released from.
- Files: `src/site/markdown/README.md` and `src/site/markdown/index.md`. Nothing else changes.

### 2.1 Facts the new text states, and where each was verified

Read from the RC at `e7a9b5d4a` on 2026-09-28.

| Fact | Source |
|---|---|
| `dsh-rest-api` is packaged as a jar, repackaged by `spring-boot-maven-plugin` | `dsh-rest-api/pom.xml`: `<packaging>jar</packaging>`, `repackage` goal |
| The integration build runs against `mongo:6` and `rabbitmq:3` | `dsh-rest-api/pom.xml`, `docker-maven-plugin` images |
| The application authenticates to MongoDB as `mongo.user` against the `dsh` database | `dsh-data/src/main/resources/dshApplicationContext.xml`, connection string `mongodb://${mongo.user}:${mongo.password}@${mongo.host}:${mongo.port}/dsh` |
| The integration build creates that user with `readWrite` on `dsh` | `dsh-rest-api/src/test/docker/mongo-init.js` |
| `mongo.*` and `spring.rabbitmq.*` can be overridden on the command line | the `start-application` execution in `dsh-rest-api/pom.xml` passes `--mongo.host`, `--mongo.port`, `--mongo.user`, `--mongo.password`, `--spring.rabbitmq.host`, `--spring.rabbitmq.port` |
| Two endpoints exist: `POST /v1/dsh/document/submit` (`title`, `contents`) and `GET /v1/dsh/document/status/{token}` | `DocumentResource.java` |
| A failed submit returns the token `ERROR` with a message; an unknown token returns status `TOKEN_NOT_FOUND` | `DocumentResource.java` |
| After a submit the status ends at `QUEUED_FOR_INDEXING_SUCCESS`, or `QUEUED_FOR_INDEXING_ERROR` when the id could not be queued; nothing moves it further | `DocumentEnqueueResponseMessageHandler.java`, `DocumentStatus.java`; no module consumes the queue |
| `dsh-doc-indexer-worker`, `dsh-doc-processor-worker`, `dsh-keyword-extractor`, `dsh-top-sentences-extractor` each hold only a `@SpringBootApplication` main class | their `src/main/java` trees |
| `dsh-solr` holds two Solr plugins, `solr-advanced-numbers-filter` and `solr-terms-vector-order`, plus `config/conf/solrconfig.xml` | `dsh-solr/` |
| `dsh-coverage-report` and `dsh-test-dataset` have no production sources | their trees |
| The data models are `Document`, `Keyword`, `Sentence` and the `DocumentStatus` workflow; the README's `RelevantSentence` does not exist | `dsh-data/src/main/java/com/mriss/dsh/data/models/` |
| Springfox is 3.0.0, and 0.3.0 serves **no** working Swagger UI; the Swagger 2 description is at `/v2/api-docs?group=dsh-app` | `spring-fox.version` 3.0.0 in parent-poms `products` 3.9.0, which DSH pins; the smoke run, Task 6, found every UI path 404 and no `springfox-boot-starter`. Raised as [#144](https://github.com/MRISS-Projects/dsh/issues/144) |
| The status moves from `QUEUED_FOR_INDEXING` to `QUEUED_FOR_INDEXING_SUCCESS` asynchronously, within seconds of the submit | smoke run, Task 6 |
| The unit build starts no Spring context, so `mvn -B clean install` needs no running MongoDB or RabbitMQ | `CLAUDE.md` hard rules, `.github/scripts/check-unit-tests-context-free.sh` |

### 2.2 Baseline

- `markdownlint src/site/markdown/README.md src/site/markdown/index.md --config .markdownlint.json`
  reports **144 errors**: MD009 58, MD040 30, MD031 23, MD010 23, MD034 5, MD005 3, MD022 1, MD012 1.
  The repository's documented lint command does not cover `src/site/`, which is how they
  accumulated.
- The AC002 grep matches in Introduction, Pre-requisites, Tomcat, Tomcat Admin User Configuration
  and Usage.

## 3. Decisions taken while specifying

1. **Correct in place, do not restructure.** The README keeps its section order. Cutting `index.md`
   down is Wave 1 task 5 and rewriting the README as the release zip's end-user guide is Wave 8
   task 5; restructuring now would pre-empt both.
2. **MongoDB and RabbitMQ come from Docker.** The per-OS native install walkthroughs are replaced by
   `docker run` of the same images the integration build uses, plus one `mongosh` command creating
   the application user. Docker becomes a prerequisite; it already is one for `-DintegrationTests`.
3. **Every reactor module gets an entry**, including `dsh-solr` and `dsh-coverage-report`, which
   the README omits today. Product names are allowed in module entries: AC005 covers the overview
   text only.
4. **`index.md`'s algorithm sections stay**, relabelled as the planned design.
5. **The whole README is linted clean**, untouched sections included, because AC007 is on the file.
6. **The staging regeneration is a post-merge check.** It can only run on the RC after the merge, so
   it is recorded as the last thing done before the issue is closed.

## 4. Design

### 4.1 `src/site/markdown/README.md`, section by section

The Maven placeholders `${release.type}`, `${project.build.version}` and `${issues.text.list}` stay
exactly as they are.

**Introduction.** Replace the first two paragraphs (from "Welcome to" to "servlet container.") with:

```markdown
Welcome to **DSH - Document Smart Highlights**. Document Smart Highlights aims to provide web
services that accept PDF and HTML files and return a list of keywords and the most relevant
sentences, extracted with NLP techniques; the sentences through automatic document summarization.
More details at the [wiki](https://github.com/MRISS-Projects/dsh/wiki).

**What 0.3.0 does today.** It accepts a PDF document, stores it in a NoSQL store, puts its id on a
queue for indexing, and reports the document's processing status. Nothing consumes that queue
yet, so 0.3.0 does not index documents and does not extract keywords or sentences.

This project is distributed as source code; no binary distribution is provided yet. Building it
produces the REST API as an executable Spring Boot jar, which runs on its own, with no separate
server to install.
```

In the goals list, goal 1 becomes: "Implement an NLP based system to extract relevant information
from PDF files in a potentially scalable fashion, using a NoSQL store and queues. This
infrastructure stores requests and chains a workflow of operations (workers) that extract keywords
and relevant sentences, all provided as a REST API." Goal 2, the contribution paragraphs and the
links are kept, with trailing spaces removed.

**Package/Folders Description.** Replaced by one entry per reactor module, in reactor order; the
`dsh-doc-analyser` children put the processor worker first, because it runs the other two:

```markdown
* **dsh-test-dataset**: the PDF files used as fixtures by automated tests.
* **dsh-data**: the data models, **Document**, **Keyword** and **Sentence**, and the
  [workflow](https://github.com/MRISS-Projects/dsh/wiki/Workflow) of statuses a document moves
  through while it is processed. Also the MongoDB persistence of documents.
* **dsh-rest-api**: the application. A Spring Boot jar exposing the REST API: document submission
  and processing status. Querying the results (keywords and relevant sentences) is not available
  yet.
* **dsh-solr**: a parent module grouping two Solr plugins, plus a `solrconfig.xml`. Nothing in
  0.3.0 runs Solr, and replacing it is proposed in
  [ADR-001](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/ADR-001-GCP-based-components.md).
  * **solr-terms-vector-order**: a search component that returns a document's term vector ordered
    by term statistics such as term and document frequency.
  * **solr-advanced-numbers-filter**: a token filter for tokens made of digits.
* **dsh-doc-indexer-worker**: a Spring Boot application holding only its main class so far. Its job
  will be to take a document id off the indexing queue, read the document from the database, send
  it to the indexer, and split its text into paragraphs, sentences and terms.
* **dsh-doc-analyser**: a parent module grouping the analysis modules below.
  * **dsh-doc-processor-worker**: an empty Spring Boot application for now; the plan is for it to
    take indexed documents off a queue and run both extractors on them.
  * **dsh-keyword-extractor**: not implemented yet. It will score each term of a document with
    combinations of TF/IDF and return the best ranked terms as keywords.
  * **dsh-top-sentences-extractor**: not implemented yet; planned to rank sentences with
    [automatic summarization](https://en.wikipedia.org/wiki/Automatic_summarization) techniques
    and return the most relevant ones.
* **dsh-coverage-report**: aggregates the test coverage of every module into one report and the
  coverage badge.
```

**Pre-requisites.** Replaced by:

```markdown
* Java 17 (Temurin)
* Maven 3.9.16
* Docker, to run MongoDB and RabbitMQ, and for the integration build
* MongoDB 6, run from the `mongo:6` image
* RabbitMQ 3, run from the `rabbitmq:3` image
```

**Java and Maven.** Content kept; lint fixes only (fence languages, blank lines around fences,
trailing spaces, the mis-indented `export PATH` line, the tab in the Windows Maven list).

**MongoDB and RabbitMQ.** Both sections, all their subsections included, are replaced by one
section:

````markdown
#### MongoDB and RabbitMQ

Run both from the images the integration build is verified against:

```bash
docker run -d --name dsh-mongo -p 127.0.0.1:27017:27017 mongo:6
docker run -d --name dsh-rabbitmq -p 127.0.0.1:5672:5672 rabbitmq:3
```

MongoDB takes a few seconds to start. Then create the user the application connects to it as, with
read and write access to the `dsh` database. Choose your own password:

```bash
docker exec dsh-mongo mongosh dsh --quiet --eval \
  'db.createUser({user: "dshuser", pwd: "YOUR-DSH-PASSWORD", roles: [{role: "readWrite", db: "dsh"}]})'
```

RabbitMQ needs no set-up: the application connects as its default `guest` user. Stop both with
`docker stop dsh-mongo dsh-rabbitmq`, and start them again with `docker start`; the MongoDB user
lives in the container, so it survives a restart but not a `docker rm`.
````

**Building From Sources.** Replaced by:

````markdown
#### Building From Sources

1. Configure Maven's user settings as described in
   [Maven Settings for GitHub Packages](#maven-settings-for-github-packages) below, which is what
   lets Maven resolve the parent POM `com.mriss.mriss-parent:products`.
2. At the root dsh folder type:

   ```bash
   mvn clean install
   ```

This runs the unit tests only, and needs neither MongoDB nor RabbitMQ running. The integration
tests run with `mvn clean install -DintegrationTests`, which starts its own MongoDB and RabbitMQ
containers, so it needs a running Docker daemon.
````

**Tomcat.** The section is deleted.

**Configuration.** "Maven Settings for GitHub Packages" is untouched apart from lint fixes. "MongoDB
Access Properties" is kept, with tabs replaced by spaces and "the password you have configured in
the steps above when installing mongo" changed to "the password you chose when creating the
MongoDB user". "Tomcat Admin User Configuration" is deleted.

**Usage.** "Running Application from Eclipse Embedded Tomcat" and "Using Application .war File on
a Servlet Container", with all their subsections, are deleted. The section becomes:

````markdown
## Usage

With MongoDB and RabbitMQ running and the build done, start the application in either of two ways.

### Running the Jar

```bash
java -jar dsh-rest-api/target/dsh-rest-api-<version>.jar
```

The MongoDB connection comes from the `mongo.*` properties in your Maven settings (see
[MongoDB Access Properties](#mongodb-access-properties)), resolved when the jar was built. Any of
them can be overridden when starting it, as can the RabbitMQ address:

```bash
java -jar dsh-rest-api/target/dsh-rest-api-<version>.jar \
  --mongo.host=localhost --mongo.port=27017 \
  --mongo.user=dshuser --mongo.password=YOUR-DSH-PASSWORD \
  --spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672
```

### Running with the Spring Boot Maven Plugin

From the `dsh-rest-api` folder:

```bash
mvn spring-boot:run
```

Either way, the application is up when the log shows a line like:

```text
c.m.dsh.restapi.DshRestApplication - Started DshRestApplication in 9.385 seconds
```
````

If the smoke run (Task 6) shows `mvn spring-boot:run` needs the same overrides, the section says
how to pass them (`-Dspring-boot.run.arguments=...`). What the smoke run shows is what the README
says. The README gives none: `mongo.*` is filtered into `mongo.properties` at build time, from the
same Maven settings the jar is built with, so a reader who used one password throughout needs no
override. That is inferred from the build, not observed; the smoke run overrode the password because
the local settings carry a different one (§7.2).

**Swagger User Interface.** Revised after the smoke run (Task 6) found no working Swagger UI in
0.3.0 ([#144](https://github.com/MRISS-Projects/dsh/issues/144)). The section, screenshot included,
is replaced by:

````markdown
### Calling the API

The REST API has the two operations of the document resource:

* **submit**, `POST /v1/dsh/document/submit`: uploads a PDF file (`contents`) with a `title`, and
  returns a token. If the submission fails, the token is `ERROR` and the message says why.
* **status**, `GET /v1/dsh/document/status/{token}`: returns the processing status of the document
  the token was issued for. Right after a submit it can show `QUEUED_FOR_INDEXING` for a moment;
  in 0.3.0 a document then ends at `QUEUED_FOR_INDEXING_SUCCESS`: stored, and its id queued for
  indexing. It shows `QUEUED_FOR_INDEXING_ERROR` if the id could not be queued, and an unknown
  token returns `TOKEN_NOT_FOUND`. No later status is reached until the indexing worker exists.

Submit a PDF file, then ask for its status with the token the submit returned:

```bash
curl -F title="My document" -F contents=@/path/to/file.pdf \
  http://localhost:8080/v1/dsh/document/submit
curl http://localhost:8080/v1/dsh/document/status/<token>
```

The API description, in Swagger 2 format, is served at
`http://localhost:8080/v2/api-docs?group=dsh-app`. The Swagger UI is not served in 0.3.0; see
[#144](https://github.com/MRISS-Projects/dsh/issues/144).
````

**Release Notes.** Untouched.

### 4.2 `src/site/markdown/index.md`

Under `# Welcome to DSH`, the `## Document Smart Highlights` paragraph becomes:

```markdown
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
```

`## Overall Process Description` becomes `## Planned Process`, and its lead-in becomes: "The design
DSH is being built towards. It is not the behaviour of 0.3.0, which covers the first two steps of
file submission and stops there." File Submission step 3 gets a closing sentence: "Retrieving the results
is not available yet."

File Indexing becomes:

```markdown
1. The first processing task is to send the file to an indexer, which identifies the individual
   terms or tokens in the file's text.
```

The keyword and sentence extraction sections are unchanged.

### 4.3 Out of scope

As the issue lists, plus:

- The `swagger-ui.jpg` image file, which the README no longer references, and its root-relative
  path, broken by design since `#90`.
- Serving the Swagger UI: [#144](https://github.com/MRISS-Projects/dsh/issues/144), milestone
  `0.4.0-SNAPSHOT`.
- Adding `src/site/` to the documented `markdownlint` command in `CLAUDE.md` and CI. Worth a small
  issue of its own; offered, not raised.
- `src/site/markdown/releases-history.md`.

## 5. Files to change

| File | Change |
|---|---|
| `src/site/markdown/README.md` | §4.1 |
| `src/site/markdown/index.md` | §4.2 |

## 6. Tasks

This is a documentation story, so "red" is the acceptance checks failing on the baseline and
"green" is them passing. Each task commits once. The lint command, used throughout:

```bash
PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH" markdownlint src/site/markdown/README.md \
  src/site/markdown/index.md --config .markdownlint.json
```

### Task 1 — Baseline, red

- [x] Run the lint command. Expect 144 errors (§2.2).
- [x] Run the AC002 grep. Expect matches:

  ```bash
  grep -niE '\.war|servlet container|tomcat-users|tomcat 8|Tomcat \(8' src/site/markdown/README.md
  ```

- [x] Run the AC005 greps. Expect `index.md` to match `SOLR`:

  ```bash
  sed -n '/^## Introduction/,/^## Package/p' src/site/markdown/README.md | grep -niE 'solr|mongo|rabbit|tomcat'
  grep -niE 'solr|mongo|rabbit|tomcat' src/site/markdown/index.md
  ```

- [x] `docker version`, to confirm Task 6 can run here. If Docker is unavailable, stop and say so.
- [x] Record the outputs in §7.1. No commit.

### Task 2 — README: Introduction and modules

- [x] Apply §4.1 Introduction and Package/Folders Description.
- [x] The AC005 README grep returns nothing.
- [x] Every `<module>` in every tracked `pom.xml` has an entry. Scanning every POM, not a list of
  aggregators, means a new aggregator is covered without editing the check; no POM holds a
  commented-out `<module>` line that would make it report a module that is not built:

  ```bash
  for m in $(git ls-files '*pom.xml' | xargs grep -ho '<module>[^<]*' | sed 's/<module>//'); do
    grep -q "\*\*$m\*\*" src/site/markdown/README.md || echo "missing: $m"
  done
  ```

- [x] Commit: `docs: state what 0.3.0 does and describe every module in the README`.

### Task 3 — README: prerequisites, installation, build

- [x] Apply §4.1 Pre-requisites, MongoDB and RabbitMQ, Building From Sources; delete Tomcat.
- [x] Commit: `docs: install MongoDB and RabbitMQ from Docker, drop Tomcat`.

### Task 4 — README: configuration, usage, Swagger

- [x] Apply §4.1 Configuration, Usage and Swagger User Interface.
- [x] The AC002 grep returns nothing, and so does
  `grep -niE 'eclipse|tomcat manager|webapps|swagger-ui\.html' src/site/markdown/README.md`.
- [x] Commit: `docs: run the REST API as a jar and describe its two endpoints`.

### Task 5 — `index.md`

- [x] Apply §4.2.
- [x] The AC005 `index.md` grep returns nothing.
- [x] Commit: `docs: say what DSH does today on the site index`.

### Task 6 — Smoke run, following the README literally

Every command is copied from the README as it now reads. A step that fails is fixed in the README,
in the commit of the task that wrote it, and the run starts again.

- [x] The two `docker run` commands and the `mongosh` user creation from §4.1, with a throwaway
  password.
- [x] Gate 1, logged per `CLAUDE.md`:

  ```bash
  mkdir -p .logs
  mvn -B clean install > .logs/mvn-clean-install.log 2>&1 &
  MVN_PID=$!
  echo "Monitor with:  tail -f .logs/mvn-clean-install.log"
  wait $MVN_PID; echo "maven exit=$?"
  ```

- [x] Start the jar with the override command from §4.1 Usage. It logs `Started DshRestApplication`.
- [x] `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/swagger-ui/` was planned to
  return `200`. It returned **404**, and so did every other Swagger UI path. What was checked
  instead: `/v2/api-docs?group=dsh-app` returns `200`. The UI is raised as
  [#144](https://github.com/MRISS-Projects/dsh/issues/144), and the README revised (§4.1).
- [x] Submit a fixture and ask for its status:

  ```bash
  pdf=dsh-test-dataset/src/test/resources/pdf/bbc-news-1.pdf
  token=$(curl -s -F title=smoke -F "contents=@$pdf" http://localhost:8080/v1/dsh/document/submit \
    | sed -E 's/.*"token":"([^"]*)".*/\1/')
  curl -s http://localhost:8080/v1/dsh/document/status/$token
  curl -s http://localhost:8080/v1/dsh/document/status/no-such-token
  ```

  Expect a token other than `ERROR`, the status `QUEUED_FOR_INDEXING_SUCCESS`, then
  `TOKEN_NOT_FOUND`. The first status call, made immediately, returned `QUEUED_FOR_INDEXING`; a
  repeat seconds later returned `QUEUED_FOR_INDEXING_SUCCESS`. The README was revised to say so.
- [x] Stop the jar, then `mvn spring-boot:run` from `dsh-rest-api` and confirm it starts. If it needs
  arguments the README does not give, add them to the README (§4.1).
- [x] `docker rm -f dsh-mongo dsh-rabbitmq`.
- [x] Record the outputs in §7.2.

### Task 7 — Lint clean

- [x] Fix every remaining lint error in both files: languages on fences (`bash`, `text`, `xml`,
  `bat`), blank lines around fences and headings, no trailing spaces, no hard tabs, bare URLs as
  links, list indentation. Wording outside §4 does not change.
- [x] The lint command exits 0.
- [x] Re-run the AC002 and AC005 greps: still nothing.
- [x] Commit: `docs: make the README and site index pass markdownlint`.

### Task 8 — After the merge into the RC

The last check before the issue is closed. It is not run on the task branch.

- [x] Once the staging run on `staging-0.3.0-SNAPSHOT-RC` has finished, the root `README.md` on the
  RC contains "What 0.3.0 does today" and no line matching the AC002 grep.
- [x] Record the run URL in §7.3.

## 7. Verification

Recorded as each task completes.

### 7.1 Baseline (Task 1)

Run on 2026-09-28 at `2c52de144`.

- Lint: 144 errors, as §2.2.
- AC002 grep: 20 matches, in Introduction, Package/Folders (`dsh-rest-api`), Pre-requisites,
  Tomcat, Tomcat Admin User Configuration and Usage.
- AC005: the README overview matches nothing; `index.md` matches `SOLR` at line 34.
- `docker version`: server 29.8.0.

### 7.2 Smoke run and gate 1 (Task 6)

Run on 2026-09-28, following the README as of Task 5, with a throwaway MongoDB password.

- `docker run` of `mongo:6` and `rabbitmq:3`, and the `mongosh` user creation: `{ ok: 1 }`.
- Gate 1, `mvn -B clean install`: `maven exit=0`, all 13 reactor modules `SUCCESS`.
- The jar, started with the §4.1 override command: `Started DshRestApplication in 6.231 seconds`.
- Submit of `bbc-news-1.pdf`: token `202609286d2eea20-…`, message empty.
- Status of that token, immediately: `QUEUED_FOR_INDEXING`; seconds later:
  `QUEUED_FOR_INDEXING_SUCCESS`. The README was corrected to say so.
- Status of `no-such-token`: `TOKEN_NOT_FOUND`.
- `/swagger-ui/`: **404**, and so are `/swagger-ui/index.html` and `/swagger-ui.html`.
  `/v2/api-docs?group=dsh-app` and `/swagger-resources`: 200. `springfox-boot-starter` is not on
  the classpath, and the webjar page at `/webjars/springfox-swagger-ui/index.html` cannot derive its
  base URL. Raised as [#144](https://github.com/MRISS-Projects/dsh/issues/144); the README states
  the UI is not served (§4.1, revised).
- `mvn spring-boot:run` from `dsh-rest-api`: `Started DshRestApplication in 3.806 seconds`, and a
  submit returned a token. This run passed `-Dspring-boot.run.arguments=--mongo.password=…`,
  because the local settings carry a different password. That no arguments are needed when the
  settings hold the MongoDB user's password is inferred from the build-time filtering, not
  observed.
- `docker rm -f dsh-mongo dsh-rabbitmq`: both removed.

Task 4's second grep also matches "Eclipse Temurin" and "Eclipse Adoptium" in the Java section.
Those name the JDK vendor, not the Eclipse IDE server AC002 excludes, so the text stays.

### 7.2.1 Lint (Task 7)

- Lint command: exit 0 on both files.
- AC002 grep and both AC005 greps: no output.

### 7.2.2 PR review round 1 (#145)

Copilot's automatic review at `ee5a2889f`, effort Balanced, raised two findings, both valid.

- The README had no entries for `solr-terms-vector-order` and `solr-advanced-numbers-filter`,
  which `dsh-solr/pom.xml` declares as reactor modules. Fixed in `8ef83d9bb`.
- The Task 2 module check scanned only the root and `dsh-doc-analyser` POMs, so it passed while
  those two entries were missing. That is how the gap got through Task 2 and the local review. The
  check now scans every tracked POM (Task 2). Run on the fixed README, it prints nothing; run on
  `ee5a2889f`'s README, it prints both modules as missing.

### 7.3 Staging regeneration (Task 8: AC007)

Task 8 ran after `#143` had been closed, on 2026-09-29.

- First staging run on the RC at `027c31c1d`:
  [36574918380](https://github.com/MRISS-Projects/dsh/actions/runs/36574918380), **red**. Maven
  (`-Ddeployment -DintegrationTests`) succeeded, but parent-poms' README placeholder check rejected
  line 425, `#115`'s issue title, which contains a literal `${jenkins.build.number}` and which
  `maven-changes-plugin` copies into the release notes. The site was not deployed and the README
  not committed. Raised as
  [parent-poms#93](https://github.com/MRISS-Projects/parent-poms/issues/93), on a `3.9.1-SNAPSHOT`
  hotfix milestone under the 0.3.0 exception. It was fixed by
  [parent-poms#94](https://github.com/MRISS-Projects/parent-poms/pull/94), merged as `1e79e944`:
  the check now reports a `${name}` only when the README source has it too. The action is pinned
  `@master`, so DSH needed no change and no re-pin.
- Second run, same RC commit:
  [36583922126](https://github.com/MRISS-Projects/dsh/actions/runs/36583922126), **green**. The
  check ran as `check-placeholders.sh README.md src/site/markdown/README.md`, the site deployed,
  and the README was committed as `323dcf0e7`.
- `323dcf0e7:README.md` contains "What 0.3.0 does today" once. The AC002 grep matches nothing.
  Version `0.3.0-SNAPSHOT - RC26 - 20260929-144401`, badge path `rcs/`. The only `${` is `#115`'s
  title at line 425.
- Both runs also ran `#46`'s integration tests in CI for the first time, all passing:
  `DocumentResourceHttpIT` 4, `DocumentResourceIT` 6, `DshRestApplicationIT` 2, and the four
  worker ITs 1, 2, 2, 2, against `mongo:6` and `rabbitmq:3` started by docker-maven-plugin.

## 8. Acceptance criteria

| AC | Delivered by | Checked by |
|---|---|---|
| AC001 — Introduction and `index.md` state what 0.3.0 does and does not do | Tasks 2, 5 | read-through: both carry the same statement |
| AC002 — no WAR, servlet container, Eclipse server, Tomcat Manager or `tomcat-users.xml`; Usage runs the app as packaged | Tasks 3, 4 | AC002 grep empty (Task 4, 7); Usage exercised in Task 6 |
| AC003 — prerequisites: Java 17, Maven 3.9.16, MongoDB and RabbitMQ matching the integration images; no Tomcat | Task 3 | against `dsh-rest-api/pom.xml` |
| AC004 — each module entry describes its module; stubs say so; no repeated text | Task 2 | module loop in Task 2; read-through for repeats |
| AC005 — generic terms in the overview text | Tasks 2, 5 | AC005 greps empty |
| AC006 — the two endpoints and the status a user sees | Task 4 | Task 6 submit and status calls |
| AC007 — `markdownlint` passes on both files; staging regenerates `README.md` | Tasks 7, 8 | lint exit 0; §7.3 |

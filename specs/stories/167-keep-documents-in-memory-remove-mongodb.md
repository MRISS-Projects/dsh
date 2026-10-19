---
issue: 167
slug: keep-documents-in-memory-remove-mongodb
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 167 — Keep documents in memory by default, and remove MongoDB

## 1. Story

**As a** developer or evaluator building, testing or running DSH
**I want** documents kept in memory by default, with MongoDB removed from the code, the build and
the workflows
**So that** DSH builds and runs with nothing installed, and no later story has to carry, test or
configure MongoDB

## 2. Context

- Wave: 1, task 8, step 3 of Wave 1's order of work (`specs/product/PRD.md` §4). DSH milestone
  `0.4.0-SNAPSHOT`.
- Issue: [#167](https://github.com/MRISS-Projects/dsh/issues/167). **The issue is the source of
  truth for the acceptance criteria.** §8 maps each one to this spec. AC010 is added by this spec
  at the owner's request (§3.1, decision 4) and is copied into the issue on approval (T0).
- Parent branch: `DEVELOP`, at `132358a69` when this spec was written. The task branch is
  `issue-167-keep-documents-in-memory-remove-mongodb`.
- Decisions this story follows and does not reopen: ADR-003 section 3 (selection by
  `dsh.persistence`, a value that matches none stops the startup, rules 1 to 3), section 7
  (`dshApplicationContext.xml` is deleted with what it wires) and section 8 (no profile keeps
  documents in memory; MongoDB is deleted, not deprecated). The PRD's re-plan of 2026-10-13,
  decision 1: the swap and the removal are one story, and the interface does not change.
- Out of scope, as the issue lists it: the conditional advance, the contract on `DocumentDao`
  and its contract test (task 9); `FileStorageService` (task 10); RabbitMQ, Solr and the stub
  applications (Wave 2); `dsh-gcp` and Firestore (Wave 5); keeping documents across a restart;
  `#158`, `#140`, `#162`; and the dated records.

### 2.1 What was established while writing this spec

All on `DEVELOP` at `132358a69`, on 2026-10-14.

| Fact | Evidence |
|---|---|
| `DocumentDao` has five methods and names no technology. `MongoDocumentDao` is its only implementation, a `@Component` delegating to `DocumentRepository extends MongoRepository<Document, String>` | `dsh-data/src/main/java/com/mriss/dsh/data/document/dao/` |
| `DocumentHandlingServiceImpl` is the only consumer of `DocumentDao`, by a field `@Autowired`. It calls `storeDocument`, `findDocumentsByHash` and `findDocumentByToken`, never `findDocumentById` | `DocumentHandlingServiceImpl.java` |
| The submission flow relies on `save` writing the id back. `storeDocumentAndQueueForProcessing` stores the document, then queues `document.getId()`. The same instance is stored again by `DocumentEnqueueResponseMessageHandler` and on an enqueue error, always after the first store | `DocumentSubmissionServiceImpl.java`, `DocumentEnqueueResponseMessageHandler.java` |
| `DshDataApplication` is a `@SpringBootApplication` in `com.mriss.dsh.data`. `DshRestApplication`'s `@ComponentScan("com.mriss.dsh")` picks it up as configuration, which is how its `@ImportResource("classpath:/dshApplicationContext.xml")` reaches the REST API's context | `DshDataApplication.java`, `DshRestApplication.java` |
| Spring Boot, its auto-configuration and its logging reach `dsh-data` **only** through `spring-boot-starter-data-mongodb`. `spring-data-commons` (`Document`'s `@Id`) and `commons-io` (`Document`'s `IOUtils`) reach it only through `spring-data-solr` | `mvn -B -pl dsh-data dependency:tree`, `.logs/mvn-dependency-tree-dsh-data.log` |
| `dsh-rest-api`, `dsh-doc-indexer-worker` and `dsh-doc-processor-worker` each have another Boot starter (`web` or `amqp`) beside the MongoDB one | their POMs |
| The only placeholders left in a filtered main resource once `mongo.properties` is gone are `${spring.rabbitmq.host:localhost}` and `${spring.rabbitmq.port:5672}`, which carry defaults | `git grep '\${' -- '*/src/main/resources/*'` |
| `maven_properties` is `required: false`, default `''`, in parent-poms' `project-staging.yml`, `project-release.yml` and `project-hotfix.yml` | `gh api repos/MRISS-Projects/parent-poms/contents/.github/workflows/<file>` |
| The REST API's context starts with no broker reachable. `enqueue-docId-context.xml` has an outbound adapter only, and `RabbitAdmin` declares on the first connection. `DshRestApplicationIT` already relies on this | `enqueue-docId-context.xml` |
| `DocumentResourceHttpIT.submit_whenValidPdf_shouldQueueForIndexing` already submits a PDF over HTTP and polls `/status/{token}` until `QUEUED_FOR_INDEXING_SUCCESS` | the test |
| `deploy.yml` deploys the ref it is dispatched on, through `project-staging.yml` with `release_type: snapshots`: a build with `-DintegrationTests`, the artifacts, the site under `snapshots/products/dsh/`, and a generated `README.md` committed back to that ref. `#127` proved itself by dispatching it on its task branch | `deploy.yml`; `specs/stories/127-deploy-snapshot-site.md` §3.1, §6.1 |
| `java-conventions.md` names `DocumentRepository` as its naming example (line 17) and `MongoRepository` as the repository pattern (line 25) | the file |

## 3. Design

### 3.1 Decisions agreed with the owner on 2026-10-14

1. **The implementation selects itself.** `InMemoryDocumentDao` carries the condition, not a
   `@Configuration` in `dsh-rest-api`. Wave 5's Firestore class takes the same shape in `dsh-gcp`,
   which `dsh-rest-api` cannot compile against (ADR-003, assumption A5), so a configuration class
   in the deployable could not name it.
2. **The coding guidance points at ADR-003.** `java-conventions.md` and the two Copilot prompts
   stop prescribing a Spring Data MongoDB repository, and state ADR-003's rules 1 and 2 instead
   (§3.7).
3. **The `http-integration-tests` profile stays.** Only its MongoDB parts go (§3.4).
4. **At least one deploy proves the story** (AC010). `deploy.yml` is dispatched on the task
   branch: it runs every test, integration included, on a fresh runner with no `mongo.*` value,
   deploys the artifacts, and publishes the site. It is also the only run of a release wrapper
   without `maven_properties` that can happen before the merge.

### 3.2 The in-memory store

`InMemoryDocumentDao`, in `dsh-data`, package `com.mriss.dsh.data.document.dao.memory`. As changed
by the first review round (§3.10); the design first written here scanned the store on every find
by hash and by token.

```java
@Component
@ConditionalOnProperty(name = "dsh.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryDocumentDao implements DocumentDao {

    private final Map<String, Document> documentsById = new ConcurrentHashMap<>();
    private final Map<String, String> idsByHash = new ConcurrentHashMap<>();
    private final Map<String, String> idsByToken = new ConcurrentHashMap<>();

    @Override
    public synchronized String storeDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (document.getFileHash() == null || document.getToken() == null) {
            throw new IllegalArgumentException("document must have a file hash and a token");
        }
        String id = document.getId();
        String owner = id != null && documentsById.containsKey(id) ? id : null;
        if (!Objects.equals(owner, idsByHash.get(document.getFileHash()))
                || !Objects.equals(owner, idsByToken.get(document.getToken()))) {
            throw new IllegalStateException("another document already has this file hash or token, or this id: " + document);
        }
        if (id == null) {
            id = UUID.randomUUID().toString();
            document.setId(id);
        }
        documentsById.put(id, document);
        idsByHash.put(document.getFileHash(), id);
        idsByToken.put(document.getToken(), id);
        return id;
    }

    @Override
    public Document findDocumentById(String id) {
        return id == null ? null : documentsById.get(id);
    }

    @Override
    public Document findDocumentByHash(String documentHash) {
        return findByIndex(idsByHash, documentHash);
    }

    @Override
    public Document findDocumentByToken(String token) {
        return findByIndex(idsByToken, token);
    }

    @Override
    public synchronized void clearDocuments() {
        documentsById.clear();
        idsByHash.clear();
        idsByToken.clear();
    }

    private Document findByIndex(Map<String, String> index, String key) {
        String id = key == null ? null : index.get(key);
        return id == null ? null : documentsById.get(id);
    }
}
```

- **An id, a file hash and a token each belong to one document at most.** Every find is one map
  read, and a null key finds nothing. `DocumentDao.findDocumentsByHash`, which returned a list, is
  `findDocumentByHash`, which returns the document.
- **A null document**, or one with no file hash or no token, throws `IllegalArgumentException`.
- **A hash or a token that belongs to another document** throws `IllegalStateException`, and so
  does another document stored under a stored id with other keys. Nothing is stored and the
  document gets no id.
- **Storing a document again under its id replaces its record**, as the submission flow relies on.
  Because a document's hash comes from its bytes alone (§3.10) and neither it nor the token can
  change, a stored record's keys never change, and no index entry can go stale.
- **Writes are `synchronized`; reads are not.** A document is written before its index entries, so
  a read never finds an id whose document is missing.

**Known limit, recorded rather than engineered around.** A read returns the stored instance, not a
copy. A caller's change to a document it read is visible to the next reader without a store. Under
MongoDB it was not. Nothing in the flow depends on the difference: every change is followed by a
store of the same instance.

### 3.3 The unit tests

`InMemoryDocumentDaoTest`, in `dsh-data`'s test tree, same package. JUnit 4, AssertJ, no Spring.
Documents are built from small byte arrays, not the PDF fixtures.

| Test | AC |
|---|---|
| `storeDocument_whenNoId_shouldReturnTheIdAndSetItOnTheSameInstance` | AC001, AC002 |
| `storeDocument_whenIdAlreadySet_shouldStoreUnderThatId` | AC001 |
| `storeDocument_whenStoredAgain_shouldReplaceTheRecordNotAddOne` | AC002 |
| `storeDocument_whenNull_shouldThrowIllegalArgumentException` | AC001 |
| `storeDocument_whenNoFileHash_shouldThrowIllegalArgumentException` | AC001 |
| `storeDocument_whenNoToken_shouldThrowIllegalArgumentException` | AC001 |
| `storeDocument_whenAnotherDocumentHasTheSameFileHash_shouldRejectIt` | AC001 |
| `storeDocument_whenAnotherDocumentHasTheSameToken_shouldRejectIt` | AC001 |
| `storeDocument_whenAnotherDocumentTakesAStoredIdWithOtherKeys_shouldRejectIt` | AC001 |
| `findDocumentById_whenStored_shouldReturnIt`, `_whenUnknown_shouldReturnNull`, `_whenNull_shouldReturnNull` | AC001 |
| `findDocumentByToken_whenStored_shouldReturnIt`, `_whenUnknown_shouldReturnNull`, `_whenNull_shouldReturnNull` | AC001 |
| `findDocumentByHash_whenStored_shouldReturnIt`, `_whenUnknown_shouldReturnNull`, `_whenNull_shouldReturnNull` | AC001 |
| `clearDocuments_whenDocumentsStored_shouldRemoveEveryOne` | AC001 |
| `storeDocument_whenManyThreadsStoreAtOnce_shouldKeepEveryDocumentUnderAnIdOfItsOwn` | AC003 |
| `storeDocument_whenManyThreadsStoreTheSameFileAtOnce_shouldAcceptExactlyOne` | AC003 |

The first concurrency test runs 8 threads, held behind one start latch, each storing 50 distinct
documents. Then every document is found by its id as the same instance, and the 400 ids are
distinct. The second runs 8 threads behind one latch, each storing its own document built from the
same bytes: exactly one is accepted, and it is the document found by that hash.

### 3.4 Selection, and the tests with a context

- `dsh-data/pom.xml` replaces `spring-boot-starter-data-mongodb` with `spring-boot-starter`. That
  is what brings `@ConditionalOnProperty`, `@SpringBootApplication` and logging once the MongoDB
  starter is gone (§2.1).
- `dsh-rest-api`'s `application.properties` sets **no** `dsh.persistence`. A comment names the
  key, its default and its one valid value. If the file set it, "key absent" could not be tested,
  and ADR-003 makes `memory` the default with no profile.
- **A value that matches none stops the startup** because no `DocumentDao` bean exists and
  `DocumentHandlingServiceImpl` injects one (ADR-003 rule 3, R11). No validator is added: rule 3
  accepts this failure, and validated binding (R6) is left to ADR-004.

**AC004: `PersistenceSelectionIT`**, in `com.mriss.dsh.restapi.integration`. It starts the real
application the way `main` does, and closes it after each case. A command-line argument outranks
every properties file, so no file can mask a case. `@SpringBootTest` is not used, because it
cannot express a startup that is expected to fail.

```java
package com.mriss.dsh.restapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;

import com.mriss.dsh.data.document.dao.DocumentDao;
import com.mriss.dsh.data.document.dao.memory.InMemoryDocumentDao;
import com.mriss.dsh.restapi.DshRestApplication;

/**
 * Which {@link DocumentDao} the application runs with, by {@code dsh.persistence} (ADR-003,
 * section 3). Starts the application as {@code main} does, once per case.
 */
public class PersistenceSelectionIT {

    private ConfigurableApplicationContext context;

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void startup_whenPersistenceKeyAbsent_shouldUseTheInMemoryStore() {
        context = start();

        assertOnlyStoreIsInMemory();
    }

    @Test
    public void startup_whenPersistenceIsMemory_shouldUseTheInMemoryStore() {
        context = start("--dsh.persistence=memory");

        assertOnlyStoreIsInMemory();
    }

    @Test
    public void startup_whenPersistenceMatchesNoStore_shouldFail() {
        Throwable thrown = catchThrowable(() -> context = start("--dsh.persistence=mongodb"));

        assertThat(thrown).as("the startup must fail").isNotNull();
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(thrown);
        assertThat(cause).isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThat(((NoSuchBeanDefinitionException) cause).getBeanType()).isEqualTo(DocumentDao.class);
    }

    private void assertOnlyStoreIsInMemory() {
        Map<String, DocumentDao> stores = context.getBeansOfType(DocumentDao.class);
        assertThat(stores).hasSize(1);
        assertThat(stores.values().iterator().next()).isInstanceOf(InMemoryDocumentDao.class);
    }

    private static ConfigurableApplicationContext start(String... arguments) {
        String[] all = new String[arguments.length + 1];
        all[0] = "--server.port=0";
        System.arraycopy(arguments, 0, all, 1, arguments.length);
        return new SpringApplicationBuilder(DshRestApplication.class).run(all);
    }
}
```

The failure case asserts the bean type, not just "something failed", so a startup that fails for
an unrelated reason does not pass it.

**The existing tests keep their shape.** `DshRestApplicationIT` and `DocumentResourceIT` keep
`@MockBean DocumentDao`, and `DocumentResourceIT` drives statuses through it. Only the Javadoc
and comments that say MongoDB is mocked, deprecated or needed change. The same goes for
`DocumentHandlingServiceImplTest`, `DocumentSubmissionServiceImplTest` and
`DshDataApplicationTest`.

**AC006: the `http-integration-tests` profile in `dsh-rest-api/pom.xml` stays.** Only its MongoDB
parts go:

| Inside the profile | After this story |
|---|---|
| `build-helper` port reservation | unchanged |
| `spring-boot:start` / `stop` | kept. The four `--mongo.*` arguments and the comment above them go. `--server.port` and the two `--spring.rabbitmq.*` arguments stay |
| `docker-maven-plugin`, `rabbitmq:3` image | unchanged |
| `docker-maven-plugin`, `mongo:6` image, its `dsh.it.mongo.port` mapping and the `mongo-init.js` bind | removed |
| failsafe's `dsh.it.baseUrl` | unchanged |
| the profile's comments | kept. They are about plugin order, which still holds with one image |

`src/test/docker/mongo-init.js` is deleted, and the directory with it. In
`DocumentResourceHttpIT`, the class Javadoc says the server keeps documents in its own memory and
RabbitMQ runs in Docker. `READ_TIMEOUT_MILLIS` stays at 60 s, and its comment gives the reason
that remains: a server that hangs fails the test, not the build. No test is added: the existing
`submit_whenValidPdf_shouldQueueForIndexing` finds the submitted document by its token through
`/status/{token}`, which is AC006.

### 3.5 Removal from code and build

| File | Change |
|---|---|
| `dsh-data/.../dao/mongo/MongoDocumentDao.java`, `DocumentRepository.java` | deleted |
| `dsh-data/src/test/.../dao/mongo/MongoDocumentDaoTest.java` | deleted |
| `dsh-data/src/main/resources/dshApplicationContext.xml`, `mongo.properties` | deleted |
| `DshDataApplication.java` | `@ImportResource` and its import go. The class stays; the stub applications are Wave 2 task 3's |
| `dsh-data/pom.xml` | MongoDB starter → `spring-boot-starter`. The second `<resource>` block, which exists only to leave `dshApplicationContext.xml` unfiltered, goes, and so does the `<exclude>` of that file in the first block |
| `dsh-rest-api/pom.xml` | MongoDB starter removed; the profile as §3.4 |
| `dsh-doc-indexer-worker/pom.xml`, `dsh-doc-analyser/dsh-doc-processor-worker/pom.xml` | MongoDB starter removed |
| `dsh-rest-api/src/main/resources/application.properties` | the `# MongoDB` block goes; the `dsh.persistence` comment (§3.4) takes its place |
| `dsh-rest-api/src/test/resources/application.properties` | the `# MongoDB` block and the `spring.autoconfigure.exclude` block with its comment go |
| `connect-mongo.sh`, `connect-mongo.bat`, `connect-mongo-super-user.sh`, `connect-mongo-super-user.bat` | deleted |

**AC005 evidence:**
`mvn -B dependency:tree -Dincludes=org.mongodb > .logs/mvn-dependency-tree-mongodb.log` over the
whole reactor. Every module's tree is printed with no `org.mongodb` line under it. The spec
records the count of `org.mongodb` lines: it is 0.

**For Wave 2 task 3, not acted on here.** `spring-data-commons` and `commons-io` reach `dsh-data`
only through `spring-data-solr` (§2.1). The story that removes Solr has to declare them, or remove
what uses them.

### 3.6 Workflows

| Workflow | Change |
|---|---|
| `ci.yml` | the `mongodb` service, the four `<mongo.*>` properties in the `settings.xml` it writes, and the "Create MongoDB user and database" step go. The `rabbitmq` service stays |
| `api-testing.yml` | the same three, the comment above the four properties, and the four `-Dmongo.*` arguments of "Start DSH REST API" |
| `copilot-setup-steps.yml` | the four `<mongo.*>` properties. The comment's last sentence, "The mongo.* values go with the rest of them in Wave 1 task 8.", goes |
| `deploy.yml`, `staging.yml`, `release.yml`, `hotfix.yml` | the `maven_properties` input and the `#114` comment above it go. The input defaults to `''` upstream (§2.1) |

**AC007 evidence.** Three sources, each covering what the others cannot:

1. Gate 1 runs as `mvn -B -s <scratchpad>/settings-no-mongo.xml clean install`. The file is a copy
   of `~/.m2/settings.xml` with every `<mongo.*>` line removed, and the spec records that
   `grep -c mongo` on it gives 0. The owner's own settings file is not edited.
2. `ci.yml` and `api-testing.yml` on the pull request. `api-testing.yml` runs because the diff
   touches `dsh-rest-api/**`.
3. The four release wrappers cannot run on a pull request. `deploy.yml` is dispatched on the
   task branch (AC010, §3.9), and since `staging.yml`, `release.yml` and `hotfix.yml` change in
   the same way (one input removed), they are checked by reading, against that run.

### 3.7 Documentation and guidance

Corrected where they describe MongoDB as part of DSH, or tell anyone to install, start or
configure it.

| File | Change |
|---|---|
| `CLAUDE.md` | Lines 4 to 7: DSH keeps documents in memory by default (`dsh.persistence`) and queues them on a self-managed RabbitMQ, and the Solr code is still here but nothing runs it. The GCP migration stays proposed in ADR-001; its Phase 1 in-memory store is in place (`#167`), and nothing runs on GCP. Gate 1: "`-DintegrationTests` also starts RabbitMQ in Docker". Gate 3: "(RabbitMQ, Solr, or any other service outside the JVM)" |
| `.claude/skills/dsh-ship-story/SKILL.md` | step 5, item 1: the same list as `CLAUDE.md`'s gate 3 |
| `.github/copilot/rules/testing-patterns.md` | the local gate's list, as `CLAUDE.md`. Layer 2: "with `@MockBean`s standing in for RabbitMQ". Layer 3: "runs **real RabbitMQ** in Docker around the integration-test phase, and the server keeps documents in its own memory", and "a request goes through the store and the broker for real" |
| `specs/architecture/system-design.md` | The diagram's `MongoDB` box becomes `Document store` / `(in memory by default)`. The migration note: the MongoDB code was removed by `#167` (ADR-003 section 8), and the RabbitMQ and Solr code is still in the repository. The `dsh-data` bullet "Contains shared JPA/MongoDB entity classes and repositories" becomes "Contains the shared models, the `DocumentDao` interface and its in-memory implementation". The stack row's Persistence cell becomes "In memory (default); Apache Solr (unused, removal decided)" |
| `src/site/markdown/README.md` | §3.7.1 |
| `docs/devops/README.md` | §3.7.2 |
| `docs/troubleshooting/README.md` | the suggested `mongodb-connection-issues.md` line and the `**MongoDB**` diagnostic line go |
| `dsh-data/src/site/markdown/index.md` | "This module has models and DAO classes to access MongoDB and SOLR." becomes "This module has the models, the document store interface with its in-memory implementation, and an empty interface for a SOLR index." |
| `dsh-rest-api/src/site/markdown/index.md` | "Stores the file at MongoDB, creating a document in the collection with:" becomes "Stores the file in the document store, as a document with:" |
| `dsh-doc-indexer-worker/src/site/markdown/index.md` | "from MongoDB" → "from the document store"; "MongoDB document id" → "document id" |
| `dsh-doc-analyser/dsh-doc-processor-worker/src/site/markdown/index.md` | "document id at MongoDB" → "document id in the document store"; "at the respective document line in the MongoDB collection" → "in the document's record in the document store" |
| `specs/architecture/data-models/README.md` | "(MongoDB documents, Solr schemas)" → "(stored documents, Solr schemas)" |
| `.github/copilot/rules/java-conventions.md` | §3.7.3 |
| `.github/copilot/prompts/component-generation.md`, `test-generation.md` | §3.7.3 |

#### 3.7.1 `src/site/markdown/README.md`

The root `README.md` is generated from this file and is never edited (`docs/devops/README.md`,
"README.md is a Generated File").

- **Package/Folders, `dsh-data`:** "Also the MongoDB persistence of documents." becomes "Also the
  document store: an interface, and the in-memory implementation DSH runs with by default."
- **Pre-requisites:** "Docker, to run RabbitMQ, and for the integration build". The `MongoDB 6`
  line goes.
- **"MongoDB and RabbitMQ"** becomes **"RabbitMQ"**. The section keeps only the `dsh-rabbitmq`
  `docker run`, and the stop/start sentence without the MongoDB user. Everything about `dsh-mongo`
  and `mongosh` goes.
- **Building From Sources:** "This runs the unit tests only, and needs no RabbitMQ running. The
  integration tests run with `mvn clean install -DintegrationTests`, which starts its own RabbitMQ
  container, so it needs a running Docker daemon."
- **"MongoDB Access Properties"** is deleted, whole.
- **Usage:** "With RabbitMQ running and the build done, start the application in either of two
  ways." Under Running the Jar, the paragraph about `mongo.*` goes, and the override example keeps
  only `--spring.rabbitmq.host=localhost --spring.rabbitmq.port=5672`, introduced as "The RabbitMQ
  address can be overridden when starting it:".
- **A new subsection, "Where Documents Are Kept"**, after Running with the Spring Boot Maven
  Plugin:

  > DSH keeps documents in memory. Nothing has to be installed for it, and every document is lost
  > when the application stops: a run with no profile is for trying DSH out and for tests, not a
  > deployment. The store is selected by `dsh.persistence`. Its default, and its only value until
  > the Firestore store arrives, is `memory`. Any other value stops the startup.

#### 3.7.2 `docs/devops/README.md`

- **The `api-testing.yml` row:** "boots `dsh-rest-api` against a RabbitMQ service container".
- **The `staging.yml` row:** the clause about passing build properties as `maven_properties` goes.
  The row ends with what the workflow delegates.
- **"How build properties reach a release build"** becomes **"Build properties"**, one paragraph:

  > No build of this repository needs a property of its own. Until `#167`, `dsh-data`'s
  > `mongo.properties` was filtered from four `mongo.*` values that every build had to supply: a
  > local one through `~/.m2/settings.xml`, and the release wrappers through `maven_properties`
  > (`#114`). `#167` removed MongoDB, and the four values with it. `staging.yml`, `deploy.yml`,
  > `release.yml` and `hotfix.yml` pass no `maven_properties`. The input stays in `parent-poms`, a
  > generic `name=value` block that holds no property name of this project (`#76`), for the day a
  > build property is needed again.

  No link points at the old anchor: `git grep -n 'how-build-properties-reach'` found nothing on
  2026-10-14. T6 reruns it before renaming the heading.
- **Local Developer Scripts:** "Three `.sh` files sit at the repository root. **None of them is
  called by CI** … They exist for local convenience only, and are described below." The sentence
  about `connect-mongo.sh` goes.

#### 3.7.3 The coding guidance

- **`java-conventions.md`, line 17**, `Repositories: *Repository suffix (e.g., DocumentRepository)`,
  becomes:
  `Stores: the interface takes the *Dao suffix (e.g., DocumentDao); an implementation is prefixed
  with what it is built on (e.g., InMemoryDocumentDao)`.
- **`java-conventions.md`, line 25**, the Spring Data line, becomes two:
  - `Reach a service outside the JVM through an interface that names no technology, in its name,
    its signatures, its types or its exceptions (ADR-003, section 3, rule 1)`
  - `A consumer names the interface, never an implementation; the implementation is selected by
    its dsh.* key (ADR-003, section 3, rules 2 and 3)`
- **`component-generation.md`, "New Repository"**, becomes **"New Store"**:

  ```text
  As a Backend Developer (see /.github/roles.md), generate a store for [EntityName]
  in the DSH project, following ADR-003 section 3
  (/specs/architecture/ADR-003-document-pipeline.md).

  Requirements:
  - An interface in dsh-data, [EntityName]Dao, that names no technology
  - Operations: [list operations]
  - An in-memory implementation, InMemory[EntityName]Dao, in dsh-data: a @Component
    selected by @ConditionalOnProperty on its dsh.* key with matchIfMissing = true
  - Include documentation referencing /specs/architecture/data-models/
  ```

- **`test-generation.md`, "Repository Test"**, becomes **"Store Test"**:

  ```text
  As a Backend Developer (see /.github/roles.md), generate tests for the store
  [StoreName] in the DSH project.

  Requirements:
  - [StoreName]Test: a unit test of the in-memory implementation, with no Spring
    context and no slice
  - Test cases:
    - Store and find an entity
    - Each operation: [method name] with [scenario]
    - A miss: an unknown key finds nothing
    - Edge case: [describe edge case]
  - The selection by the dsh.* key is tested once, in a *IT in the module's
    integration package, because it needs a context
  - Use AssertJ assertions
  ```

### 3.8 What `git grep -i mongo` still finds

After T6 every remaining hit is in one of these rows. T7 checks it: any hit outside them fails the
check.

| Path | Why it stays |
|---|---|
| `specs/architecture/ADR-001-GCP-based-components.md`, `ADR-003-document-pipeline.md`, `profiles-and-configuration.md` | dated records, not edited (#167, Out of Scope) |
| `specs/stories/**`, this spec included | dated records |
| `docs/superpowers/**` | dated records |
| `specs/product/PRD.md` | a dated record here. `dsh-reconcile-prd` updates task 8's status after the merge |
| `README.md` | generated. AC010's run regenerates it, after which only release-notes rows (issues `#139`, `#123`, `#17`, `#21`, `#18`, `#19`, `#15`) mention MongoDB |
| `specs/features/document-analysis.md`, `specs/features/indexing-workflow.md` | `#162` |
| `.github/scripts/check-unit-tests-context-free.sh` | its comment names `@DataMongoTest` among Spring Boot's test slices, which the checker must still catch. A statement about Spring, not about DSH |
| `scripts/close-wontfix-issues.sh` | the record of a one-off closure: the quoted message of `#47`'s closing comment |
| `docs/devops/README.md` | added in T7. §3.7.2's "Build properties" paragraph says the `mongo.*` values were removed by `#167`. History, not a description of DSH |
| `specs/architecture/system-design.md` | added in T7. §3.7's migration note says ADR-003 removes the MongoDB code and `#167` removed it |
| `dsh-rest-api/.../integration/PersistenceSelectionIT.java` | added in T7. §3.4's `--dsh.persistence=mongodb` is the value that matches no store |

### 3.9 The deploy (AC010)

`deploy.yml`, dispatched on the task branch:
`gh workflow run deploy.yml --ref issue-167-keep-documents-in-memory-remove-mongodb`.

- **When.** After the pull request is open and its first CI run is green, so that the deploy and CI
  build the same head. If a later commit changes code, a POM or a workflow, it is dispatched again
  before the merge. A commit of documentation only does not need a new run.
- **What it proves.** The build runs every test, unit and integration, with
  `-DintegrationTests` on a fresh runner, with no `mongo.*` value anywhere. That includes
  `PersistenceSelectionIT` and `DocumentResourceHttpIT` against a server backed by the in-memory
  store and a RabbitMQ container. The run also deploys the artifacts, publishes the site, and
  shows that a release wrapper runs without `maven_properties`.
- **What it changes.** The `0.4.0-SNAPSHOT` artifacts and the site under `snapshots/products/dsh/`
  are the task branch's until the next deploy from `DEVELOP`, as with `#127`. The run commits a
  regenerated `README.md` to the task branch, as `Auto-generated README.md [skip jenkins]`, which
  becomes part of the pull request. Pull it before the next local commit.
- **After the merge** (not an AC, because Claude does not merge): a dispatch on `DEVELOP` restores
  the mainline snapshot. Recommended to the owner in the step 6 summary.

The run is green, and:

1. The failsafe report in the log shows `PersistenceSelectionIT` (3 tests) and
   `DocumentResourceHttpIT` (4 tests) run, with 0 failures and 0 errors.
2. `docker-maven-plugin` starts one container, `dsh-it-rabbitmq-*`, and no `mongo` image is
   pulled.
3. The artifacts of all 13 modules are deployed at `0.4.0-SNAPSHOT`.
4. `https://mriss-projects.github.io/dsh/snapshots/products/dsh/index.html` answers 200, and so
   does each module's `index.html`.
5. The `README.md` committed to the branch passes the §3.8 check for `README.md`.

### 3.10 The first review round, 2026-10-19

The owner's review of `bf721150d`, the commit that indexed hashes and tokens at the request of the
review comment of 2026-10-16. That comment had asked for a hash shared by several documents and
for index keys that change on a re-store. The store needed six structures and looped in its finds.
Its premise that every `/submit` calls `getDocumentByHash` was wrong as well: `DocumentResource`
passes `useCache = false`, so only `/status/{token}` was on the request path.

Decided by the owner:

1. **An id, a file hash and a token each belong to one document**, every lookup returns one
   document, and storing another document with a stored hash is not allowed. §3.2 and §3.3.
2. **A document's file hash comes from its bytes alone.** The full-args constructor takes no
   `fileHash` and computes it; `setOriginalFileContents` and the no-arg constructor, used only by
   tests since MongoDB went, are removed; `fileHash` and `originalFileContents` are `final`.
3. **A resubmitted file keeps its document (option (a)).** `getTokenFromDocument` looks the file up
   by hash, whatever `useCache` says, and returns the stored document's token. With `useCache` and
   keywords and sentences present the result is reused and nothing is queued, as before. Otherwise
   the stored document is queued again. Without this, every second `/submit` of a file would have
   had its store rejected in the `@Async` method, after the client had a token that never resolves.

Two consequences, found while building it:

- **`Document.resetStatus()`** puts a document back at `CREATED` before it is queued again. The
  status machine has no way back: `NEUTRAL` from `QUEUED_FOR_INDEXING_SUCCESS` goes to
  `DEQUEUED_FOR_INDEXING`, not to `QUEUED_FOR_INDEXING`.
- **`DocumentHandlingService.getDocumentByHash` returns the document whatever its state.** The
  check for keywords and sentences moves to `DocumentSubmissionServiceImpl`, which owns the cache
  decision and now needs the unprocessed document too.

**Still open, recorded rather than engineered around.** Two submissions of a new file that arrive
together both find no document, and the second store is rejected in the `@Async` method, as above.
Closing it needs the token decided and the document stored in one step; no test shows it today.

## 4. Files to change

| File | Section |
|---|---|
| `dsh-data/src/main/java/com/mriss/dsh/data/document/dao/memory/InMemoryDocumentDao.java` (new) | §3.2 |
| `dsh-data/src/test/java/com/mriss/dsh/data/document/dao/memory/InMemoryDocumentDaoTest.java` (new) | §3.3 |
| `dsh-rest-api/src/test/java/com/mriss/dsh/restapi/integration/PersistenceSelectionIT.java` (new) | §3.4 |
| `MongoDocumentDao.java`, `DocumentRepository.java`, `MongoDocumentDaoTest.java`, `dshApplicationContext.xml`, `mongo.properties`, `mongo-init.js`, the four `connect-mongo*` scripts (deleted) | §3.4, §3.5 |
| `DshDataApplication.java`, the four POMs, both `application.properties` | §3.4, §3.5 |
| `DocumentResourceHttpIT.java`, `DshRestApplicationIT.java`, `DocumentResourceIT.java`, `DocumentHandlingServiceImplTest.java`, `DocumentSubmissionServiceImplTest.java`, `DshDataApplicationTest.java` (comments only) | §3.4 |
| the seven workflows | §3.6 |
| `DocumentDao.java`, `Document.java`, `DocumentTest.java`, `DocumentHandlingService.java` and its `Impl` and test, `DocumentSubmissionService.java` and its `Impl` and test, `DocumentResourceHttpIT.java` (review round 1) | §3.10 |
| `CLAUDE.md`, `dsh-ship-story/SKILL.md`, `testing-patterns.md`, `java-conventions.md`, the two prompts, `system-design.md`, `data-models/README.md`, `src/site/markdown/README.md`, `docs/devops/README.md`, `docs/troubleshooting/README.md`, the four module site pages | §3.7 |

## 5. Global constraints

- Java 17, Spring Boot 2.7.18 as today. JUnit 4 and AssertJ, as the module's tests already use.
- Production code is indented with tabs, as `dsh-data` is. The new tests use tabs, as
  `DocumentResourceHttpIT` does. The code blocks in this spec show spaces, because markdownlint
  forbids hard tabs here.
- Test methods are named `<method>_<scenario>_<outcome>` (`testing-patterns.md`).
- A unit test never starts a Spring context. `PersistenceSelectionIT` is an `*IT` in an
  `integration` package.
- Every local `mvn` run is logged to `.logs/` and its exit code reported (`CLAUDE.md`).
- `DocumentDao` is not edited: not its methods, not its Javadoc.
- No `dsh.persistence` line in any properties file.
- The dated records of §3.8 are not edited.

## 6. Verification

### 6.1 Gates

- **Gate 1 and 2, with AC007:** `mvn -B -s <scratchpad>/settings-no-mongo.xml clean install`,
  logged as `.logs/mvn-clean-install.log`.
- **Gate 3:** `mvn -B -s <scratchpad>/settings-no-mongo.xml clean verify -DintegrationTests -pl
  dsh-data -amd`, logged as `.logs/mvn-clean-verify-it.log`. Docker running. RabbitMQ is the only
  container.
- **Lint:** the `CLAUDE.md` markdownlint command, then the same with `--config .markdownlint.json`
  over `src/site/markdown/README.md` and the four module `src/site/markdown/index.md` pages,
  which the command's globs do not cover.

### 6.2 Checks

- **AC005:** §3.5, the reactor dependency tree with `-Dincludes=org.mongodb`.
- **AC008:** §3.8, `git grep -il mongo` against the table.
- **AC010:** §3.9.

## 7. Tasks

One commit per task. Every `mvn` run is logged (`CLAUDE.md`, "Always log local Maven runs").

### T0 — The issue

- [ ] Add AC010 to `#167`'s body, below AC009, as approved with this spec:

  ```markdown
  - [ ] AC010: `deploy.yml`, dispatched on the task branch, is green. Its build runs every test,
    unit and integration, on a fresh runner with no `mongo.*` value. It deploys the
    `0.4.0-SNAPSHOT` artifacts of every module and publishes the site under
    `snapshots/products/dsh/`, which answers 200. The `README.md` it commits to the branch
    mentions MongoDB only in release-notes rows.
  ```

  `gh issue edit 167 --body-file <scratchpad>/167-body.md`, from the current body with the
  criterion appended.

### T1 — Baseline

- [ ] Record `git rev-parse --short HEAD`.
- [ ] `mvn -B clean install`, logged as `.logs/mvn-clean-install-baseline.log`. Record the exit
  code and `dsh-data`'s and `dsh-rest-api`'s line and branch coverage from the `jacoco:check`
  output.
- [ ] `git grep -il mongo | sort > <scratchpad>/mongo-hits-before.txt`. Record the count.

### T2 — The in-memory store (AC001, AC002, AC003)

**Files:** create `InMemoryDocumentDao.java` and `InMemoryDocumentDaoTest.java` (§3.2, §3.3).

The code below is the task as it was built. Review round 1 replaced the store and its tests; §3.2
and §3.3 hold the current ones, and §3.10 says why.

**Interfaces:** produces `com.mriss.dsh.data.document.dao.memory.InMemoryDocumentDao implements
DocumentDao`, with a public no-argument constructor. T3 annotates it.

- [ ] Write `InMemoryDocumentDaoTest` as below.
- [ ] `mvn -B -pl dsh-data test-compile`, logged. Expect a compilation failure:
  `InMemoryDocumentDao` cannot be found. Record it as the red state.
- [ ] Write `InMemoryDocumentDao` as §3.2, **without** `@Component` and `@ConditionalOnProperty`.
  With the MongoDB DAO still a `@Component`, two `DocumentDao` beans would fail every context.
- [ ] `mvn -B -pl dsh-data -Dtest=InMemoryDocumentDaoTest test`, logged. All 12 pass.
- [ ] Commit: `feat(dsh-data): add an in-memory DocumentDao`.

```java
package com.mriss.dsh.data.document.dao.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.mriss.dsh.data.models.Document;

public class InMemoryDocumentDaoTest {

    private InMemoryDocumentDao dao;

    @Before
    public void setUp() {
        dao = new InMemoryDocumentDao();
    }

    @Test
    public void storeDocument_whenNoId_shouldReturnTheIdAndSetItOnTheSameInstance() throws Exception {
        Document document = document("a");

        String id = dao.storeDocument(document);

        assertThat(id).isNotBlank();
        assertThat(document.getId()).isEqualTo(id);
    }

    @Test
    public void storeDocument_whenIdAlreadySet_shouldStoreUnderThatId() throws Exception {
        Document document = document("a");
        document.setId("given-id");

        String id = dao.storeDocument(document);

        assertThat(id).isEqualTo("given-id");
        assertThat(dao.findDocumentById("given-id")).isSameAs(document);
    }

    @Test
    public void storeDocument_whenStoredAgain_shouldReplaceTheRecordNotAddOne() throws Exception {
        Document document = document("a");
        String first = dao.storeDocument(document);

        String second = dao.storeDocument(document);

        assertThat(second).isEqualTo(first);
        assertThat(dao.findDocumentsByHash(document.getFileHash())).containsExactly(document);
    }

    @Test
    public void storeDocument_whenNull_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> dao.storeDocument(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void findDocumentById_whenStored_shouldReturnIt() throws Exception {
        Document document = document("a");
        String id = dao.storeDocument(document);

        assertThat(dao.findDocumentById(id)).isSameAs(document);
    }

    @Test
    public void findDocumentById_whenUnknown_shouldReturnNull() throws Exception {
        dao.storeDocument(document("a"));

        assertThat(dao.findDocumentById("unknown")).isNull();
    }

    @Test
    public void findDocumentByToken_whenStored_shouldReturnIt() throws Exception {
        Document document = document("a");
        dao.storeDocument(document("b"));
        dao.storeDocument(document);

        assertThat(dao.findDocumentByToken(document.getToken())).isSameAs(document);
    }

    @Test
    public void findDocumentByToken_whenUnknown_shouldReturnNull() throws Exception {
        dao.storeDocument(document("a"));

        assertThat(dao.findDocumentByToken("unknown")).isNull();
    }

    @Test
    public void findDocumentsByHash_whenStored_shouldReturnEveryDocumentWithThatHash() throws Exception {
        Document first = document("same");
        Document second = document("same");
        Document other = document("other");
        dao.storeDocument(first);
        dao.storeDocument(second);
        dao.storeDocument(other);

        List<Document> found = dao.findDocumentsByHash(first.getFileHash());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Document::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    public void findDocumentsByHash_whenUnknown_shouldReturnAnEmptyList() throws Exception {
        dao.storeDocument(document("a"));

        assertThat(dao.findDocumentsByHash("unknown")).isNotNull().isEmpty();
    }

    @Test
    public void clearDocuments_whenDocumentsStored_shouldRemoveEveryOne() throws Exception {
        Document document = document("a");
        String id = dao.storeDocument(document);
        dao.storeDocument(document("b"));

        dao.clearDocuments();

        assertThat(dao.findDocumentById(id)).isNull();
        assertThat(dao.findDocumentByToken(document.getToken())).isNull();
        assertThat(dao.findDocumentsByHash(document.getFileHash())).isEmpty();
    }

    @Test
    public void storeDocument_whenManyThreadsStoreAtOnce_shouldKeepEveryDocumentUnderAnIdOfItsOwn() throws Exception {
        int threads = 8;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Document>>> results = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                int thread = t;
                results.add(pool.submit(() -> {
                    start.await();
                    List<Document> stored = new ArrayList<>();
                    for (int i = 0; i < perThread; i++) {
                        Document document = document("thread-" + thread + "-document-" + i);
                        dao.storeDocument(document);
                        stored.add(document);
                    }
                    return stored;
                }));
            }
            start.countDown();

            Set<String> ids = new HashSet<>();
            for (Future<List<Document>> result : results) {
                for (Document document : result.get(30, TimeUnit.SECONDS)) {
                    assertThat(dao.findDocumentById(document.getId())).isSameAs(document);
                    ids.add(document.getId());
                }
            }
            assertThat(ids).hasSize(threads * perThread);
        } finally {
            pool.shutdownNow();
        }
    }

    private static Document document(String contents) throws Exception {
        return new Document(contents.getBytes(StandardCharsets.UTF_8), "title of " + contents);
    }
}
```

The concurrency test cannot be shown red by a compile failure alone. After green, swap
`ConcurrentHashMap` for `HashMap` locally, run the test 5 times, record how many runs fail, and
restore. This is an observation for the record, not a gate: a `HashMap` race is not guaranteed to
show.

### T3 — Select the store, and remove MongoDB from the code (AC004, AC005)

**Files:** create `PersistenceSelectionIT.java` (§3.4). Modify `InMemoryDocumentDao.java`,
`DshDataApplication.java`, the four POMs, both `application.properties` (§3.5). Delete the
`mongo` package, its test, `dshApplicationContext.xml` and `mongo.properties`.

**Interfaces:** consumes `InMemoryDocumentDao` from T2.

- [ ] Write `PersistenceSelectionIT` as §3.4.
- [ ] Red. `mvn -B -pl dsh-data -DskipTests install`, then
  `mvn -B -pl dsh-rest-api verify -DintegrationTests -Dit.test=PersistenceSelectionIT`, both
  logged. Docker running, because the profile starts its containers. Expect the two "uses the
  in-memory store" cases to fail on `MongoDocumentDao`, and the failure case to fail because the
  startup succeeds. Record what actually happens.
- [ ] Add `@Component` and `@ConditionalOnProperty(name = "dsh.persistence", havingValue =
  "memory", matchIfMissing = true)` to `InMemoryDocumentDao`.
- [ ] Delete `MongoDocumentDao.java`, `DocumentRepository.java`, `MongoDocumentDaoTest.java`,
  `dshApplicationContext.xml`, `mongo.properties`. Remove `@ImportResource` and its import from
  `DshDataApplication`.
- [ ] POMs: in `dsh-data`, `spring-boot-starter-data-mongodb` → `spring-boot-starter`, and remove
  the `dshApplicationContext.xml` exclude and the second `<resource>` block with its `#46` comment.
  In `dsh-rest-api` and the two workers, remove the MongoDB starter.
- [ ] `dsh-rest-api/src/main/resources/application.properties`: replace the `# MongoDB` block
  with:

  ```properties
  # Document store (ADR-003, section 3). dsh.persistence selects it; absent means memory, its only
  # value until Wave 5. Any other value stops the startup.
  ```

- [ ] `dsh-rest-api/src/test/resources/application.properties`: remove the `# MongoDB` block, and
  the `spring.autoconfigure.exclude` line with the comment above it.
- [ ] Green. The same two commands. All three cases pass.
- [ ] `mvn -B dependency:tree -Dincludes=org.mongodb`, logged as
  `.logs/mvn-dependency-tree-mongodb.log`. `grep -c 'org.mongodb' .logs/mvn-dependency-tree-mongodb.log`
  gives 0. Record it (AC005).
- [ ] Commit: `feat: select the in-memory store by dsh.persistence and remove MongoDB`.

### T4 — Integration tests run without MongoDB (AC006)

**Files:** `dsh-rest-api/pom.xml` (the profile), `DocumentResourceHttpIT.java`, the comments of
`DshRestApplicationIT.java`, `DocumentResourceIT.java`, `DocumentHandlingServiceImplTest.java`,
`DocumentSubmissionServiceImplTest.java` and `DshDataApplicationTest.java`. Delete
`dsh-rest-api/src/test/docker/mongo-init.js`.

- [ ] Remove the `mongo` image from the profile, and the four `--mongo.*` arguments with their
  comment (§3.4 table).
- [ ] Delete `mongo-init.js`.
- [ ] `DocumentResourceHttpIT`: the class Javadoc becomes "Calls the REST API over HTTP against the
  server that the `http-integration-tests` profile starts, which keeps documents in its own memory,
  with RabbitMQ in the container the profile starts. No Spring context runs in this JVM: the
  application under test is a separate process." The `READ_TIMEOUT_MILLIS` comment becomes "So
  that a server that hangs fails the test rather than the build."
- [ ] The other five files: each sentence that says MongoDB is mocked, deprecated or required
  now names RabbitMQ only, or the document store, whichever the sentence is about. No code
  changes in them.
- [ ] `mvn -B -pl dsh-rest-api verify -DintegrationTests`, logged as
  `.logs/mvn-verify-it-dsh-rest-api.log`. Every IT passes. The log shows one container started,
  `dsh-it-rabbitmq-…`, and no `mongo` pull.
- [ ] Commit: `test(dsh-rest-api): run the integration tests without MongoDB`.

### T5 — Workflows (AC007)

**Files:** the seven workflows (§3.6).

- [ ] Make the §3.6 changes.
- [ ] `git grep -n -i mongo -- .github/workflows` finds nothing.
- [ ] Each file still parses. No Python is installed on this machine, so through Node:
  `for f in .github/workflows/*.yml; do npx --yes js-yaml@4.1.0 "$f" > /dev/null || echo "BAD $f"; done`.
  CI then parses `ci.yml` and `api-testing.yml` for real, and T8 parses `deploy.yml`.
- [ ] Commit: `ci: stop supplying and running MongoDB in every workflow`.

### T6 — Documentation, guidance and scripts (AC008)

**Files:** §3.7's table, and the four `connect-mongo*` scripts (deleted).

- [ ] `git grep -n 'how-build-properties-reach'` still finds nothing (§3.7.2). If it finds a link,
  update it with the heading.
- [ ] Make every §3.7 change, and delete the four scripts.
- [ ] Lint (§6.1). Fix until clean.
- [ ] Commit: `docs: describe DSH without MongoDB, and point the guidance at ADR-003`.

### T7 — Gates and checks (AC005, AC007, AC008, AC009)

- [ ] Build `<scratchpad>/settings-no-mongo.xml` from `~/.m2/settings.xml` with every line that
  contains `mongo.` removed. `grep -c mongo` on it gives 0.
- [ ] Gate 1 and 2 (§6.1). Record the exit code, and `dsh-data`'s and `dsh-rest-api`'s coverage
  beside T1's.
- [ ] Gate 3 (§6.1). Record the exit code and the failsafe totals per module.
- [ ] AC008: `git grep -il mongo | sort`, and check every path against §3.8. Record the list, and
  the count beside T1's.
- [ ] Fill §9 with the results. Commit: `docs(spec): record the verification of #167`.

### T8 — The deploy (AC010), during step 6 or 7

- [ ] With the pull request open and CI green on its head, dispatch `deploy.yml` on the task
  branch (§3.9), and watch it with `gh run watch`.
- [ ] Check §3.9 items 1 to 5. Record the run URL and each result in §9.
- [ ] `git pull` the regenerated `README.md`, then rerun the §3.8 check.
- [ ] Commit the §9 update: `docs(spec): record the deploy of #167`.

## 8. Acceptance criteria

| AC | Where | Evidence |
|---|---|---|
| AC001: the in-memory `DocumentDao`, interface unchanged, unit tests of every operation and miss. The owner changed `findDocumentsByHash` to `findDocumentByHash` in review round 1 | §3.2, §3.3, §3.10; T2 | `InMemoryDocumentDaoTest` |
| AC002: an id-less document gets its id on the same instance, and a repeat store replaces | §3.2; T2 | `storeDocument_whenNoId_…`, `storeDocument_whenStoredAgain_…` |
| AC003: safe for concurrent use | §3.3; T2 | `storeDocument_whenManyThreadsStoreAtOnce_…` |
| AC004: selection by `dsh.persistence`, absent / `memory` / other, in an `*IT` | §3.4; T3 | `PersistenceSelectionIT` |
| AC005: the MongoDB code is gone, and no `org.mongodb` artifact on any classpath | §3.5; T3 | the deleted files; `.logs/mvn-dependency-tree-mongodb.log`, 0 hits |
| AC006: `-DintegrationTests` without MongoDB; `DocumentResourceHttpIT` finds the document by token | §3.4; T4 | `.logs/mvn-verify-it-dsh-rest-api.log` |
| AC007: no workflow sets `mongo.*`, runs MongoDB or creates its user; the build passes with no `mongo.*` | §3.6; T5, T7, T8 | the grep; gate 1 with `settings-no-mongo.xml`; CI; the deploy |
| AC008: nothing describes MongoDB as part of DSH; the scripts deleted; every remaining hit listed | §3.7, §3.8; T6, T7 | the §3.8 check |
| AC009: gates 1, 2 and 3, and lint | §6.1; T7 | §9 |
| AC010: `deploy.yml` on the task branch: all tests, artifacts, site | §3.9; T8 | the run, §9 |

## 9. Verification results

Recorded on 2026-10-14, T8's rows on 2026-10-15, and review round 1's on 2026-10-19.

| Task | Check | Result |
|---|---|---|
| T0 | AC010 in `#167`'s body | added below AC009, as approved |
| T1 | baseline | `d3a4080e7`; `mvn -B clean install` exit 0; 74 files hit `git grep -il mongo` |
| T1 | baseline coverage | `jacoco:check` prints only "met", so read from `jacoco:report` over the same exec data: `dsh-data` LINE 0.9711 (235/242), BRANCH 0.9878 (81/82); `dsh-rest-api` LINE 0.9793 (142/145), BRANCH 0.9722 (35/36) |
| T2 | red | `test-compile` fails: `cannot find symbol: class InMemoryDocumentDao` |
| T2 | green | `InMemoryDocumentDaoTest`: 12 run, 0 failures |
| T2 | `HashMap` observation | 0 of 5 runs failed with `HashMap` in place of `ConcurrentHashMap`. The race did not show, as §T2 allowed; the test still guards the property |
| T3 | red | `PersistenceSelectionIT`: 3 run, 3 failures. Both "in-memory" cases found `MongoDocumentDao`; the failure case started up |
| T3 | green | 3 run, 0 failures. The failure case's cause: `DocumentHandlingServiceImpl.dao` required a `DocumentDao` that could not be found |
| T3 | AC005 | `mvn -B dependency:tree -Dincludes=org.mongodb`: 13 module trees, 0 `org.mongodb` lines (`.logs/mvn-dependency-tree-mongodb.log`) |
| T4 | AC006 | `mvn -B -pl dsh-rest-api clean verify -DintegrationTests`: exit 0; failsafe 15 run, 0 failures (`DocumentResourceHttpIT` 4, `DocumentResourceIT` 6, `DshRestApplicationIT` 2, `PersistenceSelectionIT` 3). One container started, `rabbitmq:3`; no line of the log mentions mongo |
| T5 | workflows | `git grep -i mongo -- .github/workflows` finds nothing. All 11 parse under `js-yaml@4.1.0`, run in a `node:20-alpine` container because no Node is installed on this machine |
| T6 | lint | `markdownlint-cli@0.45.0`, in the same container: the `CLAUDE.md` command clean; the site README and module pages clean after removing a trailing blank line, already there, from four module pages |
| T7 | AC007 settings | `settings-no-mongo.xml`: `grep -c mongo` gives 0 |
| T7 | gates 1 and 2 | `mvn -B -s settings-no-mongo.xml clean install`: exit 0, 8 modules met `jacoco:check`. `dsh-data` LINE 0.9757 (241/247), BRANCH 1.0000 (82/82); `dsh-rest-api` LINE 0.9793 (142/145), BRANCH 0.9722 (35/36) |
| T7 | gate 3 | `mvn -B -s settings-no-mongo.xml clean verify -DintegrationTests -pl dsh-data -amd`: exit 0. Failsafe: `dsh-rest-api` 15 run, `dsh-doc-indexer-worker` 1 run, 0 failures; `dsh-data` has no IT. One container, `rabbitmq:3` |
| T7 | AC008 | 33 files hit, against 74 before. Every one is in §3.8, after adding three rows that §3.7 and §3.4 made necessary: `docs/devops/README.md`, `system-design.md`, `PersistenceSelectionIT.java` |
| Step 5 | local review | four findings fixed at the owner's choice. `PersistenceSelectionIT`'s absent case asserts the key is absent: red with `-Ddsh.persistence=memory`, green without. `docs/copilot/prompt-examples.md` injects `DocumentDao, HighlightDao`. `system-design.md` drops "Manages database schema migrations". The "New Store" prompt names `@Component` |
| T8 | AC010 | `deploy.yml` on the task branch at `42259940d`, 2026-10-15: green, <https://github.com/MRISS-Projects/dsh/actions/runs/38766449969>, with no `maven_properties` |
| T8 | §3.9 item 1 | failsafe: `PersistenceSelectionIT` 3 run, `DocumentResourceHttpIT` 4 run, 0 failures and 0 errors |
| T8 | §3.9 item 2 | one container, `rabbitmq:3`; no `mongo` image pulled. The log's other mongo lines are the branch name and POMs (no jars) that the site's "Dependency Management" report resolves from the Spring Boot BOM |
| T8 | §3.9 item 3 | all 13 modules uploaded at `0.4.0-SNAPSHOT` |
| T8 | §3.9 item 4 | the site's `index.html` and each module's answer 200 |
| T8 | §3.9 item 5 | `41ff0f14d Auto-generated README.md [skip jenkins]` pulled; its mongo hits are the seven release-notes rows §3.8 allows (`#139`, `#123`, `#17`, `#21`, `#18`, `#19`, `#15`) |
| Review 1 | store red | `InMemoryDocumentDaoTest` against the old store, with `findDocumentByHash` added: 21 run, 6 failures and 1 error. The rejections of a duplicate hash, a duplicate token, another document under a stored id, a missing hash, a missing token and the concurrent same-file case fail; `findDocumentById(null)` throws `NullPointerException` |
| Review 1 | store green | 21 run, 0 failures. `f0cdc192c` |
| Review 1 | `Document` red | `test-compile` fails: no constructor `Document(String, String, String, SortedSet, SortedSet, byte[], DocumentStatus, String)` |
| Review 1 | `Document` green | `dsh-data` 69 run, 0 failures; the full-args constructor gives `039058c6…cfb81`, the SHA-256 of `{1, 2, 3}` from `sha256sum`. `b4661ea9d` |
| Review 1 | resubmission red | `resetStatus` cannot be found; `getDocumentByHash` returns null for an unprocessed document; both resubmission tests get a new token instead of the stored one |
| Review 1 | resubmission green | `dsh-rest-api` 39 run, 0 failures. Branch coverage was first 0.93: the half-processed case had lost its test when the check moved, and got one. `d0d5c9537` |
| Review 1 | HTTP red | `DocumentResourceHttpIT` with the submission service of `b4661ea9d` checked out: 5 run, 1 failure. The second `/submit` answered a new token, and the rejected store logged `SimpleAsyncUncaughtExceptionHandler` twice |
| Review 1 | gates 1 and 2 | `mvn -B clean install`: exit 0, 13 modules. `dsh-data` LINE 0.9842 (249/253), BRANCH 1.0000 (100/100); `dsh-rest-api` LINE 0.9786 (137/140), BRANCH 0.9688 (31/32) |
| Review 1 | gate 3 | `mvn -B clean verify -DintegrationTests -pl dsh-data -amd`: exit 0. Failsafe: `dsh-rest-api` 16 run (`DocumentResourceHttpIT` 5), `dsh-doc-indexer-worker` 1 run, 0 failures. No `AsyncUncaughtExceptionHandler` line in the log. `d9c57cbd7` |

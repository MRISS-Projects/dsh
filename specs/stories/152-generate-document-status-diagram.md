---
issue: 152
slug: generate-document-status-diagram
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 152 — Generate the document-status diagram from `DocumentStatus` and fail the build when it drifts

## 1. Story

**As a** DSH developer changing how a document moves through the pipeline
**I want** the document-status diagram to be generated from `DocumentStatus` and checked by the build
**So that** the diagram always shows the state machine the code actually runs, and a change to the
statuses cannot ship with a stale picture

## 2. Context

- Wave: 1, task 1 (`specs/product/PRD.md`, "Wave 1"), DSH milestone `0.4.0-SNAPSHOT`
- Issue: [#152](https://github.com/MRISS-Projects/dsh/issues/152)
- Parent branch: `DEVELOP`, at `322b32a35` when this spec was written. The task branch is
  `issue-152-generate-document-status-diagram`.

### 2.1 What exists today

- `src/site/resources/images/workflow.jpg` is the only picture of the lifecycle. It is drawn by
  hand. The wiki's [Workflow](https://github.com/MRISS-Projects/dsh/wiki/Workflow) page embeds it,
  and the daily `wiki-sync.yml` mirrors that page into `docs/wiki/Workflow.md`.
- `specs/architecture/system-design.md` line 79 names `docs/wiki/Workflow.md` as the reference for
  status transitions.
- `DocumentStatus` (`dsh-data`, `com.mriss.dsh.data.models`) has 18 statuses. Each implements
  `transition(TransitionType)`, a pure function over `SUCCESS`, `ERROR` and `NEUTRAL`. Evaluated for
  every status and type, it gives:
  - 17 edges between different statuses;
  - 6 terminal statuses, which return themselves for all three types: `QUEUED_FOR_INDEXING_ERROR`,
    `NOT_INDEXED_ERROR`, `QUEUED_FOR_PROCESSING_ERROR`, `KEYWORDS_NOT_PROCESSED_ERROR`,
    `SENTENCES_PROCESSED_SUCCESS`, `SENTENCES_NOT_PROCESSED_ERROR`;
  - one status no other status leads to: `CREATED`.
- Tests in `dsh-data` are JUnit 4 with `org.junit.Assert`, as `DocumentStatusTest` shows.
- `core.autocrlf` is `input` and there is no `.gitattributes`.

### 2.2 Constraints found while designing

- **`documentation-sync.yml` rewrites files in `specs/architecture/`.** On every push to `DEVELOP`
  that touches `specs/architecture/**`, it runs `markdown-toc -i` over `specs/architecture/*.md` and
  auto-commits the result with `[skip ci]`. A check that compared the whole file could be broken by
  that bot. The check therefore compares the Mermaid block only, and the prose around it is free.
- **The wiki is a separate repository** (`dsh.wiki.git`). Its new link points at a file on `DEVELOP`,
  which does not exist until this story's PR merges. The wiki edit therefore happens after the
  merge (§3.1, decision 7).

## 3. Design

### 3.1 Decisions

1. **The generator is a test.** A new `DocumentStatusDiagramTest` in `dsh-data`'s test sources holds
   the generator as a static method. Nothing ships in the jar, and test code is outside the
   coverage gate. There is no second script, plugin execution or tool (AC004). Rejected: a
   production `DocumentStatusDiagram` class, which would ship code nobody runs and need its own
   coverage; a Maven `exec` step that writes the file, which would be a second generator.
2. **The generation rules** (AC002):
   - The output starts with the line `stateDiagram-v2`. Every following line is indented four
     spaces. Every line ends with `\n`.
   - **Entry edges first.** `[*] --> S` for every status `S`, in enum declaration order, that no
     other status transitions into. It is derived, not hardcoded. Today it yields exactly
     `[*] --> CREATED`. If Wave 2 adds a second root, the diagram shows it, and that is correct.
   - **Then each status in enum declaration order.** Its transition types are taken in
     `TransitionType` declaration order and grouped by the status they return, in order of first
     appearance. A group whose target is the status itself is dropped, so there are no
     self-loops. Each remaining group emits `S --> T : <types>`, with the type names joined by
     `", "`. There is no "any" shorthand: all three are written out.
   - **A status with no remaining group** (it returns itself for all three types) emits
     `S --> [*]`, at its place in the walk.
3. **The comparison.**
   - The committed file is found at `<basedir>/../specs/architecture/document-status-workflow.md`,
     where `<basedir>` is the `basedir` system property, which Surefire sets to the module
     directory. If the property is absent, the working directory is used.
   - The file is read as UTF-8, and `\r\n` is normalised to `\n`.
   - A Mermaid block is the lines strictly between a line whose trimmed text is ` ```mermaid ` and
     the next line whose trimmed text is ` ``` `, each followed by `\n`. An unclosed block is not
     counted.
   - The file must hold exactly one Mermaid block, and that block must equal the generated text.
4. **The failure message** (AC003). It is a JUnit `fail(...)`, not `assertEquals`, because
   `ComparisonFailure` abbreviates long strings with `...`. One message format covers all three
   failures: the file is missing, the block count is not one, or the block is out of date. The
   message is the file's path, the problem, "It is generated from DocumentStatus; replace its
   mermaid block with:", a blank line, and then the complete fenced block, ready to paste.
5. **A second test checks the rules, not the text.** It parses the generated edges and checks them
   against `DocumentStatus` directly:
   - no edge from a status back to itself, and no duplicate edges;
   - `[*] --> CREATED` is present;
   - for every `(status, type)` whose `transition` returns a different status `T`, the edge
     `status --> T` exists and its label includes `type`;
   - the labels hold no more types than those pairs (nothing extra);
   - `status --> [*]` exists exactly when `status` returns itself for all three types.

   It names no particular status beyond `CREATED`, so Wave 2 can change the statuses without
   editing it. It catches a generator bug that would otherwise produce a consistently wrong file.
6. **The documents.**
   - `specs/architecture/document-status-workflow.md` is new. It holds a title, a paragraph saying
     the diagram is generated by `DocumentStatusDiagramTest` and how to update it, and the block.
   - `src/site/resources/images/workflow.jpg` is deleted.
   - `system-design.md` line 79 names `specs/architecture/document-status-workflow.md` directly,
     because after this story the wiki page is only a link to it (AC008).
   - `specs/product/PRD.md` line 903 keeps its mention of `workflow.jpg`. It describes this task,
     so it is history rather than a reference to the image.
7. **The wiki, after the merge** (AC007). This is outward-facing, so Claude asks for approval at
   that point, then:
   1. Clones `https://github.com/MRISS-Projects/dsh.wiki.git` into the scratchpad.
   2. Replaces `Workflow.md` with the text in §3.3, and pushes.
   3. Dispatches `wiki-sync.yml` on `DEVELOP`.

   You merge the sync PR. That turns `docs/wiki/Workflow.md` into the link, which removes the last
   reference to `workflow.jpg` (AC006).

### 3.2 The committed diagram, as generated today

This is the block the generator produces for the enum as it stands. Hand-derived from §2.1, it is
also what Task 2 commits. If the test's regenerated text differs, the test's text wins, and the
difference is investigated before committing.

```text
stateDiagram-v2
    [*] --> CREATED
    CREATED --> QUEUED_FOR_INDEXING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_INDEXING --> QUEUED_FOR_INDEXING_SUCCESS : SUCCESS
    QUEUED_FOR_INDEXING --> QUEUED_FOR_INDEXING_ERROR : ERROR
    QUEUED_FOR_INDEXING_SUCCESS --> DEQUEUED_FOR_INDEXING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_INDEXING_ERROR --> [*]
    DEQUEUED_FOR_INDEXING --> INDEXING : SUCCESS, ERROR, NEUTRAL
    INDEXING --> INDEXED_SUCCESS : SUCCESS
    INDEXING --> NOT_INDEXED_ERROR : ERROR
    INDEXED_SUCCESS --> QUEUED_FOR_PROCESSING : SUCCESS, ERROR, NEUTRAL
    NOT_INDEXED_ERROR --> [*]
    QUEUED_FOR_PROCESSING --> QUEUED_FOR_PROCESSING_SUCCESS : SUCCESS
    QUEUED_FOR_PROCESSING --> QUEUED_FOR_PROCESSING_ERROR : ERROR
    QUEUED_FOR_PROCESSING_SUCCESS --> DEQUEUED_FOR_PROCESSING : SUCCESS, ERROR, NEUTRAL
    QUEUED_FOR_PROCESSING_ERROR --> [*]
    DEQUEUED_FOR_PROCESSING --> PROCESSING_KEYWORDS : SUCCESS, ERROR, NEUTRAL
    PROCESSING_KEYWORDS --> KEYWORDS_PROCESSED_SUCCESS : SUCCESS
    PROCESSING_KEYWORDS --> KEYWORDS_NOT_PROCESSED_ERROR : ERROR
    KEYWORDS_PROCESSED_SUCCESS --> PROCESSING_SENTENCES : SUCCESS, ERROR, NEUTRAL
    KEYWORDS_NOT_PROCESSED_ERROR --> [*]
    PROCESSING_SENTENCES --> SENTENCES_PROCESSED_SUCCESS : SUCCESS
    PROCESSING_SENTENCES --> SENTENCES_NOT_PROCESSED_ERROR : ERROR
    SENTENCES_PROCESSED_SUCCESS --> [*]
    SENTENCES_NOT_PROCESSED_ERROR --> [*]
```

It is shown here as `text` so that this spec is not a second Mermaid copy someone might take as
the source.

### 3.3 The wiki's new Workflow page

```markdown
# Document Processing Workflow

The statuses a document moves through, and the transitions between them, are drawn in
[`specs/architecture/document-status-workflow.md`](https://github.com/MRISS-Projects/dsh/blob/DEVELOP/specs/architecture/document-status-workflow.md).
That diagram is generated from `DocumentStatus` in `dsh-data`, and the build fails when it no
longer matches the code.
```

### 3.4 Out of scope

Out of scope, as in the issue:

- changing any status or transition;
- `NOT_INDEXED_ERROR`'s copied description and message (`#53`);
- the pipeline-stages diagram (Wave 1 tasks 5 and 6, Wave 2 task 1);
- moving the wiki's source into `docs/wiki/` (Wave 1 task 5);
- publishing the diagram in the Maven site.

## 4. Verification design

- **Gate 1 and gate 2.** `mvn -B clean install`, logged to `.logs/mvn-clean-install.log`. Production
  sources in `dsh-data` are unchanged, so its coverage cannot drop.
- **Gate 3 does not apply.** Nothing touches an external system or a REST entry point.
- **AC003, the red build.**
  1. Change `CREATED`'s `transition` to return `INDEXING` locally.
  2. Run `mvn -B clean install -pl dsh-data`, logged to `.logs/mvn-ac003-red.log`.
  3. Check that the build fails in `DocumentStatusDiagramTest.committedDiagramMatchesTheCode`, and
     that the message holds a block containing `CREATED --> INDEXING : SUCCESS, ERROR, NEUTRAL`.
  4. `generatedDiagramFollowsTheRules` still passes, because the generator is faithful to the
     changed code.
  5. Revert, then confirm `git diff --quiet -- dsh-data/src/main` succeeds.

  `DocumentStatusTest.testTransition` also fails in step 3. That is expected and unrelated.
- **AC005.** Running `.github/scripts/check-unit-tests-context-free.sh` locally passes. The
  coverage gate is checked by gate 2.
- **AC001, rendering.** After the push, open the file on GitHub on the task branch and confirm the
  diagram renders.
- **AC006.** `git grep -n workflow.jpg` on the branch lists only `specs/product/PRD.md` (history)
  and `docs/wiki/Workflow.md` (cleared by the post-merge sync), plus this spec.
- **AC007.** After the merge and the wiki sync, `docs/wiki/Workflow.md` on `DEVELOP` holds the link.
- **Markdown lint** over the new and changed files, with the command in `CLAUDE.md`.

## 5. Files to change

| File | Change |
|---|---|
| `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusDiagramTest.java` | new: generator and two tests |
| `specs/architecture/document-status-workflow.md` | new: the committed diagram |
| `src/site/resources/images/workflow.jpg` | deleted |
| `specs/architecture/system-design.md` | line 79 points at the new file |
| wiki `Workflow.md` (separate repo, after the merge) | replaced with §3.3 |

## 6. Implementation plan

> **For agentic workers:** this plan is built with `dsh-build-story` (step 4 of the DSH process),
> TDD, red first. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** a unit test in `dsh-data` that generates the `DocumentStatus` Mermaid diagram and fails
when the committed file disagrees, plus the documentation changes that make that file the single
picture of the lifecycle.

**Architecture:** one JUnit 4 test class holds a static generator over `DocumentStatus.values()` ×
`TransitionType.values()`, a block extractor, and two tests. One compares the committed block. The
other checks the generator's output against the enum's semantics.

**Tech stack:** Java 17, JUnit 4 (`org.junit.Assert`), Maven, Mermaid `stateDiagram-v2`.

### Global constraints

- JUnit 4 only, no JUnit 5. No Spring context (`check-unit-tests-context-free.sh`). No PowerMock.
- No production source changes in `dsh-data`.
- Every local `mvn` run is logged to `.logs/` and its exit code reported (`CLAUDE.md`, "Commands").
- Gates always run with `clean`.

### Task 1: The generator and the rules test

**Files:**

- Create: `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusDiagramTest.java`

**Interfaces:**

- Produces: `static String generate()`, which returns the block text without fences, ending in
  `\n`. Also `static List<String> mermaidBlocks(String content)` and `static Path diagramFile()`,
  both used in Task 2.

- [ ] **Step 1: Write the rules test, with a `generate()` that returns only the header**

```java
package com.mriss.dsh.data.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Generates the Mermaid diagram of the {@link DocumentStatus} state machine and checks it against
 * {@code specs/architecture/document-status-workflow.md}. This class is the only generator of that
 * diagram.
 */
public class DocumentStatusDiagramTest {

    private static final String INDENT = "    ";
    private static final String ARROW = " --> ";
    private static final String LABEL_SEPARATOR = " : ";
    private static final String END = "[*]";

    @Test
    public void generatedDiagramFollowsTheRules() {
        Map<String, Set<String>> edges = new HashMap<>();
        for (String line : generate().split("\n")) {
            if (!line.contains(ARROW)) {
                continue;
            }
            String[] edgeAndLabel = line.trim().split(LABEL_SEPARATOR, 2);
            String[] ends = edgeAndLabel[0].split(ARROW);
            assertNotEquals("self-loop: " + line, ends[0], ends[1]);
            Set<String> types = edgeAndLabel.length == 2
                    ? new HashSet<>(Arrays.asList(edgeAndLabel[1].split(", ")))
                    : new HashSet<>();
            assertNull("duplicate edge: " + line, edges.put(edgeAndLabel[0], types));
        }

        assertTrue("missing entry edge", edges.containsKey(END + ARROW + DocumentStatus.CREATED.name()));

        int changingPairs = 0;
        for (DocumentStatus status : DocumentStatus.values()) {
            boolean terminal = true;
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    terminal = false;
                    changingPairs++;
                    Set<String> label = edges.get(status.name() + ARROW + target.name());
                    assertNotNull("missing edge " + status + ARROW + target, label);
                    assertTrue(status + ARROW + target + " lacks " + type, label.contains(type.name()));
                }
            }
            assertEquals("terminal edge of " + status, terminal, edges.containsKey(status.name() + ARROW + END));
        }

        int labelledTypes = edges.values().stream().mapToInt(Set::size).sum();
        assertEquals("labelled types", changingPairs, labelledTypes);
    }

    static String generate() {
        return "stateDiagram-v2\n";
    }
}
```

- [ ] **Step 2: Run it and see it fail**

Run, logged:

```bash
mkdir -p .logs
mvn -B -pl dsh-data test -Dtest=DocumentStatusDiagramTest > .logs/mvn-test-diagram.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-test-diagram.log"
wait $MVN_PID; echo "maven exit=$?"
```

Expected: a non-zero exit, with `generatedDiagramFollowsTheRules` failing on `missing entry edge`.

- [ ] **Step 3: Implement `generate()`**

Replace the stub with the following, and add `java.util.ArrayList`, `java.util.EnumSet`,
`java.util.LinkedHashMap` and `java.util.List` to the imports:

```java
    static String generate() {
        Set<DocumentStatus> targets = EnumSet.noneOf(DocumentStatus.class);
        for (DocumentStatus status : DocumentStatus.values()) {
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    targets.add(target);
                }
            }
        }

        StringBuilder out = new StringBuilder("stateDiagram-v2\n");
        for (DocumentStatus status : DocumentStatus.values()) {
            if (!targets.contains(status)) {
                out.append(INDENT).append(END).append(ARROW).append(status.name()).append('\n');
            }
        }
        for (DocumentStatus status : DocumentStatus.values()) {
            Map<DocumentStatus, List<String>> typesByTarget = new LinkedHashMap<>();
            for (TransitionType type : TransitionType.values()) {
                DocumentStatus target = status.transition(type);
                if (target != status) {
                    typesByTarget.computeIfAbsent(target, t -> new ArrayList<>()).add(type.name());
                }
            }
            typesByTarget.forEach((target, types) -> out.append(INDENT).append(status.name()).append(ARROW)
                    .append(target.name()).append(LABEL_SEPARATOR).append(String.join(", ", types)).append('\n'));
            if (typesByTarget.isEmpty()) {
                out.append(INDENT).append(status.name()).append(ARROW).append(END).append('\n');
            }
        }
        return out.toString();
    }
```

- [ ] **Step 4: Run it and see it pass**

Same command as Step 2. Expected: `maven exit=0`, with `Tests run: 1, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusDiagramTest.java
git commit -m "test(dsh-data): generate the DocumentStatus Mermaid diagram (#152)"
```

### Task 2: Compare with the committed file

**Files:**

- Modify: `dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusDiagramTest.java`
- Create: `specs/architecture/document-status-workflow.md`

**Interfaces:**

- Consumes: `generate()` from Task 1.

- [ ] **Step 1: Write the comparison test**

Add these constants and members, and add `static org.junit.Assert.fail`, `java.io.IOException`,
`java.nio.charset.StandardCharsets`, `java.nio.file.Files`, `java.nio.file.Path` and
`java.nio.file.Paths` to the imports:

```java
    static final String DIAGRAM_FILE = "specs/architecture/document-status-workflow.md";
    private static final String OPENING_FENCE = "```mermaid";
    private static final String CLOSING_FENCE = "```";

    @Test
    public void committedDiagramMatchesTheCode() throws IOException {
        String generated = generate();
        Path file = diagramFile();
        if (!Files.exists(file)) {
            fail(mismatch(file, "does not exist", generated));
        }
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).replace("\r\n", "\n");
        List<String> blocks = mermaidBlocks(content);
        if (blocks.size() != 1) {
            fail(mismatch(file, "has " + blocks.size() + " mermaid blocks, not exactly one", generated));
        }
        if (!blocks.get(0).equals(generated)) {
            fail(mismatch(file, "is out of date", generated));
        }
    }

    static Path diagramFile() {
        Path moduleDir = Paths.get(System.getProperty("basedir", "")).toAbsolutePath();
        return moduleDir.getParent().resolve(DIAGRAM_FILE);
    }

    static List<String> mermaidBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        StringBuilder block = null;
        for (String line : content.split("\n", -1)) {
            if (block == null) {
                if (line.trim().equals(OPENING_FENCE)) {
                    block = new StringBuilder();
                }
            } else if (line.trim().equals(CLOSING_FENCE)) {
                blocks.add(block.toString());
                block = null;
            } else {
                block.append(line).append('\n');
            }
        }
        return blocks;
    }

    private static String mismatch(Path file, String problem, String generated) {
        return file + " " + problem + ". It is generated from DocumentStatus; replace its mermaid block with:\n\n"
                + OPENING_FENCE + "\n" + generated + CLOSING_FENCE + "\n";
    }
```

- [ ] **Step 2: Run it and see it fail, because the file does not exist**

Same command as Task 1 Step 2. Expected: a non-zero exit. `committedDiagramMatchesTheCode` fails
with `... document-status-workflow.md does not exist. It is generated from DocumentStatus; replace
its mermaid block with:`, followed by the fenced block. Check the surefire report: the block in the
message must equal §3.2. If it differs, stop and work out why before going on.

- [ ] **Step 3: Create `specs/architecture/document-status-workflow.md`**

The block is pasted from the failure message:

````markdown
# Document Status Workflow

The statuses a document moves through in DSH, and the transitions between them. Each edge is
labelled with the `TransitionType` values that cause it. `[*]` marks where a document enters and
the statuses it never leaves.

This diagram is generated from `DocumentStatus` in `dsh-data`, not drawn.
`DocumentStatusDiagramTest` regenerates it on every build and fails when this file disagrees.
To update it, run `mvn -B -pl dsh-data test -Dtest=DocumentStatusDiagramTest`, and copy the block
from the failure message over the one below. Keep exactly one `mermaid` block in this file. The
test compares only that block, so the text around it can change freely.

```mermaid
<the block from §3.2, unchanged>
```
````

- [ ] **Step 4: Run it and see both tests pass**

Same command. Expected: `maven exit=0`, with `Tests run: 2, Failures: 0`.

- [ ] **Step 5: Commit**

```bash
git add dsh-data/src/test/java/com/mriss/dsh/data/models/DocumentStatusDiagramTest.java specs/architecture/document-status-workflow.md
git commit -m "test(dsh-data): fail the build when the committed status diagram drifts (#152)"
```

### Task 3: Retire the hand-drawn image and repoint the architecture doc

**Files:**

- Delete: `src/site/resources/images/workflow.jpg`
- Modify: `specs/architecture/system-design.md:78-79`

- [ ] **Step 1:** `git rm src/site/resources/images/workflow.jpg`.
- [ ] **Step 2:** In `system-design.md`, replace the two lines:

```markdown
- Manages document status transitions using as reference the flow
  defined in `docs/wiki/Workflow.md`.
```

with:

```markdown
- Manages document status transitions. The state machine is drawn in
  [`document-status-workflow.md`](document-status-workflow.md), which is generated from
  `DocumentStatus` and checked by the build.
```

- [ ] **Step 3:** Run `git grep -n workflow.jpg`. Expected: only `specs/product/PRD.md`,
  `docs/wiki/Workflow.md` and this spec.
- [ ] **Step 4:** Run markdownlint with the `CLAUDE.md` command. Expected: no findings.
- [ ] **Step 5: Commit**

```bash
git add -A src/site/resources/images/workflow.jpg specs/architecture/system-design.md
git commit -m "docs: replace the hand-drawn workflow image with the generated diagram (#152)"
```

### Task 4: The gates and the AC003 red build

- [ ] **Step 1: Gates 1 and 2.** Run `mvn -B clean install`, logged to `.logs/mvn-clean-install.log`.
  Expected: `maven exit=0`.
- [ ] **Step 2: AC005.** `bash .github/scripts/check-unit-tests-context-free.sh` passes.
- [ ] **Step 3: AC003.** Follow the red-build procedure in §4. Record the failure message excerpt in
  §8, then revert and confirm `git diff --quiet -- dsh-data/src/main`.
- [ ] **Step 4:** Record the results in §8 and hand over to `dsh-ship-story`.

### Task 5: After the merge — the wiki (decision 7)

- [ ] **Step 1:** Ask for approval to push to the wiki.
- [ ] **Step 2:** Clone `https://github.com/MRISS-Projects/dsh.wiki.git` into the scratchpad.
  Replace `Workflow.md` with §3.3, commit, and push.
- [ ] **Step 3:** `gh workflow run wiki-sync.yml --ref DEVELOP`, then link the sync PR for you to
  merge.
- [ ] **Step 4:** After that merge, confirm `docs/wiki/Workflow.md` on `DEVELOP` holds the link
  (AC007). `git grep workflow.jpg` then lists only history (AC006).

## 7. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: a committed Mermaid file, rendered by GitHub | Task 2, §4 rendering check |
| AC002: derived edges, labels, no self-loops, `[*]` entry and terminals | decision 2, Task 1 rules test |
| AC003: the build fails on a mismatch with a copy-paste message | decision 4, Task 2, Task 4 Step 3 |
| AC004: one generator | decision 1 |
| AC005: no Spring context; `dsh-data` keeps 95% | Task 4 Steps 1 and 2 |
| AC006: `workflow.jpg` deleted, unreferenced | Task 3, Task 5 Step 4 |
| AC007: the wiki links to the file | decision 7, Task 5 |
| AC008: `system-design.md` points at the file | Task 3 Step 2 |

## 8. Verification results

To be recorded during the build.

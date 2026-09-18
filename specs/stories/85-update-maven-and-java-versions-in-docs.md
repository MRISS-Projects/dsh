---
issue: 85
slug: update-maven-and-java-versions-in-docs
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 85 — Update documentation: replace Maven 3.3.9 with 3.9.9 and standardise Java version to 17

> **This story is one half of a pair.** `MRISS-Projects/parent-poms#57` carries the same title and
> fixes the same rot in that repository's own installation pages. The two are scheduled together in
> one cycle, reusing the pattern `#86`/`#58` established — see §7. The DSH half stands on its own
> and does **not** wait on a parent-poms release.

## 1. Story

**As a** developer setting up this project for the first time
**I want** the installation instructions to describe the toolchain the build actually requires
**So that** following them end to end produces a machine that can build DSH, instead of one that
cannot

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Twin issue upstream: `MRISS-Projects/parent-poms#57`, milestone `3.8.0-SNAPSHOT`
- Standard being applied: `parent-poms#55`, which established Maven 3.9.9 + Temurin Java 17 as the
  canonical toolchain for the estate
- Pinned in CI by `#86` (PR #108) and `parent-poms#58` (`7411cbec`) — this story makes the prose
  agree with those pins

`#86` pinned Maven 3.9.9 in both workflows that invoke Maven. It deliberately stopped there. Its
spec said so at line 127: `README.md` and `src/site/markdown/README.md` "hold the only remaining
`3.3.9` references in the tree — six lines each… That is `#85`'s scope, and splitting it across two
stories would leave both half-done."

That claim was re-checked at the head of this branch and holds. `git grep` over the tracked tree
finds stale Maven or Java versions in those two files and nowhere else.
`specs/architecture/system-design.md` already says Java 17; `#97` corrected it.

So the scope is two files — and, because one of them is generated, really one.

## 3. Three defects the issue's acceptance criteria do not catch

`#85` asks for `3.3.9` → `3.9.9` and "Java 8 or Java 11" → "Java 17 (Temurin)". Read literally,
its AC001 — "contains no reference to Maven `3.3.9` or Java versions older than 17" — can pass
while the file is still wrong in three ways. All three were found by reading the file rather than
grepping it.

### 3.1 A second stale Maven version the grep does not name

`src/site/markdown/README.md:185`, inside the Linux `mvn -version` sample block, prints:

```text
Apache Maven 3.3.1 (cab6659f9874fa96462afef40fcf6bc033d58c1c; 2015-03-13T17:10:27-03:00)
```

**3.3.1, not 3.3.9** — the sample output never matched the version the same page told the reader to
download. A sweep keyed on the string `3.3.9` leaves it in place and AC001 still passes.

### 3.2 The page prescribes a JVM flag that stops Maven starting on Java 17

`src/site/markdown/README.md:164`:

```bash
export MAVEN_OPTS='-Xmx1024m -XX:MaxPermSize=256m'
```

`MaxPermSize` was removed from HotSpot after Java 8. Verified against the Temurin 17 JDK on the
development machine:

```text
$ jdk-17.0.20.1+1/bin/java -XX:MaxPermSize=256m -version
Unrecognized VM option 'MaxPermSize=256m'
Error: Could not create the Java Virtual Machine.
Error: A fatal exception has occurred. Program will exit.
```

This is the whole point of the story rather than a detail of it. A reader who follows the
prerequisites section as written — Java 17 per this change, `MAVEN_OPTS` per the existing line —
ends up with a `mvn` that refuses to start. Line 184 even documents the old behaviour, carrying
Java 8's warning text (`ignoring option MaxPermSize=256m; support was removed in 8.0`) in the
sample output; on 17 it is fatal, not ignored.

### 3.3 "Java 17 (Temurin)" is a link and a layout change, not a number change

Three places where substituting a version number is not enough:

| Line | What is there | Why a number swap does not fix it |
|---|---|---|
| 93 | Oracle's `jdk8-downloads-2133151.html` | Wrong vendor for Temurin, and the page no longer serves that JDK |
| 108 | `ln -s jdk1.8.0_XX java` | The 17 archives are named `jdk-17.0.x+y`, not `jdk1.8.0_XX` |
| 188, 213 | `Java home: …/jdk1.8.0_45/jre` | Java 17 has no separate `jre` subdirectory; the sample cannot be right with only the digits changed |

**Found while building — the `Java home:` line does not survive at all.** Maven 3.9.x changed the
`mvn -version` format: there is no `Java home:` line, the JDK path moved onto the `Java version:`
line behind a `runtime:` label, and the version parenthesis carries a commit hash with no build
date. So those two lines are deleted rather than corrected, and both sample blocks lose a line.
The conclusion above stands; the mechanism is stronger than it was written.

All three are folded into this story. They are inside the two files the issue names, and inside
what "standardise Java version to 17" has to mean for the page to be followable.

## 4. Files to change in DSH

### 4.1 `src/site/markdown/README.md`

The only file this story edits by hand. The edits, by line:

| Line | Now | Becomes |
|---|---|---|
| 73 | `* Java 1.8` | `* Java 17 (Temurin)` |
| 75 | `* Maven 3.3.9` | `* Maven 3.9.9` |
| 93 | Oracle JDK 8 download link | The Adoptium Temurin 17 download page |
| 97 | `JDK 8 Update XX.` | Temurin 17 wording; the JDK-not-JRE warning keeps its point, and the J2EE / NetBeans advice goes — neither is a thing Adoptium ships |
| 108 | `ln -s jdk1.8.0_XX java` | `ln -s jdk-17.0.x+y java`, with the surrounding sentence adjusted |
| 148-150 | `java version "1.8.0_45"` block | Real `java -version` output from a Temurin 17 JDK (see §9.2) |
| 155 | `Dowload maven **3.3.9**` and the `archive.apache.org` URL | `3.9.9` and the 3.9.9 archive URL. The `Dowload` typo is fixed while the line is being rewritten anyway |
| 162, 171 | `apache-maven-3.3.9` in two `M2_HOME` examples | `apache-maven-3.9.9` |
| 164 | `MAVEN_OPTS='-Xmx1024m -XX:MaxPermSize=256m'` | `MAVEN_OPTS='-Xmx1024m'`, matching line 173 which is already correct |
| 184-188 | Linux `mvn -version` block: MaxPermSize warning, `Apache Maven 3.3.1`, `1.8.0_45`, `…/jre` | Real `mvn -version` output from the documented toolchain (see §9.2) |
| 210-213 | Windows `mvn -version` block: `3.3.9`, `C:\data\apache-maven-3.3.9`, `1.8.0_45`, `…\jre` | The same, in Windows form |

Two constraints on the edit, both from `docs/devops/README.md:102-137`:

- **Introduce no placeholder syntax.** The file is filtered at generation time, so any `${…}`
  written here is interpolated — including properties that hold credentials. The Linux examples
  must stay `$JAVA_HOME` / `$M2_HOME` / `$PATH` and never gain braces.
- **Do not disturb the two existing placeholders.** `${project.build.version}` at line 7 and
  `${issues.text.list}` are the generator's inputs. A grep for placeholder syntax returning exactly
  those two lines is the check that does not go stale.

**Found while building — the second placeholder moved.** The edit is a net **4 lines shorter**
(§3.3's note, plus the collapsed JDK download sub-list), so `${issues.text.list}` sits at line
**578**, not 582. Every reference to 582 in this spec was written before the edit existed; 578 is
the built value. This is exactly why the constraint is phrased as "a grep returning two lines"
rather than as two fixed numbers — the grep is what should be run, not the line numbers trusted.

### 4.2 `README.md` — regenerated, never hand-edited

`docs/devops/README.md:104-106` states the rule: "**Edit the source, never `README.md` itself** —
the next staging, release or hotfix run overwrites the generated copy and a hand edit made there is
lost without warning."

So this story does not touch it. It arrives via a `staging.yml` dispatch against the task branch,
which is how PR #107 landed the first regenerated README since **2020-02-22**. CI commits it as
`Auto-generated README.md [skip jenkins]` by `github-actions[bot]`.

The local alternative was considered and rejected. `mvn -B -Ddeployment -Dcommit.readme.phase=none
process-resources` does regenerate, but `-Dcommit.readme.phase=none` disarms only the *commit* —
`copy-readme-md` still overwrites the working tree, needing `git checkout -- README.md` afterwards
— and `${issues.text.list}` is produced by `maven-changes-plugin:github-text-list` configured
`failOnError=false`. Without a resolvable `github.personal.token` that fails soft, and the
generated file loses its issue history silently rather than failing the build. §9.3 turns that into
a check rather than an assumption.

## 5. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `README.md` | Generated. §4.2 |
| `specs/architecture/system-design.md` | Already says Java 17 — `#97` corrected it |
| `.github/workflows/ci.yml`, `api-testing.yml` | `#86` pinned them; this story makes the prose agree, not the other way round |
| The MongoDB, RabbitMQ and Tomcat prerequisites and their install sections | §10 |

## 6. Issue body reconciliation

`#85`'s body holds up better than the last three stories' did, but two points need correcting on
the issue before it is built:

1. **AC001 is not sufficient**, for the reason in §3.1 — it names `3.3.9` and "Java versions older
   than 17", and the file also contains `3.3.1`. The AC is restated in §8 in a form that catches it.
2. **The regeneration command is wrong.** The body says "via `mvn process-resources -Ddeployment`
   or equivalent". That command regenerates but does not commit, and on this machine would blank
   the issue list (§4.2). The regeneration path is a `staging.yml` dispatch.

Neither changes what the story delivers, so `#85` is **not** rewritten the way `#93` and `#103`
were. The corrections are recorded here and commented on the issue.

## 7. The parent-poms round trip

### 7.1 Why it belongs in this cycle

`specs/product/PRD.md` names `#85`/`#57` as "the same shape" as `#86`/`#58` and says to reuse that
pattern. Two further reasons found while specifying this:

- **`#57` read literally is already done, and would be closed as a no-op.** Its "Files to Update"
  section names `.md` files under `docs/`, `specs/` and `src/site/`. At `ea531f1f` no `.md` file in
  parent-poms mentions Maven 3.3.9, Java 8 or Java 11. The rot is in **APT**:
  `infrastructure/src/site/apt/maven.apt` and `infrastructure/src/site/apt/java.apt`.
- **Those two files are the ancestor of the DSH README text.** Same prose, same `3.3.1` sample
  block, same `MaxPermSize` flag, same Oracle link. The two halves are one edit in two markup
  languages, and doing them apart means diagnosing §3 twice.

`#57` is one of only two issues left on parent-poms' `3.8.0-SNAPSHOT` milestone. Clearing it does
**not** unblock the 3.8.0 release on its own — `#13` remains — but it removes half of what stands
between now and it.

### 7.2 Which round trip, and which of its steps are no-ops

`CLAUDE.md` describes a full round trip and a light one. `#57` already has an issue and a milestone,
so it is the full form — but three of its six steps are no-ops here, exactly as they were for `#58`:

| Step | Applies? |
|---|---|
| 1. Open an issue in parent-poms | Already open as `#57` |
| 2. Milestone open as a `-SNAPSHOT` | `3.8.0-SNAPSHOT`, open |
| 3. Implement and test it there | Yes — §7.5 |
| 4. Point DSH's root `pom.xml` at that SNAPSHOT to validate | **No-op.** `#57` touches no POM |
| 5. Close the issue and release parent-poms | Issue closed **by the repo owner**. Release waits on `#13` |
| 6. Re-pin DSH to the released version | **No-op**, for the same reason as step 4 |

Delivery follows `#58`'s shape: **one direct commit on parent-poms `master`**, not a pull request.
`#58` landed as `7411cbec ci(#58): pin Maven 3.9.9 in build.yml and deploy.yml`, and the recent
history there is direct commits with `(#n)` in the subject.

Per `CLAUDE.md`, Claude does not close the issue. That is the owner's.

### 7.3 The format change: APT becomes Markdown

The two files are converted to `infrastructure/src/site/markdown/maven.md` and
`infrastructure/src/site/markdown/java.md` as part of this commit, rather than edited in place as
APT. The `src/site/apt/` originals are deleted.

The reason is direction, not tidiness: the estate is moving off APT, and writing freshly corrected
prose into a format being abandoned means touching these two files twice. Conversion costs the same
whether the prose is stale or fresh.

It also makes `#57`'s own description true. Its "Files to Update" list names `.md` files under
`src/site/` — the very mismatch that made it read as already done. Converting satisfies the issue
as written instead of correcting it downward.

Two mechanics were checked before committing to this:

- **Markdown already works there.** `parent-poms/pom.xml:214-232` gives `maven-site-plugin` explicit
  dependencies on **both** `doxia-module-apt` and `doxia-module-markdown` at 2.0.0, and 15 `.md`
  site files already render. No build change is needed.
- **Navigation survives.** `infrastructure/src/site/site.xml` links by output name —
  `href="java.html"`, `href="maven.html"`. Doxia renders `markdown/maven.md` to the same
  `maven.html`, so no nav entry changes.

### 7.4 The TOC macro has no Markdown equivalent, so it is replaced rather than ported

Both files open with the APT macro `%{toc|section=1|fromDepth=2|toDepth=3}`. Six of parent-poms' 17
APT site pages use it, so how it converts decides the shape of the follow-up migration too.

Static inspection of the Doxia 2.0.0 jars in the local repository:

| Artifact | Classes referencing macros |
|---|---|
| `doxia-core` | The whole machinery — `macro/toc/TocMacro`, `macro/manager/*`, and `AbstractParser.executeMacro` |
| `doxia-module-apt` | `AptParser` and `AptParser$MacroBlock` — APT detects macro syntax itself |
| `doxia-module-markdown` | **None.** No class in the module references a macro |

`MarkdownParser$MarkdownHtmlParser extends Xhtml5Parser`, so it inherits the XML parsing path. In
that path `handleStartTag` and `handleEndTag` both declare `MacroExecutionException` while
`handleComment` does **not** — a comment cannot execute a macro. So the HTML-comment macro form has
no handler to reach.

**This is static analysis of the bytecode, not an observed render.** It is strong enough to design
around and not strong enough to state as fact, so the design removes the dependency rather than
betting on it: both converted pages replace the macro with a hand-written Markdown list of links to
their own sections. Each page is short — `java.apt` is 87 lines and `maven.apt` 156 — so the list is
four or five entries, deterministic, and needs no macro at all.

§9.4 makes the rendered result the check. If the render shows the macro would in fact have worked,
that is recorded on the follow-up issue as a finding, and the hand-written lists stay — they are
correct either way.

**Found while building — the macro was the less important half of this question.** The render
produced no macro warning, consistent with the analysis above. But it exposed a defect the analysis
could not have predicted: **Doxia generates heading anchors in its own scheme, and a hand-written
contents list must match it.** The first render emitted `id="Donwload_and_Installation"` —
capitalised, underscore-separated — while the lists had been written with GitHub-style
`#donwload-and-installation`. **Every contents link on both pages was dead, and the build was
green.** Nothing in `mvn site` warns about an unresolved in-page anchor.

Both files now use Doxia's form, verified by extracting every `href="#…"` and every generated `id`
from the rendered HTML and checking each link against the anchor set (§9.4).

This has a consequence the follow-up issue must carry: the two anchor conventions are **mutually
exclusive**. Doxia's form is correct in the generated site and wrong when the same `.md` is browsed
on GitHub, which would want the lowercase-hyphen form. The site wins, because `src/site/markdown/`
exists to feed it — and it is not a regression, since GitHub never rendered the APT originals at
all. But it is a real cost of the APT-to-Markdown move, and it applies to all 15 remaining pages.

### 7.5 Files to change in parent-poms

| File | Change |
|---|---|
| `infrastructure/src/site/apt/maven.apt` | Deleted |
| `infrastructure/src/site/apt/java.apt` | Deleted |
| `infrastructure/src/site/markdown/maven.md` | New. The §3 and §4.1 corrections, in Markdown, TOC macro replaced per §7.4 |
| `infrastructure/src/site/markdown/java.md` | New. Same |

The APT-specific constructs map as: APT bold to `**bold**`, APT monospace to backticks, APT's
`{{{url}text}}` links to `[text](url)`, `+---+` verbatim blocks to fenced blocks with a language,
and `[[1]]` lists to Markdown ordered lists.

Content beyond `#57`'s scope is carried across unchanged, not fixed: the Subversion and subclipse
instructions, the "PCCOE version of windows" paragraph, the `global-settinsg.xml` and `toos` typos,
and the two empty link targets. Those belong to whoever owns that page's content, and mixing them in
would make a version fix unreviewable. They are named in §10 so the omission is a decision.

### 7.6 The follow-up issue

A second parent-poms issue covers the remaining **15** APT site pages — the other five under
`infrastructure/src/site/apt/` (`os`, `setup`, `svn`, `tomcat`, `writing-projects-documentation`),
four under `infrastructure/maven-archetypes/src/site/apt/`, and six under the three archetypes' own
`src/site/apt/`. Its completion criterion is concrete: **`doxia-module-apt` can be dropped from
`maven-site-plugin`'s dependency list** once no `.apt` remains.

It goes on **`3.9.0-SNAPSHOT`, deliberately not `3.8.0-SNAPSHOT`**. That milestone is down to `#13`
and `#57`, and putting a 15-file migration on it pushes the 3.8.0 release further out — the
opposite of why `#85` was picked up first.

The **39** `.apt` files under `src/main/resources/archetype-resources/` are **not** in that issue.
They are template content shipped into new projects generated from the archetypes, so converting
them changes what every future project is born with. That is a separate decision, as is DSH's own
26 `.fml` FAQ files. Neither is opened by this story.

## 8. Acceptance criteria

- [ ] **AC001** — `src/site/markdown/README.md` contains no reference to any Maven version other
  than 3.9.9, and none to a Java version older than 17. Verified by the §9.1 grep, which covers
  `3.3.1` as well as `3.3.9`.
- [ ] **AC002** — `src/site/markdown/README.md` contains no `-XX:MaxPermSize`.
- [ ] **AC003** — The Java download link points at Adoptium Temurin 17, and the `ln -s` example and
  both `Java home:` sample lines describe a Java 17 layout with no `jre` subdirectory.
- [ ] **AC004** — The `java -version` and both `mvn -version` sample blocks are real output from a
  Maven 3.9.9 + Temurin 17 toolchain, produced per §9.2, not hand-written. **Met, with one
  qualification recorded rather than glossed:** the version, vendor, `runtime:` label, encoding and
  OS lines are verbatim from the run; the *paths* and the locale were genericised to the
  conventions this file already uses (`/home/[YOUR_USER]/…`, `C:\data\…`, `en_US`), because the raw
  output carries this machine's home directory and a `pt_BR` locale. The Linux block is that same
  real output transposed to Linux paths, which §9.2 anticipated.
- [ ] **AC005** — A grep for placeholder syntax in `src/site/markdown/README.md` returns exactly two
  lines, 7 and 578, and none was introduced anywhere else. **Met** — the grep returns exactly those
  two lines.
- [ ] **AC006** — Root `README.md` is regenerated by a `staging.yml` dispatch against the task
  branch, lands as an `Auto-generated README.md [skip jenkins]` commit, and differs from its source
  only at lines 7 and 578. Its issue-history table is non-empty. **Met** — run 35401633533 pushed
  `d9d565d6 Auto-generated README.md [skip jenkins]`; line 7 resolved to
  `0.3.0-SNAPSHOT - RC5 - 20260918-222811`, the issue history carries 7 milestone tables and 58
  rows, and the three region diffs in §9.3 are all empty.
- [ ] **AC007** — markdownlint passes over the repository per `CLAUDE.md`'s command. **Found while
  building: this AC is weaker than it reads, and is left as written.** That command's globs are
  `specs/**`, `.github/**`, `docs/**`, `CLAUDE.md` and `.claude/**` — they do **not** cover
  `src/site/markdown/README.md`, the only file this story edits. Linted directly, that file carries
  **143 pre-existing violations** across seven rules (MD009, MD010, MD040, MD031, MD034, MD005,
  MD022), none introduced here. Verified by linting the file at `HEAD` and in the working tree and
  comparing by rule class: the rule set is identical and MD009 falls from 61 to 58, because the
  edit removed three trailing-space lines. Bringing the file into the gate means fixing 143
  violations in a file the next story rewrites again (`#87`), so it is **not** folded in — see
  §10.
- [ ] **AC008** — In parent-poms, `infrastructure/src/site/apt/maven.apt` and `java.apt` are gone,
  `infrastructure/src/site/markdown/maven.md` and `java.md` exist, and the same greps as AC001 and
  AC002 return nothing over them.
- [ ] **AC009** — parent-poms' site renders both pages, `maven.html` and `java.html` exist with
  their content and their in-page link lists, and `infrastructure/src/site/site.xml` needs no edit.
  **Met, and it caught the story's one real defect** — see §7.4. Verified mechanically: every
  `href="#…"` in each rendered page resolves to a generated `id`, which was false on the first
  render and true on the second. `site.xml` was not touched.
- [ ] **AC010** — The follow-up migration issue exists in parent-poms on `3.9.0-SNAPSHOT`, and the
  `#57` commit SHA is commented on `#85`. **Met** — `parent-poms#70`, and the SHA comment names
  `3708e450`.
- [ ] **AC011** — `#94` carries a comment recording the two findings in §10.1: the single false
  positive that enforcement would hit on day one, and the version-drift guard as a candidate second
  check. **Met.**

## 9. Testing approach

There is no code in this story, so "test" means: run what the document tells the reader to run, and
compare.

### 9.1 The grep that defines AC001 and AC002

```bash
git grep -nE '3\.3\.9|3\.3\.1|1\.8\.0|jdk1\.8|Java 1\.8|J2SE|jdk8-downloads|MaxPermSize' \
  -- src/site/markdown/README.md
```

Must return nothing. The same pattern, run in parent-poms over
`infrastructure/src/site/markdown/maven.md` and `java.md`, defines AC008.

### 9.2 AC004 — the sample blocks are produced, not written

The three sample blocks are the only part of the page that asserts what the reader will see. They
are currently wrong in a way that proves the point: line 185 shows `3.3.1` under a heading that
told the reader to install `3.3.9`, which can only happen if someone typed it.

So they are generated. Install Maven 3.9.9 alongside the existing 3.9.16 **by following the
rewritten instructions**, point it at the Temurin 17 JDK already on the machine, run `java -version`
and `mvn -version`, and paste the actual output.

This doubles as the end-to-end test of the instructions: if following them does not produce a
working toolchain, the story is not done. The MaxPermSize defect in §3.2 is exactly what that
exercise catches.

The Windows block at 210-213 is produced on this machine directly. The Linux block at 184-188 keeps
its Linux-shaped paths, with the version, vendor and `Java home:` lines taken from the real run.

### 9.3 AC006 — the staging dispatch, and the check it needs

Dispatch `staging.yml` against the task branch, then, on the commit CI pushes:

**Found while building — the check below replaced the one written here first.** The original was
`diff <(sed '7d;578d' source) <(sed '7d;578d' generated)`, which assumes each placeholder expands to
exactly one line. `${project.build.version}` does. `${issues.text.list}` **expands to 93 lines**, so
deleting one line from each file cannot align them and the diff reports a 92-line phantom
difference. Compare region by region instead:

```bash
git pull
S=src/site/markdown/README.md; G=README.md
sl=$(wc -l < $S); gl=$(wc -l < $G); n=$((gl-sl+1))   # lines placeholder 2 expands to
diff <(sed -n '1,6p' $S)        <(sed -n '1,6p' $G)          # before placeholder 1
diff <(sed -n '8,577p' $S)      <(sed -n '8,577p' $G)        # between the placeholders
diff <(sed -n "579,${sl}p" $S)  <(sed -n "$((578+n)),${gl}p" $G)   # after placeholder 2
```

All three must be empty, and line 7 must have resolved to a version-build-timestamp string.
Measured: source 578 lines, generated 670, so `${issues.text.list}` expanded to 93 and every
region matched.

Then read the diff rather than trusting it. `${issues.text.list}` is produced with
`failOnError=false`, so a token that does not resolve yields an **empty** table and a green build.
The regenerated `README.md` must still carry its per-milestone issue tables. If it does not, the
dispatch failed silently and the commit must not be kept.

### 9.4 AC009 — the parent-poms site render

Build the `infrastructure` module's site locally and open the two pages. Per `CLAUDE.md`'s rule,
Maven never runs as a silent blocking command — it runs to a log with the exit code reported.

The log goes **outside the parent-poms working tree**. DSH gitignores `.logs/`; parent-poms does
not, and has no logging convention of its own, so writing one there leaves an untracked directory
in a repository whose `readme-generation` profile has a `git commit -a` fallback path (`#97` §4.2).
Use a path under the session scratchpad instead:

```bash
LOG=/tmp/mvn-site-infrastructure.log   # or the session scratchpad; not the parent-poms tree
mvn -B -pl infrastructure site > "$LOG" 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f $LOG"
wait $MVN_PID; echo "maven exit=$?"
```

Confirm `target/site/maven.html` and `target/site/java.html` exist, carry the converted content,
and that the nav entries from `site.xml` still resolve. Record whether any macro-in-comment form
rendered, for §7.4's open question and the follow-up issue.

**A green build is not evidence the pages are right.** Extract every `href="#…"` and every generated
`id` from each rendered page and check each link against the anchor set — an unresolved in-page
anchor produces no warning and does not fail `mvn site`. This is what caught the defect in §7.4:

```bash
for p in java maven; do
  ids=$(grep -oE 'id="[A-Za-z][^"]*"' "$p.html" | sed 's/id="//;s/"//' | sort -u)
  for l in $(grep -oE 'href="#[^"]*"' "$p.html" | sed 's/href="#//;s/"//' | sort -u); do
    echo "$ids" | grep -qx "$l" || echo "BROKEN -> $p.html #$l"
  done
done
```

### 9.5 What is not tested

Nobody installs MongoDB, RabbitMQ or Tomcat from these instructions as part of this story. Those
sections are untouched (§10) and their correctness is not claimed either way.

## 10. Out of scope

- **The MongoDB 3.4 / 4.0.6, RabbitMQ 3.6.14 / 3.7.14 and Tomcat 8.0.X prerequisites** and their
  install sections. ADR-001 Waves 2 and 3 replace MongoDB and RabbitMQ with Firestore and Pub/Sub,
  so re-documenting them now is throwaway work. They are stale; this story does not pretend
  otherwise.
- **`#87`'s bump of 3.9.9 to 3.9.16.** It is in Wave 1 and will rewrite these same lines. Named here
  so the overlap is a recorded decision rather than a surprise — `#87` moved out of Wave 0 precisely
  to stop `#86` and `#87` contradicting each other inside one wave, and the same reasoning puts the
  second write of these lines there.
- **A drift guard** tying the documented Maven version to the workflow pin. Deferred to `#94`
  rather than simply dropped — see §10.1.
- **parent-poms' other APT rot** — Subversion and subclipse, the "PCCOE" paragraph, the
  `global-settinsg.xml` and `toos` typos, the empty link targets. §7.5.
- **The other 15 APT site pages** and the **39** archetype-resource `.apt` files. §7.6.
- **DSH's 26 `.fml` FAQ files.** A different non-Markdown site format, not named by this request.
- **Bringing `src/site/markdown/README.md` into the markdownlint gate.** Found while building
  (AC007): the file sits outside the enforced globs and carries 143 pre-existing violations. Fixing
  them is a whitespace-and-fences rewrite of a 578-line file that `#87` edits again, and it would
  bury this story's ten substantive line changes in several hundred cosmetic ones. It is a
  candidate issue, not part of this one.

### 10.1 Why the drift guard goes to `#94` rather than into this story

The obvious reflex is to make `#94`'s `check-spec-references` cover the documents this story
changes, so that if `#94` is decided in favour of enforcing, this work is protected. **That does not
work, and the reason is worth recording so it is not proposed again.**

`check-spec-references` (`.github/workflows/spec-validation.yml:80-91`) reads
`.github/copilot-instructions.md`, extracts backtick-quoted paths beginning `/`, and checks each
exists on disk. It has no knowledge of version strings. The only file this story touches that
appears in `copilot-instructions.md` is `/docs/devops/README.md`, which §5 leaves unchanged. So
enforcing it would validate nothing `#85` produces, and `#94`'s own body scopes out widening what
it checks.

Two findings go on `#94` instead (AC011):

1. **Enforcement fails on day one, on a false positive.** Running the job's script against the
   current tree produces exactly one would-be failure: `/docs/wiki/<Page-Name>.md`. That is a
   placeholder inside prose, not a reference. `#94`'s AC002 requires the job to pass on the current
   tree, so whoever builds it must first decide how a documentation placeholder is exempted from a
   path check. Nothing on the issue says this today.
2. **A version-drift guard is the candidate second check.** Tying the Maven version documented in
   `src/site/markdown/README.md` to the version pinned in the workflows by `#86` would have caught
   the `3.3.1` in §3.1 automatically, and would make `#87`'s bump a one-line change instead of
   another manual sweep.

Both belong on `#94` because it is the issue that decides whether this workflow holds gates at all.
Settling that question for one check and rediscovering the other afterwards is the rework this
avoids. If `#94` resolves towards removing the job, the drift guard becomes its own issue then,
decided with that answer in hand.

## 11. Implementation order

1. Comment the two §6 corrections on `#85`.
2. Rewrite `src/site/markdown/README.md`, leaving the three sample blocks as placeholders.
3. Install Maven 3.9.9 by following the rewritten instructions; run `java -version` and
   `mvn -version`; paste the real output into the three blocks (§9.2).
4. Run §9.1 and markdownlint. Commit.
5. parent-poms: convert the two files (§7.3-7.5), render the site (§9.4), commit on `master` as
   `docs(#57): …`.
6. Comment the `#57` SHA on `#85`. Ask the owner to close `#57`.
7. Open the follow-up migration issue on `3.9.0-SNAPSHOT` (§7.6), once its content is approved, and
   comment the two §10.1 findings on `#94`.
8. Push the task branch, dispatch `staging.yml` against it, and verify the regenerated `README.md`
   per §9.3.
9. Steps 5 and 6 of the process — local review, then the pull request.

# DSH Product Requirements Document (PRD)

## 1. Purpose

This PRD turns the proposed GCP migration in
[`specs/architecture/ADR-001-GCP-based-components.md`](../architecture/ADR-001-GCP-based-components.md)
into schedulable waves of work, and triages the existing GitHub issue backlog into those waves.
It is the single place that says what DSH builds next and in what order.

The PRD is step 1's output in the eight-step process described in
[`docs/process/ai-driven-development.md`](../../docs/process/ai-driven-development.md): the
`dsh-plan-wave` skill updates it after a brainstorming pass, `dsh-new-story` reads a task from it
to draft a GitHub issue, and the wave-to-milestone mapping in §3 tells `dsh-new-story` which
milestone to set on that issue. Step 8's `dsh-reconcile-prd` closes the loop the other way,
reconciling this document against GitHub once a story has merged and its issue is closed.

ADR-001 itself is **Status: Proposed**. No migration code exists yet — the codebase has no GCP
dependencies and no code marked `@Deprecated`. Nor does the document pipeline ADR-001 would migrate:
§4's "The 2026-09-28 re-plan", between Wave 0 and Wave 1, records what exists, what does not, and
the decisions that reordered the waves around building it. "The 2026-10-10 re-plan", after it,
records the operated tiers on GCP and the `0.6.0` milestone that first releases through them.
"The 2026-10-13 re-plan" records what
[ADR-003](../architecture/ADR-003-document-pipeline.md), **Status: Accepted**, changed in the
waves. It decides how the pipeline is built, and it removes the legacy code where ADR-001
planned to deprecate it.

## 2. How to read a wave

- Waves are **ordered**. A task belongs in the earliest wave whose dependencies it satisfies.
- Each task listed under a wave is meant to become one INVEST story via `dsh-new-story` (step 2 of
  the process) — independent, negotiable, valuable, estimable, small, and testable. A task too
  large for one task branch gets split into more than one story at that point, not written as a
  single oversized story.
- An issue number next to a task (e.g. `#48`) means a GitHub issue exists for it and should be
  referenced from the resulting story. Wave 0's items all have issues; later waves' tasks mostly do
  not yet, and become issues via `dsh-new-story` when they are picked up.
- Where an item has an issue, **the issue is the source of truth** for its rationale and acceptance
  criteria — this document should not restate them, so the two cannot drift apart.
- Where a wave lists its issues in a table, the `Status` column carries only what GitHub says:
  `open`, or `closed` naming the pull request that delivered it. It is maintained by step 8
  (`dsh-reconcile-prd`) after a merge, never hand-edited ahead of one. Titles in that column are
  the GitHub titles, normalised to sentence case with any `[STORY]` prefix dropped.
- Waves 1-5 used to mirror the five migration phases in ADR-001 §4 one for one. Since the re-plan of
  2026-09-28 they do not: a task still drawn from an ADR-001 phase table says so. ADR-003
  (Wave 1 task 2) revised ADR-001 on 2026-10-13, and each of its phases names the wave that
  delivers it.
- A wave can list issues in other repositories (parent-poms, the `maven-changes-plugin` fork,
  maven-repo) when DSH's work depends on them. They are linked in full, since a bare `#n` here means
  a DSH issue.

## 3. Wave-to-milestone mapping

| Wave | Milestone |
|---|---|
| 0 | `0.3.0` (released 2026-09-29) |
| 1 | `0.4.0-SNAPSHOT` |
| 2 | `0.4.0-SNAPSHOT` |
| 3 | `0.5.0-SNAPSHOT` |
| 4 | `0.6.0-SNAPSHOT` |
| 5 | `0.6.0-SNAPSHOT` |
| 6 | `0.6.0-SNAPSHOT` |
| 7 | `0.6.0-SNAPSHOT` |
| 8 | none: retired on 2026-10-13 (§4) |
| 9 | `1.0.0-SNAPSHOT` |
| 10 | `1.0.0-SNAPSHOT` |
| 11 | `1.0.0-SNAPSHOT` |

Waves 4 to 11 carry the numbers and milestones of the re-plan of 2026-10-10 (§4, "The 2026-10-10
re-plan"), which added Waves 4 and 7 and the `0.6.0` milestone.

Wave 1's upstream step 0 was the exception. Its DSH issue, `#146`, was built on the `0.3.x` hotfix line
and shipped as a patch release, not in `0.4.0`. It went on `0.3.1-SNAPSHOT`, which was renamed `0.3.2`,
because the release shipped as 0.3.2 (§4, Wave 1, step 0). The milestone was closed on 2026-09-30.

Every milestone in the table exists in GitHub. `0.5.0-SNAPSHOT` was created on 2026-10-16, when
`#163` became Wave 3's first issue. `0.6.0-SNAPSHOT` was created on 2026-10-10, ahead of its first
new issue, because `#45` and `#49` moved onto it that day. `dsh-new-story` sets `--milestone` on
`gh issue create` from this table, keyed by the wave the task came from.

**parent-poms has its own milestones, one per wave that carries parent-poms work.** A wave's
upstream issues go on the milestone below, so that a parent-poms release clears one wave's work and
does not wait for a later wave's. Decided on 2026-10-02, when every open parent-poms issue was given
a milestone.

| Wave | parent-poms milestone | Issues on 2026-10-02 |
|---|---|---|
| 1, through upstream step 5 | `3.10.0-SNAPSHOT`, now `3.10.0` | all six closed; released on 2026-10-03 |
| 1, upstream step 6 | `3.11.0-SNAPSHOT`, now `3.11.0` | `#98`, `#106`; both closed; released on 2026-10-04, DSH not re-pinned |
| 4 | `3.12.0-SNAPSHOT` | `#90`; `#109` joined on 2026-10-10 |
| 7 | `3.13.0-SNAPSHOT` | `#91`, since 2026-10-10 |
| 10 | `3.14.0-SNAPSHOT` | `#92`, since 2026-10-10 |
| 11 | `3.15.0-SNAPSHOT` | `#85`, since 2026-10-10 |

A wave with no row has no parent-poms work planned. When one gains some, it gets the next minor.

The last four rows were re-keyed by the re-plan of 2026-10-10. Until then `3.12.0-SNAPSHOT` was the
distribution wave's and held `#90`, `#91` and `#92`, and `3.13.0-SNAPSHOT` was the backlog's and
held `#85`. The distribution work was split across three waves, so `#91` moved to
`3.13.0-SNAPSHOT`, and `#92` and `#85` moved to two milestones created that day.

## 4. The waves

### Wave 0 — Engineering foundation

Milestone: `0.3.0`, **released on 2026-09-29** as tag `v0.3.0` by release run
[36606687680](https://github.com/MRISS-Projects/dsh/actions/runs/36606687680). The milestone was
renamed from `0.3.0-SNAPSHOT` before the release and closed after it. Housekeeping and build-health work that has no dependency on the GCP
migration, plus gaps found while writing this PRD.

**Triaged issues.** `Status` is reconciled against GitHub in step 8 of the process
(`dsh-reconcile-prd`); the issue itself remains the source of truth for rationale and acceptance
criteria.

The table starts at `#85`. Nine earlier issues carry this milestone — `#13`, `#14`, `#66`, `#67`,
`#68`, `#71`, `#72`, `#83`, `#84` — and are deliberately absent: all nine closed before this PRD
existed, and listing completed pre-PRD work would grow the table without informing anyone. A
reconciliation that rediscovers them should leave them out.

| Issue | Status | Title |
|---|---|---|
| `#85` | **closed** — PR #109 | Update documentation: replace Maven 3.3.9 with 3.9.9 and standardise Java version to 17 |
| `#86` | **closed** — PR #108 | Pin Maven 3.9.9 in all GitHub Actions workflows that invoke Maven |
| `#87` | **closed** — PR #126 | Update Maven pinned version from 3.9.9 to 3.9.16 in documentation and GitHub Actions |
| `#43` | **closed** — no PR; already met by parent-poms `3.9.0` | Configure surefire, jacoco and other useful reports for the maven generated docs |
| `#46` | **closed** — PR #141 | Implement integration tests using embedded tomcat server |
| `#70` | **closed** — PR #130 | Project link not working at maven generated site |
| `#90` | **closed** — PR #129 | index.html missing from published site on gh-pages (root + all submodules) |
| `#92` | **closed** — PR #96 | Remove the dead Travis build estate |
| `#93` | **closed** — PR #102 | Remove the redundant coverage ratchet — `jacoco:check` at 95% is already inherited |
| `#94` | **closed** — PR #110 | Make `check-spec-references` enforcing, or remove it |
| `#95` | **closed** — PR #98 | Use a read-only token for CI package authentication |
| `#97` | **closed** — PR #107 | Resolve the tooling orphaned by the Travis estate removal |
| `#99` | **closed** — PR #100 | Standardise Maven builds on `-U` while the parent is a SNAPSHOT |
| `#101` | **closed** — PR #106 | Fail CI when the package token cannot authenticate, not just when it is absent |
| `#103` | **closed** — PR #105 | Make PR review rounds repo-aware and authoritative |
| `#104` | **closed** — PR #131 | Regenerate the coverage badge, or stop publishing a stale one |
| `#111` | **closed** — PR #116 | Let `release.yml` and `hotfix.yml` dispatch a release rehearsal |
| `#112` | **closed** — PR #121 | Reclassify the Spring-context tests as integration tests and pay the unit-coverage bill |
| `#113` | **closed** — PR #136 | Remove the dead `main` branch trigger from `api-testing.yml` and `documentation-sync.yml` |
| `#114` | **closed** — PR #116 | Release and hotfix wrappers do not supply the build properties DSH's reactor needs |
| `#115` | **closed** — PR #135 | `version.properties` ships an unresolved `${jenkins.build.number}` in two modules |
| `#117` | **closed** — PR #118 | Pass `development_branch` to the release and hotfix wrappers |
| `#122` | **closed** — PR #134 | Close the file streams that test fixtures leave open |
| `#123` | **closed** — PR #125 | Stop passing the Mongo setup inputs to `project-staging.yml` |
| `#124` | **closed** — PR #133 | Keep `dsh-test-dataset` fixtures and test classes out of production artifacts |
| `#139` | **closed** — PR #141 | `dsh-data` connects to MongoDB unauthenticated, with connection settings fixed at build time |
| `#143` | **closed** — PR #145 | Make the README and the site index describe what DSH does today |

`#143` was added by the re-plan of 2026-09-28 (§4, "The 2026-09-28 re-plan"):

- **Make the README and the site index true for 0.3.0.** The 0.3.0 release regenerates `README.md`
  from `src/site/markdown/README.md`, and that source describes a product that does not exist yet.
  It says DSH ships as a WAR for Tomcat 8, when `dsh-rest-api` is packaged as a jar. It lists
  MongoDB 3.4 and RabbitMQ 3.6 as prerequisites. Its `dsh-doc-processor-worker` entry repeats the
  keyword extractor's text. And nothing in it says that only submission works today. The fix states
  that status, corrects the prerequisites and module descriptions, and uses generic terms (indexer,
  NoSQL store, queue) in the overview. `src/site/markdown/index.md` gets the same status statement.
  The larger rewrite of both files is Wave 1 task 5 and Wave 10 task 5.

Issues `#92`, `#93`, `#94` and `#95` were raised from findings made while writing this PRD and
while reviewing the branch that introduced it; each carries its full rationale and acceptance
criteria. The paragraphs that originated them have been removed from this document — the issues are
now the single source of truth for that work.

`#93` was **rewritten** before it was built, and its entry above carries the new title. It was
opened on a false premise — that DSH had no coverage threshold — and ended up asking for the
opposite of what it originally asked: `jacoco:check` at 95% is inherited from parent-poms, per
module and build-failing, so the second gate (`scripts/check-coverage.sh` plus
`.github/coverage-baseline.txt`) was the redundant one. Shipped in PR #102, which removed that gate
and left `jacoco:check` as the only one.

Closing the blind spot the removal opened needed a change in `parent-poms`, not here:
`jacoco:check` skips a module that produced no exec data, so a module with production classes and
no tests would have passed at 0%. That went upstream as two direct commits under `CLAUDE.md`'s
light round trip, both referenced on `#93`. The reason there were two is recorded in `#103` below.

`#99` came later still, spun off from `#95`'s out-of-scope list while that story was being built:
`ci.yml` and `api-testing.yml` disagreed on `-U`, so the two could resolve different parent
SNAPSHOTs from the same commit. It was resolved *towards* `-U` rather than away from it — while the
parent is a `-SNAPSHOT` and this repository is the first consumer of `parent-poms` changes,
tracking the current parent on every run is the intended contract, and omitting `-U` never bought
reproducibility in the first place. Shipped in PR #100.

`#101` was spun off from `#99` during that story's review: `ci.yml`'s credential step checks that
`PACKAGES_READ_TOKEN` is non-empty, never that it authenticates, so a dead token yields either a
green build against a cached parent or — once the cache misses — a failure that reads as a broken
parent rather than a broken credential. It predates `#99` and was left out of it deliberately:
`#99` changed how often the symptom appears, not the presence-only check that hides it, and
folding a second CI behaviour change into a flag-and-prose story would have made both harder to
review. Shipped in PR #106.

`#97` was spun off from `#92` when that story shipped: removing the Travis estate deliberately left
three things behind — four uncalled `mvn` wrapper scripts, the `update-readme` Maven profile whose
only caller was the deleted `post-release-script.sh`, and the generated-vs-checked-in status of
`README.md`. `#92`'s spec promised the follow-up rather than widening its own scope.

Shipped in PR #107. Two of the issue's premises did not survive checking and the spec corrected
them rather than implementing against them. The third — that `update-readme` was merely callerless
— was the smaller half of a defect in `parent-poms`: three of the four README executions carried
`<inherited>false</inherited>`, which excludes every descendant POM, so README regeneration was
unreachable from *every* consuming project and the `Update README.md on Master` step of the release
and hotfix workflows was a no-op everywhere. That went upstream as `parent-poms#68`, now closed and
released in **3.8.0**; DSH inherits a `readme-generation` profile and keeps no local replacement.
The story also deleted five module `readme.md` site pages, because Maven's `<file><exists>` is
case-insensitive on NTFS and those files made the new profile activate — and `maven-scm-plugin`
fall through to `git commit -a` — in five modules that had no README to stage. The first README
this repository has generated since **2020-02-22** landed inside PR #107 itself, pushed there by a
`staging.yml` dispatch against the task branch.

`#103` was spun off from `#93`'s review round. Copilot raised six findings on PR #102; five were
right, and one — repeated in 3 of its 9 comments — asserted that `-Denforcer.skip=true` bypasses
the new coverage-data guard and "must be verified/fixed in the shared parent". Running it showed
the opposite, and produced the second upstream commit: the guard is unaffected, but DSH's *other*
enforcer execution has no `<skip>` and **is** disabled by that flag, so the caveat was wrong in a
different way than the review claimed. `#103` was opened on the reading that two gaps made that
round expensive, and **the first of the two turned out to be false.** Copilot code review reads
`CLAUDE.md` from the head branch of the pull request, and `CLAUDE.md` at `fb3f0a09` — the exact
commit reviewed — already stated in bold that both coverage gates are inherited from `parent-poms`
and will not be found by grepping this repository, and that `-Denforcer.skip=true` does not reach
the guard. The context was present and did not reach the finding, so the gap is salience and
reviewer effort, not absence; `#102` was reviewed on `Lite`. The second gap stands as written:
nothing said who adjudicates a disputed finding, or that evidence rather than seniority settles it.
`#93` was not widened to cover either: they concern how a review round is set up and arbitrated,
not the coverage gate, and one of the fixes lands in a `.github/skills` file that has nothing to do
with coverage. Shipped in PR #105, which added `.github/skills/code-review/SKILL.md` as the
reviewer-facing seat and wrote the adjudication rule into `dsh-pr-cycle` and step 7 of the process
doc. Like `#93`, it was **rewritten before it was built**, because the premise it was opened on
did not survive checking.

`#104` came out of reconciling this document after `#93` merged. The badge-versus-aggregate
disagreement had sat in §6 as a risk phrased as an open question — "either they measure different
scopes or the badge is stale". Checking it answered the question: the badge reads the same
`INSTRUCTION` counter from the same aggregate CSV as the 98.13% figure, so it is stale, and it is
stale because the `process-badges` profile can only be activated with `-P`, which this estate
deliberately abandoned. A diagnosed defect with a known cause is work, not a risk, so it became an
issue. It was not folded into `#93` because that story removed a coverage *gate* and explicitly
preserved the reporting path the badge belongs to — the two touch the same module and nothing else. It
shipped in PR #131: `-Ddeployment` builds now generate the badge and publish it with the site, and
nothing commits it back.

`#112` was spun off while `parent-poms#67` was being specced, and it is `#46`'s missing half. `#67`
settled what an integration test *is* for this estate — a test that starts a Spring context; a unit
test uses Mockito and no context — and measuring DSH against that definition found eight of its
twenty-five test classes already on the wrong side of it. The reclassification is not a rename:
four worker modules have exactly one test class each and it *is* the `@SpringBootTest` smoke test,
so moving it out of surefire leaves those modules producing no `jacoco.exec` at all and trips the
inherited coverage-data guard on every ordinary build. The bill is paid with unit tests, never with
a per-module exemption. It was kept out of `#46` because `#46` is about standing up an embedded
server, while this is about where the tests that already exist belong; folding them together would
have made a definition change ride on a new-capability story.

Shipped in PR #121. The eight were sorted rather than blanket-renamed: a test that used Spring
without needing it was rewritten context-free and stayed a unit test, and only the six that
exercise the context became `*IT`. A CI check now keeps the rule, and PowerMock was banned
estate-wide during specification. Both upstream changes went to parent-poms by the light round
trip and are referenced on `#112`: the `ban-powermock` enforcer execution, and a failsafe
`classesDirectory` fix that the story's own integration build proved necessary — after
`repackage`, `dsh-rest-api`'s ITs could not load the module's classes.

`#122` was spun off from `#112`'s local review and was deliberately not folded into it. Test
fixtures open `FileInputStream`s that nothing closes. `#112` made this more frequent: its
context-free `DocumentTest` rebuilds the fixtures before every test instead of once per context.
But `#112`'s spec required the fixture bodies to be carried over unchanged, and three of the six
affected test files lie outside that story's scope. Shipped in PR #134, as a test-only change: none
of the consumers the streams were handed to closes them, so the fix belonged to the tests that
opened them. It found 20 sites, not the issue's 18, because two spelled the class fully qualified.

`#46` was written up in the same session rather than retitled. It is the *other* half of `#112`,
and the two are not the same shape: `#112` moves in-process Spring-context tests, while `#46` binds
`spring-boot:start` and `spring-boot:stop` to `pre-integration-test` and `post-integration-test` so
integration tests run against the application over real HTTP. Its title was accurate all
along; what it lacked was a body, which it had never had. Scoping it turned up that
`api-testing.yml` is a green no-op — `specs/api/postman/` holds only a `README.md`, so the Postman
step skips and the job passes after building the reactor and booting the application to assert
nothing. Shipped in PR #141. The application is forked from `target/classes` rather than from the
repackaged jar, and it runs against MongoDB and RabbitMQ started in Docker by the same profile. The
story also set out the four test layers now in `testing-patterns.md`.

`#139` was found while building `#46` and fixed in the same pull request, yet kept as its own issue.
The forked application could not reach the test MongoDB. `dsh-data`'s context XML had been filtered
at build time, fixing its host and port, and its `credentials` attribute was never read, so every
environment had connected unauthenticated. The fix was unavoidable for `#46`, but the defect is a
production behaviour change in another module, and it deserved its own record. PR #141 referenced
it rather than closing it, so it was closed by hand after the merge.

`#113` came out of that same scoping and was deliberately not folded into `#46`. Two workflows
listed `main`, a branch this repository has never had, as a push trigger; a dead trigger has
nothing to do with the integration-test lifecycle, and bundling them would have put a one-line
cleanup behind a story with an open design question. Shipped in PR #136, which also corrected four
docs that restated the trigger, three of them with a lowercase `develop`.

`#123` is the consuming half of `parent-poms#78`, and it had to be a separate issue because the
two changes live in different repositories. `parent-poms#78` removed the input names `#123` stopped
passing, so `#123` had to merge first, the reverse of `#117`. It shipped in PR #125, and both
halves were proven by one staging run against the upstream branch. The design it rests on is
recorded under `parent-poms#78` below.

`#124` was spun off from shipping `#123`, and has nothing to do with it. `#123`'s local
`mvn -B install` failed `dsh-rest-api` at 0.50 line coverage with no Java or POM in its diff. Four
modules unpacked `dsh-test-dataset` with `maven-remote-resources-plugin` into
`target/test-classes`, and left the goal's `attachToMain` at its default, `true`. So every build
copied the fixtures into `target/classes` and the production jars, and a build without `clean`
copied the previous build's compiled test classes as well. The three `*IT` classes among them
escaped `jacoco:check`'s `*Test` excludes. CI never saw the gate failure, because it always starts
from a fresh checkout. The issue required investigating whether anything relied on the main-side
attachment before it was turned off.

**`#104` shipped `#124`'s configuration change first.** Its badge could not match the aggregate
while test classes leaked into it, so PR #131 set `attachToMain=false` in all four modules. `#104`'s
spec did not mention `#124`, and the overlap went unnoticed until a reconciliation of this document.
`#124` then shipped the rest in PR #133. The investigation found nothing that relied on the
main-side attachment. The two Solr modules, which never read a fixture, lost their unpack, and the
setting now carries a comment saying why it must stay `false`. What the dataset lacks for the
later waves is recorded under Wave 3, and the 1 MiB fixture under Wave 5.

**Two findings from the same review are deliberately *not* issues:**

- **The markdown lint glob already covers `.claude/**`.** `.github/workflows/spec-validation.yml`'s
  `validate-markdown` job and its trigger `paths:` were widened on the branch that created this
  PRD, so the project skills under `.claude/skills/` sit inside the enforcing gate. Done, not
  pending.
- **The JaCoCo aggregate's scope is already complete — there is nothing to widen.** Verified
  directly: `dsh-coverage-report/pom.xml` depends on all 8 code-bearing modules (`dsh-data`,
  `dsh-rest-api`, `dsh-doc-indexer-worker`, `dsh-doc-processor-worker`, `dsh-keyword-extractor`,
  `dsh-top-sentences-extractor`, `solr-advanced-numbers-filter`, `solr-terms-vector-order`) plus
  `dsh-test-dataset` (no main sources), and the aggregate CSV contains 15 packages spanning all 8.
  The remaining modules of the 13 are aggregator POMs (root, `dsh-doc-analyser`, `dsh-solr`,
  `dsh-coverage-report`) with no production code. The repository has 38 main `.java` files, so
  2,028 instructions **is** the entire codebase, not a slice.

  This matters because it reframed `#93`: the coverage floor is sensitive to a single new class
  because the codebase is genuinely small, not because the measurement is partial. An earlier draft
  of this PRD claimed the scope was narrow and asked for it to be widened — that was wrong, and it
  had gated the baseline work behind a task that could never complete. `#93` has since been
  rewritten around the real problem — see the note above the table.

**Wave 0 also has a goal in another repository.** DSH inherits from
`com.mriss.mriss-parent:products`, maintained in `MRISS-Projects/parent-poms`. Wave 0 is not
finished until `3.9.0-SNAPSHOT` is cleared, **3.9.0** is released, and DSH is re-pinned to it.
`3.10.0-SNAPSHOT` exists to hold work deliberately deferred past that release, and is listed here
so it is not mistaken for part of the goal:

| parent-poms milestone | Open issues | Outcome |
|---|---|---|
| `3.8.0` | none | **Released 2026-09-19** — cleared by `#13` |
| `3.9.0` | none | **Released 2026-09-27** — cleared by `#70`, tagged `mriss-parent-3.9.0` |
| `3.9.1` | none | **Closed 2026-09-29 without a release** — held only `#93`, a fix to the `@master`-pinned `commit-readme` action, raised by DSH `#143`'s staging run. No artifact changed, so DSH stays on `3.9.0` |
| `3.9.2` | none | **Released 2026-09-29**, tagged `mriss-parent-3.9.2`, as the hotfix line for DSH 0.3.0's release site. It was cleared by `#95` and `#96`, which were fixed together by PR #97 and moved here from `3.10.0-SNAPSHOT`. DSH `0.3.x` names it, and `DEVELOP` inherits it through the 0.3.2 merge-back. Wave 1, upstream step 0 |
| `3.10.0-SNAPSHOT` | `#74`, `#81`, `#86`, `#88`, `#89` | Opened 2026-09-20 to hold deferred work. Does **not** gate Wave 0; cleared and released at the start of Wave 1 |

**This goal is met.** `3.9.0-SNAPSHOT` was renamed to `3.9.0` before the release, as `3.8.0` was,
released on 2026-09-27 and closed. DSH's root `pom.xml` names the released `3.9.0`, and a staging
run against it on `staging-0.3.0-SNAPSHOT-RC` passed, with the README complete and every coverage
report regenerated. **From here no parent-poms issue is picked up until DSH 0.3.0 is released and
work is back on `DEVELOP`.** That included `3.10.0-SNAPSHOT` and the then unmilestoned
`parent-poms#85`. What was left of Wave 0 was DSH's own `0.3.0-SNAPSHOT` issues. The last of them,
`#143`, closed on 2026-09-29, and the milestone has no open issue. DSH 0.3.0 was released the same
day, and work is back on `DEVELOP`.

**The first half is the history below.** `parent-poms#13` was the last issue on `3.8.0-SNAPSHOT`; it was
fixed, the milestone was cleared, and **3.8.0 was released on 2026-09-19**, tagged
`mriss-parent-3.8.0`. The milestone was renamed from `3.8.0-SNAPSHOT` to `3.8.0` before the release
rather than after, because `maven-changes-plugin:github-text-list` prints each milestone's title
verbatim as a release-notes heading — every historical section carries the released name, and the
released site and `README.pdf` snapshot whatever the heading said at release time.

`parent-poms#67` was raised from this work: `maven-failsafe-plugin` was configured there in
`<pluginManagement>` with the right includes, but never activated, so integration tests could not
run in any inheriting project. **It is closed** — `parent-poms#75` merged on 2026-09-21, spec and
evidence at `parent-poms/specs/67-activate-failsafe-integration-tests.md`. The profile is keyed on
`-DintegrationTests`, so `mvn clean install` keeps running unit tests only.
The inherited 95% `jacoco:check` keeps measuring unit-test coverage, but **not by
default**: failsafe's `argLine` defaults to `${argLine}`, the property `jacoco:prepare-agent`
writes, so the obvious activation would have appended integration coverage into the exec file the
gate reads. A second JaCoCo agent writing `jacoco-it.exec` is what keeps the gate honest. And
`project-staging.yml` passes the flag, so integration tests are mandatory on every staging build of
every inheriting product rather than opt-in per project — parent-poms supplies the `-D` and nothing
more, leaving what an integration test *starts* to each product. DSH `#46` and `#112` both depend
on it, and both have shipped on it: `#112` in PR #121 and `#46` in PR #141. Closing `#67` does
not advance Wave 0's own condition, which is the **3.9.0 release** and the re-pin. That no longer waits on any issue: `#59` closed with DSH `#87` (see the
third pair below), and `#70`, the last one on the milestone, closed on 2026-09-27.

`parent-poms#69` was raised from `#97` and deliberately left there rather than folded into it.
`project-release.yml` re-versions a newly cut hotfix branch with
`mvn -DprocessAllModules=true -DnewVersion=<v> versions:set`, and that command was analysed as
writing **only the root POM** — one of 13 — which would leave a hotfix branch whose twelve modules
name a parent version that does not exist. It surfaced while choosing `set-version.sh`'s body, not
by working on the release path, and it sat on `3.9.0-SNAPSHOT` so it did not add to what had to be
cleared before **3.8.0** was released.

**`#72`'s rehearsal measured it, and the count was right.** The first release rehearsal against the
real 13-module reactor logged
`REHEARSAL evidence for #69: versions:set modified 1 of 13 pom.xml file(s)` —
[`dsh` run 35662168807](https://github.com/MRISS-Projects/dsh/actions/runs/35662168807). `#69` was
briefly closed on 2026-09-22 and reopened the same day, because its body asked for exactly that
confirmation before the workflow was changed.

**`#69` is closed** — `parent-poms#80`, merged 2026-09-24, spec and evidence at
`parent-poms/specs/69-set-hotfix-version-on-every-module.md`. Two things that were written here
while it was open turned out to be wrong, and both were corrected by building it:

- **The consequence was overstated.** This section previously said the stale parent made the next
  command, `scm:checkin`, unable to build the project model, so `#69` failed `project-release.yml`
  outright. It does not. `#72`'s own rehearsal bridge installs the release-version artifacts into
  the runner's local repository, and a real release has just deployed them to the registry, so the
  parent resolves in **both** modes and `scm:checkin` succeeds. The real failure is quieter and
  worse: the `0.3.x` line is committed and pushed with twelve modules still naming the released
  version, and nothing complains. That is why the fix is a *positive* assertion that every module
  carries the hotfix version, rather than reliance on an error.
- **The fix is not the one the issue proposed.** `release:update-versions` is driven through
  `build.NEXT_DEVELOPMENT_VERSION`, which `pom.xml` binds `<developmentVersion>` to, so it becomes
  the default for every project in the reactor. The first implementation used
  `-Dproject.dev.<groupId>:<artifactId>`, which moves only the root — and it passed a full green
  rehearsal, because the hotfix version under test, `0.3.1-SNAPSHOT`, is exactly what the release
  plugin's default version policy produces from `0.3.0` unaided. Code review caught it; re-running
  at `0.9.9-SNAPSHOT` separated the two mechanisms. The lesson is worth more than the fix: **to
  validate a command that sets a value, choose a value the system would never have chosen itself.**

A new composite action, `verify-reactor-version`, now runs in real releases and rehearsals alike
and fails the step with the offending modules named. It has already earned its place by catching
the wrong implementation above.

**`parent-poms#65` is closed.** It was fixed by `parent-poms#82`, merged 2026-09-25, with spec and
evidence at `parent-poms/specs/65-merge-release-back-into-develop.md`. Both release workflows now
end by merging the release tag into the consumer's development branch, which keeps its own
version. Without that, every fix made on an RC branch would have reached `master` and never
`DEVELOP`, and for this repository that was the 152 commits of this wave that existed only on
`staging-0.3.0-SNAPSHOT-RC`. Like `#69`, it was built against a corrected premise rather than the
one the issue proposed:

- `-X ours` would have silently discarded any RC fix that conflicted with `DEVELOP`.
- Its `versions:set` safety net was the command `#69` measured writing 1 of 13 POMs.

The fix instead aligns the tag to the development branch's version before a **plain** merge, so a
real conflict stops the run rather than being resolved by picking a side. The review round added a
retry for a development branch that advances mid-release. Rehearsals from this repository proved
four things: the release path, the hotfix path, a deliberate conflict stopping the run with nothing
pushed, and a missing branch failing before `release:prepare`. `DEVELOP` was read and kept at
`0.4.0-SNAPSHOT` even with `next_development_version` set to `0.9.9-SNAPSHOT`.

**`#117`** (above) is its consumer half, and could not be folded into it, because the two changes
live in different repositories. `#65` names the branch through a new `development_branch` input,
defaulting to `DEVELOPMENT`, and DSH's wrappers must pass `DEVELOP`. The order is forced: a caller
passing an input the called workflow does not declare is a hard error, so `#117` could only merge
after `#65`. Between the two merges, both of this repository's `release.yml` and `hotfix.yml`
failed at the new preflight in their first minute, before anything was written. **`#117` is closed**
(PR #118, merged 2026-09-25): a dry-run release from this repository merged `v0.3.0` back into
`DEVELOP` and carried 94 paths with none lost. `hotfix.yml` is verified by inspection only, until a
`0.3.x` branch exists to rehearse against.

[`parent-poms#81`](https://github.com/MRISS-Projects/parent-poms/issues/81) was spun off from
`#69`'s review round and deliberately not folded into it. Reviewing the fix surfaced that
`project-release.yml` interpolates `workflow_dispatch` inputs straight into `run:` bodies, where a
`${{ }}` expression is substituted into the script text before the shell parses it. `#69` fixed the
four uses in the two steps it owned; **twelve further steps** share the pattern, and `deploy.yml`,
`project-hotfix.yml` and the staging workflows are unaudited. Folding that in would have turned a
version-setting fix into a workflow-wide security pass, which is the wrong shape for one PR to
carry and the wrong thing to hold `0.3.0` behind.

It sits on `3.10.0-SNAPSHOT` rather than `3.9.0-SNAPSHOT`, so it does **not** gate this wave.
Reaching the input requires write access to the consuming repository, so the exposure is defence in
depth rather than an open door. The likelier practical failure is the quiet one: a value containing
a `#` truncates the command, and the step goes green having done nothing — the same shape of silent
success `#69` itself was about.

`parent-poms#72` was raised from planning the order of this milestone, and it exists because two of
its own issues could not otherwise be validated. `#65` requires validation "against a real
repository (e.g. `dsh`) with at least one RC-branch-only fix", and `#69` is unobserved analysis that
wants "a release dry run" to confirm it. Both need a real consuming release through
`project-release.yml`. DSH's next release is `0.3.0`, which this wave gates, and this wave does not
finish until **3.9.0** is released — so each was blocked on the release it is supposed to precede.
`#72` breaks that by making the release workflows rehearsable. Its DSH twin `#111` passes the input
through this repository's `release.yml` and `hotfix.yml` wrappers; like `#85`/`#57` and `#86`/`#58`,
the pair needs no release to reach here, because the wrappers reference parent-poms at `@master` and
neither change touches a POM. How the two issues are validated in the meantime is recorded in §6.

**`#111` is closed** (PR #116, merged 2026-09-23), built under `#114`'s branch rather than its own:
the two stories edit the same two wrapper files for different inputs, and `#114` could not prove
itself without `#111`, because `project-release.yml` is `workflow_call` only and the rehearsal was
unreachable from here until a wrapper could request it. One dispatched rehearsal closed both.

One finding from scoping `#72` is worth recording here, because it narrows what the rehearsal can
prove. Six of `project-release.yml`'s eight write points act on refs that `release:prepare` creates,
so suppressing every write does not skip them — it makes them unreachable, `#69`'s `versions:set`
among them. A rehearsal that proves anything about `#65` or `#69` therefore has to write real refs
somewhere harmless rather than write nothing, and `#72` carried that as the design question it had
to settle.

**It settled it, and `#72` is closed** (PR `#77`, merged 2026-09-22). The answer was a
rehearsal-only bridge that rebuilds the release tag locally from the `pom.xml.tag` tree a dry-run
`release:prepare` already writes to disk, so every later step stays reachable while nothing reaches
a remote. `project-hotfix.yml`'s rehearsal was green end to end immediately; `project-release.yml`'s
stopped at `#69` until that was fixed, and **now runs green to the end too** — all eight declared
write points fire, the last four of them for the first time in any `project-release.yml` run. Every
run proved against the live remote that it wrote nothing.

Building it spun off two issues, a twin pair rather than one, and neither was folded into `#72`:

- **`#114`** (this repository, above) and its twin
  [`parent-poms#76`](https://github.com/MRISS-Projects/parent-poms/issues/76). The first rehearsal
  died at `release:prepare` because neither release workflow defines the `mongo.*` properties
  `ci.yml` supplies, so `mongo.properties` filters to a literal `${mongo.port}` and every
  `dsh-rest-api` Spring context fails. That was a second, independent reason DSH `0.3.0` could not
  be released. It is a pair rather than one issue because the fix has two halves that belong in
  different repositories: parent-poms needs a *generic* way for any consumer to supply build
  properties — naming `mongo` in shared infrastructure is the shape to stop repeating — and DSH
  needs to decide whether its own POM should carry defaults at all.

  **Both are closed** — `parent-poms#76` on 2026-09-23 (PR `parent-poms#79`) and `#114` the same day
  (PR #116), as a full round trip. The open question resolved to *no POM defaults*: precedence was
  measured on Maven 3.9.9 as command line > active settings profile > POM `<properties>`, so a
  default was safe but would never have been the winning value in any real build, since every path
  already supplies the four names. `#76` therefore stayed on the critical path — the release
  wrappers have no other way to carry a build property. The pair was proved by two dispatched runs
  from this repository: a rehearsal where `release:prepare`'s fork completes and writes nothing, and
  a staging run where the build opens a real connection to the MongoDB service container.

  Two issues came out of building it, neither folded into its parent:

  - [`parent-poms#78`](https://github.com/MRISS-Projects/parent-poms/issues/78) — the rest of
    `project-staging.yml`'s consumer-specific content: the `mongo:6` and `rabbitmq` service
    containers, `mongo_database`, and the step that creates the Mongo user. A `name=value` input
    cannot reach them, because a reusable workflow owns its own job and a consumer cannot declare a
    service into it. That needs a design, and folding it into `#76` would have turned a one-input
    change into an open-ended redesign.

    **It is closed, and the design question dissolved.** It was fixed by `parent-poms#83`, merged
    2026-09-26. Before designing a replacement, its spec measured whether DSH needed the services
    at all, and DSH does not. The full reactor with `-DintegrationTests` is green with nothing
    listening on 27017 or 5672. The `dsh-rest-api` ITs mock the DAO and queue service, and the
    worker contexts never connect. So the services, the user-creation step and all three `mongo_*`
    inputs were deleted with no extension point in their place. A product whose integration tests
    need live infrastructure starts it from those tests. A `build.yml` guard now fails any reusable
    workflow that declares `services:`. The "real connection to the MongoDB service container" that
    `#114`'s staging run observed, described above, was the Mongo driver's background monitor
    thread, not a test dependency. `#123` is the consuming half, and `staging.yml` now passes only
    `maven_properties`.
  - **`#115`** (above) — `version.properties` ships `${jenkins.build.number}` unresolved in
    `dsh-data` and `dsh-rest-api`. Found by `#114`'s placeholder sweep, which is the only reason
    anyone looked: nothing defines that property, no Java reads the file, and there is no Jenkins.
    It predates `#114` and has nothing to do with supplying `mongo.*`, so `#114`'s AC003 was
    narrowed to `mongo.*` and `#115` carries the general form. Shipped in PR #135 by deleting
    both files rather than resolving the line: git history shows nothing has read them since
    2017. `#114`'s AC003 stays narrowed, since it is ticked on a closed issue; `#115`'s AC001 now
    carries the general placeholder check.

At the end of Wave 0 the root `pom.xml` should inherit from a **released `3.9.0`**, not a SNAPSHOT.
That also retires the accepted risk in §6 — see there for why the SNAPSHOT pin stands until then.
The round trip for making a change in parent-poms and re-pinning here is documented in
`docs/process/ai-driven-development.md`, "Working across the parent-poms boundary".

Note the overlap: parent-poms `#57`/`#58`/`#59` carry the same titles as DSH `#85`/`#86`/`#87`.
Both repositories have workflows that invoke Maven and docs that cite versions, so the work is
genuinely parallel rather than duplicated — each issue is scoped to its own repository and
cross-links its twin.

**The second pair is done, and was built as one cycle.** DSH `#86` and parent-poms `#58` both
closed on 2026-09-18 — the first time a pair was delivered together rather than one repository at a
time. It was cheap because the change needed no release: DSH's four release wrappers reference
parent-poms' reusable workflows at `@master`, and `#58` touched no POM, so it reached DSH the
moment it merged. That is the pattern to reuse for `#85`/`#57`, which are the same shape.

**The first pair is done too, and the pattern held.** DSH `#85` and parent-poms `#57` both closed
on 2026-09-19, delivered as one cycle for the same reason: `#57` touched no POM, so it reached DSH
without a release. `#57` needed correcting before it could be built — its "Files to Update" list
named `.md` files, while the rot was in `infrastructure/src/site/apt/{maven,java}.apt`, so read
literally the issue was already done. Those two pages were converted to Markdown rather than edited
as APT, which also made the issue's own file list true. Closing `#57` clears everything from
`3.8.0-SNAPSHOT` except `#13`, which is now the single item standing between here and releasing
**3.8.0**.

**Two parent-poms issues were spun off from `#85`, both onto `3.9.0-SNAPSHOT`.**

- `parent-poms#70` — convert the remaining APT site pages to Markdown. `#85`'s two pages were
  the pilot, and this finishes the format migration. It was not folded into `#57`
  because a 15-file migration on `3.8.0-SNAPSHOT` would push the 3.8.0 release further out, which
  is the opposite of why `#85` was picked up first. The 39 APT files under
  `src/main/resources/archetype-resources/` are deliberately excluded — they are template content
  shipped into new projects, so converting them is a separate decision. **Closed** 2026-09-27 by
  parent-poms PR #87, clearing `3.9.0-SNAPSHOT`. Its completion criterion, dropping
  `doxia-module-apt` from `maven-site-plugin`, turned out unable to fail: the plugin already
  bundles that module at the same version. The dependency went anyway, as a no-op, and the real
  check became "no `.apt` left under any `src/site/` outside `archetype-resources/`". So a
  consumer's APT pages still render.

  The conversion kept every page's content unchanged on purpose, and it spun off
  [`parent-poms#85`](https://github.com/MRISS-Projects/parent-poms/issues/85) — not DSH `#85` — for
  the content question: five images never committed, empty link targets, and pages about
  Subversion, Tomcat 8 and the retired APT format, for both consuming projects and parent-poms
  contributors. It had **no milestone** then, so it did not gate 3.9.0. It went on
  parent-poms `3.13.0-SNAPSHOT` on 2026-10-02, and since 2026-10-10 it is on `3.15.0-SNAPSHOT`,
  Wave 11.
- `parent-poms#71` — `commit-readme-md` runs three times per staging run, and one of those commits
  carries an unresolved `${timestamp}` into the published `README.md`. A later execution repairs
  it, so a *successful* run ends correct; a run that fails inside that window leaves the consuming
  repository holding a broken README, and nothing reports it. Reproduced four times now, across
  three branches and both repositories — the two original DSH branches, then on 2026-09-19 in
  parent-poms' own `3.9.0-SNAPSHOT` deploy and in a DSH staging run on
  `staging-0.3.0-SNAPSHOT-RC`. All four show the same shape: three README commits, the middle one
  corrupt, the final state clean. It belongs upstream rather than here
  because it is a defect in the inherited `readme-generation` profile, and it was not folded into
  `#70` because the two share nothing but the repository. **Closed** — parent-poms PR #73 moved the
  commit out of the Maven lifecycle into the `.github/actions/commit-readme` composite action, so a
  replayed `process-resources` can regenerate the file but never commit it. It no longer counts
  against clearing `3.9.0-SNAPSHOT`.

**`parent-poms#86` was found while `#70` was in review, and it sits on `3.10.0-SNAPSHOT`.** Every
generated `README.md`'s release notes silently dropped closed issues that had no label: 3 of
`3.9.0-SNAPSHOT`'s 8, for example. The cause is in the forked `maven-changes-plugin`, whose
`github-text-list` takes an issue's type from its first label and skips an issue with none. The fix
belongs in the fork, as
[`MRISS-Projects/maven-changes-plugin#36`](https://github.com/MRISS-Projects/maven-changes-plugin/issues/36)
on its milestone `2.12.10`, released on 2026-09-30. `parent-poms#86` is the consuming half: bump
`changes.plugin.version` to the released `2.12.10` and verify a README generated with it. It is not
DSH `#86`, below. In the meantime the six affected issues were labelled, DSH `#114` among them, so
the current READMEs correct themselves on their next generation. That is why `parent-poms#86` does
not gate 3.9.0.

**`parent-poms#89` was spun off from `#70`, and on 2026-09-28 it joined `3.10.0-SNAPSHOT`** (Wave
1's upstream block). Scoping `#70` found a second
dead navigation link: the "Products" entry in the site's parent menu resolves to
`dsh/rcs/products/index.html`, which 404s. The link is computed from `<url>` values defined in
parent-poms, so the fix belongs there rather than in an override in DSH's `site.xml`. It was not
folded into `#70`, because it lives in another repository and is subject to the rule that no
parent-poms work is picked up before DSH 0.3.0 ships.

**DSH `#87` is back in this wave, so the third pair moves in step again.** It left for Wave 1 on
2026-09-17 because `#86` and `#87` contradicted each other inside one wave: `#86` pinned 3.9.9 in
the workflows and `#87` replaced that same pin with 3.9.16, so building both here meant writing a
version and immediately rewriting it. That reason expired when `#86` shipped in PR #108. It returned
on 2026-09-25 because the deferral had split the pair across a release boundary: parent-poms `#59`
still gates **3.9.0**, and DSH's release wrappers call parent-poms' reusable workflows at `@master`.
Shipping `#59` alone would make DSH's releases build on 3.9.16 while `ci.yml` and `api-testing.yml`
stayed on 3.9.9 until `0.4.0-SNAPSHOT` — the same CI-versus-release drift `#99` was built to remove.
Moving `#59` to `3.10.0-SNAPSHOT` instead was considered and rejected; the pair is delivered as one
cycle, like `#85`/`#57` and `#86`/`#58`, and for the same reason it needs no release to reach here.

**The third pair is done, and the pattern held a third time.** DSH `#87` and parent-poms `#59` both
closed on 2026-09-26, delivered as one cycle: parent-poms PR #84 merged first, then DSH PR #126,
because DSH's staging and release evidence could only run once `#59` was on `master`. `#59` went one
step beyond its issue: the four `project-*.yml` reusable workflows pinned Maven but never asserted
it, so they gained the same `Verify Maven version` guard as `build.yml`. A staging or release run
now proves which Maven it ran. With `#59` closed, `#70` was the only issue left gating **3.9.0**,
and it closed on 2026-09-27.
Parent-poms `#13` (image links broken in the generated Maven site) is **closed**,
fixed and released in 3.8.0. It was adjacent to DSH `#70` and `#90`, and that adjacency has now
been tested for one of the two: a staging run on `staging-0.3.0-SNAPSHOT-RC` against the released
`3.8.0` shows `#90` reproducing unchanged — root and every module still missing `index.html`, the
live URL still 404 — so **`#90` is not a symptom of `#13`** and stands on its own diagnosis.
That diagnosis held: `#90` closed on 2026-09-27 with PR #129. The redirect that parent-poms `#55`
added to its `deployment` profile sends `generatedSiteDirectory` away from `target/generated-site`,
where DSH's filtered markdown lives, and DSH's root `pom.xml` now points it back.
`#70` turned out to be unrelated to both: the dead link was DSH's own top-bar "Project" item,
pointing at a retired classic GitHub Projects board. It closed on 2026-09-27 with PR #130, which
points it at the milestones page instead.

The root `pom.xml`'s SNAPSHOT parent pin was a related item, deliberately *not* actionable until
the above completed. It has: the pin is the released `3.9.0` since 2026-09-27 — see §6.

### The 2026-09-28 re-plan

Waves 1 onward were rewritten on 2026-09-28, in a brainstorm held before closing 0.3.0. It covered
three things: documentation, delivery to an end user, and how Spring Integration carries the
pipeline. One finding reshaped all three, and it is recorded here because every wave below rests on
it.

**DSH does not process documents yet.** `dsh-rest-api` accepts a document, stores it and enqueues
its id, and nothing consumes the queue. `dsh-doc-indexer-worker`, `dsh-doc-processor-worker`,
`dsh-keyword-extractor` and `dsh-top-sentences-extractor` each hold a 17-line `Application` class
and nothing else. No keyword or sentence extraction code exists, here or anywhere else; it is
written from scratch against the two papers the wiki cites. `DocumentStatus` models all 18 states of
the pipeline, and only the first few are reachable. ADR-001's Context table said the workers
"dequeue and process", and it treated `dequeue-docId-context.xml`, an unused test resource, as
production configuration. ADR-003 revised that table on 2026-10-13. Until that day this
paragraph said the file does not exist. It exists, under `dsh-rest-api`'s test resources.

The owner's decisions from that session:

1. **The pipeline is built on the new interfaces.** Nothing new is built on RabbitMQ or Solr, and
   neither is refactored again. ADR-001's plan to wrap and deprecate them assumed consumers that
   were never written.
2. **The pipeline's flow is Spring Integration's Java DSL.** One flow definition is the bus, and the
   stage beans are plain Java classes with no messaging code. There is no XML for the main flow.
3. **Maven Central is ruled out.** Anyone building DSH without a GitHub token needs the MRISS
   artifacts from GitHub Packages shipped to them, the `maven-changes-plugin` fork included.
4. **Algorithms before GCP.** The pipeline produces results on the infrastructure that already
   exists, and GCP then becomes a swap behind interfaces that already exist.

So waves 1-7 no longer mirror ADR-001's phases one for one. Where a task is still drawn from an
ADR-001 phase table, it says so.

### The 2026-10-10 re-plan

Waves 4 onward were re-planned on 2026-10-10, after `#48` closed. Its findings,
`specs/architecture/profiles-and-configuration.md`, plan three operated tiers in one GCP project:
`dsh-dev`, `dsh-staging` and `dsh`. Until then no wave built any of them, and the only deploy in
the plan was the end user's, in the distribution wave. Where a wave below cites "the findings", it
means that document.

The owner's decisions from that session:

1. **A new Wave 4 builds the foundation and the development tier.** The findings (§9.1) say that
   the first `gcp` code needs somewhere real to run. So the image and a minimal deploy move forward
   from the distribution wave, which keeps the end-user packaging, and Firestore starts on a tier
   that already exists. Two alternatives were rejected. Folding the work into the Firestore wave
   mixed deploy plumbing with persistence. Building all three tiers at once put the staging and
   production deploys several waves ahead of any release candidate that could exercise them.
2. **A `0.6.0` milestone carries the GCP swap, and a new Wave 7 closes it.** With everything on
   `1.0.0-SNAPSHOT`, the first `dsh-staging` deploy, the first `dsh` deploy, the commit gate and
   the smoke check would all have run for the first time on the 1.0.0 release. 0.3.0 and 0.3.1
   showed what the first run of a release path costs (Wave 1, upstream step 0). So Waves 4 to 7 are
   on `0.6.0-SNAPSHOT`, and 0.6.0's release candidate and release are the first real run. Pub/Sub
   is inside 0.6.0 on purpose: with zero minimum instances, in-memory channels are not sound on
   Cloud Run (findings §8.4), and the first production deployment should have a pipeline that
   finishes.

The waves were renumbered:

| Wave | Before 2026-10-10 | Theme | Milestone |
|---|---|---|---|
| 0 to 3 | 0 to 3 | unchanged | unchanged |
| 4 | new | GCP foundation and the development tier | `0.6.0-SNAPSHOT` |
| 5 | 4 | GCP: Firestore and GCS | `0.6.0-SNAPSHOT`, was `1.0.0-SNAPSHOT` |
| 6 | 5 | GCP: Pub/Sub | `0.6.0-SNAPSHOT`, was `1.0.0-SNAPSHOT` |
| 7 | new | The release path on GCP | `0.6.0-SNAPSHOT` |
| 8 | 6 | GCP: term statistics and search | `1.0.0-SNAPSHOT` |
| 9 | 7 | Validation and cutover | `1.0.0-SNAPSHOT` |
| 10 | 8 | Distribution | `1.0.0-SNAPSHOT` |
| 11 | 9 | Product backlog | `1.0.0-SNAPSHOT` |

The session also found three things the findings do not list:

- **`#45`, the actuators, sat in the product backlog.** The smoke check reads health, the version
  and the commit, so it moves to Wave 4.
- **`#49`, the emulators, sat with the Pub/Sub work.** The Firestore code needs the Firestore
  emulator for its layer 3 tests, so `#49` moves to Wave 5, narrowed to that emulator.
- **`#148`, the release skill, is built before the 0.4.0 release candidate, when no tier exists.**
  The findings give it the commit gate and the production smoke check. Those cannot be part of it
  then, and are Wave 7 task 3.

**Documents that keep old wave numbers.** These are dated records and are not corrected. The
findings, `#48`'s spec and `specs/stories/143-readme-describes-dsh-today.md` keep the numbers of
before 2026-10-10: read them against the table above.
`specs/stories/124-keep-test-fixtures-out-of-production-artifacts.md` and
`scripts/close-wontfix-issues.sh` keep the numbers of 2026-09-16, from before both re-plans.

### The 2026-10-13 re-plan

Waves 1 onward were re-planned on 2026-10-13, after `#160` closed.
[ADR-003](../architecture/ADR-003-document-pipeline.md) decides how the document pipeline is
built, and its "Consequences for the waves" table lists what each wave owes it. No wave was added
or renumbered, and no milestone moved. Wave 8 was left empty and is retired.

What ADR-003 decides, as far as the waves show it:

- **The legacy code is removed, not migrated.** MongoDB goes in Wave 1 task 8, RabbitMQ in Wave 2
  task 1 and Solr in Wave 2 task 3. No `legacy`, `mongodb` or `rabbitmq` profile ever exists.
- **A run with no profile needs nothing installed.** Documents are kept in memory, files in a local
  directory, and the hops between stages are in the JVM.
- **Each service outside the JVM is selected by a property**: `dsh.persistence`, `dsh.transport`
  and `dsh.file-store`. The `gcp` profile sets the three together, and the GCP implementations
  live in a module of their own, `dsh-gcp`.
- **No search engine has a role.** Corpus term statistics are stored with the documents.

The owner's decisions from that session:

1. **Wave 1 tasks 8 and 9 are cut differently.** ADR-003 found that they did not hold as it had
   listed them: task 8 added the conditional status advance to `DocumentDao`, so
   `MongoDocumentDao` would have had to implement it, only for task 9 to delete it. Task 8 is now
   the swap, the in-memory `DocumentDao` in and MongoDB out, with the interface as it stands. Task
   9 is the new behaviour: the advance and the store's contract. No MongoDB code is written. Two
   alternatives were rejected. One story with all of it put a wide removal and a concurrency
   contract in one review. Three stories needed MongoDB made selectable beside the in-memory
   store, for the length of one story.
2. **Task 8 goes first in Wave 1's order of work, and tasks 9 and 10 stay after `#120`.** Five
   items are lighter once MongoDB is gone: `#157`, `#158`, `#119`, `#120` and `#137`. The
   cost is accepted: the in-memory DAO is written before the Spring Boot upgrade and before
   `#132`'s analysers. Wave 2 task 3 goes ahead of `#119` and `#120` for the same reason, so
   that neither carries Solr.
3. **`#157` is superseded by task 8, and `#140` is reduced** to the part `#48` added to it,
   the test profile's properties file. The owner closed `#157` the same day.
4. **Every wave keeps its number, and Wave 8 is retired.** ADR-003 was accepted citing Wave 9 and
   Wave 11, and ADR-001's phases name their waves. Renumbering would have made both wrong on the
   day of the acceptance.
5. **The code is the baseline for the public API.** The OpenAPI document and four feature specs
   describe routes and statuses the code does not have. Wave 1 task 11 makes them describe what
   exists, and from then on an API change starts in the OpenAPI document. Moving the code to the
   OpenAPI document's routes was rejected: the walking skeleton would also have carried a
   breaking redesign of the API.
6. **`NOT_INDEXED_ERROR`'s wrong description gets an issue of its own**, Wave 1 task 12, so that
   the fix is in 0.4.0's release notes. That issue is `#171`.
7. **Wave 2 task 1 is three tasks.** ADR-003 gave it the rules for repeats and failures on top of
   the flow. They are task 4 now, and the generated pipeline diagram is task 5.

**Existing tasks keep their numbers.** A new task takes the next free number in its wave, and the
order is stated in prose. Where ADR-003 says "Wave 1 task 8" or "Wave 1 task 9", it means the cut
from before this re-plan, and the first rows of the table below translate.

**The mapping.** ADR-003 ("After the merge", step 3) asks the re-plan for one line for each row of
its consequences table, naming the task that now carries it.

| Row of ADR-003's consequences table | Carried by |
|---|---|
| Wave 1 task 3, ADR-004 | Wave 1 task 3 |
| Wave 1 tasks 5 and 6, the wiki | Wave 1 tasks 5 and 6 |
| Wave 1 task 8: the conditional advance, the in-memory default, the contract and its test | Split. The in-memory default is Wave 1 task 8. The advance, the contract and its test are Wave 1 task 9 |
| Wave 1 task 9: remove MongoDB | Wave 1 task 8 |
| Wave 1 tasks 8 and 9, their order | Settled by decisions 1 and 2. Steps 3 and 9 of Wave 1's order of work |
| Wave 1 task 10 | Wave 1 task 10 |
| Wave 2 task 1 | Wave 2 task 1, and Wave 2 task 4 for the two rules a repeat follows and the two kinds of failure |
| Wave 2 task 3 | Wave 2 task 3 |
| Wave 3 task 1 | Wave 3 task 1 |
| Wave 3 task 2 | Wave 3 task 2 |
| Wave 3, every stage | The paragraph above Wave 3's tasks, and the limits in its tasks 4 and 6 |
| Wave 5 | Wave 5 tasks 1, 2, 3 and 7 |
| Wave 6 | Wave 6 tasks 1, 2 and 4 |
| Wave 8, empty | Wave 8, retired |
| Wave 9 task 2, nothing left to remove | Dropped. The number is kept |
| Wave 11, a backlog line | The line under Wave 11's triaged issues |
| `specs/architecture/system-design.md` and the `dsh-plan-wave` skill | No task. Both were corrected as the re-plan's first act, in commit `fc2017253` |
| `specs/features/indexing-workflow.md` | Wave 1 task 11 |

ADR-003's eight follow-up candidates were settled in the same session:

1. `#157`: superseded (decision 3).
2. `#140`: reduced (decision 3).
3. `#158`, `#137` and `#120`: still valid, each with less to cover. Wave 1's triaged issues say
   what.
4. The PRD re-plan: this one.
5. `NOT_INDEXED_ERROR`: Wave 1 task 12 (decision 6). `#171` — **closed** — PR #172.
6. The sentence on `dequeue-docId-context.xml`: corrected, in "The 2026-09-28 re-plan" above.
7. `CLAUDE.md` and `system-design.md` describing MongoDB, RabbitMQ and Solr as current: each is
   corrected by the story that removes the code. Those are Wave 1 task 8, Wave 2 task 1 and Wave 2
   task 3. `CLAUDE.md`'s paragraph on what was stale until this re-plan was removed with it.
8. The OpenAPI document and the feature specs: Wave 1 task 11 (decision 5).

### Wave 1 — Pipeline design, documentation and upstream catch-up

Milestone: `0.4.0-SNAPSHOT`.

**Upstream catch-up comes first.** Nothing upstream was to start before 0.3.0 was released and work
was back on `DEVELOP` (Wave 0). Both held on 2026-09-29, so this block goes before anything else in
the wave, in order:

0. **Done on 2026-09-30, shipped as DSH 0.3.2. Hotfix DSH 0.3.1, to repair 0.3.0's release site.**
   The plan as written is kept below. Its outcome follows it. `parent-poms#95` and `#96` came out of DSH
   0.3.0's release on 2026-09-29. The release site's test and coverage reports are empty (`#95`)
   and it has no coverage badge (`#96`), so the badge on `master`'s README is broken. Both have one
   cause: `project-release.yml` deletes `target/checkout`, where `release:perform` ran the tests,
   before it builds the site. Both belong to parent-poms, not DSH, because staging publishes both
   correctly. They are fixes, so they go on a parent-poms hotfix line (§6) rather than wait for
   3.10.0:

   1. Open parent-poms milestone `3.9.2-SNAPSHOT`. `3.9.1` exists already, closed without a
      release.
   2. Move `parent-poms#95` and `#96` from `3.10.0-SNAPSHOT` to `3.9.2-SNAPSHOT`.
   3. Fix both, snapshot-deploy `3.9.2-SNAPSHOT`, and pin DSH's root `pom.xml` **on `0.3.x`** to
      it. The DSH change is a task branch cut from `0.3.x` and merged back into it. Its issue is
      `#146`, on DSH milestone `0.3.1-SNAPSHOT`, and it carries sub-steps 3 to 6 on the DSH side.
   4. Dispatch `hotfix.yml` with `branch_name=0.3.x` and `dry_run=true`. It must show the test and
      coverage reports and the badge generated where the site is built.
   5. Rename the milestone to `3.9.2`, release it, and re-pin `0.3.x` to the released `3.9.2`:
      maven-release-plugin refuses a SNAPSHOT parent.
   6. Dispatch `hotfix.yml` for real. It releases **DSH 0.3.1**, whose site carries the reports and
      the badge, and that fixes `master`'s README. This replaces the one-off republish of the 0.3.0
      site from `v0.3.0` that `#95` originally asked for, and `#96` AC003 with it; both issues'
      criteria are corrected to match before they are built.

   **Outcome.** `#146` is closed. Its spec, `specs/stories/146-release-0-3-1-with-site-reports.md`,
   holds the evidence.
   - **The pin was needed.** The fix, parent-poms PR #97, touches `products/pom.xml`. It makes
     `release:prepare`'s forked build a release's single test run, integration tests included, and
     builds the site from that tree. A workflow-only fix would not have needed the pin.
   - **Proven before merging.** parent-poms 3.9.2 was proven by DSH rehearsals of both workflows
     against the PR branch before it merged, and was released on 2026-09-29.
   - **0.3.1 was abandoned.** The first real dispatch failed before any write. The second left a
     partial 0.3.1: tag `v0.3.1`, 12 of 13 artifacts, and `0.3.x` moved on. That was caused by a
     transient HTTP 500 from GitHub Packages in `release:perform`.
   - **Shipped as 0.3.2.** Rather than delete the tag and force-push `0.3.x`, the line released
     **0.3.2** on 2026-09-30. Its site carries the reports (127 tests, 98% aggregate coverage) and the
     badge, and `master`'s README shows it.
   - **The merge-back** left `DEVELOP` at `0.4.0-SNAPSHOT` naming parent `3.9.2`, as expected. Step
     5's re-pin to `3.10.0` supersedes that.

   `parent-poms#98`, the missing retry on a transient registry error during `release:perform`, came
   out of that second dispatch. It is not folded into `#95`: it is a separate failure mode of the
   same step. It was deferred then, with no milestone. Since 2026-10-02 it is step 6 below, on
   parent-poms `3.11.0-SNAPSHOT`, and it was closed on 2026-10-04.
1. **Done on 2026-09-30: maven-changes-plugin 2.12.10 released.**
   [`maven-changes-plugin#36`](https://github.com/MRISS-Projects/maven-changes-plugin/issues/36),
   [`#37`](https://github.com/MRISS-Projects/maven-changes-plugin/issues/37) and
   [`#38`](https://github.com/MRISS-Projects/maven-changes-plugin/issues/38) are **closed**,
   merged by the fork's PRs #39, #40 and #41. The fork's milestone `2.12.10` is closed too. The
   release followed the last section of the fork's README: its PR #43, tag `maven-changes-plugin-2.12.10`,
   and `DEVELOP` moved on to `2.12.11-SNAPSHOT`. Each issue's spec is in the fork's `specs/`.
   `#38` renders a closed milestone that has no issues, such as DSH's `0.3.1`. It is opt-in, through
   `includeEmptyMilestones`, so parent-poms turned it on in step 2.

   [`maven-changes-plugin#42`](https://github.com/MRISS-Projects/maven-changes-plugin/issues/42)
   came out of `#38`: `github-text-list` with the APT formatter throws an NPE on every GitHub issue.
   It was not folded into `#38`, because it is an older, separate defect. No MRISS project uses
   the APT formatter, so it has no milestone and no wave.
2. **Done on 2026-10-02: parent-poms `3.10.0-SNAPSHOT` is built, and its milestone is clear.** It
   held six issues, all closed. `#89` had joined it on 2026-09-28, because its 404 is on DSH's own
   site. `#95` and `#96` left it for step 0 on 2026-09-29. `#104` joined on 2026-10-02. The release
   itself is step 4.
   - `#86` — **closed**, parent-poms PR #99. The changes plugin is at 2.12.10, and
     `includeEmptyMilestones` is on in both `github-text-list` executions.
   - `#81` — **closed**, parent-poms PR #100. No `run:` body interpolates an expression any more,
     and a guard in parent-poms' `build.yml` keeps it so.
   - `#88` — **closed**, parent-poms PR #101. Site publication is three steps: stage, verify,
     publish. One `gh-pages` commit per run replaces one per module.
   - `#74` — **closed**, parent-poms PR #103. The parent-poms site has a real landing page.
   - `#89` — **closed**, parent-poms PR #102. A redirect page at `<type>/products/index.html` in
     the consumer's own site, because the link cannot be changed from parent-poms. It stayed open
     until step 3 showed the live link working.
   - `#104` — **closed**, parent-poms PR #105. `project-staging.yml` takes a `release_type` input,
     so it is also a consumer's snapshot deploy.

     `#104` came out of DSH `#127`: the snapshot deploy needed everything `project-staging.yml`
     does, but for two literals that tied it to a release candidate. It was not folded into `#127`,
     because the change is to parent-poms' reusable workflow, not to DSH.

   One thing stays unproven. `project-staging.yml` has no dry run, so its first run in `rcs` mode
   since `#88` and `#104` is DSH's 0.4.0 RC. Its `snapshots` mode ran for real in step 3.
3. **Done on 2026-10-02: DSH `#127`, against a temporary `3.10.0-SNAPSHOT` pin, before 3.10.0 is
   released.** `#127` is **closed**, PR #149. Its spec,
   `specs/stories/127-deploy-snapshot-site.md`, holds the evidence. It moved here from the triaged
   issues on 2026-10-02, because it is a real publish from a consumer that needs no RC.
   - **The snapshot site is live** at `snapshots/products/dsh/`, deployed by the `Deploy Snapshot`
     workflow (`deploy.yml`), a wrapper over `project-staging.yml`. The 2020 site at
     `snapshots/dsh/` is removed.
   - **It proved three parent-poms fixes from a consumer**, in the proof run on the task branch and
     again from `DEVELOP`: `#89`'s "Products" link through the redirect page, `#88`'s stage, verify
     and publish steps with a real push, and `#86`'s README, which shows `0.3.1` with a `No issues`
     row.
   - **A placeholder `deploy.yml` sits on `master`**, by a one-off exception the owner approved.
     GitHub dispatches a workflow only if its file exists on the default branch, and `master`
     receives code only at a release. The placeholder refuses to run there, and the 0.4.0 release
     replaces it.
   - **`DEVELOP` was on a `-SNAPSHOT` parent** from this merge until step 5 re-pinned it to the
     released `3.10.0` on 2026-10-03.
4. **Done on 2026-10-03: parent-poms 3.10.0 released**, by the full round trip in `CLAUDE.md`, after
   `#89` was closed and the milestone cleared.
   [Run 37080121832](https://github.com/MRISS-Projects/parent-poms/actions/runs/37080121832) tagged
   `mriss-parent-3.10.0`, published `mriss-parent` and `products` at `3.10.0`, and moved parent-poms
   `master` to `3.11.0-SNAPSHOT`. The milestone was renamed `3.10.0` first, so the release notes say
   `### Version 3.10.0`.
   - **`#86`'s last check passed:** the released README and site report both list `3.10.0` first,
     with all six issues, then `3.9.2` downward.
5. **Done on 2026-10-03: DSH re-pinned to the released `3.10.0`.** `#150` — **closed**, PR #151. Its
   spec, `specs/stories/150-repin-parent-3-10-0.md`, holds the evidence.
   - **`-U` is dropped from `ci.yml` and `api-testing.yml`.** The docs say to add it back for the
     next `SNAPSHOT` pin.
   - **The issue-labelling rule is retired**, as both copies named. `CLAUDE.md` lost it, and
     `dsh-new-story` keeps a plain labelling instruction. An unlabelled issue is listed with Type
     `n/a`, shown on a README generated against the released parent.
6. **Done on 2026-10-04: parent-poms `3.11.0-SNAPSHOT`'s two issues are closed**, before DSH's
   0.4.0 RC and release. Both harden a workflow that the 0.4.0 pipeline runs:
   - [`parent-poms#98`](https://github.com/MRISS-Projects/parent-poms/issues/98) — `release:perform`
     fails the whole release on a transient GitHub Packages 500, with no retry. **closed** —
     parent-poms PR #108, merged on 2026-10-04 with no POM change. It is the failure that left 0.3.1
     half released (step 0). The release and hotfix workflows now retry an upload that GitHub
     Packages answers with 429 or a transient 5xx. DSH dry runs of both workflows passed after the
     merge: hotfix
     [run 37201685895](https://github.com/MRISS-Projects/dsh/actions/runs/37201685895) and release
     [run 37201688263](https://github.com/MRISS-Projects/dsh/actions/runs/37201688263). It goes
     with DSH `#148`, the release skill, which this wave already wants before 0.4.0.
   - [`parent-poms#106`](https://github.com/MRISS-Projects/parent-poms/issues/106) —
     `project-staging.yml` has no concurrency group, unlike `project-release.yml` and
     `project-hotfix.yml`. **closed** — parent-poms PR #107, merged on 2026-10-03 with no POM
     change. Staging, release and hotfix now share one group per consumer, `dsh-site` for DSH. Its
     first release-path exercise was `#98`'s two dry runs on 2026-10-04. The release job waited in
     the group for the whole hotfix job and started two seconds after it ended.

     `#106` came out of the review of DSH PR #149. It was not folded into `#127`, because the gap is
     in parent-poms' workflow. DSH's own `deploy-snapshot` group in `deploy.yml` is now redundant,
     but harmless.

   A workflow change reaches DSH on merge, because its wrappers call `@master`, and neither issue
   changed a POM. parent-poms 3.11.0 was released on 2026-10-04 anyway, to record them, by
   [run 37210362198](https://github.com/MRISS-Projects/parent-poms/actions/runs/37210362198). It
   tagged `mriss-parent-3.11.0`, its release notes list both issues, and it moved parent-poms
   `master` to `3.12.0-SNAPSHOT`. The milestone was renamed `3.11.0` first and closed after it.
   **DSH stays pinned to
   `3.10.0`**, by decision: `products:3.11.0` holds the same POM, so a re-pin would change nothing.
7. **Done on 2026-10-04: maven-repo holds no artifacts in git.**
   [`maven-repo#9`](https://github.com/MRISS-Projects/maven-repo/issues/9) — remove the Maven artifacts
   committed to master. **closed** — maven-repo PR #10. Its description holds the evidence.
   - **It did not lead back into parent-poms.** The last committer was a `maven-scm-publish-plugin`
     execution in parent-poms `products/pom.xml`, already removed by `cde78fd2` on 2026-05-21.
   - **The loss is accepted.** 116 SNAPSHOTs and 45 DSH releases, 0.0.1 to 0.2.4 of the uppercase
     `DSH-*` modules, existed only in git and are gone. The SNAPSHOTs include `mail-processor-service`'s,
     which the issue had not listed. That project stalls until it moves to the current parent-poms
     structure.

**The upstream block is complete.** Task 1 is done too. The rest of the wave runs in this order,
agreed on 2026-10-06 and re-ordered from step 3 on by the re-plan of 2026-10-13. It interleaves
the triaged issues with the tasks, because the wave has two
critical paths that do not wait on each other: design, which Wave 2 waits on, and the platform,
which new code waits on. Items are named by their issue or task number below, which stay as they
are.

1. **Done on 2026-10-06: the legacy release and RC sites are gone.** `#128` — **closed** — PR
   #156. Both trees were removed, with no pointer page, following `#127`'s precedent for
   `snapshots/dsh/`. Its spec, `specs/stories/128-point-wiki-release-link-at-products-path.md`,
   holds the evidence.
2. `#48`, then task 2 (ADR-003). **The `#48` half was done on 2026-10-10:** `#48` — **closed** —
   PR #159. Its findings are `specs/architecture/profiles-and-configuration.md`, and its spec,
   `specs/stories/48-investigate-spring-and-maven-profiles.md`, holds the evidence. The findings
   feed the ADR's choice of transport and persistence by profile. The ADR comes before the Spring
   Boot upgrade: it decides structure, not API detail, and more of the wave waits on it than on
   anything else. **The ADR-003 half was done on 2026-10-13:** `#160` — **closed** — PR #161.
   The ADR is `specs/architecture/ADR-003-document-pipeline.md`, and task 2 below has its
   outcome.
3. **Task 8, the in-memory `DocumentDao` replaces MongoDB.** First since the re-plan of
   2026-10-13, because five items are lighter once MongoDB is gone. `#158` has no
   `mongo.properties` to leave filtered, `#119` has no MongoDB test to migrate, `#120` upgrades
   no MongoDB dependency, `#137` starts no MongoDB container, and `#157` loses its purpose. Until
   then this step read "`#157`, `#158`, `#140`, then `#119`", and tasks 8 to 10 were step 8.
4. `#158`, `#140`, Wave 2 task 3, then `#119`. Mechanical work. `#140` comes before `#120`,
   so `#120` carries fewer settings. Wave 2 task 3 removes `dsh-solr` and the stub applications.
   It depends on no other task, and it comes here so that `#119` migrates no Solr test and `#120`
   carries no `spring-data-solr`.
5. `#120`, after `#119` for the reason given with the triaged issues.
6. Task 3 (ADR-004). It needed ADR-003's answers on search and on local mode, and it has them.
7. `#144`, then `#137`. Both are about the REST API, and both want the Spring Boot line `#120`
   settles.
8. Task 5 (`#44`), task 6, then task 4. Tasks 5 and 6 draw from ADR-003. Task 5 decides whether
   the wiki's source moves into `docs/wiki/`, and task 4's reachability check needs that answer.
   That move would also retire `wiki-sync.yml`, so its disabling for inactivity (task 1's outcome)
   waits on task 5 rather than getting an issue now.
9. Tasks 9 and 10, the new behaviour in `dsh-data`. They come after `#120`, so that new code is
   written on the final Spring Boot line, and after `#132`.
10. `#148`, last, before the 0.4.0 release candidate. It should learn from everything this wave
    changes in the build. The commit gate and the production smoke check that the findings (§9.1)
    give the skill are not part of it: no tier exists before Wave 4. They are Wave 7 task 3.

Task 7, `#132`, task 11 and task 12 have no fixed step. They fill gaps, such as an ADR in review,
but each has a latest point. `#132` lands before tasks 9 and 10, so that the new `dsh-data` code
is written to the analysers' rules, and before `#148` in any case, because it changes the build.
Task 8 is written before it, which the re-plan of 2026-10-13 accepted. Task 7 lands before `#148`,
inside the wave, because Wave 3's stories cite the papers. Task 11 lands before Wave 2 task 2,
which adds to the OpenAPI document. Task 12 lands before Wave 2 task 1, whose flow is the first
thing that can leave a document in `NOT_INDEXED_ERROR`. **Tasks 11 and 12 landed on 2026-10-16**,
ahead of both: `#162` — **closed** — PR #164, and `#171` — **closed** — PR #172. **Task 7 landed
the same day**, ahead of `#148`: `#173` — **closed** — PR #174.

Every open parent-poms issue has a milestone since 2026-10-02 (§3). Since the re-plan of
2026-10-10, `parent-poms#90` and `#109` belong to Wave 4, `#91` to Wave 7, `#92` to Wave 10, and
`#85` to Wave 11.

**Triaged issues:**

- `#48` — **closed** — PR #159. Investigate how to use profiles (dev, staging, production) with
  Spring and Maven. It underpins selecting a transport or a persistence implementation by profile,
  which ADR-003 (task 2 below) relies on.
- `#119` — Migrate every test to JUnit 5 and drop the vintage engine.
- `#120` — Upgrade Spring Boot to a supported line, version chosen by analysis.

  Like `#48`, neither is an ADR-001 phase task. Both were raised on 2026-09-25, while
  specifying `#112`, and they are ordered: `#119` lands first, because Spring Boot 3's
  `spring-boot-starter-test` drops the vintage engine and JUnit 4 tests would stop running
  without failing anything. `#120` belongs in this wave because Wave 5's
  `spring-cloud-gcp-starter-*` release line is tied to the Spring Boot line, and because the
  pipeline written in Waves 2 and 3 should be written once, against the Boot line it will ship on.
  Neither issue names a target version. Both leave it to their spec's analysis, and for `#120` that
  includes whether the version moves in parent-poms `products/pom.xml`, where it is managed today
  for every product.
- `#127` — **closed** — PR #149. Deploy the snapshot site from DEVELOP, as parent-poms' deploy.yml does.
- `#128` — **closed** — PR #156. Point the wiki's release link at releases/products/dsh once 0.3.0 is released.

  Neither is an ADR-001 phase task either. Both came from reworking the wiki's
  [Code Based Site and Reports](https://github.com/MRISS-Projects/dsh/wiki/Code-Based-Site-and-Reports)
  page on 2026-09-27. At the time, `snapshots/dsh` served a site from 2020, and `group.id.path`
  (`products/dsh`) was about to send 0.3.0's site to `releases/products/dsh`, which would leave the
  wiki's release link on 0.2.x. `#128` could only start once 0.3.0 was out, which it was on
  2026-09-29. `#90`'s fix held on the release: `releases/products/dsh/` serves the root `index.html`
  and all 13 modules' pages. The wiki's release row was moved to `releases/products/dsh/` on
  2026-09-30, after 0.3.2, ahead of the story. All three legacy trees are now gone: `#127` removed
  `snapshots/dsh/`, and `#128` removed `releases/dsh/` and `rcs/dsh/`.

  `#127` ran as step 3 of the upstream block above, not in the order of this list. It was also
  the live proof of three parent-poms fixes, ahead of the 3.10.0 release.
- `#132` — Enforce SpotBugs and Checkstyle in the build and publish their reports.

  Not an ADR-001 phase task. It came from triaging Wave 0's `#43` on 2026-09-27. `#43` asked for
  site reports that parent-poms `3.9.0` already ships, so it closed as met instead of being
  repurposed. The analysers it did not cover became this new issue. Their configuration belongs
  in parent-poms, which is why it waits for 0.3.0.
- `#137` — Run the Postman collections against a lifecycle-managed server.
- `#140` — Give dsh-rest-api's tests a `test` profile file, so that it stops replacing the main
  `application.properties`.

  Neither is an ADR-001 phase task. Both came out of `#46` on 2026-09-28. `#137` is the open
  question `#46`'s spec deferred rather than answered: whether newman joins the Maven lifecycle,
  and what becomes of `api-testing.yml`. It is layer 4 of the test layers in `testing-patterns.md`.
  `#140` came from `#46`'s code review, as "Remove the unused `spring.data.mongodb.*` settings
  from dsh-rest-api". Since the re-plan of 2026-10-13 only the part `#48` added to it is left,
  the test profile's properties file, because task 8 removes those settings. It was retitled on
  GitHub that day.
- `#44` — Add architecture and components diagrams. It moves here from the product backlog, and
  task 5 below is written as its body, so no new issue is opened for that task.
- `#144` — Swagger UI is not served: `/swagger-ui/` returns 404 and the webjar page fails to load.

  Not an ADR-001 phase task. It came out of `#143` on 2026-09-28, whose smoke run followed the
  README literally and found no Swagger UI at any path. It was not folded into `#143`: that story
  changed documentation only, and the fix is a change to `dsh-rest-api`, so the 0.3.0 README says
  the UI is not served. It sits in this wave rather than Wave 2 because it does not depend on
  ADR-003, and because the choice it leaves open, Springfox's starter or springdoc-openapi, turns on
  the Spring Boot line `#120` settles.

- `#148` — A dsh-release skill that runs a DSH release or hotfix end to end, with the checks 0.3.0
  and 0.3.2 taught us.

  Not an ADR-001 phase task. It was raised on 2026-09-30, after `#146`'s release. That release
  shipped without its release-notes section, because the story closed after the README was
  generated. It also took three dispatches and left a partial 0.3.1. `#148` was not folded into
  `#146`, which was a release, not tooling. It belongs in this wave because the next release is
  0.4.0, and the skill should exist before it.

- `#157` — **closed**, with no pull request: superseded by task 8 (§5). Give `mongo.*` its values
  at run time, not through build-time filtering.
- `#158` — Stop Maven from filtering `${…}` in Spring configuration.

  Neither is an ADR-001 phase task. Both came out of `#48` on 2026-10-10, as follow-ups 2 and 1 of
  its findings (`specs/architecture/profiles-and-configuration.md`, §9.2). They were not folded
  into `#48`, which was an investigation and changed no code. Their milestone, `0.4.0-SNAPSHOT`, is
  shared by Waves 1 and 2. They sit in this wave because neither depends on ADR-003.

  **`#157` was superseded on 2026-10-13 by task 8**, which removes MongoDB and every `mongo.*`
  value with it, the build-time values in the six workflows included. It has no step in the
  order of work any more. The owner closed it that day, and it is in §5. `#158` is in step 4.

  The findings' other four follow-ups went into issues that existed or belong elsewhere: `#137`
  gained AC005, `#120` gained AC007, `#140` gained a second change to make, and
  `parent-poms#109` is in Wave 4.

Since the re-plan of 2026-10-13, three of these have less to cover than their bodies say. `#158`
has no `mongo.properties` to leave filtered. `#137` starts no MongoDB container. `#120` upgrades
no MongoDB dependency, and no `spring-data-solr` either, because Wave 2 task 3 runs ahead of it.

`#142`, which came from the same `#46` review, moved to Wave 2 on 2026-09-28.

**Tasks:**

1. **Done on 2026-10-06: keep the document-status diagram generated from code.** `#152` —
   **closed** — PR #153. Generate the document-status diagram from DocumentStatus and fail the build
   when it drifts. Its spec, `specs/stories/152-generate-document-status-diagram.md`, holds the
   evidence. The plan as written is kept below.

   `src/site/resources/images/workflow.jpg` drew `DocumentStatus`'s state machine by hand.
   `transition(TransitionType)` is a pure function over three transition types, so the whole graph
   can be derived. A context-free unit test in `dsh-data` renders a Mermaid `stateDiagram-v2` from
   `DocumentStatus` × `TransitionType`, dropping self-loops and ending every state that returns itself for all three types at `[*]`. It
   compares the result with a committed `specs/architecture/document-status-workflow.md`, and on a
   mismatch fails with the regenerated text. There is one generator, the test, so the diagram and
   the code cannot disagree without the build going red. `workflow.jpg` is deleted, and the wiki's
   Workflow page links to the file, because a wiki page cannot include a repository file. **This
   task comes before tasks 2 and 3 of Wave 2**, which will change the statuses: the queued and
   dequeued states come from the RabbitMQ design.

   **Outcome.** The diagram is `specs/architecture/document-status-workflow.md`, generated and
   checked by `DocumentStatusDiagramTest`. The wiki's Workflow page links to it, and the sync PR,
   #154, brought that page into `docs/wiki/`. `wiki-sync.yml` had been disabled by GitHub for
   inactivity since 2026-07-21. It was re-enabled on 2026-10-06 to run that sync, and it will be
   disabled again after 60 days without activity on `master`. No issue is open for that yet.
2. **Done on 2026-10-13: ADR-003 — the document pipeline.** `#160` — **closed** — PR #161.
   ADR-003: decide the document pipeline, and revise ADR-001 to match. Its spec,
   `specs/stories/160-adr-003-document-pipeline.md`, holds the evidence. The plan as written is
   kept below.

   Decides:
   - The flow: one Java DSL `IntegrationFlow` as the bus, with stages as POJO endpoints and one
     interceptor or advice that records each `DocumentStatus` transition, so no stage touches
     persistence for status.
   - The deployables: one application running the whole pipeline, with channels between stages that
     can later be split into separate services by profile. What becomes of the four stub worker
     modules follows from this.
   - The transports: in-memory channels by default, with transport adapters (Pub/Sub, Wave 6)
     selected by profile.
   - Where corpus document-frequency statistics live. IDF needs them across every document, and
     Solr supplied them until now.
   - **Whether Vertex AI Search has any role.** DSH extracts keywords and sentences; it offers no
     search, and Vertex AI Search exposes no term vectors. Wave 8's contents follow from the answer.
   - The statuses, if the design changes them. Task 1 keeps the diagram in step.

   Four more, added on 2026-10-10 from `#48`'s findings (§9.1):
   - **How an implementation is selected.** The findings propose a property per service, with
     `gcp` as the profile that sets them together (R3). Waves 5 and 6 are written against the
     answer.
   - **The `gcp` transport is driven by requests.** With zero minimum instances, each stage on
     Cloud Run has to be started by a request, which points at Pub/Sub delivering by push (§8.4).
     Wave 6 task 2 follows from this.
   - **Whether the XML contexts go under a profile where they stand**, which the findings show is
     possible (S5), or are rewritten in Java first.
   - **What a run with no profile persists to once the legacy implementations are removed**
     (Wave 9 task 2). The candidates are an in-memory implementation, which no wave plans, and the
     Firestore emulator (§6.3).

   ADR-003 also revises ADR-001 in place, which its **Proposed** status allows: the Context table is
   corrected, Phase 1 tasks 3-6 and the Phase 2 migration utility are dropped, and Phases 3-5 are
   rewritten to match the waves below.

   **Outcome.** The ADR is `specs/architecture/ADR-003-document-pipeline.md`, accepted by the
   merge of PR #161 on 2026-10-13. Every question listed above is decided there. ADR-001 was
   revised in place and keeps its status, Proposed. The re-plan of 2026-10-13 applied the ADR's
   "Consequences for the waves" to this document.
3. **ADR-004 — distribution.** Decides:
   - A run path and a build path. Running DSH needs a container image, and a public GHCR image pulls
     without credentials. Cloud Run cannot deploy from ghcr.io directly, so the deploy script copies
     the image into the user's Artifact Registry or configures a remote repository there.
   - The release zip, attached to the GitHub Release rather than published as a package: sources,
     binaries, the deploy script, and an offline Maven repository of the MRISS artifacts the build
     resolves (`parent-poms#92`). Its acceptance test is a build from the zip with an empty local
     repository and no GitHub credentials.
   - What local mode can and cannot do. Firestore and Pub/Sub have emulators, and GCS has only
     third-party fakes. Search needs no real GCP project: ADR-003 gives no search engine a role
     (section 6). Until 2026-10-13 this bullet said a local run might still need one.

   More, added on 2026-10-10 from `#48`'s findings (§9.1). Waves 4 and 7 deliver this part, and
   Wave 10 the part above:
   - **The operated tiers.** `dsh-dev`, `dsh-staging` and `dsh` are three runs of one deploy, in
     one GCP project, each given a base name. Whether that deploy and the end user's script are
     one script.
   - **The names**, from the table in the findings (§8.1), and the points the documentation leaves
     open (§8.3). One image, tagged with the dotted version; revisions named with dashes.
   - **The check on the base name and on `gcp`.** A deployment that leaves either out starts
     cleanly: the first as production, the second without GCP.
   - **The bucket.** Uniform bucket-level access and a managed folder per tier, or one tier can
     overwrite another's objects.
   - **The cost rule, service by service**: the setting that costs nothing while idle, the
     retention that removes old `dev` and `rc` artifacts and test objects, and which tier holds
     the one free Firestore database.
   - **Layer 5.** How the tests authenticate to a Cloud Run service, and how they clean up what
     they write on `dsh-dev` and `dsh-staging`.
   - **The release gate.** Where it runs, whether the commit is also a label on the revision, and
     how a hotfix branch reaches `dsh-staging` while a release candidate occupies it.

   More, added on 2026-10-13 from ADR-003's consequences:
   - **The push endpoints.** Under `gcp`, Pub/Sub calls the service on two endpoints (Wave 6
     task 2). How they are protected, so that they accept requests from Pub/Sub only.
   - **Rules R5, R6, R8 and R10 of the findings**, which ADR-003 leaves to this ADR.
   - **The retention of the corpus term statistics.** They gain every test document on an
     operated tier, and their interface offers no removal.
   - **How a test that calls a store directly reaches `dsh-dev`.** Layer 5a goes over the wire
     and cannot run the two-caller case of Wave 5 task 7.
4. **Documentation map.** `CLAUDE.md` is the root of the developer documentation tree. A CI check,
   like `check-spec-references`, fails when a Markdown file under `docs/`, `specs/` or `.github/`
   cannot be reached from `CLAUDE.md` by links. The same task deletes the empty scaffolding READMEs
   (`specs/testing/*`, `specs/requirements/*`, `docs/user-guides`).
5. **Wiki restructure** (`#44`). Home carries the overview and a Mermaid diagram of the pipeline's
   stages, marking stages not yet built as planned. It adds an Architecture page, and a Developers
   page that points at `CLAUDE.md` rather than restating it. `src/site/markdown/index.md` shrinks to
   one paragraph and links, so the overview exists once. User-facing and overview pages use generic
   terms (indexer, NoSQL store, queue); ADRs and developer pages keep the product names. The task
   also decides whether the wiki's source moves into `docs/wiki/`, published to the wiki rather
   than synced from it, so its pages get review and markdownlint.
6. **Wiki page on Spring Integration**, with a diagram of the flow. It follows task 2. Its
   diagrams, and task 5's, draw from ADR-003 sections 1 and 4, and the page can link to the
   ADR's sequence diagram.

   The diagrams in tasks 5 and 6 are drawn by hand from ADR-003, because no flow exists yet to
   generate them from. They are interim: Wave 2 task 5 generates the pipeline diagram from the flow
   and keeps it in sync.
7. **Done on 2026-10-16: the reference papers are recorded.** `#173` — **closed** — PR #174.
   Record the reference papers Wave 3 is built from, and keep their full text out of git unless a
   licence allows it. Neither licence allows it, so both papers are cited from
   `specs/references/README.md`, not committed. The story's spec,
   `specs/stories/173-record-reference-papers.md`, holds the evidence. The plan as written is kept
   below.

   **Reference papers.** The keyword paper is committed under `specs/` only if its licence permits
   redistribution; otherwise it is cited by DOI. The sentence paper is paid: it stays in a gitignored
   local folder and is cited by DOI. The owner is a co-author, so whether the publisher allows posting
   the accepted manuscript is worth checking. Wave 3's stories cite these.
8. **The in-memory `DocumentDao` replaces MongoDB.** `#167` — open. Keep documents in memory by
   default, and remove MongoDB.

   `dsh-data` gains an in-memory
   implementation of `DocumentDao` as the interface stands today. It is the default, selected by
   `dsh.persistence` (ADR-003, section 3). The same story removes MongoDB: `MongoDocumentDao`,
   `DocumentRepository`, `dshApplicationContext.xml`, `mongo.properties`, the MongoDB
   dependencies, the `spring.data.mongodb.*` settings in `dsh-rest-api`, the MongoDB container
   that `-DintegrationTests` starts, and the `mongo.*` values in six workflows and in the
   `settings.xml` instructions. It corrects `CLAUDE.md` and
   `specs/architecture/system-design.md` where they describe MongoDB as current. The interface
   does not change in this task, so no MongoDB code is written. (ADR-001 Phase 1: the in-memory
   implementation of task 1.1, and task 1.2.)

   Until 2026-10-13 this task wrapped `DocumentRepository` behind a
   `DocumentPersistenceRepository` interface and marked the wrapper `@Deprecated`.
9. **The conditional status advance, and the store's contract.** `DocumentDao` gains an advance
   that names the status it expects, and applies only if the stored status is still that one
   (ADR-003, section 1). The contract is written on the interface: the advance is atomic, a write
   names its fields, and a read returns a copy. One contract test holds it, with a case of two
   callers attempting one advance at once, and every implementation passes that same test. Where
   the test class lives, so that `dsh-gcp` can run it in Wave 5, is this task's to choose. Only
   the in-memory implementation exists to satisfy it. (ADR-001 Phase 1: the rest of task 1.1.)

   Until 2026-10-13 this task refactored `MongoDocumentDao` and marked it `@Deprecated`.
10. **The file store.** `dsh-data` gains a `FileStorageService` interface, and `byte[]
    originalFileContents` leaves the `Document` entity for it. The store holds two kinds of
    object: the original file, and the indexing stage's output (Wave 3 task 1). It lets a caller
    store an object so that a repeat replaces it, which `store(byte[]) → URI` alone does not.
    `LocalFileStorageService` is the default, selected by `dsh.file-store`. It writes under
    `dsh.storage.local-directory`, whose default this task chooses. Nothing is marked
    `@Deprecated`. `GcsFileStorageService` follows in Wave 5. (ADR-001 Phase 1 task 1.7.)
11. **Done on 2026-10-16: the OpenAPI document and the feature specs describe what exists.**
    `#162` — **closed** — PR #164. Make the OpenAPI document and the feature specs describe the API
    that exists. Its spec, `specs/stories/162-openapi-and-feature-specs-describe-the-api.md`, holds
    the evidence. The plan as written is kept below.

    Added on 2026-10-13. `specs/api/openapi/dsh-rest-api.yaml` describes four routes under
    `/api/v1` that the code does not have, and `document-analysis.md`, `indexing-workflow.md`, `rest-api-endpoints.md`
    and `smart-highlighting.md` in `specs/features/` follow it. The code is the baseline. The
    OpenAPI document is rewritten to the two endpoints `DocumentResource` has. The four feature
    specs are corrected against the code and ADR-003, and what they describe that no wave has
    built yet is marked as planned. That covers what ADR-003 overrides in
    `indexing-workflow.md`: the three retries of a failed task, the "Analysis timeout" row, and
    Solr. From then on an API change starts in the OpenAPI document, as
    `.github/copilot/rules/api-standards.md` asks. Documentation only.
12. **Done on 2026-10-16: every status describes itself.** `#171` — **closed** — PR #172.
    `NOT_INDEXED_ERROR` answers `DEQUEUED_FOR_INDEXING`'s description and message. Its spec,
    `specs/stories/171-not-indexed-error-describes-itself.md`, holds the evidence. The plan as
    written is kept below.

    Added on 2026-10-13. `NOT_INDEXED_ERROR` is declared with
    the description and the message of `DEQUEUED_FOR_INDEXING`, and a client reads both through
    `/status/{token}`. The task is the fix, and a test that holds every status to a description
    of its own. It gets an issue, so that the fix is in 0.4.0's release notes.

### Wave 2 — Pipeline walking skeleton

Milestone: `0.4.0-SNAPSHOT`. It runs on the in-memory `DocumentDao` (Wave 1 task 8) and on
in-memory channels, with no RabbitMQ and no Solr. Every task depends on ADR-003. Until
2026-10-13 this line said "the existing persistence layer", which was MongoDB.

**Triaged issue:** `#142` — Enqueue acks are applied to every in-flight document, not the one they
confirm. It came out of Copilot's review of PR #141. The defect lives in `DocumentQueueServiceImpl`
and `DocumentEnqueueResponseMessageHandler`, which task 1 deletes, and the re-plan rules out fixing
RabbitMQ code. But the defect is about behaviour, not about RabbitMQ: a document must end in its own
status when submissions overlap. So it is not closed as won't-fix. Task 1's story carries the test
`#142` describes as an acceptance criterion against the new flow: two overlapping submissions, one
forced to fail, each ending in its own status. That story closes `#142`. It was briefly proposed
for won't-fix on 2026-09-28, and moved here the same day instead. **0.3.0 ships with the defect**,
knowingly: status is wrong for overlapping submissions until Wave 2.

**Tasks:**

Task 1 comes first. Tasks 2, 4 and 5 follow it, in any order. Task 3 depends on none of them, and
runs at step 4 of Wave 1's order of work. Tasks 4 and 5 were split out of task 1 on 2026-10-13.

1. **The flow, end to end.** A gateway receives a submitted document id, and pass-through stages carry
   it through every status to `SENTENCES_PROCESSED_SUCCESS` (ADR-003, section 1).
   - **The flow.** One Java DSL `IntegrationFlow` in `dsh-rest-api`, with two asynchronous hops.
     A hop stays inside the JVM here. It is selected by `dsh.transport`, whose default is
     `memory`.
   - **The status recorder.** One component records every transition, at twelve points, through
     the conditional advance of Wave 1 task 9. No stage writes a status.
   - **`/submit` stores and sends before it answers.** The `@Async` call after the response is
     removed, and the document is stored as `CREATED` before the first point.
   - **The RabbitMQ enqueue path is deleted**: `enqueue-docId-context.xml`,
     `DocumentQueueServiceImpl`, its handlers, the AMQP dependencies, the RabbitMQ container that
     `-DintegrationTests` starts, and the unused test resource `dequeue-docId-context.xml`. The
     story corrects `CLAUDE.md` and `specs/architecture/system-design.md` where they describe
     RabbitMQ as current.
   - **Three proofs.** `#142`'s test of two overlapping submissions, which proves ADR-003's
     assumption A1. A context test started with a value of `dsh.transport` that matches no
     transport, which proves A6. And the table of twelve points against
     `DocumentStatus.transition`: applying each point's transition to its "From" status gives its
     "To" status.
2. **A results endpoint.** Retrieve a document's keywords and sentences by token. The API has
   `/submit` and `/status/{token}` and nothing that returns a result. It returns empty lists until
   Wave 3 fills them, but its contract is fixed here. The contract is written in the OpenAPI
   document first, from the baseline Wave 1 task 11 leaves.
3. **The modules, per ADR-003 section 2.** `dsh-doc-indexer-worker` is renamed
   `dsh-doc-indexer`. It, `dsh-keyword-extractor` and `dsh-top-sentences-extractor` become
   libraries with no `Application` class. `dsh-doc-processor-worker` is removed. `dsh-solr` is
   removed with its two plugins, and the `spring-data-solr` declarations go with it. The story
   corrects `CLAUDE.md`'s module table and `specs/architecture/system-design.md`. It depends on
   no other task of this wave, and runs at step 4 of Wave 1's order of work, ahead of `#119` and
   `#120`. Until 2026-10-13 this task read "Consolidate, keep or remove the four stub worker
   modules as the ADR decides", and removing `dsh-solr` was Wave 9 task 2.
4. **Repeats and failures.** Split out of task 1 on 2026-10-13. What the flow does when a message
   arrives twice, and when something fails (ADR-003, section 1, "Compare-and-set" and "Failures"):
   - **The first write wins**, where the sender recorded a hop's error and the message arrives
     all the same.
   - **A repeat resumes from the document's status.** It does not run again a stage the document
     is past, and it sends on the next hop when that send was never recorded as a success.
   - **A store's failure is told from a stage's own.** The first records nothing and leaves the
     message unconsumed. The second records the stage's error status and consumes the message. A
     stage's own failure is not retried.
5. **The pipeline diagram is generated from the flow.** Split out of task 1 on 2026-10-13. An
   integration test reads the running flow's channels and endpoints from Spring Integration's
   `IntegrationGraphServer`, renders them as Mermaid, and fails with the regenerated text when
   they differ from a committed `specs/architecture/document-pipeline-flow.md`. It is the same
   pattern as Wave 1 task 1's status diagram, but it must be an `*IT`: the graph exists only in a
   running Spring context, and a unit test never starts one. So it runs under
   `-DintegrationTests` and on every staging build, not on pull-request CI. The wiki pages from
   Wave 1 tasks 5 and 6 then link to this file, and their hand-drawn diagrams are retired.

### Wave 3 — Analysis algorithms

Milestone: `0.5.0-SNAPSHOT`, created on 2026-10-16 for `#163` (§3). Everything here is written from scratch against the two papers recorded in [`specs/references/README.md`](../references/README.md) (Wave 1 task 7). Each
story brings its expected-output fixtures.

**Triaged issue:** `#163` — Ranked keyword and sentence sets drop entries that tie on score. It was
found on 2026-10-14 while building `#162` (that story's spec, §8.5). It was not folded into `#162`,
which changed documentation only. It sits in this wave because nothing fills the sets until tasks
4 and 6 rank keywords and sentences into them, so the defect is latent until then.

**Test fixtures.** `dsh-test-dataset` holds four PDFs and nothing else (`#124` §3). A module unpacks
them with a `maven-remote-resources-plugin:process` execution into `target/test-classes`, with
`attachToMain=false` — copy the block from `dsh-data/pom.xml`. This wave meets two of its gaps:

1. **No HTML fixtures**, although `#12` and `README.md` promise PDF and HTML. Task 1 adds them.
2. **No expected outputs.** No reference keywords or top sentences exist to assert against. Each
   task adds the ones it needs, derived from the papers.

The bundle is a plugin parameter, not a dependency, so `mvn -pl <module> -am` does not build
`dsh-test-dataset`, and the plugin resolves it from the local repository or GitHub Packages
instead. That copy can be missing or older than the source. Run a full `mvn -B install` first, or
add `dsh-test-dataset` to `-pl`.

**Every stage in this wave is idempotent**: running it twice for one document leaves the same
result (ADR-003, section 1). It stores its result through the store's interface, in a write that
names its fields, before the recorder writes the success status.

**Tasks:**

1. **Text extraction.** PDF and HTML into title, paragraphs, sentences and terms. Terms are
   extracted by a library inside the JVM, and the library is the spec's choice. The stage's output,
   the extracted text and the document's own term frequencies, goes to the file store (Wave 1
   task 10) and not into the record. A repeat replaces it.
2. **Corpus term statistics.** Term frequency per document and document frequency across the corpus.
   The interface of ADR-003 section 5, in `dsh-data`, with an in-memory implementation and one
   contract test. The update is atomic, and it counts a document once however often its stage
   runs. The statistics are stored where the documents are, so `dsh.persistence` selects both.
   Until 2026-10-13 this task read "stored where ADR-003 decides".
3. **TF and IDF variants**, as the keyword paper defines them.
4. **Keyword ranking** by the meta-algorithmic combination of task 3's variants. It sets a limit on
   the number of keywords returned, which keeps the record bounded.
5. **Sentence similarity to the title**, by the sentence paper's algorithm.
6. **Sentence ranking.** Combine task 5 with task 3's scores, and return the top sentences with their
   paragraph numbers. It sets a limit on the number of sentences returned, for the same reason.

### Wave 4 — GCP foundation and the development tier

Milestone: `0.6.0-SNAPSHOT`. Added by the re-plan of 2026-10-10. It delivers the part of ADR-004
(Wave 1 task 3) that the operated tiers need, and every task depends on that ADR. No GCP project
exists before this wave.

**At the end of this wave `dsh-dev` proves only that the image starts and reports itself.** No
`gcp` implementation exists yet, so no document test runs there until Wave 5. The deployment sets
`gcp` from the first deploy all the same, so that the deploy does not change when Wave 5 gives the
profile something to select.

**Triaged issue:** `#45` — Document, configure and test Spring Boot actuators for the REST API
module. It moves here from the product backlog, because the smoke check reads health, the version
and the commit. The issue has no body, so task 1 below is written as its body, as was done for
`#44`. Its GitHub milestone moved from `1.0.0-SNAPSHOT` to `0.6.0-SNAPSHOT` on 2026-10-10.

**Upstream issues:**

- [`parent-poms#90`](https://github.com/MRISS-Projects/parent-poms/issues/90) — manage
  docker-maven-plugin's version. Needed by task 2.
- [`parent-poms#109`](https://github.com/MRISS-Projects/parent-poms/issues/109) — expose the
  version and the build number as outputs of the reusable staging workflow. Needed by task 4.

  `#109` came out of DSH `#48` on 2026-10-10. It was not folded into `#91`, which covers the
  release and hotfix workflows; `#109` is the staging one.

Both are on parent-poms `3.12.0-SNAPSHOT`, this wave's milestone there (§3). They were listed under
the distribution wave until 2026-10-10.

**Tasks:**

1. **Version, commit and health** (`#45`). The build stamps the version and the commit into the jar
   with `spring-boot:build-info`, and stamps nothing else (findings M2, R8). The application serves
   health, and info carrying both values. Other actuator endpoints stay off. What health reports
   while persistence is in memory, which is the default from Wave 1 task 8 until Wave 5, is the
   spec's to settle.
2. **The container image.** A Dockerfile, and an image named `dsh` and tagged with the dotted
   version (findings §8.2), built as ADR-004 decides. One image serves every tier and local mode.
   It runs locally before anything is deployed. Publishing it for end users is Wave 10 task 1.
3. **The GCP project bootstrap.** The owner creates the project and its billing, with a budget
   alert. A script that is safe to run twice creates the rest: the APIs, the image repository with
   a cleanup policy for `dev` and `rc` tags (findings §8.4), and the identity GitHub Actions
   deploys with. Every setting follows the cost rule in `CLAUDE.md`.
4. **Deploy `dsh-dev` from `deploy.yml`.** A DSH job after the reusable workflow pushes the image
   and deploys revision `dsh-dev-<version>-dev-<n>` (findings §8.1), with zero minimum instances,
   `DSH_NAME=dsh-dev` and `SPRING_PROFILES_ACTIVE=gcp`. It takes the version and the build number
   from `parent-poms#109`'s outputs. The first deploy settles two points the documentation leaves
   open (findings §8.3): what a deploy does when the revision name already exists, and whether a
   traffic tag may start with a digit.
5. **The smoke check, layer 5b, against `dsh-dev`.** After each deploy it asks for health, the
   version and the commit, and compares them with what was built. It also checks that the base name
   the application reports is the service's, which is the check the findings (section 5) leave to
   ADR-004. That introduces `dsh.name` (R5), so `#158` comes first (Wave 1). The check reads and
   writes no document, and Wave 7 reuses it for production. The same story adds layer 5 and rule
   R10 to `.github/copilot/rules/testing-patterns.md`.

### Wave 5 — GCP: Firestore and GCS

Milestone: `0.6.0-SNAPSHOT`, moved from `1.0.0-SNAPSHOT` on 2026-10-10. It was Wave 4 until then.
ADR-001 §4, "Phase 2", as ADR-003 revised it. The migration utility is dropped: no deployment
holds data to migrate.

The GCP implementations live in a new module, `dsh-gcp`, and nowhere else (ADR-003, sections 2 and
3). `dsh-rest-api` depends on it at `runtime` scope and no other module does, so a class anywhere
else that names a Firestore or Cloud Storage type does not compile. An implementation is selected
by a property, `dsh.persistence` or `dsh.file-store`, and the `gcp` profile sets them together.
Until 2026-10-13 the tasks below put these classes in `dsh-data`.

**Triaged issue:** `#49` — re-scoped twice. Originally titled "Create docker structure based on
docker files to have all servers configured as docker containers to run tests and/or the
application" — i.e., Docker containers for MongoDB/RabbitMQ/Solr. On 2026-09-28 it was re-scoped to
Firestore and Pub/Sub emulators for local and CI testing, and placed with the Pub/Sub work. On
2026-10-10 it was narrowed to the **Firestore emulator** and moved here, because this wave's
Firestore code needs it for layer 3 (findings §7). Wave 6 task 3 adds the Pub/Sub emulator to the
same structure, and Wave 10's local mode reuses both. Its GitHub milestone moved from
`0.4.0-SNAPSHOT` to `1.0.0-SNAPSHOT` on 2026-09-28, and to `0.6.0-SNAPSHOT` on 2026-10-10.

Cloud Storage has no official emulator (findings G6). So task 4 is verified on `dsh-dev`, by
task 6.

**Tasks:**

Tasks 4 to 6 keep their numbers. Until 2026-10-13, task 2 created a
`FirestoreDocumentPersistenceRepository` and task 3 put the DAO on top of it. ADR-003 leaves no
such layer, so the two are one task now, and task 3 is new.

1. **The `dsh-gcp` module.** A library with `spring-cloud-gcp-starter-data-firestore` and
   `spring-cloud-gcp-starter-storage`. `application-gcp.properties` in `dsh-rest-api` sets
   `dsh.persistence` and `dsh.file-store`. The context test of the `gcp` profile proves
   ADR-003's assumption A5: the context finds the implementations in a module the application
   cannot compile against. The task also settles how the module's unit tests sit beside the
   contract tests, because a run against an emulator is an integration test and counts nothing
   toward the 95% gate.
2. **`FirestoreDocumentDao implements DocumentDao`**, selected by `dsh.persistence=firestore`.
   The mapping of `Document` to Firestore is inside the module, and `Document` carries no
   provider's annotation. The status advance is a transaction that reads the document and writes
   the status only if it matches. It passes Wave 1 task 9's contract test against the emulator.
3. **Corpus term statistics on Firestore**, behind the interface of Wave 3 task 2, with a bound on
   the writes per document (ADR-003, section 5). It passes that task's contract test against the
   emulator. It proves assumption A4: a layout exists that bounds the writes, fits Firestore's
   limits, and lets one transaction cover the update.
4. **`GcsFileStorageService implements FileStorageService`**, selected by `dsh.file-store=gcs`.
   It uploads and downloads objects in a GCS bucket. Add a fixture above Firestore's 1 MiB
   document limit: the largest today, `The-Categories.pdf`, is 262 KiB.
5. **The tier's data resources.** The deploy creates the Firestore database `dsh-dev-db`, the
   bucket with uniform bucket-level access, the `dsh-dev/` managed folder with the tier's service
   account granted on it alone, and a lifecycle rule that removes test objects (findings §8.1,
   §8.4). Which tier holds the one free Firestore database is ADR-004's choice, and so is the
   retention of the term statistics.
6. **Layer 5a begins.** The layer 3 tests run over the wire against `dsh-dev` after each deploy:
   submission and status, with the document in Firestore and its file in the bucket. How they
   authenticate, and how they clean up what they write, is ADR-004's to decide (findings §7). The
   pipeline finishing on Cloud Run is asserted from Wave 6, not here.
7. **Two callers against Firestore itself.** Added on 2026-10-13. The Firestore emulator does not
   implement all the transaction behaviour of production, so it proves the advance for one caller
   only. A test that calls the store directly, in `dsh-dev`, has two callers attempt one advance at
   once, and one succeeds (assumption A3). Layer 5a goes over the wire and cannot run it. How the
   test reaches `dsh-dev` is ADR-004's to decide (Wave 1 task 3).

### Wave 6 — GCP: Pub/Sub

Milestone: `0.6.0-SNAPSHOT`, moved from `1.0.0-SNAPSHOT` on 2026-10-10. It was Wave 5 until then.
ADR-001 Phase 3, rewritten by ADR-003 for the flow Wave 2 builds. The Pub/Sub classes go into
`dsh-gcp` (Wave 5 task 1).

`#49` was listed here until 2026-10-10. It is in Wave 5 now.

**Tasks:**

1. Add `spring-cloud-gcp-starter-pubsub` and `spring-integration-gcp`, in `dsh-gcp`.
2. **The `pubsub` transport**, selected by `dsh.transport=pubsub`, which the `gcp` profile sets
   (ADR-003, section 4). Each of the flow's two hops is a topic with a push subscription to the
   same Cloud Run service, so minimum instances stays 0. For each hop there is a publish to the
   topic, whose outcome is awaited inside the request, and a push endpoint that hands the
   document's id to the flow. The endpoint answers with a success code to consume the message,
   and with a failure to have it delivered again. The endpoints are protected as ADR-004 decides
   (Wave 1 task 3). Until 2026-10-13 this task read "The `gcp` transport for the channels between
   stages, as ADR-003 decides", and until 2026-10-10 "Pub/Sub channel adapters for the channels
   between stages, under `@Profile("gcp")`".
3. **The Pub/Sub emulator**, added to `#49`'s structure, for layer 3.
4. **The tier's topics and subscriptions.** The deploy creates two topics and two push
   subscriptions for each tier, named `dsh-dev-<channel>` on the first (findings §8.1). It sets
   what ADR-003 leaves to this wave: the retry policy, a dead-letter topic and its maximum of
   delivery attempts, the message retention, and the acknowledgment deadline. The task also
   settles what the publisher tries again before it reports a failed send, because a failed send
   is final once it is recorded. Layer 5a then asserts that a submitted document reaches its final
   status on `dsh-dev`. That proves assumption A2: the outcome of a publish is known inside the
   request that made it.

### Wave 7 — The release path on GCP

Milestone: `0.6.0-SNAPSHOT`. Added by the re-plan of 2026-10-10. It gives the release candidate and
the release their own tiers, by running Wave 4's deploy twice more under other base names (findings,
section 5). The gate is stated once in the findings (section 7), for a release and a hotfix alike.

**Upstream issue:**

- [`parent-poms#91`](https://github.com/MRISS-Projects/parent-poms/issues/91) — expose the release
  tag and version as outputs of the reusable release and hotfix workflows. A DSH job after the
  reusable workflow cannot otherwise know which release to deploy. Needed by task 2. On parent-poms
  `3.13.0-SNAPSHOT`, this wave's milestone there (§3). It was listed under the distribution wave
  until 2026-10-10.

**Tasks:**

1. **Deploy `dsh-staging` from `staging.yml`.** Revision `dsh-staging-<version>-rc-<n>`, with its
   own database, managed folder, topics and subscriptions, and layer 5a against it.
   `parent-poms#109` (Wave 4) already covers the outputs of a release-candidate build.
2. **Deploy `dsh` from `release.yml` and `hotfix.yml`.** It is built from the release tag, so the
   image is a rebuild, one release commit ahead of what staging tested. Layer 5b covers that
   rebuild: health, the version and the commit, and no document. Production data is never used by a
   test.
3. **The commit gate, and the release skill.** A release or a hotfix goes ahead only when the
   `dsh-staging` revision that passed layer 5a was built from the head of the branch being
   released, read from its build stamp. If it was not, staging runs first. Where the gate runs is
   ADR-004's to decide. The `dsh-release` skill (`#148`, Wave 1) gains the gate and the production
   smoke check.

**0.6.0's release candidate and release are the first real run of this path** (§6).

Left open, for ADR-004: how a hotfix branch reaches `dsh-staging` while a release candidate
occupies it.

### Wave 8 — Retired

It was "GCP: term statistics and search", on `1.0.0-SNAPSHOT`, and Wave 6 until 2026-10-10. Its
contents waited on ADR-003, which left it empty on 2026-10-13. Corpus term statistics on Firestore
are Wave 5 task 3. No search engine has a role (ADR-003, section 6), and search across documents is
a line in Wave 11's backlog. The number is kept, so that Waves 9 to 11 stay what ADR-003 and
ADR-001 call them.

### Wave 9 — Validation and cutover

Milestone: `1.0.0-SNAPSHOT`. It was Wave 7 until 2026-10-10. ADR-001 Phase 5, less its parity
comparison: there is no old pipeline to compare with.

Two of its four tasks were dropped on 2026-10-10, because earlier waves now do them:

- **"Run the integration tests against the `gcp` profile"** is layer 5a. It runs against `dsh-dev`
  from Wave 5 and against `dsh-staging` from Wave 7.
- **"Make `gcp` the default profile"** is no change to the jar. The jar sets no default profile
  (R2), and the deployment has set `gcp` since Wave 4.

**Tasks:**

1. Performance-test against `specs/testing/performance-benchmarks/` (FR004: 95% of documents under
   10 MB processed within 30 s). It runs against `dsh-staging`, and the cost rule in `CLAUDE.md`
   holds for it as for everything else: minimum instances stays 0.
2. **Dropped on 2026-10-13: nothing is left to remove.** MongoDB goes in Wave 1 task 8, RabbitMQ
   in Wave 2 task 1, and `dsh-solr` in Wave 2 task 3. A run with no profile persists in memory,
   also from Wave 1 task 8. Until then this task read "Remove `dsh-solr` and whatever remains of
   the legacy implementations, as ADR-003 decides". The number is kept.

### Wave 10 — Distribution

Milestone: `1.0.0-SNAPSHOT`. It was Wave 8 until 2026-10-10. Delivers what is left of ADR-004 after
Waves 4 and 7: the end-user packaging. An end user downloads one zip from a GitHub Release and runs
a script; the root `README.md` describes exactly that.

**Upstream issue:**

- [`parent-poms#92`](https://github.com/MRISS-Projects/parent-poms/issues/92) — export the MRISS
  build artifacts a consumer needs as an offline Maven repository. Needed by task 2. On parent-poms
  `3.14.0-SNAPSHOT`, this wave's milestone there (§3).

`parent-poms#90` and `#109` moved to Wave 4 on 2026-10-10, and `parent-poms#91` to Wave 7.

**Tasks:**

1. Publish the container image publicly on every release, as ADR-004 decides. The Dockerfile and
   the image itself are Wave 4 task 2.
2. The release zip: sources, binaries, the offline Maven repository and a build-from-sources script.
   It passes ADR-004's acceptance test.
3. The deploy script, for an end user. It is Wave 4's deploy in the form a user runs against a GCP
   project of their own. It checks its prerequisites (gcloud installed, authenticated, a project
   chosen) and tells the user what is missing. It creates each resource only if absent, so a second
   run is safe, and it deploys the image under the default name, `dsh`. Whether it is the same
   script that deploys the operated tiers is ADR-004's to decide.
4. Local mode: the application in Docker on localhost, with each service on GCP or on an emulator,
   by configuration. It reuses `#49` and Wave 6 task 3. The configuration is the three keys of
   ADR-003, section 3, each of which can be set alone. Nothing in it needs a real GCP project
   because of search.
5. Rewrite the root `README.md` (its source is `src/site/markdown/README.md`) as the user guide for
   the zip and the script: prerequisites, credentials, deploy, local mode.
6. Access through IAP for named Google accounts, which needs no Workspace domain. It waits for a
   front end, which no wave yet plans. Anything it needs that has a fixed cost while idle, such as
   a load balancer, needs the owner's approval under the cost rule.

### Wave 11 — Product backlog

Milestone: `1.0.0-SNAPSHOT`. It was Wave 9 until 2026-10-10. Existing backlog issues with no
dependency on the GCP migration, scheduled after it so the migration lands first.

**Triaged issues:**

- `#50` — Add extra Swagger documentation using annotations
- `#51` — Investigate and add support for `spring-boot-starter-hateoas`
- `#52` — Improvement for performance: file hash generation could be another service. Reviewed and
  **kept**: the issue mentions Mongo only as one option for where to generate the hash, and the
  file-hash-as-a-service idea itself is independent of the persistence backend, so it survives the
  migration.
- `#53` — Make the `DocumentStatus` enumeration dynamic by reading descriptions and messages from
  properties files, per locale
- `#138` — Return 4xx for document submission and lookup failures. Came out of `#46` on 2026-09-28:
  its over-the-wire IT asserts the API's current contract, `200` with an `ERROR` token for a
  failed submission. That contract is a product decision, so the IT pinned it rather than changing
  it.

`#44` moved to Wave 1 on 2026-09-28, and `#45` to Wave 4 on 2026-10-10.

**A backlog line with no issue, added on 2026-10-13: search across documents.** It is the one
thing that would bring a search engine back (ADR-003, section 6). It has two preconditions, and
neither is planned: an owner for each document, so that a search returns only what its caller
may see, and a check of the engine's cost against the cost rule in `CLAUDE.md`. Both need a new
decision.

**Upstream issues:**

- [`parent-poms#85`](https://github.com/MRISS-Projects/parent-poms/issues/85) — decide whether the
  infrastructure site pages are still worth keeping, and what replaces them. On parent-poms
  `3.15.0-SNAPSHOT` since 2026-10-10; it was on `3.13.0-SNAPSHOT` from 2026-10-02 until then. It
  depends on nothing in DSH's plan and blocks nothing, so it is scheduled with the backlog.

## 5. Won't-fix

Three issues were superseded and will not be built as written. Two were superseded by the ADR-001
migration itself. Both were **closed as `not planned` on 2026-09-16** by the repo owner, each with
a comment naming the wave that supersedes it. `scripts/close-wontfix-issues.sh` records exactly
what was run. The third, `#157`, was superseded by ADR-003 and joined on 2026-10-13.

| Issue | Reason | Superseded by |
|---|---|---|
| `#65` (closed) — Implement indexer-worker daemon | Its body specifies enqueuing via RabbitMQ and storing results in Solr — both surfaces this migration replaces. | Wave 2 (pipeline skeleton), Wave 3 (algorithms) and Wave 6 (Pub/Sub) |
| `#47` (closed) — Mongo DAO ordering by timestamp | Targets `MongoDocumentDao`, which ADR-001 Phase 1 wraps and Phase 2 replaces with a Firestore-backed implementation. Ordering behaviour belongs on the new repository, not the one being replaced. | Wave 5 (Firestore + GCS) |
| `#157` (closed) — Give `mongo.*` its values at run time, not through build-time filtering | Every acceptance criterion is about `mongo.*` values. ADR-003 removes MongoDB where ADR-001 planned to migrate it, so none of them has a subject. What its AC004 asked for, the build-time values gone from the six workflows, is part of the task that supersedes it. | Wave 1 task 8 (the in-memory `DocumentDao` replaces MongoDB) |

For `#65` and `#47`, the *Superseded by* column was renumbered by the re-plans of 2026-09-28 and
2026-10-10. The comments on those two issues, and `scripts/close-wontfix-issues.sh`, keep the
numbers of 2026-09-16: Wave 3 and Wave 4 for `#65`, Wave 2 for `#47`.

`#157` was closed by the owner on 2026-10-13, the day the re-plan superseded it, with a comment
naming the task. It came out of `#48` three days earlier and was never started.

`#52` is explicitly **not** in this table — see Wave 11 above for why it was reviewed and kept.

## 6. Known risks / accepted decisions

- **SNAPSHOT parent pin — closed on 2026-09-27.** The root `pom.xml` now names the released
  `com.mriss.mriss-parent:products:3.9.0`, and it stays there: no parent-poms work is picked up
  before DSH 0.3.0 ships, so there is no newer SNAPSHOT to track. The first local build on the new
  pin failed `dsh-rest-api`'s coverage gate at 0.50 lines, from stale IDE output in
  `target/classes`. `clean install` passed, and the local gate is now `mvn -B clean install`
  (`CLAUDE.md`). The decision as it stood until then:

  The root `pom.xml` intentionally tracked `com.mriss.mriss-parent:products:3.9.0-SNAPSHOT`. A
  SNAPSHOT parent
  re-resolves as the parent moves, so the same commit can build differently from one run to the
  next — that is a real reproducibility cost. It is accepted because upcoming work on the
  `MRISS-Projects/parent-poms` project will change this repository's parent, and staying on the
  SNAPSHOT is how those changes reach DSH without a release cycle per iteration. Both Maven
  invocations in this repository's workflows pass `-U` (`#99`), which does not add that drift — it
  makes the drift the SNAPSHOT pin already carried consistent and visible, instead of dependent on
  the age of whatever `~/.m2` cache the runner restored. **Do not file this as a separate task.**
  It is not a standing risk with no end date: Wave 0 now carries the parent-poms goal explicitly,
  and **half of it is done — `3.8.0` was released on 2026-09-19**. What remains is to clear
  `3.9.0-SNAPSHOT`, release `3.9.0`, then re-pin this repo's root `pom.xml` to the released
  `3.9.0`. **This risk closes when that completes**, and needs no owner action before then.

  The pin moved from `3.8.0-SNAPSHOT` to `3.9.0-SNAPSHOT` on 2026-09-19, by way of a deliberate
  detour through the released `3.8.0` to prove a real consumer builds against it. One operational
  fact came out of that and is not recorded in any issue: **`deploy.yml`'s release path rolls
  `master` to the next `-SNAPSHOT` but deploys only the release**, so `products:3.9.0-SNAPSHOT` did
  not exist in GitHub Packages until a separate snapshot deploy published it. Re-pinning to the
  next SNAPSHOT after a release requires that deploy first, or the consuming repository's CI breaks
  on an unresolvable parent.
- **A wave that needs upstream work pins a parent-poms hotfix `-SNAPSHOT` — accepted, proposed on
  2026-09-26 and adopted on 2026-09-27.** When a story finds that the fix it needs belongs in
  parent-poms, that story opens the upstream issues and fixes them. It does not park them for a
  later parent-poms release. The fixes go on parent-poms' hotfix line, and DSH's root `pom.xml` is
  pinned to that hotfix `-SNAPSHOT`, for example `3.9.1-SNAPSHOT`.
  - **The pin stays until the end of the wave.** Once opened, the hotfix `-SNAPSHOT` remains DSH's
    parent for the rest of the wave. Any further upstream fix the wave needs lands on the same
    hotfix line.
  - **Released upstream, then re-pinned here, just before DSH releases.** maven-release-plugin
    refuses a SNAPSHOT parent. So before any DSH release, parent-poms releases the hotfix and DSH's
    root `pom.xml` is re-pinned to that released version. The reproducibility cost of a SNAPSHOT
    parent is the one described in the entry above, and it ends at that re-pin.
  - **A patch version carries fixes only.** A wave that needs new upstream behaviour, rather than a
    fix, uses the full round trip in `CLAUDE.md` against parent-poms' next minor version.
  - **The snapshot must be deployed before it is pinned.** As the entry above records, a hotfix
    `-SNAPSHOT` is not in GitHub Packages until a snapshot deploy publishes it.
  - **A workflow-only fix needs no pin.** parent-poms' reusable workflows and its `commit-readme`
    action are called `@master`, so a fix to them reaches DSH when it merges. The hotfix milestone
    still tracks the issue, but nothing is deployed, pinned or released. `parent-poms#93` was the
    first: milestone `3.9.1`, closed on 2026-09-29 without a release. `3.9.2`, opened the same day
    for `parent-poms#95` and `#96`, was the first hotfix line pinned. Its fix touched
    `products/pom.xml`, so the pin was required. The pin was on DSH's `0.3.x`, not `DEVELOP`, because
    what it fixed shipped as a DSH patch release, 0.3.2 (Wave 1, upstream step 0).

  This applies from Wave 1, the first wave after DSH 0.3.0 shipped on 2026-09-29. Until then, no
  parent-poms work was picked up at all (entry above), with one exception granted on 2026-09-26: an
  upstream issue raised and fixed inside the DSH story cycle that found it. `parent-poms#93` used it.
- **`parent-poms#65` and `parent-poms#69` ship on 3.9.0 proven by rehearsal, not by a real release —
  accepted, with a named confirming run.** Both have acceptance criteria that can only be met by a
  real consuming release, and the release they need is the one this wave exists to unblock. Rather
  than move them to a later milestone and leave the release path carrying two known defects, or hold
  **3.9.0** open indefinitely, they are validated by a rehearsal on a scratch branch — observed
  through markers in the build log — and **DSH's real `0.3.0` release is the confirming run**.
  Anything the rehearsal missed is fixed then, from the hotfix line if it has to be.

  The cost is explicit: a rehearsal exercises the commands, not the full consequence of a release, so
  `0.3.0` is the first time either fix is proven end to end. That is accepted because `0.3.0` is
  DSH's first release through these workflows either way — there is no earlier real release to
  validate against, and a rehearsal is strictly more evidence than the status quo, which is none.
  `parent-poms#72` builds the rehearsal, and its own AC004 required it to say so on both issues if
  it turned out it could not exercise them.

  **The rehearsal exists, and AC004 was met rather than invoked.** `#72` closed on 2026-09-22 and
  reported on both issues. `#69` was exercised and measured — 1 of 13, its analysis confirmed. `#65`
  was not built by `#72`, so what it got instead is the mode to be validated in, the
  `merge-to-develop` marker slot that `#72`'s set-equality check will force it to declare, and the
  evidence line to copy. `project-hotfix.yml`'s rehearsal already reaches and exercises the
  merge-to-master step, so `#65` could be validated there even before `#69` was fixed.

  **`#69` is now fixed and merged, so this decision applies only to `#65`.** `#69` was not merely
  validated by rehearsal — it was corrected by one. Its fix shipped with a permanent check that
  fails the release if any module is left behind, which is stronger than the rehearsal evidence
  this decision was prepared to accept.

  One part of the accepted cost has already come due, and in the direction that favours this
  decision: the rehearsal found *more* than the two known defects — see `#114` and `parent-poms#76`
  in Wave 0 — and found them without pushing a tag, deploying an artifact or touching `gh-pages`.

  **The rehearsal has now been dispatched from this repository too**, on 2026-09-23 while validating
  `#114`, and it re-measured `#69` against parent-poms `master` as it stood then: `versions:set`
  modified 1 of 13 `pom.xml` files. So `#69` was confirmed twice, from both sides of the boundary.
  That run also wrote nothing — 267 package versions before and after, RC branch intact, no `v0.3.0`
  tag.

  **This decision closes when `0.3.0` is released** and `#65` is confirmed or corrected against that
  run. `#69` no longer rides on it: it was the one that had to be fixed *before* the release rather
  than validated by it, and it was — `parent-poms#80`, merged 2026-09-24, proved by a rehearsal at a
  hotfix version the default version policy could not have produced.

  **`#65` is now built and merged too** (`parent-poms#82`, 2026-09-25), so this decision is down to
  its last clause: the real `0.3.0` release confirms, or corrects, what the rehearsals showed.
  `#117` removed the last blocker (PR #118, 2026-09-25): the release wrapper now passes
  `development_branch`, and a rehearsal through it reached and verified the merge-back.

  **Closed on 2026-09-29: the real `0.3.0` release confirmed `#65`.** A rehearsal first
  ([36604595994](https://github.com/MRISS-Projects/dsh/actions/runs/36604595994)) announced all 9
  write points exactly once and wrote nothing. Then the release
  ([36606687680](https://github.com/MRISS-Projects/dsh/actions/runs/36606687680)) merged `v0.3.0`
  into `DEVELOP` (`e6cccf502`, after `f0d1931e7` aligned it to `0.4.0-SNAPSHOT`). The RC's commits,
  315 of them not on `DEVELOP` before, are reachable from it, and `#69`'s check found all 13 modules
  at `0.3.1-SNAPSHOT` on `0.3.x`. The release did surface two defects outside `#65` and `#69`,
  both in its site: `parent-poms#95` and `#96` (Wave 1, upstream step 0). Both were fixed in 3.9.2
  and shipped by DSH 0.3.2.
- **The release path on GCP is first proven by the 0.6.0 release — accepted on 2026-10-10.** Wave 7
  builds the `dsh-staging` deploy, the `dsh` deploy, the commit gate and the production smoke
  check. None of them can run for real before a release candidate and a release exist to run them.
  The deploy itself is proven earlier, by every `dsh-dev` deploy from Wave 4 on, because the three
  tiers are three runs of one deploy. What stays unproven until 0.6.0 is the gate, the production
  run and the smoke check against `dsh`.
- **Until 0.6.0, a released DSH keeps documents in memory only — accepted on 2026-10-13, with
  ADR-003.** Wave 1 task 8 removes MongoDB, and Firestore arrives in Wave 5. So 0.4.0 and 0.5.0
  lose every document at a restart, and a file in the local directory can outlive its record
  (ADR-003, section 8). It is accepted because no deployment exists before Wave 4 and none holds
  data. The alternative was MongoDB kept selectable until Firestore exists, and that means
  writing MongoDB code for the conditional advance and for the term statistics, which a later
  wave deletes.

  The `0.6.0` milestone exists to bound that cost. A defect found on the first run costs a patch
  release of 0.6.0, not of 1.0.0. Anything the run misses is fixed then, from the hotfix line if it
  has to be, as was done for 0.3.0.

  Two costs come with it:

  - **A production deployment exists from 0.6.0**, two milestones before the end-user packaging.
    Under the cost rule in `CLAUDE.md` it is billed for what it stores and for nothing while idle.
  - **`dsh-staging` stays deployed between releases**, because the gate reads the revision that
    passed. The same rule makes that affordable.

  **This decision closes when 0.6.0 is released** and the run confirms, or corrects, what Wave 7
  built.

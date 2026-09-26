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
dependencies and no code marked `@Deprecated`. Waves 1-5 below describe the work ADR-001 proposes,
not work already in progress.

## 2. How to read a wave

- Waves are **ordered**. A task belongs in the earliest wave whose dependencies it satisfies.
- Each task listed under a wave is meant to become one INVEST story via `dsh-new-story` (step 2 of
  the process) — independent, negotiable, valuable, estimable, small, and testable. A task too
  large for one task branch gets split into more than one story at that point, not written as a
  single oversized story.
- An issue number next to a task (e.g. `#48`) means a GitHub issue exists for it and should be
  referenced from the resulting story. Wave 0's items all have issues; waves 1-6 mostly do not yet,
  and their tasks become issues via `dsh-new-story` when they are picked up.
- Where an item has an issue, **the issue is the source of truth** for its rationale and acceptance
  criteria — this document should not restate them, so the two cannot drift apart.
- Where a wave lists its issues in a table, the `Status` column carries only what GitHub says:
  `open`, or `closed` naming the pull request that delivered it. It is maintained by step 8
  (`dsh-reconcile-prd`) after a merge, never hand-edited ahead of one. Titles in that column are
  the GitHub titles, normalised to sentence case with any `[STORY]` prefix dropped.
- Waves 1-5 mirror the five migration phases in ADR-001 §4 one-for-one. Their task lists are drawn
  directly from those phase tables — no tasks were invented here that ADR-001 does not already
  enumerate.

## 3. Wave-to-milestone mapping

| Wave | Milestone |
|---|---|
| 0 | `0.3.0-SNAPSHOT` |
| 1 | `0.4.0-SNAPSHOT` |
| 2 | `0.4.0-SNAPSHOT` |
| 3 | `0.4.0-SNAPSHOT` |
| 4 | `1.0.0-SNAPSHOT` |
| 5 | `1.0.0-SNAPSHOT` |
| 6 | `1.0.0-SNAPSHOT` |

These three milestones already exist in GitHub. `dsh-new-story` sets `--milestone` on
`gh issue create` from this table, keyed by the wave the task came from.

## 4. The waves

### Wave 0 — Engineering foundation

Milestone: `0.3.0-SNAPSHOT`. Housekeeping and build-health work that has no dependency on the GCP
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
| `#87` | open | Update Maven pinned version from 3.9.9 to 3.9.16 in documentation and GitHub Actions |
| `#43` | open | Configure surefire, jacoco and other useful reports for the maven generated docs |
| `#46` | open | Implement integration tests using embedded tomcat server |
| `#70` | open | Project link not working at maven generated site |
| `#90` | open | index.html missing from published site on gh-pages (root + all submodules) |
| `#92` | **closed** — PR #96 | Remove the dead Travis build estate |
| `#93` | **closed** — PR #102 | Remove the redundant coverage ratchet — `jacoco:check` at 95% is already inherited |
| `#94` | **closed** — PR #110 | Make `check-spec-references` enforcing, or remove it |
| `#95` | **closed** — PR #98 | Use a read-only token for CI package authentication |
| `#97` | **closed** — PR #107 | Resolve the tooling orphaned by the Travis estate removal |
| `#99` | **closed** — PR #100 | Standardise Maven builds on `-U` while the parent is a SNAPSHOT |
| `#101` | **closed** — PR #106 | Fail CI when the package token cannot authenticate, not just when it is absent |
| `#103` | **closed** — PR #105 | Make PR review rounds repo-aware and authoritative |
| `#104` | open | Regenerate the coverage badge, or stop publishing a stale one |
| `#111` | **closed** — PR #116 | Let `release.yml` and `hotfix.yml` dispatch a release rehearsal |
| `#112` | **closed** — PR #121 | Reclassify the Spring-context tests as integration tests and pay the unit-coverage bill |
| `#113` | open | Remove the dead `main` branch trigger from `api-testing.yml` and `documentation-sync.yml` |
| `#114` | **closed** — PR #116 | Release and hotfix wrappers do not supply the build properties DSH's reactor needs |
| `#115` | open | `version.properties` ships an unresolved `${jenkins.build.number}` in two modules |
| `#117` | **closed** — PR #118 | Pass `development_branch` to the release and hotfix wrappers |
| `#122` | open | Close the file streams that test fixtures leave open |

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
preserved the reporting path the badge belongs to — the two touch the same module and nothing else.

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
affected test files lie outside that story's scope.

`#46` was written up in the same session rather than retitled. It is the *other* half of `#112`,
and the two are not the same shape: `#112` moves in-process Spring-context tests, while `#46` binds
`spring-boot:start` and `spring-boot:stop` to `pre-integration-test` and `post-integration-test` so
integration tests run against the packaged artifact over real HTTP. Its title was accurate all
along; what it lacked was a body, which it had never had. Scoping it turned up that
`api-testing.yml` is a green no-op — `specs/api/postman/` holds only a `README.md`, so the Postman
step skips and the job passes after building the reactor and booting the application to assert
nothing.

`#113` came out of that same scoping and was deliberately not folded into `#46`. Two workflows
trigger on pushes to a branch named `main`, which this repository has never had; a dead trigger has
nothing to do with the integration-test lifecycle, and bundling them would have put a one-line
cleanup behind a story with an open design question.

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
| `3.9.0-SNAPSHOT` | `#59`, `#70`, `#78` | Clear, then release **3.9.0** |
| `3.10.0-SNAPSHOT` | `#74`, `#81` | Opened 2026-09-20 to hold deferred work. Does **not** gate Wave 0 |

**Half of this goal is done.** `parent-poms#13` was the last issue on `3.8.0-SNAPSHOT`; it was
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
on it: `#112` has since shipped on it, and `#46` is unblocked, since this repository already names
`3.9.0-SNAPSHOT`. Closing `#67` does not advance Wave 0's own condition, which is the **3.9.0
release** and the re-pin. That still waits on the three issues left on the milestone: `#59`, `#70` and `#78`.

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
    change into an open-ended redesign. Until it lands, `staging.yml` names `dshuser` and `dshpass`
    twice — once for the build, once for the setup step.
  - **`#115`** (above) — `version.properties` ships `${jenkins.build.number}` unresolved in
    `dsh-data` and `dsh-rest-api`. Found by `#114`'s placeholder sweep, which is the only reason
    anyone looked: nothing defines that property, no Java reads the file, and there is no Jenkins.
    It predates `#114` and has nothing to do with supplying `mongo.*`, so `#114`'s AC003 was
    narrowed to `mongo.*` and `#115` carries the general form.

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

- `parent-poms#70` — convert the remaining 15 APT site pages to Markdown. `#85`'s two pages were
  the pilot; this finishes the format migration, with `doxia-module-apt` leaving
  `maven-site-plugin`'s dependency list as the completion criterion. It was not folded into `#57`
  because a 15-file migration on `3.8.0-SNAPSHOT` would push the 3.8.0 release further out, which
  is the opposite of why `#85` was picked up first. The 39 APT files under
  `src/main/resources/archetype-resources/` are deliberately excluded — they are template content
  shipped into new projects, so converting them is a separate decision.
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
Parent-poms `#13` (image links broken in the generated Maven site) is **closed**,
fixed and released in 3.8.0. It was adjacent to DSH `#70` and `#90`, and that adjacency has now
been tested for one of the two: a staging run on `staging-0.3.0-SNAPSHOT-RC` against the released
`3.8.0` shows `#90` reproducing unchanged — root and every module still missing `index.html`, the
live URL still 404 — so **`#90` is not a symptom of `#13`** and stands on its own diagnosis.
`#70` was not exercised by that run and remains unverified.

The root `pom.xml`'s SNAPSHOT parent pin is a related, but deliberately *not* actionable, item
**until the above completes** — see §6.

### Wave 1 — ADR-001 Phase 1: interface extraction and deprecation

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 1 — Interface Extraction &
Deprecation". This wave's theme legitimately involves deprecation as the work to be performed —
marking current implementations `@Deprecated` as new interfaces are extracted — not a claim that
anything is deprecated today.

**Triaged issues:**

- `#48` — Investigate how to use profiles (dev, staging, production) with Spring and Maven. This
  underpins the `@Profile`-based selection ADR-001 Phase 1 calls for throughout.
- `#119` — Migrate every test to JUnit 5 and drop the vintage engine.
- `#120` — Upgrade Spring Boot to a supported line, version chosen by analysis.

  Like `#48`, neither is an ADR-001 phase task. Both were raised on 2026-09-25, while
  specifying `#112`, and they are ordered: `#119` lands first, because Spring Boot 3's
  `spring-boot-starter-test` drops the vintage engine and JUnit 4 tests would stop running
  without failing anything. `#120` belongs in this wave rather than later because Wave 2's
  `spring-cloud-gcp-starter-*` release line is tied to the Spring Boot line, and choosing Boot after
  the GCP code exists would mean migrating that code twice. Neither issue names a target version.
  Both leave it to their spec's analysis, and for `#120` that includes whether the version moves in
  parent-poms `products/pom.xml`, where it is managed today for every product.

**Tasks (ADR-001 §4 Phase 1):**

1. `dsh-data` — Create `DocumentPersistenceRepository` interface. Wrap the existing
   `DocumentRepository` (Mongo) as `MongoDocumentPersistenceRepository implements
   DocumentPersistenceRepository`, marked `@Deprecated`.
2. `dsh-data` — Refactor `MongoDocumentDao` to depend on `DocumentPersistenceRepository` instead of
   `DocumentRepository` directly. Mark `MongoDocumentDao` `@Deprecated`.
3. `dsh-rest-api` — Convert `enqueue-docId-context.xml` / `dequeue-docId-context.xml` to Java
   `@Configuration` classes. Create `RabbitMqIntegrationConfig` (`@Profile("rabbitmq")`), marked
   `@Deprecated`.
4. `dsh-rest-api` — Mark `DocumentQueueServiceImpl` (RabbitMQ) `@Deprecated`, assign
   `@Profile("rabbitmq")`.
5. `dsh-solr` — Extract a `TermVectorOrderingService` interface from
   `OrderedTermVectorComponent`'s sorting logic (accepts term-vector entries and order options,
   returns the sorted list). Mark the Solr adapter `@Deprecated`.
6. `dsh-solr` — Evaluate `AdvancedNumberFilter`. If expressible as a pre-indexing step, extract a
   `NumberFilterService` interface. Mark the Solr implementation `@Deprecated`.
7. `dsh-data` — Refactor the `Document` entity: extract `byte[] originalFileContents` into a
   `FileStorageService` interface with `store(byte[]) → URI` and `retrieve(URI) → byte[]`. Create
   `LocalFileStorageService` (deprecated, for backward compatibility); `GcsFileStorageService`
   follows in Wave 2.

### Wave 2 — ADR-001 Phase 2: Firestore + GCS

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 2 — GCP Implementation:
Firestore + GCS (MongoDB Replacement)". No existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 2):**

1. `dsh-data` — Add `spring-cloud-gcp-starter-data-firestore` and
   `spring-cloud-gcp-starter-storage` dependencies.
2. `dsh-data` — Create `FirestoreDocumentPersistenceRepository implements
   DocumentPersistenceRepository`. Map `Document` fields to Firestore collections.
3. `dsh-data` — Create `FirestoreDocumentDao implements DocumentDao` (`@Profile("gcp")`). Wire to
   `FirestoreDocumentPersistenceRepository`.
4. `dsh-data` — Create `GcsFileStorageService implements FileStorageService` (`@Profile("gcp")`).
   Upload/download file bytes to a GCS bucket; the `Document` entity stores a `fileStorageUri`
   instead of raw bytes.
5. `dsh-data` — Write a migration utility to bulk-copy existing MongoDB documents to Firestore and
   upload `originalFileContents` blobs to GCS, replacing the field with the resulting URI.

### Wave 3 — ADR-001 Phase 3: Cloud Pub/Sub

Milestone: `0.4.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 3 — GCP Implementation: Cloud
Pub/Sub (RabbitMQ Replacement)".

**Triaged issue:** `#49` — re-scoped. Originally titled "Create docker structure based on docker
files to have all servers configured as docker containers to run tests and/or the application" —
i.e., Docker containers for MongoDB/RabbitMQ/Solr. It is re-scoped to **Firestore and Pub/Sub
emulators** for local and CI testing, which is why it sits in Wave 3 (alongside the Pub/Sub work
it will exercise) rather than Wave 0.

**Tasks (ADR-001 §4 Phase 3):**

1. `dsh-rest-api` — Add `spring-cloud-gcp-starter-pubsub` and `spring-integration-gcp`
   dependencies.
2. `dsh-rest-api` — Create `PubSubIntegrationConfig` (`@Profile("gcp")`) — defines a
   `PubSubTemplate` and outbound channel adapter to a `document-tasks` topic.
3. `dsh-rest-api` — Create `PubSubDocumentQueueServiceImpl implements DocumentQueueService`
   (`@Profile("gcp")`). Publishes document IDs to the Pub/Sub topic.
4. `dsh-doc-indexer-worker` — Replace `spring-boot-starter-amqp` with
   `spring-cloud-gcp-starter-pubsub`. Create a Pub/Sub subscriber (pull or push via a Cloud Run
   HTTP endpoint) that invokes the existing processing pipeline.
5. `dsh-doc-analyser` / `dsh-doc-processor-worker` — Replace the `spring-boot-starter-amqp`
   dependency. Wire a Pub/Sub subscriber analogously to task 4.

### Wave 4 — ADR-001 Phase 4: Vertex AI Search

Milestone: `1.0.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 4 — GCP Implementation: Vertex AI
Search (Solr Replacement)". No existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 4):**

1. `dsh-solr` (or a new `dsh-search` module) — Add `google-cloud-discoveryengine` dependency.
2. New module / `dsh-solr` — Create `VertexAiSearchIndexingService`, indexing document content in
   a Vertex AI Search data store via the Document AI / ingestion APIs.
3. New module / `dsh-solr` — Create `VertexAiSearchQueryService`, executing full-text search
   queries via the Vertex AI Search serving API.
4. **`OrderedTermVectorComponent` migration.** Vertex AI Search does not expose raw term vectors
   (TF, DF, TF-IDF) the way Solr's `TermVectorComponent` does:
   - At indexing time, compute TF/DF/TF-IDF statistics in the application layer (Java) from the
     extracted document text, before sending it to Vertex AI Search. Store these statistics as
     structured metadata on the Vertex AI Search document or in Firestore.
   - Extract the sorting logic currently in `SortedNamedList` / `TermsVectorComparator` / `Order`
     into the new `TermVectorOrderingService` implementation (`VertexAiTermVectorOrderingService`).
     It retrieves the pre-computed term statistics from Firestore or Vertex AI Search metadata and
     applies the same ascending/descending sort by the requested field.
   - The `order` query parameter format (`order=tv.tf;desc`) is preserved in the REST API;
     `dsh-rest-api` delegates to `TermVectorOrderingService`, now infrastructure-agnostic.
   - `Order` (enum), `OrderOptions` (POJO), and `TermsVectorComparator` (a generic
     `Comparator<Object>` depending only on `OrderOptions`) are reusable as-is. Only
     `OrderedTermVectorComponent` and `SortedNamedList` are Solr-coupled and need replacement.
5. **`AdvancedNumberFilter` migration.** Evaluate Vertex AI Search's built-in
   tokenisation/filtering. If number filtering is not natively supported, implement a
   `NumberFilterPreProcessor` in the indexing pipeline that strips or normalises numeric tokens
   before content is sent to Vertex AI Search.

### Wave 5 — ADR-001 Phase 5: validation and cutover

Milestone: `1.0.0-SNAPSHOT`. Tasks drawn from ADR-001 §4, "Phase 5 — Validation & Cutover". No
existing GitHub issue was triaged into this wave.

**Tasks (ADR-001 §4 Phase 5):**

1. Run both profiles (`rabbitmq`/`mongodb` and `gcp`) in parallel in a staging environment.
   Compare results for functional parity.
2. Execute integration tests from `specs/testing/test-plans/` against the `gcp` profile.
3. Performance-test against benchmarks in `specs/testing/performance-benchmarks/` (FR004: 95% of
   documents under 10 MB processed within 30 s).
4. Switch the default Spring profile to `gcp`. Deprecated implementations remain available via
   `@Profile("legacy")`.

### Wave 6 — Product backlog

Milestone: `1.0.0-SNAPSHOT`. Existing backlog issues with no dependency on the GCP migration,
scheduled after it so the migration lands first.

**Triaged issues:**

- `#44` — Add architecture and components diagrams and description following the three
  architecture patterns at content.pivotal.io/blog/agile-architecture
- `#45` — Document, configure and test Spring Boot actuators for the REST API module
- `#50` — Add extra Swagger documentation using annotations
- `#51` — Investigate and add support for `spring-boot-starter-hateoas`
- `#52` — Improvement for performance: file hash generation could be another service. Reviewed and
  **kept**: the issue mentions Mongo only as one option for where to generate the hash, and the
  file-hash-as-a-service idea itself is independent of the persistence backend, so it survives the
  migration.
- `#53` — Make the `DocumentStatus` enumeration dynamic by reading descriptions and messages from
  properties files, per locale

## 5. Won't-fix

Two issues were superseded by the ADR-001 migration itself and will not be built as written. Both
were **closed as `not planned` on 2026-09-16** by the repo owner, each with a comment naming the
wave that supersedes it. `scripts/close-wontfix-issues.sh` records exactly what was run.

| Issue | Reason | Superseded by |
|---|---|---|
| `#65` (closed) — Implement indexer-worker daemon | Its body specifies enqueuing via RabbitMQ and storing results in Solr — both surfaces this migration replaces. | Wave 3 (Cloud Pub/Sub) and Wave 4 (Vertex AI Search) |
| `#47` (closed) — Mongo DAO ordering by timestamp | Targets `MongoDocumentDao`, which ADR-001 Phase 1 wraps and Phase 2 replaces with a Firestore-backed implementation. Ordering behaviour belongs on the new repository, not the one being replaced. | Wave 2 (Firestore + GCS) |

`#52` is explicitly **not** in this table — see Wave 6 above for why it was reviewed and kept.

## 6. Known risks / accepted decisions

- **SNAPSHOT parent pin — accepted deliberately, not an oversight.** The root `pom.xml`
  intentionally tracks `com.mriss.mriss-parent:products:3.9.0-SNAPSHOT`. A SNAPSHOT parent
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
- **The coverage badge is stale, and nothing regenerates it.** The committed badge
  `dsh-coverage-report/badges/jacoco.svg` reads 92%, while CI's JaCoCo aggregate computes 98.13%
  against a 2,028-instruction denominator, which is the whole codebase, not a partial one — see
  Wave 0's note on aggregate scope.

  Earlier drafts of this risk left open whether the two measure different scopes or the badge had
  simply gone stale. **It is staleness, and that is now settled.** The badge is configured with
  `<metric>instruction</metric>` against `jacoco-aggregate/jacoco.csv` in
  `dsh-coverage-report/pom.xml` — the same counter and the same file the aggregate figure comes
  from, so the two cannot legitimately disagree. The file's only commit is the April 2026
  directory-casing rename, so its content is older than that.

  The cause is not an infrequent pipeline run. The `process-badges` profile is
  `activeByDefault=false` with no property activation, so it is reachable only with
  `-P process-badges` — and no workflow in this repository passes `-P` at all, nor does any
  reusable workflow in `parent-poms`. The only occurrences in the tracked tree are historical
  planning documents. So the profile was orphaned by the estate's deliberate move away from `-P`
  (see `parent-poms`' CLAUDE.md: "never reintroduce `-P`"), not by scheduling. Running
  `staging.yml` more often would not fix it.

  Whoever picks this up should also expect the profile's `maven-scm-plugin` `checkin` execution,
  which commits the badge back to the repository from inside the build — a git write needing
  credentials, and one that interacts with the `[skip ci]` conventions.

  **`#93` closing does not close this.** That story removed the second coverage *gate* and
  deliberately left the `dsh-coverage-report` aggregation, the badge and CI's uploaded artifact
  untouched — the reporting path is exactly what this risk concerns.

  **Now tracked by `#104` in Wave 0**, which carries the full diagnosis and the two side-findings
  that came with it: the badge's `<passing>70</passing>` threshold contradicts the real 95% gate,
  and `maven-scm-plugin` is pinned locally to `1.9.5` against `parent-poms`' managed `2.1.0`. This
  entry should be deleted when `#104` ships — `#104`'s AC005 requires that — since a diagnosed
  defect with an owner is work in a wave, not a standing risk.

---
issue: 114
slug: supply-build-properties-to-release-wrappers
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
also_closes: 111
---

# Story 114 — Supply build properties to the release, hotfix and staging wrappers

## 1. Story

**As a** release manager cutting a DSH release
**I want** this repository's wrappers to supply the build properties its own reactor needs
**So that** `release:prepare` completes instead of dying inside its forked `clean install`

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#114](https://github.com/MRISS-Projects/dsh/issues/114)
- **Also closes [#111](https://github.com/MRISS-Projects/dsh/issues/111)** — see §2.1
- Parent branch: `staging-0.3.0-SNAPSHOT-RC` — matches the `0.3.0-SNAPSHOT` milestone on both
  issues, and follows `#92`, `#95`, `#97`, `#99`, `#101` and `#103`, which all shipped onto the
  same RC. `DEVELOP` is at `0.4.0-SNAPSHOT`.
- Upstream half: [`parent-poms#76`](https://github.com/MRISS-Projects/parent-poms/issues/76),
  widened by this spec — see §5 and §6
- Found while building [`parent-poms#72`](https://github.com/MRISS-Projects/parent-poms/issues/72),
  closed by its PR #77 on 2026-09-22

### 2.1 Why `#111` is folded in

`#114` needs one thing to prove itself: a release run that reaches the end of
`release:prepare`. DSH cannot dispatch one. `project-release.yml` and `project-hotfix.yml` are
`workflow_call` only, so the rehearsal `parent-poms#72` just built is unreachable from this
repository until `#111` adds the `dry_run` passthrough — to the same two wrapper files this
story edits, for a different input.

Three options were weighed with the owner: sequence `#111` first and edit the wrappers twice;
build both under this branch; or ship `#114` with only a local dry run as evidence. **The owner
chose the second.** The dispatched rehearsal is the expensive artifact, and one run proves
`#114` AC001 and `#111` AC004 together.

`#114`'s own issue body already anticipated this — "different input, same file, and they should
probably be built together."

**Consequence for `dsh-ship-story`:** one PR, `Refs #114` and `Refs #111`, and both issues
close on merge. That is a deviation from one-story-one-PR and is recorded here deliberately.
`#111`'s acceptance criteria are restated verbatim in §10 so nothing is lost by not giving it
its own spec file.

## 3. The defect

`.github/workflows/release.yml:25-31` and `.github/workflows/hotfix.yml:13-15` pass `git_project`
and the version inputs and nothing else.

`dsh-data/src/main/resources/mongo.properties` is four lines, every one of them a filtered
placeholder:

```properties
mongo.host=${mongo.host}
mongo.port=${mongo.port}
mongo.user=${mongo.user}
mongo.password=${mongo.password}
```

`.github/workflows/ci.yml:179-182` supplies all four from the `github-packages` profile of the
`settings.xml` it writes. `project-release.yml` writes its own `settings.xml`
(`origin/master:.github/workflows/project-release.yml:89-153`) and defines none of them.

With the property undefined, `maven-resources-plugin` leaves the text literal and every
`dsh-rest-api` Spring context fails with `Circular placeholder reference 'mongo.port' in property
definitions`. The reactor dies at `dsh-rest-api`, inside `release:prepare`'s `<preparationGoals>`
fork, at phase 10 of 17 —
[run 35650261302](https://github.com/MRISS-Projects/parent-poms/actions/runs/35650261302).

Supplying the four properties and changing nothing else took the identical command through all 17
phases — [run 35651368119](https://github.com/MRISS-Projects/parent-poms/actions/runs/35651368119).

**No live MongoDB is involved.** The failure is property resolution, not connectivity: the bean is
defined but never connected during these tests, and `dsh-data`'s own 51 tests passed in both runs.

## 4. The measurement that settles `#114`'s open question

`#114` AC004 asks for the (a)/(b)/(c) decision "recorded with the evidence that settled it", and
both issues flag one assumption that had to be measured: whether an active settings-profile
property still beats a POM `<properties>` default. If it did not, giving DSH's POM defaults would
silently shadow `ci.yml`'s live-Mongo values.

**Measured on 2026-09-22**, Maven 3.9.9 (the version CI pins via `stCarolas/setup-maven` in every
workflow), throwaway single-module project, `${mongo.port}` filtered into a resource:

| Where the value came from | Filtered result |
|---|---|
| POM `<properties>` default + active settings-profile property | `FROM_SETTINGS` |
| POM `<properties>` default, settings defining nothing | `POM_DEFAULT` |
| Both, plus `-Dmongo.port=FROM_CLI` | `FROM_CLI` |

Reproducible with a `pom.xml` carrying `<mongo.port>POM_DEFAULT</mongo.port>`, a filtered
`src/main/resources/t.properties` containing `mongo.port=${mongo.port}`, and a `settings.xml`
whose active `github-packages` profile sets `<mongo.port>FROM_SETTINGS</mongo.port>`:

```bash
mvn -B -s settings-with-prop.xml clean process-resources && cat target/classes/t.properties
mvn -B -s settings-empty.xml     clean process-resources && cat target/classes/t.properties
mvn -B -s settings-with-prop.xml -Dmongo.port=FROM_CLI clean process-resources && cat target/classes/t.properties
```

So precedence is **command line > active settings profile > POM `<properties>`**, and option (b)
from the issue body is technically safe: POM defaults would never shadow anything.

**The decision is nonetheless (a) — properties supplied by this repository's wrappers, no POM
defaults.** Two reasons, one from the owner and one from the measurement:

1. **Infrastructure supplies the pipe, the project supplies the content.** Owner's call, taken
   during this spec's brainstorm. parent-poms must never carry a default for a consumer's
   property, and each project is responsible for passing the properties that are live when it
   calls a reusable workflow. That principle also decides §5.2.
2. **A POM default would never be the value in any real build.** The table above shows settings
   always wins, and every path that builds DSH today already supplies these four names — see the
   inventory in §5.1. A default's only effect would be to mask a missing configuration with a
   fabricated `localhost:27017`, in a file that ships in a released POM.

The gap a default would have covered — a fresh contributor whose `~/.m2/settings.xml` has no
`mongo.*` entries — predates this story and is documentation's job. §7.4 covers it.

## 5. Design

### 5.1 Who supplies the properties, after this story

| Path | How `mongo.*` arrives | Live Mongo behind it |
|---|---|---|
| Local `mvn install` | the developer's own `~/.m2/settings.xml` | the developer's |
| `ci.yml` | the settings profile it writes itself (`ci.yml:179-182`) | yes, service container |
| `staging.yml` | **changes** — the new generic input; two of the five `mongo_*` inputs go | yes, service container |
| `release.yml` / `hotfix.yml` | **new** — the generic input; nothing supplies them today | no, never connects |

`ci.yml` is untouched: it writes its own `settings.xml` and calls no reusable workflow.

### 5.2 Upstream: one generic input, and nothing named `mongo`

`parent-poms#76` gives `project-release.yml`, `project-hotfix.yml` **and** `project-staging.yml`
one input — `maven_properties`, a multi-line `name=value` block, default empty — rendered into
the `<properties>` of the `github-packages` profile in the `settings.xml` each of them writes.

Settings-level properties are the right carrier, and `-D` is not. `release:prepare` forks
`clean install` with only `<arguments>`, which `parent-poms/pom.xml:367` binds in
`<configuration>`, so `-Darguments=…` is inert — the user-property trap `parent-poms#69` also
hit. The fork inherits the same `~/.m2/settings.xml`, which is exactly why run 35651368119
completed all 17 phases where its predecessor died at phase 10.

Three mechanics matter, and all three are the upstream spec's to implement:

- **The heredoc stays quoted.** Each workflow writes its `settings.xml` with
  `cat > ~/.m2/settings.xml << 'SETTINGS_EOF'`. Unquoting it to interpolate would also expand
  `${env.DEPLOY_TOKEN}` and `${env.GITHUB_ACTOR}`, which must survive into the file literally.
  The `<properties>` block therefore carries a marker comment that a following step replaces —
  and **deletes entirely when the input is empty**, which is what makes `#76` AC002's
  byte-identical claim true rather than approximate.
- **Input lines are validated, not trusted.** This input lands inside XML the workflow writes.
  Each non-empty line must match `^[A-Za-z0-9._-]+=` or the workflow fails loudly; values are
  XML-escaped for `&`, `<` and `>`.
- **It is one composite action, not three copies.** `.github/actions/` upstream already holds
  `commit-readme`, `rehearsal-setup`, `rehearsal-tag` and `rehearsal-verify`, each a small shell
  script beside a `*.test.sh` that runs under `sh`. The renderer follows that pattern, which
  gives the upstream half a genuine red-green cycle despite parent-poms having no Java source.

### 5.3 Upstream: what this story widens, and what it refuses to

`#76` as written excludes `project-staging.yml`: "Collapsing `project-staging.yml`'s five
`mongo_*` inputs onto the new input. Staging also needs its service container, which cannot be
made generic the same way, and nothing needs that change yet."

Half of that is right. Split by what the generic input can actually carry:

| What is DSH-shaped in `project-staging.yml` | Where | Generic input fixes it? |
|---|---|---|
| `mongo_host`, `mongo_port` → `MONGO_FLAGS` | inputs 29-38, build step 196-208 | **Yes** — they configure the build and nothing else, which is the pipe `#76` builds |
| `mongo_user`, `mongo_password` | inputs 39-48, `MONGO_FLAGS` **and** the setup step | **Partly** — see below |
| `mongo_database` | input 49-53, used only by the setup step | No — an argument to a setup action, not a build property |
| "Create MongoDB user and database" (`docker run … mongosh … createUser`) | step 172-182 | No — consumer-specific setup logic |
| `services: mongodb: mongo:6` and `rabbitmq: rabbitmq:3-management`, hardcoded | job `services:` 68-76 | No — a reusable workflow owns the job, so a consumer cannot declare services into it |

So `#76` is widened to the build properties, and its "Out of scope" paragraph is rewritten rather
than deleted: staging's *properties* come in, staging's *services* stay out. Rows 3-5 become
[`parent-poms#78`](https://github.com/MRISS-Projects/parent-poms/issues/78), raised by Task 2.
They need a design (how does a consumer declare a service container to a job it does not own?),
they have no DSH deadline, and folding them into `#76` would turn a one-input change into an
open-ended redesign.

**Row 2 is why `#76` removes two inputs and not four.** Found while writing the upstream spec,
after `#76` had already been widened: `mongo_user` and `mongo_password` are read twice — once
into `MONGO_FLAGS`, and once by the user-creation step and its `if: inputs.mongo_user != ''`
guard. `#76` stops building `MONGO_FLAGS`, so their build use goes; their setup use cannot,
because the step that reads them is `#78`'s. Removing the inputs while keeping the step would
break the only consumer of it.

`#76` therefore deletes `mongo_host` and `mongo_port` outright, and leaves `mongo_user`,
`mongo_password` and `mongo_database` as arguments to setup logic rather than build
configuration — which is how `#76` AC003 is reworded, per that spec's §4.3 and Task 12. DSH's
`staging.yml` keeps passing all three until `#78` lands, so `dshuser` and `dshpass` are named
twice in that wrapper: once in `maven_properties` for the build, once as inputs for the setup
step. Visible, temporary, and cheaper than dragging `#78`'s redesign into this story.

### 5.4 No deprecation shim is needed

Removing four inputs from `project-staging.yml` breaks any caller still passing them: GitHub
fails a `workflow_call` given an input the callee does not declare. That would matter if
anything fired automatically. Nothing does — `stage.yml`, `staging.yml`, `release.yml` and
`hotfix.yml` are every one of them `workflow_dispatch` only, as
`docs/devops/README.md:72-74` records and §7 re-checks. The window between the upstream merge
and this repository's merge is therefore inert, and no transitional no-op inputs are added
upstream. DSH is also the only consumer of these workflows today.

### 5.5 The round trip ends short of a release, deliberately

`CLAUDE.md`'s full round trip ends by releasing parent-poms and re-pinning this repository's
root `pom.xml`. **This story does neither**, and this is a decision, not an omission:

- parent-poms' `3.9.0-SNAPSHOT` milestone still holds `#59`, `#65`, `#69`, `#70`, `#76` itself
  and the new issue from Task 2. `CLAUDE.md` is explicit: "Before releasing parent-poms, clear
  the milestone being released."
- DSH does not need the release. The wrappers reference the reusable workflows at `@master`, so
  a merge to parent-poms `master` is enough, and the root `pom.xml:9-12` already points at
  `3.9.0-SNAPSHOT`.
- The re-pin to a released `3.9.0` is already Wave 0's closing goal in `specs/product/PRD.md`
  §4, tracked there and not here.

This is also **not** the light round trip from `CLAUDE.md`. Three workflows, a new public input
and a new composite action are not "a change small enough to review in one sitting". `#76` gets
its own spec, branch, PR and milestone placement upstream.

## 6. The upstream half, in order

Tasks 1-6 happen in `MRISS-Projects/parent-poms`, not in this repository. They are listed here
because this story is blocked on them and because the round trip is what the owner asked for.

- [x] **Task 1 — widen `#76`.** Done 2026-09-22. The issue body now brings
      `project-staging.yml`'s four build properties into scope, keeps the service container and
      `mongo_database` out with `#78` named, answers the POM-default question with §4's evidence
      instead of deferring it, and adds AC005 for staging. Its title now names all three
      workflows. Per §5.3.
- [x] **Task 2 — raise the follow-up issue.** Done 2026-09-22 —
      [`parent-poms#78`](https://github.com/MRISS-Projects/parent-poms/issues/78), covering the
      `mongo:6` and `rabbitmq:3-management` service containers, `mongo_database`, and the "Create
      MongoDB user and database" step. Plain `task` issue; INVEST framing is not required there.
- [x] **Task 3 — milestone.** Done. `3.9.0-SNAPSHOT` is open and carries both `#76` and `#78`.
- [ ] **Task 4 — spec upstream.** Task branch `issue-76-consumer-supplied-maven-properties`, cut from
      a freshly fetched `master` — the local clone was two commits behind `origin/master` when this
      spec was written, and PR #77 is one of them. Spec at
      `specs/76-consumer-supplied-maven-properties.md`, matching `specs/67-*` and `specs/72-*`.
      `dsh-story-spec`'s refuse-`master` rule is DSH-scoped; upstream specs live on `master`.
- [ ] **Task 5 — implement upstream, red first.** The renderer script and its `*.test.sh` under
      `.github/actions/`, then the three workflows. Per §5.2.
- [ ] **Task 6 — validate from here, then merge.** With this repository's wrappers temporarily
      pointing at `@issue-76-consumer-supplied-maven-properties`, run the dispatches in §11. Record
      the deviation in the PR, re-pin to `@master` after the upstream merge, and re-run at least the
      staging dispatch against `@master`.

## 7. Files to change in this repository

### 7.1 `.github/workflows/release.yml`

Two inputs on `workflow_dispatch`, both passed through to `project-release.yml`:

- `dry_run` — `type: boolean`, `default: false`. `#111` AC001 and AC003. Its description names
  what a rehearsal does not do: no artifact deploy, no `gh-pages` publication, no push to
  `master`, no deletion of the RC branch.
- `maven_properties` is **not** a dispatch input. The four values are constants of this
  repository, so the wrapper passes them literally in its `with:` block, the way `staging.yml`
  passes `mongo_host: localhost` today. An operator dispatching a release must not be able to
  retype a database password into a form field.

The dispatch input, appended after `initial_hotfix_version` (line 20):

```yaml
      dry_run:
        description: >-
          Rehearse the release: no artifact deploy, no gh-pages publication, no push to master,
          no deletion of the RC branch. Every suppressed write is announced in the log.
        required: false
        type: boolean
        default: false
```

`type: boolean` renders as a checkbox in the Actions tab, and `project-release.yml` declares
`dry_run` as a boolean too (`origin/master:.github/workflows/project-release.yml:35-38`), so the
types match and no string coercion is involved.

The `with:` block gains both:

```yaml
      dry_run: ${{ inputs.dry_run }}
      maven_properties: |
        mongo.host=localhost
        mongo.port=27017
        mongo.user=dshuser
        mongo.password=dshpass
```

Same four values as `ci.yml:179-182`. Nothing connects to them on this path (§3), so they are
placeholders that satisfy resource filtering, not credentials.

### 7.2 `.github/workflows/hotfix.yml`

The same two changes, against `project-hotfix.yml`. `#111` AC002.

### 7.3 `.github/workflows/staging.yml`

The same `maven_properties` block is added, carrying all four `mongo.*` values. Of the five
`mongo_*` inputs, **two go and three stay** (§5.3): `mongo_host` and `mongo_port` (lines 16-17)
are deleted, because `#76` deletes the upstream inputs they feed; `mongo_user`, `mongo_password`
and `mongo_database` (lines 18-20) stay, because upstream they now feed only the user-creation
step that `parent-poms#78` will remove.

The wrapper therefore names `dshuser` and `dshpass` twice until `#78` lands — once in
`maven_properties`, once as inputs. Deliberate, and explained in a comment at the call site so
the next reader does not "fix" it by deleting one of them.

No `dry_run` here. `project-staging.yml` has no rehearsal mode, and `#111` scopes itself to the
release and hotfix wrappers.

### 7.4 `docs/devops/README.md`

- **Workflow Reference table, lines 72-74** — the three rows gain the new inputs. `staging.yml`'s
  row must no longer imply five `mongo_*` inputs; it now passes `maven_properties` plus three.
- **New subsection under `## Workflow Reference`** — "Rehearsing a release". `#111` AC006: that a
  rehearsal exists, how to dispatch one from the Actions tab, and that it is the intended step
  before a first release on any line. It names what a rehearsal does not do, matching AC003's
  list.
- **New subsection** — "How build properties reach a release build". The precedence table from
  §4, the four `mongo.*` names, and the rule that this repository supplies them while parent-poms
  supplies only the mechanism. This is also where the fresh-contributor gap from §4 is closed:
  a local `mvn install` needs those four names in the developer's own `~/.m2/settings.xml`, and
  this is the only place that says so.

### 7.5 `specs/product/PRD.md`

§4's Wave 0 table lists `#111` and `#114` as separate open rows, and the §6 paragraph on `#114`
still records its open question as open. Both need the decision from §4 and the pairing from
§2.1. `dsh-reconcile-prd` (step 8) owns this edit after the merge; it is listed here so the
reconciliation is not mistaken for drift.

## 8. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `.github/workflows/ci.yml` | Writes its own `settings.xml` and calls no reusable workflow. Its four `mongo.*` properties (179-182) are the live-Mongo values and keep winning, per §4. Untouched. |
| `.github/workflows/stage.yml` | Calls `project-stage.yml`, which runs `versions:set` and a commit — no `clean install`, so no filtered resource, so no property need. Confirmed by reading it, not assumed. |
| Root `pom.xml` and every module POM | No defaults, per §4's decision. The parent stays `3.9.0-SNAPSHOT`, per §5.5. |
| `dsh-data/src/main/resources/mongo.properties` | The four placeholders are correct. The defect is that one build path never defined them, not that the file is wrong. |
| `project-staging.yml`'s `mongo_database`, user-creation step and service containers | `parent-poms#78`, upstream. §5.3. |
| `.github/workflows/api-testing.yml` | Path-scoped to `dsh-rest-api/**` and `specs/api/**`, and invokes Maven directly rather than through a reusable workflow. This diff touches neither path. |

## 9. Implementation tasks, in this repository

Upstream Tasks 1-6 (§6) come first and are not repeated here. There is no Java change anywhere in
this story, so there is no unit test and TDD's red-green cycle does not apply to the DSH half —
the equivalent evidence is a reproduction that fails before the change and passes after, which
Task 7 performs locally and Task 12 performs in CI.

- [ ] **Task 7 — reproduce the failure locally, before changing anything.**
      **Run `clean install`, not `release:prepare`.** `<preparationGoals>` is `clean install`
      (`origin/master:pom.xml:371` upstream), so `clean install` *is* the command that died at
      phase 10 in run 35650261302 — and it reproduces the defect without `release:prepare`'s
      other preconditions, a clean working tree among them, which this branch does not have while
      the spec is uncommitted. A failure there would be the wrong failure.

      The reproduction needs a `settings.xml` that defines none of the four names. Derive one
      from the developer's own, **outside the working tree** — it carries their package token,
      and `.logs/` being gitignored is not a reason to put a credential inside the repository:

      ```bash
      mkdir -p .logs
      WORK="$(mktemp -d)"
      sed '/<mongo\./d' ~/.m2/settings.xml > "$WORK/settings-no-mongo.xml"
      grep -c 'mongo\.' "$WORK/settings-no-mongo.xml"   # must print 0

      mvn -B -s "$WORK/settings-no-mongo.xml" clean install > .logs/mvn-install-before.log 2>&1 &
      MVN_PID=$!
      echo "Monitor with:  tail -f .logs/mvn-install-before.log"
      wait $MVN_PID; echo "maven exit=$?"
      grep -n "Circular placeholder reference" .logs/mvn-install-before.log
      rm -rf "$WORK"
      ```

      Expected: non-zero exit, and the grep finds
      `Circular placeholder reference 'mongo.port'` in `dsh-rest-api`. **A green run means the
      reproduction is wrong, not that the defect is absent** — almost certainly the four names
      survived the `sed`, which the `grep -c` guards against. This is the run the reactor
      logging rule in `CLAUDE.md` exists for: 13 modules, redirect to `.logs/`, report the exit
      code explicitly, commit no log.

      Re-run the same command after Task 10 with the unedited `~/.m2/settings.xml` to show it
      green. That pair is this story's red-green: the DSH half touches no Java, so there is no
      unit test to write.

- [ ] **Task 8 — `release.yml`.** Add the `dry_run` dispatch input and the `maven_properties`
      `with:` block from §7.1. Commit.

- [ ] **Task 9 — `hotfix.yml`.** The same two changes against `project-hotfix.yml` (§7.2).
      Commit.

- [ ] **Task 10 — `staging.yml`.** Add `maven_properties`, delete `mongo_host` and `mongo_port`,
      and keep `mongo_user`, `mongo_password` and `mongo_database` with the comment explaining why
      two of them are named twice (§7.3). Commit.

- [ ] **Task 11 — documentation.** The three edits in `§7.4`. Run the markdown lint command from
      `CLAUDE.md`'s Commands table before committing.

- [ ] **Task 12 — prove it.** The dispatched runs in §11, links pasted into both issues. This is
      the task that closes `#114` AC001 and `#111` AC004, and it cannot start before upstream
      Task 6.

- [ ] **Task 13 — `#111` AC005's inputs diff.** Resolve the `with:` block of `release.yml` before
      and after, and show that a dispatch which leaves `dry_run` at its default sends the same
      values it sends today plus `maven_properties`. Checked by diffing the two resolved blocks,
      not by assertion.

## 10. Acceptance criteria

Restated from both issues. `#111`'s are reproduced verbatim in intent because it has no spec
file of its own (§2.1).

**From `#114`:**

- [ ] **AC001** — `mvn release:prepare` on DSH completes its `<preparationGoals>` fork with no
      `mongo.*` property on the command line. Proven by the dispatched rehearsal in §11, not by
      a local run alone.
- [ ] **AC002** — `ci.yml` and `staging.yml` still reach a live MongoDB with their own values.
      `ci.yml` is untouched; `staging.yml`'s dispatch in §11 must show its Mongo-dependent tests
      passing against the service container.
- [ ] **AC003** — No unresolved `mongo.*` placeholder survives resource filtering in any module,
      under either a plain `mvn install` or a release build. Checkable by sweeping
      `*/target/classes/**` for `${` after each run — **narrowed from "no unresolved `${...}`",
      see §11.1.**
- [ ] **AC004** — The decision between (a), (b) and (c) is recorded with the evidence that
      settled it. §4, and it resolves to (a).

**From `#111`:**

- [ ] **AC005** — `release.yml` accepts a rehearsal input on `workflow_dispatch` and passes it to
      `project-release.yml`, defaulting to a real release.
- [ ] **AC006** — `hotfix.yml` does the same for `project-hotfix.yml`.
- [ ] **AC007** — The input renders in the Actions tab with a description naming at least: no
      artifact deploy, no `gh-pages` publication, no push to `master`, no deletion of the RC
      branch.
- [ ] **AC008** — Demonstrated by a dispatched rehearsal against this repository, its log linked
      from `#111`, showing the upstream marker lines and that `staging-0.3.0-SNAPSHOT-RC` still
      exists afterwards.
- [ ] **AC009** — A real release dispatched without the rehearsal input produces the same
      workflow inputs it does today, plus `maven_properties`. Checked by diffing the resolved
      `with:` block (Task 13).
- [ ] **AC010** — `docs/devops/README.md` records that a rehearsal exists, how to dispatch one,
      and that it is the intended step before a first release on any line.

**Shared:**

- [ ] **AC011** — CI is green on the pull request. No `.java` file appears in the diff, so the
      coverage gate is unaffected.
- [ ] **AC012** — Markdown lint passes over the changed documentation, using the command in
      `CLAUDE.md`'s Commands table.

## 11. Verification runs

In cost order. Each one proves something the cheaper one cannot.

1. **Local dry-run prepare, before and after** (Task 7, then again after Task 10 with a
   settings file carrying the four names). Decisive for the mechanism in §3, and free. It does
   **not** prove the workflow, because it does not exercise the `settings.xml` the reusable
   workflow writes.
2. **One dispatched `staging.yml` run.** The only path where a live Mongo is actually connected,
   so it is the real proof of AC002 — and the strongest end-to-end proof of the upstream
   renderer, because a failure to render would surface as failing Mongo-dependent tests rather
   than as a missing placeholder. Run it against the upstream task branch first (§6 Task 6) and
   again against `@master` after the upstream merge.
3. **One dispatched `release.yml` rehearsal**, `dry_run: true`. Proves `#114` AC001 and `#111`
   AC004/AC008 in a single run. Confirm afterwards that `staging-0.3.0-SNAPSHOT-RC` still exists
   and that the upstream `assert-no-writes` verification passed.

A `hotfix.yml` rehearsal needs a `0.3.x` branch, which does not exist until `0.3.0` is released.
`#111` AC002/AC006 is therefore verified by inspection plus the resolved-inputs diff, and the
first real hotfix run is the confirming one. Stated here so the gap is a recorded decision rather
than a missing run.

### 11.1 What the local runs proved, and the one thing they found

**Red, before any change.** `mvn -B -s <settings without the four names> clean install`:
`BUILD FAILURE` at `dsh-rest-api`, 15 errors, 105 occurrences of
`Circular placeholder reference 'mongo.port' in property definitions`. The reactor reached the
module that fails and failed there, which is the defect in §3 reproduced exactly.

**Green, after Task 10**, same command with the developer's own settings: `BUILD SUCCESS`,
13/13 modules, zero occurrences, coverage gate included because it is bound to `verify`. The
`mongo.*` placeholders resolve in `dsh-data/target/classes/mongo.properties`.

**AC003 had to be narrowed.** The sweep for unresolved `${...}` found two files this story does
not touch:

```text
dsh-data/target/classes/version.properties
dsh-rest-api/target/classes/version.properties
```

Both carry `jenkins.build.number=${jenkins.build.number}`. Nothing in this repository or in
`parent-poms` defines that property, no `.java` file reads `version.properties`, and there is no
Jenkins — it is residue of a build system this project no longer has, shipped literally into
`target/classes` on every build. It predates this story, is unrelated to supplying `mongo.*`, and
is the same family as `#92` and `#97`.

Raised as [`#115`](https://github.com/MRISS-Projects/dsh/issues/115), whose AC004 restores this
story's AC003 to its general form once the placeholder is resolved or the file is removed. AC003
here is narrowed to `mongo.*` so it states what this story actually controls — the alternative
was either deleting two orphaned lines inside an unrelated PR, or leaving an AC that cannot pass.

**One rule was broken while gathering this evidence,** and it is recorded because the spec is
where a future reader looks for how the numbers were obtained: the filtered `mongo.properties`
was printed in full, which put the developer's local Mongo password into the session transcript.
Read property *names* when checking filtering — `grep -o '^[a-z.]*='` — never the resolved file.

## 12. Issue reconciliation

Three issue bodies drift from this spec and are corrected when the PRs open:

| Issue | Correction |
|---|---|
| `parent-poms#76` | Widened per §5.3 — staging's two build-only inputs out, service container and `mongo_database` out, new AC for staging, cross-link to `#78`. |
| `dsh#114` | The open question in "Open question for the spec to settle" is answered: (a), with §4's evidence. The blocking-dependency note stays true. |
| `dsh#111` | Note that it is built and closed under `#114`'s branch and PR, with the reason from §2.1. |
| `dsh#114` AC003 | Narrowed to `mongo.*`, with `#115` raised for the `jenkins.build.number` residue the original sweep found — §11.1. |

## 13. Out of scope

- **Service containers and Mongo user creation in `project-staging.yml`.** `parent-poms#78`, the new upstream
  issue. §5.3 says why a `name=value` input cannot reach them.
- **Releasing parent-poms and re-pinning the root `pom.xml` to `3.9.0`.** §5.5. Wave 0's closing
  goal, tracked in `specs/product/PRD.md` §4.
- **POM defaults for `mongo.*`.** Decided against in §4. If a future contributor wants the
  fresh-checkout convenience, the documented settings entries in §7.4 are the supported route.
- **`stage.yml`'s missing passthroughs**, and `release.yml`'s missing `site_deployment_url`.
  Real gaps, both noted in `#111`'s body, neither caused nor fixed here. Raise separately.
- **Migrating `mongo.properties` to Spring's own externalised configuration**, which would remove
  the resource-filtering dependency altogether. A larger change, and one that ADR-001's Firestore
  migration may moot.
- **Performing the `0.3.0` release.** The owner's call, and the end of Wave 0.

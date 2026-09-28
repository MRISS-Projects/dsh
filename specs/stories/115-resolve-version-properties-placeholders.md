---
issue: 115
slug: resolve-version-properties-placeholders
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 115 — Delete the orphaned `version.properties` from `dsh-data` and `dsh-rest-api`

## 1. Story

**As a** developer reading a built artifact's `version.properties`
**I want** every filtered placeholder in it to resolve to a real value
**So that** a shipped resource states facts rather than carrying a token nothing defines

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md` §4), milestone `0.3.0-SNAPSHOT`
- Issue: [#115](https://github.com/MRISS-Projects/dsh/issues/115), label `bug`
- Parent branch: `staging-0.3.0-SNAPSHOT-RC`. It matches the milestone, and `#122`, `#123` and
  `#124` were cut from and merged into the same RC.
- Found by `#114`'s AC003 sweep, which was narrowed to `mongo.*` because of it —
  `specs/stories/114-supply-build-properties-to-release-wrappers.md` §11.1.

Two identical files are filtered into `target/classes` and from there into each module's jar:

- `dsh-data/src/main/resources/version.properties`
- `dsh-rest-api/src/main/resources/version.properties`

```properties
version=${project.version}
timestamp=${timestamp}
jenkins.build.number=${jenkins.build.number}
```

`project.version` resolves from the POM. `timestamp` resolves from `buildnumber-maven-plugin`'s
`create-timestamp` execution in `mriss-parent` 3.9.0 (`timestampPropertyName=timestamp`).
`jenkins.build.number` resolves nowhere and ships literally.

### 2.1 Who reads the file — nobody, since 2017

The issue asks for this to be established before anything is removed. Git history settles it:

| Date | Commit | What happened |
|---|---|---|
| 2017-12-05 | `4adb1588e` | Both files arrive with two readers in `DSH-rest-api`. `ConfigProperties` loaded `version.properties` from the classpath and `getVersion()` returned `version-jenkins.build.number-timestamp`. `VersionUtils` read `WEB-INF/classes/version.properties` and, when `jenkins.build.number` was set, rewrote the file at WAR start-up via `InitServlet`. |
| 2017-12-31 | `8ab860b4c` | `ConfigProperties`, `VersionUtils` and `InitServlet` deleted. No reader remains. |
| 2018-02-04 | `706ed696b` | Springfox Swagger added. `SwaggerConfig.apiInfo()` sets a title and a description, never `.version(...)` — then and now (`SwaggerConfig.java:70-74`). The Swagger UI shows Springfox's default. |
| 2019-03-01 | `7ea3e13c2` | The old `parent-pom.xml` drops `<jenkins.build.number>dev</jenkins.build.number>`. From here the placeholder ships unresolved. |

Checked on the RC at `a8fc6ebc2`:

1. **No Java reader.** `git log -S'version.properties' --all -- '*.java'` returns only the two
   2017 commits above. No `.java` file on the RC names the file, calls `getResource*`, or uses
   `ClassPathResource` or `ResourceLoader`.
2. **No other reader.** No workflow, POM, site page, Dockerfile or deploy manifest names it. The
   repository has no Dockerfile, Helm chart or Kubernetes manifest at all.
3. **The Swagger page does not use it.** See the 2018 row. The published API document,
   `specs/api/openapi/dsh-rest-api.yaml`, hard-codes `version: 1.0.0`.
4. **The actuator does not use it.** `/actuator/info` is exposed, but no `build-info` goal and no
   `info.*` property feeds it.
5. **The `WEB-INF` path is gone.** Both modules package as `jar`.
6. **It could not be relied on anyway.** `dsh-rest-api` is a Spring Boot jar holding its own copy at
   `BOOT-INF/classes/version.properties` and `dsh-data`'s copy, at its jar root, under `BOOT-INF/lib`. Both
   sit at the same classpath path, so which one a lookup finds depends on classpath order.

## 3. Design

### 3.1 Decision: delete both files

Three options were weighed on the issue's terms (AC002):

| Option | Verdict |
|---|---|
| Give `jenkins.build.number` a value | Rejected. The issue's Out of Scope forbids reintroducing a build number; that is a new requirement. |
| Delete the `jenkins.build.number` line only | Rejected. It meets AC001 but keeps a file with no reader, one that §2.1 shows has had none for nine years. |
| **Delete both files** | **Chosen.** The file exists for a reader deleted in 2017. Nothing is lost, because nothing consumes it. |

AC003 applies only if the file survives, so it is discharged by deletion. §2.1 is still recorded
on the issue, so the next person asking "is this used?" about a similar file has the method.

If a build version is wanted in the running service later — on the Swagger page, say — the
route is Spring Boot's `build-info` goal plus `BuildProperties`, not this file. That is a new
requirement and not part of this story.

### 3.2 The sweep

AC001's check is `#114`'s sweep, made exact. A plain `grep '${'` over `target/classes` also
matches three binary PDFs in `dsh-test-dataset` (`bbc-news-1.pdf`, `edition.cnn.com-2.pdf`,
`The-Categories.pdf`), where the bytes `${` are compressed-stream noise, not placeholders. `-I`
skips binary files:

```bash
find . -type f -path '*/target/classes/*' -not -path './.git/*' -exec grep -lIF '${' {} +
```

`-F` makes `${` a fixed string, so no regex quoting is needed. `find` descends into nested
modules (`dsh-doc-analyser/*`, `dsh-solr/*`) as well as top-level ones.

On the RC today, after `mvn -B clean install`, it prints exactly:

```text
./dsh-data/target/classes/version.properties
./dsh-rest-api/target/classes/version.properties
```

After this story it prints nothing. That is the red and the green.

### 3.3 AC004: `#114`'s AC003 stays narrowed

`#114` is closed, and its AC003 was ticked against the narrowed `mongo.*` wording. Rewriting a
ticked criterion on a closed issue back to "no unresolved `${...}`" would claim a check `#114`
never ran. So AC004 is met by its second branch — this issue records why the AC should not be
restored:

- the general guarantee moves here, as `#115`'s AC001, verified by §3.2's sweep;
- `#114`'s body is left as it is;
- a comment on `#114` (§5, Task 3) and one line appended to `#114`'s spec §11.1 point forward to
  `#115`.

The line appended to the end of `#114`'s §11.1 paragraph that begins "Raised as `#115`":

```markdown
`#115` deleted both files rather than restoring this AC on a closed issue; its AC001 carries the
general sweep. See `specs/stories/115-resolve-version-properties-placeholders.md` §3.3.
```

### 3.4 Out of scope

- **Any build number or build version in the artifact or on the Swagger page.** §3.1.
- **A permanent CI guard for unresolved placeholders.** A new requirement; raise it separately if
  wanted.
- **`#114`'s `mongo.*` work**, as the issue says.
- **The `specs/product/PRD.md` row for `#115`.** `dsh-reconcile-prd` updates it after merge.

## 4. Files to change

| File | Change |
|---|---|
| `dsh-data/src/main/resources/version.properties` | Delete |
| `dsh-rest-api/src/main/resources/version.properties` | Delete |
| `specs/stories/114-supply-build-properties-to-release-wrappers.md` | Append §3.3's line to §11.1 |
| `specs/stories/115-resolve-version-properties-placeholders.md` | This spec; build record in §7 |

No `.java` file changes and no POM changes. Both modules keep `<filtering>true</filtering>`, since
`dsh-data`'s `mongo.properties` needs it.

## 5. Tasks

Every Maven run follows `CLAUDE.md`'s "Always log local Maven runs": redirect to `.logs/`, print
the `tail -f` command, `wait`, and report the exit code.

There is no Java under test. The failing check is §3.2's sweep: **Task 1** records it red on the
RC, and Task 2 turns it green without changing any other measurement.

- [x] **Task 1 — baseline on the RC.** Before any edit:
      1. `mvn -B clean install > .logs/mvn-clean-install-baseline.log 2>&1`. Expected: exit 0.
         Record in §7 the last `Tests run:` summary per module, and the counts of
         `All coverage checks have been met.` and `Rule violated`.
      2. Run §3.2's sweep. Expected: exactly the two `version.properties` paths. Record them.
      3. `jar tf <jar> | grep -E '(^|/)version\.properties$'` for
         `dsh-data/target/dsh-data-0.3.0-SNAPSHOT.jar` (expected: `version.properties`) and
         `dsh-rest-api/target/dsh-rest-api-0.3.0-SNAPSHOT.jar`, a Spring Boot jar (expected:
         `BOOT-INF/classes/version.properties`). The copy inside the nested
         `BOOT-INF/lib/dsh-data-0.3.0-SNAPSHOT.jar` is the same file as the `dsh-data` jar's, so the
         first check covers it.
      4. Commit nothing.
- [x] **Task 2 — delete the files.**
      `git rm dsh-data/src/main/resources/version.properties dsh-rest-api/src/main/resources/version.properties`.
      `mvn -B clean install > .logs/mvn-clean-install.log 2>&1`. Expected:
      - exit 0;
      - each module's last `Tests run:` summary equals Task 1's;
      - `All coverage checks have been met.` count equals Task 1's, `Rule violated` is `0`;
      - §3.2's sweep prints nothing;
      - Task 1's `jar tf` checks print nothing.

      Commit: `fix(#115): delete the orphaned version.properties from dsh-data and dsh-rest-api`.
- [ ] **Task 3 — record the decision.**
      1. Append §3.3's line to `#114`'s spec §11.1.
      2. Fill in §7, tick §8, and run the `CLAUDE.md` markdownlint command. Expected: no findings.
      3. Commit: `docs(#115): record the decision and the verification`.
      4. After the user approves the text, post §6's two comments: the first on `#115`, the second
         on `#114`. Record both comment URLs in §7.

## 6. Issue comments

On `#115` (AC002, AC003, AC004):

```markdown
**Decision: both `version.properties` files are deleted** — neither the line alone, nor a value
for `jenkins.build.number`.

**Who read it.** Nobody since 2017. `ConfigProperties` and `VersionUtils` arrived with the file in
`4adb1588e` and were deleted in `8ab860b4c` (2017-12-31). Springfox, added in `706ed696b`
(2018-02), never set an API version from it. `/actuator/info` has no `build-info` source. The
modules package as `jar`, so `VersionUtils`' `WEB-INF` path no longer exists, and `dsh-rest-api`'s
classpath carries two copies at the same root path. `jenkins.build.number` stopped resolving
when `7ea3e13c2` (2019-03-01) removed its `dev` default. Full trail: spec §2.1.

**AC003** is discharged by deletion: the file does not survive.

**AC004.** `#114`'s AC003 stays narrowed. `#114` is closed with that AC ticked against the
`mongo.*` wording, and restoring the general form there would claim a check `#114` never ran. The
general guarantee is this issue's AC001, verified by the sweep in spec §3.2 — `grep -I`, because
three binary PDFs in `dsh-test-dataset` otherwise match `${`. `#114`'s spec §11.1 and a comment on
`#114` point here.
```

On `#114`:

```markdown
Follow-up to AC003's narrowing: `#115` deleted both `version.properties` files, so no built
resource carries an unresolved `${...}` any more. This issue's AC003 is left in its narrowed form,
as verified. The general check now lives in `#115`'s AC001.
```

## 7. Build record

Built on 2026-09-28 on `issue-115-resolve-version-properties-placeholders`, cut from the RC at
`06c0013ef`.

| Run | Log | Exit | Sweep (§3.2) | `jar tf` hits | `All coverage checks have been met.` | `Rule violated` |
|---|---|---|---|---|---|---|
| Task 1 — baseline, before any edit | `.logs/mvn-clean-install-baseline.log` | 0 | 2 paths | 2 | 8 | 0 |
| Task 2 — after deleting both files | `.logs/mvn-clean-install.log` | 0 | none | 0 | 8 | 0 |

Task 1's sweep printed exactly the two paths §3.2 predicted:

```text
./dsh-data/target/classes/version.properties
./dsh-rest-api/target/classes/version.properties
```

and its `jar tf` checks found `version.properties` in the `dsh-data` jar and
`BOOT-INF/classes/version.properties` in the `dsh-rest-api` jar. Task 2's sweep and both `jar tf`
checks printed nothing.

Each module's last surefire `Tests run:` summary was identical in both runs, all with
`Failures: 0, Errors: 0, Skipped: 0`:

| Module | Tests run |
|---|---|
| `dsh-data` | 51 |
| `dsh-rest-api` | 36 |
| SOLR - Terms Vector Orderer | 22 |
| SOLR - Advanced Numbers Filter | 10 |
| `dsh-doc-indexer-worker` | 2 |
| Document Keyword Extractor | 2 |
| `dsh-top-sentences-extractor` | 2 |
| `dsh-doc-processor-worker` | 2 |

Commits: `a99f2ad82` (the deletion, Task 2) and the `docs(#115)` commit that records this (Task 3).
Issue comments (Task 3.4): pending the user's approval of §6's text.

## 8. Acceptance criteria

- [x] **AC001** — No built resource under any module's `target/classes` contains an unresolved
      `${...}` placeholder after `mvn -B install`. The run uses `clean install`, because
      `CLAUDE.md` requires `clean` for any result about the build. *§3.2, Task 2.*
- [ ] **AC002** — The decision is recorded on this issue, with its evidence: delete both files.
      *§3.1, §6.*
- [x] **AC003** — Discharged: `version.properties` does not survive. The reader history is recorded
      anyway. *§2.1, §6.*
- [ ] **AC004** — `#115` records why `#114`'s AC003 is not restored, and `#114` points forward.
      *§3.3, §6.*

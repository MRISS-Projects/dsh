---
issue: 104
slug: regenerate-coverage-badge
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 104 — Regenerate the coverage badge, or stop publishing a stale one

## 1. Story

**As a** person reading DSH's README
**I want** the coverage badge to show the coverage this build actually has
**So that** the headline number is evidence rather than decoration

## 2. Root cause

The issue's own diagnosis holds, and is not repeated here: `README.md` shows the committed
`dsh-coverage-report/badges/jacoco.svg` (92%), and the `process-badges` profile that would
regenerate it is reachable only through `-P`, which nothing in the estate passes. Checking it on
2026-09-27 found three more things. The fourth is the one that changes the story's scope.

1. **The profile would be wrong even if something activated it.** Its badge execution is bound to
   `process-resources`. The CSV it reads, `jacoco-aggregate/jacoco.csv`, is written by
   `report-aggregate` at `verify`. In one build the badge step reads no CSV (after `clean`) or the
   previous build's. It could never show the current commit's figure.
2. **Committing the badge from inside the build was already rejected upstream.** parent-poms `#71`
   moved the README commit out of an `scm:checkin` in `process-resources` into the separate
   `commit-readme` step, which runs after the build and commits `README.md` only.
3. **The site already publishes the aggregate report**, at
   `rcs/products/dsh/dsh-coverage-report/jacoco-aggregate/` on `gh-pages`. The README's relative
   badge link is broken on the site: the staging log reports
   `Image not found. URI: dsh-coverage-report/badges/jacoco.svg`.
4. **The published aggregate counts test classes as production code.** The `gh-pages` CSV of
   2026-09-26 lists `TermsVectorComparatorTest`, `OrderTest` and anonymous classes of
   `OrderedTermVectorComponentTest` under `solr-terms-vector-order`, for a total of 6,910
   instructions (98.42%). A local clean unit build has 2,028 instructions (98.52%) and no test
   classes. A badge generated from the published CSV would pass the issue's AC002, by matching the
   file, while showing the wrong number.

### 2.1 Why test classes reach `target/classes`

Four modules unpack the `dsh-test-dataset` resource bundle into their test output:

| Module | POM |
|---|---|
| `dsh-data` | `dsh-data/pom.xml:116` |
| `dsh-rest-api` | `dsh-rest-api/pom.xml:174` |
| `solr-advanced-numbers-filter` | `dsh-solr/solr-advanced-numbers-filter/pom.xml:76` |
| `solr-terms-vector-order` | `dsh-solr/solr-terms-vector-order/pom.xml:76` |

Each configures `maven-remote-resources-plugin:process` with
`<outputDirectory>${project.build.directory}/test-classes</outputDirectory>` and
`<attached>false</attached>`. The plugin is at 3.3.0, and **3.x has no `attached` parameter.**
`mvn help:describe -Dplugin=org.apache.maven.plugins:maven-remote-resources-plugin:3.3.0 -Dgoal=process -Ddetail`
lists `attachToMain` and `attachToTest`, both defaulting to `true`. Maven ignores the unknown
`attached` silently, so `attachToMain=true` registers `target/test-classes` as a **main** resource
directory, and `resources:resources` copies its whole content into `target/classes` at every
`process-resources`.

- **On a clean build** only the four dataset fixtures are there. They are copied into
  `target/classes` and so ship inside the main jars. The log line is
  `Copying 4 resources from target\test-classes to target\classes`.
- **Once tests have been compiled**, the copy takes the compiled test classes with it. That happens
  in the `site` lifecycle, where `jxr` and `javadoc` fork `compile` and `test-compile` after an
  earlier `deploy` left `target/test-classes` full, and in any second `install` without `clean`.

Reproduced locally on 2026-09-27: after `mvn -B clean install`, `dsh-data/target/classes` held `0`
`*Test*.class` files. After a following `mvn -B site`, it held `8`, the log showed
`Copying 14 resources from target\test-classes to target\classes`, and JaCoCo reported
`Analyzed bundle 'dsh-data' with 36 classes` against 28 in the `deploy` step. The staging log of run
`36286315531` shows the same 28 → 36.

**The same mechanism caused `CLAUDE.md`'s 2026-09-27 incident**, where a plain `mvn -B install`
failed `dsh-rest-api` at 0.50 line coverage. `CLAUDE.md` attributes it to an IDE build leaving test
classes in `target/classes`. No IDE is needed. Reproduced the same day on the tree the `site` run
above had left: `mvn -B install` without `clean` failed with
`Rule violated for bundle dsh-rest-api: lines covered ratio is 0.50, but expected minimum is 0.95`,
the same figure, while `dsh-data` still reported `All coverage checks have been met.` §6 corrects
`CLAUDE.md`.

This is DSH's configuration, not parent-poms'. The four executions are declared in DSH's POMs, and
parent-poms configures no `remote-resources`.

## 3. The change

### 3.1 Stop test output leaking into production classes

In each of the four POMs in §2.1, replace `<attached>false</attached>` with
`<attachToMain>false</attachToMain>`. `attachToTest` keeps its default: the output is already in
`target/test-classes`, where the tests read it.

### 3.2 Generate the badge in the build, publish it with the site

In `dsh-coverage-report/pom.xml`, replace the `process-badges` profile with:

```xml
<profile>
    <!-- #104: the coverage badge is published with the site, next to the report it is computed
         from, and never committed. Activated by -Ddeployment, which the staging and release
         builds already pass; the estate activates profiles by property, never by -P. -->
    <id>coverage-badge</id>
    <activation>
        <property>
            <name>deployment</name>
        </property>
    </activation>
    <build>
        <plugins>
            <plugin>
                <groupId>com.sigpwned</groupId>
                <artifactId>jacoco-badge-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>generate-jacoco-badge</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>badge</goal>
                        </goals>
                        <configuration>
                            <passing>95</passing>
                            <metric>instruction</metric>
                            <reportFile>${project.reporting.outputDirectory}/jacoco-aggregate/jacoco.csv</reportFile>
                            <badgeFile>${project.reporting.outputDirectory}/badges/jacoco.svg</badgeFile>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

- **Phase `verify`, after `report-aggregate`.** Both executions are bound to `verify`. Maven orders
  executions in one phase by plugin declaration order, and a profile's plugins merge after the
  `<build>` plugins, so the badge runs second. Task 2 verifies the order in the build log rather
  than trusting this.
- **`<passing>95</passing>`** is the real gate (AC003). Under it the badge is not green.
- **`maven-scm-plugin` is deleted, with its `1.9.5` pin.** Nothing is committed, so there are no
  credentials to name, no version override to reconcile against parent-poms' managed `2.1.0`, and
  no commit that could retrigger CI (AC004).
- **The plugin version stays managed by parent-poms** (`jacoco.badge.maven.plugin`, 0.1.4).

The staging workflow runs `clean deploy` and then `site-deploy`, without `clean` between them. The
badge written to `target/site/badges/` in the first step is still there when the second one
publishes `target/site/`. With §3.1 in place, the `jacoco-aggregate` that `site-deploy` regenerates
analyses the same classes and the same exec files as the one the badge was computed from.

Delete the tracked `dsh-coverage-report/badges/jacoco.svg` and its directory.

### 3.3 Point the README at the published badge

`README.md` is generated from `src/site/markdown/README.md` by parent-poms' `readme-generation`
profile. Change line 3 of the template to:

```markdown
![Coverage](https://mriss-projects.github.io/dsh/${release.type}/products/dsh/dsh-coverage-report/badges/jacoco.svg)
```

`${release.type}` resolves to `rcs` in a staging build and `releases` in a release build, so each
generated README shows the badge of the build that generated it. The URL is absolute, so the same
link works on GitHub, on the site and in the README PDF. `README.md` itself is regenerated and
committed by the next staging run. It is not edited by hand.

### 3.4 How the badge lands — the AC004 decision

**Produced without a commit.** The badge is a build output published next to the report it is
computed from, in the same site deploy. It was chosen on 2026-09-27 over the alternatives:

- **Committing it back** would repeat what parent-poms `#71` removed, and needs a write credential
  and a `[skip ci]` guard in DSH's build.
- **A shields.io dynamic badge** reading the published `jacoco.xml` would have to compute a ratio
  from two XML attributes, adds an external dependency, and still inherits §2's finding 4.

The cost is that the badge moves only when a staging or release build runs, not on every CI build.
That is the cadence at which the site's report moves too, and the badge claims no more than it.

## 4. Acceptance criteria

The issue's five criteria, with the evidence each one needs. AC006 comes from §2.1.

- [ ] **AC001** — The badge is regenerated by the staging build, activated by `-Ddeployment`. No
  `-P` is added anywhere. *Evidence:* the staging log shows
  `jacoco-badge-maven-plugin:0.1.4:badge (generate-jacoco-badge) @ dsh-coverage-report` after
  `report-aggregate (report-code-coverage)`, and
  `git grep -nE -- '(^|[[:space:]])-P[[:space:]a-z]' -- .github pom.xml '*/pom.xml'` finds nothing new.
- [ ] **AC002** — The figure in the published SVG matches the INSTRUCTION ratio of the published
  `jacoco.csv` from the same staging run, rounded the way the plugin rounds it (Task 2 establishes
  the rule from the plugin's source). *Evidence:* the output of the commands in §5.3.
- [ ] **AC003** — The badge configuration says `<passing>95</passing>`.
- [ ] **AC004** — §3.4 records the decision, and `docs/devops/README.md` states it (§6). No
  `maven-scm-plugin` remains in `dsh-coverage-report/pom.xml`.
- [ ] **AC005** — `README.md`, as regenerated by the staging run, links the published badge, and
  that URL returns HTTP 200 with the AC002 figure. The §6 risk entry in `specs/product/PRD.md` is
  removed.
- [ ] **AC006** — No test class reaches production output. After `mvn -B clean install` and then
  `mvn -B site`, `find . -path '*/target/classes/*' -name '*Test*.class'` prints nothing, and the
  published aggregate CSV lists no class whose name ends in `Test`.
- [ ] **AC007** — `mvn -B clean install` passes with both quality gates, per `CLAUDE.md`.

## 5. Testing approach

This is build configuration. The red/green cycle runs on Maven builds, logged per `CLAUDE.md`.

### 5.1 The leak (§3.1)

**Red**, before the change:

```bash
mkdir -p .logs
mvn -B clean install > .logs/mvn-clean-install.log 2>&1; echo "install exit=$?"
mvn -B site > .logs/mvn-site.log 2>&1; echo "site exit=$?"
find . -path '*/target/classes/*' -name '*Test*.class' | wc -l
grep -c 'from target.test-classes to target.classes' .logs/mvn-clean-install.log
```

Non-zero, then `4`.

**Green**, after it: the same commands print `0` and `0`. Then:

- `mvn -B install` a second time, without `clean`, passes the coverage gate;
- the four modules' tests still pass, which shows the dataset fixtures still reach
  `target/test-classes`;
- the aggregate CSV totals the same number of instructions after `site` as after `install`.

### 5.2 The badge (§3.2)

```bash
mvn -B -Ddeployment -Drelease.type=rcs -Dbuild.number=RC0 clean install \
  > .logs/mvn-clean-install-deployment.log 2>&1; echo "maven exit=$?"
grep -n 'report-aggregate\|jacoco-badge' .logs/mvn-clean-install-deployment.log
ls dsh-coverage-report/target/site/badges/jacoco.svg
```

The badge line comes after the `report-aggregate` line, and the SVG exists. Run without
`-Ddeployment`, the badge execution does not appear. `-Ddeployment` regenerates `README.md`, so run
`git checkout README.md` afterwards.

### 5.3 End to end

Dispatch `staging.yml` on the task branch. Once Pages reports the new `gh-pages` commit `built`:

```bash
base=https://mriss-projects.github.io/dsh/rcs/products/dsh/dsh-coverage-report
curl -s "$base/jacoco-aggregate/jacoco.csv" \
  | awk -F, 'NR>1{m+=$4;c+=$5} END{printf "csv: %d/%d = %.4f%%\n",c,m+c,100*c/(m+c)}'
curl -s "$base/badges/jacoco.svg" | grep -o '>[0-9.]*%<' | tr -d '><' | sort -u
curl -s "$base/jacoco-aggregate/jacoco.csv" | cut -d, -f3 | grep -c 'Test$'
```

The badge figure equals the CSV ratio under the plugin's rounding (AC002), and the last line prints
`0` (AC006). The run pushes an `Auto-generated README.md` commit to the task branch, so pull before
the next commit.

## 6. Documentation

- **`docs/devops/README.md`** — a short "Coverage badge" subsection: what generates the badge,
  when (`-Ddeployment` builds only), where it is published, and why it is not committed (§3.4).
- **`CLAUDE.md`** — replace "an IDE build had left test classes in `target/classes`" with the real
  mechanism (§2.1), and say `#104` fixed it. The rule that the gate always includes `clean` stays:
  stale output can still skew coverage in other ways, and CI builds from a fresh checkout
  regardless.
- **`specs/product/PRD.md`** — remove the §6 risk entry "The coverage badge is stale, and nothing
  regenerates it" (AC005). Status and placement are left to `dsh-reconcile-prd`.

## 7. Out of scope

- **The coverage gate** (95%, inherited from parent-poms) and **moving badge generation into
  parent-poms**, as the issue says.
- **Per-module badges** and a **badge on every CI build.** CI's token is read-only and CI builds
  pull-request code. See §3.4.
- **Other uses of `maven-remote-resources-plugin` in the estate.** If parent-poms or another
  consumer carries the same `attached` configuration, that is a separate issue there.

## 8. Implementation order

### Task 1 — the leak: red, fix, green

Run §5.1's red commands, including a second `mvn -B install` without `clean`, which must fail
`dsh-rest-api` at 0.50 as in §2.1. Apply §3.1. Run §5.1's green commands, where the same second
install must pass. Commit.

### Task 2 — the badge

Read the plugin's rounding rule from `jacoco-badge-maven-plugin` 0.1.4's source, and record it in
AC002's evidence. Apply §3.2 and delete the tracked SVG. Run §5.2. Commit.

### Task 3 — the README template

Apply §3.3. Commit.

### Task 4 — publish and verify

Push, dispatch `staging.yml` on the task branch, and run §5.3. Record the evidence against AC001,
AC002, AC005 and AC006.

### Task 5 — document and ship

Apply §6. Run `mvn -B clean install` for AC007 and the markdown lint. Hand over to `dsh-ship-story`.

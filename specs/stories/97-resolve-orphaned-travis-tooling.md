---
issue: 97
slug: resolve-orphaned-travis-tooling
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 97 — Resolve the tooling orphaned by the Travis estate removal

## 1. Story

**As a** developer working in this repository
**I want** the tooling left orphaned by the Travis estate removal either wired up, deleted, or
documented
**So that** the repository does not accumulate a second generation of scripts and Maven profiles
that nobody calls and nobody dares delete

## 2. Context

`#92` (delivered by PR `#96`) removed the dead Travis estate and deliberately left three loose ends,
promising this follow-up in §9 of `specs/stories/92-remove-travis-build-estate.md`:

1. four `mvn` wrapper scripts with no caller — `deploy.sh`, `maven-site.sh`,
   `maven-site-deploy.sh`, `set-version.sh`;
2. the `update-readme` Maven profile, whose only caller was the deleted `post-release-script.sh`;
3. `README.md:350`, which now names a GitHub Packages prerequisite the README never explains.

Two of the issue's own premises did not survive checking, and this spec corrects them rather than
implementing against them.

**AC003 is already satisfied at the branch point.** The issue states that `${branch.name}` "has no
definer anywhere in the repository". It also has no *consumer*: `git grep 'branch\.name'` returns
zero hits on `staging-0.3.0-SNAPSHOT-RC`. `#92` removed the last one along with the Travis badge
(`f9d8456f`). AC003 is retained below as a regression guard, not as work.

**AC005 cites a script that `#93` deleted.** `./scripts/check-coverage.sh` was removed by PR `#102`
together with `.github/coverage-baseline.txt`; the inherited `jacoco:check` is the only coverage
gate now, and it is bound to `verify`, so `mvn -B install` runs it. AC005 is restated accordingly.

**The third premise — that the `update-readme` profile is simply callerless — is true but
misleading**, and §3 replaces it.

## 3. Why README generation stopped, in mechanism

`README.md` is a generated file. Its source is `src/site/markdown/README.md`, and two placeholders
survive there today:

| Line | Placeholder | Filled by |
|---|---|---|
| 7 | `${project.build.version}` | a `deployment` / `release-deployment` profile property |
| 513 | `${issues.text.list}` | `maven-changes-plugin:github-text-list` |

The committed `README.md` was last machine-generated on **2020-02-22** — its version line still
reads `0.3.0-SNAPSHOT - 20200222-214018` — and has been hand-edited since, including by `#92`,
which edited both copies in parallel.

### 3.1 The release path already has a step for this, and it is a no-op for DSH

`MRISS-Projects/parent-poms` runs a README step in two of its four reusable workflows —
`project-release.yml:216` and `project-hotfix.yml:175`, both identical:

```yaml
- name: Update README.md on Master
  run: |
    cd target/checkout
    mvn -B \
      -Ddeployment \
      -Drelease-deployment \
      process-resources
```

It does **not** pass `-Dupdate-readme`, so it never calls DSH's profile. It relies instead on the
executions inside parent-poms' own `deployment` profile (`parent-poms/pom.xml:933-1079`):

| Execution | Plugin | Phase | Line |
|---|---|---|---|
| `generate-list-of-issues` | `maven-changes-plugin` | `generate-sources` | 997 |
| `create-time-stamp` | `buildnumber-maven-plugin` | `process-resources` | 1018 |
| `copy-readme-md` | `maven-resources-plugin` | `process-resources` | 1036 |
| `commit-readme-md` | `maven-scm-plugin` | `${commit.readme.phase}` | 1063 |

**Three of those four carry `<inherited>false</inherited>`.** That flag means "not propagated to any
descendant POM", not "not propagated to submodules" — so it correctly scopes them to parent-poms'
own root and, by the same rule, makes them structurally unreachable from DSH.

Measured, not inferred. Running

```bash
mvn -B -N -Ddeployment -Drelease-deployment help:effective-pom -Doutput=<file>
```

in the DSH root yields an active `<build><plugins>` containing `generate-list-of-issues` — the one
execution of the four that has no `<inherited>` flag — and **neither `copy-readme-md` nor
`commit-readme-md`, and no `buildnumber-maven-plugin` execution at all**. The only occurrences of
those two ids anywhere in the effective model are inside the inert `update-readme` profile
declaration.

> **Do not commit the output of that command.** With `-Ddeployment` active it interpolates
> `${github.personal.token}` from the developer's `settings.xml` into
> `maven-changes-plugin`'s configuration, so the file contains a live GitHub PAT. Write it to
> `.logs/` (gitignored) and delete it after reading.

So the `Update README.md on Master` step generates the issue list into `target/` and stops. By the
same inheritance rule it is a no-op for *any* product repository, not just DSH.
`project-staging.yml` and `project-stage.yml` have no README step at all.

### 3.2 The upstream comment that records the opposite

`parent-poms/products/pom.xml:158-161` states:

> FR001/FR008: 'deployment' profile removed from products level. The copy-readme-md and
> commit-readme-md executions it contained are identical to the root parent-poms/pom.xml
> 'deployment' profile and are **fully covered by inheritance**.

They are not covered by inheritance, for the reason above. Before that removal
(`parent-poms@114e0f71`), `copy-readme-md` sat in `products/pom.xml` **without**
`<inherited>false</inherited>`, one level above DSH, and did reach it. The comment is the cause of
this story and is corrected as part of §4.

### 3.3 DSH's own `deployment` profile is broken in a way this would have exposed

`pom.xml:336-338` overrides the inherited property:

```xml
<properties>
    <project.build.version>${project.version} - ${timestamp}</project.build.version>
</properties>
```

`${timestamp}` is produced by `create-time-stamp`, one of the three non-inherited executions, so it
has no definer in DSH. Measured:

```text
mvn -N -Ddeployment help:evaluate -Dexpression=project.build.version
  → 0.3.0-SNAPSHOT - null

mvn -N -Ddeployment -Drelease-deployment -Dproduct-release-deployment (same expression)
  → 0.3.0-SNAPSHOT
```

The staging path — the one this story tests with — renders a broken version line today. The release
path is unaffected, because `product-release-deployment` and `release-deployment` both reset the
property to plain `${project.version}`.

## 4. The upstream change in `MRISS-Projects/parent-poms`

The feature belongs upstream: `parent-poms` exists to be reused, and every product repository that
keeps a `src/site/markdown/README.md` should get README regeneration by inheriting it, not by
copying it. DSH keeps no local replacement.

### 4.1 What is added

One new profile in `parent-poms/pom.xml`, inserted between the closing `</profile>` of `deployment`
(line 1079) and the opening `<profile>` of `release-deployment` (line 1080). **Placement is
load-bearing** — see §4.4.

The four `<plugin>` blocks listed in §3.1 move out of `deployment` into it *unchanged except for the
deletion of the three `<inherited>false</inherited>` lines*, and keep their relative declaration
order, because `create-time-stamp`, `copy-readme-md` and `commit-readme-md` all bind to
`process-resources` and same-phase executions run in POM declaration order — the timestamp must be
set before the resource is filtered.

```xml
<profile>
    <id>readme-generation</id>
    <!-- Two conditions, both required: Maven ANDs multiple activators (3.2.2+). The -Ddeployment
         half is what the staging, release and hotfix workflows already pass. The file half is what
         `<inherited>false</inherited>` could not express: it selects the reactor root of the
         CONSUMING project, because that is the only module holding a README source. Without it,
         see 4.2, maven-scm-plugin falls back to `git commit -a` in every module that has no
         README.md to stage. -->
    <activation>
        <property>
            <name>deployment</name>
        </property>
        <file>
            <exists>${basedir}/src/site/markdown/README.md</exists>
        </file>
    </activation>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-changes-plugin</artifactId>
                <executions>
                    <execution>
                        <id>generate-list-of-issues</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>github-text-list</goal>
                        </goals>
                        <configuration>
                            <includeOpenIssues>false</includeOpenIssues>
                            <onlyMilestoneIssues>true</onlyMilestoneIssues>
                            <columnNames>Id,Type,Summary,Assignee,Reporter,Updated</columnNames>
                            <failOnError>false</failOnError>
                            <githubAPIServerId>github.com</githubAPIServerId>
                            <personalToken>${github.personal.token}</personalToken>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>buildnumber-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>create-time-stamp</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>create-timestamp</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <timestampFormat>yyyyMMdd-HHmmss</timestampFormat>
                    <timestampPropertyName>timestamp</timestampPropertyName>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-readme-md</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>.</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>src/site/markdown</directory>
                                    <includes>
                                        <include>README.md</include>
                                    </includes>
                                    <targetPath>.</targetPath>
                                    <filtering>true</filtering>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-scm-plugin</artifactId>
                <executions>
                    <execution>
                        <id>commit-readme-md</id>
                        <phase>${commit.readme.phase}</phase>
                        <goals>
                            <goal>checkin</goal>
                        </goals>
                        <configuration>
                            <basedir>${basedir}</basedir>
                            <includes>README.md</includes>
                            <message>Auto-generated README.md [skip jenkins]</message>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

The `<properties>` of `deployment` (lines 941-944) **stay where they are**, including
`project.build.version` and `project.scm.id`. Moving them would change property precedence against
`release-deployment`; leaving them costs nothing, because an unresolved `${timestamp}` in a module
where the profile is inactive is a string nobody reads.

`commit.readme.phase` (`parent-poms/pom.xml:79-82`, default `process-resources`) stays where it is
too, so `-Dcommit.readme.phase=none` keeps disarming the commit — `build.yml:149,157` depends on
that.

### 4.2 Why activation keys on the file, and not simply dropping `<inherited>false</inherited>`

Deleting the three flags in place, leaving the executions inside `deployment`, looks equivalent and
is much smaller. It is wrong, and the failure is silent rather than loud.

`deployment` activates on a property, which is true for **every** module in the reactor. With the
flags gone, all three executions reach all 13 DSH modules. Two are harmless there —
`create-timestamp` just sets a property, and `copy-resources` skips a non-existent
`src/site/markdown`. The third is not.

Probed on a synthetic two-level reactor (installed parent → consumer root + one child), Maven
3.9.9, `maven-scm-plugin:2.1.0`, with a real git repository:

| Module | `includes=README.md` matches | git commands the provider issued |
|---|---|---|
| consumer root | yes | `git add -- README.md` → `git commit -F <msg>` |
| child (clean subtree) | no | `git status --porcelain .` → no commit |
| child (**dirty** subtree) | no | `git status --porcelain .` → `git commit -F <msg> **-a**` |

In the third case the child module committed `child/tracked.txt` — a modification with nothing to
do with any README — under the message `Auto-generated README.md`. The build exited **0**. Nothing
in the log marks it as wrong.

That is the real hazard: not a broken build, but a module with no README to stage falling through
to `git commit -a` and sweeping whatever is dirty in its subtree into a commit that claims to be a
generated README. In CI the checkout is clean, so it would lie dormant; on a developer's machine
running any `-Ddeployment` build it commits uncommitted work.

The file-activated profile makes that path **structurally unreachable**: the profile exists only
where `src/site/markdown/README.md` exists, which is exactly where `copy-readme-md` has just
written a `README.md` for `includes` to match. `git add` always has a target, so the `-a` fallback
is never selected. The neighbouring edge — `git add` staging nothing because the generated file is
byte-identical — is likewise unreachable, because the version line embeds `${timestamp}` and
therefore differs on every run.

### 4.3 Why `<file><exists>` is trustworthy here, given §4.2 of spec 93

`specs/stories/93-remove-redundant-coverage-ratchet.md` §4.2 rejected this very mechanism:

> Evaluating at execution time also avoids profile `<activation><file><exists>` semantics, whose
> relative-path resolution for a profile inherited into a multi-module reactor is unreliable
> (MNG-2363). A gate is the wrong place to depend on that.

That caution was right for a coverage gate and is not carried over blindly. It was measured on
Maven 3.9.9 — the version pinned by every workflow in both repositories — using the same synthetic
parent/consumer/child reactor, with the parent installed to the local repository so that activation
is genuinely resolved through inheritance:

| Case | `-Ddeployment` | file present | profile active |
|---|---|---|---|
| consumer root | yes | yes | ✅ |
| consumer child | yes | no | ❌ |
| consumer root | **no** | yes | ❌ |
| consumer root | yes | **removed** | ❌ |

Both spellings were compared in the same run — relative `src/site/markdown/README.md` and
interpolated `${basedir}/src/site/markdown/README.md`. On 3.9.9 they behave identically and
per-module; MNG-2363 does not manifest. The interpolated form is the one adopted anyway, because it
states the intent explicitly and does not depend on which basedir a reader assumes.

The distinction from `#93` stands on its own terms: a coverage gate must not be silently skippable,
so it was built to evaluate at execution time. README generation is the opposite case — a feature
that must not run where there is nothing to generate — and the staging dispatch in §8 confirms the
activation end to end rather than trusting the table above.

### 4.4 Blast radius

The change reaches **every** consuming repository on the next snapshot deploy.

Within parent-poms itself, behaviour is unchanged. Only its own root holds
`src/site/markdown/README.md` (`git ls-files '*README.md'` returns exactly `README.md` and
`src/site/markdown/README.md`), so the same single execution point, the same phases and the same
order survive the move. Its `deploy.yml` snapshot path must still produce an `Auto-generated
README.md` commit — that is the upstream regression check.

Within DSH, the same holds: no submodule has a `README.md` at all, and only the root has
`src/site/markdown/README.md`. Every other `README.md` in the repository lives under `docs/` or
`specs/`, which are not Maven modules.

The genuinely new behaviour is that **any** product repository root now regenerates and commits its
README on any `-Ddeployment` build, where today only parent-poms does. Two things bound that:
`-Dcommit.readme.phase=none` already exists as the opt-out, and `deploy.sh` — DSH's only local
caller of `-Ddeployment` — is deleted by this same story (§5.2).

Profile placement between `deployment` and `release-deployment` is required. Maven resolves a
property defined by several active profiles in favour of the **last declared**, and
`release-deployment` (line 1081) deliberately resets `project.build.version` to plain
`${project.version}` so that a released README carries no build number or timestamp. A profile
declared after it that redefined the property would silently break released READMEs. The profile
above defines no properties at all, so this is defence in depth rather than a live hazard — but the
next person to add one there should find the reason written down.

### 4.5 How the change is made

This is profile structure, so `CLAUDE.md` puts it on the **full round trip**, not the light one.
Two steps of that procedure are already satisfied and one does not apply:

1. Open a plain issue in `parent-poms` — text in §4.6. INVEST framing is not required there.
2. **Already true:** milestone `3.8.0-SNAPSHOT` is open, with `#59`, `#65` and `#67` still on it.
3. Implement and test there against that `-SNAPSHOT`.
4. **Already true:** DSH's root `pom.xml` pins `com.mriss.mriss-parent:products:3.8.0-SNAPSHOT`,
   which *is* the parent's live development version, and every Maven invocation in this repository
   passes `-U`. There is no temporary re-pointing to do and no step 6 re-pin.
5. **Does not apply to this story.** Releasing parent-poms would require clearing milestone
   `3.8.0-SNAPSHOT` first, and `#59`, `#65` and `#67` are not this story's work. `#97` ships against
   the deployed snapshot; the release happens when 3.8.0 is cut.

What makes the change visible to DSH's CI and workflows is therefore a **snapshot deploy**, not a
release:

```bash
gh workflow run deploy.yml --repo MRISS-Projects/parent-poms -f release_type=snapshots
```

A local `mvn -B install` in parent-poms is still worth running first, so the local repository
carries the change for §8's local checks.

### 4.6 The `parent-poms` issue to open

Opened only after approval, per `CLAUDE.md`. Proposed title and body:

> **Title:** README regeneration is unreachable from any consuming project
>
> The `deployment` profile's `copy-readme-md`, `commit-readme-md` and `create-time-stamp`
> executions carry `<inherited>false</inherited>`, which excludes every descendant POM, not just
> submodules. A consuming project's reactor root therefore never receives them, so the
> `Update README.md on Master` step in `project-release.yml:216` and `project-hotfix.yml:175` is a
> no-op for every product repository. Confirmed with `help:effective-pom` in `MRISS-Projects/dsh`.
>
> `products/pom.xml:158-161` records the opposite — "fully covered by inheritance" — and is the
> reason the gap went unnoticed. Before `114e0f71` these executions lived at products level
> *without* the flag and did reach consumers.
>
> Simply deleting the flags is not safe: `deployment` activates per-property in every module, and
> in a module where `includes=README.md` matches nothing, `maven-scm-plugin:checkin` falls back to
> `git commit -a` and commits unrelated working-tree changes under the message
> `Auto-generated README.md`, with exit code 0.
>
> Fix: move the four README executions into a new `readme-generation` profile activated on
> `-Ddeployment` **and** `<file><exists>${basedir}/src/site/markdown/README.md</exists>`, declared
> between `deployment` and `release-deployment`, dropping the three `<inherited>false</inherited>`
> lines. Correct the `products/pom.xml` comment, `CLAUDE.md:64` and
> `specs/github-actions-reusable-workflows.md:97`, all of which describe `update-readme` as the
> product-specific mechanism.
>
> Raised from `MRISS-Projects/dsh#97`. Full analysis in that repository at
> `specs/stories/97-resolve-orphaned-travis-tooling.md`.

Two documentation files in parent-poms describe the old arrangement and are corrected with it:

- `CLAUDE.md:64` — the `update-readme` row of the profile table, which names it as the
  product-specific README mechanism.
- `specs/github-actions-reusable-workflows.md:97` — `Profile: update-readme (activated by
  -Dupdate-readme) — DSH-specific`.

## 5. Files to change in DSH

### 5.1 `pom.xml`

**Delete lines 432-513** — the `update-readme` profile and the `<!-- FR002: ... -->` comment above
it. No local replacement: the behaviour arrives by inheritance once §4 is deployed. The profile's
own `generate-list-of-issues` goes with it and is not missed; the inherited one is already active
under `-Ddeployment` and is better configured, carrying `githubAPIServerId`, `personalToken` and
`runOnlyAtExecutionRoot`, none of which the DSH copy had.

**Delete lines 336-338** — the `<properties>` block of the `deployment` profile. With
`create-time-stamp` inherited, the parent's `${project.version} - ${build.number} - ${timestamp}`
resolves, and it is strictly better than the DSH override: it picks up the `RC<n>` that
`project-staging.yml` passes as `-Dbuild.number`.

**Rewrite the two comments that document the deleted things.** Lines 323-325 currently read:

```xml
<!-- FR002: 'deployment' and 'product-release-deployment' removed – fully covered by parent hierarchy.
     'release-deployment' removed – inherited from parent-poms/pom.xml.
     'update-readme' kept – DSH-specific README SCM customisation. -->
```

The third line becomes a statement that README generation is inherited from parent-poms'
`readme-generation` profile and that DSH deliberately holds no local copy. Lines 327-328:

```xml
<!-- DSH-specific: kept because it overrides project.build.version with timestamp
     and runs the DSH pdf-plugin with generated-site as siteDirectory. -->
```

The first reason is being deleted; only the pdf-plugin reason survives, and the comment must say
only that.

### 5.2 The four wrapper scripts

**`deploy.sh` — deleted.**

```bash
mvn -gs ~/apps/maven/conf/empty-settings.xml -Ddeployment -DskipTests clean deploy && \
mvn -gs ~/apps/maven/conf/empty-settings.xml -Ddeployment site-deploy
```

`~/apps/maven/conf/empty-settings.xml` does not exist on the author's current machine, so the
script is already broken for the one person it was written for. Deployment is CI's job — staging,
release and hotfix all deploy through `parent-poms` reusable workflows — and after §4 a local
`-Ddeployment` build also regenerates and commits `README.md`, which is not something a convenience
wrapper should do behind a developer's back.

**`maven-site.sh` and `maven-site-deploy.sh` — retained, documented, unchanged.** They stage the
Maven site into `file:///tmp` for local preview and touch nothing shared.

**`set-version.sh` — retained, documented, with an argument guard.** It is the local counterpart of
what `project-release.yml` does in CI and is genuinely useful for re-versioning the whole reactor
by hand; `parent-poms` keeps `set-version.sh` and `set-version.bat` at its own root for the same
reason. Today it fails opaquely when called with no argument, passing an empty
`-DdevelopmentVersion=` to Maven:

```bash
#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: ${0##*/} <version>   e.g. ${0##*/} 0.4.0-SNAPSHOT" >&2
    exit 1
fi

mvn --batch-mode -DdevelopmentVersion="$1" -DautoVersionSubmodules=true release:update-versions
```

If §8.2 shows that `release:update-versions` does not reach every module, the body is replaced by
the form `project-release.yml` itself uses, and the spec's claim changes with it:

```bash
mvn --batch-mode -DprocessAllModules=true -DnewVersion="$1" versions:set
```

### 5.3 `src/site/markdown/README.md` — and only this copy

AC004's content goes **into the source**. `README.md` is regenerated from this file, so an edit made
directly to `README.md` is destroyed by the next staging or release run.

Two changes. First, near line 350, the sentence that currently sends the reader to a configuration
section that does not answer the question:

> Maven development user settings should be correctly configured (see configuration section below).
> Your `~/.m2/settings.xml` must also authenticate against GitHub Packages, which is where the
> parent POM `com.mriss.mriss-parent:products` resolves from.

becomes a pointer to a new subsection. Second, that subsection itself, modelled on the
`settings.xml` that `.github/workflows/ci.yml` writes for itself and naming all three server ids
that workflow uses:

```xml
<settings>
  <servers>
    <server>
      <id>MRISS-Projects-maven-repo</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
    <server>
      <id>MRISS-Projects-maven-repo-plugins</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
    <server>
      <id>github.com</id>
      <username>YOUR-GITHUB-USERNAME</username>
      <password>YOUR-PACKAGES-READ-TOKEN</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>github-packages</id>
      <repositories>
        <repository>
          <id>MRISS-Projects-maven-repo</id>
          <url>https://maven.pkg.github.com/MRISS-Projects/maven-repo</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>MRISS-Projects-maven-repo-plugins</id>
          <url>https://maven.pkg.github.com/MRISS-Projects/maven-repo</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>github-packages</activeProfile>
  </activeProfiles>
</settings>
```

The prose around it says where the credential comes from — a classic PAT holding `read:packages`
and nothing else, from an account with read access to the `MRISS-Projects` organisation — and
cross-references the Secrets section of `docs/devops/README.md` rather than restating it. It also
states that the token is pasted into the developer's own `~/.m2/settings.xml` and never committed.

### 5.4 `docs/devops/README.md`

Two additions.

**"README.md is a generated file."** Source, generator, and the rule: edit
`src/site/markdown/README.md`, never `README.md`. Which workflows regenerate it — `staging.yml`
during `clean deploy`, `release.yml` and `hotfix.yml` in their `Update README.md on Master` step —
and that the mechanism is the inherited `readme-generation` profile in `parent-poms`, not anything
declared here. `-Dcommit.readme.phase=none` as the opt-out for a build that must not commit.

**"Local developer scripts."** One row each for `set-version.sh`, `maven-site.sh` and
`maven-site-deploy.sh`: what it runs, when a developer would want it, and that none of them is
called by CI. This is what turns AC001's `git grep` from a bare hit into a documented caller.

### 5.5 Three stale references to `deploy.sh`

Deleting the script leaves three documents instructing a reader to maintain it:

- `.github/roles.md:70` — "Maintain build and deployment scripts (e.g., `deploy.sh`)"
- `.github/copilot/prompts/feature-implementation.md:59` — "Update build scripts (deploy.sh) as
  necessary"
- `docs/copilot/prompt-examples.md:180` — same line as above

Each is corrected to name the deployment path that actually exists: the reusable workflows in
`parent-poms`, dispatched through `.github/workflows/{staging,release,hotfix,stage}.yml`.

## 6. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `README.md` | Generated. It is rewritten by the staging dispatch in §8.3, not by hand. |
| `.github/workflows/*.yml` | Staging, release and hotfix already pass `-Ddeployment`; §4 is what makes that sufficient. No workflow edit is needed in either repository. |
| `src/site/markdown/releases-history.md` | Already points at GitHub milestones; the regenerated issue tables in README complement it rather than duplicating its job. |
| `connect-mongo.sh`, `connect-mongo-super-user.sh` | Not in `#97`'s scope — never part of the Travis estate and never orphaned by it. |
| `dsh-*/pom.xml` | No module pom is touched. The profile work is entirely at the reactor root and upstream. |

## 7. Acceptance criteria

- [ ] **AC001** — `deploy.sh` is deleted and `git grep 'deploy\.sh'` returns no hit that instructs a
  reader to maintain it. `set-version.sh`, `maven-site.sh` and `maven-site-deploy.sh` are retained,
  and `git grep` for each returns a hit in `docs/devops/README.md` describing when to run it.
- [ ] **AC002** — the `update-readme` profile is gone from the root `pom.xml`, no replacement is
  declared in this repository, and README regeneration reaches DSH by inheritance from
  `parent-poms`' `readme-generation` profile. Proven by §8.3: a `staging.yml` dispatch produces an
  `Auto-generated README.md` commit whose line 7 matches `0.3.0-SNAPSHOT - RC<n> - <timestamp>`
  with no unresolved `${...}`, and whose issue tables list the `0.3.0-SNAPSHOT` milestone.
- [ ] **AC003** — `git grep 'branch\.name'` returns no hits. Already true at the branch point;
  carried as a regression guard.
- [ ] **AC004** — `src/site/markdown/README.md` documents `~/.m2/settings.xml` for GitHub Packages
  with a `<server>` example, and the regenerated `README.md` carries it after §8.3. The source is
  edited; the generated copy is not hand-edited.
- [ ] **AC005** — `mvn -B install` succeeds. *(Restated: the issue's `./scripts/check-coverage.sh`
  was deleted by `#93`/PR `#102`; `jacoco:check` is bound to `verify`, so this one command is the
  whole gate.)*
- [ ] **AC006** — markdown lint passes over the changed documentation.
- [ ] **AC007** — CI is green on the pull request.
- [ ] **AC008** — `set-version.sh` versions the entire reactor. §8.2's procedure sets a throwaway
  version, shows all 13 POMs carrying it, restores, and ends with an empty `git status --porcelain`.
- [ ] **AC009** — the `parent-poms` round trip is closed out: the issue from §4.6 exists, the change
  is on `parent-poms` `master`, `deploy.yml` has run with `release_type: snapshots`, and the
  resulting commit SHA is commented on `#97`.

## 8. Testing approach

### 8.1 Order of operations

§4 lands before §5 can be verified. The sequence is: parent-poms issue → parent-poms change →
`mvn -B install` locally in parent-poms → `deploy.yml` snapshot dispatch → SHA commented on `#97` →
DSH changes → staging dispatch.

Per `CLAUDE.md`, every local Maven run in this 13-module reactor is redirected to `.logs/` and its
exit code reported explicitly:

```bash
mkdir -p .logs
mvn -B install > .logs/mvn-install.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-install.log"
wait $MVN_PID; echo "maven exit=$?"
```

### 8.2 AC008 — the `set-version.sh` chain test

Run on the task branch with a clean tree, against a version that could never be real, so that a
missed file is obvious and a forgotten restore cannot be mistaken for real work:

```bash
git status --porcelain                      # must print nothing before starting
./set-version.sh 9.9.9-CHAINTEST-SNAPSHOT

# every POM in the reactor must carry it - root <version>, submodules <parent><version>
git ls-files '*pom.xml' | wc -l             # 13
git grep -l '9.9.9-CHAINTEST-SNAPSHOT' -- '*pom.xml' | wc -l
git diff --stat -- '*pom.xml'

git checkout -- .
git status --porcelain                      # must print nothing again
```

The test passes when the two counts are both **13** and the tree is clean at both ends. It also
covers the guard: `./set-version.sh` with no argument must print usage and exit 1 without invoking
Maven, and `./set-version.sh a b` likewise.

If the counts disagree — `release:update-versions` leaving a module behind — the script is switched
to the `versions:set -DprocessAllModules=true` form given in §5.2 and the test re-run unchanged.
That is a substitution the AC anticipates, not a failure of it.

### 8.3 AC002 and AC004 — the staging dispatch

`project-staging.yml` checks out whatever ref it is given and pushes with `DEPLOY_TOKEN`, so the
task branch can be exercised while the PR is open, and the generated commit lands inside the PR:

```bash
gh workflow run staging.yml -f branch_name=issue-97-resolve-orphaned-travis-tooling
gh run watch
git pull
```

Expected, on the task branch:

1. a commit `Auto-generated README.md [skip jenkins]`, author `github-actions[bot]`;
2. `sed -n '7p' README.md` reading `0.3.0-SNAPSHOT - RC<n> - <yyyyMMdd-HHmmss>`;
3. `grep -c '\${' README.md` returning **0**;
4. the `### Version 0.3.0-SNAPSHOT` table listing that milestone's closed issues, not the
   2020 snapshot of `#14`;
5. the §5.3 `<server>` section present in the generated `README.md`.

Two things to know before dispatching. The run publishes `0.3.0-SNAPSHOT` RC artifacts from a task
branch, so a later staging run on the real RC may hit an artifact conflict — `project-staging.yml`
already treats HTTP 409 as a warning and continues. And the run deploys the site to `gh-pages`;
that path is currently healthy (last publish 2026-09-15), which is part of why this workflow is the
right one to test with.

The dispatch is repeated against `staging-0.3.0-SNAPSHOT-RC` after the PR merges, to confirm the
behaviour on the branch that will actually be released.

### 8.4 The upstream regression check

parent-poms must keep regenerating its own README. After the `deploy.yml` snapshot dispatch of
§4.5, its `master` must carry a fresh `Auto-generated README.md [skip jenkins]` commit, and
`sed -n '7p' README.md` there must still read `3.8.0-SNAPSHOT - <n> - <timestamp>`. If the profile
move broke parent-poms' own generation, this is where it shows.

### 8.5 What is not tested by this story

`release.yml` and `hotfix.yml` inherit the same fix and are exercised when 0.3.0 is cut. They are
not dispatched here: a release workflow deletes the RC branch, merges to `master` and creates a
hotfix line, none of which is reversible for the sake of a test. The staging path exercises the
same four executions through the same profile, and §4.4 records what remains unproven.

## 9. Out of scope

- **The Travis estate itself** — `#92`, delivered by PR `#96`.
- **`connect-mongo.sh` and `connect-mongo-super-user.sh`** — orphaned by nothing; they are Mongo
  shell helpers and `#97` was never about them.
- **The `setup-java@v4` deprecation warning** in `.github/workflows/ci.yml` — real, unrelated, and
  raised separately if it matters.
- **Releasing `parent-poms`** — milestone `3.8.0-SNAPSHOT` still carries `#59`, `#65` and `#67`, and
  `CLAUDE.md` forbids releasing a milestone that is not cleared. `#97` ships against the deployed
  snapshot; see §4.5.
- **The `Update README.md on Master` step's other assumption** — that `process-resources` alone is
  enough to regenerate a README on `master` after a release. This story makes the step functional
  for the first time; whether its placement in `project-release.yml` is also correct is a question
  for the release that first exercises it.
- **Changing what the generated README contains.** The issue tables, their column set and the
  milestone filter are inherited configuration and stay as they are. This story restores
  generation; it does not redesign the output.

---
issue: 48
slug: investigate-spring-and-maven-profiles
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 48 — Investigate how to use profiles (dev, staging, production) with Spring and Maven

## 1. Story

**As a** developer about to write ADR-003 (the pipeline) and ADR-004 (distribution)
**I want** measured evidence of how Spring profiles, Maven and deploy-time values should share the
work of configuring DSH by capability and by tier, with a convention proposed from that evidence
**So that** the ADRs choose transports, persistence and deployment names on observed behaviour, not
on assumption

## 2. Context

- Wave: 1, step 2 of the order agreed on 2026-10-06 (`specs/product/PRD.md` §4, Wave 1), DSH
  milestone `0.4.0-SNAPSHOT`. `#48` comes before task 2 (ADR-003), which cites its findings.
- Issue: [#48](https://github.com/MRISS-Projects/dsh/issues/48). Its body is empty, so this spec
  carries the story and the acceptance criteria. §8 holds a body to post on the issue.
- Parent branch: `DEVELOP`, at `a760552f8` when this spec was written. The task branch is
  `issue-48-investigate-spring-and-maven-profiles`.
- **This story changes no production code, no POM and no workflow.** Its output is one document,
  `specs/architecture/profiles-and-configuration.md`: findings, each backed by a probe that was
  run, and a convention that is proposed and not implemented.

### 2.1 What was established while writing this spec

All on `DEVELOP` at `a760552f8`, on 2026-10-06 and 2026-10-07.

| Fact | Evidence |
|---|---|
| Spring profiles are not used. No `@Profile`, no `application-<profile>.properties`, no `spring.profiles.*` setting in any module | a grep over every `pom.xml`, `*.java`, `*.properties` and `*.yml` |
| One activation exists and selects nothing: `--spring.profiles.active=test` | `.github/workflows/api-testing.yml:240`, and the row above |
| The Maven profiles select build behaviour, not an environment: `deployment`, `product-release-deployment`, `coverage-badge`, `http-integration-tests`. All are activated by property (`-D`); this estate dropped `-P` | the `<profile>` blocks in `pom.xml`, `dsh-coverage-report/pom.xml`, `dsh-rest-api/pom.xml`; `specs/features/pom-hierarchy-migration.md` FR002 |
| Environment values are fixed at build time. `dsh-data/src/main/resources/mongo.properties` holds `mongo.host=${mongo.host}` and three more, filled by Maven resource filtering from `settings.xml` | the file; `docs/devops/README.md`, "How build properties reach a release build" |
| That is why six workflows carry dummy `mongo.*` values, and why a build without them fails with `Circular placeholder reference 'mongo.port'` | `ci.yml`, `api-testing.yml`, `deploy.yml`, `staging.yml`, `release.yml`, `hotfix.yml`; the same README section |
| The jar still takes the values at run time: `--mongo.port=…` overrides the filtered file | `dsh-rest-api/pom.xml:277-281`, proved by `#46` |
| Every module filters `src/main/resources` with Maven's default delimiters, `${…}` and `@…@`. Neither parent POM configures delimiters, and DSH does not inherit `spring-boot-starter-parent`, which is what normally restricts filtering to `@…@` | `dsh-rest-api/pom.xml:113-120`; a grep for `delimiter` over `products-3.10.0.pom` and `mriss-parent-3.10.0.pom` |
| Spring Boot is `2.7.18`, managed in parent-poms `products/pom.xml`. `#120` will move it | `products-3.10.0.pom:80` |
| MongoDB and RabbitMQ are wired in XML, through `@ImportResource`, with no condition on them | `dsh-data/…/dshApplicationContext.xml`, `dsh-rest-api/…/enqueue-docId-context.xml` |
| `dsh-rest-api`'s test `application.properties` repeats the main one line for line, plus one setting | the two files |
| Layer 4, the Postman collections, does nothing today. `specs/api/postman/` holds only a `README.md`, and the workflow prints "No Postman collections found" | `api-testing.yml:252-260` |
| Layer 3's tests already take their target from `dsh.it.baseUrl` | `dsh-rest-api/pom.xml:365`; `.github/copilot/rules/testing-patterns.md`, "Layer 3" |
| The PRD already plans a capability profile: `@Profile("gcp")` in Waves 4 and 5, and "make `gcp` the default profile" in Wave 7 | `specs/product/PRD.md` §4 |

### 2.2 Decisions agreed with the owner on 2026-10-07

These frame the investigation. The findings test them; they do not assume them.

1. **Findings only.** The findings document also proposes a skeleton convention. Nothing in it is
   implemented by this story.
2. **Three tiers are planned**, in one GCP project: `dsh-dev`, deployed from `DEVELOP` by
   `deploy.yml`; `dsh-staging`, deployed from the release-candidate branch; and `dsh`, deployed by
   a release or a hotfix. An end user's own deployment is also named `dsh`.
3. **One project, not three.** Each tier is told apart by name, service by service: the Cloud Run
   service name, the Firestore database name, and so on.
4. **One bucket, with one sub-folder per tier.**
5. **The tier is a deployment value, not a build input.** Spring profiles select a capability. The
   same artifact serves every tier. The owner's earlier practice, Maven profiles that set the
   Spring profile and the names at build time, is compared against this on evidence (§3.3).
6. **A fifth test layer** runs over the wire against a real GCP deployment. Every test that reads
   or writes a document runs against `dsh-dev` or `dsh-staging`, never against production. During
   a release or a hotfix, `dsh-staging` is where those tests run. Production gets a smoke check
   that touches no data at all: it asks for the health, the version and the commit, and nothing
   else. The owner confirmed this on 2026-10-07: production data is never used by a test.
7. **A release is gated on the commit, not on the clock.** The staging deployment that passed
   layer 5 must have been built from the head of the branch being released.
8. **A hotfix behaves exactly like a release.** Only the branch it starts from differs.
9. **One image.** The same container image runs locally against emulators and on Cloud Run against
   the real services. Cloud Run needs an image, not a Dockerfile.
10. **GCP-free for now.** No GCP project exists yet. Probes run locally; GCP facts are cited from
    documentation and labelled as not verified by a run.
11. **Revisions and images are named from the application version**, with the dots written as
    dashes, because a Cloud Run revision name takes no dot. A snapshot adds `-dev-<build number>`
    and a release candidate adds `-rc-<build number>`:

    | Tier | Service | Revision suffix | Revision | Built by | From |
    |---|---|---|---|---|---|
    | development | `dsh-dev` | `0-4-0-dev-54` | `dsh-dev-0-4-0-dev-54` | `deploy.yml` | `DEVELOP`, at `0.4.0-SNAPSHOT` |
    | staging | `dsh-staging` | `0-4-0-rc-54` | `dsh-staging-0-4-0-rc-54` | `staging.yml` | the RC branch, at `0.4.0-SNAPSHOT` |
    | production | `dsh` | `0-4-0` | `dsh-0-4-0` | `release.yml` or `hotfix.yml` | the release tag, `v0.4.0` |

    The owner's rule, given on 2026-10-07, with four points settled while recording it:

    - **`rc`, not `RC`.** The findings confirm from documentation (G1) that a revision name is
      lower case only.
    - **Every name starts with `dsh`, and the service name is what puts it there.** The owner
      asked for the prefix because a name may not start with a digit. A revision's name is its
      service's name followed by the suffix, so the prefix is already present, and adding `dsh`
      to the suffix would repeat it. An image is `dsh:<tag>`, so its name has it too. The findings
      confirm both from documentation (G1, G7), and whether a traffic tag may start with a digit
      (G5), which decides whether the smoke check's tag needs a letter in front.
    - **The build number already exists.** parent-poms `project-staging.yml` passes
      `-Dbuild.number`, `RC<n>` for a release candidate and `<n>` for a snapshot, defaulting to
      `GITHUB_RUN_NUMBER`. The names reuse it; they do not introduce a second counter.
    - **Production is built from the tag, not from `master`.** `release:perform` checks out the
      tag `v<version>` and builds it, and the workflow merges that tag into `master` afterwards
      (parent-poms `project-release.yml`). So the production image is a rebuild. Its commit is
      the release commit, which `release:prepare` adds on top of the commit staging tested, and
      the two differ only in the POM versions. Decision 7's gate therefore compares the branch
      head when the release is dispatched, and the production smoke check covers the rebuild.
12. **Nothing stays alive when it is not in use.** Costs are restricted. Every Cloud Run service
    runs with a minimum of zero instances, in every tier, and that is the model for every other
    service: wherever a service offers the choice, it is configured to cost nothing while idle.
    Cold starts are accepted, and reduced with the options Cloud Run offers for them.

    **The owner asked for this rule to stand in `CLAUDE.md` as a restriction**, so that it binds
    every proposal and every configuration from now on, before ADR-004 exists. This story adds it
    (§3.9). The settings service by service go to ADR-004. The rule also reaches this story's
    findings in two ways:

    - **It constrains the transport that `gcp` selects.** A service with zero minimum instances
      is billed, and given CPU, while it handles a request. Work that continues after the response
      is sent is not guaranteed to run, and a message held in an in-memory channel is lost when
      the instance stops. So on GCP each pipeline stage has to be driven by a request, which
      points at Pub/Sub delivering by push to an HTTP endpoint, and away from a subscriber that
      polls. The findings confirm the billing model from documentation (G8) and hand the
      consequence to ADR-003. In-memory channels remain right for the default, local mode.
    - **It makes a standing `dsh-staging` affordable.** An idle tier costs its storage and nothing
      else, so staging can stay deployed between releases, which decision 7's gate relies on.

## 3. Design

### 3.1 The deliverable

`specs/architecture/profiles-and-configuration.md`, with these sections in this order:

1. **Summary.** The answers, in a page, before any evidence.
2. **DSH today.** §2.1's table, kept current.
3. **Findings: Spring.** S1 to S8 (§3.2).
4. **Findings: Maven.** M1 to M4 (§3.2).
5. **The tier model.** Three approaches compared (§3.3).
6. **Proposed convention.** Marked "Proposed, not in force; ADR-003 and ADR-004 decide" (§3.4).
7. **Test layers and configuration.** Layers 1 to 5 (§3.5).
8. **GCP naming in one project.** From documentation only (§3.6).
9. **Consequences for the waves, and follow-up candidates** (§3.7).
10. **Appendix: the probe harness**, so a reader can reproduce any finding.

Every finding in sections 3 and 4 has the same five parts:

| Part | Content |
|---|---|
| Answer | one or two sentences |
| Probe | the probe's id, the command, and the source lines that matter |
| Observed | the output, copied, not paraphrased |
| Prediction | "confirmed" or "refuted", against §3.2. A refuted prediction says what was expected |
| Consequence for DSH | what the convention must do because of it |

The header of the findings sections records Spring Boot `2.7.18`, the Maven and JDK versions
printed by `mvn -version`, and the date. `#120` changes the Boot line, so each finding that the
Boot 3 documentation contradicts says so.

### 3.2 The questions, and what each probe is predicted to show

The prediction is written here, before any probe runs. That is this story's red-then-green: a
prediction that fails is a finding, and it is reported as one. Probe ids are those of §5.3.

#### Spring

| Id | Question | Prediction |
|---|---|---|
| S1 | How is a profile activated, and which source wins? | An argument beats a system property, which beats an environment variable, which beats a value in the jar's `application.properties`. A later source replaces the list; it does not add to it. `spring.profiles.default` in `application.properties` works and is replaced by any activation |
| S2 | How do profile-specific files combine? | `application-<p>.properties` overrides `application.properties`. With two profiles, the one listed last wins. An `on-profile` document inside `application.properties` applies only under that profile. `spring.profiles.active` inside a profile-specific file stops the startup |
| S3 | Can one profile switch on another? | Yes, with `spring.profiles.group.<p>`. Activating `dev` activates `dev` then `gcp`, and `gcp`'s file wins where both set a key |
| S4 | How is one of two implementations chosen? | `@Profile("gcp")` and `@Profile("!gcp")` select exactly one bean. `@ConditionalOnProperty` selects per service, and a profile file can set that property, so a profile supplies defaults that one argument can override. A value that matches no implementation leaves no bean, and the startup fails only where something injects it |
| S5 | Do profiles and run-time values reach the XML contexts? | A nested `<beans profile="gcp">` is honoured in an `@ImportResource` file. A `${…}` in the XML takes an argument or an environment variable before the file named by `<context:property-placeholder>`. That file's values never reach the `Environment`, so they cannot drive a condition |
| S6 | What happens to configuration files in a library module? | Two `application.properties` on the classpath are not merged: the application's hides the library's whole file. The same holds for `application-gcp.properties`. A file named in `spring.config.import` is loaded with its profile variant, and it overrides the file that imports it. A `@PropertySource` file is loaded without a profile variant and loses to `application.properties` |
| S7 | How do deploy-time values reach the application? | `PROBE_NAME=dsh-dev` sets `probe.name`, and every `${probe.name}` derived from it follows at run time. An argument beats the environment variable. A required value with no default stops the startup and names the missing key. A key with a dash is reached by an environment variable with an underscore and by one without |
| S8 | What configuration does a test get? | A test `application.properties` hides the main one entirely, which is why `dsh-rest-api`'s repeats it. `application-test.properties` with `@ActiveProfiles("test")` merges instead. `@TestPropertySource` beats both. `@ActiveProfiles` beats a `spring.profiles.active` leaked from the shell or the Maven command line |

#### Maven

| Id | Question | Prediction |
|---|---|---|
| M1 | What does resource filtering do to a Spring placeholder? | With no Maven property of that name, `${probe.name}` survives into the jar. With one, it is replaced at build time and no run-time value can change it. The self-reference `x=${x}` reproduces DSH's `Circular placeholder reference`. Restricting filtering to `@…@` stops all three while `@project.version@` still works |
| M2 | What may the build legitimately stamp? | `spring-boot:build-info` writes the version and an extra `commit` property, and the running application reads both. That is what a release gate on the commit needs |
| M3 | How does a Maven run choose a profile without baking it in? | A `-Dspring.profiles.active=gcp` on the `mvn` command line, or `SPRING_PROFILES_ACTIVE` in the shell, reaches the test JVM. A test with no `@ActiveProfiles` then runs under `gcp` |
| M4 | What does a Maven profile per tier produce? | Two builds, `-Dtier=dev` and `-Dtier=staging`, give two jars whose configuration differs. One build run with two environment values gives the same two behaviours from one jar |

M3's other half, how `spring-boot:start` passes arguments and profiles, is not probed again. `#46`
proved it for arguments, and the plugin's `profiles` parameter is cited from its documentation.

### 3.3 The tier model: three approaches, one comparison

The findings compare three approaches on the criteria below, each cell citing a probe.

- **A. Maven profile per tier.** The build sets the Spring profile and the names. The owner's
  practice on earlier projects.
- **B. Spring profile per tier, in the jar.** `application-dev.properties` and its siblings, with
  a profile group that adds `gcp`. The deployment sets `SPRING_PROFILES_ACTIVE=dev`.
- **C. Capability profiles only; the tier is a value.** The deployment sets one base name, and the
  resource names derive from it at run time.

| Criterion | Why it matters here |
|---|---|
| One artifact for every tier | decision 5, and decision 7's gate means nothing otherwise |
| An end user deploys under a name of their own without rebuilding | decision 2; the bucket name cannot be DSH's to choose |
| Each resource name is defined once | the benefit approach A was chosen for elsewhere |
| A missing value fails at startup and names the key | today's failure is a circular-placeholder error that reads like a Spring bug |
| A new tier needs no change to the jar | a fourth name, such as a preview deployment |
| Works the same for a release and a hotfix | decision 8 |

The expected recommendation is C. The findings may recommend otherwise if the probes say so.

### 3.4 The convention the findings propose

A skeleton, written from the probes. The candidate below is what this spec expects; each line is
confirmed, changed or dropped by the finding named beside it.

- **Profiles name a capability, never a tier.** No profile means everything in memory and nothing
  on GCP reachable. `gcp` selects the GCP implementations. (S1, S4)
- **A profile supplies defaults; a property selects each implementation.** `gcp`'s file sets, for
  example, the transport and the persistence, and one value can override either. This is what
  lets a local run put some services on emulators and others on GCP. (S4)
- **An emulator is `gcp` pointed elsewhere**, by endpoint values, not a third set of beans. (S7)
- **The tier is one value, the base name**, defaulting to `dsh`. Names derive from it at run time:
  the Firestore database, the Pub/Sub topics and subscriptions, and the object prefix inside the
  bucket. The bucket name is a separate, required value with no default. (S7, §3.6)
- **Only the deployable module owns `application*.properties`.** What a library module uses
  instead follows from S6.
- **The build never supplies an environment value.** It stamps the version and the commit, and
  nothing else. Spring configuration is filtered with `@…@` only, or not at all. (M1, M2)
- **How "make `gcp` the default" (Wave 7) should be done**, given that `@Profile("!gcp")` then
  needs some other profile to be named. (S1)
- **What becomes of `mongo.*`** and of the `test` profile that selects nothing. (M1, §2.1)

The property namespace, the profile names and the file layout are shown as a tree and a table of
keys. ADR-003 owns the final names.

### 3.5 Test layers and configuration

A table, one row per layer, saying which configuration the layer runs with and where its values
come from. Layers 1 to 4 are described as they are; the row for layer 5 is new:

| Layer | Target | Configuration |
|---|---|---|
| 5a. Acceptance, deployed | `dsh-dev` after a `DEVELOP` deploy; `dsh-staging` after a staging deploy | the layer-3 or layer-4 tests with their base URL pointed at the deployment; they write data |
| 5b. Smoke, production | `dsh`, after a release or hotfix deploy | health, and the released version and commit (M2). It reads and writes no document |

Both rows start from a stopped service (decision 12), so their first request is a cold start, and
their timeouts must allow for one.

The section also states the gate from decisions 7 and 8 in one paragraph, identically for release
and hotfix, and names the open points it leaves to `#148` and ADR-004: how a hotfix branch reaches
`dsh-staging`, how layer 5 authenticates to Cloud Run, and how the tests clean up what they write.

### 3.6 GCP naming in one project, from documentation

No probe runs against GCP. Each row cites a documentation page by URL and access date, quotes the
constraint, and carries the label **Not verified by a run**.

| Id | Fact to establish |
|---|---|
| G1 | Cloud Run: the rules for a service name and its scope (project and region); the rules for a revision name and its suffix, including case, length, the first character, and whether a suffix can be used twice in one service |
| G2 | Firestore: more than one database in a project, the rules for a database id, whether the emulator supports named databases, and how Spring Cloud GCP selects one |
| G3 | Pub/Sub: the rules for topic and subscription names |
| G4 | Cloud Storage: bucket names are global; what an object prefix is; how access and lifecycle can be scoped to a prefix |
| G5 | Cloud Run: deploying a revision with no traffic, and reaching it by a tag; the rules for a tag, including its first character |
| G6 | Which of these services have an official emulator |
| G7 | Container images: which characters a tag takes in Artifact Registry and in GHCR, including its first character, and so whether an image can carry the version with its dots |
| G8 | Cost while idle. Cloud Run: minimum instances, when CPU is allocated and billed, and the options that shorten a cold start. For Firestore, Pub/Sub, Cloud Storage and Artifact Registry: what an unused tier costs, whether a free quota covers every Firestore database or one per project, and what removes old objects and images automatically |

From these, the section gives the naming table for one project: for each service, the name each of
`dsh-dev`, `dsh-staging` and `dsh` gets, and the rule that produces it from the base name. It adds
decision 11's revision and image names, and it answers three questions that rule leaves open:

- **Whether the image tag keeps the dots.** If G7 allows them, the findings weigh one dashed
  string for both against a public image tagged `0.4.0`, which is what a user pulling it expects.
- **What happens when a name is reused.** A re-run of a workflow keeps its run number, and a
  second deploy of an unchanged release keeps its version. G1 says whether either collides.
- **Where the commit is recorded.** The build number does not identify a commit, and decision 7's
  gate needs one. The candidates are M2's build stamp and a label on the revision.

It also records one consequence of decision 4 as a constraint for ADR-004. Access to a bucket is
granted on the bucket, so three tiers in one bucket are isolated from each other only if access is
scoped to the prefix (G4). Without that, the `dsh-dev` service can overwrite production's objects.

### 3.7 Consequences for the waves, and follow-up candidates

The findings document lists these. **This story edits neither the PRD nor any issue.** The PRD
changes go through `dsh-plan-wave` after the findings merge, and an issue is created only after
the owner approves its text.

Consequences to list, each with the decision or finding behind it:

- Wave 4 needs somewhere real to run its first `gcp` code. The container image and a minimal deploy
  move forward from Wave 8, which keeps the end-user packaging.
- ADR-004 gains the operated tiers as three runs of the same deploy, the single project, the
  bucket's prefix isolation, and the single image.
- ADR-004 gains decision 12 as a rule with a table: for each service, the setting that makes it
  cost nothing while idle, and the retention that removes old `dev` and `rc` images, revisions and
  test objects. Anything with a fixed hourly cost, such as a load balancer in front of the
  service, has to be justified against it.
- ADR-003 gains decision 12's constraint on the `gcp` transport: stages driven by a request.
  Wave 5's "Pub/Sub channel adapters" is restated once the ADR decides.
- `testing-patterns.md` gains layer 5 when ADR-004 lands.
- A deploy job that runs after the reusable staging or release workflow needs the build number and
  the version from it. Whether those are outputs today is checked, and if not it is a parent-poms
  issue beside `parent-poms#91`.
- `#148`, the release skill, gains the commit gate and the production smoke check, for release and
  hotfix alike.
- Wave 7's tasks 1 and 3 are restated in terms of the convention.

Follow-up candidates, each with a one-paragraph draft:

- Move `mongo.*` from build-time filtering to run-time configuration, or record why it should wait
  for the wave that removes MongoDB.
- Remove `--spring.profiles.active=test` from `api-testing.yml`, or fold it into `#137`.
- Restrict resource filtering of Spring configuration to `@…@`, if M1 confirms the hazard.

### 3.8 Out of scope

- Any change to a POM, a workflow, a `*.properties` file, a Spring context or a Java class.
- The probe harness is not committed. It lives in the session scratchpad; the appendix of the
  findings document holds its sources.
- Re-running the probes on a Spring Boot 3 line. That is `#120`'s to decide.
- The GCP project, IAM, the Dockerfile, the image and the deploy script: ADR-004.
- The pipeline's structure and the final profile and property names: ADR-003.
- The PRD.

### 3.9 The cost rule in `CLAUDE.md`

A new section, placed after "Quality gates" and before "The development process". It is a
restriction on what may be written or proposed, which is what `CLAUDE.md` is for; the reasons and
the per-service settings stay out of it. The text:

```markdown
## Cost rule for GCP

**Nothing stays alive when it is not in use.** Costs are restricted. Any GCP configuration written
or proposed in this repository, for any tier, takes the setting that costs nothing while idle
wherever the service offers one.

- **Cloud Run: minimum instances is 0, always.** Cold starts are accepted. Reduce them with Cloud
  Run's own options, never by keeping an instance warm.
- **Every other service follows the same rule.** Prefer what is billed by use over what is billed
  by the hour.
- **Stored data is in use.** Storage billed by what is kept is allowed. Set a retention, so that
  test data does not accumulate.
- **An exception needs the owner's approval before it is written down.** That covers a service
  with no such setting, and anything with a fixed cost while idle. Say what it costs and why it
  cannot be avoided.
```

## 4. Files to change

| File | Change |
|---|---|
| `specs/architecture/profiles-and-configuration.md` | created: the findings and the proposed convention |
| `CLAUDE.md` | one row in the router table, pointing at the new document as "Proposed profile and configuration convention"; and the "Cost rule for GCP" section of §3.9 |
| `specs/stories/48-investigate-spring-and-maven-profiles.md` | §10, the verification results, added by the build |

## 5. The probe harness

A two-module Maven project shaped like DSH: `lib` stands for `dsh-data` (a library with an XML
context and a filtered properties file), `app` for `dsh-rest-api` (the deployable). It prints one
report, and every probe is that report under different arguments, environment or files.

It imports `spring-boot-dependencies:2.7.18`, as parent-poms `products/pom.xml` does, and has no
parent, so it inherits neither the coverage gate nor `spring-boot-starter-parent`'s filtering
rules. Both modules filter `src/main/resources` with the default delimiters, as DSH's do.

### 5.1 Where it lives, and the guards

```bash
# env.sh - sourced at the top of every probe command
PROBES="<session scratchpad>/profile-probes"
JAVA17="$HOME/apps/jdk-17.0.20.1+1/bin/java"   # `java` on PATH is not the JDK Maven uses
cd "$PROBES" || exit 1
case "$(pwd)" in */github/dsh*) echo "refusing to run in the repository"; exit 1;; esac
build() { mkdir -p logs; mvn -B clean package "$@" > "logs/${LOG:-build}.log" 2>&1; echo "maven exit=$?"; }
run()   { "$JAVA17" -jar app/target/app-1.jar "$@" 2>&1 | grep -E '^PROBE|FAILED|Exception|Description:|Action:|Reason:'; }
```

- The project is its own git repository (`git init` inside `$PROBES`). The base is one commit, and
  each variation of §5.3 is applied to a clean tree and reverted with `git checkout . && git clean
  -fd -e logs`.
- Nothing is installed: every build stops at `package`, so no `probe:*` artifact reaches
  `D:\.m2\repository`.
- Every build writes a log and prints its exit code, as `CLAUDE.md` requires of a local `mvn`.

### 5.2 The base sources

`pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>probe</groupId>
  <artifactId>profile-probes</artifactId>
  <version>1</version>
  <packaging>pom</packaging>
  <modules>
    <module>lib</module>
    <module>app</module>
  </modules>
  <properties>
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-boot.version>2.7.18</spring-boot.version>
    <probe.commit>unknown</probe.commit>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

`lib/pom.xml`. The XML context is excluded from filtering, as `dsh-data` excludes its own:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>probe</groupId>
    <artifactId>profile-probes</artifactId>
    <version>1</version>
  </parent>
  <artifactId>lib</artifactId>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
    </dependency>
  </dependencies>
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <excludes>
          <exclude>lib-context.xml</exclude>
        </excludes>
        <filtering>true</filtering>
      </resource>
      <resource>
        <directory>src/main/resources</directory>
        <includes>
          <include>lib-context.xml</include>
        </includes>
        <filtering>false</filtering>
      </resource>
    </resources>
  </build>
</project>
```

`app/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>probe</groupId>
    <artifactId>profile-probes</artifactId>
    <version>1</version>
  </parent>
  <artifactId>app</artifactId>
  <dependencies>
    <dependency>
      <groupId>probe</groupId>
      <artifactId>lib</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>build-info</goal>
              <goal>repackage</goal>
            </goals>
            <configuration>
              <additionalProperties>
                <commit>${probe.commit}</commit>
              </additionalProperties>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

`lib/src/main/java/probe/lib/`, one type per file:

```java
package probe.lib;
public interface Store { }

package probe.lib;
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Profile("!gcp")
public class InMemoryStore implements Store { }

package probe.lib;
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Profile("gcp")
public class GcpStore implements Store { }

package probe.lib;
public interface Transport { }

package probe.lib;
@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "probe.transport", havingValue = "memory", matchIfMissing = true)
public class MemoryTransport implements Transport { }

package probe.lib;
@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "probe.transport", havingValue = "pubsub")
public class PubSubTransport implements Transport { }

package probe.lib;
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.ImportResource("classpath:/lib-context.xml")
@org.springframework.context.annotation.PropertySource("classpath:/lib-sourced.properties")
public class LibConfig { }
```

`lib/src/main/resources/lib-context.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd">

  <context:property-placeholder location="classpath:/lib.properties" />

  <bean id="xmlAlways" class="java.lang.String">
    <constructor-arg value="${probe.lib.host}" />
  </bean>

  <beans profile="gcp">
    <bean id="xmlGcpOnly" class="java.lang.String">
      <constructor-arg value="gcp" />
    </bean>
  </beans>
</beans>
```

`lib/src/main/resources/`, the properties files:

| File | Content |
|---|---|
| `lib.properties` | `probe.lib.host=from-lib-properties` |
| `application.properties` | `probe.origin=lib` and `probe.only-in-lib=yes` |
| `application-gcp.properties` | `probe.lib-gcp=yes` |
| `lib-imported.properties` | `probe.imported=yes` and `probe.precedence=imported` |
| `lib-imported-gcp.properties` | `probe.imported-gcp=yes` |
| `lib-sourced.properties` | `probe.sourced=yes` and `probe.precedence=sourced` |
| `lib-sourced-gcp.properties` | `probe.sourced-gcp=yes` |

`app/src/main/resources/application.properties`:

```properties
spring.main.banner-mode=off
logging.level.root=WARN
spring.config.import=classpath:lib-imported.properties
spring.profiles.group.dev=gcp
probe.origin=app
probe.only-in-app=yes
probe.layer=base
probe.name=dsh
probe.database-id=${probe.name}
probe.prefix=${probe.name}/
probe.precedence=app
#---
spring.config.activate.on-profile=gcp
probe.doc=gcp-doc
```

`app/src/main/resources/`, the profile files:

| File | Content |
|---|---|
| `application-gcp.properties` | `probe.layer=gcp` and `probe.transport=pubsub` |
| `application-dev.properties` | `probe.layer=dev` and `probe.name=dsh-dev` |
| `application-other.properties` | `probe.layer=other` |

`app/src/main/java/probe/app/ProbeReport.java`:

```java
package probe.app;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import probe.lib.Store;
import probe.lib.Transport;

@Component
public class ProbeReport {

    private static final String[] KEYS = { "probe.origin", "probe.only-in-app", "probe.only-in-lib",
            "probe.layer", "probe.doc", "probe.name", "probe.database-id", "probe.prefix", "probe.bucket",
            "probe.transport", "probe.lib-gcp", "probe.imported", "probe.imported-gcp", "probe.sourced",
            "probe.sourced-gcp", "probe.precedence", "probe.lib.host" };

    private static final String[] XML_BEANS = { "xmlAlways", "xmlGcpOnly" };

    private final Environment env;
    private final ApplicationContext ctx;
    private final ObjectProvider<BuildProperties> build;

    public ProbeReport(Environment env, ApplicationContext ctx, ObjectProvider<BuildProperties> build) {
        this.env = env;
        this.ctx = ctx;
        this.build = build;
    }

    public List<String> lines() {
        List<String> out = new ArrayList<>();
        out.add("PROBE active=" + String.join(",", env.getActiveProfiles()));
        out.add("PROBE default=" + String.join(",", env.getDefaultProfiles()));
        for (String key : KEYS) {
            out.add("PROBE " + key + "=" + value(key));
        }
        out.add("PROBE stores=" + new TreeSet<>(ctx.getBeansOfType(Store.class).keySet()));
        out.add("PROBE transports=" + new TreeSet<>(ctx.getBeansOfType(Transport.class).keySet()));
        for (String bean : XML_BEANS) {
            out.add("PROBE bean " + bean + "=" + (ctx.containsBean(bean) ? ctx.getBean(bean) : "<absent>"));
        }
        BuildProperties b = build.getIfAvailable();
        out.add("PROBE build.version=" + (b == null ? "<absent>" : b.getVersion()));
        out.add("PROBE build.commit=" + (b == null ? "<absent>" : b.get("commit")));
        return out;
    }

    private String value(String key) {
        try {
            return env.getProperty(key, "<unset>");
        } catch (IllegalArgumentException e) {
            return "<unresolvable: " + e.getMessage() + ">";
        }
    }
}
```

`app/src/main/java/probe/app/ProbeApplication.java`:

```java
package probe.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "probe")
public class ProbeApplication implements CommandLineRunner {

    private final ProbeReport report;

    public ProbeApplication(ProbeReport report) {
        this.report = report;
    }

    public static void main(String[] args) {
        SpringApplication.run(ProbeApplication.class, args);
    }

    @Override
    public void run(String... args) {
        report.lines().forEach(System.out::println);
    }
}
```

`app/src/main/java/probe/app/StrictConfig.java`, which exists only when `probe.strict` is set, so
that a required value and a required bean can be made to fail on demand:

```java
package probe.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import probe.lib.Transport;

@Configuration
@ConditionalOnProperty("probe.strict")
public class StrictConfig {

    public StrictConfig(@Value("${probe.bucket}") String bucket, Transport transport) {
        System.out.println("PROBE strict bucket=" + bucket + " transport=" + transport.getClass().getSimpleName());
    }
}
```

`app/src/test/java/probe/app/ProbeContextTest.java`. The runner prints the report when the context
starts, so the test needs no body:

```java
package probe.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProbeContextTest {

    @Test
    void contextLoads() {
    }
}
```

The harness uses JUnit 5, Boot 2.7's default. DSH's tests are JUnit 4 until `#119`; profile and
property handling in a test context does not depend on the engine.

### 5.3 The probes

`run` is §5.1's function. "Rebuild" means `build` after the edit, and the tree is reverted after.
Where a probe starts the application more than once, each start is a lettered step.

**The baseline, P0.** `build`, then `run`. It must print exactly this. If it does not, stop: the
difference is either a defect in the harness or a finding, and it must be understood before any
other probe is trusted.

```text
PROBE active=
PROBE default=default
PROBE probe.origin=app
PROBE probe.only-in-app=yes
PROBE probe.only-in-lib=<unset>
PROBE probe.layer=base
PROBE probe.doc=<unset>
PROBE probe.name=dsh
PROBE probe.database-id=dsh
PROBE probe.prefix=dsh/
PROBE probe.bucket=<unset>
PROBE probe.transport=<unset>
PROBE probe.lib-gcp=<unset>
PROBE probe.imported=yes
PROBE probe.imported-gcp=<unset>
PROBE probe.sourced=yes
PROBE probe.sourced-gcp=<unset>
PROBE probe.precedence=imported
PROBE probe.lib.host=<unset>
PROBE stores=[inMemoryStore]
PROBE transports=[memoryTransport]
PROBE bean xmlAlways=from-lib-properties
PROBE bean xmlGcpOnly=<absent>
PROBE build.version=1
PROBE build.commit=unknown
```

**Run-only probes**, against the baseline jar:

| Probe | Command | Answers |
|---|---|---|
| P1 | `run --spring.profiles.active=gcp` | S1, S2, S4, S5, S6 |
| P2 | `SPRING_PROFILES_ACTIVE=gcp run` | S1 |
| P3 | `"$JAVA17" -Dspring.profiles.active=gcp -jar app/target/app-1.jar` | S1 |
| P4 | `SPRING_PROFILES_ACTIVE=gcp run --spring.profiles.active=other` | S1 |
| P5 | `run --spring.profiles.active=gcp,other`, then `other,gcp` | S2 |
| P6 | `run --spring.profiles.active=dev` | S3 |
| P7 | `run --spring.profiles.active=gcp --probe.transport=memory` | S4 |
| P8 | a. `run --probe.transport=bogus`; b. the same with `--probe.strict=true --probe.bucket=b` | S4 |
| P9 | a. `run --probe.lib.host=from-arg`; b. `PROBE_LIB_HOST=from-env run` | S5 |
| P10 | a. `PROBE_NAME=dsh-dev run`; b. the same with `--probe.name=dsh-arg` | S7 |
| P11 | a. `run --probe.strict=true`; b. `PROBE_BUCKET=b run --probe.strict=true` | S7 |
| P12 | a. `PROBE_DATABASE_ID=x run`; b. `PROBE_DATABASEID=y run` | S7 |
| P13 | `PROBE_PRECEDENCE=env run` | S6, S7 |

**Probes that change the tree:**

| Probe | Change | Then | Answers |
|---|---|---|---|
| P14 | add `spring.profiles.active=gcp` to `app`'s `application.properties`, above the `#---` line | rebuild; `run`; `run --spring.profiles.active=other` | S1 |
| P15 | add `spring.profiles.default=gcp` in the same place | rebuild; `run`; `run --spring.profiles.active=other` | S1 |
| P16 | add `spring.profiles.active=other` to `app`'s `application-gcp.properties` | rebuild; `run --spring.profiles.active=gcp`, keeping the whole output | S2 |
| P17 | delete `app`'s `application-gcp.properties` | rebuild; `run --spring.profiles.active=gcp` | S6 |
| P18 | none | `build -Dprobe.name=baked`; `unzip -p app/target/app-1.jar BOOT-INF/classes/application.properties`; `PROBE_NAME=dsh-dev run` | M1 |
| P19 | change `lib.properties` to `probe.lib.host=${probe.lib.host}` | rebuild; `run`, keeping the whole output; `run --probe.lib.host=from-arg`; then `build -Dprobe.lib.host=baked` and `run` | M1 |
| P20 | in both modules, configure `maven-resources-plugin` with `<useDefaultDelimiters>false</useDefaultDelimiters>` and the single delimiter `@`; add `probe.version=@project.version@` to `app`'s `application.properties` | `build -Dprobe.name=baked`; the same `unzip`; `PROBE_NAME=dsh-dev run` | M1 |
| P21 | none | `build -Dprobe.commit=abc1234`; `run` | M2 |
| P22 | none | a. `LOG=p22a build -Dspring.profiles.active=gcp`; b. `SPRING_PROFILES_ACTIVE=gcp LOG=p22b build`. Read the test's `PROBE` lines in each log | M3 |
| P23 | add `@ActiveProfiles("other")` to `ProbeContextTest` | the two builds of P22 again | S8, M3 |
| P24 | add `app/src/test/resources/application.properties` holding `probe.origin=test` | rebuild; read the test's `PROBE` lines | S8 |
| P25 | add `app/src/test/resources/application-test.properties` holding `probe.origin=test`, and `@ActiveProfiles("test")` | rebuild; read the test's `PROBE` lines | S8 |
| P26 | P25, plus `@TestPropertySource(properties = "probe.origin=inline")` | rebuild; read the test's `PROBE` lines | S8 |
| P27 | in `app/pom.xml`, two profiles activated by the property `tier` (`dev`, `staging`), each setting `probe.tier.name`; add `tier.properties` holding `probe.baked=@probe.tier.name@` | `build -Dtier=dev`, then `build -Dtier=staging`; `unzip -p` the file from each jar | M4 |

For P27, the comparison is of the file's content. Two jars differ byte for byte after any two
builds, because `build-info` records the time, so the jars' checksums prove nothing.

P22 and P24 to P26 read the report from the build log, where surefire prints the test's output.

## 6. Tasks

There is no Java under test in this repository. The red check for each finding is its prediction
in §3.2, written before the probe ran.

- [x] **T1 — build the harness and prove it.**
      1. Create `$PROBES` in the session scratchpad from §5.2, with `env.sh` from §5.1. `git init`,
         and commit the base.
      2. `mvn -version`; record the Maven and JDK lines.
      3. Run P0. Expected: §5.3's baseline, line for line.
      4. If it differs, stop and resolve it before T2. Record the difference either way.
- [x] **T2 — run P1 to P13.** For each, save the output to `logs/p<n>.txt`. Mark each prediction
      of §3.2 confirmed or refuted as it comes in. Do not adjust a probe to make a prediction pass;
      a probe changes only if it failed to ask its question, and the change is recorded.
- [x] **T3 — run P14 to P27.** One at a time, each from a clean tree, reverting after. Save each
      output. After the last, `git status` in `$PROBES` shows a clean tree.
- [x] **T4 — establish G1 to G8.** Read the documentation pages. Record for each the URL, the date
      and the quoted constraint. Anything the documentation does not settle is recorded as open.
- [x] **T5 — write `specs/architecture/profiles-and-configuration.md`**, in §3.1's order.
      1. Sections 2 to 4 and the appendix first, from the saved outputs.
      2. Then section 5, the comparison of §3.3, citing probes in every cell.
      3. Then sections 6 to 9, from §3.4 to §3.7.
      4. Section 1, the summary, last.
      5. Run the checks of §7.1 and §7.2. Expected: all hold.
      6. Commit: `docs(#48): findings on Spring and Maven profiles, with a proposed convention`.
- [x] **T6 — `CLAUDE.md`.**
      1. Add the row to the router table. Commit:
         `docs(#48): route CLAUDE.md to the profile findings`.
      2. Add §3.9's section, word for word, after "Quality gates". Commit:
         `docs(#48): state the GCP cost rule in CLAUDE.md`.
      3. Run markdownlint with `export PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"` and the
         command in `CLAUDE.md`. Expected: no findings.
- [x] **T7 — gates and record.**
      1. `mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as `CLAUDE.md` requires.
         Expected: exit 0. Gate 3 does not apply: no code changed.
      2. Run §7.3's check.
      3. Add §10 to this spec: the versions, the baseline result, a table of every prediction with
         its outcome, and the gate results. Lint. Commit: `docs(#48): record the verification`.
- [x] **T8 — hand over.** Show the owner the follow-up candidates of §3.7 and the issue body of §8.
      Post the body, and open any issue, only after the owner approves the text.

## 7. Verification

### 7.1 Every finding is backed

- Each of S1 to S8 and M1 to M4 has the five parts of §3.1, and its "Observed" part is output
  copied from a file under `$PROBES/logs`.
- Every probe id from P0 to P27 is cited by at least one finding:
  `for n in $(seq 0 27); do grep -q "P$n\b" specs/architecture/profiles-and-configuration.md || echo "P$n uncited"; done`
  prints nothing.
- Every prediction of §3.2 is marked confirmed or refuted in the findings, and in §10's table.

### 7.2 Nothing unverified reads as verified

- Each of G1 to G8 carries a URL, an access date and the label "Not verified by a run".
- The convention's heading says it is proposed and not in force.
- No sentence in the findings states GCP behaviour without a G id beside it.

### 7.3 Nothing else changed

`git diff --stat DEVELOP...HEAD` lists three files: the findings document, `CLAUDE.md` and this
spec. No POM, workflow, properties file, XML context or Java class appears.

## 8. Proposed issue body

Posted on `#48` in T8, on 2026-10-10, after approval:

> **As a** developer about to write ADR-003 (the pipeline) and ADR-004 (distribution)
> **I want** measured evidence of how Spring profiles, Maven and deploy-time values should share
> the work of configuring DSH by capability and by tier, with a convention proposed from it
> **So that** the ADRs choose transports, persistence and deployment names on observed behaviour
>
> **Acceptance criteria**
>
> - AC001: `specs/architecture/profiles-and-configuration.md` answers each Spring and Maven
>   question of the story's spec with a probe that was run, and shows the probe and its output.
> - AC002: Each prediction made in the spec is reported as confirmed or refuted.
> - AC003: Three ways to model the tiers are compared on evidence, with a recommendation.
> - AC004: A configuration convention is proposed, for one GCP project holding `dsh-dev`,
>   `dsh-staging` and `dsh`. Nothing is implemented.
> - AC005: The five test layers are mapped to the configuration each runs with, and the release
>   and hotfix gate is stated once, for both.
> - AC006: Every GCP fact cites its documentation and is labelled as not verified by a run.
> - AC007: The consequences for the waves and the follow-up candidates are listed. No issue is
>   opened or changed before the owner approves it, and the PRD is not edited by this story.
> - AC008: No production code, POM or workflow changes.
> - AC009: `CLAUDE.md` states the GCP cost rule: nothing stays alive when it is not in use, and
>   Cloud Run's minimum instances is always 0.
>
> Spec: `specs/stories/48-investigate-spring-and-maven-profiles.md`.

## 9. Acceptance criteria

| AC | Covered by |
|---|---|
| AC001: every question answered by a probe that was run | §3.2, §5.3, T1 to T3, T5, §7.1 |
| AC002: every prediction reported as confirmed or refuted | §3.2, T2, T3, §7.1, §10 |
| AC003: three tier models compared on evidence, with a recommendation | §3.3, T5 |
| AC004: a convention proposed for one project and three tiers, nothing implemented | §3.4, §3.6, T5, §7.2, §7.3 |
| AC005: five layers mapped to configuration; one gate for release and hotfix | §3.5, T5 |
| AC006: GCP facts cited and labelled | §3.6, T4, §7.2 |
| AC007: consequences and follow-ups listed; no issue opened or changed before approval, PRD untouched | §3.7, T5, T8, §7.3, §10.7 |
| AC008: no production code, POM or workflow changes | §3.8, T7, §7.3 |
| AC009: `CLAUDE.md` states the GCP cost rule | decision 12, §3.9, T6 |

## 10. Verification results

Built on `issue-48-investigate-spring-and-maven-profiles`, from `d3d607871` (the spec commit), on
2026-10-07.

### 10.1 T1, the harness and its baseline

- Apache Maven 3.9.16, JDK 17.0.20.1 (Eclipse Adoptium), Spring Boot 2.7.18, Windows 11.
- The harness was created in the session scratchpad from §5.2, unchanged, as its own git
  repository. Nothing was installed into `D:\.m2\repository`.
- **P0 printed §5.3's baseline line for line**, all 25 lines. No difference to resolve.

### 10.2 T2 and T3, the predictions

| Id | Prediction of §3.2 | Outcome | Probes |
|---|---|---|---|
| S1 | activation order; a later source replaces; a default in the jar works | confirmed | P1 to P4, P3b, P4b, P14, P15 |
| S2 | a profile's file overrides; the last listed wins; `on-profile` documents; no activation from a profile's file | confirmed | P0, P1, P5, P16 |
| S3 | a group activates its members, and the member's file wins | confirmed | P6 |
| S4 | `@Profile` leaves one bean; a property selects per service; a wrong value fails only where injected | confirmed | P0, P1, P7, P8 |
| S5 | XML honours a nested profile; the environment beats the located file; that file never reaches the `Environment` | confirmed | P0, P1, P9 |
| S6 | files of one name are not merged; an import brings its profile variant and overrides its importer; `@PropertySource` has no variant and loses | confirmed | P0, P1, P13, P17, P30 |
| S7 | an environment variable drives the derived names; an argument wins; a missing value stops the startup; both variable forms reach a dashed key | confirmed | P10 to P13, P28 |
| S8 | a test `application.properties` hides the main one; a profile's file merges; `@TestPropertySource` wins; `@ActiveProfiles` beats a leak | confirmed | P22 to P26, P29 |
| M1 | a placeholder survives without a Maven property and is baked with one; the self-reference fails as DSH's does; `@…@` only stops it | confirmed | P18, P19, P20 |
| M2 | the build stamps the version and a commit, and the application reads both | confirmed | P21 |
| M3 | a profile on the `mvn` command line or in the shell reaches the test JVM | confirmed | P22, P23 |
| M4 | a Maven profile per tier gives two different jars; one jar and two values give the same behaviours | confirmed | P27 |

No prediction was refuted. Four results were not predicted, and the findings report each:

- A build with no tier succeeds and ships the unresolved token (P27c).
- An empty `@ActiveProfiles` does not protect a test from a leaked profile (P29).
- A profile's file that is broken passes the build unless a test activates that profile (P16).
- A profile name that matches nothing is accepted in silence (P31).

After the last probe, `git status` in the harness showed a clean tree.

### 10.3 Departures from the plan

- **Six probes were added:** P3b, P4b, P28, P29, P30 and P31. The appendix of the findings
  document says why for each. P30 mattered most: no planned probe showed `application.properties`
  beating a `@PropertySource` file directly, and the prediction for S6 claims it.
- **P19 needed `-DskipTests`** for its run-time steps, because its first build failed on the
  circular placeholder, which is the finding.
- **P20 configured the plugin in the root POM**, inherited by both modules, not in each module.
- **The findings' appendix does not repeat the harness sources.** §3.1 and §3.8 said it would hold
  them. It names this spec's §5.2 instead, and lists every probe with what was run. Copying 370
  lines into a second file in the same repository added nothing a reader needs.
- **The findings were not checked against the Spring Boot 3 documentation.** §3.1 said each finding
  that it contradicts would say so. The document states this limit at the head of its section 3,
  and lists a re-run on `#120`'s Boot line as a follow-up candidate.

### 10.4 T4, the GCP facts

G1 to G8 are in the findings' section 8, each with its URL, read on 2026-10-07, under the label
"Not verified by a run". `cloud.google.com` documentation now redirects to `docs.cloud.google.com`.

Five points the documentation did not settle are listed there as open (§8.3): reusing a revision
name, a traffic tag's first character, named databases in the Firestore emulator, GHCR's tag
rules, and the idle cost of Pub/Sub and Artifact Registry. One claim about the traffic tag came
from a search result whose page could not be read; it is reported as unconfirmed, not as a fact.

Two facts changed the plan, and both are in the findings:

- **`dsh` is too short to be a Firestore database id**, which needs four characters. The proposed
  rule is the base name with `-db`.
- **The Firestore free tier covers one database per project.** Two of the three tiers are billed
  from their first operation.

### 10.5 T5 and T6, the checks of §7

- §7.1: every one of S1 to S8 and M1 to M4 has its five parts, counted by script. The loop over
  P0 to P27 prints nothing.
- §7.2: section 8 carries the label and the date; the convention's heading says "Proposed, not in
  force". The GCP statements outside section 8 each carry a G id or point at §8.4.
- `CLAUDE.md`'s "Cost rule for GCP" section matches §3.9 word for word, checked with `diff`.
- markdownlint, with the command in `CLAUDE.md`: no findings.

### 10.6 T7, the gates and §7.3

- **Gates 1 and 2:** `mvn -B clean install` at `bf875f81a`, logged to
  `.logs/mvn-clean-install.log`. Exit 0, `BUILD SUCCESS`, all 13 modules, in 1 min 5 s. The log
  holds "All coverage checks have been met" eight times, once for each module with production
  sources.
- **Gate 3 does not apply.** No code changed.
- **§7.3:** `git diff --stat DEVELOP...HEAD` lists three files: `CLAUDE.md`,
  `specs/architecture/profiles-and-configuration.md` and this spec. No POM, workflow, properties
  file, XML context or Java class appears.

### 10.7 T8, the hand-over

On 2026-10-10 the owner approved the issue body and asked for every follow-up candidate to become
an issue, added to one that exists wherever one fits.

- **`#48`** carries the body of §8.
- **AC007 was reworded before it was posted.** It said that no issue is opened by this story. The
  owner's decision made that false, so it now says that none is opened or changed before the owner
  approves it. §8 and §9 hold the posted text.

| Candidate, by its number in the findings' §9.2 | Where it went |
|---|---|
| 1. Stop filtering Spring configuration with `${…}` | new: `#158` |
| 2. Move `mongo.*` to run-time configuration | new: `#157` |
| 3. Remove the `test` profile from `api-testing.yml` | added to `#137`: a context bullet and AC005 |
| 4. Rename `dsh-rest-api`'s test `application.properties` | added to `#140`: a second paragraph |
| 5. Workflow outputs for the build number and the version | new: `parent-poms#109`, beside `parent-poms#91` |
| 6. Re-run the harness on the Boot line `#120` chooses | added to `#120`: a context bullet and AC007 |

The three issues that already existed were only added to; no line of them was removed. `#157` and
`#158` are on `0.4.0-SNAPSHOT`, and `parent-poms#109` is on `3.12.0-SNAPSHOT`. `#148` was not
changed: its commit gate and smoke check need a deployment that ADR-004 has yet to describe. The
PRD lists none of this yet; `dsh-plan-wave` and `dsh-reconcile-prd` bring it in.

### 10.8 The local review

An independent review of `a760552f8..c067c100c` on 2026-10-10 compared every finding's quoted
output with the saved logs, and the repository facts with the files, and found them correct. It
raised no critical finding, five important ones and a list of minor ones. Each was checked against
the text and all were fixed, on the owner's instruction.

| Finding | Fix |
|---|---|
| The tier comparison said approach C fails loudly on a missing value. The base name has a default, which is production's name, and a missing `gcp` runs in memory | The cell is qualified. Section 5 gains "What C does not catch", and section 9 a row handing the check to ADR-004 |
| "A Spring profile per tier needs the jar changed" was not probed against a file outside the jar | The summary and the table say so |
| "An idle database still costs only its storage" had no source, against §7.2 | Removed. Firestore joins the idle-cost point in §8.3. The statement about bucket-level access is marked as an inference |
| The cost rule did not say whether stored data is allowed | The owner added a bullet to the rule. §3.9 holds the new text, and `CLAUDE.md` matches it |
| "Three open points need a first deploy" disagreed with §8.3 | §8.3 lists six, and a first deploy settles two. The summary and section 9 say so |
| Answers that went past their output: S1, S8 and M2's consequence | Each is narrowed to what was run |
| Section 2 did not name the placeholder that already sits in a filtered file | A row for `enqueue-docId-context.xml`. Follow-up 1 names the XML contexts |
| The test-layer table showed layers 1 to 4 as proposed | A sentence under it says what differs today |
| R4 cited fewer sources than it needs; the bucket was "required under `gcp`"; R2 left today's wiring unnamed | R4 cites G6 and §8.3. The bucket is required when the file store is `gcs`. §6.4 hands the wiring to ADR-003 |
| §3.6's label on the revision, and §3.7's retention of revisions and load-balancer example, were missing | The label is an open point of section 7. Revisions are a sixth open point of §8.3. The example is in section 9 |
| "Every fact is quoted"; the Spring Cloud GCP reference carries no version | The section says what is quoted and what is paraphrased. The G2 row states the limit |
| `CLAUDE.md` did not list the cost rule among what it covers | It does now |

§10.4's count of five open points was true when written; there are six now. Decision 12 says that
an idle tier costs its storage and nothing else. That is the plan's assumption, and for Firestore
the findings now list it as open.

### 10.9 Review rounds on the pull request

**Round 1.** Copilot reviewed PR `#159` at `4b37615ce` on 2026-10-10, at effort `Balanced`, and
left two findings. Both were checked and both were valid.

| Finding | Verdict | Fix |
|---|---|---|
| The convention's defaults named an in-memory persistence and an in-memory file store, which neither the code nor the PRD provides (medium) | Valid. The only `DocumentDao` is `MongoDocumentDao`; the PRD plans `LocalFileStorageService` and keeps the Mongo layer in Wave 2 | `d9baf5432`. The owner chose to align the defaults with the plan: `mongo`, `local`, and `memory` for the transport alone. R2 promises only that a run with no profile reaches nothing on GCP. What such a run persists to after Wave 7 is open for ADR-003 |
| "Removing old revisions" was listed as open, though Cloud Run's documentation settles it (low) | Valid, checked against the page | `0ee986b93`. Two G8 facts added, read on 2026-10-10. §8.3 is back to five open points |

The first changes a line of §3.4, which said that no profile means everything in memory. The second
corrects a point that §10.8's fixes had added without reading the page.

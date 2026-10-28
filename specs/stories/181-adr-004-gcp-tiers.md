---
issue: 181
slug: adr-004-gcp-tiers
parent_branch: DEVELOP
wave: 1
milestone: 0.4.0-SNAPSHOT
---

# Story 181 — ADR-004: decide how DSH runs on its three GCP tiers

## 1. Story

**As a** developer about to build the GCP tiers (Wave 4), their Firestore, Cloud Storage and
Pub/Sub resources (Waves 5 and 6) and the release path (Wave 7)
**I want** one accepted record of how DSH runs on `dsh-dev`, `dsh-staging` and `dsh`: the deploy,
the names, who can call the service, what each service costs while idle, what happens to test data,
and where the release gate runs
**So that** the tasks in those waves that say "as ADR-004 decides" are written against decisions,
instead of each one deciding them again

## 2. Context

- Wave: 1, task 3, step 6 of the order agreed on 2026-10-06 and re-ordered on 2026-10-13
  (`specs/product/PRD.md` §4, Wave 1). DSH milestone `0.4.0-SNAPSHOT`. Step 5, `#120`, is closed.
- Issue: [#181](https://github.com/MRISS-Projects/dsh/issues/181). **The issue is the source of
  truth for the acceptance criteria.** §6 maps each one to this spec.
- Parent branch: `DEVELOP`, at `30d97b8c6` when this spec was written. The task branch is
  `issue-181-adr-004-gcp-tiers`.
- **This story is the first half of task 3.** The PRD's task covers the operated tiers, which
  Waves 4 and 7 deliver, and the end user's packaging, which Wave 10 delivers. `dsh-new-story`
  split it on 2026-10-28, because the whole failed "Small": fifteen subjects against `#160`'s nine,
  and an end-user half not needed until Wave 10. The second half, ADR-005, is opened after this
  story merges, because it builds on this ADR's deploy and names.
- **This story changes no production code, no test, no POM, no workflow and no script, and
  creates no GCP resource.** Its output is one new document,
  `specs/architecture/ADR-004-gcp-tiers.md`, and one row in `CLAUDE.md`'s router table (§4).
- "The findings" means `specs/architecture/profiles-and-configuration.md`, the output of `#48`. Its
  probe ids (P0 to P31), finding ids (S1 to S8, M1 to M4), rule ids (R1 to R12) and GCP fact ids
  (G1 to G8) are used here as they are there. ADR-003's fact ids (F1 to F12) and assumption ids
  (A1 to A6) are used as they are there.
- **New ids, chosen so that they clash with neither.** This story's GCP facts are K1 to K19 (§3.11)
  and its assumptions H1 to H6 (§3.12). An unqualified "A3" in this spec is always ADR-003's.

### 2.1 What was established while writing this spec

On `DEVELOP` at `30d97b8c6`, and from documentation read on 2026-10-28.

| Fact | Evidence |
|---|---|
| A file already stored is found by its hash and processed again in place; no second record is created | `DocumentSubmissionServiceImpl.getTokenFromDocument`: `getDocumentByHash`, then `stored.resetStatus()` when `useCache` is false |
| Term statistics count a document once, however often they are called, and offer no removal | ADR-003, section 5, "What the interface must offer" |
| Stages are idempotent, because Pub/Sub delivers at least once | ADR-003, section 1, line 124; F3 |
| ADR-003 leaves to this ADR: R5, R6, R8 and R10; the protection of the push endpoints; the topic prefix; the retention of term statistics and whether their interface gains a removal; how a test that calls a store directly reaches `dsh-dev`; the Firestore edition, with Wave 5 | ADR-003 lines 259, 494-497, 534-538, 630, 829, 875 |
| `staging.yml` is dispatched by hand with a `branch_name` input. The run's own commit is that of the branch it was dispatched from, not necessarily the branch it builds | `.github/workflows/staging.yml`, `on:` block |
| `hotfix.yml` releases from a `<version>.x` branch and has no staging step | `.github/workflows/hotfix.yml`; `docs/devops/README.md` |
| Firestore's free tier goes to the first database created in a project that has none, and moves to the next one created if that database is deleted | K1 |
| A Firestore grant can be limited to one database by an IAM condition on `resource.name`. Without one, a grant covers every database in the project | K3 |
| Cloud Storage's free tier applies only in `us-east1`, `us-west1` and `us-central1` | K4 |
| A private Cloud Run service checks a Pub/Sub push token itself | K7 |
| An ID token for a GitHub Actions job needs a service account to impersonate; direct Workload Identity Federation cannot mint one | K11 |
| IAP enabled on a Cloud Run service may break Pub/Sub push to that service | K12 |

### 2.2 Decisions agreed with the owner on 2026-10-28

These are the ADR's decisions. Step 4 writes them up; it does not reopen them. Each names the
alternatives that were put to the owner and rejected, because the ADR has to record them (AC002).

1. **One region, `us-central1`, for all three tiers.** It is one of the three regions where Cloud
   Storage's free tier applies (K4).
   *Rejected:* a region near the owner, such as `southamerica-east1`, which has no Cloud Storage
   free tier and buys latency that nothing in DSH needs: the pipeline is asynchronous and the only
   callers are tests.
2. **All three services are private.** Cloud Run's invoker check stays on. Only identities granted
   `roles/run.invoker` can call a tier (K7, K8).
   *Rejected:* `dsh` public with the other two private, which lets anyone write to production and
   be billed for; and all three public, which also needs a token check in the application on the
   push endpoints.
3. **The push endpoints are protected by IAM alone.** Only trusted identities hold invoker, and a
   stray call to a push endpoint does no more than a redelivery, which every stage already
   tolerates (ADR-003, section 1). The ADR records that ADR-003's requirement, "the endpoints
   accept requests from Pub/Sub only", is met in that form.
   *Rejected:* IAM Conditions on `request.path` (K8), which meets the requirement literally but
   stops a test from calling a push endpoint and rests on the least familiar mechanism; and a check
   of the token's `email` claim in the application, which adds code to guard against identities DSH
   chose itself.
4. **`dsh-dev` holds the one free Firestore database.** It carries the most operations, and Wave 5
   task 5 creates `dsh-dev-db` first, a wave before the other two exist (K1). Nothing ever deletes a
   database, or the free tier moves. An end user's `dsh` gets the free tier in their own project,
   because it is the only database there.
   *Rejected:* `dsh`, which would have to be created out of order to claim it, for traffic that does
   not exist yet; and `dsh-staging`, which has the fewest operations.
5. **Layer 5a tests are idempotent, and each removes in teardown exactly the data it created.**
   That is documents it created, their original files and indexing output, and their term
   statistics. A record that existed before the test ran is processed again and left in place.
   Data created by manual calls to the API, now or from a future front end, is never removed by a
   test. Teardown reaches the stores directly, under `dsh-tester`, which has no grant on `dsh`'s
   data. **The term statistics interface gains a removal**: forget one document's terms, the
   mirror of recording them once. Records are keyed by file hash (§2.1), so a run whose teardown
   never happened processes its leftovers again rather than adding to them: data stays bounded by
   the fixture set even then.
   *Rejected:* keeping all test data, bounded only by the fixtures, which was the first proposal
   and leaves tests that depend on what an earlier run left; wiping a whole tier before each run,
   which needs a tool able to delete a tier and a guard against its pointing at `dsh`; and expiry by
   age, which needs an expiry field, is billed outside the free tier (K1), and lets the term
   statistics count an expired fixture again.
6. **One staging tier, and the last deploy wins.** `staging.yml` also accepts a hotfix branch. A
   hotfix displaces a release candidate on `dsh-staging`. Refined by decision 13: the release gate
   asks whether a commit passed, so a release candidate that passed before it was displaced is
   released without a redeploy.
   *Rejected:* a fourth tier, `dsh-hotfix`, which is a fourth database billed from its first
   operation and a tier idle almost always; and a revision tagged on `dsh-staging` with no traffic
   (G5), because Pub/Sub pushes to the service's URL, so the hotfix's pipeline would run on the
   release candidate's revision.
7. **A front end is not decided here.** The ADR records the two patterns Cloud Run documents and
   the constraint between them, as a consequence for Wave 10 task 6: IAP on the service, for named
   Google accounts (K12); or a public service that verifies a user's token itself, through Identity
   Platform or Firebase Authentication (K13). IAP may break Pub/Sub push on the same service (K12),
   and a public service exposes the push endpoints, so either way the likely shape is two services
   from one image: a public one for `/submit` and `/status`, and a private one for the push
   endpoints. ADR-003 section 2 already allows that split.
   *Rejected:* deciding now, with no front end planned and Identity Platform's free tier not
   checked against the cost rule.

**Proposals of this spec, approved by the owner on 2026-10-28 in three parts.** They are decisions
of the ADR in the same way:

- Decision 8: **One deploy, also the end user's.** A single idempotent script deploys a tier from
  its base name, its image tag and its revision suffix, creating each resource only if absent. The
  workflows run it, and Wave 10 task 3 gives it to the end user with prerequisite checks and
  documentation, not as a second deploy (§3.2).
- Decision 9: **Six kinds of identity, all service accounts, no key** (§3.3).
- Decision 10: **The names**: the findings' §8.1 table, the image path, the bucket key, and a
  production deploy that takes no traffic until layer 5b passes on a fixed tag, `candidate`
  (§3.4).
- Decision 11: **A startup check against the two silent omissions**, on `K_SERVICE` (§3.5).
- Decision 12: **The bucket, the cost settings service by service, and Firestore's Standard
  edition** (§3.7, §3.8).
- Decision 13: **The release gate is a GitHub commit status**, `dsh-staging/acceptance`, posted
  after layer 5a passes and required on the head of the branch being released (§3.9).
- Decision 14: **R5, R6, R8 and R10 are in force** (§3.10).

### 2.3 Inputs that this story does not reopen

- The owner's decisions of 2026-10-10 (PRD §4): three operated tiers, `dsh-dev`, `dsh-staging`
  and `dsh`, in one GCP project; `dsh-dev` is deployed from `DEVELOP`, `dsh-staging` from a
  staging build, `dsh` from a release or hotfix; Wave 4 builds `dsh-dev` first; `0.6.0` is the
  first release through the tiers.
- ADR-003's decisions: the `gcp` profile and the three keys it sets; Pub/Sub push to the same
  service; the push endpoints accept requests from Pub/Sub only; nothing in the jar or the image
  names the owner's project; stages are idempotent.
- The cost rule in `CLAUDE.md`: Cloud Run's minimum instances is 0, always, and an exception needs
  the owner's approval before it is written down.

## 3. Design

### 3.1 The deliverable

`specs/architecture/ADR-004-gcp-tiers.md`, in this order:

1. **Header.** The table ADR-003 uses: date, status `Accepted`, authors. The sentence that the
   owner's merge is the acceptance.
2. **Context.** What exists: no GCP project, no deploy, no image. The inputs of §2.3, and what
   ADR-003 and the findings left to this ADR (§2.1). That this ADR is the first half of PRD Wave 1
   task 3, and ADR-005 the second.
3. **Decision.** Nine numbered sections, one per criterion of AC003 to AC011, in that order (§3.2 to
   §3.10). Each has paragraphs that start `**Decision.**`, `**Reason.**` and `**Rejected.**`.
4. **Evidence.** One table of the facts K1 to K19, each with its URL, the date read, the words
   quoted, and "Not verified by a run". The G and F facts it relies on are cited by id, not copied.
5. **Assumptions.** H1 to H6, each with the wave and task that proves it (§3.12).
6. **Consequences.** Positive and negative, as ADR-003 has them.
7. **Consequences for the waves.** The table of §3.13, its "After the merge" paragraph, and the
   front-end note of decision 7.
8. **Alternatives considered.** The rejected options of §2.2, in one table.
9. **References.**

Wave numbers are those of the re-plan of 2026-10-10, and the ADR says so once. Product names are
used throughout (Cloud Run, Firestore, Pub/Sub, Cloud Storage, Artifact Registry), as ADRs and
developer pages do.

### 3.2 Section 1 of the ADR: the tiers and the deploy (AC003)

- **One deploy, run under three base names.** A script, safe to run twice, takes the base name, the
  image tag and the revision suffix. It creates the tier's resources only if absent, then deploys.
  Where it lives and what it is written in are Wave 4 task 4's.
- **Who runs it.** `deploy.yml` for `dsh-dev`, `staging.yml` for `dsh-staging`, and `release.yml`
  and `hotfix.yml` for `dsh`, each in a DSH job after the reusable workflow.
- **It is the end user's script too.** Wave 10 task 3 adds the prerequisite checks and the
  documentation. So what an end user runs is what CI runs on every push to `DEVELOP`.
- **Every deploy sets** minimum instances 0, request-based billing, `--no-allow-unauthenticated`,
  `DSH_NAME=<base>`, `SPRING_PROFILES_ACTIVE=gcp`, the tier's runtime identity, and a maximum of
  instances. The maximum is a parameter, 1 by default for every tier: a cost guard against a storm of
  redeliveries. Wave 9 task 1's performance test revisits it.
- **The region** is `us-central1` (decision 1).
- **GitHub Actions authenticates through Workload Identity Federation**, with a pool limited by an
  attribute condition to the repository `MRISS-Projects/dsh`, impersonating the service accounts of
  §3.3. No service account key exists (K11). The finer conditions, such as which branches may
  impersonate `dsh-deployer`, are Wave 4 task 3's.

*Rejected:* a script per tier, which lets the tiers drift; a separate end-user script, which would
be the one deploy CI never runs; a key stored as a GitHub secret, which the action itself advises
against (K11); and no maximum of instances, which leaves the cost of a redelivery storm unbounded.

### 3.3 The identities

Part of section 1 of the ADR, under its own heading. All are service accounts; none has a key.

| Identity | Granted | Not granted | Used by |
|---|---|---|---|
| `dsh-deployer` | deploy Cloud Run services; create a tier's database, topics, subscriptions and managed folder; act as the runtime and push identities | read or write any document or object | the deploy |
| `<base>-run`, one per tier | `roles/datastore.user` with a condition on its own database (K3); object access on its own managed folder; publish to its own topics | anything of another tier | the service at run time |
| `<base>-push`, one per tier | `roles/run.invoker` on its own tier's service | anything else | the tier's push subscriptions (K7) |
| `dsh-tester` | invoker on `dsh-dev` and `dsh-staging`; `roles/datastore.user` conditioned on `dsh-dev-db` and `dsh-staging-db`; object access on `dsh-dev/` and `dsh-staging/` | anything on `dsh` | layer 5a, its teardown, and the two-caller test (A3) |
| `dsh-smoke` | invoker on all three services | any data | layer 5b, and the commit check of §3.9 |
| a developer's own account | invoker on `dsh-dev` | — | a local run through `gcloud run services proxy` (K9) |

Splitting `dsh-smoke` from `dsh-tester` means a layer 5a test pointed at production by mistake is
refused with a 403. Service account ids are 6 to 30 characters, and `dsh-staging-push`, the
longest, has 16; the build cites the limit (K19).

*Rejected:* one runtime identity for all tiers, which lets `dsh-dev` write production's data; one
push identity for all tiers, which lets a `dsh-dev` subscription call `dsh`; and one test identity
for 5a and 5b, which gives the identity that writes test data the right to call production.

### 3.4 Section 2 of the ADR: the names (AC004)

The findings' table (§8.1) is accepted, with the bucket and the image made exact:

| Resource | Rule | `dsh-dev` | `dsh-staging` | `dsh` |
|---|---|---|---|---|
| Cloud Run service | the base name (G1) | `dsh-dev` | `dsh-staging` | `dsh` |
| Cloud Run revision | service, then the version with dashes; `-dev-<n>` or `-rc-<n>` (G1) | `dsh-dev-0-4-0-dev-54` | `dsh-staging-0-4-0-rc-54` | `dsh-0-4-0` |
| Firestore database | the base name and `-db` (G2) | `dsh-dev-db` | `dsh-staging-db` | `dsh-db` |
| Topic, subscription | the base name, a dash, the channel (G3); the channels are Wave 6's | `dsh-dev-<channel>` | `dsh-staging-<channel>` | `dsh-<channel>` |
| Bucket | one per project, named by the deploy through `dsh.storage.bucket`; proposed `<project-id>-dsh` (G4) | the same | the same | the same |
| Object prefix, managed folder | the base name and `/` (G4) | `dsh-dev/` | `dsh-staging/` | `dsh/` |
| Image | `us-central1-docker.pkg.dev/<project>/dsh/dsh`, tagged with the dotted version (G7) | `:0.4.0-dev.54` | `:0.4.0-rc.54` | `:0.4.0` |
| Runtime and push identities | the base name and `-run` or `-push` | `dsh-dev-run` | `dsh-staging-run` | `dsh-run` |

- **The image keeps its dots, and the revision uses dashes**, as the findings proposed (§8.2). The
  owner's rule of one dashed string for both is rejected: it gives up the tag a user pulling the
  image expects.
- **Production takes no traffic until it passes.** `dsh` is deployed with `--no-traffic` under a
  fixed tag, `candidate`. Layer 5b runs against the tag's URL (G5), with a token whose audience is
  the service's URL (K10). Traffic then moves to the revision. A bad rebuild never serves. It costs
  nothing. `dsh-dev` and `dsh-staging` are deployed straight to traffic.

**The five open points of the findings (§8.3).**

| Point | Settled by |
|---|---|
| Reusing a revision name | Wave 4 task 4, at the first re-run of a deploy. Until then the deploy assumes it fails (H2) |
| A traffic tag's first character | Avoided: the only tag is `candidate`, which starts with a letter |
| Named databases in the Firestore emulator | Wave 5 task 1, with `#49`, for layer 3; ADR-005 for local mode |
| GHCR's tag rules | ADR-005 |
| What an idle tier costs | Section 6 of the ADR, from the pricing pages (K14 to K17), confirmed by the budget alert (H5) |

*Rejected:* one dashed string for the image and the revision (above); a bucket per tier, which the
findings' isolation by managed folder makes unnecessary and which multiplies global names; and
deploying `dsh` straight to traffic, which lets a bad rebuild serve until 5b fails.

### 3.5 Section 3 of the ADR: the two silent omissions (AC005)

**Decision.** A check at the application's startup. Cloud Run sets `K_SERVICE`, the service's name,
in every container (K5). When `K_SERVICE` is present, the application refuses to start unless
`gcp` is among the active profiles and `dsh.name` equals `K_SERVICE`. The container exits, the
revision never passes its startup probe, and it takes no traffic (K6). That catches a missing base
name, a missing `gcp`, and a mistyped one, before any request is served or any write is made.

- Locally and in every test `K_SERVICE` is absent, and nothing changes.
- An end user's deploy names the service after the base name (§3.2), so the check holds for them.
- Wave 4 task 5's smoke check stays as the second line: it reads the base name the application
  reports.
- That the deploy then fails, instead of leaving a revision that never became ready, is H1.
- The code is Wave 4 task 5's, which introduces `dsh.name`.

*Rejected:* a check after the deploy only, by which time the bad revision serves and may have
written to production's database; and removing the default `dsh` from `dsh.name`, which is what
lets anyone run DSH locally with no name at all (findings, section 5).

### 3.6 Section 4 of the ADR: who can call the service (AC006)

- **Decisions 2 and 3.** Every tier is private; the identities of §3.3 hold invoker.
- **Layer 5a and 5b in CI** fetch a Google-signed ID token for `dsh-tester` or `dsh-smoke`, through
  the impersonation of §3.2, with the service's URL as its audience (K10, K11). The tests' HTTP
  client adds one header; nothing else in them changes.
- **A local run** reaches `dsh-dev` through `gcloud run services proxy dsh-dev`, which serves it on
  `http://localhost:8080` with the developer's token (K9). The layer 3 and 4 tests run unchanged
  with that base URL. `gcloud auth print-identity-token` also works, and the documentation marks it
  as fit for development only, because its token carries no audience (K9).
- **The push endpoints** are reached by `<base>-push`, through Cloud Run's built-in check of the
  push token (K7).
- **A front end:** decision 7.

### 3.7 Section 5 of the ADR: the bucket (AC007)

One bucket, in `us-central1`, Standard storage class, with uniform bucket-level access (G4), no
object versioning, and soft delete off (K16). A managed folder per tier, `<base>/`. `<base>-run`
holds object access on its own folder only, and `dsh-tester` on `dsh-dev/` and `dsh-staging/`. No
identity holds object access on the bucket as a whole. `dsh-deployer` creates the folders and reads
no object.

Soft delete is off because it bills deleted objects for its retention, which would bill every
teardown (K16). The build reads the default retention and the billing from the page; if it shows
otherwise, the owner is asked before the ADR says it.

*Rejected:* a bucket per tier (§3.4); grants on the bucket as a whole, which are not limited to a
prefix (findings §8.4); and soft delete left at its default.

### 3.8 Section 6 of the ADR: the cost rule, service by service (AC008)

| Service | Settings | What an idle tier costs |
|---|---|---|
| Cloud Run | minimum instances 0, request-based billing, a maximum of instances (§3.2), no tag left serving | nothing (G8) |
| Firestore | **Standard edition**. `dsh-dev-db` created first, and free (decision 4). No point-in-time recovery, no scheduled backups, **delete protection on** (K17) | what it stores (K2) |
| Pub/Sub | no topic retention, no retention of acknowledged messages, no snapshots; the retention of unacknowledged messages is Wave 6 task 4's | expected nothing; read from the pricing page (K14) |
| Cloud Storage | §3.7, inside the free tier's 5 GB in `us-central1` (K4) | what it stores |
| Artifact Registry | a cleanup policy removes `dev` and `rc` tags older than 14 days, keeps the 5 most recent of each, and removes untagged images; release tags are kept, because a rollback deploys them (G8) | what it stores beyond 0.5 GB (K4) |
| Revisions | not deleted. An idle revision is not billed, and Cloud Run prunes past 1,000 (G8) | nothing |
| Cloud Logging | the default retention of 30 days, which is not charged (K15) | ingestion beyond the free allotment (K15) |

- **Standard edition, not Enterprise.** Its free tier includes deletes, which Enterprise's does not
  (K2), and teardown deletes. ADR-003's status advance is correct under both (F10).
- **Delete protection** guards production and keeps the free tier where decision 4 put it.
- **Nothing else is used**: no load balancer, Cloud NAT, VPC connector, Secret Manager, Cloud Build
  or Cloud Scheduler. Each would be a cost to weigh against the rule; none is needed.
- **No exception is needed.** Nothing in the design has a fixed cost while idle.
- The budget alert is Wave 4 task 3's, and its amount is the owner's to set there.

*Rejected:* Enterprise edition (above); keeping every image, which grows Artifact Registry past its
free 0.5 GB with images no one deploys; and deleting old revisions, which saves nothing.

### 3.9 Section 7 of the ADR: test data (AC009), and section 8: the release gate (AC010)

**Test data.** Decision 5, with these consequences written into the ADR:

- **How a test knows what it created.** Before it submits a fixture, the test reads by the file's
  hash whether the document exists. Teardown removes only the documents the test created.
- **The removal** is one operation on the term statistics interface, added by Wave 3 task 2 with
  its contract test, and implemented on Firestore by Wave 5 task 3. It takes the document's
  distinct terms, which the indexing stage already produces.
- **The two-caller test** (A3, Wave 5 task 7) reaches `dsh-dev-db` the same way teardown does:
  from a GitHub Actions job, under `dsh-tester`, calling Firestore directly (H4).
- **The cost rule's "set a retention"** is met for Firestore and Cloud Storage by teardown, with
  the bound of decision 5 behind it; for images by the cleanup policy (§3.8); for messages by
  Wave 6's retention.

**The release gate.** A revision keeps serving when layer 5a fails against it, and the commit of a
`staging.yml` run is not the commit it built (§2.1). So neither "what staging runs now" nor "the
last staging run" proves that a commit passed.

- **The record.** After layer 5a passes, `staging.yml` posts a GitHub commit status,
  `dsh-staging/acceptance`, `success`, on the commit it deployed. Before posting, it checks, under
  `dsh-smoke`, that the running revision's build stamp names that commit (M2).
- **Where the gate runs.** In a first job of `release.yml` and `hotfix.yml`, before the reusable
  workflow is called, so that a refusal stops before any write. It requires that status on the head
  of the branch being released. The `dsh-release` skill (`#148`, Wave 7 task 3) checks the same
  status before it dispatches.
- **No label on the revision.** The commit status is the one record. A label would be a second
  claim, able to disagree with the first.
- **A hotfix and a release candidate** (decision 6). `staging.yml` accepts a hotfix branch. The
  last deploy wins `dsh-staging`. A release candidate whose head passed is released without being
  deployed again; one whose head moved since is staged again first, which the gate enforces.

*Rejected:* the gate reading the commit from the running revision, which proves what runs, not
what passed; the gate reading the last `staging.yml` run, whose commit is that of the dispatching
branch; a commit label on the revision (above); and the gate in the skill only, which a manual
dispatch would bypass.

### 3.10 Section 9 of the ADR: the rules left to it (AC011)

| Rule | In force | What it fixes |
|---|---|---|
| R5, the tier is one value, the base name | yes | `dsh.name`, set by the variable `DSH_NAME`, default `dsh`. Every resource name derives from it in `application-gcp.properties` |
| R6, a required value is bound through a validated class | yes | Under `gcp`, `dsh.storage.bucket` is required, has no default, and its absence stops the startup (P28a) |
| R8, the build stamps the version and the commit, and nothing else | yes | `spring-boot:build-info`, Wave 4 task 1 (M2) |
| R10, every context test names its profile | yes | It lands in `.github/copilot/rules/testing-patterns.md` with layer 5, in Wave 4 task 5 |

### 3.11 Facts to cite

**Each fact is quoted from its page during the build**, with its URL, the date and the words
quoted, and the label "Not verified by a run". The table below says what each must establish and
whether it was read while this spec was written. A fact read here is read again in the build and
quoted from the page, not from this table. A fact that cannot be found documented is dropped, and
what rested on it becomes an assumption or goes back to the owner.

| Id | What it has to establish | Read on 2026-10-28 | Used by |
|---|---|---|---|
| K1 | Firestore's free tier: one database per project, the first created, moving on deletion; TTL deletes, point-in-time recovery and backups are excluded | yes, `docs.cloud.google.com/firestore/quotas` | decisions 4, 5 |
| K2 | The free amounts per edition; Enterprise's table has no delete allowance | yes, the same page | §3.8 |
| K3 | A Firestore grant limited to one database by an IAM condition on `resource.name` | from a Google Cloud blog and the Enterprise connection page; the build cites the Firestore documentation's own page on database access conditions | §3.3 |
| K4 | The free tier of Cloud Run, Cloud Storage (US regions only), Artifact Registry and Pub/Sub | yes, `docs.cloud.google.com/free/docs/free-cloud-features` | decision 1, §3.8 |
| K5 | Cloud Run sets `K_SERVICE` to the service's name in every container | yes, `docs.cloud.google.com/run/docs/container-contract` | §3.5 |
| K6 | A container whose startup probe fails is shut down; a container is ready to serve once its startup probe passes; the default probe is TCP on the port | yes, `docs.cloud.google.com/run/docs/configuring/healthchecks` | §3.5 |
| K7 | A private service checks a push token itself; the push identity needs `roles/run.invoker`; `--no-allow-unauthenticated` | yes, `docs.cloud.google.com/run/docs/triggering/pubsub-push` | decisions 2, 3 |
| K8 | Invoker is granted per service; IAM Conditions on `request.path`; the invoker check can be disabled to make a service public | yes, `docs.cloud.google.com/run/docs/securing/managing-access` | decisions 2, 3 |
| K9 | `gcloud run services proxy`; `print-identity-token` for development only, without an audience | yes, `docs.cloud.google.com/run/docs/authenticating/developers` | §3.6 |
| K10 | A token's audience is the service's URL, also when a tag is targeted | yes, `docs.cloud.google.com/run/docs/authenticating/service-to-service` | §3.4, §3.6 |
| K11 | Workload Identity Federation is preferred over keys; an ID token needs a service account to impersonate | yes, `github.com/google-github-actions/auth`, the action's own documentation | §3.2 |
| K12 | IAP can be enabled on a Cloud Run service directly; it may break Pub/Sub push to it | yes, `docs.cloud.google.com/run/docs/securing/identity-aware-proxy-cloud-run` | decision 7 |
| K13 | End users are authenticated by Identity Platform or Firebase Authentication on a public service that verifies the token | yes, `docs.cloud.google.com/run/docs/authenticating/end-users` | decision 7 |
| K14 | What Pub/Sub bills for, and whether an idle topic or subscription costs anything | no: the pricing page did not render. To be read | §3.8 |
| K15 | Cloud Logging's free ingestion allotment, and that the default 30-day retention is not charged | in part: the retention, from `docs.cloud.google.com/logging/quotas`. The allotment is to be read | §3.8 |
| K16 | Cloud Storage soft delete: its default retention, that it bills deleted objects, and that it can be turned off | no. To be read | §3.7 |
| K17 | Firestore delete protection, and that point-in-time recovery and backups are billed | in part: their exclusion from the free tier is K1. To be read | §3.8 |
| K18 | Cloud Run's maximum of instances, and how requests beyond it are answered | no. To be read | §3.2 |
| K19 | The length and character rules of a service account id | no. To be read | §3.3 |

If K14, K16 or K18 contradicts this design, the owner is asked before the ADR is written that way.

### 3.12 Assumptions

Each is something only a deploy can show.

| Id | Assumption | Proved by |
|---|---|---|
| H1 | A revision whose container exits at startup fails the deploy and takes no traffic | Wave 4 tasks 4 and 5, with a deliberate deploy that leaves out `DSH_NAME` |
| H2 | Deploying a revision suffix that already exists fails | Wave 4 task 4 |
| H3 | `roles/datastore.user` with a condition on `dsh-dev-db` and `dsh-staging-db` stops `dsh-tester` reading `dsh-db` | Wave 5 task 5, by a test that expects the refusal |
| H4 | Teardown and the two-caller test reach Firestore and Cloud Storage under `dsh-tester`, impersonated from GitHub Actions | Wave 5 tasks 6 and 7 |
| H5 | An idle tier costs what section 6 of the ADR says | the budget alert and the billing report, through Waves 4 and 5 |
| H6 | A revision tagged `candidate` with no traffic accepts a token whose audience is the service's URL | Wave 7 task 2 |

### 3.13 Consequences for the waves

**This story does not edit the PRD.** The ADR lists the consequences, and the PRD changes go
through `dsh-plan-wave` afterwards. No issue is opened or changed before the owner approves it.

| Where | Consequence |
|---|---|
| Wave 1 task 3 | Split. This ADR is its first half; ADR-005, the end user's packaging, is opened after this story merges |
| Wave 3 task 2 | The term statistics interface gains a removal, and its contract test covers it (decision 5) |
| Wave 4 task 2 | The image's path and tags (§3.4) |
| Wave 4 task 3 | The Workload Identity pool limited to the repository; `dsh-deployer`, `dsh-tester` and `dsh-smoke`; the Artifact Registry repository `dsh` in `us-central1` with its cleanup policy; the bucket with its settings (§3.7); the budget alert |
| Wave 4 task 4 | The deploy script of §3.2, with its settings; it creates `<base>-run` and `<base>-push`; it settles H2 |
| Wave 4 task 5 | The startup check of §3.5, and H1; the smoke check runs as `dsh-smoke`; R10 and layer 5 in `testing-patterns.md` |
| Wave 5 task 1 | `dsh.storage.bucket` is required under `gcp` (R6) |
| Wave 5 task 3 | The removal on Firestore |
| Wave 5 task 5 | `dsh-dev-db` first, Standard edition, delete protection; the managed folder and the grants of §3.3; H3 |
| Wave 5 task 6 | Layer 5a authenticates as `dsh-tester`; teardown as decision 5 says; H4 |
| Wave 5 task 7 | The two-caller test runs under `dsh-tester`, directly against `dsh-dev-db` |
| Wave 6 task 2 | The push endpoints are protected by IAM alone (decision 3) |
| Wave 6 task 4 | Topics and subscriptions named `<base>-<channel>`; push by `<base>-push` |
| Wave 7 task 1 | `staging.yml` accepts hotfix branches and posts `dsh-staging/acceptance` |
| Wave 7 task 2 | `dsh` is deployed with no traffic under `candidate`; layer 5b, then traffic moves; H6 |
| Wave 7 task 3 | The gate is a first job of `release.yml` and `hotfix.yml` requiring the status; the skill checks it too. The open point on a hotfix and `dsh-staging` is closed |
| Wave 9 task 1 | The maximum of instances is revisited |
| Wave 10 task 3 | The end user's script is the deploy of §3.2, with prerequisite checks |
| Wave 10 task 6 | The two front-end patterns and the constraint between them (decision 7) |

### 3.14 Out of scope

As the issue lists it:

- The end user's packaging, the second half of task 3: the public image and how an end user's
  Cloud Run reaches it, the release zip with its offline Maven repository (`parent-poms#92`) and
  its acceptance test, local mode, and the two open points of §8.3 only it meets. That is ADR-005.
- Any code, script or GCP resource.
- Adding layer 5 and R10 to `testing-patterns.md` (Wave 4 task 5).
- Editing `specs/product/PRD.md`.
- Editing ADR-003 or ADR-001. Where this ADR refines either, it says so in its own text.
- The findings and the story specs, which are dated records.

And from this spec:

- Opening ADR-005, or any issue named in §3.13.
- The channels' names, the endpoint paths and the redelivery settings, which are Wave 6's.
- The budget's amount, and the finer conditions on the Workload Identity pool (Wave 4 task 3).

## 4. Files to change

| File | Change |
|---|---|
| `specs/architecture/ADR-004-gcp-tiers.md` | New. §3.1 |
| `CLAUDE.md` | One row in the router table, after ADR-003's: "The GCP tiers: deploy, identities, names, cost and the release gate", leading to ADR-004 |
| `specs/stories/181-adr-004-gcp-tiers.md` | This spec |

Nothing else. No POM, workflow, script, properties file, Java class or test appears in the diff.

## 5. Verification

### 5.1 Every decision is closed

- The ADR's Decision part has nine numbered sections, each with paragraphs starting
  `**Decision.**`, `**Reason.**` and `**Rejected.**`.
- `grep -n -i "to be decided\|TBD\|open question"` over the ADR prints nothing.
- What the ADR leaves to another story is named with that story: ADR-005, and the wave tasks of
  §3.13.

### 5.2 Nothing unverified reads as verified

- Each of K1 to K19 that the ADR keeps has a URL, a date, quoted words and "Not verified by a run".
- No sentence in the ADR states GCP behaviour without a G, F or K id beside it.
- Each of H1 to H6 names the wave and task that proves it.
- Every id the ADR cites exists where it says: P, S, M, R and G in the findings, F and A in
  ADR-003, K and H in the ADR itself.

### 5.3 The cost rule holds

- Every Cloud Run setting in the ADR has minimum instances 0.
- The ADR names no load balancer, NAT, VPC connector, Secret Manager, Cloud Build or Cloud
  Scheduler, except in the sentence that says none is used, and in decision 7's note on IAP.
- No resource with a fixed cost while idle is decided. If the build finds one, it stops and asks.

### 5.4 The documents are well formed

- The markdown lint command in `CLAUDE.md` reports nothing.
- The router row's link resolves.

### 5.5 Nothing else changed

`git diff --stat DEVELOP...HEAD` lists the three files of §4 and no other.

### 5.6 The gates

`mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as `CLAUDE.md` requires. Expected:
exit 0. Gate 3 does not apply: no code changed.

## 6. Acceptance criteria

The criteria are the issue's. The column says where this spec covers each.

| AC | Covered by |
|---|---|
| AC001: the ADR exists, with ADR-003's parts, status `Accepted`, lint clean | §3.1, §5.4 |
| AC002: every decision closed, with reason and rejected alternatives; a fixed-cost item goes to the owner | §2.2, §3.1, §5.1, §5.3 |
| AC003: the tiers and the deploy, the region, GitHub Actions' authentication, the end user's script | decisions 1 and 8, §3.2, §3.3 |
| AC004: every name per tier; the five open points | decision 10, §3.4 |
| AC005: the two silent omissions | decision 11, §3.5 |
| AC006: public or private; layer 5a and 5b; the push endpoints | decisions 2, 3 and 7, §3.6 |
| AC007: the bucket | decision 12, §3.7 |
| AC008: the cost rule service by service; the free database; the edition; images and revisions | decisions 4 and 12, §3.8 |
| AC009: cleanup, the term statistics, the store-direct test | decision 5, §3.9 |
| AC010: where the gate runs, the label, a hotfix and a release candidate | decisions 6 and 13, §3.9 |
| AC011: R5, R6, R8 and R10 | decision 14, §3.10 |
| AC012: evidence, labels and assumptions | §3.11, §3.12, §5.2 |
| AC013: consequences for the waves; PRD untouched; no issue changed before approval | §3.13, §5.5 |
| AC014: `CLAUDE.md`'s router leads to ADR-004 | §4, §5.4 |
| AC015: documentation only, no GCP resource | §2, §4, §5.5 |

## 7. Tasks

Written on 2026-10-28, after the owner approved §1 to §6. `dsh-build-story` (step 4) runs the tasks
in order. Each task that changes a file is one commit.

**Goal.** ADR-004 exists with every decision closed, and `CLAUDE.md` leads to it.

**How the work is tested.** There is no Java under test. The checks of §5 are the tests, and §7.2
writes them as commands. T1 runs them before anything is written and records that they fail. Each
later task ends with the checks it has to turn green.

### 7.1 Constraints on every task

- **The diff holds the three files of §4 and no other.** No POM, workflow, script, properties
  file, Java class or test.
- **Not touched:** `specs/product/PRD.md`, `specs/architecture/profiles-and-configuration.md`,
  `specs/architecture/ADR-001-GCP-based-components.md`,
  `specs/architecture/ADR-003-document-pipeline.md`, `.github/copilot/rules/testing-patterns.md`,
  and every GitHub issue.
- **No GCP resource is created, and no `gcloud` command that changes anything is run.** Reading
  documentation is the only contact with GCP.
- **Evidence.** A sentence about Spring or Maven behaviour carries a probe, finding or rule id of
  the findings. A sentence about GCP carries a G, F or K id. Whatever none supports is an
  assumption, H1 to H6, with the wave and task that proves it. ADR-003's assumptions are cited as
  "ADR-003's A3", never as a bare "A3".
- **Wording.** Product names: Cloud Run, Firestore, Pub/Sub, Cloud Storage, Artifact Registry.
  Wave numbers are those of the re-plan of 2026-10-10, and the ADR says so once. The channel names
  and endpoint paths are Wave 6's, and the ADR writes `<channel>` where one would go.
- **Nothing is copied from memory.** A quotation is copied from the page it cites, on the day it is
  read, including the facts this spec marks as read on 2026-10-28.
- **Lint before every commit**, with check C4. A commit message is the one the task gives, and it
  ends with the attribution trailers in force for the session.
- **The shell keeps no state between commands.** Every command block below starts with the prelude
  of §7.2, from the repository root.

### 7.2 The checks

The prelude:

```bash
export PATH="$HOME/apps/node-v24.21.0-win-x64:$PATH"
SPEC=specs/stories/181-adr-004-gcp-tiers.md
ADR4=specs/architecture/ADR-004-gcp-tiers.md
ADR3=specs/architecture/ADR-003-document-pipeline.md
FIND=specs/architecture/profiles-and-configuration.md
```

**C1, every decision is closed (§5.1).** Each of the nine numbered sections of the Decision part
holds one paragraph that starts `**Decision.**`, one that starts `**Reason.**` and one that starts
`**Rejected.**`.

```bash
test -f "$ADR4" && echo "the ADR exists"
grep -n '^| \*\*Status\*\* *| Accepted' "$ADR4"
grep -c '^### [1-9]\. ' "$ADR4"
grep -c '^\*\*Decision\.\*\*' "$ADR4"
grep -c '^\*\*Reason\.\*\*' "$ADR4"
grep -c '^\*\*Rejected\.\*\*' "$ADR4"
grep -n -i 'to be decided\|TBD\|open question' "$ADR4"
awk '/^## Alternatives considered/{f=1;next} /^## /{f=0} f && /^\| /' "$ADR4" | wc -l
```

Expected: `the ADR exists`; one status line; `9` four times; nothing from the fifth `grep`; `38`,
which is a header, a separator and the 36 alternatives of T3's step 3.

**C2, nothing unverified reads as verified (§5.2).**

```bash
for id in $(grep -o '\b[PSMRG][0-9]\+[ab]\?\b' "$ADR4" | sort -u); do
  grep -q "\b$id\b" "$FIND" || echo "$id is not in the findings"
done
for id in $(grep -o '\b[FA][0-9]\+\b' "$ADR4" | sort -u); do
  grep -q "^| $id |" "$ADR3" || echo "$id is not in ADR-003"
done
for id in $(grep -o '\bK[0-9]\+\b' "$ADR4" | sort -u); do
  grep -q "^| $id |.*https://.*Not verified by a run" "$ADR4" || echo "$id has no complete row"
done
for n in 1 2 3 4 5 6; do
  grep -q "^| H$n |.*Wave [0-9]" "$ADR4" || echo "H$n names no wave"
done
```

Expected: nothing from any loop. A K id that T2 dropped appears nowhere in the ADR, so it is not
looked for.

One part of §5.2 has no command: that no sentence states GCP behaviour without a G, F or K id
beside it. T3 does it by reading the ADR once from top to bottom for that alone.

**C3, the cost rule holds (§5.3).**

```bash
grep -n -i 'minimum instances\|min-instances\|min instances' "$ADR4"
grep -n -i 'load balancer\|Cloud NAT\|VPC connector\|Secret Manager\|Cloud Build\|Cloud Scheduler' "$ADR4"
```

Expected: every line of the first `grep` says 0. The second prints lines only from section 6's
sentence that names what is not used, section 4's note on a front end, and the Alternatives table;
each is read once.

**C4, the lint.**

```bash
markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' \
  --ignore 'docs/wiki/**' --config .markdownlint.json; echo "lint exit=$?"
```

Expected: `lint exit=0` and no finding.

**C5, the ADR can be found (§5.4).**

```bash
grep -n 'ADR-004-gcp-tiers.md' CLAUDE.md
test -f "$ADR4" && echo "the link resolves"
```

Expected: one line, in the router table, and `the link resolves`.

**C6, nothing else changed (§5.5).**

```bash
git diff --stat DEVELOP...HEAD
```

Expected: the three files of §4 and no other.

### 7.3 The tasks

- [ ] **T1 — record the red state.** Before any file of §4 other than this spec is changed.

  1. Run C1 and C5. Expected: C1 prints nothing for `test -f` and an error for each `grep` and the
     `awk`, because the ADR does not exist. C5 prints nothing. Keep the outputs for §8.
  2. Nothing is committed by this task.

- [ ] **T2 — establish K1 to K19.** Read each page, and note the URL, the date and the words that
  state the fact. The notes stay in the session's scratchpad until T3 writes them into the ADR.
  The pages for K1 to K13 were read on 2026-10-28 and are read again. The others are starting
  points and may have moved.

  | Id | Start from |
  |---|---|
  | K1, K2 | `https://docs.cloud.google.com/firestore/quotas` |
  | K3 | `https://docs.cloud.google.com/firestore/native/docs/security/iam`, and the page it links for database access conditions |
  | K4 | `https://docs.cloud.google.com/free/docs/free-cloud-features` |
  | K5 | `https://docs.cloud.google.com/run/docs/container-contract` |
  | K6 | `https://docs.cloud.google.com/run/docs/configuring/healthchecks` |
  | K7 | `https://docs.cloud.google.com/run/docs/triggering/pubsub-push` |
  | K8 | `https://docs.cloud.google.com/run/docs/securing/managing-access` |
  | K9 | `https://docs.cloud.google.com/run/docs/authenticating/developers` |
  | K10 | `https://docs.cloud.google.com/run/docs/authenticating/service-to-service` |
  | K11 | `https://github.com/google-github-actions/auth`, the README |
  | K12 | `https://docs.cloud.google.com/run/docs/securing/identity-aware-proxy-cloud-run` |
  | K13 | `https://docs.cloud.google.com/run/docs/authenticating/end-users` |
  | K14 | `https://cloud.google.com/pubsub/pricing`. It did not render on 2026-10-28; if it still does not, `https://docs.cloud.google.com/pubsub/docs/billing` or the Pub/Sub quotas page |
  | K15 | `https://docs.cloud.google.com/logging/quotas` and `https://cloud.google.com/products/observability/pricing` |
  | K16 | `https://docs.cloud.google.com/storage/docs/soft-delete` |
  | K17 | `https://docs.cloud.google.com/firestore/native/docs/manage-databases`, for delete protection, and `https://cloud.google.com/firestore/pricing`, for point-in-time recovery and backups |
  | K18 | `https://docs.cloud.google.com/run/docs/configuring/max-instances` |
  | K19 | `https://docs.cloud.google.com/iam/docs/service-accounts-create` |

  1. For each fact, decide: kept, with its quotation; or not found. A fact that is not found is
     dropped, or becomes an assumption with the wave that proves it. Every sentence of §3 that
     rested on it is then worded as an assumption in the ADR.
  2. **If K14, K16 or K18 contradicts the design, stop.** That is: an idle Pub/Sub topic or
     subscription is billed (§3.8), soft delete cannot be turned off or does not bill deleted
     objects (§3.7), or a maximum of instances cannot be set or rejects what it cannot serve in a
     way Pub/Sub does not retry (§3.2). Tell the owner what the page says, and do not write the
     ADR around it.
  3. If any other page contradicts a decision of §2.2, stop in the same way.
  4. Nothing is committed by this task.

- [ ] **T3 — write `specs/architecture/ADR-004-gcp-tiers.md`.**

  1. Create the file with these headings, in this order, and the header table ADR-003 uses: date of
     the day, status `Accepted`, authors `Backend Development Team`, then the sentence "The owner's
     merge of the pull request that adds this file is the acceptance."

     ```text
     # ADR-004: The GCP Tiers
     ## Context
     ### What exists today
     ### Inputs
     ### What this ADR answers
     ## Decision
     ### 1. The tiers and the deploy
     #### The identities
     ### 2. The names
     ### 3. The two silent omissions
     ### 4. Who can call the service
     #### A front end
     ### 5. The bucket
     ### 6. The cost rule, service by service
     ### 7. Test data
     ### 8. The release gate
     ### 9. The rules left to this ADR
     ## Evidence
     ## Assumptions
     ## Consequences
     ### Positive
     ### Negative
     ## Consequences for the waves
     ### After the merge
     ## Alternatives considered
     ## References
     ```

  2. Write Context. "What exists today": no GCP project, no image, no deploy, and the facts of §2.1
     that are about the code and the workflows. "Inputs": §2.3. "What this ADR answers": the list
     ADR-003 and the findings left to it (§2.1's fourth row, and findings §9.1's six rows for
     ADR-004), and that this ADR is the first half of PRD Wave 1 task 3, with ADR-005 the second.
  3. Write the nine decision sections. Each starts with three paragraphs, `**Decision.**`,
     `**Reason.**` and `**Rejected.**`, and then holds the detail under `####` headings.

     | Section | Answers | Written from | Rejected alternatives it records |
     |---|---|---|---|
     | 1. The tiers and the deploy | AC003 | decisions 1 and 8; §3.2; §3.3 under "The identities", with its table | a region near the owner; a script per tier; a separate end-user script; a stored key; no maximum of instances; one runtime identity; one push identity; one test identity for 5a and 5b — 8 |
     | 2. The names | AC004 | decision 10; §3.4, both tables | one dashed string for image and revision; a bucket per tier; `dsh` deployed straight to traffic — 3 |
     | 3. The two silent omissions | AC005 | decision 11; §3.5 | a check after the deploy only; no default for `dsh.name` — 2 |
     | 4. Who can call the service | AC006 | decisions 2, 3 and 7; §3.6. "A front end" holds decision 7 | `dsh` public; all public; IAM Conditions on `request.path`; the `email` claim checked in the application; a front end decided now — 5 |
     | 5. The bucket | AC007 | decision 12; §3.7 | grants on the bucket as a whole; soft delete left on — 2. A bucket per tier is section 2's and is not counted again |
     | 6. The cost rule, service by service | AC008 | decisions 4 and 12; §3.8, the table and its bullets | the free database on `dsh`; on `dsh-staging`; Enterprise edition; keeping every image; deleting old revisions — 5 |
     | 7. Test data | AC009 | decision 5; §3.9, "Test data" | keeping all test data; wiping a tier; expiry by age — 3 |
     | 8. The release gate | AC010 | decisions 6 and 13; §3.9, "The release gate" | a fourth tier; a tagged revision on `dsh-staging`; the commit read from the running revision; the last `staging.yml` run; a commit label on the revision; the gate in the skill only — 6 |
     | 9. The rules left to this ADR | AC011 | decision 14; §3.10, the table | the tier as a Maven profile (findings section 5, approach A); the tier as a Spring profile in the jar (approach B) — 2 |

     The last column adds up to 36.
  4. Write Evidence as two tables. The first lists each statement that rests on the findings or on
     ADR-003, with its id. The second has one row for each K fact kept by T2, one id to a row: the
     id, what it establishes, the words quoted, the source as a full `https://` URL, the date read,
     and `Not verified by a run`. G and F facts are cited by id and not copied.
  5. Write Assumptions as §3.12's table, H1 to H6, plus any fact T2 moved there.
  6. Write Consequences.

     - Positive: one deploy serves the three tiers and the end user, so the end user runs what CI
       runs. Nothing costs while idle, and no exception to the cost rule is needed. No key exists.
       Production's data cannot be reached by a test, by IAM. A misconfigured revision never
       serves. A bad production rebuild never takes traffic. The release gate records what passed,
       and a release candidate displaced by a hotfix keeps its pass.
     - Negative: every caller needs a token, developers included, through the proxy. Six kinds of
       identity and per-database conditions have to be kept right. The startup check ties the
       application to a variable Cloud Run sets. A front end will need a second service. A test has
       to track what it created, and a teardown that does not run leaves data behind, bounded by the
       fixtures. A maximum of one instance answers a burst slowly, and Pub/Sub redelivers what it
       refuses. Two of the three databases are billed from their first operation. The gate trusts a
       commit status, which anyone with write access to the repository can post.

  7. Write "Consequences for the waves" from §3.13: the table, then "After the merge":
     1. The PRD is restated from the table through `dsh-plan-wave` or `dsh-reconcile-prd`. Wave 1
        task 3 is split, and its open point in Wave 7 is closed.
     2. ADR-005, the end user's packaging, is opened through `dsh-new-story`.
     3. No other issue is opened or changed without the owner's approval.
  8. Write "Alternatives considered" as one table of 36 rows: the alternative, the section it lost
     in, and the reason. One row for each alternative in step 3's last column.
  9. Write References: the findings, ADR-003, ADR-001, the PRD, this spec, `#48`, `#160` and
     `#181`, `.github/workflows/staging.yml`, `release.yml` and `hotfix.yml`, and
     `DocumentSubmissionServiceImpl`.
  10. Read the ADR once from top to bottom for one thing: a sentence about GCP with no G, F or K id
      beside it. Give it one, or word it as an assumption.
  11. Run C1, C2, C3 and C4. Expected: as §7.2 states.
  12. Commit: `docs(#181): ADR-004, the GCP tiers`.

- [ ] **T4 — `CLAUDE.md`.**

  1. In the router table, add below the row for ADR-003:
     ``| The GCP tiers: deploy, identities, names, cost and the release gate | `specs/architecture/ADR-004-gcp-tiers.md` |``
  2. Nothing else in `CLAUDE.md` changes. Its opening paragraph says nothing runs on GCP, and that
     stays true.
  3. Run C5 and C4. Expected: as §7.2 states.
  4. Commit: `docs(#181): point CLAUDE.md at ADR-004`.

- [ ] **T5 — gates and record.**

  1. `mvn -B clean install`, logged to `.logs/mvn-clean-install.log` as `CLAUDE.md` requires, with
     the exit code reported. Expected: exit 0. Gate 3 does not apply: no code changed.
  2. Run C1 to C6 once more, all of them. Expected: as §7.2 states.
  3. Add §8, "Verification results", to this spec: the commit the build started from, T1's red
     outputs, each fact of T2 as kept, dropped or moved, the output of every check, and the gate's
     exit code. Tick the tasks done. Lint. Commit: `docs(#181): record the verification`.

- [ ] **T6 — hand over.** Show the owner the "After the merge" list, and post or change nothing
  without approval. The pull request is opened in step 6, by `dsh-ship-story`, and the owner's
  merge of it is ADR-004's acceptance.

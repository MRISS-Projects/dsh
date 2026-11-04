# ADR-004: The GCP Tiers

| Field       | Value                        |
|-------------|------------------------------|
| **Date**    | 2026-11-03                   |
| **Status**  | Accepted                     |
| **Authors** | Backend Development Team     |

The owner's merge of the pull request that adds this file is the acceptance.

---

## Context

DSH runs nowhere on GCP yet. This ADR decides how it runs on its three operated tiers, `dsh-dev`,
`dsh-staging` and `dsh`: the deploy, the identities, the names, who can call the service, the
bucket, what each service costs while idle, what happens to test data, and where the release gate
runs. [ADR-003](./ADR-003-document-pipeline.md) decided how the pipeline is built, and this ADR
does not reopen it. [ADR-001](./ADR-001-GCP-based-components.md) stays the record of why DSH moves
to GCP.

"The findings" means [`profiles-and-configuration.md`](./profiles-and-configuration.md), the
output of `#48`. Its probe ids (P0 to P31), finding ids (S1 to S8, M1 to M4), rule ids (R1 to R12)
and GCP fact ids (G1 to G8) are used here as they are there. ADR-003's fact ids (F1 to F12) and
assumption ids (A1 to A6) are used as they are there, and its assumptions are always written
"ADR-003's A3". Facts read for this ADR are K1 to K19, and its assumptions are H1 to H6; both are
listed under [Evidence](#evidence) and [Assumptions](#assumptions).

Wave and task numbers are those of the re-plan of 2026-10-10 in `specs/product/PRD.md`. A later
re-plan will move them.

### What exists today

On `DEVELOP` at `30d97b8c6`, and on this ADR's task branch at `063582a62`, on 2026-11-03.

| Fact | Evidence |
|---|---|
| No GCP project exists, no image is built, and nothing is deployed | no `Dockerfile` in the repository; no workflow calls `gcloud` |
| A file already stored is found by its hash and processed again in place. No second record is created | `DocumentSubmissionServiceImpl.getTokenFromDocument`: `getDocumentByHash`, then `stored.resetStatus()` when `useCache` is false |
| Term statistics count a document once, however often they are called, and offer no removal | ADR-003, section 5, "What the interface must offer" |
| Stages are idempotent, because Pub/Sub delivers at least once | ADR-003, section 1, "A stage"; F3 |
| `staging.yml` is dispatched by hand with a `branch_name` input. The run's own commit is that of the branch it was dispatched from, which need not be the branch it builds | `.github/workflows/staging.yml`, its `on:` block |
| `release.yml` and `hotfix.yml` are dispatched by hand. `hotfix.yml` releases from a `<version>.x` branch and has no staging step | the two files; `docs/devops/README.md` |
| `deploy.yml` is dispatched by hand, on `DEVELOP` | `.github/workflows/deploy.yml`, its `on:` block |
| Each of these four workflows calls a reusable workflow of `parent-poms`. None deploys anything to GCP | the `uses:` line of each of the four files |

### Inputs

These were decided before this ADR, and it does not reopen them.

- **The owner's decisions of 2026-10-10** (PRD §4). Three operated tiers, `dsh-dev`, `dsh-staging`
  and `dsh`, in one GCP project. `dsh-dev` is deployed from `DEVELOP`, `dsh-staging` from a
  staging build, `dsh` from a release or a hotfix. Wave 4 builds `dsh-dev` first. `0.6.0` is the
  first release through the tiers.
- **ADR-003's decisions.** The `gcp` profile and the three keys it sets. Pub/Sub push to the same
  service. The push endpoints accept requests from Pub/Sub only. Nothing in the jar or the image
  names the owner's project. Stages are idempotent.
- **The cost rule** in `CLAUDE.md`. Cloud Run's minimum instances is 0, always, and an exception
  needs the owner's approval before it is written down.

The decisions below were agreed with the owner on 2026-10-28.

### What this ADR answers

ADR-003 left these to it:

- R5, R6, R8 and R10 of the findings (ADR-003, section 3).
- How the push endpoints accept requests from Pub/Sub only, which depends on whether the service
  is public (ADR-003, section 4).
- The prefix of the topics (ADR-003, section 4).
- What retention the cost rule asks of the term statistics, and whether their interface gains a
  removal (ADR-003, section 5).
- How a test that calls a store directly reaches `dsh-dev` (ADR-003's A3).
- Which edition of Firestore DSH uses, with Wave 5 (ADR-003, section 1).

The findings left these to it (§9.1, and section 7):

- The operated tiers are three runs of one deploy, each given a base name.
- The naming table of §8.1, and the five open points of §8.3.
- The deploy sets the base name and `gcp` every time, and something checks both.
- The bucket's uniform bucket-level access and its managed folder per tier.
- The cost rule service by service, the retention that removes old artifacts, and which tier
  holds the free Firestore database.
- One image tagged with the dotted version, and revisions named with dashes.
- How a hotfix reaches `dsh-staging` while a release candidate may occupy it, how layer 5
  authenticates, how tests clean up what they write, and whether the commit is a label on the
  revision.

The PRD adds two: where the release gate runs (Wave 7 task 3), and whether the end user's deploy
script is the one that deploys the operated tiers (Wave 10 task 3).

**This ADR is the first half of PRD Wave 1 task 3.** That task also covers the end user's
packaging, which Wave 10 delivers. It was split on 2026-10-28. The second half is ADR-005: the
public image and how an end user's Cloud Run reaches it, the release zip and its acceptance test,
and local mode. It builds on this ADR's deploy and names.

---

## Decision

Nine sections, one for each question the issue asks (`#181`, AC003 to AC011), in that order.

### 1. The tiers and the deploy

**Decision.** One deploy, run under three base names. A script that is safe to run twice takes the
base name, the image tag and the revision suffix, creates the tier's resources only if they are
absent, and deploys. The workflows run it, and it is also the script an end user runs. Every tier
is in `us-central1`. GitHub Actions authenticates through Workload Identity Federation and holds
no key. Six kinds of identity exist, all of them service accounts, and none has a key.

**Reason.** Three runs of one script cannot drift apart, and an end user who runs it runs what CI
runs on every deploy of `DEVELOP`. `us-central1` is one of the three regions where Cloud Storage's
free tier applies (K4). The pipeline is asynchronous and its only callers are tests, so nothing in
DSH needs a region close to anyone. A key stored as a secret is a long-lived credential (K11), and
federation needs none. An identity per tier and per role is what lets IAM refuse a tier's service,
or a test, access to another tier's data.

**Rejected.** Eight alternatives.

- **A region near the owner, such as `southamerica-east1`.** It has no Cloud Storage free tier
  (K4), and buys latency that nothing in DSH needs.
- **A script per tier.** It lets the tiers drift.
- **A separate script for the end user.** It would be the one deploy that CI never runs.
- **A service account key stored as a GitHub secret.** The action's own documentation recommends
  federation over keys, and calls a key a long-lived credential to be treated like a password (K11).
- **No maximum of instances.** It leaves the cost of a storm of redeliveries unbounded.
- **One runtime identity for every tier.** It lets `dsh-dev` write production's data.
- **One push identity for every tier.** It lets a `dsh-dev` subscription call `dsh`.
- **One test identity for layers 5a and 5b.** It gives the identity that writes test data the
  right to call production.

#### The deploy

- **Inputs.** The base name, the image tag and the revision suffix. Where the script lives and what
  it is written in are Wave 4 task 4's.
- **Safe to run twice.** It creates each resource only if it is absent: the tier's service
  accounts, database, managed folder, topics and subscriptions. Then it deploys a revision.
- **Who runs it.** `deploy.yml` for `dsh-dev`, `staging.yml` for `dsh-staging`, and `release.yml`
  and `hotfix.yml` for `dsh`. Each runs it in a DSH job after the reusable workflow of
  `parent-poms`.
- **The end user.** Wave 10 task 3 adds checks of the prerequisites and the documentation. It does
  not add a second deploy.
- **Every deploy sets:** minimum instances 0; request-based billing (G8);
  `--no-allow-unauthenticated` (K7); `DSH_NAME=<base>`; `SPRING_PROFILES_ACTIVE=gcp`; the tier's
  runtime identity; and a maximum of instances.
- **The maximum of instances** is a parameter, 1 by default on every tier. It is a guard on cost,
  not a hard limit: Cloud Run can exceed it briefly, and answers a request it has no instance for
  with HTTP 429 (K18). Pub/Sub delivers again a push that is answered with anything but a success
  code (F2), so a burst on a push endpoint is slowed, not lost. Wave 9 task 1's performance test
  revisits the value.
- **The region** is `us-central1`, for every resource that has one.

#### Authentication from GitHub Actions

A Workload Identity pool, limited by an attribute condition to the repository
`MRISS-Projects/dsh`, lets a job impersonate the service accounts below. An ID token, which a job
needs to call a private service, can only be had by impersonating a service account (K11), so
every identity a job uses is a service account. No service account key exists. The finer
conditions, such as which branches may impersonate `dsh-deployer`, are Wave 4 task 3's.

#### The identities

| Identity | Granted | Not granted | Used by |
|---|---|---|---|
| `dsh-deployer` | deploy Cloud Run services; create a tier's database, topics, subscriptions and managed folder; act as the runtime and push identities | read or write any document or object | the deploy |
| `<base>-run`, one per tier | `roles/datastore.user` with a condition on its own database (K3); object access on its own managed folder (G4); publish to its own topics | anything of another tier | the service at run time |
| `<base>-push`, one per tier | `roles/run.invoker` on its own tier's service (K7, K8) | anything else | the tier's push subscriptions |
| `dsh-tester` | invoker on `dsh-dev` and `dsh-staging`; `roles/datastore.user` with a condition on `dsh-dev-db` and `dsh-staging-db`; object access on `dsh-dev/` and `dsh-staging/` | anything on `dsh` | layer 5a, its teardown, and the test of two callers (ADR-003's A3) |
| `dsh-smoke` | invoker on all three services | any data | layer 5b, and the check of the build stamp in section 8 |
| a developer's own account | invoker on `dsh-dev` | — | a local run through `gcloud run services proxy` (K9) |

- **Splitting `dsh-smoke` from `dsh-tester`** means that a layer 5a test pointed at production by
  mistake is refused by the invoker check, which admits only identities granted on the service (K8).
- **The per-database condition** is an IAM condition on `resource.name` (K3). That it stops
  `dsh-tester` reading `dsh-db` is H3.
- **The names fit.** A service account id has 6 to 30 characters, lowercase letters, digits and
  dashes (K19). The longest here is `dsh-staging-push`, with 16, and the shortest `dsh-run`, with 7.

### 2. The names

**Decision.** The findings' table (§8.1) is accepted, with the bucket and the image made exact and
the identities added. The image keeps its dots, and the revision uses dashes. Topics and
subscriptions take the prefix the findings propose, the base name and a dash. Production is
deployed with no traffic under a fixed tag, `candidate`, and takes traffic only after layer 5b
passes on it.

**Reason.** A user pulling the image expects the dotted version as its tag, and the tag grammar
allows it (G7). A revision name allows only lowercase letters, numbers and dashes (G1). One bucket
with a managed folder per tier isolates the tiers (G4) without multiplying names that must be
globally unique. A revision deployed with no traffic under a tag can be tested at its own URL
(G5), so a bad production rebuild never serves. A revision that receives no requests is not billed
(G8), so the wait costs nothing.

**Rejected.** Three alternatives.

- **One dashed string for the image tag and the revision**, the owner's first rule. It satisfies
  the grammar (G7), and gives up the tag a user pulling the image expects.
- **A bucket per tier.** The managed folder already isolates the tiers (G4), and bucket names are
  global.
- **`dsh` deployed straight to traffic.** A bad rebuild serves until layer 5b fails.

#### The table

| Resource | Rule | `dsh-dev` | `dsh-staging` | `dsh` |
|---|---|---|---|---|
| Cloud Run service | the base name (G1) | `dsh-dev` | `dsh-staging` | `dsh` |
| Cloud Run revision | the service, then the version with dashes; `-dev-<n>` or `-rc-<n>` (G1) | `dsh-dev-0-4-0-dev-54` | `dsh-staging-0-4-0-rc-54` | `dsh-0-4-0` |
| Firestore database | the base name and `-db` (G2) | `dsh-dev-db` | `dsh-staging-db` | `dsh-db` |
| Topic, subscription | the base name, a dash, the channel (G3). The channels are Wave 6's | `dsh-dev-<channel>` | `dsh-staging-<channel>` | `dsh-<channel>` |
| Bucket | one per project, named by the deploy through `dsh.storage.bucket`; proposed `<project-id>-dsh` (G4) | the same | the same | the same |
| Object prefix, managed folder | the base name and `/` (G4) | `dsh-dev/` | `dsh-staging/` | `dsh/` |
| Image | `us-central1-docker.pkg.dev/<project>/dsh/dsh`, tagged with the dotted version (G7) | `:0.4.0-dev.54` | `:0.4.0-rc.54` | `:0.4.0` |
| Runtime identity | the base name and `-run` (K19) | `dsh-dev-run` | `dsh-staging-run` | `dsh-run` |
| Push identity | the base name and `-push` (K19) | `dsh-dev-push` | `dsh-staging-push` | `dsh-push` |

The bucket's name is proposed, not fixed: bucket names are global (G4), so an end user's project
needs a name of its own, and the deploy passes it as a value. Nothing in the image names it.

#### Production takes no traffic until it passes

`dsh` is deployed with `--no-traffic` under the tag `candidate`. Layer 5b runs against the tag's
URL (G5), with a token whose audience is the service's URL, which stays the audience when a tag is
targeted (K10). That the tagged revision accepts such a token is H6. Traffic then moves to the
revision. `dsh-dev` and `dsh-staging` are deployed straight to traffic.

#### The five open points of the findings (§8.3)

| Point | Settled by |
|---|---|
| Reusing a revision name | Wave 4 task 4, at the first re-run of a deploy. Until then the deploy assumes it fails (H2) |
| A traffic tag's first character | Avoided. The only tag is `candidate`, which starts with a letter |
| Named databases in the Firestore emulator | Wave 5 task 1, with `#49`, for layer 3; ADR-005 for local mode |
| GHCR's tag rules | ADR-005 |
| What an idle tier costs | Section 6, from the pricing pages (K14 to K17), confirmed by the budget alert (H5) |

### 3. The two silent omissions

**Decision.** The application checks itself at startup. Cloud Run sets `K_SERVICE`, the name of
the service, in every container (K5). When `K_SERVICE` is present, the application refuses to start
unless `gcp` is among the active profiles and `dsh.name` equals `K_SERVICE`. The container exits,
its startup probe never passes, and the revision takes no traffic (K6).

**Reason.** The findings (section 5) found two omissions that start cleanly: a deploy that leaves
out the base name derives production's names, and a deploy that leaves out `gcp`, or mistypes it,
runs without GCP (R2, P31). The check catches a missing base name, a missing `gcp` and a mistyped
one, before any request is served and before any write is made. A container is ready to serve only
once its startup probe passes, and one whose probe fails is shut down (K6).

**Rejected.** Two alternatives.

- **A check after the deploy only.** By then the bad revision serves, and may have written to
  production's database.
- **No default for `dsh.name`.** The default `dsh` is what lets anyone run DSH locally with no name
  at all (findings, section 5).

#### What the check changes

- **Locally and in every test** `K_SERVICE` is absent, and nothing changes.
- **An end user's deploy** names the service after the base name (section 1), so the check holds
  for them. A production deploy that leaves out `DSH_NAME` passes the check, because the default
  is `dsh`, and is harmless for the same reason.
- **Wave 4 task 5's smoke check stays** as the second line: it reads the base name the application
  reports.
- **That the deploy then fails**, instead of leaving a revision that never became ready, is H1.
- **The code is Wave 4 task 5's**, which introduces `dsh.name`.

### 4. Who can call the service

**Decision.** All three services are private. Cloud Run's invoker check stays on, and only the
identities of section 1 hold `roles/run.invoker`, each on the services the table names (K8). The
push endpoints are protected by IAM alone: only the tier's push identity, `dsh-tester`,
`dsh-smoke` and, on `dsh-dev`, a developer hold invoker. ADR-003's requirement, "the endpoints
accept requests from Pub/Sub only", is met in that form. A front end is not decided here.

**Reason.** A private service checks a push subscription's token itself, with no code in the
application (K7). Invoker is granted per service (K8), so each tier admits only the identities
chosen for it. A stray call to a push endpoint by one of those identities does no more than a
redelivery does, and every stage already tolerates a redelivery (ADR-003, section 1; F3).

**Rejected.** Five alternatives.

- **`dsh` public and the other two private.** Anyone could write to production, and be billed for.
- **All three public.** Making a service public disables the invoker check (K8), so the push
  endpoints would need a check of the token in the application.
- **IAM Conditions on `request.path`** (K8). They meet the requirement literally, stop a test from
  calling a push endpoint, and rest on the least familiar mechanism.
- **A check of the token's `email` claim in the application.** It adds code to guard against
  identities DSH chose itself.
- **Deciding a front end now.** None is planned, and Identity Platform's free tier has not been
  checked against the cost rule.

#### Layers 5a and 5b in CI

They fetch a Google-signed ID token for `dsh-tester` or `dsh-smoke`, through the impersonation of
section 1 (K11), with the service's URL as its audience (K10). The tests' HTTP client adds one
header, and nothing else in them changes.

#### A local run

A developer reaches `dsh-dev` through `gcloud run services proxy dsh-dev`, which serves the private
service on `http://localhost:8080` with the developer's token (K9). The layer 3 and 4 tests run
unchanged with that base URL. A token printed by `gcloud` also works. The documentation marks it as
fit for development only, because such a token has no audience claim (K9).

#### The push endpoints

Each tier's push subscriptions authenticate as `<base>-push`, which holds invoker on that tier's
service alone. Cloud Run checks the push token before the request reaches the application (K7).
The endpoint paths are Wave 6's.

#### A front end

Not decided. Cloud Run documents two patterns, and Wave 10 task 6 chooses between them:

- **IAP on the service**, for named Google accounts. It can be enabled on a Cloud Run service
  directly (K12). It replaces the caller's identity, so Pub/Sub push to the same service may fail
  (K12).
- **A public service that verifies a user's token itself**, through Identity Platform or Firebase
  Authentication (K13). A public service exposes the push endpoints with it (K8).

Either way the likely shape is two services from one image: a public one for `/submit` and
`/status`, and a private one for the push endpoints. ADR-003, section 2, already allows that split.
The second service's name is not the base name, so the check of section 3 has to accept it; Wave
10 task 6 settles how. Anything a front end needs that has a fixed cost while idle goes to the
owner under the cost rule.

### 5. The bucket

**Decision.** One bucket, in `us-central1`, Standard storage class, with uniform bucket-level
access, no object versioning, and soft delete off. A managed folder per tier, `<base>/`.
`<base>-run` holds object access on its own folder only, and `dsh-tester` on `dsh-dev/` and
`dsh-staging/`. No identity holds object access on the bucket as a whole. `dsh-deployer` creates
the folders and reads no object.

**Reason.** A managed folder is what lets a role be granted on a prefix, and it needs uniform
bucket-level access (G4). Regional storage in `us-central1` is inside the free tier (K4). Soft
delete is on by default, keeps a deleted object for 7 days, and bills it until then (K16), which
would bill every teardown of section 7. Setting its retention to 0 turns it off (K16).

**Rejected.** Two alternatives. A bucket per tier is section 2's.

- **Grants on the bucket as a whole.** They are not limited to a prefix (findings §8.4).
- **Soft delete left at its default.** Every object a teardown removes would be billed for a
  further 7 days (K16).

### 6. The cost rule, service by service

**Decision.** Every service takes the setting that costs nothing while idle, as the table says.
Firestore is used in its Standard edition. `dsh-dev-db` is the one free database: it is created
first, and no database is ever deleted. Delete protection is on for every database. Nothing in the
design has a fixed cost while idle, so no exception to the cost rule is needed.

**Reason.** The free tier goes to the first database created in a project, and moves to the next
one created if that database is deleted (K1). `dsh-dev` carries the most operations, and Wave 5
task 5 creates `dsh-dev-db` a wave before the other two exist. Standard edition's free tier
includes deletes, and Enterprise edition's has no delete allowance (K2); teardown deletes. Delete
protection guards production and keeps the free tier where this section puts it (K17, K1). An end
user's `dsh` gets the free tier in their own project, because it is the only database there (K1).

**Rejected.** Five alternatives.

- **The free database on `dsh`.** It would have to be created out of order to claim it (K1), for
  traffic that does not exist yet.
- **The free database on `dsh-staging`.** It has the fewest operations.
- **Enterprise edition.** Its free tier has no delete allowance (K2), and teardown deletes.
- **Keeping every image.** Artifact Registry grows past its free 0.5 GB (K4) with images no one
  deploys.
- **Deleting old revisions.** It saves nothing: a revision that receives no requests is not billed
  (G8).

#### The table

| Service | Settings | What an idle tier costs |
|---|---|---|
| Cloud Run | minimum instances 0 (G8); request-based billing (G8); a maximum of instances (section 1, K18); no tag left serving | nothing: CPU is allocated only while a request is processed (G8), and the free tier covers 2 million requests a month (K4) |
| Firestore | Standard edition (K2). `dsh-dev-db` created first, and free (K1). No point-in-time recovery and no scheduled backups, which are billed (K17) and outside the free tier (K1). Delete protection on (K17) | what it stores. Firestore bills by document read, write and delete (F6) |
| Pub/Sub | no topic retention, no retention of acknowledged messages, no snapshots, each of which bills storage (K14). The retention of unacknowledged messages is Wave 6 task 4's | nothing: Pub/Sub bills the bytes published and delivered, and the messages it retains (K14). No message moves while a tier is idle |
| Cloud Storage | section 5. Regional storage in `us-central1` is inside the free tier's 5 GB (K4) | what it stores |
| Artifact Registry | a cleanup policy removes `dev` and `rc` tags older than 14 days, keeps the 5 most recent of each, and removes untagged images (G8). Release tags are kept, because a rollback deploys them | what it stores beyond 0.5 GB (K4) |
| Revisions | not deleted. Cloud Run removes the oldest past 1,000 (G8) | nothing (G8) |
| Cloud Logging | the default retention of 30 days, which incurs no retention cost (K15) | ingestion beyond the free 50 GiB a month (K15) |

- **Nothing else is used**: no load balancer, Cloud NAT, VPC connector, Secret Manager, Cloud Build
  or Cloud Scheduler. Each would be a cost to weigh against the rule, and none is needed.
- **Two of the three databases are billed from their first operation** (K1). What each costs is
  what it stores and the operations made on it (F6); an idle one makes none.
- **The budget alert** is Wave 4 task 3's, and its amount is the owner's to set there. That an idle
  tier costs what this table says is H5.

### 7. Test data

**Decision.** Layer 5a tests are idempotent, and each removes in teardown exactly the data it
created: the documents it created, their original files and indexing output, and their term
statistics. A record that existed before the test ran is processed again and left in place. Data
created by manual calls to the API, now or from a future front end, is never removed by a test.
Teardown reaches the stores directly, under `dsh-tester`. The term statistics interface gains a
removal: forget one document's terms, the mirror of recording them once.

**Reason.** A test that leaves nothing behind does not depend on what an earlier run left. Records
are keyed by the file's hash (Context, "What exists today"), so a run whose teardown never happened
processes its leftovers again rather than adding to them, and the data stays bounded by the fixture
set even then. Term statistics count a document once and offer no removal (ADR-003, section 5), so
without a removal every test document would stay counted on its tier. `dsh-tester` has no grant on
`dsh`'s data (section 1), so teardown cannot reach production.

**Rejected.** Three alternatives.

- **Keeping all test data, bounded only by the fixtures.** It was the first proposal, and leaves
  tests that depend on what an earlier run left.
- **Wiping a whole tier before each run.** It needs a tool able to delete a tier, and a guard
  against its pointing at `dsh`.
- **Expiry by age.** It needs an expiry field. Firestore's managed deletes are outside the free
  tier (K1). And the term statistics would count an expired fixture again when it is next
  submitted.

#### How a test knows what it created

Before it submits a fixture, the test reads by the file's hash whether the document exists.
Teardown removes only the documents the test created. Data a test did not create is left alone,
whoever created it.

#### The removal

One operation on the term statistics interface, added by Wave 3 task 2 with its contract test,
and implemented on Firestore by Wave 5 task 3. It takes the document's distinct terms, which the
indexing stage already writes to the file store (ADR-003, section 1). So teardown removes the terms
before it removes the indexing output.

#### The test of two callers

ADR-003's A3 is proved in `dsh-dev` by a test that calls the store directly (Wave 5 task 7). It
reaches `dsh-dev-db` the way teardown does: from a GitHub Actions job, under `dsh-tester`, calling
Firestore directly. That this works is H4.

#### The cost rule's "set a retention"

It is met for Firestore and Cloud Storage by teardown, with the bound above behind it; for images
by the cleanup policy (section 6); and for messages by Wave 6's retention. No lifecycle rule
removes objects by age, for the reason given against expiry by age.

### 8. The release gate

**Decision.** The record that a commit passed is a GitHub commit status, `dsh-staging/acceptance`.
After layer 5a passes, `staging.yml` posts it, `success`, on the commit it deployed. The gate is a
first job of `release.yml` and `hotfix.yml`, before the reusable workflow is called, and requires
that status on the head of the branch being released. The `dsh-release` skill checks the same
status before it dispatches. There is one staging tier, `staging.yml` also accepts a hotfix branch,
and the last deploy wins `dsh-staging`.

**Reason.** Nothing in the design withdraws a revision when layer 5a fails against it, and a `staging.yml` run's own
commit is that of the branch it was dispatched from, not the commit it built (Context, "What exists
today"). So neither "what staging runs now" nor "the last staging run" proves that a commit passed.
A status on the commit records exactly that. A first job refuses before anything is written. A
hotfix that displaces a release candidate on `dsh-staging` takes nothing from it: the gate asks
whether a commit passed, not what staging runs.

**Rejected.** Six alternatives.

- **A fourth tier, `dsh-hotfix`.** A fourth database billed from its first operation (K1), for a
  tier idle almost always.
- **A hotfix as a revision tagged on `dsh-staging`, with no traffic** (G5). Pub/Sub pushes to the
  service's URL (G8), so the hotfix's pipeline would run on the release candidate's revision.
- **The gate reading the commit from the running revision.** It proves what runs, not what passed.
- **The gate reading the last `staging.yml` run.** Its commit is that of the dispatching branch.
- **The commit as a label on the revision.** A second claim, able to disagree with the status.
- **The gate in the skill only.** A manual dispatch would bypass it.

#### The record

After layer 5a passes, and before it posts the status, `staging.yml` checks under `dsh-smoke` that
the running revision's build stamp names the commit it deployed (M2). The status is then posted on
that commit. It is the one record: no label is put on the revision.

#### Where the gate runs

In a first job of `release.yml` and of `hotfix.yml`, so that a refusal stops before any write. The
job requires `dsh-staging/acceptance`, `success`, on the head of the branch being released. The
`dsh-release` skill (`#148`, Wave 7 task 3) checks it too, before it dispatches.

#### A hotfix and a release candidate

`staging.yml` accepts a hotfix branch, `<version>.x`. The last deploy wins `dsh-staging`. A
release candidate whose head passed is released without being deployed again. One whose head moved
since is staged again first, which the gate enforces. This closes the point the PRD leaves open
under Wave 7.

### 9. The rules left to this ADR

**Decision.** R5, R6, R8 and R10 of the findings are in force.

**Reason.** They are what the findings' tier model needs (section 5, approach C): one image for
every tier, the tier as one value set where the application is deployed, a missing required value
that stops the startup, a build stamp that the release gate can read, and context tests that a
profile leaked from the shell cannot change (M3).

**Rejected.** Two alternatives, both from the findings (section 5).

- **The tier as a Maven profile (approach A).** It builds a different jar for each tier (P27a,
  P27b), and a build with no tier succeeds and ships an unresolved token (P27c). The jar that
  passed on staging is then not the jar released (M4).
- **The tier as a Spring profile in the jar (approach B).** A new name needs a new file in the jar,
  and a profile that matches no file is accepted in silence (P31).

#### The table

| Rule | What it fixes |
|---|---|
| R5, the tier is one value, the base name | `dsh.name`, set by the variable `DSH_NAME`, default `dsh`. Every resource name derives from it in `application-gcp.properties` (S7, P10a) |
| R6, a required value is bound through a validated class | Under `gcp`, `dsh.storage.bucket` is required, has no default, and its absence stops the startup (P28a). Wave 5 task 1 |
| R8, the build stamps the version and the commit, and nothing else | `spring-boot:build-info`, Wave 4 task 1 (M2). Section 8 reads the commit from it |
| R10, every context test names its profile | It lands in `.github/copilot/rules/testing-patterns.md` with layer 5, in Wave 4 task 5 |

---

## Evidence

A statement about Spring or Maven behaviour cites the findings. A statement about GCP cites a
fact. What neither supports is an assumption, in the next section.

### What rests on the findings and on ADR-003

| Statement | Cites |
|---|---|
| The tier is one value, set where the application is deployed, from an unchanged jar | S7, P10a, R5 |
| A deploy that leaves out the base name, or `gcp`, starts cleanly | findings section 5; R2, P31 |
| A required value with no default, bound through a validated class, stops the startup | P28a, R6 |
| The build can stamp the version and the commit, and the application reads them | M2, R8 |
| A profile exported in the shell reaches the tests' JVM and changes their beans | M3, R10 |
| A Maven profile per tier builds a different jar per tier, and one with no tier ships an unresolved token | P27a, P27b, P27c, M4 |
| A Spring profile that matches no file is accepted in silence | P31 |
| Revision names, database ids, Pub/Sub ids, bucket names, managed folders, traffic tags, image tags, minimum instances 0 by default, billing, revisions and cleanup policies | G1 to G8 |
| Stages are idempotent, and Pub/Sub delivers at least once | ADR-003, section 1; F3 |
| A push is acknowledged by a success code, and delivered again after any other | F2 |
| Firestore bills by document read, write and delete | F6 |
| Term statistics count a document once and offer no removal | ADR-003, section 5 |

### GCP facts

**Not verified by a run.** No GCP project exists yet. Each fact is taken from the page named. The
words in quotation marks are the page's; the rest is a paraphrase. The G facts are the findings',
and the F facts ADR-003's; both are cited by id and not copied. The K facts were read for this ADR,
on 2026-11-03. Three pricing pages, Pub/Sub's, Firestore's and Google Cloud Observability's, were
read from their HTML with `curl`, because the fetching tool received none of their text.

| Id | What it establishes | Words quoted | Source | Read | Status |
|---|---|---|---|---|---|
| K1 | The free tier is one database per project, the first created, and moves when that database is deleted. Managed deletes, point-in-time recovery and backups are outside it | "The free tier applies to only one Firestore database per project." "The first database that is created in a project without a free tier database will get the free tier." "If the database with the free tier applied is deleted, the next database created will receive the free tier." "The following operations and features don't include free usage", which lists managed deletes (TTL), PITR data, backup data, restore operations and clone operations | <https://docs.cloud.google.com/firestore/quotas> | 2026-11-03 | Not verified by a run |
| K2 | The free amounts per edition. Standard's include deletes; Enterprise's table has no delete allowance | Standard edition: "Document reads", "50,000 per day"; "Document writes", "20,000 per day"; "Document deletes", "20,000 per day". Enterprise edition: "Read units", "50,000 per day"; "Real-time update units", "50,000 per day"; "Write units", "40,000 per day" | <https://docs.cloud.google.com/firestore/quotas> | 2026-11-03 | Not verified by a run |
| K3 | A Firestore role can be limited to one database by an IAM condition on `resource.name` | "You can use Identity and Access Management Conditions to configure access permissions on a per-database level." The page's condition for one database is `resource.name=="projects/PROJECT_ID/databases/DATABASE_ID"` | <https://docs.cloud.google.com/firestore/native/docs/manage-databases> | 2026-11-03 | Not verified by a run |
| K4 | The free tier of Cloud Run, Cloud Storage, Artifact Registry and Pub/Sub, and Cloud Storage's regions | Cloud Run: "2 million requests per month." Cloud Storage: "5 GB-months of regional storage (US regions only) per month"; "The Free Tier benefits for Cloud Storage apply only to usage in the us-east1, us-west1, and us-central1 regions." Artifact Registry: "0.5 GB of storage per month." Pub/Sub: "10 GiB of messages per month." | <https://docs.cloud.google.com/free/docs/free-cloud-features> | 2026-11-03 | Not verified by a run |
| K5 | Cloud Run sets `K_SERVICE` to the service's name in every container | "The following environment variables are automatically added to all the running containers except `PORT`." `K_SERVICE`: "The name of the Cloud Run service being run." | <https://docs.cloud.google.com/run/docs/container-contract> | 2026-11-03 | Not verified by a run |
| K6 | A container whose startup probe fails is shut down, and is ready to serve only once it passes. The default probe is TCP | A startup probe that does not succeed in time means "the container is shut down". "Once the startup check passes, Cloud Run considers your container ready to serve traffic." "Cloud Run automatically configures a TCP startup probe with the following default values:" | <https://docs.cloud.google.com/run/docs/configuring/healthchecks> | 2026-11-03 | Not verified by a run |
| K7 | A private service checks a push token itself. The push identity is granted `roles/run.invoker`. `--no-allow-unauthenticated` keeps a service private | "By keeping the service private you can rely on Cloud Run's automatic Pub/Sub integration to authenticate requests." "For Cloud Run only, there is a built-in authentication check that the token is valid." "Give the invoker service account permission to invoke your `pubsub-tutorial` service:", with `--role=roles/run.invoker`. "The `--no-allow-unauthenticated` flag restricts unauthenticated access to the service." | <https://docs.cloud.google.com/run/docs/triggering/pubsub-push> | 2026-11-03 | Not verified by a run |
| K8 | Invoker is granted per service; IAM Conditions can use `request.path`; a public service disables the invoker check | "You can control access on a per-service, per-job, per-worker-pool, or per-instance basis using IAM." "…(`request.path`) condition attributes to grant conditional access when invoking Cloud Run services and worker pools." "The recommended way to make a public service is to disable the Cloud Run Invoker IAM check." | <https://docs.cloud.google.com/run/docs/securing/managing-access> | 2026-11-03 | Not verified by a run |
| K9 | `gcloud run services proxy` serves a private service locally; a token printed by `gcloud` is for development only and has no audience | "This proxies the private service to `http://localhost:8080`". "For development purposes, use gcloud CLI-generated ID tokens." "such tokens lack an audience claim, which makes them susceptible to replay attacks." | <https://docs.cloud.google.com/run/docs/authenticating/developers> | 2026-11-03 | Not verified by a run |
| K10 | A token's audience is the service's URL, also when a traffic tag is targeted | "Set the audience claim (`aud`) to the URL of the receiving service or a configured custom audience." "the `aud` value must remain as the URL of the service, even when making requests to a specific traffic tag" | <https://docs.cloud.google.com/run/docs/authenticating/service-to-service> | 2026-11-03 | Not verified by a run |
| K11 | Workload Identity Federation is recommended over keys; an ID token needs a service account to impersonate | "Workload Identity Federation is recommended over Service Account Keys". "Service Account Key JSON credentials are long-lived credentials and must be treated like a password." "To generate OAuth 2.0 access tokens or ID tokens, you must provide a service account email" | <https://github.com/google-github-actions/auth> | 2026-11-03 | Not verified by a run |
| K12 | IAP can be enabled on a Cloud Run service directly, and may break Pub/Sub push to it | "By enabling IAP on Cloud Run directly, you can secure traffic with a single click". "Because IAP intercepts requests and replaces the original caller's identity," "services like Pub/Sub that rely on their own authentication might fail." | <https://docs.cloud.google.com/run/docs/securing/identity-aware-proxy-cloud-run> | 2026-11-03 | Not verified by a run |
| K13 | End users are authenticated by Identity Platform or Firebase Authentication, on a public service that verifies their token | "Using Firebase Authentication is similar to using Identity Platform." The steps include "Deploy your Cloud Run service publicly" and "Add code to your Cloud Run service to verify ID tokens." | <https://docs.cloud.google.com/run/docs/authenticating/end-users> | 2026-11-03 | Not verified by a run |
| K14 | Pub/Sub bills the bytes published and delivered, and the messages it retains; it lists no charge per topic or subscription | "Throughput is the total number of bytes written (publish throughput) to a Pub/Sub topic or read (subscribe throughput) from a subscription to a topic over an interval of time." "Every calendar month, the first 10 GiB of throughput identified as the Message Delivery Basic SKU for a billing account is free." Storage is charged when "A topic is configured to retain messages"; "storage fees are charged for retained acknowledged messages"; "message storage fees are charged for storing the messages retained by the snapshot". "If subscribers are able to process the messages within 1 day of publishing, subscription owners are not charged storage fees for unacknowledged messages." | <https://cloud.google.com/pubsub/pricing> | 2026-11-03 | Not verified by a run |
| K15 | Cloud Logging's default retention is 30 days and incurs no retention cost; its free allotment is 50 GiB a month | The retention table's row for the `_Default` bucket reads "30 days" and "Configurable" (quotas). "Logs retained for the default retention period don't incur a retention cost." (pricing). "First 50 GiB of log data per project per month." (free tier) | <https://docs.cloud.google.com/logging/quotas>, <https://cloud.google.com/products/observability/pricing>, <https://docs.cloud.google.com/free/docs/free-cloud-features> | 2026-11-03 | Not verified by a run |
| K16 | Soft delete is on by default for 7 days, bills deleted objects, and is turned off by a retention of 0 | "Soft delete is enabled by default for all buckets that support it, with a default retention duration of 7 days." "Soft-deleted objects continue to accrue storage charges until their retention period expires." "To disable soft delete, you set the retention duration to 0." | <https://docs.cloud.google.com/storage/docs/soft-delete> | 2026-11-03 | Not verified by a run |
| K17 | Delete protection prevents a database's deletion and is off by default; point-in-time recovery and backups are billed | "Use delete protection to prevent accidental deletion of a database." "You cannot delete a database with delete protection enabled until you disable delete protection." "Delete protection is disabled by default." (databases). "If you enable PITR, you are charged for the storage of PITR data." "If you enable backups, you are charged for the storage of your database backups." (pricing) | <https://docs.cloud.google.com/firestore/native/docs/manage-databases>, <https://cloud.google.com/firestore/pricing> | 2026-11-03 | Not verified by a run |
| K18 | A maximum of instances can be set, may be exceeded briefly, and a request with no instance to serve it is answered with HTTP 429 | "this maximum setting can be exceeded for a brief period due to circumstances such as traffic spikes" (maximum instances). HTTP 429: "The request was aborted because there was no available instance." "The Cloud Run service might have reached its maximum container instance limit" (troubleshooting) | <https://docs.cloud.google.com/run/docs/configuring/max-instances>, <https://docs.cloud.google.com/run/docs/troubleshooting> | 2026-11-03 | Not verified by a run |
| K19 | A service account id has 6 to 30 characters, lowercase letters, digits and dashes | "The ID must be between 6 and 30 characters, and can contain lowercase alphanumeric characters and dashes." | <https://docs.cloud.google.com/iam/docs/service-accounts-create> | 2026-11-03 | Not verified by a run |

---

## Assumptions

Each is something only a deploy can show.

| Id | Assumption | Proved by |
|---|---|---|
| H1 | A revision whose container exits at startup fails the deploy, and takes no traffic | Wave 4 tasks 4 and 5, by a deliberate deploy that leaves out `DSH_NAME` |
| H2 | Deploying a revision suffix that already exists fails | Wave 4 task 4, at the first re-run of a deploy |
| H3 | `roles/datastore.user` with a condition on `dsh-dev-db` and `dsh-staging-db` stops `dsh-tester` reading `dsh-db` | Wave 5 task 5, by a test that expects the refusal |
| H4 | Teardown and the test of two callers reach Firestore and Cloud Storage under `dsh-tester`, impersonated from GitHub Actions | Wave 5 tasks 6 and 7 |
| H5 | An idle tier costs what section 6 says | Wave 4 task 3's budget alert, and the billing report through Wave 5 |
| H6 | A revision tagged `candidate` with no traffic accepts a token whose audience is the service's URL | Wave 7 task 2 |

---

## Consequences

### Positive

- **One deploy serves the three tiers and the end user**, so the end user runs what CI runs.
- **Nothing costs while idle**, and no exception to the cost rule is needed.
- **No key exists.**
- **Production's data cannot be reached by a test**, by IAM.
- **A misconfigured revision never serves.**
- **A bad production rebuild never takes traffic.**
- **The release gate records what passed**, and a release candidate displaced by a hotfix keeps
  its pass.

### Negative

- **Every caller needs a token**, developers included, through the proxy.
- **Six kinds of identity and per-database conditions** have to be kept right.
- **The startup check ties the application to a variable Cloud Run sets** (K5).
- **A front end will need a second service**, and the startup check has to accept its name.
- **A test has to track what it created**, and a teardown that does not run leaves data behind,
  bounded by the fixtures.
- **A maximum of one instance answers a burst slowly**, and Pub/Sub delivers again what it refuses
  (K18, F2). A client of `/submit` sees the 429 itself.
- **Two of the three databases are billed from their first operation** (K1).
- **The gate trusts a commit status**, which anyone with write access to the repository can post.

---

## Consequences for the waves

This ADR does not edit the PRD. It lists the consequences, and the PRD changes go through
`dsh-plan-wave` afterwards. No issue is opened or changed before the owner approves it.

| Where | Consequence |
|---|---|
| Wave 1 task 3 | Split. This ADR is its first half. ADR-005, the end user's packaging, is opened after this ADR merges |
| Wave 3 task 2 | The term statistics interface gains a removal, and its contract test covers it (section 7) |
| Wave 4 task 2 | The image's path and tags (section 2) |
| Wave 4 task 3 | The Workload Identity pool limited to the repository; `dsh-deployer`, `dsh-tester` and `dsh-smoke`; the Artifact Registry repository `dsh` in `us-central1` with its cleanup policy; the bucket with its settings (section 5); the budget alert |
| Wave 4 task 4 | The deploy of section 1, with its settings. It creates `<base>-run` and `<base>-push`. It settles H2. The point on a traffic tag's first character is closed by section 2 |
| Wave 4 task 5 | The startup check of section 3, and H1. The smoke check runs as `dsh-smoke`. R10 and layer 5 in `testing-patterns.md` |
| Wave 5 task 1 | `dsh.storage.bucket` is required under `gcp` (R6) |
| Wave 5 task 3 | The removal on Firestore |
| Wave 5 task 5 | `dsh-dev-db` first, Standard edition, delete protection. The managed folder and the grants of section 1. H3. No lifecycle rule removes test objects by age: teardown does (section 7) |
| Wave 5 task 6 | Layer 5a authenticates as `dsh-tester`, and tears down as section 7 says. H4 |
| Wave 5 task 7 | The test of two callers runs under `dsh-tester`, directly against `dsh-dev-db` |
| Wave 6 task 2 | The push endpoints are protected by IAM alone (section 4) |
| Wave 6 task 4 | Topics and subscriptions named `<base>-<channel>`, pushed by `<base>-push` |
| Wave 7 task 1 | `staging.yml` accepts hotfix branches and posts `dsh-staging/acceptance` |
| Wave 7 task 2 | `dsh` is deployed with no traffic under `candidate`; layer 5b, then traffic moves. H6 |
| Wave 7 task 3 | The gate is a first job of `release.yml` and `hotfix.yml` requiring the status, and the skill checks it too. The open point on a hotfix and `dsh-staging` is closed |
| Wave 9 task 1 | The maximum of instances is revisited |
| Wave 10 task 3 | The end user's script is the deploy of section 1, with checks of its prerequisites |
| Wave 10 task 6 | The two front-end patterns and the constraint between them (section 4, "A front end") |

### After the merge

This ADR records the consequences, and it changes no plan by itself.

1. **The PRD is restated from the table** through `dsh-plan-wave` or `dsh-reconcile-prd`. Wave 1
   task 3 is split, and its open point in Wave 7 is closed.
2. **ADR-005, the end user's packaging, is opened** through `dsh-new-story`.
3. **No other issue is opened or changed** without the owner's approval.

---

## Alternatives considered

| Alternative | Lost to | Reason |
| --- | --- | --- |
| A region near the owner, such as `southamerica-east1` | `us-central1` (section 1) | No Cloud Storage free tier (K4), and latency nothing in DSH needs |
| A script per tier | One deploy under three base names (section 1) | The tiers drift |
| A separate script for the end user | One deploy, also the end user's (section 1) | It would be the one deploy CI never runs |
| A service account key stored as a GitHub secret | Workload Identity Federation (section 1) | A long-lived credential, which the action advises against (K11) |
| No maximum of instances | A maximum, 1 by default (section 1) | The cost of a storm of redeliveries is unbounded |
| One runtime identity for every tier | `<base>-run` per tier (section 1) | `dsh-dev` could write production's data |
| One push identity for every tier | `<base>-push` per tier (section 1) | A `dsh-dev` subscription could call `dsh` |
| One test identity for layers 5a and 5b | `dsh-tester` and `dsh-smoke` (section 1) | The identity that writes test data could call production |
| One dashed string for the image tag and the revision | Dots in the tag, dashes in the revision (section 2) | It gives up the tag a user pulling the image expects |
| A bucket per tier | One bucket, a managed folder per tier (section 2) | The folder already isolates the tiers (G4), and bucket names are global |
| `dsh` deployed straight to traffic | No traffic under `candidate` until layer 5b passes (section 2) | A bad rebuild serves until layer 5b fails |
| A check after the deploy only | The startup check (section 3) | The bad revision serves first, and may write to production's database |
| No default for `dsh.name` | The startup check (section 3) | The default lets anyone run DSH locally with no name |
| `dsh` public and the other two private | All three private (section 4) | Anyone could write to production, and be billed for |
| All three public | All three private (section 4) | The push endpoints would need a token check in the application (K8) |
| IAM Conditions on `request.path` | IAM alone (section 4) | A test could not call a push endpoint, and the mechanism is the least familiar (K8) |
| A check of the token's `email` claim in the application | IAM alone (section 4) | Code to guard against identities DSH chose itself |
| Deciding a front end now | Two patterns recorded for Wave 10 task 6 (section 4) | None is planned, and Identity Platform's free tier is unchecked |
| Grants on the bucket as a whole | Grants on each managed folder (section 5) | Not limited to a prefix (findings §8.4) |
| Soft delete left at its default | Soft delete off (section 5) | Every teardown would be billed for 7 more days (K16) |
| The free database on `dsh` | The free database on `dsh-dev` (section 6) | Created out of order to claim it (K1), for traffic that does not exist yet |
| The free database on `dsh-staging` | The free database on `dsh-dev` (section 6) | It has the fewest operations |
| Firestore Enterprise edition | Standard edition (section 6) | Its free tier has no delete allowance (K2), and teardown deletes |
| Keeping every image | A cleanup policy for `dev` and `rc` tags (section 6) | Artifact Registry grows past its free 0.5 GB (K4) |
| Deleting old revisions | Revisions kept (section 6) | It saves nothing: an idle revision is not billed (G8) |
| Keeping all test data, bounded by the fixtures | Teardown of what a test created (section 7) | Tests depend on what an earlier run left |
| Wiping a whole tier before each run | Teardown of what a test created (section 7) | A tool able to delete a tier, and a guard against `dsh` |
| Expiry by age | Teardown of what a test created (section 7) | An expiry field, managed deletes outside the free tier (K1), and an expired fixture counted again |
| A fourth tier, `dsh-hotfix` | One staging tier, the last deploy wins (section 8) | A fourth database billed from its first operation (K1), for a tier idle almost always |
| A hotfix as a tagged revision on `dsh-staging`, with no traffic | One staging tier, the last deploy wins (section 8) | Pub/Sub pushes to the service's URL (G8), so the hotfix's pipeline runs on the candidate's revision |
| The gate reading the commit from the running revision | A commit status (section 8) | It proves what runs, not what passed |
| The gate reading the last `staging.yml` run | A commit status (section 8) | Its commit is that of the dispatching branch |
| The commit as a label on the revision | A commit status (section 8) | A second claim, able to disagree with the first |
| The gate in the skill only | A first job of the release workflows (section 8) | A manual dispatch would bypass it |
| The tier as a Maven profile (findings approach A) | The tier as a value set at deploy (section 9) | A different jar per tier (P27a, P27b), and an unresolved token when the tier is left out (P27c) |
| The tier as a Spring profile in the jar (findings approach B) | The tier as a value set at deploy (section 9) | A new name needs a new file in the jar, and a profile matching no file is silent (P31) |

---

## References

- The findings: [`specs/architecture/profiles-and-configuration.md`](./profiles-and-configuration.md)
- ADR-003: [`specs/architecture/ADR-003-document-pipeline.md`](./ADR-003-document-pipeline.md)
- ADR-001: [`specs/architecture/ADR-001-GCP-based-components.md`](./ADR-001-GCP-based-components.md)
- The PRD: [`specs/product/PRD.md`](../product/PRD.md)
- This ADR's story spec: [`specs/stories/181-adr-004-gcp-tiers.md`](../stories/181-adr-004-gcp-tiers.md)
- [`#48`](https://github.com/MRISS-Projects/dsh/issues/48), the investigation of Spring and Maven
  profiles
- [`#160`](https://github.com/MRISS-Projects/dsh/issues/160), ADR-003's story
- [`#181`](https://github.com/MRISS-Projects/dsh/issues/181), this ADR's story
- The workflows: `.github/workflows/staging.yml`, `.github/workflows/release.yml`,
  `.github/workflows/hotfix.yml` and `.github/workflows/deploy.yml`
- `DocumentSubmissionServiceImpl`:
  `dsh-rest-api/src/main/java/com/mriss/dsh/restapi/service/DocumentSubmissionServiceImpl.java`

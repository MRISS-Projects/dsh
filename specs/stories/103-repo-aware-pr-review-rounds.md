---
issue: 103
slug: repo-aware-pr-review-rounds
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 103 — Make PR review rounds repo-aware and authoritative

## 1. Story

**As a** developer running a review round on a DSH pull request
**I want** the facts a reviewer needs to reach it, and a written rule for who adjudicates a
disputed finding
**So that** a round spends its time on real defects instead of on re-litigating gates the
reviewer was told about and did not apply

## 2. Context

- Wave: 0 — Engineering foundation (`specs/product/PRD.md`)
- Spun off from: `#93` / PR #102, during that story's review round
- Related: `.github/copilot-instructions.md`, `.github/copilot/rules/*`, `CLAUDE.md`,
  `.claude/skills/dsh-pr-cycle/SKILL.md`, `.claude/skills/dsh-ship-story/SKILL.md`,
  `docs/process/ai-driven-development.md`

On PR #102 Copilot raised six findings. Five were valid. One — repeated across three of its nine
comments — claimed `-Denforcer.skip=true` bypasses `enforce-coverage-data-exists` and "must be
verified/fixed in the shared parent". It does not: that execution sets `<skip>` explicitly, which
beats the parameter's `enforcer.skip` user property. A single run shows both behaviours, because
DSH's *other* enforcer execution (`enforce-lowercase-artifact-id`, root `pom.xml`) has no
`<skip>` and **is** disabled by the same flag.

`#103` was opened on the reading that two gaps produced that round: that Copilot had no
review-specific repository context, and that nothing said who adjudicates a disputed finding.

**The first of those is false, and this spec is written around the corrected diagnosis.** The
issue body is being rewritten accordingly — see §9 — in the same way `#93` was rewritten before
it was built.

## 3. The first premise is false — the evidence

Copilot code review reads `CLAUDE.md`, `REVIEW.md`, `AGENTS.md`, `*.instructions.md` and agent
skills, and reads them **from the head branch of the pull request**, not the base branch
([Copilot code review: Customization and configurability improvements][cr-changelog],
[Using custom instructions to unlock the power of Copilot code review][cr-docs]).

Copilot's review of PR #102 was submitted at `2026-09-17T19:04:42Z` against commit `fb3f0a09`:

```bash
gh api repos/MRISS-Projects/dsh/pulls/102/reviews \
  --jq '.[] | "\(.submitted_at) \(.user.login) commit=\(.commit_id[0:8]) state=\(.state)"'
```

`CLAUDE.md` at that exact commit already said, in bold, under "Quality gates":

```bash
git show fb3f0a09:CLAUDE.md | sed -n '/^## Quality gates/,/^## The development process/p'
```

> Every module with production sources holds at least 95% LINE and 95% BRANCH coverage, enforced
> by `jacoco:check` bound to `verify`, plus the `enforce-coverage-data-exists` guard that fails a
> module which produced no coverage data at all. **Both are inherited from
> `MRISS-Projects/parent-poms`, not declared here — grepping this repository will not find them.**

and, in the following paragraph:

> `-Dcoverage.data.check.skip=true` disables the guard on its own and exists for a module that is
> genuinely exempt — note that `-Denforcer.skip=true` does **not** work here, because the
> execution sets `<skip>` explicitly.

That is the precise fact the false finding contradicted: present, bolded, in a file the reviewer
reads, on the commit it reviewed.

A second, weaker instance of the same correction: all seven `dsh-*` skills were in the tree
throughout, and `.claude/skills` is one of the three documented project-skill directories
([About agent skills][skills-concepts]).

```bash
git ls-tree --name-only 2691ef0f .claude/skills/
```

## 4. The re-diagnosis

There is one gap, not two, and the first remedy's justification changes:

| Original claim | Status | What replaces it |
|---|---|---|
| Copilot had no review-specific repo context | **Disproven** | The context existed and did not reach the finding. The problem is salience and reviewer effort, not absence. |
| Nothing says who adjudicates a disputed finding | **Holds** | `dsh-pr-cycle` §2 says not to change code to satisfy a wrong claim, but not who decides or what settles it. |

This matters beyond bookkeeping. Adding a fourth copy of a fact that is already present and
already ignored would be fixing what the finding *said* rather than what was *wrong* — the exact
failure mode this story exists to prevent. The `.github/skills` file is still worth building, but
as a **salience measure under a stated hypothesis**, not as a gap-filler:

> A short file selected by description-matching and loaded whole has a better chance of being
> attended to than one bold sentence inside a 200-line router document — and `#102` was reviewed
> on `Lite`, the cheaper effort level, which is now the leading candidate for why the present
> facts were not applied.

Both candidate causes get a remedy in this story, and §11 is designed so the two do not confound
each other.

## 5. The seat rule

Reviewer-facing content and author-facing content are different jobs for different readers. This
story writes that boundary down rather than leaving it to be re-derived:

| Seat | Directory | Read by | Purpose |
|---|---|---|---|
| Ours | `.claude/skills/` | Claude | How the author works — the eight steps of the process |
| GitHub's | `.github/skills/` | Copilot code review | What an adversarial reviewer must know |

Copilot's review is adversarial by design and is driven from its own seat. Our local review in
step 5 (`superpowers:requesting-code-review`) is review of our own work — a different seat, a
different purpose, and not a substitute for the other.

The rule is **additive**. `CLAUDE.md`, `.github/copilot-instructions.md` and
`.github/copilot/rules/*` continue to be read by whoever reads them today; nothing is moved out
of them and nothing is stripped from them to make room for the new file. Losing context we
already have is a worse outcome than duplicating a sentence.

## 6. Verified facts about the configuration surface

AC003 requires the path and frontmatter be confirmed against GitHub's documentation rather than
assumed. They are, and this section is the record.

**Project-skill directories.** Exactly three are supported:
`.github/skills`, `.claude/skills`, `.agents/skills`. Each skill gets its own subdirectory, named
in lowercase with hyphens for spaces ([About agent skills][skills-concepts],
[Adding agent skills for GitHub Copilot][skills-howto]).

**`SKILL.md` frontmatter:**

| Field | Required | Constraint |
|---|---|---|
| `name` | yes | Lowercase, hyphens for spaces; typically matches the directory name |
| `description` | yes | What the skill does and when Copilot should use it |
| `license` | no | Description of the applicable licence |
| `allowed-tools` | no | Tools pre-approved for use without a confirmation prompt |

**Which ref is read.** The head branch, explicitly so that changes to instructions and skills can
be tested in the pull request that introduces them, without merging first ([cr-changelog]). This
is what makes §11's experiment possible on this story's own PR.

**One documented ambiguity, deliberately not resolved here.** The GA changelog for code review
names only `.github/skills` ([Copilot code review: Agent skills and MCP now generally
available][skills-ga]), while the concepts page lists all three directories for skills generally.
Whether code review also loads `.claude/skills` is therefore unsettled. See §12.

[cr-changelog]: https://github.blog/changelog/2026-07-17-copilot-code-review-customization-and-configurability-improvements/
[cr-docs]: https://docs.github.com/en/copilot/tutorials/customize-code-review
[skills-concepts]: https://docs.github.com/en/copilot/concepts/agents/about-agent-skills
[skills-howto]: https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/customize-cloud-agent/add-skills
[skills-ga]: https://github.blog/changelog/2026-07-29-copilot-code-review-agent-skills-and-mcp-now-generally-available/

## 7. Files to change

### 7.1 `.github/skills/code-review/SKILL.md` — new

Frontmatter per §6: `name: code-review`, and a `description` that triggers on reviewing a pull
request in this repository rather than on the process step it belongs to.

**It stays short on purpose.** Salience is the hypothesis; a long file argues against it. It
**points at** `.github/copilot-instructions.md`, `.github/copilot/rules/*` and `CLAUDE.md` for
standards rather than restating them, which is also the router rule `CLAUDE.md` itself sets.

Content is limited to what a reviewer gets wrong from the diff alone:

1. **The coverage gates are inherited.** `jacoco:check` at 95% LINE and BRANCH, and
   `enforce-coverage-data-exists`, both bound to `verify`, both from `MRISS-Projects/parent-poms`.
   They are not in this repository and grepping for them will not find them. A finding that they
   are missing, or that a flag bypasses them, must be run before it is filed.
2. **A task branch never targets `master`.** It targets `DEVELOP`, an RC branch, or a hotfix
   line. A finding that the base branch is wrong is wrong.
3. **`Refs`, not `Closes`, on a story PR.** GitHub only auto-closes on merge into the default
   branch, which is `master`, which a story PR never targets. `Closes` would read as though the
   issue were handled and do nothing.

No `allowed-tools` entry. Granting the reviewer shell access is a larger decision than this story
should make — see §12.

### 7.2 `.claude/skills/dsh-pr-cycle/SKILL.md`

§2 gains the adjudication rule, stated as a rule and not as advice:

> Claude makes the final call and does not change correct work to satisfy a review — but the
> tiebreaker is evidence, not seniority. Where no evidence can be produced either way, report the
> dispute as unresolved rather than winning it by default.

This sits with the existing three-check triage table, which already says what to verify; what it
has never said is who wins when verification is contested.

### 7.3 `.claude/skills/dsh-ship-story/SKILL.md`

Step 6 gains a note that `Balanced` is the effort level to request for a substantive pull
request, that `Lite` is cost-efficient and targeted, and that the choice is **a human action
Claude cannot take**.

**Corrected during step 7, after the fact was checked against GitHub's documentation.** This
section, the shipped skill and §7.4's process note all originally said the effort level is "not a
repository setting". **That is false.** There are three layers, and collapsing them to one denied
a setting that exists:

| Layer | What it governs |
|---|---|
| Organization default | Inherited by repositories that have not set their own |
| Repository setting | Settings > Copilot > Code review > "Review effort level" — the default for **automatic** reviews |
| Per-PR choice | Under **Reviewers** when a review is requested; applies to that review alone and changes neither default |

Sources: [Configuring code review][cfg-review] ("next to 'Review effort level', select the effort
level for automatic reviews in this repository") and the [effort levels GA changelog][effort-ga]
("Your choice only applies to that review and doesn't change the repository or organization
default").

What survives unchanged: there is no workflow to change, no file in this repository sets it, and
Claude cannot select any of the three.

**Why this is recorded rather than quietly fixed.** The claim was asserted without being checked,
inside the story whose purpose is to stop exactly that. §11.2 records the same failure twice over
already; this is the third instance and the only one caught after the code shipped to a pull
request rather than before. It also bears on the experiment: PR #105's round-1 review arrived
**automatically**, so its `Lite` came from a default — the very layer this section denied — and
not from a per-PR choice.

[cfg-review]: https://docs.github.com/en/copilot/how-tos/copilot-on-github/set-up-copilot/configure-code-review
[effort-ga]: https://github.blog/changelog/2026-08-07-copilot-code-review-effort-levels-are-generally-available/

### 7.4 `docs/process/ai-driven-development.md`

Step 7 gains the seat rule from §5 and the effort-level note, so the full account of the process
carries them and not only the skills. This mirrors how `#93` shipped: the skill and the process
doc are updated together, so neither is the sole source.

### 7.5 `specs/product/PRD.md`

The `#103` paragraph in Wave 0 asserts "Copilot had no review-specific repository context" — the
premise §3 disproves. The sentence naming two gaps is corrected to name one, and to say that the
context was present and did not reach the finding.

This is done here rather than left to step 8 because `dsh-reconcile-prd` reconciles status and
placement, not rationale. A wrong rationale left in place would outlive the issue that carried it.

## 8. Files that deliberately stay unchanged

- **`CLAUDE.md`.** Its "Quality gates" section is the fact the reviewer failed to apply, and §5's
  additive rule says it keeps being read. Trimming it to avoid duplicating the new skill would
  remove working context to solve a problem that is not duplication.
- **`.github/copilot-instructions.md` and `.github/copilot/rules/*`.** Authoring guidance, which
  is a different job from reviewing. The new skill points at them; it does not absorb them.
- **The other five `dsh-*` skills.** They are in our seat and no reviewer behaviour depends on
  them. Whether Copilot reads that directory at all is §12's open question, and speculative edits
  to seven files ahead of the answer is the wrong order.
- **Any workflow under `.github/workflows/`, with one amendment recorded below.** Effort level is
  a per-PR human choice; there is no workflow to change for it.

  **Amended during step 5.** This section originally justified leaving workflows alone with
  "`markdownlint` already covers `.github/**/*.md`, so the new file needs no glob change to be
  gated". That is true of the *lint glob* (`spec-validation.yml`, the `markdownlint` invocation)
  and incomplete about the *trigger*: the workflow's `paths:` filters list `.github/copilot/**`
  and `.github/roles.md` but not `.github/skills/**`, so a future pull request touching only the
  review skill — the file most likely to be edited alone — would never start the job. This pull
  request is unaffected, because it also touches `specs/**`, `docs/**` and `.claude/**`.

  `.github/skills/**` is therefore added to both the `push:` and `pull_request:` `paths:` lists.
  Two lines, no behaviour change to any job, and it makes §11's claim that the new file "is inside
  the enforcing gate from the moment it lands" true of the trigger as well as the glob. Nothing
  else under `.github/workflows/` is touched.

## 9. Issue body reconciliation

`#103`'s body states the disproven premise as fact and frames AC002 around context that "a
reviewer cannot infer from the diff". Building the story while leaving that in place would make
the issue and the spec disagree, and the PRD says the issue is the source of truth for rationale.

The body is rewritten before implementation starts, as `#93`'s was. The rewrite:

- replaces gap 1 with the corrected diagnosis and the `fb3f0a09` evidence,
- rewords AC002 from "facts a reviewer cannot infer from the diff" to facts the reviewer
  demonstrably failed to apply, restated where the loading model gives them a better chance,
- promotes AC005 from a footnote to a primary remedy,
- rewrites AC006 as the three-point experiment in §11,
- adds AC007 for the PRD correction.

The title stands: the story still makes review rounds repo-aware and authoritative. Only the
account of why does not.

**Claude does not edit the issue until the human approves the rewritten body.**

## 10. Acceptance criteria

- **AC001** — A review-focused agent skill exists at `.github/skills/code-review/SKILL.md`. It
  points at `.github/copilot-instructions.md`, `.github/copilot/rules/*` and `CLAUDE.md` rather
  than duplicating them, and it is short enough to read in one sitting.
- **AC002** — It states the three facts in §7.1: the coverage gates are inherited from
  `MRISS-Projects/parent-poms` and will not be found in this repository; a task branch never
  targets `master`; `Refs`, not `Closes`, on a story PR. These are facts the `#102` reviewer had
  available and did not apply; that they are restated as a salience measure is recorded in §4 of
  this spec and **not** in the skill, which addresses the reviewer and has no use for the reason.
- **AC003** — The path and frontmatter conform to GitHub's published schema, verified against the
  documentation rather than assumed. The verification is recorded in §6 with citations, and the
  shipped file matches it.
- **AC004** — `dsh-pr-cycle` §2 states the adjudication rule: Claude makes the final call and does
  not change correct work to satisfy a review, the tiebreaker is evidence rather than seniority,
  and an unevidenced dispute is reported unresolved rather than won by default.
- **AC005** — `dsh-ship-story` step 6 states that `Balanced` is the effort level for a substantive
  PR and that it is a per-PR human choice made when the review is requested.
- **AC006** — The experiment in §11.1 is run and its outcome recorded in this spec: three data
  points, with the skill's effect and the effort level's effect distinguished rather than
  confounded. Recording an inconclusive result is a pass; claiming an unmeasured cause is not.
- **AC007** — `specs/product/PRD.md`'s `#103` paragraph no longer asserts that Copilot had no
  review-specific repository context.

## 11. Testing approach

There is no production code in this story, so the gate is unchanged and adds no coverage:

```bash
mkdir -p .logs
mvn -B install > .logs/mvn-install.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-install.log"
wait $MVN_PID; echo "maven exit=$?"
```

`markdownlint` already lints `.github/**/*.md`, so `.github/skills/code-review/SKILL.md` is
inside the enforcing gate from the moment it lands:

```bash
markdownlint 'specs/**/*.md' '.github/**/*.md' 'docs/**/*.md' 'CLAUDE.md' '.claude/**/*.md' \
  --ignore 'docs/wiki/**' --config .markdownlint.json
```

### 11.1 The AC006 experiment

Two variables changed between `#102` and this story: the skill now exists, and the effort level
can be raised. Changing both at once would produce a result with no attributable cause. Because
Copilot reads skills from the head branch (§6), all three points can be taken without merging:

| Point | Effort | Skill present | Source |
|---|---|---|---|
| Baseline | `Lite` | no | PR #102, already recorded |
| Round 1 | `Lite` | yes | This PR, first review |
| Round 2 | `Balanced` | yes | This PR, re-requested review |

Baseline → round 1 isolates the skill. Round 1 → round 2 isolates effort.

**Round 1 deliberately contradicts AC005's own guidance, once.** This story ships the rule that a
substantive PR is reviewed on `Balanced`, then opens its own PR on `Lite`. That is not an
oversight: holding effort at the baseline value is the only way to attribute round 1's outcome to
the skill. AC005 governs every subsequent PR; this one is the experiment that justifies it.

**Why this PR is a fair test bed despite carrying no Java.** The diff contains the material the
`#102` finding got wrong: `dsh-pr-cycle`'s coverage-gate paragraph, the new skill's statement
that the gates are inherited and unfindable here, and §3's `-Denforcer.skip=true` analysis. A
reviewer inclined to dispute an unfindable gate has something to dispute.

**What is recorded** for each round: whether any finding asserts a gate is missing or bypassable,
the total finding count, and how many needed an answer rather than a fix — the same shape as the
`#91` tally in `docs/process/ai-driven-development.md`. (An earlier draft of this section called
that a `#102` tally. It is `#91`'s; `#102` had none, and §11.2 records `#102`'s for the first
time.)

**One assumption to verify, not assume.** Whether the effort level can be changed and the Copilot
review re-requested on an already-open PR. The issue records effort as a per-PR choice made under
Reviewers, which implies it, but implication is not evidence. If it cannot be done, round 2 moves
to the next story's PR and this section records that, rather than the story claiming a data point
it did not take.

### 11.2 Recorded outcomes

Filled in as each data point is taken.

**How a finding is counted**, stated before the numbers so rounds 1 and 2 are tallied the same
way. A **comment** is one anchored review comment, whether surfaced inline or listed under
"Suppressed comments" in the review body. A **distinct finding** is one claim, counted once even
when the reviewer repeats it against several files — so the companion-guard claim raised against
three files is one finding, not three.

| Point | Effort | Skill | Gate-missing or bypass finding? | Findings | Needed an answer, not a fix |
|---|---|---|---|---|---|
| Baseline — PR #102 | `Lite` | no | **yes** — 1 finding, restated in 3 of 9 comments | 6 distinct, in 9 comments | 1 of 6 |
| Round 1 — PR #105, first review | `Lite` | yes | **no** | **0**, in 0 comments | — (none raised) |
| Round 2 — PR #105, re-requested | `Balanced` | yes | **no** | **2 distinct, in 2 comments** | 0 of 2 |

**Baseline, reconstructed from GitHub rather than from memory.** Copilot's single review of PR #102
(`2026-09-17T19:04:42Z`, commit `fb3f0a09`, state `COMMENTED`) carried 2 surfaced inline comments
and 7 suppressed comments — 9 comments, 6 distinct findings. Five were valid and were fixed. The
sixth — that `-Denforcer.skip=true` bypasses `enforce-coverage-data-exists` and "must be
verified/fixed in the shared parent" — was false, and appeared in 3 of the 9 comments:
`.github/copilot/rules/testing-patterns.md:98`, `CLAUDE.md:110`, and
`specs/stories/93-remove-redundant-coverage-ratchet.md:284`. The effort level is not inferred: the
review body footer states it.

```bash
# the review itself - submitted_at, commit, state
gh api repos/MRISS-Projects/dsh/pulls/102/reviews \
  --jq '.[] | "\(.submitted_at) \(.user.login) commit=\(.commit_id[0:8]) state=\(.state)"'

# the body carries the 7 suppressed comments, the per-file summary table,
# and the line "Review effort level: Lite"
gh api repos/MRISS-Projects/dsh/pulls/102/reviews \
  --jq '.[] | select(.user.login | test("[Cc]opilot")) | .body'
```

The `pulls/102/comments` endpoint is **not** the way to reproduce the count: it returns 4, being
the 2 surfaced Copilot comments plus 2 human replies, and it never sees the 7 suppressed ones,
which exist only inside the review body.

**Round 1, taken on this pull request.** Copilot reviewed PR #105 automatically on open — no
review had to be requested — at `2026-09-17T22:10:55Z`, against commit `15938fe0`, state
`COMMENTED`. The verdict was "Approval recommended". The footer records `Files reviewed: 7/7`,
`Comments generated: 0` and `Review effort level: Lite`. The effort level this round needed
arrived by default, so baseline → round 1 isolates the skill exactly as designed. No finding of
any kind was raised, and in particular none asserting that a gate is missing or bypassable.

```bash
gh api repos/MRISS-Projects/dsh/pulls/105/reviews \
  --jq '.[] | select(.user.login | test("[Cc]opilot")) | .body'
gh api repos/MRISS-Projects/dsh/pulls/105/comments --paginate --jq 'length'   # 0
```

**What round 1 does and does not license saying.** The comparison is more direct than §11.1
anticipated. The exact proposition Copilot contradicted three times on `#102` — that
`-Denforcer.skip=true` does not reach `enforce-coverage-data-exists` — is asserted three times in
this diff: in `.github/skills/code-review/SKILL.md`, in §3 above, and in `specs/product/PRD.md`.
Same reviewer, same effort level, same assertion in front of it. This time it drew nothing.

That is the result the salience hypothesis predicts, but one sample does not establish it. A
zero-finding review is equally consistent with a diff that simply had less to find, and Copilot is
not deterministic — the same input can yield a different review. It is therefore recorded as **a
data point consistent with the hypothesis, not a confirmation of it.** AC006 asks for the
measurement and forbids claiming an unmeasured cause; this is the former.

**Two corrections made to this section during step 5**, both caught by the local code review and
both of the kind this story exists to prevent. The comment count was first written as 4, inherited
from the `#93`-era prose in §2 and `specs/product/PRD.md` rather than from the reconstruction it
claimed to come from; it is 3. And the reproduce block first cited the `comments` endpoint for the
figure of 9, which it does not return. Leaving either in a section headed "reconstructed from
GitHub rather than from memory" would have shipped this story's own failure mode inside its
evidence.

**Round 2, taken on this pull request.** The review was re-requested at `Balanced` and submitted
at `2026-09-17T22:31:19Z` against commit `da8b7c93`. The footer records `Files reviewed: 7/7`,
`Comments generated: 2` and `Review effort level: Balanced`. Verdict: "Changes recommended". No
finding asserted that a gate is missing or bypassable. Both findings were **valid and were
fixed** — neither needed an answer instead of a fix:

- `specs/product/PRD.md` and `specs/stories/103-…:27` still said the false `#102` finding was
  repeated **four** times, contradicting the reconstruction in this section, which says 3.

**This is the sharpest result of the three, and it is not flattering.** §11.2 already stated that
the "4" had been "inherited from the `#93`-era prose in §2 and `specs/product/PRD.md`" — it named
the two locations carrying the error and then left both uncorrected. The local `superpowers`
review missed it, round 1 at `Lite` missed it, and the author missed it. `Balanced` found it in
one pass, on unchanged code, and flagged it as "previously missed".

**What the three points support, and what they do not.**

- *Baseline → round 1* isolates the skill: at the same effort level, the false gate-bypass finding
  that recurred in 3 comments did not recur at all. Consistent with the salience hypothesis; not
  proof, for the reasons recorded above round 1.
- *Round 1 → round 2* isolates effort, and is the cleaner of the two. Same commit family, same
  skill, effort raised: findings went 0 → 2, both valid, both real defects on code round 1 had
  already reviewed. **`Balanced` is measurably deeper than `Lite` on this repository's material**,
  which is the evidence AC005's rule was previously asserted without.
- Neither point licenses a claim about *why* the `#102` false finding did not recur. Two variables
  moved across the three rounds and each was isolated once, at n=1 per cell, against a
  nondeterministic reviewer.

**The §11.1 assumption is verified, not assumed.** The effort level *can* be raised and the
Copilot review re-requested on an already-open pull request: round 2 was taken that way, and the
review body states `Review effort level: Balanced` where round 1's states `Lite`. Round 2 did not
need to move to another story's PR.

**A correction this round also produced.** Checking how the effort level is actually selected
showed that §7.3's "not a repository setting" was false — there are three layers, one of them a
repository setting. Recorded in §7.3 with citations. It bears on the reading of round 1: that
review arrived automatically, so its `Lite` came from a default, not from a per-PR choice.

## 12. Out of scope

- **Pinning Copilot's model.** Not configurable per the documentation; only effort level is
  selectable.
- **Porting any of this to `parent-poms`**, which keeps a deliberately minimal Claude setup.
- **Replacing the local `superpowers:requesting-code-review` pass in step 5.** Different seat,
  different purpose — §5.
- **Whether Copilot code review also loads `.claude/skills`.** Documented ambiguously (§6) and
  unresolved. If round 1 shows our seat leaking into GitHub's — a reviewer acting on
  author-facing process instructions — that is a follow-up issue, not a fix here, because the
  remedy would touch all seven skills and none of it is needed if the answer is no.
- **`allowed-tools` for the review skill.** Letting the reviewer run a build would have settled
  the `#102` dispute at source, which makes it worth a decision of its own rather than a line in
  this one.
- **`REVIEW.md`.** A second documented reviewer-facing vehicle, rejected here because two files
  carrying the same three facts is the duplication the router rule exists to prevent.

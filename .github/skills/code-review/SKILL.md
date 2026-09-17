---
name: code-review
description: Use when reviewing a pull request in the DSH repository - the repository facts a reviewer cannot confirm from the diff alone, and where the standards to review against are written.
---

# Reviewing a DSH pull request

Standards are written down elsewhere and are not repeated here. Review against them:

| For | Read |
|---|---|
| Coding standards, module guidelines | `.github/copilot-instructions.md` |
| Java conventions | `.github/copilot/rules/java-conventions.md` |
| API standards | `.github/copilot/rules/api-standards.md` |
| Testing patterns | `.github/copilot/rules/testing-patterns.md` |
| Commands, branch rules, quality gates | `CLAUDE.md` |

What follows is the rest: three facts about this repository that the diff does not show, and that
a finding will be wrong without.

## 1. The coverage gates are inherited, so they are not in this repository

Every module with production sources must hold at least 95% LINE and 95% BRANCH coverage. Two
executions enforce it, both bound to `verify`:

- `jacoco:check`, at `element=BUNDLE`, LINE and BRANCH >= 0.95.
- `enforce-coverage-data-exists`, which fails a module that produced no coverage data at all —
  the companion guard, because `jacoco:check` silently passes a module with no exec data.

**Both come from `MRISS-Projects/parent-poms`. Neither is declared here.** Searching this
repository for them will not find them, and that absence is the expected state, not a defect.
`mvn -B install` runs both by itself; there is no second command.

**You cannot run this build, so do not file a finding that a gate is missing or that some flag
bypasses one.** Such a claim can only be settled by running Maven against the resolved parent,
which this review cannot do. If you believe a gate is missing or bypassable, raise it as a
question in the pull request summary — never as a finding, and never as a finding carrying a
"should be verified upstream" caveat, which is the same thing.

One case in particular, because it has already been filed once and was wrong:
`-Denforcer.skip=true` does **not** disable `enforce-coverage-data-exists`. That execution sets
`<skip>` explicitly, and explicit configuration beats the parameter's `enforcer.skip` user
property. It *does* disable this repository's own `enforce-lowercase-artifact-id` rule in the root
`pom.xml`, which sets no `<skip>`.

## 2. A task branch never targets `master`

`master` carries release automation only. A task branch is cut from, and merges back into, one of
three parents: `DEVELOP`, a release-candidate branch (`staging-X.Y.Z-SNAPSHOT-RC`), or a hotfix
line (`X.Y.x`).

Which of the three is correct for a given pull request depends on where the branch was cut, and
**the diff does not show that.** So do not file a base-branch finding at all — except one: a pull
request targeting `master` is always wrong. A finding that the base *should* be `master` is
likewise always wrong.

## 3. `Refs`, not `Closes`, on a story pull request

GitHub auto-closes a linked issue only when the pull request merges into the repository's
**default** branch. Here that is `master`, which a story PR never targets — see point 2. So
`Closes #<n>` would do nothing while reading as though the issue were handled, which is why story
PR bodies say `Refs #<n>` instead.

Verified on `#98`: merged into `staging-0.3.0-SNAPSHOT-RC` with `Closes #95` in the body, and
`#95` stayed open. Do not file a finding asking for `Refs` to be changed to `Closes`.

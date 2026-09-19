---
issue: 94
slug: enforce-spec-references
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 94 — Make `check-spec-references` enforcing

## 1. Story

**As a** reviewer trusting the CI checks
**I want** `check-spec-references` to fail on a broken reference
**So that** a green check means something

## 2. Context

`.github/workflows/spec-validation.yml` runs three jobs. Two are real gates: `validate-openapi`
lints the OpenAPI spec with Redocly, and `validate-markdown` runs markdownlint. The third,
`check-spec-references`, has never been able to fail:

```bash
missing=0
while IFS= read -r line; do
  file=$(echo "$line" | grep -oP '(?<=`)/[^`]+' | head -1)
  if [ -n "$file" ] && [ ! -e ".${file}" ]; then
    echo "WARNING: Referenced file not found: $file"
  fi
done < .github/copilot-instructions.md
echo "Reference check complete."
```

`missing` is initialised, never incremented, and never read. The loop prints a `WARNING` and moves
on. The step's last command is an `echo`, so the job exits 0 whatever it found.

This is the same class of defect as the `|| true` that used to swallow every markdownlint failure
in this same workflow, fixed earlier in Wave 0. `docs/devops/README.md` currently describes the job
as advisory only — accurate, and the reason this story exists.

The job guards something worth guarding. `.github/copilot-instructions.md` is the index that points
Copilot and Claude at the specs; a reference there that no longer resolves sends an agent to a file
that does not exist. Wave 0 is moving and deleting documentation at a steady rate, so the risk is
live rather than theoretical.

## 3. What the job does today — measured

Running the job's own script against the tree at `issue-94-enforce-spec-references`:

| Outcome | Count |
|---|---|
| Backtick-quoted `/…` paths extracted | 41 |
| Resolve to an existing file or directory | 40 |
| Do not resolve | 1 |
| Exit code | 0 |

Proof that the exit code is unconditional, run against a fixture containing one deliberately broken
reference:

```text
$ # fixture: a single line reading  See `/definitely/not/here.md` for details.
$ # the script exactly as it stands in spec-validation.yml, reading that fixture
Checking that files referenced in copilot-instructions.md exist...
WARNING: Referenced file not found: /definitely/not/here.md
Reference check complete.
CURRENT SCRIPT exit=0
```

It sees the broken reference, names it, and passes.

## 4. The one unresolved reference is not a broken link

The single non-resolving path is on `.github/copilot-instructions.md:75`:

```markdown
| *(additional pages appear here after first sync)* | `/docs/wiki/<Page-Name>.md` |
```

It is the second row of the wiki table, and `<Page-Name>` is a placeholder standing for "whatever
the wiki sync brings down". The row above it, `` `/docs/wiki/Home.md` ``, is a real reference and
resolves.

This matters for how the story is scoped. There is no broken reference in the tree to repair —
AC002's "or the broken references are fixed in the same PR" has nothing to act on. What the tree
has is a piece of prose the extractor mistakes for a path. So the fix is two-sided: the checker
learns to skip placeholders, and line 75 stops looking like one.

## 5. The checker

Moves out of the YAML into `.github/scripts/check-spec-references.sh`, so that it can be run
locally and, more importantly, tested. It takes the source file and the repository root as optional
arguments — defaulting to the real ones — which is what makes the tests in §6 possible at all.

### 5.1 Exit conditions

Four, evaluated in this order:

| # | Condition | Exit | Message |
|---|---|---|---|
| 1 | `SOURCE` is not a readable file | 1 | `ERROR: reference source not found: <path>` |
| 2 | Zero references extracted | 1 | `ERROR: extracted no references from <path>; the check verified nothing.` |
| 3 | One or more references do not resolve | 1 | `ERROR: referenced file not found: <path>`, one line each |
| 4 | Otherwise | 0 | `Checked N referenced path(s); 0 missing.` |

Condition 3 is the one the issue asks for. Conditions 1 and 2 exist because without them the job
can still pass while verifying nothing — see §5.4.

### 5.2 Why placeholders are skipped before counting

A path containing `<` or `>` is a documentation placeholder, not a reference. It is skipped before
`total` is incremented, so it neither fails the run nor counts toward the zero-reference guard. A
run whose only "reference" is `` `/docs/wiki/<Page-Name>.md` `` extracted nothing real and is
treated as such.

The rule is a guard, not load-bearing: §7.4 also removes the backticks from line 75, so the tree
would pass even if the rule were deleted. Both halves are deliberate. The rule means the next
templated path someone writes does not turn the gate red; the line-75 fix means the gate is not
relying on the rule today.

### 5.3 Why `head -1` goes

The current extraction pipes through `head -1`, keeping only the first backtick-quoted path per
line. No line in `copilot-instructions.md` currently holds two, so removing it changes nothing
about today's result — the count stays 40.

It goes anyway, because it is the same defect as the counter: a check that under-reports by
construction. A gate that silently examines a subset of its input is how this story started. This
is not widening what the check covers — same file, same kind of reference — it is making the check
do what its name says.

### 5.4 Why zero references is a failure

If `copilot-instructions.md` is renamed, restructured to use relative links, or the extraction
pattern stops matching it, the loop iterates over nothing, finds nothing missing, and exits 0.
Green check, zero verification — the hole this story is closing, reopened one rename later.

This repository already names that pattern. `enforce-coverage-data-exists`, inherited from
`MRISS-Projects/parent-poms` and described in `CLAUDE.md`, fails a module that produced no coverage
data at all rather than letting "nothing measured" read as "nothing wrong". Conditions 1 and 2 are
the same idea applied to the same failure mode.

### 5.5 The script

```bash
#!/usr/bin/env bash
#
# Verify that every repository path referenced in .github/copilot-instructions.md
# resolves to a file or directory that exists.
#
# A reference is a backtick-quoted path beginning with "/", interpreted relative
# to the repository root. Paths containing "<" or ">" are documentation
# placeholders, not references, and are skipped.
#
# Usage: check-spec-references.sh [SOURCE_FILE] [REPO_ROOT]
#
# Exits non-zero when a reference does not resolve, when SOURCE_FILE cannot be
# read, or when no references are extracted at all -- a run that verified
# nothing is a failure, not a pass.

set -euo pipefail

SOURCE="${1:-.github/copilot-instructions.md}"
ROOT="${2:-.}"

if [ ! -f "$SOURCE" ]; then
  echo "ERROR: reference source not found: $SOURCE"
  exit 1
fi

total=0
missing=0

while IFS= read -r ref; do
  case "$ref" in
    *"<"* | *">"*) continue ;;
  esac
  total=$((total + 1))
  if [ ! -e "${ROOT}${ref}" ]; then
    echo "ERROR: referenced file not found: $ref"
    missing=$((missing + 1))
  fi
done < <(grep -oP '(?<=`)/[^`]+' "$SOURCE" || true)

if [ "$total" -eq 0 ]; then
  echo "ERROR: extracted no references from $SOURCE; the check verified nothing."
  exit 1
fi

echo "Checked $total referenced path(s); $missing missing."

if [ "$missing" -gt 0 ]; then
  exit 1
fi
```

Three details worth stating, because each is a place where a plausible-looking edit breaks the
gate quietly:

- **`bash`, not `sh`.** The loop is fed by process substitution, which POSIX `sh` does not have.
  The shebang and the workflow invocation both say `bash`.
- **`grep -oP` needs GNU grep.** `ubuntu-latest` has it; so does Git Bash on Windows. The pattern
  is unchanged from the current job, so this is not a new constraint.
- **`|| true` on the `grep`.** Under `set -e`, `grep` exiting 1 on no matches would otherwise kill
  the script before the zero-reference guard could report it. The guard, not the shell, decides
  what "no matches" means.

## 6. The tests

`.github/scripts/check-spec-references.test.sh`, run as a step of the same job. Plain bash — no
Node, no Python, no test framework, no new dependency. It builds fixtures under `mktemp -d`, calls
the checker with an explicit source and root, and asserts the exit code and, where it matters, that
the message names the offending path. A `trap` removes the temporary directory.

The reason for its existence is the reason for the story: a gate that silently always passed went
unnoticed for its entire life. The fix for that is not a better script, it is a check on the check.

### 6.1 Cases

| Case | Fixture | Expected |
|---|---|---|
| every reference resolves | one path, present | exit 0, `Checked 1 referenced path(s); 0 missing.` |
| a missing reference fails and is named | one path, absent | exit 1, names `/specs/gone.md` |
| a placeholder is skipped, not resolved | placeholder plus one real path | exit 0, counts 1 |
| every path on a line is checked, not just the first | two paths on one line, second absent | exit 1, names the second |
| extracting zero references is a failure | prose with no paths | exit 1, `the check verified nothing` |
| an unreadable source file is a failure | path that does not exist | exit 1, `reference source not found` |
| a directory reference resolves | trailing-slash path, present | exit 0 |

Case four is the one that locks in §5.3: under `head -1` the second path on that line is never
examined, so the case passes only against the fixed extraction.

### 6.2 Mutation evidence

A test suite that passes is not yet evidence. Each defect the checker is meant to prevent was
reintroduced into a copy of the script and the suite re-run:

| Mutant | Defect reintroduced | Result |
|---|---|---|
| A | `head -1` restored on the extraction | 2 of 7 cases fail |
| B | zero-reference guard block deleted | 1 of 7 cases fails |
| C | the `missing` increment replaced with a no-op — the original defect | 2 of 7 cases fail |

Mutant C is the bug this story exists to fix, and it is caught. Mutant B is caught by exactly the
case written for it, which is the result to want: the guard is tested, not incidentally covered.

## 7. Files to change

### 7.1 `.github/scripts/check-spec-references.sh` — new

The script in §5.5. Committed with the executable bit set:

```bash
git update-index --add --chmod=+x .github/scripts/check-spec-references.sh
```

On Windows the mode does not come along by itself. The workflow also invokes it through `bash`
explicitly (§7.3), so neither mechanism is load-bearing alone.

### 7.2 `.github/scripts/check-spec-references.test.sh` — new

The suite in §6, same executable-bit treatment.

### 7.3 `.github/workflows/spec-validation.yml`

The `check-spec-references` job's single step becomes two:

```yaml
      - name: Test the reference checker
        run: bash .github/scripts/check-spec-references.test.sh

      - name: Verify referenced spec files exist
        run: bash .github/scripts/check-spec-references.sh
```

The tests run first. If the checker's own logic is broken, the job says so before it starts
reporting on the documentation.

**And a gap that the extraction above would otherwise open.** The workflow's `push` and
`pull_request` triggers are both path-scoped, and neither list includes the new directory:

```yaml
    paths:
      - 'specs/**'
      - 'docs/**'
      - '.github/copilot-instructions.md'
      - '.github/copilot/**'
      - '.github/roles.md'
      - '.github/skills/**'
      - '.markdownlint.json'
      - 'CLAUDE.md'
      - '.claude/**'
```

Moving the script out of the YAML and into `.github/scripts/` would mean that editing the checker,
or its tests, no longer triggers the workflow that runs them. `- '.github/scripts/**'` is added to
both lists.

Only that entry. Adding `.github/workflows/**` would fire this documentation job on every
unrelated edit to the release wrappers, which is noise rather than coverage.

### 7.4 `.github/copilot-instructions.md`

Line 75, the placeholder row of the wiki table:

```markdown
| *(additional pages appear here after first sync)* | `/docs/wiki/<Page-Name>.md` |
```

becomes

```markdown
| *(additional pages appear here after first sync)* | `/docs/wiki/` plus the page name |
```

The real reference `` `/docs/wiki/` `` still resolves and is still checked; what goes is the part
that was never a path. See §5.2 for why this is done as well as the placeholder rule rather than
instead of it.

### 7.5 `docs/devops/README.md`

Two edits, both required by AC003.

**The workflow table.** The `spec-validation.yml` row currently ends:

> …and checks references from `copilot-instructions.md` — this last check is advisory only: it
> prints a `WARNING` per unresolved reference but always exits 0, so it never fails the job.

replaced with:

> …and verifies that every path referenced from `copilot-instructions.md` resolves. That last
> check is enforcing: it fails the job on an unresolved reference, on a source file it cannot
> read, and when it extracts no references at all. Its logic is covered by
> `.github/scripts/check-spec-references.test.sh`, which the same job runs first.

The row's trigger column also gains `.github/scripts/**`, to stay true to §7.3.

**The Mermaid node.** In the Pipeline Map, the `SV` node reads:

```text
SV["spec-validation.yml<br/>specs, docs, copilot files<br/>OpenAPI lint + markdownlint"]
```

becomes:

```text
SV["spec-validation.yml<br/>specs, docs, copilot files<br/>OpenAPI lint + markdownlint + spec references"]
```

The diagram sits under a sentence claiming it was checked node-for-node against the live YAML. The
reference check is a gate now, so leaving it off the node would make that sentence false.

## 8. Files that deliberately stay unchanged

| File | Why |
|---|---|
| `CLAUDE.md` | Says nothing about this job. Its quality-gates section covers the Maven gates, which are untouched. |
| `.github/workflows/ci.yml` | Different workflow, different gates. |
| Every other file under `specs/` and `docs/` | 40 of 41 references already resolve; there is nothing to repair. |
| The `validate-openapi` and `validate-markdown` jobs | Already enforcing. Out of scope. |

## 9. Acceptance criteria

| AC | Text | How it is met |
|---|---|---|
| AC001 | Either the job fails when a referenced file does not exist, or the job is removed. | §5.1 condition 3. The job is kept and made enforcing; conditions 1 and 2 close the two ways it could still pass without verifying anything. |
| AC002 | If made enforcing, it passes on the current tree, or the broken references are fixed in the same PR. | Both. The checker passes on the tree as it stands (§10), and line 75 — the only non-resolving path, and prose rather than a broken link — is corrected in §7.4. |
| AC003 | `docs/devops/README.md`'s workflow table matches the new behaviour. | §7.5, table row and Mermaid node. |

## 10. Verification and evidence

**Red — the job as it stands.** Reproduced in §3: a fixture containing one broken reference is
reported as a `WARNING` and the script exits 0.

**Green — the replacement, same fixture.**

```text
ERROR: referenced file not found: /definitely/not/here.md
Checked 1 referenced path(s); 1 missing.
NEW SCRIPT exit=1
```

**AC002 — the real tree.**

```text
$ bash .github/scripts/check-spec-references.sh
Checked 40 referenced path(s); 0 missing.
exit=0
```

40 rather than 41 because the line-75 placeholder is skipped. This run was made *before* the §7.4
edit, which is the point: the checker passes on the tree as it is, so the documentation fix is a
tidy-up rather than a rescue.

**AC002 — the real tree, after the §7.4 edit.** The count goes up by one, because line 75 stops
being a skipped placeholder and becomes a real reference to `/docs/wiki/`, which exists:

```text
Checked 41 referenced path(s); 0 missing.
exit=0
```

Both numbers are recorded deliberately. `40` is what the finished checker reports against the tree
as it stands today, and `41` is what CI will print once the PR lands — a reviewer comparing the two
should see the increment and know it is the line-75 fix, not drift.

**The test suite.**

```text
PASS: every reference resolves
PASS: a missing reference fails and is named
PASS: a <placeholder> is skipped, not resolved
PASS: every path on a line is checked, not just the first
PASS: extracting zero references is a failure
PASS: an unreadable source file is a failure
PASS: a directory reference resolves

All 7 case(s) passed.
exit=0
```

**The suite against deliberately broken checkers.** The three mutants of §6.2, each killed by the
case written for it.

All five runs above were made locally under Git Bash before this spec was written. What they do not
prove is behaviour on `ubuntu-latest` — same `bash`, same GNU `grep`, but the first CI run on the
PR is what confirms it, along with the fact that the new path-filter entry actually triggers the
workflow.

## 11. Out of scope

- **Widening what is checked beyond `copilot-instructions.md`.** Named out of scope by the issue.
  Relative links, links in `specs/` and `docs/`, and Markdown link syntax are all untouched.
- **Malformed backtick spans.** A single pair spanning two paths would extract as one nonsense
  path. No instance exists in the file. Left alone deliberately: the gate is enforcing now, so the
  day someone writes one, CI says so.
- **Making the checker resolve anchors or URLs.** It checks path existence, nothing more.
- **The `.github/skills/` path-filter entry**, which is in the workflow's trigger lists but not in
  the README's account of them. A pre-existing documentation drift, unrelated to this change.

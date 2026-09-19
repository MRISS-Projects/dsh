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

Running the job's own script against the tree at `issue-94-enforce-spec-references`, before the
§7.4 edit:

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

Six, evaluated in this order:

| # | Condition | Exit | Message |
|---|---|---|---|
| 1 | `SOURCE` is not a file | 1 | `ERROR: reference source not found: <path>` |
| 2 | `SOURCE` is not readable | 1 | `ERROR: reference source not readable: <path>` |
| 3 | The extractor itself fails — `grep` exits above 1 | 1 | `ERROR: could not extract references from <path>; grep exited <n>.` |
| 4 | Zero references extracted | 1 | `ERROR: extracted no references from <path>; the check verified nothing.` |
| 5 | One or more references do not resolve | 1 | `ERROR: referenced file not found: <path>`, one line each |
| 6 | Otherwise | 0 | `Checked N referenced path(s); 0 missing.` |

Condition 5 is the one the issue asks for. Conditions 1 to 4 exist because without them the job can
still pass, or fail for a reason it misreports, while verifying nothing — see §5.4.

### 5.2 Why placeholders are skipped before counting

A path containing `<` or `>` is a documentation placeholder, not a reference. It is skipped before
`total` is incremented, so it neither fails the run nor counts toward the zero-reference guard. A
run whose only "reference" is `` `/docs/wiki/<Page-Name>.md` `` extracted nothing real and is
treated as such.

The rule is a guard, not load-bearing: §7.4 also removes the backticks from line 75, so the tree
would pass even if the rule were deleted. Both halves are deliberate. The rule means the next
templated path someone writes does not turn the gate red; the line-75 fix means the gate is not
relying on the rule today.

### 5.3 How references are extracted

Two changes from the job's current pipeline, for the same underlying reason.

**`head -1` goes.** The current extraction pipes through `head -1`, keeping only the first
backtick-quoted path per line. No line in `copilot-instructions.md` currently holds two, so removing
it changes nothing about today's result — the count stays 41. It goes anyway, because it is the same
defect as the counter: a check that under-reports by construction. A gate that silently examines a
subset of its input is how this story started.

**Code spans are matched as pairs.** The current pattern, ``(?<=`)/[^`]+``, treats *every* backtick
as an opening delimiter, so a closing backtick followed by `/` yields a phantom reference. Measured,
against a checker that was otherwise finished:

```text
$ # source line:  Controllers live under `dsh-rest-api`/src/main/java; see `/specs/x.md`.
ERROR: referenced file not found: /src/main/java; see
Checked 2 referenced path(s); 1 missing.
exit=1
```

That is well-formed Markdown, not the malformed span §11 exempts. With the check enforcing, an
ordinary prose edit could turn `spec-validation` red naming a path nobody wrote as a reference, and
nothing in the output would suggest the extractor was at fault.

Requiring a *closing* backtick does not fix it. `` `\K/[^`]+(?=`) `` extracts the same phantom
from that line — `/src/main/java; see` plus the trailing space — because the backtick it accepts as
the closing delimiter is the *next* span's opening one. Measured: that pattern reports it too.

What works is extracting whole spans, so the regex engine consumes both delimiters, and then keeping
the ones whose content is a path:

```bash
grep -oP '`[^`]+`'     # -> `dsh-rest-api`  and  `/specs/x.md`
```

The delimiters are then stripped and anything not beginning with `/` is dropped — which also means
inline code like `` `mvn -B install` `` is ignored by an explicit rule rather than as a side effect
of the regex. On `copilot-instructions.md` this is byte-identical to the current pattern's output:
41 references, same paths, verified by `diff`.

### 5.4 Why "verified nothing" is a failure

If `copilot-instructions.md` is renamed, restructured to use relative links, or the extraction
pattern stops matching it, the loop iterates over nothing, finds nothing missing, and exits 0.
Green check, zero verification — the hole this story is closing, reopened one rename later.

The same reasoning covers three neighbouring failures, which is why conditions 1 to 4 are separate
rather than collapsed into one:

- **An unreadable source.** `-f` alone passes a file that exists but cannot be read, which then
  falls through and reports "verified nothing" — true, but it sends the maintainer hunting for a
  documentation breakage. `-r` names the real problem.
- **A failing extractor.** `grep` exits 1 on no matches and above 1 on failure: no PCRE support, an
  unreadable file. Swallowing both with `|| true` turns a missing tool into "this document contains
  no references". The status is checked, and anything above 1 is reported as what it is.
- **Zero references.** Reported by condition 4, as above.

This repository already names the pattern. `enforce-coverage-data-exists`, inherited from
`MRISS-Projects/parent-poms` and described in `CLAUDE.md`, fails a module that produced no coverage
data at all rather than letting "nothing measured" read as "nothing wrong". Conditions 1 to 4 are
the same idea applied to the same failure mode.

### 5.5 The script

```bash
#!/usr/bin/env bash
#
# Verify that every repository path referenced in .github/copilot-instructions.md
# resolves to a file or directory that exists.
#
# A reference is a path beginning with "/" inside a backtick code span,
# interpreted relative to the repository root. Spans are matched as pairs, so a
# closing backtick cannot open a reference: in "`dsh-rest-api`/src/main/java"
# the path is prose, not a reference. Inline code that is not a path is ignored,
# and a path containing "<" or ">" is a documentation placeholder, not a
# reference, and is skipped.
#
# Usage: check-spec-references.sh [SOURCE_FILE] [REPO_ROOT]
#
# Exits non-zero when a reference does not resolve, when SOURCE_FILE cannot be
# read, when the extractor itself fails, or when no references are extracted at
# all -- a run that verified nothing is a failure, not a pass.
#
# Note: this check runs from .github/workflows/spec-validation.yml, whose
# triggers are path-scoped. Every path referenced from the source file currently
# lives under one of those trigger paths, so deleting one always runs this job.
# A reference to a path outside them would not, and the gate would go quiet.

set -euo pipefail

SOURCE="${1:-.github/copilot-instructions.md}"
ROOT="${2:-.}"

if [ ! -f "$SOURCE" ]; then
  echo "ERROR: reference source not found: $SOURCE"
  exit 1
fi

if [ ! -r "$SOURCE" ]; then
  echo "ERROR: reference source not readable: $SOURCE"
  exit 1
fi

# grep exits 1 on no matches, which the zero-reference guard below reports. Any
# higher status is the extractor failing -- no PCRE support, an unreadable file
# -- and must not be allowed to read as "this document contains no references".
if spans="$(grep -oP '`[^`]+`' "$SOURCE")"; then
  grep_status=0
else
  grep_status=$?
fi

if [ "$grep_status" -gt 1 ]; then
  echo "ERROR: could not extract references from $SOURCE; grep exited $grep_status."
  exit 1
fi

total=0
missing=0

while IFS= read -r span; do
  # Strip the delimiters. The slice is safe only because the pattern above cannot
  # match fewer than three characters: a one-character span would make the length
  # negative and abort bash. An empty span -- the single blank line a here-string
  # feeds in when nothing matched -- slices to empty and is dropped just below.
  ref="${span:1:${#span}-2}"
  case "$ref" in
    /*) ;;
    *) continue ;;
  esac
  case "$ref" in
    *"<"* | *">"*) continue ;;
  esac
  total=$((total + 1))
  if [ ! -e "${ROOT}${ref}" ]; then
    echo "ERROR: referenced file not found: $ref"
    missing=$((missing + 1))
  fi
done <<< "$spans"

if [ "$total" -eq 0 ]; then
  echo "ERROR: extracted no references from $SOURCE; the check verified nothing."
  exit 1
fi

echo "Checked $total referenced path(s); $missing missing."

if [ "$missing" -gt 0 ]; then
  exit 1
fi
```

Four details worth stating, because each is a place where a plausible-looking edit breaks the gate
quietly:

- **`bash`, not `sh`.** The loop is fed by a here-string and the script uses `${span:1:...}`
  substring expansion, neither of which POSIX `sh` provides. The shebang and the workflow
  invocation both say `bash`.
- **`grep -oP` needs GNU grep.** `ubuntu-latest` has it; so does Git Bash on Windows. The pattern is
  PCRE as it was before, so this is not a new constraint — and condition 3 now reports it rather
  than letting a missing `-P` read as an empty document.
- **The `grep` status is captured, not discarded.** Assigning inside `if` keeps `set -e` from killing
  the script, while leaving the status available to distinguish "no matches" from "grep failed".
- **The delimiters are stripped by substring, not by `tr`.** `tr -d` would also delete backticks from
  inside a path, and a pipeline would hide the `grep` status that condition 3 depends on. The slice
  is safe only because the pattern cannot match fewer than three characters: a one-character span
  would evaluate to a negative length and abort bash. An empty span — the blank line a here-string
  feeds in when nothing matched — slices to empty and is dropped by the path filter, which is why
  the zero-reference guard is still what reports it. The script states that invariant inline.

## 6. The tests

`.github/scripts/check-spec-references.test.sh`, run as a step of the same job. Plain bash — no
Node, no Python, no test framework, no new dependency. It builds fixtures under `mktemp -d`, calls
the checker with an explicit source and root, and asserts the exit code and, where it matters, that
the message names the offending path. A `trap` removes the temporary directory.

One case is conditional. Revoking read access needs a platform that honours it: `chmod 000` does
nothing under Git Bash on Windows, and nothing for `root`. That case checks whether the fixture is
still readable and reports `SKIP` rather than passing for the wrong reason, so it is real coverage on
the Linux runner and an honest gap locally.

The reason the suite exists is the reason for the story: a gate that silently always passed went
unnoticed for its entire life. The fix for that is not a better script, it is a check on the check.

### 6.1 Cases

| Case | Fixture | Expected |
|---|---|---|
| every reference resolves | one path, present | exit 0, `Checked 1 referenced path(s); 0 missing.` |
| a missing reference fails and is named | one path, absent | exit 1, names `/specs/gone.md` |
| a placeholder is skipped, not resolved | placeholder plus one real path | exit 0, counts 1 |
| every path on a line is checked, not just the first | two paths on one line, second absent | exit 1, names the second |
| a closing backtick does not open a reference | a closed span followed by a prose slash, plus one real path | exit 0, counts 1 |
| inline code that is not a path is not a reference | `` `mvn -B install` `` plus one real path | exit 0, counts 1 |
| extracting zero references is a failure | prose with no paths | exit 1, `the check verified nothing` |
| a failing extractor is a tool error, not an empty document | a stub `grep` on `PATH` exiting 2 | exit 1, `grep exited 2` |
| a missing source file is a failure | path that does not exist | exit 1, `reference source not found` |
| an unreadable source file is a failure | `chmod 000` fixture; `SKIP` where not honoured | exit 1, `reference source not readable` |
| a directory reference resolves | trailing-slash path, present | exit 0 |

Two cases carry the extraction rules of §5.3: the `head -1` defect is caught because the second path
on a line is never examined under it, and the phantom-reference defect is caught by the
closing-backtick case. The stub-`grep` case is how condition 3 is reachable at all — the checker
resolves `grep` through `PATH`, so a fixture directory ahead of it can make the extractor fail on
demand without touching the script.

### 6.2 Mutation evidence

A test suite that passes is not yet evidence. Each defect the checker is meant to prevent was
reintroduced into a copy of the script and the suite re-run:

| Mutant | Defect reintroduced | Result |
|---|---|---|
| A | `head -1` restored on the extraction | 4 of 10 cases fail |
| B | zero-reference guard deleted | 1 of 10 cases fails |
| C | the `missing` increment replaced with a no-op — the original defect | 2 of 10 cases fail |
| D | unpaired extraction restored — the phantom-reference defect | 1 of 10 cases fails |
| E | extractor-failure guard deleted | 1 of 10 cases fails |
| F | unreadable-source guard deleted | 0 locally; its only case is skipped |

Mutant C is the bug this story exists to fix, and it is caught. Mutants B, D and E are each caught by
exactly the case written for them, which is the result to want: the guard is tested, not incidentally
covered.

**Mutant F is not killed locally, and that is stated rather than worked around.** Its only case is
the conditional one, so on Windows the mutant survives because the case skips. `chmod 000` and an
`icacls` deny were both tried, and neither revokes read access for this user under Git Bash. The
`-r` guard is therefore covered by the Linux runner only, and the first CI run is what confirms it.

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

The row's trigger column gains `.github/scripts/**`, to stay true to §7.3, and `.github/skills/**`,
which the workflow has had all along and this cell has always omitted. §11 lists that omission as
pre-existing drift, out of scope — but this change rewrites the cell, and line 59 of the same file
claims the table was checked node-for-node against the live YAML. Leaving a known-false entry in a
line this PR edits is not inheriting the drift, it is re-asserting it.

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

Both numbers are recorded deliberately. `40` is what the checker reported against the tree before
the line-75 fix, and `41` is what CI will print once the PR lands — a reviewer comparing the two
should see the increment and know it is that fix, not drift.

**The phantom reference of §5.3, against the finished checker.** The same line that made its
predecessor fail:

```text
$ # Controllers live under `dsh-rest-api`/src/main/java; see `/specs/present.md`.
Checked 1 referenced path(s); 0 missing.
exit=0
```

**The test suite.**

```text
PASS: every reference resolves
PASS: a missing reference fails and is named
PASS: a <placeholder> is skipped, not resolved
PASS: every path on a line is checked, not just the first
PASS: a closing backtick does not open a reference
PASS: inline code that is not a path is not a reference
PASS: extracting zero references is a failure
PASS: a failing extractor is a tool error, not an empty document
PASS: a missing source file is a failure
SKIP: an unreadable source file is a failure
      chmod does not revoke read access here
PASS: a directory reference resolves

All 10 case(s) passed; 1 skipped.
exit=0
```

**The suite against deliberately broken checkers.** The six mutants of §6.2. Five are killed by the
case written for them; F survives locally because its only case skips, which §6.2 records rather
than hides.

**The Maven gate.** `mvn -B install` — `BUILD SUCCESS`, 13 of 13 modules, `maven exit=0`. The story
touches no Java, so this confirms rather than discovers; it is recorded because CLAUDE.md makes it
the gate, not because it was in doubt.

**Markdownlint.** The documented command over `specs/`, `.github/`, `docs/`, `CLAUDE.md` and
`.claude/` exits 0, which is the same check `validate-markdown` runs on the PR.

Every run above was made locally under Git Bash. Three things they do not prove, and the first CI
run is what settles each: behaviour on `ubuntu-latest` (same `bash`, same GNU `grep`), the `-r`
guard and mutant F (both need a platform that honours `chmod`), and that the new path-filter entry
actually triggers the workflow.

## 11. Out of scope

- **Widening what is checked beyond `copilot-instructions.md`.** Named out of scope by the issue.
  Relative links, links in `specs/` and `docs/`, and Markdown link syntax are all untouched.
- **Malformed backtick spans.** A single span holding two paths still extracts as one nonsense path,
  because pairing the delimiters (§5.3) fixes which backticks open a reference, not what a person
  wrote between them. No instance exists in the file. Left alone deliberately: the gate is enforcing
  now, so the day someone writes one, CI says so.
- **Making the checker resolve anchors or URLs.** It checks path existence, nothing more.
- **A reference to a path outside the workflow's trigger filters.** All 32 unique referenced paths
  sit under one today, so deleting any of them runs this job. A future reference to, say, a path
  under `dsh-data/src/` would not, and a code-only PR could delete it while the gate stayed quiet.
  The script header records the coupling; widening the filters to cover it is not this story.

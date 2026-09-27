---
issue: 70
slug: fix-project-nav-link
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 70 — Project link not working at maven generated site

## 1. Story

**As a** reader of the DSH project site, for an RC or a release
**I want** the top-bar link that promises the project's plan to open a page that shows it
**So that** I can see what is planned for DSH, by milestone, without hitting a GitHub 404

## 2. Root cause

The issue was opened on 2020-02-22 with a title and no body. Its only comment, from 2026-09-16,
suspected parent-poms `#13`. That suspicion does not hold: `#13` was about image links in `.md`
pages, and this link is declared in DSH itself.

- DSH's root `src/site-desc/site.xml:13` declares a top-bar link,
  `<item name="Project" href="https://github.com/orgs/MRISS-Projects/projects/1" />`.
- None of the 12 module `site.xml` files declares an active `<links>`. Each has one, but only inside
  a commented-out block, so a search for `<links` finds 12 matches that do nothing. Every module
  inherits the root's link, and all 13 published sites carry it. Checked live on the root,
  `dsh-data`, `dsh-solr` and `dsh-doc-analyser/dsh-keyword-extractor`.
- The target returns **404**. The link was written for an organisation board of GitHub's "classic"
  Projects, and GitHub has retired those. The board is gone, not just moved.

Scanned on 2026-09-27, the RC site home at `rcs/products/dsh/` has two broken navigation links.
This is one of them. The other is the "Products" parent menu entry, which is computed from
parent-poms' `<url>` values. It was already out of scope for `#90`, and it is now parent-poms `#89`
(§7).

## 3. The change

One line in the root `src/site-desc/site.xml`. The target and the label both change:

```xml
<links>
    <item name="Wiki" href="https://github.com/MRISS-Projects/dsh/wiki" />
    <item name="Milestones" href="https://github.com/MRISS-Projects/dsh/milestones" />
</links>
```

**Target.** The repository's milestones page was chosen on 2026-09-27 over a GitHub Projects (v2)
board and over removing the link. The milestones already carry the plan: each wave in
`specs/product/PRD.md` maps to one (PRD §3). The page cannot disappear through a GitHub product
retirement, which is exactly how the old link died, and it needs no board kept in sync.

**Label.** "Project" becomes "Milestones". A link called "Project" that opens a milestone list would
promise something it does not deliver, and the label alone is what a reader sees.

## 4. Acceptance criteria

The issue has no criteria of its own. All of these come from this spec.

- [x] **AC001** — No published DSH page links `github.com/orgs/MRISS-Projects/projects`. After the
  staging run, this prints `0`:

  ```bash
  git fetch origin gh-pages
  git grep -l 'github.com/orgs/MRISS-Projects/projects' origin/gh-pages -- 'rcs/products/dsh/*.html' \
    | grep -vE '/(apidocs|testapidocs|xref|xref-test|jacoco|jacoco-aggregate)/' | wc -l
  ```

  *Evidence:* staging run
  [`36335688376`](https://github.com/MRISS-Projects/dsh/actions/runs/36335688376) published
  `gh-pages` commit `0ca98e688`, and the command printed `0`. As a check that the grep can match,
  the same command with the new URL finds 254 pages carrying the `Milestones` link.
- [x] **AC002** — The home page of every one of the 13 sites has a top-bar "Milestones" link to
  `https://github.com/MRISS-Projects/dsh/milestones`, and that URL returns HTTP 200.
  *Evidence:* once Pages reported `0ca98e688` `built`, the live `index.html` of the root and each of
  the 12 modules contained
  `href="https://github.com/MRISS-Projects/dsh/milestones" class="externalLink">Milestones`: 13 of
  13. The milestones URL returned `200`.
- [x] **AC003** — `#70` gets a comment that summarises §2 and §3 and links this spec. The issue has
  never had a body, and the comment is where its diagnosis lives.
  *Evidence:* [the comment on `#70`](https://github.com/MRISS-Projects/dsh/issues/70#issuecomment-5858094796).
- [x] **AC004** — `mvn -B clean install` passes with both quality gates, per `CLAUDE.md`.
  *Evidence:* on commit `d15df4d20`, `maven exit=0`, `BUILD SUCCESS`, and
  `All coverage checks have been met.` in each of the 8 code-bearing modules.

**Red/green**, per §5: before the change the local site build gave `13` home pages with the old link
and `0` with the new one. After it, `0` and `13`. Both builds printed `BUILD SUCCESS`.

## 5. Testing approach

This is site configuration, so the red/green cycle runs on site builds, not unit tests. The build
is `#90`'s, and the flags match CI:

```bash
mkdir -p .logs
mvn -B -Ddeployment -Drelease.type=rcs -Dbuild.number=RC0 -DskipTests clean install site \
  > .logs/mvn-site-deployment.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-site-deployment.log"
wait $MVN_PID; echo "maven exit=$?"
```

Then count the 13 home pages that carry each link:

```bash
grep -l 'github.com/orgs/MRISS-Projects/projects/1' $(find . -path '*/target/site/index.html') | wc -l
grep -l 'href="https://github.com/MRISS-Projects/dsh/milestones"' $(find . -path '*/target/site/index.html') | wc -l
```

**Red** is `13` then `0`. **Green** is `0` then `13`.

The build regenerates the root `README.md` through the `readme-generation` profile. Restore it with
`git checkout README.md` after each run.

**End to end**: dispatch `staging.yml` on the task branch, as `#90` did, then run AC001 and AC002
against `gh-pages` and the live site, once Pages reports the new commit `built`. The run pushes an
`Auto-generated README.md` commit to the task branch, so pull before the next commit.

**Not tested here**: the release site. It is built from the same `site.xml`, and the release run is
its evidence.

## 6. Documentation

Nothing else links the old board. Before the change, `grep -rn "projects/1"` outside `target/` and
`docs/wiki/` found only `site.xml:13`. After it, the search finds only this spec. The wiki's "Code Based Site and Reports" page lists site URLs, not the top bar,
so it does not change.

## 7. Out of scope

- The "Products" entry in the root's parent menu, which links `../index.html` and 404s. It is
  computed from `<url>` values that parent-poms defines, so the fix belongs there: parent-poms `#89`,
  raised 2026-09-27 with no milestone. Under the standing rule, no parent-poms work is picked up
  before DSH 0.3.0 ships. Overriding the menu in DSH's `site.xml` was rejected as a local
  workaround for a shared gap.
- The other broken links recorded in `#90`'s §4 and §7: the licenses pages and the two `README.html`
  images, which are there by design.
- A CI check for external link rot. The site had one dead external link, and a checker that fetches
  third-party URLs on every run would be flakier than the defect it guards against.

## 8. Implementation order

Commits are one per task, with messages `build(#70): ...` / `docs(#70): ...`, each ending in the
session's attribution lines.

### Task 1 — red, then the fix

**Files:** Modify `src/site-desc/site.xml:13`.

- [x] Run §5's build and both counts. Expect `13` and `0`. Restore `README.md`.
- [x] Replace the `Project` item with the `Milestones` item, exactly as in §3.
- [x] Re-run the build and counts. Expect `0` and `13`. Restore `README.md`.
- [x] Run `mvn -B clean install` logged to `.logs/mvn-clean-install.log`, and report the exit code
      (AC004).
- [x] Commit: `build(#70): point the site's top-bar link at the milestones page`.

### Task 2 — publish and verify

- [x] Push the branch and dispatch `staging.yml` with `branch_name=issue-70-fix-project-nav-link`.
      Record the run URL, then `git pull` for the README commit.
- [x] Run AC001 and AC002, and record the output.
- [x] Fill §4's checkboxes with the evidence.
- [x] Commit: `docs(#70): record the staging evidence in the story spec`.

### Task 3 — document and ship

- [x] Post the AC003 comment on `#70`, after the user approves its text.
- [ ] Hand over to `dsh-ship-story`: local review, push, and the PR into
      `staging-0.3.0-SNAPSHOT-RC`.

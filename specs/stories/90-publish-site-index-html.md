---
issue: 90
slug: publish-site-index-html
parent_branch: staging-0.3.0-SNAPSHOT-RC
wave: 0
milestone: 0.3.0-SNAPSHOT
---

# Story 90 — index.html missing from published site on gh-pages (root + all submodules)

## 1. Story

**As a** reader of the DSH project site, for an RC or a release
**I want** every module's home page, and every link under *Documentation*, *Code Reports* and
*Modules*, to open a page
**So that** the 0.3.0-SNAPSHOT RC site and the 0.3.0 release site are as navigable as the 0.2.4
release site was

## 2. Root cause

The issue suspected the publish step (`maven-scm-publish-plugin`) or the staging step. It is
neither. **The pages are never generated** under the flags CI uses.

- DSH's root `pom.xml` sets `maven-site-plugin`'s `siteDirectory` to `src/site-desc`, which holds
  only `site.xml`. The markdown and FAQ sources in each module's `src/site` are copied, filtered, to
  `target/generated-site` by the `<resources>` blocks at `process-resources` (root `pom.xml:109-129`,
  repeated in every module's `pom.xml`). The site plugin reached them only through its **default**
  `generatedSiteDirectory`, which is `${project.build.directory}/generated-site`.
- parent-poms commit `2b342fba` (2026-09-15, fixing parent-poms `#55`) redirects
  `generatedSiteDirectory` to `${project.build.directory}/generated-site-reports` inside the
  `deployment` profile of `products/pom.xml`. That is correct for a product that renders from
  `src/site`: its PDF-staging copy of `src/site` into `target/generated-site` would otherwise be
  scanned twice and clash.
- DSH inherits that profile. Under `-Ddeployment`, which every staging, release and hotfix site build
  passes, the site plugin no longer looks at `target/generated-site`. So no markdown or FAQ document
  renders, in any module, and `index.html`, `faq.html`, `dev-FAQ.html`, `releases-history.html` and
  `README.html` are all absent. The report pages are unaffected, which is why the site looked mostly
  complete.

A local `mvn site` without `-Ddeployment` does not activate the profile, and that is why the issue
saw generation work locally.

### Evidence

| Run | Flags | Site plugin log | `index.html` |
|---|---|---|---|
| Staging run `36286315531`, every module | CI's | `Rendering N report documents` only | absent |
| Local, `dsh-data` | `-Ddeployment` | `Rendering 18 report documents` only | absent |
| Local, `dsh-data` | none | `Rendering 3 generated Doxia documents: 2 fml, 1 markdown` | present |
| Local, root + `dsh-data`, probe override (§3) | `-Ddeployment` | root: `5 generated Doxia documents: 2 fml, 3 markdown`; `dsh-data`: `3 ...` | present |

The probe's local logs are `.logs/mvn-site-deployment-dsh-data.log`, `.logs/mvn-site-dsh-data.log`
and `.logs/mvn-site-deployment-probe.log`. Those are gitignored, so the numbers above are the record.
The publish step is also cleared independently: JaCoCo's own `index.html` files under
`dsh-coverage-report/jacoco-aggregate/` do reach `gh-pages`, so nothing filters the file name.

## 3. The change

One element in the root `pom.xml`, in the existing `maven-site-plugin` block (lines 186-193):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-site-plugin</artifactId>
    <configuration>
        <!-- DSH-specific: site descriptor lives in src/site-desc -->
        <siteDirectory>src/site-desc</siteDirectory>
        <!-- DSH-specific: the markdown and FAQ pages are rendered from target/generated-site,
             where the <resources> blocks copy src/site filtered. parent-poms' 'deployment' profile
             (products/pom.xml, 2b342fba, parent-poms#55) redirects generatedSiteDirectory away from
             it, which is right for a product rendering from src/site but left every DSH module
             without index.html under -Ddeployment (MRISS-Projects/dsh#90). siteDirectory above holds
             only site.xml, so pointing back here cannot produce the clash #55 fixed. -->
        <generatedSiteDirectory>${project.build.directory}/generated-site</generatedSiteDirectory>
    </configuration>
</plugin>
```

Every module inherits it; no module's `pom.xml` configures `maven-site-plugin` itself. Build-level
configuration in DSH wins over the parent's profile configuration, because the parent's profile
is merged into the parent's model before DSH inherits from it. The probe confirmed this.

### Why here and not in parent-poms

parent-poms' redirect is right for the conventional layout. DSH's `src/site-desc` arrangement is
the exception, and the correction belongs where the exception is declared. Moving DSH to the
conventional layout was considered and rejected: `README.md` and `releases-history.md` must be
filtered, so rendering `src/site` raw would publish `${...}` placeholders, and rendering the
filtered copy needs `target/generated-site`, which brings back the clash.

## 4. Link inventory

A link check of the 213 non-generated pages published under `rcs/products/dsh` (excluding
`apidocs`, `testapidocs`, `xref`, `xref-test` and JaCoCo output), run before this change:

| Target | State today | After this story |
|---|---|---|
| `index.html`, root and all 12 modules | missing | fixed by §3 |
| `releases-history.html` (root) | missing | fixed by §3 |
| *Code Reports*: GitHub report, `dsh-coverage-report/jacoco-aggregate/index.html`, `surefire.html` (127 tests, aggregated), Javadoc, Test Javadoc, `xref`, `xref-test` | working | unchanged |
| `failsafe.html` | working | unchanged |
| Root *Parent* menu, "Products" → `../index.html` | broken, also broken in 0.2.4 | out of scope (§7) |
| `LICENSE.txt`, `src/assemble/EHCACHE-CORE-LICENSE.txt`, `jquery.org/license` on the licenses pages | broken | out of scope (§7) |
| Root `README.html` images: `dsh-coverage-report/badges/jacoco.svg`, `/src/site/resources/images/swagger-ui.jpg` | not yet visible: the page was not rendered | broken by design, also in 0.2.4 (§7) |

Compared with the 0.2.4 release site (`releases/dsh`), four pages look missing and are not:
newer `maven-project-info-reports-plugin` versions renamed `project-summary.html`, `team-list.html`,
`source-repository.html` and `issue-tracking.html` to `summary.html`, `team.html`, `scm.html` and
`issue-management.html`, and the RC menus link the new names. 0.2.4's `surefire-report.html` is
`surefire.html` in the current site, as `site.xml` already declares.

The 0.3.0 release site is built by `project-release.yml` with the same `-Ddeployment`, so §3 applies
to it too. It can only be observed on the release run itself.

## 5. Acceptance criteria

The issue's three criteria are AC001-AC003. AC004 and AC005 are added by this spec.

- [x] **AC001** — The root module's `index.html` is published to `gh-pages` and
  `https://mriss-projects.github.io/dsh/rcs/products/dsh/index.html` returns HTTP 200.
  *Evidence:* staging run [`36289784377`](https://github.com/MRISS-Projects/dsh/actions/runs/36289784377)
  published `gh-pages` commit `51b1bca3`. Once Pages reported that commit `built`, the `curl` printed
  `200`. The same check while Pages was still `building` printed `404`, so wait for the build.
- [x] **AC002** — Every module's `index.html` is published under its path. This command, after the
  staging run, prints 13 lines: the root and the 12 modules listed in §6.

  ```bash
  git fetch origin gh-pages
  git ls-tree -r --name-only origin/gh-pages \
    | grep -E '^rcs/products/dsh/(([a-z-]+/){0,2})index\.html$' \
    | grep -vE '/(apidocs|testapidocs|xref|xref-test|jacoco|jacoco-aggregate)/'
  ```

  *Evidence:* 13 lines after run `36289784377`: `index.html` under the root, `dsh-coverage-report`,
  `dsh-data`, `dsh-doc-analyser` and its three sub-modules, `dsh-doc-indexer-worker`, `dsh-rest-api`,
  `dsh-solr` and its two sub-modules, and `dsh-test-dataset`.

- [x] **AC003** — The root cause is documented: §2 of this spec, the comment in §3, and a comment on
  `#90` that summarises §2 and links this spec.
  *Evidence:* §2, the comment in the root `pom.xml`, and
  [the comment on `#90`](https://github.com/MRISS-Projects/dsh/issues/90#issuecomment-5852233188).
- [x] **AC004** — The §4 link check, re-run against the published tree, reports no broken link except
  the out-of-scope and by-design rows of §4's table.
  *Amended after the staging run.* The criterion first read "the two out-of-scope rows". The run
  rendered the root `README.html` for the first time, which exposed its two images, so the by-design
  row was added to §4 with its reason in §7.
  *Evidence:* the checker resolves every relative `href`/`src` against the tree extracted by
  `git archive origin/gh-pages rcs/products/dsh`, skipping the generated trees listed in §4. It
  reports each distinct pair of missing file and `href` once, so a module whose pages link its own
  `index.html` as `index.html`, `./<module>/index.html` and `<module>/index.html` counts three
  times.
  - **Before the change:** 213 pages, 81 broken pairs. 40 are the module `index.html` links,
    1 is `releases-history.html`, 39 are the licenses rows (13 each of `LICENSE.txt`,
    `EHCACHE-CORE-LICENSE.txt` and `jquery.org/license`), and 1 is the "Products" `../index.html`.
    No page linked `faq.html`, `dev-FAQ.html` or `README.html`, so they were missing but not counted.
  - **After run `36289784377`:** 254 pages, 42 broken pairs. That is the same 39 licenses pairs and
    the "Products" link, plus the two `README.html` images. Nothing is left outside §4's last three
    rows.
  - **The 41 new pages** are exactly what the fix renders: `index.html`, `faq.html` and
    `dev-FAQ.html` in each of the 13 modules, plus the root `README.html` and
    `releases-history.html`. No page was removed.
- [x] **AC005** — `mvn -B clean install` passes with both quality gates, per `CLAUDE.md`.
  *Evidence:* on commit `089c7fbb2`, `maven exit=0`, `All coverage checks have been met.`,
  `BUILD SUCCESS`.

## 6. Testing approach

This is build configuration, so the red/green cycle runs on builds, not unit tests.

**Red**, already observed (§2) and re-run on the branch before the edit, so that the evidence comes
from the branch head:

```bash
mkdir -p .logs
mvn -B -Ddeployment -Drelease.type=rcs -Dbuild.number=RC0 -DskipTests clean install site \
  > .logs/mvn-site-deployment.log 2>&1 &
MVN_PID=$!
echo "Monitor with:  tail -f .logs/mvn-site-deployment.log"
wait $MVN_PID; echo "maven exit=$?"
```

The command needs `install`, not just `site`. The markdown only reaches `target/generated-site` at
`process-resources`. CI gets there through the `clean deploy` step that runs before the site-deploy
step in the same workspace.

Then check the 13 module site directories:

```bash
for m in . dsh-test-dataset dsh-data dsh-rest-api dsh-solr dsh-solr/solr-terms-vector-order \
         dsh-solr/solr-advanced-numbers-filter dsh-doc-indexer-worker dsh-doc-analyser \
         dsh-doc-analyser/dsh-keyword-extractor dsh-doc-analyser/dsh-top-sentences-extractor \
         dsh-doc-analyser/dsh-doc-processor-worker dsh-coverage-report; do
  [ -f "$m/target/site/index.html" ] && echo "ok      $m" || echo "MISSING $m"
done
```

Red means 13 `MISSING`. **Green** means 13 `ok` after §3, plus `target/site/releases-history.html`,
and `grep -c "generated Doxia documents" .logs/mvn-site-deployment.log` printing 13.

The build regenerates the root `README.md` through the `readme-generation` profile. Restore it with
`git checkout README.md` after each run. It is committed only by the staging workflow.

**End to end**: dispatch `staging.yml` on the task branch, as `#87` did. It publishes to
`rcs/products/dsh`, and that is the site the RC reads anyway. Then run AC001, AC002 and AC004. The run
also pushes an `Auto-generated README.md` commit to the task branch, so pull before the next commit.

**Not tested here**: the release site. `project-release.yml` builds it with the same flag (§4), and
the release run is its evidence.

## 7. Out of scope

- The root's "Products" parent link and the licenses pages' `LICENSE.txt` links. Both are broken in
  0.2.4 as well, and neither is a *Documentation*, *Code Reports* or *Modules* link.
- The two images in the site's root `README.html` (§4). This is deliberate, not a defect, and
  should not be raised as one. `src/site/markdown/README.md` is filtered both into the repository's
  root `README.md` and into the site. GitHub and Doxia resolve relative paths from different roots,
  so no single path works in both. parent-poms made the same trade-off in `#13` (commit `46467306`)
  for `eclipse.md`: one canonical home, no build-time rewriting. `README.md`'s home is GitHub, where it is the
  repository front page, so its images resolve there and the site copy accepts two broken images.
  `eclipse.md` in parent-poms is the same trade-off made in the site's favour.
- The 2020 site still served at `snapshots/dsh` — `#127`.
- The wiki's release link — `#128`, which will find `#90` closed.
- A workflow guard that fails a publication when `index.html` is missing: raised as
  parent-poms `#88`, milestone `3.10.0-SNAPSHOT`, to be worked after DSH 0.3.0.
- Stale files already on `gh-pages`. `skipDeletedFiles=true` never removes them, and none of them is
  a broken link.

## 8. Implementation order

Commits are one per task, messages `build(#90): ...` / `docs(#90): ...`, each ending in the session's
attribution lines.

### Task 1 — red, then the fix

**Files:** Modify `pom.xml:186-193`.

- [x] Run §6's red build and the 13-directory check. Expect 13 `MISSING`, and no
      `generated Doxia documents` line in the log. Restore `README.md`.
- [x] Add the `generatedSiteDirectory` element and its comment exactly as in §3.
- [x] Re-run the same build and check. Expect 13 `ok`, `releases-history.html` present, and 13
      `generated Doxia documents` lines. Also run `grep -c "clashes with existing" .logs/mvn-site-deployment.log`,
      which must print 0. Restore `README.md`.
- [x] Run `mvn -B clean install` logged to `.logs/mvn-clean-install.log`, with the exit code reported
      (AC005).
- [x] Commit: `build(#90): render DSH site pages from generated-site under -Ddeployment`.

### Task 2 — publish and verify

- [x] Push the branch and dispatch `staging.yml` with `branch_name=issue-90-publish-site-index-html`.
      Record the run URL. Then `git pull` for the README commit.
- [x] Run AC001 (`curl -s -o /dev/null -w '%{http_code}'` on the URL) and AC002. Record the output.
- [x] Re-run the §4 link check against `git archive origin/gh-pages rcs/products/dsh`, and record
      that only the out-of-scope and by-design rows remain (AC004).
- [x] Fill §5's checkboxes with the evidence.
- [x] Commit: `docs(#90): record the staging evidence in the story spec`.

### Task 3 — document and ship

- [x] Post the AC003 comment on `#90`, after the user approves its text.
- [ ] Hand over to `dsh-ship-story`: local review, push, and the PR into
      `staging-0.3.0-SNAPSHOT-RC`.
- [ ] At the end of the story, offer the PRD rule the user proposed on 2026-09-26: a parent-poms
      hotfix `-SNAPSHOT` may be pinned when a wave needs upstream work, and the upstream issues are
      opened and fixed within the same story. It carries two caveats. maven-release-plugin refuses a
      SNAPSHOT parent, so the hotfix is released and re-pinned before any DSH release. And a patch
      version carries fixes only.

# docs/wiki — the wiki's source

The pages of the [DSH wiki](https://github.com/MRISS-Projects/dsh/wiki) are written here and
published from here. Each top-level `.md` file except this README is one page, named after the
file: `Home.md` is the wiki's Home page.

## How a page is published

`.github/workflows/wiki-publish.yml` runs on every push to `DEVELOP` that changes this directory,
and can be dispatched by hand. It runs `.github/scripts/publish-wiki.sh`, which mirrors this
directory into the wiki's own repository. A file added here adds a page, a file changed changes
it, and a file deleted deletes it. This README is not published.

## Editing

Edit the pages here, by pull request, like any other file. They are reviewed, and markdownlint
checks them with the rest of the repository's Markdown. Do not edit the wiki on GitHub: the next
publish overwrites the edit.

## Writing a page

- **Links between pages** use the wiki's form, the page's name without `.md`:
  `[Architecture](Architecture)`. They resolve in the wiki, not in this directory.
- **Links to repository files** are absolute, to
  `https://github.com/MRISS-Projects/dsh/blob/DEVELOP/...`. A wiki page cannot resolve a path in
  the repository.
- **Names.** Home and Architecture use generic terms: queue, NoSQL store, file store. The products
  behind them are changing, under ADR-001, and an overview that names them goes stale. Developer
  pages keep the product names.
- **Planned parts of a diagram** carry "(planned)" in their label, a dashed outline, and dashed
  edges into them. A planned edge between two built parts is dashed, and carries "(planned)" in
  its label.

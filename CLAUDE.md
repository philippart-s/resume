# Working in this repository

A static resume site (Quarkus + Roq), rendered in French at `/` and English at
`/en/`, printable to a two page A4 PDF.

- **Editing the resume content**: everything is in `data/resumes/<version>.yml`.
  See [README.md](README.md).
- **Changing how it works**: read [ARCHITECTURE.md](ARCHITECTURE.md) first. It
  documents the architecture decisions and the traps already paid for.

## Conventions

- **Language**: conversation with the owner happens in French; **everything
  committed is in English** — comments, identifiers, commit messages,
  documentation. The only French content is the resume itself, in
  `data/resumes/fr.yml`.
- **Commits are never automatic.** For each one: show the changed files and wait
  for approval, then propose the message and wait for a second approval. Keep them
  atomic, one intent per commit.
- **Commit messages**: conventional commit with an emoji right after the type —
  `feat: 👤 migrate profile data`. Body explains *why*, not what.
- **Authorship**: commits belong to the repository owner. Never add a
  `Co-Authored-By` trailer.

## Where to change what

| Goal | Place |
|---|---|
| resume wording, figures, sections, accounts | `data/resumes/*.yml` |
| what a page renders (version, language, listing) | frontmatter of `content/*.html` |
| page structure | `templates/layouts/resume.html` and `templates/partials/` |
| colors, tokens, spacing | `web/style.css` and its `_*.css` fragments |
| paper output | `web/_print.css` |
| data shape | `src/main/java/dev/philippart/resume/Resumes.java` |

**Never put resume content in a template.** Section titles, labels and figures all
come from the data files, which is what makes a new language or a job targeted
version a data-only change.

## Rules that prevent known breakage

1. Read frontmatter through `page.data.getString('key')` / `getJsonArray` /
   `getBoolean`. A **plain property access on a missing key throws at render time**
   and returns a 500.
2. Use `.or('value')`, not `?:`, inside a `{#let}`.
3. Any helper method added to the records in `Resumes.java` must be `@JsonIgnore`d.
4. A new `.css` file in `web/` must start with `_` unless it is meant to be a
   bundler entry point.
5. Templates in this project use `{=expr}` for output expressions.
6. Do not reintroduce `quarkus-roq-theme-resume`.
7. Keep `fr.yml` and `en.yml` structurally identical: same sections, same figures,
   same flags. Only the wording differs.

## Verifying a change

```bash
./mvnw -B package -DskipTests    # templates and data binding are validated here
java -jar target/quarkus-app/quarkus-run.jar &
curl -s localhost:8080/ ; curl -s localhost:8080/en/
```

- **Check Maven's real exit code**: `./mvnw … | tail` returns `tail`'s status, so a
  broken build looks green while the old jar serves stale pages.
- Give the server a few seconds before the first request, otherwise a retry loop
  burns through its attempts during JVM startup and you end up reading the previous
  response.
- When refactoring templates, diff the rendered HTML before and after instead of
  trusting a visual check.
- Print output cannot be checked from the shell: verify the `@media print` rules
  reached `/static/bundle/app-*.css`, then ask the owner to look at the print
  preview.

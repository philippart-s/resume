# Résumé / CV

A resume built with [Roq](https://iamroq.dev), readable on screen and printable to
a two page A4 PDF. One page per version — French, English, or a version targeting
a given job — each fed by its own content file.

**You edit content in `data/resumes/` and page settings in the frontmatter of
`content/*.html`. Nothing else needs to be touched to keep the resume up to date.**

## Run it

```bash
roq start          # dev mode with live reload on http://localhost:8080
```

Install the CLI once with
`curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force roq@quarkiverse/quarkus-roq`,
or use `./mvnw quarkus:dev` instead.

```bash
roq generate       # build the static site into target/roq/
roq serve          # preview the generated site
```

**To export the PDF**: open the page in a browser and print (`Cmd+P` / `Ctrl+P`),
then "Save as PDF". A dedicated print stylesheet reflows it for paper, see
[Printing](#printing).

## Where the content lives

| File | Holds |
|---|---|
| `data/resumes/fr.yml` | the whole French resume: identity, figures, sections, accounts |
| `data/resumes/<id>.yml` | any other version, one file each |
| `public/images/` | the profile picture, and any other image |
| `content/index.html` | the French page: its title, and which version it renders |

The `data/resumes/` directory must hold at least one file. A **misspelled key
fails the build** with the field name, the known keys and the path in the YAML, so
a typo never silently drops content.

## A resume file

Five blocks, all optional:

```yaml
profile:
  firstName: Stéphane
  lastName: Philippart
  picture: stephane_philippart.jpg   # file name only, taken from public/images/
  jobTitle: Senior Developer Advocate - IA / Cloud Native
  city: Tours
  country: France
  email: s.philippart@gmail.com
  site: https://philippart-s.github.io/blog/
  bio: |
    Markdown. Rendered as the lead paragraph above the experience.

availability: Remote / déplacements France & Europe   # optional contact line

highlights:                          # the figures band under the header
  - value: "77"                      # the number, large and colored
    label: "talks en conférence"     # what it counts
    detail: "en 4 ans"               # optional, screen only

sections:
  - title: Expériences professionnelles
    main: true                       # wide column; without it, the side rail
    items:
      - header: "OVHcloud, 2022 → aujourd'hui"   # small line above the title
        title: "Senior Developer Advocate"       # bold line, optional
        link: https://example.com                # optional, wraps the title
        content: |                               # optional, markdown
          - **bold**, lists, [links](https://example.com).

social:
  - name: LinkedIn                   # selects the icon, see below
    type: philippartstephane         # displayed label, usually the handle
    url: https://www.linkedin.com/in/philippartstephane
    print: true                      # kept on paper, see Printing
```

- **section and item order on the page follows the file order.**
- `city` and `country` are shown together; if either is missing, the line is
  hidden. `email` becomes a `mailto:` link.
- if the `picture` file is not found, the photo is simply not displayed.
- four or five `highlights` is the practical maximum, beyond that the band stops
  being scannable. `detail` is dropped when printing, to keep the band on one line.
- an item needs no `title`: the bold line is then skipped, which suits a
  text-only item.
- `logo` is supported on an item: `logo: {label: …, imageUrl: …, link: …}`, the
  image resolved from `public/images/`.
- in `social`, `name` selects the icon. Icons exist for **LinkedIn, GitHub,
  GitLab, X, Stack Overflow**; any other name falls back to a generic link icon.
  Adding one means adding a `{#case}` to `templates/partials/resume-icon.html`.

## Adding a version

Another language, or a resume aimed at a given job:

1. copy `data/resumes/fr.yml` to `data/resumes/<id>.yml` and edit its content;
2. add a page in `content/` pointing at it:

```
---
title: Stéphane Philippart's resume
description: …
layout: resume
lang: en
resume: <id>
---
```

The page URL comes from the file name, so `content/en.html` is served at `/en/`.
No template and no Java to change.

## Page settings

| Key | Effect |
|---|---|
| `title` | browser tab, `<title>`, link previews |
| `description` | meta description and link previews |
| `layout` | must stay `resume` |
| `lang` | `<html lang>` and `og:locale`; defaults to `fr` |
| `resume` | which file of `data/resumes/` to render, without its extension; defaults to `fr` |

Asking for a version that does not exist fails with the list of the available
ones, rather than rendering an empty page.

## Showing and hiding things

| To hide | Do this |
|---|---|
| a whole section | remove it from the `sections` list |
| the key figures band | remove the `highlights` block |
| the availability line | remove the `availability` key |
| the profile picture | remove `picture`, or the file itself |
| a social account | remove it from the `social` list |
| social accounts on paper only | flag the ones to keep with `print: true` |
| the location line | remove `city` or `country` |

## Printing

`web/_print.css` holds everything specific to paper. The layout keeps its two
columns, the rail floating beside the experience.

Two knobs decide whether the resume fits on two pages, both at the top of the
file:

| Setting | Current | Effect |
|---|---|---|
| `--spacing` | `0.22rem` | every margin, padding and gap at once (the screen scale is `0.25rem`) |
| `font-size` on `html, body` | `9.5pt` | text density; below `9pt` paper reading suffers |

Dropped when printing: the dark mode toggle, the figure details, the footer, the
timeline decoration, and the social accounts not flagged `print: true` — when no
account is flagged, all of them are printed. The profile picture is kept, at a
fixed 22 mm. Link URLs are printed in parentheses after the link text, since a
printed link cannot be clicked.

## Colors

`web/style.css` is the single stylesheet entry point and selects the palette:

```css
@import "./_theme-blue.css";   /* blue, emerald, amber, rose, cyan — or drop it for purple */
```

Files prefixed with `_` are fragments, not entry points: without that prefix the
bundler emits them twice, once alone and once inlined.

## Project layout

```
content/            one page per resume version
data/resumes/       one content file per version
public/images/      pictures
templates/
  layouts/          base (html skeleton), resume (the page)
  partials/         header, sections, side rail, icons, footer, dark mode toggle
web/                style.css (entry), _print.css, _resume-theme.css, palettes, resume.js
src/main/java/      Resumes, the record binding data/resumes/
```

The design tokens in `web/_resume-theme.css`, `resume.js` and three partials come
from `quarkus-roq-theme-resume` (Apache-2.0), kept in the project when the theme
dependency was dropped in favour of owning the data model.

## Learn more

- [Roq documentation](https://iamroq.dev/docs/) — content, layouts, data, plugins
- [Qute reference](https://quarkus.io/guides/qute-reference) — the template syntax
  used in `templates/`. Note that expressions are written `{=expr}`, the
  alternative syntax enabled by `quarkus.qute.alt-expr-syntax`

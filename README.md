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
| `data/resumes/en.yml` | the English one, same structure |
| `data/resumes/<id>.yml` | any other version, one file each |
| `public/images/` | the profile picture, and any other image |
| `content/index.html` | the French page, served at `/` |
| `content/en.html` | the English page, served at `/en/` |

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
nav: true
---
```

The page URL comes from the file name, so `content/en.html` is served at `/en/`.
No template and no Java to change.

Pages flagged `nav: true` are listed in the **language switcher** shown top right,
and declared to search engines as `hreflang` alternates of each other. Leave the
flag out for a version aimed at a single application: it stays reachable by URL
but is neither advertised nor indexed as an alternate.

## Targeting a company

A version aimed at one company is a **delta, not a copy**: it names the version it
builds on and carries only what it changes, so a fix to the base reaches every
version at once.

**1. The content** — `data/resumes/<target>.yml`:

```yaml
base: fr                                   # the version to build upon
profile:
  jobTitle: Senior Developer Advocate - the angle that matters to this company
  bio: |
    Lead paragraph rewritten for this target.
sections:
  - title: Expériences professionnelles     # section matched by its title
    items:
      - header: "OVHcloud, 2022 → aujourd'hui"   # item matched by its header
        content: |
          Bullets reordered, the ones that matter to this company first.
  - title: Centres d'intérêt
    remove: true                            # drop a block to make room
```

**2. The page** — `content/<target>.html`:

```
---
title: CV de Stéphane Philippart
layout: resume
lang: fr
resume: <target>
draft: true
---
```

`draft: true` keeps it **out of the published site**: it is not generated, not
linked, not indexed — sending a link to one company never exposes the version
written for another. Omit `nav: true` as well, so it stays out of the language
switcher.

**3. Preview and print it**:

```bash
./mvnw quarkus:dev -Dsite.draft=true      # then open /<target>/ and print to PDF
```

### What merges, and how

| Block | Rule |
|---|---|
| `profile` | field by field: a field given here wins, the others are inherited |
| `availability` | replaced when present |
| `highlights` | merged by `label` |
| `sections` | merged by `title` |
| items inside a section | merged by `header` |
| `social` | merged by `name` |

**Order** comes from the base. To change it, name the keys in an `order` list —
section titles at the top level, item headers inside a section:

```yaml
sections:
  - title: Skills
    order: ["Artificial Intelligence", "Cloud & platform", "Java", "Advocacy"]
```

Keys named there come first, in that order; the others follow in their inherited
order. This is deliberately not "restate the list to reorder it": restating a list
copies its content too, which then drifts from the base. Naming a key that does not
exist fails the page, like `remove`.

Anything absent is inherited. A key unknown to the base is **appended** after the
inherited entries — that is how you add a section or an item. `remove: true` drops
an entry, and a `remove` on a key that does not exist **fails that page with the
list of valid keys**, so a mistyped title never passes unnoticed.

An English target names `base: en`. A version may itself build on another version;
a cycle is reported rather than looping.

## Page settings

| Key | Effect |
|---|---|
| `title` | browser tab, `<title>`, link previews |
| `description` | meta description and link previews |
| `layout` | must stay `resume` |
| `lang` | `<html lang>` and `og:locale`; defaults to `fr` |
| `resume` | which file of `data/resumes/` to render, without its extension; defaults to `fr` |
| `nav` | `true` puts the page in the language switcher, top right. A version you would rather not advertise simply omits it |

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

Dropped when printing: the dark mode toggle, the language switcher, the figure
details, the footer, the timeline decoration, and the social accounts not flagged
`print: true` — when no account is flagged, all of them are printed. The profile
picture is kept, at a fixed 22 mm. Link URLs are printed in parentheses after the
link text, since a printed link cannot be clicked.

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

## Link preview and favicon

Two images in `public/images/` are not resume content, but they are what people see
before they read anything:

| File | Shown |
|---|---|
| `og-card.png` (1200×630) | the preview card when the link is shared on LinkedIn, Slack, Teams… declared by `image: og-card.png` in each page |
| `favicon.svg`, `favicon.png`, `apple-touch-icon.png` | the browser tab and mobile home screen icons |

Both are generated from an SVG source kept next to them (`og-card.svg`,
`favicon.svg`). **Edit the SVG, then regenerate** — for instance when the job title
changes:

```bash
cd public/images
qlmanage -t -s 1200 -o . og-card.svg && sips -c 630 1200 og-card.svg.png --out og-card.png && rm og-card.svg.png
qlmanage -t -s 512  -o . favicon.svg && sips -z 180 180 favicon.svg.png --out apple-touch-icon.png \
  && sips -z 32 32 favicon.svg.png --out favicon.png && rm favicon.svg.png
```

Those two commands are macOS only. The card canvas is square on purpose: the
rasterizer fits into a square, and `sips` crops the middle 630 rows.

Without `image:` on a page there is **no `og:image` at all**, and sharing platforms
fall back to the favicon — which is how the Roq logo used to show up in previews.

## Maintaining the project

[ARCHITECTURE.md](ARCHITECTURE.md) covers the architecture decisions, the rendering
pipeline, the print strategy and the traps met along the way. [CLAUDE.md](CLAUDE.md)
holds the working conventions, for AI coding assistants and humans alike.

## Learn more

- [Roq documentation](https://iamroq.dev/docs/) — content, layouts, data, plugins
- [Qute reference](https://quarkus.io/guides/qute-reference) — the template syntax
  used in `templates/`. Note that expressions are written `{=expr}`, the
  alternative syntax enabled by `quarkus.qute.alt-expr-syntax`

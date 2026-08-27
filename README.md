# Résumé / CV

A single page resume built with [Roq](https://iamroq.dev) and the
`quarkus-roq-theme-resume` theme, readable on screen and printable to a two page
A4 PDF.

**You edit content in `data/` and settings in the frontmatter of
`content/index.html`. Nothing else needs to be touched to keep the resume up to
date.**

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

**To export the PDF**: open the site in a browser and print (`Cmd+P` / `Ctrl+P`),
then "Save as PDF". A dedicated print stylesheet reflows the page for paper, see
[Printing](#printing).

## Where the content lives

| File | Holds |
|---|---|
| `data/profile.yml` | identity, job title, contact details, lead paragraph |
| `data/bio.yml` | every section of the resume: experience, skills, education… |
| `data/highlights.yml` | the key figures shown in the band under the header |
| `data/social.yml` | social accounts and handles |
| `public/images/` | the profile picture (and any other image) |
| `content/index.html` | page title and the settings listed below |

All four data files must exist: their beans are generated at build time, so
deleting one breaks the build. To hide a block, empty its list instead
(`[]` for `highlights.yml`), or remove the section from `bio.yml`.

### `profile.yml`

```yaml
firstName: Stéphane
lastName: Philippart
picture: stephane_philippart.jpg    # file name only, taken from public/images/
jobTitle: Senior Developer Advocate — Java & IA / Cloud Native
city: Tours
country: France
email: s.philippart@gmail.com
site: https://philippart-s.github.io/blog/
bio: |
  Markdown. Rendered as the lead paragraph above the experience.
```

- `city` and `country` are shown together; if either is missing, the line is
  hidden.
- `email` becomes a `mailto:` link, `site` an external link.
- if the `picture` file is not found, the photo is simply not displayed.
- `phone` is accepted by the theme but **not displayed** by this resume; put it
  in a variant if you need it.

### `bio.yml`

A list of sections, each with a list of items:

```yaml
- title: Expériences professionnelles     # section heading
  items:
    - header: "OVHcloud, 2022 → aujourd'hui"   # small line above the title
      title: "Senior Developer Advocate"       # bold line, optional
      link: https://example.com                # optional, wraps the title
      content: |                               # optional, markdown
        - Anything markdown: **bold**, lists, [links](https://example.com).
```

- **section order on the page follows the file order.**
- `header` and `title` are both **mandatory keys** — the theme's data model
  rejects an item missing either, and the build fails. For a text-only item, keep
  `title: ""`: the rendering then skips the bold line instead of leaving an empty
  one.
- `content` is markdown, which is where bullet lists and links belong.
- `logo` is supported: `logo: {label: …, imageUrl: …, link: …}`, the image being
  resolved from `public/images/`.
- the theme's `collapsible`, `collapsed`, `ruler` and `subItems` fields are
  **ignored** by this resume's rendering.

### `highlights.yml`

The figures a reader should catch first, in a band under the header:

```yaml
- value: "77"                    # the number, large and colored
  label: "talks en conférence"   # what it counts
  detail: "en 4 ans, circuit européen"   # optional, screen only
```

Four or five entries is the practical maximum; beyond that the band stops being
scannable. `detail` is dropped when printing, to keep the band on one line.

### `social.yml`

```yaml
- name: LinkedIn                 # selects the icon, see below
  type: philippartstephane       # the displayed label, usually the handle
  url: https://www.linkedin.com/in/philippartstephane
```

`name` drives which icon is shown. Icons exist for **LinkedIn, GitHub, GitLab,
X, Stack Overflow**; any other name falls back to a generic link icon. Adding an
icon means adding a `{#case}` to `templates/partials/resume-icon.html`.

If `type` is empty, `name` is displayed instead.

## Page settings

All of these live in the frontmatter of `content/index.html`:

```yaml
---
title: CV de Stéphane Philippart
description: CV de Stéphane Philippart, Senior Developer Advocate — …
layout: resume-hybrid
lang: fr
availability: Remote / déplacements France & Europe
printSocial:
  - LinkedIn
mainSections:
  - Expériences professionnelles
---
```

| Key | Effect |
|---|---|
| `title` | browser tab, `<title>`, link previews |
| `description` | meta description and link previews |
| `layout` | must stay `resume-hybrid` |
| `lang` | `<html lang>` and `og:locale`; defaults to `fr` |
| `availability` | optional extra contact line (remote, mobility…). Omit the key to hide it |
| `printSocial` | optional: the only networks kept **when printing**. Omit the key to print them all |
| `mainSections` | optional: section titles rendered in the wide left column. Every other section goes to the right rail. Omit the key and everything lands in the left column |

`mainSections` and `printSocial` match section and network names **exactly**, so
renaming a section in `bio.yml` means renaming it here too.

## Showing and hiding things

| To hide | Do this |
|---|---|
| a whole section | remove it from `data/bio.yml` |
| the key figures band | empty `data/highlights.yml` (`[]`) |
| the availability line | remove the `availability` key |
| the profile picture | remove `picture` from `profile.yml`, or the file itself |
| a social account | remove it from `data/social.yml` |
| social accounts on paper only | list the ones to keep in `printSocial` |
| the location line | remove `city` or `country` |

## Printing

`web/print.css` holds everything specific to paper. The layout keeps its two
columns, the rail floating beside the experience.

Two knobs decide whether the resume fits on two pages, both at the top of the
file:

| Setting | Current | Effect |
|---|---|---|
| `--spacing` | `0.22rem` | every margin, padding and gap at once (the screen scale is `0.25rem`) |
| `font-size` on `html, body` | `9.5pt` | text density; below `9pt` paper reading suffers |

Dropped when printing: the dark mode toggle, the profile picture, the figure
details, the "Crafted with Roq" footer, the timeline decoration, and the social
accounts not listed in `printSocial`. Link URLs are printed in parentheses after
the link text, since a printed link cannot be clicked.

## Colors

`web/style.css` selects the color theme:

```css
@import "./_theme-blue.css";   /* blue, emerald, amber, rose, cyan — or none for the default purple */
```

## Learn more

- [Roq documentation](https://iamroq.dev/docs/) — content, layouts, data, plugins
- [Qute reference](https://quarkus.io/guides/qute-reference) — the template syntax
  used in `templates/`. Note that expressions here are written `{=expr}`, the
  alternative syntax enabled by `quarkus.qute.alt-expr-syntax`

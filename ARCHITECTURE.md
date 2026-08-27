# Architecture and maintenance notes

This documents **how the project is built and why**, for whoever maintains it —
human or AI assistant. For **how to edit the resume content**, see the
[README](README.md); nothing here is needed for that.

Stack: Quarkus 3.39.1 · Java 25 · Roq 2.1.7 (static site generator) · Quarkus Web
Bundler 2.3.3 with Tailwind CSS 4 · Alpine.js 3 · Maven.

## What this project is

A single page resume, rendered once per language (`/` French, `/en/` English), that
must read well in three contexts:

1. **on screen**, light and dark;
2. **on paper**, exported through the browser's print dialog, two A4 pages;
3. **to a recruiter scanning for six seconds** — which is why key figures sit in a
   band right under the identity, and why nothing is hidden behind interaction.

There is no server: `roq generate` produces a static site, deployed to GitHub Pages
by `.github/workflows/deploy.yml` on every push to `main`. That workflow pins
`java-version: '25'`: the `quarkiverse/quarkus-roq` action installs **Java 21 by
default**, which fails on `maven.compiler.release=25`. Raising the release in
`pom.xml` means raising it there too.

## The one decision everything follows from: no theme

The project started on `quarkus-roq-theme-resume`. **The dependency was dropped**
because the theme's data model was bound to fixed file names — `data/profile.yml`
and `data/bio.yml`, both `required = true`, enforced at build time even when no
template uses them. That made a symmetric structure impossible: French would have
lived in the theme's files and every other language or job targeted version in
parallel files of a different shape.

What the project now owns, and where it came from:

| Owned file | Origin |
|---|---|
| `web/_resume-theme.css` | the theme's design tokens, copied as is (Apache-2.0) |
| `web/_theme-blue.css` | one of the theme's accent palettes |
| `web/resume.js` | the theme's Alpine.js and Inter font bootstrap |
| `templates/partials/resume-head.html`, `resume-footer.html`, `resume-toggle-darkmode.html` | the theme's partials |
| `templates/layouts/base.html`, `resume.html` | replacements for the theme's `default` and `resume` layouts |
| `src/main/java/dev/philippart/resume/Resumes.java` | replacement for the theme's `Profile` and `Bio` records |

Consequences to keep in mind:

- the theme's Maven dependency also carried **`quarkus-web-bundler-tailwindcss`**
  and the **`alpinejs` / `@fontsource/inter`** npm packages. They are now declared
  in `pom.xml`; without them the CSS does not compile and the dark mode toggle
  dies silently;
- theme updates no longer arrive. The tokens in `web/_resume-theme.css` are ours to
  maintain;
- **do not reintroduce the theme.** It would bring back the required data files and
  the layout shadowing described below.

## Data model

`Resumes` binds `data/resumes/` through `@DataMapping(type = OBJECT_DIR)`: one YAML
file per version, keyed by file name. A page picks one with its `resume`
frontmatter key. **Adding a version is one YAML file plus one page — no Java, no
template.**

Two flags live in the data rather than in the page:

- `main: true` on a section puts it in the wide column, everything else goes to the
  side rail;
- `print: true` on a social account keeps it on paper.

Both used to be lists of names in the page frontmatter (`mainSections`,
`printSocial`). They had to match section and account names **exactly**, so
renaming a section silently moved it to the rail. Flags cannot drift.

Every record component is nullable and the templates guard on them, so a resume
file can omit any block. A **misspelled key fails the build** with the field name,
the known keys and the YAML path — content is never silently dropped.

## Rendering pipeline

```
content/index.html            frontmatter only: title, description, lang, resume, nav
  └─ layouts/resume.html      resolves the variant, lays out the page
       ├─ layouts/base.html   <html lang>, <head>, <body>
       ├─ partials/resume-head.html        meta, SEO, hreflang, bundle
       ├─ partials/resume-langs.html       FR / EN switcher
       ├─ partials/resume-header.html      identity, contact, social, figures band
       ├─ partials/resume-sections.html    wide column, timeline rendering
       ├─ partials/resume-aside.html       side rail, compact rendering
       ├─ partials/resume-icon.html        inline brand and contact SVGs
       └─ partials/resume-footer.html
```

`layouts/resume.html` is the only place that touches CDI beans: it resolves
`cdi:resumes.get(...)` once and passes `content` down. Partials take parameters, so
they can render any variant.

**The rail comes before the main column in the DOM**, its visual position restored
by `order` on mobile and `col-start` / `row-start` on wide screens. This is not
cosmetic: print floats the rail, and a float must precede the content that flows
around it.

Icons are inline SVG from Simple Icons (CC0) and Bootstrap Icons (MIT), switched on
a name in `resume-icon.html`. No icon webfont: an external font would add a request
and print unpredictably. An unknown name falls back to a generic link icon.

## Print strategy

`web/_print.css` is where the paper version lives. Four decisions there are the
result of failures worth not repeating:

1. **Two columns through a float, not the grid.** A single row CSS grid is pushed
   whole when it does not fit in the remaining space, which left an almost blank
   first page. A float fragments properly.
2. **`--spacing` is the density lever.** Every Tailwind spacing utility derives
   from it, so one value compacts margins, paddings and gaps together. With
   `font-size` on `html, body`, these are the only two knobs needed to fit two pages.
3. **Tailwind `md:` and wider breakpoints never match on paper.** A usable A4 page
   is about 45rem, below the 48rem `md` threshold, so any layout that depends on a
   breakpoint must be restated inside `@media print`.
4. **Backgrounds are not printed by default, borders are.** The figures band gets
   its separators from a `gap-px` over a colored background on screen; on paper the
   same separators are real borders. Same reason `display: flow-root` is set on
   section title blocks: a block border would otherwise run under the floating rail.

## CSS and JS assembly

`web/style.css` is the **single bundler entry point**. Any `.css` file in `web/`
without a leading underscore is treated as an entry point too, and would be emitted
twice — once alone, once inlined. That doubled the bundle to 133 kB before the
fragments were renamed `_print.css` and `_resume-theme.css`; it is 69 kB now.

npm packages are declared as **mvnpm Maven dependencies** (`org.mvnpm:alpinejs`,
`org.mvnpm.at.fontsource:inter`), which the web bundler installs into
`node_modules` at build time. They are not resolved from the imports in
`web/resume.js` alone.

Alpine.js exists for one feature: the dark mode toggle. `x-data` on the page root
holds the state.

## Traps checked the hard way

| Symptom | Cause and fix |
|---|---|
| HTTP 500, `Property "x" not found on JsonObject` | a missing frontmatter key **throws** instead of returning null. Use `page.data.getString('x')`, `getJsonArray`, `getBoolean` — never `page.data.x` |
| Build error `empty expression found {?:}` | `?:` is refused inside `{#let}`, where parameters are split into separate expressions. Use `.or('value')` |
| `Unrecognized field` on a data record | Roq serializes records back to JSON between reading an `OBJECT_DIR` mapping and binding it, so accessor shaped helpers travel into that JSON. Annotate them `@JsonIgnore` |
| Expressions rendered literally | `quarkus.qute.alt-expr-syntax=true`: templates in this project write `{=expr}`. Only templates coming from a jar use `{expr}` |
| A local layout ignored | a layout named like a theme layout is shadowed by the theme's. No longer applies here, since there is no theme — but it is why `base` and `resume` were once called `base-fr` and `resume-hybrid` |
| English page titled in French | Roq's `{#seo}` tag composes `page title - site title`, and the site title is the French home page's. `resume-head.html` calls the subtags itself to avoid it |

## Verifying a change

There are **no automated tests** — a deliberate choice for a two page static site.
Verification is a build plus a look at the rendered HTML:

```bash
./mvnw -B package -DskipTests            # data binding and templates are validated at build time
java -jar target/quarkus-app/quarkus-run.jar &
curl -s localhost:8080/ | less           # French page
curl -s localhost:8080/en/ | less        # English page
```

Two habits worth keeping:

- **check the real Maven exit code.** `./mvnw … | tail` reports the exit code of
  `tail`, so a failed build looks green and the previous jar keeps serving stale
  content;
- **when refactoring templates, diff the rendered HTML** before and after
  (normalising whitespace). That is how the theme removal was proven to change
  nothing visible.

Print output cannot be verified from the command line: check `@media print` rules
made it into `/static/bundle/app-*.css`, then look at the browser's print preview.

## Deliberately absent

- **no tests**, see above;
- **`phone`** is not displayed, by choice — the data model has no field for it;
- **item `collapsible`, `collapsed`, `ruler` and `subItems`**, supported by the old
  theme, are not rendered. Reintroducing sub-items means recursion in
  `resume-sections.html`;
- **no analytics**. `resume-head.html` still carries the theme's GA4 hook, active
  only if an `analytics` key appears in the site data.

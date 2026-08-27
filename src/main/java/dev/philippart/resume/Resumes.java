package dev.philippart.resume;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;
import io.vertx.core.json.JsonObject;

/// Every resume variant, one YAML file per variant in `data/resumes/`.
///
/// A variant is either a **base** — a complete resume, one per language — or an
/// **overlay**: it names a `base` and carries only what it changes, which is how a
/// version targeting a company stays a handful of lines instead of a full copy.
/// Selection happens through the `resume` key of a page's frontmatter.
///
/// Roq serializes these records back to JSON between reading the directory and
/// binding it, so every helper below is `@JsonIgnore`d: an accessor shaped method
/// would otherwise be written to that intermediate JSON and rejected on the way
/// back.
@DataMapping(value = "resumes", type = DataMapping.Type.OBJECT_DIR, required = true)
public record Resumes(Map<String, Content> map) {

    /// The version a page asks for, with its base already merged in.
    public Content get(String id) {
        return resolve(id, new LinkedHashSet<>());
    }

    /// The same, with an overlay carried by a page's own frontmatter rather than by
    /// a file of `data/resumes/`: this is how a version targeting a company stays a
    /// single file, kept in a private repository.
    public Content get(String id, JsonObject overlay) {
        var base = get(id);
        return overlay == null ? base : merge(base, overlay.mapTo(Content.class));
    }

    private Content resolve(String id, Set<String> visited) {
        if (!visited.add(id)) {
            throw new IllegalArgumentException("Resume variants form a cycle: %s".formatted(visited));
        }
        var content = map.get(id);
        if (content == null) {
            throw new IllegalArgumentException(
                    "No resume variant [%s] in data/resumes/, available: %s".formatted(id, map.keySet()));
        }
        return content.base() == null ? content : merge(resolve(content.base(), visited), content);
    }

    /// One resume: who I am, the figures, the sections and the social accounts.
    ///
    /// In an overlay, [#base()] names the variant to build upon and every other
    /// block is optional: what is absent is inherited.
    public record Content(
            String base,
            Profile profile,
            String availability,
            List<Highlight> highlights,
            List<String> order,
            List<Section> sections,
            List<Social> social) {

        /// Sections flagged `main: true`, rendered in the wide column.
        @JsonIgnore
        public List<Section> mainSections() {
            return sections == null ? List.of() : sections.stream().filter(Section::isMain).toList();
        }

        /// Every other section, rendered in the side rail.
        @JsonIgnore
        public List<Section> sideSections() {
            return sections == null ? List.of() : sections.stream().filter(s -> !s.isMain()).toList();
        }

        /// Whether some accounts are flagged `print: true`, in which case the others
        /// are dropped from the printed resume.
        @JsonIgnore
        public boolean hasPrintSelection() {
            return social != null && social.stream().anyMatch(Social::isPrinted);
        }
    }

    public record Profile(
            String firstName,
            String lastName,
            String picture,
            String jobTitle,
            String city,
            String country,
            String email,
            String site,
            String bio) {
    }

    /// A key figure of the header band, merged by [#label()].
    public record Highlight(String value, String label, String detail, Boolean remove) {
    }

    /// A section, merged by [#title()]. [#order()] lists item headers to place first.
    public record Section(String title, Boolean main, Boolean remove, List<String> order, List<Item> items) {

        @JsonIgnore
        public boolean isMain() {
            return Boolean.TRUE.equals(main);
        }
    }

    /// An item, merged by [#header()]. [#content()] is markdown.
    public record Item(String header, String title, String link, String content, Logo logo, Boolean remove) {
    }

    public record Logo(String label, String imageUrl, String link) {
    }

    /// A social account, merged by [#name()]. [#type()] is the displayed handle.
    public record Social(String name, String type, String url, Boolean print, Boolean remove) {

        @JsonIgnore
        public boolean isPrinted() {
            return Boolean.TRUE.equals(print);
        }
    }

    // --- merging -----------------------------------------------------------
    //
    // One rule everywhere: a value given by the overlay wins, an absent one is
    // inherited, and lists merge by key — title for sections, header for items,
    // name for accounts, label for figures. An entry flagged `remove: true`
    // disappears, and fails the page render if its key is unknown; any other
    // unknown key is appended after the inherited ones. Merging happens when a
    // page is rendered, so a broken overlay breaks that page only.
    //
    // Order comes from the base unless the overlay names keys in `order`, which
    // avoids restating a whole list — and its content — just to move one entry.

    private static Content merge(Content base, Content overlay) {
        var sections = mergeList("section", base.sections(), overlay.sections(), Section::title,
                Resumes::mergeSection, s -> Boolean.TRUE.equals(s.remove()));
        return new Content(
                null,
                mergeProfile(base.profile(), overlay.profile()),
                pick(overlay.availability(), base.availability()),
                mergeList("figure", base.highlights(), overlay.highlights(), Highlight::label,
                        Resumes::mergeHighlight, h -> Boolean.TRUE.equals(h.remove())),
                null,
                reorder("section", sections, overlay.order(), Section::title),
                mergeList("account", base.social(), overlay.social(), Social::name,
                        Resumes::mergeSocial, s -> Boolean.TRUE.equals(s.remove())));
    }

    private static Profile mergeProfile(Profile base, Profile overlay) {
        if (overlay == null || base == null) {
            return overlay == null ? base : overlay;
        }
        return new Profile(
                pick(overlay.firstName(), base.firstName()),
                pick(overlay.lastName(), base.lastName()),
                pick(overlay.picture(), base.picture()),
                pick(overlay.jobTitle(), base.jobTitle()),
                pick(overlay.city(), base.city()),
                pick(overlay.country(), base.country()),
                pick(overlay.email(), base.email()),
                pick(overlay.site(), base.site()),
                pick(overlay.bio(), base.bio()));
    }

    private static Highlight mergeHighlight(Highlight base, Highlight overlay) {
        return new Highlight(
                pick(overlay.value(), base.value()),
                pick(overlay.label(), base.label()),
                pick(overlay.detail(), base.detail()),
                null);
    }

    private static Section mergeSection(Section base, Section overlay) {
        var items = mergeList("item", base.items(), overlay.items(), Item::header,
                Resumes::mergeItem, i -> Boolean.TRUE.equals(i.remove()));
        return new Section(
                pick(overlay.title(), base.title()),
                pick(overlay.main(), base.main()),
                null,
                null,
                reorder("item", items, overlay.order(), Item::header));
    }

    private static Item mergeItem(Item base, Item overlay) {
        return new Item(
                pick(overlay.header(), base.header()),
                pick(overlay.title(), base.title()),
                pick(overlay.link(), base.link()),
                pick(overlay.content(), base.content()),
                pick(overlay.logo(), base.logo()),
                null);
    }

    private static Social mergeSocial(Social base, Social overlay) {
        return new Social(
                pick(overlay.name(), base.name()),
                pick(overlay.type(), base.type()),
                pick(overlay.url(), base.url()),
                pick(overlay.print(), base.print()),
                null);
    }

    private static <T> List<T> mergeList(String what, List<T> base, List<T> overlay,
            Function<T, String> key, BinaryOperator<T> merger, Predicate<T> removed) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay.stream().filter(Predicate.not(removed)).toList();
        }
        var merged = new LinkedHashMap<String, T>();
        base.forEach(entry -> merged.put(String.valueOf(key.apply(entry)), entry));
        for (T entry : overlay) {
            var entryKey = String.valueOf(key.apply(entry));
            if (removed.test(entry)) {
                if (merged.remove(entryKey) == null) {
                    // Silently ignoring this would hide a typo in the key, and the
                    // entry meant to disappear would quietly stay in the resume.
                    // Raised while rendering, so only the faulty variant breaks.
                    throw new IllegalArgumentException(
                            "Cannot remove %s [%s]: no such key in the base, available: %s"
                                    .formatted(what, entryKey, merged.keySet()));
                }
            } else {
                merged.merge(entryKey, entry, merger);
            }
        }
        return List.copyOf(merged.values());
    }

    /// Places the keys named by the overlay first, in that order; everything else
    /// keeps its inherited order behind them. Naming a key that does not exist is
    /// an error: silently ignoring it would leave the resume in the base order.
    private static <T> List<T> reorder(String what, List<T> entries, List<String> order,
            Function<T, String> key) {
        if (order == null || entries == null) {
            return entries;
        }
        var remaining = new LinkedHashMap<String, T>();
        entries.forEach(entry -> remaining.put(String.valueOf(key.apply(entry)), entry));
        var ordered = new java.util.ArrayList<T>(entries.size());
        for (String wanted : order) {
            var entry = remaining.remove(wanted);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Cannot order %s [%s]: no such key, available: %s"
                                .formatted(what, wanted, remaining.keySet()));
            }
            ordered.add(entry);
        }
        ordered.addAll(remaining.values());
        return List.copyOf(ordered);
    }

    private static <T> T pick(T overlay, T base) {
        return overlay != null ? overlay : base;
    }
}

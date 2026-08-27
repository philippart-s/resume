package dev.philippart.resume;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/// Every resume variant, one YAML file per variant in `data/resumes/`.
///
/// Roq serializes these records back to JSON between reading the directory and
/// binding it, so every helper below is `@JsonIgnore`d: an accessor shaped method
/// would otherwise be written to that intermediate JSON and rejected on the way
/// back.
///
/// A new variant — another language, or a version targeting a given job — is a new
/// file in that directory, selected by the `resume` key of a page's frontmatter.
/// Neither this class nor the templates need to change.
@DataMapping(value = "resumes", type = DataMapping.Type.OBJECT_DIR, required = true)
public record Resumes(Map<String, Content> map) {

    /// The variant a page asks for, failing with the available ids when it is missing,
    /// which is friendlier than a template blowing up on a null.
    public Content get(String id) {
        var content = map.get(id);
        if (content == null) {
            throw new IllegalArgumentException(
                    "No resume variant [%s] in data/resumes/, available: %s".formatted(id, map.keySet()));
        }
        return content;
    }

    /// One complete resume: who I am, the figures, the sections and the social accounts.
    public record Content(
            Profile profile,
            String availability,
            List<Highlight> highlights,
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

    /// A key figure of the header band: a [#value()] to scan, a [#label()] to read,
    /// and a [#detail()] shown on screen only.
    public record Highlight(String value, String label, String detail) {
    }

    public record Section(String title, Boolean main, List<Item> items) {

        @JsonIgnore
        public boolean isMain() {
            return Boolean.TRUE.equals(main);
        }
    }

    /// [#header()] is the small line above [#title()]; [#content()] is markdown.
    public record Item(String header, String title, String link, String content, Logo logo) {
    }

    public record Logo(String label, String imageUrl, String link) {
    }

    /// [#name()] selects the icon, [#type()] is the displayed handle.
    public record Social(String name, String type, String url, Boolean print) {

        @JsonIgnore
        public boolean isPrinted() {
            return Boolean.TRUE.equals(print);
        }
    }
}

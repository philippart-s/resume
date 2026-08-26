package dev.philippart.resume;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/// Key figures displayed in the resume header band, loaded from `data/highlights.yml`.
///
/// Kept out of `bio.yml` on purpose: the theme `Bio.Section` record only accepts
/// `title` and `items`, so these figures need their own data file.
@DataMapping(value = "highlights", type = DataMapping.Type.ARRAY_FILE)
public record Highlights(List<Highlight> list) {

    /// A single figure: a [#value()] to scan, a [#label()] to read, and an optional [#detail()].
    public record Highlight(
            @JsonProperty(required = true) String value,
            @JsonProperty(required = true) String label,
            String detail) {
    }
}

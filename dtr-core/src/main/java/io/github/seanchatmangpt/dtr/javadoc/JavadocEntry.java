package io.github.seanchatmangpt.dtr.javadoc;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured representation of a Javadoc comment extracted by dtr-javadoc.
 *
 * <p>Loaded from {@code docs/meta/javadoc.json} at test startup via {@link JavadocIndex}.
 * Keyed by fully-qualified class name + "#" + method name.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JavadocEntry(
    String description,
    List<ParamDoc> params,
    @JsonProperty("returns") String returns,
    List<ThrowsDoc> throws_,
    @JsonProperty("since") String since,
    @JsonProperty("deprecated") String deprecated,
    List<String> see
) {

    /**
     * Returns the {@code @return} value as an Optional.
     */
    public Optional<String> optReturns() {
        return Optional.ofNullable(returns);
    }

    /**
     * Returns the {@code @since} value as an Optional.
     */
    public Optional<String> optSince() {
        return Optional.ofNullable(since);
    }

    /**
     * Returns the {@code @deprecated} value as an Optional.
     */
    public Optional<String> optDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    /**
     * A documented method parameter from {@code @param}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParamDoc(String name, String description) {}

    /**
     * A documented exception from {@code @throws} or {@code @exception}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ThrowsDoc(String exception, String description) {}
}

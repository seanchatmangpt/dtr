# `JavadocEntry`

> **Package:** `io.github.seanchatmangpt.dtr.javadoc`  

Structured representation of a Javadoc comment extracted by dtr-javadoc. <p>Loaded from {@code docs/meta/javadoc.json} at test startup via {@link JavadocIndex}. Keyed by fully-qualified class name + "#" + method name.</p>

```java
@JsonIgnoreProperties(ignoreUnknown = true) public record JavadocEntry( String description, List<ParamDoc> params, @JsonProperty("returns") String returns, List<ThrowsDoc> throws_, @JsonProperty("since") String since, @JsonProperty("deprecated") String deprecated, List<String> see ) {
    // optReturns, optSince, optDeprecated
}
```

---

## Methods

### `optDeprecated`

Returns the {@code @deprecated} value as an Optional.

---

### `optReturns`

Returns the {@code @return} value as an Optional.

---

### `optSince`

Returns the {@code @since} value as an Optional.

---


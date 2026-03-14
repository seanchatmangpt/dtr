# `BlogTemplate`

> **Package:** `io.github.seanchatmangpt.dtr.render.blog`  

Sealed interface for blog platform templates. Each platform (Dev.to, Medium, LinkedIn, etc.) has different front matter, metadata, and formatting requirements. Sealed implementations ensure exhaustive handling when adding new platforms.

```java
public sealed interface BlogTemplate permits DevToTemplate, MediumTemplate, SubstackTemplate, LinkedInTemplate, HashnodeTemplate {
    // frontMatter, heroImage, readingTime, canonicalUrl, formatCallToAction, formatTweetable, footnoteMarker, platformName
}
```

---

## Methods

### `canonicalUrl`

Formats the canonical URL (points back to GitHub/docs).

| Parameter | Description |
| --- | --- |
| `docTestUrl` | the base documentation URL |

> **Returns:** the canonical URL for the blog post

---

### `footnoteMarker`

Returns the platform's preferred footnote marker format.

| Parameter | Description |
| --- | --- |
| `index` | the footnote index (1-based) |

> **Returns:** the formatted footnote marker (e.g., "[^1]" for Markdown, "[1]" for others)

---

### `formatCallToAction`

Formats a call-to-action link for the article.

| Parameter | Description |
| --- | --- |
| `text` | the CTA button text |
| `url` | the target URL |

> **Returns:** the formatted CTA (as Markdown link or platform-specific format)

---

### `formatTweetable`

Formats a tweetable/social media excerpt.

| Parameter | Description |
| --- | --- |
| `text` | the text to format (≤280 chars) |

> **Returns:** the formatted text with platform-specific metadata

---

### `frontMatter`

Generates platform-specific front matter (YAML or JSON headers).

| Parameter | Description |
| --- | --- |
| `meta` | the document metadata |

> **Returns:** the front matter string

---

### `heroImage`

Formats a hero image reference (platform-specific markdown syntax).

| Parameter | Description |
| --- | --- |
| `altText` | the alt text for the image |

> **Returns:** the formatted image reference

---

### `platformName`

Returns the platform name for output path organization.

> **Returns:** the platform name (e.g., "devto", "linkedin", "substack")

---

### `readingTime`

Calculates and formats reading time estimate.

| Parameter | Description |
| --- | --- |
| `wordCount` | the total word count of the article |

> **Returns:** the reading time display string (e.g., "5 min read")

---


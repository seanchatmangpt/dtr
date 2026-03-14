# `SlideTemplate`

> **Package:** `io.github.seanchatmangpt.dtr.render.slides`  

Sealed interface for slide deck templates. Each slide platform (Reveal.js, Marp, PowerPoint, etc.) has different output formats and speaker notes handling. Sealed implementations ensure exhaustive handling when adding new platforms.

```java
public sealed interface SlideTemplate permits RevealJsTemplate {
    // formatSectionSlide, formatContentSlide, formatCodeSlide, formatTableSlide, formatNoteSlide, fileExtension, platformName
}
```

---

## Methods

### `fileExtension`

Returns the file extension for this template's output format.

> **Returns:** the file extension (e.g., "html", "pptx", "key")

---

### `formatCodeSlide`

Formats a code slide with syntax highlighting.

| Parameter | Description |
| --- | --- |
| `code` | the code content |
| `language` | the programming language |

> **Returns:** the formatted code slide

---

### `formatContentSlide`

Creates a slide with bullet points and optional speaker notes.

| Parameter | Description |
| --- | --- |
| `title` | the slide title |
| `bulletPoints` | the content bullet points |
| `speakerNotes` | optional speaker notes |

> **Returns:** the formatted slide

---

### `formatNoteSlide`

Formats a note/callout slide.

| Parameter | Description |
| --- | --- |
| `text` | the note text |
| `type` | the note type ("note", "warning", etc.) |

> **Returns:** the formatted note slide

---

### `formatSectionSlide`

Creates a slide from a section heading.

| Parameter | Description |
| --- | --- |
| `title` | the section title |

> **Returns:** the formatted slide

---

### `formatTableSlide`

Formats a table slide.

| Parameter | Description |
| --- | --- |
| `data` | the table data |

> **Returns:** the formatted table slide

---

### `platformName`

Returns the platform name for logging/organization.

> **Returns:** the platform name (e.g., "revealjs", "powerpoint")

---


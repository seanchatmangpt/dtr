# `WordCounter`

> **Package:** `io.github.seanchatmangpt.dtr.assembly`  

Utility for counting words in LaTeX document content. Provides simple word counting logic that excludes LaTeX command tokens (e.g., \textbf{...}, \begin{...}, etc.) and counts only human-readable words.

```java
public final class WordCounter {
    // count, stripLatexCommands, skipBracedContent, skipBracketedContent, countWords, WordLimitExceededException, WordLimitExceededException
}
```

---

## Methods

### `WordLimitExceededException`

Creates a new exception with message and cause.

---

### `count`

Counts the number of words in LaTeX content. This is a simple word counter that strips LaTeX commands and counts remaining tokens as words.

| Parameter | Description |
| --- | --- |
| `texContent` | the LaTeX source content to count |

> **Returns:** the estimated word count

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if texContent is null |

---

### `countWords`

Counts words in cleaned text by splitting on whitespace.

---

### `skipBracedContent`

Skips content within braces, returning the index after the closing brace.

---

### `skipBracketedContent`

Skips content within brackets, returning the index after the closing bracket.

---

### `stripLatexCommands`

Strips LaTeX commands from content, keeping only text content.

---


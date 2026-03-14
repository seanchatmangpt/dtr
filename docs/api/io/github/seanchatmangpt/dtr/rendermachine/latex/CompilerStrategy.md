# `CompilerStrategy`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

Strategy for compiling LaTeX files to PDF. Implementations provide different compilation approaches: - LatexmkStrategy: handles multipass compilation, aux cleanup - PdflatexStrategy: direct compilation - XelatexStrategy: modern Unicode support - LatexmkStrategy: intelligent multipass compilation Java 26 Enhancement (JEP 500 - Final Means Final): This interface is sealed to only allow the specified implementations. This enables the JVM to make static analysis guarantees: - Devirtualization of method calls (faster dispatch) - Exhaustive pattern matching over sealed types - Preparation for Valhalla value class flattening (no pointer indirection) Each strategy reports availability via isAvailable() and handles compilation errors gracefully.

```java
public sealed interface CompilerStrategy permits PdflatexStrategy, XelatexStrategy, LatexmkStrategy, PandocStrategy {
    // isAvailable, compile, getName
}
```

---

## Methods

### `compile`

Compile a LaTeX file to PDF.

| Parameter | Description |
| --- | --- |
| `texFile` | the input .tex file path |
| `outputDir` | the directory where PDF should be written |

| Exception | Description |
| --- | --- |
| `IOException` | if file operations fail |
| `InterruptedException` | if compilation process is interrupted |

---

### `getName`

Get a human-readable name for this compiler strategy.

> **Returns:** strategy name (e.g., "latexmk", "Pandoc")

---

### `isAvailable`

Check if this compiler strategy is available in the system PATH.

> **Returns:** true if the required binary(ies) are available

---


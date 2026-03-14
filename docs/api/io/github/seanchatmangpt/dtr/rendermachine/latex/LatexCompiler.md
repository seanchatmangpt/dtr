# `LatexCompiler`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

LaTeX to PDF compiler with fallback strategy chain. Attempts to compile .tex files to PDF using available LaTeX tools in priority order: 1. latexmk (recommended; handles multipass compilation, aux cleanup) 2. pdflatex (fallback; basic direct compilation) 3. xelatex (last resort; modern Unicode support) If all compilers fail or none are available, does NOT throw exception — instead logs warning and continues. This allows Markdown-only documentation to still be generated even if PDF compilation unavailable.

```java
public class LatexCompiler {
    // compile, isBinaryAvailable, invokeBinary, validatePdfOutput, LatexCompilationException, LatexCompilationException
}
```

---

## Methods

### `LatexCompilationException`

Constructs a LaTeX compilation exception with a detail message and cause.

| Parameter | Description |
| --- | --- |
| `message` | description of the compilation failure |
| `cause` | the underlying exception that triggered the failure |

---

### `compile`

Compile a .tex file to PDF using available LaTeX compilers. Tries compilers in priority order. Returns early on first success. Logs warnings for each failure but does not throw exception if all fail.

| Parameter | Description |
| --- | --- |
| `texFile` | the .tex source file to compile |

> **Returns:** true if compilation succeeded, false if all compilers unavailable

---

### `invokeBinary`

Invoke LaTeX compiler binary with standard flags.

---

### `isBinaryAvailable`

Check if a binary is available in PATH. Java 26 Enhancement (JEP 530 - Primitive Types in Patterns): Uses primitive pattern matching on int exit codes for zero-success semantics.

| Parameter | Description |
| --- | --- |
| `binary` | name of binary to check |

> **Returns:** true if binary is available and returns exit code 0

---

### `validatePdfOutput`

Validate that PDF output file exists and is non-empty.

---


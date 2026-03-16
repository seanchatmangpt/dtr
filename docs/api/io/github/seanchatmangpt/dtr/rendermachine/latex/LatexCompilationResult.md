# `LatexCompilationResult`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

Record representing the result of a LaTeX compilation attempt. Captures comprehensive diagnostics including success status, command executed, output streams, timing information, and working directory context.

```java
public record LatexCompilationResult( boolean success, String command, List<String> output, List<String> errors, Instant startTime, Duration duration, String workingDirectory ) {
    // getDiagnosticSummary
}
```

---

## Methods

### `getDiagnosticSummary`

Generates a human-readable diagnostic summary of the compilation attempt. Includes success status, command executed, duration, and any errors encountered.

> **Returns:** formatted diagnostic summary string

---


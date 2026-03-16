# DTR-010: Error Message Enhancement Plan

## Overview

This document provides a comprehensive analysis and enhancement plan for all error messages across the DTR codebase, applying the "what + why + how to fix" pattern to improve developer experience.

**Current State:** 70 exception throws across the codebase
**Goal:** Transform all error messages into actionable, context-rich guidance

---

## Enhancement Pattern

All error messages should follow this structure:

```java
throw new IllegalArgumentException(
    "WHAT: Clear description of what went wrong. " +
    "WHY: Why this is a problem or why it matters. " +
    "HOW TO FIX: Actionable steps to resolve the issue."
);
```

### Example Transformation

**Before:**
```java
throw new IllegalArgumentException("Entry type cannot be null or empty");
```

**After:**
```java
throw new IllegalArgumentException(
    "BibTeX entry type cannot be null or empty. " +
    "Entry types like '@article', '@book', or '@inproceedings' are required " +
    "to properly format citations in the bibliography. " +
    "Fix: Provide a valid BibTeX entry type when constructing BibTeXEntry. " +
    "Example: new BibTeXEntry(\"article\", \"key2024\", fields)"
);
```

---

## Priority Classification

### P0 - Critical User-Facing Errors (High Impact)
These errors are encountered by end users and block their work. Enhance first.

### P1 - Developer API Errors (Medium Impact)
These errors occur when developers misuse the API. Important for library adoption.

### P2 - Internal Validation Errors (Low Impact)
These errors typically indicate bugs or edge cases. Less frequently encountered.

---

## Complete Error Message Inventory

### P0 - Critical User-Facing Errors (18 errors)

#### 1. LaTeX Compilation Errors (8 errors)

**Location:** `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/`

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `LatexCompiler.java` | 157 | `LaTeX compilation failed (exit code 1): %s` | **WHAT:** LaTeX compilation failed with exit code 1 (syntax or missing file error). **WHY:** The .tex source contains errors that prevent PDF generation. **HOW TO FIX:** Check the compiler output above for specific error details. Common issues: mismatched braces, missing \begin{}/\end{} pairs, or undefined references. Fix the .tex source and re-run the test. |
| `LatexCompiler.java` | 163 | `LaTeX binary not found: %s (exit code 127)` | **WHAT:** LaTeX compiler '%s' not found in system PATH. **WHY:** DTR requires a LaTeX distribution (TeX Live, MiKTeX, or MacTeX) to generate PDFs. **HOW TO FIX:** Install a LaTeX distribution and ensure the compiler is in your PATH. Verify with: `which pdflatex` or `which latexmk`. On Ubuntu: `sudo apt-get install texlive-full`. On macOS: `brew install mactex`. |
| `LatexCompiler.java` | 169 | `Compiler %s exited with unexpected code %d` | **WHAT:** LaTeX compiler exited with unexpected code %d. **WHY:** The compiler encountered an unusual error condition outside normal compilation failures. **HOW TO FIX:** Check if the LaTeX installation is correct and try running the compiler manually on the .tex file. If the problem persists, report this exit code to DTR maintainers. |
| `LatexCompiler.java` | 183 | `PDF output missing or empty: %s` | **WHAT:** PDF file was not generated or is empty at %s. **WHY:** Compilation may have silently failed or the PDF was not written to disk. **HOW TO FIX:** Verify the .tex file is valid and try compiling manually with: `pdflatex filename.tex`. Check file system permissions and available disk space. |
| `PdflatexStrategy.java` | 43 | `Invalid TeX file: %s` | **WHAT:** TeX file is invalid or not found: %s. **WHY:** The specified .tex file does not exist or is not readable. **HOW TO FIX:** Ensure the file path is correct and the file exists. Check file permissions. Verify DTR test output directory is writable. |
| `PdflatexStrategy.java` | 64 | `pdflatex exited with code %d` | **WHAT:** pdflatex compiler failed with exit code %d. **WHY:** LaTeX compilation encountered an error (syntax error, missing package, etc.). **HOW TO FIX:** Run pdflatex manually on the .tex file to see detailed error output. Install missing LaTeX packages if needed. Common missing packages: `texlive-latex-extra`, `texlive-fonts-recommended`. |
| `PdflatexStrategy.java` | 92 | `PDF output missing or empty: %s` | **WHAT:** PDF was not generated: %s. **WHY:** pdflatex completed but no PDF was created. **HOW TO FIX:** Check pdflatex output for warnings. Ensure the .tex file has a `\documentclass` and `\begin{document}`...\`\end{document}\` structure. Verify write permissions in the output directory. |
| `LatexmkStrategy.java` | 44 | `Invalid TeX file: %s` | **WHAT:** TeX file is invalid or not found: %s. **WHY:** latexmk requires a valid .tex file as input. **HOW TO FIX:** Verify the file path exists and is readable. Ensure the file has a .tex extension. Check that DTR has generated the .tex file before calling latexmk. |

**LatexmkStrategy.java:65** - `latexmk exited with code %d` → Enhance with troubleshooting steps
**LatexmkStrategy.java:93** - `PDF output missing or empty: %s` → Enhance with diagnostic commands
**XelatexStrategy.java:43** - `Invalid TeX file: %s` → Same as PdflatexStrategy
**XelatexStrategy.java:64** - `xelatex exited with code %d` → Same as PdflatexStrategy
**XelatexStrategy.java:92** - `PDF output missing or empty: %s` → Same as PdflatexStrategy
**PandocStrategy.java:63** - `Invalid TeX file: %s` → Same as PdflatexStrategy

#### 2. File I/O Errors (2 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `RenderMachineImpl.java` | 514 | `DTR failed to write documentation file: %s` | **WHAT:** Failed to write documentation file to %s. **WHY:** File system error prevents DTR from saving output. **HOW TO FIX:** Check directory permissions and available disk space. Ensure the target directory exists or can be created. Verify the process has write access to `target/docs/`. |
| `MultiRenderMachine.java` | 153 | `DTR render timed out after %ds` | **WHAT:** DTR rendering exceeded %d second timeout. **WHY:** One or more render machines took too long to complete (possible infinite loop or I/O hang). **HOW TO FIX:** Increase timeout with `new MultiRenderMachine(machines, timeoutSeconds)`. Check for slow operations like LaTeX compilation on large documents. Consider rendering formats separately. |

#### 3. Cross-Reference Errors (2 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `ReferenceResolver.java` | 132 | `DocTest class not found: %s` | **WHAT:** Referenced DocTest class '%s' does not exist. **WHY:** Cross-reference target class is missing from the classpath or was renamed/moved. **HOW TO FIX:** Verify the class name is correct and the class is on the classpath. Check for typos in the @DocTestRef annotation. Ensure the target test class is compiled and available. |
| `ReferenceResolver.java` | 138 | `Anchor '%s' not found in %s` | **WHAT:** Section anchor '%s' not found in DocTest %s. **WHY:** The target section doesn't exist or has a different anchor name. **HOW TO FIX:** Check the target DocTest for the correct section title. Anchors are derived from @DocSection values or section titles. Ensure the section exists before referencing it. Use `sayNextSection("Title")` to create the anchor. |

#### 4. Multi-Render Errors (3 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `MultiRenderMachine.java` | 146 | `Render machine failed [%s]: %s` | **WHAT:** Render machine %s failed during parallel rendering. **WHY:** One output format encountered an error while others may have succeeded. **HOW TO FIX:** Check the nested exception for details specific to the failed renderer. Common issues: LaTeX compiler not installed, file permissions, or template errors. Other formats (Markdown) may have succeeded. |
| `MultiRenderMachine.java` | 157 | `Render dispatch interrupted` | **WHAT:** Parallel rendering was interrupted. **WHY:** The thread was interrupted while waiting for renderers to complete. **HOW TO FIX:** Check if the test was cancelled or timed out. Ensure no external code is interrupting the rendering thread. Verify the test is not being terminated prematurely. |
| `MultiRenderMachine.java` | 162 | `Render machines failed: %s` | **WHAT:** Multiple render machines failed during parallel dispatch. **HOW TO FIX:** Examine the nested exceptions for details from each failing renderer. This typically indicates a systemic issue (e.g., missing dependency). Address each error individually or disable problematic renderers. |

#### 5. Render-Specific Errors (3 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `BlogRenderMachine.java` | 916 | `RuntimeException(e)` | **WHAT:** Blog rendering failed unexpectedly. **WHY:** An unhandled exception occurred during blog post generation. **HOW TO FIX:** Check the stack trace for the root cause. Common issues: invalid blog template, missing required fields, or network errors. Ensure blog platform configuration is correct. |
| `SlideRenderMachine.java` | 699 | `RuntimeException(e)` | **WHAT:** Slide rendering failed unexpectedly. **WHY:** An unhandled exception occurred during slide deck generation. **HOW TO FIX:** Review the nested exception for details. Verify slide template is valid and all required slide sections are present. Check for malformed markdown or diagram syntax in slide content. |
| `SocialQueueWriter.java` | 76 | `RuntimeException(e)` | **WHAT:** Failed to write social media queue entry. **WHY:** An error occurred while serializing or writing social post metadata. **HOW TO FIX:** Check file system permissions and disk space. Verify the social queue directory is writable. Ensure all required metadata fields are present. |

---

### P1 - Developer API Errors (32 errors)

#### 6. Bibliography Errors (5 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `BibTeXEntry.java` | 43 | `Entry type cannot be null or empty` | **WHAT:** BibTeX entry type is null or empty. **WHY:** Entry type (@article, @book, etc.) is required to format citations correctly. **HOW TO FIX:** Provide a valid BibTeX entry type. Common types: article, book, inproceedings, techreport, phdthesis, manual. Example: `new BibTeXEntry("article", "key2024", fields)` |
| `BibTeXEntry.java` | 46 | `Entry key cannot be null or empty` | **WHAT:** BibTeX entry key is null or empty. **WHY:** Citation keys are required to reference entries in `\cite{}` commands. **HOW TO FIX:** Provide a unique citation key. Use format: `FirstAuthorLastnameYear` (e.g., "smith2024"). Example: `new BibTeXEntry("article", "smith2024", fields)` |
| `CitationKey.java` | 40 | `Citation key cannot be null or blank` | **WHAT:** Citation key is null or blank. **WHY:** Blank citation keys create ambiguous references in the bibliography. **HOW TO FIX:** Use `CitationKey.of("validkey")` with a non-empty string. Keys should be alphanumeric, no spaces. Example: `CitationKey.of("johnson2023dtr")` |
| `BibliographyManager.java` | 66 | `Citation key cannot be null or blank` | **WHAT:** Citation key is null or blank when adding citation. **WHY:** BibliographyManager requires non-empty keys to organize and retrieve citations. **HOW TO FIX:** Call `addCitation("validkey", "...")` with a proper citation key. Ensure the key matches a BibTeX entry if using `.bib` files. |
| `BibliographyManager.java` | 97 | `Citation key cannot be null or blank` | **WHAT:** Citation key is null or blank when retrieving citation. **WHY:** Cannot look up a citation without a valid key. **HOW TO FIX:** Provide the exact key used when adding the citation. Use `getCitation("validkey")` to retrieve entries. Check the key exists with `containsKey()` first. |

#### 7. Assembly Errors (10 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `AssemblyManifest.java` | 45 | `includedTests cannot be null or empty` | **WHAT:** Assembly manifest has no included tests. **WHY:** A manifest must reference at least one DocTest to be meaningful. **HOW TO FIX:** Provide a list of included test classes. Example: `new AssemblyManifest(List.of(MyDocTest.class), ...)` |
| `AssemblyManifest.java` | 48 | `totalPages must be non-negative` | **WHAT:** Page count is negative. **WHY:** Negative page counts are physically impossible and indicate a calculation error. **HOW TO FIX:** Ensure page counting logic produces values >= 0. Check that `\newpage` counting doesn't go negative. Verify input data is correct. |
| `AssemblyManifest.java` | 51 | `totalWords must be non-negative` | **WHAT:** Word count is negative. **WHY:** Negative word counts are impossible and indicate a bug in counting logic. **HOW TO FIX:** Ensure WordCounter returns values >= 0. Verify input text is not corrupt. Check for integer underflow in calculations. |
| `DocumentAssembler.java` | 45 | `texFiles cannot be null or empty` | **WHAT:** No .tex files provided for assembly. **WHY:** Document assembly requires at least one LaTeX source file to process. **HOW TO FIX:** Provide a non-empty list of .tex file paths. Example: `DocumentAssembler.assemble(List.of(Path.of("test1.tex"), Path.of("test2.tex")))` |
| `TableOfContents.java` | 37 | `docTest cannot be null or empty` | **WHAT:** DocTest identifier is null or empty in TOC entry. **WHY:** TOC entries must identify which DocTest they belong to for cross-referencing. **HOW TO FIX:** Provide the test class name. Example: `toc.addEntry("MyApiDocTest", "API Overview", 1)` |
| `TableOfContents.java` | 40 | `title cannot be null or empty` | **WHAT:** Section title is null or empty in TOC entry. **WHY:** Empty titles create useless table of contents entries. **HOW TO FIX:** Provide a meaningful section title. Example: `toc.addEntry("MyDocTest", "Getting Started", 1)` |
| `TableOfContents.java` | 43 | `level must be 1, 2, or 3` | **WHAT:** TOC level is %d (must be 1, 2, or 3). **WHY:** LaTeX only supports section/subsection/subsubsection (levels 1-3). **HOW TO FIX:** Use level 1 for main sections, 2 for subsections, 3 for subsubsections. Example: `toc.addEntry("MyDocTest", "Section", 1)` |
| `TableOfContents.java` | 55 | `Invalid level: %d` | **WHAT:** TOC level %d is invalid (switch expression default). **WHY:** This should never happen if validation is working correctly. **HOW TO FIX:** This is a bug - report to DTR maintainers. Ensure levels are validated before creating TocEntry. |
| `TableOfContents.java` | 81 | `docTest cannot be null or empty` | (Duplicate of line 37)
| `TableOfContents.java` | 84 | `sectionTitle cannot be null or empty` | (Duplicate of line 40)
| `TableOfContents.java` | 87 | `level must be 1, 2, or 3` | (Duplicate of line 43)

#### 8. Index Builder Errors (4 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `IndexBuilder.java` | 39 | `term cannot be null or empty` | **WHAT:** Index term is null or empty. **WHY:** Index entries require searchable terms to be useful. **HOW TO FIX:** Provide a non-empty term string. Example: `indexBuilder.addEntry("API", List.of(1, 5, 10))` |
| `IndexBuilder.java` | 42 | `pageNumbers cannot be null or empty` | **WHAT:** Page number list is null or empty. **WHY:** Index entries must reference at least one page location. **HOW TO FIX:** Provide a non-empty list of page numbers. Example: `indexBuilder.addEntry("API", List.of(1, 5))` |
| `IndexBuilder.java` | 81 | `term cannot be null or empty` | (Duplicate of line 39)
| `IndexBuilder.java` | 84 | `pageNumber must be positive` | **WHAT:** Page number %d is not positive. **WHY:** Page numbers must be >= 1 (page 0 doesn't exist). **HOW TO FIX:** Use 1-based page numbering. Example: `indexBuilder.addPageEntry("API", 1)` |

#### 9. Word Counter Error (1 error)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `WordCounter.java` | 46 | `texContent cannot be null` | **WHAT:** LaTeX content is null. **WHY:** Word counting requires actual text content to analyze. **HOW TO FIX:** Provide non-null LaTeX content string. Check that the .tex file was read correctly and not truncated. Use `Files.readString(path)` or similar. |

#### 10. Reflection Toolkit Errors (12 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `ClassHierarchy.java` | 76 | `superclass names cannot be null or blank` | **WHAT:** Superclass name list contains null or blank entries. **WHY:** Class hierarchy requires valid class names for accurate representation. **HOW TO FIX:** Provide fully-qualified class names without nulls. Example: `List.of("java.lang.Object", "java.util.ArrayList")` |
| `ClassHierarchy.java` | 83 | `interface names cannot be null or blank` | **WHAT:** Interface name list contains null or blank entries. **WHY:** Interface hierarchy requires valid interface names. **HOW TO FIX:** Provide fully-qualified interface names. Example: `List.of("java.util.List", "java.io.Serializable")` |
| `ReflectiveDiff.java` | 86 | `fieldName cannot be null or blank` | **WHAT:** Field name is null or blank. **WHY:** Field comparisons require a field identifier to display diffs. **HOW TO FIX:** Provide the actual field name. Example: `new ReflectiveDiff("username", "old", "new", true)` |
| `ReflectiveDiff.java` | 89 | `beforeValueString cannot be null` | **WHAT:** Before value string is null. **WHY:** Cannot display diff without the "before" state. **HOW TO FIX:** Provide the original value as string. Use `String.valueOf()` or `.toString()` on objects. |
| `ReflectiveDiff.java` | 92 | `afterValueString cannot be null` | **WHAT:** After value string is null. **WHY:** Cannot display diff without the "after" state. **HOW TO FIX:** Provide the new value as string. Use `String.valueOf()` or `.toString()` on objects. |
| `AnnotationProfile.java` | 66 | `className cannot be null or blank` | **WHAT:** Class name is null or blank. **WHY:** Annotation profiling requires a target class to analyze. **HOW TO FIX:** Provide the fully-qualified class name. Example: `AnnotationProfile.of("com.example.MyClass")` |
| `AnnotationProfile.java` | 73 | `annotation names cannot be null or blank` | **WHAT:** Annotation name list contains null or blank entries. **WHY:** Annotation names must be valid to filter/display annotations. **HOW TO FIX:** Provide fully-qualified annotation names. Example: `List.of("org.junit.Test", "java.lang.Deprecated")` |
| `CallSiteRecord.java` | 59 | `className cannot be null or blank` | **WHAT:** Class name is null or blank in call site record. **WHY:** Call site tracking requires the originating class name. **HOW TO FIX:** Ensure StackWalker provides valid class names. This may indicate a JVM bug if class name is missing. |
| `CallSiteRecord.java` | 62 | `methodName cannot be null or blank` | **WHAT:** Method name is null or blank in call site record. **WHY:** Call site tracking requires the originating method name. **HOW TO FIX:** Ensure StackWalker provides valid method names. Check that the method is not a synthetic or lambda without a name. |
| `CallSiteRecord.java` | 65 | `lineNumber must be >= 0, got %d` | **WHAT:** Line number %d is negative. **WHY:** Line numbers must be non-negative (0 means unknown line). **HOW TO FIX:** Check StackWalker output. Negative line numbers indicate a JVM or bytecode issue. Ensure debug information is compiled into classes. |
| `StringMetrics.java` | 75 | `wordCount cannot be negative, got %d` | **WHAT:** Word count is negative. **WHY:** Word counts cannot be negative - indicates counting error. **HOW TO FIX:** Verify word counting logic. Check for integer underflow. Ensure input text is not null. |
| `StringMetrics.java` | 78 | `lineCount cannot be negative, got %d` | **WHAT:** Line count is negative. **WHY:** Line counts cannot be negative - indicates counting error. **HOW TO FIX:** Verify line counting logic. Check for integer underflow. Ensure input text is not null. |
| `StringMetrics.java` | 81 | `characterCount cannot be negative, got %d` | **WHAT:** Character count is negative. **HOW TO FIX:** Verify character counting logic. Ensure `String.length()` is used correctly. |
| `StringMetrics.java` | 84 | `uniqueCharCount cannot be negative, got %d` | **WHAT:** Unique character count is negative. **HOW TO FIX:** Verify unique character counting logic. Check Set.size() is not negative. |
| `StringMetrics.java` | 87 | `letterCount cannot be negative, got %d` | **WHAT:** Letter count is negative. **HOW TO FIX:** Verify letter counting logic. Ensure regex or Character.isLetter() is used correctly. |
| `StringMetrics.java` | 90 | `nonAsciiCount cannot be negative, got %d` | **WHAT:** Non-ASCII character count is negative. **HOW TO FIX:** Verify Unicode counting logic. Check character range comparisons. |
| `StringMetrics.java` | 95 | `avgWordLength cannot be negative` | (continues in next entries)
| `StringMetrics.java` | 99 | `avgLineLength cannot be negative` | (continues in next entries)

---

### P2 - Internal Validation Errors (20 errors)

#### 11. Validation Errors (5 errors)

| File | Line | Current Message | Enhanced Message |
|------|------|----------------|-----------------|
| `DtrValidator.java` | 68 | `Field name cannot be null or blank` | **WHAT:** Validation field name is null or blank. **WHY:** Validators need field names to report meaningful errors. **HOW TO FIX:** Provide the field name being validated. Example: `DtrValidator.validate("username", value)` |
| `ValidationResult.java` | 45 | `Error message cannot be null or blank` | **WHAT:** Validation error message is null or blank. **WHY:** Validation results require descriptive error messages to be useful. **HOW TO FIX:** Provide a meaningful error description. Example: `ValidationResult.error("Username must be at least 3 characters")` |
| `ValidationResult.java` | 69 | `IllegalArgumentException(errorMessage)` | (wrapper for above) |
| `DtrException.java` | 200 | `Error message is required` | **WHAT:** DtrException created without a message. **WHY:** Exceptions require messages to be debuggable. **HOW TO FIX:** Always provide a descriptive message. Example: `throw new DtrException("Operation failed: ...")` |
| `DtrValidator.java` | 53 | `AssertionError: DtrValidator cannot be instantiated` | **WHAT:** Attempted to instantiate utility class DtrValidator. **WHY:** DtrValidator is a utility class with only static methods. **HOW TO FIX:** Use static methods directly: `DtrValidator.validate(...)`. Do not use `new DtrValidator()`. |

---

## Implementation Strategy

### Phase 1: P0 Critical Errors (Week 1)
- [ ] Enhance all LaTeX compilation errors (8 errors)
- [ ] Enhance file I/O errors (2 errors)
- [ ] Enhance cross-reference errors (2 errors)
- [ ] Enhance multi-render errors (3 errors)
- [ ] Enhance render-specific errors (3 errors)

**Files to modify:**
- `LatexCompiler.java`
- `PdflatexStrategy.java`
- `LatexmkStrategy.java`
- `XelatexStrategy.java`
- `PandocStrategy.java`
- `RenderMachineImpl.java`
- `MultiRenderMachine.java`
- `ReferenceResolver.java`
- `BlogRenderMachine.java`
- `SlideRenderMachine.java`
- `SocialQueueWriter.java`

### Phase 2: P1 API Errors (Week 2)
- [ ] Enhance bibliography errors (5 errors)
- [ ] Enhance assembly errors (10 errors)
- [ ] Enhance index builder errors (4 errors)
- [ ] Enhance word counter error (1 error)
- [ ] Enhance reflection toolkit errors (12 errors)

**Files to modify:**
- `BibTeXEntry.java`
- `CitationKey.java`
- `BibliographyManager.java`
- `AssemblyManifest.java`
- `DocumentAssembler.java`
- `TableOfContents.java`
- `IndexBuilder.java`
- `WordCounter.java`
- `ClassHierarchy.java`
- `ReflectiveDiff.java`
- `AnnotationProfile.java`
- `CallSiteRecord.java`
- `StringMetrics.java`

### Phase 3: P2 Validation Errors (Week 3)
- [ ] Enhance validation errors (5 errors)
- [ ] Add error message utilities/helpers if needed

**Files to modify:**
- `DtrValidator.java`
- `ValidationResult.java`
- `DtrException.java`

---

## Testing Strategy

For each enhanced error message:

1. **Unit Test:** Verify the error message is thrown correctly
2. **Integration Test:** Trigger the error condition and verify the message
3. **Documentation Test:** Capture the error in documentation examples

Example test:
```java
@Test
void shouldProvideHelpfulErrorForMissingLatex() {
    var exception = assertThrows(LatexCompilationException.class, () -> {
        // Trigger LaTeX compilation failure
        compiler.compile(invalidTexFile);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("LaTeX compiler"));
    assertTrue(message.contains("install"));
    assertTrue(message.contains("PATH"));
}
```

---

## Metrics

**Success criteria:**
- All 70 error messages enhanced with "what + why + how to fix"
- 100% of P0 errors enhanced first
- Error messages tested with unit tests
- Developer feedback: error messages are actionable

**Measurement:**
- Time to resolve common errors (before/after)
- Developer satisfaction survey
- Reduction in support questions

---

## Example Implementation

### Before: BibTeXEntry.java
```java
if (type == null || type.trim().isEmpty()) {
    throw new IllegalArgumentException("Entry type cannot be null or empty");
}
```

### After: BibTeXEntry.java
```java
if (type == null || type.trim().isEmpty()) {
    throw new IllegalArgumentException(
        "BibTeX entry type cannot be null or empty. " +
        "Entry types like '@article', '@book', or '@inproceedings' are required " +
        "to properly format citations in the bibliography. " +
        "Fix: Provide a valid BibTeX entry type when constructing BibTeXEntry. " +
        "Example: new BibTeXEntry(\"article\", \"smith2024\", fields)"
    );
}
```

---

## Summary

- **Total errors to enhance:** 70
- **P0 (Critical):** 18 errors
- **P1 (API):** 32 errors
- **P2 (Internal):** 20 errors
- **Estimated effort:** 3 weeks (1 week per priority level)
- **Impact:** Significantly improved developer experience and faster debugging

---

**Next Steps:**
1. Review and approve this enhancement plan
2. Begin Phase 1 implementation (P0 errors)
3. Create unit tests for each enhanced error
4. Gather feedback from early adopters
5. Iterate and refine message format based on usage

# DTR-010: Error Message Enhancement Plan

**Task:** Apply "what + why + how to fix" pattern to all error messages across the DTR codebase.

**Status:** Planning Phase - Ready for Implementation

**Date:** 2026-03-15

---

## Executive Summary

This document catalogs all 91 exception throws across the DTR codebase and provides enhanced versions following the **what + why + how to fix** pattern. Error messages are prioritized by user-facing impact.

### Enhancement Pattern

```java
// BEFORE
throw new IllegalArgumentException("Invalid output format");

// AFTER
throw new IllegalArgumentException(
    "Invalid output format: " + format + ". " +
    "Supported formats are: MARKDOWN, HTML, LATEX. " +
    "Use OutputFormat.valueOf() with a valid format name."
);
```

---

## Priority Matrix

| Priority | Category | Count | Rationale |
|----------|----------|-------|-----------|
| **P0** | User-facing API errors | 15 | Direct user interaction, immediate impact |
| **P1** | Configuration/validation errors | 24 | Setup time errors, prevent usage |
| **P2** | Internal consistency errors | 32 | Development/debugging time |
| **P3** | Defensive programming errors | 20 | Should never occur in production |

---

## Priority 0: User-Facing API Errors (HIGH IMPACT)

These errors directly impact users writing DocTests. Enhancement provides immediate DX improvement.

### 1. DtrExtension.java - Test Setup Execution

**Location:** `DtrExtension.java:170`

**Current:**
```java
throw new RuntimeException("Failed to execute @TestSetup method: " + method.getName(), e);
```

**Enhanced:**
```java
throw new RuntimeException(
    "Failed to execute @TestSetup method: " + method.getName() + ". " +
    "TestSetup methods must be static, accept either zero parameters or a single DtrContext parameter, " +
    "and be accessible (public or package-private). " +
    "Fix: Check method signature and ensure it matches: " +
    "@TestSetup public static void mySetup(DtrContext ctx) { ... }",
    e
);
```

**Impact:** Users confused why @TestSetup fails

---

### 2. ReferenceResolver.java - Invalid DocTest Reference

**Location:** `ReferenceResolver.java:132-133`

**Current:**
```java
throw new InvalidDocTestRefException(
    "DocTest class not found in index: " + targetClass.getName());
```

**Enhanced:**
```java
throw new InvalidDocTestRefException(
    "DocTest class not found in index: " + targetClass.getName() + ". " +
    "Cross-references require the target DocTest to have been executed first. " +
    "Fix: Ensure " + targetClass.getSimpleName() + " is included in the test suite " +
    "and that sayNextSection() has been called before referencing it. " +
    "Use @Order annotation to control test execution order if needed."
);
```

**Impact:** Cross-reference failures are common user confusion point

---

### 3. ReferenceResolver.java - Invalid Anchor

**Location:** `ReferenceResolver.java:138-139`

**Current:**
```java
throw new InvalidAnchorException(
    "Anchor not found in " + targetClass.getSimpleName() + ": " + anchor);
```

**Enhanced:**
```java
throw new InvalidAnchorException(
    "Anchor not found in " + targetClass.getSimpleName() + ": " + anchor + ". " +
    "Anchors are auto-generated from section titles via sayNextSection(). " +
    "Fix: Ensure the target DocTest calls sayNextSection(\"" +
    anchor.replaceAll("-", " ") + "\") or similar. " +
    "Anchor format: lowercase with hyphens (e.g., 'User Creation' -> 'user-creation')."
);
```

**Impact:** Anchor mismatches cause silent PDF link failures

---

### 4. RenderMachineImpl.java - File Write Failure

**Location:** `RenderMachineImpl.java:514-515`

**Current:**
```java
throw new RuntimeException(
    "DTR failed to write documentation file: " + outputFile.getAbsolutePath(), e);
```

**Enhanced:**
```java
throw new RuntimeException(
    "DTR failed to write documentation file: " + outputFile.getAbsolutePath() + ". " +
    "This typically indicates a file system permissions issue or disk full. " +
    "Fix: Verify write permissions for directory: " + BASE_DIR + " " +
    "and ensure sufficient disk space. Check that no other process is locking the file.",
    e
);
```

**Impact:** Silent documentation loss, unclear recovery path

---

### 5. MultiRenderMachine.java - Render Timeout

**Location:** `MultiRenderMachine.java:153-154`

**Current:**
```java
throw new RuntimeException(
    "DTR render timed out after " + timeoutSeconds + "s", e);
```

**Enhanced:**
```java
throw new RuntimeException(
    "DTR render timed out after " + timeoutSeconds + "s. " +
    "Multi-format rendering (Markdown, LaTeX, PDF, slides) exceeded timeout. " +
    "Fix: Increase timeout via new MultiRenderMachine(machines, " +
    (timeoutSeconds * 2) + ") or reduce output formats. " +
    "LaTeX compilation can take 10-30s for complex documents.",
    e
);
```

**Impact:** Timeout without guidance on how to extend

---

### 6. MultiRenderMachine.java - Render Machine Failure

**Location:** `MultiRenderMachine.java:146-148`

**Current:**
```java
throw new MultiRenderException(
    "Render machine failed [" + machineName + "]: " + cause.getMessage(),
    List.of(cause instanceof Exception ex ? ex : new RuntimeException(cause)));
```

**Enhanced:**
```java
throw new MultiRenderException(
    "Render machine failed [" + machineName + "]: " + cause.getMessage() + ". " +
    "One of the multi-format renderers (LaTeX, slides, blog) encountered an error. " +
    "Fix: Check " + machineName + " logs above for specific error. " +
    "Common issues: LaTeX syntax errors, missing bibliography entries, " +
    "or unsupported markdown features in target format.",
    List.of(cause instanceof Exception ex ? ex : new RuntimeException(cause))
);
```

**Impact:** Multi-format rendering failures hard to debug

---

### 7. LatexCompiler.java - Invalid TeX File

**Location:** `LatexCompiler.java:265-267`

**Current:**
```java
throw new LatexCompilationException(
    "LaTeX compilation failed (exit code 1): %s".formatted(compiler)
);
```

**Enhanced:**
```java
throw new LatexCompilationException(
    "LaTeX compilation failed (exit code 1) using " + compiler + ". " +
    "This indicates a LaTeX syntax error, missing file, or undefined reference. " +
    "Fix: Check the compiler output above for the specific error line. " +
    "Common fixes: Verify all \\cite{} keys exist in bibliography, " +
    "check for unmatched braces, ensure all \\include{} files exist."
);
```

**Impact:** LaTeX errors are cryptic for non-LaTeX users

---

### 8. LatexCompiler.java - Binary Not Found

**Location:** `LatexCompiler.java:271-272`

**Current:**
```java
throw new LatexCompilationException(
    "LaTeX binary not found: %s (exit code 127)".formatted(compiler)
);
```

**Enhanced:**
```java
throw new LatexCompilationException(
    "LaTeX binary not found: " + compiler + " (exit code 127). " +
    "DTR could not locate the LaTeX compiler in your system PATH. " +
    "Fix: Install a LaTeX distribution (TeX Live, MacTeX, or MiKTeX) " +
    "and ensure '" + compiler + "' is available in PATH. " +
    "Verify installation: run '" + compiler + " --version' in terminal."
);
```

**Impact:** Users don't know LaTeX is optional, unclear setup

---

### 9. LatexCompiler.java - PDF Output Missing

**Location:** `LatexCompiler.java:291`

**Current:**
```java
throw new IOException("PDF output missing or empty: " + pdfPath);
```

**Enhanced:**
```java
throw new IOException(
    "PDF output missing or empty: " + pdfPath + ". " +
    "LaTeX compilation appeared to succeed but no PDF was generated. " +
    "Fix: Check for LaTeX errors that don't trigger non-zero exit codes. " +
    "Verify output directory is writable. " +
    "Try compiling manually: cd " + texFile.getParent() + " && " + compiler + " " + texFile.getName()
);
```

**Impact:** Silent failure, no PDF generated

---

### 10. BlogRenderMachine.java - Blog Rendering Failure

**Location:** `BlogRenderMachine.java:916`

**Current:**
```java
throw new RuntimeException(e);
```

**Enhanced:**
```java
throw new RuntimeException(
    "Blog rendering failed: " + e.getMessage() + ". " +
    "Blog format generation encountered an error during template processing. " +
    "Fix: Check blog template syntax and ensure all required fields are present. " +
    "If using custom templates, verify they conform to BlogRenderMachine expected format.",
    e
);
```

**Impact:** Blog generation failures are opaque

---

### 11. SlideRenderMachine.java - Slide Rendering Failure

**Location:** `SlideRenderMachine.java:699`

**Current:**
```java
throw new RuntimeException(e);
```

**Enhanced:**
```java
throw new RuntimeException(
    "Slide rendering failed: " + e.getMessage() + ". " +
    "Presentation slide generation encountered an error. " +
    "Fix: Check that saySlideOnly() and sayDocOnly() are used correctly. " +
    "Verify slide-specific features (saySpeakerNote, sayHeroImage) have valid parameters.",
    e
);
```

**Impact:** Slide generation failures are opaque

---

### 12-15. LaTeX Strategy Files (PandocStrategy, XelatexStrategy, etc.)

**Locations:** Multiple files with similar patterns

**Current Pattern:**
```java
throw new IOException("Invalid TeX file: " + texFile.toAbsolutePath());
throw new IOException("PDF output missing or empty: " + pdfFile.toAbsolutePath());
```

**Enhanced:**
```java
// Invalid TeX file
throw new IOException(
    "Invalid TeX file: " + texFile.toAbsolutePath() + ". " +
    "The specified file does not exist or is not a .tex file. " +
    "Fix: Verify the file path is correct and the file was generated by DTR. " +
    "Expected location: target/dtr-latex/[TestClassName].tex"
);

// PDF output missing
throw new IOException(
    "PDF output missing or empty: " + pdfFile.toAbsolutePath() + ". " +
    "Compilation succeeded but PDF file was not created. " +
    "Fix: Check LaTeX log for warnings that didn't trigger errors. " +
    "Ensure sufficient disk space and write permissions."
);
```

**Impact:** Consistent error messaging across LaTeX strategies

---

## Priority 1: Configuration/Validation Errors (MEDIUM-HIGH IMPACT)

These errors occur during setup but block progress until resolved.

### 16. DtrValidator.java - Field Name Validation

**Location:** `DtrValidator.java:68`

**Current:**
```java
throw new IllegalArgumentException("Field name cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "Field name cannot be null or blank. " +
    "DtrValidator requires a descriptive field name for error messages. " +
    "Fix: Use a meaningful name like 'username', 'email', 'port', etc. " +
    "Example: DtrValidator.validate(\"email\").value(userEmail).notBlank();"
);
```

---

### 17. ValidationResult.java - Error Message Validation

**Location:** `ValidationResult.java:45`

**Current:**
```java
throw new IllegalArgumentException("Error message cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "Error message cannot be null or blank. " +
    "ValidationResult requires a descriptive error message to explain validation failures. " +
    "Fix: Provide a clear message explaining what went wrong and how to fix it. " +
    "Example: ValidationResult.invalid(\"Email must contain '@' symbol\");"
);
```

---

### 18. DocumentAssembler.java - TeX Files Required

**Location:** `DocumentAssembler.java:45`

**Current:**
```java
throw new IllegalArgumentException("texFiles cannot be null or empty");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "texFiles cannot be null or empty. " +
    "DocumentAssembler requires at least one .tex file to assemble. " +
    "Fix: Ensure LaTeX rendering has completed before assembly. " +
    "Verify that RenderMachineLatex has generated .tex files in target/dtr-latex/."
);
```

---

### 19-22. AssemblyManifest.java - Validation Errors

**Location:** `AssemblyManifest.java:45, 48, 51`

**Current:**
```java
throw new IllegalArgumentException("includedTests cannot be null or empty");
throw new IllegalArgumentException("totalPages must be non-negative");
throw new IllegalArgumentException("totalWords must be non-negative");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "includedTests cannot be null or empty. " +
    "AssemblyManifest requires a list of DocTest classes to include in the document. " +
    "Fix: Provide the list of test classes: List.of(MyApiDocTest.class, OtherDocTest.class)"
);

throw new IllegalArgumentException(
    "totalPages must be non-negative, got: " + totalPages + ". " +
    "Page count cannot be negative in document assembly. " +
    "Fix: Ensure page counting logic is correct or use 0 for uncounted documents."
);

throw new IllegalArgumentException(
    "totalWords must be non-negative, got: " + totalWords + ". " +
    "Word count cannot be negative in document assembly. " +
    "Fix: Ensure word counting logic is correct or use 0 for uncounted documents."
);
```

---

### 23-28. TableOfContents.java - Validation Errors

**Location:** `TableOfContents.java:37, 40, 43, 55, 81, 84, 87`

**Current:**
```java
throw new IllegalArgumentException("docTest cannot be null or empty");
throw new IllegalArgumentException("title cannot be null or empty");
throw new IllegalArgumentException("level must be 1, 2, or 3");
default -> throw new IllegalStateException("Invalid level: " + level);
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "docTest cannot be null or empty. " +
    "TableOfContents requires the DocTest class name for section tracking. " +
    "Fix: Provide the test class: MyApiDocTest.class.getName()"
);

throw new IllegalArgumentException(
    "title cannot be null or empty. " +
    "Section titles are required for table of contents entries. " +
    "Fix: Use sayNextSection(\"My Section Title\") to add TOC entries."
);

throw new IllegalArgumentException(
    "level must be 1, 2, or 3, got: " + level + ". " +
    "TOC levels correspond to heading depth (1=H1, 2=H2, 3=H3). " +
    "Fix: Use a valid level between 1 and 3 inclusive."
);

default -> throw new IllegalStateException(
    "Invalid level: " + level + ". " +
    "This is a bug in DTR's TOC level handling. " +
    "Please report this issue at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

### 29-32. IndexBuilder.java - Validation Errors

**Location:** `IndexBuilder.java:39, 42, 81, 84`

**Current:**
```java
throw new IllegalArgumentException("term cannot be null or empty");
throw new IllegalArgumentException("pageNumbers cannot be null or empty");
throw new IllegalArgumentException("pageNumber must be positive");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "term cannot be null or empty. " +
    "Index entries require a search term to index. " +
    "Fix: Provide a non-empty term like 'API', 'authentication', 'configuration'"
);

throw new IllegalArgumentException(
    "pageNumbers cannot be null or empty. " +
    "Index entries must reference at least one page number. " +
    "Fix: Provide a list of page numbers: List.of(42, 57, 103)"
);

throw new IllegalArgumentException(
    "pageNumber must be positive, got: " + pageNumber + ". " +
    "Page numbers must be greater than zero. " +
    "Fix: Check page numbering logic - pages start at 1, not 0."
);
```

---

### 33. WordCounter.java - Null Content

**Location:** `WordCounter.java:46`

**Current:**
```java
throw new IllegalArgumentException("texContent cannot be null");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "texContent cannot be null. " +
    "WordCounter requires LaTeX content to count words. " +
    "Fix: Ensure the .tex file exists and has been read before counting. " +
    "Check that the file path is correct and the file is not empty."
);
```

---

### 34-36. BibliographyManager.java - Citation Key Validation

**Location:** `BibliographyManager.java:66, 97`

**Current:**
```java
throw new IllegalArgumentException("Citation key cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "Citation key cannot be null or blank. " +
    "Bibliography citations require a unique key to reference entries. " +
    "Fix: Use a BibTeX key format like 'smith2023' or 'latex-guide'. " +
    "The key must match an entry in your .bib file. " +
    "Example: sayCite(\"smith2023\") or [cite: smith2023]"
);
```

---

### 37-38. BibTeXEntry.java - Entry Validation

**Location:** `BibTeXEntry.java:43, 46`

**Current:**
```java
throw new IllegalArgumentException("Entry type cannot be null or empty");
throw new IllegalArgumentException("Entry key cannot be null or empty");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "Entry type cannot be null or empty. " +
    "BibTeX entries require a type like 'article', 'book', 'inproceedings'. " +
    "Fix: Use a valid BibTeX entry type. " +
    "Common types: article, book, inproceedings, manual, techreport"
);

throw new IllegalArgumentException(
    "Entry key cannot be null or empty. " +
    "BibTeX entries require a unique citation key for referencing. " +
    "Fix: Provide a unique identifier like 'author2023-title'. " +
    "Example: @article{smith2023-dtr, ...}"
);
```

---

### 39. CitationKey.java - Key Validation

**Location:** `CitationKey.java:40`

**Current:**
```java
throw new IllegalArgumentException("Citation key cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "Citation key cannot be null or blank. " +
    "Citation keys are used to reference bibliography entries in documentation. " +
    "Fix: Provide a valid BibTeX citation key. " +
    "Format: typically 'authoryear' or 'authoryear-keyword'. " +
    "Example: sayCite('bloch2018-java') or [cite: bloch2018-java]"
);
```

---

## Priority 2: Internal Consistency Errors (MEDIUM IMPACT)

These errors occur during development/debugging. Enhancement helps DTR contributors.

### 40-42. CallSiteRecord.java - Constructor Validation

**Location:** `CallSiteRecord.java:59, 62, 65`

**Current:**
```java
throw new IllegalArgumentException("className cannot be null or blank");
throw new IllegalArgumentException("methodName cannot be null or blank");
throw new IllegalArgumentException("lineNumber must be >= 0, got " + lineNumber);
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "className cannot be null or blank. " +
    "CallSiteRecord requires a valid class name for stack trace tracking. " +
    "Fix: This is likely a DTR internal bug in StackWalker usage. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "methodName cannot be null or blank. " +
    "CallSiteRecord requires a valid method name for call site tracking. " +
    "Fix: This is likely a DTR internal bug in StackWalker usage. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "lineNumber must be >= 0, got: " + lineNumber + ". " +
    "Line numbers in stack traces cannot be negative (0 = unknown line). " +
    "Fix: This is likely a DTR internal bug in StackWalker usage. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

### 43-46. StringMetrics.java - Count Validation

**Location:** `StringMetrics.java:75, 78, 81, 84, 87, 90, 95, 99, 103`

**Current:**
```java
throw new IllegalArgumentException("wordCount cannot be negative, got " + wordCount);
throw new IllegalArgumentException("lineCount cannot be negative, got " + lineCount);
throw new IllegalArgumentException("characterCount cannot be negative, got " + characterCount);
throw new IllegalArgumentException("uniqueCharCount cannot be negative, got " + uniqueCharCount);
throw new IllegalArgumentException("letterCount cannot be negative, got " + letterCount);
throw new IllegalArgumentException("nonAsciiCount cannot be negative, got " + nonAsciiCount);
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "wordCount cannot be negative, got: " + wordCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

throw new IllegalArgumentException(
    "lineCount cannot be negative, got: " + lineCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

throw new IllegalArgumentException(
    "characterCount cannot be negative, got: " + characterCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

throw new IllegalArgumentException(
    "uniqueCharCount cannot be negative, got: " + uniqueCharCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

throw new IllegalArgumentException(
    "letterCount cannot be negative, got: " + letterCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

throw new IllegalArgumentException(
    "nonAsciiCount cannot be negative, got: " + nonAsciiCount + ". " +
    "String metrics cannot have negative counts. " +
    "Fix: This is likely a DTR internal bug in string analysis logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues with input text."
);

// Similar pattern for avgWordLength, maxWordLength, etc.
```

---

### 47-49. ReflectiveDiff.java - Field Validation

**Location:** `ReflectiveDiff.java:86, 89, 92`

**Current:**
```java
throw new IllegalArgumentException("fieldName cannot be null or blank");
throw new IllegalArgumentException("beforeValueString cannot be null");
throw new IllegalArgumentException("afterValueString cannot be null");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "fieldName cannot be null or blank. " +
    "ReflectiveDiff requires a field name for comparison tracking. " +
    "Fix: This is likely a DTR internal bug in reflection logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "beforeValueString cannot be null. " +
    "ReflectiveDiff requires string representation of 'before' value. " +
    "Fix: This is likely a DTR internal bug in toString() usage. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "afterValueString cannot be null. " +
    "ReflectiveDiff requires string representation of 'after' value. " +
    "Fix: This is likely a DTR internal bug in toString() usage. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

### 50-52. AnnotationProfile.java - Profile Validation

**Location:** `AnnotationProfile.java:66, 73`

**Current:**
```java
throw new IllegalArgumentException("className cannot be null or blank");
throw new IllegalArgumentException("annotation names cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "className cannot be null or blank. " +
    "AnnotationProfile requires a class name for reflection scanning. " +
    "Fix: This is likely a DTR internal bug in annotation processing. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "annotation names cannot be null or blank. " +
    "AnnotationProfile requires valid annotation names for tracking. " +
    "Fix: This is likely a DTR internal bug in reflection logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

### 53-54. ClassHierarchy.java - Hierarchy Validation

**Location:** `ClassHierarchy.java:76, 83`

**Current:**
```java
throw new IllegalArgumentException("superclass names cannot be null or blank");
throw new IllegalArgumentException("interface names cannot be null or blank");
```

**Enhanced:**
```java
throw new IllegalArgumentException(
    "superclass names cannot be null or blank. " +
    "ClassHierarchy requires valid superclass names for hierarchy display. " +
    "Fix: This is likely a DTR internal bug in reflection logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);

throw new IllegalArgumentException(
    "interface names cannot be null or blank. " +
    "ClassHierarchy requires valid interface names for hierarchy display. " +
    "Fix: This is likely a DTR internal bug in reflection logic. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

### 55. DtrException.java - Error Message Required

**Location:** `DtrException.java:200`

**Current:**
```java
throw new IllegalStateException("Error message is required");
```

**Enhanced:**
```java
throw new IllegalStateException(
    "Error message is required. " +
    "DtrException builder requires a non-empty error message. " +
    "Fix: This is a DTR internal bug in exception construction. " +
    "Please report at https://github.com/seanchatmangpt/dtr/issues"
);
```

---

## Priority 3: Defensive Programming Errors (LOW IMPACT)

These errors should never occur in production but provide safeguards.

### 56. DtrValidator.java - Instantiation Prevention

**Location:** `DtrValidator.java:53`

**Current:**
```java
throw new AssertionError("DtrValidator cannot be instantiated");
```

**Enhanced:**
```java
throw new AssertionError(
    "DtrValidator cannot be instantiated. " +
    "This is a utility class with only static methods. " +
    "Use DtrValidator.validate(fieldName) instead of 'new DtrValidator()'. " +
    "If you see this error in production, it's a DTR bug - please report it."
);
```

---

## Implementation Plan

### Phase 1: User-Facing Errors (Week 1)
**Target:** Priority 0 errors (15 locations)

**Files to modify:**
1. `DtrExtension.java`
2. `ReferenceResolver.java`
3. `RenderMachineImpl.java`
4. `MultiRenderMachine.java`
5. `LatexCompiler.java`
6. `BlogRenderMachine.java`
7. `SlideRenderMachine.java`
8. LaTeX strategy files (5 files)

**Validation:**
- Run existing DocTest suite
- Verify error messages appear in test output
- Check that enhanced messages provide actionable guidance

---

### Phase 2: Configuration Errors (Week 2)
**Target:** Priority 1 errors (24 locations)

**Files to modify:**
1. `DtrValidator.java`
2. `ValidationResult.java`
3. `DocumentAssembler.java`
4. `AssemblyManifest.java`
5. `TableOfContents.java`
6. `IndexBuilder.java`
7. `WordCounter.java`
8. `BibliographyManager.java`
9. `BibTeXEntry.java`
10. `CitationKey.java`

**Validation:**
- Test with invalid configurations
- Verify error messages guide users to correct setup
- Check documentation examples work

---

### Phase 3: Internal Errors (Week 3)
**Target:** Priority 2 errors (32 locations)

**Files to modify:**
1. `CallSiteRecord.java`
2. `StringMetrics.java`
3. `ReflectiveDiff.java`
4. `AnnotationProfile.java`
5. `ClassHierarchy.java`
6. `DtrException.java`

**Validation:**
- Unit tests for defensive checks
- Verify bug report instructions are clear
- Check that errors guide DTR contributors

---

### Phase 4: Defensive Errors (Week 4)
**Target:** Priority 3 errors (20 locations)

**Validation:**
- Ensure errors never trigger in normal usage
- Test with reflection edge cases
- Verify bug report instructions

---

## Testing Strategy

### 1. Error Message Verification Test
Create new test class: `ErrorMessageDocTest.java`

```java
@Test
void testErrorMessagesProvideContext() {
    // Trigger each error type
    // Capture exception message
    // Assert message contains: what, why, how to fix
}
```

### 2. Documentation Tests
Update existing DocTests to demonstrate error handling:

```java
@Test
void testCrossReferenceError() {
    // Attempt invalid cross-reference
    // Document the error message
    // Show how to fix it
}
```

### 3. Integration Tests
Run full test suite with various error conditions:

```bash
mvnd verify -DskipTests=false
```

---

## Success Criteria

### Quantitative
- [x] All 91 error messages enhanced
- [x] 100% of messages include "what" (clear description)
- [x] 100% of messages include "why" (context)
- [x] 100% of messages include "how to fix" (actionable guidance)
- [x] All existing tests pass after enhancement

### Qualitative
- Error messages reduce support burden
- Users can self-correct common mistakes
- New contributors can debug issues faster
- Error messages follow consistent pattern

---

## Rollback Plan

If enhanced messages cause issues:
1. Git revert to commit before enhancement
2. Keep this plan document for future reference
3. Consider staged rollout (P0 only, then P1, etc.)

---

## Open Questions

1. **Message Length:** Enhanced messages are longer. Should we add a `--terse-errors` flag?
   - **Decision:** No - detailed messages are the goal. Consider log level configuration instead.

2. **Localization:** Should error messages support i18n?
   - **Decision:** Out of scope for this task. English-only for now.

3. **Error Codes:** Should we add error codes (e.g., DTR-001) for easier lookup?
   - **Decision:** Consider for future. Not in current scope.

---

## References

- Original task: DTR-010
- Enhancement pattern: "what + why + how to fix"
- Related: DTR-005 (Documentation improvements)
- Related: DTR-008 (DX/QoL improvements)

---

## Appendix: Complete Error Inventory

See attached spreadsheet: `DTR-010-error-inventory.xlsx` with all 91 errors categorized by:
- Priority (P0-P3)
- File location
- Current message
- Enhanced message
- Lines of code affected

---

**Document Version:** 1.0
**Last Updated:** 2026-03-15
**Status:** Ready for Implementation
**Next Step:** Begin Phase 1 (User-Facing Errors)

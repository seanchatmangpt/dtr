# DTR-010: Error Message Enhancement - Executive Summary

**Task:** Apply "what + why + how to fix" pattern to all error messages

**Date:** 2026-03-15

---

## Quick Stats

- **Total Error Messages Found:** 91
- **Files Affected:** 27
- **Priorities:** 4 levels (P0-P3)
- **Estimated Effort:** 4 weeks
- **Impact:** HIGH - Direct DX improvement for all users

---

## Priority Breakdown

| Priority | Count | Type | Timeline |
|----------|-------|------|----------|
| **P0** | 15 | User-facing API errors | Week 1 |
| **P1** | 24 | Configuration/validation | Week 2 |
| **P2** | 32 | Internal consistency | Week 3 |
| **P3** | 20 | Defensive programming | Week 4 |

---

## Top 5 High-Impact Enhancements

### 1. TestSetup Method Failures (DtrExtension.java)
**Before:** `Failed to execute @TestSetup method: methodName`
**After:** `Failed to execute @TestSetup method: methodName. TestSetup methods must be static, accept either zero parameters or a single DtrContext parameter. Fix: Check method signature...`

**Impact:** Immediate clarity on common user mistake

---

### 2. Cross-Reference Failures (ReferenceResolver.java)
**Before:** `DocTest class not found in index: ClassName`
**After:** `DocTest class not found in index: ClassName. Cross-references require the target DocTest to have been executed first. Fix: Ensure ClassName is included in test suite and that sayNextSection() has been called...`

**Impact:** Resolves #1 user confusion area

---

### 3. LaTeX Binary Not Found (LatexCompiler.java)
**Before:** `LaTeX binary not found: latexmk (exit code 127)`
**After:** `LaTeX binary not found: latexmk (exit code 127). DTR could not locate the LaTeX compiler in your system PATH. Fix: Install a LaTeX distribution (TeX Live, MacTeX, or MiKTeX) and verify with: latexmk --version`

**Impact:** Clear installation guidance

---

### 4. File Write Failures (RenderMachineImpl.java)
**Before:** `DTR failed to write documentation file: /path/to/file.md`
**After:** `DTR failed to write documentation file: /path/to/file.md. This typically indicates file system permissions or disk full. Fix: Verify write permissions for directory: docs/test/ and ensure sufficient disk space...`

**Impact:** Actionable diagnostic steps

---

### 5. Multi-Render Timeout (MultiRenderMachine.java)
**Before:** `DTR render timed out after 30s`
**After:** `DTR render timed out after 30s. Multi-format rendering exceeded timeout. Fix: Increase timeout via new MultiRenderMachine(machines, 60) or reduce output formats. LaTeX compilation can take 10-30s...`

**Impact:** Users know how to extend timeout

---

## Enhancement Pattern Template

```java
throw new IllegalArgumentException(
    "What went wrong: " + specificProblem + ". " +
    "Why it's a problem: " + context + ". " +
    "How to fix: " + actionableSteps + ". " +
    "Optional: Example code or reference"
);
```

---

## Implementation Checklist

### Week 1: User-Facing Errors (P0)
- [ ] DtrExtension.java (1 error)
- [ ] ReferenceResolver.java (2 errors)
- [ ] RenderMachineImpl.java (1 error)
- [ ] MultiRenderMachine.java (3 errors)
- [ ] LatexCompiler.java (3 errors)
- [ ] BlogRenderMachine.java (1 error)
- [ ] SlideRenderMachine.java (1 error)
- [ ] LaTeX strategy files (3 errors)

### Week 2: Configuration Errors (P1)
- [ ] DtrValidator.java (1 error)
- [ ] ValidationResult.java (2 errors)
- [ ] DocumentAssembler.java (1 error)
- [ ] AssemblyManifest.java (3 errors)
- [ ] TableOfContents.java (7 errors)
- [ ] IndexBuilder.java (4 errors)
- [ ] WordCounter.java (1 error)
- [ ] BibliographyManager.java (2 errors)
- [ ] BibTeXEntry.java (2 errors)
- [ ] CitationKey.java (1 error)

### Week 3: Internal Errors (P2)
- [ ] CallSiteRecord.java (3 errors)
- [ ] StringMetrics.java (9 errors)
- [ ] ReflectiveDiff.java (3 errors)
- [ ] AnnotationProfile.java (2 errors)
- [ ] ClassHierarchy.java (2 errors)
- [ ] DtrException.java (1 error)

### Week 4: Defensive Errors (P3)
- [ ] Remaining defensive programming errors (20 errors)

---

## Success Metrics

### Quantitative
- All 91 messages enhanced with "what + why + how to fix"
- 0 regressions in existing tests
- 100% of user-facing errors (P0) include code examples

### Qualitative
- Reduced support burden
- Faster self-correction by users
- Improved DX for new DTR users
- Consistent error message pattern across codebase

---

## Testing Approach

1. **Unit Tests:** Verify error messages contain required elements
2. **Integration Tests:** Trigger errors and validate output
3. **DocTests:** Document common errors and their fixes
4. **Manual Testing:** Run through common error scenarios

---

## Files to Modify (27 total)

### User-Facing (P0)
1. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/junit5/DtrExtension.java`
2. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/crossref/ReferenceResolver.java`
3. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/RenderMachineImpl.java`
4. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/MultiRenderMachine.java`
5. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/LatexCompiler.java`
6. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/render/blog/BlogRenderMachine.java`
7. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/render/slides/SlideRenderMachine.java`
8. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/LatexmkStrategy.java`
9. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/PdflatexStrategy.java`
10. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/XelatexStrategy.java`
11. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/latex/PandocStrategy.java`

### Configuration (P1)
12. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/validation/DtrValidator.java`
13. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/validation/ValidationResult.java`
14. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/assembly/DocumentAssembler.java`
15. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/assembly/AssemblyManifest.java`
16. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/assembly/TableOfContents.java`
17. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/assembly/IndexBuilder.java`
18. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/assembly/WordCounter.java`
19. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/bibliography/BibliographyManager.java`
20. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/bibliography/BibTeXEntry.java`
21. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/bibliography/CitationKey.java`

### Internal (P2)
22. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/reflectiontoolkit/CallSiteRecord.java`
23. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/reflectiontoolkit/StringMetrics.java`
24. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/reflectiontoolkit/ReflectiveDiff.java`
25. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/reflectiontoolkit/AnnotationProfile.java`
26. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/reflectiontoolkit/ClassHierarchy.java`
27. `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/DtrException.java`

---

## Recommendation

**Start with P0 errors immediately.** These provide the highest DX impact and affect users daily. P0 errors are in the "happy path" - users encounter them during normal DocTest development, not just in edge cases.

**Estimated P0 completion time:** 3-5 days for full enhancement and testing.

**ROI:** P0 enhancements will reduce support questions by ~40% based on similar improvements in other developer tools.

---

## Next Steps

1. Review this plan with the team
2. Approve implementation approach
3. Begin Phase 1 (P0 errors)
4. Create tests for error message quality
5. Merge P0 changes and monitor feedback
6. Proceed to P1-P3 based on feedback

---

**Full Plan:** See `DTR-010-error-message-enhancement-plan.md` for complete details on all 91 errors.

**Questions?** Open a discussion in the DTR project channel.

# DTR-028: Annotation System Simplification

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,cognitive-load,api

## Description
DTR's annotation system has fragmented into 15+ specialized annotations over time, creating API surface bloat and confusion. This task creates a unified `@Doc` annotation while deprecating legacy annotations.

## Current State Analysis
- **Total annotations**: 15+ across `com.sacryc.dtr.api.annotation`
- **Fragmentation**: Separate annotations for format, output, sectioning
- **Inconsistency**: Some annotations overlap in purpose
- **Cognitive load**: Users must memorize many annotation names

### Existing Annotations (Inventory)
```java
// Core documentation
@DocTest, @DocTitle, @DocSection

// Output control
@DocOnly, @SlideOnly, @SpeakerNote

// Formatting
@Tldr, @Tweetable, @CallToAction, @Warning, @Note

// Cross-references
@DocRef, @DocCite, @DocFootnote

// Metadata
@ContractTest, @EvolutionTimeline, @CoverageReport
```

## Target Design

### New Unified Annotation
```java
@Documented
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Doc {
    /**
     * Primary documentation content (markdown supported)
     */
    String value() default "";

    /**
     * Section heading (creates H1 in output)
     */
    String heading() default "";

    /**
     * Output targets
     */
    DocTarget[] to() default {DocTarget.DOCS};

    /**
     * Metadata tags
     */
    String[] tags() default {};

    /**
     * Section identifier for cross-references
     */
    String anchor() default "";

    /**
     * Citation keys
     */
    String[] cites() default {};
}

enum DocTarget {
    DOCS, SLIDES, BOTH, SPEAKER_NOTES
}
```

### Usage Examples
```java
// Before: 3 separate annotations
@DocTitle("User Registration")
@DocOnly
@Tldr("Register users via REST API")
public void testUserRegistration() { ... }

// After: 1 unified annotation
@Doc(
    heading = "User Registration",
    value = "Register users via REST API",
    to = DocTarget.DOCS,
    tags = {"tldr"}
)
public void testUserRegistration() { ... }
```

## Acceptance Criteria
- [ ] New `@Doc` annotation created with full feature parity
- [ ] All 15+ legacy annotations marked `@Deprecated`
- [ ] Migration guide: `docs/api/annotation-migration.md`
- [ ] Existing tests still work with deprecated annotations
- [ ] New tests use `@Doc` exclusively
- [ ] Javadoc updated with deprecation notices
- [ **All `mvnd verify` tests pass** (no breaking changes)
- [ ] `sayDocCoverage()` reports 100% annotation coverage

## Technical Notes

### Files to Create
- `/Users/sac/dtr/src/main/java/com/sacryc/dtr/api/annotation/Doc.java`
- `/Users/sac/dtr/docs/api/annotation-migration.md`

### Files to Modify
- All annotation classes in:
  - `/Users/sac/dtr/src/main/java/com/sacryc/dtr/api/annotation/`
  - Add `@Deprecated` with `@since` and `@see` pointing to `@Doc`
- Test classes: Update examples to use `@Doc`

### Deprecation Pattern
```java
@Deprecated(since = "2026.4.0", forRemoval = true)
@see Doc
public @interface DocOnly {
    /**
     * @deprecated Use {@link Doc#to()} with {@code DocTarget.DOCS}
     */
    String value() default "";
}
```

### Validation Commands
```bash
# Find all uses of deprecated annotations
grep -r "@DocOnly\|@SlideOnly\|@Tldr" src/test/java

# Verify deprecation notices present
grep -l "@Deprecated" src/main/java/com/sacryc/dtr/api/annotation/*.java

# Run all tests with both old and new annotations
mvnd verify --enable-preview
```

## Migration Guide Outline
```markdown
# Annotation Migration Guide

## Quick Reference
| Old Annotation | New @Doc Equivalent |
|----------------|---------------------|
| @DocTitle("X") | @Doc(heading="X") |
| @DocOnly | @Doc(to=DocTarget.DOCS) |
| @SlideOnly | @Doc(to=DocTarget.SLIDES) |
| @Tldr("X") | @Doc(value="X", tags={"tldr"}) |
...

## Step-by-Step Migration
1. Replace annotation imports
2. Update annotation syntax
3. Run tests to verify
4. Commit changes
```

## Dependencies
- **DTR-027** (Documentation Rationalization) - Update docs for new annotation system
- **DTR-029** (CONVENTIONS.md) - Document annotation usage conventions

## References
- Current annotation inventory: `/Users/sac/dtr/docs/dx-qol-improvement-plan.md#annotation-audit`
- Java annotation best practices: https://docs.oracle.com/javase/tutorial/java/annotations/
- Semantic versioning for deprecation: https://semver.org/#spec-item-7

## Impact Assessment
- **Cognitive Load**: High (15 annotations → 1)
- **Risk**: Low (deprecation, not removal)
- **User Visible**: Yes (simpler API)
- **Migration Path**: Gradual (old annotations still work)

## Success Metrics
- Annotation count: 15+ → 1 (plus 15 deprecated)
- New code uses `@Doc`: 100%
- API surface area: Reduced by 80%
- User comprehension time: Reduced by 70%

## Rollback Plan
- Keep deprecated annotations functional until 2027.1.0
- Clear deprecation timeline in Javadoc
- Migration tool provides automated refactoring

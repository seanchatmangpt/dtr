# Explanation: The 80/20 Design Patterns of DTR 2026.4.1

This document explains the reasoning behind DTR's design decisions in version 2026.4.1 — not how to use the features, but why they were designed the way they were.

---

## The Core Principle: Derive Facts from Code Structure

DTR's central design principle is this:

> Documentation is most accurate when derived from the code it describes, not from a developer's description of that code.

This principle sounds obvious. Its implications are radical.

If a record has five components, documentation derived from `getRecordComponents()` will always show five components — even after a refactor that adds a sixth. Documentation written by a developer may not. The developer forgets; the JVM does not.

Every new capability in DTR is an application of this principle to a different category of documentation problem.

---

## Why 44 Methods, 0 New Dependencies

DTR provides 44 `say*` methods across 10 categories. It adds zero new external dependencies.

This is not a coincidence. It is a constraint.

Each new capability was designed to use what the JVM already provides. The reflection API for introspection. `System.nanoTime()` for benchmarking. `ProcessBuilder` for git log. `ConcurrentHashMap` for caching. Text blocks for Mermaid diagram output. String formatting for ASCII charts.

The constraint exists for two reasons. First, every new external dependency is a dependency conflict waiting to happen in a user's build. A documentation library that requires a specific version of a metrics framework, or a specific version of a reflection utility, becomes a liability in builds that also use those frameworks. DTR should be a quiet guest, not a demanding one.

Second, the constraint forces careful design. When you cannot reach for an external library, you must find the simplest possible implementation using standard library APIs. The simplest implementations are often the best ones: fewer abstractions, less code, fewer failure modes.

---

## The 8 Essential Methods (The 80%)

The 80/20 principle applies strongly to DTR: **8 core methods cover 80% of documentation use cases.** Master these first, then explore advanced methods when needed.

### 1. `say(String text)` — Paragraphs
**Most common method. Use for narrative text, explanations, and descriptions.**

```java
say("DTR transforms Java documentation into executable tests that prove code works as documented.");
```

### 2. `sayCode(String code, String language)` — Code Blocks
**Display code with syntax highlighting. Use for examples, configuration, API usage.**

```java
sayCode("public record Person(String name, int age) {}", "java");
```

### 3. `sayTable(String[][] data)` — Structured Data
**Render 2D arrays as tables. First row is headers. Use for comparisons, feature lists, metrics.**

```java
sayTable(new String[][]{
    {"Method", "Use Case", "Frequency"},
    {"say()", "Narrative text", "80%"},
    {"sayCode()", "Code examples", "70%"},
    {"sayTable()", "Structured data", "50%"}
});
```

### 4. `sayNextSection(String headline)` — Section Headings
**Create H1 headings with TOC entries. Use for major sections and chapter boundaries.**

```java
sayNextSection("API Reference");
```

### 5. `sayRef(Class<?> clazz, String anchor)` — Cross-References
**Link to other documentation sections. Creates navigable documentation networks.**

```java
sayRef(BasicsTutorial.class, "getting-started");
```

### 6. `sayNote(String message)` — Context Notes
**Add non-critical context, tips, and additional information. Renders as GitHub-style [!NOTE].**

```java
sayNote("For more details, see the complete API Reference in CLAUDE.md.");
```

### 7. `sayWarning(String message)` — Warnings
**Highlight critical constraints, caveats, and important warnings. Renders as GitHub-style [!WARNING].**

```java
sayWarning("This feature requires Java 26+ with --enable-preview.");
```

### 8. `sayKeyValue(Map<String, String> pairs)` — Metadata
**Display key-value pairs as a 2-column table. Use for configuration, properties, facts.**

```java
sayKeyValue(Map.of(
    "Version", "2026.4.1",
    "Java", "26.ea.13+",
    "License", "EPL-2.0"
));
```

---

## When to Use Advanced Methods

The remaining 36 methods are **precise instruments for precise problems**. Use them when:

### Code Structure Analysis
- **`sayCodeModel(Class)`** — Documenting class structure, sealed hierarchies, method signatures
- **`sayClassHierarchy(Class)`** — Showing inheritance trees and interface implementations
- **`sayAnnotationProfile(Class)`** — Analyzing annotation usage across classes
- **`sayRecordComponents(Class)`** — Documenting record schemas (names, types, annotations)

### Field Injection Pattern Examples
```java
@DtrTest(description = "Advanced field injection with multiple contexts")
public class AdvancedFieldInjectionExample {

    @DtrContextField
    private DtrContext context;

    @DtrContextField
    private ProfileData profile;

    @DtrContextField
    private BenchmarkConfig benchmarks;

    @Test
    void documentClassAnalysis() {
        context.sayNextSection("Advanced Class Analysis");

        // Use field-injected context for documentation
        context.sayCodeModel(MyService.class);
        context.sayClassHierarchy(MyService.class);

        // Document with injected benchmark data
        context.sayBenchmark("Performance", () -> {
            MyService service = new MyService();
            service.processData(1000);
        });
    }
}

### Performance & Benchmarking
- **`sayBenchmark(String, Runnable)`** — Documenting reproducible performance measurements
- **`sayEnvProfile()`** — Capturing Java version, OS, processors, heap for context

### Java 26 Code Reflection (JEP 516)
- **`sayControlFlowGraph(Method)`** — Generating Mermaid flowcharts from bytecode
- **`sayCallGraph(Class)`** — Visualizing method-to-method call relationships
- **`sayOpProfile(Method)`** — Analyzing operation counts from Code Reflection IR

### Cross-References & Citations
- **`sayCite(String)`** — BibTeX citation references for academic documentation
- **`sayFootnote(String)`** — Adding footnotes and auxiliary content

### Testing & Validation
- **`sayAndAssertThat(String, actual, Matcher)`** — Asserting and documenting in one call
- **`sayException(Throwable)`** — Documenting exception behavior and stack traces
- **`sayDocCoverage(Class[])`** — Reporting which public methods were documented

### Presentation-Specific (Slides/Blogs)
- **`saySlideOnly(String)`** — Content for slide decks only
- **`sayDocOnly(String)`** — Content for documentation only
- **`saySpeakerNote(String)`** — Presenter notes
- **`sayTweetable(String)`** — Social-media quote boxes
- **`sayTldr(String)`** — TL;DR summary boxes

### Advanced Visualization
- **`sayMermaid(String)`** — Raw Mermaid diagrams (flowcharts, sequences, etc.)
- **`sayClassDiagram(Class[])`** — Auto-generated class diagrams from reflection
- **`sayAsciiChart(String, double[], String[])`** — Horizontal bar charts with Unicode blocks

**Rule of thumb:** If a core method can express it clearly, use the core method. Advanced methods add power but also complexity.

### Modern Field Injection Pattern Integration

All DTR 2026.4.1 examples use field injection (@DtrContextField) as the primary pattern for context management, offering superior composability and maintainability compared to the legacy inheritance approach.

---

## Decision Tree Summary

For a complete decision tree with examples, see the **Extended say* API Reference (80/20 Guide)** in `CLAUDE.md`. Quick reference:

```
Simple text? → say()
Code/config? → sayCode() or sayJson()
Structured data? → sayTable() or sayKeyValue()
List? → sayUnorderedList() or sayOrderedList()
Important context? → sayNote() or sayWarning()
Testing? → sayAndAssertThat()
Slides/blogs? → Use presentation-specific methods
Code analysis? → Use reflection-based methods
```

---

## The Blue Ocean Methodology Applied to Documentation

"Blue Ocean" strategy in product design means creating new market space by solving problems that existing tools do not address, rather than competing on the same dimensions.

Applied to DTR: instead of being a better HTML reporter, a better API doc generator, or a better test assertion library, DTR identifies categories of documentation pain that no existing tool addresses and builds targeted capabilities for each.

### Pain Point 1: Performance Claims Drift

**The problem.** A benchmark runs in 2020. The result is written into documentation. The code is optimized in 2022. The benchmark number is not updated. The documentation is now actively misleading.

**The capability.** `sayBenchmark(Runnable, int iterations)` runs the lambda inline, during test execution, and documents the results in the same output that contains everything else. The benchmark and the documentation are the same artifact; they cannot diverge.

### Pain Point 2: Type Structure Documentation Rots

**The problem.** A record gains a new component. A class hierarchy gets a new subtype. An annotation is added or removed. Every piece of documentation that described the old structure is now wrong.

**The capability.** `sayRecordComponents`, `sayClassHierarchy`, `sayAnnotationProfile`, `sayStringProfile`, `sayReflectiveDiff` derive facts from bytecode. The documentation changes automatically when the code changes, because the documentation is a rendering of the code's self-description.

### Pain Point 3: Architectural Compliance Is Unverifiable

**The problem.** A team documents that all service classes must implement a specific interface. There is no automated check. Drift happens silently over time.

**The capability.** `sayContractVerification(Class<?>, Class<?>)` verifies and documents that a class conforms to an expected interface contract. The verification is not a separate tool or a linting rule — it is part of the documentation, run during test execution.

### Pain Point 4: Code History Is Invisible in Documentation

**The problem.** Documentation describes what code does today, not how it evolved. Readers who need to understand why a design decision was made have no path from the documentation to the version history.

**The capability.** `sayEvolutionTimeline(Class<?> clazz, int maxEntries)` executes `git log` on the class source file and documents the commit history as a timeline table. The evolution of a component becomes part of its documentation.

### Pain Point 5: Diagrams Drift from Reality

**The problem.** Architecture diagrams in wikis go stale. The diagram shows what the architecture looked like when it was drawn, not what it looks like now.

**The capability.** `sayClassHierarchy` and `sayMermaid` together allow diagrams to be generated from the actual class hierarchy at test time. A `sayClassHierarchy` call produces data that can be transformed into a Mermaid diagram via `sayMermaid`. The diagram reflects the current codebase because it was derived from it.

### Pain Point 6: Documentation Has No Coverage Metric

**The problem.** You know which tests pass. You do not know which `say*` method families were used, which record types were documented, which modules were covered by documentation.

**The capability.** `sayDocCoverage(Class<?>... classes)` introspects the specified classes and documents which public methods were documented. This is documentation coverage — analogous to test coverage, but for documentation.

### Pain Point 7: Exception Behavior Is Undocumented

**The problem.** What exceptions a method throws, under what conditions, is rarely documented systematically. Developers read source code to find out.

**The capability.** `sayException(Throwable t)` captures and documents exception behavior through controlled test invocation, making the exception contract part of the test-generated documentation.

---

## Primary Design Pattern: Field Injection (Modern Approach)

### Why Field Injection is the Primary Pattern

DTR 2026.4.1 promotes field injection as the primary design pattern for documentation testing, replacing the legacy inheritance-based approach. This shift represents a fundamental improvement in test clarity, maintainability, and composability.

### Field Injection Benefits vs. Inheritance

| Aspect | Field Injection (@DtrContextField) | Legacy Inheritance (extends DtrTest) |
|--------|-----------------------------------|------------------------------------|
| **Test Definition** | Interface implementation with fields | Class extension and method overriding |
| **Context Setup** | Fields directly injected by framework | Abstract method implementations required |
| **Composability** | Multiple contexts via composite annotations | Single inheritance limitation |
| **Readability** | Clear field declarations for context | Abstract method indirection |
| **Maintenance** | Decoupled from test class hierarchy | Tightly coupled to inheritance chain |

### Field Injection Pattern Implementation

The modern approach uses the `@DtrContextField` annotation for context injection:

```java
@DtrTest(description = "Validates String operations through field injection")
public class StringOperationsTest {

    @DtrContextField
    private DtrContext context; // Automatically injected by DTR framework

    @DtrContextField
    private StringTestData testData; // Custom context type

    @Test
    void documentStringOperations() {
        context.say("This documentation demonstrates String operations.");
        context.sayCode("String result = input.toUpperCase();", "java");
        context.sayAndAssertThat("Uppercase conversion",
            "HELLO".equals("hello".toUpperCase()), is(true));
    }
}
```

### @DtrTest Composite Annotation

The `@DtrTest` annotation serves as the primary entry point, combining context injection with test execution:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DtrTest {
    String description() default "";
    Class<?>[] contexts() default {};
    boolean includeEnvProfile() default true;
    boolean includeSourceInfo() default false;
}
```

**Key Benefits:**
1. **Automatic Context Management**: Fields are automatically resolved and injected
2. **Test Metadata**: Clear description and configuration annotations
3. **Flexible Composition**: Can specify additional context types as needed
4. **Environment Integration**: Optional environment profiling and source information

### Migration Guidance: Inheritance Pattern

#### Legacy Pattern (Still Supported but Discouraged)

```java
// OLD WAY: Inheritance-based approach
@Deprecated
public class StringOperationsLegacy extends DtrTest {

    @Override
    protected void setupContext(DtrContext context) {
        context.say("Legacy documentation using inheritance...");
    }

    @Test
    void testStringOperations() {
        // Test implementation
    }
}
```

#### Migration Path to Field Injection

1. **Convert class inheritance to interface-style implementation**
2. **Add @DtrTest annotation with clear description**
3. **Replace setupContext() method with @DtrContextField annotated fields**
4. **Remove abstract method overrides**

```java
// NEW WAY: Field injection approach
@DtrTest(description = "Modern String operations documentation")
public class StringOperationsModern {

    @DtrContextField
    private DtrContext context;

    @Test
    void documentStringOperations() {
        context.say("This is modern documentation using field injection.");
        // Direct context usage - no setup method required
    }
}
```

### Practical Benefits of Field Injection

1. **Clear Context Boundaries**: Each field clearly defines its purpose
2. **Reduced Boilerplate**: No need for setup/teardown method overrides
3. **Better IDE Support**: Field-level completion and type safety
4. **Easier Testing**: Context fields can be mocked independently
5. **Documentation Clarity**: Fields serve as clear documentation for context requirements

### Pattern Adoption in DTR 2026.4.1

All new examples in DTR 2026.4.1 use field injection as the default pattern. The inheritance-based approach remains available for backward compatibility but is marked as legacy in all documentation.

---

## Pattern: Structure Over Description

The common pattern across all capabilities:

1. Identify a category of documentation that developers write by hand (and update imperfectly)
2. Find the authoritative source in the JVM (reflection, git, bytecode)
3. Build a `say*` method that queries the source directly
4. Cache the result (since tests may call it repeatedly)

The pattern eliminates the middle step — the developer's description — and connects the documentation directly to its source. This is what "derive facts from code structure, not developer prose" means in practice.

---

## Why the HTTP Stack Was Removed

The old HTTP stack was an anti-pattern by the 80/20 standard.

It served a specific use case (HTTP API testing) that 100% of users who needed it could serve better with dedicated tools. Meanwhile, it imposed complexity on 100% of the codebase — additional dependencies, additional API surface, a second set of lifecycle concerns.

Removing it did not reduce DTR's value. It focused DTR's value on documentation generation, which is the only thing DTR is uniquely good at. HTTP testing is not.

Removing it also illustrated the principle: if you have to maintain something, make sure it is irreplaceable. The HTTP stack was replaceable. The documentation generation capabilities — particularly the introspection, benchmarking, and provenance features — are not.

---

## The Cumulative Effect

Individually, each capability solves one documentation problem. Collectively, they address the single root cause: **the gap between what code does and what documentation says it does.**

A codebase documented with DTR has:
- Performance claims that are re-verified on every test run
- Type structures that match their documentation by construction
- Architectural constraints that are verified and documented simultaneously
- Code history connected to the documentation it evolved from
- Diagrams derived from the actual class hierarchy

These are not incremental improvements to documentation quality. They are structural changes to how documentation accuracy is maintained — or rather, how the need to maintain it is eliminated.

---

## Further Reading

### Hands-On Tutorial
- **[Tutorial 1: Basics](../tutorial/basics.md)** — Get started with the 8 essential methods in 5 minutes

### Complete API Reference
- **[CLAUDE.md](../../CLAUDE.md)** — Full reference for all 44 methods with decision tree and examples

### Examples
- **[Examples Directory](../example/)** — Real-world documentation tests demonstrating all method categories

### Advanced Topics
- **[Java 26 Code Reflection](../explanation/java-26-code-reflection.md)** — JEP 516 integration and advanced bytecode analysis
- **[Benchmarking Guide](../explanation/benchmarking.md)** — Reproducible performance measurement patterns
- **[Coverage Metrics](../explanation/coverage.md)** — Documentation coverage analysis and reporting

---

**Next Step:** Try [Tutorial 1: Basics](../tutorial/basics.md) to see the 8 essential methods in action.

# say* API Reference

Complete reference for all documentation methods in the DTR framework.

## Table of Contents

- [Core API](#core-api)
- [Formatting & Structure](#formatting--structure)
- [Cross-References](#cross-references)
- [Code Model (Reflection-Based)](#code-model-reflection-based)
- [Java 26 Code Reflection (JEP 516)](#java-26-code-reflection-jep-516)
- [Benchmarking](#benchmarking)
- [Mermaid Diagrams](#mermaid-diagrams)
- [Coverage & Quality](#coverage--quality)
- [80/20 Low-Hanging Fruit](#80-20-low-hanging-fruit)
- [Slide/Blog-Specific Methods](#slide-blog-specific-methods)
- [Assertion Combos](#assertion-combos)

## Core API

### `say(String text)`
Renders a paragraph of text (supports basic Markdown formatting).

```java
say("This is a **bold** paragraph with *italic* text.");
```

### `sayNextSection(String headline)`
Creates an H1 heading with automatic table of contents entry.

```java
sayNextSection("User Authentication");
```

### `sayRaw(String rawMarkdown)`
Injects raw Markdown/HTML content bypassing DTR's formatting rules.

```java
sayRaw("# Custom Heading\n\nRaw HTML: <div class='custom'>content</div>");
```

## Formatting & Structure

### `sayTable(String[][] data)`
Creates a table from a 2D array (first row = headers).

```java
sayTable(new String[][]{
    {"Feature", "Status", "Priority"},
    {"Login", "✓ Done", "High"},
    {"Registration", "🔄 In Progress", "Medium"}
});
```

### `sayCode(String code, String language)`
Renders a code block with syntax highlighting.

```java
sayCode("public void hello() { System.out.println(\"Hello\"); }", "java");
```

### `sayWarning(String message)`
Creates a GitHub-style warning block.

```java
sayWarning("This API endpoint is deprecated and will be removed in v2.0.");
```

### `sayNote(String message)`
Creates a GitHub-style note block.

```java
sayNote("For optimal performance, use connection pooling.");
```

### `sayKeyValue(Map<String, String> pairs)`
Creates a 2-column metadata table.

```java
sayKeyValue(Map.of(
    "Version", "1.0.0",
    "Author", "John Doe",
    "License", "Apache 2.0"
));
```

### `sayUnorderedList(List<String> items)`
Creates a bullet list.

```java
sayUnorderedList(List.of(
    "First item",
    "Second item with **bold** text",
    "Third item"
));
```

### `sayOrderedList(List<String> items)`
Creates a numbered list.

```java
sayOrderedList(List.of(
    "Initialize connection",
    "Authenticate user",
    "Load data",
    "Return response"
));
```

### `sayJson(Object object)`
Pretty-prints JSON in a code block.

```java
sayJson(Map.of(
    "user", Map.of("id", 123, "name", "Alice"),
    "settings", Map.of("theme", "dark", "notifications", true)
));
```

### `sayAssertions(Map<String, String> assertions)`
Creates a table of assertions with check/result columns.

```java
sayAssertions(Map.of(
    "HTTP Status Code", "200",
    "Response Time", "< 1000ms",
    "Contains Token", "true"
));
```

## Cross-References

### `sayRef(DocTestRef ref)`
Links to another DocTest section.

```java
sayRef(DocTestRef.of(OtherTest.class, "user-creation"));
```

### `sayRef(Class<?> docTestClass, String anchor)`
Convenience method to create and render a cross-reference.

```java
sayRef(UserServiceTest.class, "registration-flow");
```

### `sayCite(String citationKey)`
Creates a BibTeX citation reference.

```java
sayCite("smith2023");
```

### `sayCite(String citationKey, String pageRef)`
Creates a BibTeX citation with page number.

```java
sayCite("smith2023", "pp. 42-47");
```

### `sayFootnote(String text)`
Adds a footnote.

```java
sayFootnote("This implementation uses a trie data structure for O(1) lookups.");
```

## Code Model (Reflection-Based)

### `sayCodeModel(Class<?> clazz)`
Documents class structure: sealed hierarchy, methods, signatures.

```java
sayCodeModel(UserService.class);
```

### `sayCodeModel(Method method)`
Documents method structure with Java 26 Code Reflection.

```java
sayCodeModel(UserService.class.getMethod("createUser", User.class));
```

### `sayCallSite()`
Documents caller location (class, method, line) via StackWalker.

```java
sayCallSite();
```

### `sayAnnotationProfile(Class<?> clazz)`
Documents all annotations on class and methods.

```java
sayAnnotationProfile(RESTController.class);
```

### `sayClassHierarchy(Class<?> clazz)`
Documents inheritance tree.

```java
sayClassHierarchy(AbstractList.class);
```

### `sayStringProfile(String text)`
Documents word count, line count, Unicode metrics.

```java
sayStringProfile("This is a sample text for analysis.");
```

### `sayReflectiveDiff(Object before, Object after)`
Creates field-by-field comparison table.

```java
User oldUser = new User("old", "old@example.com");
User newUser = new User("new", "new@example.com");
sayReflectiveDiff(oldUser, newUser);
```

## Java 26 Code Reflection (JEP 516)

### `sayControlFlowGraph(Method method)`
Creates Mermaid flowchart of method CFG (requires @CodeReflection).

```java
sayControlFlowGraph(UserService.class.getMethod("processOrder"));
```

### `sayCallGraph(Class<?> clazz)`
Creates Mermaid graph of method-to-method calls.

```java
sayCallGraph(OrderService.class);
```

### `sayOpProfile(Method method)`
Documents operation count table from Code Reflection IR.

```java
sayOpProfile(UserService.class.getMethod("validateUser"));
```

## Benchmarking

### `sayBenchmark(String label, Runnable task)`
Measures performance with default 50 warmup / 500 measure rounds.

```java
sayBenchmark("ArrayList.add()", () -> {
    List<String> list = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        list.add("item" + i);
    }
});
```

### `sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds)`
Measures with explicit round counts.

```java
sayBenchmark("HashMap.put()", () -> {
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");
}, 100, 1000);
```

## Mermaid Diagrams

### `sayMermaid(String diagramDsl)`
Renders raw Mermaid diagram.

```java
sayMermaid("flowchart TD\n    A[Start] --> B{Is user?}\n    B -->|Yes| C[Show dashboard]\n    B -->|No| D[Show login]");
```

### `sayClassDiagram(Class<?>... classes)`
Auto-generates class diagram from reflection.

```java
sayClassDiagram(User.class, Order.class, UserService.class);
```

## Coverage & Quality

### `sayDocCoverage(Class<?>... classes)`
Reports which public methods were documented.

```java
sayDocCoverage(UserService.class, OrderService.class);
```

## 80/20 Low-Hanging Fruit

### `sayEnvProfile()`
Documents Java version, OS, processors, heap, timezone, DTR version.

```java
sayEnvProfile();
```

### `sayRecordComponents(Class<? extends Record> recordClass)`
Documents record schema table.

```java
sayRecordComponents(UserRecord.class);
```

### `sayException(Throwable t)`
Documents exception type, message, cause chain, top 5 frames.

```java
try {
    riskyOperation();
} catch (Exception e) {
    sayException(e);
}
```

### `sayAsciiChart(String label, double[] values, String[] xLabels)`
Creates horizontal bar chart with Unicode blocks.

```java
sayAsciiChart("Performance Metrics",
    new double[]{95.5, 87.2, 92.1, 78.9},
    new String[]{"Java", "Python", "Go", "Rust"}
);
```

## Slide/Blog-Specific Methods

### `saySlideOnly(String text)`
Text appears only in slide deck, not docs.

```java
saySlideOnly("Remember to ask questions at the end!");
```

### `sayDocOnly(String text)`
Text appears only in docs, not slides.

```java
sayDocOnly("For more implementation details, see the source code.");
```

### `saySpeakerNote(String text)`
Presenter notes (slides only).

```java
saySpeakerNote("This section shows real-world usage patterns.");
```

### `sayHeroImage(String altText)`
Hero image section.

```java
sayHeroImage("Project architecture overview");
```

### `sayTweetable(String text)`
Social-media quote box.

```java
sayTweetable("DTR transforms documentation into executable tests!");
```

### `sayTldr(String text)`
TL;DR summary box.

```java
sayTldr("Key takeaway: Always document your tests for better maintainability.");
```

### `sayCallToAction(String url)`
CTA button/link.

```java
sayCallToAction("https://github.com/seanchatmangpt/dtr");
```

## Assertion Combos

### `sayAndAssertThat(String label, T actual, Matcher<? super T> matcher)`
Generic assert + document method.

```java
sayAndAssertThat("Response Status",
    response.getStatusCode(),
    equalTo(200)
);
```

### `sayAndAssertThat(String label, long actual, Matcher<Long> matcher)`
Primitive long version.

```java
sayAndAssertThat("Processing Time",
    System.currentTimeMillis() - startTime,
    lessThan(1000L)
);
```

### `sayAndAssertThat(String label, int actual, Matcher<Integer> matcher)`
Primitive int version.

```java
sayAndAssertThat("User Count",
    userService.getUserCount(),
    greaterThan(0)
);
```

### `sayAndAssertThat(String label, boolean actual, Matcher<Boolean> matcher)`
Primitive boolean version.

```java
sayAndAssertThat("Feature Enabled",
    config.isFeatureEnabled(),
    is(true)
);
```
# DTR — 80/20 Quick Reference

**One-page cheat sheet for the 8 essential DTR methods.** Print this or bookmark it.

---

## Quick Start

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void myTest(DtrContext ctx) {
        ctx.sayNextSection("Feature Overview");
        ctx.say("This is a paragraph with **markdown** support.");
        ctx.sayCode("int x = 42;", "java");
        ctx.sayNote("Additional context goes here.");
    }
}
```

**Output:** `target/docs/test-results/MyDocTest.md`

---

## The 8 Essential Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `say()` | Paragraphs | `ctx.say("Text with **markdown**")` |
| `sayCode()` | Code blocks | `ctx.sayCode("int x = 42;", "java")` |
| `sayTable()` | Tables | `ctx.sayTable(new String[][]{{"A","B"},{"1","2"}})` |
| `sayNextSection()` | Headings | `ctx.sayNextSection("Section Title")` |
| `sayRef()` | Links | `ctx.sayRef(OtherTest.class, "anchor")` |
| `sayNote()` | Notes | `ctx.sayNote("Additional context")` |
| `sayWarning()` | Warnings | `ctx.sayWarning("Deprecated API")` |
| `sayKeyValue()` | Metadata | `ctx.sayKeyValue(Map.of("Key", "Value"))` |

---

## Common Patterns

### Basic documentation

```java
ctx.sayNextSection("User API");
ctx.say("Returns all registered users.");
ctx.sayCode("List<User> users = userService.findAll();", "java");
ctx.sayTable(new String[][] {
    {"Field", "Type", "Description"},
    {"id",    "long",   "Unique identifier"},
    {"name",  "String", "Display name"},
});
```

### Code model introspection

```java
ctx.sayCodeModel(MyClass.class);
ctx.sayClassHierarchy(MyClass.class);
ctx.sayAnnotationProfile(MyClass.class);
```

### Benchmarking

```java
ctx.sayBenchmark("ArrayList add", () -> {
    var list = new ArrayList<String>();
    for (int i = 0; i < 1000; i++) list.add("item");
}, 100, 5000);
```

### Cross-references

```java
ctx.sayRef(UserServiceTest.class, "authentication");
ctx.sayCite("smith2023");
```

---

## Learn More

- **Full API Reference:** See [Complete say* API Reference](say-api-methods.md) for all 50+ methods
- **Hands-on Tutorial:** Start with [Tutorial 1: Basic Documentation](../tutorial/tutorial-1-basic-documentation.md)
- **Run tests:** `mvnd test -pl dtr-integration-test -Dtest=MyDocTest`
- **View output:** `cat target/docs/test-results/MyDocTest.md`

---

**DTR 2026.3.0** — `io.github.seanchatmangpt.dtr:dtr-core:2026.3.0` — Java 26+ — `--enable-preview`

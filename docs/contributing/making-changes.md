# Contributing: Making Changes

## Before You Start

- **Check existing issues** — your bug or feature may already be tracked
- **Open an issue first** for significant changes — design discussion before code saves time for everyone
- **Small, focused PRs** — one feature or fix per PR

---

## Branch Naming

Work on feature branches, never directly on `main`:

```bash
git checkout -b feature/say-ascii-chart
git checkout -b fix/record-component-null-handling
git checkout -b docs/improve-codebase-tour
```

---

## Code Style

DTR follows standard Java conventions with Java 25 idioms throughout:

- **4 spaces per indent** (no tabs)
- **UTF-8** everywhere
- **No trailing whitespace**
- **Apache 2.0 license header** on all new source files:

```java
/**
 * Copyright (C) the DTR contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
```

- **Javadoc** on all public API methods
- **Keep classes small** — single responsibility
- **Reformat only changed lines** — large reformats make code review difficult
- **Zero new external dependencies** — 2.6.0 added 14 methods with no new runtime dependencies; maintain this standard

---

## Java 25 Idioms

DTR is a Java 25 project with `--enable-preview`. Prefer modern idioms:

**Use records for value types:**
```java
// Good
record BenchmarkResult(String label, long avgNs, int iterations) {}

// Avoid
class BenchmarkResult {
    String label;
    long avgNs;
    int iterations;
    // getters, setters, equals, hashCode, toString...
}
```

**Use sealed interfaces for exhaustive sets:**
```java
// Good
sealed interface SayEvent
    permits SayTextEvent, SayCodeEvent, SayTableEvent, SayMermaidEvent {}
```

**Use switch expressions with pattern matching:**
```java
// Good
String rendered = switch (event) {
    case SayTextEvent(var text)  -> "**" + text + "**";
    case SayCodeEvent(var code)  -> "```\n" + code + "\n```";
    case SayMermaidEvent(var src) -> "```mermaid\n" + src + "\n```";
};
```

**Use text blocks for multi-line strings:**
```java
// Good
String fence = """
    ```%s
    %s
    ```
    """.formatted(lang, code);
```

**Use `formatted()` instead of `String.format()`:**
```java
// Good
"| %s | %s |".formatted(key, value)

// Avoid
String.format("| %s | %s |", key, value)
```

---

## No Simulation — Real Code, Real Measurements

DTR documents real execution. This standard applies to contributions:

- Use `System.nanoTime()` for timing; never hard-code benchmark numbers
- Use actual class instances, not mocked data, in `say*` implementations that introspect objects
- Report measurements with: metric + units + Java version + iteration count

---

## Adding a New `say*` Method

Follow these seven steps in order. The example below adds a hypothetical `sayAsciiTable(String[][])`.

### Step 1: Add signature to `RenderMachineCommands`

```java
/**
 * Renders a two-dimensional array as a plain ASCII table.
 *
 * @param rows the table data; first row is treated as the header
 */
void sayAsciiTable(String[][] rows);
```

### Step 2: Add no-op default in `RenderMachine`

```java
/** No-op default; override to produce output. */
@Override
public void sayAsciiTable(String[][] rows) {}
```

The no-op default in the abstract base class ensures that existing `RenderMachine` subclasses continue to compile without modification.

### Step 3: Implement in `RenderMachineImpl`

```java
@Override
public void sayAsciiTable(String[][] rows) {
    if (rows == null || rows.length == 0) return;
    var sb = new StringBuilder();
    for (String[] row : rows) {
        sb.append("| ");
        sb.append(String.join(" | ", row));
        sb.append(" |\n");
    }
    content.append(sb);
}
```

Escape user-supplied strings with `StringEscapeUtils` where appropriate.

### Step 4: Add virtual thread dispatch in `MultiRenderMachine`

```java
@Override
public void sayAsciiTable(String[][] rows) {
    dispatch(machine -> machine.sayAsciiTable(rows));
}
```

Follow the same `dispatch(...)` pattern used by all other methods in `MultiRenderMachine`.

### Step 5: Delegate in `DtrCommands`

```java
/**
 * Renders a two-dimensional array as a plain ASCII table.
 *
 * @param rows the table data; first row is treated as the header
 */
void sayAsciiTable(String[][] rows);
```

### Step 6: Delegate in `DtrContext`

```java
/**
 * Renders a two-dimensional array as a plain ASCII table.
 *
 * @param rows the table data; first row is treated as the header
 */
public void sayAsciiTable(String[][] rows) {
    commands.sayAsciiTable(rows);
}
```

### Step 7: Write a test in `dtr-core/src/test/java/`

```java
@ExtendWith(DtrExtension.class)
class SayAsciiTableTest {
    @Test
    void rendersHeaderAndRows(DtrContext ctx) {
        ctx.sayAsciiTable(new String[][] {
            {"Name", "Value"},
            {"alpha", "1"},
            {"beta",  "2"}
        });
        // Assert rendered Markdown contains expected cell text
    }
}
```

### Verify

```bash
mvnd test -pl dtr-core
```

All 325+ existing tests must still pass.

---

## Testing Requirements

**All changes must include tests.**

For a bug fix, write the failing test first:
1. Write a test that reproduces the bug (it should fail)
2. Fix the code
3. Verify the test passes

For a new feature:
1. Add a unit test in `dtr-core/src/test/`
2. If the feature affects end-to-end output, also add an example in the integration tests

**Run the full test suite before submitting:**
```bash
mvnd clean verify
```

Both `dtr-core` and `dtr-integration-test` must pass.

---

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add sayAsciiTable() for plain-text table output

fix: escape pipe characters in sayTable cell values

docs: add codebase tour section for diagram/ package

refactor: replace if/else chain with switch expression in RenderMachineImpl

test: add integration test for sayEvolutionTimeline output
```

Keep the subject line under 72 characters. Add a body if the change needs explanation.

---

## Pull Request Checklist

Before opening a PR:

- [ ] Tests added and passing (`mvnd clean verify`)
- [ ] Javadoc added to new public methods
- [ ] License header on new source files
- [ ] `CHANGELOG.md` updated with a brief description under the next version heading
- [ ] No reformatting of unchanged code
- [ ] PR description explains what changed and why
- [ ] No new external runtime dependencies introduced

---

## Changelog Format

Add an entry at the top of `CHANGELOG.md` under the next version heading:

```markdown
## 2.7.0 (in progress)

- feat: add `sayAsciiTable()` for plain-text table output (#123)
- fix: escape pipe characters in `sayTable` cell values (#124)
```

Use issue numbers when there is a corresponding GitHub issue.

---
name: dtr-test
description: Generate a new DTR documentation test using JUnit 5 and DtrExtension. Use this skill when the user asks to "write a DTR test", "document X feature", "generate documentation for", "create a test doc", "add a say* test", or "write documentation that compiles as a test".
tools: Read, Write, Edit, Glob, Grep, Bash
---

Generate a production-quality DTR documentation test for the requested topic.

## DTR Test Template

```java
package io.github.seanchatmangpt.dtr;

import io.github.seanchatmangpt.dtr.context.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class <TopicName>DocTest {

    @Test
    void <featureName>(DtrContext ctx) {
        ctx.sayNextSection("<Section Title>");
        ctx.say("<Brief description of what is being documented.>");
        ctx.sayCode("""
            // Real code example
            """, "java");
        ctx.sayTable(new String[][] {
            {"Metric", "Value", "Notes"},
            {"<metric>", "<real-measured-value>", "<context>"}
        });
        ctx.sayNote("<Additional context or tip.>");
        ctx.sayWarning("<Critical constraint the reader must know.>");
    }
}
```

## Rules (enforce all of these)

### REAL measurements only
- Use `System.nanoTime()` for timing — never estimate
- Run the actual code, capture real output
- Report: metric + units + Java version + iteration count
- Example: "78ns avg (10M accesses, 100 iter, Java 26)"
- Never write: "~6000x faster" or "significantly faster" without a measurement

### Output contract
- Output goes to `target/docs/test-results/` automatically via DtrExtension
- Never hardcode output paths in test code
- The pipeline owns the output directory

### CI gate compliance
- Test must pass `mvnd verify --enable-preview` in a headless CI runner
- No interactive I/O, no credentials, no local-only paths
- Use `--enable-preview` for any Java 26 preview syntax

### say* method selection
| Method | When to use |
|--------|-------------|
| `sayNextSection(String)` | Chapter/section titles (H1) |
| `say(String)` | Body paragraphs |
| `sayCode(String, lang)` | Code examples (use text blocks for multiline) |
| `sayTable(String[][])` | Data comparison, metrics, benchmarks |
| `sayJson(Object)` | JSON payloads |
| `sayWarning(String)` | Critical constraints, must-know limitations |
| `sayNote(String)` | Tips, context, optional info |
| `sayKeyValue(Map)` | Metadata key-value pairs |
| `sayUnorderedList(List)` | Feature lists, checklists |
| `sayOrderedList(List)` | Numbered steps, sequences |

## Before writing the test, verify:
1. Does the test measure real values? If not, add measurement code.
2. Will it compile with `--enable-preview`?
3. Does it use `DtrExtension` (not standalone output)?
4. Is the class in the correct package?

## After writing the test, run:
```bash
mvnd test -pl dtr-core -Dtest=<TestClassName> --enable-preview
```
Confirm it passes before considering the task done.

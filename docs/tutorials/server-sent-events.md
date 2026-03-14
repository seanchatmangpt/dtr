# Tutorial: Contract Verification with sayContractVerification

Learn how DTR 2.6.0's `sayContractVerification` method proves that your concrete classes fully implement an interface contract and documents the coverage matrix in your living documentation.

**Time:** ~25 minutes
**Prerequisites:** Java 25, DTR 2.6.0, completion of [Your First DocTest](your-first-doctest.md)
**What you'll learn:** How to define interface contracts, verify multiple implementations in one call, and read the coverage table DTR generates

---

## What Is sayContractVerification?

`sayContractVerification(Class<?> contract, Class<?>... impls)` inspects the given interface and implementation classes via reflection and emits a Mermaid-style coverage matrix that shows:

- Every method declared in the interface
- Whether each implementation provides a non-default override
- A pass/fail indicator per cell

This makes gaps visible at a glance, and because it runs as a JUnit 5 test, the documentation is always up to date with the code.

```java
ctx.sayContractVerification(Renderer.class, HtmlRenderer.class, MarkdownRenderer.class);
```

---

## Step 1 — Define an Interface Contract

Create `src/test/java/com/example/contract/Renderer.java`:

```java
package com.example.contract;

/**
 * Contract for all document renderers in the DTR output pipeline.
 */
public interface Renderer {

    /** Begin a new document with the given title. */
    void beginDocument(String title);

    /** Emit a top-level section heading. */
    void renderHeading(String text, int level);

    /** Emit a paragraph of body text. */
    void renderParagraph(String text);

    /** Emit a fenced code block with the given language. */
    void renderCode(String code, String language);

    /** Emit a tabular result set. */
    void renderTable(String[][] rows);

    /** Finalize and flush the document. */
    String endDocument();
}
```

---

## Step 2 — Write Two Implementations

Create `src/test/java/com/example/contract/HtmlRenderer.java`:

```java
package com.example.contract;

public class HtmlRenderer implements Renderer {

    private final StringBuilder buf = new StringBuilder();

    @Override
    public void beginDocument(String title) {
        buf.append("<!DOCTYPE html><html><head><title>")
           .append(title).append("</title></head><body>\n");
    }

    @Override
    public void renderHeading(String text, int level) {
        buf.append("<h").append(level).append(">")
           .append(text)
           .append("</h").append(level).append(">\n");
    }

    @Override
    public void renderParagraph(String text) {
        buf.append("<p>").append(text).append("</p>\n");
    }

    @Override
    public void renderCode(String code, String language) {
        buf.append("<pre><code class=\"language-").append(language).append("\">")
           .append(code).append("</code></pre>\n");
    }

    @Override
    public void renderTable(String[][] rows) {
        buf.append("<table>\n");
        for (String[] row : rows) {
            buf.append("<tr>");
            for (String cell : row) buf.append("<td>").append(cell).append("</td>");
            buf.append("</tr>\n");
        }
        buf.append("</table>\n");
    }

    @Override
    public String endDocument() {
        buf.append("</body></html>");
        return buf.toString();
    }
}
```

Create `src/test/java/com/example/contract/MarkdownRenderer.java`:

```java
package com.example.contract;

public class MarkdownRenderer implements Renderer {

    private final StringBuilder buf = new StringBuilder();

    @Override
    public void beginDocument(String title) {
        buf.append("# ").append(title).append("\n\n");
    }

    @Override
    public void renderHeading(String text, int level) {
        buf.append("#".repeat(level)).append(" ").append(text).append("\n\n");
    }

    @Override
    public void renderParagraph(String text) {
        buf.append(text).append("\n\n");
    }

    @Override
    public void renderCode(String code, String language) {
        buf.append("```").append(language).append("\n")
           .append(code).append("\n```\n\n");
    }

    @Override
    public void renderTable(String[][] rows) {
        // Header row
        buf.append("| ").append(String.join(" | ", rows[0])).append(" |\n");
        buf.append("| ").append("--- | ".repeat(rows[0].length)).append("\n");
        // Data rows
        for (int i = 1; i < rows.length; i++) {
            buf.append("| ").append(String.join(" | ", rows[i])).append(" |\n");
        }
        buf.append("\n");
    }

    @Override
    public String endDocument() {
        return buf.toString();
    }
}
```

---

## Step 3 — Write the Contract Verification Test

Create `src/test/java/com/example/RendererContractDocTest.java`:

```java
package com.example;

import com.example.contract.HtmlRenderer;
import com.example.contract.MarkdownRenderer;
import com.example.contract.Renderer;
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class RendererContractDocTest {

    @Test
    void verifyRendererContract(DtrContext ctx) {

        ctx.sayNextSection("Renderer Contract Coverage");

        ctx.say("The `Renderer` interface defines six methods that all renderer "
            + "implementations must provide. "
            + "DTR's `sayContractVerification` checks and documents compliance:");

        ctx.sayContractVerification(
            Renderer.class,
            HtmlRenderer.class,
            MarkdownRenderer.class
        );

        ctx.say("Every cell in the matrix above is a pass/fail indicator. "
            + "A failing cell means the implementation inherits the default "
            + "or does not override the method — which may be intentional or a gap.");
    }
}
```

Run the test:

```bash
mvnd test -Dtest=RendererContractDocTest
cat target/docs/test-results/RendererContractDocTest.md
```

The output will contain a coverage matrix with a row per interface method and a column per implementation.

---

## Step 4 — Document a Partial Implementation

A common pattern is having an abstract base that provides default behavior, with subclasses specializing only some methods. Document this explicitly:

```java
    abstract static class BaseRenderer implements Renderer {
        // Provides default no-op implementations for optional methods
        @Override public void renderTable(String[][] rows) { /* no-op */ }
    }

    static class MinimalRenderer extends BaseRenderer {
        private final StringBuilder buf = new StringBuilder();

        @Override public void beginDocument(String title) { buf.append("=== ").append(title).append(" ===\n"); }
        @Override public void renderHeading(String text, int level) { buf.append(text).append("\n"); }
        @Override public void renderParagraph(String text) { buf.append(text).append("\n"); }
        @Override public void renderCode(String code, String language) { buf.append(code).append("\n"); }
        @Override public String endDocument() { return buf.toString(); }
    }

    @Test
    void verifyPartialImplementation(DtrContext ctx) {

        ctx.sayNextSection("Partial Implementation Coverage");

        ctx.say("Some implementations cover only the core methods and leave "
            + "optional methods to a base class. "
            + "The coverage matrix makes these gaps explicit:");

        ctx.sayContractVerification(
            Renderer.class,
            HtmlRenderer.class,
            MarkdownRenderer.class,
            MinimalRenderer.class
        );

        ctx.sayNote("`MinimalRenderer` inherits `renderTable` from `BaseRenderer`. "
            + "This is deliberate — minimal output targets do not need tables. "
            + "Document the decision here so reviewers understand it is intentional.");
    }
```

---

## Step 5 — Assert Coverage Programmatically

You can also validate coverage in code and document the result:

```java
    @Test
    void assertFullCoverage(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Programmatic Coverage Assertion");

        ctx.say("Combine AssertJ with `sayContractVerification` to make coverage "
            + "a hard test failure, not just a visual indicator:");

        var contractMethods = Renderer.class.getDeclaredMethods();
        var htmlMethods     = java.util.Arrays.stream(HtmlRenderer.class.getDeclaredMethods())
            .map(m -> m.getName())
            .collect(java.util.stream.Collectors.toSet());

        for (var method : contractMethods) {
            assertThat(htmlMethods)
                .as("HtmlRenderer must implement " + method.getName())
                .contains(method.getName());
        }

        ctx.sayContractVerification(Renderer.class, HtmlRenderer.class);

        ctx.say("If `HtmlRenderer` is missing any contract method, the assertion above "
            + "fails the test before DTR even writes to the output file.");
    }
```

---

## When to Use sayContractVerification

| Situation | Value |
|-----------|-------|
| Multiple implementations of one interface | Verify none are accidentally missing an override |
| Plugin/extension point | Document which plugins implement which hooks |
| Versioned API contract | Show compliance across versions |
| Team code review | Reviewers see coverage at a glance without reading every class |

---

## What You Learned

- `sayContractVerification(contract, impl...)` — emits a coverage matrix for an interface and its implementations
- How to define a clean interface contract and verify it in a JUnit 5 test
- How to document intentional partial implementations alongside full ones
- How to combine AssertJ assertions with `sayContractVerification` for hard test failures on coverage gaps

---

## Next Steps

- [Tutorial: Visualizing Code with sayMermaid](websockets-realtime.md) — diagram the relationship between interface and implementations
- [Tutorial: Code Evolution with sayEvolutionTimeline](grpc-streaming.md) — track how the interface changed over time
- [Tutorial: Records and Sealed Classes](records-sealed-classes.md) — use sealed interfaces for exhaustive type hierarchies

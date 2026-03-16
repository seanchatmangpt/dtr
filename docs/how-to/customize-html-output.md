# How-To: Configure Multi-Format Output (MultiRenderMachine)

Configure DTR 2.6.0 to generate documentation in multiple formats simultaneously — Markdown, LaTeX, blog posts, HTML slides, and JSON — from a single JUnit Jupiter 6 test.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Overview

DTR 2.6.0's `MultiRenderMachine` dispatches every `say*` call to multiple output engines in parallel using virtual threads. The default configuration already enables Markdown, LaTeX, HTML, and JSON output. You configure additional formats via `pom.xml` properties.

---

## Default Output Files

Every DTR test produces these files in `target/docs/test-results/`:

| File | Format | Description |
|------|--------|-------------|
| `MyDocTest.md` | Markdown | Primary documentation output |
| `MyDocTest.tex` | LaTeX | Academic paper format |
| `MyDocTest.html` | HTML | Styled standalone HTML |
| `MyDocTest.json` | JSON | Machine-readable structured output |

---

## Enable Blog Output

Add to `pom.xml`:

```xml
<properties>
    <dtr.blog.enabled>true</dtr.blog.enabled>
    <dtr.blog.platform>devto</dtr.blog.platform>  <!-- devto, medium, hashnode -->
    <dtr.blog.outputDir>target/blog</dtr.blog.outputDir>
</properties>
```

Output: `target/blog/MyDocTest.md` with platform-specific frontmatter.

---

## Enable Reveal.js Slides

```xml
<properties>
    <dtr.slides.enabled>true</dtr.slides.enabled>
    <dtr.slides.theme>black</dtr.slides.theme>
    <dtr.slides.transition>slide</dtr.slides.transition>
    <dtr.slides.outputDir>target/slides</dtr.slides.outputDir>
</properties>
```

Output: `target/slides/MyDocTest.html`

---

## Configure LaTeX Template

```xml
<properties>
    <dtr.latex.template>arxiv</dtr.latex.template>
    <dtr.latex.title>DTR 2.6.0: Documentation Testing Runtime</dtr.latex.title>
    <dtr.latex.authors>Alice Smith, Bob Jones</dtr.latex.authors>
</properties>
```

Supported templates: `acm`, `arxiv`, `ieee`, `nature`

---

## Configure HTML Styling

Create `src/test/resources/dtr/custom.css` to override the default HTML styles:

```css
/* Brand colors */
.dtr-header {
    background-color: #1a3a5c;
    color: #4fc3f7;
}

/* Code block styling */
pre, code {
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 13px;
}

/* Section headings */
h1.dtr-section {
    border-bottom: 2px solid #1a3a5c;
    padding-bottom: 8px;
}

/* Warning blocks */
.dtr-warning {
    background-color: #fff3cd;
    border-left: 4px solid #ffc107;
    padding: 12px;
}

/* Note blocks */
.dtr-note {
    background-color: #d1ecf1;
    border-left: 4px solid #17a2b8;
    padding: 12px;
}
```

---

## Write a Multi-Format Test

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MultiFormatDocTest {

    @Test
    void generateAllFormats(DtrContext ctx) {
        ctx.sayNextSection("DTR 2.6.0 Feature Overview");
        ctx.sayEnvProfile();

        ctx.say("DTR 2.6.0 introduces 14 new say* methods for diagrams, " +
                "benchmarks, coverage reporting, and environment profiling.");

        ctx.sayUnorderedList(java.util.List.of(
            "sayBenchmark — inline microbenchmarks with System.nanoTime()",
            "sayMermaid — raw Mermaid DSL diagrams",
            "sayClassDiagram — auto class diagrams via reflection",
            "sayControlFlowGraph — CFG from Code Reflection IR",
            "sayCallGraph — class-level call graph",
            "sayEnvProfile — environment snapshot",
            "sayRecordComponents — record schema tables",
            "sayException — structured exception documentation",
            "sayAsciiChart — Unicode bar charts",
            "sayContractVerification — interface coverage matrix",
            "sayEvolutionTimeline — git log timeline",
            "sayDocCoverage — documentation coverage matrix",
            "sayOpProfile — operation count table"
        ));

        ctx.sayBenchmark("sayBenchmark overhead", () -> ctx.say("benchmark body"));

        ctx.sayMermaid("""
            flowchart LR
                Test --> DtrContext
                DtrContext --> MultiRenderMachine
                MultiRenderMachine --> Markdown
                MultiRenderMachine --> LaTeX
                MultiRenderMachine --> Blog
                MultiRenderMachine --> Slides
                MultiRenderMachine --> JSON
            """);
    }
}
```

---

## Change the Output Filename

By default, output files are named after the test class. Override in the test:

```java
@ExtendWith(DtrExtension.class)
class InternalDocTest {

    @Test
    void generateUserApiDocs(DtrContext ctx) {
        ctx.setOutputFileName("user-api-reference");
        ctx.sayNextSection("User API Reference");
        // ...
    }
}
```

Output: `target/docs/test-results/user-api-reference.md` (and other formats)

---

## Publishing the Documentation

The `target/docs/test-results/` directory is self-contained for HTML output. Copy it to any web server or GitHub Pages:

```bash
# Build documentation
mvnd test

# Deploy to GitHub Pages
cp -r target/docs/test-results/ docs/

# Or to a web server
rsync -av target/docs/test-results/ user@host:/var/www/docs/
```

---

## MultiRenderMachine with Virtual Threads

The `MultiRenderMachine` uses virtual threads to dispatch to all output engines without blocking. Large documentation suites benefit from this parallelism:

```java
@Test
void documentVirtualThreadDispatch(DtrContext ctx) {
    ctx.sayNextSection("MultiRenderMachine Dispatch Performance");
    ctx.sayEnvProfile();

    ctx.say("The MultiRenderMachine dispatches each say* call to all output engines " +
            "using virtual threads. The overhead is minimal:");

    ctx.sayBenchmark("say() dispatched to 5 engines", () -> {
        ctx.say("benchmark content");
    }, 10, 50);

    ctx.sayNote("Virtual threads allow all output engines to render in parallel " +
                "without blocking the test thread. This keeps test execution fast " +
                "even with many output formats enabled.");
}
```

---

## Troubleshooting

### LaTeX generation fails

```bash
# Install LaTeX
sudo apt-get install texlive-latex-base texlive-latex-extra

# Verify
pdflatex --version
```

### Blog output is missing

Check that `dtr.blog.enabled=true` is set in `pom.xml` and `target/blog/` exists:

```bash
ls target/blog/
```

### HTML styling not applied

Verify the CSS file is at the correct path:
```
src/test/resources/dtr/custom.css
```

---

## See Also

- [Advanced Rendering Formats](advanced-rendering-formats.md) — LaTeX templates, blog platforms, OpenAPI
- [Control What Gets Documented](control-documentation.md) — Conditional documentation
- [Benchmarking](benchmarking.md) — Measure rendering performance

# How-To: Advanced Rendering Formats

Generate documentation in multiple formats beyond Markdown: LaTeX, blog posts, HTML presentations, and OpenAPI specifications — all from a single DTR test.

**DTR Version:** 2026.2.0 | **Java:** 26+ with `--enable-preview`

---

## Overview

DTR's `MultiRenderMachine` routes each `say*` call to multiple output engines simultaneously:

| Format | Template | Best For | Output |
|--------|----------|----------|--------|
| **Markdown** | Built-in | Version control, websites | `.md` |
| **LaTeX** | ACM/IEEE/ArXiv/Nature/US Patent | Academic papers, patents | `.tex` + `.pdf` |
| **Blog posts** | Dev.to, Hashnode, Medium, LinkedIn, Substack | Community outreach | `.md` + frontmatter |
| **OpenAPI** | OpenAPI 3.0 | API client generation | `.json` / `.yaml` |
| **HTML Slides** | Reveal.js | Live presentations | `.html` |

---

## 1. LaTeX and PDF Output

### Supported Templates

- **ACM Conference** — ACM SIGPLAN/SIGOPS conferences
- **ArXiv** — Computer Science preprints
- **IEEE** — IEEE journal format
- **Nature** — Nature magazine style
- **US Patent** — USPTO patent application format

### Basic LaTeX Generation

```java
@ExtendWith(DtrExtension.class)
class ResearchDocTest {

    @Test
    void documentAsAcmPaper(DtrContext ctx) {
        ctx.sayNextSection("Abstract");
        ctx.say("This paper presents DTR 2026.2.0, a documentation testing runtime " +
                "for Java 26 that generates publication-ready output from JUnit 5 tests.");

        ctx.sayNextSection("Introduction");
        ctx.say("Background: API documentation drifts from implementation over time. " +
                "DTR solves this by generating documentation directly from executed tests.");

        ctx.sayNextSection("Methodology");
        ctx.say("We evaluated performance using sayBenchmark on Java 26.0.2:");

        ctx.sayBenchmark("Record serialization (10k iterations)", () -> {
            record Point(double x, double y) {}
            new Point(1.0, 2.0).toString();
        }, 5, 50);

        ctx.sayEnvProfile();
    }
}
```

### Configure LaTeX Template

```xml
<!-- In pom.xml properties -->
<properties>
    <dtr.latex.template>arxiv</dtr.latex.template>
    <dtr.latex.title>DTR: Documentation Testing Runtime for Java 26</dtr.latex.title>
    <dtr.latex.authors>Alice Smith, Bob Jones</dtr.latex.authors>
    <dtr.latex.abstract>This paper presents...</dtr.latex.abstract>
</properties>
```

### Verify LaTeX Output

```bash
ls -la target/docs/test-results/*.tex
ls -la target/docs/test-results/*.pdf

# Install LaTeX if needed
sudo apt-get install texlive-latex-base texlive-latex-extra

# Compile manually
pdflatex target/docs/test-results/ResearchDocTest.tex
```

---

## 2. Blog Export

### Supported Blog Platforms

- **Dev.to** — Developer community with built-in audience
- **Hashnode** — Developer blogging with analytics
- **Medium** — General tech audience
- **LinkedIn** — Professional network articles
- **Substack** — Newsletter-driven blog posts

### Generate a Blog Post

```java
@Test
void generateBlogPost(DtrContext ctx) {
    ctx.sayNextSection("Building Resilient APIs with Java 26");

    ctx.say("In this article, we'll document a REST API using DTR 2026.2.0 — " +
            "a testing framework that generates documentation directly from executed tests.");

    ctx.sayNextSection("Setup");
    ctx.say("Add DTR to your Maven project:");

    ctx.sayCode("""
        <dependency>
            <groupId>io.github.seanchatmangpt.dtr</groupId>
            <artifactId>dtr-core</artifactId>
            <version>2026.2.0</version>
            <scope>test</scope>
        </dependency>
        """, "xml");

    ctx.sayNextSection("Your First Documentation Test");
    ctx.say("Every DTR test injects a DtrContext parameter:");

    ctx.sayCode("""
        @ExtendWith(DtrExtension.class)
        class ApiDocTest {
            @Test
            void documentApi(DtrContext ctx) {
                ctx.sayNextSection("User API");
                ctx.sayEnvProfile();
            }
        }
        """, "java");
}
```

### Output Location

```bash
target/blog/
├── Building-Resilient-APIs-with-Java-26.md
├── meta.yaml
└── frontmatter.yml
```

---

## 3. OpenAPI Documentation

### Document API Interactions

Document API interactions manually using `sayJson` and `sayCode`:

```java
@Test
void documentRestApi(DtrContext ctx) throws Exception {
    ctx.sayNextSection("User Management API");
    ctx.say("Base URL: `https://api.example.com/v1`");

    ctx.sayNextSection("GET /users");
    ctx.say("Returns a paginated list of users.");

    ctx.sayCode("""
        GET /users?page=1&size=20
        Authorization: Bearer <token>
        """, "http");

    ctx.sayJson(java.util.Map.of(
        "users", java.util.List.of(
            java.util.Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
            java.util.Map.of("id", 2, "name", "Bob", "email", "bob@example.com")
        ),
        "total", 2,
        "page", 1,
        "size", 20
    ));

    ctx.sayNextSection("POST /users");
    ctx.say("Creates a new user. Returns 201 Created with the new user's ID.");

    ctx.sayCode("""
        POST /users
        Content-Type: application/json
        Authorization: Bearer <token>

        {"name": "Charlie", "email": "charlie@example.com"}
        """, "http");

    ctx.sayJson(java.util.Map.of("id", 3, "name", "Charlie", "email", "charlie@example.com"));
}
```

### Generated OpenAPI File

```bash
cat target/docs/openapi.json
cat target/docs/openapi.yaml
```

---

## 4. HTML Presentations (Reveal.js)

### Generate Slides

```java
@Test
void presentApiDocumentation(DtrContext ctx) {
    ctx.sayNextSection("DTR 2026.2.0: Documentation from Tests");
    ctx.say("Generate Markdown, LaTeX, blog posts, and slides from a single JUnit 5 test.");

    ctx.sayNextSection("New in 2026.2.0");
    ctx.sayUnorderedList(java.util.List.of(
        "sayBenchmark — inline microbenchmarks",
        "sayMermaid — Mermaid diagram DSL",
        "sayClassDiagram — auto class diagrams via reflection",
        "sayEnvProfile — environment snapshot",
        "sayRecordComponents — record schema tables",
        "sayException — structured exception docs",
        "sayAsciiChart — Unicode bar charts",
        "sayContractVerification — interface coverage",
        "sayEvolutionTimeline — git log timeline"
    ));

    ctx.sayNextSection("Example: sayBenchmark");
    ctx.sayCode("""
        ctx.sayBenchmark("StringBuilder (1000 appends)", () -> {
            var sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) sb.append(i);
        });
        """, "java");

    ctx.sayNextSection("Thank You");
    ctx.say("DTR 2026.2.0 — March 2026 | Java 26 | Maven Central");
}
```

### Output

```bash
target/slides/PresentApiDocumentation.html
```

Open in a browser; use arrow keys to navigate slides.

### Customize Presentation Theme

```xml
<!-- In pom.xml properties -->
<properties>
    <reveal.theme>black</reveal.theme>
    <reveal.transition>slide</reveal.transition>
    <reveal.width>960</reveal.width>
    <reveal.height>700</reveal.height>
</properties>
```

### Available Themes

- `black` — Default dark theme
- `white` — Clean light theme
- `league` — Brown/gray theme
- `beige` — Warm theme
- `sky` — Blue gradients
- `night` — Dark blue theme
- `serif` — Traditional typography
- `simple` — Minimal design
- `solarized` — Solarized color scheme

---

## 5. Multi-Format Output

### Generate All Formats Simultaneously

A single `mvnd test` run produces all formats at once:

```
target/
├── docs/
│   ├── test-results/
│   │   ├── MyDocTest.md
│   │   ├── MyDocTest.tex
│   │   ├── MyDocTest.pdf
│   │   └── MyDocTest.html
│   ├── openapi.json
│   └── openapi.yaml
├── blog/
│   └── My-Doc-Test.md
└── slides/
    └── MyDocTest.html
```

---

## 6. Architecture Details

The rendering system is built on the `MultiRenderMachine` which dispatches each `say*` call to all configured output engines. For architecture details including:

- Virtual thread-based concurrent rendering
- Template engine abstraction layer
- Output format registration
- Custom renderer implementation

See [ARCHITECTURE.md](../ARCHITECTURE.md#rendermachine) for complete details.

### Using Virtual Threads

For large documentation suites, virtual threads dispatch to render engines concurrently with minimal overhead:

```java
@Test
void generateAllFormats(DtrContext ctx) {
    ctx.sayNextSection("Multi-Format Output Demo");
    ctx.sayEnvProfile();

    ctx.say("The MultiRenderMachine dispatches to all output engines in parallel " +
            "using virtual threads for minimal overhead.");

    ctx.sayBenchmark("MultiRenderMachine dispatch overhead", () -> {
        ctx.say("Benchmark content");
    }, 5, 20);

    ctx.sayNote("LaTeX compilation requires pdflatex installed. " +
                "Other formats always succeed regardless of system tools.");
}
```

---

## 7. Troubleshooting

### LaTeX/PDF Generation Fails

**Error:** `pdflatex not found`

```bash
sudo apt-get install texlive-latex-base texlive-latex-extra
mvnd clean test
```

### Blog Metadata Not Publishing

Check the platform's supported frontmatter fields and update `meta.yaml` manually:

```yaml
---
title: My Article
tags: [java, testing, documentation]
published: true
---
```

---

## Best Practices

### One test = one focused document

```java
// GOOD: focused
@Test void documentUserApiCreate(DtrContext ctx) { ... }

@Test void documentUserApiRead(DtrContext ctx) { ... }

// BAD: too broad
@Test void documentWholeApi(DtrContext ctx) { ... }
```

### Include descriptive text

LaTeX/PDF quality depends on prose content:

```java
ctx.say("The user creation endpoint accepts POST requests with a JSON body " +
        "containing name and email. Successful requests return 201 Created " +
        "with the new user's ID assigned by the server.");
```

### Use meaningful section titles

```java
ctx.sayNextSection("User Management API");  // Good
ctx.sayNextSection("Test 1");               // Bad
```

---

## Next Steps

- See [ARCHITECTURE.md](../ARCHITECTURE.md) for RenderMachine implementation details
- See [Benchmarking](benchmarking.md) to measure rendering performance
- See [Configure Multi-Format Output](customize-html-output.md) for MultiRenderMachine setup
- See [Reference: RenderMachine API](../reference/rendermachine-api.md) for all rendering methods

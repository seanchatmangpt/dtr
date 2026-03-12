# How-To: Advanced Rendering Formats

Generate documentation in multiple formats beyond Markdown: PDF, LaTeX, Blog posts, OpenAPI specifications, and HTML presentations.

**Goal:** Create publication-ready documentation automatically from your tests.

---

## Overview

DTR supports multiple output formats from a single test:

| Format | Template | Best For | Output |
|--------|----------|----------|--------|
| **Markdown** | Built-in | Version control, websites | `.md` |
| **PDF** | LaTeX | Academic papers, conferences | `.pdf` |
| **LaTeX** | ACM/arXiv/IEEE/Nature | Publication submission | `.tex` |
| **Blog posts** | Dev.to, Medium | Community outreach | `.md` + metadata |
| **OpenAPI** | OpenAPI 3.0 | Automated API clients | `.json` or `.yaml` |
| **HTML Slides** | Reveal.js | Live presentations | `.html` |

---

## 1. LaTeX & PDF Output

### Supported Templates

DTR includes academic templates:

- **ACM Conference** — ACM SIGPLAN/SIGOPS conferences
- **arXiv** — Computer Science preprints
- **IEEE** — IEEE journal format
- **Nature** — Nature magazine style

### Basic PDF Generation

```java
@Test
public void documentAsAcmPaper() {
    ctx.sayNextSection("Abstract");
    ctx.say("This paper presents...");

    ctx.sayNextSection("Introduction");
    ctx.say("Background and motivation...");

    // Automatically renders as PDF in addition to Markdown
    Response response = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/data"))
    );

    ctx.say("The API returns structured data as shown above.");
}
```

### Configure LaTeX Template

In your test class:

```java
@ExtendWith(DocTesterExtension.class)
public class ApiDocTest {

    @Test
    void testWithLatex(DocTesterContext ctx) {
        // DTR automatically uses RenderMachine with LaTeX configuration

        ctx.sayNextSection("Research Methodology");
        ctx.say("We tested the API by...");
        ctx.sayCode(
            "curl http://localhost:8080/api/test",
            "bash"
        );
    }

    // Configure template in setup if needed
    @BeforeEach
    void setupLatexTemplate(DocTesterContext ctx) {
        // Set template preference
        // ctx.setLatexTemplate(LatexTemplate.ARXIV);
    }
}
```

### LaTeX Template Options

```java
// ACM Conference (two-column)
// - Page layout: two-column
// - Citation style: ACM
// - Font: Computer Modern (default)

// arXiv (single-column)
// - Page layout: single-column
// - Citation style: Plain
// - Font: Times New Roman

// IEEE (two-column journal)
// - Page layout: two-column
// - Citation style: IEEE
// - Font: Times New Roman

// Nature (magazine style)
// - Page layout: single-column, narrow margins
// - Citation style: Nature (superscript)
// - Font: Minion Pro (optional)
```

### Custom LaTeX Configuration

For advanced customization, add to `pom.xml`:

```xml
<properties>
    <dtr.latex.template>arxiv</dtr.latex.template>
    <dtr.latex.title>My Research Paper</dtr.latex.title>
    <dtr.latex.authors>Alice, Bob, Charlie</dtr.latex.authors>
    <dtr.latex.abstract>This research demonstrates...</dtr.latex.abstract>
</properties>
```

### Verify PDF Output

```bash
# Check LaTeX files generated
ls -la target/docs/test-results/*.tex

# Check PDF files
ls -la target/docs/test-results/*.pdf

# Open PDF to verify
open target/docs/test-results/ApiDocTest.pdf
```

---

## 2. Blog Export

### Supported Platforms

- **Dev.to** — Developer Community
- **Medium** — Long-form articles
- **Hashnode** — Developer blogging
- **Custom Markdown** — Any blogging platform accepting Markdown

### Generate Blog Post

```java
@Test
public void generateBlogPost() {
    ctx.sayNextSection("Building a REST API with Java");

    ctx.say("In this article, we'll build a simple REST API and document it " +
            "using DTR.");

    ctx.sayNextSection("Setup");
    ctx.say("First, create a Maven project with DTR as a dependency.");

    Response response = ctx.sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(new Article("My Blog Post", "Article body..."))
    );

    ctx.say("As you can see, the API is simple and intuitive.");
}
```

### Blog Metadata

Add metadata for blog platforms:

```java
@Test
@BlogMetadata(
    title = "Testing APIs with DTR",
    slug = "testing-apis-dtr",
    tags = {"api", "testing", "java", "documentation"},
    canonicalUrl = "https://mysite.com/api-testing"
)
public void testApiAndPublish() {
    // Test content...
}
```

### Output Locations

```bash
# Generated blog files
target/blog/
├── Testing-APIs-with-DTR.md        # Markdown for platforms
├── meta.yaml                         # Metadata (title, tags, etc.)
├── cover-image.png                  # Featured image (if provided)
└── frontmatter.yml                  # YAML frontmatter for Jekyll
```

### Publish to Dev.to

```bash
# 1. Generate blog markdown
mvnd test -Dtest=YourTest

# 2. Manually publish to Dev.to
# - Create account at dev.to
# - Post the generated Markdown
# - Or use Dev.to API:
curl -X POST https://dev.to/api/articles \
  -H "api-key: your_api_key" \
  -H "Content-Type: application/json" \
  -d @target/blog/meta.yaml
```

---

## 3. OpenAPI Documentation

### Auto-Generate OpenAPI from Tests

```java
@Test
public void documentRestApiAsOpenApi() {
    ctx.sayNextSection("User Management API");

    ctx.say("Get all users:");
    Response getAllUsers = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users"))
    );

    ctx.say("Get a specific user:");
    Response getUser = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users/123"))
    );

    ctx.say("Create a new user:");
    Response createUser = ctx.sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .contentTypeApplicationJson()
            .payload(new User("alice", "alice@example.com"))
    );

    ctx.say("Update a user:");
    Response updateUser = ctx.sayAndMakeRequest(
        Request.PUT()
            .url(testServerUrl().path("/api/users/123"))
            .contentTypeApplicationJson()
            .payload(new User("alice2", "alice2@example.com"))
    );

    ctx.say("Delete a user:");
    Response deleteUser = ctx.sayAndMakeRequest(
        Request.DELETE().url(testServerUrl().path("/api/users/123"))
    );

    // DTR automatically collects OpenAPI spec from requests/responses
}
```

### OpenAPI Output Format

```bash
# Check generated OpenAPI spec
cat target/docs/openapi.json

# Or in YAML format
cat target/docs/openapi.yaml
```

### Example Generated OpenAPI

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "User Management API",
    "version": "1.0.0"
  },
  "paths": {
    "/api/users": {
      "get": {
        "summary": "Get all users",
        "responses": {
          "200": {
            "description": "Success",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "$ref": "#/components/schemas/User"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "summary": "Create a new user",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/User"
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "User": {
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "email": {"type": "string"}
        }
      }
    }
  }
}
```

### Customize OpenAPI Spec

Add descriptions and other metadata:

```java
@Test
@OpenApiInfo(
    title = "User Management API",
    version = "1.0.0",
    description = "Complete API for managing users",
    contact = @Contact(
        name = "API Support",
        email = "api@example.com"
    )
)
public void documentApi() {
    ctx.sayNextSection("Overview");
    ctx.say("This API provides user management operations.");
    // test requests...
}
```

### Verify with Swagger UI

```bash
# 1. Use Swagger Editor online
# - Go to https://editor.swagger.io/
# - Copy-paste content of target/docs/openapi.json
# - Interactive API documentation displays

# Or 2. Run locally
docker run -p 8080:8080 -v $(pwd)/target/docs:/api \
  swaggerapi/swagger-ui
# - Visit http://localhost:8080
# - Specify URL: file:///api/openapi.json
```

---

## 4. HTML Presentations (Reveal.js)

### Generate Slides from Tests

```java
@Test
@SlideFormat  // Generate as Reveal.js presentation
public void presentApiDocumentation() {
    ctx.sayNextSection("REST API Documentation");
    ctx.say("Presenting the User Management API");

    // First slide
    ctx.slideBreak();
    ctx.sayNextSection("Endpoints");
    ctx.say("GET /api/users — List all users");
    ctx.say("POST /api/users — Create user");
    ctx.say("PUT /api/users/{id} — Update user");
    ctx.say("DELETE /api/users/{id} — Delete user");

    // Show request/response example
    ctx.slideBreak();
    ctx.sayNextSection("Example Request");
    Response response = ctx.sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .contentTypeApplicationJson()
            .payload(new User("alice", "alice@example.com"))
    );

    // Final slide
    ctx.slideBreak();
    ctx.sayNextSection("Questions?");
    ctx.say("Thank you for reviewing our API documentation!");
}
```

### Output

```bash
# Generated presentation
target/slides/ApiDocumentation.html

# Open in browser
open target/slides/ApiDocumentation.html
```

### Customize Presentation Theme

```xml
<properties>
    <reveal.theme>black</reveal.theme>  <!-- white, black, league, sky, beige, simple, serif, blood, night, moon, solarized -->
    <reveal.transition>slide</reveal.transition>  <!-- fade, slide, convex, concave, zoom -->
</properties>
```

---

## 5. Multi-Format Output

### Generate All Formats Simultaneously

```java
@Test
public void generateAllFormats() {
    ctx.sayNextSection("Comprehensive API Documentation");

    ctx.say("This test generates documentation in multiple formats:");
    ctx.sayUnorderedList(Arrays.asList(
        "Markdown (GitHub, websites)",
        "PDF (academic papers)",
        "Blog post (Dev.to, Medium)",
        "OpenAPI spec (Swagger UI)",
        "HTML slides (presentations)"
    ));

    Response response = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/data"))
    );

    ctx.say("All formats are generated automatically!");
}
```

### Output Directory Structure

```
target/
├── docs/
│   ├── test-results/
│   │   ├── ApiDocTest.md           # Markdown
│   │   ├── ApiDocTest.tex          # LaTeX source
│   │   ├── ApiDocTest.pdf          # PDF (if LaTeX installed)
│   │   └── ...other formats
│   ├── openapi.json                 # OpenAPI specification
│   └── openapi.yaml
├── blog/
│   ├── Comprehensive-Api-Docs.md   # Blog-ready Markdown
│   └── meta.yaml
└── slides/
    └── ApiDocTest.html             # Reveal.js presentation
```

---

## 6. Custom Rendering

### Create Custom Output Format

For specialized formats (custom HTML, restructuredText, etc.):

```java
public interface CustomRenderer {
    void startSection(String title);
    void addParagraph(String text);
    void addCode(String code, String language);
    void addTable(String[][] data);
    String getOutput();
}

public class MyCustomRenderer implements CustomRenderer {
    // Implement rendering logic
}
```

### Integrate Custom Renderer

```java
@Test
public void renderCustomFormat() {
    CustomRenderer renderer = new MyCustomRenderer();

    ctx.sayNextSection("Custom Format Test");
    // DTR captures all say* calls
    // Post-process with custom renderer if needed

    Response response = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/data"))
    );
}
```

---

## 7. Troubleshooting

### LaTeX/PDF Generation Fails

**Error:** `pdflatex not found`

**Solution:**
```bash
# Install LaTeX
sudo apt-get install texlive-latex-base texlive-latex-extra

# Or macOS
brew install mactex

# Retry build
mvnd clean test
```

### OpenAPI Spec is Incomplete

**Issue:** Some endpoints missing or schema is wrong

**Solution:**
1. Ensure all important fields are present in test payloads
2. Document edge cases in test comments
3. Manually refine generated spec if needed:
   ```bash
   edit target/docs/openapi.json
   ```

### Blog Metadata Not Publishing

**Issue:** Tags or title not working on platform

**Solution:**
1. Check platform's supported metadata fields
2. Use standard Markdown frontmatter:
   ```yaml
   ---
   title: My Article
   tags: [api, testing, java]
   published: true
   ---
   ```

---

## Best Practices

### 1. One Test = One Document

Keep tests focused on one aspect:

```java
// ✅ GOOD: One test, one focused document
@Test
public void documentUserApiCreate() { ... }

@Test
public void documentUserApiRead() { ... }

// ❌ BAD: One test, too many concerns
@Test
public void documentWholeApi() { ... }
```

### 2. Include Descriptive Text

LaTeX/PDF output quality depends on content:

```java
ctx.say("The user creation endpoint accepts POST requests with a JSON body " +
        "containing name and email fields. Successful requests return 201 Created " +
        "with the new user's ID.");
```

### 3. Use Meaningful Section Titles

These become document structure:

```java
ctx.sayNextSection("User Management API"); // Good
ctx.sayNextSection("Test 1");               // Bad
```

### 4. Document Examples

Include realistic examples for each operation:

```java
ctx.say("Example request:");
ctx.sayCode("""
    {
      "name": "Alice",
      "email": "alice@example.com"
    }
    """, "json");

ctx.say("Example response:");
Response response = ctx.sayAndMakeRequest(...);
```

---

## Next Steps

- See [Benchmarking](benchmarking.md) to measure output generation performance
- See [Customize HTML Output](customize-html-output.md) for HTML styling
- See [Reference: RenderMachine API](../reference/rendermachine-api.md) for all rendering methods

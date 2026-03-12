# Architecture and Design

This document provides a deep technical overview of DTR (Documentation Testing Runtime), covering its internal structure, component design, and extension patterns. It's intended for Java developers who want to understand how DTR works or extend it with custom functionality.

---

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Module Structure](#module-structure)
3. [Core Components](#core-components)
4. [Request/Response Pipeline](#requestresponse-pipeline)
5. [Documentation Generation](#documentation-generation)
6. [Java 25 Features Used](#java-25-features-used)
7. [Design Patterns](#design-patterns)
8. [Extension Points](#extension-points)

---

## System Architecture Overview

DTR's architecture follows a three-layer model: **Test Execution**, **Capture and Rendering**, and **Output Generation**.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Test Code                                │
│  @Test void testGetUsers(DocTesterContext ctx) { ... }          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ calls say* methods
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RenderMachine Pipeline                        │
│  - Captures documentation calls (sayCode, sayJson, etc.)        │
│  - Routes to format-specific rendering (Markdown, LaTeX, etc.)  │
│  - Manages TestBrowser for HTTP interactions                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ accumulates content
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Output Generation                            │
│  - Assembles document metadata and content                      │
│  - Writes Markdown, LaTeX, PDF, HTML, OpenAPI specs            │
│  - Updates indexes, table of contents, bibliographies           │
└─────────────────────────────────────────────────────────────────┘
```

### High-Level Flow

1. **JUnit 5 Test Execution**: DocTesterExtension registers before each test method.
2. **Context Injection**: DocTesterContext is injected into test methods, providing access to say* methods and TestBrowser.
3. **Documentation Capture**: Each say* call is routed to the active RenderMachine implementation.
4. **HTTP Intercept**: sayAndMakeRequest() executes the request via TestBrowser AND documents the exchange.
5. **Output Finalization**: After all tests in a class complete, RenderMachine.finishAndWriteOut() writes documentation to disk.

---

## Module Structure

DTR is a Maven multi-module project with clear separation of concerns:

```
dtr/
├── pom.xml                           # Parent POM (versions, Java 25 release)
│
├── dtr-core/                         # Core library (implements DTR)
│   ├── src/main/java/io/github/seanchatmangpt/dtr/
│   │   ├── junit5/                   # JUnit 5 integration
│   │   │   ├── DocTesterExtension.java
│   │   │   ├── DocTesterContext.java
│   │   │   └── DocTesterCommands.java
│   │   │
│   │   ├── rendermachine/            # Output format implementations
│   │   │   ├── RenderMachine.java                    (abstract base)
│   │   │   ├── RenderMachineImpl.java                (Markdown)
│   │   │   ├── RenderMachineCommands.java           (say* interface)
│   │   │   │
│   │   │   ├── latex/
│   │   │   │   ├── RenderMachineLatex.java          (LaTeX/PDF)
│   │   │   │   ├── LatexTemplate.java               (sealed)
│   │   │   │   ├── ArXivTemplate.java
│   │   │   │   ├── ACMTemplate.java
│   │   │   │   ├── CompilerStrategy.java            (sealed)
│   │   │   │   ├── LatexmkStrategy.java
│   │   │   │   └── PdflatexStrategy.java
│   │   │   │
│   │   │   └── render/
│   │   │       ├── blog/
│   │   │       │   ├── BlogRenderMachine.java       (Markdown for blogs)
│   │   │       │   └── BlogTemplate.java
│   │   │       └── slides/
│   │   │           ├── SlideRenderMachine.java      (Reveal.js HTML5)
│   │   │           └── SlideTemplate.java
│   │   │
│   │   ├── testbrowser/              # HTTP client & request/response
│   │   │   ├── TestBrowser.java                     (interface)
│   │   │   ├── TestBrowserImpl.java                  (Apache HttpClient 5)
│   │   │   ├── Request.java                         (fluent builder)
│   │   │   ├── Response.java                        (with deserialization)
│   │   │   ├── Url.java                             (URL builder)
│   │   │   │
│   │   │   └── auth/
│   │   │       ├── AuthProvider.java                (sealed interface)
│   │   │       ├── BasicAuth.java
│   │   │       ├── BearerTokenAuth.java
│   │   │       ├── ApiKeyAuth.java
│   │   │       ├── OAuth2TokenManager.java
│   │   │       └── SessionAwareAuthProvider.java
│   │   │
│   │   ├── assembly/                 # Document composition
│   │   │   ├── DocumentAssembler.java               (aggregates .tex files)
│   │   │   ├── AssemblyManifest.java                (record)
│   │   │   ├── TableOfContents.java
│   │   │   ├── IndexBuilder.java
│   │   │   └── WordCounter.java
│   │   │
│   │   ├── openapi/                  # OpenAPI spec generation
│   │   │   ├── OpenApiCollector.java
│   │   │   ├── OpenApiSpec.java
│   │   │   └── OpenApiWriter.java
│   │   │
│   │   ├── crossref/                 # Cross-references & bibliography
│   │   │   ├── CrossReferenceIndex.java             (singleton)
│   │   │   ├── DocTestRef.java                      (record)
│   │   │   └── ReferenceResolver.java
│   │   │
│   │   ├── bibliography/
│   │   │   ├── BibliographyManager.java
│   │   │   └── BibTeXRenderer.java
│   │   │
│   │   ├── metadata/                 # Build context & runtime info
│   │   │   ├── DocMetadata.java                     (record, cached)
│   │   │   └── ...
│   │   │
│   │   ├── config/
│   │   │   └── RenderConfig.java                    (configuration record)
│   │   │
│   │   ├── reflectiontoolkit/        # Introspection utilities
│   │   │   ├── ClassHierarchy.java
│   │   │   ├── AnnotationProfile.java
│   │   │   ├── ReflectiveDiff.java
│   │   │   └── CallSiteRecord.java                  (record)
│   │   │
│   │   └── sse/                      # Server-Sent Events support
│   │       ├── SseClient.java                       (interface)
│   │       ├── SseClientImpl.java
│   │       ├── SseEvent.java                        (record)
│   │       └── SseSubscription.java
│   │
│   └── src/test/java/               # DTR test suite
│       └── ...DocTest.java
│
├── dtr-integration-test/             # Full-stack integration tests
│   └── src/test/java/ApiControllerDocTest.java
│
├── dtr-benchmarks/                   # Performance benchmarks
│   └── src/main/java/DocMetadataBenchmarkRunner.java
│
└── doctester-cli/                    # Command-line interface (future)
    └── ...
```

### Module Dependencies

| Module | Purpose | Scope |
|--------|---------|-------|
| **dtr-core** | Main library; no runtime dependencies beyond test frameworks | compile |
| **dtr-integration-test** | Full-stack tests with real HTTP server | test |
| **dtr-benchmarks** | Performance benchmarks (optional) | test |

---

## Core Components

### 1. RenderMachine Class Hierarchy

**RenderMachine** is the abstract base class for all output renderers. It defines the contract for documentation generation and provides template method hooks.

```java
// Simplified hierarchy
public abstract class RenderMachine implements RenderMachineCommands {
    public abstract void setTestBrowser(TestBrowser testBrowser);
    public abstract void setFileName(String fileName);
    public abstract void finishAndWriteOut();

    // Template method hooks for format-specific content
    public void saySlideOnly(String text) { }
    public void sayDocOnly(String text) { say(text); }
    public void saySpeakerNote(String text) { }
    public void sayHeroImage(String altText) { }
    public void sayTweetable(String text) { }
    public void sayTldr(String text) { }
    public void sayCallToAction(String url) { }
}
```

**Design Note on Sealed Classes**: RenderMachine is NOT sealed due to Java modular constraints. Sealed classes in Java 25 require all permitted subclasses to be in the same package or module. Since RenderMachine implementations span multiple packages (rendermachine, rendermachine.latex, render.blog, render.slides), the class uses standard inheritance with all implementations marked `final` to maintain single inheritance and enable JIT devirtualization.

### 2. RenderMachineImpl (Markdown Output)

Converts test calls into Markdown suitable for GitHub, static site generators, and documentation platforms.

```java
public final class RenderMachineImpl extends RenderMachine {
    private final List<String> markdownDocument = new ArrayList<>();
    private final List<String> sections = new ArrayList<>();
    private final List<String> toc = new ArrayList<>();

    @Override
    public void sayNextSection(String heading) {
        sections.add(heading);
        String anchorId = convertTextToId(heading);
        toc.add("- [%s](#%s)".formatted(heading, anchorId));
        markdownDocument.add("");
        markdownDocument.add("## " + heading);
    }

    @Override
    public void finishAndWriteOut() {
        // Write to docs/test/*.md
    }
}
```

**Key Features**:
- Accumulates content in `List<String>` for line-by-line assembly
- Generates table of contents with anchor IDs
- Escapes special characters for Markdown safety
- Writes output to `docs/test/` directory

### 3. RenderMachineLatex (LaTeX/PDF Output)

Generates publication-ready LaTeX documents compilable to PDF via pdflatex, latexmk, or xelatex.

```java
public final class RenderMachineLatex extends RenderMachine {
    private final LatexTemplate template;  // sealed interface
    private final DocMetadata metadata;
    private final List<String> texDocument;

    @Override
    public void say(String text) {
        texDocument.add(template.escapeLatex(text));
    }

    @Override
    public void finishAndWriteOut() {
        // Generate .tex file, compile to PDF
    }
}
```

**Sealed Template System**: Uses sealed interface `LatexTemplate` with implementations:
- **ArXivTemplate**: arXiv.org preprint format
- **ACMTemplate**: ACM conference format (SIGPLAN, TODS, etc.)
- **IEEETemplate**: IEEE conference format
- **NatureTemplate**: Nature journal format
- **UsPatentTemplate**: US Patent office format

This sealed design enables exhaustive pattern matching and JIT optimization for template dispatch.

### 4. BlogRenderMachine (Blog/Social Media)

Generates blog-post markdown optimized for Dev.to, Medium, Substack, LinkedIn, and Hashnode, with social media queue entries.

```java
public final class BlogRenderMachine extends RenderMachine {
    private final BlogTemplate template;
    private final List<String> tweetables = new ArrayList<>();
    private String tldr = "";
    private String cta = "";

    @Override
    public void sayTweetable(String text) {
        tweetables.add(text.substring(0, Math.min(280, text.length())));
    }

    @Override
    public void sayTldr(String text) {
        this.tldr = text;
    }
}
```

### 5. SlideRenderMachine (Reveal.js HTML5 Presentations)

Generates Reveal.js-based HTML5 presentations from test documentation, with speaker notes and presenter mode.

### 6. TestBrowser & TestBrowserImpl

**TestBrowser** is the interface for HTTP client functionality:

```java
public interface TestBrowser {
    Response makeRequest(Request request);
    List<Cookie> getCookies();
    Cookie getCookieWithName(String name);
    void clearCookies();
}
```

**TestBrowserImpl** wraps Apache HttpClient 5:

```java
public final class TestBrowserImpl implements TestBrowser {
    private final CloseableHttpClient httpClient;
    private final BasicCookieStore cookieStore;
    private final TestBrowserConfig config;

    @Override
    public Response makeRequest(Request request) {
        // Dispatch to GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD
        // Serialize payload (JSON, XML, form data)
        // Handle redirects, cookies, auth
        // Deserialize response body
    }
}
```

---

## Request/Response Pipeline

### 1. Request Construction (Builder Pattern)

DTR uses the **builder pattern** for fluent request construction:

```java
Request request = Request.POST()
    .url(ctx.testServerUrl().path("/api/users"))
    .payload(new User("Alice", "alice@example.com"))
    .header("X-API-Key", "secret123");

Response response = ctx.sayAndMakeRequest(request);
```

**Request Class Structure**:

```java
public class Request {
    public String httpRequestType;        // GET, POST, PUT, DELETE, etc.
    public URI uri;
    public Map<String, String> headers;
    public Map<String, String> formParameters;
    public Object payload;               // Auto-serialized to JSON
    public Map<String, File> filesToUpload;
    public boolean followRedirects;

    public static Request GET() { ... }
    public static Request POST() { ... }
    public static Request PUT() { ... }
    public static Request DELETE() { ... }

    public Request url(Url url) { ... }
    public Request url(String url) { ... }
    public Request header(String key, String value) { ... }
    public Request payload(Object object) { ... }
    public Request formParameter(String key, String value) { ... }
}
```

**Url Builder**:

```java
Url url = Url.host("http://localhost:8080")
    .path("/api/v1/users")
    .queryParameter("page", "1")
    .queryParameter("size", "10");

String fullUrl = url.toString();  // http://localhost:8080/api/v1/users?page=1&size=10
```

### 2. Response Capturing and Deserialization

**Response** captures the full HTTP exchange with automatic deserialization:

```java
public class Response {
    public int httpStatus;
    public String contentType;
    public Map<String, String> headers;
    public String body;
    public byte[] bodyAsBytes;
    public Object payload;  // Deserialized JSON or XML

    // Convenience methods
    public <T> T payloadAs(Class<T> type) { ... }
    public JsonNode bodyAsJsonNode() { ... }
}
```

### 3. Documentation Integration

When `ctx.sayAndMakeRequest(request)` is called:

1. **Execute**: TestBrowser makes the actual HTTP request
2. **Capture**: Response is captured with full headers, body, status code
3. **Document**: RenderMachine formats the request/response for documentation
4. **Return**: Response object is returned for assertions

```java
@Override
public Response sayAndMakeRequest(Request httpRequest) {
    // 1. Make request via TestBrowser
    Response response = testBrowser.makeRequest(httpRequest);

    // 2. Document the exchange
    renderMachine.sayAndMakeRequest(httpRequest);  // Formats request

    // 3. Return for assertions
    return response;
}
```

---

## Documentation Generation

### 1. Say* Methods (RenderMachineCommands Interface)

The `RenderMachineCommands` interface defines all documentation methods:

| Method | Purpose | Example Output (Markdown) |
|--------|---------|---------------------------|
| `say(String text)` | Paragraph text | `<paragraph>` |
| `sayNextSection(String heading)` | Section header | `## Heading` |
| `sayCode(String code, String language)` | Code block with syntax highlighting | `` ``` `` |
| `sayTable(String[][] data)` | Markdown table | `\| Col1 \| Col2 \|` |
| `sayJson(Object object)` | Pretty-printed JSON | `{` with indentation |
| `sayWarning(String message)` | Warning/alert box | `> **Warning**:` |
| `sayNote(String message)` | Note/info box | `> **Note**:` |
| `sayKeyValue(Map<String, String> pairs)` | Key-value pairs | Table with 2 columns |
| `sayUnorderedList(List<String> items)` | Bullet list | `- Item 1` |
| `sayOrderedList(List<String> items)` | Numbered list | `1. Item 1` |
| `sayAndMakeRequest(Request)` | HTTP request + response | Request/response blocks |
| `sayAndAssertThat(message, actual, matcher)` | Assertion + result | Formatted assertion |
| `sayCite(citationKey)` | Bibliography reference | `\cite{key}` (LaTeX) or link |
| `sayFootnote(String text)` | Footnote | `[^1]: Text` (Markdown) |
| `sayRef(DocTestRef)` | Cross-reference | `\ref{label}` (LaTeX) |
| `sayCodeModel(Class<?> clazz)` | Class structure via reflection | Table of fields/methods |
| `sayCallSite()` | Current stack trace location | File, line number, method |

### 2. Output Format Selection and Routing

RenderMachine implementations handle format-specific rendering:

**Markdown (RenderMachineImpl)**:
```java
@Override
public void sayCode(String code, String language) {
    markdownDocument.add("```" + language);
    markdownDocument.add(code);
    markdownDocument.add("```");
}
```

**LaTeX (RenderMachineLatex)**:
```java
@Override
public void sayCode(String code, String language) {
    texDocument.add("\\begin{listing}[H]");
    texDocument.add("\\begin{minted}{" + language + "}");
    texDocument.add(template.escapeLatex(code));
    texDocument.add("\\end{minted}");
    texDocument.add("\\end{listing}");
}
```

**Blog (BlogRenderMachine)**:
```java
@Override
public void sayCode(String code, String language) {
    // Platform-specific syntax highlighting markers (Dev.to, Medium)
    buffer.append("```" + language + "\n");
    buffer.append(code + "\n");
    buffer.append("```\n");
}
```

### 3. Cross-Reference System

**CrossReferenceIndex** manages all cross-references between DocTests:

```java
public class CrossReferenceIndex {
    private static CrossReferenceIndex instance;
    private final List<DocTestRef> registeredReferences;
    private final ReferenceResolver resolver;

    public void registerReference(DocTestRef ref) {
        registeredReferences.add(ref);
    }

    public String resolveLabel(String key) {
        // Returns \label{key} for LaTeX or anchor for Markdown
    }
}
```

**DocTestRef** is a record capturing reference metadata:

```java
public record DocTestRef(
    String testClassName,
    String methodName,
    int lineNumber,
    String label,
    String pageRef
) {}
```

Usage in tests:

```java
@Test
void testUserCreation(DocTesterContext ctx) {
    ctx.sayNextSection("Create User");
    // ... make request ...
    ctx.sayRef(new DocTestRef(..., "sec:create-user", "1"));
}

@Test
void testUserRetrieval(DocTesterContext ctx) {
    ctx.sayCite("sec:create-user");  // Links to previous test
}
```

### 4. Bibliography Management

**BibliographyManager** manages BibTeX citations in LaTeX output:

```java
BibliographyManager bibManager = BibliographyManager.getInstance();
bibManager.addEntry(new BibTeXEntry(
    "Knuth1984",
    "book",
    Map.of(
        "author", "Donald E. Knuth",
        "title", "The TeXbook",
        "year", "1984"
    )
));

// In LaTeX output
texDocument.add("\\cite{Knuth1984}");
```

### 5. OpenAPI Spec Generation

**OpenApiCollector** observes HTTP interactions and generates OpenAPI 3.1 specs:

```java
OpenApiCollector collector = new OpenApiCollector("My API", "1.0.0");

@Test
void testGetUsers(DocTesterContext ctx) {
    Request request = Request.GET().url(ctx.testServerUrl().path("/users"));
    Response response = ctx.sayAndMakeRequest(request);

    // Automatically recorded by OpenApiCollector
    // Generates endpoint in OpenAPI spec
}

// Export
OpenApiSpec spec = collector.build();
String json = spec.toJson();
String yaml = spec.toYaml();
```

---

## Java 25 Features Used

### 1. Sealed Classes/Interfaces (JEP 409)

**AuthProvider** (sealed interface):
```java
public sealed interface AuthProvider
    permits BasicAuth, BearerTokenAuth, ApiKeyAuth, OAuth2TokenManager, SessionAwareAuthProvider {

    Request apply(Request request);
}

// Usage with pattern matching
Request authenticated = switch (authProvider) {
    case BasicAuth ba -> ba.apply(request);
    case BearerTokenAuth bta -> bta.apply(request);
    case ApiKeyAuth aka -> aka.apply(request);
    case OAuth2TokenManager otm -> otm.apply(request);
    case SessionAwareAuthProvider sap -> sap.apply(request);
};
```

**CompilerStrategy** (sealed interface for LaTeX compilation):
```java
public sealed interface CompilerStrategy
    permits PdflatexStrategy, XelatexStrategy, LatexmkStrategy, PandocStrategy {

    boolean isAvailable();
    void compile(Path texFile, Path outputDir) throws IOException, InterruptedException;
}
```

**Benefits**:
- Exhaustive pattern matching without `default` cases
- JIT compiler can devirtualize method calls (no pointer indirection)
- Enables static analysis and compiler optimizations
- Preparation for Valhalla value class flattening

### 2. Records

**DocMetadata** (JEP 395):
```java
public record DocMetadata(
    String projectName,
    String projectVersion,
    String buildTimestamp,  // ISO 8601
    String javaVersion,
    String mavenVersion,
    String gitCommit,
    String gitBranch,
    String gitAuthor,
    String hostname
) {}
```

**Benefits**:
- Immutable data carrier class (no boilerplate getters/setters)
- Compact constructor for validation
- `equals()`, `hashCode()`, `toString()` generated automatically
- Cached globally to avoid repeated metadata collection (500ms-2.5s savings)

**DocTestRef**:
```java
public record DocTestRef(
    String testClassName,
    String methodName,
    int lineNumber,
    String label,
    String pageRef
) {}
```

**SseEvent** (Server-Sent Events):
```java
public record SseEvent(
    String id,
    String event,
    String data,
    long retry
) {}
```

**AssemblyManifest** (document composition):
```java
public record AssemblyManifest(
    List<Path> texFiles,
    int totalPages,
    int totalWords,
    int totalCodeListings,
    int totalTables,
    int totalCitations,
    int totalCrossReferences,
    Instant generatedAt
) {}
```

### 3. Pattern Matching

**Request dispatch in TestBrowserImpl**:
```java
String method = request.httpRequestType;

// Traditional dispatch (pre-Java 21)
if ("GET".equals(method)) {
    // ...
} else if ("POST".equals(method)) {
    // ...
}

// Pattern matching (Java 21+)
// Note: Request.httpRequestType would ideally be a sealed interface
// to enable full pattern matching exhaustiveness
```

### 4. Text Blocks (JEP 355)

**LaTeX template definitions**:
```java
String latexTemplate = """
    \\documentclass{article}
    \\usepackage{listings}
    \\usepackage{xcolor}

    \\begin{document}
    \\title{%s}
    \\author{Generated by DTR}
    \\maketitle

    %s

    \\end{document}
    """;
```

**Benefits**:
- Readable multi-line strings without escape characters
- Preserves indentation
- No extra concatenation overhead

### 5. `var` Keyword (Local Variable Type Inference)

Throughout codebase:
```java
var request = Request.GET().url("...");
var response = ctx.sayAndMakeRequest(request);
var metadata = DocMetadata.capture();
var sections = new ArrayList<String>();
```

---

## Design Patterns

### 1. Builder Pattern

**Request & Url builders** enable fluent API construction:

```java
Request request = Request.POST()
    .url(Url.host("http://api.example.com")
        .path("/v1/users")
        .queryParameter("format", "json"))
    .header("Authorization", "Bearer token...")
    .payload(new CreateUserRequest("Alice"));
```

**RenderMachine construction**:
```java
RenderMachine md = new RenderMachineImpl();
RenderMachine latex = new RenderMachineLatex(new ArXivTemplate(), metadata);
RenderMachine blog = new BlogRenderMachine(new DevToTemplate());
```

### 2. Strategy Pattern

**CompilerStrategy** for LaTeX compilation:

```java
public sealed interface CompilerStrategy {
    void compile(Path texFile, Path outputDir) throws IOException, InterruptedException;
}

// Implementations
public final class LatexmkStrategy implements CompilerStrategy { ... }
public final class PdflatexStrategy implements CompilerStrategy { ... }
public final class XelatexStrategy implements CompilerStrategy { ... }
public final class PandocStrategy implements CompilerStrategy { ... }

// Usage: Try strategies in order of availability
CompilerStrategy compiler = new LatexmkStrategy();
if (!compiler.isAvailable()) {
    compiler = new PdflatexStrategy();
}
compiler.compile(texFile, outputDir);
```

**AuthProvider** strategy:

```java
public sealed interface AuthProvider {
    Request apply(Request request);
}

// Different authentication strategies
var basicAuth = new BasicAuth("user", "pass");
var tokenAuth = new BearerTokenAuth("jwt...");
var apiKeyAuth = new ApiKeyAuth("X-API-Key", "secret");

request = basicAuth.apply(request);
```

**LatexTemplate** strategy:

```java
public sealed interface LatexTemplate {
    String formatSection(String heading);
    String formatTable(String[][] data);
    String escapeLatex(String text);
}

// Format-specific implementations
var arxiv = new ArXivTemplate();
var ieee = new IEEETemplate();
var acm = new ACMTemplate();

texDocument.add(selectedTemplate.formatSection(heading));
```

### 3. Template Method Pattern

**RenderMachine base class** defines the template for subclasses:

```java
public abstract class RenderMachine implements RenderMachineCommands {

    // Hook methods for subclass customization
    public void saySlideOnly(String text) { }
    public void sayDocOnly(String text) { say(text); }
    public void sayHeroImage(String altText) { }
    public void sayTweetable(String text) { }

    // Concrete template method (orchestrates the flow)
    public void finishAndWriteOut() {
        // Subclasses override to implement format-specific output
        // e.g., write Markdown, LaTeX, HTML, etc.
    }
}
```

Each subclass implements the hooks for its specific format:

```java
public final class RenderMachineLatex extends RenderMachine {
    @Override
    public void saySlideOnly(String text) {
        // Ignore for LaTeX output
    }

    @Override
    public void finishAndWriteOut() {
        // Write .tex file, optionally compile to PDF
    }
}

public final class SlideRenderMachine extends RenderMachine {
    @Override
    public void saySlideOnly(String text) {
        // Include in slide output
        buffer.append(text);
    }

    @Override
    public void saySpeakerNote(String text) {
        // Include speaker notes
        speakerNotes.add(text);
    }
}
```

### 4. Extension Pattern (JUnit 5)

**DocTesterExtension** implements JUnit 5's extension model:

```java
public class DocTesterExtension implements BeforeEachCallback, AfterAllCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        // Inject RenderMachine and TestBrowser into context
        var renderMachine = getOrCreateRenderMachine(context);
        var testBrowser = new TestBrowserImpl();

        // Make available to test method
        context.getStore(NAMESPACE).put(TEST_BROWSER_KEY, testBrowser);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Finalize documentation after all tests
        var renderMachine = getRenderMachine(context);
        renderMachine.finishAndWriteOut();
    }
}
```

**DocTesterContext** acts as a facade for test methods:

```java
@Test
void testApiEndpoint(DocTesterContext ctx) {
    // ctx injected by DocTesterExtension
    ctx.sayNextSection("GET /users");
    var response = ctx.sayAndMakeRequest(Request.GET()...);
    ctx.sayAndAssertThat("Status", response.httpStatus, is(200));
}
```

---

## Extension Points

### 1. Custom RenderMachine Implementation

Create a custom render machine for unsupported output formats (e.g., AsciiDoc, reStructuredText):

```java
public final class RenderMachineAsciiDoc extends RenderMachine {
    private final StringBuilder adocDocument = new StringBuilder();

    @Override
    public void say(String text) {
        adocDocument.append(text).append("\n");
    }

    @Override
    public void sayNextSection(String heading) {
        adocDocument.append("\n== ").append(heading).append("\n");
    }

    @Override
    public void sayCode(String code, String language) {
        adocDocument.append("\n[source,").append(language).append("]\n");
        adocDocument.append("----\n");
        adocDocument.append(code).append("\n");
        adocDocument.append("----\n");
    }

    @Override
    public void finishAndWriteOut() {
        // Write to docs/test/*.adoc
        Path outputPath = Paths.get("docs/test/" + fileName + ".adoc");
        Files.writeString(outputPath, adocDocument.toString());
    }

    @Override
    public void setTestBrowser(TestBrowser testBrowser) {
        this.testBrowser = testBrowser;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
```

### 2. Custom AuthProvider

Implement sealed interface `AuthProvider` for custom authentication:

```java
public final class OAuthClientCredentialsAuth implements AuthProvider {
    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;

    @Override
    public Request apply(Request request) {
        String token = fetchAccessToken();
        request.header("Authorization", "Bearer " + token);
        return request;
    }

    private String fetchAccessToken() {
        // Implementation using client credentials flow
    }
}

// Usage
var auth = new OAuthClientCredentialsAuth(...);
Request authenticated = auth.apply(request);
```

### 3. Custom LatexTemplate

Implement sealed interface `LatexTemplate` for publication-specific formatting:

```java
public final class CustomPublisherTemplate implements LatexTemplate {

    @Override
    public String formatSection(String heading) {
        return "\\section{" + escapeLatex(heading) + "}";
    }

    @Override
    public String formatTable(String[][] data) {
        // Custom table formatting for publisher requirements
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{table}[H]\n");
        // ... build table ...
        sb.append("\\end{table}\n");
        return sb.toString();
    }

    @Override
    public String escapeLatex(String text) {
        // Publisher-specific character escaping
    }
}

// Usage
var template = new CustomPublisherTemplate();
var renderMachine = new RenderMachineLatex(template, metadata);
```

### 4. Custom CompilerStrategy

Implement sealed interface `CompilerStrategy` for alternative LaTeX compilers:

```java
public final class TinyTexStrategy implements CompilerStrategy {

    @Override
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("tlmgr", "--version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void compile(Path texFile, Path outputDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "pdflatex",
            "-interaction=nonstopmode",
            "-output-directory=" + outputDir,
            texFile.toString()
        );
        Process process = pb.start();
        if (process.waitFor() != 0) {
            throw new IOException("TinyTeX compilation failed");
        }
    }
}
```

### 5. Plugging Into DocTesterExtension

Override extension configuration in test class:

```java
@ExtendWith(DocTesterExtension.class)
public class MyDocTest {

    @Test
    void testWithCustomRendering(DocTesterContext ctx) {
        // Use the injected context (which uses default RenderMachineImpl)
        ctx.sayNextSection("My Test");
        ctx.say("Documentation");
    }

    // To use custom RenderMachine, extend DocTesterExtension
}

// Custom extension to use AsciiDoc renderer
public class AsciiDocExtension extends DocTesterExtension {

    @Override
    protected RenderMachine createRenderMachine(ExtensionContext context) {
        return new RenderMachineAsciiDoc();
    }
}

@ExtendWith(AsciiDocExtension.class)
public class MyAsciiDocTest {

    @Test
    void testWithAsciiDoc(DocTesterContext ctx) {
        ctx.sayNextSection("AsciiDoc Test");
        ctx.say("This renders to AsciiDoc!");
    }
}
```

---

## Summary

DTR's architecture emphasizes:

1. **Separation of Concerns**: RenderMachine handles output format; TestBrowser handles HTTP; DocTesterContext provides the facade
2. **Extensibility**: Sealed interfaces (AuthProvider, CompilerStrategy, LatexTemplate) enable exhaustive pattern matching and safe subclassing
3. **Type Safety**: Records (DocMetadata, DocTestRef, AssemblyManifest) eliminate boilerplate and capture invariants
4. **Testability**: Dependency injection through DocTesterContext allows mocking and custom implementations
5. **Performance**: Metadata caching, sealed class devirtualization, and minimal runtime dependencies
6. **Java 25 Alignment**: Uses records, sealed classes, pattern matching, and text blocks for modern, maintainable code

This design makes DTR a flexible foundation for generating documentation from test execution while maintaining clean, understandable code.

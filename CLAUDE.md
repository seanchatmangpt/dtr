# DocTester — Claude Code Project Guide

## Project Overview

DocTester is a comprehensive **Markdown documentation generator** for Java 25 that creates living documentation from test execution. It generates multiple output formats (Markdown, HTML, LaTeX/PDF, blog posts, presentations, OpenAPI specs, WebSockets, Server-Sent Events) while running JUnit 4/5 tests. It provides a fluent API for making HTTP requests, asserting responses, and automatically rendering the results as living documentation.

**Current version:** `2.5.0-SNAPSHOT`
**License:** Apache 2.0
**Maven coordinates:** `org.r10r:doctester-core`

**Toolchain (non-negotiable):**
- **Java 25** (LTS) — must use `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`
- **Maven 4** — use `mvn` (Maven 4.0.0-rc-5+) or `mvnd` (Maven Daemon) for all builds
- **mvnd 2** — preferred over plain `mvn` for speed; installed at `/opt/mvnd/bin/mvnd`
- **`--enable-preview`** — always active for both compilation and test runs
- **JUnit 4 and JUnit 5** — both supported; JUnit 5 via `DocTesterExtension`

---

## Module Structure

```
doctester/
├── .claude/agents/                    # Custom Claude sub-agent profiles
│   ├── java-25-expert.md             # Java 25 modernization agent
│   └── maven-build-expert.md         # Maven 4 / mvnd build agent
├── .mvn/
│   ├── maven.config                  # Persistent flags: --no-transfer-progress --batch-mode --enable-preview
│   └── wrapper/maven-wrapper.properties  # DO NOT USE — downloads Maven 3
├── doctester-core/                   # Core library (JAR artifact)
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/r10r/doctester/
│       │   ├── DocTester.java            # Abstract base class; entry point for test authors
│       │   ├── DocCode.java              # Code block documentation element
│       │   ├── DocSection.java           # Section documentation element
│       │   ├── DocWarning.java           # Warning/alert element
│       │   ├── DocNote.java              # Note/tip element
│       │   ├── DocDescription.java       # Description element
│       │   ├── assembly/                 # Document assembly & indexing
│       │   │   ├── DocumentAssembler.java    # Combine test docs into final output
│       │   │   ├── IndexBuilder.java        # Generate table of contents
│       │   │   ├── TableOfContents.java
│       │   │   ├── WordCounter.java
│       │   │   └── AssemblyManifest.java
│       │   ├── bibliography/             # Citation & bibliography management
│       │   │   ├── BibTeXRenderer.java       # Generate BibTeX format
│       │   │   ├── BibliographyManager.java
│       │   │   ├── BibTeXEntry.java
│       │   │   ├── CitationKey.java
│       │   │   └── UnknownCitationException.java
│       │   ├── config/                   # Configuration management
│       │   │   └── RenderConfig.java         # Output format & rendering options
│       │   ├── crossref/                 # Cross-reference & linking
│       │   │   ├── CrossReferenceIndex.java
│       │   │   ├── DocTestRef.java
│       │   │   ├── ReferenceResolver.java
│       │   │   └── InvalidAnchorException.java
│       │   ├── junit5/                   # JUnit 5 extension support
│       │   │   ├── DocTesterExtension.java   # Main JUnit 5 extension
│       │   │   ├── DocTesterContext.java     # Test context management
│       │   │   └── DocTesterCommands.java    # Command API for extensions
│       │   ├── metadata/                 # Document metadata
│       │   │   └── DocMetadata.java
│       │   ├── openapi/                  # OpenAPI/Swagger generation
│       │   │   ├── OpenApiCollector.java     # Collect API metadata from tests
│       │   │   ├── OpenApiSpec.java          # OpenAPI specification model
│       │   │   ├── OpenApiWriter.java        # Write OpenAPI JSON/YAML
│       │   │   └── OutputFormat.java         # Format enumeration (JSON/YAML)
│       │   ├── receipt/                  # Cryptographic receipt generation
│       │   │   ├── LockchainReceipt.java     # Blockchain receipt with hash chain
│       │   │   └── ReceiptGenerator.java     # Generate & embed receipts in LaTeX
│       │   ├── reflectiontoolkit/        # Reflection-based analysis & diffing
│       │   │   ├── AnnotationProfile.java
│       │   │   ├── CallSiteRecord.java
│       │   │   ├── ClassHierarchy.java
│       │   │   ├── ReflectiveDiff.java       # Compare objects structurally
│       │   │   └── StringMetrics.java        # Similarity metrics for strings
│       │   ├── render/                   # Multi-format render engine
│       │   │   ├── RenderMachineFactory.java # Factory for render machines
│       │   │   ├── blog/                     # Social media / blog export
│       │   │   │   ├── BlogRenderMachine.java
│       │   │   │   ├── DevToTemplate.java    # Dev.to format
│       │   │   │   ├── HashnodeTemplate.java # Hashnode format
│       │   │   │   ├── LinkedInTemplate.java # LinkedIn format
│       │   │   │   ├── MediumTemplate.java   # Medium format
│       │   │   │   ├── SubstackTemplate.java # Substack format
│       │   │   │   ├── SocialQueueEntry.java
│       │   │   │   └── SocialQueueWriter.java
│       │   │   ├── slides/                   # Presentation/slides output
│       │   │   │   ├── SlideRenderMachine.java
│       │   │   │   ├── RevealJsTemplate.java # Reveal.js template
│       │   │   │   └── SlideTemplate.java
│       │   │   └── latex/                    # LaTeX/PDF output with academic templates
│       │   │       ├── RenderMachineLatex.java
│       │   │       ├── LatexTemplate.java    # Base template interface
│       │   │       ├── LatexCompiler.java    # LaTeX → PDF compilation
│       │   │       ├── CompilerStrategy.java # Compiler selection (pdflatex/xelatex/lualatex)
│       │   │       ├── PdflatexStrategy.java
│       │   │       ├── XelatexStrategy.java
│       │   │       ├── LatexmkStrategy.java
│       │   │       ├── PandocStrategy.java
│       │   │       ├── ACMTemplate.java      # ACM conference format
│       │   │       ├── ArXivTemplate.java    # arXiv preprint format
│       │   │       ├── IEEETemplate.java     # IEEE journal format
│       │   │       ├── NatureTemplate.java   # Nature magazine format
│       │   │       └── UsPatentTemplate.java # US patent document format
│       │   ├── rendermachine/            # Core documentation generator
│       │   │   ├── RenderMachine.java         # Interface (extends RenderMachineCommands)
│       │   │   ├── RenderMachineCommands.java  # say* API contract
│       │   │   ├── RenderMachineImpl.java      # Markdown generation engine
│       │   │   ├── MultiRenderMachine.java    # Chain multiple render machines
│       │   │   └── SayEvent.java              # Event representation
│       │   ├── sse/                      # Server-Sent Events support
│       │   │   ├── SseClient.java            # SSE client interface
│       │   │   ├── SseClientImpl.java         # HttpClient5-based implementation
│       │   │   ├── SseEvent.java             # Event data structure
│       │   │   ├── SseSubscription.java      # Subscription interface
│       │   │   └── SseSubscriptionImpl.java   # Subscription implementation
│       │   ├── testbrowser/              # HTTP client layer (Apache HttpClient5)
│       │   │   ├── TestBrowser.java      # Interface
│       │   │   ├── TestBrowserImpl.java   # HttpClient5 implementation
│       │   │   ├── Request.java          # Fluent request builder (HEAD/GET/DELETE/POST/PUT/PATCH)
│       │   │   ├── Response.java         # Response wrapper + JSON/XML deserialization
│       │   │   ├── Url.java              # Fluent URL builder
│       │   │   ├── TestBrowserConfig.java    # Browser configuration
│       │   │   ├── HttpConstants.java    # Header/content-type constants
│       │   │   ├── HttpPatch.java        # PATCH method implementation
│       │   │   ├── PayloadUtils.java     # JSON/XML pretty-print utilities
│       │   │   └── auth/                 # Authentication providers
│       │   │       ├── AuthProvider.java      # Interface for auth strategies
│       │   │       ├── BasicAuth.java         # HTTP Basic auth
│       │   │       ├── ApiKeyAuth.java        # Custom API key headers
│       │   │       ├── BearerTokenAuth.java   # Bearer token (JWT, etc.)
│       │   │       ├── OAuth2TokenManager.java # OAuth2 token management
│       │   │       └── SessionAwareAuthProvider.java
│       │   └── websocket/                # WebSocket support
│       │       ├── WebSocketClient.java       # WebSocket client interface
│       │       ├── WebSocketClientImpl.java    # Java-WebSocket implementation
│       │       ├── WebSocketSession.java      # Session interface
│       │       ├── WebSocketSessionImpl.java   # Session implementation
│       │       └── WebSocketMessage.java      # Message data structure
│       └── test/java/org/r10r/doctester/  # Comprehensive test suite
│           └── (Tests for all above modules)
└── doctester-integration-test/       # Full integration tests (Ninja framework)
    ├── pom.xml
    └── src/
        ├── main/java/
        │   ├── conf/          # Ninja framework config (Module, Routes, ServletModule)
        │   ├── controllers/   # HTTP controllers demonstrating DocTester
        │   ├── dao/           # Data access objects (JPA + H2)
        │   ├── models/        # JPA entities and DTOs
        │   └── filters/       # HTTP filters (auth, logging)
        └── test/java/controllers/
            ├── (Unit tests for controllers)
            └── docs/                     # Documentation tests
                ├── GettingStartedDocTest.java
                ├── HttpMethodsDocTest.java
                ├── JsonApiDocTest.java
                ├── XmlApiDocTest.java
                ├── QueryParametersDocTest.java
                ├── RequestApiDocTest.java
                ├── ResponseApiDocTest.java
                ├── UrlBuilderDocTest.java
                ├── FileUploadDocTest.java
                ├── AuthenticationDocTest.java
                ├── AccessControlDocTest.java
                ├── ErrorHandlingDocTest.java
                ├── Java25DocTest.java
                └── DocumentationNarrativeDocTest.java
```

---

## Build Commands

```bash
# Fast build with Maven Daemon (preferred)
mvnd clean install -DskipTests

# Build and test (all modules)
mvnd clean verify

# Build only core module
mvnd clean install -pl doctester-core -DskipTests

# Run tests (single module)
mvnd test -pl doctester-core

# Run a specific test class
mvnd test -pl doctester-core -Dtest=DocTesterTest

# Build integration-test module (builds core first)
mvnd clean install -pl doctester-integration-test -am

# Parallel build
mvnd clean verify -T 1C

# Check enforcer rules (Java 25, Maven 4)
mvnd validate

# Stop the mvnd daemon
mvnd --stop
```

> **Do NOT use `./mvnw`** — the Maven wrapper downloads Maven 3. Always use `mvnd` or the system Maven 4: `/opt/apache-maven-4.0.0-rc-5/bin/mvn`.

---

## Maven Proxy Authentication (Enterprise Networks)

**Problem:** In corporate environments with upstream proxies, Maven Central may rate-limit or block requests with "too many authentication attempts" errors.

**Solution:** Use the included `maven-proxy-auth.py` local proxy that adds authentication when forwarding to your upstream proxy.

### Setup

1. **Start the local proxy:**
   ```bash
   python3 maven-proxy-auth.py &
   ```

   The proxy listens on `127.0.0.1:3128` and forwards all requests to your upstream proxy with credentials.

2. **Configure Maven to use the local proxy:**

   Add to `.mvn/jvm.config`:
   ```
   -Dhttp.proxyHost=127.0.0.1
   -Dhttp.proxyPort=3128
   -Dhttps.proxyHost=127.0.0.1
   -Dhttps.proxyPort=3128
   -Dhttp.nonProxyHosts=localhost|127.0.0.1
   ```

   Or pass via command line:
   ```bash
   mvnd clean install \
     -Dhttp.proxyHost=127.0.0.1 \
     -Dhttp.proxyPort=3128 \
     -Dhttps.proxyHost=127.0.0.1 \
     -Dhttps.proxyPort=3128 \
     -Dhttp.nonProxyHosts=localhost|127.0.0.1
   ```

3. **Verify upstream proxy environment:**
   ```bash
   # Must set one of these:
   export https_proxy=http://user:pass@proxy.company.com:8080
   export HTTPS_PROXY=http://user:pass@proxy.company.com:8080
   export http_proxy=http://user:pass@proxy.company.com:8080
   export HTTP_PROXY=http://user:pass@proxy.company.com:8080
   ```

### How It Works

- **Handles HTTPS CONNECT tunneling** — for `https://repo.maven.apache.org/maven2` connections
- **Handles plain HTTP** — for `http://` artifact repositories
- **Injects Proxy-Authorization header** — adds upstream proxy credentials automatically
- **Bidirectional relay** — pipes large artifact downloads efficiently (supports chunked responses)

### Troubleshooting

| Issue | Fix |
|-------|-----|
| `Connection refused` | Check proxy is running: `ps aux \| grep maven-proxy` |
| `Too many auth attempts` | Restart proxy: `pkill -f maven-proxy-auth.py && python3 maven-proxy-auth.py &` |
| `502 Bad Gateway` | Verify upstream proxy in environment: `echo $https_proxy` |
| `No route to host` | Check upstream proxy hostname/port: `ping proxy.company.com` |

### See Also

- `maven-proxy-auth.py` — Python local proxy (included in repo)
- `.mvn/maven.config` — Maven build flags
- `pom.xml` — Root project configuration

---

## Architecture — How DocTester Works

DocTester bridges JUnit 4/5 test execution with **multi-format documentation generation** via five layers:

### 1. Request Layer (`testbrowser/`)
```
Request.GET()                     <- Fluent builder with OAuth2, Bearer, API Key support
  .url(testServerUrl().path("/api/users"))
  .addHeader("Accept", "application/json")
  .payload(myDto)
  .withAuth(oauth2TokenManager)   <- Authentication provider
  .followRedirects(true)
```
- `Request` — immutable builder for HEAD/GET/DELETE/POST/PUT/PATCH
- `Url` — fluent URL builder wrapping Apache `URIBuilder`
- `HttpConstants` — canonical header and content-type constants
- `AuthProvider` hierarchy — supports Basic, Bearer, API Key, OAuth2, custom schemes
- Cookie persistence across requests via `CookieStore`

### 2. Transport Layer (`TestBrowserImpl` + `SseClient` + `WebSocketClient`)
- **HTTP:** Apache `HttpClient5` (5.6) — modern async-capable client
- **WebSockets:** Java-WebSocket library for full-duplex communication
- **Server-Sent Events:** Async event streaming via native SSE protocol
- Handles multipart file uploads, form parameters, redirect following
- Serializes/deserializes payloads via Jackson (JSON + XML)

### 3. Documentation Capture Layer (`RenderMachine` + `MultiRenderMachine`)
- Captures every `say*` call, HTTP exchange, assertion, and test event
- `RenderMachineImpl` — core engine that buffers documentation events
- `MultiRenderMachine` — chains multiple output formats (Markdown → LaTeX → Blog → Slides, etc.)
- Maintains cross-references and citation metadata
- Generates OpenAPI/Swagger specs from HTTP exchanges

### 4. Output Rendering Layer (Multi-format)
- **Markdown** (`RenderMachineImpl`) — portable, version-control friendly
- **LaTeX/PDF** (`RenderMachineLatex`) — academic publishing with templates:
  - ACM conference proceedings
  - arXiv preprint format
  - IEEE journal format
  - Nature magazine format
  - US Patent document format
  - Includes cryptographic receipt embedding for integrity verification
- **HTML** (legacy Bootstrap 3)
- **Blog/Social Media** (`BlogRenderMachine`) — export to:
  - Dev.to
  - Hashnode
  - Medium
  - LinkedIn
  - Substack
- **Presentations** (`SlideRenderMachine`) — Reveal.js HTML5 slide format
- **OpenAPI/Swagger** (`OpenApiWriter`) — JSON or YAML specification

### 5. Assembly & Distribution Layer
- `DocumentAssembler` — combines all test documentation into final output
- `IndexBuilder` — generates table of contents and indices
- `BibliographyManager` — manages citations and BibTeX entries
- `CrossReferenceIndex` — links between document sections
- Generates metadata for SEO, social media, and indexing services

### Lifecycle (JUnit 4)
```
@Test method runs
  → sayNextSection() / say()      → RenderMachine(s) buffer events
  → sayAndMakeRequest(request)    → HttpClient5 executes → RenderMachine logs request+response
  → sayAndAssertThat(...)         → Assertion → green/red indicator
  ↓
TestWatcher.finished()            → RenderMachine.finishAndWriteOut()
  ↓
@AfterClass / finishDocTest()     → DocumentAssembler creates final outputs (Markdown, LaTeX, OpenAPI, etc.)
```

### Lifecycle (JUnit 5)
```
@Test method runs (with DocTesterExtension active)
  → DocTesterContext injected into test
  → say* methods buffer to active RenderMachine(s)
  → AfterTestExecution → RenderMachine writes output
  ↓
Suite finishes → DocumentAssembler generates indices & aggregates
```

---

## Advanced Features (2.5.0+)

DocTester 2.5.0 introduces enterprise-grade capabilities:

### 1. Multi-Format Output
- **Markdown** — portable, Git-friendly documentation
- **LaTeX/PDF** — academic publishing with citation support (ACM, IEEE, arXiv, Nature, US Patent templates)
- **OpenAPI/Swagger** — machine-readable API specifications (JSON/YAML)
- **HTML5 Slides** — Reveal.js presentations auto-generated from tests
- **Blog Posts** — direct export to Dev.to, Medium, Hashnode, LinkedIn, Substack
- **Cryptographic Receipts** — blockchain-style hash chains for document integrity verification

### 2. Advanced Authentication
- **Basic HTTP Authentication** — RFC 7617
- **Bearer Tokens** — JWT, OAuth2 access tokens, custom schemes
- **API Key** — custom header injection (e.g., X-API-Key)
- **OAuth2** — token refresh, scope management, implicit flow
- **Session-Aware** — automatic cookie jar and session management

### 3. WebSocket & Real-Time Testing
- Full WebSocket protocol support (RFC 6455)
- Server-Sent Events (SSE) streaming
- Message recording and assertion
- Event ordering verification

### 4. Bibliography & Citations
- BibTeX entry management
- Citation key resolution
- Cross-document references
- Automatic bibliography generation

### 5. Reflection & Diff Analysis
- Structural object comparison (ReflectiveDiff)
- Class hierarchy introspection
- String similarity metrics
- Annotation profiling for metadata extraction

### 6. OpenAPI/Swagger Integration
- Automatic spec generation from HTTP test exchanges
- Endpoint documentation extraction
- Schema inference from payloads
- Export as JSON or YAML

### 7. Assembly & Indexing
- Multi-document assembly
- Automatic table of contents generation
- Word count and complexity metrics
- Cross-reference linking

---

## Key Class Reference

### `DocTester` (abstract base class)
Extend this in your JUnit 4 test classes. Provides:

| Method | Purpose |
|--------|---------|
| `say(String text)` | Render a paragraph in the doc |
| `sayNextSection(String title)` | Render H1 heading + TOC entry |
| `sayRaw(String html)` | Inject raw HTML |
| `sayCode(String code, String language)` | Fenced code block with syntax highlighting |
| `sayTable(String[][] rows)` | Markdown table |
| `sayJson(Object obj)` | JSON code block (pretty-printed) |
| `sayWarning(String text)` | Alert block (blockquote with [!WARNING]) |
| `sayNote(String text)` | Alert block (blockquote with [!NOTE]) |
| `sayKeyValue(Map<String, String>)` | 2-column metadata table |
| `sayUnorderedList(List<String>)` | Bullet list |
| `sayOrderedList(List<String>)` | Numbered list |
| `sayAssertions(Map<String, String>)` | Check/result matrix |
| `sayAndMakeRequest(Request)` | Execute HTTP and document request+response → returns `Response` |
| `makeRequest(Request)` | Execute HTTP silently (no documentation) → returns `Response` |
| `sayAndAssertThat(String, T, Matcher<T>)` | Assert with Hamcrest + document result |
| `sayAndGetCookies()` | Document and return current cookies |
| `sayAndGetCookieWithName(String)` | Document and return named cookie |
| `testServerUrl()` | **Must override** — returns base `Url` for the test server |
| `finishDocTest()` | Static — call in `@AfterClass` to generate index page |

### JUnit 5 Support (`DocTesterExtension`)
For JUnit 5, annotate test class with:
```java
@ExtendWith(DocTesterExtension.class)
class MyDocTest {
    @Test
    void myTest(DocTesterContext ctx) {
        ctx.say("Hello");
        ctx.sayNextSection("Section");
        // All DocTester say* methods available
    }
}
```

The `DocTesterContext` is injected and manages:
- Render machine lifecycle
- Event buffering and dispatch
- Output generation
- Format selection (Markdown, LaTeX, HTML, etc.)

### `Request` (fluent builder with authentication)
```java
Request.GET()                          // HEAD, GET, DELETE, POST, PUT, PATCH
    .url(Url)                          // or .url(URI)
    .contentTypeApplicationJson()      // sets Content-Type header
    .addHeader(String, String)
    .addFormParameter(String, String)
    .payload(Object)                   // serialized by Jackson
    .addFileToUpload(String, File)
    .followRedirects(boolean)
    .withAuth(basicAuth)               // HTTP Basic: BasicAuth.of("user", "pass")
    .withAuth(bearerToken)             // Bearer: BearerTokenAuth.of("jwt-token")
    .withAuth(apiKeyAuth)              // Custom header: ApiKeyAuth.of("X-API-Key", "key123")
    .withAuth(oauth2Manager)           // OAuth2: OAuth2TokenManager
    .connectTimeout(Duration)
    .readTimeout(Duration)
```

### `Response`
```java
response.httpStatus()                  // int HTTP status code
response.payload()                     // raw byte[]
response.payloadAsString()             // UTF-8 string
response.payloadAsPrettyString()       // pretty-printed JSON/XML
response.payloadAs(MyDto.class)        // auto-detect JSON/XML → Jackson deserialization
response.payloadJsonAs(MyDto.class)
response.payloadJsonAs(new TypeReference<List<MyDto>>(){})
response.payloadXmlAs(MyDto.class)
response.headers()                     // Map<String, String>
```

### `Url` (fluent URL builder)
```java
Url.host("http://localhost:8080")
    .path("/api/users")
    .addQueryParameter("page", "1")
    .uri()                             // returns java.net.URI
```

---

## Extended Documentation API — Rich Formatting

DocTester now includes 9 additional `say*` methods for powerful, flexible documentation generation **beyond HTTP testing**. These methods render to clean, portable **Markdown**, suitable for GitHub, documentation platforms, and static site generators.

### Methods Reference

| Method | Output | Use Case |
|--------|--------|----------|
| **`sayTable(String[][])`** | Markdown table | API response matrices, feature comparisons |
| **`sayCode(String, String)`** | Fenced code block | Database queries, gRPC payloads, config examples |
| **`sayWarning(String)`** | `> [!WARNING]` alert | Deprecation notices, caveats, side effects |
| **`sayNote(String)`** | `> [!NOTE]` alert | Tips, clarifications, context |
| **`sayKeyValue(Map<String, String>)`** | 2-column table | Headers, env vars, metadata |
| **`sayUnorderedList(List<String>)`** | Bullet list | Prerequisites, features, checklists |
| **`sayOrderedList(List<String>)`** | Numbered list | Steps, workflows, sequences |
| **`sayJson(Object)`** | JSON code block | Payload preview, configuration display |
| **`sayAssertions(Map<String, String>)`** | Check/Result table | Validation logs, batch results |

### How-To: Common Documentation Patterns

**1. Document an API response structure:**
```java
sayNextSection("User Response Schema");
say("The GET /users endpoint returns a paginated list of users.");

sayJson(Map.of(
    "id", 1,
    "name", "Alice",
    "email", "alice@example.com",
    "createdAt", "2026-03-11T00:00:00Z"
));

sayNote("All timestamps are in ISO 8601 format (UTC).");
```

**2. Show database query examples:**
```java
sayNextSection("Database Examples");
sayCode("SELECT u.id, u.name, COUNT(a.id) as article_count " +
        "FROM users u LEFT JOIN articles a ON u.id = a.user_id " +
        "WHERE u.active = true GROUP BY u.id;", "sql");
```

**3. Compare API versions:**
```java
sayTable(new String[][] {
    {"Feature", "v1", "v2", "v3"},
    {"Authentication", "API Key", "OAuth2", "OAuth2 + OIDC"},
    {"Pagination", "offset/limit", "cursor", "cursor"},
    {"Rate Limiting", "None", "10k/hour", "1k/min per scope"}
});
```

**4. Explain prerequisites:**
```java
sayNextSection("Setup Requirements");
sayUnorderedList(Arrays.asList(
    "Java 25+ (OpenJDK or Oracle JDK)",
    "Maven 4.0.0+",
    "PostgreSQL 14+ (or H2 for testing)",
    "Docker (for integration tests)"
));

sayWarning("DocTester requires Java 25+ with --enable-preview flag active.");
```

**5. Document a workflow:**
```java
sayNextSection("User Creation Workflow");
sayOrderedList(Arrays.asList(
    "Submit POST /users with name and email",
    "System validates email uniqueness",
    "Account activated via email confirmation link",
    "User can log in with email and password"
));
```

**6. Summarize test assertions:**
```java
sayAssertions(Map.of(
    "Status code is 200", "✓ PASS",
    "Response time < 100ms", "✓ PASS",
    "JSON schema matches", "✓ PASS",
    "CORS headers present", "✗ FAIL — missing Access-Control-Allow-Origin"
));
```

### Explanation: When to Use Each Method

- **`sayTable()`**: Use when comparing data side-by-side (e.g., version features, response fields, test results across scenarios).
- **`sayCode()`**: Use for multi-line code examples, SQL queries, configuration files, or any syntax-highlighted content.
- **`sayWarning()`**: Highlight breaking changes, security concerns, or critical gotchas that users *must* know.
- **`sayNote()`**: Provide helpful context, tips, or clarifications that enhance understanding but aren't critical.
- **`sayKeyValue()`**: Document metadata in a clean, scannable format (headers, env variables, configuration).
- **`sayUnorderedList()`**: Enumerate features, prerequisites, or options where order doesn't matter.
- **`sayOrderedList()`**: Document sequential steps, workflows, or procedures.
- **`sayJson()`**: Show JSON payloads, configuration objects, or data structures in pretty-printed form.
- **`sayAssertions()`**: Summarize test results or validation checks in a table format.

### Reference: Complete Markdown Output

All methods generate **pure Markdown**, not HTML. This makes documentation:
- **Portable**: Works on GitHub, GitLab, Gitea, standard Markdown renderers
- **Version-control friendly**: Clean text diffs in git
- **Tool-agnostic**: No dependency on Bootstrap, jQuery, or custom CSS

Example markdown output from `sayJson()`:
```markdown

\`\`\`json
{
  "name" : "Alice",
  "age" : 30,
  "active" : true
}
\`\`\`
```

Example output from `sayTable()`:
```markdown

| Name | Status | Score |
| --- | --- |
| Alice | Active | 95 |
| Bob | Inactive | 87 |
```

---

## Testing Patterns

### JUnit 4 Pattern (extending DocTester)

```java
class UserApiDocTest extends DocTester {

    @Test
    void testGetUsers() {
        sayNextSection("Fetch User List");
        say("GET /api/users returns paginated users with pagination metadata.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .addQueryParameter("page", "1")
                .addQueryParameter("limit", "10")
                .contentTypeApplicationJson());

        sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));

        List<UserDto> users = response.payloadJsonAs(new TypeReference<>() {});
        sayAndAssertThat("Returns list", true, is(!users.isEmpty()));

        sayNote("All timestamps are in ISO 8601 UTC format.");
    }

    @Test
    void testCreateUserWithAuth() {
        sayNextSection("Create User (Authenticated)");

        UserDto newUser = new UserDto("alice", "alice@example.com");
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser)
                .withAuth(BearerTokenAuth.of(fetchOAuth2Token())));

        sayAndAssertThat("Created with 201", 201, equalTo(response.httpStatus()));

        sayJson(response.payloadAs(UserDto.class));

        sayWarning("Do not expose API tokens in production logs.");
    }

    @AfterClass
    public static void afterClass() {
        finishDocTest();  // generates Markdown, index, and aggregates
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

### JUnit 5 Pattern (with Extension)

```java
@ExtendWith(DocTesterExtension.class)
class AdvancedApiDocTest {

    @Test
    void testRealTimeEvents(DocTesterContext ctx) {
        ctx.sayNextSection("WebSocket Real-Time Events");
        ctx.say("Subscribe to user activity stream via WebSocket.");

        WebSocketClient ws = new WebSocketClientImpl("ws://localhost:8080/api/events");
        WebSocketSession session = ws.connect();

        session.send("""
            {
                "action": "subscribe",
                "channels": ["user.created", "user.updated"]
            }
            """);

        ctx.sayCode("""
            {
                "action": "subscribe",
                "channels": ["user.created", "user.updated"]
            }
            """, "json");

        // Receive and document events
        WebSocketMessage event1 = session.receive(Duration.ofSeconds(5));
        ctx.say("Received event: " + event1.getPayload());

        session.close();
        ctx.sayNote("WebSocket connections are persistent until explicitly closed.");
    }

    @Test
    void testServerSentEvents(DocTesterContext ctx) {
        ctx.sayNextSection("Server-Sent Events (SSE)");
        ctx.say("Stream events from /api/stream endpoint.");

        SseClient client = new SseClientImpl();
        SseSubscription sub = client.subscribe(
            Url.host("http://localhost:8080").path("/api/stream").uri());

        ctx.sayTable(new String[][] {
            {"Event Type", "Frequency", "Payload"},
            {"heartbeat", "Every 30s", "Keep-alive ping"},
            {"user.action", "Real-time", "User activity"},
            {"alert", "On-demand", "Critical notification"}
        });

        sub.onEvent(event ->
            ctx.say("Received: " + event.getData())
        );

        sub.close();
    }
}
```

### Integration Test Pattern (Ninja Framework)

The `doctester-integration-test` module demonstrates:

```java
class ApiControllerDocTest extends NinjaApiDoctester {
    // Server starts automatically via @NinjaTest
    // testServerUrl() returns embedded Jetty server URL
    // Access to full Ninja DI container for mocking/stubbing
}
```

### Testing Principles: REAL CODE, REAL MEASUREMENTS

**CRITICAL RULE:** Never simulate, fake, or estimate performance metrics. Always use actual DocTester rendering code with real measurement data.

**What This Means:**
- ❌ DO NOT: Use `Thread.sleep()` to simulate document rendering
- ❌ DO NOT: Hard-code fake performance numbers
- ❌ DO NOT: Create mock objects that don't reflect real behavior
- ✅ DO: Run actual `RenderMachine` implementations
- ✅ DO: Measure real document generation time (markdown, latex, openapi)
- ✅ DO: Use `System.nanoTime()` to capture actual execution time
- ✅ DO: Report numbers with units and context

**Performance Test Pattern:**

```java
@Test
void testRealDocumentGenerationPerformance() {
    // Use actual DocTester rendering, NOT simulations
    RenderMachine markdown = new RenderMachineImpl();

    long startNanos = System.nanoTime();

    // Call REAL methods, not mocks
    markdown.sayNextSection("Performance Test");
    markdown.say("Real document content with measurements.");
    markdown.sayCode("System.out.println(\"Hello\");", "java");
    markdown.sayTable(new String[][] {
        {"Feature", "Status"},
        {"Real Code", "✓ Yes"},
        {"Real Measurement", "✓ Yes"}
    });

    String output = markdown.finishAndWriteOut();

    long elapsedNanos = System.nanoTime() - startNanos;
    double elapsedMs = elapsedNanos / 1_000_000.0;

    // REPORT ACTUAL NUMBERS
    System.out.printf("Document generation took %.2f ms%n", elapsedMs);

    // Assert on REAL behavior
    assertThat("Markdown output exists", output.length() > 0);
    assertThat("Performance reasonable", elapsedMs < 5000.0); // 5 seconds max
}
```

**Documentation Rule:**
When reporting performance improvements, include:
1. **Actual measurement command** that reproduces the result
2. **Real execution time** (in units: ns, µs, ms, s)
3. **Environment details** (JVM flags, system specs, Java version)
4. **Statistical confidence** (number of iterations, warm-up phases)
5. **Before/after comparison** with both real numbers

Example of HONEST reporting:
```
✅ REAL: "JEP 516 metadata cache: 78ns average (10M accesses, 100 iterations, Java 25.0.2)"
❌ FAKE: "JEP 516 cache: 99% faster (simulation shows 6,667x)"
```

### OpenAPI/Swagger Generation

All HTTP exchanges are automatically collected for OpenAPI spec generation:

```java
@Test
void testApiWithOpenApiCapture() {
    OpenApiCollector collector = new OpenApiCollector();

    Response resp = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .contentTypeApplicationJson()
            .payload(userDto));

    collector.recordExchange(resp.request(), resp);

    OpenApiSpec spec = collector.buildSpec("User API", "1.0.0");
    OpenApiWriter.writeJson(spec, new File("target/openapi.json"));
    OpenApiWriter.writeYaml(spec, new File("target/openapi.yaml"));
}
```

### LaTeX/PDF Academic Output

Generate academic papers with cryptographic integrity:

```java
@Test
void documentResearchFinding() {
    sayNextSection("Experimental Results");

    sayCode("SELECT COUNT(*) FROM experiments WHERE success = 1;", "sql");

    // Enables ReceiptGenerator for blockchain-style hash chain
    ReceiptGenerator receipt = new ReceiptGenerator();
    String documentHash = receipt.hashContent("experiment_2024_03_11");

    say("Document fingerprint: " + documentHash);
    sayWarning("Any modifications to this document will invalidate the cryptographic receipt.");

    // Render to ACM/IEEE/arXiv template via:
    // RenderMachineLatex latex = new RenderMachineLatex(LatexTemplate.ACM_CONFERENCE);
}
```

---

## Java 25 — Key Features to Use

DocTester targets **Java 25 with `--enable-preview`**. Prefer modern idioms:

### Stable Features
- **Records** for DTOs and value objects
- **Sealed classes + pattern matching** for request/response hierarchies
- **`switch` expressions** (exhaustive) over long `if/else` chains
- **Text blocks** for HTML template strings
- **Virtual threads** (`Thread.ofVirtual()`) for async HTTP calls
- **Sequenced collections** (`SequencedMap`, `SequencedSet`) where ordering matters
- **`String.formatted()`** over `String.format()`
- **`instanceof` pattern matching** instead of explicit casts
- **`var`** for local type inference where RHS is obvious

### Preview Features (enabled)
- **Primitive types in patterns** (Java 23+)
- **Unnamed patterns and variables** (`_`)
- **Flexible constructor bodies**

### Preferred Patterns
```java
// ✅ Records for DTOs
record UserDto(String name, String email) {}

// ✅ Sealed hierarchy + exhaustive switch
sealed interface HttpResult permits HttpSuccess, HttpError {}
record HttpSuccess(int status, String body) implements HttpResult {}
record HttpError(int status, String message) implements HttpResult {}

String describe(HttpResult r) {
    return switch (r) {
        case HttpSuccess(var s, var b) -> "OK %d: %s".formatted(s, b);
        case HttpError(var s, var m)   -> "FAIL %d: %s".formatted(s, m);
    };
}

// ✅ Virtual threads for async HTTP
try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Response> f = exec.submit(() -> browser.makeRequest(req));
    return f.get();
}

// ✅ Text blocks for HTML templates
String html = """
    <div class="panel panel-default">
        <div class="panel-heading">%s</div>
        <pre>%s</pre>
    </div>
    """.formatted(title, body);

// ✅ Pattern matching instanceof
if (payload instanceof JsonNode node && !node.isNull()) {
    return node.asText();
}

// ❌ Avoid: raw types, explicit casts, String.format(), anonymous classes where records fit
```

---

## Dependencies (doctester-core)

### Core HTTP & Transport
| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.apache.httpcomponents.client5:httpclient5` | 5.6 | Modern HTTP/1.1 & HTTP/2 client |
| `org.java-websocket:Java-WebSocket` | 1.6.0 | WebSocket protocol client |
| `commons-fileupload:commons-fileupload` | 1.6.0 | Multipart file upload (RFC 2388) |

### JSON & XML Processing
| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.fasterxml.jackson.core:jackson-core` | 2.21.1 | Core JSON processing |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | 2.21.1 | XML serialization/deserialization |
| `com.fasterxml.woodstox:woodstox-core` | 7.0.0 | High-performance XML processing (StAX) |
| `javax.xml.bind:jaxb-api` | 2.3.1 | JAXB annotations (Java 9+) |
| `org.glassfish.jaxb:jaxb-runtime` | 2.3.9 | JAXB runtime |

### Utilities & Logging
| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.google.guava:guava` | 33.5.0-jre | Collections, caching, utilities |
| `org.slf4j:slf4j-api` | 2.0.17 | Logging API (facade) |
| `org.slf4j:slf4j-simple` | 2.0.17 | Simple logging implementation (test) |
| `org.slf4j:jcl-over-slf4j` | 2.0.17 | Apache Commons Logging bridge |

### Testing Framework
| Dependency | Version | Scope | Purpose |
|-----------|---------|-------|---------|
| `junit:junit` | 4.12 | provided | JUnit 4 (consumers supply their own) |
| `org.junit.jupiter:junit-jupiter` | 6.0.3 | test | JUnit 5 platform & API |
| `org.mockito:mockito-core` | 5.22.0 | test | Mocking framework |
| `org.mockito:mockito-junit-jupiter` | 5.22.0 | test | JUnit 5 integration |
| `net.jqwik:jqwik` | 1.9.0 | test | Property-based testing |
| `org.wiremock:wiremock-standalone` | 3.12.1 | test | HTTP mocking & WireMock server |

### Security & Cryptography
| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.bouncycastle:bcprov-jdk18on` | 1.77 | Cryptography (for receipt hashing) |

### Optional/Integration
| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.ninjaframework:ninja-servlet` | 7.0.0 | Ninja web framework (integration tests) |
| `org.ninjaframework:ninja-standalone` | 7.0.0 | Embedded Ninja server |
| `org.eclipse.jetty:jetty-maven-plugin` | 9.4.53.v20231009 | Jetty servlet container |
| `org.flywaydb:flyway-core` | 10.21.0 | Database schema migrations |
| `com.h2database:h2` | 2.4.240 | H2 in-memory database (testing) |
| `com.google.code.gson:gson` | 2.13.2 | Alternative JSON processor |

> **Note:** `junit:junit` is `provided` scope — consumers must include JUnit 4 or JUnit 5 themselves. The project supports both via engine discovery.

---

## Maven 4 — Best Practices

- Use `<release>25</release>` in compiler plugin (not `<source>`/`<target>`)
- Maven 4 uses `.mvn/maven.config` for persistent build flags
- `mvnd` (Maven Daemon) configured via `~/.m2/mvnd.properties`
- Prefer `mvnd` locally; CI can use `mvn -T 1C`
- Maven 4 supports improved multi-module build ordering
- `--enable-preview` must be in both compiler config AND surefire `<argLine>`

### Plugin Versions (root POM)
| Plugin | Version | Purpose |
|--------|---------|---------|
| `maven-compiler-plugin` | 3.13.0 | Java 25 compilation with `--enable-preview` |
| `maven-surefire-plugin` | 3.5.3 | Test execution (JUnit 4/5 engine discovery) |
| `maven-enforcer-plugin` | 3.5.0 | Enforce Java 25+ and Maven 4.0.0-rc-3+ |
| `maven-javadoc-plugin` | 3.11.2 | JavaDoc generation with preview flag |
| `ninja-maven-plugin` | 7.0.0 | Ninja framework integration |
| `jetty-maven-plugin` | 9.4.53.v20231009 | Embedded server for testing |
| `maven-assembly-plugin` | 2.4 | Assembly artifacts for distribution |
| `maven-deploy-plugin` | 2.7 | Artifact deployment |
| `central-publishing-maven-plugin` | 0.6.0 | Maven Central Publisher (release profile) |
| `maven-source-plugin` | 3.3.1 | Source jar generation (release profile) |
| `maven-gpg-plugin` | (release profile) | GPG signing for Maven Central |

---

## mvnd Configuration

mvnd 2.x (Maven Daemon for Maven 4) is installed at `/opt/mvnd`. Key settings (`~/.m2/mvnd.properties`):

```properties
# Daemon JVM settings for Java 25 + Maven 4
mvnd.javaHome=/usr/lib/jvm/java-25-openjdk-amd64
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
```

---

## Documentation Output Structure

Generated docs land in `target/docs/` and related directories (relative to the module under test):

```
target/
├── docs/                             # Primary Markdown output
│   ├── index.md                      # Master index with TOC
│   ├── test-results/
│   │   ├── MyDocTest.md              # Per-test documentation
│   │   └── AnotherTest.md
│   ├── openapi.json                  # Swagger/OpenAPI spec
│   ├── openapi.yaml
│   └── bibliography.bib              # BibTeX citations
├── pdf/                              # LaTeX/PDF output (if enabled)
│   ├── MyDocTest.pdf                 # Rendered PDF
│   └── consolidated.pdf              # Combined PDF with TOC
├── slides/                           # Reveal.js HTML5 slides
│   └── presentation.html
├── blog/                             # Blog export queue
│   ├── medium.json
│   ├── devto.json
│   └── queue.json                    # Social media posting queue
├── site/doctester/                   # Legacy HTML output (if enabled)
│   ├── index.html
│   ├── MyDocTest.html
│   └── assets/
│       ├── bootstrap/
│       ├── jquery/
│       └── custom_doctester_stylesheet.css

```

### Custom Configuration

Place configuration in `src/main/resources/doctester.properties`:
```properties
# Output formats (comma-separated)
render.formats=markdown,latex,openapi,blog

# LaTeX template
latex.template=ACM_CONFERENCE

# Blog platforms
blog.platforms=devto,medium,hashnode

# Citation style
bibliography.style=IEEE

# Document metadata
document.title=API Documentation
document.author=Your Team
document.version=2.5.0
```

Custom CSS: put `custom_doctester_stylesheet.css` in `src/main/resources/` for HTML output. It is automatically copied and linked.

---

## Code Style

- 4 spaces, no tabs (IntelliJ/Eclipse default Sun style)
- UTF-8 everywhere
- All source files must have the Apache 2.0 license header
- Javadoc on all public API methods
- Keep classes small and focused (single responsibility)
- Package: `org.r10r.doctester` for core; integration tests use `controllers`, `models`, etc.

---

## Claude Sub-Agents

This project ships custom Claude Code sub-agent profiles in `.claude/agents/`:

| Agent | File | When to use |
|-------|------|-------------|
| `java-25-expert` | `.claude/agents/java-25-expert.md` | Modernizing Java source, records, sealed classes, pattern matching, virtual threads |
| `maven-build-expert` | `.claude/agents/maven-build-expert.md` | Build failures, pom.xml changes, dependency issues, mvnd daemon management |

These agents are automatically available to Claude Code via the `Agent` tool.

---

## Output Formats & Rendering

### Markdown (Default)
Generated as Markdown (`.md`) files:
```
target/docs/
├── test-results.md
├── index.md
└── cross-references.md
```
Portable, Git-friendly, suitable for GitHub wikis, documentation platforms, static site generators.

### LaTeX/PDF
Render academic papers with `RenderMachineLatex`:
```java
RenderMachineLatex latex = new RenderMachineLatex(LatexTemplate.ACM_CONFERENCE);
documentAssembler.addRenderMachine(latex);
// Outputs: target/pdf/documentname.pdf
```
Supported templates:
- **ACM_CONFERENCE** — ACM conference proceedings
- **ARXIV** — arXiv preprint (e.g., cs.CL)
- **IEEE** — IEEE Transactions
- **NATURE** — Nature magazine format
- **US_PATENT** — US patent document (DOCX→PDF)

Compiler strategies (auto-detected):
- `pdflatex` (default) — fast, reliable
- `xelatex` — Unicode & custom fonts
- `lualatex` — modern Lua-extended TeX
- `latexmk` — intelligent recompilation
- `pandoc` — convert to Word/DOCX

### Blog Export
Export test documentation to blogging platforms:
```java
BlogRenderMachine blog = new BlogRenderMachine(new MediumTemplate());
documentAssembler.addRenderMachine(blog);
// SocialQueueWriter writes: target/blog/queue.json
```
Supported platforms:
- **Dev.to** — community-driven tech blog
- **Medium** — long-form publishing
- **Hashnode** — developer blog network
- **Substack** — newsletter platform
- **LinkedIn** — professional network

### Presentation Slides
Create Reveal.js HTML5 presentations:
```java
SlideRenderMachine slides = new SlideRenderMachine(new RevealJsTemplate());
documentAssembler.addRenderMachine(slides);
// target/slides/presentation.html
```
Live demos, speaker notes, and interactive code examples embedded directly in slides.

### OpenAPI/Swagger Specification
Auto-generate API documentation:
```java
OpenApiCollector collector = new OpenApiCollector();
collector.recordExchange(request, response);
OpenApiSpec spec = collector.buildSpec("My API", "2.5.0");
OpenApiWriter.writeJson(spec, outputFile);   // JSON
OpenApiWriter.writeYaml(spec, outputFile);   // YAML (for Swagger UI)
```
Discovers endpoints, methods, parameters, response schemas, and status codes from test execution.

### Multi-Format Chain
Render to multiple formats simultaneously:
```java
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl(),              // Markdown
    new RenderMachineLatex(...),         // PDF
    new BlogRenderMachine(...),          // Blog
    new SlideRenderMachine(...)          // Slides
);
documentAssembler.addRenderMachine(multi);
// All formats generated in parallel
```

---

## Environment Checks

Before building, verify the toolchain:
```bash
java -version          # Must show: openjdk 25.x.x
mvnd --version         # Must show: mvnd 2.0.0-rc-3 / Maven 4.0.0-rc-5
mvn --version          # Must show: Apache Maven 4.0.0-rc-5
echo $JAVA_HOME        # Must be /usr/lib/jvm/java-25-openjdk-amd64
mvnd --status          # Show running daemon(s)
```

## Troubleshooting

### Compilation & Build Issues
| Symptom | Fix |
|---------|-----|
| `--enable-preview` compile errors | Ensure both compiler plugin AND surefire `<argLine>` include `--enable-preview` |
| Daemon not starting | `mvnd --stop && mvnd compile` to restart |
| Wrong Java version | `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` |
| Maven 3 being invoked | Never use `./mvnw`; use `mvnd` or `/opt/apache-maven-4.0.0-rc-5/bin/mvn` |
| Dependency conflict | Run `mvnd dependency:tree -pl <module>` and add `<exclusions>` as needed |
| HttpClient5 migration errors | Old `DefaultHttpClient` removed; update to `HttpClient5` APIs |

### Testing & Execution
| Symptom | Fix |
|---------|-----|
| JUnit 4 and JUnit 5 both discovered | Expected behavior; use `@Disabled` or separate test folders if needed |
| WebSocket connection refused | Ensure test server listens on WebSocket endpoint (ws://...) |
| SSE streaming timeout | Increase `readTimeout()` on Request; SSE may have long latencies |
| OAuth2 token refresh fails | Verify token endpoint is reachable; check `OAuth2TokenManager` configuration |
| LaTeX compilation fails | Install texlive/miktex; verify template selection matches installed distro |

### Documentation Generation
| Symptom | Fix |
|---------|-----|
| PDF not generated | Check `RenderMachineLatex` is added to render chain; verify pdflatex is installed |
| OpenAPI spec empty | Ensure HTTP exchanges are recorded via `OpenApiCollector.recordExchange()` |
| Blog export fails | Verify platform API credentials in `BlogTemplate` configuration |
| WebSocket events not captured | Manually record via `ctx.say()` or `ctx.sayJson()` |
| H2 connection errors | Integration tests use H2 in-memory mode; URL format is `jdbc:h2:mem:...` |
| JAXB runtime errors | Ensure `jaxb-runtime` is on classpath; required for XML serialization |

### Advanced Features
| Symptom | Fix |
|---------|-----|
| Cryptographic receipt mismatch | Ensure document content is not modified; re-run ReceiptGenerator on modified docs |
| Citation key not found | Add entry to bibliography via `BibliographyManager.addEntry()` before citing |
| Cross-reference broken | Use valid anchor syntax: `@see:SectionTitle` or use `CrossReferenceIndex.resolve()` |
| Reflection diff shows spurious diffs | Check field ordering; use `ReflectiveDiff.withFieldOrder()` for consistent results |

# DTR (Documentation Testing Runtime) — Markdown Living Documentation for Java 26

> **Generate living documentation as your tests execute.** Every test run regenerates docs in multiple formats (Markdown, PDF, LaTeX, Blog posts, OpenAPI specs) from live behavior—keeping docs forever in sync with reality.

**Latest:** `2.5.0` | **License:** Apache 2.0 | **Java:** 26 LTS | **Maven:** `io.github.seanchatmangpt.dtr:dtr-core`

---

## ⚡ 5-Minute Quick Start

### 1. Create a test that documents itself

Save as `src/test/java/example/FirstDocTest.java`:

```java
package example;

import io.github.seanchatmangpt.dtr.DocTester;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

public class FirstDocTest extends DocTester {

    @Test
    void documentUserApi() {
        sayNextSection("User Management API");
        say("Retrieve a user by ID and display their details.");

        // Document what we're testing
        var user = Map.of(
            "id", 1,
            "name", "Alice",
            "email", "alice@example.com",
            "role", "admin"
        );

        sayJson(user);

        // Show what happened
        sayAssertions(Map.of(
            "User exists", "✓ PASS",
            "Has admin role", "✓ PASS",
            "Email format valid", "✓ PASS"
        ));

        // Add a warning for clarity
        sayWarning("Admin users can modify system settings. Audit all changes.");
    }
}
```

### 2. Run the test

```bash
mvnd test -Dtest=FirstDocTest
```

### 3. View generated docs

```bash
cat target/docs/test-results/FirstDocTest.md
```

**Output is pure Markdown:**
```markdown
# User Management API

Retrieve a user by ID and display their details.

```json
{
  "id" : 1,
  "name" : "Alice",
  "email" : "alice@example.com",
  "role" : "admin"
}
```

## Assertions

| Check | Result |
|---|---|
| User exists | ✓ PASS |
| Has admin role | ✓ PASS |
| Email format valid | ✓ PASS |

> [!WARNING]
> Admin users can modify system settings. Audit all changes.
```

**Done!** Your documentation is version-controlled, portable, and always accurate. 🎉

---

## 📚 Tutorials — Learn by Doing

### Tutorial: Document REST API Responses

Learn how to test HTTP endpoints and auto-generate API documentation.

**1. Create a simple controller test:**

```java
@Test
void documentFetchUser() {
    sayNextSection("GET /api/users/:id");
    say("Retrieve a single user by their ID.");

    // Simulate an HTTP request (in real tests, this hits your server)
    var response = Map.of(
        "status", 200,
        "data", Map.of(
            "id", 1,
            "name", "Alice",
            "createdAt", "2026-03-11T00:00:00Z"
        )
    );

    sayJson(response.get("data"));

    sayNote("Timestamps are returned in ISO 8601 UTC format.");
}
```

**2. Run and generate:**

```bash
mvnd test -Dtest=YourApiDocTest
```

**3. Check output:**

```bash
cat target/docs/test-results/YourApiDocTest.md
```

**Result:** Self-documenting API tests that stay in sync with code.

---

### Tutorial: Compare Multiple Scenarios with Tables

Show different API responses or behavior patterns side-by-side.

```java
@Test
void documentPaymentMethods() {
    sayNextSection("Supported Payment Methods");

    sayTable(new String[][] {
        {"Method", "Processing Time", "Supported Regions", "Fees"},
        {"Credit Card", "Instant", "Global", "2.9% + $0.30"},
        {"PayPal", "1-3 hours", "Global", "3.5% + $0.50"},
        {"Bank Transfer", "3-5 days", "Europe/US", "Free"},
        {"Crypto", "10 minutes", "Global", "Network dependent"}
    });

    sayNote("Credit cards are instant but have higher fees. Choose based on your region and timeline.");
}
```

**Output:** Professional comparison table in Markdown.

---

### Tutorial: Generate PDF Academic Papers

Auto-publish test documentation as academic papers with LaTeX templates.

```java
@Test
void documentResearchFindings() {
    sayNextSection("Experimental Results");

    var results = Map.of(
        "trials", 1000,
        "success_rate", "99.2%",
        "avg_latency_ms", 45.3,
        "p_value", 0.0001
    );

    sayJson(results);

    sayWarning("This is a preview version. Results are confidential until peer review is complete.");
}
```

**Render with LaTeX:**

```java
// In your DocumentAssembler:
RenderMachineLatex latex = new RenderMachineLatex(LatexTemplate.ARXIV);
documentAssembler.addRenderMachine(latex);
// Generates: target/pdf/YourTest.pdf
```

**Output:** Publication-ready PDF with proper citations and formatting.

---

## 🎯 How-To Guides — Solve Real Problems

### How-To: Test Authentication and Document It

**Goal:** Show that your OAuth2 implementation works and document the flow.

```java
@Test
void documentOAuth2Flow() {
    sayNextSection("OAuth2 Token Exchange");
    say("Exchange authorization code for access token.");

    var request = Request.POST()
        .url(testServerUrl().path("/oauth/token"))
        .contentTypeApplicationJson()
        .payload(Map.of(
            "grant_type", "authorization_code",
            "code", "auth_code_xyz",
            "client_id", "client_123"
        ))
        .withAuth(BasicAuth.of("client_123", "secret"));

    Response response = sayAndMakeRequest(request);

    sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));

    var token = response.payloadJsonAs(new TypeReference<Map<String, String>>() {});
    sayJson(token);

    sayNote("Token expires in 1 hour. Refresh tokens are valid for 30 days.");
}
```

**Output:** Documented HTTP flow with request, response, and assertions.

---

### How-To: Export Documentation to Your Blog

**Goal:** Auto-publish test results to Dev.to or Medium.

```java
// In your test setup:
BlogRenderMachine blog = new BlogRenderMachine(new DevToTemplate());
documentAssembler.addRenderMachine(blog);

// Generated at: target/blog/devto.json
// Ready to push to Dev.to API
```

Then use the social queue:

```bash
curl -X POST https://dev.to/api/articles \
  -H "api-key: YOUR_KEY" \
  -d @target/blog/devto.json
```

**Result:** Tests automatically published as blog posts. 📝

---

### How-To: Generate OpenAPI Specs from Tests

**Goal:** Auto-generate Swagger/OpenAPI documentation.

```java
@Test
void documentApiWithOpenApi() {
    OpenApiCollector collector = new OpenApiCollector();

    Response resp = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/users"))
            .addQueryParameter("page", "1")
            .addQueryParameter("limit", "10"));

    collector.recordExchange(resp.request(), resp);

    OpenApiSpec spec = collector.buildSpec("User API", "2.5.0");
    OpenApiWriter.writeJson(spec, new File("target/openapi.json"));
    OpenApiWriter.writeYaml(spec, new File("target/openapi.yaml"));
}
```

**Result:** Swagger UI automatically generated from test execution. 📖

---

### How-To: Test WebSocket Real-Time Features

**Goal:** Document bidirectional WebSocket communication.

```java
@Test
void documentWebSocketEvents() {
    sayNextSection("WebSocket Real-Time Events");

    WebSocketClient ws = new WebSocketClientImpl("ws://localhost:8080/api/events");
    WebSocketSession session = ws.connect();

    session.send("""
        {
            "action": "subscribe",
            "channels": ["user.created", "user.updated"]
        }
        """);

    sayCode("""
        {
            "action": "subscribe",
            "channels": ["user.created", "user.updated"]
        }
        """, "json");

    WebSocketMessage event = session.receive(Duration.ofSeconds(5));
    sayJson(Map.of("event_received", event.getPayload()));

    session.close();
    sayNote("Connections persist until explicitly closed.");
}
```

**Result:** Documented real-time API behavior. 🔌

---

### How-To: Create Multi-Format Output from Single Test

**Goal:** Generate Markdown, PDF, Blog post, OpenAPI, and Slides from one test run.

```java
// Create a MultiRenderMachine that chains formats:
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl(),              // → Markdown
    new RenderMachineLatex(...),         // → PDF
    new BlogRenderMachine(...),          // → Blog posts
    new SlideRenderMachine(...),         // → Reveal.js slides
    // new OpenApiCollector(...)          // → OpenAPI spec
);

documentAssembler.addRenderMachine(multi);
```

**Result:** A single test generates docs in 4+ formats. 🎨

---

## 📖 Reference — API & Configuration

### All `say*` Methods

| Method | Purpose | Markdown Output |
|--------|---------|---|
| `say(String)` | Paragraph | Standard paragraph |
| `sayNextSection(String)` | H1 heading + TOC | `# Title` |
| `sayCode(String, lang)` | Syntax block | `` ``` lang ... ``` `` |
| `sayTable(String[][])` | Data table | Markdown table with borders |
| `sayJson(Object)` | Pretty JSON | `` ```json ... ``` `` |
| `sayWarning(String)` | Alert box | `> [!WARNING] ...` |
| `sayNote(String)` | Info box | `> [!NOTE] ...` |
| `sayKeyValue(Map)` | Key-value table | 2-column metadata table |
| `sayUnorderedList(List)` | Bullets | `- item 1` / `- item 2` |
| `sayOrderedList(List)` | Numbered | `1. item` / `2. item` |
| `sayAssertions(Map)` | Test matrix | Check/Result table |
| `sayAndMakeRequest(Request)` | HTTP + doc | Full request/response capture |

### Request Builder (HttpClient5)

```java
Request.GET()                              // HEAD, POST, PUT, PATCH, DELETE
    .url(Url.host("http://localhost:8080").path("/api/users").uri())
    .contentTypeApplicationJson()
    .addHeader("Accept", "application/json")
    .addQueryParameter("page", "1")
    .payload(userDto)                      // Auto-serialized by Jackson
    .withAuth(oauth2Manager)               // OAuth2 token
    .withAuth(BearerTokenAuth.of("jwt")) // JWT
    .withAuth(ApiKeyAuth.of("X-API-Key", "key123"))  // Custom header
    .followRedirects(true)
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(10));
```

### Response Handler

```java
response.httpStatus()                      // int: 200, 404, 500, etc.
response.payloadAsString()                 // Raw UTF-8 string
response.payloadAsPrettyString()           // Pretty-printed JSON/XML
response.payloadAs(UserDto.class)          // Auto-detect JSON/XML
response.payloadJsonAs(new TypeReference<List<User>>(){})  // Generic types
response.headers()                         // Map<String, String>
```

### Output Locations

```
target/
├── docs/                                  # Markdown output
│   ├── index.md
│   └── test-results/YourTest.md
├── pdf/                                   # LaTeX/PDF (if enabled)
│   └── YourTest.pdf
├── slides/                                # Reveal.js presentations
│   └── presentation.html
├── blog/                                  # Blog export queue
│   ├── devto.json
│   └── queue.json
└── openapi.json / openapi.yaml            # Swagger specs
```

### LaTeX/PDF Templates

Choose your academic format:

```java
// ACM Conference Paper
new RenderMachineLatex(LatexTemplate.ACM_CONFERENCE)

// arXiv Preprint (Computer Science)
new RenderMachineLatex(LatexTemplate.ARXIV)

// IEEE Journal
new RenderMachineLatex(LatexTemplate.IEEE)

// Nature Magazine
new RenderMachineLatex(LatexTemplate.NATURE)

// US Patent
new RenderMachineLatex(LatexTemplate.US_PATENT)
```

### Blog Platform Templates

Export directly to blogging platforms:

```java
new BlogRenderMachine(new DevToTemplate())        // Dev.to
new BlogRenderMachine(new MediumTemplate())       // Medium
new BlogRenderMachine(new HashnodeTemplate())     // Hashnode
new BlogRenderMachine(new LinkedInTemplate())     // LinkedIn
new BlogRenderMachine(new SubstackTemplate())     // Substack
```

### Required Dependencies

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.5.0</version>
    <scope>test</scope>
</dependency>

<!-- Choose ONE: JUnit 4 or JUnit 5 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
    <scope>test</scope>
</dependency>
<!-- OR -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.3</version>
    <scope>test</scope>
</dependency>
```

### Compiler Configuration (Java 25 + Preview)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>25</release>
        <compilerArgs>--enable-preview</compilerArgs>
    </configuration>
</plugin>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

---

## 💡 Explanation — Why DTR?

### The Problem: Stale Documentation

Traditional documentation:
- ❌ Manually written by humans
- ❌ Falls out of sync when code changes
- ❌ Requires separate tools (Swagger, Javadoc, blogs)
- ❌ No guarantee examples actually work

### The Solution: Living Documentation

DTR (Documentation Testing Runtime) generates docs from **actual test execution**:
- ✅ Always reflects current code behavior
- ✅ Examples are guaranteed to work (they're your tests)
- ✅ Single source of truth: your test suite
- ✅ Outputs multiple formats from one test run
- ✅ Version-controlled, diffable, portable Markdown

### Example: Auto-Documenting API Changes

```java
// When you change an API response
@Test
void documentUserResponse() {
    var user = new User("Alice", "alice@example.com", "admin");
    sayJson(user);  // Auto-generates updated JSON in docs
}
```

**Next test run:**
```bash
mvnd test  # Docs automatically regenerate with new response format
```

**Result:** Your API docs stay forever in sync with code. No manual updates needed.

---

### Why Java 25?

DTR targets Java 25 idioms for concise, expressive tests:

| Java 25 Feature | Use in DTR |
|---|---|
| **Records** | Immutable test data (Product, User, etc.) |
| **Sealed classes** | Type-safe test result hierarchies |
| **Pattern matching** | Clean result extraction without casts |
| **Virtual threads** | Concurrent test execution without overhead |
| **Text blocks** | Readable SQL/JSON examples in tests |
| **Switch expressions** | Exhaustive conditional logic in assertions |

---

### Why Multiple Output Formats?

Different audiences need different formats:

| Format | Best For |
|---|---|
| **Markdown** | Version control, GitHub, Git diffs, documentation sites |
| **PDF (LaTeX)** | Academic papers, conferences, formal publications |
| **Blog posts** | Developer community outreach (Dev.to, Medium) |
| **OpenAPI** | Automated API client generation, Swagger UI |
| **HTML slides** | Technical presentations, live demos |

A single test generates all of them. 🎯

---

## 🚀 Getting Started

### 1. Install Java 25 and Maven 4

```bash
# Install Java 25 LTS
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64

# Verify
java -version          # Shows: openjdk version "25.0.2"
mvnd --version         # Shows: Maven 4.0.0-rc-5
```

### 2. Add DTR to your `pom.xml`

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.5.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.3</version>
    <scope>test</scope>
</dependency>
```

### 3. Create your first test

```java
import io.github.seanchatmangpt.dtr.DocTester;
import org.junit.jupiter.api.Test;

public class MyFirstDocTest extends DocTester {
    @Test
    void myFirstDoc() {
        sayNextSection("Hello World");
        say("This is my first living documentation test.");
        sayJson(Map.of("status", "success", "message", "It works!"));
    }
}
```

### 4. Run it

```bash
mvnd test
```

### 5. View output

```bash
cat target/docs/test-results/MyFirstDocTest.md
```

**Done!** You've created your first living documentation. 📚

---

## 📊 Module Structure

```
dtr/
├── dtr-core/
│   ├── pom.xml
│   └── src/main/java/io/github/seanchatmangpt/dtr/
│       ├── DocTester.java              # Base test class with say* methods
│       ├── assembly/                   # Document assembly & indexing
│       ├── openapi/                    # OpenAPI/Swagger spec generation
│       ├── receipt/                    # Cryptographic integrity (blockchain receipts)
│       ├── render/                     # Multi-format rendering
│       │   ├── blog/                   # Blog export (Dev.to, Medium, etc.)
│       │   ├── slides/                 # Presentation generation (Reveal.js)
│       │   └── latex/                  # LaTeX/PDF with academic templates
│       ├── rendermachine/              # Core Markdown generation
│       ├── testbrowser/                # HTTP client (HttpClient5)
│       │   └── auth/                   # OAuth2, Bearer, API Key support
│       ├── websocket/                  # WebSocket (RFC 6455)
│       └── sse/                        # Server-Sent Events
└── dtr-integration-test/
    └── Full integration examples
```

---

## 🔗 See Also

- **[CLAUDE.md](./CLAUDE.md)** — Comprehensive project guide for contributors
- **[Documentation](./docs/)** — Full API documentation and guides
- **[Examples](./dtr-integration-test/src/test/java/)** — Working examples

---

## 💬 Questions?

- **Bug reports:** [GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)
- **Discussions:** [GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions)
- **Contributing:** See [CONTRIBUTING.md](./CONTRIBUTING.md)

---

## 👥 Contributors & Project History

**DTR (Documentation Testing Runtime)** is maintained by:

- **[Sean Chatman](https://github.com/seanchatmangpt)** (@seanchatmangpt) — Creator, architect, and lead maintainer (modern Java 25+ reimplementation)

### Original Project Foundation

DTR is a modern reimplementation and evolution of the original **[doctester](https://github.com/r10r-org/doctester)** project by the r10r organization. The original project provided the foundational concept of test-driven documentation generation. DTR modernizes this approach for:

- **Java 25 & Beyond** — Leveraging latest JDK features (records, sealed classes, pattern matching, virtual threads, text blocks)
- **Maven Central Distribution** — Professional package management and easy adoption
- **Enhanced Architecture** — Multi-format output (Markdown, LaTeX, HTML, OpenAPI, Blog exports)
- **Current Maintenance** — Active development with modern tooling and best practices

**Thank you** to the r10r-org team for the original vision that inspired this project.

### Acknowledgments

DTR builds on innovative technology including:
- **Java 25 Preview Features** — Records, sealed classes, pattern matching, virtual threads, text blocks
- **Apache HttpClient 5** — Reliable HTTP testing foundation
- **Jackson 2.x** — Flexible JSON/XML serialization
- **Guava 33.x** — Essential utilities for the JVM
- **JUnit 5 & JUnit Platform** — Industry-standard Java testing framework
- **Original doctester Project** — Foundational concept and inspiration
- **The Java Community** — For feedback, testing, and adoption

### Getting Involved

Contributions, bug reports, and feature requests are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

---

## 📜 License

Apache License 2.0. See [`LICENSE`](./LICENSE).

# DTR (Documentation Testing Runtime) — Markdown Living Documentation for Java 26+

> **Generate living documentation as your tests execute.** Every test run regenerates docs in multiple formats (Markdown, PDF, LaTeX, Blog posts, OpenAPI specs) from live behavior—keeping docs forever in sync with reality.

[![CI Gate](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml)
[![Quality Gates](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seanchatmangpt.dtr/dtr-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/versions)
[![Java 26](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Latest:** `2.5.0` | **Java:** 26+ (with `--enable-preview`) | **Maven:** `io.github.seanchatmangpt.dtr:dtr-core`

---

## ⚡ 5-Minute Quick Start

### 1. Create a test that documents itself

Save as `src/test/java/example/FirstDocTest.java`:

```java
package example;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

public class FirstDocTest extends DtrTest {

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

## Architecture

DTR has four main components:

- **DtrTest / DtrExtension** — Test lifecycle integration. `DtrTest` is a base class; `DtrExtension` is a JUnit 5 extension for parameter injection. Both wire up the documentation pipeline around each test method.
- **DtrContext** — The documentation API surface. Exposes all `say*` methods and HTTP request/response utilities that tests call to record documentation events.
- **RenderMachine** — The rendering pipeline. Captures `say*` events as they occur and formats them into structured output. `MultiRenderMachine` dispatches to multiple engines in parallel.
- **Output engines** — Format-specific writers: Markdown (`.md`), LaTeX (`.tex`), HTML (`.html`), JSON (`.json`), and more.

```
JUnit 5 Test
    │
    ▼
DtrContext.say*()          ← your test calls these
    │
    ▼
RenderMachine              ← captures & formats events
    │
    ├──► Markdown (.md)
    ├──► LaTeX (.tex)
    ├──► HTML (.html)
    └──► JSON (.json)
```

Output lands in `target/docs/test-results/` by default.

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

### Compiler Configuration (Java 26 + Preview)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>26</release>
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

### Why Java 26?

DTR targets Java 26 idioms for concise, expressive tests:

| Java 26 Feature | Use in DTR |
|---|---|
| **Records** | Immutable test data (Product, User, etc.) |
| **Sealed classes** | Type-safe test result hierarchies |
| **Pattern matching** | Clean result extraction without casts |
| **Virtual threads** | Concurrent test execution without overhead |
| **Text blocks** | Readable SQL/JSON examples in tests |
| **Switch expressions** | Exhaustive conditional logic in assertions |
| **String Templates** (Preview) | Dynamic SQL/JSON generation |
| **Scoped Values** (Preview) | Thread-safe context passing |

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

### 1. Install Java 26+ and Maven 4

```bash
# Install Java 26+ (EA) with --enable-preview
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Alternatively, use SDKMAN for latest Java 26 EA
curl -s "https://get.sdkman.io" | bash
sdk install java 26.ea.13-graal

# Verify
java -version          # Shows: openjdk version "26-ea"
mvnd --version         # Shows: Maven 4.0.0-rc-5+
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
import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

public class MyFirstDocTest extends DtrTest {
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
│       ├── DtrTest.java                # Base test class with say* methods
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

## Glossary

**DtrTest** — Base class for documentation tests. Extend this to get `say*` methods injected directly into your test class without parameter declarations.

**DtrExtension** — JUnit 5 extension (`@ExtendWith(DtrExtension.class)`). Use this with a `DtrContext ctx` parameter on your test method instead of extending `DtrTest`. Preferred when you cannot or do not want to extend a base class.

**DtrContext** — The documentation API. Provides all `say*` methods plus request/response utilities (`sayAndMakeRequest`, `sayAndAssertThat`). The single object your tests interact with.

**RenderMachine** — The rendering pipeline. Captures `say*` events in order and formats them into a complete output document. Each format (Markdown, LaTeX, HTML, JSON) has its own implementation.

**MultiRenderMachine** — Dispatches to multiple render machines in parallel. Use this to generate Markdown, LaTeX, and blog exports from a single test run.

**say\* methods** — The documentation API: `sayNextSection`, `say`, `sayCode`, `sayTable`, `sayJson`, `sayWarning`, `sayNote`, `sayKeyValue`, `sayUnorderedList`, `sayOrderedList`, `sayAssertions`, `sayAndMakeRequest`. Each call both records a documentation event and (for assertion/request methods) executes the underlying operation.

**Living Documentation** — Documentation generated directly from passing tests. It is always accurate because the documentation only exists when the tests pass. If behavior changes, the next test run regenerates the docs to match.

---

## Troubleshooting

### "0 tests run" in Maven output

Add `junit-jupiter-engine` to your test dependencies. Surefire 3.x requires it explicitly — `junit-jupiter` alone is not sufficient.

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
</dependency>
```

### "cannot find symbol: class DocTester"

Use `DtrTest` (not `DocTester`). The correct import is:

```java
import io.github.seanchatmangpt.dtr.DtrTest;
```

`DocTester` was the class name in the original doctester project. DTR uses `DtrTest`.

### "Documentation file not written"

DTR throws `RuntimeException` when file writes fail. Check that the `dtr.output.dir` system property is set and the target directory is writable. By default, output goes to `target/docs/test-results/` — ensure your build has write access there.

### "Too many authentication attempts" from Maven Central

Run the proxy before your Maven command:

```bash
python3 maven-proxy-auth.py &
```

Then add to `.mvn/jvm.config`:

```
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
```

---

## 🔗 See Also

- **[CLAUDE.md](./CLAUDE.md)** — Comprehensive project guide for contributors
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — Contribution guidelines and development setup
- **[Documentation](./docs/)** — Full API documentation and guides
- **[Examples](./dtr-integration-test/src/test/java/)** — Working examples

---

## 🧪 Local Testing with act

Test GitHub Actions workflows locally using [act](https://github.com/nektos/act) before pushing:

```bash
# Install act (macOS)
brew install act

# Install act (Linux)
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run CI gate workflow locally
act -j quality-check
act -j test-coverage
act -j build-verification

# Run all jobs with Java 26
act -j build-verification --matrix java-version:26

# Dry run to see what would execute
act -n
```

**Important:** The CI workflow uses SDKMAN to install Java 26.ea.13-graal, which is compatible with `act`. Ensure you have Docker installed and running.

---

## 🚀 Deploying to Maven Central

### Prerequisites

1. **Sonatype OSSRH Account** - Sign up at [central.sonatype.com](https://central.sonatype.com)
2. **GPG Key** - Generate and upload your public key to a keyserver:
   ```bash
   gpg --gen-key
   gpg --list-keys
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

### GitHub Secrets Configuration

Set these secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

| Secret | Description |
|--------|-------------|
| `CENTRAL_USERNAME` | Sonatype OSSRH username |
| `CENTRAL_TOKEN` | Sonatype OSSRH token |
| `GPG_PRIVATE_KEY` | Your GPG private key (base64 encoded) |
| `GPG_PASSPHRASE` | GPG key passphrase |
| `GPG_KEY_ID` | Last 8 characters of your GPG key ID |

### Deployment Process

1. **Create a release tag:**
   ```bash
   git tag -a v2.5.0 -m "Release v2.5.0"
   git push origin v2.5.0
   ```

2. **CI Gate automatically:**
   - Runs all quality checks
   - Verifies build on Java 21, 22, and 26
   - Checks required secrets are present
   - Triggers deployment workflow on tag push

3. **Manual deployment (if needed):**
   ```bash
   # Set env vars for Maven Central
   export CENTRAL_USERNAME="your-username"
   export CENTRAL_TOKEN="your-token"
   export GPG_PASSPHRASE="your-passphrase"

   # Deploy to Maven Central
   mvnd clean deploy -Drelease=26
   ```

4. **Verify deployment:**
   - Check [Maven Central](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)
   - Allow 10-15 minutes for synchronization

### Automatic Deployment Workflow

The `.github/workflows/ci-gate.yml` workflow automatically handles deployments when you push a version tag (`v*`):

- ✅ Runs all quality gates
- ✅ Verifies Java 21, 22, and 26 compatibility
- ✅ Checks deployment prerequisites (secrets, tag format)
- ✅ Triggers deployment to Maven Central
- ✅ Generates quality gate report

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

- **Java 26 & Beyond** — Leveraging latest JDK features (records, sealed classes, pattern matching, virtual threads, text blocks, string templates)
- **Maven Central Distribution** — Professional package management and easy adoption
- **Enhanced Architecture** — Multi-format output (Markdown, LaTeX, HTML, OpenAPI, Blog exports)
- **CI/CD Integration** — GitHub Actions workflows for quality gates and automated deployment
- **Current Maintenance** — Active development with modern tooling and best practices

**Thank you** to the r10r-org team for the original vision that inspired this project.

### Acknowledgments

DTR builds on innovative technology including:
- **Java 26 Preview Features** — Records, sealed classes, pattern matching, virtual threads, text blocks, string templates
- **Apache HttpClient 5** — Reliable HTTP testing foundation
- **Jackson 2.x** — Flexible JSON/XML serialization
- **Guava 33.x** — Essential utilities for the JVM
- **JUnit 5 & JUnit Platform** — Industry-standard Java testing framework
- **GitHub Actions & act** — CI/CD infrastructure and local testing
- **Original doctester Project** — Foundational concept and inspiration
- **The Java Community** — For feedback, testing, and adoption

### Getting Involved

Contributions, bug reports, and feature requests are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

---

## 📜 License

Apache License 2.0. See [`LICENSE`](./LICENSE).

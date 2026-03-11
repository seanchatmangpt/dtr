# DocTester — Claude Code Project Guide

## Project Overview

DocTester is a Java testing framework that generates Bootstrap-styled HTML documentation while running JUnit tests. It provides a fluent API for making HTTP requests, asserting responses, and automatically rendering the results as living API documentation.

**Current version:** `1.1.12-SNAPSHOT`
**License:** Apache 2.0
**Maven coordinates:** `org.r10r:doctester-core`

**Toolchain (non-negotiable):**
- **Java 25** (LTS) — must use `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`
- **Maven 4** — use `mvn` (Maven 4.0.0-rc-5+) or `mvnd` (Maven Daemon) for all builds
- **mvnd 2** — preferred over plain `mvn` for speed; installed at `/opt/mvnd/bin/mvnd`
- **`--enable-preview`** — always active for both compilation and test runs

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
│       │   ├── testbrowser/              # HTTP client layer
│       │   │   ├── TestBrowser.java      # Interface
│       │   │   ├── TestBrowserImpl.java  # Apache HttpClient implementation
│       │   │   ├── Request.java          # Fluent request builder
│       │   │   ├── Response.java         # Response wrapper + deserialization
│       │   │   ├── Url.java              # Fluent URL builder
│       │   │   ├── HttpConstants.java    # Header/content-type constants
│       │   │   └── PayloadUtils.java     # JSON/XML pretty-print utilities
│       │   └── rendermachine/            # HTML documentation generator
│       │       ├── RenderMachine.java         # Interface (extends RenderMachineCommands)
│       │       ├── RenderMachineCommands.java  # say* API contract
│       │       ├── RenderMachineImpl.java      # Bootstrap HTML generation
│       │       └── RenderMachineHtml.java      # HTML template constants
│       └── test/java/org/r10r/doctester/
│           ├── DocTesterTest.java
│           ├── DocTesterLifecycleTest.java
│           ├── StressTest.java / StressBreakpointTest.java / StressFinalTest.java
│           ├── ValidateAndStressTest.java
│           ├── rendermachine/RenderMachineImplTest.java
│           └── testbrowser/
│               ├── RequestTest.java / ResponseTest.java / UrlTest.java
│               └── testmodels/Article.java, ArticlesDto.java, User.java
└── doctester-integration-test/       # Full integration tests (WAR)
    ├── pom.xml
    └── src/
        ├── main/java/
        │   ├── conf/          # Ninja framework config (Module, Routes, ServletModule)
        │   ├── controllers/   # ApiController, ApplicationController, etc.
        │   ├── dao/           # ArticleDao, UserDao, SetupDao (JPA + H2)
        │   ├── models/        # Article, User JPA entities + DTOs
        │   └── filters/       # AdminFilter, LoggerFilter
        └── test/java/controllers/
            ├── ApiControllerDocTest.java     # Primary documentation test
            ├── ApiControllerMockTest.java
            ├── ApiControllerTest.java
            └── utils/
                ├── NinjaApiDoctester.java    # DocTester subclass for Ninja framework
                └── NinjaTest.java
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

## Architecture — How DocTester Works

DocTester bridges JUnit 4 test execution with HTML documentation generation via three layers:

### 1. Request Layer (`testbrowser/`)
```
Request.GET()                     <- Fluent builder
  .url(testServerUrl().path("/api/users"))
  .addHeader("Accept", "application/json")
  .payload(myDto)
```
- `Request` — immutable builder for HEAD/GET/DELETE/POST/PUT/PATCH
- `Url` — fluent URL builder wrapping Apache `URIBuilder`
- `HttpConstants` — canonical header and content-type string constants

### 2. Execution Layer (`TestBrowserImpl`)
- Wraps Apache `DefaultHttpClient` (HttpComponents 4.5)
- Persists cookies across requests in `CookieStore`
- Handles multipart file uploads, form parameters, redirect following
- Serializes/deserializes payloads via Jackson (JSON + XML)

### 3. Documentation Layer (`rendermachine/`)
- `RenderMachineImpl` captures every `say*` call and HTTP exchange
- Generates Bootstrap 3-styled HTML to `target/site/doctester/<TestClassName>.html`
- Maintains a table of contents from `sayNextSection()` headings
- Produces an index page at `target/site/doctester/index.html` after all tests
- Copies Bootstrap 3.0.0 and jQuery 1.9.0 assets from classpath resources

### Lifecycle
```
@Test method runs
  → sayNextSection() / say()      → RenderMachine buffers HTML
  → sayAndMakeRequest(request)    → TestBrowserImpl executes HTTP → RenderMachine logs request+response
  → sayAndAssertThat(...)         → Hamcrest assertion → green/red panel in HTML
  ↓
TestWatcher.finished()            → RenderMachine.finishAndWriteOut() writes <TestClass>.html
  ↓
@AfterClass / finishDocTest()     → Index page generated with links to all docs
```

---

## Key Class Reference

### `DocTester` (abstract base class)
Extend this in your test classes. Provides:

| Method | Purpose |
|--------|---------|
| `say(String text)` | Render a paragraph in the doc |
| `sayNextSection(String title)` | Render H1 heading + TOC entry |
| `sayRaw(String html)` | Inject raw HTML |
| `sayAndMakeRequest(Request)` | Execute HTTP and document request+response → returns `Response` |
| `makeRequest(Request)` | Execute HTTP silently (no documentation) → returns `Response` |
| `sayAndAssertThat(String, T, Matcher<T>)` | Assert with Hamcrest + document result |
| `sayAndGetCookies()` | Document and return current cookies |
| `sayAndGetCookieWithName(String)` | Document and return named cookie |
| `testServerUrl()` | **Must override** — returns base `Url` for the test server |
| `finishDocTest()` | Static — call in `@AfterClass` to generate index page |

### `Request` (fluent builder)
```java
Request.GET()                          // HEAD, GET, DELETE, POST, PUT, PATCH
    .url(Url)                          // or .url(URI)
    .contentTypeApplicationJson()      // sets Content-Type header
    .addHeader(String, String)
    .addFormParameter(String, String)
    .payload(Object)                   // serialized by Jackson
    .addFileToUpload(String, File)
    .followRedirects(boolean)
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

Tests extend `DocTester` and use the `say*` fluent API:

```java
class MyApiDocTest extends DocTester {

    @Test
    void testGetUsers() {
        sayNextSection("User API");
        say("GET /api/users returns all active users");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson());

        sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));

        List<UserDto> users = response.payloadJsonAs(new TypeReference<>() {});
        sayAndAssertThat("At least one user returned", true, is(users.size() > 0));
    }

    @Test
    void testCreateUser() {
        sayNextSection("Create User");

        UserDto newUser = new UserDto("alice", "alice@example.com");
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        sayAndAssertThat("Created with 201", 201, equalTo(response.httpStatus()));
    }

    @AfterClass
    public static void afterClass() {
        finishDocTest();  // generates index.html
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

### Integration Test Pattern (Ninja Framework)
The `doctester-integration-test` module uses `NinjaApiDoctester`, which extends `DocTester` and integrates with the Ninja framework's embedded Jetty server:

```java
class ApiControllerDocTest extends NinjaApiDoctester {
    // server starts automatically via NinjaTest
    // testServerUrl() returns the embedded server URL
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

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.google.guava:guava` | 18.0 | Utilities |
| `commons-fileupload:commons-fileupload` | 1.3.1 | Multipart file upload |
| `org.apache.httpcomponents:httpclient` | 4.5 | HTTP client |
| `org.apache.httpcomponents:httpmime` | 4.5 | Multipart HTTP |
| `com.fasterxml.jackson.core:jackson-core` | 2.5.4 | JSON serialization |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | 2.5.4 | XML serialization |
| `com.fasterxml.woodstox:woodstox-core` | 5.0.1 | XML processing |
| `org.slf4j:slf4j-api` | 1.7.12 | Logging API |
| `junit:junit` | 4.12 | Test framework (provided scope) |
| `org.mockito:mockito-core` | 4.11.0 | Mocking (test scope) |

> **Note:** JUnit is `provided` scope — consumers must provide JUnit themselves.

---

## Maven 4 — Best Practices

- Use `<release>25</release>` in compiler plugin (not `<source>`/`<target>`)
- Maven 4 uses `.mvn/maven.config` for persistent build flags
- `mvnd` (Maven Daemon) configured via `~/.m2/mvnd.properties`
- Prefer `mvnd` locally; CI can use `mvn -T 1C`
- Maven 4 supports improved multi-module build ordering
- `--enable-preview` must be in both compiler config AND surefire `<argLine>`

### Plugin Versions (root POM)
| Plugin | Version |
|--------|---------|
| `maven-compiler-plugin` | 3.13.0 |
| `maven-surefire-plugin` | 3.5.2 |
| `maven-enforcer-plugin` | 3.5.0 |
| `central-publishing-maven-plugin` | (release profile) |
| `maven-source-plugin` | (release profile) |
| `maven-javadoc-plugin` | (release profile) |
| `maven-gpg-plugin` | (release profile) |
| `maven-license-plugin` | (license profile) |

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

## HTML Documentation Output

Generated docs land in `target/site/doctester/` (relative to the module under test):

```
target/site/doctester/
├── index.html                    # Index linking all doc-tests
├── <TestClassName>.html          # Per-test-class documentation
├── bootstrap/
│   ├── css/bootstrap.min.css
│   └── js/bootstrap.min.js
├── jquery/jquery-1.9.0.min.js
└── custom_doctester_stylesheet.css  # Optional custom CSS (place in src/main/resources)
```

Custom CSS: put `custom_doctester_stylesheet.css` in `src/main/resources/` of the module running the tests. It is automatically copied and linked.

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

| Symptom | Fix |
|---------|-----|
| `--enable-preview` compile errors | Ensure both compiler plugin AND surefire `<argLine>` include `--enable-preview` |
| Daemon not starting | `mvnd --stop && mvnd compile` to restart |
| Wrong Java version | `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` |
| Maven 3 being invoked | Never use `./mvnw`; use `mvnd` or `/opt/apache-maven-4.0.0-rc-5/bin/mvn` |
| Tests fail with JAXB errors | `doctester-integration-test` adds `jakarta.xml.bind:jakarta.xml.bind-api:2.3.3` — ensure it's present |
| H2 connection errors | Integration tests use H2 in-memory mode; URL format is `jdbc:h2:mem:...` |
| Dependency conflict | Run `mvnd dependency:tree -pl <module>` and add `<exclusions>` as needed |

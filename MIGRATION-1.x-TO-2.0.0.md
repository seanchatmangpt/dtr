# DTR 1.x to 2.0.0 Migration Guide

**Version:** 2.0.0
**Release Date:** 2026-03-10
**Java Requirement:** Java 25 (LTS)

---

## Overview

DTR 2.0.0 is a major release with **breaking changes** that modernize the framework for contemporary Java and improve documentation portability. This guide helps you upgrade from 1.x (1.1.12) to 2.0.0.

### Key Changes at a Glance

| Aspect | 1.x | 2.0.0 | Breaking? |
|--------|-----|-------|-----------|
| Output Format | Bootstrap HTML | Markdown (portable) | **YES** |
| Output Location | `target/site/doctester/` | `docs/test/` | **YES** |
| Java Version | 1.8+ | **25 (LTS only)** | **YES** |
| Annotations | Manual method calls | `@DocSection`, `@DocDescription`, etc. | No (optional) |
| WebSocket Support | None | Full support | No (new) |
| Server-Sent Events | None | Full support | No (new) |
| OpenAPI Generation | None | Built-in | No (new) |
| Authentication | Basic auth only | Multiple providers | No (new) |

---

## Breaking Changes

### 1. Output Format: HTML → Markdown

**Version 1.x:**
```
target/site/doctester/
├── index.html
├── ApiControllerDocTest.html
├── bootstrap/
└── jquery/
```

**Version 2.0.0:**
```
docs/test/
├── README.md          (index)
├── ApiControllerDocTest.md
└── (OpenAPI specs if generated)
```

**Why:** Markdown is portable, version-control friendly, and works natively on GitHub. No external CSS/JS dependencies.

**Migration Impact:**
- Update CI/CD to look for docs in `docs/test/` instead of `target/site/doctester/`
- If you deploy HTML docs, render Markdown via a static site generator (Jekyll, MkDocs, Hugo, etc.)
- No Javadoc configuration needed—Markdown integrates directly into your README

### 2. Java Version: 1.8+ → 25 (LTS)

**Version 1.x:** Supports Java 1.8 through 21
**Version 2.0.0:** **Requires Java 25 (LTS)**

**Migration:**
```bash
# Verify your Java installation
java -version
# Must output: openjdk 25.x.x (or later 25 release)

# Update JAVA_HOME if needed
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64

# Update pom.xml (compiler plugin)
# OLD:
<source>11</source>
<target>11</target>

# NEW:
<release>25</release>
<compilerArgs>
  <arg>--enable-preview</arg>
</compilerArgs>
```

**Why:** Java 25 enables record-based DTOs, sealed hierarchies, pattern matching, and virtual threads for better concurrency.

### 3. Output Directory Structure

All documentation is now written to `docs/test/` at the module level:

**Version 1.x:**
```
mvnd test -pl dtr-core
# → target/site/doctester/ created in dtr-core/
```

**Version 2.0.0:**
```
mvnd test -pl dtr-core
# → docs/test/ created in dtr-core/
```

**For multi-module projects:** Each module gets its own `docs/test/` directory. Consider merging them in your build or documentation pipeline.

### 4. Bootstrap/jQuery Removed

**Version 1.x:**
```
target/site/doctester/
├── bootstrap/css/bootstrap.min.css
├── bootstrap/js/bootstrap.min.js
└── jquery/jquery-1.9.0.min.js
```

**Version 2.0.0:** No static assets—Markdown is self-contained.

**Migration:** If you had custom CSS in `src/test/resources/custom_doctester_stylesheet.css`, it is no longer used. Instead, customize via:
- Markdown front matter (YAML)
- Static site generator theme config
- Custom CSS in your site generator

---

## Upgraded Method Signatures

### Response Payload Deserialization

**Version 1.x:**
```java
// Old method names
ArticlesDto dto = response.payloadAsJson(ArticlesDto.class);
List<User> users = response.payloadJsonAs(new TypeReference<List<User>>(){});
```

**Version 2.0.0:**
```java
// Unified API (both still work—backward compatible at runtime)
ArticlesDto dto = response.payloadAs(ArticlesDto.class);  // auto-detects JSON/XML
List<User> users = response.payloadAs(new TypeReference<List<User>>(){});
```

**Migration:** The old methods still work (deprecated but functional). Use the new `payloadAs()` API for cleaner code.

### New Annotation-Based API

**Version 1.x — Manual sections:**
```java
@Test
public void testListArticles() {
    sayNextSection("Articles API");
    say("Retrieve all articles for the authenticated user.");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
}
```

**Version 2.0.0 — Annotation-driven (optional but recommended):**
```java
@Test
@DocSection("Articles API")
@DocDescription("Retrieve all articles for the authenticated user.")
public void testListArticles() {
    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
}
```

**Benefits:**
- Cleaner test body
- Automatic section heading and description before test execution
- Metadata available for tool integration

**Important:** Both approaches work together. If you use `@DocSection`, an implicit `sayNextSection()` is called before the test runs.

---

## New Features You Should Know About

### 1. Annotation-Based Metadata

#### `@DocSection`
Declares the H1 heading for a test method.

```java
@Test
@DocSection("User Registration")
public void testCreateUser() { ... }
```

#### `@DocDescription`
Multiple description paragraphs (each becomes a separate `<p>` in Markdown).

```java
@Test
@DocSection("Search API")
@DocDescription({
    "Search for articles by keyword.",
    "Supports pagination via 'page' and 'limit' query parameters.",
    "Returns up to 100 results per page."
})
public void testSearch() { ... }
```

#### `@DocCode`
Embed code blocks in documentation.

```java
@Test
@DocSection("API Example")
@DocCode(language = "bash", value = "curl -X GET http://localhost:8080/api/users")
public void testExample() { ... }
```

#### `@DocNote` and `@DocWarning`
Render advisory boxes.

```java
@Test
@DocSection("Authentication")
@DocWarning("Token expires after 1 hour. Request a new token before expiry.")
public void testTokenRefresh() { ... }
```

### 2. WebSocket Support

**New Package:** `io.github.seanchatmangpt.dtr.doctester.websocket`

```java
// Create a WebSocket session
WebSocketClient client = new WebSocketClientImpl();
WebSocketSession session = client.connect(
    testServerUrl().path("/ws/chat").uri(),
    new WebSocketMessage("Hello", "text")
);

// Send and receive messages
session.send(new WebSocketMessage("Hello, server!", "text"));
WebSocketMessage response = session.receive();

// Documented in your test output
sayAndAssertThat("Server echoes the message",
    "Hello, server!",
    equalTo(response.payload()));
```

### 3. Server-Sent Events (SSE) Support

**New Package:** `io.github.seanchatmangpt.dtr.doctester.sse`

```java
// Subscribe to SSE stream
SseClient sseClient = new SseClientImpl();
SseSubscription subscription = sseClient.subscribe(
    testServerUrl().path("/api/events").uri()
);

// Listen for events
SseEvent event = subscription.nextEvent(Duration.ofSeconds(5));
sayAndAssertThat("Event type is 'message'",
    "message",
    equalTo(event.eventType()));

subscription.unsubscribe();
```

### 4. OpenAPI Specification Generation

**New Package:** `io.github.seanchatmangpt.dtr.doctester.openapi`

Automatically generate OpenAPI 3.0 specs from your DocTests:

```java
// In your DTR base class or helper
OpenApiCollector collector = new OpenApiCollector();

// Your tests automatically record HTTP calls
// After testing:
OpenApiSpec spec = collector.buildSpec("My API", "1.0.0");
OpenApiWriter.write(spec, OutputFormat.YAML);  // → target/site/doctester/openapi.yaml
```

**Output:** Machine-readable OpenAPI specs for Swagger UI, code generation, etc.

### 5. Advanced Authentication Providers

**New Package:** `io.github.seanchatmangpt.dtr.doctester.auth`

Instead of manual header manipulation:

```java
// Version 1.x — Manual
Request.GET()
    .url(...)
    .addHeader("Authorization", "Bearer " + token)

// Version 2.0.0 — Providers
Request.GET()
    .url(...)
    .auth(BearerTokenAuth.of(token))
```

**Built-in providers:**
- `BasicAuth` — HTTP Basic Authentication
- `BearerTokenAuth` — OAuth 2.0 Bearer tokens
- `ApiKeyAuth` — Custom API key headers
- `JwtAuth` — JSON Web Tokens with auto-refresh

---

## Step-by-Step Migration

### Step 1: Update Java Version

**In your root `pom.xml`:**

```xml
<properties>
    <!-- OLD -->
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>

    <!-- NEW -->
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

**In `maven-compiler-plugin`:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>25</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

**In `maven-surefire-plugin`:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

### Step 2: Update DTR Dependency

**In your test module `pom.xml`:**

```xml
<!-- OLD -->
<dependency>
    <groupId>org.doctester</groupId>
    <artifactId>dtr-core</artifactId>
    <version>1.1.12</version>
    <scope>test</scope>
</dependency>

<!-- NEW -->
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

### Step 3: Update CI/CD Output Path

**GitHub Actions Example:**

```yaml
# OLD
- name: Publish Documentation
  run: |
    if [ -d "target/site/doctester" ]; then
      cp -r target/site/doctester ./docs
    fi

# NEW
- name: Publish Documentation
  run: |
    if [ -d "target/docs" ]; then
      cp -r target/docs ./site
    fi
```

### Step 4: Refactor Test Class (Optional but Recommended)

**Before (Version 1.x):**

```java
public class UserApiDocTest extends DTR {

    @Test
    public void testListUsers() {
        sayNextSection("Get All Users");
        say("Retrieve a list of all registered users.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson());

        sayAndAssertThat("HTTP 200", 200, equalTo(response.httpStatus()));
    }

    @Test
    public void testCreateUser() {
        sayNextSection("Create User");
        say("Create a new user account.");

        User newUser = new User("alice@example.com", "alice");
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        sayAndAssertThat("HTTP 201", 201, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**After (Version 2.0.0):**

```java
public class UserApiDocTest extends DTR {

    @Test
    @DocSection("Get All Users")
    @DocDescription("Retrieve a list of all registered users.")
    public void testListUsers() {
        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson());

        sayAndAssertThat("HTTP 200", 200, equalTo(response.httpStatus()));
    }

    @Test
    @DocSection("Create User")
    @DocDescription("Create a new user account.")
    public void testCreateUser() {
        User newUser = new User("alice@example.com", "alice");
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        sayAndAssertThat("HTTP 201", 201, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

### Step 5: Add Custom CSS (if applicable)

**Version 1.x:**
```
src/test/resources/custom_doctester_stylesheet.css
```

**Version 2.0.0:** Not applicable (Markdown is self-contained).

If you need styling, use a static site generator:
- **MkDocs** (Python) — add theme config in `mkdocs.yml`
- **Jekyll** (Ruby) — place Markdown in `_docs/`, style with themes
- **Docusaurus** (Node.js) — modern doc site generator

**Example with MkDocs:**

```bash
pip install mkdocs mkdocs-material
cd your-project
mkdocs new docs-site
cp docs/test/*.md docs-site/docs/
mkdocs serve  # Live preview
mkdocs build  # Static HTML in site/
```

---

## Testing the Migration

### Quick Validation

```bash
# 1. Verify Java 25
java -version  # Must show "openjdk 25.x.x"

# 2. Verify Maven 4
mvn --version  # Must show "Apache Maven 4.0.0-rc-5" or higher

# 3. Compile and run tests
mvnd clean verify

# 4. Check output directory
ls -la docs/test/
# Should show: README.md, TestClassName.md
```

### Compare 1.x vs 2.0.0 Output

**1.x Output Structure:**
```
target/site/doctester/
├── index.html              # Lists all test docs
├── UserApiDocTest.html     # Bootstrap-styled HTML
├── bootstrap/              # CSS/JS assets
└── jquery/
```

**2.0.0 Output Structure:**
```
docs/test/
├── README.md              # Lists all test docs (Markdown)
└── UserApiDocTest.md      # Clean, portable Markdown
```

**Same content, different format.** The Markdown is suitable for:
- Version control (Git diff-able)
- GitHub rendering (automatic preview)
- Static site generators
- PDF export (pandoc, etc.)

---

## Compatibility Notes

### Backward Compatibility

**Good news:**
- All existing `say*` method signatures still work
- `makeRequest` and `sayAndMakeRequest` unchanged
- `Request` and `Response` APIs are fully compatible
- `Url` builder methods unchanged

**Not backward compatible:**
- HTML output format (Markdown now)
- Output directory (`docs/test/` instead of `target/site/doctester/`)
- Java version requirement (25 only)

### Dependencies

**Version 1.x minimum:**
```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
</dependency>
```

**Version 2.0.0 supports both:**
```xml
<!-- Still supports JUnit 4 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
</dependency>

<!-- But also supports JUnit 5 (Jupiter) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.1</version>
</dependency>
```

---

## Troubleshooting

### Problem: "Java 25 not found"

**Error:**
```
[ERROR] COMPILATION ERROR
[ERROR] Java 25 or higher is required.
```

**Solution:**
```bash
# Install Java 25 (if not present)
sudo apt install openjdk-25-jdk  # Ubuntu/Debian
brew install openjdk@25           # macOS

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64

# Verify
java -version  # Must show "openjdk 25.x.x"
```

### Problem: "Markdown output is not generating"

**Error:**
```
Tests pass, but no docs/test/ directory appears.
```

**Solution:**
1. Ensure tests actually run (not skipped)
2. Check for compile errors (Java 25 preview features)
3. Verify `@AfterClass finishDocTest()` is called:

```java
@AfterClass
public static void afterTests() {
    DTR.finishDocTest();
}
```

### Problem: "Cannot parse Markdown in static site generator"

**Common issue:** Front matter or special formatting not supported.

**Solution:** Use a compatible generator:
- **MkDocs** — lightweight, Markdown-first
- **Docusaurus** — React-based, feature-rich
- **Hugo** — fast, supports multiple formats
- **GitHub Pages** — native Markdown support

---

## Migration Checklist

- [ ] Java 25 installed and `JAVA_HOME` set
- [ ] `pom.xml` updated with `<release>25</release>` and `--enable-preview`
- [ ] DTR dependency updated to 2.0.0
- [ ] Test classes compile without preview warnings
- [ ] Tests run and output Markdown to `docs/test/`
- [ ] CI/CD pipeline updated to use `docs/test/` path
- [ ] Documentation build process updated (MkDocs, Jekyll, etc.)
- [ ] Old `target/site/doctester/` references removed from scripts
- [ ] Team documentation updated

---

## What's Next?

### Explore New Features

1. **Use Annotations** — Refactor `sayNextSection()` calls to `@DocSection`
2. **Generate OpenAPI** — Enable `OpenApiCollector` for API specs
3. **Test WebSockets** — Add WebSocket tests to your suite
4. **Test SSE** — Document Server-Sent Events endpoints
5. **Try Java 25 Features** — Use records for DTOs, sealed hierarchies, etc.

### Upgrade Your Documentation Pipeline

1. Set up MkDocs or your preferred static site generator
2. Configure auto-build on test execution
3. Deploy generated Markdown as part of CI/CD
4. Link API docs from your main README

### Get Help

- **Official Docs:** https://github.com/seanchatmangpt/doctester (updated for 2.0.0)
- **Issue Tracker:** Report bugs or request features
- **Community:** Discuss on GitHub Discussions

---

## Release Notes — What Changed

### Major Features
- ✅ Markdown output format (portable, version-control friendly)
- ✅ Annotation-based API (`@DocSection`, `@DocDescription`, etc.)
- ✅ WebSocket support
- ✅ Server-Sent Events (SSE) support
- ✅ OpenAPI 3.0 specification generation
- ✅ JUnit 5 support (backward compatible with JUnit 4)
- ✅ Java 25 with `--enable-preview`
- ✅ Advanced authentication providers

### Minor Improvements
- 📈 Better error messages
- 📈 Improved Markdown rendering
- 📈 Virtual threads for async operations
- 📈 Records for DTOs
- 📈 Pattern matching in assertions

### Removed
- ❌ HTML/Bootstrap output (replaced with Markdown)
- ❌ jQuery/Bootstrap assets (no longer needed)
- ❌ Support for Java < 25

---

## Questions?

For detailed questions about specific features, see:
- [WebSocket Testing Guide](/docs/how-to/test-websockets.md)
- [OpenAPI Generation Guide](/docs/how-to/generate-openapi.md)
- [SSE Testing Guide](/docs/how-to/test-sse-endpoints.md)
- [Authentication Providers](/docs/reference/authentication.md)

Happy upgrading! 🚀

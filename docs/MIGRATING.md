# DTR Migration Guide

**Last Updated:** March 15, 2026

**Target Audience:** Users upgrading from previous DTR versions

---

## Table of Contents

1. [Overview](#overview)
2. [Historical Migration Guides](#historical-migration-guides)
3. [v2026.3.0 → v2026.4.0 (LOW IMPACT)](#v202630--v202640-low-impact)
4. [v2.5.x → v2.6.0 (HIGH IMPACT)](#v25x--v260-high-impact)
5. [v2.0.0 → v2.5.x](#v200--v25x)
6. [v1.x → v2.0.0](#v1x--v200)
7. [Feature Mapping Table](#feature-mapping-table)
8. [Troubleshooting Migrations](#troubleshooting-migrations)

---

## Overview

This guide helps you migrate between DTR versions. DTR uses **calendar versioning** (CalVer) starting from 2026.3.0, which corresponds to semantic version 2.6.0.

### Version Mapping

| CalVer | Semantic | Status | Key Changes |
|--------|----------|--------|-------------|
| 2026.4.0 | — | Current | ParameterResolver fix, 12 new say* methods, @DtrConfig |
| 2026.3.0 | 2.6.0 | Stable | HTTP client removed, 14 new say* methods |
| 2026.3.0 | 2.5.0 | Stable | Maven Central ready, RenderMachine unsealed |
| 2026.3.0 | 2.4.0 | Stable | Introspection methods added |
| 2.0.0 | 2.0.0 | Legacy | Java 26 migration, Markdown-first |

### Migration Impact Assessment

- **v2026.3.0 → v2026.4.0**: ✅ **LOW IMPACT** — ParameterResolver fix, new features, minimal breaking changes
- **v2.5.x → v2.6.0**: ⚠️ **HIGH IMPACT** — HTTP client layer removed, requires code changes
- **v2.0.0 → v2.5.x**: ✅ **LOW IMPACT** — Mostly additive, RenderMachine sealed→abstract
- **v1.x → v2.0.0**: ⚠️ **BREAKING** — Complete rewrite, Java 26 required

---

## Historical Migration Guides

The following migration guides are archived for historical reference:

| Guide | Versions Covered | Archive Location |
|-------|------------------|------------------|
| 2.4.x → 2.5.x → 2.6.0 | Early migration paths | [releases/archive/MIGRATION_2.4_to_2.5.md](releases/archive/MIGRATION_2.4_to_2.5.md) |

**Note:** These guides are preserved for historical context. For current migration information, use the sections below.

---

## v2026.3.0 → v2026.4.0 (LOW IMPACT)

### What Changed

DTR 2026.4.0 is a **DX/QoL enhancement release** with minimal breaking changes. Most users can upgrade without any code modifications.

**Key highlights:**
- Critical bug fix: `DtrContext` parameter injection now works
- 12 new `say*` methods (assertion combos, presentation support)
- `@DtrConfig` annotation for hierarchical configuration
- `@LivePreview` annotation for IDE integration
- `RenderConfig` class removed (use `@DtrConfig` instead)
- `sayCodeModel(Method)` deprecated (use `sayMethodSignature(Method)` instead)

### Breaking Changes

#### 1. RenderConfig Removed

**Affected:** Users who programmatically created `RenderMachine` instances (rare)

**Before (2026.3.0):**
```java
RenderConfig config = new RenderConfig();
List<RenderMachine> machines = config.createRenderMachines();
```

**After (2026.4.0):**
```java
// Option 1: Annotation-based (recommended)
@DtrConfig(format = OutputFormat.MARKDOWN)
class MyTest { }

// Option 2: Programmatic
DtrConfiguration config = DtrConfiguration.builder()
    .format(OutputFormat.MARKDOWN)
    .build();
```

#### 2. sayCodeModel(Method) Deprecated

**Affected:** Users calling `sayCodeModel(Method)` (low impact)

**Migration:**
```java
// Before (deprecated, still works)
ctx.sayCodeModel(MyClass.class.getMethod("myMethod"));

// After (recommended)
ctx.sayMethodSignature(MyClass.class.getMethod("myMethod"));
```

**Timeline:** Removal planned for 2027.1.0

### New Features

#### DtrContext Parameter Injection (Fixed)

The `DtrContext` parameter injection now works correctly:

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @Test
    void testGetUser(DtrContext ctx) {  // This now works!
        ctx.say("Returns user details by ID.");
    }
}
```

#### Assertion + Documentation Combined

Four new `sayAndAssertThat` methods:

```java
ctx.sayAndAssertThat("HTTP Status", response.getStatusCode(), equalTo(200));
ctx.sayAndAssertThat("Response Size", response.getBody().length(), greaterThan(0));
```

#### Presentation-Specific Methods

New methods for slides, blogs, and social media:

```java
ctx.sayTldr("Quick summary for busy readers");
ctx.saySlideOnly("Simplified bullet points for slides");
ctx.saySpeakerNote("Presenter notes (slides only)");
ctx.sayTweetable("DTR 2026.4.0 adds 12 new say* methods!");
ctx.sayDocOnly("This appears only in docs, not slides");
ctx.sayHeroImage("Architecture diagram");
ctx.sayCallToAction("https://github.com/seanchatmangpt/dtr");
```

#### Configuration Annotations

```java
@DtrConfig(
    format = OutputFormat.MARKDOWN,
    outputDir = "target/docs",
    autoSection = true
)
class MyTest { }

@LivePreview
@Test
void testWithPreview(DtrContext ctx) {
    ctx.say("This can be previewed in supported IDEs");
}
```

### Migration Steps

1. **Update dependency:**
```xml
<version>2026.4.0</version>
```

2. **Run tests** — all should pass without changes

3. **Check for RenderConfig usage:**
```bash
grep -r "RenderConfig" src/test/java
```

4. **Check for sayCodeModel(Method) usage:**
```bash
grep -r "sayCodeModel.*getMethod" src/test/java
```

5. **Adopt new features** (optional):
   - Replace separate assertion + documentation with `sayAndAssertThat`
   - Add presentation-specific content for slides/blogs
   - Use `@DtrConfig` for configuration

### Validation Checklist

- [ ] `pom.xml` uses version `2026.4.0`
- [ ] All tests pass with `mvnd clean test`
- [ ] Documentation appears in `target/docs/`
- [ ] No `RenderConfig` references (or migrated to `@DtrConfig`)
- [ ] No deprecated `sayCodeModel(Method)` calls (or migrated to `sayMethodSignature`)

---

## v2.5.x → v2.6.0 (HIGH IMPACT)

### What Changed

DTR 2.6.0 (2026.3.0) is a **breaking release** that removes the entire HTTP client layer. DTR is now a pure documentation-generation library with no HTTP testing responsibilities.

**Key Principle:** DTR focuses on documentation generation only. HTTP testing should use standard Java libraries or dedicated testing frameworks.

### What Was Removed

The following APIs are **completely gone** in v2.6.0:

| Removed API | Purpose | Replacement |
|-------------|---------|-------------|
| `TestBrowser` (interface) | HTTP client abstraction | Use `java.net.http.HttpClient` or RestAssured/WebTestClient |
| `TestBrowserImpl` | HTTP client implementation | No replacement |
| `Request.GET()` | HTTP request builder | `HttpRequest.newBuilder().GET()` |
| `Request.POST()` | HTTP request builder | `HttpRequest.newBuilder().POST()` |
| `Request.PUT()` | HTTP request builder | `HttpRequest.newBuilder().PUT()` |
| `Request.DELETE()` | HTTP request builder | `HttpRequest.newBuilder().DELETE()` |
| `Response` class | HTTP response wrapper | `HttpResponse<String>` |
| `response.httpStatus()` | Get status code | `response.statusCode()` |
| `response.payloadAs()` | Get response body | `response.body()` |
| `sayAndMakeRequest(Request)` | Execute & document HTTP | Make request manually, then document with `say*` |
| `sayAndAssertThat(..., Matcher)` | Assert & document | Use AssertJ/Hamcrest separately |
| `testServerUrl()` | Base URL config | Manage URLs yourself |
| `setTestBrowser()` / `getTestBrowser()` | HTTP client lifecycle | No replacement |
| `WebSocketClient` | WebSocket testing | `java.net.http.WebSocket` |
| `ServerSentEventsClient` | SSE testing | Parse SSE streams manually |
| `BearerTokenAuth` | OAuth bearer tokens | Set `Authorization: Bearer` header |
| `ApiKeyAuth` | API key auth | Set custom header or query param |
| `BasicAuth` | HTTP basic auth | Set `Authorization: Basic` header |
| `getCookies()` / `getCookieWithName()` | Cookie management | `java.net.CookieManager` |
| `sayAndGetCookies()` | Document cookies | Document manually |

### What Was Added

In exchange for the removed HTTP stack, v2.6.0 adds **14 new `say*` methods** for powerful documentation capabilities:

| New Method | Purpose |
|------------|---------|
| `sayBenchmark(String, Runnable)` | Inline microbenchmarking with System.nanoTime() |
| `sayBenchmark(String, Runnable, int, int)` | Configurable warmup/measure rounds |
| `sayMermaid(String)` | Raw Mermaid diagram DSL |
| `sayClassDiagram(Class<?>...)` | Auto-generate Mermaid classDiagram |
| `sayControlFlowGraph(Method)` | Mermaid CFG from Code Reflection |
| `sayCallGraph(Class<?>)` | Mermaid call graph visualization |
| `sayOpProfile(Method)` | Operation count table |
| `sayDocCoverage(Class<?>...)` | Documentation coverage matrix |
| `sayEnvProfile()` | Environment snapshot (Java, OS, heap) |
| `sayRecordComponents(Class<? extends Record>)` | Record schema table |
| `sayException(Throwable)` | Structured exception documentation |
| `sayAsciiChart(String, double[], String[])` | Unicode bar chart |
| `sayContractVerification(Class<?>, Class<?>...)` | Interface contract coverage |
| `sayEvolutionTimeline(Class<?>, int)` | Git log timeline for class |

### Migration Steps

#### Step 1: Update Dependency Version

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.3.0</version>
    <scope>test</scope>
</dependency>
```

#### Step 2: Find All Removed API Usage

Run this command to find all references to removed APIs:

```bash
grep -rn "sayAndMakeRequest\|sayAndAssertThat\|Request\.GET\|Request\.POST\|Request\.PUT\|Request\.DELETE\|testServerUrl\|TestBrowser\|WebSocketClient\|ServerSentEventsClient\|BearerTokenAuth\|ApiKeyAuth\|BasicAuth\|getCookies\|getCookieWithName" src/test/java
```

#### Step 3: Update Test Class Structure

**Before (v2.5.x):**

```java
import io.github.seanchatmangpt.dtr.DTR;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class UserApiDocTest extends DTR {

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testGetUsers() {
        sayNextSection("List Users");
        say("Retrieve all registered users from the system.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
        );

        sayAndAssertThat("HTTP status is 200", 200, equalTo(response.httpStatus()));
        sayJson(response.payloadAs(String.class));
    }

    @Test
    public void testCreateUser() {
        sayNextSection("Create User");

        String requestBody = """
            {"name": "Alice", "email": "alice@example.com"}
            """;

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .body(requestBody)
        );

        sayAndAssertThat("HTTP status is 201", 201, equalTo(response.httpStatus()));
    }
}
```

**After (v2.6.0):**

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class UserApiDocTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Test
    void testGetUsers(DtrContext ctx) throws Exception {
        ctx.sayNextSection("List Users");
        ctx.say("Retrieve all registered users from the system.");

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        ctx.say("HTTP Status: " + response.statusCode());
        ctx.sayJson(response.body());
    }

    @Test
    void testCreateUser(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Create User");

        String requestBody = """
            {"name": "Alice", "email": "alice@example.com"}
            """;

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);

        ctx.say("HTTP Status: " + response.statusCode());
        ctx.sayJson(response.body());
    }
}
```

#### Step 4: Replace Authentication

**Before (v2.5.x):**

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/users"))
        .auth(BearerTokenAuth.of("my-token-123"))
);
```

**After (v2.6.0):**

```java
var request = HttpRequest.newBuilder()
    .uri(URI.create(BASE_URL + "/api/users"))
    .header("Authorization", "Bearer my-token-123")
    .GET()
    .build();
```

#### Step 5: Replace Cookie Management

**Before (v2.5.x):**

```java
// TestBrowser automatically stores and replays session cookies
Response loginResponse = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/login"))
        .body("{\"user\":\"alice\",\"password\":\"secret\"}")
);

// Cookies automatically sent on next request
Response dashboardResponse = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/dashboard"))
);
```

**After (v2.6.0):**

```java
var client = HttpClient.newBuilder()
    .cookieHandler(new CookieManager())
    .build();

// Login request - cookies stored automatically
var loginRequest = HttpRequest.newBuilder()
    .uri(URI.create(BASE_URL + "/api/login"))
    .header("Content-Type", "application/json")
    .POST(BodyPublishers.ofString("{\"user\":\"alice\",\"password\":\"secret\"}"))
    .build();

client.send(loginRequest, BodyHandlers.ofString());

// Dashboard request - cookies sent automatically
var dashboardRequest = HttpRequest.newBuilder()
    .uri(URI.create(BASE_URL + "/api/dashboard"))
    .GET()
    .build();

HttpResponse<String> dashboardResponse = client.send(dashboardRequest, BodyHandlers.ofString());
```

#### Step 6: Replace WebSocket Testing

**Before (v2.5.x):**

```java
WebSocketClient wsClient = new WebSocketClient("ws://localhost:8080/ws");
WebSocketSession session = wsClient.connect();

session.sendTextMessage("{\"action\":\"subscribe\",\"symbol\":\"AAPL\"}");
String message = session.waitForMessage(5, TimeUnit.SECONDS);

say("Received: " + message);
```

**After (v2.6.0):**

```java
var ws = HttpClient.newHttpClient().newWebSocketBuilder()
    .build(URI.create("ws://localhost:8080/ws"), new WebSocket.Listener() {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            ctx.say("Received: " + data);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    });

// Send message
ws.sendText("{\"action\":\"subscribe\",\"symbol\":\"AAPL\"}", true);

// Wait for response (use CountDownLatch or CompletableFuture in real code)
Thread.sleep(1000);
```

### Complete Working Example

Here's a complete migration example showing a real-world API test:

**Before (v2.5.x):**

```java
public class TodoApiDocTest extends DTR {

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testCreateAndListTodos() {
        sayNextSection("Todo API Workflow");

        // Create a todo
        say("Create a new todo item:");
        Response createResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/todos"))
                .contentTypeApplicationJson()
                .body("{\"title\":\"Buy milk\",\"completed\":false}")
        );
        sayAndAssertThat("Created", 201, equalTo(createResponse.httpStatus()));

        // List all todos
        say("List all todos:");
        Response listResponse = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/todos"))
                .contentTypeApplicationJson()
        );
        sayAndAssertThat("OK", 200, equalTo(listResponse.httpStatus()));
        sayJson(listResponse.payloadAs(String.class));
    }
}
```

**After (v2.6.0):**

```java
@ExtendWith(DtrExtension.class)
class TodoApiDocTest {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void testCreateAndListTodos(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Todo API Workflow");

        // Create a todo
        ctx.say("Create a new todo item:");

        var createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/todos"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(
                "{\"title\":\"Buy milk\",\"completed\":false}"
            ))
            .build();

        HttpResponse<String> createResponse = httpClient.send(
            createRequest,
            BodyHandlers.ofString()
        );

        assertThat(createResponse.statusCode()).isEqualTo(201);
        ctx.say("HTTP Status: " + createResponse.statusCode());
        ctx.sayJson(createResponse.body());

        // List all todos
        ctx.say("List all todos:");

        var listRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/todos"))
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> listResponse = httpClient.send(
            listRequest,
            BodyHandlers.ofString()
        );

        assertThat(listResponse.statusCode()).isEqualTo(200);
        ctx.say("HTTP Status: " + listResponse.statusCode());
        ctx.sayJson(listResponse.body());
    }
}
```

### Rollback Procedure

If the migration to v2.6.0 fails or you need more time:

1. **Revert to v2.5.x** in your `pom.xml`:

```xml
<version>2026.3.0</version>  <!-- This is v2.5.x -->
```

2. **Restore your test code** from version control:

```bash
git checkout HEAD~1 -- src/test/java
```

3. **Reinstall dependencies**:

```bash
mvnd clean install
```

### Validation Checklist

After migrating to v2.6.0, verify:

- [ ] `pom.xml` uses version `2026.3.0` (or later)
- [ ] All test classes use `@ExtendWith(DtrExtension.class)`
- [ ] All test methods accept `DtrContext ctx` parameter
- [ ] No references to `sayAndMakeRequest`, `sayAndAssertThat`, `Request`, `Response`
- [ ] No references to `testServerUrl()`, `TestBrowser`, `TestBrowserImpl`
- [ ] HTTP calls use `java.net.http.HttpClient` (or other HTTP library)
- [ ] Assertions use AssertJ `assertThat()` or Hamcrest `assertThat()`
- [ ] `mvnd clean test` completes successfully
- [ ] Documentation appears in `target/docs/test/`

---

## v2.0.0 → v2.5.x

### What Changed

This migration covers versions from v2.0.0 through v2.5.x (2026.3.0). Most changes are backward compatible.

### Key Changes

| Version | Key Changes | Breaking? |
|---------|-------------|-----------|
| v2.0.0 | Java 26 required, Markdown output, JUnit Jupiter 6 support | Yes |
| v2.4.0 | Added introspection methods (`sayCallSite`, `sayAnnotationProfile`, etc.) | No |
| v2.5.0 | RenderMachine: sealed → abstract, Maven Central ready | Rare edge case |

### v2.5.x Breaking Changes

#### RenderMachine No Longer Sealed

**Affected:** Custom `RenderMachine` implementations (rare)

**Before (v2.4.0):**

```java
public sealed class MyCustomRenderer implements RenderMachine
    permits MyCustomRendererImpl {
    // ...
}
```

**After (v2.5.0):**

```java
public final class MyCustomRenderer extends RenderMachine {
    @Override
    public void setFileName(String fileName) { /* ... */ }

    @Override
    public void finishAndWriteOut() { /* ... */ }
}
```

### Migration Steps (v2.0.0 → v2.5.x)

1. **Update dependency:**

```xml
<version>2026.3.0</version>
```

2. **No code changes required** for 99% of users

3. **Run tests:**

```bash
mvnd clean test
```

---

## v1.x → v2.0.0

### What Changed

DTR 2.0.0 was a complete rewrite with major breaking changes:

- **Java 26 required** (was Java 8+)
- **Markdown-first output** (was HTML)
- **JUnit Jupiter 6 support** (was JUnit 4)
- **HttpClient 5.x** (was HttpClient 4.5)
- **Annotation-based testing API** (`@DocSection`, `@DocDescription`)

### Breaking Changes

#### 1. Java Version Requirement

**v1.x:** Java 1.8 - 21 supported

**v2.0.0:** Java 26 (LTS) required

```bash
# Set Java 26
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk
java -version  # Verify: openjdk version "26.0.0"
```

#### 2. Documentation Output Format

**v1.x:** HTML output in `target/site/dtr/`

**v2.0.0:** Markdown output in `target/docs/`

Update CI/CD to look for docs in the new location:

```yaml
# OLD
- name: Publish Documentation
  run: |
    if [ -d "target/site/dtr" ]; then
      cp -r target/site/dtr ./docs
    fi

# NEW
- name: Publish Documentation
  run: |
    if [ -d "target/docs" ]; then
      cp -r target/docs ./site
    fi
```

#### 3. Test Class Structure

**Before (v1.x):**

```java
import io.github.seanchatmangpt.dtr.DTR;
import org.junit.Test;

public class UserApiDocTest extends DTR {

    @Test
    public void testListUsers() {
        sayNextSection("Get All Users");
        say("Retrieve a list of all registered users.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
        );

        sayAndAssertThat("HTTP 200", 200, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**After (v2.0.0):**

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
public class UserApiDocTest {

    @Test
    @DocSection("Get All Users")
    @DocDescription("Retrieve a list of all registered users.")
    public void testListUsers(DtrContext ctx) {
        // Same test logic
    }
}
```

#### 4. Maven Compiler Configuration

**v1.x:**

```xml
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```

**v2.0.0:**

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <enablePreview>true</enablePreview>
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

### Migration Path (v1.x → v2.0.0)

1. **Upgrade Java to 26**
2. **Update Maven to 4.0.0-rc-3+**
3. **Update pom.xml** (see above)
4. **Migrate JUnit 4 → JUnit Jupiter 6**
5. **Update test class structure** (remove `extends DTR`, add `@ExtendWith`)
6. **Update CI/CD documentation paths**
7. **Run `mvnd clean test`**

**Consider staying on v1.x** if:
- You cannot upgrade to Java 26
- You need HTML output (not Markdown)
- You depend on JUnit 4

---

## Feature Mapping Table

### HTTP Testing Features

| v2.5.x Feature | v2.6.0+ Replacement | Notes |
|----------------|---------------------|-------|
| `TestBrowser` | `java.net.http.HttpClient` | Use Java 11+ HTTP client |
| `Request.GET()` | `HttpRequest.newBuilder().GET()` | Standard Java API |
| `Request.POST()` | `HttpRequest.newBuilder().POST()` | Use `BodyPublishers.ofString()` |
| `Request.PUT()` | `HttpRequest.newBuilder().PUT()` | Use `BodyPublishers.ofString()` |
| `Request.DELETE()` | `HttpRequest.newBuilder().DELETE()` | Standard Java API |
| `Response.httpStatus()` | `HttpResponse.statusCode()` | Different method name |
| `Response.payloadAs()` | `HttpResponse.body()` | Direct access |
| `sayAndMakeRequest()` | Manual HTTP + `ctx.sayJson()` | Separate concerns |
| `sayAndAssertThat()` | `assertThat()` + `ctx.sayAssertions()` | Use AssertJ/Hamcrest |
| `testServerUrl()` | Manage URLs manually | Use constants or config |
| `BearerTokenAuth` | `Authorization: Bearer` header | Standard HTTP header |
| `ApiKeyAuth` | Custom header or query param | No special handling |
| `BasicAuth` | `Authorization: Basic` header | Use `java.net.Base64` encoder |
| `getCookies()` | `java.net.CookieManager` | Standard Java API |
| `WebSocketClient` | `java.net.http.WebSocket` | Java 11+ WebSocket API |
| `ServerSentEventsClient` | Manual SSE parsing | Read lines, parse `data:` |

### Documentation Features

| v2.5.x Feature | v2.6.0+ Status | Notes |
|----------------|----------------|-------|
| `say(text)` | ✅ Unchanged | Core API |
| `sayNextSection(headline)` | ✅ Unchanged | Core API |
| `sayCode(code, lang)` | ✅ Unchanged | Core API |
| `sayJson(object)` | ✅ Unchanged | Core API |
| `sayTable(data)` | ✅ Unchanged | Core API |
| `sayWarning(message)` | ✅ Unchanged | Core API |
| `sayNote(message)` | ✅ Unchanged | Core API |
| `sayKeyValue(pairs)` | ✅ Unchanged | Core API |
| `sayRef(ref)` | ✅ Unchanged | Core API |
| `sayCite(key)` | ✅ Unchanged | Core API |
| `sayCallSite()` | ✅ Unchanged | Added in v2.4.0 |
| `sayAnnotationProfile()` | ✅ Unchanged | Added in v2.4.0 |
| `sayClassHierarchy()` | ✅ Unchanged | Added in v2.4.0 |
| `sayStringProfile()` | ✅ Unchanged | Added in v2.4.0 |
| `sayReflectiveDiff()` | ✅ Unchanged | Added in v2.4.0 |
| `sayBenchmark()` | ✅ New in v2.6.0 | Microbenchmarking |
| `sayMermaid()` | ✅ New in v2.6.0 | Mermaid diagrams |
| `sayClassDiagram()` | ✅ New in v2.6.0 | Auto class diagrams |
| `sayControlFlowGraph()` | ✅ New in v2.6.0 | Code Reflection CFG |
| `sayCallGraph()` | ✅ New in v2.6.0 | Call graph visualization |
| `sayOpProfile()` | ✅ New in v2.6.0 | Operation count table |
| `sayDocCoverage()` | ✅ New in v2.6.0 | Coverage matrix |
| `sayEnvProfile()` | ✅ New in v2.6.0 | Environment snapshot |
| `sayRecordComponents()` | ✅ New in v2.6.0 | Record schema |
| `sayException()` | ✅ New in v2.6.0 | Exception documentation |
| `sayAsciiChart()` | ✅ New in v2.6.0 | ASCII bar charts |
| `sayContractVerification()` | ✅ New in v2.6.0 | Interface contract coverage |
| `sayEvolutionTimeline()` | ✅ New in v2.6.0 | Git history timeline |

---

## Troubleshooting Migrations

### Common Issues and Solutions

#### 1. Compilation Errors After Upgrading to v2.6.0

**Symptom:** Compiler errors about missing `TestBrowser`, `Request`, `Response` classes.

**Solution:** You haven't fully migrated from the HTTP client layer. Review the [v2.5.x → v2.6.0 migration steps](#step-3-update-test-class-structure).

**Find all remaining references:**

```bash
grep -rn "TestBrowser\|Request\.GET\|sayAndMakeRequest" src/test/java
```

#### 2. `--enable-preview` Errors

**Symptom:** Build fails with error about preview features not being enabled.

**Solution:** Ensure `.mvn/maven.config` contains:

```
--enable-preview
```

And your `pom.xml` has:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

#### 3. Java Version Mismatch

**Symptom:** Build fails with "Java 26 or higher is required."

**Solution:** Set `JAVA_HOME` to Java 26:

```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk
java -version  # Verify
```

#### 4. JUnit 4 vs JUnit Jupiter 6 Confusion

**Symptom:** Tests don't run or `@Test` not found.

**Solution:** Ensure you're using JUnit Jupiter 6 annotations:

```java
// Wrong (JUnit 4)
import org.junit.Test;

// Correct (JUnit Jupiter 6)
import org.junit.jupiter.api.Test;
```

And your `pom.xml` has:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.3</version>
    <scope>test</scope>
</dependency>
```

#### 5. Documentation Not Generated

**Symptom:** Tests pass but no `target/docs/` directory.

**Solution:** Ensure tests use `@ExtendWith(DtrExtension.class)` and accept `DtrContext ctx` parameter:

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @Test
    void testSomething(DtrContext ctx) {
        ctx.say("This generates documentation");
    }
}
```

#### 6. Rollback to Previous Version

If you need to rollback quickly:

```xml
<!-- In pom.xml -->
<version>2026.3.0</version>  <!-- v2.5.x, before HTTP client removal -->
```

Then restore your test code from git:

```bash
git checkout HEAD~1 -- src/test/java
mvnd clean install
```

### Getting Help

If you encounter issues not covered here:

1. **Check compilation errors** — The compiler lists all removed API references
2. **Review the changelog** — See `CHANGELOG.md` for detailed changes
3. **Search existing issues** — https://github.com/seanchatmangpt/dtr/issues
4. **Ask a question** — https://github.com/seanchatmangpt/dtr/discussions
5. **Report a bug** — https://github.com/seanchatmangpt/dtr/issues/new

When reporting issues, include:
- DTR version (from `pom.xml`)
- Java version (`java -version`)
- Maven version (`mvnd --version`)
- Full error message and stack trace
- Minimal reproducible example

---

## Quick Reference

### Version Compatibility Matrix

| DTR Version | Java Required | Maven Required | JUnit | HTTP Client | Status |
|-------------|---------------|----------------|-------|-------------|--------|
| 2026.3.0 (v2.6.0) | 26+ | 4.0.0-rc-3+ | JUnit Jupiter 6 | None (use your own) | ✅ Current |
| 2026.3.0 (v2.5.x) | 26+ | 4.0.0-rc-3+ | JUnit Jupiter 6 | Built-in (deprecated) | ✅ Stable |
| 2026.3.0 (v2.4.x) | 26+ | 4.0.0-rc-3+ | JUnit Jupiter 6 | Built-in | ✅ Stable |
| v2.0.0 | 26+ | 4.0.0-rc-3+ | JUnit Jupiter 6 | Built-in | ⚠️ Legacy |
| v1.x | 8-21 | 3.x | JUnit 4 | Built-in | ❌ Deprecated |

### Migration Decision Tree

```
Are you upgrading from v1.x?
  ├── Yes → Full rewrite required. See [v1.x → v2.0.0](#v1x--v200)
  └── No → Continue

Are you upgrading from v2.0.0 - v2.5.x?
  ├── Yes → Mostly additive. See [v2.0.0 → v2.5.x](#v200--v25x)
  └── No → Continue

Are you upgrading from v2.5.x to v2.6.0+?
  ├── Yes → HIGH IMPACT. HTTP client removed. See [v2.5.x → v2.6.0](#v25x--v260-high-impact)
  └── No → You're on the latest version
```

---

**Last Updated:** March 14, 2026

**DTR Version:** 2026.3.0 (v2.6.0)

**For the latest version of this guide, see:** https://github.com/seanchatmangpt/dtr/docs/MIGRATING.md

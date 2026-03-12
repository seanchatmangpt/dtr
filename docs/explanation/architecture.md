# Explanation: Architecture

This document describes DocTester's module structure, class design, and the extension points available to customize its behavior.

---

## Module structure

DTR is a two-module Maven project:

```
doctester/
├── pom.xml                          # Parent POM (enforcer, versions)
├── dtr-core/                  # The library
│   └── src/main/java/org/r10r/doctester/
│       ├── DTR.java
│       ├── testbrowser/             # HTTP client
│       └── rendermachine/           # HTML output
└── dtr-integration-test/      # Full-stack integration tests
    └── src/test/java/controllers/
        └── ApiControllerDocTest.java
```

**`dtr-core`** is the artifact you add to your project. It is a JAR with no runtime dependencies beyond test scope.

**`dtr-integration-test`** spins up a real web server (Ninja Framework + Jetty) and runs `ApiControllerDocTest` against it. This module exists to:
1. Verify DTR works end-to-end
2. Serve as a living example of all DTR features

---

## Package layout

```
io.github.seanchatmangpt.dtr.doctester
├── DTR.java               # Abstract base class

io.github.seanchatmangpt.dtr.doctester.testbrowser
├── TestBrowser.java             # HTTP client interface
├── TestBrowserImpl.java         # Apache HttpClient implementation
├── Request.java                 # Fluent request builder
├── Response.java                # Response with deserialization
├── Url.java                     # URL builder
├── HttpConstants.java           # HTTP string constants
└── PayloadUtils.java            # JSON/XML formatting helpers

io.github.seanchatmangpt.dtr.doctester.rendermachine
├── RenderMachineCommands.java   # Output method interface
├── RenderMachine.java           # Full interface (commands + lifecycle)
├── RenderMachineImpl.java       # Bootstrap HTML implementation
└── RenderMachineHtml.java       # HTML template string constants
```

---

## Class relationships

```
     ┌─────────────────────────────────────────────┐
     │              DTR (abstract)            │
     │                                              │
     │  + say(), sayNextSection(), sayRaw()         │
     │  + sayAndMakeRequest()                       │
     │  + sayAndAssertThat()                        │
     │  + getCookies(), clearCookies()              │
     │                                              │
     │  delegates to:                               │
     │   - TestBrowser (per test method)            │
     │   - RenderMachine (per test class, static)   │
     └──────────────┬──────────────────┬────────────┘
                    │                  │
        ┌───────────▼──┐    ┌──────────▼──────────┐
        │ TestBrowser  │    │   RenderMachine      │
        │ (interface)  │    │   (interface)        │
        └──────┬───────┘    └──────────┬───────────┘
               │                       │
   ┌───────────▼─────┐     ┌───────────▼──────────┐
   │ TestBrowserImpl │     │  RenderMachineImpl    │
   │ (Apache Http)   │     │  (Bootstrap HTML)     │
   └─────────────────┘     └──────────────────────┘
```

`DocTester` depends on interfaces, not implementations. This is the primary extension point: inject different `TestBrowser` or `RenderMachine` implementations via `getTestBrowser()` and `getRenderMachine()`.

---

## TestBrowserImpl internals

`TestBrowserImpl` wraps Apache `HttpClient` 4.5:

- One `CloseableHttpClient` per instance (per test method)
- `BasicCookieStore` for cookie persistence
- `LaxRedirectStrategy` for redirect following
- Payload serialization:
  - JSON: `com.fasterxml.jackson.databind.ObjectMapper`
  - XML: `com.fasterxml.jackson.dataformat.xml.XmlMapper`
- HTTP methods are dispatched via if/else on `request.method()`:

```java
// Simplified dispatch logic in TestBrowserImpl
if (request.method().equals(HttpConstants.GET)) {
    // execute HttpGet
} else if (request.method().equals(HttpConstants.POST)) {
    // build entity (JSON body, form params, or multipart)
    // execute HttpPost
} // ... etc
```

The branching is straightforward but verbose. Refactoring to a sealed class + pattern match would be a good Java 25 modernization.

---

## RenderMachineImpl internals

`RenderMachineImpl` builds HTML by string concatenation into a `StringBuilder`:

```java
// Simplified
public void say(String text) {
    content.append("<p>").append(HtmlEscapers.htmlEscaper().escape(text)).append("</p>");
}

public void sayNextSection(String title) {
    sections.add(title);
    String id = title.replaceAll("\\s+", "_");
    content.append("<h1 id='").append(id).append("'>").append(title).append("</h1>");
}
```

HTML template snippets are in `RenderMachineHtml` as string constants (Bootstrap navbar, sidebar, footer, panel templates).

`finishAndWriteOut()` assembles the full page:
1. HTML head + navbar
2. Sidebar with links to sections
3. Main content (accumulated buffer)
4. Bootstrap panel for each request/response (stored as structured data, not pre-rendered)
5. Write to file

---

## Extension points

DTR has three extension points, all in `DTR.java`:

### 1. `testServerUrl()` (override)

The simplest extension: just return a different `Url`. Used in every real project.

### 2. `getTestBrowser()` (override)

Replace the HTTP client entirely. Useful for:
- Using OkHttp or Java's `HttpClient` instead of Apache
- Adding request logging, metrics, or tracing
- Mocking HTTP responses in unit tests of DTR itself
- Custom SSL certificate handling

### 3. `getRenderMachine()` (override)

Replace the HTML renderer. Useful for:
- Generating Markdown or AsciiDoc instead of HTML
- Adding custom section templates
- Writing to a different output path
- Integrating with a documentation system (Confluence, Notion, etc.)

---

## Dependencies in dtr-core

| Dependency | Purpose | Scope |
|---|---|---|
| `junit:junit:4.12` | Test base class (`@Before`, `@AfterClass`) | provided |
| `httpclient:4.5` | HTTP execution | compile |
| `httpmime:4.5` | Multipart file uploads | compile |
| `jackson-core:2.5.4` | JSON serialization | compile |
| `jackson-dataformat-xml:2.5.4` | XML serialization | compile |
| `woodstox-core:5.0.1` | XML parsing (Woodstox StAX) | compile |
| `guava:18.0` | `HtmlEscapers`, `Lists`, `Maps` utilities | compile |
| `commons-fileupload:1.3.1` | Multipart parsing | compile |
| `slf4j-api:1.7.12` | Logging API | compile |
| `mockito-core:4.11.0` | Testing DTR itself | test |

The `provided` scope for JUnit means DTR doesn't force a specific JUnit version on consumers — you supply it.

---

## Package-Level Responsibilities

### Core Package: `io.github.seanchatmangpt.dtr`

**Role:** Entry point and lifecycle management

**Key Classes:**
- `DTR` (abstract) — Base class all tests inherit from
  - Lifecycle: `@Before`, `@After`, `@BeforeClass`, `@AfterClass` hooks
  - Extension points: `testServerUrl()`, `getTestBrowser()`, `getRenderMachine()`
  - API methods: All `say*()` methods delegate to `RenderMachine` and `TestBrowser`

**Responsibility:** Connect test lifecycle to RenderMachine and TestBrowser

---

### TestBrowser Package: `io.github.seanchatmangpt.dtr.testbrowser`

**Role:** HTTP client abstraction and request/response handling

**Key Classes:**
- `TestBrowser` (interface) — What any HTTP implementation must provide
- `TestBrowserImpl` (class) — Apache HttpClient 4.5 implementation
  - Manages `CloseableHttpClient` (one per test method)
  - Handles cookies with `BasicCookieStore`
  - Serializes/deserializes payloads (JSON, XML)
  - Dispatches HTTP methods (GET, POST, PUT, DELETE, etc.)

- `Request` (builder) — Fluent API for constructing HTTP requests
  - Methods: `GET()`, `POST()`, `PUT()`, `DELETE()`, `PATCH()`
  - Chainable options: `url()`, `payload()`, `header()`, `contentType()`, etc.
  - Supports multipart file uploads via `multipartForm()`

- `Response` (wrapper) — Handles response data and deserialization
  - Provides access to: status code, headers, body, content type
  - Deserialization: `payloadAs(Class)` for JSON/XML → Java objects
  - Assertion helpers: `assertThat()` methods

- `Url` (builder) — Fluent URL construction
  - Methods: `host()`, `path()`, `queryParam()`, `fragment()`
  - Immutable and safe for reuse across requests

- `PayloadUtils` (utility) — Content type detection and formatting
  - Detects JSON/XML from Content-Type headers
  - Pretty-prints JSON and XML for documentation
  - Handles charset variations (e.g., `application/json; charset=utf-8`)

- `HttpConstants` (constants) — HTTP method and header names
  - HTTP methods: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
  - Headers: Content-Type, Accept, Authorization, Accept-Encoding, etc.

**Responsibility:** Abstract away HTTP complexity; provide simple, testable request/response interface

---

### RenderMachine Package: `io.github.seanchatmangpt.dtr.rendermachine`

**Role:** Output generation (HTML, Markdown, LaTeX, etc.)

**Key Classes:**
- `RenderMachine` (interface) — Comprehensive rendering interface
  - Extends `RenderMachineCommands` (the output methods)
  - Additional methods: `finishAndWriteOut()` (flush to disk)

- `RenderMachineCommands` (interface) — Minimal output API
  - Core methods: `say()`, `sayNextSection()`, `sayCode()`, `sayTable()`, `sayJson()`
  - Extension point: implement to add new output formats

- `RenderMachineImpl` (class) — Default Bootstrap HTML implementation
  - Output method: accumulates HTML into `StringBuilder`
  - Sections tracked in a list (for table of contents)
  - Request/response pairs stored as objects, rendered on `finishAndWriteOut()`
  - Lifecycle: static field in `DTR` (persists across test methods in one class)
  - File output: writes to `target/site/doctester/{TestClassName}.html`

- `RenderMachineHtml` (constants) — HTML template strings
  - Bootstrap navbar, sidebar, footer templates
  - Responsive grid layout snippets
  - Panel templates for request/response pairs
  - CSS styling (inlined)

**Responsibility:** Transform test output into user-friendly documentation (HTML by default, extensible)

**Extension Pattern:** Custom renderers can implement `RenderMachine` or `RenderMachineCommands` to:
- Generate Markdown, AsciiDoc, or ReStructuredText
- Output to Confluence, Notion, or other platforms
- Create LaTeX for PDF generation
- Generate OpenAPI specs
- Build blog post exports

---

## Module Overview

The project contains three Maven modules:

### Module 1: `dtr-core`
**Maven Coordinate:** `io.github.seanchatmangpt.dtr:dtr-core:2.5.0`

**What it is:** The main library JAR

**Contents:**
- All classes in packages above (DTR, testbrowser, rendermachine)
- Bootstrap 3 CSS (bundled)
- No runtime dependencies (everything is test-scoped)

**Who uses it:** Your project (add as `<scope>test</scope>`)

**Output:** `target/site/doctester/*.html` files

---

### Module 2: `dtr-integration-test`
**Maven Coordinate:** Internal only (not published to Maven Central)

**What it is:** Full-stack integration test suite

**Contents:**
- Real web server (Ninja Framework + Jetty)
- `PhDThesisDocTest` — Comprehensive example using all DTR features
- Real HTTP requests (not mocked)
- Database interactions (if configured)

**Purpose:**
- Verify DTR works end-to-end
- Serve as canonical documentation (every feature demonstrated here)
- Catch regressions

**Output:** Full HTML docs + PDF/LaTeX/OpenAPI (if enabled)

---

### Module 3: `dtr-benchmarks`
**Maven Coordinate:** Internal only (not published to Maven Central)

**What it is:** JMH microbenchmarks

**Contents:**
- Benchmark classes for RenderMachine operations
- Benchmark classes for TestBrowser HTTP operations
- Virtual thread performance comparisons

**Purpose:**
- Measure DTR's performance impact
- Track regressions across releases
- Guide optimization efforts

**Output:** JMH result files (`target/benchmarks-results/`)

---

## Data Flow

### Request/Response Documentation Flow

```
User writes test:
  ctx.sayAndMakeRequest(request)
    ↓
  DTR.sayAndMakeRequest() delegates:
    ├─→ TestBrowser.makeRequest(request)  [executes HTTP]
    │      ↓
    │      [Returns Response]
    │
    └─→ RenderMachine.sayRequest(request)  [documents]
       RenderMachine.sayResponse(response) [documents]
    ↓
  [Test method ends]
    ↓
  @After hook calls:
    RenderMachine.finishAndWriteOut()
    ↓
  File written:
    target/site/doctester/TestClass.html
```

### Output Assembly (RenderMachineImpl.finishAndWriteOut)

```
1. HTML skeleton (doctype, head, opening tags)
2. Bootstrap navbar (title, timestamp)
3. Table of contents (links to sections)
4. Main content buffer (all say* calls)
5. Request/Response panels (structured data → formatted HTML)
6. Footer
7. Write to file
```

---

## Lifecycle Details

### Per Test Class

- `@BeforeClass` (static): RenderMachine initialized (static field)
- All test methods in class accumulate output to same RenderMachine
- `@AfterClass` (static): `finishAndWriteOut()` called, file written

**Consequence:** All output from a test class goes into one HTML file.

### Per Test Method

- `@Before`: New `TestBrowser` instance created
  - Fresh `CloseableHttpClient` initialized
  - New `BasicCookieStore` (empty)
- Test method executes: `sayAndMakeRequest()` calls, assertions
- `@After`: `TestBrowser` closed
  - HTTP client closed (connections pooled, then shut down)
  - Cookie jar discarded (not persisted between methods)

**Consequence:** Cookies don't persist between test methods. Each method starts fresh.

---

## Design decisions

**Why JUnit 4, not 5?**
DTR was designed before JUnit 5 became dominant. JUnit 4's `@Before`/`@AfterClass` are simpler to hook into than JUnit 5's extension model. Migrating to JUnit 5 is feasible but non-trivial.

**Why Apache HttpClient, not Java's built-in HttpClient?**
Apache HttpClient 4.5 was the standard choice when DTR was built. Java 11 introduced `java.net.http.HttpClient`, which would be a cleaner dependency. A migration would be a good contribution.

**Why static HTML, not live Swagger?**
Static HTML is deployable anywhere, requires no runtime dependencies, and can be checked into version control. The trade-off is no interactivity (no "Try it" button). For interactive docs, combine DTR with OpenAPI annotations on your server.

**Why Bootstrap 3, not Bootstrap 5?**
Bootstrap 3.0.0 is bundled in the JAR and has been there since DTR was first released. It works. Upgrading to Bootstrap 5 would be a cosmetic improvement with no functional change — a reasonable contribution if you care about the look.

**Why Guava?**
Guava's `HtmlEscapers.htmlEscaper()` was used before `String.replace()` escaping was considered safe, and `Lists`/`Maps` factory methods predate Java 9's `List.of()`. Guava could be removed as a dependency and replaced with Java 9+ built-ins.

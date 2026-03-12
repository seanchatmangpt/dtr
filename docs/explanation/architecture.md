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

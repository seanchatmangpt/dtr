# DTR — Living API Documentation for Java

[![Build Status](https://github.com/r10r-org/doctester/actions/workflows/build.yml/badge.svg)](https://github.com/r10r-org/doctester/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.r10r/dtr-core/badge.svg)](https://central.sonatype.com/artifact/org.r10r/dtr-core)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)

**DocTester 2.0.0** generates **Markdown documentation while running JUnit tests**. Write tests once, get living API documentation for free.

> **Upgrading from 1.x?** See the [Migration Guide](MIGRATION-1.x-TO-2.0.0.md) for breaking changes and new features.

---

## The Problem DTR Solves

You build a REST API with multiple endpoints, payloads, and response codes. You test it thoroughly with JUnit. But your API documentation? It rots.

- Developers update endpoints, forget to update docs
- Documentation diverges from reality
- Users follow outdated examples
- Integration breaks silently

**DocTester's answer:** Make tests *and* documentation the same thing. Every test generates a clean, readable page. When your test runs, your docs stay accurate.

```java
@Test
@DocSection("List Articles")
@DocDescription("Retrieve all articles for the authenticated user.")
public void testListArticles() {
    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("Status is 200 OK", 200, equalTo(response.httpStatus()));
    List<Article> articles = response.payloadAs(new TypeReference<>() {});
    sayAndAssertThat("Contains articles", true, is(articles.size() > 0));
}
```

This test **runs normally with JUnit** and **generates Markdown documentation** showing request, response, and assertions.

---

## What's New in 2.0.0

### Markdown Output (Portable, Version-Control Friendly)

**Version 1.x:** Bootstrap HTML with CSS/JS assets
**Version 2.0.0:** Pure Markdown — no external dependencies, GitHub-ready

```
target/docs/
├── README.md                  # Auto-generated index
├── UserApiDocTest.md          # Clean, diff-able Markdown
└── ArticleApiDocTest.md
```

Markdown is:
- **Diff-able** — see exactly what changed in Git
- **Portable** — render with any static site generator (MkDocs, Docusaurus, Jekyll, Hugo)
- **GitHub-native** — displays automatically in GitHub repos
- **Easy to extend** — mix auto-generated sections with hand-written content

### Annotation-Based API

Stop writing `sayNextSection()` boilerplate. Use annotations:

```java
@Test
@DocSection("Create Article")
@DocDescription({
    "Create a new article in the system.",
    "Requires authentication and valid payload."
})
@DocWarning("Titles must be unique.")
public void testCreateArticle() {
    // Test body — no section/description calls needed
}
```

Built-in annotations:
- `@DocSection` — H1 heading
- `@DocDescription` — Multiple paragraphs
- `@DocCode` — Code blocks
- `@DocNote` — Info boxes
- `@DocWarning` — Warning boxes

### WebSocket Support

Test real-time endpoints directly:

```java
@Test
@DocSection("WebSocket Chat")
public void testChatMessage() {
    WebSocketSession session = new WebSocketClientImpl()
        .connect(testServerUrl().path("/ws/chat").uri());

    session.send(new WebSocketMessage("Hello!", "text"));
    WebSocketMessage reply = session.receive();

    sayAndAssertThat("Server echoes message", "Hello!", equalTo(reply.payload()));
}
```

### Server-Sent Events (SSE)

Document streaming endpoints:

```java
@Test
@DocSection("Live Event Stream")
public void testEventStream() {
    SseSubscription subscription = new SseClientImpl()
        .subscribe(testServerUrl().path("/api/events").uri());

    SseEvent event = subscription.nextEvent(Duration.ofSeconds(5));
    sayAndAssertThat("Event received", "data", contains(event.eventType()));
}
```

### OpenAPI Specification Generation

Auto-generate OpenAPI 3.0 specs from your tests:

```java
// Automatically collected while running DocTests
OpenApiCollector collector = new OpenApiCollector();
// ... tests run, all HTTP exchanges recorded ...
OpenApiSpec spec = collector.buildSpec("My API", "1.0.0");
OpenApiWriter.write(spec, OutputFormat.YAML);  // → openapi.yaml
```

Use the generated spec for:
- Swagger UI
- Code generation (OpenAPI Generator)
- API mocking
- Contract testing

### Java 25 Ready

Modern Java idioms throughout:

```java
// Records for DTOs
record CreateUserRequest(String email, String name) {}

// Sealed hierarchies for type-safe results
sealed interface ApiResult permits ApiSuccess, ApiError {}

// Pattern matching
if (response.httpStatus() == 200) {
    var articles = response.payloadAs(ArticlesDto.class);
    // ...
}

// Virtual threads for concurrent requests
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Response> f1 = executor.submit(() -> makeRequest(req1));
    Future<Response> f2 = executor.submit(() -> makeRequest(req2));
    // ...
}
```

---

## Quick Start (5 minutes)

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.r10r</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Verify Java 25

```bash
java -version
# Must show: openjdk 25.x.x (or later 25 LTS release)

export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
```

### 3. Configure Maven

In your root `pom.xml`:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>

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

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

### 4. Write Your First Test

```java
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.Url;
import org.r10r.doctester.DocSection;
import org.r10r.doctester.DocDescription;
import org.junit.Test;
import static org.hamcrest.Matchers.equalTo;

public class UserApiDocTest extends DTR {

    @Test
    @DocSection("Get All Users")
    @DocDescription("Retrieve a paginated list of all users.")
    public void testListUsers() {
        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .addQueryParameter("page", "1")
                .contentTypeApplicationJson());

        sayAndAssertThat("Returns 200 OK", 200, equalTo(response.httpStatus()));
    }

    @Test
    @DocSection("Create User")
    @DocDescription("Create a new user account.")
    public void testCreateUser() {
        var newUser = new CreateUserRequest("alice@example.com", "Alice");

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        sayAndAssertThat("Returns 201 Created", 201, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}

record CreateUserRequest(String email, String name) {}
```

### 5. Run Tests and View Docs

```bash
mvnd clean test
open target/docs/README.md  # View generated documentation
```

Output in `target/docs/`:
```markdown
# API Documentation

- [UserApiDocTest](UserApiDocTest.md)

---

# UserApiDocTest

## Get All Users

Retrieve a paginated list of all users.

**Request:**
```
GET /api/users?page=1 HTTP/1.1
Content-Type: application/json
```

**Response:**
```json
{
  "users": [
    { "id": 1, "email": "alice@example.com", "name": "Alice" },
    ...
  ]
}
```

✓ Returns 200 OK

...
```

---

## Core Features

### Fluent HTTP API

Make requests with a clean, chainable builder:

```java
Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles"))
        .contentTypeApplicationJson()
        .addHeader("Authorization", "Bearer " + token)
        .payload(newArticle)
        .followRedirects(true));

// Auto-detect JSON/XML and deserialize
Article article = response.payloadAs(Article.class);
List<Article> articles = response.payloadAs(new TypeReference<>() {});
```

**Request methods:** GET, HEAD, DELETE, POST, PUT, PATCH

### Smart Assertions

Use Hamcrest matchers — assertions appear in your documentation:

```java
sayAndAssertThat("Status is 201", 201, equalTo(response.httpStatus()));
sayAndAssertThat("Contains email", "alice@example.com",
    containsString(response.payloadAsString()));
sayAndAssertThat("Has 10 users", 10,
    equalTo(articles.size()));
```

On **pass:** Green check mark in docs
On **fail:** Red error with stack trace in docs + JUnit failure

### Cookie Management

Automatic cookie jar across requests:

```java
// First request sets Set-Cookie header
Response login = sayAndMakeRequest(
    Request.POST().url(testServerUrl().path("/login")));

// Second request automatically includes the cookie
Response profile = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/profile")));
// Session cookie from login is automatically sent
```

### File Uploads

```java
sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/avatars"))
        .addFileToUpload("file", new File("avatar.png")));
```

### Query Parameters & Form Data

```java
// Query parameters
Request.GET()
    .url(testServerUrl().path("/api/articles")
        .addQueryParameter("search", "java")
        .addQueryParameter("page", "1"))

// Form parameters
Request.POST()
    .url(testServerUrl().path("/api/search"))
    .addFormParameter("q", "java")
    .addFormParameter("limit", "10")
```

---

## Advanced Usage

### Custom Authentication

Use built-in providers:

```java
Request.GET()
    .url(testServerUrl().path("/api/protected"))
    .auth(BearerTokenAuth.of(token))
    // or: BasicAuth.of("user", "pass")
    // or: ApiKeyAuth.header("X-API-Key", apiKey)
    // or: JwtAuth.of(token)
```

### Multi-Module Documentation

Each module generates its own `target/docs/`:

```bash
my-api/
├── api-core/target/docs/           # Core API tests
├── api-auth/target/docs/           # Auth service tests
└── api-web/target/docs/            # Web service tests
```

Merge with your static site generator:

```bash
# MkDocs example
mkdocs.yml:
nav:
  - Core API: '!import ./api-core/target/docs'
  - Authentication: '!import ./api-auth/target/docs'
  - Web: '!import ./api-web/target/docs'
```

### Using with Frameworks

#### Ninja Framework

```java
public class MyApiDocTest extends NinjaApiDoctester {
    // Embedded Ninja server started automatically
    // testServerUrl() points to the running server
}
```

#### Spring Boot

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class MyApiDocTest extends DTR {
    @LocalServerPort
    int port;

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:" + port);
    }
}
```

#### Quarkus

```java
@QuarkusTest
public class MyApiDocTest extends DTR {
    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8081");  // Quarkus default
    }
}
```

---

## Documentation Output

### Example Generated Documentation

After running `mvnd test`:

**target/docs/README.md:**
```markdown
# API Documentation

Generated from DocTests on 2026-03-10.

## Test Suites

- [UserApiDocTest](UserApiDocTest.md) — User management endpoints
- [ArticleApiDocTest](ArticleApiDocTest.md) — Article API endpoints
- [AuthenticationDocTest](AuthenticationDocTest.md) — Authentication flows
```

**target/docs/UserApiDocTest.md:**
```markdown
# UserApiDocTest

## Get All Users

Retrieve a paginated list of all users.

### Request

```
GET /api/users?page=1 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Response

```json
{
  "users": [
    { "id": 1, "email": "alice@example.com", "name": "Alice" },
    { "id": 2, "email": "bob@example.com", "name": "Bob" }
  ],
  "total": 42,
  "page": 1
}
```

✓ Returns 200 OK

## Create User

Create a new user account.

> **Note:** Email addresses must be unique in the system.

### Request

```
POST /api/users HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "email": "alice@example.com",
  "name": "Alice"
}
```

### Response

```json
{
  "id": 3,
  "email": "alice@example.com",
  "name": "Alice",
  "createdAt": "2026-03-10T12:00:00Z"
}
```

✓ Returns 201 Created
✓ Response contains user ID
```

---

## Publishing Documentation

### Option 1: GitHub Pages + MkDocs

```bash
pip install mkdocs mkdocs-material
cd your-project
mkdocs new docs-site
cp target/docs/*.md docs-site/docs/api
# Edit mkdocs.yml to include API docs
mkdocs gh-deploy
```

Your docs are now live at: `https://username.github.io/repo-name/api/`

### Option 2: Docusaurus

```bash
npm install docusaurus
npx docusaurus new . --package-manager npm
cp target/docs/*.md docs/
# Add to docusaurus.config.js sidebars
npm run build && npm run serve
```

### Option 3: Plain GitHub Markdown

Just commit `target/docs/` to Git:

```bash
git add target/docs/README.md target/docs/*.md
git commit -m "Update API documentation"
```

GitHub automatically renders Markdown in your repo.

---

## Requirements

- **Java:** 25 (LTS) — no exceptions
- **Maven:** 4.0.0-rc-3+ (or Maven Daemon `mvnd` 2+)
- **JUnit:** 4.12+ (or JUnit Jupiter 5.10+)

---

## Getting Help

- **📚 Full Documentation:** [Official Docs](docs/index.md)
- **🎓 Tutorials:** [Your First DocTest](docs/tutorials/your-first-doctest.md)
- **🔍 API Reference:** [Complete API Docs](docs/reference/index.md)
- **💬 GitHub Discussions:** Ask questions, share ideas
- **🐛 Bug Reports:** [Issue Tracker](https://github.com/r10r-org/doctester/issues)

---

## Contributing

Contributions are welcome! See [Contributing Guide](docs/contributing/index.md).

- Fork the repo
- Make your changes
- Run `mvnd clean verify`
- Submit a pull request

---

## License

Apache 2.0 — See [LICENSE.txt](LICENSE.txt)

---

## Changelog

### Version 2.0.0 (2026-03-10)

#### Major Changes
- **Breaking:** Output format changed from HTML to Markdown
- **Breaking:** Output location changed from `target/site/doctester/` to `target/docs/`
- **Breaking:** Java 25 (LTS) required — no earlier versions supported
- **New:** Annotation-based API (`@DocSection`, `@DocDescription`, etc.)
- **New:** WebSocket support via `org.r10r.doctester.websocket`
- **New:** Server-Sent Events (SSE) support via `org.r10r.doctester.sse`
- **New:** OpenAPI 3.0 specification generation
- **New:** JUnit 5 support (backward compatible with JUnit 4)
- **New:** Advanced authentication providers (Bearer, JWT, API Key, etc.)

#### Details
- Markdown is portable, version-control friendly, and works with any static site generator
- All annotations are optional — you can still use the classic `say*` methods if preferred
- No Bootstrap/jQuery assets required — pure Markdown is self-contained
- Virtual threads enabled for better concurrent testing
- Pattern matching and sealed classes leverage Java 25 features

[Full Changelog](changelog.md)

### Version 1.1.12

Last release of the 1.x series. Generates Bootstrap HTML documentation.

---

## Acknowledgments

DTR is inspired by Python doctests and the Devbliss doctest library. Thanks to all contributors!

---

**Happy documenting!** 🚀

For questions or suggestions, [open an issue](https://github.com/r10r-org/doctester/issues) on GitHub.

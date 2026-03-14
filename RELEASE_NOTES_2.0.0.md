# DTR 2.0.0 Release Notes

**Release Date:** March 10, 2026

**Git Tag:** `v2.0.0`

**License:** Apache 2.0

---

## Overview

DTR 2.0.0 is a **major release** with significant architectural improvements, modernization for Java 26 + Maven 4, and new enterprise-grade testing capabilities. This release introduces breaking changes to modernize the framework for contemporary Java development.

**Maven Coordinates:**
```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2.0.0</version>
  <scope>test</scope>
</dependency>
```

---

## Major Features

### 1. Markdown-First Documentation

- **Native Markdown output** for all documentation (better for version control, diffs, and documentation generators like Sphinx/Pandoc)
- Documents are now generated as `.md` files in `target/docs/` instead of `.html` in `target/site/dtr/`
- Markdown renderer produces cleaner, more portable documentation
- Bootstrap HTML output available via optional plugin
- No external CSS/JS dependencies required

**Why:** Markdown is portable, version-control friendly, and works natively on GitHub without requiring external CSS/JS assets.

### 2. Annotation-Based Testing API

- `@DocSection`: Declarative test section definition without `sayNextSection()` calls
- `@DocDescription`: Automatic method documentation extraction
- `@DocNote`, `@DocWarning`, `@DocCode`: Inline documentation annotations for highlighting important information
- Annotations enable cleaner, more readable test code with less boilerplate

**Example:**
```java
@Test
@DocSection("User Registration API")
@DocDescription("Register a new user with email and password")
@DocWarning("Email must be unique across the system")
public void testCreateUser() {
    // test code
}
```

### 3. JUnit 5 Support

- `DTRExtension`: New Jupiter-compatible extension replacing JUnit 4 base class pattern
- Full integration with JUnit 5 lifecycle hooks and parameterized tests
- Support for property-based testing via jqwik integration
- Mockito JUnit Jupiter support for advanced mocking patterns
- Backward compatibility with JUnit 4 via vintage engine

### 4. WebSocket & Server-Sent Events (SSE) Protocol Support

- `WebSocketTestClient`: Fluent API for WebSocket handshake, message send/receive, and connection validation
- `ServerSentEventsClient`: SSE stream testing with event assertion and timeout handling
- Automatic documentation of WebSocket frames and SSE events
- Full request/response cycle capture in Markdown documentation

**Use Cases:**
- Real-time chat and notification testing
- Event streaming validation
- Bidirectional communication verification

### 5. Advanced Authentication Providers

- `BearerTokenProvider`: OAuth 2.0 Bearer token management with automatic refresh
- `ApiKeyProvider`: API key injection (header, query parameter, or custom)
- `BasicAuthProvider`: HTTP Basic Authentication with credentials caching
- Custom `AuthenticationProvider` interface for enterprise SSO integration
- Automatic header injection and credential lifecycle management

**Example:**
```java
Request.GET()
    .url(testServerUrl().path("/api/users"))
    .auth(BearerTokenAuth.of(accessToken))
```

### 6. OpenAPI 3.0 Generation

- Automatic OpenAPI schema generation from DocTest request/response examples
- Annotation-driven schema hints (`@ApiSchema`, `@ApiResponse`)
- Generates production-ready OpenAPI specifications from living documentation
- Integration with Swagger UI and other OpenAPI tools

**Output:** Machine-readable OpenAPI specs for Swagger UI, code generation, and API contract testing.

### 7. Java 26 Modernization

DTR is built on **Java 26 (LTS)** with `--enable-preview` enabled, leveraging modern language features:

- **Records** for request/response DTOs and value objects
- **Sealed class hierarchies** for request methods and authentication types
- **Text blocks** for HTML/Markdown templates
- **Virtual threads** for parallel HTTP test execution
- **Pattern matching and exhaustive switch expressions**
- **Sequenced collections** for ordered data structures

**Example:**
```java
record UserDto(String name, String email) {}

sealed interface HttpResult permits HttpSuccess, HttpError {}
record HttpSuccess(int status, String body) implements HttpResult {}
record HttpError(int status, String message) implements HttpResult {}

String describe(HttpResult r) {
    return switch (r) {
        case HttpSuccess(var s, var b) -> "OK %d: %s".formatted(s, b);
        case HttpError(var s, var m)   -> "FAIL %d: %s".formatted(s, m);
    };
}
```

### 8. Enhanced Testing Capabilities

- **Property-Based Testing**: jqwik integration for generating thousands of test cases
- **Chaos/Fault Injection Testing**: WireMock standalone for simulating network faults
- **Stress Testing**: Parallel test execution framework for load testing endpoints
- **Test Reproducibility**: jqwik property test database for minimal failing examples

### 9. Maven 4 Build Toolchain

- **Maven 4.0.0-rc-5+** required (rc-3 minimum)
- **Maven Daemon (mvnd 2.x)** support for faster builds
- Improved dependency resolution and conflict handling
- `--enable-preview` flags built into `.mvn/maven.config`
- Enhanced enforcer rules validating Java 26 + Maven 4
- Build performance improvements through parallel compilation

---

## Breaking Changes

**⚠️ Important:** This is a major version with breaking changes. See migration guide below.

### 1. Markdown-First Output Format (BREAKING)

**Version 1.x:**
```
target/site/dtr/
├── index.html
├── ApiControllerDocTest.html
├── bootstrap/
└── jquery/
```

**Version 2.0.0:**
```
target/docs/
├── README.md          (index)
├── ApiControllerDocTest.md
└── (OpenAPI specs if generated)
```

**Migration Impact:**
- Update CI/CD to look for docs in `target/docs/` instead of `target/site/dtr/`
- If you deploy HTML docs, render Markdown via a static site generator (Jekyll, MkDocs, Hugo, Docusaurus)
- No Javadoc configuration needed—Markdown integrates directly into your README

### 2. Java Version Requirement (BREAKING)

**Version 1.x:** Supports Java 1.8 through 21

**Version 2.0.0:** **Requires Java 26 (LTS)** only

**Migration:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
```

**Why:** Java 26 enables record-based DTOs, sealed hierarchies, pattern matching, and virtual threads for better concurrency.

### 3. HTTP Client 5.x Upgrade (BREAKING)

**Version 1.x:** Apache HttpClient 4.5.x

**Version 2.0.0:** Apache HttpClient 5.6

**Migration:** If you have custom `TestBrowser` implementations:
- Replace `org.apache.httpcomponents:httpclient` 4.5 with `httpclient5` 5.6
- Update URI handling: use `HttpUriRequest` from `org.apache.hc.client5.http.classic.methods`
- Connection pooling now uses `HttpClientConnectionManager` from httpcore5

### 4. Removed HTML Rendering Classes (BREAKING)

- `RenderMachineImpl` and HTML-specific rendering logic removed
- Use new `MarkdownRenderMachine` or implement custom `RenderMachine` interface
- Bootstrap/jQuery assets no longer included

---

## Migration Guide

See the detailed migration guide: **[MIGRATION-1.x-TO-2.0.0.md](https://github.com/seanchatmangpt/dtr/blob/main/MIGRATION-1.x-TO-2.0.0.md)**

### Quick Start for Upgrading

#### Step 1: Update Java to 25 LTS
```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
java -version  # Verify: openjdk 25.x.x
```

#### Step 2: Update Maven Compiler in pom.xml
```xml
<properties>
  <maven.compiler.release>26</maven.compiler.release>
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

#### Step 3: Update DTR Dependency
```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2.0.0</version>
  <scope>test</scope>
</dependency>
```

#### Step 4: Update CI/CD Documentation Path
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

#### Step 5: Refactor Test Class (Optional but Recommended)

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

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

---

## New Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `org.apache.httpcomponents.client5:httpclient5` | 5.6 | HTTP client (upgraded from 4.5) |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.1 | JSON serialization (upgraded) |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | 2.21.1 | XML serialization (upgraded) |
| `junit:junit-jupiter` | 6.0.3 | JUnit 5 core |
| `org.junit.vintage:junit-vintage-engine` | 6.0.3 | JUnit 4 backward compatibility |
| `net.jqwik:jqwik` | 1.9.0 | Property-based testing |
| `org.mockito:mockito-junit-jupiter` | 5.22.0 | Mockito integration with JUnit 5 |
| `com.google.guava:guava` | 33.5.0-jre | Utilities |
| `org.slf4j:slf4j-api` | 2.0.17 | Logging |
| `org.ninja-framework:ninja-core` | 7.0.0 | Integration test framework |
| `com.h2database:h2` | 2.4.240 | Integration test database |
| `org.flywaydb:flyway-core` | 10.21.0 | Database migrations |
| `org.eclipse.jetty:jetty-server` | 9.4.53 | Embedded servlet container |

---

## System Requirements

- **Java:** 25 (LTS) — Required. Must use `JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64`
- **Maven:** 4.0.0-rc-5+ (Maven 4 or later). Do not use Maven 3.
- **Maven Daemon:** mvnd 2.x (optional, but recommended for speed)
- **Build Flags:** `--enable-preview` enabled in `.mvn/maven.config`
- **Operating System:** Linux, macOS, Windows (with Git Bash)

---

## Build and Test Commands

```bash
# Set up Java 26
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Fast build with Maven Daemon (preferred)
mvnd clean install -DskipTests

# Build and test (all modules)
mvnd clean verify

# Build only core module
mvnd clean install -pl dtr-core -DskipTests

# Run tests (single module)
mvnd test -pl dtr-core

# Run integration tests
mvnd clean install -pl dtr-integration-test -am

# Parallel build
mvnd clean verify -T 1C

# Check enforcer rules (Java 26, Maven 4)
mvnd validate
```

---

## Documentation

- **[CHANGELOG_2.0.0.md](https://github.com/seanchatmangpt/dtr/blob/main/CHANGELOG_2.0.0.md)** — Detailed changelog
- **[MIGRATION-1.x-TO-2.0.0.md](https://github.com/seanchatmangpt/dtr/blob/main/MIGRATION-1.x-TO-2.0.0.md)** — Complete migration guide
- **[CLAUDE.md](https://github.com/seanchatmangpt/dtr/blob/main/CLAUDE.md)** — Project architecture and Java 26 features
- **[README-2.0.0.md](https://github.com/seanchatmangpt/dtr/blob/main/README-2.0.0.md)** — New documentation for 2.0.0

---

## Enhancements

- Improved payload serialization with better error messages
- Faster test execution with Maven Daemon (mvnd 2.x)
- Better cookie and session management across test lifecycle
- Enhanced assertion messages for clearer test failures
- Full source and Javadoc JAR generation for Maven Central
- GPG signing for all release artifacts
- Fixed out-of-memory issue with large HTML generation by streaming writes
- Corrected servlet-api scope to prevent test classpath conflicts
- Resolved dependency conflicts with old Ninja framework bundles
- Fixed JAXB module issues on Java 26+

---

## Bug Fixes

- Fixed out-of-memory issue with large HTML generation by streaming writes
- Corrected servlet-api scope to prevent test classpath conflicts
- Resolved dependency conflicts with old Ninja framework bundles
- Fixed JAXB module issues on Java 26+
- Improved error handling in WebSocket connection failures
- Enhanced SSE stream termination and cleanup

---

## Known Limitations

- HTML output from Markdown requires external tools (Pandoc, Jekyll, MkDocs, etc.)
- JUnit 4 tests require migration to JUnit 5 for full feature support
- Custom `TestBrowser` implementations require updates for HttpClient 5.x

---

## Contributors

This release incorporates contributions and improvements from the Docker/Kubernetes era of API testing and modern Java language features.

---

## License

Apache License 2.0

---

## Release Information

- **Release Date:** March 10, 2026
- **Git Tag:** `v2.0.0`
- **Repository:** https://github.com/seanchatmangpt/dtr
- **Maven Central:** https://mvnrepository.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/2.0.0

---

## Getting Started

```bash
# Clone the repository
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Set up Java 26
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Build and test
mvnd clean verify

# Run integration tests
mvnd clean install -pl dtr-integration-test -am

# View generated documentation
cat target/docs/README.md
```

---

**Thank you for using DTR 2.0.0!**

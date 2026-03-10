# DocTester — Claude Code Project Guide

## Project Overview

DocTester is a Java testing framework that generates HTML documentation while running JUnit tests. It provides a fluent API for making HTTP requests and documenting REST API responses.

**Toolchain (non-negotiable):**
- **Java 25** (LTS) — must use `JAVA_HOME` pointing to Java 25
- **Maven 4** — use `mvn` (Maven 4.0.0-rc-5+) or `mvnd` (Maven Daemon) for all builds
- **mvnd 2** — preferred over plain `mvn` for speed; use `mvnd` for all interactive/CI builds (mvnd 2.x targets Maven 4)

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

# Stop the mvnd daemon
mvnd --stop
```

## Linting / Code Quality

There is no dedicated linter configured. Maven enforcer validates Java 25+ and Maven 4+.

```bash
# Check enforcer rules (Java 25, Maven 4)
mvnd validate
```

## Java 25 — Key Features to Use

DocTester targets **Java 25 with `--enable-preview`**. Prefer modern idioms:

### Prefer Modern Java Patterns
- **Records** for DTOs and value objects
- **Sealed classes + pattern matching** for request/response hierarchies
- **`switch` expressions** (exhaustive) over long `if/else` chains
- **Text blocks** for HTML template strings
- **Virtual threads** (`Thread.ofVirtual()`) for async HTTP calls
- **Sequenced collections** (`SequencedMap`, `SequencedSet`) where ordering matters
- **`String.format`** replaced by `formatted()` or text blocks
- **`instanceof` pattern matching** instead of explicit casts

### Preview Features (enabled)
- **Primitive types in patterns** (Java 23+)
- **Unnamed patterns and variables** (`_`)
- **Value types / Valhalla** if available in 25 EA

### Example — prefer this:
```java
// Modern: records + sealed types + pattern matching
sealed interface HttpResult permits Success, Failure {}
record Success(Response response) implements HttpResult {}
record Failure(int statusCode, String body) implements HttpResult {}

HttpResult result = makeRequest(req);
String message = switch (result) {
    case Success(var r)       -> "OK: " + r.statusCode();
    case Failure(var code, _) -> "Error: " + code;
};
```

## Maven 4 — Best Practices

- Use `<release>25</release>` in compiler plugin (not `<source>`/`<target>`)
- Maven 4 uses `.mvn/maven.config` for persistent flags
- `mvnd` (Maven Daemon) is configured via `~/.m2/mvnd.properties`
- Prefer `mvnd` for local dev; CI can use `mvn` with `-T 1C` for parallelism
- Maven 4 supports **build ordering** and improved multi-module support

## Module Structure

```
doctester/
├── doctester-core/          # Core library: DocTester, TestBrowser, HTTP client
│   └── src/
│       ├── main/java/org/r10r/doctester/
│       │   ├── DocTester.java            # Main base class
│       │   ├── testbrowser/              # HTTP client (TestBrowserImpl)
│       │   └── rendermachine/            # HTML doc generation
│       └── test/java/org/r10r/doctester/
└── doctester-integration-test/           # Full integration tests
```

## Code Style

- 4 spaces, no tabs (IntelliJ/Eclipse default Sun style)
- UTF-8 everywhere
- All files must have Apache 2.0 license header
- Javadoc on all public API methods
- Keep classes small and focused (single responsibility)

## Testing Patterns

Tests extend `DocTester` and use the `say*` fluent API:

```java
class MyApiDocTest extends DocTester {

    @Test
    void testUsersEndpoint() {
        sayNextSection("User API");
        say("GET /api/users returns all users");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path("/api/users")));

        sayAndAssertThat("Response is 200", 200, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

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

## Environment Checks

Before building, verify the toolchain:
```bash
java -version          # Must show: openjdk 25.x.x
mvnd --version         # Must show: mvnd 2.0.0-rc-3 / Maven 4.0.0-rc-5
mvn --version          # Must show: Apache Maven 4.0.0-rc-5
echo $JAVA_HOME        # Must be /usr/lib/jvm/java-25-openjdk-amd64
```

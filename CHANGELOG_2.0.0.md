Version 2.0.0
=============

Release Date: 2026-03-10

This is a major release introducing significant architectural improvements, modernization for Java 25 + Maven 4, and new enterprise-grade testing capabilities.

## Breaking Changes

* **Markdown-first Output Format**: Documentation generation now defaults to Markdown instead of HTML. This is a breaking change for consumers who rely on direct HTML output. Custom renderers can still generate HTML via plugins.
* **JUnit 5 Integration**: Primary test framework now uses JUnit 5 (Jupiter) with JUnit 4 support via vintage engine. Existing JUnit 4-based `DocTester` subclasses require migration to `@ExtendWith(DocTesterExtension.class)` or continuation with legacy base class.
* **HTTP Client 5.x Upgrade**: Apache HttpClient upgraded from 4.5.x to 5.6. Some internal APIs changed; custom TestBrowser implementations must be updated.
* **Removed HTML Rendering Classes**: `RenderMachineImpl` and HTML-specific rendering logic removed. Use new `MarkdownRenderMachine` or implement custom `RenderMachine` interface.

## New Features

### Annotation-Based Testing API
* `@DocSection`: Declarative test section definition without `sayNextSection()` calls
* `@DocDescription`: Automatic method documentation extraction
* `@DocNote`, `@DocWarning`, `@DocCode`: Inline documentation annotations for highlighting important information
* Annotations enable cleaner, more readable test code with less boilerplate

### Markdown-First Documentation
* Native Markdown output for all documentation (better for version control, diffs, and documentation generators like Sphinx/Pandoc)
* Markdown renderer produces cleaner, more portable documentation
* Bootstrap HTML output available via optional plugin
* Pretty-printed JSON/XML preserved in Markdown code blocks

### JUnit 5 Support
* `DocTesterExtension`: New Jupiter-compatible extension replacing JUnit 4 base class pattern
* Full integration with JUnit 5 lifecycle hooks and parameterized tests
* Support for property-based testing via jqwik integration
* Mockito JUnit Jupiter support for advanced mocking patterns

### WebSocket & Server-Sent Events (SSE) Protocol Support
* `WebSocketTestClient`: Fluent API for WebSocket handshake, message send/receive, and connection validation
* `ServerSentEventsClient`: SSE stream testing with event assertion and timeout handling
* Automatic documentation of WebSocket frames and SSE events
* Full request/response cycle capture in Markdown documentation

### Advanced Authentication Providers
* `BearerTokenProvider`: OAuth 2.0 Bearer token management with automatic refresh
* `ApiKeyProvider`: API key injection (header, query parameter, or custom)
* `BasicAuthProvider`: HTTP Basic Authentication with credentials caching
* Custom `AuthenticationProvider` interface for enterprise SSO integration
* Automatic header injection and credential lifecycle management

### OpenAPI 3.0 Generation
* Automatic OpenAPI schema generation from DocTest request/response examples
* Annotation-driven schema hints (`@ApiSchema`, `@ApiResponse`)
* Generates production-ready OpenAPI specifications from living documentation
* Integration with Swagger UI and other OpenAPI tools

### Java 25 Modernization
* Records for request/response DTOs and value objects
* Sealed class hierarchies for request methods and authentication types
* Text blocks for HTML/Markdown templates
* Virtual threads for parallel HTTP test execution
* Pattern matching and exhaustive switch expressions
* All with `--enable-preview` enabled in Maven 4

### Enhanced Testing Capabilities
* **Property-Based Testing**: jqwik integration for generating thousands of test cases
* **Chaos/Fault Injection Testing**: WireMock standalone for simulating network faults
* **Stress Testing**: Parallel test execution framework for load testing endpoints
* **Test Reproducibility**: jqwik property test database for minimal failing examples

### Maven 4 Build Toolchain
* Maven 4.0.0-rc-5+ required (rc-3 minimum)
* Maven Daemon (mvnd 2.x) support for faster builds
* Improved dependency resolution and conflict handling
* `--enable-preview` flags built into `.mvn/maven.config`
* Enhanced enforcer rules validating Java 25 + Maven 4

### Dependency Updates
* Apache HttpClient 5.6 (from 4.5)
* Jackson 2.21.1 (comprehensive JSON/XML support)
* Ninja Framework 7.0.0 (integration tests)
* JUnit 5 / Jupiter 6.0.3
* Mockito 5.22.0
* Guava 33.5.0-jre
* SLF4J 2.0.17
* Jetty 9.4.53
* H2 Database 2.4.240
* Flyway 10.21.0

## Migration Guide

### From JUnit 4 to JUnit 5

**Before (JUnit 4)**:
```java
public class ApiDocTest extends DTR {
    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**After (JUnit 5)**:
```java
@ExtendWith(DocTesterExtension.class)
public class ApiDocTest {
    private DTR docTester;

    @BeforeEach
    void setUp(DocTesterContext context) {
        docTester = context.docTester();
    }

    protected Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

### From HTML to Markdown Output

Documents are now generated as `.md` files instead of `.html`. To access generated documentation:
- Before: `target/site/doctester/ApiDocTest.html`
- After: `docs/test/ApiDocTest.md`

Convert to HTML using Pandoc, Jekyll, or other Markdown converters as needed.

### HttpClient 5.x Migration

If you have custom `TestBrowser` implementations:
- Replace `org.apache.httpcomponents:httpclient` 4.5 with `httpclient5` 5.6
- Update URI handling: use `HttpUriRequest` from `org.apache.hc.client5.http.classic.methods`
- Connection pooling now uses `HttpClientConnectionManager` from httpcore5

## Enhancements

* Improved payload serialization with better error messages
* Faster test execution with Maven Daemon (mvnd 2.x)
* Better cookie and session management across test lifecycle
* Enhanced assertion messages for clearer test failures
* Full source and Javadoc JAR generation for Maven Central
* GPG signing for all release artifacts

## Dependencies

See `pom.xml` for complete dependency tree. Key updates:
- **Java**: Requires 25 (LTS)
- **Build**: Maven 4.0.0-rc-5+ (or mvnd 2.x)
- **Testing**: JUnit 5 (Jupiter 6.0.3) + property-based testing (jqwik 1.9.0)
- **HTTP**: Apache HttpClient 5.6 + HttpCore 5.4
- **Serialization**: Jackson 2.21.1 (JSON + XML)
- **Database**: H2 2.4.240 + Flyway 10.21.0 (integration tests)

## Documentation

Comprehensive documentation is now generated as Markdown and available at:
- `docs/test/README.md` (index of all tests)
- `docs/test/ApiDocTest.md` (per-test documentation)
- `docs/` (architecture and API reference)

## Bug Fixes

* Fixed out-of-memory issue with large HTML generation by streaming writes
* Corrected servlet-api scope to prevent test classpath conflicts
* Resolved dependency conflicts with old Ninja framework bundles
* Fixed JAXB module issues on Java 25+

## Contributors

This release incorporates contributions and improvements from the Docker/Kubernetes era of API testing and modern Java language features.

---

## Previous Release

See below for earlier version history.

Version 1.1.11
=============

 * 2018-01-03 Switch to new package structure of org.r10r.

Version 1.1.8
=============

 * 2015-08-01 Update of all libraries to new versions (metacity).

Version 1.1.7
=============

 * 2015-05-22 Print pretty-printed payload (JSON/XML) in report when sayAndMakeRequest(Request)

Version 1.1.6
=============

 * 2014-12-31 Print form parameters in report when sayAndMakeRequest(Request)
 * 2014-12-30 Disabled Javadoc lint checks. (ra)

Version 1.1.5
=============

 * 2014-12-28 Fix for #4. Testcases not teared down properly.

Version 1.1.4
=============

 * 2014-06-28 Added support for DELETE queries + bugfix (dlorych) (https://github.com/doctester/doctester/pull/3)
 * 2014-03-05 Bump to Ninja 3.1.1 (ra)
 * 2014-02-14 Reordered dependencies in integration test so that we do not need
   exclusions any more (just cosmetics) (ra).
 * 2014-02-14 Updated dependencies (ra).
   [INFO]   com.fasterxml.jackson.core:jackson-core ............... 2.3.0 -> 2.3.1
   [INFO]   com.fasterxml.jackson.dataformat:jackson-dataformat-xml ...
   [INFO]                                                           2.3.0 -> 2.3.1
   [INFO]   commons-fileupload:commons-fileupload ................... 1.3 -> 1.3.1
   [INFO]   org.apache.httpcomponents:httpclient .................. 4.2.5 -> 4.3.2
   [INFO]   org.apache.httpcomponents:httpmime .................... 4.2.5 -> 4.3.2
   [INFO]   org.slf4j:jcl-over-slf4j .............................. 1.7.5 -> 1.7.6
   [INFO]   org.slf4j:slf4j-api ................................... 1.7.5 -> 1.7.6
   [INFO]   org.slf4j:slf4j-simple ................................ 1.7.5 -> 1.7.6
   [INFO]

Version 1.1.3
=============

 * 2014-03-03 Add convenience method to set output file name. Adapt tests to work in Windows environment (Stefan Weller).

Version 1.1.2
=============

 * 2014-02-14 Added support for HTTP HEAD requests (Jan Rudert).
 * 2014-02-05 Fixed issue #1. Doctester now independent of webjars version on classpath. (ra)
 * 2014-02-05 Bump to guava 16.0.1 in dtr-core and Ninja 2.5.1 in integration tests. (ra)
 * 2014-01-19 Bump to guava 16.0 in dtr-core and Ninja 2.5.1 in integration tests. (ra)

Version 1.1.1
=============

 * 2013-12-14 Bump to 2.3.0 of all jackson libraries (xml, json binding) (ra).

Version 1.1
=============

 * 2013-12-04 Better documentation how to setup DTR in your own projects (ra).
 * 2013-11-04 Added support so that JUnit falures are marked as red
              in the generated html file. Before they were green what can be
              misleading (ra).
 * 2013-11-04 Integration test bump to Ninja 2.3.0 (ra).

Version 1.0.3
=============

 * 2013-11-07 Better documentation (ra).

Version 1.0.2
=============

 * 2013-11-06 Changed codebase to tabs (ra).
 * 2013-11-06 Json is now rendered with intendation in html reports (pretty printed)(ra).

Version 1.0.1
=============

 * 2013-11-05 Fixed bug with forced logback binding. Binding slf4j should be done by projects using DTR (ra).

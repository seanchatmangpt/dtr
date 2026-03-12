# Known Issues & Limitations

DTR 2.5.0 has excellent coverage of REST APIs and modern Java features. This document lists known limitations and workarounds.

## Version Information

**Current Version:** 2.5.0-SNAPSHOT
**Java Requirement:** 25+
**Maven Requirement:** 4.0.0-rc-5+

---

## Core API

### Limited Support for Streaming Responses

**Status:** Known limitation
**Severity:** Medium

Large streaming responses (video, large files) are buffered entirely in memory. This can cause OutOfMemoryError with responses >100MB.

**Workaround:**
- Test with smaller payloads in documentation tests
- Use separate integration tests for large file streaming
- Monitor heap usage with `MAVEN_OPTS="-Xmx2g"`

---

### Request Body Size Limit

**Status:** Known limitation
**Severity:** Low

DTR buffers request/response bodies for documentation. Very large request bodies (>50MB) may slow down documentation generation.

**Workaround:**
- Keep test payloads reasonably sized
- Use parameterized tests to avoid repeated large bodies
- Consider splitting into multiple smaller requests

---

## Output Rendering

### LaTeX/PDF Output Dependencies

**Status:** Known limitation
**Severity:** Medium (for LaTeX users)

PDF generation requires system-level LaTeX installation (`pdflatex`, `xelatex`, or `lualatex`).

**Workaround:**
- Install TeXLive: `sudo apt-get install texlive-latex-base` (Linux) or `brew cask install mactex` (macOS)
- Alternatively, export as Markdown and use Pandoc for PDF conversion
- Use online LaTeX services (Overleaf) for PDF generation

---

### Blog Export Limited Styling

**Status:** Known limitation
**Severity:** Low

Blog export generates basic HTML. Advanced styling requires manual CSS customization.

**Workaround:**
- Use HTML output for richer styling
- Customize CSS in generated blog files post-generation
- Consider using static site generators (Hugo, Jekyll) with exported Markdown

---

### OpenAPI Generation Limitations

**Status:** Known limitation
**Severity:** Medium

OpenAPI generation infers schema from test payloads. Complex types, recursive structures, or optional fields not present in tests may not be fully documented.

**Workaround:**
- Ensure test payloads cover all important fields
- Manually add OpenAPI extensions in javadoc comments
- Use tools like Swagger Editor to refine generated specs

---

## Real-Time Protocols

### WebSocket Message Ordering

**Status:** Known limitation
**Severity:** Low

High-volume WebSocket message tests may have messages arrive out of documented order due to buffering. Order is preserved within a connection but not guaranteed across rapid message bursts.

**Workaround:**
- Add explicit ordering validation in tests (sequence numbers)
- Use smaller message batches in tests
- Document expected behavior in comments

---

### gRPC Server Reflection

**Status:** Known limitation
**Severity:** Low

DTR doesn't auto-discover gRPC services via server reflection. Services must be explicitly defined in test code.

**Workaround:**
- Explicitly import proto-generated service classes
- Manually construct service stubs in tests
- Generate OpenAPI specs from proto definitions as supplementary documentation

---

### SSE Connection Limits

**Status:** Known limitation
**Severity:** Low

SSE tests don't support multiplexed connections to multiple SSE endpoints in a single test. Connection limit is ~1 per test method.

**Workaround:**
- Test each endpoint in separate test methods
- Use parameterized tests for similar endpoints
- Consider gRPC streaming for multi-endpoint scenarios

---

## Java Language Features

### Limited Pattern Matching for Complex Types

**Status:** Known limitation
**Severity:** Low

Pattern matching in DTR tests works best with simple records and sealed classes. Complex nested types may require manual destructuring.

**Workaround:**
- Use records for test payloads (simpler structure)
- Keep sealed class hierarchies relatively flat
- Use traditional if-else for complex type discrimination

---

### Virtual Thread Performance Varies

**Status:** Known limitation
**Severity:** Low

Virtual thread performance characteristics vary significantly based on workload and JVM tuning. Real measurements are essential.

**Workaround:**
- Always use `System.nanoTime()` for measurements
- Run benchmarks multiple times (100+ iterations)
- Use JMH for reliable performance data

See [Benchmarking](../how-to/benchmarking.md) for measurement best practices.

---

## Performance Characteristics

### Documentation Generation Overhead

**Measured Impact:** ~10-15% test execution time overhead for typical tests

Generating documentation (capturing requests, rendering output) adds measurable overhead. Minimal impact for most tests, but significant for high-volume stress tests.

**Workaround:**
- Disable documentation in stress tests: use standard JUnit instead
- Use fewer `say*()` calls in performance-critical tests
- Run performance benchmarks in separate test suite

---

### Memory Usage with Large Test Suites

**Measured Impact:** ~50MB base + ~1-5MB per 1000 test assertions

Large test suites may exhaust heap memory. Typical `DocTesterContext` overhead is minimal, but accumulated across hundreds of tests it becomes visible.

**Workaround:**
- Set `MAVEN_OPTS="-Xmx2g"` for large test suites
- Break very large test classes into multiple files
- Use test suite organization to enable parallel execution

---

### Maven Build Slow on First Run

**Measured Impact:** First build takes 2-3x longer than subsequent builds (Maven daemon warm-up)

Maven daemon speeds up builds significantly. First build downloads artifacts, initializes daemon, and compiles all modules.

**Workaround:**
- Use `mvnd` instead of `mvn` (Maven daemon is much faster)
- Run `mvnd clean install` once, then incremental builds are fast
- Use `mvnd -T 1C` for parallel module builds

---

## Browser Compatibility

### Old Browser Support

**Status:** HTML5 required

Generated HTML documentation uses modern HTML5 and ES6 JavaScript. Internet Explorer and very old browsers are not supported.

**Workaround:**
- Use modern browsers (Chrome, Firefox, Safari, Edge 2020+)
- For legacy browser support, manually convert HTML to XHTML 1.0

---

### Mobile Browser Rendering

**Status:** Responsive design implemented

HTML output is responsive but designed primarily for desktop viewing. Code blocks may be difficult to read on small screens.

**Workaround:**
- View on desktop/tablet for full experience
- Use Markdown output for mobile-friendly viewing
- Export as PDF for consistent cross-device rendering

---

## Framework Integration

### Spring Boot Testing

**Status:** Supported but limited auto-configuration

DTR works with Spring Boot but doesn't auto-wire Spring test context. Manual setup required.

**Workaround:**
- Use `@SpringBootTest` with manual `testServerUrl()` configuration
- Or use standalone test server in test class
- See [Integrate with Frameworks](../how-to/integrate-with-frameworks.md)

---

### Quarkus Native Image

**Status:** Limited support

Quarkus native image compilation is partially supported. Some reflection-heavy operations may fail in native mode.

**Workaround:**
- Use JVM mode for testing: `quarkus run` instead of native
- For native tests, mark reflection-sensitive classes in `reflection-config.json`

---

## Java Version Compatibility

### Java 26+ Preview Features

**Status:** Required for next release

DTR 2.5.0 targets Java 25. Java 26+ preview features are not yet fully integrated (records and sealed classes become standard, pattern matching expands).

**Workaround:**
- Use Java 25 with `--enable-preview`
- Monitor DTR release notes for Java 26 upgrades
- Preview features remain backward-compatible

---

## Workarounds Summary

| Issue | Severity | Workaround |
|-------|----------|-----------|
| Large streaming responses | Medium | Use smaller payloads in tests |
| LaTeX/PDF dependencies | Medium | Install TeXLive or use alternative PDF tools |
| OpenAPI schema inference | Medium | Ensure test payloads cover all fields |
| Memory on large suites | Medium | Set `MAVEN_OPTS="-Xmx2g"` |
| Build slow on first run | Low | Use mvnd daemon for faster builds |
| WebSocket message ordering | Low | Add explicit ordering validation |
| Pattern matching on complex types | Low | Use simpler record structures |
| Old browser support | Low | Use modern browsers or PDF export |
| Spring Boot integration | Low | Manual test server setup required |

---

## Reporting Issues

If you encounter an issue not listed here:

1. Check [FAQ & Troubleshooting](FAQ_AND_TROUBLESHOOTING.md) for diagnostic steps
2. Enable debug output: `mvnd test -X`
3. Consult relevant tutorial or how-to guide
4. Report to project maintainers with:
   - Java version (`java -version`)
   - Maven version (`mvnd --version`)
   - Minimal test case that reproduces the issue
   - Full error output and stack trace

---

## Future Work

The following limitations are on the roadmap for future releases:

- [ ] Streaming response handling without buffering
- [ ] Enhanced native image support for Quarkus
- [ ] Auto-discovery of gRPC services via reflection
- [ ] Advanced OpenAPI schema inference
- [ ] Java 26 sealed classes as public standard feature (not preview)

Last updated: March 12, 2026

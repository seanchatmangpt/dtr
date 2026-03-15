# Known Issues and Limitations

**DTR 2026.3.0** — Current limitations, workarounds, and version-specific notes.

> **Looking for help with a specific problem?** Start with the [Troubleshooting Guide](../TROUBLESHOOTING.md) for symptom-based solutions. For bugs and feature requests, see [GitHub Issues](https://github.com/seanchatmangpt/dtr/issues).

---

## Version Information

| Property | Value |
|----------|-------|
| Current version | 2026.3.0 |
| Java requirement | 26+ with `--enable-preview` |
| Maven requirement | 4.0.0-rc-3+ |
| mvnd requirement | 2.0.0+ |

---

## Core API

### Documentation generation overhead

**Status:** Known limitation
**Severity:** Low

Generating documentation adds approximately 10–15% overhead to test execution time. Significant only in high-volume stress tests.

**Workaround:**
- Use `ctx.sayBenchmark(...)` for precise measurement; its timing excludes documentation rendering overhead
- Disable documentation in stress tests by using standard JUnit without `DtrExtension`

---

### Memory usage with large test suites

**Measured:** ~50 MB base + ~1–5 MB per 1 000 `say*` calls

Large test suites may exhaust heap memory.

**Workaround:**
```bash
export MAVEN_OPTS="-Xmx2g"
mvnd clean install
```

---

## Output Rendering

### LaTeX PDF generation requires system LaTeX

**Status:** Known limitation
**Severity:** Medium (for LaTeX users)

`RenderMachineLatex` with `PdflatexStrategy` or `XelatexStrategy` requires a system-level LaTeX installation.

**Workaround:**
- Install TeX Live: `sudo apt-get install texlive-latex-base` (Linux) or `brew install --cask mactex` (macOS)
- Use `PandocStrategy` (requires only Pandoc): `new RenderMachineLatex(new ACMTemplate(), new PandocStrategy())`
- Export as Markdown and convert offline

---

### Mermaid diagram export in LaTeX

**Status:** Known limitation
**Severity:** Low

`sayMermaid` and `sayClassDiagram` embed diagram source in LaTeX output as verbatim code. PDF rendering requires `mermaid-cli` (`mmdc`) to be installed and on `PATH`.

**Workaround:**
- Install `mermaid-cli`: `npm install -g @mermaid-js/mermaid-cli`
- Or render Mermaid diagrams in HTML output (works without `mmdc`) and export as PDF via browser

---

### Blog export styling

**Status:** Known limitation
**Severity:** Low

`BlogRenderMachine` generates Markdown formatted for the target platform. Advanced platform-specific styling (Medium `subtitle`, Substack `preview text`) requires manual post-editing.

---

## Code Reflection

### sayCodeModel / sayControlFlowGraph require --enable-preview

**Status:** By design
**Severity:** Low

JEP 516 Code Reflection is a preview feature in Java 26. All `sayCodeModel`, `sayControlFlowGraph`, `sayCallGraph`, and `sayOpProfile` calls will fail with `IllegalAccessError` if `--enable-preview` is not active.

**Workaround:** Ensure `--enable-preview` is in `.mvn/maven.config` and passed to Surefire via `<argLine>`.

---

### sayEvolutionTimeline requires git tags

**Status:** Known limitation
**Severity:** Low

`sayEvolutionTimeline` reads git history for version tags matching `v[0-9]+\.[0-9]+\.[0-9]+`. If the repository has no such tags, the method renders a warning and skips the timeline output.

**Workaround:** Create semver tags: `git tag v2026.3.0`

---

## WireMock and Jetty Warnings

**Status:** Known warning (non-fatal)
**Severity:** Low

WireMock (if present as a test dependency) emits a Jetty 9.4.x deprecation warning on Java 26:

```
WARNING: Jetty 9.4.x is deprecated
```

This is a WireMock/Jetty compatibility issue, not a DTR issue. The warning is harmless; tests run correctly.

**Workaround:** Suppress the warning by adding to Surefire `<argLine>`:
```
-Dorg.eclipse.jetty.util.log.announce=false
```

---

## Java Language Features

### Pattern matching on complex nested types

**Status:** Known limitation
**Severity:** Low

`sayReflectiveDiff` and `sayRecordComponents` work best with flat records. Deeply nested record hierarchies or types with circular references may produce incomplete or truncated output.

**Workaround:**
- Keep record hierarchies flat for documentation targets
- For circular types, use `ctx.sayJson(object)` instead

---

### Virtual thread performance varies by workload

**Status:** Known limitation
**Severity:** Low

`MultiRenderMachine` uses one virtual thread per machine. For tests that produce very small documentation, the fan-out overhead may exceed single-machine execution time.

**Workaround:**
- Use `MultiRenderMachine` only when output volume per test class is substantial
- For lightweight tests, `RenderMachineImpl` is faster

---

## Performance Characteristics

### sayBenchmark: same-JVM measurements

**Status:** Known limitation
**Severity:** Low

`sayBenchmark` measures within the same JVM as the test. JIT compilation state, GC pauses, and other JVM activities can affect readings. Results are suitable for documentation and relative comparisons, not for production SLA claims.

**Workaround:**
- Use JMH for production-grade benchmarking
- Use `sayBenchmark` with high iteration counts (≥ 10 000) to amortize JVM noise
- Always call `ctx.sayEnvProfile()` alongside benchmark results to record the execution context

---

### Maven build slow on first run

**Measured:** First build 2–3x slower than subsequent builds (daemon warm-up + artifact download)

**Workaround:**
- Use `mvnd` (Maven daemon) for all builds; subsequent builds reuse the warm daemon
- Run `mvnd clean install -T 1C` for parallel module builds

---

## Browser Compatibility

### HTML output requires modern browser

Generated HTML uses HTML5 and Mermaid.js (ES2015+). Internet Explorer is not supported.

**Workaround:** Use Chrome, Firefox, Safari, or Edge (2020+). Export as PDF for archival.

---

## Framework Integration

### Spring Boot: no auto-configuration

DTR does not auto-wire Spring test context. Manual setup is required.

**Workaround:** Use `@SpringBootTest` and inject the base URL explicitly:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DtrExtension.class)
class SpringDocTest {

    @LocalServerPort
    int port;

    @Test
    void test(DtrContext ctx) {
        var uri = URI.create("http://localhost:" + port + "/api/users");
        // use java.net.http.HttpClient to call uri
        ctx.say("Port: " + port);
    }
}
```

---

## Workarounds Summary

| Issue | Severity | Workaround |
|-------|----------|------------|
| LaTeX PDF dependencies | Medium | Install TeX Live or use PandocStrategy |
| Mermaid in LaTeX | Low | Install mermaid-cli or use HTML output |
| WireMock Jetty warnings | Low | Suppress with `-Dorg.eclipse.jetty.util.log.announce=false` |
| sayEvolutionTimeline no tags | Low | Create semver git tags |
| sayBenchmark same-JVM noise | Low | Use high iteration counts; call sayEnvProfile |
| Memory on large test suites | Low | Set `MAVEN_OPTS="-Xmx2g"` |
| Spring Boot no auto-wire | Low | Use `@LocalServerPort` and inject URL manually |

---

## Getting Help

### For Common Problems

See the [Troubleshooting Guide](../TROUBLESHOOTING.md) for symptom-based solutions to:
- Setup issues (preview features, dependencies, Java version)
- Build errors (compilation failures, dependency conflicts)
- Runtime failures (extension not loading, tests not executing)
- Output problems (no documentation, empty files, wrong location)
- Performance issues (slow builds, out of memory errors)
- Migration issues (breaking changes from previous versions)

### For Bugs and Feature Requests

- **Search existing issues:** [GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)
- **Report a new issue:** Include:
  - Java version (`java -version`)
  - Maven version (`mvnd --version`)
  - DTR version (from `pom.xml`)
  - Minimal test case that reproduces the issue
  - Full error output and stack trace
  - Debug output: `mvnd test -X`

---

## Roadmap

The following items are planned for future releases:

- [ ] `sayMermaid` LaTeX embedding without external `mmdc` dependency
- [ ] Blog platform API integration for direct publish from `BlogRenderMachine`
- [ ] `sayDocCoverage` integration with JaCoCo for line-level coverage data
- [ ] Quarkus native image support for reflection-heavy methods

Last updated: 2026-03-15

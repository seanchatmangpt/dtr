# FAQ and Troubleshooting Guide

**DTR 2026.2.0+** — Quick answers to common questions.

> **📋 Comprehensive Troubleshooting:** For detailed symptom-based problem solving, see [TROUBLESHOOTING.md](../TROUBLESHOOTING.md).

---

## Quick Setup Checklist

**Before diving in, verify the basics:**

```bash
# 1. Java 26+ required
java -version

# 2. Maven 4.0.0-rc-5+ with mvnd daemon
mvnd --version

# 3. Preview features enabled
cat .mvn/maven.config  # Must contain: --enable-preview

# 4. DTR dependency in test scope
grep -A 5 "dtr-core" pom.xml  # Must show: <scope>test</scope>
```

**If any of these fail, see the [Setup Issues](../TROUBLESHOOTING.md#setup-issues) section in TROUBLESHOOTING.md.**

---

## Top 10 Frequently Asked Questions

### Installation & Setup

#### Q1: Maven says "could not find artifact" or "ClassNotFoundException: DtrExtension"

**Quick Answer:** Verify DTR dependency with correct scope and force update.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.2.0</version>
    <scope>test</scope>  <!-- CRITICAL -->
</dependency>
```

```bash
mvnd clean install -U
```

**🔍 Detailed:** [Missing Dependency](../TROUBLESHOOTING.md#missing-dependency)

---

#### Q2: "Preview features disabled" or "IllegalAccessError" on Code Reflection

**Quick Answer:** Enable preview features in `.mvn/maven.config` and `pom.xml`.

```bash
# .mvn/maven.config
--enable-preview
```

```xml
<!-- pom.xml - add to both compiler and surefire plugins -->
<compilerArgs>
    <arg>--enable-preview</arg>
</compilerArgs>
<argLine>--enable-preview</argLine>
```

**🔍 Detailed:** [Preview Features Not Enabled](../TROUBLESHOOTING.md#preview-features-not-enabled)

---

#### Q3: "Unsupported class version" or "Java 26 is required"

**Quick Answer:** Upgrade to Java 26 and configure JAVA_HOME.

```bash
# macOS
brew install openjdk@26
export JAVA_HOME=/opt/homebrew/opt/openjdk@26

# Linux
sudo apt-get install openjdk-26-jdk
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Verify
java -version  # Should show 26
```

**🔍 Detailed:** [Java Version Mismatch](../TROUBLESHOOTING.md#java-version-mismatch)

---

### Test Execution

#### Q4: Tests compile but don't run ("No tests were executed")

**Quick Answer:** Verify JUnit 5 imports and test class naming.

```java
// Correct imports (JUnit 5)
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;

@ExtendWith(DtrExtension.class)
class MyDocTest {  // Must match *Test.java pattern
    @Test
    void test(DtrContext ctx) {
        ctx.say("Hello, DTR!");
    }
}
```

**🔍 Detailed:** [Tests Not Executed](../TROUBLESHOOTING.md#tests-not-executed)

---

#### Q5: Test passes but no documentation generated

**Quick Answer:** Ensure `@ExtendWith(DtrExtension.class)` and at least one `say*()` call.

```bash
# Diagnose
grep "@ExtendWith" src/test/java/MyDocTest.java  # Must find DtrExtension.class
grep "ctx\\.say" src/test/java/MyDocTest.java    # Must find at least one
ls -la target/docs/test-results/                  # Check output
```

**🔍 Detailed:** [No Documentation Generated](../TROUBLESHOOTING.md#no-documentation-generated)

---

#### Q6: "DtrContext cannot be resolved" or parameter resolution failed

**Quick Answer:** Add `@ExtendWith(DtrExtension.class)` to test class.

```java
@ExtendWith(DtrExtension.class)  // ADD THIS
class MyDocTest {
    @Test
    void test(DtrContext ctx) {  // ctx will be injected
        ctx.say("This works!");
    }
}
```

**🔍 Detailed:** [Extension Not Loading](../TROUBLESHOOTING.md#extension-not-loading)

---

### Output & Rendering

#### Q7: Documentation files are empty or only contain metadata

**Quick Answer:** Ensure unconditional `say*()` calls or use `@DocSection` annotations.

```java
// WRONG (conditional)
if (condition) {
    ctx.say("Only runs if true");  // Empty docs if false
}

// RIGHT (unconditional)
ctx.say("Test verifies authentication");
if (condition) {
    ctx.sayNote("Condition was true");
} else {
    ctx.sayNote("Condition was false");
}

// OR use annotations
@Test
@DocSection("User Authentication")
@DocDescription({"Verifies user login with valid credentials"})
void test(DtrContext ctx) {
    // Annotations generate content even without say* calls
}
```

**🔍 Detailed:** [Empty Documentation Files](../TROUBLESHOOTING.md#empty-documentation-files)

---

#### Q8: Mermaid diagrams show as raw text in HTML

**Quick Answer:** Check network access to Mermaid.js CDN or configure local CDN URL.

```bash
# Test CDN access
curl -I https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js

# Use local Mermaid.js for offline environments
mvnd test -Ddtr.mermaid.cdn=http://localhost/mermaid.min.js
```

**🔍 Detailed:** See [Output Problems](../TROUBLESHOOTING.md#output-problems) in TROUBLESHOOTING.md.

---

### Performance & Build

#### Q9: Build is slow (30+ seconds)

**Quick Answer:** Use mvnd daemon, parallel builds, and build only changed modules.

```bash
# Use mvnd (not mvn)
mvnd clean test  # Reuses daemon, much faster

# Parallel builds
mvnd -T 1C clean test  # 1 thread per CPU core

# Build only what changed
mvnd clean install -pl dtr-core -am

# Restart unresponsive daemon
mvnd --stop
mvnd clean test
```

**🔍 Detailed:** [Slow Builds](../TROUBLESHOOTING.md#slow-builds)

---

#### Q10: Out of memory during build

**Quick Answer:** Increase Maven and Surefire heap sizes.

```bash
# Maven heap
export MAVEN_OPTS="-Xmx2g"
mvnd clean test

# Or configure in ~/.m2/mvnd.properties
mvnd.maxHeapSize=2g
```

```xml
<!-- Surefire heap in pom.xml -->
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Xmx1g --enable-preview</argLine>
    </configuration>
</plugin>
```

**🔍 Detailed:** [Out of Memory](../TROUBLESHOOTING.md#out-of-memory)

---

## Migration from DTR 2.x to 2026.2.0+

### Q: What changed in DTR 2026?

**Key Breaking Changes:**

1. **Package structure changed:**
   ```java
   // Old (2.x)
   import io.github.seanchatmangpt.dtr.core.DtrContext;

   // New (2026.2.0+)
   import io.github.seanchatmangpt.dtr.junit5.DtrContext;
   ```

2. **Extension registration:**
   ```java
   // Old (2.x)
   class MyDocTest extends DtrTest {
       @Test
       void test() {
           say("Hello");
       }
   }

   // New (2026.2.0+ - recommended)
   @ExtendWith(DtrExtension.class)
   class MyDocTest {
       @Test
       void test(DtrContext ctx) {
           ctx.say("Hello");
       }
   }
   ```

3. **HTTP client layer removed in 2.6.0:**
   - `sayAndMakeRequest()`, `Request`, `Response` no longer exist
   - Use standard `java.net.http.HttpClient` instead
   - Document results with `ctx.sayCode()`, `ctx.sayJson()`, `ctx.sayAssertions()`

**🔍 Detailed:** [v2026 Breaking Changes](../TROUBLESHOOTING.md#v2026-breaking-changes)

---

### Q: Where did `sayAndMakeRequest` go?

**Answer:** Removed in v2.6.0. Use standard HTTP clients and document results manually.

```java
// Old (v2.5.x)
Response r = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));
sayAndAssertThat("Status is 200", r.httpStatus(), equalTo(200));

// New (v2.6.0+)
var client = java.net.http.HttpClient.newHttpClient();
var request = java.net.http.HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/users"))
    .GET().build();
var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

ctx.sayCode("GET /api/users", "http");
ctx.sayAssertions(Map.of("Status is 200", response.statusCode() == 200));
ctx.sayJson(response.body());
```

---

### Q: Where did `sayAndAssertThat` go?

**Answer:** Removed in v2.6.0. Use JUnit 5 assertions for testing, `ctx.sayAssertions()` for documentation.

```java
// Assert (throws if false)
assertEquals(200, response.statusCode(), "Status must be 200");
assertTrue(response.body().contains("Alice"), "Body must contain Alice");

// Document the assertions
ctx.sayAssertions(Map.of(
    "Status is 200",         response.statusCode() == 200,
    "Body contains Alice",   response.body().contains("Alice")
));
```

---

### Q: What about WebSocket/SSE APIs and authentication helpers?

**Answer:** All removed in v2.6.0 as part of HTTP client layer simplification.

- `WebSocketClient`, `ServerSentEventsClient` → Use `jakarta.websocket.*` or `java.net.http.HttpClient`
- `BearerTokenAuth`, `ApiKeyAuth`, `BasicAuth` → Pass tokens as headers manually
- DTR 2026.2.0+ focuses on documentation generation, not HTTP testing

---

## Getting Help

### Debug Commands

```bash
# Run specific test with verbose output
mvnd test -Dtest=MyDocTest -v

# Enable debug logging
mvnd test -X

# Check surefire reports
cat target/surefire-reports/MyDocTest.txt

# Verify versions
java -version          # Should be 26+
mvnd --version         # Should be 2.0.0+
cat .mvn/maven.config  # Must have --enable-preview
```

---

### Additional Resources

- **🔍 Comprehensive Troubleshooting:** [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)
- **📖 API Reference:** [say* Core API Reference](request-api.md)
- **🎨 RenderMachine:** [RenderMachine API](rendermachine-api.md)
- **📝 80/20 Quick Reference:** [Quick Start Guide](80-20-quick-reference.md)
- **🏗️ Architecture:** [How DTR Works](../explanation/how-dtr-works.md)
- **🐛 Report Issues:** [GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)

---

## Additional FAQ Topics

### LaTeX/PDF Generation

#### Q: `pdflatex not found` or LaTeX compilation fails

**Quick Answer:** Install LaTeX or use `PandocStrategy`.

```bash
# Install LaTeX
# Ubuntu/Debian
sudo apt-get install texlive-latex-base texlive-fonts-recommended

# macOS
brew install --cask mactex

# Verify
pdflatex --version
```

```java
// Or use Pandoc strategy (requires pandoc installed)
ctx.setRenderMachine(
    new RenderMachineLatex(new IEEETemplate(), new PandocStrategy())
);
```

---

### Git Integration

#### Q: `sayEvolutionTimeline` shows "git history unavailable"

**Quick Answer:** Ensure git repository has semver version tags.

```bash
# Check for tags
git log --oneline --decorate | head -20
git tag | grep -E '^v[0-9]'

# Create missing tags
git tag v2026.2.0
git tag v2026.2.0

# Verify
git tag -l
```

---

### Advanced Debugging

#### Q: How do I diagnose "No Docs Generated"?

```bash
# 1. Did the test run?
mvnd test -Dtest=MyDocTest
# Look for: Tests run: 1

# 2. Is extension loaded?
grep "@ExtendWith" src/test/java/MyDocTest.java
# Must find: @ExtendWith(DtrExtension.class)

# 3. Are say* methods called?
grep "ctx\\.say" src/test/java/MyDocTest.java
# Must find at least one

# 4. Check output directory
ls -la target/docs/test-results/
# Should show: MyDocTest.md
```

#### Q: How do I diagnose "Compilation Errors"?

```bash
# 1. Check Java version
java -version
# Must be: openjdk version "26.x.x"

# 2. Check preview enabled
cat .mvn/maven.config
# Must contain: --enable-preview

# 3. Verify dependency
grep -A 3 "dtr-core" pom.xml
# Must contain: <scope>test</scope>

# 4. Clean rebuild
mvnd clean compile -U
```

#### Q: How do I diagnose "Runtime Failures"?

```bash
# 1. Check imports
grep "import.*dtr" src/test/java/MyDocTest.java
# Must be: io.github.seanchatmangpt.dtr.junit5.*

# 2. Verify JUnit 5
grep "import org.junit" src/test/java/MyDocTest.java
# Must be: org.junit.jupiter.api.*

# 3. Check test naming
ls src/test/java/**/*Test.java
# Must match: *Test.java pattern

# 4. Run with verbose output
mvnd test -Dtest=MyDocTest -v
```

---

**Last Updated:** 2026-03-15
**DTR Version:** 2026.2.0+
**For comprehensive troubleshooting, see [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)**

---

## v2.6.0 Migration Questions

### Q: `sayAndMakeRequest` is not found — where did it go?

`sayAndMakeRequest(Request)` was **removed in v2.6.0** along with the entire HTTP client layer (`TestBrowser`, `Request`, `Response`, `Url`).

DTR 2.6.0 focuses on documentation generation, not HTTP testing. To test HTTP APIs alongside documentation:

- Use standard HTTP client libraries (`java.net.http.HttpClient`, OkHttp, RestAssured) in your tests
- Document the results with `ctx.sayCode(...)`, `ctx.sayJson(...)`, `ctx.sayTable(...)`, and `ctx.sayAssertions(Map.of("Status is 200", status == 200))`

**Example migration:**

Before (v2.5.x):
```java
Response r = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));
sayAndAssertThat("Status is 200", r.httpStatus(), equalTo(200));
```

After (v2.6.0):
```java
var client = java.net.http.HttpClient.newHttpClient();
var request = java.net.http.HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/users"))
    .GET().build();
var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

ctx.sayCode("GET /api/users", "http");
ctx.sayAssertions(Map.of("Status is 200", response.statusCode() == 200));
ctx.sayJson(response.body());
```

---

### Q: `sayAndAssertThat` is not found — where did it go?

`sayAndAssertThat(String, T, Matcher)` was **removed in v2.6.0**.

**Replacement:** Use JUnit 5 assertions for the assertion, and `ctx.sayAssertions(Map)` to document results:

```java
// Assert (throws if false)
assertEquals(200, response.statusCode(), "Status must be 200");
assertTrue(response.body().contains("Alice"), "Body must contain Alice");

// Document the assertions
ctx.sayAssertions(Map.of(
    "Status is 200",         response.statusCode() == 200,
    "Body contains Alice",   response.body().contains("Alice")
));
```

---

### Q: Sealed class errors — `RenderMachine cannot be subclassed`

In v2.5.0 `RenderMachine` was changed from `sealed` to plain `abstract`. If you are seeing sealed class errors, you are likely still on v2.4.x.

**Fix:** Upgrade to 2.6.0 in `pom.xml`:

```xml
<version>2.6.0</version>
```

Then `mvnd clean install`. Your custom `RenderMachine` subclasses will compile without issue.

---

### Q: WebSocket / SSE APIs are not found

`WebSocketClient`, `WebSocketSession`, `ServerSentEventsClient`, and all SSE stream APIs were **removed in v2.6.0**.

These are no longer part of DTR. To test WebSocket or SSE endpoints:
- Use Jakarta WebSocket (`jakarta.websocket.*`) directly
- Use standard `java.net.http.HttpClient` with SSE response body handlers
- Document results manually with `ctx.sayTable(...)` and `ctx.sayCode(...)`

---

### Q: `BearerTokenAuth` / `ApiKeyAuth` / `BasicAuth` are not found

All authentication helper classes were **removed in v2.6.0** with the rest of the HTTP client layer.

Authenticate directly using standard Java HTTP clients and pass tokens as headers manually.

---

## Test Execution

### Q: Tests compile but don't run

**Error:** `@Test` annotation not recognized or `DtrContext` cannot be resolved

**Solution:**
Ensure you are using JUnit 5 and the correct imports:

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void test(DtrContext ctx) { ... }
}
```

---

### Q: Test runs but finds no documentation output

**Error:** `target/docs/test-results/` directory is empty or missing

**Solution:**
1. Verify tests call at least one `say*()` method
2. Check the output directory:
   ```bash
   ls -la target/docs/test-results/
   ```
3. Run a specific test with verbose output:
   ```bash
   mvnd test -Dtest=MyDocTest -v
   cat target/docs/test-results/MyDocTest.md
   ```

---

## Output and Rendering

### Q: LaTeX/PDF generation fails

**Error:** `pdflatex not found` or LaTeX compilation error

**Solution:**
1. Ensure LaTeX is installed:
   ```bash
   pdflatex --version
   ```

2. If not installed:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install texlive-latex-base texlive-fonts-recommended

   # macOS
   brew install --cask mactex
   ```

3. Alternatively, use `PandocStrategy`:
   ```java
   ctx.setRenderMachine(
       new RenderMachineLatex(new IEEETemplate(), new PandocStrategy())
   );
   ```

---

### Q: Mermaid diagrams are not rendering in HTML output

**Error:** Mermaid diagrams show as raw text in the HTML file

**Solution:**
`RenderMachineImpl` includes Mermaid.js from CDN. Check network access. For offline environments, serve Mermaid.js locally and configure the CDN URL via system property:

```bash
mvnd test -Ddtr.mermaid.cdn=http://localhost/mermaid.min.js
```

---

### Q: `sayEvolutionTimeline` renders a warning instead of a timeline

**Error:** `"git history unavailable — skipping timeline"`

**Solution:**
`sayEvolutionTimeline` requires a git repository with semver version tags. Verify:

```bash
git log --oneline --decorate | head -20  # should show tags like v2.5.0, v2.6.0
git tag | grep -E '^v[0-9]'              # list version tags
```

If no tags exist, create them:
```bash
git tag v2.6.0
```

---

## Build and Performance

### Q: Build is slow

**Solution:**
1. Use mvnd daemon (much faster than plain mvn):
   ```bash
   mvnd clean install  # reuses daemon
   ```

2. Build only changed modules:
   ```bash
   mvnd clean install -pl dtr-core
   ```

3. Use parallel builds:
   ```bash
   mvnd -T 1C clean install
   ```

---

### Q: Maven daemon becomes unresponsive

```bash
mvnd --stop        # stop all daemon processes
mvnd clean install # restart
```

---

### Q: Out of memory during build

```bash
export MAVEN_OPTS="-Xmx2g"
mvnd clean install
```

---

## Debugging

### Q: How do I debug a failing test?

```bash
mvnd test -pl dtr-integration-test -Dtest=YourTest -v
mvnd test -pl dtr-integration-test -Dtest=YourTest -X
cat target/surefire-reports/YourTest.txt
```

---

### Q: How do I verify Java/Maven/mvnd versions?

```bash
java -version          # should be 25+
mvnd --version         # should be 2.0.0+
mvn --version          # should be 4.0.0-rc-3+
echo $JAVA_HOME        # verify path
```

---

## Getting help

- Check the quick reference: [80/20 Quick Reference](80-20-quick-reference.md)
- Full API reference: [say* Core API Reference](request-api.md)
- RenderMachine reference: [RenderMachine API](rendermachine-api.md)
- Architecture explanation: `docs/explanation/how-dtr-works.md`

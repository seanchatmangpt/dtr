# FAQ and Troubleshooting Guide

**DTR 2.6.0** — Common issues, solutions, and diagnostic commands.

---

## Installation and Setup

### Q: Maven says "could not find artifact"

**Error:** `The requested required projects <module> do not exist`

**Solution:**
1. Ensure you are using the v2.6.0 dependency:
   ```xml
   <dependency>
       <groupId>io.github.seanchatmangpt.dtr</groupId>
       <artifactId>dtr-core</artifactId>
       <version>2.6.0</version>
       <scope>test</scope>
   </dependency>
   ```

2. Clear Maven cache and rebuild:
   ```bash
   mvnd clean install -U
   ```

3. If Maven Central download fails, start the proxy and retry:
   ```bash
   python3 maven-proxy-auth.py &
   mvnd clean install -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
   ```

---

### Q: "too many authentication attempts" from Maven Central

**Error:** `Failed to authenticate with the remote repository at central`

**Solution:**
Maven Central rate-limits authentication attempts. Start the proxy server:

```bash
# 1. Start proxy
python3 maven-proxy-auth.py &

# 2. Add to .mvn/jvm.config
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1

# 3. Rebuild
mvnd clean install
```

**Troubleshooting the proxy:**
```bash
pkill -f maven-proxy
python3 maven-proxy-auth.py &
ps aux | grep maven-proxy  # verify running
echo $https_proxy           # check env var
```

---

### Q: Java version error

**Error:** `Unsupported class version` or `Java 26 is required`

**Solution:**
```bash
java -version  # verify 25+
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
java -version  # verify again
mvnd clean install
```

---

### Q: "Preview features disabled" error

**Error:** `error: preview features are not enabled` or `IllegalAccessError` on Code Reflection methods

**Solution:**
1. Add to `.mvn/maven.config`:
   ```
   --enable-preview
   ```

2. Add to `pom.xml` compiler and surefire plugins:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
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
       <configuration>
           <argLine>--enable-preview</argLine>
       </configuration>
   </plugin>
   ```

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

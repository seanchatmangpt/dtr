# DTR Migration Guide: 2.4.x → 2.5.x → 2.6.0

**Last Updated:** March 14, 2026

---

## Overview

This guide covers two migrations:

1. [Migrating 2.4.x → 2.5.x](#migrating-24x--25x) — RenderMachine and Maven Central
2. [Migrating 2.5.x → 2.6.0](#migrating-25x--260) — HTTP stack removal and new say* methods

---

## Migrating 2.4.x → 2.5.x

### Key Changes at a Glance

| Aspect | 2.4.0 | 2.5.0 | Breaking? |
|--------|-------|-------|-----------|
| RenderMachine | Sealed class | Abstract base class | No* |
| Maven Central | Not available | Published via Sonatype | No (additive) |
| Java Version | Java 25 | Java 25+ | No (same) |
| Preview Flags | Required | Enforced | No (same) |
| Introspection Methods | All supported | All supported + cached | No (improvement) |

*Only custom RenderMachine implementations require changes (rare)

### Breaking Changes in 2.5.0

#### RenderMachine is No Longer Sealed

**Impact Level:** Low (most users unaffected)

**What Changed:** `RenderMachine` transitioned from `sealed class` to `abstract class`. This enables implementations across multiple packages.

**What Still Works (No Changes Needed):**
```java
// All of this works identically in 2.5.0
RenderMachine machine = ctx.getRenderMachine();  // ✓
machine.say("content");                          // ✓
machine.sayJson(data);                           // ✓
machine.finishAndWriteOut();                     // ✓
```

**What Breaks (Rare Edge Case):**
```java
// THIS NO LONGER WORKS (sealed class type narrowing)
boolean isValidRenderer = renderer instanceof RenderMachineImpl
    || renderer instanceof RenderMachineLatex;

// New approach: use interface checks instead
```

### Step-by-Step Migration (2.4.x → 2.5.x)

**Step 1:** Update `pom.xml`:
```xml
<version>2.5.0</version>
```

**Step 2:** Verify Java 25:
```bash
java -version  # Must show openjdk 25 or higher
```

**Step 3:** Run clean build:
```bash
mvnd clean test
```

**Step 4 (optional):** If you have a custom RenderMachine, refactor from `sealed` to `extends`:

Before (2.4.0):
```java
public sealed class MyCustomRenderer implements RenderMachine permits ... {
    @Override
    public void say(String text) { ... }
}
```

After (2.5.0):
```java
public final class MyCustomRenderer extends RenderMachine {
    @Override
    public void setFileName(String fileName) { ... }

    @Override
    public void finishAndWriteOut() { ... }

    @Override
    public void say(String text) { ... }
}
```

---

## Migrating 2.5.x → 2.6.0

### Summary

DTR 2.6.0 is a **breaking release** for projects that used the built-in HTTP stack. The entire HTTP client layer has been removed. All tests must use `java.net.http.HttpClient` for HTTP calls, and standard AssertJ/Hamcrest for assertions.

In exchange, 14 powerful new `say*` methods are added for diagrams, benchmarks, coverage, and more.

### What Was Removed

The following APIs are **completely gone** in 2.6.0:

| Removed | Replacement |
|---------|-------------|
| `sayAndMakeRequest(Request)` | `java.net.http.HttpClient` + `ctx.sayJson(response)` |
| `sayAndAssertThat(String, T, Matcher)` | `assertThat(...)` from AssertJ or Hamcrest |
| `makeRequest(Request)` | `HttpClient.send(...)` |
| `Request.GET()`, `Request.POST()`, `Request.PUT()`, `Request.DELETE()` | `HttpRequest.newBuilder()` |
| `Response`, `response.httpStatus()`, `response.payloadAs()` | `HttpResponse<String>` |
| `testServerUrl()`, `TestBrowser`, `TestBrowserImpl` | No replacement (start servers independently) |
| `WebSocketClient`, `WebSocketSession` | `java.net.http.WebSocket` |
| `ServerSentEventsClient` | Standard SSE parsing |
| `BearerTokenAuth`, `ApiKeyAuth`, `BasicAuth` | Standard `Authorization` header |
| `getCookies()`, `getCookieWithName()`, `sayAndGetCookies()` | `java.net.CookieManager` |

### What Was Added

| New Method | Purpose |
|-----------|---------|
| `sayBenchmark(String, Runnable)` | Inline microbenchmark with System.nanoTime() |
| `sayBenchmark(String, Runnable, int, int)` | Configurable warmup and measure rounds |
| `sayMermaid(String)` | Raw Mermaid DSL fenced block |
| `sayClassDiagram(Class<?>...)` | Auto Mermaid classDiagram via reflection |
| `sayControlFlowGraph(Method)` | Mermaid CFG from Code Reflection IR |
| `sayCallGraph(Class<?>)` | Mermaid call graph |
| `sayOpProfile(Method)` | Operation count table |
| `sayDocCoverage(Class<?>...)` | Documentation coverage matrix |
| `sayEnvProfile()` | Environment snapshot (Java, OS, heap, DTR) |
| `sayRecordComponents(Class<? extends Record>)` | Record schema table |
| `sayException(Throwable)` | Structured exception documentation |
| `sayAsciiChart(String, double[], String[])` | Unicode bar chart |
| `sayContractVerification(Class<?>, Class<?>...)` | Interface contract coverage |
| `sayEvolutionTimeline(Class<?>, int)` | Git log timeline for a class |

### Step-by-Step Migration (2.5.x → 2.6.0)

#### Step 1: Update the version

```xml
<version>2.6.0</version>
```

#### Step 2: Switch to JUnit 5

DTR 2.6.0 requires JUnit 5. Replace JUnit 4 with JUnit 5 Jupiter in your `pom.xml`:

```xml
<!-- Remove this -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.x</version>
    <scope>test</scope>
</dependency>

<!-- Add this -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

#### Step 3: Update test class structure

Before (2.5.x, JUnit 4):
```java
public class MyApiDocTest extends DTR {

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testGetUsers() {
        sayNextSection("List Users");
        Response response = sayAndMakeRequest(Request.GET()
            .url(testServerUrl().path("/api/users")));
        sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
    }
}
```

After (2.6.0, JUnit 5):
```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class MyApiDocTest {

    @Test
    void testGetUsers(DtrContext ctx) throws Exception {
        ctx.sayNextSection("List Users");

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/users"))
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.say("Response status: " + response.statusCode());
        ctx.sayJson(response.body());
    }
}
```

#### Step 4: Replace sayAndAssertThat

Before:
```java
sayAndAssertThat("Status is 200", response.httpStatus(), equalTo(200));
```

After (use standard AssertJ):
```java
assertThat(response.statusCode()).isEqualTo(200);
ctx.say("Status: " + response.statusCode()); // document separately if needed
```

#### Step 5: Run the build

```bash
mvnd clean test
```

Fix any remaining compilation errors from removed APIs. The compiler will list all references to removed classes.

### Find All Removed API References

Run this to find all files that reference removed APIs:

```bash
grep -rn "sayAndMakeRequest\|sayAndAssertThat\|Request\.GET\|Request\.POST\|testServerUrl\|TestBrowser\|WebSocketClient\|ServerSentEventsClient\|BearerTokenAuth\|ApiKeyAuth\|BasicAuth" src/test/
```

---

## Validation Checklist

After upgrading to 2.6.0, verify:

- [ ] `pom.xml` version is `2.6.0`
- [ ] JUnit 5 Jupiter dependency added, JUnit 4 removed
- [ ] All test classes use `@ExtendWith(DtrExtension.class)` (not `extends DTR`)
- [ ] All `@Test` methods accept `DtrContext ctx` parameter
- [ ] No references to `sayAndMakeRequest`, `sayAndAssertThat`, `Request`, `Response`, `testServerUrl()`
- [ ] HTTP calls use `java.net.http.HttpClient`
- [ ] Assertions use AssertJ `assertThat(...)` or Hamcrest `assertThat(...)`
- [ ] `mvnd clean test` completes successfully
- [ ] Output appears in `target/docs/test-results/`

---

## Getting Help

If you encounter issues:

1. Check compilation errors — the compiler lists all removed API references
2. See [add-to-maven.md](add-to-maven.md) for the correct 2.6.0 pom.xml structure
3. Report issues at https://github.com/seanchatmangpt/dtr/issues

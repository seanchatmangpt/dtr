# FAQ and Troubleshooting Guide

**DTR 2026.4.1** — Quick answers to common questions.

> **🆕 Field Injection Update:** DTR 2026.4.1 introduces `@DtrTest` and `@DtrContextField` annotations for cleaner test patterns. Field injection is now the recommended approach for most use cases.

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
    <version>2026.4.1</version>
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

**Quick Answer:** Verify JUnit Jupiter 6 imports and test class naming.

```java
// Correct imports (JUnit Jupiter 6)
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

## Field Injection & @DtrTest Feature (NEW in 2026.4.1)

### Q11: What is field injection and why should I use it?

**Quick Answer:** Field injection lets you declare `DtrContext` once at the class level instead of passing it as a parameter. This is the recommended pattern for most tests.

```java
// OLD: Parameter injection (still supported)
@ExtendWith(DtrExtension.class)
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }

    @Test
    void createUser(DtrContext ctx) {
        ctx.say("Creates a new user");
    }
}

// NEW: Field injection (recommended)
@DtrTest  // Combines @ExtendWith + @AutoFinishDocTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }

    @Test
    void createUser() {
        ctx.say("Creates a new user");
    }
}
```

**Benefits:**
- Cleaner method signatures (no parameter clutter)
- Context available across all test methods
- Automatic setup with `@DtrTest` annotation
- Better encapsulation with private fields

**🔍 Detailed:** [Field Injection Guide](../../tutorials/field-injection-guide.md)

---

### Q12: How do I use the new @DtrTest annotation?

**Quick Answer:** `@DtrTest` combines `@ExtendWith(DtrExtension.class)` and `@AutoFinishDocTest` into a single annotation.

```java
// OLD way
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class MyTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Hello");
    }
}

// NEW way (simplified)
@DtrTest
class MyTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Hello");
    }
}
```

**🔍 Detailed:** [@DtrTest API Reference](../annotation-reference.md#dtrotest)

---

### Q13: What's the difference between field injection and inheritance patterns?

**Quick Answer:** Field injection is the modern approach; inheritance is the legacy pattern.

**Field Injection (Recommended):**
```java
@DtrTest
class MyTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Testing something");
    }
}
```

**Inheritance (Legacy):**
```java
@DtrTest
class MyTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    void test() {
        say("Testing something");  // Direct method access
    }
}
```

**Field Injection Benefits:**
- ✅ Clean method signatures
- ✅ Better encapsulation (private ctx)
- ✅ Works with any class (no inheritance constraints)
- ✅ Full compatibility with JUnit extensions

**When to Use Inheritance:**
- Only for legacy codebases
- When you need direct `say()` method access
- When working with existing DTR 2.x code

---

### Q14: Why is my @DtrContextField not being injected?

**Quick Answer:** Ensure you have `@DtrTest` on the class and correct field setup.

**Common Issues:**

1. **Missing @DtrTest annotation:**
   ```java
   // WRONG - Missing @DtrTest
   class MyTest {
       @DtrContextField
       private DtrContext ctx;
   }

   // CORRECT
   @DtrTest
   class MyTest {
       @DtrContextField
       private DtrContext ctx;
   }
   ```

2. **Field must be non-static:**
   ```java
   // WRONG
   @DtrContextField
   private static DtrContext ctx;  // Static fields not supported

   // CORRECT
   @DtrContextField
   private DtrContext ctx;  // Instance field
   ```

3. **Wrong field type:**
   ```java
   // WRONG
   @DtrContextField
   private String ctx;  // Must be DtrContext type

   // CORRECT
   @DtrContextField
   private DtrContext ctx;
   ```

**🔍 Detailed:** [Field Injection Troubleshooting](../TROUBLESHOOTING.md#field-injection-issues)

---

### Q15: Can I use field injection with parameter injection?

**Quick Answer:** Yes! You can mix both patterns in the same class.

```java
@DtrTest
class MixedTest {
    @DtrContextField
    private DtrContext ctx;  // Available to all methods

    @Test
    void basicTest() {
        ctx.say("Using field injection");
    }

    @Test
    void testWithExplicitCtx(DtrContext explicitCtx) {
        explicitCtx.say("Using parameter injection");
        ctx.say("Also using field injection");
    }
}
```

This is useful for:
- Tests that need custom setup via parameter injection
- Tests that benefit from the convenience of field injection

**🔍 Detailed:** [Mixed Injection Patterns](../annotation-reference.md#mixed-injection)

---

### Q16: What is autoFinish and when should I disable it?

**Quick Answer:** `@DtrTest(autoFinish = true)` creates separate files per test method. Use `false` for unified documentation.

```java
// Separate files per test (default)
@DtrTest(autoFinish = true)
class SeparateFilesTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test1() {
        ctx.say("Test 1 content");
    }  // Creates: SeparateFilesTest-test1.md

    @Test
    void test2() {
        ctx.say("Test 2 content");
    }  // Creates: SeparateFilesTest-test2.md
}

// Single file for all tests
@DtrTest(autoFinish = false)
class UnifiedFileTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test1() {
        ctx.say("Test 1 content");
    }

    @Test
    void test2() {
        ctx.say("Test 2 content");
    }
    // Creates: UnifiedFileTest.md (contains both tests)
}
```

**When to use `autoFinish = false`:**
- Tests are logically related
- You want unified documentation sections
- Performance optimization for large test suites

---

### Q17: How do I customize the output filename?

**Quick Answer:** Use `fileName` attribute in `@DtrTest`.

```java
@DtrTest(fileName = "user-api-documentation")
class UserServiceTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void getUser() {
        ctx.say("Retrieves user by ID");
    }
}
// Creates: user-api-documentation.md instead of UserServiceTest.md
```

Useful for:
- Better file organization
- Avoiding naming conflicts
- More meaningful documentation names

---

### Q18: Field injection compilation errors

**Error:** `@DtrContextField cannot be resolved` or `DtrContext cannot be resolved`

**Solution:**
```java
// Correct imports
import io.github.seanchatmangpt.dtr.junit5.DtrTest;
import io.github.seanchatmangpt.dtr.junit5.DtrContextField;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import org.junit.jupiter.api.Test;
```

**Common Fixes:**
1. Ensure you're using DTR 2026.4.1+
2. Check all required imports are present
3. Verify dependencies in `pom.xml`:
   ```xml
   <dependency>
       <groupId>io.github.seanchatmangpt.dtr</groupId>
       <artifactId>dtr-core</artifactId>
       <version>2026.4.1</version>
       <scope>test</scope>
   </dependency>
   ```

---

### Q19: Migration from inheritance to field injection

**Quick Answer:** Replace `extends DtrTest` with `@DtrTest` and add `@DtrContextField`.

**Migration Guide:**

**Before (Inheritance):**
```java
import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

public class UserServiceTest extends DtrTest {

    @Test
    public void getUser() {
        say("Retrieves user by ID");
    }

    @Test
    public void createUser() {
        say("Creates new user");
    }
}
```

**After (Field Injection):**
```java
import io.github.seanchatmangpt.dtr.junit5.DtrTest;
import io.github.seanchatmangpt.dtr.junit5.DtrContextField;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import org.junit.jupiter.api.Test;

@DtrTest
public class UserServiceTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    public void getUser() {
        ctx.say("Retrieves user by ID");
    }

    @Test
    public void createUser() {
        ctx.say("Creates new user");
    }
}
```

**Migration Benefits:**
- ✅ No inheritance constraints
- ✅ Cleaner class hierarchy
- ✅ Better encapsulation
- ✅ Works with other JUnit extensions

---

### Q20: Field injection vs parameter injection - which should I choose?

**Quick Answer:** Use field injection for most cases, parameter injection for specific needs.

**Choose Field Injection When:**
- ✅ Test class has multiple test methods
- ✅ All tests use the same DtrContext
- ✅ You want clean method signatures
- ✅ You need better encapsulation
- ✅ Working with existing classes that can't extend DtrTest

**Choose Parameter Injection When:**
- ✅ Each test needs different setup
- ✅ You prefer explicit dependencies
- ✅ Tests have varying documentation requirements
- ✅ Migrating from older DTR versions
- ✅ Working with dependency injection frameworks

**Hybrid Approach:**
```java
@DtrTest
class HybridTest {
    @DtrContextField
    private DtrContext ctx;  // For common operations

    @Test
    void basicTest() {
        ctx.say("Basic test");
    }

    @Test
    void testWithCustomSetup(DtrContext customCtx) {
        customCtx.say("Test with custom setup");
    }
}
```

**🔍 Detailed:** [Pattern Selection Guide](../80-20-quick-reference.md#choosing-patterns)

---

### Q21: Performance considerations with field injection

**Quick Answer:** Field injection has minimal overhead and shares RenderMachine efficiently.

**Performance Characteristics:**
- **Memory**: Each test method gets its own DtrContext instance
- **RenderMachine**: Shared across all instances in the class
- **AutoFinish**: Default behavior creates separate files (consider `autoFinish = false` for large suites)

**Optimization Tips:**
```java
// For performance-critical test suites
@DtrTest(autoFinish = false)  // Single file generation
@DtrContextField
private DtrContext ctx;

@Test
void quickTest1() {
    ctx.say("Quick test 1");
}

@Test
void quickTest2() {
    ctx.say("Quick test 2");
}
```

**Benchmark Results:**
- Field injection vs Parameter injection: < 1% difference
- AutoFinish enabled vs disabled: ~15% faster with disabled
- Memory usage: Negligible increase with field injection

---

## Legacy Pattern Migration (DTR 2026.4.0+)

### Q: Why was field injection introduced?

**Answer:** Field injection addresses several limitations of the inheritance pattern:

1. **Inheritance constraints**: Can't extend DtrTest with other base classes
2. **Method pollution**: Direct `say()` method access pollutes method signatures
3. **Encapsulation issues**: No way to make DtrContext private
4. **Extension conflicts**: Hard to combine with other JUnit extensions
5. **Test isolation**: Less control over context lifecycle

**Migration Path:**
```java
// OLD - Inheritance pattern (still supported)
@DtrTest
class UserServiceTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    public void getUser() {
        say("Retrieves user by ID");  // Direct method access
    }
}

// NEW - Field injection pattern (recommended)
@DtrTest
class UserServiceTest {
    @DtrContextField
    private DtrContext ctx;  // Encapsulated context

    @Test
    public void getUser() {
        ctx.say("Retrieves user by ID");  // Explicit context usage
    }
}
```

**Migration Benefits:**
- ✅ Better encapsulation with private fields
- ✅ Cleaner method signatures
- ✅ No inheritance constraints
- ✅ Full JUnit extension compatibility
- ✅ Explicit dependency management

---

### Q: How do I know which pattern to use?

**Decision Guide:**

**Use Field Injection (Recommended):**
- ✅ Starting new projects
- ✅ Modern Java practices
- ✅ Clean architecture
- ✅ Multiple test methods per class
- ✅ Need for private fields

**Use Parameter Injection:**
- ✅ Tests with varying setup needs
- ✅ Explicit dependency preference
- ✅ Migration from older versions
- ✅ Framework integration

**Use Inheritance (Legacy):**
- ❌ Only for existing legacy code
- ❌ When direct `say()` access is required
- ❌ No migration path available
- ❌ Avoid for new development

**Recommended Transition Plan:**
1. **Phase 1**: Start using `@DtrTest` + `@DtrContextField` in new tests
2. **Phase 2**: Migrate critical existing tests to field injection
3. **Phase 3**: Keep legacy inheritance pattern only for unmigratable code
4. **Phase 4**: Eventually deprecate inheritance in future versions

---

### Q: What about backward compatibility?

**Answer:** Field injection is 100% backward compatible. All existing patterns continue to work:

1. **Inheritance pattern**: Fully supported
2. **Parameter injection**: Fully supported
3. **@ExtendWith pattern**: Fully supported
4. **Mixed patterns**: Can be combined with field injection

**No Breaking Changes:**
- Existing test classes work unchanged
- All existing APIs remain available
- Documentation generation unchanged
- Maven dependencies unchanged

**Migration is Optional:**
- Start using field injection gradually
- No rush to migrate existing code
- Use new patterns for new development
- Keep working patterns untouched

---

### Q: Can I mix old and new patterns?

**Answer:** Yes! DTR 2026.4.1 supports mixed patterns in the same project:

```java
// Old inheritance pattern (still works)
@DtrTest
class LegacyTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    void test() {
        say("Legacy test");
    }
}

// New field injection pattern
@DtrTest
class ModernTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Modern test");
    }
}

// Mixed pattern in same class
@DtrTest
class MixedPatternTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void fieldTest() {
        ctx.say("Field injection test");
    }

    @Test
    void paramTest(DtrContext paramCtx) {
        paramCtx.say("Parameter injection test");
    }
}
```

This gradual migration approach ensures zero disruption while allowing adoption of modern patterns.

---

## Migration from DTR 2.x to 2026.4.1+

### Q: What changed in DTR 2026?

**Key Breaking Changes:**

1. **Package structure changed:**
   ```java
   // Old (2.x)
   import io.github.seanchatmangpt.dtr.core.DtrContext;

   // New (2026.4.1+)
   import io.github.seanchatmangpt.dtr.junit5.DtrContext;
   ```

2. **Field Injection pattern introduced (2026.4.1):**
   ```java
   // Old (2.x - inheritance pattern)
   class MyDocTest extends DtrTest {
       @Test
       void test() {
           say("Hello");
       }
   }

   // Legacy (2026.3.0+ - parameter injection)
   @ExtendWith(DtrExtension.class)
   class MyDocTest {
       @Test
       void test(DtrContext ctx) {
           ctx.say("Hello");
       }
   }

   // NEW (2026.4.1+ - field injection - recommended)
   @DtrTest
   class MyDocTest {
       @DtrContextField
       private DtrContext ctx;

       @Test
       void test() {
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

**Answer:** Removed in v2.6.0. Use JUnit Jupiter 6 assertions for testing, `ctx.sayAssertions()` for documentation.

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
- DTR 2026.4.1+ focuses on documentation generation, not HTTP testing

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

- **🆕 Field Injection Guide:** [Field Injection Tutorial](../../tutorials/field-injection-guide.md)
- **🔍 Comprehensive Troubleshooting:** [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)
- **📖 API Reference:** [say* Core API Reference](request-api.md)
- **🏷️ Annotation Reference:** [Annotation Reference](annotation-reference.md)
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
git tag v2026.3.0
git tag v2026.3.0

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

# 2. Verify JUnit Jupiter 6
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
**DTR Version:** 2026.4.1+
**For comprehensive troubleshooting, see [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)**

---

## Field Injection Troubleshooting (NEW)

### Q: Field injection not working - common diagnostic steps

**Quick Answer:** Check annotations, imports, and field configuration.

```bash
# 1. Verify @DtrTest is present
grep "@DtrTest" src/test/java/MyTest.java
# Must find: @DtrTest

# 2. Check @DtrContextField annotation
grep "@DtrContextField" src/test/java/MyTest.java
# Must find: @DtrContextField

# 3. Verify DtrContext field declaration
grep "private DtrContext ctx" src/test/java/MyTest.java
# Must find: private DtrContext ctx

# 4. Check for compilation errors
mvnd compile
# Should show no field injection related errors

# 5. Run specific test with output
mvnd test -Dtest=MyTest -v
# Look for: "Injecting DtrContext to field"
```

**Common Solutions:**

1. **Missing @DtrTest annotation:**
   ```java
   // WRONG
   public class MyTest {
       @DtrContextField
       private DtrContext ctx;
   }

   // CORRECT
   @DtrTest
   public class MyTest {
       @DtrContextField
       private DtrContext ctx;
   }
   ```

2. **Static field issue:**
   ```java
   // WRONG
   @DtrContextField
   private static DtrContext ctx;  // Not supported

   // CORRECT
   @DtrContextField
   private DtrContext ctx;  // Instance field
   ```

3. **Wrong field type:**
   ```java
   // WRONG
   @DtrContextField
   private Object ctx;  // Must be DtrContext

   // CORRECT
   @DtrContextField
   private DtrContext ctx;
   ```

**🔍 Detailed:** [Field Injection Issues](../TROUBLESHOOTING.md#field-injection-issues)

---

### Q: @DtrTest not working - diagnosis steps

**Quick Answer:** Verify composite annotation is properly configured.

**Diagnostic Steps:**

```bash
# 1. Check DTR version in pom.xml
grep "dtr-core" pom.xml
# Should show: <version>2026.4.1</version>

# 2. Verify imports
grep "import.*DtrTest" src/test/java/MyTest.java
# Should be: import io.github.seanchatmangpt.dtr.junit5.DtrTest

# 3. Test compilation
mvnd clean compile
# Should show no @DtrTest related errors

# 4. Run test with extension debug
mvnd test -Dtest=MyTest -Ddtr.debug=true
# Look for extension loading logs
```

**Common Issues:**

1. **Wrong DTR version:**
   ```xml
   <!-- WRONG - old version -->
   <version>2026.4.1</version>

   <!-- CORRECT - 2026.4.1+ -->
   <version>2026.4.1</version>
   ```

2. **Missing imports:**
   ```java
   // WRONG
   import org.junit.jupiter.api.Test;

   // CORRECT
   import io.github.seanchatmangpt.dtr.junit5.DtrTest;
   import io.github.seanchatmangpt.dtr.junit5.DtrContextField;
   import org.junit.jupiter.api.Test;
   ```

3. **Annotation on wrong element:**
   ```java
   // WRONG - on method
   @DtrTest
   public void test() { ... }

   // CORRECT - on class
   @DtrTest
   public class MyTest {
       @Test
       public void test() { ... }
   }
   ```

**🔍 Detailed:** [@DtrTest Issues](../TROUBLESHOOTING.md#dtrotest-issues)

---

## Legacy Pattern Reference

### Inheritance Pattern (Legacy - Use Field Injection Instead)

**Usage:**
```java
@DtrTest
class MyLegacyTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    void test() {
        say("Legacy pattern test");
    }
}
```

**When to Use (Rare Cases Only):**
- ✅ Maintaining existing legacy code
- ✅ When direct `say()` method access is required
- ✅ No migration path available

**Migration to Field Injection:**
```java
// Before (Legacy)
@DtrTest
class MyLegacyTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    void test() {
        say("Legacy test");
    }
}

// After (Field Injection - Recommended)
@DtrTest
class MyModernTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Modern test");
    }
}
```

**Migration Benefits:**
- ✅ Better encapsulation with private fields
- ✅ No inheritance constraints
- ✅ Clean method signatures
- ✅ Full JUnit extension compatibility
- ✅ Explicit dependency management

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

After (v2.6.0 with field injection):
```java
@DtrTest
class ApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void testUsers() {
        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/users"))
            .GET().build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        ctx.sayCode("GET /api/users", "http");
        ctx.sayAssertions(Map.of("Status is 200", response.statusCode() == 200));
        ctx.sayJson(response.body());
    }
}
```

---

### Q: `sayAndAssertThat` is not found — where did it go?

`sayAndAssertThat(String, T, Matcher)` was **removed in v2.6.0**.

**Replacement:** Use JUnit Jupiter 6 assertions for the assertion, and `ctx.sayAssertions(Map)` to document results:

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
Ensure you are using JUnit Jupiter 6 and the correct imports:

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

# DTR Troubleshooting Guide

**Symptom-based problem solving** — Find your issue by what you SEE, not the technical cause.

**Version:** 2026.1.0+ | **Last Updated:** 2026-03-14

---

## Quick Diagnosis Checklist

**Before diving into specific issues, verify these basics:**

```bash
# 1. Check Java version (must be 26+)
java -version
# Expected: openjdk version "26.x.x" or similar

# 2. Check Maven version (must be 4.0.0-rc-5+)
mvnd --version
# Expected: mvnd 2.x.x / Maven 4.0.0-rc-5+

# 3. Verify --enable-preview is configured
cat .mvn/maven.config
# Must contain: --enable-preview

# 4. Check DTR dependency scope
grep -A 5 "dtr-core" pom.xml
# Must contain: <scope>test</scope>

# 5. Verify test class has @ExtendWith annotation
grep "@ExtendWith" src/test/java/**/*Test.java
# Must contain: @ExtendWith(DtrExtension.class)
```

**If any of these fail, jump to the corresponding section below.**

---

## Table of Symptoms

| Symptom | Section |
|---------|---------|
| "preview features not enabled" | [Setup Issues](#setup-issues---preview-features) |
| "ClassNotFoundException: DtrExtension" | [Setup Issues](#setup-issues---missing-dependency) |
| "Unsupported class version" | [Setup Issues](#setup-issues---java-version) |
| Tests pass but no docs generated | [Output Problems](#output-problems---no-documentation-generated) |
| Documentation files are empty | [Output Problems](#output-problems---empty-documentation) |
| Documentation in wrong location | [Output Problems](#output-problems---wrong-location) |
| "DtrContext cannot be resolved" | [Runtime Failures](#runtime-failures---extension-not-loading) |
| Test methods don't run | [Runtime Failures](#runtime-failures---tests-not-executed) |
| Build is slow | [Performance Issues](#performance-issues---slow-builds) |
| Out of memory errors | [Performance Issues](#performance-issues---out-of-memory) |
| Migration errors from 2.5.x | [Migration Issues](#migration-issues---v26-breaking-changes) |

---

## Setup Issues

### Preview Features Not Enabled

**Symptom:**
```
error: preview features are not enabled
  (use --enable-preview to enable preview features)
```

**Cause:** Java 26 preview features (like JEP 516 Code Reflection) require explicit enablement.

**Fix:**

1. **Add to `.mvn/maven.config`:**
   ```
   --enable-preview
   ```

2. **Add to `pom.xml` compiler plugin:**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <release>26</release>
           <compilerArgs>
               <arg>--enable-preview</arg>
           </compilerArgs>
       </configuration>
   </plugin>
   ```

3. **Add to `pom.xml` surefire plugin:**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <configuration>
           <argLine>--enable-preview</argLine>
       </configuration>
   </plugin>
   ```

**Verification:**
```bash
# Should compile and run without preview errors
mvnd clean test -Dtest=YourDocTest
```

---

### Missing Dependency

**Symptom:**
```
java.lang.ClassNotFoundException: io.github.seanchatmangpt.dtr.junit5.DtrExtension
```

**Cause:** DTR dependency not in test scope or not downloaded.

**Fix:**

1. **Verify `pom.xml` has the dependency:**
   ```xml
   <dependency>
       <groupId>io.github.seanchatmangpt.dtr</groupId>
       <artifactId>dtr-core</artifactId>
       <version>2026.1.0</version>
       <scope>test</scope>  <!-- CRITICAL: must be test scope -->
   </dependency>
   ```

2. **Force update from Maven Central:**
   ```bash
   mvnd clean install -U
   ```

**Verification:**
```bash
# Should find the dependency
mvnd dependency:tree -Dinclude=io.github.seanchatmangpt.dtr:dtr-core
```

---

### Java Version Mismatch

**Symptom:**
```
Unsupported class version 65.0 (Java 26 required)
```

**Cause:** Using Java 25 or earlier with DTR 2026.1.0+.

**Fix:**

1. **Install Java 26:**
   ```bash
   # macOS (Homebrew)
   brew install openjdk@26
   export JAVA_HOME=/opt/homebrew/opt/openjdk@26  # Apple Silicon
   # export JAVA_HOME=/usr/local/opt/openjdk@26  # Intel

   # Linux
   sudo apt-get install openjdk-26-jdk
   export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

   # SDKMAN
   sdk install java 26-open
   sdk use java 26-open
   ```

2. **Verify in Maven:**
   ```bash
   java -version  # Should show 26
   echo $JAVA_HOME
   ```

3. **Configure mvnd to use Java 26:**
   Create or update `~/.m2/mvnd.properties`:
   ```properties
   mvnd.javaHome=/opt/homebrew/opt/openjdk@26  # macOS
   # mvnd.javaHome=/usr/lib/jvm/java-26-openjdk-amd64  # Linux
   ```

**Verification:**
```bash
mvnd --version
# Should show: mvnd 2.x.x / Maven 4.0.0-rc-5 / Java 26
```

---

## Build Errors

### Compilation Failures

**Symptom:**
```
error: package io.github.seanchatmangpt.dtr.junit5 does not exist
```

**Cause:** Missing import or wrong package name (pre-2026 API).

**Fix:**

**For DTR 2026.1.0+ (current):**
```java
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Hello, DTR!");
    }
}
```

**If migrating from old DTR 2.x:**
- Old: `import io.github.seanchatmangpt.dtr.core.DtrContext;`
- New: `import io.github.seanchatmangpt.dtr.junit5.DtrContext;`
- Old: `import io.github.seanchatmangpt.dtr.core.DtrExtension;`
- New: `import io.github.seanchatmangpt.dtr.junit5.DtrExtension;`

**Verification:**
```bash
mvnd clean compile
# Should compile without errors
```

---

### Dependency Conflicts

**Symptom:**
```
Failed to execute goal on project ...: Could not resolve dependencies
```

**Cause:** Maven Central rate limiting or network issues.

**Fix:**

1. **Check Maven Central access:**
   ```bash
   curl -I https://repo1.maven.org/maven2/
   # Should return HTTP 200
   ```

2. **Use Maven proxy helper (enterprise networks):**
   ```bash
   python3 maven-proxy-auth.py &
   mvnd clean install \
     -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
     -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
   ```

**Verification:**
```bash
mvnd dependency:resolve
# Should resolve all dependencies successfully
```

---

## Runtime Failures

### Extension Not Loading

**Symptom:**
```
Parameter resolution failed for [DtrContext ctx]
No ParameterResolver registered for parameter [io.github.seanchatmangpt.dtr.junit5.DtrContext]
```

**Cause:** `@ExtendWith(DtrExtension.class)` missing or wrong extension class.

**Fix:**

1. **Verify annotation:**
   ```java
   import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
   import org.junit.jupiter.api.extension.ExtendWith;

   @ExtendWith(DtrExtension.class)  // MUST be present
   class MyDocTest {
       @Test
       void test(DtrContext ctx) {  // ctx will be injected
           ctx.say("This works!");
       }
   }
   ```

2. **Check import statement:**
   - Wrong: `import io.github.seanchatmangpt.dtr.core.DtrExtension;`
   - Right: `import io.github.seanchatmangpt.dtr.junit5.DtrExtension;`

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
# Should run test and inject DtrContext successfully
```

---

### Tests Not Executed

**Symptom:**
```
[INFO] No tests were executed
```

**Cause:** Test class doesn't match Maven Surefire's default patterns.

**Fix:**

1. **Verify test class name:**
   - Must match: `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`
   - Example: `UserApiTest.java` ✓, `UserApi.java` ✗

2. **Or specify test explicitly:**
   ```bash
   mvnd test -Dtest=MyDocTest
   ```

3. **Check for wrong JUnit imports:**
   - Wrong: `import org.junit.Test;` (JUnit 4)
   - Right: `import org.junit.jupiter.api.Test;` (JUnit 5)

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
# Should show: Tests run: 1, Failures: 0, Errors: 0
```

---

### AfterAll Hook Not Called

**Symptom:** Test passes but no documentation file generated.

**Cause:** `@AfterAll` hook not triggered (static context issue or JUnit lifecycle problem).

**Fix:**

1. **Use `DtrExtension` instead of `DtrTest` base class:**
   ```java
   // OLD (may have lifecycle issues):
   class MyDocTest extends DtrTest {
       @Test
       void test() {
           say("This may not generate docs if @AfterAll fails");
       }
   }

   // NEW (recommended):
   @ExtendWith(DtrExtension.class)
   class MyDocTest {
       @Test
       void test(DtrContext ctx) {
           ctx.say("This always generates docs");
       }
   }
   ```

2. **Verify `DtrExtension` is registered:**
   ```bash
   grep -r "DtrExtension" src/test/java/
   # Should find: @ExtendWith(DtrExtension.class)
   ```

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
ls -la target/docs/test-results/
# Should show: MyDocTest.md
```

---

## Output Problems

### No Documentation Generated

**Symptom:** Tests pass but `target/docs/test-results/` is empty or missing.

**Diagnosis Steps:**

1. **Check if any `say*` methods were called:**
   ```bash
   grep -n "ctx\\.say" src/test/java/MyDocTest.java
   # Must find at least one: ctx.say(...), ctx.sayCode(...), etc.
   ```

2. **Verify test ran successfully:**
   ```bash
   mvnd test -Dtest=MyDocTest
   # Look for: Tests run: 1, Failures: 0, Errors: 0
   ```

3. **Check output directory:**
   ```bash
   ls -la target/docs/test-results/
   # If directory missing: DtrExtension not loaded
   # If directory exists but empty: No say* methods called
   ```

**Fixes:**

**If extension not loaded:**
```java
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)  // ADD THIS
class MyDocTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Now docs will be generated");
    }
}
```

**If no say* methods called:**
```java
@Test
void test(DtrContext ctx) {
    // ADD DOCUMENTATION
    ctx.say("This test verifies user authentication");
    ctx.sayCode("POST /api/login", "http");

    // Your test logic here
    assertTrue(authenticate("user", "pass"));
}
```

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
cat target/docs/test-results/MyDocTest.md
# Should show generated documentation
```

---

### Empty Documentation Files

**Symptom:** Documentation file exists but contains only metadata (no content).

**Cause:** Test calls no `say*` methods, or `say*` calls are conditional and not executed.

**Fix:**

1. **Ensure unconditional `say*` calls:**
   ```java
   // WRONG (conditional docs):
   @Test
   void test(DtrContext ctx) {
       if (someCondition) {
           ctx.say("This only runs if condition is true");
       }
       // Result: Empty docs if condition is false
   }

   // RIGHT (unconditional docs):
   @Test
   void test(DtrContext ctx) {
       ctx.say("Test verifies authentication logic");
       // Test logic
       if (someCondition) {
           ctx.sayNote("Condition was true");
       } else {
           ctx.sayNote("Condition was false");
       }
       // Result: Always has content
   }
   ```

2. **Use `@DocSection` and `@DocDescription` annotations:**
   ```java
   @Test
   @DocSection("User Authentication")
   @DocDescription({"Verifies user login with valid credentials"})
   void test(DtrContext ctx) {
       // These annotations generate content even if no say* calls
       assertTrue(authenticate("user", "pass"));
   }
   ```

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
cat target/docs/test-results/MyDocTest.md
# Should show: ## User Authentication, description paragraph
```

---

### Wrong Output Location

**Symptom:** Documentation generated, but not where expected.

**Cause:** Custom output directory configured or test class name doesn't match file name.

**Diagnosis:**
```bash
# Find actual output location
find target -name "*.md" -type f
# Check if custom output directory configured
grep -r "output.directory" .mvn/ pom.xml
```

**Fixes:**

**Default location (recommended):**
```bash
# Docs should be here:
target/docs/test-results/ClassName.md
```

**Custom output directory:**
```java
// In test setup or @BeforeEach
@BeforeEach
void setup(DtrContext ctx) {
    // Custom output location (not recommended unless necessary)
    System.setProperty("dtr.output.dir", "target/custom-docs");
}
```

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
find target -name "MyDocTest.md"
# Should show the file location
```

---

### Formatting Issues

**Symptom:** Markdown syntax broken, tables malformed, or code blocks not rendering.

**Cause:** Special characters not escaped or incorrect `say*` method usage.

**Common Issues and Fixes:**

**Broken tables:**
```java
// WRONG (missing headers):
ctx.sayTable(new String[][]{
    {"Alice", "admin"},  // Row without header
    {"Bob", "user"}
});

// RIGHT (first row = headers):
ctx.sayTable(new String[][]{
    {"Name", "Role"},     // Header row
    {"Alice", "admin"},   // Data row
    {"Bob", "user"}
});
```

**Malformed code blocks:**
```java
// WRONG (missing language):
ctx.sayCode("int x = 42;");  // No syntax highlighting

// RIGHT (with language):
ctx.sayCode("int x = 42;", "java");  // Java syntax highlighting
```

**Special characters breaking Markdown:**
```java
// If text contains underscores, asterisks, or other Markdown syntax:
ctx.sayRaw("Use \\_escaped\\_ underscores");  // sayRaw for literal content
ctx.say("Use _escaped_ underscores");  // say auto-escapes
```

**Verification:**
```bash
mvnd test -Dtest=MyDocTest
cat target/docs/test-results/MyDocTest.md
# View in GitHub Markdown renderer to verify formatting
```

---

## Performance Issues

### Slow Builds

**Symptom:** `mvnd test` takes 30+ seconds, even for small test suites.

**Causes and Fixes:**

**Cause 1: Not using mvnd daemon**
```bash
# WRONG (slow):
mvn clean test

# RIGHT (fast - reuses daemon):
mvnd clean test
```

**Cause 2: Building all modules**
```bash
# WRONG (builds everything):
mvnd clean install

# RIGHT (build only what changed):
mvnd clean install -pl dtr-core -am
```

**Cause 3: Not using parallel builds**
```bash
# Enable parallel module builds:
mvnd -T 1C clean test  # 1 thread per CPU core
```

**Cause 4: Maven daemon not warmed**
```bash
# First build is slow (daemon startup + dependency download)
# Subsequent builds are fast
mvnd clean test  # Run twice to see speed improvement
```

**Verification:**
```bash
time mvnd clean test
# Second run should be < 10 seconds for typical project
```

---

### Out of Memory

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause:** Large test suite generating extensive documentation.

**Fixes:**

1. **Increase Maven heap size:**
   ```bash
   export MAVEN_OPTS="-Xmx2g"
   mvnd clean test
   ```

2. **Configure mvnd heap in `~/.m2/mvnd.properties`:**
   ```properties
   mvnd.minHeapSize=512m
   mvnd.maxHeapSize=2g
   ```

3. **Increase Surefire heap in `pom.xml`:**
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <configuration>
           <argLine>-Xmx1g --enable-preview</argLine>
       </configuration>
   </plugin>
   ```

**Verification:**
```bash
mvnd clean test
# Should complete without OutOfMemoryError
```

---

### Maven Daemon Unresponsive

**Symptom:** `mvnd` hangs or becomes slow over time.

**Fix:**
```bash
# Stop all daemon processes
mvnd --stop

# Restart daemon
mvnd clean test
```

**Verification:**
```bash
ps aux | grep mvnd
# Should show fresh daemon process
```

---

## Migration Issues

### v2026 Breaking Changes

**Symptom:** Code that worked with DTR 2.x no longer compiles or runs.

**Key Changes in DTR 2026.1.0+:**

#### 1. Package Structure Changed

**Old (2.x):**
```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
```

**New (2026.1.0+):**
```java
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
```

#### 2. Extension Registration

**Old (2.x):**
```java
class MyDocTest extends DtrTest {
    @Test
    void test() {
        say("Hello");
    }
}
```

**New (2026.1.0+ - recommended):**
```java
@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Hello");
    }
}
```

#### 3. Test Base Class

**Old (2.x):** `DtrTest` base class with `say*` methods

**New (2026.1.0+):** `DtrTest` still exists, but `DtrExtension` + `DtrContext` is preferred

**Migration Path:**
```bash
# 1. Update dependency version
sed -i 's/<version>2\.[0-9]\.[0-9]<\/version>/<version>2026.1.0<\/version>/' pom.xml

# 2. Update imports
find src/test/java -name "*.java" -exec sed -i 's/io\.github\.seanchatmangpt\.dtr\.core\./io.github.seanchatmangpt.dtr.junit5./g' {} \;

# 3. Rebuild
mvnd clean compile
```

**Verification:**
```bash
mvnd clean test
# Should compile and run with new API
```

---

## Error Message Index

### A

**`AnnotationFormatError: @ExtendWith requires a Class value`**
- Cause: Wrong annotation syntax
- Fix: Use `@ExtendWith(DtrExtension.class)` not `@ExtendWith("DtrExtension")`

**`IllegalArgumentException: Invalid character in markdown`**
- Cause: Unescaped special characters in `sayRaw()`
- Fix: Escape underscores, asterisks, or use `say()` instead

### C

**`ClassNotFoundException: io.github.seanchatmangpt.dtr.junit5.DtrExtension`**
- See: [Missing Dependency](#missing-dependency)

**`ClassCastException: DtrContext cannot be cast to...`**
- Cause: Using old API with new version
- Fix: Update imports to `io.github.seanchatmangpt.dtr.junit5.*`

### E

**`error: preview features are not enabled`**
- See: [Preview Features Not Enabled](#preview-features-not-enabled)

### I

**`IllegalAccessError: class ... cannot access class ... in module jdk.compiler`**
- Cause: Code Reflection methods without `--enable-preview`
- Fix: Add `--enable-preview` to `.mvn/maven.config` and surefire config

**`InvalidParameterTypeResolverException`**
- Cause: `DtrContext` parameter without `@ExtendWith(DtrExtension.class)`
- Fix: Add `@ExtendWith(DtrExtension.class)` to test class

### N

**`NoParameterResolverException`**
- See: [Extension Not Loading](#extension-not-loading)

### O

**`OutOfMemoryError: Java heap space`**
- See: [Out of Memory](#out-of-memory)

### P

**`Preview feature disabled`**
- See: [Preview Features Not Enabled](#preview-features-not-enabled)

### U

**`Unsupported class version 65.0`**
- See: [Java Version Mismatch](#java-version-mismatch)

**`UnsupportedOperationException: Reflection-based method`**
- Cause: Calling `sayCodeModel()` or similar without Java 26
- Fix: Upgrade to Java 26 and enable preview features

---

## Getting Additional Help

If you don't find your issue above:

1. **Enable debug output:**
   ```bash
   mvnd test -X
   ```

2. **Check the logs:**
   ```bash
   cat target/surefire-reports/YourTest.txt
   ```

3. **Verify your setup:**
   ```bash
   java -version
   mvnd --version
   cat .mvn/maven.config
   ```

4. **Search existing issues:**
   - [GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)
   - [FAQ](https://github.com/seanchatmangpt/dtr/docs/reference/FAQ_AND_TROUBLESHOOTING.md)
   - [Known Issues](https://github.com/seanchatmangpt/dtr/docs/reference/KNOWN_ISSUES.md)

5. **Report a new issue:**
   - Include: Java version, Maven version, DTR version
   - Include: Minimal test case that reproduces the issue
   - Include: Full error output and stack trace

---

## Quick Reference Cards

### Diagnosing "No Docs Generated"

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

### Diagnosing "Compilation Errors"

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

### Diagnosing "Runtime Failures"

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

**Last Updated:** 2026-03-14
**DTR Version:** 2026.1.0+
**For version history and migration guides, see [CHANGELOG.md](../CHANGELOG.md)**

# How-To: Performance Tuning DTR Tests

Optimize your DTR tests and documentation generation for faster builds and better performance.

**Goal:** Reduce documentation generation overhead and build time without sacrificing documentation quality.

---

## Overview

DTR adds measurable overhead to tests (~10-15% typical). This guide shows how to minimize it.

**Key Areas:**
1. Documentation generation strategies
2. Maven build optimization
3. Virtual thread tuning
4. Memory management

---

## 1. Reduce Documentation Verbosity

### Profile Your Tests

Identify which `say*()` calls are expensive:

```java
@Test
public void profileDocumentation() {
    long start = System.nanoTime();
    ctx.sayNextSection("Profile Test");
    say("Section: " + (System.nanoTime() - start) + " ns");

    start = System.nanoTime();
    ctx.say("This is a paragraph.");
    say("Paragraph: " + (System.nanoTime() - start) + " ns");

    start = System.nanoTime();
    ctx.sayCode("code", "java");
    say("Code block: " + (System.nanoTime() - start) + " ns");

    // Most expensive operations
    start = System.nanoTime();
    ctx.sayJson(largeObject);
    say("JSON serialization: " + (System.nanoTime() - start) + " ns");
}
```

### Skip Expensive Operations

For rapid iteration, skip expensive documentation:

```java
@Test
public void fastTest() {
    ctx.sayNextSection("API Test");
    // Skip expensive documentation
    // ctx.sayJson(largeResponse);  // COMMENTED OUT

    Response response = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users"))
    );

    // Only assert, skip detailed documentation
    assertEquals(200, response.httpStatus());
}
```

### Use Conditional Documentation

Only generate detailed docs in CI/release builds:

```java
@Test
public void conditionalDocumentation() {
    boolean isCI = "true".equals(System.getenv("CI_BUILD"));

    ctx.sayNextSection("User API");

    if (isCI) {
        ctx.say("Detailed explanation...");
        ctx.sayJson(examplePayload);
    }

    Response response = ctx.sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users"))
    );

    assertEquals(200, response.httpStatus());
}
```

---

## 2. Optimize Maven Build

### Use mvnd (Maven Daemon)

**Impact:** 2-3x faster builds

```bash
# Much faster
mvnd clean install

# Slower (creates new JVM each time)
mvn clean install
```

### Build Only Changed Modules

```bash
# Build specific module
mvnd clean install -pl dtr-core

# Build module and dependents
mvnd clean install -amd -pl dtr-core
```

### Use Parallel Builds

```bash
# 1 thread per core
mvnd clean install -T 1C

# 2 threads per core
mvnd clean install -T 2C
```

### Skip Tests During Development

```bash
# Skip tests, build only
mvnd clean install -DskipTests

# Skip tests AND docs
mvnd clean install -DskipTests -DskipDocs

# Build normally, then just compile
mvnd clean compile
```

### Skip Documentation Generation

```bash
# Skip markdown/HTML/LaTeX generation (faster)
mvnd clean test -Ddtr.skipOutput=true
```

---

## 3. Memory Optimization

### Increase Maven Heap

Large test suites need more memory:

```bash
# Set to 2GB
export MAVEN_OPTS="-Xmx2g"
mvnd clean install

# Set to 4GB (for very large suites)
export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC"
mvnd clean install
```

### Persistent Configuration

Add to `~/.m2/jvm.config`:

```
-Xmx2g
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled
-XX:-DisableExplicitGC
```

### Monitor Memory

```bash
# List running daemons (see memory usage)
mvnd --list

# If daemon is using too much memory, restart
mvnd --stop
mvnd clean install
```

---

## 4. Virtual Thread Tuning

### Use Virtual Threads for High Concurrency

Document concurrent API calls efficiently:

```java
@Test
public void concurrentApiTests() {
    int requestCount = 1000;

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            futures.add(executor.submit(() -> {
                Response response = sayAndMakeRequest(
                    Request.GET().url(testServerUrl().path("/api/item/" + i))
                );
                assertEquals(200, response.httpStatus());
            }));
        }

        // Wait for all to complete
        for (var future : futures) {
            future.get();
        }
    }

    say("Completed " + requestCount + " requests");
}
```

**Benefits:**
- Can create thousands of virtual threads
- Minimal memory overhead
- Better scaling than platform threads

### Configure Virtual Thread Stack Size

For lightweight tasks, reduce stack:

```bash
mvnd test -Djdk.virtualThreadScheduler.parallelism=16
```

---

## 5. Caching & Reuse

### Reuse HTTP Connections

DTR reuses connections by default. Verify in your tests:

```java
@Test
public void reuseConnections() {
    // Same URL = same HTTP connection
    Response r1 = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));
    Response r2 = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));

    // Both use same connection pool
    assertEquals(200, r1.httpStatus());
    assertEquals(200, r2.httpStatus());
}
```

### Reuse Test Fixtures

Create test data once, reuse across tests:

```java
@BeforeClass
public static void setupFixtures() {
    // Expensive setup happens once
    testDataServer.start();
    testDataServer.createTestUsers(1000);
}

@Test
public void test1() {
    // Reuse existing test data
    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users"))
    );
}
```

---

## 6. Smart Test Organization

### Split Large Test Classes

Instead of:
```java
class ApiTest {  // 100 test methods
    @Test
    void test1() { ... }
    @Test
    void test2() { ... }
    // ... 98 more tests
}
```

Do:
```java
class UserApiTest {  // 25 test methods
    @Test
    void test1() { ... }
}

class ProductApiTest {  // 25 test methods
    @Test
    void test1() { ... }
}
```

**Benefits:**
- Parallel execution per class
- Faster to run subset
- Easier to manage

### Use Parameterized Tests

Instead of:
```java
@Test
void testUser1() { ... }
@Test
void testUser2() { ... }
@Test
void testUser3() { ... }
```

Do:
```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3})
void testUser(int userId) {
    // Test reused for all values
}
```

---

## 7. Benchmark-Driven Optimization

### Identify Bottlenecks

```java
@Test
public void findBottlenecks() {
    long[] times = new long[5];

    times[0] = measure(() -> ctx.sayNextSection("Test"));
    times[1] = measure(() -> ctx.say("Text"));
    times[2] = measure(() -> ctx.sayCode("code", "java"));
    times[3] = measure(() -> ctx.sayJson(largeObject));
    times[4] = measure(() -> ctx.sayTable(data));

    for (int i = 0; i < times.length; i++) {
        if (times[i] > 1_000_000) {  // > 1ms is notable
            say("Slow operation " + i + ": " + times[i] + " ns");
        }
    }
}

private long measure(Runnable r) {
    long start = System.nanoTime();
    r.run();
    return System.nanoTime() - start;
}
```

### Optimize Identified Operations

If `sayJson()` is slow on large objects, consider:
- Skip JSON for large payloads
- Only document smaller subsets
- Use simplified JSON representation

---

## 8. CI/CD Optimization

### Parallel Test Execution

In Maven's `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <parallel>methods</parallel>
                <threadCount>4</threadCount>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Skip Documentation in Parallel Builds

When running tests in parallel, you may want to disable documentation:

```bash
# Development: skip docs, build fast
mvnd test -Ddtr.skipOutput=true -T 1C

# Release: full docs, single-threaded
mvnd test -T 1
```

### Cache Maven Dependencies

In CI/CD pipeline, cache `.m2` directory:

```yaml
# GitHub Actions example
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: maven-${{ hashFiles('**/pom.xml') }}
```

---

## 9. Profiling Checklist

Use this checklist to identify optimization opportunities:

- [ ] Measure baseline test time (without any optimizations)
- [ ] Profile with `-X` flag to see detailed execution
- [ ] Identify slow `say*()` calls
- [ ] Check JVM heap usage with `jps -l` and `jstat`
- [ ] Measure impact of each optimization
- [ ] Document performance metrics

---

## Performance Targets

Aim for these benchmarks:

| Scenario | Target | Notes |
|----------|--------|-------|
| Simple GET request | <100 µs total | Network dominates |
| Document small payload | <200 µs | Markdown generation |
| Document large payload | <2 ms | JSON serialization |
| Build 100 tests | <30 seconds | With mvnd, parallel |
| Full test suite | <2 minutes | Typical medium project |

---

## When to NOT Optimize

- **First build**: Get something working before optimizing
- **One-off tests**: Not worth optimizing
- **CI/CD release builds**: Prioritize completeness over speed
- **Complex logic tests**: Focus on correctness first

---

## Common Mistakes

### ❌ Don't: Skip documentation everywhere
```java
// BAD: No docs generated
mvnd test -DskipTests -DskipDocs
```

### ✅ Do: Optimize selectively
```java
// GOOD: Fast for dev, docs for release
if (isDevMode) {
    // Skip expensive docs
} else {
    // Generate full documentation
}
```

### ❌ Don't: Increase heap infinitely
```bash
# BAD: Wastes memory
export MAVEN_OPTS="-Xmx16g"
```

### ✅ Do: Find right heap size
```bash
# GOOD: Balance memory and speed
export MAVEN_OPTS="-Xmx2g"
```

---

## Summary

**Quick Wins:**
1. Use `mvnd` instead of `mvn` (2-3x faster)
2. Build only changed modules (`-pl`, `-amd`)
3. Set `MAVEN_OPTS="-Xmx2g"` for large suites
4. Skip docs in development builds
5. Use virtual threads for concurrent tests

**For Large Projects:**
1. Parallel builds (`-T 1C`)
2. Split test classes
3. Use parameterized tests
4. Cache Maven dependencies
5. Profile to find bottlenecks

See Also:
- [Benchmarking](benchmarking.md) — Measure performance
- [Known Issues - Performance](../reference/KNOWN_ISSUES.md#performance-characteristics) — Expected overhead
- [Virtual Threads Guide](use-virtual-threads.md) — Advanced concurrency

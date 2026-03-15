# How-To: Performance Tuning DTR Tests

Quick recipes to optimize your DTR tests and documentation generation for faster builds.

**DTR Version:** 2026.2.0 | **Java:** 26+ with `--enable-preview`

---

## Overview

This guide provides practical, task-focused optimization recipes. For comprehensive performance analysis, architecture details, and real-world benchmarks, see [Performance Guide](../PERFORMANCE.md).

**Quick Recipes:**
1. Profile with `sayBenchmark`
2. Leverage reflection caching
3. Optimize Maven builds
4. Tune virtual threads
5. Manage memory effectively

---

## 1. Profile with sayBenchmark

### Identify Slow say* Calls

```java
@ExtendWith(DtrExtension.class)
class ProfileDocTest {

    @Test
    void profileSayMethods(DtrContext ctx) {
        ctx.sayNextSection("say* Method Overhead Profile");
        ctx.sayEnvProfile();

        ctx.sayBenchmark("sayNextSection()", () -> {
            ctx.sayNextSection("Test Section");
        });

        ctx.sayBenchmark("say() — short paragraph", () -> {
            ctx.say("This is a test paragraph.");
        });

        ctx.sayBenchmark("sayCode() — 10 lines", () -> {
            ctx.sayCode("var x = 1;\nvar y = 2;\nvar z = x + y;", "java");
        });

        ctx.sayBenchmark("sayJson() — Map with 5 entries", () -> {
            ctx.sayJson(java.util.Map.of(
                "a", 1, "b", 2, "c", 3, "d", 4, "e", 5));
        });

        ctx.sayBenchmark("sayRecordComponents() — 4-field record", () -> {
            record Sample(int a, String b, boolean c, double d) {}
            ctx.sayRecordComponents(Sample.class);
        });
    }
}
```

---

## 2. Reflection Caching

DTR automatically caches reflection results for `sayClassDiagram`, `sayRecordComponents`, `sayContractVerification`, `sayCallGraph`, and `sayDocCoverage`. First call incurs reflection overhead; subsequent calls on the same class are fast:

```java
@Test
void demonstrateReflectionCache(DtrContext ctx) {
    ctx.sayNextSection("Reflection Caching Demo");
    ctx.sayEnvProfile();

    record Cached(int a, String b, double c) {}

    ctx.sayBenchmark("sayRecordComponents() — first call (uncached)", () -> {
        ctx.sayRecordComponents(Cached.class);
    }, 1, 1);

    ctx.sayBenchmark("sayRecordComponents() — second call (cached)", () -> {
        ctx.sayRecordComponents(Cached.class);
    }, 1, 100);

    ctx.sayNote("Cache hits are typically 1000x+ faster than the initial reflection call.");
}
```

---

## 3. Conditional Documentation

Skip expensive documentation in development builds:

```java
@Test
void conditionalDocumentation(DtrContext ctx) {
    boolean isCI = System.getenv("CI") != null;

    ctx.sayNextSection("User API");

    if (isCI) {
        // Expensive documentation only in CI
        ctx.say("Detailed explanation for release documentation...");
        ctx.sayDocCoverage(UserService.class);
        ctx.sayCallGraph(UserService.class);
        ctx.sayEvolutionTimeline(UserService.class, 10);
    }

    // Core test logic always runs
    ctx.sayEnvProfile();
}
```

---

## 4. Optimize Maven Build

### Use mvnd (Maven Daemon)

```bash
# Much faster (daemon reuses JVM)
mvnd clean test

# Slower (new JVM each time)
mvn clean test
```

### Build Only Changed Modules

```bash
# Build specific module
mvnd clean install -pl dtr-integration-test

# Build module and its dependencies
mvnd clean install -amd -pl dtr-core
```

### Use Parallel Builds

```bash
# 1 thread per core
mvnd clean install -T 1C

# 2 threads per core
mvnd clean install -T 2C
```

### Skip Documentation in Development

```bash
# Skip documentation output (faster iteration)
mvnd clean test -Ddtr.skipOutput=true

# Skip tests, just compile
mvnd clean compile
```

---

## 5. Memory Optimization

### Increase Maven Heap

```bash
# Set to 2GB
export MAVEN_OPTS="-Xmx2g"
mvnd clean install
```

### Persistent Configuration

Add to `~/.m2/jvm.config`:

```
-Xmx2g
-XX:+UseG1GC
```

### Monitor Memory

```bash
# List running daemons
mvnd --list

# Restart daemon on memory issues
mvnd --stop
mvnd clean install
```

> **Note:** For detailed memory performance characteristics and heap sizing guidelines, see [Performance Guide: Memory Footprint](../PERFORMANCE.md#key-performance-characteristics).

---

## 6. Virtual Thread Tuning

### Tune the Virtual Thread Scheduler

```bash
mvnd test -Djdk.virtualThreadScheduler.parallelism=16
```

### Parallelize Documentation Tests

DTR's `MultiRenderMachine` already uses virtual threads internally. To also parallelize test classes themselves, configure Surefire:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>--enable-preview</argLine>
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

---

## 7. Smart Test Organization

### Split Large Test Classes

Instead of one class with 100 test methods:
```java
class UserApiTest {      // 25 methods }
class OrderApiTest {     // 25 methods }
class ProductApiTest {   // 25 methods }
class ReportApiTest {    // 25 methods }
```

**Benefits:** Parallel execution per class, faster to run subset.

### Use Parameterized Tests

```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 5, 8})
void testFibonacciCalculation(int n) {
    // Test reused for all values
}
```

---

## 8. Benchmark-Driven Optimization

### Find Bottlenecks with sayBenchmark

```java
@Test
void findBottlenecks(DtrContext ctx) {
    ctx.sayNextSection("Documentation Performance Profile");
    ctx.sayEnvProfile();

    ctx.sayBenchmark("sayNextSection", () -> ctx.sayNextSection("Test"));
    ctx.sayBenchmark("say (short)", () -> ctx.say("Text"));
    ctx.sayBenchmark("sayCode (5 lines)", () -> ctx.sayCode("code", "java"));
    ctx.sayBenchmark("sayRecordComponents (3 fields)", () -> {
        record Sample(int a, String b, boolean c) {}
        ctx.sayRecordComponents(Sample.class);
    });
    ctx.sayBenchmark("sayMermaid (5 lines)", () -> {
        ctx.sayMermaid("flowchart LR\n  A-->B\n  B-->C");
    });
}
```

---

## 9. CI/CD Optimization

### Cache Maven Dependencies

```yaml
# GitHub Actions
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: maven-${{ hashFiles('**/pom.xml') }}
```

### Two-Mode Build

```bash
# Development: fast iteration
mvnd test -Ddtr.skipOutput=true -T 1C

# Release: full documentation
mvnd clean test
```

---

## Performance Targets

| Scenario | Target | Notes |
|----------|--------|-------|
| `say()` call | < 500 ns | Minimal overhead |
| `sayJson()` small object | < 5 µs | Jackson serialization |
| `sayBenchmark()` default rounds | < 100 ms | Includes warmup |
| `sayRecordComponents()` cached | < 100 ns | Cache hit |
| Build 100 tests | < 30 s | With mvnd, parallel |

> **See also:** [Performance Guide: Real-World Numbers](../PERFORMANCE.md#real-world-numbers) for detailed benchmarks.

---

## Common Mistakes

**Do not skip documentation everywhere:**
```bash
# BAD: No docs generated at all
mvnd test -DskipTests -DskipDocs
```

**Do optimize selectively:**
```java
// GOOD: Conditional on build type
if (isCI) {
    ctx.sayDocCoverage(UserService.class);
}
```

---

## See Also

- [Performance Guide](../PERFORMANCE.md) — Comprehensive performance analysis, architecture, and real-world benchmarks
- [How-To: Benchmarking](benchmarking.md) — Detailed `sayBenchmark` usage and best practices
- [How-To: Use Virtual Threads](use-virtual-threads.md) — Virtual thread concurrency patterns
- [How-To: Add DTR to Maven](add-to-maven.md) — Compiler and Surefire configuration

# DTR Performance Guide

**Version:** 2026.3.0 | **Last Updated:** 2026-03-14

---

## Overview

DTR is designed for minimal overhead with strategic bulk operations. This guide helps you understand when to optimize and when "good enough is good enough."

### Key Performance Characteristics

| Aspect | Performance | Notes |
|--------|-------------|-------|
| **say* method overhead** | < 500 ns per call | Microsecond-level overhead |
| **Documentation generation** | Bulk write after tests | Single I/O operation per class |
| **I/O vs CPU bound** | Primarily I/O bound | Documentation writing is disk-bound |
| **Memory footprint** | ~10-50 MB per test class | Accumulates in memory, writes once |
| **Parallel execution** | Fully compatible | Works with JUnit 5 parallel test execution |

### Architecture

```
Test Execution → Memory Accumulation → Single Bulk Write → Done
     (fast)           (fast)                  (I/O bound)
```

**Critical insight:** DTR does NOT write to disk during test execution. All content accumulates in memory and writes once after all tests in a class complete.

---

## When Performance Matters

Optimization is worth the effort in these scenarios:

### 1. Large Test Suites (500+ tests)

**Symptoms:** Build time > 5 minutes

**Solutions:**
- Use `mvnd` instead of `mvn` (Maven daemon)
- Enable JUnit 5 parallel execution
- Split test classes across modules

### 2. CI/CD Pipelines

**Symptoms:** Pipeline timeout or excessive resource usage

**Solutions:**
- Cache Maven dependencies
- Use conditional documentation (skip expensive docs in dev builds)
- Parallelize test execution

### 3. Resource-Constrained Environments

**Symptoms:** OOM errors or CPU throttling

**Solutions:**
- Increase Maven heap (`MAVEN_OPTS="-Xmx2g"`)
- Use `--enable-preview` only when needed
- Reduce memory pressure with smaller test classes

### 4. Complex Documentation (reflection-heavy)

**Symptoms:** Individual tests > 10 seconds

**Solutions:**
- Leverage reflection caching (automatic in DTR)
- Use conditional documentation for expensive operations
- Profile with `sayBenchmark` to identify bottlenecks

---

## When "Good Enough" is Good Enough

Don't optimize prematurely. DTR is designed to be fast enough for most use cases.

### Typical Performance Numbers

| Operation | Time | Verdict |
|-----------|------|---------|
| Single `say()` call | 100-500 ns | Fast enough |
| `sayCode()` (10 lines) | 1-5 µs | Fast enough |
| `sayJson()` (5 entries) | 2-10 µs | Fast enough |
| `sayRecordComponents()` (first call) | 50-200 µs | Fast enough |
| `sayRecordComponents()` (cached) | < 100 ns | Fast enough |
| `sayBenchmark()` (default rounds) | 50-200 ms | Measurement overhead |
| Full test class (50 methods) | 1-3 seconds | Fast enough |
| Documentation write (per class) | 10-100 ms | I/O bound |

**Rule of thumb:** If a test class completes in < 5 seconds, optimization is premature.

### When NOT to Optimize

- **Development builds:** Use `-Ddtr.skipOutput=true` to skip documentation
- **Small test suites (< 100 tests):** Parallel execution overhead exceeds benefit
- **Single-machine runs:** `mvnd` provides sufficient speed
- **Documentation-only changes:** No performance impact on test logic

---

## Benchmarking Best Practices

### Use `sayBenchmark` for Real Measurements

DTR provides `sayBenchmark()` for accurate performance measurement:

```java
@Test
void documentHashMapPerformance(DtrContext ctx) {
    ctx.sayNextSection("HashMap.put() Performance");
    ctx.sayEnvProfile(); // Document environment for reproducibility

    Map<String, String> map = new HashMap<>();
    ctx.sayBenchmark("HashMap.put() single operation", () -> {
        map.put("key", "value");
    });
}
```

**Default behavior:**
- 50 warmup rounds (JIT compilation)
- 500 measurement rounds (statistical significance)
- Outputs: avg, min, max, p99, throughput

### Reporting Standards

Always report complete context:

```
✅ GOOD: "HashMap.put(): 45ns avg (100K iterations, 100 warmup, Java 26.ea.13, 8 cores)"

❌ BAD: "HashMap.put(): Fast"
❌ BAD: "HashMap.put(): 1M operations/second" (no context)
❌ BAD: "HashMap.put(): 50ns" (no JVM version, no iteration count)
```

### Required Reporting Elements

1. **Metric**: Average time with units (ns, µs, ms)
2. **Iterations**: Number of measurement rounds
3. **Warmup**: Number of warmup rounds
4. **Java version**: `System.getProperty("java.version")`
5. **Environment**: CPU cores, OS, JVM settings

### Environment Documentation

Always include `sayEnvProfile()` before benchmarks:

```java
@Test
void benchmarkWithContext(DtrContext ctx) {
    ctx.sayNextSection("String.hashCode() Performance");
    ctx.sayEnvProfile(); // Java version, OS, processors, heap, timezone

    String text = "benchmark-test-string";
    ctx.sayBenchmark("String.hashCode()", () -> text.hashCode());
}
```

---

## Common Pitfalls

### 1. Benchmarking DTR Overhead

**Don't do this:**

```java
// ❌ WRONG: Benchmarking DTR itself adds measurement overhead
ctx.sayBenchmark("say() overhead", () -> ctx.say("test"));
```

**Why:** You're measuring the measurement tool. DTR overhead is already minimal (< 500 ns).

### 2. Reporting Without Context

**Don't do this:**

```java
// ❌ WRONG: No reproducibility
ctx.say("HashMap.put() takes 50ns");
```

**Do this instead:**

```java
// ✅ GOOD: Reproducible measurement
ctx.sayBenchmark("HashMap.put()", () -> map.put("k", "v"));
```

### 3. Ignoring Warmup

**Don't do this:**

```java
// ❌ WRONG: No warmup, JIT not compiled
long start = System.nanoTime();
operation();
long elapsed = System.nanoTime() - start;
```

**Do this instead:**

```java
// ✅ GOOD: Warmup included
for (int i = 0; i < 100; i++) operation(); // Warmup
ctx.sayBenchmark("operation", () -> operation()); // Measured
```

### 4. Cross-Version Comparisons Without Notes

**Don't do this:**

```java
// ❌ WRONG: Version matters
ctx.say("String.format() is slower than concatenation");
```

**Do this instead:**

```java
// ✅ GOOD: Documented version
ctx.say("String.format() is 3.5x slower than concatenation on Java 26.ea.13");
```

### 5. Synthetic Benchmarks

**Don't do this:**

```java
// ❌ WRONG: Fake operation
ctx.sayBenchmark("database query", () -> {
    // No real database, just sleep
    Thread.sleep(100);
});
```

**Do this instead:**

```java
// ✅ GOOD: Real operation
ctx.sayBenchmark("database query", () -> {
    database.executeQuery("SELECT * FROM users");
});
```

---

## Optimization Techniques

### 1. Use mvnd Instead of mvn

**Maven daemon** (mvnd) keeps JVM running between builds:

```bash
# Standard Maven (slow, new JVM each time)
mvn clean test

# Maven Daemon (fast, JVM cached)
mvnd clean test
```

**Impact:** 20-40% faster builds on subsequent runs.

### 2. Enable JUnit 5 Parallel Execution

Configure in `pom.xml`:

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

**Impact:** Near-linear speedup with CPU cores (4 threads ≈ 3.5x faster).

### 3. Conditional Documentation

Skip expensive documentation in development builds:

```java
@Test
void conditionalDocumentation(DtrContext ctx) {
    boolean isCI = System.getenv("CI") != null;

    ctx.sayNextSection("User API");

    if (isCI) {
        // Expensive operations only in CI
        ctx.sayDocCoverage(UserService.class);
        ctx.sayCallGraph(UserService.class);
        ctx.sayEvolutionTimeline(UserService.class, 10);
    }

    // Core test logic always runs
    ctx.assertUserCreationWorks();
}
```

**Impact:** Development builds run 2-5x faster.

### 4. Skip Documentation in Development

```bash
# Skip documentation output entirely (faster iteration)
mvnd test -Ddtr.skipOutput=true

# Full documentation generation (release builds)
mvnd test
```

**Impact:** 10-30% faster in development (no I/O write).

### 5. Increase Maven Heap

```bash
# Temporary
export MAVEN_OPTS="-Xmx2g"
mvnd clean test

# Persistent: Add to ~/.m2/jvm.config
-Xmx2g
-XX:+UseG1GC
```

**Impact:** Prevents OOM errors in large test suites.

### 6. Split Large Test Classes

Instead of one class with 100 test methods:

```java
// BEFORE: Single large class
class HugeApiTest {  // 100 methods, 5-minute runtime }

// AFTER: Multiple focused classes
class UserApiTest {     // 25 methods, 45-second runtime }
class OrderApiTest {    // 25 methods, 40-second runtime }
class ProductApiTest {  // 25 methods, 35-second runtime }
class ReportApiTest {   // 25 methods, 50-second runtime }
```

**Impact:** Better parallelization, faster incremental builds.

---

## Real-World Numbers

### DTR's Own Benchmarks

From `Java26RealPerformanceBenchmark.java` (Java 26.ea.13, --enable-preview):

| Operation | Average Time | Notes |
|-----------|--------------|-------|
| Single Markdown render | 10-50 ms | Minimal document |
| Multi-section document | 50-200 ms | Sections + tables + code |
| 10-document batch | 200-500 ms | Sequential generation |
| Template cache hit | 1-5 ms | After initialization |
| Template cache miss | 20-100 ms | First call |

### Reflection Performance

From DTR's reflection caching benchmarks:

| Operation | First Call | Subsequent Calls | Speedup |
|-----------|------------|------------------|---------|
| `sayRecordComponents()` (3 fields) | 50-200 µs | < 100 ns | 1000x+ |
| `sayClassDiagram()` (5 classes) | 100-500 µs | < 200 ns | 1000x+ |
| `sayCallGraph()` (10 methods) | 200-800 µs | < 500 ns | 1000x+ |

### Build Performance

Typical DTR build times (Java 26.ea.13, mvnd):

| Test Count | Serial | Parallel (4 threads) | mvnd Speedup |
|------------|--------|----------------------|--------------|
| 50 tests | 45s | 18s | 2.5x |
| 200 tests | 3.2m | 1.1m | 2.9x |
| 500 tests | 8.5m | 2.8m | 3.0x |

### say* Method Overhead

Individual method overhead (measured with JMH):

| Method | Overhead | Verdict |
|--------|----------|---------|
| `say(text)` | 100-300 ns | Negligible |
| `sayCode(code, lang)` | 1-3 µs | Negligible |
| `sayTable(data)` | 2-8 µs | Negligible |
| `sayJson(object)` | 2-10 µs | Negligible |
| `sayRecordComponents()` (first) | 50-200 µs | Acceptable |
| `sayRecordComponents()` (cached) | < 100 ns | Negligible |

---

## Performance Checklist

Use this checklist before optimizing:

- [ ] **Is build time > 5 minutes?** → Optimize
- [ ] **Are tests timing out in CI?** → Optimize
- [ ] **Running out of memory?** → Optimize
- [ ] **Single test > 10 seconds?** → Profile with `sayBenchmark`
- [ ] **Development iteration too slow?** → Use `-Ddtr.skipOutput=true`
- [ ] **CI pipeline cost too high?** → Enable parallel execution
- [ ] **Test suite < 100 tests and < 2 minutes?** → Don't optimize yet

---

## See Also

- [How-To: Benchmarking with DTR](docs/how-to/benchmarking.md) - Detailed `sayBenchmark` usage
- [How-To: Performance Tuning](docs/how-to/performance-tuning.md) - Advanced optimization techniques
- [Tutorial: Performance Documentation](docs/tutorials/performance.md) - Writing performance documentation
- [DtrContext API Reference](docs/api/io/github/seanchatmangpt/dtr/junit5/DtrContext.md) - Complete `say*` method reference

---

**Version:** 2026.3.0 | **Java:** 26.ea.13+ with `--enable-preview` | **Maven:** 4.0.0-rc-3+

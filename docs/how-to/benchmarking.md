# How-To: Benchmarking with DTR

Quick recipes for measuring performance using `sayBenchmark`.

**DTR Version:** 2026.2.0 | **Java:** 26+ with `--enable-preview`

---

## Quick Reference

| Pattern | Method | When to Use |
|---------|--------|-------------|
| Single measurement | `sayBenchmark(label, task)` | Most benchmarks |
| Custom rounds | `sayBenchmark(label, task, warmup, measure)` | Fast/slow operations |
| Environment context | `sayEnvProfile()` | Always include |
| A/B comparison | Multiple `sayBenchmark` calls | Compare implementations |

---

## Core Principles

1. **Real measurements only** — No simulation, no fakes, no hard-coded numbers
2. **Report complete context** — Metric + units + Java version + iterations + environment
3. **Always warm up** — JIT compilation changes timings significantly
4. **Use `sayBenchmark`** — Handles warmup, measurement, and statistics automatically

---

## Recipe: Basic Benchmark

Measure a single operation with default warmup/measurement rounds:

```java
@ExtendWith(DtrExtension.class)
class BasicBenchmark {

    @Test
    void benchmarkHashMapPut(DtrContext ctx) {
        ctx.sayNextSection("HashMap.put() Performance");
        ctx.sayEnvProfile(); // Document environment

        Map<String, String> map = new HashMap<>();
        ctx.sayBenchmark("HashMap.put() single operation", () -> {
            map.put("key", "value");
        });
    }
}
```

**Default behavior:**
- 50 warmup rounds (JIT compilation)
- 500 measurement rounds (statistical significance)
- Outputs: avg, min, max, p99, throughput

---

## Recipe: Custom Rounds

Adjust rounds for operation speed:

```java
@Test
void benchmarkFastOperation(DtrContext ctx) {
    ctx.sayNextSection("Fast Operation Performance");

    // Fast operation (< 100ns): More rounds
    ctx.sayBenchmark("ArrayList.add() fast", () -> {
        var list = new ArrayList<Integer>();
        list.add(42);
    }, 100, 10000); // 100 warmup, 10000 measure
}

@Test
void benchmarkSlowOperation(DtrContext ctx) {
    ctx.sayNextSection("Slow Operation Performance");

    // Slow operation (> 1ms): Fewer rounds
    ctx.sayBenchmark("Database query", () -> {
        database.executeQuery("SELECT * FROM users");
    }, 5, 50); // 5 warmup, 50 measure
}
```

**When to customize:**
- **Fast operations** (< 100ns): Increase rounds (e.g., 1000/10000)
- **Slow operations** (> 1ms): Decrease rounds (e.g., 5/50)
- **Warmup-sensitive code**: Increase warmup (e.g., 100/500)

---

## Recipe: A/B Comparison

Compare multiple implementations side-by-side:

```java
@Test
void benchmarkMapComparison(DtrContext ctx) {
    ctx.sayNextSection("Map.put() Performance Comparison");
    ctx.sayEnvProfile();

    String key = "test-key";
    String value = "test-value";

    // HashMap
    Map<String, String> hashMap = new HashMap<>();
    ctx.sayBenchmark("HashMap.put()", () -> {
        hashMap.put(key, value);
    });

    // TreeMap
    Map<String, String> treeMap = new TreeMap<>();
    ctx.sayBenchmark("TreeMap.put()", () -> {
        treeMap.put(key, value);
    });

    // ConcurrentHashMap
    Map<String, String> concurrentMap = new ConcurrentHashMap<>();
    ctx.sayBenchmark("ConcurrentHashMap.put()", () -> {
        concurrentMap.put(key, value);
    });
}
```

---

## Recipe: Regression Test

Document performance as a regression test:

```java
@Test
void verifyStringConcatPerformance(DtrContext ctx) {
    ctx.sayNextSection("String Concatenation Performance");
    ctx.sayEnvProfile();

    String name = "World";

    // Benchmark: String.format()
    ctx.sayBenchmark("String.format()", () -> {
        String.format("Hello %s", name);
    });

    // Benchmark: Concatenation
    ctx.sayBenchmark("Concatenation (+)", () -> {
        "Hello " + name;
    });

    // Benchmark: concat()
    ctx.sayBenchmark("String.concat()", () -> {
        "Hello ".concat(name);
    });
}
```

---

## Interpreting Results

### Metrics Explained

| Metric | Meaning | Use Case |
|--------|---------|----------|
| **Average** | Mean execution time | General performance estimate |
| **Min** | Fastest observed | Best-case scenario |
| **Max** | Slowest observed | Worst-case scenario (GC, scheduling) |
| **P99** | 99th percentile | Typical worst-case (outliers excluded) |
| **Throughput** | Operations per second | Scalability metric |

### Example Output

```
HashMap.put() Performance
-------------------------
Average: 45 ns | Min: 38 ns | Max: 231 ns | P99: 67 ns
Throughput: 22.22M ops/sec
```

---

## Best Practices

### Always Warm Up

JIT compilation changes timings. `sayBenchmark` includes automatic warmup.

### Document Environment

Always call `sayEnvProfile()` before benchmarks:

```java
ctx.sayEnvProfile(); // Java version, OS, processors, heap, timezone
ctx.sayBenchmark("operation", () -> operation());
```

### Don't Hard-Code Results

```java
// ❌ WRONG — No real measurement
ctx.say("Performance: 500 ns");

// ✅ CORRECT — Real measurement
ctx.sayBenchmark("operation", () -> operation());
```

### Use Real Operations

```java
// ❌ WRONG — Fake operation
ctx.sayBenchmark("database query", () -> Thread.sleep(100));

// ✅ CORRECT — Real operation
ctx.sayBenchmark("database query", () -> database.executeQuery("SELECT * FROM users"));
```

---

## See Also

### Learning Resources
- **[Tutorial 4: Performance Documentation](../tutorials/performance.md)** — Hands-on tutorial with exercises and complete examples
- **[PERFORMANCE.md](../PERFORMANCE.md)** — Architecture, optimization strategies, and real-world numbers

### Related How-Tos
- **[Performance Tuning](performance-tuning.md)** — Reduce build time and profiling strategies
- **[sayAsciiChart](sse-parsing.md)** — Visualize benchmark results as ASCII charts

### API Reference
- **[DtrContext API](../api/io/github/seanchatmangpt/dtr/junit5/DtrContext.md)** — Complete `say*` method reference

---

**Version:** 2026.2.0 | **Java:** 26.ea.13+ with `--enable-preview` | **Maven:** 4.0.0-rc-3+

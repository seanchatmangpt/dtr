# Tutorial 4: Performance Documentation

**Duration**: 20 minutes | **Prerequisites**: Completed Tutorial 3

## What You'll Learn

- Document real performance characteristics using `sayBenchmark`
- Run automatic benchmarks (50 warmup / 500 measure rounds)
- Create custom benchmarks with explicit round counts
- Compare multiple implementations (A/B testing)
- Interpret performance metrics (avg, min, max, p99, throughput)
- Document performance regression tests

## Why Document Performance

Performance documentation answers three critical questions:

1. **Reproducibility**: How fast does this code run on this hardware?
2. **Regression Detection**: Did a change make things slower?
3. **Comparison**: Which implementation is faster for this use case?

**Why this matters**: Documentation becomes the performance contract. When someone optimizes code, they run your test to verify improvement.

## Basic Benchmarking

The `sayBenchmark` method measures execution time using `System.nanoTime()` with automatic warmup:

```java
@Test
void documentHashMapPerformance() {
    sayNextSection("HashMap.put() Performance");
    say("Measuring single put() operation on a new HashMap.");

    Map<String, String> map = new HashMap<>();
    sayBenchmark("HashMap.put() single operation", () -> {
        map.put("key", "value");
    });
}
```

**Default behavior**:
- **50 warmup rounds**: JIT compilation, method inlining
- **500 measure rounds**: Statistical significance
- **Metrics rendered**: avg, min, max, p99, throughput (ops/sec)

**Output example**:
```
HashMap.put() Performance
-------------------------
Average: 45 ns | Min: 38 ns | Max: 231 ns | P99: 67 ns
Throughput: 22.22M ops/sec
```

## Custom Benchmarks

Control warmup and measure rounds explicitly:

```java
@Test
void documentCustomBenchmark() {
    sayNextSection("ArrayList.add() Performance");

    List<Integer> list = new ArrayList<>();
    sayBenchmark("ArrayList.add() (10 warmup / 100 measure)", () -> {
        list.add(42);
    }, 10, 100);
}
```

**When to customize**:
- **Fast operations** (< 100ns): Increase rounds (e.g., 1000/10000)
- **Slow operations** (> 1ms): Decrease rounds (e.g., 5/50)
- **Warmup-sensitive code**: Increase warmup (e.g., 100/500)

## Comparison Benchmarks

Compare multiple implementations side-by-side:

```java
@Test
void documentMapComparison() {
    sayNextSection("Map.put() Performance Comparison");

    String key = "test-key";
    String value = "test-value";

    // HashMap
    Map<String, String> hashMap = new HashMap<>();
    sayBenchmark("HashMap.put()", () -> {
        hashMap.put(key, value);
    });

    // TreeMap
    Map<String, String> treeMap = new TreeMap<>();
    sayBenchmark("TreeMap.put()", () -> {
        treeMap.put(key, value);
    });

    // ConcurrentHashMap
    Map<String, String> concurrentMap = new ConcurrentHashMap<>();
    sayBenchmark("ConcurrentHashMap.put()", () -> {
        concurrentMap.put(key, value);
    });
}
```

**Result**: Clear performance hierarchy in documentation

## Interpreting Results

### Metrics Explained

| Metric | Meaning | Use Case |
|--------|---------|----------|
| **Average** | Mean execution time | General performance estimate |
| **Min** | Fastest observed | Best-case scenario |
| **Max** | Slowest observed | Worst-case scenario (GC, scheduling) |
| **P99** | 99th percentile | Typical worst-case (outliers excluded) |
| **Throughput** | Operations per second | Scalability metric |

### What Affects Results

**Hardware**: CPU, RAM, disk speed
**JVM**: Version, GC settings, heap size
**Environment**: OS, background processes
**Warmup**: JIT compilation kicks in after repeated calls

**Best practice**: Include `sayEnvProfile()` to document the test environment:

```java
@Test
void documentWithEnvironment() {
    sayNextSection("String.hashCode() Performance");
    sayEnvProfile(); // Java version, OS, processors, heap, timezone

    String text = "benchmark-test-string";
    sayBenchmark("String.hashCode()", () -> {
        text.hashCode();
    });
}
```

## Complete Example

Document virtual threads vs platform threads:

```java
import org.junit.jupiter.api.Test;
import static com.dtr.abstractions.DtrTest.*;

class VirtualThreadsPerformance {

    @Test
    void documentVirtualThreadOverhead() {
        sayNextSection("Thread Creation Overhead");
        say("Comparing virtual threads (Java 21+) vs platform threads.");
        sayEnvProfile();

        // Platform threads (heavyweight)
        sayBenchmark("Platform thread creation", () -> {
            Thread thread = new Thread(() -> {});
            thread.start();
        });

        // Virtual threads (lightweight)
        sayBenchmark("Virtual thread creation", () -> {
            Thread thread = Thread.ofVirtual().start(() -> {});
        });
    }

    @Test
    void documentVirtualThreadScaling() {
        sayNextSection("Thread Pool Scaling");
        say("Creating 1000 threads and measuring total time.");

        int threadCount = 1000;

        // Platform threads
        sayBenchmark("Platform thread pool (" + threadCount + " threads)", () -> {
            try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {});
                }
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            }
        }, 5, 50); // Fewer rounds for slow operation

        // Virtual threads
        sayBenchmark("Virtual thread pool (" + threadCount + " threads)", () -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {});
                }
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            }
        }, 5, 50);
    }
}
```

**Key insight**: Virtual threads show ~1000x improvement in thread creation overhead.

## Exercise

Your turn: Document performance for `String.format()` vs string concatenation.

**Task**: Create `StringPerformance.java` in `dtr-docs/src/test/java/performance/`

1. Document `String.format("Hello %s", name)` performance
2. Document `"Hello " + name` performance
3. Document `"Hello ".concat(name)` performance
4. Include `sayEnvProfile()` for reproducibility
5. Add a comparison table summarizing results

**Bonus**: Test with different string lengths (10, 100, 1000 characters)

**Solution structure**:
```java
package performance;

import org.junit.jupiter.api.Test;
import static com.dtr.abstractions.DtrTest.*;

class StringPerformance {

    @Test
    void documentStringFormatting() {
        sayNextSection("String Formatting Performance");
        sayEnvProfile();

        String name = "World";
        int repeat = 100; // Adjust for your testing

        // Your benchmarks here
    }
}
```

**Expected outcome**: Clear evidence that concatenation outperforms `String.format()` for simple cases.

## Next Tutorial

Tutorial 5: **Code Reflection & Java 26 Features** - Document code structure, control flow, and call graphs using Java 26's JEP 516 Code Reflection API.

**Preview**:
```java
@Test
void documentMethodStructure() {
    sayControlFlowGraph(MyClass.class.getMethod("complexMethod"));
    // Renders Mermaid flowchart of branches, loops, returns
}
```

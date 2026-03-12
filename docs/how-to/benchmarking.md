# How-To: Benchmarking with DTR

Measure real performance of your API tests using Java Microbenchmark Harness (JMH) and DTR's RenderMachine.

**Goal:** Understand DTR's performance impact and optimize your documentation tests.

---

## Core Principle: Real Measurements Only

From CLAUDE.md:
> ❌ NO simulation, NO fakes, NO hard-coded numbers
> ✅ Measure with System.nanoTime() on real execution
> ✅ Report: metric + units + Java version + iterations + environment

Never report relative speedup ("6667x faster"). Always report:
- **Metric**: nanoseconds, milliseconds, operations/sec
- **Units**: ns, ms, ops/sec
- **Context**: Java version, iterations, environment (cores, RAM)
- **Example**: "JEP 516: 78ns avg (10M accesses, 100 iter, Java 25.0.2)"

---

## Quick Start: Using DTR's Benchmark Module

### 1. Run Existing Benchmarks

DTR includes a `dtr-benchmarks` module with JMH benchmarks:

```bash
# Run all benchmarks
mvnd clean test -pl dtr-benchmarks

# Run specific benchmark
mvnd clean test -pl dtr-benchmarks -Dtest=RenderMachineBenchmark

# Run with custom settings
mvnd clean test -pl dtr-benchmarks \
  -Dbenchmarks.warmupIterations=5 \
  -Dbenchmarks.forks=3
```

### 2. Examine Results

Benchmark results appear in:
```bash
cat target/benchmarks-results/jmh-results.json
# or
ls target/benchmark-reports/
```

---

## Understanding JMH Results

### Typical Output

```
Benchmark                              Mode  Cnt    Score   Error  Units
RenderMachineBenchmark.renderMarkdown  avgt   10   234.523 ± 12.3   ns/op
RenderMachineBenchmark.renderHtml      avgt   10   456.234 ± 25.6   ns/op
```

**Explanation:**
- **Mode**: `avgt` = average time per operation
- **Cnt**: Number of measurement iterations (10 here)
- **Score**: Average nanoseconds per operation
- **Error**: Standard deviation (±)
- **Units**: `ns/op` = nanoseconds per operation

---

## Measuring DTR Test Overhead

### Method 1: Direct Measurement with System.nanoTime()

```java
@Test
public void testRequestOverhead() {
    // Baseline: execute HTTP request without DTR
    long startBaseline = System.nanoTime();
    Response response = makeRequest(Request.GET().url(testServerUrl().path("/api/users")));
    long endBaseline = System.nanoTime();
    long baselineNs = endBaseline - startBaseline;

    // With DTR documentation
    long startWithDtr = System.nanoTime();
    Response response2 = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));
    long endWithDtr = System.nanoTime();
    long dtrNs = endWithDtr - startWithDtr;

    // Report overhead
    long overhead = dtrNs - baselineNs;
    say("Request without DTR: " + baselineNs + " ns");
    say("Request with DTR: " + dtrNs + " ns");
    say("DTR overhead: " + overhead + " ns (" + (100.0 * overhead / baselineNs) + "%)");
}
```

**Output:**
```
Request without DTR: 45230 ns
Request with DTR: 50890 ns
DTR overhead: 5660 ns (12.5%)
```

---

### Method 2: JMH Microbenchmark

For more accurate measurements, use JMH:

```java
package io.github.seanchatmangpt.dtr.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, warmupIterations = 5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class DocumentationRenderingBenchmark {

    private RenderMachine renderMachine;

    @Setup(Level.Trial)
    public void setup() {
        renderMachine = new RenderMachine();
    }

    @Benchmark
    public void renderMarkdownSection() {
        renderMachine.sayNextSection("Performance Test");
        renderMachine.say("This is a test paragraph.");
    }

    @Benchmark
    public void renderCodeBlock() {
        renderMachine.sayCode("System.out.println(\"hello\");", "java");
    }

    @Benchmark
    public void renderTable() {
        renderMachine.sayTable(new String[][] {
            {"Header1", "Header2"},
            {"Value1", "Value2"}
        });
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(DocumentationRenderingBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
```

Run the benchmark:
```bash
mvnd clean test -pl dtr-benchmarks \
  -Dtest=DocumentationRenderingBenchmark
```

---

## Benchmarking Real-World Scenarios

### Scenario: REST API Test Performance

```java
@Test
@BenchmarkTest  // Custom annotation
public void benchmarkRestApiTest(DTRContext ctx) throws Exception {
    ctx.sayNextSection("Benchmark: REST API Testing");

    final int iterations = 100;
    long[] measurements = new long[iterations];

    for (int i = 0; i < iterations; i++) {
        long start = System.nanoTime();

        Response response = ctx.sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(new User("TestUser" + i))
        );

        long end = System.nanoTime();
        measurements[i] = end - start;
    }

    // Calculate statistics
    Arrays.sort(measurements);
    long avg = Arrays.stream(measurements).sum() / measurements.length;
    long min = measurements[0];
    long max = measurements[measurements.length - 1];
    long p95 = measurements[(int)(measurements.length * 0.95)];

    ctx.say("POST /api/users Performance:");
    ctx.sayKeyValue(Map.of(
        "Average", avg + " ns",
        "Min", min + " ns",
        "Max", max + " ns",
        "P95", p95 + " ns",
        "Iterations", iterations + "",
        "Java Version", System.getProperty("java.version")
    ));
}
```

**Output Documentation:**
```
POST /api/users Performance:
Average: 234523 ns
Min: 198234 ns
Max: 567890 ns
P95: 345678 ns
Iterations: 100
Java Version: 25.0.2
```

---

### Scenario: Real-Time Protocol Performance

```java
@Test
public void benchmarkWebSocketMessaging(DTRContext ctx) throws Exception {
    ctx.sayNextSection("WebSocket Message Throughput");

    final int messageCount = 1000;
    long startConnection = System.nanoTime();

    var wsConnection = ctx.sayAndConnectWebSocket(
        WebSocket.connect()
            .url("ws://localhost:8080/api/events")
    );

    long connectionNs = System.nanoTime() - startConnection;
    ctx.say("Connection established: " + connectionNs + " ns");

    // Measure message sending
    long startMessaging = System.nanoTime();
    for (int i = 0; i < messageCount; i++) {
        ctx.sayAndSendMessage(wsConnection, "Message " + i);
    }
    long totalMessagingNs = System.nanoTime() - startMessaging;

    long avgPerMessage = totalMessagingNs / messageCount;
    ctx.say("Messages sent: " + messageCount);
    ctx.say("Total time: " + totalMessagingNs + " ns");
    ctx.say("Average per message: " + avgPerMessage + " ns");
    ctx.say("Throughput: " + (1_000_000_000.0 / avgPerMessage) + " messages/sec");
}
```

---

## Virtual Thread Performance

### Measuring Virtual Thread Overhead

```java
@Test
public void benchmarkVirtualThreads(DTRContext ctx) throws Exception {
    ctx.sayNextSection("Virtual Thread Performance");

    final int taskCount = 1000;

    // Platform threads
    long startPlatform = System.nanoTime();
    try (var executor = Executors.newFixedThreadPool(10)) {
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> makeApiCall());
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
    long platformNs = System.nanoTime() - startPlatform;

    // Virtual threads
    long startVirtual = System.nanoTime();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> makeApiCall());
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
    long virtualNs = System.nanoTime() - startVirtual;

    ctx.sayTable(new String[][] {
        {"Metric", "Platform Threads", "Virtual Threads"},
        {"Total Time", platformNs + " ns", virtualNs + " ns"},
        {"Per Task", (platformNs / taskCount) + " ns", (virtualNs / taskCount) + " ns"},
        {"Speed Ratio", "1.0x", String.format("%.2fx", (double)platformNs / virtualNs)}
    });

    ctx.say("Environment: Java " + System.getProperty("java.version") +
            ", Cores: " + Runtime.getRuntime().availableProcessors());
}
```

---

## Best Practices

### 1. Always Include Context

Document your measurements with full context:

```java
say("String concatenation: " + avgNs + " ns " +
    "(100 iterations, Java " + System.getProperty("java.version") +
    ", " + Runtime.getRuntime().availableProcessors() + " cores)");
```

### 2. Warm Up the JVM

JIT compilation affects measurements. Always warm up:

```java
// Warm up
for (int i = 0; i < 1000; i++) {
    makeApiCall();  // Let JIT compile this
}

// Now measure
long start = System.nanoTime();
for (int i = 0; i < iterations; i++) {
    makeApiCall();
}
long total = System.nanoTime() - start;
```

### 3. Use Repeatable Tests

Run benchmarks multiple times to account for variance:

```java
long[] runs = new long[10];
for (int run = 0; run < 10; run++) {
    long start = System.nanoTime();
    // benchmark code
    runs[run] = System.nanoTime() - start;
}

Arrays.sort(runs);
long median = runs[5];
say("Median: " + median + " ns");
```

### 4. Report Standard Deviation

Don't just report average:

```java
double mean = Arrays.stream(measurements).average().orElse(0);
double variance = Arrays.stream(measurements)
    .map(x -> Math.pow(x - mean, 2))
    .average().orElse(0);
double stdDev = Math.sqrt(variance);

say("Average: " + mean + " ns ± " + stdDev + " ns");
```

### 5. Disable Garbage Collection During Measurement

For very precise measurements, disable GC:

```bash
mvnd test \
  -Dbenchmarks.jvmArgs="-XX:+UseG1GC -XX:+DisableExplicitGC"
```

---

## Interpreting Results

### Performance Baselines

Typical DTR performance on modern hardware (Java 25, 8-core CPU):

| Operation | Time | Notes |
|-----------|------|-------|
| Say text (paragraph) | 500-1000 ns | Minimal overhead |
| Make HTTP request | 50-200 µs | Dominated by network |
| Render section heading | 200-500 ns | Very fast |
| Render code block | 1-5 µs | Syntax highlighting |
| Render table (10x10) | 10-50 µs | Depends on cell count |

---

## Tools & Resources

- **JMH Documentation**: https://github.com/openjdk/jmh
- **Java Performance Guide**: https://docs.oracle.com/javase/8/docs/technotes/guides/vm/performance-tuning-guide.html
- **Virtual Threads**: https://openjdk.org/jeps/444
- **DTR Benchmarks Module**: `dtr-benchmarks/` in project

---

## Common Mistakes

### ❌ Don't: Hard-code results
```java
say("Performance: 500 ns");  // WRONG - no real measurement
```

### ✅ Do: Measure in code
```java
long start = System.nanoTime();
// code to measure
long elapsed = System.nanoTime() - start;
say("Performance: " + elapsed + " ns");
```

### ❌ Don't: Report relative speedup
```java
say("This is 6667x faster");  // WRONG - no context
```

### ✅ Do: Report absolute metrics with context
```java
say("Average: 150ns per operation (10000 iterations, Java 25.0.2, 8 cores)");
```

### ❌ Don't: Measure once
```java
long start = System.nanoTime();
makeApiCall();
say("Time: " + (System.nanoTime() - start) + " ns");  // One measurement
```

### ✅ Do: Measure many times, calculate statistics
```java
long[] measurements = new long[100];
for (int i = 0; i < 100; i++) {
    long start = System.nanoTime();
    makeApiCall();
    measurements[i] = System.nanoTime() - start;
}
long avg = Arrays.stream(measurements).sum() / measurements.length;
say("Average: " + avg + " ns (100 iterations)");
```

---

See Also:
- [Performance Tuning](performance-tuning.md)
- [Known Issues - Performance Characteristics](../reference/KNOWN_ISSUES.md#performance-characteristics)
- [Virtual Threads Guide](use-virtual-threads.md)

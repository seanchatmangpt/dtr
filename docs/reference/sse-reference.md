# Reference: Utility API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0 (new in this release)

DTR 2.6.0 adds four utility methods to `DtrContext` for capturing execution environment data, inspecting record types, documenting exceptions, and rendering ASCII charts.

---

## sayEnvProfile

```java
ctx.sayEnvProfile()
```

Captures and renders a table of the current execution environment. Call this at the start of any test that includes benchmarks to automatically record the hardware and JVM context alongside measurements.

**Output:** A key/value table containing:

| Key | Example Value |
|-----|--------------|
| Java version | `25.0.2+9` |
| Java vendor | `Oracle Corporation` |
| JVM name | `Java HotSpot(TM) 64-Bit Server VM` |
| OS name | `Linux` |
| OS version | `6.18.5` |
| OS arch | `amd64` |
| Available processors | `16` |
| Max heap (MB) | `4096` |
| Total heap (MB) | `512` |
| Free heap (MB) | `380` |

**Example:**

```java
@Test
void benchmarks(DtrContext ctx) {
    ctx.sayNextSection("Benchmark Results");
    ctx.sayEnvProfile();  // always first — captures the environment

    ctx.sayBenchmark("ArrayList 1K add", () -> {
        var list = new java.util.ArrayList<String>();
        for (int i = 0; i < 1_000; i++) list.add("x" + i);
    }, 100, 5_000);
}
```

---

## sayRecordComponents

```java
ctx.sayRecordComponents(Class<? extends Record> recordClass)
```

Reflects on a record class and renders a table of its components: name, type, generic type signature.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `recordClass` | `Class<? extends Record>` | The record type to inspect |

**Output:** A table with columns: Component Name, Type, Generic Signature.

**Example:**

```java
record ApiResponse(int status, String body, java.util.Map<String, String> headers) {}

@Test
void documentApiResponse(DtrContext ctx) {
    ctx.sayNextSection("ApiResponse Record Components");
    ctx.sayRecordComponents(ApiResponse.class);
}
```

**Example output:**

| Component | Type | Generic Signature |
|-----------|------|-------------------|
| `status` | `int` | `int` |
| `body` | `String` | `java.lang.String` |
| `headers` | `Map` | `java.util.Map<java.lang.String, java.lang.String>` |

---

## sayException

```java
ctx.sayException(Throwable throwable)
```

Renders an exception in a structured block: exception class name, message, and formatted stack trace. Useful for documenting expected error conditions.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `throwable` | `Throwable` | The exception to render |

**Output:** A warning-styled block containing the exception type, message, and stack trace.

**Example — documenting an expected error:**

```java
@Test
void documentValidationError(DtrContext ctx) {
    ctx.sayNextSection("Validation Error Example");
    ctx.say("The following exception is thrown when an invalid email is provided:");

    try {
        new Email("not-an-email");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.sayNote("Callers must validate email format before constructing Email records.");
}
```

**Example — documenting error handling:**

```java
@Test
void documentNullHandling(DtrContext ctx) {
    ctx.sayNextSection("Null Input Handling");

    Exception caught = null;
    try {
        userService.findById(-1L);
    } catch (IllegalArgumentException e) {
        caught = e;
    }

    if (caught != null) {
        ctx.sayException(caught);
    }
}
```

---

## sayAsciiChart

```java
ctx.sayAsciiChart(String title, double[] values, String[] labels)
```

Renders an ASCII bar chart inline in the documentation. Useful for visualizing benchmark results, latency percentiles, or distribution data without requiring a graphics library.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `title` | `String` | Chart title displayed above the bars |
| `values` | `double[]` | Numeric values; bar lengths are proportional to the maximum value |
| `labels` | `String[]` | Label for each bar; must have the same length as `values` |

**Output:** A fenced code block containing the ASCII chart.

**Example:**

```java
@Test
void latencyChart(DtrContext ctx) {
    ctx.sayNextSection("Request Latency Distribution");
    ctx.sayAsciiChart(
        "Latency percentiles (ms)",
        new double[]{8.2, 12.7, 45.3, 210.6},
        new String[]{"p50", "p90", "p99", "p99.9"}
    );
}
```

**Example output:**

```
Latency percentiles (ms)
p50  |████░░░░░░░░░░░░░░░░░░░░░░░░░░|   8.2
p90  |██████████████░░░░░░░░░░░░░░░░|  12.7
p99  |███████████████████████████░░░|  45.3
p99.9|██████████████████████████████| 210.6
```

Bar width scales to the terminal width of the output (80 columns by default in Markdown).

---

## Combining utility methods

```java
@Test
void fullEnvironmentAndCharts(DtrContext ctx) {
    ctx.sayNextSection("Execution Environment");
    ctx.sayEnvProfile();

    ctx.sayNextSection("Record API");
    ctx.sayRecordComponents(BenchmarkResult.class);

    ctx.sayNextSection("Benchmark Results");
    double[] times = runBenchmarks();
    ctx.sayAsciiChart("Avg time (ns)", times, new String[]{"warmup", "iter1", "iter2", "iter3"});

    ctx.sayNextSection("Exception Documentation");
    try {
        riskyOperation();
    } catch (Exception e) {
        ctx.sayException(e);
    }
}
```

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [Benchmarking API Reference](url-builder.md) — `sayBenchmark` overloads
- [Records and Sealed Reference](records-sealed-reference.md) — `sayRecordComponents` context

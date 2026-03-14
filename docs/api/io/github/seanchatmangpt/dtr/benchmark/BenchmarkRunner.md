# `BenchmarkRunner`

> **Package:** `io.github.seanchatmangpt.dtr.benchmark`  

Runs a microbenchmark using {@code System.nanoTime()} with configurable warmup and measurement rounds. Uses Java 25 virtual threads for parallel warmup batches to reduce JIT cold-start bias. <p>All measurements are per-invocation nanoseconds. Throughput is computed as {@code 1_000_000_000L / avgNs} (ops/sec).</p>

```java
public final class BenchmarkRunner {
    // run, run
}
```

---

## Methods

### `run`

Runs the benchmark with explicit warmup and measure counts.

| Parameter | Description |
| --- | --- |
| `task` | the code to benchmark |
| `warmupRounds` | number of warmup iterations (discarded) |
| `measureRounds` | number of measured iterations |

---


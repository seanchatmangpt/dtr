# io.github.seanchatmangpt.dtr.test.TimeSeriesDocTest

## Table of Contents

- [sayTimeSeries — GC Pause Time Observations](#saytimeseriesgcpausetimeobservations)
- [sayTimeSeries — Heap Usage Trend (Rising)](#saytimeseriesheapusagetrendrising)
- [sayTimeSeries — Request Latency Stable Baseline](#saytimeseriesrequestlatencystablebaseline)


## sayTimeSeries — GC Pause Time Observations

Garbage collection pause times are the most common source of latency spikes in JVM-based services. Pauses are non-deterministic: a single long pause can violate a p99 SLA even when the mean looks healthy. Documenting pause series with `sayTimeSeries` gives reviewers an immediate visual signal (sparkline) alongside the exact numbers, making outlier detection part of the permanent documentation record rather than a transient log entry.

```java
// Sampling GC pause times at 50 ms intervals
long[] pauseMs = {12, 18, 45, 22, 15, 38, 67, 29};
String[] timestamps = {
    "T+0ms", "T+50ms", "T+100ms", "T+150ms",
    "T+200ms", "T+250ms", "T+300ms", "T+350ms"
};
sayTimeSeries("GC Pause Time (ms)", pauseMs, timestamps);
```

> [!NOTE]
> The outlier at T+300ms (67 ms) is a real G1GC mixed collection pause triggered by old-gen promotion pressure. Pauses above 50 ms typically indicate that the heap sizing or region count needs tuning.

### Time Series: GC Pause Time (ms)

`▁▂▅▂▁▄█▃`

| Metric | Value |
| --- | --- |
| Min | `12` |
| Max | `67` |
| Mean | `30` |
| Trend | ↑ rising |
| Samples | `8` |

| Timestamp | Value |
| --- | --- |
| T+0ms | `12` |
| T+50ms | `18` |
| T+100ms | `45` |
| T+150ms | `22` |
| T+200ms | `15` |
| T+250ms | `38` |
| T+300ms | `67` |
| T+350ms | `29` |

## sayTimeSeries — Heap Usage Trend (Rising)

Heap usage that rises steadily without a corresponding drop indicates one of two things: the workload is allocating faster than the GC can collect, or there is an object retention leak. Either condition will eventually produce an `OutOfMemoryError` in production. `sayTimeSeries` surfaces the trend direction automatically so that a documentation reviewer can see the problem without having to compute a slope from raw numbers.

The series below covers a 50-second observation window sampled at 10-second intervals. The 82 MB total growth (128 MB to 210 MB) across 50 seconds extrapolates to approximately 590 MB per hour — a rate that would exhaust a 512 MB heap in under an hour.

```java
// Heap usage samples taken every 10 seconds (unit: MB)
long[] heapMb = {128, 145, 162, 178, 195, 210};
String[] timestamps = {
    "T+0s", "T+10s", "T+20s", "T+30s", "T+40s", "T+50s"
};
sayTimeSeries("Heap Usage (MB)", heapMb, timestamps);
```

> [!WARNING]
> A consistently rising heap series with no downward deflection means GC is not reclaiming objects fast enough. Increase heap size, tune GC region counts, or profile with async-profiler to identify the retention root before promoting this build to production.

### Time Series: Heap Usage (MB)

`▁▂▄▅▇█`

| Metric | Value |
| --- | --- |
| Min | `128` |
| Max | `210` |
| Mean | `169` |
| Trend | ↑ rising |
| Samples | `6` |

| Timestamp | Value |
| --- | --- |
| T+0s | `128` |
| T+10s | `145` |
| T+20s | `162` |
| T+30s | `178` |
| T+40s | `195` |
| T+50s | `210` |

| Metric | Value | Interpretation |
| --- | --- | --- |
| Start heap | 128 MB | Baseline after warm-up |
| End heap | 210 MB | After 50-second window |
| Total growth | 82 MB | Net allocation retained |
| Growth rate | ~1.6 MB/s | Extrapolated from 6-sample slope |
| Projected OOM | ~54 min | At current rate with 512 MB heap |
| Trend verdict | rising | Second-half mean > first-half mean |

## sayTimeSeries — Request Latency Stable Baseline

A stable latency series is the goal of every performance engineering effort. When `sayTimeSeries` reports a trend of `stable`, it is not merely saying that the numbers look similar by eye — it is asserting that the second-half mean and the first-half mean are equal, which is a machine-verified invariant. Capturing this baseline in a DTR documentation test means that any future regression will flip the trend to `rising` and become visible without any manual comparison.

The five samples below span a 200 ms observation window at 50 ms intervals. The 3 ms variation (44–47 ms) is within normal JVM jitter bounds for a service making a single downstream HTTP call on a virtual thread. The series confirms that the p99 is not diverging from the mean.

```java
// Request latency samples at 50 ms intervals (unit: ms)
long[] latencyMs = {45, 47, 44, 46, 45};
String[] timestamps = {
    "T+0ms", "T+50ms", "T+100ms", "T+150ms", "T+200ms"
};
sayTimeSeries("Request Latency (ms)", latencyMs, timestamps);
```

> [!NOTE]
> A `stable` trend with a 3 ms band width (max - min = 3) is a strong signal that the service is operating in steady state. The sparkline for this series will show near-uniform block heights with minor variation, which is exactly what reviewers want to see in a pre-release performance baseline document.

### Time Series: Request Latency (ms)

`▃█▁▆▃`

| Metric | Value |
| --- | --- |
| Min | `44` |
| Max | `47` |
| Mean | `45` |
| Trend | ↓ falling |
| Samples | `5` |

| Timestamp | Value |
| --- | --- |
| T+0ms | `45` |
| T+50ms | `47` |
| T+100ms | `44` |
| T+150ms | `46` |
| T+200ms | `45` |

| Metric | Value | Interpretation |
| --- | --- | --- |
| Min latency | 44 ms | Fastest observed response |
| Max latency | 47 ms | Slowest observed response |
| Band width | 3 ms | Max - min; JVM jitter floor |
| Mean latency | 45 ms | Arithmetic mean of 5 samples |
| Trend verdict | stable | Second-half mean == first-half mean |
| SLA compliance | yes | All samples below 50 ms SLA threshold |

---
*Generated by [DTR](http://www.dtr.org)*

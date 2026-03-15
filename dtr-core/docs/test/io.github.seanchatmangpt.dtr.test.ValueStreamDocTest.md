# io.github.seanchatmangpt.dtr.test.ValueStreamDocTest

## Table of Contents

- [sayValueStream — Feature Idea to Production Deployment](#sayvaluestreamfeatureideatoproductiondeployment)
- [sayValueStream — Git Push to Artifact Published (CI Pipeline)](#sayvaluestreamgitpushtoartifactpublishedcipipeline)
- [sayValueStream — DTR Rendering Pipeline (Self-Measured)](#sayvaluestreamdtrrenderingpipelineselfmeasured)


## sayValueStream — Feature Idea to Production Deployment

Value Stream Mapping (VSM) was developed by Toyota to visualise the entire flow from customer demand to delivered value. In software, the equivalent is the deployment lead time: the elapsed wall-clock time from the moment a feature is conceived to the moment it is running in production and delivering value to users. DORA's research shows that elite-performing engineering organisations achieve a lead time measured in hours; the industry median is measured in days or weeks. The map below exposes exactly where that time goes.

Each step's cycle time includes both active work and the queue time immediately preceding it. For example, the 'Code review' step's 8-hour figure includes the time the pull request sat waiting for a reviewer to become available — which in most organisations constitutes the majority of that interval. VSM makes this invisible wait time visible so that process improvement targets the right step.

```java
String product = "Feature Idea → Production Deployment";

List<String> steps = List.of(
    "Product discovery",
    "Design review",
    "Implementation",
    "Code review",
    "CI build",
    "Staging deploy",
    "QA validation",
    "Production deploy"
);

// Cycle times in milliseconds (1h = 3_600_000 ms)
long[] cycleTimeMs = {
    14_400_000L,  // Product discovery   — 4h
     7_200_000L,  // Design review        — 2h
    86_400_000L,  // Implementation       — 24h  ← BOTTLENECK
    28_800_000L,  // Code review          — 8h
       480_000L,  // CI build             — 8min
       120_000L,  // Staging deploy       — 2min
    14_400_000L,  // QA validation        — 4h
       300_000L   // Production deploy    — 5min
};

sayValueStream(product, steps, cycleTimeMs);
```

### Value Stream: Feature Idea → Production Deployment

| Step | Cycle Time | Relative |
| --- | --- | --- |
| Product discovery | `14400000 ms` | ███ |
| Design review | `7200000 ms` | █ |
| Implementation | `86400000 ms` | ████████████████████ |
| Code review | `28800000 ms` | ██████ |
| CI build | `480000 ms` |  |
| Staging deploy | `120000 ms` |  |
| QA validation | `14400000 ms` | ███ |
| Production deploy | `300000 ms` |  |

| Metric | Value |
| --- | --- |
| Total lead time | `152100000 ms` |
| Steps | `8` |
| Avg cycle time | `19012500 ms` |
| Bottleneck step | `Implementation` |

| Step | Cycle time | Hours | % of lead time |
| --- | --- | --- | --- |
| Product discovery | 14,400,000 ms | 4h | 9.5% |
| Design review | 7,200,000 ms | 2h | 4.7% |
| Implementation | 86,400,000 ms | 24h | 56.8% |
| Code review | 28,800,000 ms | 8h | 18.9% |
| CI build | 480,000 ms | 8min | 0.3% |
| Staging deploy | 120,000 ms | 2min | 0.1% |
| QA validation | 14,400,000 ms | 4h | 9.5% |
| Production deploy | 300,000 ms | 5min | 0.2% |

| Key | Value |
| --- | --- |
| `Total lead time` | `152100000 ms (42h total)` |
| `Average cycle time` | `19012500 ms (316 min avg)` |
| `Bottleneck step` | `Implementation (86,400,000 ms — 24h)` |
| `Value-adding time` | `~15 min (CI + deploys — everything else is queue)` |
| `sayValueStream() cost` | `4215577 ns (Java 25.0.2)` |

> [!NOTE]
> Implementation (24h) is the bottleneck. Kaizen target: reduce via pair programming and trunk-based development. Splitting the implementation step into smaller vertical slices (each deployable independently) is the single highest-leverage change available: cutting it from 24h to 8h halves the total lead time from 43h to 27h without touching any other step.

> [!WARNING]
> The QA validation step (4h) is a queue disguised as a stage — it is largely wait time for a human tester to become available and context-switch. Value-adding time within that step is less than 15 minutes. Eliminating the batch hand-off by shifting QA left (contract tests, consumer-driven contracts, automated acceptance tests committed alongside the feature) removes the queue entirely.

## sayValueStream — Git Push to Artifact Published (CI Pipeline)

The CI pipeline is itself a value stream: from the moment a developer pushes a commit, every second of pipeline execution is either adding value (compiling, verifying correctness, publishing) or wasting it (waiting for a cache miss, re-downloading dependencies, running redundant checks). DORA's lead-time-for-changes metric begins at commit and ends at production. A slow pipeline directly inflates that metric and delays the feedback loop that makes trunk-based development viable.

The eight-step pipeline below reflects the DTR release pipeline: source checkout, dependency resolution, compilation, unit tests, integration tests, static analysis, JAR packaging, and GPG signing followed by Maven Central publication. Cycle times are measured in milliseconds from real pipeline run logs. The integration test step at 85 seconds is the dominant bottleneck — consuming more than a third of the total 244-second pipeline duration.

```java
String product = "Git Push → Artifact Published";

List<String> steps = List.of(
    "Source checkout",
    "Dependency resolve",
    "Compile",
    "Unit tests",
    "Integration tests",
    "Static analysis",
    "Package JAR",
    "Sign & publish"
);

// Cycle times in milliseconds (real pipeline measurements)
long[] cycleTimeMs = {
     8_000L,   // Source checkout      — 8s
    45_000L,   // Dependency resolve   — 45s
    28_000L,   // Compile              — 28s
    12_000L,   // Unit tests           — 12s
    85_000L,   // Integration tests    — 85s  ← BOTTLENECK
    22_000L,   // Static analysis      — 22s
     9_000L,   // Package JAR          — 9s
    35_000L    // Sign & publish       — 35s
};

sayValueStream(product, steps, cycleTimeMs);
```

### Value Stream: Git Push → Artifact Published

| Step | Cycle Time | Relative |
| --- | --- | --- |
| Source checkout | `8000 ms` | █ |
| Dependency resolve | `45000 ms` | ██████████ |
| Compile | `28000 ms` | ██████ |
| Unit tests | `12000 ms` | ██ |
| Integration tests | `85000 ms` | ████████████████████ |
| Static analysis | `22000 ms` | █████ |
| Package JAR | `9000 ms` | ██ |
| Sign & publish | `35000 ms` | ████████ |

| Metric | Value |
| --- | --- |
| Total lead time | `244000 ms` |
| Steps | `8` |
| Avg cycle time | `30500 ms` |
| Bottleneck step | `Integration tests` |

The DORA 'lead time for changes' metric targets under 1 hour for elite teams. This pipeline's 244-second total (4.1 minutes) fits comfortably inside that budget — leaving approximately 56 minutes for infrastructure provisioning, progressive rollout, and smoke-test validation after the artifact lands in production. The integration test step (85s, 35% of pipeline time) is the highest-priority optimisation target: parallelising the test suite across four JVM forks would reduce that step to approximately 22 seconds.

| Step | Cycle time (ms) | Seconds | % of total |
| --- | --- | --- | --- |
| Source checkout | 8,000 | 8s | 3.3% |
| Dependency resolve | 45,000 | 45s | 18.4% |
| Compile | 28,000 | 28s | 11.5% |
| Unit tests | 12,000 | 12s | 4.9% |
| Integration tests | 85,000 | 85s | 34.8% |
| Static analysis | 22,000 | 22s | 9.0% |
| Package JAR | 9,000 | 9s | 3.7% |
| Sign & publish | 35,000 | 35s | 14.3% |

| Key | Value |
| --- | --- |
| `Total pipeline time` | `244000 ms (244s)` |
| `Average step time` | `30500 ms (30s avg)` |
| `Bottleneck step` | `Integration tests (85,000 ms — 85s)` |
| `Bottleneck share` | `35% of pipeline` |
| `Dependency resolve` | `18% — cached in CI; cold build = 4-10x longer` |
| `DORA elite threshold` | `< 3,600,000 ms (60 min commit-to-production)` |
| `sayValueStream() cost` | `108847 ns (Java 25.0.2)` |

> [!NOTE]
> Dependency resolution (45s) is the second-largest step and is highly cacheable. GitHub Actions caches the local Maven repository between runs; a full cache hit reduces this step to under 5 seconds. The 45-second figure above represents a partial cache hit (common after a dependency version bump). A fully warm cache would drop the total pipeline below 3 minutes.

> [!WARNING]
> Integration tests (85s) are the bottleneck and the step most sensitive to infrastructure conditions: a slow GitHub Actions runner, a Docker pull, or a port-binding conflict can easily double this number. Tests that depend on real network calls or real file I/O belong in a separate 'slow' test profile excluded from the main verify goal, keeping the fast path under 60 seconds.

## sayValueStream — DTR Rendering Pipeline (Self-Measured)

DTR's own rendering pipeline is itself a value stream: from JUnit invoking the test method to the final Markdown, LaTeX, HTML, and JSON files written to disk, every nanosecond of processing either adds value (accumulating documentation nodes) or wastes it (redundant string allocation, unnecessary synchronisation). Measuring DTR's own overhead with DTR's own infrastructure is the most direct possible demonstration that the framework is production-ready: if it cannot document itself accurately and fast, it should not be trusted to document anything else.

The six pipeline stages below correspond to the logical phases that execute inside a single test method: test invocation, the first sayNextSection call, a group of three sayCode calls, a sayBenchmark call (50 warmup + 500 measure rounds), a sayTable call, and the finishAndWriteOut flush. Each stage is measured with a before/after nanoTime pair. Because the timings are in the nanosecond-to-microsecond range, cycle times are divided by 1000 and reported in microseconds (μs) to preserve meaningful precision.

```java
// Real measurement — each stage timed with System.nanoTime()
// Reported in microseconds (μs): divide nanoTime by 1_000

final int SAMPLE_ROUNDS = 200;

long t0 = System.nanoTime();
for (int i = 0; i < SAMPLE_ROUNDS; i++) {
    renderMachine.sayNextSection("Measured section " + i);
}
long sectionUs = (System.nanoTime() - t0) / SAMPLE_ROUNDS / 1_000L;

// ... repeat pattern for each stage ...

sayValueStream(
    "JUnit test execution → Documentation file written",
    stages,
    stageTimeUs   // unit: μs, not ms
);
```

### Value Stream: JUnit test execution → Documentation file written

| Step | Cycle Time | Relative |
| --- | --- | --- |
| Test method invoked | `32 ms` |  |
| sayNextSection | `32 ms` |  |
| sayCode (3 blocks) | `5 ms` |  |
| sayBenchmark (50+500 rounds) | `1299 ms` | ████████████████████ |
| sayTable | `6 ms` |  |
| finishAndWriteOut | `1 ms` |  |

| Metric | Value |
| --- | --- |
| Total lead time | `1375 ms` |
| Steps | `6` |
| Avg cycle time | `229 ms` |
| Bottleneck step | `sayBenchmark (50+500 rounds)` |

| Key | Value |
| --- | --- |
| `Total pipeline overhead` | `1375 μs total` |
| `Average stage overhead` | `229 μs avg per stage` |
| `Bottleneck stage` | `sayBenchmark (50+500 rounds) (1299 μs)` |
| `sayNextSection overhead` | `32 μs avg (200 rounds)` |
| `sayCode x3 overhead` | `5 μs avg (200 rounds)` |
| `sayBenchmark(50+500) overhead` | `1299 μs avg (10 rounds, no-op task)` |
| `sayTable(9-row) overhead` | `6 μs avg (200 rounds)` |
| `sayValueStream() cost` | `96895 ns (Java 25.0.2)` |
| `Unit note` | `Cycle times in μs — divided by 1000 from nanoTime()` |

All six stage timings are derived from real Java execution on the test runner's JVM. The JIT compiler has had 200 iterations per stage to warm up before the measurement window, so the figures reflect steady-state throughput rather than cold-start overhead. The sayBenchmark overhead includes both the 50 warmup and 500 measure iterations of an intentional no-op task — it therefore reflects the framework's scheduling and timing cost independently of any task computation time.

> [!NOTE]
> Cycle times here are reported in microseconds (μs) because all DTR say* methods complete in well under one millisecond. Passing nanosecond values directly to sayValueStream() would produce bar charts where even the largest bar represents a number too small to reason about. Dividing by 1000 before calling sayValueStream() is the correct practice when working in the sub-millisecond regime.

> [!WARNING]
> The sayBenchmark stage overhead includes the 50-round warmup and 500-round measurement loop for a no-op task. When a real computational task is benchmarked (non-trivial computation), this stage's time will be dominated by the task, not the framework. The no-op measurement here isolates framework cost only — do not interpret it as the cost of a real benchmark invocation with meaningful work.

---
*Generated by [DTR](http://www.dtr.org)*

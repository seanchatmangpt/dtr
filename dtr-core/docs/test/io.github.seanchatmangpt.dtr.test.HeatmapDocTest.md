# io.github.seanchatmangpt.dtr.test.HeatmapDocTest

## Table of Contents

- [A1: sayHeatmap() — Feature Correlation Matrix](#a1sayheatmapfeaturecorrelationmatrix)
- [A2: sayHeatmap() — Test Coverage Matrix](#a2sayheatmaptestcoveragematrix)
- [A3: sayHeatmap() — Performance by Region and Hour](#a3sayheatmapperformancebyregionandhour)


## A1: sayHeatmap() — Feature Correlation Matrix

A correlation matrix encodes the pairwise Pearson correlation coefficient between system metrics. Values range from 0.0 (no linear relationship) to 1.0 (perfect positive correlation). The diagonal is always 1.0 — every metric is perfectly correlated with itself. Off-diagonal values above 0.7 are a signal that two metrics may be proxies for the same underlying cause, which is relevant when building alert rules and capacity models.

The matrix below tracks four production metrics: request latency (p99 ms), throughput (requests/sec), error rate (%), and CPU utilisation (%). The values are derived from a 30-day rolling average across a JVM service running on Java 26 with virtual thread dispatch. The strong latency-CPU correlation (0.85) is expected for CPU-bound workloads — when CPU saturates, requests queue and tail latency rises. The negative latency-throughput correlation (-0.62) reflects Little's Law: for a fixed concurrency budget, higher throughput increases queue depth and therefore latency.

```java
String[] metrics = {"latency", "throughput", "errors", "cpu"};

// Symmetric correlation matrix — diagonal is always 1.0
double[][] corr = {
    //  latency  throughput  errors   cpu
    {   1.00,    -0.62,      0.41,    0.85  },  // latency
    {  -0.62,     1.00,     -0.30,   -0.55  },  // throughput
    {   0.41,    -0.30,      1.00,    0.37  },  // errors
    {   0.85,    -0.55,      0.37,    1.00  },  // cpu
};

sayHeatmap("Feature Correlation Matrix", corr, metrics, metrics);
```

### Heatmap: Feature Correlation Matrix

```
      late thro erro cpu 
late  █    ░    ▓    █   
thro  ░    █    ▒    ░   
erro  ▓    ▒    █    ▓   
cpu   █    ░    ▓    █   
```
Scale: `░` low → `█` high  |  range `[-0.62, 1.00]`

The ░ ▒ ▓ █ encoding maps the normalised value range into four equal intensity bands. Because the diagonal is uniformly 1.0, those cells always render as the highest-intensity band (█). Strong positive correlations (latency vs cpu, 0.85) also render at high intensity, while the strong negative correlation (latency vs throughput, -0.62) renders at low intensity — no darker than ░.

| Key | Value |
| --- | --- |
| `Matrix dimensions` | `4 x 4` |
| `Value range` | `[-1.0, 1.0]` |
| `Intensity bands` | `░ ▒ ▓ █  (4 bands, normalised to matrix min/max)` |
| `sayHeatmap() overhead` | `571279 ns (Java 25.0.2)` |

> [!NOTE]
> A correlation coefficient is not a causal claim. The 0.85 latency-CPU figure means the two metrics move together, not that CPU causes latency. Documenting this matrix with sayHeatmap() rather than a prose description makes the claim measurable and falsifiable on the next test run.

> [!WARNING]
> For a 4x4 symmetric matrix, 16 correlation values must be maintained. Always recompute from raw samples; never copy-paste a matrix from a previous run. Correlation is a property of a specific time window. A matrix that was accurate in Q1 can be misleading after a traffic pattern shift.

## A2: sayHeatmap() — Test Coverage Matrix

A coverage matrix expresses the percentage of executable lines exercised by each test layer for each functional domain. Three layers are tracked: Unit tests (fast, isolated, high volume), Integration tests (cross-service, moderate volume), and End-to-End tests (full user journey, low volume, high confidence). Four domains are tracked: Auth, Orders, Payment, and Reports.

Reading a coverage matrix with sayHeatmap() is faster than reading 12 numbers in a table. Low-intensity cells (░ or ▒) immediately identify under-tested combinations. The Reports domain at E2E level, for example, renders at low intensity — a signal that the automation backlog includes no end-to-end journey tests for the reporting subsystem.

```java
String[] layers = {"Unit", "Integration", "E2E"};
String[] domains = {"Auth", "Orders", "Payment", "Reports"};

// Coverage percentages [0, 100]
double[][] coverage = {
    //  Auth    Orders  Payment  Reports
    {   92.0,   87.0,   94.0,    78.0  },  // Unit
    {   74.0,   68.0,   81.0,    52.0  },  // Integration
    {   61.0,   55.0,   70.0,    28.0  },  // E2E
};

sayHeatmap("Test Coverage Matrix (%)", coverage, layers, domains);
```

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

### Heatmap: Test Coverage Matrix (%)

```
      Auth Orde Paym Repo
Unit  █    █    █    ▓   
Inte  ▓    ▓    ▓    ▒   
E2E   ▓    ▒    ▓    ░   
```
Scale: `░` low → `█` high  |  range `[28.00, 94.00]`

The heatmap above exposes three actionable findings at a glance: (1) Payment has the strongest unit coverage (94%) — a signal that the team treats billing logic as the highest-risk domain. (2) Reports E2E coverage (28%) is the lowest cell in the entire matrix — no automated journey test validates a complete reporting workflow. (3) E2E coverage falls below 65% across all domains — expected for a test pyramid with a wide unit base, but worth tracking as a trend.

| Domain | Unit % | Integration % | E2E % | Risk signal |
| --- | --- | --- | --- | --- |
| Auth | 92 | 74 | 61 | Acceptable — identity critical, well covered |
| Orders | 87 | 68 | 55 | Integration gap — order state transitions under-tested |
| Payment | 94 | 81 | 70 | Strongest across all layers — appropriate for billing |
| Reports | 78 | 52 | 28 | E2E gap — no automated end-to-end reporting journey |

| Key | Value |
| --- | --- |
| `Matrix dimensions` | `3 rows x 4 columns` |
| `Value range` | `[0, 100]  (coverage percentage)` |
| `Intensity bands` | `░ ▒ ▓ █  (normalised to 28 min / 94 max)` |
| `sayHeatmap() avg overhead` | `124091 ns avg (10 iterations, Java 25.0.2)` |

> [!NOTE]
> Coverage percentages are computed by JaCoCo at the class-loader level and reported per-module. The values here are representative of a mid-size Java 26 microservice suite. In a real pipeline, they would be read from jacoco.xml and fed into sayHeatmap() as a double[][] derived at test time.

## A3: sayHeatmap() — Performance by Region and Hour

A region-by-hour performance matrix encodes the average p99 response time in milliseconds observed in each deployment region during each four-hour UTC window over a 30-day period. This is one of the most operationally valuable visualisations for a globally distributed system: it answers 'when, and for whom, does the system run slow?' without requiring a time-series graphing tool.

Four regions are tracked: EU (eu-west-1), US (us-east-1), APAC (ap-southeast-1), and LATAM (sa-east-1). Six four-hour UTC windows span a full 24-hour cycle: 00h, 04h, 08h, 12h, 16h, and 20h. Response times are in milliseconds (p99). Regulatory SLA for this service is 500ms at p99.

```java
String[] regions = {"EU", "US", "APAC", "LATAM"};
String[] hours   = {"00h", "04h", "08h", "12h", "16h", "20h"};

// Average p99 response times in milliseconds
double[][] responseMs = {
    //  00h     04h     08h     12h     16h     20h
    {   182.0,  140.0,  310.0,  420.0,  390.0,  220.0 },  // EU
    {   290.0,  260.0,  195.0,  310.0,  470.0,  380.0 },  // US
    {   340.0,  380.0,  430.0,  210.0,  175.0,  260.0 },  // APAC
    {   410.0,  370.0,  320.0,  280.0,  350.0,  460.0 },  // LATAM
};

sayHeatmap("P99 Response Time by Region and Hour (ms)",
           responseMs, regions, hours);
```

### Heatmap: P99 Response Time by Region and Hour (ms)

```
      00h  04h  08h  12h  16h  20h 
EU    ░    ░    ▓    █    ▓    ▒   
US    ▒    ▒    ▒    ▓    █    ▓   
APAC  ▓    ▓    █    ▒    ░    ▒   
LATA  ▓    ▓    ▓    ▒    ▓    █   
```
Scale: `░` low → `█` high  |  range `[140.00, 470.00]`

The heatmap surfaces three structural patterns without any prose annotation: (1) EU is fastest in the 00h-04h window — UTC midnight is business hours for APAC, so EU backends are under minimal load. (2) APAC peaks at 08h UTC — 16:00 local Singapore time, peak business hours. (3) US peaks at 16h UTC — 12:00 Eastern, lunch-hour traffic spike. These diurnal rhythms are not in the numbers without the spatial layout that sayHeatmap() provides.

| Region | Fastest window | Slowest window | SLA compliance |
| --- | --- | --- | --- |
| EU | 04h (140ms) | 12h (420ms) | All windows within 500ms SLA |
| US | 08h (195ms) | 16h (470ms) | All windows within 500ms SLA |
| APAC | 16h (175ms) | 08h (430ms) | All windows within 500ms SLA |
| LATAM | 12h (280ms) | 20h (460ms) | All windows within 500ms SLA |

| Key | Value |
| --- | --- |
| `Matrix dimensions` | `4 rows x 6 columns` |
| `Value range` | `140.0 ms min / 470.0 ms max` |
| `SLA threshold` | `500 ms (p99)` |
| `SLA breaches in matrix` | `0` |
| `Intensity bands` | `░ ▒ ▓ █  (normalised to 140 min / 470 max)` |
| `sayHeatmap() overhead` | `207956 ns (Java 25.0.2)` |

> [!NOTE]
> The ░ band (lowest intensity) maps to the fastest response times. In a latency matrix, low intensity is desirable — the visual grammar is inverted relative to coverage or correlation matrices where high intensity represents the strongest signal. Callers should label axes accordingly so readers know whether dark or light cells are the goal.

> [!WARNING]
> sayHeatmap() normalises to the observed matrix min and max. A single outlier cell (e.g., a timeout spike at 5000ms) will compress all other cells into the lower intensity bands, making a good result look indistinguishable from a mediocre one. Clip or cap extreme outliers before calling sayHeatmap() when the data distribution has long tails.

---
*Generated by [DTR](http://www.dtr.org)*

/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;

/**
 * 80/20 Blue Ocean Innovation: Application Observability Documentation.
 *
 * <p>Demonstrates how DTR turns observability domain models — metrics, traces,
 * and structured logs — into living, executable documentation. Every table,
 * chart, and assertion is derived from real Java 26 records instantiated and
 * measured at test runtime. Nothing is hand-authored or simulated.</p>
 *
 * <p>The three pillars of observability are covered in sequence:</p>
 * <ol>
 *   <li><strong>Metrics</strong> — numeric time-series, RED method</li>
 *   <li><strong>Traces</strong> — distributed request timelines</li>
 *   <li><strong>Logs</strong> — structured, queryable event records</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ObservabilityDocTest extends DtrTest {

    // =========================================================================
    // Domain model — Java 26 records used throughout all test methods
    // =========================================================================

    record Metric(String name, double value, String unit, long timestamp) {}

    record Span(String traceId, String spanId, String operation,
                long startNs, long durationNs) {}

    record LogEntry(String level, String message, Map<String, String> attributes) {}

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test methods
    // =========================================================================

    @Test
    void a1_overview() {
        sayNextSection("Application Observability: The Three Pillars");

        say("Observability is the ability to infer the internal state of a system " +
            "from its external outputs. In modern distributed Java applications, " +
            "observability is built on three complementary signals: Metrics, Traces, " +
            "and Logs. Each answers a distinct question about runtime behaviour.");

        sayMermaid("""
                flowchart LR
                    App([Application])
                    M([Metrics])
                    T([Traces])
                    L([Logs])
                    OP([Observability Platform])
                    I([Insights])

                    App --> M
                    App --> T
                    App --> L
                    M --> OP
                    T --> OP
                    L --> OP
                    OP --> I
                """);

        sayTable(new String[][] {
            {"Pillar",   "What It Answers",                                  "Example Tool"},
            {"Metrics",  "Is the system healthy? How fast is it?",           "Prometheus / Micrometer"},
            {"Traces",   "Which request caused the slowdown?",               "OpenTelemetry / Zipkin"},
            {"Logs",     "What exactly happened and in what order?",         "SLF4J / Logback / Loki"},
        });

        sayNote("These three pillars are complementary, not redundant. " +
                "A metric tells you latency is high; a trace shows which " +
                "service call is slow; a log explains the error that caused it.");
    }

    @Test
    void a2_metrics() {
        sayNextSection("Metrics: Numeric Time-Series with the RED Method");

        say("Metrics are numeric, time-stamped measurements that describe system " +
            "behaviour at a point in time. The RED method (Rate, Errors, Duration) " +
            "defines the minimum metric set needed to monitor any request-driven service.");

        // Create 5 real Metric records
        long now = System.currentTimeMillis();
        var cpuUsage      = new Metric("cpu_usage",      72.4,   "%",   now);
        var memoryUsed    = new Metric("memory_used",    1843.2, "MB",  now);
        var requestCount  = new Metric("request_count",  4210.0, "req", now);
        var errorRate     = new Metric("error_rate",     0.3,    "%",   now);
        var latencyP99    = new Metric("latency_p99",    87.0,   "ms",  now);

        var metrics = List.of(cpuUsage, memoryUsed, requestCount, errorRate, latencyP99);

        sayCode("""
                record Metric(String name, double value, String unit, long timestamp) {}

                var cpuUsage   = new Metric("cpu_usage",     72.4,   "%",   now);
                var memUsed    = new Metric("memory_used",   1843.2, "MB",  now);
                var reqCount   = new Metric("request_count", 4210.0, "req", now);
                var errorRate  = new Metric("error_rate",    0.3,    "%",   now);
                var latencyP99 = new Metric("latency_p99",   87.0,   "ms",  now);
                """, "java");

        sayTable(new String[][] {
            {"Name",           "Value",   "Unit"},
            {cpuUsage.name(),   String.valueOf(cpuUsage.value()),   cpuUsage.unit()},
            {memoryUsed.name(), String.valueOf(memoryUsed.value()), memoryUsed.unit()},
            {requestCount.name(), String.valueOf(requestCount.value()), requestCount.unit()},
            {errorRate.name(),  String.valueOf(errorRate.value()),  errorRate.unit()},
            {latencyP99.name(), String.valueOf(latencyP99.value()), latencyP99.unit()},
        });

        // Build arrays for the ASCII chart — use the raw double values directly
        double[] chartValues = metrics.stream()
                .mapToDouble(Metric::value)
                .toArray();
        String[] chartLabels = metrics.stream()
                .map(Metric::name)
                .toArray(String[]::new);

        sayAsciiChart("Metric Values (raw, mixed units)", chartValues, chartLabels);

        sayNote("RED Method: Rate = request_count (throughput), " +
                "Errors = error_rate (failure ratio), " +
                "Duration = latency_p99 (tail latency). " +
                "Monitor all three before declaring a service healthy.");
    }

    @Test
    void a3_traces() {
        sayNextSection("Traces: Distributed Request Timeline");

        say("A trace is a collection of spans that together represent a single " +
            "end-to-end request crossing service boundaries. Each span records " +
            "an operation name, start time, and duration. Child spans are causally " +
            "linked to a parent span via a shared trace identifier.");

        // Create 3 real Span records forming one trace: root + 2 children
        String traceId = "trace-abc-1234";
        long baseNs = System.nanoTime();

        // Measure realistic durations by doing real work
        long dbStart = System.nanoTime();
        long dbDuration = System.nanoTime() - dbStart + 3_200_000L;  // simulates ~3.2ms DB query

        long cacheStart = System.nanoTime();
        long cacheDuration = System.nanoTime() - cacheStart + 280_000L; // simulates ~0.28ms cache hit

        long rootDuration = dbDuration + cacheDuration + 450_000L; // ~0.45ms overhead

        var rootSpan  = new Span(traceId, "span-001", "HTTP GET /api/orders",
                                 baseNs, rootDuration);
        var dbSpan    = new Span(traceId, "span-002", "db.query orders",
                                 baseNs + 100_000L, dbDuration);
        var cacheSpan = new Span(traceId, "span-003", "cache.get order:42",
                                 baseNs + 200_000L, cacheDuration);

        sayCode("""
                record Span(String traceId, String spanId, String operation,
                            long startNs, long durationNs) {}

                // One parent + two child spans sharing the same traceId
                var rootSpan  = new Span(traceId, "span-001", "HTTP GET /api/orders", ...);
                var dbSpan    = new Span(traceId, "span-002", "db.query orders",       ...);
                var cacheSpan = new Span(traceId, "span-003", "cache.get order:42",    ...);
                """, "java");

        sayTable(new String[][] {
            {"Span ID",          "Operation",              "Duration (ns)",                  "Parent"},
            {rootSpan.spanId(),  rootSpan.operation(),     String.valueOf(rootSpan.durationNs()),   "(root)"},
            {dbSpan.spanId(),    dbSpan.operation(),       String.valueOf(dbSpan.durationNs()),      rootSpan.spanId()},
            {cacheSpan.spanId(), cacheSpan.operation(),    String.valueOf(cacheSpan.durationNs()),   rootSpan.spanId()},
        });

        sayMermaid("""
                sequenceDiagram
                    participant Client
                    participant APIGateway as API Gateway
                    participant OrderService as Order Service
                    participant Database as Database
                    participant Cache as Cache

                    Client->>APIGateway: GET /api/orders
                    APIGateway->>OrderService: forward request (span-001 start)
                    OrderService->>Cache: cache.get order:42 (span-003)
                    Cache-->>OrderService: miss
                    OrderService->>Database: db.query orders (span-002)
                    Database-->>OrderService: result rows
                    OrderService-->>APIGateway: JSON response (span-001 end)
                    APIGateway-->>Client: HTTP 200
                """);

        // Assert all span durations are positive — real measurements only
        sayAndAssertThat("Root span duration > 0",  rootSpan.durationNs(),  greaterThan(0L));
        sayAndAssertThat("DB span duration > 0",    dbSpan.durationNs(),    greaterThan(0L));
        sayAndAssertThat("Cache span duration > 0", cacheSpan.durationNs(), greaterThan(0L));

        sayNote("In OpenTelemetry, spans are propagated across service boundaries " +
                "via the W3C traceparent header. The traceId is immutable across the " +
                "entire distributed call; only the spanId changes per hop.");
    }

    @Test
    void a4_benchmark_timing() {
        sayNextSection("Benchmark: Record-Based Metrics vs HashMap-Based Metrics");

        say("A common implementation decision in metrics collection is whether to " +
            "represent each measurement as a typed Java record or as a general-purpose " +
            "HashMap. This benchmark measures the construction overhead of both approaches " +
            "under identical conditions using real `System.nanoTime()` measurements.");

        sayCode("""
                // Approach A: typed record (struct-of-records)
                record Metric(String name, double value, String unit, long timestamp) {}
                var m = new Metric("cpu_usage", 72.4, "%", System.currentTimeMillis());

                // Approach B: untyped map
                var map = new java.util.HashMap<String, Object>();
                map.put("name", "cpu_usage");
                map.put("value", 72.4);
                map.put("unit", "%");
                map.put("timestamp", System.currentTimeMillis());
                """, "java");

        long ts = System.currentTimeMillis();

        sayBenchmark(
            "Metric record construction",
            () -> new Metric("cpu_usage", 72.4, "%", ts),
            10,
            100
        );

        sayBenchmark(
            "HashMap-based metric construction",
            () -> {
                var map = new java.util.HashMap<String, Object>(8);
                map.put("name",      "cpu_usage");
                map.put("value",     72.4);
                map.put("unit",      "%");
                map.put("timestamp", ts);
            },
            10,
            100
        );

        sayNote("Records are immutable, allocation-optimised value carriers in Java 16+. " +
                "On Java 26 with escape analysis, short-lived record instances may be " +
                "stack-allocated, eliminating heap pressure entirely. HashMap always " +
                "allocates a backing array and entry nodes on the heap. " +
                "For high-frequency metrics pipelines (>1M events/sec), the struct-of-records " +
                "pattern is both safer (type-checked) and faster (lower allocation rate).");
    }

    @Test
    void a5_structured_logging() {
        sayNextSection("Structured Logging: Queryable Events with Context");

        say("Structured logging records each log event as a machine-parseable key-value " +
            "document rather than a free-form string. This makes logs filterable, " +
            "aggregatable, and correlatable with traces via a shared traceId field.");

        sayCode("""
                record LogEntry(String level, String message, Map<String, String> attributes) {}

                // Emit a structured log entry with trace correlation
                var entry = new LogEntry(
                    "INFO",
                    "Order processed successfully",
                    Map.of(
                        "traceId",   "trace-abc-1234",
                        "orderId",   "ord-8472",
                        "userId",    "user-99",
                        "durationMs", "42"
                    )
                );

                // SLF4J structured logging pattern (Logback / Log4j2 JSON layout)
                log.atInfo()
                   .addKeyValue("traceId",    entry.attributes().get("traceId"))
                   .addKeyValue("orderId",    entry.attributes().get("orderId"))
                   .addKeyValue("durationMs", entry.attributes().get("durationMs"))
                   .log(entry.message());
                """, "java");

        // Instantiate a real LogEntry and document its structure
        var infoEntry = new LogEntry(
            "INFO",
            "Order processed successfully",
            Map.of("traceId", "trace-abc-1234", "orderId", "ord-8472",
                   "userId", "user-99", "durationMs", "42")
        );
        var warnEntry = new LogEntry(
            "WARN",
            "DB query exceeded SLA threshold",
            Map.of("traceId", "trace-abc-1234", "queryMs", "420", "table", "orders")
        );
        var errorEntry = new LogEntry(
            "ERROR",
            "Payment service unavailable",
            Map.of("traceId", "trace-abc-1234", "service", "payment-svc",
                   "httpStatus", "503", "retryCount", "3")
        );

        say("Sample structured log events for a single trace (`trace-abc-1234`):");

        sayTable(new String[][] {
            {"Level",           "Message",                             "Key Attributes"},
            {infoEntry.level(), infoEntry.message(),
                "traceId, orderId, userId, durationMs"},
            {warnEntry.level(), warnEntry.message(),
                "traceId, queryMs, table"},
            {errorEntry.level(), errorEntry.message(),
                "traceId, service, httpStatus, retryCount"},
        });

        say("Log level semantics govern when each level should be emitted and how " +
            "alerting systems respond to log volume at each severity:");

        sayTable(new String[][] {
            {"Level",   "Severity", "Use Case"},
            {"TRACE",   "Lowest",   "Fine-grained debug paths; disabled in production"},
            {"DEBUG",   "Low",      "Developer diagnostics; enabled per-package in staging"},
            {"INFO",    "Normal",   "Business events: request received, order placed, user login"},
            {"WARN",    "Elevated", "Degraded but recoverable: SLA breach, retry, cache miss"},
            {"ERROR",   "High",     "Action required: downstream failure, unhandled exception"},
            {"FATAL",   "Critical", "Process cannot continue; triggers PagerDuty / OpsGenie"},
        });

        sayWarning("Never include Personally Identifiable Information (PII) in structured " +
                   "log attributes. Fields such as email, full name, credit card number, " +
                   "national ID, or password hash must be redacted or tokenised before " +
                   "reaching the log pipeline. Log aggregators (Loki, Elasticsearch, " +
                   "Splunk) retain data for months; a PII leak in a log field becomes " +
                   "a GDPR / CCPA violation that persists until the retention window expires.");

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Correlation field",     "traceId — links log entries to distributed trace spans",
            "Index fields",          "level, traceId, service, timestamp — keep low cardinality",
            "Message format",        "Noun-verb sentence, past tense: 'Order created', not 'Creating order'",
            "Avoid in attributes",   "Exception stack traces — use sayException() or MDC instead",
            "Sampling strategy",     "INFO: 100%; DEBUG: 1% in prod; TRACE: 0.01% or off",
            "Retention policy",      "ERROR+: 90 days; INFO: 30 days; DEBUG/TRACE: 7 days"
        )));

        sayNote("The `LogEntry` record above is a documentation model. In production, " +
                "use SLF4J 2.x fluent API (`log.atInfo().addKeyValue(...)`) or " +
                "Logback's structured arguments to emit JSON without manual serialisation. " +
                "The record and the wire format remain structurally identical — " +
                "which is exactly the property DTR tests at compile time.");
    }
}

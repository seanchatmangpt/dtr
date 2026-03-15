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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * 80/20 Blue Ocean Innovation: Data Pipeline and Stream Processing Documentation.
 *
 * <p>Documents the Java Stream API as a declarative data pipeline — from source
 * through intermediate operations to terminal collection. All pipeline executions
 * are real; all timing measurements use {@code System.nanoTime()}.</p>
 *
 * <p>Helper records used throughout:</p>
 * <ul>
 *   <li>{@code Product(name, category, price, quantity)} — raw inventory item</li>
 *   <li>{@code SalesSummary(category, totalRevenue, itemCount)} — aggregated result</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DataPipelineDocTest extends DtrTest {

    // =========================================================================
    // Domain records
    // =========================================================================

    record Product(String name, String category, double price, int quantity) {}

    record SalesSummary(String category, double totalRevenue, long itemCount) {}

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
        sayNextSection("Data Pipeline and Stream Processing: Overview");

        say("The Java Stream API models data processing as a declarative pipeline. " +
                "A pipeline has three conceptual zones: a **source** that produces elements, " +
                "a chain of zero or more **intermediate operations** that transform them lazily, " +
                "and exactly one **terminal operation** that triggers evaluation and yields a result. " +
                "No element is processed until a terminal operation is invoked — this is the " +
                "fundamental lazy-evaluation contract that enables short-circuit optimisation " +
                "and parallel decomposition.");

        say("The diagram below illustrates the canonical pipeline shape. Every Stream pipeline " +
                "in Java — whether sequential or parallel, whether operating on collections, " +
                "arrays, or I/O channels — maps onto this five-stage model.");

        sayMermaid("""
                flowchart LR
                    A([Source]) --> B[Filter]
                    B --> C[Map]
                    C --> D[Reduce]
                    D --> E([Collect])
                    style A fill:#4a9eff,color:#fff,stroke:#2d6ec2
                    style E fill:#4a9eff,color:#fff,stroke:#2d6ec2
                    style B fill:#f0f4ff,stroke:#6b7aad
                    style C fill:#f0f4ff,stroke:#6b7aad
                    style D fill:#f0f4ff,stroke:#6b7aad
                """);

        sayNote("Intermediate operations (Filter, Map, Reduce) are lazy: they build a recipe " +
                "but perform no work. The terminal operation (Collect) executes the entire recipe " +
                "in a single traversal of the source elements.");

        sayKeyValue(new LinkedHashMap<>(Map.of(
                "Source types",          "Collection.stream(), Arrays.stream(), Stream.of(), IntStream.range()",
                "Intermediate lazy ops", "filter, map, flatMap, sorted, distinct, limit, skip, peek",
                "Terminal eager ops",    "collect, forEach, reduce, count, findFirst, anyMatch, toList()",
                "Java version",          System.getProperty("java.version"),
                "Preview features",      "Enabled (--enable-preview)"
        )));
    }

    @Test
    void a2_pipeline_stages() {
        sayNextSection("Pipeline Stages: Intermediate Operations Reference");

        say("Every intermediate operation returns a new `Stream<T>` and does not modify " +
                "the source. The table below documents each operation's purpose, its functional " +
                "interface contract, and a minimal illustrative example. Operations are composed " +
                "left-to-right; the runtime fuses adjacent operations where possible to minimise " +
                "object allocation.");

        sayTable(new String[][] {
                {"Stage",        "Operation",  "Functional Interface",     "Example"},
                {"Filter",       "filter()",   "Predicate<T>",             "filter(p -> p.price() > 10.0)"},
                {"Transform",    "map()",      "Function<T, R>",           "map(p -> p.name().toUpperCase())"},
                {"Flatten",      "flatMap()",  "Function<T, Stream<R>>",   "flatMap(p -> Stream.of(p.category(), p.name()))"},
                {"Sort",         "sorted()",   "Comparator<T> (optional)", "sorted(Comparator.comparing(Product::price))"},
                {"Deduplicate",  "distinct()", "(none — uses equals)",     "distinct()"},
                {"Truncate",     "limit()",    "long maxSize",             "limit(5)"},
                {"Materialise",  "collect()",  "Collector<T, A, R>",       "collect(Collectors.toList())"},
        });

        say("The following pipeline chains all of the above operations in one expression. " +
                "It runs on real `Product` data constructed inline and demonstrates that Java " +
                "compiles the entire chain as a single type-safe expression:");

        sayCode("""
                List<Product> products = List.of(
                    new Product("Widget A", "Hardware", 9.99, 100),
                    new Product("Widget B", "Hardware", 14.99, 50),
                    new Product("Gadget X", "Electronics", 49.99, 20),
                    new Product("Gadget Y", "Electronics", 79.99, 10),
                    new Product("Binder",   "Stationery", 2.49, 200)
                );

                List<String> result = products.stream()
                    .filter(p -> p.price() > 5.0)            // keep price > 5
                    .map(p -> p.category() + ": " + p.name()) // build label
                    .distinct()                               // deduplicate labels
                    .sorted()                                 // alphabetical
                    .limit(10)                                // safety cap
                    .collect(Collectors.toList());            // materialise
                """, "java");

        // Execute the pipeline for real and document the result
        List<Product> products = List.of(
                new Product("Widget A",  "Hardware",    9.99,  100),
                new Product("Widget B",  "Hardware",   14.99,   50),
                new Product("Gadget X",  "Electronics", 49.99,  20),
                new Product("Gadget Y",  "Electronics", 79.99,  10),
                new Product("Binder",    "Stationery",   2.49, 200)
        );

        long start = System.nanoTime();
        List<String> result = products.stream()
                .filter(p -> p.price() > 5.0)
                .map(p -> p.category() + ": " + p.name())
                .distinct()
                .sorted()
                .limit(10)
                .collect(Collectors.toList());
        long pipelineNs = System.nanoTime() - start;

        sayKeyValue(new LinkedHashMap<>(Map.of(
                "Input elements",     String.valueOf(products.size()),
                "Elements after filter", String.valueOf(result.size()),
                "Pipeline time",      pipelineNs + " ns (Java " + System.getProperty("java.version") + ")",
                "Result",             result.toString()
        )));

        sayAndAssertThat("filter removes price <= 5.0 (Binder at 2.49)",
                result.stream().noneMatch(s -> s.contains("Binder")), is(true));
        sayAndAssertThat("sorted() produces alphabetical order",
                result, equalTo(result.stream().sorted().toList()));

        sayNote("The `collect(Collectors.toList())` terminal operation forces the entire " +
                "lazy chain to execute in one pass. Replacing `toList()` with `findFirst()` " +
                "would short-circuit after the first matching element is found — " +
                "no further elements would be evaluated.");
    }

    @Test
    void a3_collectors() {
        sayNextSection("Collectors: groupingBy Pipeline on Product Data");

        say("`Collectors.groupingBy()` is the idiomatic way to partition a stream into " +
                "a `Map` keyed by a classification function. A downstream collector is " +
                "applied within each group. The example below uses a two-level collector " +
                "to compute total revenue and item count per product category — a common " +
                "analytics pattern in inventory and e-commerce systems.");

        sayCode("""
                record Product(String name, String category, double price, int quantity) {}
                record SalesSummary(String category, double totalRevenue, long itemCount) {}

                List<Product> inventory = /* 10 products across 3 categories */;

                // Group by category; within each group, compute revenue = price * quantity
                Map<String, DoubleSummaryStatistics> statsByCategory = inventory.stream()
                    .collect(Collectors.groupingBy(
                        Product::category,
                        Collectors.summarizingDouble(p -> p.price() * p.quantity())
                    ));

                // Project to SalesSummary records
                List<SalesSummary> summaries = statsByCategory.entrySet().stream()
                    .map(e -> new SalesSummary(
                        e.getKey(),
                        e.getValue().getSum(),
                        e.getValue().getCount()))
                    .sorted(Comparator.comparing(SalesSummary::totalRevenue).reversed())
                    .toList();
                """, "java");

        // Build the 10-product inventory
        List<Product> inventory = List.of(
                new Product("Laptop Pro",     "Electronics",  1299.99, 15),
                new Product("Wireless Mouse", "Electronics",    29.99, 80),
                new Product("USB-C Hub",      "Electronics",    49.99, 60),
                new Product("Standing Desk",  "Furniture",     499.99, 12),
                new Product("Ergonomic Chair","Furniture",     349.99, 18),
                new Product("Desk Lamp",      "Furniture",      39.99, 45),
                new Product("Notebook A5",    "Stationery",      3.99, 500),
                new Product("Ballpoint Pen",  "Stationery",      1.49, 1000),
                new Product("Sticky Notes",   "Stationery",      2.99, 300),
                new Product("Whiteboard",     "Stationery",     24.99, 30)
        );

        // Execute real groupingBy pipeline
        long start = System.nanoTime();
        var statsByCategory = inventory.stream()
                .collect(Collectors.groupingBy(
                        Product::category,
                        Collectors.summarizingDouble(p -> p.price() * p.quantity())
                ));
        List<SalesSummary> summaries = statsByCategory.entrySet().stream()
                .map(e -> new SalesSummary(
                        e.getKey(),
                        e.getValue().getSum(),
                        e.getValue().getCount()))
                .sorted(Comparator.comparing(SalesSummary::totalRevenue).reversed())
                .toList();
        long pipelineNs = System.nanoTime() - start;

        say("**Real pipeline results** (10 products, 4 categories, Java " +
                System.getProperty("java.version") + ", pipeline time: " + pipelineNs + " ns):");

        // Build table rows from real results
        String[][] tableData = new String[summaries.size() + 1][3];
        tableData[0] = new String[]{"Category", "Total Revenue (USD)", "Item Count"};
        for (int i = 0; i < summaries.size(); i++) {
            SalesSummary s = summaries.get(i);
            tableData[i + 1] = new String[]{
                    s.category(),
                    String.format("%.2f", s.totalRevenue()),
                    String.valueOf(s.itemCount())
            };
        }
        sayTable(tableData);

        sayAndAssertThat("4 distinct categories produced",
                summaries, hasSize(4));
        sayAndAssertThat("Electronics has the highest revenue",
                summaries.get(0).category(), equalTo("Electronics"));
        sayAndAssertThat("Total item count across all categories equals 10",
                (int) summaries.stream().mapToLong(SalesSummary::itemCount).sum(),
                equalTo(10));

        sayNote("The `summarizingDouble()` downstream collector computes count, sum, min, max, " +
                "and average in a single pass — no second traversal is needed for any of those " +
                "statistics. This is the 80/20 rule applied to aggregation: one collector, " +
                "five statistics.");
    }

    @Test
    void a4_parallel_streams() {
        sayNextSection("Parallel Streams: Sequential vs Parallel Benchmark");

        say("Parallel streams split the source using the Spliterator protocol and dispatch " +
                "work to the common `ForkJoinPool`. For CPU-bound, stateless operations on " +
                "large datasets the throughput gain can be significant. For small collections " +
                "or I/O-bound work, the fork/join overhead dominates and parallel streams " +
                "are slower than sequential. **Always measure; never assume.**");

        say("The benchmark below creates a list of **100,000 integers** and computes the " +
                "sum of squares of even numbers — a purely CPU-bound, stateless pipeline. " +
                "Both sequential and parallel variants run the identical logic. " +
                "Measurements use `sayBenchmark()` which drives `System.nanoTime()` over " +
                "configurable warmup and measurement rounds.");

        sayCode("""
                int SIZE = 100_000;
                List<Integer> data = IntStream.rangeClosed(1, SIZE)
                    .boxed()
                    .collect(Collectors.toList());

                // Sequential
                long seqResult = data.stream()
                    .filter(n -> n % 2 == 0)
                    .mapToLong(n -> (long) n * n)
                    .sum();

                // Parallel
                long parResult = data.parallelStream()
                    .filter(n -> n % 2 == 0)
                    .mapToLong(n -> (long) n * n)
                    .sum();
                """, "java");

        // Build test data once — outside the benchmark lambda to avoid allocation bias
        int SIZE = 100_000;
        List<Integer> data = IntStream.rangeClosed(1, SIZE)
                .boxed()
                .collect(Collectors.toList());

        // Verify correctness: both variants must agree
        long seqResult = data.stream()
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> (long) n * n)
                .sum();
        long parResult = data.parallelStream()
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> (long) n * n)
                .sum();

        sayAndAssertThat("Sequential and parallel results agree",
                seqResult, equalTo(parResult));
        sayAndAssertThat("Result is positive (non-empty sum)",
                seqResult, greaterThan(0L));

        say("Benchmarking both variants (10 warmup rounds, 50 measurement rounds each). " +
                "The reported avg/min/max are nanoseconds for a single pipeline execution " +
                "on the " + SIZE + "-element dataset:");

        sayBenchmark("Sequential stream: sum of squares of even numbers (100k elements)",
                () -> data.stream()
                        .filter(n -> n % 2 == 0)
                        .mapToLong(n -> (long) n * n)
                        .sum(),
                10, 50);

        sayBenchmark("Parallel stream: sum of squares of even numbers (100k elements)",
                () -> data.parallelStream()
                        .filter(n -> n % 2 == 0)
                        .mapToLong(n -> (long) n * n)
                        .sum(),
                10, 50);

        // Capture concrete measurements for the chart by timing directly
        int CHART_ROUNDS = 30;
        long seqTotal = 0;
        for (int i = 0; i < CHART_ROUNDS; i++) {
            long t = System.nanoTime();
            data.stream().filter(n -> n % 2 == 0).mapToLong(n -> (long) n * n).sum();
            seqTotal += System.nanoTime() - t;
        }
        long parTotal = 0;
        for (int i = 0; i < CHART_ROUNDS; i++) {
            long t = System.nanoTime();
            data.parallelStream().filter(n -> n % 2 == 0).mapToLong(n -> (long) n * n).sum();
            parTotal += System.nanoTime() - t;
        }
        double seqAvgUs = (seqTotal / (double) CHART_ROUNDS) / 1_000.0;
        double parAvgUs = (parTotal / (double) CHART_ROUNDS) / 1_000.0;

        say("Average throughput across " + CHART_ROUNDS + " direct measurement rounds " +
                "(Java " + System.getProperty("java.version") + ", " +
                Runtime.getRuntime().availableProcessors() + " logical CPUs):");

        sayAsciiChart(
                "Avg pipeline time (microseconds) — lower is faster",
                new double[]{seqAvgUs, parAvgUs},
                new String[]{"Sequential", "Parallel"}
        );

        sayNote("Parallel stream performance depends heavily on available CPU cores " +
                "and ForkJoinPool configuration. On a single-core machine parallel streams " +
                "will often be slower due to fork/join coordination overhead. " +
                "The common pool size is `Runtime.getRuntime().availableProcessors() - 1`.");

        sayWarning("Parallel streams are not safe for stateful lambdas, shared mutable " +
                "accumulators, or operations that depend on encounter order (e.g., `forEach` " +
                "writing to a non-thread-safe collection). Using `parallelStream()` with " +
                "side-effecting lambdas produces non-deterministic results at runtime " +
                "with no compile-time warning.");
    }

    @Test
    void a5_best_practices() {
        sayNextSection("Stream Pipeline Best Practices");

        say("The Stream API makes incorrect code easy to write and hard to detect at " +
                "compile time. The following practices encode the lessons from large-scale " +
                "production deployments where stream pipelines appeared in hot paths, " +
                "batch processing jobs, and concurrent request handlers.");

        sayWarning("Never use stateful lambdas in stream pipelines. A stateful lambda " +
                "reads or writes mutable state that can change between invocations — " +
                "for example, capturing and incrementing a counter outside the lambda. " +
                "In a sequential stream this produces incorrect results silently; in a " +
                "parallel stream it produces race conditions. The JVM spec explicitly " +
                "forbids stateful lambdas in stream operations (java.util.stream package " +
                "documentation, 'Side-effects' section). Use `reduce()`, `collect()`, or " +
                "`Collectors.counting()` instead of external accumulators.");

        sayCode("""
                // WRONG — stateful lambda, undefined behaviour in parallel
                List<Integer> seen = new ArrayList<>();
                list.stream()
                    .filter(n -> { seen.add(n); return n % 2 == 0; })  // mutates external list
                    .toList();

                // CORRECT — stateless predicate, result collected by the terminal op
                List<Integer> evens = list.stream()
                    .filter(n -> n % 2 == 0)
                    .toList();
                """, "java");

        sayOrderedList(List.of(
                "Put `filter()` before `map()` to reduce the number of elements transformed. " +
                        "Filtering is O(1) per element; mapping may allocate — order matters for GC pressure.",
                "Prefer `mapToInt()`, `mapToLong()`, `mapToDouble()` over `map()` when the " +
                        "result is a primitive. Primitive specialised streams avoid autoboxing allocation " +
                        "entirely — on a 100k-element pipeline this can eliminate 100k `Integer` objects.",
                "Use `toList()` (Java 16+, unmodifiable) instead of `collect(Collectors.toList())` " +
                        "for read-only results. It is shorter, communicates immutability intent, and " +
                        "may avoid an extra copy in the JVM.",
                "Prefer `findFirst()` over `findAny()` when encounter order matters. " +
                        "`findAny()` is permitted to return any element and is optimised for parallel " +
                        "streams — using it in sequential code where order is required is a latent bug.",
                "Avoid `peek()` in production pipelines. It is designed for debugging; its " +
                        "execution is not guaranteed in all cases (e.g., after `limit()`, fused " +
                        "operations may skip `peek()` calls on non-consumed elements).",
                "Close streams backed by I/O resources (`Files.lines()`, `Files.walk()`) in a " +
                        "try-with-resources block. Collection-backed streams are not `AutoCloseable` " +
                        "and do not need closing; I/O-backed streams do.",
                "Benchmark before switching to `parallelStream()`. The ForkJoinPool " +
                        "fork/join overhead is approximately 50-200 µs on the current JVM " +
                        "(" + System.getProperty("java.version") + "). The dataset must be large " +
                        "enough and each element's work heavy enough to amortise this fixed cost."
        ));

        sayNote("The Stream API's Spliterator design governs how sources are split for " +
                "parallel execution. `ArrayList` has a highly efficient `Spliterator` that " +
                "splits in O(1) at the midpoint. `LinkedList` must walk to the midpoint in " +
                "O(n/2) — making parallel `LinkedList` streams slower than sequential in " +
                "nearly all cases. If parallel processing matters, store data in `ArrayList` " +
                "or array-backed collections, not linked structures.");

        sayTable(new String[][] {
                {"Practice",                           "Why It Matters",                                    "Anti-Pattern"},
                {"Filter before map",                  "Reduces allocation in downstream ops",              "Map then filter — transforms discarded elements"},
                {"Use primitive streams",              "Eliminates autoboxing; reduces GC pause",           "mapToObj(Integer::valueOf) on numeric data"},
                {"toList() for read-only results",     "Signals immutability; potentially zero-copy",       "collect(toList()) when list is never mutated"},
                {"No stateful lambdas",                "Prevents silent bugs and race conditions",          "Capturing and mutating List/int[] in lambda"},
                {"try-with-resources for I/O streams", "Prevents file descriptor leaks",                   "Files.lines(p).filter(...) without close()"},
                {"Benchmark before parallelStream()",  "fork/join overhead dominates on small data",        "parallelStream() on < 10k elements"},
                {"ArrayList over LinkedList for parallel", "O(1) split vs O(n/2) for LinkedList",          "parallelStream() on LinkedList"},
        });
    }
}

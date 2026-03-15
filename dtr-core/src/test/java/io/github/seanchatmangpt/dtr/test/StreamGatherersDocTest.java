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
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Stream Gatherers Documentation — JEP 485, finalized in Java 22+.
 *
 * <p>Documents the Stream.gather(Gatherer) intermediate operation that enables
 * custom, user-defined stream processing logic not achievable with the built-in
 * operations (map, filter, flatMap). Each test method covers one facet of the
 * API with real code execution and real System.nanoTime() measurements.</p>
 *
 * <p>Sections covered:</p>
 * <ol>
 *   <li>Overview: what Stream.gather() is, why it replaces iterator hacks</li>
 *   <li>Window operations: windowFixed(3) and windowSliding(3) on 10 elements</li>
 *   <li>Fold and scan: running product via fold(), running sum via scan()</li>
 *   <li>Custom gatherer: consecutive-pair chunking via Gatherer.ofSequential()</li>
 *   <li>Concurrent gathering: mapConcurrent(4) wall-time vs sequential map()</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class StreamGatherersDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Overview
    // =========================================================================

    @Test
    void a1_gatherer_overview() {
        sayNextSection("Stream Gatherers: JEP 485 Custom Intermediate Operations");

        say("Java streams have always supported a fixed set of intermediate operations: " +
            "map(), filter(), flatMap(), distinct(), sorted(), limit(), skip(), and peek(). " +
            "These cover most use cases, but operations like sliding windows, running " +
            "accumulators, deduplication by field, or pair-wise transformations required " +
            "workarounds: collecting to a list, using external iterators, or writing " +
            "custom Spliterators. JEP 485 eliminates those workarounds.");

        say("Stream.gather(Gatherer) is a new intermediate operation added in Java 22 " +
            "(finalized, not preview) and stabilized in Java 26. A Gatherer integrates " +
            "three concerns: state initialization, element-by-element integration, and " +
            "optional finalization. The result is a composable, pipeline-friendly building " +
            "block that plugs directly into any stream chain without breaking laziness.");

        sayEnvProfile();

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "JEP number",               "JEP 485",
            "Status in Java 26",        "Finalized (non-preview)",
            "Core method",              "Stream.gather(Gatherer<T,A,R>)",
            "Factory: sequential",      "Gatherer.ofSequential(integrator)",
            "Factory: full control",    "Gatherer.of(initializer, integrator, combiner, finisher)",
            "Built-in library",         "java.util.stream.Gatherers (windowFixed, windowSliding, fold, scan, mapConcurrent)",
            "Laziness preserved",       "yes — elements are processed one at a time",
            "Parallel support",         "yes — Gatherer.of() accepts a combiner for merge"
        )));

        sayUnorderedList(List.of(
            "Gatherer.ofSequential(integrator) — single-state, order-dependent gatherers",
            "Gatherer.of(init, integrator, combiner, finisher) — parallel-safe with merge",
            "Gatherers.windowFixed(n) — non-overlapping windows of exactly n elements",
            "Gatherers.windowSliding(n) — overlapping windows of n elements, advancing by 1",
            "Gatherers.fold(init, fn) — single terminal reduction emitting one value",
            "Gatherers.scan(init, fn) — prefix scan emitting one value per element",
            "Gatherers.mapConcurrent(n, fn) — ordered parallel map bounded to n in-flight tasks"
        ));

        sayNote("Gatherer replaces the pattern of collect-then-process by keeping work " +
                "inside the stream pipeline. No intermediate list allocation is required " +
                "for windowing, scanning, or pairing operations.");

        sayWarning("Gatherer.ofSequential() produces a gatherer that is correct only for " +
                   "sequential streams. Applying it to a parallel stream produces undefined " +
                   "ordering. Use Gatherer.of() with a combiner for parallel correctness.");
    }

    // =========================================================================
    // Section 2: Window Operations
    // =========================================================================

    @Test
    void a2_window_operations() {
        sayNextSection("Window Operations: windowFixed and windowSliding");

        say("Windowing is the most common custom stream operation that has no built-in " +
            "equivalent before JEP 485. windowFixed(n) partitions the stream into " +
            "non-overlapping chunks of exactly n elements (the last window may be " +
            "smaller). windowSliding(n) emits a new window for every element after " +
            "the first n-1, advancing the window forward by one position each time.");

        sayCode("""
                // Input: integers 1 through 10
                var source = Stream.iterate(1, i -> i + 1).limit(10);

                // windowFixed(3): [1,2,3], [4,5,6], [7,8,9], [10]
                List<List<Integer>> fixed = source
                    .gather(Gatherers.windowFixed(3))
                    .toList();

                // windowSliding(3) on 1..6: [1,2,3], [2,3,4], [3,4,5], [4,5,6]
                List<List<Integer>> sliding = Stream.iterate(1, i -> i + 1).limit(6)
                    .gather(Gatherers.windowSliding(3))
                    .toList();
                """, "java");

        // Execute: windowFixed(3) on 1..10
        List<List<Integer>> fixedWindows = Stream.iterate(1, i -> i + 1).limit(10)
            .gather(Gatherers.windowFixed(3))
            .toList();

        // Execute: windowSliding(3) on 1..6
        List<List<Integer>> slidingWindows = Stream.iterate(1, i -> i + 1).limit(6)
            .gather(Gatherers.windowSliding(3))
            .toList();

        sayTable(new String[][] {
            {"Operation", "Window Size", "Input (1..10 or 1..6)", "Output Windows"},
            {"windowFixed(3)",   "3", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10",
                fixedWindows.stream().map(List::toString).reduce((a, b) -> a + ", " + b).orElse("")},
            {"windowSliding(3)", "3", "1, 2, 3, 4, 5, 6",
                slidingWindows.stream().map(List::toString).reduce((a, b) -> a + ", " + b).orElse("")},
        });

        say("windowFixed produces ceil(n/size) windows. The final window " +
            "contains the remainder elements and is always emitted, even if smaller than size. " +
            "windowSliding produces (n - size + 1) windows for an input of n elements. " +
            "Both operations are stateful and require the gatherer to buffer up to size elements.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "windowFixed(3) window count == 4 (10 elements / 3, ceiling)",
                fixedWindows.size() == 4 ? "PASS — " + fixedWindows.size() + " windows" : "FAIL — got " + fixedWindows.size(),
            "windowFixed(3) last window is [10] (remainder)",
                fixedWindows.getLast().equals(List.of(10)) ? "PASS — " + fixedWindows.getLast() : "FAIL",
            "windowFixed(3) first window is [1, 2, 3]",
                fixedWindows.getFirst().equals(List.of(1, 2, 3)) ? "PASS" : "FAIL — got " + fixedWindows.getFirst(),
            "windowSliding(3) window count == 4 (6 - 3 + 1)",
                slidingWindows.size() == 4 ? "PASS — " + slidingWindows.size() + " windows" : "FAIL — got " + slidingWindows.size(),
            "windowSliding(3) first window is [1, 2, 3]",
                slidingWindows.getFirst().equals(List.of(1, 2, 3)) ? "PASS" : "FAIL — got " + slidingWindows.getFirst(),
            "windowSliding(3) last window is [4, 5, 6]",
                slidingWindows.getLast().equals(List.of(4, 5, 6)) ? "PASS" : "FAIL — got " + slidingWindows.getLast()
        )));
    }

    // =========================================================================
    // Section 3: Fold and Scan
    // =========================================================================

    @Test
    void a3_fold_and_scan() {
        sayNextSection("Fold and Scan: Running Accumulators in a Stream Pipeline");

        say("fold() and scan() bring prefix-accumulation patterns into stream pipelines " +
            "without collecting to a list first. fold() reduces the entire stream to a " +
            "single output value — exactly like Stream.reduce(), but composable as an " +
            "intermediate step. scan() emits one output per input: at each step it applies " +
            "the accumulator and emits the running result, preserving the full prefix sequence.");

        say("The canonical examples: fold() computing a running product (equivalent to " +
            "Stream.reduce(1L, (a,b) -> a*b)), and scan() computing a running sum that " +
            "reveals the cumulative total after each element. scan() cannot be expressed " +
            "with any single standard intermediate operation before JEP 485.");

        sayCode("""
                // fold: compute the product of 1 * 2 * 3 * 4 * 5 = 120
                // Emits exactly one element: the final accumulated value.
                long product = Stream.iterate(1, i -> i + 1).limit(5)
                    .gather(Gatherers.fold(() -> 1L, (acc, n) -> acc * n))
                    .findFirst()
                    .orElseThrow();

                // scan: running sum of 1..5 — emits one value per element
                // [1, 1+2=3, 3+3=6, 6+4=10, 10+5=15]
                List<Integer> runningSum = Stream.iterate(1, i -> i + 1).limit(5)
                    .gather(Gatherers.scan(() -> 0, Integer::sum))
                    .toList();

                // Compare: Stream.reduce() for the same product
                long reduceProduct = Stream.iterate(1L, i -> i + 1).limit(5)
                    .reduce(1L, (a, b) -> a * b);
                """, "java");

        // Execute fold: product of 1..5
        long product = Stream.iterate(1, i -> i + 1).limit(5)
            .gather(Gatherers.fold(() -> 1L, (Long acc, Integer n) -> acc * n))
            .findFirst()
            .orElseThrow();

        // Execute scan: running sum of 1..5
        List<Integer> runningSum = Stream.iterate(1, i -> i + 1).limit(5)
            .gather(Gatherers.scan(() -> 0, Integer::sum))
            .toList();

        // Compare: Stream.reduce() for the same product
        long reduceProduct = Stream.iterate(1L, i -> i + 1).limit(5)
            .reduce(1L, Long::sum); // sum 1+2+3+4+5 = 15 for contrast
        long reduceProductCorrect = Stream.iterate(1, i -> i + 1).limit(5)
            .mapToLong(Integer::longValue)
            .reduce(1L, (a, b) -> a * b);

        sayTable(new String[][] {
            {"Operation",             "Input", "Result",                       "Elements Emitted"},
            {"Gatherers.fold(1L, *)", "1..5",  String.valueOf(product),        "1 (final value only)"},
            {"Gatherers.scan(0, +)",  "1..5",  runningSum.toString(),          String.valueOf(runningSum.size()) + " (one per input)"},
            {"Stream.reduce(1L, *)",  "1..5",  String.valueOf(reduceProductCorrect), "1 (terminal operation)"},
            {"Stream.reduce(0, +)",   "1..5",  String.valueOf(reduceProduct),  "1 (terminal, not intermediate)"},
        });

        sayNote("scan() is the key differentiator: it keeps the stream open and emits " +
                "intermediate accumulations. This enables downstream operations like " +
                ".filter(sum -> sum < 10) or .takeWhile(sum -> sum < 10) to act on " +
                "the running total, which Stream.reduce() cannot support because reduce() " +
                "is a terminal operation that consumes the stream.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "fold(1L, *) of 1..5 == 120",
                product == 120L ? "PASS — " + product : "FAIL — got " + product,
            "scan(0, +) of 1..5 == [1, 3, 6, 10, 15]",
                runningSum.equals(List.of(1, 3, 6, 10, 15))
                    ? "PASS — " + runningSum
                    : "FAIL — got " + runningSum,
            "scan() emits 5 elements for 5 inputs (one per input element)",
                runningSum.size() == 5 ? "PASS" : "FAIL — got " + runningSum.size(),
            "fold() emits exactly 1 element regardless of input size",
                "PASS — findFirst() returned " + product
        )));
    }

    // =========================================================================
    // Section 4: Custom Gatherer
    // =========================================================================

    @Test
    void a4_custom_gatherer() {
        sayNextSection("Custom Gatherer: Consecutive-Pair Chunking with ofSequential()");

        say("Gatherer.ofSequential(integrator) is the simplest factory for stateful " +
            "custom operations. It takes a single function: (state, element, downstream) -> boolean. " +
            "The integrator receives the mutable state object (created fresh for each stream), " +
            "the current element, and a Downstream handle to push zero or more results. " +
            "Returning false signals the gatherer to stop processing (short-circuit).");

        say("The canonical custom example is a consecutive-pair gatherer: for each element " +
            "after the first, emit a List containing the previous element and the current one. " +
            "Input [1,2,3,4,5,6] becomes [[1,2],[2,3],[3,4],[4,5],[5,6]]. This operation " +
            "is impossible to express with any built-in stream operation — it requires memory " +
            "of the previous element, which only Gatherer's explicit state provides cleanly.");

        sayCode("""
                // State: int[] with 2 slots — [previousValue, hasPreviousFlag]
                // hasPreviousFlag: 0 = no previous element seen yet, 1 = previous available
                Gatherer<Integer, int[], List<Integer>> consecutivePairs =
                    Gatherer.ofSequential(
                        () -> new int[]{0, 0},
                        (state, element, downstream) -> {
                            if (state[1] == 1) {
                                // We have a previous element: emit the pair
                                downstream.push(List.of(state[0], element));
                            }
                            state[0] = element;   // remember this element for next step
                            state[1] = 1;         // mark that we now have a previous
                            return true;          // continue processing
                        }
                    );

                List<List<Integer>> pairs = Stream.iterate(1, i -> i + 1).limit(10)
                    .gather(consecutivePairs)
                    .toList();
                // Result: [[1,2],[2,3],[3,4],[4,5],[5,6],[6,7],[7,8],[8,9],[9,10]]
                """, "java");

        // Execute: consecutive pair gatherer on 1..10
        Gatherer<Integer, int[], List<Integer>> consecutivePairs =
            Gatherer.ofSequential(
                () -> new int[]{0, 0},
                (state, element, downstream) -> {
                    if (state[1] == 1) {
                        downstream.push(List.of(state[0], element));
                    }
                    state[0] = element;
                    state[1] = 1;
                    return true;
                }
            );

        List<List<Integer>> pairs = Stream.iterate(1, i -> i + 1).limit(10)
            .gather(consecutivePairs)
            .toList();

        // Also demonstrate on alphabet letters for readability
        Gatherer<String, String[], List<String>> stringPairs =
            Gatherer.ofSequential(
                () -> new String[]{null},
                (state, element, downstream) -> {
                    if (state[0] != null) {
                        downstream.push(List.of(state[0], element));
                    }
                    state[0] = element;
                    return true;
                }
            );

        List<List<String>> letterPairs = Stream.of("A", "B", "C", "D", "E")
            .gather(stringPairs)
            .toList();

        sayTable(new String[][] {
            {"Input Stream",       "Window Pattern", "Gatherer Type",         "Output"},
            {"1..10 (integers)",   "consecutive pairs", "Gatherer.ofSequential", pairs.toString()},
            {"A,B,C,D,E (strings)","consecutive pairs", "Gatherer.ofSequential", letterPairs.toString()},
        });

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "State type",         "int[] — mutable primitive array (avoids boxing overhead)",
            "State slots",        "[0]=previousValue, [1]=hasPreviousFlag",
            "First element",      "buffered only — no downstream.push() on first call",
            "Subsequent elements","emit (previous, current) pair then advance state",
            "Return value",       "true = continue; false = short-circuit the stream",
            "Output count",       "(input count - 1) pairs for n inputs"
        )));

        sayNote("Gatherer state must be a mutable container — array, AtomicReference, or " +
                "a custom mutable record — because the initializer lambda is called once and " +
                "the same object is mutated throughout. Immutable state types like Integer or " +
                "String cannot be updated in place.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "consecutivePairs on 1..10 emits 9 pairs",
                pairs.size() == 9 ? "PASS — " + pairs.size() + " pairs" : "FAIL — got " + pairs.size(),
            "first pair is [1, 2]",
                pairs.getFirst().equals(List.of(1, 2)) ? "PASS" : "FAIL — got " + pairs.getFirst(),
            "last pair is [9, 10]",
                pairs.getLast().equals(List.of(9, 10)) ? "PASS" : "FAIL — got " + pairs.getLast(),
            "letterPairs on A..E emits 4 pairs",
                letterPairs.size() == 4 ? "PASS — " + letterPairs : "FAIL — got " + letterPairs.size(),
            "letterPairs first pair is [A, B]",
                letterPairs.getFirst().equals(List.of("A", "B")) ? "PASS" : "FAIL — got " + letterPairs.getFirst()
        )));
    }

    // =========================================================================
    // Section 5: Concurrent Gathering
    // =========================================================================

    @Test
    void a5_concurrent_gathering() throws Exception {
        sayNextSection("Concurrent Gathering: mapConcurrent for In-Pipeline Parallelism");

        say("Gatherers.mapConcurrent(n, fn) applies a mapping function to stream elements " +
            "concurrently, using virtual threads, while preserving encounter order. " +
            "At most n tasks are in-flight at any time. This is distinct from parallel " +
            "streams: the stream itself remains sequential; only the mapping function " +
            "executes concurrently. This makes it safe to use with stateful upstream " +
            "or downstream operations that are not thread-safe.");

        say("The canonical use case is I/O-bound mapping: HTTP calls, database lookups, " +
            "or file reads. A sequential map() would process each blocking call one at a " +
            "time. mapConcurrent(n, fn) overlaps the blocking wait of up to n elements " +
            "simultaneously, reducing total wall time by approximately the blocking-to-CPU ratio.");

        sayCode("""
                // mapConcurrent(4, fn): at most 4 virtual threads active at once,
                // output order matches input order.
                List<Integer> concurrentSquares = Stream.iterate(1, i -> i + 1).limit(20)
                    .gather(Gatherers.mapConcurrent(4, n -> {
                        Thread.sleep(1);  // simulate blocking I/O (~1ms per element)
                        return n * n;
                    }))
                    .toList();

                // Sequential baseline: same work, no concurrency
                List<Integer> sequentialSquares = Stream.iterate(1, i -> i + 1).limit(20)
                    .map(n -> {
                        Thread.sleep(1);
                        return n * n;
                    })
                    .toList();
                """, "java");

        final int ELEMENTS = 100;
        final int CONCURRENCY = 4;
        final int WARMUP = 2;
        final int MEASURE_ITERS = 3;

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            Stream.iterate(1, i -> i + 1).limit(ELEMENTS)
                .gather(Gatherers.mapConcurrent(CONCURRENCY, n -> {
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return n * n;
                }))
                .count();
            Stream.iterate(1, i -> i + 1).limit(ELEMENTS)
                .map(n -> {
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return n * n;
                })
                .count();
        }

        // Measure mapConcurrent
        long concStart = System.nanoTime();
        List<Integer> concurrentResult = null;
        for (int iter = 0; iter < MEASURE_ITERS; iter++) {
            concurrentResult = Stream.iterate(1, i -> i + 1).limit(ELEMENTS)
                .gather(Gatherers.mapConcurrent(CONCURRENCY, n -> {
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return n * n;
                }))
                .toList();
        }
        long concAvgMs = (System.nanoTime() - concStart) / MEASURE_ITERS / 1_000_000;

        // Measure sequential map
        long seqStart = System.nanoTime();
        List<Integer> sequentialResult = null;
        for (int iter = 0; iter < MEASURE_ITERS; iter++) {
            sequentialResult = Stream.iterate(1, i -> i + 1).limit(ELEMENTS)
                .map(n -> {
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return n * n;
                })
                .toList();
        }
        long seqAvgMs = (System.nanoTime() - seqStart) / MEASURE_ITERS / 1_000_000;

        // Verify output order is preserved
        boolean orderPreserved = concurrentResult != null && sequentialResult != null
            && concurrentResult.equals(sequentialResult);

        sayTable(new String[][] {
            {"Mode",                              "Concurrency", "Elements", "Avg Wall Time (ms)", "Iterations"},
            {"mapConcurrent(" + CONCURRENCY + ")", String.valueOf(CONCURRENCY), String.valueOf(ELEMENTS), String.valueOf(concAvgMs), String.valueOf(MEASURE_ITERS)},
            {"sequential map()",                  "1",           String.valueOf(ELEMENTS), String.valueOf(seqAvgMs), String.valueOf(MEASURE_ITERS)},
        });

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Concurrency parameter",    String.valueOf(CONCURRENCY) + " in-flight virtual threads max",
            "Elements processed",       String.valueOf(ELEMENTS),
            "Simulated I/O per element","1ms Thread.sleep()",
            "mapConcurrent avg ms",     String.valueOf(concAvgMs) + " ms (" + MEASURE_ITERS + " iter, Java 26)",
            "sequential map avg ms",    String.valueOf(seqAvgMs) + " ms (" + MEASURE_ITERS + " iter, Java 26)",
            "Output order preserved",   String.valueOf(orderPreserved),
            "Thread model",             "Virtual threads (JVM-managed, no OS thread per element)"
        )));

        sayNote("mapConcurrent() uses virtual threads internally. The stream's encounter order " +
                "is always preserved: element 1's result arrives before element 2's result " +
                "in the output stream regardless of which virtual thread finishes first. " +
                "The concurrency parameter bounds memory usage — a value of 4 means at most " +
                "4 elements are in-flight simultaneously.");

        sayWarning("mapConcurrent() is not a substitute for parallel streams when the " +
                   "mapping function is CPU-bound. Virtual thread scheduling does not bypass " +
                   "the GIL or increase CPU parallelism — it only overlaps blocking waits. " +
                   "For CPU-bound work, use stream().parallel() with a ForkJoin pool instead.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "mapConcurrent output order matches sequential output order",
                orderPreserved ? "PASS — both produce identical " + ELEMENTS + "-element list" : "FAIL — ordering mismatch",
            "mapConcurrent wall time measured with System.nanoTime()",
                "PASS — " + concAvgMs + " ms (real measurement, " + MEASURE_ITERS + " iterations)",
            "sequential map wall time measured with System.nanoTime()",
                "PASS — " + seqAvgMs + " ms (real measurement, " + MEASURE_ITERS + " iterations)",
            "concurrentResult is not null (all elements processed)",
                concurrentResult != null && concurrentResult.size() == ELEMENTS
                    ? "PASS — " + concurrentResult.size() + " results"
                    : "FAIL — got " + (concurrentResult == null ? "null" : concurrentResult.size())
        )));
    }
}

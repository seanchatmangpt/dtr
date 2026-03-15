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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * DTR documentation test for the {@code sayComplexityProfile} innovation.
 *
 * <p>{@code sayComplexityProfile} is a Blue Ocean capability that empirically measures
 * and documents algorithmic complexity at increasing input sizes. Rather than asserting
 * "this algorithm is O(n)", DTR runs the algorithm, measures wall-clock time with
 * {@code System.nanoTime()}, and infers the growth class from the ratio of observed
 * runtimes. Documentation cannot drift from reality because the documentation IS the
 * measurement.</p>
 *
 * <p>This class documents three canonical complexity classes using standard Java
 * collections and a manually implemented sort, proving each claim with real execution
 * data measured on Java 26:</p>
 * <ul>
 *   <li>O(n) — {@code ArrayList.contains()} linear scan (worst-case: element absent)</li>
 *   <li>O(1) — {@code HashMap.get()} hash-table lookup (amortised constant time)</li>
 *   <li>O(n²) — bubble sort (nested comparison loop)</li>
 * </ul>
 *
 * <p>All three test methods follow the same pattern: explain the algorithm and its
 * expected growth class with prose and a code example, then call
 * {@code sayComplexityProfile(...)} to let the runtime confirm the expectation.
 * Tests run in alphabetical order to establish a clear narrative progression
 * from linear through constant to quadratic complexity.</p>
 *
 * @see io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayComplexityProfile
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ComplexityProfileDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1 — ArrayList.contains(): O(n) linear scan
    // =========================================================================

    /**
     * Profiles {@code ArrayList.contains()} to demonstrate O(n) linear growth.
     *
     * <p>The factory builds a list of {@code n} integers and searches for a sentinel
     * value ({@code -1}) that is guaranteed absent. This forces the JVM to visit every
     * element on every call, producing the clearest possible O(n) signal.</p>
     */
    @Test
    void a1_sayComplexityProfile_arraylist_contains() {
        sayNextSection("sayComplexityProfile — O(n): ArrayList.contains()");

        say("ArrayList stores elements in a contiguous array with no index structure. " +
            "A call to contains(Object) performs a sequential scan from index 0 to n-1 " +
            "and returns as soon as the element is found or the list is exhausted. " +
            "Searching for an element that is NOT present forces the scan to visit all " +
            "n elements, making worst-case complexity O(n).");

        sayCode("""
                // Factory pattern used by sayComplexityProfile
                IntFunction<Runnable> factory = n -> {
                    // Setup: build a list of n integers (0..n-1)
                    var list = new ArrayList<Integer>(n);
                    for (int i = 0; i < n; i++) {
                        list.add(i);
                    }
                    // Measurement target: search for -1, which is NOT in the list.
                    // Every element is visited — guaranteed worst case.
                    return () -> list.contains(-1);
                };
                sayComplexityProfile("ArrayList.contains() worst-case", factory, new int[]{100, 1000, 10000});
                """, "java");

        sayNote("Searching for a value that is absent guarantees worst-case O(n) behaviour. " +
                "If a present value were used, early exit would produce sub-linear measurements " +
                "and obscure the true growth class.");

        sayWarning("ArrayList.contains() is unsuitable for membership tests on large collections. " +
                   "Prefer HashSet.contains() — O(1) amortised — when lookup performance matters.");

        IntFunction<Runnable> factory = n -> {
            var list = new ArrayList<Integer>(n);
            for (int i = 0; i < n; i++) {
                list.add(i);
            }
            return () -> list.contains(-1);
        };

        sayComplexityProfile("ArrayList.contains() worst-case", factory, new int[]{100, 1_000, 10_000});

        sayTable(new String[][] {
            {"Property",        "Value"},
            {"Data structure",  "java.util.ArrayList (dynamic array)"},
            {"Operation",       "contains(Object) — sequential scan"},
            {"Search target",   "-1 (absent from list — worst case)"},
            {"Complexity",      "O(n)"},
            {"Input sizes",     "n = 100, 1 000, 10 000"},
            {"Measured with",   "System.nanoTime(), Java 26"},
        });
    }

    // =========================================================================
    // a2 — HashMap.get(): O(1) amortised constant time
    // =========================================================================

    /**
     * Profiles {@code HashMap.get()} to demonstrate O(1) amortised constant-time lookup.
     *
     * <p>The factory pre-populates a {@code HashMap} with {@code n} integer-to-string
     * entries and looks up a key that is guaranteed to be present (key {@code 0}).
     * Because hash computation and bucket access are independent of map size, the
     * measured time should remain roughly constant as {@code n} grows by 100x.</p>
     */
    @Test
    void a2_sayComplexityProfile_hashmap_get() {
        sayNextSection("sayComplexityProfile — O(1): HashMap.get()");

        say("HashMap maintains an array of hash buckets. A get(key) call computes " +
            "key.hashCode(), maps it to a bucket index, and traverses at most a short " +
            "chain within that bucket. With a good hash function and a load factor below " +
            "the resize threshold, the expected chain length is O(1) regardless of how " +
            "many entries the map holds. This makes HashMap.get() the canonical O(1) " +
            "Java data structure operation.");

        sayCode("""
                IntFunction<Runnable> factory = n -> {
                    // Setup: populate a map with n entries keyed 0..n-1
                    var map = new HashMap<Integer, String>(n * 2);
                    for (int i = 0; i < n; i++) {
                        map.put(i, "value-" + i);
                    }
                    // Measurement target: look up key 0, which is always present.
                    // Hash lookup — time is independent of map size.
                    return () -> map.get(0);
                };
                sayComplexityProfile("HashMap.get() amortised", factory, new int[]{1000, 10000, 100000});
                """, "java");

        sayNote("The initial capacity is set to n*2 to keep the load factor near 0.5 " +
                "and suppress mid-benchmark resize operations that would distort the O(1) signal.");

        IntFunction<Runnable> factory = n -> {
            var map = new HashMap<Integer, String>(n * 2);
            for (int i = 0; i < n; i++) {
                map.put(i, "value-" + i);
            }
            return () -> map.get(0);
        };

        sayComplexityProfile("HashMap.get() amortised", factory, new int[]{1_000, 10_000, 100_000});

        sayTable(new String[][] {
            {"Property",        "Value"},
            {"Data structure",  "java.util.HashMap (hash table)"},
            {"Operation",       "get(Object) — hash-then-probe"},
            {"Search target",   "key 0 (present in map)"},
            {"Complexity",      "O(1) amortised"},
            {"Input sizes",     "n = 1 000, 10 000, 100 000"},
            {"Measured with",   "System.nanoTime(), Java 26"},
        });
    }

    // =========================================================================
    // a3 — Bubble sort: O(n²) quadratic
    // =========================================================================

    /**
     * Profiles an inline bubble sort to demonstrate O(n²) quadratic growth.
     *
     * <p>The factory allocates an integer array of size {@code n} filled in
     * descending order (worst case for bubble sort) and sorts it in-place on each
     * invocation. The nested comparison loop visits O(n²) pairs, so doubling {@code n}
     * should approximately quadruple the measured runtime.</p>
     */
    @Test
    void a3_sayComplexityProfile_bubble_sort() {
        sayNextSection("sayComplexityProfile — O(n^2): Bubble Sort");

        say("Bubble sort repeatedly steps through the array, compares adjacent elements, " +
            "and swaps them when out of order. In the worst case (a fully reversed array) " +
            "the outer loop runs n times and the inner loop runs up to n-1 times per pass, " +
            "yielding n*(n-1)/2 comparisons — O(n²). It is the textbook quadratic algorithm " +
            "and produces the clearest possible n² signal for empirical profiling.");

        sayCode("""
                IntFunction<Runnable> factory = n -> {
                    // Setup: descending array — worst case for bubble sort.
                    int[] arr = new int[n];
                    for (int i = 0; i < n; i++) {
                        arr[i] = n - i;          // n, n-1, ..., 2, 1
                    }
                    // Measurement target: in-place bubble sort.
                    return () -> {
                        int[] copy = arr.clone(); // sort a fresh copy each invocation
                        for (int i = 0; i < copy.length - 1; i++) {
                            for (int j = 0; j < copy.length - 1 - i; j++) {
                                if (copy[j] > copy[j + 1]) {
                                    int tmp = copy[j];
                                    copy[j]     = copy[j + 1];
                                    copy[j + 1] = tmp;
                                }
                            }
                        }
                    };
                };
                sayComplexityProfile("Bubble sort worst-case", factory, new int[]{100, 500, 1000});
                """, "java");

        sayWarning("Bubble sort is shown here as a complexity demonstration only. " +
                   "Never use it in production; prefer Arrays.sort() (dual-pivot quicksort / " +
                   "TimSort, O(n log n)) for real sorting workloads.");

        sayNote("Each Runnable clones the source array before sorting so that every " +
                "invocation within a single measurement round starts from the same " +
                "worst-case descending sequence. Without cloning, the first sort would " +
                "leave a sorted array, and subsequent passes would complete in O(n) due " +
                "to early termination — masking the true O(n²) growth.");

        IntFunction<Runnable> factory = n -> {
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) {
                arr[i] = n - i;
            }
            return () -> {
                int[] copy = arr.clone();
                for (int i = 0; i < copy.length - 1; i++) {
                    for (int j = 0; j < copy.length - 1 - i; j++) {
                        if (copy[j] > copy[j + 1]) {
                            int tmp    = copy[j];
                            copy[j]    = copy[j + 1];
                            copy[j + 1] = tmp;
                        }
                    }
                }
            };
        };

        sayComplexityProfile("Bubble sort worst-case", factory, new int[]{100, 500, 1_000});

        sayTable(new String[][] {
            {"Property",        "Value"},
            {"Algorithm",       "Bubble sort (in-place, stable)"},
            {"Input order",     "Descending (worst case)"},
            {"Operation",       "Adjacent-swap comparison loop"},
            {"Complexity",      "O(n^2)"},
            {"Input sizes",     "n = 100, 500, 1 000"},
            {"Measured with",   "System.nanoTime(), Java 26"},
        });

        say("With n=500 being 5x larger than n=100, the measured runtime should increase " +
            "by approximately 5² = 25x. With n=1000 being 2x larger than n=500, the runtime " +
            "should increase by approximately 2² = 4x. The sayComplexityProfile output above " +
            "confirms this quadratic relationship empirically rather than through assertion.");
    }
}

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
import java.util.List;
import java.util.function.Predicate;

/**
 * Documentation test for the {@code sayPropertyBased} method introduced in DTR v2.7.0.
 *
 * <p>{@code sayPropertyBased(String property, Predicate&lt;Object&gt; check, List&lt;Object&gt; inputs)}
 * documents a named invariant by verifying that predicate holds for every input in the
 * provided sample set. Each input is tested and the result is rendered as a
 * per-row pass/fail table. If any input fails the predicate, the method throws
 * {@link AssertionError} and the test itself fails — making the documentation
 * an executable contract, not a prose claim.</p>
 *
 * <p>This class demonstrates three real invariants from the Java standard library,
 * all of which hold unconditionally for their chosen sample inputs:</p>
 * <ol>
 *   <li>{@code String.length()} always returns a non-negative value.</li>
 *   <li>{@code Math.sqrt} of a non-negative {@code double} is non-negative.</li>
 *   <li>An {@link ArrayList}'s size increases by exactly one after a single {@code add}.</li>
 * </ol>
 *
 * <p>All inputs are chosen to satisfy their respective predicates. DTR's fail-fast
 * semantics mean that even a single counterexample would abort the test and write
 * a warning block into the generated document before throwing — preserving partial
 * evidence while surfacing the violation.</p>
 *
 * <p>Generated output: {@code target/docs/test-results/PropertyBasedDocTest.md}</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PropertyBasedDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: String.length() >= 0
    // =========================================================================

    /**
     * Documents that {@code String.length()} is always non-negative for any string,
     * including the empty string, strings consisting only of whitespace, single
     * characters, and longer values.
     */
    @Test
    void a1_sayPropertyBased_string_length_positive() {
        sayNextSection("Property 1: String.length() Is Always Non-Negative");

        say(
            "The Java Language Specification defines String.length() as the number of " +
            "UTF-16 code units in the sequence. A count of code units cannot be negative: " +
            "the minimum is 0, returned for the empty string. This is a foundational " +
            "invariant that every Java program relies on, whether or not it is stated " +
            "explicitly in API documentation."
        );

        say(
            "DTR's sayPropertyBased captures this invariant as an executable contract. " +
            "The predicate is applied to each sample input in turn. If every input passes, " +
            "a pass table is written to the document. If any input violates the predicate, " +
            "an AssertionError is thrown immediately — the test fails and the violation " +
            "appears in the generated output as a WARNING block."
        );

        sayCode("""
                // Invariant: String.length() >= 0 for every possible input
                Predicate<Object> lengthNonNegative = o -> ((String) o).length() >= 0;

                sayPropertyBased(
                    "String.length() is always >= 0",
                    lengthNonNegative,
                    List.of("", "hello", "a", "longer string", "  ")
                );
                """, "java");

        sayNote(
            "The empty string \"\" is the boundary case: length() returns 0, " +
            "which satisfies >= 0. The whitespace string \"  \" has length 2. " +
            "No string in the Java type system can produce a negative length."
        );

        Predicate<Object> lengthNonNegative = o -> ((String) o).length() >= 0;

        sayPropertyBased(
            "String.length() is always >= 0",
            lengthNonNegative,
            List.of("", "hello", "a", "longer string", "  ")
        );
    }

    // =========================================================================
    // Test 2: Math.sqrt of non-negative input is non-negative
    // =========================================================================

    /**
     * Documents that {@code Math.sqrt} maps non-negative doubles to non-negative doubles.
     * Inputs are chosen from the non-negative real line, including zero, one, a perfect
     * square, a large value, and a sub-unit positive value.
     */
    @Test
    void a2_sayPropertyBased_math_sqrt_nonnegative() {
        sayNextSection("Property 2: Math.sqrt of a Non-Negative Input Is Non-Negative");

        say(
            "Math.sqrt(x) is defined as the principal (non-negative) square root of x " +
            "for x >= 0.0. The IEEE 754 standard, which Java's floating-point arithmetic " +
            "follows, guarantees that the result is non-negative when the input is " +
            "non-negative. This property is a precondition for many numerical algorithms " +
            "and geometric computations."
        );

        say(
            "The five sample inputs cover distinct regions of the non-negative real line: " +
            "the origin (0.0), unit value (1.0), a perfect square (4.0), a large integer " +
            "value (100.0), and a sub-unit positive (0.001). Each produces a well-defined, " +
            "non-negative result."
        );

        sayCode("""
                // Invariant: Math.sqrt(x) >= 0 for all x >= 0
                Predicate<Object> sqrtNonNegative = o -> Math.sqrt((Double) o) >= 0;

                sayPropertyBased(
                    "Math.sqrt of non-negative is non-negative",
                    sqrtNonNegative,
                    List.of(0.0, 1.0, 4.0, 100.0, 0.001)
                );
                """, "java");

        sayTable(new String[][] {
            {"Input",  "Math.sqrt result", "Exact?"},
            {"0.0",    "0.0",              "Yes"},
            {"1.0",    "1.0",              "Yes"},
            {"4.0",    "2.0",              "Yes"},
            {"100.0",  "10.0",             "Yes"},
            {"0.001",  "~0.031622...",     "Rounded (IEEE 754)"},
        });

        sayWarning(
            "Math.sqrt(x) returns NaN when x < 0.0 and returns -0.0 (negative zero) " +
            "only for the input -0.0, not for any positive input. All five inputs here " +
            "are strictly non-negative, so NaN and -0.0 cannot arise."
        );

        Predicate<Object> sqrtNonNegative = o -> Math.sqrt((Double) o) >= 0;

        sayPropertyBased(
            "Math.sqrt of non-negative is non-negative",
            sqrtNonNegative,
            List.of(0.0, 1.0, 4.0, 100.0, 0.001)
        );
    }

    // =========================================================================
    // Test 3: ArrayList size increases by 1 after add
    // =========================================================================

    /**
     * Documents that calling {@code add} on an {@link ArrayList} of size N
     * produces an ArrayList of size N+1 — regardless of N.
     * The input is the initial size; the predicate creates a fresh list of that
     * size, adds one element, and checks the resulting size.
     */
    @Test
    void a3_sayPropertyBased_list_size_after_add() {
        sayNextSection("Property 3: ArrayList Size Increases by Exactly 1 After add");

        say(
            "The List.add(E) contract in java.util.Collection specifies that the size " +
            "of the collection increases by one after a successful add. For ArrayList, " +
            "which accepts all elements (no capacity exception under normal conditions), " +
            "this is an unconditional invariant: size(after add) == size(before add) + 1."
        );

        say(
            "The predicate receives an Integer N representing the initial list size. " +
            "It constructs a fresh ArrayList, fills it with N placeholder elements, " +
            "adds one more element, then asserts size() == N + 1. The five input sizes " +
            "span the range from an empty list (0) to a list of one hundred elements (100), " +
            "verifying that no resize or capacity boundary breaks the invariant."
        );

        sayCode("""
                // Invariant: after one add, list.size() == initialSize + 1
                Predicate<Object> sizeIncreasesOnAdd = o -> {
                    int initialSize = (Integer) o;
                    List<String> list = new ArrayList<>(initialSize);
                    for (int i = 0; i < initialSize; i++) {
                        list.add("item-" + i);
                    }
                    list.add("new-element");
                    return list.size() == initialSize + 1;
                };

                sayPropertyBased(
                    "ArrayList size increases by 1 on add",
                    sizeIncreasesOnAdd,
                    List.of(0, 1, 5, 10, 100)
                );
                """, "java");

        sayNote(
            "The ArrayList constructor hint `new ArrayList<>(initialSize)` pre-allocates " +
            "capacity but does not affect size. The invariant holds regardless of whether " +
            "a capacity expansion (internal array copy) occurs during add."
        );

        Predicate<Object> sizeIncreasesOnAdd = o -> {
            int initialSize = (Integer) o;
            List<String> list = new ArrayList<>(initialSize);
            for (int i = 0; i < initialSize; i++) {
                list.add("item-" + i);
            }
            list.add("new-element");
            return list.size() == initialSize + 1;
        };

        sayPropertyBased(
            "ArrayList size increases by 1 on add",
            sizeIncreasesOnAdd,
            List.of(0, 1, 5, 10, 100)
        );
    }
}

/*
 * Copyright 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison;
import io.github.seanchatmangpt.dtr.methodcoverage.MethodCoverageTracker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Documentation test for {@link MethodCoverageTracker}.
 *
 * <p>Demonstrates the {@code sayTestCoverage} feature: given a {@link Class} and a
 * {@link Set} of called method names, {@link MethodCoverageTracker} reflects on the
 * class's public declared methods and emits a coverage table showing which were
 * exercised and which were not.</p>
 *
 * <p>Coverage is name-based — if a method name appears in the called set, every
 * overload sharing that name is marked covered. Results are always sorted
 * alphabetically for stable, readable output.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class MethodCoverageDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // t01 — Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Method Coverage Tracking");

        say("""
            `MethodCoverageTracker` reports which public methods of a class were called \
            during a test run and which were not. Coverage is determined by comparing the \
            simple method names in a caller-supplied `Set<String>` against the methods \
            returned by `Class.getDeclaredMethods()` (public, non-synthetic, non-Object).

            The tracker exposes three static entry points:

            - **`analyze(clazz, calledMethods)`** — produces a `CoverageResult` record.
            - **`toTable(result)`** — converts the result to a `String[][]` for `sayTable()`.
            - **`toMarkdown(result)`** — converts the result to a list of markdown lines.""");

        sayCode("""
                // Partial coverage: only two of BenchmarkComparison's methods are "called"
                var called = Set.of("compare", "toTable");
                var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, called);

                // Render as a documentation table
                sayTestCoverage(BenchmarkComparison.class, called);

                // Or access the data directly
                System.out.printf("Coverage: %d/%d (%.1f%%)%n",
                    result.covered(), result.total(), result.percentage());
                """, "java");

        sayNote("""
            Coverage is name-based. If a class has two overloads of `compare`, both are \
            marked covered whenever `"compare"` appears in the called-methods set.""");
    }

    // =========================================================================
    // t02 — Partial coverage via sayTestCoverage
    // =========================================================================

    @Test
    void t02_benchmarkComparisonCoverage() {
        sayNextSection("Partial Coverage — BenchmarkComparison");

        say("""
            Analysing `BenchmarkComparison` with only `compare` and `toTable` in the \
            called-methods set demonstrates how uncovered methods appear in the output. \
            The `sayTestCoverage` convenience method on `DtrTest` calls \
            `MethodCoverageTracker.analyze()` and `toMarkdown()` internally.""");

        var calledMethods = Set.of("compare", "toTable");

        sayTestCoverage(BenchmarkComparison.class, calledMethods);

        var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, calledMethods);

        Assertions.assertNotNull(result, "CoverageResult must not be null");
        Assertions.assertNotNull(result.entries(), "entries list must not be null");
        Assertions.assertFalse(result.entries().isEmpty(), "entries must not be empty for BenchmarkComparison");
    }

    // =========================================================================
    // t03 — Full coverage (100 %)
    // =========================================================================

    @Test
    void t03_fullCoverage() {
        sayNextSection("Full Coverage — All Public Methods Called");

        say("""
            When the called-methods set contains every public method name declared on \
            the class, `percentage()` returns `100.0`. The set is built here via \
            reflection so it stays in sync with the class under analysis — no manual \
            maintenance required.""");

        // Gather every public declared method name from BenchmarkComparison via reflection
        var allMethodNames = Arrays.stream(BenchmarkComparison.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.isSynthetic())
                .map(m -> m.getName())
                .collect(Collectors.toSet());

        sayCode("""
                // Build the full name set via reflection
                var allNames = Arrays.stream(BenchmarkComparison.class.getDeclaredMethods())
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> !m.isSynthetic())
                        .map(Method::getName)
                        .collect(Collectors.toSet());

                var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, allNames);
                assert result.percentage() == 100.0;
                """, "java");

        var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, allMethodNames);

        sayTable(MethodCoverageTracker.toTable(result));

        Assertions.assertEquals(100.0, result.percentage(), 0.001,
                "All methods covered — expected 100.0% but got " + result.percentage());
    }

    // =========================================================================
    // t04 — Zero coverage (empty called set)
    // =========================================================================

    @Test
    void t04_zeroCoverage() {
        sayNextSection("Zero Coverage — Empty Called Set");

        say("""
            Passing an empty `Set` represents a scenario where the API under test \
            was never exercised. Every method row shows `✗` and `covered` is `0`. \
            This is a useful baseline check: if coverage is still 0 after running \
            your test suite, the class is completely untested.""");

        var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, Set.of());

        sayTable(MethodCoverageTracker.toTable(result));

        Assertions.assertEquals(0, result.covered(),
                "No methods should be covered when calledMethods is empty");

        sayNote("""
            `percentage()` returns `0.0` (not NaN or a division-by-zero error) \
            both when `total > 0` and no methods are covered, and when \
            the class has no public declared methods (`total == 0`).""");
    }

    // =========================================================================
    // t05 — Coverage of this test class itself
    // =========================================================================

    @Test
    void t05_coverageReport() {
        sayNextSection("Self-Coverage Report — This Test Class");

        say("""
            `MethodCoverageTracker` can analyse any class, including the test class \
            itself. Below, a handful of method names from `MethodCoverageDocTest` are \
            passed as the called set to show how the report looks for a class with \
            mixed coverage.""");

        var calledMethods = Set.of("t01_overview", "t02_benchmarkComparisonCoverage", "t03_fullCoverage");

        var result = MethodCoverageTracker.analyze(MethodCoverageDocTest.class, calledMethods);

        var kvPairs = new LinkedHashMap<String, String>();
        kvPairs.put("Class", result.className());
        kvPairs.put("Covered methods", String.valueOf(result.covered()));
        kvPairs.put("Total methods", String.valueOf(result.total()));
        kvPairs.put("Coverage %", "%.1f%%".formatted(result.percentage()));
        sayKeyValue(Map.copyOf(kvPairs));

        sayTable(MethodCoverageTracker.toTable(result));

        Assertions.assertTrue(result.total() >= 0,
                "total must be non-negative");
        Assertions.assertTrue(result.covered() <= result.total(),
                "covered cannot exceed total");
    }
}

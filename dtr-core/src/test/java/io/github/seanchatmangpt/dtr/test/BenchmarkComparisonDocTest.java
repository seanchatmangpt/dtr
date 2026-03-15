/*
 * Copyright (C) 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison.ComparisonEntry;
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison.ComparisonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documentation test for {@link BenchmarkComparison}.
 *
 * <p>Demonstrates and documents the multi-task benchmark comparison API by running
 * real measurements against deterministic tasks (String operations, integer arithmetic).
 * The approach follows Joe Armstrong's principle: "The benchmark IS the documentation."
 * Every table rendered here reflects live JVM measurements taken at test-run time.</p>
 *
 * <p>Timing assertions are deliberately absent — wall-clock results are non-deterministic
 * across CI environments. Tests assert only structural invariants: non-null results,
 * correct record shape, and table dimensions.</p>
 *
 * <p>All benchmark calls use minimal warmup (5 rounds) and measure (20 rounds) counts
 * so that the test suite completes quickly in any environment.</p>
 *
 * @see BenchmarkComparison
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class BenchmarkComparisonDocTest extends DtrTest {

    /** Flush and write the generated documentation file after all test methods complete. */
    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Overview and API walkthrough
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Benchmark Comparison — Overview");

        say("""
                `BenchmarkComparison` is DTR's multi-task performance comparison engine. \
                It accepts a map of named `Runnable` tasks, measures each one using \
                `BenchmarkRunner`, ranks them by average nanoseconds (fastest first), \
                and returns an immutable `ComparisonResult` record. The result can be \
                rendered as a `String[][]` table via `toTable()` or as Markdown lines \
                via `toMarkdown()` — both suitable for direct use in DTR documentation.""");

        say("""
                The core design follows two principles. First, every number is real: \
                `BenchmarkRunner` uses `System.nanoTime()` with configurable warmup and \
                measure rounds, so the JIT compiler has stabilised before measurement begins. \
                Second, relative speed is expressed as a ratio against the slowest task — \
                the slowest entry always shows `1.00x`, while faster entries show higher \
                multipliers. This avoids the common mistake of expressing speedup against \
                an arbitrary baseline.""");

        sayCode("""
                // Build a map of label → task (insertion order is preserved during measurement)
                var tasks = new LinkedHashMap<String, Runnable>();
                tasks.put("concat",        () -> "hello".concat(" world"));
                tasks.put("StringBuilder", () -> new StringBuilder("hello").append(" world").toString());
                tasks.put("format",        () -> String.format("%s %s", "hello", "world"));

                // Run with explicit warmup and measure rounds (fast for tests)
                var result = BenchmarkComparison.compare(tasks, 5, 20);

                // Render as a DTR table
                sayTable(BenchmarkComparison.toTable(result));

                // result.fastest() → label of the task with lowest avgNs
                // result.slowest() → label of the task with highest avgNs
                // result.entries() → ranked List<ComparisonEntry>, fastest first
                """, "java");

        sayKeyValue(Map.of(
                "Primary input",  "Map<String, Runnable> — ordered map of label → task",
                "Primary output", "ComparisonResult — immutable record with ranked entries",
                "Ranking basis",  "avgNs ascending (lowest avg nanoseconds = rank 1)",
                "Relative speed", "slowestAvgNs / thisAvgNs (fastest task is highest value)",
                "Since",          "2026.1.0"
        ));

        sayNote("""
                Use `LinkedHashMap` to preserve the insertion order of tasks during \
                measurement. `BenchmarkComparison.compare()` measures tasks in the order \
                they appear in the map's entry set, then re-sorts by `avgNs` for ranking.""");
    }

    // =========================================================================
    // Test 2: Three-way String concat comparison via sayBenchmarkComparison
    // =========================================================================

    @Test
    void t02_stringConcatComparison() {
        sayNextSection("String Concatenation: Three-Way Comparison");

        say("""
                String concatenation is a perennial benchmark subject. Java offers at least \
                three idiomatic approaches: the `+` operator (compiled to `StringBuilder` \
                by javac for literals, but via `StringConcatFactory` invokedynamic for \
                variables), explicit `StringBuilder`, and `String.format()`. Their relative \
                performance differs substantially because `format()` parses a format string \
                at runtime whereas the others do not.""");

        say("""
                The three tasks below use constant strings to eliminate allocation variance \
                and allow the JIT to optimise aggressively. The result shows the relative \
                ranking on the executing JVM — not a universal truth, but an honest \
                measurement of this run.""");

        sayCode("""
                var tasks = new LinkedHashMap<String, Runnable>();
                tasks.put("concat",        () -> "hello".concat(" world"));
                tasks.put("StringBuilder", () -> new StringBuilder("hello").append(" world").toString());
                tasks.put("format",        () -> String.format("%s %s", "hello", "world"));

                sayBenchmarkComparison(tasks);   // renders ranked table via RenderMachine
                """, "java");

        var tasks = new LinkedHashMap<String, Runnable>();
        tasks.put("concat",        () -> "hello".concat(" world"));
        tasks.put("StringBuilder", () -> new StringBuilder("hello").append(" world").toString());
        tasks.put("format",        () -> String.format("%s %s", "hello", "world"));

        // sayBenchmarkComparison delegates to RenderMachineImpl which calls compare() internally.
        // We also call compare() directly so we can assert structural invariants below.
        sayBenchmarkComparison(tasks);

        var result = BenchmarkComparison.compare(tasks, 5, 20);

        sayNote("""
                Timing values vary by JVM warm-up state, CPU frequency scaling, and \
                container resource limits. The ranking may differ between runs, but the \
                structural properties — non-null fastest/slowest labels, non-empty entries \
                list, rank 1 having the lowest avgNs — are invariant.""");

        // Structural assertions — never assert on absolute nanosecond values
        assertNotNull(result, "compare() must return a non-null ComparisonResult");
        assertNotNull(result.fastest(), "fastest() must be non-null");
        assertNotNull(result.slowest(), "slowest() must be non-null");
        assertFalse(result.entries().isEmpty(), "entries() must be non-empty");
        assertTrue(result.entries().size() == 3, "must have exactly 3 ranked entries");

        var rank1 = result.entries().getFirst();
        assertTrue(rank1.rank() == 1, "first entry must carry rank 1");
        assertTrue(rank1.avgNs() >= 0, "avgNs must be non-negative");
        assertTrue(rank1.relativeSpeed() >= 1.0,
                "fastest task must have relativeSpeed >= 1.0 (it is the highest multiplier)");
    }

    // =========================================================================
    // Test 3: toTable() rendering — verify 2D array shape
    // =========================================================================

    @Test
    void t03_tableRendering() {
        sayNextSection("toTable() — Rendering a ComparisonResult as a 2D Array");

        say("""
                `BenchmarkComparison.toTable(result)` converts a `ComparisonResult` into \
                a `String[][]` suitable for passing to `sayTable()`. The first row contains \
                the five column headers; subsequent rows hold one entry each, sorted fastest \
                first. The five columns are: Rank, Label, Avg (ns), Relative Speed, and \
                vs Fastest.""");

        say("""
                The "vs Fastest" column is distinct from "Relative Speed": where Relative \
                Speed compares each task to the slowest (slowest = 1.00x), vs Fastest \
                compares each task to the fastest (fastest = 1.00x, slower tasks > 1.00x). \
                Both columns together give a complete picture of the spread.""");

        sayCode("""
                var tasks = new LinkedHashMap<String, Runnable>();
                tasks.put("arrayAccess", () -> { int[] a = {1,2,3}; return a[0]; });
                tasks.put("stringLen",   () -> "benchmark".length());

                var result = BenchmarkComparison.compare(tasks, 5, 20);
                var table  = BenchmarkComparison.toTable(result);

                // table[0]  → {"Rank", "Label", "Avg (ns)", "Relative Speed", "vs Fastest"}
                // table[1]  → first ranked entry (fastest)
                // table[2]  → second ranked entry
                sayTable(table);
                """, "java");

        var tasks = new LinkedHashMap<String, Runnable>();
        tasks.put("arrayAccess", () -> { var a = new int[]{1, 2, 3}; if (a[0] < 0) throw new AssertionError(); });
        tasks.put("stringLen",   () -> { if ("benchmark".length() < 0) throw new AssertionError(); });

        var result = BenchmarkComparison.compare(tasks, 5, 20);
        var table  = BenchmarkComparison.toTable(result);

        sayTable(table);

        sayNote("""
                `toTable()` is purely a formatting utility — it never re-runs measurements. \
                Pass the same `ComparisonResult` to both `toTable()` and `toMarkdown()` \
                to get consistent numbers across render targets.""");

        // Structural: header row + one row per task = 3 rows total for 2 tasks
        assertNotNull(table, "toTable() must return a non-null array");
        assertTrue(table.length == 3,
                "table must have 3 rows: 1 header + 2 entries; got " + table.length);
        assertTrue(table[0].length == 5,
                "header row must have 5 columns; got " + table[0].length);
        assertTrue(table[1].length == 5,
                "data rows must have 5 columns; got " + table[1].length);

        // Header values
        assertTrue("Rank".equals(table[0][0]), "first header column must be 'Rank'");
        assertTrue("Label".equals(table[0][1]), "second header column must be 'Label'");
    }

    // =========================================================================
    // Test 4: toMarkdown() rendering — verify Markdown line content
    // =========================================================================

    @Test
    void t04_markdownRendering() {
        sayNextSection("toMarkdown() — Rendering a ComparisonResult as Markdown Lines");

        say("""
                `BenchmarkComparison.toMarkdown(result)` converts a `ComparisonResult` into \
                a list of Markdown lines: an H3 heading, a blank line, a GFM pipe-table \
                (header + separator + data rows), another blank line, and a summary sentence. \
                The returned list is suitable for passing line-by-line to `sayRaw()` or \
                joining with `"\\n"` for embedding in external pipelines.""");

        sayCode("""
                var result = BenchmarkComparison.compare(tasks, 5, 20);
                var lines  = BenchmarkComparison.toMarkdown(result);

                // Render each line through the DTR pipeline
                lines.forEach(line -> sayRaw(line));
                """, "java");

        var tasks = new LinkedHashMap<String, Runnable>();
        tasks.put("charAt",      () -> { if ("hello".charAt(0) == 0) throw new AssertionError(); });
        tasks.put("codePointAt", () -> { if ("hello".codePointAt(0) == 0) throw new AssertionError(); });

        var result = BenchmarkComparison.compare(tasks, 5, 20);
        var lines  = BenchmarkComparison.toMarkdown(result);

        say("Rendered output from `toMarkdown()` via `sayRaw()`:");
        for (var line : lines) {
            sayRaw(line);
        }

        sayWarning("""
                Do not embed absolute nanosecond values from `toMarkdown()` output into \
                static documentation. The numbers are JVM-specific and will drift between \
                Java versions, hardware generations, and container resource configurations. \
                Always regenerate by running the tests.""");

        // Structural assertions on the markdown line list
        assertNotNull(lines, "toMarkdown() must return a non-null list");
        assertFalse(lines.isEmpty(), "toMarkdown() must return a non-empty list");

        var firstLine = lines.getFirst();
        assertNotNull(firstLine, "first line must be non-null");
        assertTrue(firstLine.contains("Benchmark"),
                "first line must contain 'Benchmark'; got: " + firstLine);

        // Summary line (last non-empty line) must reference fastest and slowest
        var summaryLine = lines.stream()
                .filter(l -> l.contains("Fastest"))
                .findFirst()
                .orElse(null);
        assertNotNull(summaryLine, "toMarkdown() must include a summary line containing 'Fastest'");
        assertTrue(summaryLine.contains(result.fastest()),
                "summary line must name the fastest task: " + result.fastest());
    }

    // =========================================================================
    // Test 5: Record structure documentation
    // =========================================================================

    @Test
    void t05_recordStructure() {
        sayNextSection("Record Structure — ComparisonResult and ComparisonEntry");

        say("""
                Both result types in `BenchmarkComparison` are Java records: immutable, \
                transparent data carriers with compiler-generated accessors, `equals`, \
                `hashCode`, and `toString`. Records express intent clearly — these types \
                exist to carry data, not to encapsulate behaviour.""");

        say("""
                `ComparisonResult` is the top-level outcome of a `compare()` call. It holds \
                the ranked list of entries and the labels of the fastest and slowest tasks. \
                `ComparisonEntry` is the per-task measurement: label, average nanoseconds, \
                relative speed multiplier, and 1-based rank.""");

        sayCode("""
                // ComparisonResult record
                public record ComparisonResult(
                        List<ComparisonEntry> entries,  // ranked fastest-first
                        String fastest,                 // label of lowest avgNs
                        String slowest) {}              // label of highest avgNs

                // ComparisonEntry record
                public record ComparisonEntry(
                        String label,          // preserved from input map key
                        long   avgNs,          // average ns/invocation from BenchmarkRunner
                        double relativeSpeed,  // slowestAvgNs / avgNs (fastest has highest value)
                        int    rank) {}        // 1-based (1 = fastest)
                """, "java");

        // Document both records via reflection
        sayRecordComponents(ComparisonResult.class);
        sayRecordComponents(ComparisonEntry.class);

        sayTable(new String[][] {
            {"Field", "Record", "Type", "Invariant"},
            {"entries",       "ComparisonResult", "List<ComparisonEntry>", "Non-null, non-empty, sorted by avgNs asc"},
            {"fastest",       "ComparisonResult", "String",                "Non-null; equals entries.getFirst().label()"},
            {"slowest",       "ComparisonResult", "String",                "Non-null; equals entries.getLast().label()"},
            {"label",         "ComparisonEntry",  "String",                "Non-null; preserved from input map key"},
            {"avgNs",         "ComparisonEntry",  "long",                  ">= 0"},
            {"relativeSpeed", "ComparisonEntry",  "double",                ">= 1.0; slowest task is exactly 1.00"},
            {"rank",          "ComparisonEntry",  "int",                   "1-based; rank 1 = fastest task"},
        });

        sayNote("""
                `relativeSpeed` for the fastest task is equal to `slowestAvgNs / fastestAvgNs`, \
                which will be >= 1.0. The slowest task is always exactly 1.00 because it \
                is the baseline: `slowestAvgNs / slowestAvgNs = 1.0`.""");

        // Verify record components exist via reflection
        var resultComponents = ComparisonResult.class.getRecordComponents();
        assertNotNull(resultComponents, "ComparisonResult must have record components");
        assertTrue(resultComponents.length == 3,
                "ComparisonResult must have 3 components (entries, fastest, slowest); got "
                        + resultComponents.length);

        var entryComponents = ComparisonEntry.class.getRecordComponents();
        assertNotNull(entryComponents, "ComparisonEntry must have record components");
        assertTrue(entryComponents.length == 4,
                "ComparisonEntry must have 4 components (label, avgNs, relativeSpeed, rank); got "
                        + entryComponents.length);

        // Verify a live instance satisfies all invariants
        var tasks = new LinkedHashMap<String, Runnable>();
        tasks.put("fast", () -> { if ("x".length() < 0) throw new AssertionError(); });
        tasks.put("slow", () -> {
            long sum = 0;
            for (int i = 0; i < 100; i++) sum += i;
            if (sum < 0) throw new AssertionError();
        });

        var result = BenchmarkComparison.compare(tasks, 5, 20);

        assertNotNull(result.entries(), "entries() must be non-null");
        assertFalse(result.entries().isEmpty(), "entries() must be non-empty");
        assertNotNull(result.fastest(), "fastest() must be non-null");
        assertNotNull(result.slowest(), "slowest() must be non-null");

        var slowestEntry = result.entries().getLast();
        assertTrue(Math.abs(slowestEntry.relativeSpeed() - 1.0) < 0.01,
                "slowest entry relativeSpeed must be ~1.00; got " + slowestEntry.relativeSpeed());

        assertTrue(result.entries().getFirst().rank() == 1,
                "first entry must have rank 1");
    }
}

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
package io.github.seanchatmangpt.dtr.benchmark;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-task benchmark comparison — runs N labeled tasks and produces a comparative
 * performance matrix with relative speedup columns.
 *
 * <p>Tasks are measured using {@link BenchmarkRunner} and sorted by average nanoseconds
 * ascending (fastest first). The slowest task is the baseline at {@code 1.0x}; every
 * other task's {@code relativeSpeed} is {@code slowestAvgNs / thisAvgNs}, so faster
 * tasks yield higher multipliers.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * var tasks = new LinkedHashMap<String, Runnable>();
 * tasks.put("concat",        () -> "hello".concat(" world"));
 * tasks.put("StringBuilder", () -> new StringBuilder("hello").append(" world").toString());
 * tasks.put("format",        () -> String.format("%s %s", "hello", "world"));
 *
 * var result = BenchmarkComparison.compare(tasks, 10, 50);
 * sayTable(BenchmarkComparison.toTable(result));
 * }</pre>
 *
 * <p>Joe Armstrong: "The benchmark IS the documentation."</p>
 *
 * @since 2026.1.0
 */
public final class BenchmarkComparison {

    private BenchmarkComparison() {}

    // -------------------------------------------------------------------------
    // Result records
    // -------------------------------------------------------------------------

    /**
     * A single task's measurement outcome after ranking.
     *
     * @param label         human-readable task name (preserved from the input map key)
     * @param avgNs         average nanoseconds per invocation from {@link BenchmarkRunner}
     * @param relativeSpeed {@code slowestAvgNs / avgNs} — the fastest task has the highest value;
     *                      the slowest task is always {@code 1.0}
     * @param rank          1-based rank (1 = fastest)
     */
    public record ComparisonEntry(String label, long avgNs, double relativeSpeed, int rank) {}

    /**
     * The full comparison result: ranked entries plus named fastest/slowest labels.
     *
     * @param entries  ranked list of {@link ComparisonEntry}, sorted fastest-first
     * @param fastest  label of the task with the lowest {@code avgNs}
     * @param slowest  label of the task with the highest {@code avgNs}
     */
    public record ComparisonResult(List<ComparisonEntry> entries, String fastest, String slowest) {}

    // -------------------------------------------------------------------------
    // Core comparison methods
    // -------------------------------------------------------------------------

    /**
     * Runs all tasks in the map with default benchmark settings (50 warmup / 500 measure rounds),
     * computes relative speedup vs the slowest, and returns a ranked {@link ComparisonResult}.
     *
     * <p>Insertion order of the map is preserved during measurement; entries are then sorted
     * by {@code avgNs} ascending for ranking.</p>
     *
     * @param tasks non-null map of label → task; must contain at least one entry
     * @return a non-null {@link ComparisonResult} with all entries ranked
     * @throws IllegalArgumentException if {@code tasks} is null or empty
     */
    public static ComparisonResult compare(Map<String, Runnable> tasks) {
        return compare(tasks, 50, 500);
    }

    /**
     * Runs all tasks in the map with explicit warmup and measurement round counts,
     * computes relative speedup vs the slowest, and returns a ranked {@link ComparisonResult}.
     *
     * <p>Insertion order of the map is preserved during measurement; entries are then sorted
     * by {@code avgNs} ascending for ranking.</p>
     *
     * @param tasks         non-null map of label → task; must contain at least one entry
     * @param warmupRounds  number of warmup iterations passed to {@link BenchmarkRunner}
     * @param measureRounds number of measured iterations passed to {@link BenchmarkRunner}
     * @return a non-null {@link ComparisonResult} with all entries ranked
     * @throws IllegalArgumentException if {@code tasks} is null or empty
     */
    public static ComparisonResult compare(Map<String, Runnable> tasks, int warmupRounds, int measureRounds) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks must be non-null and non-empty");
        }

        // Measure each task in insertion order, preserving label association
        var rawResults = new LinkedHashMap<String, Long>(tasks.size() * 2);
        for (var entry : tasks.entrySet()) {
            var result = BenchmarkRunner.run(entry.getValue(), warmupRounds, measureRounds);
            rawResults.put(entry.getKey(), result.avgNs());
        }

        // Find the slowest (highest avgNs) — it is the 1.0x baseline
        long slowestAvgNs = rawResults.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElseThrow();

        // Build unsorted entry list
        var unsorted = new ArrayList<ComparisonEntry>(rawResults.size());
        for (var entry : rawResults.entrySet()) {
            long avg = entry.getValue();
            // Avoid division by zero: if avg == 0 treat relative speed as slowestAvgNs
            double relative = avg > 0
                    ? (double) slowestAvgNs / avg
                    : (double) slowestAvgNs;
            // Round to two decimal places for stable display
            double relativeRounded = Math.round(relative * 100.0) / 100.0;
            unsorted.add(new ComparisonEntry(entry.getKey(), avg, relativeRounded, 0 /* rank set below */));
        }

        // Sort fastest-first and assign 1-based ranks
        unsorted.sort(Comparator.comparingLong(ComparisonEntry::avgNs));
        var ranked = new ArrayList<ComparisonEntry>(unsorted.size());
        for (int i = 0; i < unsorted.size(); i++) {
            var e = unsorted.get(i);
            ranked.add(new ComparisonEntry(e.label(), e.avgNs(), e.relativeSpeed(), i + 1));
        }

        String fastest = ranked.getFirst().label();
        String slowest = ranked.getLast().label();

        return new ComparisonResult(List.copyOf(ranked), fastest, slowest);
    }

    // -------------------------------------------------------------------------
    // Table and markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link ComparisonResult} into a 2D {@code String[][]} table suitable
     * for passing to {@code sayTable()}.
     *
     * <p>Headers: {@code ["Rank", "Label", "Avg (ns)", "Relative Speed", "vs Fastest"]}</p>
     * <p>Rows are sorted by {@code avgNs} ascending (fastest first).</p>
     * <p>The "vs Fastest" column expresses each task's time relative to the fastest task:
     * fastest = {@code 1.00x}, slower tasks are {@code > 1.00x}.</p>
     *
     * @param result a non-null {@link ComparisonResult}
     * @return a 2D array with one header row followed by one row per entry
     */
    public static String[][] toTable(ComparisonResult result) {
        var entries = result.entries();
        // The fastest entry is rank 1 (first after sort); its avgNs is the reference for "vs Fastest"
        long fastestAvgNs = entries.getFirst().avgNs();

        // rows = header + one per entry
        var rows = new String[entries.size() + 1][5];
        rows[0] = new String[]{"Rank", "Label", "Avg (ns)", "Relative Speed", "vs Fastest"};

        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            double vsFastest = fastestAvgNs > 0
                    ? Math.round((double) e.avgNs() / fastestAvgNs * 100.0) / 100.0
                    : 1.0;
            rows[i + 1] = new String[]{
                String.valueOf(e.rank()),
                e.label(),
                String.valueOf(e.avgNs()),
                "%.2fx".formatted(e.relativeSpeed()),
                "%.2fx".formatted(vsFastest)
            };
        }
        return rows;
    }

    /**
     * Converts a {@link ComparisonResult} into a list of markdown lines with a heading,
     * table, and a one-sentence summary.
     *
     * <p>The returned list is suitable for joining with {@code "\n"} or passing line-by-line
     * to {@code sayRaw()}.</p>
     *
     * @param result a non-null {@link ComparisonResult}
     * @return mutable list of markdown lines (never null, never empty)
     */
    public static List<String> toMarkdown(ComparisonResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Benchmark Comparison Results");
        lines.add("");
        lines.add("| Rank | Label | Avg (ns) | Relative Speed | vs Fastest |");
        lines.add("|-----:|-------|--------:|---------------:|-----------:|");

        long fastestAvgNs = result.entries().getFirst().avgNs();
        for (var e : result.entries()) {
            double vsFastest = fastestAvgNs > 0
                    ? Math.round((double) e.avgNs() / fastestAvgNs * 100.0) / 100.0
                    : 1.0;
            lines.add("| %d | %s | %d | %.2fx | %.2fx |".formatted(
                    e.rank(), e.label(), e.avgNs(), e.relativeSpeed(), vsFastest));
        }

        lines.add("");
        lines.add("**Fastest:** `%s` (%d ns avg) — **%.2fx** faster than the slowest (`%s`).".formatted(
                result.fastest(),
                result.entries().getFirst().avgNs(),
                result.entries().getLast().relativeSpeed(),
                result.slowest()));

        return lines;
    }
}

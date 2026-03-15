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
package io.github.seanchatmangpt.dtr.fuzz;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generates random inputs and profiles how they distribute across
 * user-defined categories. Useful for documenting edge-case coverage,
 * boundary behavior, and statistical properties of algorithms.
 *
 * <p>Usage example:
 * <pre>{@code
 * FuzzResult result = FuzzProfiler.profile(
 *     "Integer sign distribution",
 *     () -> random.nextInt(200) - 100,
 *     n -> n < 0 ? "negative" : n == 0 ? "zero" : "positive",
 *     10_000
 * );
 * }</pre>
 *
 * <p>The resulting {@link FuzzResult} contains a sorted {@link CategoryCount}
 * list (most-frequent first), each entry carrying a Unicode bar proportional
 * to its share of the total sample count.</p>
 */
public final class FuzzProfiler {

    /** Per-category statistics produced by {@link #profile}. */
    public record CategoryCount(
        String category,
        long count,
        double percentage,
        String bar   // Unicode bar proportional to percentage, width normalised to most common
    ) {}

    /** Full profiling result returned by {@link #profile}. */
    public record FuzzResult(
        String label,
        int sampleCount,
        List<CategoryCount> distribution,
        String mostCommon,
        String leastCommon
    ) {}

    private static final int MAX_BAR_WIDTH = 20;

    private FuzzProfiler() {}

    /**
     * Generates {@code samples} values using {@code generator}, classifies each
     * with {@code categorizer}, and returns a {@link FuzzResult} sorted by
     * frequency descending.
     *
     * @param <T>        the input type
     * @param label      a human-readable label for the profile (used in doc output)
     * @param generator  supplies random inputs — use a seeded {@link java.util.Random}
     *                   for reproducible documentation
     * @param categorizer maps each generated value to a category name
     * @param samples    number of random inputs to generate (e.g. 10_000)
     * @return a {@link FuzzResult} containing the full distribution
     */
    public static <T> FuzzResult profile(
            String label,
            Supplier<T> generator,
            Function<T, String> categorizer,
            int samples) {

        var counts = new LinkedHashMap<String, Long>();

        for (int i = 0; i < samples; i++) {
            String category = categorizer.apply(generator.get());
            counts.merge(category, 1L, Long::sum);
        }

        // Sort by count descending
        var sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        long maxCount = sorted.isEmpty() ? 1L : sorted.getFirst().getValue();

        var distribution = new ArrayList<CategoryCount>(sorted.size());
        for (var entry : sorted) {
            double pct = (100.0 * entry.getValue()) / samples;
            int barLen = (int) Math.round((MAX_BAR_WIDTH * entry.getValue()) / (double) maxCount);
            String bar = "\u2588".repeat(Math.max(barLen, 1));
            distribution.add(new CategoryCount(entry.getKey(), entry.getValue(), pct, bar));
        }

        String mostCommon  = sorted.isEmpty() ? "" : sorted.getFirst().getKey();
        String leastCommon = sorted.isEmpty() ? "" : sorted.getLast().getKey();

        return new FuzzResult(label, samples, List.copyOf(distribution), mostCommon, leastCommon);
    }
}

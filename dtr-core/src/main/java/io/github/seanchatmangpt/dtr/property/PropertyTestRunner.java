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
package io.github.seanchatmangpt.dtr.property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Runs property-based tests — generates random values, applies a predicate, collects
 * counterexamples, and returns a structured result that can be rendered as documentation.
 *
 * <p>Design principles:</p>
 * <ul>
 *   <li>No external dependencies — pure Java 26</li>
 *   <li>The harness never throws; predicate exceptions count as failures</li>
 *   <li>At most 5 counterexamples are collected (collecting more adds no diagnostic value)</li>
 *   <li>A {@link PropertyResult} record is immutable and carries the full test summary</li>
 * </ul>
 *
 * @since 2026.1.0
 */
public final class PropertyTestRunner {

    private static final int MAX_COUNTEREXAMPLES = 5;

    private PropertyTestRunner() {
        // utility class — not instantiable
    }

    // =========================================================================
    // Public result type
    // =========================================================================

    /**
     * Immutable summary of a single property-based test run.
     *
     * @param label          human-readable description of the property under test
     * @param trials         total number of random trials executed
     * @param failures       number of trials where the predicate returned false or threw
     * @param counterexamples up to 5 failing input values (as {@code toString()} representations)
     * @param passed         {@code true} iff {@code failures == 0}
     */
    public record PropertyResult(
            String label,
            int trials,
            int failures,
            List<String> counterexamples,
            boolean passed) {}

    // =========================================================================
    // Runner
    // =========================================================================

    /**
     * Runs {@code trials} iterations of the property defined by {@code gen} + {@code predicate}.
     *
     * <p>For each trial:</p>
     * <ol>
     *   <li>A value is obtained from {@code gen.get()}</li>
     *   <li>{@code predicate.test(value)} is called inside a try-catch</li>
     *   <li>If the predicate returns {@code false} or throws, the trial is counted as a failure
     *       and the value's {@code toString()} (or the exception message) is recorded as a
     *       counterexample, up to the limit of 5</li>
     * </ol>
     *
     * @param label     human-readable description of the property (e.g. "addition is commutative")
     * @param gen       value supplier called once per trial; may return any {@code Object}
     * @param predicate the property under test; must return {@code true} for all valid inputs
     * @param trials    number of random trials to run (positive integer)
     * @return a {@link PropertyResult} summarising the run; never {@code null}; never throws
     */
    public static PropertyResult run(String label,
                                     Supplier<Object> gen,
                                     Predicate<Object> predicate,
                                     int trials) {
        int failures = 0;
        var counterexamples = new ArrayList<String>(MAX_COUNTEREXAMPLES);

        for (int i = 0; i < trials; i++) {
            Object value;
            try {
                value = gen.get();
            } catch (Exception e) {
                // Generator itself failed — treat as a failure with the exception message
                failures++;
                if (counterexamples.size() < MAX_COUNTEREXAMPLES) {
                    counterexamples.add("(generator threw) " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                }
                continue;
            }

            boolean holds;
            String counterexampleLabel;
            try {
                holds = predicate.test(value);
                counterexampleLabel = String.valueOf(value);
            } catch (Exception e) {
                holds = false;
                counterexampleLabel = String.valueOf(value)
                        + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")";
            }

            if (!holds) {
                failures++;
                if (counterexamples.size() < MAX_COUNTEREXAMPLES) {
                    counterexamples.add(counterexampleLabel);
                }
            }
        }

        return new PropertyResult(
                label,
                trials,
                failures,
                List.copyOf(counterexamples),
                failures == 0);
    }

    // =========================================================================
    // Markdown renderer
    // =========================================================================

    /**
     * Converts a {@link PropertyResult} into a list of Markdown lines suitable for
     * direct inclusion in a DTR document.
     *
     * <p>Output structure:</p>
     * <pre>
     * ### Property Test: {label}
     *
     * | Metric     | Value |
     * | ---        | ---   |
     * | Trials     | N     |
     * | Failures   | N     |
     * | Passed     | true  |
     *
     * #### Counterexamples          (only when failures > 0)
     * - value1
     * - value2
     *
     * &gt; [!NOTE]
     * &gt; ✓ Property holds for all N trials.
     *
     * — or —
     *
     * &gt; [!WARNING]
     * &gt; ✗ Property violated in N/M trials.
     * </pre>
     *
     * @param result the result to render; must not be {@code null}
     * @return an unmodifiable list of Markdown lines; never {@code null}
     */
    public static List<String> toMarkdown(PropertyResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Property Test: " + result.label());
        lines.add("");

        // Summary table
        lines.add("| Metric | Value |");
        lines.add("| --- | --- |");
        lines.add("| Trials | " + result.trials() + " |");
        lines.add("| Failures | " + result.failures() + " |");
        lines.add("| Passed | " + result.passed() + " |");
        lines.add("");

        // Counterexamples section — only rendered when failures were found
        if (!result.counterexamples().isEmpty()) {
            lines.add("#### Counterexamples");
            lines.add("");
            for (var example : result.counterexamples()) {
                lines.add("- `" + example + "`");
            }
            lines.add("");
        }

        // Verdict callout
        if (result.passed()) {
            lines.add("> [!NOTE]");
            lines.add("> ✓ Property holds for all " + result.trials() + " trials.");
        } else {
            lines.add("> [!WARNING]");
            lines.add("> ✗ Property violated in " + result.failures()
                    + "/" + result.trials() + " trials.");
        }
        lines.add("");

        return List.copyOf(lines);
    }
}

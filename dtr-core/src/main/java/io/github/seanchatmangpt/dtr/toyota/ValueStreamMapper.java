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
package io.github.seanchatmangpt.dtr.toyota;

import java.util.List;

/**
 * Toyota Value Stream Mapping (VSM) utility for DTR documentation.
 *
 * <p>Models a pipeline as a series of {@link ProcessStep} instances, each labelled
 * with a {@link StepKind} that classifies it as value-adding or one of the four
 * recognised waste categories from the Toyota Production System:</p>
 * <ul>
 *   <li>Waiting (Muda type 1) — idle time between steps</li>
 *   <li>Rework (Muda type 2) — retry or correction loops</li>
 *   <li>Transport (Muda type 3) — unnecessary data movement</li>
 *   <li>Overprocessing (Muda type 4) — work beyond what is required</li>
 * </ul>
 *
 * <p>After mapping, {@link #map(String, List)} computes totals and the
 * Process Cycle Efficiency (PCF = valueAddNs / totalNs * 100). Toyota targets
 * PCF ≥ 80%.</p>
 *
 * @since 2026.1.0
 */
public final class ValueStreamMapper {

    private ValueStreamMapper() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /**
     * Classification of a process step in the Toyota Value Stream Map.
     */
    public enum StepKind {
        /** Directly produces customer value. */
        VALUE_ADD,
        /** Idle time — waiting for a lock, I/O, or upstream step. */
        WASTE_WAITING,
        /** Correction loops — retry, error handling, re-serialisation. */
        WASTE_REWORK,
        /** Unnecessary data movement or copy operations. */
        WASTE_TRANSPORT,
        /** Work performed beyond what the customer requires. */
        WASTE_OVERPROCESSING
    }

    /**
     * A single measured step in the value stream.
     *
     * @param name        human-readable step name
     * @param kind        value-add or waste category
     * @param actualNs    measured wall-clock time in nanoseconds
     * @param description one-sentence explanation of the step
     */
    public record ProcessStep(String name, StepKind kind, long actualNs, String description) {}

    /**
     * Aggregate result of mapping an entire stream.
     *
     * @param streamName    display name for the stream
     * @param steps         ordered list of process steps
     * @param totalNs       sum of all step durations in nanoseconds
     * @param valueAddNs    sum of VALUE_ADD step durations
     * @param wasteNs       sum of all waste step durations
     * @param pcfEfficiency Process Cycle Efficiency as a percentage (0–100)
     */
    public record StreamAnalysis(
            String streamName,
            List<ProcessStep> steps,
            long totalNs,
            long valueAddNs,
            long wasteNs,
            double pcfEfficiency) {}

    // =========================================================================
    // Factory helpers
    // =========================================================================

    /**
     * Times {@code work} with {@link System#nanoTime()} and constructs a
     * {@link ProcessStep} from the measured duration.
     *
     * @param name        step label
     * @param kind        value-add or waste category
     * @param work        the code to execute and time
     * @param description one-sentence explanation
     * @return a ProcessStep with the real measured duration
     */
    public static ProcessStep step(String name, StepKind kind, Runnable work, String description) {
        long start = System.nanoTime();
        work.run();
        long ns = System.nanoTime() - start;
        return new ProcessStep(name, kind, ns, description);
    }

    /**
     * Computes totals and PCF for a list of steps, producing a {@link StreamAnalysis}.
     *
     * @param name  display name for the stream
     * @param steps the ordered steps that make up the stream
     * @return a fully-populated StreamAnalysis
     */
    public static StreamAnalysis map(String name, List<ProcessStep> steps) {
        long totalNs    = 0L;
        long valueAddNs = 0L;
        for (ProcessStep s : steps) {
            totalNs += s.actualNs();
            if (s.kind() == StepKind.VALUE_ADD) {
                valueAddNs += s.actualNs();
            }
        }
        long wasteNs       = totalNs - valueAddNs;
        double pcfEfficiency = totalNs > 0 ? (valueAddNs * 100.0) / totalNs : 0.0;
        return new StreamAnalysis(name, List.copyOf(steps), totalNs, valueAddNs, wasteNs, pcfEfficiency);
    }

    // =========================================================================
    // Formatting
    // =========================================================================

    /**
     * Formats a nanosecond value as a human-readable string.
     *
     * <ul>
     *   <li>&lt; 1,000 ns → "Xns"</li>
     *   <li>&lt; 1,000,000 ns → "X.Xµs"</li>
     *   <li>otherwise → "X.Xms"</li>
     * </ul>
     *
     * @param ns nanoseconds to format
     * @return human-readable string
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return String.format("%.1fµs", ns / 1_000.0);
        } else {
            return String.format("%.1fms", ns / 1_000_000.0);
        }
    }
}

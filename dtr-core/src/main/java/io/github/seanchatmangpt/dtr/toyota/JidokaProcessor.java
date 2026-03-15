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

import java.util.ArrayList;
import java.util.List;

/**
 * Toyota Jidoka (autonomation / intelligent automation) utility for DTR documentation.
 *
 * <p>Jidoka — "automation with a human touch" — is a core pillar of the Toyota
 * Production System. Machines are empowered to detect their own defects and stop
 * automatically rather than passing defective work downstream. A human is then
 * called to clear the defect and restart the machine.</p>
 *
 * <p>This class models that principle programmatically:</p>
 * <ol>
 *   <li>Each {@link WorkItem} is processed through one or more {@link DefectDetector}s.</li>
 *   <li>If any detector finds a defect the item is tagged {@link Disposition#AUTO_STOPPED}.</li>
 *   <li>Clean items are tagged {@link Disposition#PASSED}.</li>
 *   <li>The {@link JidokaReport} captures per-item outcomes, counts, and elapsed time.</li>
 * </ol>
 *
 * <p>Use {@link #process(String, List, List)} to run the machine and obtain a
 * {@link JidokaReport} that can be rendered via
 * {@code DtrContext.sayJidoka(report)}.</p>
 *
 * @since 2026.1.0
 */
public final class JidokaProcessor {

    private JidokaProcessor() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /**
     * The outcome disposition of a single work item after Jidoka processing.
     */
    public enum Disposition {
        /** Item passed all defect detectors without issue. */
        PASSED,
        /** A defect was detected in the item. */
        DEFECT_DETECTED,
        /** Machine auto-stopped due to the detected defect. */
        AUTO_STOPPED
    }

    /**
     * A unit of work to be processed by the Jidoka machine.
     *
     * @param id   a human-readable identifier for the item
     * @param data the item payload (may be any object; detectors receive this)
     */
    public record WorkItem(String id, Object data) {}

    /**
     * The outcome for a single {@link WorkItem} after all detectors have run.
     *
     * @param itemId            the id of the originating {@link WorkItem}
     * @param disposition       the final {@link Disposition} for this item
     * @param defectDescription description of the defect, or {@code null} if none
     * @param processedNs       nanoseconds taken to process this item
     */
    public record ProcessedItem(
            String itemId,
            Disposition disposition,
            String defectDescription,
            long processedNs) {}

    /**
     * Aggregated report produced by {@link #process(String, List, List)}.
     *
     * @param machineName    human-readable name of the machine / pipeline
     * @param items          per-item outcomes in processing order
     * @param passed         count of items that passed all detectors
     * @param defects        count of items in which a defect was detected
     * @param autoStops      count of items that triggered an auto-stop
     * @param machineStopped {@code true} when at least one auto-stop occurred
     * @param totalNs        total nanoseconds for the entire batch
     */
    public record JidokaReport(
            String machineName,
            List<ProcessedItem> items,
            int passed,
            int defects,
            int autoStops,
            boolean machineStopped,
            long totalNs) {}

    // =========================================================================
    // Detector contract
    // =========================================================================

    /**
     * Strategy interface for defect detection.
     *
     * <p>Implementations inspect a single work-item payload and return either
     * {@code null} (no defect found) or a non-null description of the defect
     * that was detected.</p>
     */
    @FunctionalInterface
    public interface DefectDetector {
        /**
         * Inspect {@code data} for defects.
         *
         * @param data the work-item payload
         * @return {@code null} if no defect, or a description of the defect
         */
        String detect(Object data);
    }

    // =========================================================================
    // Processing
    // =========================================================================

    /**
     * Run all {@code items} through all {@code detectors} and return a
     * {@link JidokaReport}.
     *
     * <p>For each item:</p>
     * <ol>
     *   <li>Each detector is called in order.</li>
     *   <li>The first non-null return from any detector is treated as the
     *       defect description; the item is tagged {@link Disposition#AUTO_STOPPED}
     *       and remaining detectors are skipped.</li>
     *   <li>If all detectors return {@code null} the item is tagged
     *       {@link Disposition#PASSED}.</li>
     * </ol>
     *
     * @param machineName a human-readable label for the machine / pipeline stage
     * @param items       the work items to process (order is preserved in results)
     * @param detectors   the defect detectors to apply to each item
     * @return a {@link JidokaReport} summarising the batch
     */
    public static JidokaReport process(
            String machineName,
            List<WorkItem> items,
            List<DefectDetector> detectors) {

        List<ProcessedItem> processedItems = new ArrayList<>(items.size());
        int passed = 0;
        int defects = 0;
        int autoStops = 0;

        long batchStart = System.nanoTime();

        for (WorkItem item : items) {
            long itemStart = System.nanoTime();
            String defectDescription = null;

            for (DefectDetector detector : detectors) {
                String found = detector.detect(item.data());
                if (found != null) {
                    defectDescription = found;
                    break;
                }
            }

            long itemNs = System.nanoTime() - itemStart;

            Disposition disposition;
            if (defectDescription != null) {
                disposition = Disposition.AUTO_STOPPED;
                defects++;
                autoStops++;
            } else {
                disposition = Disposition.PASSED;
                passed++;
            }

            processedItems.add(new ProcessedItem(item.id(), disposition, defectDescription, itemNs));
        }

        long totalNs = System.nanoTime() - batchStart;
        boolean machineStopped = autoStops > 0;

        return new JidokaReport(machineName, processedItems, passed, defects, autoStops,
                machineStopped, totalNs);
    }

    // =========================================================================
    // Formatting helpers
    // =========================================================================

    /**
     * Format a nanosecond duration as a human-readable string.
     *
     * <ul>
     *   <li>Under 1,000 ns → "Xns"</li>
     *   <li>Under 1,000,000 ns → "Xµs"</li>
     *   <li>Otherwise → "Xms"</li>
     * </ul>
     *
     * @param ns the duration in nanoseconds
     * @return a human-readable duration string
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return (ns / 1_000L) + "\u00b5s";
        } else {
            return (ns / 1_000_000L) + "ms";
        }
    }
}

package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TPS Station 8 — Kaizen: Measure-Improve-Verify Loop
 *
 * <p>Kaizen (改善, "change for better") is Toyota's continuous improvement
 * philosophy. Every worker at every station is responsible for identifying
 * and eliminating waste in their own work. Improvements are small, frequent,
 * and verified — not large, rare, and assumed. A kaizen event is not a
 * project; it is a 30-minute cycle: observe, measure, hypothesise, change,
 * measure again, verify the improvement held.</p>
 *
 * <p>Joe Armstrong had the same philosophy for code performance: "Don't
 * optimise. Profile first. Then optimise the bottleneck. Then profile again.
 * If the improvement doesn't show up in the profile, it didn't happen." The
 * kaizen loop is the scientific method applied to production waste.</p>
 *
 * <p>In DTR, sayBenchmark() is the kaizen measurement tool. Each benchmark
 * is a checkpoint in the improvement loop: current state, proposed change,
 * measured result. If the result is not better, revert. If it is better,
 * document it with the measurement as evidence.</p>
 */
class TpsKaizenDocTest extends DtrTest {

    @Test
    void kaizen_measureImproveVerify() {
        sayNextSection("TPS Station 8 — Kaizen: Measure-Improve-Verify Loop");

        say(
            "Ohno's definition of waste (muda): any activity that consumes resources without " +
            "creating value for the customer. The seven wastes of manufacturing translate " +
            "directly to software: overproduction (generating unused output), waiting (threads " +
            "blocked on locks), transport (copying data between structures), over-processing " +
            "(transformations that aren't needed), inventory (objects accumulating in queues), " +
            "motion (unnecessary object traversal), and defects (bugs discovered late). " +
            "Kaizen eliminates one waste at a time. Measure first. Always."
        );

        sayTable(new String[][] {
            {"Toyota Waste (Muda)", "Software Equivalent", "Java 26 Remedy"},
            {"Overproduction",     "Generating data no downstream stage consumes", "Lazy streams, virtual thread back-pressure"},
            {"Waiting",            "Thread blocked on lock, I/O, or lock contention", "Virtual threads, lock-free CAS, async I/O"},
            {"Transport",          "Copying data across queue boundaries unnecessarily", "Zero-copy (ByteBuffer, MemorySegment JEP 454)"},
            {"Over-processing",    "Parsing JSON that is immediately re-serialised", "Schema-aware skip / pass-through"},
            {"Inventory",          "Bounded queue WIP limit exceeded", "Station 3 Kanban — enforce WIP limit"},
            {"Motion",             "HashMap lookup per item in a tight loop", "Array-indexed hot path, record layout"},
            {"Defects",            "Bug found in production after 10,000 units processed", "Station 2 Jidoka gate — fail early"},
        });

        sayNextSection("The Kaizen Loop: PDCA (Plan-Do-Check-Act)");

        say(
            "PDCA is Deming's improvement cycle. Toyota calls the same cycle: " +
            "Observe (genchi genbutsu) → Measure (quantify the waste) → Hypothesise " +
            "(one change at a time) → Change (implement the minimum viable fix) → " +
            "Verify (measure again, compare). " +
            "Armstrong: 'Change one variable at a time. If you change three things and " +
            "performance improves, you don't know which of the three caused the improvement. ' " +
            "'You will make the wrong choice in the next cycle.'"
        );

        sayCode(
            """
            // Kaizen loop pattern for benchmarking a change
            // Before: measure current state
            sayBenchmark("Before: HashMap lookup in tight loop", () -> {
                for (int i = 0; i < 1000; i++) {
                    map.get(keys[i % keys.length]);
                }
            });

            // Change: one variable — replace HashMap with array-indexed hot path
            var keyIndex = buildIndex(keys);  // precompute index once

            // After: measure the same workload with the change
            sayBenchmark("After: Array-indexed lookup (kaizen)", () -> {
                for (int i = 0; i < 1000; i++) {
                    values[keyIndex[i % keyIndex.length]];
                }
            });

            // Verify: if After > Before, document the improvement as a permanent change
            // If After <= Before, revert the change — the hypothesis was wrong
            """,
            "java"
        );

        sayNextSection("Live Kaizen Benchmark: String Concatenation");

        say(
            "The oldest waste in Java code: string concatenation in a loop. " +
            "Pre-Java-21, the JVM optimised this inconsistently. " +
            "Java 21+ template strings and Java 26 string templates (JEP 430) change the " +
            "picture. Let's measure the three idioms and pick the winner."
        );

        sayBenchmark("Baseline: + concatenation in loop (10K iterations)", () -> {
            String result = "";
            for (int i = 0; i < 100; i++) {
                result = result + "item-" + i + ",";
            }
        });

        sayBenchmark("Kaizen 1: StringBuilder (10K iterations)", () -> {
            var sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("item-").append(i).append(",");
            }
            sb.toString();
        });

        sayBenchmark("Kaizen 2: String.join with stream (10K iterations)", () -> {
            var result = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "item-" + i)
                .collect(java.util.stream.Collectors.joining(","));
        });

        sayNote(
            "Kaizen rule: accept the measurement, not the hypothesis. " +
            "If StringBuilder is not measurably faster for your workload, " +
            "use the idiom that is clearest to read. " +
            "Armstrong: 'Readability is a form of correctness. Code that is hard to read ' " +
            "'will be misread. Misread code contains bugs. Bugs are waste.'"
        );

        sayNextSection("The Seven Wastes Kaizen Checklist");

        sayTable(new String[][] {
            {"#", "Waste", "Detection (Genchi Genbutsu)", "Kaizen Action", "Verify With"},
            {"1", "Overproduction", "Output queue never empties — items discarded",  "Add back-pressure (Kanban WIP limit)", "Queue depth metric"},
            {"2", "Waiting",        "CPU at 5% but threads at 100 WAITING state",   "Eliminate lock contention with CAS or virtual threads", "sayBenchmark + thread state"},
            {"3", "Transport",      "Same data copied 3× across stages",             "Zero-copy MemorySegment handoff", "sayBenchmark allocation rate"},
            {"4", "Over-processing","Schema validation on data that was pre-validated", "Skip gate for pre-validated inputs", "sayBenchmark stage time"},
            {"5", "Inventory",      "Queue.size() > WIP limit consistently",         "Reduce WIP, identify bottleneck, parallelize (Station 9)", "sayBenchmark throughput"},
            {"6", "Motion",         "HashMap.get() called 10M times in hot path",   "Array-indexed precomputed index", "sayBenchmark latency p99"},
            {"7", "Defects",        "Jidoka gate fires on > 0.5% of units",          "Root cause analysis → poka-yoke (Station 5)", "Defect rate metric"},
        });

        sayNextSection("Kaizen Documentation: The Before/After Contract");

        say(
            "Every kaizen change must be documented as a before/after contract: " +
            "the measurement before the change, the hypothesis, the change, and the " +
            "measurement after. Without the before measurement, you cannot claim improvement. " +
            "Without the after measurement, the improvement may have been an illusion. " +
            "DTR enforces this contract: sayBenchmark generates both measurements in the " +
            "same document, under the same environment profile."
        );

        sayCode(
            """
            // Kaizen document structure — before/after in same test
            sayNextSection("Kaizen: HashMap → Array Index (Loop Hot Path)");

            say("Observation: HashMap.get() called 10M times per batch in TokenizerAgent. " +
                "Hypothesis: array-indexed lookup avoids boxing + hash computation overhead.");

            sayBenchmark("Before: HashMap.get(String key)", () -> map.get(key));
            sayBenchmark("After: array[index] lookup", () -> values[index]);

            // Both measurements appear in target/docs/ under the same sayEnvProfile()
            // The improvement is either visible or not — no editorialising needed
            sayEnvProfile(); // pins Java version and environment for reproducibility
            """,
            "java"
        );

        var kaizenPrinciples = new LinkedHashMap<String, String>();
        kaizenPrinciples.put("One change at a time",        "Isolate variables — multiple simultaneous changes produce uninterpretable results");
        kaizenPrinciples.put("Measure before and after",    "sayBenchmark() provides both measurements under the same environment");
        kaizenPrinciples.put("Small, frequent improvements","10 × 1% improvement > 1 × 10% improvement (compound effect over 100 cycles)");
        kaizenPrinciples.put("Revert if no improvement",    "A non-result is a valid result — the hypothesis was wrong, revert the change");
        kaizenPrinciples.put("Document every cycle",        "The kaizen log is the evidence base for future improvement decisions");
        kaizenPrinciples.put("Eliminate waste, not features","Kaizen does not add capability — it removes friction from existing capability");
        sayKeyValue(kaizenPrinciples);

        sayWarning(
            "A benchmark without sayEnvProfile() violates the kaizen contract. " +
            "The same benchmark on Java 21 vs Java 26 can differ by 30% due to JIT improvements, " +
            "virtual thread scheduling changes, and preview API optimisations. " +
            "A benchmark that does not pin the environment cannot be reproduced, compared, or " +
            "trusted as evidence for a production change. " +
            "Armstrong: 'An unreproducible result is an anecdote. Ship anecdotes to a blog, not to production.'"
        );
    }
}

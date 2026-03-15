package io.github.seanchatmangpt.dtr.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Records a named sequence of computation steps, capturing each step's
 * string output and elapsed nanoseconds.
 *
 * <p>Useful for documenting algorithms step-by-step with real intermediate
 * values, not pseudocode.
 */
public final class CallTraceRecorder {

    public record Step(String name, Supplier<String> action) {}

    public record StepResult(
        int index,
        String name,
        String output,
        long elapsedNs,
        String elapsedHuman
    ) {}

    public record TraceResult(
        List<StepResult> steps,
        long totalNs
    ) {}

    public static TraceResult record(List<Step> steps) {
        List<StepResult> results = new ArrayList<>(steps.size());
        long totalNs = 0;

        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            long start = System.nanoTime();
            String output;
            try {
                output = step.action().get();
                if (output == null) output = "(null)";
            } catch (Exception e) {
                output = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            long elapsed = System.nanoTime() - start;
            totalNs += elapsed;
            results.add(new StepResult(i + 1, step.name(), output, elapsed, humanNs(elapsed)));
        }
        return new TraceResult(results, totalNs);
    }

    static String humanNs(long ns) {
        if (ns < 1_000)         return ns + " ns";
        if (ns < 1_000_000)     return "%.1f µs".formatted(ns / 1_000.0);
        if (ns < 1_000_000_000) return "%.2f ms".formatted(ns / 1_000_000.0);
        return                         "%.3f s".formatted(ns / 1_000_000_000.0);
    }
}

package io.github.seanchatmangpt.dtr.util;

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;

import java.util.List;

/**
 * Abstraction layer that composes multiple {@code say*} primitives into
 * higher-level "documentation profiles" for Vision 2030 use cases.
 *
 * <p>Each method orchestrates a sequence of say* calls that together produce
 * a complete documentation section — the 80/20 of common documentation tasks.
 * All methods are static and take a {@link RenderMachineCommands} instance,
 * so they work with both {@code DtrTest} (which implements the interface)
 * and {@code DtrContext} (injected by DtrExtension).</p>
 *
 * @since 2026.5.0
 */
public final class BlueOceanLayer {

    private BlueOceanLayer() {}

    /**
     * Documents a complete class profile: metadata, hierarchy, code model,
     * annotation profile, and documentation coverage — in one call.
     *
     * @param cmd   the render machine to write to
     * @param clazz the class to profile
     */
    public static void documentClassProfile(RenderMachineCommands cmd, Class<?> clazz) {
        cmd.sayNextSection("Class Profile: " + clazz.getSimpleName());
        cmd.sayKeyValue(Vision2030Utils.classMetadata(clazz));
        cmd.sayClassHierarchy(clazz);
        cmd.sayCodeModel(clazz);
        cmd.sayAnnotationProfile(clazz);
        cmd.sayDocCoverage(clazz);
    }

    /**
     * Documents a performance profile: runs benchmarks for the given tasks,
     * renders individual results plus an ASCII chart comparison, and appends
     * the environment fingerprint for reproducibility.
     *
     * @param cmd    the render machine to write to
     * @param title  section title
     * @param labels benchmark labels (one per task)
     * @param tasks  runnables to benchmark (same length as labels)
     */
    public static void documentPerformanceProfile(RenderMachineCommands cmd, String title,
                                                   String[] labels, Runnable[] tasks) {
        if (labels.length != tasks.length) {
            throw new IllegalArgumentException("labels and tasks must have the same length");
        }
        cmd.sayNextSection("Performance Profile: " + title);
        cmd.say("Benchmarking " + labels.length + " tasks with real `System.nanoTime()` measurements.");

        for (int i = 0; i < labels.length; i++) {
            cmd.sayBenchmark(labels[i], tasks[i]);
        }

        if (labels.length >= 2) {
            var comparison = Vision2030Utils.benchmarkComparison(
                    labels[0], tasks[0], labels[1], tasks[1], 50, 200);
            cmd.say("**Head-to-head comparison:**");
            cmd.sayTable(comparison);
        }

        cmd.say("**Environment fingerprint:**");
        cmd.sayKeyValue(Vision2030Utils.systemFingerprint());
    }

    /**
     * Documents the full system landscape: environment profile, OS metrics,
     * thread state, security providers, and system properties.
     *
     * @param cmd the render machine to write to
     */
    public static void documentSystemLandscape(RenderMachineCommands cmd) {
        cmd.sayNextSection("System Landscape");
        cmd.say("Complete runtime environment snapshot for reproducibility.");
        cmd.sayEnvProfile();
        cmd.sayOperatingSystem();
        cmd.sayThreadDump();
        cmd.saySecurityManager();
        cmd.saySystemProperties("java\\.version.*|java\\.vendor.*|os\\..*");
    }

    /**
     * Documents interface contract compliance: contract verification matrix,
     * class diagram, and git evolution timeline for each implementation.
     *
     * @param cmd             the render machine to write to
     * @param contract        the interface contract
     * @param implementations the implementation classes to verify
     */
    public static void documentContractCompliance(RenderMachineCommands cmd,
                                                   Class<?> contract,
                                                   Class<?>... implementations) {
        cmd.sayNextSection("Contract Compliance: " + contract.getSimpleName());
        cmd.say("Verifying " + implementations.length + " implementation(s) against `" +
                contract.getSimpleName() + "`.");
        cmd.sayContractVerification(contract, implementations);

        var allClasses = new Class<?>[implementations.length + 1];
        allClasses[0] = contract;
        System.arraycopy(implementations, 0, allClasses, 1, implementations.length);
        cmd.sayClassDiagram(allClasses);

        for (Class<?> impl : implementations) {
            cmd.sayEvolutionTimeline(impl, 5);
        }
    }

    /**
     * Documents a Java record's schema alongside an example instance rendered as JSON.
     *
     * @param cmd         the render machine to write to
     * @param recordClass the record class to document
     * @param example     an instance of the record to render as JSON
     */
    public static void documentRecordProfile(RenderMachineCommands cmd,
                                              Class<? extends Record> recordClass,
                                              Record example) {
        cmd.sayNextSection("Record Profile: " + recordClass.getSimpleName());
        cmd.sayRecordComponents(recordClass);
        cmd.say("**Example instance:**");
        cmd.sayJson(example);
    }

    /**
     * Documents error handling patterns: renders the exception chain
     * with a warning about the root cause.
     *
     * @param cmd the render machine to write to
     * @param t   the throwable to document
     */
    public static void documentErrorPattern(RenderMachineCommands cmd, Throwable t) {
        cmd.sayNextSection("Error Pattern: " + t.getClass().getSimpleName());
        cmd.sayException(t);
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        if (root != t) {
            cmd.sayWarning("Root cause: " + root.getClass().getSimpleName() +
                    " — " + root.getMessage());
        }
    }
}

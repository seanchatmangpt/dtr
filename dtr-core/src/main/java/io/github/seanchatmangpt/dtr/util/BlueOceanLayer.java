package io.github.seanchatmangpt.dtr.util;

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
            throw new IllegalArgumentException(
                    "labels length %d != tasks length %d".formatted(labels.length, tasks.length));
        }
        cmd.sayNextSection("Performance Profile: " + title);
        cmd.say("Benchmarking %d task(s) with real `System.nanoTime()` measurements.".formatted(labels.length));

        for (var i = 0; i < labels.length; i++) {
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
        cmd.say("Verifying %d implementation(s) against `%s`.".formatted(
                implementations.length, contract.getSimpleName()));
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

    // =========================================================================
    // Diagram-Focused and Code-Reflection-Focused Composites
    // =========================================================================

    /**
     * Documents a complete Code Reflection profile for every declared method in
     * a class: method signature via {@code sayCodeModel(Method)}, control flow
     * graph via {@code sayControlFlowGraph(Method)}, and operation profile via
     * {@code sayOpProfile(Method)}.
     *
     * <p>On Java 26+ with {@code @CodeReflection}-annotated methods, this
     * produces rich IR-derived documentation. On older runtimes, graceful
     * fallbacks render method signatures only.</p>
     *
     * @param cmd   the render machine to write to
     * @param clazz the class whose declared methods to profile
     */
    public static void documentCodeReflectionProfile(RenderMachineCommands cmd, Class<?> clazz) {
        cmd.sayNextSection("Code Reflection Profile: " + clazz.getSimpleName());
        cmd.say("Java 26 Code Reflection analysis (JEP 516) for all declared methods in `%s`.".formatted(
                clazz.getSimpleName()));

        Method[] methods = clazz.getDeclaredMethods();
        cmd.say("Found **%d** declared method(s) to analyze.".formatted(methods.length));

        for (Method method : methods) {
            cmd.say("---");
            cmd.say("**Method: `%s`**".formatted(method.getName()));
            cmd.sayCodeModel(method);
            cmd.sayControlFlowGraph(method);
            cmd.sayOpProfile(method);
        }

        cmd.sayNote("Methods annotated with `@CodeReflection` produce full IR analysis. " +
                "Non-annotated methods fall back to signature rendering.");
    }

    /**
     * Documents an architecture diagram for a set of classes: auto-generated
     * class diagram, call graph for the primary class, and a custom Mermaid
     * sequence diagram showing typical interaction flow.
     *
     * @param cmd     the render machine to write to
     * @param title   the architecture section title
     * @param classes the classes to include (first class is used for call graph)
     */
    public static void documentArchitectureDiagram(RenderMachineCommands cmd, String title,
                                                    Class<?>... classes) {
        if (classes.length == 0) {
            throw new IllegalArgumentException("At least one class must be provided");
        }
        cmd.sayNextSection("Architecture Diagram: " + title);
        cmd.say("Multi-perspective architecture documentation for %d class(es).".formatted(classes.length));

        cmd.say("**Class Structure (inheritance and interfaces):**");
        cmd.sayClassDiagram(classes);

        cmd.say("**Call Graph for `%s` (method-to-method calls):**".formatted(classes[0].getSimpleName()));
        cmd.sayCallGraph(classes[0]);

        cmd.say("**Interaction Flow (sequence diagram):**");
        cmd.sayMermaid(buildSequenceDiagram(classes));

        cmd.sayNote("Class diagram and call graph are generated from live reflection " +
                "and Code Reflection IR respectively.");
    }

    /**
     * Documents the "everything" profile for a class — the ultimate one-liner
     * for complete class documentation. Composes class profile, code reflection
     * profile, environment snapshot, documentation coverage, and git history.
     *
     * @param cmd   the render machine to write to
     * @param clazz the class to fully audit
     */
    public static void documentFullAudit(RenderMachineCommands cmd, Class<?> clazz) {
        cmd.sayNextSection("Full Audit: " + clazz.getSimpleName());
        cmd.say("Comprehensive documentation audit for `%s`.".formatted(clazz.getName()));

        documentClassProfile(cmd, clazz);
        documentCodeReflectionProfile(cmd, clazz);

        cmd.sayNextSection("Environment: " + clazz.getSimpleName());
        cmd.sayEnvProfile();

        cmd.sayNextSection("Documentation Coverage: " + clazz.getSimpleName());
        cmd.sayDocCoverage(clazz);

        cmd.sayNextSection("Evolution Timeline: " + clazz.getSimpleName());
        cmd.sayEvolutionTimeline(clazz, 10);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildSequenceDiagram(Class<?>... classes) {
        var sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        for (Class<?> clazz : classes) {
            sb.append("    participant ").append(clazz.getSimpleName()).append("\n");
        }

        if (classes.length >= 2) {
            var primaryName = classes[0].getSimpleName();
            for (var i = 1; i < classes.length; i++) {
                var targetName = classes[i].getSimpleName();
                sb.append("    ").append(primaryName).append("->>")
                        .append(targetName).append(": uses\n");
                sb.append("    ").append(targetName).append("-->>")
                        .append(primaryName).append(": returns\n");
            }
        } else {
            var name = classes[0].getSimpleName();
            var shown = 0;
            for (Method m : classes[0].getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers()) && shown < 5) {
                    sb.append("    Client->>").append(name)
                            .append(": ").append(m.getName()).append("()\n");
                    sb.append("    ").append(name).append("-->>Client: ")
                            .append(m.getReturnType().getSimpleName()).append("\n");
                    shown++;
                }
            }
            if (shown == 0) {
                sb.append("    Note over ").append(name).append(": No public methods to diagram\n");
            }
        }

        return sb.toString();
    }
}

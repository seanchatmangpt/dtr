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
package io.github.seanchatmangpt.dtr.rendermachine;

import java.util.List;
import java.util.Map;

import io.github.seanchatmangpt.dtr.crossref.DocTestRef;

/**
 * Core documentation-output contract for the DTR render machine.
 *
 * <p>Every {@code say*} method maps to a distinct documentation primitive
 * (paragraph, heading, table, code block, etc.).  Implementations route the
 * calls to one or more output engines (Markdown, LaTeX, HTML, blog, …).
 *
 * @since 1.0
 */
public interface RenderMachineCommands {

    /**
     * A text that will be rendered as a paragraph in the documentation.
     * No escaping is done. You can use markdown formatting inside the text.
     *
     * @param text A text that may contain markdown formatting like "This is my **bold** text".
     */
    public void say(String text);

    /**
     * A heading that will appear as a top-level section in the documentation
     * and in the table of contents. No escaping is done.
     *
     * @param headline The section heading text.
     */
    public void sayNextSection(String headline);

    /**
     * Injects raw content directly into the documentation output.
     * Use this for custom markdown or other content that bypasses normal formatting.
     *
     * @param rawMarkdown Raw content to inject (e.g., markdown tables, code blocks, or HTML).
     */
    public void sayRaw(String rawMarkdown);

    /**
     * Renders a markdown table from a 2D string array.
     * The first row is treated as table headers.
     *
     * @param data A 2D array where each row is a list of cells. First row becomes TH.
     */
    public void sayTable(String[][] data);

    /**
     * Renders a code block with optional syntax highlighting language hint.
     *
     * @param code The code content.
     * @param language The programming language for syntax highlighting (e.g., "java", "sql", "json").
     */
    public void sayCode(String code, String language);

    /**
     * Renders a warning callout box (GitHub-style [!WARNING] alert).
     *
     * @param message The warning message.
     */
    public void sayWarning(String message);

    /**
     * Renders an info callout box (GitHub-style [!NOTE] alert).
     *
     * @param message The info message.
     */
    public void sayNote(String message);

    /**
     * Renders key-value pairs in a readable format.
     *
     * @param pairs A map of keys to values.
     */
    public void sayKeyValue(Map<String, String> pairs);

    /**
     * Renders an unordered (bullet) list.
     *
     * @param items List of strings to render as bullet points.
     */
    public void sayUnorderedList(List<String> items);

    /**
     * Renders an ordered (numbered) list.
     *
     * @param items List of strings to render as numbered items.
     */
    public void sayOrderedList(List<String> items);

    /**
     * Serializes an object to JSON and renders it in a code block.
     *
     * @param object The object to serialize (will be rendered as pretty-printed JSON).
     */
    public void sayJson(Object object);

    /**
     * Renders assertion results in a table format with Check and Result columns.
     *
     * @param assertions A map where keys are check descriptions and values are results.
     */
    public void sayAssertions(Map<String, String> assertions);

    /**
     * Renders a cross-reference to another DocTest's section.
     *
     * The reference is resolved using the CrossReferenceIndex and rendered
     * as a markdown link in Markdown mode or a LaTeX \ref{} command in LaTeX mode.
     *
     * @param ref the cross-reference to another DocTest section
     */
    public void sayRef(DocTestRef ref);

    /**
     * Renders a citation reference using BibTeX citation key.
     *
     * @param citationKey The BibTeX citation key to reference.
     */
    public void sayCite(String citationKey);

    /**
     * Renders a citation reference with page number specification.
     *
     * @param citationKey The BibTeX citation key to reference.
     * @param pageRef The page reference (e.g., "42" or "pp. 10-15").
     */
    public void sayCite(String citationKey, String pageRef);

    /**
     * Renders a footnote with the given text.
     *
     * @param text The footnote content.
     */
    public void sayFootnote(String text);

    /**
     * Documents a class's structure using Java reflection — the DTR stand-in for
     * Project Babylon's Code Reflection API (JEP 494).
     *
     * <p>Renders the class's sealed hierarchy (if sealed), record components (if a record),
     * and all public method signatures — derived directly from the class bytecode, not from
     * developer-written descriptions. The documentation cannot drift from the implementation
     * because it IS the implementation.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * sayCodeModel(SayEvent.class);
     * // Renders: sealed interface with all 13 permitted record subtypes
     * // Each subtype's components, each method's signature
     * // All extracted from bytecode via reflection
     * }</pre>
     *
     * @param clazz the class to introspect and document
     */
    void sayCodeModel(Class<?> clazz);

    /**
     * Documents a method's structure using Project Babylon CodeReflection API.
     *
     * <p>On Java 26+, uses {@code java.lang.reflect.code.CodeReflection.reflect(method)}
     * to introspect the method's bytecode operations — control flow, method calls, field
     * accesses, etc. Renders a detailed breakdown of operation types and their counts.</p>
     *
     * <p>On Java 26 and earlier, gracefully falls back to rendering the method signature
     * (parameters with types, return type) extracted via reflection.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * sayCodeModel(SayEvent.class.getMethod("toString"));
     * // Java 26+: Renders operation breakdown (INVOKE: 3, FIELD_READ: 1, etc.)
     * // Java 26-: Renders String toString() signature only
     * }</pre>
     *
     * @param method the method to introspect and document (must not be null)
     */
    void sayCodeModel(java.lang.reflect.Method method);

    /**
     * Documents the current call site using {@link StackWalker}.
     *
     * <p>Blue ocean innovation: the documentation knows exactly where it was written.
     * Every section generated by this call carries provenance — class, method, line number —
     * derived from the live JVM stack at the moment of invocation.</p>
     *
     * <p>Uses {@code StackWalker.getInstance(RETAIN_CLASS_REFERENCE)} to walk the
     * call stack and find the first frame outside of DTR's own machinery.</p>
     */
    void sayCallSite();

    /**
     * Documents all annotations on a class and its methods using reflection.
     *
     * <p>Blue ocean innovation: renders the complete annotation landscape of any class —
     * class-level annotations and per-method annotations in a structured table.
     * No manual listing, no drift — derived from the bytecode at test runtime.</p>
     *
     * @param clazz the class to inspect for annotations
     */
    void sayAnnotationProfile(Class<?> clazz);

    /**
     * Renders the full class hierarchy (superclass chain + interfaces) as a tree.
     *
     * <p>Blue ocean innovation: visualizes the inheritance graph of any class using only
     * {@link Class#getSuperclass()} and {@link Class#getInterfaces()} — no external deps.
     * Documents "where does this class fit in the type hierarchy" automatically.</p>
     *
     * @param clazz the class whose hierarchy to render
     */
    void sayClassHierarchy(Class<?> clazz);

    /**
     * Analyzes a string and renders its structural profile using Java string APIs.
     *
     * <p>Blue ocean innovation: renders word count, line count, character category
     * distribution, and Unicode metrics for any string — using only
     * {@link String#chars()}, {@link String#lines()}, and {@link Character} APIs.
     * Invaluable for documenting text processing and NLP APIs.</p>
     *
     * @param text the string to profile (may be a sample payload, template, etc.)
     */
    void sayStringProfile(String text);

    /**
     * Compares two objects field-by-field using reflection and renders a diff table.
     *
     * <p>Blue ocean innovation: shows exactly what changed between two states of an object —
     * before/after, v1/v2, request/response. Uses {@link java.lang.reflect.Field} with
     * {@code setAccessible(true)} to compare every declared field.
     * Documents "what changed" automatically.</p>
     *
     * @param before the object representing the "before" state
     * @param after  the object representing the "after" state (must be same type as before)
     */
    void sayReflectiveDiff(Object before, Object after);

    // =========================================================================
    // Java 26 Code Reflection API (JEP 516 / Project Babylon)
    // =========================================================================

    /**
     * Documents the control flow graph of a {@code @CodeReflection}-annotated method
     * using the Java 26 Code Reflection API (JEP 516). Renders a Mermaid
     * {@code flowchart TD} diagram where each basic block is a node and branch
     * ops produce directed edges. Falls back to a text note on older runtimes.
     *
     * @param method the method whose CFG to render (should be annotated with
     *               {@code @java.lang.reflect.code.CodeReflection})
     */
    void sayControlFlowGraph(java.lang.reflect.Method method);

    /**
     * Renders a Mermaid {@code graph LR} showing all method-to-method call
     * relationships in the given class, extracted from InvokeOp nodes in each
     * method's Java 26 Code Reflection IR. Only methods annotated with
     * {@code @CodeReflection} contribute edges.
     *
     * @param clazz the class whose call graph to render
     */
    void sayCallGraph(Class<?> clazz);

    /**
     * Renders a lightweight operation-count table for a method using the Java 26
     * Code Reflection API — same IR traversal as {@link #sayCodeModel(java.lang.reflect.Method)}
     * but without the IR excerpt, for quick performance characterization.
     *
     * @param method the method to profile (should be annotated with {@code @CodeReflection})
     */
    void sayOpProfile(java.lang.reflect.Method method);

    // =========================================================================
    // Blue Ocean: Inline Benchmarking
    // =========================================================================

    /**
     * Measures the given task using {@code System.nanoTime()} with default warmup
     * (50 rounds) and measurement (500 rounds) and renders a performance table with
     * avg/min/max/p99 nanoseconds and throughput ops/sec.
     *
     * <p>Uses Java 26 virtual threads for parallel warmup batches to reduce cold-start
     * bias. All measurements are real — no simulation, no hard-coded numbers.</p>
     *
     * @param label a human-readable label for the benchmark
     * @param task  the code to benchmark
     */
    void sayBenchmark(String label, Runnable task);

    /**
     * Measures the given task with explicit warmup and measurement round counts.
     *
     * @param label         a human-readable label for the benchmark
     * @param task          the code to benchmark
     * @param warmupRounds  number of warmup iterations (discarded from results)
     * @param measureRounds number of measured iterations
     */
    void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds);

    // =========================================================================
    // Blue Ocean: Mermaid Diagrams
    // =========================================================================

    /**
     * Renders a raw Mermaid diagram as a fenced {@code mermaid} code block.
     * Mermaid renders natively on GitHub, GitLab, and Obsidian.
     *
     * @param diagramDsl the Mermaid DSL string (e.g., "flowchart TD\n    A --> B")
     */
    void sayMermaid(String diagramDsl);

    /**
     * Auto-generates a Mermaid {@code classDiagram} from the given classes using
     * reflection ({@link Class#getSuperclass()}, {@link Class#getInterfaces()},
     * {@link Class#getDeclaredMethods()}). Inheritance and implementation
     * relationships are rendered as directed edges.
     *
     * @param classes the classes to include in the diagram
     */
    void sayClassDiagram(Class<?>... classes);

    // =========================================================================
    // Blue Ocean: Documentation Coverage
    // =========================================================================

    /**
     * Renders a documentation coverage report for the given classes — which public
     * methods were documented in this test vs. which were not. Coverage is tracked
     * automatically as {@code say*} methods are called.
     *
     * @param classes the classes whose public API to check for documentation coverage
     */
    void sayDocCoverage(Class<?>... classes);

    // =========================================================================
    // 80/20 Low-Hanging Fruit
    // =========================================================================

    /**
     * Renders a zero-parameter environment snapshot: Java version, OS, available
     * processors, max heap (MB), timezone, and DTR version. Useful as a
     * "generated with" footer in any documentation section.
     */
    void sayEnvProfile();

    /**
     * Renders a schema table for a Java record class — component names, types, and
     * any annotations present. Uses {@link Class#getRecordComponents()} (Java 16+).
     *
     * @param recordClass the record class to document
     */
    void sayRecordComponents(Class<? extends Record> recordClass);

    /**
     * Documents an exception — type, message, full cause chain, and the top 5
     * stack frames — in a structured table. Useful in error-handling and resilience
     * documentation sections.
     *
     * @param t the throwable to document (must not be null)
     */
    void sayException(Throwable t);

    /**
     * Renders an inline ASCII horizontal bar chart for numeric data. Bars are drawn
     * with Unicode block characters ({@code ████}) normalized to the maximum value.
     * No external dependencies — pure Java string math.
     *
     * @param label   the chart title
     * @param values  the numeric values (one bar per value)
     * @param xLabels labels for each bar (must have the same length as {@code values})
     */
    void sayAsciiChart(String label, double[] values, String[] xLabels);

    // =========================================================================
    // Bonus: Contract Verification + Git Evolution Timeline
    // =========================================================================

    /**
     * Documents interface contract coverage across implementation classes. For each
     * public method in the contract interface, checks whether each implementation
     * provides a concrete override (✅ direct), inherits it (↗ inherited), or is
     * missing it entirely (❌ MISSING). Uses only standard Java reflection.
     *
     * <p>If the contract is a sealed interface, permitted subclasses are automatically
     * detected so the user does not need to enumerate them.</p>
     *
     * @param contract        the interface whose methods to verify
     * @param implementations zero or more implementation classes to check
     */
    void sayContractVerification(Class<?> contract, Class<?>... implementations);

    /**
     * Derives the git commit history for the source file of the given class using
     * {@code git log --follow} and renders it as a timeline table (commit, date,
     * author, subject). Falls back gracefully with a note if git is unavailable.
     *
     * <p>Follows the same {@code ProcessBuilder} + try/catch + fallback pattern
     * already used in {@code DocMetadata}.</p>
     *
     * @param clazz      the class whose source file history to document
     * @param maxEntries maximum number of commits to include (most recent first)
     */
    void sayEvolutionTimeline(Class<?> clazz, int maxEntries);

    /**
     * Renders the extracted Javadoc for a method: description, parameter table,
     * return value, throws, and {@code @since} — sourced from
     * {@code docs/meta/javadoc.json} (generated by the dtr-javadoc Rust binary).
     *
     * <p>If the JSON index is not present or the method has no entry, this is a no-op.</p>
     *
     * @param method the method whose Javadoc to render (must not be null)
     */
    void sayJavadoc(java.lang.reflect.Method method);

    // =========================================================================
    // Blue Ocean: JVM System Properties Documentation
    // =========================================================================

    /**
     * Documents all JVM system properties for reproducibility and configuration
     * transparency. Renders a sorted markdown table with Property Key and Value columns.
     *
     * <p>Blue ocean innovation: captures the exact JVM configuration at test runtime,
     * including java.home, java.version, user.timezone, file.encoding, user.country,
     * os.name, os.arch, and all other system properties. Essential for documenting
     * configuration-driven behavior and ensuring reproducibility across environments.</p>
     *
     * <p>Uses {@link System#getProperties()} to retrieve all properties and sorts them
     * alphabetically by key for consistent output.</p>
     */
    void saySystemProperties();

    /**
     * Documents JVM system properties matching a regex filter pattern.
     * Renders a sorted markdown table with Property Key and Value columns.
     *
     * <p>Blue ocean innovation: focus documentation on specific property subsets.
     * For example, use {@code "java.*"} to document only Java-related properties,
     * or {@code "user.*"} to document user-specific configuration. Useful for
     * targeted configuration documentation without cluttering output with unrelated properties.</p>
     *
     * <p>Uses {@link java.util.regex.Pattern#compile(String)} with
     * {@code asPredicate()} to filter property keys before rendering.</p>
     *
     * @param regexFilter the regular expression pattern to match property keys
     *                    (e.g., "java.*", "user.*", "os.*")
     * @throws java.util.regex.PatternSyntaxException if the regex pattern is invalid
     */
    void saySystemProperties(String regexFilter);

    /**
     * Documents the Java security environment — security manager presence,
     * installed security providers, and available cryptographic algorithms.
     *
     * <p>Blue ocean innovation: automatically captures the security landscape of the JVM,
     * including which crypto providers are available (e.g., SunJCE, SunRsaSign), their versions,
     * and what cryptographic algorithms are supported. Essential for documenting security-sensitive
     * code, FIPS compliance, and cryptographic operations.</p>
     *
     * <p>Renders three tables:</p>
     * <ol>
     *   <li>Security Manager Status (present/absent)</li>
     *   <li>Security Providers (name, version, info)</li>
     *   <li>Available Cryptographic Algorithms (sample from KeyPairGenerator, Cipher, MessageDigest)</li>
     * </ol>
     */
    void saySecurityManager();

    /**
     * Documents Java 9+ module (JPMS) dependencies and exports for the given classes.
     *
     * <p>Blue ocean innovation: renders the complete JPMS module graph — showing each module's
     * name, automatic status, required modules (with modifiers like TRANSITIVE, STATIC),
     * exported packages (qualified or unqualified), opened packages, services used, and services
     * provided. Essential for documenting modular applications and troubleshooting module access issues.</p>
     *
     * <p>Uses {@link Class#getModule()} to retrieve the module for each class, then extracts
     * the module descriptor via {@link java.lang.Module#getDescriptor()}. Handles named modules
     * and unnamed modules (the classpath) differently — for unnamed modules, renders a note
     * that JPMS descriptors are not available.</p>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * sayModuleDependencies(String.class, List.class);
     * // Renders:
     * // ### Module Dependencies
     * //
     * // #### java.base (named module)
     * // - **Automatic:** No
     * // - **Packages:** 500+ packages
     * // - **Requires:** (none — java.base is the base module)
     * // - **Exports:** java.lang, java.util, java.io, ...
     * // - **Opens:** (none)
     * // - **Uses:** java.lang.System$LoggerFinder
     * // - **Provides:** (none)
     * }</pre>
     *
     * @param classes the classes whose modules to document (may be from different modules)
     */
    void sayModuleDependencies(Class<?>... classes);

    /**
     * Documents the current JVM thread state.
     *
     * <p>Blue ocean innovation: captures and renders a complete thread dump with aggregate
     * metrics and per-thread details. Uses {@link java.lang.management.ManagementFactory#getThreadMXBean()}
     * to introspect the JVM's thread state without external tools.</p>
     *
     * <p>Renders two sections:</p>
     * <ol>
     *   <li><strong>Summary metrics:</strong> total thread count, daemon thread count, peak thread count,
     *       and total started thread count</li>
     *   <li><strong>Thread table:</strong> for each live thread, displays Thread ID, name, state
     *       (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED), and whether alive</li>
     * </ol>
     *
     * <p>Invaluable for concurrency behavior documentation and thread pool sizing decisions.
     * On Java 21+, shows virtual thread usage alongside platform threads.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * sayThreadDump();
     * // Renders:
     * // ## Thread Summary
     * // | Metric | Value |
     * // | --- | --- |
     * // | Thread Count | 42 |
     * // | Daemon Thread Count | 8 |
     * // | Peak Thread Count | 45 |
     * // | Total Started Thread Count | 127 |
     * //
     * // ## Thread Details
     * // | Thread ID | Name | State | Alive | Interrupted |
     * // | --- | --- | --- | --- | --- |
     * // | 1 | main | RUNNABLE | true | N/A |
     * // | 42 | ForkJoinPool.commonPool-worker-1 | WAITING | true | N/A |
     * // }</pre>
     */
    void sayThreadDump();

    /**
     * Documents OS-level environment metrics for platform-specific behavior documentation.
     *
     * <p>Captures operating system metadata using {@link java.lang.management.ManagementFactory}
     * and {@link com.sun.management.OperatingSystemMXBean} (if available). This is essential for:
     * <ul>
     *   <li>Documenting platform-specific behavior (Windows vs Linux vs macOS)</li>
     *   <li>Showing available processor count for parallel stream documentation</li>
     *   <li>Revealing CPU pressure via system load average</li>
     *   <li>Displaying memory headroom (physical and swap space)</li>
     * </ul>
     *
     * <p>Renders a markdown table with basic metrics (OS name, version, arch, processors)
     * and extended metrics (CPU load, memory usage) when available. Falls back gracefully
     * if {@code com.sun.management.OperatingSystemMXBean} is unavailable.</p>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * ### Operating System Metrics
     * | Metric | Value |
     * | --- | --- |
     * | OS Name | Linux |
     * | OS Version | 6.8.0-48-generic |
     * | OS Architecture | amd64 |
     * | Available Processors | 16 |
     * | System Load Average | 3.42 |
     * | Process CPU Load | 12.5% |
     * | System CPU Load | 35.8% |
     * | Total Physical Memory | 32768 MB |
     * | Free Physical Memory | 8192 MB |
     * | Used Physical Memory | 24576 MB |
     * | Total Swap Space | 16384 MB |
     * | Free Swap Space | 12288 MB |
     * }</pre>
     */
    void sayOperatingSystem();

    // -------------------------------------------------------------------------
    // Blue-Ocean Innovations (80/20 swarm — 10 new capabilities)
    // -------------------------------------------------------------------------

    /**
     * Empirically measures an algorithm's runtime at increasing input sizes
     * and infers its complexity class (O(1), O(log n), O(n), O(n log n),
     * O(n²), O(n³)) via log-log regression.
     */
    default void sayTimeComplexity(String label,
            java.util.function.IntFunction<Runnable> factory) {
        var result = io.github.seanchatmangpt.dtr.complexity.TimeComplexityAnalyzer
                .analyze(label, factory);
        String[][] table = new String[result.measurements().length + 1][3];
        table[0] = new String[]{"n", "Avg (ns)", "Relative"};
        long base = result.measurements()[0].nanosAvg();
        for (int i = 0; i < result.measurements().length; i++) {
            var m = result.measurements()[i];
            table[i + 1] = new String[]{
                String.valueOf(m.n()), String.valueOf(m.nanosAvg()),
                String.format("%.1fx", (double) m.nanosAvg() / base)};
        }
        sayTable(table);
        say("Inferred complexity: **" + result.inferredClass() + "**");
    }

    /**
     * Measures heap allocation before and after running {@code work},
     * then renders bytes allocated and GC collections triggered.
     */
    default void sayHeapDelta(String label, Runnable work) {
        var result = io.github.seanchatmangpt.dtr.memory.HeapDeltaTracker
                .track(label, work);
        sayTable(new String[][]{
            {"Metric", "Value"},
            {"Label",        result.label()},
            {"Delta",        result.deltaHuman()},
            {"GC triggered", result.gcCollectionsDelta() > 0
                             ? "yes (" + result.gcCollectionsDelta() + ")" : "no"}
        });
    }

    /**
     * Reflects over a sealed interface or abstract class and renders its
     * complete permitted-subtype tree with record components and depth.
     */
    default void saySealedHierarchy(Class<?> sealedRoot) {
        var result = io.github.seanchatmangpt.dtr.reflectiontoolkit
                .SealedHierarchyAnalyzer.analyze(sealedRoot);
        say("Sealed hierarchy: **" + result.rootName() + "** ("
                + result.subtypes().size() + " permitted subtypes)");
        String[][] table = new String[result.subtypes().size() + 1][4];
        table[0] = new String[]{"Subtype", "Kind", "Components", "Depth"};
        for (int i = 0; i < result.subtypes().size(); i++) {
            var s = result.subtypes().get(i);
            table[i + 1] = new String[]{
                "  ".repeat(s.depth() - 1) + s.name(), s.kind(),
                s.components().isEmpty() ? "—" : s.components(),
                String.valueOf(s.depth())};
        }
        sayTable(table);
    }

    /**
     * Reads the JPMS {@code ModuleDescriptor} for the given class's module
     * and renders requires / exports / opens / uses / provides directives.
     */
    default void sayModuleDescriptor(Class<?> clazz) {
        var report = io.github.seanchatmangpt.dtr.reflectiontoolkit
                .ModuleDescriptorRenderer.render(clazz);
        sayTable(new String[][]{
            {"Property", "Value"},
            {"Module",    report.moduleName()},
            {"Version",   report.version()},
            {"Open",      String.valueOf(report.isOpen())},
            {"Automatic", String.valueOf(report.isAutomatic())}
        });
    }

    /**
     * Times two or more alternative implementations of the same operation
     * and renders a ranked comparison table (name, avg ns, relative speed).
     */
    default void sayAlternatives(
            java.util.List<io.github.seanchatmangpt.dtr.comparison.AlternativesComparer.Alternative> alternatives) {
        var result = io.github.seanchatmangpt.dtr.comparison.AlternativesComparer
                .compare(alternatives);
        String[][] table = new String[result.results().size() + 1][4];
        table[0] = new String[]{"Implementation", "Avg (ns)", "Relative", "Rank"};
        for (int i = 0; i < result.results().size(); i++) {
            var r = result.results().get(i);
            table[i + 1] = new String[]{r.name(), String.valueOf(r.avgNanos()),
                String.format("%.2fx", r.relativeToFastest()), r.rank()};
        }
        sayTable(table);
        say("Fastest: **" + result.fastestName() + "**");
    }

    /**
     * Probes a shared operation from N virtual threads simultaneously,
     * reports exceptions detected and thread-safety verdict.
     */
    default void sayThreadSafety(String label, Runnable sharedOperation,
            int threads, int operationsEach) {
        var result = io.github.seanchatmangpt.dtr.concurrency.ThreadSafetyProbe
                .probe(label, sharedOperation, threads, operationsEach);
        sayTable(new String[][]{
            {"Metric", "Value"},
            {"Threads",     String.valueOf(result.threads())},
            {"Total ops",   String.valueOf(result.totalOperations())},
            {"Exceptions",  String.valueOf(result.exceptionsDetected())},
            {"Throughput",  String.format("%.0f ops/s", result.operationsPerSecond())},
            {"Thread-safe", result.appearsThreadSafe() ? "✓ yes" : "✗ NO"}
        });
    }

    /**
     * Evaluates named pre/post/invariant conditions and renders a ✓/✗ table
     * proving each contract holds at documentation time.
     */
    default void sayInvariantTable(
            io.github.seanchatmangpt.dtr.contract.InvariantTable.InvariantResult result) {
        String[][] table = new String[result.rows().size() + 1][3];
        table[0] = new String[]{"Invariant", "Kind", "Result"};
        for (int i = 0; i < result.rows().size(); i++) {
            var row = result.rows().get(i);
            table[i + 1] = new String[]{row.name(), row.kind(), row.symbol()};
        }
        sayTable(table);
        say("**" + result.passing() + " passing**, **" + result.failing() + " failing**");
    }

    /**
     * Executes named steps sequentially, captures each step's string output
     * and elapsed time, renders a step → output → timing table.
     */
    default void sayCallTrace(
            java.util.List<io.github.seanchatmangpt.dtr.trace.CallTraceRecorder.Step> steps) {
        var trace = io.github.seanchatmangpt.dtr.trace.CallTraceRecorder.record(steps);
        String[][] table = new String[trace.steps().size() + 1][4];
        table[0] = new String[]{"#", "Step", "Output", "Elapsed"};
        for (int i = 0; i < trace.steps().size(); i++) {
            var s = trace.steps().get(i);
            table[i + 1] = new String[]{String.valueOf(s.index()),
                s.name(), s.output(), s.elapsedHuman()};
        }
        sayTable(table);
        say("Total: **" + io.github.seanchatmangpt.dtr.trace.CallTraceRecorder
                .humanNs(trace.totalNs()) + "**");
    }

    /**
     * Renders a feature × version compatibility matrix with ✓/✗/⚡ cells.
     */
    default void sayVersionMatrix(
            io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder.MatrixResult matrix) {
        int cols = matrix.versions().size() + 2;
        String[][] table = new String[matrix.rows().size() + 1][cols];
        table[0] = new String[cols];
        table[0][0] = "Feature"; table[0][1] = "JEP";
        for (int v = 0; v < matrix.versions().size(); v++) table[0][v + 2] = matrix.versions().get(v);
        for (int r = 0; r < matrix.rows().size(); r++) {
            var row = matrix.rows().get(r);
            table[r + 1] = new String[cols];
            table[r + 1][0] = row.featureName(); table[r + 1][1] = row.jepRef();
            for (int v = 0; v < matrix.versions().size(); v++) {
                table[r + 1][v + 2] = row.versionValues()
                    .getOrDefault(matrix.versions().get(v),
                        io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder.CellValue.NA)
                    .symbol();
            }
        }
        sayTable(table);
    }

    /**
     * Documents a {@link io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.SupervisionReport}
     * as a structured table plus a summary line following Joe Armstrong's
     * "Let It Crash" supervision philosophy.
     *
     * <p>Renders a 5-column table (Process | Action | Outcome | Elapsed | Restarts)
     * with one row per {@link io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.CrashResult},
     * followed by a bold summary: "N crashes, M total restarts — Let It Crash ✓".</p>
     *
     * @param report the supervision report produced by
     *               {@link io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner#supervise}
     */
    default void sayLetItCrash(
            io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.SupervisionReport report) {

        var results = report.results();
        String[][] table = new String[results.size() + 1][5];
        table[0] = new String[]{"Process", "Action", "Outcome", "Elapsed", "Restarts"};

        // Count restarts per process name for the Restarts column
        java.util.Map<String, Integer> restartsByProcess = new java.util.LinkedHashMap<>();
        for (var r : results) {
            if (r.action().startsWith("restart-")) {
                restartsByProcess.merge(r.processName(), 1, Integer::sum);
            }
        }

        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            int processRestarts = restartsByProcess.getOrDefault(r.processName(), 0);
            table[i + 1] = new String[]{
                r.processName(),
                r.action(),
                r.outcome(),
                io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.humanNs(r.elapsedNs()),
                r.action().equals("start") ? String.valueOf(processRestarts) : ""
            };
        }

        sayTable(table);
        say("**" + report.totalCrashes() + " crashes**, **"
                + report.totalRestarts() + " total restarts** — Let It Crash ✓");
    }

    /**
     * Generates {@code samples} random inputs, categorizes each, and renders
     * a distribution table with counts, percentages, and Unicode bar chart.
     */
    default <T> void sayFuzzProfile(String label,
            java.util.function.Supplier<T> generator,
            java.util.function.Function<T, String> categorizer,
            int samples) {
        var result = io.github.seanchatmangpt.dtr.fuzz.FuzzProfiler
                .profile(label, generator, categorizer, samples);
        String[][] table = new String[result.distribution().size() + 1][4];
        table[0] = new String[]{"Category", "Count", "Pct", "Distribution"};
        for (int i = 0; i < result.distribution().size(); i++) {
            var c = result.distribution().get(i);
            table[i + 1] = new String[]{c.category(), String.valueOf(c.count()),
                String.format("%.1f%%", c.percentage()), c.bar()};
        }
        sayTable(table);
        say("Most common: **" + result.mostCommon() + "** | "
                + "Least common: **" + result.leastCommon() + "** | "
                + "Total samples: " + result.sampleCount());
    }

    // =========================================================================
    // Blue Ocean: Actor Mailbox (Joe Armstrong Message-Passing Concurrency)
    // =========================================================================

    /**
     * Documents a Joe Armstrong-style actor-model simulation run as a structured
     * table plus a summary sentence.
     *
     * <p>Renders one row per actor showing: actor name, messages received count,
     * messages sent (replies) count, and processing time. Follows the row table
     * with a bold summary: total messages and actor count.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * ActorMailbox.ActorReport report = ActorMailbox.simulate(
     *     "Counter System", actors, initialMessages);
     * sayActorMailbox(report);
     * // | Actor | Messages Received | Messages Sent | Processing Time |
     * // | counter | 3 | 2 | 42us |
     * // **5 total messages** across **2 actors** -- Actor Model check
     * }</pre>
     *
     * @param report the actor system report produced by
     *               {@link io.github.seanchatmangpt.dtr.actor.ActorMailbox#simulate}
     */
    default void sayActorMailbox(
            io.github.seanchatmangpt.dtr.actor.ActorMailbox.ActorReport report) {
        var traces = report.actors();
        String[][] table = new String[traces.size() + 1][4];
        table[0] = new String[]{"Actor", "Messages Received", "Messages Sent", "Processing Time"};
        for (int i = 0; i < traces.size(); i++) {
            var t = traces.get(i);
            table[i + 1] = new String[]{
                t.actorName(),
                String.valueOf(t.received().size()),
                String.valueOf(t.sent().size()),
                io.github.seanchatmangpt.dtr.actor.ActorMailbox.humanNs(t.processingNs())
            };
        }
        sayTable(table);
        say("**" + report.totalMessages() + " total messages** across **"
                + traces.size() + " actors** \u2014 Actor Model \u2713");
    }

    // =========================================================================
    // Blue Ocean: Toyota Value Stream Mapping
    // =========================================================================

    /**
     * Documents a Toyota Value Stream Map — classifies each pipeline step as
     * value-add or one of four waste categories, then renders a step-by-step
     * table and a Process Cycle Efficiency (PCF) summary line.
     *
     * <p>PCF = valueAddNs / totalNs × 100. Toyota targets PCF ≥ 80%.
     * A warning is emitted when PCF &lt; 50%; a note is emitted when PCF ≥ 80%.</p>
     *
     * @param stream the {@link io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.StreamAnalysis}
     *               produced by {@link io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper#map}
     */
    default void sayValueStream(
            io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.StreamAnalysis stream) {

        var steps = stream.steps();
        long totalNs = stream.totalNs();

        String[][] table = new String[steps.size() + 1][5];
        table[0] = new String[]{"Step", "Kind", "Time", "% of Total", "Description"};
        for (int i = 0; i < steps.size(); i++) {
            var s = steps.get(i);
            String pct = totalNs > 0
                    ? String.format("%.1f%%", (s.actualNs() * 100.0) / totalNs)
                    : "n/a";
            table[i + 1] = new String[]{
                s.name(),
                s.kind().name(),
                io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.humanNs(s.actualNs()),
                pct,
                s.description()
            };
        }
        sayTable(table);

        double pcf      = stream.pcfEfficiency();
        double wastePct = 100.0 - pcf;
        say(String.format(
                "PCF: **%.1f%%** value-add | Waste: **%.1f%%** | Total: **%s**",
                pcf, wastePct,
                io.github.seanchatmangpt.dtr.toyota.ValueStreamMapper.humanNs(totalNs)));

        if (pcf < 50.0) {
            sayWarning("PCF < 50% \u2014 majority of time is waste. Apply kaizen.");
        } else if (pcf >= 80.0) {
            sayNote("PCF \u2265 80% \u2014 lean stream. Toyota target achieved.");
        }
    }

    // =========================================================================
    // Blue Ocean: Toyota Kanban Pull-System Visualization
    // =========================================================================

    /**
     * Renders a Toyota Production System kanban board snapshot as structured
     * documentation: a column stats table, flow efficiency summary, blocked card
     * count, and a WIP-limit warning when any column is over its limit.
     *
     * <p>Flow efficiency is the percentage of cards in the DONE state.
     * WIP (Work In Progress) limits enforce the pull-system discipline:
     * no new work enters a column until existing work exits it.</p>
     *
     * @param board the board snapshot to render (computed via
     *              {@link io.github.seanchatmangpt.dtr.toyota.KanbanBoard#snapshot})
     */
    default void sayKanbanBoard(
            io.github.seanchatmangpt.dtr.toyota.KanbanBoard.BoardSnapshot board) {

        say("# Kanban Board: " + board.boardName());

        var columns = board.columns();
        String[][] table = new String[columns.size() + 1][5];
        table[0] = new String[]{"Column", "Cards", "WIP Limit", "Over WIP?", "Avg Cycle"};
        for (int i = 0; i < columns.size(); i++) {
            var col = columns.get(i);
            String wipLabel = col.wipLimit() == 0 ? "\u2014" : String.valueOf(col.wipLimit());
            String overWip = col.overWip() ? "YES" : "no";
            String avgCycle = col.avgCycleMs() == 0.0 ? "\u2014"
                    : String.format("%.1f ms", col.avgCycleMs());
            table[i + 1] = new String[]{
                col.name(),
                String.valueOf(col.count()),
                wipLabel,
                overWip,
                avgCycle
            };
        }
        sayTable(table);

        say(String.format(
                "Flow efficiency: **%.1f%%** | Blocked: **%d** | Total: **%d**",
                board.flowEfficiency(), board.blockedCount(), board.totalCards()));

        long overWipCount = columns.stream().filter(
                io.github.seanchatmangpt.dtr.toyota.KanbanBoard.ColumnStats::overWip).count();
        if (overWipCount > 0) {
            sayWarning("WIP limit exceeded in " + overWipCount
                    + " column" + (overWipCount == 1 ? "" : "s")
                    + " \u2014 pull, don\u2019t push!");
        }
    }

    // =========================================================================
    // Blue Ocean: Toyota Kaizen Cycle (Continuous Improvement Measurement)
    // =========================================================================

    /**
     * Documents a {@link io.github.seanchatmangpt.dtr.toyota.KaizenCycle.KaizenReport}
     * as a structured performance trend table plus a summary line.
     *
     * <p>Renders one row per cycle showing Cycle, Avg (ns), Min (ns), Max (ns), and
     * Improvement %. Follows the table with a bold baseline → final summary and
     * total improvement. Emits a note when improvement exceeds 20% and a warning
     * when the process has not yet converged.</p>
     *
     * @param report the Kaizen report produced by
     *               {@link io.github.seanchatmangpt.dtr.toyota.KaizenCycle#run}
     */
    default void sayKaizenCycle(
            io.github.seanchatmangpt.dtr.toyota.KaizenCycle.KaizenReport report) {

        var cycles = report.cycles();
        String[][] table = new String[cycles.size() + 1][5];
        table[0] = new String[]{"Cycle", "Avg (ns)", "Min (ns)", "Max (ns)", "Improvement %"};

        for (int i = 0; i < cycles.size(); i++) {
            var c = cycles.get(i);
            table[i + 1] = new String[]{
                String.valueOf(c.cycle()),
                String.valueOf(c.avgNs()),
                String.valueOf(c.minNs()),
                String.valueOf(c.maxNs()),
                String.format("%.2f%%", c.improvementPct())
            };
        }
        sayTable(table);

        say("Baseline: **" + report.baselineNs() + "ns** \u2192 Final: **"
                + report.finalNs() + "ns** | Total improvement: **"
                + String.format("%.2f%%", report.totalImprovementPct())
                + "** | Converged: **" + (report.converged() ? "yes" : "no") + "**");

        if (report.totalImprovementPct() > 20.0) {
            sayNote("Significant kaizen improvement \u2014 "
                    + String.format("%.2f%%", report.totalImprovementPct())
                    + " faster than baseline");
        }

        if (!report.converged()) {
            sayWarning("Process has not converged \u2014 more kaizen cycles needed");
        }
    }

    // =========================================================================
    // Blue Ocean: Toyota Andon Board — Stop-the-Line Health Dashboard
    // =========================================================================

    /**
     * Renders a Toyota Andon system health dashboard from a
     * {@link io.github.seanchatmangpt.dtr.toyota.AndonBoard.BoardState}.
     *
     * <p>Displays the overall line status (RUNNING or STOPPED), a per-station
     * signal table, aggregate counts, and appropriate warnings or notes based on
     * station health. Follows Toyota's Andon philosophy: any RED station stops the
     * line; YELLOW stations require monitoring.</p>
     *
     * <p>Renders:</p>
     * <ol>
     *   <li>Bold summary line: line name and RUNNING/STOPPED status</li>
     *   <li>Table: Station | Signal | Status | Defect</li>
     *   <li>Count summary: GREEN: N | YELLOW: M | RED: K</li>
     *   <li>Warning if redCount &gt; 0 (line stopped due to defects)</li>
     *   <li>Note if line is running but yellowCount &gt; 0 (degraded stations)</li>
     * </ol>
     *
     * @param board the evaluated board state produced by
     *              {@link io.github.seanchatmangpt.dtr.toyota.AndonBoard#evaluate}
     */
    default void sayAndonBoard(
            io.github.seanchatmangpt.dtr.toyota.AndonBoard.BoardState board) {

        say("**Andon Board: " + board.lineName() + "** \u2014 Line "
                + (board.lineRunning() ? "\uD83D\uDFE2 RUNNING" : "\uD83D\uDD34 STOPPED"));

        var stations = board.stations();
        String[][] table = new String[stations.size() + 1][4];
        table[0] = new String[]{"Station", "Signal", "Status", "Defect"};
        for (int i = 0; i < stations.size(); i++) {
            var s = stations.get(i);
            String defectMsg = s.defect() != null
                    ? s.defect().getClass().getSimpleName() + ": " + s.defect().getMessage()
                    : "\u2014";
            String signalLabel = switch (s.signal()) {
                case GREEN  -> "\uD83D\uDFE2 GREEN";
                case YELLOW -> "\uD83D\uDFE1 YELLOW";
                case RED    -> "\uD83D\uDD34 RED";
            };
            table[i + 1] = new String[]{s.name(), signalLabel, s.status(), defectMsg};
        }
        sayTable(table);

        say("GREEN: " + board.greenCount()
                + " | YELLOW: " + board.yellowCount()
                + " | RED: " + board.redCount());

        if (board.redCount() > 0) {
            sayWarning("Line stopped \u2014 " + board.redCount()
                    + " station(s) have defects. Pull the Andon cord!");
        }

        if (board.lineRunning() && board.yellowCount() > 0) {
            sayNote("Degraded stations detected \u2014 monitor closely");
        }
    }

    // =========================================================================
    // Blue Ocean: Toyota Poka-Yoke (Mistake-Proofing)
    // =========================================================================

    /**
     * Documents a Toyota Poka-Yoke validation report — renders each guard's result
     * in a table and emits a summary with a pass/block verdict.
     *
     * <p>Poka-Yoke (mistake-proofing) prevents defects by evaluating a set of guards
     * before a process is allowed to proceed. This method documents which guards were
     * checked, whether they detected defects, and whether the process was allowed or
     * blocked as a result.</p>
     *
     * <p>Renders:</p>
     * <ol>
     *   <li>A status line: process name and ALLOWED / BLOCKED verdict</li>
     *   <li>A 5-column table: Guard | Type | Status | Description | Value Tested</li>
     *   <li>A summary line: triggered count, safe count, total</li>
     *   <li>A warning (if blocked) or note (if allowed)</li>
     * </ol>
     *
     * @param report the {@link io.github.seanchatmangpt.dtr.toyota.PokaYoke.PokaYokeReport}
     *               produced by {@link io.github.seanchatmangpt.dtr.toyota.PokaYoke#validate}
     */
    default void sayPokaYoke(
            io.github.seanchatmangpt.dtr.toyota.PokaYoke.PokaYokeReport report) {

        say("**Poka-Yoke: " + report.processName() + "** \u2014 Process "
                + (report.processAllowed() ? "\u2713 ALLOWED" : "\u2717 BLOCKED"));

        var guards = report.guards();
        String[][] table = new String[guards.size() + 1][5];
        table[0] = new String[]{"Guard", "Type", "Status", "Description", "Value Tested"};
        for (int i = 0; i < guards.size(); i++) {
            var g = guards.get(i);
            table[i + 1] = new String[]{
                g.guardName(),
                g.type().name(),
                g.triggered() ? "\u2717 TRIGGERED" : "\u2713 safe",
                g.description(),
                g.testedValue() == null ? "null" : String.valueOf(g.testedValue())
            };
        }
        sayTable(table);

        int n = report.triggered();
        int m = report.safe();
        int k = guards.size();
        say("Guards triggered: **" + n + "** | Safe: **" + m + "** | Total: **" + k + "**");

        if (!report.processAllowed()) {
            sayWarning("Process blocked by " + n
                    + " Poka-Yoke guard(s) \u2014 defect prevented");
        } else {
            sayNote("All Poka-Yoke guards passed \u2014 mistake-proofing verified");
        }
    }

    // =========================================================================
    // Blue Ocean: Toyota Jidoka (Autonomation / Intelligent Automation)
    // =========================================================================

    /**
     * Documents a Toyota Jidoka processing run — per-item defect disposition table,
     * pass/defect/auto-stop counts, and a machine-status summary line.
     *
     * <p>Jidoka ("automation with a human touch") empowers machines to detect their
     * own defects and stop automatically rather than passing bad work downstream.
     * This method renders the full audit trail of a
     * {@link io.github.seanchatmangpt.dtr.toyota.JidokaProcessor#process} run as
     * structured documentation.</p>
     *
     * <p>Renders:</p>
     * <ol>
     *   <li>A status headline: machine name + running/stopped indicator.</li>
     *   <li>A 4-column table: Item ID | Disposition | Defect Description | Time.</li>
     *   <li>A bold summary line: passed / defects / auto-stops of total.</li>
     *   <li>A warning when the machine stopped, or a note when all items passed.</li>
     * </ol>
     *
     * @param report the report produced by
     *               {@link io.github.seanchatmangpt.dtr.toyota.JidokaProcessor#process}
     */
    default void sayJidoka(
            io.github.seanchatmangpt.dtr.toyota.JidokaProcessor.JidokaReport report) {

        String status = report.machineStopped()
                ? "\uD83D\uDD34 STOPPED (defect detected)"
                : "\uD83D\uDFE2 RUNNING";
        say("**Jidoka: " + report.machineName() + "** \u2014 Machine " + status);

        var items = report.items();
        String[][] table = new String[items.size() + 1][4];
        table[0] = new String[]{"Item ID", "Disposition", "Defect Description", "Time"};
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            table[i + 1] = new String[]{
                item.itemId(),
                item.disposition().name(),
                item.defectDescription() != null ? item.defectDescription() : "\u2014",
                io.github.seanchatmangpt.dtr.toyota.JidokaProcessor.humanNs(item.processedNs())
            };
        }
        sayTable(table);

        int total = items.size();
        say("Passed: **" + report.passed() + "** | Defects: **" + report.defects()
                + "** | Auto-Stops: **" + report.autoStops() + "** of **" + total + "** items");

        if (report.machineStopped()) {
            sayWarning("Jidoka auto-stop triggered \u2014 human intervention required to clear defect");
        } else {
            sayNote("Jidoka: all items passed autonomous quality check");
        }
    }

    // =========================================================================
    // Toyota Production System: Takt Time
    // =========================================================================

    /**
     * Documents a Toyota Takt Time analysis as a structured metrics table plus
     * a summary line comparing cycle time to takt time.
     *
     * <p>Takt time ({@code T}) is the rhythm of customer demand:
     * {@code T = availableTime / customerDemand}. A process is on-pace when its
     * average cycle time ({@code C}) is at or below takt time.</p>
     *
     * <p>Renders:</p>
     * <ol>
     *   <li>A metric table with all {@link io.github.seanchatmangpt.dtr.toyota.TaktTimer.TaktAnalysis} fields</li>
     *   <li>A summary line: Takt | Cycle | Utilization — meets/behind demand</li>
     *   <li>A {@code [!WARNING]} block if cycle exceeds takt</li>
     *   <li>A {@code [!NOTE]} block if utilization is below 60%</li>
     * </ol>
     *
     * @param analysis the takt analysis produced by
     *                 {@link io.github.seanchatmangpt.dtr.toyota.TaktTimer#measure}
     */
    default void sayTaktTime(
            io.github.seanchatmangpt.dtr.toyota.TaktTimer.TaktAnalysis analysis) {

        sayTable(new String[][]{
            {"Metric", "Value"},
            {"Process",            analysis.processName()},
            {"Units produced",     String.valueOf(analysis.unitsProduced())},
            {"Customer demand",    String.valueOf(analysis.customerDemand())},
            {"Available time",     io.github.seanchatmangpt.dtr.toyota.TaktTimer.humanNs(analysis.availableTimeNs())},
            {"Takt time (T)",      io.github.seanchatmangpt.dtr.toyota.TaktTimer.humanNs(analysis.taktTimeNs())},
            {"Avg cycle time (C)", io.github.seanchatmangpt.dtr.toyota.TaktTimer.humanNs(analysis.avgCycleNs())},
            {"Utilization",        String.format("%.1f%%", analysis.utilizationPct())},
            {"Meets target",       analysis.meetsTarget() ? "yes" : "NO"}
        });

        say("Takt: **" + io.github.seanchatmangpt.dtr.toyota.TaktTimer.humanNs(analysis.taktTimeNs())
                + "** | Cycle: **" + io.github.seanchatmangpt.dtr.toyota.TaktTimer.humanNs(analysis.avgCycleNs())
                + "** | Utilization: **" + String.format("%.1f", analysis.utilizationPct()) + "%**"
                + " \u2014 " + (analysis.meetsTarget() ? "\u2713 meets demand" : "\u2717 behind demand"));

        if (!analysis.meetsTarget()) {
            sayWarning("Cycle time exceeds takt \u2014 production cannot keep up with demand");
        }

        if (analysis.utilizationPct() < 60.0) {
            sayNote("Utilization < 60% \u2014 capacity exists for increased demand");
        }
    }

    // =========================================================================
    // Blue Ocean: Erlang OTP Supervision Tree
    // =========================================================================

    /**
     * Documents a {@link io.github.seanchatmangpt.dtr.erlang.SupervisionTree.SupervisionReport}
     * as a structured table plus summary lines following Joe Armstrong's OTP supervision
     * philosophy.
     *
     * <p>Renders a 5-column table (Child | State | Restarts | Crash Reasons | Total Time)
     * with one row per child report, followed by a bold summary line showing total crashes,
     * total restarts, and the active strategy. Emits a warning when the supervisor is
     * unhealthy (any child exceeded {@code maxRestarts}) and a note with a Joe Armstrong
     * quote.</p>
     *
     * @param report the supervision report produced by
     *               {@link io.github.seanchatmangpt.dtr.erlang.SupervisionTree#supervise}
     */
    default void sayErlangSupervisor(
            io.github.seanchatmangpt.dtr.erlang.SupervisionTree.SupervisionReport report) {

        say("**Supervision Tree: " + report.supervisorName()
                + "** | Strategy: **" + report.strategy()
                + "** | Health: " + (report.supervisorHealthy() ? "\u2713" : "\u2717"));

        var children = report.children();
        String[][] table = new String[children.size() + 1][5];
        table[0] = new String[]{"Child", "State", "Restarts", "Crash Reasons", "Total Time"};
        for (int i = 0; i < children.size(); i++) {
            var c = children.get(i);
            String reasons = c.crashReasons().isEmpty()
                    ? "\u2014"
                    : String.join("; ", c.crashReasons());
            table[i + 1] = new String[]{
                c.name(),
                c.state().name(),
                String.valueOf(c.restartCount()),
                reasons,
                io.github.seanchatmangpt.dtr.erlang.SupervisionTree.humanNs(c.totalNs())
            };
        }
        sayTable(table);

        say("Crashes: **" + report.totalCrashes()
                + "** | Restarts: **" + report.totalRestarts()
                + "** | Strategy: **" + report.strategy() + "**");

        if (!report.supervisorHealthy()) {
            sayWarning("Supervisor unhealthy \u2014 child exceeded maxRestarts. "
                    + "Process escalation required.");
        }

        sayNote("Erlang OTP: 'supervisors are the backbone of fault-tolerant systems'"
                + " \u2014 Joe Armstrong");
    }
}

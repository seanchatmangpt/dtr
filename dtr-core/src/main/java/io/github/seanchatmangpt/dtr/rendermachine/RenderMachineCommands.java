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
}

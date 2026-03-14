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

import org.apache.hc.client5.http.cookie.Cookie;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import org.hamcrest.Matcher;

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
     * @return all cookies saved by this TestBrowser.
     */
    public List<Cookie> sayAndGetCookies();

    public Cookie sayAndGetCookieWithName(String name);

    public Response sayAndMakeRequest(Request httpRequest);

    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher);

    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher);

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
     * <p>On Java 25 and earlier, gracefully falls back to rendering the method signature
     * (parameters with types, return type) extracted via reflection.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * sayCodeModel(SayEvent.class.getMethod("toString"));
     * // Java 26+: Renders operation breakdown (INVOKE: 3, FIELD_READ: 1, etc.)
     * // Java 25-: Renders String toString() signature only
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
     * <p>Uses Java 25 virtual threads for parallel warmup batches to reduce cold-start
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
}

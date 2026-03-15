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
    // 80/20 Blue Ocean Innovations — v2.7.0
    // =========================================================================

    /**
     * Documents how a metric evolves over time with an ASCII sparkline and
     * trend summary (min, max, mean, direction). Zero dependencies — pure Java
     * string math using Unicode block characters (▁▂▃▄▅▆▇█).
     *
     * <p>Blue ocean: no existing documentation framework shows metric trends
     * inline alongside the narrative.</p>
     *
     * @param label      descriptive label for the metric (e.g. "GC pause (ms)")
     * @param values     numeric samples in chronological order
     * @param timestamps human-readable timestamp strings (same length as values)
     */
    void sayTimeSeries(String label, long[] values, String[] timestamps);

    /**
     * Empirically profiles algorithmic complexity by running a task factory
     * at increasing input sizes and measuring wall-clock time. Renders a
     * measurement table and infers the growth class (O(1), O(n), O(n²), etc.).
     *
     * <p>Blue ocean: documentation that PROVES complexity instead of asserting it.</p>
     *
     * @param label       descriptive label (e.g. "ArrayList.contains()")
     * @param taskFactory produces a {@code Runnable} for the given input size {@code n}
     * @param ns          array of input sizes to measure (e.g. {100, 1_000, 10_000})
     */
    void sayComplexityProfile(String label,
                              java.util.function.IntFunction<Runnable> taskFactory,
                              int[] ns);

    /**
     * Renders a finite state machine as a Mermaid {@code stateDiagram-v2} diagram.
     * Keys in {@code transitions} are "FROM:EVENT" strings; values are destination
     * state names. The initial state is taken as the first key's source.
     *
     * <p>Blue ocean: state machines pervade systems yet are almost never documented
     * in a living, executable form.</p>
     *
     * @param title       human-readable title shown above the diagram
     * @param transitions map of "FROM:EVENT" → "TO_STATE"
     */
    void sayStateMachine(String title,
                         java.util.Map<String, String> transitions);

    /**
     * Documents a data transformation pipeline by executing each stage against
     * sample inputs and capturing intermediate outputs. Renders a flowchart and
     * a table showing input → stage output at each step.
     *
     * <p>Blue ocean: ETL / pipeline docs are perpetually stale; this makes
     * them executable so they cannot drift.</p>
     *
     * @param title  pipeline name (e.g. "Order Processing Pipeline")
     * @param stages ordered list of stage labels
     * @param transforms ordered list of {@code java.util.function.Function<Object,Object>}
     *                   applied sequentially to the sample input
     * @param sample     representative input value for the first stage
     */
    void sayDataFlow(String title,
                     java.util.List<String> stages,
                     java.util.List<java.util.function.Function<Object, Object>> transforms,
                     Object sample);

    /**
     * Computes the semantic diff between two class versions (e.g. old API vs.
     * new API) using reflection. Produces three tables: added methods, removed
     * methods, and signature-changed methods.
     *
     * <p>Blue ocean: breaking-change documentation that is automatically complete
     * — no manual "CHANGELOG" entries needed.</p>
     *
     * @param before class representing the previous API version
     * @param after  class representing the new API version
     */
    void sayApiDiff(Class<?> before, Class<?> after);

    /**
     * Renders a 2-D ASCII heatmap using Unicode block characters (░▒▓█) for
     * matrix data. Normalises values to [0, 1] and maps to four intensity levels.
     * Ideal for correlation matrices, confusion matrices, and perf heat maps.
     *
     * <p>Blue ocean: 2-D visualisation in plain text — works in any terminal,
     * any Markdown renderer, and any CI log.</p>
     *
     * @param title     descriptive title shown above the heatmap
     * @param matrix    2-D data array [rows][cols]
     * @param rowLabels labels for each row
     * @param colLabels labels for each column
     */
    void sayHeatmap(String title,
                    double[][] matrix,
                    String[] rowLabels,
                    String[] colLabels);

    /**
     * Documents a logical property by evaluating a predicate against a list of
     * sample inputs. Renders a table showing each input, the predicate result,
     * and a PASS/FAIL marker. Fails the test if any input violates the property.
     *
     * <p>Blue ocean: property-based invariant documentation — proves correctness
     * across representative examples inline with the narrative.</p>
     *
     * @param property description of the invariant (e.g. "result is always positive")
     * @param check    predicate that must hold for every input
     * @param inputs   representative sample inputs
     */
    void sayPropertyBased(String property,
                          java.util.function.Predicate<Object> check,
                          java.util.List<Object> inputs);

    /**
     * Renders a parallel execution trace as a Mermaid Gantt chart. Each agent
     * is a section; each {@code timeSlot} is a {@code long[2]} of
     * {startMs, endMs} relative to trace start.
     *
     * <p>Blue ocean: multi-agent / multi-thread execution is hard to reason about
     * from logs; this makes the timeline visual and living.</p>
     *
     * @param title     chart title
     * @param agents    agent/thread names (same length as timeSlots)
     * @param timeSlots parallel list of {startMs, durationMs} per agent
     */
    void sayParallelTrace(String title,
                          java.util.List<String> agents,
                          java.util.List<long[]> timeSlots);

    /**
     * Documents a decision algorithm as a Mermaid {@code flowchart TD}.
     * The {@code branches} map encodes the tree: keys are node labels (questions
     * / conditions); values are either a {@code String} (leaf answer) or a nested
     * {@code Map<String,Object>} (sub-tree). Renders at most 5 levels deep.
     *
     * <p>Blue ocean: decision logic is usually buried in code; this surfaces it
     * as an auto-generated, always-current diagram.</p>
     *
     * @param title    chart title / root question label
     * @param branches decision tree encoded as nested map
     */
    void sayDecisionTree(String title,
                         java.util.Map<String, Object> branches);

    /**
     * Documents an AI agent's reasoning loop: observations (inputs the agent
     * perceives), decisions (actions it chose), and tools (external calls it
     * made). Renders a sequence diagram showing the agent ↔ environment
     * interaction over one full loop iteration.
     *
     * <p>Blue ocean: the first documentation primitive designed specifically for
     * agentic AI workflows — no other documentation framework has this.</p>
     *
     * @param agentName    name of the agent (e.g. "DTR Documentation Agent")
     * @param observations what the agent observed (in order)
     * @param decisions    what the agent decided (in order)
     * @param tools        external tool calls the agent made (in order)
     */
    void sayAgentLoop(String agentName,
                      java.util.List<String> observations,
                      java.util.List<String> decisions,
                      java.util.List<String> tools);

    // ── Toyota Production System + Joe Armstrong Blue Ocean innovations ────────

    /**
     * Documents an Erlang/OTP-style supervision tree showing which supervisors
     * manage which workers. Renders as a Mermaid {@code graph TD} with
     * supervisor → child edges, restart strategies, and worker counts.
     *
     * <p>Blue ocean: surfaces fault-tolerance topology that is implicit in OTP
     * app files but never visualised in documentation.</p>
     *
     * @param title       diagram title (e.g. "Payment Service Supervision Tree")
     * @param supervisors map of supervisor name → list of child names
     */
    void saySupervisionTree(String title,
                            java.util.Map<String, java.util.List<String>> supervisors);

    /**
     * Documents actor-model message passing: which actors send what messages
     * to which targets. Renders a Mermaid sequence diagram showing concurrent
     * asynchronous communication without shared state.
     *
     * <p>Blue ocean: Joe Armstrong's "share nothing" discipline made visible —
     * every message hop is explicit, auditable, and always up to date.</p>
     *
     * @param title    diagram title
     * @param actors   actor names in order of appearance
     * @param messages list of {@code [sender, receiver, message]} triples
     */
    void sayActorMessages(String title,
                          java.util.List<String> actors,
                          java.util.List<String[]> messages);

    /**
     * Documents a "let it crash" fault-tolerance scenario: a list of failures
     * paired with supervisor-driven recoveries. Renders a two-column table plus
     * a recovery-ratio metric (recoveries / failures).
     *
     * <p>Blue ocean: makes the implicit restart contract explicit — reviewers
     * can audit fault coverage without reading OTP supervisor specs.</p>
     *
     * @param scenario   scenario name (e.g. "Database connection pool exhausted")
     * @param failures   failure descriptions in order
     * @param recoveries supervisor recovery actions in corresponding order
     */
    void sayFaultTolerance(String scenario,
                           java.util.List<String> failures,
                           java.util.List<String> recoveries);

    /**
     * Documents a Kaizen continuous-improvement event: measures a metric before
     * and after the improvement, renders a comparison table with absolute delta
     * and percentage improvement, and calls out the improvement ratio.
     *
     * <p>Blue ocean: Toyota's improvement discipline applied to software metrics
     * — latency, throughput, defect rate — with machine-verified numbers.</p>
     *
     * @param metric name of the metric (e.g. "P99 latency", "Build time")
     * @param before measurement samples before the improvement
     * @param after  measurement samples after the improvement
     * @param unit   unit label (e.g. "ms", "s", "defects/kloc")
     */
    void sayKaizen(String metric, long[] before, long[] after, String unit);

    /**
     * Documents a Kanban board snapshot: items in backlog, in progress (WIP),
     * and done. Renders as a three-column markdown table and reports the
     * WIP count and flow efficiency (done / total).
     *
     * <p>Blue ocean: living documentation of work state — the board IS the
     * documentation, auto-generated from your tracking data.</p>
     *
     * @param board   board name (e.g. "Sprint 42", "Q2 Infrastructure")
     * @param backlog items not yet started
     * @param wip     items currently in progress
     * @param done    completed items
     */
    void sayKanban(String board,
                   java.util.List<String> backlog,
                   java.util.List<String> wip,
                   java.util.List<String> done);

    /**
     * Documents Erlang-style pattern matching: a set of patterns, the values
     * they are matched against, and whether each match succeeds. Renders a
     * three-column table with ✅ / ❌ match indicators.
     *
     * <p>Blue ocean: pattern-matching logic is tested but never documented —
     * this surfaces the full match matrix as executable specification.</p>
     *
     * @param title    section title
     * @param patterns pattern strings (e.g. "{ok, Value}", "_")
     * @param values   value strings tested against corresponding patterns
     * @param matches  true if the pattern matched, false if it did not
     */
    void sayPatternMatch(String title,
                         java.util.List<String> patterns,
                         java.util.List<String> values,
                         java.util.List<Boolean> matches);

    /**
     * Documents a Toyota Andon-cord production-status board: each workstation
     * and its current status. Renders a table with status-coded rows
     * (✅ Normal / ⚠️ Caution / ❌ Stopped) and a station-health summary.
     *
     * <p>Blue ocean: production dashboards are ephemeral; this generates a
     * point-in-time snapshot in the documentation record.</p>
     *
     * @param system   system or line name (e.g. "Payment Processing Line")
     * @param stations station / service names
     * @param statuses status strings: "NORMAL", "CAUTION", or "STOPPED"
     */
    void sayAndon(String system,
                  java.util.List<String> stations,
                  java.util.List<String> statuses);

    /**
     * Documents a Muda (waste) analysis: identifies the seven TPS wastes present
     * in a process and the corresponding improvement actions. Renders a
     * waste-type → description → action table and a waste-elimination ratio.
     *
     * <p>Blue ocean: waste is identified in retrospectives but rarely committed
     * to documentation — this makes the analysis a first-class artefact.</p>
     *
     * @param process      process name (e.g. "Manual deployment pipeline")
     * @param wastes       waste descriptions (what waste was found)
     * @param improvements improvement actions for each waste
     */
    void sayMuda(String process,
                 java.util.List<String> wastes,
                 java.util.List<String> improvements);

    /**
     * Documents a Value Stream Map: the sequence of process steps from demand
     * to delivery, each with a measured cycle time. Renders a bar chart of
     * cycle times and reports total lead time, value-adding time, and efficiency.
     *
     * <p>Blue ocean: value stream mapping is a whiteboard exercise; this
     * generates a measurable, always-current version from real data.</p>
     *
     * @param product     product or feature name (e.g. "Feature → Production")
     * @param steps       process step names in flow order
     * @param cycleTimeMs measured cycle time for each step in milliseconds
     */
    void sayValueStream(String product,
                        java.util.List<String> steps,
                        long[] cycleTimeMs);

    /**
     * Documents Poka-yoke (mistake-proofing) devices: each mistake-proof
     * mechanism and whether it was verified as effective. Renders a
     * mechanism → verified table with ✅ / ❌ indicators and an effectiveness %.
     *
     * <p>Blue ocean: error-prevention mechanisms are described in runbooks
     * but never tested and documented together — this unifies both.</p>
     *
     * @param operation     operation name (e.g. "Production deployment")
     * @param mistakeProofs mistake-proofing mechanism descriptions
     * @param verified      true if the mechanism was confirmed effective
     */
    void sayPokaYoke(String operation,
                     java.util.List<String> mistakeProofs,
                     java.util.List<Boolean> verified);
}

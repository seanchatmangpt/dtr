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
import java.util.Optional;

import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.coverage.CoverageRow;

/**
 * Sealed event hierarchy for the DTR render pipeline.
 *
 * <p>Every {@code say*} invocation on a {@link RenderMachine} corresponds to a
 * {@code SayEvent} subtype. The sealed hierarchy with exhaustive switch expressions
 * ensures compile-time completeness — adding a new event type forces every renderer
 * to handle it or fail compilation.</p>
 *
 * <p>This is the canonical demonstration of sealed classes + records + pattern matching
 * working together as a type-safe event system. The pattern:</p>
 * <pre>{@code
 * String rendered = switch (event) {
 *     case SayEvent.TextEvent(var text)           -> renderParagraph(text);
 *     case SayEvent.SectionEvent(var heading)     -> renderSection(heading);
 *     case SayEvent.CodeEvent(var code, var lang) -> renderCode(code, lang);
 *     // ... exhaustive — compiler enforces completeness
 * };
 * }</pre>
 *
 * <p>No visitor pattern. No dispatch maps. No instanceof chains. No defaults.
 * The type system proves every case is handled.</p>
 *
 * <p>Inspired by Project Babylon's code model approach: the event structure IS the
 * documentation contract, not a description of it.</p>
 *
 * @since 2.0.0
 */
public sealed interface SayEvent
        permits SayEvent.TextEvent,
                SayEvent.SectionEvent,
                SayEvent.CodeEvent,
                SayEvent.TableEvent,
                SayEvent.JsonEvent,
                SayEvent.NoteEvent,
                SayEvent.WarningEvent,
                SayEvent.KeyValueEvent,
                SayEvent.UnorderedListEvent,
                SayEvent.OrderedListEvent,
                SayEvent.AssertionsEvent,
                SayEvent.CitationEvent,
                SayEvent.FootnoteEvent,
                SayEvent.RefEvent,
                SayEvent.RawEvent,
                SayEvent.CodeModelEvent,
                SayEvent.MethodCodeModelEvent,
                SayEvent.ControlFlowGraphEvent,
                SayEvent.CallGraphEvent,
                SayEvent.OpProfileEvent,
                SayEvent.BenchmarkEvent,
                SayEvent.MermaidEvent,
                SayEvent.DocCoverageEvent,
                SayEvent.EnvProfileEvent,
                SayEvent.RecordSchemaEvent,
                SayEvent.ExceptionEvent,
                SayEvent.AsciiChartEvent,
                // ── Missing core events ──────────────────────────────────────
                SayEvent.AnnotationProfileEvent,
                SayEvent.CallSiteEvent,
                SayEvent.ClassDiagramEvent,
                SayEvent.ClassHierarchyEvent,
                SayEvent.ContractVerificationEvent,
                SayEvent.EvolutionTimelineEvent,
                SayEvent.JavadocEvent,
                SayEvent.ModuleDependenciesEvent,
                SayEvent.OperatingSystemEvent,
                SayEvent.ReflectiveDiffEvent,
                SayEvent.SecurityManagerEvent,
                SayEvent.StringProfileEvent,
                SayEvent.SystemPropertiesEvent,
                SayEvent.ThreadDumpEvent,
                // ── Innovation events (Phase 2) ──────────────────────────────
                SayEvent.HttpContractEvent,
                SayEvent.PerformanceRegressionEvent,
                SayEvent.VirtualThreadComparisonEvent,
                SayEvent.NarrativeScenarioEvent,
                SayEvent.DataSampleEvent,
                SayEvent.DecisionRecordEvent,
                SayEvent.LoadProfileEvent,
                SayEvent.TypeCompatEvent,
                SayEvent.SecurityProfileEvent,
                SayEvent.GitHotspotEvent,
                // ── Wave 3 innovations ───────────────────────────────────────
                SayEvent.BenchmarkComparisonEvent,
                SayEvent.JavadocSelfEvent,
                SayEvent.ParallelBenchmarkEvent,
                SayEvent.DependencyGraphEvent,
                SayEvent.ApiContractEvent,
                SayEvent.DocumentSnapshotEvent,
                SayEvent.DocumentDiffEvent,
                SayEvent.SchemaEvolutionEvent,
                SayEvent.PropertyTestEvent,
                SayEvent.MethodCoverageEvent {

    /**
     * A narrative paragraph — the most fundamental documentation unit.
     *
     * @param text the paragraph text, may contain markdown formatting
     */
    record TextEvent(String text) implements SayEvent {
        public TextEvent {
            java.util.Objects.requireNonNull(text, "text must not be null");
        }
    }

    /**
     * A section heading that appears in the table of contents.
     *
     * @param heading the section title
     */
    record SectionEvent(String heading) implements SayEvent {
        public SectionEvent {
            java.util.Objects.requireNonNull(heading, "heading must not be null");
        }
    }

    /**
     * A fenced code block with optional language for syntax highlighting.
     *
     * @param code     the source code content
     * @param language the language hint (e.g., "java", "sql", "json") — may be empty
     */
    record CodeEvent(String code, String language) implements SayEvent {}

    /**
     * A markdown table rendered from a 2D string array.
     * The first row becomes the table header.
     *
     * @param data 2D array of cell values; {@code data[0]} is the header row
     */
    record TableEvent(String[][] data) implements SayEvent {}

    /**
     * A JSON code block rendered from a serializable object.
     *
     * @param object the object to serialize as pretty-printed JSON
     */
    record JsonEvent(Object object) implements SayEvent {}

    /**
     * An informational callout — GitHub-style {@code > [!NOTE]}.
     *
     * @param message the note content
     */
    record NoteEvent(String message) implements SayEvent {}

    /**
     * A warning callout — GitHub-style {@code > [!WARNING]}.
     *
     * @param message the warning content
     */
    record WarningEvent(String message) implements SayEvent {}

    /**
     * Key-value pairs rendered as a two-column markdown table.
     *
     * @param pairs the key→value mapping to render
     */
    record KeyValueEvent(Map<String, String> pairs) implements SayEvent {}

    /**
     * An unordered bullet list.
     *
     * @param items the list items
     */
    record UnorderedListEvent(List<String> items) implements SayEvent {}

    /**
     * An ordered (numbered) list.
     *
     * @param items the list items in display order
     */
    record OrderedListEvent(List<String> items) implements SayEvent {}

    /**
     * An assertions summary table with Check and Result columns.
     *
     * @param assertions map of check descriptions to result strings
     */
    record AssertionsEvent(Map<String, String> assertions) implements SayEvent {}

    /**
     * A BibTeX citation reference, optionally with a page number.
     *
     * @param citationKey the BibTeX key
     * @param pageRef     optional page reference (e.g., "42", "pp. 10-15")
     */
    record CitationEvent(String citationKey, java.util.Optional<String> pageRef)
            implements SayEvent {}

    /**
     * A footnote to be rendered at the end of the document section.
     *
     * @param text the footnote content
     */
    record FootnoteEvent(String text) implements SayEvent {}

    /**
     * A cross-reference link to another DocTest's section.
     *
     * @param ref the target reference
     */
    record RefEvent(DocTestRef ref) implements SayEvent {}

    /**
     * Raw markdown content injected directly into the output.
     *
     * @param markdown the raw markdown string
     */
    record RawEvent(String markdown) implements SayEvent {}

    /**
     * A code model introspection event — the DTR stand-in for Project Babylon's
     * Code Reflection API (JEP 494).
     *
     * <p>Renders a class's sealed hierarchy, record components, and public method
     * signatures derived from the bytecode. The documentation cannot drift from the
     * implementation because it IS the implementation.</p>
     *
     * @param clazz the class to introspect and document
     */
    record CodeModelEvent(Class<?> clazz) implements SayEvent {
        public CodeModelEvent {
            java.util.Objects.requireNonNull(clazz, "clazz must not be null");
        }
    }

    /**
     * A method code model event using Java 26 Code Reflection (JEP 516 / Project Babylon).
     * Carries the op-count map, block count, IR excerpt, and method signature.
     */
    record MethodCodeModelEvent(
            String methodSig,
            Map<String, Long> opCounts,
            int blockCount,
            int totalOps,
            List<String> irExcerpt
    ) implements SayEvent {}

    /**
     * A control flow graph rendered as a Mermaid flowchart, derived from the Java 26
     * Code Reflection IR of a method.
     */
    record ControlFlowGraphEvent(String mermaidDsl, String methodSig) implements SayEvent {}

    /**
     * A call graph showing method-to-method invocation relationships, derived from
     * InvokeOp traversal of the Code Reflection IR.
     */
    record CallGraphEvent(String mermaidDsl, String className) implements SayEvent {}

    /**
     * A lightweight op-profile table (counts only, no IR excerpt) for a method.
     */
    record OpProfileEvent(
            String methodSig,
            Map<String, Long> opCounts,
            int blockCount
    ) implements SayEvent {}

    /**
     * A real benchmark measurement using System.nanoTime() with warmup rounds.
     */
    record BenchmarkEvent(
            String label,
            long avgNs,
            long minNs,
            long maxNs,
            long p99Ns,
            long opsPerSec,
            int warmupRounds,
            int measureRounds
    ) implements SayEvent {}

    /**
     * A Mermaid diagram (any type) to be rendered as a fenced mermaid code block.
     */
    record MermaidEvent(String diagramDsl) implements SayEvent {
        public MermaidEvent {
            java.util.Objects.requireNonNull(diagramDsl, "diagramDsl must not be null");
        }
    }

    /**
     * Documentation coverage report for a class — which public methods were documented
     * during this test and which were not.
     */
    record DocCoverageEvent(
            String className,
            List<CoverageRow> rows,
            int covered,
            int total
    ) implements SayEvent {}

    /**
     * A snapshot of the current runtime environment (Java version, OS, heap, etc.).
     */
    record EnvProfileEvent(Map<String, String> properties) implements SayEvent {}

    /**
     * A schema table for a Java record class — component names, types, and annotations.
     */
    record RecordSchemaEvent(String recordName, List<String> componentLines) implements SayEvent {}

    /**
     * An exception chain — type, message, causes, and top stack frames.
     */
    record ExceptionEvent(
            String exceptionType,
            String message,
            List<String> causeChain,
            List<String> topFrames
    ) implements SayEvent {}

    /**
     * An ASCII horizontal bar chart for numeric data.
     */
    record AsciiChartEvent(String label, double[] values, String[] xLabels) implements SayEvent {}

    // ── Missing core events ──────────────────────────────────────────────────

    /** All annotations present on a class and its methods. */
    record AnnotationProfileEvent(Class<?> clazz) implements SayEvent {}

    /** Caller location captured at render time via StackWalker — no fields needed. */
    record CallSiteEvent() implements SayEvent {}

    /** Auto-generated Mermaid classDiagram from one or more classes. */
    record ClassDiagramEvent(Class<?>[] classes) implements SayEvent {}

    /** Inheritance tree for a class — superclass chain plus implemented interfaces. */
    record ClassHierarchyEvent(Class<?> clazz) implements SayEvent {}

    /** Interface implementation coverage across one or more implementation classes. */
    record ContractVerificationEvent(Class<?> contract, Class<?>[] implementations) implements SayEvent {}

    /** Git log --follow timeline for a class source file. */
    record EvolutionTimelineEvent(Class<?> clazz, int maxEntries) implements SayEvent {}

    /** Javadoc extracted from docs/meta/javadoc.json for a specific method. */
    record JavadocEvent(java.lang.reflect.Method method) implements SayEvent {}

    /** Module dependency graph for a set of classes. */
    record ModuleDependenciesEvent(Class<?>[] classes) implements SayEvent {}

    /** Operating system name, version, architecture, and kernel details. */
    record OperatingSystemEvent() implements SayEvent {}

    /** Field-by-field comparison table between two object snapshots. */
    record ReflectiveDiffEvent(Object before, Object after) implements SayEvent {}

    /** Security manager status and relevant policy details. */
    record SecurityManagerEvent() implements SayEvent {}

    /** Word count, line count, and Unicode metrics for a string. */
    record StringProfileEvent(String text) implements SayEvent {}

    /** System properties dump; optional filter prefix covers both overloads. */
    record SystemPropertiesEvent(Optional<String> filter) implements SayEvent {}

    /** Snapshot of all live threads at the time of the call. */
    record ThreadDumpEvent() implements SayEvent {}

    // ── Innovation events (Phase 2) ──────────────────────────────────────────

    /** HTTP contract assertion — expected fields/values against a live endpoint. */
    record HttpContractEvent(String url, String[][] expectedFields) implements SayEvent {}

    /** Performance regression check — label and baseline nanoseconds only (Runnable excluded). */
    record PerformanceRegressionEvent(String label, long baselineNs) implements SayEvent {}

    /** Virtual-thread vs platform-thread throughput comparison for a workload. */
    record VirtualThreadComparisonEvent(String label, int taskCount) implements SayEvent {}

    /** BDD narrative scenario: Given / When / Then (Runnable excluded). */
    record NarrativeScenarioEvent(String given, String when, String then) implements SayEvent {}

    /** Metadata summary of a data sample — type name, total rows, max sample rows. */
    record DataSampleEvent(String dataTypeName, int totalRows, int maxSampleRows) implements SayEvent {}

    /** Architecture Decision Record with full ADR fields. */
    record DecisionRecordEvent(
            String id,
            String title,
            String context,
            String decision,
            String consequences
    ) implements SayEvent {}

    /** Load-profile measurement — label, thread count, and duration in ms (Runnable excluded). */
    record LoadProfileEvent(String label, int threads, long durationMs) implements SayEvent {}

    /** Binary compatibility check between two class versions. */
    record TypeCompatEvent(Class<?> v1, Class<?> v2) implements SayEvent {}

    /** Security annotations and permission requirements for a class. */
    record SecurityProfileEvent(Class<?> clazz) implements SayEvent {}

    /** Git churn hotspot analysis for a class file within a project root. */
    record GitHotspotEvent(Class<?> clazz, String projectRoot) implements SayEvent {}

    // ── Wave 3 innovation events ─────────────────────────────────────────────

    /** Multi-task benchmark comparison — ranked table of relative speeds. */
    record BenchmarkComparisonEvent(String[] labels, String[][] resultTable) implements SayEvent {}

    /** Auto-detected caller javadoc — class name, method name, line number via StackWalker. */
    record JavadocSelfEvent(String callerClass, String callerMethod, int lineNumber) implements SayEvent {}

    /** Parallel scalability benchmark — throughput at increasing thread counts. */
    record ParallelBenchmarkEvent(String label, int[] threadCounts, long[] throughputs) implements SayEvent {}

    /** Maven dependency graph rendered as a Mermaid diagram. */
    record DependencyGraphEvent(String mermaidDsl, String projectRoot) implements SayEvent {}

    /** Reflective API contract table — public methods with params, return type, annotations. */
    record ApiContractEvent(String className, String[][] rows) implements SayEvent {}

    /** Document snapshot saved to target/docs/snapshots/{key}. */
    record DocumentSnapshotEvent(String key, int lineCount) implements SayEvent {}

    /** Diff between current document lines and a previous snapshot. */
    record DocumentDiffEvent(String key, List<String> added, List<String> removed) implements SayEvent {}

    /** Git-based schema evolution history for a class's source file. */
    record SchemaEvolutionEvent(String className, List<String[]> history) implements SayEvent {}

    /** Property-based test result — trials, failures, and counterexamples. */
    record PropertyTestEvent(String label, int trials, int failures, List<String> counterexamples) implements SayEvent {}

    /** Method-level documentation coverage for a class. */
    record MethodCoverageEvent(String className, List<String[]> rows, int covered, int total) implements SayEvent {}
}

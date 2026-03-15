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

/**
 * Abstract base class for render machines that convert test execution into documentation.
 *
 * <p>Supports multiple output formats: Markdown, Blog posts, Slides, LaTeX, etc.</p>
 *
 * <p>Java 26 Design Note:</p>
 * <p>While sealed classes (JEP 409) would normally enforce a closed type hierarchy for
 * static analysis and devirtualization, this class remains open due to Java's constraint
 * that sealed classes in non-modular projects cannot have permitted subclasses in
 * different packages. Since RenderMachine implementations are distributed across
 * io.github.seanchatmangpt.dtr.rendermachine, rendermachine.latex, render.blog, and
 * render.slides packages, the class uses standard inheritance with all implementations
 * marked {@code final} to maintain single inheritance and enable JIT devirtualization.</p>
 *
 * <p>Standard implementation hierarchy:</p>
 * <ul>
 *   <li>RenderMachineImpl: Markdown output</li>
 *   <li>RenderMachineLatex: LaTeX/PDF output</li>
 *   <li>MultiRenderMachine: Parallel dispatch to multiple machines</li>
 *   <li>BlogRenderMachine: Social media and blog platform export</li>
 *   <li>SlideRenderMachine: Reveal.js HTML5 presentation output</li>
 * </ul>
 */
public abstract class RenderMachine implements RenderMachineCommands {

    /**
     * Sets the output filename (typically the test class name).
     *
     * @param fileName the filename for the generated documentation
     */
    public abstract void setFileName(String fileName);

    /**
     * Renders content only for slide output (ignored by doc/blog render machines).
     *
     * @param text the text to render on slides only
     */
    public void saySlideOnly(String text) {
        // No-op for non-slide render machines
    }

    /**
     * Renders content only for documentation/blog output (ignored by slide render machines).
     *
     * @param text the text to render in docs only
     */
    public void sayDocOnly(String text) {
        // Most render machines will override this
        say(text);
    }

    /**
     * Renders speaker notes for slides (ignored by doc/blog render machines).
     *
     * @param text the speaker notes text
     */
    public void saySpeakerNote(String text) {
        // No-op for non-slide render machines
    }

    /**
     * Renders a hero image for blogs and slides (ignored by other formats).
     *
     * @param altText the alt text for the image
     */
    public void sayHeroImage(String altText) {
        // No-op for formats that don't support hero images
    }

    /**
     * Renders a tweetable (≤280 chars) for social media queue.
     *
     * @param text the text to tweet (will be truncated to 280 chars)
     */
    public void sayTweetable(String text) {
        // No-op for formats that don't generate social content
    }

    /**
     * Renders a TLDR (too long; didn't read) summary for blogs.
     *
     * @param text the summary text
     */
    public void sayTldr(String text) {
        // No-op for formats that don't support TLDR
    }

    /**
     * Renders a call-to-action link for blogs.
     *
     * @param url the URL for the CTA button/link
     */
    public void sayCallToAction(String url) {
        // No-op for formats that don't support CTAs
    }

    /**
     * Documents a class's structure using Java reflection.
     *
     * <p>Default no-op implementation — override in render machines that support
     * code model rendering (e.g., {@link RenderMachineImpl}).</p>
     *
     * @param clazz the class to introspect and document
     */
    public void sayCodeModel(Class<?> clazz) {
        // No-op for render machines that don't support code model rendering
    }

    /**
     * Documents a method's structure using reflection/CodeReflection API.
     *
     * <p>Default no-op implementation — override in render machines that support
     * method introspection (e.g., {@link RenderMachineImpl}).</p>
     *
     * @param method the method to introspect and document
     */
    public void sayCodeModel(java.lang.reflect.Method method) {
        // No-op for render machines that don't support method code model rendering
    }

    /** Documents current call site — no-op in base class. */
    public void sayCallSite() {}

    /** Documents annotation profile — no-op in base class. */
    public void sayAnnotationProfile(Class<?> clazz) {}

    /** Documents class hierarchy — no-op in base class. */
    public void sayClassHierarchy(Class<?> clazz) {}

    /** Documents string profile — no-op in base class. */
    public void sayStringProfile(String text) {}

    /** Documents reflective diff — no-op in base class. */
    public void sayReflectiveDiff(Object before, Object after) {}

    /** Control flow graph — no-op in base class. */
    public void sayControlFlowGraph(java.lang.reflect.Method method) {}

    /** Call graph — no-op in base class. */
    public void sayCallGraph(Class<?> clazz) {}

    /** Op profile — no-op in base class. */
    public void sayOpProfile(java.lang.reflect.Method method) {}

    /** Benchmark — no-op in base class. */
    public void sayBenchmark(String label, Runnable task) {}

    /** Benchmark with explicit rounds — no-op in base class. */
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {}

    /** Raw Mermaid diagram — no-op in base class. */
    public void sayMermaid(String diagramDsl) {}

    /** Class diagram — no-op in base class. */
    public void sayClassDiagram(Class<?>... classes) {}

    /** Documentation coverage — no-op in base class. */
    public void sayDocCoverage(Class<?>... classes) {}

    /** Environment profile — no-op in base class. */
    public void sayEnvProfile() {}

    /** Record components schema — no-op in base class. */
    public void sayRecordComponents(Class<? extends Record> recordClass) {}

    /** Exception chain documentation — no-op in base class. */
    public void sayException(Throwable t) {}

    /** ASCII chart — no-op in base class. */
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {}

    /** Contract verification — no-op in base class. */
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {}

    /** Git evolution timeline — no-op in base class. */
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {}

    /** Javadoc documentation from dtr-javadoc index — no-op in base class. */
    public void sayJavadoc(java.lang.reflect.Method method) {}

    // =========================================================================
    // 80/20 Blue Ocean Innovations — v2.7.0 no-ops
    // =========================================================================

    /** Time-series sparkline + trend — no-op in base class. */
    public void sayTimeSeries(String label, long[] values, String[] timestamps) {}

    /** Empirical complexity profile — no-op in base class. */
    public void sayComplexityProfile(String label,
                                     java.util.function.IntFunction<Runnable> taskFactory,
                                     int[] ns) {}

    /** State machine Mermaid stateDiagram-v2 — no-op in base class. */
    public void sayStateMachine(String title,
                                java.util.Map<String, String> transitions) {}

    /** Data transformation pipeline flowchart — no-op in base class. */
    public void sayDataFlow(String title,
                            java.util.List<String> stages,
                            java.util.List<java.util.function.Function<Object, Object>> transforms,
                            Object sample) {}

    /** Semantic API diff between two class versions — no-op in base class. */
    public void sayApiDiff(Class<?> before, Class<?> after) {}

    /** 2-D ASCII heatmap — no-op in base class. */
    public void sayHeatmap(String title,
                           double[][] matrix,
                           String[] rowLabels,
                           String[] colLabels) {}

    /** Property-based invariant documentation — no-op in base class. */
    public void sayPropertyBased(String property,
                                 java.util.function.Predicate<Object> check,
                                 java.util.List<Object> inputs) {}

    /** Parallel execution Gantt trace — no-op in base class. */
    public void sayParallelTrace(String title,
                                 java.util.List<String> agents,
                                 java.util.List<long[]> timeSlots) {}

    /** Decision tree flowchart — no-op in base class. */
    public void sayDecisionTree(String title,
                                java.util.Map<String, Object> branches) {}

    /** AI agent loop sequence diagram — no-op in base class. */
    public void sayAgentLoop(String agentName,
                             java.util.List<String> observations,
                             java.util.List<String> decisions,
                             java.util.List<String> tools) {}

    // ── Toyota Production System + Joe Armstrong — no-op defaults ─────────────

    /** Erlang OTP supervision tree — no-op in base class. */
    public void saySupervisionTree(String title,
                                   java.util.Map<String, java.util.List<String>> supervisors) {}

    /** Actor model message passing — no-op in base class. */
    public void sayActorMessages(String title,
                                 java.util.List<String> actors,
                                 java.util.List<String[]> messages) {}

    /** Let-it-crash fault-tolerance scenario — no-op in base class. */
    public void sayFaultTolerance(String scenario,
                                  java.util.List<String> failures,
                                  java.util.List<String> recoveries) {}

    /** Kaizen continuous-improvement before/after — no-op in base class. */
    public void sayKaizen(String metric, long[] before, long[] after, String unit) {}

    /** Kanban WIP board snapshot — no-op in base class. */
    public void sayKanban(String board,
                          java.util.List<String> backlog,
                          java.util.List<String> wip,
                          java.util.List<String> done) {}

    /** Erlang-style pattern match results — no-op in base class. */
    public void sayPatternMatch(String title,
                                java.util.List<String> patterns,
                                java.util.List<String> values,
                                java.util.List<Boolean> matches) {}

    /** Toyota Andon production status board — no-op in base class. */
    public void sayAndon(String system,
                         java.util.List<String> stations,
                         java.util.List<String> statuses) {}

    /** Muda waste-elimination analysis — no-op in base class. */
    public void sayMuda(String process,
                        java.util.List<String> wastes,
                        java.util.List<String> improvements) {}

    /** Value stream map with cycle times — no-op in base class. */
    public void sayValueStream(String product,
                               java.util.List<String> steps,
                               long[] cycleTimeMs) {}

    /** Poka-yoke mistake-proofing verification — no-op in base class. */
    public void sayPokaYoke(String operation,
                            java.util.List<String> mistakeProofs,
                            java.util.List<Boolean> verified) {}

    /**
     * Finishes documentation generation and writes output to disk.
     */
    public abstract void finishAndWriteOut();

}

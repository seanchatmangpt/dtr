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

    /** HTTP contract check — no-op in base class. */
    public void sayHttpContract(String url, String[][] expectedFields) {}

    /** Performance regression check — no-op in base class. */
    public void sayPerformanceRegression(String label, long baselineNs, Runnable task) {}

    /** Virtual thread comparison — no-op in base class. */
    public void sayVirtualThreadComparison(String label, int taskCount, Runnable task) {}

    /** Narrative scenario — no-op in base class. */
    public void sayNarrativeScenario(String given, String when, String then, Runnable action) {}

    /** Data sample — no-op in base class. */
    public void sayDataSample(java.util.List<?> data, int maxSampleRows) {}

    /** Decision record — no-op in base class. */
    public void sayDecisionRecord(String id, String title, String context, String decision, String consequences) {}

    /** Load profile — no-op in base class. */
    public void sayLoadProfile(String label, int threads, long durationMs, Runnable task) {}

    /** Type compat — no-op in base class. */
    public void sayTypeCompat(Class<?> v1, Class<?> v2) {}

    /** Security profile — no-op in base class. */
    public void saySecurityProfile(Class<?> clazz) {}

    /** Git hotspot — no-op in base class. */
    public void sayGitHotspot(Class<?> clazz, String projectRoot) {}

    /**
     * Finishes documentation generation and writes output to disk.
     */
    public abstract void finishAndWriteOut();

}

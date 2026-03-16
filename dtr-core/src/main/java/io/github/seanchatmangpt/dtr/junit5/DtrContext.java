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
package io.github.seanchatmangpt.dtr.junit5;

import io.github.seanchatmangpt.dtr.coverage.CoverageRow;
import io.github.seanchatmangpt.dtr.coverage.DocCoverageAnalyzer;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object for JUnit 5 DTR tests.
 *
 * <p>Provides access to all DTR functionality within JUnit 5 test methods.
 * Can be injected as a parameter into test methods when using
 * {@link DtrExtension}.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest {
 *
 *     @Test
 *     void testGetUsers(DtrContext ctx) {
 *         ctx.sayNextSection("User API");
 *         ctx.say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 */
public class DtrContext implements RenderMachineCommands {

    private final RenderMachine renderMachine;

    /** Tracks which method names were documented during this test for sayDocCoverage(). */
    private final Set<String> documentedMethodNames = new HashSet<>();

    /**
     * Creates a new DtrContext.
     *
     * @param renderMachine the render machine for documentation output
     */
    public DtrContext(RenderMachine renderMachine) {
        this.renderMachine = renderMachine;
    }

    // ========================================================================
    // RenderMachineCommands implementation
    // ========================================================================

    @Override
    public void say(String text) {
        documentedMethodNames.add("say");
        renderMachine.say(text);
    }

    @Override
    public void sayNextSection(String headline) {
        documentedMethodNames.add("sayNextSection");
        renderMachine.sayNextSection(headline);
    }

    @Override
    public void sayRaw(String rawHtml) {
        renderMachine.sayRaw(rawHtml);
    }

    @Override
    public void sayTable(String[][] data) {
        documentedMethodNames.add("sayTable");
        renderMachine.sayTable(data);
    }

    @Override
    public void sayCode(String code, String language) {
        documentedMethodNames.add("sayCode");
        renderMachine.sayCode(code, language);
    }

    @Override
    public void sayWarning(String message) {
        renderMachine.sayWarning(message);
    }

    @Override
    public void sayNote(String message) {
        renderMachine.sayNote(message);
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        renderMachine.sayKeyValue(pairs);
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        renderMachine.sayUnorderedList(items);
    }

    @Override
    public void sayOrderedList(List<String> items) {
        renderMachine.sayOrderedList(items);
    }

    @Override
    public void sayJson(Object object) {
        renderMachine.sayJson(object);
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        renderMachine.sayAssertions(assertions);
    }

    @Override
    public void sayCite(String citationKey) {
        renderMachine.sayCite(citationKey);
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        renderMachine.sayCite(citationKey, pageRef);
    }

    @Override
    public void sayFootnote(String text) {
        renderMachine.sayFootnote(text);
    }

    @Override
    public void sayRef(DocTestRef ref) {
        renderMachine.sayRef(ref);
    }

    /**
     * Convenience method to create and render a cross-reference to another DocTest's section.
     *
     * <p>Creates a {@link DocTestRef} and delegates to {@link #sayRef(DocTestRef)}.
     * The reference is rendered as a markdown link in Markdown mode or as a LaTeX
     * cross-reference command in LaTeX mode. In LaTeX, the resolved section number
     * (e.g., "Section 3.2") is automatically substituted after compilation.</p>
     *
     * @param docTestClass the target DocTest class (must not be null)
     * @param anchor the section/anchor name within that DocTest, e.g., "user-registration"
     *               (typically derived from @DocSection annotation value)
     * @see DocTestRef#of(Class, String)
     */
    public void sayRef(Class<?> docTestClass, String anchor) {
        sayRef(DocTestRef.of(docTestClass, anchor));
    }

    // ========================================================================
    // Code model introspection
    // ========================================================================

    @Override
    public void sayCodeModel(Class<?> clazz) {
        renderMachine.sayCodeModel(clazz);
    }

    @Override
    public void sayMethodSignature(java.lang.reflect.Method method) {
        renderMachine.sayMethodSignature(method);
    }

    /**
     * @deprecated Use {@link #sayMethodSignature(java.lang.reflect.Method)} instead.
     *             This method name is ambiguous - it documents method signatures, not full code models.
     *             Scheduled for removal in a future release.
     */
    @Deprecated(forRemoval = true, since = "2026.4.0")
    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        renderMachine.sayCodeModel(method);
    }

    @Override
    public void sayCallSite() {
        renderMachine.sayCallSite();
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        renderMachine.sayAnnotationProfile(clazz);
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        renderMachine.sayClassHierarchy(clazz);
    }

    @Override
    public void sayStringProfile(String text) {
        renderMachine.sayStringProfile(text);
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        renderMachine.sayReflectiveDiff(before, after);
    }

    // ========================================================================
    // Java 26 Code Reflection + Blue Ocean innovations
    // ========================================================================

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        renderMachine.sayControlFlowGraph(method);
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        renderMachine.sayCallGraph(clazz);
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        renderMachine.sayOpProfile(method);
    }

    @Override
    public void sayBenchmark(String label, Runnable task) {
        renderMachine.sayBenchmark(label, task);
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        renderMachine.sayBenchmark(label, task, warmupRounds, measureRounds);
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        renderMachine.sayMermaid(diagramDsl);
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        renderMachine.sayClassDiagram(classes);
    }

    /**
     * Renders a documentation coverage report for the given classes, using the
     * set of method names tracked during this test via say* calls.
     *
     * @param classes the classes whose public API to check for coverage
     */
    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        if (renderMachine instanceof RenderMachineImpl impl) {
            for (Class<?> clazz : classes) {
                List<CoverageRow> rows = DocCoverageAnalyzer.analyze(clazz, documentedMethodNames);
                impl.sayDocCoverage(clazz, rows);
            }
        } else {
            renderMachine.sayDocCoverage(classes);
        }
    }

    @Override
    public void sayEnvProfile() {
        renderMachine.sayEnvProfile();
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        renderMachine.sayRecordComponents(recordClass);
    }

    @Override
    public void sayException(Throwable t) {
        renderMachine.sayException(t);
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        renderMachine.sayAsciiChart(label, values, xLabels);
    }

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        renderMachine.sayContractVerification(contract, implementations);
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        renderMachine.sayEvolutionTimeline(clazz, maxEntries);
    }

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        renderMachine.sayJavadoc(method);
    }

    @Override
    public void saySystemProperties() {
        renderMachine.saySystemProperties();
    }

    @Override
    public void saySystemProperties(String regexFilter) {
        renderMachine.saySystemProperties(regexFilter);
    }

    @Override
    public void saySecurityManager() {
        renderMachine.saySecurityManager();
    }

    @Override
    public void sayModuleDependencies(Class<?>... classes) {
        renderMachine.sayModuleDependencies(classes);
    }

    @Override
    public void sayThreadDump() {
        renderMachine.sayThreadDump();
    }

    @Override
    public void sayOperatingSystem() {
        renderMachine.sayOperatingSystem();
    }

    // ========================================================================
    // Presentation-specific methods (slides/blog only)
    // ========================================================================

    /**
     * Renders content only for slide output (ignored by markdown/blog render machines).
     *
     * @param text the text to render on slides only
     */
    public void saySlideOnly(String text) {
        renderMachine.saySlideOnly(text);
    }

    /**
     * Renders content only for documentation/blog output (ignored by slide render machines).
     *
     * @param text the text to render in docs only
     */
    public void sayDocOnly(String text) {
        renderMachine.sayDocOnly(text);
    }

    /**
     * Renders speaker notes for slides (ignored by doc/blog render machines).
     *
     * @param text the speaker notes text
     */
    public void saySpeakerNote(String text) {
        renderMachine.saySpeakerNote(text);
    }

    /**
     * Renders a hero image for blogs and slides (ignored by other formats).
     *
     * @param altText the alt text for the image
     */
    public void sayHeroImage(String altText) {
        renderMachine.sayHeroImage(altText);
    }

    /**
     * Renders a tweetable excerpt (≤280 chars) for social media queue.
     *
     * @param text the text to tweet (will be truncated to 280 chars)
     */
    public void sayTweetable(String text) {
        renderMachine.sayTweetable(text);
    }

    /**
     * Renders a TLDR (too long; didn't read) summary for blogs.
     *
     * @param text the summary text
     */
    public void sayTldr(String text) {
        renderMachine.sayTldr(text);
    }

    /**
     * Renders a call-to-action link for blogs.
     *
     * @param url the URL for the CTA button/link
     */
    public void sayCallToAction(String url) {
        renderMachine.sayCallToAction(url);
    }

    // ========================================================================
    // Assertion helpers (assert + document in one call)
    // ========================================================================

    /**
     * Runs a Hamcrest assertion and documents the result as a table row.
     * Passes a {@code ✓ PASS} label on success; rethrows on failure.
     *
     * @param label the check description
     * @param actual the actual value
     * @param matcher the Hamcrest matcher
     */
    public <T> void sayAndAssertThat(String label, T actual, org.hamcrest.Matcher<? super T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(label, actual, matcher);
        sayAssertions(Map.of(label, "✓ PASS"));
    }

    /**
     * Overload for {@code long} primitives — avoids ambiguous autoboxing.
     *
     * @param label the check description
     * @param actual the actual value
     * @param matcher the Hamcrest matcher
     */
    public void sayAndAssertThat(String label, long actual, org.hamcrest.Matcher<Long> matcher) {
        org.hamcrest.MatcherAssert.assertThat(label, actual, matcher);
        sayAssertions(Map.of(label, "✓ PASS"));
    }

    /**
     * Overload for {@code int} primitives.
     *
     * @param label the check description
     * @param actual the actual value
     * @param matcher the Hamcrest matcher
     */
    public void sayAndAssertThat(String label, int actual, org.hamcrest.Matcher<Integer> matcher) {
        org.hamcrest.MatcherAssert.assertThat(label, actual, matcher);
        sayAssertions(Map.of(label, "✓ PASS"));
    }

    /**
     * Overload for {@code boolean} primitives.
     *
     * @param label the check description
     * @param actual the actual value
     * @param matcher the Hamcrest matcher
     */
    public void sayAndAssertThat(String label, boolean actual, org.hamcrest.Matcher<Boolean> matcher) {
        org.hamcrest.MatcherAssert.assertThat(label, actual, matcher);
        sayAssertions(Map.of(label, "✓ PASS"));
    }

    // ========================================================================
    // Accessors for internal components
    // ========================================================================

    /**
     * Gets the underlying RenderMachine.
     *
     * @return the render machine
     */
    public RenderMachine getRenderMachine() {
        return renderMachine;
    }
}

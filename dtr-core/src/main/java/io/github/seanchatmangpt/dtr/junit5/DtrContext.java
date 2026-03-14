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

    // ========================================================================
    // Code model introspection
    // ========================================================================

    @Override
    public void sayCodeModel(Class<?> clazz) {
        renderMachine.sayCodeModel(clazz);
    }

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

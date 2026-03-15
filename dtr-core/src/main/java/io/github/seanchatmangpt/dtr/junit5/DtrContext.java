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

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        renderMachine.sayJavadoc(method);
    }

    // ========================================================================
    // 80/20 Blue Ocean Innovations — v2.7.0 delegations
    // ========================================================================

    @Override
    public void sayTimeSeries(String label, long[] values, String[] timestamps) {
        renderMachine.sayTimeSeries(label, values, timestamps);
    }

    @Override
    public void sayComplexityProfile(String label,
                                     java.util.function.IntFunction<Runnable> taskFactory,
                                     int[] ns) {
        renderMachine.sayComplexityProfile(label, taskFactory, ns);
    }

    @Override
    public void sayStateMachine(String title, java.util.Map<String, String> transitions) {
        renderMachine.sayStateMachine(title, transitions);
    }

    @Override
    public void sayDataFlow(String title,
                            java.util.List<String> stages,
                            java.util.List<java.util.function.Function<Object, Object>> transforms,
                            Object sample) {
        renderMachine.sayDataFlow(title, stages, transforms, sample);
    }

    @Override
    public void sayApiDiff(Class<?> before, Class<?> after) {
        renderMachine.sayApiDiff(before, after);
    }

    @Override
    public void sayHeatmap(String title, double[][] matrix, String[] rowLabels, String[] colLabels) {
        renderMachine.sayHeatmap(title, matrix, rowLabels, colLabels);
    }

    @Override
    public void sayPropertyBased(String property,
                                 java.util.function.Predicate<Object> check,
                                 java.util.List<Object> inputs) {
        renderMachine.sayPropertyBased(property, check, inputs);
    }

    @Override
    public void sayParallelTrace(String title,
                                 java.util.List<String> agents,
                                 java.util.List<long[]> timeSlots) {
        renderMachine.sayParallelTrace(title, agents, timeSlots);
    }

    @Override
    public void sayDecisionTree(String title, java.util.Map<String, Object> branches) {
        renderMachine.sayDecisionTree(title, branches);
    }

    @Override
    public void sayAgentLoop(String agentName,
                             java.util.List<String> observations,
                             java.util.List<String> decisions,
                             java.util.List<String> tools) {
        renderMachine.sayAgentLoop(agentName, observations, decisions, tools);
    }

    // ── Toyota Production System + Joe Armstrong Blue Ocean innovations ────────

    @Override
    public void saySupervisionTree(String title,
                                   java.util.Map<String, java.util.List<String>> supervisors) {
        renderMachine.saySupervisionTree(title, supervisors);
    }

    @Override
    public void sayActorMessages(String title,
                                 java.util.List<String> actors,
                                 java.util.List<String[]> messages) {
        renderMachine.sayActorMessages(title, actors, messages);
    }

    @Override
    public void sayFaultTolerance(String scenario,
                                  java.util.List<String> failures,
                                  java.util.List<String> recoveries) {
        renderMachine.sayFaultTolerance(scenario, failures, recoveries);
    }

    @Override
    public void sayKaizen(String metric, long[] before, long[] after, String unit) {
        renderMachine.sayKaizen(metric, before, after, unit);
    }

    @Override
    public void sayKanban(String board,
                          java.util.List<String> backlog,
                          java.util.List<String> wip,
                          java.util.List<String> done) {
        renderMachine.sayKanban(board, backlog, wip, done);
    }

    @Override
    public void sayPatternMatch(String title,
                                java.util.List<String> patterns,
                                java.util.List<String> values,
                                java.util.List<Boolean> matches) {
        renderMachine.sayPatternMatch(title, patterns, values, matches);
    }

    @Override
    public void sayAndon(String system,
                         java.util.List<String> stations,
                         java.util.List<String> statuses) {
        renderMachine.sayAndon(system, stations, statuses);
    }

    @Override
    public void sayMuda(String process,
                        java.util.List<String> wastes,
                        java.util.List<String> improvements) {
        renderMachine.sayMuda(process, wastes, improvements);
    }

    @Override
    public void sayValueStream(String product,
                               java.util.List<String> steps,
                               long[] cycleTimeMs) {
        renderMachine.sayValueStream(product, steps, cycleTimeMs);
    }

    @Override
    public void sayPokaYoke(String operation,
                            java.util.List<String> mistakeProofs,
                            java.util.List<Boolean> verified) {
        renderMachine.sayPokaYoke(operation, mistakeProofs, verified);
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

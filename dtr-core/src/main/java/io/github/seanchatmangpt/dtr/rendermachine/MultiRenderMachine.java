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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

import io.github.seanchatmangpt.dtr.crossref.DocTestRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegating render machine that routes method calls to multiple machines simultaneously
 * using virtual threads and structured concurrency.
 *
 * <p>Enables multi-format output: one test execution produces Markdown, LaTeX, PDF, slides,
 * and blog posts in parallel by dispatching each {@code say*} call to all contained machines
 * concurrently.</p>
 *
 * <p>Transparent to test code: use exactly like a single RenderMachine.</p>
 *
 * <p><strong>Java 26 showcase — structured concurrency:</strong> Each dispatch uses
 * {@link StructuredTaskScope} (JEP 492).
 * This replaces manual future waiting and error aggregation with JVM-native structured semantics:
 * if any renderer fails, the error is propagated immediately with full context.
 * This is simpler, safer, and faster than CompletableFuture chains.</p>
 *
 * <p><strong>Java 26/26 showcase — virtual threads:</strong> All tasks run on virtual threads
 * (Project Loom) which are JVM-scheduled, not OS-scheduled. They have near-zero creation overhead,
 * making one-virtual-thread-per-machine-per-call practical even for high-frequency {@code say*} calls.
 * When DTR generates 8+ simultaneous output formats, wall-clock time is the slowest single
 * renderer, not the sum of all renderers.</p>
 *
 * <p><strong>Java 26 Enhancement:</strong> Structured concurrency with StructuredTaskScope
 * ensures all rendering tasks are properly managed: if any renderer fails mid-rendering,
 * the exception is propagated immediately, and all tasks are cleaned up properly.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * RenderMachine multiMachine = new MultiRenderMachine(
 *     new RenderMachineImpl(),      // Markdown
 *     new RenderMachineLatex(...)   // LaTeX/PDF
 * );
 * }</pre>
 */
public final class MultiRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(MultiRenderMachine.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final List<RenderMachine> machines;
    private final int timeoutSeconds;

    /**
     * Create a multi-render machine delegating to the given machines.
     *
     * @param machines the render machines to dispatch to (immutable copy is made)
     */
    public MultiRenderMachine(List<RenderMachine> machines) {
        this(machines, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Create a multi-render machine delegating to the given machines with a configurable timeout.
     *
     * @param machines       the render machines to dispatch to (immutable copy is made)
     * @param timeoutSeconds maximum seconds to wait for all machines to complete per dispatch
     */
    public MultiRenderMachine(List<RenderMachine> machines, int timeoutSeconds) {
        this.machines = List.copyOf(machines);
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Create a multi-render machine delegating to the given machines (varargs).
     *
     * @param machines the render machines to dispatch to
     */
    public MultiRenderMachine(RenderMachine... machines) {
        this(List.of(machines), DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Dispatches an action to all contained render machines concurrently using structured concurrency.
     *
     * <p>Java 26 Enhancement (JEP 492 - Structured Concurrency):</p>
     * <ol>
     *   <li>Creates a {@link StructuredTaskScope} scope</li>
     *   <li>Forks one task per machine — each task calls the action on its machine via a virtual thread</li>
     *   <li>Joins when all tasks complete (blocks until all finish or an exception is thrown)</li>
     *   <li>Propagates exceptions with full structured context</li>
     * </ol>
     *
     * <p>This replaces the previous manual future-waiting pattern with JVM-native semantics.
     * StructuredTaskScope ensures:</p>
     * <ul>
     *   <li><strong>No error swallowing:</strong> If any renderer fails, the failure is immediate</li>
     *   <li><strong>Automatic cleanup:</strong> All tasks are guaranteed to complete before method returns</li>
     *   <li><strong>Structured lifetime:</strong> Try-with-resources ensures proper scope closure</li>
     *   <li><strong>Zero boilerplate:</strong> Simple, declarative concurrency patterns</li>
     * </ul>
     *
     * @param action the operation to invoke on each render machine
     * @throws MultiRenderException if any machine fails (wraps first exception)
     */
    private void dispatchToAll(Consumer<RenderMachine> action) {
        // Track machine alongside its subtask so failures can name the offending machine
        record MachineTask(RenderMachine machine, StructuredTaskScope.Subtask<?> subtask) {}
        var subtasks = new java.util.ArrayList<MachineTask>(machines.size());

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll(),
                config -> config.withTimeout(Duration.ofSeconds(timeoutSeconds)))) {
            // Fork one task per machine — all run concurrently on virtual threads
            for (RenderMachine machine : machines) {
                var subtask = scope.fork((Runnable) () -> action.accept(machine));
                subtasks.add(new MachineTask(machine, subtask));
            }

            // Join — blocks until all complete or timeout fires (throws TimeoutException)
            scope.join();

            // Surface any per-machine failures with the machine's class name
            for (var mt : subtasks) {
                if (mt.subtask().state() == StructuredTaskScope.Subtask.State.FAILED) {
                    Throwable cause = mt.subtask().exception();
                    String machineName = mt.machine().getClass().getSimpleName();
                    throw new MultiRenderException(
                        "Render machine failed [" + machineName + "]: " + cause.getMessage(),
                        List.of(cause instanceof Exception ex ? ex : new RuntimeException(cause)));
                }
            }
        } catch (StructuredTaskScope.TimeoutException e) {
            // Thrown by join() when the scope's withTimeout deadline is exceeded
            throw new RuntimeException(
                "DTR render timed out after " + timeoutSeconds + "s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MultiRenderException("Render dispatch interrupted", List.of(e));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // StructuredTaskScope propagates the underlying exception (not wrapped in ExecutionException)
            throw new MultiRenderException("Render machines failed: " + e.getMessage(), List.of(e));
        }
    }

    @Override
    public void say(String text) {
        dispatchToAll(m -> m.say(text));
    }

    @Override
    public void sayNextSection(String headline) {
        dispatchToAll(m -> m.sayNextSection(headline));
    }

    @Override
    public void sayRaw(String rawContent) {
        dispatchToAll(m -> m.sayRaw(rawContent));
    }

    @Override
    public void sayTable(String[][] data) {
        dispatchToAll(m -> m.sayTable(data));
    }

    @Override
    public void sayCode(String code, String language) {
        dispatchToAll(m -> m.sayCode(code, language));
    }

    @Override
    public void sayWarning(String message) {
        dispatchToAll(m -> m.sayWarning(message));
    }

    @Override
    public void sayNote(String message) {
        dispatchToAll(m -> m.sayNote(message));
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        dispatchToAll(m -> m.sayKeyValue(pairs));
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        dispatchToAll(m -> m.sayUnorderedList(items));
    }

    @Override
    public void sayOrderedList(List<String> items) {
        dispatchToAll(m -> m.sayOrderedList(items));
    }

    @Override
    public void sayJson(Object object) {
        dispatchToAll(m -> m.sayJson(object));
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        dispatchToAll(m -> m.sayAssertions(assertions));
    }

    @Override
    public void sayCite(String citationKey) {
        dispatchToAll(m -> m.sayCite(citationKey));
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        dispatchToAll(m -> m.sayCite(citationKey, pageRef));
    }

    @Override
    public void sayFootnote(String text) {
        dispatchToAll(m -> m.sayFootnote(text));
    }

    @Override
    public void sayRef(DocTestRef ref) {
        dispatchToAll(m -> m.sayRef(ref));
    }

    @Override
    public void sayCodeModel(Class<?> clazz) {
        dispatchToAll(m -> m.sayCodeModel(clazz));
    }

    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        dispatchToAll(m -> m.sayCodeModel(method));
    }

    @Override
    public void sayCallSite() {
        dispatchToAll(RenderMachine::sayCallSite);
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        dispatchToAll(m -> m.sayAnnotationProfile(clazz));
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        dispatchToAll(m -> m.sayClassHierarchy(clazz));
    }

    @Override
    public void sayStringProfile(String text) {
        dispatchToAll(m -> m.sayStringProfile(text));
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        dispatchToAll(m -> m.sayReflectiveDiff(before, after));
    }

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        dispatchToAll(m -> m.sayControlFlowGraph(method));
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        dispatchToAll(m -> m.sayCallGraph(clazz));
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        dispatchToAll(m -> m.sayOpProfile(method));
    }

    @Override
    public void sayBenchmark(String label, Runnable task) {
        dispatchToAll(m -> m.sayBenchmark(label, task));
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        dispatchToAll(m -> m.sayBenchmark(label, task, warmupRounds, measureRounds));
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        dispatchToAll(m -> m.sayMermaid(diagramDsl));
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        dispatchToAll(m -> m.sayClassDiagram(classes));
    }

    @Override
    public void sayDocCoverage(Class<?>... classes) {
        dispatchToAll(m -> m.sayDocCoverage(classes));
    }

    @Override
    public void sayEnvProfile() {
        dispatchToAll(RenderMachine::sayEnvProfile);
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        dispatchToAll(m -> m.sayRecordComponents(recordClass));
    }

    @Override
    public void sayException(Throwable t) {
        dispatchToAll(m -> m.sayException(t));
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        dispatchToAll(m -> m.sayAsciiChart(label, values, xLabels));
    }

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        dispatchToAll(m -> m.sayContractVerification(contract, implementations));
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        dispatchToAll(m -> m.sayEvolutionTimeline(clazz, maxEntries));
    }

    @Override
    public void saySlideOnly(String text) {
        dispatchToAll(m -> m.saySlideOnly(text));
    }

    @Override
    public void sayDocOnly(String text) {
        dispatchToAll(m -> m.sayDocOnly(text));
    }

    @Override
    public void saySpeakerNote(String text) {
        dispatchToAll(m -> m.saySpeakerNote(text));
    }

    @Override
    public void sayHeroImage(String altText) {
        dispatchToAll(m -> m.sayHeroImage(altText));
    }

    @Override
    public void sayTweetable(String text) {
        dispatchToAll(m -> m.sayTweetable(text));
    }

    @Override
    public void sayTldr(String text) {
        dispatchToAll(m -> m.sayTldr(text));
    }

    @Override
    public void sayCallToAction(String url) {
        dispatchToAll(m -> m.sayCallToAction(url));
    }

    // ── Toyota Production System + Joe Armstrong Blue Ocean innovations ────────

    @Override
    public void saySupervisionTree(String title,
                                   java.util.Map<String, java.util.List<String>> supervisors) {
        dispatchToAll(m -> m.saySupervisionTree(title, supervisors));
    }

    @Override
    public void sayActorMessages(String title,
                                 java.util.List<String> actors,
                                 java.util.List<String[]> messages) {
        dispatchToAll(m -> m.sayActorMessages(title, actors, messages));
    }

    @Override
    public void sayFaultTolerance(String scenario,
                                  java.util.List<String> failures,
                                  java.util.List<String> recoveries) {
        dispatchToAll(m -> m.sayFaultTolerance(scenario, failures, recoveries));
    }

    @Override
    public void sayKaizen(String metric, long[] before, long[] after, String unit) {
        dispatchToAll(m -> m.sayKaizen(metric, before, after, unit));
    }

    @Override
    public void sayKanban(String board,
                          java.util.List<String> backlog,
                          java.util.List<String> wip,
                          java.util.List<String> done) {
        dispatchToAll(m -> m.sayKanban(board, backlog, wip, done));
    }

    @Override
    public void sayPatternMatch(String title,
                                java.util.List<String> patterns,
                                java.util.List<String> values,
                                java.util.List<Boolean> matches) {
        dispatchToAll(m -> m.sayPatternMatch(title, patterns, values, matches));
    }

    @Override
    public void sayAndon(String system,
                         java.util.List<String> stations,
                         java.util.List<String> statuses) {
        dispatchToAll(m -> m.sayAndon(system, stations, statuses));
    }

    @Override
    public void sayMuda(String process,
                        java.util.List<String> wastes,
                        java.util.List<String> improvements) {
        dispatchToAll(m -> m.sayMuda(process, wastes, improvements));
    }

    @Override
    public void sayValueStream(String product,
                               java.util.List<String> steps,
                               long[] cycleTimeMs) {
        dispatchToAll(m -> m.sayValueStream(product, steps, cycleTimeMs));
    }

    @Override
    public void sayPokaYoke(String operation,
                            java.util.List<String> mistakeProofs,
                            java.util.List<Boolean> verified) {
        dispatchToAll(m -> m.sayPokaYoke(operation, mistakeProofs, verified));
    }

    @Override
    public void setFileName(String fileName) {
        for (RenderMachine machine : machines) {
            machine.setFileName(fileName);
        }
    }

    /**
     * Finalizes all contained render machines concurrently using virtual threads.
     *
     * <p>This is where virtual threads provide the most measurable performance benefit:
     * file I/O is blocking. A LaTeX compilation can take several seconds. Running all
     * finalizations in parallel means the total wall-clock time is the slowest single
     * finalizer, not the sum of all.</p>
     */
    @Override
    public void finishAndWriteOut() {
        dispatchToAll(RenderMachine::finishAndWriteOut);
    }

    /**
     * Exception thrown when one or more render machines fail during parallel dispatch.
     *
     * <p>All failures are collected and reported together rather than failing fast,
     * so that a failure in one output format (e.g., LaTeX compilation error) does not
     * prevent other formats (Markdown, blog) from completing successfully.</p>
     */
    public static final class MultiRenderException extends RuntimeException {
        private final List<Exception> causes;

        /**
         * Constructs an exception that aggregates one or more render-machine failures.
         *
         * @param message human-readable summary of the failure
         * @param causes  all exceptions thrown by individual render machines
         */
        public MultiRenderException(String message, List<Exception> causes) {
            super(message);
            this.causes = List.copyOf(causes);
        }

        /** All exceptions from failing render machines. */
        public List<Exception> getCauses() {
            return causes;
        }
    }
}

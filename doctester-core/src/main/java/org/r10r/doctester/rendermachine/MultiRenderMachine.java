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
package org.r10r.doctester.rendermachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.crossref.DocTestRef;
import org.hamcrest.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegating render machine that routes method calls to multiple machines simultaneously
 * using virtual threads.
 *
 * <p>Enables multi-format output: one test execution produces Markdown, LaTeX, PDF, slides,
 * and blog posts in parallel by dispatching each {@code say*} call to all contained machines
 * concurrently.</p>
 *
 * <p>Transparent to test code: use exactly like a single RenderMachine.</p>
 *
 * <p><strong>Java 25/26 showcase — virtual threads:</strong> Each dispatch uses
 * {@link Executors#newVirtualThreadPerTaskExecutor()} to run all machines concurrently.
 * Virtual threads (Project Loom) are JVM-scheduled, not OS-scheduled. They have near-zero
 * creation overhead, making one-virtual-thread-per-machine-per-call practical even for
 * high-frequency {@code say*} calls. When DocTester generates 11 simultaneous output
 * formats, wall-clock time is the slowest single renderer, not the sum of all renderers.</p>
 *
 * <p><strong>Java 25/26 showcase — unnamed patterns:</strong> The {@code dispatchToAll}
 * helper uses {@code var _} for ignored future results in void-dispatch paths, demonstrating
 * unnamed variables (JEP 456, stable in Java 22+).</p>
 *
 * <p><strong>Java 25/26 showcase — pattern matching instanceof:</strong> The
 * {@code dispatchToAll} error handling uses pattern matching to extract the underlying
 * exception without explicit casting:</p>
 * <pre>{@code
 * e.getCause() instanceof Exception ex ? ex : new RuntimeException(e.getCause())
 * }</pre>
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

    private final List<RenderMachine> machines;

    /**
     * Create a multi-render machine delegating to the given machines.
     *
     * @param machines the render machines to dispatch to (immutable copy is made)
     */
    public MultiRenderMachine(List<RenderMachine> machines) {
        this.machines = List.copyOf(machines);
    }

    /**
     * Create a multi-render machine delegating to the given machines (varargs).
     *
     * @param machines the render machines to dispatch to
     */
    public MultiRenderMachine(RenderMachine... machines) {
        this.machines = List.of(machines);
    }

    /**
     * Dispatches an action to all contained render machines concurrently using virtual threads.
     *
     * <p>This is the core of the Java 25/26 showcase in this class. Every {@code say*} method
     * delegates to this single helper, which:</p>
     * <ol>
     *   <li>Creates a virtual-thread-per-task executor (zero OS-thread overhead)</li>
     *   <li>Submits one task per machine — each task calls the action on its machine</li>
     *   <li>Collects all futures, waits for completion, aggregates any failures</li>
     *   <li>Throws {@link MultiRenderException} if any machine fails</li>
     * </ol>
     *
     * <p>The try-with-resources on the executor ensures all virtual threads are joined
     * before the method returns — structured concurrency without the Structured Concurrency
     * API (JEP 453).</p>
     *
     * @param action the operation to invoke on each render machine
     */
    private void dispatchToAll(Consumer<RenderMachine> action) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit one virtual thread per machine — all run concurrently
            var futures = machines.stream()
                .map(m -> executor.submit(() -> {
                    action.accept(m);
                    return null;
                }))
                .toList();

            List<Exception> errors = new ArrayList<>();
            for (var future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    // Pattern matching instanceof — no explicit cast needed
                    errors.add(e.getCause() instanceof Exception ex
                        ? ex
                        : new RuntimeException(e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(e);
                }
            }

            if (!errors.isEmpty()) {
                throw new MultiRenderException("Parallel dispatch to render machines failed", errors);
            }
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

    @Override
    public List<Cookie> sayAndGetCookies() {
        // Only the first machine's cookies matter; all observe the same browser state
        return machines.getFirst().sayAndGetCookies();
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        return machines.getFirst().sayAndGetCookieWithName(name);
    }

    @Override
    public Response sayAndMakeRequest(Request httpRequest) {
        // First machine executes the HTTP request and documents it
        Response response = machines.getFirst().sayAndMakeRequest(httpRequest);

        // Remaining machines document the same exchange without re-executing HTTP
        // Uses SequencedCollection.subList semantics for the tail
        if (machines.size() > 1) {
            machines.subList(1, machines.size()).forEach(m ->
                m.sayRaw("*[HTTP exchange documented by primary render machine]*"));
        }

        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        dispatchToAll(m -> m.sayAndAssertThat(message, actual, matcher));
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        dispatchToAll(m -> m.sayAndAssertThat(message, reason, actual, matcher));
    }

    @Override
    public void setTestBrowser(TestBrowser testBrowser) {
        for (RenderMachine machine : machines) {
            machine.setTestBrowser(testBrowser);
        }
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

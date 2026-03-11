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

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.crossref.DocTestRef;
import org.hamcrest.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegating render machine that routes method calls to multiple machines simultaneously.
 *
 * Enables multi-format output: one test execution produces Markdown, LaTeX, PDF, etc.
 * in parallel by delegating each say* method call to all contained machines.
 *
 * Transparent to test code: use exactly like a single RenderMachine.
 *
 * Example:
 * <pre>
 * RenderMachine multiMachine = new MultiRenderMachine(
 *     new RenderMachineImpl(),      // Markdown
 *     new RenderMachineLatex(...)  // LaTeX/PDF
 * );
 * </pre>
 */
public final class MultiRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(MultiRenderMachine.class);

    private final List<RenderMachine> machines;

    /**
     * Create a multi-render machine delegating to the given machines.
     */
    public MultiRenderMachine(List<RenderMachine> machines) {
        this.machines = List.copyOf(machines); // Immutable copy
    }

    /**
     * Create a multi-render machine delegating to the given machines (varargs).
     */
    public MultiRenderMachine(RenderMachine... machines) {
        this.machines = List.of(machines);
    }

    @Override
    public void say(String text) {
        for (RenderMachine machine : machines) {
            machine.say(text);
        }
    }

    @Override
    public void sayNextSection(String headline) {
        for (RenderMachine machine : machines) {
            machine.sayNextSection(headline);
        }
    }

    @Override
    public void sayRaw(String rawContent) {
        for (RenderMachine machine : machines) {
            machine.sayRaw(rawContent);
        }
    }

    @Override
    public void sayTable(String[][] data) {
        for (RenderMachine machine : machines) {
            machine.sayTable(data);
        }
    }

    @Override
    public void sayCode(String code, String language) {
        for (RenderMachine machine : machines) {
            machine.sayCode(code, language);
        }
    }

    @Override
    public void sayWarning(String message) {
        for (RenderMachine machine : machines) {
            machine.sayWarning(message);
        }
    }

    @Override
    public void sayNote(String message) {
        for (RenderMachine machine : machines) {
            machine.sayNote(message);
        }
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        for (RenderMachine machine : machines) {
            machine.sayKeyValue(pairs);
        }
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        for (RenderMachine machine : machines) {
            machine.sayUnorderedList(items);
        }
    }

    @Override
    public void sayOrderedList(List<String> items) {
        for (RenderMachine machine : machines) {
            machine.sayOrderedList(items);
        }
    }

    @Override
    public void sayJson(Object object) {
        for (RenderMachine machine : machines) {
            machine.sayJson(object);
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        for (RenderMachine machine : machines) {
            machine.sayAssertions(assertions);
        }
    }

    @Override
    public void sayCite(String citationKey) {
        for (RenderMachine machine : machines) {
            machine.sayCite(citationKey);
        }
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        for (RenderMachine machine : machines) {
            machine.sayCite(citationKey, pageRef);
        }
    }

    @Override
    public void sayFootnote(String text) {
        for (RenderMachine machine : machines) {
            machine.sayFootnote(text);
        }
    }

    @Override
    public void sayRef(DocTestRef ref) {
        for (RenderMachine machine : machines) {
            machine.sayRef(ref);
        }
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        // Only first machine's cookies matter; all observe same browser
        return machines.get(0).sayAndGetCookies();
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        return machines.get(0).sayAndGetCookieWithName(name);
    }

    @Override
    public Response sayAndMakeRequest(Request httpRequest) {
        // Only first machine executes HTTP; others document the same request/response
        Response response = machines.get(0).sayAndMakeRequest(httpRequest);

        // Other machines need to see the same HTTP exchange (without re-executing)
        // For now, only first machine documents the actual request
        // TODO: Add a method to machines to document pre-executed request/response

        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        for (RenderMachine machine : machines) {
            machine.sayAndAssertThat(message, actual, matcher);
        }
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        for (RenderMachine machine : machines) {
            machine.sayAndAssertThat(message, reason, actual, matcher);
        }
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

    @Override
    public void finishAndWriteOut() {
        List<Exception> errors = new ArrayList<>();

        for (RenderMachine machine : machines) {
            try {
                machine.finishAndWriteOut();
            } catch (Exception e) {
                logger.warn("Error finalizing render machine: {}", machine.getClass().getSimpleName(), e);
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            var msg = new StringBuilder("Multiple render machines failed finalization:\n");
            for (Exception e : errors) {
                msg.append("  - ").append(e.getMessage()).append("\n");
            }
            throw new MultiRenderException(msg.toString(), errors);
        }
    }

    /**
     * Exception thrown when multiple render machines fail during finalization.
     */
    public static final class MultiRenderException extends RuntimeException {
        private final List<Exception> causes;

        public MultiRenderException(String message, List<Exception> causes) {
            super(message);
            this.causes = List.copyOf(causes);
        }

        public List<Exception> getCauses() {
            return causes;
        }
    }
}

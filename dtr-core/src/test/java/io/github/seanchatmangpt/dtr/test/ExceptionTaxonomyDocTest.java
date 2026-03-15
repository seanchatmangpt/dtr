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
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exception Taxonomy and Recovery Documentation — DTR documents exception
 * hierarchies, failure modes, and recovery strategies from live exception
 * instances.
 *
 * <p>This is a Blue Ocean innovation: no other library can document
 * "this is exactly what the exception looks like at runtime" because
 * the docs ARE the test. Every stack trace, cause chain, and message
 * shown in the output was captured from a real thrown exception.</p>
 *
 * <p>All exceptions are thrown and caught live during test execution.
 * No stack traces are hardcoded or simulated.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ExceptionTaxonomyDocTest extends DtrTest {

    // =========================================================================
    // Custom domain exception hierarchy for DTR
    // =========================================================================

    static class DtrProcessingException extends RuntimeException {
        public DtrProcessingException(String msg) {
            super(msg);
        }

        public DtrProcessingException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    static class DtrValidationException extends DtrProcessingException {
        private final String field;

        public DtrValidationException(String field, String msg) {
            super(msg);
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Exception Hierarchy — Checked vs Unchecked
    // =========================================================================

    @Test
    void a1_exception_hierarchy() {
        sayNextSection("Exception Hierarchy: Checked vs Unchecked");

        say(
            "Java divides throwable types into two fundamental branches. " +
            "Checked exceptions (subclasses of Exception but not RuntimeException) " +
            "are enforced at compile time — callers must either catch them or declare " +
            "them with `throws`. Unchecked exceptions (RuntimeException and Error) " +
            "bypass this enforcement and propagate freely through the call stack. " +
            "Understanding which branch to use determines API usability and error-recovery design."
        );

        sayMermaid(
            "classDiagram\n" +
            "    Throwable <|-- Error\n" +
            "    Throwable <|-- Exception\n" +
            "    Exception <|-- RuntimeException\n" +
            "    RuntimeException <|-- NullPointerException\n" +
            "    RuntimeException <|-- IllegalArgumentException\n" +
            "    RuntimeException <|-- IllegalStateException\n" +
            "    RuntimeException <|-- UnsupportedOperationException\n" +
            "    Exception <|-- IOException\n" +
            "    Exception <|-- ReflectiveOperationException\n" +
            "    Error <|-- OutOfMemoryError\n" +
            "    Error <|-- StackOverflowError\n"
        );

        say(
            "The following class hierarchy trees are derived from live reflection " +
            "on each exception class loaded in this JVM. The hierarchy shown is " +
            "exactly what the runtime sees — not a hand-drawn diagram."
        );

        say("NullPointerException hierarchy (unchecked):");
        sayClassHierarchy(NullPointerException.class);

        say("IllegalArgumentException hierarchy (unchecked):");
        sayClassHierarchy(IllegalArgumentException.class);

        say("IllegalStateException hierarchy (unchecked):");
        sayClassHierarchy(IllegalStateException.class);

        say("UnsupportedOperationException hierarchy (unchecked):");
        sayClassHierarchy(UnsupportedOperationException.class);

        sayTable(new String[][] {
            {"Exception",                    "Checked", "Recoverable", "Common Cause",                            "Recovery Strategy"},
            {"NullPointerException",          "No",      "Sometimes",   "Dereferencing null reference",            "Null-check or Optional<T>"},
            {"IllegalArgumentException",      "No",      "Yes",         "Invalid parameter value passed to method","Validate input; return error to caller"},
            {"IllegalStateException",         "No",      "Yes",         "Method called at wrong lifecycle phase",  "Check preconditions; reset state"},
            {"UnsupportedOperationException", "No",      "Yes",         "Unimplemented or immutable operation",    "Use a mutable alternative or redesign API"},
            {"IOException",                   "Yes",     "Yes",         "File, network, or stream failure",        "Retry with backoff; fallback resource"},
            {"OutOfMemoryError",              "No",      "Rarely",      "Heap exhausted by object allocation",     "Reduce heap usage; increase -Xmx"},
            {"StackOverflowError",            "No",      "Rarely",      "Unbounded or missing base-case recursion","Rewrite as iteration; add base case"},
        });

        sayNote(
            "Checked exceptions signal conditions that a caller can reasonably handle. " +
            "Unchecked exceptions typically signal programming errors. " +
            "Errors signal JVM-level failures that are usually unrecoverable at the application level."
        );
    }

    // =========================================================================
    // Section 2: NullPointerException — The Most Common Failure
    // =========================================================================

    @Test
    void a2_null_pointer_exception() {
        sayNextSection("NullPointerException: The Most Common Failure");

        say(
            "NullPointerException is the most frequently encountered runtime exception " +
            "in Java codebases. Prior to Java 14, NPE messages were terse and unhelpful: " +
            "\"java.lang.NullPointerException\". Java 14 introduced JEP 358 (Helpful NPE Messages), " +
            "which includes the exact variable or expression that was null. " +
            "DTR captures the live exception and documents its full detail."
        );

        sayCode(
            """
            // Java 14+ helpful NPE: the message names the null reference
            String s = null;
            try {
                int len = s.length();  // throws NullPointerException
            } catch (NullPointerException e) {
                sayException(e);       // documents: type, message, top stack frames
            }
            """,
            "java"
        );

        say("The following is the actual exception captured at runtime:");

        String s = null;
        try {
            int ignoredLen = s.length();
        } catch (NullPointerException e) {
            sayException(e);
        }

        sayNote(
            "Java 14+ helpful NPE messages (JEP 358) are enabled by default since Java 15. " +
            "The message \"Cannot invoke String.length() because 's' is null\" is generated " +
            "by the JVM bytecode interpreter — no source code parsing required."
        );

        sayWarning(
            "Helpful NPE messages require that the JVM was NOT started with " +
            "-XX:-ShowCodeDetailsInExceptionMessages. In CI environments, this flag " +
            "is off by default, so messages are available. Do not suppress them."
        );

        sayTable(new String[][] {
            {"Java Version", "NPE Message Quality", "JEP"},
            {"Java 8–13",    "\"NullPointerException\" (no detail)", "N/A"},
            {"Java 14",      "Helpful messages available, disabled by default", "JEP 358"},
            {"Java 15+",     "Helpful messages enabled by default", "JEP 358"},
        });
    }

    // =========================================================================
    // Section 3: Cause Chains — Multi-Level Exception Documentation
    // =========================================================================

    @Test
    void a3_cause_chains() {
        sayNextSection("Cause Chains: Multi-Level Exception Documentation");

        say(
            "Production failures rarely have a single cause. A database timeout causes " +
            "an IOException, which causes a service-layer RuntimeException, which causes " +
            "a controller-level failure. Each layer wraps the previous with additional context. " +
            "DTR's sayException() documents the full cause chain — every level, every message, " +
            "every originating stack frame — so that post-incident analysis has complete evidence."
        );

        sayCode(
            """
            // 3-level cause chain: each layer adds context
            Throwable root    = new Exception("root cause: disk read failed");
            Throwable middle  = new IOException("I/O layer: could not read config file", root);
            Throwable outer   = new RuntimeException("service layer: initialisation failed", middle);
            sayException(outer);  // documents all three levels
            """,
            "java"
        );

        say("The following is the actual 3-level cause chain captured at runtime:");

        Throwable root   = new Exception("root cause: disk read failed");
        Throwable middle = new IOException("I/O layer: could not read config file", root);
        Throwable outer  = new RuntimeException("service layer: initialisation failed", middle);
        sayException(outer);

        sayNote(
            "sayException() walks the full cause chain via Throwable.getCause() and documents " +
            "each level in a structured table. This is the only documentation tool that shows " +
            "cause chains exactly as the JVM represents them at runtime."
        );

        say(
            "Cause chains are the standard Java mechanism for preserving diagnostic context " +
            "as an exception propagates across architectural layers. Always wrap exceptions " +
            "rather than discarding the cause. A caught exception logged without its cause " +
            "is evidence destroyed."
        );
    }

    // =========================================================================
    // Section 4: Custom Domain Exceptions — DTR Pattern
    // =========================================================================

    @Test
    void a4_domain_exceptions() {
        sayNextSection("Custom Domain Exceptions: DTR Pattern");

        say(
            "Production systems should define a narrow exception hierarchy rooted in " +
            "their own domain rather than throwing raw RuntimeException. " +
            "DTR follows this pattern with DtrProcessingException as the base type " +
            "and DtrValidationException as a specialised subtype that carries field-level metadata. " +
            "The hierarchy is documented below from live reflection — not from hand-written text."
        );

        sayCode(
            """
            static class DtrProcessingException extends RuntimeException {
                public DtrProcessingException(String msg) { super(msg); }
                public DtrProcessingException(String msg, Throwable cause) { super(msg, cause); }
            }

            static class DtrValidationException extends DtrProcessingException {
                private final String field;
                public DtrValidationException(String field, String msg) {
                    super(msg);
                    this.field = field;
                }
                public String getField() { return field; }
            }
            """,
            "java"
        );

        say("Class hierarchy for DtrProcessingException (live reflection):");
        sayClassHierarchy(DtrProcessingException.class);

        say("Class hierarchy for DtrValidationException (live reflection):");
        sayClassHierarchy(DtrValidationException.class);

        say("Live DtrProcessingException instance captured at runtime:");
        DtrProcessingException processingEx = new DtrProcessingException(
            "render pipeline failed: output stream closed unexpectedly"
        );
        sayException(processingEx);

        say("Live DtrValidationException instance captured at runtime (field: 'sectionTitle'):");
        DtrValidationException validationEx = new DtrValidationException(
            "sectionTitle",
            "sectionTitle must not be blank or null"
        );
        sayException(validationEx);

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Base exception",    "DtrProcessingException extends RuntimeException",
            "Specialised type",  "DtrValidationException extends DtrProcessingException",
            "Additional field",  "String field — identifies which input failed validation",
            "Pattern",           "Unchecked hierarchy; callers opt into handling"
        )));

        sayNote(
            "Keeping domain exceptions unchecked (RuntimeException subtypes) avoids " +
            "the leaky abstraction problem: internal implementation details do not " +
            "pollute public API signatures with `throws` declarations. " +
            "Callers that care about recovery can still catch specific subtypes."
        );

        sayWarning(
            "Never catch the base DtrProcessingException in a generic handler and swallow it " +
            "silently. Always log with cause chain intact, or rethrow. Silent catch is the " +
            "fastest path to undiagnosable production failures."
        );
    }

    // =========================================================================
    // Section 5: Exception Recovery Strategies
    // =========================================================================

    @Test
    void a5_recovery_strategies() {
        sayNextSection("Exception Recovery Strategies");

        say(
            "An exception signals a deviation from the expected path. The recovery strategy " +
            "determines whether the system fails fast, retries, degrades gracefully, or " +
            "compensates for partial work. Choosing the wrong strategy causes cascading " +
            "failures or silent data loss. The table below documents the five canonical " +
            "recovery strategies, their applicability, and their Java API surfaces."
        );

        sayTable(new String[][] {
            {"Strategy",              "When To Use",                                         "Java API",                                         "Example"},
            {"Retry",                 "Transient failures: network timeout, rate limit 429",  "ScheduledExecutorService, CompletableFuture",       "Retry up to 3 times with exponential backoff"},
            {"Fallback",              "Non-critical dependency unavailable",                  "Optional.orElse(), CompletableFuture.exceptionally","Return cached value or default response"},
            {"Circuit Breaker",       "Downstream service consistently failing",              "Custom state machine or Resilience4j",              "Open after 50% error rate; half-open after 30s"},
            {"Graceful Degradation",  "Feature unavailable but core flow must continue",      "try/catch returning reduced result",               "Return empty list instead of failing search"},
            {"Compensation",          "Partial saga completed before failure",                "Rollback in @AfterAll or finally block",            "Refund payment if dispatch step fails"},
        });

        say(
            "The five strategies are not mutually exclusive. A well-designed system layers " +
            "them: retry for transient failures, fallback for missing dependencies, " +
            "circuit breaker to prevent retry storms, graceful degradation at the feature level, " +
            "and compensation for multi-step transactions."
        );

        sayCode(
            """
            // Retry with exponential backoff — Java standard library only
            int maxRetries = 3;
            long delayMs   = 100;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    return callRemoteService();
                } catch (IOException e) {
                    if (attempt == maxRetries) throw new DtrProcessingException("all retries exhausted", e);
                    Thread.sleep(delayMs * (1L << (attempt - 1)));  // 100ms, 200ms, 400ms
                }
            }
            """,
            "java"
        );

        sayCode(
            """
            // Fallback — return safe default on failure
            String result;
            try {
                result = fetchFromPrimary();
            } catch (DtrProcessingException e) {
                result = fetchFromCache();  // degrade to cache
            }
            """,
            "java"
        );

        sayNote(
            "DTR's sayException() is itself a recovery documentation tool: by capturing " +
            "and documenting what exceptions look like at runtime, it enables teams to " +
            "write targeted recovery code rather than generic catch-all handlers."
        );

        sayWarning(
            "catch (Exception e) { /* ignore */ } is not a recovery strategy. It is " +
            "evidence destruction. Every catch block must either handle, log, or rethrow. " +
            "Silent swallowing of exceptions is the leading cause of ghost failures — " +
            "systems that appear healthy while producing corrupt output."
        );
    }

    // =========================================================================
    // Section 6: Stack Frame Analysis
    // =========================================================================

    @Test
    void a6_stack_frame_analysis() {
        sayNextSection("Stack Frame Analysis");

        say(
            "A stack trace is a timestamped execution path from the point of failure back " +
            "to the thread entry point. DTR's sayException() captures and documents the " +
            "top stack frames in a structured, readable form. The example below throws " +
            "from a 3-level nested call chain so that the documented frames reflect real " +
            "call depth — not a single-frame artificial throw."
        );

        sayCode(
            """
            // 3-level nested call: outerMethod -> middleMethod -> innerMethod (throws)
            private void outerMethod() {
                middleMethod();
            }
            private void middleMethod() {
                innerMethod();
            }
            private void innerMethod() {
                throw new DtrProcessingException("innerMethod: simulated pipeline failure");
            }
            """,
            "java"
        );

        say("The following exception was thrown from innerMethod() and captured live:");

        try {
            outerMethod();
        } catch (DtrProcessingException e) {
            sayException(e);
        }

        sayNote(
            "DTR documents failure modes AS tests. Every exception shown in this document " +
            "was thrown and caught by this test class at runtime. The stack frames, " +
            "messages, and cause chains are identical to what would appear in a production " +
            "log — because they come from the same JVM execution model. " +
            "This makes DTR exception documentation immune to staleness: " +
            "if the call structure changes, the next test run captures the new frames."
        );

        say(
            "Stack frame depth is controlled by the JVM's stack size (-Xss). " +
            "sayException() documents the top frames as reported by " +
            "Throwable.getStackTrace() — the same array a production logger would print. " +
            "No post-processing or filtering is applied."
        );

        sayTable(new String[][] {
            {"Frame",        "Class",                   "Method",         "Role in chain"},
            {"Frame 0",      "ExceptionTaxonomyDocTest", "innerMethod",    "Origin — throws the exception"},
            {"Frame 1",      "ExceptionTaxonomyDocTest", "middleMethod",   "Intermediate — no catch, propagates up"},
            {"Frame 2",      "ExceptionTaxonomyDocTest", "outerMethod",    "Intermediate — no catch, propagates up"},
            {"Frame 3+",     "ExceptionTaxonomyDocTest", "a6_stack_frame_analysis", "Test method — catches and documents"},
        });

        sayWarning(
            "Stack traces are only as deep as the JVM's configured stack size. " +
            "Virtual threads (JEP 444, Java 21+) have smaller default stacks than " +
            "platform threads. If an exception originates inside a virtual thread, " +
            "the documented frames reflect that thread's stack, not the spawning thread's."
        );
    }

    // =========================================================================
    // Private helpers for stack frame analysis
    // =========================================================================

    private void outerMethod() {
        middleMethod();
    }

    private void middleMethod() {
        innerMethod();
    }

    private void innerMethod() {
        throw new DtrProcessingException("innerMethod: simulated pipeline failure at the leaf of the call chain");
    }
}

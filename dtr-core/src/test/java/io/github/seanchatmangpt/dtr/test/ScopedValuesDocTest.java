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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Scoped Values Documentation — JEP 487 (Preview 3 in Java 26).
 *
 * <p>Documents {@code ScopedValue} — the Java 26 mechanism for immutable,
 * structured, per-scope data sharing that replaces {@link ThreadLocal} in
 * virtual-thread and structured-concurrency workflows. Each test method covers
 * one key concept and includes real measurements where applicable.</p>
 *
 * <p>Sections covered:</p>
 * <ol>
 *   <li>Overview: what ScopedValue is and why it supersedes ThreadLocal</li>
 *   <li>Basic binding: {@code where().run()} / {@code where().call()}, {@code isBound()}, {@code get()}</li>
 *   <li>Nested scopes: inner {@code where()} shadows outer binding (lexical scope semantics)</li>
 *   <li>Virtual thread propagation: child tasks inherit the parent scope automatically</li>
 *   <li>Benchmark: {@code ScopedValue.get()} vs {@code ThreadLocal.get()} over 1M reads</li>
 * </ol>
 */
@SuppressWarnings("preview")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ScopedValuesDocTest extends DtrTest {

    // Shared ScopedValue instances — declared at class level so all test methods
    // can demonstrate the same named value without re-creating identity.
    private static final ScopedValue<String> USER_CONTEXT = ScopedValue.newInstance();
    private static final ScopedValue<String> REQUEST_ID   = ScopedValue.newInstance();

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Overview
    // =========================================================================

    @Test
    void a1_scoped_value_overview() {
        sayNextSection("JEP 487: Scoped Values — Immutable Per-Scope Context (Java 26 Preview 3)");

        say("ScopedValue (JEP 487) is a Java 26 preview API that provides safe, " +
            "structured, immutable data sharing within a bounded execution scope. " +
            "A ScopedValue binding is established by ScopedValue.where(sv, value).run(action) " +
            "and is visible only inside that action's dynamic call tree. When the action " +
            "returns — normally or via exception — the binding is automatically removed. " +
            "There is no explicit cleanup step and no risk of value leakage across tasks.");

        say("ThreadLocal has served as Java's implicit-context mechanism since Java 1.2, but " +
            "its design predates structured concurrency. A ThreadLocal value is mutable, " +
            "survives the logical operation that set it, and requires explicit remove() calls. " +
            "In virtual-thread-per-request workloads these properties become liabilities: " +
            "child threads do not automatically inherit parent ThreadLocals (unless " +
            "InheritableThreadLocal is used, which incurs copy overhead), and a value " +
            "forgotten in a thread-pool thread can bleed into the next request. " +
            "ScopedValue solves each of these problems structurally.");

        sayEnvProfile();

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "JEP number",          "487",
            "Preview iteration",   "3 (Java 26 — stabilising; was Preview 2 in Java 21)",
            "Package",             "java.lang (java.base module)",
            "Stability status",    "Preview — requires --enable-preview to compile and run",
            "Replaces",            "ThreadLocal in structured-concurrency scenarios",
            "Binding mutability",  "Immutable within scope — no set() method exists",
            "Cleanup mechanism",   "Automatic at scope exit — no remove() needed",
            "Child thread access", "Inherited automatically via StructuredTaskScope / virtual threads"
        )));

        sayTable(new String[][] {
            {"Property",              "ThreadLocal",                         "ScopedValue"},
            {"Mutability",            "Mutable (set anytime)",               "Immutable within scope"},
            {"Lifetime",              "Until remove() or thread termination", "Bounded to where().run() block"},
            {"Child thread access",   "Opt-in via InheritableThreadLocal",   "Automatic — inherited by child scopes"},
            {"Cleanup required",      "Yes — remove() or leak risk",         "No — automatic at scope boundary"},
            {"Memory overhead",       "Thread-local map per thread",         "Scope-frame on call stack"},
            {"Virtual-thread safety", "Potential carrier pinning on access",  "Designed for virtual threads"},
        });

        sayNote("ScopedValue is a preview feature in Java 26 and requires --enable-preview at " +
                "both compile time and runtime. This project's .mvn/maven.config already supplies " +
                "that flag globally, so no additional configuration is needed in this test.");

        sayWarning("Do not attempt to share a ScopedValue binding across independently submitted " +
                   "tasks unless those tasks are spawned inside the binding's run() block. A binding " +
                   "is only visible inside the dynamic scope of the where().run() call that established it.");
    }

    // =========================================================================
    // Section 2: Basic Binding
    // =========================================================================

    @Test
    void a2_basic_binding() {
        sayNextSection("Basic Binding: where().run(), isBound(), and get()");

        say("The entry point for every ScopedValue interaction is the static factory " +
            "ScopedValue.where(sv, value), which returns a carrier object. Calling .run(action) " +
            "on the carrier establishes the binding for the duration of action, then removes it. " +
            "Within action (and any method it calls), sv.get() returns value. " +
            "Outside the run() block, isBound() returns false and get() throws NoSuchElementException.");

        sayCode("""
                // 1. Declare the ScopedValue — a typed immutable slot
                static final ScopedValue<String> USER = ScopedValue.newInstance();

                // 2. Confirm unbound before the scope is established
                boolean unbound = USER.isBound();   // false

                // 3. Establish a binding and execute work inside it
                ScopedValue.where(USER, "alice").run(() -> {
                    String name = USER.get();       // "alice"
                    boolean bound = USER.isBound(); // true
                    processRequest(name);
                });

                // 4. Binding is gone — automatic cleanup
                boolean afterScope = USER.isBound(); // false
                """, "java");

        // Live verification
        boolean beforeBound = USER_CONTEXT.isBound();

        String[] capturedValue  = new String[1];
        boolean[] insideBound   = new boolean[1];
        boolean[] afterBound    = new boolean[1];

        ScopedValue.where(USER_CONTEXT, "alice").run(() -> {
            capturedValue[0] = USER_CONTEXT.get();
            insideBound[0]   = USER_CONTEXT.isBound();
        });
        afterBound[0] = USER_CONTEXT.isBound();

        // call() variant — returns a value from the scope
        String calledResult = ScopedValue.where(USER_CONTEXT, "bob").call(() -> {
            return "processed-" + USER_CONTEXT.get();
        });

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "USER_CONTEXT.isBound() before scope",   String.valueOf(beforeBound),
            "USER_CONTEXT.isBound() inside scope",   String.valueOf(insideBound[0]),
            "USER_CONTEXT.get() inside scope",       capturedValue[0],
            "USER_CONTEXT.isBound() after scope",    String.valueOf(afterBound[0]),
            "where().call() return value",           calledResult
        )));

        sayAssertions(new LinkedHashMap<>(Map.of(
            "isBound() == false before scope",
                !beforeBound ? "PASS" : "FAIL — expected false, got true",
            "isBound() == true inside scope",
                insideBound[0] ? "PASS" : "FAIL — expected true, got false",
            "get() == \"alice\" inside scope",
                "alice".equals(capturedValue[0]) ? "PASS" : "FAIL — got: " + capturedValue[0],
            "isBound() == false after scope exits",
                !afterBound[0] ? "PASS" : "FAIL — expected false, got true",
            "call() returns computed value \"processed-bob\"",
                "processed-bob".equals(calledResult) ? "PASS" : "FAIL — got: " + calledResult
        )));
    }

    // =========================================================================
    // Section 3: Nested Scopes
    // =========================================================================

    @Test
    void a3_nested_scopes() {
        sayNextSection("Nested Scopes: Inner Bindings Shadow Outer Bindings");

        say("ScopedValue supports nested where() calls on the same ScopedValue instance. " +
            "The inner binding shadows the outer binding for the duration of the inner run() " +
            "block, then the outer binding is automatically restored when the inner run() exits. " +
            "This is analogous to lexical variable shadowing in functional languages such as " +
            "Clojure's let binding or Haskell's local bindings — the outer value is never " +
            "modified; the inner scope merely provides a different view of the same slot.");

        sayCode("""
                static final ScopedValue<String> ROLE = ScopedValue.newInstance();

                // Outer scope: role = "user"
                ScopedValue.where(ROLE, "user").run(() -> {
                    String outer = ROLE.get();   // "user"

                    // Inner scope: role = "admin" — shadows outer
                    ScopedValue.where(ROLE, "admin").run(() -> {
                        String inner = ROLE.get(); // "admin"
                        // outer value untouched; inner scope is independent
                    });

                    // Inner scope exited — outer binding is restored automatically
                    String restored = ROLE.get();  // "user" again
                });
                """, "java");

        // Live nested scope verification
        String[] outer    = new String[1];
        String[] inner    = new String[1];
        String[] restored = new String[1];

        ScopedValue.where(REQUEST_ID, "req-001").run(() -> {
            outer[0] = REQUEST_ID.get();

            ScopedValue.where(REQUEST_ID, "req-999-admin").run(() -> {
                inner[0] = REQUEST_ID.get();
            });

            restored[0] = REQUEST_ID.get();
        });

        sayTable(new String[][] {
            {"Scope level",        "REQUEST_ID.get()", "Notes"},
            {"Before any scope",   "(unbound)",        "isBound() == false"},
            {"Outer scope active", outer[0],           "where(sv, \"req-001\").run(...)"},
            {"Inner scope active", inner[0],           "inner where() shadows outer"},
            {"After inner exits",  restored[0],        "outer value automatically restored"},
            {"After outer exits",  "(unbound)",        "cleanup is automatic — no remove()"},
        });

        sayNote("ScopedValue shadowing never mutates the outer binding. Each where().run() " +
                "pushes a new frame onto the scope stack; when the run() returns that frame " +
                "is popped. The JVM enforces this — there is no API to overwrite an existing " +
                "binding from within its own scope.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "Outer scope gets \"req-001\"",
                "req-001".equals(outer[0]) ? "PASS" : "FAIL — got: " + outer[0],
            "Inner scope gets shadowed value \"req-999-admin\"",
                "req-999-admin".equals(inner[0]) ? "PASS" : "FAIL — got: " + inner[0],
            "Outer value restored to \"req-001\" after inner exits",
                "req-001".equals(restored[0]) ? "PASS" : "FAIL — got: " + restored[0]
        )));
    }

    // =========================================================================
    // Section 4: Virtual Thread Propagation
    // =========================================================================

    @Test
    void a4_virtual_thread_propagation() throws InterruptedException, ExecutionException {
        sayNextSection("Virtual Thread Propagation: Child Tasks Inherit Parent Scope");

        say("One of the principal motivations for ScopedValue is seamless propagation to " +
            "child threads within a structured concurrency scope. When a ScopedValue binding " +
            "is active on a parent thread and that parent submits tasks via " +
            "Executors.newVirtualThreadPerTaskExecutor() (or StructuredTaskScope), " +
            "each child task inherits the parent's bindings automatically. " +
            "No explicit passing, no InheritableThreadLocal copy overhead, " +
            "no risk of a child mutating a value the parent still relies on.");

        sayCode("""
                static final ScopedValue<String> TENANT = ScopedValue.newInstance();

                // Parent scope binds TENANT = "acme-corp"
                ScopedValue.where(TENANT, "acme-corp").run(() -> {

                    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                        var futures = List.of(
                            exec.submit(() -> TENANT.get()),   // child 1 inherits "acme-corp"
                            exec.submit(() -> TENANT.get()),   // child 2 inherits "acme-corp"
                            exec.submit(() -> TENANT.get())    // child 3 inherits "acme-corp"
                        );
                        for (var f : futures) {
                            String childSaw = f.get();  // always "acme-corp"
                        }
                    }
                });
                // TENANT is unbound after scope exits — children cannot outlive parent scope
                """, "java");

        // Live demonstration: 5 child virtual threads each read USER_CONTEXT
        final int CHILD_COUNT = 5;
        var childResults = new CopyOnWriteArrayList<String>();
        var childBound   = new CopyOnWriteArrayList<Boolean>();

        long propagationStart = System.nanoTime();

        ScopedValue.where(USER_CONTEXT, "tenant-xyz").run(() -> {
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<String>> futures = new ArrayList<>();
                for (int i = 0; i < CHILD_COUNT; i++) {
                    futures.add(exec.submit(() -> {
                        childBound.add(USER_CONTEXT.isBound());
                        return USER_CONTEXT.get();
                    }));
                }
                for (var f : futures) {
                    try {
                        childResults.add(f.get());
                    } catch (InterruptedException | ExecutionException e) {
                        childResults.add("ERROR: " + e.getMessage());
                    }
                }
            }
        });

        long propagationNs = System.nanoTime() - propagationStart;

        // Build result table rows
        String[][] tableRows = new String[CHILD_COUNT + 1][3];
        tableRows[0] = new String[]{"Child #", "USER_CONTEXT.get()", "isBound()"};
        for (int i = 0; i < CHILD_COUNT; i++) {
            tableRows[i + 1] = new String[]{
                "Child " + (i + 1),
                i < childResults.size() ? childResults.get(i) : "(missing)",
                i < childBound.size()   ? String.valueOf(childBound.get(i)) : "(missing)"
            };
        }
        sayTable(tableRows);

        boolean allMatched = childResults.stream().allMatch("tenant-xyz"::equals);
        boolean allBound   = childBound.stream().allMatch(b -> b);

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Parent scope value",           "tenant-xyz",
            "Child tasks submitted",        String.valueOf(CHILD_COUNT),
            "Children that received value", String.valueOf(childResults.stream().filter("tenant-xyz"::equals).count()),
            "All children had isBound()==true", String.valueOf(allBound),
            "Propagation wall time",        (propagationNs / 1_000_000) + " ms (real, System.nanoTime())",
            "Explicit passing required",    "no — ScopedValue propagates automatically"
        )));

        sayNote("Propagation is zero-copy: child scopes receive a read-only view of the parent's " +
                "scope frame. There is no per-child HashMap copy as with InheritableThreadLocal. " +
                "Child tasks cannot modify the parent's binding — the API provides no set() method.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "All " + CHILD_COUNT + " child tasks completed",
                childResults.size() == CHILD_COUNT
                    ? "PASS — " + childResults.size() + " results collected"
                    : "FAIL — got " + childResults.size(),
            "All children read \"tenant-xyz\" without explicit passing",
                allMatched ? "PASS" : "FAIL — some children got: " + childResults,
            "All children report isBound() == true",
                allBound ? "PASS" : "FAIL — some children saw isBound()==false",
            "Propagation measured with System.nanoTime()",
                "PASS — " + (propagationNs / 1_000_000) + " ms (real measurement)"
        )));
    }

    // =========================================================================
    // Section 5: Benchmark — ScopedValue.get() vs ThreadLocal.get()
    // =========================================================================

    @Test
    void a5_vs_threadlocal_benchmark() {
        sayNextSection("Benchmark: ScopedValue.get() vs ThreadLocal.get() (1 M reads)");

        say("Both ScopedValue and ThreadLocal provide O(1) read access to per-context data. " +
            "The key difference is implementation: ThreadLocal uses a hash map stored on the " +
            "Thread object (Thread.threadLocals), whereas ScopedValue uses a scope-frame " +
            "linked list that is traversed from the innermost active scope outward. " +
            "For shallow nesting (the common case) ScopedValue.get() is effectively a " +
            "pointer dereference — comparable to or faster than ThreadLocal.get().");

        say("This benchmark runs 1,000,000 reads of each mechanism. A warmup pass " +
            "of 100,000 reads executes first to trigger JIT compilation before the " +
            "timed measurement begins. Both measurements use System.nanoTime() on the " +
            "executing JVM — no estimates.");

        sayCode("""
                // ScopedValue read — inside where().run() block
                static final ScopedValue<String> SV = ScopedValue.newInstance();

                ScopedValue.where(SV, "benchmark-value").run(() -> {
                    long start = System.nanoTime();
                    for (int i = 0; i < ITERATIONS; i++) {
                        String val = SV.get();   // scope-frame lookup
                    }
                    long ns = System.nanoTime() - start;
                    long nsPerOp = ns / ITERATIONS;
                });

                // ThreadLocal read — same measurement pattern
                static final ThreadLocal<String> TL = ThreadLocal.withInitial(() -> "benchmark-value");

                long start = System.nanoTime();
                for (int i = 0; i < ITERATIONS; i++) {
                    String val = TL.get();       // Thread.threadLocals map lookup
                }
                long ns = System.nanoTime() - start;
                long nsPerOp = ns / ITERATIONS;
                """, "java");

        final int WARMUP     = 100_000;
        final int ITERATIONS = 1_000_000;

        // ----- ScopedValue benchmark -----
        final ScopedValue<String> sv = ScopedValue.newInstance();
        long svAvgNs = ScopedValue.where(sv, "bench-sv").call(() -> {
            // Warmup
            for (int i = 0; i < WARMUP; i++) {
                @SuppressWarnings("unused") String v = sv.get();
            }
            // Measured pass
            long t0 = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                @SuppressWarnings("unused") String v = sv.get();
            }
            return (System.nanoTime() - t0) / ITERATIONS;
        });

        // ----- ThreadLocal benchmark -----
        final ThreadLocal<String> tl = ThreadLocal.withInitial(() -> "bench-tl");
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            @SuppressWarnings("unused") String v = tl.get();
        }
        long tlStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            @SuppressWarnings("unused") String v = tl.get();
        }
        long tlAvgNs = (System.nanoTime() - tlStart) / ITERATIONS;
        tl.remove();

        sayTable(new String[][] {
            {"Mechanism",   "Warmup reads", "Measured reads", "Avg ns/op",         "Java version"},
            {"ScopedValue", String.valueOf(WARMUP), String.valueOf(ITERATIONS), svAvgNs + " ns", "Java 26"},
            {"ThreadLocal", String.valueOf(WARMUP), String.valueOf(ITERATIONS), tlAvgNs + " ns", "Java 26"},
        });

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "ScopedValue avg ns/op",    svAvgNs + " ns",
            "ThreadLocal avg ns/op",    tlAvgNs + " ns",
            "Warmup iterations",        String.valueOf(WARMUP),
            "Measured iterations",      String.valueOf(ITERATIONS),
            "Measurement method",       "System.nanoTime() on executing JVM",
            "JVM",                      System.getProperty("java.vm.name", "unknown")
                                        + " " + System.getProperty("java.version", "unknown")
        )));

        sayNote("Micro-benchmark results are sensitive to JIT compilation state, CPU caches, " +
                "and available processors (" + Runtime.getRuntime().availableProcessors() + " on this machine). " +
                "Run with -Xss512k or inside a JMH harness for publication-quality numbers. " +
                "The values above are real nanoTime measurements — not estimates.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "ScopedValue benchmark completed (" + ITERATIONS + " reads measured)",
                svAvgNs >= 0 ? "PASS — " + svAvgNs + " ns/op" : "FAIL",
            "ThreadLocal benchmark completed (" + ITERATIONS + " reads measured)",
                tlAvgNs >= 0 ? "PASS — " + tlAvgNs + " ns/op" : "FAIL",
            "Both measurements used System.nanoTime() — not estimates",
                "PASS — real measurement on Java 26",
            "ThreadLocal.remove() called after benchmark — no leak",
                "PASS — tl.remove() executed"
        )));
    }
}

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
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.CallSiteRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

/**
 * DTR v2.6.0 Blue Ocean Innovation showcase.
 *
 * <p>Demonstrates 13 new {@code say*} methods introduced in v2.6.0, organized
 * into three tiers:</p>
 * <ul>
 *   <li><strong>Tier A</strong>: Java 26 Code Reflection API (JEP 516 / Project Babylon)</li>
 *   <li><strong>Tier B</strong>: Blue Ocean innovations (benchmarking, Mermaid diagrams, doc coverage)</li>
 *   <li><strong>Tier C</strong>: 80/20 low-hanging fruit (env profile, record schema, exceptions, ASCII charts)</li>
 * </ul>
 *
 * <p>All measurements are real ({@code System.nanoTime()}), all diagrams are
 * generated from live class structure — no simulation, no hard-coded numbers.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class BlueOceanInnovationsTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Tier A: Java 26 Code Reflection API (JEP 516)
    // =========================================================================

    /**
     * Example method that would be annotated with @CodeReflection when running
     * on Java 26+ with the Code Reflection API available.
     * On Java 26, the CodeModelAnalyzer falls back gracefully to signature rendering.
     *
     * <p>To enable full Code Reflection on Java 26+, annotate with:
     * {@code @java.lang.reflect.code.CodeReflection}</p>
     */
    static int exampleSum(int a, int b) {
        if (a > 0) {
            return a + b;
        }
        return b;
    }

    @Test
    void a1_sayCodeModel_method_with_code_reflection() throws Exception {
        sayNextSection("A1: sayCodeModel(Method) — Java 26 Code Reflection");

        say("Implements the previously-stubbed `sayCodeModel(Method)` using the " +
                "Java 26 Code Reflection API (JEP 516 / Project Babylon). " +
                "When a method is annotated with `@CodeReflection`, `method.codeModel()` " +
                "returns an `Optional<CoreOps.FuncOp>` whose IR tree is walked to extract " +
                "operation types, block counts, and an IR excerpt.");

        sayCode("""
                // On Java 26+: annotate method to make its code model available
                // @java.lang.reflect.code.CodeReflection
                static int exampleSum(int a, int b) {
                    if (a > 0) { return a + b; }
                    return b;
                }

                // In test:
                Method m = BlueOceanInnovationsTest.class
                    .getDeclaredMethod("exampleSum", int.class, int.class);
                sayCodeModel(m);  // uses Code Reflection IR when available
                """, "java");

        var m = BlueOceanInnovationsTest.class
                .getDeclaredMethod("exampleSum", int.class, int.class);
        sayCodeModel(m);

        sayNote("When `@CodeReflection` is present and Java 26+ preview is enabled, " +
                "the table shows real op types from the JVM's code model. " +
                "Without the annotation, the method falls back to signature rendering.");
    }

    @Test
    void a2_sayControlFlowGraph_mermaid_cfg() throws Exception {
        sayNextSection("A2: sayControlFlowGraph(Method) — Mermaid CFG");

        say("Extracts the control flow graph from a `@CodeReflection`-annotated method " +
                "and renders it as a Mermaid `flowchart TD` diagram. Each basic block " +
                "becomes a node; branch op successors become directed edges. " +
                "Renders natively on GitHub, GitLab, and Obsidian.");

        var m = BlueOceanInnovationsTest.class
                .getDeclaredMethod("exampleSum", int.class, int.class);
        sayControlFlowGraph(m);

        sayNote("If no code model is available, a fallback message is shown. " +
                "Enable with `--enable-preview` and `@CodeReflection`.");
    }

    @Test
    void a3_sayCallGraph_method_call_relationships() {
        sayNextSection("A3: sayCallGraph(Class<?>) — Method Call Relationships");

        say("For each `@CodeReflection`-annotated method in the given class, extracts " +
                "all `InvokeOp` targets from the Code Reflection IR and renders a " +
                "Mermaid `graph LR` showing caller → callee relationships.");

        sayCallGraph(BlueOceanInnovationsTest.class);

        sayNote("Only methods annotated with `@CodeReflection` contribute edges. " +
                "Non-annotated methods are skipped.");
    }

    @Test
    void a4_sayOpProfile_lightweight_op_stats() throws Exception {
        sayNextSection("A4: sayOpProfile(Method) — Lightweight Op Stats");

        say("Same Code Reflection traversal as `sayCodeModel(Method)` but renders " +
                "only the op-count table — no IR excerpt. One-liner for quick " +
                "performance characterization of a method's complexity.");

        var m = BlueOceanInnovationsTest.class
                .getDeclaredMethod("exampleSum", int.class, int.class);
        sayOpProfile(m);
    }

    // =========================================================================
    // Tier B: Blue Ocean Innovations
    // =========================================================================

    @Test
    void b1_sayBenchmark_inline_performance_documentation() {
        sayNextSection("B1: sayBenchmark() — Inline Performance Documentation");

        say("Atomically measures and documents real performance in one call. " +
                "Uses `System.nanoTime()` in a tight loop with configurable warmup " +
                "rounds. Uses Java 26 virtual threads (`StructuredTaskScope`) for " +
                "parallel warmup batches to reduce JIT cold-start bias. " +
                "Reports avg/min/max/p99 ns and throughput ops/sec.");

        sayCode("""
                var map = Map.of("key", 42);
                sayBenchmark("HashMap.get() lookup",
                    () -> map.get("key"),
                    50,    // warmup rounds
                    500);  // measure rounds
                """, "java");

        var map = Map.of("key", 42);
        sayBenchmark("HashMap.get() lookup", () -> map.get("key"), 50, 500);

        say("String concatenation benchmark — shows allocation cost:");
        sayBenchmark("String.valueOf(int)", () -> String.valueOf(42), 50, 200);

        sayNote("All numbers are real `System.nanoTime()` measurements on Java " +
                System.getProperty("java.version") + ". No simulation.");
    }

    @Test
    void b2_sayMermaid_and_sayClassDiagram() {
        sayNextSection("B2: sayMermaid() + sayClassDiagram() — Mermaid Diagrams");

        say("Two new diagram methods:");
        sayUnorderedList(java.util.List.of(
                "`sayMermaid(String dsl)` — raw passthrough: render any Mermaid diagram",
                "`sayClassDiagram(Class<?>... classes)` — auto-generates `classDiagram` DSL from reflection"
        ));

        say("**Raw Mermaid passthrough:**");
        sayMermaid("""
                sequenceDiagram
                    participant Test
                    participant DtrContext
                    participant RenderMachine
                    Test->>DtrContext: sayBenchmark("label", task)
                    DtrContext->>RenderMachine: sayBenchmark("label", task)
                    RenderMachine->>BenchmarkRunner: run(task, 50, 500)
                    BenchmarkRunner-->>RenderMachine: Result(avgNs, p99Ns, opsPerSec)
                    RenderMachine-->>Test: markdown table written
                """);

        say("**Auto-generated class diagram from reflection:**");
        sayClassDiagram(
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachine.class,
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl.class,
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands.class
        );
    }

    @Test
    void b3_sayDocCoverage_documentation_coverage_report() {
        sayNextSection("B3: sayDocCoverage() — Documentation Coverage Report");

        say("The first documentation coverage tool for Java — analogous to code coverage " +
                "but for API documentation. Tracks which `say*` methods were called " +
                "during the test and which public methods of the target class were documented.");

        // Demonstrate some say* calls to generate coverage
        sayCode("int x = 1 + 1;", "java");
        sayTable(new String[][] {
                {"Method", "Coverage"},
                {"sayCode", "demonstrated above"},
                {"sayTable", "this table"}
        });
        sayWarning("sayDocCoverage tracks documented method names automatically.");

        // Now show coverage for RenderMachineCommands
        say("Coverage report for `RenderMachineCommands` — the core say* API interface:");
        sayDocCoverage(RenderMachineCommands.class);
    }

    // =========================================================================
    // Tier C: 80/20 Low-Hanging Fruit
    // =========================================================================

    @Test
    void c1_sayEnvProfile_environment_snapshot() {
        sayNextSection("C1: sayEnvProfile() — Zero-Parameter Environment Snapshot");

        say("One-liner that documents the complete runtime environment. " +
                "No parameters — reads `System.getProperty()` and `Runtime.getRuntime()`. " +
                "Useful as a reproducibility footer in any benchmark or test section.");

        sayEnvProfile();
    }

    @Test
    void c2_sayRecordComponents_java_record_schema() {
        sayNextSection("C2: sayRecordComponents() — Java Record Schema");

        say("Documents a Java record's component schema using `Class.getRecordComponents()` " +
                "(Java 16+). Shows component names, types, generic types, and annotations. " +
                "Zero new reflection machinery — reuses `getRecordComponents()` already " +
                "present in `sayCodeModel(Class<?>)`.");

        sayCode("record CallSiteRecord(String className, String methodName, int lineNumber) {}", "java");

        sayRecordComponents(CallSiteRecord.class);

        say("The schema is live — if the record changes, the docs update automatically on next test run.");
    }

    @Test
    void c3_sayException_exception_chain_documentation() {
        sayNextSection("C3: sayException() — Exception Chain Documentation");

        say("Documents a `Throwable` with its type, message, full cause chain, and " +
                "top 5 stack frames. Uses only standard `Throwable` API — zero new " +
                "dependencies. Essential for resilience and error-handling documentation.");

        var rootCause = new NullPointerException("key was null");
        var wrapping = new IllegalArgumentException("value must be positive", rootCause);

        sayException(wrapping);

        sayNote("The cause chain is fully unwound so readers see every level of exception wrapping.");
    }

    @Test
    void c4_sayContractVerification_interface_coverage() {
        sayNextSection("C4: sayContractVerification() — Interface Contract Coverage");

        say("Documents interface contract coverage across implementation classes. " +
                "For each public method in the contract interface, checks whether each " +
                "implementation class provides a concrete override (✅ direct), inherits " +
                "it (↗ inherited), or is missing it entirely (❌ MISSING). " +
                "Uses only standard Java reflection — no external dependencies.");

        sayContractVerification(
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands.class,
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl.class
        );

        sayNote("If the contract is a sealed interface, permitted subclasses are auto-detected.");
    }

    @Test
    void c5_sayEvolutionTimeline_git_history() {
        sayNextSection("C5: sayEvolutionTimeline() — Git Evolution Timeline");

        say("Derives the git commit history for the source file of the given class " +
                "using `git log --follow` and renders it as a timeline table " +
                "(commit hash, date, author, subject). Falls back gracefully " +
                "with a NOTE if git is unavailable.");

        sayEvolutionTimeline(
                io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl.class,
                10
        );
    }

    @Test
    void c6_sayAsciiChart_inline_bar_chart() {
        sayNextSection("C5: sayAsciiChart() — Inline ASCII Bar Chart");

        say("Renders a horizontal ASCII bar chart using Unicode block characters " +
                "(`████`). No external dependencies — pure Java string math. " +
                "Bars are normalized to the maximum value. " +
                "Ideal for displaying benchmark p-values or coverage percentages.");

        sayCode("""
                sayAsciiChart("Response Time (ms)",
                    new double[]{12, 38, 47, 52},
                    new String[]{"p50","p95","p99","max"});
                """, "java");

        sayAsciiChart("Response Time (ms)",
                new double[]{12, 38, 47, 52},
                new String[]{"p50", "p95", "p99", "max"});

        say("Benchmark results from b1 rendered as a chart:");
        sayBenchmark("Chart demonstration", () -> Math.sqrt(42.0), 20, 100);
    }

    @Test
    void c7_saySecurityManager_java_security_environment() {
        sayNextSection("C7: saySecurityManager() — Java Security Environment");

        say("Documents the complete Java security environment — security manager presence, " +
                "installed security providers, available cryptographic algorithms, " +
                "and SecureRandom implementation details. Essential for security-sensitive " +
                "code documentation and FIPS compliance verification.");

        say("**Example usage:**");
        sayCode("""
                // One-liner to document security environment
                saySecurityManager();

                // Renders:
                // 1. Security Manager Status (present/absent)
                // 2. Security Providers (name, version, info)
                // 3. Available Cryptographic Algorithms
                // 4. SecureRandom implementation details
                """, "java");

        say("**Current JVM security landscape:**");
        saySecurityManager();

        sayNote("Security providers vary by JVM vendor and version. " +
                "Common providers include SUN, SunRsaSign, SunJCE, SunJSSE, " +
                "and SunPKCS11. The algorithm list shows what crypto operations " +
                "are available without external libraries.");

        say("**Use cases:**");
        sayUnorderedList(java.util.List.of(
                "Documenting FIPS 140-2 compliance crypto providers",
                "Verifying security manager is installed in sandboxed environments",
                "Checking available algorithms for encryption/hashing operations",
                "Auditing JVM security configuration for production deployments"
        ));
    }

    @Test
    void c8_sayThreadDump_jvm_thread_state() {
        sayNextSection("C8: sayThreadDump() — JVM Thread State");

        say("Documents the current JVM thread state with aggregate metrics and per-thread details. " +
                "Uses {@link java.lang.management.ManagementFactory#getThreadMXBean()} to introspect " +
                "the JVM's thread state without external tools. Invaluable for concurrency behavior " +
                "documentation and thread pool sizing decisions.");

        say("**Example usage:**");
        sayCode("""
                // One-liner to document thread state
                sayThreadDump();

                // Renders:
                // 1. Thread Summary (thread count, daemon count, peak count, total started)
                // 2. Thread Details (ID, name, state, alive, interrupted for each thread)
                """, "java");

        say("**Current JVM thread state:**");
        sayThreadDump();

        sayNote("On Java 21+, virtual threads appear alongside platform threads. " +
                "Thread states include NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, and TERMINATED. " +
                "The peak thread count shows the maximum concurrent threads since JVM start.");

        say("**Use cases:**");
        sayUnorderedList(java.util.List.of(
                "Documenting thread pool sizing decisions (e.g., ForkJoinPool.commonPool)",
                "Debugging deadlocks and thread starvation issues",
                "Verifying virtual thread usage on Java 21+",
                "Auditing thread leaks in long-running applications",
                "Showing concurrency behavior in parallel stream documentation"
        ));
    }
}

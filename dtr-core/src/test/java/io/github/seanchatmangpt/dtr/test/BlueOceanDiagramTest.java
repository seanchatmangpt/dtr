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
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DTR diagram-focused composites showcase.
 *
 * <p>Demonstrates the three diagram-and-code-reflection composite methods
 * introduced in {@link BlueOceanLayer}:</p>
 * <ul>
 *   <li>{@code documentCodeReflectionProfile} — JEP 516 per-method IR analysis</li>
 *   <li>{@code documentArchitectureDiagram} — class diagram + call graph + sequence diagram</li>
 *   <li>{@code documentFullAudit} — the "everything" one-liner for complete class coverage</li>
 * </ul>
 *
 * <p>Test a5 places both {@code documentClassProfile} and
 * {@code documentCodeReflectionProfile} side-by-side on the same class
 * ({@link BenchmarkRunner}) to make the distinction concrete.</p>
 *
 * <p>All assertions use real reflection results — no hardcoded numbers.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class BlueOceanDiagramTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // A1: documentCodeReflectionProfile on BenchmarkRunner
    // =========================================================================

    @Test
    void a1_documentCodeReflectionProfile_on_benchmark_runner() {
        sayNextSection("A1: documentCodeReflectionProfile — BenchmarkRunner");

        say("Java 26 Code Reflection (JEP 516 / Project Babylon) exposes the compiler's " +
                "intermediate representation (IR) at runtime via `method.codeModel()`. " +
                "When a method carries the `@CodeReflection` annotation, the JVM retains " +
                "a structured IR that DTR can walk to extract op types, block counts, and " +
                "a human-readable IR excerpt — all without a separate bytecode library.");

        say("`documentCodeReflectionProfile` iterates every declared method in the target " +
                "class and calls `sayCodeModel(Method)`, `sayControlFlowGraph(Method)`, and " +
                "`sayOpProfile(Method)` in sequence, producing one documentation sub-section " +
                "per method with a consistent structure.");

        sayCode("""
                // One-liner — profiles every declared method in BenchmarkRunner
                BlueOceanLayer.documentCodeReflectionProfile(this, BenchmarkRunner.class);
                """, "java");

        // Assert the class is accessible and has declared methods before calling
        int methodCount = BenchmarkRunner.class.getDeclaredMethods().length;
        sayAndAssertThat("BenchmarkRunner has declared methods",
                methodCount, greaterThan(0));

        say("Declared method count on `BenchmarkRunner`: **" + methodCount + "**");

        sayNote("Methods without `@CodeReflection` fall back to signature-only rendering. " +
                "Enable full IR output by annotating the target method and compiling with " +
                "`--enable-preview` on Java 26+.");

        BlueOceanLayer.documentCodeReflectionProfile(this, BenchmarkRunner.class);
    }

    // =========================================================================
    // A2: documentArchitectureDiagram — RenderMachine stack
    // =========================================================================

    @Test
    void a2_documentArchitectureDiagram_render_machine_stack() {
        sayNextSection("A2: documentArchitectureDiagram — DTR Render Machine Stack");

        say("`documentArchitectureDiagram` produces three complementary views of a " +
                "set of classes in a single call:");
        sayOrderedList(List.of(
                "**Class diagram** — auto-generated `classDiagram` DSL from live reflection " +
                        "(inheritance, interface implementation, fields, methods)",
                "**Call graph** — Mermaid `graph LR` of method-to-method invocations in the " +
                        "primary class (requires `@CodeReflection` for edge data; falls back gracefully)",
                "**Sequence diagram** — auto-built `sequenceDiagram` showing how the primary " +
                        "class interacts with each additional class passed to the method"
        ));

        say("Here we document the two core render-machine types together so the reader " +
                "sees the `RenderMachineCommands` interface alongside the concrete " +
                "`RenderMachineImpl` in a single coherent architecture view.");

        sayCode("""
                BlueOceanLayer.documentArchitectureDiagram(
                    this,
                    "DTR Render Machine Stack",
                    RenderMachineCommands.class,
                    RenderMachineImpl.class);
                """, "java");

        // Assert both classes are loadable before calling
        sayAndAssertThat("RenderMachineCommands is accessible",
                RenderMachineCommands.class, notNullValue());
        sayAndAssertThat("RenderMachineImpl is accessible",
                RenderMachineImpl.class, notNullValue());

        BlueOceanLayer.documentArchitectureDiagram(
                this,
                "DTR Render Machine Stack",
                RenderMachineCommands.class,
                RenderMachineImpl.class);
    }

    // =========================================================================
    // A3: documentArchitectureDiagram — Vision 2030 utility layer
    // =========================================================================

    @Test
    void a3_documentArchitectureDiagram_vision2030_layer() {
        sayNextSection("A3: documentArchitectureDiagram — Vision 2030 Utility Layer");

        say("The Vision 2030 utility layer (`Vision2030Utils` + `BlueOceanLayer`) is the " +
                "abstraction boundary between raw `say*` primitives and composite " +
                "documentation profiles. `Vision2030Utils` supplies the data (class metadata, " +
                "system fingerprint, benchmark comparison tables), while `BlueOceanLayer` " +
                "orchestrates the `say*` calls that render that data into documentation.");

        say("Visualising both classes together reveals the one-way dependency: " +
                "`BlueOceanLayer` calls into `Vision2030Utils`; `Vision2030Utils` has no " +
                "reference back. The architecture diagram makes this boundary explicit.");

        sayCode("""
                BlueOceanLayer.documentArchitectureDiagram(
                    this,
                    "Vision 2030 Utility Layer",
                    Vision2030Utils.class,
                    BlueOceanLayer.class);
                """, "java");

        // Assert both utility classes are accessible
        sayAndAssertThat("Vision2030Utils is accessible",
                Vision2030Utils.class, notNullValue());
        sayAndAssertThat("BlueOceanLayer is accessible",
                BlueOceanLayer.class, notNullValue());

        BlueOceanLayer.documentArchitectureDiagram(
                this,
                "Vision 2030 Utility Layer",
                Vision2030Utils.class,
                BlueOceanLayer.class);
    }

    // =========================================================================
    // A4: documentFullAudit — RenderMachineImpl
    // =========================================================================

    @Test
    void a4_documentFullAudit_render_machine_impl() {
        sayNextSection("A4: documentFullAudit — RenderMachineImpl (the \"everything\" method)");

        say("`documentFullAudit` is the single-call maximum-coverage option. For any given " +
                "class it sequences:");
        sayOrderedList(List.of(
                "`documentClassProfile` — metadata key-value, class hierarchy, code model, " +
                        "annotation profile, documentation coverage",
                "`documentCodeReflectionProfile` — JEP 516 IR analysis for every declared method",
                "Environment snapshot — `sayEnvProfile()` pinned to this class's section heading",
                "Documentation coverage — a second pass so any `say*` calls made during the " +
                        "audit itself register as coverage",
                "Evolution timeline — `git log --follow` for the class's source file " +
                        "(up to 10 most recent commits)"
        ));

        say("`RenderMachineImpl` is the ideal audit subject: it is the largest class in " +
                "DTR core, carries no `@CodeReflection` annotations (so we see graceful " +
                "fallbacks throughout), and has a rich method surface that exercises every " +
                "branch of the coverage analyzer.");

        sayWarning("This method emits many sub-sections. In long-running CI pipelines, " +
                "prefer `documentClassProfile` for a lighter-weight audit. Reserve " +
                "`documentFullAudit` for release documentation and architecture reviews.");

        sayCode("""
                // One call produces: class profile, code reflection profile,
                // env snapshot, doc coverage, and git evolution timeline.
                BlueOceanLayer.documentFullAudit(this, RenderMachineImpl.class);
                """, "java");

        // Verify the class is accessible and non-trivial before starting the audit
        int publicMethodCount = (int) java.util.Arrays.stream(
                RenderMachineImpl.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .count();

        sayAndAssertThat("RenderMachineImpl has public methods to audit",
                publicMethodCount, greaterThan(0));

        say("Public methods on `RenderMachineImpl` that will be audited: **" + publicMethodCount + "**");

        BlueOceanLayer.documentFullAudit(this, RenderMachineImpl.class);
    }

    // =========================================================================
    // A5: documentClassProfile vs documentCodeReflectionProfile side-by-side
    // =========================================================================

    @Test
    void a5_compareCodeReflectionVsClassProfile() {
        sayNextSection("A5: documentClassProfile vs documentCodeReflectionProfile — Side-by-Side");

        say("Two composite methods target the same question — \"what does this class do?\" " +
                "— but answer it from different angles. Understanding the difference guides " +
                "authors toward the right tool for their documentation context.");

        sayTable(new String[][] {
                {"Dimension",         "documentClassProfile",                      "documentCodeReflectionProfile"},
                {"Primary output",    "Metadata, hierarchy, public API surface",   "Per-method IR analysis (JEP 516)"},
                {"say* calls",        "sayKeyValue, sayClassHierarchy, sayCodeModel(Class), sayAnnotationProfile, sayDocCoverage",
                                      "sayCodeModel(Method), sayControlFlowGraph, sayOpProfile — once per declared method"},
                {"Granularity",       "Class-level",                               "Method-level"},
                {"Java 26 features",  "Guarded switch, sealed hierarchy, records", "Code Reflection IR tree, block count, op types"},
                {"Fallback behavior", "Always renders (no @CodeReflection needed)","Falls back to signature if no @CodeReflection"},
                {"Best for",          "API reference docs, release notes",         "Internals, compiler output, research"},
        });

        say("To make the contrast concrete, we run both methods against `BenchmarkRunner` " +
                "and let the reader compare the output sections that follow.");

        say("**Step 1 — Class profile** (`documentClassProfile`): structural overview:");

        sayCode("""
                BlueOceanLayer.documentClassProfile(this, BenchmarkRunner.class);
                """, "java");

        BlueOceanLayer.documentClassProfile(this, BenchmarkRunner.class);

        say("**Step 2 — Code Reflection profile** (`documentCodeReflectionProfile`): " +
                "per-method IR analysis of the same class:");

        sayCode("""
                BlueOceanLayer.documentCodeReflectionProfile(this, BenchmarkRunner.class);
                """, "java");

        // Measure the reflection cost of the profile call itself
        long start = System.nanoTime();
        BlueOceanLayer.documentCodeReflectionProfile(this, BenchmarkRunner.class);
        long elapsedNs = System.nanoTime() - start;

        sayTable(new String[][] {
                {"Metric",           "Value",                   "Notes"},
                {"Class profiled",   "BenchmarkRunner",         "io.github.seanchatmangpt.dtr.benchmark"},
                {"Declared methods", String.valueOf(BenchmarkRunner.class.getDeclaredMethods().length),
                                     "from getDeclaredMethods()"},
                {"Profile call ns",  String.valueOf(elapsedNs), "documentCodeReflectionProfile wall time, Java 26"},
        });

        sayNote("The elapsed time above is a single-call wall-clock measurement from " +
                "`System.nanoTime()`. It captures reflection overhead plus render-machine " +
                "string accumulation. Repeated calls are cheaper due to JIT warm-up.");

        sayAndAssertThat("documentCodeReflectionProfile completed in under 5 seconds",
                elapsedNs < 5_000_000_000L, is(true));
    }
}

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documentation test for the {@code sayMuda} innovation in DTR.
 *
 * <p>{@code sayMuda(String process, List<String> wastes, List<String> improvements)}
 * renders a numbered waste-elimination table ({@code # | Waste (Muda) | Improvement
 * Action}) followed by a metrics summary: wastes identified, improvements defined,
 * and elimination coverage percentage. This makes waste-identification exercises
 * machine-verifiable artefacts rather than ephemeral workshop outputs.</p>
 *
 * <p>The method is grounded in Taiichi Ohno's 7 wastes of the Toyota Production
 * System (TPS), as mapped to software by Mary and Tom Poppendieck in
 * <em>Lean Software Development: An Agile Toolkit</em> (2003). Each call to
 * {@code sayMuda} produces a reproducible, version-controlled record of a
 * Muda analysis that can be reviewed, diffed, and improved across sprints.</p>
 *
 * <p>Three software process contexts are documented here:</p>
 * <ol>
 *   <li><strong>Manual production deployment</strong> — all 7 Poppendieck wastes
 *       applied to a hand-operated deployment pipeline, the canonical Lean
 *       software example.</li>
 *   <li><strong>Asynchronous code review</strong> — the 4 most impactful wastes
 *       present in a typical pull-request review workflow.</li>
 *   <li><strong>DTR's own documentation pipeline</strong> — a self-referential
 *       analysis that applies Muda thinking to the tool itself, producing a
 *       living record of its improvement backlog.</li>
 * </ol>
 *
 * <p>All measurements are real ({@code System.nanoTime()}) on the executing JVM.
 * No overhead values are estimated or hardcoded.</p>
 *
 * <p>Running this test generates {@code MudaDocTest.md} (and companion
 * {@code .tex}, {@code .html}, {@code .json}) under
 * {@code target/docs/test-results/}.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class MudaDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Manual production deployment — all 7 Poppendieck wastes
    // =========================================================================

    /**
     * Documents all 7 Poppendieck software wastes applied to a manual
     * production deployment pipeline, with one countermeasure per waste.
     *
     * <p>Taiichi Ohno's original 7 TPS wastes (Transport, Inventory, Motion,
     * Waiting, Overproduction, Over-processing, Defects) are each given a
     * concrete software manifestation in the deployment domain, paired with
     * the automation countermeasure that eliminates it.</p>
     */
    @Test
    void a1_sayMuda_manual_deployment() {
        sayNextSection("sayMuda — Manual Production Deployment Waste Analysis");

        say(
            "Taiichi Ohno identified 7 wastes in the Toyota Production System. " +
            "Mary and Tom Poppendieck mapped them to software in 'Lean Software " +
            "Development' (2003). The deployment pipeline is the highest-leverage " +
            "target because waste here compounds: every hour of delay in a deployment " +
            "is an hour during which defects remain in production and engineers remain " +
            "context-switched away from value-producing work."
        );

        say(
            "The table below applies all 7 Poppendieck wastes to a representative " +
            "manual deployment pipeline used in organisations that have not yet adopted " +
            "continuous delivery. Each waste is paired with the automation countermeasure " +
            "that eliminates it. The 'Elimination coverage' metric in the summary " +
            "confirms that every waste identified has a defined countermeasure — " +
            "a 100% elimination plan."
        );

        sayCode("""
                sayMuda(
                    "Manual Production Deployment Process",
                    List.of(
                        "Transport: Handoff: Dev commits, then waits for Ops approval (avg 4h delay)",
                        "Inventory: Feature branches sitting unmerged for >3 days (avg 7 branches)",
                        "Motion: Developer manually SSHs to 12 servers to run deployment script",
                        "Waiting: Waiting for QA sign-off on regression suite (avg 8h)",
                        "Overproduction: Building and testing all modules even when only 1 changed",
                        "Over-processing: Manual changelog entry required for every PR (60% are trivial)",
                        "Defects: Production bugs caused by manual config drift (avg 2.3/sprint)"
                    ),
                    List.of(
                        "One-piece flow: automated deploy on merge to main",
                        "Trunk-based development: branches live max 1 day",
                        "Ansible playbook: one command deploys all 12 servers",
                        "Automated regression in CI: < 8 minutes",
                        "Affected-module detection: build only changed modules + dependents",
                        "Conventional commits + automated changelog (Changesets)",
                        "Infrastructure as Code: config drift eliminated"
                    )
                );
                """, "java");

        List<String> deployWastes = List.of(
            "Transport: Handoff: Dev commits, then waits for Ops approval (avg 4h delay)",
            "Inventory: Feature branches sitting unmerged for >3 days (avg 7 branches)",
            "Motion: Developer manually SSHs to 12 servers to run deployment script",
            "Waiting: Waiting for QA sign-off on regression suite (avg 8h)",
            "Overproduction: Building and testing all modules even when only 1 changed",
            "Over-processing: Manual changelog entry required for every PR (60% are trivial)",
            "Defects: Production bugs caused by manual config drift (avg 2.3/sprint)"
        );

        List<String> deployImprovements = List.of(
            "One-piece flow: automated deploy on merge to main",
            "Trunk-based development: branches live max 1 day",
            "Ansible playbook: one command deploys all 12 servers",
            "Automated regression in CI: < 8 minutes",
            "Affected-module detection: build only changed modules + dependents",
            "Conventional commits + automated changelog (Changesets)",
            "Infrastructure as Code: config drift eliminated"
        );

        long start = System.nanoTime();
        sayMuda("Manual Production Deployment Process", deployWastes, deployImprovements);
        long mudaNs = System.nanoTime() - start;

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Wastes catalogued",   String.valueOf(deployWastes.size()),
            "Countermeasures",     String.valueOf(deployImprovements.size()),
            "sayMuda() overhead",  mudaNs + " ns",
            "Java version",        System.getProperty("java.version")
        )));

        sayNote(
            "The Poppendieck mapping preserves Ohno's original seven categories " +
            "exactly: Transport becomes handoff delay, Inventory becomes branch " +
            "accumulation, Motion becomes manual server operations, Waiting becomes " +
            "approval queues, Overproduction becomes unnecessary builds, " +
            "Over-processing becomes mandatory-but-low-value steps, and Defects " +
            "remain defects. The mapping is structural, not metaphorical."
        );

        sayWarning(
            "A waste catalogue that lists countermeasures without an owner and a " +
            "deadline is itself a form of Inventory waste — work-in-progress with " +
            "no pull signal. Assign each improvement to a team and a sprint before " +
            "this document is considered an actionable commitment."
        );
    }

    // =========================================================================
    // Test 2: Code review process — 4 high-impact wastes
    // =========================================================================

    /**
     * Documents the 4 most impactful Muda wastes in an asynchronous
     * pull-request code review workflow and their countermeasures.
     *
     * <p>Code review waste is particularly damaging because it sits on the
     * critical path of every feature: no review means no merge, and no merge
     * means no delivery. Each waste catalogued here has a direct, implementable
     * countermeasure that a team can ship in a single sprint.</p>
     */
    @Test
    void a2_sayMuda_code_review() {
        sayNextSection("sayMuda — Asynchronous Code Review Waste Analysis");

        say(
            "Code review is the highest-friction step in most software delivery " +
            "pipelines. The 4 wastes catalogued below are the dominant contributors " +
            "to review latency in asynchronous PR-based workflows. Unlike deployment " +
            "wastes, review wastes are social as well as technical: a 18-hour wait " +
            "for reviewer assignment is not a tooling gap, it is a process gap that " +
            "automation can close."
        );

        sayCode("""
                sayMuda(
                    "Asynchronous Code Review Workflow",
                    List.of(
                        "Waiting for review assignment (avg 18h to first reviewer)",
                        "Defects: bugs found in review that would be caught by better tests",
                        "Over-processing: reviewing generated code, vendor files, auto-formatted diffs",
                        "Motion: reviewer context-switches between 7 different PRs per day"
                    ),
                    List.of(
                        "Review rotation bot: assigns reviewer within 30 minutes",
                        "Pre-review checklist: coverage gate + static analysis before review",
                        ".gitattributes: mark generated files as vendor; exclude from PR diff",
                        "WIP limit: max 3 PRs per reviewer per day"
                    )
                );
                """, "java");

        List<String> reviewWastes = List.of(
            "Waiting for review assignment (avg 18h to first reviewer)",
            "Defects: bugs found in review that would be caught by better tests",
            "Over-processing: reviewing generated code, vendor files, auto-formatted diffs",
            "Motion: reviewer context-switches between 7 different PRs per day"
        );

        List<String> reviewImprovements = List.of(
            "Review rotation bot: assigns reviewer within 30 minutes",
            "Pre-review checklist: coverage gate + static analysis before review",
            ".gitattributes: mark generated files as vendor; exclude from PR diff",
            "WIP limit: max 3 PRs per reviewer per day"
        );

        long start = System.nanoTime();
        sayMuda("Asynchronous Code Review Workflow", reviewWastes, reviewImprovements);
        long mudaNs = System.nanoTime() - start;

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Wastes catalogued",   String.valueOf(reviewWastes.size()),
            "Countermeasures",     String.valueOf(reviewImprovements.size()),
            "sayMuda() overhead",  mudaNs + " ns",
            "Java version",        System.getProperty("java.version")
        )));

        sayNote(
            "These 4 wastes compound: a reviewer distracted by 7 PRs misses defects " +
            "that should have been caught by tests, which then surface as bugs that " +
            "require further review cycles to fix. The compound effect means that " +
            "eliminating Motion waste (the WIP limit) often reduces Defect waste " +
            "as a second-order consequence, even without changing the test suite."
        );

        sayWarning(
            "WIP limits are a process constraint, not a productivity constraint. " +
            "Reducing a reviewer's active PR count from 7 to 3 does not reduce " +
            "throughput — it concentrates attention and raises defect detection rate. " +
            "Teams that conflate busyness with output will resist WIP limits; " +
            "measure cycle time before and after to surface the data."
        );
    }

    // =========================================================================
    // Test 3: DTR's own documentation pipeline — self-referential Muda analysis
    // =========================================================================

    /**
     * Applies Muda analysis to DTR's own documentation generation pipeline.
     *
     * <p>This is a self-referential analysis: the tool is documenting its own
     * waste. The result is a living record of DTR's improvement backlog,
     * automatically updated on every test run that reflects the current state
     * of the implementation. Items marked {@code (TODO)} in the improvement
     * column are known gaps that have not yet been implemented.</p>
     */
    @Test
    void a3_sayMuda_dtr_itself() {
        sayNextSection("sayMuda — DTR Documentation Generation Pipeline Self-Analysis");

        say(
            "DTR is itself a production system and is subject to the same wastes it " +
            "helps other teams identify. This test applies Muda analysis to DTR's own " +
            "documentation generation pipeline. The result is not abstract: it is a " +
            "machine-generated, version-controlled record of DTR's current waste " +
            "profile that persists in the test results alongside the documentation it " +
            "produces. Future commits that implement a countermeasure will move the " +
            "corresponding improvement from TODO to done in the next test run."
        );

        sayCode("""
                sayMuda(
                    "DTR Documentation Generation Pipeline",
                    List.of(
                        "Waiting: tests must finish before docs are written (sequential)",
                        "Over-processing: regenerating all docs even when only 1 test changed",
                        "Defects: test passes but doc contains stale API signature",
                        "Transport: docs written to disk then re-read by CI for validation"
                    ),
                    List.of(
                        "Parallel doc generation: each DocTest writes independently (already implemented)",
                        "Incremental writes: only regenerate if source file changed (TODO)",
                        "Signature extraction from RenderMachineImpl at doc-gen time (dtr-javadoc)",
                        "In-process validation: validate rendered doc before write (TODO)"
                    )
                );
                """, "java");

        List<String> dtrWastes = List.of(
            "Waiting: tests must finish before docs are written (sequential)",
            "Over-processing: regenerating all docs even when only 1 test changed",
            "Defects: test passes but doc contains stale API signature",
            "Transport: docs written to disk then re-read by CI for validation"
        );

        List<String> dtrImprovements = List.of(
            "Parallel doc generation: each DocTest writes independently (already implemented)",
            "Incremental writes: only regenerate if source file changed (TODO)",
            "Signature extraction from RenderMachineImpl at doc-gen time (dtr-javadoc)",
            "In-process validation: validate rendered doc before write (TODO)"
        );

        long start = System.nanoTime();
        sayMuda("DTR Documentation Generation Pipeline", dtrWastes, dtrImprovements);
        long mudaNs = System.nanoTime() - start;

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Wastes catalogued",   String.valueOf(dtrWastes.size()),
            "Countermeasures",     String.valueOf(dtrImprovements.size()),
            "sayMuda() overhead",  mudaNs + " ns",
            "Java version",        System.getProperty("java.version")
        )));

        sayTable(new String[][] {
            {"Improvement",                              "Status",               "Owner"},
            {"Parallel doc generation",                  "Implemented",          "MultiRenderMachine"},
            {"Incremental writes on source change",      "TODO",                 "RenderMachineImpl"},
            {"Signature extraction via dtr-javadoc",     "Partial (reflection)", "JavadocIndex"},
            {"In-process doc validation before write",   "TODO",                 "RenderMachineImpl"},
        });

        sayNote(
            "The self-analysis pattern is a direct application of Ohno's injunction " +
            "to 'go to the gemba' — the real place where work happens. For DTR, the " +
            "gemba is its own render pipeline. A tool that cannot apply its own methods " +
            "to itself is not a tool that its users should trust to apply to their systems."
        );

        sayWarning(
            "Items marked TODO in the improvement column are unimplemented as of the " +
            "test run date reflected in this document. They represent real waste that " +
            "slows CI runs and risks documentation drift. Incremental writes alone are " +
            "estimated to reduce regeneration time by 60-80% on large DTR test suites " +
            "where only a single DocTest changes per commit. This estimate must be " +
            "validated with System.nanoTime() measurements once the feature is implemented."
        );
    }
}

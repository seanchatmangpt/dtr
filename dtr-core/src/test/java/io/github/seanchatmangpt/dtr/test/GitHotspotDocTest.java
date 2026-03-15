/*
 * Copyright (C) 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.githotspot.GitHotspotAnalyzer;
import io.github.seanchatmangpt.dtr.githotspot.GitHotspotAnalyzer.HotspotResult;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.notNullValue;

/**
 * DTR documentation test for {@link GitHotspotAnalyzer}.
 *
 * <p>Demonstrates the {@code sayGitHotspot} innovation: using {@code git log --follow}
 * to surface commit frequency and author churn for any Java class in the project.
 * Files touched by many commits and many authors are maintenance hotspots — high
 * change probability, high review cost, high design pressure.</p>
 *
 * <p>This test is resilient to environments without git access. When git is
 * unavailable or a file has no recorded history, results are documented
 * gracefully rather than causing a test failure.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class GitHotspotDocTest extends DtrTest {

    private static final String PROJECT_ROOT = "/home/user/dtr";

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Git Hotspot Analysis — Overview");

        say("""
                A **git hotspot** is a source file that accumulates an unusually high \
                number of commits from many different authors. High commit frequency \
                signals instability, complexity, or tight coupling — all of which \
                increase maintenance cost.""");

        say("""
                `GitHotspotAnalyzer.analyze(Class<?> clazz, String projectRoot)` invokes \
                `git log --follow --format=\\"%H|%an|%ad|%s\\"` and parses the output \
                into a `HotspotResult` record containing total commits, author breakdown, \
                and first/last commit dates.""");

        sayNote("Analysis is capped at 100 commits per file to keep test runs fast.");

        sayCode("""
                var result = GitHotspotAnalyzer.analyze(DtrContext.class, "/home/user/dtr");
                if (result.found()) {
                    sayTable(GitHotspotAnalyzer.toTable(result));
                } else {
                    say("No git history found — skipping hotspot table.");
                }""", "java");
    }

    // =========================================================================
    // Section 2: DtrContext hotspot analysis
    // =========================================================================

    @Test
    void t02_dtrContextHotspot() {
        sayNextSection("Hotspot: DtrContext");

        say("""
                `DtrContext` is the central context object for JUnit 5 DTR tests. \
                Because it is the primary entry point for test authors it tends to \
                evolve frequently as new `say*` methods are added.""");

        var result = GitHotspotAnalyzer.analyze(DtrContext.class, PROJECT_ROOT);

        sayAndAssertThat("HotspotResult for DtrContext is not null", result, notNullValue());

        if (result.found()) {
            say("Git history was found. Summary table below.");
            sayTable(GitHotspotAnalyzer.toTable(result));

            say("Narrative report:");
            for (var line : GitHotspotAnalyzer.toMarkdown(result)) {
                say(line);
            }

            if (result.totalCommits() >= 5) {
                sayNote("""
                        `DtrContext` has %d commits on record — a confirmed hotspot \
                        worth watching in code review.""".formatted(result.totalCommits()));
            }
        } else {
            sayNote("""
                    Git history was not available for `DtrContext` in this environment. \
                    This is expected in CI containers that perform shallow clones or \
                    mount the source directory without git metadata.""");

            say("Fallback table (no history mode):");
            sayTable(GitHotspotAnalyzer.toTable(result));
        }
    }

    // =========================================================================
    // Section 3: RenderMachineImpl hotspot analysis
    // =========================================================================

    @Test
    void t03_renderMachineImplHotspot() {
        sayNextSection("Hotspot: RenderMachineImpl");

        say("""
                `RenderMachineImpl` is the concrete rendering engine that converts \
                `say*` calls into Markdown output. It is one of the most-edited files \
                in the DTR codebase because every new capability requires changes here.""");

        var result = GitHotspotAnalyzer.analyze(RenderMachineImpl.class, PROJECT_ROOT);

        sayAndAssertThat("HotspotResult for RenderMachineImpl is not null", result, notNullValue());

        if (result.found()) {
            say("Git history was found. Summary table below.");
            sayTable(GitHotspotAnalyzer.toTable(result));

            say("Narrative report:");
            for (var line : GitHotspotAnalyzer.toMarkdown(result)) {
                say(line);
            }

            if (result.totalCommits() >= 5) {
                sayWarning("""
                        `RenderMachineImpl` has %d commits — high churn in the rendering \
                        layer can introduce output-format regressions. Consider extracting \
                        renderer strategies per format.""".formatted(result.totalCommits()));
            }
        } else {
            sayNote("""
                    Git history was not available for `RenderMachineImpl` in this environment. \
                    Running outside a full git clone (e.g., shallow clone, zip extraction) \
                    is the most common cause.""");

            say("Fallback table (no history mode):");
            sayTable(GitHotspotAnalyzer.toTable(result));
        }
    }

    // =========================================================================
    // Section 4: Record schema — HotspotResult
    // =========================================================================

    @Test
    void t04_recordSchema() {
        sayNextSection("HotspotResult Record Schema");

        say("""
                The `HotspotResult` record is a pure data carrier. Its components \
                are documented here using `sayRecordComponents()` so the schema \
                cannot drift from the implementation.""");

        sayRecordComponents(HotspotResult.class);

        say("""
                The `AuthorStat` nested record captures per-author contribution \
                within a single file's history:""");

        sayRecordComponents(GitHotspotAnalyzer.AuthorStat.class);
    }

    // =========================================================================
    // Section 5: Robustness — invalid project root
    // =========================================================================

    @Test
    void t05_robustnessInvalidRoot() {
        sayNextSection("Robustness: Invalid Project Root");

        say("""
                `GitHotspotAnalyzer.analyze()` must never throw. When git is \
                unavailable or the project root does not exist, it returns a \
                `HotspotResult` with `found=false`.""");

        var result = GitHotspotAnalyzer.analyze(DtrContext.class, "/nonexistent/path/xyz");

        sayAndAssertThat("Result is not null even for invalid root", result, notNullValue());

        sayTable(new String[][] {
            {"Input", "Expected Behavior", "Actual found()"},
            {"Invalid project root", "found=false, no exception", String.valueOf(result.found())}
        });

        if (!result.found()) {
            sayNote("Confirmed: invalid project root returns found=false without throwing.");
        }
    }
}

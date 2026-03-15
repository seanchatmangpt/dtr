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
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison;
import io.github.seanchatmangpt.dtr.schemaevolution.SchemaEvolutionReader;
import io.github.seanchatmangpt.dtr.schemaevolution.SchemaEvolutionReader.SchemaEvolutionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DTR documentation test for {@link SchemaEvolutionReader}.
 *
 * <p>Demonstrates {@code saySchemaEvolution}: running {@code git log --follow} against a
 * class's source file to capture its commit history as living documentation. Schema
 * evolution records how a class changed over time — field additions, renames, behavior
 * changes — expressed directly through git history rather than developer-maintained
 * prose.</p>
 *
 * <p>All tests tolerate empty git history: CI containers that perform shallow clones or
 * run without git access will produce empty commit lists rather than failures.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SchemaEvolutionDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Schema Evolution via Git History");

        say("""
                **Schema evolution** tracks how a Java class changes over time by reading \
                its git commit history. Instead of maintaining a manual changelog, \
                `SchemaEvolutionReader` shells out to `git log --follow` and parses the \
                structured output into `CommitEntry` records. The `--follow` flag ensures \
                renames are tracked so history is never lost when a class is moved or \
                refactored.""");

        say("""
                The reader returns a `SchemaEvolutionResult` containing the class name, \
                the relative source path used in the git query, and an ordered list of \
                commits from newest to oldest. If git is unavailable — for example in a \
                shallow clone or containerised CI environment — the commit list is empty \
                and no exception is thrown.""");

        sayCode("""
                // Underlying git command executed by SchemaEvolutionReader
                git -C <projectRoot> log \\
                    --follow \\
                    --pretty=format:"%h|%ad|%an|%s" \\
                    --date=short \\
                    -- <relativeSourcePath>""", "bash");

        sayCode("""
                // Java 26 usage
                var result = SchemaEvolutionReader.read(MyRecord.class, "..");
                SchemaEvolutionReader.toMarkdown(result).forEach(this::sayRaw);""", "java");

        sayNote("""
                The `toMarkdown()` output is capped at 20 entries and escapes \
                pipe characters in commit subjects so Markdown tables render correctly.""");
    }

    // =========================================================================
    // Section 2: saySchemaEvolution — integration via render machine
    // =========================================================================

    @Test
    void t02_trackBenchmarkComparison() {
        sayNextSection("Schema Evolution: BenchmarkComparison");

        say("""
                `BenchmarkComparison` is one of the DTR benchmark support classes. \
                Its git history illustrates how the benchmarking API evolved as new \
                comparison features were added. Here we invoke `saySchemaEvolution` \
                through the render machine — the same path used in production DTR tests.""");

        // ".." resolves from dtr-core (Maven working directory during test) to the git root
        saySchemaEvolution(BenchmarkComparison.class, "..");

        say("Schema evolution rendered without exception.");
    }

    // =========================================================================
    // Section 3: Direct read — inspect result fields
    // =========================================================================

    @Test
    void t03_directRead() {
        sayNextSection("Direct Read: SchemaEvolutionReader.read()");

        say("""
                `SchemaEvolutionReader.read()` can be called directly when test code \
                needs to inspect the raw result — for example to assert on commit count \
                or to build a custom table. The render machine path calls this method \
                internally, but direct access allows richer assertions.""");

        var result = SchemaEvolutionReader.read(BenchmarkComparison.class, "..");

        assertNotNull(result, "SchemaEvolutionResult must not be null");
        assertNotNull(result.commits(), "commits list must not be null");
        assertNotNull(result.className(), "className must not be null");
        assertNotNull(result.sourceFilePath(), "sourceFilePath must not be null");

        var pairs = new LinkedHashMap<String, String>();
        pairs.put("Class name", result.className());
        pairs.put("Source path", result.sourceFilePath());
        pairs.put("Commit count", String.valueOf(result.commits().size()));
        sayKeyValue(pairs);

        if (result.commits().isEmpty()) {
            sayNote("""
                    No git commits found. This is expected in shallow-clone CI environments. \
                    The result is valid — commits is an empty list, not null.""");
        } else {
            say("Most recent commit: `%s` by %s on %s — %s".formatted(
                    result.commits().getFirst().hash(),
                    result.commits().getFirst().author(),
                    result.commits().getFirst().date(),
                    result.commits().getFirst().subject()));
        }
    }

    // =========================================================================
    // Section 4: Markdown rendering
    // =========================================================================

    @Test
    void t04_markdownRendering() {
        sayNextSection("Markdown Rendering: SchemaEvolutionReader.toMarkdown()");

        say("""
                `toMarkdown()` converts a `SchemaEvolutionResult` into an ordered list \
                of Markdown lines. Each line is suitable for passing to `sayRaw()`. The \
                method always returns at least one line (the heading) so the list is \
                never empty.""");

        var result = SchemaEvolutionReader.read(BenchmarkComparison.class, "..");
        var lines = SchemaEvolutionReader.toMarkdown(result);

        assertNotNull(lines, "toMarkdown() must return a non-null list");
        // The contract guarantees at least the heading line is present
        org.junit.jupiter.api.Assertions.assertFalse(
                lines.isEmpty(),
                "toMarkdown() must return at least one line");

        for (var line : lines) {
            sayRaw(line);
        }

        sayNote("Rendered %d Markdown lines for class `%s`.".formatted(
                lines.size(), result.className()));
    }

    // =========================================================================
    // Section 5: Graceful handling — no git in /tmp
    // =========================================================================

    @Test
    void t05_noGitGraceful() {
        sayNextSection("Robustness: No Git Available");

        say("""
                `SchemaEvolutionReader.read()` must never throw regardless of the \
                environment. When called with a project root that has no git repository \
                — such as `/tmp` — it returns a `SchemaEvolutionResult` with an empty \
                commit list rather than propagating an exception.""");

        // java.lang.String is a JDK class with no source in /tmp — guaranteed empty result
        var result = SchemaEvolutionReader.read(String.class, "/tmp");

        assertNotNull(result, "Result must not be null for non-git directory");
        assertNotNull(result.commits(), "commits list must not be null even when git is absent");

        sayTable(new String[][] {
            {"Input class", "Project root", "commits.size()", "Exception thrown?"},
            {String.class.getName(), "/tmp", String.valueOf(result.commits().size()), "false"}
        });

        sayNote("""
                `commits()` is %s. No exception was thrown — the reader degraded \
                gracefully as designed.""".formatted(
                result.commits().isEmpty() ? "empty (expected)" : "non-empty (git found something)"));
    }
}

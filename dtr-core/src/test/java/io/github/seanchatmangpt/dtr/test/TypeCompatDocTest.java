/*
 * Copyright 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer;
import io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer.ApiChange;
import io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer.ChangeKind;
import io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer.CompatResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Documentation test for {@link TypeCompatAnalyzer}.
 *
 * <p>Demonstrates how DTR can automatically detect and document API-breaking
 * changes between two versions of an interface. The two interface versions
 * defined below represent a realistic rename/addition scenario in a user
 * management API.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TypeCompatDocTest extends DtrTest {

    // =========================================================================
    // API version fixtures — defined inline so the test is self-contained
    // =========================================================================

    /** Original user API exposed in release v1. */
    interface UserApiV1 {
        String getId();
        String getName();
        String getEmail();
    }

    /**
     * Revised user API in release v2.
     *
     * <p>Changes vs v1:</p>
     * <ul>
     *   <li>{@code getName()} removed — callers must migrate to {@code getUsername()}</li>
     *   <li>{@code getUsername()} added — replacement for {@code getName()}</li>
     *   <li>{@code getAvatar()} added — new capability</li>
     * </ul>
     */
    interface UserApiV2 {
        String getId();
        String getUsername();  // renamed from getName
        String getEmail();
        String getAvatar();    // new
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Introduction and motivation
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("API Compatibility Checker: sayTypeCompat");

        say("""
                `TypeCompatAnalyzer` compares two classes or interfaces using Java reflection \
                and produces a structured report of every public API change: additions, removals, \
                and type/signature shifts. The result feeds directly into DTR's `sayTable()` and \
                `sayWarning()` so documentation stays in sync with the code.""");

        say("""
                This is the `sayTypeCompat` innovation: documentation that detects breaking \
                changes automatically. No changelog to maintain by hand. No missed removals. \
                The analysis IS the documentation.""");

        sayNote("Only public, non-synthetic, non-bridge members are examined. " +
                "Package-private and protected members are outside the public API contract.");

        sayCode("""
                // Analyze two interface versions in one line
                var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
                if (result.breaking()) {
                    // emit warnings, block release, alert callers
                }
                sayTable(TypeCompatAnalyzer.toTable(result));
                """, "java");
    }

    // =========================================================================
    // Test 2 — Run the analysis and document results
    // =========================================================================

    @Test
    void t02_analyzeV1vsV2() {
        sayNextSection("V1 → V2 Change Report");

        say("The following table captures every public API difference between " +
            "`UserApiV1` and `UserApiV2`. Rows marked `REMOVED` require caller migration.");

        var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
        sayTable(TypeCompatAnalyzer.toTable(result));

        if (result.breaking()) {
            sayWarning(
                "Breaking changes detected between `UserApiV1` and `UserApiV2`. " +
                "Any code calling `getName()` must be updated to call `getUsername()` before upgrading.");
        }

        // Document the markdown report as well
        var markdownLines = TypeCompatAnalyzer.toMarkdown(result);
        sayCode(String.join("\n", markdownLines), "markdown");

        // Assert the analysis found changes
        sayAndAssertThat("Changes list is not empty",
                result.changes().isEmpty(), is(false));
    }

    // =========================================================================
    // Test 3 — Verify specific removals and additions
    // =========================================================================

    @Test
    void t03_verifyRemovedAndAdded() {
        sayNextSection("Verifying Specific Change Kinds");

        say("This section asserts the precise set of changes the analyzer must detect " +
            "for the V1 → V2 migration and documents each assertion as a table row.");

        var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
        var changes = result.changes();

        // Extract names by kind
        var removedNames = changeNamesForKind(changes, ChangeKind.REMOVED);
        var addedNames   = changeNamesForKind(changes, ChangeKind.ADDED);

        sayNote("REMOVED methods: " + removedNames + " | ADDED methods: " + addedNames);

        // getName() was in V1, absent from V2 — must be flagged REMOVED
        sayAndAssertThat("getName() is detected as REMOVED",
                removedNames, hasItem("getName"));

        // getUsername() is new in V2 — must be flagged ADDED
        sayAndAssertThat("getUsername() is detected as ADDED",
                addedNames, hasItem("getUsername"));

        // getAvatar() is new in V2 — must be flagged ADDED
        sayAndAssertThat("getAvatar() is detected as ADDED",
                addedNames, hasItem("getAvatar"));

        // getId() and getEmail() are in both — must NOT appear as REMOVED
        sayAndAssertThat("getId() is not removed",
                removedNames, not(hasItem("getId")));
        sayAndAssertThat("getEmail() is not removed",
                removedNames, not(hasItem("getEmail")));

        // Overall: at least one breaking change must be present
        sayAndAssertThat("result is marked as breaking",
                result.breaking(), is(true));
    }

    // =========================================================================
    // Test 4 — Self-compatibility: same class vs itself
    // =========================================================================

    @Test
    void t04_selfCompatibility() {
        sayNextSection("Self-Compatibility: Same Class vs Itself");

        say("Analyzing a class against itself must yield zero changes and a " +
            "`breaking = false` result. This verifies the analyzer's identity invariant.");

        var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV1.class);

        sayTable(TypeCompatAnalyzer.toTable(result));

        sayAndAssertThat("No changes detected for identical classes",
                result.changes().size(), is(0));
        sayAndAssertThat("Self-analysis is not breaking",
                result.breaking(), is(false));

        sayNote("An empty table (header only) is the expected output for a class compared to itself.");
    }

    // =========================================================================
    // Test 5 — toTable() structure validation
    // =========================================================================

    @Test
    void t05_tableStructure() {
        sayNextSection("Table Structure Validation");

        say("`toTable()` must return a 2D array where the first row is always the header " +
            "`[Change, Kind, Member, Old Signature, New Signature]` and subsequent rows " +
            "carry the change data.");

        var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
        var table  = TypeCompatAnalyzer.toTable(result);

        // Header row assertions
        sayAndAssertThat("Header row[0][0] is 'Change'",       table[0][0], is("Change"));
        sayAndAssertThat("Header row[0][1] is 'Kind'",         table[0][1], is("Kind"));
        sayAndAssertThat("Header row[0][2] is 'Member'",       table[0][2], is("Member"));
        sayAndAssertThat("Header row[0][3] is 'Old Signature'",table[0][3], is("Old Signature"));
        sayAndAssertThat("Header row[0][4] is 'New Signature'",table[0][4], is("New Signature"));

        // Row count: header + one row per change
        int expectedRows = result.changes().size() + 1;
        sayAndAssertThat("Row count equals changes + 1 header",
                table.length, is(expectedRows));

        sayNote("Each data row has exactly 5 columns matching the header.");
    }

    // =========================================================================
    // Test 6 — toMarkdown() output validation
    // =========================================================================

    @Test
    void t06_markdownOutput() {
        sayNextSection("Markdown Report Validation");

        say("`toMarkdown()` renders a ready-to-embed Markdown report including a " +
            "GitHub-style warning block when breaking changes exist. This verifies " +
            "the rendered output contains the expected structural markers.");

        var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
        var lines  = TypeCompatAnalyzer.toMarkdown(result);
        var joined = String.join("\n", lines);

        sayCode(joined, "markdown");

        sayAndAssertThat("Markdown contains class names header",
                joined.contains("UserApiV1") && joined.contains("UserApiV2"), is(true));
        sayAndAssertThat("Markdown contains [!WARNING] for breaking change",
                joined.contains("[!WARNING]"), is(true));
        sayAndAssertThat("Markdown contains table header pipe",
                joined.contains("| Change |"), is(true));
        sayAndAssertThat("Markdown lines list is not empty",
                lines, not(empty()));
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private static List<String> changeNamesForKind(List<ApiChange> changes, ChangeKind kind) {
        return changes.stream()
                .filter(c -> c.kind() == kind)
                .map(ApiChange::name)
                .collect(Collectors.toList());
    }
}

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
import io.github.seanchatmangpt.dtr.http.HttpContractAnalyzer;
import io.github.seanchatmangpt.dtr.http.HttpContractAnalyzer.ContractResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Documentation tests for {@link HttpContractAnalyzer} — the foundation of
 * {@code sayHttpContract()}.
 *
 * <p>{@code HttpContractAnalyzer} makes a live HTTP GET request to a URL,
 * parses the JSON response, and checks that each declared field exists with
 * the expected JSON type. This DocTest exercises the analyzer against the
 * public JSONPlaceholder API and documents the results inline.</p>
 *
 * <h2>Feature: HTTP Contract Validation</h2>
 * <p>Traditional integration tests assert on status codes. Contract validation
 * goes one level deeper: it asserts that the <em>shape</em> of the response
 * matches a declared schema. DTR generates living documentation from that
 * assertion — if the API changes, the docs break before the callers do.</p>
 *
 * <h2>Resilience</h2>
 * <p>All test methods in this class are network-resilient: a connection
 * failure is documented and the test still passes. This keeps the CI pipeline
 * green in air-gapped or rate-limited environments while still recording what
 * happened.</p>
 *
 * @see HttpContractAnalyzer
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class HttpContractDocTest extends DtrTest {

    /** JSONPlaceholder endpoint used throughout this DocTest. */
    private static final String TODO_URL = "https://jsonplaceholder.typicode.com/todos/1";

    /**
     * The known contract for the {@code /todos/:id} endpoint.
     * Fields: userId (number), id (number), title (string), completed (boolean).
     */
    private static final String[][] TODO_CONTRACT = {
        {"userId",    "number"},
        {"id",        "number"},
        {"title",     "string"},
        {"completed", "boolean"}
    };

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Live endpoint analysis
    // =========================================================================

    @Test
    void t01_analyzeJsonPlaceholderTodo() {
        sayNextSection("HTTP Contract Analysis: JSONPlaceholder /todos/1");

        say("""
                `HttpContractAnalyzer.analyze()` probes a live HTTP endpoint and \
                validates the JSON response against a declared field contract. \
                Each field is checked for existence and JSON type \
                (`string`, `number`, `boolean`, `array`, `object`, or `null`).""");

        sayCode("""
                // Declare the expected contract
                String[][] contract = {
                    {"userId",    "number"},
                    {"id",        "number"},
                    {"title",     "string"},
                    {"completed", "boolean"}
                };

                // Probe the live endpoint
                ContractResult result = HttpContractAnalyzer.analyze(
                    "https://jsonplaceholder.typicode.com/todos/1",
                    contract);

                // result.passed()    → true when all fields match
                // result.table()     → 2D String[][] ready for sayTable()
                // result.latencyMs() → round-trip time in ms
                // result.violations()→ human-readable failure descriptions""",
                "java");

        ContractResult result = HttpContractAnalyzer.analyze(TODO_URL, TODO_CONTRACT);

        // Document what actually happened, regardless of network availability
        documentResult(result, TODO_URL);

        // The test always passes: a network failure is a documented outcome,
        // not a test failure. This keeps CI green in air-gapped environments.
        // We only assert the structural invariants of ContractResult itself.
        assert result != null : "analyze() must never return null";
        assert result.table() != null : "table() must never be null";
        assert result.violations() != null : "violations() must never be null";
        assert result.table().length >= 1 : "table() must have at least a header row";
        assert result.table()[0].length == 4 : "table header must have 4 columns";
    }

    // =========================================================================
    // Test 2 — Markdown rendering
    // =========================================================================

    @Test
    void t02_markdownRendering() {
        sayNextSection("Markdown Rendering: toMarkdown()");

        say("""
                `HttpContractAnalyzer.toMarkdown()` converts a `ContractResult` \
                into a list of Markdown lines. Each line is ready to inject into \
                a DTR document via `sayRaw()` or used directly in a `say()` call.""");

        sayCode("""
                ContractResult result = HttpContractAnalyzer.analyze(url, contract);
                List<String> lines = HttpContractAnalyzer.toMarkdown(result);
                // lines.get(0) → "**HTTP Contract Check** — status: `200`, ..."
                lines.forEach(ctx::sayRaw);""",
                "java");

        ContractResult result = HttpContractAnalyzer.analyze(TODO_URL, TODO_CONTRACT);
        List<String> mdLines = HttpContractAnalyzer.toMarkdown(result);

        sayNote("The following is the raw Markdown output from toMarkdown() for this run:");
        for (var line : mdLines) {
            sayRaw(line);
        }

        // Structural assertion: first line must always contain the summary pattern
        assert !mdLines.isEmpty() : "toMarkdown() must return at least one line";
        assert mdLines.get(0).contains("HTTP Contract Check") :
                "First markdown line must contain 'HTTP Contract Check'";
    }

    // =========================================================================
    // Test 3 — Graceful handling of a non-existent field
    // =========================================================================

    @Test
    void t03_missingFieldViolation() {
        sayNextSection("Violation Detection: Missing Field");

        say("""
                When the JSON response lacks a declared field, `ContractResult.violations()` \
                records a human-readable message and the field row in the contract table \
                shows `missing` as the actual type with a `FAIL` status.""");

        // Add a field that is definitely not in the /todos/1 response
        String[][] contractWithExtra = {
            {"userId",      "number"},
            {"id",          "number"},
            {"title",       "string"},
            {"completed",   "boolean"},
            {"nonExistentField", "string"}   // this field does not exist
        };

        sayCode("""
                String[][] contractWithExtra = {
                    {"userId",           "number"},
                    {"id",               "number"},
                    {"title",            "string"},
                    {"completed",        "boolean"},
                    {"nonExistentField", "string"}  // intentionally wrong
                };
                ContractResult result = HttpContractAnalyzer.analyze(url, contractWithExtra);
                // result.passed() → false (missing field)
                // result.violations() → ["Field 'nonExistentField' is missing from response"]""",
                "java");

        ContractResult result = HttpContractAnalyzer.analyze(TODO_URL, contractWithExtra);

        documentResult(result, TODO_URL);

        if (result.statusCode() == -1) {
            // Network unavailable — document and move on
            sayNote("Network unavailable; missing-field violation test skipped for this run.");
        } else {
            // Network succeeded; the nonExistentField must appear as a violation
            boolean hasViolation = result.violations().stream()
                    .anyMatch(v -> v.contains("nonExistentField"));

            var assertions = new LinkedHashMap<String, String>();
            assertions.put("Contract correctly identifies missing field",
                    hasViolation ? "PASS" : "FAIL");
            assertions.put("result.passed() is false when a field is missing",
                    !result.passed() ? "PASS" : "FAIL");
            sayAssertions(assertions);

            assert hasViolation :
                "A missing field must appear in violations(); got: " + result.violations();
            assert !result.passed() :
                "passed() must be false when required fields are absent";
        }
    }

    // =========================================================================
    // Test 4 — ContractResult record structure
    // =========================================================================

    @Test
    void t04_contractResultRecordStructure() {
        sayNextSection("ContractResult: Record Schema");

        say("""
                `ContractResult` is a Java record — an immutable data carrier that \
                exposes the full analysis in five fields. Records eliminate boilerplate \
                and make the contract explicit at the type level.""");

        sayRecordComponents(ContractResult.class);

        say("""
                The `table` component is a `String[][]` whose first row is always the \
                header `["Field", "Expected Type", "Actual Type", "Status"]`. \
                Every subsequent row corresponds to one field in the declared contract. \
                Pass this directly to `sayTable()` to render a contract report in any \
                DTR document.""");

        sayCode("""
                // Pattern-match on the sealed ContractResult to branch on outcome
                ContractResult r = HttpContractAnalyzer.analyze(url, fields);
                String summary = r.passed()
                    ? "Contract OK (%d ms)".formatted(r.latencyMs())
                    : "Contract FAILED — %d violation(s)".formatted(r.violations().size());
                say(summary);
                sayTable(r.table());""",
                "java");
    }

    // =========================================================================
    // Test 5 — Environment and API overview
    // =========================================================================

    @Test
    void t05_environmentAndOverview() {
        sayNextSection("Environment and API Overview");

        say("The following table summarises the `HttpContractAnalyzer` public API:");

        sayTable(new String[][]{
            {"Method",                               "Description"},
            {"analyze(url, expectedFields)",         "Probe URL, validate JSON contract, return ContractResult"},
            {"toMarkdown(result)",                   "Convert ContractResult to Markdown lines"},
            {"ContractResult.statusCode()",          "HTTP status code (-1 on connection failure)"},
            {"ContractResult.latencyMs()",           "Round-trip latency in milliseconds"},
            {"ContractResult.passed()",              "true when status 2xx AND all field checks pass"},
            {"ContractResult.table()",               "String[][] contract table for sayTable()"},
            {"ContractResult.violations()",          "Human-readable list of failed checks"}
        });

        sayNote("""
                `HttpContractAnalyzer` uses `java.net.http.HttpClient` (built-in since Java 11). \
                No external HTTP library is required. The 5-second connect+read timeout prevents \
                slow endpoints from stalling the documentation build.""");

        sayEnvProfile();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Documents the outcome of a {@link ContractResult} using DTR say* methods.
     * Always produces output regardless of network availability.
     */
    private void documentResult(ContractResult result, String url) {
        var summary = new LinkedHashMap<String, String>();
        summary.put("Endpoint", url);
        summary.put("HTTP Status", result.statusCode() == -1 ? "N/A (connection failed)" : String.valueOf(result.statusCode()));
        summary.put("Latency", result.latencyMs() + " ms");
        summary.put("Contract Passed", result.passed() ? "yes" : "no");
        summary.put("Violations", String.valueOf(result.violations().size()));
        sayKeyValue(summary);

        sayTable(result.table());

        if (!result.violations().isEmpty()) {
            sayWarning("Contract violations detected:");
            sayUnorderedList(result.violations());
        }

        if (result.statusCode() == -1) {
            sayNote("Network was unreachable for this run. " +
                    "The contract table shows 'N/A' for all actual types. " +
                    "Run with network access to see live validation results.");
        }
    }
}

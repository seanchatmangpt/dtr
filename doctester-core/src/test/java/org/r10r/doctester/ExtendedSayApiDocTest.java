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
package org.r10r.doctester;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Url;

/**
 * Demonstrates the extended say* API for rich documentation generation.
 *
 * This test showcases all 9 new documentation methods using Diataxis structure:
 * - Tutorials (this test)
 * - How-to guides (CLAUDE.md)
 * - Explanation (comments & docs)
 * - Reference (method signatures)
 *
 * All methods generate clean, portable Markdown suitable for GitHub, documentation
 * platforms, and static site generators.
 */
class ExtendedSayApiDocTest extends DocTester {

    @Test
    void testDocumentationAPI() {
        sayNextSection("Extended Documentation API");
        say("DocTester provides a rich set of `say*` methods for flexible, "
                + "powerful documentation generation. This guide demonstrates all 9 methods.");

        // ========================================================================
        // 1. sayTable() - Markdown tables for comparisons
        // ========================================================================
        sayNextSection("1. Tables with sayTable()");
        say("Use `sayTable()` to render feature matrices, API response schemas, or "
                + "version comparisons. The first row becomes table headers.");

        sayTable(new String[][] {
            {"Feature", "DocTester v1", "DocTester v2"},
            {"Markdown output", "No", "✓ Yes"},
            {"Extended say* API", "No", "✓ Yes (9 methods)"},
            {"Java 25 support", "No", "✓ Yes"},
            {"Diataxis structure", "No", "✓ Yes"},
            {"JSON documentation", "Manual", "✓ Automatic"}
        });

        // ========================================================================
        // 2. sayCode() - Syntax-highlighted code blocks
        // ========================================================================
        sayNextSection("2. Code Blocks with sayCode()");
        say("Display code examples with language hints for syntax highlighting. "
                + "Supports any language: java, sql, json, xml, bash, etc.");

        sayCode("SELECT u.id, u.name, COUNT(a.id) as article_count\n"
                + "FROM users u\n"
                + "LEFT JOIN articles a ON u.id = a.user_id\n"
                + "WHERE u.active = true\n"
                + "GROUP BY u.id\n"
                + "ORDER BY article_count DESC;", "sql");

        say("SQL queries are especially useful in integration tests to document "
                + "expected database state and query patterns.");

        // ========================================================================
        // 3. sayWarning() - Important callout boxes
        // ========================================================================
        sayNextSection("3. Warnings with sayWarning()");
        say("Highlight breaking changes, security concerns, or critical gotchas that "
                + "users **must** know about.");

        sayWarning("Markdown-only output: DocTester v2 no longer generates HTML. "
                + "Use a Markdown renderer (GitHub, Pandoc, Hugo) to view documentation.");

        sayNote("This change makes documentation portable and version-control friendly.");

        // ========================================================================
        // 4. sayKeyValue() - Metadata in clean format
        // ========================================================================
        sayNextSection("4. Key-Value Pairs with sayKeyValue()");
        say("Document configuration, environment variables, or HTTP headers in a "
                + "clean, scannable format.");

        sayKeyValue(Map.of(
            "Base URL", "http://localhost:8080",
            "API Version", "v2",
            "Auth Method", "OAuth2",
            "Rate Limit", "1000 requests/minute",
            "Timeout", "30 seconds"
        ));

        // ========================================================================
        // 5. sayUnorderedList() - Checklists and features
        // ========================================================================
        sayNextSection("5. Bullet Lists with sayUnorderedList()");
        say("Use unordered lists for prerequisites, feature checklists, or options "
                + "where sequence doesn't matter.");

        sayUnorderedList(Arrays.asList(
            "Java 25 LTS with --enable-preview",
            "Maven 4.0.0+ or mvnd 2+",
            "JUnit 4.12+ (or JUnit 5 via @ExtendWith(DocTesterExtension.class))",
            "Jackson 2.5.4+ for JSON/XML serialization",
            "Apache HttpComponents 4.5+ for HTTP client"
        ));

        // ========================================================================
        // 6. sayOrderedList() - Workflows and procedures
        // ========================================================================
        sayNextSection("6. Numbered Lists with sayOrderedList()");
        say("Document step-by-step workflows, procedures, or sequential instructions.");

        sayOrderedList(Arrays.asList(
            "Extend DocTester in your test class",
            "Override testServerUrl() to return your server's base URL",
            "Use say*() methods to document test behavior",
            "Assertions are automatically logged in green/red",
            "Call finishDocTest() in @AfterClass to generate index",
            "Output appears in target/docs/<TestClassName>.md"
        ));

        // ========================================================================
        // 7. sayJson() - Pretty-printed JSON payloads
        // ========================================================================
        sayNextSection("7. JSON Payloads with sayJson()");
        say("Serialize objects to pretty-printed JSON code blocks. Especially useful "
                + "for documenting API response structures.");

        record User(int id, String name, String email, boolean active) {}
        sayJson(new User(1, "Alice Johnson", "alice@example.com", true));

        say("The `sayJson()` method uses Jackson's pretty-printer to render objects "
                + "in a readable format. Perfect for showing expected response shapes.");

        // ========================================================================
        // 8. sayAssertions() - Test result matrices
        // ========================================================================
        sayNextSection("8. Assertion Results with sayAssertions()");
        say("Summarize test validations in a clean Check/Result table. Useful for "
                + "batch validation or non-assertion results.");

        sayAssertions(Map.of(
            "HTTP Status is 200", "✓ PASS",
            "Response time < 100ms", "✓ PASS",
            "JSON schema valid", "✓ PASS",
            "Cache headers present", "⚠ INFO — X-Cache-Hit not set",
            "CORS headers match policy", "✗ FAIL — Access-Control-Allow-Origin missing"
        ));

        // ========================================================================
        // 9. Combining with sayAndAssertThat()
        // ========================================================================
        sayNextSection("9. Integration with Assertions");
        say("New say* methods work seamlessly with traditional `sayAndAssertThat()` "
                + "assertions. Both generate markdown, side-by-side.");

        sayAndAssertThat("DocTester is extensible", "9 methods implemented",
                equalTo("9 methods implemented"));

        // ========================================================================
        // Final Summary
        // ========================================================================
        sayNextSection("Summary: When to Use Each Method");

        sayTable(new String[][] {
            {"Method", "Best For", "Example"},
            {"sayTable()", "Comparisons, matrices", "Feature list, API versions"},
            {"sayCode()", "Code examples", "SQL queries, configuration"},
            {"sayWarning()", "Critical info", "Breaking changes, security"},
            {"sayNote()", "Tips & clarifications", "Context, helpful hints"},
            {"sayKeyValue()", "Metadata", "Environment vars, headers"},
            {"sayUnorderedList()", "Options, features", "Prerequisites, checklist"},
            {"sayOrderedList()", "Sequences, steps", "Workflow, procedure"},
            {"sayJson()", "Data structures", "API response, configuration"},
            {"sayAssertions()", "Validation summary", "Test results, checks"}
        });

        say("All methods generate **pure Markdown**, making documentation:");
        sayUnorderedList(Arrays.asList(
            "**Portable** — Works on GitHub, GitLab, Gitea, Markdown renderers",
            "**Version-control friendly** — Clean text diffs in git",
            "**Tool-agnostic** — No HTML/CSS/JS dependencies"
        ));

        say("Combined with the `say()`, `sayNextSection()`, and `sayAndMakeRequest()` "
                + "methods, DocTester provides everything needed for **living, executable "
                + "documentation**.");
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}

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
import java.util.Map;

/**
 * Documents the {@code sayDecisionTree(String title, Map<String, Object> branches)}
 * innovation introduced in DTR.
 *
 * <p>{@code sayDecisionTree} renders a branching decision algorithm as a Mermaid
 * {@code flowchart TD} diagram directly inside a documentation test. This converts
 * institutional knowledge that normally lives only in an architect's head — or buried
 * in prose — into a machine-verified, version-controlled flowchart that renders in
 * every GitHub Markdown view without any plugin or external dependency.</p>
 *
 * <p>The {@code branches} parameter is a {@code Map<String, Object>} where:</p>
 * <ul>
 *   <li>Each key is a branch label — a condition, question, or edge description.</li>
 *   <li>Each value is either a {@code String} (a leaf answer/outcome) or a nested
 *       {@code Map<String, Object>} (a sub-tree with further branching).</li>
 * </ul>
 *
 * <p>{@code LinkedHashMap} must be used in preference to {@code Map.of()} whenever
 * the visual order of branches matters. {@code Map.of()} has undefined iteration order
 * in Java, which produces non-deterministic Mermaid output — a subtle form of
 * documentation drift that is difficult to detect in code review.</p>
 *
 * <p>Three scenarios are documented here, each representative of a real architectural
 * decision that DTR-maintained codebases must make explicitly:</p>
 * <ol>
 *   <li>CalVer release type selection — the DTR-specific workflow every contributor uses.</li>
 *   <li>HTTP error handling — a universal pattern for resilient API clients.</li>
 *   <li>Feature flag evaluation — a two-level gate controlling gradual rollouts.</li>
 * </ol>
 *
 * @see DtrTest#sayDecisionTree(String, Map)
 * @since 2.7.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DecisionTreeDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1: DTR release type selector — CalVer semantics
    // =========================================================================

    /**
     * Documents the CalVer release type decision that every DTR contributor faces
     * before running {@code make release-*}. The tree encodes the three-way fork
     * ({@code release-year} / {@code release-minor} / {@code release-patch}) as
     * an executable Mermaid flowchart so that a new contributor can determine the
     * correct command by following the diagram rather than reading prose.
     */
    @Test
    void a1_sayDecisionTree_release_type_selector() {
        sayNextSection("sayDecisionTree — DTR CalVer Release Type Selector");

        say(
            "DTR uses Calendar Versioning (CalVer) with the scheme YYYY.MINOR.PATCH. " +
            "The year component is owned by the calendar — it advances automatically " +
            "on 1 January, not by human decision. The minor and patch components are " +
            "owned by the type of change being released. Every contributor must answer " +
            "two questions before issuing a release command: has the year rolled over, " +
            "and is this change a new capability or a correction?"
        );

        say(
            "The three release commands map to exactly three outcomes. No other version " +
            "numbers are valid. The script computes the arithmetic; the human supplies " +
            "only the semantic classification. Documenting this decision as a flowchart " +
            "eliminates the ambiguity that prose descriptions of versioning policies " +
            "inevitably produce."
        );

        sayCode("""
                // Build the release type decision tree using LinkedHashMap to
                // preserve the top-to-bottom visual order of the branches.
                var newYearBranch = new LinkedHashMap<String, Object>();
                newYearBranch.put("Yes", "make release-year");

                var methodBranch = new LinkedHashMap<String, Object>();
                methodBranch.put("Yes", "make release-minor");
                methodBranch.put("No",  "make release-patch");

                var noBranch = new LinkedHashMap<String, Object>();
                noBranch.put("New say* method?", methodBranch);

                var branches = new LinkedHashMap<String, Object>();
                branches.put("Is it a new year?", newYearBranch);
                branches.put("No",                noBranch);

                sayDecisionTree("Which release type?", branches);
                """, "java");

        sayTable(new String[][] {
            {"Command",              "Semantic meaning",                "Version change"},
            {"make release-year",    "January 1 year boundary crossed", "YYYY+1.1.0"},
            {"make release-minor",   "New say* method or capability",   "YYYY.(N+1).0"},
            {"make release-patch",   "Bug fix or dependency update",    "YYYY.MINOR.(N+1)"},
        });

        sayWarning(
            "The human picks the release type; the script computes the version number. " +
            "Never type a version manually. Running `make release-minor` when the change " +
            "is actually a bug fix is not wrong in itself, but it signals a new feature " +
            "to downstream consumers who parse MINOR increments as API additions. " +
            "Semantic accuracy matters for automated dependency management tools."
        );

        var newYearBranch = new LinkedHashMap<String, Object>();
        newYearBranch.put("Yes", "make release-year");

        var methodBranch = new LinkedHashMap<String, Object>();
        methodBranch.put("Yes", "make release-minor");
        methodBranch.put("No",  "make release-patch");

        var noBranch = new LinkedHashMap<String, Object>();
        noBranch.put("New say* method?", methodBranch);

        var branches = new LinkedHashMap<String, Object>();
        branches.put("Is it a new year?", newYearBranch);
        branches.put("No",                noBranch);

        sayDecisionTree("Which release type?", branches);

        sayNote(
            "The `Is it a new year?` node appears first in the flowchart because it " +
            "is inserted first into the LinkedHashMap. Using Map.of() here would produce " +
            "a non-deterministic branch order that changes between JVM invocations, " +
            "making the rendered diagram unreliable in version-controlled documentation."
        );
    }

    // =========================================================================
    // a2: HTTP error handling — 4xx/5xx classification and response strategy
    // =========================================================================

    /**
     * Documents the HTTP error handling decision that every API client must implement.
     * The tree separates client errors (4xx) from server errors (5xx), then applies
     * specific strategies — 404 page, JSON error body, retry with backoff, or log-and-
     * continue — based on the concrete status code range.
     */
    @Test
    void a2_sayDecisionTree_error_handling() {
        sayNextSection("sayDecisionTree — HTTP Error Handler");

        say(
            "HTTP error handling is one of the most common sources of silent failure in " +
            "distributed systems. A client that treats all non-200 responses identically " +
            "will retry 404s indefinitely, swallow rate-limit signals, and surface 5xx " +
            "errors to the end user with no recovery attempt. The correct strategy depends " +
            "on the status code family: 4xx errors indicate a client-side problem that " +
            "retrying will not fix, whereas 5xx errors indicate a server-side transient " +
            "that exponential backoff can recover from."
        );

        say(
            "Documenting this logic as a `sayDecisionTree` flowchart rather than as inline " +
            "comments serves two purposes. First, the diagram is rendered in every pull " +
            "request review, making the error handling contract visible to reviewers who " +
            "do not read Java. Second, any change to the branching logic requires a " +
            "corresponding change to the DTR test, making silent regressions impossible."
        );

        sayCode("""
                var notFoundBranch = new LinkedHashMap<String, Object>();
                notFoundBranch.put("Yes", "Return 404 page");
                notFoundBranch.put("No",  "Return error JSON");

                var fourXxBranch = new LinkedHashMap<String, Object>();
                fourXxBranch.put("404?", notFoundBranch);

                var retryBranch = new LinkedHashMap<String, Object>();
                retryBranch.put("Yes", "Retry with backoff");
                retryBranch.put("No",  "Log and continue");

                var fiveXxBranch = new LinkedHashMap<String, Object>();
                fiveXxBranch.put("5xx?", retryBranch);

                var branches = new LinkedHashMap<String, Object>();
                branches.put("4xx status?", fourXxBranch);
                branches.put("No",          fiveXxBranch);

                sayDecisionTree("HTTP Error Handler", branches);
                """, "java");

        sayTable(new String[][] {
            {"Status range", "Specific check", "Strategy",             "Rationale"},
            {"4xx",          "404",            "Return 404 page",      "Resource does not exist; user-facing error page"},
            {"4xx",          "Other 4xx",      "Return error JSON",    "Client bug; structured body for programmatic handling"},
            {"5xx",          "Yes",            "Retry with backoff",   "Transient server fault; exponential backoff recovers"},
            {"5xx",          "No (non-5xx)",   "Log and continue",     "Unexpected code; record and degrade gracefully"},
        });

        var notFoundBranch = new LinkedHashMap<String, Object>();
        notFoundBranch.put("Yes", "Return 404 page");
        notFoundBranch.put("No",  "Return error JSON");

        var fourXxBranch = new LinkedHashMap<String, Object>();
        fourXxBranch.put("404?", notFoundBranch);

        var retryBranch = new LinkedHashMap<String, Object>();
        retryBranch.put("Yes", "Retry with backoff");
        retryBranch.put("No",  "Log and continue");

        var fiveXxBranch = new LinkedHashMap<String, Object>();
        fiveXxBranch.put("5xx?", retryBranch);

        var branches = new LinkedHashMap<String, Object>();
        branches.put("4xx status?", fourXxBranch);
        branches.put("No",          fiveXxBranch);

        sayDecisionTree("HTTP Error Handler", branches);

        sayWarning(
            "Never retry 4xx responses. A 429 Too Many Requests is the only 4xx that " +
            "benefits from a delay-and-retry, and it must use the Retry-After header " +
            "value rather than a fixed backoff interval. Retrying a 401 or 403 without " +
            "refreshing credentials will exhaust the retry budget and still fail."
        );
    }

    // =========================================================================
    // a3: Feature flag evaluation — two-level gate for gradual rollout
    // =========================================================================

    /**
     * Documents the feature flag evaluation logic for a gradual rollout gate.
     * The two-level tree first checks whether the flag is globally enabled, then
     * checks whether the requesting user is in the pilot cohort. This is the
     * canonical pattern for controlled feature exposure without a full kill-switch
     * redeployment.
     */
    @Test
    void a3_sayDecisionTree_feature_flag() {
        sayNextSection("sayDecisionTree — Feature Flag Evaluation");

        say(
            "Feature flags are the standard mechanism for decoupling code deployment " +
            "from feature activation. A two-level evaluation gate is the most common " +
            "real-world shape: an outer global kill-switch that can disable the feature " +
            "for all users instantly, and an inner cohort check that restricts the active " +
            "feature to a pilot group during the gradual rollout window. Capturing this " +
            "logic as a `sayDecisionTree` turns the flag evaluation contract into a " +
            "first-class documentation artefact that is verified on every CI run."
        );

        say(
            "The diagram below encodes the exact evaluation order. Global enablement is " +
            "checked first because it is the cheapest operation — a single boolean config " +
            "read requires no database or RPC call. The pilot cohort check is deferred " +
            "until the global gate passes, avoiding unnecessary identity lookups for the " +
            "common case where the flag is disabled in production."
        );

        sayCode("""
                var pilotBranch = new LinkedHashMap<String, Object>();
                pilotBranch.put("Yes", "Show new feature");
                pilotBranch.put("No",  "Show existing feature");

                var branches = new LinkedHashMap<String, Object>();
                branches.put("Flag globally enabled?", Map.of(
                    "Yes", pilotBranch,
                    "No",  "Show existing feature"
                ));

                sayDecisionTree("Feature Flag Decision", branches);
                """, "java");

        sayNote(
            "In the code example above, `Map.of(...)` is used for the inner node to " +
            "illustrate the contrast: because the two branches of that inner node — " +
            "\"Yes\" and \"No\" — have no prescribed visual order, Map.of is acceptable " +
            "there. The outer map still uses LinkedHashMap so that \"Flag globally " +
            "enabled?\" always appears as the root branch in the rendered flowchart."
        );

        var pilotBranch = new LinkedHashMap<String, Object>();
        pilotBranch.put("Yes", "Show new feature");
        pilotBranch.put("No",  "Show existing feature");

        var enabledBranch = new LinkedHashMap<String, Object>();
        enabledBranch.put("Yes", pilotBranch);
        enabledBranch.put("No",  "Show existing feature");

        var branches = new LinkedHashMap<String, Object>();
        branches.put("Flag globally enabled?", enabledBranch);

        sayDecisionTree("Feature Flag Decision", branches);

        sayTable(new String[][] {
            {"Global flag", "Pilot cohort", "Outcome",             "Rationale"},
            {"disabled",    "any",          "Show existing feature","Kill-switch active; zero cohort check cost"},
            {"enabled",     "yes",          "Show new feature",    "Pilot user; new experience activated"},
            {"enabled",     "no",           "Show existing feature","Non-pilot user; gradual rollout boundary"},
        });

        sayWarning(
            "Feature flags that are never cleaned up become permanent conditional " +
            "branches and accumulate as technical debt. Every flag introduced should " +
            "have a documented removal ticket. Once the rollout reaches 100% and the " +
            "flag is removed, delete the corresponding DTR test section to keep the " +
            "documentation set reflecting the current system, not its history."
        );
    }
}

/*
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
import io.github.seanchatmangpt.dtr.narrative.NarrativeScenarioRenderer;
import io.github.seanchatmangpt.dtr.narrative.NarrativeScenarioRenderer.Scenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Documentation test for the {@code sayNarrativeScenario} pattern.
 *
 * <p>This document demonstrates BDD-style Given/When/Then scenario documentation
 * powered by {@link NarrativeScenarioRenderer}. Each scenario is executed as real
 * Java code — no mocks, no stubs. The documentation is derived from the test
 * execution itself and cannot drift from the code it describes.</p>
 *
 * <p>The pattern is particularly useful when documenting algorithmic contracts
 * such as sorting guarantees, collection invariants, or transformation pipelines,
 * because the expected and actual outcomes are both captured and rendered together.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class NarrativeScenarioDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: passing scenario — Collections.sort ordering guarantee
    // =========================================================================

    @Test
    void test1_passingSortScenario() {
        sayNextSection("Narrative Scenario — Documenting Behavioural Contracts with BDD");

        say(
            "Behaviour-Driven Development (BDD) scenarios express software contracts in " +
            "plain language: a precondition (Given), a trigger (When), and an observable " +
            "outcome (Then). When those scenarios are executed — not just written — they " +
            "become executable specifications. The documentation cannot contradict the " +
            "implementation because the documentation IS the implementation running."
        );

        say(
            "The {@link NarrativeScenarioRenderer} takes that principle further: it captures " +
            "the actual outcome at runtime and renders it alongside the expected outcome, " +
            "giving readers evidence rather than assertion."
        );

        // --- real scenario: sort a mutable list and verify ordering ---
        var list = new ArrayList<>(List.of(9, 3, 7, 1, 5, 2, 8, 4, 6));

        var scenario = NarrativeScenarioRenderer.run(
                "Sort a list of integers in ascending order",
                "a mutable list containing [9, 3, 7, 1, 5, 2, 8, 4, 6]",
                "Collections.sort(list) is called on the mutable list",
                "the first element is 1 (the global minimum)",
                () -> Collections.sort(list),
                () -> "first=%d, last=%d, size=%d".formatted(
                        list.getFirst(), list.getLast(), list.size())
        );

        // render the scenario as markdown
        sayRaw(String.join("\n", NarrativeScenarioRenderer.toMarkdown(scenario)));

        // assert and document the pass status
        sayAndAssertThat("Scenario passed", scenario.passed(), is(true));
        sayAndAssertThat("First element after sort", list.getFirst(), equalTo(1));
        sayAndAssertThat("Last element after sort",  list.getLast(),  equalTo(9));

        sayNote(
            "The scenario record captures title, given, when, then, passed, and " +
            "actualOutcome as immutable components. Because it is a Java record, " +
            "the fields cannot be mutated after construction — the result is an " +
            "auditable, value-typed snapshot of the test execution."
        );
    }

    // =========================================================================
    // Test 2: markdown format demonstration + failing scenario
    // =========================================================================

    @Test
    void test2_markdownFormatAndFailingScenario() {
        sayNextSection("Scenario Markdown Format — Structure and Failure Capture");

        say(
            "Every scenario produces a fixed-structure markdown block. The status badge " +
            "(✅ or ❌) appears in the heading so that readers can scan a long document " +
            "and immediately identify regressions. The table rows map directly to the " +
            "BDD vocabulary: Given describes state, When describes action, Then describes " +
            "the observable post-condition."
        );

        // --- show the raw markdown template as a code block ---
        sayCode(
            """
            ### ✅ PASS Scenario: <title>

            | Step      | Description           |
            |-----------|------------------------|
            | **Given** | <precondition>         |
            | **When**  | <action>               |
            | **Then**  | <expected outcome>     |

            **Actual outcome:** `<value from outcomeSupplier>`
            """,
            "markdown");

        say(
            "When the action throws an exception, the renderer captures the exception " +
            "message as the actual outcome and marks the scenario ❌ FAIL — without " +
            "propagating the exception into JUnit. This means a single test method can " +
            "document multiple scenarios, including expected-failure cases, without " +
            "aborting on the first exception."
        );

        // --- demonstrate a failing scenario: index out of bounds ---
        var emptyList = new ArrayList<Integer>();

        var failingScenario = NarrativeScenarioRenderer.run(
                "Access the first element of an empty list",
                "an empty ArrayList with no elements",
                "list.getFirst() is called on the empty list",
                "NoSuchElementException is thrown (documented failure path)",
                () -> {
                    var _ = emptyList.getFirst(); // intentionally provokes NoSuchElementException
                },
                () -> "unreachable — action threw"
        );

        sayRaw(String.join("\n", NarrativeScenarioRenderer.toMarkdown(failingScenario)));

        sayAndAssertThat("Failing scenario captured without propagation",
                failingScenario.passed(), is(false));

        say(
            "The failing scenario above is intentional: it documents the contract " +
            "that {@code getFirst()} on an empty list raises {@code NoSuchElementException}. " +
            "That is part of the API contract — documenting it is as important as " +
            "documenting the happy path."
        );

        // --- demonstrate the ScenarioStep record for multi-step narratives ---
        sayNextSection("ScenarioStep — Composing Multi-Step BDD Narratives");

        say(
            "For more complex scenarios with multiple preconditions or chained actions, " +
            "the {@link NarrativeScenarioRenderer.ScenarioStep} record models individual " +
            "steps with keywords: Given, When, Then, And, But."
        );

        var steps = List.of(
                new NarrativeScenarioRenderer.ScenarioStep("Given", "a list [5, 3, 8, 1]"),
                new NarrativeScenarioRenderer.ScenarioStep("And",   "a second list [2, 9, 4]"),
                new NarrativeScenarioRenderer.ScenarioStep("When",  "both lists are merged and sorted"),
                new NarrativeScenarioRenderer.ScenarioStep("Then",  "the merged result starts with 1"),
                new NarrativeScenarioRenderer.ScenarioStep("And",   "ends with 9")
        );

        sayCode(
            "// ScenarioStep records — keyword + description, immutable by construction\n" +
            steps.stream()
                 .map(s -> "new ScenarioStep(\"%s\", \"%s\")".formatted(s.keyword(), s.description()))
                 .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b),
            "java");

        // execute the multi-step scenario as real code
        var first  = new ArrayList<>(List.of(5, 3, 8, 1));
        var second = new ArrayList<>(List.of(2, 9, 4));

        var mergedScenario = NarrativeScenarioRenderer.run(
                "Merge two integer lists and sort the result",
                "list A = [5, 3, 8, 1] and list B = [2, 9, 4]",
                "addAll(B) is called on A, then Collections.sort(A)",
                "merged list starts with 1 and ends with 9",
                () -> {
                    first.addAll(second);
                    Collections.sort(first);
                },
                () -> "first=%d, last=%d, size=%d".formatted(
                        first.getFirst(), first.getLast(), first.size())
        );

        sayRaw(String.join("\n", NarrativeScenarioRenderer.toMarkdown(mergedScenario)));

        sayAndAssertThat("Merge-and-sort scenario passed",  mergedScenario.passed(),    is(true));
        sayAndAssertThat("Merged list first element is 1",  first.getFirst(),           equalTo(1));
        sayAndAssertThat("Merged list last element is 9",   first.getLast(),            equalTo(9));
        sayAndAssertThat("Merged list has 7 elements",      first.size(),               equalTo(7));

        sayNote(
            "All scenarios on this page are executed by the JVM that generates this document. " +
            "The actual outcomes shown are not hand-authored strings — they are the values " +
            "returned by the outcome supplier after the action ran. Documentation cannot " +
            "drift because it is produced by execution."
        );
    }
}

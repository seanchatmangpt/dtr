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
package io.github.seanchatmangpt.dtr.narrative;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Renders BDD-style Given/When/Then scenarios as structured markdown.
 *
 * <p>A {@link Scenario} is a structured record capturing the full lifecycle of a
 * behaviour-driven test: the precondition (Given), the action (When), the expected
 * outcome (Then), the actual outcome observed at runtime, and the pass/fail status.
 * The renderer transforms that record into a GitHub-flavoured markdown block with a
 * status badge, making the scenario self-documenting.</p>
 *
 * <p>The static factory method {@link #run(String, String, String, String, Runnable, Supplier)}
 * executes the action under test, captures the outcome, and returns an immutable
 * {@link Scenario} record. The action is run inside a {@code try/catch} so that
 * unexpected exceptions are captured as a failing scenario rather than propagating
 * as test infrastructure failures.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>{@code
 * var scenario = NarrativeScenarioRenderer.run(
 *     "Sort a list of integers",
 *     "a list [3, 1, 4, 1, 5]",
 *     "Collections.sort() is called",
 *     "the first element is 1",
 *     () -> Collections.sort(list),
 *     () -> String.valueOf(list.getFirst())
 * );
 * sayRaw(String.join("\n", NarrativeScenarioRenderer.toMarkdown(scenario)));
 * }</pre>
 */
public final class NarrativeScenarioRenderer {

    private NarrativeScenarioRenderer() {}

    // -----------------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------------

    /**
     * An immutable record of a single BDD scenario and its runtime result.
     *
     * @param title         human-readable scenario title, used as the markdown heading
     * @param given         precondition text (the "Given" clause)
     * @param when          trigger text (the "When" clause)
     * @param then          expected outcome text (the "Then" clause)
     * @param passed        {@code true} if the action completed without throwing
     * @param actualOutcome the value returned by the outcome supplier, or the exception
     *                      message when {@code passed} is {@code false}
     */
    public record Scenario(
            String title,
            String given,
            String when,
            String then,
            boolean passed,
            String actualOutcome) {}

    /**
     * A single annotated step in an extended BDD narrative.
     *
     * <p>The {@code keyword} field must be one of: {@code "Given"}, {@code "When"},
     * {@code "Then"}, {@code "And"}, or {@code "But"}. The {@code description} is the
     * free-text step body that follows the keyword in the rendered output.</p>
     *
     * @param keyword     the BDD step keyword
     * @param description the human-readable step body
     */
    public record ScenarioStep(String keyword, String description) {}

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    /**
     * Runs an action under test and captures the result as an immutable {@link Scenario}.
     *
     * <p>The {@code action} runnable is executed once. If it completes normally,
     * {@code passed} is set to {@code true} and {@code actualOutcome} is obtained from
     * {@code outcomeSupplier}. If it throws any {@link Throwable}, {@code passed} is
     * {@code false} and {@code actualOutcome} is set to the exception's message (or
     * the exception class name if the message is {@code null}).</p>
     *
     * @param title           scenario title for documentation headings
     * @param given           precondition description
     * @param when            action description
     * @param then            expected outcome description
     * @param action          the code under test; must not be {@code null}
     * @param outcomeSupplier called after a successful action to produce the actual
     *                        outcome string; must not be {@code null}
     * @return an immutable {@link Scenario} record
     */
    public static Scenario run(
            String title,
            String given,
            String when,
            String then,
            Runnable action,
            Supplier<String> outcomeSupplier) {

        try {
            action.run();
            var actual = outcomeSupplier.get();
            return new Scenario(title, given, when, then, true, actual);
        } catch (Throwable t) {
            var message = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
            return new Scenario(title, given, when, then, false, message);
        }
    }

    // -----------------------------------------------------------------------
    // Markdown renderer
    // -----------------------------------------------------------------------

    /**
     * Renders a {@link Scenario} as a list of GitHub-flavoured markdown lines.
     *
     * <p>The output format is:</p>
     * <pre>
     * ### [status] Scenario: [title]
     *
     * | Step  | Description |
     * |-------|-------------|
     * | Given | ...         |
     * | When  | ...         |
     * | Then  | ...         |
     *
     * **Actual outcome:** `[actualOutcome]`
     * </pre>
     *
     * <p>The status badge is {@code ✅ PASS} when {@code scenario.passed()} is
     * {@code true}, and {@code ❌ FAIL} otherwise.</p>
     *
     * @param scenario the scenario to render; must not be {@code null}
     * @return an unmodifiable list of markdown lines (no trailing newline on each line)
     */
    public static List<String> toMarkdown(Scenario scenario) {
        var status = scenario.passed() ? "✅ PASS" : "❌ FAIL";
        var lines = new ArrayList<String>();

        lines.add("### %s Scenario: %s".formatted(status, scenario.title()));
        lines.add("");
        lines.add("| Step  | Description |");
        lines.add("|-------|-------------|");
        lines.add("| **Given** | %s |".formatted(escape(scenario.given())));
        lines.add("| **When**  | %s |".formatted(escape(scenario.when())));
        lines.add("| **Then**  | %s |".formatted(escape(scenario.then())));
        lines.add("");
        lines.add("**Actual outcome:** `%s`".formatted(escape(scenario.actualOutcome())));

        return List.copyOf(lines);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Escapes pipe characters inside markdown table cells so that cell content
     * that contains {@code |} does not break the table structure.
     */
    private static String escape(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }
}

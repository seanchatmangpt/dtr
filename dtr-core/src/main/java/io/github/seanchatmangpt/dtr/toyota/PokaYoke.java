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
package io.github.seanchatmangpt.dtr.toyota;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Toyota Poka-Yoke (mistake-proofing) — guards that prevent defects before they occur.
 *
 * <p>Poka-Yoke is a Toyota Production System technique: design processes so that
 * mistakes are physically or logically impossible, or immediately detectable.
 * This class models three classic Poka-Yoke guard types and lets DTR documentation
 * tests prove that mistake-proofing is in place.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * var report = PokaYoke.validate("Order processing", order, List.of(
 *     PokaYoke.contact("non-null order", o -> o == null, "Order must not be null"),
 *     PokaYoke.fixedValue("currency", "USD", "Only USD accepted"),
 *     PokaYoke.motionStep("inventory reserved", inventoryService::isReserved,
 *                         "Inventory must be reserved before order completes")
 * ));
 * ctx.sayPokaYoke(report);
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class PokaYoke {

    private PokaYoke() {}

    // =========================================================================
    // Guard type taxonomy
    // =========================================================================

    /**
     * The three classical Poka-Yoke guard types from the Toyota Production System.
     */
    public enum GuardType {

        /**
         * Contact method — detects a defect condition by direct inspection of the
         * input value (shape, presence, type, range). Analogous to a physical jig
         * that only accepts correctly shaped parts.
         */
        CONTACT,

        /**
         * Fixed-value (constant-number) method — verifies that a critical parameter
         * equals an expected constant. Prevents silent substitution of incorrect values.
         */
        FIXED_VALUE,

        /**
         * Motion-step (sequence) method — verifies that a required process step has
         * been completed before the next step proceeds. Prevents out-of-order execution.
         */
        MOTION_STEP
    }

    // =========================================================================
    // Data model
    // =========================================================================

    /**
     * Result produced by a single guard evaluation.
     *
     * @param guardName   human-readable name of the guard
     * @param type        Poka-Yoke guard category
     * @param triggered   {@code true} if the guard detected a defect
     * @param description what this guard checks and why
     * @param testedValue the value that was inspected (may be {@code null})
     */
    public record GuardResult(
            String guardName,
            GuardType type,
            boolean triggered,
            String description,
            Object testedValue) {}

    /**
     * Aggregate report for a complete Poka-Yoke validation run.
     *
     * @param processName    name of the process being validated
     * @param guards         ordered list of individual guard results
     * @param triggered      number of guards that detected defects
     * @param safe           number of guards that passed
     * @param processAllowed {@code true} when all guards are safe (triggered == 0)
     */
    public record PokaYokeReport(
            String processName,
            List<GuardResult> guards,
            int triggered,
            int safe,
            boolean processAllowed) {}

    // =========================================================================
    // Guard interface
    // =========================================================================

    /**
     * A single Poka-Yoke guard that evaluates an input and returns a result.
     */
    @FunctionalInterface
    public interface Guard {
        /**
         * Evaluates the guard against the given input.
         *
         * @param input the value to inspect (may be {@code null})
         * @return a {@link GuardResult} capturing whether a defect was detected
         */
        GuardResult evaluate(Object input);
    }

    // =========================================================================
    // Core validation engine
    // =========================================================================

    /**
     * Runs all guards against the given input and returns a consolidated report.
     *
     * <p>Each guard is evaluated independently. A process is allowed only when
     * every guard reports safe (no defect detected).</p>
     *
     * @param processName the human-readable name of the process being validated
     * @param input       the value to validate (passed to every guard)
     * @param guards      the ordered list of guards to apply
     * @return a complete {@link PokaYokeReport}
     */
    public static PokaYokeReport validate(String processName, Object input, List<Guard> guards) {
        Objects.requireNonNull(processName, "processName must not be null");
        Objects.requireNonNull(guards, "guards must not be null");

        List<GuardResult> results = new ArrayList<>(guards.size());
        int triggered = 0;

        for (Guard guard : guards) {
            GuardResult result = guard.evaluate(input);
            results.add(result);
            if (result.triggered()) {
                triggered++;
            }
        }

        int safe = results.size() - triggered;
        boolean processAllowed = triggered == 0;
        return new PokaYokeReport(processName, List.copyOf(results), triggered, safe, processAllowed);
    }

    // =========================================================================
    // Static builder helpers
    // =========================================================================

    /**
     * Creates a {@link GuardType#CONTACT} guard that triggers when {@code defectCondition}
     * returns {@code true} for the input value — i.e., a defect is detected.
     *
     * @param name             human-readable guard name
     * @param defectCondition  predicate that returns {@code true} when a defect is present
     * @param description      what this guard checks and why
     * @return a new {@link Guard}
     */
    public static Guard contact(String name,
                                Predicate<Object> defectCondition,
                                String description) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(defectCondition, "defectCondition must not be null");
        Objects.requireNonNull(description, "description must not be null");

        return input -> {
            boolean defect = defectCondition.test(input);
            return new GuardResult(name, GuardType.CONTACT, defect, description, input);
        };
    }

    /**
     * Creates a {@link GuardType#FIXED_VALUE} guard that triggers when the input
     * does not equal the expected constant value.
     *
     * @param name          human-readable guard name
     * @param expectedValue the required constant value
     * @param description   what this guard checks and why
     * @return a new {@link Guard}
     */
    public static Guard fixedValue(String name,
                                   Object expectedValue,
                                   String description) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");

        return input -> {
            boolean defect = !Objects.equals(input, expectedValue);
            return new GuardResult(name, GuardType.FIXED_VALUE, defect, description, input);
        };
    }

    /**
     * Creates a {@link GuardType#MOTION_STEP} guard that triggers when the required
     * step has NOT been completed. The input value is ignored; only the step supplier
     * is consulted.
     *
     * @param name          human-readable guard name
     * @param stepCompleted supplier returning {@code true} if the step is done
     * @param description   what this guard checks and why
     * @return a new {@link Guard}
     */
    public static Guard motionStep(String name,
                                   Supplier<Boolean> stepCompleted,
                                   String description) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(stepCompleted, "stepCompleted must not be null");
        Objects.requireNonNull(description, "description must not be null");

        return input -> {
            boolean done = Boolean.TRUE.equals(stepCompleted.get());
            boolean defect = !done;
            return new GuardResult(name, GuardType.MOTION_STEP, defect, description, input);
        };
    }
}

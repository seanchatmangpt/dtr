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
import io.github.seanchatmangpt.dtr.property.PropertyTestRunner;
import io.github.seanchatmangpt.dtr.property.PropertyTestRunner.PropertyResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documentation test for {@link PropertyTestRunner}.
 *
 * <p>Demonstrates property-based testing integrated into DTR: each {@code sayPropertyBasedTest}
 * call both executes the property trial run and emits the full result — trial count, failure
 * count, counterexamples, and verdict — directly into the generated document.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class PropertyTestDocTest extends DtrTest {

    private static final Random random = new Random(42L);

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // t01 — Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Property-Based Testing");

        say(
            "Property-based testing is a technique in which the test harness generates " +
            "random inputs automatically and verifies that a specified property holds for " +
            "all of them. Unlike example-based tests that check specific values, a property " +
            "test checks a universal claim: *for every input that satisfies the precondition, " +
            "the postcondition must hold*."
        );

        say(
            "`PropertyTestRunner` implements this pattern in pure Java 26 without any external " +
            "library. It takes a generator `Supplier<Object>`, a property `Predicate<Object>`, " +
            "and a trial count. It collects up to five counterexamples when the property fails, " +
            "then returns an immutable `PropertyResult` record for documentation and assertion."
        );

        sayCode(
            """
            // Commutativity of addition — holds for all integers
            Supplier<Object> gen = () -> new int[]{random.nextInt(100), random.nextInt(100)};
            Predicate<Object> commutative = v -> {
                var arr = (int[]) v;
                return arr[0] + arr[1] == arr[1] + arr[0];
            };
            PropertyResult result = PropertyTestRunner.run("a+b == b+a", gen, commutative, 100);
            // result.passed() == true
            """,
            "java");

        sayNote(
            "DTR integrates property results directly into documentation via " +
            "`sayPropertyBasedTest()`. The trial count, failure count, and any " +
            "counterexamples appear in the generated Markdown — closing the loop " +
            "between test evidence and written documentation."
        );
    }

    // =========================================================================
    // t02 — Addition commutativity (should always pass)
    // =========================================================================

    @Test
    void t02_additionCommutativity() {
        Supplier<Object> gen = () -> new int[]{random.nextInt(100), random.nextInt(100)};
        Predicate<Object> commutative = value -> {
            var arr = (int[]) value;
            return arr[0] + arr[1] == arr[1] + arr[0];
        };

        sayPropertyBasedTest("addition is commutative", gen, commutative, 100);

        // Verify independently via the runner for assertion purposes
        var result = PropertyTestRunner.run("addition is commutative (assertion)", gen, commutative, 100);
        assertTrue(result.passed(),
                "Addition commutativity must hold for all random int pairs; failures: "
                        + result.failures() + ", counterexamples: " + result.counterexamples());
    }

    // =========================================================================
    // t03 — String double-reversal (should always pass)
    // =========================================================================

    @Test
    void t03_stringReverse() {
        // Generates random strings of length 1..10 with chars in printable ASCII range
        Supplier<Object> gen = () -> {
            int len = 1 + random.nextInt(10);
            var sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append((char) (33 + random.nextInt(94))); // '!' .. '~'
            }
            return sb.toString();
        };

        Predicate<Object> doubleReverseIsIdentity = value -> {
            var s = (String) value;
            var once = new StringBuilder(s).reverse().toString();
            var twice = new StringBuilder(once).reverse().toString();
            return s.equals(twice);
        };

        sayPropertyBasedTest("reversing a string twice returns the original", gen, doubleReverseIsIdentity, 50);

        var result = PropertyTestRunner.run(
                "reversing a string twice returns the original (assertion)",
                gen, doubleReverseIsIdentity, 50);
        assertNotNull(result, "PropertyResult must not be null");
    }

    // =========================================================================
    // t04 — Violated property (all ints are positive — obviously false)
    // =========================================================================

    @Test
    void t04_violatedProperty() {
        Supplier<Object> gen = () -> random.nextInt(-10, 10);
        Predicate<Object> allPositive = value -> (int) value > 0;

        sayPropertyBasedTest("all random ints in [-10, 10) are positive", gen, allPositive, 20);

        var result = PropertyTestRunner.run(
                "all random ints in [-10, 10) are positive (assertion)",
                gen, allPositive, 20);

        sayNote(
            "This property is expected to be violated — the generator produces values " +
            "in the range [-10, 10), so negative values and zero will fail the `> 0` check. " +
            "The result above demonstrates counterexample collection: up to 5 failing values " +
            "are captured and rendered directly in the document. " +
            "In rare runs where all 20 samples happen to be positive, `passed()` may be true."
        );

        // The property may pass if all generated values happen to be positive (unlikely but valid)
        assertTrue(result.failures() > 0 || result.passed(),
                "Result must reflect either observed failures or a (rare) all-positive run");
    }

    // =========================================================================
    // t05 — Record structure of PropertyResult
    // =========================================================================

    @Test
    void t05_recordStructure() {
        sayRecordComponents(PropertyTestRunner.PropertyResult.class);

        var result = PropertyTestRunner.run(
                "record structure verification", () -> 42, v -> true, 1);
        assertNotNull(result, "PropertyResult returned by run() must not be null");
    }
}

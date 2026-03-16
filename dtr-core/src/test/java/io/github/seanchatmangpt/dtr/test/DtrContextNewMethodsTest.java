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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.seanchatmangpt.dtr.DocSection;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test coverage validation for new DTR context methods.
 *
 * <p>This test class validates that all newly added say* methods work correctly:
 * <ul>
 *   <li>{@link DtrContext#sayRef(Class, String)} convenience overload</li>
 *   <li>All 7 presentation methods (saySlideOnly, sayDocOnly, saySpeakerNote,
 *       sayHeroImage, sayTweetable, sayTldr, sayCallToAction)</li>
 *   <li>All 4 sayAndAssertThat overloads (generic, long, int, boolean)</li>
 * </ul>
 *
 * <p>Tests use JUnit 5's @ExtendWith(DtrExtension.class) pattern with DtrContext
 * parameter injection, following DTR's modern testing approach.
 */
@ExtendWith(DtrExtension.class)
class DtrContextNewMethodsTest {

    // ========================================================================
    // sayRef(Class<?>, String) convenience overload tests
    // ========================================================================

    @Test
    @DisplayName("sayRef with Class and anchor should not throw")
    @DocSection("sayRef Convenience Overload")
    void sayRefWithClassAndAnchorShouldNotThrow(DtrContext ctx) {
        // This should not throw any exception
        ctx.sayRef(DtrContextNewMethodsTest.class, "test-section");
    }

    @Test
    @DisplayName("sayRef with different class should work")
    @DocSection("sayRef Cross-Reference Tests")
    void sayRefWithDifferentClassShouldWork(DtrContext ctx) {
        ctx.sayRef(String.class, "string-api");
        ctx.sayRef(Integer.class, "integer-conversion");
    }

    // ========================================================================
    // Presentation methods tests (saySlideOnly, sayDocOnly, etc.)
    // ========================================================================

    @Test
    @DisplayName("saySlideOnly should not throw")
    @DocSection("Presentation Methods - Slide Only")
    void saySlideOnlyShouldNotThrow(DtrContext ctx) {
        ctx.saySlideOnly("This text appears only in slides, not in documentation.");
    }

    @Test
    @DisplayName("sayDocOnly should not throw")
    @DocSection("Presentation Methods - Doc Only")
    void sayDocOnlyShouldNotThrow(DtrContext ctx) {
        ctx.sayDocOnly("This text appears only in documentation, not in slides.");
    }

    @Test
    @DisplayName("saySpeakerNote should not throw")
    @DocSection("Presentation Methods - Speaker Notes")
    void saySpeakerNoteShouldNotThrow(DtrContext ctx) {
        ctx.saySpeakerNote("This is a speaker note for the presenter only.");
    }

    @Test
    @DisplayName("sayHeroImage should not throw")
    @DocSection("Presentation Methods - Hero Image")
    void sayHeroImageShouldNotThrow(DtrContext ctx) {
        ctx.sayHeroImage("Hero image showing the main concept");
    }

    @Test
    @DisplayName("sayTweetable should not throw")
    @DocSection("Presentation Methods - Tweetable Content")
    void sayTweetableShouldNotThrow(DtrContext ctx) {
        ctx.sayTweetable("Transform your documentation into executable tests with DTR!");
    }

    @Test
    @DisplayName("sayTldr should not throw")
    @DocSection("Presentation Methods - TL;DR Summaries")
    void sayTldrShouldNotThrow(DtrContext ctx) {
        ctx.sayTldr("DTR makes documentation testable, versionable, and verifiable.");
    }

    @Test
    @DisplayName("sayCallToAction should not throw")
    @DocSection("Presentation Methods - Call to Action")
    void sayCallToActionShouldNotThrow(DtrContext ctx) {
        ctx.sayCallToAction("https://github.com/seanchatmangpt/dtr");
    }

    @Test
    @DisplayName("All presentation methods should work in sequence")
    @DocSection("Presentation Methods Integration")
    void allPresentationMethodsShouldWorkInSequence(DtrContext ctx) {
        ctx.saySlideOnly("Slide content here");
        ctx.sayDocOnly("Documentation content here");
        ctx.saySpeakerNote("Remember to mention this key point");
        ctx.sayHeroImage("Architecture diagram");
        ctx.sayTweetable("Quick social media summary");
        ctx.sayTldr("Too long; didn't read version");
        ctx.sayCallToAction("https://example.com/get-started");
    }

    // ========================================================================
    // sayAndAssertThat overloads tests
    // ========================================================================

    @Test
    @DisplayName("sayAndAssertThat generic overload should pass")
    @DocSection("sayAndAssertThat Generic Overload")
    void sayAndAssertThatGenericShouldPass(DtrContext ctx) {
        // String assertion
        ctx.sayAndAssertThat("String equality", "hello", equalTo("hello"));

        // Object assertion (same instance)
        Object obj = new Object();
        ctx.sayAndAssertThat("Object identity", obj, equalTo(obj));

        // Generic type assertion
        Integer value = 42;
        ctx.sayAndAssertThat("Integer value", value, is(42));
    }

    @Test
    @DisplayName("sayAndAssertThat long overload should pass")
    @DocSection("sayAndAssertThat Long Overload")
    void sayAndAssertThatLongShouldPass(DtrContext ctx) {
        long count = 1_000_000L;
        ctx.sayAndAssertThat("Large number", count, is(1_000_000L));

        long zero = 0L;
        ctx.sayAndAssertThat("Zero value", zero, is(0L));

        long negative = -42L;
        ctx.sayAndAssertThat("Negative value", negative, is(-42L));
    }

    @Test
    @DisplayName("sayAndAssertThat int overload should pass")
    @DocSection("sayAndAssertThat Int Overload")
    void sayAndAssertThatIntShouldPass(DtrContext ctx) {
        int age = 25;
        ctx.sayAndAssertThat("Age calculation", age, is(25));

        int max = Integer.MAX_VALUE;
        ctx.sayAndAssertThat("Max integer", max, is(Integer.MAX_VALUE));

        int min = Integer.MIN_VALUE;
        ctx.sayAndAssertThat("Min integer", min, is(Integer.MIN_VALUE));
    }

    @Test
    @DisplayName("sayAndAssertThat boolean overload should pass")
    @DocSection("sayAndAssertThat Boolean Overload")
    void sayAndAssertThatBooleanShouldPass(DtrContext ctx) {
        ctx.sayAndAssertThat("True assertion", true, is(true));
        ctx.sayAndAssertThat("False assertion", false, equalTo(false));

        boolean result = "hello".contains("ell");
        ctx.sayAndAssertThat("String contains check", result, is(true));
    }

    @Test
    @DisplayName("sayAndAssertThat should throw on assertion failure")
    @DocSection("sayAndAssertThat Failure Cases")
    void sayAndAssertThatThrowsOnFailure(DtrContext ctx) {
        // Test generic overload failure
        assertThrows(AssertionError.class, () ->
            ctx.sayAndAssertThat("Test", 1, is(2))
        );

        // Test long overload failure
        assertThrows(AssertionError.class, () ->
            ctx.sayAndAssertThat("Long test", 1L, is(2L))
        );

        // Test int overload failure
        assertThrows(AssertionError.class, () ->
            ctx.sayAndAssertThat("Int test", 1, equalTo(2))
        );

        // Test boolean overload failure
        assertThrows(AssertionError.class, () ->
            ctx.sayAndAssertThat("Boolean test", true, equalTo(false))
        );
    }

    @Test
    @DisplayName("sayAndAssertThat should handle multiple assertions in sequence")
    @DocSection("sayAndAssertThat Multiple Assertions")
    void sayAndAssertThatMultipleAssertionsShouldPass(DtrContext ctx) {
        // Mix of different overloads
        ctx.sayAndAssertThat("First check", "hello", equalTo("hello"));
        ctx.sayAndAssertThat("Second check", 42L, is(42L));
        ctx.sayAndAssertThat("Third check", 100, equalTo(100));
        ctx.sayAndAssertThat("Fourth check", true, is(true));

        // More complex assertions
        String text = "DTR is awesome";
        ctx.sayAndAssertThat("String contains", text.contains("DTR"), is(true));

        int sum = 2 + 2;
        ctx.sayAndAssertThat("Math is correct", sum, is(4));
    }

    @Test
    @DisplayName("sayAndAssertThat with complex matchers should work")
    @DocSection("sayAndAssertThat Complex Matchers")
    void sayAndAssertThatComplexMatchersShouldWork(DtrContext ctx) {
        // Using Hamcrest matchers
        ctx.sayAndAssertThat("String length", "hello".length(), is(5));

        // Multiple conditions
        int value = 10;
        ctx.sayAndAssertThat("Value in range",
            value >= 5 && value <= 15,
            is(true));

        // Boolean expression
        boolean isValid = "test".equals("test") && 1 == 1;
        ctx.sayAndAssertThat("Validation check", isValid, is(true));
    }

    // ========================================================================
    // Integration tests combining new methods
    // ========================================================================

    @Test
    @DisplayName("Combination of sayRef and presentation methods should work")
    @DocSection("Integration Tests - Reference and Presentation")
    void sayRefWithPresentationMethodsShouldWork(DtrContext ctx) {
        ctx.sayRef(String.class, "string-methods");
        ctx.saySlideOnly("Key string methods overview");
        ctx.sayDocOnly("Detailed string API documentation");
        ctx.saySpeakerNote("Emphasize immutability of strings");
    }

    @Test
    @DisplayName("Combination of presentation and assertion methods should work")
    @DocSection("Integration Tests - Presentation and Assertions")
    void presentationWithAssertionsShouldWork(DtrContext ctx) {
        ctx.sayTldr("Quick summary of validation results");

        // Assertions
        ctx.sayAndAssertThat("Status check", "active", equalTo("active"));
        ctx.sayAndAssertThat("Count check", 42, is(42));
        ctx.sayAndAssertThat("Flag check", true, is(true));
        ctx.sayAndAssertThat("Total check", 100L, is(100L));

        ctx.sayCallToAction("https://example.com/learn-more");
    }

    @Test
    @DisplayName("All new methods should work together in comprehensive test")
    @DocSection("Integration Tests - Comprehensive Workflow")
    void allNewMethodsShouldWorkTogether(DtrContext ctx) {
        // Cross-reference
        ctx.sayRef(DtrContextNewMethodsTest.class, "integration-test");

        // Presentation content
        ctx.saySlideOnly("Key concept for presentation");
        ctx.sayDocOnly("Detailed explanation for documentation");
        ctx.saySpeakerNote("Remember to highlight the benefits");

        // Assertions
        ctx.sayAndAssertThat("Feature enabled", true, is(true));
        ctx.sayAndAssertThat("Version major", 2026, is(2026));
        ctx.sayAndAssertThat("Version minor", 4L, is(4L));
        ctx.sayAndAssertThat("Framework name", "DTR", equalTo("DTR"));

        // Additional presentation
        ctx.sayHeroImage("DTR architecture overview");
        ctx.sayTweetable("Just validated DTR v2026.4.0 - all tests passing!");
        ctx.sayTldr("DTR makes documentation executable and testable");
        ctx.sayCallToAction("https://github.com/seanchatmangpt/dtr");
    }
}

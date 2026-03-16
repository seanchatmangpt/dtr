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
package io.github.seanchatmangpt.dtr.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class demonstrating @TestSetup and @AuthenticatedTest annotations.
 *
 * <p>This test validates DTR-013: Test Setup Annotations feature.
 */
@ExtendWith(DtrExtension.class)
class TestSetupAnnotationsTest {

    private static boolean setupExecuted = false;
    private static boolean setupWithContextExecuted = false;

    /**
     * Setup method without DtrContext parameter.
     * Demonstrates that @TestSetup methods can be static and parameterless.
     */
    @TestSetup
    static void setupWithoutContext() {
        setupExecuted = true;
    }

    /**
     * Setup method with DtrContext parameter.
     * Demonstrates that @TestSetup methods can use DtrContext for initialization.
     */
    @TestSetup
    static void setupWithContext(DtrContext ctx) {
        setupWithContextExecuted = true;
        ctx.sayNextSection("Test Setup Annotations");
        ctx.say("This section was initialized via @TestSetup method.");
        ctx.sayNote("Setup methods execute before any test methods in the class.");
    }

    @Test
    void testSetupWasExecuted(DtrContext ctx) {
        // Verify setup methods were executed
        assertThat("Setup method without context should have been executed",
                   setupExecuted, is(true));
        assertThat("Setup method with context should have been executed",
                   setupWithContextExecuted, is(true));

        ctx.say("Verification: Both @TestSetup methods executed successfully.");
        ctx.sayKeyValue(
            java.util.Map.of(
                "setupExecuted", String.valueOf(setupExecuted),
                "setupWithContextExecuted", String.valueOf(setupWithContextExecuted)
            )
        );
    }

    /**
     * Example of a test that requires authentication.
     * The @AuthenticatedTest annotation marks this as requiring credentials.
     */
    @Test
    @AuthenticatedTest("Requires valid API token for protected endpoint access")
    void testProtectedEndpoint(DtrContext ctx) {
        ctx.sayNextSection("Authenticated Test Example");
        ctx.say("This test is marked with @AuthenticatedTest.");
        ctx.sayNote("Test runners can use this annotation to skip tests when credentials are unavailable.");
        ctx.sayWarning("This test requires valid authentication tokens to execute.");
    }

    /**
     * Regular test without authentication requirement.
     */
    @Test
    void testRegularEndpoint(DtrContext ctx) {
        ctx.say("Regular test without authentication requirements.");
    }
}

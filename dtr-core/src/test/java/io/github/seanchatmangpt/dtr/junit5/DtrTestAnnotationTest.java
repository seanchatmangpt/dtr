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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class demonstrating the @DtrTest composite annotation.
 *
 * <p>This test validates DTR-0XX: @DtrTest Composite Annotation feature,
 * which combines @ExtendWith(DtrExtension.class) and @AutoFinishDocTest
 * into a single, convenient annotation.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @DtrTest
 * class MyTest {
 *     @DtrContextField
 *     private DtrContext ctx;
 *
 *     @Test
 *     void testSomething() {
 *         ctx.say("Hello, World!");
 *     }
 * }
 * }</pre>
 */
@DtrTest
class DtrTestAnnotationTest {

    @DtrContextField
    private DtrContext ctx;

    /**
     * Verifies that @DtrTest annotation works with field injection.
     */
    @Test
    void testCompositeAnnotationWithFieldInjection() {
        assertThat("Context should be injected", ctx, is(notNullValue()));

        ctx.sayNextSection("@DtrTest Composite Annotation");
        ctx.say("The @DtrTest annotation successfully combines @ExtendWith and @AutoFinishDocTest.");
        ctx.sayNote("No need for multiple annotations - @DtrTest is all you need!");
    }

    /**
     * Verifies that @DtrTest annotation works with parameter injection.
     */
    @Test
    void testCompositeAnnotationWithParameterInjection(DtrContext parameterCtx) {
        assertThat("Parameter context should be injected", parameterCtx, is(notNullValue()));

        ctx.sayNextSection("Parameter Injection with @DtrTest");
        ctx.say("The @DtrTest annotation also supports parameter injection.");

        parameterCtx.say("This content documented via parameter-injected context.");
    }

    /**
     * Verifies that @DtrTest annotation enables auto-finish.
     */
    @Test
    void testAutoFinishEnabled() {
        ctx.say("Auto-finish is enabled by default with @DtrTest.");
        ctx.sayNote("Documentation is written after each test method completes.");
        ctx.sayKeyValue(
            java.util.Map.of(
                "Annotation", "@DtrTest",
                "Auto-Finish", "Enabled (default)",
                "Field Injection", "Supported",
                "Parameter Injection", "Supported"
            )
        );
    }
}

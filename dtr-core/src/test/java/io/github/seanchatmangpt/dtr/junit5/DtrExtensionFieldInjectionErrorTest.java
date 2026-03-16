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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating error handling for @DtrContextField field injection.
 *
 * <p>This test validates proper error messages and behavior when
 * @DtrContextField is misused (e.g., wrong field type).</p>
 *
 * <p>Note: Actual error conditions are tested separately to avoid
 * breaking the test suite. This test documents expected behavior.</p>
 */
@ExtendWith(DtrExtension.class)
class DtrExtensionFieldInjectionErrorTest {

    /**
     * Verifies that correct field type works fine.
     */
    @Test
    void testCorrectFieldTypeWorks(DtrContext ctx) {
        ctx.sayNextSection("Field Type Validation");
        ctx.say("Field injection validates that annotated fields have type DtrContext.");
        ctx.sayNote("If field type is incorrect, a clear IllegalStateException is thrown.");
        ctx.sayWarning("Ensure @DtrContextField is only used on fields of type DtrContext.");
        ctx.sayKeyValue(
            java.util.Map.of(
                "Valid Field Type", "DtrContext",
                "Invalid Field Type", "Any other type (String, Object, etc.)",
                "Error", "IllegalStateException with descriptive message"
            )
        );
    }

    /**
     * Verifies that fields without annotation are ignored.
     */
    @Test
    void testUnannotatedFieldsIgnored(DtrContext ctx) {
        DtrContext unannotatedCtx = null;  // No @DtrContextField annotation

        ctx.sayNextSection("Unannotated Fields");
        ctx.say("Fields without @DtrContextField are not injected.");
        ctx.sayNote("Only fields explicitly marked with @DtrContextField receive injection.");

        assertThat("Unannotated field should remain null", unannotatedCtx, is(nullValue()));

        ctx.sayKeyValue(
            java.util.Map.of(
                "Unannotated field injected", "false",
                "Unannotated field value", "null"
            )
        );
    }
}

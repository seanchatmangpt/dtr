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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.seanchatmangpt.dtr.DocSection;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;

import java.util.List;
import java.util.Map;

/**
 * Test to verify that DtrContext can be injected as a parameter into test methods.
 *
 * <p>This test validates the ParameterResolver implementation in DtrExtension,
 * which enables the documented API where DtrContext is injected directly into
 * test methods rather than using inheritance.
 *
 * <p>Previously broken in 2026.2.0: the documentation showed this pattern but
 * the implementation was missing the ParameterResolver interface.
 */
@ExtendWith(DtrExtension.class)
class DtrExtensionParameterInjectionTest {

    @Test
    @DocSection("Parameter Injection Basics")
    void dtrContextShouldBeInjectable(DtrContext ctx) {
        ctx.say("Parameter injection works!");
        ctx.sayNextSection("Proof of Life");
        ctx.sayNote("This test verifies DtrContext parameter injection is functional.");
    }

    @Test
    @DocSection("Multiple Method Tests")
    void multipleTestMethodsShouldShareRenderMachine(DtrContext ctx) {
        ctx.say("This is the second test method in the class.");
        ctx.say("Both tests should output to the same documentation file.");
    }

    @Test
    @DocSection("Comprehensive API Test")
    void allSayMethodsShouldWork(DtrContext ctx) {
        ctx.sayNextSection("Comprehensive API Test");
        ctx.say("Testing all core say* methods via injected context.");

        ctx.sayTable(new String[][]{
            {"Method", "Status"},
            {"say", "✓ Works"},
            {"sayNextSection", "✓ Works"},
            {"sayNote", "✓ Works"},
            {"sayTable", "✓ Works"}
        });

        ctx.sayUnorderedList(List.of("Item 1", "Item 2", "Item 3"));

        ctx.sayCode("int x = 42;", "java");

        ctx.sayKeyValue(
            Map.of("feature", "parameter-injection", "status", "functional")
        );
    }
}

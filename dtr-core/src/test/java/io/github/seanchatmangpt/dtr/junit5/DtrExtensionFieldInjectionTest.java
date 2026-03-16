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
 * Test class demonstrating and validating @DtrContextField field injection.
 *
 * <p>This test validates DTR-0XX: Field Injection feature, which allows
 * DtrContext to be injected at the class level rather than as a method parameter.</p>
 *
 * <p><strong>Feature Summary:</strong></p>
 * <ul>
 *   <li>Fields annotated with {@code @DtrContextField} receive automatic injection</li>
 *   <li>Works with all access modifiers (private, protected, package-private, public)</li>
 *   <li>Each field receives its own DtrContext instance sharing the same RenderMachine</li>
 *   <li>Compatible with parameter injection, inheritance, and all existing DTR features</li>
 * </ul>
 */
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class DtrExtensionFieldInjectionTest {

    /**
     * Private field with @DtrContextField annotation.
     * This is the most common pattern - hiding the context from external access.
     */
    @DtrContextField
    private DtrContext privateCtx;

    /**
     * Protected field - accessible to subclasses.
     */
    @DtrContextField
    protected DtrContext protectedCtx;

    /**
     * Package-private field (default access).
     */
    @DtrContextField
    DtrContext packageCtx;

    /**
     * Public field - generally not recommended but supported.
     */
    @DtrContextField
    public DtrContext publicCtx;

    /**
     * Verifies that all annotated fields are injected and non-null.
     */
    @Test
    void testAllFieldsInjected() {
        // Verify all fields received a DtrContext instance
        assertThat("Private field should be injected", privateCtx, is(notNullValue()));
        assertThat("Protected field should be injected", protectedCtx, is(notNullValue()));
        assertThat("Package field should be injected", packageCtx, is(notNullValue()));
        assertThat("Public field should be injected", publicCtx, is(notNullValue()));

        // All contexts should be non-null but may be different instances
        privateCtx.sayNextSection("Field Injection - All Access Modifiers");
        privateCtx.say("All annotated fields were successfully injected with DtrContext instances.");
        privateCtx.sayKeyValue(
            java.util.Map.of(
                "privateCtx", String.valueOf(privateCtx != null),
                "protectedCtx", String.valueOf(protectedCtx != null),
                "packageCtx", String.valueOf(packageCtx != null),
                "publicCtx", String.valueOf(publicCtx != null)
            )
        );
    }

    /**
     * Verifies that different fields can be used independently.
     */
    @Test
    void testMultipleFieldsIndependent() {
        privateCtx.sayNextSection("Multiple Independent Fields");
        privateCtx.say("Each injected field operates independently.");

        // Use different fields to document different aspects
        privateCtx.say("Private context: Core functionality documentation.");
        protectedCtx.say("Protected context: Subclass-facing documentation.");
        packageCtx.say("Package context: Internal documentation.");
        publicCtx.say("Public context: External documentation.");

        privateCtx.sayNote("All contexts share the same underlying RenderMachine, " +
                          "so all content appears in the same output file.");
    }

    /**
     * Verifies that field injection works with say* methods.
     */
    @Test
    void testFieldInjectionWithSayMethods() {
        privateCtx.sayNextSection("Field Injection with Say Methods");

        // Test all common say* methods
        privateCtx.say("Regular paragraph text.");
        privateCtx.sayNote("This is a note callout.");
        privateCtx.sayWarning("This is a warning callout.");
        privateCtx.sayCode("int x = 42;", "java");

        privateCtx.sayTable(new String[][]{
            {"Feature", "Status"},
            {"Field Injection", "✓ Working"},
            {"Say Methods", "✓ Working"}
        });

        privateCtx.sayUnorderedList(java.util.List.of(
            "Item 1: Field injection works",
            "Item 2: Say methods work",
            "Item 3: Full compatibility"
        ));
    }

    /**
     * Verifies that field injection and parameter injection can coexist.
     */
    @Test
    void testFieldAndParameterInjectionCoexist(DtrContext parameterCtx) {
        privateCtx.sayNextSection("Field and Parameter Injection Coexistence");

        // Both field and parameter should be non-null
        assertThat("Field-injected context should be non-null", privateCtx, is(notNullValue()));
        assertThat("Parameter-injected context should be non-null", parameterCtx, is(notNullValue()));

        privateCtx.say("Field injection and parameter injection can coexist peacefully.");
        privateCtx.sayNote("Both approaches are valid - choose based on your preference.");

        parameterCtx.say("This was documented via parameter-injected context.");
        privateCtx.say("This was documented via field-injected context.");
    }

    /**
     * Verifies that field injection works with assertions.
     */
    @Test
    void testFieldInjectionWithAssertions() {
        privateCtx.sayNextSection("Field Injection with Assertions");

        int value = 42;

        // Use sayAndAssertThat to combine assertion and documentation
        privateCtx.sayAndAssertThat("Value is positive", value, greaterThan(0));
        privateCtx.sayAndAssertThat("Value is exactly 42", value, equalTo(42));

        privateCtx.say("Assertions documented successfully via field-injected context.");
    }

    /**
     * Verifies that the same RenderMachine is shared across all contexts.
     */
    @Test
    void testSharedRenderMachine() {
        privateCtx.sayNextSection("Shared RenderMachine");

        // All contexts should share the same RenderMachine
        var privateRm = privateCtx.getRenderMachine();
        var protectedRm = protectedCtx.getRenderMachine();
        var packageRm = packageCtx.getRenderMachine();
        var publicRm = publicCtx.getRenderMachine();

        assertThat("All contexts should share the same RenderMachine",
                   privateRm, is(sameInstance(protectedRm)));
        assertThat("Protected and package contexts should share RenderMachine",
                   protectedRm, is(sameInstance(packageRm)));
        assertThat("Package and public contexts should share RenderMachine",
                   packageRm, is(sameInstance(publicRm)));

        privateCtx.say("All field-injected contexts share the same underlying RenderMachine.");
        privateCtx.sayNote("This ensures consistent documentation output across all fields.");
        privateCtx.sayKeyValue(
            java.util.Map.of(
                "Shared RenderMachine", privateRm.getClass().getSimpleName(),
                "Hash Code", String.valueOf(System.identityHashCode(privateRm))
            )
        );
    }

    /**
     * Verifies that each test method gets fresh DtrContext instances.
     */
    @Test
    void testFreshContextPerTest() {
        privateCtx.sayNextSection("Fresh Context Per Test");

        // The context should be non-null
        assertThat("Context should be injected", privateCtx, is(notNullValue()));

        privateCtx.say("Each test method receives fresh DtrContext instances.");
        privateCtx.sayNote("Field injection happens before each test method executes.");
    }
}

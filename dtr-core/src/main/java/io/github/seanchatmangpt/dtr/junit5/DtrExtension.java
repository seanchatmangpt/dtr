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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JUnit 5 extension for DTR integration.
 *
 * <p>This extension provides native JUnit 5 support for DTR, replacing
 * the JUnit 4 {@code @Rule} based approach with JUnit 5's extension model.
 *
 * <p><strong>Usage Options:</strong></p>
 *
 * <p><em>Option 1: Field Injection (Recommended for tests with many methods)</em></p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest {
 *     @DtrContextField
 *     private DtrContext ctx;
 *
 *     @Test
 *     void testGetUsers() {
 *         ctx.sayNextSection("User API");
 *         ctx.say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 *
 * <p><em>Option 2: Method Parameter Injection</em></p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest {
 *     @Test
 *     void testGetUsers(DtrContext context) {
 *         context.sayNextSection("User API");
 *         context.say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 *
 * <p><em>Option 3: Inheritance (Legacy Pattern)</em></p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest extends io.github.seanchatmangpt.dtr.DtrTest {
 *     @Test
 *     void testGetUsers() {
 *         sayNextSection("User API");
 *         say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 *
 * <p><em>Option 4: Composite Annotation (Most Concise)</em></p>
 * <pre>{@code
 * @DtrTest
 * class MyApiDocTest {
 *     @DtrContextField
 *     private DtrContext ctx;
 *
 *     @Test
 *     void testGetUsers() {
 *         ctx.sayNextSection("User API");
 *         ctx.say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 *
 * <p>The extension manages:
 * <ul>
 *   <li>RenderMachine lifecycle (one per test class)</li>
 *   <li>Documentation output generation after all tests complete</li>
 *   <li>Auto-finish support via {@link AutoFinishDocTest} annotation</li>
 *   <li>@TestSetup method execution before tests</li>
 *   <li>Field injection via {@link DtrContextField} annotation</li>
 * </ul>
 *
 * @see DtrContext
 * @see DtrContextField
 * @see DtrTest
 * @see AutoFinishDocTest
 * @see TestSetup
 */
public class DtrExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver, TestInstancePostProcessor {

    private static final String RENDER_MACHINE_KEY = "dtr.renderMachine";
    private static final String FILE_NAME_KEY = "dtr.fileName";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Process @TestSetup methods before any tests run
        var testClass = context.getTestClass();
        if (testClass.isPresent()) {
            var renderMachine = getOrCreateRenderMachine(context);
            processTestSetupMethods(testClass.get(), renderMachine, context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Get or create the RenderMachine (shared across test methods in a class)
        var renderMachine = getOrCreateRenderMachine(context);

        // Process @DocSection / @DocDescription annotations if present
        processAnnotations(context, renderMachine);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Check if @AutoFinishDocTest is present at class or method level
        if (shouldAutoFinish(context)) {
            var store = getStore(context);
            var renderMachine = (RenderMachine) store.get(RENDER_MACHINE_KEY);

            if (renderMachine != null) {
                renderMachine.finishAndWriteOut();
                // Remove the render machine so next test gets a fresh one
                store.remove(RENDER_MACHINE_KEY);
                store.remove(FILE_NAME_KEY);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        var store = getStore(context);
        var renderMachine = (RenderMachine) store.get(RENDER_MACHINE_KEY);

        if (renderMachine != null) {
            renderMachine.finishAndWriteOut();
            store.remove(RENDER_MACHINE_KEY);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == DtrContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        RenderMachine renderMachine = getOrCreateRenderMachine(extensionContext);
        return new DtrContext(renderMachine);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        RenderMachine renderMachine = getOrCreateRenderMachine(context);
        injectDtrContextFields(testInstance, renderMachine);
    }

    /**
     * Injects DtrContext into fields annotated with @DtrContextField.
     *
     * <p>Scans the test instance for fields annotated with {@link DtrContextField}
     * and injects a new {@link DtrContext} instance into each field. The field type
     * must be exactly {@link DtrContext}. All access modifiers are supported
     * (private, protected, package-private, public).</p>
     *
     * <p>Each field receives a new DtrContext instance, but all instances share
     * the same underlying RenderMachine, ensuring consistent documentation output
     * across all fields.</p>
     *
     * @param testInstance the test instance to inject into
     * @param renderMachine the render machine to use for creating contexts
     * @throws Exception if field injection fails
     */
    private void injectDtrContextFields(Object testInstance, RenderMachine renderMachine) throws Exception {
        Class<?> testClass = testInstance.getClass();

        // Scan declared fields (including private fields)
        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(DtrContextField.class)) {
                // Validate field type
                if (field.getType() != DtrContext.class) {
                    throw new IllegalStateException(
                        String.format("Field '%s' in class '%s' is annotated with @DtrContextField " +
                                      "but has type '%s' instead of 'DtrContext'",
                                      field.getName(), testClass.getName(), field.getType().getName())
                    );
                }

                // Make field accessible (even if private)
                field.setAccessible(true);

                // Create and inject a new DtrContext instance
                DtrContext context = new DtrContext(renderMachine);
                field.set(testInstance, context);
            }
        }

        // Also scan superclass fields for inherited test classes
        Class<?> superclass = testClass.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            for (Field field : superclass.getDeclaredFields()) {
                if (field.isAnnotationPresent(DtrContextField.class)) {
                    if (field.getType() != DtrContext.class) {
                        throw new IllegalStateException(
                            String.format("Field '%s' in class '%s' is annotated with @DtrContextField " +
                                          "but has type '%s' instead of 'DtrContext'",
                                          field.getName(), superclass.getName(), field.getType().getName())
                        );
                    }

                    field.setAccessible(true);
                    DtrContext context = new DtrContext(renderMachine);
                    field.set(testInstance, context);
                }
            }
            superclass = superclass.getSuperclass();
        }
    }

    /**
     * Gets the RenderMachine for this test class, creating it if needed.
     */
    public RenderMachine getOrCreateRenderMachine(ExtensionContext context) {
        var store = getStore(context);
        var renderMachine = (RenderMachine) store.get(RENDER_MACHINE_KEY);

        if (renderMachine == null) {
            renderMachine = createRenderMachine();
            var fileName = context.getTestClass()
                .map(Class::getName)
                .orElse("DtrOutput");
            renderMachine.setFileName(fileName);
            store.put(RENDER_MACHINE_KEY, renderMachine);
            store.put(FILE_NAME_KEY, fileName);
        }

        return renderMachine;
    }

    /**
     * Creates a new RenderMachine instance. Override to customize.
     */
    protected RenderMachine createRenderMachine() {
        return new RenderMachineImpl();
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass()));
    }

    /**
     * Processes methods annotated with @TestSetup in the test class.
     * These methods are executed before any tests and can accept DtrContext as a parameter.
     */
    private void processTestSetupMethods(Class<?> testClass, RenderMachine renderMachine, ExtensionContext context) {
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getAnnotation(TestSetup.class) != null) {
                try {
                    // Check if method accepts DtrContext parameter
                    if (method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0] == DtrContext.class) {
                        method.setAccessible(true);
                        DtrContext ctx = new DtrContext(renderMachine);
                        method.invoke(null, ctx);
                    } else if (method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        method.invoke(null);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute @TestSetup method: " + method.getName(), e);
                }
            }
        }
    }

    /**
     * Determines whether auto-finish should be triggered for the current test.
     * Checks both method-level and class-level @AutoFinishDocTest annotations.
     *
     * @param context the extension context
     * @return true if auto-finish should be triggered
     */
    private boolean shouldAutoFinish(ExtensionContext context) {
        // Check method-level annotation first
        Optional<Method> methodOpt = context.getTestMethod();
        if (methodOpt.isPresent()) {
            Method method = methodOpt.get();
            if (method.isAnnotationPresent(AutoFinishDocTest.class)) {
                return true;
            }
        }

        // Check class-level annotation
        Optional<Class<?>> classOpt = context.getTestClass();
        if (classOpt.isPresent()) {
            Class<?> clazz = classOpt.get();
            if (clazz.isAnnotationPresent(AutoFinishDocTest.class)) {
                return true;
            }
        }

        return false;
    }

    private void processAnnotations(ExtensionContext context, RenderMachine renderMachine) {
        var methodOpt = context.getTestMethod();
        if (methodOpt.isEmpty()) {
            return;
        }

        Method method = methodOpt.get();

        // Process DocSection annotation
        var section = method.getAnnotation(io.github.seanchatmangpt.dtr.DocSection.class);
        if (section != null) {
            renderMachine.sayNextSection(section.value());
        }

        // Process DocDescription annotation
        var desc = method.getAnnotation(io.github.seanchatmangpt.dtr.DocDescription.class);
        if (desc != null) {
            for (String line : desc.value()) {
                renderMachine.say(line);
            }
        }

        // Process DocNote annotation
        var note = method.getAnnotation(io.github.seanchatmangpt.dtr.DocNote.class);
        if (note != null) {
            for (String line : note.value()) {
                renderMachine.sayRaw("> [!NOTE]\n> " + line);
            }
        }

        // Process DocWarning annotation
        var warning = method.getAnnotation(io.github.seanchatmangpt.dtr.DocWarning.class);
        if (warning != null) {
            for (String line : warning.value()) {
                renderMachine.sayRaw("> [!WARNING]\n> " + line);
            }
        }

        // Process DocCode annotation
        var code = method.getAnnotation(io.github.seanchatmangpt.dtr.DocCode.class);
        if (code != null) {
            StringBuilder sb = new StringBuilder();
            String lang = code.language();
            sb.append("```").append(lang).append('\n');
            String[] lines = code.value();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                sb.append(lines[i]);
            }
            sb.append("\n```");
            renderMachine.sayRaw(sb.toString());
        }
    }
}

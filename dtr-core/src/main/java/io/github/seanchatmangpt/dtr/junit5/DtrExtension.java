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
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JUnit 5 extension for DTR integration.
 *
 * <p>This extension provides native JUnit 5 support for DTR, replacing
 * the JUnit 4 {@code @Rule} based approach with JUnit 5's extension model.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest implements DtrCommands {
 *
 *     @Test
 *     void testGetUsers(DtrContext context) {
 *         context.sayNextSection("User API");
 *         context.say("Documentation for User API goes here.");
 *     }
 * }
 * }</pre>
 *
 * <p>The extension manages:
 * <ul>
 *   <li>RenderMachine lifecycle (one per test class)</li>
 *   <li>Documentation output generation after all tests complete</li>
 * </ul>
 */
public class DtrExtension implements BeforeEachCallback, AfterAllCallback, ParameterResolver {

    private static final String RENDER_MACHINE_KEY = "dtr.renderMachine";
    private static final String FILE_NAME_KEY = "dtr.fileName";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Get or create the RenderMachine (shared across test methods in a class)
        var renderMachine = getOrCreateRenderMachine(context);

        // Process @DocSection / @DocDescription annotations if present
        processAnnotations(context, renderMachine);
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

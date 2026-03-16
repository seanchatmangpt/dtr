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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation that combines {@link ExtendWith}{@code (DtrExtension.class)}
 * and {@link AutoFinishDocTest} for streamlined DTR test configuration.
 *
 * <p>This annotation provides a concise way to enable DTR documentation generation
 * for test classes. It is functionally equivalent to:</p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * @AutoFinishDocTest
 * class MyTest { ... }
 * }</pre>
 *
 * <p><strong>Basic Usage (Field Injection):</strong></p>
 * <pre>{@code
 * @DtrTest
 * class MyApiTest {
 *     @DtrContextField
 *     private DtrContext ctx;
 *
 *     @Test
 *     void listUsers() {
 *         ctx.say("Returns all users");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Parameter Injection (Still Supported):</strong></p>
 * <pre>{@code
 * @DtrTest
 * class MyApiTest {
 *     @Test
 *     void listUsers(DtrContext ctx) {
 *         ctx.say("Returns all users");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Inheritance Pattern (Legacy):</strong></p>
 * <pre>{@code
 * @DtrTest
 * class MyApiTest extends io.github.seanchatmangpt.dtr.DtrTest {
 *     @Test
 *     void listUsers() {
 *         say("Returns all users");  // Direct say* method access
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Annotation Attributes:</strong></p>
 * <ul>
 *   <li>{@code autoFinish} — When {@code true} (default), automatically calls
 *       {@code finishAndWriteOut()} after each test method, generating separate
 *       documentation files. When {@code false}, documentation is written once
 *       after all tests in the class complete.</li>
 *   <li>{@code fileName} — Optional custom filename for the documentation output.
 *       Defaults to the fully-qualified test class name.</li>
 * </ul>
 *
 * <p><strong>Choosing the Right Pattern:</strong></p>
 * <ul>
 *   <li><strong>Field injection</strong> ({@code @DtrContextField}) — Best for tests with
 *       many methods that all need documentation context. Cleaner method signatures.</li>
 *   <li><strong>Parameter injection</strong> — Best when each test needs different
 *       documentation setup or when you prefer explicit dependencies.</li>
 *   <li><strong>Inheritance</strong> ({@code extends DtrTest}) — Legacy pattern. Still
 *       fully supported. Use {@code say()} methods directly without {@code ctx.} prefix.</li>
 * </ul>
 *
 * @see DtrExtension
 * @see DtrContext
 * @see DtrContextField
 * @see AutoFinishDocTest
 * @see io.github.seanchatmangpt.dtr.DtrTest
 * @since 2026.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
public @interface DtrTest {

    /**
     * Whether to automatically finish documentation output after each test method.
     *
     * <p>When {@code true}, each test method generates its own documentation file.
     * When {@code false}, all tests in the class contribute to a single documentation file
     * written after {@code @AfterAll}.</p>
     *
     * @return {@code true} to auto-finish after each test, {@code false} for class-level output
     */
    boolean autoFinish() default true;

    /**
     * Optional custom filename for the documentation output.
     *
     * <p>When specified, overrides the default filename (fully-qualified class name).
     * Useful for organizing documentation or avoiding naming conflicts.</p>
     *
     * <p>Example: {@code @DtrTest(fileName = "api-users")}</p>
     *
     * @return custom filename, or empty string to use default
     */
    String fileName() default "";
}

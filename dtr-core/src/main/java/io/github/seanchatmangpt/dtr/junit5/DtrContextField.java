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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to receive automatic {@link DtrContext} injection by {@link DtrExtension}.
 *
 * <p>This annotation enables class-level field injection as an alternative to method parameter
 * injection. Fields annotated with {@code @DtrContextField} will be automatically populated
 * with a {@link DtrContext} instance before test execution.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
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
 * <p><strong>Field Injection vs Parameter Injection:</strong></p>
 * <ul>
 *   <li><strong>Field injection</strong> ({@code @DtrContextField}) — context declared once at class level,
 *       accessible in all test methods. Cleaner for tests with many methods.</li>
 *   <li><strong>Parameter injection</strong> ({@code DtrContext ctx}) — context passed as method parameter.
 *       More explicit, preferred when each test needs different documentation setup.</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Each test method receives its own {@link DtrContext} instance,
 * but all instances share the same underlying {@link io.github.seanchatmangpt.dtr.rendermachine.RenderMachine}.
 * This ensures test isolation while maintaining a single documentation output per test class.</p>
 *
 * <p><strong>Supported Field Types:</strong></p>
 * <ul>
 *   <li>Instance fields (non-static)</li>
 *   <li>Static fields (shared across all test methods in the class)</li>
 *   <li>Any access modifier (private, protected, package-private, public)</li>
 * </ul>
 *
 * @see DtrExtension
 * @see DtrContext
 * @since 2026.4.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DtrContextField {
}

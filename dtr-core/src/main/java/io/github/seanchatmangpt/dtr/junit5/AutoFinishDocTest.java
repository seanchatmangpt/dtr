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
 * Automatically calls finishAndWriteOut() after each test method.
 * Eliminates the need for manual ctx.finishAndWriteOut() calls.
 *
 * <p>When applied at the class level, all test methods in that class will
 * automatically finish their documentation output after execution. When applied
 * at the method level, only that specific test method will auto-finish.</p>
 *
 * <p>This annotation is particularly useful for:</p>
 * <ul>
 *   <li>Granular per-test output files (each test gets its own documentation)</li>
 *   <li>Tests that need immediate documentation output for later steps</li>
 *   <li>Eliminating boilerplate finishAndWriteOut() calls</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * &#64;ExtendWith(DtrExtension.class)
 * &#64;AutoFinishDocTest
 * class MyDocumentationTest {
 *     &#64;Test
 *     void documentFeature(DtrContext ctx) {
 *         ctx.say("Content"); // No need to call finishAndWriteOut()
 *     }
 * }
 * </pre>
 *
 * <p>Method-level usage:
 * <pre>
 * &#64;ExtendWith(DtrExtension.class)
 * class MixedDocumentationTest {
 *     &#64;Test
 *     &#64;AutoFinishDocTest
 *     void autoFinishedTest(DtrContext ctx) {
 *         ctx.say("This test auto-finishes");
 *     }
 *
 *     &#64;Test
 *     void manualTest(DtrContext ctx) {
 *         ctx.say("This test requires manual finishAndWriteOut()");
 *     }
 * }
 * </pre>
 *
 * @see DtrExtension
 * @see DtrContext
 * @since 2026.4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFinishDocTest {
}

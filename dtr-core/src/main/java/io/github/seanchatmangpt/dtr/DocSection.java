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
package io.github.seanchatmangpt.dtr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the section heading for a DTR test method.
 *
 * <p>When placed on a {@code @Test} method, DTR automatically calls
 * {@link DTR#sayNextSection(String)} with the given title at the start
 * of the test, before any code in the method body runs. This is equivalent
 * to writing {@code sayNextSection("My Section")} as the first line of the test.</p>
 *
 * <p><strong>Rendering:</strong></p>
 * <ul>
 *   <li>Markdown: renders as H2 heading ({@code ## Section Title})</li>
 *   <li>LaTeX: renders as {@code \section{}} command</li>
 *   <li>Blog/Slides: format-specific heading level</li>
 * </ul>
 *
 * <p><strong>Annotation Processing Order:</strong></p>
 * <p>When multiple documentation annotations are present on a test method,
 * DTR processes them in this fixed order:</p>
 * <ol>
 *   <li>{@link DocSection} — section heading (first)</li>
 *   <li>{@link DocDescription} — narrative paragraphs</li>
 *   <li>{@link DocNote} — informational callouts</li>
 *   <li>{@link DocWarning} — warning callouts</li>
 *   <li>{@link DocCode} — code blocks (last)</li>
 * </ol>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * @Test
 * @DocSection("User Authentication")
 * @DocDescription("Verifies that valid credentials return a 200 response.")
 * public void testLogin() {
 *     Response response = sayAndMakeRequest(
 *         Request.POST().url(testServerUrl().path("/login")).payload(...));
 *     sayAndAssertThat("Login succeeds", 200, equalTo(response.httpStatus()));
 * }
 * }</pre>
 *
 * @see DTR#processDocAnnotations(java.lang.reflect.Method) for implementation details
 * @see DocDescription for narrative content
 * @see DocNote for informational callouts
 * @see DocWarning for warning callouts
 * @see DocCode for code example blocks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocSection {

    /**
     * The section heading text. Rendered as H2 in Markdown, {@code \section{}} in LaTeX.
     * Markdown formatting is <strong>not</strong> applied — use plain text.
     */
    String value();
}

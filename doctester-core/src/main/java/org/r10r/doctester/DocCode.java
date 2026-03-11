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
package org.r10r.doctester;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Renders a code example block in the generated Markdown documentation.
 *
 * <p>The lines in {@link #value()} are joined with newlines and emitted as a
 * fenced code block with triple backticks. An optional {@link #language()} hint
 * is added after the opening backticks (e.g. {@code "java"}, {@code "json"},
 * {@code "http"}) for syntax highlighting in Markdown renderers.
 *
 * <p>No escaping is needed — the content is placed inside a fenced code block
 * where special characters are rendered literally.
 *
 * <p>Rendering order within a test method (all annotations optional):
 * <ol>
 *   <li>{@link DocSection} — section heading (H1)</li>
 *   <li>{@link DocDescription} — narrative paragraphs</li>
 *   <li>{@link DocNote} — informational callout boxes</li>
 *   <li>{@link DocWarning} — warning callout boxes</li>
 *   <li>{@link DocCode} — code example blocks <em>(this annotation)</em></li>
 * </ol>
 *
 * <p>Example output:
 * <pre>{@code
 * ```json
 * {
 *   "username": "alice",
 *   "email": "alice@example.com"
 * }
 * ```
 * }</pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Test
 * @DocSection("Create User — Request Shape")
 * @DocCode(
 *     language = "json",
 *     value = {
 *         "{",
 *         "  \"username\": \"alice\",",
 *         "  \"email\": \"alice@example.com\"",
 *         "}"
 *     }
 * )
 * public void testCreateUser() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocCode {

    /**
     * Lines of source code to display. Each element becomes one line in the
     * rendered fenced code block. No escaping is needed.
     */
    String[] value();

    /**
     * Optional language hint added after the opening triple backticks
     * (e.g. {@code "java"}, {@code "json"}, {@code "http"}, {@code "bash"}).
     * Defaults to an empty string (no language hint emitted).
     */
    String language() default "";
}

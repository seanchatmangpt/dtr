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
 * Renders one or more informational callout boxes in the generated HTML documentation.
 *
 * <p>Each element in {@link #value()} is emitted as a separate Bootstrap
 * {@code alert-info} panel immediately after any {@link DocDescription} paragraphs
 * and before any {@link DocWarning} boxes. HTML in the values is rendered verbatim,
 * so inline markup such as {@code <code>} or {@code <a href="...">} is supported.
 *
 * <p>Rendering order within a test method (all annotations optional):
 * <ol>
 *   <li>{@link DocSection} — section heading (H1)</li>
 *   <li>{@link DocDescription} — narrative paragraphs</li>
 *   <li>{@link DocNote} — informational callout boxes <em>(this annotation)</em></li>
 *   <li>{@link DocWarning} — warning callout boxes</li>
 *   <li>{@link DocCode} — code example blocks</li>
 * </ol>
 *
 * <p>Example:
 * <pre>{@code
 * @Test
 * @DocSection("User API")
 * @DocDescription("Returns all active users.")
 * @DocNote("Requires the <code>Accept: application/json</code> header.")
 * public void testGetUsers() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocNote {

    /**
     * One or more informational callout texts. Each element produces a separate
     * {@code <div class="alert alert-info">} block in the output HTML.
     * HTML markup is allowed.
     */
    String[] value();
}

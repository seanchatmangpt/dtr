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
 * Renders one or more warning callout boxes in the generated Markdown documentation.
 *
 * <p>Each element in {@link #value()} is emitted as a separate GitHub-flavored
 * WARNING admonition, rendered after any {@link DocNote} boxes
 * and before any {@link DocCode} blocks. Markdown formatting in the values
 * is rendered verbatim.
 *
 * <p>Rendering order within a test method (all annotations optional):
 * <ol>
 *   <li>{@link DocSection} — section heading (H1)</li>
 *   <li>{@link DocDescription} — narrative paragraphs</li>
 *   <li>{@link DocNote} — informational callout boxes</li>
 *   <li>{@link DocWarning} — warning callout boxes <em>(this annotation)</em></li>
 *   <li>{@link DocCode} — code example blocks</li>
 * </ol>
 *
 * <p>Example output:
 * <pre>{@code
 * > [!WARNING]
 * > This operation is **irreversible**. The user record is permanently removed.
 * }</pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Test
 * @DocSection("Delete User")
 * @DocWarning("This operation is **irreversible**. The user record is permanently removed.")
 * public void testDeleteUser() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocWarning {

    /**
     * One or more warning texts. Each element produces a separate
     * GitHub-flavored {@code > [!WARNING]} admonition block in the output Markdown.
     * Markdown formatting is allowed.
     */
    String[] value();
}

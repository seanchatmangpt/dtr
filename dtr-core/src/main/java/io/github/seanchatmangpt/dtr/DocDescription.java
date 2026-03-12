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
 * Declares one or more description paragraphs for a DocTester test method.
 *
 * <p>When placed on a {@code @Test} method, DocTester automatically calls
 * {@link DocTester#say(String)} for each line in {@link #value()} at the start
 * of the test (after any {@link DocSection} heading). This is equivalent to
 * writing {@code say("...")} calls as the first lines of the test body.
 *
 * <p>Multiple lines produce multiple paragraphs in the generated HTML:
 * <pre>{@code
 * @Test
 * @DocSection("Article API")
 * @DocDescription({
 *     "This endpoint returns a paginated list of articles.",
 *     "Pass the 'page' query parameter to navigate between pages."
 * })
 * public void testListArticles() {
 *     ...
 * }
 * }</pre>
 *
 * <p>If only one description is needed, a single string is also accepted:
 * <pre>{@code
 * @DocDescription("Returns HTTP 200 with an empty array when no articles exist.")
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocDescription {

    /**
     * One or more description paragraphs to render before the test body executes.
     * Each element is emitted as a separate {@code say()} paragraph.
     */
    String[] value();
}

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
 * Declares the section heading for a DocTester test method.
 *
 * <p>When placed on a {@code @Test} method, DocTester automatically calls
 * {@link DocTester#sayNextSection(String)} with the given title at the start
 * of the test, before any code in the method body runs. This is equivalent
 * to writing {@code sayNextSection("My Section")} as the first line of the test.
 *
 * <p>Example:
 * <pre>{@code
 * @Test
 * @DocSection("User Authentication")
 * @DocDescription("Verifies that valid credentials return a 200 response.")
 * public void testLogin() {
 *     Response response = sayAndMakeRequest(
 *         Request.POST().url(testServerUrl().path("/login"))...);
 *     sayAndAssertThat("Login succeeds", 200, equalTo(response.httpStatus()));
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocSection {

    /**
     * The section heading text rendered as an H1 in the generated HTML documentation.
     */
    String value();
}

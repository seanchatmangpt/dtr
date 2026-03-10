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
package org.r10r.doctester.rendermachine;

import java.util.List;

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.hamcrest.Matcher;

public interface RenderMachineCommands {

    /**
     * A text that will be rendered as a paragraph in the documentation.
     * No escaping is done. You can use markdown formatting inside the text.
     *
     * @param text A text that may contain markdown formatting like "This is my **bold** text".
     */
    public void say(String text);

    /**
     * A heading that will appear as a top-level section in the documentation
     * and in the table of contents. No escaping is done.
     *
     * @param headline The section heading text.
     */
    public void sayNextSection(String headline);

    /**
     * Injects raw content directly into the documentation output.
     * Use this for custom markdown or other content that bypasses normal formatting.
     *
     * @param rawMarkdown Raw content to inject (e.g., markdown tables, code blocks, or HTML).
     */
    public void sayRaw(String rawMarkdown);

    /**
     * @return all cookies saved by this TestBrowser.
     */
    public List<Cookie> sayAndGetCookies();

    public Cookie sayAndGetCookieWithName(String name);

    public Response sayAndMakeRequest(Request httpRequest);

    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher);

    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher);
}

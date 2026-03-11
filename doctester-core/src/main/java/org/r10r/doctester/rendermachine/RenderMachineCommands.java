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
import java.util.Map;

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.crossref.DocTestRef;
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
     * Renders a markdown table from a 2D string array.
     * The first row is treated as table headers.
     *
     * @param data A 2D array where each row is a list of cells. First row becomes TH.
     */
    public void sayTable(String[][] data);

    /**
     * Renders a code block with optional syntax highlighting language hint.
     *
     * @param code The code content.
     * @param language The programming language for syntax highlighting (e.g., "java", "sql", "json").
     */
    public void sayCode(String code, String language);

    /**
     * Renders a warning callout box (GitHub-style [!WARNING] alert).
     *
     * @param message The warning message.
     */
    public void sayWarning(String message);

    /**
     * Renders an info callout box (GitHub-style [!NOTE] alert).
     *
     * @param message The info message.
     */
    public void sayNote(String message);

    /**
     * Renders key-value pairs in a readable format.
     *
     * @param pairs A map of keys to values.
     */
    public void sayKeyValue(Map<String, String> pairs);

    /**
     * Renders an unordered (bullet) list.
     *
     * @param items List of strings to render as bullet points.
     */
    public void sayUnorderedList(List<String> items);

    /**
     * Renders an ordered (numbered) list.
     *
     * @param items List of strings to render as numbered items.
     */
    public void sayOrderedList(List<String> items);

    /**
     * Serializes an object to JSON and renders it in a code block.
     *
     * @param object The object to serialize (will be rendered as pretty-printed JSON).
     */
    public void sayJson(Object object);

    /**
     * Renders assertion results in a table format with Check and Result columns.
     *
     * @param assertions A map where keys are check descriptions and values are results.
     */
    public void sayAssertions(Map<String, String> assertions);

    /**
     * Renders a cross-reference to another DocTest's section.
     *
     * The reference is resolved using the CrossReferenceIndex and rendered
     * as a markdown link in Markdown mode or a LaTeX \ref{} command in LaTeX mode.
     *
     * @param ref the cross-reference to another DocTest section
     */
    public void sayRef(DocTestRef ref);

    /**
     * Renders a citation reference using BibTeX citation key.
     *
     * @param citationKey The BibTeX citation key to reference.
     */
    public void sayCite(String citationKey);

    /**
     * Renders a citation reference with page number specification.
     *
     * @param citationKey The BibTeX citation key to reference.
     * @param pageRef The page reference (e.g., "42" or "pp. 10-15").
     */
    public void sayCite(String citationKey, String pageRef);

    /**
     * Renders a footnote with the given text.
     *
     * @param text The footnote content.
     */
    public void sayFootnote(String text);

    /**
     * @return all cookies saved by this TestBrowser.
     */
    public List<Cookie> sayAndGetCookies();

    public Cookie sayAndGetCookieWithName(String name);

    public Response sayAndMakeRequest(Request httpRequest);

    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher);

    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher);
}

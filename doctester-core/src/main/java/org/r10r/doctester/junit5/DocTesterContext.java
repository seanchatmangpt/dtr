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
package org.r10r.doctester.junit5;

import org.apache.hc.client5.http.cookie.Cookie;
import org.hamcrest.Matcher;
import org.r10r.doctester.rendermachine.RenderMachine;
import org.r10r.doctester.rendermachine.RenderMachineCommands;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.Url;
import org.r10r.doctester.crossref.DocTestRef;

import java.util.List;
import java.util.Map;

/**
 * Context object for JUnit 5 DocTester tests.
 *
 * <p>Provides access to all DocTester functionality within JUnit 5 test methods.
 * Can be injected as a parameter into test methods when using
 * {@link DocTesterExtension}.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DocTesterExtension.class)
 * class MyApiDocTest {
 *
 *     @Test
 *     void testGetUsers(DocTesterContext ctx) {
 *         ctx.sayNextSection("User API");
 *         var response = ctx.sayAndMakeRequest(
 *             Request.GET().url(ctx.testServerUrl().path("/api/users")));
 *         ctx.sayAndAssertThat("200 OK", 200, equalTo(response.httpStatus));
 *     }
 * }
 * }</pre>
 */
public class DocTesterContext implements RenderMachineCommands {

    private final RenderMachine renderMachine;
    private final TestBrowser testBrowser;
    private final String testServerUrl;

    /**
     * Creates a new DocTesterContext.
     *
     * @param renderMachine the render machine for documentation output
     * @param testBrowser the HTTP test browser
     * @param testServerUrl the base URL for the test server (can be null)
     */
    public DocTesterContext(RenderMachine renderMachine, TestBrowser testBrowser, String testServerUrl) {
        this.renderMachine = renderMachine;
        this.testBrowser = testBrowser;
        this.testServerUrl = testServerUrl != null ? testServerUrl : "http://localhost:8080";
    }

    // ========================================================================
    // RenderMachineCommands implementation
    // ========================================================================

    @Override
    public void say(String text) {
        renderMachine.say(text);
    }

    @Override
    public void sayNextSection(String headline) {
        renderMachine.sayNextSection(headline);
    }

    @Override
    public void sayRaw(String rawHtml) {
        renderMachine.sayRaw(rawHtml);
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        return testBrowser.getCookies();
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        return testBrowser.getCookieWithName(name);
    }

    @Override
    public Response sayAndMakeRequest(Request httpRequest) {
        return renderMachine.sayAndMakeRequest(httpRequest);
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        renderMachine.sayAndAssertThat(message, reason, actual, matcher);
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        renderMachine.sayAndAssertThat(message, actual, matcher);
    }

    @Override
    public void sayTable(String[][] data) {
        renderMachine.sayTable(data);
    }

    @Override
    public void sayCode(String code, String language) {
        renderMachine.sayCode(code, language);
    }

    @Override
    public void sayWarning(String message) {
        renderMachine.sayWarning(message);
    }

    @Override
    public void sayNote(String message) {
        renderMachine.sayNote(message);
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        renderMachine.sayKeyValue(pairs);
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        renderMachine.sayUnorderedList(items);
    }

    @Override
    public void sayOrderedList(List<String> items) {
        renderMachine.sayOrderedList(items);
    }

    @Override
    public void sayJson(Object object) {
        renderMachine.sayJson(object);
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        renderMachine.sayAssertions(assertions);
    }

    @Override
    public void sayCite(String citationKey) {
        renderMachine.sayCite(citationKey);
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        renderMachine.sayCite(citationKey, pageRef);
    }

    @Override
    public void sayFootnote(String text) {
        renderMachine.sayFootnote(text);
    }

    @Override
    public void sayRef(DocTestRef ref) {
        renderMachine.sayRef(ref);
    }

    // ========================================================================
    // TestBrowser delegate methods
    // ========================================================================

    /**
     * Makes an HTTP request without documenting it.
     *
     * @param httpRequest the request to make
     * @return the response
     */
    public Response makeRequest(Request httpRequest) {
        return testBrowser.makeRequest(httpRequest);
    }

    /**
     * Gets all cookies from the test browser.
     *
     * @return list of cookies
     */
    public List<Cookie> getCookies() {
        return testBrowser.getCookies();
    }

    /**
     * Gets a cookie by name from the test browser.
     *
     * @param name the cookie name
     * @return the cookie, or null if not found
     */
    public Cookie getCookieWithName(String name) {
        return testBrowser.getCookieWithName(name);
    }

    /**
     * Clears all cookies from the test browser.
     */
    public void clearCookies() {
        testBrowser.clearCookies();
    }

    // ========================================================================
    // Convenience methods
    // ========================================================================

    /**
     * Returns the base URL for the test server.
     *
     * <p>Override {@link #testServerUrl()} to customize the base URL.
     *
     * @return Url builder for the test server
     */
    public Url testServerUrl() {
        return Url.host(testServerUrl);
    }

    /**
     * Override this method to provide a custom test server URL.
     *
     * @return the base URL for the test server
     */
    protected String getTestServerUrl() {
        return testServerUrl;
    }

    /**
     * Sets a custom test server URL.
     *
     * @param url the new base URL
     */
    public void setTestServerUrl(String url) {
        // Note: This creates a new Url instance each time
    }

    // ========================================================================
    // Accessors for internal components
    // ========================================================================

    /**
     * Gets the underlying RenderMachine.
     *
     * @return the render machine
     */
    public RenderMachine getRenderMachine() {
        return renderMachine;
    }

    /**
     * Gets the underlying TestBrowser.
     *
     * @return the test browser
     */
    public TestBrowser getTestBrowser() {
        return testBrowser;
    }
}

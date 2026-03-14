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

import org.apache.hc.client5.http.cookie.Cookie;
import org.hamcrest.Matcher;
import io.github.seanchatmangpt.dtr.coverage.CoverageRow;
import io.github.seanchatmangpt.dtr.coverage.DocCoverageAnalyzer;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
import io.github.seanchatmangpt.dtr.testbrowser.Url;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context object for JUnit 5 DTR tests.
 *
 * <p>Provides access to all DTR functionality within JUnit 5 test methods.
 * Can be injected as a parameter into test methods when using
 * {@link DtrExtension}.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyApiDocTest {
 *
 *     @Test
 *     void testGetUsers(DtrContext ctx) {
 *         ctx.sayNextSection("User API");
 *         var response = ctx.sayAndMakeRequest(
 *             Request.GET().url(ctx.testServerUrl().path("/api/users")));
 *         ctx.sayAndAssertThat("200 OK", 200, equalTo(response.httpStatus));
 *     }
 * }
 * }</pre>
 */
public class DtrContext implements RenderMachineCommands {

    private final RenderMachine renderMachine;
    private final TestBrowser testBrowser;
    private String testServerUrl;

    /** Tracks which method names were documented during this test for sayDocCoverage(). */
    private final Set<String> documentedMethodNames = new HashSet<>();

    /**
     * Creates a new DtrContext.
     *
     * @param renderMachine the render machine for documentation output
     * @param testBrowser the HTTP test browser
     * @param testServerUrl the base URL for the test server (can be null)
     */
    public DtrContext(RenderMachine renderMachine, TestBrowser testBrowser, String testServerUrl) {
        this.renderMachine = renderMachine;
        this.testBrowser = testBrowser;
        this.testServerUrl = testServerUrl != null ? testServerUrl : "http://localhost:8080";
    }

    // ========================================================================
    // RenderMachineCommands implementation
    // ========================================================================

    @Override
    public void say(String text) {
        documentedMethodNames.add("say");
        renderMachine.say(text);
    }

    @Override
    public void sayNextSection(String headline) {
        documentedMethodNames.add("sayNextSection");
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
        documentedMethodNames.add("sayTable");
        renderMachine.sayTable(data);
    }

    @Override
    public void sayCode(String code, String language) {
        documentedMethodNames.add("sayCode");
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
     * Sets a custom test server URL. Subsequent calls to {@link #testServerUrl()}
     * and any requests routed through it will use this base URL.
     *
     * @param url the new base URL (must not be null or blank)
     * @throws IllegalArgumentException if url is null or blank
     */
    public void setTestServerUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Test server URL cannot be null or blank");
        }
        this.testServerUrl = url;
    }

    // ========================================================================
    // Code model introspection
    // ========================================================================

    @Override
    public void sayCodeModel(Class<?> clazz) {
        renderMachine.sayCodeModel(clazz);
    }

    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        renderMachine.sayCodeModel(method);
    }

    @Override
    public void sayCallSite() {
        renderMachine.sayCallSite();
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        renderMachine.sayAnnotationProfile(clazz);
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        renderMachine.sayClassHierarchy(clazz);
    }

    @Override
    public void sayStringProfile(String text) {
        renderMachine.sayStringProfile(text);
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        renderMachine.sayReflectiveDiff(before, after);
    }

    // ========================================================================
    // Java 26 Code Reflection + Blue Ocean innovations
    // ========================================================================

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        renderMachine.sayControlFlowGraph(method);
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        renderMachine.sayCallGraph(clazz);
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        renderMachine.sayOpProfile(method);
    }

    @Override
    public void sayBenchmark(String label, Runnable task) {
        renderMachine.sayBenchmark(label, task);
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        renderMachine.sayBenchmark(label, task, warmupRounds, measureRounds);
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        renderMachine.sayMermaid(diagramDsl);
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        renderMachine.sayClassDiagram(classes);
    }

    /**
     * Renders a documentation coverage report for the given classes, using the
     * set of method names tracked during this test via say* calls.
     *
     * @param classes the classes whose public API to check for coverage
     */
    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        if (renderMachine instanceof RenderMachineImpl impl) {
            for (Class<?> clazz : classes) {
                List<CoverageRow> rows = DocCoverageAnalyzer.analyze(clazz, documentedMethodNames);
                impl.sayDocCoverage(clazz, rows);
            }
        } else {
            renderMachine.sayDocCoverage(classes);
        }
    }

    @Override
    public void sayEnvProfile() {
        renderMachine.sayEnvProfile();
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        renderMachine.sayRecordComponents(recordClass);
    }

    @Override
    public void sayException(Throwable t) {
        renderMachine.sayException(t);
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        renderMachine.sayAsciiChart(label, values, xLabels);
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

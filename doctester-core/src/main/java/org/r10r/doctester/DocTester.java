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

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.rendermachine.RenderMachine;
import org.r10r.doctester.rendermachine.RenderMachineCommands;
import org.r10r.doctester.rendermachine.RenderMachineImpl;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.TestBrowserImpl;
import org.r10r.doctester.testbrowser.Url;

public abstract class DocTester implements TestBrowser, RenderMachineCommands {

    /**
     * classNameForDocTesterOutputFile will be set by the testWatcher. That way
     * we can easily generate a filename as output filename. Usually it is
     * something like "com.mycompany.NameOfClassTest".
     */
    private String classNameForDocTesterOutputFile;

    /**
     * Captured from TestInfo for annotation processing in setupForTestCaseMethod.
     */
    private Method currentTestMethod;

    private final Logger logger = LoggerFactory.getLogger(DocTester.class);

    // Unique for each test method.
    private TestBrowser testBrowser;

	// Unique for whole testClass => one outputfile per testClass.
    // Protected only for testing
    protected static RenderMachine renderMachine = null;

    @BeforeEach
    public void setupForTestCaseMethod(TestInfo testInfo) {

        // Capture class name and test method from TestInfo (JUnit 5 replacement for @Rule TestWatcher)
        classNameForDocTesterOutputFile = testInfo.getTestClass()
                .map(Class::getName)
                .orElse(getClass().getName());
        currentTestMethod = testInfo.getTestMethod().orElse(null);

        initRenderingMachineIfNull();

        // Set a fresh TestBrowser for each testmethod.
        testBrowser = getTestBrowser();
        renderMachine.setTestBrowser(testBrowser);

        // This is all a bit strange. But JUnit's @BeforeAll
        // is static. Therefore the only possibility to transmit
        // the filename to the renderMachine is here.
        // We accept that we set the fileName too often.
        renderMachine.setFileName(classNameForDocTesterOutputFile);

        // Process @DocSection / @DocDescription annotations declared on the test method.
        processDocAnnotations(currentTestMethod);

    }

    /**
     * Inspects the test method for doc annotations and emits the corresponding
     * render-machine calls. All five annotation types are optional and independent.
     * They are always emitted in this fixed order regardless of the order they
     * appear in source code:
     * <ol>
     *   <li>{@link DocSection} — section heading via {@code sayNextSection()}</li>
     *   <li>{@link DocDescription} — narrative paragraphs via {@code say()}</li>
     *   <li>{@link DocNote} — info callout boxes via {@code sayRaw()}</li>
     *   <li>{@link DocWarning} — warning callout boxes via {@code sayRaw()}</li>
     *   <li>{@link DocCode} — HTML-escaped {@code <pre><code>} blocks via {@code sayRaw()}</li>
     * </ol>
     *
     * @param method the test method (from TestInfo)
     */
    private void processDocAnnotations(Method method) {

        if (method == null) {
            return;
        }

        // 1. Section heading
        DocSection section = method.getAnnotation(DocSection.class);
        if (section != null) {
            renderMachine.sayNextSection(section.value());
        }

        // 2. Narrative description paragraphs
        DocDescription desc = method.getAnnotation(DocDescription.class);
        if (desc != null) {
            for (String line : desc.value()) {
                renderMachine.say(line);
            }
        }

        // 3. Informational callout boxes (Markdown GitHub-style alerts)
        DocNote note = method.getAnnotation(DocNote.class);
        if (note != null) {
            for (String line : note.value()) {
                renderMachine.sayRaw("> [!NOTE]\n> " + line);
            }
        }

        // 4. Warning callout boxes (Markdown GitHub-style alerts)
        DocWarning warning = method.getAnnotation(DocWarning.class);
        if (warning != null) {
            for (String line : warning.value()) {
                renderMachine.sayRaw("> [!WARNING]\n> " + line);
            }
        }

        // 5. Code example block — fenced code block in Markdown
        DocCode code = method.getAnnotation(DocCode.class);
        if (code != null) {
            String lang = code.language();
            StringBuilder sb = new StringBuilder("```").append(lang).append('\n');
            for (String line : code.value()) {
                sb.append(line).append('\n');
            }
            sb.append("```");
            renderMachine.sayRaw(sb.toString());
        }

    }

    public void initRenderingMachineIfNull() {

        if (renderMachine == null) {
            renderMachine = getRenderMachine();
        }

    }

    @AfterAll
    public static void finishDocTest() {

        if (renderMachine != null) {
            renderMachine.finishAndWriteOut();
            renderMachine = null;
        }

    }

    // ////////////////////////////////////////////////////////////////////////
    // Say methods to generate documentation
    // ////////////////////////////////////////////////////////////////////////
    @Override
    public final void say(String textAsParagraph) {
        renderMachine.say(textAsParagraph);
    }

    @Override
    public final void sayNextSection(String textAsH1) {
        renderMachine.sayNextSection(textAsH1);
    }

    @Override
    public final void sayRaw(String rawHtml) {
        renderMachine.sayRaw(rawHtml);
    }

    @Override
    public final <T> void sayAndAssertThat(String message,
            String reason,
            T actual,
            Matcher<? super T> matcher) {

        renderMachine.sayAndAssertThat(message, reason, actual, matcher);

    }

    @Override
    public final <T> void sayAndAssertThat(String message,
            T actual,
            Matcher<? super T> matcher) {

        sayAndAssertThat(message, "", actual, matcher);

    }

    // //////////////////////////////////////////////////////////////////////////
    // Inlined methods of the TestBrowser (for convenience)
    // //////////////////////////////////////////////////////////////////////////
    /**
     * @return all cookies saved by this TestBrowser.
     */
    @Override
    public final List<Cookie> getCookies() {
        return testBrowser.getCookies();
    }

    @Override
    public final List<Cookie> sayAndGetCookies() {
        return testBrowser.getCookies();
    }

    @Override
    public final Cookie getCookieWithName(String name) {
        return testBrowser.getCookieWithName(name);
    }

    @Override
    public final Cookie sayAndGetCookieWithName(String name) {
        return testBrowser.getCookieWithName(name);
    }

    @Override
    public final void clearCookies() {
        testBrowser.clearCookies();
    }

    @Override
    public final Response makeRequest(Request httpRequest) {
        return testBrowser.makeRequest(httpRequest);
    }

    @Override
    public final Response sayAndMakeRequest(Request httpRequest) {
        return renderMachine.sayAndMakeRequest(httpRequest);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Configuration of DoctestJ
    // //////////////////////////////////////////////////////////////////////////
    /**
     * You may override this method if you want to supply your own testbrowser
     * for your class or classes.
     *
     * @return a TestBrowser that will be used for each test method.
     */
    public TestBrowser getTestBrowser() {

        return new TestBrowserImpl();

    }

    /**
     * You may override this method if you want to supply your own rendering
     * machine for your class or classes.
     *
     * @return a RenderMachine that generates output and lives for a whole test
     * class.
     */
    public RenderMachine getRenderMachine() {

        return new RenderMachineImpl();

    }

    /**
     * Convenience method that allows you to write tests with the testbrowser in
     * a fluent way.
     *
     * <code>
     *
     * sayAndMakeRequest(
     *           Request
     *               .GET()
     *               .url(testServerUrl().path("search").addQueryParameter("q", "toys")));
     * </code>
     *
     *
     * @return a valid host name of your test server (eg http://localhost:8127).
     * This will be used in the testServerUrl() method.
     */
    public Url testServerUrl() {

        final String errorText = "If you want to use the TestBrowser you have to override getTestServerUrl().";
        logger.error(errorText);

        throw new IllegalStateException(errorText);
    }

    /**
     * Alternative way to set the output file name. This can be handy when
     * DocTester is not part of JUnit lifecycle.
     *
     * @param name alternative name of output file
     */
    public void setClassNameForDocTesterOutputFile(final String name) {
        this.classNameForDocTesterOutputFile = name;
    }
}

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

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.crossref.CrossReferenceIndex;
import io.github.seanchatmangpt.dtr.render.RenderMachineFactory;

/**
 * Abstract base class for documentation testing framework using JUnit 5.
 *
 * <p>DtrTest bridges test execution and documentation generation, allowing developers
 * to write tests that simultaneously verify behavior and auto-generate comprehensive
 * documentation. Supports multiple output formats: Markdown, LaTeX/PDF, blog posts,
 * and presentation slides.</p>
 *
 * <p><strong>Core Features:</strong></p>
 * <ul>
 *   <li>Documentation generation via {@link RenderMachine} (multiple output formats)</li>
 *   <li>Annotation-driven documentation: {@link DocSection}, {@link DocDescription}, etc.</li>
 *   <li>Reflection-based code introspection: {@link #sayCodeModel(Class)}, {@link #sayAnnotationProfile(Class)}</li>
 *   <li>Cross-references between DocTests with automatic section numbering</li>
 * </ul>
 *
 * <p><strong>Basic Usage:</strong></p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class MyDocTest extends DtrTest {
 *
 *     @Test
 *     @DocSection("Overview")
 *     @DocDescription("Describes the feature.")
 *     void testFeature() {
 *         say("Feature works as expected.");
 *         sayCode("System.out.println(\"Hello\");", "java");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>JUnit 5 Integration:</strong></p>
 * <p>While this class is abstract and doesn't require {@code @ExtendWith(DtrExtension.class)},
 * it is designed to work seamlessly with that extension. The extension manages the
 * RenderMachine lifecycle (one per test class).</p>
 *
 * <p><strong>Annotation Processing:</strong></p>
 * <p>The following annotations on test methods are automatically processed and rendered:</p>
 * <ul>
 *   <li>{@code @DocSection("title")} → H2 heading in output</li>
 *   <li>{@code @DocDescription({"line1", "line2"})} → narrative paragraphs</li>
 *   <li>{@code @DocNote({"note"})} → GitHub-style [!NOTE] alert boxes</li>
 *   <li>{@code @DocWarning({"warn"})} → GitHub-style [!WARNING] alert boxes</li>
 *   <li>{@code @DocCode({"code line"}, language="java")} → syntax-highlighted code blocks</li>
 * </ul>
 *
 * <p><strong>Output Lifecycle:</strong></p>
 * <p>Documentation is accumulated during test execution and written to disk after all tests
 * in the class complete (via {@code @AfterAll} hook). Output location: {@code docs/test/&lt;ClassName&gt;.*}</p>
 *
 * @see DtrExtension for JUnit 5 integration
 * @see RenderMachine for output format options
 * @since 1.0.0
 */
public abstract class DtrTest implements RenderMachineCommands {

    /**
     * classNameForDtrOutputFile will be set by the testWatcher. That way
     * we can easily generate a filename as output filename. Usually it is
     * something like "com.mycompany.NameOfClassTest".
     */
    private String classNameForDtrOutputFile;

    /**
     * Captured from TestInfo for annotation processing in setupForTestCaseMethod.
     */
    private Method currentTestMethod;

    private final Logger logger = LoggerFactory.getLogger(DtrTest.class);

	// Unique for whole testClass => one outputfile per testClass.
    // Protected only for testing
    protected static RenderMachine renderMachine = null;

    /**
     * JUnit 5 {@code @BeforeEach} lifecycle hook that wires the test class name
     * and current test method into the render machine before each test method runs.
     * Processes any {@link DocSection}, {@link DocDescription}, {@link DocNote},
     * {@link DocWarning}, and {@link DocCode} annotations present on the test method.
     *
     * @param testInfo JUnit 5 test metadata (class name, method reference)
     */
    @BeforeEach
    public void setupForTestCaseMethod(TestInfo testInfo) {

        // Capture class name and test method from TestInfo (JUnit 5 replacement for @Rule TestWatcher)
        classNameForDtrOutputFile = testInfo.getTestClass()
                .map(Class::getName)
                .orElse(getClass().getName());
        currentTestMethod = testInfo.getTestMethod().orElse(null);

        initRenderingMachineIfNull();

        // This is all a bit strange. But JUnit's @BeforeAll
        // is static. Therefore the only possibility to transmit
        // the filename to the renderMachine is here.
        // We accept that we set the fileName too often.
        renderMachine.setFileName(classNameForDtrOutputFile);

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

    /**
     * Lazily initialises the shared {@code renderMachine} if it has not yet been
     * created. Called from {@link #setupForTestCaseMethod} and available to
     * subclasses that need to trigger initialisation outside the normal lifecycle.
     */
    public void initRenderingMachineIfNull() {

        if (renderMachine == null) {
            renderMachine = getRenderMachine();
        }

    }

    /**
     * JUnit 5 {@code @AfterAll} lifecycle hook that flushes and finalises the
     * shared render machine after all test methods in the class have run.
     * Calls {@code finishAndWriteOut()} to close output streams and write the
     * final document files, then nulls the reference so the next test class
     * starts fresh.
     */
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
    public final void sayTable(String[][] data) {
        renderMachine.sayTable(data);
    }

    @Override
    public final void sayCode(String code, String language) {
        renderMachine.sayCode(code, language);
    }

    @Override
    public final void sayWarning(String message) {
        renderMachine.sayWarning(message);
    }

    @Override
    public final void sayNote(String message) {
        renderMachine.sayNote(message);
    }

    @Override
    public final void sayKeyValue(Map<String, String> pairs) {
        renderMachine.sayKeyValue(pairs);
    }

    @Override
    public final void sayUnorderedList(List<String> items) {
        renderMachine.sayUnorderedList(items);
    }

    @Override
    public final void sayOrderedList(List<String> items) {
        renderMachine.sayOrderedList(items);
    }

    @Override
    public final void sayJson(Object object) {
        renderMachine.sayJson(object);
    }

    @Override
    public final void sayAssertions(Map<String, String> assertions) {
        renderMachine.sayAssertions(assertions);
    }

    @Override
    public final void sayRef(DocTestRef ref) {
        if (ref != null) {
            CrossReferenceIndex.getInstance().register(ref);
        }
        renderMachine.sayRef(ref);
    }

    /**
     * Convenience method to create and render a cross-reference to another DocTest's section.
     *
     * <p>Creates a {@link DocTestRef} and delegates to {@link #sayRef(DocTestRef)}.
     * The reference is rendered as a markdown link in Markdown mode or as a LaTeX
     * cross-reference command in LaTeX mode. In LaTeX, the resolved section number
     * (e.g., "Section 3.2") is automatically substituted after compilation.</p>
     *
     * @param docTestClass the target DocTest class (must not be null)
     * @param anchor the section/anchor name within that DocTest, e.g., "user-registration"
     *               (typically derived from @DocSection annotation value)
     * @see DocTestRef#of(Class, String)
     * @see CrossReferenceIndex for reference resolution
     */
    public final void sayRef(Class<?> docTestClass, String anchor) {
        sayRef(DocTestRef.of(docTestClass, anchor));
    }

    @Override
    public final void sayFootnote(String text) {
        renderMachine.sayFootnote(text);
    }

    @Override
    public final void sayCite(String citationKey) {
        renderMachine.sayCite(citationKey);
    }

    @Override
    public final void sayCite(String citationKey, String pageRef) {
        renderMachine.sayCite(citationKey, pageRef);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Configuration of DoctestJ
    // //////////////////////////////////////////////////////////////////////////

    /**
     * You may override this method if you want to supply your own rendering
     * machine for your class or classes.
     *
     * @return a RenderMachine that generates output and lives for a whole test
     * class.
     */
    public RenderMachine getRenderMachine() {
        return RenderMachineFactory.createRenderMachine(getClass().getSimpleName());
    }

    /**
     * Alternative way to set the output file name. This can be handy when
     * DTR is not part of JUnit lifecycle.
     *
     * @param name alternative name of output file
     */
    public void setClassNameForDtrOutputFile(final String name) {
        this.classNameForDtrOutputFile = name;
    }

    /**
     * Renders content only for slide output (ignored by markdown/blog render machines).
     *
     * @param text the text to render on slides only
     */
    public void saySlideOnly(String text) {
        renderMachine.saySlideOnly(text);
    }

    /**
     * Renders content only for documentation/blog output (ignored by slide render machines).
     *
     * @param text the text to render in docs only
     */
    public void sayDocOnly(String text) {
        renderMachine.sayDocOnly(text);
    }

    /**
     * Renders speaker notes for slides (ignored by doc/blog render machines).
     *
     * @param text the speaker notes text
     */
    public void saySpeakerNote(String text) {
        renderMachine.saySpeakerNote(text);
    }

    /**
     * Renders a hero image for blogs and slides (ignored by other formats).
     *
     * @param altText the alt text for the image
     */
    public void sayHeroImage(String altText) {
        renderMachine.sayHeroImage(altText);
    }

    /**
     * Renders a tweetable excerpt (≤280 chars) for social media queue.
     *
     * @param text the text to tweet (will be truncated to 280 chars)
     */
    public void sayTweetable(String text) {
        renderMachine.sayTweetable(text);
    }

    /**
     * Renders a TLDR (too long; didn't read) summary for blogs.
     *
     * @param text the summary text
     */
    public void sayTldr(String text) {
        renderMachine.sayTldr(text);
    }

    /**
     * Renders a call-to-action link for blogs.
     *
     * @param url the URL for the CTA button/link
     */
    public void sayCallToAction(String url) {
        renderMachine.sayCallToAction(url);
    }

    /**
     * Documents a class's structure using Java reflection — the DtrTest stand-in for
     * Project Babylon's Code Reflection API (JEP 494).
     *
     * <p>Renders the class's sealed hierarchy (if sealed), record components (if a record),
     * and all public method signatures — derived directly from the bytecode, not from
     * developer-written descriptions. The documentation cannot drift from the implementation
     * because it IS the implementation.</p>
     *
     * <p>This is the most uniquely DtrTest application of Project Babylon's vision:
     * instead of a developer describing what code does, the code describes itself.</p>
     *
     * @param clazz the class to introspect and document
     */
    public final void sayCodeModel(Class<?> clazz) {
        renderMachine.sayCodeModel(clazz);
    }

    /**
     * Documents a method's structure using Project Babylon CodeReflection API.
     *
     * <p>On Java 26+, uses {@code java.lang.reflect.code.CodeReflection.reflect(method)}
     * to introspect the method's bytecode operations. On Java 25 and earlier, renders
     * the method signature extracted via reflection.</p>
     *
     * @param method the method to introspect and document
     */
    @Override
    public final void sayCodeModel(java.lang.reflect.Method method) {
        renderMachine.sayCodeModel(method);
    }

    /**
     * Documents the current call site using {@link StackWalker}.
     * Renders the calling class, method name, and line number as provenance metadata.
     */
    public final void sayCallSite() {
        renderMachine.sayCallSite();
    }

    /**
     * Documents all annotations on a class and its methods using reflection.
     *
     * @param clazz the class to inspect for annotations
     */
    public final void sayAnnotationProfile(Class<?> clazz) {
        renderMachine.sayAnnotationProfile(clazz);
    }

    /**
     * Renders the full class hierarchy (superclass chain + interfaces) as a tree.
     *
     * @param clazz the class whose hierarchy to render
     */
    public final void sayClassHierarchy(Class<?> clazz) {
        renderMachine.sayClassHierarchy(clazz);
    }

    /**
     * Analyzes a string and renders its structural profile using Java string APIs.
     *
     * @param text the string to profile
     */
    public final void sayStringProfile(String text) {
        renderMachine.sayStringProfile(text);
    }

    /**
     * Compares two objects field-by-field using reflection and renders a diff table.
     *
     * @param before the object representing the "before" state
     * @param after  the object representing the "after" state (must be same type)
     */
    public final void sayReflectiveDiff(Object before, Object after) {
        renderMachine.sayReflectiveDiff(before, after);
    }

    /** Renders a control flow graph via Java 26 Code Reflection (JEP 516). */
    public final void sayControlFlowGraph(java.lang.reflect.Method method) {
        renderMachine.sayControlFlowGraph(method);
    }

    /** Renders a call graph via Java 26 Code Reflection InvokeOp traversal. */
    public final void sayCallGraph(Class<?> clazz) {
        renderMachine.sayCallGraph(clazz);
    }

    /** Renders an op-profile table via Java 26 Code Reflection. */
    public final void sayOpProfile(java.lang.reflect.Method method) {
        renderMachine.sayOpProfile(method);
    }

    /** Benchmarks and documents a task with real nanoTime measurements. */
    public final void sayBenchmark(String label, Runnable task) {
        renderMachine.sayBenchmark(label, task);
    }

    /** Benchmarks with explicit warmup/measure rounds. */
    public final void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        renderMachine.sayBenchmark(label, task, warmupRounds, measureRounds);
    }

    /** Renders a raw Mermaid diagram as a fenced code block. */
    public final void sayMermaid(String diagramDsl) {
        renderMachine.sayMermaid(diagramDsl);
    }

    /** Auto-generates a Mermaid classDiagram from reflection. */
    public final void sayClassDiagram(Class<?>... classes) {
        renderMachine.sayClassDiagram(classes);
    }

    /** Renders an environment snapshot (Java, OS, heap, etc.). */
    public final void sayEnvProfile() {
        renderMachine.sayEnvProfile();
    }

    /** Renders a Java record's component schema table. */
    public final void sayRecordComponents(Class<? extends Record> recordClass) {
        renderMachine.sayRecordComponents(recordClass);
    }

    /** Documents an exception chain in a structured table. */
    public final void sayException(Throwable t) {
        renderMachine.sayException(t);
    }

    /** Renders an ASCII horizontal bar chart for numeric data. */
    public final void sayAsciiChart(String label, double[] values, String[] xLabels) {
        renderMachine.sayAsciiChart(label, values, xLabels);
    }

    /** Renders a documentation coverage report for the given classes. */
    public final void sayDocCoverage(Class<?>... classes) {
        renderMachine.sayDocCoverage(classes);
    }

    /** Documents interface contract coverage across implementation classes. */
    public final void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        renderMachine.sayContractVerification(contract, implementations);
    }

    /** Documents git commit history for the source file of the given class. */
    public final void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        renderMachine.sayEvolutionTimeline(clazz, maxEntries);
    }
}

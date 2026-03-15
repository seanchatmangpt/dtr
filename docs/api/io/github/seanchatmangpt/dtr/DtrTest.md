# `DtrTest`

> **Package:** `io.github.seanchatmangpt.dtr`  
> **Since:** `1.0.0`  

Abstract base class for documentation testing framework using JUnit 5. <p>DtrTest bridges test execution and documentation generation, allowing developers to write tests that simultaneously verify behavior and auto-generate comprehensive documentation. Supports multiple output formats: Markdown, LaTeX/PDF, blog posts, and presentation slides.</p> <p><strong>Core Features:</strong></p> <ul>   <li>Documentation generation via {@link RenderMachine} (multiple output formats)</li>   <li>Annotation-driven documentation: {@link DocSection}, {@link DocDescription}, etc.</li>   <li>Reflection-based code introspection: {@link #sayCodeModel(Class)}, {@link #sayAnnotationProfile(Class)}</li>   <li>Cross-references between DocTests with automatic section numbering</li> </ul> <p><strong>Basic Usage:</strong></p> <pre>{@code @ExtendWith(DtrExtension.class) class MyDocTest extends DtrTest {     @Test     @DocSection("Overview")     @DocDescription("Describes the feature.")     void testFeature() {         say("Feature works as expected.");         sayCode("System.out.println(\"Hello\");", "java");     } } }</pre> <p><strong>JUnit 5 Integration:</strong></p> <p>While this class is abstract and doesn't require {@code @ExtendWith(DtrExtension.class)}, it is designed to work seamlessly with that extension. The extension manages the RenderMachine lifecycle (one per test class).</p> <p><strong>Annotation Processing:</strong></p> <p>The following annotations on test methods are automatically processed and rendered:</p> <ul>   <li>{@code @DocSection("title")} → H2 heading in output</li>   <li>{@code @DocDescription({"line1", "line2"})} → narrative paragraphs</li>   <li>{@code @DocNote({"note"})} → GitHub-style [!NOTE] alert boxes</li>   <li>{@code @DocWarning({"warn"})} → GitHub-style [!WARNING] alert boxes</li>   <li>{@code @DocCode({"code line"}, language="java")} → syntax-highlighted code blocks</li> </ul> <p><strong>Output Lifecycle:</strong></p> <p>Documentation is accumulated during test execution and written to disk after all tests in the class complete (via {@code @AfterAll} hook). Output location: {@code docs/test/&lt;ClassName&gt;.*}</p>

```java
public abstract class DtrTest implements RenderMachineCommands {
    // setupForTestCaseMethod, processDocAnnotations, initRenderingMachineIfNull, finishDocTest, sayRef, getRenderMachine, setClassNameForDtrOutputFile, saySlideOnly, ... (60 total)
}
```

---

## Methods

### `finishDocTest`

JUnit 5 {@code @AfterAll} lifecycle hook that flushes and finalises the shared render machine after all test methods in the class have run. Calls {@code finishAndWriteOut()} to close output streams and write the final document files, then nulls the reference so the next test class starts fresh.

---

### `getRenderMachine`

You may override this method if you want to supply your own rendering machine for your class or classes.

> **Returns:** a RenderMachine that generates output and lives for a whole test class.

---

### `initRenderingMachineIfNull`

Lazily initialises the shared {@code renderMachine} if it has not yet been created. Called from {@link #setupForTestCaseMethod} and available to subclasses that need to trigger initialisation outside the normal lifecycle.

---

### `processDocAnnotations`

Inspects the test method for doc annotations and emits the corresponding render-machine calls. All five annotation types are optional and independent. They are always emitted in this fixed order regardless of the order they appear in source code: <ol>   <li>{@link DocSection} — section heading via {@code sayNextSection()}</li>   <li>{@link DocDescription} — narrative paragraphs via {@code say()}</li>   <li>{@link DocNote} — info callout boxes via {@code sayRaw()}</li>   <li>{@link DocWarning} — warning callout boxes via {@code sayRaw()}</li>   <li>{@link DocCode} — HTML-escaped {@code <pre><code>} blocks via {@code sayRaw()}</li> </ol>

| Parameter | Description |
| --- | --- |
| `method` | the test method (from TestInfo) |

---

### `sayActorMessages`

Documents actor-model message passing as a sequence diagram. */

---

### `sayAgentLoop`

Documents an AI agent's reasoning loop as a sequence diagram. */

---

### `sayAndAssertThat`

Overload for {@code boolean} primitives. */

---

### `sayAndon`

Documents a Toyota Andon-cord production-status board. */

---

### `sayAnnotationProfile`

Documents all annotations on a class and its methods using reflection.

| Parameter | Description |
| --- | --- |
| `clazz` | the class to inspect for annotations |

---

### `sayApiDiff`

Computes the semantic diff between two API class versions. */

---

### `sayAsciiChart`

Renders an ASCII horizontal bar chart for numeric data. */

---

### `sayBenchmark`

Benchmarks with explicit warmup/measure rounds. */

---

### `sayCallGraph`

Renders a call graph via Java 26 Code Reflection InvokeOp traversal. */

---

### `sayCallSite`

Documents the current call site using {@link StackWalker}. Renders the calling class, method name, and line number as provenance metadata.

---

### `sayCallToAction`

Renders a call-to-action link for blogs.

| Parameter | Description |
| --- | --- |
| `url` | the URL for the CTA button/link |

---

### `sayClassDiagram`

Auto-generates a Mermaid classDiagram from reflection. */

---

### `sayClassHierarchy`

Renders the full class hierarchy (superclass chain + interfaces) as a tree.

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose hierarchy to render |

---

### `sayCodeModel`

Documents a method's structure using Project Babylon CodeReflection API. <p>On Java 26+, uses {@code java.lang.reflect.code.CodeReflection.reflect(method)} to introspect the method's bytecode operations. On Java 26 and earlier, renders the method signature extracted via reflection.</p>

| Parameter | Description |
| --- | --- |
| `method` | the method to introspect and document |

---

### `sayComplexityProfile`

Empirically profiles algorithmic complexity at increasing input sizes. */

---

### `sayContractVerification`

Documents interface contract coverage across implementation classes. */

---

### `sayControlFlowGraph`

Renders a control flow graph via Java 26 Code Reflection (JEP 516). */

---

### `sayDataFlow`

Documents a data transformation pipeline by running samples through stages. */

---

### `sayDecisionTree`

Documents a decision algorithm as a Mermaid flowchart. */

---

### `sayDocCoverage`

Renders a documentation coverage report for the given classes. */

---

### `sayDocOnly`

Renders content only for documentation/blog output (ignored by slide render machines).

| Parameter | Description |
| --- | --- |
| `text` | the text to render in docs only |

---

### `sayEnvProfile`

Renders an environment snapshot (Java, OS, heap, etc.). */

---

### `sayEvolutionTimeline`

Documents git commit history for the source file of the given class. */

---

### `sayException`

Documents an exception chain in a structured table. */

---

### `sayFaultTolerance`

Documents a "let it crash" fault-tolerance scenario. */

---

### `sayHeatmap`

Renders a 2-D ASCII heatmap for matrix data. */

---

### `sayHeroImage`

Renders a hero image for blogs and slides (ignored by other formats).

| Parameter | Description |
| --- | --- |
| `altText` | the alt text for the image |

---

### `sayJavadoc`

Renders Javadoc for a method from the dtr-javadoc index. */

---

### `sayKaizen`

Documents a Kaizen continuous-improvement event with before/after metrics. */

---

### `sayKanban`

Documents a Kanban board snapshot. */

---

### `sayMermaid`

Renders a raw Mermaid diagram as a fenced code block. */

---

### `sayMuda`

Documents a Muda waste-elimination analysis. */

---

### `sayOpProfile`

Renders an op-profile table via Java 26 Code Reflection. */

---

### `sayParallelTrace`

Renders a parallel execution trace as a Mermaid Gantt chart. */

---

### `sayPatternMatch`

Documents Erlang-style pattern matching results. */

---

### `sayPokaYoke`

Documents Poka-yoke mistake-proofing mechanisms and their verification. */

---

### `sayPropertyBased`

Documents a property-based invariant with verification across sample inputs. */

---

### `sayRecordComponents`

Renders a Java record's component schema table. */

---

### `sayRef`

Convenience method to create and render a cross-reference to another DocTest's section. <p>Creates a {@link DocTestRef} and delegates to {@link #sayRef(DocTestRef)}. The reference is rendered as a markdown link in Markdown mode or as a LaTeX cross-reference command in LaTeX mode. In LaTeX, the resolved section number (e.g., "Section 3.2") is automatically substituted after compilation.</p>

| Parameter | Description |
| --- | --- |
| `docTestClass` | the target DocTest class (must not be null) |
| `anchor` | the section/anchor name within that DocTest, e.g., "user-registration"               (typically derived from @DocSection annotation value) |

---

### `sayReflectiveDiff`

Compares two objects field-by-field using reflection and renders a diff table.

| Parameter | Description |
| --- | --- |
| `before` | the object representing the "before" state |
| `after` | the object representing the "after" state (must be same type) |

---

### `saySlideOnly`

Renders content only for slide output (ignored by markdown/blog render machines).

| Parameter | Description |
| --- | --- |
| `text` | the text to render on slides only |

---

### `saySpeakerNote`

Renders speaker notes for slides (ignored by doc/blog render machines).

| Parameter | Description |
| --- | --- |
| `text` | the speaker notes text |

---

### `sayStateMachine`

Renders a finite state machine as a Mermaid stateDiagram-v2. */

---

### `sayStringProfile`

Analyzes a string and renders its structural profile using Java string APIs.

| Parameter | Description |
| --- | --- |
| `text` | the string to profile |

---

### `saySupervisionTree`

Documents an Erlang/OTP supervision tree as a Mermaid graph. */

---

### `sayTimeSeries`

Documents a metric time-series with ASCII sparkline and trend summary. */

---

### `sayTldr`

Renders a TLDR (too long; didn't read) summary for blogs.

| Parameter | Description |
| --- | --- |
| `text` | the summary text |

---

### `sayTweetable`

Renders a tweetable excerpt (≤280 chars) for social media queue.

| Parameter | Description |
| --- | --- |
| `text` | the text to tweet (will be truncated to 280 chars) |

---

### `sayValueStream`

Documents a Value Stream Map with measured cycle times. */

---

### `setClassNameForDtrOutputFile`

Alternative way to set the output file name. This can be handy when DTR is not part of JUnit lifecycle.

| Parameter | Description |
| --- | --- |
| `name` | alternative name of output file |

---

### `setupForTestCaseMethod`

JUnit 5 {@code @BeforeEach} lifecycle hook that wires the test class name and current test method into the render machine before each test method runs. Processes any {@link DocSection}, {@link DocDescription}, {@link DocNote}, {@link DocWarning}, and {@link DocCode} annotations present on the test method.

| Parameter | Description |
| --- | --- |
| `testInfo` | JUnit 5 test metadata (class name, method reference) |

---


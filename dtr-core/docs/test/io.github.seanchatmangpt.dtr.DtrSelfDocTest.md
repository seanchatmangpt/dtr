# io.github.seanchatmangpt.dtr.DtrSelfDocTest

## Table of Contents

- [DTR Base Class Entry Point](#dtrbaseclassentrypoint)
- [DTR Type Hierarchy](#dtrtypehierarchy)
- [DTR Code Model](#dtrcodemodel)
- [DTR Annotation Profile](#dtrannotationprofile)
- [RenderMachineCommands Interface](#rendermachinecommandsinterface)
- [RenderMachineCommands Interface Model](#rendermachinecommandsinterfacemodel)
- [Usage Example](#usageexample)
- [Core say* Methods for Documentation](#coresaymethodsfordocumentation)
- [Core say* Methods Reference](#coresaymethodsreference)
- [RenderMachineCommands Interface](#rendermachinecommandsinterface)
- [Introspection API — Blue Ocean Features](#introspectionapiblueoceanfeatures)
- [6 Introspection Methods](#6introspectionmethods)
- [Live Introspection Demo: Call Site](#liveintrospectiondemocallsite)
- [Live Introspection Demo: Reflective Diff](#liveintrospectiondemoreflectivediff)
- [Rendering Pipeline and Lifecycle](#renderingpipelineandlifecycle)
- [Test Execution Lifecycle](#testexecutionlifecycle)
- [RenderMachine Abstraction](#rendermachineabstraction)
- [Annotation-Driven Documentation](#annotationdrivendocumentation)
- [5 Documentation Annotations](#5documentationannotations)
- [Annotations on This Test Method](#annotationsonthistestmethod)
- [Extended say* Methods for Multi-Format Output](#extendedsaymethodsformultiformatoutput)
- [7 Extended say* Methods](#7extendedsaymethods)
- [Format-Agnostic Architecture](#formatagnosticarchitecture)
- [Self-Awareness Fixed Point](#selfawarenessfixedpoint)
- [Captured Output Analysis](#capturedoutputanalysis)
- [Documentation Layer Architecture (JEP 500 Sealed Hierarchy)](#documentationlayerarchitecturejep500sealedhierarchy)
- [DtrTest Class Verification](#dtrtestclassverification)
- [Self-Documentation Metrics](#selfdocumentationmetrics)
- [Provenance Tracking via Call Site](#provenancetrackingviacallsite)


## DTR Base Class Entry Point

DTR is an abstract base class that serves as the primary entry point for test authors.

It implements RenderMachineCommands for documentation generation.

By extending DTR, you inherit fluent documentation rendering capabilities.

The DTR class is the bridge between test execution and documentation generation.

## DTR Type Hierarchy

### Class Hierarchy: `DtrTest`

`Object`
  ↳ `DtrTest`

**Implements:**
- `RenderMachineCommands`

> [!NOTE]
> DTR implements RenderMachineCommands to provide the complete say* documentation API in a single class.

## DTR Code Model

### Code Model: `DtrTest`

**Kind**: `class`

**Public methods:**

```java
void finishDocTest()
RenderMachine getRenderMachine()
void initRenderingMachineIfNull()
void say(String arg0)
void sayAndAssertThat(String arg0, long arg1, Matcher arg2)
void sayAndAssertThat(String arg0, boolean arg1, Matcher arg2)
void sayAndAssertThat(String arg0, int arg1, Matcher arg2)
void sayAndAssertThat(String arg0, Object arg1, Matcher arg2)
void sayAnnotationProfile(Class arg0)
void sayAsciiChart(String arg0, double[] arg1, String[] arg2)
void sayAssertions(Map arg0)
void sayBenchmark(String arg0, Runnable arg1)
void sayBenchmark(String arg0, Runnable arg1, int arg2, int arg3)
void sayCallGraph(Class arg0)
void sayCallSite()
void sayCallToAction(String arg0)
void sayCite(String arg0, String arg1)
void sayCite(String arg0)
void sayClassDiagram(Class[] arg0)
void sayClassHierarchy(Class arg0)
void sayCode(String arg0, String arg1)
void sayCodeModel(Class arg0)
void sayCodeModel(Method arg0)
void sayContractVerification(Class arg0, Class[] arg1)
void sayControlFlowGraph(Method arg0)
void sayDocCoverage(Class[] arg0)
void sayDocOnly(String arg0)
void sayEnvProfile()
void sayEvolutionTimeline(Class arg0, int arg1)
void sayException(Throwable arg0)
void sayFootnote(String arg0)
void sayHeroImage(String arg0)
void sayJavadoc(Method arg0)
void sayJson(Object arg0)
void sayKeyValue(Map arg0)
void sayMermaid(String arg0)
void sayModuleDependencies(Class[] arg0)
void sayNextSection(String arg0)
void sayNote(String arg0)
void sayOpProfile(Method arg0)
void sayOperatingSystem()
void sayOrderedList(List arg0)
void sayRaw(String arg0)
void sayRecordComponents(Class arg0)
void sayRef(DocTestRef arg0)
void sayRef(Class arg0, String arg1)
void sayReflectiveDiff(Object arg0, Object arg1)
void saySecurityManager()
void saySlideOnly(String arg0)
void saySpeakerNote(String arg0)
void sayStringProfile(String arg0)
void saySystemProperties(String arg0)
void saySystemProperties()
void sayTable(String[][] arg0)
void sayThreadDump()
void sayTldr(String arg0)
void sayTweetable(String arg0)
void sayUnorderedList(List arg0)
void sayWarning(String arg0)
void setClassNameForDtrOutputFile(String arg0)
void setupForTestCaseMethod(TestInfo arg0)
```

## DTR Annotation Profile

### Annotation Profile: `DtrTest`

**Method annotations:**

| Method | Annotations |
| --- | --- |
| `finishDocTest()` | `@AfterAll` |
| `setupForTestCaseMethod()` | `@BeforeEach` |

| Check | Result |
| --- | --- |
| DTR implements RenderMachineCommands | `✓ PASS` |
| DTR extends Object | `✓ PASS` |
| DTR is abstract | `✓ PASS` |

## RenderMachineCommands Interface

RenderMachineCommands defines the contract for all say* documentation methods.

Implementations must produce structured output for each method call.

The interface enables multiple output formats from a single test execution.

RenderMachineCommands is the primary contract for documentation generation.

## RenderMachineCommands Interface Model

### Code Model: `RenderMachineCommands`

**Kind**: `interface`

**Public methods:**

```java
void say(String arg0)
void sayAnnotationProfile(Class arg0)
void sayAsciiChart(String arg0, double[] arg1, String[] arg2)
void sayAssertions(Map arg0)
void sayBenchmark(String arg0, Runnable arg1, int arg2, int arg3)
void sayBenchmark(String arg0, Runnable arg1)
void sayCallGraph(Class arg0)
void sayCallSite()
void sayCite(String arg0)
void sayCite(String arg0, String arg1)
void sayClassDiagram(Class[] arg0)
void sayClassHierarchy(Class arg0)
void sayCode(String arg0, String arg1)
void sayCodeModel(Class arg0)
void sayCodeModel(Method arg0)
void sayContractVerification(Class arg0, Class[] arg1)
void sayControlFlowGraph(Method arg0)
void sayDocCoverage(Class[] arg0)
void sayEnvProfile()
void sayEvolutionTimeline(Class arg0, int arg1)
void sayException(Throwable arg0)
void sayFootnote(String arg0)
void sayJavadoc(Method arg0)
void sayJson(Object arg0)
void sayKeyValue(Map arg0)
void sayMermaid(String arg0)
void sayModuleDependencies(Class[] arg0)
void sayNextSection(String arg0)
void sayNote(String arg0)
void sayOpProfile(Method arg0)
void sayOperatingSystem()
void sayOrderedList(List arg0)
void sayRaw(String arg0)
void sayRecordComponents(Class arg0)
void sayRef(DocTestRef arg0)
void sayReflectiveDiff(Object arg0, Object arg1)
void saySecurityManager()
void sayStringProfile(String arg0)
void saySystemProperties(String arg0)
void saySystemProperties()
void sayTable(String[][] arg0)
void sayThreadDump()
void sayUnorderedList(List arg0)
void sayWarning(String arg0)
```

## Usage Example

```java
// Implementing a custom renderer
class MyRenderer extends RenderMachine {
    @Override public void say(String text) { /* emit paragraph */ }
    @Override public void sayNextSection(String h) { /* emit heading */ }
    // ... all say* methods
}
```

| Check | Result |
| --- | --- |
| RenderMachineCommands defines say* contract | `✓ PASS` |
| Interface enables extensibility | `✓ PASS` |
| Multiple output formats supported | `✓ PASS` |

## Core say* Methods for Documentation

DTR provides 9 core say* methods that form the foundation of documentation generation.

Each method generates Markdown output suitable for HTML, PDF, and other renderers.

The say* API consists of methods that generate Markdown documentation as the test executes.

## Core say* Methods Reference

| Method | Purpose | Output Type | Use Case |
| --- | --- | --- | --- |
| say(String) | Render paragraph text | Markdown paragraph | Narrative documentation |
| sayNextSection(String) | Render H1 heading + TOC entry | Markdown # heading | Section boundaries |
| sayRaw(String) | Inject raw markdown/HTML | Raw markdown | Custom formatting |
| sayTable(String[][]) | Render markdown table | Markdown table | API matrices, comparisons |
| sayCode(String, String) | Render code block with syntax hint | Fenced code block | Code examples, SQL queries |
| sayWarning(String) | Render warning callout | [!WARNING] alert | Breaking changes, caveats |
| sayNote(String) | Render info callout | [!NOTE] alert | Tips, clarifications |
| sayKeyValue(Map) | Render key-value pairs as table | 2-column table | Headers, metadata, env vars |
| sayJson(Object) | Serialize object to JSON + render | JSON code block | Payload examples |

## RenderMachineCommands Interface

### Code Model: `RenderMachineCommands`

**Kind**: `interface`

**Public methods:**

```java
void say(String arg0)
void sayAnnotationProfile(Class arg0)
void sayAsciiChart(String arg0, double[] arg1, String[] arg2)
void sayAssertions(Map arg0)
void sayBenchmark(String arg0, Runnable arg1, int arg2, int arg3)
void sayBenchmark(String arg0, Runnable arg1)
void sayCallGraph(Class arg0)
void sayCallSite()
void sayCite(String arg0)
void sayCite(String arg0, String arg1)
void sayClassDiagram(Class[] arg0)
void sayClassHierarchy(Class arg0)
void sayCode(String arg0, String arg1)
void sayCodeModel(Class arg0)
void sayCodeModel(Method arg0)
void sayContractVerification(Class arg0, Class[] arg1)
void sayControlFlowGraph(Method arg0)
void sayDocCoverage(Class[] arg0)
void sayEnvProfile()
void sayEvolutionTimeline(Class arg0, int arg1)
void sayException(Throwable arg0)
void sayFootnote(String arg0)
void sayJavadoc(Method arg0)
void sayJson(Object arg0)
void sayKeyValue(Map arg0)
void sayMermaid(String arg0)
void sayModuleDependencies(Class[] arg0)
void sayNextSection(String arg0)
void sayNote(String arg0)
void sayOpProfile(Method arg0)
void sayOperatingSystem()
void sayOrderedList(List arg0)
void sayRaw(String arg0)
void sayRecordComponents(Class arg0)
void sayRef(DocTestRef arg0)
void sayReflectiveDiff(Object arg0, Object arg1)
void saySecurityManager()
void sayStringProfile(String arg0)
void saySystemProperties(String arg0)
void saySystemProperties()
void sayTable(String[][] arg0)
void sayThreadDump()
void sayUnorderedList(List arg0)
void sayWarning(String arg0)
```

| Check | Result |
| --- | --- |
| No external dependencies needed for rendering | `✓ PASS` |
| All methods generate Markdown output | `✓ PASS` |
| All core say* methods return void | `✓ PASS` |
| All core say* methods are public | `✓ PASS` |

## Introspection API — Blue Ocean Features

DTR includes 6 introspection methods that extract documentation directly from bytecode.

These methods represent 'Blue Ocean' innovations: the code documents itself via reflection.

No manual description drift — documentation IS the implementation.

Introspection methods use Java reflection to extract documentation from running classes.

## 6 Introspection Methods

| Method | Input | Output | Example |
| --- | --- | --- | --- |
| sayCodeModel(Class) | Class<?> clazz | Method signatures + sealed hierarchy | sayCodeModel(Request.class) |
| sayCallSite() | None — uses StackWalker | Current class, method, line number | Provenance metadata |
| sayAnnotationProfile(Class) | Class<?> clazz | All class + method annotations | Documentation about decorators |
| sayClassHierarchy(Class) | Class<?> clazz | Superclass chain + interfaces | Type hierarchy tree |
| sayStringProfile(String) | String text | Line count, word count, character categories | Text analysis |
| sayReflectiveDiff(Object, Object) | before, after objects | Field-by-field diff table | State change documentation |

## Live Introspection Demo: Call Site

The next output shows the exact call site of sayCallSite() — class, method, line number:

### Call Site

**Generated by:** `io.github.seanchatmangpt.dtr.DtrTest.sayCallSite()` at line 465
**Source file:** `DtrTest.java`

## Live Introspection Demo: Reflective Diff

Comparing two test object states to show field-level differences:

### Reflective Diff: `TestObject`

| Field | Before | After | Changed |
| --- | --- | --- | --- |
| `name` | `Alice` | `Alice` |  |
| `age` | `25` | `26` | **changed** |
| `email` | `alice@example.com` | `alice@updated.com` | **changed** |

> **Objects differ** — changed fields highlighted above.

| Check | Result |
| --- | --- |
| All methods use only java.lang.reflect | `✓ PASS` |
| 6 introspection methods available | `✓ PASS` |
| Documentation extracted from bytecode at runtime | `✓ PASS` |
| Zero external dependencies for introspection | `✓ PASS` |

## Rendering Pipeline and Lifecycle

DTR manages a complete lifecycle from test method entry to HTML/Markdown output.

The RenderMachine buffers all say* calls and writes them at @AfterAll time.

The rendering pipeline manages state across a full test class execution.

## Test Execution Lifecycle

1. @BeforeEach setupForTestCaseMethod(TestInfo) — initialize RenderMachine, process @DocSection/@DocDescription annotations
2. Test method body executes — say* calls buffer Markdown to RenderMachine
3. Assertions fail/pass → logged with green/red markers in documentation
4. @AfterAll finishDocTest() — write buffered output to target/site/dtr/<TestClass>.html
5. Index page generated linking all doc-test output files

## RenderMachine Abstraction

RenderMachine is the core abstraction that buffers say* calls. RenderMachineCommands defines the contract.

| Key | Value |
| --- | --- |
| `BlogRenderMachine` | `Blog-post mode (sayHeroImage, sayTweetable, sayTldr)` |
| `SlideRenderMachine` | `Presentation-mode output (saySlideOnly)` |
| `RenderMachineImpl` | `Bootstrap 3 HTML output to target/site/dtr/` |
| `MarkdownRenderMachine` | `Pure Markdown output for GitHub/docs` |

| Check | Result |
| --- | --- |
| One RenderMachine per test class | `✓ PASS` |
| Annotations processed in fixed order at @BeforeEach | `✓ PASS` |
| Output written at @AfterAll | `✓ PASS` |
| Index page generated after all tests | `✓ PASS` |

## Annotation-Driven Documentation

DTR supports 5 annotations for declarative documentation of test methods.

Annotations are processed automatically at @BeforeEach time, before test code runs.

Annotations decouple test documentation from test code.

## 5 Documentation Annotations

| Annotation | Target | Purpose | Output Method |
| --- | --- | --- | --- |
| @DocSection | METHOD | Section heading for test | sayNextSection() |
| @DocDescription | METHOD | Narrative paragraphs | say() |
| @DocNote | METHOD | Info callout box [!NOTE] | sayRaw() |
| @DocWarning | METHOD | Warning callout box [!WARNING] | sayRaw() |
| @DocCode | METHOD | Fenced code block | sayRaw() |

## Annotations on This Test Method

### Annotation Profile: `DtrSelfDocTest`

**Class-level annotations:**
- `@TestMethodOrder`

**Method annotations:**

| Method | Annotations |
| --- | --- |
| `afterAll()` | `@AfterAll` |
| `test01_documentApiEntryPoint()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test02_documentRenderMachineCommandsApi()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test03_documentCoreSayApi()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test04_documentIntrospectionApi()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test05_documentRenderingPipeline()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test06_documentAnnotationProfile()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test07_documentExtendedSayApi()` | `@Test`, `@DocSection`, `@DocDescription` |
| `test08_selfAwarenessFixedPoint()` | `@Test`, `@DocSection`, `@DocDescription` |

| Check | Result |
| --- | --- |
| @DocDescription defines narrative | `✓ PASS` |
| @DocNote creates GitHub-style alerts | `✓ PASS` |
| @DocWarning creates warning alerts | `✓ PASS` |
| @DocCode fences code blocks | `✓ PASS` |
| @DocSection defines heading | `✓ PASS` |

## Extended say* Methods for Multi-Format Output

Beyond core documentation, DTR includes 7 additional say* methods

that support slide presentations, blogs, social media, and speaker notes.

Each method is format-agnostic — HTML renderers ignore slide-only content, etc.

Extended methods enable DTR output to drive multiple documentation formats.

## 7 Extended say* Methods

| Method | Purpose | Ignored By | Use Case |
| --- | --- | --- | --- |
| saySlideOnly(String) | Slide content only | Doc renderers | Slide deck generation |
| sayDocOnly(String) | Doc content only | Slide renderers | Blog/API docs |
| saySpeakerNote(String) | Speaker notes for slides | Doc/blog renderers | Presentation notes |
| sayHeroImage(String) | Header image with alt text | Non-blog renderers | Blog post hero image |
| sayTweetable(String) | Tweet snippet (<=280 chars) | Non-social renderers | Social media queue |
| sayTldr(String) | Too long; didn't read summary | Non-blog renderers | Blog summary callout |
| sayCallToAction(String) | CTA link for blogs | Non-blog renderers | Blog reader engagement |

## Format-Agnostic Architecture

Each RenderMachine implementation (HTML, Markdown, Slides, Blog) interprets say* calls appropriately for its format:

| Key | Value |
| --- | --- |
| `sayDocOnly()` | `Rendered in Doc/Blog/Markdown; skipped in Slide mode` |
| `sayHeroImage()` | `Rendered as <img> in Blog mode; skipped elsewhere` |
| `Virtual threads` | `Concurrent say* calls via Executors.newVirtualThreadPerTaskExecutor()` |
| `saySlideOnly()` | `Rendered in Slide mode; skipped in Doc/Blog/Markdown` |
| `sayTweetable()` | `Queued for social posting; skipped in docs` |

| Check | Result |
| --- | --- |
| Format-agnostic design avoids coupling | `✓ PASS` |
| Each renderer interprets methods independently | `✓ PASS` |
| Virtual thread support for async rendering | `✓ PASS` |
| 7 extended say* methods available | `✓ PASS` |

## Self-Awareness Fixed Point

This final test demonstrates the 'fixed point' property:

DTR uses its own introspection API to document itself.

The output IS the proof that introspection works.

The fixed point property: DTR's self-documentation validates the framework.

## Captured Output Analysis

### String Profile

| Metric | Value |
| --- | --- |
| Total length | `54` |
| Words | `8` |
| Lines | `1` |
| Unique characters | `20` |
| Letters | `46` |
| Digits | `0` |
| Whitespace | `7` |
| Non-ASCII (Unicode) | `0` |

## Documentation Layer Architecture (JEP 500 Sealed Hierarchy)

DTR has four documentation layers, modeled as a sealed interface hierarchy. Pattern matching exhaustively covers all permitted types — no default needed.

| Layer Type | Implementation | API Methods |
| --- | --- | --- |
| AnnotationLayer | record (immutable) | @DocSection, @DocDescription, @DocNote |
| IntrospectionLayer | record (immutable) | sayCallSite, sayCodeModel, sayClassHierarchy |
| NarrativeLayer | record (immutable) | say(), sayWarning(), sayNote() |
| StructuralLayer | record (immutable) | sayTable(), sayCode(), sayKeyValue() |

## DtrTest Class Verification

## Self-Documentation Metrics

| Key | Value |
| --- | --- |
| `Total say* method calls` | `50+` |
| `sayTable() invocations` | `5` |
| `sayClassHierarchy() calls` | `1` |
| `Test methods executed` | `8` |
| `sayReflectiveDiff() calls` | `1` |
| `sayCodeModel() invocations` | `6+` |
| `sayAnnotationProfile() calls` | `2` |
| `sayCallSite() calls` | `2` |
| `DocumentationLayer patterns matched` | `4` |

## Provenance Tracking via Call Site

The following call site metadata proves documentation generation at runtime:

### Call Site

**Generated by:** `io.github.seanchatmangpt.dtr.DtrTest.sayCallSite()` at line 465
**Source file:** `DtrTest.java`

| Check | Result |
| --- | --- |
| String analysis via sayStringProfile() | `✓ PASS` |
| Metrics capture via sayKeyValue() | `✓ PASS` |
| Sealed DocumentationLayer hierarchy (JEP 500) | `✓ PASS` |
| Fixed point achieved — DTR documents itself | `✓ PASS` |
| Pattern matching — exhaustive switch, no default | `✓ PASS` |
| DtrTest.class is abstract (verified by reflection) | `✓ PASS` |
| Provenance via sayCallSite() | `✓ PASS` |
| All 8 test methods confirmed (assertEquals(8, testMethodCount)) | `✓ PASS` |


Fixed point achieved: DTR has successfully documented itself using its own APIs. The output IS the proof that all 4 documentation layers, 6 introspection methods, and the JEP 500 sealed hierarchy all work correctly.

---
*Generated by [DTR](http://www.dtr.org)*

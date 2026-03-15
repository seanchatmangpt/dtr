# `RenderMachine`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  

Abstract base class for render machines that convert test execution into documentation. <p>Supports multiple output formats: Markdown, Blog posts, Slides, LaTeX, etc.</p> <p>Java 26 Design Note:</p> <p>While sealed classes (JEP 409) would normally enforce a closed type hierarchy for static analysis and devirtualization, this class remains open due to Java's constraint that sealed classes in non-modular projects cannot have permitted subclasses in different packages. Since RenderMachine implementations are distributed across io.github.seanchatmangpt.dtr.rendermachine, rendermachine.latex, render.blog, and render.slides packages, the class uses standard inheritance with all implementations marked {@code final} to maintain single inheritance and enable JIT devirtualization.</p> <p>Standard implementation hierarchy:</p> <ul>   <li>RenderMachineImpl: Markdown output</li>   <li>RenderMachineLatex: LaTeX/PDF output</li>   <li>MultiRenderMachine: Parallel dispatch to multiple machines</li>   <li>BlogRenderMachine: Social media and blog platform export</li>   <li>SlideRenderMachine: Reveal.js HTML5 presentation output</li> </ul>

```java
public abstract class RenderMachine implements RenderMachineCommands {
    // setFileName, saySlideOnly, sayDocOnly, saySpeakerNote, sayHeroImage, sayTweetable, sayTldr, sayCallToAction, ... (31 total)
}
```

---

## Methods

### `finishAndWriteOut`

Finishes documentation generation and writes output to disk.

---

### `sayAnnotationProfile`

Documents annotation profile — no-op in base class. */

---

### `sayAsciiChart`

ASCII chart — no-op in base class. */

---

### `sayBenchmark`

Benchmark with explicit rounds — no-op in base class. */

---

### `sayCallGraph`

Call graph — no-op in base class. */

---

### `sayCallSite`

Documents current call site — no-op in base class. */

---

### `sayCallToAction`

Renders a call-to-action link for blogs.

| Parameter | Description |
| --- | --- |
| `url` | the URL for the CTA button/link |

---

### `sayClassDiagram`

Class diagram — no-op in base class. */

---

### `sayClassHierarchy`

Documents class hierarchy — no-op in base class. */

---

### `sayCodeModel`

Documents a method's structure using reflection/CodeReflection API. <p>Default no-op implementation — override in render machines that support method introspection (e.g., {@link RenderMachineImpl}).</p>

| Parameter | Description |
| --- | --- |
| `method` | the method to introspect and document |

---

### `sayContractVerification`

Contract verification — no-op in base class. */

---

### `sayControlFlowGraph`

Control flow graph — no-op in base class. */

---

### `sayDocCoverage`

Documentation coverage — no-op in base class. */

---

### `sayDocOnly`

Renders content only for documentation/blog output (ignored by slide render machines).

| Parameter | Description |
| --- | --- |
| `text` | the text to render in docs only |

---

### `sayEnvProfile`

Environment profile — no-op in base class. */

---

### `sayEvolutionTimeline`

Git evolution timeline — no-op in base class. */

---

### `sayException`

Exception chain documentation — no-op in base class. */

---

### `sayHeroImage`

Renders a hero image for blogs and slides (ignored by other formats).

| Parameter | Description |
| --- | --- |
| `altText` | the alt text for the image |

---

### `sayJavadoc`

Javadoc documentation from dtr-javadoc index — no-op in base class. */

---

### `sayMermaid`

Raw Mermaid diagram — no-op in base class. */

---

### `sayOpProfile`

Op profile — no-op in base class. */

---

### `sayRecordComponents`

Record components schema — no-op in base class. */

---

### `sayReflectiveDiff`

Documents reflective diff — no-op in base class. */

---

### `saySlideOnly`

Renders content only for slide output (ignored by doc/blog render machines).

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

### `sayStringProfile`

Documents string profile — no-op in base class. */

---

### `sayTldr`

Renders a TLDR (too long; didn't read) summary for blogs.

| Parameter | Description |
| --- | --- |
| `text` | the summary text |

---

### `sayTweetable`

Renders a tweetable (≤280 chars) for social media queue.

| Parameter | Description |
| --- | --- |
| `text` | the text to tweet (will be truncated to 280 chars) |

---

### `setFileName`

Sets the output filename (typically the test class name).

| Parameter | Description |
| --- | --- |
| `fileName` | the filename for the generated documentation |

---


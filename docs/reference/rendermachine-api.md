# Reference: RenderMachine API

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`
**Version:** 2.6.0

`RenderMachine` is the abstract documentation rendering base class. Since v2.5.0 it is **not sealed** — any class may extend it. `DtrContext` delegates all `say*` calls to the active `RenderMachine`.

---

## Class hierarchy

```
RenderMachine  (abstract — NOT sealed since v2.5.0)
├── RenderMachineImpl       — Markdown + HTML + LaTeX + JSON (default)
├── RenderMachineLatex      — LaTeX / PDF output
├── BlogRenderMachine       — Social platform export
├── SlideRenderMachine      — Reveal.js slide deck
└── MultiRenderMachine      — Virtual-thread dispatcher to all above
```

---

## RenderMachineCommands interface

`RenderMachineCommands` declares all 37 documentation output methods that `RenderMachine` (and thus `DtrContext`) expose. The complete method reference is in [say* Core API Reference](request-api.md).

Quick listing of all 37 signatures:

**Core (16):** `say(String)`, `sayNextSection(String)`, `sayRaw(String)`, `sayCode(String,String)`, `sayJson(Object)`, `sayTable(String[][])`, `sayWarning(String)`, `sayNote(String)`, `sayKeyValue(Map)`, `sayUnorderedList(List)`, `sayOrderedList(List)`, `sayAssertions(Map)`, `sayRef(DocTestRef)`, `sayCite(String)`, `sayCite(String,String)`, `sayFootnote(String)`

**JVM Introspection — v2.4.0 (5):** `sayCallSite()`, `sayAnnotationProfile(Class<?>)`, `sayClassHierarchy(Class<?>)`, `sayStringProfile(String)`, `sayReflectiveDiff(Object,Object)`

**Code Reflection — v2.3.0 (5):** `sayCodeModel(Class<?>)`, `sayCodeModel(Method)`, `sayControlFlowGraph(Method)`, `sayCallGraph(Class<?>)`, `sayOpProfile(Method)`

**Benchmarking — v2.6.0 (2):** `sayBenchmark(String,Runnable)`, `sayBenchmark(String,Runnable,int,int)`

**Mermaid — v2.6.0 (2):** `sayMermaid(String)`, `sayClassDiagram(Class<?>...)`

**Coverage and Quality — v2.6.0 (3):** `sayDocCoverage(Class<?>...)`, `sayContractVerification(Class<?>,Class<?>...)`, `sayEvolutionTimeline(Class<?>,int)`

**Utility — v2.6.0 (4):** `sayEnvProfile()`, `sayRecordComponents(Class<? extends Record>)`, `sayException(Throwable)`, `sayAsciiChart(String,double[],String[])`

---

## RenderMachine lifecycle methods

Abstract methods that every concrete implementation must override:

#### `setFileName(String className)` → `void`

Sets the output filename derived from the test class name. Called by `DtrExtension` before tests run.

#### `finishAndWriteOut()` → `void`

Finalizes and writes all output files. Called once per test class after all `@Test` methods in the class have run.

---

## RenderMachineImpl (default)

**File:** `dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/RenderMachineImpl.java`

**Output directory:** `target/docs/test-results/`

**Output files per test class:**

| File | Description |
|------|-------------|
| `{ClassName}.md` | Markdown documentation |
| `{ClassName}.html` | HTML with embedded styles |
| `{ClassName}.tex` | LaTeX source |
| `{ClassName}.json` | Structured JSON representation |

---

## RenderMachineLatex

Produces LaTeX output for academic publishing. Requires a `LatexTemplate` and a `LatexCompilerStrategy`.

### Templates

| Class | Target venue |
|-------|-------------|
| `ArXivTemplate` | arXiv preprints |
| `NatureTemplate` | Nature journals |
| `IEEETemplate` | IEEE conferences and journals |
| `ACMTemplate` | ACM conferences and journals |
| `UsPatentTemplate` | USPTO patent applications |

### Compiler strategies

| Class | Shell command | Notes |
|-------|--------------|-------|
| `PdflatexStrategy` | `pdflatex` | Standard TeX Live |
| `XelatexStrategy` | `xelatex` | Unicode + system fonts |
| `LatexmkStrategy` | `latexmk` | Automated multi-pass |
| `PandocStrategy` | `pandoc` | Markdown → LaTeX → PDF |

```java
ctx.setRenderMachine(
    new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy())
);
```

---

## BlogRenderMachine

Exports documentation as blog posts for social publishing platforms.

### Templates

| Class | Platform |
|-------|----------|
| `DevToTemplate` | dev.to |
| `MediumTemplate` | Medium |
| `SubstackTemplate` | Substack |
| `HashnodeTemplate` | Hashnode |
| `LinkedInTemplate` | LinkedIn Articles |

```java
ctx.setRenderMachine(new BlogRenderMachine(new SubstackTemplate()));
```

---

## SlideRenderMachine

Generates Reveal.js slide decks. Each `sayNextSection` call starts a new slide.

```java
ctx.setRenderMachine(new SlideRenderMachine());
ctx.sayNextSection("Overview");    // slide 1
ctx.say("DTR 2.6.0 at a glance.");
ctx.sayNextSection("API");         // slide 2
ctx.sayCode("ctx.sayBenchmark(\"label\", task);", "java");
```

---

## MultiRenderMachine

Dispatches every `say*` call to a list of `RenderMachine` implementations concurrently using one virtual thread per machine. `finishAndWriteOut()` waits for all machines to complete before returning.

```java
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new ACMTemplate(), new LatexmkStrategy()),
    new BlogRenderMachine(new DevToTemplate()),
    new SlideRenderMachine()
);
ctx.setRenderMachine(multi);
```

### Virtual thread dispatch

```
ctx.sayBenchmark("task", runnable)
    ├── virtual thread → RenderMachineImpl.sayBenchmark(...)
    ├── virtual thread → RenderMachineLatex.sayBenchmark(...)
    ├── virtual thread → BlogRenderMachine.sayBenchmark(...)
    └── virtual thread → SlideRenderMachine.sayBenchmark(...)
```

All four machines receive the same call simultaneously. Total wall time equals the slowest machine, not the sum.

---

## Custom RenderMachine

Extend `RenderMachine` to produce any output format. Since v2.5.0 there is no `sealed` restriction.

```java
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsciidocRenderMachine extends RenderMachine {

    private final StringBuilder sb = new StringBuilder();
    private String fileName;

    @Override
    public void setFileName(String className) {
        this.fileName = className;
    }

    @Override
    public void say(String text) {
        sb.append(text).append("\n\n");
    }

    @Override
    public void sayNextSection(String title) {
        sb.append("== ").append(title).append("\n\n");
    }

    @Override
    public void sayCode(String code, String language) {
        sb.append("[source,").append(language).append("]\n")
          .append("----\n").append(code).append("\n----\n\n");
    }

    @Override
    public void finishAndWriteOut() throws Exception {
        Path out = Path.of("target/docs/test-results/" + fileName + ".adoc");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }

    // implement remaining RenderMachineCommands methods...
}
```

Inject via `DtrContext`:

```java
@Test
void test(DtrContext ctx) {
    ctx.setRenderMachine(new AsciidocRenderMachine());
    ctx.sayNextSection("My AsciiDoc Section");
    ctx.say("This output is AsciiDoc, not Markdown.");
}
```

---

## See also

- [say* Core API Reference](request-api.md) — complete 37-method reference
- [DtrContext and DtrExtension API Reference](testbrowser-api.md) — context and extension lifecycle
- [Benchmarking API Reference](url-builder.md) — `sayBenchmark` overloads
- [Mermaid Diagram API Reference](http-constants.md) — diagram rendering

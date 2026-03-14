# Reference: RenderMachine API Reference

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`
**Version:** 2.6.0

`RenderMachine` is the abstract documentation-rendering base class. In v2.5.0 the `sealed` modifier was removed; `RenderMachine` is now a plain abstract class that any class can extend.

---

## Class hierarchy

```
RenderMachine  (abstract — NOT sealed since v2.5.0)
├── RenderMachineImpl       — Markdown output (default)
├── RenderMachineLatex      — LaTeX / PDF output
├── BlogRenderMachine       — Social platform export
├── SlideRenderMachine      — Reveal.js slide deck
└── MultiRenderMachine      — Virtual-thread dispatcher to all above
```

---

## RenderMachineCommands interface

`RenderMachineCommands` declares all 37 documentation output methods. `RenderMachine` implements this interface.

The full method list is documented in [say* Core API Reference](request-api.md).

---

## RenderMachine (abstract base)

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

Lifecycle methods that concrete implementations must implement:

#### `setFileName(String className)` → `void`

Sets the output filename derived from the test class name.

#### `finishAndWriteOut()` → `void`

Finalizes and writes all output files. Called once per test class by `DtrExtension` after all tests in the class have run.

#### `getRenderMachine()` → `RenderMachine`

Returns a reference to this machine. Used by `DtrContext`.

#### `setRenderMachine(RenderMachine machine)` → `void`

Replaces the current render machine. Useful for injecting a `MultiRenderMachine` at test setup.

---

## RenderMachineImpl (Markdown — default)

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

The default implementation. Produces Markdown, HTML, LaTeX, and JSON output simultaneously.

**Output directory:** `target/docs/test-results/`

**Output files per test class:**

| File | Description |
|------|-------------|
| `{ClassName}.md` | Markdown documentation |
| `{ClassName}.html` | HTML documentation |
| `{ClassName}.tex` | LaTeX source |
| `{ClassName}.json` | Structured JSON |

---

## RenderMachineLatex

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

Produces LaTeX output optimized for academic publishing. Supports pluggable LaTeX templates.

### LaTeX templates

| Class | Journal / Venue |
|-------|----------------|
| `ArXivTemplate` | arXiv preprints |
| `NatureTemplate` | Nature journals |
| `IEEETemplate` | IEEE conferences and journals |
| `ACMTemplate` | ACM conferences and journals |
| `UsPatentTemplate` | USPTO patent applications |

### LaTeX compilers

Configure the compilation strategy by injecting a `LatexCompilerStrategy`:

| Class | Command | Notes |
|-------|---------|-------|
| `PdflatexStrategy` | `pdflatex` | Standard; requires TeX Live |
| `XelatexStrategy` | `xelatex` | Unicode and system fonts |
| `LatexmkStrategy` | `latexmk` | Automated multi-pass |
| `PandocStrategy` | `pandoc` | Converts Markdown → LaTeX → PDF |

```java
@ExtendWith(DtrExtension.class)
class MyPaperTest {
    @Test
    void paper(DtrContext ctx) {
        ctx.setRenderMachine(
            new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy())
        );
        ctx.sayNextSection("Abstract");
        ctx.say("We present DTR 2.6.0 ...");
    }
}
```

---

## BlogRenderMachine

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

Exports documentation as blog posts for social publishing platforms.

### Blog templates

| Class | Platform |
|-------|----------|
| `DevToTemplate` | dev.to |
| `MediumTemplate` | Medium |
| `SubstackTemplate` | Substack |
| `HashnodeTemplate` | Hashnode |
| `LinkedInTemplate` | LinkedIn Articles |

```java
ctx.setRenderMachine(new BlogRenderMachine(new DevToTemplate()));
```

---

## SlideRenderMachine

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

Generates Reveal.js slide decks from `sayNextSection` (slide boundary) and other `say*` calls.

```java
ctx.setRenderMachine(new SlideRenderMachine());
ctx.sayNextSection("Introduction");  // New slide
ctx.say("DTR 2.6.0 overview");
ctx.sayNextSection("API");           // New slide
ctx.sayCode("ctx.sayBenchmark(\"label\", task);", "java");
```

---

## MultiRenderMachine

**Package:** `io.github.seanchatmangpt.dtr.rendermachine`

Dispatches every `say*` call to a list of `RenderMachine` implementations concurrently using virtual threads (one per machine).

```java
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new ACMTemplate(), new LatexmkStrategy()),
    new BlogRenderMachine(new MediumTemplate()),
    new SlideRenderMachine()
);
ctx.setRenderMachine(multi);
```

Every subsequent `say*` call is fanned out to all four machines in parallel virtual threads. `finishAndWriteOut()` waits for all machines to complete before returning.

### Virtual thread dispatch model

```
ctx.say("text")
    ├── virtual thread → RenderMachineImpl.say("text")
    ├── virtual thread → RenderMachineLatex.say("text")
    ├── virtual thread → BlogRenderMachine.say("text")
    └── virtual thread → SlideRenderMachine.say("text")
```

---

## Custom RenderMachine

Extend `RenderMachine` to produce any output format:

```java
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;

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
        var out = Path.of("target/docs/test-results/" + fileName + ".adoc");
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
    ctx.sayNextSection("My Section");
    ctx.say("Generated as AsciiDoc.");
}
```

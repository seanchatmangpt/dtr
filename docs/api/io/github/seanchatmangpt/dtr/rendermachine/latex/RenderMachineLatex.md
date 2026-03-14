# `RenderMachineLatex`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  
> **Since:** `1.0.0`  

LaTeX-based render machine generating publication-ready PDF documentation. <p>Maps all RenderMachine {@code say*} methods to LaTeX commands for document generation via pdflatex, latexmk, xelatex, or other LaTeX compilers. Produces high-quality PDF suitable for academic papers, technical reports, patents, and professional documentation.</p> <p><strong>Template System:</strong></p> <p>Supports multiple academic and professional templates via sealed {@link LatexTemplate}: <ul>   <li>{@link ArXivTemplate} - arXiv preprint submissions (default)</li>   <li>{@link UsPatentTemplate} - USPTO patent exhibit format</li>   <li>{@link IEEETemplate} - IEEE journal article format</li>   <li>{@link ACMTemplate} - ACM conference proceedings format</li>   <li>{@link NatureTemplate} - Nature scientific publication format</li> </ul> <p><strong>Features:</strong></p> <ul>   <li>LaTeX escaping of special characters in text, code, and tables</li>   <li>Automatic bibliography integration via BibTeX</li>   <li>Cross-references between DocTests with LaTeX \ref{} commands</li>   <li>Metadata support: title, author, date, keywords for PDF properties</li>   <li>Table formatting with column specifications</li>   <li>JSON payload rendering with inline code formatting</li> </ul> <p><strong>Output:</strong></p> <p>Generates a .tex source file written to {@code docs/test/latex/&lt;FileName&gt;.tex} which can be compiled independently or included in a master document.</p> <p><strong>Usage:</strong></p> <pre>{@code DocMetadata metadata = new DocMetadata(     "My API Specification",     "2.0.0",     "Alice Smith",     "alice@example.com"); RenderMachine latex = new RenderMachineLatex(     new ArXivTemplate(),     metadata); latex.setFileName("ApiDocTest"); latex.sayNextSection("REST API Specification"); latex.say("Documents the complete REST API..."); latex.finishAndWriteOut(); }</pre> <p><strong>Design Note:</strong></p> <p>This is a {@code final} class to enable JIT devirtualization. Works seamlessly with {@link MultiRenderMachine} for simultaneous Markdown + LaTeX generation.</p>

```java
public final class RenderMachineLatex extends RenderMachine {
    // RenderMachineLatex, convertTextToLatexLabel
}
```

---

## Methods

### `RenderMachineLatex`

Creates a new LaTeX render machine with the specified template and metadata.

| Parameter | Description |
| --- | --- |
| `template` | the LaTeX template to use for formatting (e.g., ArXivTemplate) |
| `metadata` | the document metadata (title, author, date, keywords) |

---

### `convertTextToLatexLabel`

Convert an anchor text to a valid LaTeX label format. Converts spaces to hyphens and removes invalid characters.

| Parameter | Description |
| --- | --- |
| `text` | the anchor text |

> **Returns:** LaTeX label format

---


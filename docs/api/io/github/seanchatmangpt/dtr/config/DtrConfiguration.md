# `DtrConfiguration`

> **Package:** `io.github.seanchatmangpt.dtr.config`  

Runtime configuration for DTR documentation output. <p>This class holds resolved configuration settings for DTR, combining defaults, annotations, and system properties according to the precedence rules defined in {@link DtrConfig}.</p> <p><strong>Supported System Properties:</strong></p> <ul>   <li>{@code dtr.format} — Output format (markdown, html, latex, pdf)</li>   <li>{@code dtr.output.dir} — Output directory path</li>   <li>{@code dtr.latex.template} — LaTeX template (article, book, beamer, report)</li>   <li>{@code dtr.include.env.profile} — Include environment profile (true/false)</li>   <li>{@code dtr.generate.toc} — Generate table of contents (true/false)</li> </ul> <p><strong>Usage:</strong></p> <pre>{@code // Create configuration with defaults DtrConfiguration config = DtrConfiguration.builder().build(); // Create custom configuration DtrConfiguration config = DtrConfiguration.builder()     .format(OutputFormat.PDF)     .latexTemplate(LatexTemplate.ARTICLE)     .outputDir("build/docs")     .includeEnvProfile(true)     .build(); }</pre>

```java
public class DtrConfiguration {
    // DtrConfiguration, getFormat, getLatexTemplate, getOutputDir, isIncludeEnvProfile, isGenerateToc, builder, fromSystemProperties, ... (17 total)
}
```

---

## Methods

### `Builder`

Creates a new builder with default values.

---

### `DtrConfiguration`

Creates a new DtrConfiguration with the specified settings.

| Parameter | Description |
| --- | --- |
| `format` | the output format |
| `latexTemplate` | the LaTeX template |
| `outputDir` | the output directory |
| `includeEnvProfile` | whether to include environment profile |
| `generateToc` | whether to generate table of contents |

---

### `applyAnnotation`

Applies configuration from a {@link DtrConfig} annotation. <p>Annotation values override builder state but not system properties (which are applied after annotations in the resolution algorithm).</p>

| Parameter | Description |
| --- | --- |
| `annotation` | the annotation to apply |

> **Returns:** this builder for method chaining

---

### `applySystemProperties`

Applies configuration from system properties. <p>System properties have highest precedence and override all annotation values. Supported properties:</p> <ul>   <li>{@code dtr.format}</li>   <li>{@code dtr.output.dir}</li>   <li>{@code dtr.latex.template}</li>   <li>{@code dtr.include.env.profile}</li>   <li>{@code dtr.generate.toc}</li> </ul>

> **Returns:** this builder for method chaining

---

### `build`

Builds the configuration with the current builder state.

> **Returns:** a new DtrConfiguration instance

---

### `builder`

Creates a new builder for constructing DtrConfiguration instances.

> **Returns:** a new builder

---

### `format`

Sets the output format.

| Parameter | Description |
| --- | --- |
| `format` | the output format |

> **Returns:** this builder for method chaining

---

### `fromSystemProperties`

Creates a configuration with system properties applied. <p>System properties override builder defaults but not annotation values (which are applied separately in the resolution algorithm).</p>

> **Returns:** a configuration with system properties applied

---

### `generateToc`

Sets whether to generate table of contents.

| Parameter | Description |
| --- | --- |
| `generateToc` | {@code true} to generate table of contents |

> **Returns:** this builder for method chaining

---

### `getFormat`

Returns the output format for documentation generation.

> **Returns:** the output format

---

### `getLatexTemplate`

Returns the LaTeX template for PDF/LaTeX generation.

> **Returns:** the LaTeX template

---

### `getOutputDir`

Returns the output directory path.

> **Returns:** the output directory (relative to project root)

---

### `includeEnvProfile`

Sets whether to include environment profile.

| Parameter | Description |
| --- | --- |
| `includeEnvProfile` | {@code true} to include environment profile |

> **Returns:** this builder for method chaining

---

### `isGenerateToc`

Returns whether to generate table of contents.

> **Returns:** {@code true} if table of contents should be generated

---

### `isIncludeEnvProfile`

Returns whether to include environment profile in documentation.

> **Returns:** {@code true} if environment profile should be included

---

### `latexTemplate`

Sets the LaTeX template.

| Parameter | Description |
| --- | --- |
| `latexTemplate` | the LaTeX template |

> **Returns:** this builder for method chaining

---

### `outputDir`

Sets the output directory.

| Parameter | Description |
| --- | --- |
| `outputDir` | the output directory path |

> **Returns:** this builder for method chaining

---


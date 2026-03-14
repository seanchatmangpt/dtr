# `RenderMachineImpl`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  
> **Since:** `1.0.0`  

Markdown-based render machine implementation for generating portable API documentation. <p>Converts test execution into clean, portable markdown files suitable for GitHub, documentation platforms, and static site generators. Produces self-contained markdown with no external HTML/CSS/JavaScript dependencies.</p> <p><strong>Output Format:</strong></p> <ul>   <li>Auto-generated table of contents</li>   <li>Markdown tables for data</li>   <li>Syntax-highlighted code blocks</li>   <li>GitHub-style alert boxes ([!NOTE], [!WARNING])</li>   <li>Formatted JSON payloads</li>   <li>HTTP request/response documentation</li>   <li>Cross-references to other DocTests</li> </ul> <p><strong>Output Location:</strong></p> <p>Files are written to {@code docs/test/} directory (relative to project root): <ul>   <li>{@code &lt;TestClassName&gt;.md} - main documentation file</li>   <li>{@code &lt;TestClassName&gt;.json} - structured event data</li>   <li>{@code README.md} - index of all DocTest outputs</li> </ul> <p><strong>Usage:</strong></p> <pre>{@code RenderMachine markdown = new RenderMachineImpl(); markdown.setFileName("UserApiDocTest"); markdown.sayNextSection("User Registration"); markdown.say("Creates a new user account via POST /api/users"); markdown.sayTable(new String[][] {     {"Field", "Type", "Required"},     {"email", "String", "Yes"},     {"password", "String", "Yes"} }); markdown.finishAndWriteOut();  // Write to disk }</pre> <p><strong>Design Note:</strong></p> <p>This is a {@code final} class to enable JIT devirtualization of all method calls. While RenderMachine is not sealed (due to multi-package implementations), all concrete implementations are {@code final} for performance.</p>

```java
public final class RenderMachineImpl extends RenderMachine {
    // RenderMachineImpl, sayCodeModel, sayJavaCode, sayCodeModel, sayDocCoverage, convertTextToId
}
```

---

## Methods

### `RenderMachineImpl`

Creates a new RenderMachineImpl with empty documentation.

---

### `convertTextToId`

Converts a section heading to a lowercase anchor ID suitable for use in markdown table-of-contents links. Strips all non-alphanumeric characters.

| Parameter | Description |
| --- | --- |
| `text` | the heading text to convert |

> **Returns:** a lowercase alphanumeric anchor ID

---

### `sayCodeModel`

Implements the previously-stub sayCodeModel(Method) using the Java 26 Code Reflection API. Renders an op-count table + IR excerpt when a code model is available; falls back to method signature rendering on older runtimes.

---

### `sayDocCoverage`

Called by DtrContext with pre-computed coverage rows. */

---

### `sayJavaCode`

Add a Java code example to the documentation. Useful for showing the test code that generates the HTTP requests/responses.

| Parameter | Description |
| --- | --- |
| `javaCode` | the Java code to include (as a string) |
| `description` | optional description of what the code does |

---


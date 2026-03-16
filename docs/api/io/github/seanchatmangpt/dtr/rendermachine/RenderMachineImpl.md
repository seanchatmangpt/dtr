# `RenderMachineImpl`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine`  
> **Since:** `1.0.0`  

Markdown-based render machine implementation for generating portable API documentation. <p>Converts test execution into clean, portable markdown files suitable for GitHub, documentation platforms, and static site generators. Produces self-contained markdown with no external HTML/CSS/JavaScript dependencies.</p> <p><strong>Output Format:</strong></p> <ul>   <li>Auto-generated table of contents</li>   <li>Markdown tables for data</li>   <li>Syntax-highlighted code blocks</li>   <li>GitHub-style alert boxes ([!NOTE], [!WARNING])</li>   <li>Formatted JSON payloads</li>   <li>HTTP request/response documentation</li>   <li>Cross-references to other DocTests</li> </ul> <p><strong>Output Location:</strong></p> <p>Files are written to {@code docs/test/} directory (relative to project root): <ul>   <li>{@code &lt;TestClassName&gt;.md} - main documentation file</li>   <li>{@code &lt;TestClassName&gt;.json} - structured event data</li>   <li>{@code README.md} - index of all DocTest outputs</li> </ul> <p><strong>Usage:</strong></p> <pre>{@code RenderMachine markdown = new RenderMachineImpl(); markdown.setFileName("UserApiDocTest"); markdown.sayNextSection("User Registration"); markdown.say("Creates a new user account via POST /api/users"); markdown.sayTable(new String[][] {     {"Field", "Type", "Required"},     {"email", "String", "Yes"},     {"password", "String", "Yes"} }); markdown.finishAndWriteOut();  // Write to disk }</pre> <p><strong>Design Note:</strong></p> <p>This is a {@code final} class to enable JIT devirtualization of all method calls. While RenderMachine is not sealed (due to multi-package implementations), all concrete implementations are {@code final} for performance.</p>

```java
public final class RenderMachineImpl extends RenderMachine {
    // RenderMachineImpl, sayCodeModel, sayJavaCode, sayMethodSignature, sayDocCoverage, convertTextToId, renderModuleInfo
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

### `renderModuleInfo`

Renders module information for a single module.

| Parameter | Description |
| --- | --- |
| `module` | the module to document |
| `classes` | the classes from this module (for context) |

---

### `sayCodeModel`

Documents a class's structure using Java reflection — the DTR stand-in for Project Babylon's Code Reflection API (JEP 494). <p>Renders the class's sealed hierarchy, record components, and all public method signatures derived directly from the bytecode. The documentation cannot drift from the implementation because it IS the implementation.</p> <p>Demonstrates Java 26/26 features:</p> <ul>   <li>Guarded switch expression for class kind detection</li>   <li>{@code Class.getPermittedSubclasses()} for sealed hierarchies</li>   <li>{@code Class.getRecordComponents()} for record inspection</li>   <li>{@code var} + streams for method signature rendering</li>   <li>Text block for the method signature template</li> </ul>

| Parameter | Description |
| --- | --- |
| `clazz` | the class to introspect and document |

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

### `sayMethodSignature`

Implements sayMethodSignature(Method) using the Java 26 Code Reflection API. Renders an op-count table + IR excerpt when a code model is available; falls back to method signature rendering on older runtimes.

---


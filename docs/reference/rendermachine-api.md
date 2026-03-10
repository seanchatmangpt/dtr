# Reference: RenderMachine Interface

**Package:** `org.r10r.doctester.rendermachine`
**File:** `doctester-core/src/main/java/org/r10r/doctester/rendermachine/RenderMachine.java`

`RenderMachine` is the HTML documentation generator interface. The default implementation is `RenderMachineImpl`. Override `getRenderMachine()` in your `DocTester` subclass to supply a custom renderer.

---

## Interface hierarchy

```
RenderMachineCommands          (output methods)
    └── RenderMachine          (lifecycle methods)
            └── RenderMachineImpl  (default Bootstrap implementation)
```

---

## RenderMachineCommands

Defines the documentation output methods (the `say*` API). All of these are called by `DocTester` when you use the `say*` methods in your tests.

#### `say(String text)`
Render a paragraph.

#### `sayNextSection(String title)`
Render a section heading and add to navigation sidebar.

#### `sayRaw(String html)`
Inject raw HTML.

#### `sayAndAssertThat(String message, T actual, Matcher<T> matcher)`
Assert with visual output.

#### `sayAndAssertThat(String message, String reason, T actual, Matcher<T> matcher)`
Assert with reason and visual output.

#### `sayAndMakeRequest(Request request)` → `Response`
Execute HTTP request and document it.

#### `sayAndGetCookies()` → `List<Cookie>`
Document and return all cookies.

#### `sayAndGetCookieWithName(String name)` → `Cookie`
Document and return a specific cookie.

---

## RenderMachine lifecycle methods

#### `setTestBrowser(TestBrowser browser)`
Injects the `TestBrowser` used for HTTP calls.

#### `setFileName(String className)`
Sets the output filename (based on the test class name).

#### `finishAndWriteOut()`
Finalizes the HTML and writes the output files. Called once per test class by `DocTester`'s `@AfterClass`.

---

## Default implementation: RenderMachineImpl

**File:** `doctester-core/src/main/java/org/r10r/doctester/rendermachine/RenderMachineImpl.java`

**Output directory:** `target/site/doctester/`

**Output files per test run:**
1. `{FullyQualifiedClassName}.html` — The test's documentation page
2. `index.html` — Updated index listing all DocTest pages
3. `assets/` — Bootstrap 3.0.0 CSS/JS and jQuery 1.9.0 (copied once)

**Page structure:**
- Bootstrap 3 `navbar` with the class name as title
- Scrollable sidebar with section links (from `sayNextSection` calls)
- Main content area with paragraphs, request panels, assertion boxes

**Request/response panel:**
- Bootstrap `panel-primary` for the request
- Bootstrap `panel-default` for the response
- Formatted request: method, URL, headers, payload (pretty-printed)
- Formatted response: status code, headers, body (pretty-printed)

**Assertion boxes:**
- `alert-success` (green) on pass, showing the message
- `alert-danger` (red) on fail, showing message + stack trace

---

## Custom RenderMachine

Implement `RenderMachine` to generate different output formats:

```java
public class MarkdownRenderMachine implements RenderMachine {

    private final StringBuilder sb = new StringBuilder();
    private TestBrowser browser;
    private String fileName;

    @Override
    public void setTestBrowser(TestBrowser browser) {
        this.browser = browser;
    }

    @Override
    public void setFileName(String className) {
        this.fileName = className;
    }

    @Override
    public void say(String text) {
        sb.append("\n").append(text).append("\n");
    }

    @Override
    public void sayNextSection(String title) {
        sb.append("\n## ").append(title).append("\n");
    }

    @Override
    public Response sayAndMakeRequest(Request request) {
        Response response = browser.makeRequest(request);
        sb.append("\n```\n")
          .append(request.method()).append(" ").append(request.uri())
          .append("\n→ ").append(response.httpStatus())
          .append("\n```\n");
        return response;
    }

    @Override
    public void finishAndWriteOut() {
        Path out = Path.of("target/site/doctester/" + fileName + ".md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString());
    }

    // ... implement remaining methods
}
```

Inject it:

```java
public abstract class MarkdownDocTester extends DocTester {

    @Override
    public RenderMachine getRenderMachine() {
        return new MarkdownRenderMachine();
    }
}
```

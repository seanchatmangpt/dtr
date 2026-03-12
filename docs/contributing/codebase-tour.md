# Contributing: Codebase Tour

A quick orientation to the key files before you make changes.

## Where to find things

### The public API your users call

**`DTR.java`** — `dtr-core/src/main/java/org/r10r/doctester/DTR.java`

The abstract base class. All `say*` methods here. This is what user-facing changes touch. Read it first.

**`Request.java`** — `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Request.java`

The fluent request builder. Adding new HTTP options (new headers, new authentication types) goes here.

**`Response.java`** — `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Response.java`

Response holder with deserialization methods. If Jackson serialization breaks, look here.

**`Url.java`** — `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Url.java`

URL builder. Very simple. Rarely needs changes.

### The HTTP engine

**`TestBrowserImpl.java`** — `dtr-core/src/main/java/org/r10r/doctester/testbrowser/TestBrowserImpl.java`

Where HTTP actually happens. Apache HttpClient calls are here. If a request type doesn't work (wrong headers, missing content type), start here.

**`PayloadUtils.java`** — `dtr-core/src/main/java/org/r10r/doctester/testbrowser/PayloadUtils.java`

Content type detection and pretty-printing. If JSON/XML isn't being detected correctly, look here.

### The HTML generator

**`RenderMachineImpl.java`** — `dtr-core/src/main/java/org/r10r/doctester/rendermachine/RenderMachineImpl.java`

HTML assembly. The `finishAndWriteOut()` method is where the full page is built. If the HTML output looks wrong, this is the file.

**`RenderMachineHtml.java`** — `dtr-core/src/main/java/org/r10r/doctester/rendermachine/RenderMachineHtml.java`

HTML template string constants. Bootstrap panel HTML, navbar, sidebar template. If you're updating the Bootstrap version or changing the page layout, this is where the templates live.

### Tests

**`DocTesterTest.java`** — `dtr-core/src/test/java/org/r10r/doctester/DocTesterTest.java`

Unit tests for DocTester's core functionality. Uses Mockito to mock `TestBrowser` and `RenderMachine`.

**`ApiControllerDocTest.java`** — `dtr-integration-test/src/test/java/controllers/ApiControllerDocTest.java`

The integration test. Runs a full Ninja web server. This is both a test and an example — it demonstrates every major DTR feature.

---

## Common change locations

| Change type | Files to touch |
|---|---|
| New `say*` method | `RenderMachineCommands.java`, `RenderMachine.java`, `RenderMachineImpl.java`, `DTR.java` |
| New Request option | `Request.java`, possibly `TestBrowserImpl.java` |
| New Response method | `Response.java` |
| New content type support | `HttpConstants.java`, `PayloadUtils.java`, `Request.java`, `TestBrowserImpl.java` |
| HTML output change | `RenderMachineHtml.java`, `RenderMachineImpl.java` |
| Bug in HTTP execution | `TestBrowserImpl.java` |
| Bug in serialization | `PayloadUtils.java`, `Response.java` |

---

## The interfaces

Three key interfaces define the extension contracts:

**`TestBrowser`** — what any HTTP client must implement
**`RenderMachine`** (extends `RenderMachineCommands`) — what any renderer must implement

When you add a new `say*` method:
1. Add it to `RenderMachineCommands` (the minimal output interface)
2. Implement it in `RenderMachineImpl`
3. Add a delegating call in `DocTester`
4. Add a test in `DocTesterTest`

When you add a new HTTP feature:
1. Add the option to `Request` (builder method)
2. Handle it in `TestBrowserImpl.makeRequest()`
3. Add a test in `DocTesterTest` or the integration test

---

## Key idioms in the codebase

**HTML escaping:** User-supplied text passed to `say()` is escaped with `HtmlEscapers.htmlEscaper().escape(text)`. Never concatenate user strings into HTML directly.

**Content type detection:** `PayloadUtils.isContentTypeApplicationJson(headers)` and `isContentTypeApplicationXml(headers)` check for both `application/json` and `application/json; charset=utf-8`. New content type variants need to be added there.

**Static render machine:** `RenderMachineImpl` is stored as a static field in `DocTester`. This is why it accumulates output across all test methods in a class. Don't change this without understanding the lifecycle implications.

**Per-method browser:** `TestBrowserImpl` is created fresh in `@Before`. The cookie jar is per `TestBrowserImpl` instance, which is why cookies don't persist between test methods.

---

## Module Responsibilities

### dtr-core Module
**What it is:** The main library JAR users depend on

**Responsibility:** Provide DTR testing framework

**Key directories:**
- `src/main/java/io/github/seanchatmangpt/dtr/` — All production code
- `src/test/java/` — Unit tests (mocked, fast)

**When to modify:** Adding features, fixing bugs, improving performance

---

### dtr-integration-test Module
**What it is:** End-to-end integration testing

**Responsibility:** Demonstrate all features with real HTTP server

**Key classes:**
- `PhDThesisDocTest` — Comprehensive example of all DTR capabilities
- Server configuration — Ninja Framework + Jetty

**When to modify:** Adding integration tests for new features, updating examples

---

### dtr-benchmarks Module
**What it is:** Performance measurement suite

**Responsibility:** Track performance across releases

**Key files:**
- JMH benchmark classes for RenderMachine and TestBrowser operations

**When to modify:** Adding benchmarks for new features

---

## Common Development Tasks

### Add a New `say*` Method

**Steps:**
1. Add method signature to `RenderMachineCommands.java`
2. Add full method to `RenderMachine.java`
3. Implement in `RenderMachineImpl.java`
4. Add HTML template (if needed) in `RenderMachineHtml.java`
5. Add delegating method in `DTR.java`
6. Add unit test in `DocTesterTest.java`
7. Add usage example in `PhDThesisDocTest.java`

**Verify:**
```bash
mvnd test -pl dtr-core
mvnd test -pl dtr-integration-test
open target/site/doctester/PhDThesisDocTest.html
```

---

### Fix an HTTP Bug

**Steps:**
1. Locate issue in `TestBrowserImpl.java`
2. Write test in `DocTesterTest.java` (mocked)
3. Fix the bug
4. Add end-to-end test in `PhDThesisDocTest.java` if needed

**Verify:**
```bash
mvnd test -pl dtr-core
mvnd test -pl dtr-integration-test
```

---

### Add New Request Option

**Steps:**
1. Add builder method to `Request.java`
2. Handle it in `TestBrowserImpl.makeRequest()`
3. Add constant to `HttpConstants.java` if needed
4. Test in `DocTesterTest.java`
5. Example in `PhDThesisDocTest.java`

**Verify:**
```bash
mvnd test -pl dtr-core
mvnd test -pl dtr-integration-test
```

---

### Improve HTML Layout

**Steps:**
1. Update templates in `RenderMachineHtml.java`
2. Update logic in `RenderMachineImpl.java`
3. Update `PhDThesisDocTest.java` examples if needed

**Verify:**
```bash
mvnd test -pl dtr-integration-test
open target/site/doctester/PhDThesisDocTest.html  # Visually inspect
```

---

## Testing Strategy

### Unit Tests (Fast)
- Mocked dependencies
- Test individual components
- Run: `mvnd test -pl dtr-core`

### Integration Tests (Slow)
- Real HTTP requests
- Full end-to-end workflows
- Run: `mvnd test -pl dtr-integration-test`

### Manual Testing
1. Run tests
2. Open generated HTML
3. Visually inspect output
4. Test responsive design

---

## Build Tips

**Fast iteration:**
```bash
mvnd clean compile -pl dtr-core       # Only compile
mvnd test -pl dtr-core                # Unit tests only
```

**Full build:**
```bash
mvnd clean verify                      # All tests
```

**Check integration test output:**
```bash
open target/site/doctester/PhDThesisDocTest.html
```

---

## Related Documentation

- **Architecture:** [See Architecture Guide](../explanation/architecture.md) for detailed design
- **Making Changes:** [See Making Changes](making-changes.md) for code standards
- **Releasing:** [See Releasing](releasing.md) for release process

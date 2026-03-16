# Reference: Annotation Reference

**Version:** 2026.4.1

---

## Overview

DTR provides 13 annotations for configuring test execution, documentation generation, and development workflow. These annotations are organized into four categories:

- **Test Setup** — Enable DTR and inject context (field injection recommended)
- **Documentation** — Declarative documentation via annotations
- **Lifecycle** — Control when documentation is written
- **Configuration** — Customize output format and behavior
- **Development Tools** — IDE integration and test metadata

---

## Getting Started: Modern DTR Approach

### Recommended Pattern for New Tests

Start with these two annotations for **all new DTR tests**:

```java
@DtrTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }
}
```

### Why This Pattern is Recommended

1. **Minimal Configuration** - Just two annotations for full DTR functionality
2. **Clean Test Methods** - No parameter passing needed
3. **Auto-Finish Enabled** - Documentation written automatically after each test
4. **Better IDE Support** - Full autocomplete for `ctx.say*()` methods
5. **Future-Proof** - Uses the latest DTR features

### Quick Reference: Modern vs Legacy

| Pattern | Annotations | Recommended | Use Case |
|---------|------------|-------------|----------|
| **Modern** | `@DtrTest` + `@DtrContextField` | ✅ **YES** | All new tests |
| Legacy (Param) | `@ExtendWith` + `@AutoFinishDocTest` | ❌ NO | Legacy migration only |
| Legacy (Inheritance) | `extends DtrTest` | ❌ NO | Legacy migration only |

---

## Quick Reference

| Annotation | Category | Target | Purpose |
|------------|----------|--------|---------|
| `@DtrTest` | Test Setup | TYPE | **Primary**: enables DTR + auto-finish + field injection |
| `@DtrContextField` | Test Setup | FIELD | **Recommended**: Inject DtrContext into class fields |
| `@TestSetup` | Test Setup | METHOD | DTR-aware test setup (alternative to @BeforeEach) |
| `@DocSection` | Documentation | METHOD | Section heading (H2) |
| `@DocDescription` | Documentation | METHOD | Narrative paragraphs |
| `@DocNote` | Documentation | METHOD | Informational callout boxes |
| `@DocWarning` | Documentation | METHOD | Warning callout boxes |
| `@DocCode` | Documentation | METHOD | Fenced code blocks |
| `@AutoFinishDocTest` | Lifecycle | TYPE, METHOD | Legacy: auto-call `finishAndWriteOut()` after tests |
| `@DtrConfig` | Configuration | TYPE, METHOD, PACKAGE | Hierarchical output configuration |
| `@LivePreview` | Development Tools | TYPE, METHOD | IDE live preview support |
| `@AuthenticatedTest` | Development Tools | METHOD | Marker for auth-required tests |

---

## Test Setup Annotations

### @DtrTest

**Purpose:** **Primary annotation** for DTR test configuration. Composite annotation that combines `@ExtendWith(DtrExtension.class)`, `@AutoFinishDocTest`, and optimized for field injection with `@DtrContextField`. This is the recommended approach for all new DTR tests.

**Target:** `TYPE` (class-level)

**Attributes:**
- `autoFinish` (boolean, default `true`) — Auto-finish after each test method
- `fileName` (String, default `""`) — Custom output filename

**Recommended Pattern (Field Injection):**
```java
@DtrTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }

    @Test
    void createUser() {
        ctx.say("Creates a new user");
    }
}
```

**Why Field Injection is Recommended:**
- **Cleaner method signatures** - No `DtrContext` parameter in every test method
- **Better IDE support** - Autocomplete for `ctx.say*()` methods
- **Context reuse** - Same context instance used across all test methods in the class
- **Minimal boilerplate** - Just `@DtrTest` + `@DtrContextField` required

**Alternative Patterns:**

**Parameter Injection (For specialized cases):**
```java
@DtrTest
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }
}
```

**Legacy Pattern (Not recommended for new code):**
```java
@ExtendWith(DtrExtension.class)  // LEGACY: Use @DtrTest instead
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }
}
```

**Inheritance Pattern (Legacy - not recommended):**
```java
@DtrTest  // Still works but field injection is preferred
class MyApiTest extends io.github.seanchatmangpt.dtr.DtrTest {
    @Test
    void listUsers() {
        say("Returns all users");  // Direct method access
    }
}
```

**Best Practices:**
- **Always start with `@DtrTest` + `@DtrContextField`** for new tests
- Use field injection for test classes with 2+ methods
- Use parameter injection only when each test needs different setup
- Avoid manual `@ExtendWith(DtrExtension.class)` for new code

**Migration from Legacy:**
```java
// OLD (Legacy)
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class MyLegacyTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Content");
    }
}

// NEW (Recommended)
@DtrTest
class MyNewTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Content");
    }
}
```

**See Also:**
- [@DtrContextField](#dtrcontextfield) — Field-level context injection (essential partner)
- [@AutoFinishDocTest](#autofinishdoctest) — Legacy auto-finish behavior
- [Complete API Reference](complete-api-reference.md) — All 50+ `say*` methods

**Since:** 2026.4.0

---

### @DtrContextField

**Purpose:** **Essential partner annotation** for `@DtrTest`. Marks a field to receive automatic `DtrContext` injection by `DtrExtension`. Provides class-level field injection as the recommended approach for all new DTR tests.

**Target:** `FIELD`

**Attributes:** None

**Recommended Usage (with @DtrTest):**
```java
@DtrTest  // Primary annotation - provides auto-finish and extension setup
class MyApiTest {
    @DtrContextField  // Essential partner - provides field injection
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }

    @Test
    void createUser() {
        ctx.say("Creates a new user");
        // Context reused across all test methods
    }
}
```

**Why @DtrContextField + @DtrTest is Recommended:**
- **Minimal configuration** - Just two annotations for full DTR functionality
- **Clean test methods** - No parameter passing needed
- **Context consistency** - Same context used across all tests in the class
- **Better IDE support** - Full autocomplete for `ctx.say*()` methods

**Field Injection vs Parameter Injection:**

| Aspect | Field Injection (`@DtrContextField`) | Parameter Injection |
|--------|-------------------------------------|---------------------|
| Declaration | Once at class level | Per-method parameter |
| Accessibility | All test methods | Specific test methods |
| Method signatures | Cleaner (no ctx parameter) | More explicit |
| Use case | **Recommended for most tests** | Specialized cases only |
| Best with | `@DtrTest` | `@ExtendWith(DtrExtension.class)` |

**Supported Field Types:**
- Instance fields (non-static) — each test gets its own instance (recommended)
- Static fields — shared across all test methods in the class
- Any access modifier (private, protected, package-private, public)

**Thread Safety:** Each test method receives its own `DtrContext` instance, but all instances share the same underlying `RenderMachine`. This ensures test isolation while maintaining a single documentation output per test class.

**Best Practices:**
- **Always use `@DtrContextField` with `@DtrTest`** for new tests
- Use private fields for encapsulation (DTR uses reflection to inject)
- Use instance fields (non-static) for test isolation
- Consider static fields for shared documentation setup across tests
- Field injection is **required** for the recommended `@DtrTest` pattern

**Migration from Parameter Injection:**
```java
// OLD (Parameter Injection)
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }
}

// NEW (Field Injection - Recommended)
@DtrTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }
}
```

**See Also:**
- [@DtrTest](#dtrtest) — Primary annotation that enables field injection
- [DtrContext API](testbrowser-api.md) — Complete context method reference

**Since:** 2026.4.0

---

### @TestSetup

**Purpose:** Marks a method to be executed before each test for setup purposes. Alternative to JUnit's `@BeforeEach` with DTR-specific lifecycle integration. Works with the recommended `@DtrTest + @DtrContextField` pattern.

**Target:** `METHOD`

**Attributes:** None

**Usage Example (Recommended Pattern):**
```java
@DtrTest
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @TestSetup
    void setupDocumentation() {
        ctx.sayNextSection("API Documentation");
        ctx.say("This section documents the REST API.");
    }

    @Test
    void listUsers() {
        ctx.say("Testing user list endpoint...");
    }

    @Test
    void createUser() {
        ctx.say("Testing user creation endpoint...");
    }
}
```

**Legacy Pattern (Not recommended):**
```java
@ExtendWith(DtrExtension.class)  // LEGACY: Use @DtrTest instead
class MyLegacyApiTest {
    @TestSetup
    void setupDocumentation(DtrContext ctx) {
        ctx.say("Setup with parameter injection");
    }
}
```

**Capabilities:**
- Can accept `DtrContext` as a parameter (injected by DTR)
- Executes before `@Test` methods
- Can be used for documentation initialization
- Works with both field and parameter injection patterns

**Best Practices:**
- Use `@TestSetup` with `@DtrTest + @DtrContextField` for DTR-aware setup
- Use JUnit's `@BeforeEach` for general test setup (data, mocks, etc.)
- Combine both if needed: `@TestSetup` for documentation, `@BeforeEach` for test data
- **Always use field injection in setup methods** when using `@DtrTest`

**Migration from Legacy:**
```java
// OLD (Legacy)
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class MyLegacyTest {
    private DtrContext ctx;

    @TestSetup
    void setup(DtrContext ctx) {
        this.ctx = ctx;
        ctx.say("Setup");
    }
}

// NEW (Recommended)
@DtrTest
class MyNewTest {
    @DtrContextField
    private DtrContext ctx;

    @TestSetup
    void setup() {
        ctx.say("Setup");
    }
}
```

**See Also:**
- [@DtrTest](#dtrtest) — Primary test setup annotation
- [@DtrContextField](#dtrcontextfield) — Field injection (required for recommended pattern)
- [JUnit 5 @BeforeEach](https://junit.org/junit5/docs/current/api/org/junit/jupiter/api/BeforeEach.html) — Standard setup hook

---

## Documentation Annotations

### @DocSection

**Purpose:** Declares the section heading for a DTR test method. Automatically calls `sayNextSection(title)` at the start of the test.

**Target:** `METHOD`

**Attributes:**
- `value` (String, required) — Section heading text

**Rendering:**
- Markdown: H2 heading (`## Section Title`)
- LaTeX: `\section{}` command
- Blog/Slides: Format-specific heading level

**Usage Example:**
```java
@Test
@DocSection("User Authentication")
@DocDescription("Verifies that valid credentials return a 200 response.")
public void testLogin() {
    Response response = sayAndMakeRequest(
        Request.POST().url(testServerUrl().path("/login")).payload(...));
    sayAndAssertThat("Login succeeds", 200, equalTo(response.httpStatus()));
}
```

**Annotation Processing Order:**
When multiple documentation annotations are present, DTR processes them in this fixed order:
1. `@DocSection` — section heading (first)
2. `@DocDescription` — narrative paragraphs
3. `@DocNote` — informational callouts
4. `@DocWarning` — warning callouts
5. `@DocCode` — code blocks (last)

**Best Practices:**
- Use plain text (no Markdown formatting) in section titles
- Keep titles concise (3-8 words)
- Use title case for consistency
- One section per test method for clear organization

**See Also:**
- [@DocDescription](#docdescription) — Narrative content
- [sayNextSection()](complete-api-reference.md) — Programmatic alternative

---

### @DocDescription

**Purpose:** Declares one or more description paragraphs for a DTR test method. Automatically calls `say(text)` for each line at the start of the test.

**Target:** `METHOD`

**Attributes:**
- `value` (String[], required) — One or more description paragraphs

**Usage Examples:**

**Single paragraph:**
```java
@Test
@DocDescription("Returns HTTP 200 with an empty array when no articles exist.")
public void testListArticles() {
    // Test implementation
}
```

**Multiple paragraphs:**
```java
@Test
@DocSection("Article API")
@DocDescription({
    "This endpoint returns a paginated list of articles.",
    "Pass the 'page' query parameter to navigate between pages."
})
public void testListArticles() {
    // Test implementation
}
```

**Annotation Processing Order:**
1. `@DocSection` — section heading
2. `@DocDescription` — narrative paragraphs (this annotation)
3. `@DocNote` — informational callouts
4. `@DocWarning` — warning callouts
5. `@DocCode` — code blocks

**Best Practices:**
- Use `@DocDescription` for high-level explanations
- Keep paragraphs focused (one idea per paragraph)
- Use multiple `@DocDescription` elements for longer content
- Consider programmatic `say()` calls for dynamic content

**See Also:**
- [@DocSection](#docsection) — Section headings
- [say()](complete-api-reference.md) — Programmatic alternative

---

### @DocNote

**Purpose:** Renders one or more informational callout boxes in the generated Markdown documentation. Each element produces a GitHub-flavored `> [!NOTE]` admonition.

**Target:** `METHOD`

**Attributes:**
- `value` (String[], required) — One or more informational callout texts

**Rendering Output:**
```markdown
> [!NOTE]
> Requires the `Accept: application/json` header.
```

**Usage Example:**
```java
@Test
@DocSection("User API")
@DocDescription("Returns all active users.")
@DocNote("Requires the `Accept: application/json` header.")
public void testGetUsers() {
    // Test implementation
}
```

**Multiple notes:**
```java
@Test
@DocNote({
    "Authentication is required for this endpoint.",
    "Rate limit: 100 requests per minute per user."
})
public void testProtectedEndpoint() {
    // Test implementation
}
```

**Annotation Processing Order:**
1. `@DocSection` — section heading
2. `@DocDescription` — narrative paragraphs
3. `@DocNote` — informational callouts (this annotation)
4. `@DocWarning` — warning callouts
5. `@DocCode` — code blocks

**Best Practices:**
- Use for helpful hints, tips, and supplementary information
- Keep notes concise (1-2 sentences)
- Use Markdown formatting (backticks, bold, links) for emphasis
- Avoid critical information (use `@DocWarning` instead)

**See Also:**
- [@DocWarning](#docwarning) — Warning callouts
- [sayNote()](complete-api-reference.md) — Programmatic alternative

---

### @DocWarning

**Purpose:** Renders one or more warning callout boxes in the generated Markdown documentation. Each element produces a GitHub-flavored `> [!WARNING]` admonition.

**Target:** `METHOD`

**Attributes:**
- `value` (String[], required) — One or more warning texts

**Rendering Output:**
```markdown
> [!WARNING]
> This operation is **irreversible**. The user record is permanently removed.
```

**Usage Example:**
```java
@Test
@DocSection("Delete User")
@DocDescription("Permanently removes a user from the system.")
@DocWarning("This operation is **irreversible**. The user record is permanently removed.")
public void testDeleteUser() {
    // Test implementation
}
```

**Multiple warnings:**
```java
@Test
@DocWarning({
    "This endpoint is deprecated and will be removed in v2.0.",
    "Use the new /api/v2/users endpoint instead."
})
public void testDeprecatedEndpoint() {
    // Test implementation
}
```

**Annotation Processing Order:**
1. `@DocSection` — section heading
2. `@DocDescription` — narrative paragraphs
3. `@DocNote` — informational callouts
4. `@DocWarning` — warning callouts (this annotation)
5. `@DocCode` — code blocks

**Best Practices:**
- Use for critical information: breaking changes, security issues, irreversible operations
- Keep warnings concise and actionable
- Use strong language ("must not", "required", "critical")
- Consider using `@DocNote` for non-critical information

**See Also:**
- [@DocNote](#docnote) — Informational callouts
- [sayWarning()](complete-api-reference.md) — Programmatic alternative

---

### @DocCode

**Purpose:** Renders a code example block in the generated Markdown documentation. Lines are joined with newlines and emitted as a fenced code block.

**Target:** `METHOD`

**Attributes:**
- `value` (String[], required) — Lines of source code to display
- `language` (String, default `""`) — Optional language hint for syntax highlighting

**Rendering Output:**
````markdown
```json
{
  "username": "alice",
  "email": "alice@example.com"
}
```
````

**Usage Examples:**

**JSON example:**
```java
@Test
@DocSection("Create User — Request Shape")
@DocCode(
    language = "json",
    value = {
        "{",
        "  \"username\": \"alice\",",
        "  \"email\": \"alice@example.com\"",
        "}"
    }
)
public void testCreateUser() {
    // Test implementation
}
```

**Java example:**
```java
@Test
@DocCode(
    language = "java",
    value = {
        "DtrContext ctx = new DtrContext();",
        "ctx.say(\"Hello, World!\");",
        "ctx.finishAndWriteOut();"
    }
)
public void testCodeExample() {
    // Test implementation
}
```

**No language hint:**
```java
@Test
@DocCode(value = {
    "curl -X POST https://api.example.com/users \\",
    "  -H 'Content-Type: application/json' \\",
    "  -d '{\"name\":\"Alice\"}'"
})
public void testCurlExample() {
    // Test implementation
}
```

**Annotation Processing Order:**
1. `@DocSection` — section heading
2. `@DocDescription` — narrative paragraphs
3. `@DocNote` — informational callouts
4. `@DocWarning` — warning callouts
5. `@DocCode` — code blocks (this annotation)

**Best Practices:**
- Always specify `language` for syntax highlighting (java, json, bash, etc.)
- No escaping needed — content is in fenced code block
- Use for static code examples (consider `sayCode()` for dynamic content)
- Keep examples concise and realistic

**See Also:**
- [sayCode()](complete-api-reference.md) — Programmatic alternative
- [Complete API Reference](complete-api-reference.md) — All 50+ say* methods

---

## Lifecycle Annotations

### @AutoFinishDocTest

**Purpose:** **Legacy annotation** that automatically calls `finishAndWriteOut()` after each test method. Replaced by `@DtrTest` which includes this functionality by default.

**Target:** `TYPE`, `METHOD`

**Attributes:** None

**Legacy Usage (Not recommended for new code):**
```java
@ExtendWith(DtrExtension.class)  // LEGACY: Use @DtrTest instead
@AutoFinishDocTest                // LEGACY: Included in @DtrTest
class MyLegacyDocumentationTest {
    @Test
    void documentFeature(DtrContext ctx) {
        ctx.say("Content");
        // No need to call finishAndWriteOut()
    }
}
```

**Recommended Replacement:**
```java
@DtrTest  // Modern equivalent - includes auto-finish by default
class MyModernDocumentationTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void documentFeature() {
        ctx.say("Content");
        // Auto-finish is enabled by default with @DtrTest
    }
}
```

**When to Use (Legacy):**
- **Only for legacy codebases** migrating from older versions
- When you need selective auto-finish behavior (method-level only)
- When you cannot migrate to `@DtrTest` due to existing constraints

**Migration from Legacy to Modern:**
```java
// OLD (Legacy)
@ExtendWith(DtrExtension.class)
@AutoFinishDocTest
class MyLegacyTest {
    @Test
    void test(DtrContext ctx) {
        ctx.say("Legacy approach");
    }
}

// NEW (Recommended)
@DtrTest
class MyModernTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Modern approach");
    }
}
```

**Performance Considerations:**
- Each auto-finished test generates a separate output file
- More files = more I/O overhead
- `@DtrTest` provides the same functionality with better organization

**See Also:**
- [@DtrTest](#dtrtest) — Modern replacement that includes auto-finish
- [@DtrContextField](#dtrcontextfield) — Field injection (required for recommended pattern)
- [finishAndWriteOut()](testbrowser-api.md) — Manual finish method

**Since:** 2026.4.0 (Legacy)

---

## Configuration Annotations

### @DtrConfig

**Purpose:** Hierarchical configuration for DTR documentation output at the package, class, or method level.

**Target:** `TYPE`, `METHOD`, `PACKAGE`

**Attributes:**

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `format` | `OutputFormat` | `MARKDOWN` | Output format (MARKDOWN, HTML, LATEX, PDF) |
| `latexTemplate` | `LatexTemplate` | `ARTICLE` | LaTeX template (ARTICLE, BOOK, BEAMER, REPORT) |
| `outputDir` | String | `"docs/test"` | Output directory relative to project root |
| `includeEnvProfile` | boolean | `false` | Include environment profile (Java version, OS, etc.) |
| `generateToc` | boolean | `true` | Generate table of contents from `sayNextSection()` calls |

**Configuration Precedence (highest to lowest):**
1. System properties (e.g., `-Ddtr.format=markdown`)
2. `@DtrConfig` on test method
3. `@DtrConfig` on test class
4. `@DtrConfig` on package (package-info.java)
5. Default values

**Usage Examples:**

**Package-level configuration (package-info.java):**
```java
@DtrConfig(format = OutputFormat.HTML, outputDir = "docs/api")
package com.example.docs;
```

**Class-level override:**
```java
@DtrConfig(
    format = OutputFormat.LATEX,
    latexTemplate = LatexTemplate.ARTICLE,
    outputDir = "docs/latex"
)
public class PdfDocumentationTest {
    // All tests in this class generate LaTeX output
}
```

**Method-level override:**
```java
@Test
@DtrConfig(format = OutputFormat.MARKDOWN, includeEnvProfile = true)
public void testWithEnvProfile() {
    // This test includes environment profile
}
```

**System Property Override:**
```bash
mvn test -Ddtr.format=latex -Ddtr.output.dir=build/docs
```

**Output Format Options:**

| Format | Description | Extension | Use Case |
|--------|-------------|-----------|----------|
| `MARKDOWN` | GitHub Flavored Markdown | `.md` | Default documentation |
| `HTML` | Standalone HTML with embedded CSS | `.html` | Web publishing |
| `LATEX` | LaTeX source files | `.tex` | Academic publishing |
| `PDF` | Compiled PDF via LaTeX | `.pdf` | Print documents |

**LaTeX Template Options:**

| Template | Description | Use Case |
|----------|-------------|----------|
| `ARTICLE` | Standard article template | Technical reports, API docs |
| `BOOK` | Book template for long-form docs | Multi-chapter guides |
| `BEAMER` | Presentation slides | Conference talks, training |
| `REPORT` | Formal report template | Academic papers, theses |

**Supported System Properties:**
- `dtr.format` — Output format (markdown, html, latex, pdf)
- `dtr.output.dir` — Output directory path
- `dtr.include.env.profile` — Include environment profile (true/false)
- `dtr.generate.toc` — Generate table of contents (true/false)

**Best Practices:**
- Use package-level configuration for consistency across test suites
- Override at method level for special-case formatting
- Use system properties for environment-specific configuration (CI vs local)
- Consider `includeEnvProfile = true` for reproducibility documentation

**See Also:**
- [Configuration Reference](configuration.md) — Complete configuration guide
- [Output Formats](configuration.md#output-format-configuration) — Format details

---

## Development Tools Annotations

### @LivePreview

**Purpose:** Enables live preview of documentation in supported IDEs. Generates a companion `.dtr.preview` file that IDEs can use for real-time preview rendering.

**Target:** `TYPE`, `METHOD`

**Attributes:**

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `refreshRateMs` | long | `500L` | Refresh rate in milliseconds (100-1000+) |
| `inlineGutter` | boolean | `true` | Show preview in editor gutter |
| `autoOpen` | boolean | `false` | Auto-open preview panel when tests run |
| `customCss` | String | `""` | Custom CSS for preview rendering (IDE-specific) |
| `includeMetadata` | boolean | `true` | Include test execution metadata |

**Usage Example:**
```java
@LivePreview(refreshRateMs = 300, inlineGutter = true)
public class MyDocumentationTest extends DtrTest {

    @Test
    public void demonstrateFeature(DtrContext ctx) {
        ctx.say("This content appears in the live preview");
    }
}
```

**IDE Integration:**
Live preview requires IDE-specific extensions:
- **IntelliJ IDEA:** Install "DTR Live Preview" plugin from marketplace
- **VS Code:** Install "DTR Preview" extension
- **Eclipse:** Install "DTR Tools" plugin via Eclipse Marketplace

**Preview File Format:**
Generated `.dtr.preview` files use JSON format:
```json
{
  "version": "1.0",
  "testClass": "com.example.MyTest",
  "timestamp": "2026-03-15T10:30:00Z",
  "content": "# Documentation Content\n\n...",
  "metadata": {
    "refreshRateMs": 500,
    "inlineGutter": true
  }
}
```

**Performance Considerations:**
- Lower refresh rates (100ms) = more responsive but higher CPU usage
- Higher refresh rates (1000ms+) = lower CPU usage but less responsive
- For large test suites (100+ methods), consider `refreshRateMs = 1000`
- Inline gutter disabled by default for projects with 100+ test methods

**Refresh Rate Recommendations:**
- **100-300ms:** Interactive development, small test suites
- **500ms:** Default, balanced performance
- **1000ms+:** Large test suites, resource-constrained environments

**Best Practices:**
- Enable `inlineGutter` for documentation-heavy tests
- Disable `inlineGutter` for large test suites to reduce clutter
- Use `refreshRateMs = 1000` for CI environments
- Customize CSS with `customCss` for brand consistency

**See Also:**
- [Configuration Reference](configuration.md) — Output configuration
- [DtrContext API](testbrowser-api.md) — Documentation methods

**Since:** 2026.2.0

---

### @AuthenticatedTest

**Purpose:** Marker annotation for tests that require authentication. Can be used by test runners to skip authentication-required tests when credentials are not available.

**Target:** `METHOD`

**Attributes:**
- `value` (String, default `""`) — Description of what authentication is required

**Usage Example:**
```java
@Test
@AuthenticatedTest("Requires valid JWT token")
void testProtectedApi(DtrContext ctx) {
    // Test requires valid auth token
    String token = authenticate("user", "pass");
    Response response = api.get("/protected", token);
    ctx.sayAndAssertThat("Protected endpoint returns 200", response.status(), is(200));
}
```

**Integration with Test Runners:**
Test runners can filter tests based on this annotation:
```java
// Example: Skip auth tests if credentials not available
if (!credentialsAvailable()) {
    skipTestsWithAnnotation(AuthenticatedTest.class);
}
```

**Best Practices:**
- Describe authentication requirement in `value` attribute
- Combine with `@DocNote` to document authentication setup
- Use for tests that require external auth services (OAuth, SAML, etc.)
- Consider using environment variables for credentials

**Example with Documentation:**
```java
@Test
@AuthenticatedTest("Requires ADMIN role via OAuth2")
@DocSection("Admin-Only Endpoint")
@DocDescription("Deletes a user account. Only accessible to administrators.")
@DocNote("Set ADMIN_TOKEN environment variable to run this test.")
public void testDeleteUserAsAdmin() {
    String token = System.getenv("ADMIN_TOKEN");
    Response response = api.delete("/users/123", token);
    ctx.sayAndAssertThat("Admin can delete users", response.status(), is(204));
}
```

**See Also:**
- [@DocNote](#docnote) — Document authentication setup
- [CI/CD Integration](../how-to/ci-cd-integration.md) — Credential management

---

## Annotation Processing Order

### Documentation Annotations

When multiple documentation annotations are present on a test method, DTR processes them in this fixed order:

1. **@DocSection** — Section heading (H2)
2. **@DocDescription** — Narrative paragraphs
3. **@DocNote** — Informational callout boxes
4. **@DocWarning** — Warning callout boxes
5. **@DocCode** — Code example blocks

**Example:**
```java
@Test
@DocSection("Create User")                                    // 1st
@DocDescription("Creates a new user account.")                // 2nd
@DocNote("Email must be unique.")                             // 3rd
@DocWarning("Password must be 12+ characters.")               // 4th
@DocCode(language = "json", value = {...})                    // 5th
public void testCreateUser() {
    // Test implementation
}
```

### Configuration Annotations

**@DtrConfig** follows hierarchical precedence (highest to lowest):

1. **System properties** — e.g., `-Ddtr.format=markdown`
2. **@DtrConfig on test method** — Method-specific override
3. **@DtrConfig on test class** — Class-level configuration
4. **@DtrConfig on package** — Package-level configuration (package-info.java)
5. **Default values** — Built-in defaults

**Example:**
```java
// package-info.java
@DtrConfig(format = OutputFormat.HTML, outputDir = "docs/api")
package com.example.docs;

// Class-level override
@DtrConfig(format = OutputFormat.MARKDOWN)  // Overrides package HTML
public class MyClassTest {
    @Test
    @DtrConfig(format = OutputFormat.LATEX)  // Overrides class MARKDOWN
    public void testWithLatex() {
        // Generates LaTeX output
    }
}
```

---

## Common Patterns

### Complete Documentation Test (Recommended Pattern)

The modern approach combines `@DtrTest` + `@DtrContextField` for maximum simplicity and functionality:

```java
@DtrTest
class UserApiDocumentationTest {

    @DtrContextField  // Essential partner for clean test methods
    private DtrContext ctx;

    @Test
    @DocSection("User Registration")
    @DocDescription({
        "Registers a new user account with email and password.",
        "Returns 201 on success, 400 for validation errors."
    })
    @DocNote("Email must be unique across all users.")
    @DocWarning("Password must be 12+ characters with special chars.")
    @DocCode(
        language = "json",
        value = {
            "{",
            "  \"email\": \"user@example.com\",",
            "  \"password\": \"SecurePass123!\"",
            "}"
        }
    )
    public void testUserRegistration() {
        ctx.sayKeyValue("Endpoint", "POST /api/users");
        ctx.sayKeyValue("Auth Required", "No");

        Response response = api.post("/api/users", userPayload);
        ctx.sayAndAssertThat("Registration succeeds", response.status(), is(201));
    }
}
```

**Why This Pattern is Recommended:**
- **Minimal Annotations** - Just `@DtrTest` + `@DtrContextField`
- **Clean Methods** - No parameters in test methods
- **Auto-Finish Enabled** - Documentation written automatically
- **Better IDE Support** - Full autocomplete for `ctx.say*()` methods

### Package-Level Configuration

Organizing documentation by package:

```java
// package-info.java
@DtrConfig(
    format = OutputFormat.HTML,
    outputDir = "docs/public-api",
    includeEnvProfile = true
)
package com.example.api.public_;

// package-info.java
@DtrConfig(
    format = OutputFormat.MARKDOWN,
    outputDir = "docs/internal-api"
)
package com.example.api.internal;
```

### Legacy Pattern (Not Recommended)

For reference only - used for migrating older codebases:

```java
@ExtendWith(DtrExtension.class)  // LEGACY: Use @DtrTest instead
@AutoFinishDocTest                // LEGACY: Included in @DtrTest
class MixedDocumentationTest {

    @Test
    @AutoFinishDocTest
    public void quickTest(DtrContext ctx) {
        ctx.say("Quick test with auto-finish");
        // Generates: MixedDocumentationTest.quickTest.md
    }

    @Test
    public void comprehensiveTest(DtrContext ctx) {
        ctx.sayNextSection("Comprehensive Documentation");
        ctx.say("Multiple sections...");
        ctx.sayNextSection("More Content");
        ctx.say("All in one file");

        ctx.finishAndWriteOut();
        // Generates: MixedDocumentationTest.comprehensiveTest.md
    }
}
```

---

## Migration Guide: Legacy to Modern

### From Legacy Parameter Injection

**Old Way (Legacy):**
```java
@ExtendWith(DtrExtension.class)      // ❌ LEGACY
@AutoFinishDocTest                   // ❌ LEGACY
class MyLegacyApiTest {

    @Test
    void getUser(DtrContext ctx) {    // ❌ Parameter injection
        ctx.say("Gets a user");
        ctx.finishAndWriteOut();      // ❌ Manual finish
    }
}
```

**New Way (Recommended):**
```java
@DtrTest                             // ✅ Modern - includes extension + auto-finish
class MyModernApiTest {

    @DtrContextField                  // ✅ Essential partner
    private DtrContext ctx;

    @Test
    void getUser() {                   // ✅ Clean method signature
        ctx.say("Gets a user");        // ✅ Auto-finish enabled
    }
}
```

### From Legacy Inheritance

**Old Way (Legacy):**
```java
@ExtendWith(DtrExtension.class)      // ❌ LEGACY
class MyLegacyApiTest extends DtrTest {  // ❌ Inheritance

    @Test
    void getUser() {
        say("Gets a user");           // ❌ Direct method access
    }
}
```

**New Way (Recommended):**
```java
@DtrTest                             // ✅ Modern - no inheritance needed
class MyModernApiTest {

    @DtrContextField                  // ✅ Essential partner
    private DtrContext ctx;

    @Test
    void getUser() {                   // ✅ Clean method signature
        ctx.say("Gets a user");        // ✅ Auto-finish enabled
    }
}
```

### Migration Checklist

1. **Replace `@ExtendWith(DtrExtension.class)`** with `@DtrTest`
2. **Replace `@AutoFinishDocTest`** - it's included in `@DtrTest`
3. **Add `@DtrContextField`** for field injection
4. **Remove `DtrContext` parameters** from test methods
5. **Remove inheritance** from `DtrTest`
6. **Update method calls** from `say()` to `ctx.say()`

### Common Migration Issues

**Issue:** Missing `@DtrContextField`
```java
// ❌ Wrong - context not injected
@DtrTest
class MissingFieldTest {
    @Test
    void test() {
        ctx.say("This will fail - ctx is null");
    }
}

// ✅ Correct - field injection
@DtrTest
class CorrectTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("This works");
    }
}
```

**Issue:** Old syntax with parameter injection
```java
// ❌ Legacy
@DtrTest
class LegacySyntaxTest {
    @Test
    void test(DtrContext ctx) {  // ❌ Parameter injection (old pattern)
        ctx.say("Works but not recommended");
    }
}

// ✅ Modern
@DtrTest
class ModernSyntaxTest {
    @DtrContextField
    private DtrContext ctx;      // ✅ Field injection (recommended)

    @Test
    void test() {                // ✅ Clean method signature
        ctx.say("Recommended approach");
    }
}
```

---

## See Also

- **[Complete API Reference](complete-api-reference.md)** — All 50+ `say*` methods documented
- **[Configuration](configuration.md)** — System properties and environment variables
- **[DtrContext and DtrExtension API Reference](testbrowser-api.md)** — API surface
- **[Testing with Documentation](../tutorials/03-testing-with-documentation.md)** — Assertions combined with docs
- **[Advanced Patterns](../tutorials/06-advanced-patterns.md)** — Benchmarking, diagrams, and quality metrics
- **[Field Injection Guide](../tutorials/field-injection-guide.md)** — Detailed field injection tutorial

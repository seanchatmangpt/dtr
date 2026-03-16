# `DtrExtension`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

JUnit 5 extension for DTR integration. <p>This extension provides native JUnit 5 support for DTR, replacing the JUnit 4 {@code @Rule} based approach with JUnit 5's extension model. <p><strong>Usage Options:</strong></p> <p><em>Option 1: Field Injection (Recommended for tests with many methods)</em></p> <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest {     @DtrContextField     private DtrContext ctx;     @Test     void testGetUsers() {         ctx.sayNextSection("User API");         ctx.say("Documentation for User API goes here.");     } } }</pre> <p><em>Option 2: Method Parameter Injection</em></p> <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest {     @Test     void testGetUsers(DtrContext context) {         context.sayNextSection("User API");         context.say("Documentation for User API goes here.");     } } }</pre> <p><em>Option 3: Inheritance (Legacy Pattern)</em></p> <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest extends io.github.seanchatmangpt.dtr.DtrTest {     @Test     void testGetUsers() {         sayNextSection("User API");         say("Documentation for User API goes here.");     } } }</pre> <p><em>Option 4: Composite Annotation (Most Concise)</em></p> <pre>{@code @DtrTest class MyApiDocTest {     @DtrContextField     private DtrContext ctx;     @Test     void testGetUsers() {         ctx.sayNextSection("User API");         ctx.say("Documentation for User API goes here.");     } } }</pre> <p>The extension manages: <ul>   <li>RenderMachine lifecycle (one per test class)</li>   <li>Documentation output generation after all tests complete</li>   <li>Auto-finish support via {@link AutoFinishDocTest} annotation</li>   <li>@TestSetup method execution before tests</li>   <li>Field injection via {@link DtrContextField} annotation</li> </ul>

```java
public class DtrExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver, TestInstancePostProcessor {
    // injectDtrContextFields, getOrCreateRenderMachine, createRenderMachine, processTestSetupMethods, shouldAutoFinish
}
```

---

## Methods

### `createRenderMachine`

Creates a new RenderMachine instance. Override to customize.

---

### `getOrCreateRenderMachine`

Gets the RenderMachine for this test class, creating it if needed.

---

### `injectDtrContextFields`

Injects DtrContext into fields annotated with @DtrContextField. <p>Scans the test instance for fields annotated with {@link DtrContextField} and injects a new {@link DtrContext} instance into each field. The field type must be exactly {@link DtrContext}. All access modifiers are supported (private, protected, package-private, public).</p> <p>Each field receives a new DtrContext instance, but all instances share the same underlying RenderMachine, ensuring consistent documentation output across all fields.</p>

| Parameter | Description |
| --- | --- |
| `testInstance` | the test instance to inject into |
| `renderMachine` | the render machine to use for creating contexts |

| Exception | Description |
| --- | --- |
| `Exception` | if field injection fails |

---

### `processTestSetupMethods`

Processes methods annotated with @TestSetup in the test class. These methods are executed before any tests and can accept DtrContext as a parameter.

---

### `shouldAutoFinish`

Determines whether auto-finish should be triggered for the current test. Checks both method-level and class-level @AutoFinishDocTest annotations.

| Parameter | Description |
| --- | --- |
| `context` | the extension context |

> **Returns:** true if auto-finish should be triggered

---


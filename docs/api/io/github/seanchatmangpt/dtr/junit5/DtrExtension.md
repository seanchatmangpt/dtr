# `DtrExtension`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

JUnit 5 extension for DTR integration. <p>This extension provides native JUnit 5 support for DTR, replacing the JUnit 4 {@code @Rule} based approach with JUnit 5's extension model. <p>Usage: <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest implements DtrCommands {     @Test     void testGetUsers(DtrContext context) {         context.sayNextSection("User API");         context.say("Documentation for User API goes here.");     } } }</pre> <p>The extension manages: <ul>   <li>RenderMachine lifecycle (one per test class)</li>   <li>Documentation output generation after all tests complete</li>   <li>Auto-finish support via {@link AutoFinishDocTest} annotation</li>   <li>@TestSetup method execution before tests</li> </ul>

```java
public class DtrExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {
    // getOrCreateRenderMachine, createRenderMachine, processTestSetupMethods, shouldAutoFinish
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


# `DtrExtension`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

JUnit Jupiter 6 extension for DTR integration. <p>This extension provides native JUnit Jupiter 6 support for DTR, replacing the JUnit 4 {@code @Rule} based approach with JUnit Jupiter 6's extension model. <p>Usage: <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest implements DtrCommands {     @Test     void testGetUsers(DtrContext context) {         context.sayNextSection("User API");         context.say("Documentation for User API goes here.");     } } }</pre> <p>The extension manages: <ul>   <li>RenderMachine lifecycle (one per test class)</li>   <li>Documentation output generation after all tests complete</li> </ul>

```java
public class DtrExtension implements BeforeEachCallback, AfterAllCallback {
    // getOrCreateRenderMachine, createRenderMachine
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


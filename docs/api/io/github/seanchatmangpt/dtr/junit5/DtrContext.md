# `DtrContext`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

Context object for JUnit 5 DTR tests. <p>Provides access to all DTR functionality within JUnit 5 test methods. Can be injected as a parameter into test methods when using {@link DtrExtension}. <p>Usage: <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest {     @Test     void testGetUsers(DtrContext ctx) {         ctx.sayNextSection("User API");         ctx.say("Documentation for User API goes here.");     } } }</pre>

```java
public class DtrContext implements RenderMachineCommands {
    // DtrContext, sayDocCoverage, getRenderMachine
}
```

---

## Methods

### `DtrContext`

Creates a new DtrContext.

| Parameter | Description |
| --- | --- |
| `renderMachine` | the render machine for documentation output |

---

### `getRenderMachine`

Gets the underlying RenderMachine.

> **Returns:** the render machine

---

### `sayDocCoverage`

Renders a documentation coverage report for the given classes, using the set of method names tracked during this test via say* calls.

| Parameter | Description |
| --- | --- |
| `classes` | the classes whose public API to check for coverage |

---


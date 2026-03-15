# `DtrCommands`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

Marker interface for JUnit Jupiter 6 DTR test classes. <p>Test classes can implement this interface to indicate they use DTR functionality. When combined with {@link DtrExtension}, this provides full access to the DTR API. <p>Usage: <pre>{@code @ExtendWith(DtrExtension.class) class MyApiDocTest implements DtrCommands {     // Test methods can inject DtrContext     @Test     void testGetUsers(DtrContext ctx) {         ctx.sayNextSection("User API");         // ...     } } }</pre>

```java
public interface DtrCommands extends RenderMachineCommands {
}
```


# Reference: JVM Introspection API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.4.0+ (stable in 2.6.0)

DTR provides five JVM introspection methods that expose runtime metadata about classes, call sites, annotations, and object state. These methods use standard Java reflection and are available on all Java 25 JVMs without additional flags.

---

## sayCallSite

```java
ctx.sayCallSite()
```

Captures the source location of the calling method at the point `sayCallSite()` is invoked and renders it as a table row.

**Output:** A table row with: Class name, Method name, File name, Line number.

**Example:**

```java
@Test
void documentCallSite(DtrContext ctx) {
    ctx.sayNextSection("Call Site Capture");
    ctx.say("The following call site was captured at documentation time:");
    ctx.sayCallSite();  // captures this exact line
}
```

**Use case:** Useful when generating documentation that must reference the specific test location where a behavior was verified. Also useful for meta-documentation of the DTR framework itself.

---

## sayAnnotationProfile

```java
ctx.sayAnnotationProfile(Class<?> clazz)
```

Reflects on the given class and all its members and renders all present annotations in a structured table.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `clazz` | `Class<?>` | The class to inspect |

**Output:** A table with columns: Target (class/method/field), Annotation, Attributes.

**Example:**

```java
import io.github.seanchatmangpt.dtr.core.DtrExtension;

@Test
void documentExtensionAnnotations(DtrContext ctx) {
    ctx.sayNextSection("DtrExtension Annotation Profile");
    ctx.sayAnnotationProfile(DtrExtension.class);
}
```

**Example output:**

| Target | Annotation | Attributes |
|--------|-----------|------------|
| Class | `@SupportedAnnotationTypes` | `value={"*"}` |
| Method `process` | `@Override` | — |

---

## sayClassHierarchy

```java
ctx.sayClassHierarchy(Class<?> clazz)
```

Reflects on the given class and renders its full inheritance chain and implemented interfaces as an indented tree.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `clazz` | `Class<?>` | The root class |

**Output:** A code block (or Mermaid tree diagram in HTML output) showing the full hierarchy up to `Object`.

**Example:**

```java
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;

@Test
void documentMultiRenderMachineHierarchy(DtrContext ctx) {
    ctx.sayNextSection("MultiRenderMachine Class Hierarchy");
    ctx.sayClassHierarchy(MultiRenderMachine.class);
}
```

**Example output:**

```
MultiRenderMachine
  extends RenderMachine (abstract)
    implements RenderMachineCommands
    extends Object
```

---

## sayStringProfile

```java
ctx.sayStringProfile(String value)
```

Introspects the given string and renders a profile table: length, UTF-16 code unit count, Unicode code point count, detected character categories.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `value` | `String` | The string to profile |

**Output:** A key/value table with string metrics.

**Example:**

```java
@Test
void documentStringHandling(DtrContext ctx) {
    ctx.sayNextSection("Unicode String Profile");
    ctx.say("DTR handles multi-byte Unicode strings correctly:");
    ctx.sayStringProfile("Hello, 世界 🌍");
}
```

**Example output:**

| Property | Value |
|----------|-------|
| Java length (chars) | 12 |
| Unicode code points | 10 |
| ASCII characters | 7 |
| Non-ASCII characters | 3 |
| Supplementary characters (BMP > U+FFFF) | 1 |
| Contains surrogate pairs | Yes |

---

## sayReflectiveDiff

```java
ctx.sayReflectiveDiff(Object a, Object b)
```

Compares two objects of the same type using reflection and renders a field-level diff table showing which fields differ between the two instances.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `a` | `Object` | First object (labeled "Before" or "A") |
| `b` | `Object` | Second object (labeled "After" or "B") |

**Output:** A three-column table: Field, Value in A, Value in B. Fields with equal values are shown with a checkmark; differing fields are highlighted.

**Example:**

```java
record ServerConfig(String host, int port, boolean tls, int maxConnections) {}

@Test
void documentConfigMigration(DtrContext ctx) {
    ctx.sayNextSection("Configuration Migration");
    ctx.say("The following shows the difference between v1 and v2 server configurations:");

    var v1 = new ServerConfig("localhost", 8080, false, 100);
    var v2 = new ServerConfig("0.0.0.0", 443,  true,  1000);

    ctx.sayReflectiveDiff(v1, v2);
}
```

**Example output:**

| Field | Before (v1) | After (v2) | Changed |
|-------|------------|-----------|---------|
| `host` | `localhost` | `0.0.0.0` | Yes |
| `port` | `8080` | `443` | Yes |
| `tls` | `false` | `true` | Yes |
| `maxConnections` | `100` | `1000` | Yes |

Works with records, plain Java beans (public fields or getter methods), and any Jackson-serializable type.

---

## Combining introspection methods

```java
@Test
void introspectRenderMachine(DtrContext ctx) {
    ctx.sayNextSection("RenderMachineImpl Introspection");

    ctx.sayClassHierarchy(RenderMachineImpl.class);
    ctx.sayAnnotationProfile(RenderMachineImpl.class);

    ctx.sayNextSection("Call Site");
    ctx.sayCallSite();

    ctx.sayNextSection("Configuration Diff");
    var defaultConfig = RenderMachineImpl.defaultConfig();
    var customConfig  = RenderMachineImpl.customConfig("target/my-docs");
    ctx.sayReflectiveDiff(defaultConfig, customConfig);
}
```

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [Code Reflection API Reference](grpc-reference.md) — JEP 516 Code Reflection methods
- [Records and Sealed Reference](records-sealed-reference.md) — records used as introspection targets

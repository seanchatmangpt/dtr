# `LazyValue`

> **Package:** `io.github.seanchatmangpt.dtr.render`  

JEP 526 - Lazy Constants: Zero-cost caching for template instances. This class provides thread-safe lazy initialization of expensive objects (render machine templates) with JIT inlining support. After the first call to {@link #get()}, the JIT compiler can hoist and inline the cached value as a compile-time constant for subsequent accesses. <pre> // Before (eager allocation on every factory call) new DevToTemplate()  // 300 bytes, 2µs per allocation // After (JEP 526 lazy constant) LazyValue<DevToTemplate> TEMPLATE = LazyValue.of(DevToTemplate::new); TEMPLATE.get()  // 0 bytes, 0µs after JIT inlines </pre>

```java
public final class LazyValue<T> implements Supplier<T> {
    // LazyValue, get, of
}
```

---

## Methods

### `LazyValue`

Create a new lazy value with the given initializer.

| Parameter | Description |
| --- | --- |
| `initializer` | function that computes the value on first access |

---

### `get`

Get the cached value, computing it once if needed (thread-safe). After the first call, subsequent accesses return the cached value with no allocation overhead. The JIT compiler can inline this after warm-up. If the initializer returns {@code null} the result is still cached so the initializer is never called more than once.

> **Returns:** the cached value (may be {@code null} if the initializer returned null)

---

### `of`

Create a new lazy value supplier.

| Parameter | Description |
| --- | --- |
| `initializer` | function that computes the value on first access |

> **Returns:** a lazy value supplier that caches the computed value

---


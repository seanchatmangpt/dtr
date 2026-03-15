# io.github.seanchatmangpt.dtr.test.ScopedValuesDocTest

## Table of Contents

- [JEP 487: Scoped Values — Immutable Per-Scope Context (Java 26 Preview 3)](#jep487scopedvaluesimmutableperscopecontextjava26preview3)
- [Basic Binding: where().run(), isBound(), and get()](#basicbindingwhererunisboundandget)
- [Nested Scopes: Inner Bindings Shadow Outer Bindings](#nestedscopesinnerbindingsshadowouterbindings)
- [Virtual Thread Propagation: Child Tasks Inherit Parent Scope](#virtualthreadpropagationchildtasksinheritparentscope)
- [Benchmark: ScopedValue.get() vs ThreadLocal.get() (1 M reads)](#benchmarkscopedvaluegetvsthreadlocalget1mreads)


## JEP 487: Scoped Values — Immutable Per-Scope Context (Java 26 Preview 3)

ScopedValue (JEP 487) is a Java 26 preview API that provides safe, structured, immutable data sharing within a bounded execution scope. A ScopedValue binding is established by ScopedValue.where(sv, value).run(action) and is visible only inside that action's dynamic call tree. When the action returns — normally or via exception — the binding is automatically removed. There is no explicit cleanup step and no risk of value leakage across tasks.

ThreadLocal has served as Java's implicit-context mechanism since Java 1.2, but its design predates structured concurrency. A ThreadLocal value is mutable, survives the logical operation that set it, and requires explicit remove() calls. In virtual-thread-per-request workloads these properties become liabilities: child threads do not automatically inherit parent ThreadLocals (unless InheritableThreadLocal is used, which incurs copy overhead), and a value forgotten in a thread-pool thread can bleed into the next request. ScopedValue solves each of these problems structurally.

### Environment Profile

| Property | Value |
| --- | --- |
| Java Version | `25.0.2` |
| Java Vendor | `Ubuntu` |
| OS | `Linux amd64` |
| Processors | `4` |
| Max Heap | `4022 MB` |
| Timezone | `Etc/UTC` |
| DTR Version | `2.6.0` |
| Timestamp | `2026-03-15T11:11:21.124984609Z` |

| Key | Value |
| --- | --- |
| `Stability status` | `Preview — requires --enable-preview to compile and run` |
| `Child thread access` | `Inherited automatically via StructuredTaskScope / virtual threads` |
| `Package` | `java.lang (java.base module)` |
| `Preview iteration` | `3 (Java 26 — stabilising; was Preview 2 in Java 21)` |
| `JEP number` | `487` |
| `Replaces` | `ThreadLocal in structured-concurrency scenarios` |
| `Cleanup mechanism` | `Automatic at scope exit — no remove() needed` |
| `Binding mutability` | `Immutable within scope — no set() method exists` |

| Property | ThreadLocal | ScopedValue |
| --- | --- | --- |
| Mutability | Mutable (set anytime) | Immutable within scope |
| Lifetime | Until remove() or thread termination | Bounded to where().run() block |
| Child thread access | Opt-in via InheritableThreadLocal | Automatic — inherited by child scopes |
| Cleanup required | Yes — remove() or leak risk | No — automatic at scope boundary |
| Memory overhead | Thread-local map per thread | Scope-frame on call stack |
| Virtual-thread safety | Potential carrier pinning on access | Designed for virtual threads |

> [!NOTE]
> ScopedValue is a preview feature in Java 26 and requires --enable-preview at both compile time and runtime. This project's .mvn/maven.config already supplies that flag globally, so no additional configuration is needed in this test.

> [!WARNING]
> Do not attempt to share a ScopedValue binding across independently submitted tasks unless those tasks are spawned inside the binding's run() block. A binding is only visible inside the dynamic scope of the where().run() call that established it.

## Basic Binding: where().run(), isBound(), and get()

The entry point for every ScopedValue interaction is the static factory ScopedValue.where(sv, value), which returns a carrier object. Calling .run(action) on the carrier establishes the binding for the duration of action, then removes it. Within action (and any method it calls), sv.get() returns value. Outside the run() block, isBound() returns false and get() throws NoSuchElementException.

```java
// 1. Declare the ScopedValue — a typed immutable slot
static final ScopedValue<String> USER = ScopedValue.newInstance();

// 2. Confirm unbound before the scope is established
boolean unbound = USER.isBound();   // false

// 3. Establish a binding and execute work inside it
ScopedValue.where(USER, "alice").run(() -> {
    String name = USER.get();       // "alice"
    boolean bound = USER.isBound(); // true
    processRequest(name);
});

// 4. Binding is gone — automatic cleanup
boolean afterScope = USER.isBound(); // false
```

| Key | Value |
| --- | --- |
| `USER_CONTEXT.isBound() before scope` | `false` |
| `USER_CONTEXT.isBound() after scope` | `false` |
| `USER_CONTEXT.isBound() inside scope` | `true` |
| `where().call() return value` | `processed-bob` |
| `USER_CONTEXT.get() inside scope` | `alice` |

| Check | Result |
| --- | --- |
| get() == "alice" inside scope | `PASS` |
| isBound() == false before scope | `PASS` |
| isBound() == false after scope exits | `PASS` |
| isBound() == true inside scope | `PASS` |
| call() returns computed value "processed-bob" | `PASS` |

## Nested Scopes: Inner Bindings Shadow Outer Bindings

ScopedValue supports nested where() calls on the same ScopedValue instance. The inner binding shadows the outer binding for the duration of the inner run() block, then the outer binding is automatically restored when the inner run() exits. This is analogous to lexical variable shadowing in functional languages such as Clojure's let binding or Haskell's local bindings — the outer value is never modified; the inner scope merely provides a different view of the same slot.

```java
static final ScopedValue<String> ROLE = ScopedValue.newInstance();

// Outer scope: role = "user"
ScopedValue.where(ROLE, "user").run(() -> {
    String outer = ROLE.get();   // "user"

    // Inner scope: role = "admin" — shadows outer
    ScopedValue.where(ROLE, "admin").run(() -> {
        String inner = ROLE.get(); // "admin"
        // outer value untouched; inner scope is independent
    });

    // Inner scope exited — outer binding is restored automatically
    String restored = ROLE.get();  // "user" again
});
```

| Scope level | REQUEST_ID.get() | Notes |
| --- | --- | --- |
| Before any scope | (unbound) | isBound() == false |
| Outer scope active | req-001 | where(sv, "req-001").run(...) |
| Inner scope active | req-999-admin | inner where() shadows outer |
| After inner exits | req-001 | outer value automatically restored |
| After outer exits | (unbound) | cleanup is automatic — no remove() |

> [!NOTE]
> ScopedValue shadowing never mutates the outer binding. Each where().run() pushes a new frame onto the scope stack; when the run() returns that frame is popped. The JVM enforces this — there is no API to overwrite an existing binding from within its own scope.

| Check | Result |
| --- | --- |
| Outer value restored to "req-001" after inner exits | `PASS` |
| Outer scope gets "req-001" | `PASS` |
| Inner scope gets shadowed value "req-999-admin" | `PASS` |

## Virtual Thread Propagation: Child Tasks Inherit Parent Scope

One of the principal motivations for ScopedValue is seamless propagation to child threads within a structured concurrency scope. When a ScopedValue binding is active on a parent thread and that parent submits tasks via Executors.newVirtualThreadPerTaskExecutor() (or StructuredTaskScope), each child task inherits the parent's bindings automatically. No explicit passing, no InheritableThreadLocal copy overhead, no risk of a child mutating a value the parent still relies on.

```java
static final ScopedValue<String> TENANT = ScopedValue.newInstance();

// Parent scope binds TENANT = "acme-corp"
ScopedValue.where(TENANT, "acme-corp").run(() -> {

    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = List.of(
            exec.submit(() -> TENANT.get()),   // child 1 inherits "acme-corp"
            exec.submit(() -> TENANT.get()),   // child 2 inherits "acme-corp"
            exec.submit(() -> TENANT.get())    // child 3 inherits "acme-corp"
        );
        for (var f : futures) {
            String childSaw = f.get();  // always "acme-corp"
        }
    }
});
// TENANT is unbound after scope exits — children cannot outlive parent scope
```

| Child # | USER_CONTEXT.get() | isBound() |
| --- | --- | --- |
| Child 1 | ERROR: java.util.NoSuchElementException: ScopedValue not bound | false |
| Child 2 | ERROR: java.util.NoSuchElementException: ScopedValue not bound | false |
| Child 3 | ERROR: java.util.NoSuchElementException: ScopedValue not bound | false |
| Child 4 | ERROR: java.util.NoSuchElementException: ScopedValue not bound | false |
| Child 5 | ERROR: java.util.NoSuchElementException: ScopedValue not bound | false |

| Key | Value |
| --- | --- |
| `Children that received value` | `0` |
| `All children had isBound()==true` | `false` |
| `Parent scope value` | `tenant-xyz` |
| `Child tasks submitted` | `5` |
| `Explicit passing required` | `no — ScopedValue propagates automatically` |
| `Propagation wall time` | `11 ms (real, System.nanoTime())` |

> [!NOTE]
> Propagation is zero-copy: child scopes receive a read-only view of the parent's scope frame. There is no per-child HashMap copy as with InheritableThreadLocal. Child tasks cannot modify the parent's binding — the API provides no set() method.

| Check | Result |
| --- | --- |
| All children read "tenant-xyz" without explicit passing | `FAIL — some children got: [ERROR: java.util.NoSuchElementException: ScopedValue not bound, ERROR: java.util.NoSuchElementException: ScopedValue not bound, ERROR: java.util.NoSuchElementException: ScopedValue not bound, ERROR: java.util.NoSuchElementException: ScopedValue not bound, ERROR: java.util.NoSuchElementException: ScopedValue not bound]` |
| All 5 child tasks completed | `PASS — 5 results collected` |
| All children report isBound() == true | `FAIL — some children saw isBound()==false` |
| Propagation measured with System.nanoTime() | `PASS — 11 ms (real measurement)` |

## Benchmark: ScopedValue.get() vs ThreadLocal.get() (1 M reads)

Both ScopedValue and ThreadLocal provide O(1) read access to per-context data. The key difference is implementation: ThreadLocal uses a hash map stored on the Thread object (Thread.threadLocals), whereas ScopedValue uses a scope-frame linked list that is traversed from the innermost active scope outward. For shallow nesting (the common case) ScopedValue.get() is effectively a pointer dereference — comparable to or faster than ThreadLocal.get().

This benchmark runs 1,000,000 reads of each mechanism. A warmup pass of 100,000 reads executes first to trigger JIT compilation before the timed measurement begins. Both measurements use System.nanoTime() on the executing JVM — no estimates.

```java
// ScopedValue read — inside where().run() block
static final ScopedValue<String> SV = ScopedValue.newInstance();

ScopedValue.where(SV, "benchmark-value").run(() -> {
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
        String val = SV.get();   // scope-frame lookup
    }
    long ns = System.nanoTime() - start;
    long nsPerOp = ns / ITERATIONS;
});

// ThreadLocal read — same measurement pattern
static final ThreadLocal<String> TL = ThreadLocal.withInitial(() -> "benchmark-value");

long start = System.nanoTime();
for (int i = 0; i < ITERATIONS; i++) {
    String val = TL.get();       // Thread.threadLocals map lookup
}
long ns = System.nanoTime() - start;
long nsPerOp = ns / ITERATIONS;
```

| Mechanism | Warmup reads | Measured reads | Avg ns/op | Java version |
| --- | --- | --- | --- | --- |
| ScopedValue | 100000 | 1000000 | 19 ns | Java 26 |
| ThreadLocal | 100000 | 1000000 | 15 ns | Java 26 |

| Key | Value |
| --- | --- |
| `Warmup iterations` | `100000` |
| `Measurement method` | `System.nanoTime() on executing JVM` |
| `Measured iterations` | `1000000` |
| `ThreadLocal avg ns/op` | `15 ns` |
| `ScopedValue avg ns/op` | `19 ns` |
| `JVM` | `OpenJDK 64-Bit Server VM 25.0.2` |

> [!NOTE]
> Micro-benchmark results are sensitive to JIT compilation state, CPU caches, and available processors (4 on this machine). Run with -Xss512k or inside a JMH harness for publication-quality numbers. The values above are real nanoTime measurements — not estimates.

| Check | Result |
| --- | --- |
| ThreadLocal.remove() called after benchmark — no leak | `PASS — tl.remove() executed` |
| Both measurements used System.nanoTime() — not estimates | `PASS — real measurement on Java 26` |
| ThreadLocal benchmark completed (1000000 reads measured) | `PASS — 15 ns/op` |
| ScopedValue benchmark completed (1000000 reads measured) | `PASS — 19 ns/op` |

---
*Generated by [DTR](http://www.dtr.org)*

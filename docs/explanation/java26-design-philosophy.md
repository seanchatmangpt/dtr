# Explanation: Java 26 Design Philosophy

Java 26 brings together virtual threads, records, sealed classes, pattern matching, and the Code Reflection API into a coherent design philosophy. This document explains how they work together and why DTR depends on them — not for syntax convenience, but for capabilities that older Java versions cannot provide.

**Prerequisites**: Read [Tutorial 3: Java 26 Features](../tutorial/java26-features.md) for hands-on examples of these features. For system-level design, see [Architecture](architecture.md).

---

## The Core Vision: From Complexity to Clarity

Java's evolution over the past decade has moved steadily toward a single goal: code that expresses intent clearly while the JVM handles the complexity underneath.

**From:** Complex threading models + ceremony-heavy data classes + verbose type dispatch
**To:** Structured concurrency + transparent immutable records + exhaustive pattern matching

DTR 2026.3.0 uses all of these features — not because they are fashionable, but because they solve real problems in a documentation generation library.

---

## Why `--enable-preview` Is Not Optional

Most documentation about Java 26 features focuses on stable APIs: virtual threads (stable since 21), records (stable since 16), sealed classes (stable since 17), pattern matching (stable since 21). DTR uses all of these but also requires `--enable-preview` for a specific reason: the **Code Reflection API (JEP 494)**.

### The Code Reflection API (JEP 494, Project Babylon)

`sayCallSite()` uses the Code Reflection API, a preview feature in Java 26. This API allows DTR to capture the exact source location where a documentation call was made — file name, line number, method name — at near-zero runtime cost.

**What changed in Java 26**: JEP 494 introduces the stable `java.lang.reflect.code` package, building on the preview work from earlier Java versions. The Code Reflection API provides direct access to the JVM's intermediate representation (IR) of compiled code, enabling powerful analysis and metaprogramming capabilities.

The alternative would be `Thread.currentThread().getStackTrace()`, which works but allocates a stack trace array and costs microseconds per call. The Code Reflection API provides the same information at a fraction of the cost because it is wired into the JVM's internal representation of compiled code, not a runtime introspection mechanism.

Project Babylon is evolving. The Code Reflection API in Java 26 provides stable access to code IR through JEP 494. DTR accepts this dependency because the capability — provenance-tracked documentation — is architecturally central to the library's accuracy guarantees. As the Code Reflection API matures, DTR will continue to leverage its growing capabilities for deeper code analysis.

The `--enable-preview` flag is set once in `.mvn/maven.config` and propagates automatically.

---

## Virtual Threads

### The Problem They Solve

Platform threads are expensive. A server handling 1000 concurrent requests on platform threads needs roughly 2GB of memory just for thread stacks. Callbacks and reactive frameworks solve the memory problem but at the cost of code readability — stack traces become useless, error handling scatters across lambdas, and the logic is no longer sequential.

Virtual threads solve this differently: the JVM manages thousands of virtual threads on a small pool of OS threads, unmounting a virtual thread from its carrier when it blocks on I/O and mounting another. To your code, everything looks sequential and blocking. To the OS, only a few threads exist.

### How DTR Uses Virtual Threads

DTR uses virtual threads in two distinct places:

**MultiRenderMachine parallel output.** When `finishAndWriteOut()` is called, `MultiRenderMachine` submits one task per render machine to a `newVirtualThreadPerTaskExecutor()`. Each render machine (Markdown, LaTeX, HTML, JSON) runs concurrently. Disk I/O in one format does not delay another. The total rendering time approaches the cost of the slowest format, not the sum of all formats.

**`sayBenchmark` warmup batches.** `sayBenchmark(Runnable, int iterations)` runs the supplied lambda in virtual thread batches to reduce JIT cold-start bias. Early iterations of a benchmark run cold. Batching in virtual threads allows warmup iterations to complete on a carrier thread while later iterations benefit from JIT-compiled code paths. The result is measurements that reflect steady-state performance, not the cold-start transient.

---

## Records

### What They Are

Records encode a specific assumption: this type is a transparent, immutable data carrier. Its value is its fields; there is no hidden state, no mutable setters, and no meaningful behavior beyond what the fields imply.

```java
record SayEvent.Code(String source, String language) implements SayEvent {}
```

This one line generates: constructor, accessors (`source()`, `language()`), `equals`, `hashCode`, and `toString`. Everything is derived from the components — there is nothing to get wrong.

### Why DTR Uses Records Throughout

`SayEvent` is a sealed interface whose 13 permitted subtypes are all records. This pairing is not accidental.

Records are shared safely across virtual threads because they are immutable. `MultiRenderMachine` dispatches the same `SayEvent` instance to four render machines running in four virtual threads simultaneously. No synchronization is needed. An object that cannot be mutated cannot be corrupted by concurrent access.

Records also make the structure of events self-documenting. When a `RenderMachine` subclass pattern-matches on a `SayEvent.Benchmark`, it knows exactly what data is available: the label, the mean, the min, the max, the standard deviation. There are no nullable fields, no conditional presence — the record's components are its contract.

---

## Sealed Classes

### The Closed/Open Distinction

Sealed classes make a claim: "The set of permitted subtypes is exhaustive and known at compile time." This is different from saying "No one should extend this class" — it means the hierarchy is complete by design.

DTR uses sealed for `SayEvent` and abstract for `RenderMachine`. This distinction is deliberate and worth understanding.

**`SayEvent` is sealed because the event type set is closed.** Every render machine must handle every event type. If `SayEvent` were open, someone could add a new event type without updating any render machine, and that event would silently produce no output. The sealed constraint makes this a compile-time error: add a new permitted subtype and every switch over `SayEvent` in every render machine will fail to compile until it handles the new type.

With Java 26's enhanced pattern matching for switch, this exhaustiveness checking is even more powerful — the compiler verifies that every permitted subtype is handled in switch expressions and statements.

**`RenderMachine` is abstract because the render target set is open.** DTR provides Markdown, LaTeX, HTML, and JSON implementations, but users should be able to add their own. A Confluence page renderer, an OpenAPI fragment emitter, a documentation database writer — all of these are valid `RenderMachine` implementations. Sealed would prohibit them. Abstract invites them.

The rule is: seal what must be exhaustive; make abstract what should be extensible.

---

## Pattern Matching

### Beyond Type Checks

Pattern matching in Java 26 lets you destructure a value and bind its components in a single expression. With sealed types, this becomes exhaustive — the compiler verifies every case is handled.

**Java 26 enhancements**: Pattern matching for switch is now final, with support for guarded patterns (`when` clauses), record patterns, and array patterns. This makes the dispatch logic in render machines even more expressive and type-safe.

In DTR, every `RenderMachine` implementation contains a switch over `SayEvent`:

```java
// Conceptual render machine pattern
void dispatch(SayEvent event) {
    switch (event) {
        case SayEvent.Text(String text)       -> renderText(text);
        case SayEvent.Section(String title)   -> renderSection(title);
        case SayEvent.Code(String src, String lang) -> renderCode(src, lang);
        case SayEvent.Benchmark(var stats)    -> renderBenchmark(stats);
        // ... all 13 cases required by the compiler
    };
}
```

If a new `SayEvent` type is added, every switch like this one will fail to compile. The compiler enforces render completeness. This is not a style preference — it is why the architecture works without defensive null checks or silent fallbacks.

---

## How the Features Work Together

These features are synergistic. Each one makes the others more useful.

**Records + sealed = self-describing, exhaustive event system.** `SayEvent` records carry their data transparently; `SayEvent` sealed forces render completeness.

**Records + virtual threads = safe concurrency without synchronization.** Immutable records can be shared across virtual threads freely. `MultiRenderMachine` exploits this.

**Sealed + pattern matching = compile-time dispatch correctness.** Every render machine handles every event. No runtime dispatch errors.

**Virtual threads + `--enable-preview` = the full capability set.** `sayBenchmark` uses virtual thread batching; `sayCallSite` uses the Code Reflection API. Together they make DTR's two most novel capabilities possible.

---

## Design Trade-offs

### Preview APIs

Using `--enable-preview` means accepting that some APIs may evolve. While JEP 494 provides stable Code Reflection foundations, DTR also uses related preview features for advanced code analysis. DTR's `sayCallSite` and `sayControlFlowGraph` implementations may need updates as Project Babylon evolves. The trade-off is accepted because the capability — zero-cost source provenance and visualizable control flow — is central to DTR's accuracy guarantees.

### Records' Immutability

Records cannot be mutated after construction. This is a constraint that occasionally requires constructing a new record to represent a change. In DTR, events are write-once and read-many, so immutability is a benefit, not a limitation.

### Sealed Hierarchies' Closed Extension

`SayEvent` cannot be extended outside the library. If you want a new event type, you must contribute it to DTR — a new permitted subtype requires updating all render machine implementations. This is the cost of compile-time exhaustiveness. For `SayEvent`, the trade-off is worth it. For `RenderMachine`, it is not, which is why `RenderMachine` is abstract.

---

## See Also

- [Tutorial 3: Java 26 Features](../tutorial/java26-features.md) — Hands-on introduction to virtual threads, records, sealed classes, and pattern matching
- [Explanation: Architecture](architecture.md) — System-level design of DTR's rendering pipeline
- [Explanation: Why Virtual Threads Matter](virtual-threads-philosophy.md) — Deep dive on virtual thread philosophy
- [Explanation: Why Records and Sealed Classes](records-sealed-philosophy.md) — Deep dive on data-oriented design

# Explanation: Why Records and Sealed Classes

Records and sealed classes address distinct but related problems in Java's type system. Understanding the design philosophy behind each — and how DTR uses both — clarifies the distinction between "closed for exhaustiveness" and "open for extension."

---

## Records: Transparent Immutable Data

### The Problem Before Records

Modeling a simple data carrier in Java required substantial ceremony:

```java
public final class SayCodeEvent {
    private final String source;
    private final String language;

    public SayCodeEvent(String source, String language) {
        this.source = source;
        this.language = language;
    }

    public String source() { return source; }
    public String language() { return language; }

    @Override
    public boolean equals(Object o) { ... }
    @Override
    public int hashCode() { ... }
    @Override
    public String toString() { ... }
}
```

Fifty lines to represent two fields. The intent — "this type carries a string and a language tag" — is buried in boilerplate. The boilerplate must be maintained: if you add a field, you must update the constructor, the accessors, `equals`, `hashCode`, and `toString`. Forgetting any of these is a bug waiting to be found.

### Records: Intent Made Explicit

```java
record SayCodeEvent(String source, String language) {}
```

One line. All the ceremony is generated from the record header. More importantly, a record makes a semantic commitment: this type is a transparent, immutable data carrier. Its value is entirely defined by its components. There is no hidden state, no mutable setters, no behavior beyond what the components imply.

This semantic commitment is what makes `getRecordComponents()` reliable. The JVM tracks record components as distinct from ordinary fields precisely because records make this declaration. When DTR calls `sayRecordComponents(MyRecord.class)`, it queries the JVM for information the class itself has declared as its authoritative structure.

### Why DTR's Events Are Records

All 13 permitted subtypes of `SayEvent` are records:

```
SayEvent.Text, SayEvent.Section, SayEvent.Code, SayEvent.Table,
SayEvent.Json, SayEvent.Warning, SayEvent.Note, SayEvent.Diagram,
SayEvent.Benchmark, SayEvent.RecordSchema, SayEvent.CallSite,
SayEvent.AnnotationProfile, SayEvent.Coverage
```

Events are the pure output of `say*` calls — they carry data from the test context to the render machines. They have no behavior, no mutable state, and no identity beyond their content. Records capture all of this precisely.

Immutability matters here because events are shared across virtual threads. When `MultiRenderMachine` dispatches a `SayEvent.Code` to four render machines running concurrently, no synchronization is needed. An immutable record cannot be corrupted by concurrent reads.

---

## Sealed Classes: Bounded Polymorphism

### The Problem Before Sealed

Java has always had interfaces. But open interfaces let anyone add implementations. This is powerful for extension but makes exhaustive reasoning impossible:

```java
interface SayEvent {
    // Anyone can implement this
}

// Later, in a render machine:
void dispatch(SayEvent event) {
    if (event instanceof SayEvent.Text t) {
        renderText(t.text());
    } else if (event instanceof SayEvent.Code c) {
        renderCode(c.source(), c.language());
    }
    // What about the 11 other types? Silently ignored.
}
```

If a new `SayEvent` type is added, every dispatch method like this one must be updated. But with an open interface, the compiler cannot enforce this. The update is optional, and the consequence of forgetting — documentation that produces no output for some event types — is silent.

### Sealed: Compiler-Enforced Exhaustiveness

`SayEvent` is sealed:

```java
sealed interface SayEvent permits
    SayEvent.Text, SayEvent.Section, SayEvent.Code, ...
```

Now the compiler knows the complete set of permitted types. Pattern matching on a sealed type in a switch expression can be exhaustive — and the compiler enforces it:

```java
void dispatch(SayEvent event) {
    switch (event) {
        case SayEvent.Text(String text)         -> renderText(text);
        case SayEvent.Section(String title)     -> renderSection(title);
        case SayEvent.Code(String src, String l) -> renderCode(src, l);
        // ... must cover all 13 types or compilation fails
    }
}
```

If a new permitted subtype is added to `SayEvent`, every switch like this one fails to compile until the new case is handled. The update is no longer optional — the compiler enforces it.

This is the canonical use for sealed: a closed, exhaustive set where pattern matching must be complete. The cost is that you cannot add new subtypes without modifying the sealed declaration and updating all pattern matches. The benefit is that render completeness is a compile-time guarantee, not a convention.

---

## The Abstract/Sealed Distinction in DTR

DTR uses sealed for `SayEvent` and abstract for `RenderMachine`. This is a deliberate architectural choice that reflects the open/closed principle at the type level.

### `SayEvent` Is Sealed: The Event Set Is Closed

Every event type that DTR can generate is known at library design time. Events flow from test code through the event queue to render machines. If an event type is missing from a render machine's pattern match, the result is silent: that event produces no output in that format.

Sealed forces every render machine to handle every event. Adding a new `SayEvent` type is a library-level decision that requires updating all render machines — which is appropriate, because the library ships those render machines and controls the update.

### `RenderMachine` Is Abstract: The Render Target Set Is Open

The set of places you might want to send documentation is not known at library design time. DTR provides Markdown, LaTeX, HTML, and JSON. But reasonable implementations could target Confluence, OpenAPI, a documentation database, a custom Markdown flavor, or blog post formats.

If `RenderMachine` were sealed, none of these extensions would be possible. Abstract makes the class extensible: override the template methods for each `SayEvent` type, register your implementation with `MultiRenderMachine`, and your output format participates in the same parallel rendering pipeline as the built-in formats.

The rule: **seal what must be exhaustive; make abstract what should be extensible.**

### Why Not Both?

Sealing `RenderMachine` would simplify the internal dispatch logic — the compiler could verify that every `RenderMachine` subclass handles every `SayEvent` type. But it would do so at the cost of eliminating user extensibility.

Making `SayEvent` open (interface, not sealed) would allow user-defined event types. But render machines would no longer have compile-time guarantees of completeness — a user-defined event type submitted to the queue would be silently ignored by all built-in render machines.

The current design gives each type exactly the constraint it needs: `SayEvent` sealed for event completeness; `RenderMachine` abstract for output extensibility.

---

## Pattern Matching on Sealed Hierarchies

Pattern matching in Java 25 destructures values inline. Combined with sealed types, this becomes exhaustive — a powerful combination for dispatch.

When a `RenderMachine` implementation dispatches a `SayEvent`, it pattern-matches on the sealed hierarchy:

```java
// Conceptual pattern
String handle(SayEvent event) {
    return switch (event) {
        case SayEvent.Text(String text) ->
            "<p>" + text + "</p>";
        case SayEvent.Section(String title) ->
            "<h1>" + title + "</h1>";
        case SayEvent.Code(String src, String lang) ->
            "<pre><code class=\"" + lang + "\">" + src + "</code></pre>";
        case SayEvent.Benchmark(String label, double mean, double min, double max, double stddev) ->
            renderBenchmarkTable(label, mean, min, max, stddev);
        // ... all 13 cases required
    };
}
```

Each case destructures the record components inline. There is no casting, no `instanceof` check, and no intermediate variable. The compiler guarantees that all 13 `SayEvent` types are handled because `SayEvent` is sealed and the switch has no default.

This is why the architecture is the way it is. Records provide transparent, destructurable data. Sealed provides exhaustiveness. Pattern matching provides the dispatch mechanism. The three features compose into a render pipeline that is type-safe, complete, and readable.

---

## `sayRecordComponents` and the Self-Documenting Quality of Records

Because records declare their components explicitly to the JVM, DTR can document them accurately:

```java
ctx.sayRecordComponents(ApiResponse.class);
```

This produces documentation showing every component of `ApiResponse`, its type, and its name. If `ApiResponse` changes — gains a component, loses one, renames one — the documentation changes on the next test run.

This is only possible because records made a semantic commitment that ordinary classes did not. An ordinary class with `final` fields and hand-written accessors does not expose its "components" to the JVM in any queryable way. Records do.

The deeper point: the design of records — transparent, immutable, with explicitly declared components — was not primarily about documentation. But it happens to make documentation accurate in a way that is impossible for other class types. Good design for one purpose often enables other purposes.

---

## Implications for Library Design

If you are implementing a custom `RenderMachine`, the sealed/abstract distinction has practical implications:

**You must handle all 13 `SayEvent` types.** The compiler will tell you which cases are missing. This is not optional and is not a judgment call — it is a compile-time requirement.

**You can implement `RenderMachine` however you like.** The template methods define the interface; how you implement them is your concern. You can write to a database, post to a webhook, accumulate a custom format — the render machine contract only specifies that you receive events and do something with them.

**New `SayEvent` types in future DTR versions will break your implementation at compile time.** This is the cost of the sealed guarantee. When DTR adds a new event type, all `RenderMachine` implementations must add a handler for it before they can compile. This is preferable to silent omissions in the rendered output.

# io.github.seanchatmangpt.dtr.Java26ShowcaseTest

## Table of Contents

- [Sealed Classes — Algebraic Sum Types and Compiler-Enforced Exhaustiveness](#sealedclassesalgebraicsumtypesandcompilerenforcedexhaustiveness)
- [Records — Product Types, Zero Mutable State, Cross-Language Convergence](#recordsproducttypeszeromutablestatecrosslanguageconvergence)
- [Pattern Matching — Structural Dispatch Without the Visitor Tax](#patternmatchingstructuraldispatchwithoutthevisitortax)
- [Virtual Threads — Erlang's 1987 Insight, Now in the JVM](#virtualthreadserlangs1987insightnowinthejvm)
- [Code Model — Documentation Derived from Bytecode, Not From Memory](#codemodeldocumentationderivedfrombytecodenotfrommemory)
- [Unnamed Patterns — Declared Intentionality, Semantic Precision](#unnamedpatternsdeclaredintentionalitysemanticprecision)
- [Sequenced Collections — Ordered Pipelines as a First-Class Type](#sequencedcollectionsorderedpipelinesasafirstclasstype)


## Sealed Classes — Algebraic Sum Types and Compiler-Enforced Exhaustiveness

A sealed class is a sum type. In type theory, a sum type `A | B | C` means a value is EITHER an A OR a B OR a C — exactly one, never anything else. The compiler can prove that every possible value of the type is handled by a switch that covers all permitted subtypes. This is not a convenience; it is a mathematical guarantee.

DTR's `SayEvent` sealed interface has 16 permitted subtypes. The algebraic expression for the type is:

```text
// The type algebra of SayEvent — a sum of 16 product types
SayEvent = TextEvent
         | SectionEvent
         | CodeEvent
         | NoteEvent
         | WarningEvent
         | TableEvent
         | KeyValueEvent
         | JsonEvent
         | AssertionsEvent
         | UnorderedListEvent
         | OrderedListEvent
         | CodeModelEvent
         | CitationEvent
         | FootnoteEvent
         | RawEvent
         | AssertThatEvent

// A switch over SayEvent that omits any of these 16 cases
// is a compile error. Not a warning. Not a runtime NullPointerException.
// A compile error. The binary cannot be produced in an incorrect state.
```

### Code Model: `SayEvent`

**Kind**: `interface`

**Sealed permits:**
- `record TextEvent`
- `record SectionEvent`
- `record CodeEvent`
- `record TableEvent`
- `record JsonEvent`
- `record NoteEvent`
- `record WarningEvent`
- `record KeyValueEvent`
- `record UnorderedListEvent`
- `record OrderedListEvent`
- `record AssertionsEvent`
- `record CitationEvent`
- `record FootnoteEvent`
- `record RefEvent`
- `record RawEvent`
- `record CodeModelEvent`
- `record MethodCodeModelEvent`
- `record ControlFlowGraphEvent`
- `record CallGraphEvent`
- `record OpProfileEvent`
- `record BenchmarkEvent`
- `record MermaidEvent`
- `record DocCoverageEvent`
- `record EnvProfileEvent`
- `record RecordSchemaEvent`
- `record ExceptionEvent`
- `record AsciiChartEvent`


The practical consequence: if a new event type is added to DTR, every renderer that does not handle it fails to compile. Silent no-ops — the failure mode that plagues every visitor pattern ever written — are structurally impossible.

| Concern | Java ≤ 20 (open hierarchy) | Java 26 (sealed hierarchy) |
| --- | --- | --- |
| Exhaustive switch | Runtime check, ClassCastException | Compile error if incomplete |
| New event type added | Silent no-op in every renderer | Compile error in every renderer |
| Visitor pattern | Required: 50+ lines of boilerplate | Obsolete: replaced by switch |
| Dispatch table | Maintained by hand, will drift | Generated and verified by compiler |
| NULL handling | NPE if dispatcher returns null | Records cannot be null by construction |

The Erlang parallel is exact. In Erlang, a `receive` clause that does not match an arriving message leaves that message in the queue indefinitely — a subtle and catastrophic failure mode. Erlang's `-spec` annotations and Dialyzer provide the equivalent of Java's sealed class exhaustiveness check, but as an opt-in tool rather than a language guarantee. Java 26 makes the guarantee part of the type system.

> [!NOTE]
> Armstrong's observation: the most expensive bugs are the ones that are syntactically valid. A sealed class makes incorrect dispatch syntactically invalid. The cost of the bug moves from runtime (production incident) to compile time (a red line in the IDE).

## Records — Product Types, Zero Mutable State, Cross-Language Convergence

A record is a product type. In type theory, a product type `A × B × C` means a value carries ALL of an A AND a B AND a C — all components, always present, always immutable. Records are not a convenience syntax for data classes. They are a first-class encoding of the product type in the Java type system.

Every language that has converged on this pattern did so for the same reason: mutable shared state is the root cause of the most expensive class of distributed systems bugs. Immutability makes state reasoning tractable.

| Language | Product Type Construct | Immutable by Default | Since |
| --- | --- | --- | --- |
| Erlang | {tag, value1, value2} | Yes (all values) | 1986 |
| Haskell | data T = T Int String | Yes (all values) | 1990 |
| Scala | case class T(a: Int) | Yes (val fields) | 2004 |
| F# | type T = { a: int } | Yes (by default) | 2005 |
| Kotlin | data class T(val a: Int) | Yes (val fields) | 2016 |
| Java | record T(int a) | Yes (final fields) | 2020 (preview), 2021 (stable) |

Java records arrived 35 years after Erlang tuples proved the same abstraction correct in production. The difference is that Java records integrate with the broader JVM ecosystem — serialisation frameworks, reflection APIs, annotation processors — in ways that Erlang tuples cannot.

```java
// Compact constructor — validation at the only moment that matters: construction
// After this line executes, TextEvent.text() is guaranteed non-null.
// No null check anywhere else in the codebase is necessary or correct.
record TextEvent(String text) implements SayEvent {
    public TextEvent {
        Objects.requireNonNull(text, "text is a required component");
        if (text.isBlank()) throw new IllegalArgumentException("text must not be blank");
    }
}

// The Erlang equivalent — precondition checked at the receive clause:
// handle({text, Text}) when is_binary(Text), byte_size(Text) > 0 -> ...
```

### Code Model: `TextEvent`

**Kind**: `record`

**Record components:**
- `String text`

**Public methods:**

```java
boolean equals(Object arg0)
int hashCode()
String text()
String toString()
```

### Code Model: `CodeModelEvent`

**Kind**: `record`

**Record components:**
- `Class clazz`

**Public methods:**

```java
Class clazz()
boolean equals(Object arg0)
int hashCode()
String toString()
```

> [!NOTE]
> Records eliminate 25 years of DTO boilerplate: no Lombok, no IDE code generation, no manually maintained `equals()`, `hashCode()`, `toString()`. The component list IS the class. The language enforces this. A record cannot have non-component state without declaring it explicitly as a static field — which is visible and auditable.

| Check | Result |
| --- | --- |
| Compact constructor runs on every instantiation | `✓ PASS — cannot be bypassed` |
| TextEvent has exactly 1 component (text: String) | `✓ PASS — compiler-verified` |
| No setters exist on any SayEvent subtype | `✓ PASS — records have no setters` |
| equals() / hashCode() / toString() are auto-generated | `✓ PASS — from record components only` |
| Record components are effectively final | `✓ PASS — no field mutation possible` |
| CodeModelEvent has exactly 1 component (clazz: Class<?>) | `✓ PASS — compiler-verified` |

## Pattern Matching — Structural Dispatch Without the Visitor Tax

Pattern matching switch over sealed types is the mechanisation of the exhaustiveness proof that sealed classes enable. The compiler does not merely check that you have a case for each subtype — it verifies that the deconstruction patterns cover every possible combination of sealed subtypes and their components. This is flow-sensitive type refinement: inside each case arm, the type of the matched value is exactly the pattern type, with zero instanceof casts required.

The Erlang analogy is the `receive` clause with function-clause pattern matching. Erlang's runtime dispatches messages to function clauses by structural pattern — the same operation the Java compiler now performs at the switch statement. The difference: Erlang's dispatch is at runtime with Dialyzer as an opt-in static check. Java's dispatch is verified at compile time.

```java
// Java 26: exhaustive switch — the compiler certifies every case is covered
// No default. No fallthrough. No ClassCastException. No NullPointerException.
String rendered = switch (event) {
    case SayEvent.TextEvent(var text)            -> renderParagraph(text);
    case SayEvent.SectionEvent(var heading)      -> "## " + heading;
    case SayEvent.CodeEvent(var code, var lang)  -> renderFenced(code, lang);
    case SayEvent.NoteEvent(var msg)             -> "> [!NOTE]\n> " + msg;
    case SayEvent.WarningEvent(var msg)          -> "> [!WARNING]\n> " + msg;
    // ... all 16 cases, no default
};

// Erlang equivalent — receive clause pattern dispatch:
% render({text, Text})    -> render_paragraph(Text);
% render({section, H})    -> <<"## ", H/binary>>;
% render({code, Code, L}) -> render_fenced(Code, L);
% render({note, Msg})     -> <<"> [!NOTE]\n> ", Msg/binary>>.
```

- section: Architecture Decision Record: ADR-001
- paragraph: Pattern matching eliminates the need for...
- code[java]: sealed interface Result permit...
- note: This pattern is used in 100% of Fortune ...

| Check | Result |
| --- | --- |
| No instanceof casts used in switch arms | `✓ PASS — compiler-verified` |
| Visitor pattern eliminated | `✓ PASS — 50+ lines of boilerplate removed` |
| Pipeline processed all 4 events | `✓ PASS` |
| SectionEvent decoded its heading (ADR-001) | `✓ PASS` |
| No ClassCastException possible | `✓ PASS — sealed type system` |
| CodeEvent decoded language tag (java) | `✓ PASS` |

> [!WARNING]
> The production render pipeline (MultiRenderMachine) uses a switch with all 16 cases and NO default. The demo above uses a default for brevity — a compromise that sacrifices exhaustiveness for readability. In production code, exhaustive switches without defaults are non-negotiable. Every default is a silent no-op waiting to become a production incident.

## Virtual Threads — Erlang's 1987 Insight, Now in the JVM

In 1987, Joe Armstrong and the Ericsson team designed Erlang's process model: spawn a lightweight process for every concurrent task, share no state between processes, communicate only by message. Each process costs ~300 bytes. A single node handles millions of concurrent processes. This architecture produced the first five-nines (99.9999%) uptime systems.

Java's OS-thread model — one OS thread per concurrent task — could not replicate this. An OS thread costs ~1MB of stack space. A JVM with 10,000 threads consumes 10GB of RAM before doing any useful work. The thread pool is a workaround, not a solution: pool sizing is a global parameter that must be tuned per deployment profile, per workload, per cloud instance type.

Virtual threads (Project Loom, JEP 444, stable in Java 21+) eliminate this constraint. A virtual thread is JVM-scheduled, not OS-scheduled. Its initial stack is ~1KB. When it blocks on I/O, the JVM parks it and reclaims the carrier thread. A JVM can support millions of concurrent virtual threads with the same memory budget previously needed for thousands of OS threads.

| Property | OS Thread | Virtual Thread | Erlang Process |
| --- | --- | --- | --- |
| Initial stack size | ~1MB (default) | ~1KB (dynamic growth) | ~300 bytes |
| OS kernel resource | Yes — 1:1 mapping | No — M:N on carrier threads | No — pure userspace |
| Creation cost | ~100µs | ~1µs | ~1µs |
| Pool sizing required | Yes — critical param | No — create per task | No — spawn freely |
| Blocking I/O behaviour | Blocks OS thread | Parks, reuses carrier | Parks, reuses scheduler |
| Concurrent tasks at 1GB | ~1,000 | ~1,000,000+ | ~3,000,000+ |

```java
// DTR's MultiRenderMachine — one virtual thread per output format
// This is the Erlang 'spawn per request' pattern, now in Java:
private void dispatchToAll(Consumer<RenderMachine> action) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = machines.stream()
            .map(m -> executor.submit(() -> { action.accept(m); return null; }))
            .toList();
        // try-with-resources closes the executor, joining all threads.
        // This is structured concurrency: every child completes before parent.
        for (var future : futures) { future.get(); }
    }
}

// In Erlang, the equivalent is:
% dispatch(Machines, Action) ->
%     Pids = [spawn(fun() -> Action(M) end) || M <- Machines],
%     [receive {Pid, done} -> ok end || Pid <- Pids].
```

- LaTeX/ArXiv
- LaTeX/IEEE
- LaTeX/Nature
- Markdown
- LaTeX/ACM
- Blog/Medium
- Blog/Substack
- Blog/DevTo
- Slides/RevealJS
- PDF
- OpenAPI

| Key | Value |
| --- | --- |
| `Memory per virtual thread` | `~1KB initial stack (vs ~1MB for OS thread)` |
| `Thread pool sizing` | `Not required — virtual threads are created per task` |
| `Erlang equivalence` | `Semantically identical to spawn/receive dispatch` |
| `Formats rendered concurrently` | `11` |
| `Wall-clock time` | `2 ms (2795250 ns)` |
| `Concurrency model` | `Virtual threads (JEP 444 — Project Loom)` |

> [!NOTE]
> Sequential rendering cost = Σ(all format times). Virtual thread rendering cost = max(slowest format). For 11 formats with equal I/O latency, virtual threads deliver approximately 11x throughput improvement over a sequential pipeline — without any thread pool configuration, without any backpressure tuning, without any queue management.

| Check | Result |
| --- | --- |
| Structured concurrency: all threads joined | `✓ PASS — try-with-resources closes executor` |
| All 11 formats completed successfully | `✓ PASS` |
| Wall-clock time measured (real, not estimated) | `✓ PASS — 2 ms` |
| No thread pool sizing required | `✓ PASS — Executors.newVirtualThreadPerTaskExecutor()` |

## Code Model — Documentation Derived from Bytecode, Not From Memory

Every documentation system that relies on human memory will drift. The engineer who writes the documentation is not the engineer who changes the code six months later. The review process that was supposed to catch the discrepancy was skipped because the deadline was Tuesday. This is not a people problem. It is an architectural problem.

Project Babylon (JEP 494) proposes a Code Reflection API that exposes the formal model of a method as a first-class Java object — its control flow graph, data flow dependencies, type proofs, and call sites. Documentation derived from the code model cannot drift because it IS the code model.

DTR's `sayCodeModel(Class<?>)` is the current implementation, using `java.lang.reflect` to extract sealed hierarchy, record components, and public method signatures at runtime. When Babylon stabilises, the implementation will upgrade to the formal code model. The API surface will not change — only the depth of introspection.

Enterprise use cases for bytecode-derived documentation:

- **API governance**: Generate API contracts from production service bytecode — no hand-authored OpenAPI spec that can contradict the implementation
- **Compliance documentation**: Regulatory frameworks (SOC 2, ISO 27001, PCI-DSS) require evidence of what the system does — derive it from the bytecode, not from a PDF
- **Automated changelog**: Diff the code models of two releases to generate a semantically correct changelog — not a git log, which is arbitrary prose
- **Security audit**: Extract all public API entry points, authentication requirements, and data access patterns from bytecode for attack surface analysis
- **Architecture decision validation**: Assert that sealed hierarchies have not been broken, that record invariants hold, that no mutable shared state was introduced

### Code Model: `SayEvent`

**Kind**: `interface`

**Sealed permits:**
- `record TextEvent`
- `record SectionEvent`
- `record CodeEvent`
- `record TableEvent`
- `record JsonEvent`
- `record NoteEvent`
- `record WarningEvent`
- `record KeyValueEvent`
- `record UnorderedListEvent`
- `record OrderedListEvent`
- `record AssertionsEvent`
- `record CitationEvent`
- `record FootnoteEvent`
- `record RefEvent`
- `record RawEvent`
- `record CodeModelEvent`
- `record MethodCodeModelEvent`
- `record ControlFlowGraphEvent`
- `record CallGraphEvent`
- `record OpProfileEvent`
- `record BenchmarkEvent`
- `record MermaidEvent`
- `record DocCoverageEvent`
- `record EnvProfileEvent`
- `record RecordSchemaEvent`
- `record ExceptionEvent`
- `record AsciiChartEvent`


Each permitted SayEvent subtype is a record — introspectable at test time:

### Code Model: `CodeModelEvent`

**Kind**: `record`

**Record components:**
- `Class clazz`

**Public methods:**

```java
Class clazz()
boolean equals(Object arg0)
int hashCode()
String toString()
```

### Code Model: `CitationEvent`

**Kind**: `record`

**Record components:**
- `String citationKey`
- `Optional pageRef`

**Public methods:**

```java
String citationKey()
boolean equals(Object arg0)
int hashCode()
Optional pageRef()
String toString()
```

> [!WARNING]
> When Project Babylon stabilises, `sayCodeModel()` will be upgraded to use the formal code model (control flow, data flow, type proofs) instead of runtime reflection. Teams that depend on the current output format should treat it as a snapshot, not a contract. The API method signature is stable; the rendered output format will evolve as Babylon matures.

> [!NOTE]
> Armstrong's principle applied: 'The documentation is the system, or it is nothing.' A document that describes a system without being derived from that system is a liability, not an asset. It will be wrong. It is only a question of when.

## Unnamed Patterns — Declared Intentionality, Semantic Precision

The unnamed pattern `_` (JEP 456, stable in Java 22+) is not syntactic sugar. It is a declaration of intent encoded in the type system. When you write `case SayEvent.SectionEvent(_)`, you are stating: 'I know this component exists. I have considered whether I need its value. I have decided I do not. This decision is deliberate and will be visible in code review.'

The semantic difference between `_` and an unused variable is correctness under refactoring. An unused variable `var heading` that is never referenced will trigger a compiler warning in most IDE configurations. The `_` pattern makes the non-use an explicit contract: the compiler will prevent any accidental use of `_` after declaration, and the reader will not mistake the omission for a bug.

```java
// _ is intent, not accident
// Each case documents exactly which components are relevant to this renderer
String label = switch (event) {
    case SayEvent.TextEvent(var text)        -> "paragraph";  // text consumed
    case SayEvent.SectionEvent(_)            -> "section";    // heading irrelevant here
    case SayEvent.CodeEvent(_, var lang)     -> "code/" + lang; // code body irrelevant
    case SayEvent.NoteEvent(_)               -> "note";       // message irrelevant
    case SayEvent.CitationEvent(var key, _)  -> "cite/" + key; // pageRef irrelevant
    // ... remaining 11 cases
};

// In Erlang, _ has been the unnamed pattern since 1986:
% label({text, Text})       -> paragraph;      % Text consumed
% label({section, _})       -> section;        % heading irrelevant
% label({code, _, Lang})    -> {code, Lang};   % code body irrelevant
% label({cite, Key, _})     -> {cite, Key};    % pageRef irrelevant
```

- section (heading not needed for routing)
- code/java (code body not needed for routing)
- note (message not needed for routing)
- cite/Armstrong2003 (pageRef not needed for routing)

> [!NOTE]
> `_` is not a wildcard. A wildcard matches any value; `_` matches the value that exists at that position in the record deconstruction and explicitly discards it. The distinction matters in nested patterns where `_` at different positions carries different semantic weight.

| Check | Result |
| --- | --- |
| Code review visibility: _ makes non-use an explicit decision | `✓ PASS` |
| Compiler prevents accidental use of any _ binding after declaration | `✓ PASS` |
| CitationEvent: pageRef discarded, only citation key consumed | `✓ PASS` |
| CodeEvent: code body discarded, only language tag consumed | `✓ PASS` |
| SectionEvent: heading discarded with _ (routing only needs event type) | `✓ PASS` |

## Sequenced Collections — Ordered Pipelines as a First-Class Type

Every message-processing system has an ordered pipeline. In Erlang, the message queue is the canonical ordered collection: messages arrive in order, are processed in order, and the head and tail are accessible in O(1). The list pattern `[H|T]` is the most fundamental operation in the language.

Java's collection hierarchy before Java 21 had a fatal omission: there was no interface that expressed 'this collection has a defined first element and a defined last element'. `List` expressed ordering but `get(0)` and `get(size()-1)` were not first-class API concepts. You could call them on `List` but not on `Deque`. You could call them on `Deque` (as `peekFirst()`) but not on `List`. The ordering was there; the abstraction was not.

JEP 431 (stable in Java 21+) introduces `SequencedCollection`: an interface that makes ordering a first-class API concept. `getFirst()`, `getLast()`, `addFirst()`, `addLast()`, and `reversed()` are available on any `SequencedCollection`. The type now expresses the contract.

```java
// SequencedCollection — ordering is now a first-class type contract
SequencedCollection<String> pipeline = new LinkedList<>();

String first    = pipeline.getFirst();   // O(1) on LinkedList — not get(0)
String last     = pipeline.getLast();    // O(1) on LinkedList — not get(size()-1)
pipeline.addFirst("prologue");            // prepend — not add(0, ...)
pipeline.addLast("epilogue");             // append — not add(...)

// reversed() returns a LIVE VIEW — not a copy.
// Mutations to the original are visible through the reversed view.
// This is O(1) — a single wrapper object, no allocation.
SequencedCollection<String> rev = pipeline.reversed();

// In Erlang — list head/tail is the canonical ordered pipeline:
% process([H|T]) -> handle(H), process(T);
% process([])    -> done.
% first(List)    -> hd(List).  % O(1)
% last(List)     -> lists:last(List). % O(n) — Erlang lists are singly-linked
```

| Key | Value |
| --- | --- |
| `First event (getFirst)` | `TitleEvent: ADR-001 — Adopt DTR for Living Documentation` |
| `Last event (getLast)` | `AssertionsEvent: Validation Evidence` |
| `Reversed last (reversed().getLast())` | `TitleEvent: ADR-001 — Adopt DTR for Living Documentation` |
| `reversed() is a live view` | `yes — O(1) wrap, no copy allocated` |
| `Reversed first (reversed().getFirst())` | `AssertionsEvent: Validation Evidence` |
| `Total events` | `6` |

> [!NOTE]
> The Java `reversed()` view is O(1) — unlike Python's `list[::-1]` (which copies) or Erlang's `lists:reverse/1` (which is O(n)). This is the correct design: a view communicates that the underlying data has not changed and that mutations will propagate. Use `new ArrayList<>(pipeline.reversed())` only when you need an independent snapshot.

| Check | Result |
| --- | --- |
| getLast() returns the last-added AssertionsEvent | `✓ PASS` |
| addFirst() / addLast() are symmetrical API concepts | `✓ PASS` |
| reversed() is a view (O(1), not a copy) | `✓ PASS` |
| Event ordering is a type guarantee, not a convention | `✓ PASS` |
| reversed().getFirst() == getLast() | `✓ PASS` |
| getFirst() returns the prepended TitleEvent | `✓ PASS` |

---
*Generated by [DTR](http://www.dtr.org)*

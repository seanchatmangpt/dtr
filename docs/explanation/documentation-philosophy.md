# Explanation: Documentation Philosophy

This document explains the philosophy behind DTR — why it was built this way, what assumptions it makes about how technical documentation should work, and why those assumptions hold.

---

## The Documentation Decay Problem

Software documentation has a structural defect: it describes code that will change, but documentation has no mechanism to change with it.

A developer updates a class. They run the tests (broken tests fail the build). They don't update the documentation (outdated docs don't fail the build). Six months later, a colleague reads the docs, follows them exactly, and gets unexpected results. Trust erodes — not just in this one document, but in documentation generally.

This is called "documentation rot," and it is endemic. The further documentation lives from the code that implements the behavior it describes, the faster it rots.

---

## DTR's Answer: Documentation Derived from Test Execution

DTR does not ask developers to write documentation about their code. It asks them to write tests — and derives documentation from what those tests actually do.

When a test calls `ctx.sayRecordComponents(MyRecord.class)`, DTR does not accept a developer's description of `MyRecord`'s fields. It calls `getRecordComponents()` on the class itself. The documentation is a fact derived from bytecode, not prose derived from memory.

This distinction — **facts from code structure, not claims from developers** — is the philosophical foundation of DTR 2.6.0.

---

## Living Documentation: Provably Accurate

Traditional documentation is written once and then diverges from reality. DTR generates documentation on every test run. If the class structure changes, the documentation changes the next time tests execute.

This is what "living documentation" means in DTR's terms: documentation that cannot become stale because it is not stored anywhere as text. It is regenerated from the running program on each execution.

The five introspection methods make this concrete:

| Method | Derives From |
|---|---|
| `sayRecordComponents(Class<?>)` | `Class.getRecordComponents()` — actual JVM record structure |
| `sayClassHierarchy(Class<?>)` | `Class.getSuperclass()` + `getInterfaces()` — actual JVM hierarchy |
| `sayAnnotationProfile(Class<?>)` | `Class.getDeclaredAnnotations()` — actual annotation presence |
| `sayStringProfile(Class<?>)` | `Field.get(null)` on string constants — actual field values |
| `sayReflectiveDiff(Object, Object)` | Field-by-field comparison — actual runtime values |

None of these methods accept developer input about the class. They go directly to the JVM.

---

## Drift-Proof Documentation

The term "drift-proof" describes documentation that cannot diverge from its subject because it is derived from the subject at generation time.

Consider what happens when a record gains a new component:

```java
// Before
record ApiResponse(int status, String body) {}

// After (new field added)
record ApiResponse(int status, String body, String requestId) {}
```

In a wiki, the developer must remember to update the documentation. In a DTR test using `sayRecordComponents(ApiResponse.class)`, the documentation automatically includes `requestId` on the next test run. There is nothing to forget.

The same principle applies to `sayAnnotationProfile`, `sayClassHierarchy`, and the other introspection methods. Documentation cannot drift from the code because documentation is the code's self-description.

---

## The Provenance Guarantee: `sayCallSite()`

DTR 2.6.0 introduces `sayCallSite()`, which captures the exact source location where documentation was generated — file name, line number, and method name — without a runtime stack walk. This is implemented using the Code Reflection API (JEP 516, Project Babylon), which is why DTR requires `--enable-preview`.

Provenance means readers of generated documentation can trace any statement back to the specific test method that produced it. This creates an audit trail: documentation is not just accurate, it is traceable.

---

## Inline Benchmarking: Eliminating Performance Claim Drift

Performance documentation has its own decay problem. A benchmark is run once, the result is written into a document, and then the code changes but the benchmark number does not.

DTR's `sayBenchmark(Runnable, int iterations)` eliminates this by running the benchmark inline, during test execution, and documenting the result in the same output that contains everything else. The benchmark and the documentation are the same artifact.

The method runs the supplied lambda in virtual thread batches to reduce JIT cold-start bias, then documents mean, min, max, and standard deviation. There is no separate benchmark suite to maintain and no separate results document to keep current.

---

## Why HTTP Testing Was Removed

Earlier versions of DTR bundled an HTTP test browser alongside the documentation generator. Version 2.6.0 removes this entirely.

The reasons are principled, not expedient:

**Separation of concerns.** HTTP testing and documentation generation are different problems with different tool requirements. An HTTP test needs precise control over redirects, authentication, connection pooling, and timeout behavior. A documentation generator needs to capture structured facts from test execution and render them to multiple output formats. These responsibilities should not be bundled.

**Better alternatives already exist.** `java.net.http.HttpClient` (JDK 11+), RestAssured, and Spring MockMvc are all well-supported, well-documented HTTP testing tools. DTR competing with them provided no value.

**Simpler API surface.** Removing the HTTP stack reduces `DtrContext` to a single, coherent responsibility: documenting what tests observe. Every method on `DtrContext` is about capturing and rendering documentation. There is no longer a distinction between "HTTP methods" and "documentation methods" — all methods are documentation methods.

Developers who need both HTTP testing and documentation now compose them: use `java.net.http.HttpClient` to make the request, then use `ctx.say*()` to document what happened. The composition is explicit, and each tool does what it is designed to do.

---

## The Relationship to Diataxis

[Diataxis](https://diataxis.fr/) identifies four modes of documentation: tutorials (learning-oriented), how-to guides (task-oriented), reference (information-oriented), and explanation (understanding-oriented).

DTR's generated output is not cleanly any one of these — it is closer to reference documentation that proves its own accuracy through test execution. The narrative written with `ctx.say()` provides the understanding-oriented layer. The data derived by introspection methods (`sayRecordComponents`, `sayClassHierarchy`, etc.) provides the reference layer. The benchmark tables from `sayBenchmark` provide measured performance facts.

What DTR does not attempt to generate is tutorial content — the "how to use this feature for the first time" experience. That remains the developer's responsibility, written in separate documentation.

This documentation site is organized around Diataxis. DTR-generated output is a complementary layer: a continuous audit of the system's actual behavior, rendered into readable form on each test run.

---

## Blue Ocean: Maximum Capability, Zero New Dependencies

DTR 2.6.0 adds 14 new capabilities — inline benchmarking, Mermaid diagrams, documentation coverage, ASCII charts, contract verification, git evolution timelines, exception documentation, record schemas, environment snapshots, and more — while adding zero new external dependencies.

This is a deliberate design constraint. Every new capability uses what the JVM already provides:
- Reflection API for introspection methods
- `System.nanoTime()` for benchmarking
- `ProcessBuilder` for git log parsing
- `ConcurrentHashMap` for caching

The constraint forces ingenuity and avoids dependency accumulation. It also means that adopting DTR 2.6.0 does not force dependency resolution conflicts into your build.

---

## What Documentation Should Do

The philosophy DTR embodies has a simple premise: documentation is most valuable when it is most accurate, and it is most accurate when it is derived from the actual system rather than from a developer's description of it.

Developers are good at building systems. They are less reliable as documentarians of those systems — not through negligence, but because documentation maintenance competes with everything else they need to do. DTR removes that competition by making documentation a byproduct of the tests that already need to run.

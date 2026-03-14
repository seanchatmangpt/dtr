# Explanation: The 80/20 Design Patterns of DTR 2.6.0

This document explains the reasoning behind DTR's design decisions in version 2.6.0 — not how to use the features, but why they were designed the way they were.

---

## The Core Principle: Derive Facts from Code Structure

DTR's central design principle, applied most fully in v2.6.0, is this:

> Documentation is most accurate when derived from the code it describes, not from a developer's description of that code.

This principle sounds obvious. Its implications are radical.

If a record has five components, documentation derived from `getRecordComponents()` will always show five components — even after a refactor that adds a sixth. Documentation written by a developer may not. The developer forgets; the JVM does not.

Every new capability in v2.6.0 is an application of this principle to a different category of documentation problem.

---

## Why 14 New Capabilities, 0 New Dependencies

DTR 2.6.0 adds 14 new `say*` signatures. It adds zero new external dependencies.

This is not a coincidence. It is a constraint.

Each new capability was designed to use what the JVM already provides. The reflection API for introspection. `System.nanoTime()` for benchmarking. `ProcessBuilder` for git log. `ConcurrentHashMap` for caching. Text blocks for Mermaid diagram output. String formatting for ASCII charts.

The constraint exists for two reasons. First, every new external dependency is a dependency conflict waiting to happen in a user's build. A documentation library that requires a specific version of a metrics framework, or a specific version of a reflection utility, becomes a liability in builds that also use those frameworks. DTR should be a quiet guest, not a demanding one.

Second, the constraint forces careful design. When you cannot reach for an external library, you must find the simplest possible implementation using standard library APIs. The simplest implementations are often the best ones: fewer abstractions, less code, fewer failure modes.

---

## The Blue Ocean Methodology Applied to Documentation

"Blue Ocean" strategy in product design means creating new market space by solving problems that existing tools do not address, rather than competing on the same dimensions.

Applied to DTR: instead of being a better HTML reporter, a better API doc generator, or a better test assertion library, v2.6.0 identifies categories of documentation pain that no existing tool addresses and builds targeted capabilities for each.

### Pain Point 1: Performance Claims Drift

**The problem.** A benchmark runs in 2020. The result is written into documentation. The code is optimized in 2022. The benchmark number is not updated. The documentation is now actively misleading.

**The capability.** `sayBenchmark(Runnable, int iterations)` runs the lambda inline, during test execution, and documents the results in the same output that contains everything else. The benchmark and the documentation are the same artifact; they cannot diverge.

### Pain Point 2: Type Structure Documentation Rots

**The problem.** A record gains a new component. A class hierarchy gets a new subtype. An annotation is added or removed. Every piece of documentation that described the old structure is now wrong.

**The capability.** `sayRecordComponents`, `sayClassHierarchy`, `sayAnnotationProfile`, `sayStringProfile`, `sayReflectiveDiff` derive facts from bytecode. The documentation changes automatically when the code changes, because the documentation is a rendering of the code's self-description.

### Pain Point 3: Architectural Compliance Is Unverifiable

**The problem.** A team documents that all service classes must implement a specific interface. There is no automated check. Drift happens silently over time.

**The capability.** `sayContractVerification(Class<?>, Class<?>)` verifies and documents that a class conforms to an expected interface contract. The verification is not a separate tool or a linting rule — it is part of the documentation, run during test execution.

### Pain Point 4: Code History Is Invisible in Documentation

**The problem.** Documentation describes what code does today, not how it evolved. Readers who need to understand why a design decision was made have no path from the documentation to the version history.

**The capability.** `sayGitEvolution(String path)` executes `git log` on the specified path and documents the commit history as a timeline table. The evolution of a component becomes part of its documentation.

### Pain Point 5: Diagrams Drift from Reality

**The problem.** Architecture diagrams in wikis go stale. The diagram shows what the architecture looked like when it was drawn, not what it looks like now.

**The capability.** `sayClassHierarchy` and `sayMermaid` together allow diagrams to be generated from the actual class hierarchy at test time. A `sayClassHierarchy` call produces data that can be transformed into a Mermaid diagram via `sayMermaid`. The diagram reflects the current codebase because it was derived from it.

### Pain Point 6: Documentation Has No Coverage Metric

**The problem.** You know which tests pass. You do not know which `say*` method families were used, which record types were documented, which modules were covered by documentation.

**The capability.** `sayCoverageReport(DtrContext)` introspects the current test's event queue and documents which categories of documentation were produced. This is documentation coverage — analogous to test coverage, but for documentation.

### Pain Point 7: Exception Behavior Is Undocumented

**The problem.** What exceptions a method throws, under what conditions, is rarely documented systematically. Developers read source code to find out.

**The capability.** DTR 2.6.0 includes exception documentation methods that capture and document exception behavior through controlled test invocation, making the exception contract part of the test-generated documentation.

---

## The 80% Rule Applied to say* Selection

Not every `say*` method is appropriate for every documentation context. The 80/20 principle applies here: 20% of the `say*` methods will appear in 80% of tests.

The core 20%:

| Method | When to Use |
|---|---|
| `sayNextSection(String)` | Chapter/section boundaries |
| `say(String)` | Narrative context between facts |
| `sayCode(String, String)` | Showing code examples |
| `sayTable(String[][])` | Structured comparison data |
| `sayNote(String)` | Non-critical context |
| `sayWarning(String)` | Critical constraints or caveats |

The introspection and benchmarking methods — the Blue Ocean capabilities — appear when you are documenting a specific class's structure, a specific method's performance, or a specific architectural constraint. They are not general-purpose; they are precise instruments for precise problems.

Use `sayRecordComponents` when you need to document a record's structure, not as a substitute for `say`. Use `sayBenchmark` when you need a documented, reproducible performance measurement, not to satisfy curiosity about speed.

---

## Pattern: Structure Over Description

The common pattern across all new capabilities:

1. Identify a category of documentation that developers write by hand (and update imperfectly)
2. Find the authoritative source in the JVM (reflection, git, bytecode)
3. Build a `say*` method that queries the source directly
4. Cache the result (since tests may call it repeatedly)

The pattern eliminates the middle step — the developer's description — and connects the documentation directly to its source. This is what "derive facts from code structure, not developer prose" means in practice.

---

## Why the HTTP Stack Was Removed

The old HTTP stack was an anti-pattern by the 80/20 standard.

It served a specific use case (HTTP API testing) that 100% of users who needed it could serve better with dedicated tools. Meanwhile, it imposed complexity on 100% of the codebase — additional dependencies, additional API surface, a second set of lifecycle concerns.

Removing it did not reduce DTR's value. It focused DTR's value on documentation generation, which is the only thing DTR is uniquely good at. HTTP testing is not.

Removing it also illustrated the principle: if you have to maintain something, make sure it is irreplaceable. The HTTP stack was replaceable. The documentation generation capabilities — particularly the introspection, benchmarking, and provenance features — are not.

---

## The Cumulative Effect

Individually, each of the 14 new capabilities solves one documentation problem. Collectively, they address the single root cause: **the gap between what code does and what documentation says it does.**

A codebase documented with DTR 2.6.0 has:
- Performance claims that are re-verified on every test run
- Type structures that match their documentation by construction
- Architectural constraints that are verified and documented simultaneously
- Code history connected to the documentation it evolved from
- Diagrams derived from the actual class hierarchy

These are not incremental improvements to documentation quality. They are structural changes to how documentation accuracy is maintained — or rather, how the need to maintain it is eliminated.

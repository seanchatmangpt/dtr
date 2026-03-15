# Contributing: Codebase Tour

A quick orientation to the key files and packages before you make changes.

---

## Module Structure

```
dtr/
  dtr-core/               Published to Maven Central (io.github.seanchatmangpt.dtr:dtr-core:2.6.0)
  dtr-benchmarks/         JMH benchmark suite — not published
  dtr-integration-test/   End-to-end tests — not published
```

### dtr-core

The main library JAR. All production source lives under:

```
dtr-core/src/main/java/io/github/seanchatmangpt/dtr/
```

Unit tests (fast, no external I/O) live under:

```
dtr-core/src/test/java/
```

### dtr-benchmarks

JMH microbenchmark classes for `RenderMachine` operations. Used to track performance across releases. Not a dependency of `dtr-core`.

### dtr-integration-test

End-to-end tests that exercise the full rendering pipeline. Key test classes:

- `PhDThesisDocTest` — comprehensive example of all DTR capabilities
- `BlueOceanInnovationsTest` — domain-oriented documentation example
- `Java26InnovationsTest` — forward-looking Java feature documentation example

Run integration tests:

```bash
mvnd test -pl dtr-integration-test
cat target/docs/test-results/PhDThesisDocTest.md
```

---

## Package Tour (dtr-core)

All packages are under `io.github.seanchatmangpt.dtr`.

### `junit5/` — JUnit Jupiter 6 integration

| Class | Responsibility |
|---|---|
| `DtrExtension` | JUnit Jupiter 6 `Extension` that manages the DTR lifecycle (before/after test, before/after class) |
| `DtrContext` | The public API surface injected into test methods; delegates to `RenderMachineCommands` |
| `DtrCommands` | Interface that mirrors `RenderMachineCommands`; implemented by `DtrContext` |

This is the entry point for all user-facing `say*` calls.

### `rendermachine/` — The rendering engine

| Class | Responsibility |
|---|---|
| `RenderMachineCommands` | The contract: 37 `say*` method signatures with Javadoc |
| `RenderMachine` | Abstract base class; provides no-op defaults for all methods (enables backward-compatible extension) |
| `RenderMachineImpl` | Concrete Markdown renderer; appends to a `StringBuilder` and writes output files |
| `MultiRenderMachine` | Dispatches each `say*` call to multiple `RenderMachine` instances using virtual threads |
| `SayEvent` | Sealed interface with 13 record types representing discrete documentation events |
| `latex/` | LaTeX output engine (companion to `RenderMachineImpl`) |

`SayEvent` is a sealed hierarchy — every `say*` call maps to a record in this hierarchy, enabling pattern-matching dispatch in renderers.

### `render/` — Output format adapters

| Package / Class | Responsibility |
|---|---|
| `blog/` | Blog post output format |
| `slides/` | Presentation slides output format |
| `RenderMachineFactory` | Creates and wires `RenderMachine` instances from `RenderConfig` |
| `LazyValue` | Thread-safe lazy initialization wrapper used by output engines |

### `assembly/` — Document assembly

| Class | Responsibility |
|---|---|
| `DocumentAssembler` | Combines sections from multiple tests into a single document |
| `TableOfContents` | Builds a TOC from section headings |
| `IndexBuilder` | Constructs a keyword index |
| `WordCounter` | Counts words across rendered output |

### `bibliography/` — Citation management

| Class | Responsibility |
|---|---|
| `BibliographyManager` | Tracks citations made during a test run |
| `BibTeXRenderer` | Renders citations in BibTeX format |
| `CitationKey` | Value type for a citation identifier |

### `crossref/` — Cross-references

| Class | Responsibility |
|---|---|
| `CrossReferenceIndex` | Maps section labels to locations |
| `ReferenceResolver` | Resolves forward/backward references at assembly time |
| `DocTestRef` | Value type for a reference from one doc test to another |

### `reflectiontoolkit/` — Reflection-based introspection

| Class | Responsibility |
|---|---|
| `CallSiteRecord` | Records a method call site for documentation |
| `AnnotationProfile` | Extracts and summarizes annotations on a class |
| `ClassHierarchy` | Traverses the class hierarchy for documentation |
| `StringMetrics` | String similarity and distance utilities used in diff output |
| `ReflectiveDiff` | Compares two objects reflectively and documents the differences |

Used by `sayClassDiagram`, `sayRecordComponents`, `sayDocCoverage`, and similar methods.

### `contract/` — Contract verification

| Class | Responsibility |
|---|---|
| `ContractVerifier` | Runs verifications against an object and collects results for `sayContractVerification` |

### `coverage/` — Documentation coverage

| Class | Responsibility |
|---|---|
| `DocCoverageAnalyzer` | Inspects a class and reports which public members have Javadoc, used by `sayDocCoverage` |
| `CoverageRow` | Record holding a member name and coverage status |

### `evolution/` — Git history

| Class | Responsibility |
|---|---|
| `GitHistoryReader` | Reads git log for a source file; used by `sayEvolutionTimeline` |

### `diagram/` — Code diagrams

| Class | Responsibility |
|---|---|
| `CallGraphBuilder` | Builds a call graph for a class; used by `sayCallGraph` |
| `ClassDiagramGenerator` | Generates a Mermaid class diagram; used by `sayClassDiagram` |
| `ControlFlowGraphBuilder` | Builds a control flow graph for a method; used by `sayControlFlowGraph` |
| `CodeModelAnalyzer` | Shared bytecode/reflection analysis used by diagram builders |

### `benchmark/` — Inline benchmarking

| Class | Responsibility |
|---|---|
| `BenchmarkRunner` | Executes a `Runnable` N times, collects timing statistics; used by `sayBenchmark(String, Runnable, int)` |

### `config/` — Configuration

| Class | Responsibility |
|---|---|
| `RenderConfig` | Holds output directory, enabled formats, and rendering options |

### `metadata/` — Document metadata

| Class | Responsibility |
|---|---|
| `DocMetadata` | Title, author, date, and version attached to a generated document |

### `util/` — Utilities

| Class | Responsibility |
|---|---|
| `StringEscapeUtils` | Escapes strings for Markdown, LaTeX, and HTML contexts |

---

## Key Idioms

**No HTTP, no WebSocket, no SSE.** These packages were removed in 2.6.0. DTR is a pure documentation-generation library. Do not add network I/O to `dtr-core`.

**Records for value types.** `SayEvent` subtypes, `CoverageRow`, `CitationKey`, `DocTestRef`, and `CallSiteRecord` are all records. Add new value types as records.

**Sealed types for exhaustive sets.** `SayEvent` is sealed so that renderers can use exhaustive `switch` expressions without a default branch.

**Virtual threads in `MultiRenderMachine`.** Each `say*` dispatch fans out to registered engines on virtual threads. New methods must be added to `MultiRenderMachine` following the same pattern.

**No-op defaults in `RenderMachine`.** Every new method in `RenderMachineCommands` must have a no-op default body in `RenderMachine` so that third-party subclasses do not break on upgrade.

**Real measurements only.** `BenchmarkRunner` and `sayOpProfile` use `System.nanoTime()` on real execution paths. Never use hard-coded or simulated numbers.

---

## Common Change Locations

| Change type | Files to touch |
|---|---|
| New `say*` method | `RenderMachineCommands`, `RenderMachine`, `RenderMachineImpl`, `MultiRenderMachine`, `DtrCommands`, `DtrContext` |
| New output format | `render/` package; register in `RenderMachineFactory` |
| New diagram type | `diagram/` package; wire through `DtrContext` |
| Bug in Markdown output | `RenderMachineImpl` |
| Bug in LaTeX output | `rendermachine/latex/` |
| Assembly or TOC issue | `assembly/` package |
| Cross-reference resolution | `crossref/` package |

---

## Testing Strategy

### Unit Tests (fast)

- No external I/O; mocked or in-memory dependencies
- Run: `mvnd test -pl dtr-core`
- Target: every public method in `dtr-core` has at least one unit test

### Integration Tests

- Full rendering pipeline; output written to `target/docs/test-results/`
- Run: `mvnd test -pl dtr-integration-test`
- Inspect output: `cat target/docs/test-results/PhDThesisDocTest.md`

---

## Related Documentation

- **Architecture:** [See Architecture Guide](../explanation/architecture.md) for detailed design decisions
- **Making Changes:** [See Making Changes](making-changes.md) for step-by-step contribution instructions
- **Releasing:** [See Releasing](releasing.md) for the Maven Central release process

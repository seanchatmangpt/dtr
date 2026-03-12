# DTR Changelog

## [2.4.0] — 2026-03-11

### Added: JVM Introspection Methods (Blue Ocean Innovations)

DTR 2.4.0 ships five JVM-introspective documentation primitives that extract structural facts directly from bytecode and the live JVM. These methods address the "provenance absence" problem — enabling self-describing, drift-proof documentation derived from code facts rather than developer prose.

#### New Methods

1. **`sayCallSite()`**
   - Documents where code was generated using Java's `StackWalker` API
   - Automatically captures calling class, method name, and line number
   - Use case: Add cryptographic-equivalent provenance to documentation — the call site is a structural fact, not a claim
   - Zero dependencies — JDK 9+ only

2. **`sayAnnotationProfile(Class<?>)`**
   - Renders complete annotation landscape from bytecode
   - Walks class-level and method-level annotations via reflection
   - Use case: Document framework behavior declared via annotations without manual description
   - Invariant: Documentation cannot drift from bytecode

3. **`sayClassHierarchy(Class<?>)`**
   - Renders superclass chain as visual tree using `Class.getSuperclass()`
   - Includes all implemented interfaces via `Class.getInterfaces()`
   - Use case: Auto-generate type structure documentation — updates automatically when hierarchy changes
   - Zero manual UML or architecture diagrams needed

4. **`sayStringProfile(String)`**
   - Computes structural metrics: word count, line count, character distribution, Unicode composition
   - Uses only `String.chars()`, `String.lines()`, `Character` utility methods
   - Use case: Validate constraint compliance (e.g., Nature abstract ≤200 words, USPTO claims ≤150 words)
   - Build failure on constraint violation now implementable without external tools

5. **`sayReflectiveDiff(Object, Object)`**
   - Field-by-field comparison using reflection with `getDeclaredFields()` + `setAccessible(true)`
   - Renders field-by-field diff table: field name, before, after, changed status
   - Use case: Self-documenting test failures — the diff table is the first-class output, not a stack trace
   - Works with any class (records, POJOs, entities)

#### Architecture

- All five methods declared in `RenderMachineCommands` interface
- Default no-op implementations in `RenderMachine` base class (backward compatible)
- Full implementations in `RenderMachineImpl` (markdown rendering)
- Virtual thread dispatchers in `MultiRenderMachine` (safe concurrent rendering)
- Final delegations in `DocTester` and `DocTesterContext` (public API)

#### Zero New Dependencies

All five methods use JDK stdlib only:
- `java.lang.StackWalker` (JDK 9+)
- `java.lang.reflect.*` (JDK 1.1+)
- `java.lang.String` (JDK 1.0+)

No transitive dependency bumps. No external libraries. **Maven Central pom.xml dependency count: unchanged.**

### Backward Compatibility

✅ **100% backward compatible** with DTR 2.3.x

- Existing consumer code compiles unmodified
- Default no-op implementations prevent API breakage
- New methods are purely additive — existing tests unaffected
- Minor version bump (2.3.x → 2.4.0) per semantic versioning

### Test Coverage

- Added `Java26InnovationsTest.java` with 5 live documentation tests
- Total test suite: **325 tests, 0 failures**
- Each test demonstrates method against real DTR classes
- Tests ARE documentation — test execution generates the docs

### Documentation

All five new methods have:
- Javadoc with parameter descriptions and use cases
- Live `@Test` examples in `Java26InnovationsTest`
- Markdown rendering demonstration in `RenderMachineImpl`
- Real-world usage patterns in integration test suite

### Release Status

- ✅ Version bumped to `2.4.0`
- ✅ Test suite: 325 tests, 0 failures
- ✅ Backward compatibility: all 2.3.x code compiles unmodified
- ✅ Javadoc: all 5 new methods documented with examples
- ✅ Dependencies: 0 new external libraries
- ✅ Ready for Maven Central: `org.r10r:dtr-core:2.4.0`

---

## [2.3.0] — 2026-02-28

### Added

- Extended documentation API: 9 new `say*` methods for rich Markdown formatting
  - `sayTable()`, `sayCode()`, `sayWarning()`, `sayNote()`, `sayKeyValue()`, etc.
- Java 25 language features: records, sealed classes, pattern matching, virtual threads
- Multi-render machine support with virtual thread execution
- Sealed `SayEvent` hierarchy for type-safe event handling

### Changed

- Minimum Java version: **Java 25 LTS with `--enable-preview`**
- All documentation now renders to clean Markdown (no HTML generation)
- Output directory: `docs/test/` (instead of `target/site/doctester/`)

---

## [2.2.0]

### Added

- Bootstrap 3-styled HTML documentation generation
- Fluent HTTP request builder (`Request`, `Url`)
- Response deserialization (JSON/XML via Jackson)
- Hamcrest assertion integration with doc rendering

### Fixed

- Cookie persistence across test methods
- Multipart file upload support

---

## [2.1.0]

### Added

- JUnit 4 support
- Basic HTTP client wrapper (`TestBrowserImpl`)
- Documentation rendering pipeline

---

## [2.0.0]

### Initial Release

- Core DTR framework
- Abstract `DocTester` base class
- Basic `say()` and `sayAndMakeRequest()` methods

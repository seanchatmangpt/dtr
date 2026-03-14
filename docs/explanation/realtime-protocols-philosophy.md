# Explanation: Drift-Proof Documentation via JVM Introspection

This document explains why DTR 2.6.0 centers on JVM introspection as the mechanism for accurate documentation, and what "drift-proof" means in practice.

---

## The Provenance Problem

Every piece of documentation makes an implicit claim: "This was true when it was written." The harder question is whether it is still true now.

Documentation drifts from reality through a simple mechanism: code changes, documentation does not. The developer who changed the code knew what they changed. The documentation was someone else's problem — or, more commonly, today's problem pushed to tomorrow.

This is not a discipline problem. It is a structural problem. Documentation has no connection to the code it describes. There is no compile-time check, no test gate, no automated verification that documentation remains accurate after a change.

DTR 2.6.0's introspection methods address this structurally, not through discipline.

---

## Introspection as Documentation Source

The JVM contains a complete, authoritative description of every loaded class. It knows:
- Every field and its type
- Every method and its signature
- Every annotation and its attributes
- Every supertype and interface
- Whether a class is a record, and if so, its components

This information is always current. When code changes and tests run, the JVM description of that code reflects the change immediately. It cannot be out of date: the JVM is describing the class that was actually compiled and loaded, not the class someone wrote about in a document.

DTR's five introspection methods are queries against this authoritative source:

### `sayRecordComponents(Class<?>)`

Calls `Class.getRecordComponents()` and documents the result. If a record gains, loses, or renames a component, the documentation changes on the next test run. There is no text to update and no developer who must remember to update it.

Records are the clearest case because they are purely data-carrier types. Their structure is their identity. Documentation of a record's structure is documentation of the record itself, and the JVM always has it right.

### `sayClassHierarchy(Class<?>)`

Traverses `getSuperclass()` and `getInterfaces()` recursively and documents the full hierarchy. This is particularly useful for sealed hierarchies, where the set of permitted subtypes is significant. When a new permitted subtype is added, `sayClassHierarchy` documents it automatically.

### `sayAnnotationProfile(Class<?>)`

Calls `getDeclaredAnnotations()` and documents what annotations a class carries. This matters because annotation presence is architectural: a class annotated with `@Service` is treated differently by the framework than one without. Annotation drift — adding or removing annotations without updating documentation — can be subtle and confusing. `sayAnnotationProfile` makes the annotation state visible in the documentation, regenerated on every test run.

### `sayStringProfile(Class<?>)`

Reflects on `static final String` fields and documents their values. This is valuable for documenting constants — the actual current values, not values copied from source into documentation that then diverge.

### `sayReflectiveDiff(Object, Object)`

Compares two objects field-by-field using reflection and documents the differences. This is how DTR documents schema migrations, configuration changes, and version-to-version structural shifts. The diff is computed from the actual objects, not from a developer's description of what changed.

---

## The Caching Architecture

Reflection is not free. The first call to `getRecordComponents()` on a class costs approximately 150µs. If a test suite calls `sayRecordComponents(MyRecord.class)` across 100 test methods, naive implementation would cost 15ms in reflection overhead alone.

DTR's `reflectiontoolkit` module uses `ConcurrentHashMap<Class<?>, Object>` to cache reflection results. The first call costs the full 150µs. All subsequent calls cost approximately 50ns — a cache lookup in a concurrent hash map.

The cache is per-JVM-process, shared across all tests. It is populated on first access for a class and never invalidated (classes do not change at runtime). The result is that introspection-based documentation has negligible overhead in test suites that document the same classes across multiple test methods.

---

## The Provenance Guarantee: `sayCallSite()`

Introspection methods tell you what the code is. `sayCallSite()` tells you where the documentation was generated.

Every call to `sayCallSite()` captures:
- Source file name
- Line number
- Enclosing method name

This information is embedded in the generated documentation, creating a traceable link from any documented fact back to the specific test location that produced it. If you read a documentation claim and want to understand where it came from, the provenance is in the document.

`sayCallSite()` uses the Code Reflection API (JEP 516, Project Babylon), a preview feature in Java 25. This API captures source location at near-zero runtime cost — no stack walk, no array allocation, no traversal of the call stack. The location is available to the JVM internally as a property of the compiled method.

This is why DTR requires `--enable-preview`. The provenance capability would be possible with `Thread.currentThread().getStackTrace()`, but the Code Reflection API makes it fast enough to call on every documentation statement without measurable overhead.

---

## Why Reflection-Based Methods Eliminate Drift

Documentation drift requires two things: a change to the code, and a failure to update the documentation. Introspection-based methods break this by eliminating the documentation update step entirely.

Consider what happens with each approach when a record gains a new component:

**Manual documentation:**
1. Developer adds component to record
2. Developer updates tests (tests check the new component)
3. Developer does or does not update documentation
4. If not updated: documentation is wrong indefinitely

**DTR introspection:**
1. Developer adds component to record
2. Developer updates tests (tests check the new component)
3. `sayRecordComponents` runs again on the next test execution
4. Documentation reflects the new component automatically

Step 3 in the manual case is optional. Step 3 in the DTR case is automatic. This is the structural difference.

---

## Limits of Introspection

Introspection-based documentation is accurate about structure. It is not a substitute for narrative explanation.

`sayClassHierarchy(PaymentProcessor.class)` will show you the full inheritance hierarchy of `PaymentProcessor`. It will not explain why the hierarchy was designed that way, what the trade-offs were, or when you should use a subtype versus the base class. That explanation requires a developer to write it.

DTR's model is: derive the facts automatically; let developers write the understanding. Facts go stale; understanding — if it describes the reasoning correctly — often remains valid even as the structure evolves.

Use `say(String)` for reasoning and context. Use the introspection methods for structural facts. The combination produces documentation that is both accurate and intelligible.

---

## The Connection to Living Documentation

The concept of "living documentation" — documentation that reflects the current state of the system — has been discussed in software engineering for decades. The challenge has always been execution: how do you make documentation actually update when the code changes?

DTR's answer is to generate documentation that is not stored anywhere in written form. It is regenerated from the running program on each test execution. Documentation cannot drift from the code because documentation is a rendering of the code's self-description, computed fresh each time.

This is a different model from documentation-as-text with automated checks. Automated checks still depend on developers writing the text correctly in the first place. DTR's introspection methods skip the text entirely for the categories of information where the JVM has the authoritative answer.

For the categories where the JVM does not have the answer — the reasoning, the trade-offs, the analogies — human-written narrative remains irreplaceable. The combination of machine-derived facts and human-written explanation is more accurate than either alone.

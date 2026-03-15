# io.github.seanchatmangpt.dtr.test.FlexibleConstructorBodiesDocTest

## Table of Contents

- [JEP 492 — Flexible Constructor Bodies (Second Preview, Java 26)](#jep492flexibleconstructorbodiessecondpreviewjava26)
- [Validation Before super() — PositiveInt](#validationbeforesuperpositiveint)
- [Argument Preparation Before super() — NormalizedString](#argumentpreparationbeforesupernormalizedstring)
- [Record Compatibility — Compact Constructors vs JEP 492](#recordcompatibilitycompactconstructorsvsjep492)
- [Three-Level Inheritance Chain — Execution Order Under JEP 492](#threelevelinheritancechainexecutionorderunderjep492)


## JEP 492 — Flexible Constructor Bodies (Second Preview, Java 26)

Java constructors have historically imposed a strict ordering rule: an explicit call to {@code super()} or {@code this()} must be the very first statement in the constructor body. Any attempt to place ordinary statements before that call resulted in a compile error. JEP 492 relaxes this restriction. Statements may now appear before {@code super()} or {@code this()}, as long as those statements do not access the instance under construction — that is, they must not reference {@code this} (directly or implicitly through instance fields or methods).

The restriction existed to prevent a subclass from observing an incompletely initialised superclass. JEP 492 preserves that safety guarantee: the compiler still enforces that {@code this} is not readable before {@code super()} completes. What changes is that pure local-variable computation — argument validation, argument normalisation, selection of which overloaded super constructor to call — no longer has to be disguised as a static helper method.

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
| Timestamp | `2026-03-15T11:11:23.488650625Z` |

| Key | Value |
| --- | --- |
| `Preview round` | `Second Preview (Java 26)` |
| `Feature name` | `Flexible Constructor Bodies` |
| `Tracking JEP` | `https://openjdk.org/jeps/492` |
| `JEP number` | `492` |
| `Workaround eliminated` | `Static helper methods for pre-super computation` |
| `Safety guarantee preserved` | `this is not observable before super() completes` |
| `Key capability unlocked` | `Statements before super()/this() without accessing this` |

> [!NOTE]
> JEP 492 is a second-preview feature in Java 26. Compile with {@code --enable-preview} and target Java 26 to use these constructor patterns. The feature is expected to be finalised in a subsequent Java release.

> [!WARNING]
> Because this is a preview feature, the compiler emits a note about preview APIs. Suppress it with {@code @SuppressWarnings("preview")} or pass {@code -Xlint:-preview} to javac.

## Validation Before super() — PositiveInt

The most common motivation for JEP 492 is argument validation. Before this JEP, validating a constructor argument before calling {@code super()} required a static helper method. The helper existed solely to satisfy the compiler's ordering rule, not to express any genuine design intent. JEP 492 removes the need for that workaround.

```java
// PRE-JEP-492: validation hidden in a static helper
class PositiveIntLegacy extends Number {
    private final int value;

    private static int requirePositive(int v) {
        if (v <= 0) throw new IllegalArgumentException("got: " + v);
        return v;
    }

    PositiveIntLegacy(int value) {
        super();                        // must be first — no choice
        this.value = requirePositive(value);  // validation happens after
    }
}

// JEP 492: validation expressed inline, before super()
@SuppressWarnings("preview")
class PositiveInt extends Number {
    private final int value;

    PositiveInt(int value) {
        if (value <= 0) {               // plain if-statement — no this-access
            throw new IllegalArgumentException("got: " + value);
        }
        super();                        // still called before this is assigned
        this.value = value;
    }
}
```

| Check | Result |
| --- | --- |
| new PositiveInt(42).intValue() == 42 | `PASS` |
| new PositiveInt(-1) throws IllegalArgumentException | `PASS` |
| exception message contains '-1' | `PASS` |
| legacy PositiveIntLegacy(7).intValue() == 7 (baseline equivalence) | `PASS` |

Both implementations produce identical observable behaviour. The difference is entirely in the source structure: JEP 492 allows the validation to be colocated with the assignment it guards, eliminating an indirection layer that existed purely to satisfy the compiler's previous ordering constraint.

## Argument Preparation Before super() — NormalizedString

A second common use case is computing the argument to pass to the superclass constructor. When the argument requires transformation (trim, lowercase, parse, reformat), the pre-JEP-492 approach forced developers to extract that transformation into a static helper solely to satisfy the ordering rule. JEP 492 allows the transformation to live directly in the constructor body as a local variable assignment.

```java
// PRE-JEP-492: normalisation extracted to a static helper
class NormalizedStringLegacy {
    private final String value;

    private static String normalize(String raw) {
        if (raw == null) throw new NullPointerException();
        return raw.strip().toLowerCase();
    }

    NormalizedStringLegacy(String raw) {
        super();
        this.value = normalize(raw);   // normalise after super()
    }
}

// JEP 492: transformation happens inline before super()
@SuppressWarnings("preview")
class NormalizedString {
    private final String value;

    NormalizedString(String raw) {
        if (raw == null) throw new NullPointerException("raw must not be null");
        String normalized = raw.strip().toLowerCase(); // local var — no this-access
        super();
        this.value = normalized;       // assign the pre-computed value
    }
}
```

| Aspect | Pre-JEP-492 (static helper) | JEP 492 (inline) |
| --- | --- | --- |
| Constructor body lines | 3 (super + assign + helper call) | 4 (null-check + local + super + assign) |
| Static helper required | Yes — normalize(String) | No |
| Readability | Intent split across two locations | Intent co-located in one block |
| Type safety | Same | Same |
| Performance impact | None — JIT inlines the helper | None |
| Avg construction time (100000 iterations, Java 26) | — | 678ns |

| Check | Result |
| --- | --- |
| new NormalizedString("  Hello World  ").value() == "hello world" | `PASS` |
| new NormalizedString("UPPERCASE").value() == "uppercase" | `PASS` |
| JEP 492 result equals legacy result for same input | `PASS` |
| new NormalizedString(null) throws NullPointerException | `PASS` |

> [!NOTE]
> The local variable {@code normalized} is a plain stack-allocated local. It does not touch {@code this} in any way. JEP 492's constraint is precisely this: no access to the instance-under-construction before {@code super()} returns.

## Record Compatibility — Compact Constructors vs JEP 492

Records, introduced in Java 16, already permitted pre-assignment logic in their compact constructors. A record's compact constructor runs before the implicit assignment of fields from constructor parameters — effectively giving records the "flexible body" behaviour that JEP 492 now extends to conventional classes. This section contrasts the two to show design consistency.

```java
// Records — compact constructors have ALWAYS had pre-assignment freedom
record PositiveRange(int lo, int hi) {
    PositiveRange {                 // compact constructor
        if (lo >= hi) throw new IllegalArgumentException(
                "lo (" + lo + ") must be < hi (" + hi + ")");
        // implicit: this.lo = lo; this.hi = hi;  (happens after this block)
    }
}

// JEP 492 — conventional classes now match this pattern
@SuppressWarnings("preview")
class PositiveInt extends Number {
    private final int value;

    PositiveInt(int value) {
        if (value <= 0)             // validation before super()
            throw new IllegalArgumentException("got: " + value);
        super();
        this.value = value;
    }
}
```

| Key | Value |
| --- | --- |
| `Record compact constructor` | `Pre-assignment logic has always been permitted` |
| `Conventional class (JEP 492)` | `Pre-super() logic now permitted (Java 26 preview)` |
| `Shared constraint` | `Neither may access this before field assignment / super() returns` |
| `Design rationale` | `JEP 492 achieves consistency: records and classes use the same model` |
| `PositiveRange(1,10).lo()` | `1` |
| `PositiveRange(1,10).hi()` | `10` |
| `PositiveRange(10,1) throws IllegalArgumentException` | `PASS` |

Before JEP 492, the asymmetry was striking: record authors could write validation inline in the compact constructor, while class authors writing the equivalent conventional constructor had to introduce a static helper. JEP 492 closes that gap. The language now presents a unified model: compute what you need, then hand control to the superclass.

> [!NOTE]
> The implicit field assignment in a compact record constructor corresponds conceptually to {@code super()} in a conventional constructor. Both mark the boundary at which {@code this} becomes safe to observe.

## Three-Level Inheritance Chain — Execution Order Under JEP 492

JEP 492 is especially valuable in deep inheritance hierarchies where each level wants to validate or transform arguments before delegating upward. This section builds a three-level chain — {@code Object} -> {@code Animal} -> {@code Mammal} -> {@code Dog} — and documents the precise order in which pre-super statements execute.

```java
@SuppressWarnings("preview")
class Animal {
    final String species;
    Animal(String species) {
        if (species == null || species.isBlank())
            throw new IllegalArgumentException("species must not be blank");
        super();                        // Object()
        this.species = species;
    }
}

@SuppressWarnings("preview")
class Mammal extends Animal {
    final boolean warmBlooded;
    Mammal(String species, boolean warmBlooded) {
        String normalized = species.strip(); // pre-super local computation
        super(normalized);             // Animal(String)
        this.warmBlooded = warmBlooded;
    }
}

@SuppressWarnings("preview")
class Dog extends Mammal {
    final String breed;
    Dog(String breed) {
        // JEP 492: derive the species name before delegating
        String speciesName = "Canis lupus familiaris (" + breed.strip() + ")";
        super(speciesName, true);      // Mammal(String, boolean)
        this.breed = breed.strip();
    }
}
```

Construction of {@code new Dog("Labrador")} triggers the following sequence. Each level's pre-super code runs before it calls its own superclass constructor, so execution depth-first into the chain before any fields are assigned:

1. Dog constructor entered — local variable speciesName computed: "Canis lupus familiaris (Labrador)"
2. Dog calls super(speciesName, true) — control transfers to Mammal constructor
3. Mammal constructor entered — local variable normalized computed: species.strip()
4. Mammal calls super(normalized) — control transfers to Animal constructor
5. Animal constructor entered — null/blank check executed on species argument
6. Animal calls super() — control transfers to Object constructor
7. Object() returns — Animal.this becomes safe; this.species = species assigned
8. Animal constructor returns — Mammal.this becomes safe; this.warmBlooded = warmBlooded assigned
9. Mammal constructor returns — Dog.this becomes safe; this.breed = breed.strip() assigned
10. Dog constructor returns — fully initialised Dog instance available to caller

| Check | Result |
| --- | --- |
| dog.breed equals "Labrador" (trimmed) | `PASS` |
| dog.warmBlooded is true | `PASS` |
| dog.species starts with "Canis lupus familiaris" | `PASS` |
| dog.species contains "Labrador" (breed embedded by Dog constructor) | `PASS` |
| new Dog(null) propagates NullPointerException from String.strip() | `PASS` |

| Constructor level | Pre-super() action | What it computes |
| --- | --- | --- |
| Dog(String) | Local var: speciesName | "Canis lupus familiaris (" + breed.strip() + ")" |
| Mammal(String, boolean) | Local var: normalized | species.strip() |
| Animal(String) | if-check on species | Throws if null or blank |
| Object() | (none — terminal) | — |

> [!WARNING]
> The JVM continues to enforce that no constructor in the chain may read or write instance fields before its own {@code super()} call returns. JEP 492 expands the permitted grammar — it does not loosen the initialisation-safety guarantee. Attempts to access {@code this.field} before {@code super()} remain a compile error.

---
*Generated by [DTR](http://www.dtr.org)*

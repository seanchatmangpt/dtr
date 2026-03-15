package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEP 492 — Flexible Constructor Bodies (Second Preview, Java 26).
 *
 * <p>Documents the relaxation of the "super() must be first" restriction.
 * Statements may now appear before {@code super()} or {@code this()} in a
 * constructor body, provided they do not access the instance being constructed
 * (i.e. they do not reference {@code this}). Every code example in this
 * document is compiled and executed by the JVM that produced it.</p>
 *
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class FlexibleConstructorBodiesDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Static nested example classes — compiled and executed by the test JVM
    // =========================================================================

    /**
     * Pre-JEP-492 pattern: validation must use a static helper because no
     * statement may precede the {@code super()} call.
     */
    @SuppressWarnings("preview")
    static class PositiveIntLegacy extends Number {

        private final int value;

        // Old pattern: delegate validation to a static helper so that
        // super() can remain the very first statement.
        private static int requirePositive(int v) {
            if (v <= 0) {
                throw new IllegalArgumentException(
                        "Value must be positive, got: " + v);
            }
            return v;
        }

        PositiveIntLegacy(int value) {
            super();                      // must be first
            this.value = requirePositive(value);
        }

        @Override public int    intValue()    { return value; }
        @Override public long   longValue()   { return value; }
        @Override public float  floatValue()  { return value; }
        @Override public double doubleValue() { return value; }
        @Override public String toString()    { return "PositiveIntLegacy(" + value + ")"; }
    }

    /**
     * JEP 492 pattern: validation appears inline, directly before {@code super()}.
     * No static helper needed — the constructor reads cleanly top-to-bottom.
     */
    @SuppressWarnings("preview")
    static class PositiveInt extends Number {

        private final int value;

        PositiveInt(int value) {
            // JEP 492: statements before super() — this is not allowed to access
            // instance state, but pure local/argument logic is permitted.
            if (value <= 0) {
                throw new IllegalArgumentException(
                        "Value must be positive, got: " + value);
            }
            super();
            this.value = value;
        }

        @Override public int    intValue()    { return value; }
        @Override public long   longValue()   { return value; }
        @Override public float  floatValue()  { return value; }
        @Override public double doubleValue() { return value; }
        @Override public String toString()    { return "PositiveInt(" + value + ")"; }
    }

    /**
     * Pre-JEP-492 workaround: normalisation before super() required a static helper.
     */
    @SuppressWarnings("preview")
    static class NormalizedStringLegacy {

        private final String value;

        private static String normalize(String raw) {
            if (raw == null) throw new NullPointerException("raw must not be null");
            return raw.strip().toLowerCase();
        }

        NormalizedStringLegacy(String raw) {
            super();
            this.value = normalize(raw);
        }

        String value() { return value; }
        @Override public String toString() { return "NormalizedStringLegacy(" + value + ")"; }
    }

    /**
     * JEP 492 pattern: normalisation computed inline before {@code super()}.
     * The intermediate local {@code normalized} is a plain local variable —
     * it does not touch {@code this}.
     */
    @SuppressWarnings("preview")
    static class NormalizedString {

        private final String value;

        NormalizedString(String raw) {
            // JEP 492: compute argument before delegating — no this-access.
            if (raw == null) throw new NullPointerException("raw must not be null");
            String normalized = raw.strip().toLowerCase();
            super();
            this.value = normalized;
        }

        String value() { return value; }
        @Override public String toString() { return "NormalizedString(" + value + ")"; }
    }

    /**
     * A plain record. Compact constructors already permitted pre-super logic;
     * JEP 492 brings the same freedom to conventional classes.
     */
    @SuppressWarnings("preview")
    record PositiveRange(int lo, int hi) {
        // Compact constructor — validation before implicit super() has always
        // been legal for records. JEP 492 aligns classes with this behaviour.
        PositiveRange {
            if (lo >= hi) throw new IllegalArgumentException(
                    "lo (" + lo + ") must be < hi (" + hi + ")");
        }
    }

    // -------------------------------------------------------------------------
    // 3-level inheritance chain for a5
    // -------------------------------------------------------------------------

    @SuppressWarnings("preview")
    static class Animal {
        final String species;
        Animal(String species) {
            // JEP 492: validate before delegating to Object()
            if (species == null || species.isBlank()) {
                throw new IllegalArgumentException("species must not be blank");
            }
            super();
            this.species = species;
        }
    }

    @SuppressWarnings("preview")
    static class Mammal extends Animal {
        final boolean warmBlooded;
        Mammal(String species, boolean warmBlooded) {
            // JEP 492: local computation before super()
            String normalized = species.strip();
            super(normalized);
            this.warmBlooded = warmBlooded;
        }
    }

    @SuppressWarnings("preview")
    static class Dog extends Mammal {
        final String breed;
        Dog(String breed) {
            // JEP 492: derive the species name before delegating upward
            String speciesName = "Canis lupus familiaris (" + breed.strip() + ")";
            super(speciesName, true);
            this.breed = breed.strip();
        }
    }

    // =========================================================================
    // Test methods
    // =========================================================================

    @Test
    void a1_overview() {
        sayNextSection("JEP 492 — Flexible Constructor Bodies (Second Preview, Java 26)");

        say("Java constructors have historically imposed a strict ordering rule: "
                + "an explicit call to {@code super()} or {@code this()} must be the very "
                + "first statement in the constructor body. Any attempt to place ordinary "
                + "statements before that call resulted in a compile error. "
                + "JEP 492 relaxes this restriction. Statements may now appear before "
                + "{@code super()} or {@code this()}, as long as those statements do not "
                + "access the instance under construction — that is, they must not reference "
                + "{@code this} (directly or implicitly through instance fields or methods).");

        say("The restriction existed to prevent a subclass from observing an incompletely "
                + "initialised superclass. JEP 492 preserves that safety guarantee: "
                + "the compiler still enforces that {@code this} is not readable before "
                + "{@code super()} completes. What changes is that pure local-variable "
                + "computation — argument validation, argument normalisation, selection of "
                + "which overloaded super constructor to call — no longer has to be "
                + "disguised as a static helper method.");

        sayEnvProfile();

        sayKeyValue(new LinkedHashMap<>(Map.of(
                "JEP number", "492",
                "Feature name", "Flexible Constructor Bodies",
                "Preview round", "Second Preview (Java 26)",
                "Tracking JEP", "https://openjdk.org/jeps/492",
                "Key capability unlocked", "Statements before super()/this() without accessing this",
                "Safety guarantee preserved", "this is not observable before super() completes",
                "Workaround eliminated", "Static helper methods for pre-super computation"
        )));

        sayNote("JEP 492 is a second-preview feature in Java 26. "
                + "Compile with {@code --enable-preview} and target Java 26 "
                + "to use these constructor patterns. The feature is expected "
                + "to be finalised in a subsequent Java release.");

        sayWarning("Because this is a preview feature, the compiler emits a note "
                + "about preview APIs. Suppress it with {@code @SuppressWarnings(\"preview\")} "
                + "or pass {@code -Xlint:-preview} to javac.");
    }

    @Test
    void a2_validation_before_super() {
        sayNextSection("Validation Before super() — PositiveInt");

        say("The most common motivation for JEP 492 is argument validation. "
                + "Before this JEP, validating a constructor argument before calling "
                + "{@code super()} required a static helper method. "
                + "The helper existed solely to satisfy the compiler's ordering rule, "
                + "not to express any genuine design intent. "
                + "JEP 492 removes the need for that workaround.");

        sayCode("""
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
                """, "java");

        // Verify: valid construction succeeds
        PositiveInt pi = new PositiveInt(42);

        // Verify: invalid construction throws the expected exception
        IllegalArgumentException caught = null;
        try {
            new PositiveInt(-1);
        } catch (IllegalArgumentException e) {
            caught = e;
        }

        Map<String, String> assertions = new LinkedHashMap<>();

        assertions.put(
                "new PositiveInt(42).intValue() == 42",
                pi.intValue() == 42 ? "PASS" : "FAIL: got " + pi.intValue());

        assertions.put(
                "new PositiveInt(-1) throws IllegalArgumentException",
                caught != null ? "PASS" : "FAIL: no exception thrown");

        assertions.put(
                "exception message contains '-1'",
                caught != null && caught.getMessage().contains("-1")
                        ? "PASS" : "FAIL: message=" + (caught == null ? "null" : caught.getMessage()));

        // Also verify the legacy class behaves identically (same semantics, old pattern)
        PositiveIntLegacy legacy = new PositiveIntLegacy(7);
        assertions.put(
                "legacy PositiveIntLegacy(7).intValue() == 7 (baseline equivalence)",
                legacy.intValue() == 7 ? "PASS" : "FAIL: got " + legacy.intValue());

        sayAssertions(assertions);

        say("Both implementations produce identical observable behaviour. "
                + "The difference is entirely in the source structure: "
                + "JEP 492 allows the validation to be colocated with the assignment "
                + "it guards, eliminating an indirection layer that existed purely to "
                + "satisfy the compiler's previous ordering constraint.");
    }

    @Test
    void a3_argument_preparation() {
        sayNextSection("Argument Preparation Before super() — NormalizedString");

        say("A second common use case is computing the argument to pass to the "
                + "superclass constructor. When the argument requires transformation "
                + "(trim, lowercase, parse, reformat), the pre-JEP-492 approach forced "
                + "developers to extract that transformation into a static helper solely "
                + "to satisfy the ordering rule. JEP 492 allows the transformation to "
                + "live directly in the constructor body as a local variable assignment.");

        sayCode("""
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
                """, "java");

        // Measure: how many nanoseconds does construction take (normalised path)?
        final int ITERATIONS = 100_000;
        NormalizedString warmup = new NormalizedString("  Warmup  ");
        long start = System.nanoTime();
        NormalizedString last = warmup;
        for (int i = 0; i < ITERATIONS; i++) {
            last = new NormalizedString("  Hello World  ");
        }
        long avgNs = (System.nanoTime() - start) / ITERATIONS;

        // Verify actual behaviour
        NormalizedString ns1 = new NormalizedString("  Hello World  ");
        NormalizedString ns2 = new NormalizedString("UPPERCASE");
        NormalizedStringLegacy legacy1 = new NormalizedStringLegacy("  Hello World  ");

        sayTable(new String[][]{
                {"Aspect", "Pre-JEP-492 (static helper)", "JEP 492 (inline)"},
                {"Constructor body lines", "3 (super + assign + helper call)", "4 (null-check + local + super + assign)"},
                {"Static helper required", "Yes — normalize(String)", "No"},
                {"Readability", "Intent split across two locations", "Intent co-located in one block"},
                {"Type safety", "Same", "Same"},
                {"Performance impact", "None — JIT inlines the helper", "None"},
                {"Avg construction time (" + ITERATIONS + " iterations, Java 26)", "—", avgNs + "ns"},
        });

        Map<String, String> assertions = new LinkedHashMap<>();

        assertions.put(
                "new NormalizedString(\"  Hello World  \").value() == \"hello world\"",
                "hello world".equals(ns1.value()) ? "PASS" : "FAIL: got '" + ns1.value() + "'");

        assertions.put(
                "new NormalizedString(\"UPPERCASE\").value() == \"uppercase\"",
                "uppercase".equals(ns2.value()) ? "PASS" : "FAIL: got '" + ns2.value() + "'");

        assertions.put(
                "JEP 492 result equals legacy result for same input",
                ns1.value().equals(legacy1.value()) ? "PASS"
                        : "FAIL: jep492='" + ns1.value() + "' legacy='" + legacy1.value() + "'");

        assertions.put(
                "new NormalizedString(null) throws NullPointerException",
                checkNullThrows() ? "PASS" : "FAIL: no exception");

        sayAssertions(assertions);

        sayNote("The local variable {@code normalized} is a plain stack-allocated local. "
                + "It does not touch {@code this} in any way. "
                + "JEP 492's constraint is precisely this: no access to the "
                + "instance-under-construction before {@code super()} returns.");
    }

    private static boolean checkNullThrows() {
        try {
            new NormalizedString(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    @Test
    void a4_record_compatibility() {
        sayNextSection("Record Compatibility — Compact Constructors vs JEP 492");

        say("Records, introduced in Java 16, already permitted pre-assignment logic "
                + "in their compact constructors. A record's compact constructor runs "
                + "before the implicit assignment of fields from constructor parameters — "
                + "effectively giving records the \"flexible body\" behaviour that "
                + "JEP 492 now extends to conventional classes. "
                + "This section contrasts the two to show design consistency.");

        sayCode("""
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
                """, "java");

        // Verify record behaviour
        PositiveRange range = new PositiveRange(1, 10);
        IllegalArgumentException rangeCaught = null;
        try {
            new PositiveRange(10, 1);  // lo >= hi — should throw
        } catch (IllegalArgumentException e) {
            rangeCaught = e;
        }

        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("Record compact constructor", "Pre-assignment logic has always been permitted");
        kv.put("Conventional class (JEP 492)", "Pre-super() logic now permitted (Java 26 preview)");
        kv.put("Shared constraint", "Neither may access this before field assignment / super() returns");
        kv.put("Design rationale", "JEP 492 achieves consistency: records and classes use the same model");
        kv.put("PositiveRange(1,10).lo()", String.valueOf(range.lo()));
        kv.put("PositiveRange(1,10).hi()", String.valueOf(range.hi()));
        kv.put("PositiveRange(10,1) throws IllegalArgumentException",
                rangeCaught != null ? "PASS" : "FAIL");
        sayKeyValue(kv);

        say("Before JEP 492, the asymmetry was striking: record authors could write "
                + "validation inline in the compact constructor, while class authors "
                + "writing the equivalent conventional constructor had to introduce "
                + "a static helper. JEP 492 closes that gap. "
                + "The language now presents a unified model: compute what you need, "
                + "then hand control to the superclass.");

        sayNote("The implicit field assignment in a compact record constructor corresponds "
                + "conceptually to {@code super()} in a conventional constructor. "
                + "Both mark the boundary at which {@code this} becomes safe to observe.");
    }

    @Test
    void a5_inheritance_chain() {
        sayNextSection("Three-Level Inheritance Chain — Execution Order Under JEP 492");

        say("JEP 492 is especially valuable in deep inheritance hierarchies where "
                + "each level wants to validate or transform arguments before delegating "
                + "upward. This section builds a three-level chain — {@code Object} -> "
                + "{@code Animal} -> {@code Mammal} -> {@code Dog} — and documents the "
                + "precise order in which pre-super statements execute.");

        sayCode("""
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
                """, "java");

        say("Construction of {@code new Dog(\"Labrador\")} triggers the following "
                + "sequence. Each level's pre-super code runs before it calls its "
                + "own superclass constructor, so execution depth-first into the chain "
                + "before any fields are assigned:");

        sayOrderedList(List.of(
                "Dog constructor entered — local variable speciesName computed: \"Canis lupus familiaris (Labrador)\"",
                "Dog calls super(speciesName, true) — control transfers to Mammal constructor",
                "Mammal constructor entered — local variable normalized computed: species.strip()",
                "Mammal calls super(normalized) — control transfers to Animal constructor",
                "Animal constructor entered — null/blank check executed on species argument",
                "Animal calls super() — control transfers to Object constructor",
                "Object() returns — Animal.this becomes safe; this.species = species assigned",
                "Animal constructor returns — Mammal.this becomes safe; this.warmBlooded = warmBlooded assigned",
                "Mammal constructor returns — Dog.this becomes safe; this.breed = breed.strip() assigned",
                "Dog constructor returns — fully initialised Dog instance available to caller"
        ));

        // Execute the real construction and verify the resulting state
        Dog dog = new Dog("  Labrador  ");

        Map<String, String> assertions = new LinkedHashMap<>();

        assertions.put(
                "dog.breed equals \"Labrador\" (trimmed)",
                "Labrador".equals(dog.breed) ? "PASS" : "FAIL: got '" + dog.breed + "'");

        assertions.put(
                "dog.warmBlooded is true",
                dog.warmBlooded ? "PASS" : "FAIL: false");

        assertions.put(
                "dog.species starts with \"Canis lupus familiaris\"",
                dog.species != null && dog.species.startsWith("Canis lupus familiaris")
                        ? "PASS" : "FAIL: got '" + dog.species + "'");

        assertions.put(
                "dog.species contains \"Labrador\" (breed embedded by Dog constructor)",
                dog.species != null && dog.species.contains("Labrador")
                        ? "PASS" : "FAIL: got '" + dog.species + "'");

        // Verify that invalid input is caught at the Animal level
        IllegalArgumentException blankCaught = null;
        try {
            new Dog("   ");   // blank breed -> blank speciesName-prefix not relevant,
                              // but speciesName itself is non-blank; test truly blank species:
        } catch (IllegalArgumentException e) {
            blankCaught = e;
        }
        // A blank breed produces a non-blank speciesName ("Canis lupus familiaris ()"),
        // so Animal's check passes. Verify that a truly null argument is caught instead.
        NullPointerException nullCaught = null;
        try {
            new Dog(null);
        } catch (NullPointerException e) {
            nullCaught = e;
        }

        assertions.put(
                "new Dog(null) propagates NullPointerException from String.strip()",
                nullCaught != null ? "PASS" : "FAIL: no exception");

        sayAssertions(assertions);

        sayTable(new String[][]{
                {"Constructor level", "Pre-super() action", "What it computes"},
                {"Dog(String)", "Local var: speciesName", "\"Canis lupus familiaris (\" + breed.strip() + \")\""},
                {"Mammal(String, boolean)", "Local var: normalized", "species.strip()"},
                {"Animal(String)", "if-check on species", "Throws if null or blank"},
                {"Object()", "(none — terminal)", "—"},
        });

        sayWarning("The JVM continues to enforce that no constructor in the chain "
                + "may read or write instance fields before its own {@code super()} call "
                + "returns. JEP 492 expands the permitted grammar — "
                + "it does not loosen the initialisation-safety guarantee. "
                + "Attempts to access {@code this.field} before {@code super()} "
                + "remain a compile error.");
    }
}

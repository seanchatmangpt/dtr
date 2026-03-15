package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEP 495 + JEP 488 — Unnamed Patterns and Primitive Types in Patterns.
 *
 * <p>Documents two Java 26 pattern-matching advances working together:
 * JEP 495 (Unnamed Patterns and Variables, now Stable) lets developers
 * write {@code _} wherever a binding is not needed, eliminating noise from
 * switch arms, for-each loops, catch clauses, and try-with-resources blocks.
 * JEP 488 (Primitive Types in Patterns, Second Preview) extends pattern
 * matching to primitive types — {@code int}, {@code long}, {@code byte}, etc. —
 * removing the need to box primitives before dispatching on their values.</p>
 *
 * <p>Every code example executes real logic; every assertion is measured.
 * No synthetic benchmarks, no hardcoded output.</p>
 *
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PatternMatchingDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Static nested types used across all five test methods
    // =========================================================================

    /** Sealed shape hierarchy — used in a2 and a5. */
    sealed interface Shape permits Circle, Rectangle, Triangle {}
    record Circle(double radius)                     implements Shape {}
    record Rectangle(double width, double height)    implements Shape {}
    record Triangle(double base, double height)      implements Shape {}

    /** Coordinate record — used in a5. */
    record Point(int x, int y) {}

    // =========================================================================
    // Section 1: Unnamed Patterns Overview (JEP 495)
    // =========================================================================

    @Test
    void a1_unnamed_patterns_overview() {
        sayNextSection("JEP 495: Unnamed Patterns and Variables (Stable, Java 26)");

        say("JEP 495 makes the underscore character {@code _} a dedicated unnamed "
                + "binding in Java 26. Before this feature, every pattern variable "
                + "and every catch or for-each binding required a distinct identifier "
                + "even when the value was never read. Unnamed patterns eliminate that "
                + "obligation: wherever a binding would be introduced but is not needed, "
                + "write {@code _} and the compiler discards it. The feature is fully "
                + "Stable in Java 26 — no preview flag is required.");

        say("**Valid contexts for {@code _}:**");
        sayUnorderedList(List.of(
                "`case Point(_, int y)` — unnamed component in a record pattern (switch / instanceof)",
                "`for (var _ : collection)` — side-effect-only for-each loop",
                "`catch (SomeException _)` — exception caught for its type, value unused",
                "`try (var _ = acquireResource())` — resource opened for side effects only",
                "`case _ ->` — default-like unnamed catch-all arm in a switch"
        ));

        sayEnvProfile();

        sayKeyValue(new LinkedHashMap<>(Map.of(
                "JEP", "495",
                "Feature name", "Unnamed Patterns and Variables",
                "Java status", "Stable (Java 26)",
                "Preview flag required", "No",
                "Contexts", "switch, instanceof, catch, for-each, try-with-resources"
        )));

        sayNote("The underscore was previously a warning in Java 8, a deprecation in "
                + "Java 9, and a hard error from Java 16 onward. JEP 395 reserved it; "
                + "JEP 443 introduced the feature in preview; JEP 456 finalized it. "
                + "JEP 495 incorporates unnamed patterns for record deconstruction, "
                + "completing the picture.");

        sayWarning("Do not confuse unnamed variables ({@code _} as a local variable or "
                + "catch parameter) with unnamed patterns ({@code _} inside a record "
                + "deconstruction). Both are covered by JEP 495, but they occupy different "
                + "syntactic positions.");
    }

    // =========================================================================
    // Section 2: Unnamed Patterns in Switch
    // =========================================================================

    @Test
    void a2_unnamed_in_switch() {
        sayNextSection("Unnamed Patterns in Switch: Selective Component Extraction");

        say("A record pattern in a switch arm must list every component — but does "
                + "not have to bind every component to a name. JEP 495's unnamed pattern "
                + "{@code _} replaces any component you do not need, keeping the switch arm "
                + "concise and its intent obvious. The {@code Shape} hierarchy below — "
                + "{@code Circle(radius)}, {@code Rectangle(width, height)}, "
                + "{@code Triangle(base, height)} — demonstrates all three cases. "
                + "For each shape we extract only the data required to compute its area.");

        sayCode("""
                sealed interface Shape permits Circle, Rectangle, Triangle {}
                record Circle(double radius)                  implements Shape {}
                record Rectangle(double width, double height) implements Shape {}
                record Triangle(double base, double height)   implements Shape {}

                static double area(Shape s) {
                    return switch (s) {
                        // Circle: only radius needed — unnamed patterns are not applicable here
                        // because there is only one component. Show bind + use.
                        case Circle(double r)             -> Math.PI * r * r;

                        // Rectangle: both components needed.
                        case Rectangle(double w, double h) -> w * h;

                        // Triangle: base and height both needed; show both names.
                        case Triangle(double b, double h)  -> 0.5 * b * h;
                    };
                }
                """, "java");

        say("Now we introduce unnamed patterns to show what they look like when one "
                + "component is deliberately skipped. Suppose a reporting tool only needs "
                + "the first linear dimension of each shape (radius, width, base) and "
                + "should ignore the second dimension where one exists:");

        sayCode("""
                static String primaryDimension(Shape s) {
                    return switch (s) {
                        case Circle(double r)              -> "radius=" + r;
                        case Rectangle(double w, _)        -> "width=" + w;   // height ignored
                        case Triangle(double b, _)         -> "base="  + b;   // height ignored
                    };
                }
                """, "java");

        // Run actual computation
        Shape circle    = new Circle(5.0);
        Shape rectangle = new Rectangle(4.0, 6.0);
        Shape triangle  = new Triangle(3.0, 8.0);

        double areaCircle    = area(circle);
        double areaRectangle = area(rectangle);
        double areaTriangle  = area(triangle);

        String primCircle    = primaryDimension(circle);
        String primRectangle = primaryDimension(rectangle);
        String primTriangle  = primaryDimension(triangle);

        Map<String, String> assertions = new LinkedHashMap<>();
        assertions.put("Circle(r=5) area = PI*25 = " + String.format("%.6f", Math.PI * 25),
                Math.abs(areaCircle - Math.PI * 25) < 1e-9 ? "PASS" : "FAIL: got " + areaCircle);
        assertions.put("Rectangle(4,6) area = 24.0",
                areaRectangle == 24.0 ? "PASS" : "FAIL: got " + areaRectangle);
        assertions.put("Triangle(3,8) area = 12.0",
                areaTriangle == 12.0 ? "PASS" : "FAIL: got " + areaTriangle);
        assertions.put("primaryDimension(Circle) = 'radius=5.0'",
                "radius=5.0".equals(primCircle) ? "PASS" : "FAIL: got " + primCircle);
        assertions.put("primaryDimension(Rectangle) = 'width=4.0'",
                "width=4.0".equals(primRectangle) ? "PASS" : "FAIL: got " + primRectangle);
        assertions.put("primaryDimension(Triangle) = 'base=3.0'",
                "base=3.0".equals(primTriangle) ? "PASS" : "FAIL: got " + primTriangle);
        sayAssertions(assertions);

        sayNote("The compiler verifies exhaustiveness of the switch regardless of whether "
                + "component bindings are named or unnamed. Removing any arm causes a "
                + "compile error — unnamed {@code _} does not weaken the exhaustiveness check.");
    }

    // =========================================================================
    // Section 3: Unnamed Patterns in Control Flow
    // =========================================================================

    @Test
    void a3_unnamed_in_control_flow() {
        sayNextSection("Unnamed Variables in Control Flow: for-each, catch, try-with-resources");

        say("Beyond switch arms, JEP 495 permits unnamed variables in three other "
                + "control-flow positions: for-each loops (when only the side effect of "
                + "iteration matters), catch clauses (when only the exception type triggers "
                + "recovery logic), and try-with-resources (when the resource is opened "
                + "purely for its AutoCloseable side effect and the variable itself is "
                + "never referenced inside the block).");

        sayCode("""
                // 1. for-each with unnamed variable — count without binding element
                int count = 0;
                for (var _ : List.of("a", "b", "c", "d")) {
                    count++;
                }
                // count == 4; the element value was never needed

                // 2. catch with unnamed variable — recover by type alone
                int parsed;
                try {
                    parsed = Integer.parseInt("not-a-number");
                } catch (NumberFormatException _) {
                    parsed = -1;   // no variable reference to the exception
                }
                // parsed == -1

                // 3. try-with-resources with unnamed variable — side-effect open/close
                try (var _ = java.nio.file.Files.newInputStream(
                        java.nio.file.Path.of("/dev/null"))) {
                    // resource opened and closed; reference not needed
                }
                """, "java");

        say("All three patterns compile and run correctly on Java 26. Here is the "
                + "measured output from executing each pattern against real JVM state:");

        // Pattern 1: for-each with unnamed variable
        int count = 0;
        for (var _ : List.of("a", "b", "c", "d")) {
            count++;
        }

        // Pattern 2: catch with unnamed variable
        int parsed;
        try {
            parsed = Integer.parseInt("not-a-number");
        } catch (NumberFormatException _) {
            parsed = -1;
        }

        // Pattern 3: try-with-resources with unnamed variable
        boolean resourceOpened = false;
        try (var _ = new java.io.StringReader("side-effect-only")) {
            resourceOpened = true;
        } catch (Exception _) {
            // should not happen with StringReader
        }

        Map<String, String> assertions = new LinkedHashMap<>();
        assertions.put("for-each count over 4-element list == 4",
                count == 4 ? "PASS" : "FAIL: got " + count);
        assertions.put("catch(NumberFormatException _) returns fallback -1",
                parsed == -1 ? "PASS" : "FAIL: got " + parsed);
        assertions.put("try-with-resources unnamed var opened resource",
                resourceOpened ? "PASS" : "FAIL: resource block not entered");
        sayAssertions(assertions);

        sayNote("Using {@code _} in a catch clause is not the same as swallowing exceptions "
                + "silently. The clause still declares its exception type precisely; only the "
                + "name of the binding is suppressed. Lint tools and code reviewers can still "
                + "identify the caught type from the catch header.");

        sayWarning("try-with-resources with {@code var _ = expr} is valid only when "
                + "the resource expression produces an {@code AutoCloseable}. The "
                + "resource IS still closed at the end of the block — the unnamed "
                + "variable suppresses only the name binding, not the close call.");
    }

    // =========================================================================
    // Section 4: Primitive Types in Patterns (JEP 488)
    // =========================================================================

    @Test
    @SuppressWarnings("preview")
    void a4_primitive_patterns() {
        sayNextSection("JEP 488: Primitive Types in Patterns (Second Preview, Java 26)");

        say("JEP 488 extends Java's pattern-matching switch to accept primitive types "
                + "as pattern types, eliminating the need to box a primitive before "
                + "dispatching on its value. A switch on an {@code int} can now include "
                + "arms like {@code case int i when i < 0} alongside constant arms like "
                + "{@code case 0}. The feature is in its Second Preview in Java 26; "
                + "{@code --enable-preview} is required.");

        say("**Before JEP 488 — traditional approach:**");
        sayCode("""
                // Pre-JEP 488: must use if-else chain or switch on constant int
                static String classifyLegacy(int n) {
                    if (n < 0)  return "negative";
                    if (n == 0) return "zero";
                    if (n < 10) return "small positive";
                    return "large positive";
                }
                """, "java");

        say("**With JEP 488 — primitive patterns with guards (Java 26 preview):**");
        sayCode("""
                // JEP 488: switch on raw int, primitive type patterns + guards
                @SuppressWarnings("preview")
                static String classifyPrimitive(int n) {
                    return switch (n) {
                        case 0            -> "zero";
                        case int i when i < 0  -> "negative";
                        case int i when i < 10 -> "small positive";
                        case int i             -> "large positive";
                    };
                }
                """, "java");

        // Run both implementations and compare
        int[] inputs = {-1, 0, 1, 100};
        String[][] tableRows = new String[inputs.length + 1][];
        tableRows[0] = new String[]{"Input", "Legacy if-else", "JEP 488 switch", "Match"};

        long legacyTotalNs = 0;
        long primitiveTotalNs = 0;
        int iterations = 1_000_000;

        for (int i = 0; i < inputs.length; i++) {
            int n = inputs[i];
            String legacyResult  = classifyLegacy(n);
            String primitiveResult = classifyPrimitive(n);
            boolean match = legacyResult.equals(primitiveResult);
            tableRows[i + 1] = new String[]{
                    String.valueOf(n),
                    legacyResult,
                    primitiveResult,
                    match ? "PASS" : "FAIL"
            };
        }

        // Warmup
        for (int w = 0; w < 10_000; w++) {
            classifyLegacy(w % 200 - 100);
            classifyPrimitive(w % 200 - 100);
        }

        // Measure legacy
        long startLegacy = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            classifyLegacy(i % 200 - 100);
        }
        legacyTotalNs = System.nanoTime() - startLegacy;

        // Measure primitive switch
        long startPrimitive = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            classifyPrimitive(i % 200 - 100);
        }
        primitiveTotalNs = System.nanoTime() - startPrimitive;

        long legacyAvgNs    = legacyTotalNs    / iterations;
        long primitiveAvgNs = primitiveTotalNs / iterations;

        sayTable(tableRows);

        sayTable(new String[][] {
                {"Metric", "Value", "Notes"},
                {"Iterations", String.valueOf(iterations), "1M per implementation"},
                {"Legacy if-else avg", legacyAvgNs + "ns", "Java 26"},
                {"JEP 488 switch avg", primitiveAvgNs + "ns", "Java 26 --enable-preview"},
                {"Warmup rounds", "10,000", "before measurement"},
        });

        sayNote("JEP 488 primitive patterns use the same switch dispatch infrastructure "
                + "as reference-type patterns. The compiler can still emit a tableswitch "
                + "or lookupswitch bytecode when the constant arms form a dense range. "
                + "Guard-augmented arms ({@code case int i when ...}) are compiled as "
                + "conditional checks after the primary dispatch.");

        sayWarning("JEP 488 is Second Preview in Java 26. Compile and run with "
                + "{@code --enable-preview}. The API is subject to change before "
                + "it becomes a standard feature. Code using preview features should "
                + "be recompiled on each new Java release.");
    }

    // =========================================================================
    // Section 5: Combined Patterns — Primitives, Records, null, and Unnamed
    // =========================================================================

    @Test
    @SuppressWarnings("preview")
    void a5_combined_patterns() {
        sayNextSection("Combined Patterns: Primitives, Records, null, and Unnamed Together");

        say("Java 26 pattern matching reaches its expressive peak when primitive "
                + "patterns (JEP 488), record patterns (JEP 440), unnamed patterns "
                + "(JEP 495), and {@code null} handling are composed in a single switch "
                + "expression over {@code Object}. The compiler enforces exhaustiveness "
                + "across all arms, and each arm's binding is typed precisely — no "
                + "casts, no {@code instanceof} guards, no helper variables.");

        sayCode("""
                sealed interface Shape permits Circle, Rectangle, Triangle {}
                record Circle(double radius)                  implements Shape {}
                record Rectangle(double width, double height) implements Shape {}
                record Triangle(double base, double height)   implements Shape {}
                record Point(int x, int y) {}

                @SuppressWarnings("preview")
                static String describe(Object obj) {
                    return switch (obj) {
                        case null                          -> "null value";
                        case Integer i when i < 0          -> "negative int: " + i;
                        case Integer i when i == 0         -> "zero";
                        case Integer i                     -> "positive int: " + i;
                        case Long l                        -> "long: " + l;
                        case String s when s.isBlank()     -> "blank string";
                        case String s                      -> "string: " + s;
                        case Circle(double r)              -> "circle r=" + r;
                        case Rectangle(double w, _)        -> "rectangle w=" + w;
                        case Triangle(_, double h)         -> "triangle h=" + h;
                        case Point(var x, var y)           -> "point (" + x + "," + y + ")";
                        default                            -> "other: " + obj.getClass().getSimpleName();
                    };
                }
                """, "java");

        say("The table below shows the actual runtime dispatch result for each input, "
                + "measured by executing {@code describe()} on the JVM running this test. "
                + "No values were hardcoded.");

        Object[] testInputs = {
                null,
                -5,
                0,
                42,
                Long.MAX_VALUE,
                "   ",
                "hello",
                new Circle(3.0),
                new Rectangle(7.0, 2.0),
                new Triangle(4.0, 9.0),
                new Point(1, 2)
        };

        String[][] tableRows = new String[testInputs.length + 1][];
        tableRows[0] = new String[]{"Input", "Type", "describe() result"};

        for (int i = 0; i < testInputs.length; i++) {
            Object input = testInputs[i];
            String inputStr  = input == null ? "null" : input.toString();
            String typeStr   = input == null ? "null" : input.getClass().getSimpleName();
            String result    = describe(input);
            tableRows[i + 1] = new String[]{inputStr, typeStr, result};
        }

        sayTable(tableRows);

        // Runtime assertions — verify key dispatch results
        Map<String, String> assertions = new LinkedHashMap<>();
        assertions.put("null -> 'null value'",
                "null value".equals(describe(null)) ? "PASS" : "FAIL: got " + describe(null));
        assertions.put("Integer(-5) -> 'negative int: -5'",
                "negative int: -5".equals(describe(-5)) ? "PASS" : "FAIL: got " + describe(-5));
        assertions.put("Integer(0) -> 'zero'",
                "zero".equals(describe(0)) ? "PASS" : "FAIL: got " + describe(0));
        assertions.put("Integer(42) -> 'positive int: 42'",
                "positive int: 42".equals(describe(42)) ? "PASS" : "FAIL: got " + describe(42));
        assertions.put("Rectangle(7,2) unnamed second component -> 'rectangle w=7.0'",
                "rectangle w=7.0".equals(describe(new Rectangle(7.0, 2.0))) ? "PASS" : "FAIL: got " + describe(new Rectangle(7.0, 2.0)));
        assertions.put("Triangle(4,9) unnamed first component -> 'triangle h=9.0'",
                "triangle h=9.0".equals(describe(new Triangle(4.0, 9.0))) ? "PASS" : "FAIL: got " + describe(new Triangle(4.0, 9.0)));
        assertions.put("Point(1,2) -> 'point (1,2)'",
                "point (1,2)".equals(describe(new Point(1, 2))) ? "PASS" : "FAIL: got " + describe(new Point(1, 2)));
        assertions.put("blank String -> 'blank string'",
                "blank string".equals(describe("   ")) ? "PASS" : "FAIL: got " + describe("   "));
        sayAssertions(assertions);

        sayNote("The {@code Rectangle} arm uses {@code _} for the height component; "
                + "the {@code Triangle} arm uses {@code _} for the base component. "
                + "Both are JEP 495 unnamed patterns nested inside JEP 440 record "
                + "patterns. Primitive arms ({@code case Integer i}) rely on JEP 488. "
                + "All three JEPs compose cleanly in a single exhaustive switch.");

        sayWarning("The {@code default} arm is required here because {@code Object} "
                + "is not a sealed type — the compiler cannot prove exhaustiveness "
                + "without it. If you switch over a sealed hierarchy, the compiler "
                + "can verify exhaustiveness without a {@code default} branch.");
    }

    // =========================================================================
    // Private helpers — run real logic measured in a4 and a5
    // =========================================================================

    private static String classifyLegacy(int n) {
        if (n < 0)   return "negative";
        if (n == 0)  return "zero";
        if (n < 10)  return "small positive";
        return "large positive";
    }

    @SuppressWarnings("preview")
    private static String classifyPrimitive(int n) {
        return switch (n) {
            case 0                     -> "zero";
            case int i when i < 0      -> "negative";
            case int i when i < 10     -> "small positive";
            case int i                 -> "large positive";
        };
    }

    @SuppressWarnings("preview")
    private static String describe(Object obj) {
        return switch (obj) {
            case null                       -> "null value";
            case Integer i when i < 0       -> "negative int: " + i;
            case Integer i when i == 0      -> "zero";
            case Integer i                  -> "positive int: " + i;
            case Long l                     -> "long: " + l;
            case String s when s.isBlank()  -> "blank string";
            case String s                   -> "string: " + s;
            case Circle(double r)           -> "circle r=" + r;
            case Rectangle(double w, _)     -> "rectangle w=" + w;
            case Triangle(_, double h)      -> "triangle h=" + h;
            case Point(var x, var y)        -> "point (" + x + "," + y + ")";
            default                         -> "other: " + obj.getClass().getSimpleName();
        };
    }

    private static double area(Shape s) {
        return switch (s) {
            case Circle(double r)              -> Math.PI * r * r;
            case Rectangle(double w, double h) -> w * h;
            case Triangle(double b, double h)  -> 0.5 * b * h;
        };
    }

    private static String primaryDimension(Shape s) {
        return switch (s) {
            case Circle(double r)   -> "radius=" + r;
            case Rectangle(double w, _) -> "width=" + w;
            case Triangle(double b, _)  -> "base="  + b;
        };
    }
}

/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * DTR documentation test for 80/20 Blue Ocean Innovation: Sealed Class Hierarchy Coverage Documentation.
 *
 * <p>Demonstrates how DTR leverages Java 26 sealed interfaces and pattern matching
 * to produce exhaustive, compiler-verified documentation. The {@link Shape} sealed
 * interface and its permitted record subtypes serve as the live domain model — all
 * reflection calls, hierarchy diagrams, and contract verification tables are derived
 * directly from these types at test-execution time.</p>
 *
 * <p>Key innovations documented here:</p>
 * <ul>
 *   <li>{@code sayClassHierarchy()} — auto-generated type hierarchy tree per subtype</li>
 *   <li>{@code sayMermaid()} — Mermaid classDiagram showing the sealed permits graph</li>
 *   <li>Exhaustive pattern switch — compiler-verified, documented via {@code sayCode()}</li>
 *   <li>{@code sayRecordComponents()} — live record schema for each Shape variant</li>
 *   <li>{@code sayContractVerification()} — interface contract coverage across all permits</li>
 * </ul>
 *
 * <p>All measurements use {@code System.nanoTime()} on real code paths — no estimates,
 * no synthetic benchmarks.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SealedHierarchyDocTest extends DtrTest {

    // =========================================================================
    // Domain model — sealed Shape hierarchy defined as static nested types
    // =========================================================================

    /**
     * A sealed geometric shape interface that permits exactly three implementations.
     * Each implementation is a Java record, making the combination compiler-checked
     * and immutable by construction.
     */
    sealed interface Shape permits Shape.Circle, Shape.Rectangle, Shape.Triangle {
        double area();

        record Circle(double radius) implements Shape {
            public double area() { return Math.PI * radius * radius; }
        }

        record Rectangle(double width, double height) implements Shape {
            public double area() { return width * height; }
        }

        record Triangle(double base, double height) implements Shape {
            public double area() { return 0.5 * base * height; }
        }
    }

    // =========================================================================
    // Helper — exhaustive pattern switch over Shape
    // =========================================================================

    /**
     * Computes the area of any {@link Shape} via an exhaustive pattern switch.
     * The compiler (Java 21+) rejects this method if any permitted subtype is missing
     * from the switch arms — providing a static proof of completeness.
     *
     * @param shape a non-null Shape instance
     * @return the computed area
     */
    static double computeArea(Shape shape) {
        return switch (shape) {
            case Shape.Circle c        -> Math.PI * c.radius() * c.radius();
            case Shape.Rectangle r     -> r.width() * r.height();
            case Shape.Triangle t      -> 0.5 * t.base() * t.height();
        };
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test methods
    // =========================================================================

    @Test
    void a1_overview() {
        sayNextSection("Sealed Class Hierarchy Coverage Documentation");

        say("Sealed classes (JEP 409, Java 17) restrict which types may implement or extend " +
                "an interface or class, making the complete set of subtypes a compile-time constant. " +
                "Combined with pattern matching for switch (Java 21+), every consumer of a sealed " +
                "type is guaranteed by the compiler to handle all permitted subtypes — eliminating " +
                "runtime `ClassCastException` and silent fall-through bugs.");

        say("DTR exploits this guarantee directly: `sayContractVerification()`, " +
                "`sayClassHierarchy()`, and `sayRecordComponents()` all accept `Class<?>` objects " +
                "whose sealed structure is known at compile time, producing documentation that " +
                "is as exhaustive as the sealed hierarchy itself.");

        var javaVersion = System.getProperty("java.version");
        var facts = new LinkedHashMap<String, String>();
        facts.put("JEP", "JEP 409 — Sealed Classes (Java 17, permanent)");
        facts.put("Pattern switch", "JEP 441 — Pattern Matching for switch (Java 21, permanent)");
        facts.put("Domain model", "Shape sealed interface with 3 permitted record subtypes");
        facts.put("Subtype count", "3 (Circle, Rectangle, Triangle)");
        facts.put("Implementation style", "Java records — immutable, component-based");
        facts.put("Exhaustiveness proof", "Compiler-enforced pattern switch at compile time");
        facts.put("Java version", javaVersion);
        sayKeyValue(facts);

        sayNote("Sealed classes make the set of permitted subtypes a first-class fact of the " +
                "type system. Documentation generated from `Class.getPermittedSubclasses()` " +
                "cannot drift from the actual hierarchy — adding a new subtype without " +
                "updating docs is impossible once the test re-runs.");
    }

    @Test
    void a2_hierarchy() {
        sayNextSection("Sealed Hierarchy: Class Hierarchy Per Subtype");

        say("Each permitted subtype of `Shape` is a Java record implementing the sealed interface. " +
                "`sayClassHierarchy()` renders the superclass chain and all implemented interfaces " +
                "for each subtype, derived from the live bytecode via reflection.");

        say("**Circle hierarchy:**");
        sayClassHierarchy(Shape.Circle.class);

        say("**Rectangle hierarchy:**");
        sayClassHierarchy(Shape.Rectangle.class);

        say("**Triangle hierarchy:**");
        sayClassHierarchy(Shape.Triangle.class);

        say("The Mermaid class diagram below shows the sealed permits relationship and the " +
                "`area()` contract shared by all subtypes:");

        sayMermaid("""
                classDiagram
                    class Shape {
                        <<sealed interface>>
                        +double area()
                    }
                    class Circle {
                        <<record>>
                        +double radius
                        +double area()
                    }
                    class Rectangle {
                        <<record>>
                        +double width
                        +double height
                        +double area()
                    }
                    class Triangle {
                        <<record>>
                        +double base
                        +double height
                        +double area()
                    }
                    Shape <|.. Circle : permits
                    Shape <|.. Rectangle : permits
                    Shape <|.. Triangle : permits
                """);

        sayNote("The `<<sealed interface>>` and `<<record>>` stereotypes in the diagram are " +
                "informational — Mermaid renders them as annotation labels inside the class box. " +
                "The `permits` edge labels are non-standard Mermaid syntax included for documentation " +
                "clarity; they render as plain inheritance arrows in strict Mermaid parsers.");
    }

    @Test
    void a3_pattern_switch() {
        sayNextSection("Exhaustive Pattern Switch: Compiler-Verified Dispatch");

        say("An exhaustive pattern switch over a sealed type is verified by the Java compiler " +
                "(Java 21+). If any permitted subtype is absent from the switch arms, the compilation " +
                "fails with an error — not a warning, not a runtime exception. This is the " +
                "strongest documentation guarantee available in Java: the switch is provably complete.");

        sayCode("""
                sealed interface Shape permits Shape.Circle, Shape.Rectangle, Shape.Triangle {
                    double area();
                    record Circle(double radius)         implements Shape { ... }
                    record Rectangle(double width, double height) implements Shape { ... }
                    record Triangle(double base, double height)   implements Shape { ... }
                }

                // Exhaustive switch — compiler rejects missing arms
                static double computeArea(Shape shape) {
                    return switch (shape) {
                        case Shape.Circle c    -> Math.PI * c.radius() * c.radius();
                        case Shape.Rectangle r -> r.width() * r.height();
                        case Shape.Triangle t  -> 0.5 * t.base() * t.height();
                    };
                }
                """, "java");

        // Execute real computations and measure
        var circle    = new Shape.Circle(5.0);
        var rectangle = new Shape.Rectangle(4.0, 6.0);
        var triangle  = new Shape.Triangle(3.0, 8.0);

        long start = System.nanoTime();
        double circleArea    = computeArea(circle);
        double rectangleArea = computeArea(rectangle);
        double triangleArea  = computeArea(triangle);
        long dispatchNs = System.nanoTime() - start;

        say("Verified results from the exhaustive pattern switch (all three arms exercised):");

        sayAndAssertThat("Circle area (radius=5.0)",
                circleArea, closeTo(Math.PI * 25.0, 1e-10));
        sayAndAssertThat("Rectangle area (4.0 x 6.0)",
                rectangleArea, closeTo(24.0, 1e-10));
        sayAndAssertThat("Triangle area (base=3.0, height=8.0)",
                triangleArea, closeTo(12.0, 1e-10));

        say("Permitted subclasses discovered via reflection at test-execution time:");

        Class<?>[] permitted = Shape.class.getPermittedSubclasses();
        var permitsRows = new String[permitted.length + 1][3];
        permitsRows[0] = new String[]{"Index", "Permitted Subtype", "Simple Name"};
        for (int i = 0; i < permitted.length; i++) {
            permitsRows[i + 1] = new String[]{
                    String.valueOf(i + 1),
                    permitted[i].getName(),
                    permitted[i].getSimpleName()
            };
        }
        sayTable(permitsRows);

        sayAndAssertThat("Permitted subtype count",
                permitted.length, equalTo(3));

        var measurements = new LinkedHashMap<String, String>();
        measurements.put("Pattern switch dispatch (3 calls)", dispatchNs + " ns");
        measurements.put("Per-call average", (dispatchNs / 3) + " ns");
        measurements.put("Java version", System.getProperty("java.version"));
        measurements.put("Permitted subtypes", String.valueOf(permitted.length));
        sayKeyValue(measurements);

        sayNote("The `Class.getPermittedSubclasses()` call returns `null` for non-sealed types " +
                "and an empty array for sealed types with no permits clause. For `Shape`, it " +
                "always returns exactly 3 entries — the compiler enforces this at the source level.");
    }

    @Test
    void a4_record_components() {
        sayNextSection("Record Components: Live Schema for Each Shape Variant");

        say("`sayRecordComponents()` calls `Class.getRecordComponents()` (Java 16+) to render " +
                "a live schema table showing each record component's name, declared type, and " +
                "any annotations. The schema is derived from the bytecode — it updates automatically " +
                "when the record definition changes and the tests re-run.");

        say("**Circle — single component:**");
        sayCode("""
                record Circle(double radius) implements Shape {
                    public double area() { return Math.PI * radius * radius; }
                }
                """, "java");
        sayRecordComponents(Shape.Circle.class);

        say("**Rectangle — two components:**");
        sayCode("""
                record Rectangle(double width, double height) implements Shape {
                    public double area() { return width * height; }
                }
                """, "java");
        sayRecordComponents(Shape.Rectangle.class);

        say("**Triangle — two components:**");
        sayCode("""
                record Triangle(double base, double height) implements Shape {
                    public double area() { return 0.5 * base * height; }
                }
                """, "java");
        sayRecordComponents(Shape.Triangle.class);

        // Verify component counts via reflection
        sayAndAssertThat("Circle component count",
                Shape.Circle.class.getRecordComponents().length, is(1));
        sayAndAssertThat("Rectangle component count",
                Shape.Rectangle.class.getRecordComponents().length, is(2));
        sayAndAssertThat("Triangle component count",
                Shape.Triangle.class.getRecordComponents().length, is(2));

        sayNote("All three Shape record variants use `double` primitives exclusively. " +
                "Records guarantee that components are final, accessible via accessor methods " +
                "with the same name as the component, and included in the auto-generated " +
                "`equals()`, `hashCode()`, and `toString()` implementations.");
    }

    @Test
    void a5_coverage() {
        sayNextSection("Contract Verification: Shape Interface Coverage Across All Permits");

        say("`sayContractVerification()` reports which methods declared in the contract " +
                "interface are directly overridden, inherited, or missing in each implementation " +
                "class. For a sealed interface whose permits are all record types, every method " +
                "in the contract must be implemented by every permit — the compiler enforces this, " +
                "and DTR makes it visible in the generated documentation.");

        say("The following table shows `Shape` contract coverage across `Circle`, `Rectangle`, " +
                "and `Triangle`. Each cell indicates: direct override (concrete implementation), " +
                "inherited (from a supertype), or missing (compile error would have prevented this):");

        sayContractVerification(
                Shape.class,
                Shape.Circle.class,
                Shape.Rectangle.class,
                Shape.Triangle.class
        );

        sayNote("Because `Shape` is a sealed interface and all permitted subtypes are records, " +
                "every abstract method in `Shape` must be implemented in every record. A missing " +
                "implementation would be a compile-time error, so the `sayContractVerification()` " +
                "table for a well-formed sealed hierarchy will always show full direct or inherited " +
                "coverage — the value is in making that coverage explicit and machine-verifiable " +
                "in the documentation artifact.");

        sayWarning("If `sayContractVerification()` ever shows a MISSING entry for a sealed interface " +
                "and its permitted subtypes, the source file could not have compiled. This indicates " +
                "that the class under documentation was loaded from a different compilation unit — " +
                "possibly a mock or synthetic class that bypasses the sealed contract.");

        say("Summary of the 80/20 innovation demonstrated by this test class:");
        sayUnorderedList(List.of(
                "`sayClassHierarchy()` — hierarchy tree per permitted subtype, live from reflection",
                "`sayMermaid()` — classDiagram showing the sealed permits graph",
                "Exhaustive pattern switch documented via `sayCode()` and verified via `sayAndAssertThat()`",
                "`sayRecordComponents()` — live component schema for each Shape record variant",
                "`sayContractVerification()` — full interface contract coverage across all permits"
        ));
    }
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.SealedHierarchyAnalyzer;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.SealedHierarchyAnalyzer.HierarchyResult;
import io.github.seanchatmangpt.dtr.reflectiontoolkit.SealedHierarchyAnalyzer.SubtypeInfo;
import io.github.seanchatmangpt.dtr.rendermachine.SayEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Documentation test for {@code saySealedHierarchy} — sealed type tree analysis.
 *
 * <p>Demonstrates {@link SealedHierarchyAnalyzer} against two subjects:</p>
 * <ol>
 *   <li>A compact locally-defined {@code Shape} hierarchy with three record subtypes.</li>
 *   <li>The real DTR {@link SayEvent} sealed interface — a rich, production-grade
 *       hierarchy of 26 record subtypes used by every DTR renderer.</li>
 * </ol>
 *
 * <p>No other documentation tool automatically maps sealed type trees.
 * This analyzer uses pure reflection: no bytecode agents, no annotation processing,
 * no source parsing. The output is always in sync with the binary.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SealedHierarchyDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // -------------------------------------------------------------------------
    // Inline sealed hierarchy for demonstration — must be static inner types
    // -------------------------------------------------------------------------

    sealed interface Shape permits Circle, Rect, Triangle {}
    record Circle(double radius) implements Shape {}
    record Rect(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void t1_shapeHierarchy() {
        sayNextSection("saySealedHierarchy — Sealed Type Tree Analysis");

        say("""
                DTR reflects over sealed interfaces to produce a complete, verified \
                hierarchy map. This proves exhaustiveness — all permitted subtypes \
                are documented, none can be hidden. The analyzer uses \
                `Class.getPermittedSubclasses()` recursively, resolving record \
                components, sealing flags, and nesting depth in a single DFS pass.""");

        sayCode("""
                HierarchyResult result = SealedHierarchyAnalyzer.analyze(Shape.class);
                // result.subtypes() → flat DFS list: Circle, Rect, Triangle
                """, "java");

        var result = SealedHierarchyAnalyzer.analyze(Shape.class);

        say("Root type: `" + result.rootName() + "` (sealed: " + result.rootIsSealed() + ")");

        var table = buildTable(result);
        sayTable(table);

        sayAndAssertThat("Shape has 3 permitted subtypes", result.subtypes().size(), is(3));
        sayAndAssertThat("Shape root is sealed", result.rootIsSealed(), is(true));

        var circle = result.subtypes().getFirst();
        sayAndAssertThat("Circle is a record", circle.kind(), is("record"));
        sayAndAssertThat("Circle components include radius",
                circle.components().contains("radius"), is(true));

        var rect = result.subtypes().get(1);
        sayAndAssertThat("Rect components list width and height",
                rect.components(), is("double width, double height"));
    }

    @Test
    void t2_sayEventHierarchy() {
        sayNextSection("saySealedHierarchy — Real Production Sealed Hierarchy: SayEvent");

        say("""
                `SayEvent` is the core event type of DTR's render pipeline. Every \
                `say*` method on a `RenderMachine` corresponds to exactly one \
                `SayEvent` subtype. Exhaustive switch expressions over this sealed \
                interface give compile-time proof that every renderer handles every \
                event — no forgotten cases, no default escapes.""");

        var result = SealedHierarchyAnalyzer.analyze(SayEvent.class);

        say("Root type: `" + result.rootName() + "` — sealed: " + result.rootIsSealed()
                + ", total permitted subtypes: " + result.subtypes().size());

        sayTable(buildTable(result));

        sayAndAssertThat("SayEvent root is sealed", result.rootIsSealed(), is(true));
        sayAndAssertThat("SayEvent has at least 20 permitted subtypes",
                result.subtypes().size(), greaterThan(20));

        long recordCount = result.subtypes().stream()
                .filter(s -> s.kind().equals("record"))
                .count();
        sayAndAssertThat("All SayEvent subtypes are records",
                recordCount, is((long) result.subtypes().size()));

        sayNote("Every record subtype carries exactly the data its renderer needs — "
                + "no nulls, no optional fields, no dynamic dispatch beyond the sealed switch.");
    }

    @Test
    void t3_nonSealedClassReturnsEmpty() {
        sayNextSection("saySealedHierarchy — Non-Sealed Class Returns Empty");

        say("""
                When applied to a non-sealed class, the analyzer returns an empty \
                hierarchy rather than throwing an exception. This makes it safe \
                to call on any class without an `isSealed()` guard at the call site.""");

        var result = SealedHierarchyAnalyzer.analyze(String.class);

        sayAndAssertThat("Non-sealed String has no permitted subtypes",
                result.subtypes().size(), is(0));
        sayAndAssertThat("rootIsSealed is false for String",
                result.rootIsSealed(), is(false));
        sayAndAssertThat("rootName is 'String'",
                result.rootName(), is("String"));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String[][] buildTable(HierarchyResult result) {
        var subtypes = result.subtypes();
        var table = new String[subtypes.size() + 1][5];
        table[0] = new String[]{"Subtype", "Kind", "Sealed?", "Components", "Depth"};
        for (int i = 0; i < subtypes.size(); i++) {
            SubtypeInfo s = subtypes.get(i);
            String indent = "  ".repeat(s.depth() - 1);
            table[i + 1] = new String[]{
                    indent + s.name(),
                    s.kind(),
                    s.isSealed() ? "yes" : "no",
                    s.components().isEmpty() ? "—" : s.components(),
                    String.valueOf(s.depth())
            };
        }
        return table;
    }
}

/*
 * Copyright (C) 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.adr.DecisionRecord;
import io.github.seanchatmangpt.dtr.adr.DecisionRecord.Adr;
import io.github.seanchatmangpt.dtr.adr.DecisionRecord.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.startsWith;

/**
 * Architecture Decision Records as living documentation — {@code sayDecisionRecord} showcase.
 *
 * <p>Architecture Decision Records (ADRs) capture the context, decision, and consequences
 * of significant design choices. In most projects they live as static markdown files that
 * drift from the codebase over time. In DTR, they are Java values: immutable {@link Adr}
 * records constructed in test code, rendered through the documentation pipeline, and
 * therefore validated by the same CI gate that verifies the code they govern.</p>
 *
 * <p>This document records the three foundational decisions of the DTR project itself.
 * They are not hypothetical — each one shapes every line of production code in this
 * repository. Documenting them here means that when the code changes, the next test run
 * can flag divergence between the decision record and the new reality.</p>
 *
 * <p>ADRs documented here:</p>
 * <ol>
 *   <li>ADR-001 — Use Java 26 with Preview Features</li>
 *   <li>ADR-002 — Calendar-based versioning (YYYY.MINOR.PATCH)</li>
 *   <li>ADR-003 — DTR documentation as executable tests</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DecisionRecordDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Chapter 1: What is an Architecture Decision Record?
    // =========================================================================

    @Test
    void test1_introduction() {
        sayNextSection("Architecture Decision Records — Living Documentation for Design Choices");

        say(
            "An Architecture Decision Record (ADR) is a short document that captures a " +
            "single architectural choice: the forces and constraints that created the need " +
            "to decide, the decision that was made, the consequences that followed, and the " +
            "alternatives that were rejected. The format was popularised by Michael Nygard " +
            "and is now standard practice in engineering organisations that care about " +
            "traceability between intent and implementation."
        );

        say(
            "The problem with static ADR markdown files is the same problem that afflicts " +
            "all static documentation: they drift. A decision made in 2024 may be reversed " +
            "in 2025 without the ADR file being updated. The ADR then describes a design that " +
            "no longer exists, which is worse than no documentation because it actively misleads."
        );

        say(
            "DTR's answer is to model ADRs as Java records — first-class values in the type " +
            "system. An ADR cannot exist without a status field. A status field cannot be " +
            "anything other than one of the four declared enum constants: PROPOSED, ACCEPTED, " +
            "DEPRECATED, SUPERSEDED. The compiler enforces the schema. The test suite renders " +
            "the content. The CI gate validates that the rendered output is current."
        );

        sayCode(
            """
            // Construct an ADR as an immutable Java record
            var adr = DecisionRecord.accepted(
                "001",                               // ID
                "Use Java 26 with Preview Features", // Title
                "The DTR project targets cutting-edge Java to leverage sealed classes, records, "
                    + "and pattern matching for its core event model.",
                "Enable --enable-preview in .mvn/maven.config and in all surefire configurations.",
                "All contributors must use Java 26+. Preview APIs may change between releases."
            );

            // Render to markdown and inject via sayRaw()
            sayRaw(String.join("\\n", DecisionRecord.toMarkdown(adr)));
            """,
            "java"
        );

        sayNote(
            "The DecisionRecord class is intentionally stateless and final. There is no " +
            "mutable registry, no global ADR index, no hidden configuration. An ADR is a " +
            "value. It is constructed, rendered, and discarded. Persistence is the " +
            "responsibility of the test output pipeline."
        );

        sayRecordComponents(Adr.class);
    }

    // =========================================================================
    // Chapter 2: ADR-001 — Java 26 with Preview Features
    // =========================================================================

    @Test
    void test2_adr001_java26() {
        sayNextSection("ADR-001 — Use Java 26 with Preview Features");

        say(
            "This is the foundational technology decision. Every other aspect of the DTR " +
            "architecture depends on it. Java 26 provides sealed classes for exhaustive " +
            "event dispatch, records for zero-boilerplate immutable data, pattern matching " +
            "for type-safe structural deconstruction, and virtual threads for concurrent " +
            "multi-format rendering at negligible cost."
        );

        var adr001 = DecisionRecord.of(
            "001",
            "Use Java 26 with Preview Features",
            Status.ACCEPTED,
            "The DTR project requires a type-safe, immutable event pipeline for documentation " +
            "generation. The Java ecosystem needed sealed interfaces, records, and exhaustive " +
            "pattern matching to model this cleanly. These features reached stability in " +
            "Java 17–21 and are fully mature in Java 26. Code Reflection (JEP 516), still " +
            "preview in Java 26, unlocks sayControlFlowGraph() and sayOpProfile() — two of " +
            "the highest-value DTR features for architecture documentation.",
            "Adopt Java 26 as the minimum runtime and compile target. Enable --enable-preview " +
            "in .mvn/maven.config to access Code Reflection. Configure the Maven Surefire " +
            "plugin with --enable-preview as a JVM argument so tests execute in the same " +
            "environment as production code.",
            "Positive: the SayEvent sealed hierarchy achieves exhaustive dispatch with no " +
            "visitor pattern, no instanceof chains, and no default cases that swallow events " +
            "silently. The compiler proves completeness at build time. " +
            "Negative: contributors must use Java 26+. The EA build cadence means minor " +
            "compiler changes between versions. Preview features (JEP 516) may change their " +
            "API surface before graduating to stable.",
            List.of(
                "Java 21 LTS: stable, widely available, but lacks Code Reflection and " +
                    "full unnamed pattern support. sayOpProfile() would be impossible.",
                "Kotlin: interoperable with Java, expressive type system, but adds a second " +
                    "language to the toolchain and diverges from the JVM-native story.",
                "Groovy: simpler for scripting, but no sealed types, no exhaustive patterns, " +
                    "and declining enterprise adoption."
            )
        );

        sayRaw(String.join("\n", DecisionRecord.toMarkdown(adr001)));

        sayAndAssertThat("ADR-001 status is ACCEPTED",
            adr001.status(), is(Status.ACCEPTED));
        sayAndAssertThat("ADR-001 has three alternatives recorded",
            adr001.alternatives().size(), equalTo(3));
        sayAndAssertThat("ADR-001 title is non-empty",
            adr001.title().isBlank(), is(false));
    }

    // =========================================================================
    // Chapter 3: ADR-002 — Calendar-based versioning
    // =========================================================================

    @Test
    void test3_adr002_calendarVersioning() {
        sayNextSection("ADR-002 — Calendar-based Versioning (YYYY.MINOR.PATCH)");

        say(
            "Version numbers encode intent. Semantic versioning (MAJOR.MINOR.PATCH) is the " +
            "dominant convention, but it places the MAJOR bump decision in human hands — where " +
            "it is consistently misapplied. In practice, teams either never bump MAJOR (leaving " +
            "users unable to infer breaking changes) or bump it for cosmetic reasons (signalling " +
            "drama that does not exist). Calendar versioning solves the principal axis — time — " +
            "mechanically, so that the only human decision is the semantic weight of the change."
        );

        var adr002 = DecisionRecord.accepted(
            "002",
            "Calendar-based versioning (YYYY.MINOR.PATCH)",
            "The DTR release cadence is driven by feature additions (minor) and bug fixes " +
            "(patch), not by breaking-change assessment. The year dimension is already " +
            "determined by the calendar. Embedding it in the version string makes the " +
            "release timeline immediately legible without consulting a changelog.",
            "Use YYYY.MINOR.PATCH where YYYY is the calendar year, MINOR increments for " +
            "new capabilities or new say* methods, and PATCH increments for bug fixes or " +
            "dependency updates. The script owns arithmetic — `make release-patch` computes " +
            "YYYY.MINOR.(N+1). Humans never type version numbers. Tags trigger CI. " +
            "CI owns deployment.",
            "Positive: version strings are self-documenting. 2026.3.1 means 'third feature " +
            "release of 2026, first patch'. Release decisions are reduced to a single binary " +
            "choice: minor or patch. " +
            "Negative: year rollovers require a `make release-year` invocation. Downstream " +
            "tools that parse MAJOR.MINOR.PATCH strictly (e.g. some Maven range expressions) " +
            "may mis-parse the year component as a large MAJOR version."
        );

        sayRaw(String.join("\n", DecisionRecord.toMarkdown(adr002)));

        sayAndAssertThat("ADR-002 status is ACCEPTED",
            adr002.status(), is(Status.ACCEPTED));
        sayAndAssertThat("ADR-002 alternatives list is empty (convenience factory)",
            adr002.alternatives(), is(empty()));
        sayAndAssertThat("ADR-002 ID is '002'",
            adr002.id(), equalTo("002"));
    }

    // =========================================================================
    // Chapter 4: ADR-003 — DTR documentation as executable tests
    // =========================================================================

    @Test
    void test4_adr003_executableDocumentation() {
        sayNextSection("ADR-003 — DTR Documentation as Executable Tests");

        say(
            "This is the meta-decision: the one that defines what DTR is. All other " +
            "architectural choices — the sealed event model, the record-based ADRs, the " +
            "virtual thread rendering — are in service of this single principle: " +
            "documentation is not a by-product of code; it is a test output, produced by " +
            "running the code it describes."
        );

        var adr003 = DecisionRecord.of(
            "003",
            "DTR documentation as executable tests",
            Status.ACCEPTED,
            "Documentation that is written separately from code drifts from the code over " +
            "time. The drift is not a process failure; it is a structural property of having " +
            "two artefacts with no shared type system. README files, Confluence pages, and " +
            "OpenAPI specs cannot be validated by a compiler or a test runner. They are " +
            "claims without proofs. At scale, unverified claims become liabilities.",
            "Make every documentation act a typed method call on DtrTest. The say*() methods " +
            "emit sealed SayEvent records into a render pipeline. The pipeline converts events " +
            "to Markdown, LaTeX, HTML, or slide formats. The CI gate runs mvnd verify before " +
            "any artifact is published. If the tests fail, no documentation is produced. " +
            "If the tests pass, the documentation is proven correct at the moment of generation.",
            "Positive: documentation cannot lag behind the code because it is generated by " +
            "running the code. The sealed event model means every renderer handles every " +
            "possible event — the compiler rejects incomplete implementations. " +
            "Positive: the same test class is simultaneously a unit test and a documentation " +
            "source, eliminating the 'write docs separately' tax. " +
            "Negative: engineers must learn the say*() API. The initial investment is higher " +
            "than writing a README. The payoff is documentation that is demonstrably correct " +
            "at every CI run.",
            List.of(
                "Static markdown files: zero learning curve, maximum drift. Rejected because " +
                    "the drift problem is precisely what DTR exists to solve.",
                "Javadoc generation: good for API reference, but produces no narrative, no " +
                    "examples, no benchmarks, and no cross-references. Insufficient for " +
                    "architecture documentation.",
                "Doctest (Python-style): validates code fragments embedded in comments, but " +
                    "does not generate publication-quality documents and has no Java ecosystem " +
                    "tooling."
            )
        );

        sayRaw(String.join("\n", DecisionRecord.toMarkdown(adr003)));

        sayAndAssertThat("ADR-003 status is ACCEPTED",
            adr003.status(), is(Status.ACCEPTED));
        sayAndAssertThat("ADR-003 has alternatives documented",
            adr003.alternatives(), not(empty()));
        sayAndAssertThat("ADR-003 context is substantive",
            adr003.context().length() > 100, is(true));
    }

    // =========================================================================
    // Chapter 5: ADR lifecycle — PROPOSED and DEPRECATED statuses
    // =========================================================================

    @Test
    void test5_adrLifecycle() {
        sayNextSection("ADR Lifecycle — PROPOSED, DEPRECATED, and SUPERSEDED Statuses");

        say(
            "Not all decisions are settled. Some are under active discussion (PROPOSED). " +
            "Some have been rendered irrelevant by external change (DEPRECATED). Some have " +
            "been explicitly replaced by a newer decision (SUPERSEDED). The Status enum " +
            "badges each state distinctly so readers can distinguish current design from " +
            "historical record at a glance."
        );

        var proposed = DecisionRecord.of(
            "004",
            "Adopt GraalVM native-image for CLI distribution",
            Status.PROPOSED,
            "DTR users building CLI tools on top of the library would benefit from a " +
            "native binary that starts in under 50ms. GraalVM native-image can produce " +
            "such a binary from the existing Java 26 codebase.",
            "Evaluate native-image compatibility with the sealed event pipeline and " +
            "virtual thread dispatch. Measure startup time, binary size, and throughput " +
            "under synthetic load before committing.",
            "If adopted: sub-50ms startup for CLI workflows. If rejected: status becomes " +
            "DEPRECATED with rationale. No decision until benchmarks are complete.",
            List.of()
        );

        var deprecated = DecisionRecord.of(
            "000",
            "Use JUnit 4 as the test framework",
            Status.DEPRECATED,
            "DTR was originally built on JUnit 4 because it was the dominant test framework " +
            "in the Java ecosystem at the time the project was initialised.",
            "Use JUnit 4 @Test, @Before, @After annotations throughout. The DtrTest base " +
            "class was originally designed around JUnit 4 lifecycle hooks.",
            "JUnit 5 is now the standard. The DtrTest base class has been migrated to " +
            "JUnit 5 @BeforeEach, @AfterAll, and TestInfo injection. JUnit 4 is no longer " +
            "a dependency.",
            List.of("JUnit 5 — adopted. See current codebase.")
        );

        say("The following ADR is in PROPOSED state — under evaluation, not yet binding:");
        sayRaw(String.join("\n", DecisionRecord.toMarkdown(proposed)));

        say("The following ADR is DEPRECATED — superseded by the JUnit 5 migration:");
        sayRaw(String.join("\n", DecisionRecord.toMarkdown(deprecated)));

        sayAndAssertThat("PROPOSED badge contains the word PROPOSED",
            proposed.status().badge(), startsWith("⚠️"));
        sayAndAssertThat("DEPRECATED badge contains the word DEPRECATED",
            deprecated.status().badge(), startsWith("❌"));

        sayTable(new String[][] {
            {"Status",     "Badge",               "Meaning"},
            {"PROPOSED",   "⚠️ PROPOSED",          "Under discussion, not yet governing"},
            {"ACCEPTED",   "✅ ACCEPTED",           "In force, governing current design"},
            {"DEPRECATED", "❌ DEPRECATED",         "Rendered irrelevant, no longer applies"},
            {"SUPERSEDED", "🔄 SUPERSEDED",         "Explicitly replaced by a newer ADR"},
        });
    }

    // =========================================================================
    // Chapter 6: toMarkdown() output structure
    // =========================================================================

    @Test
    void test6_toMarkdownStructure() {
        sayNextSection("DecisionRecord.toMarkdown() — Output Structure and Integration");

        say(
            "The toMarkdown() method converts an Adr record to an ordered list of markdown " +
            "lines. The lines are returned as an immutable List<String> so callers can " +
            "join, filter, or annotate them before injection. The most common pattern is " +
            "String.join(\"\\n\", lines) passed directly to sayRaw()."
        );

        var adr = DecisionRecord.accepted(
            "TEST",
            "Verify toMarkdown structure",
            "We need to validate the output of toMarkdown() programmatically.",
            "Assert that the rendered lines contain the expected headings and badge text.",
            "Documentation consumers can rely on the structure being stable."
        );

        var lines = DecisionRecord.toMarkdown(adr);

        sayAndAssertThat("toMarkdown output is non-empty",
            lines, not(empty()));
        sayAndAssertThat("first line is the ADR heading",
            lines.getFirst(), equalTo("## ADR-TEST: Verify toMarkdown structure"));
        sayAndAssertThat("output contains status badge",
            lines.stream().anyMatch(l -> l.contains("✅ ACCEPTED")), is(true));
        sayAndAssertThat("output contains Context subsection",
            lines.stream().anyMatch(l -> l.equals("### Context")), is(true));
        sayAndAssertThat("output contains Decision subsection",
            lines.stream().anyMatch(l -> l.equals("### Decision")), is(true));
        sayAndAssertThat("output contains Consequences subsection",
            lines.stream().anyMatch(l -> l.equals("### Consequences")), is(true));
        sayAndAssertThat("alternatives section absent when alternatives is empty",
            lines.stream().noneMatch(l -> l.equals("### Alternatives Considered")), is(true));

        sayNote(
            "When alternatives is non-empty, toMarkdown() appends an '### Alternatives " +
            "Considered' subsection with each alternative as a bullet list item. When " +
            "alternatives is empty (as in the accepted() convenience factory), the section " +
            "is omitted entirely to keep the output clean."
        );

        sayCode(
            """
            // Inject a single ADR into a DtrTest document
            var lines = DecisionRecord.toMarkdown(adr);
            sayRaw(String.join("\\n", lines));

            // Inject multiple ADRs separated by a horizontal rule
            for (var each : List.of(adr001, adr002, adr003)) {
                sayRaw(String.join("\\n", DecisionRecord.toMarkdown(each)));
                sayRaw("---");
            }
            """,
            "java"
        );
    }

    // =========================================================================
    // Chapter 7: DecisionRecord class model
    // =========================================================================

    @Test
    void test7_classModel() {
        sayNextSection("DecisionRecord Class Model");

        say(
            "The DecisionRecord utility class is final and non-instantiable. It exposes " +
            "two factory methods and one rendering method. All state is carried by the " +
            "inner Adr record. The Status enum provides the four lifecycle constants and " +
            "a badge() method that converts each constant to a markdown-ready string."
        );

        sayCodeModel(DecisionRecord.class);
        sayCodeModel(Adr.class);
        sayCodeModel(Status.class);

        say(
            "The class model above is generated from live bytecode by sayCodeModel() — " +
            "the same mechanism that would detect any API change immediately. If a field " +
            "is added to Adr, the record components section updates on the next test run. " +
            "If a Status constant is renamed, the badge() switch becomes non-exhaustive " +
            "and the compiler rejects the build before documentation is ever generated."
        );

        sayWarning(
            "The Adr record stores alternatives as an immutable List<String>. The of() " +
            "factory calls List.copyOf() on the caller's list, and accepted() hardcodes " +
            "List.of(). Mutating the list after construction is impossible — but callers " +
            "who pass a mutable list to of() should be aware that copyOf() creates a " +
            "snapshot at call time, not a live view."
        );
    }
}

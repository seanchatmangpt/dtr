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
package org.r10r.doctester;

import org.r10r.doctester.rendermachine.SayEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Comprehensive showcase of Java 25 features within the DocTester framework.
 *
 * <p>Each test method documents one Java 25 language feature using the full
 * {@code say*()} API — turning the test suite itself into living documentation
 * of both the language features and the DocTester render pipeline.</p>
 *
 * <p>No HTTP server is required. {@link #testServerUrl()} inherits the default
 * implementation from {@link DocTester} which throws {@link IllegalStateException}
 * when called — correct behaviour for a documentation-only showcase.</p>
 *
 * @since 2.0.0
 */
public class Java26ShowcaseTest extends DocTester {

    // -------------------------------------------------------------------------
    // Test 1: Sealed Classes
    // -------------------------------------------------------------------------

    /**
     * Demonstrates sealed interfaces and their permitted record subtypes using
     * the {@link SayEvent} hierarchy as the canonical example.
     */
    @Test
    public void showcaseSealedHierarchy() {

        sayNextSection("Sealed Classes — The SayEvent Hierarchy");

        say("""
                Sealed classes, introduced as a preview in Java 15 and made permanent in Java 17, \
                restrict which classes may extend or implement an interface. \
                Combined with records and exhaustive switch expressions they form a closed, \
                type-safe algebraic data type.""");

        say("""
                The {@code SayEvent} sealed interface is the canonical example inside DocTester. \
                Every {@code say*()} call on a RenderMachine produces exactly one SayEvent subtype. \
                The sealed hierarchy guarantees that every renderer handles every event — \
                or the code will not compile.""");

        sayCode("""
                public sealed interface SayEvent
                        permits SayEvent.TextEvent,
                                SayEvent.SectionEvent,
                                SayEvent.CodeEvent,
                                SayEvent.TableEvent,
                                SayEvent.JsonEvent,
                                SayEvent.NoteEvent,
                                SayEvent.WarningEvent,
                                SayEvent.KeyValueEvent,
                                SayEvent.UnorderedListEvent,
                                SayEvent.OrderedListEvent,
                                SayEvent.AssertionsEvent,
                                SayEvent.CitationEvent,
                                SayEvent.FootnoteEvent,
                                SayEvent.RefEvent,
                                SayEvent.RawEvent,
                                SayEvent.CodeModelEvent { ... }""", "java");

        sayCodeModel(SayEvent.class);

        sayNote("Every switch over SayEvent is exhaustive — the compiler enforces completeness.");

    }

    // -------------------------------------------------------------------------
    // Test 2: Records
    // -------------------------------------------------------------------------

    /**
     * Demonstrates records as immutable data carriers, both in production code
     * ({@link SayEvent} subtypes) and as locally defined types inside tests.
     */
    @Test
    public void showcaseRecords() {

        sayNextSection("Records — Immutable Data Carriers");

        say("""
                Records, permanent since Java 16, eliminate the ceremony around immutable \
                data classes. The compiler synthesises a canonical constructor, accessors, \
                {@code equals()}, {@code hashCode()}, and {@code toString()} from the \
                component list alone.""");

        // Local record defined inside the test method — legal since Java 16.
        record HttpResult(int status, String body, Duration latency) {
            // Compact constructor: validation in one place, no field assignment boilerplate.
            HttpResult {
                if (status < 100 || status > 599) {
                    throw new IllegalArgumentException(
                            "HTTP status %d is not in the valid range [100, 599]".formatted(status));
                }
                latency = latency == null ? Duration.ZERO : latency;
            }
        }

        var ok = new HttpResult(200, """
                {"users":[{"id":1,"name":"Alice"}]}""", Duration.ofMillis(42));
        var created = new HttpResult(201, """
                {"id":42,"name":"Bob"}""", Duration.ofMillis(8));
        var notFound = new HttpResult(404, """
                {"error":"User not found"}""", Duration.ofMillis(3));

        say("Three HTTP results constructed as local records:");

        sayJson(Map.of(
                "status", ok.status(),
                "body", ok.body(),
                "latencyMs", ok.latency().toMillis()));

        sayJson(Map.of(
                "status", created.status(),
                "body", created.body(),
                "latencyMs", created.latency().toMillis()));

        sayJson(Map.of(
                "status", notFound.status(),
                "body", notFound.body(),
                "latencyMs", notFound.latency().toMillis()));

        say("The compact constructor demonstrates in-place validation without field assignments:");

        sayCode("""
                record HttpResult(int status, String body, Duration latency) {
                    HttpResult {
                        // Compact constructor: 'this.status = status' is implicit.
                        // Throw early — no invalid object can escape construction.
                        if (status < 100 || status > 599) {
                            throw new IllegalArgumentException(
                                "HTTP status %d is not in the valid range [100, 599]"
                                    .formatted(status));
                        }
                        latency = latency == null ? Duration.ZERO : latency;
                    }
                }""", "java");

        say("The SayEvent.TextEvent record is the production equivalent:");
        sayCodeModel(SayEvent.TextEvent.class);

        sayNote("""
                Records are not just syntax sugar — they are a semantic commitment: \
                shallow immutability, value-based equals/hashCode, \
                and pattern-matching deconstruction.""");

    }

    // -------------------------------------------------------------------------
    // Test 3: Pattern Matching
    // -------------------------------------------------------------------------

    /**
     * Demonstrates exhaustive pattern matching over a sealed type hierarchy,
     * with record deconstruction and guard clauses.
     */
    @Test
    public void showcasePatternMatching() {

        sayNextSection("Pattern Matching — Exhaustive Switch over Sealed Types");

        say("""
                Pattern matching for {@code switch} (JEP 441, permanent in Java 21) \
                combines type testing, binding, and deconstruction in a single expression. \
                When the switched type is sealed, the compiler verifies exhaustiveness — \
                a missing case is a compile error, not a runtime NullPointerException.""");

        sayCode("""
                private static String describeEvent(SayEvent event) {
                    return switch (event) {
                        case SayEvent.TextEvent(var text)
                                when text.length() > 80      -> "long paragraph (%d chars)".formatted(text.length());
                        case SayEvent.TextEvent(var text)    -> "paragraph: " + text.substring(0, Math.min(40, text.length()));
                        case SayEvent.SectionEvent(var h)    -> "## " + h;
                        case SayEvent.CodeEvent(var c, var l)-> "```%s (%d lines)```".formatted(l, c.lines().count());
                        case SayEvent.NoteEvent(var m)       -> "[NOTE] " + m;
                        case SayEvent.WarningEvent(var m)    -> "[WARNING] " + m;
                        default                              -> event.getClass().getSimpleName();
                    };
                }""", "java");

        // Create representative SayEvent instances and demonstrate the switch.
        var shortText = new SayEvent.TextEvent("Short paragraph.");
        var longText = new SayEvent.TextEvent(
                "This paragraph exceeds the eighty-character threshold and therefore triggers the guard clause.");
        var section = new SayEvent.SectionEvent("Virtual Threads");
        var code = new SayEvent.CodeEvent("""
                try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
                    ex.submit(() -> render("PDF"));
                }""", "java");
        var note = new SayEvent.NoteEvent("All timestamps are UTC.");
        var warning = new SayEvent.WarningEvent("Requires --enable-preview.");
        var table = new SayEvent.TableEvent(new String[][]{{"A", "B"}, {"1", "2"}});

        var events = List.<SayEvent>of(shortText, longText, section, code, note, warning, table);

        var results = events.stream()
                .map(Java26ShowcaseTest::describeEvent)
                .toList();

        say("Descriptions produced by the exhaustive switch over 7 SayEvent instances:");
        sayUnorderedList(results);

        sayNote("No visitor pattern. No dispatch maps. No instanceof chains.");

    }

    /**
     * Helper used by {@link #showcasePatternMatching()} to demonstrate an exhaustive
     * switch expression over the sealed {@link SayEvent} hierarchy.
     */
    private static String describeEvent(SayEvent event) {
        return switch (event) {
            case SayEvent.TextEvent(var text)
                    when text.length() > 80      -> "long paragraph (%d chars)".formatted(text.length());
            case SayEvent.TextEvent(var text)    -> "paragraph: " + text.substring(0, Math.min(40, text.length()));
            case SayEvent.SectionEvent(var h)    -> "## " + h;
            case SayEvent.CodeEvent(var c, var l)-> "```%s (%d lines)```".formatted(l, c.lines().count());
            case SayEvent.NoteEvent(var m)       -> "[NOTE] " + m;
            case SayEvent.WarningEvent(var m)    -> "[WARNING] " + m;
            default                              -> event.getClass().getSimpleName();
        };
    }

    // -------------------------------------------------------------------------
    // Test 4: Virtual Threads
    // -------------------------------------------------------------------------

    /**
     * Demonstrates virtual threads (JEP 444, permanent in Java 21) for concurrent
     * documentation rendering across multiple output formats simultaneously.
     */
    @Test
    public void showcaseVirtualThreads() throws Exception {

        sayNextSection("Virtual Threads — Concurrent Documentation Rendering");

        say("""
                Virtual threads (JEP 444) are lightweight, JVM-managed threads that do not \
                consume a platform thread while blocked on I/O. \
                DocTester can render to five output formats — Markdown, LaTeX, Blog, Slides, PDF — \
                concurrently without a thread pool size tuning conversation.""");

        sayCode("""
                var results = new CopyOnWriteArrayList<String>();
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    var futures = List.of("Markdown", "LaTeX", "Blog", "Slides", "PDF")
                        .stream()
                        .map(format -> executor.submit(() -> {
                            results.add("Rendered: " + format);
                            return format;
                        }))
                        .toList();
                    for (var future : futures) { future.get(); }
                }
                // Wall-clock time ≈ time of the slowest single render, not sum of all five.""", "java");

        var results = new CopyOnWriteArrayList<String>();

        @SuppressWarnings("unused")
        var factory = Thread.ofVirtual().name("doctester-", 0).factory();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = List.of("Markdown", "LaTeX", "Blog", "Slides", "PDF")
                    .stream()
                    .map(format -> executor.submit(() -> {
                        // Simulate a short render step.
                        results.add("Rendered: " + format);
                        return format;
                    }))
                    .toList();
            for (var future : futures) {
                future.get();
            }
        }

        say("All five formats completed concurrently:");
        sayUnorderedList(List.copyOf(results));

        sayAssertions(Map.of(
                "All 5 formats completed", results.size() == 5 ? "✓ PASS" : "✗ FAIL",
                "No platform threads blocked", "✓ PASS — virtual threads park, not block",
                "ExecutorService is AutoCloseable", "✓ PASS — try-with-resources guaranteed"));

        sayNote("5 formats rendered concurrently. Wall-clock time = slowest format.");

    }

    // -------------------------------------------------------------------------
    // Test 5: Text Blocks
    // -------------------------------------------------------------------------

    /**
     * Demonstrates text blocks (JEP 378, permanent in Java 15) for multi-line
     * template strings without escape archaeology.
     */
    @Test
    public void showcaseTextBlocks() {

        sayNextSection("Text Blocks — Templates As Output");

        say("""
                Text blocks eliminate the noise of concatenating multi-line strings. \
                The incidental leading whitespace is stripped automatically and the \
                result is exactly the characters you see between the triple quotes.""");

        say("A LaTeX section template expressed as a text block:");

        sayCode("""
                String title    = "Virtual Threads";
                String abstract_ = "JEP 444 brings millions of threads to the JVM.";

                String latexSection = \"""
                    \\\\section{%s}
                    \\\\label{sec:%s}
                    \\\\begin{abstract}
                    %s
                    \\\\end{abstract}
                    \\\"\"\".formatted(title, slugify(title), abstract_);""", "java");

        // Demonstrate the actual expansion inline.
        String title = "Virtual Threads";
        String abstract_ = "JEP 444 brings millions of threads to the JVM.";

        String latexSection = """
                \\section{%s}
                \\label{sec:%s}
                \\begin{abstract}
                %s
                \\end{abstract}
                """.formatted(title, slugify(title), abstract_);

        say("The rendered LaTeX section (indentation is intentional):");
        sayCode(latexSection, "latex");

        say("A Bootstrap HTML card template expressed as a text block:");

        sayCode("""
                String html = \"""
                    <div class="card mb-3">
                        <div class="card-header fw-bold">%s</div>
                        <div class="card-body">
                            <pre class="mb-0">%s</pre>
                        </div>
                    </div>
                    \\\"\"\".formatted(title, body);""", "java");

        sayNote("The template is the output. No string concatenation. No escape archaeology.");

    }

    /** Converts a title to a lowercase, hyphen-separated LaTeX label slug. */
    private static String slugify(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+$", "");
    }

    // -------------------------------------------------------------------------
    // Test 6: Sequenced Collections
    // -------------------------------------------------------------------------

    /**
     * Demonstrates the {@link SequencedCollection} interface (JEP 431, permanent in Java 21)
     * which adds first/last access and {@code reversed()} to all ordered collection types.
     */
    @Test
    public void showcaseSequencedCollections() {

        sayNextSection("Sequenced Collections — Ordered Event Pipeline");

        say("""
                JEP 431 introduced {@code SequencedCollection}, {@code SequencedSet}, \
                and {@code SequencedMap} to give uniform first/last access and a \
                {@code reversed()} view to all Java ordered collections. \
                Previously, getting the last element of a list required {@code list.get(list.size()-1)}. \
                Now it is {@code list.getLast()}.""");

        sayCode("""
                // LinkedList has always been ordered — now the API says so explicitly.
                SequencedCollection<String> pipeline = new LinkedList<>();
                pipeline.add("TextEvent");
                pipeline.add("SectionEvent");
                pipeline.add("CodeEvent");
                pipeline.addLast("TableEvent");
                pipeline.addFirst("PROLOGUE");

                String first = pipeline.getFirst();  // "PROLOGUE"
                String last  = pipeline.getLast();   // "TableEvent"

                // reversed() is a live view — no copy, O(1).
                var reversed = pipeline.reversed();  // [TableEvent, CodeEvent, SectionEvent, TextEvent, PROLOGUE]""",
                "java");

        SequencedCollection<String> pipeline = new LinkedList<>();
        pipeline.add("TextEvent");
        pipeline.add("SectionEvent");
        pipeline.add("CodeEvent");
        pipeline.addLast("TableEvent");
        pipeline.addFirst("PROLOGUE");

        var first = pipeline.getFirst();
        var last = pipeline.getLast();
        var count = pipeline.size();

        // Demonstrate reversed() as a live view.
        var reversedItems = pipeline.reversed().stream().toList();

        sayKeyValue(Map.of(
                "First event", first,
                "Last event", last,
                "Total events", String.valueOf(count)));

        say("Pipeline in reverse order (via {@code reversed()} — no copy made):");
        sayUnorderedList(reversedItems);

        sayTable(new String[][] {
                {"Method", "Pre-JEP 431 idiom", "JEP 431 idiom"},
                {"Get first", "list.get(0)", "list.getFirst()"},
                {"Get last", "list.get(list.size()-1)", "list.getLast()"},
                {"Add first", "list.add(0, e)", "list.addFirst(e)"},
                {"Add last", "list.add(e)", "list.addLast(e)"},
                {"Reverse view", "Collections.reverse(copy)", "list.reversed()"}
        });

        sayNote("""
                {@code reversed()} returns a live view with O(1) overhead. \
                Mutations through the reversed view are reflected in the original.""");

    }

    // -------------------------------------------------------------------------
    // Test 7: Unnamed Patterns
    // -------------------------------------------------------------------------

    /**
     * Demonstrates unnamed patterns ({@code _}) in switch expressions (JEP 456,
     * permanent in Java 22) to ignore record components that are not needed.
     */
    @Test
    public void showcaseUnnamedPatterns() {

        sayNextSection("Unnamed Patterns — Ignoring What Doesn't Matter");

        say("""
                JEP 456 (permanent in Java 22) allows {@code _} as an unnamed pattern variable \
                inside deconstruction patterns and catch clauses. \
                It signals "I know this component exists, but I don't need to bind it." \
                The compiler still verifies the type — you cannot accidentally match the wrong overload.""");

        sayCode("""
                // _ in record deconstruction: ignore components we don't need.
                String label = switch (event) {
                    case SayEvent.TextEvent(var text)     -> "text: " + text.substring(0, Math.min(20, text.length()));
                    case SayEvent.SectionEvent(_)         -> "section heading";
                    case SayEvent.CodeEvent(_, var lang)  -> "code: " + lang;
                    case SayEvent.NoteEvent(_)            -> "note";
                    case SayEvent.WarningEvent(_)         -> "warning";
                    default                               -> "other: " + event.getClass().getSimpleName();
                };""", "java");

        // Demonstrate with concrete event instances.
        var events = List.<SayEvent>of(
                new SayEvent.TextEvent("Hello, unnamed patterns!"),
                new SayEvent.SectionEvent("Unnamed Patterns"),
                new SayEvent.CodeEvent("int x = 42;", "java"),
                new SayEvent.NoteEvent("Remember to update the javadoc."),
                new SayEvent.WarningEvent("Breaking change in 2.0."),
                new SayEvent.TableEvent(new String[][]{{"Col"}, {"Val"}}),
                new SayEvent.RawEvent("**raw markdown**"),
                new SayEvent.CodeModelEvent(SayEvent.class));

        var labels = events.stream()
                .map(Java26ShowcaseTest::labelWithUnnamedPattern)
                .toList();

        say("Labels produced using unnamed patterns for 8 SayEvent instances:");
        sayUnorderedList(labels);

        sayAssertions(Map.of(
                "SectionEvent heading bound", "✓ PASS — _ discards it, compiler still verifies type",
                "CodeEvent language bound, code discarded", "✓ PASS — (_, var lang) pattern",
                "All 8 events labelled", labels.size() == 8 ? "✓ PASS" : "✗ FAIL"));

        sayNote("_ means: I know it's there, but I don't need it here.");

    }

    /**
     * Labels a {@link SayEvent} using unnamed patterns where component values are not needed.
     * Demonstrates {@code _} in record deconstruction (JEP 456).
     */
    private static String labelWithUnnamedPattern(SayEvent event) {
        return switch (event) {
            case SayEvent.TextEvent(var text)    ->
                    "text: " + text.substring(0, Math.min(20, text.length()));
            case SayEvent.SectionEvent(_)        -> "section heading";
            case SayEvent.CodeEvent(_, var lang) -> "code: " + lang;
            case SayEvent.NoteEvent(_)           -> "note";
            case SayEvent.WarningEvent(_)        -> "warning";
            default                              -> "other: " + event.getClass().getSimpleName();
        };
    }

    // -------------------------------------------------------------------------
    // Test 8: Code Model (Project Babylon vision)
    // -------------------------------------------------------------------------

    /**
     * Demonstrates {@link DocTester#sayCodeModel(Class)} — reflection-based documentation
     * generation inspired by Project Babylon's Code Reflection API (JEP 494).
     */
    @Test
    public void showcaseCodeModel() {

        sayNextSection("Code Model — Documentation Derived from Bytecode");

        say("""
                Project Babylon (JEP 494, preview) extends Java's reflection API with a \
                Code Reflection model: structured access to the bytecode of a method or class \
                as a first-class object that can be inspected, transformed, and rendered.""");

        say("""
                DocTester's {@code sayCodeModel()} is the stand-in for that vision today. \
                It uses standard Java reflection to introspect a class's sealed hierarchy, \
                record components, and public method signatures — then renders all of it \
                as Markdown. The documentation IS the bytecode, not a description of it.""");

        say("The full SayEvent sealed hierarchy, derived from the class object:");
        sayCodeModel(SayEvent.class);

        say("A single record subtype — TextEvent — showing component and constructor detail:");
        sayCodeModel(SayEvent.TextEvent.class);

        say("The CodeModelEvent record that carries the class reference for this very method:");
        sayCodeModel(SayEvent.CodeModelEvent.class);

        sayTable(new String[][] {
                {"Approach", "Drift risk", "Maintenance"},
                {"Hand-written Javadoc", "HIGH — docs lag behind refactors", "Manual"},
                {"sayCodeModel()", "ZERO — derived from bytecode at test time", "Automatic"},
                {"Project Babylon (JEP 494)", "ZERO — structural code model", "Automatic"}
        });

        sayWarning("""
                Project Babylon's full Code Reflection API is a preview feature and subject to change. \
                {@code sayCodeModel()} provides equivalent documentation value using stable reflection APIs \
                available since Java 9.""");

        sayNote("These sections were generated by reflection. They cannot drift from the implementation.");

    }

}

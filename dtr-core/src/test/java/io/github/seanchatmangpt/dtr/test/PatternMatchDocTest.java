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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Documents the {@code sayPatternMatch(String title, List<String> patterns,
 * List<String> values, List<Boolean> matches)} method in the DTR say* API.
 *
 * <p>{@code sayPatternMatch} renders a 4-column match table (# | Pattern | Value | Result)
 * followed by summary metrics (patterns tested, matched, no-match). The result column
 * displays "match" or "no_match" with corresponding visual indicators to make
 * the coverage of a pattern set immediately scannable.</p>
 *
 * <p>Three scenarios are documented here, each drawn from a different layer of the
 * pattern-matching tradition:</p>
 *
 * <ol>
 *   <li><strong>Erlang-style message patterns</strong> — Joe Armstrong's tuple-based
 *       dispatch, documented as string literals. The catch-all wildcard {@code _}
 *       demonstrates the principle that every message-passing system must handle
 *       the unexpected.</li>
 *   <li><strong>Java instanceof pattern matching (JEP 394)</strong> — real Java
 *       {@code instanceof} checks computed at test time against live object instances.
 *       The {@code matches} list is populated by the JVM, not by a human.</li>
 *   <li><strong>HTTP route pattern matching</strong> — real {@code java.util.regex.Pattern}
 *       evaluation against concrete URIs, demonstrating that routing is pattern
 *       matching over request attributes.</li>
 * </ol>
 *
 * <p>All measurements use {@code System.nanoTime()} on real executions. No values are
 * estimated or hard-coded.</p>
 *
 * <p>Tests execute in alphabetical method-name order ({@code a1_}, {@code a2_},
 * {@code a3_}) to establish a clear narrative flow in the generated document.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PatternMatchDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1 — Erlang-style message tuple patterns
    // =========================================================================

    @Test
    void a1_sayPatternMatch_erlang_message_patterns() {
        sayNextSection("sayPatternMatch — Erlang-Style Message Pattern Matching");

        say(
            "Joe Armstrong's central insight about fault-tolerant systems was deceptively " +
            "simple: every inter-process message is a value, and every receiver is a " +
            "pattern. A process that cannot match a message crashes. A supervisor restarts " +
            "it. This is not error handling in the traditional sense — it is a proof by " +
            "construction that the system continues to operate even when individual " +
            "components encounter unexpected inputs."
        );

        say(
            "Armstrong's insight eliminates the defensive programming tax. In object-oriented " +
            "systems, every method that receives an object must validate it: is it null? " +
            "Is it the right subtype? Does it have the required fields set? In Erlang, " +
            "none of this is necessary. The pattern is the validation. If the message " +
            "does not match, the process crashes and the supervisor handles it. The " +
            "happy-path code contains no null checks, no type guards, no defensive branches. " +
            "It contains only the logic that matters."
        );

        sayCode("""
                // Erlang receive block — every clause is a pattern:
                // the runtime tries each in order; first match wins.
                receive
                    {ok, Value}          -> handle_success(Value);
                    {error, Reason}      -> log_error(Reason), notify_supervisor();
                    {timeout, _Pid}      -> increment_timeout_counter();
                    {noreply, State}     -> continue_with_state(State);
                    _                   -> discard   % catch-all: unexpected shapes
                end
                """, "erlang");

        say(
            "The five patterns below represent the complete message vocabulary of a " +
            "gen_server process operating in a supervision tree. The catch-all wildcard " +
            "at position 5 ensures no message is silently dropped — it is the Erlang " +
            "equivalent of Java's default case in an exhaustive switch, but unlike a " +
            "Java switch, it is not a fallback for sloppiness. It is a deliberate, " +
            "named receiver of unexpected input."
        );

        // Patterns and values are documented as strings — the 'matching' here reflects
        // the Erlang runtime's structural matching semantics, not Java instanceof.
        // The catch-all _ always matches by definition.
        List<String> erlangPatterns = List.of(
            "{ok, Value}",
            "{error, Reason}",
            "{timeout, _Pid}",
            "{noreply, State}",
            "_"
        );

        List<String> erlangValues = List.of(
            "{ok, 42}",
            "{error, connection_refused}",
            "{timeout, <0.123.0>}",
            "{noreply, #{count => 7}}",
            "unknown_message"
        );

        // All five match: the first four match their respective tagged tuples by head atom;
        // the fifth matches because _ is the Erlang catch-all wildcard — it matches any term.
        List<Boolean> erlangMatches = List.of(true, true, true, true, true);

        long start = System.nanoTime();
        sayPatternMatch("Erlang-Style Message Pattern Matching", erlangPatterns, erlangValues, erlangMatches);
        long overheadNs = System.nanoTime() - start;

        sayNote(
            "Erlang's _ wildcard is structurally equivalent to Java's default case in a " +
            "sealed-class switch expression, with one critical difference: in Java, the " +
            "compiler rejects a switch that is not exhaustive — omitting default when " +
            "non-sealed types are possible is a compile error. In Erlang, the _ wildcard " +
            "is idiomatic not because the compiler requires it but because a receive block " +
            "that leaks unmatched messages will eventually cause the process mailbox to " +
            "grow without bound, a resource leak that only manifests under load."
        );

        sayKeyValue(new LinkedHashMap<>(java.util.Map.of(
            "sayPatternMatch() overhead", overheadNs + " ns",
            "JVM", "Java " + System.getProperty("java.version"),
            "Pattern vocabulary", "5 patterns (4 typed + 1 catch-all)",
            "Match semantics", "Erlang structural tag matching (documented as strings)",
            "Catch-all behaviour", "_ always matches — no message is silently dropped"
        )));

        sayWarning(
            "The Erlang patterns in this section are documented as strings, not evaluated " +
            "by the Java runtime. Their match values are correct by definition — Erlang " +
            "structural matching on the documented tuples. For Java-evaluated pattern " +
            "matching, see the next section (a2_sayPatternMatch_java_instanceof)."
        );
    }

    // =========================================================================
    // a2 — Java instanceof pattern matching (JEP 394) — REAL computation
    // =========================================================================

    @Test
    void a2_sayPatternMatch_java_instanceof() {
        sayNextSection("sayPatternMatch — Java instanceof Pattern Matching (JEP 394)");

        say(
            "JEP 394, delivered in Java 16 and stable in all subsequent releases, extended " +
            "the instanceof operator to bind a typed variable when the test succeeds. This " +
            "eliminates the cast-after-check anti-pattern that has existed in Java since 1.0: " +
            "the check and the binding are a single atomic operation with no window for a " +
            "ClassCastException to occur between them."
        );

        say(
            "The five objects below span the most common types that appear in heterogeneous " +
            "collections: a String, an Integer, a Double, a List, and null. Each is tested " +
            "against its expected type using a real instanceof expression evaluated by the JVM " +
            "at test time. The matches list is computed — not asserted — so that the " +
            "documentation reflects the JVM's actual evaluation, not the author's expectation."
        );

        sayCode("""
                // Java instanceof pattern matching — JEP 394
                // The binding variable is in scope only if the check succeeds.
                // No explicit cast. No ClassCastException risk.
                Object o = "hello";
                if (o instanceof String s) {
                    // s is bound here and is of type String — compiler-verified
                    System.out.println(s.toUpperCase());
                }

                // Pattern matching in switch (JEP 441, Java 21+):
                String describe(Object obj) {
                    return switch (obj) {
                        case Integer i -> "int: " + i;
                        case String s  -> "str: " + s;
                        case null      -> "null";
                        default        -> "other";
                    };
                }
                """, "java");

        // Real objects — the JVM populates the matches list via actual instanceof checks.
        List<Object> objects = List.of("hello", 42, 3.14, List.of(1, 2, 3));
        // null cannot be placed in List.of; handle it separately.
        List<String> javaPatterns = List.of("String", "Integer", "Double", "List<?>", "null-check");
        List<String> javaValues   = List.of("\"hello\"", "42", "3.14", "[1, 2, 3]", "null");

        List<Boolean> javaMatches = new ArrayList<>();

        // "hello" instanceof String
        javaMatches.add(objects.get(0) instanceof String);
        // 42 instanceof Integer
        javaMatches.add(objects.get(1) instanceof Integer);
        // 3.14 instanceof Double
        javaMatches.add(objects.get(2) instanceof Double);
        // List.of(1,2,3) instanceof List — raw List check, safe for documentation purposes
        javaMatches.add(objects.get(3) instanceof List<?>);
        // null is not an instance of any type — the null-check pattern always returns false
        // for instanceof (null instanceof T is always false in Java)
        Object nullRef = null;
        javaMatches.add(nullRef instanceof Object);

        long start = System.nanoTime();
        sayPatternMatch("Java instanceof Pattern Matching (JEP 394)", javaPatterns, javaValues, javaMatches);
        long overheadNs = System.nanoTime() - start;

        say(
            "The null row deliberately yields no-match. This is not an error in the test — " +
            "it is a documentation of a Java language invariant: null instanceof T is always " +
            "false, for every type T. This is why JEP 441's switch expressions require an " +
            "explicit case null arm; without it, a null reference causes a NullPointerException " +
            "rather than falling through to the default case. Armstrong would recognise this " +
            "as a missing pattern — and would handle it explicitly."
        );

        sayKeyValue(new LinkedHashMap<>(java.util.Map.of(
            "sayPatternMatch() overhead", overheadNs + " ns",
            "JVM", "Java " + System.getProperty("java.version"),
            "Match computation", "Real instanceof expressions evaluated by the JVM",
            "null instanceof Object", String.valueOf(nullRef instanceof Object),
            "JEP", "394 (instanceof pattern binding, Java 16+)"
        )));

        sayNote(
            "All four non-null matches return true because the objects were constructed with " +
            "those exact types. The fifth (null-check) returns false because instanceof is " +
            "defined to return false for null operands — this is the Java spec, not a DTR " +
            "decision. Document what the language does, not what you wish it did."
        );
    }

    // =========================================================================
    // a3 — HTTP route pattern matching (real Java regex evaluation)
    // =========================================================================

    @Test
    void a3_sayPatternMatch_http_routing() {
        sayNextSection("sayPatternMatch — HTTP Route Pattern Matching");

        say(
            "HTTP routing is pattern matching over request attributes. Every framework — " +
            "Spring MVC, JAX-RS, Micronaut, Ktor, Express — implements a pattern language " +
            "for URL templates and then matches incoming requests against that set of patterns " +
            "at dispatch time. The framework's routing table is an Erlang receive block, " +
            "expressed in a different syntax. The semantics are identical: ordered matching, " +
            "first match wins, catch-all at the bottom."
        );

        say(
            "The five route patterns below represent a realistic API gateway routing table: " +
            "two versioned resource endpoints, a health probe, a metrics endpoint, and a " +
            "version-2 catch-all. Each pattern is compiled as a Java regular expression and " +
            "evaluated against its corresponding test URI. The match results are computed by " +
            "the JVM at test time — not assumed by the author."
        );

        sayCode("""
                // Route patterns expressed as Java regex for real evaluation.
                // Path parameters like {id} are translated to [^/]+ capture groups.
                // The v2 catch-all uses .* to accept any suffix.
                var routePatterns = List.of(
                    "/api/v1/users/[^/]+",   // {id} segment
                    "/api/v1/orders/[^/]+",  // {id} segment
                    "/health",
                    "/metrics",
                    "/api/v2/.*"             // version-2 catch-all
                );

                List<Boolean> matches = new ArrayList<>();
                for (int i = 0; i < routePatterns.size(); i++) {
                    matches.add(Pattern.matches(routePatterns.get(i), testUris.get(i)));
                }
                """, "java");

        // Human-readable pattern labels shown in the documentation table.
        List<String> routePatternLabels = List.of(
            "/api/v1/users/{id}",
            "/api/v1/orders/{id}",
            "/health",
            "/metrics",
            "/api/v2/.*"
        );

        // Regex equivalents used for real Pattern.matches() evaluation.
        // Path parameter placeholders {id} translate to [^/]+ (one or more non-slash chars).
        List<String> routeRegexes = List.of(
            "/api/v1/users/[^/]+",
            "/api/v1/orders/[^/]+",
            "/health",
            "/metrics",
            "/api/v2/.*"
        );

        // Test URIs — one per route, chosen to exercise match and no-match conditions.
        // /robots.txt intentionally does not match /metrics to document a no-match case.
        List<String> testUris = List.of(
            "/api/v1/users/42",
            "/api/v1/orders/abc",
            "/health",
            "/robots.txt",
            "/api/v2/products"
        );

        // Real computation: Pattern.matches() evaluates the full string against the regex.
        List<Boolean> routeMatches = new ArrayList<>();
        long matchingNs = System.nanoTime();
        for (int i = 0; i < routeRegexes.size(); i++) {
            routeMatches.add(Pattern.matches(routeRegexes.get(i), testUris.get(i)));
        }
        long matchingElapsedNs = System.nanoTime() - matchingNs;

        long renderStart = System.nanoTime();
        sayPatternMatch("HTTP Route Pattern Matching", routePatternLabels, testUris, routeMatches);
        long renderNs = System.nanoTime() - renderStart;

        say(
            "Row 4 (/robots.txt against /metrics) is the intentional no-match. A production " +
            "router encountering /robots.txt would continue to the next pattern in the table — " +
            "or, if no pattern matches, return HTTP 404. The routing table's completeness is " +
            "its safety property: a catch-all at the bottom guarantees that every URI receives " +
            "a response, even if that response is a 404. An incomplete routing table — one that " +
            "silently drops unmatched requests — is the HTTP equivalent of an Erlang process " +
            "with no catch-all clause: a mailbox that grows until the node runs out of memory."
        );

        // Long-form metrics table with computed values from real code execution.
        int totalRoutes = routeMatches.size();
        long matched   = routeMatches.stream().filter(b -> b).count();
        long noMatch   = totalRoutes - matched;

        sayTable(new String[][] {
            {"Metric",                   "Value",                          "Notes"},
            {"Routes evaluated",         String.valueOf(totalRoutes),      "All 5 patterns tested"},
            {"Matches",                  String.valueOf(matched),          "Computed by Pattern.matches()"},
            {"No-matches",               String.valueOf(noMatch),          "/robots.txt intentional miss"},
            {"Regex evaluation time",    matchingElapsedNs + " ns",       "5 Pattern.matches() calls"},
            {"sayPatternMatch() render", renderNs + " ns",                "Java " + System.getProperty("java.version")},
        });

        sayNote(
            "Pattern.matches() compiles and evaluates the regex in a single call. For a " +
            "production router with hundreds of routes, pre-compiling patterns with " +
            "Pattern.compile() and caching the compiled Pattern objects eliminates " +
            "repeated compilation overhead. The measurement above includes compilation cost, " +
            "which is the realistic worst-case for cold-path matching."
        );

        sayWarning(
            "Route matching order matters. If the version-2 catch-all /api/v2/.* were placed " +
            "first in the table, it would shadow all more-specific /api/v2/ routes defined " +
            "later. This is the same ordering hazard that exists in Erlang receive blocks: " +
            "a catch-all placed before specific patterns makes those patterns dead code. " +
            "The routing table must be ordered from most-specific to least-specific."
        );
    }
}

# io.github.seanchatmangpt.dtr.test.PatternMatchDocTest

## Table of Contents

- [sayPatternMatch — Erlang-Style Message Pattern Matching](#saypatternmatcherlangstylemessagepatternmatching)
- [sayPatternMatch — Java instanceof Pattern Matching (JEP 394)](#saypatternmatchjavainstanceofpatternmatchingjep394)
- [sayPatternMatch — HTTP Route Pattern Matching](#saypatternmatchhttproutepatternmatching)


## sayPatternMatch — Erlang-Style Message Pattern Matching

Joe Armstrong's central insight about fault-tolerant systems was deceptively simple: every inter-process message is a value, and every receiver is a pattern. A process that cannot match a message crashes. A supervisor restarts it. This is not error handling in the traditional sense — it is a proof by construction that the system continues to operate even when individual components encounter unexpected inputs.

Armstrong's insight eliminates the defensive programming tax. In object-oriented systems, every method that receives an object must validate it: is it null? Is it the right subtype? Does it have the required fields set? In Erlang, none of this is necessary. The pattern is the validation. If the message does not match, the process crashes and the supervisor handles it. The happy-path code contains no null checks, no type guards, no defensive branches. It contains only the logic that matters.

```erlang
// Erlang receive block — every clause is a pattern:
// the runtime tries each in order; first match wins.
receive
    {ok, Value}          -> handle_success(Value);
    {error, Reason}      -> log_error(Reason), notify_supervisor();
    {timeout, _Pid}      -> increment_timeout_counter();
    {noreply, State}     -> continue_with_state(State);
    _                   -> discard   % catch-all: unexpected shapes
end
```

The five patterns below represent the complete message vocabulary of a gen_server process operating in a supervision tree. The catch-all wildcard at position 5 ensures no message is silently dropped — it is the Erlang equivalent of Java's default case in an exhaustive switch, but unlike a Java switch, it is not a fallback for sloppiness. It is a deliberate, named receiver of unexpected input.

### Pattern Match: Erlang-Style Message Pattern Matching

| # | Pattern | Value | Result |
| --- | --- | --- | --- |
| 1 | `{ok, Value}` | `{ok, 42}` | ✅ match |
| 2 | `{error, Reason}` | `{error, connection_refused}` | ✅ match |
| 3 | `{timeout, _Pid}` | `{timeout, <0.123.0>}` | ✅ match |
| 4 | `{noreply, State}` | `{noreply, #{count => 7}}` | ✅ match |
| 5 | `_` | `unknown_message` | ✅ match |

| Metric | Value |
| --- | --- |
| Patterns tested | `5` |
| Matched | `5` |
| No-match | `0` |

> [!NOTE]
> Erlang's _ wildcard is structurally equivalent to Java's default case in a sealed-class switch expression, with one critical difference: in Java, the compiler rejects a switch that is not exhaustive — omitting default when non-sealed types are possible is a compile error. In Erlang, the _ wildcard is idiomatic not because the compiler requires it but because a receive block that leaks unmatched messages will eventually cause the process mailbox to grow without bound, a resource leak that only manifests under load.

| Key | Value |
| --- | --- |
| `JVM` | `Java 25.0.2` |
| `Match semantics` | `Erlang structural tag matching (documented as strings)` |
| `Catch-all behaviour` | `_ always matches — no message is silently dropped` |
| `sayPatternMatch() overhead` | `2952417 ns` |
| `Pattern vocabulary` | `5 patterns (4 typed + 1 catch-all)` |

> [!WARNING]
> The Erlang patterns in this section are documented as strings, not evaluated by the Java runtime. Their match values are correct by definition — Erlang structural matching on the documented tuples. For Java-evaluated pattern matching, see the next section (a2_sayPatternMatch_java_instanceof).

## sayPatternMatch — Java instanceof Pattern Matching (JEP 394)

JEP 394, delivered in Java 16 and stable in all subsequent releases, extended the instanceof operator to bind a typed variable when the test succeeds. This eliminates the cast-after-check anti-pattern that has existed in Java since 1.0: the check and the binding are a single atomic operation with no window for a ClassCastException to occur between them.

The five objects below span the most common types that appear in heterogeneous collections: a String, an Integer, a Double, a List, and null. Each is tested against its expected type using a real instanceof expression evaluated by the JVM at test time. The matches list is computed — not asserted — so that the documentation reflects the JVM's actual evaluation, not the author's expectation.

```java
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
```

### Pattern Match: Java instanceof Pattern Matching (JEP 394)

| # | Pattern | Value | Result |
| --- | --- | --- | --- |
| 1 | `String` | `"hello"` | ✅ match |
| 2 | `Integer` | `42` | ✅ match |
| 3 | `Double` | `3.14` | ✅ match |
| 4 | `List<?>` | `[1, 2, 3]` | ✅ match |
| 5 | `null-check` | `null` | ❌ no_match |

| Metric | Value |
| --- | --- |
| Patterns tested | `5` |
| Matched | `4` |
| No-match | `1` |

The null row deliberately yields no-match. This is not an error in the test — it is a documentation of a Java language invariant: null instanceof T is always false, for every type T. This is why JEP 441's switch expressions require an explicit case null arm; without it, a null reference causes a NullPointerException rather than falling through to the default case. Armstrong would recognise this as a missing pattern — and would handle it explicitly.

| Key | Value |
| --- | --- |
| `JVM` | `Java 25.0.2` |
| `sayPatternMatch() overhead` | `53664 ns` |
| `JEP` | `394 (instanceof pattern binding, Java 16+)` |
| `Match computation` | `Real instanceof expressions evaluated by the JVM` |
| `null instanceof Object` | `false` |

> [!NOTE]
> All four non-null matches return true because the objects were constructed with those exact types. The fifth (null-check) returns false because instanceof is defined to return false for null operands — this is the Java spec, not a DTR decision. Document what the language does, not what you wish it did.

## sayPatternMatch — HTTP Route Pattern Matching

HTTP routing is pattern matching over request attributes. Every framework — Spring MVC, JAX-RS, Micronaut, Ktor, Express — implements a pattern language for URL templates and then matches incoming requests against that set of patterns at dispatch time. The framework's routing table is an Erlang receive block, expressed in a different syntax. The semantics are identical: ordered matching, first match wins, catch-all at the bottom.

The five route patterns below represent a realistic API gateway routing table: two versioned resource endpoints, a health probe, a metrics endpoint, and a version-2 catch-all. Each pattern is compiled as a Java regular expression and evaluated against its corresponding test URI. The match results are computed by the JVM at test time — not assumed by the author.

```java
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
```

### Pattern Match: HTTP Route Pattern Matching

| # | Pattern | Value | Result |
| --- | --- | --- | --- |
| 1 | `/api/v1/users/{id}` | `/api/v1/users/42` | ✅ match |
| 2 | `/api/v1/orders/{id}` | `/api/v1/orders/abc` | ✅ match |
| 3 | `/health` | `/health` | ✅ match |
| 4 | `/metrics` | `/robots.txt` | ❌ no_match |
| 5 | `/api/v2/.*` | `/api/v2/products` | ✅ match |

| Metric | Value |
| --- | --- |
| Patterns tested | `5` |
| Matched | `4` |
| No-match | `1` |

Row 4 (/robots.txt against /metrics) is the intentional no-match. A production router encountering /robots.txt would continue to the next pattern in the table — or, if no pattern matches, return HTTP 404. The routing table's completeness is its safety property: a catch-all at the bottom guarantees that every URI receives a response, even if that response is a 404. An incomplete routing table — one that silently drops unmatched requests — is the HTTP equivalent of an Erlang process with no catch-all clause: a mailbox that grows until the node runs out of memory.

| Metric | Value | Notes |
| --- | --- | --- |
| Routes evaluated | 5 | All 5 patterns tested |
| Matches | 4 | Computed by Pattern.matches() |
| No-matches | 1 | /robots.txt intentional miss |
| Regex evaluation time | 248292 ns | 5 Pattern.matches() calls |
| sayPatternMatch() render | 89017 ns | Java 25.0.2 |

> [!NOTE]
> Pattern.matches() compiles and evaluates the regex in a single call. For a production router with hundreds of routes, pre-compiling patterns with Pattern.compile() and caching the compiled Pattern objects eliminates repeated compilation overhead. The measurement above includes compilation cost, which is the realistic worst-case for cold-path matching.

> [!WARNING]
> Route matching order matters. If the version-2 catch-all /api/v2/.* were placed first in the table, it would shadow all more-specific /api/v2/ routes defined later. This is the same ordering hazard that exists in Erlang receive blocks: a catch-all placed before specific patterns makes those patterns dead code. The routing table must be ordered from most-specific to least-specific.

---
*Generated by [DTR](http://www.dtr.org)*

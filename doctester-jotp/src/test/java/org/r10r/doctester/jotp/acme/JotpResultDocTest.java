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
package org.r10r.doctester.jotp.acme;

import org.acme.Result;
import org.junit.Test;
import org.r10r.doctester.DocTester;

import static org.hamcrest.CoreMatchers.is;

/**
 * DocTester documentation for {@link Result} — the JOTP (Java Open Telecom Protocol)
 * railway-oriented result type with both Erlang-style ({@code Ok}/{@code Err}) and
 * explicit ({@code Success}/{@code Failure}) naming conventions.
 *
 * <p>Sourced from <a href="https://github.com/seanchatmangpt/java-maven-template">
 * seanchatmangpt/java-maven-template</a> (git submodule at {@code jotp/}).
 *
 * <p>Covers: all four permitted record variants, both naming conventions, factory
 * methods ({@code ok}, {@code err}, {@code success}, {@code failure}, {@code of}),
 * predicates ({@code isSuccess}, {@code isError}/{@code isFailure}), and all
 * transformation methods ({@code map}, {@code flatMap}, {@code fold}, {@code orElse},
 * {@code orElseThrow}).
 */
public class JotpResultDocTest extends DocTester {

    // =========================================================================
    // Factory methods — Erlang-style (ok / err)
    // =========================================================================

    @Test
    public void testOkAndErrErlangStyle() {
        sayNextSection("Result.ok() / Result.err() — Erlang-Style Factory Methods");
        say("JOTP's <code>Result&lt;S,F&gt;</code> is the Java equivalent of Erlang's "
                + "tagged tuples. The Erlang-style factory names mirror the OTP convention "
                + "exactly:");
        sayRaw("<table class=\"table table-bordered table-sm\">"
                + "<tr><th>Erlang</th><th>Java (JOTP)</th></tr>"
                + "<tr><td><code>{ok, Value}</code></td><td><code>Result.ok(value)</code></td></tr>"
                + "<tr><td><code>{error, Reason}</code></td><td><code>Result.err(reason)</code></td></tr>"
                + "</table>");

        Result<Integer, String> success = Result.ok(42);
        Result<Integer, String> failure = Result.err("not-found");

        sayAndAssertThat("ok(42).isSuccess() is true",    true,  is(success.isSuccess()));
        sayAndAssertThat("ok(42).isError() is false",     false, is(success.isError()));
        sayAndAssertThat("err(...).isError() is true",    true,  is(failure.isError()));
        sayAndAssertThat("err(...).isFailure() is true",  true,  is(failure.isFailure()));
        sayAndAssertThat("err(...).isSuccess() is false", false, is(failure.isSuccess()));
    }

    @Test
    public void testSuccessAndFailureExplicitStyle() {
        sayNextSection("Result.success() / Result.failure() — Explicit Naming Convention");
        say("For readability in codebases that prefer English over Erlang convention, "
                + "JOTP provides <code>success()</code> and <code>failure()</code> aliases. "
                + "Both conventions are equivalent; all predicates and transformations "
                + "work identically on all four permitted record types.");

        Result<String, Integer> s = Result.success("data");
        Result<String, Integer> f = Result.failure(404);

        sayAndAssertThat("success(...).isSuccess() is true",   true,  is(s.isSuccess()));
        sayAndAssertThat("failure(...).isFailure() is true",   true,  is(f.isFailure()));
        sayAndAssertThat("failure(...).isError() is true",     true,  is(f.isError()));
    }

    // =========================================================================
    // Result.of — exception lifting
    // =========================================================================

    @Test
    public void testOfLiftingSupplier() {
        sayNextSection("Result.of() — Lifting Exception-Throwing Operations into Result");
        say("<code>Result.of(supplier)</code> bridges Java's exception-based APIs and "
                + "railway-oriented programming. Any exception thrown by the supplier "
                + "becomes an <code>Err</code>; a normal return becomes an <code>Ok</code>. "
                + "This is the OTP equivalent of wrapping a call inside a "
                + "<code>try/catch</code> and returning <code>{ok, V}</code> or <code>{error, E}</code>.");

        Result<Integer, Exception> parsed  = Result.of(() -> Integer.parseInt("99"));
        Result<Integer, Exception> invalid = Result.of(() -> Integer.parseInt("oops"));

        sayAndAssertThat("of(() -> parseInt('99')) is a Success",  true,  is(parsed.isSuccess()));
        sayAndAssertThat("orElse(-1) extracts 99 from the success", 99,   is(parsed.orElse(-1)));
        sayAndAssertThat("of(() -> parseInt('oops')) is a Failure", true,  is(invalid.isFailure()));
        sayAndAssertThat("orElse(-1) returns -1 for the failure",  -1,    is(invalid.orElse(-1)));
    }

    // =========================================================================
    // map — transform success value
    // =========================================================================

    @Test
    public void testMapOnOk() {
        sayNextSection("Result.map() — Transform the Success Value (Railway Map)");
        say("The <em>railway-oriented programming</em> metaphor: success and failure are two "
                + "parallel tracks. <code>map(f)</code> applies <code>f</code> on the success "
                + "track only — a failure short-circuits immediately, never calling <code>f</code>. "
                + "Works identically for all four record variants (<code>Ok</code>, "
                + "<code>Success</code>, <code>Err</code>, <code>Failure</code>).");

        Result<Integer, String> doubled = Result.<Integer, String>ok(21).map(n -> n * 2);
        Result<Integer, String> bypass  = Result.<Integer, String>err("x").map(n -> n * 2);

        sayAndAssertThat("ok(21).map(*2) = ok(42)", 42,     is(doubled.orElse(0)));
        sayAndAssertThat("err('x').map(*2) stays err", true, is(bypass.isError()));
    }

    @Test
    public void testMapChaining() {
        sayNextSection("Result.map() — Chaining Multiple Transformations");
        say("Multiple <code>map()</code> calls chain naturally. Each transformation "
                + "runs only if the previous step succeeded — failures short-circuit "
                + "the entire chain at the first failure point.");

        String result = Result.<String, Exception>ok("  hello world  ")
                .map(String::trim)
                .map(String::toUpperCase)
                .map(s -> "[" + s + "]")
                .orElse("failed");

        sayAndAssertThat("chained map: trim → upper → bracket",
                "[HELLO WORLD]", is(result));
    }

    // =========================================================================
    // flatMap — chain Result-returning operations
    // =========================================================================

    @Test
    public void testFlatMapChaining() {
        sayNextSection("Result.flatMap() — Chaining Result-Returning Steps");
        say("<code>flatMap(f)</code> is the monadic bind for <code>Result</code>. "
                + "Each step can independently succeed or fail; the chain short-circuits "
                + "on the first failure. This is Erlang's pattern of chaining "
                + "<code>case {ok, V} -> ...</code> without nested ifs.");

        Result<String, String> pipeline = Result.<Integer, String>ok(42)
                .flatMap(n -> n > 0
                        ? Result.ok(String.valueOf(n))
                        : Result.err("negative"))
                .flatMap(s -> s.length() > 0
                        ? Result.ok("Value: " + s)
                        : Result.err("empty"));

        sayAndAssertThat("flatMap pipeline succeeds: 'Value: 42'",
                "Value: 42", is(pipeline.orElse("")));
    }

    @Test
    public void testFlatMapShortCircuit() {
        sayNextSection("Result.flatMap() — Short-Circuit on First Failure");
        say("Once any step returns an error, all subsequent <code>flatMap</code> "
                + "and <code>map</code> calls are skipped. The final result is the "
                + "first failure encountered in the chain.");

        Result<String, String> result = Result.<Integer, String>ok(-5)
                .flatMap(n -> n > 0
                        ? Result.ok(String.valueOf(n))
                        : Result.err("must-be-positive"))
                .flatMap(s -> Result.ok("Value: " + s))    // never reached
                .map(s -> s.toUpperCase());                 // never reached

        sayAndAssertThat("chain short-circuits after first flatMap failure",
                true, is(result.isError()));
    }

    // =========================================================================
    // fold — exhaustive pattern match
    // =========================================================================

    @Test
    public void testFoldBothCases() {
        sayNextSection("Result.fold() — Exhaustive Elimination of Both Cases");
        say("<code>fold(onSuccess, onError)</code> is the terminating operation: "
                + "it handles both tracks with separate functions and collapses to a "
                + "single value of type <code>T</code>. Equivalent to Rust's <code>match</code> "
                + "or Haskell's <code>either</code>. Works on all four record variants.");

        Result<Integer, String> ok  = Result.ok(100);
        Result<Integer, String> err = Result.err("timeout");

        String okMsg  = ok.fold(v -> "Got " + v,    e -> "Error: " + e);
        String errMsg = err.fold(v -> "Got " + v,   e -> "Error: " + e);

        sayAndAssertThat("fold on Ok uses onSuccess handler",  "Got 100",       is(okMsg));
        sayAndAssertThat("fold on Err uses onError handler",   "Error: timeout", is(errMsg));
    }

    // =========================================================================
    // orElse / orElseThrow
    // =========================================================================

    @Test
    public void testOrElse() {
        sayNextSection("Result.orElse() — Default Value for Failure");
        say("<code>orElse(default)</code> extracts the success value or returns the "
                + "provided constant for any failure variant.");

        sayAndAssertThat("ok('real').orElse('default') → 'real'",
                "real", is(Result.<String, String>ok("real").orElse("default")));
        sayAndAssertThat("err('x').orElse('default') → 'default'",
                "default", is(Result.<String, String>err("x").orElse("default")));
        sayAndAssertThat("success('v').orElse('d') → 'v'",
                "v", is(Result.<String, String>success("v").orElse("d")));
        sayAndAssertThat("failure('x').orElse('d') → 'd'",
                "d", is(Result.<String, String>failure("x").orElse("d")));
    }

    @Test
    public void testOrElseThrow() {
        sayNextSection("Result.orElseThrow() — Unwrap or Propagate RuntimeException");
        say("<code>orElseThrow()</code> returns the success value directly. "
                + "For a failure, if the error is a <code>Throwable</code> it is wrapped "
                + "in a <code>RuntimeException</code> and thrown; otherwise the error's "
                + "<code>toString()</code> is used as the exception message.");

        String value = Result.<String, String>ok("safe").orElseThrow();
        sayAndAssertThat("ok('safe').orElseThrow() returns 'safe'", "safe", is(value));

        boolean threw = false;
        try {
            Result.<String, Exception>err(new IllegalStateException("bad")).orElseThrow();
        } catch (RuntimeException e) {
            threw = true;
        }
        sayAndAssertThat("err(exception).orElseThrow() throws RuntimeException", true, is(threw));
    }

    // =========================================================================
    // Sealed-interface pattern matching on all four variants
    // =========================================================================

    @Test
    public void testSwitchOnAllFourVariants() {
        sayNextSection("Exhaustive switch on Result — All Four Sealed Variants");
        say("Because <code>Result</code> is sealed with four permitted records "
                + "(<code>Ok</code>, <code>Err</code>, <code>Success</code>, <code>Failure</code>), "
                + "a switch expression must cover all four for exhaustiveness. "
                + "The compiler enforces this at compile time — no <code>default</code> needed.");
        sayRaw("<pre>switch (result) {\n"
                + "    case Result.Ok&lt;S,F&gt;(var v)      -&gt; ...\n"
                + "    case Result.Success&lt;S,F&gt;(var v)  -&gt; ...\n"
                + "    case Result.Err&lt;S,F&gt;(var e)     -&gt; ...\n"
                + "    case Result.Failure&lt;S,F&gt;(var e) -&gt; ...\n"
                + "}</pre>");

        Result<Integer, String> ok  = Result.ok(1);
        Result<Integer, String> sc  = Result.success(2);
        Result<Integer, String> err = Result.err("e1");
        Result<Integer, String> fl  = Result.failure("e2");

        String labelOk  = classify(ok);
        String labelSc  = classify(sc);
        String labelErr = classify(err);
        String labelFl  = classify(fl);

        sayAndAssertThat("ok(1) matches Ok arm",        "ok:1",      is(labelOk));
        sayAndAssertThat("success(2) matches Success arm", "success:2", is(labelSc));
        sayAndAssertThat("err('e1') matches Err arm",   "err:e1",    is(labelErr));
        sayAndAssertThat("failure('e2') matches Failure arm", "failure:e2", is(labelFl));
    }

    private static String classify(Result<Integer, String> r) {
        return switch (r) {
            case Result.Ok<Integer, String>(var v)      -> "ok:" + v;
            case Result.Success<Integer, String>(var v) -> "success:" + v;
            case Result.Err<Integer, String>(var e)     -> "err:" + e;
            case Result.Failure<Integer, String>(var e) -> "failure:" + e;
        };
    }
}

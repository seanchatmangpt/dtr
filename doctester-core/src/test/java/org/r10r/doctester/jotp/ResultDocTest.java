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
package org.r10r.doctester.jotp;

import org.junit.Test;
import org.r10r.doctester.DocTester;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * DocTester documentation for {@link Result} — a sealed interface that models
 * either a successful value ({@code Success<T>}) or a failure ({@code Failure<E>}).
 *
 * <p>Sourced from
 * <a href="https://github.com/cchacin/java-maven-template">cchacin/java-maven-template</a>.
 *
 * <p>Covers every Java 25 pattern used in {@code Result}:
 * <ul>
 *   <li>Sealed interfaces with {@code permits}</li>
 *   <li>Records as permitted subtypes</li>
 *   <li>Record deconstruction patterns in {@code switch} expressions</li>
 *   <li>Exhaustive {@code switch} (compiler verifies all cases are covered)</li>
 *   <li>{@code instanceof} with deconstruction ({@code peek} / {@code peekError})</li>
 *   <li>Unnamed pattern variable ({@code ignored}) in {@code orElse} / {@code orElseGet}</li>
 *   <li>Functional composition: {@code map}, {@code mapError}, {@code flatMap}, {@code fold}</li>
 *   <li>Error recovery: {@code recover}, {@code recoverWith}, {@code orElse},
 *       {@code orElseGet}, {@code orElseThrow}</li>
 *   <li>Side-effect hooks: {@code peek}, {@code peekError}</li>
 *   <li>{@code of()} — checked-exception lifting via {@link Result.ThrowingSupplier}</li>
 * </ul>
 */
public class ResultDocTest extends DocTester {

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    @Test
    public void testSuccessFactory() {
        sayNextSection("Result.success() — Creating a Successful Result");
        say("The static factory method <code>Result.success(value)</code> wraps any value "
                + "in a <code>Success</code> record. The resulting <code>Result</code> is "
                + "immutable and reports <code>isSuccess() == true</code>.");

        Result<String, Exception> result = Result.success("Hello, JOTP!");

        sayAndAssertThat("isSuccess() returns true for a Success",   true,  is(result.isSuccess()));
        sayAndAssertThat("isFailure() returns false for a Success",  false, is(result.isFailure()));
    }

    @Test
    public void testFailureFactory() {
        sayNextSection("Result.failure() — Creating a Failed Result");
        say("<code>Result.failure(error)</code> wraps an error in a <code>Failure</code> record. "
                + "The error type <code>E</code> can be any type — an exception, a string, "
                + "an enum, etc.");

        Result<String, String> result = Result.failure("Something went wrong");

        sayAndAssertThat("isFailure() returns true for a Failure",   true,  is(result.isFailure()));
        sayAndAssertThat("isSuccess() returns false for a Failure",  false, is(result.isSuccess()));
    }

    @Test
    public void testOfWithSuccess() {
        sayNextSection("Result.of() — Lifting a Checked-Exception Operation into Result");
        say("<code>Result.of(ThrowingSupplier)</code> executes a lambda that may throw a "
                + "checked exception. If it returns normally the value is wrapped in "
                + "<code>Success</code>; if it throws the exception is wrapped in "
                + "<code>Failure</code>. This bridges traditional Java exception handling "
                + "with functional error handling.");

        Result<Integer, Exception> success = Result.of(() -> Integer.parseInt("42"));

        sayAndAssertThat("Parsing '42' produces a Success",   true, is(success.isSuccess()));
        sayAndAssertThat("The parsed value is 42",            42,   is(success.orElse(-1)));
    }

    @Test
    public void testOfWithFailure() {
        sayNextSection("Result.of() — Catching Exceptions as Failure");
        say("When the <code>ThrowingSupplier</code> throws, <code>of()</code> catches it "
                + "and returns a <code>Failure</code> without propagating the exception.");

        Result<Integer, Exception> failure = Result.of(() -> Integer.parseInt("not-a-number"));

        sayAndAssertThat("Parsing 'not-a-number' produces a Failure", true,
                is(failure.isFailure()));
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    @Test
    public void testMapOnSuccess() {
        sayNextSection("Result.map() — Transforming a Success Value");
        say("<code>map(f)</code> applies function <code>f</code> to the value inside a "
                + "<code>Success</code>, returning a new <code>Success</code> with the "
                + "transformed value. It uses a record deconstruction pattern internally:");
        sayRaw("<pre>case Success&lt;T, E&gt;(var value) -&gt; new Success&lt;&gt;(mapper.apply(value));</pre>");

        Result<Integer, String> doubled = Result.<Integer, String>success(21)
                .map(n -> n * 2);

        sayAndAssertThat("map doubles 21 to 42",  42, is(doubled.orElse(0)));
        sayAndAssertThat("result is still a Success", true, is(doubled.isSuccess()));
    }

    @Test
    public void testMapPassesThroughFailure() {
        sayNextSection("Result.map() — Failure Passes Through Unchanged");
        say("When the <code>Result</code> is already a <code>Failure</code>, "
                + "<code>map()</code> skips the function entirely and returns the "
                + "same <code>Failure</code>. This is the short-circuit behaviour that "
                + "makes chaining safe.");

        Result<Integer, String> failure = Result.<Integer, String>failure("original error")
                .map(n -> n * 2);

        sayAndAssertThat("map on Failure leaves it as Failure", true,  is(failure.isFailure()));
    }

    @Test
    public void testMapError() {
        sayNextSection("Result.mapError() — Transforming a Failure's Error");
        say("<code>mapError(f)</code> applies <code>f</code> to the error inside a "
                + "<code>Failure</code>, leaving a <code>Success</code> untouched. "
                + "Useful for translating low-level errors into domain-specific types.");

        Result<String, String> mapped = Result.<String, Integer>failure(404)
                .mapError(code -> "HTTP Error " + code);

        String foldedError = mapped.<String>fold(v -> v, e -> e);
        sayAndAssertThat("mapError converts error int 404 to message",
                "HTTP Error 404",
                is(foldedError));

        Result<String, String> success = Result.<String, Integer>success("data")
                .mapError(code -> "HTTP Error " + code);

        sayAndAssertThat("mapError on Success leaves it unchanged", true, is(success.isSuccess()));
    }

    @Test
    public void testFlatMap() {
        sayNextSection("Result.flatMap() — Chaining Result-Returning Steps");
        say("<code>flatMap(f)</code> applies a function that itself returns a "
                + "<code>Result</code>, allowing sequential operations where any step "
                + "may fail. The chain short-circuits on the first <code>Failure</code>.");

        Result<String, String> result = Result.<Integer, String>success(42)
                .flatMap(n -> n > 0
                        ? Result.success(String.valueOf(n))
                        : Result.failure("must be positive"))
                .flatMap(s -> s.length() > 0
                        ? Result.success("Value: " + s)
                        : Result.failure("blank string"));

        sayAndAssertThat("chained flatMap on 42 produces final Success", true, is(result.isSuccess()));
        sayAndAssertThat("final value is 'Value: 42'", "Value: 42", is(result.orElse("")));
    }

    @Test
    public void testFlatMapShortCircuits() {
        sayNextSection("Result.flatMap() — Short-Circuit on First Failure");
        say("Once any step in a <code>flatMap</code> chain returns a <code>Failure</code>, "
                + "all subsequent functions are skipped.");

        Result<String, String> result = Result.<Integer, String>success(-1)
                .flatMap(n -> n > 0
                        ? Result.success(String.valueOf(n))
                        : Result.failure("must be positive"))
                .flatMap(s -> Result.success("Value: " + s));  // never reached

        sayAndAssertThat("flatMap short-circuits after first Failure", true, is(result.isFailure()));
    }

    // -------------------------------------------------------------------------
    // Extraction patterns
    // -------------------------------------------------------------------------

    @Test
    public void testFold() {
        sayNextSection("Result.fold() — Exhaustive Pattern Match over Both Cases");
        say("<code>fold(onSuccess, onFailure)</code> handles both <code>Success</code> and "
                + "<code>Failure</code> with separate functions, always returning a value "
                + "of type <code>U</code>. Internally it uses an exhaustive "
                + "<code>switch</code> expression over the sealed type — no "
                + "<code>default</code> branch is needed because the compiler knows all "
                + "permitted subtypes:");
        sayRaw("<pre>return switch (this) {\n"
                + "    case Success&lt;T, E&gt;(var value) -&gt; onSuccess.apply(value);\n"
                + "    case Failure&lt;T, E&gt;(var error) -&gt; onFailure.apply(error);\n"
                + "};</pre>");

        Result<Integer, String> success = Result.success(100);
        String successMsg = success.fold(
                value -> "Success: " + value,
                error -> "Failure: " + error);

        sayAndAssertThat("fold on Success uses the onSuccess handler",
                "Success: 100", is(successMsg));

        Result<Integer, String> failure = Result.failure("timeout");
        String failureMsg = failure.fold(
                value -> "Success: " + value,
                error -> "Failure: " + error);

        sayAndAssertThat("fold on Failure uses the onFailure handler",
                "Failure: timeout", is(failureMsg));
    }

    @Test
    public void testRecover() {
        sayNextSection("Result.recover() — Providing a Fallback Value from the Error");
        say("<code>recover(f)</code> returns the success value directly, or applies "
                + "<code>f</code> to the error to produce a fallback. "
                + "Always returns a plain <code>T</code>, never a <code>Result</code>.");

        Result<String, Integer> failure = Result.failure(503);
        String recovered = failure.recover(code -> "default-for-" + code);

        sayAndAssertThat("recover produces fallback 'default-for-503'",
                "default-for-503", is(recovered));

        Result<String, Integer> success = Result.success("real-value");
        String kept = success.recover(code -> "fallback");

        sayAndAssertThat("recover on Success returns the original value",
                "real-value", is(kept));
    }

    @Test
    public void testRecoverWith() {
        sayNextSection("Result.recoverWith() — Replacing a Failure with Another Result");
        say("<code>recoverWith(f)</code> allows a <code>Failure</code> to be replaced by "
                + "any <code>Result</code> — the replacement may itself succeed or fail. "
                + "A <code>Success</code> is returned as-is without invoking <code>f</code>.");

        Result<String, String> failure = Result.failure("network-error");
        Result<String, String> recovered = failure.recoverWith(err -> Result.success("cached-data"));

        sayAndAssertThat("recoverWith turns Failure into a new Success", true, is(recovered.isSuccess()));
        sayAndAssertThat("recovered value is 'cached-data'", "cached-data", is(recovered.orElse("")));

        Result<String, String> success = Result.success("original");
        Result<String, String> unchanged = success.recoverWith(err -> Result.success("never used"));

        sayAndAssertThat("recoverWith on Success returns original unchanged",
                "original", is(unchanged.orElse("")));
    }

    @Test
    public void testOrElse() {
        sayNextSection("Result.orElse() — Default Value for Failure");
        say("<code>orElse(default)</code> extracts the success value or returns the "
                + "provided constant if the <code>Result</code> is a <code>Failure</code>. "
                + "Internally uses the unnamed pattern <code>case Failure&lt;T,E&gt; ignored</code> "
                + "to discard the error.");

        Result<String, Exception> failure = Result.failure(new RuntimeException("oops"));
        sayAndAssertThat("orElse returns 'default' for a Failure",
                "default", is(failure.orElse("default")));

        Result<String, Exception> success = Result.success("actual");
        sayAndAssertThat("orElse returns the actual value for a Success",
                "actual", is(success.orElse("default")));
    }

    @Test
    public void testOrElseGet() {
        sayNextSection("Result.orElseGet() — Lazily Computed Default");
        say("<code>orElseGet(supplier)</code> is like <code>orElse</code> but accepts a "
                + "<code>Supplier</code>. The supplier is only evaluated when the "
                + "<code>Result</code> is a <code>Failure</code>, avoiding unnecessary "
                + "computation for expensive defaults.");

        Result<String, String> failure = Result.failure("err");
        String val = failure.orElseGet(() -> "computed-" + "default");

        sayAndAssertThat("orElseGet computes 'computed-default' lazily on Failure",
                "computed-default", is(val));

        Result<String, String> success = Result.success("fast-path");
        String fast = success.orElseGet(() -> { throw new AssertionError("should not run"); });

        sayAndAssertThat("orElseGet does NOT invoke the supplier for a Success",
                "fast-path", is(fast));
    }

    @Test
    public void testOrElseThrow() {
        sayNextSection("Result.orElseThrow() — Unwrap or Propagate Exception");
        say("<code>orElseThrow()</code> returns the success value or wraps the error in a "
                + "<code>RuntimeException</code> and throws it. "
                + "When the error is already a <code>Throwable</code> it is re-wrapped as "
                + "the cause; otherwise the error's <code>toString()</code> is used as the "
                + "exception message.");

        Result<String, Exception> success = Result.success("unwrapped");
        sayAndAssertThat("orElseThrow on Success returns the value",
                "unwrapped", is(success.orElseThrow()));

        Result<String, Exception> failure = Result.failure(new IllegalStateException("bad state"));
        boolean threw = false;
        try {
            failure.orElseThrow();
        } catch (RuntimeException e) {
            threw = true;
        }
        sayAndAssertThat("orElseThrow on Failure throws RuntimeException", true, is(threw));
    }

    // -------------------------------------------------------------------------
    // Side-effect hooks
    // -------------------------------------------------------------------------

    @Test
    public void testPeek() {
        sayNextSection("Result.peek() — Observing a Success Value (Side Effects)");
        say("<code>peek(consumer)</code> runs the consumer on the success value for side "
                + "effects (logging, metrics, debugging). It returns <code>this</code> so "
                + "it can be inserted into a pipeline without breaking the chain. "
                + "Internally it uses <code>instanceof</code> with a deconstruction pattern:");
        sayRaw("<pre>if (this instanceof Success&lt;T, E&gt;(var value)) {\n"
                + "    consumer.accept(value);\n"
                + "}</pre>");

        var observed = new AtomicReference<String>();
        Result<String, String> result = Result.<String, String>success("peek-me")
                .peek(observed::set);

        sayAndAssertThat("peek captures the success value", "peek-me", is(observed.get()));
        sayAndAssertThat("peek returns the original Result unchanged", true, is(result.isSuccess()));
    }

    @Test
    public void testPeekDoesNothingOnFailure() {
        sayNextSection("Result.peek() — No-Op on Failure");
        say("When the <code>Result</code> is a <code>Failure</code>, <code>peek()</code> "
                + "does nothing and the consumer is never called.");

        var observed = new AtomicReference<String>("untouched");
        Result<String, String> failure = Result.<String, String>failure("err")
                .peek(observed::set);

        sayAndAssertThat("peek on Failure does not invoke the consumer",
                "untouched", is(observed.get()));
        sayAndAssertThat("peek on Failure returns the original Failure", true, is(failure.isFailure()));
    }

    @Test
    public void testPeekError() {
        sayNextSection("Result.peekError() — Observing an Error Value (Side Effects)");
        say("<code>peekError(consumer)</code> mirrors <code>peek</code> but fires on "
                + "<code>Failure</code>. Useful for logging errors without short-circuiting "
                + "the pipeline. Uses <code>instanceof</code> with a deconstruction pattern:");
        sayRaw("<pre>if (this instanceof Failure&lt;T, E&gt;(var error)) {\n"
                + "    consumer.accept(error);\n"
                + "}</pre>");

        var observed = new AtomicReference<String>();
        Result<String, String> result = Result.<String, String>failure("the-error")
                .peekError(observed::set);

        sayAndAssertThat("peekError captures the error value", "the-error", is(observed.get()));
        sayAndAssertThat("peekError returns the original Result unchanged", true, is(result.isFailure()));
    }

    @Test
    public void testPeekErrorDoesNothingOnSuccess() {
        sayNextSection("Result.peekError() — No-Op on Success");
        say("When the <code>Result</code> is a <code>Success</code>, "
                + "<code>peekError()</code> does nothing and the consumer is never called.");

        var observed = new AtomicReference<String>("untouched");
        Result<String, String> success = Result.<String, String>success("ok")
                .peekError(observed::set);

        sayAndAssertThat("peekError on Success does not invoke the consumer",
                "untouched", is(observed.get()));
    }

    // -------------------------------------------------------------------------
    // Sealed interface + record deconstruction patterns (Java 25)
    // -------------------------------------------------------------------------

    @Test
    public void testSealedSwitchWithDeconstructionPatterns() {
        sayNextSection("Sealed Interface Switch — Exhaustive Deconstruction Patterns");
        say("Because <code>Result</code> is <code>sealed permits Success, Failure</code>, "
                + "a <code>switch</code> expression with both cases is exhaustive — the "
                + "compiler rejects any non-exhaustive switch at compile time. "
                + "Record deconstruction binds the inner component directly:");
        sayRaw("<pre>String label = switch (result) {\n"
                + "    case Result.Success(var v) -&gt; \"Got \" + v;\n"
                + "    case Result.Failure(var e) -&gt; \"Error: \" + e;\n"
                + "};</pre>");

        Result<Integer, String> success = Result.success(42);
        String successLabel = switch (success) {
            case Result.Success(var v) -> "Got " + v;
            case Result.Failure(var e) -> "Error: " + e;
        };
        sayAndAssertThat("Switch on Success matches Success arm", "Got 42", is(successLabel));

        Result<Integer, String> failure = Result.failure("not found");
        String failureLabel = switch (failure) {
            case Result.Success(var v) -> "Got " + v;
            case Result.Failure(var e) -> "Error: " + e;
        };
        sayAndAssertThat("Switch on Failure matches Failure arm", "Error: not found", is(failureLabel));
    }

    @Test
    public void testInstanceofDeconstructionPattern() {
        sayNextSection("instanceof with Deconstruction — Java 25 Pattern Matching");
        say("<code>instanceof</code> can be combined with record deconstruction to "
                + "simultaneously test the type and bind the inner component in one step, "
                + "without an explicit cast. This is the pattern used by "
                + "<code>peek()</code> and <code>peekError()</code>.");

        Result<String, String> result = Result.success("deconstructed");

        boolean bound = false;
        if (result instanceof Result.Success(var value)) {
            bound = value.equals("deconstructed");
        }

        sayAndAssertThat("instanceof deconstruction binds the inner value directly",
                true, is(bound));
    }

    // -------------------------------------------------------------------------
    // Composing operations — full pipeline
    // -------------------------------------------------------------------------

    @Test
    public void testFullPipeline() {
        sayNextSection("Full Result Pipeline — of() → map() → flatMap() → fold()");
        say("All operations compose naturally. The pipeline below reads a value with "
                + "<code>of()</code>, normalises it with <code>map()</code>, validates it "
                + "with <code>flatMap()</code>, and collapses it to a <code>String</code> "
                + "with <code>fold()</code>. Each step is documented in prior sections.");

        String outcome = Result.<String, Exception>of(() -> "  hello world  ")
                .map(String::trim)
                .map(String::toUpperCase)
                .flatMap(s -> s.isEmpty()
                        ? Result.failure(new IllegalStateException("blank input"))
                        : Result.success(s))
                .fold(
                        value -> "Processed: " + value,
                        err   -> "Failed: " + err.getMessage());

        sayAndAssertThat("Pipeline trims, upper-cases, and labels '  hello world  '",
                "Processed: HELLO WORLD", is(outcome));
    }

    @Test
    public void testPipelineWithFailureShortCircuit() {
        sayNextSection("Full Pipeline — Short-Circuit on Failure");
        say("If any step in the pipeline fails, subsequent <code>map</code> / "
                + "<code>flatMap</code> calls are skipped and <code>fold</code> "
                + "dispatches to the <code>onFailure</code> handler.");

        String outcome = Result.<String, Exception>of(() -> "   ")
                .map(String::trim)
                .flatMap(s -> s.isEmpty()
                        ? Result.failure(new IllegalStateException("blank input"))
                        : Result.success(s))
                .map(String::toUpperCase)          // skipped — already Failure
                .fold(
                        value -> "Processed: " + value,
                        err   -> "Failed: " + err.getMessage());

        sayAndAssertThat("Pipeline short-circuits on blank input to 'Failed: blank input'",
                "Failed: blank input", is(outcome));
    }
}

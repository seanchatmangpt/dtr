# io.github.seanchatmangpt.dtr.jotp.ResultDocTest

## Table of Contents

- [Result.success() — Creating a Successful Result](#resultsuccesscreatingasuccessfulresult)
- [Result.peekError() — Observing an Error Value (Side Effects)](#resultpeekerrorobservinganerrorvaluesideeffects)
- [Result.orElseGet() — Lazily Computed Default](#resultorelsegetlazilycomputeddefault)
- [Result.flatMap() — Chaining Result-Returning Steps](#resultflatmapchainingresultreturningsteps)
- [Result.fold() — Exhaustive Pattern Match over Both Cases](#resultfoldexhaustivepatternmatchoverbothcases)
- [Result.peek() — Observing a Success Value (Side Effects)](#resultpeekobservingasuccessvaluesideeffects)
- [Result.peekError() — No-Op on Success](#resultpeekerrornooponsuccess)
- [Result.failure() — Creating a Failed Result](#resultfailurecreatingafailedresult)
- [Sealed Interface Switch — Exhaustive Deconstruction Patterns](#sealedinterfaceswitchexhaustivedeconstructionpatterns)
- [Full Result Pipeline — of() → map() → flatMap() → fold()](#fullresultpipelineofmapflatmapfold)
- [Result.map() — Failure Passes Through Unchanged](#resultmapfailurepassesthroughunchanged)
- [Result.recoverWith() — Replacing a Failure with Another Result](#resultrecoverwithreplacingafailurewithanotherresult)
- [Result.of() — Lifting a Checked-Exception Operation into Result](#resultofliftingacheckedexceptionoperationintoresult)
- [Result.flatMap() — Short-Circuit on First Failure](#resultflatmapshortcircuitonfirstfailure)
- [Result.map() — Transforming a Success Value](#resultmaptransformingasuccessvalue)
- [Result.recover() — Providing a Fallback Value from the Error](#resultrecoverprovidingafallbackvaluefromtheerror)
- [Result.peek() — No-Op on Failure](#resultpeeknooponfailure)
- [Result.mapError() — Transforming a Failure's Error](#resultmaperrortransformingafailureserror)
- [Result.of() — Catching Exceptions as Failure](#resultofcatchingexceptionsasfailure)
- [instanceof with Deconstruction — Java 26 Pattern Matching](#instanceofwithdeconstructionjava26patternmatching)
- [Result.orElse() — Default Value for Failure](#resultorelsedefaultvalueforfailure)
- [Result.orElseThrow() — Unwrap or Propagate Exception](#resultorelsethrowunwraporpropagateexception)
- [Full Pipeline — Short-Circuit on Failure](#fullpipelineshortcircuitonfailure)


## Result.success() — Creating a Successful Result

The static factory method <code>Result.success(value)</code> wraps any value in a <code>Success</code> record. The resulting <code>Result</code> is immutable and reports <code>isSuccess() == true</code>.

| Check | Result |
| --- | --- |
| isSuccess() returns true for a Success | `✓ PASS` |

| Check | Result |
| --- | --- |
| isFailure() returns false for a Success | `✓ PASS` |

## Result.peekError() — Observing an Error Value (Side Effects)

<code>peekError(consumer)</code> mirrors <code>peek</code> but fires on <code>Failure</code>. Useful for logging errors without short-circuiting the pipeline. Uses <code>instanceof</code> with a deconstruction pattern:
<pre>if (this instanceof Failure&lt;T, E&gt;(var error)) {
    consumer.accept(error);
}</pre>

| Check | Result |
| --- | --- |
| peekError captures the error value | `✓ PASS` |

| Check | Result |
| --- | --- |
| peekError returns the original Result unchanged | `✓ PASS` |

## Result.orElseGet() — Lazily Computed Default

<code>orElseGet(supplier)</code> is like <code>orElse</code> but accepts a <code>Supplier</code>. The supplier is only evaluated when the <code>Result</code> is a <code>Failure</code>, avoiding unnecessary computation for expensive defaults.

| Check | Result |
| --- | --- |
| orElseGet computes 'computed-default' lazily on Failure | `✓ PASS` |

| Check | Result |
| --- | --- |
| orElseGet does NOT invoke the supplier for a Success | `✓ PASS` |

## Result.flatMap() — Chaining Result-Returning Steps

<code>flatMap(f)</code> applies a function that itself returns a <code>Result</code>, allowing sequential operations where any step may fail. The chain short-circuits on the first <code>Failure</code>.

| Check | Result |
| --- | --- |
| chained flatMap on 42 produces final Success | `✓ PASS` |

| Check | Result |
| --- | --- |
| final value is 'Value: 42' | `✓ PASS` |

## Result.fold() — Exhaustive Pattern Match over Both Cases

<code>fold(onSuccess, onFailure)</code> handles both <code>Success</code> and <code>Failure</code> with separate functions, always returning a value of type <code>U</code>. Internally it uses an exhaustive <code>switch</code> expression over the sealed type — no <code>default</code> branch is needed because the compiler knows all permitted subtypes:
<pre>return switch (this) {
    case Success&lt;T, E&gt;(var value) -&gt; onSuccess.apply(value);
    case Failure&lt;T, E&gt;(var error) -&gt; onFailure.apply(error);
};</pre>

| Check | Result |
| --- | --- |
| fold on Success uses the onSuccess handler | `✓ PASS` |

| Check | Result |
| --- | --- |
| fold on Failure uses the onFailure handler | `✓ PASS` |

## Result.peek() — Observing a Success Value (Side Effects)

<code>peek(consumer)</code> runs the consumer on the success value for side effects (logging, metrics, debugging). It returns <code>this</code> so it can be inserted into a pipeline without breaking the chain. Internally it uses <code>instanceof</code> with a deconstruction pattern:
<pre>if (this instanceof Success&lt;T, E&gt;(var value)) {
    consumer.accept(value);
}</pre>

| Check | Result |
| --- | --- |
| peek captures the success value | `✓ PASS` |

| Check | Result |
| --- | --- |
| peek returns the original Result unchanged | `✓ PASS` |

## Result.peekError() — No-Op on Success

When the <code>Result</code> is a <code>Success</code>, <code>peekError()</code> does nothing and the consumer is never called.

| Check | Result |
| --- | --- |
| peekError on Success does not invoke the consumer | `✓ PASS` |

## Result.failure() — Creating a Failed Result

<code>Result.failure(error)</code> wraps an error in a <code>Failure</code> record. The error type <code>E</code> can be any type — an exception, a string, an enum, etc.

| Check | Result |
| --- | --- |
| isFailure() returns true for a Failure | `✓ PASS` |

| Check | Result |
| --- | --- |
| isSuccess() returns false for a Failure | `✓ PASS` |

## Sealed Interface Switch — Exhaustive Deconstruction Patterns

Because <code>Result</code> is <code>sealed permits Success, Failure</code>, a <code>switch</code> expression with both cases is exhaustive — the compiler rejects any non-exhaustive switch at compile time. Record deconstruction binds the inner component directly:
<pre>String label = switch (result) {
    case Result.Success(var v) -&gt; "Got " + v;
    case Result.Failure(var e) -&gt; "Error: " + e;
};</pre>

| Check | Result |
| --- | --- |
| Switch on Success matches Success arm | `✓ PASS` |

| Check | Result |
| --- | --- |
| Switch on Failure matches Failure arm | `✓ PASS` |

## Full Result Pipeline — of() → map() → flatMap() → fold()

All operations compose naturally. The pipeline below reads a value with <code>of()</code>, normalises it with <code>map()</code>, validates it with <code>flatMap()</code>, and collapses it to a <code>String</code> with <code>fold()</code>. Each step is documented in prior sections.

| Check | Result |
| --- | --- |
| Pipeline trims, upper-cases, and labels '  hello world  ' | `✓ PASS` |

## Result.map() — Failure Passes Through Unchanged

When the <code>Result</code> is already a <code>Failure</code>, <code>map()</code> skips the function entirely and returns the same <code>Failure</code>. This is the short-circuit behaviour that makes chaining safe.

| Check | Result |
| --- | --- |
| map on Failure leaves it as Failure | `✓ PASS` |

## Result.recoverWith() — Replacing a Failure with Another Result

<code>recoverWith(f)</code> allows a <code>Failure</code> to be replaced by any <code>Result</code> — the replacement may itself succeed or fail. A <code>Success</code> is returned as-is without invoking <code>f</code>.

| Check | Result |
| --- | --- |
| recoverWith turns Failure into a new Success | `✓ PASS` |

| Check | Result |
| --- | --- |
| recovered value is 'cached-data' | `✓ PASS` |

| Check | Result |
| --- | --- |
| recoverWith on Success returns original unchanged | `✓ PASS` |

## Result.of() — Lifting a Checked-Exception Operation into Result

<code>Result.of(ThrowingSupplier)</code> executes a lambda that may throw a checked exception. If it returns normally the value is wrapped in <code>Success</code>; if it throws the exception is wrapped in <code>Failure</code>. This bridges traditional Java exception handling with functional error handling.

| Check | Result |
| --- | --- |
| Parsing '42' produces a Success | `✓ PASS` |

| Check | Result |
| --- | --- |
| The parsed value is 42 | `✓ PASS` |

## Result.flatMap() — Short-Circuit on First Failure

Once any step in a <code>flatMap</code> chain returns a <code>Failure</code>, all subsequent functions are skipped.

| Check | Result |
| --- | --- |
| flatMap short-circuits after first Failure | `✓ PASS` |

## Result.map() — Transforming a Success Value

<code>map(f)</code> applies function <code>f</code> to the value inside a <code>Success</code>, returning a new <code>Success</code> with the transformed value. It uses a record deconstruction pattern internally:
<pre>case Success&lt;T, E&gt;(var value) -&gt; new Success&lt;&gt;(mapper.apply(value));</pre>

| Check | Result |
| --- | --- |
| map doubles 21 to 42 | `✓ PASS` |

| Check | Result |
| --- | --- |
| result is still a Success | `✓ PASS` |

## Result.recover() — Providing a Fallback Value from the Error

<code>recover(f)</code> returns the success value directly, or applies <code>f</code> to the error to produce a fallback. Always returns a plain <code>T</code>, never a <code>Result</code>.

| Check | Result |
| --- | --- |
| recover produces fallback 'default-for-503' | `✓ PASS` |

| Check | Result |
| --- | --- |
| recover on Success returns the original value | `✓ PASS` |

## Result.peek() — No-Op on Failure

When the <code>Result</code> is a <code>Failure</code>, <code>peek()</code> does nothing and the consumer is never called.

| Check | Result |
| --- | --- |
| peek on Failure does not invoke the consumer | `✓ PASS` |

| Check | Result |
| --- | --- |
| peek on Failure returns the original Failure | `✓ PASS` |

## Result.mapError() — Transforming a Failure's Error

<code>mapError(f)</code> applies <code>f</code> to the error inside a <code>Failure</code>, leaving a <code>Success</code> untouched. Useful for translating low-level errors into domain-specific types.

| Check | Result |
| --- | --- |
| mapError converts error int 404 to message | `✓ PASS` |

| Check | Result |
| --- | --- |
| mapError on Success leaves it unchanged | `✓ PASS` |

## Result.of() — Catching Exceptions as Failure

When the <code>ThrowingSupplier</code> throws, <code>of()</code> catches it and returns a <code>Failure</code> without propagating the exception.

| Check | Result |
| --- | --- |
| Parsing 'not-a-number' produces a Failure | `✓ PASS` |

## instanceof with Deconstruction — Java 26 Pattern Matching

<code>instanceof</code> can be combined with record deconstruction to simultaneously test the type and bind the inner component in one step, without an explicit cast. This is the pattern used by <code>peek()</code> and <code>peekError()</code>.

| Check | Result |
| --- | --- |
| instanceof deconstruction binds the inner value directly | `✓ PASS` |

## Result.orElse() — Default Value for Failure

<code>orElse(default)</code> extracts the success value or returns the provided constant if the <code>Result</code> is a <code>Failure</code>. Internally uses the unnamed pattern <code>case Failure&lt;T,E&gt; ignored</code> to discard the error.

| Check | Result |
| --- | --- |
| orElse returns 'default' for a Failure | `✓ PASS` |

| Check | Result |
| --- | --- |
| orElse returns the actual value for a Success | `✓ PASS` |

## Result.orElseThrow() — Unwrap or Propagate Exception

<code>orElseThrow()</code> returns the success value or wraps the error in a <code>RuntimeException</code> and throws it. When the error is already a <code>Throwable</code> it is re-wrapped as the cause; otherwise the error's <code>toString()</code> is used as the exception message.

| Check | Result |
| --- | --- |
| orElseThrow on Success returns the value | `✓ PASS` |

| Check | Result |
| --- | --- |
| orElseThrow on Failure throws RuntimeException | `✓ PASS` |

## Full Pipeline — Short-Circuit on Failure

If any step in the pipeline fails, subsequent <code>map</code> / <code>flatMap</code> calls are skipped and <code>fold</code> dispatches to the <code>onFailure</code> handler.

| Check | Result |
| --- | --- |
| Pipeline short-circuits on blank input to 'Failed: blank input' | `✓ PASS` |

---
*Generated by [DTR](http://www.dtr.org)*

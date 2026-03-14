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
package io.github.seanchatmangpt.dtr.jotp;

import org.junit.Test;
import io.github.seanchatmangpt.dtr.DtrTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

/**
 * DTR documentation for {@link Maths} — a class that showcases Java 26
 * <em>records</em> as concise, immutable containers for primitive values.
 *
 * <p>Covers:
 * <ul>
 *   <li>Record instantiation with {@code int} primitives</li>
 *   <li>Auto-generated accessors ({@code x()}, {@code y()}, {@code result()})</li>
 *   <li>Structural equality via auto-generated {@code equals()}</li>
 *   <li>Readable {@code toString()} from auto-generated implementation</li>
 *   <li>The {@code sum()} method widening {@code int} → {@code long}</li>
 *   <li>Edge cases: zero, negatives, overflow boundary</li>
 * </ul>
 */
public class MathsDocTest extends DtrTest {

    // -------------------------------------------------------------------------
    // Records — instantiation and accessors
    // -------------------------------------------------------------------------

    @Test
    public void testRecordInstantiationAndAccessors() {
        sayNextSection("Maths.Input — Record Instantiation and Primitive Accessors");
        say("A Java record is a concise immutable data class. The compiler auto-generates "
                + "a canonical constructor, accessor methods named after each component, "
                + "plus <code>equals()</code>, <code>hashCode()</code>, and <code>toString()</code>.");
        say("Creating <code>new Maths.Input(3, 4)</code> stores two <code>int</code> primitives.");

        var input = new Maths.Input(3, 4);

        sayAndAssertThat("input.x() returns the first int component: 3",   3, is(input.x()));
        sayAndAssertThat("input.y() returns the second int component: 4",  4, is(input.y()));
    }

    @Test
    public void testOutputRecordAccessor() {
        sayNextSection("Maths.Output — Record with a long Primitive");
        say("<code>Maths.Output</code> holds a single <code>long</code> field. "
                + "Storing the sum as <code>long</code> prevents overflow when both "
                + "<code>int</code> operands are at their maximum values.");

        var output = new Maths.Output(42L);

        sayAndAssertThat("output.result() returns the long component: 42", 42L, is(output.result()));
    }

    // -------------------------------------------------------------------------
    // Records — structural equality
    // -------------------------------------------------------------------------

    @Test
    public void testRecordStructuralEquality() {
        sayNextSection("Record Value Equality — equals() and hashCode()");
        say("Two records are equal when all their components are equal. "
                + "This is value equality (structural), not reference equality.");

        var a = new Maths.Input(5, 7);
        var b = new Maths.Input(5, 7);
        var c = new Maths.Input(5, 8);

        sayAndAssertThat("Input(5,7).equals(Input(5,7)) is true",  a, is(b));
        sayAndAssertThat("Input(5,7).equals(Input(5,8)) is false", a, is(not(c)));
        sayAndAssertThat("hashCode() is consistent with equals()",
                a.hashCode(), is(b.hashCode()));
    }

    @Test
    public void testOutputRecordEquality() {
        sayNextSection("Output Record Equality");
        say("Output records are also compared by value.");

        var out1 = new Maths.Output(100L);
        var out2 = new Maths.Output(100L);
        var out3 = new Maths.Output(200L);

        sayAndAssertThat("Output(100).equals(Output(100)) is true",  out1, is(out2));
        sayAndAssertThat("Output(100).equals(Output(200)) is false", out1, is(not(out3)));
    }

    // -------------------------------------------------------------------------
    // Records — toString
    // -------------------------------------------------------------------------

    @Test
    public void testRecordToString() {
        sayNextSection("Record toString() — Human-Readable Representation");
        say("The auto-generated <code>toString()</code> includes the record name and all "
                + "component names and values, which is useful for logging and debugging.");

        var input  = new Maths.Input(1, 2);
        var output = new Maths.Output(3L);

        sayAndAssertThat("Input(1,2).toString() contains component values",
                true, is(input.toString().contains("1") && input.toString().contains("2")));
        sayAndAssertThat("Output(3).toString() contains result value",
                true, is(output.toString().contains("3")));
    }

    // -------------------------------------------------------------------------
    // sum() — basic operations
    // -------------------------------------------------------------------------

    @Test
    public void testSumPositiveIntegers() {
        sayNextSection("Maths.sum() — Adding Two Positive int Primitives");
        say("<code>sum()</code> accepts a <code>Maths.Input</code> record and returns a "
                + "<code>Maths.Output</code> record whose <code>result</code> is the arithmetic "
                + "sum of the two <code>int</code> components, widened to <code>long</code>.");

        var maths = new Maths();

        sayAndAssertThat("sum(10, 32) = 42", 42L,
                is(maths.sum(new Maths.Input(10, 32)).result()));
        sayAndAssertThat("sum(1, 1) = 2",     2L,
                is(maths.sum(new Maths.Input(1, 1)).result()));
        sayAndAssertThat("sum(100, 900) = 1000", 1000L,
                is(maths.sum(new Maths.Input(100, 900)).result()));
    }

    // -------------------------------------------------------------------------
    // sum() — edge cases with primitive boundaries
    // -------------------------------------------------------------------------

    @Test
    public void testSumWithZero() {
        sayNextSection("Maths.sum() — Zero as an Identity Element");
        say("Adding zero to any integer returns that integer unchanged. "
                + "Both <code>sum(0, n)</code> and <code>sum(n, 0)</code> yield <code>n</code>.");

        var maths = new Maths();

        sayAndAssertThat("sum(0, 0) = 0", 0L,
                is(maths.sum(new Maths.Input(0, 0)).result()));
        sayAndAssertThat("sum(0, 7) = 7", 7L,
                is(maths.sum(new Maths.Input(0, 7)).result()));
        sayAndAssertThat("sum(7, 0) = 7", 7L,
                is(maths.sum(new Maths.Input(7, 0)).result()));
    }

    @Test
    public void testSumWithNegativeIntegers() {
        sayNextSection("Maths.sum() — Negative int Primitives");
        say("Records happily store negative <code>int</code> values. "
                + "Adding a negative is equivalent to subtraction.");

        var maths = new Maths();

        sayAndAssertThat("sum(-5, 3) = -2",   -2L,
                is(maths.sum(new Maths.Input(-5, 3)).result()));
        sayAndAssertThat("sum(-10, -10) = -20", -20L,
                is(maths.sum(new Maths.Input(-10, -10)).result()));
        sayAndAssertThat("sum(-1, 1) = 0",      0L,
                is(maths.sum(new Maths.Input(-1, 1)).result()));
    }

    @Test
    public void testSumAtIntegerOverflowBoundary() {
        sayNextSection("Maths.sum() — int Overflow Avoided by long Result");
        say("Because <code>Output.result</code> is a <code>long</code>, adding two "
                + "<code>int</code> values near <code>Integer.MAX_VALUE</code> does "
                + "<em>not</em> overflow — the addition is widened before overflow can occur.");

        var maths = new Maths();
        long expected = (long) Integer.MAX_VALUE + 1L;

        sayAndAssertThat(
                "sum(Integer.MAX_VALUE, 1) = " + expected + " (no overflow)",
                expected,
                is(maths.sum(new Maths.Input(Integer.MAX_VALUE, 1)).result()));

        long expectedMin = (long) Integer.MIN_VALUE - 1L;
        sayAndAssertThat(
                "sum(Integer.MIN_VALUE, -1) = " + expectedMin + " (no underflow)",
                expectedMin,
                is(maths.sum(new Maths.Input(Integer.MIN_VALUE, -1)).result()));
    }

    @Test
    public void testSumSymmetry() {
        sayNextSection("Maths.sum() — Commutativity");
        say("Addition is commutative: <code>sum(x, y) == sum(y, x)</code>. "
                + "This mirrors the mathematical property of the <code>+</code> operator.");

        var maths = new Maths();

        long ab = maths.sum(new Maths.Input(13, 29)).result();
        long ba = maths.sum(new Maths.Input(29, 13)).result();

        sayAndAssertThat("sum(13, 29) equals sum(29, 13)", ab, is(ba));
    }
}

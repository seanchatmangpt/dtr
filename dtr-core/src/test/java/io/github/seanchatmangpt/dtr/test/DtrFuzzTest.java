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
package io.github.seanchatmangpt.dtr;

import net.jqwik.api.*;
import net.jqwik.api.constraints.CharRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.testbrowser.Url;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fuzz / mutation tests using <a href="https://jqwik.net">jqwik</a> with
 * adversarial input generation.
 *
 * <p>jqwik generates random inputs, shrinks failures, and can simulate broad
 * fuzzing scenarios without a coverage-guided engine.  Each {@code @Property}
 * runs with a high {@code tries} count and uses the widest possible character
 * ranges (including surrogates, null characters, BOM, RTL marks, etc.) to
 * exercise edge cases that hand-crafted unit tests miss.
 *
 * <p>For coverage-guided fuzzing campaigns, run with
 * <a href="https://github.com/CodeIntelligenceTesting/jazzer">Jazzer</a>
 * or <a href="https://github.com/rohanpadhye/JQF">JQF</a> as an external
 * tool (these require a separate JVM agent and are not wired into the normal
 * {@code mvnd test} lifecycle).
 *
 * <h2>Input domains fuzzed</h2>
 * <ol>
 *   <li><b>Full Unicode + surrogates</b> — characters 0x0000–0xFFFF including
 *       surrogates, control characters, BOM, ZWNJ, RTL override, etc.</li>
 *   <li><b>High-codepoint strings</b> — supplementary plane characters
 *       (emoji, mathematical, CJK extension) decoded from byte arrays.</li>
 *   <li><b>Structured adversarial strings</b> — SQL injection, XSS payloads,
 *       format strings, null bytes embedded in strings.</li>
 *   <li><b>Long strings</b> — up to 100 000 characters to test buffer
 *       handling and memory behaviour.</li>
 * </ol>
 */
@Label("DTR — fuzz tests (jqwik adversarial generators)")
class DtrFuzzTest {

    private RenderMachineImpl rm;

    @BeforeProperty
    void freshRenderMachine() {
        rm = new RenderMachineImpl();
        rm.setFileName("DtrFuzzTest");
    }

    // =========================================================================
    // Arbitraries — adversarial input generators
    // =========================================================================

    /** Full Unicode range 0x0000–0xFFFF (includes surrogates, control chars). */
    @Provide
    Arbitrary<String> fullUnicode() {
        return Arbitraries.strings()
                .withCharRange('\u0000', '\uFFFF')
                .ofMinLength(0)
                .ofMaxLength(500);
    }

    /** Very long strings to stress buffering. */
    @Provide
    Arbitrary<String> longStrings() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(1000)
                .ofMaxLength(100_000);
    }

    /** Known-adversarial string payloads. */
    @Provide
    Arbitrary<String> adversarialStrings() {
        return Arbitraries.of(
                "",
                "\u0000",                                     // null byte
                "\uFFFE\uFEFF",                               // BOM markers
                "\u202E",                                     // RTL override
                "<script>alert('xss')</script>",              // XSS
                "'; DROP TABLE users; --",                    // SQL injection
                "%s %s %s %s %n",                             // format string
                "\r\n\r\nHTTP/1.1 200 OK\r\n",               // HTTP response splitting
                "A".repeat(100_000),                          // very long single line
                "\uD800\uDFFF",                               // surrogate pair (emoji range)
                "</div></div></body></html>",                  // HTML close tags
                "{{7*7}}${7*7}#{7*7}",                        // template injection
                "\t\n\r\f\u000B",                             // all whitespace types
                "\uD83D\uDE80\uD83C\uDF1F",                   // emoji (astral plane)
                "日本語中文한국어عربي"                    // multilingual
        );
    }

    /** Random raw bytes decoded as UTF-8 (may produce replacement characters). */
    @Provide
    Arbitrary<String> randomBytesAsString() {
        return Arbitraries.bytes()
                .array(byte[].class)
                .ofMinSize(0)
                .ofMaxSize(1024)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    // =========================================================================
    // 1. say() — fuzz with all adversarial generators
    // =========================================================================

    @Property(tries = 2000)
    @Label("say() never throws for full Unicode range")
    void fuzzSayWithFullUnicode(@ForAll("fullUnicode") String text) {
        assertDoesNotThrow(() -> rm.say(text));
    }

    @Property(tries = 50)
    @Label("say() never throws for long strings")
    void fuzzSayWithLongStrings(@ForAll("longStrings") String text) {
        assertDoesNotThrow(() -> rm.say(text));
    }

    @Property
    @Label("say() never throws for adversarial payloads")
    void fuzzSayWithAdversarialStrings(@ForAll("adversarialStrings") String text) {
        assertDoesNotThrow(() -> rm.say(text));
    }

    @Property(tries = 500)
    @Label("say() never throws for random bytes decoded as UTF-8")
    void fuzzSayWithRandomBytes(@ForAll("randomBytesAsString") String text) {
        assertDoesNotThrow(() -> rm.say(text));
    }

    // =========================================================================
    // 2. sayNextSection() — same adversarial generators
    // =========================================================================

    @Property(tries = 2000)
    @Label("sayNextSection() never throws for full Unicode range")
    void fuzzSayNextSectionWithFullUnicode(@ForAll("fullUnicode") String text) {
        assertDoesNotThrow(() -> rm.sayNextSection(text));
    }

    @Property
    @Label("sayNextSection() never throws for adversarial payloads")
    void fuzzSayNextSectionWithAdversarialStrings(@ForAll("adversarialStrings") String text) {
        assertDoesNotThrow(() -> rm.sayNextSection(text));
    }

    // =========================================================================
    // 3. sayRaw() — HTML fragment fuzzing
    // =========================================================================

    @Property(tries = 2000)
    @Label("sayRaw() never throws for full Unicode range")
    void fuzzSayRawWithFullUnicode(@ForAll("fullUnicode") String html) {
        assertDoesNotThrow(() -> rm.sayRaw(html));
    }

    @Property
    @Label("sayRaw() never throws for adversarial payloads")
    void fuzzSayRawWithAdversarialStrings(@ForAll("adversarialStrings") String html) {
        assertDoesNotThrow(() -> rm.sayRaw(html));
    }

    // =========================================================================
    // 4. convertTextToId() — invariant holds across all inputs
    // =========================================================================

    @Property(tries = 2000)
    @Label("convertTextToId([a-z0-9]*) invariant for full Unicode")
    void fuzzConvertTextToIdWithFullUnicode(@ForAll("fullUnicode") String text) {
        String id = rm.convertTextToId(text);
        assertTrue(id.matches("[a-z0-9]*"),
                "id must match [a-z0-9]* but got: \"" + id + "\"");
    }

    @Property
    @Label("convertTextToId([a-z0-9]*) invariant for adversarial strings")
    void fuzzConvertTextToIdWithAdversarialStrings(@ForAll("adversarialStrings") String text) {
        String id = rm.convertTextToId(text);
        assertTrue(id.matches("[a-z0-9]*"),
                "id must match [a-z0-9]* but got: \"" + id + "\"");
    }

    // =========================================================================
    // 5. sayAndAssertThat() — message/reason string fuzzing
    // =========================================================================

    @Property(tries = 2000)
    @Label("sayAndAssertThat() message/reason fuzz with full Unicode")
    void fuzzSayAndAssertThatMessageWithFullUnicode(
            @ForAll("fullUnicode") String message,
            @ForAll("fullUnicode") String reason) {
        // Matching value: assertion always passes; we fuzz message/reason strings
        assertDoesNotThrow(() ->
                rm.sayAndAssertThat(message, reason, "x", equalTo("x")));
    }

    @Property
    @Label("sayAndAssertThat() message fuzz with adversarial strings")
    void fuzzSayAndAssertThatMessageWithAdversarialStrings(
            @ForAll("adversarialStrings") String message,
            @ForAll("adversarialStrings") String reason) {
        assertDoesNotThrow(() ->
                rm.sayAndAssertThat(message, reason, 42, equalTo(42)));
    }

    // =========================================================================
    // 5b. convertTextToId() — determinism invariant (same input → same output always)
    // =========================================================================

    /**
     * Determinism theorem: {@code convertTextToId(x)} must always return the same
     * value for the same input {@code x}, regardless of when or how many times
     * it is called. This is a stronger property than idempotency (which only requires
     * f(f(x)) == f(x)); determinism requires f(x) == f(x) across all invocations.
     *
     * <p>Joe Armstrong: "Functions must be total and deterministic — same input,
     * same output, always. Any function that violates this is not a function."
     */
    @Property(tries = 2000)
    @Label("convertTextToId determinism: same input always yields same output (across two fresh instances)")
    void convertTextToIdDeterminism(@ForAll String text) {
        // Two fresh RenderMachineImpl instances must produce identical IDs
        // for the same input — no shared state contamination
        var rm1 = new RenderMachineImpl();
        var rm2 = new RenderMachineImpl();
        rm1.setFileName("FuzzDeterminism1");
        rm2.setFileName("FuzzDeterminism2");

        String id1 = rm1.convertTextToId(text);
        String id2 = rm2.convertTextToId(text);

        assertEquals(id1, id2,
            "convertTextToId must be deterministic across instances: " +
            "input=" + text.length() + " chars, got '" + id1 + "' vs '" + id2 + "'");
    }

    // =========================================================================
    // 6. Url path/query fuzzing — adversarial path segments
    // =========================================================================

    @Property(tries = 2000)
    @Label("Url.path() never throws for printable ASCII path segments")
    void fuzzUrlPathWithAscii(
            @ForAll @CharRange(from = 0x20, to = 0x7E) @StringLength(min = 0, max = 100) String segment) {
        assertDoesNotThrow(() -> {
            try {
                Url.host("http://localhost:8080").path(segment).toString();
            } catch (IllegalStateException _) {
                // URISyntaxException for malformed input is acceptable
            }
        });
    }

    @Property(tries = 500)
    @Label("Url query params never throw for random bytes decoded as UTF-8")
    void fuzzUrlQueryParamWithRandomBytes(
            @ForAll("randomBytesAsString") String key,
            @ForAll("randomBytesAsString") String value) {
        assertDoesNotThrow(() -> {
            try {
                Url.host("http://localhost:8080").addQueryParameter(key, value).toString();
            } catch (IllegalStateException _) {
                // URISyntaxException is acceptable
            }
        });
    }
}

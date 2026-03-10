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

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.r10r.doctester.rendermachine.RenderMachineMarkdownImpl;
import org.r10r.doctester.testbrowser.Url;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests using <a href="https://jqwik.net">jqwik</a>.
 *
 * <p>jqwik runs each {@code @Property} method hundreds of times with
 * randomly generated inputs (default 1000 tries). When a failure is found it
 * <em>shrinks</em> the example to the smallest reproducer and reports it.
 *
 * <h2>What this validates:</h2>
 * <ol>
 *   <li><b>Total functions</b> — {@code say()}, {@code sayNextSection()},
 *       and {@code sayRaw()} must never throw for <em>any</em> String input,
 *       including null-like edge cases and adversarial Unicode.</li>
 *   <li><b>ID generation invariant</b> — {@code convertTextToId()} always
 *       returns a non-null string composed only of lowercase word chars.</li>
 *   <li><b>Url structural invariants</b> — trailing-slash stripping,
 *       leading-slash injection on paths, query-parameter round-trip.</li>
 *   <li><b>Assertion contract</b> — {@code sayAndAssertThat()} with a
 *       matching value never throws for any comparable type.</li>
 * </ol>
 */
@Label("DocTester — property-based tests (jqwik)")
class DocTesterPropertyTest {

    // A fresh RenderMachineMarkdownImpl for each property trial avoids cross-trial
    // state pollution. The constructor only allocates lists (no I/O).
    private RenderMachineMarkdownImpl rm;

    @BeforeProperty
    void freshRenderMachine() {
        rm = new RenderMachineMarkdownImpl();
        rm.setFileName("DocTesterPropertyTest");
    }

    // =========================================================================
    // 1. Total-function properties — must not throw for any String
    // =========================================================================

    @Property
    @Label("say(s) never throws for any String s")
    void sayNeverThrows(@ForAll String text) {
        assertDoesNotThrow(() -> rm.say(text));
    }

    @Property
    @Label("sayNextSection(s) never throws for any String s")
    void sayNextSectionNeverThrows(@ForAll String text) {
        assertDoesNotThrow(() -> rm.sayNextSection(text));
    }

    @Property
    @Label("sayRaw(s) never throws for any String s")
    void sayRawNeverThrows(@ForAll String html) {
        assertDoesNotThrow(() -> rm.sayRaw(html));
    }

    // =========================================================================
    // 2. ID-generation invariants
    // =========================================================================

    @Property
    @Label("convertTextToId always returns non-null")
    void convertTextToIdReturnsNonNull(@ForAll String text) {
        String id = rm.convertTextToId(text);
        assertNotNull(id);
    }

    @Property
    @Label("convertTextToId result matches [a-z0-9]*")
    void convertTextToIdContainsOnlyWordChars(@ForAll String text) {
        String id = rm.convertTextToId(text);
        assertTrue(id.matches("[a-z0-9]*"),
                "id must contain only lowercase word chars, got: \"" + id + "\"");
    }

    @Property
    @Label("convertTextToId is idempotent")
    void convertTextToIdIsIdempotent(@ForAll String text) {
        String once = rm.convertTextToId(text);
        String twice = rm.convertTextToId(once);
        assertEquals(once, twice,
                "convertTextToId must be idempotent");
    }

    // =========================================================================
    // 3. Url structural invariants
    // =========================================================================

    /** Trailing slash on host is always stripped. */
    @Property
    @Label("Url.host() strips trailing slash")
    void urlHostStripsTrailingSlash(
            @ForAll("safeHosts") String host) {
        String withSlash = Url.host(host + "/").toString();
        String without   = Url.host(host).toString();
        assertEquals(without, withSlash,
                "Trailing slash on host must be stripped");
    }

    /** path() always ensures a leading '/' in the result. */
    @Property
    @Label("Url path() always results in a leading /")
    void pathAlwaysHasLeadingSlash(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String segment) {
        String url = Url.host("http://localhost:8080").path(segment).toString();
        assertTrue(url.contains("/" + segment),
                "Path must appear with a leading slash; url=" + url);
    }

    /** path() normalises a path that already has a leading '/'. */
    @Property
    @Label("Url path() with leading slash is idempotent")
    void pathWithLeadingSlashIdempotent(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String segment) {
        String withSlash    = Url.host("http://localhost:8080").path("/" + segment).toString();
        String withoutSlash = Url.host("http://localhost:8080").path(segment).toString();
        assertEquals(withSlash, withoutSlash,
                "path() must not double-add the leading slash");
    }

    /** Query parameter key always appears in the stringified URL. */
    @Property
    @Label("Query parameter key appears in URL")
    void queryParamKeyAppearsInUrl(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String key,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String value) {
        String url = Url.host("http://localhost:8080")
                .addQueryParameter(key, value)
                .toString();
        assertTrue(url.contains(key),   "key must appear in URL; url=" + url);
        assertTrue(url.contains(value), "value must appear in URL; url=" + url);
    }

    // =========================================================================
    // 4. Assertion contract
    // =========================================================================

    @Property
    @Label("sayAndAssertThat(msg, v, equalTo(v)) never throws for any String v")
    void sayAndAssertThatMatchingNeverThrows(@ForAll String value) {
        assertDoesNotThrow(() ->
                rm.sayAndAssertThat("Property assertion", value, equalTo(value)));
    }

    @Property
    @Label("sayAndAssertThat(msg, v, equalTo(v)) never throws for any int v")
    void sayAndAssertThatMatchingIntNeverThrows(@ForAll int value) {
        assertDoesNotThrow(() ->
                rm.sayAndAssertThat("Property int assertion", value, equalTo(value)));
    }

    // =========================================================================
    // Providers
    // =========================================================================

    /** Well-formed HTTP host strings (the URL class rejects malformed URIs). */
    @Provide
    Arbitrary<String> safeHosts() {
        return Arbitraries.of(
                "http://localhost:8080",
                "http://example.com",
                "http://test.org:9090",
                "https://api.example.com:443",
                "http://127.0.0.1:3000");
    }
}

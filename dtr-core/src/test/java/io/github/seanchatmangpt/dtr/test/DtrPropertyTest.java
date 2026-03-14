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
import net.jqwik.api.lifecycle.BeforeProperty;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

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
 * </ol>
 */
@Label("DTR — property-based tests (jqwik)")
class DtrPropertyTest {

    // A fresh RenderMachineImpl for each property trial avoids cross-trial
    // state pollution. The constructor only allocates lists (no I/O).
    private RenderMachineImpl rm;

    @BeforeProperty
    void freshRenderMachine() {
        rm = new RenderMachineImpl();
        rm.setFileName("DtrPropertyTest");
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

}

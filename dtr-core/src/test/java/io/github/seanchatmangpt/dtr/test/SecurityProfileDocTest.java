/*
 * Copyright 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.security.SecurityProfileAnalyzer;
import io.github.seanchatmangpt.dtr.security.SecurityProfileAnalyzer.SecurityProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;

/**
 * Documentation test for {@link SecurityProfileAnalyzer}.
 *
 * <p>Demonstrates the {@code saySecurityProfile} innovation: given any Java class,
 * {@code SecurityProfileAnalyzer} uses reflection to surface security-relevant
 * observations — public field exposure, sensitive naming patterns, serialisation
 * risks, and immutability guarantees — without any source-level annotation or
 * build-time instrumentation.</p>
 *
 * <p>Two classes are analysed side-by-side: a deliberately flawed {@link UserService}
 * (showing WARN and RISK findings) and a clean {@link SafeCredentials} record
 * (showing positive INFO findings).</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SecurityProfileDocTest extends DtrTest {

    // =========================================================================
    // Domain models used across all tests in this class
    // =========================================================================

    /**
     * Intentionally insecure service class used to demonstrate how
     * {@code SecurityProfileAnalyzer} surfaces real problems.
     *
     * <p>Problems present:</p>
     * <ul>
     *   <li>{@code password} is a public field — RISK (sensitive name) + WARN (public field)</li>
     *   <li>{@code getPassword()} is a public method returning it — WARN (sensitive method name)</li>
     *   <li>{@code email} is private with a benign getter — no finding</li>
     * </ul>
     */
    static class UserService {
        public String password;        // RISK: public field with sensitive name
        private String email;

        public String getPassword() {  // WARN: method exposes sensitive name
            return password;
        }

        public String getEmail() {
            return email;
        }
    }

    /**
     * A clean record that stores credentials with no public fields and no
     * inheritance from {@link java.io.Serializable}.
     *
     * <p>Because it is a record it earns the positive immutability INFO finding.
     * Note: record component accessors are named after the component ({@code token()}
     * and {@code secret()}) so they will produce WARN findings — intentionally
     * included to show that even "safe" designs have observations worth documenting.</p>
     */
    record SafeCredentials(String token, String secret) {}

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test methods
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("SecurityProfileAnalyzer — Overview");

        say("""
                `SecurityProfileAnalyzer` analyses any Java class via reflection and returns
                a `SecurityProfile` containing a list of `SecurityFinding` records, each
                carrying a category, a short finding label, contextual detail, and a severity
                level: `INFO`, `WARN`, or `RISK`.""");

        say("""
                The analyzer performs six checks in a single pass over the class's declared
                fields and methods — no agents, no bytecode manipulation, and no external
                dependencies beyond the standard Java reflection API.""");

        sayNote("""
                All checks are read-only reflective introspection. The analyzer never loads,
                instantiates, or mutates the class under analysis.""");

        sayCode("""
                // Minimal usage
                SecurityProfile profile = SecurityProfileAnalyzer.analyze(UserService.class);
                String[][] table        = SecurityProfileAnalyzer.toTable(profile);
                sayTable(table);
                """, "java");
    }

    @Test
    void t02_userServiceFindings() {
        sayNextSection("UserService — Flawed Class Analysis");

        say("""
                `UserService` is a deliberately insecure class. It declares a `public String password`
                field and a `getPassword()` method, providing two distinct signal paths that the
                analyzer detects independently.""");

        var profile = SecurityProfileAnalyzer.analyze(UserService.class);

        sayWarning("""
                `UserService` has at least one RISK-level finding. In a real codebase this class
                should be refactored: make `password` private, use `char[]` instead of `String`
                to allow zeroing, and avoid returning sensitive values directly from getters.""");

        sayTable(SecurityProfileAnalyzer.toTable(profile));

        say("Summary of `UserService` metrics:");
        sayKeyValue(java.util.Map.of(
                "Public methods",       String.valueOf(profile.publicMethods()),
                "Public fields",        String.valueOf(profile.publicFields()),
                "Implements Serializable", String.valueOf(profile.serializable()),
                "Total findings",       String.valueOf(profile.findings().size())
        ));

        // Verify the analyzer actually caught something
        sayAndAssertThat(
                "UserService has at least one finding",
                profile.findings(),
                not(empty())
        );

        // At least one finding must be RISK severity
        long riskCount = profile.findings().stream()
                .filter(f -> "RISK".equals(f.severity()))
                .count();
        sayAndAssertThat(
                "UserService has at least one RISK-level finding",
                (int) riskCount,
                greaterThan(0)
        );
    }

    @Test
    void t03_safeCredentialsRecord() {
        sayNextSection("SafeCredentials — Record as a Positive Example");

        say("""
                Records are immutable by design: all components are private-final and there are
                no public setters. `SecurityProfileAnalyzer` recognises this and emits a positive
                `INFO` finding for the immutability guarantee.""");

        var profile = SecurityProfileAnalyzer.analyze(SafeCredentials.class);

        sayNote("""
                Even a well-designed record like `SafeCredentials` may still have `WARN` findings
                for component accessor methods whose names contain sensitive tokens (`token`, `secret`).
                This is expected and informational — it does not mean the record is unsafe.""");

        sayTable(SecurityProfileAnalyzer.toTable(profile));

        // Records are never Serializable by default unless they explicitly implement it
        sayAndAssertThat(
                "SafeCredentials does not implement Serializable",
                profile.serializable(),
                is(false)
        );

        // The record immutability INFO finding must be present
        boolean hasImmutabilityFinding = profile.findings().stream()
                .anyMatch(f -> "INFO".equals(f.severity())
                        && f.category().equals("Immutability"));
        sayAndAssertThat(
                "Record earns Immutability INFO finding",
                hasImmutabilityFinding,
                is(true)
        );
    }

    @Test
    void t04_markdownReport() {
        sayNextSection("Markdown Report Output");

        say("""
                `SecurityProfileAnalyzer.toMarkdown(profile)` returns a list of markdown lines
                that can be joined and embedded in documentation or written to a file. Each
                finding is prefixed with its severity badge for quick visual scanning.""");

        var profile = SecurityProfileAnalyzer.analyze(UserService.class);
        var markdownLines = SecurityProfileAnalyzer.toMarkdown(profile);

        sayCode(String.join("\n", markdownLines), "markdown");

        // Report must contain both the class name and at least one severity badge
        boolean containsClassName = markdownLines.stream()
                .anyMatch(l -> l.contains("UserService"));
        boolean containsBadge = markdownLines.stream()
                .anyMatch(l -> l.contains("[RISK]") || l.contains("[WARN]") || l.contains("[INFO]"));

        sayAndAssertThat("Markdown report contains class name", containsClassName, is(true));
        sayAndAssertThat("Markdown report contains at least one severity badge", containsBadge, is(true));
    }

    @Test
    void t05_sixChecksDocumented() {
        sayNextSection("The Six Checks — Reference Table");

        say("""
                The analyzer performs exactly six checks in order. Each check targets a specific
                aspect of the class's security surface. The table below documents each check,
                its trigger condition, and its assigned severity.""");

        sayTable(new String[][]{
                {"#", "Category",       "Trigger Condition",                                         "Severity"},
                {"1", "Serialization",  "Implements Serializable without static final serialVersionUID", "RISK"},
                {"2", "Public API",     "Declares one or more public instance or static fields",      "WARN"},
                {"3", "Sensitive API",  "Method name contains: password, secret, token, key, credential", "WARN"},
                {"4", "Sensitive Data", "Field name contains: password, secret, token, key, credential",  "RISK"},
                {"5", "Lifecycle",      "Class carries @Deprecated annotation",                       "INFO"},
                {"6", "Immutability",   "Class is a Java record (shallowly immutable)",               "INFO"},
        });

        sayNote("""
                Checks 3 and 4 use case-insensitive substring matching on the lower-cased member
                name. A field named `ApiToken` triggers check 4 because its lower-cased form
                contains the token `token`.""");
    }
}

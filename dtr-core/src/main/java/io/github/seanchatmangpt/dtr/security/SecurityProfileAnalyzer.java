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
package io.github.seanchatmangpt.dtr.security;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Analyzes a class's security surface via reflection — public API exposure,
 * {@link Serializable} check, sensitive field patterns, and security-relevant
 * annotations.
 *
 * <p>All analysis is purely reflective: no bytecode manipulation, no agents,
 * no external dependencies. Findings are categorised into three severity levels:</p>
 * <ul>
 *   <li><strong>INFO</strong> — neutral observation (e.g. immutable record)</li>
 *   <li><strong>WARN</strong> — design smell worth reviewing</li>
 *   <li><strong>RISK</strong> — probable security exposure requiring remediation</li>
 * </ul>
 *
 * <p>Typical usage inside a DTR doc-test:</p>
 * <pre>{@code
 * var profile = SecurityProfileAnalyzer.analyze(UserService.class);
 * sayTable(SecurityProfileAnalyzer.toTable(profile));
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class SecurityProfileAnalyzer {

    // -------------------------------------------------------------------------
    // Sensitive name patterns (lower-cased substring matching)
    // -------------------------------------------------------------------------

    private static final Set<String> SENSITIVE_TOKENS = Set.of(
            "password", "secret", "token", "key", "credential"
    );

    private SecurityProfileAnalyzer() {}

    // =========================================================================
    // Public result types
    // =========================================================================

    /**
     * A single security observation derived from class analysis.
     *
     * @param category the broad category (e.g. "Serialization", "Public API")
     * @param finding  a short human-readable label identifying what was found
     * @param detail   expanded context — member name, type, rationale
     * @param severity one of {@code "INFO"}, {@code "WARN"}, or {@code "RISK"}
     */
    public record SecurityFinding(
            String category,
            String finding,
            String detail,
            String severity) {}

    /**
     * The complete security analysis result for a single class.
     *
     * @param clazz         the analysed class
     * @param findings      all findings (may be empty if nothing was detected)
     * @param publicMethods count of public methods declared directly on the class
     * @param publicFields  count of public fields declared directly on the class
     * @param serializable  {@code true} if the class implements {@link Serializable}
     */
    public record SecurityProfile(
            Class<?> clazz,
            List<SecurityFinding> findings,
            int publicMethods,
            int publicFields,
            boolean serializable) {}

    // =========================================================================
    // Core analysis
    // =========================================================================

    /**
     * Analyses the security surface of {@code clazz} and returns a
     * {@link SecurityProfile} containing all findings.
     *
     * <p>The six checks performed, in order:</p>
     * <ol>
     *   <li>Implements {@link Serializable} without a {@code serialVersionUID} field → RISK</li>
     *   <li>Declares public instance or static fields → WARN per field</li>
     *   <li>Declares methods whose names contain sensitive tokens → WARN per method</li>
     *   <li>Declares fields whose names contain sensitive tokens → RISK per field</li>
     *   <li>Carries {@link Deprecated} annotation → INFO</li>
     *   <li>Is a {@link Record} subtype (immutable by design) → INFO</li>
     * </ol>
     *
     * @param clazz the class to analyse; must not be {@code null}
     * @return a fully populated {@link SecurityProfile}
     * @throws NullPointerException if {@code clazz} is {@code null}
     */
    public static SecurityProfile analyze(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz must not be null");
        }

        var findings = new ArrayList<SecurityFinding>();

        Field[] declaredFields  = clazz.getDeclaredFields();
        Method[] declaredMethods = clazz.getDeclaredMethods();

        // ------------------------------------------------------------------
        // Check 1: Serializable without serialVersionUID
        // ------------------------------------------------------------------
        boolean serializable = Serializable.class.isAssignableFrom(clazz);
        if (serializable) {
            boolean hasSerialVersionUID = Arrays.stream(declaredFields)
                    .anyMatch(f -> f.getName().equals("serialVersionUID")
                            && Modifier.isStatic(f.getModifiers())
                            && Modifier.isFinal(f.getModifiers()));
            if (!hasSerialVersionUID) {
                findings.add(new SecurityFinding(
                        "Serialization",
                        "Serializable without serialVersionUID",
                        "Class implements Serializable but declares no static final serialVersionUID. "
                                + "Missing UID allows accidental deserialization of incompatible class versions.",
                        "RISK"
                ));
            }
        }

        // ------------------------------------------------------------------
        // Check 2: Public fields (prefer encapsulation)
        // ------------------------------------------------------------------
        int publicFieldCount = 0;
        for (var f : declaredFields) {
            if (Modifier.isPublic(f.getModifiers()) && !f.isSynthetic()) {
                publicFieldCount++;
                findings.add(new SecurityFinding(
                        "Public API",
                        "Public field exposes internal state",
                        "Field `%s %s` is public. Prefer private fields with controlled accessors."
                                .formatted(f.getType().getSimpleName(), f.getName()),
                        "WARN"
                ));
            }
        }

        // ------------------------------------------------------------------
        // Check 3: Methods with sensitive names
        // ------------------------------------------------------------------
        for (var m : declaredMethods) {
            if (m.isSynthetic()) {
                continue;
            }
            var nameLower = m.getName().toLowerCase(Locale.ROOT);
            for (var token : SENSITIVE_TOKENS) {
                if (nameLower.contains(token)) {
                    findings.add(new SecurityFinding(
                            "Sensitive API",
                            "Method name exposes sensitive concern",
                            "Method `%s` contains the sensitive token \"%s\". "
                                    + "Consider whether this method should be public or logged."
                                    .formatted(m.getName(), token),
                            "WARN"
                    ));
                    break; // one finding per method is enough
                }
            }
        }

        // ------------------------------------------------------------------
        // Check 4: Fields with sensitive names
        // ------------------------------------------------------------------
        for (var f : declaredFields) {
            if (f.isSynthetic()) {
                continue;
            }
            var nameLower = f.getName().toLowerCase(Locale.ROOT);
            for (var token : SENSITIVE_TOKENS) {
                if (nameLower.contains(token)) {
                    int mods = f.getModifiers();
                    String visibility = Modifier.isPublic(mods) ? "public"
                            : Modifier.isProtected(mods) ? "protected"
                            : Modifier.isPrivate(mods) ? "private"
                            : "package-private";
                    findings.add(new SecurityFinding(
                            "Sensitive Data",
                            "Field stores sensitive data",
                            "Field `%s %s` (visibility: %s) matches sensitive token \"%s\". "
                                    + "Ensure it is never serialized, logged, or exposed via reflection."
                                    .formatted(f.getType().getSimpleName(), f.getName(), visibility, token),
                            "RISK"
                    ));
                    break; // one finding per field is enough
                }
            }
        }

        // ------------------------------------------------------------------
        // Check 5: @Deprecated annotation
        // ------------------------------------------------------------------
        if (clazz.isAnnotationPresent(Deprecated.class)) {
            findings.add(new SecurityFinding(
                    "Lifecycle",
                    "Class is deprecated",
                    "Class `%s` carries @Deprecated. Deprecated code may contain known security issues "
                            + "or outdated practices that were fixed in replacement APIs."
                            .formatted(clazz.getSimpleName()),
                    "INFO"
            ));
        }

        // ------------------------------------------------------------------
        // Check 6: Record → immutable by design (positive finding)
        // ------------------------------------------------------------------
        if (clazz.isRecord()) {
            findings.add(new SecurityFinding(
                    "Immutability",
                    "Record is immutable by design",
                    "Class `%s` is a Java record. Records are shallowly immutable: all components are "
                            + "private-final with no public setters, reducing accidental mutation risks."
                            .formatted(clazz.getSimpleName()),
                    "INFO"
            ));
        }

        // ------------------------------------------------------------------
        // Count public methods (declared only, non-synthetic)
        // ------------------------------------------------------------------
        int publicMethodCount = (int) Arrays.stream(declaredMethods)
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .count();

        return new SecurityProfile(
                clazz,
                List.copyOf(findings),
                publicMethodCount,
                publicFieldCount,
                serializable
        );
    }

    // =========================================================================
    // Formatting utilities
    // =========================================================================

    /**
     * Converts a {@link SecurityProfile} into a 2-D table suitable for
     * {@code sayTable(String[][])}. The first row is the header row.
     *
     * <p>Columns: Category | Finding | Detail | Severity</p>
     *
     * <p>If there are no findings an informational placeholder row is returned
     * so that the caller's table is never empty.</p>
     *
     * @param profile the profile to convert; must not be {@code null}
     * @return a 2-D array with at least two rows (header + one data row)
     */
    public static String[][] toTable(SecurityProfile profile) {
        var findings = profile.findings();
        int rows = Math.max(findings.size(), 1);
        var table = new String[rows + 1][4];

        // Header
        table[0] = new String[]{"Category", "Finding", "Detail", "Severity"};

        if (findings.isEmpty()) {
            table[1] = new String[]{"—", "No findings", "No security concerns were detected.", "INFO"};
        } else {
            for (int i = 0; i < findings.size(); i++) {
                var f = findings.get(i);
                table[i + 1] = new String[]{f.category(), f.finding(), f.detail(), f.severity()};
            }
        }
        return table;
    }

    /**
     * Converts a {@link SecurityProfile} into a list of formatted markdown lines
     * representing a concise security report.
     *
     * <p>The report includes a summary section (class name, public method/field
     * counts, serializable flag) followed by one bullet per finding with its
     * severity prefix.</p>
     *
     * @param profile the profile to format; must not be {@code null}
     * @return an unmodifiable list of markdown lines (never {@code null}, never empty)
     */
    public static List<String> toMarkdown(SecurityProfile profile) {
        var lines = new ArrayList<String>();
        var clazz = profile.clazz();

        lines.add("## Security Profile: `%s`".formatted(clazz.getSimpleName()));
        lines.add("");
        lines.add("| Property | Value |");
        lines.add("|---|---|");
        lines.add("| Class | `%s` |".formatted(clazz.getName()));
        lines.add("| Public methods | %d |".formatted(profile.publicMethods()));
        lines.add("| Public fields | %d |".formatted(profile.publicFields()));
        lines.add("| Implements Serializable | %s |".formatted(profile.serializable() ? "yes" : "no"));
        lines.add("| Total findings | %d |".formatted(profile.findings().size()));
        lines.add("");

        if (profile.findings().isEmpty()) {
            lines.add("_No security concerns detected._");
        } else {
            lines.add("### Findings");
            lines.add("");
            for (var f : profile.findings()) {
                String badge = switch (f.severity()) {
                    case "RISK" -> "[RISK]";
                    case "WARN" -> "[WARN]";
                    default     -> "[INFO]";
                };
                lines.add("- **%s** `%s` — %s".formatted(badge, f.finding(), f.detail()));
            }
        }
        return List.copyOf(lines);
    }
}

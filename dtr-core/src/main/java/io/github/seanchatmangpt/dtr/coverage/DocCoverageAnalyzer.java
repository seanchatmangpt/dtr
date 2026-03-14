package io.github.seanchatmangpt.dtr.coverage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyzes documentation coverage for a class — which public methods were documented
 * in the current test (tracked via a set of method signatures) vs. which were not.
 */
public final class DocCoverageAnalyzer {

    private DocCoverageAnalyzer() {}

    /**
     * Produces a coverage report for the given class.
     *
     * @param clazz            the class whose public API to check
     * @param documentedSigs   set of method signatures that were documented (caller-tracked)
     * @return list of coverage rows, one per public method
     */
    public static List<CoverageRow> analyze(Class<?> clazz, Set<String> documentedSigs) {
        var rows = new ArrayList<CoverageRow>();

        var publicMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();

        for (Method m : publicMethods) {
            String sig = buildSig(m);
            // Check if any documented sig starts with or equals this method name
            boolean documented = documentedSigs.stream()
                    .anyMatch(s -> s.equals(m.getName()) || s.startsWith(m.getName() + "("));
            String via = documented
                    ? documentedSigs.stream()
                            .filter(s -> s.equals(m.getName()) || s.startsWith(m.getName() + "("))
                            .findFirst()
                            .orElse("✅")
                    : "—";
            rows.add(new CoverageRow(sig, documented, documented ? "✅ " + via : "❌"));
        }

        return rows;
    }

    private static String buildSig(Method m) {
        String params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return m.getReturnType().getSimpleName() + " " + m.getName() + "(" + params + ")";
    }
}

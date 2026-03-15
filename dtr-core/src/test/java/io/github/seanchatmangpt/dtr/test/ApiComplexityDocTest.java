package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * API Complexity Profiler — uses reflection to measure the cognitive load
 * of the RenderMachineCommands interface and document it with real metrics
 * derived from bytecode. No other tool generates documentation that quantifies
 * API cognitive load with live reflection data.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApiComplexityDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // -------------------------------------------------------------------------
    // Shared reflection data — computed once, reused across test methods
    // -------------------------------------------------------------------------

    private static final Method[] ALL_METHODS = RenderMachineCommands.class.getMethods();

    @Test
    void a1_complexity_overview() {
        sayNextSection("API Complexity: What It Measures and Why It Matters");

        say("Every public API imposes a cognitive load on its users. Cognitive load is " +
            "not subjective — it can be quantified from the bytecode. The number of " +
            "methods, the total parameters those methods accept, the number of overloaded " +
            "name groups, and the prevalence of generic type parameters are all measurable " +
            "proxies for how much a developer must hold in working memory before they can " +
            "use the API correctly.");

        say("DTR's API Complexity Profiler applies this measurement to " +
            "`RenderMachineCommands` — the core `say*` contract that every DTR test " +
            "interacts with. The profiler uses `Class.getMethods()`, " +
            "`Method.getParameterCount()`, and `Method.getGenericParameterTypes()` to " +
            "derive its metrics entirely from the compiled interface bytecode. If the " +
            "interface changes, the numbers change automatically on the next test run.");

        sayCode("""
                // How the profiler works — all metrics from a single reflection call
                Method[] methods = RenderMachineCommands.class.getMethods();

                int totalParams = Arrays.stream(methods)
                    .mapToInt(Method::getParameterCount)
                    .sum();

                long genericMethods = Arrays.stream(methods)
                    .filter(m -> Arrays.stream(m.getGenericParameterTypes())
                        .anyMatch(t -> t instanceof ParameterizedType))
                    .count();
                """, "java");

        sayNote("All metrics below are measured from the live bytecode of " +
                "`RenderMachineCommands` at test runtime. They update automatically " +
                "whenever the interface evolves.");

        sayWarning("A high complexity score does not mean the API is bad — it means " +
                   "users need more cognitive support (examples, defaults, builder patterns) " +
                   "to use it without errors.");
    }

    @Test
    void a2_surface_metrics() {
        sayNextSection("RenderMachineCommands: Surface Metrics");

        say("The surface metrics below characterise the overall shape of the " +
            "`RenderMachineCommands` API. They are the starting point for any " +
            "API usability review.");

        Method[] methods = ALL_METHODS;

        int totalMethods = methods.length;
        int totalParams = Arrays.stream(methods)
                .mapToInt(Method::getParameterCount)
                .sum();
        double avgParams = totalMethods == 0 ? 0.0
                : (double) totalParams / totalMethods;

        // Find method with maximum parameter count
        Method maxParamMethod = Arrays.stream(methods)
                .max(Comparator.comparingInt(Method::getParameterCount))
                .orElse(null);
        int maxParams = maxParamMethod == null ? 0 : maxParamMethod.getParameterCount();
        String maxParamMethodName = maxParamMethod == null ? "n/a" : maxParamMethod.getName();

        long genericCount = Arrays.stream(methods)
                .filter(m -> Arrays.stream(m.getGenericParameterTypes())
                        .anyMatch(t -> t instanceof ParameterizedType))
                .count();

        long voidCount = Arrays.stream(methods)
                .filter(m -> m.getReturnType().equals(void.class))
                .count();
        long nonVoidCount = totalMethods - voidCount;

        // Overloaded method names = names that appear more than once
        Map<String, Long> nameCounts = Arrays.stream(methods)
                .collect(Collectors.groupingBy(Method::getName, Collectors.counting()));
        long overloadedNames = nameCounts.values().stream()
                .filter(c -> c > 1)
                .count();

        sayTable(new String[][] {
                {"Metric", "Value"},
                {"Total public methods", String.valueOf(totalMethods)},
                {"Total parameters (all methods combined)", String.valueOf(totalParams)},
                {"Average parameters per method", String.format("%.2f", avgParams)},
                {"Max parameters in one method", maxParams + " (" + maxParamMethodName + ")"},
                {"Generic (parameterized) methods", String.valueOf(genericCount)},
                {"Void methods", String.valueOf(voidCount)},
                {"Methods with return value", String.valueOf(nonVoidCount)},
                {"Overloaded method names", String.valueOf(overloadedNames)},
        });

        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("Interface analysed", RenderMachineCommands.class.getName());
        kv.put("Reflection source", "Class.getMethods() (public + inherited)");
        kv.put("Java version", System.getProperty("java.version"));
        kv.put("Measured at", java.time.Instant.now().toString());
        sayKeyValue(kv);
    }

    @Test
    void a3_parameter_distribution() {
        sayNextSection("Parameter Count Distribution");

        say("The parameter count distribution shows how many methods require exactly " +
            "0, 1, 2, 3, or 4+ parameters. Methods with 0 or 1 parameters have the " +
            "lowest cognitive load — the caller needs to supply little or no context. " +
            "Methods with 4+ parameters are the most demanding and are candidates for " +
            "builder-pattern refactoring.");

        Method[] methods = ALL_METHODS;

        // Count methods per param bucket
        int count0 = 0, count1 = 0, count2 = 0, count3 = 0, count4plus = 0;
        Map<Integer, List<String>> bucketExamples = new TreeMap<>();
        for (int i = 0; i <= 4; i++) {
            bucketExamples.put(i, new ArrayList<>());
        }

        for (Method m : methods) {
            int pc = m.getParameterCount();
            switch (pc) {
                case 0 -> {
                    count0++;
                    bucketExamples.get(0).add(m.getName());
                }
                case 1 -> {
                    count1++;
                    bucketExamples.get(1).add(m.getName());
                }
                case 2 -> {
                    count2++;
                    bucketExamples.get(2).add(m.getName());
                }
                case 3 -> {
                    count3++;
                    bucketExamples.get(3).add(m.getName());
                }
                default -> {
                    count4plus++;
                    bucketExamples.get(4).add(m.getName());
                }
            }
        }

        // Frequency table with examples (up to 2 per bucket)
        sayTable(new String[][] {
                {"Param Count", "Method Count", "Example Methods"},
                {"0", String.valueOf(count0),    examplesStr(bucketExamples.get(0), 2)},
                {"1", String.valueOf(count1),    examplesStr(bucketExamples.get(1), 2)},
                {"2", String.valueOf(count2),    examplesStr(bucketExamples.get(2), 2)},
                {"3", String.valueOf(count3),    examplesStr(bucketExamples.get(3), 2)},
                {"4+", String.valueOf(count4plus), examplesStr(bucketExamples.get(4), 2)},
        });

        // ASCII chart of the distribution
        double[] values = {count0, count1, count2, count3, count4plus};
        String[] xLabels = {"0", "1", "2", "3", "4+"};
        sayAsciiChart("Methods by Parameter Count", values, xLabels);

        sayNote("A distribution weighted towards 0-1 parameters indicates a fluent, " +
                "easy-to-use API. A long tail of 4+ parameter methods signals " +
                "complexity hotspots that may benefit from value objects or builders.");
    }

    @Test
    void a4_top_complex_methods() {
        sayNextSection("Top-10 Complex Methods");

        say("The methods below rank highest by parameter count combined with generic " +
            "type usage. These are the API entry points that require the most context " +
            "from the caller and are therefore the most likely sources of user error.");

        Method[] methods = ALL_METHODS;

        // Sort by param count descending, then name ascending for tie-breaking
        List<Method> sorted = Arrays.stream(methods)
                .sorted(Comparator.comparingInt(Method::getParameterCount)
                        .reversed()
                        .thenComparing(Method::getName))
                .limit(10)
                .collect(Collectors.toList());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Method", "Params", "Return Type", "Is Void", "Has Generics"});

        for (Method m : sorted) {
            boolean hasGenerics = Arrays.stream(m.getGenericParameterTypes())
                    .anyMatch(t -> t instanceof ParameterizedType);
            boolean isVoid = m.getReturnType().equals(void.class);

            rows.add(new String[]{
                    m.getName(),
                    String.valueOf(m.getParameterCount()),
                    m.getReturnType().getSimpleName(),
                    isVoid ? "yes" : "no",
                    hasGenerics ? "yes" : "no",
            });
        }

        sayTable(rows.toArray(new String[0][]));

        sayNote("Methods marked 'Has Generics: yes' accept parameterized types " +
                "(e.g., `List<String>`, `Map<String,String>`). These impose additional " +
                "cognitive load because callers must reason about type parameters.");
    }

    @Test
    void a5_overload_groups() {
        sayNextSection("Overload Groups");

        say("Overloading — multiple methods sharing the same name but differing in " +
            "parameter signatures — is a double-edged usability tool. When used well " +
            "it reduces ceremony (callers use the simplest overload by default). " +
            "When overused it creates ambiguity and forces callers to read every variant " +
            "before choosing. The table below shows every overloaded family in " +
            "`RenderMachineCommands`.");

        Method[] methods = ALL_METHODS;

        // Group by method name
        Map<String, List<Method>> groups = Arrays.stream(methods)
                .collect(Collectors.groupingBy(
                        Method::getName,
                        TreeMap::new,
                        Collectors.toList()));

        // Only overloaded names (count > 1)
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Method Name", "Overload Count", "Parameter Variants"});

        for (Map.Entry<String, List<Method>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                String name = entry.getKey();
                int overloadCount = entry.getValue().size();

                // Build a compact string showing each variant's param types
                String variants = entry.getValue().stream()
                        .sorted(Comparator.comparingInt(Method::getParameterCount))
                        .map(m -> "(" + Arrays.stream(m.getParameterTypes())
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", ")) + ")")
                        .collect(Collectors.joining(" | "));

                rows.add(new String[]{name, String.valueOf(overloadCount), variants});
            }
        }

        if (rows.size() == 1) {
            // Only the header row — no overloads found
            say("No overloaded method groups were found in `RenderMachineCommands`. " +
                "Every method name is unique, which maximises discoverability at the " +
                "cost of some verbosity.");
        } else {
            sayTable(rows.toArray(new String[0][]));
        }

        sayNote("Overload families with identical parameter counts but different types " +
                "are the most hazardous — callers may silently invoke the wrong variant " +
                "due to implicit widening or autoboxing.");
    }

    @Test
    void a6_complexity_score() {
        sayNextSection("Complexity Score");

        say("The composite complexity score aggregates the three strongest predictors " +
            "of API cognitive load into a single number. A lower score is better.");

        Method[] methods = ALL_METHODS;

        int totalParams = Arrays.stream(methods)
                .mapToInt(Method::getParameterCount)
                .sum();

        // Count distinct overloaded names (names with more than one method)
        Map<String, Long> nameCounts = Arrays.stream(methods)
                .collect(Collectors.groupingBy(Method::getName, Collectors.counting()));
        long overloadedNames = nameCounts.values().stream()
                .filter(c -> c > 1)
                .count();

        long genericCount = Arrays.stream(methods)
                .filter(m -> Arrays.stream(m.getGenericParameterTypes())
                        .anyMatch(t -> t instanceof ParameterizedType))
                .count();

        // Composite formula: total_params + (overloads * 2) + (generics * 3)
        long score = totalParams + (overloadedNames * 2) + (genericCount * 3);

        // Baseline: a minimal 5-method API with 1 param each, no overloads, no generics
        long baselineScore = 5; // 5 params + 0 + 0

        sayNote("Complexity score formula: `total_params + (overloaded_names × 2) + " +
                "(generic_methods × 3)`. Parameters contribute linearly. Overloaded " +
                "method names are weighted ×2 because callers must evaluate multiple " +
                "candidates. Generic methods are weighted ×3 because callers must reason " +
                "about type arguments in addition to value arguments.");

        sayTable(new String[][] {
                {"Component", "Raw Count", "Weight", "Contribution"},
                {"Total parameters", String.valueOf(totalParams), "×1",
                        String.valueOf(totalParams)},
                {"Overloaded method names", String.valueOf(overloadedNames), "×2",
                        String.valueOf(overloadedNames * 2)},
                {"Generic (parameterized) methods", String.valueOf(genericCount), "×3",
                        String.valueOf(genericCount * 3)},
                {"Composite score", "", "", String.valueOf(score)},
        });

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("RenderMachineCommands score", String.valueOf(score));
        summary.put("Baseline (5 single-param methods)", String.valueOf(baselineScore));
        summary.put("Score ratio vs baseline", String.format("%.1fx", (double) score / baselineScore));
        summary.put("Methods analysed", String.valueOf(methods.length));
        sayKeyValue(summary);

        say("A higher score relative to the baseline indicates a richer but more " +
            "demanding API surface. RenderMachineCommands is intentionally broad " +
            "because it must cover every documentation primitive from simple paragraphs " +
            "to live benchmarks and Mermaid diagrams. The score should be interpreted " +
            "alongside the parameter distribution: if most methods have 0-1 parameters " +
            "the score is large but each individual interaction is still simple.");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns up to {@code max} example names from the list, comma-separated. */
    private static String examplesStr(List<String> names, int max) {
        if (names == null || names.isEmpty()) {
            return "(none)";
        }
        return names.stream()
                .sorted()
                .limit(max)
                .collect(Collectors.joining(", "));
    }
}

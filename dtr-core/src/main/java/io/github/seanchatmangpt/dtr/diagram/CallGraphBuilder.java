package io.github.seanchatmangpt.dtr.diagram;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds a Mermaid {@code graph LR} call graph for all methods in a class that
 * have a code model available (annotated with {@code @CodeReflection}).
 *
 * <p>Extracts {@code InvokeOp} targets from the Java 26 Code Reflection IR
 * (JEP 516 / Project Babylon) and renders directed caller → callee edges.</p>
 */
@SuppressWarnings("preview")
public final class CallGraphBuilder {

    private CallGraphBuilder() {}

    /**
     * Builds a Mermaid {@code graph LR} DSL string from the call relationships in
     * the given class. Only methods with code models contribute edges.
     *
     * @param clazz the class to analyze
     * @return Mermaid graph DSL, or an empty string if no call relationships found
     */
    public static String build(Class<?> clazz) {
        List<String> edges = new ArrayList<>();

        for (Method method : Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers()))
                .toList()) {
            List<String> callees = CodeModelAnalyzer.extractCallees(method);
            for (String callee : callees) {
                String safeCallee = callee.replace(".", "_").replace("<", "").replace(">", "");
                String safeCaller = method.getName();
                String edge = "    " + safeCaller + " --> " + safeCallee;
                if (!edges.contains(edge)) {
                    edges.add(edge);
                }
            }
        }

        if (edges.isEmpty()) {
            return "";
        }

        return "graph LR\n" + String.join("\n", edges);
    }
}

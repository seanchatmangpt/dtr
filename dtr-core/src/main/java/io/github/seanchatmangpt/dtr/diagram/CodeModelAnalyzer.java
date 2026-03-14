package io.github.seanchatmangpt.dtr.diagram;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared utility for Java 26 Code Reflection API (JEP 516 / Project Babylon) traversal.
 *
 * <p>Uses {@code method.codeModel()} (preview API) to walk the IR tree of a
 * {@code @CodeReflection}-annotated method and extract operation counts, block counts,
 * and IR excerpts for documentation.</p>
 *
 * <p>Falls back gracefully to method signature introspection when no code model is
 * available (method not annotated with {@code @CodeReflection}, or runtime &lt; Java 25).</p>
 */
@SuppressWarnings("preview")
public final class CodeModelAnalyzer {

    private CodeModelAnalyzer() {}

    /**
     * Analysis result from walking a method's code model.
     *
     * @param methodSig   human-readable method signature
     * @param opCounts    map from Op simple class name to count, insertion-ordered
     * @param blockCount  number of basic blocks in the method body
     * @param totalOps    total number of operations across all blocks
     * @param irExcerpt   first {@code excerptLimit} op descriptions as strings
     * @param hasModel    true if a real code model was obtained; false = fallback mode
     */
    public record Analysis(
            String methodSig,
            Map<String, Long> opCounts,
            int blockCount,
            int totalOps,
            List<String> irExcerpt,
            boolean hasModel
    ) {}

    /**
     * Analyzes a method using the Java 26 Code Reflection API, with fallback to
     * standard reflection on older runtimes or un-annotated methods.
     *
     * @param method       the method to analyze (should be annotated with {@code @CodeReflection})
     * @param excerptLimit maximum number of op descriptions to include in the IR excerpt
     * @return analysis result; never null
     */
    public static Analysis analyze(Method method, int excerptLimit) {
        String sig = buildSignature(method);

        try {
            // Java 25/26 preview: Method.codeModel() returns Optional<CoreOps.FuncOp>
            var codeModelOpt = (java.util.Optional<?>) method.getClass()
                    .getMethod("codeModel")
                    .invoke(method);

            if (codeModelOpt.isEmpty()) {
                return fallback(sig);
            }

            var funcOp = codeModelOpt.get();

            // Walk blocks and ops via reflection to avoid hard compile-time dependency
            // on preview types (java.lang.reflect.code.op.CoreOps.FuncOp)
            var body = funcOp.getClass().getMethod("body").invoke(funcOp);
            @SuppressWarnings("unchecked")
            var blocks = (List<?>) body.getClass().getMethod("blocks").invoke(body);

            Map<String, Long> opCounts = new LinkedHashMap<>();
            List<String> irExcerpt = new ArrayList<>();
            int totalOps = 0;

            for (var block : blocks) {
                @SuppressWarnings("unchecked")
                var ops = (List<?>) block.getClass().getMethod("ops").invoke(block);
                for (var op : ops) {
                    String opName = op.getClass().getSimpleName();
                    opCounts.merge(opName, 1L, Long::sum);
                    totalOps++;
                    if (irExcerpt.size() < excerptLimit) {
                        irExcerpt.add(opName + ": " + describeOp(op));
                    }
                }
            }

            return new Analysis(sig, opCounts, blocks.size(), totalOps, irExcerpt, true);

        } catch (NoSuchMethodException e) {
            // codeModel() not available on this Java version
            return fallback(sig);
        } catch (Exception e) {
            return fallback(sig);
        }
    }

    /**
     * Extracts all unique callee method names from InvokeOp nodes in a method's code model.
     *
     * @param method the method to analyze
     * @return list of callee method simple names, empty if no code model available
     */
    public static List<String> extractCallees(Method method) {
        try {
            var codeModelOpt = (java.util.Optional<?>) method.getClass()
                    .getMethod("codeModel")
                    .invoke(method);
            if (codeModelOpt.isEmpty()) return List.of();

            var funcOp = codeModelOpt.get();
            var body = funcOp.getClass().getMethod("body").invoke(funcOp);
            @SuppressWarnings("unchecked")
            var blocks = (List<?>) body.getClass().getMethod("blocks").invoke(body);

            List<String> callees = new ArrayList<>();
            for (var block : blocks) {
                @SuppressWarnings("unchecked")
                var ops = (List<?>) block.getClass().getMethod("ops").invoke(block);
                for (var op : ops) {
                    if (op.getClass().getSimpleName().contains("Invoke")) {
                        String desc = describeOp(op);
                        if (!desc.isEmpty() && !callees.contains(desc)) {
                            callees.add(desc);
                        }
                    }
                }
            }
            return callees;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Extracts blocks with their ops for CFG generation.
     * Returns a list of block descriptions, each containing op names.
     */
    public static List<BlockInfo> extractBlocks(Method method) {
        try {
            var codeModelOpt = (java.util.Optional<?>) method.getClass()
                    .getMethod("codeModel")
                    .invoke(method);
            if (codeModelOpt.isEmpty()) return List.of();

            var funcOp = codeModelOpt.get();
            var body = funcOp.getClass().getMethod("body").invoke(funcOp);
            @SuppressWarnings("unchecked")
            var blocks = (List<?>) body.getClass().getMethod("blocks").invoke(body);

            List<BlockInfo> result = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.get(i);
                @SuppressWarnings("unchecked")
                var ops = (List<?>) block.getClass().getMethod("ops").invoke(block);

                List<String> opDescs = new ArrayList<>();
                List<Integer> successors = new ArrayList<>();
                boolean isTerminator = false;

                for (var op : ops) {
                    String name = op.getClass().getSimpleName();
                    opDescs.add(name + ": " + describeOp(op));
                    // Detect branch ops that have successors
                    if (name.contains("Branch") || name.contains("Return")) {
                        isTerminator = true;
                        // Try to get successors
                        try {
                            @SuppressWarnings("unchecked")
                            var succs = (List<?>) op.getClass().getMethod("successors").invoke(op);
                            for (var s : succs) {
                                // Find the block index this successor refers to
                                for (int j = 0; j < blocks.size(); j++) {
                                    if (blocks.get(j) == s || blocks.get(j).equals(s)) {
                                        successors.add(j);
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // Successor extraction is best-effort
                        }
                    }
                }
                result.add(new BlockInfo(i, opDescs, successors, isTerminator));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Info about a single basic block extracted from a code model. */
    public record BlockInfo(int index, List<String> opDescs, List<Integer> successors, boolean isTerminator) {}

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static String buildSignature(Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return method.getReturnType().getSimpleName() + " " + method.getName() + "(" + params + ")";
    }

    private static String describeOp(Object op) {
        // Best-effort: try invokeDescriptor().name() for InvokeOp, otherwise toString snippet
        try {
            var descriptor = op.getClass().getMethod("invokeDescriptor").invoke(op);
            return (String) descriptor.getClass().getMethod("name").invoke(descriptor);
        } catch (Exception ignored) {}
        // Fall back to a short toString snippet
        String s = op.toString();
        return s.length() > 40 ? s.substring(0, 40) + "…" : s;
    }

    private static Analysis fallback(String sig) {
        return new Analysis(sig, Map.of(), 0, 0, List.of(), false);
    }
}

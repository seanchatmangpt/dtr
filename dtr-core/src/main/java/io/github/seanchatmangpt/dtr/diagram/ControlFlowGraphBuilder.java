package io.github.seanchatmangpt.dtr.diagram;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Builds a Mermaid {@code flowchart TD} diagram from the Java 26 Code Reflection IR
 * (JEP 516 / Project Babylon) of a method.
 *
 * <p>Each basic block becomes a node. Conditional branch successors become directed edges.
 * Op descriptions (first 2 per block) appear as node labels.</p>
 */
@SuppressWarnings("preview")
public final class ControlFlowGraphBuilder {

    private ControlFlowGraphBuilder() {}

    /**
     * Builds a Mermaid flowchart DSL string from the code model of the given method.
     * Returns an empty string if no code model is available.
     *
     * @param method the method (should be annotated with {@code @CodeReflection})
     * @return Mermaid {@code flowchart TD} DSL, or empty string on failure
     */
    public static String build(Method method) {
        List<CodeModelAnalyzer.BlockInfo> blocks = CodeModelAnalyzer.extractBlocks(method);
        if (blocks.isEmpty()) {
            return "";  // hguard-ok: empty CFG is the correct result when no code model available
        }

        var sb = new StringBuilder("flowchart TD\n");

        // Emit nodes
        for (var block : blocks) {
            String nodeId = "B" + block.index();
            // Use first 2 op descriptions as label content
            List<String> descs = block.opDescs();
            String label = descs.isEmpty() ? "Block " + block.index()
                    : descs.subList(0, Math.min(2, descs.size())).stream()
                            .map(d -> d.length() > 25 ? d.substring(0, 25) : d)
                            .reduce((a, b) -> a + "\\n" + b)
                            .orElse("Block " + block.index());
            sb.append("    ").append(nodeId).append("[\"").append(label).append("\"]\n");
        }

        // Emit edges from branch/successor info
        for (var block : blocks) {
            String fromId = "B" + block.index();
            if (!block.successors().isEmpty()) {
                for (int succ : block.successors()) {
                    sb.append("    ").append(fromId).append(" --> B").append(succ).append("\n");
                }
            } else if (!block.isTerminator()) {
                // Implicit fall-through to next block
                int next = block.index() + 1;
                if (next < blocks.size()) {
                    sb.append("    ").append(fromId).append(" --> B").append(next).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }
}

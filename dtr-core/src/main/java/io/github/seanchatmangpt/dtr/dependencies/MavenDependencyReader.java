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
package io.github.seanchatmangpt.dtr.dependencies;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Maven {@code pom.xml} located in a given project root directory and
 * generates a Mermaid LR dependency graph from its {@code <dependency>} elements.
 *
 * <p>All parsing is performed with the standard JDK {@link DocumentBuilderFactory} —
 * no external XML libraries are required. All exceptions are caught internally;
 * callers always receive a valid {@link DependencyGraphResult} even when the
 * {@code pom.xml} is absent or malformed.</p>
 *
 * <p>Typical usage inside a DTR doc-test:</p>
 * <pre>{@code
 * var result = MavenDependencyReader.read(".");
 * sayMermaid(result.mermaidDsl());
 * sayTable(MavenDependencyReader.toMarkdown(result).toArray(new String[0]));
 * }</pre>
 *
 * @since 2026.3.0
 */
public final class MavenDependencyReader {

    private MavenDependencyReader() {}

    // -------------------------------------------------------------------------
    // Public result types
    // -------------------------------------------------------------------------

    /**
     * A single Maven dependency extracted from a {@code <dependency>} element.
     *
     * @param groupId    the {@code <groupId>} value (never {@code null}, may be empty)
     * @param artifactId the {@code <artifactId>} value (never {@code null}, may be empty)
     * @param version    the {@code <version>} value, or {@code ""} when absent
     * @param scope      the {@code <scope>} value, or {@code ""} when absent (implies compile)
     */
    public record Dependency(String groupId, String artifactId, String version, String scope) {}

    /**
     * The complete result of a single {@link #read(String)} call.
     *
     * @param projectRoot the path that was passed to {@link #read(String)}
     * @param dependencies all dependencies parsed from the pom.xml (empty list on error)
     * @param mermaidDsl  the Mermaid {@code graph LR} DSL string, or an error note
     */
    public record DependencyGraphResult(
            String projectRoot,
            List<Dependency> dependencies,
            String mermaidDsl) {}

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    /**
     * Parses {@code {projectRoot}/pom.xml} and builds a {@link DependencyGraphResult}.
     *
     * <p>The Mermaid graph uses the project's own {@code <artifactId>} as the root
     * node label (read from the top-level project element, not from any dependency
     * element). Each dependency is represented as an edge:
     * {@code ROOT[projectArtifactId] --> DEP_n[artifactId:scope]}.</p>
     *
     * <p>This method never throws. Any {@link Exception} is caught, and the returned
     * result will contain an empty dependency list and a Mermaid comment noting the
     * error.</p>
     *
     * @param projectRoot path to the directory containing {@code pom.xml}
     *                    (e.g. {@code "."} for the Maven working directory)
     * @return a fully populated {@link DependencyGraphResult}; never {@code null}
     */
    public static DependencyGraphResult read(String projectRoot) {
        var pomFile = new File(projectRoot, "pom.xml");

        if (!pomFile.exists()) {
            var note = "graph LR\n  NOTE[\"pom.xml not found at: " + pomFile.getAbsolutePath() + "\"]";
            return new DependencyGraphResult(projectRoot, List.of(), note);
        }

        try {
            var factory = DocumentBuilderFactory.newInstance();
            // Disable external entity resolution for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Suppress noisy SAX parser output to stderr
            builder.setErrorHandler(null);

            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            // Read the project-level artifactId (root node label)
            var projectArtifactId = extractProjectArtifactId(doc);

            // Collect all <dependency> elements from <dependencies> section
            var deps = extractDependencies(doc);

            // Build Mermaid DSL
            var mermaid = buildMermaid(projectArtifactId, deps);

            return new DependencyGraphResult(projectRoot, deps, mermaid);

        } catch (Exception e) {
            var errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            var note = "graph LR\n  ERROR[\"Parse error — " + sanitizeMermaidLabel(errorMsg) + "\"]";
            return new DependencyGraphResult(projectRoot, List.of(), note);
        }
    }

    /**
     * Converts a {@link DependencyGraphResult} into a list of Markdown lines suitable
     * for appending to a documentation document.
     *
     * <p>The output contains:
     * <ol>
     *   <li>A level-3 heading: {@code ### Maven Dependencies: {projectRoot}}</li>
     *   <li>A total count line</li>
     *   <li>A fenced Mermaid code block</li>
     *   <li>A dependency table with columns {@code groupId}, {@code artifactId},
     *       {@code version}, {@code scope}</li>
     * </ol>
     * </p>
     *
     * @param result the result to format; must not be {@code null}
     * @return an unmodifiable list of Markdown lines; never {@code null}
     */
    public static List<String> toMarkdown(DependencyGraphResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Maven Dependencies: " + result.projectRoot());
        lines.add("");
        lines.add("Total: " + result.dependencies().size() + " dependencies");
        lines.add("");

        // Fenced Mermaid code block
        lines.add("```mermaid");
        lines.add(result.mermaidDsl());
        lines.add("```");
        lines.add("");

        // Dependency table
        if (!result.dependencies().isEmpty()) {
            lines.add("| groupId | artifactId | version | scope |");
            lines.add("|---------|------------|---------|-------|");
            for (var dep : result.dependencies()) {
                var version = dep.version().isEmpty() ? "—" : dep.version();
                var scope   = dep.scope().isEmpty()   ? "compile" : dep.scope();
                lines.add("| %s | %s | %s | %s |".formatted(
                        dep.groupId(), dep.artifactId(), version, scope));
            }
            lines.add("");
        }

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String extractProjectArtifactId(Document doc) {
        // The project artifactId is a direct child of the root <project> element,
        // not nested inside <dependencies>. We look at direct children only to
        // avoid picking up a dependency's <artifactId>.
        var root = doc.getDocumentElement();
        var children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            var node = children.item(i);
            if (node instanceof Element el && "artifactId".equals(el.getLocalName() != null
                    ? el.getLocalName() : el.getNodeName())) {
                var text = el.getTextContent().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "project";
    }

    private static List<Dependency> extractDependencies(Document doc) {
        var deps = new ArrayList<Dependency>();
        NodeList depNodes = doc.getElementsByTagName("dependency");

        for (int i = 0; i < depNodes.getLength(); i++) {
            if (depNodes.item(i) instanceof Element depEl) {
                var groupId    = childText(depEl, "groupId");
                var artifactId = childText(depEl, "artifactId");
                var version    = childText(depEl, "version");
                var scope      = childText(depEl, "scope");
                deps.add(new Dependency(groupId, artifactId, version, scope));
            }
        }

        return List.copyOf(deps);
    }

    private static String childText(Element parent, String tagName) {
        var nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) instanceof Element el) {
            return el.getTextContent().trim();
        }
        return "";
    }

    private static String buildMermaid(String projectArtifactId, List<Dependency> deps) {
        var sb = new StringBuilder("graph LR\n");
        var rootLabel = sanitizeMermaidLabel(projectArtifactId);
        sb.append("  ROOT[").append(rootLabel).append("]");

        if (deps.isEmpty()) {
            sb.append("\n  ROOT --> NONE[\"no dependencies\"]");
            return sb.toString();
        }

        sb.append("\n");
        for (int i = 0; i < deps.size(); i++) {
            var dep = deps.get(i);
            var scope = dep.scope().isEmpty() ? "compile" : dep.scope();
            var nodeId = "DEP" + i;
            var nodeLabel = sanitizeMermaidLabel(dep.artifactId() + ":" + scope);
            sb.append("  ROOT --> ").append(nodeId).append("[").append(nodeLabel).append("]\n");
        }

        // Remove trailing newline
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Escapes characters that would break a Mermaid node label.
     * Mermaid labels inside {@code []} cannot contain {@code [}, {@code ]},
     * {@code "}, or raw newlines.
     */
    private static String sanitizeMermaidLabel(String label) {
        if (label == null || label.isEmpty()) {
            return "unknown";
        }
        return label
                .replace("\"", "'")
                .replace("[", "(")
                .replace("]", ")")
                .replace("\n", " ")
                .replace("\r", "");
    }
}

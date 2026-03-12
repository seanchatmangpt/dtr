/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.assembly;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.github.seanchatmangpt.dtr.receipt.LockchainReceipt;

/**
 * Immutable manifest of a complete document assembly combining multiple DocTests.
 */
public record AssemblyManifest(
    List<?> includedTests,
    int totalPages,
    int totalWords,
    int totalCodeListings,
    int totalTables,
    int totalCitations,
    int totalCrossReferences,
    LockchainReceipt assemblyReceipt,
    Map<String, LockchainReceipt> componentReceipts,
    Instant assembledAt,
    String sha3HashOfManifest
) {

    /**
     * Validates the manifest on construction.
     */
    public AssemblyManifest {
        if (includedTests == null || includedTests.isEmpty()) {
            throw new IllegalArgumentException("includedTests cannot be null or empty");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must be non-negative");
        }
        if (totalWords < 0) {
            throw new IllegalArgumentException("totalWords must be non-negative");
        }
        if (assemblyReceipt == null) {
            throw new IllegalArgumentException("assemblyReceipt cannot be null");
        }
        if (componentReceipts == null) {
            throw new IllegalArgumentException("componentReceipts cannot be null");
        }
        if (assembledAt == null) {
            throw new IllegalArgumentException("assembledAt cannot be null");
        }
        if (sha3HashOfManifest == null || sha3HashOfManifest.trim().isEmpty()) {
            throw new IllegalArgumentException("sha3HashOfManifest cannot be null or empty");
        }
    }

    /**
     * Returns a machine-readable JSON representation of this manifest.
     */
    public String toJson() {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"assembledAt\": \"").append(assembledAt).append("\",\n");
        sb.append("  \"sha3HashOfManifest\": \"").append(sha3HashOfManifest).append("\",\n");
        sb.append("  \"statistics\": {\n");
        sb.append("    \"totalPages\": ").append(totalPages).append(",\n");
        sb.append("    \"totalWords\": ").append(totalWords).append(",\n");
        sb.append("    \"totalCodeListings\": ").append(totalCodeListings).append(",\n");
        sb.append("    \"totalTables\": ").append(totalTables).append(",\n");
        sb.append("    \"totalCitations\": ").append(totalCitations).append(",\n");
        sb.append("    \"totalCrossReferences\": ").append(totalCrossReferences).append("\n");
        sb.append("  },\n");
        sb.append("  \"includedTests\": ").append(includedTests.size()).append(",\n");
        sb.append("  \"assemblyReceipt\": ").append(assemblyReceipt.toJson()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Returns human-readable colophon page content for the assembled document.
     */
    public String toColophonText() {
        return """
            \\newpage
            \\vspace*{\\fill}

            \\begin{center}
            \\noindent\\rule{\\textwidth}{1pt}
            \\textbf{\\Large ASSEMBLY MANIFEST}
            \\noindent\\rule{\\textwidth}{1pt}
            \\end{center}

            \\small
            \\begin{tabular}{ll}
            \\textbf{Assembled:} & %s \\\\
            \\textbf{Total Pages:} & %d \\\\
            \\textbf{Total Words:} & %,d \\\\
            \\textbf{Code Listings:} & %d \\\\
            \\textbf{Tables/Figures:} & %d \\\\
            \\textbf{Citations:} & %d \\\\
            \\textbf{Cross-References:} & %d \\\\
            \\textbf{Included DocTests:} & %d \\\\
            \\end{tabular}

            \\vspace{0.5cm}

            \\noindent
            This assembled document combines multiple DocTest executions into
            a unified, cross-referenced specification.

            \\begin{center}
            \\textit{Complete assembly integration complete}
            \\end{center}

            \\vspace*{\\fill}
            """.formatted(
            assembledAt,
            totalPages,
            totalWords,
            totalCodeListings,
            totalTables,
            totalCitations,
            totalCrossReferences,
            includedTests.size()
        );
    }
}

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
package io.github.seanchatmangpt.dtr.rendermachine.latex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pandoc-based PDF compilation strategy.
 *
 * Fallback compiler when pdflatex, latexmk, and xelatex are unavailable.
 * Pandoc provides reduced-fidelity PDF: BibTeX, cross-references, and
 * two-column layouts will not work, but basic LaTeX compilation succeeds.
 *
 * This strategy logs warnings about lost features but does NOT fail the build
 * if Pandoc is unavailable or compilation fails.
 */
public final class PandocStrategy implements CompilerStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PandocStrategy.class);

    private static final String PANDOC_BINARY = "pandoc";
    private static final String WARNING_BIBTEX = "Bibliography not resolved (BibTeX unavailable in Pandoc)";
    private static final String WARNING_XREFS = "Cross-references not resolved (will appear as ??)";
    private static final String WARNING_COLUMNS = "Two-column layout not supported (appears as single column)";
    private static final String WARNING_WORDCOUNT = "Word count not enforced (Pandoc produces full document)";

    @Override
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PANDOC_BINARY, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void compile(Path texFile, Path outputDir) throws IOException, InterruptedException {
        if (!Files.exists(texFile) || !texFile.toString().endsWith(".tex")) {
            logger.warn("Invalid TeX file: {}", texFile.toAbsolutePath());
            throw new IOException("Invalid TeX file: " + texFile.toAbsolutePath());
        }

        logDegradationWarnings();

        Path pdfOutputPath = outputDir.resolve(texFile.getFileName().toString()
                .replaceAll("\\.tex$", ".pdf"));

        ProcessBuilder pb = buildPandocCommand(texFile, pdfOutputPath);
        pb.directory(outputDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();

        if (exitCode != 0) {
            logger.warn("Pandoc compilation exited with code {}. Output:\n{}",
                    exitCode, output);
            // Non-fatal: do not throw exception
            return;
        }

        if (!Files.exists(pdfOutputPath) || Files.size(pdfOutputPath) == 0) {
            logger.warn("Pandoc PDF output missing or empty: {}", pdfOutputPath.toAbsolutePath());
            // Non-fatal: do not throw exception
            return;
        }

        logger.info("Successfully compiled {} to PDF via Pandoc", texFile.getFileName());
    }

    @Override
    public String getName() {
        return "Pandoc";
    }

    /**
     * Build the Pandoc command with appropriate engine selection.
     */
    private ProcessBuilder buildPandocCommand(Path texFile, Path pdfOutput) {
        String pdfEngine = selectPdfEngine();
        return new ProcessBuilder(
                PANDOC_BINARY,
                "--from", "latex",
                "--to", "pdf",
                "--pdf-engine=" + pdfEngine,
                "-o", pdfOutput.toAbsolutePath().toString(),
                texFile.toAbsolutePath().toString()
        );
    }

    /**
     * Select the best available PDF engine.
     */
    private String selectPdfEngine() {
        for (String engine : new String[]{"xelatex", "pdflatex"}) {
            if (isPdfEngineAvailable(engine)) {
                logger.debug("Using Pandoc with PDF engine: {}", engine);
                return engine;
            }
        }
        // Fallback: Pandoc will use its default (usually pdflatex)
        logger.debug("No explicit PDF engine available; Pandoc will use default");
        return "pdflatex";
    }

    /**
     * Check if a PDF engine (xelatex, pdflatex) is available.
     */
    private boolean isPdfEngineAvailable(String engine) {
        try {
            ProcessBuilder pb = new ProcessBuilder(engine, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Log warnings about Pandoc's reduced fidelity.
     */
    private void logDegradationWarnings() {
        logger.warn("=== Pandoc PDF Compilation (Reduced Fidelity) ===");
        logger.warn(WARNING_BIBTEX);
        logger.warn(WARNING_XREFS);
        logger.warn(WARNING_COLUMNS);
        logger.warn(WARNING_WORDCOUNT);
        logger.warn("=== Some LaTeX features unavailable ===");
    }
}

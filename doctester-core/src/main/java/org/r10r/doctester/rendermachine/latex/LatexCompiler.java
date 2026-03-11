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
package org.r10r.doctester.rendermachine.latex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaTeX to PDF compiler with fallback strategy chain.
 *
 * Attempts to compile .tex files to PDF using available LaTeX tools in
 * priority order:
 * 1. latexmk (recommended; handles multipass compilation, aux cleanup)
 * 2. pdflatex (fallback; basic direct compilation)
 * 3. xelatex (last resort; modern Unicode support)
 *
 * If all compilers fail or none are available, does NOT throw exception —
 * instead logs warning and continues. This allows Markdown-only documentation
 * to still be generated even if PDF compilation unavailable.
 */
public class LatexCompiler {

    private static final Logger logger = LoggerFactory.getLogger(LatexCompiler.class);

    private static final String[] COMPILER_CHAIN = {
        "latexmk",
        "pdflatex",
        "xelatex"
    };

    /**
     * Compile a .tex file to PDF using available LaTeX compilers.
     *
     * Tries compilers in priority order. Returns early on first success.
     * Logs warnings for each failure but does not throw exception if all fail.
     *
     * @param texFile the .tex source file to compile
     * @return true if compilation succeeded, false if all compilers unavailable
     */
    public boolean compile(File texFile) {
        if (!texFile.exists() || !texFile.getName().endsWith(".tex")) {
            logger.warn("Invalid TeX file: {}", texFile.getAbsolutePath());
            return false;
        }

        for (String compiler : COMPILER_CHAIN) {
            try {
                if (isBinaryAvailable(compiler)) {
                    logger.info("Using {} for LaTeX compilation", compiler);
                    invokeBinary(compiler, texFile);
                    validatePdfOutput(texFile);
                    logger.info("Successfully compiled: {}", texFile.getName());
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Compiler {} failed, trying next: {}", compiler, e.getMessage());
                continue;
            }
        }

        logger.warn(
            "No LaTeX compiler available (checked: latexmk, pdflatex, xelatex). "
            + "PDF generation skipped. Markdown documentation still available.");
        return false;
    }

    /**
     * Check if a binary is available in PATH.
     */
    private boolean isBinaryAvailable(String binary) {
        try {
            ProcessBuilder pb = new ProcessBuilder(binary, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Invoke LaTeX compiler binary with standard flags.
     */
    private void invokeBinary(String compiler, File texFile) throws IOException, InterruptedException {
        ProcessBuilder pb;

        if ("latexmk".equals(compiler)) {
            // latexmk handles multipass compilation and cleanup
            pb = new ProcessBuilder(
                compiler,
                "-pdf",
                "-interaction=nonstopmode",
                "-halt-on-error",
                "-file-line-error",
                texFile.getAbsolutePath()
            );
        } else {
            // pdflatex, xelatex
            pb = new ProcessBuilder(
                compiler,
                "-interaction=nonstopmode",
                "-halt-on-error",
                "-file-line-error",
                "-pdf",
                texFile.getAbsolutePath()
            );
        }

        pb.directory(texFile.getParentFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();

        // Capture output for logging
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        int exitCode = p.waitFor();

        if (exitCode != 0) {
            logger.debug("Compiler output:\n{}", output);
            throw new LatexCompilationException(
                "Compiler %s exited with code %d".formatted(compiler, exitCode)
            );
        }
    }

    /**
     * Validate that PDF output file exists and is non-empty.
     */
    private void validatePdfOutput(File texFile) throws IOException {
        String pdfPath = texFile.getAbsolutePath().replaceAll("\\.tex$", ".pdf");
        File pdfFile = new File(pdfPath);

        if (!pdfFile.exists() || pdfFile.length() == 0) {
            throw new IOException("PDF output missing or empty: " + pdfPath);
        }
    }

    /**
     * Exception thrown when LaTeX compilation fails.
     */
    public static class LatexCompilationException extends RuntimeException {
        public LatexCompilationException(String message) {
            super(message);
        }

        public LatexCompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

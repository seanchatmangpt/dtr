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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaTeX compilation strategy using latexmk.
 *
 * Recommended compiler: handles multipass compilation, auxiliary file cleanup,
 * bibliography generation, and cross-reference resolution automatically.
 */
public class LatexmkStrategy implements CompilerStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LatexmkStrategy.class);

    @Override
    public boolean isAvailable() {
        return isBinaryAvailable("latexmk");
    }

    @Override
    public void compile(Path texFile, Path outputDir) throws IOException, InterruptedException {
        if (!Files.exists(texFile) || !texFile.toString().endsWith(".tex")) {
            throw new IOException("Invalid TeX file: " + texFile.toAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(
                "latexmk",
                "-pdf",
                "-interaction=nonstopmode",
                "-halt-on-error",
                "-file-line-error",
                texFile.toAbsolutePath().toString()
        );

        pb.directory(outputDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();

        if (exitCode != 0) {
            logger.debug("latexmk output:\n{}", output);
            throw new IOException("latexmk exited with code " + exitCode);
        }

        validatePdfOutput(texFile, outputDir);
    }

    @Override
    public String getName() {
        return "latexmk";
    }

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

    private void validatePdfOutput(Path texFile, Path outputDir) throws IOException {
        String pdfName = texFile.getFileName().toString().replaceAll("\\.tex$", ".pdf");
        Path pdfFile = outputDir.resolve(pdfName);

        if (!Files.exists(pdfFile) || Files.size(pdfFile) == 0) {
            throw new IOException("PDF output missing or empty: " + pdfFile.toAbsolutePath());
        }
    }
}

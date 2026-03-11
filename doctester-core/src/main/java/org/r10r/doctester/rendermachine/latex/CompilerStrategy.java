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
import java.nio.file.Path;

/**
 * Strategy for compiling LaTeX files to PDF.
 *
 * Implementations provide different compilation approaches:
 * - LatexmkStrategy: handles multipass compilation, aux cleanup
 * - PdflatexStrategy: direct compilation
 * - XelatexStrategy: modern Unicode support
 * - LatexmkStrategy: intelligent multipass compilation
 *
 * Java 26 Enhancement (JEP 500 - Final Means Final):
 * This interface is sealed to only allow the specified implementations.
 * This enables the JVM to make static analysis guarantees:
 * - Devirtualization of method calls (faster dispatch)
 * - Exhaustive pattern matching over sealed types
 * - Preparation for Valhalla value class flattening (no pointer indirection)
 *
 * Each strategy reports availability via isAvailable() and
 * handles compilation errors gracefully.
 */
public sealed interface CompilerStrategy
    permits PdflatexStrategy, XelatexStrategy, LatexmkStrategy, PandocStrategy {

    /**
     * Check if this compiler strategy is available in the system PATH.
     *
     * @return true if the required binary(ies) are available
     */
    boolean isAvailable();

    /**
     * Compile a LaTeX file to PDF.
     *
     * @param texFile the input .tex file path
     * @param outputDir the directory where PDF should be written
     * @throws IOException if file operations fail
     * @throws InterruptedException if compilation process is interrupted
     */
    void compile(Path texFile, Path outputDir) throws IOException, InterruptedException;

    /**
     * Get a human-readable name for this compiler strategy.
     *
     * @return strategy name (e.g., "latexmk", "Pandoc")
     */
    String getName();
}

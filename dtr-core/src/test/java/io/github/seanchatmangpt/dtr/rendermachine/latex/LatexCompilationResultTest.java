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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Test for LatexCompilationResult record.
 */
class LatexCompilationResultTest {

    @Test
    void testSuccessfulCompilationResult() {
        LatexCompilationResult result = new LatexCompilationResult(
            true,
            "latexmk",
            List.of("Output line 1", "Output line 2"),
            List.of(),
            Instant.now(),
            Duration.ofMillis(1234),
            "/path/to/working/dir"
        );

        assertThat(result.success(), is(true));
        assertThat(result.command(), is("latexmk"));
        assertThat(result.duration().toMillis(), is(1234L));
        assertThat(result.workingDirectory(), is("/path/to/working/dir"));

        String summary = result.getDiagnosticSummary();
        assertThat(summary, containsString("SUCCEEDED"));
        assertThat(summary, containsString("latexmk"));
        assertThat(summary, containsString("1234ms"));
    }

    @Test
    void testFailedCompilationResult() {
        LatexCompilationResult result = new LatexCompilationResult(
            false,
            "pdflatex",
            List.of("Processing document..."),
            List.of("! LaTeX Error: File `graphicx.sty' not found"),
            Instant.now(),
            Duration.ofMillis(500),
            "/tmp/latex"
        );

        assertThat(result.success(), is(false));
        assertThat(result.errors().size(), is(1));

        String summary = result.getDiagnosticSummary();
        assertThat(summary, containsString("FAILED"));
        assertThat(summary, containsString("Errors (1):"));
        assertThat(summary, containsString("! LaTeX Error: File `graphicx.sty' not found"));
        assertThat(summary, containsString("Output (last 10 lines):"));
    }

    @Test
    void testDiagnosticSummaryWithLargeOutput() {
        // Create result with 20 output lines
        List<String> largeOutput = IntStream.range(0, 20)
            .mapToObj(i -> "Output line " + i)
            .toList();

        LatexCompilationResult result = new LatexCompilationResult(
            false,
            "xelatex",
            largeOutput,
            List.of("! Compilation failed"),
            Instant.now(),
            Duration.ofSeconds(2),
            "/work/dir"
        );

        String summary = result.getDiagnosticSummary();
        // Should show last 10 lines only
        assertThat(summary, containsString("Output line 10"));
        assertThat(summary, containsString("Output line 19"));
        // First lines should not be in summary
        assertThat(summary, not(containsString("Output line 0")));
        assertThat(summary, not(containsString("Output line 9")));
    }
}

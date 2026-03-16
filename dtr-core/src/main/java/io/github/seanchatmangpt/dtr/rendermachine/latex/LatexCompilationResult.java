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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Record representing the result of a LaTeX compilation attempt.
 *
 * Captures comprehensive diagnostics including success status, command executed,
 * output streams, timing information, and working directory context.
 */
public record LatexCompilationResult(
    boolean success,
    String command,
    List<String> output,
    List<String> errors,
    Instant startTime,
    Duration duration,
    String workingDirectory
) {
    /**
     * Generates a human-readable diagnostic summary of the compilation attempt.
     *
     * Includes success status, command executed, duration, and any errors encountered.
     *
     * @return formatted diagnostic summary string
     */
    public String getDiagnosticSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("LaTeX Compilation ").append(success ? "SUCCEEDED" : "FAILED").append("\n");
        sb.append("Command: ").append(command).append("\n");
        sb.append("Duration: ").append(duration.toMillis()).append("ms\n");
        sb.append("Working Directory: ").append(workingDirectory).append("\n");
        if (!errors.isEmpty()) {
            sb.append("Errors (").append(errors.size()).append("):\n");
            errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
        }
        if (!output.isEmpty() && !success) {
            sb.append("Output (last 10 lines):\n");
            output.stream()
                .skip(Math.max(0, output.size() - 10))
                .forEach(line -> sb.append("  ").append(line).append("\n"));
        }
        return sb.toString();
    }
}

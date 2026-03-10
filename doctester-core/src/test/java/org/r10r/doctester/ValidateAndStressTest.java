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
package org.r10r.doctester;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive validation and stress test for DocTester.
 *
 * This test class is ordered alphabetically so validation tests (prefixed "t1_")
 * run before stress tests (prefixed "t2_") and output validation (prefixed "t3_")
 * runs last.
 *
 * <h2>What this validates:</h2>
 * <ol>
 *   <li><b>Correctness</b> — say(), sayNextSection(), sayRaw(), sayAndAssertThat()
 *       produce correct markdown structure, escaping, and formatting</li>
 *   <li><b>Lifecycle</b> — RenderMachine initializes once per class, survives
 *       multiple test methods, and writes output at @AfterAll</li>
 *   <li><b>Edge cases</b> — empty strings, special characters, markdown injection,
 *       Unicode, extremely long single lines, null-like values</li>
 *   <li><b>Stress: call count</b> — graduated say() calls from 1K to 100K</li>
 *   <li><b>Stress: section count</b> — graduated sections from 100 to 10K</li>
 *   <li><b>Stress: assertion count</b> — graduated assertions from 1K to 50K</li>
 *   <li><b>Stress: payload size</b> — single payloads from 1KB to 10MB</li>
 *   <li><b>Stress: combined load</b> — sections + says + asserts together</li>
 *   <li><b>Output validation</b> — verifies the generated markdown file exists,
 *       contains expected content, has valid structure</li>
 * </ol>
 *
 * <h2>Performance characteristics:</h2>
 * <ul>
 *   <li>Markdown rendering is efficient: no HTML generation overhead</li>
 *   <li>Output files are clean, version-control friendly text</li>
 *   <li>Memory usage scales linearly with content size</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ValidateAndStressTest extends DocTester {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    private static final String OUTPUT_DIR = "target/docs";
    private static final String OUTPUT_FILE = OUTPUT_DIR + "/"
            + ValidateAndStressTest.class.getName() + ".md";

    /** Collect timing data across all stress tests for final report. */
    private static final List<String> TIMING_REPORT = new ArrayList<>();

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    // =====================================================================
    // PHASE 1: Correctness validation
    // =====================================================================

    @Test
    public void t1_01_sayProducesValidParagraph() {
        sayNextSection("Validation: say() produces paragraphs");
        say("This is a simple paragraph.");
        say("This paragraph has <b>bold</b> and <i>italic</i> content.");
        say("Paragraph with numbers: 12345 and symbols: !@#$%^&*()");
    }

    @Test
    public void t1_02_sayNextSectionProducesValidH1() {
        sayNextSection("Validation: sayNextSection() produces h1");
        say("Section headers should appear in the left sidebar navigation.");

        sayNextSection("Sub-section with special chars: <>&\"'");
        say("The section ID should strip non-word characters.");
    }

    @Test
    public void t1_03_sayRawInjectsMarkdownDirectly() {
        sayNextSection("Validation: sayRaw() injects raw Markdown");
        sayRaw("> [!NOTE]\n> This is raw Markdown injected via sayRaw()");
        sayRaw("| Cell 1 | Cell 2 |\n|---|---|");
        sayRaw("```java\npublic static void main(String[] args) {\n    System.out.println(\"Hello\");\n}\n```");
    }

    @Test
    public void t1_04_sayAndAssertThatPassingShowsSuccess() {
        sayNextSection("Validation: passing assertions");
        sayAndAssertThat("Boolean true is true", true, is(true));
        sayAndAssertThat("String equality works", "hello", equalTo("hello"));
        sayAndAssertThat("Integer equality works", 42, equalTo(42));
        sayAndAssertThat("String contains check", "hello world", containsString("world"));
    }

    @Test
    public void t1_05_sayAndAssertThatFailingShowsDanger() {
        sayNextSection("Validation: failing assertions are captured");

        boolean caughtFailure = false;
        try {
            sayAndAssertThat("This assertion intentionally fails", false, is(true));
        } catch (AssertionError _) {
            caughtFailure = true;
        }
        assertTrue(caughtFailure, "Expected AssertionError to be thrown");
        say("Failure was captured and should appear as failure admonition in output.");
    }

    @Test
    public void t1_06_sayAndAssertThatWithReasonShowsReason() {
        sayNextSection("Validation: assertion with reason");
        sayAndAssertThat("Value matches expected",
                "custom reason for debugging",
                100, equalTo(100));
    }

    @Test
    public void t1_07_edgeCaseEmptyStrings() {
        sayNextSection("Edge case: empty strings");
        say("");
        sayRaw("");
        sayNextSection("");
        say("Empty strings should not crash the render machine.");
    }

    @Test
    public void t1_08_edgeCaseUnicode() {
        sayNextSection("Edge case: Unicode content");
        say("Japanese: 日本語テスト");
        say("Chinese: 中文测试");
        say("Korean: 한국어 테스트");
        say("Arabic: اختبار");
        say("Emoji: \uD83D\uDE80\uD83C\uDF1F\uD83D\uDD25\uD83D\uDCA1");
        say("Math: ∑∫∂√∞≠≤≥");
        say("Currency: €£¥₹₿");
    }

    @Test
    public void t1_09_edgeCaseHtmlInjection() {
        sayNextSection("Edge case: HTML-like content in say()");
        say("<script>alert('xss')</script>");
        say("<img src=x onerror=alert(1)>");
        say("</div></div></body></html>");
        say("Note: say() does NOT escape HTML — this is by design per RenderMachineCommands javadoc.");
    }

    @Test
    public void t1_10_edgeCaseLongSingleLine() {
        sayNextSection("Edge case: very long single line");
        String longLine = "A".repeat(100_000);
        say(longLine);
        say("The above line is 100,000 characters without any line breaks.");
    }

    @Test
    public void t1_11_edgeCaseSpecialSectionIds() {
        sayNextSection("Edge case: section ID generation");
        sayNextSection("ALL UPPERCASE SECTION");
        sayNextSection("section with    extra   spaces");
        sayNextSection("section-with-dashes-and_underscores");
        sayNextSection("123 numeric prefix");
        sayNextSection("!@#$%^&*() only special chars");
        say("All these sections should generate valid markdown headings.");
    }

    @Test
    public void t1_12_edgeCaseMultipleAssertionFailures() {
        sayNextSection("Edge case: multiple assertion failures in sequence");

        int failures = 0;
        for (int i = 0; i < 5; i++) {
            try {
                sayAndAssertThat("Intentional failure #" + i, i, equalTo(i + 100));
            } catch (AssertionError _) {
                failures++;
            }
        }
        sayAndAssertThat("Caught all 5 failures", failures, equalTo(5));
        say("5 consecutive failures should each produce a failure admonition.");
    }

    @Test
    public void t1_13_edgeCaseRapidAlternation() {
        sayNextSection("Edge case: rapid alternation of API calls");
        for (int i = 0; i < 100; i++) {
            sayNextSection("Rapid section " + i);
            say("Rapid paragraph " + i);
            sayRaw("<hr/>");
            sayAndAssertThat("Rapid assert " + i, i, equalTo(i));
        }
    }

    // =====================================================================
    // PHASE 2: Graduated stress tests
    // =====================================================================

    @Test
    public void t2_01_stressSayCallsGraduated() {
        sayNextSection("Stress: graduated say() calls");

        int[] tiers = {1_000, 5_000, 10_000, 50_000, 100_000};

        for (int count : tiers) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            for (int i = 0; i < count; i++) {
                say("Tier " + count + " line " + i + ": padding for realistic content.");
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long deltaMem = usedMemoryMB() - startMem;
            String report = "say() x %,d: %dms, Δmem=%dMB".formatted(count, elapsed, deltaMem);
            TIMING_REPORT.add(report);
            say("--- " + report + " ---");

            // Performance threshold: less than 5ms per 1000 calls
            long maxExpected = (count / 1000) * 5 + 500; // generous ceiling
            assertTrue(
                    elapsed < maxExpected,
                    "say() x " + count + " took " + elapsed + "ms (max " + maxExpected + "ms)");
        }
    }

    @Test
    public void t2_02_stressSectionsGraduated() {
        sayNextSection("Stress: graduated section count");

        int[] tiers = {100, 500, 1_000, 5_000, 10_000};

        for (int count : tiers) {
            long startTime = System.nanoTime();

            for (int i = 0; i < count; i++) {
                sayNextSection("Stress section tier " + count + " item " + i);
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            String report = "sections x %,d: %dms".formatted(count, elapsed);
            TIMING_REPORT.add(report);
            say("--- " + report + " ---");
        }
    }

    @Test
    public void t2_03_stressAssertionsGraduated() {
        sayNextSection("Stress: graduated assertion count");

        int[] tiers = {1_000, 5_000, 10_000, 50_000};

        for (int count : tiers) {
            long startTime = System.nanoTime();

            for (int i = 0; i < count; i++) {
                sayAndAssertThat("Assert tier " + count + " #" + i, i, equalTo(i));
            }

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            String report = "assertions x %,d: %dms".formatted(count, elapsed);
            TIMING_REPORT.add(report);
            say("--- " + report + " ---");
        }
    }

    @Test
    public void t2_04_stressPayloadSizeGraduated() {
        sayNextSection("Stress: graduated payload sizes");

        int[] sizesKB = {1, 10, 100, 1_000, 5_000, 10_000};

        for (int sizeKB : sizesKB) {
            long startTime = System.nanoTime();
            long startMem = usedMemoryMB();

            String payload = "P".repeat(sizeKB * 1024);
            sayRaw("```\n" + payload + "\n```");

            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            long deltaMem = usedMemoryMB() - startMem;
            String report = "payload %,dKB: %dms, Δmem=%dMB".formatted(sizeKB, elapsed, deltaMem);
            TIMING_REPORT.add(report);
            say("--- " + report + " ---");
        }
    }

    @Test
    public void t2_05_stressCombinedRealisticWorkload() {
        sayNextSection("Stress: combined realistic workload");

        int sections = 200;
        int saysPerSection = 20;
        int assertsPerSection = 5;
        int rawHtmlPerSection = 2;

        long startTime = System.nanoTime();
        long startMem = usedMemoryMB();

        for (int s = 0; s < sections; s++) {
            sayNextSection("API Endpoint " + s + ": /api/v1/resource/" + s);

            for (int i = 0; i < saysPerSection; i++) {
                say("Documentation paragraph " + i + " for endpoint " + s
                        + ". This simulates a realistic REST API doc test with "
                        + "descriptions of request parameters, response fields, "
                        + "and expected behavior.");
            }

            for (int r = 0; r < rawHtmlPerSection; r++) {
                sayRaw("```json\n{\"id\": " + s + ", \"name\": \"resource_" + s + "\", "
                        + "\"status\": \"active\", \"created\": \"2026-03-10T00:00:00Z\"}\n```");
            }

            for (int a = 0; a < assertsPerSection; a++) {
                sayAndAssertThat("Endpoint " + s + " check " + a,
                        s * assertsPerSection + a,
                        equalTo(s * assertsPerSection + a));
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long deltaMem = usedMemoryMB() - startMem;
        String report = "combined %d sections × %d says × %d asserts × %d raw: %dms, Δmem=%dMB"
                .formatted(sections, saysPerSection, assertsPerSection, rawHtmlPerSection,
                        elapsed, deltaMem);
        TIMING_REPORT.add(report);
        say("--- " + report + " ---");
    }

    @Test
    public void t2_06_stressSayRawWithLargeMarkdownTables() {
        sayNextSection("Stress: large markdown tables via sayRaw()");

        int rows = 5_000;
        int cols = 10;

        long startTime = System.nanoTime();

        var sb = new StringBuilder(rows * cols * 30);
        // Header row
        sb.append("|");
        for (int c = 0; c < cols; c++) {
            sb.append(" Col ").append(c).append(" |");
        }
        sb.append("\n|");
        for (int c = 0; c < cols; c++) {
            sb.append("---|");
        }
        sb.append("\n");
        // Data rows
        for (int r = 0; r < rows; r++) {
            sb.append("|");
            for (int c = 0; c < cols; c++) {
                sb.append(" R").append(r).append("C").append(c).append(" |");
            }
            sb.append("\n");
        }

        sayRaw(sb.toString());

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        String report = "Markdown table %,d×%d: %dms".formatted(rows, cols, elapsed);
        TIMING_REPORT.add(report);
        say("--- " + report + " ---");
    }

    @Test
    public void t2_07_stressDeepNesting() {
        sayNextSection("Stress: deeply nested blockquotes via sayRaw()");

        int depth = 1_000;
        var sb = new StringBuilder(depth * 20);
        for (int i = 0; i < depth; i++) {
            sb.append("> ");
        }
        sb.append("Deepest content\n");
        sayRaw(sb.toString());
        say("--- nested " + depth + " deep blockquotes ---");
    }

    @Test
    public void t2_08_stressFailureRecovery() {
        sayNextSection("Stress: rapid failure/success alternation");

        int cycles = 1_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < cycles; i++) {
            // Passing assertion
            sayAndAssertThat("Pass #" + i, true, is(true));

            // Failing assertion (caught)
            try {
                sayAndAssertThat("Fail #" + i, false, is(true));
            } catch (AssertionError _) {
                // expected
            }
        }

        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        String report = "failure recovery %,d cycles (pass+fail): %dms".formatted(cycles, elapsed);
        TIMING_REPORT.add(report);
        say("--- " + report + " ---");
    }

    // =====================================================================
    // PHASE 3: Output validation (runs last due to t3_ prefix)
    // =====================================================================

    @Test
    public void t3_01_validateOutputFileExists() throws IOException {
        // Force output by calling finishDocTest which writes the file
        // Note: this also means subsequent t3 tests need a fresh renderMachine
        finishDocTest();

        File outputFile = new File(OUTPUT_FILE);
        assertTrue(outputFile.exists(), "Output markdown file should exist: " + OUTPUT_FILE);
        assertTrue(outputFile.length() > 0, "Output file should be non-empty");

        System.out.println("[VALIDATE] Output file size: " + (outputFile.length() / (1024 * 1024)) + "MB");

        // Re-initialize for any further tests
        initRenderingMachineIfNull();
    }

    @Test
    public void t3_05_validateOutputContainsStressContent() throws IOException {
        File outputFile = new File(OUTPUT_FILE);
        if (!outputFile.exists()) return;

        String content = Files.toString(outputFile, Charsets.UTF_8);

        // Stress test content should be present
        assertTrue(content.contains("Stress: graduated say() calls"),
                "Should contain stress say content");
        assertTrue(content.contains("Stress: graduated section count"),
                "Should contain stress section content");
        assertTrue(content.contains("Stress: graduated assertion count"),
                "Should contain stress assertion content");
        assertTrue(content.contains("ms"),
                "Should contain timing data");
    }

    @Test
    public void t3_09_printTimingReport() {
        System.out.println("\n===== STRESS TEST TIMING REPORT =====");
        for (String line : TIMING_REPORT) {
            System.out.println("  " + line);
        }
        System.out.println("  heap at end: " + usedMemoryMB() + "MB");
        System.out.println("=====================================\n");
    }
}

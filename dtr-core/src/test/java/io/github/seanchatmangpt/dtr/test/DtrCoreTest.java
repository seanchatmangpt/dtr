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
package io.github.seanchatmangpt.dtr;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core DTR API tests.
 *
 * <p>Joe Armstrong quality principles applied:
 * <ul>
 *   <li><strong>Every say* output has structural invariants</strong> — not just "it ran", but
 *       "the output contains valid Markdown structure"</li>
 *   <li><strong>Boundary and Unicode cases are tested</strong> — adversarial inputs must not
 *       corrupt output files</li>
 *   <li><strong>Concurrency is always tested</strong> — the say* API must be safe under virtual
 *       thread concurrency on isolated instances</li>
 * </ul>
 */
public class DtrCoreTest extends DtrTest {

    public static String EXPECTED_FILENAME = DtrCoreTest.class.getName() + ".md";

    // =========================================================================
    // Sealed scenario types — Fortune 5 domain modeling for test cases
    // =========================================================================

    /** Sealed hierarchy of test scenarios for systematic coverage. */
    sealed interface TestScenario permits HappyPath, EdgeCase {}

    /** A scenario that should succeed and produce a specific Markdown fragment. */
    record HappyPath(String desc, String expectedFragment) implements TestScenario {}

    /** A scenario with boundary/adversarial input that must not corrupt output. */
    record EdgeCase(String desc, String input, String expectedBehavior) implements TestScenario {}

    /** A named Markdown invariant: a structural property the output must satisfy. */
    record MarkdownInvariant(String name, Predicate<String> check) {}

    @Test
    public void testThatIndexFileWritingWorks() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedIndex = new File("docs/test/README.md");

        Assertions.assertTrue(expectedIndex.exists());

        // README.md is the index, verify it contains the expected header
        assertThatFileContainsText(expectedIndex, "API Documentation");

    }

    @Test
    public void testThatIndexWritingOutDoctestFileWorks() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + EXPECTED_FILENAME);
        File expectedIndexFile = new File("docs/test/README.md");

        // just a simple test to make sure the name is written somewhere in the file.
        assertThatFileContainsText(expectedDoctestfile, DtrCoreTest.class.getSimpleName());

        // just a simple test to make sure that README.md contains a "link" to the doctest file.
        assertThatFileContainsText(expectedIndexFile, EXPECTED_FILENAME);

    }

    @Test
    public void testThatMarkdownOutputContainsExpectedContent() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + EXPECTED_FILENAME);

        // Verify the markdown file was created and contains test content
        assertThatFileContainsText(expectedDoctestfile, "another fun heading!");
        assertThatFileContainsText(expectedDoctestfile, "and a very long text...!");

    }

    @Test
    public void testThatSayTableGeneratesMarkdownTable() throws Exception {

        sayNextSection("Feature Comparison");
        say("The following table demonstrates sayTable() markdown output:");

        sayTable(new String[][] {
            {"Feature", "Status", "Version"},
            {"Extended say* API", "✓ Available", "v2.0.0"},
            {"Markdown output", "✓ Available", "v2.0.0"},
            {"Java 25 support", "✓ Available", "v2.0.0"}
        });

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify markdown table structure
        assertThatFileContainsText(expectedDoctestfile, "| Feature | Status | Version |");
        assertThatFileContainsText(expectedDoctestfile, "| --- |");
        assertThatFileContainsText(expectedDoctestfile, "| Extended say* API | ✓ Available | v2.0.0 |");

    }

    @Test
    public void testThatSayCodeGeneratesFencedCodeBlock() throws Exception {

        sayNextSection("Code Examples");
        say("Here is a SQL query example:");

        sayCode("SELECT id, name FROM users WHERE active = true;", "sql");

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify code block structure
        assertThatFileContainsText(expectedDoctestfile, "```sql");
        assertThatFileContainsText(expectedDoctestfile, "SELECT id, name FROM users WHERE active = true;");
        assertThatFileContainsText(expectedDoctestfile, "```");

    }

    @Test
    public void testThatSayWarningGeneratesAlertBox() throws Exception {

        sayNextSection("Important Notes");
        sayWarning("This is a critical warning that must be visible!");

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify warning alert structure
        assertThatFileContainsText(expectedDoctestfile, "> [!WARNING]");
        assertThatFileContainsText(expectedDoctestfile, "> This is a critical warning that must be visible!");

    }

    @Test
    public void testThatSayNoteGeneratesInfoBox() throws Exception {

        sayNextSection("Tips");
        sayNote("This is a helpful tip for users.");

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify note alert structure
        assertThatFileContainsText(expectedDoctestfile, "> [!NOTE]");
        assertThatFileContainsText(expectedDoctestfile, "> This is a helpful tip for users.");

    }

    @Test
    public void testThatSayKeyValueGeneratesMetadataTable() throws Exception {

        sayNextSection("Configuration");
        sayKeyValue(Map.of(
            "API Version", "v2",
            "Base URL", "https://api.example.com",
            "Timeout", "30 seconds"
        ));

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify key-value table structure
        assertThatFileContainsText(expectedDoctestfile, "| Key | Value |");
        assertThatFileContainsText(expectedDoctestfile, "| `API Version` | `v2` |");
        assertThatFileContainsText(expectedDoctestfile, "| `Timeout` | `30 seconds` |");

    }

    @Test
    public void testThatSayUnorderedListGeneratesBulletList() throws Exception {

        sayNextSection("Requirements");
        sayUnorderedList(Arrays.asList(
            "Java 25 or higher",
            "Maven 4.0.0+",
            "JUnit 4.12+"
        ));

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify bullet list structure
        assertThatFileContainsText(expectedDoctestfile, "- Java 25 or higher");
        assertThatFileContainsText(expectedDoctestfile, "- Maven 4.0.0+");
        assertThatFileContainsText(expectedDoctestfile, "- JUnit 4.12+");

    }

    @Test
    public void testThatSayOrderedListGeneratesNumberedList() throws Exception {

        sayNextSection("Setup Steps");
        sayOrderedList(Arrays.asList(
            "Clone the repository",
            "Run mvnd clean install",
            "Run the tests"
        ));

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify numbered list structure
        assertThatFileContainsText(expectedDoctestfile, "1. Clone the repository");
        assertThatFileContainsText(expectedDoctestfile, "2. Run mvnd clean install");
        assertThatFileContainsText(expectedDoctestfile, "3. Run the tests");

    }

    @Test
    public void testThatSayJsonGeneratesPrettyPrintedJson() throws Exception {

        sayNextSection("Data Structures");
        sayJson(Map.of(
            "id", 1,
            "name", "Test User",
            "active", true
        ));

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify JSON code block structure
        assertThatFileContainsText(expectedDoctestfile, "```json");
        assertThatFileContainsText(expectedDoctestfile, "\"name\" : \"Test User\"");
        assertThatFileContainsText(expectedDoctestfile, "```");

    }

    @Test
    public void testThatSayAssertionsGeneratesResultTable() throws Exception {

        sayNextSection("Validation Results");
        sayAssertions(Map.of(
            "Schema is valid", "✓ PASS",
            "Response time < 100ms", "✓ PASS",
            "Cache headers present", "✗ FAIL"
        ));

        finishDocTest();

        File expectedDoctestfile = new File("docs/test/" + DtrCoreTest.EXPECTED_FILENAME);

        // Verify assertions table structure
        assertThatFileContainsText(expectedDoctestfile, "| Check | Result |");
        assertThatFileContainsText(expectedDoctestfile, "| Schema is valid | `✓ PASS` |");
        assertThatFileContainsText(expectedDoctestfile, "| Cache headers present | `✗ FAIL` |");

    }

    // =========================================================================
    // Structural invariant tests — every say* output must satisfy Markdown contracts
    // =========================================================================

    @Test
    public void testMarkdownStructuralInvariants() throws Exception {
        // Define structural invariants for each say* method
        var invariants = Arrays.asList(
            new MarkdownInvariant("sayTable produces pipe chars",
                content -> content.contains("| Feature |") && content.contains("| --- |")),
            new MarkdownInvariant("sayCode produces backtick fence",
                content -> content.contains("```java") && content.contains("```")),
            new MarkdownInvariant("sayWarning produces GitHub alert",
                content -> content.contains("> [!WARNING]")),
            new MarkdownInvariant("sayNote produces GitHub note",
                content -> content.contains("> [!NOTE]"))
        );

        // Generate all output types
        sayNextSection("Structural Invariant Test");
        sayTable(new String[][] {
            {"Feature", "Status"},
            {"Invariant test", "✓ Running"}
        });
        sayCode("System.out.println(\"invariant\");", "java");
        sayWarning("Invariant warning verification");
        sayNote("Invariant note verification");

        finishDocTest();

        File outputFile = new File("docs/test/" + EXPECTED_FILENAME);
        String content = Files.toString(outputFile, Charsets.UTF_8);

        // Assert every invariant holds
        for (var inv : invariants) {
            assertTrue(inv.check().test(content),
                "Markdown invariant violated: '" + inv.name() + "'" +
                " — output file does not satisfy the structural contract.");
        }
    }

    // =========================================================================
    // Boundary and Unicode edge case tests
    // =========================================================================

    @Test
    public void testBoundaryAndUnicodeEdgeCases() throws Exception {
        var edgeCases = Arrays.asList(
            new EdgeCase("empty string", "", "file written without exception"),
            new EdgeCase("unicode emoji", "Japanese: 日本語テスト 🚀", "UTF-8 preserved"),
            new EdgeCase("pipe in text", "table | pipe | test", "file written without exception"),
            new EdgeCase("backtick in text", "code `snippet` here", "file written without exception")
        );

        for (var ec : edgeCases) {
            // Each edge case must not throw
            assertDoesNotThrow(() -> say(ec.input()),
                "say() must not throw for edge case: " + ec.desc());
        }

        // Unicode content must be preserved exactly in the output file
        String unicodeContent = "日本語テスト 🚀 emoji ✓";
        say(unicodeContent);

        finishDocTest();

        File outputFile = new File("docs/test/" + EXPECTED_FILENAME);
        assertTrue(outputFile.exists(), "Output file must exist after edge case say() calls");

        // Read with UTF-8 explicitly to verify round-trip encoding
        String rawContent = new String(
            java.nio.file.Files.readAllBytes(outputFile.toPath()),
            StandardCharsets.UTF_8);
        assertTrue(rawContent.contains("日本語テスト"),
            "Japanese Unicode characters must be preserved in UTF-8 output");
        assertTrue(rawContent.contains("🚀"),
            "Emoji characters must be preserved in UTF-8 output");
    }

    // =========================================================================
    // Concurrent safety — isolated DocTester instances under virtual thread pressure
    // =========================================================================

    @Test
    public void testConcurrentSafetyWithVirtualThreads() throws Exception {
        int threadCount  = 10;
        int callsPerThread = 100;

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Each thread uses its own isolated RenderMachineImpl — no shared mutable state
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        var rm = new io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl();
                        rm.setFileName("ConcurrentSafety-thread-" + threadId);
                        for (int i = 0; i < callsPerThread; i++) {
                            rm.say("Thread " + threadId + " call " + i);
                        }
                        // Verify via public method the instance is alive and responsive
                        String id = rm.convertTextToId("thread-" + threadId);
                        if (id != null) successes.incrementAndGet();
                        else            failures.incrementAndGet();
                    } catch (Exception e) {
                        failures.incrementAndGet();
                        System.err.println("[Concurrent] Thread " + threadId + " threw: " + e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(completed, threadCount + " virtual threads must complete within 30 seconds");
        assertEquals(threadCount, successes.get(),
            "All " + threadCount + " virtual threads must complete successfully; failures=" + failures.get());
        assertEquals(0, failures.get(),
            "No virtual thread must fail; got " + failures.get() + " failures");
    }

    public void doCreateSomeTestOuputForDoctest() {

        sayNextSection("another fun heading!");
        say("and a very long text...!");

    }

    public static void assertThatFileContainsText(File file, String text) throws IOException {

        String content = Files.toString(file, Charsets.UTF_8);
        Assertions.assertTrue(content.contains(text));

    }

}

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

import org.r10r.doctester.DocTester;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DocTesterTest extends DocTester {

    public static String EXPECTED_FILENAME = DocTesterTest.class.getName() + ".md";

    @Test
    public void testThatIndexFileWritingWorks() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedIndex = new File("target/docs/README.md");

        Assertions.assertTrue(expectedIndex.exists());

        // README.md is the index, verify it contains the expected header
        assertThatFileContainsText(expectedIndex, "API Documentation");

    }

    @Test
    public void testThatIndexWritingOutDoctestFileWorks() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedDoctestfile = new File("target/docs/" + EXPECTED_FILENAME);
        File expectedIndexFile = new File("target/docs/README.md");

        // just a simple test to make sure the name is written somewhere in the file.
        assertThatFileContainsText(expectedDoctestfile, DocTesterTest.class.getSimpleName());

        // just a simple test to make sure that README.md contains a "link" to the doctest file.
        assertThatFileContainsText(expectedIndexFile, EXPECTED_FILENAME);

    }

    @Test
    public void testThatMarkdownOutputContainsExpectedContent() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedDoctestfile = new File("target/docs/" + EXPECTED_FILENAME);

        // Verify the markdown file was created and contains test content
        assertThatFileContainsText(expectedDoctestfile, "another fun heading!");
        assertThatFileContainsText(expectedDoctestfile, "and a very long text...!");

    }

    @Test
    public void testThatUsageOfTestBrowserWithoutSpecifyingGetTestUrlIsNotAllowed() {

        Assertions.assertThrows(IllegalStateException.class, () -> {
            testServerUrl();
        });

    }

    @Test
    public void testThatAssertionFailureGetsWrittenToMarkdownFile() throws Exception {

        boolean gotTestFailure = false;

        try {
            sayAndAssertThat("This will go wrong", false, is(true));
        } catch (AssertionError assertionError) {
            gotTestFailure = true;
        }

        assertThat(gotTestFailure, is(true));

        finishDocTest();

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

        // Verify that assertion failures are marked with ✗ and include error message
        assertThatFileContainsText(expectedDoctestfile, "✗ **FAILED**: This will go wrong");
        assertThatFileContainsText(expectedDoctestfile, "java.lang.AssertionError");

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

        // Verify warning alert structure
        assertThatFileContainsText(expectedDoctestfile, "> [!WARNING]");
        assertThatFileContainsText(expectedDoctestfile, "> This is a critical warning that must be visible!");

    }

    @Test
    public void testThatSayNoteGeneratesInfoBox() throws Exception {

        sayNextSection("Tips");
        sayNote("This is a helpful tip for users.");

        finishDocTest();

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

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

        File expectedDoctestfile = new File("target/docs/" + DocTesterTest.EXPECTED_FILENAME);

        // Verify assertions table structure
        assertThatFileContainsText(expectedDoctestfile, "| Check | Result |");
        assertThatFileContainsText(expectedDoctestfile, "| Schema is valid | `✓ PASS` |");
        assertThatFileContainsText(expectedDoctestfile, "| Cache headers present | `✗ FAIL` |");

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

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
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DocTesterTest extends DocTester {

    public static String EXPECTED_FILENAME = DocTesterTest.class.getName() + ".md";

    @Test
    public void testThatIndexFileWritingWorks() throws Exception {

        doCreateSomeTestOuputForDoctest();

        finishDocTest();

        File expectedIndex = new File("target/docs/README.md");

        Assert.assertTrue(expectedIndex.exists());

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

    @Test(expected = IllegalStateException.class)
    public void testThatUsageOfTestBrowserWithoutSpecifyingGetTestUrlIsNotAllowed() {

        testServerUrl();

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

    public void doCreateSomeTestOuputForDoctest() {

        sayNextSection("another fun heading!");
        say("and a very long text...!");

    }

    public static void assertThatFileContainsText(File file, String text) throws IOException {

        String content = Files.toString(file, Charsets.UTF_8);
        Assert.assertTrue(content.contains(text));

    }

}

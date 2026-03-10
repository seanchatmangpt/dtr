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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Doctests verifying that {@link DocSection} and {@link DocDescription}
 * annotations work correctly as declarative alternatives to calling
 * {@code sayNextSection()} and {@code say()} explicitly in the test body.
 *
 * <p>Each {@code @Test} method below also serves as a living example of the
 * annotation API; the generated HTML for this class documents the feature
 * while simultaneously proving it works.
 */
public class AnnotationDocTest extends DocTester {

    private static final String EXPECTED_HTML =
            "target/site/doctester/" + AnnotationDocTest.class.getName() + ".html";

    // -------------------------------------------------------------------------
    // @DocSection tests
    // -------------------------------------------------------------------------

    @Test
    @DocSection("Annotation API — @DocSection")
    public void testDocSectionAnnotationRendersHeading() throws IOException {

        say("The <code>@DocSection</code> annotation placed on a test method automatically "
                + "calls <code>sayNextSection(value)</code> before the test body runs. "
                + "This heading was produced by <code>@DocSection(\"Annotation API — @DocSection\")</code>.");

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        Assert.assertTrue("HTML output file must exist", html.exists());

        String content = Files.toString(html, Charsets.UTF_8);
        Assert.assertTrue(
                "@DocSection value must appear as a heading in the HTML",
                content.contains("Annotation API — @DocSection"));

    }

    @Test
    @DocSection("Annotation API — @DocDescription (single line)")
    @DocDescription("This paragraph was injected by a single-value @DocDescription annotation.")
    public void testDocDescriptionSingleLineRendersOneParagraph() throws IOException {

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        String content = Files.toString(html, Charsets.UTF_8);

        Assert.assertTrue(
                "@DocDescription single-line value must appear in the HTML",
                content.contains("This paragraph was injected by a single-value @DocDescription annotation."));

    }

    @Test
    @DocSection("Annotation API — @DocDescription (multiple lines)")
    @DocDescription({
        "First paragraph from a multi-value @DocDescription.",
        "Second paragraph from the same @DocDescription annotation."
    })
    public void testDocDescriptionMultipleValuesRenderMultipleParagraphs() throws IOException {

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        String content = Files.toString(html, Charsets.UTF_8);

        Assert.assertTrue(
                "First @DocDescription line must appear in the HTML",
                content.contains("First paragraph from a multi-value @DocDescription."));
        Assert.assertTrue(
                "Second @DocDescription line must appear in the HTML",
                content.contains("Second paragraph from the same @DocDescription annotation."));

    }

    @Test
    @DocSection("Annotation API — combined @DocSection + @DocDescription")
    @DocDescription({
        "Both annotations may be combined on the same method.",
        "The section heading is emitted first, then each description paragraph in order."
    })
    public void testDocSectionAndDocDescriptionCanBeCombined() throws IOException {

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        String content = Files.toString(html, Charsets.UTF_8);

        Assert.assertTrue(
                "Combined section heading must appear in the HTML",
                content.contains("Annotation API — combined @DocSection + @DocDescription"));
        Assert.assertTrue(
                "First combined description paragraph must appear in the HTML",
                content.contains("Both annotations may be combined on the same method."));

    }

    @Test
    @DocSection("Annotation API — backward compatibility")
    public void testMethodWithoutDocDescriptionStillWorks() throws IOException {

        say("A test method with only <code>@DocSection</code> and no "
                + "<code>@DocDescription</code> is perfectly valid. "
                + "The description may be written inline using <code>say()</code> as before.");

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        Assert.assertTrue("HTML output file must exist for backward-compat test", html.exists());

    }

    @Test
    public void testMethodWithNoAnnotationsStillWorks() throws IOException {

        sayNextSection("Annotation API — no annotations");
        say("A test with no <code>@DocSection</code> or <code>@DocDescription</code> "
                + "annotations continues to work exactly as before.");

        finishDocTest();

        File html = new File(EXPECTED_HTML);
        Assert.assertTrue("HTML output file must exist when no annotations used", html.exists());

    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @AfterClass
    public static void afterClass() {
        finishDocTest();
    }

}

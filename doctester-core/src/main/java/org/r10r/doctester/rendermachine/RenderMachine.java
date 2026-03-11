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
package org.r10r.doctester.rendermachine;

import org.r10r.doctester.testbrowser.TestBrowser;

/**
 * Abstract base class for render machines that convert test execution into documentation.
 *
 * Supports multiple output formats: Markdown, Blog posts, Slides, LaTeX, etc.
 * Subclasses implement platform-specific rendering and output generation.
 */
public abstract class RenderMachine implements RenderMachineCommands {

    /**
     * Sets the TestBrowser instance for HTTP request execution.
     *
     * @param testBrowser the HTTP client to use for requests
     */
    public abstract void setTestBrowser(TestBrowser testBrowser);

    /**
     * Sets the output filename (typically the test class name).
     *
     * @param fileName the filename for the generated documentation
     */
    public abstract void setFileName(String fileName);

    /**
     * Renders content only for slide output (ignored by doc/blog render machines).
     *
     * @param text the text to render on slides only
     */
    public void saySlideOnly(String text) {
        // No-op for non-slide render machines
    }

    /**
     * Renders content only for documentation/blog output (ignored by slide render machines).
     *
     * @param text the text to render in docs only
     */
    public void sayDocOnly(String text) {
        // Most render machines will override this
        say(text);
    }

    /**
     * Renders speaker notes for slides (ignored by doc/blog render machines).
     *
     * @param text the speaker notes text
     */
    public void saySpeakerNote(String text) {
        // No-op for non-slide render machines
    }

    /**
     * Renders a hero image for blogs and slides (ignored by other formats).
     *
     * @param altText the alt text for the image
     */
    public void sayHeroImage(String altText) {
        // No-op for formats that don't support hero images
    }

    /**
     * Renders a tweetable (≤280 chars) for social media queue.
     *
     * @param text the text to tweet (will be truncated to 280 chars)
     */
    public void sayTweetable(String text) {
        // No-op for formats that don't generate social content
    }

    /**
     * Renders a TLDR (too long; didn't read) summary for blogs.
     *
     * @param text the summary text
     */
    public void sayTldr(String text) {
        // No-op for formats that don't support TLDR
    }

    /**
     * Renders a call-to-action link for blogs.
     *
     * @param url the URL for the CTA button/link
     */
    public void sayCallToAction(String url) {
        // No-op for formats that don't support CTAs
    }

    /**
     * Documents a class's structure using Java reflection.
     *
     * <p>Default no-op implementation — override in render machines that support
     * code model rendering (e.g., {@link RenderMachineImpl}).</p>
     *
     * @param clazz the class to introspect and document
     */
    public void sayCodeModel(Class<?> clazz) {
        // No-op for render machines that don't support code model rendering
    }

    /** Documents current call site — no-op in base class. */
    public void sayCallSite() {}

    /** Documents annotation profile — no-op in base class. */
    public void sayAnnotationProfile(Class<?> clazz) {}

    /** Documents class hierarchy — no-op in base class. */
    public void sayClassHierarchy(Class<?> clazz) {}

    /** Documents string profile — no-op in base class. */
    public void sayStringProfile(String text) {}

    /** Documents reflective diff — no-op in base class. */
    public void sayReflectiveDiff(Object before, Object after) {}

    /**
     * Finishes documentation generation and writes output to disk.
     */
    public abstract void finishAndWriteOut();

}

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
package org.r10r.doctester.render.blog;

/**
 * Sealed interface for blog platform templates.
 *
 * Each platform (Dev.to, Medium, LinkedIn, etc.) has different front matter,
 * metadata, and formatting requirements. Sealed implementations ensure exhaustive
 * handling when adding new platforms.
 */
public sealed interface BlogTemplate permits
    DevToTemplate,
    MediumTemplate,
    SubstackTemplate,
    LinkedInTemplate,
    HashnodeTemplate {

    /**
     * Generates platform-specific front matter (YAML or JSON headers).
     *
     * @param meta the document metadata
     * @return the front matter string
     */
    String frontMatter(BlogMetadata meta);

    /**
     * Formats a hero image reference (platform-specific markdown syntax).
     *
     * @param altText the alt text for the image
     * @return the formatted image reference
     */
    String heroImage(String altText);

    /**
     * Calculates and formats reading time estimate.
     *
     * @param wordCount the total word count of the article
     * @return the reading time display string (e.g., "5 min read")
     */
    String readingTime(int wordCount);

    /**
     * Formats the canonical URL (points back to GitHub/docs).
     *
     * @param docTestUrl the base documentation URL
     * @return the canonical URL for the blog post
     */
    String canonicalUrl(String docTestUrl);

    /**
     * Formats a call-to-action link for the article.
     *
     * @param text the CTA button text
     * @param url the target URL
     * @return the formatted CTA (as Markdown link or platform-specific format)
     */
    String formatCallToAction(String text, String url);

    /**
     * Formats a tweetable/social media excerpt.
     *
     * @param text the text to format (≤280 chars)
     * @return the formatted text with platform-specific metadata
     */
    String formatTweetable(String text);

    /**
     * Returns the platform's preferred footnote marker format.
     *
     * @param index the footnote index (1-based)
     * @return the formatted footnote marker (e.g., "[^1]" for Markdown, "[1]" for others)
     */
    String footnoteMarker(int index);

    /**
     * Returns the platform name for output path organization.
     *
     * @return the platform name (e.g., "devto", "linkedin", "substack")
     */
    String platformName();

    /**
     * Record for document metadata shared across all blog platforms.
     */
    record BlogMetadata(
        String title,
        String author,
        String description,
        String createdAt,
        int wordCount,
        String docTestClassName,
        String docTestUrl,
        java.util.List<String> tags
    ) {}
}

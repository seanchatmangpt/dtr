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
package io.github.seanchatmangpt.dtr.render.blog;

/**
 * LinkedIn platform template for article posts.
 *
 * LinkedIn articles are limited in markdown support and character limits.
 * Formatting optimized for algorithm engagement (short paragraphs, numbered lists).
 */
public record LinkedInTemplate() implements BlogTemplate {

    @Override
    public String frontMatter(BlogMetadata meta) {
        return """
            ---
            title: %s
            description: %s
            ---
            """.formatted(
            meta.title(),
            meta.description()
        );
    }

    @Override
    public String heroImage(String altText) {
        return "";  // hguard-ok: LinkedIn manages hero images via its upload UI, not inline content
    }

    @Override
    public String readingTime(int wordCount) {
        int minutes = Math.max(1, wordCount / 200);
        return "*~%d min read*".formatted(minutes);
    }

    @Override
    public String canonicalUrl(String docTestUrl) {
        return "";  // hguard-ok: LinkedIn has no canonical URL metadata field — platform constraint, not a stub
    }

    @Override
    public String formatCallToAction(String text, String url) {
        return "\n→ %s: %s".formatted(text, url);
    }

    @Override
    public String formatTweetable(String text) {
        return text.length() > 280 ? text.substring(0, 277) + "..." : text;
    }

    @Override
    public String footnoteMarker(int index) {
        return "[%d]".formatted(index);
    }

    @Override
    public String platformName() {
        return "linkedin";
    }
}

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
 * Substack newsletter platform template.
 *
 * Substack uses plain Markdown with optional metadata headers.
 * Newsletter-specific formatting for subscriber engagement.
 */
public record SubstackTemplate() implements BlogTemplate {

    @Override
    public String frontMatter(BlogMetadata meta) {
        return """
            ---
            title: %s
            subtitle: %s
            ---
            """.formatted(
            meta.title(),
            meta.description()
        );
    }

    @Override
    public String heroImage(String altText) {
        return "";  // Substack uses cover image differently
    }

    @Override
    public String readingTime(int wordCount) {
        int minutes = Math.max(1, wordCount / 200);
        return "*%d min read*".formatted(minutes);
    }

    @Override
    public String canonicalUrl(String docTestUrl) {
        return "([Read on GitHub](%s))".formatted(docTestUrl);
    }

    @Override
    public String formatCallToAction(String text, String url) {
        return "\n\n**[%s](%s)**\n".formatted(text, url);
    }

    @Override
    public String formatTweetable(String text) {
        return text;
    }

    @Override
    public String footnoteMarker(int index) {
        return "[%d]".formatted(index);
    }

    @Override
    public String platformName() {
        return "substack";
    }
}

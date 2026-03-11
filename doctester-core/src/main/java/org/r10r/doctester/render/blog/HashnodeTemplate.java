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
 * Hashnode platform template for technical content.
 *
 * Hashnode is a technical blogging platform with strong support for code,
 * YAML front matter, and community engagement features.
 */
public record HashnodeTemplate() implements BlogTemplate {

    @Override
    public String frontMatter(BlogMetadata meta) {
        return """
            ---
            title: %s
            description: %s
            tags: [%s]
            canonical_url: %s
            enableToc: true
            ---
            """.formatted(
            meta.title(),
            meta.description(),
            String.join(", ", meta.tags()),
            meta.docTestUrl()
        );
    }

    @Override
    public String heroImage(String altText) {
        return "![%s](https://via.placeholder.com/1200x630?text=%s)".formatted(altText, altText);
    }

    @Override
    public String readingTime(int wordCount) {
        int minutes = Math.max(1, wordCount / 200);
        return "%d min read".formatted(minutes);
    }

    @Override
    public String canonicalUrl(String docTestUrl) {
        return docTestUrl;
    }

    @Override
    public String formatCallToAction(String text, String url) {
        return "[%s](%s)".formatted(text, url);
    }

    @Override
    public String formatTweetable(String text) {
        return text;
    }

    @Override
    public String footnoteMarker(int index) {
        return "[^%d]".formatted(index);
    }

    @Override
    public String platformName() {
        return "hashnode";
    }
}

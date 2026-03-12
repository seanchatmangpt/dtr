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
 * Dev.to (dev.to) blog platform template.
 *
 * Uses YAML front matter compatible with Dev.to's markdown format.
 * Dev.to allows markdown with custom metadata headers.
 */
public record DevToTemplate() implements BlogTemplate {

    @Override
    public String frontMatter(BlogMetadata meta) {
        return """
            ---
            title: %s
            description: %s
            tags: %s
            canonical_url: %s
            series: DTR Tests
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
        return minutes + " min read";
    }

    @Override
    public String canonicalUrl(String docTestUrl) {
        return docTestUrl;
    }

    @Override
    public String formatCallToAction(String text, String url) {
        return "{% cta https://%s %s %}".formatted(url, text);
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
        return "devto";
    }
}

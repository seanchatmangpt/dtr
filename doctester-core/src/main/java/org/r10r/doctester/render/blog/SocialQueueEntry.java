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
 * Immutable record representing a single social media queue entry.
 *
 * Used to batch social media content (tweets, LinkedIn posts, etc.)
 * for external publishing APIs.
 *
 * @param platform the social platform ("twitter", "linkedin", etc.)
 * @param content the post content (text, with optional markdown)
 * @param url the target URL for CTAs
 * @param createdAt ISO 8601 timestamp when the entry was created
 */
public record SocialQueueEntry(
    String platform,
    String content,
    String url,
    String createdAt
) {

    /**
     * Create a new social queue entry with current timestamp.
     *
     * @param platform the social platform
     * @param content the post content
     * @param url the target URL
     */
    public SocialQueueEntry(String platform, String content, String url) {
        this(platform, content, url, java.time.Instant.now().toString());
    }
}

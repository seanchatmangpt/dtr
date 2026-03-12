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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes social media queue entries to JSON for batch publishing.
 *
 * Generates social-queue.json containing tweets, LinkedIn posts, and other
 * platform-specific content ready for external APIs (Twitter, LinkedIn, etc.).
 */
public final class SocialQueueWriter {

    private static final Logger logger = LoggerFactory.getLogger(SocialQueueWriter.class);
    private static final String QUEUE_DIR = "target/site/dtr/social";
    private static final ObjectMapper mapper = new ObjectMapper();

    private SocialQueueWriter() {
    }

    /**
     * Write social queue entries to JSON file.
     *
     * @param docTestName the name of the doc test (for organization)
     * @param tweetables list of tweetable content (≤280 chars each)
     * @param tldr the TL;DR summary for social previews
     * @param cta the call-to-action URL
     */
    public static void writeSocialQueue(String docTestName, List<String> tweetables, String tldr, String cta) {
        try {
            File queueDir = new File(QUEUE_DIR);
            queueDir.mkdirs();

            List<SocialQueueEntry> entries = new ArrayList<>();

            // Add tweets
            for (String tweet : tweetables) {
                entries.add(new SocialQueueEntry("twitter", tweet, cta));
            }

            // Add LinkedIn post if TLDR is present
            if (tldr != null && !tldr.isEmpty()) {
                String linkedInPost = tldr + (cta != null && !cta.isEmpty() ? "\n\n→ " + cta : "");
                entries.add(new SocialQueueEntry("linkedin", linkedInPost, cta));
            }

            // Write as JSON
            SocialQueue queue = new SocialQueue(docTestName, entries);
            File queueFile = new File(queueDir, "social-queue.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(queueFile, queue);

            logger.info("Generated social queue with {} entries to {}", entries.size(), queueFile);
        } catch (IOException e) {
            logger.error("Failed to write social queue", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Container record for the social queue JSON structure.
     */
    private record SocialQueue(
        String docTest,
        java.util.List<SocialQueueEntry> entries,
        String generatedAt
    ) {
        /**
         * Create a new social queue with current timestamp.
         *
         * @param docTest the doc test name
         * @param entries the queue entries
         */
        SocialQueue(String docTest, java.util.List<SocialQueueEntry> entries) {
            this(docTest, entries, java.time.Instant.now().toString());
        }
    }
}

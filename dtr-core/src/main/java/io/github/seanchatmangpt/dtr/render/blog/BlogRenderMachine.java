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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.cookie.Cookie;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blog post render machine generating platform-specific markdown.
 *
 * Converts test execution into blog post markdown compatible with Dev.to,
 * Medium, Substack, LinkedIn, and Hashnode. Generates social media queue
 * entries for tweets and platform-specific content.
 */
public class BlogRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(BlogRenderMachine.class);

    private static final String BASE_DIR = "target/site/doctester/blog";
    private final BlogTemplate template;
    private final StringBuilder buffer = new StringBuilder();
    private final List<String> tweetables = new ArrayList<>();
    private final List<String> sections = new ArrayList<>();
    private String fileName;
    private String tldr = "";
    private String cta = "";
    private int wordCount = 0;
    private TestBrowser testBrowser;

    /**
     * Create a blog render machine with the given platform template.
     *
     * @param template the blog platform template (Dev.to, Medium, etc.)
     */
    public BlogRenderMachine(BlogTemplate template) {
        this.template = template;
    }

    @Override
    public void setTestBrowser(TestBrowser testBrowser) {
        this.testBrowser = testBrowser;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void say(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.append(text).append("\n\n");
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayNextSection(String heading) {
        if (heading == null || heading.isEmpty()) {
            return;
        }
        sections.add(heading);
        buffer.append("## ").append(heading).append("\n\n");
    }

    @Override
    public void sayRaw(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return;
        }
        buffer.append(markdown).append("\n\n");
    }

    @Override
    public void sayTable(String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }
        for (String[] row : data) {
            buffer.append("| ");
            for (String cell : row) {
                buffer.append(cell != null ? cell : "").append(" | ");
            }
            buffer.append("\n");
            if (data[0] == row) {
                // Header separator
                buffer.append("|");
                for (int i = 0; i < row.length; i++) {
                    buffer.append(" --- |");
                }
                buffer.append("\n");
            }
        }
        buffer.append("\n");
    }

    @Override
    public void sayCode(String code, String language) {
        if (code == null || code.isEmpty()) {
            return;
        }
        String lang = language != null ? language : "text";
        buffer.append("```").append(lang).append("\n")
            .append(code).append("\n```\n\n");
        wordCount += 2;
    }

    @Override
    public void sayWarning(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        buffer.append("> [!WARNING]\n> ").append(message).append("\n\n");
        wordCount += message.split("\\s+").length;
    }

    @Override
    public void sayNote(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        buffer.append("> [!NOTE]\n> ").append(message).append("\n\n");
        wordCount += message.split("\\s+").length;
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return;
        }
        buffer.append("| Key | Value |\n| --- | --- |\n");
        for (var entry : pairs.entrySet()) {
            buffer.append("| ").append(entry.getKey()).append(" | ")
                .append(entry.getValue()).append(" |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (String item : items) {
            buffer.append("- ").append(item).append("\n");
            wordCount += item.split("\\s+").length;
        }
        buffer.append("\n");
    }

    @Override
    public void sayOrderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            buffer.append(i + 1).append(". ").append(items.get(i)).append("\n");
            wordCount += items.get(i).split("\\s+").length;
        }
        buffer.append("\n");
    }

    @Override
    public void sayJson(Object object) {
        if (object == null) {
            return;
        }
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
            sayCode(json, "json");
        } catch (Exception e) {
            logger.warn("Could not serialize object to JSON", e);
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return;
        }
        buffer.append("| Check | Result |\n| --- | --- |\n");
        for (var entry : assertions.entrySet()) {
            buffer.append("| ").append(entry.getKey()).append(" | ")
                .append(entry.getValue()).append(" |\n");
        }
        buffer.append("\n");
    }

    @Override
    public void sayRef(DocTestRef ref) {
        // Render as markdown link to another doc
        if (ref == null) {
            return;
        }
        buffer.append("[See ").append(ref.anchor()).append("](../markdown/")
            .append(ref.docTestClass()).append(".md#")
            .append(ref.anchor().toLowerCase().replace(" ", "-")).append(")\n\n");
    }

    @Override
    public void sayCite(String citationKey) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        buffer.append("[^").append(citationKey).append("]");
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        buffer.append("[^").append(citationKey).append(":").append(pageRef).append("]");
    }

    @Override
    public void sayFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.append(" ").append(template.footnoteMarker(1)).append(" ");
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayTweetable(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String tweet = text.length() > 280 ? text.substring(0, 277) + "..." : text;
        tweetables.add(tweet);
    }

    @Override
    public void sayTldr(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        this.tldr = text;
        wordCount += text.split("\\s+").length;
    }

    @Override
    public void sayCallToAction(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        this.cta = url;
    }

    @Override
    public void sayHeroImage(String altText) {
        // Blog platform templates handle hero images differently
        if (altText == null || altText.isEmpty()) {
            return;
        }
        buffer.insert(0, template.heroImage(altText) + "\n\n");
    }

    @Override
    public void saySlideOnly(String text) {
        // Ignored for blog output
    }

    @Override
    public void sayDocOnly(String text) {
        // Render in blog posts
        say(text);
    }

    @Override
    public void saySpeakerNote(String text) {
        // Ignored for blog output
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        List<Cookie> cookies = testBrowser.getCookies();
        buffer.append("### Cookies\n");
        for (Cookie cookie : cookies) {
            buffer.append("- **").append(cookie.getName()).append("**: `")
                .append(cookie.getValue()).append("`\n");
        }
        buffer.append("\n");
        return cookies;
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        Cookie cookie = testBrowser.getCookieWithName(name);
        if (cookie != null) {
            buffer.append("### Cookie: ").append(name).append("\n")
                .append("- **Value**: `").append(cookie.getValue()).append("`\n\n");
        }
        return cookie;
    }

    @Override
    public Response sayAndMakeRequest(Request httpRequest) {
        Response response = testBrowser.makeRequest(httpRequest);
        buffer.append("### Request\n\n```\n")
            .append(httpRequest.httpRequestType).append(" ")
            .append(httpRequest.uri).append("\n```\n\n");
        buffer.append("### Response\n\n```\n")
            .append(response.httpStatus).append(" ")
            .append(response.payloadAsString()).append("\n```\n\n");
        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        boolean matches = matcher.matches(actual);
        buffer.append("- **").append(message).append("**: ")
            .append(matches ? "✓ PASS" : "✗ FAIL").append("\n");
        if (!matches) {
            buffer.append("  Reason: ").append(reason).append("\n");
        }
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        sayAndAssertThat(message, "", actual, matcher);
    }

    @Override
    public void finishAndWriteOut() {
        // Write blog post markdown
        writeBlogPost();

        // Write social queue
        SocialQueueWriter.writeSocialQueue(fileName, tweetables, tldr, cta);

        logger.info("Generated blog post for {} to {}/{}/{}.md",
            fileName, BASE_DIR, template.platformName(), fileName);
    }

    private void writeBlogPost() {
        try {
            File dir = new File(BASE_DIR + "/" + template.platformName());
            dir.mkdirs();

            File outFile = new File(dir, fileName + ".md");

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

                // Write front matter
                BlogTemplate.BlogMetadata meta = new BlogTemplate.BlogMetadata(
                    fileName,
                    "DocTester",
                    tldr.isEmpty() ? "API Documentation" : tldr,
                    java.time.LocalDate.now().toString(),
                    wordCount,
                    fileName,
                    "https://github.com/r10r/doctester",
                    Arrays.asList("api", "testing", "documentation")
                );

                writer.write(template.frontMatter(meta));
                writer.write("\n\n");

                // Write TLDR if present
                if (!tldr.isEmpty()) {
                    writer.write("> **TL;DR** ");
                    writer.write(tldr);
                    writer.write("\n\n");
                }

                // Write content
                writer.write(buffer.toString());

                // Write CTA if present
                if (!cta.isEmpty()) {
                    writer.write("\n\n### Learn More\n\n");
                    writer.write(template.formatCallToAction("Read Full Documentation", cta));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write blog post", e);
            throw new RuntimeException(e);
        }
    }
}

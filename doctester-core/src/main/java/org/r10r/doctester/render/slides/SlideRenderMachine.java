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
package org.r10r.doctester.render.slides;

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
import org.r10r.doctester.crossref.DocTestRef;
import org.r10r.doctester.rendermachine.RenderMachine;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slide deck render machine generating presentation decks.
 *
 * Converts test execution into presentation slides (Reveal.js HTML, etc.).
 * Maps say* methods to slide content: sayNextSection → new slide, say → bullets, etc.
 */
public class SlideRenderMachine extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(SlideRenderMachine.class);
    private static final String BASE_DIR = "target/site/doctester/slides";

    private final SlideTemplate template;
    private final StringBuilder slideBuffer = new StringBuilder();
    private final List<String> speakerNotes = new ArrayList<>();
    private String fileName;
    private String currentTitle = "";
    private List<String> currentBullets = new ArrayList<>();
    private String currentSpeakerNote = "";
    private TestBrowser testBrowser;

    /**
     * Create a slide render machine with the given template.
     *
     * @param template the slide template (Reveal.js, Marp, etc.)
     */
    public SlideRenderMachine(SlideTemplate template) {
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
        // Convert paragraph to bullet point on slide
        currentBullets.add(text);
    }

    @Override
    public void sayNextSection(String heading) {
        if (heading == null || heading.isEmpty()) {
            return;
        }
        // Finish current slide if any
        if (!currentTitle.isEmpty() || !currentBullets.isEmpty()) {
            flushCurrentSlide();
        }
        // Start new section
        currentTitle = heading;
        currentBullets = new ArrayList<>();
        slideBuffer.append(template.formatSectionSlide(heading));
    }

    @Override
    public void sayRaw(String markdown) {
        // For slides, skip raw markdown (not applicable)
    }

    @Override
    public void sayTable(String[][] data) {
        flushCurrentSlide();
        slideBuffer.append(template.formatTableSlide(data));
    }

    @Override
    public void sayCode(String code, String language) {
        flushCurrentSlide();
        slideBuffer.append(template.formatCodeSlide(code, language));
    }

    @Override
    public void sayWarning(String message) {
        flushCurrentSlide();
        slideBuffer.append(template.formatNoteSlide(message, "warning"));
    }

    @Override
    public void sayNote(String message) {
        flushCurrentSlide();
        slideBuffer.append(template.formatNoteSlide(message, "info"));
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        // Render as table slide
        if (pairs != null && !pairs.isEmpty()) {
            String[][] data = new String[pairs.size() + 1][2];
            data[0] = new String[]{"Key", "Value"};
            int row = 1;
            for (var entry : pairs.entrySet()) {
                data[row][0] = entry.getKey();
                data[row][1] = entry.getValue();
                row++;
            }
            sayTable(data);
        }
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items != null) {
            currentBullets.addAll(items);
        }
    }

    @Override
    public void sayOrderedList(List<String> items) {
        // For slides, treat ordered list as bullets
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                currentBullets.add((i + 1) + ". " + items.get(i));
            }
        }
    }

    @Override
    public void sayJson(Object object) {
        flushCurrentSlide();
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
            slideBuffer.append(template.formatCodeSlide(json, "json"));
        } catch (Exception e) {
            logger.warn("Could not serialize object to JSON", e);
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions != null && !assertions.isEmpty()) {
            String[][] data = new String[assertions.size() + 1][2];
            data[0] = new String[]{"Check", "Result"};
            int row = 1;
            for (var entry : assertions.entrySet()) {
                data[row][0] = entry.getKey();
                data[row][1] = entry.getValue();
                row++;
            }
            sayTable(data);
        }
    }

    @Override
    public void sayRef(DocTestRef ref) {
        if (ref != null) {
            currentBullets.add("See " + ref.anchor());
        }
    }

    @Override
    public void sayCite(String citationKey) {
        if (citationKey != null && !citationKey.isEmpty()) {
            currentBullets.add("[" + citationKey + "]");
        }
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        if (citationKey != null && !citationKey.isEmpty()) {
            currentBullets.add("[" + citationKey + " p. " + pageRef + "]");
        }
    }

    @Override
    public void sayFootnote(String text) {
        if (text != null && !text.isEmpty()) {
            currentBullets.add("* " + text);
        }
    }

    @Override
    public void sayTweetable(String text) {
        // Ignored for slides
    }

    @Override
    public void sayTldr(String text) {
        // Ignored for slides
    }

    @Override
    public void sayCallToAction(String url) {
        // Ignored for slides
    }

    @Override
    public void sayHeroImage(String altText) {
        // Ignored for Reveal.js (would need CDN URL)
    }

    @Override
    public void saySlideOnly(String text) {
        if (text != null && !text.isEmpty()) {
            currentBullets.add(text);
        }
    }

    @Override
    public void sayDocOnly(String text) {
        // Ignored for slides
    }

    @Override
    public void saySpeakerNote(String text) {
        if (text != null && !text.isEmpty()) {
            currentSpeakerNote = text;
        }
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        List<Cookie> cookies = testBrowser.getCookies();
        for (Cookie c : cookies) {
            currentBullets.add(c.getName() + " = " + c.getValue());
        }
        return cookies;
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        return testBrowser.getCookieWithName(name);
    }

    @Override
    public Response sayAndMakeRequest(Request httpRequest) {
        Response response = testBrowser.makeRequest(httpRequest);
        flushCurrentSlide();
        currentTitle = "Request/Response";
        currentBullets.add(httpRequest.httpRequestType + " " + httpRequest.uri);
        currentBullets.add("Status: " + response.httpStatus);
        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        boolean matches = matcher.matches(actual);
        currentBullets.add("✓ " + message + (matches ? " PASS" : " FAIL"));
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        sayAndAssertThat(message, "", actual, matcher);
    }

    @Override
    public void finishAndWriteOut() {
        // Flush any remaining content
        flushCurrentSlide();

        // Write Reveal.js HTML
        writeSlidesDeck();

        logger.info("Generated slides for {} to {}/{}.{}",
            fileName, BASE_DIR, fileName, template.fileExtension());
    }

    private void flushCurrentSlide() {
        if (!currentTitle.isEmpty() || !currentBullets.isEmpty()) {
            slideBuffer.append(template.formatContentSlide(currentTitle, currentBullets, currentSpeakerNote));
            currentTitle = "";
            currentBullets = new ArrayList<>();
            currentSpeakerNote = "";
        }
    }

    private void writeSlidesDeck() {
        try {
            File dir = new File(BASE_DIR);
            dir.mkdirs();

            File outFile = new File(dir, fileName + "." + template.fileExtension());

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

                // Write HTML5 Reveal.js template
                writer.write("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>%s</title>
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.min.css">
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/theme/black.min.css">
                        <style>
                            .reveal pre { width: 100%%; }
                            .reveal code { padding: 2px 8px; background: rgba(255,255,255,0.1); }
                            .highlight-warn { background: rgba(255,100,0,0.3); padding: 20px; border-radius: 5px; }
                            .highlight-error { background: rgba(255,0,0,0.3); padding: 20px; border-radius: 5px; }
                            .highlight-info { background: rgba(0,100,255,0.3); padding: 20px; border-radius: 5px; }
                        </style>
                    </head>
                    <body>
                        <div class="reveal">
                            <div class="slides">
                    """.formatted(fileName));

                // Write slide content
                writer.write(slideBuffer.toString());

                writer.write("""
                            </div>
                        </div>
                        <script src="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.min.js"></script>
                        <script src="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/plugin/highlight/highlight.min.js"></script>
                        <script>
                            Reveal.initialize({
                                hash: true,
                                center: true,
                                transition: 'slide',
                                plugins: [RevealHighlight]
                            });
                        </script>
                    </body>
                    </html>
                    """);
            }
        } catch (IOException e) {
            logger.error("Failed to write slide deck", e);
            throw new RuntimeException(e);
        }
    }
}

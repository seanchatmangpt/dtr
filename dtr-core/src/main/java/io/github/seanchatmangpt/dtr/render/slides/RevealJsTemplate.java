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
package io.github.seanchatmangpt.dtr.render.slides;

import io.github.seanchatmangpt.dtr.util.StringEscapeUtils;
import java.util.List;

/**
 * Reveal.js slide template for HTML5 presentation decks.
 *
 * Generates self-contained HTML5 using Reveal.js CDN.
 * Supports speaker notes, code highlighting, and markdown content.
 */
public record RevealJsTemplate() implements SlideTemplate {

    @Override
    public String formatSectionSlide(String title) {
        return """
            <section>
                <h2>%s</h2>
            </section>
            """.formatted(StringEscapeUtils.escapeHtml(title));
    }

    @Override
    public String formatContentSlide(String title, List<String> bulletPoints, String speakerNotes) {
        StringBuilder html = new StringBuilder();
        html.append("<section>\n");
        html.append("    <h3>").append(StringEscapeUtils.escapeHtml(title)).append("</h3>\n");
        if (bulletPoints != null && !bulletPoints.isEmpty()) {
            html.append("    <ul>\n");
            for (String bullet : bulletPoints) {
                html.append("        <li>").append(StringEscapeUtils.escapeHtml(bullet)).append("</li>\n");
            }
            html.append("    </ul>\n");
        }
        if (speakerNotes != null && !speakerNotes.isEmpty()) {
            html.append("    <aside class=\"notes\">\n");
            html.append("        ").append(StringEscapeUtils.escapeHtml(speakerNotes)).append("\n");
            html.append("    </aside>\n");
        }
        html.append("</section>\n");
        return html.toString();
    }

    @Override
    public String formatCodeSlide(String code, String language) {
        String lang = language != null ? language : "java";
        return """
            <section>
                <pre><code class="language-%s" data-trim>%s</code></pre>
            </section>
            """.formatted(lang, StringEscapeUtils.escapeHtml(code));
    }

    @Override
    public String formatTableSlide(String[][] data) {
        if (data == null || data.length == 0) {
            return "";  // hguard-ok: null/empty guard — no slide rendered when no data rows provided
        }
        StringBuilder html = new StringBuilder();
        html.append("<section>\n");
        html.append("    <table>\n");
        for (int i = 0; i < data.length; i++) {
            String[] row = data[i];
            String tag = (i == 0) ? "th" : "td";
            html.append("        <tr>\n");
            for (String cell : row) {
                html.append("            <").append(tag).append(">")
                    .append(StringEscapeUtils.escapeHtml(cell != null ? cell : "")).append("</")
                    .append(tag).append(">\n");
            }
            html.append("        </tr>\n");
        }
        html.append("    </table>\n");
        html.append("</section>\n");
        return html.toString();
    }

    @Override
    public String formatNoteSlide(String text, String type) {
        String className = switch (type) {
            case "warning" -> "warn";
            case "error" -> "error";
            default -> "info";
        };
        return """
            <section>
                <div class="highlight-%s">
                    <p>%s</p>
                </div>
            </section>
            """.formatted(className, StringEscapeUtils.escapeHtml(text));
    }

    @Override
    public String fileExtension() {
        return "html";
    }

    @Override
    public String platformName() {
        return "revealjs";
    }

}

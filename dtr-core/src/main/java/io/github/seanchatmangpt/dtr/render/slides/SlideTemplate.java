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

import java.util.List;

/**
 * Sealed interface for slide deck templates.
 *
 * Each slide platform (Reveal.js, Marp, PowerPoint, etc.) has different
 * output formats and speaker notes handling. Sealed implementations ensure
 * exhaustive handling when adding new platforms.
 */
public sealed interface SlideTemplate permits
    RevealJsTemplate {

    /**
     * Creates a slide from a section heading.
     *
     * @param title the section title
     * @return the formatted slide
     */
    String formatSectionSlide(String title);

    /**
     * Creates a slide with bullet points and optional speaker notes.
     *
     * @param title the slide title
     * @param bulletPoints the content bullet points
     * @param speakerNotes optional speaker notes
     * @return the formatted slide
     */
    String formatContentSlide(String title, List<String> bulletPoints, String speakerNotes);

    /**
     * Formats a code slide with syntax highlighting.
     *
     * @param code the code content
     * @param language the programming language
     * @return the formatted code slide
     */
    String formatCodeSlide(String code, String language);

    /**
     * Formats a table slide.
     *
     * @param data the table data
     * @return the formatted table slide
     */
    String formatTableSlide(String[][] data);

    /**
     * Formats a note/callout slide.
     *
     * @param text the note text
     * @param type the note type ("note", "warning", etc.)
     * @return the formatted note slide
     */
    String formatNoteSlide(String text, String type);

    /**
     * Returns the file extension for this template's output format.
     *
     * @return the file extension (e.g., "html", "pptx", "key")
     */
    String fileExtension();

    /**
     * Returns the platform name for logging/organization.
     *
     * @return the platform name (e.g., "revealjs", "powerpoint")
     */
    String platformName();
}

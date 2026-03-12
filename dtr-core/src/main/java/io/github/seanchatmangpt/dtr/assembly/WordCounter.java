/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.assembly;

import java.util.regex.Pattern;

/**
 * Utility for counting words in LaTeX document content.
 *
 * Provides simple word counting logic that excludes LaTeX command tokens
 * (e.g., \textbf{...}, \begin{...}, etc.) and counts only human-readable words.
 */
public final class WordCounter {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+(?:'\\w+)?\\b");

    private WordCounter() {
        // Utility class
    }

    /**
     * Counts the number of words in LaTeX content.
     *
     * This is a simple word counter that strips LaTeX commands and counts
     * remaining tokens as words.
     *
     * @param texContent the LaTeX source content to count
     * @return the estimated word count
     * @throws IllegalArgumentException if texContent is null
     */
    public static int count(String texContent) {
        if (texContent == null) {
            throw new IllegalArgumentException("texContent cannot be null");
        }

        if (texContent.isEmpty()) {
            return 0;
        }

        // Strip LaTeX commands and count remaining words
        String cleaned = stripLatexCommands(texContent);
        return countWords(cleaned);
    }

    /**
     * Strips LaTeX commands from content, keeping only text content.
     */
    private static String stripLatexCommands(String content) {
        var result = new StringBuilder();
        var i = 0;

        while (i < content.length()) {
            char ch = content.charAt(i);

            // Skip LaTeX commands (tokens starting with \)
            if (ch == '\\') {
                // Skip the backslash and the command name
                i++;
                while (i < content.length() && Character.isLetter(content.charAt(i))) {
                    i++;
                }
                // Skip optional arguments in brackets
                if (i < content.length() && content.charAt(i) == '[') {
                    i = skipBracketedContent(content, i);
                }
                // Skip mandatory arguments in braces
                if (i < content.length() && content.charAt(i) == '{') {
                    i = skipBracedContent(content, i);
                }
                continue;
            }

            // Skip content in braces (but keep it if not part of a command)
            if (ch == '{') {
                i = skipBracedContent(content, i);
                continue;
            }

            // Skip content in brackets
            if (ch == '[') {
                i = skipBracketedContent(content, i);
                continue;
            }

            // Keep everything else (text and whitespace)
            result.append(ch);
            i++;
        }

        return result.toString();
    }

    /**
     * Skips content within braces, returning the index after the closing brace.
     */
    private static int skipBracedContent(String content, int startIndex) {
        if (startIndex >= content.length() || content.charAt(startIndex) != '{') {
            return startIndex;
        }

        var depth = 1;
        var i = startIndex + 1;

        while (i < content.length() && depth > 0) {
            char ch = content.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            }
            i++;
        }

        return i;
    }

    /**
     * Skips content within brackets, returning the index after the closing bracket.
     */
    private static int skipBracketedContent(String content, int startIndex) {
        if (startIndex >= content.length() || content.charAt(startIndex) != '[') {
            return startIndex;
        }

        var i = startIndex + 1;

        while (i < content.length() && content.charAt(i) != ']') {
            i++;
        }

        if (i < content.length()) {
            i++;  // skip the closing bracket
        }

        return i;
    }

    /**
     * Counts words in cleaned text by splitting on whitespace.
     */
    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        var matcher = WORD_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Exception thrown when a document section exceeds the word limit.
     */
    public static final class WordLimitExceededException extends RuntimeException {
        /**
         * Creates a new exception with the given message.
         */
        public WordLimitExceededException(String message) {
            super(message);
        }

        /**
         * Creates a new exception with message and cause.
         */
        public WordLimitExceededException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

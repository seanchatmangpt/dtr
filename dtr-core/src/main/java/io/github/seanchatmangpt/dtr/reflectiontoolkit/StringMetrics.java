/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.seanchatmangpt.dtr.reflectiontoolkit;

/**
 * Record capturing detailed metrics about a string's content and composition.
 *
 * <p>This immutable record represents various statistical measures of a string:
 * word count, line count, character count, unique character count, letter count,
 * and non-ASCII character count. These metrics are useful for text analysis, documentation
 * generation, and content validation in API testing frameworks.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * String text = "Hello World\nFoo Bar";
 * StringMetrics metrics = new StringMetrics(
 *     4,          // wordCount: "Hello", "World", "Foo", "Bar"
 *     2,          // lineCount: 2 lines
 *     19,         // characterCount: total chars including whitespace/newlines
 *     13,         // uniqueCharCount: distinct characters
 *     8,          // letterCount: letters only (H,e,l,l,o,F,o,B,a,r minus duplicates)
 *     0           // nonAsciiCount: 0 (all ASCII)
 * );
 *
 * System.out.println(metrics.wordCount());           // 4
 * System.out.println(metrics.lineCount());           // 2
 * System.out.println(metrics.characterCount());      // 19
 * }</pre>
 *
 * <p><strong>Compact Constructor Validation:</strong>
 * The compact canonical constructor validates:
 * <ul>
 *   <li>All counts are non-negative (>= 0)</li>
 *   <li>{@code uniqueCharCount} does not exceed {@code characterCount}</li>
 *   <li>{@code letterCount} does not exceed {@code characterCount}</li>
 *   <li>{@code nonAsciiCount} does not exceed {@code characterCount}</li>
 * </ul>
 *
 * @param wordCount          Number of words in the string (split by whitespace)
 * @param lineCount          Number of lines (separated by newline characters)
 * @param characterCount     Total number of characters including whitespace
 * @param uniqueCharCount    Number of unique/distinct characters
 * @param letterCount        Number of alphabetic characters (a-zA-Z)
 * @param nonAsciiCount      Number of non-ASCII characters (codepoint > 127)
 *
 * @since Java 26
 */
public record StringMetrics(
        long wordCount,
        long lineCount,
        long characterCount,
        long uniqueCharCount,
        long letterCount,
        long nonAsciiCount) {

    /**
     * Compact canonical constructor with defensive validation.
     *
     * @throws IllegalArgumentException if any count is negative or if counts are logically inconsistent
     */
    public StringMetrics {
        if (wordCount < 0) {
            throw new IllegalArgumentException("wordCount cannot be negative, got " + wordCount);
        }
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount cannot be negative, got " + lineCount);
        }
        if (characterCount < 0) {
            throw new IllegalArgumentException("characterCount cannot be negative, got " + characterCount);
        }
        if (uniqueCharCount < 0) {
            throw new IllegalArgumentException("uniqueCharCount cannot be negative, got " + uniqueCharCount);
        }
        if (letterCount < 0) {
            throw new IllegalArgumentException("letterCount cannot be negative, got " + letterCount);
        }
        if (nonAsciiCount < 0) {
            throw new IllegalArgumentException("nonAsciiCount cannot be negative, got " + nonAsciiCount);
        }

        // Logical consistency checks
        if (uniqueCharCount > characterCount) {
            throw new IllegalArgumentException(
                "uniqueCharCount (" + uniqueCharCount + ") cannot exceed characterCount (" + characterCount + ")");
        }
        if (letterCount > characterCount) {
            throw new IllegalArgumentException(
                "letterCount (" + letterCount + ") cannot exceed characterCount (" + characterCount + ")");
        }
        if (nonAsciiCount > characterCount) {
            throw new IllegalArgumentException(
                "nonAsciiCount (" + nonAsciiCount + ") cannot exceed characterCount (" + characterCount + ")");
        }
    }
}

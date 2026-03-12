package io.github.seanchatmangpt.dtr.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StringEscapeUtils escape methods.
 */
@DisplayName("StringEscapeUtils")
class StringEscapeUtilsTest {

    // ==================== LaTeX Tests ====================

    @Test
    @DisplayName("escapeLaTeX handles null input")
    void testEscapeLatexNull() {
        assertEquals("", StringEscapeUtils.escapeLaTeX(null));
    }

    @Test
    @DisplayName("escapeLaTeX handles empty string")
    void testEscapeLatexEmpty() {
        assertEquals("", StringEscapeUtils.escapeLaTeX(""));
    }

    @ParameterizedTest
    @CsvSource({
        "'test', 'test'",
        "'hello world', 'hello world'",
        "'simple text', 'simple text'"
    })
    @DisplayName("escapeLaTeX passes through plain text")
    void testEscapeLatexPlainText(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeLaTeX(input));
    }

    @Test
    @DisplayName("escapeLaTeX escapes backslash")
    void testEscapeLatexBackslash() {
        assertEquals("\\textbackslash{}", StringEscapeUtils.escapeLaTeX("\\"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes ampersand")
    void testEscapeLatexAmpersand() {
        assertEquals("\\&", StringEscapeUtils.escapeLaTeX("&"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes percent")
    void testEscapeLatexPercent() {
        assertEquals("\\%", StringEscapeUtils.escapeLaTeX("%"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes dollar")
    void testEscapeLatexDollar() {
        assertEquals("\\$", StringEscapeUtils.escapeLaTeX("$"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes hash")
    void testEscapeLatexHash() {
        assertEquals("\\#", StringEscapeUtils.escapeLaTeX("#"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes underscore")
    void testEscapeLatexUnderscore() {
        assertEquals("\\_", StringEscapeUtils.escapeLaTeX("_"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes braces")
    void testEscapeLatexBraces() {
        assertEquals("\\{", StringEscapeUtils.escapeLaTeX("{"));
        assertEquals("\\}", StringEscapeUtils.escapeLaTeX("}"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes tilde")
    void testEscapeLatexTilde() {
        assertEquals("\\textasciitilde{}", StringEscapeUtils.escapeLaTeX("~"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes caret")
    void testEscapeLatexCaret() {
        assertEquals("\\textasciicircum{}", StringEscapeUtils.escapeLaTeX("^"));
    }

    @Test
    @DisplayName("escapeLaTeX escapes pipe")
    void testEscapeLatexPipe() {
        assertEquals("\\textbar{}", StringEscapeUtils.escapeLaTeX("|"));
    }

    @Test
    @DisplayName("escapeLaTeX handles multiple special characters")
    void testEscapeLatexMultiple() {
        String input = "Price: $100 & profit = 50%";
        String expected = "Price: \\$100 \\& profit = 50\\%";
        assertEquals(expected, StringEscapeUtils.escapeLaTeX(input));
    }

    // ==================== JSON Tests ====================

    @Test
    @DisplayName("escapeJson handles null input")
    void testEscapeJsonNull() {
        assertEquals("", StringEscapeUtils.escapeJson(null));
    }

    @Test
    @DisplayName("escapeJson handles empty string")
    void testEscapeJsonEmpty() {
        assertEquals("", StringEscapeUtils.escapeJson(""));
    }

    @ParameterizedTest
    @CsvSource({
        "'test', 'test'",
        "'hello world', 'hello world'"
    })
    @DisplayName("escapeJson passes through plain text")
    void testEscapeJsonPlainText(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeJson(input));
    }

    @Test
    @DisplayName("escapeJson escapes backslash")
    void testEscapeJsonBackslash() {
        assertEquals("\\\\", StringEscapeUtils.escapeJson("\\"));
    }

    @Test
    @DisplayName("escapeJson escapes quotes")
    void testEscapeJsonQuote() {
        assertEquals("\\\"", StringEscapeUtils.escapeJson("\""));
    }

    @Test
    @DisplayName("escapeJson escapes newline")
    void testEscapeJsonNewline() {
        String input = "line1\nline2";
        String expected = "line1\\nline2";
        assertEquals(expected, StringEscapeUtils.escapeJson(input));
    }

    @Test
    @DisplayName("escapeJson escapes carriage return")
    void testEscapeJsonCarriageReturn() {
        String input = "line1\rline2";
        String expected = "line1\\rline2";
        assertEquals(expected, StringEscapeUtils.escapeJson(input));
    }

    @Test
    @DisplayName("escapeJson escapes tab")
    void testEscapeJsonTab() {
        String input = "col1\tcol2";
        String expected = "col1\\tcol2";
        assertEquals(expected, StringEscapeUtils.escapeJson(input));
    }

    // ==================== YAML Tests ====================

    @Test
    @DisplayName("escapeYaml handles null input")
    void testEscapeYamlNull() {
        assertEquals("", StringEscapeUtils.escapeYaml(null));
    }

    @Test
    @DisplayName("escapeYaml handles empty string")
    void testEscapeYamlEmpty() {
        assertEquals("", StringEscapeUtils.escapeYaml(""));
    }

    @ParameterizedTest
    @CsvSource({
        "'test', 'test'",
        "'hello world', 'hello world'"
    })
    @DisplayName("escapeYaml passes through plain text")
    void testEscapeYamlPlainText(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeYaml(input));
    }

    @Test
    @DisplayName("escapeYaml escapes single quotes")
    void testEscapeYamlSingleQuote() {
        String input = "it's";
        String expected = "it''s";
        assertEquals(expected, StringEscapeUtils.escapeYaml(input));
    }

    @Test
    @DisplayName("escapeYaml escapes multiple single quotes")
    void testEscapeYamlMultipleSingleQuotes() {
        String input = "'quoted'";
        String expected = "''quoted''";
        assertEquals(expected, StringEscapeUtils.escapeYaml(input));
    }

    // ==================== HTML Tests ====================

    @Test
    @DisplayName("escapeHtml handles null input")
    void testEscapeHtmlNull() {
        assertEquals("", StringEscapeUtils.escapeHtml(null));
    }

    @Test
    @DisplayName("escapeHtml handles empty string")
    void testEscapeHtmlEmpty() {
        assertEquals("", StringEscapeUtils.escapeHtml(""));
    }

    @ParameterizedTest
    @CsvSource({
        "'test', 'test'",
        "'hello world', 'hello world'"
    })
    @DisplayName("escapeHtml passes through plain text")
    void testEscapeHtmlPlainText(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeHtml(input));
    }

    @ParameterizedTest
    @CsvSource({
        "'&', '&amp;'",
        "'<', '&lt;'",
        "'>', '&gt;'",
        "'\"', '&quot;'",
        "'''', '&#39;'"
    })
    @DisplayName("escapeHtml escapes special HTML characters")
    void testEscapeHtmlSpecialChars(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeHtml(input));
    }

    @Test
    @DisplayName("escapeHtml handles multiple special characters")
    void testEscapeHtmlMultiple() {
        String input = "<div>\"test\" & 'quoted'</div>";
        String expected = "&lt;div&gt;&quot;test&quot; &amp; &#39;quoted&#39;&lt;/div&gt;";
        assertEquals(expected, StringEscapeUtils.escapeHtml(input));
    }

    // ==================== BibTeX Tests ====================

    @Test
    @DisplayName("escapeBibValue handles null input")
    void testEscapeBibValueNull() {
        assertEquals("", StringEscapeUtils.escapeBibValue(null));
    }

    @Test
    @DisplayName("escapeBibValue handles empty string")
    void testEscapeBibValueEmpty() {
        assertEquals("", StringEscapeUtils.escapeBibValue(""));
    }

    @ParameterizedTest
    @CsvSource({
        "'test', 'test'",
        "'simple text', 'simple text'"
    })
    @DisplayName("escapeBibValue passes through plain text")
    void testEscapeBibValuePlainText(String input, String expected) {
        assertEquals(expected, StringEscapeUtils.escapeBibValue(input));
    }

    @Test
    @DisplayName("escapeBibValue escapes backslash")
    void testEscapeBibValueBackslash() {
        String input = "C:\\path";
        String expected = "C:\\\\path";
        assertEquals(expected, StringEscapeUtils.escapeBibValue(input));
    }

    @Test
    @DisplayName("escapeBibValue escapes quotes")
    void testEscapeBibValueQuote() {
        String input = "\"quoted\"";
        String expected = "\\\"quoted\\\"";
        assertEquals(expected, StringEscapeUtils.escapeBibValue(input));
    }

    @Test
    @DisplayName("escapeBibValue replaces newline with space")
    void testEscapeBibValueNewline() {
        String input = "line1\nline2";
        String expected = "line1 line2";
        assertEquals(expected, StringEscapeUtils.escapeBibValue(input));
    }

    @Test
    @DisplayName("escapeBibValue handles combination of escapes")
    void testEscapeBibValueCombination() {
        String input = "C:\\Program Files\\\"My App\"";
        String expected = "C:\\\\Program Files\\\\\\\"My App\\\"";
        assertEquals(expected, StringEscapeUtils.escapeBibValue(input));
    }
}

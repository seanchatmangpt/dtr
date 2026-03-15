package io.github.seanchatmangpt.dtr.util;

/**
 * Utility class for escaping strings to various formats (LaTeX, JSON, YAML, HTML, BibTeX).
 * Consolidates duplicate escape logic from multiple template and writer classes.
 */
public final class StringEscapeUtils {

    private StringEscapeUtils() {
        // Utility class, no instantiation
    }

    /**
     * Escapes special LaTeX characters in the given string.
     *
     * @param text the text to escape
     * @return escaped text safe for LaTeX
     */
    public static String escapeLaTeX(String text) {
        if (text == null || text.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — caller passes non-null non-empty input
        }

        return text
            .replace("\\", "\u0000")  // Placeholder for backslash
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}")
            .replace("|", "\\textbar{}")
            .replace("\u0000", "\\textbackslash{}");  // Replace placeholder with actual escape
    }

    /**
     * Escapes special JSON characters in the given string.
     *
     * @param text the text to escape
     * @return escaped text safe for JSON
     */
    public static String escapeJson(String text) {
        if (text == null) {
            return "";  // hguard-ok: null guard — empty string is the correct contract for null input
        }
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Escapes special YAML characters in the given string.
     *
     * @param text the text to escape
     * @return escaped text safe for YAML
     */
    public static String escapeYaml(String text) {
        if (text == null) {
            return "";  // hguard-ok: null guard — empty string is the correct contract for null input
        }
        return text.replace("'", "''");
    }

    /**
     * Escapes special HTML characters in the given string.
     *
     * @param text the text to escape
     * @return escaped text safe for HTML
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";  // hguard-ok: null guard — empty string is the correct contract for null input
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Escapes special BibTeX characters in the given string.
     *
     * @param value the raw value to escape
     * @return escaped value safe for BibTeX
     */
    public static String escapeBibValue(String value) {
        if (value == null) {
            return "";  // hguard-ok: null guard — empty string is the correct contract for null input
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ");
    }
}

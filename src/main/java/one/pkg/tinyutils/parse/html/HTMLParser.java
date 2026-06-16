package one.pkg.tinyutils.parse.html;

import one.pkg.tinyutils.Collections;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

public class HTMLParser {
    private static final Map<String, String> HTML_ENTITY_UNESCAPE_MAP = Collections.newLinkedHashMap(5);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern LEADING_WHITESPACE_PATTERN = Pattern.compile("^\\s+");
    private static final Pattern BR_PATTERN = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern MULTIPLE_NEWLINES_PATTERN = Pattern.compile("\\n+");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile(" {2,}");

    static {
        HTML_ENTITY_UNESCAPE_MAP.put("&lt;", "<");
        HTML_ENTITY_UNESCAPE_MAP.put("&gt;", ">");
        HTML_ENTITY_UNESCAPE_MAP.put("&quot;", "\"");
        HTML_ENTITY_UNESCAPE_MAP.put("&#39;", "'");
        HTML_ENTITY_UNESCAPE_MAP.put("&amp;", "&");
    }

    /**
     * Decodes a Base64 encoded string and removes any HTML tags from the decoded output.
     * <p>
     * Also trims leading whitespace from the resulting string.
     *
     * @param base64EncodedString the Base64 encoded string containing potential HTML content
     * @return the decoded string with HTML tags removed and leading whitespace trimmed
     */
    public static String decodeAndStripHtml(String base64EncodedString) {
        String decoded = new String(Base64.getDecoder().decode(base64EncodedString));
        String withoutTags = HTML_TAG_PATTERN.matcher(decoded).replaceAll("");
        return LEADING_WHITESPACE_PATTERN.matcher(withoutTags).replaceFirst("");
    }

    /**
     * Escapes special HTML characters in a given string.
     *
     * @param input the raw string to be escaped
     * @return the escaped string suitable for HTML content
     */
    public static String escapeHtml(String input) {
        // Bolt: Optimization - Avoid intermediate string allocations and early return if no escaping needed
        if (input == null || input.isEmpty()) {
            return input;
        }

        int len = input.length();
        int i = 0;
        for (; i < len; i++) {
            char c = input.charAt(i);
            if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
                break;
            }
        }

        if (i == len) {
            return input;
        }

        StringBuilder escaped = new StringBuilder(len + 16);
        escaped.append(input, 0, i);

        for (; i < len; i++) {
            char c = input.charAt(i);
            if (c == '&') escaped.append("&amp;");
            else if (c == '<') escaped.append("&lt;");
            else if (c == '>') escaped.append("&gt;");
            else if (c == '"') escaped.append("&quot;");
            else if (c == '\'') escaped.append("&#39;");
            else escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * Cleans the provided input string by removing HTML tags, normalizing line breaks,
     * replacing consecutive spaces with a single space, and trimming leading and trailing whitespace.
     *
     * @param input the input string to be cleaned; must not be null or blank
     * @return the cleaned string with HTML tags removed, normalized line breaks, trimmed whitespace,
     * and reduced consecutive spaces
     */
    public static @NotNull String clean(@NotNull String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String normalizedBreaks = BR_PATTERN.matcher(input).replaceAll("\n");
        String withoutHtmlTags = HTML_TAG_PATTERN.matcher(normalizedBreaks).replaceAll("");
        String singleNewlines = MULTIPLE_NEWLINES_PATTERN.matcher(withoutHtmlTags).replaceAll("\n");
        String singleSpaces = MULTIPLE_SPACES_PATTERN.matcher(singleNewlines).replaceAll(" ");

        return singleSpaces.trim();
    }

    /**
     * Unescapes special HTML characters in a given string.
     *
     * @param escapedString the string with escaped HTML characters
     * @return the unescaped raw string
     */
    public static String unescapeHtml(String escapedString) {
        // Bolt: Optimization - Avoid intermediate substring allocations and Map lookups by parsing manually
        int ampIdx = escapedString.indexOf('&');
        if (ampIdx == -1) {
            return escapedString;
        }
        StringBuilder result = new StringBuilder(escapedString.length());
        result.append(escapedString, 0, ampIdx);
        int len = escapedString.length();
        for (int i = ampIdx; i < len; i++) {
            char current = escapedString.charAt(i);
            if (current == '&') {
                int semicolonIndex = escapedString.indexOf(';', i);
                if (semicolonIndex > i) {
                    int entityLen = semicolonIndex - i + 1;
                    if (entityLen == 4) {
                        if (escapedString.charAt(i+1) == 'l' && escapedString.charAt(i+2) == 't') {
                            result.append('<');
                            i = semicolonIndex;
                            continue;
                        } else if (escapedString.charAt(i+1) == 'g' && escapedString.charAt(i+2) == 't') {
                            result.append('>');
                            i = semicolonIndex;
                            continue;
                        }
                    } else if (entityLen == 5) {
                        if (escapedString.charAt(i+1) == 'a' && escapedString.charAt(i+2) == 'm' && escapedString.charAt(i+3) == 'p') {
                            result.append('&');
                            i = semicolonIndex;
                            continue;
                        } else if (escapedString.charAt(i+1) == '#' && escapedString.charAt(i+2) == '3' && escapedString.charAt(i+3) == '9') {
                            result.append('\'');
                            i = semicolonIndex;
                            continue;
                        }
                    } else if (entityLen == 6) {
                        if (escapedString.charAt(i+1) == 'q' && escapedString.charAt(i+2) == 'u' && escapedString.charAt(i+3) == 'o' && escapedString.charAt(i+4) == 't') {
                            result.append('"');
                            i = semicolonIndex;
                            continue;
                        }
                    }
                }
            }
            result.append(current);
        }
        return result.toString();
    }
}

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
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(c);
            }
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
        if (escapedString.indexOf('&') == -1) {
            return escapedString;
        }
        StringBuilder result = new StringBuilder(escapedString.length());
        int len = escapedString.length();
        for (int i = 0; i < len; i++) {
            char current = escapedString.charAt(i);
            if (current == '&') {
                int semicolonIndex = escapedString.indexOf(';', i);
                if (semicolonIndex > i) {
                    String entity = escapedString.substring(i, semicolonIndex + 1);
                    String replacement = HTML_ENTITY_UNESCAPE_MAP.get(entity);
                    if (replacement != null) {
                        result.append(replacement);
                        i = semicolonIndex;
                        continue;
                    }
                }
            }
            result.append(current);
        }
        return result.toString();
    }
}

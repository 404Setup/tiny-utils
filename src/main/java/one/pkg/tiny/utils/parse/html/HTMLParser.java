package one.pkg.tiny.utils.parse.html;

import one.pkg.tiny.utils.Collections;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Map;

public class HTMLParser {
    private static final Map<String, String> HTML_ENTITY_UNESCAPE_MAP = Collections.newLinkedHashMap(5);

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
        return new String(Base64.getDecoder().decode(base64EncodedString)).replaceAll("<[^>]+>", "").replaceFirst("^\\s+", "");
    }

    /**
     * Escapes special HTML characters in a given string.
     *
     * @param input the raw string to be escaped
     * @return the escaped string suitable for HTML content
     */
    public static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder escaped = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            escaped.append(getHtmlEscapedChar(c));
        }
        return escaped.toString();
    }

    /**
     * Converts a single character to its HTML-escaped equivalent if it is a special character.
     * <p>
     * If the character does not require escaping, the character itself is returned as a string.
     *
     * @param c the character to be HTML-escaped
     * @return the HTML-escaped string representation of the character, or the character itself as a string if no escaping is required
     */
    private static String getHtmlEscapedChar(char c) {
        return switch (c) {
            case '&' -> "&amp;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            case '"' -> "&quot;";
            case '\'' -> "&#39;";
            default -> String.valueOf(c);
        };
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

        final String BR_REGEX = "(?i)<br\\s*/?>";
        final String HTML_TAG_REGEX = "<[^>]+>";
        final String MULTIPLE_NEWLINES_REGEX = "\\n+";
        final String MULTIPLE_SPACES_REGEX = " {2,}";

        String normalizedBreaks = input.replaceAll(BR_REGEX, "\n");
        String withoutHtmlTags = normalizedBreaks.replaceAll(HTML_TAG_REGEX, "");
        String singleNewlines = withoutHtmlTags.replaceAll(MULTIPLE_NEWLINES_REGEX, "\n");
        String singleSpaces = singleNewlines.replaceAll(MULTIPLE_SPACES_REGEX, " ");

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

package one.pkg.tinyutils.parse.uuid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UUIDParser {
    private static final Pattern UUID_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    /**
     * Formats a given string representation of a UUID into its canonical form with dashes.
     * The input must be a valid UUID string without dashes; otherwise, the method will return null.
     *
     * @param uuidString the input string representing a UUID, expected without dashes
     *                   and non-null, must match the pattern of a valid UUID.
     * @return the formatted UUID as a {@link UUID} object if the input matches a valid format;
     * null otherwise.
     */
    public static @Nullable UUID format(@NotNull String uuidString) {
        if (uuidString == null || uuidString.isBlank()) return null;

        Matcher matcher = UUID_PATTERN.matcher(uuidString);
        if (matcher.matches()) {
            String formattedUUID = String.format("%s-%s-%s-%s-%s",
                    matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
            return UUID.fromString(formattedUUID);
        }
        return null;
    }

    /**
     * Removes all dashes from the string representation of the given UUID.
     *
     * @param uuid the UUID from which to remove dashes, must not be null
     * @return the string representation of the UUID without dashes
     */
    public static @NotNull String removeDashes(@NotNull UUID uuid) {
        return removeDashes(uuid.toString());
    }

    /**
     * Removes all dashes from the given UUID string and converts it to lowercase.
     *
     * @param uuidString the input string representing a UUID, expected to contain dashes and not null
     * @return the UUID string with all dashes removed, converted to lowercase
     */
    public static @NotNull String removeDashes(@NotNull String uuidString) {
        return uuidString.replace("-", "").toLowerCase();
    }
}

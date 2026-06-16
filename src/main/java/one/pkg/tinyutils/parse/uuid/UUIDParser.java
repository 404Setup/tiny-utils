package one.pkg.tinyutils.parse.uuid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDParser {

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
        if (uuidString == null || uuidString.length() != 32) return null;

        try {
            // Optimization: Avoid regex matcher and String.format by parsing bits directly.
            // This is significantly faster and allocates fewer objects.
            long mostSigBits = Long.parseUnsignedLong(uuidString, 0, 16, 16);
            long leastSigBits = Long.parseUnsignedLong(uuidString, 16, 32, 16);
            return new UUID(mostSigBits, leastSigBits);
        } catch (NumberFormatException e) {
            // If the string contains non-hex characters, it's not a valid UUID string.
            // The original regex matcher would have failed to match, returning null.
            return null;
        }
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

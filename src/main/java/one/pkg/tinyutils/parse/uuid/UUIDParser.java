package one.pkg.tinyutils.parse.uuid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDParser {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

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
     * <p>
     * Optimization: Avoids intermediate string allocations by manually formatting bits.
     *
     * @param uuid the UUID from which to remove dashes, must not be null
     * @return the string representation of the UUID without dashes
     */
    public static @NotNull String removeDashes(@NotNull UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();

        char[] result = new char[32];
        formatHex(most, result, 0);
        formatHex(least, result, 16);

        return new String(result);
    }

    private static void formatHex(long value, char[] dest, int offset) {
        for (int i = 15; i >= 0; i--) {
            dest[offset + i] = HEX_DIGITS[(int) (value & 0xF)];
            value >>>= 4;
        }
    }

    /**
     * Removes all dashes from the given UUID string and converts it to lowercase.
     * <p>
     * Optimization: Processes chars directly to avoid multiple string allocations from replace() and toLowerCase().
     *
     * @param uuidString the input string representing a UUID, expected to contain dashes and not null
     * @return the UUID string with all dashes removed, converted to lowercase
     */
    public static @NotNull String removeDashes(@NotNull String uuidString) {
        if (uuidString.length() == 36) {
            char[] result = new char[32];
            int j = 0;
            for (int i = 0; i < 36; i++) {
                char c = uuidString.charAt(i);
                if (c != '-') {
                    if (c >= 'A' && c <= 'Z') c = (char) (c + 32);
                    if (j < 32) {
                        result[j] = c;
                    }
                    j++;
                }
            }
            if (j == 32) return new String(result);
        }
        return uuidString.replace("-", "").toLowerCase();
    }
}

package one.pkg.tinyutils.parse.version;

import one.pkg.tinyutils.Collections;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The VersionComparator class provides utility methods for comparing software version strings.
 */
public class VersionComparator {
    private static final String DEFAULT_VERSION_SEGMENT = "0";

    /**
     * Checks if the given string consists only of numeric digits.
     * Bolt: Optimization - Avoid regex matcher for performance
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits the given version string into its individual components.
     *
     * @param version the version string to be split into components
     * @return a list of strings where the initial components are the numeric parts of the version,
     * followed by the optional suffix, if present
     */
    private static List<String> splitVersion(String version) {
        // Bolt: Optimization - Use indexOf and substring instead of split() for zero allocation parsing
        int dashIdx = version.indexOf('-');
        int plusIdx = version.indexOf('+');
        int endIdx = version.length();

        if (dashIdx != -1) endIdx = Math.min(endIdx, dashIdx);
        if (plusIdx != -1) endIdx = Math.min(endIdx, plusIdx);

        String mainVersion = version.substring(0, endIdx);

        // Bolt: Optimization - Avoid regex Pattern.compile internally by manually parsing
        int count = 1;
        for (int i = 0; i < mainVersion.length(); i++) {
            if (mainVersion.charAt(i) == '.') count++;
        }

        String[] versionParts = new String[count];
        int start = 0;
        int partIdx = 0;
        for (int i = 0; i < mainVersion.length(); i++) {
            if (mainVersion.charAt(i) == '.') {
                versionParts[partIdx++] = mainVersion.substring(start, i);
                start = i + 1;
            }
        }
        versionParts[partIdx] = mainVersion.substring(start);

        String suffix = dashIdx != -1 ? version.substring(dashIdx + 1) : "";
        List<String> result = Collections.newArrayList(versionParts);
        result.add(suffix);
        return result;
    }

    /**
     * Compares two version strings and returns an integer indicating their relative order.
     *
     * @param segment1 the local version string to be compared
     * @param segment2 the remote version string to be compared
     * @return a negative integer if vLocal is less than vRemote,
     * zero if both versions are equal,
     * or a positive integer if vLocal is greater than vRemote
     */
    private static int compareVersionSegments(String segment1, String segment2) {
        // Bolt: Optimization - Use manual character loop instead of regex matcher for checking numeric strings
        boolean isSegment1Numeric = isNumeric(segment1);
        boolean isSegment2Numeric = isNumeric(segment2);

        if (isSegment1Numeric && isSegment2Numeric) {
            return Integer.compare(Integer.parseInt(segment1), Integer.parseInt(segment2));
        } else if (isSegment1Numeric) {
            return 1;
        } else if (isSegment2Numeric) {
            return -1;
        } else {
            return segment1.compareToIgnoreCase(segment2);
        }
    }

    /**
     * Compares two version strings and returns an integer indicating their relative order.
     *
     * @param localVersion  the local version string to be compared
     * @param remoteVersion the remote version string to be compared
     * @return a negative integer if localVersion is less than remoteVersion,
     * zero if both versions are equal,
     * or a positive integer if localVersion is greater than remoteVersion
     */
    public static int compareVersions(String localVersion, String remoteVersion) {
        List<String> localSegments = splitVersion(localVersion);
        List<String> remoteSegments = splitVersion(remoteVersion);

        int maxSegments = Math.max(localSegments.size(), remoteSegments.size());

        for (int i = 0; i < maxSegments; i++) {
            String localPart = i < localSegments.size() ? localSegments.get(i) : DEFAULT_VERSION_SEGMENT;
            String remotePart = i < remoteSegments.size() ? remoteSegments.get(i) : DEFAULT_VERSION_SEGMENT;

            int comparisonResult = compareVersionSegments(localPart, remotePart);
            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }

        return 0;
    }
}

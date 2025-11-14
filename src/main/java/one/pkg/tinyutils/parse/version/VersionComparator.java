package one.pkg.tinyutils.parse.version;

import one.pkg.tinyutils.Collections;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The VersionComparator class provides utility methods for comparing software version strings.
 */
public class VersionComparator {
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
    private static final String DEFAULT_VERSION_SEGMENT = "0";

    /**
     * Splits the given version string into its individual components.
     *
     * @param version the version string to be split into components
     * @return a list of strings where the initial components are the numeric parts of the version,
     * followed by the optional suffix, if present
     */
    private static List<String> splitVersion(String version) {
        String mainVersion = version.split("-")[0].split("\\+")[0];
        String[] versionParts = mainVersion.split("\\.");
        String suffix = version.contains("-") ? version.substring(version.indexOf('-') + 1) : "";
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
        boolean isSegment1Numeric = NUMERIC_PATTERN.matcher(segment1).matches();
        boolean isSegment2Numeric = NUMERIC_PATTERN.matcher(segment2).matches();

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

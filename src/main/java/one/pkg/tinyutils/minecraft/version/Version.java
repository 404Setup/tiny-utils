package one.pkg.tinyutils.minecraft.version;


import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    private static final Pattern RELEASE_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Pattern SHORT_RELEASE_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)$");
    private static final Pattern PRE_RELEASE_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)-(pre|rc)(\\d+)$");
    private static final Pattern BETA_PATTERN = Pattern.compile("^b(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("^(\\d{2})w(\\d{2})([a-z])(?:_or_([a-z]))?$");

    private final String original;
    private final VersionType type;
    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    public Version(String original) {
        this.original = original;

        String trimmed = original.trim();
        int maj;
        int min;
        int pat;
        String pre = null;
        VersionType verType;

        Matcher matcher;
        if ((matcher = RELEASE_PATTERN.matcher(trimmed)).matches()) {
            maj = Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = Integer.parseInt(matcher.group(3));
            verType = VersionType.RELEASE;
        } else if ((matcher = SHORT_RELEASE_PATTERN.matcher(trimmed)).matches()) {
            maj = Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = 0;
            verType = VersionType.RELEASE;
        } else if ((matcher = PRE_RELEASE_PATTERN.matcher(trimmed)).matches()) {
            maj = Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = Integer.parseInt(matcher.group(3));
            String t = matcher.group(4);
            String n = matcher.group(5);
            pre = t + n;
            verType = t.equals("rc") ? VersionType.RELEASE_CANDIDATE : VersionType.PRE_RELEASE;
        } else if ((matcher = BETA_PATTERN.matcher(trimmed)).matches()) {
            maj = Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = Integer.parseInt(matcher.group(3));
            verType = VersionType.BETA;
        } else if ((matcher = SNAPSHOT_PATTERN.matcher(trimmed)).matches()) {
            maj = 2000 + Integer.parseInt(matcher.group(1));
            min = Integer.parseInt(matcher.group(2));
            pat = matcher.group(3).charAt(0) - 'a';
            verType = VersionType.SNAPSHOT;
        } else {
            String[] parts = trimmed.split("\\.");
            maj = parseIntOrZero(parts.length > 0 ? getDigits(parts[0]) : "0");
            min = parseIntOrZero(parts.length > 1 ? getDigits(parts[1]) : "0");
            pat = parseIntOrZero(parts.length > 2 ? getDigits(parts[2]) : "0");
            verType = (parts.length >= 2 && allDigits(parts)) ? VersionType.RELEASE : VersionType.UNKNOWN;
        }

        this.major = maj;
        this.minor = min;
        this.patch = pat;
        this.preRelease = pre;
        this.type = verType;
    }

    public Version(int major, int minor, int patch) {
        this(major + "." + minor + "." + patch);
    }

    private static String getDigits(String str) {
        StringBuilder digits = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) break;
            digits.append(c);
        }
        return digits.toString();
    }

    private static boolean allDigits(String[] parts) {
        for (String part : parts) {
            if (!part.matches("\\d+")) return false;
        }
        return true;
    }

    private static int parseIntOrZero(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        result = Integer.compare(this.typePriority(), other.typePriority());
        if (result != 0) return result;

        return Objects.compare(this.preRelease != null ? this.preRelease : "",
                other.preRelease != null ? other.preRelease : "",
                String::compareTo);
    }

    private int typePriority() {
        return switch (type) {
            case BETA -> 1;
            case SNAPSHOT -> 2;
            case PRE_RELEASE -> 3;
            case RELEASE_CANDIDATE -> 4;
            case RELEASE -> 5;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return original;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
                minor == version.minor &&
                patch == version.patch &&
                type == version.type &&
                Objects.equals(preRelease, version.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, type, preRelease);
    }

    public String getOriginal() {
        return original;
    }

    public VersionType getType() {
        return type;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    public enum VersionType {
        RELEASE,
        PRE_RELEASE,
        RELEASE_CANDIDATE,
        BETA,
        SNAPSHOT,
        UNKNOWN
    }
}
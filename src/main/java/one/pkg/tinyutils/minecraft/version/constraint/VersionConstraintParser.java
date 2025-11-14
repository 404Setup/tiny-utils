package one.pkg.tinyutils.minecraft.version.constraint;

import one.pkg.tinyutils.minecraft.version.Version;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class VersionConstraintParser {
    private static final String VERSION_PATTERN =
            "\\d+\\.\\d+\\.\\d+(?:-(?:pre|rc)\\d+)?|b\\d+\\.\\d+\\.\\d+|\\d{2}w\\d{2}[a-z](?:_or_[a-z])?|[\\w.-]+";

    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+){1,2})$");
    private static final Pattern EXACT_PATTERN = Pattern.compile("^(" + VERSION_PATTERN + ")$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(" + VERSION_PATTERN + ")-(" + VERSION_PATTERN + ")$");
    private static final Pattern EQUAL_PATTERN = Pattern.compile("^=(" + VERSION_PATTERN + ")$");
    private static final Pattern TILDE_PATTERN = Pattern.compile("^~(" + VERSION_PATTERN + ")$");
    private static final Pattern CARET_PATTERN = Pattern.compile("^\\^(" + VERSION_PATTERN + ")$");
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("^(>=|<=|>|<)\\s*(" + VERSION_PATTERN + ")$");
    private static final Pattern MAVEN_RANGE_PATTERN = Pattern.compile("^[\\[(]([\\w.,-]+)[])]$");
    private static final Pattern COMPOSITE_PATTERN = Pattern.compile("^(.+?)\\s+(.+)$");

    public static VersionConstraint parse(String constraintStr) throws IllegalArgumentException {
        String trimmed = constraintStr.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Version constraint cannot be empty");
        }

        // Check for simple version pattern first (e.g., 1.12.2)
        Matcher simpleMatcher = SIMPLE_VERSION_PATTERN.matcher(trimmed);
        if (simpleMatcher.matches()) {
            return new ExactVersionConstraint(simpleMatcher.group(1));
        }

        Matcher equalMatcher = EQUAL_PATTERN.matcher(trimmed);
        if (equalMatcher.matches()) return new ExactVersionConstraint(equalMatcher.group(1));

        Matcher rangeMatcher = RANGE_PATTERN.matcher(trimmed);
        if (rangeMatcher.matches()) {
            Version min = new Version(rangeMatcher.group(1));
            Version max = new Version(rangeMatcher.group(2));
            return new RangeConstraint(min, max, true, true, trimmed);
        }

        Matcher tildeMatcher = TILDE_PATTERN.matcher(trimmed);
        if (tildeMatcher.matches()) return new TildeConstraint(trimmed);

        Matcher caretMatcher = CARET_PATTERN.matcher(trimmed);
        if (caretMatcher.matches()) return new CaretConstraint(trimmed);

        Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(trimmed);
        if (comparisonMatcher.matches()) {
            return parseComparison(comparisonMatcher.group(1), comparisonMatcher.group(2), trimmed);
        }

        Matcher mavenRangeMatcher = MAVEN_RANGE_PATTERN.matcher(trimmed);
        if (mavenRangeMatcher.matches()) {
            return parseMavenRange(trimmed, mavenRangeMatcher.group(1));
        }

        Matcher compositeMatcher = COMPOSITE_PATTERN.matcher(trimmed);
        if (compositeMatcher.matches()) {
            try {
                VersionConstraint first = parse(compositeMatcher.group(1));
                VersionConstraint second = parse(compositeMatcher.group(2));
                return new CompositeConstraint(List.of(first, second), trimmed);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse version constraint: " + constraintStr);
            }
        }

        // Fallback to generic EXACT_PATTERN for other cases
        Matcher exactMatcher = EXACT_PATTERN.matcher(trimmed);
        if (exactMatcher.matches()) return new ExactVersionConstraint(exactMatcher.group(1));

        throw new IllegalArgumentException("Unable to parse version constraint: " + constraintStr);
    }

    private static VersionConstraint parseComparison(String operator, String versionStr, String original)
            throws IllegalArgumentException {
        Version version = new Version(versionStr);
        return switch (operator) {
            case ">=" -> new RangeConstraint(version, null, true, false, original);
            case "<=" -> new RangeConstraint(null, version, false, true, original);
            case ">" -> new RangeConstraint(version, null, false, false, original);
            case "<" -> new RangeConstraint(null, version, false, false, original);
            default -> throw new IllegalArgumentException("Unknown comparison operator: " + operator);
        };
    }

    private static VersionConstraint parseMavenRange(String original, String content) {
        boolean includeMin = original.startsWith("[");
        boolean includeMax = original.endsWith("]");

        String[] parts = content.split(",");
        switch (parts.length) {
            case 1:
                if (original.endsWith(",)")) {
                    Version min = new Version(parts[0]);
                    return new RangeConstraint(min, null, includeMin, false, original);
                } else {
                    return new ExactVersionConstraint(parts[0]);
                }

            case 2:
                Version min = new Version(parts[0]);
                Version max = parts[1].isEmpty() ? null : new Version(parts[1]);
                return new RangeConstraint(min, max, includeMin, includeMax, original);

            default:
                List<VersionConstraint> constraints = Arrays.stream(parts)
                        .filter(s -> !s.isEmpty())
                        .map(ExactVersionConstraint::new)
                        .collect(Collectors.toList());
                return new OrConstraint(constraints, original);
        }
    }
}
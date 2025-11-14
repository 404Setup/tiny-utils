package one.pkg.tinyutils.parse.time;


import one.pkg.tinyutils.exception.ForeverNonException;
import one.pkg.tinyutils.exception.ParseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * The TimeParser class provides functionality for parsing time-related string inputs
 * and performing operations such as calculating future times, validating time formats,
 * and checking time conditions.
 */
public class TimeParser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Parses a time argument string and returns the formatted future time or a special value such as "forever".
     *
     * @param arg the time argument as a string; can be literal "forever" or a duration string with a valid unit
     * @return a formatted string representing the calculated future time, or the literal "forever" for the special case
     * @throws ParseException if the argument is null, empty, contains invalid characters, has an unsupported unit,
     *                        or if the numeric portion of the duration is invalid
     */
    public static String parse(String arg) throws ParseException {
        if (arg == null || arg.isBlank()) {
            throw new ParseException("Time argument cannot be null, empty, or only whitespace.");
        }
        if (arg.equals("forever")) {
            return "forever";
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime future = calculateFutureTime(now, arg);
            return future.format(FORMATTER);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid numeric value in argument: '" + arg + "' - " + e.getMessage());
        }
    }

    private static LocalDateTime calculateFutureTime(LocalDateTime now, String arg) throws ParseException {
        for (TimeUnit unit : TimeUnit.values()) {
            if (arg.endsWith(unit.getSuffix())) {
                long value = parseTimeValue(unit.getSuffix(), arg);
                return unit.addTime(now, value);
            }
        }
        throw new ParseException("Invalid time format or unsupported time unit: " + arg);
    }

    /**
     * Parses a time value string with a specified unit tag and converts it into a numeric value.
     *
     * @param tag the unit tag that the time value must end with (e.g., "s" for seconds, "m" for minutes)
     * @param arg the time value string to parse; must end with the specified tag
     * @return the numeric value of the time, extracted from the string
     * @throws ParseException if the provided string does not end with the specified tag,
     *                        if the numeric value cannot be parsed, or if the value is negative
     */
    private static long parseTimeValue(String tag, String arg) throws ParseException {
        if (!arg.endsWith(tag)) {
            throw new ParseException("Time unit '" + tag + "' not found at the end of argument: " + arg);
        }
        String numberPart = arg.substring(0, arg.length() - tag.length());
        long t = Long.parseLong(numberPart);
        if (t < 0) {
            throw new ParseException("Time value cannot be negative: " + arg);
        }
        return t;
    }

    /**
     * Parses a string representation of a time into a {@code LocalDateTime} object.
     *
     * @param timeString the string representing the time to parse; must conform to the expected format
     *                   or be the literal "forever"
     * @return the parsed {@code LocalDateTime} object based on the input string
     * @throws ParseException      if the input string is invalid or cannot be parsed into a {@code LocalDateTime}
     * @throws ForeverNonException if the input string is the literal "forever"
     */
    public static LocalDateTime parseStringTime(String timeString) throws ParseException, ForeverNonException {
        if (Objects.equals(timeString, "forever")) {
            throw new ForeverNonException();
        }
        try {
            return LocalDateTime.parse(timeString, FORMATTER);
        } catch (Exception e) {
            throw new ParseException("Invalid time string format: '" + timeString + "'");
        }
    }

    /**
     * Checks if the given LocalDateTime is earlier than the current time.
     *
     * @param dateTime the LocalDateTime object to check
     * @return true if the specified time is in the past; false otherwise
     */
    public static boolean isTimeInPast(LocalDateTime dateTime) {
        return dateTime.isBefore(LocalDateTime.now());
    }
}

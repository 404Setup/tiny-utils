package one.pkg.tinyutils.parse.time;


import one.pkg.tinyutils.exception.ParseException;

import java.time.LocalDateTime;

/**
 * The TimeUnit enum represents various units of time, each of which can be used to
 * perform time-based calculations on a {@code LocalDateTime} object.
 * <p>
 * Each time unit is associated with a specific string suffix used for identifying units
 * (e.g., "s" for seconds, "m" for minutes), and provides an implementation for modifying
 * the given time by the specified value of the unit.
 * <p>
 * TimeUnit supports operations such as adding specific amounts of time to a {@code LocalDateTime}
 * instance and retrieving a unit from its designated string suffix.
 */
public enum TimeUnit {
    SECOND("s") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusSeconds(value);
        }
    },
    MINUTE("m") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusMinutes(value);
        }
    },
    HOUR("h") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusHours(value);
        }
    },
    DAY("d") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusDays(value);
        }
    },
    MONTH("mo") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusMonths(value);
        }
    },
    YEAR("y") {
        @Override
        public LocalDateTime addTime(LocalDateTime now, long value) {
            return now.plusYears(value);
        }
    };

    private final String suffix;

    /**
     * Constructs a new TimeUnit instance with the specified suffix.
     * <p>
     * The suffix represents the string identifier for the time unit (e.g., "s" for seconds, "m" for minutes).
     *
     * @param suffix the string identifier for the time unit
     */
    TimeUnit(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Determines the {@code TimeUnit} associated with the specified string suffix.
     * <p>
     * The method iterates over all available {@code TimeUnit} values, compares their
     * suffixes, and returns the matching unit.
     *
     * @param suffix the string representation of the time unit, such as "s" for seconds,
     *               "m" for minutes, etc.
     * @return the {@code TimeUnit} corresponding to the provided suffix
     * @throws ParseException if the specified suffix does not match any supported time unit
     */
    public static TimeUnit fromSuffix(String suffix) throws ParseException {
        for (TimeUnit unit : values()) {
            if (unit.getSuffix().equals(suffix)) {
                return unit;
            }
        }
        throw new ParseException("Unsupported time unit: " + suffix);
    }

    /**
     * Retrieves the string suffix associated with the current time unit.
     *
     * @return the suffix string that identifies the time unit (e.g., "s" for seconds, "m" for minutes)
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Adds a specified amount of time to a given {@code LocalDateTime} object.
     *
     * @param now   the initial {@code LocalDateTime} instance to which the time will be added
     * @param value the amount of time to add, specified as a long value
     * @return a new {@code LocalDateTime} instance with the added time
     */
    public abstract LocalDateTime addTime(LocalDateTime now, long value);

}

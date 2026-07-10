package com.databuff.apm.common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform wall-clock timezone. Portal datetime text and Doris {@code startTime}
 * are interpreted and formatted in {@link #SHANGHAI} (UTC+8).
 */
public final class ApmTimeZones {

    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    public static final String WALL_CLOCK_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** Accepts optional fractional seconds (e.g. Doris {@code FROM_UNIXTIME} microsecond suffix). */
    public static final DateTimeFormatter WALL_CLOCK_LOCAL = new DateTimeFormatterBuilder()
            .appendPattern(WALL_CLOCK_PATTERN)
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    public static final DateTimeFormatter WALL_CLOCK =
            WALL_CLOCK_LOCAL.withZone(SHANGHAI);
    private static final Map<String, DateTimeFormatter> FORMATTERS = new ConcurrentHashMap<>();

    private ApmTimeZones() {
    }

    /**
     * Normalizes portal wall-clock text before parsing. LLM tool calls sometimes drop the
     * space between date and time ({@code 2026-06-0812:27:00}) or use ISO {@code T} separator.
     */
    public static String normalizeWallClockText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String normalized = text.trim().replace('T', ' ');
        if (normalized.length() == 18
                && normalized.charAt(10) != ' '
                && normalized.charAt(4) == '-'
                && normalized.charAt(7) == '-'
                && normalized.charAt(12) == ':'
                && normalized.charAt(15) == ':') {
            normalized = normalized.substring(0, 10) + ' ' + normalized.substring(10);
        }
        return normalized;
    }

    public static long wallClockToEpochMilli(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }
        String trimmed = normalizeWallClockText(text);
        try {
            LocalDateTime local = LocalDateTime.parse(trimmed, WALL_CLOCK_LOCAL);
            return local.atZone(SHANGHAI).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            String iso = trimmed.replace(' ', 'T');
            if (!iso.contains("Z") && !iso.contains("+")) {
                iso += "Z";
            }
            return Instant.parse(iso).toEpochMilli();
        }
    }

    public static long wallClockToEpochSecond(String text) {
        if (text == null || text.isBlank()) {
            return 0L;
        }
        String trimmed = normalizeWallClockText(text);
        try {
            LocalDateTime local = LocalDateTime.parse(trimmed, WALL_CLOCK_LOCAL);
            return local.atZone(SHANGHAI).toEpochSecond();
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    public static String formatWallClock(long epochMilli) {
        return WALL_CLOCK.format(Instant.ofEpochMilli(epochMilli));
    }

    public static String formatBucket(long epochSecond, String pattern) {
        return FORMATTERS.computeIfAbsent(pattern, DateTimeFormatter::ofPattern)
                .format(Instant.ofEpochSecond(epochSecond).atZone(SHANGHAI));
    }

    /** Numeric {@code yyyyMMddHHmm} bucket for UTC-aligned minute epoch seconds. */
    public static long wallClockMinuteBucket(long utcAlignedMinuteEpochSecond) {
        ZonedDateTime zdt = Instant.ofEpochSecond(utcAlignedMinuteEpochSecond).atZone(SHANGHAI);
        return zdt.getYear() * 100_000_000L
                + zdt.getMonthValue() * 1_000_000L
                + zdt.getDayOfMonth() * 10_000L
                + zdt.getHour() * 100L
                + zdt.getMinute();
    }

    /** Numeric {@code yyyyMMddHH} bucket for UTC-aligned hour epoch seconds. */
    public static long wallClockHourBucket(long utcAlignedHourEpochSecond) {
        ZonedDateTime zdt = Instant.ofEpochSecond(utcAlignedHourEpochSecond).atZone(SHANGHAI);
        return zdt.getYear() * 1_000_000L
                + zdt.getMonthValue() * 10_000L
                + zdt.getDayOfMonth() * 100L
                + zdt.getHour();
    }
}

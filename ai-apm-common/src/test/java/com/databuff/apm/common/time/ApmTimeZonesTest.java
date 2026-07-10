package com.databuff.apm.common.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApmTimeZonesTest {

    @Test
    void wallClockRoundTripUsesShanghaiZone() {
        long millis = ApmTimeZones.wallClockToEpochMilli("2026-06-05 22:10:00");
        long expected = LocalDateTime.of(2026, 6, 5, 22, 10, 0)
                .atZone(ApmTimeZones.SHANGHAI)
                .toInstant()
                .toEpochMilli();
        assertThat(millis).isEqualTo(expected);
        assertThat(ApmTimeZones.formatWallClock(millis)).isEqualTo("2026-06-05 22:10:00");
    }

    @Test
    void wallClockToEpochSecondUsesShanghaiZone() {
        long seconds = ApmTimeZones.wallClockToEpochSecond("2026-06-05 22:10:00");
        long expected = LocalDateTime.of(2026, 6, 5, 22, 10, 0)
                .atZone(ApmTimeZones.SHANGHAI)
                .toEpochSecond();
        assertThat(seconds).isEqualTo(expected);
    }

    @Test
    void normalizesMissingSpaceBetweenDateAndTime() {
        assertThat(ApmTimeZones.normalizeWallClockText("2026-06-0812:27:00"))
                .isEqualTo("2026-06-08 12:27:00");
        long millis = ApmTimeZones.wallClockToEpochMilli("2026-06-0812:27:00");
        long expected = LocalDateTime.of(2026, 6, 8, 12, 27, 0)
                .atZone(ApmTimeZones.SHANGHAI)
                .toInstant()
                .toEpochMilli();
        assertThat(millis).isEqualTo(expected);
    }

    @Test
    void normalizesIsoTSeparator() {
        assertThat(ApmTimeZones.normalizeWallClockText("2026-06-08T12:27:00"))
                .isEqualTo("2026-06-08 12:27:00");
    }

    @Test
    void wallClockParsesDorisFromUnixTimeFraction() {
        long millis = ApmTimeZones.wallClockToEpochMilli("2026-06-05 22:10:00.000000");
        long expected = LocalDateTime.of(2026, 6, 5, 22, 10, 0)
                .atZone(ApmTimeZones.SHANGHAI)
                .toInstant()
                .toEpochMilli();
        assertThat(millis).isEqualTo(expected);
    }

    @Test
    void wallClockMinuteAndHourBucketsMatchFormatBucket() {
        long[] samples = {
                0L,
                1_700_000_000L,
                1_704_067_200L,
                ApmTimeZones.wallClockToEpochSecond("2026-06-05 22:10:00"),
        };
        for (long epochSec : samples) {
            long minuteAligned = (epochSec / 60) * 60;
            long hourAligned = (epochSec / 3600) * 3600;
            assertThat(ApmTimeZones.wallClockMinuteBucket(minuteAligned))
                    .isEqualTo(Long.parseLong(ApmTimeZones.formatBucket(minuteAligned, "yyyyMMddHHmm")));
            assertThat(ApmTimeZones.wallClockHourBucket(hourAligned))
                    .isEqualTo(Long.parseLong(ApmTimeZones.formatBucket(hourAligned, "yyyyMMddHH")));
        }
    }
}

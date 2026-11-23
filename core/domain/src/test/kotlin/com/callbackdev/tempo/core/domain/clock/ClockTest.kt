package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.FreeGap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ClockTest {
    @Test
    fun `the phone decides unless the reader did`() {
        assertThat(ClockFormat.SYSTEM.uses24Hour(phoneUses24Hour = true)).isTrue()
        assertThat(ClockFormat.SYSTEM.uses24Hour(phoneUses24Hour = false)).isFalse()
        assertThat(ClockFormat.H24.uses24Hour(phoneUses24Hour = false)).isTrue()
        assertThat(ClockFormat.H12.uses24Hour(phoneUses24Hour = true)).isFalse()
    }

    @Test
    fun `the ticker waits for the next minute's start`() {
        val now = Instant.parse("2026-10-07T09:41:15.250Z")
        assertThat(untilNextMinute(now)).isEqualTo(Duration.ofMillis(44_750))
    }

    @Test
    fun `at a minute's exact start the next is a whole minute away`() {
        val now = Instant.parse("2026-10-07T09:41:00Z")
        assertThat(untilNextMinute(now)).isEqualTo(Duration.ofMinutes(1))
    }

    @Test
    fun `a new event starts at the next half hour, strictly after now`() {
        val rome = ZoneId.of("Europe/Rome")
        fun at(local: String) = LocalDateTime.parse(local).atZone(rome).toInstant()
        assertThat(nextHalfHour(at("2026-10-07T10:05:30"), rome)).isEqualTo(at("2026-10-07T10:30"))
        assertThat(nextHalfHour(at("2026-10-07T10:00"), rome)).isEqualTo(at("2026-10-07T10:30"))
        assertThat(nextHalfHour(at("2026-10-07T10:31"), rome)).isEqualTo(at("2026-10-07T11:00"))
        assertThat(nextHalfHour(at("2026-10-07T23:45"), rome)).isEqualTo(at("2026-10-08T00:00"))
    }

    @Test
    fun `a free gap's new event starts with the gap, an hour long or as long as the gap`() {
        val now = at("2026-10-07T10:20:41")
        val hour = Duration.ofHours(1)
        assertThat(newEventIn(FreeGap(at("2026-10-07T11:00"), at("2026-10-07T13:00")), now, rome, hour))
            .isEqualTo(FreeGap(at("2026-10-07T11:00"), at("2026-10-07T12:00")))
        assertThat(newEventIn(FreeGap(at("2026-10-07T14:00"), at("2026-10-07T14:40")), now, rome, hour))
            .isEqualTo(FreeGap(at("2026-10-07T14:00"), at("2026-10-07T14:40")))
    }

    @Test
    fun `the gap open now gives an event at the next half hour, never at now's seconds`() {
        val hour = Duration.ofHours(1)
        val now = at("2026-10-07T08:07:41")
        assertThat(newEventIn(FreeGap(now, at("2026-10-07T10:00")), now, rome, hour))
            .isEqualTo(FreeGap(at("2026-10-07T08:30"), at("2026-10-07T09:30")))
        // The half hour inside the gap's end: the event stops with the gap.
        assertThat(newEventIn(FreeGap(now, at("2026-10-07T09:10")), now, rome, hour))
            .isEqualTo(FreeGap(at("2026-10-07T08:30"), at("2026-10-07T09:10")))
        // No half hour left in the gap: its whole minute.
        val late = at("2026-10-07T09:41:10")
        assertThat(newEventIn(FreeGap(late, at("2026-10-07T09:58")), late, rome, hour))
            .isEqualTo(FreeGap(at("2026-10-07T09:41"), at("2026-10-07T09:58")))
    }

    @Test
    fun `a new event today starts at the next half hour`() {
        val now = at("2026-10-07T10:05")
        assertThat(newEventOn(LocalDate.parse("2026-10-07"), now, rome)).isEqualTo(at("2026-10-07T10:30"))
    }

    @Test
    fun `on another date it starts at the same time of day, behind today or ahead`() {
        val now = at("2026-10-07T10:05")
        assertThat(newEventOn(LocalDate.parse("2026-10-22"), now, rome)).isEqualTo(at("2026-10-22T10:30"))
        assertThat(newEventOn(LocalDate.parse("2026-10-01"), now, rome)).isEqualTo(at("2026-10-01T10:30"))
        // Across the end of daylight saving (25 October): still half past ten on the wall.
        assertThat(newEventOn(LocalDate.parse("2026-11-12"), now, rome)).isEqualTo(at("2026-11-12T10:30"))
    }

    @Test
    fun `late in the evening another date starts in the morning, not at midnight`() {
        val now = at("2026-10-07T23:40")
        assertThat(newEventOn(LocalDate.parse("2026-10-09"), now, rome)).isEqualTo(at("2026-10-09T09:00"))
        assertThat(newEventOn(LocalDate.parse("2026-10-08"), now, rome)).isEqualTo(at("2026-10-08T09:00"))
        // Today keeps the button's own start, midnight included.
        assertThat(newEventOn(LocalDate.parse("2026-10-07"), now, rome)).isEqualTo(at("2026-10-08T00:00"))
    }

    @Test
    fun `a time the date's clock skips gives the morning`() {
        // 29 March 2026, Rome: 02:00 to 03:00 does not exist.
        val now = at("2026-03-20T02:10")
        assertThat(newEventOn(LocalDate.parse("2026-03-29"), now, rome)).isEqualTo(at("2026-03-29T09:00"))
    }

    private val rome = ZoneId.of("Europe/Rome")

    private fun at(local: String) = LocalDateTime.parse(local).atZone(rome).toInstant()
}

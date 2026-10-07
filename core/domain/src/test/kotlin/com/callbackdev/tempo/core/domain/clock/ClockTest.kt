package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.model.ClockFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
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
}

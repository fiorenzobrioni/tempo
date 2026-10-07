package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.model.ClockFormat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

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
}

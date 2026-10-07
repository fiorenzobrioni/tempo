package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.at
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AlarmDayTest {
    private val now = at("2026-10-07T22:00")

    @Test
    fun `today, tomorrow, or a named day`() {
        assertThat(AlarmDay.of(at("2026-10-07T23:30"), now, Rome)).isEqualTo(AlarmDay.Today)
        assertThat(AlarmDay.of(at("2026-10-08T07:00"), now, Rome)).isEqualTo(AlarmDay.Tomorrow)
        assertThat(AlarmDay.of(at("2026-10-10T07:00"), now, Rome)).isEqualTo(AlarmDay.On(LocalDate.parse("2026-10-10")))
    }
}

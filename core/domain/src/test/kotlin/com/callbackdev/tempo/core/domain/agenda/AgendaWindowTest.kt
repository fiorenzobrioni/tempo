package com.callbackdev.tempo.core.domain.agenda

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AgendaWindowTest {
    @Test
    fun `a day wider on each side than the horizon`() {
        val window = AgendaWindow.of(at("2026-10-07T15:00"), Rome, days = 7)
        assertThat(window.start).isEqualTo(at("2026-10-06T00:00"))
        assertThat(window.end).isEqualTo(at("2026-10-15T00:00"))
    }

    @Test
    fun `the widening catches an all-day event west of Greenwich`() {
        // The 7th's all-day event begins at 20:00 on the 6th in New York: inside the window.
        val window = AgendaWindow.of(at("2026-10-07T08:00", NewYork), NewYork, days = 1)
        val holiday = allDay(1, "2026-10-07")
        assertThat(holiday.begin).isAtLeast(window.start)
        assertThat(holiday.end).isAtMost(window.end)
    }
}

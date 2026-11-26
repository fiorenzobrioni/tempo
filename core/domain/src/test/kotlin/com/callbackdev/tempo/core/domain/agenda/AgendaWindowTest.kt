package com.callbackdev.tempo.core.domain.agenda

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun `a span of dates is widened by a day on each side too`() {
        val window = AgendaWindow.of(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-12-06"), Rome)
        assertThat(window.start).isEqualTo(at("2026-10-25T00:00"))
        assertThat(window.end).isEqualTo(at("2026-12-08T00:00"))
    }

    @Test
    fun `two spans read as one hold both`() {
        // A fortnight from the month's last day runs past the month's last week.
        val agenda = AgendaWindow.of(at("2026-11-30T10:00"), Rome, days = 14)
        val month = AgendaWindow.of(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-12-06"), Rome)
        val both = agenda.covering(month)
        assertThat(both.start).isEqualTo(month.start)
        assertThat(both.end).isEqualTo(agenda.end)
        assertThat(month.covering(agenda)).isEqualTo(both)
    }

    @Test
    fun `a span that holds the other is the one read`() {
        val agenda = AgendaWindow.of(at("2026-11-10T10:00"), Rome, days = 7)
        val month = AgendaWindow.of(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-12-06"), Rome)
        assertThat(agenda.covering(month)).isEqualTo(month)
        assertThat(agenda.covering(agenda)).isEqualTo(agenda)
    }
}

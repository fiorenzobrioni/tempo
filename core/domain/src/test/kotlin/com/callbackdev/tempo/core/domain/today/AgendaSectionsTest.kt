package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class AgendaSectionsTest {
    private val today = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", title = "Today")
    private val tomorrow = timed(2, "2026-10-08T09:00", "2026-10-08T10:00", title = "Tomorrow")
    private val friday = timed(3, "2026-10-09T09:00", "2026-10-09T10:00", title = "Friday")
    private val sunday = timed(4, "2026-10-11T10:00", "2026-10-11T12:00", title = "Sunday")
    private val now = at("2026-10-07T08:00")

    private fun kinds(sections: List<AgendaSection>) = sections.map {
        when (it) {
            is AgendaSection.Today -> "today"
            is AgendaSection.Tomorrow -> "tomorrow"
            is AgendaSection.Later -> "later ${it.day.date.dayOfMonth}"
            is AgendaSection.Nothing -> "nothing ${it.from.dayOfMonth}-${it.to.dayOfMonth}"
        }
    }

    @Test
    fun `today, tomorrow in full, the days after compact, empty days folded`() {
        val sections = AgendaSections.of(agenda(today, tomorrow, friday, sunday, now = now))
        assertThat(kinds(sections))
            .containsExactly("today", "tomorrow", "later 9", "nothing 10-10", "later 11", "nothing 12-13")
            .inOrder()
    }

    @Test
    fun `an empty tomorrow starts the run and says so`() {
        val sections = AgendaSections.of(agenda(today, sunday, now = now))
        assertThat(kinds(sections)).containsExactly("today", "nothing 8-10", "later 11", "nothing 12-13").inOrder()
        val run = sections[1] as AgendaSection.Nothing
        assertThat(run.startsTomorrow).isTrue()
        assertThat(run.isOneDay).isFalse()
        assertThat(run.days).isEqualTo(3)
        assertThat(run.from).isEqualTo(LocalDate.parse("2026-10-08"))
    }

    @Test
    fun `an empty week is one row`() {
        val sections = AgendaSections.of(agenda(now = now))
        assertThat(kinds(sections)).containsExactly("today", "nothing 8-13").inOrder()
    }

    @Test
    fun `an all-day event keeps its day out of the fold`() {
        val sections = AgendaSections.of(agenda(allDay(9, "2026-10-10"), now = now))
        assertThat(kinds(sections)).containsExactly("today", "nothing 8-9", "later 10", "nothing 11-13").inOrder()
        assertThat((sections[1] as AgendaSection.Nothing).startsTomorrow).isTrue()
        assertThat((sections[3] as AgendaSection.Nothing).startsTomorrow).isFalse()
    }

    @Test
    fun `today alone on the horizon is today alone`() {
        assertThat(kinds(AgendaSections.of(agenda(now = now, days = 1)))).containsExactly("today")
    }

    @Test
    fun `today and an empty tomorrow`() {
        val sections = AgendaSections.of(agenda(today, now = now, days = 2))
        assertThat(kinds(sections)).containsExactly("today", "nothing 8-8").inOrder()
        val run = sections[1] as AgendaSection.Nothing
        assertThat(run.isOneDay).isTrue()
        assertThat(run.days).isEqualTo(1)
    }
}

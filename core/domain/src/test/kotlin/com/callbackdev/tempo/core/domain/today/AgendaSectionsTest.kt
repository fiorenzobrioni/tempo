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
    fun `two weeks of a sparse calendar cost a card or two`() {
        val nextThursday = timed(5, "2026-10-15T09:00", "2026-10-15T10:00", title = "Next Thursday")
        val sections = AgendaSections.of(agenda(today, tomorrow, nextThursday, now = now, days = 14))
        assertThat(kinds(sections))
            .containsExactly("today", "tomorrow", "nothing 9-14", "later 15", "nothing 16-20")
            .inOrder()
        assertThat((sections[2] as AgendaSection.Nothing).days).isEqualTo(6)
    }

    @Test
    fun `an empty fortnight is one row`() {
        val sections = AgendaSections.of(agenda(now = now, days = 14))
        assertThat(kinds(sections)).containsExactly("today", "nothing 8-20").inOrder()
        val run = sections[1] as AgendaSection.Nothing
        assertThat(run.days).isEqualTo(13)
        assertThat(run.startsTomorrow).isTrue()
    }

    @Test
    fun `two weeks across the clocks going back and into a new month`() {
        // Rome goes from UTC+2 to UTC+1 on Sunday 25 October 2026; the fortnight ends on 2 November.
        val fromTuesday = at("2026-10-20T08:00")
        val sections = AgendaSections.of(
            agenda(
                timed(1, "2026-10-20T10:00", "2026-10-20T11:00"),
                timed(2, "2026-10-25T09:00", "2026-10-25T10:00"),
                timed(3, "2026-11-02T09:00", "2026-11-02T10:00"),
                now = fromTuesday,
                days = 14,
            ),
        )
        assertThat(kinds(sections))
            .containsExactly("today", "nothing 21-24", "later 25", "nothing 26-1", "later 2")
            .inOrder()
        val run = sections[3] as AgendaSection.Nothing
        assertThat(run.from).isEqualTo(LocalDate.parse("2026-10-26"))
        assertThat(run.to).isEqualTo(LocalDate.parse("2026-11-01"))
        assertThat(run.days).isEqualTo(7)
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

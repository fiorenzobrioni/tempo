package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaFilter
import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.calendar
import com.callbackdev.tempo.core.domain.agenda.timed
import com.callbackdev.tempo.core.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class NewEventsTest {
    private val now = at("2026-10-07T10:20")
    private val review = timed(1, "2026-10-07T10:00", "2026-10-07T11:00")
    private val dentist = timed(2, "2026-10-07T15:00", "2026-10-07T16:00")
    private val call = timed(3, "2026-10-08T09:00", "2026-10-08T09:30")
    private val before = agenda(review, dentist, call, now = now)
    private val known = NewEvents.keys(before)

    @Test
    fun `the page as it was holds nothing new`() {
        assertThat(NewEvents.first(before, known)).isNull()
    }

    @Test
    fun `the event made is found, wherever it falls on the page`() {
        val lunch = timed(4, "2026-10-09T13:00", "2026-10-09T14:00")
        assertThat(NewEvents.first(agenda(review, dentist, call, lunch, now = now), known))
            .isEqualTo(EventKey.of(lunch))
    }

    @Test
    fun `an event moved to another start is new there`() {
        val moved = call.copy(begin = at("2026-10-08T11:00"), end = at("2026-10-08T11:30"))
        assertThat(NewEvents.first(agenda(review, dentist, moved, now = now), known)).isEqualTo(EventKey.of(moved))
    }

    @Test
    fun `an event gone and nothing added is nothing new`() {
        assertThat(NewEvents.first(agenda(review, call, now = now), known)).isNull()
    }

    @Test
    fun `a new series is found at its first occurrence`() {
        val first = timed(5, "2026-10-07T18:00", "2026-10-07T19:00")
        val second = first.copy(begin = at("2026-10-08T18:00"), end = at("2026-10-08T19:00"))
        val third = first.copy(begin = at("2026-10-09T18:00"), end = at("2026-10-09T19:00"))
        val after = agenda(review, dentist, call, third, second, first, now = now)
        assertThat(NewEvents.first(after, known)).isEqualTo(EventKey.of(first))
    }

    @Test
    fun `a date's all-day event comes before its timed ones`() {
        val late = timed(6, "2026-10-08T08:00", "2026-10-08T08:30")
        val holiday = allDay(7, "2026-10-08")
        assertThat(NewEvents.first(agenda(review, dentist, call, late, holiday, now = now), known))
            .isEqualTo(EventKey.of(holiday))
    }

    @Test
    fun `an event already over today is found too`() {
        val breakfast = timed(8, "2026-10-07T08:00", "2026-10-07T09:00")
        assertThat(NewEvents.first(agenda(review, dentist, call, breakfast, now = now), known))
            .isEqualTo(EventKey.of(breakfast))
    }

    @Test
    fun `an event past the horizon or in a hidden calendar is not on the page`() {
        val later = timed(9, "2026-10-20T09:00", "2026-10-20T10:00")
        val hidden = timed(10, "2026-10-08T15:00", "2026-10-08T16:00", calendarId = 2)
        val after = agenda(
            review,
            dentist,
            call,
            later,
            hidden,
            now = now,
            filter = AgendaFilter(hiddenCalendars = setOf(2)),
        )
        assertThat(NewEvents.first(after, known)).isNull()
    }

    @Test
    fun `over a run of dates, the event made comes with its first date`() {
        fun days(vararg events: EventInstance) = AgendaBuilder.days(
            instances = events.toList(),
            calendars = listOf(calendar(1)),
            filter = AgendaFilter(),
            now = now,
            zone = Rome,
            from = LocalDate.parse("2026-10-01"),
            to = LocalDate.parse("2026-10-31"),
        )
        val held = NewEvents.keys(days(review, dentist, call))
        assertThat(NewEvents.first(days(review, dentist, call), held)).isNull()
        val trip = allDay(11, "2026-10-20", dates = 3)
        assertThat(NewEvents.first(days(review, dentist, call, trip), held))
            .isEqualTo(NewEvent(EventKey.of(trip), LocalDate.parse("2026-10-20")))
    }
}

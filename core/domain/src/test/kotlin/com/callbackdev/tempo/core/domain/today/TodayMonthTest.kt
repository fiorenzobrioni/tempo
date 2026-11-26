package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.AgendaFilter
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.agenda.NewYork
import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.calendar
import com.callbackdev.tempo.core.domain.agenda.timed
import com.callbackdev.tempo.core.domain.month.MonthGrid
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The current month beside Today's clock (PLANNING.md §11, step 10.4): what is read for it, and its marks. */
class TodayMonthTest {
    private val november = YearMonth.of(2026, 11)
    private val green = 0xFF0B8043.toInt()
    private val calendars = listOf(calendar(1), calendar(2).copy(color = green))

    private fun month(
        vararg events: EventInstance,
        now: String = "2026-11-26T10:00",
        zone: ZoneId = Rome,
        filter: AgendaFilter = AgendaFilter(),
    ) = TodayMonth.of(november, events.toList(), calendars, filter, at(now, zone), zone)

    private fun TodayMonth.on(date: String) = marks.getValue(LocalDate.parse(date))

    @Test
    fun `the one read holds the agenda's dates and the month's, every day of a year at every horizon`() {
        for (zone in listOf(Rome, NewYork)) {
            for (days in UserSettings.HORIZON_CHOICES) {
                var date = LocalDate.parse("2026-01-01")
                repeat(365) {
                    val now = date.atTime(10, 0).atZone(zone).toInstant()
                    val agenda = AgendaWindow.of(now, zone, days)
                    val month = YearMonth.from(date)
                    val window = TodayMonth.window(agenda, month, zone)
                    assertThat(window.start).isAtMost(agenda.start)
                    assertThat(window.end).isAtLeast(agenda.end)
                    for (first in DayOfWeek.entries) {
                        val grid = MonthGrid.of(month, first)
                        assertThat(window).isEqualTo(window.covering(AgendaWindow.of(grid.first, grid.last, zone)))
                    }
                    date = date.plusDays(1)
                }
            }
        }
    }

    @Test
    fun `the read is no wider than the two ask for`() {
        // On the 30th a fortnight ends past the month's last week: the month's start, the agenda's end.
        val agenda = AgendaWindow.of(at("2026-11-30T10:00"), Rome, days = 14)
        val window = TodayMonth.window(agenda, november, Rome)
        assertThat(window.start).isEqualTo(at("2026-10-25T00:00"))
        assertThat(window.end).isEqualTo(agenda.end)
        // On the 10th a week is inside the month: the month's dates, a day wider on each side.
        val inside = TodayMonth.window(AgendaWindow.of(at("2026-11-10T10:00"), Rome, days = 7), november, Rome)
        assertThat(inside.start).isEqualTo(at("2026-10-25T00:00"))
        assertThat(inside.end).isEqualTo(at("2026-12-08T00:00"))
    }

    @Test
    fun `every date the month's grid can hold has its marks, and no other`() {
        val result = month()
        assertThat(result.month).isEqualTo(november)
        assertThat(result.marks.keys.min()).isEqualTo(LocalDate.parse("2026-10-26"))
        assertThat(result.marks.keys.max()).isEqualTo(LocalDate.parse("2026-12-06"))
        assertThat(result.marks).hasSize(42)
        assertThat(result.marks.values.all { it.isEmpty }).isTrue()
    }

    @Test
    fun `a date says what it holds, before today and after`() {
        val result = month(
            timed(1, "2026-11-03T09:00", "2026-11-03T10:00"),
            timed(2, "2026-11-26T15:00", "2026-11-26T16:00", calendarId = 2),
            timed(3, "2026-11-26T18:00", "2026-11-26T19:00"),
            allDay(4, "2026-11-28", dates = 3),
            // The agenda's fortnight past the month: read with it, and not the month's.
            timed(5, "2026-12-09T09:00", "2026-12-09T10:00"),
        )
        assertThat(result.on("2026-11-03").dots).hasSize(1)
        assertThat(result.on("2026-11-26").dots).containsExactly(calendars[1].color, calendars[0].color).inOrder()
        assertThat(result.on("2026-11-26").events).isEqualTo(2)
        assertThat(result.on("2026-11-28").lanes.first()?.startsHere).isTrue()
        assertThat(result.on("2026-11-30").lanes.first()?.endsHere).isTrue()
        assertThat(result.on("2026-11-27").isEmpty).isTrue()
        assertThat(result.marks.keys).doesNotContain(LocalDate.parse("2026-12-09"))
    }

    @Test
    fun `the reader's choices hold here as on the agenda`() {
        val result = month(
            timed(1, "2026-11-12T09:00", "2026-11-12T10:00", calendarId = 2),
            allDay(2, "2026-11-13"),
            filter = AgendaFilter(hiddenCalendars = setOf(2), showAllDay = false),
        )
        assertThat(result.on("2026-11-12").isEmpty).isTrue()
        assertThat(result.on("2026-11-13").isEmpty).isTrue()
    }

    @Test
    fun `an all-day event keeps its date west of Greenwich`() {
        // §4.2: read in UTC. In New York's zone the 12th's holiday begins on the evening of the 11th.
        val result = month(allDay(1, "2026-11-12"), now = "2026-11-10T10:00", zone = NewYork)
        assertThat(result.on("2026-11-11").isEmpty).isTrue()
        assertThat(result.on("2026-11-12").lanes.first()).isNotNull()
        assertThat(result.on("2026-11-13").isEmpty).isTrue()
    }
}

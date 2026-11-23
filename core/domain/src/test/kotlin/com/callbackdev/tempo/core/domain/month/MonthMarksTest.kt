package com.callbackdev.tempo.core.domain.month

import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaFilter
import com.callbackdev.tempo.core.domain.agenda.NewYork
import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.calendar
import com.callbackdev.tempo.core.domain.agenda.timed
import com.callbackdev.tempo.core.model.EventInstance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonthMarksTest {
    private val blue = 0xFF3F51B5.toInt()
    private val green = 0xFF0B8043.toInt()
    private val calendars = listOf(calendar(1), calendar(2).copy(color = green))

    private fun marks(
        vararg events: EventInstance,
        from: String = "2026-11-01",
        to: String = "2026-11-30",
        zone: ZoneId = Rome,
        filter: AgendaFilter = AgendaFilter(),
    ): Map<LocalDate, DayMarks> {
        val days = AgendaBuilder.days(
            instances = events.toList(),
            calendars = calendars,
            filter = filter,
            now = at("2026-11-10T10:00", zone),
            zone = zone,
            from = LocalDate.parse(from),
            to = LocalDate.parse(to),
        )
        return MonthMarks.of(days, calendars.associateBy { it.id }, zone)
    }

    private fun Map<LocalDate, DayMarks>.on(date: String) = getValue(LocalDate.parse(date))

    @Test
    fun `a date with nothing on has no mark`() {
        val day = marks().on("2026-11-12")
        assertThat(day.isEmpty).isTrue()
        assertThat(day.dots).isEmpty()
        assertThat(day.lanes).containsExactly(null, null)
        assertThat(day.more).isFalse()
    }

    @Test
    fun `an event with a time is a dot in its calendar's colour`() {
        val day = marks(
            timed(1, "2026-11-12T09:00", "2026-11-12T10:00"),
            timed(2, "2026-11-12T15:00", "2026-11-12T16:00", calendarId = 2),
        ).on("2026-11-12")
        assertThat(day.dots).containsExactly(blue, green).inOrder()
        assertThat(day.events).isEqualTo(2)
        assertThat(day.lanes).containsExactly(null, null)
    }

    @Test
    fun `an event's own colour wins over its calendar's`() {
        val red = 0xFFD50000.toInt()
        val day = marks(timed(1, "2026-11-12T09:00", "2026-11-12T10:00").copy(color = red)).on("2026-11-12")
        assertThat(day.dots).containsExactly(red)
    }

    @Test
    fun `past three, the dots say there are more`() {
        val four = (1L..4L).map { timed(it, "2026-11-12T${9 + it}:00", "2026-11-12T${9 + it}:30") }
        val three = marks(*four.take(3).toTypedArray()).on("2026-11-12")
        assertThat(three.dots).hasSize(3)
        assertThat(three.more).isFalse()
        val day = marks(*four.toTypedArray()).on("2026-11-12")
        assertThat(day.dots).hasSize(3)
        assertThat(day.more).isTrue()
        assertThat(day.events).isEqualTo(4)
    }

    @Test
    fun `an all-day event is a bar that starts and ends on its date`() {
        val day = marks(allDay(1, "2026-11-12")).on("2026-11-12")
        assertThat(day.lanes.first()).isEqualTo(DayBar(blue, startsHere = true, endsHere = true))
        assertThat(day.dots).isEmpty()
        assertThat(day.events).isEqualTo(1)
    }

    @Test
    fun `a holiday of several days is one bar across them`() {
        val all = marks(allDay(1, "2026-11-10", dates = 4))
        assertThat(all.on("2026-11-09").lanes.first()).isNull()
        assertThat(all.on("2026-11-10").lanes.first()).isEqualTo(DayBar(blue, startsHere = true, endsHere = false))
        assertThat(all.on("2026-11-11").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = false))
        assertThat(all.on("2026-11-13").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = true))
        assertThat(all.on("2026-11-14").lanes.first()).isNull()
    }

    @Test
    fun `an all-day event keeps its dates west of Greenwich`() {
        // Read in New York's zone it would start the evening of the 11th (PLANNING.md §4.2).
        val all = marks(allDay(1, "2026-11-12"), zone = NewYork)
        assertThat(all.on("2026-11-11").isEmpty).isTrue()
        assertThat(all.on("2026-11-12").lanes.first()).isEqualTo(DayBar(blue, startsHere = true, endsHere = true))
        assertThat(all.on("2026-11-13").isEmpty).isTrue()
    }

    @Test
    fun `a span that began before the dates shown comes in square`() {
        val all = marks(allDay(1, "2026-10-30", dates = 4))
        assertThat(all.on("2026-11-01").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = false))
        assertThat(all.on("2026-11-02").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = true))
    }

    @Test
    fun `a conference with times is one bar from its first date to its last`() {
        // Tuesday is whole inside it: the agenda's all-day row has it there, with times at its ends.
        val all = marks(timed(1, "2026-11-09T09:00", "2026-11-11T17:00"))
        assertThat(all.on("2026-11-09").lanes.first()).isEqualTo(DayBar(blue, startsHere = true, endsHere = false))
        assertThat(all.on("2026-11-10").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = false))
        assertThat(all.on("2026-11-11").lanes.first()).isEqualTo(DayBar(blue, startsHere = false, endsHere = true))
        assertThat(all.on("2026-11-09").dots).isEmpty()
        assertThat(all.on("2026-11-11").dots).isEmpty()
        assertThat(all.on("2026-11-09").events).isEqualTo(1)
    }

    @Test
    fun `a conference whose whole date lies past the dates shown is still a bar`() {
        val all = marks(timed(1, "2026-11-30T09:00", "2026-12-02T17:00"))
        assertThat(all.on("2026-11-30").lanes.first()).isEqualTo(DayBar(blue, startsHere = true, endsHere = false))
        assertThat(all.on("2026-11-30").dots).isEmpty()
    }

    @Test
    fun `a night that crosses midnight is a dot on each date`() {
        val all = marks(timed(1, "2026-11-12T22:00", "2026-11-13T02:00"))
        assertThat(all.on("2026-11-12").dots).containsExactly(blue)
        assertThat(all.on("2026-11-13").dots).containsExactly(blue)
        assertThat(all.on("2026-11-12").lanes).containsExactly(null, null)
    }

    @Test
    fun `a moment is a dot`() {
        val all = marks(timed(1, "2026-11-12T00:00", "2026-11-12T00:00"))
        assertThat(all.on("2026-11-12").dots).containsExactly(blue)
        assertThat(all.on("2026-11-11").isEmpty).isTrue()
    }

    @Test
    fun `two spans at once keep a lane each, on every date`() {
        // A trip, and a holiday that starts inside it and outlasts it.
        val all = marks(
            allDay(1, "2026-11-09", dates = 4),
            allDay(2, "2026-11-11", dates = 4, calendarId = 2),
        )
        assertThat(all.on("2026-11-09").lanes).containsExactly(DayBar(blue, true, false), null).inOrder()
        assertThat(all.on("2026-11-11").lanes)
            .containsExactly(DayBar(blue, false, false), DayBar(green, true, false)).inOrder()
        assertThat(all.on("2026-11-12").lanes)
            .containsExactly(DayBar(blue, false, true), DayBar(green, false, false)).inOrder()
        // The holiday stays on its lane once the trip is over: one line, not a step.
        assertThat(all.on("2026-11-13").lanes).containsExactly(null, DayBar(green, false, false)).inOrder()
        assertThat(all.on("2026-11-14").lanes).containsExactly(null, DayBar(green, false, true)).inOrder()
    }

    @Test
    fun `a lane a span left is taken by the next one`() {
        val all = marks(allDay(1, "2026-11-09", dates = 2), allDay(2, "2026-11-11", dates = 2, calendarId = 2))
        assertThat(all.on("2026-11-10").lanes).containsExactly(DayBar(blue, false, true), null).inOrder()
        assertThat(all.on("2026-11-11").lanes).containsExactly(DayBar(green, true, false), null).inOrder()
    }

    @Test
    fun `of two that start together, the longer takes the first lane`() {
        val all = marks(allDay(1, "2026-11-12"), allDay(2, "2026-11-12", dates = 3, calendarId = 2))
        assertThat(all.on("2026-11-12").lanes)
            .containsExactly(DayBar(green, true, false), DayBar(blue, true, true)).inOrder()
    }

    @Test
    fun `a third span has no lane and is said by more`() {
        val all = marks(
            allDay(1, "2026-11-12", dates = 3),
            allDay(2, "2026-11-12", dates = 2),
            allDay(3, "2026-11-12"),
        )
        val day = all.on("2026-11-12")
        assertThat(day.lanes.filterNotNull()).hasSize(2)
        assertThat(day.more).isTrue()
        assertThat(day.events).isEqualTo(3)
        assertThat(all.on("2026-11-13").more).isFalse()
    }

    @Test
    fun `what the reader hides leaves no mark`() {
        val all = marks(
            allDay(1, "2026-11-12"),
            timed(2, "2026-11-12T09:00", "2026-11-12T10:00", calendarId = 2),
            filter = AgendaFilter(hiddenCalendars = setOf(2), showAllDay = false),
        )
        assertThat(all.on("2026-11-12").isEmpty).isTrue()
    }
}

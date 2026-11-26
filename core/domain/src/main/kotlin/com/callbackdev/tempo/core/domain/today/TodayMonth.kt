package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.agenda.AgendaFilter
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.domain.month.DayMarks
import com.callbackdev.tempo.core.domain.month.MonthGrid
import com.callbackdev.tempo.core.domain.month.MonthMarks
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * The current month beside Today's clock, on a large screen (PLANNING.md §11, steps 10.4 and
 * 10.5): the month at a glance where the page has a pane to spare, and a way in to the month's own
 * page. Marks, never titles, as there.
 *
 * @property month the month the marks are of: the one of the day they were read on.
 * @property marks what each date [month]'s grid can hold says of itself, whatever day the week
 *   starts on ([MonthGrid.span]).
 */
data class TodayMonth(val month: YearMonth, val marks: Map<LocalDate, DayMarks>) {
    companion object {
        /**
         * The span to ask the provider for where Today shows [month] too: the [agenda]'s and the
         * month's as one, so the calendar is read once for the two. Today is a date of its month,
         * so the two always meet; near the month's end the agenda runs past it.
         */
        fun window(agenda: AgendaWindow, month: YearMonth, zone: ZoneId): AgendaWindow {
            val dates = MonthGrid.span(month)
            return agenda.covering(AgendaWindow.of(dates.start, dates.endInclusive, zone))
        }

        /**
         * [month]'s marks from a read that holds its dates ([window]): each date built by the
         * agenda's own builder, with the reader's choices, as the month's page builds it.
         */
        fun of(
            month: YearMonth,
            instances: List<EventInstance>,
            calendars: List<CalendarInfo>,
            filter: AgendaFilter,
            now: Instant,
            zone: ZoneId,
        ): TodayMonth {
            val dates = MonthGrid.span(month)
            val days = AgendaBuilder.days(instances, calendars, filter, now, zone, dates.start, dates.endInclusive)
            return TodayMonth(month, MonthMarks.of(days, calendars.associateBy { it.id }, zone))
        }
    }
}

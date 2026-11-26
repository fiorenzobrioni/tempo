package com.callbackdev.tempo.core.domain.agenda

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The span to ask the provider for (PLANNING.md §4.1): from the start of today to the end of the
 * horizon's last date in the phone's zone, widened by a day on each side, so an all-day event,
 * stored in UTC (§4.2), is never cut off by the window itself. [AgendaBuilder] trims to the dates.
 */
data class AgendaWindow(val start: Instant, val end: Instant) {
    /**
     * The one span that holds this one and [other]: a page that shows two views of the calendar
     * (Today's agenda and, on a large screen, the month beside it) asks the provider once.
     * [AgendaBuilder] trims each view to its own dates.
     */
    fun covering(other: AgendaWindow): AgendaWindow = AgendaWindow(minOf(start, other.start), maxOf(end, other.end))

    companion object {
        fun of(now: Instant, zone: ZoneId, days: Int): AgendaWindow {
            val today = LocalDate.ofInstant(now, zone)
            return AgendaWindow(
                start = today.minusDays(1).atStartOfDay(zone).toInstant(),
                end = today.plusDays(days.toLong() + 1).atStartOfDay(zone).toInstant(),
            )
        }

        /** The span for the dates from [from] to [to], both included (the month), widened the same way. */
        fun of(from: LocalDate, to: LocalDate, zone: ZoneId): AgendaWindow = AgendaWindow(
            start = from.minusDays(1).atStartOfDay(zone).toInstant(),
            end = to.plusDays(2).atStartOfDay(zone).toInstant(),
        )
    }
}

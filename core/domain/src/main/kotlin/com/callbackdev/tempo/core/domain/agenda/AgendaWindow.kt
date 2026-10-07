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
    companion object {
        fun of(now: Instant, zone: ZoneId, days: Int): AgendaWindow {
            val today = LocalDate.ofInstant(now, zone)
            return AgendaWindow(
                start = today.minusDays(1).atStartOfDay(zone).toInstant(),
                end = today.plusDays(days.toLong() + 1).atStartOfDay(zone).toInstant(),
            )
        }
    }
}

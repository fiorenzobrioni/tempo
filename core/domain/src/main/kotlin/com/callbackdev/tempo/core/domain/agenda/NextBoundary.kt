package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.Agenda
import java.time.Instant
import java.time.ZoneId

/**
 * The next moment the agenda changes by itself, with nothing changing in the calendar
 * (PLANNING.md §6): the nearest start or end of a timed event after now, or the next midnight,
 * whichever comes first. The widgets arm their one inexact, non-wakeup alarm on it (§7).
 *
 * All-day events need no boundary of their own: their dates turn over at the phone's midnight,
 * which is always a candidate.
 */
object NextBoundary {
    fun after(agenda: Agenda, zone: ZoneId): Instant {
        val midnight = agenda.today.plusDays(1).atStartOfDay(zone).toInstant()
        val events = agenda.days.flatMap { day ->
            day.timed.map { it.event } + day.allDay.map { it.event }.filterNot { it.allDay }
        } + agenda.underWay
        return events
            .flatMap { listOf(it.begin, it.end) }
            .filter { it > agenda.now }
            .fold(midnight) { soonest, moment -> minOf(soonest, moment) }
    }
}

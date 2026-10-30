package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.domain.widget.DialArcs
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
 *
 * With [dial], for an «In words» card on a home screen, the moment an event comes onto the dial's
 * face is one too ([DialArcs.comesOnTheFace]): the face shows the next hours from the moment it is
 * drawn, and an event that reaches them between two boundaries would wait for the next one, at
 * midnight in the evening, while the card's words already name it (review of 30 Oct 2026).
 */
object NextBoundary {
    fun after(agenda: Agenda, zone: ZoneId, dial: Boolean = false): Instant {
        val midnight = agenda.today.plusDays(1).atStartOfDay(zone).toInstant()
        val events = agenda.days.flatMap { day ->
            day.timed.map { it.event } + day.allDay.map { it.event }.filterNot { it.allDay }
        } + agenda.underWay
        val onTheFace = if (dial) {
            agenda.days.flatMap { day -> day.timed.map { DialArcs.comesOnTheFace(it.event.begin, zone) } }
        } else {
            emptyList()
        }
        return (events.flatMap { listOf(it.begin, it.end) } + onTheFace)
            .filter { it > agenda.now }
            .fold(midnight) { soonest, moment -> minOf(soonest, moment) }
    }
}

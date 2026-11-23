package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant
import java.time.LocalDate

/**
 * An event on the page, by its id and its start: the occurrence of a series the reader moved is
 * another, as it stands elsewhere on the page.
 */
data class EventKey(val eventId: Long, val begin: Instant) {
    companion object {
        fun of(event: EventInstance) = EventKey(event.eventId, event.begin)
    }
}

/**
 * The event the reader just made in the calendar app, told from the ones the page held when they
 * left for it (step 8.6): compared in memory, by [EventKey], and nothing stored.
 */
object NewEvents {
    /** Every event of [agenda], all-day and timed. */
    fun keys(agenda: Agenda): Set<EventKey> = keys(agenda.days)

    /** Every event of [days], for a page that is not the agenda (the month). */
    fun keys(days: List<AgendaDay>): Set<EventKey> = events(days).mapTo(mutableSetOf(), EventKey::of)

    /**
     * The first event of [agenda] that [known] did not hold, in the page's order (a date's all-day
     * events, then its timed ones, date after date): a new series is brought into view at its first
     * occurrence. Null when there is none, as when the reader left the new event's page unsaved or
     * made it past the horizon, or in a calendar the page does not show.
     */
    fun first(agenda: Agenda, known: Set<EventKey>): EventKey? = first(agenda.days, known)?.key

    /** As [first], over [days], with the date the event is first on: the month brings that date forward. */
    fun first(days: List<AgendaDay>, known: Set<EventKey>): NewEvent? = days.asSequence()
        .flatMap { day -> events(day).map { NewEvent(EventKey.of(it), day.date) } }
        .firstOrNull { it.key !in known }

    private fun events(days: List<AgendaDay>): Sequence<EventInstance> = days.asSequence().flatMap(::events)

    private fun events(day: AgendaDay): Sequence<EventInstance> =
        day.allDay.asSequence().map { it.event } + day.timed.asSequence().map { it.event }
}

/** An event the reader just made, and the first date it is on. */
data class NewEvent(val key: EventKey, val date: LocalDate)

package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant

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
    fun keys(agenda: Agenda): Set<EventKey> = events(agenda).mapTo(mutableSetOf(), EventKey::of)

    /**
     * The first event of [agenda] that [known] did not hold, in the page's order (a date's all-day
     * events, then its timed ones, date after date): a new series is brought into view at its first
     * occurrence. Null when there is none, as when the reader left the new event's page unsaved or
     * made it past the horizon, or in a calendar the page does not show.
     */
    fun first(agenda: Agenda, known: Set<EventKey>): EventKey? =
        events(agenda).map(EventKey::of).firstOrNull { it !in known }

    private fun events(agenda: Agenda): Sequence<EventInstance> = agenda.days.asSequence().flatMap { day ->
        day.allDay.asSequence().map { it.event } + day.timed.asSequence().map { it.event }
    }
}

package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.EventStatus

/**
 * The reader's choices that decide what the agenda shows (PLANNING.md §4.4, §5). Applied here, in
 * pure Kotlin, rather than in the provider's SQL: the query is the same for every reader, and
 * every rule has a test.
 *
 * @property shownCalendars calendars the reader turned on in Tempo, whatever the calendar app says.
 * @property hiddenCalendars calendars the reader turned off in Tempo. Wins over [shownCalendars].
 * @property showDeclined whether invitations the reader declined are shown.
 * @property showAllDay whether the calendars' all-day events are shown. A timed event that happens
 *   to cover a whole date (a three-day conference) is not one of them: it is always shown.
 */
data class AgendaFilter(
    val shownCalendars: Set<Long> = emptySet(),
    val hiddenCalendars: Set<Long> = emptySet(),
    val showDeclined: Boolean = false,
    val showAllDay: Boolean = true,
) {
    /**
     * A calendar is shown if the reader said so, or, when they said nothing, if the calendar app
     * shows it. An instance whose calendar is not in the list (a sync landing between the two
     * queries) is shown: hiding an event is worse than showing one a moment early.
     */
    fun shows(calendar: CalendarInfo?, calendarId: Long): Boolean = when (calendarId) {
        in hiddenCalendars -> false
        in shownCalendars -> true
        else -> calendar?.visibleInProvider ?: true
    }

    fun shows(event: EventInstance, calendars: Map<Long, CalendarInfo>): Boolean = when {
        event.status == EventStatus.CANCELED -> false
        event.selfStatus == AttendeeStatus.DECLINED && !showDeclined -> false
        event.allDay && !showAllDay -> false
        else -> shows(calendars[event.calendarId], event.calendarId)
    }
}

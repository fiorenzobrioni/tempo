package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.domain.agenda.AgendaFilter
import com.callbackdev.tempo.core.model.CalendarChoice
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.UserSettings

/**
 * The reader's choices of calendars, matched to the calendars the phone has now (PLANNING.md §5).
 *
 * A choice finds its calendar by id within the same account first (the same phone: a calendar
 * renamed in its app is still the same calendar), then by account and name (a restored phone,
 * where the ids are new). A choice whose calendar is gone is kept: the account may come back.
 */
object CalendarChoices {
    fun choiceFor(calendar: CalendarInfo, choices: List<CalendarChoice>): CalendarChoice? =
        choices.firstOrNull { it.calendarId == calendar.id && it.sameAccount(calendar) }
            ?: choices.firstOrNull { it.sameAccount(calendar) && it.name == calendar.name }

    /** Shown by the reader's word, or by the calendar app's when the reader said nothing. */
    fun isShown(calendar: CalendarInfo, choices: List<CalendarChoice>): Boolean =
        choiceFor(calendar, choices)?.shown ?: calendar.visibleInProvider

    /** [settings] with [calendar] shown or hidden: its old choice replaced, never doubled. */
    fun choose(settings: UserSettings, calendar: CalendarInfo, shown: Boolean): UserSettings {
        val old = choiceFor(calendar, settings.calendarChoices)
        val new = CalendarChoice(calendar.id, calendar.accountType, calendar.accountName, calendar.name, shown)
        return settings.copy(calendarChoices = settings.calendarChoices.filterNot { it == old } + new)
    }

    /** What the agenda shows, from the settings and the calendars the phone has now. */
    fun agendaFilter(settings: UserSettings, calendars: List<CalendarInfo>): AgendaFilter {
        val decided = calendars.mapNotNull { calendar ->
            choiceFor(calendar, settings.calendarChoices)?.let { calendar.id to it.shown }
        }
        return AgendaFilter(
            shownCalendars = decided.filter { it.second }.map { it.first }.toSet(),
            hiddenCalendars = decided.filterNot { it.second }.map { it.first }.toSet(),
            showDeclined = settings.showDeclined,
            showAllDay = settings.showAllDay,
        )
    }

    private fun CalendarChoice.sameAccount(calendar: CalendarInfo) =
        accountType == calendar.accountType && accountName == calendar.accountName
}

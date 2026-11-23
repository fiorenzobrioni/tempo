package com.callbackdev.tempo.feature.month

import com.callbackdev.tempo.core.calendar.CalendarDoors
import com.callbackdev.tempo.core.domain.month.DayMarks
import com.callbackdev.tempo.core.domain.today.NewEvent
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.UserSettings
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The month, as the screen draws it: a moment, the reader's settings and what the calendar says of
 * the dates read. Immutable, built by [MonthViewModel]; the screen is a plain function of it, so
 * its tests draw it without a provider. Which month is on screen and which day is chosen are the
 * screen's own: they are where the reader's finger is, not what the calendar says.
 *
 * @property reveal the event the reader just made in the calendar app, with its date, to bring
 *   forward and glow once; null otherwise, and once the page has done it.
 */
data class MonthUiState(
    val now: ZonedDateTime,
    val settings: UserSettings,
    val content: MonthContent,
    val doors: CalendarDoors,
    val reveal: NewEvent? = null,
) {
    val today: LocalDate get() = now.toLocalDate()
}

/** What the calendar gives the month: the grid is drawn in every case, with marks or without. */
sealed interface MonthContent {
    /** Before the first read: the grid alone rather than a flash of an empty month. */
    data object Loading : MonthContent

    data object NoPermission : MonthContent

    /** The provider has no calendar at all. */
    data object NoCalendars : MonthContent

    /** There are calendars, and the reader (or the calendar app) hid every one. */
    data object AllHidden : MonthContent

    /**
     * @property days the dates read, each as the agenda builds a day: the month on screen and the
     *   two beside it. A date outside them has no entry until its month is turned to.
     * @property marks what each of those dates says of itself in its cell.
     */
    data class Ready(
        val days: Map<LocalDate, AgendaDay>,
        val marks: Map<LocalDate, DayMarks>,
        val calendars: Map<Long, CalendarInfo>,
    ) : MonthContent
}

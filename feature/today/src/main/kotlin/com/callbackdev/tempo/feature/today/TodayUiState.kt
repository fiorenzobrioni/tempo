package com.callbackdev.tempo.feature.today

import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.domain.today.TodaySentence
import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.UserSettings
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Today, as the screen draws it: a moment, the reader's settings, the next alarm and what the
 * calendar says. Immutable, built by [TodayViewModel] from the clock, the settings and the
 * provider; the screen is a plain function of it, so its tests draw it without either.
 *
 * @property alarm the phone's next alarm, or null when none is set or the reader hid the line.
 * @property reveal the event the reader just made in the calendar app, to bring into view and glow
 *   once (step 8.6); null otherwise, and once the page has done it.
 */
data class TodayUiState(
    val now: ZonedDateTime,
    val settings: UserSettings,
    val alarm: Instant?,
    val content: TodayContent,
    val doors: CalendarDoors,
    val reveal: EventKey? = null,
) {
    /**
     * The first day's tip, until it is seen: only over an agenda, never beside the permission's
     * card or another state's, which have their own word to say first.
     */
    val showsTip: Boolean get() = !settings.todayTipSeen && content is TodayContent.Ready
}

/** What the agenda's place holds. */
sealed interface TodayContent {
    /** Before the first read: nothing is drawn rather than a flash of "nothing today". */
    data object Loading : TodayContent

    data object NoPermission : TodayContent

    /** The provider has no calendar at all. */
    data object NoCalendars : TodayContent

    /** There are calendars, and the reader (or the calendar app) hid every one. */
    data object AllHidden : TodayContent

    data class Ready(val agenda: Agenda, val sentence: TodaySentence, val calendars: Map<Long, CalendarInfo>) :
        TodayContent
}

/**
 * Which doors are there (PLANNING.md §4.7): the calendar app's, and the clock app's for the alarm
 * line. A button whose intent nothing takes is not drawn (CLAUDE.md, Read-only).
 *
 * @property clockPackage the phone's clock app (`ClockApp`), or null when there is none.
 */
data class CalendarDoors(
    val canCreate: Boolean,
    val canOpenEvent: Boolean,
    val canOpenDay: Boolean,
    val clockPackage: String?,
) {
    companion object {
        val None = CalendarDoors(canCreate = false, canOpenEvent = false, canOpenDay = false, clockPackage = null)
    }
}

package com.callbackdev.tempo.core.model

/**
 * Everything the reader chose, read together (PLANNING.md §5). Stored in DataStore, only where it
 * differs from these defaults, and carried by Android's backup (the one file of the allowlist).
 *
 * @property horizonDays how many dates the agenda shows, today included: one of [HORIZON_CHOICES],
 *   a week by default (owner, PLANNING.md §15).
 * @property calendarChoices the calendars the reader turned on or off in Tempo; a calendar with no
 *   choice follows the calendar app ([CalendarInfo.visibleInProvider]).
 * @property showCalendarButton whether Today draws the button that opens the reader's calendar
 *   app at today, beside the settings' gear (owner, PLANNING.md §15, 9 Oct 2026).
 * @property askedCalendarPermission whether Tempo ever asked for the calendar: what tells a
 *   refusal Android lets the app repeat from one it does not (`calendarPermission`).
 */
data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.VIVID,
    val font: AppFont = AppFont.GOOGLE_SANS,
    val dynamicColor: Boolean = false,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val dateStyle: DateStyle = DateStyle.LONG,
    val horizonDays: Int = DEFAULT_HORIZON_DAYS,
    val calendarChoices: List<CalendarChoice> = emptyList(),
    val showDeclined: Boolean = false,
    val showAllDay: Boolean = true,
    val showNextAlarm: Boolean = true,
    val showCalendarButton: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val askedCalendarPermission: Boolean = false,
) {
    companion object {
        /** Today only, today and tomorrow, a week. */
        val HORIZON_CHOICES = listOf(1, 2, 7)
        const val DEFAULT_HORIZON_DAYS = 7
    }
}

/**
 * The reader's word on one calendar: shown or not, whatever the calendar app says.
 *
 * Kept by its provider id **and** by who it is (the account and the calendar's name): a phone
 * restored from a backup, or a calendar account added again, gives the same calendars new ids,
 * and the choice must still find its calendar (PLANNING.md §5).
 */
data class CalendarChoice(
    val calendarId: Long,
    val accountType: String,
    val accountName: String,
    val name: String,
    val shown: Boolean,
)

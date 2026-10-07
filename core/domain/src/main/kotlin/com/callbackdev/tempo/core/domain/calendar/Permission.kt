package com.callbackdev.tempo.core.domain.calendar

import com.callbackdev.tempo.core.model.CalendarPermission

/**
 * Where Tempo stands with the calendar permission, from what Android tells an activity
 * (PLANNING.md §11 Phase 1).
 *
 * Android answers "should I explain before asking?" ([showRationale]) with false both before the
 * first question and after a refusal it will not let the app repeat; only remembering whether the
 * question was ever asked ([askedBefore], kept in the settings) tells the two apart.
 */
fun calendarPermission(granted: Boolean, askedBefore: Boolean, showRationale: Boolean): CalendarPermission = when {
    granted -> CalendarPermission.GRANTED
    !askedBefore || showRationale -> CalendarPermission.ASKABLE
    else -> CalendarPermission.DENIED_FOR_GOOD
}

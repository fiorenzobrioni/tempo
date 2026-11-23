package com.callbackdev.tempo.core.calendar

import android.content.Context
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant

/**
 * Which doors are there (PLANNING.md §4.7): the calendar app's, and the clock app's for Today's
 * time and its alarm line. A button whose intent nothing takes is not drawn (CLAUDE.md,
 * Read-only). Read with each read of the calendar: an app installed or removed meanwhile is seen
 * when the reader is back.
 *
 * @property clockPackage the phone's clock app ([ClockApp]), or null when there is none.
 */
data class CalendarDoors(
    val canCreate: Boolean,
    val canOpenEvent: Boolean,
    val canOpenDay: Boolean,
    val clockPackage: String?,
) {
    companion object {
        val None = CalendarDoors(canCreate = false, canOpenEvent = false, canOpenDay = false, clockPackage = null)

        fun of(context: Context, now: Instant): CalendarDoors = CalendarDoors(
            canCreate = CalendarIntents.canOpen(context, CalendarIntents.insert(now)),
            canOpenEvent = CalendarIntents.canOpen(context, CalendarIntents.view(ProbeEvent)),
            canOpenDay = CalendarIntents.canOpen(context, CalendarIntents.openDay(now)),
            clockPackage = ClockApp.packageName(context),
        )

        /** Any event: only the kind of intent matters to whether an app takes it. */
        private val ProbeEvent = EventInstance(
            eventId = 1,
            calendarId = 1,
            title = null,
            location = null,
            begin = Instant.EPOCH,
            end = Instant.EPOCH,
            allDay = false,
            color = null,
        )
    }
}

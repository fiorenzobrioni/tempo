package com.callbackdev.tempo.widget

import android.content.Context
import android.content.Intent
import com.callbackdev.tempo.core.calendar.CalendarIntents
import com.callbackdev.tempo.core.calendar.ClockApp
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Instant

/**
 * Where each of a card's touches goes, with Tempo as the fallback whenever the chosen door is not
 * there (an uninstalled clock app, no calendar app): a touch never does nothing.
 */
internal object WidgetIntents {
    /** Tempo's own front door: the launcher's intent brings back the app's task, never a second one. */
    fun tempo(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)

    /** The next alarm's door: the clock app, where the alarm is set, moved or turned off. */
    fun alarm(context: Context, doors: WidgetDoors): Intent? =
        doors.clockPackage?.let { ClockApp.open(context, it) } ?: tempo(context)

    fun header(context: Context, tap: HeaderTap, doors: WidgetDoors, now: Instant): Intent? = when (tap) {
        HeaderTap.TEMPO -> null
        HeaderTap.CALENDAR -> CalendarIntents.openDay(now).takeIf { doors.canOpenDay }?.newTask()
        HeaderTap.CLOCK -> doors.clockPackage?.let { ClockApp.open(context, it) }
    } ?: tempo(context)

    /**
     * One occurrence in the calendar app. Its identifier tells two occurrences of one event apart
     * (a daily stand-up today and tomorrow): they share the event's URI, and a pending intent
     * matched on it alone would carry the last one's times for both.
     */
    fun event(context: Context, tap: EventTap, doors: WidgetDoors, event: EventInstance): Intent? =
        if (tap == EventTap.CALENDAR && doors.canOpenEvent) {
            CalendarIntents.view(
                event,
            ).newTask().setIdentifier("tempo-event-${event.eventId}-${event.begin.toEpochMilli()}")
        } else {
            tempo(context)
        }

    private fun Intent.newTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

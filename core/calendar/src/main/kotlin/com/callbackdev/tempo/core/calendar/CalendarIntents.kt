package com.callbackdev.tempo.core.calendar

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Duration
import java.time.Instant

/**
 * The doors to the reader's calendar app (PLANNING.md §4.7). Tempo never writes the calendar:
 * seeing an event in full, creating one and editing one happen there, through Android's own
 * documented intents, and Tempo reads the result back when the provider changes.
 *
 * `ACTION_EDIT` is deliberately absent: calendar apps differ in what they do with it, and
 * `ACTION_VIEW` on the occurrence works everywhere, with editing one tap away. It joins only if
 * the owner's calendar apps are measured honouring it (§15).
 */
object CalendarIntents {
    /** One occurrence of an event: the event's page, at this occurrence's own times. */
    fun view(event: EventInstance): Intent = Intent(Intent.ACTION_VIEW)
        .setData(ContentUris.withAppendedId(Events.CONTENT_URI, event.eventId))
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin.toEpochMilli())
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli())

    /** The calendar app's new-event page, starting at [begin] (the next half hour, or a free gap's start). */
    fun insert(begin: Instant, end: Instant = begin.plus(NEW_EVENT_LENGTH)): Intent = Intent(Intent.ACTION_INSERT)
        .setData(Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin.toEpochMilli())
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toEpochMilli())

    /** The calendar app on the day of [moment]. */
    fun openDay(moment: Instant): Intent {
        val uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .also { ContentUris.appendId(it, moment.toEpochMilli()) }
            .build()
        return Intent(Intent.ACTION_VIEW).setData(uri)
    }

    /**
     * Whether an app on the phone takes [intent]: a button whose intent nothing takes is not drawn
     * (CLAUDE.md, Read-only). Seen through the manifest's `<queries>`.
     */
    fun canOpen(context: Context, intent: Intent): Boolean = context.packageManager.resolveActivity(
        intent,
        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
    ) != null

    /** A new event lasts an hour until the reader says otherwise, as calendar apps propose. */
    val NEW_EVENT_LENGTH: Duration = Duration.ofHours(1)
}

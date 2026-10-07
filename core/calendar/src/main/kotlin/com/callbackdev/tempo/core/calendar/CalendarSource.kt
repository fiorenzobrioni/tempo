package com.callbackdev.tempo.core.calendar

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Instances
import com.callbackdev.tempo.core.domain.agenda.AgendaWindow
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What one read of the calendar gives: the calendars and the instances, or why there are none. */
sealed interface CalendarRead {
    data class Read(val calendars: List<CalendarInfo>, val instances: List<EventInstance>) : CalendarRead

    /** `READ_CALENDAR` is not granted, or was revoked while the read ran. */
    data object NoPermission : CalendarRead
}

/**
 * The Calendar Provider, read (PLANNING.md §4.1). Tempo's only way in for events: it asks for the
 * phone's calendars and for the instances of a window, as stored, and leaves every decision
 * (dates, hidden calendars, declined invitations) to `AgendaBuilder` in the domain.
 *
 * Read-only by construction: nothing here, nor anywhere in Tempo, writes the provider.
 */
@Singleton
class CalendarSource @Inject constructor(@ApplicationContext private val context: Context) {
    /** Both queries, on the IO dispatcher; a permission missing or revoked mid-read is a state. */
    suspend fun read(window: AgendaWindow): CalendarRead = withContext(Dispatchers.IO) {
        if (!CalendarAccess.isGranted(context)) return@withContext CalendarRead.NoPermission
        try {
            CalendarRead.Read(calendars(), instances(window))
        } catch (_: SecurityException) {
            CalendarRead.NoPermission
        }
    }

    private fun calendars(): List<CalendarInfo> =
        context.contentResolver.query(Calendars.CONTENT_URI, CalendarProjection, null, null, null)
            ?.use { cursor -> cursor.rows { calendarOf(it) } }
            .orEmpty()

    /**
     * The instances overlapping [window]. The provider expands recurrences and applies exceptions
     * for the window asked; Tempo never reads an `RRULE` (§4.1).
     */
    private fun instances(window: AgendaWindow): List<EventInstance> {
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, window.start.toEpochMilli())
            ContentUris.appendId(it, window.end.toEpochMilli())
        }.build()
        return context.contentResolver.query(uri, InstanceProjection, null, null, "${Instances.BEGIN} ASC")
            ?.use { cursor -> cursor.rows { instanceOf(it) } }
            .orEmpty()
    }

    private fun calendarOf(cursor: Cursor): CalendarInfo? {
        val id = cursor.long(Calendars._ID) ?: return null
        return CalendarInfo(
            id = id,
            name = cursor.string(Calendars.CALENDAR_DISPLAY_NAME) ?: cursor.string(Calendars.ACCOUNT_NAME).orEmpty(),
            accountName = cursor.string(Calendars.ACCOUNT_NAME).orEmpty(),
            accountType = cursor.string(Calendars.ACCOUNT_TYPE).orEmpty(),
            color = cursor.int(Calendars.CALENDAR_COLOR),
            // A provider that does not say is taken to show it: hiding a calendar is the reader's call.
            visibleInProvider = cursor.int(Calendars.VISIBLE)?.let { it != 0 } ?: true,
            isPrimary = cursor.int(Calendars.IS_PRIMARY) == 1,
        )
    }

    private fun instanceOf(cursor: Cursor): EventInstance? {
        val begin = cursor.long(Instances.BEGIN) ?: return null
        val end = cursor.long(Instances.END) ?: begin
        return EventInstance(
            eventId = cursor.long(Instances.EVENT_ID) ?: return null,
            calendarId = cursor.long(Instances.CALENDAR_ID) ?: return null,
            title = cursor.string(Instances.TITLE)?.takeIf { it.isNotBlank() },
            location = cursor.string(Instances.EVENT_LOCATION)?.takeIf { it.isNotBlank() },
            begin = Instant.ofEpochMilli(begin),
            end = Instant.ofEpochMilli(maxOf(begin, end)),
            allDay = cursor.int(Instances.ALL_DAY) == 1,
            color = cursor.int(Instances.DISPLAY_COLOR),
            status = eventStatusOf(cursor.int(Instances.STATUS)),
            selfStatus = attendeeStatusOf(cursor.int(Instances.SELF_ATTENDEE_STATUS)),
            availability = availabilityOf(cursor.int(Instances.AVAILABILITY)),
            timeZone = cursor.string(Instances.EVENT_TIMEZONE),
        )
    }

    private companion object {
        val CalendarProjection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.VISIBLE,
            Calendars.IS_PRIMARY,
        )

        /** Only what is shown or decided on (§4.1). */
        val InstanceProjection = arrayOf(
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.TITLE,
            Instances.EVENT_LOCATION,
            Instances.DISPLAY_COLOR,
            Instances.STATUS,
            Instances.SELF_ATTENDEE_STATUS,
            Instances.AVAILABILITY,
            Instances.EVENT_TIMEZONE,
        )
    }
}

/*
 * Columns are found by name and read as nullable: a maker's provider that lacks one (no
 * DISPLAY_COLOR, no IS_PRIMARY) gives a default, never a crash (PLANNING.md §14).
 */

private fun <T : Any> Cursor.rows(read: (Cursor) -> T?): List<T> {
    val out = ArrayList<T>(count.coerceAtLeast(0))
    while (moveToNext()) read(this)?.let(out::add)
    return out
}

private fun Cursor.index(column: String): Int? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }

private fun Cursor.long(column: String): Long? = index(column)?.let(::getLong)

private fun Cursor.int(column: String): Int? = index(column)?.let(::getInt)

private fun Cursor.string(column: String): String? = index(column)?.let(::getString)

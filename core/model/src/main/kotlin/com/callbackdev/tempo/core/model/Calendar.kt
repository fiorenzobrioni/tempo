package com.callbackdev.tempo.core.model

import java.time.Instant

/**
 * One calendar of the phone, as the Calendar Provider lists it (PLANNING.md §4.1): a Google
 * calendar, an Exchange one, a CalDAV one through DAVx⁵, one kept on the phone alone.
 *
 * @property color the calendar's colour as ARGB, chosen in the calendar app: data, never a role
 *   (PLANNING.md §6), so it marks an event and never paints its text.
 * @property visibleInProvider whether the calendar app shows it (`VISIBLE`). Tempo follows that by
 *   default; the reader's own choice in Tempo wins over it.
 */
data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val color: Int?,
    val visibleInProvider: Boolean,
    val isPrimary: Boolean,
)

/**
 * One occurrence of an event, as the provider's `Instances` table expands it: a weekly meeting is
 * one instance a week, each with its own [begin] and [end], exceptions already applied.
 *
 * Read as stored, never adjusted: for an all-day event [begin] and [end] are midnights in UTC
 * ([end] exclusive), and only the domain turns them into dates (PLANNING.md §4.2).
 *
 * @property title null when the event has none (the screen says "(No title)").
 * @property color the event's own colour, or its calendar's (`DISPLAY_COLOR`); null when the
 *   provider has neither.
 * @property timeZone the zone the event was written in (`EVENT_TIMEZONE`), for saying it when it
 *   differs from the phone's; never used to place the event.
 */
data class EventInstance(
    val eventId: Long,
    val calendarId: Long,
    val title: String?,
    val location: String?,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val color: Int?,
    val status: EventStatus = EventStatus.CONFIRMED,
    val selfStatus: AttendeeStatus = AttendeeStatus.NONE,
    val availability: Availability = Availability.BUSY,
    val timeZone: String? = null,
)

/** The event's own status (`STATUS`): a cancelled one is never shown (PLANNING.md §4.4). */
enum class EventStatus {
    TENTATIVE,
    CONFIRMED,
    CANCELED,
}

/**
 * The reader's answer to an invitation (`SELF_ATTENDEE_STATUS`). [NONE] is an event of their own,
 * with no invitation. A declined one is hidden unless the reader shows them (PLANNING.md §4.4).
 */
enum class AttendeeStatus {
    NONE,
    ACCEPTED,
    DECLINED,
    INVITED,
    TENTATIVE,
}

/**
 * Whether the event makes the reader busy (`AVAILABILITY`): a [FREE] one (a reminder to watch a
 * match, a colleague's holiday in a shared calendar) is shown, but the free time runs through it.
 */
enum class Availability {
    BUSY,
    FREE,
    TENTATIVE,
}

/**
 * Where Tempo stands with `READ_CALENDAR`. [ASKABLE] covers a first run and a refusal Android lets
 * the app ask about again; [DENIED_FOR_GOOD] is a refusal only the system's settings can undo, so
 * the screen sends the reader there instead of asking a question that would never show.
 */
enum class CalendarPermission {
    GRANTED,
    ASKABLE,
    DENIED_FOR_GOOD,
}

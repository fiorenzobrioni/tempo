package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.EventStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** The zones the engine's tests read in: the owner's, one west and one east of Greenwich, UTC. */
val Rome: ZoneId = ZoneId.of("Europe/Rome")
val NewYork: ZoneId = ZoneId.of("America/New_York")
val Tokyo: ZoneId = ZoneId.of("Asia/Tokyo")
val Utc: ZoneId = ZoneOffset.UTC

/** A local date and time in [zone], as the instant the provider would store. */
fun at(local: String, zone: ZoneId = Rome): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

fun calendar(id: Long = 1, visible: Boolean = true) = CalendarInfo(
    id = id,
    name = "Calendar $id",
    accountName = "reader@example.com",
    accountType = "com.google",
    color = 0xFF3F51B5.toInt(),
    visibleInProvider = visible,
    isPrimary = id == 1L,
)

fun timed(
    id: Long,
    from: String,
    to: String,
    zone: ZoneId = Rome,
    title: String? = "Event $id",
    calendarId: Long = 1,
    status: EventStatus = EventStatus.CONFIRMED,
    selfStatus: AttendeeStatus = AttendeeStatus.NONE,
    availability: Availability = Availability.BUSY,
) = EventInstance(
    eventId = id,
    calendarId = calendarId,
    title = title,
    location = null,
    begin = at(from, zone),
    end = at(to, zone),
    allDay = false,
    color = null,
    status = status,
    selfStatus = selfStatus,
    availability = availability,
)

/** An all-day event as the provider stores it: midnight UTC of [first] to midnight UTC after its last date. */
fun allDay(id: Long, first: String, dates: Long = 1, title: String = "All day $id", calendarId: Long = 1) =
    EventInstance(
        eventId = id,
        calendarId = calendarId,
        title = title,
        location = null,
        begin = LocalDate.parse(first).atStartOfDay(ZoneOffset.UTC).toInstant(),
        end = LocalDate.parse(first).plusDays(dates).atStartOfDay(ZoneOffset.UTC).toInstant(),
        allDay = true,
        color = null,
        availability = Availability.FREE,
    )

fun agenda(
    vararg events: EventInstance,
    now: Instant,
    zone: ZoneId = Rome,
    days: Int = 7,
    filter: AgendaFilter = AgendaFilter(),
    calendars: List<CalendarInfo> = listOf(calendar(1)),
): Agenda = AgendaBuilder.build(events.toList(), calendars, filter, now, zone, days)

fun Agenda.day(date: String) = days.single { it.date == LocalDate.parse(date) }

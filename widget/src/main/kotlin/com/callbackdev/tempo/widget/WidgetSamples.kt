package com.callbackdev.tempo.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A realistic week for the picker's generated previews (API 35+), the settings screen's first
 * frame and the pictures: the README's own (Today's `TodaySample`), Wednesday 7 October 2026 in
 * Rome, three calendars of two accounts, at 10:20. A preview is a drawing of the product, never
 * a reading: the launcher asks for one before the card is placed, and the clock is drawn frozen
 * at the sample's time.
 */
object WidgetSamples {
    val FourByOne = DpSize(340.dp, 85.dp)
    val FourByTwo = DpSize(340.dp, 189.dp)

    private val zone: ZoneId = ZoneId.of("Europe/Rome")

    private val personal = calendar(1, "Personal", "lucia@example.com", 0xFF0B8043.toInt())
    private val work = calendar(2, "Work", "lucia@studio.example", 0xFF039BE5.toInt())
    private val family = calendar(3, "Family", "lucia@example.com", 0xFFF4511E.toInt())
    val calendars = listOf(personal, work, family)

    val instances: List<EventInstance> = listOf(
        timed(1, "2026-10-07T07:30", "2026-10-07T08:15", "Run in the park", personal),
        timed(2, "2026-10-07T09:30", "2026-10-07T09:45", "Standup", work),
        timed(3, "2026-10-07T10:00", "2026-10-07T11:00", "Design review", work, "Room 4"),
        timed(4, "2026-10-07T13:00", "2026-10-07T14:00", "Lunch with Giulia", personal, "Trattoria da Mario"),
        timed(5, "2026-10-07T15:00", "2026-10-07T15:45", "Dentist", personal, "Via Roma 3"),
        timed(6, "2026-10-07T18:30", "2026-10-07T19:30", "Swimming lesson", family, "Piscina comunale"),
        timed(7, "2026-10-08T09:00", "2026-10-08T09:30", "Call with Lisbon", work, "Google Meet"),
        timed(8, "2026-10-08T11:00", "2026-10-08T12:00", "Budget review", work, selfStatus = AttendeeStatus.TENTATIVE),
        timed(9, "2026-10-08T17:00", "2026-10-08T18:00", "Piano lesson", family),
        timed(10, "2026-10-09T08:10", "2026-10-09T09:45", "Train to Florence", personal),
        timed(11, "2026-10-09T20:30", "2026-10-09T22:30", "Dinner at Marco’s", personal),
        timed(12, "2026-10-11T10:00", "2026-10-11T12:00", "Farmers’ market", family, availability = Availability.FREE),
        allDay(13, "2026-10-06", 4, "Design Week", work),
        allDay(14, "2026-10-07", 1, "Anna’s birthday", family),
        allDay(15, "2026-10-10", 2, "Florence", personal),
    )

    fun at(local: String): ZonedDateTime = LocalDateTime.parse(local).atZone(zone)

    val Now: ZonedDateTime = at("2026-10-07T10:20")

    fun model(
        look: WidgetLook = WidgetLook(),
        now: ZonedDateTime = Now,
        settings: UserSettings = UserSettings(onboardingCompleted = true),
        instances: List<EventInstance> = this.instances,
        content: WidgetContent? = null,
        doors: WidgetDoors = WidgetDoors(canOpenEvent = true, canOpenDay = true, clockPackage = null),
    ): WidgetModel {
        val agenda = AgendaBuilder.build(
            instances = instances,
            calendars = calendars,
            filter = CalendarChoices.agendaFilter(settings, calendars),
            now = now.toInstant(),
            zone = zone,
            days = settings.horizonDays,
        )
        return WidgetModel(
            look = look,
            settings = settings,
            now = now,
            content = content ?: WidgetContent.Ready(agenda, calendars.associateBy { it.id }),
            doors = doors,
            frozenClock = true,
        )
    }

    private fun calendar(id: Long, name: String, account: String, color: Int) = CalendarInfo(
        id = id,
        name = name,
        accountName = account,
        accountType = "com.google",
        color = color,
        visibleInProvider = true,
        isPrimary = id == 1L,
    )

    private fun timed(
        id: Long,
        from: String,
        to: String,
        title: String,
        calendar: CalendarInfo,
        location: String? = null,
        selfStatus: AttendeeStatus = AttendeeStatus.NONE,
        availability: Availability = Availability.BUSY,
    ) = EventInstance(
        eventId = id,
        calendarId = calendar.id,
        title = title,
        location = location,
        begin = at(from).toInstant(),
        end = at(to).toInstant(),
        allDay = false,
        color = null,
        selfStatus = selfStatus,
        availability = availability,
    )

    private fun allDay(id: Long, first: String, dates: Long, title: String, calendar: CalendarInfo) = EventInstance(
        eventId = id,
        calendarId = calendar.id,
        title = title,
        location = null,
        begin = LocalDate.parse(first).atStartOfDay(ZoneOffset.UTC).toInstant(),
        end = LocalDate.parse(first).plusDays(dates).atStartOfDay(ZoneOffset.UTC).toInstant(),
        allDay = true,
        color = null,
        availability = Availability.FREE,
    )
}

package com.callbackdev.tempo.feature.today

import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.today.DaySentence
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
 * A realistic week for Today's tests and the README's pictures: Wednesday 7 October 2026 in Rome,
 * three calendars of two accounts, a morning already partly gone, a conference running all week.
 * Drawn from states, never from a provider (PLANNING.md §12).
 */
object TodaySample {
    val zone: ZoneId = ZoneId.of("Europe/Rome")

    val personal = calendar(1, "Personal", "lucia@example.com", 0xFF0B8043.toInt())
    val work = calendar(2, "Work", "lucia@studio.example", 0xFF039BE5.toInt())
    val family = calendar(3, "Family", "lucia@example.com", 0xFFF4511E.toInt())
    val calendars = listOf(personal, work, family)

    val run = timed(1, "2026-10-07T07:30", "2026-10-07T08:15", "Run in the park", personal)
    val standup = timed(2, "2026-10-07T09:30", "2026-10-07T09:45", "Standup", work)
    val review = timed(3, "2026-10-07T10:00", "2026-10-07T11:00", "Design review", work, location = "Room 4")
    val lunch =
        timed(4, "2026-10-07T13:00", "2026-10-07T14:00", "Lunch with Giulia", personal, location = "Trattoria da Mario")
    val dentist = timed(5, "2026-10-07T15:00", "2026-10-07T15:45", "Dentist", personal, location = "Via Roma 3")
    val swimming =
        timed(6, "2026-10-07T18:30", "2026-10-07T19:30", "Swimming lesson", family, location = "Piscina comunale")
    val call = timed(7, "2026-10-08T09:00", "2026-10-08T09:30", "Call with Lisbon", work, location = "Google Meet")
    val budget =
        timed(8, "2026-10-08T11:00", "2026-10-08T12:00", "Budget review", work, selfStatus = AttendeeStatus.TENTATIVE)
    val piano = timed(9, "2026-10-08T17:00", "2026-10-08T18:00", "Piano lesson", family)
    val train = timed(10, "2026-10-09T08:10", "2026-10-09T09:45", "Train to Florence", personal)
    val dinner = timed(11, "2026-10-09T20:30", "2026-10-09T22:30", "Dinner at Marco’s", personal)
    val market =
        timed(12, "2026-10-11T10:00", "2026-10-11T12:00", "Farmers’ market", family, availability = Availability.FREE)
    val conference = allDay(13, "2026-10-06", 4, "Design Week", work)
    val birthday = allDay(14, "2026-10-07", 1, "Anna’s birthday", family)
    val trip = allDay(15, "2026-10-10", 2, "Florence", personal)

    val instances = listOf(run, standup, review, lunch, dentist, swimming) +
        listOf(call, budget, piano, train, dinner, market) +
        listOf(conference, birthday, trip)

    fun at(local: String): ZonedDateTime = LocalDateTime.parse(local).atZone(zone)

    fun state(
        now: ZonedDateTime = at("2026-10-07T10:20"),
        settings: UserSettings = UserSettings(onboardingCompleted = true),
        alarm: String? = "2026-10-08T07:00",
        instances: List<EventInstance> = this.instances,
        doors: CalendarDoors = CalendarDoors.All,
    ): TodayUiState {
        val agenda = AgendaBuilder.build(
            instances = instances,
            calendars = calendars,
            filter = CalendarChoices.agendaFilter(settings, calendars),
            now = now.toInstant(),
            zone = zone,
            days = settings.horizonDays,
        )
        return TodayUiState(
            now = now,
            settings = settings,
            alarm = alarm?.let { at(it).toInstant() },
            content = TodayContent.Ready(agenda, DaySentence.today(agenda), calendars.associateBy { it.id }),
            doors = doors,
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

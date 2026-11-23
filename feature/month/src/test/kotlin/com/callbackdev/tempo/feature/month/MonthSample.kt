package com.callbackdev.tempo.feature.month

import com.callbackdev.tempo.core.calendar.CalendarDoors
import com.callbackdev.tempo.core.domain.agenda.AgendaBuilder
import com.callbackdev.tempo.core.domain.calendar.CalendarChoices
import com.callbackdev.tempo.core.domain.month.MonthMarks
import com.callbackdev.tempo.core.domain.month.MonthPages
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * A realistic month for the page's tests and the README's picture: November 2026 in Rome, seen on
 * Thursday the 12th at 10:20, a meeting under way. Three calendars of two accounts; a conference
 * that fills a week and a birthday inside it, a weekend away, a busy day and an empty one, and a
 * course that runs past the month's end. Drawn from states, never from a provider (PLANNING.md
 * §12).
 */
object MonthSample {
    val zone: ZoneId = ZoneId.of("Europe/Rome")
    val month: YearMonth = YearMonth.of(2026, 11)

    val personal = calendar(1, "Personal", "lucia@example.com", 0xFF0B8043.toInt())
    val work = calendar(2, "Work", "lucia@studio.example", 0xFF039BE5.toInt())
    val family = calendar(3, "Family", "lucia@example.com", 0xFFF4511E.toInt())
    val calendars = listOf(personal, work, family)

    val standup = timed(1, "2026-11-12T09:30", "2026-11-12T09:45", "Standup", work)
    val review = timed(2, "2026-11-12T10:00", "2026-11-12T11:00", "Design review", work, "Room 4")
    val lunch = timed(3, "2026-11-12T13:00", "2026-11-12T14:00", "Lunch with Giulia", personal, "Trattoria da Mario")
    val dentist = timed(4, "2026-11-12T15:00", "2026-11-12T15:45", "Dentist", personal, "Via Roma 3")
    val swimming = timed(5, "2026-11-12T18:30", "2026-11-12T19:30", "Swimming lesson", family, "Piscina comunale")
    val designWeek = allDay(6, "2026-11-09", 4, "Design Week", work)
    val birthday = allDay(7, "2026-11-12", 1, "Anna’s birthday", family)
    val train = timed(8, "2026-11-20T08:10", "2026-11-20T09:45", "Train to Florence", personal)
    val florence = allDay(9, "2026-11-20", 3, "Florence", personal)
    val dinner = timed(10, "2026-11-26T20:30", "2026-11-26T22:30", "Dinner at Marco’s", personal)
    val course = timed(11, "2026-11-30T09:00", "2026-12-02T17:00", "Typography course", work, "Milan")

    /** The rest of the month: the week's standing meetings, and a few days of the reader's own. */
    private val others = listOf(
        timed(20, "2026-10-29T18:00", "2026-10-29T19:00", "Piano lesson", family),
        timed(21, "2026-11-02T09:30", "2026-11-02T09:45", "Standup", work),
        timed(22, "2026-11-02T15:00", "2026-11-02T16:00", "Budget review", work),
        timed(23, "2026-11-04T07:30", "2026-11-04T08:15", "Run in the park", personal),
        timed(24, "2026-11-04T17:00", "2026-11-04T18:00", "Piano lesson", family),
        timed(25, "2026-11-05T09:30", "2026-11-05T09:45", "Standup", work),
        timed(26, "2026-11-06T20:30", "2026-11-06T22:30", "Cinema", personal),
        timed(27, "2026-11-10T10:00", "2026-11-10T11:30", "Keynote", work),
        timed(28, "2026-11-10T14:00", "2026-11-10T15:00", "Workshop", work),
        timed(29, "2026-11-11T17:00", "2026-11-11T18:00", "Piano lesson", family),
        timed(30, "2026-11-13T09:00", "2026-11-13T09:30", "Call with Lisbon", work, "Google Meet"),
        timed(31, "2026-11-16T09:30", "2026-11-16T09:45", "Standup", work),
        timed(32, "2026-11-17T11:00", "2026-11-17T12:00", "Budget review", work),
        timed(33, "2026-11-18T07:30", "2026-11-18T08:15", "Run in the park", personal),
        timed(34, "2026-11-18T17:00", "2026-11-18T18:00", "Piano lesson", family),
        timed(35, "2026-11-19T09:30", "2026-11-19T09:45", "Standup", work),
        timed(36, "2026-11-19T13:00", "2026-11-19T14:00", "Lunch with the team", work),
        timed(37, "2026-11-23T09:30", "2026-11-23T09:45", "Standup", work),
        timed(38, "2026-11-25T17:00", "2026-11-25T18:00", "Piano lesson", family),
        timed(39, "2026-11-28T10:00", "2026-11-28T12:00", "Farmers’ market", family),
        timed(40, "2026-12-03T09:30", "2026-12-03T09:45", "Standup", work),
        allDay(41, "2026-12-08", 1, "Immacolata", family),
    )

    val instances = listOf(standup, review, lunch, dentist, swimming, designWeek, birthday) +
        listOf(train, florence, dinner, course) + others

    fun at(local: String): ZonedDateTime = LocalDateTime.parse(local).atZone(zone)

    fun date(iso: String): LocalDate = LocalDate.parse(iso)

    /** Every door open: a calendar app that takes all three intents, and a clock app. */
    val allDoors =
        CalendarDoors(canCreate = true, canOpenEvent = true, canOpenDay = true, clockPackage = "com.example.clock")

    val settings = UserSettings(onboardingCompleted = true, todayTipSeen = true)

    /** @param shown the month whose dates, and its neighbours', the state holds: what the page asked to read. */
    fun state(
        now: ZonedDateTime = at("2026-11-12T10:20"),
        settings: UserSettings = this.settings,
        instances: List<EventInstance> = this.instances,
        doors: CalendarDoors = allDoors,
        shown: YearMonth = month,
    ): MonthUiState {
        val span = MonthPages.span(shown)
        val byId = calendars.associateBy { it.id }
        val days = AgendaBuilder.days(
            instances = instances,
            calendars = calendars,
            filter = CalendarChoices.agendaFilter(settings, calendars),
            now = now.toInstant(),
            zone = zone,
            from = span.start,
            to = span.endInclusive,
        )
        return MonthUiState(
            now = now,
            settings = settings,
            content = MonthContent.Ready(days.associateBy { it.date }, MonthMarks.of(days, byId, zone), byId),
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

    fun timed(id: Long, from: String, to: String, title: String, calendar: CalendarInfo, location: String? = null) =
        EventInstance(
            eventId = id,
            calendarId = calendar.id,
            title = title,
            location = location,
            begin = at(from).toInstant(),
            end = at(to).toInstant(),
            allDay = false,
            color = null,
        )

    fun allDay(id: Long, first: String, dates: Long, title: String, calendar: CalendarInfo) = EventInstance(
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

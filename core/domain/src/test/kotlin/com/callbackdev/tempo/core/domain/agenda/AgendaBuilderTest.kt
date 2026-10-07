package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** Every edge case of PLANNING.md §4.6 that the domain decides, one test each. */
class AgendaBuilderTest {
    // §4.2: all-day events are read in UTC.

    @Test
    fun `an all-day event stays on its date west of Greenwich`() {
        // 00:00 UTC on 7 October is 20:00 on the 6th in New York: read in the phone's zone it
        // would open the evening before.
        val holiday = allDay(1, "2026-10-07")
        val result = agenda(holiday, now = at("2026-10-06T21:00", NewYork), zone = NewYork, days = 2)
        assertThat(result.day("2026-10-06").allDay).isEmpty()
        assertThat(result.day("2026-10-06").timed).isEmpty()
        assertThat(result.day("2026-10-07").allDay.single().event).isEqualTo(holiday)
    }

    @Test
    fun `an all-day event does not spill into the next date east of Greenwich`() {
        // Its end, 00:00 UTC on the 8th, is 09:00 on the 8th in Tokyo.
        val holiday = allDay(1, "2026-10-07")
        val result = agenda(holiday, now = at("2026-10-07T10:00", Tokyo), zone = Tokyo, days = 2)
        assertThat(result.day("2026-10-07").allDay).hasSize(1)
        assertThat(result.day("2026-10-08").allDay).isEmpty()
    }

    @Test
    fun `an all-day event at UTC covers its date alone`() {
        val result = agenda(allDay(1, "2026-10-07"), now = at("2026-10-07T00:00", Utc), zone = Utc, days = 2)
        assertThat(result.day("2026-10-07").allDay).hasSize(1)
        assertThat(result.day("2026-10-08").allDay).isEmpty()
    }

    @Test
    fun `a multi-day all-day event says which of its days each date is`() {
        val trip = allDay(1, "2026-10-06", dates = 5)
        val result = agenda(trip, now = at("2026-10-07T09:00"), days = 7)
        val entries = result.days.flatMap { day -> day.allDay.map { day.date to it } }
        assertThat(entries.map { it.first }).containsExactly(
            LocalDate.parse("2026-10-07"),
            LocalDate.parse("2026-10-08"),
            LocalDate.parse("2026-10-09"),
            LocalDate.parse("2026-10-10"),
        ).inOrder()
        assertThat(entries.map { it.second.dayNumber }).containsExactly(2, 3, 4, 5).inOrder()
        assertThat(entries.map { it.second.dayCount }.toSet()).containsExactly(5)
    }

    @Test
    fun `the reader can leave all-day events out`() {
        val result = agenda(
            allDay(1, "2026-10-07"),
            timed(2, "2026-10-07T10:00", "2026-10-07T11:00"),
            now = at("2026-10-07T09:00"),
            filter = AgendaFilter(showAllDay = false),
        )
        assertThat(result.day("2026-10-07").allDay).isEmpty()
        assertThat(result.day("2026-10-07").timed).hasSize(1)
    }

    // §4.3: dates, midnights, overlaps.

    @Test
    fun `an event crossing midnight is on both dates, from and until`() {
        val party = timed(1, "2026-10-07T23:00", "2026-10-08T01:30")
        val result = agenda(party, now = at("2026-10-07T20:00"))
        val first = result.day("2026-10-07").timed.single()
        val second = result.day("2026-10-08").timed.single()
        assertThat(first.startsOnDate).isTrue()
        assertThat(first.endsOnDate).isFalse()
        assertThat(second.startsOnDate).isFalse()
        assertThat(second.endsOnDate).isTrue()
    }

    @Test
    fun `a timed event over two midnights is all day on the date it covers whole`() {
        // Decided in Phase 1 (PLANNING.md §15): the middle date reads "day 2 of 3" in the all-day
        // row, the first "from 21:00", the last "until 02:00".
        val conference = timed(1, "2026-10-07T21:00", "2026-10-09T02:00")
        val result = agenda(conference, now = at("2026-10-07T20:00"))
        assertThat(result.day("2026-10-07").timed.single().endsOnDate).isFalse()
        val middle = result.day("2026-10-08")
        assertThat(middle.timed).isEmpty()
        assertThat(middle.allDay.single().dayNumber).isEqualTo(2)
        assertThat(middle.allDay.single().dayCount).isEqualTo(3)
        assertThat(result.day("2026-10-09").timed.single().startsOnDate).isFalse()
    }

    @Test
    fun `a timed event covering a date is shown even with all-day events left out`() {
        val conference = timed(1, "2026-10-06T09:00", "2026-10-08T17:00")
        val result = agenda(conference, now = at("2026-10-07T09:00"), filter = AgendaFilter(showAllDay = false))
        assertThat(result.day("2026-10-07").allDay).hasSize(1)
    }

    @Test
    fun `an event ending at midnight belongs to its own date only`() {
        val evening = timed(1, "2026-10-07T22:00", "2026-10-08T00:00")
        val result = agenda(evening, now = at("2026-10-07T09:00"))
        assertThat(result.day("2026-10-07").timed.single().endsOnDate).isTrue()
        assertThat(result.day("2026-10-08").timed).isEmpty()
    }

    @Test
    fun `overlapping events are listed by start, then end, then title`() {
        val result = agenda(
            timed(1, "2026-10-07T10:00", "2026-10-07T12:00", title = "Long"),
            timed(2, "2026-10-07T10:00", "2026-10-07T11:00", title = "Short"),
            timed(3, "2026-10-07T09:30", "2026-10-07T10:30", title = "Early"),
            timed(4, "2026-10-07T10:00", "2026-10-07T11:00", title = "Also short"),
            now = at("2026-10-07T08:00"),
        )
        assertThat(result.day("2026-10-07").timed.map { it.event.title })
            .containsExactly("Early", "Also short", "Short", "Long").inOrder()
    }

    @Test
    fun `a zero-length event is a moment on its date`() {
        val reminder = timed(1, "2026-10-07T12:00", "2026-10-07T12:00")
        val before = agenda(reminder, now = at("2026-10-07T11:00"))
        assertThat(before.day("2026-10-07").timed.single().state).isEqualTo(EntryState.UPCOMING)
        assertThat(before.next).isEqualTo(reminder)
        val after = agenda(reminder, now = at("2026-10-07T12:30"))
        assertThat(after.day("2026-10-07").timed.single().state).isEqualTo(EntryState.PAST)
    }

    @Test
    fun `past, under way and upcoming, and which one is next`() {
        val done = timed(1, "2026-10-07T08:00", "2026-10-07T09:00")
        val running = timed(2, "2026-10-07T09:30", "2026-10-07T10:30")
        val later = timed(3, "2026-10-07T15:00", "2026-10-07T16:00")
        val tomorrow = timed(4, "2026-10-08T09:00", "2026-10-08T10:00")
        val result = agenda(done, running, later, tomorrow, now = at("2026-10-07T10:00"))
        assertThat(result.day("2026-10-07").timed.map { it.state })
            .containsExactly(EntryState.PAST, EntryState.UNDER_WAY, EntryState.UPCOMING).inOrder()
        assertThat(result.underWay).containsExactly(running)
        assertThat(result.next).isEqualTo(later)
    }

    @Test
    fun `next stops at the horizon`() {
        val farAway = timed(1, "2026-10-20T09:00", "2026-10-20T10:00")
        assertThat(agenda(farAway, now = at("2026-10-07T10:00"), days = 7).next).isNull()
    }

    // §4.6: recurrences, the provider's work, arrive as instances.

    @Test
    fun `a recurring event shows its moved occurrence and drops its cancelled one`() {
        val weekly = listOf(
            timed(1, "2026-10-05T09:00", "2026-10-05T09:30", title = "Standup"),
            // The exception: Wednesday's occurrence moved to 11:00.
            timed(1, "2026-10-07T11:00", "2026-10-07T11:30", title = "Standup"),
            // The deletion: the provider's instance of a cancelled exception.
            timed(1, "2026-10-09T09:00", "2026-10-09T09:30", title = "Standup", status = EventStatus.CANCELED),
        )
        val result = agenda(*weekly.toTypedArray(), now = at("2026-10-05T08:00"))
        assertThat(result.day("2026-10-07").timed.single().event.begin).isEqualTo(at("2026-10-07T11:00"))
        assertThat(result.day("2026-10-09").timed).isEmpty()
    }

    // §4.6: daylight saving and travel.

    @Test
    fun `the night clocks go forward is a 23-hour date`() {
        // Europe/Rome, 29 March 2026: 02:00 becomes 03:00. Half past midnight on the 30th must not
        // be taken for the 29th by a date assumed 24 hours long.
        val early = timed(1, "2026-03-30T00:30", "2026-03-30T00:45")
        val acrossTheGap = timed(2, "2026-03-29T01:30", "2026-03-29T03:30")
        val result = agenda(early, acrossTheGap, now = at("2026-03-29T00:10"), days = 2)
        assertThat(result.day("2026-03-29").timed.map { it.event }).containsExactly(acrossTheGap)
        assertThat(result.day("2026-03-30").timed.map { it.event }).containsExactly(early)
        assertThat(Duration.between(acrossTheGap.begin, acrossTheGap.end)).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `the night clocks go back keeps both half past twos, in order`() {
        // Europe/Rome, 25 October 2026: 03:00 becomes 02:00, so 02:30 happens twice.
        val first = timed(1, "2026-10-25T00:30", "2026-10-25T00:45", zone = Utc, title = "First 02:30")
        val second = timed(2, "2026-10-25T01:30", "2026-10-25T01:45", zone = Utc, title = "Second 02:30")
        val late = timed(3, "2026-10-25T23:30", "2026-10-25T23:45", title = "Late")
        val result = agenda(second, late, first, now = at("2026-10-25T00:10"), days = 2)
        assertThat(result.day("2026-10-25").timed.map { it.event.title })
            .containsExactly("First 02:30", "Second 02:30", "Late").inOrder()
        assertThat(result.day("2026-10-26").timed).isEmpty()
    }

    @Test
    fun `a trip moves timed events between dates but never all-day ones`() {
        // 01:00 in Rome on the 7th is 19:00 on the 6th in New York.
        val call = timed(1, "2026-10-07T01:00", "2026-10-07T02:00")
        val holiday = allDay(2, "2026-10-07")
        val inRome = agenda(call, holiday, now = at("2026-10-06T12:00"), zone = Rome, days = 2)
        val inNewYork = agenda(call, holiday, now = at("2026-10-06T12:00"), zone = NewYork, days = 2)
        assertThat(inRome.day("2026-10-07").timed.map { it.event }).containsExactly(call)
        assertThat(inNewYork.day("2026-10-06").timed.map { it.event }).containsExactly(call)
        assertThat(inRome.day("2026-10-07").allDay).hasSize(1)
        assertThat(inNewYork.day("2026-10-07").allDay).hasSize(1)
    }

    @Test
    fun `midnight turns the agenda over to the next date`() {
        val tomorrow = timed(1, "2026-10-08T09:00", "2026-10-08T10:00")
        val before = agenda(tomorrow, now = at("2026-10-07T23:59"), days = 1)
        val after = agenda(tomorrow, now = at("2026-10-08T00:00"), days = 1)
        assertThat(before.today).isEqualTo(LocalDate.parse("2026-10-07"))
        assertThat(before.day("2026-10-07").isEmpty).isTrue()
        assertThat(after.today).isEqualTo(LocalDate.parse("2026-10-08"))
        assertThat(after.day("2026-10-08").timed).hasSize(1)
    }

    // §4.4: what is not shown.

    @Test
    fun `declined invitations are hidden unless the reader shows them`() {
        val declined = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", selfStatus = AttendeeStatus.DECLINED)
        val tentative = timed(2, "2026-10-07T12:00", "2026-10-07T13:00", selfStatus = AttendeeStatus.TENTATIVE)
        val hidden = agenda(declined, tentative, now = at("2026-10-07T09:00"))
        assertThat(hidden.day("2026-10-07").timed.map { it.event }).containsExactly(tentative)
        val shown =
            agenda(declined, tentative, now = at("2026-10-07T09:00"), filter = AgendaFilter(showDeclined = true))
        assertThat(shown.day("2026-10-07").timed).hasSize(2)
    }

    @Test
    fun `a cancelled event is never shown`() {
        val cancelled = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", status = EventStatus.CANCELED)
        val result = agenda(cancelled, now = at("2026-10-07T09:00"), filter = AgendaFilter(showDeclined = true))
        assertThat(result.day("2026-10-07").isEmpty).isTrue()
        assertThat(result.next).isNull()
    }

    @Test
    fun `an event with no title and no colour is still shown`() {
        val bare = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", title = null)
        val result = agenda(bare, now = at("2026-10-07T09:00"), calendars = listOf(calendar(1).copy(color = null)))
        assertThat(result.day("2026-10-07").timed.single().event.title).isNull()
    }

    @Test
    fun `a calendar the calendar app hides is hidden until the reader shows it`() {
        val event = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", calendarId = 2)
        val calendars = listOf(calendar(1), calendar(2, visible = false))
        assertThat(
            agenda(event, now = at("2026-10-07T09:00"), calendars = calendars).day("2026-10-07").isEmpty,
        ).isTrue()
        val shown =
            agenda(
                event,
                now = at("2026-10-07T09:00"),
                calendars = calendars,
                filter = AgendaFilter(shownCalendars = setOf(2)),
            )
        assertThat(shown.day("2026-10-07").timed).hasSize(1)
    }

    @Test
    fun `the reader's hiding wins over everything`() {
        val event = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", calendarId = 1)
        val filter = AgendaFilter(shownCalendars = setOf(1), hiddenCalendars = setOf(1))
        assertThat(agenda(event, now = at("2026-10-07T09:00"), filter = filter).day("2026-10-07").isEmpty).isTrue()
    }

    @Test
    fun `an event of a calendar not in the list is shown`() {
        val event = timed(1, "2026-10-07T10:00", "2026-10-07T11:00", calendarId = 9)
        assertThat(agenda(event, now = at("2026-10-07T09:00")).day("2026-10-07").timed).hasSize(1)
    }

    // §4.6: the empty cases.

    @Test
    fun `an empty today, an empty horizon, no calendar at all`() {
        val empty = agenda(now = at("2026-10-07T09:00"), calendars = emptyList())
        assertThat(empty.days).hasSize(7)
        assertThat(empty.days.all { it.isEmpty }).isTrue()
        assertThat(empty.next).isNull()
        assertThat(empty.underWay).isEmpty()
    }

    @Test
    fun `every calendar hidden leaves every date empty`() {
        val result = agenda(
            timed(1, "2026-10-07T10:00", "2026-10-07T11:00", calendarId = 1),
            allDay(2, "2026-10-08", calendarId = 2),
            now = at("2026-10-07T09:00"),
            calendars = listOf(calendar(1), calendar(2)),
            filter = AgendaFilter(hiddenCalendars = setOf(1, 2)),
        )
        assertThat(result.days.all { it.isEmpty }).isTrue()
    }

    @Test
    fun `the horizon is today and the dates after it`() {
        val result = agenda(now = at("2026-10-07T09:00"), days = 7)
        assertThat(result.days.map { it.date }.first()).isEqualTo(LocalDate.parse("2026-10-07"))
        assertThat(result.days.map { it.date }.last()).isEqualTo(LocalDate.parse("2026-10-13"))
    }
}

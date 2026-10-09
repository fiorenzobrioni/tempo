package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CardAgendaTest {
    private val holiday = allDay(9, "2026-10-07", dates = 3, title = "Holiday")
    private val standup = timed(1, "2026-10-07T09:30", "2026-10-07T10:00", title = "Standup")
    private val review = timed(2, "2026-10-07T10:00", "2026-10-07T11:00", title = "Review")
    private val dentist = timed(3, "2026-10-07T15:00", "2026-10-07T16:00", title = "Dentist")
    private val call = timed(4, "2026-10-08T09:00", "2026-10-08T10:00", title = "Call")
    private val friday = timed(5, "2026-10-09T18:00", "2026-10-09T19:00", title = "Dinner")

    private fun com.callbackdev.tempo.core.model.AllDayEntry.entry() = event.title.orEmpty()

    private fun titles(lines: List<CardLine>) = lines.map {
        when (it) {
            is CardLine.AllDay -> it.entries.joinToString(" + ") { entry -> entry.entry() }
            is CardLine.Timed -> it.entry.event.title
            is CardLine.Heading -> "# ${it.date}"
            is CardLine.Note -> "! ${it.note}"
        }
    }

    @Test
    fun `the rest of today, all-day first, past events left out, the one under way kept`() {
        val card = CardAgenda.of(
            agenda(holiday, standup, review, dentist, now = at("2026-10-07T10:20")),
            showAllDay = true,
            showDaysAhead = false,
        )
        assertThat(titles(card.today)).containsExactly("Holiday", "Review", "Dentist").inOrder()
        assertThat(card.later).isEmpty()
    }

    @Test
    fun `the card's own switch takes the all-day events away`() {
        val card = CardAgenda.of(
            agenda(holiday, dentist, now = at("2026-10-07T10:20")),
            showAllDay = false,
            showDaysAhead = false,
        )
        assertThat(titles(card.today)).containsExactly("Dentist")
    }

    @Test
    fun `the days ahead under their headings, an empty day skipped`() {
        val card = CardAgenda.of(
            agenda(dentist, call, friday, now = at("2026-10-07T10:20")),
            showAllDay = true,
            showDaysAhead = true,
        )
        assertThat(titles(card.later))
            .containsExactly("# 2026-10-08", "Call", "# 2026-10-09", "Dinner")
            .inOrder()
    }

    @Test
    fun `today done says so before tomorrow`() {
        val card = CardAgenda.of(agenda(dentist, call, now = at("2026-10-07T20:00")), true, true)
        assertThat(card.today).containsExactly(CardLine.Note(TodayNote.NOTHING_LEFT))
        assertThat(titles(card.later)).containsExactly("# 2026-10-08", "Call").inOrder()
    }

    @Test
    fun `a day with nothing at all says that`() {
        val card = CardAgenda.of(agenda(call, now = at("2026-10-07T08:00")), true, false)
        assertThat(card.today).containsExactly(CardLine.Note(TodayNote.NOTHING_TODAY))
    }

    @Test
    fun `a date's all-day events share one line, and count as each of them`() {
        val birthday = allDay(10, "2026-10-07", title = "Birthday")
        val card = CardAgenda.of(agenda(holiday, birthday, dentist, now = at("2026-10-07T10:20")), true, false)
        assertThat(titles(card.today)).containsExactly("Birthday + Holiday", "Dentist").inOrder()
        assertThat(card.today.first().events).isEqualTo(2)
    }

    @Test
    fun `an all-day event carries its own date on every day`() {
        val card = CardAgenda.of(agenda(holiday, now = at("2026-10-07T08:00")), true, true)
        val dates = card.lines.filterIsInstance<CardLine.AllDay>().map { it.date }
        assertThat(dates).containsExactly(
            LocalDate.parse("2026-10-07"),
            LocalDate.parse("2026-10-08"),
            LocalDate.parse("2026-10-09"),
        ).inOrder()
    }

    // --- The fit ------------------------------------------------------------------------------

    private val oneEach: (CardLine) -> Float = { 10f }

    private fun card(today: Int, later: List<Int>): CardAgenda {
        var id = 0L
        fun line(date: String) = CardLine.Timed(
            com.callbackdev.tempo.core.model.TimedEntry(
                timed(++id, "${date}T10:00", "${date}T11:00"),
                true,
                true,
                com.callbackdev.tempo.core.model.EntryState.UPCOMING,
            ),
            LocalDate.parse(date),
        )
        val laterLines = later.flatMapIndexed { i, n ->
            val date = LocalDate.parse("2026-10-08").plusDays(i.toLong()).toString()
            listOf(CardLine.Heading(LocalDate.parse(date))) + List(n) { line(date) }
        }
        return CardAgenda(List(today) { line("2026-10-07") }, laterLines)
    }

    @Test
    fun `everything that fits is drawn, with no footer`() {
        assertThat(fitLines(card(3, listOf(2)), oneEach, room = 60f, footerHeight = 10f)).isEqualTo(CardFit(6, null))
    }

    @Test
    fun `today cut, its remainder counted, the days ahead not begun`() {
        assertThat(fitLines(card(5, listOf(2)), oneEach, room = 40f, footerHeight = 10f))
            .isEqualTo(CardFit(3, CardFooter.MoreToday(2)))
    }

    @Test
    fun `today whole without room for the footer is drawn whole, the days ahead not begun`() {
        // Four lines of today fill the 40 dp; nothing ahead fits, so nothing needs counting.
        assertThat(fitLines(card(4, listOf(2)), oneEach, room = 40f, footerHeight = 10f)).isEqualTo(CardFit(4, null))
    }

    @Test
    fun `a heading is never the last line, and the days ahead begun are counted`() {
        // Today 2, then «Thu», 2 events, «Fri», 2 events: 70 dp less the footer holds 6 lines,
        // which would end on «Fri»; it goes, and Friday's two are counted.
        val fit = fitLines(card(2, listOf(2, 2)), oneEach, room = 70f, footerHeight = 10f)
        assertThat(fit).isEqualTo(CardFit(5, CardFooter.MoreLater(2)))
    }

    @Test
    fun `days ahead that cannot show one event are not begun at all`() {
        val fit = fitLines(card(2, listOf(3)), oneEach, room = 35f, footerHeight = 10f)
        assertThat(fit).isEqualTo(CardFit(2, null))
    }

    @Test
    fun `lines past the most a card draws are counted, never dropped`() {
        // Room for all six of today and the two ahead, but a card draws four lines at most.
        assertThat(fitLines(card(6, listOf(2)), oneEach, room = 1000f, footerHeight = 10f, maxLines = 4))
            .isEqualTo(CardFit(4, CardFooter.MoreToday(2)))
        // Today whole within the cap: the days ahead are begun and their remainder counted.
        assertThat(fitLines(card(2, listOf(3)), oneEach, room = 1000f, footerHeight = 10f, maxLines = 4))
            .isEqualTo(CardFit(4, CardFooter.MoreLater(2)))
    }

    @Test
    fun `on a row, today's timed events come before its all-day line`() {
        val card = CardAgenda.of(agenda(holiday, review, dentist, now = at("2026-10-07T10:20")), true, false)
        assertThat(titles(card.timedFirst().today)).containsExactly("Review", "Dentist", "Holiday").inOrder()
    }

    @Test
    fun `a day of all-day events alone keeps them first`() {
        val card = CardAgenda.of(agenda(holiday, call, now = at("2026-10-07T10:20")), true, true)
        assertThat(card.timedFirst()).isEqualTo(card)
    }
}

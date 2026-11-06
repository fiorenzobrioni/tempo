package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.agenda.NewYork
import com.callbackdev.tempo.core.domain.agenda.Rome
import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
import com.callbackdev.tempo.core.domain.agenda.calendar
import com.callbackdev.tempo.core.domain.agenda.timed
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/** «In words»: what it puts large, what it says under it, and its forms at the reference grants. */
class WordsTest {
    private val standup = timed(1, "2026-10-07T09:30", "2026-10-07T10:30", title = "Standup")
    private val review = timed(2, "2026-10-07T10:00", "2026-10-07T10:15", title = "Review")
    private val dentist = timed(3, "2026-10-07T15:00", "2026-10-07T16:00", title = "Dentist")
    private val gym = timed(4, "2026-10-07T18:00", "2026-10-07T19:00", title = "Gym")
    private val call = timed(5, "2026-10-09T09:00", "2026-10-09T10:00", title = "Call")
    private val holiday = allDay(9, "2026-10-07", title = "Holiday")

    private fun day(vararg events: com.callbackdev.tempo.core.model.EventInstance, now: String, ahead: Boolean = true) =
        WordsDay.of(agenda(*events, now = at(now)), showAllDay = true, showDaysAhead = ahead)

    @Test
    fun `under way, the one that ends first, then how many are left, and their times`() {
        val result = day(standup, review, dentist, gym, now = "2026-10-07T10:05")
        assertThat(result.focus).isEqualTo(WordsFocus.UnderWay(review))
        assertThat(result.note).isEqualTo(WordsNote.ThenMore(2))
        assertThat(result.then).containsExactly(dentist, gym).inOrder()
    }

    @Test
    fun `free now, the next one, and how many after it`() {
        val result = day(standup, dentist, gym, now = "2026-10-07T11:00")
        assertThat(result.focus).isEqualTo(WordsFocus.Next(dentist, LocalDate.parse("2026-10-07"), today = true))
        assertThat(result.note).isEqualTo(WordsNote.FreeUntilThen(1))
        assertThat(result.then).containsExactly(gym)
    }

    @Test
    fun `today done, the first event of the next day that has one`() {
        val result = day(dentist, call, holiday, now = "2026-10-07T20:00")
        assertThat(result.focus).isEqualTo(WordsFocus.Next(call, LocalDate.parse("2026-10-09"), today = false))
        // Today's holiday is not said under tomorrow's event, where it would read as tomorrow's.
        assertThat(result.note).isEqualTo(WordsNote.DayDone(hadEvents = true, allDay = emptyList()))
        assertThat(result.then).isEmpty()
    }

    @Test
    fun `a free day says its all-day events`() {
        val result = day(holiday, now = "2026-10-07T08:00", ahead = false)
        assertThat(result.focus).isEqualTo(WordsFocus.Free)
        val note = result.note as WordsNote.DayDone
        assertThat(note.allDay.map { it.event }).containsExactly(holiday)
    }

    @Test
    fun `without the days ahead, a finished day is free`() {
        val result = day(dentist, call, now = "2026-10-07T20:00", ahead = false)
        assertThat(result.focus).isEqualTo(WordsFocus.Free)
        assertThat(result.note).isEqualTo(WordsNote.DayDone(hadEvents = true, allDay = emptyList()))
    }

    @Test
    fun `a day with no event at all says so`() {
        val result = day(now = "2026-10-07T08:00")
        assertThat(result.focus).isEqualTo(WordsFocus.Free)
        assertThat(result.note).isEqualTo(WordsNote.DayDone(hadEvents = false, allDay = emptyList()))
    }

    // --- The dial ---------------------------------------------------------------------------------

    private fun arcs(vararg events: com.callbackdev.tempo.core.model.EventInstance, now: String) =
        agenda(*events, now = at(now)).let { agenda ->
            DialArcs.of(agenda, WordsDay.of(agenda, true, showDaysAhead = true).focus, Rome)
        }

    @Test
    fun `the dial carries the event under way from its start, and what is still to come`() {
        val result = arcs(standup, dentist, gym, now = "2026-10-07T10:05")
        // 9:30 is 570 minutes past twelve on the face; the standup lasts an hour.
        assertThat(result.last()).isEqualTo(DialArc(from = 570f, sweep = 60f, focus = true))
        assertThat(result.filterNot { it.focus }).containsExactly(
            DialArc(from = 180f, sweep = 60f, focus = false),
            DialArc(from = 360f, sweep = 60f, focus = false),
        )
    }

    @Test
    fun `each arc carries its calendar's colour, the event's own first`() {
        val green = 0xFF0B8043.toInt()
        val orange = 0xFFF4511E.toInt()
        val personal = calendar(1).copy(color = green)
        val tinted = dentist.copy(color = orange)
        val agenda = agenda(standup, tinted, gym, now = at("2026-10-07T10:05"), calendars = listOf(personal))
        val result = DialArcs.of(agenda, WordsDay.of(agenda, true, true).focus, Rome, mapOf(1L to personal))
        // The standup (the focus, last) and the gym have no colour of their own: their calendar's.
        assertThat(result.map { it.from to it.color }).containsExactly(
            180f to orange,
            360f to green,
            570f to green,
        ).inOrder()
    }

    @Test
    fun `the card's arcs are where they were, a colour beside them`() {
        // The cards call the dial without the calendars: only the event's own colour comes along,
        // and the arcs' places and the focus are the same as Today's.
        val tinted = dentist.copy(color = 0xFFF4511E.toInt())
        val agenda = agenda(standup, tinted, now = at("2026-10-07T10:05"))
        val focus = WordsDay.of(agenda, true, true).focus
        val card = DialArcs.of(agenda, focus, Rome)
        val today = DialArcs.of(agenda, focus, Rome, mapOf(1L to calendar(1)))
        assertThat(card.map { it.copy(color = null) }).isEqualTo(today.map { it.copy(color = null) })
        assertThat(card.map { it.color }).containsExactly(0xFFF4511E.toInt(), null).inOrder()
    }

    @Test
    fun `the focus is drawn last, over the others`() {
        val result = arcs(standup, review, now = "2026-10-07T10:05")
        assertThat(result.map { it.focus }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `nothing past and no other day`() {
        // At 20:00 the dentist is over and the call is in two days: an empty face.
        assertThat(arcs(dentist, call, now = "2026-10-07T20:00")).isEmpty()
        // At 23:00 tomorrow's 08:00 would sit at 8 o'clock, just behind the hand, as if just over.
        val early = timed(6, "2026-10-08T08:00", "2026-10-08T09:00", title = "Early")
        assertThat(arcs(early, now = "2026-10-07T23:00")).isEmpty()
    }

    @Test
    fun `tomorrow's event is not on the face even as the focus, nor cut short`() {
        // The owner's evening (5 Nov 2026): nothing left today, tomorrow 08:00 to 18:00. The words
        // name it; the face drew 8 to 10 o'clock of it, behind the hand, beside "nothing today".
        val tyres = timed(6, "2026-10-08T08:00", "2026-10-08T18:00", title = "Tyres")
        val agenda = agenda(tyres, now = at("2026-10-07T22:37"))
        val focus = WordsDay.of(agenda, true, showDaysAhead = true).focus
        assertThat(focus).isEqualTo(WordsFocus.Next(tyres, LocalDate.parse("2026-10-08"), today = false))
        assertThat(DialArcs.of(agenda, focus, Rome)).isEmpty()
    }

    @Test
    fun `an event of today that runs past midnight is drawn whole`() {
        val late = timed(6, "2026-10-07T22:00", "2026-10-08T01:00", title = "Late")
        assertThat(arcs(late, now = "2026-10-07T20:00"))
            .containsExactly(DialArc(from = 600f, sweep = 180f, focus = true))
    }

    @Test
    fun `an arc never closes the circle onto the hand`() {
        val long = timed(7, "2026-10-07T08:00", "2026-10-08T08:00", title = "Conference")
        val result = arcs(long, now = "2026-10-07T12:00")
        val arc = result.single()
        // Six hours back at most, and the window ends half an hour short of the face's twelve.
        assertThat(arc.from).isEqualTo(480f)
        assertThat(arc.sweep).isEqualTo(690f)
        assertThat(arc.sweep).isLessThan(DialArcs.FACE_MINUTES)
        // Early in the day an event of today can reach past the window: cut there, or it would
        // cross the hand. At 07:00, 13:00 to 20:00 stops at 18:30.
        val afternoon = timed(8, "2026-10-07T13:00", "2026-10-07T20:00", title = "Afternoon")
        assertThat(arcs(afternoon, now = "2026-10-07T07:00"))
            .containsExactly(DialArc(from = 60f, sweep = 330f, focus = true))
    }

    @Test
    fun `the night the clocks go forward, twelve hours on the face are eleven real ones`() {
        // Europe/Rome, 28 March 2027: 02:00 becomes 03:00. From 00:30, noon is ten and a half real
        // hours away, but on the face it sits under the hand, twelve hours round, less the gap: it
        // is not drawn, or it would read as now. 11:00 is in the window.
        val noon = timed(9, "2027-03-28T12:00", "2027-03-28T13:00", title = "Noon")
        assertThat(arcs(noon, now = "2027-03-28T00:30")).isEmpty()
        val eleven = timed(10, "2027-03-28T11:00", "2027-03-28T12:00", title = "Eleven")
        assertThat(arcs(eleven, now = "2027-03-28T00:30"))
            .containsExactly(DialArc(from = 660f, sweep = 60f, focus = true))
    }

    @Test
    fun `the night the clocks go back, an arc ends where the hand will be when the event ends`() {
        // Europe/Rome, 25 October 2026: 03:00 becomes 02:00. 01:00 to 04:00 is four real hours, and
        // three on the face: the hand goes back an hour on the way.
        val night = timed(11, "2026-10-25T01:00", "2026-10-25T04:00", title = "Night shift")
        assertThat(arcs(night, now = "2026-10-25T00:30"))
            .containsExactly(DialArc(from = 60f, sweep = 180f, focus = true))
    }

    @Test
    fun `the night the clocks go back, twelve hours on the face are thirteen real ones`() {
        // America/New_York, 1 November 2026: 02:00 becomes 01:00. From midnight 11:00 is twelve
        // real hours away and eleven on the face, ahead of the hand: it is drawn, up to the
        // window's end half an hour short of the hand.
        val late = timed(12, "2026-11-01T11:00", "2026-11-01T12:00", zone = NewYork, title = "Late")
        val agenda = agenda(late, now = at("2026-11-01T00:00", NewYork), zone = NewYork)
        val result = DialArcs.of(agenda, WordsDay.of(agenda, true, true).focus, NewYork)
        assertThat(result).containsExactly(DialArc(from = 660f, sweep = 30f, focus = true))
    }

    @Test
    fun `a half-hour change moves the face by half an hour`() {
        // Australia/Lord_Howe, 4 October 2026: 02:00 becomes 02:30. 01:45 to 02:45 is half an hour.
        val zone = java.time.ZoneId.of("Australia/Lord_Howe")
        val call = timed(13, "2026-10-04T01:45", "2026-10-04T02:45", zone = zone, title = "Call")
        val agenda = agenda(call, now = at("2026-10-04T01:00", zone), zone = zone)
        val result = DialArcs.of(agenda, WordsDay.of(agenda, true, true).focus, zone)
        assertThat(result).containsExactly(DialArc(from = 105f, sweep = 60f, focus = true))
    }

    @Test
    fun `a moment still shows`() {
        val reminder = timed(8, "2026-10-07T15:00", "2026-10-07T15:00", title = "Reminder")
        assertThat(arcs(reminder, now = "2026-10-07T11:00").single().sweep).isEqualTo(DialArcs.MIN_SWEEP)
    }

    // --- The forms ----------------------------------------------------------------------------

    private fun spec(
        width: Float,
        height: Float,
        scale: Float = 1f,
        dial: Boolean = true,
        date: Boolean = true,
        noteLines: Int = 1,
        titleLines: Int = 1,
        rows: Int = 0,
        rowTimeEm: Float = 2.7f,
        secondEm: Float = 0f,
    ) = WordsSpec(
        width = width,
        height = height,
        fontScale = scale,
        dial = dial,
        // "Wednesday 7 October", "Wed 7 Oct", "07/10/2026"; the first over two lines is "7 October".
        dateEms = if (date) listOf(9.4f, 5.2f, 5.6f) else emptyList(),
        dateTwoLineEms = if (date) listOf(5.0f, 3.0f, 5.6f) else emptyList(),
        titleEm = 6.5f,
        titleLines = titleLines,
        // "Until 11:00", "11:00"
        whenEms = listOf(5.3f, 2.7f),
        noteLines = noteLines,
        then = true,
        rows = rows,
        // "13:00"
        rowTimeEm = rowTimeEm,
        secondEm = secondEm,
    )

    @Test
    fun `every reference grant has its form`() {
        assertThat(WordsFit.form(85f, 85f)).isEqualTo(WordsForm.CELL)
        assertThat(WordsFit.form(85f, 189f)).isEqualTo(WordsForm.CELL)
        assertThat(WordsFit.form(159f, 85f)).isEqualTo(WordsForm.ROW)
        assertThat(WordsFit.form(340f, 85f)).isEqualTo(WordsForm.ROW)
        assertThat(WordsFit.form(159f, 189f)).isEqualTo(WordsForm.TALL)
        assertThat(WordsFit.form(340f, 189f)).isEqualTo(WordsForm.TALL)
    }

    @Test
    fun `a four by one is a row, the dial, the title over its time, the date at the far edge`() {
        val row = WordsFit.plan(spec(340f, 85f))
        assertThat(row.dial).isEqualTo(WordsFit.ROW_DIAL_MAX)
        assertThat(row.titleSp).isEqualTo(WordsFit.TITLE_SP)
        assertThat(row.whenChoice).isEqualTo(0)
        assertThat(row.whenSp).isEqualTo(WordsFit.WHEN_SP)
        // The long date over two lines: "Wednesday / 7 October".
        assertThat(row.dateChoice).isEqualTo(0)
        assertThat(row.dateLines).isEqualTo(2)
        assertThat(row.dateSp).isEqualTo(WordsFit.DATE_SP)
    }

    @Test
    fun `a narrow row keeps the dial and sets the words a size smaller, without the date`() {
        val row = WordsFit.plan(spec(159f, 85f))
        assertThat(row.dial).isAtLeast(WordsFit.ROW_DIAL_MIN)
        assertThat(row.titleSp).isEqualTo(WordsFit.NARROW_TITLE_SP)
        assertThat(row.dateChoice).isEqualTo(-1)
        // "Design review" does not fit beside the dial: two lines over its time, not an ellipsis.
        assertThat(row.titleLines).isEqualTo(2)
    }

    @Test
    fun `a row's words fit its height at every text size`() {
        listOf(1f, 1.15f, 1.3f, 2f).forEach { scale ->
            val row = WordsFit.plan(spec(340f, 85f, scale = scale))
            val used = lineHeight(row.titleSp, scale) * row.titleLines +
                (if (row.whenChoice >= 0) lineHeight(row.whenSp, scale) else 0f)
            assertThat(used).isAtMost(85f - AgendaFit.CARD_PADDING_SNUG * 2)
            assertThat(row.dial).isAtMost(85f - AgendaFit.CARD_PADDING_SNUG * 2)
            if (row.dateChoice >= 0) {
                assertThat(clockLineHeight(row.dateSp, scale) * row.dateLines)
                    .isAtMost(85f - AgendaFit.CARD_PADDING_SNUG * 2)
            }
        }
    }

    @Test
    fun `one cell is the dial alone, and a tall cell says the time under it`() {
        val cell = WordsFit.plan(spec(85f, 85f))
        assertThat(cell.dial).isEqualTo(85f - AgendaFit.CELL_PADDING * 2)
        assertThat(cell.whenChoice).isEqualTo(-1)
        val tall = WordsFit.plan(spec(85f, 189f))
        assertThat(tall.dial).isEqualTo(WordsFit.cellCircle(85f, 189f))
        assertThat(tall.whenChoice).isAtLeast(0)
    }

    @Test
    fun `without the dial, one cell sets the time bold in its place`() {
        val cell = WordsFit.plan(spec(85f, 85f, dial = false))
        assertThat(cell.dial).isEqualTo(0f)
        assertThat(cell.whenBold).isTrue()
        assertThat(cell.whenSp).isAtLeast(WordsFit.CELL_WHEN_MIN)
    }

    @Test
    fun `a tall card hangs the words from the bottom, the dial and the date over them`() {
        val tall = WordsFit.plan(spec(340f, 189f))
        assertThat(tall.dial).isAtLeast(WordsFit.TALL_DIAL_COMFORT)
        assertThat(tall.noteLines).isEqualTo(1)
        // Beside a dial on a four-wide card, the long date on one line.
        assertThat(tall.dateChoice).isEqualTo(0)
        assertThat(tall.dateLines).isEqualTo(1)
        val small = WordsFit.plan(spec(159f, 189f))
        assertThat(small.dial).isAtLeast(WordsFit.TALL_CIRCLE_MIN)
        assertThat(small.dateChoice).isAtLeast(0)
        assertThat(small.dateWidth + small.dial + WordsFit.DIAL_GAP).isAtMost(159f - AgendaFit.CARD_PADDING * 2)
    }

    @Test
    fun `a tall card fits its height at every text size and grant`() {
        listOf(1f, 1.15f, 1.3f).forEach { scale ->
            listOf(
                159f to 189f,
                250f to 189f,
                340f to 189f,
                159f to 293f,
                250f to 293f,
                340f to 293f,
                340f to 397f,
                159f to 160f,
            ).forEach { (w, h) ->
                val plan = WordsFit.plan(spec(w, h, scale, noteLines = 2, titleLines = 2, rows = 2))
                val words = linesHeight(plan.titleSp, plan.titleLines, scale) +
                    (if (plan.rows > 0) lineHeight(WordsFit.ROW_SP, scale) * plan.rows + WordsFit.ROWS_GAP else 0f) +
                    (if (plan.whenChoice >= 0) lineHeight(plan.whenSp, scale) else 0f) +
                    (
                        if (plan.noteLines >
                            0
                        ) {
                            linesHeight(WordsFit.NOTE_SP, plan.noteLines, scale) + WordsFit.NOTE_GAP
                        } else {
                            0f
                        }
                        ) +
                    (if (plan.then) lineHeight(WordsFit.NOTE_SP, scale) else 0f)
                val top = if (plan.dial > 0f) {
                    plan.dial + WordsFit.DIAL_BOTTOM_GAP + if (plan.dateOver) {
                        clockLineHeight(plan.dateSp, scale) * plan.dateLines + WordsFit.DATE_OVER_GAP
                    } else {
                        0f
                    }
                } else if (plan.dateChoice >= 0) {
                    clockLineHeight(plan.dateSp, scale) + WordsFit.TOP_GAP
                } else {
                    0f
                }
                assertThat(words + top).isAtMost(h - AgendaFit.CARD_PADDING * 2 + 0.01f)
            }
        }
    }

    @Test
    fun `the note is whole or not there`() {
        val tight = WordsFit.plan(spec(159f, 160f, scale = 1.3f, noteLines = 3))
        assertThat(tight.noteLines).isAnyOf(0, 3)
        val long = WordsFit.plan(spec(340f, 293f, noteLines = 4))
        assertThat(long.noteLines).isEqualTo(0)
    }

    @Test
    fun `without the dial, the date rides on top of a tall card`() {
        val plan = WordsFit.plan(spec(340f, 189f, dial = false))
        assertThat(plan.dial).isEqualTo(0f)
        assertThat(plan.dateChoice).isEqualTo(0)
        assertThat(plan.dateLines).isEqualTo(1)
    }

    @Test
    fun `the date steps down before it is lost, and is never cut`() {
        // Too narrow for the long date beside the dial: a shorter one, or the long one on two lines.
        val plan = WordsFit.plan(spec(159f, 189f))
        val room = 159f - AgendaFit.CARD_PADDING * 2 - plan.dial - WordsFit.DIAL_GAP
        assertThat(plan.dateWidth).isAtMost(room)
        assertThat(WordsFit.plan(spec(340f, 189f, date = false)).dateChoice).isEqualTo(-1)
    }

    @Test
    fun `a title a little too long for a tall card steps down to stay whole`() {
        // "Design review" at 22 sp is wider than a two by two's 131 dp: 18 sp keeps it on one line.
        val plan = WordsFit.plan(spec(159f, 189f))
        assertThat(plan.titleSp).isLessThan(WordsFit.TALL_TITLE_SP)
        assertThat(plan.titleSp).isAtLeast(WordsFit.TALL_TITLE_MIN_SP)
        assertThat(WordsFit.plan(spec(340f, 189f)).titleSp).isEqualTo(WordsFit.TALL_TITLE_SP)
    }

    // --- «In words» uses its room (step 8.3) ------------------------------------------------------

    @Test
    fun `the dial's side at the reference grants`() {
        // One cell: the cell's circle, the time under it where the cell is tall.
        assertThat(WordsFit.plan(spec(85f, 85f)).dial).isEqualTo(69f)
        assertThat(WordsFit.plan(spec(85f, 189f, rows = 2)).dial).isEqualTo(69f)
        // One row: 44 dp on two cells (it was 47), 56 from three.
        assertThat(WordsFit.plan(spec(159f, 85f)).dial).isEqualTo(44f)
        assertThat(WordsFit.plan(spec(250f, 85f)).dial).isEqualTo(56f)
        assertThat(WordsFit.plan(spec(340f, 85f)).dial).isEqualTo(56f)
        // Two rows: 52.75 dp on two cells (it was 63), 73.75 from three (it was 78.5, and up to
        // 101 on a free day).
        assertThat(WordsFit.plan(spec(159f, 189f, rows = 2)).dial).isEqualTo(52.75f)
        assertThat(WordsFit.plan(spec(250f, 189f, rows = 2)).dial).isEqualTo(73.75f)
        assertThat(WordsFit.plan(spec(340f, 189f, rows = 2)).dial).isEqualTo(73.75f)
        // Three rows: the ceiling, 104 dp on two and three cells (two by three: it was 63, held by
        // the date beside it), 45% of the inner height on four.
        assertThat(WordsFit.plan(spec(159f, 293f, rows = 2)).dial).isEqualTo(104f)
        assertThat(WordsFit.plan(spec(250f, 293f, rows = 2)).dial).isEqualTo(104f)
        assertThat(WordsFit.plan(spec(340f, 293f, rows = 2)).dial).isEqualTo(119.25f)
    }

    // --- The family's circle (4 Nov 2026) ----------------------------------------------------------

    @Test
    fun `the family's circle at the reference grants`() {
        assertThat(WordsFit.cellCircle(85f, 85f)).isEqualTo(69f)
        assertThat(WordsFit.cellCircle(85f, 293f)).isEqualTo(69f)
        assertThat(WordsFit.cellCircle(110f, 100f)).isEqualTo(84f)
        assertThat(WordsFit.rowCircle(159f, 85f)).isEqualTo(44f)
        assertThat(WordsFit.rowCircle(250f, 85f)).isEqualTo(56f)
        assertThat(WordsFit.rowCircle(340f, 70f)).isEqualTo(WordsFit.ROW_DIAL_MAX)
        // Two rows high: 161 dp inside, less the band's 108.24 under three cells, 87.12 from three.
        assertThat(WordsFit.tallCircle(159f, 189f, 1f)).isEqualTo(52.75f)
        assertThat(WordsFit.tallCircle(250f, 189f, 1f)).isEqualTo(73.75f)
        assertThat(WordsFit.tallCircle(340f, 189f, 1f)).isEqualTo(73.75f)
        // At larger text sizes the band grows and the circle yields, to its floor.
        assertThat(WordsFit.tallCircle(159f, 189f, 1.15f)).isEqualTo(WordsFit.TALL_CIRCLE_MIN)
        assertThat(WordsFit.tallCircle(159f, 189f, 1.3f)).isEqualTo(WordsFit.TALL_CIRCLE_MIN)
        // Taller, larger, to the ceiling: 104 dp, or 45% of the inner side.
        assertThat(WordsFit.tallCircle(159f, 214f, 1f)).isEqualTo(77.75f)
        assertThat(WordsFit.tallCircle(159f, 293f, 1f)).isEqualTo(104f)
        assertThat(WordsFit.tallCircle(340f, 293f, 1f)).isEqualTo(119.25f)
        assertThat(WordsFit.tallCircle(340f, 397f, 1f)).isEqualTo(140.25f)
    }

    @Test
    fun `the dial is one size through the day, at every size`() {
        // Busy: the title, its time, a note over two lines, the next events. Free: the title alone
        // and a line of note. Bare: the title alone. None moves the dial: before, a free day's
        // grew (to 79 dp on a two by two 175 dp wide, to 101 on a three by two, where a busy day's
        // stood at 60.5 and 78.5).
        listOf(159f, 175f, 200f, 250f, 340f).forEach { w ->
            listOf(189f, 230f, 293f, 397f).forEach { h ->
                val busy = WordsFit.plan(spec(w, h, noteLines = 2, rows = 2))
                val free = WordsFit.plan(spec(w, h).copy(whenEms = emptyList()))
                val bare = WordsFit.plan(spec(w, h, noteLines = 0).copy(whenEms = emptyList()))
                listOf(busy, free, bare).forEach { assertThat(it.dial).isEqualTo(WordsFit.tallCircle(w, h, 1f)) }
            }
        }
        // A large text size: the dial is the circle's floor, with the words whole under it.
        val large = WordsFit.plan(spec(159f, 189f, scale = 1.3f))
        assertThat(large.dial).isEqualTo(WordsFit.TALL_CIRCLE_MIN)
        assertThat(large.whenChoice).isAtLeast(0)
    }

    @Test
    fun `where the dial leaves the date no column, the date stands over the title`() {
        // Two by three: 104 dp leave 15 beside it.
        val plan = WordsFit.plan(spec(159f, 293f, rows = 2))
        assertThat(plan.dateOver).isTrue()
        assertThat(plan.dateLines).isEqualTo(1)
        assertThat(plan.dateWidth).isAtMost(159f - AgendaFit.CARD_PADDING * 2)
        // The words it held under its 63 dp dial: the note and the next two events.
        assertThat(plan.noteLines).isEqualTo(1)
        assertThat(plan.rows).isEqualTo(2)
        // With its column, beside it.
        assertThat(WordsFit.plan(spec(159f, 189f)).dateOver).isFalse()
        assertThat(WordsFit.plan(spec(340f, 293f)).dateOver).isFalse()
        // The second clock under the date, over the title.
        assertThat(WordsFit.plan(spec(159f, 293f, secondEm = secondEm)).second).isTrue()
    }

    @Test
    fun `the owner's two-by-two gives a long title its second line under the family's circle`() {
        // 188 x 218 dp, measured from the phone's screenshots (5 Nov 2026): the circle at 81.75 dp,
        // "Prenotare cambio gomme" (too long for one line at 18 sp) over two lines, its time and
        // the note under it, as before the circle (the dial then at 78 dp). With an 8 dp gap and
        // every line counted at 1.32 em it lost its second line for 4 dp.
        val spec = spec(188f, 218f, titleLines = 2).copy(titleEm = 11.5f)
        val plan = WordsFit.plan(spec)
        assertThat(plan.dial).isEqualTo(WordsFit.tallCircle(188f, 218f, 1f))
        assertThat(plan.dial).isEqualTo(81.75f)
        assertThat(plan.titleLines).isEqualTo(2)
        assertThat(plan.whenChoice).isEqualTo(0)
        assertThat(plan.noteLines).isEqualTo(1)
        assertThat(plan.dateOver).isFalse()
    }

    @Test
    fun `the event alone has the band of a figure's line and one more, the family's ring's without its sentence`() {
        // Two rows: 161 - 66 = 95 dp; the owner's two-by-two and three rows: the ceiling.
        assertThat(WordsFit.tallCircle(159f, 189f, 1f, note = false)).isEqualTo(95f)
        assertThat(WordsFit.tallCircle(250f, 189f, 1f, note = false)).isEqualTo(95f)
        assertThat(WordsFit.tallCircle(188f, 218f, 1f, note = false)).isEqualTo(104f)
        assertThat(WordsFit.tallCircle(159f, 293f, 1f, note = false)).isEqualTo(104f)
        assertThat(WordsFit.tallCircle(340f, 293f, 1f, note = false)).isEqualTo(119.25f)
        // At 130% the band grows: 161 - 85.8.
        assertThat(WordsFit.tallCircle(159f, 189f, 1.3f, note = false)).isEqualTo(75f)
    }

    @Test
    fun `the event alone says its title on one line and its time, and the dial takes the band`() {
        val full = spec(188f, 218f, noteLines = 2, titleLines = 2, rows = 2).copy(titleEm = 11.5f)
        val plan = WordsFit.plan(full.copy(note = false))
        assertThat(plan.dial).isEqualTo(104f)
        assertThat(plan.titleLines).isEqualTo(1)
        // Too long for one line even at 18 sp: at 18, to say more of it before the ellipsis.
        assertThat(plan.titleSp).isEqualTo(WordsFit.TALL_TITLE_MIN_SP)
        assertThat(plan.whenChoice).isEqualTo(0)
        assertThat(plan.noteLines).isEqualTo(0)
        assertThat(plan.rows).isEqualTo(0)
        assertThat(plan.then).isFalse()
        // 104 dp leave the date 44 beside it: over the title, where it fits under the circle.
        assertThat(plan.dateOver).isTrue()
        assertThat(plan.dateChoice).isAtLeast(0)
        // The same at every size, a free day's and a busy one's.
        listOf(159f, 188f, 250f, 340f).forEach { w ->
            listOf(189f, 218f, 293f).forEach { h ->
                val busy = WordsFit.plan(spec(w, h, noteLines = 2, rows = 2).copy(note = false))
                val free = WordsFit.plan(spec(w, h).copy(whenEms = emptyList(), note = false))
                listOf(busy, free).forEach {
                    assertThat(it.dial).isEqualTo(WordsFit.tallCircle(w, h, 1f, note = false))
                }
            }
        }
    }

    @Test
    fun `with the event alone, a card too short for the date over the title lets the date go`() {
        // The reference two-by-two: 95 dp leave the date 24 beside it, and over the title it would
        // take the circle under 95 (the owner: the circle stays, the date goes).
        val plan = WordsFit.plan(spec(159f, 189f).copy(note = false))
        assertThat(plan.dial).isEqualTo(95f)
        assertThat(plan.dateChoice).isEqualTo(-1)
        // With the note the date keeps its place beside the dial.
        assertThat(WordsFit.plan(spec(159f, 189f)).dateChoice).isAtLeast(0)
        // Wide enough for its column, it stays beside the dial.
        val wide = WordsFit.plan(spec(250f, 189f).copy(note = false))
        assertThat(wide.dateOver).isFalse()
        assertThat(wide.dateChoice).isAtLeast(0)
    }

    @Test
    fun `what the circle gives up is air, and the words are the ones the card held`() {
        // The 19 dp under the comfort would hold the next event as a row; the card keeps its note.
        val plan = WordsFit.plan(spec(159f, 189f, noteLines = 1, rows = 1))
        assertThat(plan.rows).isEqualTo(0)
        assertThat(plan.noteLines).isEqualTo(1)
        assertThat(WordsFit.plan(spec(159f, 189f, noteLines = 2, rows = 1)).noteLines).isEqualTo(2)
    }

    @Test
    fun `the dial's ceiling is a share of the card, never under its old one`() {
        // Four by three: 45% of 265 dp of height.
        assertThat(WordsFit.tallDialCeiling(265f, 312f)).isEqualTo(119.25f)
        // Four by four: 45% of 312 dp of width.
        assertThat(WordsFit.tallDialCeiling(369f, 312f)).isWithin(0.01f).of(140.4f)
        // Two rows high: the old ceiling, where the room holds the dial under it anyway.
        assertThat(WordsFit.tallDialCeiling(161f, 312f)).isEqualTo(WordsFit.TALL_DIAL_CEILING)
    }

    @Test
    fun `a four by three lists the next two events in its band`() {
        val plan = WordsFit.plan(spec(340f, 293f, rows = 2))
        assertThat(plan.rows).isEqualTo(2)
        assertThat(plan.then).isFalse()
        assertThat(plan.noteLines).isEqualTo(1)
        assertThat(plan.rowTimeWidth).isEqualTo(widthOf(2.7f, WordsFit.ROW_SP, 1f))
        // One event after the focus is one row.
        assertThat(WordsFit.plan(spec(340f, 293f, rows = 1)).rows).isEqualTo(1)
        // Three by three and two by three too: a row's title keeps its room beside the time.
        assertThat(WordsFit.plan(spec(250f, 293f, rows = 2)).rows).isEqualTo(2)
        assertThat(WordsFit.plan(spec(159f, 293f, rows = 2)).rows).isEqualTo(2)
    }

    @Test
    fun `the rows are all or none, and the line of times stays where only it fits`() {
        // A four by two holds one line under the words at its dial's comfort, not two (under way,
        // without the note: the times say how many are left).
        val plan = WordsFit.plan(spec(340f, 189f, noteLines = 0, rows = 2))
        assertThat(plan.rows).isEqualTo(0)
        assertThat(plan.rowTimeWidth).isEqualTo(0f)
        assertThat(plan.then).isTrue()
        assertThat(plan.dial).isAtLeast(WordsFit.TALL_DIAL_COMFORT)
    }

    @Test
    fun `the note's count where neither fits`() {
        // Three by two: no room for the rows, too narrow for the line of times.
        val plan = WordsFit.plan(spec(250f, 189f, rows = 2))
        assertThat(plan.rows).isEqualTo(0)
        assertThat(plan.then).isFalse()
        assertThat(plan.noteLines).isEqualTo(1)
    }

    @Test
    fun `the rows wait for the dial's comfort`() {
        // Between two and three rows high, the rows come only once the dial has its comfort.
        listOf(189f, 210f, 230f, 250f, 293f).forEach { h ->
            val plan = WordsFit.plan(spec(340f, h, rows = 2))
            if (plan.rows > 0) assertThat(plan.dial).isAtLeast(WordsFit.TALL_DIAL_COMFORT)
        }
    }

    @Test
    fun `a row's title keeps its room beside the time, or there are no rows`() {
        // A two by three at 130% with a time as wide as "10:00 AM": the title would keep 59 dp.
        val plan = WordsFit.plan(spec(159f, 293f, scale = 1.3f, rows = 2, rowTimeEm = 4.2f))
        assertThat(plan.rows).isEqualTo(0)
        // A four by four does, where its dial (the family's 140.25 dp) leaves the rows their lines.
        assertThat(WordsFit.plan(spec(340f, 397f, scale = 1.3f, rows = 2, rowTimeEm = 4.2f)).rows).isEqualTo(2)
    }

    @Test
    fun `no rows on a row or a cell`() {
        assertThat(WordsFit.plan(spec(340f, 85f, rows = 2)).rows).isEqualTo(0)
        assertThat(WordsFit.plan(spec(85f, 189f, rows = 2)).rows).isEqualTo(0)
    }

    // --- The second clock (step 8.5) ---------------------------------------------------------

    /** "New York 23:58" in the regular face. */
    private val secondEm = 6.9f

    @Test
    fun `a tall card sets the second clock under the date, beside the dial, and keeps the rest`() {
        listOf(340f to 189f, 250f to 189f, 340f to 293f).forEach { (w, h) ->
            val without = WordsFit.plan(spec(w, h, rows = 2))
            val with = WordsFit.plan(spec(w, h, rows = 2, secondEm = secondEm))
            assertThat(with.second).isTrue()
            assertThat(with.copy(second = false)).isEqualTo(without)
            val band = w - AgendaFit.CARD_PADDING * 2 - with.dial - WordsFit.DIAL_GAP
            assertThat(widthOf(secondEm, WordsFit.SECOND_SP, 1f)).isAtMost(band)
            assertThat(
                clockLineHeight(with.dateSp, 1f) * with.dateLines + WordsFit.SECOND_TOP +
                    clockLineHeight(WordsFit.SECOND_SP, 1f),
            ).isAtMost(with.dial)
        }
    }

    @Test
    fun `a band too narrow for the second clock leaves it out, never the date`() {
        // Beside a two by two's dial the band is 66 dp, the date's.
        val plan = WordsFit.plan(spec(159f, 189f, secondEm = secondEm))
        assertThat(plan.second).isFalse()
        assertThat(plan.dateChoice).isEqualTo(WordsFit.plan(spec(159f, 189f)).dateChoice)
    }

    @Test
    fun `without the dial, the second clock rides on top under the date`() {
        val plan = WordsFit.plan(spec(340f, 189f, dial = false, secondEm = secondEm))
        assertThat(plan.second).isTrue()
        assertThat(plan.dateChoice).isEqualTo(0)
    }

    @Test
    fun `a row and a cell have no second clock`() {
        assertThat(WordsFit.plan(spec(340f, 85f, secondEm = secondEm)).second).isFalse()
        assertThat(WordsFit.plan(spec(85f, 189f, secondEm = secondEm)).second).isFalse()
    }

    @Test
    fun `a tall card with the second clock fits its height at every text size and grant`() {
        listOf(1f, 1.15f, 1.3f, 2f).forEach { scale ->
            listOf(159f to 189f, 250f to 189f, 340f to 189f, 340f to 293f, 159f to 160f).forEach { (w, h) ->
                listOf(true, false).forEach { dial ->
                    val plan = WordsFit.plan(spec(w, h, scale, dial = dial, noteLines = 2, secondEm = secondEm))
                    val words = linesHeight(plan.titleSp, plan.titleLines, scale) +
                        (if (plan.whenChoice >= 0) lineHeight(plan.whenSp, scale) else 0f) +
                        (
                            if (plan.noteLines >
                                0
                            ) {
                                linesHeight(WordsFit.NOTE_SP, plan.noteLines, scale) + WordsFit.NOTE_GAP
                            } else {
                                0f
                            }
                            ) +
                        (if (plan.then) lineHeight(WordsFit.NOTE_SP, scale) else 0f)
                    val date = if (plan.dateChoice >= 0) clockLineHeight(plan.dateSp, scale) * plan.dateLines else 0f
                    val second = if (plan.second) clockLineHeight(WordsFit.SECOND_SP, scale) else 0f
                    val top = if (plan.dial > 0f && plan.dateOver) {
                        plan.dial + WordsFit.DIAL_BOTTOM_GAP + date + WordsFit.DATE_OVER_GAP +
                            (if (plan.second) second + WordsFit.SECOND_TOP else 0f)
                    } else if (plan.dial > 0f) {
                        assertThat(date + (if (plan.second) WordsFit.SECOND_TOP else 0f) + second).isAtMost(plan.dial)
                        plan.dial + WordsFit.DIAL_BOTTOM_GAP
                    } else {
                        (if (date > 0f) date + WordsFit.TOP_GAP else 0f) +
                            (if (plan.second) second + if (date > 0f) WordsFit.SECOND_TOP else WordsFit.TOP_GAP else 0f)
                    }
                    assertThat(words + top).isAtMost(h - AgendaFit.CARD_PADDING * 2 + 0.01f)
                }
            }
        }
    }
}

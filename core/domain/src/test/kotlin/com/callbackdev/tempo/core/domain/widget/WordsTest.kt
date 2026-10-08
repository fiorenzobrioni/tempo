package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.agenda.agenda
import com.callbackdev.tempo.core.domain.agenda.allDay
import com.callbackdev.tempo.core.domain.agenda.at
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

    // --- The forms ----------------------------------------------------------------------------

    private fun spec(width: Float, height: Float, scale: Float = 1f, label: Boolean = false, title: Boolean = true) =
        WordsSpec(
            width = width,
            height = height,
            fontScale = scale,
            topEms = listOf(14.5f, 9.8f, 2.4f),
            heroEm = 2.65f,
            label = label,
            title = title,
            titleLines = 1,
            noteLines = 2,
            then = true,
        )

    @Test
    fun `every reference grant has its form`() {
        assertThat(WordsFit.form(85f, 85f)).isEqualTo(WordsForm.NEXT)
        assertThat(WordsFit.form(159f, 85f)).isEqualTo(WordsForm.LINE)
        assertThat(WordsFit.form(340f, 85f)).isEqualTo(WordsForm.LINE)
        assertThat(WordsFit.form(159f, 189f)).isEqualTo(WordsForm.STACK)
        assertThat(WordsFit.form(250f, 189f)).isEqualTo(WordsForm.STACK)
        assertThat(WordsFit.form(340f, 189f)).isEqualTo(WordsForm.PANEL)
    }

    @Test
    fun `a wide row sets the time and the title on one line, a narrow one stacks them`() {
        assertThat(WordsFit.plan(spec(340f, 85f)).titleInline).isTrue()
        val narrow = WordsFit.plan(spec(159f, 85f))
        assertThat(narrow.titleInline).isFalse()
        assertThat(narrow.titleLines).isEqualTo(1)
    }

    @Test
    fun `the one-cell card keeps its title only while the time stays comfortable`() {
        val cell = WordsFit.plan(spec(85f, 85f))
        assertThat(cell.heroSp).isAtLeast(WordsFit.NEXT_HERO_COMFORT)
        assertThat(cell.heroSp * 2.65f + FIT_SLACK).isAtMost(85f - AgendaFit.CELL_PADDING * 2)
    }

    @Test
    fun `the tall card buys the top line, the note and the line of times before it grows the time`() {
        val panel = WordsFit.plan(spec(340f, 189f))
        assertThat(panel.topChoice).isEqualTo(0)
        assertThat(panel.titleLines).isEqualTo(1)
        assertThat(panel.noteLines).isEqualTo(2)
        assertThat(panel.then).isTrue()
        assertThat(panel.heroSp).isAtLeast(WordsFit.TALL_HERO_FLOOR)
    }

    @Test
    fun `the narrow tall card shortens its top line rather than dropping it`() {
        assertThat(WordsFit.plan(spec(159f, 189f)).topChoice).isEqualTo(2)
        assertThat(WordsFit.plan(spec(200f, 189f)).topChoice).isEqualTo(1)
    }

    @Test
    fun `what the tall card holds never overflows it, at any text size`() {
        listOf(159f to 189f, 250f to 189f, 340f to 189f, 340f to 293f).forEach { (w, h) ->
            listOf(1f, 1.3f, 2f).forEach { scale ->
                val plan = WordsFit.plan(spec(w, h, scale, label = true))
                val used = lineHeight(plan.heroSp, scale) + lineHeight(WordsFit.LABEL_SP, scale) +
                    lineHeight(WordsFit.TITLE_SP, scale) * plan.titleLines +
                    (if (plan.topChoice >= 0) lineHeight(WordsFit.TOP_SP, scale) + WordsFit.TOP_GAP else 0f) +
                    (if (plan.noteLines > 0) WordsFit.NOTE_GAP else 0f) +
                    lineHeight(WordsFit.NOTE_SP, scale) * (plan.noteLines + if (plan.then) 1 else 0)
                assertThat(used).isAtMost(h - AgendaFit.CARD_PADDING * 2 + 0.5f)
            }
        }
    }

    @Test
    fun `the note is drawn whole or not at all`() {
        val tight = WordsFit.plan(spec(159f, 160f, scale = 1.3f).copy(noteLines = 3))
        assertThat(tight.noteLines).isAnyOf(0, 3)
        val long = WordsFit.plan(spec(340f, 293f).copy(noteLines = 4))
        assertThat(long.noteLines).isEqualTo(0)
    }

    @Test
    fun `free has no title, and the time takes the room`() {
        val free = WordsFit.plan(spec(340f, 189f, title = false))
        assertThat(free.titleLines).isEqualTo(0)
        assertThat(free.heroSp).isAtLeast(WordsFit.plan(spec(340f, 189f)).heroSp)
    }
}

package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.LocalDate

/**
 * One line of the «Agenda» card's list (PLANNING.md §7): an event, a date after today heading its
 * events, or what today's place says when nothing is left in it.
 */
sealed interface CardLine {
    /** A date after today, over its events ("Tomorrow", "Thursday 9"). Never the last line drawn. */
    data class Heading(val date: LocalDate) : CardLine

    /**
     * A date's all-day events, on one line ("All day: Design Week, Anna's birthday"): they are the
     * day's context, and on a card of five rows two holidays would push out what happens next.
     */
    data class AllDay(val entries: List<AllDayEntry>, val date: LocalDate) : CardLine

    data class Timed(val entry: TimedEntry, val date: LocalDate) : CardLine

    /** Today has nothing left to list: said in its place, before any day ahead. */
    data class Note(val note: TodayNote) : CardLine

    /** How many events the line stands for: what "N more" counts when it is left out. */
    val events: Int
        get() = when (this) {
            is AllDay -> entries.size
            is Timed -> 1
            is Heading, is Note -> 0
        }

    val isEvent: Boolean get() = events > 0
}

/** What the card says when today's list is empty. */
enum class TodayNote {
    /** Today had timed events and every one is over. */
    NOTHING_LEFT,

    /** Today has no event at all on the card. */
    NOTHING_TODAY,
}

/**
 * What the «Agenda» card lists, in order: the rest of today (its all-day events on one line, then
 * the timed ones under way and to come; past events are not on the card, which is the rest of the day), then,
 * where the reader wants them, the days ahead, each under its heading. The days ahead only take the
 * room today leaves (owner, PLANNING.md §15): [later] is drawn only after the whole of [today].
 */
data class CardAgenda(val today: List<CardLine>, val later: List<CardLine>) {
    val lines: List<CardLine> get() = today + later

    companion object {
        fun of(agenda: Agenda, showAllDay: Boolean, showDaysAhead: Boolean): CardAgenda {
            val first = agenda.days.first()
            val allDay = allDayLine(first, showAllDay)
            val timed = first.timed.filter { it.state != EntryState.PAST }.map { CardLine.Timed(it, first.date) }
            val today = (allDay + timed).ifEmpty {
                val note = if (first.timed.isEmpty()) TodayNote.NOTHING_TODAY else TodayNote.NOTHING_LEFT
                listOf(CardLine.Note(note))
            }
            val later = if (!showDaysAhead) {
                emptyList()
            } else {
                agenda.days.drop(1).flatMap { day ->
                    val events = allDayLine(day, showAllDay) + day.timed.map { CardLine.Timed(it, day.date) }
                    if (events.isEmpty()) emptyList() else listOf(CardLine.Heading(day.date)) + events
                }
            }
            return CardAgenda(today, later)
        }

        private fun allDayLine(day: AgendaDay, show: Boolean): List<CardLine> =
            if (show && day.allDay.isNotEmpty()) listOf(CardLine.AllDay(day.allDay, day.date)) else emptyList()
    }
}

/** The line under the list that counts what did not fit, or none when everything did. */
sealed interface CardFooter {
    /** "2 more today". */
    data class MoreToday(val count: Int) : CardFooter

    /** "3 more in the days ahead": today is whole, the days after it were cut. */
    data class MoreLater(val count: Int) : CardFooter
}

/** How much of a [CardAgenda] a card draws: the first [shown] lines, and the footer. */
data class CardFit(val shown: Int, val footer: CardFooter?)

/**
 * Takes the card's lines in order while they fit [room] (each line's height in [heights], its gaps
 * included), keeping [footerHeight] for the count whenever something is left out. Nothing is ever
 * cut in half: a line fits whole or is counted (VISION.md, "Fits, never scrolls").
 *
 * - A heading is never the last line drawn: a date with none of its events under it says nothing.
 * - The days ahead start only once today is whole; once they have started, what does not fit of
 *   them is counted as "more in the days ahead". When not one of their events fits, they are not
 *   begun, and the card is the rest of today, whole, with nothing to count.
 */
fun fitLines(agenda: CardAgenda, heights: (CardLine) -> Float, room: Float, footerHeight: Float): CardFit {
    val lines = agenda.lines
    if (total(lines, heights) <= room) return CardFit(lines.size, null)
    val today = agenda.today
    if (total(today, heights) > room) {
        // Today is cut: the days ahead are not begun, and today's own remainder is counted.
        val shown = greedy(today, 0, 0f, heights, room - footerHeight)
        return CardFit(shown, CardFooter.MoreToday(today.drop(shown).sumOf { it.events }))
    }
    val shown = greedy(lines, today.size, total(today, heights), heights, room - footerHeight)
    if (lines.subList(today.size, shown).none { it.isEvent }) return CardFit(today.size, null)
    return CardFit(shown, CardFooter.MoreLater(lines.drop(shown).sumOf { it.events }))
}

/** How many of [lines] fit [room], from [from] with [used] already spent, never ending on a heading. */
private fun greedy(lines: List<CardLine>, from: Int, used: Float, heights: (CardLine) -> Float, room: Float): Int {
    var spent = used
    var shown = from
    while (shown < lines.size && spent + heights(lines[shown]) <= room) {
        spent += heights(lines[shown])
        shown++
    }
    while (shown > from && lines[shown - 1] is CardLine.Heading) shown--
    return shown
}

private fun total(lines: List<CardLine>, heights: (CardLine) -> Float): Float =
    lines.fold(0f) { sum, line -> sum + heights(line) }

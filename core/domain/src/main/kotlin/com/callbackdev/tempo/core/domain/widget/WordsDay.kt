package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import java.time.LocalDate

/**
 * What the «In words» card puts large (PLANNING.md §7, VISION.md): the event under way while one
 * is, else the next one today, else (where the reader wants the days ahead) the first one after
 * today, else the day's freedom.
 */
sealed interface WordsFocus {
    /** Running now: "Until 11:00", "Standup". Of two at once, the one that ends first. */
    data class UnderWay(val event: EventInstance) : WordsFocus

    /** The next start, today or on [date] after it: "15:00", "Dentist". */
    data class Next(val event: EventInstance, val date: LocalDate, val today: Boolean) : WordsFocus

    /** Nothing left to come on the card's horizon: "Free". */
    data object Free : WordsFocus
}

/**
 * The line under the focus. Every case is said in clock times or counts, never in minutes from
 * now: a card is redrawn at the agenda's boundaries, not each minute, so "in 40 minutes" would be
 * wrong a minute later (PLANNING.md §15). The words are the widget's resources.
 */
sealed interface WordsNote {
    /** Under way: "Then 2 more today." or "The last one today." */
    data class ThenMore(val count: Int) : WordsNote

    /** Free until the next one: "Free until then; 2 more after it." */
    data class FreeUntilThen(val after: Int) : WordsNote

    /**
     * Today is done or empty: its all-day events where today is the focus ("Free"), else "Nothing
     * left today." or "Nothing on the calendar today."
     */
    data class DayDone(val hadEvents: Boolean, val allDay: List<AllDayEntry>) : WordsNote
}

/**
 * The «In words» card's day, from the agenda alone.
 *
 * @property then the starts after the focus, today, for the line of times on a wide card
 *   ("Then 16:30 · 18:00").
 */
data class WordsDay(val focus: WordsFocus, val note: WordsNote, val then: List<EventInstance>) {
    companion object {
        fun of(agenda: Agenda, showAllDay: Boolean, showDaysAhead: Boolean): WordsDay {
            val today = agenda.days.first()
            val upcoming = today.timed.filter { it.state == EntryState.UPCOMING }.map { it.event }
            val underWay = today.timed.filter { it.state == EntryState.UNDER_WAY }.map { it.event }
            if (underWay.isNotEmpty()) {
                return WordsDay(
                    WordsFocus.UnderWay(
                        underWay.minBy {
                            it.end
                        },
                    ),
                    WordsNote.ThenMore(upcoming.size),
                    upcoming,
                )
            }
            if (upcoming.isNotEmpty()) {
                val next = upcoming.first()
                return WordsDay(
                    WordsFocus.Next(next, today.date, today = true),
                    WordsNote.FreeUntilThen(upcoming.size - 1),
                    upcoming.drop(1),
                )
            }
            val later = if (showDaysAhead) {
                agenda.days.drop(1).firstNotNullOfOrNull { day ->
                    day.timed.firstOrNull {
                        it.startsOnDate
                    }?.let { WordsFocus.Next(it.event, day.date, today = false) }
                }
            } else {
                null
            }
            // Today's all-day events are said only while today is the focus: under tomorrow's
            // first event they would read as tomorrow's.
            val done = WordsNote.DayDone(
                hadEvents = today.timed.isNotEmpty(),
                allDay = if (showAllDay && later == null) today.allDay else emptyList(),
            )
            return WordsDay(later ?: WordsFocus.Free, done, emptyList())
        }
    }
}

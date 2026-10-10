package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Duration
import java.time.Instant

/**
 * Today in one sentence, before any list (PLANNING.md §6): where the day stands
 * now, from the agenda alone. A structure, never a string: the words are resources, in both
 * languages, with plurals, so `:feature:today` and the widgets say it in the reader's language.
 *
 * Each case names what the reader acts on next: the event under way and when it ends, the next
 * one and how long until it, the free time before it, or, with today done, tomorrow's start.
 */
sealed interface TodaySentence {
    /** No timed event today at all (all-day ones may be in their row). */
    data class FreeDay(val tomorrow: Tomorrow) : TodaySentence

    /** Today had events and all of them are over. */
    data class AllDone(val tomorrow: Tomorrow) : TodaySentence

    /** [event] is running; [laterToday] more start after now. */
    data class UnderWay(val event: EventInstance, val laterToday: Int) : TodaySentence

    /** [event] starts within the hour, in [minutes]; [laterToday] more after it. */
    data class Soon(val event: EventInstance, val minutes: Int, val laterToday: Int) : TodaySentence

    /** Nothing until [event], which starts later than an hour from now; [laterToday] more after it. */
    data class FreeUntil(val event: EventInstance, val laterToday: Int) : TodaySentence
}

/** What tomorrow looks like, said when today has nothing left. */
sealed interface Tomorrow {
    /** Tomorrow's first timed event. */
    data class StartsWith(val event: EventInstance) : Tomorrow

    /** No timed event tomorrow. */
    data object Free : Tomorrow

    /** The horizon is today alone: Tempo was asked not to look. */
    data object NotShown : Tomorrow
}

object DaySentence {
    /** How close "soon" is: within it the sentence counts minutes, past it it says the time. */
    val SOON: Duration = Duration.ofHours(1)

    fun today(agenda: Agenda): TodaySentence {
        val today = agenda.days.first()
        val now = agenda.now
        val upcoming = today.timed.filter { it.state == EntryState.UPCOMING }.map { it.event }
        val underWay = today.timed.filter { it.state == EntryState.UNDER_WAY }.map { it.event }
        return when {
            // Of two events running at once, the one that frees the reader first.
            underWay.isNotEmpty() -> TodaySentence.UnderWay(underWay.minBy { it.end }, upcoming.size)

            upcoming.isNotEmpty() -> {
                val next = upcoming.first()
                val wait = Duration.between(now, next.begin)
                if (wait <= SOON) {
                    TodaySentence.Soon(next, minutesUntil(now, next.begin), upcoming.size - 1)
                } else {
                    TodaySentence.FreeUntil(next, upcoming.size - 1)
                }
            }

            today.timed.isEmpty() -> TodaySentence.FreeDay(tomorrow(agenda))

            else -> TodaySentence.AllDone(tomorrow(agenda))
        }
    }

    private fun tomorrow(agenda: Agenda): Tomorrow {
        val day = agenda.days.getOrNull(1) ?: return Tomorrow.NotShown
        val first = day.timed.firstOrNull { it.startsOnDate }?.event ?: return Tomorrow.Free
        return Tomorrow.StartsWith(first)
    }

    /** Rounded up: an event at 10:00 is "in 1 minute" at 09:59:10, never "in 0 minutes". */
    private fun minutesUntil(now: Instant, then: Instant): Int {
        val seconds = Duration.between(now, then).seconds
        return ((seconds + 59) / 60).toInt().coerceAtLeast(1)
    }
}

/**
 * A day after today in a few words (the compact days of the agenda, VISION.md): how many events,
 * from when to when; or its all-day events alone; or nothing.
 *
 * @property first the first start on this date (an event begun the day before starts the day).
 * @property lastEnd the last end on this date, clipped to its midnight.
 */
data class DaySummary(
    val timedCount: Int,
    val allDayCount: Int,
    val first: Instant?,
    val lastEnd: Instant?,
    val firstEventStartsOnDate: Boolean,
) {
    val isEmpty: Boolean get() = timedCount == 0 && allDayCount == 0

    companion object {
        fun of(day: AgendaDay): DaySummary {
            val timed = day.timed
            return DaySummary(
                timedCount = timed.size,
                allDayCount = day.allDay.size,
                first = timed.minOfOrNull { it.event.begin },
                lastEnd = timed.filter { it.endsOnDate }.maxOfOrNull { it.event.end },
                firstEventStartsOnDate = timed.firstOrNull()?.startsOnDate ?: true,
            )
        }
    }
}

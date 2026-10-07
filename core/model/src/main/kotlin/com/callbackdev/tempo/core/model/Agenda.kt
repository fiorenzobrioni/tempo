package com.callbackdev.tempo.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * The days from today to the horizon, as the screens and the cards read them (PLANNING.md §6).
 * Built by `AgendaBuilder` from the instances, the calendars and the reader's choices; never
 * stored.
 *
 * @property today the first of [days], the phone's date at [now].
 * @property underWay the timed events running at [now], in start order.
 * @property next the first timed event to start after [now], within the horizon; null when none.
 */
data class Agenda(
    val now: Instant,
    val today: LocalDate,
    val days: List<AgendaDay>,
    val underWay: List<EventInstance>,
    val next: EventInstance?,
)

/**
 * One date of the agenda.
 *
 * @property allDay the all-day events, and the timed ones that cover the whole date (a conference
 *   from Monday 9:00 to Wednesday 17:00 is "all day" on Tuesday).
 * @property timed the events with a time on this date, in start order, then end, then title.
 * @property free the gaps between the busy events of the date; on today only what is still ahead,
 *   from now. Every gap feeds the day's sentence; [freeRows] are the ones the timeline draws.
 */
data class AgendaDay(
    val date: LocalDate,
    val allDay: List<AllDayEntry>,
    val timed: List<TimedEntry>,
    val free: List<FreeGap>,
) {
    val isEmpty: Boolean get() = allDay.isEmpty() && timed.isEmpty()

    /** The gaps long enough for a row of their own (owner, PLANNING.md §15): an hour or more. */
    val freeRows: List<FreeGap> get() = free.filter { it.duration >= FREE_ROW_MINIMUM }

    companion object {
        val FREE_ROW_MINIMUM: Duration = Duration.ofHours(1)
    }
}

/**
 * An event in a date's all-day row.
 *
 * @property dayNumber which day of the event this date is, from 1 ("day 2 of 5").
 * @property dayCount how many dates the event covers; 1 for a one-day event.
 */
data class AllDayEntry(val event: EventInstance, val dayNumber: Int, val dayCount: Int)

/**
 * An event with a time, on one date.
 *
 * @property startsOnDate false on the second date of an event that crossed midnight: it reads
 *   "until 01:30" there.
 * @property endsOnDate false on the first date of one that crosses it: "from 23:00".
 */
data class TimedEntry(
    val event: EventInstance,
    val startsOnDate: Boolean,
    val endsOnDate: Boolean,
    val state: EntryState,
)

/** Where an event stands against now (PLANNING.md §4.3). */
enum class EntryState {
    PAST,
    UNDER_WAY,
    UPCOMING,
}

/** A stretch of free time between two busy events, [start] inclusive, [end] exclusive. */
data class FreeGap(val start: Instant, val end: Instant) {
    val duration: Duration get() = Duration.between(start, end)
}

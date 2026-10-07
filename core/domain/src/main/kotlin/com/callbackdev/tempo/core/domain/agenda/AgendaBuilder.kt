package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.AllDayEntry
import com.callbackdev.tempo.core.model.AttendeeStatus
import com.callbackdev.tempo.core.model.Availability
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Turns the provider's instances into the agenda the screens and the cards read (PLANNING.md §4.2
 * to §4.4, §6). Pure: the same instances, choices, moment and zone give the same agenda, which is
 * what lets every edge case of §4.6 be a test.
 *
 * - All-day events are placed by their UTC dates ([allDayDates]); timed events by their instants
 *   in [zone], the phone's zone now.
 * - A timed event belongs to every date it touches; one that covers a whole date is in that
 *   date's all-day row ("day 2 of 3"), its first and last dates keep it with its time.
 * - Cancelled events never, declined ones and hidden calendars by the reader's choice
 *   ([AgendaFilter]).
 */
object AgendaBuilder {
    fun build(
        instances: List<EventInstance>,
        calendars: List<CalendarInfo>,
        filter: AgendaFilter,
        now: Instant,
        zone: ZoneId,
        days: Int,
    ): Agenda {
        require(days >= 1) { "An agenda has at least today" }
        val byId = calendars.associateBy { it.id }
        val shown = instances.filter { filter.shows(it, byId) }
        val today = LocalDate.ofInstant(now, zone)
        val horizonEnd = startOf(today.plusDays(days.toLong()), zone)
        val timed = shown.filterNot { it.allDay }
        return Agenda(
            now = now,
            today = today,
            days = (0 until days).map { day(today.plusDays(it.toLong()), shown, now, zone, today) },
            underWay = timed.filter { it.begin <= now && now < it.end }.sortedWith(Order),
            next = timed.filter { it.begin > now && it.begin < horizonEnd }.minWithOrNull(Order),
        )
    }

    private fun day(
        date: LocalDate,
        shown: List<EventInstance>,
        now: Instant,
        zone: ZoneId,
        today: LocalDate,
    ): AgendaDay {
        val start = startOf(date, zone)
        val end = startOf(date.plusDays(1), zone)
        val allDay = mutableListOf<AllDayEntry>()
        val timed = mutableListOf<TimedEntry>()
        for (event in shown) {
            if (event.allDay) {
                val dates = event.allDayDates()
                if (date in
                    dates
                ) {
                    allDay +=
                        AllDayEntry(event, dayNumber(dates.start, date), dayNumber(dates.start, dates.endInclusive))
                }
                continue
            }
            if (!touches(event, start, end)) continue
            if (event.begin <= start && event.end >= end) {
                // A whole date inside a longer timed event: all day here, with its place in the run.
                val first = LocalDate.ofInstant(event.begin, zone)
                val last = LocalDate.ofInstant(event.end.minusNanos(1), zone)
                allDay += AllDayEntry(event, dayNumber(first, date), dayNumber(first, last))
            } else {
                timed += TimedEntry(event, event.begin >= start, event.end <= end, stateOf(event, now))
            }
        }
        val busy = timed
            .filter { it.event.availability != Availability.FREE && it.event.selfStatus != AttendeeStatus.DECLINED }
            .map { maxOf(it.event.begin, start) to minOf(it.event.end, end) }
        return AgendaDay(
            date = date,
            allDay = allDay.sortedWith(
                compareBy<AllDayEntry> {
                    it.event.begin
                }.thenBy { it.event.title }.thenBy { it.event.eventId },
            ),
            timed = timed.sortedWith(compareBy(Order) { it.event }),
            free = FreeTime.gaps(busy, from = if (date == today) now else null),
        )
    }

    /** Half-open overlap; a zero-length event belongs to the date its moment falls in. */
    private fun touches(event: EventInstance, start: Instant, end: Instant): Boolean = if (event.begin == event.end) {
        event.begin >= start && event.begin < end
    } else {
        event.begin < end && event.end > start
    }

    private fun stateOf(event: EventInstance, now: Instant): EntryState = when {
        event.begin <= now && now < event.end -> EntryState.UNDER_WAY
        event.end <= now -> EntryState.PAST
        else -> EntryState.UPCOMING
    }

    private fun dayNumber(first: LocalDate, date: LocalDate): Int = ChronoUnit.DAYS.between(first, date).toInt() + 1

    /** A date's first moment in [zone]: midnight, or whatever the zone has instead on a DST night. */
    private fun startOf(date: LocalDate, zone: ZoneId): Instant = date.atStartOfDay(zone).toInstant()

    /** Start, then end, then title, then id, so equal times never shuffle between two reads. */
    private val Order: Comparator<EventInstance> =
        compareBy<EventInstance> { it.begin }.thenBy { it.end }.thenBy { it.title }.thenBy { it.eventId }
}

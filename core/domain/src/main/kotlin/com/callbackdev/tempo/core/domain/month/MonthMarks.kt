package com.callbackdev.tempo.core.domain.month

import com.callbackdev.tempo.core.domain.today.EventKey
import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EventInstance
import java.time.LocalDate
import java.time.ZoneId

/**
 * What a date says of itself on the month, at a glance and with no word (PLANNING.md §11 Phase 9):
 * marks, never titles. A title in a cell is a calendar app's month; a mark is the answer to "which
 * days have something on".
 *
 * Two kinds of mark, because two kinds of thing fill a day. An event with a time is a **dot**. An
 * event that takes whole dates (a birthday, a holiday, a trip; a conference from Monday morning
 * to Wednesday evening) is a **bar** under the dates it covers, one line across them, so a week
 * away reads as a week and not as seven days that each have something.
 *
 * @property lanes the bars under this date, one place per lane ([MonthMarks.LANES]); null where
 *   the lane is empty here.
 * @property dots the colours of the date's events with a time, in the day's order, at most
 *   [MonthMarks.DOTS]; a null colour is drawn in the page's quiet ink.
 * @property more whether the date holds more than its marks show: more timed events than dots, or
 *   a span of days with no lane left for it.
 * @property events how many events the date has in all, for what a screen reader says of it.
 */
data class DayMarks(val lanes: List<DayBar?>, val dots: List<Int?>, val more: Boolean, val events: Int) {
    val isEmpty: Boolean get() = events == 0
}

/**
 * A span's bar under one of its dates.
 *
 * @property startsHere whether the span's first date is this one: its end is rounded here, and
 *   square where it comes from the date before.
 * @property endsHere the same for its last date.
 */
data class DayBar(val color: Int?, val startsHere: Boolean, val endsHere: Boolean)

object MonthMarks {
    /** Two spans side by side are common (a holiday inside a trip); a third is said by [DayMarks.more]. */
    const val LANES = 2

    /** One, two, three, many: past three, a count read at a glance is no longer a count. */
    const val DOTS = 3

    /**
     * The marks of every date of [days], which follow one another. A span keeps its lane on all
     * its dates, so its bar is one line: the lanes are given out over the whole run, the span that
     * starts first taking the first free one, the longer first among those that start together.
     *
     * @param zone the phone's zone, in which a timed event's dates are read.
     */
    fun of(days: List<AgendaDay>, calendars: Map<Long, CalendarInfo>, zone: ZoneId): Map<LocalDate, DayMarks> {
        val spans = spans(days, zone)
        val lanes = lanes(spans.values)
        return days.associate { day ->
            val here = spans.values.filter { day.date in it.first..it.last }
            val moments = day.timed.filter { EventKey.of(it.event) !in spans }
            day.date to DayMarks(
                lanes = (0 until LANES).map { lane ->
                    here.firstOrNull { lanes[it.key] == lane }?.let { span ->
                        DayBar(colorOf(span.event, calendars), day.date == span.first, day.date == span.last)
                    }
                },
                dots = moments.take(DOTS).map { colorOf(it.event, calendars) },
                more = moments.size > DOTS || here.any { (lanes[it.key] ?: LANES) >= LANES },
                events = day.allDay.size + day.timed.size,
            )
        }
    }

    /** An event that takes whole dates, and the dates it is on, first and last. */
    private data class Span(val key: EventKey, val event: EventInstance, val first: LocalDate, val last: LocalDate)

    /**
     * The spans among [days]' events: every event in a date's all-day row (an all-day event, or a
     * timed one that covers the date), on all its dates, the first and the last of a timed one
     * included, where it has a time: the conference is one bar from Monday to Wednesday.
     */
    private fun spans(days: List<AgendaDay>, zone: ZoneId): Map<EventKey, Span> {
        val spans = mutableMapOf<EventKey, Span>()
        for (day in days) {
            for (entry in day.allDay) {
                val first = day.date.minusDays(entry.dayNumber - 1L)
                val key = EventKey.of(entry.event)
                spans.getOrPut(key) { Span(key, entry.event, first, first.plusDays(entry.dayCount - 1L)) }
            }
        }
        // A timed span's first or last date at the edge of the run, its whole dates beyond it.
        for (day in days) {
            for (entry in day.timed) {
                val key = EventKey.of(entry.event)
                if (key in spans) continue
                val dates = wholeDates(entry.event, zone) ?: continue
                spans[key] = Span(key, entry.event, dates.start, dates.endInclusive)
            }
        }
        return spans
    }

    /**
     * The dates a timed event is on, if it covers one of them from midnight to midnight; null for
     * an event that does not, however long: a night that crosses midnight is two dots.
     */
    private fun wholeDates(event: EventInstance, zone: ZoneId): ClosedRange<LocalDate>? {
        if (event.begin == event.end) return null
        val first = LocalDate.ofInstant(event.begin, zone)
        val last = LocalDate.ofInstant(event.end.minusNanos(1), zone)
        val whole = generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.any { date ->
            event.begin <= date.atStartOfDay(zone).toInstant() &&
                event.end >= date.plusDays(1).atStartOfDay(zone).toInstant()
        }
        return if (whole) first..last else null
    }

    /** Each span's lane, the lowest free one in the order of [SpanOrder]; past [LANES] it is not drawn. */
    private fun lanes(spans: Collection<Span>): Map<EventKey, Int> {
        val taken = mutableListOf<LocalDate>()
        return spans.sortedWith(SpanOrder).associate { span ->
            val free = taken.indexOfFirst { it < span.first }
            val lane = if (free >= 0) free else taken.size
            if (lane < taken.size) taken[lane] = span.last else taken += span.last
            span.key to lane
        }
    }

    private val SpanOrder: Comparator<Span> =
        compareBy<Span> { it.first }.thenByDescending { it.last }.thenBy { it.event.title }.thenBy { it.event.eventId }

    private fun colorOf(event: EventInstance, calendars: Map<Long, CalendarInfo>): Int? =
        event.color ?: calendars[event.calendarId]?.color
}

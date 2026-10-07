package com.callbackdev.tempo.core.domain.today

import com.callbackdev.tempo.core.model.AgendaDay
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.FreeGap
import com.callbackdev.tempo.core.model.TimedEntry
import java.time.Instant

/** One row of a day's timeline. */
sealed interface TimelineItem {
    data class Event(val entry: TimedEntry) : TimelineItem

    /** A gap of an hour or more (owner, PLANNING.md §15): a row the reader can turn into an event. */
    data class Free(val gap: FreeGap) : TimelineItem

    /**
     * Today's events already over: folded into this one row, or, [open], this row (to fold them
     * back) followed by each of them.
     */
    data class Earlier(val entries: List<TimedEntry>, val open: Boolean = false) : TimelineItem

    /** The line between what has been and what is ahead, on today only. */
    data object Now : TimelineItem
}

/**
 * The rows of one date's timeline (PLANNING.md §6, VISION.md, Today), in one column:
 *
 * - On today, the events already over come first, quieter; two or more fold into one "earlier"
 *   row unless [showEarlier] (then the row stays above them, to fold them back), so the morning
 *   never pushes what is ahead below the fold.
 * - Then "now", when anything is still ahead: the line the eye looks for.
 * - Then what is under way and ahead, with the free gaps of an hour or more between them, by
 *   start time. A gap on today starts at now, so it sits right under the line.
 * - Free gaps are rows on today only: free time is something to act on now (a touch on the row
 *   starts a new event there); on tomorrow's timeline five rows of "free" read as noise (Phase 3's
 *   screenshots). The sentence and the days' summaries still count every gap.
 */
object Timeline {
    /** Past events kept as rows of their own up to this many; more fold into one. */
    const val EARLIER_FOLD = 2

    fun of(day: AgendaDay, isToday: Boolean, showEarlier: Boolean = false): List<TimelineItem> {
        val past = if (isToday) day.timed.filter { it.state == EntryState.PAST } else emptyList()
        val ahead = day.timed - past.toSet()
        val rest = (
            ahead.map { it.event.begin to TimelineItem.Event(it) } +
                (if (isToday) day.freeRows else emptyList()).map { it.start to TimelineItem.Free(it) }
            )
            .sortedWith(compareBy<Pair<Instant, TimelineItem>> { it.first }.thenBy { it.second is TimelineItem.Free })
            .map { it.second }
        return buildList {
            if (past.size >= EARLIER_FOLD) add(TimelineItem.Earlier(past, open = showEarlier))
            if (past.size < EARLIER_FOLD || showEarlier) past.forEach { add(TimelineItem.Event(it)) }
            if (isToday && rest.isNotEmpty()) add(TimelineItem.Now)
            addAll(rest)
        }
    }
}

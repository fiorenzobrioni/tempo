package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.FreeGap
import java.time.Instant

/**
 * The free time of a date, between its busy events (PLANNING.md §6).
 *
 * Only the time between two events counts, plus, on today, the time from now to the next one:
 * the evening after the last meeting is not "free time" any more than the night is. Overlapping
 * events are merged first, so a gap is never found inside a busy stretch.
 */
object FreeTime {
    /**
     * @param busy the busy stretches of the date, clipped to it, in any order.
     * @param from today's now: gaps before it are gone and the one it falls in starts at it, and
     *   the time from it to the first busy stretch ahead is a gap too. Null for any other date.
     */
    fun gaps(busy: List<Pair<Instant, Instant>>, from: Instant? = null): List<FreeGap> {
        val merged = merge(busy)
        if (merged.isEmpty()) return emptyList()
        val gaps = mutableListOf<FreeGap>()
        if (from != null && from < merged.first().first) gaps += FreeGap(from, merged.first().first)
        for ((before, after) in merged.zipWithNext()) {
            val start = if (from != null && from > before.second) from else before.second
            if (after.first > start) gaps += FreeGap(start, after.first)
        }
        return gaps
    }

    private fun merge(busy: List<Pair<Instant, Instant>>): List<Pair<Instant, Instant>> {
        val sorted = busy.filter { it.second > it.first }.sortedBy { it.first }
        val merged = mutableListOf<Pair<Instant, Instant>>()
        for (stretch in sorted) {
            val last = merged.lastOrNull()
            if (last != null && stretch.first <= last.second) {
                merged[merged.lastIndex] = last.first to maxOf(last.second, stretch.second)
            } else {
                merged += stretch
            }
        }
        return merged
    }
}

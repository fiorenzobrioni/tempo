package com.callbackdev.tempo.core.domain.agenda

import com.callbackdev.tempo.core.model.EventInstance
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The dates an all-day event covers, first and last inclusive, read in **UTC** (PLANNING.md §4.2).
 *
 * The provider stores an all-day event from 00:00 UTC of its first date to 00:00 UTC of the date
 * after its last. Read in the phone's zone it would start the evening before west of Greenwich,
 * which is the bug this function exists to make impossible. A malformed event whose end is not
 * after its begin covers its first date alone.
 */
fun EventInstance.allDayDates(): ClosedRange<LocalDate> {
    val first = LocalDate.ofInstant(begin, ZoneOffset.UTC)
    val afterLast = LocalDate.ofInstant(end, ZoneOffset.UTC)
    val last = if (afterLast.isAfter(first)) afterLast.minusDays(1) else first
    return first..last
}

package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.domain.agenda.EventZone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The second clock (VISION.md, the clock; PLANNING.md §11, step 8.5): a city the reader chose in
 * Settings, written beside its time ("New York 04:20").
 */
object SecondZone {
    /**
     * The zone the second clock shows for the stored [id], or null where it would say nothing the
     * first clock does not: none chosen, an id this phone's time-zone data does not know (a backup
     * from a newer phone), a zone with no city, or the phone's own (the reader travelled there; an
     * old name for it, "Asia/Calcutta" beside "Asia/Kolkata", is the same clock).
     */
    fun of(id: String?, phone: ZoneId): ZoneId? {
        val zone = id?.let(EventZone::place) ?: return null
        return zone.takeUnless { it == phone || it.rules == phone.rules }
    }

    /**
     * How far the second clock's date is from the phone's at [now]: 1 is "tomorrow" (Tokyo, from
     * Rome's evening), -1 "yesterday" (New York, from Rome's early morning), 0 the same date.
     */
    fun daysAhead(now: Instant, phone: ZoneId, zone: ZoneId): Long =
        ChronoUnit.DAYS.between(LocalDate.ofInstant(now, phone), LocalDate.ofInstant(now, zone))
}

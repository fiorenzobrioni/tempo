package com.callbackdev.tempo.core.domain.today

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Which day the phone's next alarm rings, in words the clock's line can use: "Alarm at 7:00",
 * "Alarm tomorrow at 7:00", "Alarm Friday at 7:00" (VISION.md, the next alarm). Read from the
 * system's next alarm clock, which needs no permission; only the alarm the user set, never an
 * app's own timers.
 */
sealed interface AlarmDay {
    data object Today : AlarmDay

    data object Tomorrow : AlarmDay

    /** Further than tomorrow: the line names the day. */
    data class On(val date: LocalDate) : AlarmDay

    companion object {
        fun of(alarm: Instant, now: Instant, zone: ZoneId): AlarmDay {
            val today = LocalDate.ofInstant(now, zone)
            return when (val date = LocalDate.ofInstant(alarm, zone)) {
                today -> Today
                today.plusDays(1) -> Tomorrow
                else -> On(date)
            }
        }
    }
}

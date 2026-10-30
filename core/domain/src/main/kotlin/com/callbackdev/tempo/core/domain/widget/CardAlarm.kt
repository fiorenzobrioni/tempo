package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.domain.today.AlarmDay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The phone's next alarm as a card says it (PLANNING.md §7, the owner's choices of 30 Oct 2026):
 * only when it rings within the next [WITHIN], since an alarm on Monday seen on Friday is noise;
 * a clock time, and its day when it is tomorrow's ("07:00", "Tomorrow 07:00"), never "in 9 hours",
 * which a card redrawn at its boundaries would leave wrong a minute later. Tempo only reads the
 * alarm (`AlarmManager.getNextAlarmClock`, no permission): the clock app sets, rings and moves it.
 *
 * Within a day of now a clock time names one moment only, so the time alone is still true where
 * the card has no room for the day.
 */
data class CardAlarm(val at: Instant, val day: AlarmDay) {
    companion object {
        val WITHIN: Duration = Duration.ofHours(24)

        /** [next] as a card shows it at [now], or null when there is none or it is further than [WITHIN]. */
        fun of(next: Instant?, now: Instant, zone: ZoneId): CardAlarm? {
            if (next == null || next <= now || Duration.between(now, next) > WITHIN) return null
            return CardAlarm(next, AlarmDay.of(next, now, zone))
        }

        /**
         * The moments after [now] the card's alarm changes with nothing set in the clock app: it
         * comes onto the card [WITHIN] before it rings; tomorrow's becomes today's at midnight; it
         * rings and leaves. A moved or cancelled alarm is the system's broadcast's to tell.
         */
        fun changes(next: Instant?, now: Instant, zone: ZoneId): List<Instant> {
            val shown = of(next, now, zone) ?: return listOfNotNull(next?.minus(WITHIN)?.takeIf { it > now })
            val midnight = LocalDate.ofInstant(now, zone).plusDays(1).atStartOfDay(zone).toInstant()
            return listOfNotNull(midnight.takeIf { shown.day == AlarmDay.Tomorrow }, shown.at)
        }
    }
}

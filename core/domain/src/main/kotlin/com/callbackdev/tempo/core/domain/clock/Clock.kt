package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.model.ClockFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Whether a time is written on the 24-hour clock: the reader's choice, or the phone's when the
 * reader left it to the phone ([ClockFormat.SYSTEM], the default). The phone's answer is read by
 * the caller (`DateFormat.is24HourFormat`), so this stays pure.
 */
fun ClockFormat.uses24Hour(phoneUses24Hour: Boolean): Boolean = when (this) {
    ClockFormat.SYSTEM -> phoneUses24Hour
    ClockFormat.H24 -> true
    ClockFormat.H12 -> false
}

/**
 * How long until the minute after [now] begins: what a screen-on ticker waits before it redraws
 * the clock, so the minutes change on the minute and not up to 59 seconds late (PLANNING.md §9:
 * a ticker runs only while the page is visible). Never zero: at a minute's exact start the next
 * one is a full minute away.
 */
fun untilNextMinute(now: Instant): Duration {
    val next = now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
    return Duration.between(now, next)
}

/**
 * Where a new event starts by default (PLANNING.md §4.7): the next half hour after [now] in
 * [zone], strictly after it, so a reader at 10:00 sharp is offered 10:30 and never a time already
 * gone by the time the calendar app opens.
 */
fun nextHalfHour(now: Instant, zone: ZoneId): Instant {
    val local = now.atZone(zone).truncatedTo(ChronoUnit.MINUTES)
    val minutesPast = local.minute % 30
    return local.plusMinutes((30 - minutesPast).toLong()).toInstant()
}

package com.callbackdev.tempo.core.domain.clock

import com.callbackdev.tempo.core.model.ClockFormat
import com.callbackdev.tempo.core.model.FreeGap
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

/**
 * The new event a touch on a free gap hands to the calendar app (PLANNING.md §4.7): from the gap's
 * start, which is an event's end ("free from 11:00"), for [length] or to the gap's end, whichever
 * comes first. The gap that is open now starts at [now] itself, seconds and all ("8:07:41"): its
 * event starts at the next half hour instead, as the new-event button's does, while that is still
 * inside the gap (review of 30 Oct 2026).
 */
fun newEventIn(gap: FreeGap, now: Instant, zone: ZoneId, length: Duration): FreeGap {
    val start = if (gap.start > now) {
        gap.start
    } else {
        nextHalfHour(now, zone).takeIf { it < gap.end } ?: now.atZone(zone).truncatedTo(ChronoUnit.MINUTES).toInstant()
    }
    return FreeGap(start, minOf(start.plus(length), gap.end))
}

/**
 * Where a new event starts on [date], chosen on the month (PLANNING.md §4.7). Today, the next half
 * hour, as the new-event button's. On another date the same time of day, as calendar apps propose
 * it: the reader planning at ten in the morning is thinking of a morning. Late in the evening that
 * time falls on the date after (23:40 gives 00:00): then [DEFAULT_START], and so where the date has
 * no such time (the hour a clock skips in spring).
 */
fun newEventOn(date: LocalDate, now: Instant, zone: ZoneId): Instant {
    val today = now.atZone(zone).toLocalDate()
    val next = nextHalfHour(now, zone).atZone(zone)
    if (date == today) return next.toInstant()
    val time = if (next.toLocalDate() == today) next.toLocalTime() else DEFAULT_START
    val start = date.atTime(time).atZone(zone)
    return (if (start.toLocalTime() == time) start else date.atTime(DEFAULT_START).atZone(zone)).toInstant()
}

/** A new event's start on a date with no better one to offer: the morning. */
val DEFAULT_START: LocalTime = LocalTime.of(9, 0)

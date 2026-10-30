package com.callbackdev.tempo.core.domain.widget

import com.callbackdev.tempo.core.model.Agenda
import com.callbackdev.tempo.core.model.CalendarInfo
import com.callbackdev.tempo.core.model.EntryState
import com.callbackdev.tempo.core.model.EventInstance
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * One event on the «In words» dial: where it starts on a twelve-hour face, in minutes from twelve
 * o'clock (0 until 720), and how long it is there, in minutes. [focus] is the event the card's words
 * are about, drawn in the accent; the others are drawn quiet.
 *
 * [color] is the event's calendar colour (its own, else its calendar's; null when neither is
 * known). Today's face draws a quiet arc in it, as a mark, so the ring says which calendar each
 * event is, as the list's bars do; the cards keep their inks on their six grounds (PLANNING.md §15,
 * 30 Oct 2026).
 */
data class DialArc(val from: Float, val sweep: Float, val focus: Boolean, val color: Int? = null)

/**
 * The events the dial carries (PLANNING.md §7): the focus and whatever else is still to come in
 * the twelve hours the face can show, as arcs on its ring. The hands are the system's
 * `AnalogClock` and move on their own; the arcs change only where an event starts or ends, which
 * are the boundaries the card is redrawn at anyway. So the hour hand walks into a meeting's arc,
 * through it and out of it with no work by Tempo, and the dial is never wrong between redraws.
 *
 * A face shows twelve hours, so the window is cut short of them ([SPAN]): an arc that reached all
 * the way round would meet the hand from behind and read as now. It opens at the moment drawn,
 * or at the start of the event under way (up to [LOOK_BACK] before): the hand stands inside its
 * arc. What starts beyond the window is left off, never wrapped round onto the hours before it.
 *
 * The face is the clock on the wall, so the window and the arcs are measured in its time, not in
 * elapsed time: the night the clocks go forward, eleven real hours fill twelve on the face, and
 * an event across the jump ends on the face where the hand will be when it ends (Europe/Rome,
 * 28 March 2027). The night they go back, the hand walks an hour twice and an arc is an hour
 * shorter than the event. Measured in elapsed time, the window reached past the hand on the
 * first night, and an event at the same hour next morning read as now.
 */
object DialArcs {
    fun of(
        agenda: Agenda,
        focus: WordsFocus,
        zone: ZoneId,
        showDaysAhead: Boolean,
        calendars: Map<Long, CalendarInfo> = emptyMap(),
    ): List<DialArc> {
        val focused = when (focus) {
            is WordsFocus.UnderWay -> focus.event
            is WordsFocus.Next -> focus.event
            WordsFocus.Free -> null
        }
        val opens = if (focus is WordsFocus.UnderWay) {
            maxOf(focus.event.begin, agenda.now.minus(LOOK_BACK))
        } else {
            agenda.now
        }
        val today = agenda.days.firstOrNull()
        val events = buildList {
            today?.timed?.filter { it.state != EntryState.PAST }?.forEach { add(it.event) }
            if (showDaysAhead) agenda.days.drop(1).forEach { day -> day.timed.forEach { add(it.event) } }
            focused?.let { add(it) }
        }.distinctBy { it.eventId to it.begin }
        return events.mapNotNull { event ->
            arc(
                event,
                opens,
                zone,
                focus = event.sameAs(focused),
                color =
                event.color ?: calendars[event.calendarId]?.color,
            )
        }.sortedBy { it.focus }
    }

    private fun arc(event: EventInstance, opens: Instant, zone: ZoneId, focus: Boolean, color: Int?): DialArc? {
        val end = maxOf(event.end, event.begin)
        if (end < opens || (end == opens && event.begin < opens)) return null
        val from = maxOf(event.begin, opens)
        // In the hour the clocks repeat, a later moment can be earlier on the face: never below 0.
        val ahead = faceMinutesBetween(opens, from, zone).coerceAtLeast(0f)
        if (ahead >= SPAN_MINUTES) return null
        val minutes = minOf(faceMinutesBetween(from, end, zone), SPAN_MINUTES - ahead)
        return DialArc(minuteOnFace(from, zone), maxOf(minutes, MIN_SWEEP), focus, color)
    }

    /** How far the hand moves from [from] to [to]: wall-clock minutes, not elapsed ones. */
    private fun faceMinutesBetween(from: Instant, to: Instant, zone: ZoneId): Float =
        Duration.between(from.atZone(zone).toLocalDateTime(), to.atZone(zone).toLocalDateTime())
            .toSeconds() / SECONDS_PER_MINUTE

    private fun EventInstance.sameAs(other: EventInstance?) =
        other != null && eventId == other.eventId && begin == other.begin

    /**
     * When an event starting at [moment] comes onto a face drawn then: [SPAN] before it on the
     * wall clock, and a minute more, so a face drawn at that moment has it inside its window.
     */
    fun comesOnTheFace(moment: Instant, zone: ZoneId): Instant =
        moment.atZone(zone).toLocalDateTime().minus(SPAN).plusMinutes(1).atZone(zone).toInstant()

    /** Where [moment] falls on a twelve-hour face, in minutes from twelve o'clock. */
    fun minuteOnFace(moment: Instant, zone: ZoneId): Float {
        val local = moment.atZone(zone)
        return (local.hour % HOURS_ON_FACE) * MINUTES_PER_HOUR + local.minute + local.second / SECONDS_PER_MINUTE
    }

    /** The face's twelve hours, less the gap that keeps an arc from closing on the hand. */
    val SPAN: Duration = Duration.ofHours(11).plusMinutes(30)

    private val SPAN_MINUTES: Float = SPAN.toMinutes().toFloat()

    /** How far back the arc of the event under way may start: the rest of the face is the hours to come. */
    val LOOK_BACK: Duration = Duration.ofHours(6)

    /** A zero-length event, or one that is nearly over, still shows as a short arc. */
    const val MIN_SWEEP: Float = 6f

    const val FACE_MINUTES: Float = 720f
    private const val HOURS_ON_FACE = 12
    private const val MINUTES_PER_HOUR = 60f
    private const val SECONDS_PER_MINUTE = 60f
}

/**
 * The dial's proportions, as shares of its side: one drawing for the card (`WidgetDial`, where
 * the hands are the system's drawables cut to these numbers) and for Today's face (`DayDial`,
 * drawn in Compose), so the two read as the same clock.
 */
object DialGeometry {
    const val STROKE: Float = 0.085f
    const val DOT_RADIUS: Float = 0.38f
    const val DOT_SIZE: Float = 0.016f
    const val HOUR_LENGTH: Float = 0.24f
    const val HOUR_WIDTH: Float = 0.07f
    const val MINUTE_LENGTH: Float = 0.33f
    const val MINUTE_WIDTH: Float = 0.05f
    const val CAP: Float = 0.045f
}

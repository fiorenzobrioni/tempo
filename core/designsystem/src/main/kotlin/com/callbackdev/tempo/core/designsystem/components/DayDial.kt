package com.callbackdev.tempo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.callbackdev.tempo.core.designsystem.theme.TempoMotion
import com.callbackdev.tempo.core.designsystem.theme.reducedMotion
import com.callbackdev.tempo.core.domain.widget.DialArc
import com.callbackdev.tempo.core.domain.widget.DialArcs
import com.callbackdev.tempo.core.domain.widget.DialGeometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The «In words» card's dial, on Today (PLANNING.md §15, 9 Oct 2026): a clock face in the family's
 * ring, with the next twelve hours' events as arcs on it ([DialArcs]) and the hands at [faceMinutes]
 * (minutes from twelve o'clock, 0 until 720). The same geometry as the card's ([DialGeometry]), so
 * the home screen and the app show one clock.
 *
 * When the face appears the hands wind forward from twelve to now and the arcs draw themselves,
 * together, in one short movement; after that the hands move with the page's minute ticker, on
 * a spring, always forward (never back round the face at the hour). Under reduced motion every
 * movement is a plain jump. A picture to a screen reader: [description] says what it shows.
 */
@Composable
fun DayDial(faceMinutes: Float, arcs: List<DialArc>, description: String, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        if (reduced) sweep.snapTo(1f) else sweep.animateTo(1f, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
    }
    val hour = rememberHand(faceMinutes / DialArcs.FACE_MINUTES, reduced)
    val minute = rememberHand((faceMinutes % MINUTES_PER_HOUR) / MINUTES_PER_HOUR, reduced)
    val scheme = MaterialTheme.colorScheme
    val inks = DialInks(
        track = scheme.onSurface.copy(alpha = TRACK_ALPHA),
        focus = scheme.primary,
        other = scheme.onSurface.copy(alpha = OTHER_ALPHA),
        dots = scheme.onSurface.copy(alpha = DOTS_ALPHA),
        hands = scheme.onSurface,
    )
    Canvas(modifier.aspectRatio(1f).clearAndSetSemantics { contentDescription = description }) {
        drawFace(arcs, sweep.value, inks)
        drawHands(hour.value, minute.value, inks.hands)
    }
}

/**
 * A hand's position in turns, animated and never unwound: a target that passed twelve is reached
 * by going on round, so the minute hand at the hour turns through twelve rather than back.
 * The first target is reached from twelve, with the arcs' entrance.
 */
@Composable
private fun rememberHand(turns: Float, reduced: Boolean): Animatable<Float, *> {
    val hand = remember { Animatable(0f) }
    var unwound by remember { mutableFloatStateOf(0f) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(turns, reduced) {
        val forward = ((turns - unwound % 1f) + 1f) % 1f
        unwound += forward
        when {
            reduced -> hand.snapTo(unwound)
            !entered -> hand.animateTo(unwound, tween(ENTRANCE_MILLIS, easing = FastOutSlowInEasing))
            else -> hand.animateTo(unwound, TempoMotion.spatial())
        }
        entered = true
    }
    return hand
}

private class DialInks(val track: Color, val focus: Color, val other: Color, val dots: Color, val hands: Color)

private fun DrawScope.drawFace(arcs: List<DialArc>, progress: Float, inks: DialInks) {
    val side = size.minDimension
    val stroke = side * DialGeometry.STROKE
    val inset = stroke / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val topLeft = Offset(center.x - side / 2f + inset, center.y - side / 2f + inset)
    val oval = Size(side - stroke, side - stroke)
    drawArc(inks.track, 0f, FULL_TURN, useCenter = false, topLeft = topLeft, size = oval, style = Stroke(stroke))
    // A round cap reaches half the stroke past each end: the arc is shortened by as much, so it
    // begins and ends where the event does, down to a dot for a moment.
    val capDegrees = (inset / (side / 2f - inset)) * DEGREES_PER_RADIAN
    val round = Stroke(stroke, cap = StrokeCap.Round)
    arcs.forEach { arc ->
        val start = arc.from / DialArcs.FACE_MINUTES * FULL_TURN - QUARTER_TURN + capDegrees
        val sweep = (arc.sweep * progress / DialArcs.FACE_MINUTES * FULL_TURN - capDegrees * 2).coerceAtLeast(MIN_SWEEP)
        val color = if (arc.focus) inks.focus else inks.other
        drawArc(color, start, sweep, useCenter = false, topLeft = topLeft, size = oval, style = round)
    }
    repeat(QUARTERS) { quarter ->
        val angle = quarter * QUARTER_TURN * RADIANS_PER_DEGREE
        drawCircle(
            color = inks.dots,
            radius = maxOf(side * DialGeometry.DOT_SIZE, density),
            center = Offset(
                center.x + side * DialGeometry.DOT_RADIUS * sin(angle),
                center.y - side * DialGeometry.DOT_RADIUS * cos(angle),
            ),
        )
    }
}

private fun DrawScope.drawHands(hourTurns: Float, minuteTurns: Float, ink: Color) {
    val side = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    fun hand(turns: Float, length: Float, width: Float) {
        val angle = turns * FULL_TURN * RADIANS_PER_DEGREE
        // The rounded end sits inside the length, as the card's drawables: the line stops a half width short.
        val reach = side * length - side * width / 2f
        drawLine(
            color = ink,
            start = center,
            end = Offset(center.x + reach * sin(angle), center.y - reach * cos(angle)),
            strokeWidth = side * width,
            cap = StrokeCap.Round,
        )
    }
    hand(hourTurns, DialGeometry.HOUR_LENGTH, DialGeometry.HOUR_WIDTH)
    hand(minuteTurns, DialGeometry.MINUTE_LENGTH, DialGeometry.MINUTE_WIDTH)
    drawCircle(ink, radius = side * DialGeometry.CAP, center = center)
}

private const val ENTRANCE_MILLIS = 700
private const val MINUTES_PER_HOUR = 60f
private const val TRACK_ALPHA = 0.14f
private const val OTHER_ALPHA = 0.45f
private const val DOTS_ALPHA = 0.45f
private const val FULL_TURN = 360f
private const val QUARTER_TURN = 90f
private const val QUARTERS = 4
private const val MIN_SWEEP = 0.1f
private const val DEGREES_PER_RADIAN = (180 / PI).toFloat()
private const val RADIANS_PER_DEGREE = (PI / 180).toFloat()

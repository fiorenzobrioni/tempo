package com.callbackdev.tempo.widget

import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.callbackdev.tempo.core.domain.widget.DialArc
import com.callbackdev.tempo.core.domain.widget.DialArcs
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The «In words» dial (PLANNING.md §7, `docs/adr/0003-widgets.md`): Passo's ring, become a clock.
 * Two layers in one small layout placed through Glance's `AndroidRemoteViews`:
 *
 * - the face, a bitmap painted here at each redraw: the ring's track, the day's arcs on it
 *   ([DialArcs]), four quarter dots;
 * - the hands, the system's `AnalogClock` over it, tinted in the card's ink. The launcher draws
 *   them and the system moves them, as it moves the `TextClock`: no repaint by Tempo, ever.
 *
 * The arcs change only at the agenda's boundaries, where the card is redrawn anyway; between two,
 * the hour hand walks along them by itself. The samples, the previews and the pictures draw the
 * hands into the face instead, at the sample's moment ([DialPainter.paint]'s `hands`), so they show
 * the sample's time and not the moment a test ran.
 *
 * The hands' drawables (`widget_dial_hour`, `widget_dial_minute`) are drawn on a 100-unit square
 * at [HAND_DRAWABLE_DP], larger than any dial: an `AnalogClock` scales its drawables down to its
 * size, never up. Their lengths and widths are [DialGeometry]'s, so a frozen dial and a live one
 * are the same drawing.
 */

/** The dial's proportions, as shares of its side: shared by the painter and the hands' drawables. */
internal object DialGeometry {
    const val STROKE: Float = 0.085f
    const val DOT_RADIUS: Float = 0.38f
    const val DOT_SIZE: Float = 0.016f
    const val HOUR_LENGTH: Float = 0.24f
    const val HOUR_WIDTH: Float = 0.07f
    const val MINUTE_LENGTH: Float = 0.33f
    const val MINUTE_WIDTH: Float = 0.05f
    const val CAP: Float = 0.045f
}

/** The dial's inks, from the card's [WidgetPalette]. */
internal data class DialInks(val track: Color, val focus: Color, val other: Color, val dots: Color, val hands: Color) {
    companion object {
        fun of(palette: WidgetPalette) = DialInks(
            track = palette.track,
            focus = palette.accent,
            other = palette.primary.copy(alpha = OTHER_ALPHA),
            dots = palette.primary.copy(alpha = DOTS_ALPHA),
            hands = palette.primary,
        )

        private const val OTHER_ALPHA = 0.5f
        private const val DOTS_ALPHA = 0.45f
    }
}

internal object DialPainter {
    /**
     * The largest side painted (Passo's `RingPainter.MAX_SIDE_PX`): Android 17 caps the bitmap
     * memory of a `RemoteViews` for apps that target it, and the largest dial is then 0.7 MB.
     */
    const val MAX_SIDE_PX: Int = 416

    /** The face at [sideDp]: the track, the arcs, the quarter dots; and the hands at [hands], if given. */
    fun paint(sideDp: Float, density: Float, arcs: List<DialArc>, inks: DialInks, hands: LocalTime?): Bitmap {
        val side = (sideDp * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val s = side.toFloat()
        val bitmap = createBitmap(side, side)
        val canvas = Canvas(bitmap)
        val center = s / 2f
        val stroke = maxOf(s * DialGeometry.STROKE, MIN_STROKE_DP * density * (s / (sideDp * density)))
        val inset = stroke / 2f
        val oval = RectF(inset, inset, s - inset, s - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        paint.color = inks.track.toArgb()
        canvas.drawArc(oval, 0f, FULL_TURN, false, paint)
        paint.strokeCap = Paint.Cap.ROUND
        arcs.forEach { arc ->
            paint.color = (if (arc.focus) inks.focus else inks.other).toArgb()
            // A round cap reaches half the stroke past each end: the arc is shortened by as much,
            // so it begins and ends where the event does, down to a dot for a moment.
            val capDegrees = (inset / (center - inset)) * DEGREES_PER_RADIAN
            val start = arc.from / DialArcs.FACE_MINUTES * FULL_TURN - QUARTER_TURN + capDegrees
            val sweep = (arc.sweep / DialArcs.FACE_MINUTES * FULL_TURN - capDegrees * 2).coerceAtLeast(
                MIN_SWEEP_DEGREES,
            )
            canvas.drawArc(oval, start, sweep, false, paint)
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = inks.dots.toArgb() }
        repeat(QUARTERS) { quarter ->
            val angle = quarter * QUARTER_TURN * RADIANS_PER_DEGREE
            canvas.drawCircle(
                center + s * DialGeometry.DOT_RADIUS * sin(angle),
                center - s * DialGeometry.DOT_RADIUS * cos(angle),
                maxOf(s * DialGeometry.DOT_SIZE, density),
                dot,
            )
        }
        hands?.let { paintHands(canvas, s, it, inks.hands) }
        return bitmap
    }

    /** The hands as the system's `AnalogClock` draws them from `widget_dial_hour` and `_minute`. */
    private fun paintHands(canvas: Canvas, s: Float, time: LocalTime, ink: Color) {
        val center = s / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.toArgb()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val minutes = time.hour % HOURS * MINUTES + time.minute + time.second / SECONDS
        fun hand(turns: Float, length: Float, width: Float) {
            val angle = turns * FULL_TURN * RADIANS_PER_DEGREE
            // The drawable's rounded end sits inside the length: the line stops a half width short.
            val reach = s * length - s * width / 2f
            paint.strokeWidth = s * width
            canvas.drawLine(center, center, center + reach * sin(angle), center - reach * cos(angle), paint)
        }
        hand(minutes / (HOURS * MINUTES), DialGeometry.HOUR_LENGTH, DialGeometry.HOUR_WIDTH)
        hand((time.minute + time.second / SECONDS) / MINUTES, DialGeometry.MINUTE_LENGTH, DialGeometry.MINUTE_WIDTH)
        canvas.drawCircle(
            center,
            center,
            s * DialGeometry.CAP,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    ink.toArgb()
            },
        )
    }

    private const val MIN_STROKE_DP = 3f
    private const val FULL_TURN = 360f
    private const val QUARTER_TURN = 90f
    private const val QUARTERS = 4
    private const val MIN_SWEEP_DEGREES = 0.1f
    private const val DEGREES_PER_RADIAN = (180 / PI).toFloat()
    private const val RADIANS_PER_DEGREE = (PI / 180).toFloat()
    private const val HOURS = 12
    private const val MINUTES = 60f
    private const val SECONDS = 60f
}

/** The size the hands' drawables are drawn at: above the largest dial, so they are only ever scaled down. */
internal const val HAND_DRAWABLE_DP = 200

/**
 * The dial as `RemoteViews` at [sideDp]: the face painted with [arcs] in [palette]'s inks, the
 * system's hands over it (or, with [frozenAt], painted in at that moment); a touch sends [tap].
 */
internal fun dialViews(
    context: Context,
    sideDp: Float,
    arcs: List<DialArc>,
    palette: WidgetPalette,
    tap: PendingIntent?,
    description: String,
    frozenAt: LocalTime?,
): RemoteViews {
    val inks = DialInks.of(palette)
    val density = context.resources.displayMetrics.density
    val face = DialPainter.paint(sideDp, density, arcs, inks, frozenAt)
    val views = RemoteViews(
        context.packageName,
        if (frozenAt ==
            null
        ) {
            R.layout.widget_dial
        } else {
            R.layout.widget_dial_frozen
        },
    )
    views.setImageViewBitmap(R.id.widget_dial_face, face)
    if (frozenAt == null) {
        val hands = ColorStateList.valueOf(inks.hands.toArgb())
        views.setColorStateList(R.id.widget_dial_hands, "setHourHandTintList", hands)
        views.setColorStateList(R.id.widget_dial_hands, "setMinuteHandTintList", hands)
    }
    views.setContentDescription(R.id.widget_dial, description)
    if (tap != null) views.setOnClickPendingIntent(R.id.widget_dial, tap)
    return views
}

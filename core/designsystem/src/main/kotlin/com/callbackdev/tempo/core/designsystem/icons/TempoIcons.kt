package com.callbackdev.tempo.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's line icons, drawn here on a 24-unit grid: one 1.8 stroke, round caps and joins, the
 * weight of Material Symbols' outlined set, so they sit beside Material's own components. Passo's
 * drawings, copied as the family's one icon set (Passo's `PassoIcons`); a shape Tempo needs is
 * drawn here the same way. Drawn rather than taken from a library: a dozen shapes did not justify
 * a new dependency, and every one of them is geometry a reader can check.
 */
object TempoIcons {
    val Settings: ImageVector by lazy {
        icon("settings") {
            // A gear of eight teeth around a hub: points computed, not traced.
            val teeth = 8
            for (i in 0 until teeth * 4) {
                val angle = 2 * PI * i / (teeth * 4) - PI / 2
                val radius = if ((i / 2) % 2 == 0) 9.6 else 7.4
                val x = (12 + radius * cos(angle)).toFloat()
                val y = (12 + radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3f)
        }
    }

    val Back: ImageVector by lazy {
        icon("back", autoMirror = true) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("chevron_right", autoMirror = true) {
            moveTo(9.5f, 6f)
            lineTo(15.5f, 12f)
            lineTo(9.5f, 18f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        icon("chevron_left", autoMirror = true) {
            moveTo(14.5f, 6f)
            lineTo(8.5f, 12f)
            lineTo(14.5f, 18f)
        }
    }

    val Check: ImageVector by lazy {
        icon("check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    val Plus: ImageVector by lazy {
        icon("plus") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Info: ImageVector by lazy {
        icon("info") {
            circle(12f, 12f, 9f)
            moveTo(12f, 11f)
            lineTo(12f, 16.5f)
            moveTo(12f, 7.6f)
            lineTo(12f, 7.7f)
        }
    }

    val Warning: ImageVector by lazy {
        icon("warning") {
            moveTo(12f, 3.8f)
            lineTo(21f, 19.8f)
            lineTo(3f, 19.8f)
            close()
            moveTo(12f, 9.5f)
            lineTo(12f, 14f)
            moveTo(12f, 16.9f)
            lineTo(12f, 17f)
        }
    }

    /** A clock: the time, the clock format. */
    val Clock: ImageVector by lazy {
        icon("clock") {
            circle(12f, 12f, 9f)
            moveTo(12f, 7f)
            lineTo(12f, 12f)
            lineTo(15.5f, 14f)
        }
    }

    /** A page of a calendar: the calendars, the date, the calendar app. */
    val Calendar: ImageVector by lazy {
        icon("calendar") {
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            curveTo(19.1f, 5f, 20f, 5.9f, 20f, 7f)
            lineTo(20f, 18f)
            curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
            lineTo(6f, 20f)
            curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
            lineTo(4f, 7f)
            curveTo(4f, 5.9f, 4.9f, 5f, 6f, 5f)
            close()
            moveTo(4f, 9.5f)
            lineTo(20f, 9.5f)
            moveTo(8f, 3f)
            lineTo(8f, 6.5f)
            moveTo(16f, 3f)
            lineTo(16f, 6.5f)
        }
    }

    val Bell: ImageVector by lazy {
        icon("bell") {
            moveTo(6f, 16.5f)
            lineTo(6f, 11f)
            curveTo(6f, 7.5f, 8.7f, 5f, 12f, 5f)
            curveTo(15.3f, 5f, 18f, 7.5f, 18f, 11f)
            lineTo(18f, 16.5f)
            lineTo(19.5f, 18f)
            lineTo(4.5f, 18f)
            close()
            moveTo(10f, 20.5f)
            curveTo(10.4f, 21.3f, 11.1f, 21.7f, 12f, 21.7f)
            curveTo(12.9f, 21.7f, 13.6f, 21.3f, 14f, 20.5f)
        }
    }

    /** A home screen's cards: two small ones over a wide one, the sizes the family's widgets take. */
    val Widgets: ImageVector by lazy {
        icon("widgets") {
            roundRect(4f, 4f, 11f, 11f, 1.8f)
            roundRect(13f, 4f, 20f, 11f, 1.8f)
            roundRect(4f, 13f, 20f, 20f, 1.8f)
        }
    }

    /** A shield: the privacy statement. */
    val Shield: ImageVector by lazy {
        icon("shield") {
            moveTo(12f, 3f)
            lineTo(19f, 6f)
            lineTo(19f, 11f)
            curveTo(19f, 15.5f, 16f, 19f, 12f, 21f)
            curveTo(8f, 19f, 5f, 15.5f, 5f, 11f)
            lineTo(5f, 6f)
            close()
            moveTo(9f, 12f)
            lineTo(11.2f, 14.2f)
            lineTo(15.2f, 10f)
        }
    }

    private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
        moveTo(left + r, top)
        lineTo(right - r, top)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right, y1 = top + r)
        lineTo(right, bottom - r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right - r, y1 = bottom)
        lineTo(left + r, bottom)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left, y1 = bottom - r)
        lineTo(left, top + r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left + r, y1 = top)
        close()
    }

    private fun icon(name: String, autoMirror: Boolean = false, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "tempo_$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        ).build()

    private const val STROKE = 1.8f
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

private fun PathBuilder.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
    moveTo(cx - rx, cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + rx, y1 = cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - rx, y1 = cy)
    close()
}

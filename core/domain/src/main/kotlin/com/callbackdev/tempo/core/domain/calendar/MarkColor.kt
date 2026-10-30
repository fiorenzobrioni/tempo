package com.callbackdev.tempo.core.domain.calendar

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A calendar's colour as a mark on a ground (PLANNING.md §6): the reader's colour, stepped in
 * lightness only as far as it takes to reach [MIN_CONTRAST] against [ground], WCAG's ratio for a
 * graphic that carries meaning. A pale yellow calendar is darkened on a light card and kept on a
 * dark one; its hue never changes, so the reader still knows it.
 *
 * Calendar colours are data, chosen in the calendar app with no thought for Tempo's grounds:
 * they mark an event and never paint its text, so 3:1 is the rule and not text's 4.5:1.
 *
 * Colours are ARGB integers, so the function stays pure Kotlin; the alpha of the result is opaque.
 */
fun markColor(argb: Int, ground: Int): Int {
    val opaque = argb or OPAQUE
    if (contrast(opaque, ground) >= MIN_CONTRAST) return opaque
    val (h, s, l) = toHsl(opaque)
    val darker = luminance(ground) > MID_LUMINANCE
    var lightness = l
    while (true) {
        lightness = if (darker) lightness - STEP else lightness + STEP
        val candidate = fromHsl(h, s, lightness.coerceIn(0.0, 1.0))
        if (contrast(candidate, ground) >= MIN_CONTRAST || lightness !in 0.0..1.0) return candidate
    }
}

/**
 * How faint a [mark] (an opaque colour that already reads on [ground], from [markColor]) may be
 * drawn and still read: [quiet] where the mark blended over the ground keeps [MIN_CONTRAST], else
 * the lowest alpha above it that does, up to opaque. Today's dial draws its quiet arcs so (review
 * of 30 Oct 2026): a deep colour on a light page fades a long way, while a mark lifted to 3:1 on
 * a dark page barely fades, and is as quiet as the ratio allows.
 */
fun markAlpha(mark: Int, ground: Int, quiet: Float): Float {
    var alpha = quiet
    while (alpha < 1f && contrast(blend(mark, ground, alpha), ground) < MIN_CONTRAST) alpha += ALPHA_STEP
    return alpha.coerceAtMost(1f)
}

/** [top] drawn at [alpha] over an opaque [ground], channel by channel, as a canvas composites it. */
fun blend(top: Int, ground: Int, alpha: Float): Int {
    fun channel(shift: Int): Int {
        val mixed = ((top shr shift) and 0xFF) * alpha + ((ground shr shift) and 0xFF) * (1 - alpha)
        return mixed.roundToInt().coerceIn(0, 255)
    }
    return OPAQUE or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

const val MIN_CONTRAST = 3.0

private const val STEP = 0.02
private const val ALPHA_STEP = 0.05f
private const val MID_LUMINANCE = 0.18
private const val OPAQUE = 0xFF shl 24

fun contrast(a: Int, b: Int): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** WCAG's relative luminance of an sRGB colour. */
fun luminance(argb: Int): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

private fun toHsl(argb: Int): Triple<Double, Double, Double> {
    val r = ((argb shr 16) and 0xFF) / 255.0
    val g = ((argb shr 8) and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val l = (high + low) / 2
    if (high == low) return Triple(0.0, 0.0, l)
    val d = high - low
    val s = if (l > 0.5) d / (2 - high - low) else d / (high + low)
    val h = when (high) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } / 6
    return Triple(h, s, l)
}

private fun fromHsl(h: Double, s: Double, l: Double): Int {
    fun hue(p: Double, q: Double, t0: Double): Double {
        val t = (t0 + 1) % 1
        return when {
            t < 1.0 / 6 -> p + (q - p) * 6 * t
            t < 1.0 / 2 -> q
            t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
            else -> p
        }
    }
    val (r, g, b) = if (s == 0.0) {
        Triple(l, l, l)
    } else {
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        Triple(hue(p, q, h + 1.0 / 3), hue(p, q, h), hue(p, q, h - 1.0 / 3))
    }
    fun byte(v: Double) = (v * 255).toInt().coerceIn(0, 255)
    return OPAQUE or (byte(r) shl 16) or (byte(g) shl 8) or byte(b)
}
